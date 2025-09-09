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
package org.apache.juneau.junit;

import java.util.*;

import org.apache.juneau.*;

/**
 * Abstract interface for Bean-Centric Test (BCT) object conversion and property access.
 *
 * <p>This interface defines the core contract for converting objects to strings and lists,
 * and for accessing object properties in a uniform way. It forms the foundation of the BCT
 * testing framework, enabling consistent object introspection and value extraction across
 * different object types and structures.</p>
 *
 * <p>The BeanConverter is designed to handle a wide variety of Java objects including:</p>
 * <ul>
 * 	<li><b>Primitives and wrappers</b> - Numbers, booleans, characters</li>
 * 	<li><b>Collections and arrays</b> - Lists, sets, arrays, iterables, streams</li>
 * 	<li><b>Maps and map-like objects</b> - HashMap, LinkedHashMap, JsonMap, BeanMap</li>
 * 	<li><b>Custom beans and POJOs</b> - JavaBean-style objects with getters/setters</li>
 * 	<li><b>Special objects</b> - Optional, Supplier, Date/Calendar, File, streams</li>
 * </ul>
 *
 * <h5 class='section'>Core Conversion Operations:</h5>
 * <dl>
 * 	<dt><b>{@link #stringify(Object)}</b></dt>
 * 	<dd>Converts any object to its string representation, handling nested structures</dd>
 *
 * 	<dt><b>{@link #listify(Object)}</b></dt>
 * 	<dd>Converts collection-like objects (arrays, Collections, Iterables, etc.) to List&lt;Object&gt;</dd>
 *
 * 	<dt><b>{@link #swap(Object)}</b></dt>
 * 	<dd>Pre-processes objects before conversion (e.g., unwrapping Optional, calling Supplier)</dd>
 *
 * 	<dt><b>{@link #getProperty(Object, String)}</b></dt>
 * 	<dd>Accesses object properties using multiple fallback mechanisms</dd>
 * </dl>
 *
 * <h5 class='section'>Property Access Strategy:</h5>
 * <p>The {@link #getProperty(Object, String)} method uses a comprehensive fallback approach:</p>
 * <ol>
 * 	<li><b>Collection/Array access:</b> Numeric indices for arrays/lists</li>
 * 	<li><b>Universal size properties:</b> "length" and "size" for arrays, collections, and maps</li>
 * 	<li><b>Map key access:</b> Direct key lookup for Map objects</li>
 * 	<li><b>is{Property}() methods:</b> Boolean property getters</li>
 * 	<li><b>get{Property}() methods:</b> Standard JavaBean getters</li>
 * 	<li><b>get(String) methods:</b> Map-style property access</li>
 * 	<li><b>Public fields:</b> Direct field access via reflection</li>
 * 	<li><b>No-arg methods:</b> Method calls with property names</li>
 * </ol>
 *
 * <h5 class='section'>Extensibility:</h5>
 * <p>The interface is designed to be implemented by custom converters that can:</p>
 * <ul>
 * 	<li>Add custom stringification rules for specific object types</li>
 * 	<li>Define custom listification behavior for collection-like objects</li>
 * 	<li>Implement object swapping/transformation logic</li>
 * 	<li>Override property access mechanisms</li>
 * 	<li>Configure formatting and display options</li>
 * </ul>
 *
 * <h5 class='section'>Primary Implementation:</h5>
 * <p>The main implementation is {@link BasicBeanConverter}, which provides:</p>
 * <ul>
 * 	<li>Extensible type-specific conversion rules</li>
 * 	<li>Configurable settings for formatting and behavior</li>
 * 	<li>Performance optimization through caching</li>
 * 	<li>Comprehensive default handling for common Java types</li>
 * </ul>
 *
 * <h5 class='section'>Usage in BCT Framework:</h5>
 * <p>This interface is used internally by BCT assertion methods like:</p>
 * <ul>
 * 	<li>{@link TestUtils#assertBean(Object, String, String)}</li>
 * 	<li>{@link TestUtils#assertMap(Map, String, String)}</li>
 * 	<li>{@link TestUtils#assertMapped(Object, java.util.function.BiFunction, String, String)}</li>
 * 	<li>{@link TestUtils#assertList(List, Object...)}</li>
 * 	<li>{@link TestUtils#assertBeans(Collection, String, String...)}</li>
 * </ul>
 *
 * @see BasicBeanConverter
 * @see TestUtils
 */
public interface BeanConverter {

	/**
	 * Converts an object to its string representation.
	 *
	 * <p>This method applies swapping logic first via {@link #swap(Object)}, then determines
	 * the appropriate string conversion based on the object's type. The conversion handles:</p>
	 * <ul>
	 * 	<li><b>Null values:</b> Returns {@link #nullValue()}</li>
	 * 	<li><b>Primitives and wrappers:</b> Direct toString() conversion</li>
	 * 	<li><b>Arrays:</b> Converts to list format with element stringification</li>
	 * 	<li><b>Collections:</b> Converts to bracket-delimited format</li>
	 * 	<li><b>Maps:</b> Converts to brace-delimited key-value format</li>
	 * 	<li><b>Custom objects:</b> Uses registered stringifiers or fallback toString()</li>
	 * </ul>
	 *
	 * @param o The object to stringify
	 * @return The string representation of the object
	 */
	String stringify(Object o);

	/**
	 * Converts a collection-like object to a List&lt;Object&gt;.
	 *
	 * <p>This method applies swapping logic first via {@link #swap(Object)}, then converts
	 * collection-like objects to lists. Supported types include:</p>
	 * <ul>
	 * 	<li><b>Arrays:</b> All array types (primitive and object)</li>
	 * 	<li><b>Collection:</b> List, Set, Queue, and all Collection subtypes</li>
	 * 	<li><b>Iterable:</b> Any object implementing Iterable</li>
	 * 	<li><b>Iterator:</b> Consumes the iterator to create a list</li>
	 * 	<li><b>Enumeration:</b> Legacy enumeration objects</li>
	 * 	<li><b>Stream:</b> Consumes the stream to create a list</li>
	 * 	<li><b>Optional:</b> Returns empty list or single-element list</li>
	 * 	<li><b>Map:</b> Returns list of Map.Entry objects</li>
	 * </ul>
	 *
	 * @param o The object to convert to a list
	 * @return A List containing the elements, or null if the object is null
	 */
	List<Object> listify(Object o);

	/**
	 * Determines if an object can be converted to a list.
	 *
	 * <p>This method checks if the object (after swapping) is a type that can be
	 * meaningfully converted to a List&lt;Object&gt; via {@link #listify(Object)}.</p>
	 *
	 * @param o The object to test
	 * @return True if the object can be listified, false otherwise
	 */
	boolean canListify(Object o);

	/**
	 * Returns the string representation used for null values.
	 *
	 * <p>This is used by {@link #stringify(Object)} when the object (after swapping) is null.
	 * The default implementation typically returns "&lt;null&gt;" or similar.</p>
	 *
	 * @return The null value representation
	 */
	String nullValue();

	/**
	 * Pre-processes objects before conversion operations.
	 *
	 * <p>This method applies object transformations before stringification or listification.
	 * Common swapping operations include:</p>
	 * <ul>
	 * 	<li><b>Optional:</b> Unwraps to contained value or null</li>
	 * 	<li><b>Supplier:</b> Calls get() to retrieve the supplied value</li>
	 * 	<li><b>Lazy objects:</b> Forces evaluation of deferred computations</li>
	 * 	<li><b>Wrapper types:</b> Extracts wrapped values</li>
	 * </ul>
	 *
	 * <p>The method may be called recursively if swapping produces another swappable type.</p>
	 *
	 * @param o The object to swap
	 * @return The swapped object, or the original object if no swapping is needed
	 */
	Object swap(Object o);

	/**
	 * Accesses a named property or field from an object.
	 *
	 * <p>This is the core property access method used by BCT assertions. It employs
	 * multiple fallback strategies to extract values from objects:</p>
	 *
	 * <h5 class='section'>Access Priority Order:</h5>
	 * <ol>
	 * 	<li><b>Map key access:</b> If object is a Map, looks up the name as a key</li>
	 * 	<li><b>Collection/Array access:</b> Numeric names for indexed access</li>
	 * 	<li><b>Universal size properties:</b> "length" and "size" for any collection-like object</li>
	 * 	<li><b>is{Name}() methods:</b> Boolean property getters</li>
	 * 	<li><b>get{Name}() methods:</b> Standard JavaBean getters</li>
	 * 	<li><b>get(String) methods:</b> Map-style property access</li>
	 * 	<li><b>Public fields:</b> Direct field access via reflection</li>
	 * 	<li><b>No-arg methods:</b> Methods with the exact property name</li>
	 * </ol>
	 *
	 * <h5 class='section'>Special Property Names:</h5>
	 * <ul>
	 * 	<li><b>"length"/"size":</b> Returns size for arrays, collections, maps</li>
	 * 	<li><b>"0", "1", "2", etc.:</b> Array/list element access by index</li>
	 * 	<li><b>"&lt;NULL&gt;":</b> Accesses null key in maps</li>
	 * 	<li><b>"-1", "-2", etc.:</b> Negative indexing from end of array/list</li>
	 * </ul>
	 *
	 * @param object The object to access properties from
	 * @param name The property/field name to access
	 * @return The property value
	 * @throws RuntimeException if the property cannot be found or accessed
	 */
	Object getProperty(Object object, String name);

	<T> T getSetting(String key, T defaultValue);
}
