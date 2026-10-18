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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.impl.conn.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.classic.remote.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the SSRF guard of policy-covered {@code @Remote} calls on the classic {@link RestClient} against a real
 * loopback server, addressed by a host that only pin-on-connect can refuse: a guarded call must always be pinned, even when an ordinary call on the same client has left an
 * idle keep-alive connection to the same host and port, and a client whose connection manager this
 * {@link RestClient} did not create must refuse guarded calls.
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class RestClient_SsrfGuard_Test {

	private static HttpServer server;
	private static String url;

	@BeforeAll
	static void startServer() throws IOException {
		// Explicit IPv4 loopback bind, as in RestClient_Coverage2_Test.
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hello", exchange -> {
			try (var in = exchange.getRequestBody()) { in.readAllBytes(); }
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			// A fixed Content-Length keeps the connection alive for reuse once the body is consumed.
			exchange.sendResponseHeaders(200, body.length);
			try (var os = exchange.getResponseBody()) { os.write(body); }
			exchange.close();
		});
		server.start();
		// The bare decimal form of 127.0.0.1: the lexical pre-check leaves it to pin-on-connect (see
		// RemoteUrlPolicy.isDeniedHost), so only the pin can refuse a guarded call to it.
		url = "http://2130706433:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	public interface HelloRemote {
		@RemoteOp(method = "GET", path = "/hello")
		String call();
	}

	// =================================================================================================================
	// A: guarded calls never reuse a connection that was opened without the pin (non-pooled and pooled)
	// =================================================================================================================

	@Test void a01_guarded_freshClient_rejected() throws Exception {
		try (var c = client(false)) {
			assertGuardedRejected(c);
		}
	}

	@Test void a02_unguardedThenGuarded_sameClient_guardedRejected() throws Exception {
		try (var c = client(false)) {
			// Fully consumed, so the keep-alive connection (opened without the pin) is kept for reuse.
			assertPlainOk(c);
			// The guarded call must not ride that idle connection; it goes through the pin and is refused.
			assertGuardedRejected(c);
		}
	}

	@Test void a03_guardedUnguardedGuarded_sameClient_bothGuardedRejected() throws Exception {
		try (var c = client(false)) {
			assertGuardedRejected(c);
			assertPlainOk(c);
			assertGuardedRejected(c);
			// The guard does not carry over to ordinary calls either.
			assertPlainOk(c);
		}
	}

	@Test void a04_pooled_unguardedThenGuarded_sameClient_guardedRejected() throws Exception {
		try (var c = client(true)) {
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@Test void a05_pooled_guardedUnguardedGuarded_sameClient_bothGuardedRejected() throws Exception {
		try (var c = client(true)) {
			assertGuardedRejected(c);
			assertPlainOk(c);
			assertGuardedRejected(c);
			assertPlainOk(c);
		}
	}

	@Test void a06_allowPrivateUrls_unguardedThenRemote_sameClient_succeeds() throws Exception {
		// With the opt-in the @Remote call is not guarded, so it shares the ordinary connection and succeeds.
		try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).build()) {
			assertPlainOk(c);
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	// =================================================================================================================
	// B: a connection manager this RestClient did not create cannot be pinned, so guarded calls are refused
	// =================================================================================================================

	@SuppressWarnings({
		"resource" // The caller-supplied manager is owned by the client built from it.
	})
	@Test void b01_callerSuppliedConnectionManager_guardActive_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).connectionManager(new PoolingHttpClientConnectionManager()).build()) {
			assertGuardedRefused(c);
			// Ordinary calls through the caller's connection manager are unaffected.
			assertPlainOk(c);
		}
	}

	@Test void b02_overriddenCreateConnectionManager_guardActive_refused() throws Exception {
		try (var c = new OwnConnectionManagerBuilder(BasicHttpClientConnectionManager::new).rootUrl(url).build()) {
			assertGuardedRefused(c);
			assertPlainOk(c);
		}
	}

	@Test void b03_overriddenCreateConnectionManager_returningSuper_guardStillApplies() throws Exception {
		// An override that returns the default connection manager unchanged keeps the pin.
		try (var c = new SuperConnectionManagerBuilder().rootUrl(url).build()) {
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@SuppressWarnings({
		"resource" // The caller-supplied manager is owned by the client built from it.
	})
	@Test void b04_callerSuppliedConnectionManager_allowPrivateUrls_succeeds() throws Exception {
		try (var c = RestClient.create().rootUrl(url).connectionManager(new PoolingHttpClientConnectionManager()).allowPrivateUrls(true).build()) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	@Test void b05_overriddenCreateConnectionManager_returningNull_guardActive_refused() throws Exception {
		// With no connection manager, HttpClientBuilder builds its own pool, which this RestClient cannot pin.
		try (var c = new OwnConnectionManagerBuilder(() -> null).rootUrl(url).build()) {
			assertGuardedRefused(c);
			assertPlainOk(c);
		}
	}

	// =================================================================================================================
	// C: the two-pool router sends each operation to the right pool
	// =================================================================================================================

	@SuppressWarnings({
		"resource" // The leased connection goes back to the router through releaseConnection(), not close().
	})
	@Test void c01_router_guardedLease_allOperationsGoToPinnedPool() throws Exception {
		var ordinary = new Recorder("ordinary");
		var pinned = new Recorder("pinned");
		var cm = new SsrfGuardConnectionManager(ordinary.manager, pinned.manager);
		var route = new HttpRoute(new HttpHost("localhost", 80));
		HttpClientConnection conn;
		RemoteUrlPolicyState.activate();
		try {
			var req = cm.requestConnection(route, null);
			assertTrue(req.cancel());
			conn = req.get(1, TimeUnit.SECONDS);
		} finally {
			RemoteUrlPolicyState.deactivate();
		}
		// Off the guarded thread state: the lease still remembers its pool.
		cm.connect(conn, route, 1, null);
		cm.upgrade(conn, route, null);
		cm.routeComplete(conn, route, null);
		cm.releaseConnection(conn, null, 1, TimeUnit.SECONDS);
		assertEquals("[requestConnection, cancel, get, connect, upgrade, routeComplete, releaseConnection]", pinned.calls.toString());
		assertEquals("[]", ordinary.calls.toString());
	}

	@SuppressWarnings({
		"resource" // The leased connection goes back to the router through releaseConnection(), not close().
	})
	@Test void c02_router_unguardedLease_allOperationsGoToOrdinaryPool() throws Exception {
		var ordinary = new Recorder("ordinary");
		var pinned = new Recorder("pinned");
		var cm = new SsrfGuardConnectionManager(ordinary.manager, pinned.manager);
		var route = new HttpRoute(new HttpHost("localhost", 80));
		var conn = cm.requestConnection(route, null).get(1, TimeUnit.SECONDS);
		cm.connect(conn, route, 1, null);
		cm.upgrade(conn, route, null);
		cm.routeComplete(conn, route, null);
		cm.releaseConnection(conn, null, 1, TimeUnit.SECONDS);
		assertEquals("[requestConnection, get, connect, upgrade, routeComplete, releaseConnection]", ordinary.calls.toString());
		assertEquals("[]", pinned.calls.toString());
	}

	@Test void c03_router_poolWideOperations_reachBothPools() {
		var ordinary = new Recorder("ordinary");
		var pinned = new Recorder("pinned");
		var cm = new SsrfGuardConnectionManager(ordinary.manager, pinned.manager);
		cm.closeIdleConnections(1, TimeUnit.SECONDS);
		cm.closeExpiredConnections();
		cm.shutdown();
		assertEquals("[closeIdleConnections, closeExpiredConnections, shutdown]", ordinary.calls.toString());
		assertEquals("[closeIdleConnections, closeExpiredConnections, shutdown]", pinned.calls.toString());
	}

	@Test void c04_pinningResolver_publicLiteral_resolvesToThatSingleAddress() throws Exception {
		assertArrayEquals(new InetAddress[]{InetAddress.getByName("93.184.216.34")}, PolicyPinningDnsResolver.INSTANCE.resolve("93.184.216.34"));
	}

	// Records the HttpClientConnectionManager / ConnectionRequest calls made on one pool.
	private static final class Recorder {
		final List<String> calls = new ArrayList<>();
		final HttpClientConnectionManager manager;
		Recorder(String name) {
			var conn = (HttpClientConnection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpClientConnection.class}, (proxy, m, a) -> switch (m.getName()) {
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == a[0];
				case "toString" -> name + "-connection";
				default -> null;
			});
			var request = (ConnectionRequest)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ConnectionRequest.class}, (proxy, m, a) -> {
				calls.add(m.getName());
				return "get".equals(m.getName()) ? conn : Boolean.TRUE;
			});
			manager = (HttpClientConnectionManager)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{HttpClientConnectionManager.class}, (proxy, m, a) -> {
				calls.add(m.getName());
				return "requestConnection".equals(m.getName()) ? request : null;
			});
		}
	}

	// =================================================================================================================
	// D: the pinned pool runs with the settings of the connection manager it twins
	// =================================================================================================================

	@Test void d01_pooled_overrideConfiguresSuper_guardedLeaseUsesSameSettings() throws Exception {
		var b = new ConfiguringBuilder(cm -> {
			var p = (PoolingHttpClientConnectionManager)cm;
			p.setMaxTotal(11);
			p.setDefaultMaxPerRoute(7);
			p.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(4321).build());
			p.setDefaultConnectionConfig(ConnectionConfig.custom().setBufferSize(1234).build());
			p.setValidateAfterInactivity(999);
			p.setSocketConfig(target(), SocketConfig.custom().setSoTimeout(2468).build());
			p.setConnectionConfig(target(), ConnectionConfig.custom().setBufferSize(1357).build());
		});
		try (var c = b.rootUrl(url).pooled().build()) {
			assertGuardedRejected(c);
			var pinned = (PoolingHttpClientConnectionManager)pinnedPool(c);
			assertEquals(11, pinned.getMaxTotal());
			assertEquals(7, pinned.getDefaultMaxPerRoute());
			assertEquals(4321, pinned.getDefaultSocketConfig().getSoTimeout());
			assertEquals(1234, pinned.getDefaultConnectionConfig().getBufferSize());
			assertEquals(999, pinned.getValidateAfterInactivity());
			assertEquals(2468, pinned.getSocketConfig(target()).getSoTimeout());
			assertEquals(1357, pinned.getConnectionConfig(target()).getBufferSize());
			// A second guarded lease finds the per-host config already copied and leaves it as is.
			var hostSocketConfig = pinned.getSocketConfig(target());
			var hostConnectionConfig = pinned.getConnectionConfig(target());
			assertGuardedRejected(c);
			assertSame(hostSocketConfig, pinned.getSocketConfig(target()));
			assertSame(hostConnectionConfig, pinned.getConnectionConfig(target()));
		}
	}

	@SuppressWarnings({
		"resource" // The pool is owned and closed by the client; the test only inspects it.
	})
	@Test void d02_pooled_settingsChangedAfterBuild_mirroredOnNextGuardedLease() throws Exception {
		var holder = new HttpClientConnectionManager[1];
		try (var c = new ConfiguringBuilder(cm -> holder[0] = cm).rootUrl(url).pooled().build()) {
			var p = (PoolingHttpClientConnectionManager)holder[0];
			p.setDefaultMaxPerRoute(5);
			p.setMaxPerRoute(new HttpRoute(new HttpHost("2130706433", server.getAddress().getPort())), 3);
			assertGuardedRejected(c);
			var pinned = (PoolingHttpClientConnectionManager)pinnedPool(c);
			assertEquals(5, pinned.getDefaultMaxPerRoute());
			assertEquals(3, pinned.getMaxPerRoute(new HttpRoute(new HttpHost("2130706433", server.getAddress().getPort()))));
		}
	}

	@Test void d03_basic_overrideConfiguresSuper_guardedLeaseUsesSameSettings() throws Exception {
		var b = new ConfiguringBuilder(cm -> {
			var x = (BasicHttpClientConnectionManager)cm;
			x.setSocketConfig(SocketConfig.custom().setSoTimeout(4321).build());
			x.setConnectionConfig(ConnectionConfig.custom().setBufferSize(1234).build());
		});
		try (var c = b.rootUrl(url).build()) {
			assertGuardedRejected(c);
			var pinned = (BasicHttpClientConnectionManager)pinnedPool(c);
			assertEquals(4321, pinned.getSocketConfig().getSoTimeout());
			assertEquals(1234, pinned.getConnectionConfig().getBufferSize());
		}
	}

	@Test void d04_builderReusedWithPoolingToggled_pinnedPoolMatchesOrdinaryPoolType() throws Exception {
		var b = RestClient.create().rootUrl(url);
		b.build().close();
		b.pooled();
		try (var c = b.build()) {
			assertGuardedRejected(c);
			assertSame(field(routerOf(c), "ordinary").getClass(), pinnedPool(c).getClass());
		}
	}

	// =================================================================================================================
	// E: a nested guarded call does not end the outer call's guard
	// =================================================================================================================

	@Test void e01_nestedGuardedCallFromInterceptor_outerCallStillPinned() throws Exception {
		var nested = new ThreadLocal<Boolean>();
		var holder = new RestClient[1];
		var nestedRan = new boolean[1];
		HttpRequestInterceptor itcp = (req, ctx) -> {
			// Only the outer guarded request makes the nested guarded call (once).
			if (RemoteUrlPolicyState.isActive() && nested.get() == null) {
				nested.set(Boolean.TRUE);
				try {
					holder[0].getRemote(HelloRemote.class).call();
				} catch (Exception e) {
					nestedRan[0] = true;
				} finally {
					nested.remove();
				}
			}
		};
		try (var c = RestClient.create().rootUrl(url).addInterceptorFirst(itcp).build()) {
			holder[0] = c;
			assertGuardedRejected(c);
			assertTrue(nestedRan[0]);
			assertFalse(RemoteUrlPolicyState.isActive());
		}
	}

	@Test void e02_policyState_nestsAndUnmatchedDeactivateIsNoOp() {
		RemoteUrlPolicyState.deactivate();
		assertFalse(RemoteUrlPolicyState.isActive());
		RemoteUrlPolicyState.activate();
		RemoteUrlPolicyState.activate();
		RemoteUrlPolicyState.deactivate();
		assertTrue(RemoteUrlPolicyState.isActive());
		RemoteUrlPolicyState.deactivate();
		assertFalse(RemoteUrlPolicyState.isActive());
		RemoteUrlPolicyState.deactivate();
		assertFalse(RemoteUrlPolicyState.isActive());
	}

	// =================================================================================================================
	// F: the pinned pool is always this client's own, so closing the client shuts it down
	// =================================================================================================================

	@SuppressWarnings({
		"resource" // The subject is close() itself, so the client is closed explicitly.
	})
	@Test void f01_connectionManagerShared_close_shutsDownBothOwnPools() throws Exception {
		var c = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true).build();
		var router = routerOf(c);
		var ordinary = (HttpClientConnectionManager)field(router, "ordinary");
		var pinned = (HttpClientConnectionManager)field(router, "pinned");
		c.close();
		var route = new HttpRoute(new HttpHost("localhost", 80));
		assertThrows(IllegalStateException.class, () -> lease(pinned, route));
		// The ordinary pool was built by this client and nothing else can reach it, so "shared" does not keep it open.
		assertThrows(IllegalStateException.class, () -> lease(ordinary, route));
	}

	@SuppressWarnings({
		"resource" // The subject is closeQuietly() itself, so the client is closed explicitly.
	})
	@Test void f02_closeQuietly_shutsDownPinnedPool() throws Exception {
		var c = RestClient.create().rootUrl(url).connectionManagerShared(true).build();
		var pinned = (HttpClientConnectionManager)field(routerOf(c), "pinned");
		c.closeQuietly();
		var route = new HttpRoute(new HttpHost("localhost", 80));
		assertThrows(IllegalStateException.class, () -> lease(pinned, route));
	}

	@SuppressWarnings({
		"resource" // The subject is close() itself; the leased connection goes back through releaseConnection().
	})
	@Test void f03_keepHttpClientOpen_close_leavesPinnedPoolOpen() throws Exception {
		var c = RestClient.create().rootUrl(url).keepHttpClientOpen().build();
		var pinned = (HttpClientConnectionManager)field(routerOf(c), "pinned");
		c.close();
		var route = new HttpRoute(new HttpHost("localhost", 80));
		var conn = pinned.requestConnection(route, null).get(1, TimeUnit.SECONDS);
		pinned.releaseConnection(conn, null, 0, TimeUnit.MILLISECONDS);
		c.httpClient.close();
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	@SuppressWarnings({
		"resource" // The client is returned to the caller, which owns it.
	})
	private static RestClient client(boolean pooled) {
		var b = RestClient.create().rootUrl(url);
		if (pooled)
			b.pooled();
		return b.build();
	}

	private static void assertPlainOk(RestClient c) throws Exception {
		try (var req = c.get(url + "/hello"); var res = req.run()) {
			assertEquals("hi", res.getContent().asString());
		}
	}

	private static void assertGuardedRejected(RestClient c) {
		var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
		var msg = messages(ex);
		assertTrue(msg.contains("No allowed address for host '2130706433'"), msg);
	}

	private static void assertGuardedRefused(RestClient c) {
		var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
		var msg = messages(ex);
		assertTrue(msg.contains("cannot be guaranteed to honor the SSRF guardrail"), msg);
		assertTrue(msg.contains("connection manager"), msg);
	}

	// The two-pool router installed on the client's HttpClient (read reflectively: neither exposes it).
	@SuppressWarnings({
		"resource" // The test only needs the lease to succeed or throw; the connection is never used.
	})
	private static void lease(HttpClientConnectionManager manager, HttpRoute route) throws Exception {
		manager.requestConnection(route, null).get(1, TimeUnit.SECONDS);
	}

	private static Object routerOf(RestClient c) throws Exception {
		var router = field(c.httpClient, "connManager");
		assertEquals("SsrfGuardConnectionManager", router.getClass().getSimpleName());
		return router;
	}

	private static HttpHost target() {
		return new HttpHost("2130706433", server.getAddress().getPort());
	}

	private static HttpClientConnectionManager pinnedPool(RestClient c) throws Exception {
		return (HttpClientConnectionManager)field(routerOf(c), "pinned");
	}

	private static Object field(Object o, String name) throws Exception {
		for (var cl = o.getClass(); cl != null; cl = cl.getSuperclass()) {
			try {
				var f = cl.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(o);
			} catch (NoSuchFieldException e) { /* Try the superclass. */ }
		}
		throw new NoSuchFieldException(name);
	}

	// Concatenates the messages of the whole cause chain (the proxy may wrap the failure).
	private static String messages(Throwable t) {
		var sb = new StringBuilder();
		for (var x = t; x != null; x = x.getCause()) {
			if (x instanceof UndeclaredThrowableException u && u.getUndeclaredThrowable() != x.getCause())
				sb.append(messages(u.getUndeclaredThrowable()));
			sb.append(x.getClass().getName()).append(": ").append(x.getMessage()).append('\n');
		}
		return sb.toString();
	}

	static final class OwnConnectionManagerBuilder extends RestClient.Builder<OwnConnectionManagerBuilder> {
		private final Supplier<HttpClientConnectionManager> supplier;
		OwnConnectionManagerBuilder(Supplier<HttpClientConnectionManager> supplier) {
			this.supplier = supplier;
		}
		@Override
		protected HttpClientConnectionManager createConnectionManager() {
			return supplier.get();
		}
	}

	// Configures the connection manager the default createConnectionManager() built, then returns that same instance.
	static final class ConfiguringBuilder extends RestClient.Builder<ConfiguringBuilder> {
		private final Consumer<HttpClientConnectionManager> configurer;
		ConfiguringBuilder(Consumer<HttpClientConnectionManager> configurer) {
			this.configurer = configurer;
		}
		@Override
		protected HttpClientConnectionManager createConnectionManager() {
			var cm = super.createConnectionManager();
			configurer.accept(cm);
			return cm;
		}
	}

	static final class SuperConnectionManagerBuilder extends RestClient.Builder<SuperConnectionManagerBuilder> {
		@Override
		protected HttpClientConnectionManager createConnectionManager() {
			return super.createConnectionManager();
		}
	}
}
