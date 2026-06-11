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
 * AuthN {@link RestGuard} that authenticates requests via a raw API-key string.
 *
 * <p>
 * On every request, the guard:
 * <ol>
 * 	<li>Extracts the key from the configured {@linkplain Source source} &mdash; header (default
 * 		{@code X-API-Key}), query parameter, or cookie.
 * 	<li>Delegates lookup to the configured {@link ApiKeyStore}.
 * 	<li>On success, stashes the returned {@link Principal} on the request attributes under
 * 		{@link RestServerConstants#PRINCIPAL_ATTR}.
 * 	<li>On any failure path (missing key, unknown key), throws {@link AuthenticationException}
 * 		with a {@code WWW-Authenticate: ApiKey realm="<realm>"} response header.
 * </ol>
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jc>// Default source: X-API-Key header.</jc>
 * 	ApiKeyGuard.<jsm>create</jsm>().store(<jv>store</jv>).build();
 *
 * 	<jc>// Custom header.</jc>
 * 	ApiKeyGuard.<jsm>create</jsm>().store(<jv>store</jv>).fromHeader(<js>"X-Acme-Token"</js>).build();
 *
 * 	<jc>// Query-param source (handy for browser clients that can't set custom headers).</jc>
 * 	ApiKeyGuard.<jsm>create</jsm>().store(<jv>store</jv>).fromQuery(<js>"apiKey"</js>).build();
 *
 * 	<jc>// Cookie source.</jc>
 * 	ApiKeyGuard.<jsm>create</jsm>().store(<jv>store</jv>).fromCookie(<js>"api_key"</js>).build();
 * </p>
 *
 * <h5 class='topic'>Security notes</h5>
 *
 * <ul>
 * 	<li>API keys are <b>credentials</b>; never log them, always pair with TLS.
 * 	<li>Query-string keys leak into proxy / access logs and browser history &mdash; prefer header or
 * 		cookie sources for production traffic. Query-string is offered for legacy / debugging use only.
 * 	<li>{@link ApiKeyStore} implementations should compare keys in constant time
 * 		({@link java.security.MessageDigest#isEqual(byte[], byte[])}).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ApiKeyStore}
 * 	<li class='jc'>{@link AuthenticationException}
 * 	<li class='jc'>{@link Auth}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerAuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names and API key parameter names; intentional
})
public class ApiKeyGuard extends RestGuard {

	/** WWW-Authenticate response header name (RFC 7235 §4.1). */
	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	/** Source of the API-key string. */
	public enum Source {
		/** Read from a request header (default; default header name is {@code X-API-Key}). */
		HEADER,
		/** Read from a query parameter. */
		QUERY,
		/** Read from a cookie. */
		COOKIE
	}

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

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the API-key store the guard delegates to.
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
		 * Calls equivalent to {@code source(Source.HEADER)} plus {@code name(value)}. Defaults to
		 * {@link RestServerConstants#API_KEY_HEADER} ({@code "X-API-Key"}) when not set.
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
		 * Builds the guard.
		 *
		 * @return A new {@link ApiKeyGuard}.
		 */
		public ApiKeyGuard build() {
			if (store == null)
				throw new IllegalStateException("ApiKeyGuard requires an ApiKeyStore");
			return new ApiKeyGuard(this);
		}
	}

	private final ApiKeyStore store;
	private final Source source;
	private final String name;
	private final String challenge;

	/**
	 * Constructor.
	 *
	 * @param builder The builder to read configuration from.
	 */
	protected ApiKeyGuard(Builder builder) {
		this.store = builder.store;
		this.source = builder.source;
		this.name = builder.name;
		this.challenge = "ApiKey realm=\"" + builder.realm + "\"";
	}

	/**
	 * Convenience constructor: header source with default name {@code "X-API-Key"} and default realm.
	 *
	 * @param store The API-key store. Must not be <jk>null</jk>.
	 */
	public ApiKeyGuard(ApiKeyStore store) {
		this(create().store(store));
	}

	@Override /* Overridden from RestGuard */
	public boolean guard(RestRequest req, RestResponse res) {
		var key = readKey(req);
		if (isBlank(key)) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw missing();
		}
		Principal p;
		try {
			var result = store.lookup(key);
			if (result.isEmpty()) {
				res.setHeader(WWW_AUTHENTICATE, challenge);
				throw unknown();
			}
			p = result.get();
		} catch (AuthenticationException e) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw e;
		} catch (RuntimeException e) {
			res.setHeader(WWW_AUTHENTICATE, challenge);
			throw new AuthenticationException(e, "API key lookup failed").wwwAuthenticate(challenge);
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

	private String readKey(RestRequest req) {
		return switch (source) {
			case HEADER -> req.getHeader(name);
			case QUERY -> req.getQueryParam(name).orElse(null);
			case COOKIE -> ApiKeyExtractor.readCookie(req, name);
		};
	}

	private AuthenticationException missing() {
		return new AuthenticationException("API key missing").wwwAuthenticate(challenge);
	}

	private AuthenticationException unknown() {
		return new AuthenticationException("API key not recognized").wwwAuthenticate(challenge);
	}
}
