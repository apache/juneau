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
package org.apache.juneau.junit.bct;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.function.ResettableSupplier;
import org.apache.juneau.common.utils.*;
import org.opentest4j.*;

/**
 * Comprehensive utility class for Bean-Centric Tests (BCT) and general testing operations.
 *
 * <p>This class extends the functionality provided by the JUnit Assertions class, with particular emphasis
 * on the Bean-Centric Testing (BCT) framework. BCT enables sophisticated assertion patterns for
 * testing object properties, collections, maps, and complex nested structures with minimal code.</p>
 *
 * <h5 class='section'>Bean-Centric Testing (BCT) Framework:</h5>
 * <p>The BCT framework consists of several key components:</p>
 * <ul>
 *    <li><b>{@link BeanConverter}:</b> Core interface for object conversion and property access</li>
 *    <li><b>{@link BasicBeanConverter}:</b> Default implementation with extensible type handlers</li>
 *    <li><b>Assertion Methods:</b> High-level testing methods that leverage the converter framework</li>
 * </ul>
 *
 * <h5 class='section'>Primary BCT Assertion Methods:</h5>
 * <dl>
 *    <dt><b>{@link #assertBean(Object, String, String)}</b></dt>
 *    <dd>Tests object properties with nested syntax support and collection iteration</dd>
 *
 *    <dt><b>{@link #assertBeans(Collection, String, String...)}</b></dt>
 *    <dd>Tests collections of objects by extracting and comparing specific fields</dd>
 *
 *    <dt><b>{@link #assertMapped(Object, java.util.function.BiFunction, String, String)}</b></dt>
 *    <dd>Tests custom property access using BiFunction for non-standard objects</dd>
 *
 *    <dt><b>{@link #assertList(List, Object...)}</b></dt>
 *    <dd>Tests list/collection elements with varargs for expected values</dd>
 * </dl>
 *
 * <h5 class='section'>BCT Advanced Features:</h5>
 * <ul>
 *    <li><b>Nested Property Syntax:</b> "address{street,city}" for testing nested objects</li>
 *    <li><b>Collection Iteration:</b> "#{address{street,city}}" syntax for testing all elements</li>
 *    <li><b>Universal Size Properties:</b> "length" and "size" work on all collection types</li>
 *    <li><b>Array/List Access:</b> Numeric indices for element-specific testing</li>
 *    <li><b>Method Chaining:</b> Fluent setters can be tested directly</li>
 *    <li><b>Direct Field Access:</b> Public fields accessed without getters</li>
 *    <li><b>Map Key Access:</b> Including special <js>"&lt;null&gt;"</js> syntax for null keys</li>
 * </ul>
 *
 * <h5 class='section'>Converter Extensibility:</h5>
 * <p>The BCT framework is built on the extensible {@link BasicBeanConverter} which allows:</p>
 * <ul>
 *    <li><b>Custom Stringifiers:</b> Type-specific string conversion logic</li>
 *    <li><b>Custom Listifiers:</b> Collection-type conversion for iteration</li>
 *    <li><b>Custom Swappers:</b> Object transformation before conversion</li>
 *    <li><b>Custom PropertyExtractors:</b> Property extraction</li>
 *    <li><b>Configurable Settings:</b> Formatting, delimiters, and display options</li>
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 *
 * <p><b>Basic Property Testing:</b></p>
 * <p class='bjava'>
 *    <jc>// Test multiple properties</jc>
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name,age,active"</js>, <js>"John,30,true"</js>);
 *
 *    <jc>// Test nested properties - user has getAddress() returning Address with getStreet() and getCity()</jc>
 *    <jsm>assertBean</jsm>(<jv>user</jv>, <js>"name,address{street,city}"</js>, <js>"John,{123 Main St,Springfield}"</js>);
 * </p>
 *
 * <p><b>Collection and Array Testing:</b></p>
 * <p class='bjava'>
 *    <jc>// Test collection size and iterate over all elements - order has getItems() returning List&lt;Product&gt; where Product has getName()</jc>
 *    <jsm>assertBean</jsm>(<jv>order</jv>, <js>"items{length,#{name}}"</js>, <js>"{3,[{Laptop},{Phone},{Tablet}]}"</js>);
 *
 *    <jc>// Test specific array elements - listOfData is a List&lt;DataObject&gt; where DataObject has getData()</jc>
 *    <jsm>assertBean</jsm>(<jv>listOfData</jv>, <js>"0{data},1{data}"</js>, <js>"{100},{200}"</js>);
 * </p>
 *
 * <p><b>Collection Testing:</b></p>
 * <p class='bjava'>
 *    <jc>// Test list elements</jc>
 *    <jsm>assertList</jsm>(tags, <js>"red"</js>, <js>"green"</js>, <js>"blue"</js>);
 *
 *    <jc>// Test map entries using assertBean</jc>
 *    <jsm>assertBean</jsm>(<jv>config</jv>, <js>"timeout,retries"</js>, <js>"30000,3"</js>);
 * </p>
 *
 * <p><b>Custom Property Access:</b></p>
 * <p class='bjava'>
 *    <jc>// Test with custom accessor function</jc>
 *    <jsm>assertMapped</jsm>(<jv>myObject</jv>, (<jp>obj</jp>, <jp>prop</jp>) -> <jp>obj</jp>.getProperty(<jp>prop</jp>),
 *       <js>"prop1,prop2"</js>, <js>"value1,value2"</js>);
 * </p>
 *
	 * <h5 class='section'>Customizing the Default Converter:</h5>
	 * <p>The default bean converter can be customized on a per-thread basis using:</p>
	 * <ul>
	 *    <li><b>{@link #setConverter(BeanConverter)}:</b> Set a custom converter for the current thread</li>
	 *    <li><b>{@link #resetConverter()}:</b> Reset to the system default converter</li>
	 * </ul>
	 *
	 * <p class='bjava'>
	 *    <jc>// Example: Set custom converter in @BeforeEach method</jc>
	 *    <ja>@BeforeEach</ja>
	 *    <jk>void</jk> <jsm>setUp</jsm>() {
	 *       <jk>var</jk> <jv>customConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *          .defaultSettings()
	 *          .addStringifier(LocalDate.<jk>class</jk>, <jp>date</jp> -> <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>))
	 *          .addStringifier(MyType.<jk>class</jk>, <jp>obj</jp> -> <jp>obj</jp>.customFormat())
	 *          .build();
	 *       BctAssertions.<jsm>setConverter</jsm>(<jv>customConverter</jv>);
	 *    }
	 *
	 *    <jc>// All assertions in this test class now use the custom converter</jc>
	 *    <ja>@Test</ja>
	 *    <jk>void</jk> <jsm>testWithCustomConverter</jsm>() {
	 *       <jsm>assertBean</jsm>(<jv>myObject</jv>, <js>"date,property"</js>, <js>"2023-12-01,value"</js>);
	 *    }
	 *
	 *    <jc>// Clean up in @AfterEach method</jc>
	 *    <ja>@AfterEach</ja>
	 *    <jk>void</jk> <jsm>tearDown</jsm>() {
	 *       BctAssertions.<jsm>resetConverter</jsm>();
	 *    }
	 * </p>
	 *
	 * <p class='bjava'>
	 *    <jc>// Example: Per-test method converter override</jc>
	 *    <ja>@Test</ja>
	 *    <jk>void</jk> <jsm>testSpecificFormat</jsm>() {
	 *       <jk>var</jk> <jv>dateConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *          .defaultSettings()
	 *          .addStringifier(LocalDateTime.<jk>class</jk>, <jp>dt</jp> -> <jp>dt</jp>.format(DateTimeFormatter.<jsf>ISO_DATE_TIME</jsf>))
	 *          .build();
	 *       BctAssertions.<jsm>setConverter</jsm>(<jv>dateConverter</jv>);
	 *       <jkt>try</jkt> {
	 *          <jsm>assertBean</jsm>(<jv>event</jv>, <js>"timestamp"</js>, <js>"2023-12-01T10:30:00"</js>);
	 *       } <jkt>finally</jkt> {
	 *          BctAssertions.<jsm>resetConverter</jsm>();
	 *       }
	 *    }
 * </p>
 *
 * <h5 class='section'>Performance and Thread Safety:</h5>
 * <p>The BCT framework is designed for high performance with:</p>
 * <ul>
 *    <li><b>Caching:</b> Type-to-handler mappings cached for fast lookup</li>
 *    <li><b>Thread Safety:</b> All operations are thread-safe for concurrent testing</li>
 *    <li><b>Thread-Local Storage:</b> Default converter is stored per-thread, allowing parallel test execution</li>
 *    <li><b>Minimal Allocation:</b> Efficient object reuse and minimal temporary objects</li>
 * </ul>
 *
 * @see BeanConverter
 * @see BasicBeanConverter
 * @see #setConverter(BeanConverter)
 * @see #resetConverter()
 */
public class BctAssertions {

	// Thread-local memoized supplier for default converter (defaults to BasicBeanConverter.DEFAULT)
	private static final ThreadLocal<ResettableSupplier<BeanConverter>> CONVERTER_SUPPLIER = ThreadLocal.withInitial(() -> memoizeResettable(() -> BasicBeanConverter.DEFAULT));

	// Thread-local override for method-level converter customization
	private static final ThreadLocal<BeanConverter> CONVERTER_OVERRIDE = new ThreadLocal<>();

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values after being converted to strings.
	 *
	 * <p>Same as {@link #assertBean(Supplier, Object, String, String)} but without a custom message.</p>
	 *
	 * @param actual The bean object to test. Must not be null.
	 * @param fields Comma-delimited list of property names to test. Supports nested syntax with {}.
	 * @param expected Comma-delimited list of expected values. Must match the order of fields.
	 * @throws NullPointerException if the bean is null
	 * @throws AssertionError if any property values don't match expected values
	 * @see #assertBean(Supplier, Object, String, String)
	 */
	public static void assertBean(Object actual, String fields, String expected) {
		assertBean(null, actual, fields, expected);
	}

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values after being converted to strings.
	 *
	 * <p>This is the primary method for Bean-Centric Tests (BCT), supporting extensive property validation
	 * patterns including nested objects, collections, arrays, method chaining, direct field access, collection iteration
	 * with <js>"#{property}"</js> syntax, and universal <js>"length"</js>/<js>"size"</js> properties for all collection types.</p>
	 *
	 * <p>The method uses the default converter (set via {@link #setConverter(BeanConverter)}) for object introspection
	 * and value extraction. The converter provides sophisticated property access through the {@link BeanConverter}
	 * interface, supporting multiple fallback mechanisms for accessing object properties and values.</p>
	 *
	 * <h5 class='section'>Basic Usage:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test multiple properties</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"prop1,prop2,prop3"</js>, <js>"val1,val2,val3"</js>);
	 *
	 *    <jc>// Test single property</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"name"</js>, <js>"John"</js>);
	 *
	 *    <jc>// With custom error message</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"name,age"</js>, <js>"John,30"</js>, () -> <js>"User validation failed"</js>);
	 *
	 *    <jc>// With formatted message using Utils.fs() for convenient message suppliers with arguments</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"name,age"</js>, <js>"John,30"</js>, <jsm>fs</jsm>(<js>"User {0} validation failed"</js>, <js>"John"</js>));
	 * </p>
	 *
	 * <h5 class='section'>Nested Property Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test nested bean properties</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"name,address{street,city,state}"</js>, <js>"John,{123 Main St,Springfield,IL}"</js>);
	 *
	 *    <jc>// Test arbitrarily deep nesting</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"name,person{address{geo{lat,lon}}}"</js>, <js>"John,{{{40.7,-74.0}}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Array, List, and Stream Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test array/list elements by index - items is a String[] or List&lt;String&gt;</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"items{0,1,2}"</js>, <js>"{item1,item2,item3}"</js>);
	 *
	 *    <jc>// Test nested properties within array elements - orders is a List&lt;Order&gt; where Order has getId() and getTotal()</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"orders{0{id,total}}"</js>, <js>"{{123,99.95}}"</js>);
	 *
	 *    <jc>// Test array length property - items can be any array or collection type</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"items{length}"</js>, <js>"{5}"</js>);
	 *
	 *    <jc>// Works with any iterable type including Streams - userStream returns a Stream&lt;User&gt; where User has getName()</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"userStream{#{name}}"</js>, <js>"[{Alice},{Bob}]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection Iteration Syntax:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test properties across ALL elements in a collection using #{...} syntax - userList is a List&lt;User&gt; where User has getName()</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"userList{#{name}}"</js>, <js>"[{John},{Jane},{Bob}]"</js>);
	 *
	 *    <jc>// Test multiple properties from each element - orderList is a List&lt;Order&gt; where Order has getId() and getStatus()</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"orderList{#{id,status}}"</js>, <js>"[{123,ACTIVE},{124,PENDING}]"</js>);
	 *
	 *    <jc>// Works with nested properties within each element - customers is a List&lt;Customer&gt; where Customer has getAddress() returning Address with getCity()</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"customers{#{address{city}}}"</js>, <js>"[{{New York}},{{Los Angeles}}]"</js>);
	 *
	 *    <jc>// Works with arrays and any iterable collection type (including Streams)</jc>
	 *    <jsm>assertBean</jsm>(<jv>config</jv>, <js>"itemArray{#{type}}"</js>, <js>"[{String},{Integer},{Boolean}]"</js>);
	 *    <jsm>assertBean</jsm>(<jv>data</jv>, <js>"statusSet{#{name}}"</js>, <js>"[{ACTIVE},{PENDING},{CANCELLED}]"</js>);
	 *    <jsm>assertBean</jsm>(<jv>processor</jv>, <js>"dataStream{#{value}}"</js>, <js>"[{A},{B},{C}]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Universal Collection Size Properties:</h5>
	 * <p class='bjava'>
	 *    <jc>// Both 'length' and 'size' work universally across all collection types</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myArray{length}"</js>, <js>"{5}"</js>);        <jc>// Arrays</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myArray{size}"</js>, <js>"{5}"</js>);          <jc>// Also works for arrays</jc>
	 *
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myList{size}"</js>, <js>"{3}"</js>);           <jc>// Collections</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myList{length}"</js>, <js>"{3}"</js>);         <jc>// Also works for collections</jc>
	 *
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myMap{size}"</js>, <js>"{7}"</js>);            <jc>// Maps</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"myMap{length}"</js>, <js>"{7}"</js>);          <jc>// Also works for maps</jc>
	 * </p>
	 *
	 * <h5 class='section'>Class Name Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test class properties (prefer simple names for maintainability)</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"obj{class{simpleName}}"</js>, <js>"{{MyClass}}"</js>);
	 *
	 *    <jc>// Test full class names when needed</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"obj{class{name}}"</js>, <js>"{{com.example.MyClass}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Method Chaining Support:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test fluent setter chains (returns same object)</jc>
	 *    <jsm>assertBean</jsm>(
	 *       <jv>item</jv>.setType(<js>"foo"</js>).setFormat(<js>"bar"</js>).setDefault(<js>"baz"</js>),
	 *       <js>"type,format,default"</js>,
	 *       <js>"foo,bar,baz"</js>
	 *    );
	 * </p>
	 *
	 * <h5 class='section'>Advanced Collection Analysis:</h5>
	 * <p class='bjava'>
	 *    <jc>// Combine size/length, metadata, and content iteration in single assertions - users is a List&lt;User&gt;</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"users{length,class{simpleName},#{name}}"</js>,
	 *       <js>"{3,{ArrayList},[{John},{Jane},{Bob}]}"</js>);
	 *
	 *    <jc>// Comprehensive collection validation with multiple iteration patterns - items is a List&lt;Product&gt; where Product has getName() and getPrice()</jc>
	 *    <jsm>assertBean</jsm>(<jv>order</jv>, <js>"items{size,#{name},#{price}}"</js>,
	 *       <js>"{3,[{Laptop},{Phone},{Tablet}],[{999.99},{599.99},{399.99}]}"</js>);
	 *
	 *    <jc>// Perfect for validation testing - verify error count and details; errors is a List&lt;ValidationError&gt; where ValidationError has getField() and getCode()</jc>
	 *    <jsm>assertBean</jsm>(<jv>result</jv>, <js>"errors{length,#{field},#{code}}"</js>,
	 *       <js>"{2,[{email},{password}],[{E001},{E002}]}"</js>);
	 *
	 *    <jc>// Mixed collection types with consistent syntax - results and metadata are different collection types</jc>
	 *    <jsm>assertBean</jsm>(<jv>response</jv>, <js>"results{size},metadata{length}"</js>, <js>"{25},{4}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Direct Field Access:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test public fields directly (no getters required)</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"f1,f2,f3"</js>, <js>"val1,val2,val3"</js>);
	 *
	 *    <jc>// Test field properties with chaining</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"f1{length},f2{class{simpleName}}"</js>, <js>"{5},{{String}}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Map Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test map values by key</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"configMap{timeout,retries}"</js>, <js>"{30000,3}"</js>);
	 *
	 *    <jc>// Test map size</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"settings{size}"</js>, <js>"{5}"</js>);
	 *
	 *    <jc>// Test null keys using special &lt;null&gt; syntax</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"mapWithNullKey{&lt;null&gt;}"</js>, <js>"{nullKeyValue}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection and Boolean Values:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test boolean values</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"enabled,visible"</js>, <js>"true,false"</js>);
	 *
	 *    <jc>// Test enum collections</jc>
	 *    <jsm>assertBean</jsm>(<jv>myBean</jv>, <js>"statuses"</js>, <js>"[ACTIVE,PENDING]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Value Syntax Rules:</h5>
	 * <ul>
	 *    <li><b>Simple values:</b> <js>"value"</js> for direct property values</li>
	 *    <li><b>Nested values:</b> <js>"{value}"</js> for single-level nested properties</li>
	 *    <li><b>Deep nested values:</b> <js>"{{value}}"</js>, <js>"{{{value}}}"</js> for multiple nesting levels</li>
	 *    <li><b>Array/Collection values:</b> <js>"[item1,item2]"</js> for collections</li>
	 *    <li><b>Collection iteration:</b> <js>"#{property}"</js> iterates over ALL collection elements, returns <js>"[{val1},{val2}]"</js></li>
	 *    <li><b>Universal size properties:</b> <js>"length"</js> and <js>"size"</js> work on arrays, collections, and maps</li>
	 *    <li><b>Boolean values:</b> <js>"true"</js>, <js>"false"</js></li>
	 *    <li><b>Null values:</b> <js>"null"</js></li>
	 * </ul>
	 *
	 * <h5 class='section'>Property Access Priority:</h5>
	 * <ol>
	 *    <li><b>Collection/Array access:</b> Numeric indices for arrays/lists (e.g., <js>"0"</js>, <js>"1"</js>)</li>
	 *    <li><b>Universal size properties:</b> <js>"length"</js> and <js>"size"</js> for arrays, collections, and maps</li>
	 *    <li><b>Map key access:</b> Direct key lookup for Map objects (including <js>"&lt;null&gt;"</js> for null keys)</li>
	 *    <li><b>is{Property}()</b> methods (for boolean properties)</li>
	 *    <li><b>get{Property}()</b> methods</li>
	 *    <li><b>Public fields</b> (direct field access)</li>
	 * </ol>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 *                Use {@link org.apache.juneau.common.utils.Utils#fs(String, Object...) Utils.fs()} to conveniently
	 *                create message suppliers with format arguments (e.g., <code>fs("User {0} validation failed", userName)</code>).
	 * @param actual The bean object to test. Must not be null.
	 * @param fields Comma-delimited list of property names to test. Supports nested syntax with {}.
	 * @param expected Comma-delimited list of expected values. Must match the order of fields.
	 * @throws NullPointerException if the bean is null
	 * @throws AssertionError if any property values don't match expected values
	 * @see BeanConverter
	 * @see BasicBeanConverter
	 * @see #setConverter(BeanConverter)
	 * @see org.apache.juneau.common.utils.Utils#fs(String, Object...)
	 */
	public static void assertBean(Supplier<String> message, Object actual, String fields, String expected) {
		assertNotNull(actual, "Actual was null.");
		assertArgNotNull("fields", fields);
		assertArgNotNull("expected", expected);
		var converter = getConverter();
		assertEquals(expected, tokenize(fields).stream().map(x -> converter.getNested(actual, x)).collect(joining(",")), composeMessage(message, "Bean assertion failed."));
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
	 *    <jc>// Test list of user beans</jc>
	 *    <jsm>assertBeans</jsm>(<jv>userList</jv>, <js>"name,age"</js>,
	 *       <js>"John,25"</js>, <js>"Jane,30"</js>, <js>"Bob,35"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Complex Property Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test nested properties across multiple beans - orderList is a List&lt;Order&gt; where Order has getId() and getCustomer() returning Customer with getName() and getEmail()</jc>
	 *    <jsm>assertBeans</jsm>(<jv>orderList</jv>, <js>"id,customer{name,email}"</js>,
	 *       <js>"1,{John,john@example.com}"</js>,
	 *       <js>"2,{Jane,jane@example.com}"</js>);
	 *
	 *    <jc>// Test collection properties within beans - cartList is a List&lt;ShoppingCart&gt; where ShoppingCart has getItems() returning List&lt;Product&gt; and getTotal()</jc>
	 *    <jsm>assertBeans</jsm>(<jv>cartList</jv>, <js>"items{0{name}},total"</js>,
	 *       <js>"{{Laptop}},999.99"</js>,
	 *       <js>"{{Phone}},599.99"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Validation Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test validation results</jc>
	 *    <jsm>assertBeans</jsm>(<jv>validationErrors</jv>, <js>"field,message,code"</js>,
	 *       <js>"email,Invalid email format,E001"</js>,
	 *       <js>"age,Must be 18 or older,E002"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection Iteration Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test collection iteration within beans (#{...} syntax)</jc>
	 *    <jsm>assertBeans</jsm>(<jv>departmentList</jv>, <js>"name,employees{#{name}}"</js>,
	 *       <js>"Engineering,[{Alice},{Bob},{Charlie}]"</js>,
	 *       <js>"Marketing,[{David},{Eve}]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Parser Result Testing:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test parsed object collections</jc>
	 *    <jk>var</jk> <jv>parsed</jv> = JsonParser.<jsf>DEFAULT</jsf>.parse(<jv>jsonArray</jv>, MyBean[].class);
	 *    <jsm>assertBeans</jsm>(<jsm>l</jsm>(<jv>parsed</jv>), <js>"prop1,prop2"</js>,
	 *       <js>"val1,val2"</js>, <js>"val3,val4"</js>);
	 * </p>
	 *
	 * @param actual The collection of beans to check. Must not be null.
	 * @param fields A comma-delimited list of bean property names (supports nested syntax).
	 * @param expected Array of expected value strings, one per bean. Each string contains comma-delimited values matching the fields.
	 * @throws AssertionError if the collection size doesn't match values array length or if any bean properties don't match
	 * @see #assertBean(Object, String, String)
	 */
	public static void assertBeans(Object actual, String fields, String...expected) {
		assertBeans(null, actual, fields, expected);
	}

	/**
	 * Asserts that multiple beans in a collection have the expected property values.
	 *
	 * <p>Same as {@link #assertBeans(Object, String, String...)} but with a custom error message.</p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param actual The collection of beans to check. Must not be null.
	 * @param fields A comma-delimited list of bean property names (supports nested syntax).
	 * @param expected Array of expected value strings, one per bean. Each string contains comma-delimited values matching the fields.
	 * @throws AssertionError if the collection size doesn't match values array length or if any bean properties don't match
	 * @see #assertBean(Object, String, String)
	 */
	public static void assertBeans(Supplier<String> message, Object actual, String fields, String...expected) {
		assertNotNull(actual, "Value was null.");
		assertArgNotNull("fields", fields);
		assertArgNotNull("expected", expected);

		var converter = getConverter();
		var tokens = tokenize(fields);
		var errors = new ArrayList<AssertionFailedError>();
		List<Object> actualList = converter.listify(actual);

		if (ne(expected.length, actualList.size())) {
			errors.add(assertEqualsFailed(expected.length, actualList.size(), composeMessage(message, "Wrong number of beans.")));
		} else {
			for (var i = 0; i < actualList.size(); i++) {
				var i2 = i;
				var e = converter.stringify(expected[i]);
				var a = tokens.stream().map(x -> converter.getNested(actualList.get(i2), x)).collect(joining(","));
				if (ne(e, a)) {
					errors.add(assertEqualsFailed(e, a, composeMessage(message, "Bean at row <{0}> did not match.", i)));
				}
			}
		}

		if (errors.isEmpty())
			return;

		var actualStrings = new ArrayList<String>();
		for (var o : actualList) {
			actualStrings.add(tokens.stream().map(x -> converter.getNested(o, x)).collect(joining(",")));
		}

		throw assertEqualsFailed(Stream.of(expected).map(StringUtils::escapeForJava).collect(joining("\", \"", "\"", "\"")),
			actualStrings.stream().map(StringUtils::escapeForJava).collect(joining("\", \"", "\"", "\"")),
			composeMessage(message, "{0} bean assertions failed:\n{1}", errors.size(), errors.stream().map(x -> x.getMessage()).collect(joining("\n"))));
	}

	/**
	 * Asserts that the string representation of an object contains the expected substring.
	 *
	 * <p>Same as {@link #assertContains(Supplier, String, Object)} but without a custom message.</p>
	 *
	 * @param expected The substring that must be present in the actual object's string representation
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the actual object is null or its string representation doesn't contain the expected substring
	 * @see #assertContains(Supplier, String, Object)
	 */
	public static void assertContains(String expected, Object actual) {
		assertContains(null, expected, actual);
	}

	/**
	 * Asserts that the string representation of an object contains the expected substring.
	 *
	 * <p>This method converts the actual object to its string representation using the current
	 * {@link BeanConverter} and then checks if it contains the expected substring. This is useful
	 * for testing partial content matches without requiring exact string equality.</p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test that error message contains key information</jc>
	 *    <jsm>assertContains</jsm>(<js>"FileNotFoundException"</js>, <jv>exception</jv>);
	 *
	 *    <jc>// Test that object string representation contains expected data</jc>
	 *    <jsm>assertContains</jsm>(<js>"status=ACTIVE"</js>, <jv>user</jv>);
	 *
	 *    <jc>// Test partial JSON/XML content</jc>
	 *    <jsm>assertContains</jsm>(<js>"\"name\":\"John\""</js>, <jv>jsonResponse</jv>);
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param expected The substring that must be present in the actual object's string representation
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the actual object is null or its string representation doesn't contain the expected substring
	 * @see #assertContainsAll(Object, String...) for multiple substring assertions
	 * @see #assertString(String, Object) for exact string matching
	 */
	public static void assertContains(Supplier<String> message, String expected, Object actual) {
		assertArgNotNull("expected", expected);
		assertArgNotNull("actual", actual);
		assertNotNull(actual, "Value was null.");

		var a = getConverter().stringify(actual);
		assertTrue(a.contains(expected), composeMessage(message, "String did not contain expected substring.  ==> expected: <{0}> but was: <{1}>", expected, a));
	}

	/**
	 * Asserts that the string representation of an object contains all specified substrings.
	 *
	 * <p>Same as {@link #assertContainsAll(Supplier, Object, String...)} but without a custom message.</p>
	 *
	 * @param actual The object to test. Must not be null.
	 * @param expected Multiple substrings that must all be present in the actual object's string representation
	 * @throws AssertionError if the actual object is null or its string representation doesn't contain all expected substrings
	 * @see #assertContainsAll(Supplier, Object, String...)
	 */
	public static void assertContainsAll(Object actual, String...expected) {
		assertContainsAll(null, actual, expected);
	}

	/**
	 * Asserts that the string representation of an object contains all specified substrings.
	 *
	 * <p>This method is similar to {@link #assertContains(String, Object)} but tests for multiple
	 * required substrings. All provided substrings must be present in the actual object's string
	 * representation for the assertion to pass.</p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test that error contains multiple pieces of information</jc>
	 *    <jsm>assertContainsAll</jsm>(<jv>exception</jv>, <js>"FileNotFoundException"</js>, <js>"config.xml"</js>, <js>"/etc"</js>);
	 *
	 *    <jc>// Test that user object contains expected fields</jc>
	 *    <jsm>assertContainsAll</jsm>(<jv>user</jv>, <js>"name=John"</js>, <js>"age=30"</js>, <js>"status=ACTIVE"</js>);
	 *
	 *    <jc>// Test log output contains all required entries</jc>
	 *    <jsm>assertContainsAll</jsm>(<jv>logOutput</jv>, <js>"INFO"</js>, <js>"Started"</js>, <js>"Successfully"</js>);
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param actual The object to test. Must not be null.
	 * @param expected Multiple substrings that must all be present in the actual object's string representation
	 * @throws AssertionError if the actual object is null or its string representation doesn't contain all expected substrings
	 * @see #assertContains(String, Object) for single substring assertions
	 */
	public static void assertContainsAll(Supplier<String> message, Object actual, String...expected) {
		assertArgNotNull("expected", expected);
		assertNotNull(actual, "Value was null.");

		var a = getConverter().stringify(actual);
		var errors = new ArrayList<AssertionFailedError>();

		for (var e : expected) {
			if (! a.contains(e)) {
				errors.add(assertEqualsFailed(true, false, composeMessage(message, "String did not contain expected substring.  ==> expected: <{0}> but was: <{1}>", e, a)));
			}
		}

		if (errors.isEmpty())
			return;

		if (errors.size() == 1)
			throw errors.get(0);

		var missingSubstrings = new ArrayList<String>();
		for (var e : expected) {
			if (! a.contains(e)) {
				missingSubstrings.add(e);
			}
		}

		throw assertEqualsFailed(missingSubstrings.stream().map(StringUtils::escapeForJava).collect(joining("\", \"", "\"", "\"")), escapeForJava(a),
			composeMessage(message, "{0} substring assertions failed:\n{1}", errors.size(), errors.stream().map(x -> x.getMessage()).collect(joining("\n"))));
	}

	/**
	 * Asserts that a collection-like object, Optional, Value, String, or array is not null and empty.
	 *
	 * <p>Same as {@link #assertEmpty(Supplier, Object)} but without a custom message.</p>
	 *
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the object is null or not empty
	 * @see #assertEmpty(Supplier, Object)
	 */
	public static void assertEmpty(Object value) {
		assertEmpty(null, value);
	}

	/**
	 * Asserts that a collection-like object, Optional, Value, String, or array is not null and empty.
	 *
	 * <p>This method validates that the provided object is empty according to its type:</p>
	 * <ul>
	 *    <li><b>String:</b> Must have length 0</li>
	 *    <li><b>Optional:</b> Must be empty (not present)</li>
	 *    <li><b>Value:</b> Must be empty (value is null)</li>
	 *    <li><b>Map:</b> Must have no entries</li>
	 *    <li><b>Collection:</b> Must have no elements</li>
	 *    <li><b>Array:</b> Must have length 0</li>
	 *    <li><b>Other objects:</b> Must be convertible to an empty List via {@link BeanConverter#listify(Object)}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Supported Types:</h5>
	 * <p>Any object that can be converted to a List, including:</p>
	 * <ul>
	 *    <li>Collections (List, Set, Queue, etc.)</li>
	 *    <li>Arrays (primitive and object arrays)</li>
	 *    <li>Iterables, Iterators, Streams</li>
	 *    <li>Maps (converted to list of entries)</li>
	 *    <li>Optional objects</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test empty collections</jc>
	 *    <jsm>assertEmpty</jsm>(Collections.<jsm>emptyList</jsm>());
	 *    <jsm>assertEmpty</jsm>(<jk>new</jk> ArrayList&lt;&gt;());
	 *
	 *    <jc>// Test empty arrays</jc>
	 *    <jsm>assertEmpty</jsm>(<jk>new</jk> String[0]);
	 *
	 *    <jc>// Test empty Optional</jc>
	 *    <jsm>assertEmpty</jsm>(Optional.<jsm>empty</jsm>());
	 *
	 *    <jc>// Test empty Map</jc>
	 *    <jsm>assertEmpty</jsm>(<jk>new</jk> HashMap&lt;&gt;());
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the object is null or not empty
	 * @see #assertNotEmpty(Object) for testing non-empty collections
	 * @see #assertSize(int, Object) for testing specific sizes
	 */
	public static void assertEmpty(Supplier<String> message, Object value) {
		assertNotNull(value, "Value was null.");
		var size = getConverter().size(value);
		assertEquals(0, size, composeMessage(message, "Value was not empty. Size=<{0}>", size));
	}

	/**
	 * Asserts that a List or List-like object contains the expected values using flexible comparison logic.
	 *
	 * <p>Same as {@link #assertList(Supplier, Object, Object...)} but without a custom message.</p>
	 *
	 * @param actual The List to test. Must not be null.
	 * @param expected Multiple arguments of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws IllegalArgumentException if actual is null
	 * @throws AssertionError if the List size or contents don't match expected values
	 * @see #assertList(Supplier, Object, Object...)
	 */
	public static void assertList(Object actual, Object...expected) {
		assertArgNotNull("actual", actual);
		assertList(null, actual, expected);
	}

	/**
	 * Asserts that a List or List-like object contains the expected values using flexible comparison logic.
	 *
	 * <h5 class='section'>Testing Non-List Collections:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test a Set using l() conversion</jc>
	 *    Set&lt;String&gt; <jv>mySet</jv> = <jk>new</jk> TreeSet&lt;&gt;(Arrays.<jsm>asList</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>));
	 *    <jsm>assertList</jsm>(<jsm>l</jsm>(<jv>mySet</jv>), <js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *
	 *    <jc>// Test an array using l() conversion</jc>
	 *    String[] <jv>myArray</jv> = {<js>"x"</js>, <js>"y"</js>, <js>"z"</js>};
	 *    <jsm>assertList</jsm>(<jsm>l</jsm>(<jv>myArray</jv>), <js>"x"</js>, <js>"y"</js>, <js>"z"</js>);
	 *
	 *    <jc>// Test a Stream using l() conversion</jc>
	 *    Stream&lt;String&gt; <jv>myStream</jv> = Stream.<jsm>of</jsm>(<js>"foo"</js>, <js>"bar"</js>);
	 *    <jsm>assertList</jsm>(<jsm>l</jsm>(<jv>myStream</jv>), <js>"foo"</js>, <js>"bar"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 *    <jc>// Elements are converted to strings using the bean converter and compared as strings</jc>
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(1, 2, 3), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>), <js>"a"</js>, <js>"b"</js>);
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 *    <jc>// Use Predicate&lt;T&gt; for functional testing</jc>
	 *    Predicate&lt;Integer&gt; <jv>greaterThanOne</jv> = <jv>x</jv> -&gt; <jv>x</jv> &gt; 1;
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(2, 3, 4), <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>);
	 *
	 *    <jc>// Mix predicates with other comparison types</jc>
	 *    Predicate&lt;String&gt; <jv>startsWithA</jv> = <jv>s</jv> -&gt; <jv>s</jv>.startsWith(<js>"a"</js>);
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(<js>"apple"</js>, <js>"banana"</js>), <jv>startsWithA</jv>, <js>"banana"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 *    <jc>// Non-String, non-Predicate objects use <jsm>Objects.equals</jsm>() comparison</jc>
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(1, 2, 3), 1, 2, 3); <jc>// Integer objects</jc>
	 *    <jsm>assertList</jsm>(List.<jsm>of</jsm>(<jv>myBean1</jv>, <jv>myBean2</jv>), <jv>myBean1</jv>, <jv>myBean2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param actual The List to test. Must not be null.
	 * @param expected Multiple arguments of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the List size or contents don't match expected values
	 */
	@SuppressWarnings("unchecked")
	public static void assertList(Supplier<String> message, Object actual, Object...expected) {
		assertArgNotNull("expected", expected);
		assertArgNotNull("actual", actual);

		var converter = getConverter();
		List<Object> list = converter.listify(actual);
		var errors = new ArrayList<AssertionFailedError>();

		if (ne(expected.length, list.size())) {
			errors.add(assertEqualsFailed(expected.length, list.size(), composeMessage(message, "Wrong list length.")));
		} else {
			for (var i = 0; i < expected.length; i++) {
				var x = list.get(i);
				var e = expected[i];
				if (e instanceof String e2) {
					if (ne(e2, converter.stringify(x))) {
						errors.add(assertEqualsFailed(e2, converter.stringify(x), composeMessage(message, "Element at index {0} did not match.", i)));
					}
				} else if (e instanceof Predicate e2) { // NOSONAR
					if (! e2.test(x)) {
						errors.add(new AssertionFailedError(composeMessage(message, "Element at index {0} did not pass predicate.  ==> actual: <{1}>", i, converter.stringify(x)).get()));
					}
				} else {
					if (ne(e, x)) {
						errors.add(assertEqualsFailed(e, x, composeMessage(message, "Element at index {0} did not match.  ==> expected: <{1}({2})> but was: <{3}({4})>", i, e, cns(e), x, cns(x))));
					}
				}
			}
		}

		if (errors.isEmpty())
			return;

		var actualStrings = new ArrayList<String>();
		for (var o : list) {
			actualStrings.add(converter.stringify(o));
		}

		if (errors.size() == 1)
			throw errors.get(0);

		throw assertEqualsFailed(Stream.of(expected).map(converter::stringify).map(StringUtils::escapeForJava).collect(joining("\", \"", "[\"", "\"]")),
			actualStrings.stream().map(StringUtils::escapeForJava).collect(joining("\", \"", "[\"", "\"]")),
			composeMessage(message, "{0} list assertions failed:\n{1}", errors.size(), errors.stream().map(x -> x.getMessage()).collect(joining("\n"))));
	}

	/**
	 * Asserts that a Map contains the expected key/value pairs using flexible comparison logic.
	 *
	 * <p>Same as {@link #assertMap(Supplier, Map, Object...)} but without a custom message.</p>
	 *
	 * @param actual The Map to test. Must not be null.
	 * @param expected Multiple arguments of expected map entries.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the Map size or contents don't match expected values
	 * @see #assertMap(Supplier, Map, Object...)
	 */
	public static void assertMap(Map<?,?> actual, Object...expected) {
		assertMap(null, actual, expected);
	}

	/**
	 * Asserts that a Map contains the expected key/value pairs using flexible comparison logic.
	 *
	 * <h5 class='section'>Map Entry Serialization:</h5>
	 * <p>Map entries are serialized to strings as key/value pairs in the format <js>"key=value"</js>.
	 * Nested maps and collections are supported with appropriate formatting.</p>
	 *
	 * <h5 class='section'>Testing Nested Maps and Collections:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test simple map entries</jc>
	 *    Map&lt;String,String&gt; <jv>simpleMap</jv> = Map.<jsm>of</jsm>(<js>"a"</js>, <js>"1"</js>, <js>"b"</js>, <js>"2"</js>);
	 *    <jsm>assertMap</jsm>(<jv>simpleMap</jv>, <js>"a=1"</js>, <js>"b=2"</js>);
	 *
	 *    <jc>// Test nested maps</jc>
	 *    Map&lt;String,Map&lt;String,Integer&gt;&gt; <jv>nestedMap</jv> = Map.<jsm>of</jsm>(<js>"a"</js>, Map.<jsm>of</jsm>(<js>"b"</js>, 1));
	 *    <jsm>assertMap</jsm>(<jv>nestedMap</jv>, <js>"a={b=1}"</js>);
	 *
	 *    <jc>// Test maps with arrays/collections</jc>
	 *    Map&lt;String,Map&lt;String,Integer[]&gt;&gt; <jv>mapWithArrays</jv> = Map.<jsm>of</jsm>(<js>"a"</js>, Map.<jsm>of</jsm>(<js>"b"</js>, <jk>new</jk> Integer[]{1,2}));
	 *    <jsm>assertMap</jsm>(<jv>mapWithArrays</jv>, <js>"a={b=[1,2]}"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports the same comparison modes as {@link #assertList(Object, Object...)}:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 *    <jc>// Map entries are converted to strings and compared as strings</jc>
	 *    <jsm>assertMap</jsm>(Map.<jsm>of</jsm>(<js>"key1"</js>, <js>"value1"</js>), <js>"key1=value1"</js>);
	 *    <jsm>assertMap</jsm>(Map.<jsm>of</jsm>(<js>"count"</js>, 42), <js>"count=42"</js>);
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 *    <jc>// Use Predicate&lt;Map.Entry&lt;K,V&gt;&gt; for functional testing</jc>
	 *    Predicate&lt;Map.Entry&lt;String,Integer&gt;&gt; <jv>valueGreaterThanTen</jv> = <jv>entry</jv> -&gt; <jv>entry</jv>.getValue() &gt; 10;
	 *    <jsm>assertMap</jsm>(Map.<jsm>of</jsm>(<js>"count"</js>, 42), <jv>valueGreaterThanTen</jv>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 *    <jc>// Non-String, non-Predicate objects use <jsm>Objects.equals</jsm>() comparison</jc>
	 *    <jsm>assertMap</jsm>(Map.<jsm>of</jsm>(<js>"key"</js>, <jv>myObject</jv>), <jv>expectedEntry</jv>);
	 * </p>
	 *
	 * <h5 class='section'>Map Ordering Behavior:</h5>
	 * <p>The {@link Listifiers#mapListifier()} method ensures deterministic ordering for map entries:</p>
	 * <ul>
	 *    <li><b>{@link SortedMap} (TreeMap, etc.):</b> Preserves existing sort order</li>
	 *    <li><b>{@link LinkedHashMap}:</b> Preserves insertion order</li>
	 *    <li><b>{@link HashMap} and other unordered Maps:</b> Converts to {@link TreeMap} for natural key ordering</li>
	 * </ul>
	 * <p>This ensures predictable test results regardless of the original map implementation.</p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param actual The Map to test. Must not be null.
	 * @param expected Multiple arguments of expected map entries.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the Map size or contents don't match expected values
	 * @see #assertList(Supplier, Object, Object...)
	 */
	public static void assertMap(Supplier<String> message, Map<?,?> actual, Object...expected) {
		assertList(message, actual, expected);
	}

	/**
	 * Asserts that mapped property access on an object returns expected values using a custom BiFunction.
	 *
	 * <p>This is designed for testing objects that don't follow
	 * standard JavaBean patterns or require custom property access logic. The BiFunction allows complete
	 * control over how properties are retrieved from the target object.</p>
	 *
	 * <p>This method creates an intermediate LinkedHashMap to collect all property values before
	 * using the same logic as assertBean for comparison. This ensures consistent ordering
	 * and supports the full nested property syntax. The {@link BasicBeanConverter#DEFAULT} is used
	 * for value stringification and nested property access.</p>
	 *
	 * @param <T> The type of object being tested
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param actual The object to test properties on
	 * @param function The BiFunction that extracts property values. Receives (<jp>object</jp>, <jp>propertyName</jp>) and returns the property value.
	 * @param properties Comma-delimited list of property names to test
	 * @param expected Comma-delimited list of expected values (exceptions become simple class names)
	 * @throws AssertionError if any mapped property values don't match expected values
	 * @see #assertBean(Supplier, Object, String, String)
	 * @see BeanConverter
	 * @see BasicBeanConverter
	 */
	public static <T> void assertMapped(Supplier<String> message, T actual, BiFunction<T,String,Object> function, String properties, String expected) {
		assertNotNull(actual, "Value was null.");
		assertArgNotNull("function", function);
		assertArgNotNull("properties", properties);
		assertArgNotNull("expected", expected);

		var m = new LinkedHashMap<String,Object>();
		for (var p : tokenize(properties)) {
			var pv = p.getValue();
			m.put(pv, safe(() -> function.apply(actual, pv)));
		}

		assertBean(message, m, properties, expected);
	}

	/**
	 * Asserts that mapped property access on an object returns expected values using a custom BiFunction.
	 *
	 * <p>Same as {@link #assertMapped(Supplier, Object, BiFunction, String, String)} but without a custom message.</p>
	 *
	 * @param <T> The type of object being tested
	 * @param actual The object to test properties on
	 * @param function The BiFunction that extracts property values. Receives (<jp>object</jp>, <jp>propertyName</jp>) and returns the property value.
	 * @param properties Comma-delimited list of property names to test
	 * @param expected Comma-delimited list of expected values (exceptions become simple class names)
	 * @throws AssertionError if any mapped property values don't match expected values
	 * @see #assertMapped(Supplier, Object, BiFunction, String, String)
	 */
	public static <T> void assertMapped(T actual, BiFunction<T,String,Object> function, String properties, String expected) {
		assertMapped(null, actual, function, properties, expected);
	}

	/**
	 * Asserts that an object's string representation matches the specified glob-style pattern.
	 *
	 * <p>Same as {@link #assertMatchesGlob(Supplier, String, Object)} but without a custom message.</p>
	 *
	 * @param pattern The glob-style pattern to match against.
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the value is null or its string representation doesn't match the pattern
	 * @see #assertMatchesGlob(Supplier, String, Object)
	 */
	public static void assertMatchesGlob(String pattern, Object value) {
		assertMatchesGlob(null, pattern, value);
	}

	/**
	 * Asserts that an object's string representation matches the specified glob-style pattern.
	 *
	 * <p>This method converts the actual object to its string representation using the current
	 * {@link BeanConverter} and then tests it against the provided glob-style pattern.
	 * This is useful for testing string formats with simple wildcard patterns.</p>
	 *
	 * <h5 class='section'>Pattern Syntax:</h5>
	 * <p>The pattern uses glob-style wildcards:</p>
	 * <ul>
	 *    <li><b>{@code *}</b> matches any sequence of characters (including none)</li>
	 *    <li><b>{@code ?}</b> matches exactly one character</li>
	 *    <li><b>All other characters</b> are treated literally</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test filename patterns</jc>
	 *    <jsm>assertMatchesGlob</jsm>(<js>"user_*_temp"</js>, <jv>filename</jv>);
	 *
	 *    <jc>// Test single character wildcards</jc>
	 *    <jsm>assertMatchesGlob</jsm>(<js>"file?.txt"</js>, <jv>fileName</jv>);
	 *
	 *    <jc>// Test combined patterns</jc>
	 *    <jsm>assertMatchesGlob</jsm>(<js>"log_*_?.txt"</js>, <jv>logFile</jv>);
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param pattern The glob-style pattern to match against.
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the value is null or its string representation doesn't match the pattern
	 * @see #assertString(Supplier, String, Object) for exact string matching
	 * @see #assertContains(Supplier, String, Object) for substring matching
	 */
	public static void assertMatchesGlob(Supplier<String> message, String pattern, Object value) {
		assertArgNotNull("pattern", pattern);
		assertNotNull(value, "Value was null.");

		var v = getConverter().stringify(value);
		var m = StringUtils.getGlobMatchPattern(pattern).matcher(v);
		assertTrue(m.matches(), composeMessage(message, "Pattern didn''t match. ==> pattern: <{0}> but was: <{1}>", pattern, v));
	}

	/**
	 * Asserts that a collection-like object, Optional, Value, String, or array is not null and not empty.
	 *
	 * <p>Same as {@link #assertNotEmpty(Supplier, Object)} but without a custom message.</p>
	 *
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the object is null or empty
	 * @see #assertNotEmpty(Supplier, Object)
	 */
	public static void assertNotEmpty(Object value) {
		assertNotEmpty(null, value);
	}

	/**
	 * Asserts that a collection-like object, Optional, Value, String, or array is not null and not empty.
	 *
	 * <p>This method validates that the provided object is not empty according to its type:</p>
	 * <ul>
	 *    <li><b>String:</b> Must have length > 0</li>
	 *    <li><b>Optional:</b> Must be present (not empty)</li>
	 *    <li><b>Value:</b> Must not be empty (value is not null)</li>
	 *    <li><b>Map:</b> Must have at least one entry</li>
	 *    <li><b>Collection:</b> Must have at least one element</li>
	 *    <li><b>Array:</b> Must have length > 0</li>
	 *    <li><b>Other objects:</b> Must convert to a non-empty List via {@link BeanConverter#listify(Object)}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Supported Types:</h5>
	 * <p>Any object that can be converted to a List, including:</p>
	 * <ul>
	 *    <li>Collections (List, Set, Queue, etc.)</li>
	 *    <li>Arrays (primitive and object arrays)</li>
	 *    <li>Iterables, Iterators, Streams</li>
	 *    <li>Maps (converted to list of entries)</li>
	 *    <li>Optional objects</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test non-empty collections</jc>
	 *    <jsm>assertNotEmpty</jsm>(List.<jsm>of</jsm>(<js>"item1"</js>, <js>"item2"</js>));
	 *    <jsm>assertNotEmpty</jsm>(<jk>new</jk> ArrayList&lt;&gt;(Arrays.<jsm>asList</jsm>(<js>"a"</js>)));
	 *
	 *    <jc>// Test non-empty arrays</jc>
	 *    <jsm>assertNotEmpty</jsm>(<jk>new</jk> String[]{<js>"value"</js>});
	 *
	 *    <jc>// Test present Optional</jc>
	 *    <jsm>assertNotEmpty</jsm>(Optional.<jsm>of</jsm>(<js>"value"</js>));
	 *
	 *    <jc>// Test non-empty Map</jc>
	 *    <jsm>assertNotEmpty</jsm>(Map.<jsm>of</jsm>(<js>"key"</js>, <js>"value"</js>));
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param value The object to test. Must not be null.
	 * @throws AssertionError if the object is null or empty
	 * @see #assertEmpty(Supplier, Object) for testing empty collections
	 * @see #assertSize(Supplier, int, Object) for testing specific sizes
	 */
	public static void assertNotEmpty(Supplier<String> message, Object value) {
		assertNotNull(value, "Value was null.");
		int size = getConverter().size(value);
		assertTrue(size > 0, composeMessage(message, "Value was empty."));
	}

	/**
	 * Asserts that a collection-like object or string is not null and of the specified size.
	 *
	 * <p>Same as {@link #assertSize(Supplier, int, Object)} but without a custom message.</p>
	 *
	 * @param expected The expected size/length.
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the object is null or not the expected size.
	 * @see #assertSize(Supplier, int, Object)
	 */
	public static void assertSize(int expected, Object actual) {
		assertSize(null, expected, actual);
	}

	/**
	 * Asserts that a collection-like object or string is not null and of the specified size.
	 *
	 * <p>This method can validate the size of various types of objects:</p>
	 * <ul>
	 *    <li><b>String:</b> Validates character length</li>
	 *    <li><b>Collection-like objects:</b> Any object that can be converted to a List via the underlying converter</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test string length</jc>
	 *    <jsm>assertSize</jsm>(5, <js>"hello"</js>);
	 *
	 *    <jc>// Test collection size</jc>
	 *    <jsm>assertSize</jsm>(3, List.<jsm>of</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>));
	 *
	 *    <jc>// Test array size</jc>
	 *    <jsm>assertSize</jsm>(2, <jk>new</jk> String[]{<js>"x"</js>, <js>"y"</js>});
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param expected The expected size/length.
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the object is null or not the expected size.
	 */
	public static void assertSize(Supplier<String> message, int expected, Object actual) {
		assertNotNull(actual, "Value was null.");
		var size = getConverter().size(actual);
		assertEquals(expected, size, composeMessage(message, "Value not expected size."));
	}

	/**
	 * Asserts that an object's string representation exactly matches the expected value.
	 *
	 * <p>Same as {@link #assertString(Supplier, String, Object)} but without a custom message.</p>
	 *
	 * @param expected The exact string that the actual object should convert to
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the actual object is null or its string representation doesn't exactly match expected
	 * @see #assertString(Supplier, String, Object)
	 */
	public static void assertString(String expected, Object actual) {
		assertString(null, expected, actual);
	}

	/**
	 * Asserts that an object's string representation exactly matches the expected value.
	 *
	 * <p>This method converts the actual object to its string representation using the current
	 * {@link BeanConverter} and performs an exact equality comparison with the expected string.
	 * This is useful for testing complete string output, formatted objects, or converted values.</p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 *    <jc>// Test exact string conversion</jc>
	 *    <jsm>assertString</jsm>(<js>"John,30,true"</js>, <jv>user</jv>); <jc>// Assuming user converts to this format</jc>
	 *
	 *    <jc>// Test formatted dates or numbers</jc>
	 *    <jsm>assertString</jsm>(<js>"2023-12-01"</js>, <jv>localDate</jv>);
	 *
	 *    <jc>// Test complex object serialization</jc>
	 *    <jsm>assertString</jsm>(<js>"{name=John,age=30}"</js>, <jv>userMap</jv>);
	 *
	 *    <jc>// Test array/collection formatting</jc>
	 *    <jsm>assertString</jsm>(<js>"[red,green,blue]"</js>, <jv>colors</jv>);
	 * </p>
	 *
	 * @param message Optional custom error message supplier. If provided, will be composed with the default assertion message.
	 * @param expected The exact string that the actual object should convert to
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the actual object is null or its string representation doesn't exactly match expected
	 * @see #assertContains(Supplier, String, Object) for partial string matching
	 * @see #assertMatchesGlob(Supplier, String, Object) for pattern-based matching
	 */
	public static void assertString(Supplier<String> message, String expected, Object actual) {
		assertNotNull(actual, "Value was null.");

		var messageSupplier = message != null ? message : fs("");
		assertEquals(expected, getConverter().stringify(actual), messageSupplier);
	}

	/**
	 * Resets the bean converter for the current thread to the system default.
	 *
	 * <p>This method clears any thread-local converter override set via {@link #setConverter(BeanConverter)},
	 * restoring the default converter ({@link BasicBeanConverter#DEFAULT}) for subsequent assertions.</p>
	 *
	 * <p>This is typically called in test teardown methods (e.g., {@code @AfterEach}) to clean up after tests
	 * that set a custom converter.</p>
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 *    <jc>// In @AfterEach method</jc>
	 *    BctAssertions.<jsm>resetConverter</jsm>();
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>This method is thread-safe and only affects the current thread's converter.</p>
	 *
	 * @see #setConverter(BeanConverter)
	 */
	public static void resetConverter() {
		CONVERTER_OVERRIDE.remove();
		CONVERTER_SUPPLIER.get().reset();
	}

	/**
	 * Sets a custom bean converter for the current thread.
	 *
	 * <p>This method allows you to override the default converter for all assertions in the current test method.
	 * The converter will be used by all assertion methods in the current thread.</p>
	 *
	 * <p>This is particularly useful in test setup methods (e.g., {@code @BeforeEach}) to configure a custom converter
	 * for all tests in a test class or method.</p>
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 *    <jc>// In @BeforeEach method</jc>
	 *    <jk>var</jk> <jv>customConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 *       .defaultSettings()
	 *       .addStringifier(LocalDate.<jk>class</jk>, <jp>date</jp> -> <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>))
	 *       .build();
	 *    BctAssertions.<jsm>setConverter</jsm>(<jv>customConverter</jv>);
	 *
	 *    <jc>// All subsequent assertions in this test method will use the custom converter</jc>
	 *    <jsm>assertBean</jsm>(<jv>event</jv>, <js>"date"</js>, <js>"2023-12-01"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>This method is thread-safe and uses thread-local storage. Each test method running in parallel
	 * will have its own converter instance, preventing cross-thread interference.</p>
	 *
	 * @param converter The bean converter to use for the current thread. Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If converter is <jk>null</jk>.
	 * @see #resetConverter()
	 */
	public static void setConverter(BeanConverter converter) {
		assertArgNotNull("converter", converter);
		CONVERTER_OVERRIDE.set(converter);
	}

	/**
	 * Composes an error message from an optional custom message and a default message.
	 *
	 * <p>If a custom message is provided, it is composed with the default message in the format:
	 * <js>"{custom}, Caused by: {default}"</js>. Otherwise, the default message is returned.</p>
	 *
	 * @param customMessage Optional custom message supplier. Can be <jk>null</jk>.
	 * @param defaultMessage Default message template.
	 * @param defaultArgs Arguments for the default message template.
	 * @return A supplier that produces the composed error message.
	 */
	private static Supplier<String> composeMessage(Supplier<String> customMessage, String defaultMessage, Object...defaultArgs) {
		if (customMessage == null) {
			return fs(defaultMessage, defaultArgs);
		}
		return fs("{0}, Caused by: {1}", customMessage.get(), f(defaultMessage, defaultArgs));
	}

	/**
	 * Gets the bean converter for the current thread.
	 *
	 * <p>Returns the thread-local converter override if set, otherwise returns the memoized default converter.
	 * This method is used internally by all assertion methods to get the current thread-local converter.</p>
	 *
	 * @return The bean converter to use for the current thread.
	 */
	private static BeanConverter getConverter() {
		var override = CONVERTER_OVERRIDE.get();
		if (override != null) {
			return override;
		}
		return CONVERTER_SUPPLIER.get().get();
	}

	private BctAssertions() {}
}