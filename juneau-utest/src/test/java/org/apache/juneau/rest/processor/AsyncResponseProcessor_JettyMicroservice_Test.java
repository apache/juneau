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
package org.apache.juneau.rest.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity coverage for {@link AsyncResponseProcessor}.
 *
 * <p>
 * The {@code MockServletRequest} test harness reports {@code isAsyncSupported() == false}, so the
 * sibling unit-test class {@code AsyncResponseProcessor_Test} only exercises the synchronous-fallback
 * path. This class boots a real Jetty microservice on an ephemeral port to drive the async-dispatch
 * branch ({@code req.startAsync()}, the {@code AsyncListener} callbacks, the {@code finalizeAsync}
 * pipeline, and the {@code unwrap} static helper) so that the {@code AsyncContext}-driven path is
 * actually exercised at coverage time.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class AsyncResponseProcessor_JettyMicroservice_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Resource — exercises success, failed-future, timeout, completion-stage, and bare-Future rejection on the
	// real async-dispatch path.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/async/*", asyncTimeoutMillis="200")
	public static class AsyncTestServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ok")
		public CompletableFuture<String> ok() {
			return CompletableFuture.supplyAsync(() -> "async-ok");
		}

		@RestGet("/okStage")
		public CompletionStage<String> okStage() {
			return CompletableFuture.supplyAsync(() -> "async-stage-ok");
		}

		@RestGet("/failed")
		public CompletableFuture<String> failed() {
			return CompletableFuture.supplyAsync(() -> {
				throw new IllegalStateException("kaboom-async");
			});
		}

		@RestGet("/failedDirect")
		public CompletableFuture<String> failedDirect() {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new IllegalStateException("direct-failure"));
			return f;
		}

		@RestGet("/timeout")
		public CompletableFuture<String> timeout() {
			return new CompletableFuture<>();  // Never completes — fires the AsyncContext timeout.
		}

		@RestGet("/bareFuture")
		public Future<String> bareFuture() {
			// FutureTask is not a CompletionStage — must be rejected with a 500.
			return new FutureTask<>(() -> "should-not-block");
		}

		@RestGet("/sync")
		public String sync() {
			return "still-sync";
		}

		@RestGet("/failedExecutionException")
		public CompletableFuture<String> failedExecutionException() {
			// Complete with an ExecutionException directly (not the usual CompletionException)
			// so that unwrap(...) hits its ExecutionException-stripping branch.
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new ExecutionException("wrapped", new IllegalStateException("inner-cause")));
			return f;
		}
	}

	@Configuration
	public static class Config {
		@Bean(name="asyncTestServlet")
		public Servlet asyncTestServlet() { return new AsyncTestServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(Config.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Happy-path async — CompletableFuture and CompletionStage.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_completableFuture_unwrapped() throws Exception {
		var resp = get("/async/ok");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("async-ok"), "body: " + resp.body());
	}

	@Test void a02_completionStage_unwrapped() throws Exception {
		var resp = get("/async/okStage");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("async-stage-ok"), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Failed-future paths — exercise the unwrap(...) static helper for CompletionException-wrapped causes
	// (supplyAsync wraps the thrown exception in CompletionException) AND the direct-completeExceptionally case.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_failedFutureViaSupplyAsync_returns500() throws Exception {
		// supplyAsync → exception thrown in the supplier is wrapped in CompletionException
		// — exercises the unwrap branch that strips the CompletionException layer.
		var resp = get("/async/failed");
		assertEquals(500, resp.statusCode(), "body: " + resp.body());
	}

	@Test void b02_failedFutureViaCompleteExceptionally_returns500() throws Exception {
		// completeExceptionally with a plain throwable — exercises the "no CompletionException
		// wrapping" branch of unwrap (which falls through to return the throwable as-is).
		var resp = get("/async/failedDirect");
		assertEquals(500, resp.statusCode(), "body: " + resp.body());
	}

	@Test void b03_failedFutureViaExecutionException_returns500() throws Exception {
		// completeExceptionally(ExecutionException) — exercises the unwrap branch that
		// strips an ExecutionException wrapper (line 297-298 of AsyncResponseProcessor).
		var resp = get("/async/failedExecutionException");
		assertEquals(500, resp.statusCode(), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Async timeout — the AsyncContext fires onTimeout() after the configured 200ms ceiling.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_neverCompleting_returns504() throws Exception {
		var before = System.currentTimeMillis();
		var resp = get("/async/timeout");
		var elapsed = System.currentTimeMillis() - before;
		assertEquals(504, resp.statusCode(), "body: " + resp.body());
		assertTrue(elapsed < 5_000, "200ms timeout should fire well before the 5s client timeout — actual " + elapsed + "ms");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Bare Future rejection — anything that is a Future but not a CompletionStage is rejected with 500.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_bareFuture_returns500() throws Exception {
		var resp = get("/async/bareFuture");
		assertEquals(500, resp.statusCode(), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Synchronous handler in the same servlet — backward-compat smoke test that the processor passes through
	// non-future content unchanged on the real container.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_syncHandler_unchanged() throws Exception {
		var resp = get("/async/sync");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("still-sync"), "body: " + resp.body());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Resource G — async-completion executor configured exercises the whenCompleteAsync branch
	// (line 237 true, line 238 invocation).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(paths="/asyncExec/*", asyncTimeoutMillis="200", asyncCompletionExecutor="execPool")
	public static class AsyncExecServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		// The bean is declared on the resource class itself so the resource's bean store
		// resolves the executor name at startup.
		@Bean(name="execPool")
		public Executor execPool() {
			return Executors.newSingleThreadExecutor(r -> {
				var t = new Thread(r, "async-exec-pool");
				t.setDaemon(true);
				return t;
			});
		}

		@RestGet("/value")
		public CompletableFuture<String> value() {
			return CompletableFuture.supplyAsync(() -> "exec-pool-async");
		}
	}

	@Configuration
	public static class ConfigExec {
		@Bean(name="asyncExecServlet")
		public Servlet asyncExecServlet() { return new AsyncExecServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture execFixture = MicroserviceTestFixture.create()
		.configurations(ConfigExec.class);

	@Test void g01_completionExecutor_routesCallback() throws Exception {
		// asyncCompletionExecutor configured — exercises whenCompleteAsync(callback, executor)
		// path (line 237 true / line 238).
		var port = execFixture.getPort();
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + "/asyncExec/value"))
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofString());
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("exec-pool-async"), "body: " + resp.body());
	}
}
