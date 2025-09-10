package org.apache.juneau.junit;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.junit.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.opentest4j.*;

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
public class Assertions2 {

	private static final BeanConverter DEFAULT_CONVERTER = BasicBeanConverter.DEFAULT;

	public static AssertionArgs args() {
		return new AssertionArgs();
	}

	/**
	 * Asserts that the fields/properties on the specified bean are the specified values after being converted to {@link Utils#r readable} strings.
	 *
	 * <p>This is the primary method for Bean-Centric Tests (BCT), supporting extensive property validation
	 * patterns including nested objects, collections, arrays, method chaining, direct field access, collection iteration
	 * with <js>#{property}</js> syntax, and universal <js>length</js>/<js>size</js> properties for all collection types.</p>
	 *
	 * <p>The method uses the {@link BasicBeanConverter#DEFAULT} converter internally for object introspection
	 * and value extraction. The converter provides sophisticated property access through the {@link BeanConverter}
	 * interface, supporting multiple fallback mechanisms for accessing object properties and values.</p>
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
	  * <h5 class='section'>Array, List, and Stream Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test array/list elements by index</jc>
	 * 	assertBean(myBean, <js>"items{0,1,2}"</js>, <js>"{item1,item2,item3}"</js>);
	 *
	 * 	<jc>// Test nested properties within array elements</jc>
	 * 	assertBean(myBean, <js>"orders{0{id,total}}"</js>, <js>"{{123,99.95}}"</js>);
	 *
	 * 	<jc>// Test array length property</jc>
	 * 	assertBean(myBean, <js>"items{length}"</js>, <js>"{5}"</js>);
	 *
	 * 	<jc>// Works with any iterable type including Streams</jc>
	 * 	assertBean(myBean, <js>"userStream{#{name}}"</js>, <js>"[{Alice},{Bob}]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Collection Iteration Syntax:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test properties across ALL elements in a collection using #{...} syntax</jc>
	 * 	assertBean(myBean, <js>"userList{#{name}}"</js>, <js>"[{John},{Jane},{Bob}]"</js>);
	 *
	 * 	<jc>// Test multiple properties from each element</jc>
	 * 	assertBean(myBean, <js>"orderList{#{id,status}}"</js>, <js>"[{123,ACTIVE},{124,PENDING}]"</js>);
	 *
	 * 	<jc>// Works with nested properties within each element</jc>
	 * 	assertBean(myBean, <js>"customers{#{address{city}}}"</js>, <js>"[{{New York}},{{Los Angeles}}]"</js>);
	 *
	  * 	<jc>// Works with arrays and any iterable collection type (including Streams)</jc>
	 * 	assertBean(config, <js>"itemArray{#{type}}"</js>, <js>"[{String},{Integer},{Boolean}]"</js>);
	 * 	assertBean(data, <js>"statusSet{#{name}}"</js>, <js>"[{ACTIVE},{PENDING},{CANCELLED}]"</js>);
	 * 	assertBean(processor, <js>"dataStream{#{value}}"</js>, <js>"[{A},{B},{C}]"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Universal Collection Size Properties:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Both 'length' and 'size' work universally across all collection types</jc>
	 * 	assertBean(myBean, <js>"myArray{length}"</js>, <js>"{5}"</js>);        <jc>// Arrays</jc>
	 * 	assertBean(myBean, <js>"myArray{size}"</js>, <js>"{5}"</js>);          <jc>// Also works for arrays</jc>
	 *
	 * 	assertBean(myBean, <js>"myList{size}"</js>, <js>"{3}"</js>);           <jc>// Collections</jc>
	 * 	assertBean(myBean, <js>"myList{length}"</js>, <js>"{3}"</js>);         <jc>// Also works for collections</jc>
	 *
	 * 	assertBean(myBean, <js>"myMap{size}"</js>, <js>"{7}"</js>);            <jc>// Maps</jc>
	 * 	assertBean(myBean, <js>"myMap{length}"</js>, <js>"{7}"</js>);          <jc>// Also works for maps</jc>
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
	 * <h5 class='section'>Advanced Collection Analysis:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Combine size/length, metadata, and content iteration in single assertions</jc>
	 * 	assertBean(myBean, <js>"users{length,class{simpleName},#{name}}"</js>,
	 * 		<js>"{3,{ArrayList},[{John},{Jane},{Bob}]}"</js>);
	 *
	 * 	<jc>// Comprehensive collection validation with multiple iteration patterns</jc>
	 * 	assertBean(order, <js>"items{size,#{name},#{price}}"</js>,
	 * 		<js>"{3,[{Laptop},{Phone},{Tablet}],[{999.99},{599.99},{399.99}]}"</js>);
	 *
	 * 	<jc>// Perfect for validation testing - verify error count and details</jc>
	 * 	assertBean(result, <js>"errors{length,#{field},#{code}}"</js>,
	 * 		<js>"{2,[{email},{password}],[{E001},{E002}]}"</js>);
	 *
	 * 	<jc>// Mixed collection types with consistent syntax</jc>
	 * 	assertBean(response, <js>"results{size},metadata{length}"</js>, <js>"{25},{4}"</js>);
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
	  * <h5 class='section'>Map Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test map values by key</jc>
	 * 	assertBean(myBean, <js>"configMap{timeout,retries}"</js>, <js>"{30000,3}"</js>);
	 *
	 * 	<jc>// Test map size</jc>
	 * 	assertBean(myBean, <js>"settings{size}"</js>, <js>"{5}"</js>);
	 *
	 * 	<jc>// Test null keys using special &lt;NULL&gt; syntax</jc>
	 * 	assertBean(myBean, <js>"mapWithNullKey{&lt;NULL&gt;}"</js>, <js>"{nullKeyValue}"</js>);
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
	 * 	<li><b>Collection iteration:</b> <js>"#{property}"</js> iterates over ALL collection elements, returns <js>"[{val1},{val2}]"</js></li>
	 * 	<li><b>Universal size properties:</b> <js>"length"</js> and <js>"size"</js> work on arrays, collections, and maps</li>
	 * 	<li><b>Boolean values:</b> <js>"true"</js>, <js>"false"</js></li>
	 * 	<li><b>Null values:</b> <js>"null"</js></li>
	 * </ul>
	 *
	  * <h5 class='section'>Property Access Priority:</h5>
	 * <ol>
	 * 	<li><b>Collection/Array access:</b> Numeric indices for arrays/lists (e.g., <js>"0"</js>, <js>"1"</js>)</li>
	 * 	<li><b>Universal size properties:</b> <js>"length"</js> and <js>"size"</js> for arrays, collections, and maps</li>
	 * 	<li><b>Map key access:</b> Direct key lookup for Map objects (including <js>"&lt;NULL&gt;"</js> for null keys)</li>
	 * 	<li><b>is{Property}()</b> methods (for boolean properties)</li>
	 * 	<li><b>get{Property}()</b> methods</li>
	 * 	<li><b>Public fields</b> (direct field access)</li>
	 * </ol>
	 *
	 * @param actual The bean object to test. Must not be null.
	 * @param fields Comma-delimited list of property names to test. Supports nested syntax with {}.
	 * @param expected Comma-delimited list of expected values. Must match the order of fields.
	 * @throws NullPointerException if the bean is null
	 * @throws AssertionError if any property values don't match expected values
	 * @see BeanConverter
	 * @see BasicBeanConverter
	 */
	public static void assertBean(Object actual, String fields, String expected) {
		assertBean(args(), actual, fields, expected);
	}

	public static void assertBean(AssertionArgs args, Object actual, String fields, String expected) {
		assertNotNull(actual, "Actual was null.");
		assertArgNotNull("args", args);
		assertArgNotNull("fields", fields);
		assertArgNotNull("expected", expected);
		assertEquals(
			expected,
			tokenize(fields).stream().map(x -> args.getBeanConverter().orElse(DEFAULT_CONVERTER).getNested(actual, x)).collect(joining(",")),
			args.getMessage("Bean assertion failed.")
		);
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
	  * <h5 class='section'>Collection Iteration Testing:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test collection iteration within beans (#{...} syntax)</jc>
	 * 	assertBeans(departmentList, <js>"name,employees{#{name}}"</js>,
	 * 		<js>"Engineering,[{Alice},{Bob},{Charlie}]"</js>,
	 * 		<js>"Marketing,[{David},{Eve}]"</js>);
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
	public static void assertBeans(Object actual, String fields, String...expected) {
		assertBeans(args(), actual, fields, expected);
	}

	public static void assertBeans(AssertionArgs args, Object actual, String fields, String...expected) {
		assertNotNull(actual, "Value was null.");
		assertArgNotNull("args", args);
		assertArgNotNull("fields", fields);
		assertArgNotNull("expected", expected);

		var converter = args.getBeanConverter().orElse(DEFAULT_CONVERTER);
		var tokens = tokenize(fields);
		var errors = new ArrayList<AssertionFailedError>();
		var actualList = converter.listify(actual);

		if (ne(expected.length, actualList.size())) {
			errors.add(assertEqualsFailed(expected.length, actualList.size(), args.getMessage("Wrong number of beans.")));
		} else {
			for (var i = 0; i < actualList.size(); i++) {
				var i2 = i;
				var e = converter.stringify(expected[i]);
				var a = tokens.stream().map(x -> converter.getNested(actualList.get(i2), x)).collect(joining(","));
				if (ne(e, a)) {
					errors.add(assertEqualsFailed(e, a, args.getMessage("Bean at row <{0}> did not match.", i)));
				}
			}
		}

		if (errors.isEmpty()) return;

		var actualStrings = new ArrayList<String>();
		for (var o : actualList) {
			actualStrings.add(tokens.stream().map(x -> converter.getNested(o, x)).collect(joining(",")));
		}

		if (errors.size() == 1) throw errors.get(0);

		throw assertEqualsFailed(
			Stream.of(expected).map(Utils::escapeForJava).collect(joining("\", \"", "\"", "\"")),
			actualStrings.stream().map(Utils::escapeForJava).collect(joining("\", \"", "\"", "\"")),
			args.getMessage("{0} bean assertions failed:\n{1}", errors.size(), errors.stream().map(x -> x.getMessage()).collect(joining("\n")))
		);
	}

	/**
	 * Asserts that mapped property access on an object returns expected values using a custom BiFunction.
	 *
	 * <p>This is the most powerful and flexible BCT method, designed for testing objects that don't follow
	 * standard JavaBean patterns or require custom property access logic. The BiFunction allows complete
	 * control over how properties are retrieved from the target object.</p>
	 *
	 * <p>When the BiFunction throws an exception, it's automatically caught and the exception's
	 * simple class name becomes the property value for comparison (e.g., "NullPointerException").</p>
	 *
	 * <p>This method creates an intermediate LinkedHashMap to collect all property values before
	 * delegating to assertMap(Map, String, String). This ensures consistent ordering
	 * and supports the full nested property syntax. The {@link BasicBeanConverter#DEFAULT} is used
	 * for value stringification and nested property access.</p>
	 *
	 * @param <T> The type of object being tested
	 * @param actual The object to test properties on
	 * @param function The BiFunction that extracts property values. Receives (object, propertyName) and returns the property value.
	 * @param properties Comma-delimited list of property names to test
	 * @param expected Comma-delimited list of expected values (exceptions become simple class names)
	 * @throws AssertionError if any mapped property values don't match expected values
	 * @see #assertBean(Object, String, String)
	 * @see #assertMap(Map, String, String)
	 * @see BeanConverter
	 * @see BasicBeanConverter
	 */
	public static <T> void assertMapped(T actual, BiFunction<T,String,Object> function, String properties, String expected) {
		assertMapped(args(), actual, function, properties, expected);
	}

	public static <T> void assertMapped(AssertionArgs args, T actual, BiFunction<T,String,Object> function, String properties, String expected) {
		assertNotNull(actual, "Value was null.");
		assertArgNotNull("args", args);
		assertArgNotNull("function", function);
		assertArgNotNull("properties", properties);
		assertArgNotNull("expected", expected);

		var m = new LinkedHashMap<String,Object>();
		for (var p : tokenize(properties)) {
			var pv = p.getValue();
			m.put(pv, safe(() -> function.apply(actual, pv)));
		}

		assertBean(args, m, properties, expected);
	}

	/**
	 * Asserts an object matches the expected string after it's been made {@link Utils#r readable}.
	 */
	public static void assertContains(String expected, Object actual) {
		assertContains(args(), expected, actual);
	}

	public static void assertContains(AssertionArgs args, String expected, Object actual) {
		assertArgNotNull("args", args);
		assertArgNotNull("expected", expected);
		assertArgNotNull("actual", actual);
		assertNotNull(actual, "Value was null.");

		var a = args.getBeanConverter().orElse(DEFAULT_CONVERTER).stringify(actual);
		assertTrue(a.contains(expected), args.getMessage("String did not contain expected substring.  ==> expected: <{0}> but was: <{1}>", expected, a));
	}

	/**
	 * Similar to {@link #assertContains(String, Object)} but allows the expected to be a comma-delimited list of strings that
	 * all must match.
	 * @param expected
	 * @param actual
	 */
	public static void assertContainsAll(Object actual, String...expected) {
		assertContainsAll(args(), actual, expected);
	}

	public static void assertContainsAll(AssertionArgs args, Object actual, String...expected) {
		assertArgNotNull("args", args);
		assertArgNotNull("expected", expected);
		assertNotNull(actual, "Value was null.");

		var a = args.getBeanConverter().orElse(DEFAULT_CONVERTER).stringify(actual);
		for (var e : expected)
			assertTrue(a.contains(e), args.getMessage("String did not contain expected substring.  ==> expected: <{0}> but was: <{1}>", e, a));
	}

	/**
	 * Asserts that a collection is not null and empty.
	 */
	public static void assertEmpty(Object value) {
		assertEmpty(args(), value);
	}

	public static void assertEmpty(AssertionArgs args, Object value) {
		assertArgNotNull("args", args);
		assertNotNull(value, "Value was null.");

		if (value instanceof Optional v2) {
			assertTrue(v2.isEmpty(), "Optional was not empty");
			return;
		}
		if (value instanceof Map v2) {
			assertTrue(v2.isEmpty(), "Map was not empty");
			return;
		}

		var converter = args.getBeanConverter().orElse(DEFAULT_CONVERTER);

		assertTrue(converter.canListify(value), args.getMessage("Value cannot be converted to a list.  Class=<{0}>", value.getClass().getSimpleName()));
		assertTrue(converter.listify(value).isEmpty(), args.getMessage("Value was not empty."));
	}

	/**
	 * Asserts that a List contains the expected values using flexible comparison logic.
	 *
	 * <p>This is the primary method for testing all collection-like types. For non-List collections, use
	 * {@link #l(Object)} to convert them to Lists first. This unified approach eliminates the need for
	 * separate assertion methods for arrays, sets, and other collection types.</p>
	 *
	 * <h5 class='section'>Testing Non-List Collections:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test a Set using l() conversion</jc>
	 * 	Set&lt;String&gt; <jv>mySet</jv> = Set.of(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 * 	assertList(l(<jv>mySet</jv>), <js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *
	 * 	<jc>// Test an array using l() conversion</jc>
	 * 	String[] <jv>myArray</jv> = {<js>"x"</js>, <js>"y"</js>, <js>"z"</js>};
	 * 	assertList(l(<jv>myArray</jv>), <js>"x"</js>, <js>"y"</js>, <js>"z"</js>);
	 *
	 * 	<jc>// Test a Stream using l() conversion</jc>
	 * 	Stream&lt;String&gt; <jv>myStream</jv> = Stream.of(<js>"foo"</js>, <js>"bar"</js>);
	 * 	assertList(l(<jv>myStream</jv>), <js>"foo"</js>, <js>"bar"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Comparison Modes:</h5>
	 * <p>The method supports three different ways to compare expected vs actual values:</p>
	 *
	 * <h6 class='section'>1. String Comparison (Readable Format):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Elements are converted to {@link Utils#r readable} format and compared as strings</jc>
	 * 	assertList(List.of(1, 2, 3), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 * 	assertList(List.of("a", "b"), <js>"a"</js>, <js>"b"</js>);
	 * </p>
	 *
	 * <h6 class='section'>2. Predicate Testing (Functional Validation):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Use Predicate&lt;T&gt; for functional testing</jc>
	 * 	Predicate&lt;Integer&gt; <jv>greaterThanOne</jv> = <jv>x</jv> -&gt; <jv>x</jv> &gt; 1;
	 * 	assertList(List.of(2, 3, 4), <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>, <jv>greaterThanOne</jv>);
	 *
	 * 	<jc>// Mix predicates with other comparison types</jc>
	 * 	Predicate&lt;String&gt; <jv>startsWithA</jv> = <jv>s</jv> -&gt; <jv>s</jv>.startsWith(<js>"a"</js>);
	 * 	assertList(List.of(<js>"apple"</js>, <js>"banana"</js>), <jv>startsWithA</jv>, <js>"banana"</js>);
	 * </p>
	 *
	 * <h6 class='section'>3. Object Equality (Direct Comparison):</h6>
	 * <p class='bjava'>
	 * 	<jc>// Non-String, non-Predicate objects use Objects.equals() comparison</jc>
	 * 	assertList(List.of(1, 2, 3), 1, 2, 3); <jc>// Integer objects</jc>
	 * 	assertList(List.of(<jv>myBean1</jv>, <jv>myBean2</jv>), <jv>myBean1</jv>, <jv>myBean2</jv>); <jc>// Custom objects</jc>
	 * </p>
	 *
	 * @param actual The List to test. Must not be null. For other collection types, use {@link #l(Object)} to convert first.
	 * @param expected Multiple arguments of expected values.
	 *                 Can be Strings (readable format comparison), Predicates (functional testing), or Objects (direct equality).
	 * @throws AssertionError if the List size or contents don't match expected values
	 * @see #l(Object) for converting other collection types to Lists
	 */
	public static <T> void assertList(Object actual, Object...expected) {
		assertList(args(), actual, expected);
	}

	@SuppressWarnings("unchecked")
	public static <T> void assertList(AssertionArgs args, Object actual, Object...expected) {
		assertArgNotNull("args", args);
		assertArgNotNull("expected", expected);
		assertNotNull(actual, "Value was null.");

		var converter = args.getBeanConverter().orElse(DEFAULT_CONVERTER);
		var list = converter.listify(actual);
		assertEquals(expected.length, list.size(), args.getMessage("Wrong list length."));

		for (var i = 0; i < expected.length; i++) {
			var x = list.get(i);
			var e = expected[i];
			if (e instanceof String e2) {
				assertEquals(e2, converter.stringify(x), args.getMessage("Element at index {0} did not match.", i));
			} else if (e instanceof Predicate e2) {
				assertTrue(e2.test(x), args.getMessage("Element at index {0} did pass predicate.  ==> actual: <{0}>", i, converter.stringify(x)));
			} else {
				assertEquals(e, x, args.getMessage("Element at index {0} did not match.  ==> expected: <{1}({2})> but was: <{3}(4)>", i, e, t(e), x, t(x)));
			}
		}
	}

	/**
	 * Asserts that a collection is not null and not empty.
	 */
	public static void assertNotEmpty(Object value) {
		assertNotEmpty(args(), value);
	}

	public static void assertNotEmpty(AssertionArgs args, Object value) {
		assertArgNotNull("args", args);
		assertNotNull(value, "Value was null.");

		if (value instanceof Optional v2) {
			assertFalse(v2.isEmpty(), "Optional was empty");
			return;
		}
		if (value instanceof Map v2) {
			assertFalse(v2.isEmpty(), "Map was empty");
			return;
		}

		assertFalse(args.getBeanConverter().orElse(DEFAULT_CONVERTER).listify(value).isEmpty(), args.getMessage("Value was empty."));
	}

	/**
	 * Asserts that a collection-like object or string is not null and of the specified size.
	 *
	 * <p>This method can validate the size of various types of objects:</p>
	 * <ul>
	 * 	<li><b>String:</b> Validates character length</li>
	 * 	<li><b>Collection-like objects:</b> Any object that can be converted to a List via {@link #toList(Object)}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test string length</jc>
	 * 	assertSize(5, <js>"hello"</js>);
	 *
	 * 	<jc>// Test collection size</jc>
	 * 	assertSize(3, List.of(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>));
	 *
	 * 	<jc>// Test array size</jc>
	 * 	assertSize(2, <jk>new</jk> String[]{<js>"x"</js>, <js>"y"</js>});
	 * </p>
	 *
	 * @param expected The expected size/length.
	 * @param actual The object to test. Must not be null.
	 * @throws AssertionError if the object is null or not the expected size.
	 */
	public static void assertSize(int expected, Object actual) {
		assertSize(args(), expected, actual);
	}

	public static void assertSize(AssertionArgs args, int expected, Object actual) {
		assertArgNotNull("args", args);
		assertNotNull(actual, "Value was null.");

		if (actual instanceof String a) {
			assertEquals(expected, a.length(), args.getMessage("Value not expected size.  value: <{0}>", a));
			return;
		}

		var size = args.getBeanConverter().orElse(DEFAULT_CONVERTER).listify(actual).size();
		assertEquals(expected, size, args.getMessage("Value not expected size."));
	}

	/**
	 * Asserts an object matches the expected string after it's been made {@link Utils#r readable}.
	 */
	public static void assertString(String expected, Object actual) {
		assertString(args(), expected, actual);
	}

	public static void assertString(AssertionArgs args, String expected, Object actual) {
		assertArgNotNull("args", args);
		assertNotNull(actual, "Value was null.");

		assertEquals(expected, args.getBeanConverter().orElse(DEFAULT_CONVERTER).stringify(actual), args.getMessage());
	}

	/**
	 * Asserts value when stringified matches the specified pattern.
	 */
	public static void assertMatches(String pattern, Object value) {
		assertMatches(args(), pattern, value);
	}

	public static void assertMatches(AssertionArgs args, String pattern, Object value) {
		assertArgNotNull("args", args);
		assertArgNotNull("pattern", pattern);
		assertNotNull(value, "Value was null.");

		var v = args.getBeanConverter().orElse(DEFAULT_CONVERTER).stringify(value);
		var m = getMatchPattern3(pattern).matcher(v);
		assertTrue(m.matches(), args.getMessage("Pattern didn't match. ==> pattern: <{0}> but was: <{1}>", pattern, v));
	}
}
