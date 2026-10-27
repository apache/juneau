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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.protocol.*;
import org.apache.http.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.util.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.impl.execchain.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.classic.remote.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsServer;

/**
 * Exercises the connection manager the classic {@link RestClient} builds for itself: the TLS, pool, socket and
 * connection settings (made on the builder, on a caller-supplied {@code HttpClientBuilder} or through the system
 * properties of {@code useSystemProperties()}) reach both of its pools, guarded {@code @Remote} calls are refused on
 * proxy routes and through a custom {@link RestCallHandler}, and a builder reused after {@code build()} gives each client
 * its own connection manager and adds the redirect credential guard once.
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class RestClient_ConnectionManager_Test {

	private static final char[] PASSWORD = "changeit".toCharArray();

	private static HttpServer server;
	private static HttpsServer tlsServer;
	private static SSLContext trustingContext;
	private static String url;
	private static int port;
	private static int tlsPort;

	@BeforeAll
	static void startServers() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hello", RestClient_ConnectionManager_Test::hello);
		server.start();
		port = server.getAddress().getPort();
		// The bare decimal form of 127.0.0.1: only pin-on-connect can refuse a guarded call to it.
		url = "http://2130706433:" + port;

		// A self-signed certificate for "localhost" only, so a request to 127.0.0.1 fails hostname verification.
		var keyStore = selfSignedKeyStore();
		var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, PASSWORD);
		var serverContext = SSLContext.getInstance("TLS");
		serverContext.init(kmf.getKeyManagers(), null, null);
		tlsServer = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		tlsServer.setHttpsConfigurator(new HttpsConfigurator(serverContext));
		tlsServer.createContext("/hello", RestClient_ConnectionManager_Test::hello);
		tlsServer.createContext("/protocol", RestClient_ConnectionManager_Test::protocol);
		tlsServer.start();
		tlsPort = tlsServer.getAddress().getPort();

		// A trust store holding only that certificate.
		var trustStore = KeyStore.getInstance("PKCS12");
		trustStore.load(null, null);
		trustStore.setCertificateEntry("server", keyStore.getCertificate("server"));
		var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);
		trustingContext = SSLContext.getInstance("TLS");
		trustingContext.init(null, tmf.getTrustManagers(), null);
	}

	@AfterAll
	static void stopServers() {
		if (server != null)
			server.stop(0);
		if (tlsServer != null)
			tlsServer.stop(0);
	}

	private static void hello(HttpExchange exchange) throws IOException {
		try (var in = exchange.getRequestBody()) { in.readAllBytes(); }
		var body = "hi".getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		try (var os = exchange.getResponseBody()) { os.write(body); }
		exchange.close();
	}

	// Answers with the TLS protocol version the connection negotiated.
	private static void protocol(HttpExchange exchange) throws IOException {
		try (var in = exchange.getRequestBody()) { in.readAllBytes(); }
		var body = ((HttpsExchange)exchange).getSSLSession().getProtocol().getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		try (var os = exchange.getResponseBody()) { os.write(body); }
		exchange.close();
	}

	// Generated with the JDK's keytool, so the test needs neither a checked-in key nor an internal JDK API.
	private static KeyStore selfSignedKeyStore() throws Exception {
		var dir = Files.createTempDirectory("restclient-tls");
		var file = dir.resolve("server.p12");
		var keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
		var p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
			"-validity", "2", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-storetype", "PKCS12",
			"-keystore", file.toString(), "-storepass", "changeit", "-keypass", "changeit")
			.redirectErrorStream(true).start();
		String out;
		try (var is = p.getInputStream()) {
			out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertEquals(0, p.waitFor(), out);
		var ks = KeyStore.getInstance("PKCS12");
		try (var in = Files.newInputStream(file)) {
			ks.load(in, PASSWORD);
		}
		Files.delete(file);
		Files.delete(dir);
		return ks;
	}

	public interface HelloRemote {
		@RemoteOp(method = "GET", path = "/hello")
		String call();
	}

	public interface StartRemote {
		@RemoteOp(method = "GET", path = "/start")
		String start();
	}

	public interface HelloRemoteDeclaringRestCallException {
		@RemoteOp(method = "GET", path = "/hello")
		String call() throws RestCallException;
	}

	// =================================================================================================================
	// A: the TLS and pool-size builder settings reach both pools
	// =================================================================================================================

	@Test void a01_sslContext_customTrustStore_honoured() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
		}
	}

	@Test void a02_sslContext_defaultHostnameVerification_stillApplies() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).build()) {
			var ex = assertThrows(Exception.class, () -> fetch(c, "https://127.0.0.1:" + tlsPort + "/hello"));
			assertContains("doesn't match any of the subject alternative names", messages(ex));
		}
	}

	@Test void a03_sslHostnameVerifier_honoured() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).sslHostnameVerifier(NoopHostnameVerifier.INSTANCE).build()) {
			assertEquals("hi", fetch(c, "https://127.0.0.1:" + tlsPort + "/hello"));
		}
	}

	@Test void a04_sslSocketFactory_honoured() throws Exception {
		try (var c = RestClient.create().sslSocketFactory(new SSLConnectionSocketFactory(trustingContext)).build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
		}
	}

	@Test void a05_sslSocketFactory_sameFactoryInBothPools() throws Exception {
		var factory = new SSLConnectionSocketFactory(trustingContext);
		try (var c = RestClient.create().sslSocketFactory(factory).build()) {
			assertSame(factory, httpsFactory(ordinaryPool(c)));
			assertSame(factory, httpsFactory(pinnedPool(c)));
		}
	}

	@Test void a06_sslContext_sameFactoryInBothPools() throws Exception {
		try (var c = RestClient.create().pooled().sslContext(trustingContext).build()) {
			var factory = httpsFactory(ordinaryPool(c));
			assertSame(factory, httpsFactory(pinnedPool(c)));
		}
	}

	@Test void a07_maxConn_honouredInBothPools() throws Exception {
		try (var c = RestClient.create().rootUrl(url).maxConnTotal(13).maxConnPerRoute(6).build()) {
			var ordinary = (PoolingHttpClientConnectionManager)ordinaryPool(c);
			var pinned = (PoolingHttpClientConnectionManager)pinnedPool(c);
			assertEquals(13, ordinary.getMaxTotal());
			assertEquals(6, ordinary.getDefaultMaxPerRoute());
			assertEquals(13, pinned.getMaxTotal());
			assertEquals(6, pinned.getDefaultMaxPerRoute());
			// The client still works, and guarded calls still go through the pin.
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@Test void a08_pooled_maxConnTotalOnly_perRouteDefaultKept() throws Exception {
		try (var c = RestClient.create().pooled().maxConnTotal(9).build()) {
			var ordinary = (PoolingHttpClientConnectionManager)ordinaryPool(c);
			assertEquals(9, ordinary.getMaxTotal());
			try (var expected = new PoolingHttpClientConnectionManager()) {
				assertEquals(expected.getDefaultMaxPerRoute(), ordinary.getDefaultMaxPerRoute());
			}
		}
	}

	@Test void a09_maxConnPerRouteOnly_pooled() throws Exception {
		try (var c = RestClient.create().maxConnPerRoute(4).build()) {
			assertEquals(4, ((PoolingHttpClientConnectionManager)ordinaryPool(c)).getDefaultMaxPerRoute());
			assertEquals(4, ((PoolingHttpClientConnectionManager)pinnedPool(c)).getDefaultMaxPerRoute());
		}
	}

	@Test void a10_sslHostnameVerifierOnly_defaultContextWithThatVerifier() throws Exception {
		try (var c = RestClient.create().sslHostnameVerifier(NoopHostnameVerifier.INSTANCE).build()) {
			assertSame(NoopHostnameVerifier.INSTANCE, field(httpsFactory(ordinaryPool(c)), "hostnameVerifier"));
			assertSame(httpsFactory(ordinaryPool(c)), httpsFactory(pinnedPool(c)));
			// The default trust store does not trust the self-signed certificate.
			var ex = assertThrows(Exception.class, () -> fetch(c, "https://localhost:" + tlsPort + "/hello"));
			assertContains("PKIX path building failed", messages(ex));
		}
	}

	@SuppressWarnings({
		"resource" // The pools are owned and closed by the client; the test only inspects them.
	})
	@Test void a11_noPoolSettings_basicManagerKept() throws Exception {
		try (var c = RestClient.create().build()) {
			assertInstanceOf(BasicHttpClientConnectionManager.class, ordinaryPool(c));
			assertInstanceOf(BasicHttpClientConnectionManager.class, pinnedPool(c));
		}
	}

	// =================================================================================================================
	// B: a guarded call is refused on a proxy route (the pin would check the proxy, not the target)
	// =================================================================================================================

	@Test void b01_proxy_guardedCall_refused() throws Exception {
		// A public literal target: the pin would allow it, and the proxy (loopback) is what it would actually check.
		try (var c = RestClient.create().rootUrl("http://93.184.216.34").proxy(new HttpHost("127.0.0.1", port)).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("through a proxy", messages(ex));
		}
	}

	@Test void b02_proxy_allowPrivateUrls_remoteCallGoesThroughProxy() throws Exception {
		// The loopback server answers the proxied request itself; the target host is never resolved.
		try (var c = RestClient.create().rootUrl("http://target.invalid").proxy(new HttpHost("127.0.0.1", port)).allowPrivateUrls(true).build()) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
			assertEquals("hi", fetch(c, "http://target.invalid/hello"));
		}
	}

	@SuppressWarnings({
		"resource" // The router shuts both pools down in the finally block.
	})
	@Test void b03_router_proxiedRoute_guardedLease_refused() {
		var ordinary = new PoolingHttpClientConnectionManager();
		var pinned = new PoolingHttpClientConnectionManager();
		var cm = new SsrfGuardConnectionManager(ordinary, pinned);
		var route = new HttpRoute(new HttpHost("example.org", 80), new HttpHost("proxy.example", 3128));
		try {
			RemoteUrlPolicyState.activate();
			try {
				var ex = assertThrows(IllegalStateException.class, () -> cm.requestConnection(route, null));
				assertContains("proxy.example:3128", ex.getMessage());
			} finally {
				RemoteUrlPolicyState.deactivate();
			}
			// Unguarded leases on a proxy route are not affected.
			assertTrue(cm.requestConnection(route, null).cancel());
		} finally {
			cm.shutdown();
		}
	}

	@Test void b04_proxy_guardedCall_refusedAsRestCallException() throws Exception {
		try (var c = RestClient.create().rootUrl("http://93.184.216.34").proxy(new HttpHost("127.0.0.1", port)).build()) {
			var ex = assertThrows(RestCallException.class, () -> c.getRemote(HelloRemoteDeclaringRestCallException.class).call());
			assertContains("through a proxy", ex.getMessage());
			var cause = assertInstanceOf(IllegalStateException.class, ex.getCause());
			assertContains("127.0.0.1:" + port, cause.getMessage());
		}
	}

	// =================================================================================================================
	// C: a custom RestCallHandler may copy the request or send it with another context, so guarded calls through one are refused
	// =================================================================================================================

	@Test void c01_customCallHandler_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).callHandler(DelegatingCallHandler.class).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("RestCallHandler", messages(ex));
			// Ordinary calls through the handler are unaffected.
			assertPlainOk(c);
		}
	}

	@Test void c02_customCallHandler_allowPrivateUrls_succeeds() throws Exception {
		try (var c = RestClient.create().rootUrl(url).callHandler(DelegatingCallHandler.class).allowPrivateUrls(true).build()) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	public static class DelegatingCallHandler extends BasicRestCallHandler {
		public DelegatingCallHandler(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return super.run(target, request, context);
		}
	}

	// =================================================================================================================
	// D: a builder reused after build() gives each client a fresh connection manager
	// =================================================================================================================

	@Test void d01_builderReused_firstClientClosed_secondClientWorks() throws Exception {
		var b = RestClient.create().rootUrl(url);
		try (var c = b.build()) {
			assertPlainOk(c);
		}
		try (var c = b.build()) {
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@Test void d02_builderReused_eachClientOwnsItsPools() throws Exception {
		var b = RestClient.create().rootUrl(url).pooled();
		try (var c1 = b.build(); var c2 = b.build()) {
			assertNotSame(ordinaryPool(c1), ordinaryPool(c2));
			assertNotSame(pinnedPool(c1), pinnedPool(c2));
			assertNull(c1.getHttpClientConnectionManager());
			assertNull(c2.getHttpClientConnectionManager());
		}
	}

	@SuppressWarnings({
		"resource" // The caller-supplied manager is shut down in the finally block.
	})
	@Test void d03_builderReused_callerSuppliedConnectionManager_kept() throws Exception {
		var cm = new PoolingHttpClientConnectionManager();
		var b = RestClient.create().rootUrl(url).connectionManager(cm).connectionManagerShared(true);
		try (var c1 = b.build(); var c2 = b.build()) {
			assertSame(cm, c1.getHttpClientConnectionManager());
			assertSame(cm, c2.getHttpClientConnectionManager());
			assertPlainOk(c2);
		} finally {
			cm.shutdown();
		}
	}

	@Test void d04_connectionManagerShared_defaultManager_shutDownOnClose() throws Exception {
		HttpClientConnectionManager ordinary, pinned;
		try (var c = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true).build()) {
			assertPlainOk(c);
			ordinary = ordinaryPool(c);
			pinned = pinnedPool(c);
		}
		// Nothing else can reach the pools this client built, so closing it shuts both down even though "shared" is set.
		assertShutDown(ordinary);
		assertShutDown(pinned);
	}

	@SuppressWarnings({
		"resource" // The subject is closeQuietly() itself, so the client is closed explicitly.
	})
	@Test void d05_connectionManagerShared_defaultManager_shutDownOnCloseQuietly() throws Exception {
		var c = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true).build();
		assertPlainOk(c);
		var ordinary = ordinaryPool(c);
		c.closeQuietly();
		assertShutDown(ordinary);
		assertShutDown(pinnedPool(c));
	}

	@SuppressWarnings({
		"resource" // The subject is closeQuietly() on a client sharing the manager, which is shut down in the finally block.
	})
	@Test void d06_connectionManagerShared_callerSuppliedManager_leftOpenOnClose() throws Exception {
		try (var cm = new PoolingHttpClientConnectionManager()) {
			try (var c = RestClient.create().rootUrl(url).connectionManager(cm).connectionManagerShared(true).build()) {
				assertPlainOk(c);
			}
			RestClient.create().rootUrl(url).connectionManager(cm).connectionManagerShared(true).build().closeQuietly();
			assertOpen(cm);
		}
	}

	@SuppressWarnings({
		"resource" // The subject is close() itself; the pools are shut down in the finally block.
	})
	@Test void d07_connectionManagerShared_keepHttpClientOpen_defaultManagerLeftOpen() throws Exception {
		var c = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true).keepHttpClientOpen().build();
		var ordinary = ordinaryPool(c);
		var pinned = pinnedPool(c);
		try {
			c.close();
			assertOpen(ordinary);
			assertOpen(pinned);
		} finally {
			ordinary.shutdown();
			pinned.shutdown();
		}
	}

	@SuppressWarnings({
		"resource" // The subject is close() on clients sharing a manager, so the clients are closed explicitly.
	})
	@Test void d08_connectionManagerShared_overriddenCreateConnectionManager_leftOpenOnClose() throws Exception {
		var b = new ReusingBuilder().rootUrl(url).pooled().connectionManagerShared(true);
		var c1 = b.build();
		var c2 = b.build();
		var ordinary = ordinaryPool(c1);
		try {
			assertSame(ordinary, ordinaryPool(c2));
			// An overridden createConnectionManager() may hand the same instance to several clients, so with "shared" set
			// no client shuts it down; each client's pinned pool is still its own.
			var pinned1 = pinnedPool(c1);
			c1.close();
			assertShutDown(pinned1);
			assertPlainOk(c2);
			var pinned2 = pinnedPool(c2);
			c2.close();
			assertShutDown(pinned2);
			assertOpen(ordinary);
		} finally {
			ordinary.shutdown();
		}
	}

	@Test void d09_connectionManagerShared_suppliedDefaultManager_leftOpenOnClose() throws Exception {
		var b = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true);
		var cm = b.createConnectionManager();
		try {
			b.connectionManager(cm);
			try (var c = b.build()) {
				assertPlainOk(c);
			}
			// Supplied through connectionManager(...), so "shared" keeps it open even though the builder created it.
			assertOpen(cm);
		} finally {
			cm.shutdown();
		}
	}

	// Hands out the connection manager its first build() created to every later build().
	static final class ReusingBuilder extends RestClient.Builder<ReusingBuilder> {
		private HttpClientConnectionManager first;
		@Override
		protected HttpClientConnectionManager createConnectionManager() {
			if (first == null)
				first = super.createConnectionManager();
			return first;
		}
	}

	// =================================================================================================================
	// E: a RestClient subclass that overrides the request dispatch may copy the request or send it with another context, so
	// guarded calls are refused
	// =================================================================================================================

	@Test void e01_subclassOverridesRun_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(RunOverride.class)) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("overrides run(HttpHost,HttpRequest,HttpContext)", messages(ex));
			assertContains(RunOverride.class.getName(), messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void e02_subclassOverridesExecuteTarget_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(ExecuteTargetOverride.class)) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("overrides execute(HttpHost,HttpRequest,HttpContext)", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void e03_subclassOverridesExecuteUri_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(ExecuteUriOverride.class)) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("overrides execute(HttpUriRequest,HttpContext)", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void e04_subclassOfOverridingSubclass_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(RunOverrideChild.class)) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains(RunOverride.class.getName(), messages(ex));
		}
	}

	@Test void e05_subclassWithoutDispatchOverride_guardStillApplies() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(OtherOverride.class)) {
			assertGuardedRejected(c);
		}
	}

	@Test void e06_subclassOverridesRun_allowPrivateUrls_succeeds() throws Exception {
		try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).build(RunOverride.class)) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	@Test void e07_guardedHttps_sslContextSet_pinStillApplies() throws Exception {
		try (var c = RestClient.create().rootUrl("https://2130706433:" + tlsPort).sslContext(trustingContext).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("No allowed address for host '2130706433'", messages(ex));
		}
	}

	@Test void e08_subclassCreatesRequestThatRunsOnAnotherThread_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build(OtherThreadRequestClient.class)) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains(OtherThreadRequest.class.getName() + " overrides run()", messages(ex));
			assertPlainOk(c);
		}
		try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).build(OtherThreadRequestClient.class)) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	@SuppressWarnings({
		"resource" // The caller-supplied manager is owned by the client built from it.
	})
	@Test void e09_basicCallHandlerOfAnotherClient_guardedCall_refused() throws Exception {
		try (var other = RestClient.create().connectionManager(new PoolingHttpClientConnectionManager()).build()) {
			var b = RestClient.create().rootUrl(url);
			b.callHandler().impl(new BasicRestCallHandler(other));
			try (var c = b.build()) {
				var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
				assertContains("bound to a different RestClient", messages(ex));
			}
		}
	}

	@Test void e10_reusedBuilder_eachClientsOwnBasicCallHandler_guardStillApplies() throws Exception {
		var b = RestClient.create().rootUrl(url);
		try (var c1 = b.build(); var c2 = b.build()) {
			assertGuardedRejected(c1);
			assertGuardedRejected(c2);
		}
	}

	@SuppressWarnings({
		"resource" // The caller-supplied manager is owned by the client built from it.
	})
	@Test void e11_subclassCreatesRequestBoundToAnotherClient_guardedCall_refused() throws Exception {
		try (var other = RestClient.create().connectionManager(new PoolingHttpClientConnectionManager()).build()) {
			try (var c = RestClient.create().rootUrl(url).build(OtherClientRequestClient.class)) {
				OtherClientRequestClient.other = other;
				var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
				assertContains("bound to a different RestClient", messages(ex));
			}
			try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).build(OtherClientRequestClient.class)) {
				OtherClientRequestClient.other = other;
				assertEquals("hi", c.getRemote(HelloRemote.class).call());
			}
		} finally {
			OtherClientRequestClient.other = null;
		}
	}

	// Creates its requests on another client, so they go out through that client's HttpClient.
	public static class OtherClientRequestClient extends RestClient {
		@SuppressWarnings({
			"resource" // A test fixture set and cleared by the test that closes it.
		})
		static volatile RestClient other;
		public OtherClientRequestClient(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		@SuppressWarnings({
			"resource" // The request is returned to the caller, which owns it.
		})
		protected RestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
			return new RestRequest(other, uri, method, hasBody);
		}
	}

	public static class OtherThreadRequestClient extends RestClient {
		public OtherThreadRequestClient(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		@SuppressWarnings({
			"resource" // The request is returned to the caller, which owns it.
		})
		protected RestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
			return new OtherThreadRequest(this, uri, method, hasBody);
		}
	}

	// Runs the request on another thread, where the guard state of the calling thread is not set.
	public static class OtherThreadRequest extends RestRequest {
		public OtherThreadRequest(RestClient client, URI uri, String method, boolean hasBody) throws RestCallException {
			super(client, uri, method, hasBody);
		}
		@Override
		@SuppressWarnings({
			"resource" // The response is returned to the caller, which owns it.
		})
		public RestResponse run() throws RestCallException {
			var es = Executors.newSingleThreadExecutor();
			try {
				return es.submit(super::run).get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RestCallException(null, e, "Interrupted.");
			} catch (ExecutionException e) {
				throw (RestCallException)e.getCause();
			} finally {
				es.shutdown();
			}
		}
	}

	public static class RunOverride extends RestClient {
		public RunOverride(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		protected HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return super.run(target, request, context);
		}
	}

	public static class RunOverrideChild extends RunOverride {
		public RunOverrideChild(RestClient.Builder<?> builder) {
			super(builder);
		}
	}

	public static class ExecuteTargetOverride extends RestClient {
		public ExecuteTargetOverride(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return super.execute(target, request, context);
		}
	}

	public static class ExecuteUriOverride extends RestClient {
		public ExecuteUriOverride(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException {
			return super.execute(request, context);
		}
	}

	public static class OtherOverride extends RestClient {
		public OtherOverride(RestClient.Builder<?> builder) {
			super(builder);
		}
		// Overloads the dispatch methods are not routed through are not a concern.
		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException {
			return super.execute(target, request);
		}
	}

	// =================================================================================================================
	// F: TLS and pool settings made directly on a caller-supplied HttpClientBuilder reach both pools
	// =================================================================================================================

	@Test void f01_callerBuilder_sslContext_customTrustStore_honoured() throws Exception {
		try (var c = RestClient.create().httpClientBuilder(HttpClientBuilder.create().setSSLContext(trustingContext)).build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
			assertSame(httpsFactory(ordinaryPool(c)), httpsFactory(pinnedPool(c)));
		}
	}

	@Test void f02_callerBuilder_sslHostnameVerifier_honoured() throws Exception {
		var b = HttpClientBuilder.create().setSSLContext(trustingContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
		try (var c = RestClient.create().httpClientBuilder(b).build()) {
			assertEquals("hi", fetch(c, "https://127.0.0.1:" + tlsPort + "/hello"));
		}
	}

	@Test void f03_callerBuilder_sslSocketFactory_sameFactoryInBothPools() throws Exception {
		var factory = new SSLConnectionSocketFactory(trustingContext);
		try (var c = RestClient.create().httpClientBuilder(HttpClientBuilder.create().setSSLSocketFactory(factory)).build()) {
			assertSame(factory, httpsFactory(ordinaryPool(c)));
			assertSame(factory, httpsFactory(pinnedPool(c)));
		}
	}

	@Test void f04_callerBuilder_maxConn_pooledInBothPools() throws Exception {
		var b = HttpClientBuilder.create().setMaxConnTotal(11).setMaxConnPerRoute(3);
		try (var c = RestClient.create().rootUrl(url).httpClientBuilder(b).build()) {
			for (var pool : pools(c)) {
				var p = (PoolingHttpClientConnectionManager)pool;
				assertEquals(11, p.getMaxTotal());
				assertEquals(3, p.getDefaultMaxPerRoute());
			}
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@Test void f05_callerBuilder_publicSuffixMatcher_inDefaultVerifierOfBothPools() throws Exception {
		var matcher = new PublicSuffixMatcher(List.of("com"), null);
		try (var c = RestClient.create().httpClientBuilder(HttpClientBuilder.create().setPublicSuffixMatcher(matcher)).build()) {
			for (var pool : pools(c))
				assertSame(matcher, field(field(httpsFactory(pool), "hostnameVerifier"), "publicSuffixMatcher"));
		}
	}

	@Test void f06_callerBuilder_dnsResolver_ordinaryPoolOnly() throws Exception {
		DnsResolver resolver = SystemDefaultDnsResolver.INSTANCE::resolve;
		try (var c = RestClient.create().rootUrl(url).httpClientBuilder(HttpClientBuilder.create().setDnsResolver(resolver)).build()) {
			assertSame(resolver, field(field(ordinaryPool(c), "connectionOperator"), "dnsResolver"));
			// The pinned pool keeps the pin, so guarded calls are still checked.
			assertSame(PolicyPinningDnsResolver.INSTANCE, field(field(pinnedPool(c), "connectionOperator"), "dnsResolver"));
			assertPlainOk(c);
			assertGuardedRejected(c);
		}
	}

	@Test void f07_builderSetting_winsOverReplacedCallerBuilder() throws Exception {
		// sslContext(...) was set on the builder created first; the replacement carries a context with the default trust store.
		var b = RestClient.create().sslContext(trustingContext).httpClientBuilder(HttpClientBuilder.create().setSSLContext(SSLContexts.createDefault()));
		try (var c = b.build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
		}
	}

	// =================================================================================================================
	// G: the TLS and keep-alive system properties read by useSystemProperties() reach both pools
	// =================================================================================================================

	@Test void g01_useSystemProperties_systemDefaultTrustStore_honoured() throws Exception {
		// What the javax.net.ssl.* properties configure is the JVM's default SSLContext; set it directly so the test is
		// independent of when the JVM first read those properties.
		var previous = SSLContext.getDefault();
		SSLContext.setDefault(trustingContext);
		try (var c = RestClient.create().useSystemProperties().build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
			assertSame(httpsFactory(ordinaryPool(c)), httpsFactory(pinnedPool(c)));
		} finally {
			SSLContext.setDefault(previous);
		}
	}

	@Test void g02_useSystemProperties_httpsProtocols_honoured() throws Exception {
		withSystemProperty("https.protocols", "TLSv1.2", () -> {
			try (var c = RestClient.create().useSystemProperties().sslContext(trustingContext).build()) {
				assertEquals("TLSv1.2", fetch(c, "https://localhost:" + tlsPort + "/protocol"));
				for (var pool : pools(c))
					assertArrayEquals(new String[]{"TLSv1.2"}, (String[])field(httpsFactory(pool), "supportedProtocols"));
			}
		});
	}

	@Test void g02b_useSystemProperties_httpsProtocols_listTrimmed() throws Exception {
		withSystemProperty("https.protocols", "TLSv1.2 , TLSv1.3", () -> {
			try (var c = RestClient.create().useSystemProperties().sslContext(trustingContext).build()) {
				for (var pool : pools(c))
					assertArrayEquals(new String[]{"TLSv1.2","TLSv1.3"}, (String[])field(httpsFactory(pool), "supportedProtocols"));
			}
		});
	}

	@Test void g03_withoutUseSystemProperties_httpsProtocolsIgnored() throws Exception {
		withSystemProperty("https.protocols", "TLSv1.2", () -> {
			try (var c = RestClient.create().sslContext(trustingContext).build()) {
				assertNull(field(httpsFactory(ordinaryPool(c)), "supportedProtocols"));
			}
		});
	}

	@Test void g04_useSystemProperties_maxConnections_pooledInBothPools() throws Exception {
		withSystemProperty("http.maxConnections", "7", () -> {
			try (var c = RestClient.create().rootUrl(url).useSystemProperties().build()) {
				for (var pool : pools(c)) {
					var p = (PoolingHttpClientConnectionManager)pool;
					assertEquals(7, p.getDefaultMaxPerRoute());
					assertEquals(14, p.getMaxTotal());
				}
				assertPlainOk(c);
			}
		});
	}

	@Test void g05_useSystemProperties_maxConnTotalWinsOverMaxConnections() throws Exception {
		withSystemProperty("http.maxConnections", "7", () -> {
			try (var c = RestClient.create().useSystemProperties().maxConnTotal(20).build()) {
				var p = (PoolingHttpClientConnectionManager)ordinaryPool(c);
				assertEquals(7, p.getDefaultMaxPerRoute());
				assertEquals(20, p.getMaxTotal());
			}
		});
	}

	@SuppressWarnings({
		"resource" // The pools are owned and closed by the client; the test only inspects them.
	})
	@Test void g06_useSystemProperties_keepAliveOff_noPoolLimitsApplied() throws Exception {
		withSystemProperty("http.keepAlive", "false", () -> {
			try (var c = RestClient.create().useSystemProperties().build()) {
				for (var pool : pools(c))
					assertInstanceOf(BasicHttpClientConnectionManager.class, pool);
			}
		});
	}

	// =================================================================================================================
	// H: the socket, connection, time-to-live and public-suffix settings reach both pools
	// =================================================================================================================

	@Test void h01_defaultSocketConfig_bothPools() throws Exception {
		var config = SocketConfig.custom().setSoTimeout(12345).build();
		try (var c = RestClient.create().defaultSocketConfig(config).build()) {
			for (var pool : pools(c))
				assertSame(config, ((BasicHttpClientConnectionManager)pool).getSocketConfig());
		}
		try (var c = RestClient.create().pooled().defaultSocketConfig(config).build()) {
			for (var pool : pools(c))
				assertSame(config, ((PoolingHttpClientConnectionManager)pool).getDefaultSocketConfig());
		}
	}

	@Test void h02_defaultConnectionConfig_bothPools() throws Exception {
		var config = ConnectionConfig.custom().setBufferSize(4321).build();
		try (var c = RestClient.create().defaultConnectionConfig(config).build()) {
			for (var pool : pools(c))
				assertSame(config, ((BasicHttpClientConnectionManager)pool).getConnectionConfig());
		}
		try (var c = RestClient.create().pooled().defaultConnectionConfig(config).build()) {
			for (var pool : pools(c))
				assertSame(config, ((PoolingHttpClientConnectionManager)pool).getDefaultConnectionConfig());
		}
	}

	@SuppressWarnings({
		"resource" // The pools are owned and closed by the client; the test only inspects them.
	})
	@Test void h03_connectionTimeToLive_pooledInBothPools() throws Exception {
		try (var c = RestClient.create().rootUrl(url).connectionTimeToLive(5, TimeUnit.SECONDS).build()) {
			for (var pool : pools(c)) {
				var cpool = field(assertInstanceOf(PoolingHttpClientConnectionManager.class, pool), "pool");
				assertEquals(5L, field(cpool, "timeToLive"));
				assertEquals(TimeUnit.SECONDS, field(cpool, "timeUnit"));
			}
			assertPlainOk(c);
		}
	}

	@Test void h04_publicSuffixMatcher_inDefaultVerifierOfBothPools() throws Exception {
		var matcher = new PublicSuffixMatcher(List.of("com"), null);
		try (var c = RestClient.create().publicSuffixMatcher(matcher).build()) {
			for (var pool : pools(c))
				assertSame(matcher, field(field(httpsFactory(pool), "hostnameVerifier"), "publicSuffixMatcher"));
		}
	}

	// =================================================================================================================
	// J: connection-manager settings made through this builder carry over a replaced HttpClientBuilder
	// =================================================================================================================

	@SuppressWarnings({
		"resource" // The pools are owned and closed by the client; the test only inspects them.
	})
	@Test void j01_connectionTimeToLive_carriesOverReplacedBuilder() throws Exception {
		var b = RestClient.create().connectionTimeToLive(5, TimeUnit.SECONDS).httpClientBuilder(HttpClientBuilder.create());
		try (var c = b.build()) {
			for (var pool : pools(c)) {
				var cpool = field(assertInstanceOf(PoolingHttpClientConnectionManager.class, pool), "pool");
				assertEquals(5L, field(cpool, "timeToLive"));
				assertEquals(TimeUnit.SECONDS, field(cpool, "timeUnit"));
			}
		}
	}

	@Test void j02_defaultSocketConfig_carriesOverReplacedBuilder() throws Exception {
		var config = SocketConfig.custom().setSoTimeout(12345).build();
		try (var c = RestClient.create().defaultSocketConfig(config).httpClientBuilder(HttpClientBuilder.create()).build()) {
			for (var pool : pools(c))
				assertSame(config, ((BasicHttpClientConnectionManager)pool).getSocketConfig());
		}
	}

	@Test void j03_defaultConnectionConfig_carriesOverReplacedBuilder() throws Exception {
		var config = ConnectionConfig.custom().setBufferSize(4321).build();
		try (var c = RestClient.create().defaultConnectionConfig(config).httpClientBuilder(HttpClientBuilder.create()).build()) {
			for (var pool : pools(c))
				assertSame(config, ((BasicHttpClientConnectionManager)pool).getConnectionConfig());
		}
	}

	@Test void j04_publicSuffixMatcher_carriesOverReplacedBuilder() throws Exception {
		var matcher = new PublicSuffixMatcher(List.of("com"), null);
		try (var c = RestClient.create().publicSuffixMatcher(matcher).httpClientBuilder(HttpClientBuilder.create()).build()) {
			for (var pool : pools(c))
				assertSame(matcher, field(field(httpsFactory(pool), "hostnameVerifier"), "publicSuffixMatcher"));
		}
	}

	@Test void j05_useSystemProperties_systemDefaultTrustStore_carriesOverReplacedBuilder() throws Exception {
		var previous = SSLContext.getDefault();
		SSLContext.setDefault(trustingContext);
		try (var c = RestClient.create().useSystemProperties().httpClientBuilder(HttpClientBuilder.create()).build()) {
			assertEquals("hi", fetch(c, "https://localhost:" + tlsPort + "/hello"));
		} finally {
			SSLContext.setDefault(previous);
		}
	}

	@Test void j06_useSystemProperties_maxConnections_carriesOverReplacedBuilder() throws Exception {
		withSystemProperty("http.maxConnections", "7", () -> {
			try (var c = RestClient.create().useSystemProperties().httpClientBuilder(HttpClientBuilder.create()).build()) {
				for (var pool : pools(c))
					assertEquals(7, ((PoolingHttpClientConnectionManager)pool).getDefaultMaxPerRoute());
			}
		});
	}

	@Test void j07_builderSocketConfig_winsOverReplacedBuilder() throws Exception {
		var mine = SocketConfig.custom().setSoTimeout(12345).build();
		var theirs = SocketConfig.custom().setSoTimeout(999).build();
		var b = RestClient.create().defaultSocketConfig(mine).httpClientBuilder(HttpClientBuilder.create().setDefaultSocketConfig(theirs));
		try (var c = b.build()) {
			for (var pool : pools(c))
				assertSame(mine, ((BasicHttpClientConnectionManager)pool).getSocketConfig());
		}
	}

	@Test void j08_callerBuilderSocketFactory_winsOverEarlierSslContext() throws Exception {
		// As in Apache HttpClientBuilder, a socket factory wins over any SSL context, wherever each was set.
		var factory = new SSLConnectionSocketFactory(trustingContext);
		var b = RestClient.create().sslContext(SSLContexts.createDefault()).httpClientBuilder(HttpClientBuilder.create().setSSLSocketFactory(factory));
		try (var c = b.build()) {
			for (var pool : pools(c))
				assertSame(factory, httpsFactory(pool));
		}
	}

	// =================================================================================================================
	// I: a reused builder adds the redirect credential guard once
	// =================================================================================================================

	@Test void i01_builderReused_redirectGuardAddedOnce() throws Exception {
		var b = RestClient.create();
		try (var c1 = b.build(); var c2 = b.build()) {
			var interceptors = (List<?>)field(b.httpClientBuilder(), "requestLast");
			assertEquals(1, interceptors.stream().filter(ClassicRedirectCredentialGuard.class::isInstance).count());
		}
	}

	// =================================================================================================================
	// K: guarded calls need the HttpClient wired to the pinned pool, and the guard travels with the request's context
	// =================================================================================================================

	@Test void k01_createHttpClientOverride_returnsOtherClient_guardedCall_refused() throws Exception {
		try (var c = new OtherHttpClientBuilder().rootUrl(url).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("not the one this RestClient wired", messages(ex));
			assertPlainOk(c);
		}
		try (var c = new OtherHttpClientBuilder().rootUrl(url).allowPrivateUrls(true).build()) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	@Test void k02_createHttpClientOverride_wrapsOtherClient_guardedCall_refused() throws Exception {
		try (var c = new WrappingHttpClientBuilder().rootUrl(url).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("not the one this RestClient wired", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void k03_httpClientBuilderBuildOverride_returnsOtherClient_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).httpClientBuilder(new OtherClientHttpClientBuilder()).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("not the one this RestClient wired", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void k04_httpClientBuilderBuildOverride_swapsConnectionManager_guardedCall_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).httpClientBuilder(new SwappingHttpClientBuilder()).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("not the one this RestClient wired", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void k05_createHttpClientOverride_returnsSuperClient_guardStillApplies() throws Exception {
		try (var c = new SuperHttpClientBuilder().rootUrl(url).build()) {
			assertGuardedRejected(c);
		}
	}

	@Test void k06_guardedContext_otherThread_pinned() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build()) {
			var ex = assertThrows(ExecutionException.class, () -> onOtherThread(() -> status(c, guardedContext())));
			assertContains("No allowed address for host '2130706433'", messages(ex));
			// The same request without the marker is an ordinary one.
			assertEquals(200, onOtherThread(() -> status(c, null)));
		}
	}

	@Test void k07_guardedRequest_otherThreadWithoutContext_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).callHandler(OtherThreadCallHandler.class).build()) {
			OtherThreadCallHandler.dropContext = true;
			try (var rc = c.get(url + "/hello")) {
				rc.ssrfGuard();
				var ex = assertThrows(Exception.class, rc::run);
				assertContains("lacks the SSRF-guard marker", messages(ex));
			}
			// An ordinary request through the same handler is unaffected.
			assertPlainOk(c);
		} finally {
			OtherThreadCallHandler.dropContext = false;
		}
	}

	@Test void k11_guardedRequest_otherThreadWithContext_pinned() throws Exception {
		try (var c = RestClient.create().rootUrl(url).callHandler(OtherThreadCallHandler.class).build()) {
			try (var rc = c.get(url + "/hello")) {
				rc.ssrfGuard();
				var ex = assertThrows(Exception.class, rc::run);
				assertContains("No allowed address for host '2130706433'", messages(ex));
			}
			assertPlainOk(c);
		}
	}

	@SuppressWarnings({
		"resource" // context() returns the request itself, which the try-with-resources closes.
	})
	@Test void k12_guardedRequest_callerContext_markedForTheCallOnly() throws Exception {
		try (var c = RestClient.create().rootUrl(url).build()) {
			var context = new BasicHttpContext();
			try (var rc = c.get(url + "/hello")) {
				rc.context(context);
				rc.ssrfGuard();
				var ex = assertThrows(Exception.class, rc::run);
				assertContains("No allowed address for host '2130706433'", messages(ex));
			}
			assertNull(context.getAttribute(SsrfGuardMarker.ATTRIBUTE));
		}
	}

	// Sends each request from another thread, where the guard thread-local of the calling thread is not set.
	public static class OtherThreadCallHandler extends BasicRestCallHandler {
		static volatile boolean dropContext;
		public OtherThreadCallHandler(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			try {
				return onOtherThread(() -> super.run(target, request, dropContext ? null : context));
			} catch (ExecutionException e) {
				if (e.getCause() instanceof IOException e2)
					throw e2;
				throw (RuntimeException)e.getCause();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException(e.getMessage());
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}

	@Test void k08_httpProcessorReplaced_guardedContext_otherThread_newConnection_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).httpProcessor(hostOnlyProcessor()).build()) {
			var ex = assertThrows(ExecutionException.class, () -> onOtherThread(() -> status(c, guardedContext())));
			assertContains("request to open a connection of the ordinary pool", messages(ex));
		}
	}

	@Test void k09_httpProcessorReplaced_guardedContext_otherThread_reusedConnection_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).pooled().httpProcessor(hostOnlyProcessor()).build()) {
			// Leaves an open connection to the route in the ordinary pool.
			assertEquals(200, status(c, null));
			var ex = assertThrows(ExecutionException.class, () -> onOtherThread(() -> status(c, guardedContext())));
			assertContains("request over an open connection of the ordinary pool", messages(ex));
		}
	}

	@Test void k10_redirectStrategy_guardedContext_notFollowed() throws Exception {
		var response = new BasicHttpResponse(HttpVersion.HTTP_1_1, 302, "Found");
		response.addHeader("Location", "http://example.com/");
		var request = new HttpGet("http://example.com/start");
		assertFalse(new PolicyAwareRedirectStrategy().isRedirected(request, response, guardedContext()));
		assertTrue(new PolicyAwareRedirectStrategy().isRedirected(request, response, new BasicHttpContext()));
	}

	@Test void k13_httpClientBuilderBuildOverride_returnsSuperClient_guardedCall_refused() throws Exception {
		assertBuilderOverrideRefused(new SuperBuildHttpClientBuilder(), "SuperBuildHttpClientBuilder overrides build()");
	}

	@Test void k14_httpClientBuilderCreateMainExecOverride_guardedCall_refused() throws Exception {
		assertBuilderOverrideRefused(new MainExecSwapHttpClientBuilder(), "MainExecSwapHttpClientBuilder overrides createMainExec(");
	}

	@Test void k15_httpClientBuilderDecorateMainExecOverride_guardedCall_refused() throws Exception {
		assertBuilderOverrideRefused(new DecorateMainExecHttpClientBuilder(), "DecorateMainExecHttpClientBuilder overrides decorateMainExec(");
	}

	@Test void k16_httpClientBuilderDecorateProtocolExecOverride_guardedCall_refused() throws Exception {
		assertBuilderOverrideRefused(new DecorateProtocolExecHttpClientBuilder(), "DecorateProtocolExecHttpClientBuilder overrides decorateProtocolExec(");
	}

	@Test void k17_httpClientBuilderOverride_inheritedFromSuperclass_guardedCall_refused() throws Exception {
		assertBuilderOverrideRefused(new InheritingHttpClientBuilder(), "DecorateMainExecHttpClientBuilder overrides decorateMainExec(");
	}

	private static void assertBuilderOverrideRefused(HttpClientBuilder b, String expected) throws Exception {
		try (var c = RestClient.create().rootUrl(url).httpClientBuilder(b).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains(expected, messages(ex));
			assertPlainOk(c);
		}
		try (var c = RestClient.create().rootUrl(url).allowPrivateUrls(true).httpClientBuilder(b).build()) {
			assertEquals("hi", c.getRemote(HelloRemote.class).call());
		}
	}

	@Test void k18_httpProcessor_guardedCallOnCallingThread_refused() throws Exception {
		try (var c = RestClient.create().rootUrl(url).httpProcessor(hostOnlyProcessor()).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("httpProcessor(...) replaces the client's request interceptors", messages(ex));
			assertPlainOk(c);
		}
	}

	@Test void k19_guardedRedirect_crossOrigin_credentialsStripped() throws Exception {
		var hops = guardedRedirect("http://93.184.216.35/end");
		assertSize(2, hops);
		assertEquals("Bearer secret|session=abc|key|Basic proxy", hops.get(0).credentials);
		assertEquals("null|null|null|null", hops.get(1).credentials);
	}

	@Test void k20_guardedRedirect_sameOrigin_credentialsKept() throws Exception {
		var hops = guardedRedirect("http://93.184.216.34/end");
		assertSize(2, hops);
		assertEquals("Bearer secret|session=abc|key|Basic proxy", hops.get(1).credentials);
	}

	@Test void k21_guardedRedirect_crossOriginThenBack_credentialsStayStripped() throws Exception {
		var hops = guardedRedirect("http://93.184.216.35/back");
		assertSize(3, hops);
		assertEquals("null|null|null|null", hops.get(1).credentials);
		assertEquals("null|null|null|null", hops.get(2).credentials);
	}

	@Test void k22_guardedRedirect_everyHopMarked() throws Exception {
		var hops = guardedRedirect("http://93.184.216.35/back");
		assertSize(3, hops);
		for (var h : hops)
			assertEquals(Boolean.TRUE, h.marker);
		// Each hop has its own context, so no authentication state is carried from one host to the next.
		assertNotSame(hops.get(0).context, hops.get(1).context);
	}

	@Test void k23_guardRefused_nextOrdinaryCallOnSameThread_notGuarded() throws Exception {
		try (var c = RestClient.create().rootUrl(url).pooled().build()) {
			var ex = assertThrows(Exception.class, () -> status(c, guardedContext()));
			assertContains("No allowed address for host '2130706433'", messages(ex));
			assertEquals(200, status(c, null));
			assertEquals(200, status(c, null));
		}
		try (var c = RestClient.create().rootUrl("http://93.184.216.34").proxy(new HttpHost("127.0.0.1", port)).build()) {
			assertThrows(Exception.class, () -> status(c, guardedContext(), "http://93.184.216.34/hello"));
			// The refused lease left no guard decision behind: the proxied ordinary call goes through.
			assertEquals(200, status(c, null, "http://93.184.216.34/hello"));
		}
	}

	@Test void k24_guardedRedirect_noContext_crossOrigin_interceptorAndDefaultHeadersStripped() throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.35/end")) {
			var b = HttpClientBuilder.create().setDefaultHeaders(List.of(new BasicHeader("X-API-Key", "dflt")))
				.addInterceptorLast((HttpRequestInterceptor)(r, x) -> r.setHeader("Authorization", "Bearer itcp"));
			try (var c = routed.client(RestClient.create().httpClientBuilder(b))) {
				assertEquals(200, routed.guarded(c, null));
			}
			assertEquals("93.184.216.34:Bearer itcp|dflt,93.184.216.35:null|null", routed.hops());
			// Real Apache execution: each hop was pinned to its own (public) address.
			assertEquals("[/93.184.216.34:443, /93.184.216.35:443]", routed.connects.toString());
		}
	}

	@Test void k25_guardedRedirect_noContext_sameOrigin_interceptorAndDefaultHeadersKept() throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.34/end")) {
			var b = HttpClientBuilder.create().setDefaultHeaders(List.of(new BasicHeader("X-API-Key", "dflt")))
				.addInterceptorLast((HttpRequestInterceptor)(r, x) -> r.setHeader("Authorization", "Bearer itcp"));
			try (var c = routed.client(RestClient.create().httpClientBuilder(b))) {
				assertEquals(200, routed.guarded(c, null));
			}
			assertEquals("93.184.216.34:Bearer itcp|dflt,93.184.216.34:Bearer itcp|dflt", routed.hops());
		}
	}

	@Test void k26_guardedRedirect_callerContext_crossOrigin_targetAuthStateReset() throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.35/end")) {
			routed.challenge = true;
			try (var c = routed.client(RestClient.create().basicAuth("93.184.216.34", 443, "user", "pw"))) {
				assertEquals(200, routed.guarded(c, HttpClientContext.create()));
			}
			// Challenged, answered for the first host only; the answer is not carried to the second.
			assertEquals("93.184.216.34:null|null,93.184.216.34:Basic dXNlcjpwdw==|null,93.184.216.35:null|null", routed.hops());
		}
	}

	@Test void k27_guardedRedirect_callerContext_sameOrigin_targetAuthStateKept() throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.34/end")) {
			routed.challenge = true;
			try (var c = routed.client(RestClient.create().basicAuth("93.184.216.34", 443, "user", "pw"))) {
				assertEquals(200, routed.guarded(c, HttpClientContext.create()));
			}
			assertEquals("93.184.216.34:null|null,93.184.216.34:Basic dXNlcjpwdw==|null,93.184.216.34:Basic dXNlcjpwdw==|null", routed.hops());
		}
	}

	@Test void k28_guardedRedirect_reusedCallerContext_originIsTheCallsFirstHop() throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.35/end")) {
			var b = HttpClientBuilder.create().addInterceptorLast((HttpRequestInterceptor)(r, x) -> r.setHeader("Authorization", "Bearer itcp"));
			try (var c = routed.client(RestClient.create().httpClientBuilder(b))) {
				var context = HttpClientContext.create();
				// An earlier ordinary call with the same context, to the host the guarded call is later redirected to.
				assertEquals(200, status(c, context, "https://93.184.216.35/end"));
				assertEquals(200, routed.guarded(c, context));
			}
			assertEquals("93.184.216.35:Bearer itcp|null,93.184.216.34:Bearer itcp|null,93.184.216.35:null|null", routed.hops());
		}
	}

	@Test void k29_createHttpClientOverride_changesBuilderAfterSuper_guardedCall_refused() throws Exception {
		try (var c = new RebuildingHttpClientBuilder().rootUrl(url).build()) {
			var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
			assertContains("httpProcessor(...) replaces the client's request interceptors", messages(ex));
		}
	}

	@Test void k30_createHttpClientOverride_lateRequestInterceptor_guardedCall_refused() throws Exception {
		assertLateChangeRefused(b -> b.addInterceptorLast((HttpRequestInterceptor)(r, x) -> r.setHeader("Authorization", "Bearer late")),
			"a request interceptor was added after the SSRF guard's own");
	}

	@Test void k31_createHttpClientOverride_redirectStrategyReplaced_guardedCall_refused() throws Exception {
		assertLateChangeRefused(b -> b.setRedirectStrategy(DefaultRedirectStrategy.INSTANCE), "the redirect strategy was replaced");
	}

	@Test void k32_createHttpClientOverride_requestExecutorReplaced_guardedCall_refused() throws Exception {
		assertLateChangeRefused(b -> b.setRequestExecutor(new HttpRequestExecutor()), "the request executor was replaced");
	}

	// A createHttpClient() override that calls super, makes the change to the HttpClientBuilder and builds again: the
	// guarded @Remote call (whose /start redirects to another origin) is refused before anything is sent, and an ordinary
	// call still goes through.
	private static void assertLateChangeRefused(java.util.function.Consumer<HttpClientBuilder> change, String expected) throws Exception {
		try (var routed = new RoutedServer("https://93.184.216.35/end")) {
			try (var c = routed.client(new LateChangeHttpClientBuilder(change).rootUrl("https://93.184.216.34"))) {
				var ex = assertThrows(Exception.class, () -> c.getRemote(StartRemote.class).start());
				assertContains(expected, messages(ex));
				assertEquals("", routed.hops());
				assertEquals(200, status(c, null, "https://93.184.216.35/end"));
			}
		}
	}

	// Calls super.createHttpClient(), then changes the HttpClientBuilder and builds again.
	static final class LateChangeHttpClientBuilder extends RestClient.Builder<LateChangeHttpClientBuilder> {
		private final java.util.function.Consumer<HttpClientBuilder> change;
		LateChangeHttpClientBuilder(java.util.function.Consumer<HttpClientBuilder> change) {
			this.change = change;
		}
		@Override
		@SuppressWarnings({
			"resource" // The discarded client shares the builder's connection manager with the one returned, so it must stay open.
		})
		protected CloseableHttpClient createHttpClient() {
			super.createHttpClient();
			change.accept(httpClientBuilder());
			return httpClientBuilder().build();
		}
	}

	// A local HTTP server reached through a routing https socket factory: the client resolves and pins the public address
	// of each hop as usual (recorded in connects), and the factory then connects the socket to this server instead.
	// /start answers with a 302 to the given location (after a Basic challenge, if challenge is set), /end with 200.
	private static final class RoutedServer implements AutoCloseable {
		final HttpServer httpServer;
		final List<String> hops = Collections.synchronizedList(new ArrayList<>());
		final List<InetSocketAddress> connects = Collections.synchronizedList(new ArrayList<>());
		volatile boolean challenge;

		RoutedServer(String location) throws IOException {
			httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			httpServer.createContext("/", x -> {
				var h = x.getRequestHeaders();
				var authorization = h.getFirst("Authorization");
				hops.add(h.getFirst("Host") + ":" + authorization + "|" + h.getFirst("X-API-Key"));
				var path = x.getRequestURI().getPath();
				if (path.equals("/start") && challenge && authorization == null) {
					x.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"r\"");
					x.sendResponseHeaders(401, -1);
				} else if (path.equals("/start")) {
					x.getResponseHeaders().add("Location", location);
					x.sendResponseHeaders(302, -1);
				} else {
					x.sendResponseHeaders(200, -1);
				}
				x.close();
			});
			httpServer.start();
		}

		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		RestClient client(RestClient.Builder<?> b) {
			var port2 = httpServer.getAddress().getPort();
			return b.sslSocketFactory(new LayeredConnectionSocketFactory() {
				@Override
				public Socket createSocket(HttpContext context) {
					return new Socket();
				}
				@Override
				public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress,
						InetSocketAddress localAddress, HttpContext context) throws IOException {
					connects.add(remoteAddress);
					sock.connect(new InetSocketAddress("127.0.0.1", port2), connectTimeout);
					return sock;
				}
				@Override
				public Socket createLayeredSocket(Socket socket, String target, int port3, HttpContext context) {
					return socket;
				}
			}).build();
		}

		// Sends a guarded GET for https://93.184.216.34/start with the given context (null for none).
		@SuppressWarnings({
			"resource" // context() returns the request itself, which the try-with-resources closes.
		})
		int guarded(RestClient c, HttpContext context) throws Exception {
			try (var rc = c.get("https://93.184.216.34/start")) {
				if (context != null)
					rc.context(context);
				rc.ssrfGuard();
				try (var r = rc.run()) {
					return r.getStatusCode();
				}
			}
		}

		String hops() {
			return String.join(",", hops);
		}

		@Override
		public void close() {
			httpServer.stop(0);
		}
	}

	// Calls super.createHttpClient(), then sets an HttpProcessor on the builder and builds again.
	static final class RebuildingHttpClientBuilder extends RestClient.Builder<RebuildingHttpClientBuilder> {
		@Override
		@SuppressWarnings({
			"resource" // The discarded client shares the builder's connection manager with the one returned, so it must stay open.
		})
		protected CloseableHttpClient createHttpClient() {
			super.createHttpClient();
			httpClientBuilder().setHttpProcessor(hostOnlyProcessor());
			return httpClientBuilder().build();
		}
	}

	// Sends a guarded GET for http://93.184.216.34/start with credential headers (one in lower case: header names are
	// case-insensitive) through RedirectingCallHandler, which
	// answers /start with a 302 to the given location, /back with a 302 to /end on the first host, and /end with 200.
	@SuppressWarnings({
		"resource" // header() returns the request itself, which the try-with-resources closes.
	})
	private static List<RedirectingCallHandler.Hop> guardedRedirect(String location) throws Exception {
		RedirectingCallHandler.HOPS.clear();
		RedirectingCallHandler.location = location;
		try (var c = RestClient.create().callHandler(RedirectingCallHandler.class).build();
				var rc = c.get("http://93.184.216.34/start").header("Authorization", "Bearer secret").header("Cookie", "session=abc").header("x-api-key", "key")
					.header("Proxy-Authorization", "Basic proxy")) {
			rc.ssrfGuard();
			try (var r = rc.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		return new ArrayList<>(RedirectingCallHandler.HOPS);
	}

	// Answers without a network: /start redirects to the configured location, /back to /end on the first host, /end is 200.
	public static class RedirectingCallHandler extends BasicRestCallHandler {
		record Hop(String uri, String credentials, Object marker, HttpContext context) {}
		static final List<Hop> HOPS = Collections.synchronizedList(new ArrayList<>());
		static volatile String location;
		public RedirectingCallHandler(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) {
			var uri = request.getRequestLine().getUri();
			HOPS.add(new Hop(uri, value(request, "Authorization") + "|" + value(request, "Cookie") + "|" + value(request, "X-API-Key")
				+ "|" + value(request, "Proxy-Authorization"),
				context == null ? null : context.getAttribute(SsrfGuardMarker.ATTRIBUTE), context));
			var path = URI.create(uri).getPath();
			var response = new BasicHttpResponse(HttpVersion.HTTP_1_1, path.equals("/end") ? 200 : 302, "x");
			if (path.equals("/start"))
				response.addHeader("Location", location);
			else if (path.equals("/back"))
				response.addHeader("Location", "http://93.184.216.34/end");
			response.setEntity(new StringEntity("", ContentType.TEXT_PLAIN));
			return response;
		}
		private static String value(HttpRequest request, String name) {
			var h = request.getFirstHeader(name);
			return h == null ? null : h.getValue();
		}
	}

	// Overrides build(), returning the client super.build() built.
	static final class SuperBuildHttpClientBuilder extends HttpClientBuilder {
		@Override
		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		public CloseableHttpClient build() {
			return super.build();
		}
	}

	// Swaps the connection manager and request executor at the exec-chain layer; build() itself is not overridden.
	static final class MainExecSwapHttpClientBuilder extends HttpClientBuilder {
		@Override
		@SuppressWarnings({
			"resource" // The connection manager is handed to the exec chain, which owns it.
		})
		protected ClientExecChain createMainExec(HttpRequestExecutor requestExec, HttpClientConnectionManager connManager,
				ConnectionReuseStrategy reuseStrategy, ConnectionKeepAliveStrategy keepAliveStrategy, HttpProcessor proxyHttpProcessor,
				AuthenticationStrategy targetAuthStrategy, AuthenticationStrategy proxyAuthStrategy, UserTokenHandler userTokenHandler) {
			return super.createMainExec(new HttpRequestExecutor(), new PoolingHttpClientConnectionManager(), reuseStrategy,
				keepAliveStrategy, proxyHttpProcessor, targetAuthStrategy, proxyAuthStrategy, userTokenHandler);
		}
	}

	// Decorates the main exec (here a pass-through: what a decorator does with the request is not knowable).
	static class DecorateMainExecHttpClientBuilder extends HttpClientBuilder {
		@Override
		protected ClientExecChain decorateMainExec(ClientExecChain mainExec) {
			return mainExec;
		}
	}

	// Inherits the decorateMainExec override.
	static final class InheritingHttpClientBuilder extends DecorateMainExecHttpClientBuilder {}

	// Decorates the protocol exec (a pass-through).
	static final class DecorateProtocolExecHttpClientBuilder extends HttpClientBuilder {
		@Override
		protected ClientExecChain decorateProtocolExec(ClientExecChain protocolExec) {
			return protocolExec;
		}
	}

	// Calls super.createHttpClient(), then returns a client of its own.
	static final class OtherHttpClientBuilder extends RestClient.Builder<OtherHttpClientBuilder> {
		@Override
		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		protected CloseableHttpClient createHttpClient() {
			try {
				super.createHttpClient().close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return HttpClientBuilder.create().build();
		}
	}

	// Calls super.createHttpClient(), then returns a wrapper around a client of its own.
	static final class WrappingHttpClientBuilder extends RestClient.Builder<WrappingHttpClientBuilder> {
		@Override
		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		protected CloseableHttpClient createHttpClient() {
			try {
				super.createHttpClient().close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return new WrappingHttpClient(HttpClientBuilder.create().build());
		}
	}

	// Returns the client super.createHttpClient() built.
	static final class SuperHttpClientBuilder extends RestClient.Builder<SuperHttpClientBuilder> {
		@Override
		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		protected CloseableHttpClient createHttpClient() {
			return super.createHttpClient();
		}
	}

	// Builds the client it was configured for, then returns another one.
	static final class OtherClientHttpClientBuilder extends HttpClientBuilder {
		@Override
		@SuppressWarnings({
			"resource" // The client is returned to the caller, which owns it.
		})
		public CloseableHttpClient build() {
			try {
				super.build().close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			return HttpClientBuilder.create().build();
		}
	}

	// Replaces the connection manager it was given before building.
	static final class SwappingHttpClientBuilder extends HttpClientBuilder {
		@Override
		@SuppressWarnings({
			"resource" // The connection manager is handed to the built client, which owns it.
		})
		public CloseableHttpClient build() {
			setConnectionManager(new PoolingHttpClientConnectionManager());
			return super.build();
		}
	}

	@SuppressWarnings({
		"deprecation", // getParams() and getConnectionManager() are abstract in CloseableHttpClient.
		"resource" // The wrapped client is owned by this wrapper and closed in close(); doExecute() returns the response to the caller.
	})
	static final class WrappingHttpClient extends CloseableHttpClient {
		private final CloseableHttpClient inner;
		WrappingHttpClient(CloseableHttpClient inner) {
			this.inner = inner;
		}
		@Override
		protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			return inner.execute(target, request, context);
		}
		@Override
		public void close() throws IOException {
			inner.close();
		}
		@Override
		public org.apache.http.params.HttpParams getParams() {
			return inner.getParams();
		}
		@Override
		public org.apache.http.conn.ClientConnectionManager getConnectionManager() {
			return inner.getConnectionManager();
		}
	}

	// A context carrying the SSRF-guard marker of a guarded call.
	private static HttpContext guardedContext() {
		var context = new BasicHttpContext();
		context.setAttribute(SsrfGuardMarker.ATTRIBUTE, Boolean.TRUE);
		return context;
	}

	// Sends a plain GET for /hello through the client's HttpClient with the given context, and returns the status.
	private static int status(RestClient c, HttpContext context) throws IOException {
		return status(c, context, url + "/hello");
	}

	private static int status(RestClient c, HttpContext context, String uri) throws IOException {
		try (var r = (CloseableHttpResponse)c.execute(new HttpGet(uri), context)) {
			EntityUtils.consume(r.getEntity());
			return r.getStatusLine().getStatusCode();
		}
	}

	// Runs the call on a new thread, where no guard thread-local is set.
	private static <T> T onOtherThread(Callable<T> call) throws Exception {
		var es = Executors.newSingleThreadExecutor();
		try {
			return es.submit(call).get();
		} finally {
			es.shutdown();
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	// A replacement HttpProcessor that keeps only the Host header interceptor.  Any processor set this way trips the
	// guard, and without RequestTargetHost the plain calls go out with no Host header, which newer JDK HttpServers
	// (21.0.13 and later) reject with a 400.
	private static HttpProcessor hostOnlyProcessor() {
		return HttpProcessorBuilder.create().add(new RequestTargetHost()).build();
	}

	private static void withSystemProperty(String name, String value, ThrowingRunnable r) throws Exception {
		var previous = System.getProperty(name);
		System.setProperty(name, value);
		try {
			r.run();
		} finally {
			if (previous == null)
				System.clearProperty(name);
			else
				System.setProperty(name, previous);
		}
	}

	private static List<HttpClientConnectionManager> pools(RestClient c) throws Exception {
		return List.of(ordinaryPool(c), pinnedPool(c));
	}

	private static String fetch(RestClient c, String uri) throws Exception {
		try (var req = c.get(uri); var res = req.run()) {
			return res.getContent().asString();
		}
	}

	private static void assertPlainOk(RestClient c) throws Exception {
		assertEquals("hi", fetch(c, url + "/hello"));
	}

	private static void assertGuardedRejected(RestClient c) {
		var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
		assertContains("No allowed address for host '2130706433'", messages(ex));
	}

	@SuppressWarnings({
		"resource" // The leased connection goes back to the pool through releaseConnection(), not close().
	})
	private static void assertOpen(HttpClientConnectionManager pool) throws Exception {
		var conn = pool.requestConnection(new HttpRoute(new HttpHost("127.0.0.1", port)), null).get(1, TimeUnit.SECONDS);
		pool.releaseConnection(conn, null, 0, TimeUnit.SECONDS);
	}

	private static void assertShutDown(HttpClientConnectionManager pool) {
		var route = new HttpRoute(new HttpHost("127.0.0.1", port));
		assertThrows(IllegalStateException.class, () -> lease(pool, route));
	}

	@SuppressWarnings({
		"resource" // The test only needs the lease to succeed or throw; the connection is never used.
	})
	private static void lease(HttpClientConnectionManager pool, HttpRoute route) throws Exception {
		pool.requestConnection(route, null).get(1, TimeUnit.SECONDS);
	}

	private static HttpClientConnectionManager ordinaryPool(RestClient c) throws Exception {
		return (HttpClientConnectionManager)field(field(c.httpClient, "connManager"), "ordinary");
	}

	private static HttpClientConnectionManager pinnedPool(RestClient c) throws Exception {
		return (HttpClientConnectionManager)field(field(c.httpClient, "connManager"), "pinned");
	}

	@SuppressWarnings({
		"unchecked" // The operator's registry is a Lookup<ConnectionSocketFactory>.
	})
	private static ConnectionSocketFactory httpsFactory(HttpClientConnectionManager pool) throws Exception {
		var registry = (Lookup<ConnectionSocketFactory>)field(field(pool, "connectionOperator"), "socketFactoryRegistry");
		return registry.lookup("https");
	}

	private static Object field(Object o, String name) throws Exception {
		for (var cl = o.getClass(); cl != null; cl = cl.getSuperclass()) {
			try {
				var f = cl.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(o);
			} catch (@SuppressWarnings("unused") NoSuchFieldException e) { /* Try the superclass. */ }
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
}
