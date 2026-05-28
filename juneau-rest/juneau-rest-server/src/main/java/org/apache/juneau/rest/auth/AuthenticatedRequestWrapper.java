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
package org.apache.juneau.rest.auth;

import java.security.*;
import java.util.*;

import org.apache.juneau.rest.*;

import jakarta.servlet.http.*;

/**
 * An {@link HttpServletRequestWrapper} that surfaces the filter-resolved {@link Principal} and aggregated roles via the
 * standard servlet API surface.
 *
 * <p>
 * Created by {@link AuthFilter#doFilter} (standalone mode) and {@link AuthFilterChain#doFilter} (chain mode) after
 * successful authentication. Overrides three servlet-API methods so downstream code &mdash; including
 * {@link org.apache.juneau.rest.guard.RoleBasedRestGuard} and the {@link AuthArg}
 * arg-resolver &mdash; sees the filter-resolved identity without any extra wiring:
 * <ul>
 * 	<li>{@link #getUserPrincipal()} &mdash; returns the authenticated principal.
 * 	<li>{@link #isUserInRole(String)} &mdash; checks the aggregated role set.
 * 	<li>{@link #getRemoteUser()} &mdash; returns {@link Principal#getName()}.
 * 	<li>{@link #getAttribute(String)} &mdash; for the key {@link RestServerConstants#PRINCIPAL_ATTR}, returns the
 * 		principal so the {@code @Auth} arg-resolver can find it.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class AuthenticatedRequestWrapper extends HttpServletRequestWrapper {

	private final Principal principal;
	private final Set<String> roles;

	/**
	 * Constructor.
	 *
	 * @param req The underlying request. Must not be <jk>null</jk>.
	 * @param result The authentication result. Must not be <jk>null</jk>.
	 */
	public AuthenticatedRequestWrapper(HttpServletRequest req, AuthResult result) {
		super(req);
		this.principal = result.getPrincipal();
		this.roles = new HashSet<>(result.getRoles());
	}

	/**
	 * Adds additional roles to the aggregated role set.
	 *
	 * <p>
	 * Called by {@link AuthFilterChain} when subsequent filters succeed and contribute additional roles.
	 *
	 * @param moreRoles The additional roles to add.
	 * @return This object (for chaining in the chain orchestrator).
	 */
	AuthenticatedRequestWrapper withAdditionalRoles(Set<String> moreRoles) {
		roles.addAll(moreRoles);
		return this;
	}

	@Override /* Overridden from HttpServletRequest */
	public Principal getUserPrincipal() {
		return principal;
	}

	@Override /* Overridden from HttpServletRequest */
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

	@Override /* Overridden from HttpServletRequest */
	public String getRemoteUser() {
		return principal.getName();
	}

	/**
	 * Intercepts {@link RestServerConstants#PRINCIPAL_ATTR} so the {@code @Auth} arg-resolver can pick up the
	 * filter-stashed principal without any extra wiring.
	 *
	 * <p>
	 * All other attribute names delegate to the wrapped request.
	 */
	@Override /* Overridden from HttpServletRequest */
	public Object getAttribute(String name) {
		if (RestServerConstants.PRINCIPAL_ATTR.equals(name))
			return principal;
		return super.getAttribute(name);
	}
}
