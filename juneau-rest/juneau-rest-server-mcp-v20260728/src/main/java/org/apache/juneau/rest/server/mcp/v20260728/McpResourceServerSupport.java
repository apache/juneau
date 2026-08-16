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

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.apache.juneau.http.response.Forbidden;
import org.apache.juneau.http.response.InternalServerError;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.rest.server.RestContext;
import org.apache.juneau.rest.server.RestOpContext;
import org.apache.juneau.rest.server.RestRequest;
import org.apache.juneau.rest.server.RestServerConstants;
import org.apache.juneau.rest.server.auth.AuthenticationException;
import org.apache.juneau.rest.server.auth.AuthResult;
import org.apache.juneau.rest.server.mcp.McpEndpointMixin;
import org.apache.juneau.rest.server.util.RestUtils;
import org.apache.juneau.rest.server.util.UrlPath;

import jakarta.servlet.http.*;

/**
 * Request-time glue for the F2 OAuth 2.1 resource-server baseline &mdash; bearer validation, RFC 8707 audience
 * enforcement, {@code 401}/{@code 403} challenge emission, and RFC 9728 Protected Resource Metadata construction &mdash;
 * driven by an {@link McpResourceServerConfig}.
 *
 * <p>
 * Bearer extraction and token validation are delegated to the reusable
 * {@link org.apache.juneau.rest.server.auth.oauth.OAuthFilter} (RFC 6750) from
 * {@code juneau-rest-server-auth-oauth}; this class layers on the MCP-specific {@code resource_metadata} challenge
 * parameter, RFC 8707 audience matching ({@link McpAudienceValidator}), and baseline required-scope enforcement.
 *
 * @since 10.0.0
 */
public final class McpResourceServerSupport {

	private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	/** Java method name of the MCP JSON-RPC dispatch operation ({@link McpEndpointMixin#handleMcpRequest}). */
	private static final String MCP_DISPATCH_METHOD = "handleMcpRequest";

	/**
	 * Request attribute under which {@link #authenticate} stashes the caller's granted scopes (the validated token's
	 * roles) so the POST-parse dispatch point can enforce SEP-2350 per-operation step-up scopes.
	 */
	public static final String GRANTED_SCOPES_ATTR = "org.apache.juneau.rest.server.mcp.v20260728.grantedScopes";

	private McpResourceServerSupport() {}

	/**
	 * Router-identity bearer gate for the {@link McpEndpoint} mixin's MCP JSON-RPC endpoint (READY-312f F2, B1/H3).
	 *
	 * <p>
	 * A no-op unless RS auth is {@link McpResourceServerConfig#isEnabled() enabled} and this request resolves to the MCP
	 * JSON-RPC dispatch operation.  See {@link #resolvesToMcpDispatch(RestContext, HttpServletRequest)} for how the target
	 * operation is identified without any hand-rolled path reconstruction &mdash; the key change that makes this gate
	 * immune to {@code @Mixin(path/paths)} re-mounts and to trailing-slash / percent-encoding / method-override drift.
	 *
	 * <p>
	 * Fails fast with the same origin-root {@code 500} diagnostic as the well-known route (see
	 * {@link #assertWellKnownMountReachable(RestRequest)}) &mdash; rather than authenticating &mdash; whenever the
	 * resolved MCP dispatch operation is itself mounted under a {@code @Mixin(path/paths)} re-mount prefix (H1
	 * unification): a {@code 401} on such a mount would advertise a {@code resource_metadata} URL that the re-mount
	 * has already made unreachable, so the two routes must agree rather than leaving a broken half-state.
	 *
	 * @param cfg The RS config.  Must not be <jk>null</jk>.
	 * @param ctx The host {@link RestContext} handling this request (injected into the {@code @RestStartCall} hook).
	 * 	Must not be <jk>null</jk>.
	 * @param req The raw HTTP request.  Must not be <jk>null</jk>.
	 * @param res The raw HTTP response (used to emit the challenge header on rejection).  Must not be <jk>null</jk>.
	 */
	public static void gateMcpEndpoint(McpResourceServerConfig cfg, RestContext ctx, HttpServletRequest req, HttpServletResponse res) {
		if (!cfg.isEnabled())
			return;
		if (!isEffectivePost(req))
			return;
		var op = resolveMcpDispatchOp(ctx, req);
		if (op == null)
			return;
		assertOriginRootMount(req);
		assertNotMixinRemounted(op.getContext().getMixinMountPrefixes());
		authenticate(cfg, req, res);
	}

	/**
	 * Router-aligned bearer gate for the {@link McpRestServlet}'s {@code POST /} endpoint (READY-312f F2, H3).
	 *
	 * <p>
	 * A no-op unless RS auth is {@link McpResourceServerConfig#isEnabled() enabled} and this request resolves to a
	 * {@code POST} (the dedicated MCP servlet's only writable surface).  Gating every effective {@code POST} is correct for
	 * a dedicated MCP servlet: there is no non-MCP writable surface to distinguish, so no per-operation identity check is
	 * needed.
	 *
	 * @param cfg The RS config.  Must not be <jk>null</jk>.
	 * @param req The raw HTTP request.  Must not be <jk>null</jk>.
	 * @param res The raw HTTP response (used to emit the challenge header on rejection).  Must not be <jk>null</jk>.
	 */
	public static void gateMcpServlet(McpResourceServerConfig cfg, HttpServletRequest req, HttpServletResponse res) {
		if (!cfg.isEnabled())
			return;
		if (!isEffectivePost(req))
			return;
		assertOriginRootMount(req);
		authenticate(cfg, req, res);
	}

	/**
	 * Returns whether this request resolves to the MCP JSON-RPC dispatch operation ({@link McpEndpointMixin#handleMcpRequest}).
	 *
	 * <p>
	 * <b>Why this cannot drift from the router (B1/H3):</b> instead of reconstructing the MCP endpoint's path from a
	 * hardcoded constant &mdash; which the previous gate did, so a {@code @Mixin(path="/api")} re-mount routed
	 * {@code POST /api/mcp} to the handler while the gate compared {@code ["api","mcp"]} against {@code ["mcp"]} and
	 * silently skipped authentication &mdash; this identifies the <i>operation</i> the router actually selected:
	 * <ul>
	 * 	<li><b>Mixin sub-context</b> (the {@code McpEndpoint} mixin's {@code @RestStartCall} fires from
	 * 		{@code RestSession.run()} <i>after</i> the operation is resolved): the resolved Java method is available, so we
	 * 		gate iff it is {@link McpEndpointMixin#handleMcpRequest} (or an override).  No path logic at all &mdash; zero drift,
	 * 		and re-mounts / multi-mounts are handled for free because the router already routed to the op.
	 * 	<li><b>Host {@code implements McpEndpoint}</b> (the host's {@code @RestStartCall} fires <i>before</i> routing, so the
	 * 		resolved op is not yet known): we look up the dispatch operation's OWN {@link org.apache.juneau.rest.server.util.UrlPathMatcher}
	 * 		objects from {@link RestContext#getRestOperations()} and match the request against them &mdash; reusing the router's
	 * 		own matchers, so re-mounts, {@code /*}, trailing slashes and percent-encoding are all handled exactly as the router
	 * 		handles them.
	 * </ul>
	 *
	 * @param ctx The host {@link RestContext} handling this request.
	 * @param req The raw HTTP request.
	 * @return <jk>true</jk> if the request resolves to the MCP dispatch operation.
	 */
	static boolean resolvesToMcpDispatch(RestContext ctx, HttpServletRequest req) {
		return resolveMcpDispatchOp(ctx, req) != null;
	}

	/**
	 * Returns the resolved MCP JSON-RPC dispatch operation's {@link RestOpContext} for this request, or
	 * <jk>null</jk> if the request does not resolve to it.
	 *
	 * <p>
	 * Same two-case resolution as {@link #resolvesToMcpDispatch(RestContext, HttpServletRequest)}, but returns the
	 * matched operation's own {@link RestOpContext} (rather than a boolean) so callers can also inspect the
	 * operation's owning {@link RestContext#getMixinMountPrefixes() mixin mount prefixes} &mdash; e.g. to detect a
	 * {@code @Mixin(path/paths)} re-mount, the same signal {@link #assertWellKnownMountReachable(RestRequest)} uses
	 * for the well-known route.
	 *
	 * @param ctx The host {@link RestContext} handling this request.
	 * @param req The raw HTTP request.
	 * @return The resolved MCP dispatch operation's {@link RestOpContext}, or <jk>null</jk>.
	 */
	static RestOpContext resolveMcpDispatchOp(RestContext ctx, HttpServletRequest req) {
		var opSession = ctx.getLocalSession().getOpSessionOrNull();
		if (opSession != null) {
			var oc = opSession.getContext();
			return isMcpDispatchMethod(oc.getJavaMethod()) ? oc : null;
		}
		return matchMcpDispatchOp(ctx, req);
	}

	/**
	 * Returns whether the specified Java method is the MCP JSON-RPC dispatch operation
	 * ({@link McpEndpointMixin#handleMcpRequest}, or an override of it).
	 *
	 * @param m The resolved operation's Java method.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if it is the MCP dispatch method.
	 */
	static boolean isMcpDispatchMethod(Method m) {
		return m != null
			&& MCP_DISPATCH_METHOD.equals(m.getName())
			&& McpEndpointMixin.class.isAssignableFrom(m.getDeclaringClass());
	}

	/**
	 * Returns whether the request path matches the MCP dispatch operation's own router matchers (host
	 * {@code implements McpEndpoint} case, where the resolved op is not yet available).
	 *
	 * @param ctx The host {@link RestContext} handling this request.
	 * @param req The raw HTTP request.
	 * @return The MCP dispatch operation's {@link RestOpContext} if the request path matches its matchers, else
	 * 	<jk>null</jk>.
	 */
	static RestOpContext matchMcpDispatchOp(RestContext ctx, HttpServletRequest req) {
		var ops = ctx.getRestOperations();
		if (ops == null)
			return null;
		var urlPath = UrlPath.of(RestUtils.getPathInfoUndecoded(req));
		for (var oc : ops.getOpContexts())
			if (isMcpDispatchMethod(oc.getJavaMethod()))
				for (var matcher : oc.getPathMatchers())
					if (matcher.match(urlPath) != null)
						return oc;
		return null;
	}

	/**
	 * Returns whether this request resolves to a {@code POST} once method-override channels are honored (H3).
	 *
	 * <p>
	 * Mirrors {@code RestSession.getMethod()}: an explicit {@code POST}, or a {@code ?method=POST} query parameter, or an
	 * {@code X-Method: POST} header.  Because a {@code @RestStartCall} hook cannot see the host's
	 * {@code allowedMethodParams}/{@code allowedMethodHeaders} allow-lists, any override to {@code POST} is treated as a
	 * {@code POST} &mdash; this is fail-closed: the only cost is that a {@code ?method=POST} aimed at the MCP path on a
	 * host that does <i>not</i> allow overrides is challenged rather than 404'd, which never weakens the gate.
	 */
	static boolean isEffectivePost(HttpServletRequest req) {
		if ("POST".equalsIgnoreCase(req.getMethod()))
			return true;
		var mp = RestUtils.parseQuery(req.getQueryString()).get("method");
		if (mp != null && !mp.isEmpty() && "POST".equalsIgnoreCase(mp.get(0)))
			return true;
		return "POST".equalsIgnoreCase(req.getHeader("X-Method"));
	}

	/**
	 * Fails fast when this endpoint is not mounted at the origin root (READY-312f F2, H1).
	 *
	 * <p>
	 * RFC 9728 requires the Protected Resource Metadata document at an origin-root well-known location
	 * ({@code https://host/.well-known/oauth-protected-resource[/path]}), which is exactly the URL advertised in the
	 * {@code resource_metadata} challenge parameter.  Juneau registers the well-known {@code @RestGet} routes
	 * servlet-relative, so on any non-origin-root mount (a non-empty servlet path or servlet-context path) the advertised
	 * URL would resolve to a different location and 404.  Rather than silently advertising an unreachable document, RS
	 * auth refuses to operate on such a mount.
	 *
	 * <p>
	 * A container that reports the root context / root servlet mapping as {@code "/"} (rather than the empty string) is
	 * treated as origin-root &mdash; the tolerance is symmetric across {@code contextPath} and {@code servletPath} (H1 LOW).
	 *
	 * @param req The HTTP request.
	 * @throws InternalServerError Always, when a non-root context path or non-root servlet path is present.
	 */
	static void assertOriginRootMount(HttpServletRequest req) {
		var ctx = emptyIfNull(req.getContextPath());
		var svl = emptyIfNull(req.getServletPath());
		if (!isOriginRootPath(ctx) || !isOriginRootPath(svl))
			throw new InternalServerError(
				"MCP resource-server auth requires an origin-root mount so the advertised RFC 9728 well-known PRM URL "
				+ "resolves; found contextPath='%s' servletPath='%s'.  Mount the MCP endpoint at the origin root "
				+ "(no servlet path / no context path).",
				ctx, svl);
	}

	/** Returns whether a context/servlet path denotes the origin root ({@code ""} or {@code "/"}). */
	private static boolean isOriginRootPath(String p) {
		return p.isEmpty() || p.equals("/");
	}

	/**
	 * Fails fast when the well-known PRM route cannot be served from the origin root (READY-312f F2, H1 + secondary MEDIUM).
	 *
	 * <p>
	 * Extends {@link #assertOriginRootMount(HttpServletRequest)} with the {@code @Mixin(path/paths)} re-mount case: when the
	 * MCP endpoint is mounted as a mixin under one or more host-chosen prefixes, the well-known {@code @RestGet} routes are
	 * re-mounted along with every other mixin op (to {@code /<prefix>/.well-known/...}), so the origin-root PRM URL
	 * advertised in the {@code resource_metadata} challenge is unreachable.  {@code contextPath}/{@code servletPath} are both
	 * empty in that case (the host is root-mounted), so the servlet-path check alone cannot detect it &mdash; we consult the
	 * resolved operation's {@link RestContext#getMixinMountPrefixes() mixin mount prefixes} instead, and fail fast with the
	 * same origin-root diagnostic so discovery can never silently advertise an unreachable URL.
	 *
	 * @param req The REST request (its resolved operation identifies the possibly re-mounted sub-context).
	 * @throws InternalServerError When a non-root mount or a {@code @Mixin(path/paths)} re-mount is detected.
	 */
	static void assertWellKnownMountReachable(RestRequest req) {
		assertOriginRootMount(req.getHttpServletRequest());
		var opCtx = req.getOpContext();
		assertNotMixinRemounted(opCtx == null ? List.of() : opCtx.getContext().getMixinMountPrefixes());
	}

	/**
	 * Fails fast when the resolved MCP operation is mounted under one or more host-chosen {@code @Mixin(path/paths)}
	 * re-mount prefixes (READY-312f F2, H1 unification) &mdash; the SAME diagnostic used by
	 * {@link #assertWellKnownMountReachable(RestRequest)}, shared so the {@code POST} bearer gate
	 * ({@link #gateMcpEndpoint}) and the well-known route never disagree on whether a given mount is supported.
	 *
	 * @param mixinMountPrefixes The resolved operation's owning {@link RestContext#getMixinMountPrefixes() mixin
	 * 	mount prefixes}; empty when the operation is not mixin-mounted (or not re-mounted).
	 * @throws InternalServerError When {@code mixinMountPrefixes} is non-empty.
	 */
	private static void assertNotMixinRemounted(List<String> mixinMountPrefixes) {
		if (!mixinMountPrefixes.isEmpty())
			throw new InternalServerError(
				"MCP resource-server auth requires an origin-root mount so the advertised RFC 9728 well-known PRM URL "
				+ "resolves; the MCP endpoint is re-mounted under host-chosen @Mixin prefix(es) %s, which moves the "
				+ "well-known PRM route off the origin root.  Mount the MCP mixin at the origin root (no @Mixin(path/paths)).",
				mixinMountPrefixes);
	}

	/**
	 * Serves this endpoint's PRM from the SEP-2351 root well-known location (fallback), failing fast on a non-root or
	 * re-mounted mount (H1); a no-op-throwing {@code 404} when RS auth is disabled.
	 *
	 * @param cfg The RS config.
	 * @param req The REST request.
	 * @return The PRM document.
	 */
	public static McpProtectedResourceMetadata metadataForWellKnownRoot(McpResourceServerConfig cfg, RestRequest req) {
		if (!cfg.isEnabled())
			throw new NotFound();
		assertWellKnownMountReachable(req);
		return buildMetadata(cfg);
	}

	/**
	 * Serves this endpoint's PRM from the RFC 9728 path-inserted well-known location, but only when the requested suffix
	 * matches this resource's expected well-known path (M5); otherwise {@code 404}.  Fails fast on a non-root or re-mounted
	 * mount (H1); a {@code 404} when RS auth is disabled.
	 *
	 * @param cfg The RS config.
	 * @param req The REST request.
	 * @return The PRM document.
	 */
	public static McpProtectedResourceMetadata metadataForWellKnownPathInserted(McpResourceServerConfig cfg, RestRequest req) {
		if (!cfg.isEnabled())
			throw new NotFound();
		assertWellKnownMountReachable(req);
		var expected = McpWellKnownRouting.wellKnownRequestPath(cfg.getResource());
		if (!expected.equals(trimTrailingSlash(req.getHttpServletRequest().getPathInfo())))
			throw new NotFound();
		return buildMetadata(cfg);
	}

	private static String trimTrailingSlash(String p) {
		if (p == null)
			return "";
		while (p.length() > 1 && p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		return p;
	}

	/**
	 * Builds the RFC 9728 Protected Resource Metadata document from the config.
	 *
	 * @param cfg The RS config.  Must be {@link McpResourceServerConfig#isEnabled() enabled} and valid.
	 * @return The PRM bean.
	 */
	public static McpProtectedResourceMetadata buildMetadata(McpResourceServerConfig cfg) {
		cfg.validateEnabled();
		var m = new McpProtectedResourceMetadata().setResource(cfg.getResource());
		if (!cfg.getAuthorizationServers().isEmpty())
			m.setAuthorizationServers(new ArrayList<>(cfg.getAuthorizationServers()));
		if (!cfg.getScopesSupported().isEmpty())
			m.setScopesSupported(new LinkedHashSet<>(cfg.getScopesSupported()));
		if (!cfg.getBearerMethodsSupported().isEmpty())
			m.setBearerMethodsSupported(new LinkedHashSet<>(cfg.getBearerMethodsSupported()));
		return m;
	}

	/**
	 * Returns the path-inserted well-known PRM URI advertised in the {@code resource_metadata} challenge parameter.
	 *
	 * @param cfg The RS config.
	 * @return The PRM document URI.
	 */
	public static URI metadataUri(McpResourceServerConfig cfg) {
		return McpWellKnownRouting.metadataUri(cfg.getResource());
	}

	/**
	 * Authenticates the request against the RS config, establishing the authenticated principal.
	 *
	 * <p>
	 * On success the principal is stashed under {@link RestServerConstants#PRINCIPAL_ATTR} and returned.  On failure the
	 * {@code WWW-Authenticate: Bearer ...} challenge (carrying the {@code resource_metadata} pointer) is written directly
	 * to {@code res} &mdash; because an exception thrown from a {@code @RestStartCall} hook does not route through the
	 * response processors that would otherwise copy an {@link AuthenticationException}'s headers &mdash; and then a
	 * {@code 401 Unauthorized} ({@link AuthenticationException}) or {@code 403 Forbidden} (insufficient scope) is thrown.
	 *
	 * @param cfg The RS config.  Must be {@link McpResourceServerConfig#isEnabled() enabled} and valid.
	 * @param req The HTTP request.  Must not be <jk>null</jk>.
	 * @param res The HTTP response, used to write the challenge header on failure.  Must not be <jk>null</jk>.
	 * @return The authenticated principal.  Never <jk>null</jk>.
	 */
	public static Principal authenticate(McpResourceServerConfig cfg, HttpServletRequest req, HttpServletResponse res) {
		cfg.validateEnabled();
		var filter = cfg.oauthFilter();
		Optional<AuthResult> result;
		try {
			result = filter.authenticate(req);
		} catch (AuthenticationException e) {
			res.setHeader(WWW_AUTHENTICATE, invalidTokenChallenge(cfg));
			throw new AuthenticationException(e, "Invalid bearer token");
		}
		if (result.isEmpty()) {
			res.setHeader(WWW_AUTHENTICATE, missingTokenChallenge(cfg));
			throw new AuthenticationException("Bearer token required");
		}
		var auth = result.get();
		var principal = auth.getPrincipal();
		if (!McpAudienceValidator.matches(principal, cfg.getAudience(), cfg.isRequireAudienceClaim())) {
			res.setHeader(WWW_AUTHENTICATE, invalidTokenChallenge(cfg));
			throw new AuthenticationException("Token audience mismatch");
		}
		if (!satisfies(auth.getRoles(), cfg.getRequiredScopes())) {
			res.setHeader(WWW_AUTHENTICATE, insufficientScopeChallenge(cfg));
			throw new Forbidden("Insufficient scope");
		}
		// M1: write the framework-standard principal attribute so @Auth Principal resolves downstream.
		req.setAttribute(RestServerConstants.PRINCIPAL_ATTR, principal);
		// SEP-2350: stash the granted scopes so the POST-parse dispatch point can enforce per-operation step-up scopes.
		req.setAttribute(GRANTED_SCOPES_ATTR, Set.copyOf(auth.getRoles()));
		return principal;
	}

	/**
	 * Returns the granted scopes stashed by {@link #authenticate} for this request (SEP-2350).
	 *
	 * @param req The HTTP request.  May be <jk>null</jk>.
	 * @return The granted scopes, or an empty set when none were stashed (RS auth disabled / gate not run).
	 */
	@SuppressWarnings("unchecked")
	public static Set<String> grantedScopes(HttpServletRequest req) {
		if (req == null)
			return Set.of();
		var v = req.getAttribute(GRANTED_SCOPES_ATTR);
		return v instanceof Set ? (Set<String>)v : Set.of();
	}

	/**
	 * Returns the authenticated {@link Principal} {@link #authenticate} stashed for this request (READY-312f F4).
	 *
	 * <p>
	 * Reads the same framework-standard {@link RestServerConstants#PRINCIPAL_ATTR} attribute {@link #authenticate}
	 * writes on success, so this is the single source of truth for "who is calling this MCP request".  It is the seam
	 * the {@code 2026-07-28} dispatcher threads into {@link RequestStateCodec#seal}/{@link RequestStateCodec#unseal}
	 * for principal-bound {@code requestState} AAD.  Mirrors {@link #grantedScopes(HttpServletRequest)}:
	 * a <jk>null</jk> request, an absent attribute, or a non-{@link Principal} value all return <jk>null</jk> &mdash; the
	 * anonymous / RS-auth-disabled path, which every caller must handle without an NPE.  Unlike
	 * {@link #grantedScopes(HttpServletRequest)}, which falls back to an empty {@link Set} in that same situation, this
	 * returns <jk>null</jk> rather than an empty value in that case; the divergence is intentional &mdash; a scalar
	 * identity has no natural "empty" representative the way a collection does.
	 *
	 * @param req The HTTP request.  May be <jk>null</jk>.
	 * @return The authenticated principal, or <jk>null</jk> when none was stashed (RS auth disabled / gate not run /
	 * 	anonymous caller).
	 */
	public static Principal principal(HttpServletRequest req) {
		if (req == null)
			return null;
		var v = req.getAttribute(RestServerConstants.PRINCIPAL_ATTR);
		return v instanceof Principal p ? p : null;
	}

	/**
	 * Enforces SEP-2350 per-operation step-up scopes at the POST-parse dispatch point.
	 *
	 * <p>
	 * A no-op unless RS auth is {@link McpResourceServerConfig#isEnabled() enabled} and the resolved operation has a
	 * non-empty {@link McpResourceServerConfig#requiredScopesFor(McpOperationContext) required-scope set}.  When the
	 * caller's {@code grantedScopes} do not {@link #satisfies satisfy} the operation's required scopes, throws a
	 * {@code 403 Forbidden} carrying a {@code WWW-Authenticate: Bearer ..., error="insufficient_scope", scope="..."}
	 * step-up challenge that names exactly the scopes required for this operation.
	 *
	 * <p>
	 * This is the enforcement point the F2 security review mandated: unlike the {@code @RestStartCall} bearer gate (which
	 * fires before the JSON-RPC body is parsed), this runs once the MCP method &mdash; and thus the operation &mdash; is
	 * known, so the challenge can be scoped to the specific operation.  It layers on top of, and never weakens, the
	 * baseline gate (op-identity, origin-root fail-fast, audience, and baseline required-scope enforcement all still run
	 * first in {@link #authenticate}).
	 *
	 * @param cfg The RS config.  Must not be <jk>null</jk>.
	 * @param grantedScopes The caller's granted scopes.  May be <jk>null</jk> (treated as empty).
	 * @param ctx The resolved operation.  Must not be <jk>null</jk>.
	 * @throws Forbidden {@code 403 insufficient_scope} when the granted scopes do not satisfy the operation's required
	 * 	scopes.
	 */
	public static void enforceOperationScopes(McpResourceServerConfig cfg, Collection<String> grantedScopes, McpOperationContext ctx) {
		if (!cfg.isEnabled())
			return;
		var required = cfg.requiredScopesFor(ctx);
		if (required.isEmpty())
			return;
		var granted = grantedScopes == null ? Set.<String>of() : grantedScopes;
		if (satisfies(granted, required))
			return;
		var e = new Forbidden("Insufficient scope for operation '%s'", ctx.method());
		e.setHeader(WWW_AUTHENTICATE, stepUpChallenge(cfg, required));
		throw e;
	}

	/**
	 * Builds the SEP-2350 scoped step-up {@code WWW-Authenticate} challenge naming the scopes required to invoke a
	 * specific operation ({@code error="insufficient_scope"}, with the operation's {@code scope} set).
	 *
	 * @param cfg The RS config.
	 * @param operationScopes The scopes required to invoke the operation.
	 * @return The challenge value.
	 */
	public static String stepUpChallenge(McpResourceServerConfig cfg, Collection<String> operationScopes) {
		return McpBearerChallenge.create()
			.realm(cfg.getRealm())
			.error("insufficient_scope")
			.scope(operationScopes)
			.resourceMetadata(metadataUri(cfg))
			.build();
	}

	/**
	 * Returns whether the granted scopes satisfy every required scope, using exact string equality.
	 *
	 * <p>
	 * A required scope {@code r} is satisfied only when some granted scope {@code g} is exactly equal to it.  OAuth
	 * scopes have no universal hierarchy, so a granted {@code repo} does <b>not</b> imply a required {@code repo:write}
	 * or {@code repo.admin} &mdash; a broad-but-low-privilege scope must never authorize a differently named privileged
	 * operation. Applications that want scope implication must expand their granted scopes themselves before calling
	 * this method; this method does not provide a hierarchical-implication SPI.
	 *
	 * @param grantedScopes The caller's granted scopes.  Never <jk>null</jk>.
	 * @param requiredScopes The scopes required to invoke the operation.  Never <jk>null</jk>.
	 * @return <jk>true</jk> if every required scope is satisfied.
	 */
	public static boolean satisfies(Collection<String> grantedScopes, Collection<String> requiredScopes) {
		return grantedScopes.containsAll(requiredScopes);
	}

	/**
	 * Builds the {@code WWW-Authenticate} challenge for a request that presented no bearer credentials (RFC 6750
	 * &sect;3: no {@code error} code).
	 *
	 * <p>
	 * Carries a {@code scope} hint listing the endpoint-wide baseline required scopes (SEP-2350) when any are
	 * configured, so a client can request them on its first authorization; the hint is omitted (no {@code scope=""})
	 * when no baseline scope is required.
	 *
	 * @param cfg The RS config.
	 * @return The challenge value.
	 */
	public static String missingTokenChallenge(McpResourceServerConfig cfg) {
		return McpBearerChallenge.create()
			.realm(cfg.getRealm())
			.scope(cfg.getRequiredScopes())  // SEP-2350: hint the baseline scope on the initial 401 (no-op when empty).
			.resourceMetadata(metadataUri(cfg))
			.build();
	}

	/**
	 * Builds the {@code WWW-Authenticate} challenge for an invalid/expired token or an audience mismatch
	 * ({@code error="invalid_token"}).
	 *
	 * @param cfg The RS config.
	 * @return The challenge value.
	 */
	public static String invalidTokenChallenge(McpResourceServerConfig cfg) {
		return McpBearerChallenge.create()
			.realm(cfg.getRealm())
			.error("invalid_token")
			.resourceMetadata(metadataUri(cfg))
			.build();
	}

	/**
	 * Builds the {@code WWW-Authenticate} challenge for a token missing a required baseline scope
	 * ({@code error="insufficient_scope"}, with the required {@code scope} set).
	 *
	 * @param cfg The RS config.
	 * @return The challenge value.
	 */
	public static String insufficientScopeChallenge(McpResourceServerConfig cfg) {
		return McpBearerChallenge.create()
			.realm(cfg.getRealm())
			.error("insufficient_scope")
			.scope(cfg.getRequiredScopes())
			.resourceMetadata(metadataUri(cfg))
			.build();
	}
}
