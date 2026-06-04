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

import java.util.concurrent.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AsyncResponseProcessor} — the new processor that unwraps
 * {@link CompletableFuture} / {@link CompletionStage} return values from {@code @RestOp} methods
 * and bridges them to the servlet container's {@link jakarta.servlet.AsyncContext} lifecycle.
 *
 * <p>
 * In the {@code MockRestClient} test harness the underlying {@code MockServletRequest} reports
 * {@code isAsyncSupported() == false}, so this processor exercises its synchronous-fallback path
 * (block on the future with the configured timeout) which is exactly the path that test suites
 * outside a real servlet container will take. The async {@code AsyncContext}-driven path is
 * covered by integration tests against real containers; here we verify the unwrap, error
 * propagation, timeout, and bare-{@code Future} rejection contracts.
 */
class AsyncResponseProcessor_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Happy path — CompletableFuture<String> handler returns the unwrapped string body.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/sync")
		public CompletableFuture<String> sync() {
			return CompletableFuture.completedFuture("hello-async");
		}

		@RestGet("/asyncSupplier")
		public CompletableFuture<String> asyncSupplier() {
			return CompletableFuture.supplyAsync(() -> "supplied-async");
		}

		@RestGet("/completionStage")
		public CompletionStage<String> completionStage() {
			return CompletableFuture.completedStage("stage-value");
		}

		@RestGet("/pojo")
		public CompletableFuture<Pojo> pojo() {
			return CompletableFuture.completedFuture(new Pojo("foo", 42));
		}

		@RestGet("/voidContent")
		public CompletableFuture<Void> voidContent() {
			return CompletableFuture.completedFuture(null);
		}
	}

	public static final class Pojo {
		public final String name;
		public final int value;
		public Pojo(String name, int value) { this.name = name; this.value = value; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_completableFutureString_unwrapsBody() throws Exception {
		CA.get("/sync").run().assertStatus(200).assertContent().isContains("hello-async");
	}

	@Test void a02_completableFutureSupplyAsync_unwrapsBody() throws Exception {
		CA.get("/asyncSupplier").run().assertStatus(200).assertContent().isContains("supplied-async");
	}

	@Test void a03_completionStage_unwrapsBody() throws Exception {
		CA.get("/completionStage").run().assertStatus(200).assertContent().isContains("stage-value");
	}

	@Test void a04_completableFuturePojo_serializesBean() throws Exception {
		CA.get("/pojo").accept("application/json").run()
			.assertStatus(200)
			.assertContent().isContains("\"name\":\"foo\"", "\"value\":42");
	}

	@Test void a05_completableFutureVoid_emitsNull() throws Exception {
		CA.get("/voidContent").run().assertStatus(200);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Error path — failed future routes through the existing error pipeline.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet("/notFound")
		public CompletableFuture<String> notFound() {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new NotFound("Resource not found."));
			return f;
		}

		@RestGet("/internalServerError")
		public CompletableFuture<String> ise() {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new IllegalStateException("kaboom"));
			return f;
		}

		@RestGet("/badRequest/{id}")
		public CompletableFuture<String> badRequest(@Path String id) {
			var f = new CompletableFuture<String>();
			f.completeExceptionally(new BadRequest("Bad id: ''{0}''", id));
			return f;
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_failedFutureWithNotFound_returns404() throws Exception {
		CB.get("/notFound").run().assertStatus(404);
	}

	@Test void b02_failedFutureWithGenericException_returns500() throws Exception {
		CB.get("/internalServerError").run().assertStatus(500);
	}

	@Test void b03_failedFutureWithBadRequest_returns400() throws Exception {
		CB.get("/badRequest/abc").run().assertStatus(400);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Timeout path — never-completing future writes a 504.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncTimeoutMillis = "150")
	public static class C {
		@RestGet("/never")
		public CompletableFuture<String> never() {
			return new CompletableFuture<>();  // Never completes.
		}
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_neverCompletingFuture_returns504OnTimeout() throws Exception {
		CC.get("/never").run().assertStatus(504);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Per-op asyncTimeoutMillis override wins over the resource-level setting.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncTimeoutMillis = "5000")  // Resource default is 5s.
	public static class D {
		@RestOp(method = "GET", path = "/never", asyncTimeoutMillis = "100")  // Op-level wins.
		public CompletableFuture<String> never() {
			return new CompletableFuture<>();
		}
	}

	private static final MockRestClient CD = MockRestClient.buildLax(D.class);

	@Test void d01_perOpTimeoutOverride() throws Exception {
		long before = System.currentTimeMillis();
		CD.get("/never").run().assertStatus(504);
		long elapsed = System.currentTimeMillis() - before;
		assertTrue(elapsed < 2000, "per-op 100ms timeout should fire well before the 5s resource default — actual " + elapsed + "ms");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Bare Future rejection — anything that is a Future but not a CompletionStage is rejected with 500.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestGet("/bare")
		public Future<String> bareFuture() {
			// FutureTask is a bare Future — not a CompletionStage.
			return new FutureTask<>(() -> "should-not-be-blocking");
		}
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void e01_bareFuture_rejected() throws Exception {
		CE.get("/bare").run().assertStatus(500).assertContent().isContains("Bare java.util.concurrent.Future");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Synchronous handlers (no CompletableFuture) are unchanged — backward-compat smoke test.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet("/sync")
		public String sync() { return "still-synchronous"; }

		@RestGet("/null")
		public String nullReturn() { return null; }
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_syncString_unchanged() throws Exception {
		CF.get("/sync").run().assertStatus(200).assertContent().isContains("still-synchronous");
	}

	@Test void f02_syncNull_unchanged() throws Exception {
		CF.get("/null").run().assertStatus(200);
	}
}
