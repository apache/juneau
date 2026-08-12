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
package org.apache.juneau.marshall.httppart;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.math.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.commons.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.httppart.HttpPartSchema.Builder;
import org.junit.jupiter.api.*;

/**
 * Targeted branch-coverage tests for {@link HttpPartSchema} filling gaps left by
 * {@code HttpPartSchema_Validation_Test} and {@code HttpPartSchema_JakartaValidation_Test}: builder null-arg
 * no-ops, the {@code apply(Class,Method)} annotation-dispatch entry point, Jakarta-validation reflection-guard
 * edge cases, per-type "not allowed" attribute combinations, numeric exclusiveXxxValue type dispatch, and
 * low-level format-parser (date/time/duration/email/hostname/ipv6/etc.) edge cases.
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class HttpPartSchema_Coverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A: Builder null-arg no-ops
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_additionalProperties_nullIgnored() {
		var s = HttpPartSchema.create().tObject()
			.additionalProperties((Builder)null)
			.additionalProperties((HttpPartSchema)null)
			.build();
		assertNull(s.getProperty("anyKey"));
		assertFalse(s.hasProperties());
	}

	@Test void a02_items_nullIgnored() {
		var s = HttpPartSchema.create().tArray()
			.items((Builder)null)
			.items((HttpPartSchema)null)
			.build();
		assertNull(s.getItems());
	}

	@Test void a03_noValidate_nullArg_leavesDefault() {
		// A null argument to noValidate(Boolean) is ignored, so validation still runs and this throws.
		assertThrowsWithMessage(BasicRuntimeException.class, "Schema specification errors",
			()->HttpPartSchema.create().tString().minimum(1).noValidate((Boolean)null).build());
	}

	@Test void a04_property_nullKeyOrValueIgnored() {
		var inner = HttpPartSchema.create().tString();
		var s = HttpPartSchema.create().tObject()
			.property(null, inner)
			.property("foo", (Builder)null)
			.property(null, (Builder)null)
			.property(null, inner.build())
			.property("bar", (HttpPartSchema)null)
			.property(null, (HttpPartSchema)null)
			.build();
		assertNull(s.getProperty("foo"));
		assertNull(s.getProperty("bar"));
		assertFalse(s.hasProperties());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: apply(Class,Method) annotation-dispatch entry point
	//-----------------------------------------------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	private @interface B01Marker {}

	static class B01Foo {
		@B01Marker
		public String bar() { return null; }
		public String baz() { return null; }
	}

	@Test void b01_create_classAnnotation_method_withAndWithoutDirectAnnotation() throws Exception {
		var barM = B01Foo.class.getMethod("bar");
		var bazM = B01Foo.class.getMethod("baz");
		// bar() carries @B01Marker directly -> hits the nn(a) true branch (and the default dispatch arm,
		// since B01Marker isn't a recognized HTTP-part annotation name).
		var s1 = HttpPartSchema.create(B01Marker.class, barM);
		assertNotNull(s1);
		// baz() has no @B01Marker -> hits the nn(a) false branch.
		var s2 = HttpPartSchema.create(B01Marker.class, bazM);
		assertNotNull(s2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: Jakarta-validation reflection-guard / boundary edge cases
	// applyJakartaValidation() is package-private specifically so it can be exercised directly with malformed
	// or boundary annotation shapes without needing them to live under jakarta.validation.constraints.
	//-----------------------------------------------------------------------------------------------------------------

	public static class C01Bean {
		@jakarta.validation.constraints.Size(max=10)
		public String value;
	}

	@Test void c01_size_minDefaultZero_skipsMinLengthMinItems() throws Exception {
		var anno = C01Bean.class.getDeclaredField("value").getAnnotation(jakarta.validation.constraints.Size.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMinLength());
		assertNull(s.getMinItems());
		assertEquals(10L, s.getMaxLength());
		assertEquals(10L, s.getMaxItems());
	}

	public static class C02Bean {
		@jakarta.validation.constraints.Size(min=5)
		public String value;
	}

	@Test void c02_size_maxDefaultMaxValue_skipsMaxLengthMaxItems() throws Exception {
		var anno = C02Bean.class.getDeclaredField("value").getAnnotation(jakarta.validation.constraints.Size.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMaxLength());
		assertNull(s.getMaxItems());
		assertEquals(5L, s.getMinLength());
		assertEquals(5L, s.getMinItems());
	}

	// Fake annotations below share simple names with real Jakarta constraints so applyJakartaValidation's
	// switch dispatches into the same case arms, but declare a "value"/"regexp" attribute of the WRONG type
	// (String instead of long, etc). getAnnotationValue()'s type.isInstance() check then fails, returning null
	// -- exercising both its own type-mismatch branch and the caller's nn(...) guard, which real Jakarta
	// annotations (whose attributes are always the expected type) can never trigger.
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Min {
		String value() default "not-a-long";
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Max {
		String value() default "not-a-long";
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Pattern {
		int regexp() default 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface DecimalMin {
		int value() default 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface DecimalMax {
		int value() default 0;
	}

	static class C0xBean {
		@Min public String min;
		@Max public String max;
		@Pattern public String pattern;
		@DecimalMin public String decimalMin;
		@DecimalMax public String decimalMax;
	}

	@Test void c03_min_wrongAttributeType_ignored() throws Exception {
		var anno = C0xBean.class.getDeclaredField("min").getAnnotation(Min.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMinimum());
	}

	@Test void c04_max_wrongAttributeType_ignored() throws Exception {
		var anno = C0xBean.class.getDeclaredField("max").getAnnotation(Max.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMaximum());
	}

	@Test void c05_pattern_wrongAttributeType_ignored() throws Exception {
		var anno = C0xBean.class.getDeclaredField("pattern").getAnnotation(Pattern.class);
		var s = HttpPartSchema.create().tString().applyJakartaValidation(anno).build();
		assertNull(s.getPattern());
	}

	@Test void c06_decimalMin_wrongAttributeType_ignored() throws Exception {
		var anno = C0xBean.class.getDeclaredField("decimalMin").getAnnotation(DecimalMin.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMinimum());
	}

	@Test void c07_decimalMax_wrongAttributeType_ignored() throws Exception {
		var anno = C0xBean.class.getDeclaredField("decimalMax").getAnnotation(DecimalMax.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNull(s.getMaximum());
	}

	@Test void c08_unsupportedConstraint_defaultCase_ignored() throws Exception {
		// A real Jakarta constraint with no case arm in applyJakartaValidation's switch -- falls to default.
		var anno = C08Bean.class.getMethod("isValid").getAnnotation(jakarta.validation.constraints.AssertTrue.class);
		var s = HttpPartSchema.create().applyJakartaValidation(anno).build();
		assertNotNull(s);
	}

	public static class C08Bean {
		@jakarta.validation.constraints.AssertTrue
		public boolean isValid() { return true; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: Per-type "not allowed" attribute combinations (HttpPartSchema constructor validation)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_boolean_disallowedAttributes() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class, l("Attributes not allow for type='boolean'"), ()->HttpPartSchema.create().tBoolean()
			.enum_("a", "b")
			.p("dummy", HttpPartSchema.create().tString())
			.ap(HttpPartSchema.create().tString())
			.exclusiveMaximum(true).maximum(5)
			.exclusiveMinimum(true).minimum(1)
			.uniqueItems(true)
			.cfCsv()
			.items(HttpPartSchema.create().tString())
			.multipleOf(2)
			.maxItems(5L).minItems(1L)
			.maxLength(5L).minLength(1L)
			.maxProperties(5L).minProperties(1L)
			.build()).getMessage();
		for (var attr : new String[]{"enum","properties","additionalProperties","exclusiveMaximum","exclusiveMinimum",
				"uniqueItems","collectionFormat","items","maximum","minimum","multipleOf","maxItems","maxLength","maxProperties","minItems","minLength","minProperties"})
			assertTrue(messages.contains(attr), "Expected message to mention '" + attr + "':\n" + messages);
	}

	@Test void d02_array_disallowedAttributes() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class, l("Attributes not allow for type='array'"), ()->HttpPartSchema.create().tArray()
			.items(HttpPartSchema.create().tString())
			.p("dummy", HttpPartSchema.create().tString())
			.ap(HttpPartSchema.create().tString())
			.exclusiveMaximum(true).maximum(5)
			.exclusiveMinimum(true).minimum(1)
			.multipleOf(2)
			.maxLength(5L).minLength(1L)
			.maxProperties(5L).minProperties(1L)
			.build()).getMessage();
		for (var attr : new String[]{"properties","additionalProperties","exclusiveMaximum","exclusiveMinimum","maximum","minimum","multipleOf","maxLength","minLength","maxProperties","minProperties"})
			assertTrue(messages.contains(attr), "Expected message to mention '" + attr + "':\n" + messages);
	}

	@Test void d03_integer_disallowedAttributes() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class, l("Attributes not allow for type='integer'"), ()->HttpPartSchema.create().tInteger()
			.p("dummy", HttpPartSchema.create().tString())
			.ap(HttpPartSchema.create().tString())
			.uniqueItems(true)
			.cfCsv()
			.items(HttpPartSchema.create().tString())
			.maxItems(5L).minItems(1L)
			.maxLength(5L).minLength(1L)
			.maxProperties(5L).minProperties(1L)
			.build()).getMessage();
		for (var attr : new String[]{"properties","additionalProperties","uniqueItems","collectionFormat","items","maxItems","maxLength","maxProperties","minItems","minLength","minProperties"})
			assertTrue(messages.contains(attr), "Expected message to mention '" + attr + "':\n" + messages);
	}

	@Test void d04_number_disallowedAttributes() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class, l("Attributes not allow for type='number'"), ()->HttpPartSchema.create().tNumber()
			.p("dummy", HttpPartSchema.create().tString())
			.ap(HttpPartSchema.create().tString())
			.uniqueItems(true)
			.cfCsv()
			.items(HttpPartSchema.create().tString())
			.maxItems(5L).minItems(1L)
			.maxLength(5L).minLength(1L)
			.maxProperties(5L).minProperties(1L)
			.build()).getMessage();
		for (var attr : new String[]{"properties","additionalProperties","uniqueItems","collectionFormat","items","maxItems","maxLength","maxProperties","minItems","minLength","minProperties"})
			assertTrue(messages.contains(attr), "Expected message to mention '" + attr + "':\n" + messages);
	}

	@Test void d05_object_disallowedAttributes() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class, l("Attributes not allow for type='object'"), ()->HttpPartSchema.create().tObject()
			.exclusiveMaximum(true).maximum(5)
			.exclusiveMinimum(true).minimum(1)
			.uniqueItems(true)
			.items(HttpPartSchema.create().tString())
			.multipleOf(2)
			.maxItems(5L).minItems(1L)
			.maxLength(5L).minLength(1L)
			.build()).getMessage();
		for (var attr : new String[]{"exclusiveMaximum","exclusiveMinimum","uniqueItems","items","maximum","minimum","multipleOf","maxItems","maxLength","minItems","minLength"})
			assertTrue(messages.contains(attr), "Expected message to mention '" + attr + "':\n" + messages);
	}

	@Test void d06_arrayOfObjects_nonUonFormat_bothErrorsReported() {
		var messages = assertThrowsWithMessage(BasicRuntimeException.class,
			l("Invalid format for type", "Cannot define an array of objects unless array format is 'uon'"),
			()->HttpPartSchema.create().tArray().items(HttpPartSchema.create().tObject()).fByte().build()).getMessage();
		assertNotNull(messages);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: Numeric exclusiveMaximumValue/exclusiveMinimumValue type dispatch (Draft 2020-12)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_exclusiveMaximumValue_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput((short)99);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput((short)100));
	}

	@Test void e02_exclusiveMaximumValue_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput((byte)50);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput((byte)100));
	}

	@Test void e03_exclusiveMaximumValue_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(99L);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(100L));
	}

	@Test void e04_exclusiveMaximumValue_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(new AtomicLong(99));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(new AtomicLong(100)));
	}

	@Test void e05_exclusiveMaximumValue_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMaximumValue(100).build();
		s.validateOutput(BigInteger.valueOf(99));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(BigInteger.valueOf(100)));
	}

	@Test void e06_exclusiveMaximumValue_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMaximumValue(100).build();
		s.validateOutput(99.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(100.0f));
	}

	@Test void e07_exclusiveMaximumValue_double() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMaximumValue(100).build();
		s.validateOutput(99.0);
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(100.0));
	}

	@Test void e08_exclusiveMaximumValue_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMaximumValue(100).build();
		s.validateOutput(BigDecimal.valueOf(99.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Maximum value exceeded", ()->s.validateOutput(BigDecimal.valueOf(100.0)));
	}

	@Test void e09_exclusiveMinimumValue_short() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(0).build();
		s.validateOutput((short)1);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput((short)0));
	}

	@Test void e10_exclusiveMinimumValue_byte() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(0).build();
		s.validateOutput((byte)1);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput((byte)0));
	}

	@Test void e11_exclusiveMinimumValue_long() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(0).build();
		s.validateOutput(1L);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(0L));
	}

	@Test void e12_exclusiveMinimumValue_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(0).build();
		s.validateOutput(new AtomicLong(1));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(new AtomicLong(0)));
	}

	@Test void e13_exclusiveMinimumValue_bigInteger() throws Exception {
		var s = HttpPartSchema.create().tInteger().exclusiveMinimumValue(0).build();
		s.validateOutput(BigInteger.valueOf(1));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(BigInteger.valueOf(0)));
	}

	@Test void e14_exclusiveMinimumValue_float() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMinimumValue(0).build();
		s.validateOutput(1.0f);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(0.0f));
	}

	@Test void e15_exclusiveMinimumValue_double() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMinimumValue(0).build();
		s.validateOutput(1.0);
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(0.0));
	}

	@Test void e16_exclusiveMinimumValue_bigDecimal() throws Exception {
		var s = HttpPartSchema.create().tNumber().exclusiveMinimumValue(0).build();
		s.validateOutput(BigDecimal.valueOf(1.0));
		assertThrowsWithMessage(SchemaValidationException.class, "Minimum value not met", ()->s.validateOutput(BigDecimal.valueOf(0.0)));
	}

	@Test void e17_multipleOf_atomicInteger_and_atomicLong() throws Exception {
		var s = HttpPartSchema.create().tInteger().multipleOf(2).build();
		s.validateOutput(new AtomicInteger(4));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met", ()->s.validateOutput(new AtomicInteger(3)));
		s.validateOutput(new AtomicLong(4));
		assertThrowsWithMessage(SchemaValidationException.class, "Multiple-of not met", ()->s.validateOutput(new AtomicLong(3)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: Low-level format-parser edge cases (date/time/duration/email/hostname/ipv6/uri/uuid/jsonPointer/etc.)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_date_tooShort() {
		var s = HttpPartSchema.create().tString().noValidate().format("date").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1"));
	}

	@Test void f02_date_badSeparatorOrDigits() {
		var s = HttpPartSchema.create().tString().noValidate().format("date").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024.01.15"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01"));
	}

	@Test void f03_date_lineTerminatorTrailing() {
		var s = HttpPartSchema.create().tString().noValidate().format("date").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15\n"));
	}

	@Test void f04_dateTime_missingTimeSeparator() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-1510:30"));
	}

	@Test void f05_dateTime_missingMinuteColon() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15T1030"));
	}

	@Test void f06_dateTime_tooShort() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1"));
	}

	@Test void f07_dateTimeZone_badFractionalSeconds() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time-zone").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15T10:30:45.+05:00"));
	}

	@Test void f08_dateTimeZone_badTimezoneOffset() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time-zone").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15T10:30:45X05:00"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024-01-15T10:30:45+0500"));
	}

	@Test void f09_dateTimeZone_badCore() {
		var s = HttpPartSchema.create().tString().noValidate().format("date-time-zone").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2024/01/15T10:30:45+05:00"));
	}

	@Test void f10_time_zuluSuffix_trailingGarbage() {
		var s = HttpPartSchema.create().tString().noValidate().format("time").build();
		s.validateInput("10:30:45Z");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("10:30:45Zxx"));
	}

	@Test void f11_time_badTimezoneOffset() {
		var s = HttpPartSchema.create().tString().noValidate().format("time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("10:30:45+0500"));
	}

	@Test void f12_time_badCore() {
		var s = HttpPartSchema.create().tString().noValidate().format("time").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("10-30-45"));
	}

	@Test void f13_duration_missingLeadingP() {
		var s = HttpPartSchema.create().tString().noValidate().format("duration").build();
		// Empty string is rejected by the required/empty-value check before the duration-format regex ever runs.
		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("X"));
	}

	@Test void f14_duration_fractionalSeconds() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("duration").build();
		s.validateInput("PT1.5S");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("PT1.S"));
	}

	@Test void f15_duration_trailingGarbageAfterT() {
		var s = HttpPartSchema.create().tString().noValidate().format("duration").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("P1YTX"));
	}

	@Test void f16_email_localPartAllSpecialChars() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		s.validateInput("a.b_c%d+e-f@example.com");
		s.validateInput("user@my-domain.com");
		s.validateInput("user@example.COM");
	}

	@Test void f17_email_invalidLocalChar() {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("us er@example.com"));
	}

	@Test void f18_email_invalidDomainChar() {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("user@dom ain.com"));
	}

	@Test void f19_email_tldTooShortOrNonLetter() {
		var s = HttpPartSchema.create().tString().noValidate().format("email").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("user@example.c"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("user@example.c1"));
	}

	@Test void f20_idnEmail_interiorDotEdgeCases() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("idn-email").build();
		// Dot immediately after '@' or at the very end doesn't count as an interior dot -- no valid TLD separator.
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("user@.com"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("user@example."));
	}

	@Test void f21_hostname_labelBoundaryChars() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("hostname").build();
		s.validateInput("a-b.com");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("a_b.com"));
	}

	@Test void f22_ipv6_full() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv6").build();
		s.validateInput("2001:0db8:0000:0000:0000:0000:0000:0001");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2001:0db8:0000:0000:0000:0000:0000"));
	}

	@Test void f23_ipv6_trailingColon() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv6").build();
		s.validateInput("2001:0db8:0000:0000:0000:0000:0000:");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput(":"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("2001:0db8:zzzz:0000:0000:0000:0000:0000:"));
	}

	@Test void f24_ipv6_leadingDoubleColon_tooManyGroups() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("ipv6").build();
		s.validateInput("::1");
		s.validateInput("::1:2:3:4:5:6");
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("::1:2:3:4:5:6:7:8"));
	}

	@Test void f25_uriTemplate_whitespaceRejected() {
		var s = HttpPartSchema.create().tString().noValidate().format("uri-template").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("/foo/{id} bar"));
	}

	@Test void f26_jsonPointer_nonSlashPrefixRejected() {
		var s = HttpPartSchema.create().tString().noValidate().format("json-pointer").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("foo/bar"));
	}

	@Test void f27_relativeJsonPointer_variants() throws Exception {
		var s = HttpPartSchema.create().tString().noValidate().format("relative-json-pointer").build();
		s.validateInput("0");
		s.validateInput("12/foo");
		s.validateInput("1#");
		// Empty string is rejected by the required/empty-value check before the format regex ever runs.
		assertThrowsWithMessage(SchemaValidationException.class, "Empty value not allowed", ()->s.validateInput(""));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("01"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1x"));
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("1#x"));
	}

	@Test void f28_uuid_dashPositionMismatch() {
		var s = HttpPartSchema.create().tString().noValidate().format("uuid").build();
		assertThrowsWithMessage(SchemaValidationException.class, "Value does not match expected format", ()->s.validateInput("123456789-1234-1234-1234-123456789012"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: apply(Annotation) top-level dispatch -- Items/SubItems/HttpPartMarshalling instanceof branches
	//-----------------------------------------------------------------------------------------------------------------

	static class G01Bean {
		@org.apache.juneau.commons.Items(type="string", pattern="^[a-z]+$")
		public String items;
		@org.apache.juneau.commons.SubItems(type="integer", minimum="1")
		public String subItems;
		@HttpPartMarshalling(parser=SimplePartParser.class, serializer=SimplePartSerializer.class)
		public String marshalling;
	}

	@Test void g01_apply_items_viaGenericAnnotationEntryPoint() throws Exception {
		var anno = (Annotation)G01Bean.class.getField("items").getAnnotation(org.apache.juneau.commons.Items.class);
		var s = HttpPartSchema.create().tArray().apply(anno).build();
		assertEquals("^[a-z]+$", s.getPattern().pattern());
	}

	@Test void g02_apply_subItems_viaGenericAnnotationEntryPoint() throws Exception {
		var anno = (Annotation)G01Bean.class.getField("subItems").getAnnotation(org.apache.juneau.commons.SubItems.class);
		var s = HttpPartSchema.create().tInteger().apply(anno).build();
		assertEquals(1, s.getMinimum().intValue());
	}

	@Test void g03_apply_httpPartMarshalling_viaGenericAnnotationEntryPoint() throws Exception {
		var anno = (Annotation)G01Bean.class.getField("marshalling").getAnnotation(HttpPartMarshalling.class);
		var s = HttpPartSchema.create().tString().apply(anno).build();
		assertEquals(SimplePartParser.class, s.getParser());
		assertEquals(SimplePartSerializer.class, s.getSerializer());
	}

	@Test void g04_apply_httpPartMarshalling_voidDefaults_ignored() {
		// Default parser()/serializer() is HttpPartXxx.Void.class, which parser(Class)/serializer(Class) ignore.
		var anno = new HttpPartMarshalling() {
			@Override public Class<? extends Annotation> annotationType() { return HttpPartMarshalling.class; }
			@Override public Class<? extends HttpPartParser> parser() { return HttpPartParser.Void.class; }
			@Override public Class<? extends HttpPartSerializer> serializer() { return HttpPartSerializer.Void.class; }
		};
		var s = HttpPartSchema.create().tString().apply((Annotation)anno).build();
		assertNull(s.getParser());
		assertNull(s.getSerializer());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// H: applyHttpAnnotation switch dispatch + getAnnotationString/getAnnotationIntArray reflection guards
	// These fake annotations share simple names with the real HTTP-part annotations (Path/Response/StatusCode/...,
	// defined in juneau-rest-common, not a juneau-marshall dependency) purely so applyHttpAnnotation's switch on
	// annotationType().getSimpleName() dispatches into the same case arms.
	//-----------------------------------------------------------------------------------------------------------------

	@Retention(RetentionPolicy.RUNTIME)
	private @interface PathRemainder {
		String def() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Path {
		String name() default "";
		String value() default "";
		String def() default "";
	}

	// A separate holder with its own nested "Path" annotation (shadows the outer one within this scope) that
	// declares no def() method at all -- triggers getAnnotationString's NoSuchMethodException guard, so def
	// stays null, while annotationType().getSimpleName() is still "Path" so applyHttpAnnotation's switch still
	// dispatches to the same case arm.
	static class H05Holder {
		@Retention(RetentionPolicy.RUNTIME)
		@interface Path {
			String name() default "";
		}
		@Path(name="userId2")
		public String pathNoDefAttr;
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Response {}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface StatusCode {
		int[] value() default {};
	}

	// A "StatusCode" annotation whose "value" attribute is the wrong return type -- triggers
	// getAnnotationIntArray's return-type-mismatch guard (falls through to the empty-array default).
	@Retention(RetentionPolicy.RUNTIME)
	private @interface StatusCodeWrongType {
		String value() default "not-an-int-array";
	}

	// A "StatusCode" annotation missing the "value" attribute entirely -- triggers getAnnotationIntArray's
	// NoSuchMethodException guard.
	@Retention(RetentionPolicy.RUNTIME)
	private @interface StatusCodeNoValue {}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface UnrecognizedHttpPartAnnotation {
		String name() default "";
		String value() default "";
		String def() default "";
	}

	static class H01Bean {
		@PathRemainder(def="fallback")
		public String pathRemainder;
		@Path(name="userId")
		public String pathNoLeadingSlash;
		@Path(value="/foo/*")
		public String pathLeadingSlash;
		@Path(name="userId", def="_NONE_")
		public String pathDefNone;
		@Response
		public String response;
		@StatusCode(value={200,201})
		public String statusCode;
		@StatusCodeWrongType
		public String statusCodeWrongType;
		@StatusCodeNoValue
		public String statusCodeNoValue;
		@UnrecognizedHttpPartAnnotation(name="foo", def="bar")
		public String unrecognized;
	}

	private static Annotation annoOf(String fieldName, Class<? extends Annotation> annoType) throws Exception {
		return H01Bean.class.getField(fieldName).getAnnotation(annoType);
	}

	@Test void h01_pathRemainder() throws Exception {
		var s = HttpPartSchema.create().apply(annoOf("pathRemainder", PathRemainder.class)).build();
		assertEquals("/*", s.getName());
		assertEquals("fallback", s.getDefault());
		assertFalse(s.isRequired());
	}

	@Test void h02_path_noLeadingSlash_defIsOrdinaryValue_notRequired() throws Exception {
		// def() defaults to "" here (a real, non-null, non-NONE value) -- so both operands of
		// `def == null || eq(NONE, def)` are false and required ends up false.
		var s = HttpPartSchema.create().apply(annoOf("pathNoLeadingSlash", Path.class)).build();
		assertEquals("userId", s.getName());
		assertFalse(s.isRequired());
	}

	@Test void h03_path_leadingSlash_notRequired() throws Exception {
		var s = HttpPartSchema.create().apply(annoOf("pathLeadingSlash", Path.class)).build();
		assertEquals("/foo/*", s.getName());
		assertFalse(s.isRequired());
	}

	@Test void h04_path_defEqualsNone_requiredStillTrue() throws Exception {
		var s = HttpPartSchema.create().apply(annoOf("pathDefNone", Path.class)).build();
		assertTrue(s.isRequired());
	}

	@Test void h05_path_missingDefAttribute_getAnnotationStringReturnsNull() throws Exception {
		// H05Holder.Path has no def() method -- getAnnotationString hits its NoSuchMethodException guard,
		// so def is null and required(def == null || ...) short-circuits true.
		var anno = (Annotation)H05Holder.class.getField("pathNoDefAttr").getAnnotation(H05Holder.Path.class);
		var s = HttpPartSchema.create().apply(anno).build();
		assertEquals("userId2", s.getName());
		assertTrue(s.isRequired());
	}

	@Test void h06_response_allowsEmptyValue_notRequired() throws Exception {
		var s = HttpPartSchema.create().apply(annoOf("response", Response.class)).build();
		assertFalse(s.isRequired());
	}

	@Test void h07_statusCode_setsCodes() throws Exception {
		var b = HttpPartSchema.create();
		b.apply(annoOf("statusCode", StatusCode.class));
		assertEquals(set(200, 201), b.codes);
	}

	@Test void h08_statusCode_wrongAttributeType_noCodesSet() throws Exception {
		var b = HttpPartSchema.create();
		b.apply(annoOf("statusCodeWrongType", StatusCodeWrongType.class));
		assertNull(b.codes);
	}

	@Test void h09_statusCode_missingValueAttribute_noCodesSet() throws Exception {
		var b = HttpPartSchema.create();
		b.apply(annoOf("statusCodeNoValue", StatusCodeNoValue.class));
		assertNull(b.codes);
	}

	@Test void h10_unrecognizedAnnotation_defaultCase_setsNameAndDefault() throws Exception {
		var s = HttpPartSchema.create().apply(annoOf("unrecognized", UnrecognizedHttpPartAnnotation.class)).build();
		assertEquals("foo", s.getName());
		assertEquals("bar", s.getDefault());
	}
}
