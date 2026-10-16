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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.nio.charset.*;
import java.security.*;
import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import jakarta.servlet.http.*;

/**
 * Where a CSRF synchronizer token comes from, and how it is masked for embedding and checked on the way back.
 *
 * <p>
 * Two strategies are available:
 * <ul>
 * 	<li>{@link #perSession()} &mdash; the default.  The token is <c>HMAC-SHA256(key, UTF-8(sessionId))</c>, so
 * 		every session has its own token and nothing is stored in the session.  An attacker who can load the page
 * 		in their own browser learns only their own token, never the victim's.
 * 	<li>{@link #perProcess()} / {@link #perProcess(SynchronizerToken)} &mdash; one token for the whole process.
 * 		It needs no session, but any visitor can read it, so in a multi-user application it adds little beyond
 * 		the <c>Origin</c> check.
 * </ul>
 *
 * <h5 class='section'>Masking</h5>
 * <p>
 * The value written into a page is never the token itself.  Each embed gets a fresh 32-byte pad, and the
 * published value is <c>hex(pad || (pad XOR token))</c>: 128 lower-case hex characters.  The secret therefore
 * never repeats across renders or across the several places one page embeds it, which defeats an HTML
 * compression oracle (BREACH).  That is <b>all</b> masking buys: anyone who sees the wire value can unmask it, so
 * the token header must never be logged, and masking gives no protection to logs, proxies or the browser.
 *
 * <h5 class='section'>Verification</h5>
 * <p>
 * A presented value must be exactly 128 lower-case hex characters; anything else is refused before the secret is
 * touched.  It is then unmasked and compared with {@link MessageDigest#isEqual(byte[], byte[])} against the
 * expected 32 bytes.  A request with no session is compared against the HMAC of a fixed placeholder id that no
 * container can issue, so "no session" and "wrong token" do the same work and give the same answer.
 *
 * <h5 class='section'>Session ids</h5>
 * <p>
 * A per-session token changes whenever the session id changes, and this class never changes it.  An application
 * must call {@link HttpServletRequest#changeSessionId()} on login; otherwise a token seen before login stays
 * valid after it.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Default: one token per session, keyed by 32 fresh random bytes for this process.</jc>
 * 	CsrfTokens <jv>tokens</jv> = CsrfTokens.<jsm>perSession</jsm>();
 *
 * 	<jc>// A cluster: every node shares one CSPRNG-generated key, so any node accepts any node's token.</jc>
 * 	CsrfTokens <jv>clustered</jv> = CsrfTokens.<jsm>perSession</jsm>().key(<jv>keyBytesFromSecretStore</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link SynchronizerToken}
 * </ul>
 *
 * @since 10.0.0
 */
public abstract sealed class CsrfTokens {

	/** Size in bytes of a token, of a pad, and of the minimum HMAC key. */
	private static final int TOKEN_BYTES = 32;

	/** Length of a masked token on the wire: pad and masked token, hex-encoded. */
	private static final int MASKED_LENGTH = TOKEN_BYTES * 4;

	/**
	 * The session id that a request with no session is HMAC'd under.
	 *
	 * <p>
	 * It contains a NUL character.  A session id travels as a cookie value or URL parameter, and neither can carry
	 * a control character, so no container-issued id can ever equal it and no real session's token can match the
	 * dummy HMAC.
	 */
	private static final String NO_SESSION_ID = "no-session\u0000";

	private static final String HMAC = "HmacSHA256";

	private static final HexFormat HEX = HexFormat.of();

	/** Shared CSPRNG for keys and pads; {@link SecureRandom} is thread-safe. */
	private static final SecureRandom RANDOM = new SecureRandom();

	private CsrfTokens() {}

	/**
	 * A per-session strategy keyed by 32 fresh random bytes.
	 *
	 * <p>
	 * Each call returns a strategy with its own key, so tokens from two instances never cross-verify.  Use
	 * {@link PerSession#key(byte[])} to share one key across a cluster.
	 *
	 * @return A new per-session strategy.
	 */
	public static PerSession perSession() {
		var key = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(key);
		return new PerSession(key);
	}

	/**
	 * A per-process strategy holding one freshly generated token.
	 *
	 * @return A new per-process strategy.
	 */
	public static CsrfTokens perProcess() {
		return perProcess(SynchronizerToken.generate());
	}

	/**
	 * A per-process strategy holding the given token.
	 *
	 * <p>
	 * The token's value must be exactly 64 lower-case hex characters (32 bytes), which is what
	 * {@link SynchronizerToken#generate()} produces.  Any other value is refused rather than stretched or hashed
	 * into shape: a passphrase is not a secret of the required strength, and quietly accepting one would hide that.
	 *
	 * @param token The process token.  Must not be <jk>null</jk>.
	 * @return A new per-process strategy.
	 * @throws IllegalArgumentException If {@code token} is <jk>null</jk> or its value is not 64 lower-case hex
	 * 	characters.
	 */
	public static CsrfTokens perProcess(SynchronizerToken token) {
		reqnn("token", token);
		var v = token.value();
		if (! isLowerHex(v, TOKEN_BYTES * 2))
			throw iaex("Argument 'token' must hold 64 lower-case hex characters, as SynchronizerToken.generate() produces.");
		return new PerProcess(HEX.parseHex(v));
	}

	/**
	 * The expected 32-byte token for this request.
	 *
	 * <p>
	 * Never <jk>null</jk>.  For a per-session strategy with no session, this is the dummy HMAC, which no presented
	 * value can match.
	 *
	 * @param req The request.
	 * @param create Whether a per-session strategy may create a session.  A per-process strategy ignores it and
	 * 	never touches the session.
	 * @return A new array holding the expected token.
	 */
	abstract byte[] expected(HttpServletRequest req, boolean create);

	/**
	 * Masks a token for one embed, under a fresh pad.
	 *
	 * @param token The 32-byte token from {@link #expected(HttpServletRequest, boolean)}.
	 * @return 128 lower-case hex characters, different on every call.
	 * @throws IllegalArgumentException If {@code token} is not 32 bytes.
	 */
	final String mask(byte[] token) {
		reqnn("token", token);
		if (token.length != TOKEN_BYTES)
			throw iaex("Argument 'token' must be %s bytes.", TOKEN_BYTES);
		var pad = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(pad);
		var out = Arrays.copyOf(pad, TOKEN_BYTES * 2);
		for (var i = 0; i < TOKEN_BYTES; i++)
			out[TOKEN_BYTES + i] = (byte)(pad[i] ^ token[i]);
		return HEX.formatHex(out);
	}

	/**
	 * Whether a presented masked value carries the expected token.
	 *
	 * <p>
	 * The shape is checked first and does not depend on the secret.  A malformed value, including <jk>null</jk>,
	 * is <jk>false</jk>, never an exception.
	 *
	 * @param presented The value from the request header.  Can be <jk>null</jk>.
	 * @param expected The 32-byte token from {@link #expected(HttpServletRequest, boolean)}.
	 * @return <jk>true</jk> if {@code presented} unmasks to {@code expected}.
	 */
	final boolean verify(String presented, byte[] expected) {
		if (! isLowerHex(presented, MASKED_LENGTH))
			return false;
		return MessageDigest.isEqual(unmask(HEX.parseHex(presented)), expected);
	}

	/** Reverses {@link #mask(byte[])}: XORs the second half of {@code masked} with the pad in the first half. */
	private static byte[] unmask(byte[] masked) {
		var out = new byte[TOKEN_BYTES];
		for (var i = 0; i < TOKEN_BYTES; i++)
			out[i] = (byte)(masked[i] ^ masked[TOKEN_BYTES + i]);
		return out;
	}

	/** Whether {@code s} is exactly {@code length} characters of lower-case hex. */
	private static boolean isLowerHex(String s, int length) {
		if (s == null || s.length() != length)
			return false;
		for (var i = 0; i < length; i++) {
			var c = s.charAt(i);
			if ((c < '0' || c > '9') && (c < 'a' || c > 'f'))
				return false;
		}
		return true;
	}

	/**
	 * The per-session strategy: the token is <c>HMAC-SHA256(key, UTF-8(sessionId))</c>.
	 *
	 * <p>
	 * Thread-safe.  Only the key bytes are held; a {@link Mac} is created per computation.
	 */
	public static final class PerSession extends CsrfTokens {

		private final byte[] key;

		private PerSession(byte[] key) {
			this.key = key;
		}

		/**
		 * A strategy like this one, keyed by the given bytes.
		 *
		 * <p>
		 * For a cluster, where every node must accept every node's tokens.  The key <b>must</b> be output of a
		 * cryptographically secure random generator, such as 32 bytes from {@link SecureRandom}, kept in a secret
		 * store.  A passphrase or other human-chosen value is not a key, and this method cannot tell the difference.
		 *
		 * <p>
		 * The array is copied, so later changes to it have no effect.  This instance is unchanged.
		 *
		 * @param value The key.  Must not be <jk>null</jk>, and must be at least 32 bytes.
		 * @return A new strategy using {@code value} as its key.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or shorter than 32 bytes.
		 */
		public PerSession key(byte[] value) {
			reqnn("value", value);
			if (value.length < TOKEN_BYTES)
				throw iaex("Argument 'value' must be at least %s bytes.", TOKEN_BYTES);
			return new PerSession(value.clone());
		}

		@Override /* CsrfTokens */
		byte[] expected(HttpServletRequest req, boolean create) {
			var id = sessionId(req, create);
			return hmac(id == null ? NO_SESSION_ID : id);
		}

		private static String sessionId(HttpServletRequest req, boolean create) {
			try {
				var s = req.getSession(create);
				return s == null ? null : s.getId();
			} catch (IllegalStateException e) {
				// The session was invalidated, or the response is committed and a session can no longer be created.
				return null;
			}
		}

		private byte[] hmac(String id) {
			try {
				var mac = Mac.getInstance(HMAC);
				mac.init(new SecretKeySpec(key, HMAC));
				return mac.doFinal(id.getBytes(StandardCharsets.UTF_8));
			} catch (GeneralSecurityException e) {
				throw isex(e, "%s is not available.", HMAC);
			}
		}

		/**
		 * Returns a description that does <b>not</b> include the key.
		 *
		 * @return A key-free description.
		 */
		@Override /* Object */
		public String toString() { return "CsrfTokens.perSession(key=<redacted>)"; }
	}

	/**
	 * The per-process strategy: one 32-byte token for every request.
	 */
	private static final class PerProcess extends CsrfTokens {

		private final byte[] token;

		private PerProcess(byte[] token) {
			this.token = token;
		}

		@Override /* CsrfTokens */
		byte[] expected(HttpServletRequest req, boolean create) {
			return token.clone();
		}

		/**
		 * Returns a description that does <b>not</b> include the token.
		 *
		 * @return A token-free description.
		 */
		@Override /* Object */
		public String toString() { return "CsrfTokens.perProcess(token=<redacted>)"; }
	}
}
