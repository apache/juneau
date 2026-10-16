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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.BoundaryChecks.*;

import jakarta.servlet.http.*;

/**
 * The cross-site request forgery boundary for an application reachable from the network: decides whether a
 * state-changing request came from this application's own page, or from a client that is not a browser, and refuses
 * it when it came from some other site's page.
 *
 * <h5 class='section'>What this answers, and what it does not</h5>
 * <p>
 * This class answers one question:
 * <p class='bcode'>
 * 	Did this request come from this app's own page, or from a non-browser client?
 * </p>
 * <p>
 * It does <b>not</b> answer <i>who is the caller</i> (authentication) or <i>did the user mean it</i> (intent).  A
 * user who has the application's own page open, and is induced to click something on it, produces a request that
 * passes every check here.  Intent needs its own, separate gate.
 *
 * <p>
 * For an application bound only to the loopback interface, {@link LoopbackBoundary} pins a single known origin and is
 * stricter.  This class is for applications whose own origin is whatever the request's {@code Host} says it is.
 *
 * <h5 class='section'>The checks</h5>
 * <p>
 * {@link #check(HttpServletRequest)} runs these steps in order, and the first failure wins.  The method judged is the
 * {@linkplain MethodSafety#effectiveMethod(String, String, java.util.function.UnaryOperator) effective method}, so a
 * {@code method} query parameter or an override header cannot carry a write past them.
 * <table class='styled w800'>
 * 	<tr><th>Step</th><th>Applies to</th><th>Refused when</th><th>Reason</th><th>Status</th></tr>
 * 	<tr><td>Allowed hosts</td><td>Every request, when {@link Builder#allowedHosts(String...)} is set</td>
 * 		<td>The request's own host is not listed; or its {@code Host} (on HTTP/2, its {@code :authority}) is missing or
 * 		malformed</td>
 * 		<td>{@code HOST_NOT_ALLOWED}; {@code HOST_MISSING}, {@code HOST_MALFORMED}</td>
 * 		<td>Not listed: 421; missing or malformed {@code Host}: 400</td></tr>
 * 	<tr><td>Safe method</td><td>{@code GET}, {@code HEAD}, {@code OPTIONS}, {@code TRACE}</td>
 * 		<td>Never: allowed</td><td></td><td></td></tr>
 * 	<tr><td>Own origin</td><td>Unsafe requests</td>
 * 		<td>{@code Host} (on HTTP/2, {@code :authority}) is absent, repeated, or not a host with an optional port</td>
 * 		<td>{@code HOST_MISSING}, {@code HOST_MALFORMED}</td><td>400</td></tr>
 * 	<tr><td>Forwarded origin</td><td>Unsafe requests, and safe ones when {@code allowedHosts} is set, when
 * 		{@link Builder#trustedForwarding(ForwardedSource)} is on</td>
 * 		<td>The forwarded host or proto is missing, malformed, or has fewer or unequal entries than the trusted hop count;
 * 		or the container's view disagrees with it</td>
 * 		<td>{@code FORWARDED_HOST_MISSING}, {@code FORWARDED_PROTO_MISSING}, {@code FORWARDED_HOST_MALFORMED},
 * 		{@code FORWARDED_PROTO_MALFORMED}, {@code FORWARDED_HOPS_INSUFFICIENT}, {@code FORWARDED_HOPS_MISMATCH},
 * 		{@code FORWARDED_CONTAINER_MISMATCH}</td><td>403</td></tr>
 * 	<tr><td>{@code Origin} present</td><td>Unsafe requests</td>
 * 		<td>The literal {@code null}; or repeated, unparseable or not this server's own origin; or
 * 		{@code Sec-Fetch-Site} present and not exactly {@code same-origin}</td>
 * 		<td>{@code ORIGIN_NULL}, {@code ORIGIN_MISMATCH}, {@code FETCH_SITE_NOT_SAME_ORIGIN}</td><td>403</td></tr>
 * 	<tr><td>{@code Origin} absent</td><td>Unsafe requests</td>
 * 		<td>{@code Sec-Fetch-Site} present and not exactly {@code same-origin}; or, with no fetch metadata either, the
 * 		{@linkplain NonBrowser non-browser policy} refuses it</td>
 * 		<td>{@code FETCH_SITE_NOT_SAME_ORIGIN}, {@code SIMPLE_REQUEST_WITHOUT_ORIGIN}, {@code ORIGIN_MISSING}</td>
 * 		<td>403</td></tr>
 * 	<tr><td>Browser request</td><td>Unsafe requests from a page</td>
 * 		<td>{@link Builder#requireJson(boolean)} is on and the type is not JSON; or the token header is missing, or
 * 		does not verify</td>
 * 		<td>{@code CONTENT_TYPE_NOT_JSON} (415), {@code CSRF_TOKEN_MISSING}, {@code CSRF_TOKEN_MISMATCH}</td>
 * 		<td>403</td></tr>
 * </table>
 *
 * <p>
 * Origins are compared as normalized {@code (scheme, host, port)} triples, never as raw text: scheme and host are
 * lower-cased (ASCII only), an absent port means the scheme's default, and an explicit port is decimal with no sign
 * and no leading zero.  Nothing else is re-canonicalized, so {@code 127.1}, {@code [::ffff:127.0.0.1]} and a trailing
 * dot each fail to match any other spelling of the same address.  That is fail-closed, by design.
 *
 * <h5 class='section'>Non-browser clients</h5>
 * <p>
 * A request with neither {@code Origin} nor {@code Sec-Fetch-Site} is handled by {@link Builder#nonBrowser(NonBrowser)}.
 * The default, {@link NonBrowser#ADMIT_NON_SIMPLE}, admits only requests a browser could not have sent cross-site
 * without a CORS preflight, which this boundary never answers.  <b>That relies on browsers sending {@code Origin} on
 * every non-simple request they issue; current browsers do, and the {@code Origin} check is the real control for
 * them.</b>
 *
 * <h5 class='section'>The token is accepted only in the header</h5>
 * <p>
 * The token is read from the {@link #csrfHeader()} request header and nowhere else: never a query parameter, never a
 * form field.  A plain HTML {@code <form>} cannot set a header, so every HTML form POST, including the application's
 * own, is refused with {@code CSRF_TOKEN_MISSING}.  This boundary is for applications whose writes go through script
 * that sends the header.
 *
 * <h5 class='section'>The request's own origin, and proxies</h5>
 * <p>
 * By default ({@link OriginSource#HOST_HEADER}) the request's own origin is {@link HttpServletRequest#getScheme()} plus
 * the {@code Host} header (on HTTP/2, the {@code :authority}; see below).  {@code X-Forwarded-*} and {@code Forwarded}
 * are never read by this class unless {@link Builder#trustedForwarding(ForwardedSource)} is set (see below).  Behind a
 * proxy that terminates TLS, use {@link OriginSource#CONTAINER} and configure the container's own forwarded-header
 * support; the boundary then takes the origin from {@code getScheme()}, {@code getServerName()} and
 * {@code getServerPort()}.
 * <p>
 * <b>Under {@code CONTAINER} the boundary must run after the container's forwarded-header rewrite.</b>  Jetty's
 * {@code ForwardedRequestCustomizer}, Tomcat's {@code RemoteIpValve} and Spring Boot's
 * {@code forward-headers-strategy=native} rewrite the request before any filter runs, so order does not matter.
 * Spring Boot's {@code forward-headers-strategy=framework} registers {@code ForwardedHeaderFilter} at
 * {@code Ordered.HIGHEST_PRECEDENCE}, so this boundary's filter must be registered strictly later, never at
 * {@code HIGHEST_PRECEDENCE} itself.  The boundary cannot see its own order; a misordered deployment shows up as
 * scheme-mismatch refusals and the warning described below.
 *
 * <h5 class='section'>Trusting a forwarded origin</h5>
 * <p>
 * If your container cannot rewrite the request itself, or does not do so for WebSocket upgrades, use
 * {@link Builder#trustedForwarding(ForwardedSource)}.  The request's own origin is then the forwarded host
 * <b>and</b> proto of the one header family named ({@code X-Forwarded-Host} plus {@code X-Forwarded-Proto}, or
 * {@code host=} plus {@code proto=} in one {@code Forwarded} element), on HTTP requests and WebSocket handshakes
 * alike.  The other family and {@code X-Forwarded-Port} are never read; put a non-default port in the host value.
 * <ul>
 * 	<li><b>Turn it on only if every request reaches the server through your proxy, and the proxy removes or appends
 * 		after any client-supplied copy.</b>  A server clients can reach directly lets them choose the origin, and that
 * 		defeats {@link Builder#allowedHosts(String...)} for reads.
 * 	<li>Set {@link Builder#trustedHops(int)} to exactly the number of proxies that <i>append</i>.  A chain of
 * 		overwriting proxies stays at 1.  <b>Never set it higher:</b> an overstated count trusts a value the client wrote.
 * 	<li>A missing, malformed, short or misaligned value is refused ({@code FORWARDED_*}, 403), never guessed at, and
 * 		nothing falls back to {@code Host}.  A proxy that sets only {@code X-Forwarded-Proto} is not supported.
 * 	<li>{@link Builder#allowedHosts(String...)} is checked against the forwarded host, so list the public names.
 * 	<li>Under {@link OriginSource#CONTAINER} the container's rewritten origin, port included, must equal the forwarded
 * 		one, or the request is refused ({@code FORWARDED_CONTAINER_MISMATCH}); configure both to read the same header
 * 		family and hop.  Do not enable this where the container consumes the headers (Spring
 * 		{@code forward-headers-strategy=framework}): every request is then refused as missing.  Under
 * 		{@link OriginSource#HOST_HEADER} and on handshakes, a container host that equals an entry left of the selected
 * 		one is refused too, because the container trusted a hop the client could have written.
 * 	<li>A handshake is judged only through {@link #checkHandshake(String, HandshakeHosts, Map)}.
 * </ul>
 *
 * <h5 class='section'>HTTP/2 and {@code :authority}</h5>
 * <p>
 * An HTTP/2 request has no {@code Host} header; its {@code :authority} pseudo-header plays that part, and servlet
 * containers surface it only through {@link HttpServletRequest#getServerName()} and
 * {@link HttpServletRequest#getServerPort()}.  So that a deployment that speaks HTTP/2 keeps working, a request with
 * no {@code Host} header whose {@link HttpServletRequest#getProtocol()} is exactly {@code HTTP/2.0} is judged on its
 * server name and port in {@code Host}'s place, for both {@link OriginSource}s and for
 * {@link Builder#allowedHosts(String...)}.  Any other request without a {@code Host} is still refused
 * ({@code HOST_MISSING}).  A request that does carry a {@code Host} header is judged on it as before.
 * <p>
 * The same holds for a WebSocket handshake carried over HTTP/2 (RFC 8441, extended {@code CONNECT}): with no
 * {@code Host}, and no {@code Sec-WebSocket-Key} or {@code Upgrade} header to mark it an RFC 6455 request, the
 * authority of the handshake's request URI stands in for {@code Host}.  Jetty advertises RFC 8441 by default once
 * HTTP/2 is enabled; Tomcat does not support it.
 * <p>
 * Because the server name is what the container's forwarded-header support rewrites, <b>under
 * {@link OriginSource#HOST_HEADER} an HTTP/2 request is judged on the forwarded host</b> when such support is
 * enabled (Tomcat's {@code RemoteIpValve} with a host header, Spring's {@code ForwardedHeaderFilter}, Jetty's
 * {@code ForwardedRequestCustomizer}).  {@code HOST_HEADER} is therefore protocol-dependent: it reads the raw
 * {@code Host} on HTTP/1.1 and the container's server name on HTTP/2.  Enable forwarded-header support only behind a
 * proxy that overwrites {@code X-Forwarded-*}, or list the expected hosts with
 * {@link Builder#allowedHosts(String...)}.
 *
 * <h5 class='section'>Non-goals</h5>
 * <ul>
 * 	<li><b>DNS rebinding</b>, unless {@link Builder#allowedHosts(String...)} is set: a rebound page's {@code Origin} and
 * 		{@code Host} agree.  TLS defeats rebinding; a plain-HTTP intranet application should list its host names.
 * 	<li><b>Cross-origin callers (CORS).</b>  Refused.  The boundary adds no {@code Access-Control-Allow-*} header; do
 * 		not put an origin-reflecting CORS layer in front of it.
 * 	<li><b>Authentication, intent, session management.</b>  The boundary never changes a session id and never sets
 * 		cookie attributes.
 * 	<li><b>Script running inside the page</b> (XSS, a malicious extension) passes every check.
 * 	<li><b>HTML form POSTs</b> and {@code Referer} checking.
 * 	<li><b>Telling a trustworthy proxy from a client</b> (a peer-address allow-list).  See
 * 		{@link Builder#trustedForwarding(ForwardedSource)}.
 * </ul>
 *
 * <h5 class='section'>What an application must do</h5>
 * <ul>
 * 	<li>Call {@link HttpServletRequest#changeSessionId()} on login.  A per-session token follows the session id, so
 * 		without that a token seen before login stays valid after it.
 * 	<li>Mark the session cookie {@code HttpOnly} and {@code SameSite}, and do not scope it to a parent {@code Domain}.
 * 	<li>Not use URL rewriting for session ids.
 * </ul>
 *
 * <h5 class='section'>Responses and logging</h5>
 * <p>
 * A rejection message names the header at fault and the kind of value expected.  It never echoes a value the request
 * presented, never prints this server's own origin, and never reveals whether a session exists.  Each refusal is
 * logged at {@code FINE} without the query string, the token header, {@code Cookie} or {@code Authorization}; the raw
 * {@code Origin} and {@code Host} appear only at {@code FINEST}, stripped of control characters and truncated.  Three
 * misconfiguration patterns are logged once per process at {@code WARNING}: a scheme-only mismatch, a port-only
 * mismatch, and repeated missing tokens from the application's own origin.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	CsrfBoundary <jv>boundary</jv> = CsrfBoundary.<jsm>create</jsm>()
 * 		.allowedHosts(<js>"app.example.com"</js>)
 * 		.build();
 * 	<jv>servletContext</jv>.addFilter(<js>"csrf"</js>, <jk>new</jk> RequestBoundaryFilter(<jv>boundary</jv>))
 * 		.addMappingForUrlPatterns(<jk>null</jk>, <jk>false</jk>, <js>"/*"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jic'>{@link RequestBoundary}
 * 	<li class='jc'>{@link RequestBoundaryFilter}
 * 	<li class='jc'>{@link CsrfTokens}
 * 	<li class='jc'>{@link LoopbackBoundary}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names (e.g. Origin); a constant per header would obscure the wire contract.
})
public class CsrfBoundary implements RequestBoundary {

	/**
	 * How an unsafe request with neither {@code Origin} nor {@code Sec-Fetch-Site} is treated.
	 */
	public enum NonBrowser {

		/** Refuse it ({@code ORIGIN_MISSING}).  For applications with no non-browser callers. */
		REJECT,

		/** Admit it only with a valid token, exactly as for a browser request. */
		REQUIRE_TOKEN,

		/**
		 * The default.  Admit it, with no token, only when a browser could not have sent it cross-site without a
		 * preflight: the request-line method is not {@code GET}, {@code HEAD} or {@code POST}, or there is exactly one
		 * {@code Content-Type} and its base type is none of {@code application/x-www-form-urlencoded},
		 * {@code multipart/form-data} and {@code text/plain}.  Everything else is refused
		 * ({@code SIMPLE_REQUEST_WITHOUT_ORIGIN}).
		 */
		ADMIT_NON_SIMPLE
	}

	/**
	 * Where the request's own origin, which {@code Origin} must equal, comes from.
	 */
	public enum OriginSource {

		/**
		 * The default.  {@link HttpServletRequest#getScheme()} and the {@code Host} header, or, when
		 * {@link Builder#trustedForwarding(ForwardedSource)} is set, the forwarded origin in their place.
		 */
		HOST_HEADER,

		/**
		 * {@link HttpServletRequest#getScheme()}, {@link HttpServletRequest#getServerName()} and
		 * {@link HttpServletRequest#getServerPort()}, as rewritten by the container's own forwarded-header support.
		 * A {@code Host} header is still required (on HTTP/2, the {@code :authority} stands in for it).  See the class
		 * Javadoc for the filter-ordering rule.
		 */
		CONTAINER
	}

	/**
	 * The one header family whose forwarded origin the boundary trusts.  See
	 * {@link Builder#trustedForwarding(ForwardedSource)}.
	 */
	public enum ForwardedSource {

		/** The default.  No forwarded header is read on any path. */
		NONE,

		/** {@code X-Forwarded-Host} and {@code X-Forwarded-Proto}. */
		X_FORWARDED,

		/** {@code host=} and {@code proto=} of the RFC 7239 {@code Forwarded} header. */
		FORWARDED
	}

	private static final Logger LOG = Logger.getLogger(CsrfBoundary.class.getName());

	private static final Set<String> SIMPLE_METHODS = Set.of("GET", "HEAD", "POST");

	/** The number of token-missing refusals from the app's own origin after which the embed warning is logged. */
	private static final int MISSING_TOKEN_WARN_AT = 10;

	/** Longest raw header value written to the {@code FINEST} log. */
	private static final int MAX_LOGGED = 200;

	private static final AtomicBoolean SCHEME_WARNED = new AtomicBoolean();
	private static final AtomicBoolean PORT_WARNED = new AtomicBoolean();
	private static final AtomicInteger MISSING_FROM_OWN_ORIGIN = new AtomicInteger();

	private final CsrfTokens tokens;
	private final NonBrowser nonBrowser;
	private final OriginSource originSource;
	private final boolean requireJson;
	private final Set<String> allowedHosts;
	private final String csrfHeader;
	private final boolean createSessions;
	private final ForwardedSource trustedForwarding;
	private final int trustedHops;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.  Must not be <jk>null</jk>.
	 */
	protected CsrfBoundary(Builder builder) {
		reqnn("builder", builder);
		this.tokens = builder.tokens != null ? builder.tokens : CsrfTokens.perSession();
		this.nonBrowser = builder.nonBrowser;
		this.originSource = builder.originSource;
		this.requireJson = builder.requireJson;
		this.allowedHosts = Set.copyOf(builder.allowedHosts);
		this.csrfHeader = builder.csrfHeader;
		this.createSessions = builder.createSessions;
		this.trustedForwarding = builder.trustedForwarding;
		this.trustedHops = builder.trustedHops;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Applies the boundary to a request.
	 *
	 * @param req The request to check.  Must not be <jk>null</jk>.
	 * @return {@link BoundaryResult#ALLOWED} when every applicable check passed, else the first rejection encountered.
	 */
	@Override /* RequestBoundary */
	public BoundaryResult check(HttpServletRequest req) {
		reqnn("req", req);

		// Judged on the method the request claims, so an override cannot carry a write past the checks below.
		var method = MethodSafety.effectiveMethod(req.getMethod(), req.getQueryString(), req::getHeader);

		var hosts = BoundaryChecks.hostValues(req);
		Origin self;
		if (trustedForwarding == ForwardedSource.NONE) {
			self = hosts.size() == 1 ? selfOrigin(req, BoundaryChecks.parseHost(hosts.get(0))) : null;
		} else {
			// Judged only where the own origin is needed, so a direct health probe still passes.
			if (MethodSafety.isSafe(method) && allowedHosts.isEmpty())
				return BoundaryResult.ALLOWED;
			var forwarded = forwardedSelf(req, method, hosts);
			if (forwarded.refusal() != null)
				return forwarded.refusal();
			self = forwarded.origin();
		}

		// The rebinding control runs on every request: a read is as exfiltratable as a write.
		if (! allowedHosts.isEmpty()) {
			if (self == null)
				return hostFailure(req, method, hosts);
			if (! allowedHosts.contains(self.host()))
				return refuse(req, method, BoundaryReason.HOST_NOT_ALLOWED, 421,
					"Request 'Host' is not a host name this server answers to.");
		}

		if (MethodSafety.isSafe(method))
			return BoundaryResult.ALLOWED;

		if (self == null)
			return hostFailure(req, method, hosts);

		var origins = BoundaryChecks.headerValues(req, "Origin");
		var fetchSites = BoundaryChecks.headerValues(req, "Sec-Fetch-Site");
		var originMatched = false;

		// A blank Origin is still an Origin: a browser sent the header, so it must not reach the non-browser branch.
		if (! origins.isEmpty()) {
			if (origins.size() == 1 && "null".equalsIgnoreCase(origins.get(0)))
				return refuse(req, method, BoundaryReason.ORIGIN_NULL, 403,
					"A state-changing request must not carry an opaque 'null' value in 'Origin'.");
			var parsed = origins.size() == 1 ? BoundaryChecks.parseOrigin(origins.get(0)) : null;
			if (parsed == null || ! parsed.equals(self)) {
				if (parsed != null)
					warnNearMiss(parsed, self);
				return refuse(req, method, BoundaryReason.ORIGIN_MISMATCH, 403,
					"Request 'Origin' must be a single header naming this server's own origin.");
			}
			if (! fetchSites.isEmpty() && ! isSameOriginFetch(fetchSites))
				return refuseFetchSite(req, method);
			originMatched = true;
		} else if (! fetchSites.isEmpty()) {
			if (! isSameOriginFetch(fetchSites))
				return refuseFetchSite(req, method);
		} else {
			switch (nonBrowser) {
				case REJECT:
					return refuse(req, method, BoundaryReason.ORIGIN_MISSING, 403,
						"A state-changing request must carry an 'Origin' header.");
				case REQUIRE_TOKEN:
					break;
				default:
					if (! isNonSimple(req))
						return refuse(req, method, BoundaryReason.SIMPLE_REQUEST_WITHOUT_ORIGIN, 403,
							"A state-changing request with no 'Origin' header must use a method other than GET, HEAD or POST, "
							+ "or carry a single 'Content-Type' that is not a form type.");
					return requireJson && ! isSingleJson(req) ? refuseNotJson(req, method) : BoundaryResult.ALLOWED;
			}
		}

		return checkBrowser(req, method, originMatched);
	}

	/**
	 * Whether a WebSocket handshake's {@code Origin} value is well-formed enough to be checked later, once the
	 * handshake's {@code Host} is visible.
	 *
	 * <p>
	 * For a container's {@code checkOrigin} hook, which sees the {@code Origin} value and nothing else, so it can
	 * refuse the upgrade with {@code 403} before it happens.  A missing or blank value, the literal {@code null}, and
	 * anything that is not exactly <c>scheme "://" host [":" port]</c> with an {@code http} or {@code https} scheme
	 * are refused.  A well-formed value says nothing yet about whether it is this server's own origin: that is
	 * {@link #checkHandshake(String, List, List, List)}'s job.
	 *
	 * @param origin The {@code Origin} header value.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the value parses as an http(s) origin.
	 */
	public boolean acceptsHandshakeOrigin(String origin) {
		return BoundaryChecks.parseOrigin(origin) != null;
	}

	/**
	 * Applies this boundary to a WebSocket handshake.
	 *
	 * <p>
	 * A WebSocket upgrade is a {@code GET}, so {@link #check(HttpServletRequest)} lets it through untouched; this
	 * method is the boundary for the upgrade instead.  No token can be sent on a handshake, so the control is
	 * {@code Origin} and {@code Sec-Fetch-Site}:
	 * <ol>
	 * 	<li>{@code Host} must be a single well-formed value ({@code HOST_MISSING}, {@code HOST_MALFORMED}).  A handshake
	 * 		carried over HTTP/2 (RFC 8441) has none, and its request URI's authority stands in; see {@link HandshakeHosts}.
	 * 	<li>The handshake's own scheme comes from its request URI: {@code ws} is {@code http}, {@code wss} is
	 * 		{@code https}, and {@code http} or {@code https} are used as they are.  Any other scheme, or none, means the
	 * 		handshake's own origin is unknown, and it is refused ({@code ORIGIN_MISMATCH}).
	 * 	<li>When {@link Builder#allowedHosts(String...)} is set, the {@code Host} must be one of them
	 * 		({@code HOST_NOT_ALLOWED}).
	 * 	<li>{@code Origin} must be a single value naming the same scheme, host and port as the handshake's own
	 * 		scheme and {@code Host} ({@code ORIGIN_MISSING}, {@code ORIGIN_NULL}, {@code ORIGIN_MISMATCH}).
	 * 	<li>{@code Sec-Fetch-Site}, when present, must be a single {@code same-origin} value
	 * 		({@code FETCH_SITE_NOT_SAME_ORIGIN}).
	 * </ol>
	 *
	 * <p>
	 * With {@link Builder#trustedForwarding(ForwardedSource)} set, the forwarded origin replaces the {@code Host} and
	 * scheme in the steps above, {@code allowedHosts} is checked against the forwarded host, and a missing or malformed
	 * forwarded header is refused with a {@code FORWARDED_*} reason.
	 *
	 * <p>
	 * The {@code Host} header (or, over HTTP/2, the request URI's authority) is the handshake's own host under either
	 * {@link OriginSource}.  {@code X-Forwarded-*} and {@code Forwarded} are never read unless {@link Builder#trustedForwarding(ForwardedSource)}
	 * is set, which only {@link #checkHandshake(String, HandshakeHosts, Map)} supports.  Behind a proxy that terminates
	 * TLS, the scheme is right only if the
	 * container's own forwarded-header support rewrites the request URI it hands the WebSocket layer; where it does
	 * not, a {@code wss} page's handshake is refused rather than admitted on a guess.
	 *
	 * @param scheme The scheme of the handshake's request URI.  Can be <jk>null</jk>, which is refused.
	 * @param hosts Every {@code Host} value of the handshake.  Must not be <jk>null</jk>.
	 * @param origins Every {@code Origin} value of the handshake.  Must not be <jk>null</jk>.
	 * @param fetchSites Every {@code Sec-Fetch-Site} value of the handshake.  Must not be <jk>null</jk>.
	 * @return {@link BoundaryResult#ALLOWED}, or the first rejection.  A rejection message never echoes a value the
	 * 	handshake presented.
	 */
	public BoundaryResult checkHandshake(String scheme, List<String> hosts, List<String> origins, List<String> fetchSites) {
		reqnn("hosts", hosts);
		return checkHandshake(scheme, new HandshakeHosts(hosts, false), origins, fetchSites);
	}

	/**
	 * Applies this boundary to a WebSocket handshake whose {@code Host} values may have been synthesised.
	 *
	 * <p>
	 * As {@link #checkHandshake(String, List, List, List)}, except that {@code hosts} may carry the value built from the
	 * request URI's authority when a handshake over HTTP/2 (RFC 8441) has no {@code Host} header.  The synthesised value
	 * is judged exactly as a real one would be.
	 *
	 * @param scheme The scheme of the handshake's request URI.  Can be <jk>null</jk>, which is refused.
	 * @param hosts The handshake's {@code Host} values.  Must not be <jk>null</jk>.
	 * @param origins Every {@code Origin} value of the handshake.  Must not be <jk>null</jk>.
	 * @param fetchSites Every {@code Sec-Fetch-Site} value of the handshake.  Must not be <jk>null</jk>.
	 * @return {@link BoundaryResult#ALLOWED}, or the first rejection.  A rejection message never echoes a value the
	 * 	handshake presented.
	 */
	public BoundaryResult checkHandshake(String scheme, HandshakeHosts hosts, List<String> origins, List<String> fetchSites) {
		reqnn("hosts", hosts);
		reqnn("origins", origins);
		reqnn("fetchSites", fetchSites);
		return handshake(scheme, hosts, origins, fetchSites, null);
	}

	/**
	 * Applies this boundary to a WebSocket handshake, reading every header it needs from the handshake's header map.
	 *
	 * <p>
	 * As {@link #checkHandshake(String, HandshakeHosts, List, List)}, with {@code Origin} and {@code Sec-Fetch-Site}
	 * taken from {@code headers} (every value, across all spellings of the name).  This is the form to use when
	 * {@link Builder#trustedForwarding(ForwardedSource)} is set, because only it can see the forwarded headers; the
	 * other forms always refuse such a handshake with {@code FORWARDED_HOST_MISSING}.  A forwarded header the map holds
	 * under more than one spelling is refused, because the order of its values across spellings is unknowable.
	 *
	 * @param scheme The scheme of the handshake's request URI.  Can be <jk>null</jk>, which is refused.
	 * @param hosts The handshake's {@code Host} values.  Must not be <jk>null</jk>.
	 * @param headers The handshake's headers.  Can be <jk>null</jk>, which is empty.
	 * @return {@link BoundaryResult#ALLOWED}, or the first rejection.  A rejection message never echoes a value the
	 * 	handshake presented.
	 */
	public BoundaryResult checkHandshake(String scheme, HandshakeHosts hosts, Map<String,List<String>> headers) {
		reqnn("hosts", hosts);
		Function<String,List<String>> lookup = name -> handshakeHeader(headers, name);
		return handshake(scheme, hosts, handshakeHeader(headers, "Origin"), handshakeHeader(headers, "Sec-Fetch-Site"), lookup);
	}

	/**
	 * Every value of a handshake header in wire order, or <jk>null</jk> when the map holds the name under more than one
	 * spelling (the order across spellings is lost).  Empty when absent.
	 */
	private static List<String> handshakeHeader(Map<String,List<String>> headers, String name) {
		var l = new ArrayList<String>();
		var spellings = 0;
		if (headers != null) {
			for (var e : headers.entrySet()) {
				if (name.equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
					spellings++;
					for (var v : e.getValue())
						if (v != null)
							l.add(v);
				}
			}
		}
		if (spellings > 1) {
			// Origin and Sec-Fetch-Site are counted, not ordered: they stay a plain list.  Forwarded headers are refused.
			return name.equalsIgnoreCase("Origin") || name.equalsIgnoreCase("Sec-Fetch-Site") ? l : null;
		}
		return l;
	}

	private BoundaryResult handshake(String scheme, HandshakeHosts hosts, List<String> origins, List<String> fetchSites,
			Function<String,List<String>> forwardedHeaders) {
		var values = hosts.values();
		var host = values.size() == 1 ? BoundaryChecks.parseHost(values.get(0)) : null;
		if (host == null) {
			if (values.isEmpty())
				return refuseHandshake(BoundaryReason.HOST_MISSING, 400,
					"A WebSocket handshake must carry a 'Host' header (or, over HTTP/2, ':authority').", hosts, origins);
			return refuseHandshake(BoundaryReason.HOST_MALFORMED, 400,
				"Handshake 'Host' must be a single header holding a host name, IPv4 address or bracketed IPv6 address, "
				+ "with an optional port.", hosts, origins);
		}

		var ownScheme = BoundaryChecks.handshakeScheme(scheme);
		if (ownScheme == null)
			return refuseHandshake(BoundaryReason.ORIGIN_MISMATCH, 403,
				"The WebSocket handshake's own scheme could not be determined, so its 'Origin' cannot be checked.", hosts, origins);

		var self = new Origin(ownScheme, host.host(), host.port() >= 0 ? host.port() : BoundaryChecks.defaultPort(ownScheme));
		if (trustedForwarding != ForwardedSource.NONE) {
			// With no header access (the list-based forms) the forwarded host is missing, never guessed.
			var resolved = forwardedHeaders == null
				? new ForwardedOrigin.Resolved(null, List.of(), false, BoundaryReason.FORWARDED_HOST_MISSING)
				: ForwardedOrigin.resolve(trustedForwarding, trustedHops, forwardedHeaders);
			if (resolved.reason() != null)
				return refuseHandshake(resolved.reason(), 403, forwardedMessage(resolved.reason()), hosts, origins);
			if (containerDisagrees(self, resolved))
				return refuseHandshake(BoundaryReason.FORWARDED_CONTAINER_MISMATCH, 403,
					forwardedMessage(BoundaryReason.FORWARDED_CONTAINER_MISMATCH), hosts, origins);
			self = resolved.origin();
		}

		if (! allowedHosts.isEmpty() && ! allowedHosts.contains(self.host()))
			return refuseHandshake(BoundaryReason.HOST_NOT_ALLOWED, 421,
				"Handshake 'Host' is not a host name this server answers to.", hosts, origins);

		if (origins.isEmpty())
			return refuseHandshake(BoundaryReason.ORIGIN_MISSING, 403, "A WebSocket handshake must carry an 'Origin' header.", hosts, origins);
		if (origins.size() == 1 && "null".equalsIgnoreCase(origins.get(0)))
			return refuseHandshake(BoundaryReason.ORIGIN_NULL, 403,
				"A WebSocket handshake must not carry an opaque 'null' value in 'Origin'.", hosts, origins);
		var parsed = origins.size() == 1 ? BoundaryChecks.parseOrigin(origins.get(0)) : null;
		if (parsed == null || ! parsed.equals(self)) {
			if (parsed != null)
				warnNearMiss(parsed, self);
			return refuseHandshake(BoundaryReason.ORIGIN_MISMATCH, 403,
				"Handshake 'Origin' must be a single header naming this server's own origin.", hosts, origins);
		}

		if (! fetchSites.isEmpty() && ! isSameOriginFetch(fetchSites))
			return refuseHandshake(BoundaryReason.FETCH_SITE_NOT_SAME_ORIGIN, 403,
				"Handshake 'Sec-Fetch-Site' must be a single 'same-origin' value when present.", hosts, origins);

		return BoundaryResult.ALLOWED;
	}

	/**
	 * The lines of a forwarded header; empty when absent; <jk>null</jk> when the container will not enumerate them but
	 * holds a value, because one line would then stand for all and could be the client's.
	 */
	private static List<String> forwardedLines(HttpServletRequest req, String name) {
		var e = req.getHeaders(name);
		if (e == null)
			return req.getHeader(name) == null ? List.of() : null;
		var l = new ArrayList<String>();
		while (e.hasMoreElements())
			l.add(e.nextElement());
		return l;
	}

	/**
	 * Whether a container's view {@code c} of the request is one the forwarded origin must not replace.
	 *
	 * <p>
	 * Used where the container's view is the transport (a handshake, or HTTP under {@code HOST_HEADER}): equal views
	 * agree, and an unequal one is replaced, except when its host is one the forwarded header carries to the left of the
	 * selected entry, or when such an entry did not parse (a container may normalize it into a host of its own), which
	 * means the container may have trusted a hop the client could have written.
	 */
	private static boolean containerDisagrees(Origin c, ForwardedOrigin.Resolved f) {
		return ! c.host().equals(f.origin().host()) && (f.leftUnparsed() || f.leftHosts().contains(c.host()));
	}

	/** The fixed text for a forwarded-host refusal. */
	private static String forwardedMessage(BoundaryReason reason) {
		return switch (reason) {
			case FORWARDED_HOST_MISSING -> "A trusted-forwarding boundary requires the forwarded host header; none was usable.";
			case FORWARDED_PROTO_MISSING -> "A trusted-forwarding boundary requires the forwarded proto alongside the forwarded host.";
			case FORWARDED_HOST_MALFORMED -> "The forwarded host header is malformed.";
			case FORWARDED_PROTO_MALFORMED -> "The forwarded proto must be 'http' or 'https'.";
			case FORWARDED_HOPS_INSUFFICIENT -> "The forwarded header holds fewer entries than the trusted hop count.";
			case FORWARDED_HOPS_MISMATCH -> "The forwarded host and proto headers hold different numbers of entries.";
			case FORWARDED_CONTAINER_MISMATCH -> "The container's view of the request disagrees with the forwarded origin.";
			default -> throw iaex("Not a forwarded-host reason: {0}", reason);
		};
	}

	/** The outcome of {@link #forwardedSelf}: the origin to judge against, or the refusal. */
	private record Self(Origin origin, BoundaryResult refusal) {}

	/** The own origin of an HTTP request when trusted forwarding is on. */
	private Self forwardedSelf(HttpServletRequest req, String method, List<String> hosts) {
		var host = hosts.size() == 1 ? BoundaryChecks.parseHost(hosts.get(0)) : null;
		if (host == null)
			return new Self(null, hostFailure(req, method, hosts));
		var resolved = ForwardedOrigin.resolve(trustedForwarding, trustedHops, name -> forwardedLines(req, name));
		if (resolved.reason() != null)
			return new Self(null, refuse(req, method, resolved.reason(), 403, forwardedMessage(resolved.reason())));
		var c = selfOrigin(req, host);
		// Under CONTAINER the container is declared a forwarding authority, so its view must be exactly the forwarded one.
		var disagrees = originSource == OriginSource.CONTAINER ? ! resolved.origin().equals(c) : containerDisagrees(c, resolved);
		if (disagrees)
			return new Self(null, refuse(req, method, BoundaryReason.FORWARDED_CONTAINER_MISMATCH, 403,
				forwardedMessage(BoundaryReason.FORWARDED_CONTAINER_MISMATCH)));
		return new Self(resolved.origin(), null);
	}

	/** Logs a refused handshake and builds its result.  {@code message} must be a fixed text, as for {@code refuse}. */
	private static BoundaryResult refuseHandshake(BoundaryReason reason, int status, String message, HandshakeHosts hosts, List<String> origins) {
		LOG.fine(() -> "CSRF boundary refused a WebSocket handshake: reason=" + reason);
		if (LOG.isLoggable(Level.FINEST))
			LOG.finest("CSRF boundary refused a WebSocket handshake: reason=" + reason
				+ ", Origin=" + sanitize(String.join(", ", origins))
				+ ", Host=" + (hosts.synthesised() ? "(h2 :authority) " : "") + sanitize(String.join(", ", hosts.values())));
		return new BoundaryResult(reason, status, message);
	}

	/** The token and JSON checks for a request that came from a page, or that the policy holds to a page's rules. */
	private BoundaryResult checkBrowser(HttpServletRequest req, String method, boolean originMatched) {
		if (requireJson && ! isSingleJson(req))
			return refuseNotJson(req, method);

		var presented = BoundaryChecks.headerValues(req, csrfHeader);
		if (presented.isEmpty() || (presented.size() == 1 && presented.get(0).isBlank())) {
			if (originMatched && MISSING_FROM_OWN_ORIGIN.get() < MISSING_TOKEN_WARN_AT
					&& MISSING_FROM_OWN_ORIGIN.incrementAndGet() == MISSING_TOKEN_WARN_AT)
				LOG.warning("CSRF boundary: " + MISSING_TOKEN_WARN_AT + " requests from this server's own origin arrived "
					+ "without the '" + csrfHeader + "' header.  Does the page embed the token, or is this an HTML form POST?");
			return refuse(req, method, BoundaryReason.CSRF_TOKEN_MISSING, 403,
				"A state-changing request must carry the CSRF token in the '" + csrfHeader + "' header.");
		}
		// The same answer whether or not a session exists: with none, the comparison is against a value nothing matches.
		if (presented.size() != 1 || ! tokens.verify(presented.get(0), tokens.expected(req, false)))
			return refuse(req, method, BoundaryReason.CSRF_TOKEN_MISMATCH, 403,
				"The CSRF token in the '" + csrfHeader + "' header is not valid for this request.");

		return BoundaryResult.ALLOWED;
	}

	/**
	 * The request's own origin, or <jk>null</jk> if it cannot be derived.
	 *
	 * @param host The parsed {@code Host} header, or <jk>null</jk> if it is malformed.  Required in both modes, because
	 * 	{@code getServerName()} falls back to the server's own name when the request has no {@code Host}.  On HTTP/2, which
	 * 	has none, it is the server name and port the container derived from {@code :authority}.
	 */
	private Origin selfOrigin(HttpServletRequest req, Authority host) {
		if (host == null)
			return null;
		var scheme = req.getScheme() == null ? null : req.getScheme().toLowerCase(Locale.ROOT);
		if (originSource == OriginSource.CONTAINER) {
			// Tomcat reports an IPv6 server name without the brackets a Host header would carry.
			var serverName = req.getServerName();
			if (serverName != null && serverName.indexOf(':') >= 0 && ! serverName.startsWith("["))
				serverName = "[" + serverName + "]";
			var name = BoundaryChecks.parseHost(serverName);
			if (name == null || name.port() >= 0)
				return null;
			var port = req.getServerPort();
			return new Origin(scheme, name.host(), port > 0 ? port : BoundaryChecks.defaultPort(scheme));
		}
		return new Origin(scheme, host.host(), host.port() >= 0 ? host.port() : BoundaryChecks.defaultPort(scheme));
	}

	/** Non-simple in the CORS sense: a browser would have preflighted it cross-site. */
	private static boolean isNonSimple(HttpServletRequest req) {
		var m = req.getMethod() == null ? null : req.getMethod().toUpperCase(Locale.ROOT);
		if (m != null && ! SIMPLE_METHODS.contains(m))
			return true;
		// Counted, never read through getContentType(): two headers can be reported as one non-form value.
		var types = BoundaryChecks.headerValues(req, "Content-Type");
		return types.size() == 1 && ! types.get(0).isBlank() && types.get(0).indexOf(',') < 0
			&& ! BoundaryChecks.isSafelistedType(types.get(0));
	}

	private static boolean isSingleJson(HttpServletRequest req) {
		var types = BoundaryChecks.headerValues(req, "Content-Type");
		return types.size() == 1 && BoundaryChecks.isJson(types.get(0));
	}

	private static boolean isSameOriginFetch(List<String> values) {
		return values.size() == 1 && BoundaryChecks.isSameOriginFetch(values.get(0));
	}

	private static BoundaryResult hostFailure(HttpServletRequest req, String method, List<String> hosts) {
		if (hosts.isEmpty())
			return refuse(req, method, BoundaryReason.HOST_MISSING, 400, "A request must carry a 'Host' header (or, on HTTP/2, ':authority').");
		return refuse(req, method, BoundaryReason.HOST_MALFORMED, 400,
			"Request 'Host' must be a single header holding a host name, IPv4 address or bracketed IPv6 address, "
			+ "with an optional port.");
	}

	private static BoundaryResult refuseFetchSite(HttpServletRequest req, String method) {
		return refuse(req, method, BoundaryReason.FETCH_SITE_NOT_SAME_ORIGIN, 403,
			"Request 'Sec-Fetch-Site' must be a single 'same-origin' value when present on a state-changing request.");
	}

	private static BoundaryResult refuseNotJson(HttpServletRequest req, String method) {
		return refuse(req, method, BoundaryReason.CONTENT_TYPE_NOT_JSON, 415,
			"A state-changing request must carry a single 'Content-Type' of 'application/json'.");
	}

	/**
	 * Logs the refusal and builds its result.
	 *
	 * <p>
	 * {@code message} must be a fixed text naming only headers and the kind of value expected.
	 */
	private static BoundaryResult refuse(HttpServletRequest req, String method, BoundaryReason reason, int status, String message) {
		if (LOG.isLoggable(Level.FINE))
			LOG.fine("CSRF boundary refused a request: reason=" + reason
				+ ", method=" + sanitize(method)
				+ ", requestMethod=" + sanitize(req.getMethod())
				+ ", uri=" + sanitize(pathOnly(req.getRequestURI()))
				+ ", remoteAddr=" + sanitize(req.getRemoteAddr()));
		if (LOG.isLoggable(Level.FINEST))
			LOG.finest("CSRF boundary refused a request: reason=" + reason
				+ ", Origin=" + sanitize(String.join(", ", BoundaryChecks.headerValues(req, "Origin")))
				+ ", Host=" + (BoundaryChecks.isSynthesisedHost(req) ? "(h2 :authority) " : "")
				+ sanitize(String.join(", ", BoundaryChecks.hostValues(req))));
		return new BoundaryResult(reason, status, message);
	}

	/** One warning per process for an Origin that differs from the own origin only in the way a misconfiguration would. */
	private static void warnNearMiss(Origin presented, Origin self) {
		if (! presented.host().equals(self.host()))
			return;
		if (! presented.scheme().equals(self.scheme())) {
			if (SCHEME_WARNED.compareAndSet(false, true))
				LOG.warning("CSRF boundary: a request's 'Origin' differs from this server's own origin only in its scheme.  "
					+ "TLS terminated upstream?  See originSource(CONTAINER) and filter ordering.");
		} else if (presented.port() != self.port() && PORT_WARNED.compareAndSet(false, true)) {
			LOG.warning("CSRF boundary: a request's 'Origin' differs from this server's own origin only in its port.  "
				+ "Is a proxy or port mapping rewriting 'Host'?");
		}
	}

	/** The URI with any query and any {@code ;} path parameters (where a container may put a session id) cut off. */
	private static String pathOnly(String uri) {
		if (uri == null)
			return null;
		var end = uri.length();
		for (var c : new char[] { '?', ';', '#' }) {
			var i = uri.indexOf(c);
			if (i >= 0 && i < end)
				end = i;
		}
		return uri.substring(0, end);
	}

	/** A value safe for one log line: control characters removed, at most {@value #MAX_LOGGED} characters. */
	private static String sanitize(String value) {
		if (value == null)
			return "null";
		var sb = new StringBuilder(Math.min(value.length(), MAX_LOGGED));
		for (var i = 0; i < value.length() && sb.length() < MAX_LOGGED; i++) {
			var c = value.charAt(i);
			if (c >= 0x20 && c != 0x7f)
				sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Clears the once-per-process warning state, so each warning can be observed again.
	 */
	static void resetWarnings() {
		SCHEME_WARNED.set(false);
		PORT_WARNED.set(false);
		MISSING_FROM_OWN_ORIGIN.set(0);
	}

	/**
	 * A freshly masked token for the page being served to this request.
	 *
	 * <p>
	 * Never creates a session: with a session-scoped strategy and no session, the value returned verifies against
	 * nothing.  Creating the session, when that is wanted, is {@link RequestBoundaryFilter}'s job.
	 *
	 * @param req The request whose response will embed the token.  Must not be <jk>null</jk>.
	 * @return 128 lower-case hex characters, different on every call.
	 */
	@Override /* RequestBoundary */
	public String tokenFor(HttpServletRequest req) {
		reqnn("req", req);
		return tokens.mask(tokens.expected(req, false));
	}

	@Override /* RequestBoundary */
	public String csrfHeader() { return csrfHeader; }

	/**
	 * The token strategy.
	 *
	 * @return The token strategy.  Never <jk>null</jk>.
	 */
	public CsrfTokens tokens() { return tokens; }

	/**
	 * The policy for unsafe requests with neither {@code Origin} nor {@code Sec-Fetch-Site}.
	 *
	 * @return The policy.  Never <jk>null</jk>.
	 */
	public NonBrowser nonBrowser() { return nonBrowser; }

	/**
	 * Where the request's own origin comes from.
	 *
	 * @return The origin source.  Never <jk>null</jk>.
	 */
	public OriginSource originSource() { return originSource; }

	/**
	 * The header family whose forwarded origin is trusted.
	 *
	 * @return The source.  Never <jk>null</jk>.  {@link ForwardedSource#NONE} when forwarding is not trusted.
	 */
	public ForwardedSource trustedForwarding() { return trustedForwarding; }

	/**
	 * The number of trusted proxy entries; the one this far from the right of the forwarded header is used.
	 *
	 * @return The hop count, 1 to 16.
	 */
	public int trustedHops() { return trustedHops; }

	/**
	 * Whether every admitted unsafe request must carry a single {@code application/json} content type.
	 *
	 * @return <jk>true</jk> if JSON is required.
	 */
	public boolean requireJson() { return requireJson; }

	/**
	 * The host names every request's own host must be one of, lower-cased.
	 *
	 * @return An unmodifiable set; empty when the check is off.
	 */
	public Set<String> allowedHosts() { return allowedHosts; }

	/**
	 * Whether {@link RequestBoundaryFilter} may create a session so that a page it serves can carry a session-scoped
	 * token.
	 *
	 * @return <jk>true</jk> if sessions may be created.
	 */
	public boolean createSessions() { return createSessions; }

	/**
	 * Returns a description of the configuration that does <b>not</b> include any key or token.
	 *
	 * @return A secret-free description.
	 */
	@Override /* Object */
	public String toString() {
		return "CsrfBoundary(tokens=" + tokens + ", nonBrowser=" + nonBrowser + ", originSource=" + originSource
			+ ", requireJson=" + requireJson + ", allowedHosts=" + new TreeSet<>(allowedHosts) + ", csrfHeader=" + csrfHeader
			+ ", createSessions=" + createSessions + ", trustedForwarding=" + trustedForwarding + ", trustedHops=" + trustedHops + ")";
	}

	/**
	 * Builder for {@link CsrfBoundary}.
	 */
	public static class Builder {

		CsrfTokens tokens;
		NonBrowser nonBrowser = NonBrowser.ADMIT_NON_SIMPLE;
		OriginSource originSource = OriginSource.HOST_HEADER;
		boolean requireJson;
		Set<String> allowedHosts = Set.of();
		String csrfHeader = LoopbackBoundary.DEFAULT_CSRF_HEADER;
		boolean createSessions = true;
		ForwardedSource trustedForwarding = ForwardedSource.NONE;
		int trustedHops = 1;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * The token strategy.
		 *
		 * @param value The strategy.  Must not be <jk>null</jk>.  Defaults to a new {@link CsrfTokens#perSession()}.
		 * @return This object.
		 */
		public Builder tokens(CsrfTokens value) {
			tokens = reqnn("value", value);
			return this;
		}

		/**
		 * The policy for unsafe requests with neither {@code Origin} nor {@code Sec-Fetch-Site}.
		 *
		 * @param value The policy.  Must not be <jk>null</jk>.  Defaults to {@link NonBrowser#ADMIT_NON_SIMPLE}.
		 * @return This object.
		 */
		public Builder nonBrowser(NonBrowser value) {
			nonBrowser = reqnn("value", value);
			return this;
		}

		/**
		 * Where the request's own origin comes from.
		 *
		 * @param value The source.  Must not be <jk>null</jk>.  Defaults to {@link OriginSource#HOST_HEADER}.
		 * @return This object.
		 */
		public Builder originSource(OriginSource value) {
			originSource = reqnn("value", value);
			return this;
		}

		/**
		 * Whether every admitted unsafe request must carry a single {@code application/json} content type.
		 *
		 * <p>
		 * Applies to browser and admitted non-browser requests alike.  Parameters are allowed; {@code +json} types are
		 * not.
		 *
		 * @param value The setting.  Defaults to <jk>false</jk>.
		 * @return This object.
		 */
		public Builder requireJson(boolean value) {
			requireJson = value;
			return this;
		}

		/**
		 * The host names this server answers to.  When set, every request, safe ones included, whose own host is not
		 * one of them is refused with {@code 421 HOST_NOT_ALLOWED}.
		 *
		 * <p>
		 * Matching is case-insensitive and ignores the port.  A trailing-dot variant is a different name.  Replaces any
		 * earlier list.  This is the DNS-rebinding control for applications served over plain HTTP.
		 *
		 * @param values Host names with no port, e.g. {@code "app.example.com"} or {@code "[::1]"}.  Must not be
		 * 	<jk>null</jk>, and no entry may be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code values} or an entry is <jk>null</jk>, blank, or not a bare host.
		 */
		public Builder allowedHosts(String...values) {
			reqnn("values", values);
			var s = new LinkedHashSet<String>();
			for (var v : values) {
				reqnn("values", v);
				if (v.isBlank())
					throw iaex("Argument 'values' must not contain a blank entry.");
				var a = BoundaryChecks.parseHost(v.trim());
				if (a == null || a.port() >= 0)
					throw iaex("Argument 'values' must hold bare host names with no port.");
				s.add(a.host());
			}
			allowedHosts = s;
			return this;
		}

		/**
		 * The header an unsafe request must carry the CSRF token in.
		 *
		 * @param value The header name.  Must not be <jk>null</jk> or blank.  Defaults to
		 * 	{@link LoopbackBoundary#DEFAULT_CSRF_HEADER}.
		 * @return This object.
		 */
		public Builder csrfHeader(String value) {
			reqnn("value", value);
			if (value.isBlank())
				throw iaex("Argument 'value' must not be blank.");
			csrfHeader = value.trim();
			return this;
		}

		/**
		 * Whether {@link RequestBoundaryFilter} may create a session so that a page it serves can carry a
		 * session-scoped token.  Has no effect with a per-process token strategy.
		 *
		 * @param value The setting.  Defaults to <jk>true</jk>.
		 * @return This object.
		 */
		public Builder createSessions(boolean value) {
			createSessions = value;
			return this;
		}

		/**
		 * Trusts the forwarded origin one proxy header family carries, for HTTP requests and WebSocket handshakes alike.
		 *
		 * <p>
		 * With a source other than {@link ForwardedSource#NONE}, the request's own origin is the forwarded host
		 * <b>and</b> proto of that one family, and nothing else: there is no fallback to {@code Host} or
		 * {@code getScheme()}, and {@code X-Forwarded-Port} is never read.  A request missing either value, or with a
		 * malformed one, is refused with a {@code FORWARDED_*} reason.  See {@link #trustedHops(int)} for how an entry
		 * is selected.
		 *
		 * <p>
		 * <b>Turn this on only if every request reaches the server through a proxy that removes or appends after any
		 * client-supplied copy of the header.</b>  A server clients can reach directly lets them choose the origin.
		 *
		 * @param value The source.  Must not be <jk>null</jk>.  Defaults to {@link ForwardedSource#NONE}.
		 * @return This object.
		 */
		public Builder trustedForwarding(ForwardedSource value) {
			trustedForwarding = reqnn("value", value);
			return this;
		}

		/**
		 * The number of proxies that <i>append</i> to the forwarded header.
		 *
		 * <p>
		 * The entries of the header are counted in wire order across all its lines and commas, and the {@code n}th from
		 * the right is used; entries to its left are ignored.  A chain of proxies that overwrite the header stays at 1.
		 * <b>Never set this above the number of proxies that append:</b> an overstated value selects an entry the client
		 * wrote, and that fails open.  An understated value selects an inner hop and only causes refusals.
		 *
		 * @param value The hop count, 1 to 16.  Defaults to 1.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is outside 1 to 16.
		 */
		public Builder trustedHops(int value) {
			if (value < 1 || value > 16)
				throw iaex("Argument 'value' must be between 1 and 16.");
			trustedHops = value;
			return this;
		}

		/**
		 * Builds the boundary.
		 *
		 * @return A new {@link CsrfBoundary}.
		 * @throws IllegalArgumentException If {@link #trustedHops(int)} is not 1 while no forwarded source is trusted.
		 */
		public CsrfBoundary build() {
			if (trustedHops != 1 && trustedForwarding == ForwardedSource.NONE)
				throw iaex("trustedHops requires trustedForwarding.");
			return new CsrfBoundary(this);
		}
	}
}
