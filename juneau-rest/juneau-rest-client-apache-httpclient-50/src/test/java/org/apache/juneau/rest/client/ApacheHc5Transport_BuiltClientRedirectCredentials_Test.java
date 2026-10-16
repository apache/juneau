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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.hc.client5.http.*;
import org.apache.hc.client5.http.protocol.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.rest.client.apachehttpclient50.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that an {@link ApacheHc5Transport} that builds its own HttpClient follows a redirect to another origin with
 * the caller-set credential headers removed, as the other transports do, while a same-origin redirect keeps them and
 * HttpClient's own redirect handling (method rewrite, redirect cap, circular-redirect check) is unchanged.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc5Transport_BuiltClientRedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static int portA;
	private static int portB;
	private static final AtomicInteger hitsB = new AtomicInteger();
	private static final CountDownLatch sseRelease = new CountDownLatch(1);
	private static final List<String> seenByB = new CopyOnWriteArrayList<>();
	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

	@BeforeAll
	static void startServers() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback; the two servers differ only by port, so they are different origins.
		serverB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", exchange -> {
			hitsB.incrementAndGet();
			echoCreds(exchange);
		});
		serverB.createContext("/back", exchange -> {
			hitsB.incrementAndGet();
			redirect(exchange, 302, "http://127.0.0.1:" + portA + "/echo-creds");
		});
		serverB.createContext("/ping", exchange -> {
			hitsB.incrementAndGet();
			redirect(exchange, 302, "http://127.0.0.1:" + portA + "/redirect-circular");
		});
		// Answers 503 with Retry-After: 0 to the first request of each test, so the client's retry strategy resends it.
		serverB.createContext("/flaky", exchange -> {
			seenByB.add(creds(exchange));
			if (hitsB.incrementAndGet() > 1) {
				echoCreds(exchange);
				return;
			}
			exchange.getResponseHeaders().add("Retry-After", "0");
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", ApacheHc5Transport_BuiltClientRedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-cross-307", exchange -> redirect(exchange, 307, "http://127.0.0.1:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, 302, "/echo-creds"));
		// Same server and port, different host name, so a different origin.
		serverA.createContext("/redirect-host", exchange -> redirect(exchange, 302, "http://localhost:" + portA + "/echo-creds"));
		serverA.createContext("/redirect-cross-back", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/back"));
		serverA.createContext("/redirect-flaky", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/flaky"));
		serverA.createContext("/redirect-circular", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/ping"));
		// Redirects /loop/<n> to /loop/<n+1> forever, so only the redirect cap ends the chain.
		serverA.createContext("/loop/", exchange -> {
			var n = Integer.parseInt(exchange.getRequestURI().getPath().substring("/loop/".length()));
			redirect(exchange, 302, "/loop/" + (n + 1));
		});
		serverA.createContext("/to-sse", exchange -> redirect(exchange, 302, "/sse"));
		// Open-ended SSE stream: sends one event, then holds the connection open until the test class finishes.
		serverA.createContext("/sse", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
			exchange.sendResponseHeaders(200, 0);
			exchange.getResponseBody().write("data: foo\n\n".getBytes(StandardCharsets.UTF_8));
			exchange.getResponseBody().flush();
			try {
				sseRelease.await(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				exchange.close();
			}
		});
		// The SSE handler holds its thread, so the other handlers need threads of their own.
		serverA.setExecutor(EXECUTOR);
		serverA.start();
	}

	@AfterAll
	static void stopServers() {
		sseRelease.countDown();
		if (serverA != null)
			serverA.stop(0);
		if (serverB != null)
			serverB.stop(0);
		EXECUTOR.shutdownNow();
	}

	@BeforeEach
	void resetHits() {
		hitsB.set(0);
		seenByB.clear();
	}

	private static String all(HttpExchange exchange, String name) {
		var values = exchange.getRequestHeaders().get(name);
		return values == null || values.isEmpty() ? "none" : String.join(",", values);
	}

	private static String creds(HttpExchange exchange) {
		return "auth=" + all(exchange, "Authorization") + "|cookie=" + all(exchange, "Cookie")
			+ "|key=" + all(exchange, "X-API-Key") + "|trace=" + all(exchange, "X-Trace") + "|proxy=" + all(exchange, "Proxy-Authorization");
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
		var body = (exchange.getRequestMethod() + " auth=" + all(exchange, "Authorization") + "|cookie=" + all(exchange, "Cookie")
			+ "|key=" + all(exchange, "X-API-Key") + "|trace=" + all(exchange, "X-Trace") + "|proxy=" + all(exchange, "Proxy-Authorization") + (requestBody.isEmpty() ? "" : "|body=" + requestBody))
			.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, int status, String location) throws IOException {
		exchange.getRequestBody().readAllBytes();
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(status, -1);
		exchange.close();
	}

	// Returns the status code, followed by the body when the status is 200.
	private static String send(String method, String path, String body, Duration timeout, String... nameValuePairs) throws Exception {
		try (var client = RestClient.builder().transport(ApacheHc5Transport.create()).rootUrl("http://127.0.0.1:" + portA).build()) {
			var request = client.request(method, path);
			if (body != null)
				request.body(StringBody.of(body, "text/plain"));
			if (timeout != null)
				request.timeout(timeout);
			for (var i = 0; i < nameValuePairs.length; i += 2)
				request.header(nameValuePairs[i], nameValuePairs[i + 1]);
			try (var response = request.run()) {
				return response.getStatusCode() + (response.getStatusCode() == 200 ? " " + response.getBodyAsString() : "");
			}
		}
	}

	private static String get(String path, String... nameValuePairs) throws Exception {
		return send("GET", path, null, null, nameValuePairs);
	}

	private static final String[] CREDENTIALS = { "Authorization", "Bearer secret-token", "Cookie", "session=abc123", "X-API-Key", "k-1", "X-Trace", "t-1", "Proxy-Authorization", "Basic cHg6cHc=" };
	private static final String STRIPPED = "200 GET auth=none|cookie=none|key=none|trace=t-1|proxy=none";
	private static final String KEPT = "200 GET auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1|proxy=Basic cHg6cHc=";

	//-----------------------------------------------------------------------------------------------------------------
	// Cross-origin redirects are followed without the caller's credentials; same-origin ones keep them
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_crossOrigin_followedWithoutCredentials() throws Exception {
		assertEquals(STRIPPED, get("/redirect-cross", CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	@Test
	void a02_sameOrigin_keepsCredentials() throws Exception {
		assertEquals(KEPT, get("/redirect-same", CREDENTIALS));
		assertEquals(0, hitsB.get());
	}

	@Test
	void a03_crossOriginThenBack_credentialsStayStripped() throws Exception {
		// The redirect executor copies each hop from the unstripped one before it; the strip must stick once made.
		assertEquals(STRIPPED, get("/redirect-cross-back", CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	@Test
	void a04_hostChange_samePort_followedWithoutCredentials() throws Exception {
		assertEquals(STRIPPED, get("/redirect-host", CREDENTIALS));
	}

	@Test
	void a05_crossOrigin_apiKeyAlone_followedWithoutIt() throws Exception {
		// Neither Authorization nor Cookie is present; the transport's sensitive flag on X-API-Key no longer stops the redirect.
		assertEquals(STRIPPED, get("/redirect-cross", "X-API-Key", "k-1", "X-Trace", "t-1"));
		assertEquals(1, hitsB.get());
	}

	@Test
	void a06_crossOrigin_withTimeout_followedWithoutCredentials() throws Exception {
		// A per-request timeout puts a RequestConfig in the context; the context still guards the hops.
		assertEquals(STRIPPED, send("GET", "/redirect-cross", null, Duration.ofSeconds(30), CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HttpClient's own redirect handling is unchanged
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_post302_crossOrigin_followedAsGetWithoutBodyOrCredentials() throws Exception {
		assertEquals("200 GET auth=none|cookie=none|key=none|trace=none|proxy=none", send("POST", "/redirect-cross", "payload", null, CREDENTIALS));
	}

	@Test
	void b02_post307_crossOrigin_keepsMethodAndBody_withoutCredentials() throws Exception {
		assertEquals("200 POST auth=none|cookie=none|key=none|trace=t-1|proxy=none|body=payload", send("POST", "/redirect-cross-307", "payload", null, CREDENTIALS));
	}

	@Test
	void b03_redirectCap_stillEnforced() {
		var e = assertThrows(TransportException.class, () -> get("/loop/0", CREDENTIALS));
		// HttpClient reports the protocol failure as a ClientProtocolException wrapping it.
		assertInstanceOf(RedirectException.class, e.getCause().getCause(), e::toString);
		assertEquals("Maximum redirects (50) exceeded", e.getCause().getCause().getMessage());
	}

	@Test
	void b04_circularCrossOriginRedirect_stillRefused() {
		var e = assertThrows(TransportException.class, () -> get("/redirect-circular", CREDENTIALS));
		assertInstanceOf(CircularRedirectException.class, e.getCause().getCause(), e::toString);
		assertEquals(1, hitsB.get());
	}

	@Test
	void b05_redirectToSse_closeReturnsPromptly() throws Exception {
		try (var transport = ApacheHc5Transport.create()) {
			var response = transport.execute(TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + portA + "/to-sse").build());
			assertEquals(200, response.getStatusCode());
			var reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
			assertEquals("data: foo", reader.readLine());
			var start = System.nanoTime();
			response.close();
			assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5, "close() must not drain the open SSE stream");
		}
	}

	@Test
	void b06_crossOrigin_retriedAfter503_retryStaysStripped() throws Exception {
		// The other origin answers 503 with Retry-After: 0 once; the client's retry strategy resends the hop as a fresh copy
		// of the unstripped redirect request, and that resend must be stripped too.  (The built client has no
		// CredentialsProvider, so the auth-challenge resend is not reachable here; the caller-client tests cover it.)
		var stripped = "auth=none|cookie=none|key=none|trace=t-1|proxy=none";
		assertEquals("200 GET " + stripped, get("/redirect-flaky", CREDENTIALS));
		assertEquals(List.of(stripped, stripped), seenByB);
	}
}
