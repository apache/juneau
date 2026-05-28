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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.rest.*;

import jakarta.servlet.http.*;

/**
 * {@link AuthFilter} that authenticates requests via a raw API-key string.
 *
 * <p>
 * On each request this filter:
 * <ol>
 * 	<li>Extracts the key from the configured source &mdash; header (default {@code X-API-Key}), query parameter, or
 * 		cookie.
 * 	<li>If the key is absent (blank), returns {@link Optional#empty()} (filter does not apply).
 * 	<li>Delegates to the configured {@link ApiKeyStore}.
 * 	<li>On success ({@link Optional#of(Object) Optional.of(Principal)} returned), builds an {@link AuthResult}
 * 		carrying the {@link java.security.Principal} and any roles extracted from a {@link ClaimsPrincipal} claim.
 * 	<li>On lookup miss ({@link Optional#empty()} returned by the store), throws {@link AuthenticationException}.
 * </ol>
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	AuthFilterChain.<jsm>create</jsm>(<jv>bs</jv>)
 * 		.append(ApiKeyAuthFilter.<jsm>create</jsm>()
 * 			.pattern(<js>"/api/*"</js>)
 * 			.store(<jv>apiKeyStore</jv>)
 * 			.build())
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ApiKeyStore}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='jc'>{@link AuthFilter}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class ApiKeyAuthFilter extends AuthFilter {

	/** Source of the API-key string. */
	public enum Source {
		/** Read from a request header (default; default header name is {@code X-API-Key}). */
		HEADER,
		/** Read from a query parameter. */
		QUERY,
		/** Read from a cookie. */
		COOKIE
	}

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

		private ApiKeyStore store;
		private Source source = Source.HEADER;
		private String name = RestServerConstants.API_KEY_HEADER;
		private String realm = "api";
		private String rolesClaim = DEFAULT_ROLES_CLAIM;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the API-key store.
		 *
		 * @param value The store. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder store(ApiKeyStore value) {
			store = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Reads the key from the supplied request header.
		 *
		 * <p>
		 * Defaults to {@link RestServerConstants#API_KEY_HEADER} ({@code "X-API-Key"}) when not set.
		 *
		 * @param value The header name. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder fromHeader(String value) {
			source = Source.HEADER;
			name = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Reads the key from the supplied query parameter.
		 *
		 * @param value The query parameter name. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder fromQuery(String value) {
			source = Source.QUERY;
			name = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Reads the key from the supplied cookie.
		 *
		 * @param value The cookie name. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder fromCookie(String value) {
			source = Source.COOKIE;
			name = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the {@code WWW-Authenticate: ApiKey realm="<value>"} challenge realm.
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
		 * Defaults to {@code "roles"}.
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
		 * @return A new {@link ApiKeyAuthFilter}.
		 */
		public ApiKeyAuthFilter build() {
			if (store == null)
				throw new IllegalStateException("ApiKeyAuthFilter requires an ApiKeyStore");
			return new ApiKeyAuthFilter(this);
		}
	}

	private final ApiKeyStore store;
	private final Source source;
	private final String name;
	private final String challenge;
	private final String rolesClaim;

	/**
	 * Constructor.
	 *
	 * @param builder The builder to read configuration from.
	 */
	protected ApiKeyAuthFilter(Builder builder) {
		this.store = builder.store;
		this.source = builder.source;
		this.name = builder.name;
		this.challenge = "ApiKey realm=\"" + builder.realm + "\"";
		this.rolesClaim = builder.rolesClaim;
	}

	@Override /* Overridden from AuthFilter */
	public Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException {
		var key = readKey(req);
		if (isBlank(key))
			return Optional.empty();

		java.security.Principal principal;
		try {
			var result = store.lookup(key);
			if (result.isEmpty())
				throw new AuthenticationException("API key not recognized").wwwAuthenticate(challenge);
			principal = result.get();
		} catch (AuthenticationException e) {
			var hasChallenge = e.getHeaders().stream()
				.anyMatch(h -> WWW_AUTHENTICATE.equalsIgnoreCase(h.getName()));
			if (!hasChallenge)
				e.wwwAuthenticate(challenge);
			throw e;
		} catch (RuntimeException e) {
			throw new AuthenticationException(e, "API key lookup failed").wwwAuthenticate(challenge);
		}

		var roles = ClaimsRoleExtractor.extractRoles(principal, rolesClaim);
		return Optional.of(AuthResult.of(principal, roles));
	}

	private String readKey(HttpServletRequest req) {
		return switch (source) {
			case HEADER -> req.getHeader(name);
			case QUERY -> req.getParameter(name);
			case COOKIE -> ApiKeyExtractor.readCookie(req, name);
		};
	}
}
