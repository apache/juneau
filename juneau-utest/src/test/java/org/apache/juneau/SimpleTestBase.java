// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.*;

/**
 * Base class for all JUnit 5 unit tests.
 */
@TestMethodOrder(MethodName.class)
public abstract class SimpleTestBase {

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	protected static void assertJson(Object value, String json) {
		AssertionHelpers.assertJson(value, json);
	}

	protected static void assertLines(String expected, Object value) {
		assertEquals(expected, Utils.readable(value).replaceAll("\\r?\\n", "|"));
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJsonContains(Object value, String json) {
		AssertionHelpers.assertJsonContains(value, json);
	}
	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	protected static void assertTypeAndJson(Object value, Class<?> c, String json) {
		AssertionHelpers.assertTypeAndJson(value, c, json);
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	protected static void assertType(Class<?> c, Object value) {
		AssertionHelpers.assertType(c, value);
	}

	/**
	 * Asserts the serialized representation of the specified object.
	 */
	protected static void assertSerialized(Object value, WriterSerializer s, String json) {
		AssertionHelpers.assertSerialized(value, s, json);
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	protected static void assertString(String expected, Object actual) {
		AssertionHelpers.assertString(expected, actual);
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	public static void assertString(String expected, Object actual, Supplier<String> messageSupplier) {
		AssertionHelpers.assertString(expected, actual, messageSupplier);
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	protected static void assertContains(String expected, Object actual) {
		AssertionHelpers.assertContains(expected, actual);
	}

	protected static void assertContainsAll(String expected, Object actual) {
		AssertionHelpers.assertContainsAll(expected, actual);
	}

	protected static void assertEmpty(Optional<?> o) {
		AssertionHelpers.assertEmpty(o);
	}

	protected static void assertPresent(Optional<?> o) {
		AssertionHelpers.assertPresent(o);
	}

	/**
	 * Asserts the entries in an array matches the expected strings after they've been made readable.
	 */
	protected static void assertArray(Object array, Object...expected) {
		AssertionHelpers.assertArray(array, expected);
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	protected static void assertList(List<?> list, Object...expected) {
		AssertionHelpers.assertList(list, expected);
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	protected static void assertStream(Stream<?> stream, Object...expected) {
		AssertionHelpers.assertStream(stream, expected);
	}

	protected static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		return AssertionHelpers.assertThrowsWithMessage(expectedType, expectedSubstring, executable);
	}

	protected static <T extends Throwable> T assertThrowable(Class<T> expectedType, String expectedSubstring, T t) {
		var messages = AssertionHelpers.getMessages(t);
		assertTrue(messages.contains(expectedSubstring), ss("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	/**
	 * Asserts the entries in a map matches the expected strings after they've been made readable.
	 */
	protected static void assertMap(Map<?,?> map, String...expected) {
		AssertionHelpers.assertMap(map, expected);
	}

	/**
	 * Asserts that a collection is not null or empty.
	 */
	protected static void assertNotEmpty(Collection<?> c) {
		AssertionHelpers.assertNotEmpty(c);
	}

	/**
	 * Asserts that a maps is not null or empty.
	 */
	protected static void assertNotEmpty(Map<?,?> c) {
		AssertionHelpers.assertNotEmpty(c);
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	protected static void assertEmpty(Collection<?> c) {
		AssertionHelpers.assertEmpty(c);
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	protected static void assertStringEmpty(Object s) {
		assertNotNull(s);
		assertTrue(Utils.readable(s).isEmpty());
	}

	/**
	 * Asserts that a collection is not null and of the specified size.
	 */
	protected static void assertSize(int expected, Collection<?> c) {
		AssertionHelpers.assertSize(expected, c);
	}

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values after being converted to readable strings.
	 *
	 * Example:
	 * 	assertBean(
	 * 		myBean,
	 * 		"prop1,prop2,prop3"
	 * 		"val1,val2,val3"
	 * 	);
	 *
	 * Subsets of properties on nested beans can also be specified by appending arbitrarily-nested "{...}" constructs to property names.
	 * 	assertBean(
	 * 		myBean,
	 * 		"prop1{prop1a,prop1b},prop2,prop3"
	 * 		"val1{val1a,val1b},val2,val3"
	 * 	);
	 * This also works for lists and arrays when numeric subproperty names are used.
	 * 	assertBean(
	 * 		myBean,
	 * 		"myListProp{0,1,2},prop2,prop3"
	 * 		"val1{val1a,val1b,val1c},val2,val3"
	 * 	);
	 */
	protected static void assertBean(Object o, String fields, String value) {
		AssertionHelpers.assertBean(o, fields, value);
	}

	/**
	 * Asserts that the values in the specified map are the specified values after being converted to readable strings.
	 * Example:
	 * 	assertMap(
	 * 		myMap,
	 * 		"prop1,prop2,prop3"
	 * 		"val1,val2,val3"
	 * 	);
	 */
	protected static void assertMap(Map<?,?> o, String fields, String value) {
		AssertionHelpers.assertMap(o, fields, value);
	}

	/**
	 * Asserts that the values of the specified fields match the list of beans after being converted to readable strings.
	 * @param l The list of beans to check.
	 * @param fields A comma-delimited list of bean property names.
	 * @param values The comma-delimited list of values for each bean.
	 */
	@SuppressWarnings("rawtypes")
	protected static void assertBeans(Collection l, String fields, Object...values) {
		AssertionHelpers.assertBeans(l, fields, values);
	}

	protected static void assertNotEqualsAny(Object o, Object...values) {
		AssertionHelpers.assertNotEqualsAny(o, values);
	}

	protected static void assertEqualsAll(Object...values) {
		AssertionHelpers.assertEqualsAll(values);
	}

	protected static void assertJsonMatches(Object o, String pattern) throws AssertionError {
		var json = json(o);
		assertTrue(StringUtils.getMatchPattern(pattern).matcher(json).matches(), ss("JSON did not match pattern.\njson={0}", json));
	}

	protected static void assertMatches(Object o, String pattern) throws AssertionError {
		var text = Utils.readable(o);
		assertTrue(StringUtils.getMatchPattern(pattern).matcher(text).matches(), ss("Text did not match pattern.\ntext={0}", text));
	}

	/**
	 * Creates a map wrapper around a bean so that get/set operations on properties
	 * can be done via generic get()/put() methods.
	 */
	public static <T> BeanMap<T> beanMap(T bean) {
		return BeanContext.DEFAULT_SESSION.toBeanMap(bean);
	}

	/**
	 * Creates an array of objects.
	 */
	@SafeVarargs
	protected static <T> T[] a(T...x) {
		return x;
	}

	public static void assertSameObject(Object o1, Object o2) {
		assertTrue(o1 == o2, "Not the same object.");
	}

	/**
	 * Simplified string supplier with message arguments.
	 */
	public static Supplier<String> ss(String pattern, Object...args) {
		return AssertionHelpers.ss(pattern, args);
	}

	public static String json(Object o) {
		return Json5.DEFAULT.write(o);
	}

	public static String s(Object o) {
		return StringUtils.stringify(o);
	}

	@Deprecated
	protected static StringAssertion assertString(Object o) {
		return org.apache.juneau.assertions.Assertions.assertString(o);
	}
}