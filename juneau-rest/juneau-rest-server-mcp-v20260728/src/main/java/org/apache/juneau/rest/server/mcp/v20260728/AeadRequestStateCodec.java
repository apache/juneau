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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.juneau.marshall.marshaller.Json;

/**
 * Built-in default {@link RequestStateCodec}: AES-256-GCM with a per-process ephemeral key.
 *
 * <p>
 * <b>Not restart-durable and not shareable across process instances by design.</b> Each instance generates
 * its own random key at construction time; a token sealed by one instance can never be unsealed by another
 * (including the same process after a restart). Operators who need cross-restart or multi-instance resumption
 * must supply a shared/rotating-key {@link RequestStateCodec} implementation via {@link McpMrtrConfig} instead
 * of relying on this default. This is a documented, tested property (see {@code AeadRequestStateCodec_Test}),
 * not an accidental limitation.
 *
 * <p>
 * Sealed-token format (all opaque to callers): {@code base64(nonce) '.' base64(ciphertext+tag)}. The
 * plaintext is the JSON serialization of the {@link McpRequestState} record. The AAD passed to
 * {@link #seal}/{@link #unseal} is authenticated but never encrypted (standard AES-GCM AAD semantics) &mdash;
 * the dispatcher passes the canonical {@code method + '\u0000' + protocolVersion} (NUL-separated) form as the
 * AAD (see {@code McpRevision#aad}).
 *
 * <p>
 * {@link #unseal} is wired to untrusted, client-supplied {@code requestState} strings, so it applies a
 * defensive length guard (a plausibly-sized token only) before any base64 decode; an out-of-bounds input
 * returns {@link Optional#empty()} rather than allocating from an attacker-controlled length.
 */
public class AeadRequestStateCodec implements RequestStateCodec {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int KEY_BITS = 256;
	private static final int GCM_TAG_BITS = 128;
	private static final int NONCE_BYTES = 12;

	// Exact unpadded base64url length of the 12-byte nonce prefix (ceil(12*4/3)=16). The nonce part of a
	// well-formed token is always exactly this many chars, checked after the split in unseal.
	private static final int NONCE_B64_CHARS = 16;

	// Defensive bounds on the untrusted, client-supplied token string (see unseal). The real floor of a
	// well-formed token is NONCE_B64_CHARS + '.' + base64url of at least the 16-byte GCM tag (22 chars) = 39, so
	// anything shorter cannot be a valid token; 64KB is orders of magnitude above any legitimate sealed
	// McpRequestState.
	private static final int MIN_TOKEN_CHARS = NONCE_B64_CHARS + 1 + 22;
	private static final int MAX_TOKEN_CHARS = 64 * 1024;

	private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

	private final SecretKey key;
	private final SecureRandom random = new SecureRandom();

	/**
	 * Constructor. Generates a fresh, per-instance AES-256 key.
	 */
	public AeadRequestStateCodec() {
		try {
			var gen = KeyGenerator.getInstance("AES");
			gen.init(KEY_BITS);
			key = gen.generateKey();
		} catch (Exception e) { // HTT every JDK guarantees AES-256 key generation via the standard JCE provider
			throw rex(e, "Failed to generate AES-GCM key");
		}
	}

	@Override /* RequestStateCodec */
	public String seal(McpRequestState state, String aad) {
		try {
			// Random 96-bit nonce per seal. AES-GCM's birthday bound makes random nonces safe up to roughly 2^32
			// seals under a single key; the built-in default's key is per-process and ephemeral, so it never
			// approaches that, but a long-lived custom-codec key should be rotated well before 2^32 seals.
			var nonce = new byte[NONCE_BYTES];
			random.nextBytes(nonce);
			var cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
			cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
			var plaintext = Json.of(state).getBytes(StandardCharsets.UTF_8);
			var ciphertext = cipher.doFinal(plaintext);
			return B64URL.encodeToString(nonce) + "." + B64URL.encodeToString(ciphertext);
		} catch (Exception e) { // HTT encryption with a freshly-generated 12-byte nonce cannot fail under the standard JCE provider
			throw rex(e, "Failed to seal requestState");
		}
	}

	@Override /* RequestStateCodec */
	public Optional<McpRequestState> unseal(String token, String aad) {
		try {
			if (token.length() < MIN_TOKEN_CHARS || token.length() > MAX_TOKEN_CHARS)
				return Optional.empty();
			var parts = token.split("\\.", 2);
			// A well-formed token is exactly base64url(12-byte nonce) '.' base64url(ciphertext+tag); reject any
			// token missing the separator or whose nonce part is not the expected length before any base64 decode.
			if (parts.length != 2 || parts[0].length() != NONCE_B64_CHARS)
				return Optional.empty();
			var nonce = Base64.getUrlDecoder().decode(parts[0]);
			var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
			var cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
			cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
			var plaintext = cipher.doFinal(ciphertext);
			return Optional.of(Json.to(new String(plaintext, StandardCharsets.UTF_8), McpRequestState.class));
		} catch (@SuppressWarnings("unused") Exception e) {
			// Any failure (bad base64, AEAD tag mismatch from tamper/AAD mismatch, malformed JSON) is a
			// verification failure per the RequestStateCodec contract, not an exceptional condition to propagate.
			return Optional.empty();
		}
	}
}
