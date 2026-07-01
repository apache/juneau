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
package org.apache.juneau.test.junit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link TestBeanStore}.
 *
 * <p>
 * Validates the overlay-builder side of the test-time bean injection
 * mechanism.  The framework wiring (Args.overridingParent, MockRestClient.overridingBeanStore)
 * is exercised by sibling tests; this test concentrates on the {@link TestBeanStore} surface.
 */
@SuppressWarnings("resource")  // TestBeanStore instances are short-lived in-memory test fixtures; closing is irrelevant to these assertions.
class TestBeanStore_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final class Svc {
		final String tag;
		Svc(String tag) { this.tag = tag; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — basic overlay semantics
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_override_unnamedBean_lookupReturnsOverride() {
		var svc = new Svc("override");
		var store = new TestBeanStore().override(Svc.class, svc);
		assertSame(svc, store.getBean(Svc.class).get());
	}

	@Test
	void a02_override_returnsSelf_forFluentChaining() {
		var store = new TestBeanStore();
		var ret = store.override(Svc.class, new Svc("a"));
		assertSame(store, ret);
	}

	@Test
	void a03_override_isInstanceOf_BasicBeanStore() {
		var store = new TestBeanStore();
		assertTrue(store instanceof BasicBeanStore);
		assertTrue(store instanceof WritableBeanStore);
	}

	@Test
	void a04_emptyStore_lookupReturnsEmpty() {
		var store = new TestBeanStore();
		assertTrue(store.getBean(Svc.class).isEmpty());
		assertTrue(store.getBean(Svc.class, "named").isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — named-bean overrides
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_override_namedBean_isolatedFromUnnamed() {
		var unnamed = new Svc("unnamed");
		var primary = new Svc("primary");
		var secondary = new Svc("secondary");

		var store = new TestBeanStore()
			.override(Svc.class, unnamed)
			.override(Svc.class, primary, "primary")
			.override(Svc.class, secondary, "secondary");

		assertSame(unnamed, store.getBean(Svc.class).get());
		assertSame(primary, store.getBean(Svc.class, "primary").get());
		assertSame(secondary, store.getBean(Svc.class, "secondary").get());
	}

	@Test
	void b02_override_sameName_lastWins() {
		var first = new Svc("first");
		var second = new Svc("second");
		var store = new TestBeanStore()
			.override(Svc.class, first, "p")
			.override(Svc.class, second, "p");
		assertSame(second, store.getBean(Svc.class, "p").get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — supplier-based overrides
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S125"  // Explanatory prose on overload-resolution; not dead code.
	})
	@Test
	void c01_supplier_invokedLazily() {
		var calls = new AtomicInteger();
		var produced = new Svc("produced");
		// Pre-typed supplier disambiguates between override(Class<T>, T) and override(Class<T>, Supplier<T>);
		// Java's overload resolver carries the instance-form candidate forward when the lambda itself
		// is the second argument, even though Svc is not a functional interface.
		Supplier<Svc> supplier = () -> {
			calls.incrementAndGet();
			return produced;
		};
		var store = new TestBeanStore().override(Svc.class, supplier);

		assertEquals(0, calls.get(), "Supplier should not have fired before lookup");

		assertSame(produced, store.getBean(Svc.class).get());
		assertEquals(1, calls.get(), "Supplier should have fired exactly once on first lookup");
	}

	@Test
	void c02_supplier_namedVariant() {
		var produced = new Svc("named-supplier");
		Supplier<Svc> supplier = () -> produced;
		var store = new TestBeanStore().override(Svc.class, supplier, "n1");
		assertSame(produced, store.getBean(Svc.class, "n1").get());
		assertTrue(store.getBean(Svc.class).isEmpty(), "Unnamed lookup should not see the named override");
	}

	@Test
	void c03_supplier_canReturnDifferentInstancesEachInvocation() {
		// addSupplier is by spec re-invoked per lookup; verifies wiring through to BasicBeanStore.
		var counter = new AtomicInteger();
		Supplier<Svc> supplier = () -> new Svc("call-" + counter.incrementAndGet());
		var store = new TestBeanStore().override(Svc.class, supplier);

		var a = store.getBean(Svc.class).get();
		var b = store.getBean(Svc.class).get();
		assertEquals("call-1", a.tag);
		assertEquals("call-2", b.tag);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — parent-precedence: TestBeanStore as overridingParent shadows production @Bean entries
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_asOverridingParent_shadowsProductionEntries() {
		// Simulate the production shape: a BasicBeanStore configured with @Bean factory entries
		// (tier 2) and a TestBeanStore in the overridingParent slot (tier 1).
		var fromBeanFactory = new Svc("from-bean-factory");
		var fromTestOverride = new Svc("from-test-override");

		var overlay = new TestBeanStore().override(Svc.class, fromTestOverride);
		var production = new BasicBeanStore(null, overlay).addBean(Svc.class, fromBeanFactory);

		assertSame(fromTestOverride, production.getBean(Svc.class).get(),
			"TestBeanStore in overridingParent slot should shadow local @Bean entries");
	}

	@Test
	void d02_asOverridingParent_namedBeanTakesPrecedence() {
		var fromBeanFactory = new Svc("from-bean-factory");
		var fromTestOverride = new Svc("from-test-override");

		var overlay = new TestBeanStore().override(Svc.class, fromTestOverride, "primary");
		var production = new BasicBeanStore(null, overlay).addBean(Svc.class, fromBeanFactory, "primary");

		assertSame(fromTestOverride, production.getBean(Svc.class, "primary").get());
	}

	@Test
	void d03_asOverridingParent_supplierShadowsProductionInstance() {
		var fromBeanFactory = new Svc("from-bean-factory");
		var fromSupplier = new Svc("from-supplier");

		Supplier<Svc> supplier = () -> fromSupplier;
		var overlay = new TestBeanStore().override(Svc.class, supplier);
		var production = new BasicBeanStore(null, overlay).addBean(Svc.class, fromBeanFactory);

		assertSame(fromSupplier, production.getBean(Svc.class).get());
	}

	@Test
	void d04_asOverridingParent_emptyOverlay_doesNotShadow() {
		var fromBeanFactory = new Svc("from-bean-factory");
		var overlay = new TestBeanStore();
		var production = new BasicBeanStore(null, overlay).addBean(Svc.class, fromBeanFactory);

		assertSame(fromBeanFactory, production.getBean(Svc.class).get(),
			"Empty TestBeanStore in overridingParent slot must let production entries resolve");
	}

	@Test
	void d05_asOverridingParent_partialOverlay_partialShadow() {
		var localOnly = new Svc("local-only");
		var overlaid = new Svc("overlaid");
		var fromOverlay = new Svc("from-overlay");

		var overlay = new TestBeanStore().override(Svc.class, fromOverlay, "shared");
		var production = new BasicBeanStore(null, overlay)
			.addBean(Svc.class, localOnly, "local-only")
			.addBean(Svc.class, overlaid, "shared");

		assertSame(localOnly, production.getBean(Svc.class, "local-only").get(),
			"Names not in overlay should resolve from local entries");
		assertSame(fromOverlay, production.getBean(Svc.class, "shared").get(),
			"Names in overlay should be shadowed by overlay");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — null-safety
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_override_nullBean_isAllowed() {
		// addBean tolerates null beans (stores a () -> null supplier).  TestBeanStore inherits this.
		Svc nullBean = null;
		var store = new TestBeanStore().override(Svc.class, nullBean);
		assertTrue(store.hasBean(Svc.class));
		assertNull(store.getBean(Svc.class).orElse(null));
	}
}
