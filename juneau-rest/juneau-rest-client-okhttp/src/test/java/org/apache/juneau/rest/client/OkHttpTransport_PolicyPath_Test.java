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

import javax.net.*;

import org.apache.juneau.rest.client.okhttp.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Exercises the successful SSRF-guard (policy-covered {@code @Remote}) path of {@link OkHttpTransport}, and the
 * response-body wiring failure path.
 *
 * <p>
 * A guarded request may only connect to a public address, which a hermetic test cannot reach.  The tests therefore
 * target public IP literals (OkHttp connects to those without a DNS lookup) and supply an {@link OkHttpClient} whose
 * {@link SocketFactory} redirects every connect to a loopback server, recording the address the transport asked for.
 */
@SuppressWarnings({
	"java:S3415", // Expected value is already first; Sonar mistakes the upper-case constant or field for the expected value
	"resource" // Transport/response Closeables are exercised inline; lifecycle is managed by the test.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class OkHttpTransport_PolicyPath_Test {

	private static HttpServer server;
	private static int port;
	private static final List<String> hostHeaders = new CopyOnWriteArrayList<>();

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/hello", exchange -> {
			hostHeaders.add(exchange.getRequestHeaders().getFirst("Host"));
			var body = "hi".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.createContext("/to-public", exchange -> {
			hostHeaders.add(exchange.getRequestHeaders().getFirst("Host"));
			exchange.getResponseHeaders().add("Location", "http://8.8.4.4:" + port + "/hello");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/to-loopback", exchange -> {
			hostHeaders.add(exchange.getRequestHeaders().getFirst("Host"));
			exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + port + "/hello");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	@BeforeEach
	void clearHeaders() {
		hostHeaders.clear();
	}

	// =================================================================================================================
	// A: guarded requests that pass the policy are sent through the pinned client
	// =================================================================================================================

	@Test
	void a01_supportsUrlPolicy() {
		try (var transport = OkHttpTransport.create()) {
			assertTrue(transport.supportsUrlPolicy());
		}
	}

	@Test
	void a02_guarded_publicIpLiteral_sent() throws Exception {
		var connects = new CopyOnWriteArrayList<String>();
		try (var transport = transport(connects)) {
			try (var response = transport.execute(guarded("http://8.8.8.8:" + port + "/hello"))) {
				assertEquals(200, response.getStatusCode());
				assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		assertEquals(List.of("8.8.8.8:" + port), connects);
		assertEquals(List.of("8.8.8.8:" + port), hostHeaders);
	}

	@Test
	void a03_guarded_redirectToPublic_followedOnPinnedClient() throws Exception {
		var connects = new CopyOnWriteArrayList<String>();
		try (var transport = transport(connects)) {
			try (var response = transport.execute(guarded("http://8.8.8.8:" + port + "/to-public"))) {
				assertEquals(200, response.getStatusCode());
				assertEquals("hi", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		// Each hop is a separate pinned connect (both Host headers observed by the server).
		assertEquals(List.of("8.8.8.8:" + port, "8.8.4.4:" + port), connects);
		assertEquals(List.of("8.8.8.8:" + port, "8.8.4.4:" + port), hostHeaders);
	}

	@Test
	void a04_guarded_redirectToLoopback_refusedBeforeConnect() {
		var connects = new CopyOnWriteArrayList<String>();
		try (var transport = transport(connects)) {
			var ex = assertThrows(TransportException.class, () -> transport.execute(guarded("http://8.8.8.8:" + port + "/to-loopback")).close());
			assertTrue(ex.getMessage().contains("127.0.0.1"), ex.getMessage());
		}
		// Only the first hop connected; the private redirect target never did.
		assertEquals(List.of("8.8.8.8:" + port), connects);
	}

	// =================================================================================================================
	// B: response-body wiring failure
	// =================================================================================================================

	@Test
	void b01_bodyWiringFails_transportException_andResponseClosed() {
		var sourceCalls = new AtomicInteger();
		Interceptor breakBody = chain -> {
			var response = chain.proceed(chain.request());
			var real = response.body();
			return response.newBuilder().body(new ResponseBody() {
				@Override public MediaType contentType() { return real.contentType(); }
				@Override public long contentLength() { return real.contentLength(); }
				@Override public BufferedSource source() {
					// First call is the transport's byteStream(); later calls come from closing the response.
					if (sourceCalls.incrementAndGet() == 1)
						throw new IllegalStateException("boom");
					return real.source();
				}
			}).build();
		};
		var client = new OkHttpClient.Builder().addInterceptor(breakBody).build();
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var request = TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + port + "/hello").build();
			var ex = assertThrows(TransportException.class, () -> transport.execute(request));
			assertEquals("Failed to read response body: boom", ex.getMessage());
			assertInstanceOf(IllegalStateException.class, ex.getCause());
		}
		// The response was closed (its body's source() was requested again) rather than leaking the connection.
		assertEquals(2, sourceCalls.get());
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	private static TransportRequest guarded(String url) {
		return TransportRequest.builder().method("GET").uri(url).remoteUrlPolicy(true, false).build();
	}

	// An OkHttp-backed transport whose sockets all connect to the loopback server, recording the requested address.
	private static OkHttpTransport transport(List<String> connects) {
		var client = new OkHttpClient.Builder()
			.proxy(Proxy.NO_PROXY)
			.socketFactory(new SocketFactory() {
				@Override public Socket createSocket() {
					return new Socket() {
						@Override public void connect(SocketAddress endpoint, int timeout) throws IOException {
							var a = (InetSocketAddress)endpoint;
							connects.add(a.getAddress().getHostAddress() + ":" + a.getPort());
							super.connect(new InetSocketAddress("127.0.0.1", port), timeout);
						}
					};
				}
				@Override public Socket createSocket(String h, int p) { throw new UnsupportedOperationException(); }
				@Override public Socket createSocket(String h, int p, InetAddress la, int lp) { throw new UnsupportedOperationException(); }
				@Override public Socket createSocket(InetAddress h, int p) { throw new UnsupportedOperationException(); }
				@Override public Socket createSocket(InetAddress h, int p, InetAddress la, int lp) { throw new UnsupportedOperationException(); }
			})
			.build();
		return OkHttpTransport.builder().httpClient(client).build();
	}
}
