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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import jakarta.servlet.http.*;

/**
 * {@link AuthFilter} that authenticates requests via the
 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 Bearer Token</a> scheme.
 *
 * <p>
 * On each request this filter:
 * <ol>
 * 	<li>Reads the {@code Authorization} header.
 * 	<li>If the header is absent or does not start with {@code "Bearer "}, returns {@link Optional#empty()} (filter
 * 		does not apply).
 * 	<li>Extracts the raw token string and delegates to the configured {@link TokenValidator}.
 * 	<li>On success, builds an {@link AuthResult} carrying the returned {@link java.security.Principal} and any roles
 * 		extracted from a {@link ClaimsPrincipal} claim (configurable claim name; default {@code "roles"}).
 * 	<li>On validator failure ({@link AuthenticationException} thrown), re-throws with a
 * 		{@code WWW-Authenticate: Bearer realm="<realm>"} challenge.
 * </ol>
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	AuthFilterChain.<jsm>create</jsm>(<jv>bs</jv>)
 * 		.append(BearerTokenAuthFilter.<jsm>create</jsm>()
 * 			.pattern(<js>"/api/*"</js>)
 * 			.validator(<jv>jwtValidator</jv>)
 * 			.build())
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TokenValidator}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 &mdash; Bearer Token Usage</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names and bearer token format strings; intentional
})
public class BearerTokenAuthFilter extends AuthFilter {

	private static final String DEFAULT_ROLES_CLAIM = "roles";

	/**
	 * Static creator.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class.
	 */
	public static class Builder {

		private TokenValidator validator;
		private String realm = "api";
		private String rolesClaim = DEFAULT_ROLES_CLAIM;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the token validator.
		 *
		 * @param value The validator. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(TokenValidator value) {
			validator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the {@code WWW-Authenticate: Bearer realm="<value>"} challenge realm.
		 *
		 * @param value The realm. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder realm(String value) {
			realm = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the {@link ClaimsPrincipal} claim name used to extract roles.
		 *
		 * <p>
		 * Defaults to {@code "roles"}.  The claim value must be a {@link List} of {@link String} role names
		 * for the roles to be populated in {@link AuthResult#getRoles()}.
		 *
		 * @param value The claim name. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder rolesClaim(String value) {
			rolesClaim = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Builds the filter.
		 *
		 * @return A new {@link BearerTokenAuthFilter}.
		 */
		public BearerTokenAuthFilter build() {
			if (validator == null)
				throw new IllegalStateException("BearerTokenAuthFilter requires a TokenValidator");
			return new BearerTokenAuthFilter(this);
		}
	}

	private final TokenValidator validator;
	private final String challenge;
	private final String rolesClaim;

	/**
	 * Constructor.
	 *
	 * @param builder The builder to read configuration from. Must not be <jk>null</jk>.
	 */
	protected BearerTokenAuthFilter(Builder builder) {
		this.validator = builder.validator;
		this.challenge = "Bearer realm=\"" + builder.realm + "\"";
		this.rolesClaim = builder.rolesClaim;
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		var header = req.getHeader(BearerTokenExtractor.AUTHORIZATION);
		if (isBlank(header))
			return oe();
		var optToken = BearerTokenExtractor.extract(header);
		if (optToken.isEmpty())
			return oe();
		var principal = validateToken(optToken.get());
		var roles = ClaimsRoleExtractor.extractRoles(principal, rolesClaim);
		return o(AuthResult.of(principal, roles));
	}

	private java.security.Principal validateToken(String token) throws AuthenticationException {
		try {
			var p = validator.validate(token);
			if (p == null)
				throw new AuthenticationException("Token validation returned no principal").wwwAuthenticate(challenge);
			return p;
		} catch (AuthenticationException e) {
			// Preserve richer challenge from the validator if present; otherwise stamp our basic one.
			var hasChallenge = e.getHeaders().stream()
				.anyMatch(h -> WWW_AUTHENTICATE.equalsIgnoreCase(h.getName()));
			if (!hasChallenge)
				e.wwwAuthenticate(challenge);
			throw e;
		} catch (RuntimeException e) {
			throw new AuthenticationException(e, "Token validation failed").wwwAuthenticate(challenge);
		}
	}
}
