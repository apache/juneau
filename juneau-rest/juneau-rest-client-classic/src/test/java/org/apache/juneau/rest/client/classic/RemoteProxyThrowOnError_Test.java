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

import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Focused test suite for the classic REST-proxy engine's {@link Remote#throwOnError()}/{@link RemoteOp#throwOnError()}
 * behavior (TODO-351 item B-client-1, Tier 2).
 *
 * <p>
 * This member changes the classic engine's default per-call error handling to match the next-generation engine's
 * {@code throwIfError()} semantics:
 * <ul>
 * 	<li>A declared typed exception (a {@link BasicHttpException} subtype whose {@code STATUS_CODE} matches the response
 * 		status) is always thrown, regardless of {@code throwOnError}.
 * 	<li>Otherwise, when {@code throwOnError=true}, a generic {@link BasicHttpException} carrying the status is thrown.
 * 	<li>Otherwise (the default, {@code throwOnError=false}), the error body flows through as the return value.
 * </ul>
 */
@SuppressWarnings({
	"resource" // RestClient instances are created per-test and closed via try-with-resources.
})
class RemoteProxyThrowOnError_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;
	private static volatile int status = 404;
	private static volatile String responseBody = "not found";

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// starves under -T1C reactor-level parallel test load and can fail with "server failed to respond".
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/", exchange -> {
			exchange.getRequestBody().readAllBytes();
			var resp = responseBody.getBytes(UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(status, resp.length == 0 ? -1 : resp.length);
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
		status = 404;
		responseBody = "not found";
	}

	private static String url() {
		return "http://localhost:" + port;
	}

	//------------------------------------------------------------------------------------------------------------------
	// throwOnError=false (default) : error body flows through as the return value
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface DefaultRemote {
		@RemoteGet(path = "/")
		String call();
	}

	@Test void a01_default_throwOnErrorFalse_bodyFlowsThrough() throws Exception {
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var result = c.getRemote(DefaultRemote.class, url()).call();
			assertEquals("not found", result, "With throwOnError=false the 404 body should flow through");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// throwOnError=true : generic BasicHttpException thrown on unmatched error
	//------------------------------------------------------------------------------------------------------------------

	@Remote(throwOnError = true)
	public interface ThrowOnErrorRemote {
		@RemoteGet(path = "/")
		String call();
	}

	@Test void a02_throwOnErrorTrue_throwsGenericException() throws Exception {
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var proxy = c.getRemote(ThrowOnErrorRemote.class, url());
			var e = assertThrows(BasicHttpException.class, proxy::call);
			assertEquals(404, e.getStatusCode());
		}
	}

	@Remote
	public interface ThrowOnErrorMethodRemote {
		@RemoteGet(path = "/", throwOnError = true)
		String call();
	}

	@Test void a03_throwOnErrorTrue_methodLevel_throwsGenericException() throws Exception {
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var proxy = c.getRemote(ThrowOnErrorMethodRemote.class, url());
			var e = assertThrows(BasicHttpException.class, proxy::call);
			assertEquals(404, e.getStatusCode());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Declared typed exception : always thrown on a status match, regardless of throwOnError
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface TypedExceptionRemote {
		@RemoteGet(path = "/")
		String call() throws NotFound;
	}

	@Test void a04_typedException_matchesStatus_thrownEvenWhenThrowOnErrorFalse() throws Exception {
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var proxy = c.getRemote(TypedExceptionRemote.class, url());
			assertThrows(NotFound.class, proxy::call);
		}
	}

	@Test void a05_typedException_noMatch_bodyFlowsThrough() throws Exception {
		// Server returns 500 but the method only declares NotFound (404) -> no typed match, throwOnError=false -> body flows through.
		status = 500;
		responseBody = "boom";
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var proxy = c.getRemote(TypedExceptionRemote.class, url());
			assertEquals("boom", proxy.call());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Success responses are unaffected
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_successResponse_returnsBody_withThrowOnErrorTrue() throws Exception {
		status = 200;
		responseBody = "ok";
		try (var c = RestClient.create().allowPrivateUrls(true).build()) {
			var proxy = c.getRemote(ThrowOnErrorRemote.class, url());
			assertEquals("ok", proxy.call());
		}
	}
}
