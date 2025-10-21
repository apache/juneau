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
package org.apache.juneau.common.utils;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.utils.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.text.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.common.collections.*;
import org.apache.juneau.common.function.*;

/**
 * Common utility methods.
 *
 * <p>This class contains various static utility methods for working with collections, strings, objects, and other common operations.
 */
public class Utils {

	private static final Map<Class<?>,Function<String,?>> ENV_FUNCTIONS = new IdentityHashMap<>();

	static {
		ENV_FUNCTIONS.put(Boolean.class, Boolean::valueOf);
		ENV_FUNCTIONS.put(Charset.class, Charset::forName);
	}

	private static final ConcurrentHashMap<String,String> PROPERTY_TO_ENV = new ConcurrentHashMap<>();

	/**
	 * Creates an array of objects.
	 *
	 * @param <T> The component type of the array.
	 * @param x The objects to place in the array.
	 * @return A new array containing the specified objects.
	 */
	@SafeVarargs
	public static <T> T[] a(T...x) {
		return x;
	}

	/**
	 * Traverses all elements in the specified object and accumulates them into a list.
	 *
	 * @param <T> The element type.
	 * @param o The object to traverse.
	 * @return A list containing all accumulated elements.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> accumulate(Object o) {
		var l = list();
		traverse(o, l::add);
		return (List<T>)l;
	}

	/**
	 * Shortcut for creating an unmodifiable list out of an array of values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the list.
	 * @return An unmodifiable list containing the specified values, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> List<T> alist(T...values) {  // NOSONAR
		return values == null ? null : Arrays.asList(values);
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * @param <E> The element type.
	 * @param value The collection to convert.
	 * @param componentType The component type of the array.
	 * @return A new array.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] array(Collection<E> value, Class<E> componentType) {
		if (value == null)
			return null;
		E[] array = (E[])Array.newInstance(componentType, value.size());
		return value.toArray(array);
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
			return null;  // NOSONAR
		}

		assertArg(isArray(array), "Input must be an array but was {0}", array.getClass().getName());

		var componentType = array.getClass().getComponentType();
		var length = Array.getLength(array);
		var result = new ArrayList<>(length);

		// Handle primitive arrays specifically for better performance
		if (componentType.isPrimitive()) {
			if (componentType == int.class) {
				var arr = (int[])array;
				for (int value : arr) {
					result.add(value);
				}
			} else if (componentType == long.class) {
				var arr = (long[])array;
				for (long value : arr) {
					result.add(value);
				}
			} else if (componentType == double.class) {
				var arr = (double[])array;
				for (double value : arr) {
					result.add(value);
				}
			} else if (componentType == float.class) {
				var arr = (float[])array;
				for (float value : arr) {
					result.add(value);
				}
			} else if (componentType == boolean.class) {
				var arr = (boolean[])array;
				for (boolean value : arr) {
					result.add(value);
				}
			} else if (componentType == byte.class) {
				var arr = (byte[])array;
				for (byte value : arr) {
					result.add(value);
				}
			} else if (componentType == char.class) {
				var arr = (char[])array;
				for (char value : arr) {
					result.add(value);
				}
			} else if (componentType == short.class) {
				var arr = (short[])array;
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

	/**
	 * Throws an {@link IllegalArgumentException} if the specified expression is <jk>false</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.internal.ArgUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(List&lt;String&gt; <jv>foo</jv>) {
	 *		<jsm>assertArg</jsm>(<jv>foo</jv> != <jk>null</jk> &amp;&amp; ! <jv>foo</jv>.isEmpty(), <js>"'foo' cannot be null or empty."</js>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param expression The boolean expression to check.
	 * @param msg The exception message.
	 * @param args The exception message args.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final void assertArg(boolean expression, String msg, Object...args) throws IllegalArgumentException {
		if (! expression)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified argument is <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.internal.ArgUtils.*;
	 *
	 *	<jk>public</jk> String setFoo(String <jv>foo</jv>) {
	 *		<jsm>assertArgNotNull</jsm>(<js>"foo"</js>, <jv>foo</jv>);
	 *		...
	 *	}
	 * </p>
	 *
	 * @param <T> The argument data type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same argument.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static final <T> T assertArgNotNull(String name, T o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified string is <jk>null</jk> or blank.
	 *
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same object.
	 * @throws IllegalArgumentException Thrown if the specified string is <jk>null</jk> or blank.
	 */
	@SuppressWarnings("null")
	public static final String assertArgNotNullOrBlank(String name, String o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		assertArg(! o.isBlank(), "Argument ''{0}'' cannot be blank.", name);
		return o;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified value doesn't have all subclasses of the specified type.
	 *
	 * @param <E> The element type.
	 * @param name The argument name.
	 * @param type The expected parent class.
	 * @param value The array value being checked.
	 * @return The value cast to the specified array type.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	@SuppressWarnings("unchecked")
	public static final <E> Class<E>[] assertClassArrayArgIsType(String name, Class<E> type, Class<?>[] value) throws IllegalArgumentException {
		for (var i = 0; i < value.length; i++)
			if (! type.isAssignableFrom(value[i]))
				throw new IllegalArgumentException("Arg " + name + " did not have arg of type " + type.getName() + " at index " + i + ": " + value[i].getName());
		return (Class<E>[])value;
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified varargs array or any of its elements are <jk>null</jk>.
	 *
	 * @param <T> The element type.
	 * @param name The argument name.
	 * @param o The object to check.
	 * @return The same object.
	 * @throws IllegalArgumentException Thrown if the specified varargs array or any of its elements are <jk>null</jk>.
	 */
	@SuppressWarnings("null")
	public static final <T> T[] assertVarargsNotNull(String name, T[] o) throws IllegalArgumentException {
		assertArg(o != null, "Argument ''{0}'' cannot be null.", name);
		for (int i = 0; i < o.length; i++)
			assertArg(o[i] != null, "Argument ''{0}'' parameter {1} cannot be null.", name, i);
		return o;
	}

	/**
	 * Casts an object to a specific type if it's an instance of that type.
	 *
	 * @param <T> The type to cast to.
	 * @param c The type to cast to.
	 * @param o The object to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't the specified type.
	 */
	public static <T> T cast(Class<T> c, Object o) {
		return o != null && c.isInstance(o) ? c.cast(o) : null;
	}

	/**
	 * If the specified object is an instance of the specified class, casts it to that type.
	 *
	 * @param <T> The class to cast to.
	 * @param o The object to cast.
	 * @param c The class to cast to.
	 * @return The cast object, or <jk>null</jk> if the object wasn't an instance of the specified class.
	 */
	public static <T> T castOrNull(Object o, Class<T> c) {
		if (c.isInstance(o))
			return c.cast(o);
		return null;
	}

	/**
	 * Gets the fully qualified class name of the specified object.
	 *
	 * @param o The object to get the class name for.
	 * @return The fully qualified class name, or <jk>null</jk> if the object is <jk>null</jk>.
	 */
	public static String classNameOf(Object o) {
		return o == null ? null : o.getClass().getName();
	}

	/**
	 * Compares two objects for equality.
	 *
	 * <p>
	 * Nulls are always considered less-than unless both are null.
	 *
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return
	 * 	<c>-1</c>, <c>0</c>, or <c>1</c> if <c>o1</c> is less-than, equal, or greater-than <c>o2</c>.
	 *	<br><c>0</c> if objects are not of the same type or do not implement the {@link Comparable} interface.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int compare(Object o1, Object o2) {
		if (o1 == null) {
			if (o2 == null)
				return 0;
			return -1;
		} else if (o2 == null) {
			return 1;
		}

		if (o1.getClass() == o2.getClass() && o1 instanceof Comparable)
			return ((Comparable)o1).compareTo(o2);

		return 0;
	}

	/**
	 * Creates an empty array of the specified type.
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
	 * Shortcut for creating an empty list of the specified type.
	 *
	 * @param <T> The element type.
	 * @param type The element type class.
	 * @return An empty list.
	 */
	public static <T> List<T> elist(Class<T> type) {
		return Collections.emptyList();
	}

	/**
	 * Shortcut for creating an empty map of the specified types.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type class.
	 * @param valueType The value type class.
	 * @return An empty unmodifiable map.
	 */
	public static <K,V> Map<K,V> emap(Class<K> keyType, Class<V> valueType) {
		return Collections.emptyMap();
	}

	/**
	 * Returns the specified string, or blank if that string is null.
	 *
	 * @param value The value to convert to a string.
	 * @return The string representation of the value, or an empty string if <jk>null</jk>.
	 */
	public static String emptyIfNull(Object value) {
		return value == null ? "" : value.toString();
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param name The property name.
	 * @return The value if found.
	 */
	public static Optional<String> env(String name) {
		var s = System.getProperty(name);
		if (s == null)
			s = System.getenv(envName(name));
		return opt(s);
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * <p>
	 * First looks in system properties.  Then converts the name to env-safe and looks in the system environment.
	 * Then returns the default if it can't be found.
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value if not found.
	 * @return The default value.
	 */
	public static <T> T env(String name, T def) {
		return env(name).map(x -> toType(x, def)).orElse(def);
	}

	/**
	 * Tests two strings for equality, but gracefully handles nulls.
	 *
	 * @param caseInsensitive Use case-insensitive matching.
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean eq(boolean caseInsensitive, String s1, String s2) {
		return caseInsensitive ? eqic(s1, s2) : eq(s1, s2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls and arrays.
	 *
	 * @param <T> The value types.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @return <jk>true</jk> if both objects are equal based on the {@link Object#equals(Object)} method.
	 */
	public static <T> boolean eq(T o1, T o2) {
		if (isArray(o1) && isArray(o2)) {
			int l1 = Array.getLength(o1), l2 = Array.getLength(o2);
			if (l1 != l2)
				return false;
			for (int i = 0; i < l1; i++)
				if (! eq(Array.get(o1, i), Array.get(o2, i)))
					return false;
			return true;
		}
		return Objects.equals(o1, o2);
	}

	/**
	 * Tests two objects for equality, gracefully handling nulls.
	 *
	 * Allows you to simplify object comparison without sacrificing efficiency.
	 *
	 * Example:
	 * <code>
	 * 	public boolean equals(Object o)
	 * 		return eq(this, (Role)o, (x,y)-&gt;eq(x.id,y.id) &amp;&amp; eq(x.name,y.name) &amp;&amp; eq(x.created,y.created) &amp;&amp; eq(x.createdBy,y.createdBy));
	 * 	}
	 * </code>
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>true</jk> if both objects are equal based on the test.
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
	 * Tests two strings for case-insensitive equality, but gracefully handles nulls.
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are equal.
	 */
	public static boolean eqic(String s1, String s2) {
		if (s1 == null)
			return s2 == null;
		if (s2 == null)
			return false;
		return s1.equalsIgnoreCase(s2);
	}

	/**
	 * Same as MessageFormat.format().
	 *
	 * @param pattern The message pattern.
	 * @param args The arguments to substitute into the pattern.
	 * @return The formatted string.
	 */
	public static String f(String pattern, Object...args) {
		if (args.length == 0)
			return pattern;
		return MessageFormat.format(pattern, args);
	}

	/**
	 * Returns the first non-null value in the specified array
	 *
	 * @param <T> The value types.
	 * @param t The values to check.
	 * @return The first non-null value, or <jk>null</jk> if the array is null or empty or contains only <jk>null</jk> values.
	 */
	@SafeVarargs
	public static <T> T firstNonNull(T...t) {
		if (t != null)
			for (T tt : t)
				if (tt != null)
					return tt;
		return null;
	}

	/**
	 * Creates a formatted string supplier with message arguments for lazy evaluation.
	 *
	 * <p>This method returns a {@link Supplier} that formats the string pattern with the provided arguments
	 * only when the supplier's {@code get()} method is called. This is useful for expensive string formatting
	 * operations that may not always be needed, such as error messages in assertions.</p>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Lazy evaluation - string is only formatted if assertion fails</jc>
	 * 	assertTrue(condition, fms(<js>"Expected {0} but got {1}"</js>, expected, actual));
	 *
	 * 	<jc>// Can be used anywhere a Supplier&lt;String&gt; is expected</jc>
	 * 	Supplier&lt;String&gt; <jv>messageSupplier</jv> = fms(<js>"Processing item {0} of {1}"</js>, i, total);
	 * </p>
	 *
	 * @param pattern The message pattern using <js>{0}</js>, <js>{1}</js>, etc. placeholders.
	 * @param args The arguments to substitute into the pattern placeholders.
	 * @return A {@link Supplier} that will format the string when {@code get()} is called.
	 * @see StringUtils#format(String, Object...)
	 */
	public static Supplier<String> fms(String pattern, Object...args) {
		return () -> StringUtils.format(pattern, args);
	}

	/**
	 * Shortcut for calling {@link Objects#hash(Object...)}.
	 *
	 * @param values The values to hash.
	 * @return A hash code value for the given values.
	 */
	public static final int hash(Object...values) {
		return Objects.hash(values);
	}

	/**
	 * Checks if the specified object is an array.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object is not <jk>null</jk> and is an array.
	 */
	public static boolean isArray(Object o) {
		return o != null && o.getClass().isArray();
	}

	/**
	 * Returns <jk>true</jk> if the specified object can be converted to a list.
	 *
	 * <p>
	 * The following types are considered convertible:
	 * <ul>
	 * 	<li>Collections
	 * 	<li>Iterables
	 * 	<li>Iterators
	 * 	<li>Enumerations
	 * 	<li>Streams
	 * 	<li>Maps
	 * 	<li>Optional
	 * 	<li>Arrays
	 * </ul>
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the object can be converted to a list.
	 */
	public static boolean isConvertibleToList(Object o) {
		return o != null && (o instanceof Collection || o instanceof Iterable || o instanceof Iterator || o instanceof Enumeration || o instanceof Stream || o instanceof Map || o instanceof Optional
			|| isArray(o));
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
	 * Returns <jk>true</jk> if string is <jk>null</jk> or empty.
	 *
	 * @param o The string to check.
	 * @return <jk>true</jk> if string is <jk>null</jk> or empty.
	 */
	public static boolean isEmpty(String o) {
		return o == null || o.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if specified string is <jk>null</jk> or empty or consists of only blanks.
	 *
	 * @param s The string to check.
	 * @return <jk>true</jk> if specified string is <jk>null</jk> or empty or consists of only blanks.
	 */
	public static boolean isEmptyOrBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 *
	 * Works on any of the following data types:  String, CharSequence, Collection, Map, array.
	 * All other types are stringified and then checked as a String.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(Object value) {
		if (value == null)
			return false;
		if (value instanceof CharSequence x)
			return ! x.isEmpty();
		if (value instanceof Collection<?> x)
			return ! x.isEmpty();
		if (value instanceof Map<?,?> x)
			return ! x.isEmpty();
		if (isArray(value))
			return Array.getLength(value) > 0;
		return isNotEmpty(s(value));
	}

	/**
	 * Returns <jk>true</jk> if string is not <jk>null</jk> and not empty.
	 *
	 * @param o The string to check.
	 * @return <jk>true</jk> if string is not <jk>null</jk> and not empty.
	 */
	public static boolean isNotEmpty(String o) {
		return ! isEmpty(o);
	}

	/**
	 * Returns <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 *
	 * @param <T> The value types.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified number is not <jk>null</jk> and not <c>-1</c>.
	 */
	public static <T extends Number> boolean isNotMinusOne(T value) {
		return value != null && value.intValue() != -1;
	}

	/**
	 * Returns <jk>true</jk> if the specified object is not <jk>null</jk>.
	 *
	 * @param <T> The value type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified object is not <jk>null</jk>.
	 */
	public static <T> boolean isNotNull(T value) {
		return value != null;
	}

	/**
	 * Returns <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 *
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified boolean is not <jk>null</jk> and is <jk>true</jk>.
	 */
	public static boolean isTrue(Boolean value) {
		return value != null && value;
	}

	/**
	 * Shortcut for creating a modifiable list out of an array of values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the list.
	 * @return A modifiable list containing the specified values.
	 */
	@SafeVarargs
	public static <T> List<T> list(T...values) {  // NOSONAR
		return new ArrayList<>(Arrays.asList(values));
	}

	/**
	 * Convenience method for creating an {@link ArrayList} of the specified size.
	 *
	 * @param <E> The element type.
	 * @param size The initial size of the list.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> listOfSize(int size) {
		return new ArrayList<>(size);
	}

	/**
	 * Shortcut for creating a modifiable map out of an array of key-value pairs.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param values The key-value pairs (alternating keys and values).
	 * @return A modifiable LinkedHashMap containing the specified key-value pairs.
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <K,V> LinkedHashMap<K,V> map(Object...values) {  // NOSONAR
		var m = new LinkedHashMap<K,V>();
		for (var i = 0; i < values.length; i += 2) {
			m.put((K)values[i], (V)values[i + 1]);
		}
		return m;
	}

	/**
	 * Returns <jk>null</jk> for the specified type.
	 *
	 * @param <T> The type.
	 * @param type The type class.
	 * @return <jk>null</jk>.
	 */
	public static <T> T n(Class<T> type) {
		return null;
	}

	/**
	 * Returns <jk>null</jk> for the specified array type.
	 *
	 * @param <T> The component type.
	 * @param type The component type class.
	 * @return <jk>null</jk>.
	 */
	public static <T> T[] na(Class<T> type) {
		return null;
	}

	/**
	 * Null-safe not-equals check.
	 *
	 * @param <T> The object type.
	 * @param s1 Object 1.
	 * @param s2 Object 2.
	 * @return <jk>true</jk> if the objects are not equal.
	 */
	public static <T> boolean ne(T s1, T s2) {
		return ! eq(s1, s2);
	}

	/**
	 * Tests two objects for inequality, gracefully handling nulls.
	 *
	 * @param <T> Object 1 type.
	 * @param <U> Object 2 type.
	 * @param o1 Object 1.
	 * @param o2 Object 2.
	 * @param test The test to use for equality.
	 * @return <jk>false</jk> if both objects are equal based on the test.
	 */
	public static <T,U> boolean ne(T o1, U o2, BiPredicate<T,U> test) {
		if (o1 == null)
			return o2 != null;
		if (o2 == null)
			return true;
		if (o1 == o2)
			return false;
		return ! test.test(o1, o2);
	}

	/**
	 * Tests two strings for non-equality ignoring case, but gracefully handles nulls.
	 *
	 * @param s1 String 1.
	 * @param s2 String 2.
	 * @return <jk>true</jk> if the strings are not equal ignoring case.
	 */
	public static boolean neic(String s1, String s2) {
		return ! eqic(s1, s2);
	}

	/**
	 * Returns a null list.
	 *
	 * @param <T> The element type.
	 * @param type The element type class.
	 * @return <jk>null</jk>.
	 */
	public static <T> List<T> nlist(Class<T> type) {
		return null;
	}

	/**
	 * Returns a null map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type class.
	 * @param valueType The value type class.
	 * @return <jk>null</jk>.
	 */
	public static <K,V> Map<K,V> nmap(Class<K> keyType, Class<V> valueType) {
		return null;
	}

	/**
	 * Shortcut for calling {@link Optional#ofNullable(Object)}.
	 *
	 * @param <T> The object type.
	 * @param t The object to wrap in an Optional.
	 * @return An Optional containing the specified object, or empty if <jk>null</jk>.
	 */
	public static final <T> Optional<T> opt(T t) {
		return Optional.ofNullable(t);
	}

	/**
	 * Returns an empty Optional.
	 *
	 * @param <T> The object type.
	 * @return An empty Optional.
	 */
	public static final <T> Optional<T> opte() {
		return Optional.empty();
	}

	/**
	 * Prints all the specified lines to System.out.
	 *
	 * @param lines The lines to print.
	 */
	public static final void printLines(String[] lines) {
		for (var i = 0; i < lines.length; i++)
			System.out.println(String.format("%4s:" + lines[i], i + 1)); // NOSONAR - NOT DEBUG
	}

	/**
	 * Converts an arbitrary object to a readable string format suitable for debugging and testing.
	 *
	 * <p>This method provides intelligent formatting for various Java types, recursively processing
	 * nested structures to create human-readable representations. It's extensively used throughout
	 * the Juneau framework for test assertions and debugging output.</p>
	 *
	  * <h5 class='section'>Type-Specific Formatting:</h5>
	 * <ul>
	 * 	<li><b>null:</b> Returns <js>null</js></li>
	 * 	<li><b>Optional:</b> Recursively formats the contained value (or <js>null</js> if empty)</li>
	 * 	<li><b>Collections:</b> Formats as <js>"[item1,item2,item3]"</js> with comma-separated elements</li>
	 * 	<li><b>Maps:</b> Formats as <js>"{key1=value1,key2=value2}"</js> with comma-separated entries</li>
	 * 	<li><b>Map.Entry:</b> Formats as <js>"key=value"</js></li>
	 * 	<li><b>Arrays:</b> Converts to list format <js>"[item1,item2,item3]"</js></li>
	 * 	<li><b>Iterables/Iterators/Enumerations:</b> Converts to list and formats recursively</li>
	 * 	<li><b>GregorianCalendar:</b> Formats as ISO instant timestamp</li>
	 * 	<li><b>Date:</b> Formats as ISO instant string (e.g., <js>"2023-12-25T10:30:00Z"</js>)</li>
	 * 	<li><b>InputStream:</b> Converts to hexadecimal representation</li>
	 * 	<li><b>Reader:</b> Reads content and returns as string</li>
	 * 	<li><b>File:</b> Reads file content and returns as string</li>
	 * 	<li><b>byte[]:</b> Converts to hexadecimal representation</li>
	 * 	<li><b>Enum:</b> Returns the enum name via {@link Enum#name()}</li>
	 * 	<li><b>All other types:</b> Uses {@link Object#toString()}</li>
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Collections</jc>
	 * 	r(List.of("a", "b", "c")) <jc>// Returns: "[a,b,c]"</jc>
	 * 	r(Set.of(1, 2, 3)) <jc>// Returns: "[1,2,3]" (order may vary)</jc>
	 *
	 * 	<jc>// Maps</jc>
	 * 	r(Map.of("foo", "bar", "baz", 123)) <jc>// Returns: "{foo=bar,baz=123}"</jc>
	 *
	 * 	<jc>// Arrays</jc>
	 * 	r(new int[]{1, 2, 3}) <jc>// Returns: "[1,2,3]"</jc>
	 * 	r(new String[]{"a", "b"}) <jc>// Returns: "[a,b]"</jc>
	 *
	 * 	<jc>// Nested structures</jc>
	 * 	r(List.of(Map.of("x", 1), Set.of("a", "b"))) <jc>// Returns: "[{x=1},[a,b]]"</jc>
	 *
	  * 	<jc>// Special types</jc>
	 * 	r(Optional.of("test")) <jc>// Returns: "test"</jc>
	 * 	r(Optional.empty()) <jc>// Returns: null</jc>
	 * 	r(new Date(1640995200000L)) <jc>// Returns: "2022-01-01T00:00:00Z"</jc>
	 * 	r(MyEnum.FOO) <jc>// Returns: "FOO"</jc>
	 * </p>
	 *
	 * <h5 class='section'>Recursive Processing:</h5>
	 * <p>The method recursively processes nested structures, so complex objects containing
	 * collections, maps, and arrays are fully flattened into readable format. This makes it
	 * ideal for test assertions where you need to compare complex object structures.</p>
	 *
	 * <h5 class='section'>Error Handling:</h5>
	 * <p>IO operations (reading files, streams) are wrapped in safe() calls, converting
	 * any exceptions to RuntimeExceptions. Binary data (InputStreams, byte arrays) is
	 * converted to hexadecimal representation for readability.</p>
	 *
	 * @param o The object to convert to readable format. Can be <jk>null</jk>.
	 * @return A readable string representation of the object, or <jk>null</jk> if the input was <jk>null</jk>.
	 * @see #safe(ThrowingSupplier)
	 */
	public static String r(Object o) {
		if (o == null)
			return null;
		if (o instanceof Optional<?> o2)
			return r(o2.orElse(null));
		if (o instanceof Collection<?> o2)
			return o2.stream().map(Utils::r).collect(joining(",", "[", "]"));
		if (o instanceof Map<?,?> o2)
			return o2.entrySet().stream().map(Utils::r).collect(joining(",", "{", "}"));
		if (o instanceof Map.Entry<?,?> o2)
			return r(o2.getKey()) + '=' + r(o2.getValue());
		if (o instanceof Iterable<?> o2)
			return r(toList(o2));
		if (o instanceof Iterator<?> o2)
			return r(toList(o2));
		if (o instanceof Enumeration<?> o2)
			return r(toList(o2));
		if (o instanceof GregorianCalendar o2)
			return o2.toZonedDateTime().format(DateTimeFormatter.ISO_INSTANT);
		if (o instanceof Date o2)
			return o2.toInstant().toString();
		if (o instanceof InputStream o2)
			return toHex(o2);
		if (o instanceof Reader o2)
			return safe(() -> IOUtils.read(o2));
		if (o instanceof File o2)
			return safe(() -> IOUtils.read(o2));
		if (o instanceof byte[] o2)
			return toHex(o2);
		if (o instanceof Enum o2)
			return o2.name();
		if (o instanceof Class o2)
			return o2.getSimpleName();
		if (o instanceof Executable o2) {
			var sb = new StringBuilder(64);
			sb.append(o2 instanceof Constructor ? o2.getDeclaringClass().getSimpleName() : o2.getName()).append('(');
			Class<?>[] pt = o2.getParameterTypes();
			for (int i = 0; i < pt.length; i++) {
				if (i > 0)
					sb.append(',');
				sb.append(pt[i].getSimpleName());
			}
			sb.append(')');
			return sb.toString();
		}
		if (isArray(o)) {
			var l = list();
			for (var i = 0; i < Array.getLength(o); i++) {
				l.add(Array.get(o, i));
			}
			return r(l);
		}
		return o.toString();
	}

	/**
	 * Shortcut for converting an object to a string.
	 *
	 * @param val The object to convert.
	 * @return The string representation of the object, or <jk>null</jk> if the object is <jk>null</jk>.
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
			throw ThrowableUtils.asRuntimeException(t);
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
			throw new RuntimeException(e);
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
			throw ThrowableUtils.asRuntimeException(t);
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
	 * Shortcut for creating a modifiable set out of an array of values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the set.
	 * @return A modifiable LinkedHashSet containing the specified values.
	 */
	@SafeVarargs
	public static <T> LinkedHashSet<T> set(T...values) {  // NOSONAR
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/**
	 * Gets the simple class name of the specified object.
	 *
	 * @param o The object to get the simple class name for.
	 * @return The simple class name, or <jk>null</jk> if the object is <jk>null</jk>.
	 */
	public static String simpleClassNameOf(Object o) {
		return o == null ? null : o.getClass().getSimpleName();
	}

	/**
	 * Converts various collection-like objects to a {@link List}.
	 *
	 * <p>This utility method enables testing of any collection-like object by converting it to a List that can be
	 * passed to methods such as TestUtils.assertList().</p>
	 *
	 * <h5 class='section'>Supported Input Types:</h5>
	 * <ul>
	 * 	<li><b>List:</b> Returns the input unchanged</li>
	 * 	<li><b>Iterable:</b> Any collection, set, queue, etc. (converted to List preserving order)</li>
	 * 	<li><b>Iterator:</b> Converts iterator contents to List</li>
	 * 	<li><b>Enumeration:</b> Converts enumeration contents to List</li>
	 * 	<li><b>Stream:</b> Converts stream contents to List (stream is consumed)</li>
	 * 	<li><b>Map:</b> Converts map entries to List of Map.Entry objects</li>
	 * 	<li><b>Array:</b> Converts any array type (including primitive arrays) to List</li>
	 * </ul>
	 *
	 * <h5 class='section'>Usage Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Test a Set</jc>
	 * 	Set&lt;String&gt; <jv>mySet</jv> = Set.of(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 * 	assertList(toList(<jv>mySet</jv>), <js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 *
	 * 	<jc>// Test an array</jc>
	 * 	String[] <jv>myArray</jv> = {<js>"x"</js>, <js>"y"</js>, <js>"z"</js>};
	 * 	assertList(toList(<jv>myArray</jv>), <js>"x"</js>, <js>"y"</js>, <js>"z"</js>);
	 *
	 * 	<jc>// Test a primitive array</jc>
	 * 	<jk>int</jk>[] <jv>numbers</jv> = {1, 2, 3};
	 * 	assertList(toList(<jv>numbers</jv>), <js>"1"</js>, <js>"2"</js>, <js>"3"</js>);
	 *
	 * 	<jc>// Test a Stream</jc>
	 * 	Stream&lt;String&gt; <jv>myStream</jv> = Stream.of(<js>"foo"</js>, <js>"bar"</js>);
	 * 	assertList(toList(<jv>myStream</jv>), <js>"foo"</js>, <js>"bar"</js>);
	 *
	 * 	<jc>// Test a Map (converted to entries)</jc>
	 * 	Map&lt;String,Integer&gt; <jv>myMap</jv> = Map.of(<js>"a"</js>, 1, <js>"b"</js>, 2);
	 * 	assertList(toList(<jv>myMap</jv>), <js>"a=1"</js>, <js>"b=2"</js>);
	 *
	 * 	<jc>// Test any Iterable collection</jc>
	 * 	Queue&lt;String&gt; <jv>myQueue</jv> = new LinkedList&lt;&gt;(List.of(<js>"first"</js>, <js>"second"</js>));
	 * 	assertList(toList(<jv>myQueue</jv>), <js>"first"</js>, <js>"second"</js>);
	 * </p>
	 *
	 * <h5 class='section'>Integration with Testing:</h5>
	 * <p>This method is specifically designed to work with testing frameworks to provide
	 * a unified testing approach for all collection-like types. Instead of having separate assertion methods
	 * for arrays, sets, and other collections, you can convert them all to Lists and use standard
	 * list assertion methods.</p>
	 *
	 * @param o The object to convert to a List. Must not be null and must be a supported collection-like type.
	 * @return A {@link List} containing the elements from the input object.
	 * @throws IllegalArgumentException if the input object cannot be converted to a List.
	 * @see #arrayToList(Object)
	 */
	public static final List<?> toList(Object o) {  // NOSONAR
		assertArgNotNull("o", o);
		if (o instanceof List<?> o2)
			return o2;
		if (o instanceof Iterable<?> o2)
			return StreamSupport.stream(o2.spliterator(), false).toList();
		if (o instanceof Iterator<?> o2)
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(o2, 0), false).toList();
		if (o instanceof Enumeration<?> o2)
			return Collections.list(o2);
		if (o instanceof Stream<?> o2)
			return o2.toList();
		if (o instanceof Map<?,?> o2)
			return toList(o2.entrySet());
		if (o instanceof Optional<?> o2)
			return o2.isEmpty() ? Collections.emptyList() : Collections.singletonList(o2.get());
		if (isArray(o))
			return arrayToList(o);
		throw ThrowableUtils.runtimeException("Could not convert object of type {0} to a list", classNameOf(o));
	}

	/**
	 * Converts an array to a stream of objects.
	 * @param array The array to convert.
	 * @return A new stream.
	 */
	public static Stream<Object> toStream(Object array) {
		assertArg(isArray(array), "Arg was not an array.  Type: {0}", array.getClass().getName());
		var length = Array.getLength(array);
		return IntStream.range(0, length).mapToObj(i -> Array.get(array, i));
	}

	/**
	 * Traverses all elements in the specified object and executes a consumer for it.
	 *
	 * @param <T> The element type.
	 * @param o The object to traverse.
	 * @param c The consumer of the objects.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void traverse(Object o, Consumer<T> c) {
		if (o == null)
			return;
		if (o instanceof Iterable<?> o2)
			o2.forEach(x -> traverse(x, c));
		else if (o instanceof Stream<?> o2)
			o2.forEach(x -> traverse(x, c));
		else if (isArray(o))
			toStream(o).forEach(x -> traverse(x, c));
		else
			c.accept((T)o);
	}

	/**
	 * Creates an unmodifiable view of the specified list.
	 *
	 * <p>This is a null-safe wrapper around {@link Collections#unmodifiableList(List)}.</p>
	 *
	 * @param <T> The element type.
	 * @param value The list to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the list, or null if the input was null.
	 */
	public static <T> List<T> u(List<? extends T> value) {
		return value == null ? null : Collections.unmodifiableList(value);
	}

	/**
	 * Creates an unmodifiable view of the specified map.
	 *
	 * <p>This is a null-safe wrapper around {@link Collections#unmodifiableMap(Map)}.</p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The map to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the map, or null if the input was null.
	 */
	public static <K,V> Map<K,V> u(Map<? extends K,? extends V> value) {
		return value == null ? null : Collections.unmodifiableMap(value);
	}

	/**
	 * Creates an unmodifiable view of the specified set.
	 *
	 * <p>This is a null-safe wrapper around {@link Collections#unmodifiableSet(Set)}.</p>
	 *
	 * @param <T> The element type.
	 * @param value The set to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the set, or null if the input was null.
	 */
	public static <T> Set<T> u(Set<? extends T> value) {
		return value == null ? null : Collections.unmodifiableSet(value);
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
			throw ThrowableUtils.runtimeException("Invalid env type: {0}", c);
		return f.apply(s);
	}

	/** Constructor - This class is meant to be subclasses. */
	protected Utils() {}

	/**
	 * If the specified object is a {@link Supplier} or {@link Value}, returns the inner value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	public static Object unwrap(Object o) {
		if (o instanceof Supplier)
			o = unwrap(((Supplier<?>)o).get());
		if (o instanceof Value)
			o = unwrap(((Value<?>)o).get());
		if (o instanceof Optional)
			o = unwrap(((Optional<?>)o).orElse(null));
		return o;
	}
}