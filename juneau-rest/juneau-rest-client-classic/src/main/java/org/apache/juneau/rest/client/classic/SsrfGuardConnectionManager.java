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
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;

/**
 * The connection manager a {@link RestClient} installs on the client it builds, holding two separate pools.
 *
 * <p>
 * Connections for SSRF-guard-active {@code @Remote} calls (requests whose context carries the {@link SsrfGuardMarker},
 * or made while the calling thread runs a guarded call) are leased from the {@code pinned} delegate, whose connections
 * are all opened through {@link PolicyPinningDnsResolver}.  Every other connection is leased from the {@code ordinary}
 * delegate.  So a guarded call can never be handed an idle connection that an unguarded call opened without the pin,
 * and the reverse.
 *
 * <p>
 * {@link HttpClientConnectionManager#requestConnection(HttpRoute, Object)} is not given the request's context, so the
 * client's {@link GuardInterceptor}, which Apache HttpClient runs on the same thread just before the lease, reads the
 * marker and hands the decision over.  The guard then fails closed: if the interceptor did not run (an
 * {@link HttpClientBuilder#setHttpProcessor(HttpProcessor) HttpProcessor} replaced the client's interceptors) and a
 * guarded request was nevertheless handed an ordinary connection, {@link #connect connect} refuses to open it and the
 * client's {@link GuardedRequestExecutor} refuses to send the request over it.
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

	// The lease decision that GuardInterceptor hands to the next requestConnection call on the same thread: set to true
	// for a guarded request, absent otherwise.  GuardInterceptor sets or clears it for every request and requestConnection
	// clears it before anything else, so a decision can only be left over when a request interceptor after GuardInterceptor
	// throws before the lease; the next lease on that thread is then pinned although its request is not guarded.  A
	// leftover can only over-guard by pinning an ordinary request, and never lets a guarded one through unpinned.
	private static final ThreadLocal<Boolean> NEXT_LEASE_GUARDED = new ThreadLocal<>();

	// Leased connections of the pinned pool.  Connection classes use identity equality, so this is an identity map.
	// Weak keys mean a leaked (never released) connection is not kept alive by this map.
	private final Map<HttpClientConnection,Boolean> pinnedLeases = Collections.synchronizedMap(new WeakHashMap<>());

	SsrfGuardConnectionManager(HttpClientConnectionManager ordinary, HttpClientConnectionManager pinned) {
		this.ordinary = ordinary;
		this.pinned = pinned;
	}

	@Override /* HttpClientConnectionManager */
	public ConnectionRequest requestConnection(HttpRoute route, Object state) {
		var handedOver = NEXT_LEASE_GUARDED.get() != null;
		NEXT_LEASE_GUARDED.remove();
		if (! handedOver && ! SsrfGuardMarker.isGuarded(null, null))
			return ordinary.requestConnection(route, state);
		if (nn(route.getProxyHost()))
			throw new GuardRefusedException("Refusing to send a policy-covered @Remote request to '" + route.getTargetHost()
				+ "' through a proxy (" + route.getProxyHost() + "): the SSRF guardrail's pin-on-connect would check the "
				+ "proxy's address, not the target's.  Remove the proxy (proxy(...), routePlanner(...) or the proxy system "
				+ "properties used by useSystemProperties()), or set allowPrivateUrls(true) if this is an intentional "
				+ "local-dev/intranet target.");
		mirrorSettings(route);
		var request = pinned.requestConnection(route, state);
		return new ConnectionRequest() {
			@Override /* ConnectionRequest */
			@SuppressWarnings({
				"resource" // the leased connection is handed to the caller, which releases it through releaseConnection().
			})
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
		requirePinnedIfGuarded(conn, null, context, "to open a connection of the ordinary pool");
		owner(conn).connect(conn, route, connectTimeout, context);
	}

	/**
	 * Returns whether {@link #isConnectionManagerOf(HttpClient)} can check a client at all: <jk>false</jk> when the
	 * connection manager of Apache's {@code InternalHttpClient} cannot be read, in which case every client counts as not
	 * wired.
	 *
	 * @return <jk>true</jk> if clients can be checked.
	 */
	static boolean canCheckWiring() {
		return nn(CONN_MANAGER);
	}

	/**
	 * Returns whether the given client is wired to this connection manager: it is an Apache {@code InternalHttpClient}
	 * (what {@link HttpClientBuilder#build()} returns) whose connection manager is this one.
	 *
	 * @param client The client.
	 * @return <jk>false</jk> for any other client, including one that wraps such a client (it may send the request
	 * 	elsewhere), and if the client's connection manager cannot be read.
	 */
	boolean isConnectionManagerOf(HttpClient client) {
		if (CONN_MANAGER == null || client == null || client.getClass() != CONN_MANAGER.getDeclaringClass())
			return false;
		try {
			return CONN_MANAGER.get(client) == this;
		} catch (@SuppressWarnings("unused") IllegalAccessException e) {
			// HTT: unreachable, since the field was made accessible when it was looked up.
			return false;
		}
	}

	// InternalHttpClient.connManager, or null if this HttpClient release has no such field (every client then counts as
	// not wired, so guarded calls are refused).
	private static final Field CONN_MANAGER = connManagerField();

	@SuppressWarnings({
		"java:S3011" // The client's connection manager has no accessor, so the field is read reflectively.
	})
	private static Field connManagerField() {
		try {
			var f = Class.forName("org.apache.http.impl.client.InternalHttpClient").getDeclaredField("connManager");
			f.setAccessible(true);
			return f;
		} catch (@SuppressWarnings("unused") ReflectiveOperationException | RuntimeException e) {
			// HTT: only an HttpClient release without this field, or a JVM that denies reflective access to it, gets here.
			return null;
		}
	}

	// Refuses a guarded request on a connection of the ordinary pool, which was or would be opened without the pin.
	private void requirePinnedIfGuarded(HttpClientConnection conn, HttpRequest request, HttpContext context, String what) {
		// isGuarded first: it also refuses a guarded request whose context lacks the marker, on any connection.
		if (SsrfGuardMarker.isGuarded(request, context) && ! pinnedLeases.containsKey(conn))
			throw new GuardRefusedException("Refusing to send a policy-covered @Remote request " + what + ", which is not "
				+ "opened through the SSRF guardrail's pin-on-connect: the request was executed "
				+ "on a thread where its guard was not known when the connection was leased (the client's request "
				+ "interceptors were replaced, for example by httpProcessor(...)).  Remove the HttpProcessor setting, or set "
				+ "allowPrivateUrls(true) if this is an intentional local-dev/intranet target.");
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
	@SuppressWarnings({
		"java:S3776" // One flat run of compare-and-copy steps, one per pool setting.
	})
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
	 * Thrown when the guard refuses a request.  It passes through Apache HttpClient unchanged, and the {@code @Remote}
	 * call that sent the request reports it as a {@link RestCallException} with this as the cause.
	 */
	static final class GuardRefusedException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		GuardRefusedException(String message) {
			super(message);
		}
	}

	/**
	 * The request interceptor that reads the {@link SsrfGuardMarker} of each request and hands the lease decision to
	 * the connection manager (see the class description).  Apache HttpClient runs it on the thread that then leases the
	 * connection, as part of the same call; it is set for every request, so a decision is never left over for another.
	 */
	static final class GuardInterceptor implements HttpRequestInterceptor {
		@Override /* HttpRequestInterceptor */
		public void process(HttpRequest request, HttpContext context) {
			if (SsrfGuardMarker.isGuarded(request, context))
				NEXT_LEASE_GUARDED.set(Boolean.TRUE);
			else
				NEXT_LEASE_GUARDED.remove();
		}
	}

	/**
	 * The request executor of a client wired to this connection manager: it refuses to send a guarded request over a
	 * connection of the ordinary pool (an already-open one is reused without {@link #connect connect}), then hands every
	 * request to the executor the client was configured with.
	 */
	static final class GuardedRequestExecutor extends HttpRequestExecutor {
		private final HttpRequestExecutor delegate;
		private final SsrfGuardConnectionManager manager;

		GuardedRequestExecutor(HttpRequestExecutor delegate, SsrfGuardConnectionManager manager) {
			// Another client's executor (a reused HttpClientBuilder keeps the one the previous build installed) is unwrapped.
			this.delegate = delegate instanceof GuardedRequestExecutor g ? g.delegate : or(delegate, new HttpRequestExecutor());
			this.manager = manager;
		}

		@Override /* HttpRequestExecutor */
		public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws IOException, HttpException {
			manager.requirePinnedIfGuarded(conn, request, context, "over an open connection of the ordinary pool");
			return delegate.execute(request, conn, context);
		}
	}
}
