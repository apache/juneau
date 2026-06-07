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
package org.apache.juneau.rest.auth.oidc.rp;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.nio.charset.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.rest.auth.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

/**
 * Stateless {@link SessionStore} that serializes the session into an HMAC-signed compact JWT carried
 * entirely in the session cookie.  This is the documented default.
 *
 * <p>
 * Because the session lives client-side, this store scales horizontally and survives server restarts
 * with no shared store.  The trade-offs:
 * <ul>
 * 	<li><b>No server-side revocation</b> &mdash; {@link #supportsServerSideRevocation()} is
 * 		<jk>false</jk>; a stateless cookie cannot be force-revoked, so this store does NOT support
 * 		back-channel logout.  Use {@link InMemorySessionStore} or a caller-supplied distributed store
 * 		if back-channel logout is required.
 * 	<li><b>Size cap</b> &mdash; cookies are capped (~4&nbsp;KB); only minimal claims are stored, never
 * 		the raw access / refresh / ID tokens.  An over-cap payload throws rather than silently truncating.
 * 	<li><b>Key rotation</b> &mdash; rotating the signing key invalidates all live sessions (users are
 * 		bounced to re-login).
 * </ul>
 *
 * <p>
 * The payload is signed with {@code HS256} using the configured signing key (which must be at least 256
 * bits / 32 bytes).  Signature failures, expiry, and parse errors all resolve to
 * {@link Optional#empty()} on {@link #lookup(String)} (fail-closed).
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are cookie attribute names and session field names; intentional
})
public class SignedCookieSessionStore implements SessionStore {

	/** Default maximum serialized cookie size in bytes. */
	public static final int DEFAULT_MAX_COOKIE_BYTES = 4096;

	private static final String CLAIM_NAME = "n";
	private static final String CLAIM_ROLES = "r";
	private static final String CLAIM_SID = "sid";
	private static final String CLAIM_CLAIMS = "c";

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder.
	 */
	public static class Builder {
		private byte[] signingKey;
		private int maxCookieBytes = DEFAULT_MAX_COOKIE_BYTES;
		private Clock clock = Clock.systemUTC();

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the HMAC signing key from a string (UTF-8 encoded).  Required.
		 *
		 * @param value The signing key.  Must be at least 32 bytes (256 bits) when UTF-8 encoded.
		 * @return This object.
		 */
		public Builder signingKey(String value) {
			assertArgNotNullOrBlank("value", value);
			return signingKey(value.getBytes(StandardCharsets.UTF_8));
		}

		/**
		 * Sets the HMAC signing key bytes.  Required.
		 *
		 * @param value The signing key.  Must be at least 32 bytes (256 bits).
		 * @return This object.
		 */
		public Builder signingKey(byte[] value) {
			assertArgNotNull("value", value);
			assertArg(value.length >= 32, "signingKey must be at least 32 bytes (256 bits) for HS256; was {0}", value.length);
			signingKey = value.clone();
			return this;
		}

		/**
		 * Sets the maximum serialized cookie size in bytes.
		 *
		 * @param value The cap.  Must be positive.
		 * @return This object.
		 */
		public Builder maxCookieBytes(int value) {
			assertArg(value > 0, "maxCookieBytes must be positive (was {0})", value);
			maxCookieBytes = value;
			return this;
		}

		/**
		 * Overrides the clock used for expiry comparisons.  Useful in tests.
		 *
		 * @param value The clock.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clock(Clock value) {
			clock = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the store.
		 *
		 * @return A new {@link SignedCookieSessionStore}.
		 */
		public SignedCookieSessionStore build() {
			if (signingKey == null)
				throw new IllegalStateException("SignedCookieSessionStore requires signingKey(...)");
			return new SignedCookieSessionStore(this);
		}
	}

	private final byte[] signingKey;
	private final int maxCookieBytes;
	private final Clock clock;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected SignedCookieSessionStore(Builder b) {
		this.signingKey = b.signingKey;
		this.maxCookieBytes = b.maxCookieBytes;
		this.clock = b.clock;
	}

	@Override /* Overridden from SessionStore */
	public String createSessionCookieValue(OidcSession session) {
		assertArgNotNull("session", session);
		try {
			var cb = new JWTClaimsSet.Builder()
				.subject(session.subject())
				.jwtID(session.id())
				.issueTime(Date.from(session.createdAt()))
				.expirationTime(Date.from(session.expiresAt()))
				.claim(CLAIM_NAME, session.principal().getName())
				.claim(CLAIM_ROLES, new ArrayList<>(session.roles()))
				.claim(CLAIM_CLAIMS, session.principal().getClaims());
			session.sid().ifPresent(sid -> cb.claim(CLAIM_SID, sid));
			var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), cb.build());
			jwt.sign(new MACSigner(signingKey));
			var serialized = jwt.serialize();
			if (serialized.getBytes(StandardCharsets.UTF_8).length > maxCookieBytes)
				throw new IllegalStateException("Signed session cookie exceeds maxCookieBytes (" + maxCookieBytes
					+ "); reduce the claim set or switch to a server-side SessionStore.");
			return serialized;
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to sign session cookie", e);
		}
	}

	@Override /* Overridden from SessionStore */
	@SuppressWarnings({
		"unchecked" // Type erasure on reflective/generic cast; element type is verified at call site
	})
	public Optional<OidcSession> lookup(String cookieValue) {
		assertArgNotNull("cookieValue", cookieValue);
		try {
			var jwt = SignedJWT.parse(cookieValue);
			if (! jwt.verify(new MACVerifier(signingKey)))
				return opte();
			var claims = jwt.getJWTClaimsSet();
			var exp = claims.getExpirationTime();
			if (exp == null || ! clock.instant().isBefore(exp.toInstant()))
				return opte();
			var subject = claims.getSubject();
			var name = claims.getStringClaim(CLAIM_NAME);
			if (subject == null || name == null)
				return opte();
			var roles = new LinkedHashSet<String>();
			var rolesClaim = claims.getStringListClaim(CLAIM_ROLES);
			if (rolesClaim != null)
				roles.addAll(rolesClaim);
			Map<String,Object> principalClaims = (Map<String,Object>) claims.getClaim(CLAIM_CLAIMS);
			var principal = new ClaimsPrincipal(name, principalClaims);
			var sid = opt(claims.getStringClaim(CLAIM_SID));
			var createdAt = claims.getIssueTime() != null ? claims.getIssueTime().toInstant() : clock.instant();
			return opt(new OidcSession(
				claims.getJWTID(),
				subject,
				sid,
				principal,
				roles,
				opte(),
				createdAt,
				exp.toInstant()));
		} catch (java.text.ParseException | JOSEException | RuntimeException e) {
			return opte();
		}
	}

	@Override /* Overridden from SessionStore */
	public void invalidate(String cookieValue) {
		// Stateless: nothing to remove server-side.  The caller clears the cookie on the response.
	}

	@Override /* Overridden from SessionStore */
	public boolean supportsServerSideRevocation() {
		return false;
	}
}
