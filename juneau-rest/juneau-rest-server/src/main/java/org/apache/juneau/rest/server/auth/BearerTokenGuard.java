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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.security.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;

/**
 * AuthN {@link RestGuard} that authenticates requests via the
 * <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 Bearer Token</a> scheme.
 *
 * <p>
 * On every request, the guard:
 * <ol>
 * 	<li>Reads the {@code Authorization} request header.
 * 	<li>Confirms it starts with {@code "Bearer "} (case-insensitive) followed by a non-blank token.
 * 	<li>Delegates the raw token to the configured {@link TokenValidator}.
 * 	<li>On success, stashes the returned {@link Principal} on the request attributes under
 * 		{@link RestServerConstants#PRINCIPAL_ATTR} so downstream handlers can read it via
 * 		{@link AuthArg @Auth Principal} or {@code req.getAttributes().get(PRINCIPAL_ATTR)}.
 * 	<li>On any failure path (missing header, malformed scheme, validator throws), throws
 * 		{@link AuthenticationException} with a {@code WWW-Authenticate: Bearer realm="<realm>"} response
 * 		header set per RFC 7235 &sect;4.1.
 * </ol>
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet {
 *
 * 		<ja>@Bean</ja>(name=<js>"guards"</js>)
 * 		<jk>public</jk> RestGuardList guards(BeanStore <jv>bs</jv>) {
 * 			<jk>return</jk> RestGuardList.<jsm>create</jsm>(<jv>bs</jv>)
 * 				.append(
 * 					BearerTokenGuard.<jsm>create</jsm>()
 * 						.realm(<js>"api"</js>)
 * 						.validator(<jv>myTokenValidator</jv>)
 * 						.build())
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Security notes</h5>
 *
 * <ul>
 * 	<li>Bearer tokens are <b>credentials</b> &mdash; never log the full token, never echo it in a
 * 		response body, and always pair with TLS in production.
 * 	<li>{@link TokenValidator}s should run in constant time when possible (in particular, opaque-token
 * 		lookups should use constant-time comparison to defeat timing side-channels).
 * 	<li>For JWT verification, use the {@code JwtTokenValidator} shipped in the optional
 * 		{@code juneau-rest-server-auth-jwt} sub-module &mdash; it enforces algorithm allowlisting and
 * 		mandatory {@code iss} / {@code aud} / {@code exp} / {@code nbf} checks by default.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TokenValidator}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='jc'>{@link Auth}
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6750">RFC 6750 &mdash; Bearer Token Usage</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class BearerTokenGuard extends RestGuard {

	/** WWW-Authenticate response header name (RFC 7235 §4.1). */
	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

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

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the token validator the guard delegates to.
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
		 * <p>
		 * The realm string communicates to the client which protection space the credentials apply to.
		 * Defaults to {@code "api"} when not set.
		 *
		 * @param value The realm. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder realm(String value) {
			realm = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Builds the guard.
		 *
		 * @return A new {@link BearerTokenGuard}.
		 */
		public BearerTokenGuard build() {
			if (validator == null)
				throw new IllegalStateException("BearerTokenGuard requires a TokenValidator");
			return new BearerTokenGuard(this);
		}
	}

	private final TokenValidator validator;
	private final String realm;
	private final String challenge;

	/**
	 * Constructor.
	 *
	 * @param builder The builder to read configuration from.
	 */
	protected BearerTokenGuard(Builder builder) {
		this.validator = builder.validator;
		this.realm = builder.realm;
		this.challenge = "Bearer realm=\"" + realm + "\"";
	}

	/**
	 * Convenience constructor that uses the default realm ({@code "api"}).
	 *
	 * @param validator The token validator. Must not be <jk>null</jk>.
	 */
	public BearerTokenGuard(TokenValidator validator) {
		this(create().validator(validator));
	}

	@Override /* Overridden from RestGuard */
	public boolean guard(RestRequest req, RestResponse res) {
		var header = req.getHeader(BearerTokenExtractor.AUTHORIZATION);
		if (isBlank(header)) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw missing();
		}
		var optToken = BearerTokenExtractor.extract(header);
		if (optToken.isEmpty()) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw malformed();
		}
		var token = optToken.get();
		Principal p;
		try {
			p = validator.validate(token);
		} catch (AuthenticationException e) {
			// Preserve any richer challenge the validator already set on the exception; otherwise stamp our basic one.
			var override = e.getHeaders().stream()
				.filter(h -> WWW_AUTHENTICATE.equalsIgnoreCase(h.getName()))
				.map(h -> h.getValue())
				.findFirst()
				.orElse(challenge);
			res.setHeader(WWW_AUTHENTICATE, override);
			throw e;
		} catch (RuntimeException e) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw new AuthenticationException(e, "Token validation failed").wwwAuthenticate(challenge);
		}
		if (p == null) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw new AuthenticationException("Token validation returned no principal").wwwAuthenticate(challenge);
		}
		req.getAttributes().set(RestServerConstants.PRINCIPAL_ATTR, p);
		return true;
	}

	@Override /* Overridden from RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		// Not used: this guard overrides guard(req, res) directly because it needs to mutate request
		// attributes and set the WWW-Authenticate challenge on the rejection path.
		return false;
	}

	private AuthenticationException missing() {
		return new AuthenticationException("Authorization header missing").wwwAuthenticate(challenge);
	}

	private AuthenticationException malformed() {
		return new AuthenticationException("Authorization header malformed").wwwAuthenticate(challenge);
	}
}
