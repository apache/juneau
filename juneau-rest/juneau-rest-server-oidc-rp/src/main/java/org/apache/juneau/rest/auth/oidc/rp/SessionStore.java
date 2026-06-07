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

import java.util.*;

/**
 * SPI for persisting {@link OidcSession} instances and resolving them back from a session-cookie value.
 *
 * <p>
 * Two implementations are bundled:
 * <ul>
 * 	<li>{@link SignedCookieSessionStore} &mdash; the documented default.  Stateless: the session is
 * 		HMAC-signed and stored entirely in the cookie value.  Scales horizontally and survives restarts
 * 		with no shared store, but <b>cannot be server-revoked</b> (so it does not support back-channel
 * 		logout &mdash; see {@link #supportsServerSideRevocation()}).
 * 	<li>{@link InMemorySessionStore} &mdash; the simpler single-instance / dev option.  Server-side
 * 		indexed by {@code sub} and {@code sid}, so it <b>does</b> support back-channel logout.  Lost on
 * 		restart; breaks under horizontal scaling without sticky sessions.
 * </ul>
 *
 * <p>
 * Distributed stores (Redis / JDBC) are caller-supplied via this SPI.  A distributed store that wants
 * to participate in back-channel logout must implement {@link #invalidateBySubject(String)} /
 * {@link #invalidateBySessionId(String)} and report {@link #supportsServerSideRevocation()} as
 * <jk>true</jk>.
 *
 * <p>
 * Implementations must be thread-safe.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link OidcSession}
 * 	<li class='jc'>{@link InMemorySessionStore}
 * 	<li class='jc'>{@link SignedCookieSessionStore}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OidcRelyingParty">OIDC Relying Party</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface SessionStore {

	/**
	 * Persists the supplied session and returns the opaque value to set as the session cookie.
	 *
	 * <p>
	 * For a server-side store this is typically a random session id used as a lookup key; for a
	 * stateless cookie store it is the entire signed session payload.
	 *
	 * @param session The session to persist.  Must not be <jk>null</jk>.
	 * @return The cookie value the client should send back on subsequent requests.  Never <jk>null</jk>.
	 */
	String createSessionCookieValue(OidcSession session);

	/**
	 * Resolves a session from a cookie value.
	 *
	 * @param cookieValue The cookie value previously returned by {@link #createSessionCookieValue}.
	 * 	Must not be <jk>null</jk>.
	 * @return The resolved session, or {@link Optional#empty()} if absent, expired, invalid, or
	 * 	tampered.
	 */
	Optional<OidcSession> lookup(String cookieValue);

	/**
	 * Invalidates the session identified by the supplied cookie value.  No-op if absent.
	 *
	 * @param cookieValue The cookie value.  Must not be <jk>null</jk>.
	 */
	void invalidate(String cookieValue);

	/**
	 * Returns whether this store can revoke sessions server-side (a prerequisite for back-channel
	 * logout).
	 *
	 * <p>
	 * Defaults to <jk>false</jk> &mdash; stateless cookie stores cannot revoke a session the server
	 * never retained.
	 *
	 * @return <jk>true</jk> if {@link #invalidateBySubject(String)} / {@link #invalidateBySessionId(String)}
	 * 	are supported.
	 */
	default boolean supportsServerSideRevocation() {
		return false;
	}

	/**
	 * Invalidates every session belonging to the supplied subject ({@code sub}).
	 *
	 * <p>
	 * Used by {@link OidcRelyingParty#backChannelLogout} when the IdP-pushed logout token carries a
	 * {@code sub} but no {@code sid}.
	 *
	 * @param subject The subject identifier.  Must not be <jk>null</jk> or blank.
	 * @return The number of sessions invalidated.
	 * @throws UnsupportedOperationException If this store does not support server-side revocation.
	 */
	default int invalidateBySubject(String subject) {
		throw new UnsupportedOperationException("This SessionStore does not support server-side revocation (back-channel logout requires an in-memory or distributed store).");
	}

	/**
	 * Invalidates the session(s) bound to the supplied IdP session id ({@code sid}).
	 *
	 * <p>
	 * Used by {@link OidcRelyingParty#backChannelLogout} when the IdP-pushed logout token carries a
	 * {@code sid}.
	 *
	 * @param sid The IdP session id.  Must not be <jk>null</jk> or blank.
	 * @return The number of sessions invalidated.
	 * @throws UnsupportedOperationException If this store does not support server-side revocation.
	 */
	default int invalidateBySessionId(String sid) {
		throw new UnsupportedOperationException("This SessionStore does not support server-side revocation (back-channel logout requires an in-memory or distributed store).");
	}
}
