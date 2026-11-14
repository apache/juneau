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
package org.apache.juneau.rest.server.mcp;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.auth.*;

/**
 * Per-request admission checks applied to an MCP POST endpoint before JSON-RPC dispatch.
 *
 * <p>
 * The checks, in order:
 * <ol>
 * 	<li>The request <c>Content-Type</c> must be <c>application/json</c> (<c>415</c> otherwise).
 * 	<li>A request carrying an <c>Origin</c> header must have an origin that matches its <c>Host</c> header or is listed in
 * 		the resource's {@link Rest#allowedOrigins() allowedOrigins} (<c>403</c> otherwise).  Requests without an <c>Origin</c> header pass.
 * 	<li>When a {@link McpServerConfig#getTokenValidator() token validator} is configured, a valid bearer token is required.
 * 	<li>When the request is not otherwise authenticated, it is served only if the caller is a loopback address and the
 * 		<c>Host</c> header names a loopback host or a host of an allowed origin, unless
 * 		{@link McpServerConfig#isAllowUnauthenticated()} is set.
 * </ol>
 */
final class McpRequestPolicy {

	private static final Logger LOG = Logger.getLogger(McpRequestPolicy.class.getName());
	private static final String PRINCIPAL_ATTR = "juneau.principal";
	private static final AtomicBoolean UNAUTHENTICATED_WARNED = new AtomicBoolean();

	private McpRequestPolicy() {}

	/**
	 * Applies the admission checks.
	 *
	 * @param config The server config.
	 * @param req The current request.
	 * @throws UnsupportedMediaType If the content type is not JSON.
	 * @throws Forbidden If the origin, host or caller is not admitted.
	 */
	static void check(McpServerConfig config, RestRequest req) {
		var ct = req.getHeader("Content-Type");
		if (! isJson(ct))
			throw new UnsupportedMediaType("Content-Type must be application/json.");

		var allowed = req.getContext().getAllowedOrigins();
		var origin = req.getHeader("Origin");
		var host = req.getHeader("Host");
		if (origin != null && ! originAllowed(allowed, origin, host))
			throw new Forbidden("Origin not allowed.");

		if (config.getTokenValidator() != null) {
			if (req.getHttpServletRequest().getAttribute(PRINCIPAL_ATTR) == null)
				new BearerTokenGuard(config.getTokenValidator()).guard(req, req.getResponse());
			return;
		}
		if (req.getHttpServletRequest().getAttribute(PRINCIPAL_ATTR) != null)
			return;
		if (config.isAllowUnauthenticated())
			return;
		if (UNAUTHENTICATED_WARNED.compareAndSet(false, true))
			LOG.warning(() -> "MCP endpoint is serving without authentication; only loopback callers are admitted. Configure a token validator or allowUnauthenticated() to change this.");
		if (! isLoopbackAddress(req.getHttpServletRequest().getRemoteAddr()))
			throw new Forbidden("Authentication required.");
		if (host != null && ! hostAllowed(allowed, host))
			throw new Forbidden("Host not allowed.");
	}

	private static boolean isJson(String contentType) {
		if (contentType == null)
			return false;
		var i = contentType.indexOf(';');
		var mt = (i < 0 ? contentType : contentType.substring(0, i)).trim();
		return "application/json".equalsIgnoreCase(mt);
	}

	private static boolean originAllowed(Set<String> allowed, String origin, String host) {
		var o = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
		if (allowed.contains(o))
			return true;
		if (host == null)
			return false;
		try {
			var u = URI.create(origin);
			if (u.getHost() == null || u.getScheme() == null)
				return false;
			var defaultPort = "https".equalsIgnoreCase(u.getScheme()) ? 443 : 80;
			var authority = u.getPort() < 0 || u.getPort() == defaultPort ? u.getHost() : u.getHost() + ":" + u.getPort();
			var suffix = ":" + defaultPort;
			var h = host.endsWith(suffix) ? host.substring(0, host.length() - suffix.length()) : host;
			return authority.equalsIgnoreCase(h);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean hostAllowed(Set<String> allowed, String host) {
		var name = hostName(host);
		if (isLoopbackName(name))
			return true;
		for (var o : allowed) {
			try {
				var u = URI.create(o);
				if (u.getHost() != null && u.getHost().equalsIgnoreCase(name))
					return true;
			} catch (IllegalArgumentException e) {
				// Ignore malformed entry.
			}
		}
		return false;
	}

	private static String hostName(String host) {
		var h = host.trim();
		if (h.startsWith("[")) {
			var e = h.indexOf(']');
			return e < 0 ? h : h.substring(0, e + 1);
		}
		var c = h.lastIndexOf(':');
		return c < 0 ? h : h.substring(0, c);
	}

	private static boolean isLoopbackName(String name) {
		return "localhost".equalsIgnoreCase(name) || "[::1]".equals(name) || name.startsWith("127.");
	}

	private static boolean isLoopbackAddress(String addr) {
		// An empty address means an in-process (non-network) request.
		if (addr == null || addr.isEmpty())
			return true;
		return addr.startsWith("127.") || "::1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr);
	}
}
