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

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Coverage for {@link BasicBeanStore#registerConfiguration(Class)} and the
 * {@link Configuration} annotation.
 *
 * <h5 class='section'>Behaviour exercised:</h5>
 * <ul>
 * 	<li>Static and non-static {@code @Bean} fields and methods declared on a
 * 		{@code @Configuration} class are registered with the bean store.
 * 	<li>{@code @Configuration#imports()} pulls in additional configuration types,
 * 		including transitively and idempotently (a single class is processed only once).
 * 	<li>{@code @Bean} members declared on a superclass of the configuration are inherited.
 * 	<li>Registering the same {@code (type, name)} twice throws {@link BeanCreationException}.
 * 	<li>Calling {@code registerConfiguration} on a non-{@code @Configuration} type fails fast.
 * </ul>
 */
@SuppressWarnings({
	"java:S2094", // Intentionally empty bean class used as test fixture.
	"resource"    // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class Configuration_Test extends TestBase {

	//------------------------------------------------------------------------------------------------
	// Fixtures.
	//------------------------------------------------------------------------------------------------

	// Fixture classes are intentionally declared with package-private visibility (and so are their
	// constructors) to exercise the package-private constructor fallback path in BeanInstantiator.
	// The juneau-utest module previously had to force these to `public static` because BeanInstantiator
	// only considered public/protected constructors.
	static class SvcA { final String tag; SvcA(String tag) { this.tag = tag; } }
	static class SvcB { final SvcA dep; SvcB(SvcA dep) { this.dep = dep; } }
	static class SvcC { SvcC() {} }
	static class SvcD { SvcD() {} }

	@Configuration
	static class BaseConfig {
		BaseConfig() {}
		@Bean SvcA baseA() { return new SvcA("base"); }
	}

	@Configuration
	static class ChildConfig extends BaseConfig {
		ChildConfig() {}
		@Bean SvcB svcB(SvcA dep) { return new SvcB(dep); }
	}

	@Configuration
	static class StaticFieldConfig {
		@Bean static final SvcA STATIC_A = new SvcA("static");
	}

	@Configuration
	static class InstanceFieldConfig {
		InstanceFieldConfig() {}
		@Bean SvcA instanceA = new SvcA("instance");
	}

	@Configuration
	static class StaticMethodConfig {
		@Bean static SvcA staticA() { return new SvcA("static-method"); }
	}

	@Configuration
	static class NamedBeansConfig {
		NamedBeansConfig() {}
		@Bean(name = "primary") SvcA primary() { return new SvcA("primary"); }
		@Bean(name = "secondary") SvcA secondary() { return new SvcA("secondary"); }
	}

	@Configuration(imports = { ImportedConfig.class })
	static class ImporterConfig {
		ImporterConfig() {}
		@Bean SvcB svcB(SvcA dep) { return new SvcB(dep); }
	}

	@Configuration
	static class ImportedConfig {
		ImportedConfig() {}
		@Bean SvcA svcA() { return new SvcA("imported"); }
	}

	@Configuration(imports = { ImportedConfig.class, AlsoImports.class })
	static class DiamondConfig {
		DiamondConfig() {}
		@Bean SvcC svcC() { return new SvcC(); }
	}

	@Configuration(imports = { ImportedConfig.class })
	static class AlsoImports {
		AlsoImports() {}
		@Bean SvcD svcD() { return new SvcD(); }
	}

	@Configuration
	static class DuplicateBeanConfig {
		DuplicateBeanConfig() {}
		@Bean SvcA one() { return new SvcA("one"); }
		@Bean SvcA two() { return new SvcA("two"); }
	}

	static class NotConfigured { /* missing @Configuration */ }

	//------------------------------------------------------------------------------------------------
	// Tests.
	//------------------------------------------------------------------------------------------------

	@Test
	void a01_staticField_registersBean() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(StaticFieldConfig.class);
		var bean = store.getBean(SvcA.class);
		assertTrue(bean.isPresent());
		assertEquals("static", bean.get().tag);
	}

	@Test
	void a02_instanceField_registersBean() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(InstanceFieldConfig.class);
		var bean = store.getBean(SvcA.class);
		assertTrue(bean.isPresent());
		assertEquals("instance", bean.get().tag);
	}

	@Test
	void a03_staticMethod_registersBean() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(StaticMethodConfig.class);
		var bean = store.getBean(SvcA.class);
		assertTrue(bean.isPresent());
		assertEquals("static-method", bean.get().tag);
	}

	@Test
	void b01_superclassBeans_areInherited() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(ChildConfig.class);
		var a = store.getBean(SvcA.class);
		var b = store.getBean(SvcB.class);
		assertTrue(a.isPresent(), "Superclass @Bean inherited");
		assertEquals("base", a.get().tag);
		assertTrue(b.isPresent());
		assertSame(a.get(), b.get().dep, "Constructor argument resolved from inherited bean");
	}

	@Test
	void c01_imports_areProcessedRecursively() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(ImporterConfig.class);
		var a = store.getBean(SvcA.class);
		var b = store.getBean(SvcB.class);
		assertTrue(a.isPresent());
		assertEquals("imported", a.get().tag);
		assertTrue(b.isPresent());
		assertSame(a.get(), b.get().dep);
	}

	@Test
	void c02_imports_areDedupedTransitively() {
		// Diamond: DiamondConfig imports both ImportedConfig and AlsoImports; AlsoImports imports ImportedConfig.
		var store = new BasicBeanStore(null);
		store.registerConfiguration(DiamondConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent());
		assertTrue(store.getBean(SvcC.class).isPresent());
		assertTrue(store.getBean(SvcD.class).isPresent());
	}

	@Test
	void d01_namedBeans_areDistinct() {
		var store = new BasicBeanStore(null);
		store.registerConfiguration(NamedBeansConfig.class);
		var p = store.getBean(SvcA.class, "primary");
		var s = store.getBean(SvcA.class, "secondary");
		assertTrue(p.isPresent());
		assertTrue(s.isPresent());
		assertEquals("primary", p.get().tag);
		assertEquals("secondary", s.get().tag);
		assertNotSame(p.get(), s.get());
	}

	@Test
	void e01_duplicateBean_throws() {
		var store = new BasicBeanStore(null);
		var ex = assertThrows(BeanCreationException.class,
			() -> store.registerConfiguration(DuplicateBeanConfig.class));
		assertTrue(ex.getMessage().contains("Duplicate")
			|| (ex.getCause() != null && ex.getCause().getMessage() != null
				&& ex.getCause().getMessage().contains("Duplicate"))
			|| anyCauseContains(ex, "Duplicate"),
			"Expected message to mention Duplicate, got: " + ex.getMessage());
	}

	@Test
	void e02_notAConfiguration_throws() {
		var store = new BasicBeanStore(null);
		var ex = assertThrows(BeanCreationException.class,
			() -> store.registerConfiguration(NotConfigured.class));
		assertTrue(ex.getMessage().contains("@Configuration"));
	}

	@Test
	void f01_registerConfigurations_handlesNullsAndDuplicates() {
		var store = new BasicBeanStore(null);
		store.registerConfigurations(ImportedConfig.class, null, ImportedConfig.class);
		assertTrue(store.getBean(SvcA.class).isPresent());
	}

	private static boolean anyCauseContains(Throwable t, String needle) {
		while (t != null) {
			if (t.getMessage() != null && t.getMessage().contains(needle))
				return true;
			t = t.getCause();
		}
		return false;
	}
}
