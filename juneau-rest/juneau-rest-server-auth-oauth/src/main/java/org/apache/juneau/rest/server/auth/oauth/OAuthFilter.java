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
package org.apache.juneau.rest.server.auth.oauth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.*;

import jakarta.servlet.http.*;

/**
 * {@link AuthFilter} that authenticates requests carrying an OAuth 2.0 / OIDC access token via the
 * standard <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 Bearer Token</a>
 * scheme.
 *
 * <p>
 * Mirrors {@code BearerTokenAuthFilter}'s shape but routes through an OAuth-flavored {@link TokenValidator}:
 *
 * <ul>
 * 	<li>{@link OAuthIntrospectionValidator} for opaque tokens (RFC 7662).
 * 	<li>{@code JwtTokenValidator} (from {@code juneau-rest-server-auth-jwt}) for JWT access tokens (the
 * 		OIDC-default case).
 * </ul>
 *
 * <p>
 * Roles are extracted from a configurable claim (default: {@code "scope"} &mdash; values are split on
 * whitespace per RFC 6749 &sect;3.3).
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	AuthFilterChain.<jsm>create</jsm>(<jv>bs</jv>)
 * 		.append(OAuthFilter.<jsm>create</jsm>()
 * 			.pattern(<js>"/api/*"</js>)
 * 			.validator(<jv>introspectionValidator</jv>)
 * 			.build())
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link OAuthIntrospectionValidator}
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 &mdash; Bearer Token Usage</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OAuth protocol header names and grant type values; intentional
})
public class OAuthFilter extends AuthFilter {

	private static final String DEFAULT_ROLES_CLAIM = "scope";
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

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
		private TokenValidator validator;
		private String realm = "api";
		private String rolesClaim = DEFAULT_ROLES_CLAIM;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the {@link TokenValidator}.  Required.
		 *
		 * @param value The validator.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(TokenValidator value) {
			validator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the {@code WWW-Authenticate: Bearer realm="<value>"} challenge realm.
		 *
		 * @param value The realm.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder realm(String value) {
			realm = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the claim used to extract roles.  Defaults to {@code "scope"}; values are split on
		 * whitespace per RFC 6749 &sect;3.3.
		 *
		 * @param value The claim name.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder rolesClaim(String value) {
			rolesClaim = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Builds the filter.
		 *
		 * @return A new {@link OAuthFilter}.
		 */
		public OAuthFilter build() {
			if (validator == null)
				throw new IllegalStateException("OAuthFilter requires a TokenValidator");
			return new OAuthFilter(this);
		}
	}

	private final TokenValidator validator;
	private final String challenge;
	private final String rolesClaim;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthFilter(Builder b) {
		this.validator = b.validator;
		this.challenge = "Bearer realm=\"" + b.realm + "\"";
		this.rolesClaim = b.rolesClaim;
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		var header = req.getHeader(AUTHORIZATION);
		if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()))
			return oe();
		var token = header.substring(BEARER_PREFIX.length()).trim();
		if (token.isEmpty())
			return oe();
		var principal = validate(token);
		return o(AuthResult.of(principal, extractRoles(principal)));
	}

	private Principal validate(String token) throws AuthenticationException {
		try {
			var p = validator.validate(token);
			if (p == null)
				throw new AuthenticationException("Token validator returned null").wwwAuthenticate(challenge);
			return p;
		} catch (AuthenticationException e) {
			var hasChallenge = e.getHeaders().stream()
				.anyMatch(h -> "WWW-Authenticate".equalsIgnoreCase(h.getName()));
			if (!hasChallenge)
				e.wwwAuthenticate(challenge);
			throw e;
		} catch (RuntimeException e) {
			throw new AuthenticationException(e, "OAuth token validation failed").wwwAuthenticate(challenge);
		}
	}

	private Set<String> extractRoles(Principal principal) {
		if (!(principal instanceof ClaimsPrincipal cp))
			return Collections.emptySet();
		var v = cp.getClaims().get(rolesClaim);
		if (v == null)
			return Collections.emptySet();
		var out = new HashSet<String>();
		if (v instanceof String s) {
			for (var piece : s.split("\\s+"))
				if (!piece.isBlank())
					out.add(piece);
		} else if (v instanceof Collection<?> list) {
			for (var item : list)
				if (item instanceof String s)
					out.add(s);
		}
		return out;
	}
}
