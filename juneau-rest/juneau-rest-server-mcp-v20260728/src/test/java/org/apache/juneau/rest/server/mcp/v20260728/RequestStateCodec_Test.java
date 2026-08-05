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

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.Optional;

import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.Test;

/**
 * Interface-contract coverage for {@link RequestStateCodec}, proving the SPI shape itself is sound via a
 * trivial {@code Base64}-only fake implementation, before {@link AeadRequestStateCodec}'s real AEAD behavior
 * exists (see {@code AeadRequestStateCodec_Test}).
 */
class RequestStateCodec_Test {

	/**
	 * Trivial, non-cryptographic fake: the sealed token is just {@code base64(aad + '\0' + json(state))}.
	 * {@link #unseal(String, String)} rejects the token outright when the supplied AAD does not match the one
	 * baked in at seal time, proving the SPI's AAD-mismatch contract independent of any real crypto.
	 */
	private static final class FakeCodec implements RequestStateCodec {

		@Override /* RequestStateCodec */
		public String seal(McpRequestState state, String aad, Principal principal) {
			var plaintext = aad + "\u0000" + Json.of(state);
			return Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
		}

		@Override /* RequestStateCodec */
		public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
			byte[] decoded;
			try {
				decoded = Base64.getDecoder().decode(token);
			} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
				// Malformed Base64 is a verification failure per the RequestStateCodec contract, not an
				// exceptional condition to propagate — mirrors AeadRequestStateCodec's never-throw behavior.
				return Optional.empty();
			}
			var plaintext = new String(decoded, StandardCharsets.UTF_8);
			var parts = plaintext.split("\u0000", 2);
			if (parts.length != 2 || ! parts[0].equals(aad))
				return Optional.empty();
			return Optional.of(Json.to(parts[1], McpRequestState.class));
		}
	}

	@Test void a01_roundTripWithMatchingAadRecoversOriginalState() {
		var a = new FakeCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		// NOTE: FakeCodec uses '\u0000' as its own internal aad/payload framing separator, so this SPI-contract
		// fake deliberately uses a NUL-free AAD literal (the real canonical NUL-separated form is exercised by
		// AeadRequestStateCodec_Test).
		var token = a.seal(b, "tools/call:2026-07-28");
		var c = a.unseal(token, "tools/call:2026-07-28");
		assertTrue(c.isPresent());
		assertEquals(b, c.get());
	}

	@Test void a02_unsealWithMismatchedAadReturnsEmpty() {
		var a = new FakeCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L);
		var token = a.seal(b, "tools/call:2026-07-28");
		var c = a.unseal(token, "prompts/get:2026-07-28");
		assertTrue(c.isEmpty());
	}

	@Test void a03_unsealWithMalformedBase64ReturnsEmptyRatherThanThrowing() {
		var a = new FakeCodec();
		var c = a.unseal("!!!not-base64!!!", "tools/call:2026-07-28");
		assertTrue(c.isEmpty());
	}

	// F4 (READY-312f): the principal-bearing 3-arg methods are the canonical SPI; the 2-arg convenience overloads
	// delegate to them with a null (no-principal) identity, and an explicit principal is passed through verbatim.
	private static final class B_CapturingCodec implements RequestStateCodec {
		Principal sealPrincipal = () -> "<unset>";
		Principal unsealPrincipal = () -> "<unset>";

		@Override public String seal(McpRequestState state, String aad, Principal principal) { sealPrincipal = principal; return "t"; }
		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) { unsealPrincipal = principal; return Optional.empty(); }
	}

	@Test void b01_twoArgOverloadsDelegateWithNullPrincipal() {
		var a = new B_CapturingCodec();
		a.seal(new McpRequestState("c", "tools/call", 1, 1L), "aad");
		a.unseal("t", "aad");
		assertNull(a.sealPrincipal);
		assertNull(a.unsealPrincipal);
	}

	@Test void b02_threeArgMethodsReceiveTheSuppliedPrincipal() {
		var a = new B_CapturingCodec();
		Principal p = () -> "carol";
		a.seal(new McpRequestState("c", "tools/call", 1, 1L), "aad", p);
		a.unseal("t", "aad", p);
		assertSame(p, a.sealPrincipal);
		assertSame(p, a.unsealPrincipal);
	}
}
