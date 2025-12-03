package org.apache.juneau.commons.utils;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.commons.collections.*;

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

/**
 * Utility methods for working with collections and maps.
 *
 * <h5 class='section'>Complex Data Structure Construction:</h5>
 * <p>
 * This class provides convenient shorthand methods for creating complex nested data structures.
 * The primary methods for building structures are:
 * <ul>
 *    <li>{@link #a(Object...)} - Creates arrays
 *    <li>{@link #ao(Object...)} - Creates Object arrays (when type inference needs help)
 *    <li>{@link #list(Object...)} / {@link #l(Object...)} - Creates modifiable lists
 *    <li>{@link #map(Object, Object, Object, Object, Object, Object)} - Creates modifiable maps
 *    <li>{@link #m(Object, Object, Object, Object, Object, Object)} - Creates unmodifiable maps
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Array of lists of maps</jc>
 * 	<jk>var</jk> <jv>data</jv> = <jsm>a</jsm>(
 * 		<jsm>l</jsm>(<jsm>m</jsm>(<js>"name"</js>, <js>"John"</js>, <js>"age"</js>, 30)),
 * 		<jsm>l</jsm>(<jsm>m</jsm>(<js>"name"</js>, <js>"Jane"</js>, <js>"age"</js>, 25))
 * 	);
 *
 * 	<jc>// Compare with traditional Java syntax:</jc>
 * 	List&lt;Map&lt;String,Object&gt;&gt;[] <jv>data</jv> = <jk>new</jk> List[]{
 * 		<jk>new</jk> ArrayList&lt;&gt;(Arrays.asList(
 * 			<jk>new</jk> LinkedHashMap&lt;&gt;(){{
 * 				put(<js>"name"</js>, <js>"John"</js>);
 * 				put(<js>"age"</js>, 30);
 * 			}}
 * 		)),
 * 		<jk>new</jk> ArrayList&lt;&gt;(Arrays.asList(
 * 			<jk>new</jk> LinkedHashMap&lt;&gt;(){{
 * 				put(<js>"name"</js>, <js>"Jane"</js>);
 * 				put(<js>"age"</js>, 25);
 * 			}}
 * 		))
 * 	};
 * </p>
 *
 * <p class='bjava'>
 * 	<jc>// Complex nested structure: Array of lists containing maps and arrays</jc>
 * 	<jk>var</jk> <jv>complex</jv> = <jsm>a</jsm>(
 * 		<jsm>l</jsm>(
 * 			<jsm>m</jsm>(<js>"user"</js>, <js>"admin"</js>, <js>"roles"</js>, <jsm>a</jsm>(<js>"read"</js>, <js>"write"</js>, <js>"delete"</js>)),
 * 			<jsm>m</jsm>(<js>"user"</js>, <js>"guest"</js>, <js>"roles"</js>, <jsm>a</jsm>(<js>"read"</js>))
 * 		),
 * 		<jsm>l</jsm>(
 * 			<jsm>m</jsm>(<js>"status"</js>, <js>"active"</js>, <js>"count"</js>, 42)
 * 		)
 * 	);
 *
 * 	<jc>// Traditional Java equivalent (significantly more verbose):</jc>
 * 	List&lt;Map&lt;String,Object&gt;&gt;[] <jv>complex</jv> = <jk>new</jk> List[]{
 * 		<jk>new</jk> ArrayList&lt;&gt;(Arrays.asList(
 * 			<jk>new</jk> LinkedHashMap&lt;&gt;(){{
 * 				put(<js>"user"</js>, <js>"admin"</js>);
 * 				put(<js>"roles"</js>, <jk>new</jk> String[]{<js>"read"</js>, <js>"write"</js>, <js>"delete"</js>});
 * 			}},
 * 			<jk>new</jk> LinkedHashMap&lt;&gt;(){{
 * 				put(<js>"user"</js>, <js>"guest"</js>);
 * 				put(<js>"roles"</js>, <jk>new</jk> String[]{<js>"read"</js>});
 * 			}}
 * 		)),
 * 		<jk>new</jk> ArrayList&lt;&gt;(Arrays.asList(
 * 			<jk>new</jk> LinkedHashMap&lt;&gt;(){{
 * 				put(<js>"status"</js>, <js>"active"</js>);
 * 				put(<js>"count"</js>, 42);
 * 			}}
 * 		))
 * 	};
 * </p>
 *
 * <p class='bjava'>
 * 	<jc>// Using unmodifiable maps for immutable data</jc>
 * 	<jk>var</jk> <jv>config</jv> = <jsm>a</jsm>(
 * 		<jsm>m</jsm>(<js>"env"</js>, <js>"production"</js>, <js>"debug"</js>, <jk>false</jk>),
 * 		<jsm>m</jsm>(<js>"env"</js>, <js>"development"</js>, <js>"debug"</js>, <jk>true</jk>)
 * 	);
 * </p>
 *
 * <h5 class='section'>Best Practices:</h5>
 * <ul>
 *    <li>Use {@link #a(Object...)} for arrays when type can be inferred, {@link #ao(Object...)} for Object arrays
 *    <li>Use {@link #map(Object, Object, Object, Object, Object, Object)} when you need modifiable maps
 *    <li>Use {@link #m(Object, Object, Object, Object, Object, Object)} when you need immutable/unmodifiable maps
 *    <li>Use {@link #list(Object...)} or {@link #l(Object...)} for modifiable lists
 *    <li>Static import these methods for maximum readability: <code>import static org.apache.juneau.commons.utils.CollectionUtils.*;</code>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">juneau-commons</a>
 * </ul>
 */
public class CollectionUtils {

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
	 * Creates a 2-dimensional array.
	 *
	 * <p>
	 * This method provides a convenient way to create 2D arrays with cleaner syntax than traditional Java.
	 * While you could technically use {@code a(a(...), a(...))}, that approach fails for single-row arrays
	 * like {@code a(a(...))} because the type system collapses it into a 1D array.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// 2D array with multiple rows</jc>
	 * 	String[][] <jv>matrix</jv> = a2(
	 * 		a(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>),
	 * 		a(<js>"d"</js>, <js>"e"</js>, <js>"f"</js>),
	 * 		a(<js>"g"</js>, <js>"h"</js>, <js>"i"</js>)
	 * 	);
	 *
	 * 	<jc>// Single row - this works correctly with a2()</jc>
	 * 	String[][] <jv>singleRow</jv> = a2(a(<js>"x"</js>, <js>"y"</js>, <js>"z"</js>));  <jc>// Returns String[1][3]</jc>
	 *
	 * 	<jc>// Without a2(), this would fail (becomes 1D array):</jc>
	 * 	<jc>// String[] badAttempt = a(a("x", "y", "z"));  // Wrong! Returns String[3]</jc>
	 *
	 * 	<jc>// Empty 2D array</jc>
	 * 	String[][] <jv>empty</jv> = a2();  <jc>// Returns String[0][]</jc>
	 *
	 * 	<jc>// Compare with traditional Java syntax:</jc>
	 * 	String[][] <jv>traditional</jv> = <jk>new</jk> String[][] {
	 * 		{<js>"a"</js>, <js>"b"</js>, <js>"c"</js>},
	 * 		{<js>"d"</js>, <js>"e"</js>, <js>"f"</js>},
	 * 		{<js>"g"</js>, <js>"h"</js>, <js>"i"</js>}
	 * 	};
	 * </p>
	 *
	 * <h5 class='section'>Use Cases:</h5>
	 * <ul>
	 * 	<li>Creating matrices or grids
	 * 	<li>Building test data with multiple rows
	 * 	<li>Representing tabular data
	 * 	<li>Any scenario requiring a 2D array structure
	 * </ul>
	 *
	 * @param <E> The element type of the inner arrays.
	 * @param value The 1D arrays that will become rows in the 2D array.
	 * @return A 2D array containing the specified rows.
	 */
	@SafeVarargs
	public static <E> E[][] a2(E[]...value) {
		return value;
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
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.
	 * @param entries The entries to add.
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> List<E> addAll(List<E> value, E...entries) {
		if (nn(entries)) {
			if (value == null)
				value = list(entries);
			else
				Collections.addAll(value, entries);
		}
		return value;
	}

	/**
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.
	 * @param entries The entries to add.
	 * @return The set.
	 */
	public static <E> List<E> addAll(List<E> value, List<E> entries) {
		if (nn(entries)) {
			if (value == null)
				value = copyOf(entries);
			else
				value.addAll(entries);
		}
		return value;
	}

	/**
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.
	 * @param entries The entries to add.
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> Set<E> addAll(Set<E> value, E...entries) {
		if (nn(entries)) {
			if (value == null)
				value = set(entries);
			else
				Collections.addAll(value, entries);
		}
		return value;
	}

	/**
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.
	 * @param entries The entries to add.
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> SortedSet<E> addAll(SortedSet<E> value, E...entries) {
		if (nn(entries)) {
			if (value == null)
				value = sortedSet(entries);
			else
				Collections.addAll(value, entries);
		}
		return value;
	}

	/**
	 * Appends one or more elements to an array.
	 *
	 * @param <T> The element type.
	 * @param array The array to append to.
	 * @param newElements The new elements to append to the array.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] addAll(T[] array, T...newElements) {
		if (array == null)
			return newElements;
		if (newElements.length == 0)
			return array;
		var a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.length);
		for (var i = 0; i < array.length; i++)
			a[i] = array[i];
		for (var i = 0; i < newElements.length; i++)
			a[i + array.length] = newElements[i];
		return a;
	}

	/**
	 * Creates an array of objects with the return type explicitly set to {@code Object[]}.
	 *
	 * <p>
	 * This method is useful when you need to force the return type to be {@code Object[]} regardless of
	 * the actual types of the elements passed in. This is particularly helpful in scenarios where:
	 * <ul>
	 * 	<li>You're mixing different types in the same array
	 * 	<li>You need to avoid type inference issues with the generic {@link #a(Object...) a()} method
	 * 	<li>You're working with APIs that specifically require {@code Object[]}
	 * 	<li>You want to ensure maximum flexibility in what can be stored in the array
	 * </ul>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Mixed types - works perfectly with ao()</jc>
	 * 	Object[] <jv>mixed</jv> = ao(<js>"string"</js>, 42, <jk>true</jk>, 3.14, <jk>null</jk>);
	 *
	 * 	<jc>// Force Object[] return type even for uniform types</jc>
	 * 	Object[] <jv>strings</jv> = ao(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);  <jc>// Returns Object[], not String[]</jc>
	 *
	 * 	<jc>// Compare with a() which infers the most specific type:</jc>
	 * 	String[] <jv>typed</jv> = a(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);     <jc>// Returns String[]</jc>
	 *
	 * 	<jc>// Useful when you need Object[] for APIs:</jc>
	 * 	<jk>void</jk> someMethod(Object[] args) { ... }
	 * 	someMethod(ao(<js>"test"</js>, 123, <jk>true</jk>));  <jc>// No type issues</jc>
	 *
	 * 	<jc>// Empty Object array</jc>
	 * 	Object[] <jv>empty</jv> = ao();
	 *
	 * 	<jc>// With null values</jc>
	 * 	Object[] <jv>withNulls</jv> = ao(<js>"value"</js>, <jk>null</jk>, 42, <jk>null</jk>);
	 * </p>
	 *
	 * <h5 class='section'>When to Use:</h5>
	 * <ul>
	 * 	<li>Use {@link #a(Object...) a()} when you want type inference for homogeneous arrays
	 * 	<li>Use {@code ao()} when you explicitly need {@code Object[]} or have mixed types
	 * </ul>
	 *
	 * @param value The objects to place in the array.
	 * @return A new {@code Object[]} containing the specified objects.
	 */
	public static Object[] ao(Object...value) {
		return value;
	}

	/**
	 * Creates an array of the specified component type and length.
	 *
	 * @param <E> The component type of the array.
	 * @param componentType The component type of the array.
	 * @param length The length of the array.
	 * @return A new array of the specified type and length. Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] array(Class<E> componentType, int length) {
		return (E[])Array.newInstance(componentType, length);
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
		assertArgNotNull("value", value);
		var array = (E[])Array.newInstance(componentType, value.size());
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
		assertArgNotNull("array", array);
		assertArg(isArray(array), "Input must be an array but was {0}", cn(array));

		var componentType = array.getClass().getComponentType();
		var length = Array.getLength(array);
		var result = new ArrayList<>(length);

		// Handle primitive arrays specifically for better performance
		if (componentType.isPrimitive()) {
			if (componentType == int.class) {
				var arr = (int[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == long.class) {
				var arr = (long[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == double.class) {
				var arr = (double[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == float.class) {
				var arr = (float[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == boolean.class) {
				var arr = (boolean[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == byte.class) {
				var arr = (byte[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else if (componentType == char.class) {
				var arr = (char[])array;
				for (var value : arr) {
					result.add(value);
				}
			} else /* (componentType == short.class) */ {
				var arr = (short[])array;
				for (var value : arr) {
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
	 * Shortcut for creating a boolean array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk>[] <jv>myArray</jv> = <jsm>booleans</jsm>(<jk>true</jk>, <jk>false</jk>, <jk>true</jk>);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new boolean array.
	 */
	public static boolean[] booleans(boolean...value) {
		return value;
	}

	/**
	 * Shortcut for creating a byte array.
	 *
	 * <p>Accepts int values and converts them to bytes, eliminating the need for explicit byte casts.</p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>byte</jk>[] <jv>myArray</jv> = <jsm>bytes</jsm>(1, 2, 3);
	 * </p>
	 *
	 * @param value The int values to convert to bytes. Values outside the byte range (-128 to 127) will be truncated.
	 * @return A new byte array.
	 */
	public static byte[] bytes(int...value) {
		var result = new byte[value.length];
		for (var i = 0; i < value.length; i++) {
			result[i] = (byte)value[i];
		}
		return result;
	}

	/**
	 * Shortcut for creating a char array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>char</jk>[] <jv>myArray</jv> = <jsm>chars</jsm>(<js>'a'</js>, <js>'b'</js>, <js>'c'</js>);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new char array.
	 */
	public static char[] chars(char...value) {
		return value;
	}

	/**
	 * Combine an arbitrary number of arrays into a single array.
	 *
	 * @param <E> The element type.
	 * @param arrays Collection of arrays to combine.
	 * @return A new combined array, or <jk>null</jk> if all arrays are <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] combine(E[]...arrays) {
		assertArgNotNull("arrays", arrays);
		int l = 0;
		E[] a1 = null;
		for (var a : arrays) {
			if (a1 == null && nn(a))
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return null;
		var a = (E[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (var aa : arrays)
			if (nn(aa))
				for (var t : aa)
					a[i++] = t;
		return a;
	}

	/**
	 * Returns <jk>true</jk> if the specified array contains the specified element using the {@link String#equals(Object)}
	 * method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	<jk>true</jk> if the specified array contains the specified element,
	 * 	<jk>false</jk> if the array or element is <jk>null</jk>.
	 */
	public static <T> boolean contains(T element, T[] array) {
		return indexOf(element, array) != -1;
	}

	/**
	 * Copies the specified array into the specified list.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * @param array The array to copy into a list.
	 * @param list The list to copy the values into.
	 * @return The same list passed in.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List copyArrayToList(Object array, List list) {
		if (nn(array)) {
			var length = Array.getLength(array);
			for (var i = 0; i < length; i++)
				list.add(Array.get(array, i));
		}
		return list;
	}

	/**
	 * Creates a new collection from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Collection<E> copyOf(Collection<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Convenience method for copying a list.
	 *
	 * @param <E> The element type.
	 * @param value The list to copy.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> copyOf(List<E> value) {
		return value == null ? null : new ArrayList<>(value);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> List<E> copyOf(List<E> l, Function<? super E,? extends E> valueMapper) {
		return copyOf(l, valueMapper, LinkedList::new);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @param listFactory The factory for creating the list.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> List<E> copyOf(List<E> l, Function<? super E,? extends E> valueMapper, Supplier<List<E>> listFactory) {
		return l == null ? null : l.stream().map(valueMapper).collect(toCollection(listFactory));
	}

	/**
	 * Creates a new map from the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> val) {
		return val == null ? null : new LinkedHashMap<>(val);
	}

	/**
	 * Makes a deep copy of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map to copy.
	 * @param valueMapper The function to apply to each value in the map.
	 * @return A new map with the same keys as the specified map, but with values transformed by the specified function.  Null if the map being copied was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V,? extends V> valueMapper) {
		return copyOf(m, valueMapper, LinkedHashMap::new);
	}

	/**
	 * Makes a deep copy of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map to copy.
	 * @param valueMapper The function to apply to each value in the map.
	 * @param mapFactory The factory for creating the map.
	 * @return A new map with the same keys as the specified map, but with values transformed by the specified function.  Null if the map being copied was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V,? extends V> valueMapper, Supplier<Map<K,V>> mapFactory) {
		return m == null ? null : m.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> valueMapper.apply(e.getValue()), (a, b) -> b, mapFactory));
	}

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Set<E> copyOf(Set<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E,? extends E> valueMapper) {
		return copyOf(l, valueMapper, LinkedHashSet::new);
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @param setFactory The factory for creating sets.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E,? extends E> valueMapper, Supplier<Set<E>> setFactory) {
		return l == null ? null : l.stream().map(valueMapper).collect(toCollection(setFactory));
	}

	/**
	 * Makes a copy of the specified array.
	 *
	 * @param array The array to copy.
	 * @param <T> The element type.
	 * @return A new copy of the array, or <jk>null</jk> if the array was <jk>null</jk>.s
	 */
	public static <T> T[] copyOf(T[] array) {
		return array == null ? null : Arrays.copyOf(array, array.length);
	}

	/**
	 * Shortcut for creating a double array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>double</jk>[] <jv>myArray</jv> = <jsm>doubles</jsm>(1.0, 2.0, 3.0);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new double array.
	 */
	public static double[] doubles(double...value) {
		return value;
	}

	/**
	 * Returns the first element in a list.
	 *
	 * @param <E> The element type.
	 * @param l The list. Can be <jk>null</jk>.
	 * @return The first element in the list, or <jk>null</jk> if the list is <jk>null</jk> or empty.
	 */
	public static <E> E first(List<E> l) {
		return isEmpty(l) ? null : l.get(0);
	}

	/**
	 * Shortcut for creating a float array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>float</jk>[] <jv>myArray</jv> = <jsm>floats</jsm>(1.0f, 2.0f, 3.0f);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new float array.
	 */
	public static float[] floats(float...value) {
		return value;
	}

	/**
	 * Iterates the specified array in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The array to iterate.
	 * @param action The action to perform.
	 */
	public static <E> void forEachReverse(E[] value, Consumer<E> action) {
		for (var i = value.length - 1; i >= 0; i--)
			action.accept(value[i]);
	}

	/**
	 * Iterates the specified list in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The list to iterate.
	 * @param action The action to perform.
	 */
	public static <E> void forEachReverse(List<E> value, Consumer<E> action) {
		if (value instanceof ArrayList) {
			for (var i = value.size() - 1; i >= 0; i--)
				action.accept(value.get(i));
		} else {
			ListIterator<E> i = value.listIterator(value.size());
			while (i.hasPrevious())
				action.accept(i.previous());
		}
	}

	/**
	 * Returns the index position of the element in the specified array using the {@link String#equals(Object)} method.
	 *
	 * @param element The element to check for.
	 * @param array The array to check.
	 * @return
	 * 	The index position of the element in the specified array, or
	 * 	<c>-1</c> if the array doesn't contain the element, or the array or element is <jk>null</jk>.
	 */
	public static <T> int indexOf(T element, T[] array) {
		if (element == null || array == null)
			return -1;
		for (var i = 0; i < array.length; i++)
			if (eq(element, array[i]))
				return i;
		return -1;
	}

	/**
	 * Shortcut for creating an int array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>int</jk>[] <jv>myArray</jv> = <jsm>ints</jsm>(1, 2, 3);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new int array.
	 */
	public static int[] ints(int...value) {
		return value;
	}

	/**
	 * Returns <jk>true</jk> if the specified array is null or has a length of zero.
	 *
	 * @param array The array to check.
	 * @return <jk>true</jk> if the specified array is null or has a length of zero.
	 */
	public static boolean isEmptyArray(Object[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Returns <jk>true</jk> if the specified array is not null and has a length greater than zero.
	 *
	 * @param array The array to check.
	 * @return <jk>true</jk> if the specified array is not null and has a length greater than zero.
	 */
	public static boolean isNotEmptyArray(Object[] array) {
		return nn(array) && array.length > 0;
	}

	/**
	 * Shortcut for creating an unmodifiable list out of an array of values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the list.
	 * @return An unmodifiable list containing the specified values, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> List<T> l(T...values) {  // NOSONAR
		return values == null ? null : Arrays.asList(values);
	}

	/**
	 * Returns the last entry in an array.
	 *
	 * @param <E> The element type.
	 * @param l The array.
	 * @return The last element, or <jk>null</jk> if the array is <jk>null</jk> or empty.
	 */
	public static <E> E last(E[] l) {
		return (l == null || l.length == 0) ? null : l[l.length - 1];
	}

	/**
	 * Returns the last entry in a list.
	 *
	 * @param <E> The element type.
	 * @param l The list.
	 * @return The last element, or <jk>null</jk> if the list is <jk>null</jk> or empty.
	 */
	public static <E> E last(List<E> l) {
		return isEmpty(l) ? null : l.get(l.size() - 1);
	}

	/**
	 * Returns the length of the specified array.
	 *
	 * <p>
	 * This is a null-safe convenience method that wraps {@link Array#getLength(Object)}.
	 * Works with both object arrays and primitive arrays.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array1</jv> = {<js>"foo"</js>, <js>"bar"</js>};
	 * 	<jk>int</jk>[] <jv>array2</jv> = {1, 2, 3};
	 *
	 * 	<jk>int</jk> <jv>len1</jv> = length(<jv>array1</jv>);  <jc>// Returns 2</jc>
	 * 	<jk>int</jk> <jv>len2</jv> = length(<jv>array2</jv>);  <jc>// Returns 3</jc>
	 * 	<jk>int</jk> <jv>len3</jv> = length(<jk>null</jk>);    <jc>// Returns 0</jc>
	 * </p>
	 *
	 * @param array The array object. Can be <jk>null</jk>.
	 * @return The length of the array, or <c>0</c> if the array is <jk>null</jk> or not an array.
	 */
	public static int length(Object array) {
		if (array == null)
			return 0;
		assertArg(array.getClass().isArray(), "Object is not an array");
		return Array.getLength(array);
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
		return new ArrayList<>(l(values));
	}

	/**
	 * Convenience factory for a {@link ListBuilder}.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @param converters Optional converters to use for converting values.
	 * @return A new list builder.
	 */
	public static <E> ListBuilder<E> listb(Class<E> type, Converter...converters) {
		return ListBuilder.create(type).converters(converters);
	}

	/**
	 * Shortcut for creating an empty list.
	 *
	 * <p>
	 * This is a convenience method that provides a more concise syntax than {@link Collections#emptyList()}.
	 * The "e" suffix indicates "empty".
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>myList</jv> = <jsm>liste</jsm>();
	 * </p>
	 *
	 * @param <T> The element type.
	 * @return An empty unmodifiable list.
	 */
	public static <T> List<T> liste() {
		return Collections.emptyList();
	}

	/**
	 * Shortcut for creating an empty list of the specified type.
	 *
	 * @param <T> The element type.
	 * @param type The element type class.
	 * @return An empty list.
	 */
	public static <T> List<T> liste(Class<T> type) {
		return Collections.emptyList();
	}

	/**
	 * Returns a null list.
	 *
	 * @param <T> The element type.
	 * @param type The element type class.
	 * @return <jk>null</jk>.
	 */
	public static <T> List<T> listn(Class<T> type) {
		return null;
	}

	/**
	 * Convenience method for creating an {@link ArrayList}.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SafeVarargs
	public static <E> List<E> listOf(Class<E> elementType, E...values) {
		return list(values);
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
	 * Shortcut for creating a long array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>long</jk>[] <jv>myArray</jv> = <jsm>longs</jsm>(1L, 2L, 3L);
	 * </p>
	 *
	 * @param value The values to initialize the array with.
	 * @return A new long array.
	 */
	public static long[] longs(long...value) {
		return value;
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new unmodifiable empty map.
	 */
	public static <K,V> Map<K,V> m() {
		return Map.of();
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1) {
		return new SimpleUnmodifiableMap<>(a(k1), a(v1));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2) {
		return new SimpleUnmodifiableMap<>(a(k1, k2), a(v1, v2));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3), a(v1, v2, v3));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4), a(v1, v2, v3, v4));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5), a(v1, v2, v3, v4, v5));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5, k6), a(v1, v2, v3, v4, v5, v6));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5, k6, k7), a(v1, v2, v3, v4, v5, v6, v7));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5, k6, k7, k8), a(v1, v2, v3, v4, v5, v6, v7, v8));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @param k9 Key 9.
	 * @param v9 Value 9.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5, k6, k7, k8, k9), a(v1, v2, v3, v4, v5, v6, v7, v8, v9));
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 * Unlike Map.of(...), supports null keys/values and preserves insertion order.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @param k9 Key 9.
	 * @param v9 Value 9.
	 * @param k10 Key 10.
	 * @param v10 Value 10.
	 * @return A new unmodifiable map.
	 */
	public static <K,V> Map<K,V> m(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
		return new SimpleUnmodifiableMap<>(a(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10), a(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map() {
		var m = new LinkedHashMap<K,V>();
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Array utilities
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		m.put(k6, v6);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		m.put(k6, v6);
		m.put(k7, v7);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		m.put(k6, v6);
		m.put(k7, v7);
		m.put(k8, v8);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @param k9 Key 9.
	 * @param v9 Value 9.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		m.put(k6, v6);
		m.put(k7, v7);
		m.put(k8, v8);
		m.put(k9, v9);
		return m;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param k1 Key 1.
	 * @param v1 Value 1.
	 * @param k2 Key 2.
	 * @param v2 Value 2.
	 * @param k3 Key 3.
	 * @param v3 Value 3.
	 * @param k4 Key 4.
	 * @param v4 Value 4.
	 * @param k5 Key 5.
	 * @param v5 Value 5.
	 * @param k6 Key 6.
	 * @param v6 Value 6.
	 * @param k7 Key 7.
	 * @param v7 Value 7.
	 * @param k8 Key 8.
	 * @param v8 Value 8.
	 * @param k9 Key 9.
	 * @param v9 Value 9.
	 * @param k10 Key 10.
	 * @param v10 Value 10.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
		var m = new LinkedHashMap<K,V>();
		m.put(k1, v1);
		m.put(k2, v2);
		m.put(k3, v3);
		m.put(k4, v4);
		m.put(k5, v5);
		m.put(k6, v6);
		m.put(k7, v7);
		m.put(k8, v8);
		m.put(k9, v9);
		m.put(k10, v10);
		return m;
	}

	/**
	 * Convenience factory for a {@link MapBuilder} with {@link String} keys and {@link Object} values.
	 *
	 * <p>
	 * This is a shortcut for <c>MapBuilder.create(String.<jk>class</jk>, Object.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String,Object&gt; <jv>map</jv> = mapb()
	 * 		.add(<js>"foo"</js>, 1)
	 * 		.add(<js>"bar"</js>, 2)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This builder supports optional filtering to automatically exclude unwanted values:
	 * <p class='bjava'>
	 * 	<jc>// Build a map excluding null and empty values</jc>
	 * 	Map&lt;String,Object&gt; <jv>map</jv> = mapb().filtered()
	 * 		.add(<js>"foo"</js>, <jk>null</jk>)       <jc>// Excluded</jc>
	 * 		.add(<js>"bar"</js>, <js>""</js>)         <jc>// Excluded</jc>
	 * 		.add(<js>"baz"</js>, <js>"value"</js>)    <jc>// Included</jc>
	 * 		.build();
	 * </p>
	 *
	 * @return A new map builder.
	 * @see MapBuilder
	 */
	public static MapBuilder<String,Object> mapb() {
		return MapBuilder.create(String.class, Object.class);
	}

	/**
	 * Convenience factory for a {@link MapBuilder}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @param converters Optional converters to use for converting values.
	 * @return A new map builder.
	 */
	public static <K,V> MapBuilder<K,V> mapb(Class<K> keyType, Class<V> valueType, Converter...converters) {
		return MapBuilder.create(keyType, valueType).converters(converters);
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
	public static <K,V> Map<K,V> mape(Class<K> keyType, Class<V> valueType) {
		return Collections.emptyMap();
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
	public static <K,V> Map<K,V> mapn(Class<K> keyType, Class<V> valueType) {
		return null;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> mapOf(Class<K> keyType, Class<V> valueType) {
		return map();
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
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.
	 * @param entries The entries to add.
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> List<E> prependAll(List<E> value, E...entries) {
		if (nn(entries)) {
			if (value == null)
				value = list(entries);
			else
				value.addAll(0, l(entries));
		}
		return value;
	}

	/**
	 * Reverses the entries in an array.
	 *
	 * @param <E> The element type.
	 * @param array The array to reverse.
	 * @return The same array.
	 */
	/**
	 * Reverses the elements in the specified array in-place.
	 *
	 * @param <E> The element type.
	 * @param array The array to reverse.
	 * @return The same array, reversed.
	 */
	public static <E> E[] reverse(E[] array) {
		for (var i = 0; i < array.length / 2; i++) {
			E temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
		return array;
	}

	/**
	 * Returns a reversed view of the specified list.
	 *
	 * <p>
	 * The returned list is a live view that reflects changes to the original list.
	 *
	 * @param <E> The element type.
	 * @param list The list to reverse.
	 * @return A reversed view of the list.
	 */
	public static <E> List<E> reverse(List<E> list) {
		return new ReversedList<>(list);
	}

	/**
	 * Returns a reverse stream of the specified list.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list</jv> = <jsm>list</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 * 	Stream&lt;String&gt; <jv>reversed</jv> = <jsm>rstream</jsm>(<jv>list</jv>);
	 * 	<jc>// Produces stream: "c", "b", "a"</jc>
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param value The list to stream in reverse order. Can be <jk>null</jk>.
	 * @return A stream of the list elements in reverse order, or an empty stream if the list is <jk>null</jk> or empty.
	 */
	public static <T> Stream<T> rstream(List<T> value) {
		if (value == null || value.isEmpty())
			return Stream.empty();
		return IntStream.range(0, value.size()).mapToObj(i -> value.get(value.size() - 1 - i));
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
		assertArgNotNull("values", values);
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/**
	 * Convenience factory for a {@link SetBuilder}.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @param converters Optional converters to use for converting values.
	 * @return A new set builder.
	 */
	public static <E> SetBuilder<E> setb(Class<E> type, Converter...converters) {
		return SetBuilder.create(type).converters(converters);
	}

	/**
	 * Convenience method for creating a {@link LinkedHashSet}.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param values The values to initialize the set with.
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> LinkedHashSet<E> setOf(Class<E> elementType, E...values) {
		return set(values);
	}

	/**
	 * Shortcut for creating a short array.
	 *
	 * <p>Accepts int values and converts them to shorts, eliminating the need for explicit short casts.</p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>short</jk>[] <jv>myArray</jv> = <jsm>shorts</jsm>(1, 2, 3);
	 * </p>
	 *
	 * @param value The int values to convert to shorts. Values outside the short range (-32768 to 32767) will be truncated.
	 * @return A new short array.
	 */
	public static short[] shorts(int...value) {
		var result = new short[value.length];
		for (var i = 0; i < value.length; i++) {
			result[i] = (short)value[i];
		}
		return result;
	}

	/**
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param comparator The comparator to use to sort the list.
	 * @param value The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> sortedList(Comparator<E> comparator, Collection<E> value) {
		ArrayList<E> l = toList(value);
		Collections.sort(l, comparator);
		return l;
	}

	/**
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param comparator The comparator to use to sort the list.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	public static <E> List<E> sortedList(Comparator<E> comparator, E[] values) {
		List<E> l = list(values);
		Collections.sort(l, comparator);
		return l;
	}

	/**
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SafeVarargs
	public static <E> List<E> sortedList(E...values) {
		List<E> l = list(values);
		Collections.sort((List<Comparable>)l);
		return l;
	}

	/**
	 * Convenience method for creating a {@link TreeMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new modifiable set.
	 */
	public static <K,V> TreeMap<K,V> sortedMap() {
		return new TreeMap<>();
	}

	/**
	 * Convenience method for creating a {@link TreeSet}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the set with.
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> TreeSet<E> sortedSet(E...values) {
		assertArgNotNull("values", values);
		var l = new TreeSet<E>();
		for (var v : values)
			l.add(v);
		return l;
	}

	/**
	 * Returns a stream of the specified array.
	 *
	 * <p>
	 * Gracefully handles <jk>null</jk> arrays by returning an empty stream.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>array</jv> = {<js>"a"</js>, <js>"b"</js>, <js>"c"</js>};
	 *
	 * 	<jc>// Prints "a", "b", "c"</jc>
	 * 	<jsm>stream</jsm>(<jv>array</jv>).forEach(System.<jk>out</jk>::println);
	 *
	 * 	<jc>// Handles null gracefully - returns empty stream</jc>
	 * 	<jsm>stream</jsm>(<jk>null</jk>).forEach(System.<jk>out</jk>::println);  <jc>// Prints nothing</jc>
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param array The array to stream. Can be <jk>null</jk>.
	 * @return A stream of the array elements, or an empty stream if the array is <jk>null</jk>.
	 */
	public static <T> Stream<T> stream(T[] array) {
		return array == null ? Stream.empty() : Arrays.stream(array);
	}

	/**
	 * Wraps the specified list in {@link Collections#unmodifiableList(List)}.
	 *
	 * @param <E> The element type.
	 * @param value The list to wrap.
	 * @return The wrapped list.
	 */
	public static <E> List<E> synced(List<E> value) {
		return value == null ? null : Collections.synchronizedList(value);
	}

	/**
	 * Wraps the specified map in {@link Collections#unmodifiableMap(Map)}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The map to wrap.
	 * @return The wrapped map.
	 */
	public static <K,V> Map<K,V> synced(Map<K,V> value) {
		return value == null ? null : Collections.synchronizedMap(value);
	}

	/**
	 * Wraps the specified set in {@link Collections#unmodifiableSet(Set)}.
	 *
	 * @param <E> The element type.
	 * @param value The set to wrap.
	 * @return The wrapped set.
	 */
	public static <E> Set<E> synced(Set<E> value) {
		return value == null ? null : Collections.synchronizedSet(value);
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * <p>
	 * Works on both object and primitive arrays.
	 *
	 * @param <E> The element type.
	 * @param c The collection to convert to an array.
	 * @param elementType The component type of the collection.
	 * @return A new array.
	 */
	public static <E> Object toArray(Collection<?> c, Class<E> elementType) {
		var a = Array.newInstance(elementType, c.size());
		Iterator<?> it = c.iterator();
		int i = 0;
		while (it.hasNext())
			Array.set(a, i++, it.next());
		return a;
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <E> The element type.
	 * @param value The collection to copy from.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> toList(Collection<E> value) {
		return toList(value, false);
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <E> The element type.
	 * @param value The collection to copy from.
	 * @param nullIfEmpty If <jk>true</jk> will return <jk>null</jk> if the collection is empty.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> toList(Collection<E> value, boolean nullIfEmpty) {
		if (value == null || (nullIfEmpty && value.isEmpty()))
			return null;
		var l = new ArrayList<E>();
		value.forEach(x -> l.add(x));
		return l;
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
	 * @see arrayToList
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
		throw rex("Could not convert object of type {0} to a list", cn(o));
	}

	/**
	 * Converts the specified array to an <c>ArrayList</c>
	 *
	 * @param <E> The element type.
	 * @param array The array to convert.
	 * @param elementType
	 * 	The type of objects in the array.
	 * 	It must match the actual component type in the array.
	 * @return A new {@link ArrayList}
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> toList(Object array, Class<E> elementType) {
		var l = new ArrayList<E>(Array.getLength(array));
		for (var i = 0; i < Array.getLength(array); i++)
			l.add((E)Array.get(array, i));
		return l;
	}

	/**
	 * Recursively converts the specified array into a list of objects.
	 *
	 * @param array The array to convert.
	 * @return A new {@link ArrayList}
	 */
	public static List<Object> toObjectList(Object array) {
		var l = new ArrayList<>(Array.getLength(array));
		for (var i = 0; i < Array.getLength(array); i++) {
			var o = Array.get(array, i);
			if (isArray(o))
				o = toObjectList(o);
			l.add(o);
		}
		return l;
	}

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Set<E> toSet(Collection<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Converts the specified array to a <c>Set</c>.
	 *
	 * <p>
	 * The order of the entries in the set are the same as the array.
	 *
	 * @param <T> The entry type of the array.
	 * @param array The array being wrapped in a <c>Set</c> interface.
	 * @return The new set.
	 */
	public static <T> Set<T> toSet(T[] array) {
		assertArgNotNull("array", array);
		return new AbstractSet<>() {

			@Override /* Overridden from Set */
			public Iterator<T> iterator() {
				return new Iterator<>() {
					int i = 0;

					@Override /* Overridden from Iterator */
					public boolean hasNext() {
						return i < array.length;
					}

					@Override /* Overridden from Iterator */
					public T next() {
						if (i >= array.length)
							throw new NoSuchElementException();
						T t = array[i];
						i++;
						return t;
					}

					@Override /* Overridden from Iterator */
					public void remove() {
						throw unsupportedOp();
					}
				};
			}

			@Override /* Overridden from Set */
			public int size() {
				return array.length;
			}
		};
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Primitive array creation methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new {@link TreeSet} from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param value The value to copy from.
	 * @return A new {@link TreeSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> TreeSet<E> toSortedSet(Collection<E> value) {
		if (value == null)
			return null;
		var l = new TreeSet<E>();
		value.forEach(x -> l.add(x));
		return l;
	}

	/**
	 * Creates a new {@link TreeSet} from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param value The value to copy from.
	 * @param nullIfEmpty If <jk>true</jk> returns <jk>null</jk> if the collection is empty.
	 * @return A new {@link TreeSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> TreeSet<E> toSortedSet(Collection<E> value, boolean nullIfEmpty) {
		if (value == null || (nullIfEmpty && value.isEmpty()))
			return null;
		var l = new TreeSet<E>();
		value.forEach(x -> l.add(x));
		return l;
	}

	/**
	 * Creates a new {@link TreeSet} containing a copy of the specified set.
	 *
	 * @param <T> The element type.
	 * @param copyFrom The set to copy from.
	 * @return A new {@link TreeSet}, or <jk>null</jk> if the set was <jk>null</jk>.
	 */
	public static <T> TreeSet<T> toSortedSet(Set<T> copyFrom) {
		return copyFrom == null ? null : new TreeSet<>(copyFrom);
	}

	/**
	 * Converts an array to a stream of objects.
	 * @param array The array to convert.
	 * @return A new stream.
	 */
	public static Stream<Object> toStream(Object array) {
		assertArg(isArray(array), "Arg was not an array.  Type: {0}", cn(array));
		var length = Array.getLength(array);
		return IntStream.range(0, length).mapToObj(i -> Array.get(array, i));
	}

	/**
	 * Converts the specified collection to an array of strings.
	 *
	 * <p>
	 * Entries are converted to strings using {@link #toString()}.
	 * <jk>null</jk> values remain <jk>null</jk>.
	 *
	 * @param c The collection to convert.
	 * @return The collection as a string array.
	 */
	public static String[] toStringArray(Collection<?> c) {
		var r = new String[c.size()];
		var i = 0;
		for (var o : c)
			r[i++] = s(o);
		return r;
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

	//-----------------------------------------------------------------------------------------------------------------
	// Stream utilities
	//-----------------------------------------------------------------------------------------------------------------

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

	private CollectionUtils() {}
}
