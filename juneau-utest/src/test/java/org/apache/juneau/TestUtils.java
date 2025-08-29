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
import static java.lang.Integer.*;

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

public class TestUtils extends Utils2 {

	private static final ThreadLocal<TimeZone> SYSTEM_TIME_ZONE = new ThreadLocal<>();

	public static final ThreadLocal<Locale> SYSTEM_LOCALE = new ThreadLocal<>();

	/**
	 * Asserts that the entries in an array match the expected values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This method works with any array type including primitive arrays (<c>int[]</c>, <c>String[]</c>, etc.)
	 * and multi-dimensional arrays (<c>int[][]</c>, <c>String[][][]</c>). It converts the array to a list using
	 * {@link Utils#arrayToList(Object)} and then validates using the same logic as {@link #assertSet(Set, Object...)}.</p>
	 *
	 * <h5 class='section'>Basic Array Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test string array (varargs)</jc>
	 * 	assertArray(stringArray, <js>"item1"</js>, <js>"item2"</js>, <js>"item3"</js>);
	 *
	 * 	<jc>// Test string array (comma-delimited - preferred)</jc>
	 * 	assertArray(stringArray, <js>"item1,item2,item3"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Primitive Array Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test int array</jc>
	 * 	assertArray(intArray, <js>"1,2,3,4,5"</js>);
	 *
	 * 	<jc>// Test boolean array</jc>
	 * 	assertArray(boolArray, <js>"true,false,true"</js>);
	 *
	 * 	<jc>// Test byte array (useful for serialization testing)</jc>
	 * 	assertArray(byteArray, <js>"65,66,67"</js>); <jc>// ASCII: A,B,C</jc>
	 * </p>
	 *
	 * <h5 class='section'>Multi-Dimensional Arrays:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test 2D array (arrays are converted to string representation)</jc>
	 * 	assertArray(int2DArray, <js>"[1,2],[3,4],[5,6]"</js>);
	 *
	 * 	<jc>// Test 3D array</jc>
	 * 	assertArray(int3DArray, <js>"[[1,2],[3,4]],[[5,6],[7,8]]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Object Arrays:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test bean array (toString() representation)</jc>
	 * 	assertArray(beanArray, <js>"Bean1,Bean2,Bean3"</js>);
	 *
	 * 	<jc>// Test enum array</jc>
	 * 	assertArray(statusArray, <js>"ACTIVE,PENDING,CANCELLED"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Elements are converted to {@link Utils#r readable} format and compared as strings</jc>
	 * 	assertArray(new int[]{1, 2, 3}, <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 * 	assertArray(new String[]{"a", "b"}, <js>"a,b"</js>); <jc>// Comma-delimited syntax</jc>
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Use Predicate for functional testing</jc>
	 * 	Predicate&lt;Integer&gt; <jv>greaterThanZero</jv> = <jv>x</jv> -&gt; <jv>x</jv> &gt; 0;
	 * 	assertArray(<jk>new</jk> <jk>int</jk>[]{1, 2, 3}, <jv>greaterThanZero</jv>, <jv>greaterThanZero</jv>, <jv>greaterThanZero</jv>);
	 *
	 * 	<jc>// Mix predicates with other comparison types</jc>
	 * 	Predicate&lt;String&gt; <jv>hasLength3</jv> = <jv>s</jv> -&gt; <jv>s</jv>.length() == 3;
	 * 	assertArray(<jk>new</jk> String[]{<js>"abc"</js>, <js>"test"</js>}, <jv>hasLength3</jv>, <js>"test"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Non-String, non-Predicate objects use Objects.equals() comparison</jc>
	 * 	assertArray(<jk>new</jk> Integer[]{1, 2, 3}, 1, 2, 3); <jc>// Integer objects</jc>
	 * 	assertArray(<jk>new</jk> MyBean[]{<jv>bean1</jv>, <jv>bean2</jv>}, <jv>bean1</jv>, <jv>bean2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * <h5 class='section'>Escape Comma-Delimited Parsing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use '>' prefix to treat comma-containing string as single value</jc>
	 * 	assertArray(<jk>new</jk> String[]{<js>"foo,bar"</js>}, <js>">foo,bar"</js>); <jc>// Tests single element "foo,bar"</jc>
	 *
	 * 	<jc>// Useful for arrays containing CSV data or comma-separated strings</jc>
	 * 	assertArray(<jv>csvArray</jv>, <js>">item1,item2,item3"</js>); <jc>// Single element with commas</jc>
	 * </p>
	 *
	 * @param array The array to test. Can be any array type including primitives and multi-dimensional arrays.
	 * @param expected Either multiple arguments OR a single comma-delimited string of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws IllegalArgumentException if the input is not an array
	 * @throws AssertionError if the array size or contents don't match expected values
	 * @see #assertList(List, Object...)
	 * @see #assertSet(Set, Object...)
	 * @see Utils#arrayToList(Object)
	 */
	public static void assertArray(Object array, Object...expected) {
		assertCollection(arrayToList(array), expected);
	}

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This is the primary method for Bean-Centric Test Modernization (BCTM), supporting extensive property validation
	 * patterns including nested objects, collections, arrays, method chaining, and direct field access.</p>
	 *
	 * <h5 class='section'>Basic Usage:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test multiple properties</jc>
	 * 	assertBean(myBean, <js>"prop1,prop2,prop3"</js>, <js>"val1,val2,val3"</js>);
	 *
	 * 	<jc>// Test single property</jc>
	 * 	assertBean(myBean, <js>"name"</js>, <js>"John"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Nested Property Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test nested bean properties</jc>
	 * 	assertBean(myBean, <js>"address{street,city,state}"</js>, <js>"{123 Main St,Springfield,IL}"</js>);
	 *
	 * 	<jc>// Test arbitrarily deep nesting</jc>
	 * 	assertBean(myBean, <js>"person{address{geo{lat,lon}}}"</js>, <js>"{{{{40.7,-74.0}}}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Array and List Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test array/list elements by index</jc>
	 * 	assertBean(myBean, <js>"items{0,1,2}"</js>, <js>"{item1,item2,item3}"</js>);
	 *
	 * 	<jc>// Test nested properties within array elements</jc>
	 * 	assertBean(myBean, <js>"orders{0{id,total}}"</js>, <js>"{{123,99.95}}"</js>);
	 *
	 * 	<jc>// Test array length property</jc>
	 * 	assertBean(myBean, <js>"items{length}"</js>, <js>"{5}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Class Name Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test class properties (prefer simple names for maintainability)</jc>
	 * 	assertBean(myBean, <js>"obj{class{simpleName}}"</js>, <js>"{{MyClass}}"</js>);
	 *
	 * 	<jc>// Test full class names when needed</jc>
	 * 	assertBean(myBean, <js>"obj{class{name}}"</js>, <js>"{{com.example.MyClass}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Method Chaining Support:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test fluent setter chains (returns same object)</jc>
	 * 	assertBean(
	 * 		item.setType(<js>"foo"</js>).setFormat(<js>"bar"</js>).setDefault(<js>"baz"</js>),
	 * 		<js>"type,format,default"</js>,
	 * 		<js>"foo,bar,baz"</js>
	 * 	);
	 * </p>
	 *
	 * <h5 class='section'>Direct Field Access:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test public fields directly (no getters required)</jc>
	 * 	assertBean(myBean, <js>"f1,f2,f3"</js>, <js>"val1,val2,val3"</js>);
	 *
	 * 	<jc>// Test field properties with chaining</jc>
	 * 	assertBean(myBean, <js>"f1{length},f2{class{simpleName}}"</js>, <js>"{5},{{String}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection and Boolean Values:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test boolean values</jc>
	 * 	assertBean(myBean, <js>"enabled,visible"</js>, <js>"true,false"</js>);
	 *
	 * 	<jc>// Test enum collections</jc>
	 * 	assertBean(myBean, <js>"statuses"</js>, <js>"[ACTIVE,PENDING]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Value Syntax Rules:</h5>
	 * <ul>
	 * 	<li><b>Simple values:</b> <js>"value"</js> for direct property values</li>
	 * 	<li><b>Nested values:</b> <js>"{value}"</js> for single-level nested properties</li>
	 * 	<li><b>Deep nested values:</b> <js>"{{value}}"</js>, <js>"{{{value}}}"</js> for multiple nesting levels</li>
	 * 	<li><b>Array/Collection values:</b> <js>"[item1,item2]"</js> for collections</li>
	 * 	<li><b>Boolean values:</b> <js>"true"</js>, <js>"false"</js></li>
	 * 	<li><b>Null values:</b> <js>"null"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Property Access Priority:</h5>
	 * <ol>
	 * 	<li><b>is{Property}()</b> methods (for boolean properties)</li>
	 * 	<li><b>get{Property}()</b> methods</li>
	 * 	<li><b>Public fields</b> (direct field access)</li>
	 * 	<li><b>get(String)</b> methods (for Map-like objects)</li>
	 * 	<li><b>Special array properties:</b> <js>"length"</js> for arrays</li>
	 * </ol>
	 *
	 * @param bean The bean object to test. Must not be null.
	 * @param fields Comma-delimited list of property names to test. Supports nested syntax with {}.
	 * @param value Comma-delimited list of expected values. Must match the order of fields.
	 * @throws NullPointerException if the bean is null
	 * @throws AssertionError if any property values don't match expected values
	 */
	public static void assertBean(Object bean, String fields, String value) {
		assertArgNotNull("bean", bean);
		assertArgNotNull("fields", fields);
		assertArgNotNull("value", value);
		assertEquals(value, splitNested(fields).stream().map(x -> getEntry(bean, x)).collect(joining(",")));
	}

	/**
	 * Asserts that multiple beans in a collection have the expected property values.
	 *
	 * <p>This method validates that each bean in a collection has the specified property values,
	 * using the same property access logic as {@link #assertBean(Object, String, String)}.
	 * It's perfect for testing collections of similar objects or validation results.</p>
	 *
	 * <h5 class='section'>Basic Usage:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test list of user beans</jc>
	 * 	assertBeans(userList, <js>"name,age"</js>,
	 * 		<js>"John,25"</js>, <js>"Jane,30"</js>, <js>"Bob,35"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Complex Property Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test nested properties across multiple beans</jc>
	 * 	assertBeans(orderList, <js>"id,customer{name,email}"</js>,
	 * 		<js>"1,{John,john@example.com}"</js>,
	 * 		<js>"2,{Jane,jane@example.com}"</js>);
	 *
	 * 	<jc>// Test collection properties within beans</jc>
	 * 	assertBeans(cartList, <js>"items{0{name}},total"</js>,
	 * 		<js>"{{Laptop}},999.99"</js>,
	 * 		<js>"{{Phone}},599.99"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Validation Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test validation results</jc>
	 * 	assertBeans(validationErrors, <js>"field,message,code"</js>,
	 * 		<js>"email,Invalid email format,E001"</js>,
	 * 		<js>"age,Must be 18 or older,E002"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Parser Result Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test parsed object collections</jc>
	 * 	var parsed = JsonParser.DEFAULT.parse(jsonArray, MyBean[].class);
	 * 	assertBeans(Arrays.asList(parsed), <js>"prop1,prop2"</js>,
	 * 		<js>"val1,val2"</js>, <js>"val3,val4"</js>);
	 * </p>
	 *
	 * @param listOfBeans The collection of beans to check. Must not be null.
	 * @param fields A comma-delimited list of bean property names (supports nested syntax).
	 * @param values Array of expected value strings, one per bean. Each string contains comma-delimited values matching the fields.
	 * @throws AssertionError if the collection size doesn't match values array length or if any bean properties don't match
	 * @see #assertBean(Object, String, String)
	 */
	@SuppressWarnings("rawtypes")
	public static void assertBeans(Collection listOfBeans, String fields, String...values) {
		assertArgNotNull("listOfBeans", listOfBeans);
		assertArgNotNull("fields", fields);
		assertArgNotNull("values", values);

		assertEquals(values.length, listOfBeans.size(), fs("Expected {0} rows but had actual {1}", values.length, listOfBeans.size()));

		var r = 0;
		var f = splitNested(fields);
		for (var o : listOfBeans) {
			var actual = f.stream().map(x -> getEntry(o, x)).collect(joining(","));
			var r2 = r+1;
			assertEquals(r(values[r]), actual, fs("Object at row {0} didn't match.", r2));
			r++;
		}
	}

	/**
	 * Asserts the entries in a collection matches the expected values using flexible comparison logic.
	 *
	 * <p>This is the underlying implementation for {@link #assertSet(Set, Object...)}, {@link #assertList(List, Object...)},
	 * and {@link #assertArray(Object, Object...)}. It handles the dual syntax parsing and supports multiple value comparison modes.</p>
	 *
	 * <h5 class='section'>Dual Syntax Support:</h5>
	 * <ul>
	 * 	<li><b>Varargs:</b> <js>assertCollection(collection, "val1", "val2", "val3")</js></li>
	 * 	<li><b>Comma-delimited:</b> <js>assertCollection(collection, "val1,val2,val3")</js></li>
	 * 	<li><b>Escape prefix:</b> <js>assertCollection(collection, ">val1,val2,val3")</js> - treats as single value</li>
	 * </ul>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Elements are converted to {@link Utils#r readable} format and compared as strings</jc>
	 * 	assertCollection(List.of(1, 2, 3), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 * 	assertCollection(List.of("a", "b"), <js>"a,b"</js>); <jc>// Comma-delimited syntax</jc>
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Use Predicate&lt;T&gt; for functional testing</jc>
	 * 	Predicate&lt;Integer&gt; <jv>greaterThanOne</jv> = <jv>x</jv> -&gt; <jv>x</jv> &gt; 1;
	 * 	assertCollection(List.of(2, 3, 4), <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>);
	 *
	 * 	<jc>// Mix predicates with other comparison types</jc>
	 * 	Predicate&lt;String&gt; <jv>startsWithA</jv> = <jv>s</jv> -&gt; <jv>s</jv>.startsWith(<js>"a"</js>);
	 * 	assertCollection(List.of(<js>"apple"</js>, <js>"banana"</js>), <jv>startsWithA</jv>, <js>"banana"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Non-String, non-Predicate objects use Objects.equals() comparison</jc>
	 * 	assertCollection(List.of(1, 2, 3), 1, 2, 3); <jc>// Integer objects</jc>
	 * 	assertCollection(List.of(<jv>myBean1</jv>, <jv>myBean2</jv>), <jv>myBean1</jv>, <jv>myBean2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * <h5 class='section'>Escape Comma-Delimited Parsing:</h5>
	 * <p>The escape prefix '>' prevents comma-delimited parsing, useful when testing collections
	 * that contain elements with commas in their string representation.</p>
	 * <p class='bjava'>
	 * 	assertCollection(List.of(<js>"foo,bar"</js>), <js>"&gt;foo,bar"</js>); <jc>// Single element with comma</jc>
	 * </p>
	 *
	 * @param list The collection to test. Must not be null.
	 * @param expected Either multiple arguments OR a single comma-delimited string of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the collection size or contents don't match expected values
	 */
	@SuppressWarnings("unchecked")
	public static <T> void assertCollection(Collection<T> list, Object...expected) {
		assertArgNotNull("list", list);
		assertArgNotNull("expected", expected);

		// Special case when passing in a comma-delimited list.
		if (expected.length == 1 && expected[0] instanceof String && s(expected[0]).contains(","))
			expected = s(expected[0]).charAt(0) == '>' ? a(s(expected[0]).substring(1)) : splita(s(expected[0]));

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
	 * Asserts an object matches the expected string after it's been made {@link Utils#r readable}.
	 */
	public static void assertContains(String expected, Object actual) {
		assertArgNotNull("expected", expected);
		var a2 = r(actual);
		assertTrue(a2.contains(expected), fs("String did not contain expected substring.  expected={0}, actual={1}", expected, a2));
	}

	/**
	 * Similar to {@link #assertContains(String, Object)} but allows the expected to be a comma-delimited list of strings that
	 * all must match.
	 * @param expected
	 * @param actual
	 */
	public static void assertContainsAll(String expected, Object actual) {
		assertArgNotNull("expected", expected);
		var a2 = r(actual);
		for (var e : splita(expected))
			assertTrue(a2.contains(e), fs("String did not contain expected substring.  expected={0}, actual={1}", e, a2));
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	public static void assertEmpty(Collection<?> value) {
		assertNotNull(value, "Value was null.");
		assertTrue(value.isEmpty(), "Value was not empty.");
	}

	public static void assertEmpty(Map<?,?> value) {
		assertNotNull(value, "Value was null.");
		assertTrue(value.isEmpty(), "Value was not empty.");
	}

	public static void assertEmpty(Optional<?> value) {
		assertNotNull(value, "Value was null.");
		assertTrue(value.isEmpty(), "Optional was not empty.");
	}

	public static void assertEqualsAll(Object...values) {
		for (var i = 1; i < values.length; i++) {
			if (ne(values[0], values[i]))
				fail(fs("Elements at index {0} and {1} did not match.", 0, i));
		}
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

	public static void assertJsonMatches(Object o, String pattern) throws AssertionError {
		var json = json(o);
		assertTrue(getMatchPattern3(pattern).matcher(json).matches(), fs("JSON did not match pattern.\njson={0}", json));
	}

	public static void assertLines(String expected, Object value) {
		assertEquals(expected, r(value).replaceAll("\\r?\\n", "|"));
	}

	/**
	 * Asserts that the entries in a List match the expected values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This method works identically to {@link #assertSet(Set, Object...)} but is specifically typed for Lists.
	 * It supports the same dual syntax: varargs or comma-delimited strings.</p>
	 *
	 * <h5 class='section'>Basic Usage:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test list contents (varargs)</jc>
	 * 	assertList(myList, <js>"item1"</js>, <js>"item2"</js>, <js>"item3"</js>);
	 *
	 * 	<jc>// Test list contents (comma-delimited - preferred)</jc>
	 * 	assertList(myList, <js>"item1,item2,item3"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Ordered Collections:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Order matters in Lists (unlike Sets)</jc>
	 * 	assertList(orderedList, <js>"first,second,third"</js>);
	 *
	 * 	<jc>// Test JsonList contents</jc>
	 * 	assertList(jsonList, <js>"value1,value2,value3"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Elements are converted to {@link Utils#r readable} format and compared as strings</jc>
	 * 	assertList(List.of(1, 2, 3), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 * 	assertList(List.of("a", "b"), <js>"a,b"</js>); <jc>// Comma-delimited syntax</jc>
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Use Predicate for functional testing</jc>
	 * 	Predicate&lt;Integer&gt; <jv>even</jv> = <jv>x</jv> -&gt; <jv>x</jv> % 2 == 0;
	 * 	assertList(List.of(2, 4, 6), <jv>even</jv>, <jv>even</jv>, <jv>even</jv>);
	 *
	 * 	<jc>// Mix predicates with other comparison types</jc>
	 * 	Predicate&lt;String&gt; <jv>uppercase</jv> = <jv>s</jv> -&gt; <jv>s</jv>.equals(<jv>s</jv>.toUpperCase());
	 * 	assertList(List.of(<js>"HELLO"</js>, <js>"world"</js>), <jv>uppercase</jv>, <js>"world"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Non-String, non-Predicate objects use Objects.equals() comparison</jc>
	 * 	assertList(List.of(1, 2, 3), 1, 2, 3); <jc>// Integer objects</jc>
	 * 	assertList(List.of(<jv>myObj1</jv>, <jv>myObj2</jv>), <jv>myObj1</jv>, <jv>myObj2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * <h5 class='section'>Escape Comma-Delimited Parsing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Use '>' prefix to treat comma-containing string as single value</jc>
	 * 	assertList(List.of("foo,bar"), <js>">foo,bar"</js>); <jc>// Tests single element "foo,bar"</jc>
	 *
	 * 	<jc>// Useful for CSV data or comma-separated content</jc>
	 * 	assertList(<jv>csvList</jv>, <js>">item1,item2,item3"</js>); <jc>// Single element with commas</jc>
	 * </p>
	 *
	 * @param list The List to test. Must not be null.
	 * @param expected Either multiple arguments OR a single comma-delimited string of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the list size, order, or contents don't match expected values
	 * @see #assertSet(Set, Object...)
	 * @see #assertArray(Object, Object...)
	 */
	public static void assertList(List<?> list, Object...expected) {
		assertCollection(list, expected);
	}

	/**
	 * Asserts the entries in a map matches the expected strings after they've been made {@link Utils#r readable}.
	 * Can be used in cases where the map contains non-string keys.
	 */
	public static void assertMapPairs(Map<?,?> map, String...expected) {
		assertList(map.entrySet().stream().map(x -> r(x.getKey()) + "=" + r(x.getValue())).toList(), (Object[])expected);
	}

	/**
	 * Asserts that the values in the specified map are the specified values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This method works identically to {@link #assertBean(Object, String, String)} but is optimized for Java Maps.
	 * It supports the same nested property syntax and value formatting rules as <c>assertBean</c>.</p>
	 *
	 * <h5 class='section'>Basic Map Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test map entries</jc>
	 * 	assertMap(myMap, <js>"key1,key2,key3"</js>, <js>"val1,val2,val3"</js>);
	 *
	 * 	<jc>// Test single map entry</jc>
	 * 	assertMap(myMap, <js>"status"</js>, <js>"active"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Nested Object Testing in Maps:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test nested objects within map values</jc>
	 * 	assertMap(myMap, <js>"user{name,email}"</js>, <js>"{John,john@example.com}"</js>);
	 *
	 * 	<jc>// Test class properties of map values</jc>
	 * 	assertMap(myMap, <js>"items{class{simpleName}}"</js>, <js>"{{ArrayList}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection Testing in Maps:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test array/list values in maps</jc>
	 * 	assertMap(myMap, <js>"tags{0,1},count"</js>, <js>"{red,blue},2"</js>);
	 *
	 * 	<jc>// Test nested properties within collection elements</jc>
	 * 	assertMap(myMap, <js>"orders{0{id,total}}"</js>, <js>"{{123,99.95}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>BeanMap and JsonMap Usage:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Excellent for testing BeanMap instances</jc>
	 * 	var beanMap = BeanContext.DEFAULT.toBeanMap(myBean);
	 * 	assertMap(beanMap, <js>"name,age,active"</js>, <js>"John,30,true"</js>);
	 *
	 * 	<jc>// Test JsonMap parsing results</jc>
	 * 	var jsonMap = JsonMap.ofJson(<js>"{foo:'bar', baz:123}"</js>);
	 * 	assertMap(jsonMap, <js>"foo,baz"</js>, <js>"bar,123"</js>);
	 * </p>
	 *
	 * @param o The Map object to test. Must not be null.
	 * @param fields Comma-delimited list of map keys to test. Supports nested syntax with {}.
	 * @param value Comma-delimited list of expected values. Must match the order of fields.
	 * @throws NullPointerException if the map is null
	 * @throws AssertionError if any map values don't match expected values
	 * @see #assertBean(Object, String, String)
	 */
	public static void assertMap(Map<String,?> o, String fields, String value) {
		if (o == null) throw new NullPointerException("Map was null");
		assertEquals(value, splitNested(fields).stream().map(x -> getEntry(o, x)).collect(joining(",")));
	}

	public static <K> void assertMap(Map<K,?> o, Function<String,K> keyGenerator, String fields, String value) {
		if (o == null) throw new NullPointerException("Map was null");
		assertEquals(value, splitNested(fields).stream().map(x -> getEntry(o, x)).collect(joining(",")));
	}

	/**
	 * Asserts that mapped property access on an object returns expected values using a custom BiFunction.
	 *
	 * <p>This is the most powerful and flexible BCTM method, designed for testing objects that don't follow
	 * standard JavaBean patterns or require custom property access logic. The BiFunction allows complete
	 * control over how properties are retrieved from the target object.</p>
	 *
	 * <p>When the BiFunction throws an exception, it's automatically caught and the exception's
	 * simple class name becomes the property value for comparison (e.g., "NullPointerException").</p>
	 *
	 * <p>This method creates an intermediate LinkedHashMap to collect all property values before
	 * delegating to assertMap(Map, String, String). This ensures consistent ordering
	 * and supports the full nested property syntax.</p>
	 *
	 * @param <T> The type of object being tested
	 * @param o The object to test properties on
	 * @param f The BiFunction that extracts property values. Receives (object, propertyName) and returns the property value.
	 * @param properties Comma-delimited list of property names to test
	 * @param expected Comma-delimited list of expected values (exceptions become simple class names)
	 * @throws AssertionError if any mapped property values don't match expected values
	 * @see #assertBean(Object, String, String)
	 * @see #assertMap(Map, String, String)
	 */
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
	 * Asserts value when stringified matches the specified pattern.
	 */
	public static Object assertMatches(String pattern, Object value) {
		var m = getMatchPattern3(pattern).matcher(s(value));
		if (! m.matches()) {
			var msg = "Pattern didn't match: \n\tExpected:\n"+pattern+"\n\tActual:\n"+value;
			System.err.println(msg);  // For easier debugging.
			fail(msg);
		}
		return value;
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

	public static void assertNotEqualsAny(Object o, Object...values) {
		for (var i = 0; i < values.length; i++) {
			if (eq(o, values[i]))
				fail(fs("Element at index {0} unexpectedly matched.  expected={1}, actual={2}", i, values[i], s(o)));
		}
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

	public static void assertPresent(Optional<?> o) {
		assertTrue(o != null && o.isPresent(), "Optional was not present.");
	}

	public static void assertSameObject(Object o1, Object o2) {
		assertSame(o1, o2);
	}

	/**
	 * Asserts the serialized representation of the specified object.
	 */
	public static void assertSerialized(Object value, WriterSerializer s, String json) {
		assertEquals(json, s.toString(value));
	}

	/**
	 * Asserts that the entries in a Set/Collection match the expected values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This method is optimized for testing collections of simple values like strings, enums, or primitives.
	 * It supports two input formats: varargs or comma-delimited strings.</p>
	 *
	 * <h5 class='section'>Varargs Syntax:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test set with multiple string arguments</jc>
	 * 	assertSet(mySet, <js>"item1"</js>, <js>"item2"</js>, <js>"item3"</js>);
	 *
	 * 	<jc>// Test set with mixed types</jc>
	 * 	assertSet(mySet, <js>"active"</js>, <js>"pending"</js>, <js>"cancelled"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comma-Delimited String Syntax (Preferred):</h5>
	 * <p class='bjava'>
	 * 	<jc>// Cleaner syntax for multiple values</jc>
	 * 	assertSet(mySet, <js>"item1,item2,item3"</js>);
	 *
	 * 	<jc>// Test enum collections</jc>
	 * 	assertSet(statusSet, <js>"ACTIVE,PENDING,CANCELLED"</js>);
	 *
	 * 	<jc>// Test MediaType collections</jc>
	 * 	assertSet(mediaTypes, <js>"application/json,text/plain"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Special Cases:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test empty set</jc>
	 * 	assertSet(emptySet, <js>""</js>);
	 *
	 * 	<jc>// Test single value (both formats work)</jc>
	 * 	assertSet(singletonSet, <js>"onlyValue"</js>);
	 * 	assertSet(singletonSet, <js>"onlyValue"</js>); <jc>// Same as above</jc>
	 *
	 * 	<jc>// Escape comma-delimited parsing with '>' prefix</jc>
	 * 	assertSet(csvSet, <js>">item1,item2,item3"</js>); <jc>// Treats as single value "item1,item2,item3"</jc>
	 *
	 * 	<jc>// Useful when testing collections that contain comma-separated strings</jc>
	 * 	assertSet(Set.of("foo,bar"), <js>">foo,bar"</js>); <jc>// Tests single element "foo,bar"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Elements are converted to {@link Utils#r readable} format and compared as strings</jc>
	 * 	assertSet(Set.of(1, 2, 3), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 * 	assertSet(Set.of("a", "b"), <js>"a,b"</js>); <jc>// Comma-delimited syntax</jc>
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Use Predicate for functional testing</jc>
	 * 	Predicate&lt;Integer&gt; <jv>positive</jv> = <jv>x</jv> -&gt; <jv>x</jv> &gt; 0;
	 * 	assertSet(Set.of(1, 2, 3), <jv>positive</jv>, <jv>positive</jv>, <jv>positive</jv>);
	 *
	 * 	<jc>// Mix predicates with other comparison types</jc>
	 * 	Predicate&lt;String&gt; <jv>shortName</jv> = <jv>s</jv> -&gt; <jv>s</jv>.length() &lt; 5;
	 * 	assertSet(Set.of(<js>"cat"</js>, <js>"elephant"</js>), <jv>shortName</jv>, <js>"elephant"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Non-String, non-Predicate objects use Objects.equals() comparison</jc>
	 * 	assertSet(Set.of(1, 2, 3), 1, 2, 3); <jc>// Integer objects</jc>
	 * 	assertSet(Set.of(<jv>obj1</jv>, <jv>obj2</jv>), <jv>obj1</jv>, <jv>obj2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * <h5 class='section'>KeySet Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Perfect for testing Map key sets</jc>
	 * 	assertSet(myMap.keySet(), <js>"key1,key2,key3"</js>);
	 *
	 * 	<jc>// Test BeanMap property names</jc>
	 * 	assertSet(beanMap.keySet(), <js>"name,age,email"</js>);
	 * </p>
	 *
	 * @param list The Set or Collection to test. Must not be null.
	 * @param expected Either multiple arguments OR a single comma-delimited string of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the collection size or contents don't match expected values
	 * @see #assertList(List, Object...)
	 * @see #assertArray(Object, Object...)
	 */
	public static void assertSet(Set<?> list, Object...expected) {
		assertCollection(list, expected);
	}

	/**
	 * Asserts that a collection is not null and of the specified size.
	 */
	public static void assertSize(int expected, Collection<?> c) {
		assertEquals(expected, ofNullable(c).map(Collection::size).orElse(-1));
	}

	/**
	 * Asserts the entries in a list matches the expected strings after they've been made {@link Utils#r readable}.
	 */
	public static void assertStream(Stream<?> stream, Object...expected) {
		var list = stream.toList();
		if (list.size() != expected.length)
			fail(fs("Wrong list length.  expected={0}, actual={1}", expected.length, list.size()));
		for (var i = 0; i < expected.length; i++)
			if (ne(list.get(i), expected[i]))
				fail(fs("Element at index {0} did not match.  expected={1}, actual={2}", i, expected[i], r(list.get(i))));
	}

	/**
	 * Asserts an object matches the expected string after it's been made {@link Utils#r readable}.
	 */
	public static void assertString(String expected, Object actual) {
		assertEquals(expected, r(actual));
	}

	/**
	 * Asserts an object matches the expected string after it's been made {@link Utils#r readable}.
	 */
	public static void assertString(String expected, Object actual, Supplier<String> messageSupplier) {
		assertEquals(expected, r(actual), messageSupplier);
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	public static void assertStringEmpty(Object s) {
		assertNotNull(s);
		assertTrue(r(s).isEmpty());
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
	 * Asserts the JSON5 representation of the specified object.
	 */
	public static void assertType(Class<?> c, Object value) {
		assertTrue(c.isInstance(value), "Incorrect type");
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
	public static void assertTypes(Class<?> c, Object...value) {
		for (var i = 0; i < value.length; i++)
			assertTrue(c.isInstance(value[i]), fs("Incorrect type at index [{0}].", i));
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

	static String getMessages(Throwable t) {
		return Stream.iterate(t, Throwable::getCause).takeWhile(e -> e != null).map(Throwable::getMessage).collect(joining("\n"));
	}

	private static String getEntry(Object o, String name) {
		var i = name.indexOf("{");
		var pn = i == -1 ? name : name.substring(0, i);
		var spn = i == -1 ? null : splitNestedInner(name);
		var e = getEntry2(o, pn);
		if (spn == null || e == null) return r(e);
		return spn.stream().map(x -> getEntry(e, x)).collect(joining(",","{","}"));
	}

	@SuppressWarnings("unchecked")
	private static Object getEntry2(Object o, String name) {
		if (o instanceof List o2) return isNumeric(name) ? o2.get(parseInt(name)) : getBeanProp(o, name);
		if (o.getClass().isArray()) return isNumeric(name) ? Array.get(o, parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Map o2) return opt(o2.get(eq("<<<NULL>>>",name) ? null : name)).orElse(name.equals("class") ? o.getClass() : null);
		if (o instanceof Iterable o2) return isNumeric(name) ? toList(o2).get(parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Iterator o2) return isNumeric(name) ? toList(o2).get(parseInt(name)) : getBeanProp(o, name);
		if (o instanceof Enumeration o2) return isNumeric(name) ? toList(o2).get(parseInt(name)) : getBeanProp(o, name);
		return getBeanProp(o, name);
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
	 * Creates an input stream from the specified string.
	 *
	 * @param in The contents of the reader.
	 * @return A new input stream.
	 */
	public static final ByteArrayInputStream inputStream(String in) {
		return new ByteArrayInputStream(in.getBytes());
	}

	private static boolean isNumeric(String name) {
		return StringUtils.isNumeric(name);
	}

	public static String json(Object o) {
		return Json5.DEFAULT.write(o);
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
}
