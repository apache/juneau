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
import java.util.Map;

import javax.crypto.KeyGenerator;

import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.rest.server.auth.ClaimsPrincipal;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link AeadRequestStateCodec}: round-trip, tamper, AAD-mismatch, malformed-token, and
 * per-process-ephemeral-key non-durability across two independent instances.
 */
class AeadRequestStateCodec_Test {

	private static final String AAD = "tools/call" + '\u0000' + "2026-07-28";

	@Test void a01_roundTripWithMatchingAadRecoversOriginalState() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(b, AAD);
		assertEquals(4, token.split("\\.", 4).length, "wire format must be version.keyId.nonce.ciphertext");
		assertTrue(token.startsWith("1."), "version segment must be the literal \"1\"");
		var c = a.unseal(token, AAD);
		assertTrue(c.isPresent());
		assertEquals(b, c.get());
	}

	@Test void a02_tamperedCiphertextByteFailsUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(b, AAD);
		var parts = token.split("\\.", 4);
		var ciphertext = Base64.getUrlDecoder().decode(parts[3]);
		ciphertext[0] ^= 1;
		var tampered = parts[0] + "." + parts[1] + "." + parts[2] + "."
			+ Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
		var c = a.unseal(tampered, AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a03_aadMismatchFailsUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(b, AAD);
		var c = a.unseal(token, "prompts/get" + '\u0000' + "2026-07-28");  // a valid token under a mismatched AAD
		assertTrue(c.isEmpty());
	}

	@Test void a04_malformedTokenMissingSeparatorFailsUnseal() {
		// Long enough (>= MIN_TOKEN_CHARS) to clear the length guard and actually reach the segment-count check.
		var a = new AeadRequestStateCodec();
		var c = a.unseal("a".repeat(50), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05_malformedTokenInvalidBase64FailsUnseal() {
		// A correctly-shaped token (version '.' keyId '.' 16-char nonce '.' body, >= MIN_TOKEN_CHARS) whose nonce
		// segment is not valid base64url: clears the length, segment-count, and nonce-length guards and fails
		// inside the base64 decode.
		var a = new AeadRequestStateCodec();
		var keyId = Base64.getUrlEncoder().withoutPadding().encodeToString("k".getBytes(StandardCharsets.UTF_8));
		var c = a.unseal("1." + keyId + "." + "!".repeat(16) + "." + "a".repeat(22), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05b_malformedTokenWrongNonceLengthFailsUnseal() {
		// Long enough to clear the length guard and reach the nonce-length check, with a nonce segment != 16 chars.
		var a = new AeadRequestStateCodec();
		var keyId = Base64.getUrlEncoder().withoutPadding().encodeToString("k".getBytes(StandardCharsets.UTF_8));
		var c = a.unseal("1." + keyId + "." + "aaaa" + "." + "a".repeat(40), AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a05c_tooShortTokenFailsLengthGuard() {
		// Below MIN_TOKEN_CHARS: rejected by the defensive length guard before any split/decode allocation.
		var a = new AeadRequestStateCodec();
		assertTrue(a.unseal("short", AAD).isEmpty());
		assertTrue(a.unseal("1.a.bbbb.cccc", AAD).isEmpty());
	}

	@Test void a06_perProcessEphemeralKeyPreventsCrossInstanceUnseal() {
		var a = new AeadRequestStateCodec();
		var b = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(state, AAD);
		var c = b.unseal(token, AAD);
		assertTrue(c.isEmpty());
	}

	@Test void a09_sharedKeyProviderAllowsCrossInstanceUnseal() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		var sharedKey = gen.generateKey();
		var keyProvider = StaticKeyProvider.of("2026-08-a", sharedKey);
		var a = new AeadRequestStateCodec(keyProvider);
		var b = new AeadRequestStateCodec(keyProvider);
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(state, AAD);
		var c = b.unseal(token, AAD);
		assertTrue(c.isPresent(), "two codecs sharing one StaticKeyProvider must unseal each other's tokens");
		assertEquals(state, c.get());
	}

	@Test void a10_rotatingKeyProviderUnsealsRetiredKeyAndSealsUnderNewCurrent() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		var keyA = gen.generateKey();
		var keyB = gen.generateKey();
		var providerBeforeRotation = StaticKeyProvider.of("2026-07-z", keyA);
		var codecBeforeRotation = new AeadRequestStateCodec(providerBeforeRotation);
		var oldState = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var oldToken = codecBeforeRotation.seal(oldState, AAD);

		var providerAfterRotation = StaticKeyProvider.create()
			.addKey("2026-07-z", keyA)
			.addKey("2026-08-a", keyB)
			.current("2026-08-a")
			.build();
		var codecAfterRotation = new AeadRequestStateCodec(providerAfterRotation);

		var recoveredOld = codecAfterRotation.unseal(oldToken, AAD);
		assertTrue(recoveredOld.isPresent(), "a token sealed under a still-resolvable retired key must still unseal after rotation");
		assertEquals(oldState, recoveredOld.get());

		var newState = new McpRequestState("continuation-value-2", "tools/call", 1, 123456789L, "jti-2", "args-hash-1");
		var newToken = codecAfterRotation.seal(newState, AAD);
		var newKeyId = new String(Base64.getUrlDecoder().decode(newToken.split("\\.", 4)[1]), StandardCharsets.UTF_8);
		assertEquals("2026-08-a", newKeyId, "new tokens must seal under the new current key's keyId");
	}

	@Test void a11_swappedKeyIdSegmentFailsUnseal() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		var keyA = gen.generateKey();
		var keyB = gen.generateKey();
		var provider = StaticKeyProvider.create()
			.addKey("2026-08-a", keyA)
			.addKey("2026-08-b", keyB)
			.current("2026-08-a")
			.build();
		var a = new AeadRequestStateCodec(provider);
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(state, AAD);
		var parts = token.split("\\.", 4);
		var swappedKeyId = Base64.getUrlEncoder().withoutPadding().encodeToString("2026-08-b".getBytes(StandardCharsets.UTF_8));
		var tampered = parts[0] + "." + swappedKeyId + "." + parts[2] + "." + parts[3];
		var c = a.unseal(tampered, AAD);
		assertTrue(c.isEmpty(), "swapping the keyId wire segment selects the wrong key and AAD, so the GCM tag check must fail");
	}

	@Test void a12_unknownVersionSegmentFailsUnseal() {
		var a = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(state, AAD);
		var parts = token.split("\\.", 4);
		var tampered = "2." + parts[1] + "." + parts[2] + "." + parts[3];
		var c = a.unseal(tampered, AAD);
		assertTrue(c.isEmpty(), "an unrecognized version literal must fail closed");
	}

	@Test void a13_unknownKeyIdFailsUnseal() throws Exception {
		var gen = KeyGenerator.getInstance("AES");
		gen.init(256);
		var keyA = gen.generateKey();
		var sealingProvider = StaticKeyProvider.of("2026-08-a", keyA);
		var sealingCodec = new AeadRequestStateCodec(sealingProvider);
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = sealingCodec.seal(state, AAD);

		var keyB = gen.generateKey();
		var unsealingProvider = StaticKeyProvider.of("2026-08-b", keyB); // does not know "2026-08-a"
		var unsealingCodec = new AeadRequestStateCodec(unsealingProvider);
		var c = unsealingCodec.unseal(token, AAD);
		assertTrue(c.isEmpty(), "a keyId the provider cannot resolve must fail closed");
	}

	/**
	 * Pins the continuation type-fidelity contract documented on {@link McpRequestState}: a {@code Map}-valued
	 * continuation round-trips as generic JSON ({@link JsonMap}), never the original {@code Map} implementation.
	 */
	@Test void a07_mapValuedContinuationRoundTripsAsGenericJsonMapNotOriginalType() {
		var a = new AeadRequestStateCodec();
		var continuation = Map.of("step", 2, "cursor", "abc");
		var b = new McpRequestState(continuation, "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
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
		var b = new McpRequestState(continuation, "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
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

	/**
	 * Pins the fix for the latent {@code MAX_KEY_ID_B64_CHARS} bug: a legitimately-issued keyId at
	 * {@code KeyedSecret}'s max length (128 UTF-16 code units), made entirely of non-ASCII, 3-byte-UTF-8
	 * characters, must still round-trip through {@code seal}/{@code unseal} rather than getting rejected by
	 * the keyId-segment length guard before {@code resolveKey} is ever consulted.
	 */
	@Test void a14_maxLengthNonAsciiKeyIdRoundTrips() throws Exception {
		var keyId = "\u4e2d".repeat(128);
		var keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		var provider = StaticKeyProvider.of(keyId, keyGen.generateKey());
		var a = new AeadRequestStateCodec(provider);
		var b = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		var token = a.seal(b, AAD);
		var c = a.unseal(token, AAD);
		assertTrue(c.isPresent());
		assertEquals(b, c.get());
	}

	/**
	 * Pins the TODO-325 principal-bound-AAD contract: the caller's authenticated identity is folded into the AEAD's
	 * authenticated data at seal, so a token minted for one principal cannot be resumed by another. A real
	 * {@link AeadRequestStateCodec} seal uses a random per-seal nonce (see the class Javadoc), so two {@code seal}
	 * calls are never byte-identical even with everything else held fixed; this proves the binding invariant directly
	 * on ONE sealed token: it unseals successfully under the sealing principal, but fails ({@link Optional#empty()})
	 * under a completely different principal and under a <jk>null</jk> (anonymous) principal alike.
	 *
	 * <p>
	 * This is the inversion of the READY-312f F4 tripwire (formerly {@code a15_principalIsNotYetBoundSo...}): before
	 * TODO-325 landed, all three unseals succeeded; binding the identity into the AAD is exactly the change that flips
	 * the {@code bob} / <jk>null</jk> cases to failure.
	 */
	@Test void a15_principalIsBoundSoTokenRejectsDifferentPrincipal() {
		var a = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		Principal alice = () -> "alice";
		Principal bob = () -> "bob";
		var token = a.seal(state, AAD, alice);
		var underSamePrincipal = a.unseal(token, AAD, alice);
		var underDifferentPrincipal = a.unseal(token, AAD, bob);
		var underNullPrincipal = a.unseal(token, AAD, null);
		assertTrue(underSamePrincipal.isPresent(), "round trip under the sealing principal must succeed");
		assertEquals(state, underSamePrincipal.get());
		assertTrue(underDifferentPrincipal.isEmpty(),
			"principal is bound to the AAD (TODO-325), so a different principal must fail GCM tag verification");
		assertTrue(underNullPrincipal.isEmpty(),
			"principal is bound to the AAD (TODO-325), so a null (anonymous) principal must not unseal an authenticated-sealed token");
	}

	/**
	 * Pins the TODO-325 anonymous-caller policy (settled decision 2 &mdash; fail-closed with a fixed sentinel): a
	 * <jk>null</jk> principal binds to a constant sentinel identity rather than "skip binding", so anonymous&harr;
	 * anonymous round-trips while anonymous&harr;authenticated is rejected in BOTH directions.
	 */
	@Test void a16_anonymousSentinelRoundTripsButNeverCrossesAuthenticatedBoundary() {
		var a = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		Principal alice = () -> "alice";

		// Anonymous -> anonymous round-trips (the null sentinel is deterministic).
		var anonToken = a.seal(state, AAD, null);
		var anonUnderNull = a.unseal(anonToken, AAD, null);
		assertTrue(anonUnderNull.isPresent(), "anonymous seal must unseal under a null (anonymous) principal");
		assertEquals(state, anonUnderNull.get());

		// Anonymous-sealed token must NOT unseal under an authenticated principal.
		assertTrue(a.unseal(anonToken, AAD, alice).isEmpty(),
			"an anonymous-sealed token must not be resumable by an authenticated principal");

		// Authenticated-sealed token must NOT unseal anonymously (already covered by a15's null case; asserted here
		// from the opposite starting point for symmetry).
		var aliceToken = a.seal(state, AAD, alice);
		assertTrue(a.unseal(aliceToken, AAD, null).isEmpty(),
			"an authenticated-sealed token must not be resumable anonymously");
	}

	/**
	 * Pins cross-IdP collision safety at the codec level (settled decision 1 &mdash; the bound identity is
	 * {@code iss|sub}, not a bare {@code sub}): two {@link ClaimsPrincipal}s sharing the same {@code sub} but issued by
	 * different {@code iss} are distinct identities, so a token sealed under one does not unseal under the other. Also
	 * confirms the exact-match case still round-trips.
	 */
	@Test void a17_claimsPrincipalIssuerScopingPreventsCrossIdpResume() {
		var a = new AeadRequestStateCodec();
		var state = new McpRequestState("continuation-value", "tools/call", 1, 123456789L, "jti-1", "args-hash-1");
		Principal idpA = new ClaimsPrincipal("user-1", Map.of("iss", "https://idp-a.example.com", "sub", "user-1"));
		Principal idpB = new ClaimsPrincipal("user-1", Map.of("iss", "https://idp-b.example.com", "sub", "user-1"));

		var token = a.seal(state, AAD, idpA);
		var sameIdp = a.unseal(token, AAD, idpA);
		assertTrue(sameIdp.isPresent(), "same iss+sub must round-trip");
		assertEquals(state, sameIdp.get());
		assertTrue(a.unseal(token, AAD, idpB).isEmpty(),
			"same sub but a different iss is a different identity (cross-IdP collision safety), so unseal must fail");
	}

	// ---------------------------------------------------------------------------------------------
	// principalIdentity(...) helper (TODO-325): pins the exact canonical string for each derivation path so the
	// AAD-bound identity shape is locked down independent of the seal/unseal round-trip above.
	// ---------------------------------------------------------------------------------------------

	@Test void b01_identity_nullPrincipalIsFixedSentinel() {
		assertEquals("anonymous", AeadRequestStateCodec.principalIdentity(null));
	}

	@Test void b02_identity_claimsPrincipalUsesLengthPrefixedIssAndSub() {
		var p = new ClaimsPrincipal("ignored-name", Map.of("iss", "i", "sub", "s"));
		// len(iss)=1 NUL iss NUL len(sub)=1 NUL sub
		assertEquals("1\u0000i\u00001\u0000s", AeadRequestStateCodec.principalIdentity(p));
	}

	@Test void b03_identity_barePrincipalFallsBackToNameAsSubjectWithEmptyIssuer() {
		Principal p = () -> "alice";
		// iss empty (len 0), sub=name "alice" (len 5)
		assertEquals("0\u0000\u00005\u0000alice", AeadRequestStateCodec.principalIdentity(p));
	}

	@Test void b04_identity_claimsPrincipalMissingSubFallsBackToName() {
		// No sub claim -> getName() ("alice") is used as the subject; iss claim still honored.
		var p = new ClaimsPrincipal("alice", Map.of("iss", "https://idp"));
		var iss = "https://idp";
		assertEquals(iss.length() + "\u0000" + iss + "\u0000" + "5\u0000alice", AeadRequestStateCodec.principalIdentity(p));
	}

	@Test void b05_identity_differsByIssuerOnly() {
		var a = new ClaimsPrincipal("n", Map.of("iss", "https://idp-a", "sub", "same"));
		var b = new ClaimsPrincipal("n", Map.of("iss", "https://idp-b", "sub", "same"));
		assertNotEquals(AeadRequestStateCodec.principalIdentity(a), AeadRequestStateCodec.principalIdentity(b),
			"two principals differing only in issuer must produce different identities (cross-IdP collision safety)");
	}

	@Test void b06_identity_differsBySubjectOnly() {
		var a = new ClaimsPrincipal("n", Map.of("iss", "https://idp", "sub", "user-1"));
		var b = new ClaimsPrincipal("n", Map.of("iss", "https://idp", "sub", "user-2"));
		assertNotEquals(AeadRequestStateCodec.principalIdentity(a), AeadRequestStateCodec.principalIdentity(b),
			"two principals differing only in subject must produce different identities");
	}

	@Test void b07_identity_lengthPrefixDefeatsDelimiterCollision() {
		// Without length-prefixing, (iss="a", sub="\u0000b") and (iss="a\u0000", sub="b") would both join to
		// "a\u0000\u0000b" and collide.  The length prefixes keep them distinct.
		var p1 = new ClaimsPrincipal("n", Map.of("iss", "a", "sub", "\u0000b"));
		var p2 = new ClaimsPrincipal("n", Map.of("iss", "a\u0000", "sub", "b"));
		assertNotEquals(AeadRequestStateCodec.principalIdentity(p1), AeadRequestStateCodec.principalIdentity(p2),
			"length-prefixed encoding must not let a NUL-bearing iss/sub collide two distinct identities");
	}

	// ---------------------------------------------------------------------------------------------
	// lengthPrefixJoin(...) helper: pins the shared framing seal/unseal use to compose the OUTER
	// aad/keyId/identity AAD triple (the same self-delimiting scheme principalIdentity uses for iss|sub, above).
	// ---------------------------------------------------------------------------------------------

	@Test void b08_lengthPrefixJoin_defeatsFieldBoundaryCollisionAcrossThreeFields() {
		// Mirrors b07 one level up, at the outer 3-field (aad, keyId, identity) composition: without
		// length-prefixing, ("a", "b", "\u0000c") and ("a", "b\u0000", "c") would both naively join to
		// "a\u0000b\u0000\u0000c". The length prefixes keep them distinct.
		var t1 = AeadRequestStateCodec.lengthPrefixJoin("a", "b", "\u0000c");
		var t2 = AeadRequestStateCodec.lengthPrefixJoin("a", "b\u0000", "c");
		assertNotEquals(t1, t2,
			"length-prefixed outer AAD composition must not let a NUL-bearing aad/keyId/identity field collide two distinct triples");
	}

	@Test void b09_lengthPrefixJoin_matchesExpectedEncodingForThreeFields() {
		// Pins the exact wire shape: len(f0) NUL f0 NUL len(f1) NUL f1 NUL len(f2) NUL f2.
		assertEquals("2\u0000ab\u00000\u0000\u00001\u0000c", AeadRequestStateCodec.lengthPrefixJoin("ab", "", "c"));
	}
}
