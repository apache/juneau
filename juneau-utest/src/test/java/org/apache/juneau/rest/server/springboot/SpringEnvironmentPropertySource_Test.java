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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.springframework.context.*;
import org.springframework.core.env.*;
import org.springframework.mock.env.*;

/**
 * Tests for {@link SpringEnvironmentPropertySource} — the Phase 4 bridge from a Spring
 * {@link Environment} into the Juneau {@link Settings} singleton.
 *
 * <p>
 * Covers two layers:
 * <ol>
 * 	<li><b>Source semantics</b> (a-tier): direct unit tests against the {@link PropertySource}
 * 		contract using {@link MockEnvironment} — present, missing, lazy supplier, null env, supplier
 * 		throwing.
 * 	<li><b>End-to-end</b> (b-tier): {@code @Value("${...}")} resolution through the global
 * 		{@link Settings} singleton with the bridge attached, mirroring what Spring Boot does at
 * 		{@link SpringBeanStore} construction time.
 * </ol>
 *
 * <p>
 * A full {@code @SpringBootTest} bootstrap (with {@code application.yaml} on the classpath) is
 * exercised indirectly through the existing {@code StaticFilesMixin_Springboot_Test} and
 * {@code EchoMixin_Springboot_Test} smoke tests — those already build a real Spring
 * context which now installs the bridge automatically via {@link SpringBeanStore}'s constructor.
 * This test keeps the focus on the bridge itself.
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SuppressWarnings({
	"java:S2094", // Test fixture / data class, no methods required.
	"java:S2093" // SpringBeanStore teardown uses clear() (which removes the installed Settings source); try-with-resources would call close() instead, which does not remove the source and would leak the bridge into later tests.
})
class SpringEnvironmentPropertySource_Test extends TestBase {

	public static class ValueBean {
		@Value("${spring.demo.key}")
		String value;
	}

	public static class ValueBeanWithDefault {
		@Value("${spring.demo.missing:fallback}")
		String value;
	}

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	//====================================================================================================
	// Source semantics — direct unit tests.
	//====================================================================================================

	@Test
	void a01_get_returnsPresent_whenKeyExists() {
		var env = new MockEnvironment().withProperty("spring.demo.key", "from-env");
		var src = new SpringEnvironmentPropertySource(env);

		var r = src.get("spring.demo.key");
		assertTrue(r.isPresent());
		assertEquals("from-env", r.value().orElse(null));
	}

	@Test
	void a02_get_returnsMissing_whenKeyAbsent() {
		var env = new MockEnvironment();
		var src = new SpringEnvironmentPropertySource(env);

		var r = src.get("spring.demo.key");
		assertFalse(r.isPresent(), "Source must report missing() when key is not in the environment");
	}

	@Test
	void a03_constructor_nullEnv_throwsNPE() {
		assertThrows(NullPointerException.class,
			() -> new SpringEnvironmentPropertySource((Environment) null));
	}

	@Test
	void a04_supplierForm_resolvesLazily() {
		// Supplier is held but not invoked until the first get() — verifies that the lazy form
		// honors the contract that SpringBeanStore relies on (no interaction with the Spring
		// context at SpringBeanStore construction time).
		var env = new MockEnvironment().withProperty("spring.demo.key", "lazy-resolved");
		var calls = new int[]{0};
		var src = new SpringEnvironmentPropertySource(() -> {
			calls[0]++;
			return env;
		});
		assertEquals(0, calls[0], "Supplier must not be invoked at construction time");

		var r = src.get("spring.demo.key");
		assertEquals(1, calls[0], "Supplier must be invoked exactly once on first lookup");
		assertTrue(r.isPresent());
		assertEquals("lazy-resolved", r.value().orElse(null));

		var r2 = src.get("spring.demo.key");
		assertEquals(1, calls[0], "Supplier must be memoized — subsequent lookups must not re-invoke it");
		assertTrue(r2.isPresent());
	}

	@Test
	void a05_supplierForm_nullSupplierReturn_emitsMissing() {
		// A null Environment is tolerated — e.g. a mocked ApplicationContext that doesn't stub
		// getEnvironment(). Every lookup returns missing(), no NPE.
		var src = new SpringEnvironmentPropertySource(() -> null);
		var r = src.get("any.key");
		assertFalse(r.isPresent());
	}

	@Test
	void a06_supplierForm_throwingSupplier_emitsMissing() {
		// safeOpt swallows supplier exceptions — the bridge should never fail a lookup just because
		// the application context isn't ready or threw.
		var src = new SpringEnvironmentPropertySource(() -> {
			throw new IllegalStateException("not ready");
		});
		var r = src.get("any.key");
		assertFalse(r.isPresent());
	}

	@Test
	void a07_constructor_nullSupplier_throwsNPE() {
		assertThrows(NullPointerException.class,
			() -> new SpringEnvironmentPropertySource((java.util.function.Supplier<Environment>) null));
	}

	//====================================================================================================
	// End-to-end: Settings.get().addSource(...) → @Value("${...}") resolution.
	//====================================================================================================

	@Test
	void b01_endToEnd_resolveValueThroughSettings() {
		var env = new MockEnvironment().withProperty("spring.demo.key", "via-settings");
		var src = new SpringEnvironmentPropertySource(env);
		Settings.get().addSource(src);
		try {
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("via-settings", bean.value);
		} finally {
			assertTrue(Settings.get().removeSource(src));
		}
	}

	@Test
	void b02_endToEnd_defaultPath_whenKeyMissingEverywhere() {
		// No source pushed — the @Value default token (after the colon) fires.
		var bean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
		assertEquals("fallback", bean.value);
	}

	@Test
	void b03_endToEnd_laterAddSource_shadowsEarlier() {
		// Two Spring-env sources stacked: the second one wins because Settings walks sources in
		// reverse insertion order.
		var base = new MockEnvironment().withProperty("spring.demo.key", "base-val");
		var override = new MockEnvironment().withProperty("spring.demo.key", "override-val");
		var baseSrc = new SpringEnvironmentPropertySource(base);
		var overrideSrc = new SpringEnvironmentPropertySource(override);
		Settings.get().addSource(baseSrc);
		Settings.get().addSource(overrideSrc);
		try {
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("override-val", bean.value, "Later addSource() must shadow earlier addSource()");
		} finally {
			Settings.get().removeSource(overrideSrc);
			Settings.get().removeSource(baseSrc);
		}
	}

	//====================================================================================================
	// SpringBeanStore auto-registration — bridge installs and cleans up.
	//====================================================================================================

	@Test
	void c01_springBeanStore_installsBridge_andClearRemovesIt() {
		// Use a mocked ApplicationContext whose getEnvironment() returns a populated MockEnvironment.
		var env = new MockEnvironment().withProperty("spring.demo.key", "via-springbeanstore");
		var ctx = mock(ApplicationContext.class);
		when(ctx.getEnvironment()).thenReturn(env);

		// Before any SpringBeanStore is built, @Value lookup must miss → fall through to default.
		var preBean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
		assertEquals("fallback", preBean.value, "No bridge installed yet — default must fire");

		var store = new SpringBeanStore(ctx, null);
		try {
			// Bridge installed by the constructor.
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("via-springbeanstore", bean.value,
				"@Value must resolve through the SpringBeanStore-installed bridge");
		} finally {
			store.clear();
		}

		// After clear(), the bridge is removed and lookups must miss again.
		var postBean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
		assertEquals("fallback", postBean.value, "After clear(), bridge must be removed");
	}

	@Test
	void c02_springBeanStore_nullAppContext_noBridge() {
		// Null appContext means no bridge — the constructor must not register a source at all.
		var store = new SpringBeanStore(null, null);
		try {
			// No source registered → default path fires.
			var bean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
			assertEquals("fallback", bean.value);
		} finally {
			store.clear();
		}
	}
}
