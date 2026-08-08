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
package org.apache.juneau.rest.client.mcp.auth;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.*;

/**
 * A minimal loopback ({@code http://127.0.0.1}) HTTP listener that captures the single authorization-code redirect an
 * IdP sends back to a native/CLI OAuth client (SEP-837 loopback redirect handling).
 *
 * <p>
 * Binds to the loopback interface &mdash; by default on an ephemeral port (the RFC 8252 &sect;7.3 port-agnostic strategy),
 * or on a caller-chosen <em>fixed</em> port (the "bind-first" strategy) when constructed via
 * {@link #LoopbackRedirectReceiver(String, int, String)} &mdash; so the effective redirect URI (see
 * {@link #redirectUri()}) is only reachable from the same host.  The first request to the callback path completes
 * {@link #awaitCallback(Duration)} with the full request URI (including the {@code code}/{@code state}/{@code iss} query
 * parameters); the browser receives a small "you may close this window" page.
 *
 * <p>
 * Use with try-with-resources so the underlying server is always stopped.
 *
 * @since 10.0.0
 */
public class LoopbackRedirectReceiver implements AutoCloseable {

	private static final String DEFAULT_PATH = "/callback";
	private static final String DEFAULT_HTML =
		"<html><body><h3>Authorization received</h3><p>You may close this window and return to the application.</p></body></html>";

	private final HttpServer server;
	private final String path;
	private final int port;
	private final String host;
	private final CompletableFuture<URI> callback = new CompletableFuture<>();

	/**
	 * Opens a receiver on an ephemeral loopback port using the default callback path {@code /callback}.
	 *
	 * @return A started receiver.
	 * @throws IOException If the loopback server could not be started.
	 */
	public static LoopbackRedirectReceiver open() throws IOException {
		return new LoopbackRedirectReceiver(DEFAULT_PATH, DEFAULT_HTML);
	}

	/**
	 * Opens a receiver on an ephemeral loopback port (the port-agnostic strategy, RFC 8252 &sect;7.3).
	 *
	 * @param path The callback path (must start with {@code /}).  Must not be <jk>null</jk> or blank.
	 * @param successHtml The HTML body returned to the browser on the callback.  Must not be <jk>null</jk>.
	 * @throws IOException If the loopback server could not be started.
	 */
	public LoopbackRedirectReceiver(String path, String successHtml) throws IOException {
		this(path, 0, successHtml);
	}

	/**
	 * Opens a receiver on a caller-chosen <em>fixed</em> loopback port (the "bind-first" strategy for strict
	 * exact-match authorization servers that reject port-agnostic loopback redirects).
	 *
	 * <p>
	 * Pass {@code port == 0} for an ephemeral port (identical to {@link #LoopbackRedirectReceiver(String, String)}); a
	 * non-zero port binds exactly that port so it matches a {@link LoopbackRedirectUris#forPort(int, String) forPort}
	 * registration.  A fixed port can fail to bind if already in use &mdash; the {@link IOException} surfaces that to the
	 * caller.
	 *
	 * @param path The callback path (must start with {@code /}).  Must not be <jk>null</jk> or blank.
	 * @param bindPort The fixed loopback port to bind, or {@code 0} for an ephemeral port.  Must not be negative.
	 * @param successHtml The HTML body returned to the browser on the callback.  Must not be <jk>null</jk>.
	 * @throws IOException If the loopback server could not be started (e.g. the fixed port is already in use).
	 */
	public LoopbackRedirectReceiver(String path, int bindPort, String successHtml) throws IOException {
		this.path = assertArgNotNullOrBlank("path", path);
		assertArg(path.startsWith("/"), "path must start with '/' (was '%s')", path);
		assertArg(bindPort >= 0, "port must not be negative (was %s)", bindPort);
		assertArgNotNull("successHtml", successHtml);
		var html = successHtml.getBytes(StandardCharsets.UTF_8);
		var svr = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), bindPort), 0);
		try {
			port = svr.getAddress().getPort();
			host = hostLiteral(svr.getAddress().getAddress());
			svr.createContext(path, ex -> {
				var full = URI.create("http://" + host + ":" + port + ex.getRequestURI().toString());
				callback.complete(full);
				ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
				ex.sendResponseHeaders(200, html.length);
				try (var os = ex.getResponseBody()) {
					os.write(html);
				}
			});
			svr.start();
		} catch (RuntimeException e) {
			svr.stop(0);  // don't leak the bound socket if wiring fails after create()
			throw e;
		}
		server = svr;
	}

	/** Formats the bound loopback address as a URI host literal, bracketing IPv6 (e.g. {@code [::1]}). */
	private static String hostLiteral(InetAddress addr) {
		var ip = addr.getHostAddress();
		var pct = ip.indexOf('%');  // strip any IPv6 zone/scope id
		if (pct >= 0)
			ip = ip.substring(0, pct);
		return addr instanceof Inet6Address ? "[" + ip + "]" : ip;
	}

	/**
	 * Returns the loopback redirect URI the IdP should redirect the user-agent back to.
	 *
	 * @return The redirect URI (e.g. {@code http://127.0.0.1:PORT/callback}).
	 */
	public URI redirectUri() {
		return URI.create("http://" + host + ":" + port + path);
	}

	/**
	 * Returns the ephemeral loopback port the receiver is listening on.
	 *
	 * @return The port.
	 */
	public int port() {
		return port;
	}

	/**
	 * Blocks until the IdP redirects to the callback path, or the timeout elapses.
	 *
	 * @param timeout The maximum time to wait.  Must not be <jk>null</jk>.
	 * @return The full callback request URI, including query parameters.
	 * @throws McpAuthException If the wait times out or is interrupted.
	 */
	public URI awaitCallback(Duration timeout) {
		assertArgNotNull("timeout", timeout);
		try {
			return callback.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new McpAuthException("Timed out waiting for the loopback authorization redirect", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new McpAuthException("Interrupted waiting for the loopback authorization redirect", e);
		} catch (ExecutionException e) { // HTT: callback future is only ever completed normally by the handler
			throw new McpAuthException("Loopback authorization redirect failed", e);
		}
	}

	@Override /* Overridden from AutoCloseable */
	public void close() {
		server.stop(0);
	}
}
