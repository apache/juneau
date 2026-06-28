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
package org.apache.juneau.rest.server.auth;

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
@SuppressWarnings({
	"java:S1192" // Duplicated "principal" literal is an assertion parameter name; a constant would obscure rather than clarify.
})
public final class AuthResult {

	/**
	 * Controls how an {@link AuthResult} merges with results accumulated earlier in an authenticator chain.
	 *
	 * @since 10.0.0
	 */
	public enum MergeMode {
		/** Union this result's roles into the accumulated set; principal is optional (<jk>null</jk> keeps the inherited principal). */
		ADD,
		/** Discard everything accumulated so far; this result's principal + roles win.  Principal is required. */
		REPLACE
	}

	private final Principal principal;   // may be null only in ADD mode
	private final Set<String> roles;
	private final MergeMode mode;

	/**
	 * Creates an {@link AuthResult} with the specified principal and roles using {@link MergeMode#ADD} mode.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult of(Principal principal, String... roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, toSet(roles), MergeMode.ADD);
	}

	/**
	 * Creates an {@link AuthResult} with the specified principal and role set using {@link MergeMode#ADD} mode.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be <jk>null</jk> or empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult of(Principal principal, Set<String> roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, toSet(roles), MergeMode.ADD);
	}

	/**
	 * Creates a roles-only {@link AuthResult} ({@link MergeMode#ADD} mode, <jk>null</jk> principal).
	 *
	 * <p>
	 * Use this to contribute additional roles without supplying a principal &mdash; the accumulated/inherited
	 * principal is kept.  This replaces the need for a separate roles-only authenticator type.
	 *
	 * @param roles The roles to contribute. May be empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult ofRoles(String... roles) {
		return new AuthResult(null, toSet(roles), MergeMode.ADD);
	}

	/**
	 * Creates a roles-only {@link AuthResult} ({@link MergeMode#ADD} mode, <jk>null</jk> principal).
	 *
	 * @param roles The roles to contribute. May be <jk>null</jk> or empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult ofRoles(Collection<String> roles) {
		return new AuthResult(null, toSet(roles), MergeMode.ADD);
	}

	/**
	 * Creates an {@link AuthResult} that {@link MergeMode#REPLACE replaces} any accumulated identity and roles.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult replacing(Principal principal, String... roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, toSet(roles), MergeMode.REPLACE);
	}

	/**
	 * Creates an {@link AuthResult} that {@link MergeMode#REPLACE replaces} any accumulated identity and roles.
	 *
	 * @param principal The authenticated principal. Must not be <jk>null</jk>.
	 * @param roles The roles granted to this principal. May be <jk>null</jk> or empty.
	 * @return A new {@link AuthResult}.
	 */
	public static AuthResult replacing(Principal principal, Set<String> roles) {
		assertArgNotNull("principal", principal);
		return new AuthResult(principal, toSet(roles), MergeMode.REPLACE);
	}

	private static Set<String> toSet(String... roles) {
		return roles == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(roles));
	}

	private static Set<String> toSet(Collection<String> roles) {
		return roles == null ? Collections.emptySet() : new HashSet<>(roles);
	}

	private AuthResult(Principal principal, Set<String> roles, MergeMode mode) {
		this.principal = principal;
		this.roles = Collections.unmodifiableSet(roles);
		this.mode = mode;
	}

	/**
	 * Returns the authenticated principal.
	 *
	 * @return The authenticated principal.  May be <jk>null</jk> for a roles-only ({@link MergeMode#ADD}) result.
	 */
	public Principal getPrincipal() {
		return principal;
	}

	/**
	 * Returns the merge mode for this result.
	 *
	 * @return The merge mode. Never <jk>null</jk>.
	 * @since 10.0.0
	 */
	public MergeMode getMode() {
		return mode;
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
