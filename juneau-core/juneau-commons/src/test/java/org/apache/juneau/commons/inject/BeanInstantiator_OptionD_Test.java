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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for {@link BeanInstantiator}'s stricter builder-candidate selection.
 *
 * <p>
 * A builder candidate whose {@code build()} only promises a <b>supertype</b> of the requested bean type (e.g.
 * an inherited generic self-typed base builder, or one reached via a {@code Class}-parameterized factory) must
 * not displace a stricter instantiation path — a usable direct constructor, or a builder that builds the exact
 * requested type. These tests verify: (a) an exact-typed builder still wins; (b) a parent-only builder loses to
 * a direct constructor; (c) a {@code Class}-parameterized parent-only factory loses to a direct constructor;
 * (d) a subtype's own exact builder still wins even when a constructor exists; and (e) plain POJOs are
 * unaffected.
 */
@SuppressWarnings({
	"java:S2094", // Intentionally empty helper bean.
	"resource"    // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class BeanInstantiator_OptionD_Test extends TestBase {

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
		A_Base.builderUsed = false;
		C_Base.factoryUsed = false;
	}

	private <T> T run(Class<T> c) {
		return BeanInstantiator.of(c, beanStore).run();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — exact-typed builder still wins; parent-only builder loses to a direct constructor.
	//-----------------------------------------------------------------------------------------------------------------

	public static class A_Base {
		static boolean builderUsed;
		public static Builder create() { return new Builder(); }
		public A_Base() { /* intentionally empty */ }
		public static class Builder {
			public A_Base build() { builderUsed = true; return new A_Base(); }
		}
	}

	// Subtype with a usable no-arg constructor and NO builder of its own; it only inherits A_Base.create(),
	// whose build() returns the supertype A_Base.
	public static class A_Sub extends A_Base {
		public boolean viaCtor;
		public A_Sub() { this.viaCtor = true; }
	}

	@Test
	void a01_exactTypedBuilderWins() {
		var bean = run(A_Base.class);
		assertNotNull(bean);
		assertTrue(A_Base.builderUsed, "Exact-typed builder should have been used for A_Base.");
	}

	@Test
	void a02_parentOnlyBuilderLosesToDirectConstructor() {
		var bean = run(A_Sub.class);
		assertNotNull(bean);
		assertInstanceOf(A_Sub.class, bean);
		assertTrue(bean.viaCtor, "A_Sub should be built via its direct constructor.");
		assertFalse(A_Base.builderUsed, "The supertype-only builder must not be used when a direct constructor exists.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — Class-parameterized parent-only factory loses to a direct constructor (mirrors the REST builder(Class) shape).
	//-----------------------------------------------------------------------------------------------------------------

	public static class C_Base {
		static boolean factoryUsed;
		public static <R extends C_Base> DefaultBuilder<R> builder(Class<R> type) { return new DefaultBuilder<>(type); }
		public C_Base() { /* intentionally empty */ }
		public static class DefaultBuilder<R extends C_Base> {
			private final Class<R> type;
			DefaultBuilder(Class<R> type) { this.type = type; }
			public R build() {
				factoryUsed = true;
				try { return type.getDeclaredConstructor().newInstance(); }
				catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
			}
		}
	}

	public static class C_Sub extends C_Base {
		public boolean viaCtor;
		public C_Sub() { this.viaCtor = true; }
	}

	@Test
	void b01_classParameterizedParentOnlyFactoryLosesToCtor() {
		// Make the Class parameter resolvable so that, absent the Option-D rule, the weak factory would be selected.
		beanStore.add(Class.class, C_Sub.class);
		var bean = run(C_Sub.class);
		assertNotNull(bean);
		assertInstanceOf(C_Sub.class, bean);
		assertTrue(bean.viaCtor, "C_Sub should be built via its direct constructor.");
		assertFalse(C_Base.factoryUsed, "The Class-parameterized supertype-only factory must not displace the constructor.");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — a subtype's OWN exact builder still wins even when a constructor exists (gate only fires for weak builders).
	//-----------------------------------------------------------------------------------------------------------------

	public static class D_Base {
		public D_Base() { /* intentionally empty */ }
	}

	public static class D_Sub extends D_Base {
		boolean viaBuilder;
		boolean viaCtor;
		public static Builder create() { return new Builder(); }
		public D_Sub() { this.viaCtor = true; }
		D_Sub(boolean fromBuilder) { this.viaBuilder = fromBuilder; }
		public static class Builder {
			public D_Sub build() { return new D_Sub(true); }
		}
	}

	@Test
	void c01_subtypeExactBuilderWinsOverConstructor() {
		var bean = run(D_Sub.class);
		assertNotNull(bean);
		assertTrue(bean.viaBuilder, "D_Sub's own exact-typed builder should win even though a constructor exists.");
		assertFalse(bean.viaCtor);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — plain POJO with no builder is unaffected (constructor path).
	//-----------------------------------------------------------------------------------------------------------------

	public static class E_Pojo {
		public boolean viaCtor;
		public E_Pojo() { this.viaCtor = true; }
	}

	@Test
	void d01_plainPojoUsesConstructor() {
		var bean = run(E_Pojo.class);
		assertNotNull(bean);
		assertTrue(bean.viaCtor);
	}
}
