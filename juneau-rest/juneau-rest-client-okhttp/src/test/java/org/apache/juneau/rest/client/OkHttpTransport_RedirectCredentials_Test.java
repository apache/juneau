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

import org.apache.juneau.rest.client.okhttp.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Verifies that {@link OkHttpTransport} does not forward caller-set credential headers &mdash; including
 * {@code Cookie} and a same-host/different-port {@code Authorization} &mdash; when a request is transparently
 * replayed against a different origin as the result of a redirect, while still forwarding them on a
 * same-origin redirect.
 */
@SuppressWarnings({
	"resource" // Transport/client instances are short-lived test fixtures.
})
// SEPARATE_THREAD: a socket read blocked on a non-responding peer ignores interrupts, so SAME_THREAD would still hang.
@Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class OkHttpTransport_RedirectCredentials_Test {

	private static HttpServer serverA;
	private static HttpServer serverB;
	private static int portA;
	private static int portB;

	@BeforeAll
	static void startServers() throws IOException {
		// Bind to (and connect via) the explicit IPv4 loopback, not the wildcard address + "localhost": a wildcard bind can be
		// handed an ephemeral port another process already holds on 127.0.0.1 (SO_REUSEADDR), which then wins every "localhost" connection.
		serverB = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portB = serverB.getAddress().getPort();
		serverB.createContext("/echo-creds", OkHttpTransport_RedirectCredentials_Test::echoCreds);
		serverB.createContext("/protected", exchange -> {
			if ("Basic fresh".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
				echoCreds(exchange);
				return;
			}
			exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"b\"");
			exchange.sendResponseHeaders(401, -1);
			exchange.close();
		});
		serverB.start();

		serverA = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		portA = serverA.getAddress().getPort();
		serverA.createContext("/echo-creds", OkHttpTransport_RedirectCredentials_Test::echoCreds);
		serverA.createContext("/redirect-cross", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/echo-creds"));
		serverA.createContext("/redirect-same", exchange -> redirect(exchange, "http://127.0.0.1:" + portA + "/echo-creds"));
		serverA.createContext("/redirect-protected", exchange -> redirect(exchange, "http://127.0.0.1:" + portB + "/protected"));
		serverA.start();
		serverA.createContext("/chain-start", exchange -> { logHop("A1", exchange); redirect(exchange, "http://127.0.0.1:" + portB + "/chain-mid"); });
		serverB.createContext("/chain-mid", exchange -> { logHop("B", exchange); redirect(exchange, "http://127.0.0.1:" + portA + "/chain-end"); });
		serverA.createContext("/chain-end", exchange -> { logHop("A2", exchange); echoCreds(exchange); });
		serverB.createContext("/redirect-to-a", exchange -> redirect(exchange, "http://127.0.0.1:" + portA + "/echo-creds"));
	}

	@AfterAll
	static void stopServers() {
		if (serverA != null)
			serverA.stop(0);
		if (serverB != null)
			serverB.stop(0);
	}

	private static final List<String> HOPS = new java.util.concurrent.CopyOnWriteArrayList<>();

	private static void logHop(String name, HttpExchange exchange) {
		var h = exchange.getRequestHeaders();
		HOPS.add(name + ":auth=" + h.getFirst("Authorization") + "|cookie=" + h.getFirst("Cookie") + "|key=" + h.getFirst("X-API-Key")
			+ "|proxy=" + h.getFirst("Proxy-Authorization") + "|trace=" + h.getFirst("X-Trace"));
	}

	private static void echoCreds(HttpExchange exchange) throws IOException {
		var auth = exchange.getRequestHeaders().getFirst("Authorization");
		var cookie = exchange.getRequestHeaders().getFirst("Cookie");
		var body = ("auth=" + (auth != null ? auth : "none") + "|cookie=" + (cookie != null ? cookie : "none"))
			.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/plain");
		exchange.sendResponseHeaders(200, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static void redirect(HttpExchange exchange, String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	@Test
	void a01_crossOrigin_stripsCredentials() throws Exception {
		var transport = OkHttpTransport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-cross")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=none|cookie=none", response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_sameOrigin_forwardsCredentials() throws Exception {
		var transport = OkHttpTransport.create();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-same")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=Bearer secret-token|cookie=session=abc123", response.getBodyAsString());
			}
		}
	}

	@Test
	void a03_callerSuppliedClient_crossOrigin_stripsCredentials() throws Exception {
		// OkHttp itself keeps a caller-set Cookie on a redirect.
		var transport = OkHttpTransport.builder().httpClient(new okhttp3.OkHttpClient()).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-cross")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=none|cookie=none", response.getBodyAsString());
			}
		}
	}

	@Test
	void a04_callerSuppliedClient_sameOrigin_forwardsCredentials() throws Exception {
		var transport = OkHttpTransport.builder().httpClient(new okhttp3.OkHttpClient()).build();
		try (var client = RestClient.builder().transport(transport).rootUrl("http://127.0.0.1:" + portA).build()) {
			try (var response = client.get("/redirect-same")
					.header("Authorization", "Bearer secret-token")
					.header("Cookie", "session=abc123")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("auth=Bearer secret-token|cookie=session=abc123", response.getBodyAsString());
			}
		}
	}

	//====================================================================================================
	// c - Once stripped on a cross-origin hop, credentials stay stripped for the rest of the chain (A -> B -> A).
	//====================================================================================================

	private static final String ALL = "auth=Bearer secret-token|cookie=session=abc123|key=k1|proxy=Basic cHJveHk6cHc=|trace=t1";
	private static final String NONE = "auth=null|cookie=null|key=null|proxy=null|trace=t1";

	private static void assertChain(OkHttpTransport transport) throws Exception {
		HOPS.clear();
		var request = TransportRequest.builder().method("GET").uri("http://127.0.0.1:" + portA + "/chain-start")
			.header("Authorization", "Bearer secret-token").header("Cookie", "session=abc123").header("X-API-Key", "k1")
			.header("Proxy-Authorization", "Basic cHJveHk6cHc=").header("X-Trace", "t1").build();
		try (var response = transport.execute(request)) {
			assertEquals(200, response.getStatusCode());
			assertEquals("auth=none|cookie=none", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
		}
		assertEquals(List.of("A1:" + ALL, "B:" + NONE, "A2:" + NONE), HOPS);
	}

	@Test
	void c01_crossOriginThenBackToFirstOrigin_credentialsStayStripped() throws Exception {
		try (var transport = OkHttpTransport.create()) {
			assertChain(transport);
		}
	}

	@Test
	void c02_callerSuppliedClient_crossOriginThenBackToFirstOrigin_credentialsStayStripped() throws Exception {
		try (var transport = OkHttpTransport.builder().httpClient(new OkHttpClient()).build()) {
			assertChain(transport);
		}
	}

	//====================================================================================================
	// b - Headers OkHttp itself adds for the new origin are not the caller's, so they are not stripped.
	//====================================================================================================

	@Test
	void b01_crossOrigin_targetsOwnCookieJarCookie_forwarded() throws Exception {
		// BridgeInterceptor loads the target's own cookies from the client's CookieJar for each hop.
		var jar = new CookieJar() {
			@Override
			public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {}
			@Override
			public List<Cookie> loadForRequest(HttpUrl url) {
				return url.port() == portB ? List.of(new Cookie.Builder().name("jar").value("b").domain("127.0.0.1").build()) : List.of();
			}
		};
		var client = new OkHttpClient.Builder().cookieJar(jar).build();
		assertEquals("200|auth=none|cookie=jar=b", get(client, "http://127.0.0.1:" + portA + "/redirect-cross"));
	}

	@Test
	void b02_crossOrigin_authenticatorCredentials_forwarded() throws Exception {
		// The target answers 401; the caller's Authenticator supplies credentials for the new origin.
		var client = new OkHttpClient.Builder()
			.authenticator((route, response) -> response.priorResponse() != null && response.priorResponse().code() == 401 ? null
				: response.request().newBuilder().header("Authorization", "Basic fresh").build())
			.build();
		assertEquals("200|auth=Basic fresh|cookie=none", get(client, "http://127.0.0.1:" + portA + "/redirect-protected"));
	}

	@Test
	void b03_applicationInterceptorRewritesUrl_firstHopIsTheOrigin_credentialsKept() throws Exception {
		var client = new OkHttpClient.Builder().addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
			.url(chain.request().url().newBuilder().port(portB).build()).build())).build();
		assertEquals("200|auth=Bearer secret-token|cookie=session=abc123", get(client, "http://127.0.0.1:" + portA + "/echo-creds"));
	}

	@Test
	void b04_applicationInterceptorRewritesUrl_thenCrossOriginRedirect_credentialsStripped() throws Exception {
		var client = new OkHttpClient.Builder().addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
			.url(chain.request().url().newBuilder().port(portB).build()).build())).build();
		assertEquals("200|auth=none|cookie=none", get(client, "http://127.0.0.1:" + portA + "/redirect-to-a"));
	}

	// Sends a GET with caller-set Authorization (and Cookie, unless the client has a CookieJar, which would replace it)
	// through a transport over the given caller-supplied client; returns "status|body".
	private static String get(OkHttpClient client, String url) throws Exception {
		try (var transport = OkHttpTransport.builder().httpClient(client).build()) {
			var b = TransportRequest.builder().method("GET").uri(url).header("Authorization", "Bearer secret-token");
			if (client.cookieJar() == CookieJar.NO_COOKIES)
				b.header("Cookie", "session=abc123");
			try (var response = transport.execute(b.build())) {
				return response.getStatusCode() + "|" + new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
			}
		}
	}
}
