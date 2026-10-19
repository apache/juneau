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

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.rest.server.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Applies a {@link RequestBoundary} to every request reaching the servlet container, rejecting those that did not
 * come from a page this application served.
 *
 * <h5 class='section'>Why a servlet filter, and not a mixin or a guard</h5>
 * <p>
 * The boundary's value depends entirely on there being no way around it, and the two lighter-weight Juneau
 * mechanisms both leave one:
 * <ul>
 * 	<li>A {@link org.apache.juneau.rest.server.guard.RestGuard RestGuard} is declared per resource or per operation, so
 * 		an endpoint added later is unprotected until someone remembers to declare it &mdash; and its omission is
 * 		invisible in review, because nothing about the new endpoint looks different.
 * 	<li>A mixin's {@link org.apache.juneau.rest.server.RestStartCall @RestStartCall} hook fires only for requests
 * 		that resolved to one of that mixin's own endpoints, so a boundary packaged that way would not see the host
 * 		resource's operations at all.
 * </ul>
 * <p>
 * A filter registered at {@code /*} sees every request the container handles: every Juneau resource and mixin,
 * static resources served by the container or by another framework in the same application, and the paths that
 * resolve to nothing and would 404.  Nothing can opt out by omission, which is the property that makes it worth
 * having.
 *
 * <h5 class='section'>Rejection behavior</h5>
 * <p>
 * A rejected request is answered directly and the chain is not invoked, so the request never reaches application
 * code.  The response carries the boundary's chosen status, a {@value #REJECTION_HEADER} header naming the
 * {@link BoundaryReason reason}, {@code Cache-Control: no-store}, and a small JSON body carrying the same reason and a
 * message.
 * <p>
 * A rejection is never rendered as an empty result or a silent no-op.  A security refusal that looks like "no data"
 * teaches the user to ignore it, and the point of answering explicitly is that a genuine misconfiguration &mdash;
 * the application being reached at {@code localhost} when it accepts {@code 127.0.0.1}, say &mdash; is immediately
 * diagnosable instead of presenting as an inexplicably broken page.
 *
 * <h5 class='section'>Token availability to the page renderer</h5>
 * <p>
 * On an allowed request the boundary's {@link RequestBoundary#tokenFor(HttpServletRequest) token for that request}
 * is placed under the {@link #TOKEN_ATTRIBUTE} request attribute, and the header name it must be sent back under is
 * placed under the {@link #HEADER_ATTRIBUTE} sibling attribute, so a page-rendering endpoint can embed both without
 * needing its own reference to the boundary.
 * <p>
 * A page that embeds the token in more than one place should give each place its own value: a {@link CsrfBoundary}
 * masks its token under a fresh pad on every call, so the secret never repeats in the page.  For that the filter also
 * places a {@code Supplier<String>} under {@link #TOKEN_SUPPLIER_ATTRIBUTE}; each {@code get()} returns a new value.
 * Read it from the servlet request and test it with {@code instanceof TokenSupplier}: converting the attribute to a string
 * yields the supplier's own description, not a token.  {@link #TOKEN_ATTRIBUTE} keeps a single value for renderers
 * that read only a string.
 * <p>
 * A response that embeds the token must not be cached, or a cache hands the token to whoever asks next.  The code
 * that writes the token calls {@link #markTokenBearing(HttpServletResponse)} before it writes any of the body; this
 * filter cannot know which responses embed it.
 *
 * <h5 class='section'>Sessions</h5>
 * <p>
 * A {@link CsrfBoundary} with a {@linkplain CsrfTokens#perSession() per-session} token needs the session to exist
 * before the page is written, or the page carries a token that verifies against nothing.  The boundary itself never
 * creates one; this filter does, with {@link HttpServletRequest#getSession(boolean) getSession(true)}, and only when
 * all of these hold:
 * <ul>
 * 	<li>{@link CsrfBoundary#createSessions()} is on (the default);
 * 	<li>the request is allowed and its effective method is safe;
 * 	<li>the request looks like a page load: {@code Sec-Fetch-Mode: navigate} or {@code Sec-Fetch-Dest: document} or
 * 		{@code iframe}; or, with no {@code Sec-Fetch-*} header at all, an {@code Accept} header naming
 * 		{@code text/html}.
 * </ul>
 * <p>
 * So a {@code curl} {@code GET} with {@code Accept: *}{@code /*} never creates a session, while a client that asks
 * for HTML, Juneau's own HTML REST pages included, does.  The filter never changes a session id and never sets cookie
 * attributes; those stay the application's (see {@link CsrfBoundary}).
 *
 * <h5 class='section'>Registration: order and dispatcher types</h5>
 * <p>
 * Map the filter at {@code /*} so nothing can be reached around it.  Register it ahead of any filter that acts on the
 * request on the application's behalf.  A {@link CsrfBoundary} using
 * {@link CsrfBoundary.OriginSource#CONTAINER CONTAINER} is the exception that must come <i>after</i> one filter: the
 * container's forwarded-header rewrite, when that is a filter (Spring Boot's {@code ForwardedHeaderFilter} sits at
 * {@code Ordered.HIGHEST_PRECEDENCE}, so register this one strictly later).
 * <p>
 * The filter judges every request it is handed, whatever its {@link DispatcherType}; it has no dispatch-type check of
 * its own, so which dispatches it sees is decided entirely by the registration.  Register it for
 * {@link DispatcherType#REQUEST REQUEST} only unless there is a reason to do otherwise:
 * <ul>
 * 	<li>an {@code ERROR} dispatch of a refused or failed write may arrive without the token header and be refused in
 * 		turn, replacing the error page with a 403;
 * 	<li>a {@code FORWARD} to an HTML view looks like a page load and can create a session.
 * </ul>
 * <p>
 * {@code ASYNC}, {@code ERROR} and {@code FORWARD} are opt-in.  A container's default registration (a
 * {@code null} dispatcher set in {@code addMappingForUrlPatterns}, or Spring Boot's {@code FilterRegistrationBean}
 * default) is {@code REQUEST} only.
 *
 * <h5 class='section'>Example (Spring Boot):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> FilterRegistrationBean&lt;RequestBoundaryFilter&gt; boundary(RequestBoundary <jv>b</jv>) {
 * 		<jk>var</jk> <jv>reg</jv> = <jk>new</jk> FilterRegistrationBean&lt;&gt;(<jk>new</jk> RequestBoundaryFilter(<jv>b</jv>));
 * 		<jv>reg</jv>.addUrlPatterns(<js>"/*"</js>);
 * 		<jv>reg</jv>.setOrder(Ordered.<jsf>HIGHEST_PRECEDENCE</jsf>);
 * 		<jk>return</jk> <jv>reg</jv>;
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jic'>{@link RequestBoundary}
 * 	<li class='jc'>{@link LoopbackBoundaryFilter}
 * </ul>
 *
 * @since 10.0.0
 */
public class RequestBoundaryFilter implements Filter {

	/** Request attribute under which an allowed request carries the CSRF token value a page embeds. */
	public static final String TOKEN_ATTRIBUTE = "org.apache.juneau.rest.server.filter.csrfToken";

	/**
	 * Request attribute under which an allowed request carries the boundary's CSRF <b>header name</b>
	 * ({@link RequestBoundary#csrfHeader()}).
	 *
	 * <p>
	 * Published as a sibling of {@link #TOKEN_ATTRIBUTE} on the same allowed-request path so a page-rendering
	 * document shell (e.g. the console {@code <@console>} FTL directive) can emit both the {@code csrf-token} meta
	 * value <b>and</b> the header name the client script must send it back under, without needing its own reference
	 * to the boundary.
	 */
	public static final String HEADER_ATTRIBUTE = "org.apache.juneau.rest.server.filter.csrfHeader";

	/**
	 * Request attribute under which an allowed request carries a {@code Supplier<String>} of CSRF token values, one
	 * per {@code get()}.
	 *
	 * <p>
	 * Each value is {@link RequestBoundary#tokenFor(HttpServletRequest)} called afresh, so a masking boundary gives every
	 * embed in a page its own pad.  Read the attribute from the servlet request and check it with
	 * {@code instanceof TokenSupplier}; never convert it to a string.
	 *
	 * <p>
	 * Request code can call {@code setAttribute} on this name, so a reader must trust the value only when it is a
	 * {@link TokenSupplier}, a type only this filter can create.  A value of any other type under this name was put
	 * there by someone else: treat it as "no token" and fail closed, rather than embed what it returns.
	 */
	public static final String TOKEN_SUPPLIER_ATTRIBUTE = "org.apache.juneau.rest.server.filter.csrfTokenSupplier";

	/**
	 * The {@code Supplier<String>} this filter publishes under {@link #TOKEN_SUPPLIER_ATTRIBUTE}.
	 *
	 * <p>
	 * It has no public constructor, so request code that overwrites the attribute cannot produce one; a reader that
	 * accepts only this type ignores any replacement.  Each {@link #get()} is the boundary's token for the request
	 * afresh.
	 */
	public static final class TokenSupplier implements Supplier<String> {

		private final RequestBoundary boundary;
		private final HttpServletRequest req;

		private TokenSupplier(RequestBoundary boundary, HttpServletRequest req) {
			this.boundary = boundary;
			this.req = req;
		}

		@Override /* Supplier */
		public String get() {
			return boundary.tokenFor(req);
		}
	}

	/** Response header naming the {@link BoundaryReason} a request was rejected for. */
	public static final String REJECTION_HEADER = "X-Request-Boundary";

	private final RequestBoundary boundary;

	/**
	 * Constructor.
	 *
	 * @param boundary The boundary to apply to every request.  Must not be <jk>null</jk>.
	 */
	public RequestBoundaryFilter(RequestBoundary boundary) {
		this.boundary = reqnn("boundary", boundary);
	}

	/**
	 * The boundary this filter applies.
	 *
	 * @return The boundary.
	 */
	public RequestBoundary boundary() { return boundary; }

	/**
	 * Marks a response that embeds a CSRF token as one no cache may store.
	 *
	 * <p>
	 * Sets {@code Cache-Control: private, no-store}, replacing any value already set, so an earlier
	 * {@code public, max-age=...} cannot survive alongside it.  Call it before the first byte of the body is written:
	 * once the response is committed, headers can no longer change.
	 *
	 * @param res The response.  Must not be <jk>null</jk>.
	 */
	public static void markTokenBearing(HttpServletResponse res) {
		reqnn("res", res).setHeader("Cache-Control", "private, no-store");
	}

	@Override /* Filter */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (! (req instanceof HttpServletRequest req2) || ! (res instanceof HttpServletResponse res2)) {
			chain.doFilter(req, res);  // HTT: a non-HTTP servlet request cannot reach a filter mapped into an HTTP container.
			return;
		}
		var result = boundary.check(req2);
		if (! result.isAllowed()) {
			reject(res2, result);
			return;
		}
		if (mintsSessionFor(req2))
			req2.getSession(true);
		var tokens = new TokenSupplier(boundary, req2);
		req2.setAttribute(TOKEN_ATTRIBUTE, tokens.get());
		req2.setAttribute(TOKEN_SUPPLIER_ATTRIBUTE, tokens);
		req2.setAttribute(HEADER_ATTRIBUTE, boundary.csrfHeader());
		chain.doFilter(req, res);
	}

	/**
	 * Whether an allowed request should get a session before the page answering it embeds a token.
	 *
	 * <p>
	 * Only for a {@link CsrfBoundary} with a {@linkplain CsrfTokens#perSession() session-scoped} strategy and
	 * {@linkplain CsrfBoundary#createSessions() session creation} on, only on a safe request, and only when the request
	 * looks like a page load.  Never on a write: a write must already carry the token of a page that had a session.
	 */
	private boolean mintsSessionFor(HttpServletRequest req) {
		if (! (boundary instanceof CsrfBoundary b) || ! b.createSessions() || ! (b.tokens() instanceof CsrfTokens.PerSession))
			return false;
		if (! MethodSafety.isSafe(MethodSafety.effectiveMethod(req.getMethod(), req.getQueryString(), req::getHeader)))
			return false;
		return looksLikePageLoad(req);
	}

	/**
	 * Whether a request looks like a browser loading a page, as opposed to a script's fetch or a command-line client.
	 *
	 * <p>
	 * With fetch metadata, only a navigation or a {@code document}/{@code iframe} destination counts.  Without it, an
	 * {@code Accept} header naming {@code text/html} does, so {@code curl -H 'Accept: text/html'} also counts, while
	 * {@code Accept: *}{@code /*} never does.
	 */
	private static boolean looksLikePageLoad(HttpServletRequest req) {
		var mode = req.getHeader("Sec-Fetch-Mode");
		var dest = req.getHeader("Sec-Fetch-Dest");
		if ("navigate".equalsIgnoreCase(trim(mode)) || "document".equalsIgnoreCase(trim(dest)) || "iframe".equalsIgnoreCase(trim(dest)))
			return true;
		if (mode != null || dest != null || req.getHeader("Sec-Fetch-Site") != null)
			return false;
		var accept = req.getHeader("Accept");
		if (accept == null)
			return false;
		for (var range : accept.split(",")) {
			var semi = range.indexOf(';');
			if ("text/html".equalsIgnoreCase((semi < 0 ? range : range.substring(0, semi)).trim()))
				return true;
		}
		return false;
	}

	private static String trim(String s) {
		return s == null ? null : s.trim();
	}

	/**
	 * Answers a rejected request directly, without invoking the chain, so it never reaches application code.
	 *
	 * <p>
	 * The refusal is marked {@code no-store} so neither a browser nor an intermediate cache replays it once the cause
	 * has been fixed.
	 *
	 * <p>
	 * The request body is never read, so the connection is marked {@code Connection: close}.  Without it, a client that has
	 * not yet finished sending the body reuses the connection while the container is closing it, and the next request
	 * on it fails with an empty response.
	 *
	 * <p>
	 * The stream is written to but deliberately not closed: it belongs to the container, which commits and
	 * releases it once the filter returns.  Closing it here would commit the response early and cut off any
	 * outer filter that wraps it.
	 */
	@SuppressWarnings({
		"resource" // getOutputStream() returns the container-owned response stream; the filter is not its owner and must not close it.
	})
	private static void reject(HttpServletResponse res, BoundaryResult result) throws IOException {
		res.reset();
		res.setStatus(result.status());
		res.setHeader(REJECTION_HEADER, result.reason().name());
		res.setHeader("Cache-Control", "no-store");
		res.setHeader("Connection", "close");
		res.setContentType("application/json;charset=utf-8");
		var body = "{\"reason\":\"" + result.reason().name() + "\",\"message\":\"" + escape(result.message()) + "\"}";
		var bytes = body.getBytes(StandardCharsets.UTF_8);
		res.setContentLength(bytes.length);
		res.getOutputStream().write(bytes);
	}

	/**
	 * Escapes a rejection message for a JSON string literal.
	 *
	 * <p>
	 * Written out rather than delegated to a serializer so the rejection path has no dependency on marshalling
	 * configuration: this response must be producible even when the application's serializers are misconfigured,
	 * because a boundary refusal that fails to render degrades into an opaque 500.
	 */
	private static String escape(String s) {
		var sb = new StringBuilder(s.length() + 16);
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20)
						sb.append(String.format("\\u%04x", (int)c));
					else
						sb.append(c);
				}
			}
		}
		return sb.toString();
	}
}
