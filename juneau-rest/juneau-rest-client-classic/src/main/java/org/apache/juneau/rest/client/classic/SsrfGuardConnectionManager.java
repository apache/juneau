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
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.rest.client.classic.remote.*;

/**
 * The connection manager a {@link RestClient} installs on the client it builds, holding two separate pools.
 *
 * <p>
 * Connections for SSRF-guard-active {@code @Remote} calls (while {@link RemoteUrlPolicyState#isActive()} is set on
 * the calling thread) are leased from the {@code pinned} delegate, whose connections are all opened through
 * {@link PolicyPinningDnsResolver}.  Every other connection is leased from the {@code ordinary} delegate.  So a guarded
 * call can never be handed an idle connection that an unguarded call opened without the pin, and the reverse.
 *
 * <p>
 * The pinned pool runs with the ordinary pool's settings: before each guarded lease, the pool limits, the socket and
 * connection configuration and the validate-after-inactivity interval of the ordinary pool are copied onto it.  So a
 * caller who configures the default connection manager (in an override of {@code createConnectionManager()} or
 * later) configures guarded calls too.
 *
 * <p>
 * Each later operation on a leased connection (connect, upgrade, release) goes to the delegate that leased it,
 * whatever thread it runs on: a streamed response body may be released after the guarded call has returned.
 *
 * <p>
 * A guarded lease on a proxy route is refused: the pin would check the proxy's address, not the target's, and the
 * proxy then connects to the target unchecked.
 *
 * @since 10.0.0
 */
final class SsrfGuardConnectionManager implements HttpClientConnectionManager {

	private final HttpClientConnectionManager ordinary;
	private final HttpClientConnectionManager pinned;

	// Leased connections of the pinned pool.  Connection classes use identity equality, so this is an identity map;
	// weak keys mean a leaked (never released) connection is not kept alive by this map.
	private final Map<HttpClientConnection,Boolean> pinnedLeases = Collections.synchronizedMap(new WeakHashMap<>());

	SsrfGuardConnectionManager(HttpClientConnectionManager ordinary, HttpClientConnectionManager pinned) {
		this.ordinary = ordinary;
		this.pinned = pinned;
	}

	@Override /* HttpClientConnectionManager */
	public ConnectionRequest requestConnection(HttpRoute route, Object state) {
		if (! RemoteUrlPolicyState.isActive())
			return ordinary.requestConnection(route, state);
		if (nn(route.getProxyHost()))
			throw new ProxyRouteRefusedException("Refusing to send a policy-covered @Remote request to '" + route.getTargetHost()
				+ "' through a proxy (" + route.getProxyHost() + "): the SSRF guardrail's pin-on-connect would check the "
				+ "proxy's address, not the target's.  Remove the proxy (proxy(...), routePlanner(...) or the proxy system "
				+ "properties used by useSystemProperties()), or set allowPrivateUrls(true) if this is an intentional "
				+ "local-dev/intranet target.");
		mirrorSettings(route);
		var request = pinned.requestConnection(route, state);
		return new ConnectionRequest() {
			@Override /* ConnectionRequest */
			public HttpClientConnection get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
				var conn = request.get(timeout, timeUnit);
				pinnedLeases.put(conn, Boolean.TRUE);
				return conn;
			}
			@Override /* Cancellable */
			public boolean cancel() {
				return request.cancel();
			}
		};
	}

	@Override /* HttpClientConnectionManager */
	public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
		var manager = pinnedLeases.remove(conn) != null ? pinned : ordinary;
		manager.releaseConnection(conn, newState, validDuration, timeUnit);
	}

	@Override /* HttpClientConnectionManager */
	public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
		owner(conn).connect(conn, route, connectTimeout, context);
	}

	@Override /* HttpClientConnectionManager */
	public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
		owner(conn).upgrade(conn, route, context);
	}

	@Override /* HttpClientConnectionManager */
	public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
		owner(conn).routeComplete(conn, route, context);
	}

	@Override /* HttpClientConnectionManager */
	public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
		ordinary.closeIdleConnections(idletime, timeUnit);
		pinned.closeIdleConnections(idletime, timeUnit);
	}

	@Override /* HttpClientConnectionManager */
	public void closeExpiredConnections() {
		ordinary.closeExpiredConnections();
		pinned.closeExpiredConnections();
	}

	@Override /* HttpClientConnectionManager */
	public void shutdown() {
		try {
			ordinary.shutdown();
		} finally {
			pinned.shutdown();
		}
	}

	/** Shuts down the pinned pool only; the ordinary one may be shared, so its life-cycle is not ours to end. */
	void shutdownPinned() {
		pinned.shutdown();
	}

	// Copies the ordinary pool's settings onto the pinned pool (both are built by the same factory, so they are always
	// the same kind).  Only changed values are written: each setter takes the pool's lock.
	private void mirrorSettings(HttpRoute route) {
		if (ordinary instanceof PoolingHttpClientConnectionManager o) {
			var p = (PoolingHttpClientConnectionManager)pinned;
			if (p.getMaxTotal() != o.getMaxTotal())
				p.setMaxTotal(o.getMaxTotal());
			if (p.getDefaultMaxPerRoute() != o.getDefaultMaxPerRoute())
				p.setDefaultMaxPerRoute(o.getDefaultMaxPerRoute());
			if (p.getMaxPerRoute(route) != o.getMaxPerRoute(route))
				p.setMaxPerRoute(route, o.getMaxPerRoute(route));
			if (p.getDefaultSocketConfig() != o.getDefaultSocketConfig())
				p.setDefaultSocketConfig(o.getDefaultSocketConfig());
			if (p.getDefaultConnectionConfig() != o.getDefaultConnectionConfig())
				p.setDefaultConnectionConfig(o.getDefaultConnectionConfig());
			// Guarded routes never have a proxy (see requestConnection), so the pool connects to the target itself.
			mirrorHostSettings(o, p, route.getTargetHost());
			if (p.getValidateAfterInactivity() != o.getValidateAfterInactivity())
				p.setValidateAfterInactivity(o.getValidateAfterInactivity());
		} else if (ordinary instanceof BasicHttpClientConnectionManager o) {
			var p = (BasicHttpClientConnectionManager)pinned;
			if (p.getSocketConfig() != o.getSocketConfig())
				p.setSocketConfig(o.getSocketConfig());
			if (p.getConnectionConfig() != o.getConnectionConfig())
				p.setConnectionConfig(o.getConnectionConfig());
		}
	}

	private static void mirrorHostSettings(PoolingHttpClientConnectionManager o, PoolingHttpClientConnectionManager p, HttpHost host) {
		var hostSocketConfig = o.getSocketConfig(host);
		if (nn(hostSocketConfig) && p.getSocketConfig(host) != hostSocketConfig)
			p.setSocketConfig(host, hostSocketConfig);
		var hostConnectionConfig = o.getConnectionConfig(host);
		if (nn(hostConnectionConfig) && p.getConnectionConfig(host) != hostConnectionConfig)
			p.setConnectionConfig(host, hostConnectionConfig);
	}

	private HttpClientConnectionManager owner(HttpClientConnection conn) {
		return pinnedLeases.containsKey(conn) ? pinned : ordinary;
	}

	/**
	 * Thrown when a guarded lease on a proxy route is refused.  It passes through Apache HttpClient unchanged, and the
	 * {@code @Remote} call that leased the connection reports it as a {@link RestCallException} with this as the cause.
	 */
	static final class ProxyRouteRefusedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		ProxyRouteRefusedException(String message) {
			super(message);
		}
	}
}
