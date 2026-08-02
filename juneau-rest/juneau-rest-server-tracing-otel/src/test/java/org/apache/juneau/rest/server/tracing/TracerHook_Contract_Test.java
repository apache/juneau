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
package org.apache.juneau.rest.server.tracing;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link TracerHook} SPI contract end-to-end &mdash; {@link TracerHook#startSpan} fires
 * once per {@code @RestOp} call (including exception paths), the returned {@link Scope} receives the
 * resolved status code, the error path receives the thrown throwable, and the framework always
 * closes the scope (in a {@code finally} block).
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables (Scope, MockRestClient); Eclipse JDT @Owning warning is by design.
})
class TracerHook_Contract_Test extends TestBase {

	/** Recording {@link TracerHook} that captures every span open / close transition. */
	public static final class RecordingTracerHook implements TracerHook {
		public final List<RecordingScope> spans = new CopyOnWriteArrayList<>();

		@Override
		public Scope startSpan(RestRequest request) {
			var s = new RecordingScope(request.getMethod());
			spans.add(s);
			return s;
		}

		public RecordingScope last() { return spans.get(spans.size() - 1); }
	}

	public static final class RecordingScope implements Scope {
		public final String method;
		public final AtomicInteger statusCode = new AtomicInteger(-1);
		public final AtomicReference<Throwable> error = new AtomicReference<>();
		public final AtomicInteger closeCount = new AtomicInteger(0);

		RecordingScope(String method) { this.method = method; }

		@Override public void setStatusCode(int statusCode) { this.statusCode.set(statusCode); }
		@Override public void setError(Throwable t) { this.error.set(t); }
		@Override public void close() { closeCount.incrementAndGet(); }
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Happy path — one startSpan, one setStatusCode(200), zero setError, one close.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingTracerHook A_HOOK = new RecordingTracerHook();

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return A_HOOK; }

		@RestGet("/users/{id}")
		public String getUser(@org.apache.juneau.http.Path String id) { return "user:" + id; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_happyPath_oneSpanOpenedSetStatusClosed() throws Exception {
		A_HOOK.spans.clear();
		CA.get("/users/42").run().assertStatus(200);
		assertEquals(1, A_HOOK.spans.size());
		var s = A_HOOK.last();
		assertEquals("GET", s.method);
		assertEquals(200, s.statusCode.get());
		assertNull(s.error.get());
		assertEquals(1, s.closeCount.get(), "close() called exactly once");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Exception path — span still opens, status code 500, error set, scope closed exactly once.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingTracerHook B_HOOK = new RecordingTracerHook();

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return B_HOOK; }

		@RestGet("/boom")
		public String boom() { throw new IllegalStateException("kaboom"); }
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_exceptionPath_setErrorThenClose() throws Exception {
		B_HOOK.spans.clear();
		CB.get("/boom").run().assertStatus(500);
		assertEquals(1, B_HOOK.spans.size());
		var s = B_HOOK.last();
		assertEquals(500, s.statusCode.get());
		assertNotNull(s.error.get());
		assertEquals("IllegalStateException", cns(s.error.get()));
		assertEquals(1, s.closeCount.get(), "close() called exactly once even on exception path");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Off-by-default — no @Bean TracerHook means NoOpTracerHook with NoOpScope; recording hook on a
	//    different resource sees nothing.
	// -----------------------------------------------------------------------------------------------------------------

	private static final RecordingTracerHook C_CANARY = new RecordingTracerHook();

	@Rest
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test void c01_noTracerBean_noSpansFanOut() throws Exception {
		C_CANARY.spans.clear();
		CC.get("/ping").run().assertStatus(200);
		assertEquals(0, C_CANARY.spans.size(), "without a @Bean TracerHook, no spans reach external tracers");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: NoOpTracerHook direct contract.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_noOpTracer_isSingleton() {
		assertSame(NoOpTracerHook.INSTANCE, NoOpTracerHook.INSTANCE);
	}

	@Test void d02_noOpTracer_returnsNoOpScope() {
		var scope = NoOpTracerHook.INSTANCE.startSpan((RestRequest) null);
		assertSame(NoOpTracerHook.NoOpScope.INSTANCE, scope);
	}

	@Test void d03_noOpScope_allMethodsAreNoOp() {
		assertDoesNotThrow(() -> {
			NoOpTracerHook.NoOpScope.INSTANCE.setStatusCode(200);
			NoOpTracerHook.NoOpScope.INSTANCE.setError(new RuntimeException());
			NoOpTracerHook.NoOpScope.INSTANCE.close();
		});
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Source compatibility of the carrier/operation-aware TracerHook/Scope defaults added for the neutral
	//    tracing seam — a bare single-method lambda TracerHook and a legacy three-method Scope must still compile
	//    and satisfy the (unchanged) SAM/functional-interface contract.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_lambdaTracerHook_isFunctionalInterface_andThreeArgOverloadDelegates() {
		var calls = new AtomicInteger();
		// A bare lambda only ever implements the single abstract startSpan(RestRequest) method — if the new
		// carrier/operation overload were anything but a default method, this line would fail to compile.
		TracerHook hook = request -> {
			calls.incrementAndGet();
			return NoOpTracerHook.NoOpScope.INSTANCE;
		};
		var scope = hook.startSpan(null, null, TraceOperation.DEFAULT);
		assertEquals(1, calls.get(), "default startSpan(request,carrier,operation) must delegate to startSpan(request)");
		assertSame(NoOpTracerHook.NoOpScope.INSTANCE, scope);
	}

	/** Records every {@link TraceContextCarrier#set(String, String)} call for {@link #e02_defaultInject_isNoOp}. */
	private static final class E02_RecordingCarrier implements TraceContextCarrier {
		final Map<String,String> written = new LinkedHashMap<>();

		@Override public String get(String key) { return null; }
		@Override public Iterable<String> keys() { return List.of(); }
		@Override public void set(String key, String value) { written.put(key, value); }
	}

	@Test void e02_defaultInject_isNoOp() {
		TracerHook hook = request -> NoOpTracerHook.NoOpScope.INSTANCE;
		var carrier = new E02_RecordingCarrier();
		hook.inject(carrier);
		assertTrue(carrier.written.isEmpty(), "default TracerHook.inject(...) must not write anything");
	}

	@Test void e03_legacyThreeMethodScope_compilesAndRecordRpcErrorDefaultsToNoOp() {
		// A legacy Scope implementing only the three original methods (no recordRpcError override) must still
		// compile and satisfy the interface.
		Scope scope = new Scope() {
			@Override public void setStatusCode(int statusCode) { /* no-op for this test */ }
			@Override public void setError(Throwable error) { /* no-op for this test */ }
			@Override public void close() { /* no-op for this test */ }
		};
		assertDoesNotThrow(() -> scope.recordRpcError(-32601, "Method not found"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Carrier/operation extraction fires only for an active tracer, after argument resolution, and reaches the
	//    three-arg startSpan overload before the @RestOp handler runs. The active Scope is also stashed on the
	//    RestRequest (for a later MCP JSON-RPC error observation to consume) before the handler runs.
	// -----------------------------------------------------------------------------------------------------------------

	/** No-op {@link TraceContextCarrier} recognized end-to-end through {@link F_Extractor}. */
	private static final TraceContextCarrier F_CARRIER = new TraceContextCarrier() {
		@Override public String get(String key) { return null; }
		@Override public Iterable<String> keys() { return List.of(); }
		@Override public void set(String key, String value) { /* unused by this fixture */ }
	};

	/** Records every resolved-argument array it sees, and always hands back {@link #F_CARRIER}. */
	public static final class F_Extractor implements TraceContextExtractor {
		public final List<Object[]> seenArgs = new CopyOnWriteArrayList<>();
		public final AtomicInteger calls = new AtomicInteger();

		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			calls.incrementAndGet();
			seenArgs.add(resolvedArguments.clone());
			return Optional.of(F_CARRIER);
		}

		@Override
		public TraceOperation operation(RestRequest request, Object[] resolvedArguments) {
			return TraceOperation.of("custom-op");
		}
	}

	/**
	 * A {@link TracerHook} that implements only the three-arg overload &mdash; {@link #startSpan(RestRequest)}
	 * throws, so any test call reaching it proves the invoker incorrectly fell back to the legacy single-arg
	 * entry point for an active tracer instead of the carrier/operation-aware overload.
	 */
	public static final class F_TracerHook implements TracerHook {
		public final List<RecordingScope> spans = new CopyOnWriteArrayList<>();
		public volatile TraceContextCarrier lastCarrier;
		public volatile TraceOperation lastOperation;

		@Override
		public Scope startSpan(RestRequest request) {
			throw new AssertionError("startSpan(RestRequest) must not be called for an active tracer — "
				+ "the invoker must use the carrier/operation-aware overload");
		}

		@Override
		public Scope startSpan(RestRequest request, TraceContextCarrier carrier, TraceOperation operation) {
			lastCarrier = carrier;
			lastOperation = operation;
			var s = new RecordingScope(request.getMethod());
			spans.add(s);
			return s;
		}

		public RecordingScope last() { return spans.get(spans.size() - 1); }
	}

	private static final F_TracerHook F_HOOK = new F_TracerHook();
	private static final F_Extractor F_EXTRACTOR = new F_Extractor();
	private static final AtomicReference<Scope> F_SCOPE_DURING_HANDLER = new AtomicReference<>();

	@Rest
	public static class F extends RestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public TracerHook tracer() { return F_HOOK; }

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return F_EXTRACTOR; }

		@RestGet("/greet/{name}")
		public String greet(@org.apache.juneau.http.Path String name, RestRequest req) {
			// Read back the stashed Scope from *inside* the handler to prove it was set before the handler ran.
			F_SCOPE_DURING_HANDLER.set(req.getAttribute(TraceContextResponseProcessor.ATTR_SCOPE).as(Scope.class).orElse(null));
			return "hi:" + name;
		}

		@RestGet("/boom")
		public String boom() { throw new IllegalStateException("f-boom"); }

		@RestGet("/async")
		public CompletableFuture<String> async() {
			return CompletableFuture.completedFuture("f-async");
		}
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_extractionSeesResolvedArguments_andReachesThreeArgOverloadBeforeHandler() throws Exception {
		F_HOOK.spans.clear();
		F_EXTRACTOR.seenArgs.clear();
		F_EXTRACTOR.calls.set(0);
		F_SCOPE_DURING_HANDLER.set(null);
		CF.get("/greet/ada").run().assertStatus(200).assertContent("hi:ada");
		assertEquals(1, F_EXTRACTOR.calls.get());
		assertEquals(1, F_EXTRACTOR.seenArgs.size());
		var seen = F_EXTRACTOR.seenArgs.get(0);
		assertEquals(2, seen.length, "extractor must see every resolved @RestOp argument, in declaration order");
		assertEquals("ada", seen[0], "extractor must see the fully-resolved @Path argument, not the raw path segment");
		assertEquals(1, F_HOOK.spans.size());
		assertSame(F_CARRIER, F_HOOK.lastCarrier);
		assertEquals("custom-op", F_HOOK.lastOperation.getSpanName());
		assertSame(F_HOOK.last(), F_SCOPE_DURING_HANDLER.get(),
			"the Scope stashed on the request attribute must be the same instance startSpan(...) returned, and set before the handler runs");
	}

	@Test void f02_exceptionPath_extractionStillOccurs_scopeReceivesError() throws Exception {
		F_HOOK.spans.clear();
		F_EXTRACTOR.calls.set(0);
		CF.get("/boom").run().assertStatus(500);
		assertEquals(1, F_EXTRACTOR.calls.get(), "extraction must still occur even though the handler throws");
		var s = F_HOOK.last();
		assertEquals(500, s.statusCode.get());
		assertNotNull(s.error.get());
		assertEquals(1, s.closeCount.get(), "close() called exactly once even on the exception path");
	}

	@Test void f03_completionStagePath_extractionOccursOnce_scopeClosedAfterCompletion() throws Exception {
		F_HOOK.spans.clear();
		F_EXTRACTOR.calls.set(0);
		CF.get("/async").run().assertStatus(200).assertContent("f-async");
		assertEquals(1, F_EXTRACTOR.calls.get(), "extraction happens once, before the CompletionStage handler runs — not again at completion");
		var s = F_HOOK.last();
		assertEquals(200, s.statusCode.get());
		assertEquals(1, s.closeCount.get(), "deferred observability must still close the scope exactly once");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: No-tracer fast path — a registered TraceContextExtractor is never resolved/invoked when no non-no-op
	//    TracerHook bean is present, matching the pre-existing zero-allocation no-op contract.
	// -----------------------------------------------------------------------------------------------------------------

	public static final class G_CanaryExtractor implements TraceContextExtractor {
		public final AtomicInteger calls = new AtomicInteger();

		@Override
		public Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments) {
			calls.incrementAndGet();
			return Optional.empty();
		}
	}

	private static final G_CanaryExtractor G_CANARY = new G_CanaryExtractor();

	@Rest
	public static class G extends RestServlet {
		private static final long serialVersionUID = 1L;

		// No @Bean TracerHook — the framework falls back to NoOpTracerHook.

		@org.apache.juneau.commons.inject.Bean
		public TraceContextExtractor extractor() { return G_CANARY; }

		@RestGet("/ping")
		public String ping() { return "pong"; }
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test void g01_noTracerBean_extractorNeverResolvedOrCalled() throws Exception {
		G_CANARY.calls.set(0);
		CG.get("/ping").run().assertStatus(200).assertContent("pong");
		assertEquals(0, G_CANARY.calls.get(), "no-tracer fast path must not resolve/invoke a registered TraceContextExtractor");
	}
}
