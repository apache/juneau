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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.http.*;

/**
 * Pure decision logic for whether caller-set credential headers (such as {@code Authorization}
 * or {@code Cookie}) should be forwarded or stripped when a REST call is transparently replayed
 * against a redirect target (an HTTP {@code 3xx} response's {@code Location}).
 *
 * <p>
 * This class contains only the <b>decision</b> &mdash; it does not itself intercept, follow, or
 * rewrite any request. Each transport binding (classic, HttpClient-4.5/5.x, Jetty, OkHttp, the
 * JDK client, etc.) is responsible for calling into this class from its own redirect hook and
 * acting on the result.
 *
 * <p>
 * The policy is intentionally simple and conservative:
 * <ul>
 * 	<li>Credentials are forwarded only when the redirect target is the <b>exact same origin</b>
 * 		as the original request &mdash; same scheme, same host (case-insensitive), and same port
 * 		(after applying the standard default port for {@code http}/{@code https}).
 * 	<li>Credentials are stripped on any other origin change (different scheme, host, or port).
 * 	<li>Credentials are stripped on an {@code https} &rarr; {@code http} scheme downgrade, even in
 * 		the (impossible under the same-origin rule above) case that host and port otherwise match.
 * </ul>
 *
 * <p>
 * Both {@code from} and {@code to} must be absolute URIs (non-{@code null} scheme and host);
 * callers are responsible for resolving a relative {@code Location} header against the original
 * request URI before calling into this class.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>if</jk> (RedirectSecurity.<jsm>shouldStripCredentials</jsm>(<jv>originalUri</jv>, <jv>redirectUri</jv>)) {
 * 		<jc>// remove Authorization / Cookie / etc. before replaying the request</jc>
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public final class RedirectSecurity {

	private RedirectSecurity() {}

	/**
	 * The outcome of a credential-forwarding decision for a single redirect hop.
	 */
	public enum CredentialDecision {

		/** The redirect target is the same origin as the original request; credentials may be forwarded as-is. */
		FORWARD,

		/** The redirect target is cross-origin (or a scheme downgrade); credentials must be stripped before replay. */
		STRIP
	}

	/**
	 * Returns {@code true} if {@code from} and {@code to} share the same origin &mdash; scheme,
	 * host (case-insensitive), and port (after default-port normalization for {@code http}/{@code https}).
	 *
	 * @param from The original request URI. Must be absolute (non-{@code null} scheme and host).
	 * @param to The redirect target URI. Must be absolute (non-{@code null} scheme and host).
	 * @return {@code true} if both URIs resolve to the same origin.
	 * @throws IllegalArgumentException If either argument is {@code null} or not an absolute URI.
	 */
	public static boolean sameOrigin(URI from, URI to) {
		requireAbsolute(from, "from");
		requireAbsolute(to, "to");
		return from.getScheme().equalsIgnoreCase(to.getScheme())
			&& from.getHost().equalsIgnoreCase(to.getHost())
			&& normalizedPort(from) == normalizedPort(to);
	}

	/**
	 * Returns {@code true} if the redirect represents an {@code https} &rarr; {@code http} scheme
	 * downgrade.
	 *
	 * @param from The original request URI. Must be absolute (non-{@code null} scheme and host).
	 * @param to The redirect target URI. Must be absolute (non-{@code null} scheme and host).
	 * @return {@code true} if {@code from} is {@code https} and {@code to} is {@code http}.
	 * @throws IllegalArgumentException If either argument is {@code null} or not an absolute URI.
	 */
	public static boolean isDowngrade(URI from, URI to) {
		requireAbsolute(from, "from");
		requireAbsolute(to, "to");
		return "https".equalsIgnoreCase(from.getScheme()) && "http".equalsIgnoreCase(to.getScheme());
	}

	/**
	 * Decides whether credentials should be forwarded or stripped for a single redirect hop.
	 *
	 * @param from The original request URI. Must be absolute (non-{@code null} scheme and host).
	 * @param to The redirect target URI. Must be absolute (non-{@code null} scheme and host).
	 * @return {@link CredentialDecision#STRIP} unless {@code to} is the exact same origin as
	 * 	{@code from} (and not a scheme downgrade), in which case {@link CredentialDecision#FORWARD}.
	 * @throws IllegalArgumentException If either argument is {@code null} or not an absolute URI.
	 */
	public static CredentialDecision decide(URI from, URI to) {
		return shouldStripCredentials(from, to) ? CredentialDecision.STRIP : CredentialDecision.FORWARD;
	}

	/**
	 * Convenience boolean form of {@link #decide(URI, URI)}.
	 *
	 * @param from The original request URI. Must be absolute (non-{@code null} scheme and host).
	 * @param to The redirect target URI. Must be absolute (non-{@code null} scheme and host).
	 * @return {@code true} if credentials must be stripped before replaying the request against {@code to}.
	 * @throws IllegalArgumentException If either argument is {@code null} or not an absolute URI.
	 */
	public static boolean shouldStripCredentials(URI from, URI to) {
		return ! sameOrigin(from, to) || isDowngrade(from, to);
	}

	/**
	 * Returns the header names that must be stripped from a request before it is replayed
	 * cross-origin. Reuses the canonical credential-header set from {@link RedactedHeaders#DEFAULT}.
	 *
	 * @return The set of header names to strip on a cross-origin (or downgraded) redirect.
	 */
	public static Set<String> stripOnCrossOrigin() {
		return RedactedHeaders.DEFAULT;
	}

	private static int normalizedPort(URI uri) {
		var port = uri.getPort();
		if (port != -1)
			return port;
		return switch (uri.getScheme().toLowerCase(Locale.ROOT)) {
			case "https" -> 443;
			case "http" -> 80;
			default -> -1;
		};
	}

	private static void requireAbsolute(URI uri, String argName) {
		if (uri == null)
			throw iaex("Argument '%s' must not be null", argName);
		if (uri.getScheme() == null || uri.getHost() == null)
			throw iaex("Argument '%s' must be an absolute URI with a scheme and host: %s", argName, uri);
	}
}
