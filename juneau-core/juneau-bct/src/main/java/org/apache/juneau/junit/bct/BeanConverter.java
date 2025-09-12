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
package org.apache.juneau.junit.bct;

import java.util.*;

/**
 * Abstract interface for Bean-Centric Test (BCT) object conversion and property access.
 *
 * <p>This interface defines the core contract for converting objects to strings and lists,
 * and for accessing object properties in a uniform way. It forms the foundation of the BCT
 * testing framework, enabling consistent object introspection and value extraction across
 * different object types and structures.</p>
 *
 * <h5 class='section'>Core Conversion Operations:</h5>
 * <dl>
 *    <dt><b>{@link #stringify(Object)}</b></dt>
 *    <dd>Converts any object to its string representation, handling nested structures</dd>
 *
 *    <dt><b>{@link #listify(Object)}</b></dt>
 *    <dd>Converts collection-like objects (arrays, Collections, Iterables, etc.) to List&lt;Object&gt;</dd>
 *
 *    <dt><b>{@link #swap(Object)}</b></dt>
 *    <dd>Pre-processes objects before conversion (e.g., unwrapping Optional, calling Supplier)</dd>
 *
 *    <dt><b>{@link #getProperty(Object, String)}</b></dt>
 *    <dd>Accesses object properties using multiple fallback mechanisms</dd>
 * </dl>
 *
 * <h5 class='section'>Property Access Strategy:</h5>
 * <p>The {@link #getProperty(Object, String)} method uses a comprehensive fallback approach:</p>
 *
 * <h5 class='section'>Usage in BCT Framework:</h5>
 * <p>This interface is used internally by BCT assertion methods like:</p>
 * <ul>
 *    <li>{@link BctAssertions#assertBean(Object, String, String)}</li>
 *    <li>{@link BctAssertions#assertMapped(Object, java.util.function.BiFunction, String, String)}</li>
 *    <li>{@link BctAssertions#assertList(List, Object...)}</li>
 *    <li>{@link BctAssertions#assertBeans(Collection, String, String...)}</li>
 * </ul>
 *
 * @see BasicBeanConverter
 * @see BctAssertions
 */
public interface BeanConverter {

	/**
	 * Converts an object to its string representation for testing purposes.
	 *
	 * @param o The object to stringify
	 * @return The string representation of the object
	 */
	String stringify(Object o);

	/**
	 * Converts a collection-like object to a standardized List&lt;Object&gt; format.
	 *
	 * @param o The object to convert to a list. Must not be null.
	 * @return A List containing the elements
	 * @throws IllegalArgumentException if the object is null or cannot be converted to a list
	 */
	List<Object> listify(Object o);

	/**
	 * Determines if an object can be converted to a list.
	 *
	 * @param o The object to test. May be null.
	 * @return True if the object can be listified, false if null or cannot be listified
	 */
	boolean canListify(Object o);

	/**
	 * Pre-processes objects before conversion operations.
	 *
	 * @param o The object to swap
	 * @return The swapped object, or the original object if no swapping is needed
	 */
	Object swap(Object o);

	/**
	 * Accesses a named property or field from an object.
	 *
	 * @param object The object to access properties from
	 * @param name The property/field name to access
	 * @return The property value
	 * @throws RuntimeException if the property cannot be found or accessed
	 */
	Object getProperty(Object object, String name);

	/**
	 * Retrieves a configuration setting value with a fallback default.
	 *
	 * @param <T> The type of the setting value
	 * @param key The setting key to retrieve
	 * @param defaultValue The value to return if the setting is not found
	 * @return The setting value if found, otherwise the default value
	 */
	<T> T getSetting(String key, T defaultValue);

	/**
	 * Extracts a nested property value using structured field access syntax.
	 *
	 * @param o The object to extract nested properties from. May be null.
	 * @param token The parsed token containing the property access structure. Must not be null.
	 * @return A formatted string representation of the extracted nested values
	 * @throws IllegalArgumentException if the token is null
	 */
	String getNested(Object o, NestedTokenizer.Token token);
}
