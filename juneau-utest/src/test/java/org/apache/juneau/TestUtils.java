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
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.*;

public class TestUtils extends Utils2 {

	/**
	 * Asserts value when stringified matches the specified pattern.
	 */
	public static Object assertMatches(String pattern, Object value) {
		var m = Utils.getMatchPattern3(pattern).matcher(s(value));
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
		TestUtils.assertContains(json, Json5.DEFAULT.write(value));
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
		assertTrue(c.isInstance(value), "Incorrect type");
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertTypes(Class<?> c, Object...value) {
		for (var i = 0; i < value.length; i++)
			assertTrue(c.isInstance(value[i]), fs("Incorrect type at index [{0}].", i));
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
		assertTrue(a2.contains(expected), fs("String did not contain expected substring.  expected={0}, actual={1}", expected, a2));
	}

	public static void assertContainsAll(String expected, Object actual) {
		var a2 = r(actual);
		for (var e : splita(expected))
			assertTrue(a2.contains(e), fs("String did not contain expected substring.  expected={0}, actual={1}", e, a2));
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
		assertCollection(arrayToList(array), expected);
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	public static void assertList(List<?> list, Object...expected) {
		assertCollection(list, expected);
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	public static void assertSet(Set<?> list, Object...expected) {
		assertCollection(list, expected);
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	@SuppressWarnings("unchecked")
	public static void assertCollection(Collection<?> list, Object...expected) {
		if (expected.length == 1 && expected[0] instanceof String && s(expected[0]).contains(","))
			expected = s(expected[0]).charAt(0) == '>' ? new String[]{s(expected[0]).substring(1)} : splita(s(expected[0]));
		if (list.size() != expected.length)
			fail(fs("Wrong list length.  expected={0}, actual={1}", expected.length, list.size()));
		List<?> list2 = toList(list);
		for (var i = 0; i < expected.length; i++) {
			var x = list2.get(i);
			if (expected[i] instanceof String e) {
				if (ne(r(x), e))
					fail(fs("Element at index {0} did not match.  expected={1}, actual={2}", i, e, r(x)));
			} else if (expected[i] instanceof Predicate e) {
				if (! e.test(x))
					fail(fs("Element at index {0} did pass predicate.  actual={1}", i, r(x)));
			} else {
				if (ne(expected[i], x))
					fail(fs("Element at index {0} did not match.  expected={1}, actual={2}", i, r(expected[i]), r(x)));
			}
		}
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made readable.
	 */
	public static void assertStream(Stream<?> stream, Object...expected) {
		var list = stream.toList();
		if (list.size() != expected.length)
			fail(fs("Wrong list length.  expected={0}, actual={1}", expected.length, list.size()));
		for (var i = 0; i < expected.length; i++)
			if (ne(list.get(i), expected[i]))
				fail(fs("Element at index {0} did not match.  expected={1}, actual={2}", i, expected[i], r(list.get(i))));
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = TestUtils.getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return exception;
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, List<String> expectedSubstrings, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = TestUtils.getMessages(exception);
		expectedSubstrings.stream().forEach(x -> assertTrue(messages.contains(x), fs("Expected message to contain: {0}.\nActual:\n{1}", x, messages)));
		return exception;
	}

	public static <T extends Throwable> T assertThrowable(Class<? extends Throwable> expectedType, String expectedSubstring, T t) {
		var messages = TestUtils.getMessages(t);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
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
		assertEquals(value, Utils.splitNested(fields).stream().map(x -> TestUtils.getReadableEntry(o, x)).collect(joining(",")));
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
		assertEquals(value, Utils.splitNested(fields).stream().map(x -> TestUtils.getReadableEntry(o, x)).collect(joining(",")));
	}

	/**
	 * Asserts that the values of the specified fields match the list of beans after being converted to readable strings.
	 * @param l The list of beans to check.
	 * @param fields A comma-delimited list of bean property names.
	 * @param values The comma-delimited list of values for each bean.
	 */
	@SuppressWarnings("rawtypes")
	public static void assertBeans(Collection l, String fields, String...values) {
		assertEquals(values.length, l.size(), ()->"Expected "+values.length+" rows but had actual " + l.size());
		var r = 0;
		var f = Utils.splitNested(fields);
		for (var o : l) {
			var actual = f.stream().map(x -> TestUtils.getReadableEntry(o, x)).collect(joining(","));
			var r2 = r+1;
			assertEquals(r(values[r]), actual, ()->"Object at row " + r2 + " didn't match.");
			r++;
		}
	}

	public static void assertNotEqualsAny(Object o, Object...values) {
		for (var i = 0; i < values.length; i++) {
			if (eq(o, values[i]))
				fail(fs("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(o)));
		}
	}

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			if (ne(values[0], values[i]))
				fail(fs("Elements at index {0} and {1} did not match.", 0, i));
		}
	}

	private static String getReadableEntry(Object o, String name) {
		var i = name.indexOf("{");
		var pn = i == -1 ? name : name.substring(0, i);
		var spn = i == -1 ? null : Utils.splitNestedInner(name);
		var e = TestUtils.getEntry(o, pn);
		if (spn == null || e == null) return r(e);
		return spn.stream().map(x -> getReadableEntry(e, x)).collect(joining(",","{","}"));
	}

	@SuppressWarnings("unchecked")
	private static Object getEntry(Object o, String name) {
		if (o instanceof List o2) return isNumeric(name) ? o2.get(Integer.parseInt(name)) : getBeanProp(o, name);
		if (o.getClass().isArray()) return isNumeric(name) ? Array.get(o, Integer.parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Map o2) return opt(o2.get(name)).orElse(name.equals("class") ? o.getClass() : null);
		if (o instanceof Iterable o2) return isNumeric(name) ? toList(o2).get(Integer.parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Iterator o2) return isNumeric(name) ? toList(o2).get(Integer.parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Enumeration o2) return isNumeric(name) ? toList(o2).get(Integer.parseInt(name)) : getBeanProp(o, name);
		return getBeanProp(o, name);
	}

	private static boolean isNumeric(String name) {
		return StringUtils.isNumeric(name);
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
			var m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("is"+n) && x.getParameterCount() == 0 && x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o);
			}
			m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get"+n) && x.getParameterCount() == 0 && x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
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
			m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get") && x.getParameterCount() == 1 && x.getParameterTypes()[0] == String.class && x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o, name);
			}
			if (c.isArray()) {
				switch (name) {
					case "length": return Array.getLength(o);
					default: // Fall through.
				}
			}
			throw runtimeException("No field called {0} found on class {1}", name, c.getName());
		});
	}

	public static String json(Object o) {
		return Json5.DEFAULT.write(o);
	}

	public static void assertLines(String expected, Object value) {
		assertEquals(expected, r(value).replaceAll("\\r?\\n", "|"));
	}

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();
	public static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 * Use {@link #unsetTimeZone()} to unset it.
	 *
	 * @param name
	 */
	public static final synchronized void setTimeZone(String v) {
		SYSTEM_TIME_ZONE.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(v));
	}

	public static final synchronized void unsetTimeZone() {
		TimeZone.setDefault(SYSTEM_TIME_ZONE.get());
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static final void setLocale(Locale v) {
		SYSTEM_LOCALE.set(Locale.getDefault());
		Locale.setDefault(v);
	}

	public static final void unsetLocale() {
		Locale.setDefault(SYSTEM_LOCALE.get());
	}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 */
	public static final void checkXmlWhitespace(String out) throws SerializeException {
		if (out.indexOf('\u0000') != -1) {
			for (var s : out.split("\u0000"))
				checkXmlWhitespace(s);
			return;
		}

		var indent = -1;
		var startTag = Pattern.compile("^(\\s*)<[^/>]+(\\s+\\S+=['\"]\\S*['\"])*\\s*>$");  // NOSONAR
		var endTag = Pattern.compile("^(\\s*)</[^>]+>$");
		var combinedTag = Pattern.compile("^(\\s*)<[^>/]+(\\s+\\S+=['\"]\\S*['\"])*\\s*/>$");  // NOSONAR
		var contentOnly = Pattern.compile("^(\\s*)[^\\s\\<]+$");
		var tagWithContent = Pattern.compile("^(\\s*)<[^>]+>.*</[^>]+>$");
		var lines = out.split("\n");
		try {
			for (var i = 0; i < lines.length; i++) {
				var line = lines[i];
				var m = startTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on start tag line ''{0}''", i+1);
					continue;
				}
				m = endTag.matcher(line);
				if (m.matches()) {
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on end tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = combinedTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on combined tag line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = contentOnly.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on content-only line ''{0}''", i+1);
					indent--;
					continue;
				}
				m = tagWithContent.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new SerializeException("Wrong indentation detected on tag-with-content line ''{0}''", i+1);
					indent--;
					continue;
				}
				throw new SerializeException("Unmatched whitespace line at line number ''{0}''", i+1);
			}
			if (indent != -1)
				throw new SerializeException("Possible unmatched tag.  indent=''{0}''", indent);
		} catch (SerializeException e) {
			printLines(lines);
			throw e;
		}
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		checkXmlWhitespace(xml);
	}

	/**
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	/**
	 * Creates a reader from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new reader.
	 */
	public static final StringReader reader(String in) {
		return new StringReader(in);
	}

	/**
	 * Constructs a {@link URL} object from a string.
	 */
	public static URL url(String value) {
		return safe(()->new URI(value).toURL());
	}

	/**
	 * Asserts an exception is not thrown
	 * Example:  assertThrown(()->doSomething());
	 */
	public static void assertNotThrown(Snippet snippet) {
		try {
			snippet.run();
		} catch (Throwable e) {
			fail("Exception thrown.", e);
		}
	}

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
	 * @param c
	 * @return
	 */
	public static Swagger getSwagger(Class<?> c) {
		try {
			var r = c.getDeclaredConstructor().newInstance();
			var rc = RestContext.create(r.getClass(),null,null).init(()->r).build();
			var ctx = RestOpContext.create(TestUtils.class.getMethod("getSwagger", Class.class), rc).build();
			var session = RestSession.create(rc).resource(r).req(new MockServletRequest()).res(new MockServletResponse()).build();
			var req = ctx.createRequest(session);
			var ip = rc.getSwaggerProvider();
			return ip.getSwagger(rc, req.getLocale());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	public static void assertStringEmpty(Object s) {
		assertNotNull(s);
		assertTrue(r(s).isEmpty());
	}

	public static void assertJsonMatches(Object o, String pattern) throws AssertionError {
		var json = json(o);
		assertTrue(Utils.getMatchPattern3(pattern).matcher(json).matches(), fs("JSON did not match pattern.\njson={0}", json));
	}

	public static void assertSameObject(Object o1, Object o2) {
		assertSame(o1, o2);
	}

	public static Arguments args(Object...args) {
		return Arguments.of(args);
	}

	public static <T> void assertMapped(T o, BiFunction<T,String,Object> f, String properties, String expected) {
		var m = new LinkedHashMap<String,Object>();
		for (var p : split(properties)) {
			try {
				m.put(p, f.apply(o, p));
			} catch (Exception e) {
				m.put(p, e.getClass().getSimpleName());
			}
		}
		assertMap(m, properties, expected);
	}

	/**
	 * Converts any array (including primitive arrays) to a List.
	 *
	 * @param array The array to convert. Can be any array type including primitives.
	 * @return A List containing the array elements. Primitive values are auto-boxed.
	 *         Returns null if the input is null.
	 * @throws IllegalArgumentException if the input is not an array.
	 */
	public static List<Object> arrayToList(Object array) {
		if (array == null) {
			return null;
		}

		if (!array.getClass().isArray()) {
			throw new IllegalArgumentException("Input must be an array, but was: " + array.getClass().getName());
		}

		var componentType = array.getClass().getComponentType();
		var length = Array.getLength(array);
		var result = new ArrayList<Object>(length);

		// Handle primitive arrays specifically for better performance
		if (componentType.isPrimitive()) {
			if (componentType == int.class) {
				var arr = (int[]) array;
				for (int value : arr) {
					result.add(value);
				}
			} else if (componentType == long.class) {
				var arr = (long[]) array;
				for (long value : arr) {
					result.add(value);
				}
			} else if (componentType == double.class) {
				var arr = (double[]) array;
				for (double value : arr) {
					result.add(value);
				}
			} else if (componentType == float.class) {
				var arr = (float[]) array;
				for (float value : arr) {
					result.add(value);
				}
			} else if (componentType == boolean.class) {
				var arr = (boolean[]) array;
				for (boolean value : arr) {
					result.add(value);
				}
			} else if (componentType == byte.class) {
				var arr = (byte[]) array;
				for (byte value : arr) {
					result.add(value);
				}
			} else if (componentType == char.class) {
				var arr = (char[]) array;
				for (char value : arr) {
					result.add(value);
				}
			} else if (componentType == short.class) {
				var arr = (short[]) array;
				for (short value : arr) {
					result.add(value);
				}
			}
		} else {
			// Handle Object arrays
			for (var i = 0; i < length; i++) {
				result.add(Array.get(array, i));
			}
		}

		return result;
	}
}
