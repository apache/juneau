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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.junit.bct.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

/**
 * Comprehensive utility class for Bean-Centric Tests (BCT) and general testing operations.
 *
 * <p>This class provides the core testing infrastructure for Apache Juneau, with particular emphasis
 * on the Bean-Centric Testing (BCT) framework. BCT enables sophisticated assertion patterns for
 * testing object properties, collections, maps, and complex nested structures with minimal code.</p>
 *
 * <h5 class='section'>Bean-Centric Testing (BCT) Framework:</h5>
 * <p>The BCT framework consists of several key components:</p>
 * <ul>
 * 	<li><b>{@link BeanConverter}:</b> Core interface for object conversion and property access</li>
 * 	<li><b>{@link BasicBeanConverter}:</b> Default implementation with extensible type handlers</li>
 * 	<li><b>Assertion Methods:</b> High-level testing methods that leverage the converter framework</li>
 * </ul>
 *
 * <h5 class='section'>Primary BCT Assertion Methods:</h5>
 * <dl>
 * 	<dt><b>{@link #assertBean(Object, String, String)}</b></dt>
 * 	<dd>Tests object properties with nested syntax support and collection iteration</dd>
 *
 * 	<dt><b>{@link #assertMap(Map, String, String)}</b></dt>
 * 	<dd>Tests map entries with the same nested property syntax as assertBean</dd>
 *
 * 	<dt><b>{@link #assertMapped(Object, java.util.function.BiFunction, String, String)}</b></dt>
 * 	<dd>Tests custom property access using BiFunction for non-standard objects</dd>
 *
 * 	<dt><b>{@link #assertList(List, Object...)}</b></dt>
 * 	<dd>Tests list/collection elements with varargs for expected values</dd>
 *
 * 	<dt><b>{@link #assertBeans(Collection, String, String...)}</b></dt>
 * 	<dd>Tests collections of objects by extracting and comparing specific fields</dd>
 * </dl>
 *
 * <h5 class='section'>BCT Advanced Features:</h5>
 * <ul>
 * 	<li><b>Nested Property Syntax:</b> "address{street,city}" for testing nested objects</li>
 * 	<li><b>Collection Iteration:</b> "#{property}" syntax for testing all elements</li>
 * 	<li><b>Universal Size Properties:</b> "length" and "size" work on all collection types</li>
 * 	<li><b>Array/List Access:</b> Numeric indices for element-specific testing</li>
 * 	<li><b>Method Chaining:</b> Fluent setters can be tested directly</li>
 * 	<li><b>Direct Field Access:</b> Public fields accessed without getters</li>
 * 	<li><b>Map Key Access:</b> Including special "&lt;NULL&gt;" syntax for null keys</li>
 * </ul>
 *
 * <h5 class='section'>Converter Extensibility:</h5>
 * <p>The BCT framework is built on the extensible {@link BasicBeanConverter} which allows:</p>
 * <ul>
 * 	<li><b>Custom Stringifiers:</b> Type-specific string conversion logic</li>
 * 	<li><b>Custom Listifiers:</b> Collection-type conversion for iteration</li>
 * 	<li><b>Custom Swapifiers:</b> Object transformation before conversion</li>
 * 	<li><b>Configurable Settings:</b> Formatting, delimiters, and display options</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 *
 * <p><b>Basic Property Testing:</b></p>
 * <p class='bjava'>
 * 	<jc>// Test multiple properties</jc>
 * 	assertBean(user, <js>"name,age,active"</js>, <js>"John,30,true"</js>);
 *
 * 	<jc>// Test nested properties</jc>
 * 	assertBean(user, <js>"address{street,city}"</js>, <js>"{123 Main St,Springfield}"</js>);
 * </p>
 *
 * <p><b>Collection and Array Testing:</b></p>
 * <p class='bjava'>
 * 	<jc>// Test collection size and iterate over all elements</jc>
 * 	assertBean(order, <js>"items{length,#{name}}"</js>, <js>"{3,[{Laptop},{Phone},{Tablet}]}"</js>);
 *
 * 	<jc>// Test specific array elements</jc>
 * 	assertBean(data, <js>"values{0,1,2}"</js>, <js>"{100,200,300}"</js>);
 * </p>
 *
 * <p><b>Map and Collection Testing:</b></p>
 * <p class='bjava'>
 * 	<jc>// Test map entries</jc>
 * 	assertMap(config, <js>"timeout,retries"</js>, <js>"30000,3"</js>);
 *
 * 	<jc>// Test list elements</jc>
 * 	assertList(tags, <js>"red"</js>, <js>"green"</js>, <js>"blue"</js>);
 * </p>
 *
 * <p><b>Custom Property Access:</b></p>
 * <p class='bjava'>
 * 	<jc>// Test with custom accessor function</jc>
 * 	assertMapped(myObject, (obj, prop) -> obj.getProperty(prop),
 * 		<js>"prop1,prop2"</js>, <js>"value1,value2"</js>);
 * </p>
 *
 * <h5 class='section'>Performance and Thread Safety:</h5>
 * <p>The BCT framework is designed for high performance with:</p>
 * <ul>
 * 	<li><b>Caching:</b> Type-to-handler mappings cached for fast lookup</li>
 * 	<li><b>Thread Safety:</b> All operations are thread-safe for concurrent testing</li>
 * 	<li><b>Minimal Allocation:</b> Efficient object reuse and minimal temporary objects</li>
 * </ul>
 *
 * @see BeanConverter
 * @see BasicBeanConverter
 */
public class TestUtils extends Utils2 {

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();

	public static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();


	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			assertEquals(values[0], values[i], fs("Elements at index {0} and {1} did not match. {0}={2}, {1}={3}", 0, i, r(values[0]), r(values[i])));
		}
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJson(String expected, Object value) {
		assertEquals(expected, Json5.DEFAULT_SORTED.write(value));
	}

	/**
	 * Converts the specified object to a string and then replaces any newlines with pipes for easy comparison during testing.
	 * @param value
	 */
	public static String pipedLines(Object value) {
		return r(value).replaceAll("\\r?\\n", "|");
	}


	public static void assertNotEqualsAny(Object actual, Object...values) {
		assertNotNull(actual, "Value was null.");
		for (var i = 0; i < values.length; i++) {
			assertNotEquals(values[i], actual, fs("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(actual)));
		}
	}

	/**
	 * Asserts the serialized representation of the specified object.
	 */
	public static void assertSerialized(Object actual, WriterSerializer s, String expected) {
		assertEquals(expected, s.toString(actual));
	}

	public static <T extends Throwable> T assertThrowable(Class<? extends Throwable> expectedType, String expectedSubstring, T t) {
		var messages = getMessages(t);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, List<String> expectedSubstrings, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		expectedSubstrings.stream().forEach(x -> assertTrue(messages.contains(x), fs("Expected message to contain: {0}.\nActual:\n{1}", x, messages)));
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
			throw runtimeException("Property {0} not found on object of type {1}", name, cn(o));
		});
	}

	private static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
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
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	public static String json(Object o) {
		return Json5.DEFAULT_SORTED.write(o);
	}

	public static <T> T json(String o, Class<T> c) {
		return safe(()->Json5.DEFAULT_SORTED.read(o, c));
	}

	public static <T> T jsonRoundTrip(T o, Class<T> c) {
		return json(json(o), c);
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
	 * Temporarily sets the default system locale to the specified locale.
	 * Use {@link #unsetLocale()} to unset it.
	 *
	 * @param name
	 */
	public static final void setLocale(Locale v) {
		SYSTEM_LOCALE.set(Locale.getDefault());
		Locale.setDefault(v);
	}

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

	public static final <T> BeanTester<T> testBean(T bean) {
		return (BeanTester<T>) new BeanTester<>().bean(bean);
	}

	/**
	 * Extracts HTML/XML elements from a string based on element name and attributes.
	 *
	 * <p>Uses a depth-tracking parser to handle nested elements correctly, even with malformed HTML.</p>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <pre>
	 * // Extract all div elements with class='tag-block'
	 * List&lt;String&gt; blocks = extractXml(html, "div", Map.of("class", "tag-block"));
	 *
	 * // Extract all span elements (no attribute filtering)
	 * List&lt;String&gt; spans = extractXml(html, "span", null);
	 *
	 * // Extract divs with multiple attributes
	 * List&lt;String&gt; divs = extractXml(html, "div", Map.of("class", "header", "id", "main"));
	 * </pre>
	 *
	 * @param html The HTML/XML content to parse
	 * @param elementName The element name to extract (e.g., "div", "span")
	 * @param withAttributes Optional map of attribute name/value pairs that must match.
	 *                       Pass null or empty map to match all elements of the given name.
	 * @return List of HTML content strings (inner content of matching elements)
	 */
	public static List<String> extractXml(String html, String elementName, Map<String,String> withAttributes) {
		List<String> results = list();

		if (html == null || elementName == null) {
			return results;
		}

		// Find all opening tags of the specified element
		String openTag = "<" + elementName;
		int searchPos = 0;

		while ((searchPos = html.indexOf(openTag, searchPos)) != -1) {
			// Find the end of the opening tag
			int tagEnd = html.indexOf('>', searchPos);
			if (tagEnd == -1) break;

			String fullOpenTag = html.substring(searchPos, tagEnd + 1);

			// Check if attributes match
			boolean matches = true;
			if (withAttributes != null && !withAttributes.isEmpty()) {
				for (var entry : withAttributes.entrySet()) {
					String attrName = entry.getKey();
					String attrValue = entry.getValue();

					// Look for attribute in the tag (handle both single and double quotes)
					String pattern1 = attrName + "=\"" + attrValue + "\"";
					String pattern2 = attrName + "='" + attrValue + "'";

					if (!fullOpenTag.contains(pattern1) && !fullOpenTag.contains(pattern2)) {
						matches = false;
						break;
					}
				}
			}

			if (matches) {
				// Find matching closing tag by tracking depth
				int contentStart = tagEnd + 1;
				int depth = 1;
				int pos = contentStart;

				while (pos < html.length() && depth > 0) {
					// Look for next opening or closing tag of same element
					int nextOpen = html.indexOf("<" + elementName, pos);
					int nextClose = html.indexOf("</" + elementName + ">", pos);

					// Validate that nextOpen is actually an opening tag
					if (nextOpen != -1 && (nextOpen < nextClose || nextClose == -1)) {
						if (nextOpen + elementName.length() + 1 < html.length()) {
							char nextChar = html.charAt(nextOpen + elementName.length() + 1);
							if (nextChar == ' ' || nextChar == '>' || nextChar == '/') {
								depth++;
								pos = nextOpen + elementName.length() + 1;
								continue;
							}
						}
						// Not a valid opening tag, skip it
						pos = nextOpen + 1;
						continue;
					}

					if (nextClose != -1) {
						depth--;
						if (depth == 0) {
							// Found matching close tag
							results.add(html.substring(contentStart, nextClose));
							break;
						}
						pos = nextClose + elementName.length() + 3;
					} else {
						// No more closing tags
						break;
					}
				}
			}

			searchPos = tagEnd + 1;
		}

		return results;
	}
}