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
package org.apache.juneau.commons.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Acceptance tests for {@code @Value Optional<T>} wrapping parity between
 * {@link ParameterInfo#resolveValue} and {@link FieldInfo#inject}.
 *
 * <p>
 * Prior to this fix, declaring a parameter as {@code @Value("...") Optional<String>} caused
 * {@link IllegalArgumentException} at reflective invoke because {@link ParameterInfo#resolveValue}
 * returned the unwrapped scalar instead of an {@link Optional}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ParameterInfo_OptionalValue_Test extends TestBase {

	private static final String KEY = "ParameterInfo_OptionalValue_Test.key";

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(KEY);
	}

	//====================================================================================================
	// Fixtures
	//====================================================================================================

	public static class OptionalMethodBean {
		Optional<String> result;

		@Inject
		public void inject(@Value("${" + KEY + "}") Optional<String> value) {
			this.result = value;
		}
	}

	public static class OptionalConstructorBean {
		final Optional<String> value;

		@Inject
		public OptionalConstructorBean(@Value("${" + KEY + "}") Optional<String> value) {
			this.value = value;
		}
	}

	public static class PlainStringMethodBean {
		String result;

		@Inject
		public void inject(@Value("${" + KEY + ":plain-default}") String value) {
			this.result = value;
		}
	}

	/** Mixed: one Optional, one plain String — verifies per-parameter wrapping. */
	public static class MixedMethodBean {
		Optional<String> optional;
		String plain;

		@Inject
		public void inject(
				@Value("${" + KEY + "}") Optional<String> optional,
				@Value("${" + KEY + ":plain-default}") String plain) {
			this.optional = optional;
			this.plain = plain;
		}
	}

	//====================================================================================================
	// Scenario 1: @Value Optional<String> method parameter — resolved value present → Optional.of(...)
	//====================================================================================================

	@Test
	void a01_optionalMethodParam_present() {
		Settings.get().setGlobal(KEY, "hello");
		var bean = BeanInstantiator.of(OptionalMethodBean.class, beanStore).run();
		assertNotNull(bean.result, "result must not be null");
		assertTrue(bean.result.isPresent(), "result must be present");
		assertEquals("hello", bean.result.get());
	}

	//====================================================================================================
	// Scenario 2: @Value Optional<String> method parameter — no key, no default → Optional.empty()
	//====================================================================================================

	@Test
	void a02_optionalMethodParam_absent_isEmpty() {
		var bean = BeanInstantiator.of(OptionalMethodBean.class, beanStore).run();
		assertNotNull(bean.result, "result must not be null");
		assertTrue(bean.result.isEmpty(), "result must be empty when key is absent");
	}

	//====================================================================================================
	// Scenario 3: @Value Optional<String> constructor parameter — resolved value present → Optional.of(...)
	//====================================================================================================

	@Test
	void a03_optionalConstructorParam_present() {
		Settings.get().setGlobal(KEY, "world");
		var bean = BeanInstantiator.of(OptionalConstructorBean.class, beanStore).run();
		assertNotNull(bean.value, "value must not be null");
		assertTrue(bean.value.isPresent(), "value must be present");
		assertEquals("world", bean.value.get());
	}

	//====================================================================================================
	// Scenario 4: @Value Optional<String> constructor parameter — absent → Optional.empty()
	//====================================================================================================

	@Test
	void a04_optionalConstructorParam_absent_isEmpty() {
		var bean = BeanInstantiator.of(OptionalConstructorBean.class, beanStore).run();
		assertNotNull(bean.value, "value must not be null");
		assertTrue(bean.value.isEmpty(), "value must be empty when key is absent");
	}

	//====================================================================================================
	// Scenario 5: Plain String parameter (non-Optional) — unwrapped scalar, unchanged behavior
	//====================================================================================================

	@Test
	void a05_plainStringParam_usesDefault() {
		var bean = BeanInstantiator.of(PlainStringMethodBean.class, beanStore).run();
		assertEquals("plain-default", bean.result);
	}

	@Test
	void a05b_plainStringParam_fromSettings() {
		Settings.get().setGlobal(KEY, "from-settings");
		var bean = BeanInstantiator.of(PlainStringMethodBean.class, beanStore).run();
		assertEquals("from-settings", bean.result);
	}

	//====================================================================================================
	// Scenario 6: Mixed parameters (one Optional, one plain) — correct wrap per parameter
	//====================================================================================================

	@Test
	void a06_mixedParams_optionalAbsent_plainDefault() {
		var bean = BeanInstantiator.of(MixedMethodBean.class, beanStore).run();
		assertNotNull(bean.optional, "optional must not be null");
		assertTrue(bean.optional.isEmpty(), "optional must be empty when key is absent");
		assertEquals("plain-default", bean.plain);
	}

	@Test
	void a07_mixedParams_optionalPresent_plainFromSettings() {
		Settings.get().setGlobal(KEY, "mixed-value");
		var bean = BeanInstantiator.of(MixedMethodBean.class, beanStore).run();
		assertNotNull(bean.optional, "optional must not be null");
		assertTrue(bean.optional.isPresent(), "optional must be present");
		assertEquals("mixed-value", bean.optional.get());
		assertEquals("mixed-value", bean.plain);
	}
}
