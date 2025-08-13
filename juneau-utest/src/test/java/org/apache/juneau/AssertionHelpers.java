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

import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.StringUtils.ne;
import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.common.internal.Utils.cdl;
import static org.apache.juneau.common.internal.Utils.eq;
import static org.apache.juneau.common.internal.Utils.ne;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

public class AssertionHelpers {

	/**
	 * Asserts value when stringified matches the specified pattern.
	 */
	public static Object assertMatches(String pattern, Object value) {
		var m = getMatchPattern(pattern).matcher(s(value));
		if (! m.matches()) {
			var msg = "Pattern didn't match: \n\tExpected:\n"+pattern+"\n\tActual:\n"+value;
			System.err.println(msg);  // For easier debugging.
			fail(msg);
		}
		return value;
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJson(Object value, String json) {
		assertEquals(json, Json5.DEFAULT.write(value));
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJsonContains(Object value, String json) {
		assertContains(json, Json5.DEFAULT.write(value));
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertTypeAndJson(Object value, Class<?> c, String json) {
		assertTrue(c.isInstance(value), "Incorrect type.");
		assertEquals(json, Json5.DEFAULT.write(value));
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertType(Class<?> c, Object value) {
		assertTrue(c.isInstance(value), "Incorrect type.");
	}

	/**
	 * Asserts the serialized representation of the specified object.
	 */
	public static void assertSerialized(Object value, WriterSerializer s, String json) {
		assertEquals(json, s.toString(value));
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	public static void assertString(String expected, Object actual) {
		assertEquals(expected, r(actual));
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	public static void assertString(String expected, Object actual, Supplier<String> messageSupplier) {
		assertEquals(expected, r(actual), messageSupplier);
	}

	/**
	 * Asserts an object matches the expected string after it's been made readable.
	 */
	public static void assertContains(String expected, Object actual) {
		var a2 = r(actual);
		assertTrue(a2.contains(expected), ss("String did not contain expected substring.  expected={0}, actual={1}", expected, a2));
	}

	public static void assertContainsAll(String expected, Object actual) {
		var a2 = r(actual);
		for (var e : StringUtils.split(expected))
			assertTrue(a2.contains(e), ss("String did not contain expected substring.  expected={0}, actual={1}", e, a2));
	}

	public static void assertEmpty(Optional<?> o) {
		assertTrue(o != null && o.isEmpty(), "Optional was not empty.");
	}

	public static void assertPresent(Optional<?> o) {
		assertTrue(o != null && o.isPresent(), "Optional was not present.");
	}

	/**
	 * Asserts the entries in an array matches the expected strings after they've been made readable.
	 */
	public static void assertArray(Object array, Object...expected) {
		if (expected.length == 1 && expected[0] instanceof String && s(expected[0]).contains(","))
			expected = s(expected[0]).charAt(0) == '>' ? new String[]{s(expected[0]).substring(1)} : StringUtils.split(s(expected[0]));
		if (Array.getLength(array) != expected.length)
			fail(ss("Wrong array length.  expected={0}, actual={1}", expected.length, Array.getLength(array)));
		for (var i = 0; i < expected.length; i++) {
			var x = Array.get(array, i);
			if (expected[i] instanceof String e) {
				if (ne(r(x), e))
					fail(ss("Element at index {0} did not match.  expected={1}, actual={2}", i, e, r(x)));
			} else if (expected[i] instanceof Predicate e) {
				if (! e.test(x))
					fail(ss("Element at index {0} did pass predicate.  actual={1}", i, r(x)));
			} else {
				if (ne(expected[i], x))
					fail(ss("Element at index {0} did not match.  expected={1}, actual={2}", i, r(expected[i]), r(x)));
			}
		}
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	public static void assertList(List<?> list, Object...expected) {
		if (expected.length == 1 && expected[0] instanceof String && s(expected[0]).contains(","))
			expected = s(expected[0]).charAt(0) == '>' ? new String[]{s(expected[0]).substring(1)} : StringUtils.split(s(expected[0]));
		if (list.size() != expected.length)
			fail(ss("Wrong list length.  expected={0}, actual={1}", expected.length, list.size()));
		for (var i = 0; i < expected.length; i++) {
			var x = list.get(i);
			if (expected[i] instanceof String e) {
				if (ne(r(x), e))
					fail(ss("Element at index {0} did not match.  expected={1}, actual={2}", i, e, r(x)));
			} else if (expected[i] instanceof Predicate e) {
				if (! e.test(x))
					fail(ss("Element at index {0} did pass predicate.  actual={1}", i, r(x)));
			} else {
				if (ne(expected[i], x))
					fail(ss("Element at index {0} did not match.  expected={1}, actual={2}", i, r(expected[i]), r(x)));
			}
		}
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	public static void assertStream(Stream<?> stream, Object...expected) {
		var list = stream.toList();
		if (list.size() != expected.length)
			fail(ss("Wrong list length.  expected={0}, actual={1}", expected.length, list.size()));
		for (var i = 0; i < expected.length; i++)
			if (ne(list.get(i), expected[i]))
				fail(ss("Element at index {0} did not match.  expected={1}, actual={2}", i, expected[i], r(list.get(i))));
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		T exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), ss("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return exception;
	}

	public static <T extends Throwable> T assertThrowable(Class<? extends Throwable> expectedType, String expectedSubstring, T t) {
		var messages = AssertionHelpers.getMessages(t);
		assertTrue(messages.contains(expectedSubstring), ss("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}

	/**
	 * Asserts the entries in a map matches the expected strings after they've been made readable.
	 */
	public static void assertMap(Map<?,?> map, Object...expected) {
		assertList(map.entrySet().stream().map(x -> r(x.getKey()) + "=" + r(x.getValue())).toList(), expected);
	}

	/**
	 * Asserts that a collection is not null or empty.
	 */
	public static void assertNotEmpty(Collection<?> c) {
		assertTrue(c != null && ! c.isEmpty());
	}

	/**
	 * Asserts that a maps is not null or empty.
	 */
	public static void assertNotEmpty(Map<?,?> c) {
		assertTrue(c != null && ! c.isEmpty());
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	public static void assertEmpty(Collection<?> c) {
		assertTrue(c != null && c.isEmpty());
	}

	/**
	 * Asserts that a collection is not null and of the specified size.
	 */
	public static void assertSize(int expected, Collection<?> c) {
		assertEquals(expected, ofNullable(c).map(Collection::size).orElse(-1));
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
	public static void assertBean(Object o, String fields, String value) {
		if (o == null) throw new NullPointerException("Bean was null");
		assertEquals(value, splitNested(fields).stream().map(x -> getReadableEntry(o, x)).collect(joining(",")));
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
	public static void assertMap(Map<?,?> o, String fields, String value) {
		if (o == null) throw new NullPointerException("Map was null");
		assertEquals(value, cdl(fields).stream().map(x -> getReadableEntry(o, x)).collect(joining(",")));
	}

	/**
	 * Asserts that the values of the specified fields match the list of beans after being converted to readable strings.
	 * @param l The list of beans to check.
	 * @param fields A comma-delimited list of bean property names.
	 * @param values The comma-delimited list of values for each bean.
	 */
	@SuppressWarnings("rawtypes")
	public static void assertBeans(Collection l, String fields, Object...values) {
		assertEquals(values.length, l.size(), ()->"Expected "+values.length+" rows but had actual " + l.size());
		var r = 0;
		var f = splitNested(fields);
		for (var o : l) {
			var actual = f.stream().map(x -> getReadableEntry(o, x)).collect(joining(","));
			var r2 = r+1;
			assertEquals(r(values[r]), actual, ()->"Object at row " + r2 + " didn't match.");
			r++;
		}
	}

	public static void assertNotEqualsAny(Object o, Object...values) {
		for (var i = 0; i < values.length; i++) {
			if (eq(o, values[i]))
				fail(ss("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(o)));
		}
	}

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			if (ne(values[0], values[i]))
				fail(ss("Elements at index {0} and {1} did not match.", 0, i));
		}
	}

	private static String getReadableEntry(Object o, String name) {
		var i = name.indexOf("{");
		var pn = i == -1 ? name : name.substring(0, i);
		var spn = i == -1 ? null : splitNestedInner(name);
		var e = getEntry(o, pn);
		if (spn == null) return r(e);
		return spn.stream().map(x -> getReadableEntry(e, x)).collect(joining(","));
	}

	private static Object getEntry(Object o, String name) {
		if (o instanceof List) return List.class.cast(o).get(Integer.parseInt(name));
		if (o.getClass().isArray()) return Array.get(o, Integer.parseInt(name));
		if (o instanceof Map) return Map.class.cast(o).get(name);
		return getBeanProp(o, name);
	}

	/**
	 * Returns the value of the specified field/property on the specified object.
	 * First looks for getter, then looks for field.
	 * Methods and fields can be any visibility.
	 */
	public static Object getBeanProp(Object o, String name) {
		return safe(() -> {
			var f = (Field)null;
			var c = o.getClass();
			var n = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			var m = Arrays.stream(c.getMethods()).filter(x -> isGetter(x, n)).filter(x -> x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o);
			}
			var c2 = c;
			while (f == null && c2 != null) {
				f = Arrays.stream(c2.getDeclaredFields()).filter(x -> x.getName().equals(name)).findFirst().orElse(null);
				c2 = c2.getSuperclass();
			}
			if (f != null) {
				f.setAccessible(true);
				return f.get(o);
			}
			throw runtimeException("No field called {0} found on class {1}", name, c.getName());
		});
	}

	private static boolean isGetter(Method m, String n) {
		var mn = m.getName();
		return ((("get"+n).equals(mn) || ("is"+n).equals(mn)) && m.getParameterCount() == 0);
	}

	/**
	 * Simplified string supplier with message arguments.
	 */
	public static Supplier<String> ss(String pattern, Object...args) {
		return ()->StringUtils.format(pattern, args);
	}

	public static String json(Object o) {
		return Json5.DEFAULT.write(o);
	}
}
