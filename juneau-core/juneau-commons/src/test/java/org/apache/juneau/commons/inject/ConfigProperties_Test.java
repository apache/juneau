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
import java.util.function.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for the {@link ConfigProperties @ConfigProperties} annotation's resolution hook in
 * {@link BeanInstantiator}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ConfigProperties_Test extends TestBase {

	// Global overrides set by individual tests below, unset unconditionally here so a failing assertion
	// mid-test can't skip cleanup and leak state into later tests (see PropertyVars_Test / Value_SupplierFieldType_Test).
	@AfterEach
	void cleanup() {
		var settings = Settings.get();
		settings.unsetGlobal("MyServiceA01.host");
		settings.unsetGlobal("MyServiceA01.port");
		settings.unsetGlobal("MyServiceB01.host");
		settings.unsetGlobal("MyServiceB01.label");
		settings.unsetGlobal("MyServiceC01.host");
		settings.unsetGlobal("MyServiceI01.host");
	}

	// =================================================================================
	// A. BeanInstantiator resolution hook + BeanStore auto-registration
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceA01")
	public static class A01_Bean {
		public String host = "localhost";
		public int port = 8080;
	}

	@Test void a01_annotatedBeanIsBoundDuringInstantiation() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceA01.host", "example.com");
		settings.setGlobal("MyServiceA01.port", "9090");
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(A01_Bean.class, store).run();
		assertEquals("example.com", bean.host);
		assertEquals(9090, bean.port);
	}

	@Test void a02_boundInstanceIsAutoRegisteredInBeanStore() {
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(A01_Bean.class, store).run();
		assertSame(bean, store.getBean(A01_Bean.class).orElse(null));
	}

	// =================================================================================
	// B. @ConfigProperties and @Value coexist on the same class
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceB01")
	public static class B01_Bean {
		public String host = "localhost";

		// Deliberately resolves a DIFFERENT underlying key than the binder's implicit "MyServiceB01.label"
		// candidate (prefix + field name) — see b01 below: overriding that candidate key only reaches this
		// field if the binder incorrectly treats a @Value-owned field as bindable.
		@Value("${MyServiceB01.actualLabelKey:default-label}")
		public String label;
	}

	@Test void b01_configPropertiesAndValueCoexistOnSameClass() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceB01.host", "example.com");
		// Sets the config key the binder WOULD bind "label" from (prefix + field name) were it to incorrectly
		// treat this @Value-owned field as bindable. A correct binder skips it (isBindable()), so @Value's own,
		// differently-keyed expression still resolves to its default and the field is left untouched end-to-end.
		settings.setGlobal("MyServiceB01.label", "config-should-not-win");
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(B01_Bean.class, store).run();
		assertEquals("example.com", bean.host); // via ConfigPropertiesBinder
		assertEquals("default-label", bean.label); // via @Value, independently resolved; NOT clobbered by the config bind
	}

	// =================================================================================
	// C. Binding runs before @PostConstruct
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceC01")
	public static class C01_Bean {
		public String host = "localhost";
		public String observedInPostConstruct = "unset";

		@PostConstruct
		public void init() {
			observedInPostConstruct = host;
		}
	}

	@Test void c01_postConstructObservesBoundConfigField() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceC01.host", "example.com");
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(C01_Bean.class, store).run();
		assertEquals("example.com", bean.host);
		assertEquals("example.com", bean.observedInPostConstruct); // @PostConstruct saw the bound value
	}

	// =================================================================================
	// D. Non-clobbering registration — caller's explicit registration wins
	// =================================================================================

	@Test void d01_callerExplicitRegistrationWinsOverAutoRegistration() {
		var store = new BasicBeanStore(null);
		var caller = new A01_Bean();
		store.addBean(A01_Bean.class, caller); // explicit tier-2 registration
		BeanInstantiator.of(A01_Bean.class, store).run(); // auto-registers only as a tier-4 default supplier
		assertSame(caller, store.getBean(A01_Bean.class).orElse(null));
	}

	// =================================================================================
	// E. Not inherited — a subclass of an annotated type is not bound/registered
	// =================================================================================

	public static class E01_Sub extends A01_Bean {}

	@Test void e01_subclassOfAnnotatedTypeIsNotBoundOrRegistered() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceA01.host", "example.com");
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(E01_Sub.class, store).run();
		assertEquals("localhost", bean.host); // NOT bound: @ConfigProperties is not inherited
		assertFalse(store.getBean(E01_Sub.class).isPresent()); // NOT auto-registered
		assertFalse(store.getBean(A01_Bean.class).isPresent()); // and not registered under the parent type either
	}

	// =================================================================================
	// F. Null / read-only parent store still binds, skips registration
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceA01")
	public static class F01_Bean {
		public String host = "localhost";
	}

	@Test void f01_nullParentStore_stillBinds() {
		// No store to inspect for skipped registration here (that half is pinned by f02); this test only
		// asserts that binding still happens with no parent store at all.
		var settings = Settings.get();
		settings.setGlobal("MyServiceA01.host", "example.com");
		var bean = BeanInstantiator.of(F01_Bean.class).run(); // no parent store
		assertEquals("example.com", bean.host); // binding still happens
	}

	/**
	 * Minimal read-only {@link BeanStore} facade used to prove that a non-{@link WritableBeanStore} parent store
	 * still allows binding while auto-registration is silently skipped.
	 */
	static final class F02_ReadOnlyStore implements BeanStore {
		private final BeanStore delegate;

		F02_ReadOnlyStore(BeanStore delegate) {
			this.delegate = delegate;
		}

		@Override public <T> Optional<T> getBean(Class<T> beanType) { return delegate.getBean(beanType); }
		@Override public <T> Optional<T> getBean(Class<T> beanType, String name) { return delegate.getBean(beanType, name); }
		@Override public <T> Map<String,T> getBeansOfType(Class<T> beanType) { return delegate.getBeansOfType(beanType); }
		@Override public boolean hasBean(Class<?> beanType) { return delegate.hasBean(beanType); }
		@Override public boolean hasBean(Class<?> beanType, String name) { return delegate.hasBean(beanType, name); }
		@Override public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType) { return delegate.getBeanSupplier(beanType); }
		@Override public <T> Optional<Supplier<T>> getBeanSupplier(Class<T> beanType, String name) { return delegate.getBeanSupplier(beanType, name); }
	}

	@Test void f02_readOnlyParentStoreStillBindsAndSkipsRegistration() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceA01.host", "example.com");
		var delegate = new BasicBeanStore(null);
		var readOnly = new F02_ReadOnlyStore(delegate);
		var bean = BeanInstantiator.of(F01_Bean.class, readOnly).run();
		assertEquals("example.com", bean.host); // binds even though the parent store is not writable
		assertFalse(delegate.getBean(F01_Bean.class).isPresent()); // and auto-registration into the wrapped delegate is skipped
	}

	// =================================================================================
	// G. Never writes into the shared BasicBeanStore.INSTANCE
	// =================================================================================

	@Test void g01_sharedInstanceParentStoreIsNeverWrittenTo() {
		var settings = Settings.get();
		settings.setGlobal("MyServiceA01.host", "example.com");
		var bean = BeanInstantiator.of(F01_Bean.class, BasicBeanStore.INSTANCE).run();
		assertEquals("example.com", bean.host); // still binds
		assertFalse(BasicBeanStore.INSTANCE.getBean(F01_Bean.class).isPresent()); // shared INSTANCE untouched
	}

	// =================================================================================
	// H. Caller-scoped PropertySource beans in the parent store participate
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceH01")
	public static class H01_Bean {
		public String host = "localhost";
	}

	@Test void h01_scopedPropertySourceInParentStoreParticipatesAheadOfGlobal() {
		var store = new BasicBeanStore(null);
		store.addBean(PropertySource.class, name -> "MyServiceH01.host".equals(name)
			? PropertyLookupResult.present(Optional.of("scoped.example.com"))
			: PropertyLookupResult.missing());
		var bean = BeanInstantiator.of(H01_Bean.class, store).run();
		assertEquals("scoped.example.com", bean.host); // scoped source (via the internal store's parent walk) participated
	}

	// =================================================================================
	// I. of(Base).type(Sub) — declared-type semantics for the ConfigProperties hook
	// =================================================================================

	@ConfigProperties(prefix = "MyServiceI01")
	public static class I01_Base {
		public String host = "localhost";
	}

	public static class I01_Sub extends I01_Base {}

	@Test void i01_subtypeViaTypeStillBindsAndRegistersUnderDeclaredBaseType() {
		// Pins the actual declared-type semantics of the hook when combined with .type(...): the "declared
		// target type" used for both the @ConfigProperties lookup and auto-registration is the type passed to
		// of(...) (the base), not the .type(...) subtype — the mirror image of e01's not-inherited case, where
		// a subclass passed directly to of(...) does NOT trigger binding at all.
		var settings = Settings.get();
		settings.setGlobal("MyServiceI01.host", "example.com");
		var store = new BasicBeanStore(null);
		var bean = BeanInstantiator.of(I01_Base.class, store).type(I01_Sub.class).run();
		assertInstanceOf(I01_Sub.class, bean); // the requested subtype was actually instantiated
		assertEquals("example.com", bean.host); // bound under the BASE type's prefix
		assertSame(bean, store.getBean(I01_Base.class).orElse(null)); // registered under the declared BASE type
		assertFalse(store.getBean(I01_Sub.class).isPresent()); // NOT registered under the subtype
	}
}
