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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * End-to-end tests validating that the <b>classic</b> REST-proxy engine now honors the
 * {@link Remote @Remote}/{@link RemoteOp @RemoteOp} members that were previously silent no-ops, at feature parity with
 * the next-generation engine (TODO-351 item B-client-1).
 *
 * <p>
 * Each test drives a real proxy call against an in-process {@link HttpServer} and asserts on what the server received
 * (headers, query string, form body, method, path) or returned (status-code handling for retries/throwOnError).  These
 * complement the metadata-level unit tests in {@code RemoteMeta_Test}/{@code RemoteOperationMeta_Test}.
 */
@SuppressWarnings({
	"resource" // RestClient instances are created per-test and closed via try-with-resources where practical; a few short-lived proxies intentionally rely on JVM teardown.
})
class RemoteProxyParity_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;

	// Captured request state (reset per test).
	private static final AtomicInteger ATTEMPTS = new AtomicInteger();
	private static volatile int status = 200;
	private static volatile int failsBeforeSuccess = 0;
	private static volatile long sleepMs = 0;
	private static volatile String responseBody = "OK";
	private static volatile String responseContentType = "text/plain";
	private static volatile String lastMethod;
	private static volatile String lastPath;
	private static volatile String lastQuery;
	private static volatile String lastBody;
	private static volatile String lastAccept;
	private static volatile String lastContentType;
	private static volatile String lastXFoo;
	private static volatile String lastXBar;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// starves under -T1C reactor-level parallel test load and can fail with "server failed to respond".
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/", exchange -> {
			var n = ATTEMPTS.incrementAndGet();
			lastMethod = exchange.getRequestMethod();
			var uri = exchange.getRequestURI();
			lastPath = uri.getPath();
			lastQuery = uri.getRawQuery();
			var h = exchange.getRequestHeaders();
			lastAccept = h.getFirst("Accept");
			lastContentType = h.getFirst("Content-Type");
			lastXFoo = h.getFirst("X-Foo");
			lastXBar = h.getFirst("X-Bar");
			lastBody = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
			if (sleepMs > 0) {
				try { Thread.sleep(sleepMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			}
			var code = status;
			if (failsBeforeSuccess > 0 && n <= failsBeforeSuccess)
				code = 503;
			var resp = responseBody.getBytes(UTF_8);
			exchange.getResponseHeaders().set("Content-Type", responseContentType);
			exchange.sendResponseHeaders(code, resp.length == 0 ? -1 : resp.length);
			try (var os = exchange.getResponseBody()) {
				os.write(resp);
			}
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
		if (executor != null)
			executor.shutdownNow();
	}

	@BeforeEach
	void reset() {
		ATTEMPTS.set(0);
		status = 200;
		failsBeforeSuccess = 0;
		sleepMs = 0;
		responseBody = "OK";
		responseContentType = "text/plain";
		lastMethod = lastPath = lastQuery = lastBody = lastAccept = lastContentType = lastXFoo = lastXBar = null;
	}

	private static String url() {
		return "http://localhost:" + port;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 1 - headers() : method-level merges with + overrides interface-level
	//------------------------------------------------------------------------------------------------------------------

	@Remote(headers = {"X-Foo: iface", "X-Bar: bar-iface"})
	public interface HeadersRemote {
		@RemoteGet(path = "/", headers = {"X-Foo: method"})
		void call();
	}

	@Test void t1a_headers_methodOverridesInterface_andMerges() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(HeadersRemote.class, url()).call();
		}
		assertEquals("method", lastXFoo, "Method-level X-Foo should override interface-level");
		assertEquals("bar-iface", lastXBar, "Interface-only X-Bar should pass through");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 1 - queryData() : interface + method constant parts, method overrides on name collision
	//------------------------------------------------------------------------------------------------------------------

	@Remote(queryData = {"q1=iface", "q2=v2"})
	public interface QueryDataRemote {
		@RemoteGet(path = "/", queryData = {"q1=method"})
		void call();
	}

	@Test void t1b_queryData_interfaceAndMethod_merged() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(QueryDataRemote.class, url()).call();
		}
		assertNotNull(lastQuery);
		assertTrue(lastQuery.contains("q1=method"), "Actual query: " + lastQuery);
		assertTrue(lastQuery.contains("q2=v2"), "Actual query: " + lastQuery);
		assertFalse(lastQuery.contains("q1=iface"), "Method-level q1 should have overridden interface-level: " + lastQuery);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 1 - formData() : interface + method constant parts posted as the form body
	//------------------------------------------------------------------------------------------------------------------

	@Remote(formData = {"f1=iface", "f2=v2"})
	public interface FormDataRemote {
		@RemotePost(path = "/", formData = {"f1=method"})
		void call();
	}

	@Test void t1c_formData_interfaceAndMethod_merged() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(FormDataRemote.class, url()).call();
		}
		assertEquals("POST", lastMethod);
		assertNotNull(lastBody);
		assertTrue(lastBody.contains("f1=method"), "Actual body: " + lastBody);
		assertTrue(lastBody.contains("f2=v2"), "Actual body: " + lastBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 1 - accept() : sets the Accept header
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface AcceptRemote {
		@RemoteGet(path = "/", accept = "application/json")
		void call();
	}

	@Test void t1d_accept_setsAcceptHeader() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(AcceptRemote.class, url()).call();
		}
		assertEquals("application/json", lastAccept);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 1 - contentType() : labels the Content-Type header (even with no matching serializer registered)
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface ContentTypeRemote {
		@RemotePost(path = "/", contentType = "text/custom")
		void call(@Content String body);
	}

	@Test void t1e_contentType_labelsContentTypeHeader() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(ContentTypeRemote.class, url()).call("hello");
		}
		assertNotNull(lastContentType);
		assertTrue(lastContentType.startsWith("text/custom"), "Actual Content-Type: " + lastContentType);
		assertEquals("hello", lastBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 2 - baseUrl() : interface-level base/host override (resolved via VarResolver system property)
	//------------------------------------------------------------------------------------------------------------------

	@Remote(baseUrl = "$S{juneau.test.baseUrl}")
	public interface BaseUrlRemote {
		@RemoteGet(path = "/base")
		void call();
	}

	@Test void t2a_baseUrl_interfaceLevel_noClientRootUrl() throws Exception {
		System.setProperty("juneau.test.baseUrl", url());
		try (var c = RestClient.create().build()) {   // No client rootUrl -- baseUrl must supply the host.
			c.getRemote(BaseUrlRemote.class).call();
		} finally {
			System.clearProperty("juneau.test.baseUrl");
		}
		assertEquals("/base", lastPath);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 2 - @Url param : call-time endpoint replacement, wins over baseUrl
	//------------------------------------------------------------------------------------------------------------------

	@Remote(baseUrl = "http://bogus.invalid")
	public interface UrlParamRemote {
		@RemoteGet
		void call(@Url String url);
	}

	@Test void t2b_urlParam_overridesBaseUrl() throws Exception {
		try (var c = RestClient.create().build()) {
			c.getRemote(UrlParamRemote.class).call(url() + "/fromparam");
		}
		assertEquals("/fromparam", lastPath);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 2 - SSRF guard : non-http(s) schemes are rejected
	//------------------------------------------------------------------------------------------------------------------

	@Test void t2c_ssrf_urlParam_fileScheme_rejected() throws Exception {
		try (var c = RestClient.create().build()) {
			var proxy = c.getRemote(UrlParamRemote.class);
			var e = assertThrows(IllegalArgumentException.class, () -> proxy.call("file:///etc/passwd"));
			assertTrue(e.getMessage().contains("Unsupported URL scheme"), "Actual: " + e.getMessage());
		}
	}

	@Remote(baseUrl = "file:///etc")
	public interface BadSchemeBaseUrlRemote {
		@RemoteGet(path = "/passwd")
		void call();
	}

	@Test void t2d_ssrf_baseUrl_fileScheme_rejected() throws Exception {
		try (var c = RestClient.create().build()) {
			var proxy = c.getRemote(BadSchemeBaseUrlRemote.class, url());
			assertThrows(IllegalArgumentException.class, proxy::call);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 2 - timeout() : per-call socket timeout
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface TimeoutRemote {
		@RemoteGet(path = "/slow", timeout = "200ms")
		void call();
	}

	@Test void t2e_timeout_perCall_expires() throws Exception {
		sleepMs = 1500;
		try (var c = RestClient.create().build()) {
			var proxy = c.getRemote(TimeoutRemote.class, url());
			assertThrows(Exception.class, proxy::call);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Tier 2 - retries() / retryNonIdempotent() : gated automatic retry
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface RetryGetRemote {
		@RemoteGet(path = "/", retries = 5)
		String call();
	}

	@Test void t2f_retries_idempotentGet_recoversAfter503() throws Exception {
		failsBeforeSuccess = 2;  // First two attempts -> 503, third -> 200.
		try (var c = RestClient.create().build()) {
			var result = c.getRemote(RetryGetRemote.class, url()).call();
			assertEquals("OK", result);
		}
		assertEquals(3, ATTEMPTS.get(), "Should have taken 3 attempts (2 failures + 1 success)");
	}

	@Remote
	public interface RetryPostRemote {
		@RemotePost(path = "/", retries = 5)
		String call(@Content String body);
	}

	@Test void t2g_retries_nonIdempotentPost_notRetriedByDefault() throws Exception {
		failsBeforeSuccess = 2;
		responseBody = "err";
		try (var c = RestClient.create().build()) {
			// POST is not retried without retryNonIdempotent: the first 503 body flows through (throwOnError defaults false).
			var result = c.getRemote(RetryPostRemote.class, url()).call("x");
			assertEquals("err", result);
		}
		assertEquals(1, ATTEMPTS.get(), "POST should not be retried by default");
	}

	@Remote(retryNonIdempotent = true)
	public interface RetryPostOptInRemote {
		@RemotePost(path = "/", retries = 5)
		String call(@Content String body);
	}

	@Test void t2h_retries_nonIdempotentPost_retriedWhenOptedIn() throws Exception {
		failsBeforeSuccess = 2;
		try (var c = RestClient.create().build()) {
			var result = c.getRemote(RetryPostOptInRemote.class, url()).call("x");
			assertEquals("OK", result);
		}
		assertEquals(3, ATTEMPTS.get(), "POST should retry when retryNonIdempotent=true and body is repeatable");
	}
}
