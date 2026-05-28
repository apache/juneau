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
package org.apache.juneau.rest.auth.oauth.flow;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.juneau.rest.auth.oauth.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;

/**
 * Client-credentials grant (RFC 6749 &sect;4.4) flow helper.
 *
 * <p>
 * Wraps Nimbus's {@link ClientCredentialsGrant} + {@link TokenRequest} behind a Juneau-friendly builder.
 * Supports optional caching via a {@link TokenCache} keyed by {@code (clientId, scope)} so repeat callers
 * within the cache window reuse the token rather than hitting the IdP each time.
 *
 * <h5 class='topic'>Usage</h5>
 *
 * <p class='bjava'>
 * 	<jk>var</jk> token = OAuthClientCredentialsFlow.<jsm>create</jsm>()
 * 		.tokenEndpoint(URI.<jsm>create</jsm>(<js>"https://idp.example.com/oauth2/token"</js>))
 * 		.clientId(<js>"worker-service"</js>)
 * 		.clientSecret(<js>"..."</js>)
 * 		.scope(<js>"read:orders"</js>, <js>"write:orders"</js>)
 * 		.build()
 * 		.acquire();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4">RFC 6749 &sect;4.4</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class OAuthClientCredentialsFlow {

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
		private Set<String> scopes = new LinkedHashSet<>();
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
		 * Configures an optional cache for the acquired token.
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
		 * @return A new {@link OAuthClientCredentialsFlow}.
		 */
		public OAuthClientCredentialsFlow build() {
			if (tokenEndpoint == null)
				throw new IllegalStateException("OAuthClientCredentialsFlow requires tokenEndpoint(...)");
			if (clientId == null)
				throw new IllegalStateException("OAuthClientCredentialsFlow requires clientId(...)");
			if (clientSecretSupplier == null)
				throw new IllegalStateException("OAuthClientCredentialsFlow requires clientSecret(...) or clientSecretSupplier(...)");
			return new OAuthClientCredentialsFlow(this);
		}
	}

	private final URI tokenEndpoint;
	private final String clientId;
	private final Supplier<String> clientSecretSupplier;
	private final Set<String> scopes;
	private final TokenCache tokenCache;
	private final Duration cacheSkew;
	private final Consumer<HTTPRequest> httpRequestConfigurator;
	private final String cacheKey;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthClientCredentialsFlow(Builder b) {
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(b.scopes));
		this.tokenCache = b.tokenCache;
		this.cacheSkew = b.cacheSkew;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
		this.cacheKey = "cc|" + clientId + "|" + String.join(" ", scopes);
	}

	/**
	 * Acquires a token from the IdP.  Returns the cached token when configured.
	 *
	 * @return The acquired token.
	 */
	public OAuthToken acquire() {
		if (tokenCache != null) {
			var hit = tokenCache.getToken(cacheKey, Instant.now(), cacheSkew);
			if (hit.isPresent())
				return hit.get();
		}
		var clientAuth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecretSupplier.get()));
		Scope nimbusScope = scopes.isEmpty() ? null : new Scope(scopes.toArray(new String[0]));
		var req = new TokenRequest.Builder(tokenEndpoint, clientAuth, new ClientCredentialsGrant())
			.scope(nimbusScope)
			.build();
		var token = Flows.send(req, httpRequestConfigurator);
		if (tokenCache != null)
			tokenCache.putToken(cacheKey, token);
		return token;
	}
}
