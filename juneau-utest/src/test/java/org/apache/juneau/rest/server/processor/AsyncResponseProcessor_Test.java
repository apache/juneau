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
package org.apache.juneau.rest.server.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.mock.classic.MockRestClient;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

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

	// -----------------------------------------------------------------------------------------------------------------
	// G: Pre-cancelled CompletableFuture — sync fallback hits CancellationException catch.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncTimeoutMillis = "5000")
	public static class G {
		@RestGet("/cancelled")
		public CompletableFuture<String> cancelled() {
			var f = new CompletableFuture<String>();
			f.cancel(true);  // Pre-cancel — cf.get(timeout) throws CancellationException synchronously.
			return f;
		}
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test void g01_preCancelledFuture_returns500() throws Exception {
		// Cancellation surfaces as a 500 (sync-fallback CancellationException catch).
		CG.get("/cancelled").run().assertStatus(500);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: Default timeout fallback — neither @Rest nor @RestOp supplies asyncTimeoutMillis.
	//    Exercises the DEFAULT_ASYNC_TIMEOUT_MILLIS branch of resolveTimeoutMillis (line 307 false-branch).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestGet("/quick")
		public CompletableFuture<String> quick() {
			return CompletableFuture.completedFuture("default-timeout-applied");
		}
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_defaultTimeout_completesNormally() throws Exception {
		// With no annotation timeout, resolveTimeoutMillis returns DEFAULT_ASYNC_TIMEOUT_MILLIS (30s).
		// The completed future returns immediately under that 30s ceiling.
		CH.get("/quick").run().assertStatus(200).assertContent().isContains("default-timeout-applied");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// I: Static helper isAsyncDispatchOwned(...) overloads — exercised directly without a real container.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_isAsyncDispatchOwned_nullSession_false() {
		assertFalse(AsyncResponseProcessor.isAsyncDispatchOwned((org.apache.juneau.rest.server.RestOpSession) null));
	}

	@Test void i02_isAsyncDispatchOwned_nullRequest_false() {
		assertFalse(AsyncResponseProcessor.isAsyncDispatchOwned((jakarta.servlet.http.HttpServletRequest) null));
	}

	@Test void i03_isAsyncDispatchOwned_attributeMissing_false() {
		var req = MockServletRequest.create();
		assertFalse(AsyncResponseProcessor.isAsyncDispatchOwned(req));
	}

	@Test void i04_isAsyncDispatchOwned_attributeFalse_false() {
		var req = MockServletRequest.create();
		req.setAttribute(AsyncResponseProcessor.ATTR_ASYNC_DISPATCH_OWNED, Boolean.FALSE);
		assertFalse(AsyncResponseProcessor.isAsyncDispatchOwned(req));
	}

	@Test void i05_isAsyncDispatchOwned_attributeNonBoolean_false() {
		var req = MockServletRequest.create();
		req.setAttribute(AsyncResponseProcessor.ATTR_ASYNC_DISPATCH_OWNED, "true");  // Non-Boolean value.
		assertFalse(AsyncResponseProcessor.isAsyncDispatchOwned(req));
	}

	@Test void i06_isAsyncDispatchOwned_attributeBooleanTrue_true() {
		var req = MockServletRequest.create();
		req.setAttribute(AsyncResponseProcessor.ATTR_ASYNC_DISPATCH_OWNED, Boolean.TRUE);
		assertTrue(AsyncResponseProcessor.isAsyncDispatchOwned(req));
	}

	@Test void i07_attrConstant_hasExpectedValue() {
		assertEquals("org.apache.juneau.rest.server.async.dispatchOwned", AsyncResponseProcessor.ATTR_ASYNC_DISPATCH_OWNED);
	}

	@Test void i08_defaultTimeoutConstant_isThirtySeconds() {
		assertEquals(30_000L, AsyncResponseProcessor.DEFAULT_ASYNC_TIMEOUT_MILLIS);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// J: ResponseProcessor return code — process(...) on a non-future returns NEXT (passes through).
	//    Exercises the "content == null" and "non-future, non-Future" branches of process(...).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J {
		@RestGet("/string")
		public String string() { return "passes-through"; }

		@RestGet("/empty")
		public CompletableFuture<String> empty() {
			return CompletableFuture.completedFuture("");
		}
	}

	private static final MockRestClient CJ = MockRestClient.buildLax(J.class);

	@Test void j01_nonFutureContent_passesThrough() throws Exception {
		// Verifies that AsyncResponseProcessor returns NEXT on non-future content,
		// allowing the rest of the chain (SerializedPojoProcessor) to handle it.
		CJ.get("/string").run().assertStatus(200).assertContent().isContains("passes-through");
	}

	@Test void j02_emptyStringFuture_completesNormally() throws Exception {
		// Empty-string content unwrapped from the future and serialized normally.
		CJ.get("/empty").run().assertStatus(200);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// K: Failed CompletableFuture chained through CompletionStage — failed-future propagation through
	//    thenApply still surfaces as ExecutionException in the sync fallback.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K {
		@RestGet("/chained")
		public CompletionStage<String> chained() {
			return CompletableFuture.supplyAsync(() -> "step1")
				.thenApply(s -> {
					throw new IllegalStateException("downstream-failure");
				});
		}

		@RestGet("/withCompletionException")
		public CompletableFuture<String> withCompletionException() {
			var f = new CompletableFuture<String>();
			// Wrap directly in CompletionException — exercises the unwrap/cause chain.
			f.completeExceptionally(new CompletionException(new IllegalArgumentException("inner")));
			return f;
		}
	}

	private static final MockRestClient CK = MockRestClient.buildLax(K.class);

	@Test void k01_chainedFailedFuture_returns500() throws Exception {
		CK.get("/chained").run().assertStatus(500);
	}

	@Test void k02_completionExceptionWrapping_returns500() throws Exception {
		// In the sync-fallback path, ExecutionException.getCause() is the CompletionException;
		// the existing convertThrowable pipeline maps it to a 500.
		CK.get("/withCompletionException").run().assertStatus(500);
	}
}
