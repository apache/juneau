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
import static org.apache.juneau.marshall.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.stream.*;

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.junit.bct.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
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
 * These static methods are defined on {@link BctAssertions} and typically used via
 * {@code import static org.apache.juneau.marshall.junit.bct.BctAssertions.*;} (also re-exported through {@link TestBase}).
 * </p>
 * <dl>
 * 	<dt><b>{@link BctAssertions#assertBean(Object, String, String)}</b></dt>
 * 	<dd>Tests object properties with nested syntax support and collection iteration</dd>
 *
 * 	<dt><b>{@link BctAssertions#assertMap(java.util.Map, Object...)}</b></dt>
 * 	<dd>Tests map entries with the same nested property syntax as assertBean</dd>
 *
 * 	<dt><b>{@link BctAssertions#assertMapped(Object, java.util.function.BiFunction, String, String)}</b></dt>
 * 	<dd>Tests custom property access using BiFunction for non-standard objects</dd>
 *
 * 	<dt><b>{@link BctAssertions#assertList(Object, Object...)}</b></dt>
 * 	<dd>Tests list/collection elements with varargs for expected values</dd>
 *
 * 	<dt><b>{@link BctAssertions#assertBeans(Object, String, String...)}</b></dt>
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
public class TestUtils extends Utils {

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			assertEquals(values[0], values[i], fs("Elements at index {0} and {1} did not match. {0}={2}, {1}={3}", 0, i, r(values[0]), r(values[i])));
		}
	}

	/**
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertJson(String expected, Object value) {
		assertEquals(expected, json5(value));
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
		assertTrue(expectedType.isInstance(t), fs("Expected throwable of type: {0}.\nActual: {1}", expectedType.getName(), t == null ? "null" : t.getClass().getName()));
		var messages = getMessages(t);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return t;
	}

	public static <T extends Throwable> T assertThrowsWithMessage(Class<T> expectedType, String expectedSubstring, org.junit.jupiter.api.function.Executable executable) {
		var exception = Assertions.assertThrows(expectedType, executable);
		var messages = getMessages(exception);
		assertTrue(messages.contains(expectedSubstring), fs("Expected message to contain: {0}.\nActual:\n{1}", expectedSubstring, messages));
		return exception;
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
			var rc = new RestContext(new RestContext.Args(r.getClass(), null, null, () -> r, "", null, null, null, RestContext.ContextKind.ROOT));
			var ctx = new RestOpContext(TestUtils.class.getMethod("getSwagger", Class.class), rc);
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
		return json5(o);
	}

	public static <T> T json(String o, Class<T> c) {
		return safe(()->json5(o, c));
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
	 * Test whitespace and generated schema.
	 */
	public static final void validateXml(Object o, XmlSerializer s) throws Exception {
		s = s.copy().ws().ns().addNamespaceUrisToRoot().build();
		var xml = s.serialize(o);
		XmlTestUtils.checkXmlWhitespace(xml);
	}
}