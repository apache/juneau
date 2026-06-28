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
package org.apache.juneau;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.junit.jupiter.api.*;

/**
 * Test utilities for the juneau-marshall module.
 *
 * <p>Contains a marshall-compatible subset of the methods available in the full {@code TestUtils}
 * in {@code juneau-integration-tests}.  Methods that depend on {@code juneau-rest-*} modules are excluded.</p>
 */
@SuppressWarnings({
	"unchecked",      // BeanTester cast and generic type handling in test utilities
	"java:S1172"      // Parameters kept for consistent method signatures across test utilities
})
public class TestUtils extends Utils {

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();

	public static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			assertEquals(values[0], values[i], fs("Elements at index {0} and {1} did not match. {0}={2}, {1}={3}", 0, i, r(values[0]), r(values[i])));
		}
	}

	public static void assertNotEqualsAny(Object actual, Object...values) {
		assertNotNull(actual, "Value was null.");
		for (var i = 0; i < values.length; i++) {
			assertNotEquals(values[i], actual, fs("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(actual)));
		}
	}

	public static void assertSerialized(Object actual, WriterSerializer s, String expected) {
		assertEquals(expected, s.toString(actual));
	}

	@SuppressWarnings({
		"unused"  // Unused parameters/variables kept for consistent method signatures across test utilities.
	})
	public static <T extends Throwable> T assertThrowable(Class<? extends Throwable> expectedType, String expectedSubstring, T t) {
		assertTrue(expectedType.isInstance(t), fs("Expected throwable of type: {0}.\nActual: {1}", expectedType.getName(), t == null ? "null" : t.getClass().getName()));
		var messages = getMessages(t);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, List<String> expectedSubstrings, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		expectedSubstrings.forEach(x -> assertTrue(messages.contains(x), fs("Expected message to contain: {0}.\nActual:\n{1}", x, messages)));
		return exception;
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return exception;
	}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void checkXmlWhitespace(String out) throws Exception {
		if (out.indexOf('\u0000') != -1) {
			for (var s : out.split("\u0000"))
				checkXmlWhitespace(s);
			return;
		}

		var indent = -1;
		var startTag = Pattern.compile("^(\\s*)<[^\\s/>]++(\\s++\\S++=['\"]\\S*+['\"])*+\\s*+>$");
		var endTag = Pattern.compile("^(\\s*)</[^>]+>$");
		var combinedTag = Pattern.compile("^(\\s*)<[^\\s>/]++(\\s++\\S++=['\"]\\S*+['\"])*+\\s*+/>$");
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
						throw new Exception("Wrong indentation detected on start tag line ''" + (i+1) + "''");
					continue;
				}
				m = endTag.matcher(line);
				if (m.matches()) {
					if (m.group(1).length() != indent)
						throw new Exception("Wrong indentation detected on end tag line ''" + (i+1) + "''");
					indent--;
					continue;
				}
				m = combinedTag.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new Exception("Wrong indentation detected on combined tag line ''" + (i+1) + "''");
					indent--;
					continue;
				}
				m = contentOnly.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new Exception("Wrong indentation detected on content-only line ''" + (i+1) + "''");
					indent--;
					continue;
				}
				m = tagWithContent.matcher(line);
				if (m.matches()) {
					indent++;
					if (m.group(1).length() != indent)
						throw new Exception("Wrong indentation detected on tag-with-content line ''" + (i+1) + "''");
					indent--;
					continue;
				}
				throw new Exception("Unmatched whitespace line at line number ''" + (i+1) + "''");
			}
			if (indent != -1)
				throw new Exception("Possible unmatched tag.  indent=''" + indent + "''");
		} catch (Exception e) {
			printLines(lines);
			throw e;
		}
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
			m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals("get") && x.getParameterCount() == 1 && x.getParameterTypes()[0] == String.class && x.getAnnotation(BeanIgnore.class) == null).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o, name);
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
			m = Arrays.stream(c.getMethods()).filter(x -> x.getName().equals(name) && x.getParameterCount() == 0).findFirst().orElse(null);
			if (m != null) {
				m.setAccessible(true);
				return m.invoke(o);
			}
			throw rex("Property {0} not found on object of type {1}", name, cn(o));
		});
	}

	private static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}

	/**
	 * Creates an input stream from the specified string.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	public static String json(Object o) {
		return json5(o);
	}

	public static <T> T json(String o, Class<T> c) {
		return safe(()->json5(o, c));
	}

	public static <T> T jsonRoundTrip(T o, Class<T> c) {
		return json(json(o), c);
	}

	/**
	 * Creates a reader from the specified string.
	 */
	public static final StringReader reader(String in) {
		return new StringReader(in);
	}

	/**
	 * Temporarily sets the default system locale to the specified locale.
	 */
	public static final void setLocale(Locale v) {
		SYSTEM_LOCALE.set(Locale.getDefault());
		Locale.setDefault(v);
	}

	/**
	 * Temporarily sets the default system timezone to the specified timezone ID.
	 */
	public static final synchronized void setTimeZone(String v) {
		SYSTEM_TIME_ZONE.set(TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone(v));
	}

	public static final void unsetLocale() {
		Locale.setDefault(SYSTEM_LOCALE.get());
	}

	public static final synchronized void unsetTimeZone() {
		TimeZone.setDefault(SYSTEM_TIME_ZONE.get());
	}

	/**
	 * Constructs a {@link URL} object from a string.
	 */
	public static URL url(String value) {
		return safe(()->new URI(value).toURL());
	}

	/**
	 * Validates XML whitespace and namespace formatting on a serialized object.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void validateXml(Object o) throws Exception {
		validateXml(o, XmlSerializer.DEFAULT_NS_SQ);
	}

	/**
	 * Validates XML whitespace and namespace formatting on a serialized object.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		checkXmlWhitespace(xml);
	}

	public static final <T> BeanTester<T> testBean(T bean) {
		return (BeanTester<T>) new BeanTester<>().bean(bean);
	}

	/**
	 * Extracts HTML/XML elements from a string based on element name and attributes.
	 */
	public static List<String> extractXml(String html, String elementName, Map<String,String> withAttributes) {
		List<String> results = list();
		if (html == null || elementName == null)
			return results;
		var pattern = Pattern.compile("<" + Pattern.quote(elementName) + "(\\s[^>]*+)?>", Pattern.CASE_INSENSITIVE);
		@SuppressWarnings({
			"unused"  // Unused parameters/variables kept for consistent method signatures across test utilities.
		})
		var matcher = pattern.matcher(html);
		var depth = 0;
		var startPos = -1;
		for (var i = 0; i < html.length(); i++) {
			var remaining = html.substring(i);
			if (remaining.startsWith("<" + elementName) && (remaining.length() == elementName.length() + 1 || !Character.isLetterOrDigit(remaining.charAt(elementName.length() + 1)))) {
				if (depth == 0) {
					var tagEnd = html.indexOf('>', i);
					if (tagEnd == -1) break;
					var tag = html.substring(i, tagEnd + 1);
					var matches = withAttributes == null || withAttributes.isEmpty();
					if (!matches && withAttributes != null) {
						matches = true;
						for (var entry : withAttributes.entrySet()) {
							if (!tag.contains(entry.getKey() + "=\"" + entry.getValue() + "\"") && !tag.contains(entry.getKey() + "='" + entry.getValue() + "'")) {
								matches = false;
								break;
							}
						}
					}
					if (matches) {
						startPos = i;
						depth = 1;
						i = tagEnd;
					}
				} else {
					depth++;
				}
			} else if (remaining.startsWith("</" + elementName + ">")) {
				if (depth == 1 && startPos >= 0) {
					results.add(html.substring(startPos, i + elementName.length() + 3));
					depth = 0;
					startPos = -1;
				} else if (depth > 1) {
					depth--;
				}
			}
		}
		return results;
	}

	public static String assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
		return expected;
	}

	public static String pipedLines(Object value) {
		return r(value).replaceAll("\\r?\\n", "|");
	}
}
