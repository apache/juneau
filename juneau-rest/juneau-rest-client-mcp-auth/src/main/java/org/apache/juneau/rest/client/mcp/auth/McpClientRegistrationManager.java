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

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.rest.auth.oauth.oidc.*;

import com.nimbusds.oauth2.sdk.http.*;

/**
 * Orchestrates SEP-2352 registration-mechanism selection, issuer-keying, and re-registration on authorization-server
 * migration, returning a usable {@link McpClientRegistration} for a discovered AS.
 *
 * <p>
 * Given a discovered {@link OidcMetadata} (whose {@code issuer} identifies the AS and whose {@code extras} carry the
 * RFC 7591 {@code registration_endpoint}), {@link #resolve(OidcMetadata)} selects credentials per the SEP-2352 rules:
 * <ul>
 * 	<li><b>Pre-registered credentials</b> ({@link Builder#preRegistered(McpClientRegistration)}) are used only when
 * 		their bound {@code issuer} matches the discovered AS; a mismatch (the AS migrated) surfaces an
 * 		{@link McpAuthException} rather than silently reusing cross-AS credentials (SEP-2352 SHOULD).
 * 	<li><b>With a store</b> ({@link Builder#store(McpClientRegistrationStore)}) the issuer-keyed entry is reused without
 * 		a second DCR round-trip; on migration to a different issuer the store has no entry for the new AS, so the manager
 * 		re-registers via DCR and stores under the new issuer &mdash; the old AS's entry is never reused (SEP-2352
 * 		"MUST NOT reuse client credentials from a different authorization server").
 * 	<li><b>On-demand mode</b> (no store) performs a fresh DCR on every {@link #resolve(OidcMetadata)} call &mdash; fully
 * 		compliant (READY-312f Q8).
 * 	<li>If the AS advertises no {@code registration_endpoint} and no matching pre-registered credentials exist, an
 * 		{@link McpAuthException} is thrown (no way to obtain a {@code client_id}; CIMD is a tracked fast-follow).
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., ARG_value)
})
public class McpClientRegistrationManager {

	// Argument name constants for assertArgNotNull
	private static final String ARG_value = "value";

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
		McpClientRegistrationStore store;
		McpClientRegistration preRegistered;
		McpApplicationType applicationType = McpApplicationType.NATIVE;
		Supplier<List<URI>> redirectUrisSupplier;
		final Set<String> scopes = st();
		boolean confidential;
		String clientName;
		Supplier<String> initialAccessTokenSupplier;
		Duration httpTimeout = McpDynamicClientRegistrar.DEFAULT_HTTP_TIMEOUT;
		Consumer<HTTPRequest> httpRequestConfigurator;
		Function<OidcMetadata,McpClientRegistration> dcrOverride;

		/** Constructor. */
		protected Builder() {}

		/**
		 * Sets the optional issuer-keyed credential store.  When omitted, the manager runs in on-demand mode and
		 * performs a fresh DCR on every {@code resolve(...)} call (compliant per Q8).
		 *
		 * @param value The store.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder store(McpClientRegistrationStore value) {
			store = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets pre-registered (manually provisioned) credentials bound to a specific AS {@code issuer}.
		 *
		 * @param value The pre-registered credentials.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder preRegistered(McpClientRegistration value) {
			preRegistered = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the {@code application_type} declared when the manager performs DCR.  Default {@link McpApplicationType#NATIVE}.
		 *
		 * @param value The application type.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder applicationType(McpApplicationType value) {
			applicationType = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Sets the redirect URIs to register when the manager performs DCR.
		 *
		 * @param values The redirect URIs (e.g. {@link LoopbackRedirectUris#portAgnostic(String)}).  Must not be
		 * 	<jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder redirectUris(List<URI> values) {
			assertArgNotNull("values", values);
			assertArg(! values.isEmpty(), "redirectUris must not be empty");
			var copy = u(cp(values));
			redirectUrisSupplier = () -> copy;
			return this;
		}

		/**
		 * Sets a supplier of redirect URIs to register when the manager performs DCR (for the bind-first strategy where
		 * the loopback port is learned per-connection).
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder redirectUrisSupplier(Supplier<List<URI>> value) {
			redirectUrisSupplier = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Adds scopes requested during DCR.
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
		 * Registers a confidential client during DCR (default public/native).
		 *
		 * @param value <jk>true</jk> to register a confidential client.
		 * @return This object.
		 */
		public Builder confidential(boolean value) {
			confidential = value;
			return this;
		}

		/**
		 * Sets the human-readable client name declared during DCR.
		 *
		 * @param value The client name.
		 * @return This object.
		 */
		public Builder clientName(String value) {
			clientName = assertArgNotNullOrBlank(ARG_value, value);
			return this;
		}

		/**
		 * Sets the RFC 7591 &sect;3 initial access token for a protected registration endpoint.
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
		 * Sets the connect/read timeout applied to DCR requests.  Default 10 seconds.
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
		 * Sets a callback that customizes the Nimbus {@link HTTPRequest} for each DCR request.
		 *
		 * @param value The callback.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder httpRequestConfigurator(Consumer<HTTPRequest> value) {
			httpRequestConfigurator = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Overrides the registration function the manager invokes to perform DCR (advanced / test seam).
		 *
		 * <p>
		 * The default builds an {@link McpDynamicClientRegistrar} from the discovered {@link OidcMetadata} and performs
		 * a live RFC 7591 round-trip; supply this to inject an alternate registration strategy or a stub.
		 *
		 * @param value The registration function mapping a discovered AS to an issued {@link McpClientRegistration}.
		 * 	Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder registrarFunction(Function<OidcMetadata,McpClientRegistration> value) {
			dcrOverride = assertArgNotNull(ARG_value, value);
			return this;
		}

		/**
		 * Builds the manager.
		 *
		 * @return A new {@link McpClientRegistrationManager}.
		 */
		public McpClientRegistrationManager build() {
			return new McpClientRegistrationManager(this);
		}
	}

	private final McpClientRegistrationStore store;
	private final McpClientRegistration preRegistered;
	private final McpApplicationType applicationType;
	private final Supplier<List<URI>> redirectUrisSupplier;
	private final Set<String> scopes;
	private final boolean confidential;
	private final String clientName;
	private final Supplier<String> initialAccessTokenSupplier;
	private final Duration httpTimeout;
	private final Consumer<HTTPRequest> httpRequestConfigurator;
	private final Function<OidcMetadata,McpClientRegistration> dcrOverride;

	/**
	 * Constructor.
	 *
	 * @param b The builder.
	 */
	protected McpClientRegistrationManager(Builder b) {
		this.store = b.store;
		this.preRegistered = b.preRegistered;
		this.applicationType = b.applicationType;
		this.redirectUrisSupplier = b.redirectUrisSupplier;
		this.scopes = u(new LinkedHashSet<>(b.scopes));
		this.confidential = b.confidential;
		this.clientName = b.clientName;
		this.initialAccessTokenSupplier = b.initialAccessTokenSupplier;
		this.httpTimeout = b.httpTimeout;
		this.httpRequestConfigurator = b.httpRequestConfigurator;
		this.dcrOverride = b.dcrOverride;
	}

	/**
	 * Resolves a usable client registration for the discovered authorization server, applying the SEP-2352 selection,
	 * issuer-keying, and migration rules (see the class javadoc).
	 *
	 * @param as The discovered authorization-server metadata.  Must not be <jk>null</jk>.
	 * @return A usable registration bound to {@code as.issuer()}.  Never <jk>null</jk>.
	 * @throws McpAuthException If credentials cannot be obtained (AS migrated away from pre-registered creds, or the AS
	 * 	advertises no {@code registration_endpoint} and no matching pre-registered creds exist).
	 */
	public McpClientRegistration resolve(OidcMetadata as) {
		assertArgNotNull("as", as);
		var issuer = as.issuer();

		// Pre-registered credentials: use only if bound to this exact AS; a mismatch means the AS migrated (SEP-2352
		// MUST NOT reuse cross-AS creds — SHOULD surface an error rather than silently reuse).
		if (preRegistered != null) {
			if (! preRegistered.issuer().equals(issuer))
				throw new McpAuthException("Pre-registered client credentials are bound to issuer '" + preRegistered.issuer()
					+ "' but the resource now indicates a different authorization server '" + issuer
					+ "' (SEP-2352): refusing to reuse credentials across authorization servers");
			if (store != null)
				store.put(issuer, preRegistered);
			return preRegistered;
		}

		// Store-hit: reuse the issuer-keyed entry without a second DCR round-trip.  On migration to a new issuer the
		// store has no entry for it, so we fall through to DCR and store under the new issuer, never touching the old.
		if (store != null) {
			var hit = store.find(issuer);
			if (hit.isPresent()) {
				// Defensive: a buggy durable store that returns an entry keyed under the wrong issuer would otherwise
				// cause exactly the cross-AS credential reuse SEP-2352 forbids.  Never trust the returned entry's key.
				if (! hit.get().issuer().equals(issuer))
					throw new McpAuthException("Client-registration store returned an entry bound to issuer '" + hit.get().issuer()
						+ "' for a lookup of issuer '" + issuer + "' (SEP-2352): refusing to reuse credentials across authorization servers");
				return hit.get();
			}
		}

		var reg = register(as);
		if (store != null)
			store.put(issuer, reg);
		return reg;
	}

	private McpClientRegistration register(OidcMetadata as) {
		if (dcrOverride != null)
			return dcrOverride.apply(as);
		var regEndpoint = registrationEndpoint(as)
			.orElseThrow(() -> new McpAuthException("Authorization server '" + as.issuer()
				+ "' advertises no registration_endpoint and no matching pre-registered credentials are configured"
				+ " (Dynamic Client Registration not possible; CIMD is out of scope)"));
		if (redirectUrisSupplier == null)
			throw new McpAuthException("McpClientRegistrationManager requires redirectUris(...) to perform Dynamic Client Registration");
		var b = McpDynamicClientRegistrar.create()
			.registrationEndpoint(regEndpoint)
			.issuer(as.issuer())
			.applicationType(applicationType)
			.addRedirectUris(redirectUrisSupplier.get())
			.confidential(confidential)
			.httpTimeout(httpTimeout);
		scopes.forEach(b::scope);
		if (clientName != null)
			b.clientName(clientName);
		if (initialAccessTokenSupplier != null)
			b.initialAccessToken(initialAccessTokenSupplier.get());
		if (httpRequestConfigurator != null)
			b.httpRequestConfigurator(httpRequestConfigurator);
		return b.build().register();
	}

	/**
	 * Extracts the RFC 7591 {@code registration_endpoint} from the discovered AS metadata's extras, if present.
	 *
	 * <p>
	 * The value is AS-supplied, so a hostile/misconfigured AS document is defended against here: a malformed URI or a
	 * plaintext {@code http://} (non-loopback) endpoint yields the documented {@link McpAuthException} rather than a raw
	 * {@link IllegalArgumentException} (from {@code URI.create}) or a downstream {@link IllegalStateException} (from the
	 * registrar's https gate).
	 */
	static Optional<URI> registrationEndpoint(OidcMetadata as) {
		var v = as.extras().get("registration_endpoint");
		if (v == null)
			return oe();
		URI uri;
		try {
			uri = URI.create(v.toString());
		} catch (IllegalArgumentException e) {
			throw new McpAuthException("Authorization server advertised a malformed registration_endpoint: '" + v + "'", e);
		}
		if (! McpDynamicClientRegistrar.isSecureOrLoopback(uri))
			throw new McpAuthException("Authorization server advertised a non-https registration_endpoint (loopback exempt): '" + uri + "'");
		return o(uri);
	}
}
