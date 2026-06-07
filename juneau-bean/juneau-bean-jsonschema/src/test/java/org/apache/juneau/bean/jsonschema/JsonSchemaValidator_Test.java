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
package org.apache.juneau.bean.jsonschema;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link JsonSchemaValidator} in isolation against {@link JsonSchema} beans.
 */
@SuppressWarnings({
	"java:S5778" // assertThrows lambdas intentionally build the schema/validator and supply the value inline; the throwing call is unambiguous (validate) and hoisting validator+argument locals across ~40 tests would bloat the file without clarity gain.
})
class JsonSchemaValidator_Test extends TestBase {

	private static JsonSchemaValidator v(JsonSchema s) {
		return JsonSchemaValidator.of(s);
	}

	// =================================================================================================================
	// Type / enum / const
	// =================================================================================================================

	@Test void a01_typeString_match() {
		v(new JsonSchema().setType(JsonType.STRING)).validate("hello");
	}

	@Test void a01_typeString_mismatch() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.STRING)).validate(42));
	}

	@Test void a01_typeArray_acceptsAnyMember() {
		var s = new JsonSchema().setType(new JsonTypeArray(JsonType.STRING, JsonType.INTEGER));
		v(s).validate("hello");
		v(s).validate(7);
	}

	@Test void a02_enum_inSet() {
		v(new JsonSchema().addEnum("A", "B", "C")).validate("B");
	}

	@Test void a02_enum_notInSet() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().addEnum("A", "B", "C")).validate("D"));
	}

	@Test void a02_enum_numericStringCoercion() {
		v(new JsonSchema().addEnum("1", "2", "3")).validate(2);
	}

	@Test void a03_const_match() {
		v(new JsonSchema().setConst("ACTIVE")).validate("ACTIVE");
	}

	@Test void a03_const_mismatch() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst("ACTIVE")).validate("INACTIVE"));
	}

	// =================================================================================================================
	// String constraints
	// =================================================================================================================

	@Test void b01_stringMinLength_pass() {
		v(new JsonSchema().setMinLength(2)).validate("ok");
	}

	@Test void b01_stringMinLength_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinLength(2)).validate("a"));
	}

	@Test void b01_stringMaxLength_pass() {
		v(new JsonSchema().setMaxLength(4)).validate("ok");
	}

	@Test void b01_stringMaxLength_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMaxLength(4)).validate("toolong"));
	}

	@Test void b02_pattern_match() {
		v(new JsonSchema().setPattern("^[a-z]+$")).validate("abc");
	}

	@Test void b02_pattern_noMatch() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setPattern("^[a-z]+$")).validate("ABC"));
	}

	// =================================================================================================================
	// Numeric constraints
	// =================================================================================================================

	@Test void c01_minimum_pass() {
		v(new JsonSchema().setMinimum(0)).validate(0);
		v(new JsonSchema().setMinimum(0)).validate(5);
	}

	@Test void c01_minimum_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinimum(0)).validate(-1));
	}

	@Test void c01_maximum_pass() {
		v(new JsonSchema().setMaximum(100)).validate(99);
		v(new JsonSchema().setMaximum(100)).validate(100);
	}

	@Test void c01_maximum_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMaximum(100)).validate(101));
	}

	@Test void c02_exclusiveMinimum_pass() {
		v(new JsonSchema().setExclusiveMinimum(0)).validate(1);
	}

	@Test void c02_exclusiveMinimum_boundaryFail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setExclusiveMinimum(0)).validate(0));
	}

	@Test void c02_exclusiveMaximum_pass() {
		v(new JsonSchema().setExclusiveMaximum(100)).validate(99);
	}

	@Test void c02_exclusiveMaximum_boundaryFail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setExclusiveMaximum(100)).validate(100));
	}

	@Test void c03_multipleOf_exact() {
		v(new JsonSchema().setMultipleOf(5)).validate(25);
	}

	@Test void c03_multipleOf_decimal() {
		v(new JsonSchema().setMultipleOf(new BigDecimal("0.5"))).validate(new BigDecimal("2.5"));
	}

	@Test void c03_multipleOf_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMultipleOf(5)).validate(7));
	}

	@Test void c03_multipleOf_zeroIsNoop() {
		v(new JsonSchema().setMultipleOf(0)).validate(7);
	}

	// =================================================================================================================
	// Array constraints
	// =================================================================================================================

	@Test void d01_minItems_pass() {
		v(new JsonSchema().setMinItems(1)).validate(List.of("a"));
	}

	@Test void d01_minItems_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinItems(1)).validate(List.of()));
	}

	@Test void d01_maxItems_pass() {
		v(new JsonSchema().setMaxItems(2)).validate(List.of("a", "b"));
	}

	@Test void d01_maxItems_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMaxItems(2)).validate(List.of("a", "b", "c")));
	}

	@Test void d02_uniqueItems_pass() {
		v(new JsonSchema().setUniqueItems(true)).validate(List.of("a", "b", "c"));
	}

	@Test void d02_uniqueItems_fail() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setUniqueItems(true)).validate(List.of("a", "b", "a")));
	}

	@Test void d03_items_recurse_pass() {
		var schema = new JsonSchema().setItems(new JsonSchema().setType(JsonType.STRING).setMinLength(2));
		v(schema).validate(List.of("ab", "cd"));
	}

	@Test void d03_items_recurse_fail() {
		var schema = new JsonSchema().setItems(new JsonSchema().setType(JsonType.STRING).setMinLength(2));
		assertThrows(SchemaValidationException.class, () -> v(schema).validate(List.of("ab", "x")));
	}

	@Test void d04_arrayPrimitive_acceptsJavaArray() {
		var schema = new JsonSchema().setMinItems(1).setMaxItems(3);
		v(schema).validate(new String[]{"a", "b"});
	}

	// =================================================================================================================
	// Object constraints
	// =================================================================================================================

	@Test void e01_required_present() {
		var schema = new JsonSchema().addRequired("name");
		v(schema).validate(Map.of("name", "abc", "age", 30));
	}

	@Test void e01_required_missing() {
		var schema = new JsonSchema().addRequired("name");
		assertThrows(SchemaValidationException.class, () -> v(schema).validate(Map.of("age", 30)));
	}

	@Test void e02_minMaxProperties() {
		v(new JsonSchema().setMinProperties(1).setMaxProperties(3)).validate(Map.of("a", 1, "b", 2));
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinProperties(1)).validate(Map.of()));
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMaxProperties(2)).validate(Map.of("a", 1, "b", 2, "c", 3)));
	}

	@Test void e03_properties_recurse_pass() {
		var schema = new JsonSchema()
			.addProperties(new JsonSchemaProperty("name", JsonType.STRING).setMinLength(2));
		v(schema).validate(Map.of("name", "abc"));
	}

	@Test void e03_properties_recurse_fail() {
		var schema = new JsonSchema()
			.addProperties(new JsonSchemaProperty("name", JsonType.STRING).setMinLength(2));
		assertThrows(SchemaValidationException.class, () -> v(schema).validate(Map.of("name", "a")));
	}

	// =================================================================================================================
	// Null / empty schema / round-trip
	// =================================================================================================================

	@Test void f01_nullValue_emptyConstraints() {
		v(new JsonSchema().setType(JsonType.STRING)).validate(null);
	}

	@Test void f02_emptySchema_isNoop() {
		v(new JsonSchema()).validate("anything");
		v(new JsonSchema()).validate(42);
		v(new JsonSchema()).validate(null);
	}

	@Test void f03_jsonMap_factory_buildsValidator() {
		var jm = new JsonMap().append("minLength", 2).append("maxLength", 4);
		var validator = JsonSchemaValidator.of(jm);
		validator.validate("abc");
		assertThrows(SchemaValidationException.class, () -> validator.validate("a"));
	}

	@Test void f04_getSchema_returnsInputBean() {
		var s = new JsonSchema().setMinLength(2);
		var validator = JsonSchemaValidator.of(s);
		assertSame(s, validator.getSchema());
	}

	@Test void f05_typeCheckBeforeStringChecks() {
		// type=string but value is integer; type check should fire first.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.STRING).setMinLength(2)).validate(7));
	}

	@Test void f06_typeInteger_acceptsLongFloatWithoutFraction() {
		v(new JsonSchema().setType(JsonType.INTEGER)).validate(3L);
		v(new JsonSchema().setType(JsonType.INTEGER)).validate(3.0);
	}

	@Test void f06_typeInteger_rejectsFractional() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.INTEGER)).validate(3.5));
	}

	@Test void f07_typeBoolean_acceptsBoolean() {
		v(new JsonSchema().setType(JsonType.BOOLEAN)).validate(true);
	}

	@Test void f07_typeBoolean_rejectsString() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.BOOLEAN)).validate("true"));
	}

	@Test void f08_typeArray_rejectsScalar() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.ARRAY)).validate("scalar"));
	}

	@Test void f09_typeAny_alwaysMatches() {
		v(new JsonSchema().setType(JsonType.ANY)).validate("anything");
		v(new JsonSchema().setType(JsonType.ANY)).validate(123);
	}

	@Test void f10_typeNull_rejectsNonNull() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.NULL)).validate("hello"));
	}

	@Test void g01_factory_rejectsNullSchema() {
		assertThrows(Exception.class, () -> JsonSchemaValidator.of((JsonSchema) null));
	}

	@Test void g02_factory_rejectsNullMap() {
		assertThrows(Exception.class, () -> JsonSchemaValidator.of((JsonMap) null));
	}

	// =================================================================================================================
	// Branch / edge coverage
	// =================================================================================================================

	@Test void h01_validateString_appliesToCharacter() {
		v(new JsonSchema().setMinLength(1)).validate('x');
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinLength(2)).validate('x'));
	}

	@Test void h02_enum_emptyListIsNoop() {
		v(new JsonSchema().setEnum(new ArrayList<>())).validate("anything");
	}

	@Test void h03_typeArray_emptyIsNoop() {
		v(new JsonSchema().setType(new JsonTypeArray())).validate("anything");
	}

	@Test void h04_typeArray_noneMatchFails() {
		var s = new JsonSchema().setType(new JsonTypeArray(JsonType.STRING, JsonType.BOOLEAN));
		assertThrows(SchemaValidationException.class, () -> v(s).validate(42));
	}

	@Test void h05_typeBoolean_matchesType_arm() {
		v(new JsonSchema().setType(JsonType.BOOLEAN)).validate(false);
	}

	@Test void h06_typeNumber_acceptsAnyNumber() {
		v(new JsonSchema().setType(JsonType.NUMBER)).validate(3.14);
		v(new JsonSchema().setType(JsonType.NUMBER)).validate(7L);
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.NUMBER)).validate("nope"));
	}

	@Test void h07_typeArray_acceptsJavaArrayInMatchesType() {
		// Drives the `value.getClass().isArray()` branch inside matchesType (separate from validateArray dispatch).
		v(new JsonSchema().setType(JsonType.ARRAY).setMinItems(1)).validate(new int[]{1, 2});
	}

	@Test void h08_typeObject_acceptsBeanLikeValue() {
		// Java bean (non-Map, non-array, non-Number, non-Boolean, non-CharSequence) — drives isBeanLike==true arm.
		v(new JsonSchema().setType(JsonType.OBJECT)).validate(new Object());
	}

	@Test void h09_typeNull_matchesNull() {
		// validateType is skipped when value==null in validateAgainst; matchesType NULL still covered via type-array.
		var s = new JsonSchema().setType(new JsonTypeArray(JsonType.STRING, JsonType.NULL));
		v(s).validate("ok");
	}

	@Test void h10_integral_byteShortBigInteger() {
		var s = new JsonSchema().setType(JsonType.INTEGER);
		v(s).validate((byte) 1);
		v(s).validate((short) 1);
		v(s).validate(new java.math.BigInteger("12345"));
	}

	@Test void h11_integral_rejectsFloatWithFraction() {
		var s = new JsonSchema().setType(JsonType.INTEGER);
		assertThrows(SchemaValidationException.class, () -> v(s).validate(1.5f));
	}

	@Test void h12_integral_rejectsNonNumber() {
		// Drives the trailing `return false` in isIntegralValue (value is non-null but not a Number after the
		// initial fast-path checks fail).  Reached indirectly via type=[integer] + non-number value.
		var s = new JsonSchema().setType(new JsonTypeArray(JsonType.INTEGER, JsonType.STRING));
		v(s).validate("not a number");
	}

	@Test void h13_array_uniqueItems_singletonNoop() {
		// hasDuplicates fast-path: c.size() < 2.
		v(new JsonSchema().setUniqueItems(true)).validate(List.of("only"));
		v(new JsonSchema().setUniqueItems(true)).validate(List.of());
	}

	@Test void h14_objectProperties_missingKeyIsSkipped() {
		// validateObject's properties loop's containsKey==false branch.
		var schema = new JsonSchema()
			.addProperties(new JsonSchemaProperty("name", JsonType.STRING).setMinLength(2));
		v(schema).validate(Map.of("other", "ok"));
	}

	@Test void h15_arrayItems_patternComputedPerElement() {
		// Items schema carries a pattern; recursion passes patternOverride=null so validateString must
		// compile the pattern on the inner schema.
		var schema = new JsonSchema().setItems(new JsonSchema().setType(JsonType.STRING).setPattern("^[a-z]+$"));
		v(schema).validate(List.of("abc", "def"));
		assertThrows(SchemaValidationException.class, () -> v(schema).validate(List.of("abc", "DEF")));
	}

	@Test void h16_toBigDecimal_acceptsBigDecimalInputDirectly() {
		v(new JsonSchema().setMinimum(new BigDecimal("0.5"))).validate(new BigDecimal("1"));
	}

	@Test void h17_toBigDecimal_acceptsBigIntegerInput() {
		v(new JsonSchema().setMinimum(new BigInteger("10"))).validate(20);
	}

	@Test void h18_toBigDecimal_acceptsByteShortInputs() {
		v(new JsonSchema().setMinimum((byte) 0)).validate(1);
		v(new JsonSchema().setMinimum((short) 0)).validate(1);
	}

	@Test void h19_jsonEquals_numberToString_const() {
		// const is a string "5"; value is integer 5 — drives the (Number, CharSequence) branch.
		v(new JsonSchema().setConst("5")).validate(5);
	}

	@Test void h20_jsonEquals_stringToNumber_enum() {
		// enum is integer 5; value is string "5" — drives the (CharSequence, Number) branch.
		v(new JsonSchema().addEnum(5)).validate("5");
	}

	@Test void h21_jsonEquals_charSequenceEquality() {
		// Both are CharSequence with the same content — drives the CharSequence-vs-CharSequence branch.
		v(new JsonSchema().setConst(new StringBuilder("abc"))).validate("abc");
	}

	@Test void h22_jsonEquals_objectFallback_equals() {
		// Non-numeric, non-string, non-charsequence values — drives the a.equals(b) branch.
		v(new JsonSchema().setConst(Boolean.TRUE)).validate(true);
	}

	@Test void h23_jsonEquals_nonComparableFallback() {
		// Different types and a.equals(b) is false — drives the JSON serializer fallback.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst(List.of("a", "b"))).validate(List.of("c", "d")));
	}

	@Test void h24_numericValidation_nonFiniteValueFails() {
		// Drives the toBigDecimal NumberFormatException catch in validateNumber.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setMinimum(0)).validate(Double.NaN));
	}

	@Test void h25_jsonEquals_numberStringNotParseable() {
		// Drives the tryAsBigDecimal == null branch in the (Number, CharSequence) leg.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst(5)).validate("not-a-number"));
	}

	@Test void h26_jsonEquals_stringNumberNotParseable() {
		// Drives the tryAsBigDecimal == null branch in the (CharSequence, Number) leg.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst("not-a-number")).validate(5));
	}

	@Test void h27_uniqueItems_nullElementsAreEqual() {
		// Drives jsonEquals(null, null) via duplicate-null detection.
		var withDups = Arrays.asList(null, null);
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setUniqueItems(true)).validate(withDups));
	}

	@Test void h28_uniqueItems_nullVsNonNullNotEqual() {
		// Drives jsonEquals's `a == null || b == null` branch with mismatched nullness.
		var mixed = Arrays.asList(null, 5);
		v(new JsonSchema().setUniqueItems(true)).validate(mixed);
	}

	@Test void h29_matchesType_object_rejectsArray() {
		// matchesType OBJECT arm: Map || bean-like. Arrays / collections are not bean-like.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.OBJECT)).validate(new int[]{1}));
	}

	@Test void h30_matchesType_object_rejectsCollection() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.OBJECT)).validate(List.of(1, 2)));
	}

	@Test void h31_matchesType_object_rejectsNumber() {
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setType(JsonType.OBJECT)).validate(42));
	}

	@Test void h32_jsonEquals_numberToNumber_const() {
		// Drives the (Number, Number) compareTo == 0 / != 0 arms in jsonEquals.
		v(new JsonSchema().setConst(5)).validate(5);
		v(new JsonSchema().setConst(5)).validate(5L);
		v(new JsonSchema().setConst(new BigDecimal("5.0"))).validate(5);
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst(5)).validate(6));
	}

	@Test void h33_jsonEquals_objectFallback_unequal() {
		// Drives a.equals(b) == false leg with non-collection objects that go to the JSON fallback.
		assertThrows(SchemaValidationException.class, () -> v(new JsonSchema().setConst("abc")).validate(Boolean.FALSE));
	}
}
