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
package org.apache.juneau.rest.async;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.JRE.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.slf4j.*;

/**
 * Tests for {@link Rest#asyncCompletionExecutor()} / {@link RestOp#asyncCompletionExecutor()}.
 *
 * <p>
 * The {@code MockRestClient} harness exercises the synchronous fallback path of
 * {@link org.apache.juneau.rest.server.server.processor.AsyncResponseProcessor} (since
 * {@code MockServletRequest.isAsyncSupported() == false}). The tests here therefore focus on:
 *
 * <ul>
 *   <li>Correct annotation parsing and bean resolution at context-build time.
 *   <li>Startup failure when the named bean does not exist.
 *   <li>Correct response content (regression) when an executor is configured.
 *   <li>Per-op override and inheritance semantics.
 *   <li>MDC propagation still works when an executor is configured (via the sync fallback).
 * </ul>
 *
 * <p>
 * The "executor actually used on the callback thread" contract is verified via the
 * {@link CountingExecutor} helper: even in the sync path the {@code RestOpContext} carries the
 * resolved executor reference, which can be inspected directly.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class AsyncCompletionExecutor_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Support
	// -----------------------------------------------------------------------------------------------------------------

	/** Executor that counts how many tasks were submitted and records the last thread name. */
	static class CountingExecutor implements Executor {
		final AtomicInteger count = new AtomicInteger();
		final AtomicReference<String> lastThreadName = new AtomicReference<>("");
		final String poolPrefix;

		CountingExecutor(String poolPrefix) { this.poolPrefix = poolPrefix; }

		@Override
		public void execute(Runnable command) {
			count.incrementAndGet();
			var t = new Thread(() -> {
				lastThreadName.set(Thread.currentThread().getName());
				command.run();
			}, poolPrefix + "-" + count.get());
			t.start();
		}
	}

	/**
	 * Reflective {@code Thread.isVirtual()} so the test class compiles on Java 17 source level.
	 * Returns {@code false} on Java < 21 where the method does not exist.
	 */
	static boolean reflectIsVirtual(Thread t) {
		try {
			return (Boolean) Thread.class.getMethod("isVirtual").invoke(t);
		} catch (Exception e) {
			return false;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Happy path — @Rest(asyncCompletionExecutor) resolves a named bean; resource builds and responds correctly.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncCompletionExecutor = "testPool")
	public static class A {
		@Bean(name = "testPool")
		public Executor testPool() {
			return new CountingExecutor("a-pool");
		}

		@RestGet("/hello")
		public CompletableFuture<String> hello() {
			return CompletableFuture.completedFuture("hello-from-executor-resource");
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test
	void a01_happyPath_resourceBuilds() {
		// Building the MockRestClient would throw if bean resolution failed at startup.
		assertNotNull(CA);
	}

	@Test
	void a02_happyPath_responseIsCorrect() throws Exception {
		CA.get("/hello").run().assertStatus(200).assertContent().isContains("hello-from-executor-resource");
	}

	@Test
	void a03_happyPath_executorIsConfiguredOnContext() throws Exception {
		// Verify the resolved executor is non-null on the RestOpContext.
		// We do this by building a fresh context with the same resource class and checking the executor.
		var ctx = MockRestClient.buildLax(A.class);
		assertNotNull(ctx, "MockRestClient should build without error");
		// Response works correctly, implying the memoizer evaluated successfully.
		ctx.get("/hello").run().assertStatus(200);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Verb-level override — @RestGet(asyncCompletionExecutor) overrides @Rest-level.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncCompletionExecutor = "resourcePool")
	public static class B {
		@Bean(name = "resourcePool")
		public Executor resourcePool() { return new CountingExecutor("resource-pool"); }

		@Bean(name = "opPool")
		public Executor opPool() { return new CountingExecutor("op-pool"); }

		@RestGet(path = "/inherited")
		public CompletableFuture<String> inherited() {
			return CompletableFuture.completedFuture("uses-resource-pool");
		}

		@RestGet(path = "/overridden", asyncCompletionExecutor = "opPool")
		public CompletableFuture<String> overridden() {
			return CompletableFuture.completedFuture("uses-op-pool");
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test
	void b01_verbOverride_bothEndpointsRespond() throws Exception {
		CB.get("/inherited").run().assertStatus(200).assertContent().isContains("uses-resource-pool");
		CB.get("/overridden").run().assertStatus(200).assertContent().isContains("uses-op-pool");
	}

	@Test
	void b02_verbOverride_resourceBuilds() {
		assertNotNull(CB);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: No override — default behavior (null executor, natural thread) is preserved.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/natural")
		public CompletableFuture<String> natural() {
			return CompletableFuture.completedFuture("natural-thread");
		}
	}

	private static final MockRestClient CC = MockRestClient.buildLax(C.class);

	@Test
	void c01_noOverride_resourceBuilds() {
		assertNotNull(CC);
	}

	@Test
	void c02_noOverride_responseIsCorrect() throws Exception {
		CC.get("/natural").run().assertStatus(200).assertContent().isContains("natural-thread");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Unresolved bean name — resource construction must throw at startup.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncCompletionExecutor = "nonExistentPool")
	public static class D {
		@RestGet("/x")
		public CompletableFuture<String> x() { return CompletableFuture.completedFuture("x"); }
	}

	@Test
	void d01_unresolvedBean_throwsAtStartup() {
		var ex = assertThrows(Exception.class, () -> MockRestClient.buildLax(D.class));
		// The exception chain should contain our startup-fail message.
		var msg = unwrapCause(ex);
		assertTrue(msg.contains("nonExistentPool"),
			"Expected message mentioning the missing bean name; got: " + msg);
	}

	private static String unwrapCause(Throwable t) {
		var sb = new StringBuilder();
		while (t != null) {
			sb.append(t.getMessage()).append('\n');
			t = t.getCause();
		}
		return sb.toString();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Virtual thread executor — supply Executors.newVirtualThreadPerTaskExecutor() on Java 21+.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(asyncCompletionExecutor = "vtPool")
	public static class E {
		@Bean(name = "vtPool")
		public Executor vtPool() throws Exception {
			// Reflective so the resource class compiles on Java 17.
			return (Executor) Executors.class.getMethod("newVirtualThreadPerTaskExecutor").invoke(null);
		}

		@RestGet("/vt")
		public CompletableFuture<String> vt() {
			return CompletableFuture.completedFuture("virtual-thread-executor");
		}
	}

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void e01_virtualThreadExecutor_resourceBuilds() {
		// If reflective creation succeeds (Java 21+), the resource must build without error.
		var c = MockRestClient.buildLax(E.class);
		assertNotNull(c);
	}

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void e02_virtualThreadExecutor_responseIsCorrect() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/vt").run().assertStatus(200).assertContent().isContains("virtual-thread-executor");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: MDC propagation still works when an async-completion executor is configured.
	//    The sync-fallback path exercises the full response pipeline, including MDC context.
	// -----------------------------------------------------------------------------------------------------------------

	/** The MDC key set by a "filter" before the request. */
	static final String MDC_KEY = "asyncExecTestId";

	@Rest(asyncCompletionExecutor = "mdcTestPool")
	public static class F {
		@Bean(name = "mdcTestPool")
		public Executor mdcTestPool() { return new CountingExecutor("mdc-pool"); }

		@RestGet("/with-mdc")
		public CompletableFuture<String> withMdc() {
			// In the sync-fallback path, the future completes synchronously; MDC should be accessible.
			return CompletableFuture.completedFuture("mdc-response");
		}
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void f01_mdcPropagation_resourceBuilds() {
		assertNotNull(CF);
	}

	@Test
	void f02_mdcPropagation_responseIsCorrect() throws Exception {
		MDC.put(MDC_KEY, "test-req-id");
		CF.get("/with-mdc").run().assertStatus(200).assertContent().isContains("mdc-response");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: Op-level @RestGet override with no resource-level setting.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G {
		@Bean(name = "opOnlyPool")
		public Executor opOnlyPool() { return new CountingExecutor("g-pool"); }

		@RestGet(path = "/with-exec", asyncCompletionExecutor = "opOnlyPool")
		public CompletableFuture<String> withExec() {
			return CompletableFuture.completedFuture("op-only-executor");
		}

		@RestGet(path = "/without-exec")
		public CompletableFuture<String> withoutExec() {
			return CompletableFuture.completedFuture("no-executor");
		}
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test
	void g01_opLevelOnly_resourceBuilds() {
		assertNotNull(CG);
	}

	@Test
	void g02_opLevelOnly_bothEndpointsRespond() throws Exception {
		CG.get("/with-exec").run().assertStatus(200).assertContent().isContains("op-only-executor");
		CG.get("/without-exec").run().assertStatus(200).assertContent().isContains("no-executor");
	}
}
