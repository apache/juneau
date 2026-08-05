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
import javax.crypto.spec.GCMParameterSpec;

import org.apache.juneau.marshall.marshaller.Json;

/**
 * Built-in default {@link RequestStateCodec}: AES-256-GCM keyed by a pluggable {@link KeyProvider}.
 *
 * <p>
 * The no-arg constructor uses a fresh, per-process {@link EphemeralKeyProvider} &mdash; <b>not restart-durable
 * and not shareable across process instances by design.</b> Each instance generates its own random key and
 * {@code keyId} at construction time; a token sealed by one instance can never be unsealed by another
 * (including the same process after a restart). Operators who need cross-restart or multi-instance resumption
 * must supply a shared/rotating {@link KeyProvider} (e.g. {@link StaticKeyProvider}) via
 * {@link #AeadRequestStateCodec(KeyProvider)} instead of relying on this default. This is a documented, tested
 * property (see {@code AeadRequestStateCodec_Test}), not an accidental limitation.
 *
 * <p>
 * Sealed-token format (all opaque to callers, versioned so it can evolve):
 * {@code version '.' base64url(keyId) '.' base64url(nonce) '.' base64url(ciphertext+tag)}. The current
 * {@code version} literal is {@code "1"}; an unrecognized version fails {@link #unseal} closed. The
 * {@code keyId} segment is cleartext base64url of the UTF-8 {@code keyId} bytes &mdash; it has to be read
 * before the key it names can be resolved for decryption &mdash; but it is folded into the AEAD's authenticated
 * data, so a swapped {@code keyId} fails the GCM tag check exactly like any other tamper. The plaintext is the
 * JSON serialization of the {@link McpRequestState} record.
 *
 * <p>
 * The AAD passed to {@link #seal}/{@link #unseal} is authenticated but never encrypted (standard AES-GCM AAD
 * semantics). The dispatcher passes the canonical {@code method + '\u0000' + protocolVersion} (NUL-separated)
 * form as the caller-supplied AAD (see {@code McpRevision#aad}); this codec appends {@code '\u0000' + keyId}
 * to that value before passing it to the cipher, per {@link KeyProvider}'s implicit {@code keyId}-authentication
 * contract.
 *
 * <p>
 * Random per-seal nonces are safe up to roughly 2^32 seals under a single key (the AES-GCM birthday bound). The
 * ephemeral default's key is per-process and never approaches that. A long-lived custom {@link KeyProvider} key
 * (e.g. a {@link StaticKeyProvider} entry left in place for months) is the case where this bound actually
 * matters &mdash; rotate to a fresh {@code keyId} well before it, not after.
 *
 * <p>
 * {@link #unseal} is wired to untrusted, client-supplied {@code requestState} strings, so it applies defensive
 * length guards (a plausibly-sized token, and a bounded {@code keyId} segment) before any base64 decode; an
 * out-of-bounds input returns {@link Optional#empty()} rather than allocating from an attacker-controlled
 * length.
 */
public class AeadRequestStateCodec implements RequestStateCodec {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_BITS = 128;
	private static final int NONCE_BYTES = 12;

	// Wire-format version literal (see class Javadoc). Bumping this is how the format could evolve; unseal
	// rejects any other value.
	private static final String VERSION = "1";

	// Exact unpadded base64url length of the 12-byte nonce segment (ceil(12*4/3)=16). The nonce segment of a
	// well-formed token is always exactly this many chars, checked after the split in unseal.
	private static final int NONCE_B64_CHARS = 16;

	// Defensive ceiling on the base64url'd keyId segment of an untrusted, client-supplied token (see unseal).
	// Derived from KeyedSecret's MAX_KEY_ID_CHARS=128 producer limit (measured in UTF-16 code units, no
	// charset restriction) at the worst case of 3 UTF-8 bytes/char: 128*3=384 bytes, whose unpadded base64url
	// encoding is ceil(384/3)*4=512 chars. Set to that worst case so it dominates any legitimately-issued
	// keyId while still capping an attacker-inflated segment before base64 decode.
	private static final int MAX_KEY_ID_B64_CHARS = 512;

	// Defensive floor on the untrusted, client-supplied token string as a whole (see unseal). The real floor of
	// a well-formed token is VERSION(1) + '.' + smallest possible keyId segment (2, for a 1-byte keyId) + '.' +
	// NONCE_B64_CHARS + '.' + base64url of at least the 16-byte GCM tag (22 chars). 64KB is orders of magnitude
	// above any legitimate sealed McpRequestState.
	private static final int MIN_TOKEN_CHARS = VERSION.length() + 1 + 2 + 1 + NONCE_B64_CHARS + 1 + 22;
	private static final int MAX_TOKEN_CHARS = 64 * 1024;

	private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

	private final KeyProvider keyProvider;
	private final SecureRandom random = new SecureRandom();

	/**
	 * Constructor. Uses a fresh, per-process {@link EphemeralKeyProvider} &mdash; the zero-config, non-shareable,
	 * non-durable default described in the class Javadoc.
	 */
	public AeadRequestStateCodec() {
		this(new EphemeralKeyProvider());
	}

	/**
	 * Constructor.
	 *
	 * @param keyProvider The source of sealing/resolving keys. Must not be <jk>null</jk>.
	 */
	public AeadRequestStateCodec(KeyProvider keyProvider) {
		if (keyProvider == null)
			throw iaex("keyProvider must not be null");
		this.keyProvider = keyProvider;
	}

	@Override /* RequestStateCodec */
	public String seal(McpRequestState state, String aad) {
		try {
			var ks = keyProvider.currentKey();
			// Random 96-bit nonce per seal. AES-GCM's birthday bound makes random nonces safe up to roughly 2^32
			// seals under a single key; see the class Javadoc for why this matters more for a long-lived custom
			// KeyProvider key than for the per-process ephemeral default.
			var nonce = new byte[NONCE_BYTES];
			random.nextBytes(nonce);
			var cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, ks.key(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
			cipher.updateAAD((aad + '\u0000' + ks.keyId()).getBytes(StandardCharsets.UTF_8));
			var plaintext = Json.of(state).getBytes(StandardCharsets.UTF_8);
			var ciphertext = cipher.doFinal(plaintext);
			var keyIdB64 = B64URL.encodeToString(ks.keyId().getBytes(StandardCharsets.UTF_8));
			return VERSION + "." + keyIdB64 + "." + B64URL.encodeToString(nonce) + "." + B64URL.encodeToString(ciphertext);
		} catch (Exception e) { // HTT encryption with a freshly-generated 12-byte nonce cannot fail under the standard JCE provider
			throw rex(e, "Failed to seal requestState");
		}
	}

	@Override /* RequestStateCodec */
	public Optional<McpRequestState> unseal(String token, String aad) {
		try {
			if (token.length() < MIN_TOKEN_CHARS || token.length() > MAX_TOKEN_CHARS)
				return Optional.empty();
			var parts = token.split("\\.", 4);
			// A well-formed token is exactly version '.' base64url(keyId) '.' base64url(12-byte nonce) '.'
			// base64url(ciphertext+tag); reject any token with the wrong segment count, an unrecognized version,
			// an empty/oversized keyId segment, or a nonce segment that isn't the expected length before any
			// base64 decode.
			if (parts.length != 4 || !VERSION.equals(parts[0]) || parts[1].isEmpty() || parts[1].length() > MAX_KEY_ID_B64_CHARS
					|| parts[2].length() != NONCE_B64_CHARS)
				return Optional.empty();
			var keyId = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			var nonce = Base64.getUrlDecoder().decode(parts[2]);
			var ciphertext = Base64.getUrlDecoder().decode(parts[3]);
			var resolved = keyProvider.resolveKey(keyId);
			if (resolved.isEmpty())
				return Optional.empty();
			var cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, resolved.get(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
			cipher.updateAAD((aad + '\u0000' + keyId).getBytes(StandardCharsets.UTF_8));
			var plaintext = cipher.doFinal(ciphertext);
			return Optional.of(Json.to(new String(plaintext, StandardCharsets.UTF_8), McpRequestState.class));
		} catch (@SuppressWarnings("unused") Exception e) {
			// Any failure (bad base64, unknown/retired keyId, AEAD tag mismatch from tamper/AAD/keyId mismatch,
			// malformed JSON) is a verification failure per the RequestStateCodec contract, not an exceptional
			// condition to propagate.
			return Optional.empty();
		}
	}
}
