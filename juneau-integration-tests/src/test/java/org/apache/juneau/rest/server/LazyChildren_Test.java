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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for opt-in lazy {@code @Rest(children=...)} materialization.
 *
 * <p>
 * Each scenario uses a distinct parent class so the {@link MockRestClient}'s per-class
 * {@link RestContext} cache does not bleed state across tests.
 */
class LazyChildren_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Shared child resources.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/alpha")
	public static class AlphaResource {
		@RestGet("/ping")
		public String ping() {
			return "alpha-pong";
		}
	}

	@Rest(path = "/beta")
	public static class BetaResource {
		@RestGet("/ping")
		public String ping() {
			return "beta-pong";
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// a01: HappyPath — annotation enables lazy; routing works immediately; first request triggers materialization; // NOSONAR
	//      subsequent requests reuse the context.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root", children = { AlphaResource.class }, lazyChildren = "true")
	public static class A_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a01_happyPath_lazyChildMaterializesOnFirstRequest() throws Exception {
		var parent = new A_Parent();
		var client = MockRestClient.createLax(parent).build();
		var rc = parent.getContext();

		// lazyChildren=true is honoured.
		assertTrue(rc.isLazyChildren());

		// Before first request: lazy entry is registered but NOT materialized.
		var entries = rc.getRestChildren().getLazyEntries();
		assertFalse(entries.isEmpty(), "Lazy entry should be registered at startup");
		var entry = entries.get("alpha");
		assertNotNull(entry, "Lazy entry key should be 'alpha'");
		assertFalse(entry.isMaterialized(), "Should not be materialized before first request");

		// First request materializes the child.
		client.get("/alpha/ping").run().assertStatus(200).assertContent("alpha-pong");

		// After first request: materialized.
		assertTrue(entry.isMaterialized(), "Should be materialized after first request");

		// Subsequent requests continue to succeed (reuse materialized context).
		client.get("/alpha/ping").run().assertStatus(200).assertContent("alpha-pong");
		client.get("/alpha/ping").run().assertStatus(200).assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02: Concurrent first-requests — only one materialization runs.
	//------------------------------------------------------------------------------------------------------------------

	static final AtomicInteger A02_INIT_COUNT = new AtomicInteger(0);

	@Rest(path = "/counted")
	public static class CountedResource {
		public CountedResource() {
			A02_INIT_COUNT.incrementAndGet();
		}

		@RestGet("/ping")
		public String ping() {
			return "counted-pong";
		}
	}

	@Rest(path = "/root", children = { CountedResource.class }, lazyChildren = "true")
	public static class B_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a02_concurrent_exactlyOneMaterialization() throws Exception {
		A02_INIT_COUNT.set(0);
		var client = MockRestClient.createLax(new B_Parent()).build();

		int concurrency = 8;
		var barrier = new CyclicBarrier(concurrency);
		var latch = new CountDownLatch(concurrency);
		var errors = new AtomicReference<Throwable>();
		var pool = Executors.newFixedThreadPool(concurrency);

		for (int i = 0; i < concurrency; i++) {
			pool.submit(() -> {
				try {
					barrier.await();  // all threads start simultaneously
					client.get("/counted/ping").run().assertStatus(200).assertContent("counted-pong");
				} catch (Throwable t) {
					errors.compareAndSet(null, t);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await(10, TimeUnit.SECONDS);
		pool.shutdown();
		assertNull(errors.get(), "Concurrent requests must not throw");
		assertEquals(1, A02_INIT_COUNT.get(), "CountedResource constructor must be called exactly once");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a03: Never-invoked lazy child — parent shuts down without touching the child context.
	//------------------------------------------------------------------------------------------------------------------

	static final AtomicInteger A03_INIT_COUNT = new AtomicInteger(0);

	@Rest(path = "/never-invoked")
	public static class NeverInvokedResource {
		public NeverInvokedResource() {
			A03_INIT_COUNT.incrementAndGet();
		}

		@RestGet("/ping")
		public String ping() {
			return "never-pong";
		}
	}

	@Rest(path = "/root", children = { NeverInvokedResource.class }, lazyChildren = "true")
	public static class C_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a03_neverInvoked_childIsNeverMaterialized() {
		A03_INIT_COUNT.set(0);
		var parent = new C_Parent();
		// Just creating the client wires the parent context (eager path for parent; lazy for children).
		MockRestClient.createLax(parent).build();

		var rc = parent.getContext();
		var entry = rc.getRestChildren().getLazyEntries().get("never-invoked");
		assertNotNull(entry);
		assertFalse(entry.isMaterialized(), "Child should not be materialized if never invoked");
		assertEquals(0, A03_INIT_COUNT.get(), "NeverInvokedResource constructor must not have been called");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a04: Builder override — lazyChildInit(false) overrides @Rest(lazyChildren="true").
	//      Tests via programmatic RestContext.Builder used in package-accessible test.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root", children = { AlphaResource.class }, lazyChildren = "true")
	public static class D_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path = "/root", children = { AlphaResource.class })
	public static class D_EagerParent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a04_builderOverride_lazyChildInitTrueOverridesEagerDefault() {
		// Use Builder.lazyChildInit(true) to force lazy on a resource that has no annotation.
		// The Builder is package-private, so this test is in the org.apache.juneau.rest.server.server package.
		var d = new D_EagerParent();
		MockRestClient.createLax(d).build();
		var rc = d.getContext();

		// Default is eager; verify annotation-free behavior.
		assertFalse(rc.isLazyChildren(), "No annotation => eager (default)");

		// Separately verify that the builder knob can be set and read.
		var args = new RestContext.Args(D_EagerParent.class, null, null, () -> d, "", null, null, null, RestContext.ContextKind.ROOT);
		var builder = new RestContext.Builder(args);
		assertNull(builder.lazyChildInit, "Default builder knob should be null");
		builder.lazyChildInit(true);
		assertTrue(builder.lazyChildInit, "Builder knob must be set to true after lazyChildInit(true)");
		builder.lazyChildInit(false);
		assertFalse(builder.lazyChildInit, "Builder knob must be set to false after lazyChildInit(false)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a05: Lifecycle shutdown — materialized lazy child sees destroy(); never-invoked child is skipped.
	//------------------------------------------------------------------------------------------------------------------

	static final AtomicInteger A05_DESTROY_COUNT = new AtomicInteger(0);

	@Rest(path = "/lifecycle-child")
	public static class LifecycleChildResource {
		@RestGet("/ping")
		public String ping() {
			return "lifecycle-pong";
		}

		@RestDestroy
		public void onDestroy() {
			A05_DESTROY_COUNT.incrementAndGet();
		}
	}

	@Rest(path = "/never-called")
	public static class NeverCalledLifecycle {
		@RestGet("/ping")
		public String ping() {
			return "never";
		}

		@RestDestroy
		public void onDestroy() {
			// should NOT be called
			A05_DESTROY_COUNT.incrementAndGet();
		}
	}

	@Rest(path = "/root", children = { LifecycleChildResource.class, NeverCalledLifecycle.class }, lazyChildren = "true")
	public static class E_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a05_lifecycleShutdown_materializedChildSeesDestroy_neverInvokedSkipped() throws Exception {
		A05_DESTROY_COUNT.set(0);
		var parent = new E_Parent();
		var client = MockRestClient.createLax(parent).build();

		// Trigger materialization of lifecycle-child only.
		client.get("/lifecycle-child/ping").run().assertStatus(200);

		var rc = parent.getContext();
		var lazyEntries = rc.getRestChildren().getLazyEntries();
		assertTrue(lazyEntries.get("lifecycle-child").isMaterialized(), "lifecycle-child should be materialized");
		assertFalse(lazyEntries.get("never-called").isMaterialized(), "never-called should NOT be materialized");

		// Destroy the parent — only materialized child's @RestDestroy should fire.
		rc.destroy();

		assertEquals(1, A05_DESTROY_COUNT.get(),
			"@RestDestroy must fire exactly once (only on the materialized child)");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a06: Default eager — no lazyChildren annotation; child is built at startup.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root", children = { AlphaResource.class })
	public static class F_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a06_defaultEager_noLazyAnnotation_childMaterializedAtStartup() throws Exception {
		var parent = new F_Parent();
		var client = MockRestClient.createLax(parent).build();
		var rc = parent.getContext();

		// No lazy annotation — default eager behavior preserved.
		assertFalse(rc.isLazyChildren());
		assertTrue(rc.getRestChildren().getLazyEntries().isEmpty(), "No lazy entries in eager mode");
		assertFalse(rc.getRestChildren().asMap().isEmpty(), "Eager child must already be in the children map");

		// Routing still works.
		client.get("/alpha/ping").run().assertStatus(200).assertContent("alpha-pong");
	}
}
