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
package org.apache.juneau.rest.client.okhttp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Tests the origin comparison of {@link OkHttpRedirectCredentialGuard}.
 *
 * <p>
 * A host name containing an underscore (here {@code a_b}, as in a Docker Compose service name) is accepted by OkHttp,
 * but {@link java.net.URI} parses it with a {@code null} host, so the guard must compare origins on OkHttp's own
 * {@link okhttp3.HttpUrl} rather than on {@link java.net.URI}.
 */
@SuppressWarnings({
	"resource" // Transport/response Closeables are exercised inline; lifecycle is managed by the test.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class OkHttpRedirectCredentialGuard_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/echo", exchange -> {
			var auth = exchange.getRequestHeaders().getFirst("Authorization");
			var cookie = exchange.getRequestHeaders().getFirst("Cookie");
			var body = ("auth=" + (auth != null ? auth : "none") + "|cookie=" + (cookie != null ? cookie : "none")).getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/to-underscore-host", exchange -> redirect(exchange, "http://a_b:" + port + "/echo"));
		server.createContext("/to-loopback", exchange -> redirect(exchange, "http://127.0.0.1:" + port + "/echo"));
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	//====================================================================================================
	// a - Underscore host names, end to end against a real server.
	//====================================================================================================

	@Test
	void a01_crossOriginRedirect_toUnderscoreHost_credentialsStripped() throws Exception {
		assertEquals("auth=none|cookie=none", get("http://127.0.0.1:" + port + "/to-underscore-host"));
	}

	@Test
	void a02_sameOrigin_underscoreHost_noRedirect_credentialsKept() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123", get("http://a_b:" + port + "/echo"));
	}

	@Test
	void a03_sameOriginRedirect_underscoreHost_credentialsKept() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123", get("http://a_b:" + port + "/to-underscore-host"));
	}

	@Test
	void a04_crossOriginRedirect_fromUnderscoreHost_credentialsStripped() throws Exception {
		assertEquals("auth=none|cookie=none", get("http://a_b:" + port + "/to-loopback"));
	}

	private static String get(String url) throws Exception {
		Dns dns = host -> "a_b".equals(host) ? List.of(InetAddress.getByName("127.0.0.1")) : Dns.SYSTEM.lookup(host);
		var client = new OkHttpClient.Builder()
			.proxy(Proxy.NO_PROXY)
			.dns(dns)
			.addNetworkInterceptor(new OkHttpRedirectCredentialGuard())
			.build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var request = TransportRequest.builder()
				.method("GET")
				.uri(url)
				.header("Authorization", "Bearer secret-token")
				.header("Cookie", "session=abc123")
				.build();
			try (var response = transport.execute(request)) {
				assertEquals(200, response.getStatusCode());
				return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
			}
		}
	}

	//====================================================================================================
	// b - Origin components (scheme, host, port), driven directly through the interceptor.
	//====================================================================================================

	@Test
	void b01_sameOrigin_forwarded() throws Exception {
		assertForwarded("https://a_b:8443/x", "https://a_b:8443/y?z=1");
	}

	@Test
	void b02_defaultPort_explicitVsImplicit_forwarded() throws Exception {
		assertForwarded("https://a_b/x", "https://a_b:443/y");
	}

	@Test
	void b03_hostCase_forwarded() throws Exception {
		assertForwarded("http://A_B:8080/x", "http://a_b:8080/y");
	}

	@Test
	void b04_schemeUpgrade_stripped() throws Exception {
		assertStripped("http://a_b:8080/x", "https://a_b:8080/y");
	}

	@Test
	void b05_schemeDowngrade_stripped() throws Exception {
		assertStripped("https://a_b:8443/x", "http://a_b:8443/y");
	}

	@Test
	void b06_hostChange_stripped() throws Exception {
		assertStripped("http://a_b:8080/x", "http://a_c:8080/y");
	}

	@Test
	void b07_portChange_stripped() throws Exception {
		assertStripped("http://a_b:8080/x", "http://a_b:8081/y");
	}

	@Test
	void b08_crossOrigin_valuesOkHttpChangedForTheNewHop_forwarded() throws Exception {
		// A target's CookieJar cookie or an Authenticator's credentials replace the caller's value on the hop.
		var sent = intercept("http://a_b:8080/x", "http://a_c:8080/y", "Basic fresh", "jar=c", null);
		assertEquals("Basic fresh", sent.header("Authorization"));
		assertEquals("jar=c", sent.header("Cookie"));
	}

	@Test
	void b09_crossOrigin_headerTheCallerNeverSet_forwarded() throws Exception {
		// For example Proxy-Authorization from the client's proxyAuthenticator.
		var sent = intercept("http://a_b:8080/x", "http://a_c:8080/y", "Bearer secret-token", "session=abc123", "Basic proxy");
		assertNull(sent.header("Authorization"));
		assertNull(sent.header("Cookie"));
		assertEquals("Basic proxy", sent.header("Proxy-Authorization"));
	}

	@Test
	void b10_crossOrigin_multiValuedHeader_onlyTheCallersValueStripped() throws Exception {
		var hop = new Request.Builder().url("http://a_c:8080/y")
			.addHeader("Authorization", "Bearer secret-token")
			.addHeader("Authorization", "Basic fresh")
			.build();
		assertEquals(List.of("Basic fresh"), intercept("http://a_b:8080/x", hop).headers("Authorization"));
	}

	//====================================================================================================
	// c - Proxy-Authorization belongs to the proxy: kept only while the hop is plain HTTP through the same HTTP proxy.
	//====================================================================================================

	private static final String PROXY_AUTH = "Basic cHJveHk6cHc=";

	// A proxy that demands PROXY_AUTH (407 otherwise), redirects origin-a.test to origin-b.test, and logs each request
	// as "uri|Proxy-Authorization".
	private static HttpServer startProxy(List<String> log) throws IOException {
		var proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		proxy.createContext("/", exchange -> {
			var proxyAuth = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
			var uri = exchange.getRequestURI().toString();
			log.add(uri + "|" + proxyAuth);
			if (! PROXY_AUTH.equals(proxyAuth)) {
				exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"proxy\"");
				exchange.sendResponseHeaders(407, -1);
			} else if (uri.startsWith("http://origin-a.test/")) {
				exchange.getResponseHeaders().add("Location", "http://origin-b.test/echo");
				exchange.sendResponseHeaders(302, -1);
			} else {
				var auth = exchange.getRequestHeaders().getFirst("Authorization");
				var body = ("auth=" + (auth != null ? auth : "none")).getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
			exchange.close();
		});
		proxy.start();
		return proxy;
	}

	private static Proxy httpProxy(HttpServer proxy) {
		return new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxy.getAddress().getPort()));
	}

	private static TransportRequest proxiedRequest(String uri) {
		return TransportRequest.builder()
			.method("GET")
			.uri(uri)
			.header("Authorization", "Bearer secret-token")
			.header("Proxy-Authorization", PROXY_AUTH)
			.build();
	}

	@Test
	void c01_crossOriginRedirectThroughAuthenticatingProxy_succeeds_originCredentialsStripped() throws Exception {
		var log = new CopyOnWriteArrayList<String>();
		var proxy = startProxy(log);
		try {
			var client = new OkHttpClient.Builder()
				.proxy(httpProxy(proxy))
				// Like a typical proxyAuthenticator, answer a 407 with the proxy credentials the caller already set.
				.proxyAuthenticator((route, response) -> response.request().newBuilder().header("Proxy-Authorization", PROXY_AUTH).build())
				.build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build();
				var response = transport.execute(proxiedRequest("http://origin-a.test/start"))) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=none", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
			}
			assertEquals(List.of("http://origin-a.test/start|" + PROXY_AUTH, "http://origin-b.test/echo|" + PROXY_AUTH), log);
		} finally {
			proxy.stop(0);
		}
	}

	@Test
	void c02_crossOriginRedirect_differentProxy_proxyAuthorizationStripped() throws Exception {
		var logA = new CopyOnWriteArrayList<String>();
		var logB = new CopyOnWriteArrayList<String>();
		var proxyA = startProxy(logA);
		var proxyB = startProxy(logB);
		try {
			var selector = new ProxySelector() {
				@Override public List<Proxy> select(URI uri) {
					return List.of(httpProxy("origin-a.test".equals(uri.getHost()) ? proxyA : proxyB));
				}
				@Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { /* no-op */ }
			};
			var client = new OkHttpClient.Builder().proxySelector(selector).build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build();
				var response = transport.execute(proxiedRequest("http://origin-a.test/start"))) {
				// Proxy B is a different proxy than the one the caller authenticated to, so it never sees the header.
				assertEquals(407, response.getStatusCode());
			}
			assertEquals(List.of("http://origin-a.test/start|" + PROXY_AUTH), logA);
			assertEquals(List.of("http://origin-b.test/echo|null"), logB);
		} finally {
			proxyA.stop(0);
			proxyB.stop(0);
		}
	}

	@Test
	void c03_crossOriginRedirect_noProxy_proxyAuthorizationStripped() throws Exception {
		var seen = new CopyOnWriteArrayList<String>();
		var origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		var originPort = origin.getAddress().getPort();
		origin.createContext("/start", exchange -> redirect(exchange, "http://localhost:" + originPort + "/echo"));
		origin.createContext("/echo", exchange -> {
			seen.add("" + exchange.getRequestHeaders().getFirst("Proxy-Authorization"));
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		origin.start();
		try {
			var client = new OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build();
				var response = transport.execute(proxiedRequest("http://127.0.0.1:" + originPort + "/start"))) {
				assertEquals(200, response.getStatusCode());
			}
			assertEquals(List.of("null"), seen);
		} finally {
			origin.stop(0);
		}
	}

	@Test
	void c04_crossOrigin_httpProxyPlainHttp_sameProxy_proxyAuthorizationKept() throws Exception {
		var sent = interceptRouted("http://a_b:8080/x", "http://a_c:8080/y", httpProxyConnection(3128), httpProxyConnection(3128));
		assertNull(sent.header("Authorization"));
		assertEquals(PROXY_AUTH, sent.header("Proxy-Authorization"));
	}

	@Test
	void c05_crossOrigin_httpsTargetThroughHttpProxy_proxyAuthorizationStripped() throws Exception {
		// An https target is reached through a CONNECT tunnel, so the caller's header would reach the origin.
		var sent = interceptRouted("http://a_b:8080/x", "https://a_c:8443/y", httpProxyConnection(3128), httpProxyConnection(3128));
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c06_crossOrigin_proxyAddressChanged_proxyAuthorizationStripped() throws Exception {
		var sent = interceptRouted("http://a_b:8080/x", "http://a_c:8080/y", httpProxyConnection(3128), httpProxyConnection(3129));
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c07_crossOrigin_firstHopDirect_hopThroughProxy_proxyAuthorizationStripped() throws Exception {
		var sent = interceptRouted("http://a_b:8080/x", "http://a_c:8080/y", directConnection(), httpProxyConnection(3128));
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c08_crossOrigin_noConnection_proxyAuthorizationStripped() throws Exception {
		var sent = interceptRouted("http://a_b:8080/x", "http://a_c:8080/y", null, null);
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c09_crossOrigin_firstHopHttpsThroughProxy_plainHttpHopSameProxy_proxyAuthorizationStripped() throws Exception {
		// On the https first hop the header travelled inside the CONNECT tunnel's TLS; it must not drop to cleartext.
		var sent = interceptRouted("https://a_b:8443/x", "http://a_c:8080/y", httpProxyConnection(3128), httpProxyConnection(3128));
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c10_crossOrigin_socksProxy_proxyAuthorizationStripped() throws Exception {
		var socks = connection(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved("proxy.test", 1080)));
		var sent = interceptRouted("http://a_b:8080/x", "http://a_c:8080/y", socks, socks);
		assertNull(sent.header("Proxy-Authorization"));
	}

	@Test
	void c11_crossOrigin_noFirstHopTag_matchingProxy_proxyAuthorizationStripped() throws Exception {
		// Without the recorder's tag the first hop's route is unknown, so the guard fails safe.
		var original = new Request.Builder().url("http://a_b:8080/x").header("Authorization", "Bearer secret-token").header("Proxy-Authorization", PROXY_AUTH).build();
		var hop = original.newBuilder().url("http://a_c:8080/y").build();
		var sent = new AtomicReference<Request>();
		new OkHttpRedirectCredentialGuard().intercept(chain(hop, original, httpProxyConnection(3128), sent)).close();
		assertNull(sent.get().header("Proxy-Authorization"));
	}

	private static Connection httpProxyConnection(int port) {
		return connection(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("proxy.test", port)));
	}

	private static Connection directConnection() {
		return connection(Proxy.NO_PROXY);
	}

	private static Connection connection(Proxy proxy) {
		var client = new OkHttpClient();
		var address = new okhttp3.Address("a_b", 8080, client.dns(), client.socketFactory(), null, null, null, client.proxyAuthenticator(), proxy, client.protocols(), client.connectionSpecs(), client.proxySelector());
		var route = new okhttp3.Route(address, proxy, InetSocketAddress.createUnresolved("proxy.test", 1));
		return proxy(Connection.class, (m, args) -> "route".equals(m.getName()) ? route : unsupported(m));
	}

	// Runs the guard for the first hop (connection firstConn), then for a redirect hop to "to" (connection hopConn) that
	// carries the caller's Authorization and Proxy-Authorization, and returns the request the second pass sent on.
	private static Request interceptRouted(String from, String to, Connection firstConn, Connection hopConn) throws Exception {
		var original = new Request.Builder().url(from).header("Authorization", "Bearer secret-token").header("Proxy-Authorization", PROXY_AUTH).build();
		var tagged = new AtomicReference<Request>();
		OkHttpRedirectCredentialGuard.FIRST_HOP_RECORDER.intercept(chain(original, null, tagged));
		var first = tagged.get();
		new OkHttpRedirectCredentialGuard().intercept(chain(first, firstConn, new AtomicReference<>())).close();
		var hop = first.newBuilder().url(to).build();
		var sent = new AtomicReference<Request>();
		new OkHttpRedirectCredentialGuard().intercept(chain(hop, hopConn, sent)).close();
		return sent.get();
	}

	private static Interceptor.Chain chain(Request request, Connection connection, AtomicReference<Request> sent) {
		return chain(request, request, connection, sent);
	}

	// callRequest is what Call.request() reports (the original call), which differs from the current hop's request.
	private static Interceptor.Chain chain(Request request, Request callRequest, Connection connection, AtomicReference<Request> sent) {
		return proxy(Interceptor.Chain.class, (m, args) -> switch (m.getName()) {
			case "request" -> request;
			case "connection" -> connection;
			case "call" -> proxy(Call.class, (cm, cargs) -> "request".equals(cm.getName()) ? callRequest : unsupported(cm));
			case "proceed" -> {
				var r = (Request)args[0];
				sent.set(r);
				yield new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").build();
			}
			default -> unsupported(m);
		});
	}

	private static void assertForwarded(String from, String to) throws Exception {
		var sent = intercept(from, to);
		assertEquals("Bearer secret-token", sent.header("Authorization"));
		assertEquals("session=abc123", sent.header("Cookie"));
		assertEquals("v", sent.header("X-Other"));
	}

	private static void assertStripped(String from, String to) throws Exception {
		var sent = intercept(from, to);
		assertNull(sent.header("Authorization"));
		assertNull(sent.header("Cookie"));
		assertEquals("v", sent.header("X-Other"));
	}

	// Runs the guard for one hop whose original call targeted "from" and whose current request targets "to", and
	// returns the request the guard passed on down the chain.
	private static Request intercept(String from, String to) throws Exception {
		return intercept(from, to, "Bearer secret-token", "session=abc123", null);
	}

	// As above, but the hop carries the given header values; the original call always carried the caller's
	// Authorization "Bearer secret-token" and Cookie "session=abc123".
	private static Request intercept(String from, String to, String auth, String cookie, String proxyAuth) throws Exception {
		var hb = new Request.Builder().url(to).header("Authorization", auth).header("Cookie", cookie).header("X-Other", "v");
		if (proxyAuth != null)
			hb.header("Proxy-Authorization", proxyAuth);
		return intercept(from, hb.build());
	}

	// Runs the guard for the given hop of a call whose original request targeted "from" and carried the caller's
	// Authorization "Bearer secret-token" and Cookie "session=abc123".
	private static Request intercept(String from, Request hop) throws Exception {
		var original = new Request.Builder().url(from)
			.header("Authorization", "Bearer secret-token")
			.header("Cookie", "session=abc123")
			.build();
		var sent = new AtomicReference<Request>();
		var call = proxy(Call.class, (m, args) -> "request".equals(m.getName()) ? original : unsupported(m));
		var chain = proxy(Interceptor.Chain.class, (m, args) -> switch (m.getName()) {
			case "request" -> hop;
			case "call" -> call;
			case "connection" -> null;
			case "proceed" -> {
				var r = (Request)args[0];
				sent.set(r);
				yield new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").build();
			}
			default -> unsupported(m);
		});
		new OkHttpRedirectCredentialGuard().intercept(chain).close();
		return sent.get();
	}

	@FunctionalInterface
	private interface Handler {
		Object handle(Method m, Object[] args);
	}

	private static <T> T proxy(Class<T> type, Handler h) {
		return type.cast(java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, args) -> h.handle(m, args)));
	}

	private static Object unsupported(Method m) {
		throw new UnsupportedOperationException(m.getName());
	}
}
