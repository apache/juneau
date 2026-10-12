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

import static org.apache.juneau.test.bct.BctAssertions.*;
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
import org.apache.http.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.ssl.*;
import org.apache.http.conn.util.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.http.ssl.SSLContexts;
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
		exchange.getRequestBody().readAllBytes();
		var body = "hi".getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	// Answers with the TLS protocol version the connection negotiated.
	private static void protocol(HttpExchange exchange) throws IOException {
		exchange.getRequestBody().readAllBytes();
		var body = ((HttpsExchange)exchange).getSSLSession().getProtocol().getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
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
		var out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
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

	public interface HelloRemoteDeclaringRestCallException {
		@RemoteOp(method = "GET", path = "/hello")
		String call() throws RestCallException;
	}

	// =================================================================================================================
	// A: the TLS and pool-size builder settings reach both pools
	// =================================================================================================================

	@Test void a01_sslContext_customTrustStore_honoured() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).build()) {
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
		}
	}

	@Test void a02_sslContext_defaultHostnameVerification_stillApplies() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).build()) {
			var ex = assertThrows(Exception.class, () -> c.get("https://127.0.0.1:" + tlsPort + "/hello").run());
			assertContains("doesn't match any of the subject alternative names", messages(ex));
		}
	}

	@Test void a03_sslHostnameVerifier_honoured() throws Exception {
		try (var c = RestClient.create().sslContext(trustingContext).sslHostnameVerifier(NoopHostnameVerifier.INSTANCE).build()) {
			assertEquals("hi", c.get("https://127.0.0.1:" + tlsPort + "/hello").run().getContent().asString());
		}
	}

	@Test void a04_sslSocketFactory_honoured() throws Exception {
		try (var c = RestClient.create().sslSocketFactory(new SSLConnectionSocketFactory(trustingContext)).build()) {
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
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
			assertEquals(new PoolingHttpClientConnectionManager().getDefaultMaxPerRoute(), ordinary.getDefaultMaxPerRoute());
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
			var ex = assertThrows(Exception.class, () -> c.get("https://localhost:" + tlsPort + "/hello").run());
			assertContains("PKIX path building failed", messages(ex));
		}
	}

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
			assertEquals("hi", c.get("http://target.invalid/hello").run().getContent().asString());
		}
	}

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
	// C: a custom RestCallHandler may leave the calling thread, so guarded calls through one are refused
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

	@Test void d05_connectionManagerShared_defaultManager_shutDownOnCloseQuietly() throws Exception {
		var c = RestClient.create().rootUrl(url).pooled().connectionManagerShared(true).build();
		assertPlainOk(c);
		var ordinary = ordinaryPool(c);
		c.closeQuietly();
		assertShutDown(ordinary);
		assertShutDown(pinnedPool(c));
	}

	@Test void d06_connectionManagerShared_callerSuppliedManager_leftOpenOnClose() throws Exception {
		var cm = new PoolingHttpClientConnectionManager();
		try {
			try (var c = RestClient.create().rootUrl(url).connectionManager(cm).connectionManagerShared(true).build()) {
				assertPlainOk(c);
			}
			RestClient.create().rootUrl(url).connectionManager(cm).connectionManagerShared(true).build().closeQuietly();
			assertOpen(cm);
		} finally {
			cm.shutdown();
		}
	}

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
		b.connectionManager(cm);
		try {
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
	// E: a RestClient subclass that overrides the request dispatch may leave the calling thread, so guarded calls are refused
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
		static volatile RestClient other;
		public OtherClientRequestClient(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
		protected RestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
			return new RestRequest(other, uri, method, hasBody);
		}
	}

	public static class OtherThreadRequestClient extends RestClient {
		public OtherThreadRequestClient(RestClient.Builder<?> builder) {
			super(builder);
		}
		@Override
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
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
			assertSame(httpsFactory(ordinaryPool(c)), httpsFactory(pinnedPool(c)));
		}
	}

	@Test void f02_callerBuilder_sslHostnameVerifier_honoured() throws Exception {
		var b = HttpClientBuilder.create().setSSLContext(trustingContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
		try (var c = RestClient.create().httpClientBuilder(b).build()) {
			assertEquals("hi", c.get("https://127.0.0.1:" + tlsPort + "/hello").run().getContent().asString());
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
		DnsResolver resolver = host -> SystemDefaultDnsResolver.INSTANCE.resolve(host);
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
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
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
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
			assertSame(httpsFactory(ordinaryPool(c)), httpsFactory(pinnedPool(c)));
		} finally {
			SSLContext.setDefault(previous);
		}
	}

	@Test void g02_useSystemProperties_httpsProtocols_honoured() throws Exception {
		withSystemProperty("https.protocols", "TLSv1.2", () -> {
			try (var c = RestClient.create().useSystemProperties().sslContext(trustingContext).build()) {
				assertEquals("TLSv1.2", c.get("https://localhost:" + tlsPort + "/protocol").run().getContent().asString());
				for (var pool : pools(c))
					assertArrayEquals(new String[]{"TLSv1.2"}, (String[])field(httpsFactory(pool), "supportedProtocols"));
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
			assertEquals("hi", c.get("https://localhost:" + tlsPort + "/hello").run().getContent().asString());
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
	// Helpers
	// =================================================================================================================

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
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

	private static void assertPlainOk(RestClient c) throws Exception {
		assertEquals("hi", c.get(url + "/hello").run().getContent().asString());
	}

	private static void assertGuardedRejected(RestClient c) {
		var ex = assertThrows(Exception.class, () -> c.getRemote(HelloRemote.class).call());
		assertContains("No allowed address for host '2130706433'", messages(ex));
	}

	private static void assertOpen(HttpClientConnectionManager pool) throws Exception {
		var conn = pool.requestConnection(new HttpRoute(new HttpHost("127.0.0.1", port)), null).get(1, TimeUnit.SECONDS);
		pool.releaseConnection(conn, null, 0, TimeUnit.SECONDS);
	}

	private static void assertShutDown(HttpClientConnectionManager pool) {
		var route = new HttpRoute(new HttpHost("127.0.0.1", port));
		assertThrows(IllegalStateException.class, () -> pool.requestConnection(route, null).get(1, TimeUnit.SECONDS));
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
