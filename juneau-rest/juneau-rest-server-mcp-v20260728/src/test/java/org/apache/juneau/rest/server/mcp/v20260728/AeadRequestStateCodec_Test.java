/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.Map;

import org.apache.juneau.marshall.collections.JsonMap;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link AeadRequestStateCodec}: round-trip, tamper, AAD-mismatch, malformed-token, and
 * per-process-ephemeral-key non-durability across two independent instances.
 */
class AeadRequestStateCodec_Test {

	private static final String AAD = "tools/call" + '\u0000' + "2026-07-28";

	@Test void a01_roundTripWithMatchingAadRecoversOriginalState() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		var token = a.seal(b, AAD);
		var c = a.unseal(token, AAD);
		assertTrue(c.isPresent());
		assertEquals(b, c.get());
	}

	@Test void a02_tamperedCiphertextByteFailsUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		var token = a.seal(b, AAD);
		var parts = token.split("\\.", 2);
		var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
		ciphertext[0] ^= 1;
		var tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
		var c = a.unseal(tampered, AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a03_aadMismatchFailsUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		var token = a.seal(b, AAD);
		var c = a.unseal(token, "prompts/get" + '\u0000' + "2026-07-28");  // a valid token under a mismatched AAD
		assertTrue(c.isEmpty());
	}

	@Test void a04_malformedTokenMissingSeparatorFailsUnseal() {
		// Long enough (>= MIN_TOKEN_CHARS) to clear the length guard and actually reach the separator check.
		var a = new AeadRequestStateCodec();
		var c = a.unseal("a".repeat(40), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05_malformedTokenInvalidBase64FailsUnseal() {
		// A correctly-shaped token (16-char nonce part '.' body, >= MIN_TOKEN_CHARS) whose nonce part is not valid
		// base64url: clears the length and separator/nonce-length guards and fails inside the base64 decode.
		var a = new AeadRequestStateCodec();
		var c = a.unseal("!".repeat(16) + "." + "a".repeat(22), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05b_malformedTokenWrongNonceLengthFailsUnseal() {
		// Long enough to clear the length guard and reach the nonce-length check, with a nonce part != 16 chars.
		var a = new AeadRequestStateCodec();
		var c = a.unseal("aaaa" + "." + "a".repeat(40), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05c_tooShortTokenFailsLengthGuard() {
		// Below MIN_TOKEN_CHARS: rejected by the defensive length guard before any split/decode allocation.
		var a = new AeadRequestStateCodec();
		assertTrue(a.unseal("short", AAD).isEmpty());
		assertTrue(a.unseal("!!!.!!!", AAD).isEmpty());
	}

	@Test void a06_perProcessEphemeralKeyPreventsCrossInstanceUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		var token = a.seal(state, AAD);
		var c = b.unseal(token, AAD);
		assertTrue(c.isEmpty());
	}

	/**
	 * Pins the continuation type-fidelity contract documented on {@link McpRequestState}: a {@code Map}-valued
	 * continuation round-trips as generic JSON ({@link JsonMap}), never the original {@code Map} implementation.
	 */
	@Test void a07_mapValuedContinuationRoundTripsAsGenericJsonMapNotOriginalType() {
		var a = new AeadRequestStateCodec();
		var continuation = Map.of("step", 2, "cursor", "abc");
		var b = new McpRequestState(continuation, "tools/call", 1, 123456789L);
		var token = a.seal(b, AAD);
		var c = a.unseal(token, AAD);
		assertTrue(c.isPresent());
		var recovered = c.get().continuation();
		assertInstanceOf(JsonMap.class, recovered);
		assertNotEquals(continuation.getClass(), recovered.getClass());
		assertEquals(JsonMap.of("step", 2, "cursor", "abc"), recovered);
	}

	// Helper bean for a08, below; deliberately not Juneau-marshall-annotated to prove the codec's
	// generic-JSON round-trip does not depend on the continuation's original type declaring anything special.
	// Must be public (not just package-visible) for Juneau's default bean introspection to recognize it as a
	// bean rather than falling back to toString() on a non-public class.
	public static class A08_Continuation {
		private int step;
		private String note;

		public int getStep() {
			return step;
		}

		public A08_Continuation setStep(int value) {
			step = value;
			return this;
		}

		public String getNote() {
			return note;
		}

		public A08_Continuation setNote(String value) {
			note = value;
			return this;
		}
	}

	/**
	 * Pins the same continuation type-fidelity contract for an ordinary-bean continuation: it comes back as a
	 * generic {@link JsonMap}, not the original bean type &mdash; the handler is responsible for converting it
	 * back to its own type if it needs true type fidelity.
	 */
	@Test void a08_beanValuedContinuationRoundTripsAsGenericJsonMapNotOriginalBeanType() {
		var a = new AeadRequestStateCodec();
		var continuation = new A08_Continuation().setStep(2).setNote("resume");
		var b = new McpRequestState(continuation, "tools/call", 1, 123456789L);
		var token = a.seal(b, AAD);
		var c = a.unseal(token, AAD);
		assertTrue(c.isPresent());
		var recovered = c.get().continuation();
		assertFalse(recovered instanceof A08_Continuation, "must not deserialize back to the original bean type");
		assertInstanceOf(JsonMap.class, recovered);
		var recoveredMap = (JsonMap)recovered;
		assertEquals(2, recoveredMap.get("step"));
		assertEquals("resume", recoveredMap.get("note"));
	}
}
