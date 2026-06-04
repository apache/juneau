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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.security.*;
import java.util.*;

/**
 * Immutable value type returned by {@link AuthFilter#authenticate(jakarta.servlet.http.HttpServletRequest)} on success.
 *
 * <p>
 * Carries the resolved {@link Principal} and the set of roles associated with the authenticated subject.
 * The role set may be empty when no role claims are available (the principal is still valid).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class AuthResult {

	private final Principal principal;
	private final Set<String> roles;

	/**
	 * Creates an {@link AuthResult} with the specified principal and roles.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult of(Principal principal, String... roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, roles == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(roles)));
	}

	/**
	 * Creates an {@link AuthResult} with the specified principal and role set.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be <jk>null</jk> or empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult of(Principal principal, Set<String> roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, roles == null ? Collections.emptySet() : new HashSet<>(roles));
	}

	private AuthResult(Principal principal, Set<String> roles) {
		this.principal = principal;
		this.roles = Collections.unmodifiableSet(roles);
	}

	/**
	 * Returns the authenticated principal.
	 *
	 * @return The authenticated principal. Never <jk>null</jk>.
	 */
	public Principal getPrincipal() {
		return principal;
	}

	/**
	 * Returns the unmodifiable set of roles granted to this principal.
	 *
	 * @return The role set. Never <jk>null</jk>; may be empty.
	 */
	public Set<String> getRoles() {
		return roles;
	}
}
