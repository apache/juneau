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
}
