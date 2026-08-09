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
package org.apache.juneau.rest.client.mcp.auth.flow;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.UriUtils.*;

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.id.*;

/**
 * Client-credentials grant (RFC 6749 &sect;4.4) flow helper.
 *
 * <p>
 * Relocated from {@code juneau-rest-server-auth-oauth} for client-side use &mdash; see this package's javadoc.  This
 * client-side copy drops the server module's optional {@code TokenCache} coupling (caching is owned by
 * {@code McpTokenProvider}) and adds RFC 8707 resource-indicator support via {@link Builder#resource(URI)}.
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
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are OAuth protocol parameter names (e.g. "grant_type", "client_id"); intentional
})
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
		private Set<String> scopes = st();
		private URI resource;
		private Duration httpTimeout = Flows.DEFAULT_HTTP_TIMEOUT;
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
			tokenEndpoint = assertSecureOrLoopback(assertArgNotNull("value", value));
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
		 * Sets the RFC 8707 {@code resource} indicator sent on the token request.
		 *
		 * <p>
		 * The canonical URI of the protected resource (the MCP server) the acquired token is intended for.  Setting
		 * this binds the token's audience so a malicious server cannot replay it against a different resource.
		 *
		 * @param value The canonical resource URI.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder resource(URI value) {
			resource = assertArgNotNull("value", value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the token request.  Default 10 seconds.
		 *
		 * @param value The timeout.  Must not be <jk>null</jk> and must be positive.
		 * @return This object.
		 */
		public Builder httpTimeout(Duration value) {
			assertArgNotNull("value", value);
			assertArg(!value.isZero() && !value.isNegative(), "httpTimeout must be positive (was %s)", value);
			httpTimeout = value;
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
	private final URI resource;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected OAuthClientCredentialsFlow(Builder b) {
		this.tokenEndpoint = b.tokenEndpoint;
		this.clientId = b.clientId;
		this.clientSecretSupplier = b.clientSecretSupplier;
		this.scopes = u(cp(b.scopes));
		this.resource = b.resource;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Acquires a token from the IdP.
	 *
	 * @return The acquired token.
	 */
	public OAuthToken acquire() {
		var clientAuth = new ClientSecretBasic(new ClientID(clientId), new Secret(clientSecretSupplier.get()));
		Scope nimbusScope = scopes.isEmpty() ? null : new Scope(scopes.toArray(new String[0]));
		var reqBuilder = new TokenRequest.Builder(tokenEndpoint, clientAuth, new ClientCredentialsGrant())
			.scope(nimbusScope);
		if (resource != null)
			reqBuilder.resource(resource);
		return Flows.send(reqBuilder.build(), httpTimeout, httpRequestConfigurator);
	}
}
