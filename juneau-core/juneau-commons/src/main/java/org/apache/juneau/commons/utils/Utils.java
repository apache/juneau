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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;

/**
 * Common utility methods.
 *
 * <p>
 * This class contains various static utility methods for working with collections, strings, objects, and other common operations.
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 *   <li><b>Collections:</b> Array and list creation, conversion, and manipulation
 *   <li><b>Strings:</b> Formatting, comparison, and null-safe operations
 *   <li><b>Objects:</b> Equality checking, casting, and null handling
 *   <li><b>Environment:</b> System property and environment variable access
 *   <li><b>Optionals:</b> Enhanced Optional operations and conversions
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 *   <li class='jc'>{@link AssertionUtils} - For argument validation and assertion methods
 *   <li class='jc'>{@link StringUtils} - For additional string manipulation utilities
 *   <li class='link'><a class="doclink" href='../../../../../index.html#juneau-commons.utils'>Overview &gt; juneau-commons.utils</a>
 * </ul>
 */
public class Utils {

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();

	static {
		ENV_FUNCTIONS.put(Boolean.class, Boolean::valueOf);
		ENV_FUNCTIONS.put(Charset.class, Charset::forName);
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();

	/**
	 * Converts an object to a boolean.
	 *
	 * @param val The object to convert.
	 * @return The boolean value, or <jk>false</jk> if the value was <jk>null</jk>.
	 */
	public static boolean b(Object val) {
		return opt(val).map(Object::toString).map(Boolean::valueOf).orElse(false);
	}

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * <p>
	 * This is a null-safe and type-safe casting operation. Returns <jk>null</jk> if:
	 * <ul>
	 *   <li>The object is <jk>null</jk></li>
	 *   <li>The object is not an instance of the specified type</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Object <jv>obj</jv> = <js>"Hello"</js>;
	 * 	String <jv>str</jv> = cast(String.<jk>class</jk>, <jv>obj</jv>);     <jc>// "Hello"</jc>
	 * 	Integer <jv>num</jv> = cast(Integer.<jk>class</jk>, <jv>obj</jv>);   <jc>// null (not an Integer)</jc>
	 * 	String <jv>str2</jv> = cast(String.<jk>class</jk>, <jk>null</jk>);   <jc>// null</jc>
	 * </p>
	 *
	 * @param <T> The type to cast to.
	 * @param c The type to cast to.
	 * @param o The object to cast.
	 * @return The cast object, or <jk>null</jk> if the object wasn't the specified type or was <jk>null</jk>.
	 * @see #castOrNull(Object, Class)
	 */
	public static <T> T cast(Class<T> c, Object o) {
		return nn(o) && c.isInstance(o) ? c.cast(o) : null;
	}

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * <p>
	 * This method is similar to {@link #cast(Class, Object)} but with a different parameter order.
	 * Returns <jk>null</jk> if the object is not an instance of the specified class.
	 * Note: Unlike {@link #cast(Class, Object)}, this method does not check for <jk>null</jk> objects
	 * before checking the type, so a <jk>null</jk> object will return <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Object <jv>obj</jv> = <js>"Hello"</js>;
	 * 	String <jv>str</jv> = castOrNull(<jv>obj</jv>, String.<jk>class</jk>);     <jc>// "Hello"</jc>
	 * 	Integer <jv>num</jv> = castOrNull(<jv>obj</jv>, Integer.<jk>class</jk>);   <jc>// null (not an Integer)</jc>
	 * 	String <jv>str2</jv> = castOrNull(<jk>null</jk>, String.<jk>class</jk>);   <jc>// null</jc>
	 * </p>
	 *
	 * @param <T> The class to cast to.
	 * @param o The object to cast.
	 * @param c The class to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't an instance of the specified class.
	 * @see #cast(Class, Object)
	 */
	public static <T> T castOrNull(Object o, Class<T> c) {
		if (c.isInstance(o))
			return c.cast(o);
		return null;
	}

	/**
	 * Shortcut for calling {@link ClassUtils#className(Object)}.
	 *
	 * <p>
	 * Returns the fully-qualified class name including the full package path.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	cn(String.<jk>class</jk>);                  <jc>// "java.lang.String"</jc>
	 * 	cn(<jk>new</jk> HashMap&lt;&gt;());                <jc>// "java.util.HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	cn(Map.Entry.<jk>class</jk>);               <jc>// "java.util.Map$Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	cn(<jk>int</jk>.<jk>class</jk>);                      <jc>// "int"</jc>
	 * 	cn(<jk>boolean</jk>.<jk>class</jk>);                  <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	cn(String[].<jk>class</jk>);                <jc>// "[Ljava.lang.String;"</jc>
	 * 	cn(<jk>int</jk>[].<jk>class</jk>);                    <jc>// "[I"</jc>
	 * 	cn(String[][].<jk>class</jk>);              <jc>// "[[Ljava.lang.String;"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	cn(<jk>null</jk>);                          <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the class name for.
	 * @return The name of the class or <jk>null</jk> if the value was null.
	 */
	public static String cn(Object value) {
		return ClassUtils.className(value);
	}

	/**
	 * Shortcut for calling {@link ClassUtils#classNameSimple(Object)}.
	 *
	 * <p>
	 * Returns only the simple class name without any package or outer class information.
	 * For inner classes, only the innermost class name is returned.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	scn(String.<jk>class</jk>);            <jc>// "String"</jc>
	 * 	scn(<jk>new</jk> HashMap&lt;&gt;());          <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	scn(Map.Entry.<jk>class</jk>);         <jc>// "Entry"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	scn(<jk>int</jk>.<jk>class</jk>);                <jc>// "int"</jc>
	 * 	scn(<jk>boolean</jk>.<jk>class</jk>);            <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	scn(String[].<jk>class</jk>);          <jc>// "String[]"</jc>
	 * 	scn(<jk>int</jk>[].<jk>class</jk>);              <jc>// "int[]"</jc>
	 * 	scn(String[][].<jk>class</jk>);        <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	scn(<jk>null</jk>);                    <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple class name for.
	 * @return The simple name of the class or <jk>null</jk> if the value was null.
	 */
	public static String cns(Object value) {
		return ClassUtils.classNameSimple(value);
	}

	/**
	 * Shortcut for calling {@link ClassUtils#classNameSimpleQualified(Object)}.
	 *
	 * <p>
	 * Returns the simple class name including outer class names, but without the package.
	 * Inner class separators ($) are replaced with dots (.).
	 * Array types are properly formatted with brackets.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Regular classes</jc>
	 * 	sqcn(String.<jk>class</jk>);                     <jc>// "String"</jc>
	 * 	sqcn(<jk>new</jk> HashMap&lt;&gt;());                   <jc>// "HashMap"</jc>
	 *
	 * 	<jc>// Inner classes</jc>
	 * 	sqcn(Map.Entry.<jk>class</jk>);                  <jc>// "Map.Entry"</jc>
	 * 	sqcn(Outer.Inner.Deep.<jk>class</jk>);           <jc>// "Outer.Inner.Deep"</jc>
	 *
	 * 	<jc>// Primitives</jc>
	 * 	sqcn(<jk>int</jk>.<jk>class</jk>);                         <jc>// "int"</jc>
	 * 	sqcn(<jk>boolean</jk>.<jk>class</jk>);                     <jc>// "boolean"</jc>
	 *
	 * 	<jc>// Object arrays</jc>
	 * 	sqcn(String[].<jk>class</jk>);                   <jc>// "String[]"</jc>
	 * 	sqcn(Map.Entry[].<jk>class</jk>);                <jc>// "Map.Entry[]"</jc>
	 * 	sqcn(String[][].<jk>class</jk>);                 <jc>// "String[][]"</jc>
	 *
	 * 	<jc>// Primitive arrays</jc>
	 * 	sqcn(<jk>int</jk>[].<jk>class</jk>);                       <jc>// "int[]"</jc>
	 * 	sqcn(<jk>boolean</jk>[][].<jk>class</jk>);                 <jc>// "boolean[][]"</jc>
	 *
	 * 	<jc>// Null</jc>
	 * 	sqcn(<jk>null</jk>);                             <jc>// null</jc>
	 * </p>
	 *
	 * @param value The object to get the simple qualified class name for.
	 * @return The simple qualified name of the class or <jk>null</jk> if the value was null.
	 */
	public static String cnsq(Object value) {
		return ClassUtils.classNameSimpleQualified(value);
	}

	/**
	 * Compares two objects for ordering.
	 *
	 * <p>
	 * This method attempts to compare two objects using their natural ordering if they implement
	 * {@link Comparable} and are of the same type. Null handling:
	 * <ul>
	 *   <li>Both <jk>null</jk> → returns <c>0</c> (equal)</li>
	 *   <li>First <jk>null</jk> → returns <c>-1</c> (null is less-than)</li>
	 *   <li>Second <jk>null</jk> → returns <c>1</c> (null is less-than)</li>
	 *   <li>Different types or not Comparable → returns <c>0</c> (cannot compare)</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	compare(<js>"apple"</js>, <js>"banana"</js>);   <jc>// negative (apple &lt; banana)</jc>
	 * 	compare(5, 10);                                 <jc>// negative (5 &lt; 10)</jc>
	 * 	compare(<js>"apple"</js>, <js>"apple"</js>);    <jc>// 0 (equal)</jc>
	 * 	compare(<jk>null</jk>, <jk>null</jk>);          <jc>// 0 (equal)</jc>
	 * 	compare(<jk>null</jk>, <js>"apple"</js>);       <jc>// -1 (null &lt; non-null)</jc>
	 * 	compare(<js>"apple"</js>, 5);                   <jc>// 0 (different types, cannot compare)</jc>
	 * </p>
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <c>-1</c>, <c>0</c>, or <c>1</c> if <c>o1</c> is less-than, equal, or greater-than <c>o2</c>.
	 *         Returns <c>0</c> if objects are not of the same type or do not implement the {@link Comparable} interface.
	 */
	@SuppressWarnings("unchecked")
	public static int compare(Object o1, Object o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return -1;
		} else if (o2 == null) {
			return 1;
		}

		if (eq(o1.getClass(), o2.getClass()) && o1 instanceof Comparable o1a)
			return o1a.compareTo(o2);

		return 0;
	}

	/**
	 * Creates an empty array of the specified type.
	 *
	 * <p>
	 * This is a convenience method for creating empty arrays using reflection. Useful when you need
	 * an empty array of a specific type but don't have an instance to call <c>new T[0]</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>empty</jv> = ea(String.<jk>class</jk>);        <jc>// new String[0]</jc>
	 * 	Integer[] <jv>empty2</jv> = ea(Integer.<jk>class</jk>);      <jc>// new Integer[0]</jc>
	 * 	List&lt;String&gt;[] <jv>empty3</jv> = ea(List.<jk>class</jk>);  <jc>// new List[0]</jc>
	 * </p>
	 *
	 * @param <T> The component type of the array.
	 * @param type The component type class.
	 * @return An empty array of the specified type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] ea(Class<T> type) {
		return (T[])Array.newInstance(type, 0);
	}

	/**
	 * Returns the string representation of an object, or an empty string if the object is <jk>null</jk>.
	 *
	 * <p>
	 * This is a null-safe string conversion method. If the object is <jk>null</jk>, returns an empty string.
	 * Otherwise, returns the result of calling {@link Object#toString()} on the object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	emptyIfNull(<js>"Hello"</js>);     <jc>// "Hello"</jc>
	 * 	emptyIfNull(123);                  <jc>// "123"</jc>
	 * 	emptyIfNull(<jk>null</jk>);        <jc>// ""</jc>
	 * </p>
	 *
	 * @param value The value to convert to a string. Can be <jk>null</jk>.
	 * @return The string representation of the value, or an empty string if <jk>null</jk>.
	 * @see Object#toString()
	 */
	public static String emptyIfNull(Object value) {
		return value == null ? "" : value.toString();
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * This method searches for a value in the following order:
	 * <ol>
	 *   <li>System properties (via {@link System#getProperty(String)})</li>
	 *   <li>Environment variables (via {@link System#getenv(String)}) - the name is converted to env-safe format</li>
	 * </ol>
	 *
	 * <p>
	 * Returns an empty {@link Optional} if the value is not found in either location.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// System property: -Dmy.property=value</jc>
	 * 	Optional&lt;String&gt; <jv>prop</jv> = env(<js>"my.property"</js>);  <jc>// Optional.of("value")</jc>
	 *
	 * 	<jc>// Environment variable: MY_PROPERTY=value</jc>
	 * 	Optional&lt;String&gt; <jv>env</jv> = env(<js>"my.property"</js>);   <jc>// Optional.of("value") (converts to MY_PROPERTY)</jc>
	 *
	 * 	Optional&lt;String&gt; <jv>missing</jv> = env(<js>"nonexistent"</js>);  <jc>// Optional.empty()</jc>
	 * </p>
	 *
	 * @param name The property name (will be converted to env-safe format for environment variable lookup).
	 * @return An {@link Optional} containing the value if found, or empty if not found.
	 * @see #env(String, Object)
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	public static Optional<String> env(String name) {
		var s = System.getProperty(name);
		if (s == null)
			s = System.getenv(envName(name));
		return opt(s);
	}

	/**
	 * Looks up a system property or environment variable, returning a default value if not found.
	 *
	 * <p>
	 * This method searches for a value in the following order:
	 * <ol>
	 *   <li>System properties (via {@link System#getProperty(String)})</li>
	 *   <li>Environment variables (via {@link System#getenv(String)}) - the name is converted to env-safe format</li>
	 *   <li>Returns the default value if not found</li>
	 * </ol>
	 *
	 * <p>
	 * If a value is found, it is converted to the type of the default value using {@link #toType(String, Object)}.
	 * Supported types include {@link Boolean}, {@link Charset}, and other common types.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// System property: -Dmy.property=true</jc>
	 * 	Boolean <jv>flag</jv> = env(<js>"my.property"</js>, <jk>false</jk>);  <jc>// true</jc>
	 *
	 * 	<jc>// Environment variable: MY_PROPERTY=UTF-8</jc>
	 * 	Charset <jv>charset</jv> = env(<js>"my.property"</js>, Charset.defaultCharset());  <jc>// UTF-8</jc>
	 *
	 * 	<jc>// Not found, returns default</jc>
	 * 	String <jv>value</jv> = env(<js>"nonexistent"</js>, <js>"default"</js>);  <jc>// "default"</jc>
	 * </p>
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name (will be converted to env-safe format for environment variable lookup).
	 * @param def The default value to return if not found.
	 * @return The found value (converted to type T), or the default value if not found.
	 * @see #env(String)
	 * @see #toType(String, Object)
	 */
	public static <T> T env(String name, T def) {
		return env(name).map(x -> toType(x, def)).orElse(def);
	}

	/**
	 * Tests two strings for equality, with optional case-insensitive matching.
	 *
	 * <p>
	 * This method provides a unified way to compare strings with or without case sensitivity.
	 * Both strings are handled gracefully for <jk>null</jk> values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	eq(<jk>false</jk>, <js>"Hello"</js>, <js>"Hello"</js>);     <jc>// true (case-sensitive)</jc>
	 * 	eq(<jk>false</jk>, <js>"Hello"</js>, <js>"hello"</js>);     <jc>// false (case-sensitive)</jc>
	 * 	eq(<jk>true</jk>, <js>"Hello"</js>, <js>"hello"</js>);      <jc>// true (case-insensitive)</jc>
	 * 	eq(<jk>false</jk>, <jk>null</jk>, <jk>null</jk>);          <jc>// true (both null)</jc>
	 * 	eq(<jk>false</jk>, <js>"Hello"</js>, <jk>null</jk>);       <jc>// false</jc>
	 * </p>
	 *
	 * @param caseInsensitive Use case-insensitive matching.
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal (according to the case sensitivity setting).
	 * @see #eq(String, String)
	 * @see #eqic(String, String)
	 */
	public static boolean eq(boolean caseInsensitive, String s1, String s2) {
		return caseInsensitive ? eqic(s1, s2) : eq(s1, s2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls and arrays.
	 *
	 * <p>
	 * This method handles annotations specially by delegating to {@link AnnotationUtils#equals(Annotation, Annotation)}
	 * to ensure proper annotation comparison according to the annotation equality contract.
	 *
	 * @param <T> The value types.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 * @see AnnotationUtils#equals(Annotation, Annotation)
	 */
	public static <T> boolean eq(T o1, T o2) {
		// Handle annotations specially
		if (o1 instanceof java.lang.annotation.Annotation o1a && o2 instanceof java.lang.annotation.Annotation o2a)
			return AnnotationUtils.equals(o1a, o2a);

		if (isArray(o1) && isArray(o2)) {
			int l1 = Array.getLength(o1), l2 = Array.getLength(o2);
			if (l1 != l2)
				return false;
			for (var i = 0; i < l1; i++)
				if (ne(Array.get(o1, i), Array.get(o2, i)))
					return false;
			return true;
		}
		return Objects.equals(o1, o2);
	}

	/**
	 * Tests two objects for equality using a custom predicate, gracefully handling nulls.
	 *
	 * <p>
	 * This method provides a convenient way to implement custom equality logic while handling null values
	 * safely. The predicate is only called if both objects are non-null and not the same reference.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Custom equality for a Role class</jc>
	 * 	<jk>public</jk> <jk>boolean</jk> equals(Object o) {
	 * 		<jk>return</jk> eq(<jk>this</jk>, (Role)o, (x,y) -&gt;
	 * 			eq(x.id, y.id) &amp;&amp;
	 * 			eq(x.name, y.name) &amp;&amp;
	 * 			eq(x.created, y.created) &amp;&amp;
	 * 			eq(x.createdBy, y.createdBy)
	 * 		);
	 * 	}
	 *
	 * 	<jc>// Usage</jc>
	 * 	Role <jv>r1</jv> = <jk>new</jk> Role(1, <js>"admin"</js>);
	 * 	Role <jv>r2</jv> = <jk>new</jk> Role(1, <js>"admin"</js>);
	 * 	eq(<jv>r1</jv>, <jv>r2</jv>, (x,y) -&gt; x.id == y.id &amp;&amp; x.name.equals(y.name));  <jc>// true</jc>
	 * </p>
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The predicate to use for equality testing (only called if both objects are non-null and different references).
	 * @return <jk>true</jk> if both objects are equal based on the test, or if both are <jk>null</jk>, or if they are the same reference.
	 * @see #eq(Object, Object)
	 */
	public static <T,U> boolean eq(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null) {
			return o2 == null;
		}
		if (o2 == null) {
			return false;
		}
		if (o1 == o2) {
			return true;
		}
		return test.test(o1, o2);
	}

	/**
	 * Convenience method for calling {@link StringUtils#equalsIgnoreCase(Object, Object)}.
	 *
	 * <p>
	 * Tests two objects for case-insensitive string equality by converting them to strings.
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal ignoring case.
	 */
	public static boolean eqic(Object o1, Object o2) {
		return StringUtils.equalsIgnoreCase(o1, o2);
	}

	/**
	 * Convenience method for calling {@link StringUtils#equalsIgnoreCase(String, String)}.
	 *
	 * <p>
	 * Tests two strings for case-insensitive equality, but gracefully handles nulls.
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean eqic(String s1, String s2) {
		return StringUtils.equalsIgnoreCase(s1, s2);
	}

	/**
	 * Shortcut for calling {@link StringUtils#format(String, Object...)}.
	 *
	 * <p>
	 * This method provides a convenient shorthand for string formatting that supports both
	 * MessageFormat-style and printf-style formatting in the same pattern.
	 *
	 * <h5 class='section'>Format Support:</h5>
	 * <ul>
	 *   <li><b>Printf-style:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, <js>"%1$s"</js>, etc.</li>
	 *   <li><b>MessageFormat-style:</b> <js>"{0}"</js>, <js>"{1,number}"</js>, <js>"{2,date}"</js>, etc.</li>
	 *   <li><b>Un-numbered MessageFormat:</b> <js>"{}"</js> - Sequential placeholders that are automatically numbered</li>
	 *   <li><b>Mixed formats:</b> Both styles can be used in the same pattern</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Printf-style formatting</jc>
	 * 	f(<js>"Hello %s, you have %d items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Floating point</jc>
	 * 	f(<js>"Price: $%.2f"</js>, 19.99);
	 * 	<jc>// Returns: "Price: $19.99"</jc>
	 *
	 * 	<jc>// MessageFormat-style formatting</jc>
	 * 	f(<js>"Hello {0}, you have {1} items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Un-numbered MessageFormat placeholders (sequential)</jc>
	 * 	f(<js>"Hello {}, you have {} items"</js>, <js>"John"</js>, 5);
	 * 	<jc>// Returns: "Hello John, you have 5 items"</jc>
	 *
	 * 	<jc>// Mixed format styles in the same pattern</jc>
	 * 	f(<js>"User {0} has %d items and %s status"</js>, <js>"Alice"</js>, 10, <js>"active"</js>);
	 * 	<jc>// Returns: "User Alice has 10 items and active status"</jc>
	 * </p>
	 *
	 * @param pattern The format string supporting both MessageFormat and printf-style placeholders.
	 * @param args The arguments to format.
	 * @return The formatted string.
	 * @see StringUtils#format(String, Object...)
	 * @see StringFormat for detailed format specification
	 * @see #mf(String, Object...) for MessageFormat-only formatting
	 */
	public static String f(String pattern, Object...args) {
		return format(pattern, args);
	}

	/**
	 * Returns the first non-null value in the specified array.
	 *
	 * <p>
	 * This method iterates through the provided values and returns the first one that is not <jk>null</jk>.
	 * Useful for providing default values or selecting the first available option.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	firstNonNull(<jk>null</jk>, <jk>null</jk>, <js>"Hello"</js>, <js>"World"</js>);   <jc>// "Hello"</jc>
	 * 	firstNonNull(<js>"Hello"</js>, <js>"World"</js>);                                <jc>// "Hello"</jc>
	 * 	firstNonNull(<jk>null</jk>, <jk>null</jk>);                                      <jc>// null</jc>
	 * 	firstNonNull();                                                                  <jc>// null</jc>
	 * </p>
	 *
	 * @param <T> The value types.
	 * @param t The values to check.
	 * @return The first non-null value, or <jk>null</jk> if the array is null or empty or contains only <jk>null</jk> values.
	 * @see StringUtils#firstNonEmpty(String...)
	 * @see StringUtils#firstNonBlank(String...)
	 */
	@SafeVarargs
	public static <T> T firstNonNull(T...t) {
		if (nn(t))
			for (var tt : t)
				if (nn(tt))
					return tt;
		return null;
	}

	/**
	 * Creates a formatted string supplier with format arguments for lazy evaluation.
	 *
	 * <p>This method returns a {@link Supplier} that formats the string pattern with the provided arguments
	 * only when the supplier's {@code get()} method is called. This is useful for expensive string formatting
	 * operations that may not always be needed, such as error messages in assertions.</p>
	 *
	 * <p>
	 * Supports both MessageFormat-style and printf-style formatting in the same pattern.
	 * Also supports un-numbered MessageFormat placeholders: <js>"{}"</js> - Sequential placeholders that are automatically numbered.
	 * See {@link #f(String, Object...)} for format specification details.
	 * </p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Lazy evaluation - string is only formatted if assertion fails</jc>
	 * 	assertTrue(condition, fs(<js>"Expected %s but got %s"</js>, expected, actual));
	 *
	 * 	<jc>// Can be used anywhere a Supplier&lt;String&gt; is expected</jc>
	 * 	Supplier&lt;String&gt; <jv>messageSupplier</jv> = fs(<js>"Processing item %d of %d"</js>, i, total);
	 *
	 * 	<jc>// Mixed format styles</jc>
	 * 	Supplier&lt;String&gt; <jv>msg</jv> = fs(<js>"User {0} has %d items"</js>, <js>"Alice"</js>, 10);
	 *
	 * 	<jc>// Un-numbered MessageFormat placeholders</jc>
	 * 	Supplier&lt;String&gt; <jv>msg2</jv> = fs(<js>"Hello {}, you have {} items"</js>, <js>"John"</js>, 5);
	 * </p>
	 *
	 * @param pattern The format string supporting both MessageFormat and printf-style placeholders
	 * 	(e.g., <js>"{0}"</js>, <js>"%s"</js>, <js>"%d"</js>, etc.).
	 * @param args The arguments to substitute into the pattern placeholders.
	 * @return A {@link Supplier} that will format the string when {@code get()} is called.
	 * @see StringUtils#format(String, Object...)
	 * @see #f(String, Object...) for format specification details
	 * @see #mfs(String, Object...) for MessageFormat-only formatting
	 */
	public static Supplier<String> fs(String pattern, Object...args) {
		return () -> format(pattern, args);
	}

	/**
	 * Calculates a hash code for the specified values.
	 *
	 * <p>
	 * This method combines multiple values into a single hash code using the same algorithm as
	 * {@link Objects#hash(Object...)}. It handles annotations specially by delegating to
	 * {@link AnnotationUtils#hash(Annotation)} to ensure consistent hashing according to the
	 * {@link java.lang.annotation.Annotation#hashCode()} contract.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Hash multiple values</jc>
	 * 	<jk>int</jk> <jv>hash1</jv> = hash(<js>"Hello"</js>, 123, <jk>true</jk>);
	 *
	 * 	<jc>// Hash with annotations</jc>
	 * 	<jk>int</jk> <jv>hash2</jv> = hash(<jv>myAnnotation</jv>, <js>"value"</js>);
	 *
	 * 	<jc>// Use in hashCode() implementation</jc>
	 * 	<jk>public</jk> <jk>int</jk> hashCode() {
	 * 		<jk>return</jk> hash(id, name, created);
	 * 	}
	 * </p>
	 *
	 * @param values The values to hash.
	 * @return A hash code value for the given values.
	 * @see AnnotationUtils#hash(Annotation)
	 * @see Objects#hash(Object...)
	 */
	public static final int hash(Object...values) {
		assertArgNotNull("values", values);
		var result = 1;
		for (var value : values) {
			result = 31 * result + (value instanceof java.lang.annotation.Annotation ? AnnotationUtils.hash((java.lang.annotation.Annotation)value) : Objects.hashCode(value));
		}
		return result;
	}

	/**
	 * Converts the specified object into an identifiable string of the form "Class[identityHashCode]"
	 * @param o The object to convert to a string.
	 * @return An identity string.
	 */
	public static String identity(Object o) {
		if (o instanceof Optional<?> opt)
			o = opt.orElse(null);
		if (o == null)
			return null;
		return cnsq(o) + "@" + System.identityHashCode(o);
	}

	/**
	 * Checks if the specified object is an array.
	 *
	 * <p>
	 * This method checks if the object is not <jk>null</jk> and its class represents an array type
	 * (primitive arrays or object arrays).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isArray(<jk>new</jk> <jk>int</jk>[]{1, 2, 3});        <jc>// true</jc>
	 * 	isArray(<jk>new</jk> String[]{"a", "b"});            <jc>// true</jc>
	 * 	isArray(<js>"Hello"</js>);                           <jc>// false</jc>
	 * 	isArray(<jk>null</jk>);                              <jc>// false</jc>
	 * </p>
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is not <jk>null</jk> and is an array.
	 * @see Class#isArray()
	 */
	public static boolean isArray(Object o) {
		return nn(o) && o.getClass().isArray();
	}

	/**
	 * Returns <jk>true</jk> if the specified number is inclusively between the two values.
	 *
	 * @param n The number to check.
	 * @param lower The lower bound (inclusive).
	 * @param higher The upper bound (inclusive).
	 * @return <jk>true</jk> if the number is between the bounds.
	 */
	public static boolean isBetween(int n, int lower, int higher) {
		return n >= lower && n <= higher;
	}

	/**
	 * Checks if a string is empty (null or zero length).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isEmpty(<jk>null</jk>);       <jc>// true</jc>
	 * 	isEmpty(<js>""</js>);         <jc>// true</jc>
	 * 	isEmpty(<js>"   "</js>);      <jc>// false</jc>
	 * 	isEmpty(<js>"hello"</js>);    <jc>// false</jc>
	 * </p>
	 *
	 * @param str The string to check.
	 * @return <jk>true</jk> if the string is null or has zero length.
	 */
	public static boolean isEmpty(CharSequence str) {
		return str == null || str.isEmpty();
	}

	/**
	 * Checks if the specified collection is <jk>null</jk> or empty.
	 *
	 * <p>
	 * This is a null-safe operation. Returns <jk>true</jk> if the collection is <jk>null</jk> or
	 * has no elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isEmpty(<jk>null</jk>);                    <jc>// true</jc>
	 * 	isEmpty(Collections.emptyList());         <jc>// true</jc>
	 * 	isEmpty(Arrays.asList(1, 2, 3));          <jc>// false</jc>
	 * </p>
	 *
	 * @param o The collection to check.
	 * @return <jk>true</jk> if the specified collection is <jk>null</jk> or empty.
	 * @see #isNotEmpty(Collection)
	 * @see Collection#isEmpty()
	 */
	public static boolean isEmpty(Collection<?> o) {
		if (o == null)
			return true;
		return o.isEmpty();
	}

	/**
	 * Checks if the specified map is <jk>null</jk> or empty.
	 *
	 * <p>
	 * This is a null-safe operation. Returns <jk>true</jk> if the map is <jk>null</jk> or
	 * has no key-value mappings.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isEmpty(<jk>null</jk>);                    <jc>// true</jc>
	 * 	isEmpty(Collections.emptyMap());          <jc>// true</jc>
	 * 	isEmpty(Map.of(<js>"key"</js>, <js>"value"</js>));  <jc>// false</jc>
	 * </p>
	 *
	 * @param o The map to check.
	 * @return <jk>true</jk> if the specified map is <jk>null</jk> or empty.
	 * @see #isNotEmpty(Map)
	 * @see Map#isEmpty()
	 */
	public static boolean isEmpty(Map<?,?> o) {
		if (o == null)
			return true;
		return o.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is empty.
	 *
	 * <p>
	 * Return <jk>true</jk> if the value is any of the following:
	 * <ul>
	 * 	<li><jk>null</jk>
	 * 	<li>An empty Collection
	 * 	<li>An empty Map
	 * 	<li>An empty array
	 * 	<li>An empty CharSequence
	 * 	<li>An empty String when serialized to a string using {@link Object#toString()}.
	 * </ul>
	 *
	 * @param o The object to test.
	 * @return <jk>true</jk> if the specified object is empty.
	 */
	public static boolean isEmpty(Object o) {
		if (o == null)
			return true;
		if (o instanceof Collection<?> o2)
			return o2.isEmpty();
		if (o instanceof Map<?,?> o2)
			return o2.isEmpty();
		if (isArray(o))
			return (Array.getLength(o) == 0);
		return o.toString().isEmpty();
	}

	/**
	 * Checks if the specified string is not <jk>null</jk> and not empty.
	 *
	 * <p>
	 * This is the inverse of {@link #isEmpty(CharSequence)}.
	 * Note: This method does not check for blank strings (whitespace-only strings).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotEmpty(<js>"Hello"</js>);     <jc>// true</jc>
	 * 	isNotEmpty(<js>"   "</js>);       <jc>// true (whitespace is not empty)</jc>
	 * 	isNotEmpty(<jk>null</jk>);        <jc>// false</jc>
	 * 	isNotEmpty(<js>""</js>);          <jc>// false</jc>
	 * </p>
	 *
	 * @param o The string to check.
	 * @return <jk>true</jk> if string is not <jk>null</jk> and not empty.
	 * @see #isEmpty(CharSequence)
	 * @see StringUtils#isNotBlank(String)
	 */
	public static boolean isNotEmpty(CharSequence o) {
		return ! isEmpty(o);
	}

	/**
	 * Checks if the specified collection is not <jk>null</jk> and not empty.
	 *
	 * <p>
	 * This is the inverse of {@link #isEmpty(Collection)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotEmpty(Arrays.asList(1, 2, 3));          <jc>// true</jc>
	 * 	isNotEmpty(<jk>null</jk>);                    <jc>// false</jc>
	 * 	isNotEmpty(Collections.emptyList());         <jc>// false</jc>
	 * </p>
	 *
	 * @param value The collection to check.
	 * @return <jk>true</jk> if the specified collection is not <jk>null</jk> and not empty.
	 * @see #isEmpty(Collection)
	 */
	public static boolean isNotEmpty(Collection<?> value) {
		return ! isEmpty(value);
	}

	/**
	 * Checks if the specified map is not <jk>null</jk> and not empty.
	 *
	 * <p>
	 * This is the inverse of {@link #isEmpty(Map)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotEmpty(Map.of(<js>"key"</js>, <js>"value"</js>));  <jc>// true</jc>
	 * 	isNotEmpty(<jk>null</jk>);                             <jc>// false</jc>
	 * 	isNotEmpty(Collections.emptyMap());                    <jc>// false</jc>
	 * </p>
	 *
	 * @param value The map to check.
	 * @return <jk>true</jk> if the specified map is not <jk>null</jk> and not empty.
	 * @see #isEmpty(Map)
	 */
	public static boolean isNotEmpty(Map<?,?> value) {
		return ! isEmpty(value);
	}

	/**
	 * Checks if the specified object is not <jk>null</jk> and not empty.
	 *
	 * <p>
	 * This method works on any of the following data types:
	 * <ul>
	 *   <li>String, CharSequence - checks if length > 0</li>
	 *   <li>Collection - checks if not empty</li>
	 *   <li>Map - checks if not empty</li>
	 *   <li>Array - checks if length > 0</li>
	 *   <li>All other types - converts to string and checks if not empty</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotEmpty(<js>"Hello"</js>);                    <jc>// true</jc>
	 * 	isNotEmpty(Arrays.asList(1, 2));                <jc>// true</jc>
	 * 	isNotEmpty(Map.of(<js>"key"</js>, <js>"value"</js>));  <jc>// true</jc>
	 * 	isNotEmpty(<jk>null</jk>);                       <jc>// false</jc>
	 * 	isNotEmpty(<js>""</js>);                         <jc>// false</jc>
	 * 	isNotEmpty(Collections.emptyList());            <jc>// false</jc>
	 * </p>
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 * @see #isEmpty(Object)
	 */
	public static boolean isNotEmpty(Object value) {
		if (value == null)
			return false;
		if (value instanceof CharSequence value2)
			return ! value2.isEmpty();
		if (value instanceof Collection<?> value2)
			return ! value2.isEmpty();
		if (value instanceof Map<?,?> value2)
			return ! value2.isEmpty();
		if (isArray(value))
			return Array.getLength(value) > 0;
		return isNotEmpty(s(value));
	}

	/**
	 * Checks if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 *
	 * <p>
	 * This method is commonly used to check if a numeric value represents a valid index or ID,
	 * where <c>-1</c> is often used as a sentinel value to indicate "not found" or "invalid".
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotMinusOne(5);        <jc>// true</jc>
	 * 	isNotMinusOne(0);        <jc>// true</jc>
	 * 	isNotMinusOne(-1);       <jc>// false</jc>
	 * 	isNotMinusOne(<jk>null</jk>);  <jc>// false</jc>
	 * </p>
	 *
	 * @param <T> The value types.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 */
	public static <T extends Number> boolean isNotMinusOne(T value) {
		return nn(value) && value.intValue() != -1;
	}

	/**
	 * Checks if the specified object is not <jk>null</jk>.
	 *
	 * <p>
	 * This is equivalent to <c><jv>value</jv> != <jk>null</jk></c>, but provides a more readable
	 * method name for null checks.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isNotNull(<js>"Hello"</js>);     <jc>// true</jc>
	 * 	isNotNull(123);                 <jc>// true</jc>
	 * 	isNotNull(<jk>null</jk>);        <jc>// false</jc>
	 * </p>
	 *
	 * @param <T> The value type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 * @see #nn(Object)
	 */
	public static <T> boolean isNotNull(T value) {
		return value != null;
	}

	/**
	 * Checks if the specified Boolean is not <jk>null</jk> and is <jk>true</jk>.
	 *
	 * <p>
	 * This is a null-safe way to check if a Boolean wrapper is true. Returns <jk>false</jk> if
	 * the value is <jk>null</jk> or <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	isTrue(<jk>true</jk>);        <jc>// true</jc>
	 * 	isTrue(<jk>false</jk>);       <jc>// false</jc>
	 * 	isTrue(<jk>null</jk>);        <jc>// false</jc>
	 * </p>
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 */
	public static boolean isTrue(Boolean value) {
		return nn(value) && value;
	}

	/**
	 * Convenience method for calling {@link StringUtils#lowerCase(String)}.
	 *
	 * <p>
	 * Converts the string to lowercase if not null.
	 *
	 * @param value The string to convert.
	 * @return The lowercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String lc(String value) {
		return StringUtils.lowerCase(value);
	}

	/**
	 * Creates a thread-safe memoizing supplier that computes a value once and caches it.
	 *
	 * <p>
	 * The returned supplier is thread-safe and guarantees that the underlying supplier's {@link Supplier#get() get()}
	 * method is called at most once, even under concurrent access. The computed value is cached and returned
	 * on all subsequent calls.
	 *
	 * <p>
	 * This is useful for lazy initialization of expensive-to-compute values that should only be calculated once.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a memoizing supplier</jc>
	 * 	Supplier&lt;ExpensiveObject&gt; <jv>supplier</jv> = memoize(() -&gt; <jk>new</jk> ExpensiveObject());
	 *
	 * 	<jc>// First call computes and caches the value</jc>
	 * 	ExpensiveObject <jv>obj1</jv> = <jv>supplier</jv>.get();
	 *
	 * 	<jc>// Subsequent calls return the cached value (no recomputation)</jc>
	 * 	ExpensiveObject <jv>obj2</jv> = <jv>supplier</jv>.get();  <jc>// Same instance as obj1</jc>
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * The implementation uses {@link java.util.concurrent.atomic.AtomicReference} with double-checked locking
	 * to ensure thread-safe lazy initialization. Under high contention, multiple threads may compute the value,
	 * but only one result is stored and returned to all callers.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>The supplier may be called multiple times if threads race, but only one result is cached.
	 * 	<li>The cached value can be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * 	<li>Once cached, the value never changes (immutable after first computation).
	 * 	<li>The returned supplier does not support {@link #toString()}, {@link #equals(Object)}, or {@link #hashCode()}.
	 * </ul>
	 *
	 * @param <T> The type of value supplied.
	 * @param supplier The supplier to memoize. Must not be <jk>null</jk>.
	 * @return A thread-safe memoizing wrapper around the supplier.
	 * @throws NullPointerException if supplier is <jk>null</jk>.
	 */
	public static <T> Supplier<T> memoize(Supplier<T> supplier) {
		assertArgNotNull("supplier", supplier);

		var cache = new AtomicReference<Optional<T>>();

		return () -> {
			var h = cache.get();
			if (h == null) {
				h = opt(supplier.get());
				if (! cache.compareAndSet(null, h)) {
					// Another thread beat us, use their value
					h = cache.get();
				}
			}
			return h.orElse(null);
		};
	}

	/**
	 * Creates a resettable memoizing supplier that caches the result of the first call and optionally allows resetting.
	 *
	 * <p>
	 * This is similar to {@link #memoize(Supplier)}, but returns a {@link ResettableSupplier} that supports
	 * clearing the cached value, forcing recomputation on the next call.
	 *
	 * <h5 class='section'>Usage:</h5>
	 * <p class='bjava'>
	 * 	ResettableSupplier&lt;String&gt; <jv>supplier</jv> = Utils.<jsm>memoizeResettable</jsm>(() -&gt; expensiveComputation());
	 *
	 * 	<jc>// First call computes and caches</jc>
	 * 	String <jv>result1</jv> = <jv>supplier</jv>.get();
	 *
	 * 	<jc>// Subsequent calls return cached value</jc>
	 * 	String <jv>result2</jv> = <jv>supplier</jv>.get();
	 *
	 * 	<jc>// Reset forces recomputation on next get()</jc>
	 * 	<jv>supplier</jv>.reset();
	 * 	String <jv>result3</jv> = <jv>supplier</jv>.get();  <jc>// Recomputes</jc>
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * The returned supplier is thread-safe for both {@link ResettableSupplier#get()} and
	 * {@link ResettableSupplier#reset()} operations. If multiple threads call get() simultaneously
	 * after a reset, the supplier may be invoked multiple times, but only one result will be cached.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jc'>{@link ResettableSupplier}
	 * </ul>
	 *
	 * @param <T> The type of value supplied.
	 * @param supplier The supplier to memoize. Must not be <jk>null</jk>.
	 * @return A thread-safe resettable memoizing wrapper around the supplier.
	 * @throws NullPointerException if supplier is <jk>null</jk>.
	 */
	public static <T> ResettableSupplier<T> memoizeResettable(Supplier<T> supplier) {
		assertArgNotNull("supplier", supplier);
		return new ResettableSupplier<>(supplier);
	}

	/**
	 * Returns <jk>null</jk> for the specified type.
	 *
	 * <p>
	 * This is a convenience method that allows you to explicitly return <jk>null</jk> with a type
	 * parameter, which can help with type inference in some contexts.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>result</jv> = n(String.<jk>class</jk>);     <jc>// null</jc>
	 * 	List&lt;String&gt; <jv>list</jv> = n(List.<jk>class</jk>);  <jc>// null</jc>
	 * </p>
	 *
	 * @param <T> The type.
	 * @param type The type class (unused, but helps with type inference).
	 * @return <jk>null</jk>.
	 */
	public static <T> T n(Class<T> type) {
		return null;
	}

	/**
	 * Null-safe not-equals check.
	 *
	 * <p>
	 * This is the inverse of {@link #eq(Object, Object)}. Returns <jk>true</jk> if the objects
	 * are not equal, handling <jk>null</jk> values gracefully.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ne(<js>"Hello"</js>, <js>"World"</js>);     <jc>// true</jc>
	 * 	ne(<js>"Hello"</js>, <js>"Hello"</js>);     <jc>// false</jc>
	 * 	ne(<jk>null</jk>, <jk>null</jk>);          <jc>// false</jc>
	 * 	ne(<js>"Hello"</js>, <jk>null</jk>);       <jc>// true</jc>
	 * </p>
	 *
	 * @param <T> The object type.
	 * @param s1 Object 1.
	 * @param s2 Object 2.
	 * @return <jk>true</jk> if the objects are not equal.
	 * @see #eq(Object, Object)
	 */
	public static <T> boolean ne(T s1, T s2) {
		return ! eq(s1, s2);
	}

	/**
	 * Tests two objects for inequality using a custom predicate, gracefully handling nulls.
	 *
	 * <p>
	 * This is the inverse of {@link #eq(Object, Object, BiPredicate)}. The predicate is only called
	 * if both objects are non-null and not the same reference.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Role <jv>r1</jv> = <jk>new</jk> Role(1, <js>"admin"</js>);
	 * 	Role <jv>r2</jv> = <jk>new</jk> Role(2, <js>"user"</js>);
	 * 	ne(<jv>r1</jv>, <jv>r2</jv>, (x,y) -&gt; x.id == y.id);  <jc>// true (different IDs)</jc>
	 * </p>
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The predicate to use for equality testing (only called if both objects are non-null and different references).
	 * @return <jk>true</jk> if the objects are not equal based on the test, or if one is <jk>null</jk> and the other is not.
	 * @see #eq(Object, Object, BiPredicate)
	 */
	public static <T,U> boolean ne(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null)
			return nn(o2);
		if (o2 == null)
			return true;
		if (o1 == o2)
			return false;
		return ! test.test(o1, o2);
	}

	/**
	 * Tests two strings for non-equality ignoring case, but gracefully handles nulls.
	 *
	 * <p>
	 * This is the inverse of {@link #eqic(String, String)}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	neic(<js>"Hello"</js>, <js>"World"</js>);     <jc>// true</jc>
	 * 	neic(<js>"Hello"</js>, <js>"hello"</js>);     <jc>// false (equal ignoring case)</jc>
	 * 	neic(<jk>null</jk>, <jk>null</jk>);          <jc>// false (both null)</jc>
	 * 	neic(<js>"Hello"</js>, <jk>null</jk>);       <jc>// true</jc>
	 * </p>
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are not equal ignoring case.
	 * @see #eqic(String, String)
	 */
	public static boolean neic(String s1, String s2) {
		return ! eqic(s1, s2);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk>.
	 *
	 * <p>
	 * Equivalent to <c><jv>o</jv> != <jk>null</jk></c>, but with a more readable method name.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.commons.utils.Utils.*;
	 *
	 * 	<jk>if</jk> (<jsm>nn</jsm>(<jv>myObject</jv>)) {
	 * 		<jc>// Do something with non-null object</jc>
	 * 	}
	 * </p>
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 */
	public static boolean nn(Object o) {
		return isNotNull(o);
	}

	/**
	 * Shortcut for calling {@link Optional#ofNullable(Object)}.
	 *
	 * <p>
	 * This is a convenience method that provides a shorter name for wrapping objects in an Optional.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Optional&lt;String&gt; <jv>opt1</jv> = opt(<js>"Hello"</js>);     <jc>// Optional.of("Hello")</jc>
	 * 	Optional&lt;String&gt; <jv>opt2</jv> = opt(<jk>null</jk>);        <jc>// Optional.empty()</jc>
	 * </p>
	 *
	 * @param <T> The object type.
	 * @param t The object to wrap in an Optional.
	 * @return An Optional containing the specified object, or empty if <jk>null</jk>.
	 * @see Optional#ofNullable(Object)
	 * @see #opte()
	 */
	public static final <T> Optional<T> opt(T t) {
		return Optional.ofNullable(t);
	}

	/**
	 * Returns an empty Optional.
	 *
	 * <p>
	 * This is a convenience method that provides a shorter name for creating an empty Optional.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Optional&lt;String&gt; <jv>empty</jv> = opte();  <jc>// Optional.empty()</jc>
	 * </p>
	 *
	 * @param <T> The object type.
	 * @return An empty Optional.
	 * @see Optional#empty()
	 * @see #opt(Object)
	 */
	public static final <T> Optional<T> opte() {
		return Optional.empty();
	}

	/**
	 * Prints all the specified lines to System.out with line numbers.
	 *
	 * <p>
	 * Each line is printed with a 4-digit line number prefix (e.g., "   1:", "   2:", etc.).
	 * This is useful for debugging or displaying formatted output.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	printLines(<jk>new</jk> String[]{<js>"First line"</js>, <js>"Second line"</js>});
	 * 	<jc>// Output:</jc>
	 * 	<jc>//    1:First line</jc>
	 * 	<jc>//    2:Second line</jc>
	 * </p>
	 *
	 * @param lines The lines to print.
	 */
	public static final void printLines(String[] lines) {
		for (var i = 0; i < lines.length; i++)
			System.out.println(String.format("%4s:" + lines[i], i + 1)); // NOSONAR - NOT DEBUG
	}

	/**
	 * Shortcut for calling {@link StringUtils#readable(Object)}.
	 *
	 * <p>Converts an arbitrary object to a readable string format suitable for debugging and testing.
	 *
	 * @param o The object to convert to readable format. Can be <jk>null</jk>.
	 * @return A readable string representation of the object, or <jk>null</jk> if the input was <jk>null</jk>.
	 * @see StringUtils#readable(Object)
	 */
	public static String r(Object o) {
		return readable(o);
	}

	// TODO: Look for cases of getClass().getName() and getClass().getSimpleName() in the code and replace with cn() and scn().
	// These utility methods provide cleaner, more concise syntax and null-safe handling.
	// Search patterns:
	//   - Replace: getClass().getName() -> cn(this) or cn(object)
	//   - Replace: getClass().getSimpleName() -> scn(this) or scn(object)
	//   - Replace: obj.getClass().getName() -> cn(obj)
	//   - Replace: obj.getClass().getSimpleName() -> scn(obj)

	/**
	 * Shortcut for converting an object to a string.
	 *
	 * <p>
	 * This is a null-safe string conversion. Returns <jk>null</jk> if the object is <jk>null</jk>,
	 * otherwise returns the result of calling {@link Object#toString()} on the object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	s(<js>"Hello"</js>);     <jc>// "Hello"</jc>
	 * 	s(123);                 <jc>// "123"</jc>
	 * 	s(<jk>null</jk>);        <jc>// null</jc>
	 * </p>
	 *
	 * @param val The object to convert.
	 * @return The string representation of the object, or <jk>null</jk> if the object is <jk>null</jk>.
	 * @see Object#toString()
	 * @see #emptyIfNull(Object)
	 */
	public static String s(Object val) {
		return val == null ? null : val.toString();
	}

	/**
	 * Runs a snippet of code and encapsulates any throwable inside a {@link RuntimeException}.
	 *
	 * @param snippet The snippet of code to run.
	 */
	public static void safe(Snippet snippet) {
		try {
			snippet.run();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw ThrowableUtils.toRex(t);
		}
	}

	/**
	 * Used to wrap code that returns a value but throws an exception.
	 * Useful in cases where you're trying to execute code in a fluent method call
	 * or are trying to eliminate untestable catch blocks in code.
	 *
	 * @param <T> The return type.
	 * @param s The supplier that may throw an exception.
	 * @return The result of the supplier execution.
	 */
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw rex(e);
		}
	}

	/**
	 * Executes a supplier that may throw an exception and returns an Optional.
	 *
	 * <p>
	 * If the supplier executes successfully, returns {@link Optional#of(Object)} with the result.
	 * If the supplier throws any exception, returns {@link Optional#empty()}.
	 *
	 * <p>
	 * This is useful for operations that may fail but you want to handle the failure
	 * gracefully by returning an empty Optional instead of throwing an exception.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Check if AccessibleObject.isAccessible() method exists (Java 9+)</jc>
	 * 	<jk>boolean</jk> <jv>isAccessible</jv> = <jsm>safeOpt</jsm>(() -&gt;
	 * 		(<jk>boolean</jk>)AccessibleObject.<jk>class</jk>.getMethod(<js>"isAccessible"</js>).invoke(<jv>obj</jv>)
	 * 	).orElse(<jk>false</jk>);
	 * </p>
	 *
	 * @param <T> The return type.
	 * @param s The supplier that may throw an exception.
	 * @return An Optional containing the result if successful, or empty if an exception was thrown.
	 * @see #safe(ThrowingSupplier)
	 * @see #opt(Object)
	 */
	public static <T> Optional<T> safeOpt(ThrowingSupplier<T> s) {
		try {
			return Optional.of(s.get());
		} catch (@SuppressWarnings("unused") Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Allows you to wrap a supplier that throws an exception so that it can be used in a fluent interface.
	 *
	 * @param <T> The supplier type.
	 * @param supplier The supplier throwing an exception.
	 * @return The supplied result.
	 * @throws RuntimeException if supplier threw an exception.
	 */
	public static <T> T safeSupplier(ThrowableUtils.SupplierWithThrowable<T> supplier) {
		try {
			return supplier.get();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw toRex(t);
		}
	}

	/**
	 * Helper method for creating StringBuilder objects.
	 *
	 * @param value The string value to wrap in a StringBuilder.
	 * @return A new StringBuilder containing the specified value.
	 */
	public static StringBuilder sb(String value) {
		return new StringBuilder(value);
	}

	/**
	 * Shortcut for calling {@link StringUtils#stringSupplier(Supplier)}.
	 *
	 * @param s The supplier.
	 * @return A string supplier that calls {@link #r(Object)} on the supplied value.
	 */
	public static Supplier<String> ss(Supplier<?> s) {
		return stringSupplier(s);
	}

	/**
	 * Convenience method for calling {@link StringUtils#upperCase(String)}.
	 *
	 * <p>
	 * Converts the string to uppercase if not null.
	 *
	 * @param value The string to convert.
	 * @return The uppercase string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public static String uc(String value) {
		return StringUtils.upperCase(value);
	}

	/**
	 * If the specified object is a {@link Supplier} or {@link Value}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		if (o instanceof Supplier<?> o2)
			o = unwrap(o2.get());
		if (o instanceof Value<?> o2)
			o = unwrap(o2.get());
		if (o instanceof Optional<?> o2)
			o = unwrap(o2.orElse(null));
		return o;
	}

	/**
	 * Converts a property name to an environment variable name.
	 *
	 * @param name The property name to convert.
	 * @return The environment variable name (uppercase with dots replaced by underscores).
	 */
	private static String envName(String name) {
		return PROPERTY_TO_ENV.computeIfAbsent(name, x -> x.toUpperCase().replace(".", "_"));
	}

	/**
	 * Converts a string to the specified type using registered conversion functions.
	 *
	 * @param <T> The target type.
	 * @param s The string to convert.
	 * @param def The default value (used to determine the target type).
	 * @return The converted value, or <jk>null</jk> if the string or default is <jk>null</jk>.
	 * @throws RuntimeException If the type is not supported for conversion.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T toType(String s, T def) {
		if (s == null || def == null)
			return null;
		var c = (Class<T>)def.getClass();
		if (c == String.class)
			return (T)s;
		if (c.isEnum())
			return (T)Enum.valueOf((Class<? extends Enum>)c, s);
		var f = (Function<String,T>)ENV_FUNCTIONS.get(c);
		if (f == null)
			throw rex("Invalid env type: {0}", c);
		return f.apply(s);
	}

	/** Constructor - This class is meant to be subclasses. */
	protected Utils() {}
}