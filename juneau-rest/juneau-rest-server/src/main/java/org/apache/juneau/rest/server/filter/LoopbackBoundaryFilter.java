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

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.nio.charset.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Applies a {@link LoopbackBoundary} to every request reaching the servlet container, rejecting those that did not
 * come from the page this process served.
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
 * code.  The response carries the boundary's chosen status, a {@code X-Loopback-Boundary} header naming the
 * {@link LoopbackBoundary.Reason reason}, and a small JSON body carrying the same reason and a message.
 * <p>
 * A rejection is never rendered as an empty result or a silent no-op.  A security refusal that looks like "no data"
 * teaches the user to ignore it, and the point of answering explicitly is that a genuine misconfiguration &mdash;
 * the application being reached at {@code localhost} when it accepts {@code 127.0.0.1}, say &mdash; is immediately
 * diagnosable instead of presenting as an inexplicably broken page.
 *
 * <h5 class='section'>Token availability to the page renderer</h5>
 * <p>
 * On an allowed request the boundary's token value is placed under the {@link #TOKEN_ATTRIBUTE} request attribute,
 * so a page-rendering endpoint can embed it without needing its own reference to the boundary.
 *
 * <h5 class='section'>Example (Spring Boot):</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> FilterRegistrationBean&lt;LoopbackBoundaryFilter&gt; boundary(LoopbackBoundary <jv>b</jv>) {
 * 		<jk>var</jk> <jv>reg</jv> = <jk>new</jk> FilterRegistrationBean&lt;&gt;(<jk>new</jk> LoopbackBoundaryFilter(<jv>b</jv>));
 * 		<jv>reg</jv>.addUrlPatterns(<js>"/*"</js>);
 * 		<jv>reg</jv>.setOrder(Ordered.<jsf>HIGHEST_PRECEDENCE</jsf>);
 * 		<jk>return</jk> <jv>reg</jv>;
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoopbackBoundary}
 * 	<li class='jc'>{@link SynchronizerToken}
 * </ul>
 *
 * @since 10.0.0
 */
public class LoopbackBoundaryFilter implements Filter {

	/** Request attribute under which an allowed request carries the boundary's CSRF token value. */
	public static final String TOKEN_ATTRIBUTE = "org.apache.juneau.rest.server.filter.csrfToken";

	/** Response header naming the {@link LoopbackBoundary.Reason} a request was rejected for. */
	public static final String REJECTION_HEADER = "X-Loopback-Boundary";

	private final LoopbackBoundary boundary;

	/**
	 * Constructor.
	 *
	 * @param boundary The boundary to apply to every request.  Must not be <jk>null</jk>.
	 */
	public LoopbackBoundaryFilter(LoopbackBoundary boundary) {
		this.boundary = assertArgNotNull("boundary", boundary);
	}

	/**
	 * The boundary this filter applies.
	 *
	 * @return The boundary.
	 */
	public LoopbackBoundary boundary() { return boundary; }

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
		req2.setAttribute(TOKEN_ATTRIBUTE, boundary.token().value());
		chain.doFilter(req, res);
	}

	/**
	 * Answers a rejected request directly, without invoking the chain, so it never reaches application code.
	 *
	 * <p>
	 * The stream is written to but deliberately not closed: it belongs to the container, which commits and
	 * releases it once the filter returns.  Closing it here would commit the response early and cut off any
	 * outer filter that wraps it.
	 */
	@SuppressWarnings({
		"resource" // getOutputStream() returns the container-owned response stream; the filter is not its owner and must not close it.
	})
	private static void reject(HttpServletResponse res, LoopbackBoundary.Result result) throws IOException {
		res.reset();
		res.setStatus(result.status());
		res.setHeader(REJECTION_HEADER, result.reason().name());
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
