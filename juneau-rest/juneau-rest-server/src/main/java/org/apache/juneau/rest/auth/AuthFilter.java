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
package org.apache.juneau.rest.auth;

import java.io.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Abstract base class for Juneau authentication servlet filters.
 *
 * <p>
 * Implements the two-layer authentication contract:
 * <ul>
 * 	<li><b>Chain layer</b> &mdash; subclasses implement {@link #authenticate(HttpServletRequest)}, which returns
 * 		one of three states:
 * 		<ul>
 * 			<li>{@link Optional#empty()} &mdash; filter does not apply (no recognizable credentials in the request).
 * 			<li>{@link Optional#of(Object) Optional.of(AuthResult)} &mdash; credentials recognized and valid; carries
 * 				the resolved {@link java.security.Principal} and role set.
 * 			<li>throw {@link AuthenticationException} &mdash; credentials recognized but invalid (bad token, revoked
 * 				key, etc.).
 * 		</ul>
 * 	<li><b>Standalone filter layer</b> &mdash; {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} is
 * 		provided by this base and calls {@link #authenticate}, wraps the request on success, passes through on
 * 		empty, and sends a {@code 401} response on failure.
 * </ul>
 *
 * <p>
 * Subclasses are both directly usable as a {@link jakarta.servlet.Filter} (standalone) and composable inside an
 * {@link AuthFilterChain} (chain mode). In standalone mode, {@link #doFilter} drives the full request lifecycle.
 * In chain mode, {@link AuthFilterChain} calls {@link #authenticate} directly and handles the response itself.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthFilterChain}
 * 	<li class='jc'>{@link AuthResult}
 * 	<li class='jc'>{@link AuthenticatedRequestWrapper}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthFilterFramework">AuthN Filter Framework</a>
 * </ul>
 *
 * @since 9.5.0
 */
public abstract class AuthFilter implements Filter {

	/** WWW-Authenticate response header name (RFC 7235 §4.1). */
	static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	/**
	 * Inspects the request and returns the authentication result.
	 *
	 * <p>
	 * Three-state return contract:
	 * <ul>
	 * 	<li>{@link Optional#empty()} &mdash; this filter does not apply to the request (e.g. no
	 * 		{@code Authorization} header for a bearer-token filter).  The chain will continue to the next filter.
	 * 	<li>{@link Optional#of(Object) Optional.of(AuthResult)} &mdash; authentication succeeded.
	 * 	<li>throw {@link AuthenticationException} &mdash; credentials were present but invalid.
	 * </ul>
	 *
	 * @param req The incoming HTTP request. Never <jk>null</jk>.
	 * @return The authentication result, or {@link Optional#empty()} if this filter does not apply.
	 * @throws AuthenticationException If the request carries recognizable credentials that are invalid.
	 */
	public abstract Optional<AuthResult> authenticate(HttpServletRequest req) throws AuthenticationException;

	/**
	 * Standalone-filter entry point.
	 *
	 * <p>
	 * Calls {@link #authenticate}, and on success wraps the request in an {@link AuthenticatedRequestWrapper}
	 * and delegates to the next filter.  On {@link AuthenticationException}, writes a {@code 401} response with
	 * the {@code WWW-Authenticate} challenge from the exception.  On an empty result the request passes through
	 * unchanged.
	 *
	 * <p>
	 * When this filter is composed inside an {@link AuthFilterChain}, the chain calls
	 * {@link #authenticate(HttpServletRequest)} directly and this method is not invoked.
	 */
	@Override /* Overridden from Filter */
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		var hreq = (HttpServletRequest) req;
		var hresp = (HttpServletResponse) resp;
		try {
			var result = authenticate(hreq);
			if (result.isPresent()) {
				chain.doFilter(new AuthenticatedRequestWrapper(hreq, result.get()), resp);
			} else {
				chain.doFilter(req, resp);
			}
		} catch (AuthenticationException e) {
			sendChallenge(hresp, e);
		}
	}

	/**
	 * No-op default implementation.
	 */
	@Override /* Overridden from Filter */
	public void init(FilterConfig cfg) throws ServletException {
		// No-op
	}

	/**
	 * No-op default implementation.
	 */
	@Override /* Overridden from Filter */
	public void destroy() {
		// No-op
	}

	/**
	 * Writes a 401 response with the {@code WWW-Authenticate} challenge from the exception.
	 *
	 * @param resp The HTTP response.
	 * @param e The authentication failure.
	 * @throws IOException If writing to the response fails.
	 */
	static void sendChallenge(HttpServletResponse resp, AuthenticationException e) throws IOException {
		e.getHeaders().stream()
			.filter(h -> WWW_AUTHENTICATE.equalsIgnoreCase(h.getName()))
			.map(h -> h.getValue())
			.findFirst()
			.ifPresent(v -> resp.setHeader(WWW_AUTHENTICATE, v));
		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		var msg = e.getMessage();
		if (msg != null)
			resp.getWriter().write(msg);
	}
}
