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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Tests for runtime add/remove of child REST resources via {@link BasicRestServletGroup#addChild} and friends, which
 * delegate to {@link RestChildren}.
 *
 * <p>Each scenario uses a distinct parent class so the {@link MockRestClient}'s per-class
 * {@link RestContext} cache does not bleed mutations across tests.
 */
class BasicRestServletGroup_DynamicChildren_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Reusable child resources.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/alpha")
	public static class AlphaChild {
		@RestGet(path = "/ping")
		public String ping() {
			return "alpha-pong";
		}
	}

	@Rest(path = "/beta")
	public static class BetaChild {
		@RestGet(path = "/ping")
		public String ping() {
			return "beta-pong";
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// a01: addChild(Class<?>) — instantiates via bean store, mounts at @Rest(path).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class A_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a01_addChild_byClass() throws Exception {
		var parent = new A_Parent();
		var client = MockRestClient.createLax(parent).build();

		client.get("/alpha/ping").run().assertStatus(404);

		parent.addChild(AlphaChild.class);

		client.get("/alpha/ping").run().assertStatus(200).assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02: addChild(Object) — pre-instantiated resource.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class A02_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a02_addChild_byInstance() throws Exception {
		var parent = new A02_Parent();
		var client = MockRestClient.build(parent);
		var instance = new AlphaChild();

		var ctx = parent.addChild(instance);

		assertSame(instance, ctx.getResource());
		client.get("/alpha/ping").run().assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a03: addChild(String, Object) — explicit path override (mount same class at different path).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class A03_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void a03_addChild_withPathOverride() throws Exception {
		var parent = new A03_Parent();
		var client = MockRestClient.build(parent);

		parent.addChild(new AlphaChild());          // /alpha
		parent.addChild("/gamma", new AlphaChild()); // /gamma (override of @Rest(path="/alpha"))

		client.get("/alpha/ping").run().assertContent("alpha-pong");
		client.get("/gamma/ping").run().assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b01: removeChild(String) — 404 after removal; insertion order preserved for remaining children.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class B_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void b01_removeChild_byPath() throws Exception {
		var parent = new B_Parent();
		var client = MockRestClient.createLax(parent).build();

		parent.addChild(AlphaChild.class);
		parent.addChild(BetaChild.class);

		client.get("/alpha/ping").run().assertStatus(200);
		client.get("/beta/ping").run().assertStatus(200);

		var removed = parent.removeChild("alpha");
		assertNotNull(removed);
		assertEquals("alpha", removed.getPath());

		client.get("/alpha/ping").run().assertStatus(404);
		client.get("/beta/ping").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b02: removeChild(Class<?>) — matches by resource class.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class B02_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void b02_removeChild_byClass() throws Exception {
		var parent = new B02_Parent();
		var client = MockRestClient.createLax(parent).build();

		parent.addChild(AlphaChild.class);
		parent.addChild(BetaChild.class);

		var removed = parent.removeChild(BetaChild.class);
		assertNotNull(removed);
		assertEquals("beta", removed.getPath());

		client.get("/alpha/ping").run().assertStatus(200);
		client.get("/beta/ping").run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b03: removeChild on non-existent path/class returns null.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class B03_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void b03_removeChild_missing_returnsNull() {
		var parent = new B03_Parent();
		MockRestClient.build(parent);

		assertNull(parent.removeChild("doesnotexist"));
		assertNull(parent.removeChild(AlphaChild.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c01: Duplicate-path add throws IllegalStateException; original child stays intact.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class C_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void c01_addChild_duplicatePath_throws() throws Exception {
		var parent = new C_Parent();
		var client = MockRestClient.build(parent);

		parent.addChild(AlphaChild.class);
		assertThrows(IllegalStateException.class, () -> parent.addChild(AlphaChild.class));

		// Original child still routes correctly.
		client.get("/alpha/ping").run().assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c02: addChild(Object, replace=true) — evicts existing child at the same path.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class C02_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void c02_addChild_replace_evictsExisting() throws Exception {
		var parent = new C02_Parent();
		var client = MockRestClient.build(parent);
		var rc = parent.getChildResources();

		var original = new AlphaChild();
		rc.addChild(original);

		var replacement = new AlphaChild();
		var newCtx = rc.addChild(replacement, true);

		assertSame(replacement, newCtx.getResource());
		assertNotSame(original, newCtx.getResource());
		client.get("/alpha/ping").run().assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d01: Insertion order preserved in asMap() snapshot.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class D_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void d01_insertionOrderPreserved() throws Exception {
		var parent = new D_Parent();
		MockRestClient.build(parent);

		parent.addChild(BetaChild.class);
		parent.addChild(AlphaChild.class);

		var keys = new ArrayList<>(parent.getChildResources().asMap().keySet());
		assertEquals(List.of("beta", "alpha"), keys);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e01: @RestInit / @RestPostInit fire on dynamically added child.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class E_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path = "/lifecycle")
	public static class LifecycleChild {
		public final List<String> events = Collections.synchronizedList(new ArrayList<>());

		@RestInit
		public void init() {
			events.add("init");
		}

		@RestPostInit
		public void postInit() {
			events.add("postInit");
		}

		@RestPostInit(childFirst = true)
		public void postInitChildFirst() {
			events.add("postInitChildFirst");
		}

		@RestGet(path = "/events")
		public List<String> events() {
			return events;
		}
	}

	@Test
	void e01_lifecycleHooks_fireOnDynamicAdd() throws Exception {
		var parent = new E_Parent();
		MockRestClient.build(parent);

		var child = new LifecycleChild();
		parent.addChild(child);

		// @RestInit fires during construction; @RestPostInit hooks fire from addChild's postInit / postInitChildFirst calls.
		assertTrue(child.events.contains("init"));
		assertTrue(child.events.contains("postInit"));
		assertTrue(child.events.contains("postInitChildFirst"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e02: @RestDestroy fires on removeChild.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class E02_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path = "/destroyable")
	public static class DestroyableChild {
		public final AtomicBoolean destroyed = new AtomicBoolean(false);

		@RestDestroy
		public void onDestroy() {
			destroyed.set(true);
		}

		@RestGet(path = "/ping")
		public String ping() {
			return "ok";
		}
	}

	@Test
	void e02_destroyHook_firesOnRemove() throws Exception {
		var parent = new E02_Parent();
		MockRestClient.build(parent);

		var child = new DestroyableChild();
		parent.addChild(child);
		assertFalse(child.destroyed.get());

		parent.removeChild("destroyable");
		assertTrue(child.destroyed.get(), "@RestDestroy hook should have fired on removeChild");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f01: Concurrent add/remove + routing — sanity smoke (no CME, final state correct).
	//
	// This is a lightweight smoke test, not a true stress test — the goal is to exercise the volatile snapshot read
	// path under contention and assert that visible routes always resolve correctly.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class F_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void f01_concurrentAddRemoveAndRoute_smoke() throws Exception {
		var parent = new F_Parent();
		var client = MockRestClient.build(parent);

		// Seed with a permanent child that should always be routable.
		parent.addChild(AlphaChild.class);

		var stop = new AtomicBoolean(false);
		var errors = Collections.synchronizedList(new ArrayList<Throwable>());

		// Reader threads — hammer findMatch through MockRestClient against the permanent child.
		var readers = new ArrayList<Thread>();
		for (var i = 0; i < 4; i++) {
			readers.add(new Thread(() -> {
				try {
					while (! stop.get())
						client.get("/alpha/ping").run().assertStatus(200);
				} catch (Throwable t) {
					errors.add(t);
				}
			}));
		}

		// Writer threads — repeatedly add and remove a transient child class.
		var writers = new ArrayList<Thread>();
		for (var i = 0; i < 2; i++) {
			writers.add(new Thread(() -> {
				try {
					for (var j = 0; j < 50 && ! stop.get(); j++) {
						try {
							parent.addChild(BetaChild.class);
						} catch (IllegalStateException dup) {
							// Two writers racing on the same class is expected — ignore.
						}
						parent.removeChild(BetaChild.class);
					}
				} catch (Throwable t) {
					errors.add(t);
				}
			}));
		}

		readers.forEach(Thread::start);
		writers.forEach(Thread::start);

		for (var w : writers)
			w.join(TimeUnit.SECONDS.toMillis(15));
		stop.set(true);
		for (var r : readers)
			r.join(TimeUnit.SECONDS.toMillis(15));

		if (! errors.isEmpty())
			throw new AssertionError("Concurrent operations produced errors: " + errors.get(0), errors.get(0));

		// Final state: AlphaChild still mounted; BetaChild may or may not be present (last write wins).
		client.get("/alpha/ping").run().assertStatus(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g01: addChild(Class<?>) where the class is pre-registered as a bean — buildChildContext resolves via bean store.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class G_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		private final AlphaChild prebuiltAlpha = new AlphaChild();

		@Bean
		public AlphaChild prebuiltAlpha() {
			return prebuiltAlpha;
		}
	}

	@Test
	void g01_addChild_byClass_resolvesFromBeanStore() throws Exception {
		var parent = new G_Parent();
		var client = MockRestClient.build(parent);

		var ctx = parent.addChild(AlphaChild.class);

		assertSame(parent.prebuiltAlpha, ctx.getResource(), "Class-based addChild should resolve from bean store when a matching bean is registered.");
		client.get("/alpha/ping").run().assertContent("alpha-pong");
	}

	//------------------------------------------------------------------------------------------------------------------
	// h01: Servlet-typed child — setContext reflective invocation runs and Servlet.destroy() fires on remove.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/servletchild")
	public static class ServletChild extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		public final AtomicBoolean destroyed = new AtomicBoolean(false);

		@RestGet(path = "/ping")
		public String ping() {
			return "servlet-pong";
		}

		@Override
		public void destroy() {
			destroyed.set(true);
			super.destroy();
		}
	}

	@Rest(path = "/root")
	public static class H_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void h01_servletChild_setContextAndDestroyFire() throws Exception {
		var parent = new H_Parent();
		var client = MockRestClient.createLax(parent).build();

		var child = new ServletChild();
		parent.addChild(child);

		// setContext on the Servlet child was invoked during addChild — verify by issuing a successful request.
		// (BasicRestServlet defaults to text/html which wraps responses, so assert on a substring rather than equality.)
		client.get("/servletchild/ping").run().assertStatus(200).assertContent().asString().isContains("servlet-pong");

		parent.removeChild("servletchild");
		assertTrue(child.destroyed.get(), "Servlet.destroy() should fire when a Servlet-typed child is removed.");
		client.get("/servletchild/ping").run().assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i01: addChild(String, Object, boolean replace=true) — explicit path with replace semantics.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path = "/root")
	public static class I_Parent extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test
	void i01_addChild_explicitPath_withReplace() throws Exception {
		var parent = new I_Parent();
		var client = MockRestClient.build(parent);
		var rc = parent.getChildResources();

		var original = new AlphaChild();
		rc.addChild("/gamma", original);

		var replacement = new BetaChild();
		var newCtx = rc.addChild("/gamma", replacement, true);

		assertSame(replacement, newCtx.getResource());
		assertEquals("gamma", newCtx.getPath());
		// /gamma now routes through BetaChild even though the @Rest(path) on BetaChild is "/beta".
		client.get("/gamma/ping").run().assertContent("beta-pong");
	}
}
