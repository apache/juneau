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
package org.apache.juneau.petstore.console;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.server.filter.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * The console's CSRF boundary: a {@link LoopbackBoundaryFilter} applied to {@code /console} and everything under it.
 *
 * <p>
 * On console requests the boundary pins the {@code Host}, and on console writes it also requires a same-origin
 * {@code Origin}, a JSON body and the per-process {@link SynchronizerToken}.  An allowed console request carries
 * the token to the page renderer, which stamps it on {@code <body data-juneau-csrf>} for the console toolkit to send
 * back.
 *
 * <h5 class='section'>The effective method</h5>
 * <p>
 * A request is judged on the method it claims, not only on its request line.  A {@code method} query parameter or
 * an {@code X-Method}, {@code X-HTTP-Method-Override}, {@code X-HTTP-Method} or {@code X-Method-Override} header
 * naming anything other than a safe method makes the request a write, whether or not the resource is configured to
 * honour that override.  So {@code GET /console/pets/1?method=DELETE} needs the same Origin and token a real
 * {@code DELETE} does, and a later change to a resource's allowed method overrides cannot open a hole here.
 *
 * <h5 class='section'>The API's token</h5>
 * <p>
 * The {@code /petstore} API is not covered by this boundary: it sits behind its own, {@link PetstoreApiBoundary},
 * which has a different token.  The console's one write to the API, its photo upload
 * ({@code PUT /petstore/pets/{id}/photo}), carries the API's token, not this one.  So on every console request it
 * allows, this boundary also publishes a fresh API token as a {@link String} under
 * {@link PetstoreApiBoundary#TOKEN_ATTRIBUTE}, for the upload form to stamp.  A refused request publishes nothing.
 *
 * <p>
 * A runner must build this boundary with the {@link PetstoreApiBoundary} instance it mounts on the API.  The no-arg
 * constructor builds a private one that nobody mounts, so the console's uploads are then refused: visibly, and
 * closed rather than open.
 *
 * <p>
 * This boundary does not mark the response {@code no-store} for the API's token: the console page shell already
 * does so for every page carrying the console's token.  When the response is already committed, the shell drops
 * only its own token attributes, so on that rare path the API's token can still be emitted, possibly without
 * {@code no-store}.  That is accepted, as {@link PetstoreApiBoundary} describes.
 *
 * <h5 class='section'>What is outside</h5>
 * <p>
 * The {@code /petstore-ui} app (which only reads) is covered by neither boundary.  The Jetty deployment puts the
 * microservice admin resources behind a third boundary of their own.
 *
 * <h5 class='section'>The authority</h5>
 * <p>
 * The single accepted {@code Host} is the {@value #AUTHORITY_PROPERTY} setting when set, else
 * {@code localhost:<port>} with the port the request arrived on.  It is fixed on the first console request, after
 * the server has bound, so a runner that falls back to a random port still gets the right value.  Any other
 * spelling of the same server ({@code 127.0.0.1}, a LAN host name) is answered with {@code 421} on console paths.
 *
 * <p>
 * Set {@value #AUTHORITY_PROPERTY} whenever the browser's {@code Host} differs from the bound port: behind a Docker
 * port mapping or a reverse proxy, the default answers {@code 421}.  A browser leaves the port out of {@code Host}
 * for ports 80 and 443, so the value is then the bare host name.
 *
 * <p>
 * The boundary expects the {@code Origin} {@code http://<authority>}, so this works for plain HTTP only.  An HTTPS
 * deployment's console stays plain-HTTP-loopback only.  The API boundary reads no forwarded headers either, and the
 * demo does not configure the framework boundary's {@code CONTAINER} origin source, so behind a TLS-terminating proxy
 * the API refuses writes too (see {@link PetstoreApiBoundary}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>reg</jv> = <jk>new</jk> FilterRegistrationBean&lt;&gt;(<jk>new</jk> PetstoreConsoleBoundary(<jv>apiBoundary</jv>));
 * 	<jv>reg</jv>.addUrlPatterns(PetstoreConsoleBoundary.<jsf>URL_PATTERN</jsf>);
 * </p>
 *
 * <p>
 * In a Juneau microservice, contribute a <c>ServletFilterRegistration</c> <c>@Bean</c> instead:
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ServletFilterRegistration consoleBoundary() {
 * 		<jk>return</jk> ServletFilterRegistration.<jsm>create</jsm>(<jk>new</jk> PetstoreConsoleBoundary(<jf>apiBoundary</jf>))
 * 			.urlPatterns(PetstoreConsoleBoundary.<jsf>URL_PATTERN</jsf>)
 * 			.dispatcherTypes(DispatcherType.<jsf>REQUEST</jsf>, DispatcherType.<jsf>FORWARD</jsf>, DispatcherType.<jsf>ASYNC</jsf>, DispatcherType.<jsf>ERROR</jsf>)
 * 			.order(ServletFilterRegistration.<jsf>HIGHEST_PRECEDENCE</jsf>)
 * 			.build();
 * 	}
 * </p>
 * <p>
 * where {@code apiBoundary} is the {@link PetstoreApiBoundary} the runner mounts on the API.
 */
public class PetstoreConsoleBoundary implements Filter {

	/** The covered path: the console resource's mount point. */
	public static final String PATH = "/console";

	/** The servlet URL pattern the boundary is registered under. */
	public static final String URL_PATTERN = PATH + "/*";

	/** Setting (system property, environment, or any registered source) naming the one accepted console host and port. */
	public static final String AUTHORITY_PROPERTY = "petstore.console.authority";

	/** The query parameter that can override a request's method. */
	static final String OVERRIDE_PARAM = "method";

	/** The headers that can override a request's method. */
	static final List<String> OVERRIDE_HEADERS = List.of("X-Method", "X-HTTP-Method-Override", "X-HTTP-Method", "X-Method-Override");

	private final SynchronizerToken token = SynchronizerToken.generate();
	private final AtomicReference<LoopbackBoundaryFilter> filter = new AtomicReference<>();
	private final PetstoreApiBoundary api;

	/**
	 * Constructor for a console boundary with an API boundary of its own.
	 *
	 * <p>
	 * Nothing mounts that API boundary, so the API refuses the tokens this boundary publishes.  A runner that mounts
	 * {@link PetstoreApiBoundary} uses {@link #PetstoreConsoleBoundary(PetstoreApiBoundary)} instead.
	 */
	public PetstoreConsoleBoundary() {
		this(new PetstoreApiBoundary());
	}

	/**
	 * Constructor.
	 *
	 * @param api The API boundary the runner mounts, whose tokens console pages carry.  Must not be <jk>null</jk>.
	 */
	public PetstoreConsoleBoundary(PetstoreApiBoundary api) {
		this.api = Objects.requireNonNull(api, "api");
	}

	/**
	 * Applies the boundary to console requests and passes every other request straight through.
	 *
	 * <p>
	 * An allowed console request also carries the API's token (see {@link PetstoreApiBoundary}).
	 *
	 * <p>
	 * The path check makes the boundary safe to call for every request (for example when registered under a broader
	 * pattern), not only when a container maps it to {@link #URL_PATTERN}.
	 */
	@Override /* Filter */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (! (req instanceof HttpServletRequest req2 && covers(req2.getServletPath(), req2.getPathInfo()))) {
			chain.doFilter(req, res);
			return;
		}
		// The boundary sees the effective method; the resource still gets the original request.
		var method = effectiveMethod(req2.getMethod(), req2.getQueryString(), req2::getHeader);
		var checked = method.equals(req2.getMethod()) ? req2 : new HttpServletRequestWrapper(req2) {
			@Override /* HttpServletRequest */
			public String getMethod() {
				return method;
			}
		};
		filter(req2).doFilter(checked, res, (rq, rs) -> {
			req2.setAttribute(PetstoreApiBoundary.TOKEN_ATTRIBUTE, api.tokenFor(req2));
			chain.doFilter(req, res);
		});
	}

	private LoopbackBoundaryFilter filter(HttpServletRequest req) {
		var f = filter.get();
		if (f != null)
			return f;
		var boundary = LoopbackBoundary.create()
			.authority(authority(Settings.get().get(AUTHORITY_PROPERTY).orElse(null), req.getLocalPort()))
			.token(token)
			.build();
		filter.compareAndSet(null, new LoopbackBoundaryFilter(boundary));
		return filter.get();
	}

	/**
	 * The accepted console authority.
	 *
	 * @param setting The {@value #AUTHORITY_PROPERTY} setting.  Can be <jk>null</jk> or blank (not set).
	 * @param localPort The port the server is bound to.
	 * @return The setting, trimmed, when set, else {@code localhost:<localPort>}.
	 */
	static String authority(String setting, int localPort) {
		return setting == null || setting.isBlank() ? "localhost:" + localPort : setting.trim();
	}

	/**
	 * The method a request is judged on.
	 *
	 * @param method The request line's method.
	 * @param queryString The raw query string.  Can be <jk>null</jk>.
	 * @param headers Looks up a request header by name; returns <jk>null</jk> when absent.
	 * @return The request line's method when it is not safe, else the first non-safe override (upper-cased) from the
	 * 	{@code method} query parameter or an override header, else the request line's method.  A blank override is
	 * 	ignored.
	 */
	static String effectiveMethod(String method, String queryString, UnaryOperator<String> headers) {
		if (LoopbackBoundary.isStateChanging(method))
			return method;
		var claims = new ArrayList<String>();
		if (queryString != null)
			for (var pair : queryString.split("&")) {
				var i = pair.indexOf('=');
				if (i > 0 && OVERRIDE_PARAM.equalsIgnoreCase(decode(pair.substring(0, i))))
					claims.add(decode(pair.substring(i + 1)));
			}
		for (var h : OVERRIDE_HEADERS)
			claims.add(headers.apply(h));
		for (var c : claims) {
			if (c == null || c.isBlank())
				continue;
			var m = c.trim().toUpperCase(Locale.ROOT);
			if (LoopbackBoundary.isStateChanging(m))
				return m;
		}
		return method;
	}

	private static String decode(String s) {
		try {
			return URLDecoder.decode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {  // malformed %-escape: judge the raw text
			return s;
		}
	}

	/**
	 * Whether a request path is a console path.
	 *
	 * @param servletPath The request's servlet path.  Can be <jk>null</jk>.
	 * @param pathInfo The request's path info.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> for {@code /console} and every path under it.
	 */
	static boolean covers(String servletPath, String pathInfo) {
		var p = (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
		return p.equals(PATH) || p.startsWith(PATH + "/");
	}
}
