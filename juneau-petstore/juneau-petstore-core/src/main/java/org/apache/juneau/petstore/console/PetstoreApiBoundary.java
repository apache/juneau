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
import java.util.*;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.server.filter.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * The {@code /petstore} API's CSRF boundary: a {@link RequestBoundaryFilter} over the framework's {@link CsrfBoundary},
 * applied to {@code /petstore} and everything under it.
 *
 * <p>
 * A browser write to the API must come from the page's own origin and carry this boundary's token in the
 * {@code X-Csrf-Token} header.  The token travels in the header only, so every HTML {@code <form>} POST to
 * the API is refused, the application's own included.  Requests are judged on their effective method, so
 * {@code GET /petstore/pets/1?method=DELETE} counts as a write.  Reads are allowed, and so are non-browser writes
 * (no {@code Origin} and no {@code Sec-Fetch-Site}) that a browser could not have sent cross-site without a CORS
 * preflight: JSON, XML or UON bodies, and {@code PUT}, {@code DELETE} or {@code PATCH}.  A non-browser urlencoded,
 * multipart or {@code text/plain} {@code POST} is refused.  Every refusal names its reason in the
 * {@value RequestBoundaryFilter#REJECTION_HEADER} response header.
 *
 * <h5 class='section'>The token</h5>
 * <p>
 * The token is per process ({@link CsrfTokens#perProcess()}): the petstore has no login session, and the
 * {@code Origin} check does the real work.  A restart invalidates the token on every open page, so reload them.  A
 * token that leaks through a page stays valid until the restart.
 *
 * <p>
 * This token is not the console's.  The console boundary ({@link PetstoreConsoleBoundary}) has its own, and each
 * boundary refuses the other's.  The console's one write to the API, its photo upload, gets this boundary's token
 * from the console boundary, which publishes {@link #tokenFor(HttpServletRequest)} under {@link #TOKEN_ATTRIBUTE}
 * on every console request it allows; the page's script sends it in that header.  So a runner must
 * give the console boundary the same instance it mounts here: a token from any other instance is refused.
 *
 * <p>
 * The console page shell marks a token-bearing page {@code no-store}, and when the response is already committed it
 * strips only its own token attributes.  So on the rare page whose buffer was flushed before the shell ran, this
 * boundary's token can still be emitted, possibly without {@code no-store}.  That is accepted: this token is only
 * ever published on console pages, which carry the console's token too, so on every normal path the page is already
 * marked.
 *
 * <h5 class='section'>The host</h5>
 * <p>
 * The request's own origin is its scheme plus its {@code Host} header; forwarded headers are never read.  By default
 * any {@code Host} is answered, which suits a loopback developer setup.  On a host served over plain HTTP, set
 * {@value #ALLOWED_HOSTS_PROPERTY} (a system property, the environment, or any registered setting source) to the
 * comma-separated host names the browser uses, without ports.  Every request to the API under any other {@code Host},
 * reads included, is then answered with {@code 421}: this is the DNS-rebinding defence.  The setting is read once,
 * when the boundary is constructed, and a malformed entry fails construction rather than being ignored.
 *
 * <p>
 * Behind a TLS-terminating proxy the browser's {@code https} origin does not match the {@code http} request the
 * container sees, so writes are refused.  Such a deployment needs the container's forwarded-header support and the
 * framework boundary's {@link org.apache.juneau.rest.server.filter.CsrfBoundary.OriginSource#CONTAINER CONTAINER} origin source, which this demo does not
 * configure.
 *
 * <h5 class='section'>Example:</h5>
 * <p>
 * In a Juneau microservice, contribute a <c>ServletFilterRegistration</c> <c>@Bean</c> just after the console
 * boundary's, sharing the one instance with it:
 * <p class='bjava'>
 * 	<jk>private final</jk> PetstoreApiBoundary <jf>apiBoundary</jf> = <jk>new</jk> PetstoreApiBoundary();
 *
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> ServletFilterRegistration apiBoundary() {
 * 		<jk>return</jk> ServletFilterRegistration.<jsm>create</jsm>(<jf>apiBoundary</jf>)
 * 			.urlPatterns(PetstoreApiBoundary.<jsf>URL_PATTERN</jsf>)
 * 			.dispatcherTypes(DispatcherType.<jsf>REQUEST</jsf>)
 * 			.order(ServletFilterRegistration.<jsf>HIGHEST_PRECEDENCE</jsf> + 10)
 * 			.build();
 * 	}
 * </p>
 * <p>
 * and build the console boundary with {@code new PetstoreConsoleBoundary(apiBoundary)}.  Register it for
 * {@code REQUEST} dispatches only: an internal forward or error dispatch is not a new request from the browser.
 */
public class PetstoreApiBoundary implements Filter {

	/** The covered path: the API resource's mount point. */
	public static final String PATH = "/petstore";

	/** The servlet URL pattern the boundary is registered under.  It matches {@link #PATH} itself and every path under it. */
	public static final String URL_PATTERN = PATH + "/*";

	/**
	 * Setting (system property, environment, or any registered source) listing the host names the API answers to:
	 * comma-separated, no ports.  Unset or blank, any host is answered.
	 */
	public static final String ALLOWED_HOSTS_PROPERTY = "petstore.api.allowedHosts";

	/**
	 * Request attribute under which an allowed console request carries a {@link String}: a freshly masked token for
	 * this boundary, for a console page to stamp on the form that writes to the API.
	 */
	public static final String TOKEN_ATTRIBUTE = "org.apache.juneau.petstore.apiCsrfToken";

	private final CsrfBoundary boundary;
	private final RequestBoundaryFilter filter;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Reads {@value #ALLOWED_HOSTS_PROPERTY} and creates the boundary's per-process token.
	 *
	 * @throws IllegalArgumentException If {@value #ALLOWED_HOSTS_PROPERTY} holds an entry that is blank or not a bare
	 * 	host name.
	 */
	public PetstoreApiBoundary() {
		boundary = CsrfBoundary.create()
			.tokens(CsrfTokens.perProcess())
			.allowedHosts(allowedHosts(Settings.get().get(ALLOWED_HOSTS_PROPERTY).orElse(null)))
			.build();
		filter = new RequestBoundaryFilter(boundary);
	}

	/**
	 * The host names in an {@value #ALLOWED_HOSTS_PROPERTY} setting.
	 *
	 * @param setting The setting.  Can be <jk>null</jk> or blank (not set).
	 * @return The comma-separated entries, trimmed, blank ones included so the boundary refuses them; empty when the
	 * 	setting is not set.
	 */
	private static String[] allowedHosts(String setting) {
		if (setting == null || setting.isBlank())
			return new String[0];
		return Arrays.stream(setting.split(",", -1)).map(String::trim).toArray(String[]::new);
	}

	/**
	 * A freshly masked token for a page served to this request to embed.
	 *
	 * <p>
	 * Never creates a session.  The value is different on every call, and every value verifies on this instance only.
	 *
	 * @param req The request whose response will embed the token.  Must not be <jk>null</jk>.
	 * @return 128 lower-case hex characters.
	 */
	public String tokenFor(HttpServletRequest req) {
		return boundary.tokenFor(req);
	}

	/**
	 * Applies the boundary: a refused request is answered here and never reaches the API.
	 */
	@Override /* Filter */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		filter.doFilter(req, res, chain);
	}
}
