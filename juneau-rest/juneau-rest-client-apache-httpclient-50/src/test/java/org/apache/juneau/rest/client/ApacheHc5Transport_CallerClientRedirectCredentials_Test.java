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

import org.apache.hc.client5.http.impl.auth.*;
import org.apache.hc.client5.http.impl.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.juneau.rest.client.apachehttpclient50.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that an {@link ApacheHc5Transport} built with a caller-supplied {@link CloseableHttpClient} never sends
 * caller-set credential headers to a different origin on a redirect, while the client's own redirect settings decide
 * which redirects are followed.
 *
 * <p>
 * With {@code DefaultRedirectStrategy} a cross-origin redirect that would replay a credential header is not followed at
 * all.  With {@code LaxRedirectStrategy}, which follows every redirect, the transport's per-request context removes the
 * credential headers from the cross-origin hop.
 */
@SuppressWarnings({
	"java:S3415", // Expected value is already first; Sonar mistakes the upper-case constant or field for the expected value
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc5Transport_CallerClientRedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static HttpServer proxy;
	private static int portA;
	private static int portB;
	private static int proxyPort;
	private static final List<String> seenByProxy = new CopyOnWriteArrayList<>();
	private static final AtomicInteger hitsB = new AtomicInteger();
	private static final List<String> seenByB = new CopyOnWriteArrayList<>();
	private static final CountDownLatch sseRelease = new CountDownLatch(1);
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
			redirect(exchange, "http://127.0.0.1:" + portA + "/echo-creds");
		});
		// Challenges the first request of each test, so a client with credentials for this origin resends it; echoes the resend.
		serverB.createContext("/challenge", exchange -> {
			seenByB.add(creds(exchange));
			if (hitsB.incrementAndGet() > 1) {
				echoCreds(exchange);
				return;
			}
			exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"b\"");
			exchange.sendResponseHeaders(401, -1);
			exchange.close();
		});
		// Answers 503 with Retry-After: 0 to the first request of each test, so the client's retry strategy resends it.
		// The 503 also closes its connection so the resend always opens a fresh one: HttpClient spends its single retry on the 503,
		// so a resend that found a stale pooled keep-alive connection would fail with NoHttpResponseException.
		serverB.createContext("/flaky", exchange -> {
			seenByB.add(creds(exchange));
			if (hitsB.incrementAndGet() > 1) {
				echoCreds(exchange);
				return;
			}
			exchange.getResponseHeaders().add("Retry-After", "0");
			exchange.getResponseHeaders().add("Connection", "close");
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		serverB.start();

		// A plain HTTP forwarding proxy that requires Proxy-Authorization (user "u", password "p").
		proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		proxyPort = proxy.getAddress().getPort();
		proxy.createContext("/", ApacheHc5Transport_CallerClientRedirectCredentials_Test::proxy);
		proxy.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", ApacheHc5Transport_CallerClientRedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, "/echo-creds"));
		// Same server and port, different host name, so a different origin.
		serverA.createContext("/redirect-host", exchange -> redirect(exchange, "http://localhost:" + portA + "/echo-creds"));
		serverA.createContext("/redirect-cross-back", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/back"));
		serverA.createContext("/redirect-flaky", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/flaky"));
		serverA.createContext("/redirect-challenge", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/challenge"));
		serverA.createContext("/to-sse", exchange -> redirect(exchange, "/sse"));
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
		if (proxy != null)
			proxy.stop(0);
		EXECUTOR.shutdownNow();
	}

	@BeforeEach
	void resetHits() {
		hitsB.set(0);
		seenByB.clear();
		seenByProxy.clear();
	}

	private static String all(HttpExchange exchange, String name) {
		var values = exchange.getRequestHeaders().get(name);
		return values == null || values.isEmpty() ? "none" : String.join(",", values);
	}

	private static String creds(HttpExchange exchange) {
		return "auth=" + all(exchange, "Authorization") + "|cookie=" + all(exchange, "Cookie")
			+ "|key=" + all(exchange, "X-API-Key") + "|trace=" + all(exchange, "X-Trace");
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var body = creds(exchange).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	// Records each request it receives as "<target port> <Proxy-Authorization>", answers 407 without the right
	// Proxy-Authorization, and otherwise forwards the request (without Proxy-Authorization) to the absolute target URI.
	private static void proxy(HttpExchange exchange) throws IOException {
		var target = exchange.getRequestURI();
		var proxyAuth = all(exchange, "Proxy-Authorization");
		seenByProxy.add(target.getPort() + " " + proxyAuth);
		if (! proxyAuth.equals("Basic dTpw")) {
			exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"proxy\"");
			exchange.sendResponseHeaders(407, -1);
			exchange.close();
			return;
		}
		var connection = (HttpURLConnection)target.toURL().openConnection(Proxy.NO_PROXY);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod(exchange.getRequestMethod());
		for (var name : List.of("Authorization", "Cookie", "X-API-Key", "X-Trace")) {
			var values = exchange.getRequestHeaders().get(name);
			if (values != null)
				for (var value : values)
					connection.addRequestProperty(name, value);
		}
		var code = connection.getResponseCode();
		for (var name : List.of("Location", "Content-Type")) {
			var value = connection.getHeaderField(name);
			if (value != null)
				exchange.getResponseHeaders().add(name, value);
		}
		var in = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
		var body = in == null ? new byte[0] : in.readAllBytes();
		exchange.sendResponseHeaders(code, body.length == 0 ? -1 : body.length);
		if (body.length > 0)
			exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	// Returns the status code, followed by the body when the status is 200.
	private static String get(String path, String... nameValuePairs) throws Exception {
		return get(HttpClients.createDefault(), path, nameValuePairs);
	}

	private static String get(CloseableHttpClient httpClient, String path, String... nameValuePairs) throws Exception {
		return get(httpClient, null, path, nameValuePairs);
	}

	private static String get(CloseableHttpClient httpClient, Duration timeout, String path, String... nameValuePairs) throws Exception {
		var transport = ApacheHc5Transport.builder().httpClient(httpClient).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			var request = client.get(path);
			if (timeout != null)
				request.timeout(timeout);
			for (var i = 0; i < nameValuePairs.length; i += 2)
				request.header(nameValuePairs[i], nameValuePairs[i + 1]);
			try (var response = request.run()) {
				return response.getStatusCode() + (response.getStatusCode() == 200 ? " " + response.getBodyAsString() : "");
			}
		}
	}

	private static CloseableHttpClient lax() {
		return HttpClients.custom().setRedirectStrategy(LaxRedirectStrategy.INSTANCE).build();
	}

	private static final String[] CREDENTIALS = { "Authorization", "Bearer secret-token", "Cookie", "session=abc123", "X-API-Key", "k-1", "X-Trace", "t-1" };
	private static final String STRIPPED = "200 auth=none|cookie=none|key=none|trace=t-1";
	private static final String KEPT = "200 auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1";

	//-----------------------------------------------------------------------------------------------------------------
	// DefaultRedirectStrategy: a cross-origin redirect that would replay a credential header is not followed
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_crossOrigin_callerClient_notFollowedSoNoCredentialReachesTheOtherOrigin() throws Exception {
		assertEquals("302", get("/redirect-cross", CREDENTIALS));
		assertEquals(0, hitsB.get());
	}

	@Test
	void a02_sameOrigin_callerClient_forwardsCredentials() throws Exception {
		assertEquals(KEPT, get("/redirect-same", CREDENTIALS));
	}

	@Test
	void a03_crossOrigin_callerClient_apiKeyAlone_notFollowed() throws Exception {
		// Neither Authorization nor Cookie is present, so only the transport's sensitive flag on X-API-Key stops the redirect.
		assertEquals("302", get("/redirect-cross", "X-API-Key", "k-1", "X-Trace", "t-1"));
		assertEquals(0, hitsB.get());
	}

	@Test
	void a04_crossOrigin_callerClient_noCredentials_followed() throws Exception {
		assertEquals(STRIPPED, get("/redirect-cross", "X-Trace", "t-1"));
		assertEquals(1, hitsB.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LaxRedirectStrategy: every redirect is followed, and the transport's context strips the credentials
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_crossOrigin_laxRedirectStrategy_followedWithoutCredentials() throws Exception {
		// Also pins HttpClient's internal call order that the transport's guarding context relies on (the main exec stage
		// records each hop in the context before sending it); if a future HttpClient changes it, this fails.
		assertEquals(STRIPPED, get(lax(), "/redirect-cross", CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	@Test
	void b02_sameOrigin_laxRedirectStrategy_forwardsCredentials() throws Exception {
		// The other half of the call-order pin: the guard leaves a same-origin hop alone.
		assertEquals(KEPT, get(lax(), "/redirect-same", CREDENTIALS));
	}

	@Test
	void b03_hostChange_samePort_laxRedirectStrategy_followedWithoutCredentials() throws Exception {
		assertEquals(STRIPPED, get(lax(), "/redirect-host", CREDENTIALS));
	}

	@Test
	void b04_crossOriginThenBack_laxRedirectStrategy_credentialsStayStripped() throws Exception {
		// The redirect executor copies each hop from the unstripped one before it; the strip must stick once made.
		assertEquals(STRIPPED, get(lax(), "/redirect-cross-back", CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	@Test
	void b05_crossOrigin_withTimeout_laxRedirectStrategy_followedWithoutCredentials() throws Exception {
		// A per-request timeout puts a RequestConfig in the context; the context still guards the hops.
		assertEquals(STRIPPED, get(lax(), Duration.ofSeconds(30), "/redirect-cross", CREDENTIALS));
		assertEquals(1, hitsB.get());
	}

	@Test
	void b06_crossOrigin_authChallengeResend_laxRedirectStrategy_credentialsStayStripped() throws Exception {
		// The other origin challenges, and the client holds credentials for it, so HttpClient resends the hop with its
		// headers reset to the unstripped copy; the resend must be stripped too.  Auth caching is off because HttpClient 5.6's
		// auth cache fails (NullPointerException) on this exchange, where the caller's own Authorization kept HttpClient from
		// choosing an auth scheme; that happens with or without the transport's context.
		var client = HttpClients.custom()
			.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
			.disableAuthCaching()
			.setDefaultCredentialsProvider(CredentialsProviderBuilder.create().add(new HttpHost("http", "127.0.0.1", portB), "u", "p".toCharArray()).build())
			.build();
		assertEquals(STRIPPED, get(client, "/redirect-challenge", CREDENTIALS));
		assertEquals(List.of("auth=none|cookie=none|key=none|trace=t-1", "auth=none|cookie=none|key=none|trace=t-1"), seenByB);
	}

	@Test
	void b07_crossOrigin_throughAuthenticatingProxy_laxRedirectStrategy_keepsProxyAuthorization() throws Exception {
		// On a plain (not tunnelled) proxied route, Proxy-Authorization authenticates to the proxy, which every hop goes
		// through, so the context keeps the one the client's CredentialsProvider adds; the other credentials are still
		// removed from the cross-origin hop.
		var client = HttpClients.custom()
			.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
			.setProxy(new HttpHost("http", "127.0.0.1", proxyPort))
			.setDefaultCredentialsProvider(CredentialsProviderBuilder.create().add(new HttpHost("http", "127.0.0.1", proxyPort), "u", "p".toCharArray()).build())
			.build();
		assertEquals(STRIPPED, get(client, "/redirect-cross", CREDENTIALS));
		assertEquals(1, hitsB.get());
		assertEquals(portB + " Basic dTpw", seenByProxy.get(seenByProxy.size() - 1));
	}

	@Test
	void b08_crossOrigin_retriedAfter503_laxRedirectStrategy_retryStaysStripped() throws Exception {
		// The other origin answers 503 with Retry-After: 0 once; the client's retry strategy resends the hop as a fresh copy
		// of the unstripped redirect request, and that resend must be stripped too.
		assertEquals(STRIPPED, get(lax(), "/redirect-flaky", CREDENTIALS));
		assertEquals(List.of("auth=none|cookie=none|key=none|trace=t-1", "auth=none|cookie=none|key=none|trace=t-1"), seenByB);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The client's own redirect settings decide which redirects are followed
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_disableRedirectHandling_redirectNotFollowed() throws Exception {
		assertEquals("302", get(HttpClients.custom().disableRedirectHandling().build(), "/redirect-same", CREDENTIALS));
		assertEquals("302", get(HttpClients.custom().disableRedirectHandling().build(), "/redirect-cross", "X-Trace", "t-1"));
		assertEquals(0, hitsB.get());
	}

	@Test
	void c02_minimalClient_redirectNotFollowed() throws Exception {
		assertEquals("302", get(HttpClients.createMinimal(), "/redirect-same", CREDENTIALS));
		assertEquals("302", get(HttpClients.createMinimal(), "/redirect-cross", "X-Trace", "t-1"));
		assertEquals(0, hitsB.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// The final response keeps the transport's SSE handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_redirectToSse_closeReturnsPromptly() throws Exception {
		try (var transport = ApacheHc5Transport.builder().httpClient(lax()).build()) {
			var response = transport.execute(TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + portA + "/to-sse").build());
			assertEquals(200, response.getStatusCode());
			var reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
			assertEquals("data: foo", reader.readLine());
			var start = System.nanoTime();
			response.close();
			assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5, "close() must not drain the open SSE stream");
		}
	}
}
