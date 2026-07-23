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

import java.lang.annotation.*;
import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for the {@code @Value} annotation.
 *
 * <p>
 * Covers Juneau's {@link Value @Value}, Spring's {@code @Value} (detected by FQN), and the negative
 * paths around primitive-null and {@code @Value} + {@code @Inject} mutual exclusion.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class Value_Test extends TestBase {

	private static final String P_STRING = "Value_Test.string";
	private static final String P_INT = "Value_Test.int";
	private static final String P_BOOL = "Value_Test.bool";
	private static final String P_URI = "Value_Test.uri";
	private static final String P_INSTANT = "Value_Test.instant";

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	@AfterEach
	void cleanup() {
		var s = Settings.get();
		for (var k : List.of(P_STRING, P_INT, P_BOOL, P_URI, P_INSTANT, "Value_Test.optional",
			"Value_Test.timeout.ms", "Value_Test.field", "Value_Test.setter", "Value_Test.springStyle",
			"Value_Test.coerceFail", "Value_Test.optionalSetter", "Value_Test.optionalCtor"))
			s.unsetGlobal(k);
	}

	//====================================================================================================
	// Fixtures.
	//====================================================================================================

	/**
	 * A Spring-style {@code @Value} replica that {@link JsrSupport} matches by FQN.
	 *
	 * <p>
	 * The real Spring annotation is at {@code org.springframework.beans.factory.annotation.Value}; we
	 * cannot directly import that here without a compile-time Spring dependency.  Instead, this
	 * private annotation mirrors the FQN that {@link JsrSupport#SPRING_VALUE} matches but lives in
	 * the test fixture package, so {@code isValueAnnotation} alone cannot tell them apart.  We
	 * instead exercise both Juneau {@code @Value} and {@code @Value}+{@code @Inject} conflict paths
	 * directly — the Spring FQN match is exercised in the actual Spring Boot integration test
	 * fixture (Phase 4).
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.CONSTRUCTOR})
	@interface MarkerAnnotation {}

	public static class StringFieldBean {
		@Value("${" + P_STRING + ":hello}")
		String greeting;
	}

	public static class IntFieldBean {
		@Value("${" + P_INT + "}")
		int count;
	}

	public static class BoxedIntFieldBean {
		@Value("${" + P_INT + ":42}")
		Integer count;
	}

	public static class BooleanFieldBean {
		@Value("${" + P_BOOL + ":true}")
		boolean enabled;
	}

	public static class UriFieldBean {
		@Value("${" + P_URI + ":https://example.org/}")
		URI endpoint;
	}

	public static class InstantFieldBean {
		@Value("${" + P_INSTANT + ":2020-01-01T00:00:00Z}")
		Instant when;
	}

	public static class OptionalFieldBean {
		@Value("${Value_Test.optional}")
		Optional<String> maybe;
	}

	public static class ConstructorParamBean {
		final String greeting;
		final int timeoutMs;

		@Inject
		public ConstructorParamBean(
				@Value("${Value_Test.string:bonjour}") String greeting,
				@Value("${Value_Test.timeout.ms:5000}") int timeoutMs) {
			this.greeting = greeting;
			this.timeoutMs = timeoutMs;
		}
	}

	public static class SetterBean {
		String field;

		@Inject
		public void setField(@Value("${Value_Test.setter:via-setter}") String field) {
			this.field = field;
		}
	}

	/** Setter that accepts an {@code Optional<String>} @Value parameter — worked example of the fix. */
	public static class OptionalSetterBean {
		Optional<String> maybe;

		@Inject
		public void setMaybe(@Value("${Value_Test.optionalSetter}") Optional<String> maybe) {
			this.maybe = maybe;
		}
	}

	/** Constructor that mixes a plain and an Optional @Value parameter — worked example of the fix. */
	public static class OptionalConstructorBean {
		final String plain;
		final Optional<String> maybe;

		@Inject
		public OptionalConstructorBean(
				@Value("${Value_Test.string:hello}") String plain,
				@Value("${Value_Test.optionalCtor}") Optional<String> maybe) {
			this.plain = plain;
			this.maybe = maybe;
		}
	}

	public static class ConflictFieldBean {
		// Mutual-exclusion guard fires when both are present on the same site.
		@Value("${Value_Test.field}")
		@Inject
		String mixed;
	}

	public static class ConflictParamBean {
		final String s;

		@Inject
		public ConflictParamBean(@Value("${Value_Test.field}") @Inject String s) {
			this.s = s;
		}
	}

	public static class PrimitiveMissingNoDefaultBean {
		// Primitive site with no default and missing key — must throw at injection time.
		@Value("${Value_Test.missing.primitive.no.default}")
		int port;
	}

	public static class CoerceFailureBean {
		@Value("${Value_Test.coerceFail}")
		int notAnInt;
	}

	/**
	 * Field using Spring's {@code @Value} annotation (matched by FQN through {@link JsrSupport}).
	 *
	 * <p>
	 * Spring's annotation is fully-qualified so it doesn't collide with the Juneau
	 * {@link Value @Value} that the rest of this file uses.
	 */
	public static class SpringValueFieldBean {
		@org.springframework.beans.factory.annotation.Value("${Value_Test.springStyle:spring-default}")
		String value;
	}

	//====================================================================================================
	// Field injection.
	//====================================================================================================

	@Test
	void a01_field_string_default() {
		var bean = BeanInstantiator.of(StringFieldBean.class, beanStore).run();
		assertEquals("hello", bean.greeting);
	}

	@Test
	void a02_field_string_fromSettings() {
		Settings.get().setGlobal(P_STRING, "from-settings");
		var bean = BeanInstantiator.of(StringFieldBean.class, beanStore).run();
		assertEquals("from-settings", bean.greeting);
	}

	@Test
	void a03_field_intPrimitive_coerced() {
		Settings.get().setGlobal(P_INT, "1234");
		var bean = BeanInstantiator.of(IntFieldBean.class, beanStore).run();
		assertEquals(1234, bean.count);
	}

	@Test
	void a04_field_integerBoxed_default() {
		var bean = BeanInstantiator.of(BoxedIntFieldBean.class, beanStore).run();
		assertEquals(42, bean.count);
	}

	@Test
	void a05_field_booleanPrimitive_default() {
		var bean = BeanInstantiator.of(BooleanFieldBean.class, beanStore).run();
		assertTrue(bean.enabled);
	}

	@Test
	void a06_field_uri_coerced() {
		var bean = BeanInstantiator.of(UriFieldBean.class, beanStore).run();
		assertEquals(URI.create("https://example.org/"), bean.endpoint);
	}

	@Test
	void a07_field_instant_coerced() {
		var bean = BeanInstantiator.of(InstantFieldBean.class, beanStore).run();
		assertEquals(Instant.parse("2020-01-01T00:00:00Z"), bean.when);
	}

	@Test
	void a08_field_optional_missing_isEmpty() {
		var bean = BeanInstantiator.of(OptionalFieldBean.class, beanStore).run();
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isEmpty());
	}

	@Test
	void a09_field_optional_present() {
		Settings.get().setGlobal("Value_Test.optional", "have-value");
		var bean = BeanInstantiator.of(OptionalFieldBean.class, beanStore).run();
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isPresent());
		assertEquals("have-value", bean.maybe.get());
	}

	//====================================================================================================
	// Constructor-parameter injection.
	//====================================================================================================

	@Test
	void b01_constructorParam_default() {
		var bean = BeanInstantiator.of(ConstructorParamBean.class, beanStore).run();
		assertEquals("bonjour", bean.greeting);
		assertEquals(5000, bean.timeoutMs);
	}

	@Test
	void b02_constructorParam_fromSettings() {
		Settings.get().setGlobal("Value_Test.string", "salut");
		Settings.get().setGlobal("Value_Test.timeout.ms", "250");
		var bean = BeanInstantiator.of(ConstructorParamBean.class, beanStore).run();
		assertEquals("salut", bean.greeting);
		assertEquals(250, bean.timeoutMs);
	}

	//====================================================================================================
	// Setter injection.
	//====================================================================================================

	@Test
	void c01_setter_default() {
		var bean = BeanInstantiator.of(SetterBean.class, beanStore).run();
		assertEquals("via-setter", bean.field);
	}

	@Test
	void c02_setter_fromSettings() {
		Settings.get().setGlobal("Value_Test.setter", "from-settings");
		var bean = BeanInstantiator.of(SetterBean.class, beanStore).run();
		assertEquals("from-settings", bean.field);
	}

	// Worked example: Optional<T> setter parameter previously threw IAE at reflective invoke.
	@Test
	void c03_setter_optionalParam_missing_isEmpty() {
		var bean = BeanInstantiator.of(OptionalSetterBean.class, beanStore).run();
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isEmpty());
	}

	@Test
	void c04_setter_optionalParam_present() {
		Settings.get().setGlobal("Value_Test.optionalSetter", "setter-opt");
		var bean = BeanInstantiator.of(OptionalSetterBean.class, beanStore).run();
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isPresent());
		assertEquals("setter-opt", bean.maybe.get());
	}

	// Worked example: Optional<T> constructor parameter.
	@Test
	void b03_constructorParam_optionalMissing_isEmpty() {
		var bean = BeanInstantiator.of(OptionalConstructorBean.class, beanStore).run();
		assertEquals("hello", bean.plain);
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isEmpty());
	}

	@Test
	void b04_constructorParam_optionalPresent() {
		Settings.get().setGlobal("Value_Test.optionalCtor", "ctor-opt");
		var bean = BeanInstantiator.of(OptionalConstructorBean.class, beanStore).run();
		assertEquals("hello", bean.plain);
		assertNotNull(bean.maybe);
		assertTrue(bean.maybe.isPresent());
		assertEquals("ctor-opt", bean.maybe.get());
	}

	//====================================================================================================
	// Mutual-exclusion with @Inject.
	//====================================================================================================

	@Test
	void d01_field_value_plus_inject_throws() {
		var inst = BeanInstantiator.of(ConflictFieldBean.class, beanStore);
		var ex = assertThrows(BeanCreationException.class, inst::run);
		assertTrue(ex.getMessage().contains("@Value and @Inject are mutually exclusive"),
			"Unexpected message: " + ex.getMessage());
	}

	@Test
	void d02_parameter_value_plus_inject_throws() {
		var inst = BeanInstantiator.of(ConflictParamBean.class, beanStore);
		var ex = assertThrows(BeanCreationException.class, inst::run);
		assertTrue(ex.getMessage().contains("@Value and @Inject are mutually exclusive"),
			"Unexpected message: " + ex.getMessage());
	}

	//====================================================================================================
	// Failure paths.
	//====================================================================================================

	@Test
	void e01_primitive_missing_noDefault_throws() {
		var inst = BeanInstantiator.of(PrimitiveMissingNoDefaultBean.class, beanStore);
		var ex = assertThrows(BeanCreationException.class, inst::run);
		assertTrue(ex.getMessage().contains("Could not resolve required @Value"),
			"Unexpected message: " + ex.getMessage());
	}

	@Test
	void e02_coerce_failure_wrapped() {
		Settings.get().setGlobal("Value_Test.coerceFail", "not-a-number");
		var inst = BeanInstantiator.of(CoerceFailureBean.class, beanStore);
		var ex = assertThrows(BeanCreationException.class, inst::run);
		assertTrue(ex.getMessage().contains("Could not coerce @Value"),
			"Unexpected message: " + ex.getMessage());
	}

	//====================================================================================================
	// Spring's @Value (FQN-detected).
	//====================================================================================================

	@Test
	void g01_spring_value_default() {
		var bean = BeanInstantiator.of(SpringValueFieldBean.class, beanStore).run();
		assertEquals("spring-default", bean.value);
	}

	@Test
	void g02_spring_value_fromSettings() {
		Settings.get().setGlobal("Value_Test.springStyle", "spring-override");
		var bean = BeanInstantiator.of(SpringValueFieldBean.class, beanStore).run();
		assertEquals("spring-override", bean.value);
	}

	//====================================================================================================
	// JsrSupport helpers — direct unit tests.
	//====================================================================================================

	@Test
	void f01_isValueAnnotation_juneau() {
		var f = org.apache.juneau.commons.reflect.ClassInfo.of(StringFieldBean.class)
			.getDeclaredField(x -> "greeting".equals(x.getName())).orElseThrow();
		assertTrue(f.getAnnotations().stream().anyMatch(JsrSupport::isValueAnnotation));
	}

	@Test
	void f02_valueExpression_extractsValue() {
		var f = org.apache.juneau.commons.reflect.ClassInfo.of(StringFieldBean.class)
			.getDeclaredField(x -> "greeting".equals(x.getName())).orElseThrow();
		var expr = f.getAnnotations().stream()
			.map(JsrSupport::valueExpression)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
		assertEquals("${" + P_STRING + ":hello}", expr);
	}

	@Test
	void f03_valueExpression_returnsNull_forNonValueAnnotation() {
		var f = org.apache.juneau.commons.reflect.ClassInfo.of(ConflictFieldBean.class)
			.getDeclaredField(x -> "mixed".equals(x.getName())).orElseThrow();
		var injectAnno = f.getAnnotations().stream()
			.filter(a -> JsrSupport.isInjectAnnotation(a) && ! JsrSupport.isValueAnnotation(a))
			.findFirst()
			.orElseThrow();
		assertNull(JsrSupport.valueExpression(injectAnno));
	}

	//====================================================================================================
	// ValueResolver — direct unit tests for primitive boxing + null/empty branches.
	//====================================================================================================

	@Test
	void h01_resolve_primitiveLong_coerced() {
		Settings.get().setGlobal("Value_Test.long", "9999999999");
		assertEquals(9999999999L, ValueResolver.resolve("${Value_Test.long}", long.class, "h01"));
		Settings.get().unsetGlobal("Value_Test.long");
	}

	@Test
	void h02_resolve_primitiveDouble_coerced() {
		assertEquals(3.14, (Double) ValueResolver.resolve("${Value_Test.double:3.14}", double.class, "h02"), 0.001);
	}

	@Test
	void h03_resolve_primitiveFloat_coerced() {
		assertEquals(2.5f, (Float) ValueResolver.resolve("${Value_Test.float:2.5}", float.class, "h03"), 0.001f);
	}

	@Test
	void h04_resolve_primitiveShort_coerced() {
		assertEquals((short) 42, ValueResolver.resolve("${Value_Test.short:42}", short.class, "h04"));
	}

	@Test
	void h05_resolve_primitiveByte_coerced() {
		assertEquals((byte) 7, ValueResolver.resolve("${Value_Test.byte:7}", byte.class, "h05"));
	}

	@Test
	void h06_resolve_primitiveChar_coerced() {
		assertEquals('Z', ValueResolver.resolve("${Value_Test.char:Z}", char.class, "h06"));
	}

	@Test
	void h07_resolve_referenceMissing_coercesEmptyToUri() {
		// VarResolver lowers null→"" before ValueResolver sees it, so a missing-no-default key
		// arrives as "". URI("") is a valid empty-URI value; we don't second-guess the coercion.
		var u = (URI) ValueResolver.resolve("${Value_Test.missing.ref}", URI.class, "h07");
		assertNotNull(u);
		assertEquals("", u.toString());
	}

	@Test
	void h07b_resolve_referenceMissing_coerceFailure_wrapped() {
		// Non-String reference type that can't accept "" (Instant has no empty form) — must wrap.
		var ex = assertThrows(BeanCreationException.class,
			() -> ValueResolver.resolve("${Value_Test.missing.instant}", Instant.class, "h07b"));
		assertTrue(ex.getMessage().contains("Could not coerce @Value"));
	}

	@Test
	void h07c_resolve_charLength2_wraps() {
		var ex = assertThrows(BeanCreationException.class,
			() -> ValueResolver.resolve("${Value_Test.badchar:AB}", char.class, "h07c"));
		assertTrue(ex.getMessage().contains("to char"));
	}

	@Test
	void h08_resolve_emptyString_returnsEmptyForString() {
		Settings.get().setGlobal("Value_Test.empty", "");
		assertEquals("", ValueResolver.resolve("${Value_Test.empty}", String.class, "h08"));
		Settings.get().unsetGlobal("Value_Test.empty");
	}

	@Test
	void h09_findValueExpression_emptyList_returnsNull() {
		assertNull(ValueResolver.findValueExpression(List.of()));
	}

	@Test
	void h10_findValueExpression_nullList_returnsNull() {
		assertNull(ValueResolver.findValueExpression(null));
	}

	@Test
	void h11_hasValueAnnotation_emptyList_returnsFalse() {
		assertFalse(ValueResolver.hasValueAnnotation(List.of()));
	}

	@Test
	void h12_hasValueAnnotation_nullList_returnsFalse() {
		assertFalse(ValueResolver.hasValueAnnotation(null));
	}

	@Test
	void h13_checkInjectConflict_nullList_noop() {
		assertDoesNotThrow(() -> ValueResolver.checkInjectConflict(null, "h13"));
	}

	@Test
	void h14_checkInjectConflict_emptyList_noop() {
		assertDoesNotThrow(() -> ValueResolver.checkInjectConflict(List.of(), "h14"));
	}

	@Test
	void h15_checkInjectConflict_noValueAnnotation_noop() {
		// annotations present, but none is @Value → should short-circuit on the !hasValue branch.
		var f = org.apache.juneau.commons.reflect.ClassInfo.of(SetterBean.class)
			.getMethod(x -> "setField".equals(x.getName())).orElseThrow();
		assertDoesNotThrow(() -> ValueResolver.checkInjectConflict(f.getAnnotations(), "h15"));
	}

	@Test
	void h16_resolve_nullExpression_returnsNullForRefType() {
		// VarResolver.resolve(null) returns null; ValueResolver should pass that through for refs.
		assertNull(ValueResolver.resolve(null, String.class, "h16"));
	}

	@Test
	void h17_resolve_nullExpression_throwsForPrimitive() {
		var ex = assertThrows(BeanCreationException.class,
			() -> ValueResolver.resolve(null, int.class, "h17"));
		assertTrue(ex.getMessage().contains("Could not resolve required @Value"));
	}
}
