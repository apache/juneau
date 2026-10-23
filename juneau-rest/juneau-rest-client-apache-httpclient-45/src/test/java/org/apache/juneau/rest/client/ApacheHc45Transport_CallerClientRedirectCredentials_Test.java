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
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Verifies that an {@link ApacheHc45Transport} built with a caller-supplied {@link CloseableHttpClient} does not
 * forward caller-set credential headers to a different origin when that client follows a redirect, while still
 * forwarding them on a same-origin redirect, and without bypassing the client's own cookie store or credentials
 * provider.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ApacheHc45Transport_CallerClientRedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static HttpServer serverC;
	private static int portA;
	private static int portB;
	private static int portC;
	private static final AtomicReference<String> firstHopAuth = new AtomicReference<>();
	private static final AtomicReference<String> midHopAuth = new AtomicReference<>();

	@BeforeAll
	static void startServers() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback; the two servers differ only by port, so they are different origins.
		serverB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", ApacheHc45Transport_CallerClientRedirectCredentials_Test::echoCreds);
		serverB.createContext("/challenge", ApacheHc45Transport_CallerClientRedirectCredentials_Test::challenge);
		serverB.createContext("/bounce-to-a", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portA + "/echo-creds", null));
		serverB.createContext("/bounce-to-a-challenge", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portA + "/challenge", null));
		serverB.createContext("/to-c", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portC + "/to-a", null));
		serverB.createContext("/to-a-then-b", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portA + "/to-b", null));
		serverB.start();

		serverC = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portC = serverC.getAddress().getPort();
		serverC.createContext("/to-a", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portA + "/echo-creds", null));
		serverC.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", ApacheHc45Transport_CallerClientRedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds", null));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, 302, "/echo-creds", null));
		serverA.createContext("/redirect-cross-set-cookie", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds", "store=s-1; Path=/"));
		serverA.createContext("/challenge", ApacheHc45Transport_CallerClientRedirectCredentials_Test::challenge);
		serverA.createContext("/redirect-same-set-cookie", exchange -> redirect(exchange, 302, "/echo-creds", "store=s-1; Path=/"));
		serverA.createContext("/redirect-same-challenge", exchange -> redirect(exchange, 302, "/challenge", null));
		serverA.createContext("/redirect-cross-307", exchange -> redirect(exchange, 307, "http://127.0.0.1:" + portB + "/echo-creds", null));
		serverA.createContext("/redirect-same-307", exchange -> redirect(exchange, 307, "/echo-creds", null));
		serverA.createContext("/redirect-cross-challenge", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/challenge", null));
		serverA.createContext("/redirect-a-b-a", exchange -> {
			firstHopAuth.set(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
			redirect(exchange, 302, "http://127.0.0.1:" + portB + "/bounce-to-a", null);
		});
		serverA.createContext("/redirect-a-b-a-set-cookie", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/bounce-to-a", "store=s-1; Path=/"));
		serverA.createContext("/redirect-a-b-a-challenge", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/bounce-to-a-challenge", null));
		serverA.createContext("/redirect-userinfo", exchange -> redirect(exchange, 302, "http://bob:pw@127.0.0.1:" + portA + "/echo-creds", null));
		serverA.createContext("/redirect-a-b-c-a", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/to-c", null));
		serverA.createContext("/redirect-a-b-a-b", exchange -> redirect(exchange, 302, "http://127.0.0.1:" + portB + "/to-a-then-b", null));
		serverA.createContext("/to-b", exchange -> {
			midHopAuth.set(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
			redirect(exchange, 302, "http://127.0.0.1:" + portB + "/echo-creds", null);
		});
		serverA.start();
	}

	@AfterAll
	static void stopServers() {
		if (serverA != null)
			serverA.stop(0);
		if (serverB != null)
			serverB.stop(0);
		if (serverC != null)
			serverC.stop(0);
	}

	private static String all(HttpExchange exchange, String name) {
		var values = exchange.getRequestHeaders().get(name);
		return values == null || values.isEmpty() ? "none" : String.join(",", values);
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		respond(exchange, 200, "auth=" + all(exchange, "Authorization") + "|cookie=" + all(exchange, "Cookie")
			+ "|key=" + all(exchange, "X-API-Key") + "|trace=" + all(exchange, "X-Trace"));
	}

	// Answers 401 with a Basic challenge until the request carries Authorization, then echoes it.
	private static void challenge(HttpExchange exchange) throws IOException {
		if (exchange.getRequestHeaders().getFirst("Authorization") == null) {
			exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"b\"");
			respond(exchange, 401, "challenge");
			return;
		}
		echoCreds(exchange);
	}

	private static void respond(HttpExchange exchange, int status, String text) throws IOException {
		var body = text.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(status, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, int status, String location, String setCookie) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		if (setCookie != null)
			exchange.getResponseHeaders().add("Set-Cookie", setCookie);
		exchange.sendResponseHeaders(status, -1);
		exchange.close();
	}

	private static String get(CloseableHttpClient callerClient, String path) throws Exception {
		return get(ApacheHc45Transport.builder().httpClient(callerClient).build(), path);
	}

	private static String get(ApacheHc45Transport transport, String path) throws Exception {
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get(path)
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.header("X-API-Key", "k-1")
					.header("X-Trace", "t-1")
					.run()) {
			assertEquals(200, response.getStatusCode());
			return response.getBodyAsString();
		}
	}

	@Test
	void a01_crossOrigin_callerClient_stripsCallerCredentials() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(HttpClients.createDefault(), "/redirect-cross"));
	}

	@Test
	void a02_sameOrigin_callerClient_forwardsCredentials() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(HttpClients.createDefault(), "/redirect-same"));
	}

	@Test
	void a03_crossOrigin_callerClient_cookieStoreCookieStripped() throws Exception {
		// the cookie A sets lives in the caller's cookie store, which would send it on to B (a cookie's domain ignores
		// the port).  The guard strips every credential header on a cross-origin hop, so the store's Cookie goes too.
		var callerClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(callerClient, "/redirect-cross-set-cookie"));
	}

	@Test
	void a04_crossOrigin_callerClient_credentialsProviderAuthorizationStripped() throws Exception {
		// the credentials provider would answer B's challenge.  The answer is an Authorization header, so it is stripped;
		// B keeps challenging and the caller sees the 401.
		var callerClient = HttpClients.custom().setDefaultCredentialsProvider(provider(portB)).build();
		assertEquals("401:challenge", getAnyStatus(callerClient, "/redirect-cross-challenge"));
	}

	@Test
	void a05_crossOrigin307_callerClient_stripsCallerCredentials() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(HttpClients.createDefault(), "/redirect-cross-307"));
	}

	@Test
	void a06_sameOrigin307_callerClient_forwardsCredentials() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(HttpClients.createDefault(), "/redirect-same-307"));
	}

	@Test
	void a07_originBackToFirstOrigin_callerClient_credentialsRestored() throws Exception {
		// the hop that returns to the first origin gets the caller's own headers back.
		firstHopAuth.set(null);
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(HttpClients.createDefault(), "/redirect-a-b-a"));
		assertEquals("Bearer secret-token", firstHopAuth.get());
	}

	private static CredentialsProvider provider(int port) {
		var credentials = new BasicCredentialsProvider();
		credentials.setCredentials(new AuthScope("127.0.0.1", port), new UsernamePasswordCredentials("bob", "pw"));
		return credentials;
	}

	private static String getAnyStatus(CloseableHttpClient callerClient, String path) throws Exception {
		var transport = ApacheHc45Transport.builder().httpClient(callerClient).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get(path).header("Authorization", "Bearer secret-token").run()) {
			return response.getStatusCode() + ":" + response.getBodyAsString();
		}
	}

	private static CloseableHttpClient preemptive() {
		return HttpClients.custom().addInterceptorLast((HttpRequestInterceptor)(request, context) -> request.setHeader("Authorization", "Basic cHJlZW1wdA==")).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Matrix: caller header, cookie store, credentials provider and preemptive auth, each cross-origin (stripped)
	// and same-origin (kept), on both caller-supplied and transport-built clients.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_callerClient_sameOrigin_cookieStoreCookieKept() throws Exception {
		var callerClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();
		assertEquals("auth=Bearer secret-token|cookie=session=abc123,store=s-1|key=k-1|trace=t-1", get(callerClient, "/redirect-same-set-cookie"));
	}

	@Test
	void b02_callerClient_sameOrigin_credentialsProviderAnswersChallenge() throws Exception {
		var callerClient = HttpClients.custom().setDefaultCredentialsProvider(provider(portA)).build();
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(callerClient, "/redirect-same-challenge"));
	}

	@Test
	void b03_callerClient_crossOrigin_preemptiveAuthorizationStripped() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(preemptive(), "/redirect-cross"));
	}

	@Test
	void b04_callerClient_sameOrigin_preemptiveAuthorizationKept() throws Exception {
		// The preemptive interceptor overwrites the caller's header on every request, including the redirect hop.
		assertEquals("auth=Basic cHJlZW1wdA==|cookie=session=abc123|key=k-1|trace=t-1", get(preemptive(), "/redirect-same"));
	}

	@Test
	void b05_callerClient_crossOrigin_credentialsProviderStrippedEvenWithoutCallerHeaders() throws Exception {
		var callerClient = HttpClients.custom().setDefaultCredentialsProvider(provider(portB)).build();
		var transport = ApacheHc45Transport.builder().httpClient(callerClient).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get("/redirect-cross-challenge").run()) {
			assertEquals(401, response.getStatusCode());
		}
	}

	@Test
	void c01_transportBuilt_crossOrigin_stripsHeaderAndCookieStore() throws Exception {
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(ApacheHc45Transport.create(), "/redirect-cross"));
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(ApacheHc45Transport.create(), "/redirect-cross-set-cookie"));
	}

	@Test
	void c02_transportBuilt_sameOrigin_keepsHeadersAndCookieStore() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(ApacheHc45Transport.create(), "/redirect-same"));
		assertEquals("auth=Bearer secret-token|cookie=session=abc123,store=s-1|key=k-1|trace=t-1", get(ApacheHc45Transport.create(), "/redirect-same-set-cookie"));
	}

	@Test
	void d01_callerClient_aToBToA_cookieStoreCookieRestoredOnReturn() throws Exception {
		// The final hop back to A is an ordinary request to A: caller headers and the store cookie are back.
		var callerClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();
		assertEquals("auth=Bearer secret-token|cookie=session=abc123,store=s-1|key=k-1|trace=t-1", get(callerClient, "/redirect-a-b-a-set-cookie"));
	}

	@Test
	void d02_callerClient_aToBToA_credentialsProviderAuthorizationRestoredOnReturn() throws Exception {
		// No caller Authorization: A's challenge on the return hop is answered by the credentials provider again.
		var callerClient = HttpClients.custom().setDefaultCredentialsProvider(provider(portA)).build();
		var transport = ApacheHc45Transport.builder().httpClient(callerClient).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build();
				var response = client.get("/redirect-a-b-a-challenge").run()) {
			assertEquals(200, response.getStatusCode());
			assertEquals("auth=Basic Ym9iOnB3|cookie=none|key=none|trace=none", response.getBodyAsString());
		}
	}

	@Test
	void d03_callerClient_aToBToCToA_restoredOnFinalHop() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(HttpClients.createDefault(), "/redirect-a-b-c-a"));
	}

	@Test
	void d04_callerClient_aToBToAToB_secondBStripped() throws Exception {
		midHopAuth.set(null);
		assertEquals("auth=none|cookie=none|key=none|trace=t-1", get(HttpClients.createDefault(), "/redirect-a-b-a-b"));
		assertEquals("Bearer secret-token", midHopAuth.get(), "the hop back at A carried the credentials");
	}

	@Test
	void d05_transportBuilt_aToBToA_restoredOnReturn() throws Exception {
		assertEquals("auth=Bearer secret-token|cookie=session=abc123|key=k-1|trace=t-1", get(ApacheHc45Transport.create(), "/redirect-a-b-a"));
	}

	@Test
	@SuppressWarnings({
		"deprecation" // The legacy AbstractHttpClient family is exactly what is refused.
	})
	void e01_legacyDefaultHttpClient_isRefused() {
		// Reproduced before the refusal: a legacy client never re-sets the request before the wire, so the store cookie followed a cross-origin redirect.
		var builder = ApacheHc45Transport.builder();
		var legacy = new org.apache.http.impl.client.DefaultHttpClient();
		var ex = assertThrows(IllegalArgumentException.class, () -> builder.httpClient(legacy));
		assertTrue(ex.getMessage().contains("AbstractHttpClient"), ex.getMessage());
	}

	@Test
	void e02_transportBuilt_redirectLocationWithUserInfo_isRefused() throws Exception {
		var transport = ApacheHc45Transport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			var ex = assertThrows(Exception.class, () -> client.get("/redirect-userinfo").run());
			var found = false;
			for (Throwable t = ex; t != null; t = t.getCause())
				found |= String.valueOf(t.getMessage()).contains("user info");
			assertTrue(found, ex.toString());
		}
	}
}
