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
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

import okhttp3.Call;
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
