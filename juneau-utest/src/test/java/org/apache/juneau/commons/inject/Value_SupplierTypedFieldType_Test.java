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

import java.net.*;
import java.nio.file.*;
import java.time.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the typed {@code @Value Supplier<T>} autodetect contract — the extension of the bare
 * {@code Supplier<String>} path so that {@code T != String} is coerced from the resolved string
 * using the same converter machinery plain {@code @Value T} uses.
 *
 * <ul>
 * 	<li>{@code Supplier<T>} for the common converter targets (Integer, Long, Boolean, Duration, URI,
 * 		enum) resolve and coerce correctly.
 * 	<li>Each {@code .get()} re-resolves the string <i>and</i> re-coerces — live config changes are
 * 		reflected (the coerced value is never memoized).
 * 	<li>A coercion failure surfaces as a {@link RuntimeException} from {@code .get()}.
 * 	<li>{@code Supplier<String>} and raw {@code Supplier} behavior is unchanged.
 * 	<li>{@code Optional<Supplier<T>>} nesting is not specially handled.
 * </ul>
 */
@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class Value_SupplierTypedFieldType_Test extends TestBase {

	private static final String P_KEY = "Value_SupplierTypedFieldType_Test.key";

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(P_KEY);
		ValueResolver.clearTemplateCache();
	}

	enum A07_Color { RED, GREEN, BLUE }

	public static class IntegerSupplierBean {
		@Value("${" + P_KEY + ":42}")
		Supplier<Integer> value;
	}

	public static class LongSupplierBean {
		@Value("${" + P_KEY + ":42}")
		Supplier<Long> value;
	}

	public static class BooleanSupplierBean {
		@Value("${" + P_KEY + ":true}")
		Supplier<Boolean> value;
	}

	public static class DurationSupplierBean {
		@Value("${" + P_KEY + ":PT30S}")
		Supplier<Duration> value;
	}

	public static class UriSupplierBean {
		@Value("${" + P_KEY + ":https://example.org/}")
		Supplier<URI> value;
	}

	public static class EnumSupplierBean {
		@Value("${" + P_KEY + ":RED}")
		Supplier<A07_Color> value;
	}

	public static class PathSupplierBean {
		@Value("${" + P_KEY + ":/tmp/x}")
		Supplier<Path> value;
	}

	public static class StringSupplierBean {
		@Value("${" + P_KEY + ":default}")
		Supplier<String> value;
	}

	@SuppressWarnings("rawtypes")
	public static class RawSupplierBean {
		@Value("${" + P_KEY + ":default}")
		Supplier value;
	}

	public static class OptionalSupplierBean {
		@Value("${" + P_KEY + ":42}")
		java.util.Optional<Supplier<Integer>> value;
	}

	public static class IntegerSupplierCtorBean {
		final Supplier<Integer> value;

		@Inject
		public IntegerSupplierCtorBean(@Value("${" + P_KEY + ":42}") Supplier<Integer> value) {
			this.value = value;
		}
	}

	//====================================================================================================
	// a — one typed-supplier test per converter category.
	//====================================================================================================

	@Test void a01_integer() {
		Settings.get().setGlobal(P_KEY, "1234");
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		assertEquals(Integer.valueOf(1234), bean.value.get());
	}

	@Test void a02_long() {
		Settings.get().setGlobal(P_KEY, "9999999999");
		var bean = BeanInstantiator.of(LongSupplierBean.class, beanStore).run();
		assertEquals(Long.valueOf(9999999999L), bean.value.get());
	}

	@Test void a03_boolean() {
		Settings.get().setGlobal(P_KEY, "true");
		var bean = BeanInstantiator.of(BooleanSupplierBean.class, beanStore).run();
		assertEquals(Boolean.TRUE, bean.value.get());
	}

	@Test void a04_duration() {
		Settings.get().setGlobal(P_KEY, "PT1M");
		var bean = BeanInstantiator.of(DurationSupplierBean.class, beanStore).run();
		assertEquals(Duration.ofMinutes(1), bean.value.get());
	}

	@Test void a05_uri() {
		Settings.get().setGlobal(P_KEY, "https://juneau.apache.org/");
		var bean = BeanInstantiator.of(UriSupplierBean.class, beanStore).run();
		assertEquals(URI.create("https://juneau.apache.org/"), bean.value.get());
	}

	@Test void a06_defaultExpression_coerced() {
		// No Settings entry — the ${key:42} default flows through coercion the same as a resolved value.
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		assertEquals(Integer.valueOf(42), bean.value.get());
	}

	@Test void a07_enum() {
		Settings.get().setGlobal(P_KEY, "GREEN");
		var bean = BeanInstantiator.of(EnumSupplierBean.class, beanStore).run();
		assertEquals(A07_Color.GREEN, bean.value.get());
	}

	@Test void a08_constructorParam() {
		Settings.get().setGlobal(P_KEY, "777");
		var bean = BeanInstantiator.of(IntegerSupplierCtorBean.class, beanStore).run();
		assertEquals(Integer.valueOf(777), bean.value.get());
	}

	@Test void a09_path_registryBoundary_throwsFromGet() {
		// Path is NOT in the String->T converter registry (it is an interface with no (String)
		// constructor or single-arg String factory — StringSetting.asPath() special-cases Paths.get()
		// outside of Settings.toType). The typed-supplier path deliberately reuses ONLY the registry
		// (no hardcoded subset), so Supplier<Path> behaves identically to plain @Value Path: the
		// coercion fails as a RuntimeException at .get() time.
		Settings.get().setGlobal(P_KEY, "/tmp/x");
		var bean = BeanInstantiator.of(PathSupplierBean.class, beanStore).run();
		assertThrows(RuntimeException.class, () -> bean.value.get());
	}

	//====================================================================================================
	// b — late binding: each .get() re-resolves AND re-coerces (no value memoization).
	//====================================================================================================

	@Test void b01_lateBinding_reCoercesPerGet() {
		Settings.get().setGlobal(P_KEY, "1");
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		assertEquals(Integer.valueOf(1), bean.value.get());

		Settings.get().setGlobal(P_KEY, "2");
		assertEquals(Integer.valueOf(2), bean.value.get(), "Supplier<Integer> re-resolves and re-coerces per .get()");

		Settings.get().setGlobal(P_KEY, "3");
		assertEquals(Integer.valueOf(3), bean.value.get());
	}

	@Test void b02_lateBinding_notMemoizedAcrossInstances() {
		Settings.get().setGlobal(P_KEY, "10");
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		var first = bean.value.get();
		var second = bean.value.get();
		// Distinct boxed instances (outside the Integer cache range) prove no value memoization.
		Settings.get().setGlobal(P_KEY, "1000");
		assertEquals(Integer.valueOf(10), first);
		assertEquals(Integer.valueOf(10), second);
		assertEquals(Integer.valueOf(1000), bean.value.get());
	}

	//====================================================================================================
	// c — coercion failure surfaces as a RuntimeException from .get().
	//====================================================================================================

	@Test void c01_coercionFailure_throwsFromGet() {
		Settings.get().setGlobal(P_KEY, "notAnInt");
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		assertThrows(RuntimeException.class, () -> bean.value.get());
	}

	@Test void c02_coercionFailure_messageNamesKeyAndType() {
		Settings.get().setGlobal(P_KEY, "notAnInt");
		var bean = BeanInstantiator.of(IntegerSupplierBean.class, beanStore).run();
		var e = assertThrows(RuntimeException.class, () -> bean.value.get());
		assertTrue(e.getMessage().contains("Integer"), "Message should name the target type: " + e.getMessage());
		assertTrue(e.getMessage().contains(P_KEY), "Message should name the @Value expression: " + e.getMessage());
	}

	@Test void c03_enumCoercionFailure_throwsFromGet() {
		Settings.get().setGlobal(P_KEY, "PURPLE");
		var bean = BeanInstantiator.of(EnumSupplierBean.class, beanStore).run();
		assertThrows(RuntimeException.class, () -> bean.value.get());
	}

	//====================================================================================================
	// d — regression: Supplier<String> and raw Supplier behavior unchanged.
	//====================================================================================================

	@Test void d01_supplierString_unchanged() {
		Settings.get().setGlobal(P_KEY, "first");
		var bean = BeanInstantiator.of(StringSupplierBean.class, beanStore).run();
		assertEquals("first", bean.value.get());

		Settings.get().setGlobal(P_KEY, "second");
		assertEquals("second", bean.value.get(), "Supplier<String> still re-evaluates per .get()");
	}

	@Test void d02_rawSupplier_unchanged() {
		// Raw Supplier (no type argument) is not a coercible typed-supplier site — it falls through to
		// the one-shot path, where Supplier.class is not a registry-known type, so injection fails.
		Settings.get().setGlobal(P_KEY, "anything");
		assertThrows(RuntimeException.class, () -> BeanInstantiator.of(RawSupplierBean.class, beanStore).run());
	}

	//====================================================================================================
	// e — Optional<Supplier<T>> is not specially handled.
	//====================================================================================================

	@Test void e01_optionalOfSupplier_notSpeciallyHandled() {
		// Optional<Supplier<Integer>> unwraps to Supplier.class but the generic type's raw is Optional,
		// so the typed-supplier path is not taken. It is not specially supported.
		Settings.get().setGlobal(P_KEY, "42");
		assertThrows(RuntimeException.class, () -> BeanInstantiator.of(OptionalSupplierBean.class, beanStore).run());
	}
}
