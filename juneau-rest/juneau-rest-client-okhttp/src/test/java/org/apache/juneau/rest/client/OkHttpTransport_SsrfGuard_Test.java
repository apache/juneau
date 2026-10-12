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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.okhttp.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the SSRF-guard (policy-covered {@code @Remote}) path of {@link OkHttpTransport} against a real
 * loopback server: a guarded request must always be pinned, even when an unguarded request on the same transport has
 * left an idle keep-alive connection to the same URL in the pool.
 *
 * <p>
 * The hostname tests use {@code localhost} rather than {@code 127.0.0.1} because OkHttp connects to an IP-literal host
 * directly, without consulting its {@code Dns}; the transport therefore checks an IP-literal host itself (section C).
 * Likewise a proxy resolves the target host itself, so a guarded request is refused when the call would use a proxy
 * (section D).
 */
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class OkHttpTransport_SsrfGuard_Test {

	private static HttpServer server;
	private static String url;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/hello", exchange -> {
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/to-numeric", exchange -> {
			exchange.getResponseHeaders().add("Location", "http://2130706433:" + server.getAddress().getPort() + "/hello");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.start();
		url = "http://localhost:" + server.getAddress().getPort() + "/hello";
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	// =================================================================================================================
	// A: guarded requests never reuse a connection that was opened without the pin
	// =================================================================================================================

	@Test
	void a01_guarded_freshTransport_rejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a02_unguardedThenGuarded_sameTransport_guardedRejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			// Fully consumed, so the keep-alive connection (opened without the pin) goes back to the pool.
			assertPlainOk(transport);
			// The guarded request must not ride that idle pooled connection; it goes through the pin and is refused.
			assertRejectedLoopback(transport);
		}
	}

	@Test
	void a03_guardedUnguardedGuarded_sameTransport_bothGuardedRejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLoopback(transport);
			assertPlainOk(transport);
			assertRejectedLoopback(transport);
			// The guard does not carry over to ordinary requests either.
			assertPlainOk(transport);
		}
	}

	// =================================================================================================================
	// B: lookup failures on the ordinary path keep their existing shape
	// =================================================================================================================

	@Test
	void b01_unguarded_unknownHost_transportExceptionWithUnknownHostCause() throws Exception {
		var ex = assertThrows(TransportException.class, () -> executeWithDns(host -> { throw new UnknownHostException(host); }));
		assertInstanceOf(UnknownHostException.class, ex.getCause());
	}

	private static void executeWithDns(okhttp3.Dns dns) throws Exception {
		var client = new okhttp3.OkHttpClient.Builder().proxy(Proxy.NO_PROXY).dns(dns).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			transport.execute(TransportRequest.builder().method("GET").uri("http://no-such-host:1/hello").build()).close();
		}
	}

	// =================================================================================================================
	// C: IP-literal hosts are never handed to Dns by OkHttp, so the transport checks them itself
	// =================================================================================================================

	@Test
	void c01_guarded_ipv4LiteralLoopback_rejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLiteral(transport, "http://127.0.0.1:" + server.getAddress().getPort() + "/hello", "127.0.0.1");
		}
	}

	@Test
	void c02_guarded_ipv6LiteralLoopback_rejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLiteral(transport, "http://[::1]:" + server.getAddress().getPort() + "/hello", "::1");
		}
	}

	@Test
	void c03_guarded_ipv4LiteralPrivate_rejectedOnCallerSuppliedClient() throws Exception {
		var client = new okhttp3.OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			assertRejectedLiteral(transport, "http://10.1.2.3:9/hello", "10.1.2.3");
		}
	}

	@Test
	void c04_allowPrivateUrls_ipLiteralLoopback_allowed() throws Exception {
		try (var transport = OkHttpTransport.create();
			var response = transport.execute(TransportRequest.builder().method("GET")
				.uri("http://127.0.0.1:" + server.getAddress().getPort() + "/hello").remoteUrlPolicy(true, true).build())) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test
	void c05_unguarded_ipLiteralLoopback_allowed() throws Exception {
		try (var transport = OkHttpTransport.create();
			var response = transport.execute(TransportRequest.builder().method("GET")
				.uri("http://127.0.0.1:" + server.getAddress().getPort() + "/hello").build())) {
			assertEquals(200, response.getStatusCode());
		}
	}

	@Test
	void c06_guarded_numericHost_rejected() throws Exception {
		// OkHttp treats an all-digit host as an IP literal (2130706433 == 127.0.0.1) and connects without consulting Dns.
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLiteral(transport, "http://2130706433:" + server.getAddress().getPort() + "/hello", "2130706433");
		}
	}

	@Test
	void c07_guarded_shortDottedHost_rejected() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertRejectedLiteral(transport, "http://127.1:" + server.getAddress().getPort() + "/hello", "127.1");
		}
	}

	@Test
	void c08_guarded_redirectToNumericHost_rejected() throws Exception {
		var target = "http://2130706433:" + server.getAddress().getPort() + "/hello";
		// An application interceptor answers hop 0 (a public-looking name) with a redirect, so no network or Dns is needed.
		okhttp3.Interceptor redirector = chain -> {
			var req = chain.request();
			if (! "redirector.test".equals(req.url().host()))
				return chain.proceed(req);
			return new okhttp3.Response.Builder().request(req).protocol(okhttp3.Protocol.HTTP_1_1).code(302).message("Found")
				.header("Location", target).body(okhttp3.ResponseBody.create(new byte[0], null)).build();
		};
		var client = new okhttp3.OkHttpClient.Builder().proxy(Proxy.NO_PROXY).addInterceptor(redirector).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var req = TransportRequest.builder().method("GET").uri("http://redirector.test/start").remoteUrlPolicy(true, false).build();
			var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
			assertTrue(ex.getMessage().contains("No allowed address for host '2130706433'"), ex.getMessage());
		}
	}

	@Test
	void d06_guarded_underscoreHost_selectorNotConsulted() throws Exception {
		// java.net.URI parses an underscore host with a null host; OkHttp skips the ProxySelector for it, so must we.
		var calls = new AtomicInteger();
		var selector = new ProxySelector() {
			@Override public List<Proxy> select(URI uri) {
				calls.incrementAndGet();
				return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1)));
			}
			@Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { /* no-op */ }
		};
		var client = new okhttp3.OkHttpClient.Builder().proxySelector(selector).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var req = TransportRequest.builder().method("GET").uri("http://my_service:8080/hello").remoteUrlPolicy(true, false).build();
			var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
			assertFalse(ex.getMessage().contains("proxy"), ex.getMessage());
			assertEquals(0, calls.get());
		}
	}

	@Test
	void c09_guarded_malformedNumericHost_failsClosed() throws Exception {
		// Matches OkHttp's IP-literal pattern but is not a valid literal: refused outright, never handed to a resolver.
		try (var transport = OkHttpTransport.create()) {
			for (var host : List.of("1.2.3.4.5", "127.0.0.1.", "256.1.1.1", "1.2.3.256", "1.2.65536", "4294967296", "010.0.0.1")) {
				var req = TransportRequest.builder().method("GET").uri("http://" + (host.contains(":") ? "[" + host + "]" : host) + ":9/hello").remoteUrlPolicy(true, false).build();
				var ex = assertThrows(TransportException.class, () -> transport.execute(req).close(), host);
				assertTrue(ex.getMessage().contains("is not a valid IP literal"), host + ": " + ex.getMessage());
				assertInstanceOf(IllegalArgumentException.class, ex.getCause(), host);
			}
		}
	}

	// =================================================================================================================
	// D: a proxy resolves the target host itself, bypassing the Dns pin, so a guarded request must not use one
	// =================================================================================================================

	@Test
	void d01_guarded_socksProxy_targetNeverSentToProxy() throws Exception {
		try (var socks = new FakeSocksProxy()) {
			var client = new okhttp3.OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.SOCKS, socks.address())).build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
				var req = TransportRequest.builder().method("GET").uri("http://target.example.test:80/hello").remoteUrlPolicy(true, false).build();
				var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
				assertTrue(ex.getMessage().contains("proxy"), ex.getMessage());
				assertInstanceOf(IllegalArgumentException.class, ex.getCause());
				assertNull(socks.requestedHost(), "the SOCKS proxy must not be asked to resolve the target");
			}
		}
	}

	@Test
	void d02_guarded_proxySelectorReturnsProxy_rejected() throws Exception {
		var selector = new ProxySelector() {
			@Override public List<Proxy> select(URI uri) {
				return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1)));
			}
			@Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { /* no-op */ }
		};
		var client = new okhttp3.OkHttpClient.Builder().proxySelector(selector).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var req = TransportRequest.builder().method("GET").uri("http://target.example.test:80/hello").remoteUrlPolicy(true, false).build();
			var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
			assertTrue(ex.getMessage().contains("proxy"), ex.getMessage());
		}
	}

	@Test
	void d07_guarded_selectorReturnsNullOrEmpty_treatedAsDirect() throws Exception {
		for (var answer : Arrays.<List<Proxy>>asList(null, List.of())) {
			var selector = new ProxySelector() {
				@Override public List<Proxy> select(URI uri) { return answer; }
				@Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { /* no-op */ }
			};
			var client = new okhttp3.OkHttpClient.Builder().proxySelector(selector).build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
				// DIRECT, so the request proceeds to the pin, which refuses localhost.
				assertRejectedLoopback(transport);
			}
		}
	}

	@Test
	void d03_explicitProxy_supportsUrlPolicyFalse() throws Exception {
		var client = new okhttp3.OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1))).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			assertFalse(transport.supportsUrlPolicy());
		}
	}

	@Test
	void d04_noProxy_supportsUrlPolicyTrue() throws Exception {
		var client = new okhttp3.OkHttpClient.Builder().proxy(Proxy.NO_PROXY).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			assertTrue(transport.supportsUrlPolicy());
		}
	}

	@Test
	void d05_unguarded_socksProxy_stillUsed() throws Exception {
		try (var socks = new FakeSocksProxy()) {
			var client = new okhttp3.OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.SOCKS, socks.address())).build();
			try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
				var req = TransportRequest.builder().method("GET").uri("http://target.example.test:80/hello").build();
				assertThrows(TransportException.class, () -> transport.execute(req).close()); // the fake proxy closes after the handshake
				assertEquals("target.example.test", socks.requestedHost());
			}
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static TransportRequest guarded() {
		return TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
	}

	private static TransportRequest plain() {
		return TransportRequest.builder().method("GET").uri(url).build();
	}

	private static void assertPlainOk(OkHttpTransport transport) throws Exception {
		try (var response = transport.execute(plain())) {
			assertEquals(200, response.getStatusCode());
			assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	private static void assertRejectedLoopback(OkHttpTransport transport) {
		var ex = assertThrows(TransportException.class, () -> transport.execute(guarded()).close());
		assertTrue(ex.getMessage().contains("No allowed address for host 'localhost'"), ex.getMessage());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
	}

	private static void assertRejectedLiteral(OkHttpTransport transport, String uri, String host) {
		var req = TransportRequest.builder().method("GET").uri(uri).remoteUrlPolicy(true, false).build();
		var ex = assertThrows(TransportException.class, () -> transport.execute(req).close());
		assertTrue(ex.getMessage().contains("No allowed address for host '" + host + "'"), ex.getMessage());
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
	}

	/** Minimal SOCKS5 endpoint that records the domain name from the CONNECT request, then closes the connection. */
	private static final class FakeSocksProxy implements Closeable {
		private final ServerSocket ss;
		private final AtomicReference<String> host = new AtomicReference<>();
		private final CountDownLatch done = new CountDownLatch(1);

		FakeSocksProxy() throws IOException {
			ss = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
			var t = new Thread(this::serve, "fake-socks");
			t.setDaemon(true);
			t.start();
		}

		InetSocketAddress address() {
			return new InetSocketAddress(InetAddress.getLoopbackAddress(), ss.getLocalPort());
		}

		String requestedHost() throws InterruptedException {
			done.await(2, TimeUnit.SECONDS);
			return host.get();
		}

		private void serve() {
			try (var s = ss.accept()) {
				s.setSoTimeout(2000);
				var in = new DataInputStream(s.getInputStream());
				var out = s.getOutputStream();
				in.readByte(); // version
				in.readFully(new byte[in.readUnsignedByte()]); // methods
				out.write(new byte[]{5, 0});
				out.flush();
				in.readFully(new byte[3]); // ver, cmd, rsv
				if (in.readUnsignedByte() == 3) { // ATYP domain name
					var b = new byte[in.readUnsignedByte()];
					in.readFully(b);
					host.set(new String(b, StandardCharsets.US_ASCII));
				}
			} catch (IOException e) {
				// Expected when no client connects or the client gives up; nothing was recorded.
			} finally {
				done.countDown();
			}
		}

		@Override
		public void close() throws IOException {
			ss.close();
		}
	}
}
