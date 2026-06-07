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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.rest.server.auth.*;

import jakarta.servlet.http.*;

/**
 * {@link AuthFilter} that resolves the OIDC Relying Party session cookie into a {@link ClaimsPrincipal}
 * on each request, so {@code RoleBasedRestGuard} and {@code @Auth Principal} injection see the
 * filter-resolved identity (via {@link AuthenticatedRequestWrapper}) with zero downstream changes.
 *
 * <p>
 * Resolution is fail-open to <i>unauthenticated</i> (not 401): a request with no session cookie, or with
 * an expired / tampered / unknown cookie, returns {@link Optional#empty()} so the chain passes through
 * and the application's normal "not logged in" handling (typically a redirect to the login endpoint)
 * applies.  A valid session returns {@link AuthResult} carrying the session principal and roles.
 *
 * <p>
 * Like all {@link AuthFilter}s, this is usable standalone or composed inside an {@code AuthFilterChain}.
 * Note that Juneau's {@code MockRest} does not execute servlet filters, so integration tests drive
 * {@link #authenticate(HttpServletRequest)} / {@code doFilter} directly.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link OidcRelyingParty}
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OidcRelyingParty">OIDC Relying Party</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class OidcSessionAuthFilter extends AuthFilter {

	private final SessionStore sessionStore;
	private final String cookieName;

	/**
	 * Constructor.
	 *
	 * @param sessionStore The session store to resolve cookies against.  Must not be <jk>null</jk>.
	 * @param cookieName The session cookie name.  Must not be <jk>null</jk> or blank.
	 */
	public OidcSessionAuthFilter(SessionStore sessionStore, String cookieName) {
		this.sessionStore = assertArgNotNull("sessionStore", sessionStore);
		this.cookieName = assertArgNotNullOrBlank("cookieName", cookieName);
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		var cookies = req.getCookies();
		if (cookies == null)
			return opte();
		String value = null;
		for (var c : cookies) {
			if (cookieName.equals(c.getName())) {
				value = c.getValue();
				break;
			}
		}
		if (isBlank(value))
			return opte();
		var session = sessionStore.lookup(value);
		if (session.isEmpty())
			return opte();
		var s = session.get();
		return opt(AuthResult.of(s.principal(), s.roles()));
	}
}
