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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.rest.server.auth.TokenValidator;
import org.apache.juneau.rest.server.auth.oauth.OAuthFilter;

/**
 * Binding-owned OAuth 2.1 resource-server (RS) configuration for a {@code 2026-07-28} MCP endpoint &mdash; the F2
 * authorization-hardening baseline (READY-312f).
 *
 * <p>
 * <b>Off by default.</b> {@link #isEnabled()} is <jk>false</jk> unless {@link #setEnabled(boolean) enabled}, so an
 * existing MCP endpoint that does not opt in behaves exactly as before (no bearer requirement, no well-known route).
 * When enabled, the endpoint:
 * <ul>
 * 	<li>Requires a valid RFC 6750 bearer token on the MCP POST endpoint, establishing an authenticated principal.
 * 	<li>Serves an RFC 9728 Protected Resource Metadata document from {@code .well-known/oauth-protected-resource}.
 * 	<li>Answers a missing/invalid token with {@code 401} + a {@code WWW-Authenticate: Bearer ...} challenge carrying the
 * 		{@code resource_metadata} pointer.
 * 	<li>Enforces RFC 8707 audience matching against {@link #getResource() the resource identifier}.
 * </ul>
 *
 * <p>
 * <b>Mutable during setup, effectively immutable once published</b> &mdash; the same builder-less mutable-setup idiom as
 * {@link McpMrtrConfig} / {@link McpCacheConfig}: configure it fully via the {@code createMcpOptions()} /
 * {@code getMcpOptions()} override, then never mutate it afterward (a request-time reader sees it with no
 * synchronization).
 *
 * <h5 class='section'>Token-validation mode:</h5>
 * <p>
 * The validation mode (RFC 7662 introspection vs JWKS/issuer JWT verification) is selected by which
 * {@link TokenValidator} is supplied to {@link #setTokenValidator(TokenValidator)}:
 * {@code OAuthIntrospectionValidator} (from {@code juneau-rest-server-auth-oauth}) for opaque tokens, or
 * {@code JwtTokenValidator} (from {@code juneau-rest-server-auth-jwt}) for JWT access tokens.
 *
 * @since 10.0.0
 */
public class McpResourceServerConfig {

	/** Default challenge realm. */
	public static final String DEFAULT_REALM = "mcp";

	/** Default advertised/accepted bearer method. */
	public static final String DEFAULT_BEARER_METHOD = "header";

	private boolean enabled;
	private URI resource;
	private String audience;
	private String realm = DEFAULT_REALM;
	private boolean requireAudienceClaim = true;
	private TokenValidator tokenValidator;
	private final List<URI> authorizationServers = new ArrayList<>();
	private final Set<String> scopesSupported = new LinkedHashSet<>();
	private final Set<String> requiredScopes = new LinkedHashSet<>();
	private final Set<String> bearerMethodsSupported = new LinkedHashSet<>(List.of(DEFAULT_BEARER_METHOD));

	/** Wildcard operation-name key: an entry keyed on this applies to every operation of a given JSON-RPC method. */
	static final String OPERATION_WILDCARD = "*";

	private final Map<String,Set<String>> operationScopes = new LinkedHashMap<>();
	private McpOperationScopeResolver operationScopeResolver;

	@SuppressWarnings("java:S3077") // OAuthFilter is an effectively-immutable, thread-safe holder; volatile publication of the memoized instance is sufficient.
	private transient volatile OAuthFilter oauthFilter;

	/**
	 * Whether RS authentication is enabled for this binding.
	 *
	 * @return <jk>true</jk> if enabled.  Defaults to <jk>false</jk>.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables RS authentication.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setEnabled(boolean value) {
		enabled = value;
		return this;
	}

	/**
	 * This server's canonical resource identifier (RFC 9728 {@code resource} / RFC 8707 audience default).
	 *
	 * @return The resource identifier, or <jk>null</jk> if not set.
	 */
	public URI getResource() {
		return resource;
	}

	/**
	 * Sets the canonical resource identifier.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset (but is required when {@link #isEnabled() enabled}).
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setResource(URI value) {
		resource = value;
		return this;
	}

	/**
	 * The RFC 8707 audience this server accepts.
	 *
	 * @return The explicit audience, or (when unset) {@link #getResource() the resource identifier} as a string, or
	 * 	<jk>null</jk> if neither is set.
	 */
	public String getAudience() {
		if (audience != null)
			return audience;
		return resource == null ? null : resource.toString();
	}

	/**
	 * Sets an explicit RFC 8707 audience, overriding the {@link #getResource() resource-identifier} default.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to fall back to the resource identifier.
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setAudience(String value) {
		audience = value;
		return this;
	}

	/**
	 * The {@code WWW-Authenticate: Bearer realm="..."} challenge realm.
	 *
	 * @return The realm.  Never <jk>null</jk>; defaults to {@value #DEFAULT_REALM}.
	 */
	public String getRealm() {
		return realm;
	}

	/**
	 * Sets the challenge realm.
	 *
	 * @param value The new value.  Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public McpResourceServerConfig setRealm(String value) {
		if (value == null || value.isBlank())
			throw iaex("realm must not be null or blank");
		realm = value;
		return this;
	}

	/**
	 * Whether a validated token must expose audience claims for RFC 8707 audience enforcement to apply (READY-312f F2, H2).
	 *
	 * <p>
	 * When <jk>true</jk> (the default), a validator that returns a bare {@link java.security.Principal} carrying no
	 * claims is <b>rejected</b> ({@code 401 invalid_token}) rather than silently passing the confused-deputy defense.
	 * Set to <jk>false</jk> only when the {@link #getTokenValidator() token validator} itself enforces the audience
	 * internally (e.g. a {@code JwtTokenValidator} configured to verify {@code aud}) so a claims-less principal is a
	 * deliberate, trusted signal &mdash; pairs with introspection-mode validators that legitimately omit {@code aud}.
	 *
	 * @return <jk>true</jk> if a claims-less principal is rejected (the default).
	 */
	public boolean isRequireAudienceClaim() {
		return requireAudienceClaim;
	}

	/**
	 * Sets whether a validated token must expose audience claims for audience enforcement to apply (H2).
	 *
	 * @param value The new value.  Defaults to <jk>true</jk> (fail-closed).
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setRequireAudienceClaim(boolean value) {
		requireAudienceClaim = value;
		return this;
	}

	/**
	 * The token validator that verifies presented bearer tokens.
	 *
	 * @return The validator, or <jk>null</jk> if not set (required when {@link #isEnabled() enabled}).
	 */
	public TokenValidator getTokenValidator() {
		return tokenValidator;
	}

	/**
	 * Sets the token validator (selects introspection vs JWT validation mode).
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset (but is required when {@link #isEnabled() enabled}).
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setTokenValidator(TokenValidator value) {
		tokenValidator = value;
		return this;
	}

	/**
	 * The advertised {@code authorization_servers} (RFC 9728).
	 *
	 * @return A live, mutable list.  Never <jk>null</jk>.
	 */
	public List<URI> getAuthorizationServers() {
		return authorizationServers;
	}

	/**
	 * Adds an authorization-server issuer URI.
	 *
	 * @param value The issuer URI.  Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public McpResourceServerConfig addAuthorizationServer(URI value) {
		if (value == null)
			throw iaex("authorizationServer must not be null");
		authorizationServers.add(value);
		return this;
	}

	/**
	 * The advertised {@code scopes_supported} (RFC 9728).
	 *
	 * @return A live, mutable set.  Never <jk>null</jk>.
	 */
	public Set<String> getScopesSupported() {
		return scopesSupported;
	}

	/**
	 * Adds an advertised supported scope.
	 *
	 * @param value The scope.  Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public McpResourceServerConfig addScopeSupported(String value) {
		if (value == null || value.isBlank())
			throw iaex("scope must not be null or blank");
		scopesSupported.add(value);
		return this;
	}

	/**
	 * The scopes a token must carry to access the MCP endpoint (baseline insufficient-scope enforcement).
	 *
	 * @return A live, mutable set.  Never <jk>null</jk>; empty means no scope is required beyond a valid token.
	 */
	public Set<String> getRequiredScopes() {
		return requiredScopes;
	}

	/**
	 * Adds a required scope (also advertised as supported).
	 *
	 * @param value The scope.  Must not be <jk>null</jk> or blank.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public McpResourceServerConfig addRequiredScope(String value) {
		if (value == null || value.isBlank())
			throw iaex("scope must not be null or blank");
		requiredScopes.add(value);
		scopesSupported.add(value);
		return this;
	}

	/**
	 * The advertised {@code bearer_methods_supported} (RFC 9728 / RFC 6750).
	 *
	 * @return A live, mutable set.  Never <jk>null</jk>; defaults to {@code [header]}.
	 */
	public Set<String> getBearerMethodsSupported() {
		return bearerMethodsSupported;
	}

	/**
	 * Registers the scopes required to invoke a specific MCP operation (SEP-2350 per-operation step-up).
	 *
	 * <p>
	 * Populates the default static per-operation scope map consulted by {@link #requiredScopesFor(McpOperationContext)}
	 * when no explicit {@link #setOperationScopeResolver(McpOperationScopeResolver) resolver} is set.  A {@code null} or
	 * blank {@code name} registers a method-wide entry matching every operation of that JSON-RPC {@code method} (e.g.
	 * every {@code tools/call} regardless of tool name); a specific {@code name} takes precedence over the method-wide
	 * entry.  Each supplied scope is also advertised as {@link #addScopeSupported(String) supported}.
	 *
	 * @param method The JSON-RPC method (e.g. {@code tools/call}).  Must not be <jk>null</jk> or blank.
	 * @param name The operation name (tool/prompt name or resource URI), or <jk>null</jk>/blank for a method-wide entry.
	 * @param scopes The scopes required to invoke the operation.  Must not be <jk>null</jk>, empty, or contain a
	 * 	<jk>null</jk>/blank element.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If {@code method} is <jk>null</jk>/blank or {@code scopes} is invalid.
	 */
	public McpResourceServerConfig addOperationScope(String method, String name, String... scopes) {
		if (method == null || method.isBlank())
			throw iaex("method must not be null or blank");
		if (scopes == null || scopes.length == 0)
			throw iaex("scopes must not be null or empty");
		// Validate the whole scopes array BEFORE mutating scopesSupported, so a throw on a later blank element never
		// leaves earlier scopes half-advertised.
		var set = new LinkedHashSet<String>();
		for (var s : scopes) {
			if (s == null || s.isBlank())
				throw iaex("scope must not be null or blank");
			set.add(s);
		}
		scopesSupported.addAll(set);
		operationScopes.put(operationKey(method, name == null || name.isBlank() ? OPERATION_WILDCARD : name), set);
		return this;
	}

	/**
	 * The dynamic per-operation scope resolver, if one was installed.
	 *
	 * @return The resolver, or <jk>null</jk> to use the default static {@link #addOperationScope map}.
	 */
	public McpOperationScopeResolver getOperationScopeResolver() {
		return operationScopeResolver;
	}

	/**
	 * Installs a dynamic per-operation scope resolver, overriding the default static {@link #addOperationScope map}.
	 *
	 * <p>
	 * The seam for a future dynamic resolver (READY-312f Q-c); not built for 10.0.  When set, it fully replaces the
	 * static map for {@link #requiredScopesFor(McpOperationContext)}.
	 *
	 * @param value The resolver, or <jk>null</jk> to revert to the static map.
	 * @return This object (for method chaining).
	 */
	public McpResourceServerConfig setOperationScopeResolver(McpOperationScopeResolver value) {
		operationScopeResolver = value;
		return this;
	}

	/**
	 * Resolves the scopes required to invoke the given operation (SEP-2350).
	 *
	 * <p>
	 * Consults the {@link #setOperationScopeResolver(McpOperationScopeResolver) resolver} when installed; otherwise the
	 * static {@link #addOperationScope map}, preferring a {@link McpOperationContext#name() name}-specific entry over the
	 * method-wide {@value #OPERATION_WILDCARD} entry.  An empty return means "no per-operation requirement beyond the
	 * endpoint-wide {@link #getRequiredScopes() baseline}".
	 *
	 * @param ctx The resolved operation.  Must not be <jk>null</jk>.
	 * @return The required scopes (never <jk>null</jk>; possibly empty).
	 */
	public Set<String> requiredScopesFor(McpOperationContext ctx) {
		if (ctx == null)
			throw iaex("ctx must not be null");
		if (operationScopeResolver != null) {
			var s = operationScopeResolver.requiredScopesFor(ctx);
			return s == null ? Set.of() : s;
		}
		var name = ctx.name();
		if (name != null) {
			var s = operationScopes.get(operationKey(ctx.method(), name));
			if (s != null)
				return Collections.unmodifiableSet(s);
		}
		var s = operationScopes.get(operationKey(ctx.method(), OPERATION_WILDCARD));
		return s == null ? Set.of() : Collections.unmodifiableSet(s);
	}

	private static String operationKey(String method, String name) {
		return method + '\u0000' + name;
	}

	/**
	 * Validates that this config is internally consistent for the {@link #isEnabled() enabled} state.
	 *
	 * <p>
	 * A no-op when disabled.  When enabled, requires a resource identifier (absolute, with scheme + authority) and a
	 * token validator.
	 *
	 * @throws IllegalStateException If enabled but misconfigured.
	 */
	public void validateEnabled() {
		if (!enabled)
			return;
		if (resource == null)
			throw isex("McpResourceServerConfig is enabled but no resource identifier was configured");
		if (resource.getScheme() == null || resource.getRawAuthority() == null)
			throw isex("McpResourceServerConfig resource %s must be an absolute URI with a scheme and authority", resource);
		if (tokenValidator == null)
			throw isex("McpResourceServerConfig is enabled but no TokenValidator was configured");
	}

	/**
	 * Returns this config's memoized {@link OAuthFilter} (READY-312f F2, M3).
	 *
	 * <p>
	 * The filter (RFC 6750 bearer extraction + delegated token validation) is built once from the
	 * {@link #getTokenValidator() token validator} and {@link #getRealm() realm} and reused across requests rather than
	 * rebuilt per request.  First use runs {@link #validateEnabled()}; under the never-mutate-after-setup contract the
	 * validator/realm are stable by the time a request reads this, so the memoized filter is authoritative.
	 *
	 * @return The bearer filter.  Never <jk>null</jk>.
	 * @throws IllegalStateException If this config is enabled but misconfigured.
	 */
	OAuthFilter oauthFilter() {
		var f = oauthFilter;
		if (f == null) {
			synchronized (this) {
				f = oauthFilter;
				if (f == null) {
					validateEnabled();
					f = OAuthFilter.create().validator(tokenValidator).realm(realm).build();
					oauthFilter = f;
				}
			}
		}
		return f;
	}
}
