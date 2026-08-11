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

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.auth.oauth.oidc.*;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.oauth2.sdk.util.*;

import net.minidev.json.*;

/**
 * Fetches and parses an RFC 9728 Protected Resource Metadata (PRM) document, and resolves the authorization server a
 * protected MCP resource delegates to.
 *
 * <p>
 * This is the client half of the OAuth 2.1 baseline PRM discovery required by the MCP {@code 2026-07-28} revision: on a
 * {@code 401 WWW-Authenticate} challenge the client recovers the {@code resource_metadata} pointer
 * ({@link WwwAuthenticateChallenge#resourceMetadata()}), fetches the PRM document here, selects an authorization server
 * ({@link McpProtectedResourceMetadata#firstAuthorizationServer()}), and validates that server's issuer identity via
 * {@link OidcDiscoveryClient} (RFC 8414 / OIDC Discovery).
 *
 * <p>
 * When an expected resource identifier is supplied ({@link Builder#expectedResource(URI)} or the {@code expectedResource}
 * argument of {@link #fetch(URI, URI)} / {@link #parse(String, URI, URI)}), the PRM document's {@code resource} field is
 * validated against it per RFC 9728 &sect;3.3 (exact string match, mirroring the issuer-identity posture) and a mismatch
 * is rejected &mdash; this defends against a malicious server pointing the client at a PRM document minted for a
 * different resource.  When no expected resource is supplied the identity check is skipped.
 *
 * <p>
 * By default the PRM URL and any discovered authorization-server URI must use {@code https} (loopback hosts are always
 * permitted for local/testing use); {@link Builder#allowInsecureHttp(boolean)} disables the scheme check entirely.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value, PROP_resource)
})
public class McpProtectedResourceMetadataClient {

	/** Default connect/read timeout applied to a PRM fetch / AS discovery when the caller sets none. */
	static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";

	// PRM document property name constants (RFC 9728)
	private static final String PROP_resource = "resource";
	private static final String PROP_authorizationServers = "authorization_servers";
	private static final String PROP_scopesSupported = "scopes_supported";

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
		private URI expectedResource;
		private Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
		private boolean allowInsecureHttp;
		private Consumer<HTTPRequest> httpRequestConfigurator;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the expected {@code resource} identifier the PRM document's {@code resource} field is validated against
		 * (RFC 9728 &sect;3.3).
		 *
		 * <p>
		 * This is the canonical URI of the MCP server the client was actually talking to.  A per-call value passed to
		 * {@link #fetch(URI, URI)} / {@link #parse(String, URI, URI)} overrides this.
		 *
		 * @param value The expected resource identifier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder expectedResource(URI value) {
			expectedResource = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the connect/read timeout applied to the PRM fetch and AS discovery.  Default 10 seconds.
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
		 * Allows plain {@code http} PRM / authorization-server URLs (loopback hosts are always allowed regardless).
		 *
		 * @param value <jk>true</jk> to skip the {@code https} scheme check.
		 * @return This object.
		 */
		public Builder allowInsecureHttp(boolean value) {
			allowInsecureHttp = value;
			return this;
		}

		/**
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} used for the PRM fetch and AS discovery.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the client.
		 *
		 * @return A new {@link McpProtectedResourceMetadataClient}.
		 */
		public McpProtectedResourceMetadataClient build() {
			return new McpProtectedResourceMetadataClient(this);
		}
	}

	private final URI expectedResource;
	private final Duration httpTimeout;
	private final boolean allowInsecureHttp;
	private final Consumer<HTTPRequest> httpRequestConfigurator;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpProtectedResourceMetadataClient(Builder b) {
		this.expectedResource = b.expectedResource;
		this.httpTimeout = b.httpTimeout;
		this.allowInsecureHttp = b.allowInsecureHttp;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
	}

	/**
	 * Fetches and parses the PRM document at the supplied URL, validating its {@code resource} field against the
	 * builder-configured {@link Builder#expectedResource(URI)} (if any).
	 *
	 * @param prmUrl The PRM document URL (typically the {@code resource_metadata} pointer from a {@code 401}
	 * 	challenge, or the well-known URL for the resource).  Must not be <jk>null</jk>.
	 * @return The parsed metadata.
	 * @throws McpAuthException If the fetch fails, the document cannot be parsed, or the resource identity check fails.
	 */
	public McpProtectedResourceMetadata fetch(URI prmUrl) {
		return fetch(prmUrl, expectedResource);
	}

	/**
	 * Fetches and parses the PRM document at the supplied URL, validating its {@code resource} field against the
	 * supplied expected resource identifier (RFC 9728 &sect;3.3).
	 *
	 * @param prmUrl The PRM document URL.  Must not be <jk>null</jk>.
	 * @param expectedResource The resource identifier the client was actually talking to.  May be <jk>null</jk> to
	 * 	skip the identity check.
	 * @return The parsed metadata.
	 * @throws McpAuthException If the fetch fails, the document cannot be parsed, or the resource identity check fails.
	 */
	public McpProtectedResourceMetadata fetch(URI prmUrl, URI expectedResource) {
		assertArgNotNull("prmUrl", prmUrl);
		requireSecure(prmUrl, "Protected-resource-metadata URL");
		HTTPResponse resp;
		try {
			var req = new HTTPRequest(HTTPRequest.Method.GET, prmUrl.toURL());
			req.setConnectTimeout((int) httpTimeout.toMillis());
			req.setReadTimeout((int) httpTimeout.toMillis());
			req.setAccept("application/json");
			if (httpRequestConfigurator != null)
				httpRequestConfigurator.accept(req);
			resp = req.send();
		} catch (IOException e) {
			throw new McpAuthException("Protected-resource-metadata fetch failed for " + prmUrl, e);
		}
		if (!resp.indicatesSuccess())
			throw new McpAuthException("Protected-resource-metadata fetch returned HTTP " + resp.getStatusCode() + " for " + prmUrl);
		return parse(resp.getBody(), prmUrl, expectedResource);
	}

	/**
	 * Parses a PRM JSON document body, validating its {@code resource} field against the builder-configured
	 * {@link Builder#expectedResource(URI)} (if any).
	 *
	 * @param json The JSON document body.  Must not be <jk>null</jk>.
	 * @param source The source URI, used only for error messages.  May be <jk>null</jk>.
	 * @return The parsed metadata.
	 * @throws McpAuthException If the document cannot be parsed, is missing the required {@code resource} field, or
	 * 	fails the resource identity check.
	 */
	public McpProtectedResourceMetadata parse(String json, URI source) {
		return parse(json, source, expectedResource);
	}

	/**
	 * Parses a PRM JSON document body, validating its {@code resource} field against the supplied expected resource
	 * identifier (RFC 9728 &sect;3.3).
	 *
	 * @param json The JSON document body.  Must not be <jk>null</jk>.
	 * @param source The source URI, used only for error messages.  May be <jk>null</jk>.
	 * @param expectedResource The resource identifier the client was actually talking to.  May be <jk>null</jk> to
	 * 	skip the identity check.
	 * @return The parsed metadata.
	 * @throws McpAuthException If the document cannot be parsed, is missing the required {@code resource} field, or
	 * 	fails the resource identity check.
	 */
	public McpProtectedResourceMetadata parse(String json, URI source, URI expectedResource) {
		assertArgNotNull("json", json);
		JSONObject o;
		try {
			o = JSONObjectUtils.parse(json);
		} catch (ParseException e) {
			throw new McpAuthException("Protected-resource-metadata document could not be parsed for " + source, e);
		}
		McpProtectedResourceMetadata prm;
		try {
			var resource = JSONObjectUtils.getURI(o, PROP_resource);
			var authServers = new ArrayList<URI>();
			if (o.containsKey(PROP_authorizationServers))
				for (var v : JSONObjectUtils.getStringList(o, PROP_authorizationServers))
					authServers.add(URI.create(v));
			var scopes = new LinkedHashSet<String>();
			if (o.containsKey(PROP_scopesSupported))
				scopes.addAll(JSONObjectUtils.getStringList(o, PROP_scopesSupported));
			var extras = new LinkedHashMap<String,Object>();
			for (var e : o.entrySet())
				if (!STANDARD_FIELDS.contains(e.getKey()))
					extras.put(e.getKey(), e.getValue());
			prm = new McpProtectedResourceMetadata(resource, authServers, scopes, extras);
		} catch (ParseException | IllegalArgumentException e) {
			throw new McpAuthException("Protected-resource-metadata document was malformed for " + source, e);
		}
		validateResourceIdentity(prm, expectedResource);
		return prm;
	}

	/**
	 * Selects the first authorization server from the PRM document and validates its issuer identity via RFC 8414 /
	 * OIDC discovery.
	 *
	 * @param prm The parsed PRM document.  Must not be <jk>null</jk>.
	 * @return The discovered authorization-server metadata (issuer validated).
	 * @throws McpAuthException If no authorization server is advertised or discovery/issuer validation fails.
	 */
	public OidcMetadata discoverAuthorizationServer(McpProtectedResourceMetadata prm) {
		assertArgNotNull("prm", prm);
		var as = prm.firstAuthorizationServer()
			.orElseThrow(() -> new McpAuthException("Protected-resource-metadata for " + prm.resource() + " advertised no authorization_servers"));
		requireSecure(as, "Authorization-server URL");
		try {
			return OidcDiscoveryClient.create()
				.issuer(as)
				.httpTimeout(httpTimeout)
				.httpRequestConfigurator(httpRequestConfigurator == null ? h -> {} : httpRequestConfigurator)
				.build()
				.discover();
		} catch (IOException | OidcDiscoveryException e) {
			throw new McpAuthException("Authorization-server discovery/issuer validation failed for " + as, e);
		}
	}

	/**
	 * Validates the PRM {@code resource} field against the expected resource identifier (RFC 9728 &sect;3.3), using a
	 * normalization-free exact string comparison to match the issuer-identity posture.
	 */
	private static void validateResourceIdentity(McpProtectedResourceMetadata prm, URI expectedResource) {
		if (expectedResource == null)
			return;
		if (!expectedResource.toString().equals(prm.resource().toString()))
			throw new McpAuthException("Protected-resource-metadata resource identity mismatch (RFC 9728 3.3): expected '"
				+ expectedResource + "' but document declared '" + prm.resource() + "'");
	}

	private void requireSecure(URI uri, String what) {
		if (allowInsecureHttp || "https".equalsIgnoreCase(uri.getScheme()) || isLoopback(uri))
			return;
		throw new McpAuthException(what + " must use https (was '" + uri + "'); enable allowInsecureHttp for non-loopback http");
	}

	private static boolean isLoopback(URI uri) {
		var host = uri.getHost();
		if (host == null)
			return false;
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host);
	}

	private static final Set<String> STANDARD_FIELDS = Set.of(
		PROP_resource, PROP_authorizationServers, PROP_scopesSupported
	);
}
