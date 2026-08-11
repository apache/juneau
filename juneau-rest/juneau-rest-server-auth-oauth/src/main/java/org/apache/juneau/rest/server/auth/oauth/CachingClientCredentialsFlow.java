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

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.auth.oauth.flow.*;

import com.nimbusds.oauth2.sdk.http.*;

/**
 * Server-side caching decorator layering an optional {@link TokenCache} over the role-neutral
 * {@link OAuthClientCredentialsFlow}.
 *
 * <p>
 * The neutral flow performs the RFC 6749 &sect;4.4 client-credentials token acquisition; this decorator adds the
 * server-side {@code (clientId, scope)}-keyed cache so repeat callers within the cache window reuse the token rather
 * than hitting the IdP on each call.  When no {@link Builder#tokenCache(TokenCache) cache} is configured the decorator
 * simply delegates to the neutral flow.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> token = CachingClientCredentialsFlow.<jsm>create</jsm>()
 * 		.tokenEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/token"</js>))
 * 		.clientId(<js>"worker-service"</js>)
 * 		.clientSecret(<js>"..."</js>)
 * 		.scope(<js>"read:orders"</js>)
 * 		.tokenCache(BoundedLruTokenCache.<jsm>create</jsm>())
 * 		.build()
 * 		.acquire();
 * </p>
 *
 * @since 10.0.0
 */
public class CachingClientCredentialsFlow {

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
		private URI tokenEndpoint;
		private String clientId;
		private Supplier<String> clientSecretSupplier;
		private Set<String> scopes = st();
		private TokenCache tokenCache;
		private Duration cacheSkew = Duration.ofSeconds(30);
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the token endpoint URL.  Required.
		 *
		 * @param value The endpoint URL.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder tokenEndpoint(URI value) {
			tokenEndpoint = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client ID.  Required.
		 *
		 * @param value The client ID.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientId(String value) {
			clientId = assertArgNotNullOrBlank("value", value);
			return this;
		}

		/**
		 * Sets the OAuth client secret as a literal string.
		 *
		 * @param value The client secret.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder clientSecret(String value) {
			assertArgNotNullOrBlank("value", value);
			clientSecretSupplier = () -> value;
			return this;
		}

		/**
		 * Sets the OAuth client secret via a supplier.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientSecretSupplier(Supplier<String> value) {
			clientSecretSupplier = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Adds requested OAuth scopes.
		 *
		 * @param values The scopes.
		 * @return This object.
		 */
		public Builder scope(String... values) {
			assertArgNotNull("values", values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				scopes.add(v);
			}
			return this;
		}

		/**
		 * Configures the cache for the acquired token.
		 *
		 * @param value The cache.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder tokenCache(TokenCache value) {
			tokenCache = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the skew tolerance subtracted from the cached token's expiry.  Default 30s.
		 *
		 * @param value The skew.  Must be non-negative.
		 * @return This object.
		 */
		public Builder cacheSkew(Duration value) {
			assertArgNotNull("value", value);
			if (value.isNegative())
				throw new IllegalArgumentException("cacheSkew must be non-negative");
			cacheSkew = value;
			return this;
		}

		/**
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest}.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Builds the flow.
		 *
		 * @return A new {@link CachingClientCredentialsFlow}.
		 */
		public CachingClientCredentialsFlow build() {
			if (tokenEndpoint == null)
				throw new IllegalStateException("CachingClientCredentialsFlow requires tokenEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("CachingClientCredentialsFlow requires clientId(...)");
			if (clientSecretSupplier == null)
				throw new IllegalStateException("CachingClientCredentialsFlow requires clientSecret(...) or clientSecretSupplier(...)");
			return new CachingClientCredentialsFlow(this);
		}
	}

	private final OAuthClientCredentialsFlow delegate;
	private final TokenCache tokenCache;
	private final Duration cacheSkew;
	private final String cacheKey;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected CachingClientCredentialsFlow(Builder b) {
		var scopes = u(cp(b.scopes));
		var db = OAuthClientCredentialsFlow.create()
			.tokenEndpoint(b.tokenEndpoint)
			.clientId(b.clientId)
			.clientSecretSupplier(b.clientSecretSupplier);
		if (!scopes.isEmpty())
			db.scope(scopes.toArray(new String[0]));
		if (b.httpRequestConfigurator != null)
			db.httpRequestConfigurator(b.httpRequestConfigurator);
		this.delegate = db.build();
		this.tokenCache = b.tokenCache;
		this.cacheSkew = b.cacheSkew;
		this.cacheKey = "cc|" + b.clientId + "|" + String.join(" ", scopes);
	}

	/**
	 * Acquires a token, returning the cached token when a fresh one is present.
	 *
	 * @return The acquired token.
	 */
	public OAuthToken acquire() {
		if (tokenCache != null) {
			var hit = tokenCache.getToken(cacheKey, Instant.now(), cacheSkew);
			if (hit.isPresent())
				return hit.get();
		}
		var token = delegate.acquire();
		if (tokenCache != null)
			tokenCache.putToken(cacheKey, token);
		return token;
	}
}
