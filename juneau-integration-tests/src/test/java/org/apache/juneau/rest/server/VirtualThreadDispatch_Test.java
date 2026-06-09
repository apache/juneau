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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.JRE.*;

import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Tests virtual-thread dispatch — opt-in via
 * {@link Rest#virtualThreads() @Rest(virtualThreads=true)} or {@link RestOp#virtualThreads()}, with
 * graceful degradation on Java 17/18/19/20 (one-shot {@code WARNING} log + caller-thread fallback).
 *
 * <p>
 * Tests in section <b>A</b> are guarded by {@link DisabledOnJre} for any JVM older than 21 — the
 * underlying API ({@code Thread.currentThread().isVirtual()}) doesn't exist before then. Tests in
 * section <b>B</b> run on every JVM and verify the graceful-degradation contract: enabling the flag
 * on a non-supporting JVM must not break the resource and must emit a one-shot warning.
 *
 * <h5 class='section'>Java 17 reflective compile-time strategy</h5>
 * <p>
 * The virtual-thread executor in {@link RestContext} is built reflectively
 * ({@code Executors.class.getMethod("newVirtualThreadPerTaskExecutor")}) so the framework compiles
 * cleanly on Java 17 even though the API only exists at runtime on Java 21+. The runtime check
 * ({@code Runtime.version().feature() &gt;= 21}) gates the reflective lookup; on Java 17 we never
 * touch the missing method and the {@code Executor} memoizer returns {@code null} after logging the
 * warning. {@link RestOpInvoker} then falls through to the standard caller-thread dispatch path —
 * exactly the existing behavior.
 */
@SuppressWarnings({
	"java:S4144" // Per-fixture handlers share the same thread-introspection body but probe distinct virtual-thread scenarios across separate @Rest resources.
})
class VirtualThreadDispatch_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: Java 21+ — @Rest(virtualThreads=true) actually dispatches handler invocation on a virtual thread.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(virtualThreads = "true", serializers = JsonSerializer.class)
	public static class A {
		@RestGet("/which")
		public ThreadInfo which() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}

		@RestGet("/asyncWhich")
		public CompletableFuture<ThreadInfo> asyncWhich() {
			Thread t = Thread.currentThread();
			return CompletableFuture.completedFuture(new ThreadInfo(t.getName(), reflectIsVirtual(t)));
		}
	}

	/**
	 * Reflective {@code Thread.isVirtual()} so this test class compiles on the project's Java 17 source level.
	 * On Java < 21 the method does not exist and we return {@code false} — which is also the correct behavior
	 * since virtual threads cannot exist on those JVMs.
	 */
	static boolean reflectIsVirtual(Thread t) {
		try {
			var m = Thread.class.getMethod("isVirtual");
			return (Boolean) m.invoke(t);
		} catch (Exception e) {
			return false;
		}
	}

	public static final class ThreadInfo {
		public final String threadName;
		public final boolean virtual;
		public ThreadInfo(String threadName, boolean virtual) { this.threadName = threadName; this.virtual = virtual; }
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void a01_java21Plus_virtualThreadDispatch() throws Exception {
		CA.get("/which").run().assertStatus(200).assertContent().isContains("\"virtual\":true");
	}

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void a02_java21Plus_virtualThreadDispatch_combinedWithCompletableFuture() throws Exception {
		// The handler runs on a virtual thread, returns a CompletableFuture, and the response comes back unwrapped.
		// This is the high-throughput pattern: VT + CompletableFuture together.
		CA.get("/asyncWhich").run().assertStatus(200).assertContent().isContains("\"virtual\":true");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Graceful degradation on Java 17/18/19/20 — flag is logged + ignored, handlers still work.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(virtualThreads = "true", serializers = JsonSerializer.class)
	public static class B {
		@RestGet("/ok")
		public String ok() { return "ok"; }
	}

	@Test void b01_degradation_handlerStillWorks() throws Exception {
		// Whether we're on Java 17 (graceful degradation) or Java 21 (real VT), the handler must respond.
		var c = MockRestClient.buildLax(B.class);
		c.get("/ok").run().assertStatus(200).assertContent().isContains("ok");
	}

	@Rest(virtualThreads = "true", serializers = JsonSerializer.class)
	public static class BWarning {
		@RestGet("/x")
		public String x() { return "x"; }
	}

	@Test
	@DisabledForJreRange(min = JAVA_21)
	void b02_java17_logsWarningOnce() throws Exception {
		// Capture the WARNING emitted by RestContext when @Rest(virtualThreads=true) is configured on Java < 21.
		var captured = new StringBuilder();
		var logger = Logger.getLogger(RestContext.class.getName() + ".async");
		var handler = new Handler() {
			@Override public void publish(LogRecord r) {
				if (r.getLevel() == Level.WARNING)
					captured.append(r.getMessage());
			}
			@Override public void flush() { /* intentionally empty */ }
			@Override public void close() { /* intentionally empty */ }
		};
		logger.addHandler(handler);
		try {
			MockRestClient.buildLax(BWarning.class).get("/x").run().assertStatus(200);
			assertTrue(captured.toString().contains("virtual-thread") || captured.toString().contains("virtualThreads"),
				"expected warning about virtual threads on Java <21, captured: " + captured);
		} finally {
			logger.removeHandler(handler);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Per-op virtualThreads override — @RestOp(virtualThreads="false") opts out of resource-level on.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(virtualThreads = "true", serializers = JsonSerializer.class)
	public static class C {
		@RestOp(method = "GET", path = "/optedIn")
		public ThreadInfo optedIn() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}

		@RestOp(method = "GET", path = "/optedOut", virtualThreads = "false")
		public ThreadInfo optedOut() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}
	}

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void c01_perOpOptOutOverridesResourceLevel() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/optedIn").run().assertStatus(200).assertContent().isContains("\"virtual\":true");
		c.get("/optedOut").run().assertStatus(200).assertContent().isContains("\"virtual\":false");
	}

	@Test
	@EnabledForJreRange(min = JAVA_21)
	void c02_perOpOptInWithoutResourceLevel() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.get("/onlyThisOne").run().assertStatus(200).assertContent().isContains("\"virtual\":true");
		d.get("/notThisOne").run().assertStatus(200).assertContent().isContains("\"virtual\":false");
	}

	@Rest(serializers = JsonSerializer.class)
	public static class D {
		@RestOp(method = "GET", path = "/onlyThisOne", virtualThreads = "true")
		public ThreadInfo only() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}

		@RestOp(method = "GET", path = "/notThisOne")
		public ThreadInfo notMe() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Default off — no annotation means no virtual-thread dispatch even on Java 21+.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class)
	public static class E {
		@RestGet("/default")
		public ThreadInfo def() {
			Thread t = Thread.currentThread();
			return new ThreadInfo(t.getName(), reflectIsVirtual(t));
		}
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void d01_offByDefault() throws Exception {
		// On any JVM, the unannotated handler runs on the caller (request) thread — never virtual.
		// (On Java 17, isVirtual() is always false; on Java 21+ this verifies the off-by-default contract.)
		CE.get("/default").run().assertStatus(200).assertContent().isContains("\"virtual\":false");
	}
}
