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
package org.apache.juneau.rest.server.auth.oidc.rp;

import java.time.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.*;
import org.apache.juneau.rest.server.auth.oauth.*;

/**
 * Immutable record describing a logged-in OpenID Connect Relying Party session.
 *
 * <p>
 * Created by {@link OidcRelyingParty#completeLogin} after a successful login and persisted via a
 * {@link SessionStore}.  The {@link #principal()} carries the verified ID-token claims and is what the
 * {@link OidcSessionAuthFilter} surfaces to {@code RoleBasedRestGuard} / {@code @Auth Principal}.
 *
 * <p>
 * Two fields support server-side (back-channel) revocation: {@link #subject()} (the {@code sub} claim)
 * and {@link #sid()} (the optional {@code sid} session-id claim).  A server-side-indexed
 * {@link SessionStore} keys on these so an IdP-pushed logout token can invalidate the matching
 * session(s).
 *
 * @param id The opaque server-side session id.  For {@link InMemorySessionStore} this is the cache key;
 * 	for {@link SignedCookieSessionStore} it is a stable identifier embedded in the signed cookie.  Never
 * 	{@code null}.
 * @param subject The ID-token {@code sub} claim.  Never {@code null} or blank.
 * @param sid The ID-token {@code sid} (session id) claim, if the IdP issued one.  Used for back-channel
 * 	logout targeting a single session rather than all of a subject's sessions.
 * @param principal The verified ID-token claims as a {@link ClaimsPrincipal}.  Never {@code null}.
 * @param roles The roles extracted from the configured roles claim.  Never {@code null}; may be empty.
 * @param token The tokens returned from the token endpoint (access / refresh / id), if the store retains
 * 	them.  {@link SignedCookieSessionStore} does NOT retain tokens (stateless cookies don't carry them),
 * 	so this is {@link Optional#empty()} for cookie-backed sessions.
 * @param createdAt The instant the session was created.  Never {@code null}.
 * @param expiresAt The instant the session expires (bounded independently of token lifetime).  Never
 * 	{@code null}.
 * @since 10.0.0
 */
public record OidcSession(
		String id,
		String subject,
		Optional<String> sid,
		ClaimsPrincipal principal,
		Set<String> roles,
		Optional<OAuthToken> token,
		Instant createdAt,
		Instant expiresAt) {

	/**
	 * Compact constructor enforcing non-null fields and defensively copying the role set.
	 */
	public OidcSession {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(subject, "subject");
		Objects.requireNonNull(sid, "sid");
		Objects.requireNonNull(principal, "principal");
		Objects.requireNonNull(token, "token");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(expiresAt, "expiresAt");
		roles = roles == null
			? Collections.emptySet()
			: Collections.unmodifiableSet(new LinkedHashSet<>(roles));
	}

	/**
	 * Returns whether this session is expired relative to the supplied instant.
	 *
	 * @param now The reference instant.
	 * @return <jk>true</jk> if {@code now} is at or after {@link #expiresAt()}.
	 */
	public boolean isExpired(Instant now) {
		Objects.requireNonNull(now, "now");
		return !now.isBefore(expiresAt);
	}
}
