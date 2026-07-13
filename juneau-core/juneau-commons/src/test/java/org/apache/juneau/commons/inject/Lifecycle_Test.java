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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Lifecycle integration tests for {@code @PostConstruct} / {@code @PreDestroy}
 * and {@link BasicBeanStore#close()}.
 *
 * <p>Covers:
 * <ul>
 * 	<li>{@code @PostConstruct} invocation via {@link BeanInstantiator}.
 * 	<li>{@code @PreDestroy} invocation on {@link BasicBeanStore#close()} in LIFO order.
 * 	<li>{@link BasicBeanStore#close()} idempotency.
 * 	<li>Closed bean store rejects subsequent operations.
 * 	<li>Suppressed-exception aggregation when multiple destroyers throw.
 * </ul>
 */
@SuppressWarnings({
	"java:S2094", // Intentionally empty bean class used as test fixture.
	"resource",   // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
	"unused"      // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class Lifecycle_Test extends TestBase {

	//------------------------------------------------------------------------------------------------
	// Fixtures.
	//------------------------------------------------------------------------------------------------

	public static class PostConstructBean {
		public boolean postConstructCalled;
		public PostConstructBean() { /* intentionally empty */ }
		@PostConstruct public void init() { postConstructCalled = true; }
	}

	public static class TracedBean {
		public final String name;
		public final List<String> trace;
		public TracedBean(String name, List<String> trace) { this.name = name; this.trace = trace; }
		@PreDestroy public void destroy() { trace.add("destroy:" + name); }
	}

	public static class FailingDestroyBean {
		public final String name;
		public final List<String> trace;
		public FailingDestroyBean(String name, List<String> trace) { this.name = name; this.trace = trace; }
		@PreDestroy public void destroy() {
			trace.add("attempt:" + name);
			throw new RuntimeException("boom:" + name);
		}
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @PostConstruct.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_postConstruct_isInvokedOnInstantiation() {
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(PostConstructBean.class, store).run();
		assertTrue(bean.postConstructCalled, "@PostConstruct must run after instantiation");
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @PreDestroy via close().
	//------------------------------------------------------------------------------------------------

	@Test
	void b01_preDestroy_invokedInLifoOrder() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		var first = new TracedBean("first", trace);
		var second = new TracedBean("second", trace);
		store.addBean(TracedBean.class, first, "first");
		store.addBean(TracedBean.class, second, "second");

		// Resolve in order: first, second.  close() must call them in LIFO order.
		assertNotNull(store.getBean(TracedBean.class, "first").orElse(null));
		assertNotNull(store.getBean(TracedBean.class, "second").orElse(null));

		store.close();

		assertEquals(List.of("destroy:second", "destroy:first"), trace);
	}

	@Test
	void b02_close_isIdempotent() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		store.addBean(TracedBean.class, new TracedBean("only", trace));
		store.getBean(TracedBean.class);
		store.close();
		// Second close is a no-op (no additional invocations).
		store.close();
		assertEquals(1L, trace.stream().filter(s -> s.startsWith("destroy:")).count());
	}

	@Test
	void b03_closedStore_rejectsSubsequentOperations() {
		var store = new BasicBeanStore(null);
		store.close();
		assertThrows(IllegalStateException.class, () -> store.addBean(TracedBean.class, null));
		assertThrows(IllegalStateException.class, () -> store.getBean(TracedBean.class));
		assertThrows(IllegalStateException.class, store::clear);
	}

	@Test
	void b04_failingDestroyers_aggregateSuppressed() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		store.addBean(FailingDestroyBean.class, new FailingDestroyBean("a", trace), "a");
		store.addBean(FailingDestroyBean.class, new FailingDestroyBean("b", trace), "b");
		store.getBean(FailingDestroyBean.class, "a");
		store.getBean(FailingDestroyBean.class, "b");

		var ex = assertThrows(BeanCreationException.class, store::close);
		// All destroyers attempted even though both threw.
		assertEquals(2, trace.size());
		// Both failures are captured as suppressed exceptions.
		assertTrue(ex.getSuppressed().length >= 2,
			"Expected at least 2 suppressed exceptions, got " + ex.getSuppressed().length);
	}

	@Test
	void b05_unresolvedBeans_areNotDestroyed() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		// Registered but never resolved via getBean — its @PreDestroy must NOT fire.
		store.addBean(TracedBean.class, new TracedBean("never-fetched", trace));
		store.close();
		assertEquals(List.of(), trace);
	}

	//------------------------------------------------------------------------------------------------
	// Tests - @PreDestroy method-signature filtering.
	//
	// BasicBeanStore.close() walks every method on each resolved bean and only invokes those that
	// (1) return void, (2) have zero parameters, (3) are concrete, and (4) are non-static, AND
	// (5) carry a recognized @PreDestroy annotation.  The tests below verify each negative filter so
	// no foot-gun "@PreDestroy" annotated helper ever runs by accident.
	//------------------------------------------------------------------------------------------------

	public static class MalformedPreDestroyBean {
		public final List<String> trace;
		public MalformedPreDestroyBean(List<String> trace) { this.trace = trace; }
		// Valid @PreDestroy — should fire.
		@PreDestroy public void destroy() { trace.add("destroy"); }
		// Non-void return — must be skipped.
		@PreDestroy public String destroyReturning() { trace.add("destroyReturning"); return ""; }
		// Has parameters — must be skipped.
		@PreDestroy public void destroyWithArg(String s) { trace.add("destroyWithArg"); }
		// Static — must be skipped.
		@PreDestroy public static void destroyStatic() { /* must not fire */ }
		// Non-@PreDestroy method — must never be called from close().
		public void unrelated() { trace.add("unrelated"); }
	}

	@Test
	void b06_preDestroy_filtersOutMalformedMethods() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		store.addBean(MalformedPreDestroyBean.class, new MalformedPreDestroyBean(trace));
		store.getBean(MalformedPreDestroyBean.class);

		store.close();

		// Only the valid @PreDestroy method fires.
		assertEquals(List.of("destroy"), trace,
			"Only void / zero-arg / non-static / @PreDestroy-annotated methods should fire");
	}

	// Abstract parent declares an abstract @PreDestroy method.  Although Java guarantees the concrete
	// subclass overrides it, ClassInfo.getAllMethods() still surfaces the abstract declaration when
	// walking the type hierarchy — driving the {@code method.isAbstract()} skip branch in
	// {@code BasicBeanStore.close()}.
	public abstract static class AbstractDestroyer {
		public AbstractDestroyer() {}
		@PreDestroy public abstract void destroy();
	}

	public static class ConcreteDestroyer extends AbstractDestroyer {
		public final List<String> trace;
		public ConcreteDestroyer(List<String> trace) { this.trace = trace; }
		@Override public void destroy() { trace.add("concrete-destroy"); }
	}

	@Test
	void b07_preDestroy_skipsAbstractDeclaration() {
		var trace = new CopyOnWriteArrayList<String>();
		var store = new BasicBeanStore(null);
		store.addBean(ConcreteDestroyer.class, new ConcreteDestroyer(trace));
		store.getBean(ConcreteDestroyer.class);

		store.close();

		assertEquals(List.of("concrete-destroy"), trace,
			"Abstract @PreDestroy declarations must be skipped; concrete override must fire exactly once");
	}
}
