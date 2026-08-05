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
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.auth.ClaimsPrincipal;

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
 * form as the caller-supplied AAD (see {@code McpRevision#aad}); this codec composes the caller-supplied
 * {@code aad}, the {@link KeyProvider}'s {@code keyId} (per its implicit {@code keyId}-authentication contract),
 * and {@code principalIdentity(principal)} (the TODO-325 principal binding, below) into the cipher's actual AAD
 * using the same self-delimiting, length-prefixed framing described on {@link #principalIdentity(Principal)}
 * &mdash; so the three-field outer composition is unambiguous exactly as the two-field inner one is, even if a
 * field happened to contain a NUL.
 *
 * <p>
 * <b>Principal-bound AAD (TODO-325).</b> {@link #seal}/{@link #unseal} fold the caller's authenticated
 * {@link Principal} identity into the AEAD's authenticated data, so a {@code requestState} minted for one caller
 * cannot be resumed by another: a mismatched principal fails the GCM tag check and {@link #unseal} returns
 * {@link Optional#empty()}. The bound identity is a canonical, deterministic {@code iss|sub} (issuer + subject)
 * string derived by {@link #principalIdentity(Principal)} &mdash; issuer-scoped so a bare {@code sub} cannot
 * collide across IdPs. A <jk>null</jk> (anonymous / RS-auth-disabled) principal binds <b>fail-closed</b> to a
 * fixed {@code "anonymous"} sentinel rather than skipping the binding, so an anonymous-sealed token and an
 * authenticated-sealed token are never mutually resumable (anonymous&harr;anonymous still round-trips). See
 * {@link #principalIdentity(Principal)} for the exact canonical form and its collision-safety guarantee.
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

	// TODO-325 anonymous-caller sentinel (settled decision 2). A null principal binds fail-closed to this fixed
	// identity rather than skipping the binding. It is intentionally a plain literal with no NUL: a real
	// principalIdentity(...) is always length-prefixed and begins with a decimal digit and contains NULs, so the
	// sentinel can never collide with any derived identity.
	static final String ANONYMOUS_IDENTITY = "anonymous";

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
	public String seal(McpRequestState state, String aad, Principal principal) {
		try {
			var ks = keyProvider.currentKey();
			// Random 96-bit nonce per seal. AES-GCM's birthday bound makes random nonces safe up to roughly 2^32
			// seals under a single key; see the class Javadoc for why this matters more for a long-lived custom
			// KeyProvider key than for the per-process ephemeral default.
			var nonce = new byte[NONCE_BYTES];
			random.nextBytes(nonce);
			var cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, ks.key(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
			// TODO-325: fold the authenticated caller identity into the AEAD's authenticated data, so a token
			// minted for one principal cannot be resumed by another. unseal derives the SAME identity below; a
			// mismatch fails the GCM tag check there. The three fields are framed with lengthPrefixJoin (same
			// self-delimiting scheme as principalIdentity's iss|sub) so the outer composition is unambiguous too.
			cipher.updateAAD(lengthPrefixJoin(aad, ks.keyId(), principalIdentity(principal)).getBytes(StandardCharsets.UTF_8));
			var plaintext = Json.of(state).getBytes(StandardCharsets.UTF_8);
			var ciphertext = cipher.doFinal(plaintext);
			var keyIdB64 = B64URL.encodeToString(ks.keyId().getBytes(StandardCharsets.UTF_8));
			return VERSION + "." + keyIdB64 + "." + B64URL.encodeToString(nonce) + "." + B64URL.encodeToString(ciphertext);
		} catch (Exception e) { // HTT encryption with a freshly-generated 12-byte nonce cannot fail under the standard JCE provider
			throw rex(e, "Failed to seal requestState");
		}
	}

	@Override /* RequestStateCodec */
	public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
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
			// TODO-325: fold the SAME caller identity seal bound in, so a token minted for principal A fails the GCM
			// tag check (and returns Optional.empty() below) when replayed under principal B or anonymously. Same
			// lengthPrefixJoin framing as seal, above.
			cipher.updateAAD(lengthPrefixJoin(aad, keyId, principalIdentity(principal)).getBytes(StandardCharsets.UTF_8));
			var plaintext = cipher.doFinal(ciphertext);
			return Optional.of(Json.to(new String(plaintext, StandardCharsets.UTF_8), McpRequestState.class));
		} catch (@SuppressWarnings("unused") Exception e) {
			// Any failure (bad base64, unknown/retired keyId, AEAD tag mismatch from tamper/AAD/keyId mismatch,
			// malformed JSON) is a verification failure per the RequestStateCodec contract, not an exceptional
			// condition to propagate.
			return Optional.empty();
		}
	}

	/**
	 * Derives the canonical, deterministic identity string this codec folds into the AEAD's authenticated data to
	 * bind a sealed {@code requestState} to its caller (TODO-325).
	 *
	 * <p>
	 * <b>Bound identity (settled decision 1): {@code iss|sub}.</b> When {@code principal} is a {@link ClaimsPrincipal}
	 * (the type the F2 resource-server layer produces &mdash; {@code JwtTokenValidator}, OAuth introspection, OIDC, and
	 * SAML all return one), the {@code iss} and {@code sub} claims are read from its claim set; the issuer scopes the
	 * subject so a bare {@code sub} cannot collide across IdPs. When {@code sub} is absent or empty (or the principal
	 * is a bare {@link Principal} that exposes only {@link Principal#getName() getName()}), {@link Principal#getName()
	 * getName()} is used as the subject with an empty issuer &mdash; a deterministic, collision-conscious fallback.
	 *
	 * <p>
	 * <b>Anonymous (settled decision 2): fixed sentinel, fail-closed.</b> A <jk>null</jk> principal (anonymous caller /
	 * RS auth disabled) binds to the fixed {@link #ANONYMOUS_IDENTITY} sentinel rather than skipping the binding, so an
	 * anonymous-sealed token and an authenticated-sealed token are never mutually resumable.
	 *
	 * <p>
	 * <b>Encoding (collision-safe).</b> The identity is a length-prefixed, fixed-order concatenation:
	 * {@code len(iss) + '\u0000' + iss + '\u0000' + len(sub) + '\u0000' + sub}. Prefixing each component with its
	 * length makes the encoding self-delimiting, so no {@code iss}/{@code sub} value that itself contains the NUL
	 * delimiter can push two distinct identities onto the same string (which a plain {@code iss + '\u0000' + sub} join
	 * could). Both {@link #seal} and {@link #unseal} derive this identical string for the same principal, so a matched
	 * caller round-trips and any mismatch (different subject, different issuer, or authenticated-vs-anonymous) yields
	 * different AAD bytes and fails GCM tag verification. The value is never logged or surfaced in an exception.
	 *
	 * @param principal The authenticated caller. May be <jk>null</jk> (binds to {@link #ANONYMOUS_IDENTITY}).
	 * @return The canonical identity string. Never <jk>null</jk>.
	 */
	static String principalIdentity(Principal principal) {
		if (principal == null)
			return ANONYMOUS_IDENTITY;
		String iss;
		String sub;
		if (principal instanceof ClaimsPrincipal cp) {
			iss = cp.getClaim("iss", String.class).orElse("");
			sub = cp.getClaim("sub", String.class).orElse("");
			if (sub.isEmpty())
				sub = ein(cp.getName());
		} else {
			iss = "";
			sub = ein(principal.getName());
		}
		return lengthPrefixJoin(iss, sub);
	}

	/**
	 * Joins the given fields into a single self-delimiting string: each field is preceded by its own decimal
	 * length, and every token (each length, and each field) is NUL-separated &mdash; equivalent to
	 * {@code String.join("\u0000", len(fields[0]), fields[0], len(fields[1]), fields[1], ...)}. Because a
	 * field's exact length is always known before its bytes are consumed, an embedded NUL inside a field can
	 * never be misread as a token boundary, so two distinct field tuples never join to the same string. Shared
	 * by {@link #principalIdentity(Principal)} (framing {@code iss}/{@code sub}) and by {@link #seal}/
	 * {@link #unseal} (framing the outer {@code aad}/{@code keyId}/identity triple).
	 *
	 * @param fields The fields to frame, in order. Must not be <jk>null</jk> and must not contain a <jk>null</jk>
	 * 	element.
	 * @return The length-prefixed, NUL-joined encoding. Never <jk>null</jk>.
	 */
	static String lengthPrefixJoin(String... fields) {
		var sb = new StringBuilder();
		for (var f : fields) {
			if (sb.length() > 0)
				sb.append('\u0000');
			sb.append(f.length()).append('\u0000').append(f);
		}
		return sb.toString();
	}
}
