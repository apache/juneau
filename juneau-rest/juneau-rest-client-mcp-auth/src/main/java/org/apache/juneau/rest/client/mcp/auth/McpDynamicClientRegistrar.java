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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.utils.UriUtils;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.client.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.token.*;
import com.nimbusds.openid.connect.sdk.rp.*;

/**
 * Performs an RFC 7591 / OpenID Connect Dynamic Client Registration (SEP-837) round-trip against an authorization
 * server's {@code registration_endpoint}, returning the issued credentials as a redaction-safe
 * {@link McpClientRegistration} keyed by the AS {@code issuer} (the SEP-2352 binding key).
 *
 * <p>
 * The registration declares an {@code application_type} (SEP-837 <b>MUST</b>) &mdash; {@link McpApplicationType#NATIVE}
 * for CLI/desktop/loopback clients, {@link McpApplicationType#WEB} for remote browser apps &mdash; along with the
 * caller-supplied redirect URIs (see {@link LoopbackRedirectUris}), grant/response types, scope, and
 * {@code token_endpoint_auth_method} ({@code none} for a public/native client by default;
 * {@link Builder#confidential(boolean)} switches to {@code client_secret_basic}).  A protected registration endpoint's
 * RFC 7591 &sect;3 initial access token is attached when {@link Builder#initialAccessToken(String)} is set.
 *
 * <p>
 * The DCR-issued {@code client_id}/secret then feed F1's existing flows unchanged &mdash; DCR changes only how the
 * {@code client_id} was obtained, not the {@code resource=} (RFC 8707) or {@code iss} (RFC 9207 / SEP-2468)
 * enforcement; see {@link McpClientRegistrations} for the bridge to {@link McpAuthorizationCodeAcquirer} /
 * {@link McpTokenProvider}.
 *
 * <p>
 * {@link #register()} is the single public round-trip entry point.  Internally it is split into a request-build step and
 * a response-parse step (both package-private, so the module's own tests can drive them without a network) &mdash; those
 * helpers deliberately traffic in Nimbus {@code provided}-scope types and are <b>not</b> part of the public API.  A
 * registration failure (e.g. {@code invalid_redirect_uri}) surfaces as an {@link McpAuthException} carrying the AS error
 * code (SEP-837 "clients MUST be prepared to handle registration failures").
 *
 * <h5 class='topic'>Security</h5>
 * <p>
 * The registration endpoint MUST originate from validated AS metadata ({@link org.apache.juneau.rest.client.mcp.auth.oidc.OidcDiscoveryClient}
 * enforces the RFC 8414 / OIDC issuer-identity check) and MUST be {@code https} (loopback exempt for local testing,
 * matching {@code McpProtectedResourceMetadataClient.requireSecure}).  Client secrets are never logged &mdash;
 * {@link #toString()} discloses only non-secret configuration and {@link McpClientRegistration} redacts its
 * secret-bearing fields.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class McpDynamicClientRegistrar {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";
	private static final String ARG_values = "values";

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
		URI registrationEndpoint;
		URI issuer;
		McpApplicationType applicationType;
		final List<URI> redirectUris = new ArrayList<>();
		final Set<String> scopes = st();
		final Set<String> grantTypes = st();
		final Set<String> responseTypes = st();
		boolean confidential;
		String clientName;
		Supplier<String> initialAccessTokenSupplier;
		boolean requireSecure = true;
		Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
		Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the AS {@code registration_endpoint} URL.  Required.
		 *
		 * @param value The registration endpoint (typically from validated AS metadata's {@code registration_endpoint}).
		 * @return This object.
		 */
		public Builder registrationEndpoint(URI value) {
			registrationEndpoint = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the AS {@code issuer} these credentials are being registered with.  Required.
		 *
		 * <p>
		 * Stamped onto the resulting {@link McpClientRegistration} as the SEP-2352 credential-binding key.
		 *
		 * @param value The issuer URI.
		 * @return This object.
		 */
		public Builder issuer(URI value) {
			issuer = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the {@code application_type} (SEP-837 <b>MUST</b>).  Required.
		 *
		 * @param value The application type.
		 * @return This object.
		 */
		public Builder applicationType(McpApplicationType value) {
			applicationType = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Adds one or more redirect URIs to register (accumulates across calls).  At least one is required.
		 *
		 * @param values The redirect URIs (e.g. from {@link LoopbackRedirectUris#forPort(int, String)}).
		 * @return This object.
		 */
		public Builder addRedirectUri(URI... values) {
			assertArgNotNull(ARG_values, values);
			for (var v : values)
				redirectUris.add(assertArgNotNull("redirectUri", v));
			return this;
		}

		/**
		 * Adds a list of redirect URIs to register (accumulates across calls).  At least one is required.
		 *
		 * @param values The redirect URIs (e.g. from {@link LoopbackRedirectUris#forPort(int, String)}).
		 * @return This object.
		 */
		public Builder addRedirectUris(List<URI> values) {
			assertArgNotNull(ARG_values, values);
			for (var v : values)
				redirectUris.add(assertArgNotNull("redirectUri", v));
			return this;
		}

		/**
		 * Adds requested scopes.
		 *
		 * @param values The scopes.
		 * @return This object.
		 */
		public Builder scope(String... values) {
			assertArgNotNull(ARG_values, values);
			for (var v : values) {
				assertArgNotNullOrBlank("scope", v);
				scopes.add(v);
			}
			return this;
		}

		/**
		 * Adds requested {@code grant_types} (accumulates across calls).  When none are added, defaults to
		 * {@code authorization_code} + {@code refresh_token}.
		 *
		 * @param values The grant type identifiers.
		 * @return This object.
		 */
		public Builder addGrantType(String... values) {
			assertArgNotNull(ARG_values, values);
			for (var v : values) {
				assertArgNotNullOrBlank("grantType", v);
				grantTypes.add(v);
			}
			return this;
		}

		/**
		 * Adds requested {@code response_types} (accumulates across calls).  When none are added, defaults to
		 * {@code code}.
		 *
		 * @param values The response type identifiers.
		 * @return This object.
		 */
		public Builder addResponseType(String... values) {
			assertArgNotNull(ARG_values, values);
			for (var v : values) {
				assertArgNotNullOrBlank("responseType", v);
				responseTypes.add(v);
			}
			return this;
		}

		/**
		 * Registers a confidential client ({@code token_endpoint_auth_method=client_secret_basic}) instead of the
		 * default public/native client ({@code token_endpoint_auth_method=none}).
		 *
		 * @param value <jk>true</jk> to register a confidential client.
		 * @return This object.
		 */
		public Builder confidential(boolean value) {
			confidential = value;
			return this;
		}

		/**
		 * Sets the human-readable {@code client_name}.
		 *
		 * @param value The client name.
		 * @return This object.
		 */
		public Builder clientName(String value) {
			clientName = assertArgNotNullOrBlank(ARG_value, value);
			return this;
		}

		/**
		 * Sets the RFC 7591 &sect;3 initial access token used to authorize the call to a protected registration
		 * endpoint.
		 *
		 * @param value The initial access token.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder initialAccessToken(String value) {
			assertArgNotNullOrBlank(ARG_value, value);
			initialAccessTokenSupplier = () -> value;
			return this;
		}

		/**
		 * Whether to require the registration endpoint to use {@code https} (loopback exempt).  Default <jk>true</jk>.
		 *
		 * @param value <jk>false</jk> to allow a plaintext registration endpoint (testing only).
		 * @return This object.
		 */
		public Builder requireSecure(boolean value) {
			requireSecure = value;
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the registration request.  Default 10 seconds.
		 *
		 * @param value The timeout.  Must not be <jk>null</jk> and must be positive.
		 * @return This object.
		 */
		public Builder httpTimeout(Duration value) {
			assertArgNotNull(ARG_value, value);
			assertArg(!value.isZero() && !value.isNegative(), "httpTimeout must be positive (was %s)", value);
			httpTimeout = value;
			return this;
		}

		/**
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} for the registration call.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the registrar.
		 *
		 * @return A new {@link McpDynamicClientRegistrar}.
		 */
		public McpDynamicClientRegistrar build() {
			if (registrationEndpoint == null)
				throw isex("McpDynamicClientRegistrar requires registrationEndpoint(...)");
			if (issuer == null)
				throw isex("McpDynamicClientRegistrar requires issuer(...)");
			if (applicationType == null)
				throw isex("McpDynamicClientRegistrar requires applicationType(...) (SEP-837)");
			if (redirectUris.isEmpty())
				throw isex("McpDynamicClientRegistrar requires at least one redirectUri(...)");
			if (requireSecure && ! isSecureOrLoopback(registrationEndpoint))
				throw isex("McpDynamicClientRegistrar registration endpoint must use https (loopback exempt): " + registrationEndpoint);
			return new McpDynamicClientRegistrar(this);
		}
	}

	/** Default connect/read timeout applied to the registration request when the caller sets none. */
	static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

	private final URI registrationEndpoint;
	private final URI issuer;
	private final McpApplicationType applicationType;
	private final List<URI> redirectUris;
	private final Set<String> scopes;
	private final Set<String> grantTypes;
	private final Set<String> responseTypes;
	private final boolean confidential;
	private final String clientName;
	private final Supplier<String> initialAccessTokenSupplier;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpDynamicClientRegistrar(Builder b) {
		this.registrationEndpoint = b.registrationEndpoint;
		this.issuer = b.issuer;
		this.applicationType = b.applicationType;
		this.redirectUris = u(cp(b.redirectUris));
		this.scopes = u(new LinkedHashSet<>(b.scopes));
		this.grantTypes = u(b.grantTypes.isEmpty() ? new LinkedHashSet<>(List.of("authorization_code", "refresh_token")) : new LinkedHashSet<>(b.grantTypes));
		this.responseTypes = u(b.responseTypes.isEmpty() ? new LinkedHashSet<>(List.of("code")) : new LinkedHashSet<>(b.responseTypes));
		this.confidential = b.confidential;
		this.clientName = b.clientName;
		this.initialAccessTokenSupplier = b.initialAccessTokenSupplier;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Builds the Nimbus {@link OIDCClientRegistrationRequest} that {@link #register()} would send, declaring the
	 * {@code application_type}, redirect URIs, grant/response types, {@code token_endpoint_auth_method}, scope, and
	 * (if configured) the initial access token.
	 *
	 * <p>
	 * Package-private: returns a Nimbus {@code provided}-scope type, so it is an internal seam for the module's tests to
	 * inspect the exact request without a network round-trip &mdash; not part of the public API.
	 *
	 * @return The registration request.  Never <jk>null</jk>.
	 */
	OIDCClientRegistrationRequest buildRegistrationRequest() {
		var md = new OIDCClientMetadata();
		md.setApplicationType(applicationType.toNimbus());
		md.setRedirectionURIs(new LinkedHashSet<>(redirectUris));
		md.setGrantTypes(mapGrantTypes(grantTypes));
		md.setResponseTypes(mapResponseTypes(responseTypes));
		md.setTokenEndpointAuthMethod(confidential ? ClientAuthenticationMethod.CLIENT_SECRET_BASIC : ClientAuthenticationMethod.NONE);
		if (! scopes.isEmpty())
			md.setScope(Scope.parse(scopes));
		if (clientName != null)
			md.setName(clientName);
		var iat = initialAccessTokenSupplier == null ? null : new BearerAccessToken(initialAccessTokenSupplier.get());
		return new OIDCClientRegistrationRequest(registrationEndpoint, md, iat);
	}

	/**
	 * Parses a (possibly canned) registration {@link HTTPResponse} into an {@link McpClientRegistration}, stamping the
	 * configured {@code issuer}.
	 *
	 * <p>
	 * On a registration error the AS error object's code is surfaced as an {@link McpAuthException} (SEP-837
	 * registration-failure handling).
	 *
	 * <p>
	 * Package-private: accepts a Nimbus {@code provided}-scope type, so it is an internal seam for the module's tests to
	 * map a canned response &mdash; not part of the public API.
	 *
	 * @param httpResponse The registration response.  Must not be <jk>null</jk>.
	 * @return The mapped registration result.  Never <jk>null</jk>.
	 * @throws McpAuthException If the AS returned an error or the response could not be parsed.
	 */
	McpClientRegistration parseRegistrationResponse(HTTPResponse httpResponse) {
		assertArgNotNull("httpResponse", httpResponse);
		ClientRegistrationResponse resp;
		try {
			resp = OIDCClientRegistrationResponseParser.parse(httpResponse);
		} catch (ParseException e) {
			throw new McpAuthException("Dynamic client registration response could not be parsed", e);
		}
		if (! resp.indicatesSuccess()) {
			var err = resp.toErrorResponse().getErrorObject();
			var code = (err == null || err.getCode() == null) ? "unknown_error" : err.getCode();
			String desc = err == null ? null : err.getDescription();
			throw new McpAuthException("Dynamic client registration failed: " + code + (desc == null ? "" : " (" + desc + ")"));
		}
		return toRegistration(resp.toSuccessResponse().getClientInformation());
	}

	/**
	 * Performs the live RFC 7591 registration round-trip: builds the request, applies the finite HTTP timeout and the
	 * caller's {@code httpRequestConfigurator}, sends it, and maps the response.
	 *
	 * @return The issued registration.  Never <jk>null</jk>.
	 * @throws McpAuthException If the HTTP call fails or the AS returned an error.
	 */
	public McpClientRegistration register() {
		var http = buildRegistrationRequest().toHTTPRequest();
		var ms = (int) Math.min(httpTimeout.toMillis(), Integer.MAX_VALUE);
		http.setConnectTimeout(ms);
		http.setReadTimeout(ms);
		if (httpRequestConfigurator != null)
			httpRequestConfigurator.accept(http);
		HTTPResponse httpResp;
		try {
			httpResp = http.send();
		} catch (IOException e) {
			throw new McpAuthException("Dynamic client registration HTTP call failed", e);
		}
		return parseRegistrationResponse(httpResp);
	}

	private McpClientRegistration toRegistration(ClientInformation info) {
		var secret = info.getSecret();
		Optional<String> clientSecret = (secret == null || secret.getValue() == null) ? oe() : o(secret.getValue());
		Optional<Instant> secretExpires = (secret == null || secret.getExpirationDate() == null) ? oe() : o(secret.getExpirationDate().toInstant());
		var rat = info.getRegistrationAccessToken();
		Optional<String> regAccessToken = (rat == null || rat.getValue() == null) ? oe() : o(rat.getValue());
		Optional<URI> regClientUri = info.getRegistrationURI() == null ? oe() : o(info.getRegistrationURI());
		return new McpClientRegistration(
			info.getID().getValue(),
			clientSecret,
			secretExpires,
			regAccessToken,
			regClientUri,
			issuer,
			redirectUris,
			applicationType,
			extrasFrom(info));
	}

	/** Surfaces any non-registered custom fields the AS returned in the registration response as the registration's extras. */
	private static Map<String,Object> extrasFrom(ClientInformation info) {
		var md = info.getMetadata();
		var custom = md == null ? null : md.getCustomFields();
		return (custom == null || custom.isEmpty()) ? Map.of() : new LinkedHashMap<>(custom);
	}

	private static Set<GrantType> mapGrantTypes(Set<String> v) {
		var out = new LinkedHashSet<GrantType>();
		for (var g : v) {
			try {
				out.add(GrantType.parse(g));
			} catch (ParseException e) {
				throw iaex(e, "Invalid grant_type '%s'", g);
			}
		}
		return out;
	}

	private static Set<ResponseType> mapResponseTypes(Set<String> v) {
		var out = new LinkedHashSet<ResponseType>();
		for (var r : v) {
			try {
				out.add(ResponseType.parse(r));
			} catch (ParseException e) {
				throw iaex(e, "Invalid response_type '%s'", r);
			}
		}
		return out;
	}

	/** Whether the URI is {@code https} or targets a loopback host (exempt from the https requirement). */
	static boolean isSecureOrLoopback(URI uri) {
		return UriUtils.isSecureOrLoopback(uri);
	}

	@Override /* Object */
	public String toString() {
		return "McpDynamicClientRegistrar[registrationEndpoint=" + registrationEndpoint
			+ ", issuer=" + issuer
			+ ", applicationType=" + applicationType
			+ ", redirectUris=" + redirectUris
			+ ", confidential=" + confidential
			+ ", initialAccessToken=" + (initialAccessTokenSupplier != null ? "<redacted>" : "<none>")
			+ "]";
	}
}
