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
package org.apache.juneau.commons;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * Standalone test assertion helpers for juneau-commons tests.
 *
 * <p>Provides BCT-compatible assertList/assertMap/assertBean/assertSize/assertEmpty methods using
 * only JUnit5 + standard Java, with no dependency on juneau-bct. This breaks the compile-cycle
 * that would arise if juneau-commons test code depended on juneau-bct (which itself depends on
 * juneau-commons).</p>
 *
 * <p>Usage: {@code import static org.apache.juneau.commons.TestAssertions.*;}</p>
 *
 * <p>Each test class also needs {@code import static org.junit.jupiter.api.Assertions.*;} for
 * the standard JUnit assertion methods (assertEquals, assertTrue, etc.).</p>
 */
@SuppressWarnings({
	"java:S3011",  // Reflective access needed for assertBean/assertEmpty
	"java:S1172",  // Consistent method signatures are intentional
	"unused"       // Unused parameters/variables kept for consistent method signatures across test utilities.
})
public class TestAssertions {

	/**
	 * Asserts that a list/collection/array/stream contains exactly the given elements in order.
	 *
	 * <p>If an expected value is a String, the actual element is converted via {@link #stringify(Object)}.
	 * Otherwise elements are compared with {@link Objects#equals(Object, Object)}.
	 */
	public static void assertList(Object actual, Object... expected) {
		List<?> list = toList(actual);
		assertEquals(expected.length, list.size(),
			() -> "List size mismatch. expected=" + expected.length + " actual=" + list.size() + "\n  list=" + list);
		for (int i = 0; i < expected.length; i++) {
			Object e = expected[i];
			Object a = list.get(i);
			final int idx = i;
			if (e instanceof String) {
				assertEquals((String) e, stringify(a),
					() -> "List element at index " + idx + " mismatch");
			} else {
				assertEquals(e, a,
					() -> "List element at index " + idx + " mismatch");
			}
		}
	}

	/**
	 * Asserts that a map contains exactly the given entries, expressed as {@code "key=value"} strings.
	 *
	 * <p>Null keys render as {@code "<null>"}; null values render as {@code "<null>"}. Maps that are
	 * neither {@link SortedMap} nor {@link LinkedHashMap} are sorted by key string for determinism.
	 */
	public static void assertMap(Map<?, ?> actual, Object... expected) {
		Map<?, ?> ordered = toOrderedMap(actual);
		List<String> entries = ordered.entrySet().stream()
			.map(e -> stringifyKey(e.getKey()) + "=" + stringify(e.getValue()))
			.collect(Collectors.toList());
		assertList(entries, expected);
	}

	/**
	 * Asserts that the given fields/properties on an object equal the expected values.
	 *
	 * <p>Fields is a comma-separated list of property names (flat names; no nesting syntax).
	 * Each property is accessed via: exact method name, getXxx(), isXxx(), then Map key.
	 * Expected is a comma-separated list of string values where lists render as {@code [a,b]}.</p>
	 */
	public static void assertBean(Object actual, String fields, String expected) {
		String[] fieldArr = splitTopLevel(fields);
		String[] expectedArr = splitTopLevel(expected);
		assertEquals(fieldArr.length, expectedArr.length,
			"assertBean: field-count=" + fieldArr.length + " expected-count=" + expectedArr.length);
		for (int i = 0; i < fieldArr.length; i++) {
			String f = fieldArr[i].trim();
			Object value = getProperty(actual, f);
			String actualStr = stringify(value);
			String expectedStr = expectedArr[i].trim();
			assertEquals(expectedStr, actualStr, () -> "assertBean field '" + f + "' mismatch");
		}
	}

	/**
	 * Asserts that a collection, array, map, or string has the expected size.
	 * Objects with a {@code size()} or {@code length()} method are also supported.
	 */
	public static void assertSize(int expected, Object actual) {
		assertNotNull(actual, "assertSize: value was null");
		int size = sizeOf(actual);
		assertEquals(expected, size, "assertSize: expected size " + expected + " but got " + size);
	}

	/**
	 * Asserts that a collection, array, map, or string is empty.
	 * Objects with an {@code isEmpty()} method are also supported.
	 */
	public static void assertEmpty(Object value) {
		assertNotNull(value, "assertEmpty: value was null");
		assertTrue(isEmptyLike(value), () -> "assertEmpty: value was not empty: " + value);
	}

	/**
	 * Asserts that a collection, array, map, or string is not empty (size {@literal >} 0).
	 * Objects with an {@code isEmpty()} method are also supported.
	 */
	public static void assertNotEmpty(Object value) {
		assertNotNull(value, "assertNotEmpty: value was null");
		assertFalse(isEmptyLike(value), () -> "assertNotEmpty: value was empty: " + value);
	}

	/**
	 * Asserts that the string representation of {@code actual} contains all of the expected substrings.
	 */
	public static void assertContainsAll(Object actual, String... expected) {
		assertNotNull(actual, "assertContainsAll: value was null");
		String s = String.valueOf(actual);
		for (String e : expected)
			assertTrue(s.contains(e), () -> "assertContainsAll: string did not contain '" + e + "'.\n  actual: " + s);
	}

	/**
	 * Asserts that the string representation of {@code actual} contains the {@code expected} substring.
	 * Note: parameter order is (expected, actual) matching BCT convention.
	 */
	public static void assertContains(String expected, Object actual) {
		assertNotNull(actual, "assertContains: value was null");
		String s = String.valueOf(actual);
		assertTrue(s.contains(expected),
			() -> "assertContains: string did not contain '" + expected + "'.\n  actual: " + s);
	}

	/**
	 * Asserts that the string representation of {@code actual} equals {@code expected}.
	 * Uses {@link #stringify(Object)} for conversion.
	 */
	public static void assertString(String expected, Object actual) {
		assertEquals(expected, stringify(actual), "assertString mismatch");
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------------

	private static List<?> toList(Object actual) {
		if (actual instanceof List<?> l) return l;
		if (actual instanceof Collection<?> c) return new ArrayList<>(c);
		if (actual instanceof Stream<?> s) return s.collect(Collectors.toList());
		if (actual instanceof Enumeration<?> e) {
			List<Object> result = new ArrayList<>();
			while (e.hasMoreElements()) result.add(e.nextElement());
			return result;
		}
		if (actual instanceof Iterable<?> it) {
			List<Object> result = new ArrayList<>();
			for (Object o : it) result.add(o);
			return result;
		}
		if (actual != null && actual.getClass().isArray()) {
			int len = Array.getLength(actual);
			List<Object> result = new ArrayList<>(len);
			for (int i = 0; i < len; i++) result.add(Array.get(actual, i));
			return result;
		}
		throw new AssertionError("assertList: not list-like: " + actual);
	}

	/**
	 * Stringify a value using BCT-compatible format.
	 *
	 * <ul>
	 *   <li>null → {@code "<null>"}
	 *   <li>char[] → String via {@code new String(charArray)}
	 *   <li>Class → {@code getSimpleName()}
	 *   <li>Optional → unwrapped value or {@code "<null>"}
	 *   <li>Collection/array → {@code [a,b,c]}
	 *   <li>Map → {@code {k=v,...}}
	 *   <li>other → {@code String.valueOf(o)}
	 * </ul>
	 */
	static String stringify(Object o) {
		if (o == null) return "<null>";
		if (o instanceof char[] chars) return new String(chars);
		if (o instanceof Class<?> cls) return cls.getSimpleName();
		if (o instanceof Optional<?> opt) return opt.isPresent() ? stringify(opt.get()) : "<null>";
		if (o instanceof List<?> l)
			return "[" + l.stream().map(TestAssertions::stringify).collect(Collectors.joining(",")) + "]";
		if (o instanceof Collection<?> c)
			return "[" + c.stream().map(TestAssertions::stringify).collect(Collectors.joining(",")) + "]";
		if (o instanceof Map<?, ?> m) {
			return "{" + m.entrySet().stream()
				.map(e -> stringifyKey(e.getKey()) + "=" + stringify(e.getValue()))
				.collect(Collectors.joining(",")) + "}";
		}
		if (o.getClass().isArray()) {
			int len = Array.getLength(o);
			List<String> parts = new ArrayList<>(len);
			for (int i = 0; i < len; i++)
				parts.add(stringify(Array.get(o, i)));
			return "[" + String.join(",", parts) + "]";
		}
		return String.valueOf(o);
	}

	private static String stringifyKey(Object k) {
		if (k == null) return "<null>";
		return String.valueOf(k);
	}

	private static int sizeOf(Object o) {
		if (o instanceof Collection<?> c) return c.size();
		if (o instanceof Map<?, ?> m) return m.size();
		if (o instanceof String s) return s.length();
		if (o.getClass().isArray()) return Array.getLength(o);
		// Try size() method
		try {
			Method m = o.getClass().getMethod("size");
			if (m.getReturnType() == int.class || m.getReturnType() == Integer.class)
				return (int) m.invoke(o);
		} catch (ReflectiveOperationException ignored) { /* try next */ }
		// Try length() method
		try {
			Method m = o.getClass().getMethod("length");
			if (m.getReturnType() == int.class || m.getReturnType() == Integer.class)
				return (int) m.invoke(o);
		} catch (ReflectiveOperationException ignored) { /* try next */ }
		throw new AssertionError("assertSize: cannot determine size of " + o.getClass().getName());
	}

	private static boolean isEmptyLike(Object o) {
		if (o instanceof Collection<?> c) return c.isEmpty();
		if (o instanceof Map<?, ?> m) return m.isEmpty();
		if (o instanceof String s) return s.isEmpty();
		if (o instanceof Optional<?> opt) return opt.isEmpty();
		if (o instanceof Enumeration<?> e) return !e.hasMoreElements();
		if (o.getClass().isArray()) return Array.getLength(o) == 0;
		// Try isEmpty() method
		try {
			Method m = o.getClass().getMethod("isEmpty");
			if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
				return (boolean) m.invoke(o);
		} catch (ReflectiveOperationException ignored) { /* try next */ }
		// Try isPresent() method (return !isPresent() as isEmpty)
		try {
			Method m = o.getClass().getMethod("isPresent");
			if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
				return !(boolean) m.invoke(o);
		} catch (ReflectiveOperationException ignored) { /* fall through */ }
		throw new AssertionError("assertEmpty: cannot determine if " + o.getClass().getName() + " is empty");
	}

	@SuppressWarnings({
		"java:S1452"  // Wildcard type parameter required for test utility generics.
	})
	private static Map<?, ?> toOrderedMap(Map<?, ?> actual) {
		if (actual instanceof SortedMap || actual instanceof LinkedHashMap) return actual;
		List<Map.Entry<?, ?>> entries = new ArrayList<>(actual.entrySet());
		entries.sort(Comparator.comparing(e -> stringifyKey(e.getKey())));
		LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : entries)
			result.put(e.getKey(), e.getValue());
		return result;
	}

	private static Object getProperty(Object obj, String name) {
		if (obj instanceof Map<?, ?> m) return m.get(name);
		// Try exact method name (e.g. on(), value(), number())
		try {
			Method m = obj.getClass().getMethod(name);
			return m.invoke(obj);
		} catch (NoSuchMethodException ignored) {
			// try next
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("getProperty('" + name + "'): invoke failed", e);
		}
		// Try getter: getXxx()
		String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		try {
			Method m = obj.getClass().getMethod(getter);
			return m.invoke(obj);
		} catch (NoSuchMethodException ignored) {
			// try next
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("getProperty('" + name + "'): getter invoke failed", e);
		}
		// Try boolean getter: isXxx()
		String isGetter = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
		try {
			Method m = obj.getClass().getMethod(isGetter);
			return m.invoke(obj);
		} catch (NoSuchMethodException ignored) {
			// try next
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("getProperty('" + name + "'): isGetter invoke failed", e);
		}
		// Try public field
		try {
			Field f = obj.getClass().getField(name);
			return f.get(obj);
		} catch (NoSuchFieldException ignored) {
			// fall through
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("getProperty('" + name + "'): field access failed", e);
		}
		throw new AssertionError("getProperty: no property '" + name + "' on " + obj.getClass().getName());
	}

	/** Split a string by top-level commas, respecting nesting inside [] and {}. */
	static String[] splitTopLevel(String s) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '[' || c == '{') depth++;
			else if (c == ']' || c == '}') depth--;
			else if (c == ',' && depth == 0) {
				parts.add(s.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(s.substring(start));
		return parts.toArray(new String[0]);
	}
}
