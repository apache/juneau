package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Exceptions.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;

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
 *    <li>{@link #array(Object...)} - Creates arrays
 *    <li>{@link #objectArray(Object...)} - Creates Object arrays (when type inference needs help)
 *    <li>{@link #list(Object...)} / {@link #fixedSizeList(Object...)} - Creates modifiable lists
 *    <li>{@link #map(Object, Object, Object, Object, Object, Object)} - Creates modifiable maps
 *    <li>{@link #immutableMap(Object, Object, Object, Object, Object, Object)} - Creates unmodifiable maps
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
 *    <li>Use {@link #array(Object...)} for arrays when type can be inferred, {@link #objectArray(Object...)} for Object arrays
 *    <li>Use {@link #map(Object, Object, Object, Object, Object, Object)} when you need modifiable maps
 *    <li>Use {@link #immutableMap(Object, Object, Object, Object, Object, Object)} when you need immutable/unmodifiable maps
 *    <li>Use {@link #list(Object...)} or {@link #fixedSizeList(Object...)} for modifiable lists
 *    <li>Static import these methods for maximum readability: <code>import static org.apache.juneau.commons.utils.CollectionUtils.*;</code>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">juneau-commons</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class CollectionUtils {

	// Argument name constants for assertArgNotNull
	private static final String ARG_comparator = "comparator";
	private static final String ARG_input = "input";
	private static final String ARG_value = "value";
	private static final String ARG_array = "array";
	private static final String ARG_arrays = "arrays";
	private static final String ARG_values = "values";
	private static final String ARG_o = "o";

	/**
	 * Traverses all elements in the specified objects and accumulates them into a list.
	 *
	 * <p>
	 * Input shapes are processed as follows:
	 * <ul>
	 * 	<li><b>Flattened (recursively):</b> {@link Iterable}, {@link Stream}, and any array type (including all 8 primitive
	 * 		array types via {@link ClassUtils#isArray}), including arbitrary nesting and mixing of these
	 * 		(e.g. a {@code List<int[]>}, an array of {@code Stream}s, etc.).
	 * 	<li><b>Leaf (added as-is):</b> anything else — {@link String}, {@link Map}, custom beans, boxed primitives, etc.
	 * 	<li><b>{@code null} handling:</b> a {@code null} root, or {@code null} encountered mid-traversal, contributes nothing
	 * 		to the result — not even a {@code null} element.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;Object&gt; <jv>nested</jv> = List.of(<jk>new</jk> <jk>int</jk>[]{1, 2}, List.of(<js>"a"</js>, <js>"b"</js>));
	 *
	 * 	<jc>// Returns [1, 2, "a", "b"]</jc>
	 * 	List&lt;Object&gt; <jv>result</jv> = <jsm>accumulate</jsm>(<jv>nested</jv>);
	 *
	 * 	<jc>// Multi-root: returns [1, 2, "a", "b"]</jc>
	 * 	List&lt;Object&gt; <jv>result2</jv> = <jsm>accumulate</jsm>(<jk>new</jk> <jk>int</jk>[]{1, 2}, <js>"a"</js>, <js>"b"</js>);
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param o The objects to traverse. Can be <jk>null</jk> or empty, in which case an empty list is returned.
	 * @return A new modifiable list containing all accumulated leaf elements.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <T> List<T> accumulate(Object... o) {
		var l = list();
		if (o != null)
			for (var x : o)
				traverse(x, l::add);
		return (List<T>)l;
	}

	/**
	 * Traverses all elements in the specified objects and returns them as a lazily-evaluated stream,
	 * without building an intermediate list.
	 *
	 * <p>
	 * Accepts the same input shapes as {@link #accumulate(Object...)} — see that method's Javadoc for the full
	 * list of flattened vs. leaf types and null-handling semantics.
	 *
	 * <p>
	 * Named {@code deepStream} rather than {@code stream} to avoid an erasure conflict with the existing
	 * shallow-array overload {@link #stream(Object[])}, which has the same erased parameter type ({@code Object[]}).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;Object&gt; <jv>nested</jv> = List.of(<jk>new</jk> <jk>int</jk>[]{1, 2}, List.of(<js>"a"</js>, <js>"b"</js>));
	 *
	 * 	<jc>// Lazily yields 1, 2, "a", "b"</jc>
	 * 	<jsm>deepStream</jsm>(<jv>nested</jv>).forEach(System.<jk>out</jk>::println);
	 * </p>
	 *
	 * @param <T> The element type.
	 * @param o The objects to traverse. Can be <jk>null</jk> or empty, in which case an empty stream is returned.
	 * @return A lazily-evaluated stream of all leaf elements.
	 */
	public static <T> Stream<T> deepStream(Object... o) {
		if (o == null)
			return Stream.empty();
		return Arrays.stream(o).flatMap(CollectionUtils::traverseToStream);
	}

	/**
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.  Can be <jk>null</jk> (a new list is created and returned).
	 * @param entries The entries to add.  Can be <jk>null</jk> (treated as a no-op — the value is returned unchanged).
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> List<E> addAll(List<E> value, E...entries) {
		if (isNotNull(entries)) {
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
	 * @param value The collection to add to.  Can be <jk>null</jk> (a new list is created and returned).
	 * @param entries The entries to add.  Can be <jk>null</jk> (treated as a no-op — the value is returned unchanged).
	 * @return The set.
	 */
	public static <E> List<E> addAll(List<E> value, List<E> entries) {
		if (isNotNull(entries)) {
			if (value == null)
				value = new ArrayList<>(entries);
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
	 * @param value The collection to add to.  Can be <jk>null</jk> (a new set is created and returned).
	 * @param entries The entries to add.  Can be <jk>null</jk> (treated as a no-op — the value is returned unchanged).
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> Set<E> addAll(Set<E> value, E...entries) {
		if (isNotNull(entries)) {
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
	 * @param value The collection to add to.  Can be <jk>null</jk> (a new sorted set is created and returned).
	 * @param entries The entries to add.  Can be <jk>null</jk> (treated as a no-op — the value is returned unchanged).
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> SortedSet<E> addAll(SortedSet<E> value, E...entries) {
		if (isNotNull(entries)) {
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
	 * @param array The array to append to.  Can be <jk>null</jk> (the new elements are returned as-is).
	 * @param newElements The new elements to append to the array.  Must not be <jk>null</jk>; if empty, the original array is returned.
	 * @return A new array with the specified elements appended.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <T> T[] addAll(T[] array, T...newElements) {
		if (array == null)
			return newElements;
		if (newElements.length == 0)
			return array;
		var a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.length);
		System.arraycopy(array, 0, a, 0, array.length);
		System.arraycopy(newElements, 0, a, array.length, newElements.length);
		return a;
	}

	/**
	 * Creates an array of the specified component type and length.
	 *
	 * @param <E> The component type of the array.
	 * @param componentType The component type of the array.  Must not be <jk>null</jk>.
	 * @param length The length of the array.
	 * @return A new array of the specified type and length. Never <jk>null</jk>.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <E> E[] array(Class<E> componentType, int length) {
		return (E[])Array.newInstance(componentType, length);
	}

	/**
	 * Converts the specified collection to an array.
	 *
	 * @param <E> The element type.
	 * @param value The collection to convert.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link IllegalArgumentException}).
	 * @param componentType The component type of the array.  Must not be <jk>null</jk>.
	 * @return A new array.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <E> E[] array(Collection<E> value, Class<E> componentType) {
		assertArgNotNull(ARG_value, value);
		var array = (E[])Array.newInstance(componentType, value.size());
		return value.toArray(array);
	}

	/**
	 * Converts any array (including primitive arrays) to a List.
	 *
	 * @param array The array to convert. Can be any array type including primitives.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link IllegalArgumentException}).
	 * @return A List containing the array elements. Primitive values are auto-boxed.  Never <jk>null</jk>.
	 * @throws IllegalArgumentException if the input is <jk>null</jk> or is not an array.
	 */
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for array conversion
	})
	public static List<Object> arrayToList(Object array) {
		assertArgNotNull(ARG_array, array);
		assertArg(ClassUtils.isArray(array), "Input must be an array but was %s", ClassUtils.className(array));

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
	 * Combine an arbitrary number of arrays into a single array.
	 *
	 * @param <E> The element type.
	 * @param arrays Collection of arrays to combine.  Must not be <jk>null</jk> (a <jk>null</jk> array argument throws {@link IllegalArgumentException}); individual <jk>null</jk> array entries are skipped.
	 * @return A new combined array, or an empty array if all arrays are <jk>null</jk>.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <E> E[] combine(E[]...arrays) {
		assertArgNotNull(ARG_arrays, arrays);
		int l = 0;
		E[] a1 = null;
		for (var a : arrays) {
			if (a1 == null && isNotNull(a))
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return (E[]) new Object[0];
		var a = (E[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (var aa : arrays)
			if (isNotNull(aa))
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
	 * @param array The array to copy into a list.  Can be <jk>null</jk> (nothing is copied).
	 * @param list The list to copy the values into.  Must not be <jk>null</jk> when the array is non-<jk>null</jk>.
	 * @return The same list passed in.
	 */
	@SuppressWarnings({
		"unchecked", // Type erasure requires unchecked cast from array
		"rawtypes"   // Raw types necessary for generic array handling
	})
	public static List copyArrayToList(Object array, List list) {
		if (isNotNull(array)) {
			var length = Array.getLength(array);
			for (var i = 0; i < length; i++)
				list.add(Array.get(array, i));
		}
		return list;
	}

	/**
	 * Returns the next value from an iterator, or an empty Optional if there are no more elements.
	 *
	 * <p>
	 * This is a null-safe operation. Returns an empty Optional if the iterator is <jk>null</jk> or
	 * has no more elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Iterator&lt;String&gt; <jv>it</jv> = list.iterator();
	 * 	Optional&lt;String&gt; <jv>first</jv> = <jsm>next</jsm>(<jv>it</jv>);  <jc>// Optional.of("first")</jc>
	 * 	Optional&lt;String&gt; <jv>second</jv> = <jsm>next</jsm>(<jv>it</jv>);  <jc>// Optional.of("second")</jc>
	 * 	Optional&lt;String&gt; <jv>empty</jv> = <jsm>next</jsm>(<jv>it</jv>);   <jc>// Optional.empty()</jc>
	 * 	Optional&lt;String&gt; <jv>nullResult</jv> = <jsm>next</jsm>(<jk>null</jk>);  <jc>// Optional.empty()</jc>
	 * </p>
	 *
	 * @param <E> The element type.
	 * @param iterator The iterator. Can be <jk>null</jk>.
	 * @return An Optional containing the next element, or empty if the iterator is <jk>null</jk> or has no more elements.
	 */
	public static <E> Optional<E> next(Iterator<? extends E> iterator) {
		if (iterator == null || !iterator.hasNext())
			return emptyOptional();
		return optional(iterator.next());
	}

	/**
	 * Returns the first element in an iterable.
	 *
	 * <p>
	 * This is a null-safe operation. Returns an empty Optional if the iterable is <jk>null</jk> or empty.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>list</jv> = <jsm>l</jsm>(<js>"a"</js>, <js>"b"</js>, <js>"c"</js>);
	 * 	Optional&lt;String&gt; <jv>first</jv> = <jsm>first</jsm>(<jv>list</jv>);  <jc>// Optional.of("a")</jc>
	 *
	 * 	Set&lt;Integer&gt; <jv>set</jv> = <jsm>s</jsm>(1, 2, 3);
	 * 	Optional&lt;Integer&gt; <jv>firstInt</jv> = <jsm>first</jsm>(<jv>set</jv>);  <jc>// Optional.of(1) or another element</jc>
	 *
	 * 	EnumSet&lt;MyEnum&gt; <jv>enumSet</jv> = EnumSet.allOf(MyEnum.<jk>class</jk>);
	 * 	Optional&lt;MyEnum&gt; <jv>firstEnum</jv> = <jsm>first</jsm>(<jv>enumSet</jv>);  <jc>// Optional.of(first enum value)</jc>
	 *
	 * 	Optional&lt;String&gt; <jv>empty</jv> = <jsm>first</jsm>(Collections.emptyList());  <jc>// Optional.empty()</jc>
	 * 	Optional&lt;String&gt; <jv>nullResult</jv> = <jsm>first</jsm>(<jk>null</jk>);  <jc>// Optional.empty()</jc>
	 * </p>
	 *
	 * @param <E> The element type.
	 * @param iterable The iterable. Can be <jk>null</jk>.
	 * @return An Optional containing the first element, or empty if the iterable is <jk>null</jk> or empty.
	 */
	public static <E> Optional<E> first(Iterable<? extends E> iterable) {
		if (iterable == null)
			return emptyOptional();
		Iterator<? extends E> iterator = iterable.iterator();
		return next(iterator);
	}

	/**
	 * Iterates the specified array in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The array to iterate.  Must not be <jk>null</jk>.
	 * @param action The action to perform.  Must not be <jk>null</jk>.
	 */
	public static <E> void forEachReverse(E[] value, Consumer<E> action) {
		for (var i = value.length - 1; i >= 0; i--)
			action.accept(value[i]);
	}

	/**
	 * Iterates the specified list in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The list to iterate.  Must not be <jk>null</jk>.
	 * @param action The action to perform.  Must not be <jk>null</jk>.
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
			if (equal(element, array[i]))
				return i;
		return -1;
	}

	/**
	 * Returns <jk>true</jk> if the specified collection is null or empty.
	 *
	 * @param c The collection to check.
	 * @return <jk>true</jk> if the specified collection is null or empty.
	 */
	public static boolean isEmpty(Collection<?> c) {
		return c == null || c.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified map is null or empty.
	 *
	 * @param m The map to check.
	 * @return <jk>true</jk> if the specified map is null or empty.
	 */
	public static boolean isEmpty(Map<?,?> m) {
		return m == null || m.isEmpty();
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
		return isNotNull(array) && array.length > 0;
	}

	/**
	 * Creates a fixed-size list backed by the specified array (via {@link Arrays#asList(Object...)}).
	 *
	 * <p>
	 * The returned list is fixed-size: element values can be replaced via {@link List#set(int, Object)},
	 * but elements cannot be added or removed. Returns <jk>null</jk> if the input array is <jk>null</jk>.
	 * This differs from {@link #list(Object...)}, which returns a fully-modifiable {@link ArrayList}.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the list.
	 * @return A fixed-size list containing the specified values, or <jk>null</jk> if the input is <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> List<T> fixedSizeList(T...values) {
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
	 * @return A modifiable list containing the specified values, or <jk>null</jk> if the values array itself is <jk>null</jk>.
	 */
	@SafeVarargs
	public static <T> List<T> list(T...values) {
		return values == null ? null : new ArrayList<>(fixedSizeList(values));
	}

	/**
	 * Convenience factory for a {@link Lists}.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @return A new list builder.
	 */
	public static <E> Lists<E> listBuilder(Class<E> type) {
		return Lists.create(type);
	}

	/**
	 * Returns a null list.
	 *
	 * <p>Intentional null return for assertion testing.
	 *
	 * @param <T> The element type.
	 * @param type The element type class.
	 * @return <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168",    // Intentional null return for assertion testing.
		"java:S1172"     // Parameter type is used for type inference, not runtime behavior
	})
	public static <T> List<T> nullList(Class<T> type) {
		return null;
	}

	/**
	 * Convenience method for creating an {@link ArrayList}.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param values The values to initialize the list with.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return A new modifiable list, or <jk>null</jk> if the values array itself is <jk>null</jk>.
	 */
	@SafeVarargs
	public static <E> List<E> listOfType(Class<E> elementType, E...values) {
		return list(values);
	}

	/**
	 * Convenience method for creating an {@link ArrayList} of the specified size.
	 *
	 * @param <E> The element type.
	 * @param size The initial size of the list.
	 * @return A new modifiable list.
	 */
	public static <E> List<E> listOfSize(int size) {
		return new ArrayList<>(size);
	}

	/**
	 * Convenience method for creating an unmodifiable map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new unmodifiable empty map.
	 */
	public static <K,V> Map<K,V> immutableMap() {
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
	public static <K,V> Map<K,V> immutableMap(K k1, V v1) {
		return new SimpleMap<>(array(k1), array(v1));
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
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2) {
		return new SimpleMap<>(array(k1, k2), array(v1, v2));
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
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3) {
		return new SimpleMap<>(array(k1, k2, k3), array(v1, v2, v3));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
		return new SimpleMap<>(array(k1, k2, k3, k4), array(v1, v2, v3, v4));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5), array(v1, v2, v3, v4, v5));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5, k6), array(v1, v2, v3, v4, v5, v6));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5, k6, k7), array(v1, v2, v3, v4, v5, v6, v7));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5, k6, k7, k8), array(v1, v2, v3, v4, v5, v6, v7, v8));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5, k6, k7, k8, k9), array(v1, v2, v3, v4, v5, v6, v7, v8, v9));
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> immutableMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
		return new SimpleMap<>(array(k1, k2, k3, k4, k5, k6, k7, k8, k9, k10), array(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10));
	}

	/**
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> Map<K,V> map() {
		return new LinkedHashMap<>();
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
	public static <K,V> Map<K,V> map(K k1, V v1) {
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
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2) {
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
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
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
	@SuppressWarnings({
		"java:S107" // Many parameters acceptable for convenience method
	})
	public static <K,V> Map<K,V> map(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
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
	 * Convenience factory for a {@link Maps} builder.
	 *
	 * <p>
	 * This is a shortcut for <c>Maps.create().ordered()</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String,Object&gt; <jv>map</jv> = mapBuilder()
	 * 		.add(<js>"foo"</js>, 1)
	 * 		.add(<js>"bar"</js>, 2)
	 * 		.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new map builder.
	 * @see Maps
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	public static <K,V> Maps<K,V> mapBuilder() {
		return (Maps<K,V>)Maps.create().ordered();
	}

	/**
	 * Convenience factory for a {@link Maps} with {@link String} keys and {@link Object} values.
	 *
	 * <p>
	 * This is a shortcut for <c>Maps.create(String.<jk>class</jk>, Object.<jk>class</jk>)</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String,Object&gt; <jv>map</jv> = mapBuilder()
	 * 		.add(<js>"foo"</js>, 1)
	 * 		.add(<js>"bar"</js>, 2)
	 * 		.build();
	 * </p>
	 *
	 * <p>
	 * This builder supports optional filtering to automatically exclude unwanted values:
	 * <p class='bjava'>
	 * 	<jc>// Build a map excluding null and empty values</jc>
	 * 	Map&lt;String,Object&gt; <jv>map</jv> = mapBuilder().filtered()
	 * 		.add(<js>"foo"</js>, <jk>null</jk>)       <jc>// Excluded</jc>
	 * 		.add(<js>"bar"</js>, <js>""</js>)         <jc>// Excluded</jc>
	 * 		.add(<js>"baz"</js>, <js>"value"</js>)    <jc>// Included</jc>
	 * 		.build();
	 * </p>
	 *
	 * @return A new map builder.
	 * @see Maps
	 */
	@SuppressWarnings({
		"java:S100" // Method name uses underscore convention
	})
	public static Maps<String,Object> mapb_so() {
		return Maps.create(String.class, Object.class).ordered();
	}

	/**
	 * Convenience factory for a filtered, sorted, fluent map with {@link String} keys and {@link Object} values.
	 *
	 * <p>
	 * This is a shortcut for <c>Maps.create(String.<jk>class</jk>, Object.<jk>class</jk>).filtered().sorted().buildFluent()</c>.
	 *
	 * <p>
	 * This is typically used for creating property maps in <c>toString()</c> methods that need to be sorted and filtered.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// In a properties() method</jc>
	 * 	<jk>return</jk> filteredBeanPropertyMap()
	 * 		.a(<js>"name"</js>, <jv>name</jv>)
	 * 		.a(<js>"value"</js>, <jv>value</jv>);
	 * </p>
	 *
	 * @return A new filtered, sorted, fluent map builder.
	 * @see FluentMap
	 * @see Maps
	 */
	public static FluentMap<String,Object> filteredBeanPropertyMap() {
		return Maps.create(String.class, Object.class).filtered().sorted().buildFluent();
	}

	/**
	 * Convenience factory for a {@link Maps}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new map builder.
	 */
	public static <K,V> Maps<K,V> mapBuilder(Class<K> keyType, Class<V> valueType) {
		return Maps.create(keyType, valueType).ordered();
	}

	/**
	 * Returns a null map.
	 *
	 * <p>Intentional null return for assertion testing.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type class.
	 * @param valueType The value type class.
	 * @return <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168",    // Intentional null return for assertion testing. Consider Optional.
		"java:S1172"    // Parameters required for type inference in public API
	})
	public static <K,V> Map<K,V> nullMap(Class<K> keyType, Class<V> valueType) {
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
	@SuppressWarnings({
		"java:S1172" // Parameters required for type inference in public API
	})
	public static <K,V> Map<K,V> mapOfType(Class<K> keyType, Class<V> valueType) {
		return map();
	}

	/**
	 * Adds all the specified values to the specified collection.
	 * Creates a new set if the value is <jk>null</jk>.
	 *
	 * @param <E> The element type.
	 * @param value The collection to add to.  Can be <jk>null</jk> (a new list is created and returned).
	 * @param entries The entries to add.  Can be <jk>null</jk> (treated as a no-op — the value is returned unchanged).
	 * @return The set.
	 */
	@SafeVarargs
	public static <E> List<E> prependAll(List<E> value, E...entries) {
		if (isNotNull(entries)) {
			if (value == null)
				value = list(entries);
			else
				value.addAll(0, fixedSizeList(entries));
		}
		return value;
	}

	/**
	 * Reverses the elements in the specified array in-place.
	 *
	 * @param <E> The element type.
	 * @param array The array to reverse.  Must not be <jk>null</jk>.
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
	 * @param list The list to reverse.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link IllegalArgumentException}).
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
	 * Removes negation tokens from a list.
	 *
	 * <p>
	 * A negation token is a string starting with <js>'-'</js> followed by at least one character.
	 * When encountered, it removes the first prior occurrence of the corresponding positive token.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	List&lt;String&gt; <jv>result</jv> = <jsm>removeNegations</jsm>(list(<js>"a"</js>, <js>"b"</js>, <js>"-a"</js>, <js>"c"</js>));
	 * 	<jc>// Produces: ["b", "c"]</jc>
	 * </p>
	 *
	 * @param input The input list. Cannot be <jk>null</jk>.
	 * @return The list with negation tokens applied, or the original list if no negation tokens were present.
	 */
	public static List<String> removeNegations(List<String> input) {
		assertArgNotNull(ARG_input, input);
		var hasNegation = false;
		for (var token : input) {
			if (token != null && token.length() > 1 && token.charAt(0) == '-') {
				hasNegation = true;
				break;
			}
		}
		if (!hasNegation)
			return input;
		var out = new ArrayList<String>(input.size());
		for (var token : input) {
			if (token != null && token.length() > 1 && token.charAt(0) == '-')
				out.remove(token.substring(1));
			else
				out.add(token);
		}
		return out;
	}

	/**
	 * Creates a {@link TreeSet} with a custom comparator from a collection of elements.
	 *
	 * <p>
	 * Null elements in the collection are silently skipped.
	 *
	 * @param <E> The element type.
	 * @param comparator The comparator to use for ordering. Cannot be <jk>null</jk>.
	 * @param elements The initial elements. Can be <jk>null</jk> (treated as empty).
	 * @return A new {@link TreeSet} containing all non-null elements from the collection.
	 */
	public static <E> SortedSet<E> treeSet(Comparator<? super E> comparator, Collection<? extends E> elements) {
		assertArgNotNull(ARG_comparator, comparator);
		var s = new TreeSet<E>(comparator);
		if (elements != null)
			for (var e : elements)
				if (e != null)
					s.add(e);
		return s;
	}

	/**
	 * Creates a case-insensitive {@link TreeSet} from a collection of strings.
	 *
	 * <p>
	 * Equivalent to <c>treeSet(String.CASE_INSENSITIVE_ORDER, elements)</c>.
	 * Null elements in the collection are silently skipped.
	 *
	 * @param elements The initial elements. Can be <jk>null</jk> (treated as empty).
	 * @return A new case-insensitive {@link TreeSet} containing all non-null elements from the collection.
	 */
	public static SortedSet<String> treeSetCi(Collection<String> elements) {
		return treeSet(String.CASE_INSENSITIVE_ORDER, elements);
	}

	/**
	 * Convenience factory for a {@link Sets}.
	 *
	 * @param <E> The element type.
	 * @param type The element type.
	 * @return A new set builder.
	 */
	public static <E> Sets<E> setBuilder(Class<E> type) {
		return Sets.create(type).ordered();
	}

	/**
	 * Convenience method for creating a {@link LinkedHashSet}.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param values The values to initialize the set with.  Must not be <jk>null</jk> (a <jk>null</jk> array throws {@link IllegalArgumentException}).
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> Set<E> setOfType(Class<E> elementType, E...values) {
		return set(values);
	}

	/**
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param comparator The comparator to use to sort the list.  Can be <jk>null</jk> (natural ordering is used).
	 * @param value The values to initialize the list with.  Can be <jk>null</jk> (returns an empty list).
	 * @return A new modifiable list.
	 */
	public static <E> List<E> sortedList(Comparator<E> comparator, Collection<E> value) {
		List<E> l = toList(value);
		Collections.sort(l, comparator);
		return l;
	}

	/**
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param comparator The comparator to use to sort the list.  Can be <jk>null</jk> (natural ordering is used).
	 * @param values The values to initialize the list with.  Must not be <jk>null</jk>.
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
	 * @param values The values to initialize the list with.  Must not be <jk>null</jk>.
	 * @return A new modifiable list.
	 */
	@SuppressWarnings({
		"rawtypes",  // Raw types necessary for varargs handling
		"unchecked"  // Type erasure requires unchecked operations
	})
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
	public static <K,V> SortedMap<K,V> sortedMap() {
		return new TreeMap<>();
	}

	/**
	 * Convenience method for creating a {@link TreeSet}.
	 *
	 * <p>
	 * Note: <jk>null</jk> values in the varargs array are automatically filtered out
	 * because {@link TreeSet} does not support <jk>null</jk> elements.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the set with.
	 * 	<br>Must not be <jk>null</jk> (a <jk>null</jk> array throws {@link IllegalArgumentException}).
	 * 	<br>Can contain <jk>null</jk> values (they will be filtered out).
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> SortedSet<E> sortedSet(E...values) {
		assertArgNotNull(ARG_values, values);
		var l = new TreeSet<E>();
		for (var v : values)
			if (v != null)
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
	 * Wraps the specified list in a synchronized (thread-safe) view via {@link Collections#synchronizedList(List)}.
	 *
	 * @param <E> The element type.
	 * @param value The list to wrap.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return A synchronized view of the list, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168"     // Pass-through null by design. Consider empty list.
	})
	public static <E> List<E> synced(List<E> value) {
		return value == null ? null : Collections.synchronizedList(value);
	}

	/**
	 * Wraps the specified map in a synchronized (thread-safe) view via {@link Collections#synchronizedMap(Map)}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The map to wrap.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return A synchronized view of the map, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168"     // Pass-through null by design. Consider empty map.
	})
	public static <K,V> Map<K,V> synced(Map<K,V> value) {
		return value == null ? null : Collections.synchronizedMap(value);
	}

	/**
	 * Wraps the specified set in a synchronized (thread-safe) view via {@link Collections#synchronizedSet(Set)}.
	 *
	 * @param <E> The element type.
	 * @param value The set to wrap.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return A synchronized view of the set, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168"     // Pass-through null by design. Consider empty set.
	})
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
	 * @param c The collection to convert to an array.  Must not be <jk>null</jk>.
	 * @param elementType The component type of the collection.  Must not be <jk>null</jk>.
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
	 * @param value The collection to copy from.  Can be <jk>null</jk> (returns an empty list).
	 * @return A new modifiable list.
	 */
	public static <E> List<E> toList(Collection<E> value) {
		return toList(value, false);
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <E> The element type.
	 * @param value The collection to copy from.  Can be <jk>null</jk>.
	 * @param nullIfEmpty If <jk>true</jk> will return <jk>null</jk> if the collection is empty or null.
	 * @return A new modifiable list, or an empty list if the input was <jk>null</jk> (when nullIfEmpty is false), or <jk>null</jk> if nullIfEmpty and the input was null or empty.
	 */
	@SuppressWarnings({
		"java:S1168"     // Intentional null when nullIfEmpty and (null or empty).
	})
	public static <E> List<E> toList(Collection<E> value, boolean nullIfEmpty) {
		if (value == null)
			return nullIfEmpty ? null : new ArrayList<>();
		if (nullIfEmpty && value.isEmpty())
			return null;
		var l = new ArrayList<E>();
		value.forEach(l::add);
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
	 * @see #arrayToList(Object)
	 */
	@SuppressWarnings({
		"java:S1452"  // Wildcard required - List<?> for heterogeneous collections from various sources
	})
	public static final List<?> toList(Object o) {
		assertArgNotNull(ARG_o, o);
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
		if (ClassUtils.isArray(o))
			return arrayToList(o);
		throw rex("Could not convert object of type %s to a list", ClassUtils.className(o));
	}

	/**
	 * Converts the specified array to an <c>ArrayList</c>
	 *
	 * @param <E> The element type.
	 * @param array The array to convert.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link NullPointerException}).
	 * @param elementType
	 * 	The type of objects in the array.
	 * 	It must match the actual component type in the array.
	 * @return A new {@link ArrayList}
	 */
	@SuppressWarnings({
		"unchecked", // Parameter elementType is used for type inference, not runtime behavior
		"java:S1172", // Unused parameters kept for API consistency or framework requirements
	})
	public static <E> List<E> toList(Object array, Class<E> elementType) {
		var l = new ArrayList<E>(Array.getLength(array));
		for (var i = 0; i < Array.getLength(array); i++)
			l.add((E)Array.get(array, i));
		return l;
	}

	/**
	 * Recursively converts the specified array into a list of objects.
	 *
	 * @param array The array to convert.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link NullPointerException}).
	 * @return A new {@link ArrayList}
	 */
	public static List<Object> toObjectList(Object array) {
		var l = new ArrayList<>(Array.getLength(array));
		for (var i = 0; i < Array.getLength(array); i++) {
			var o = Array.get(array, i);
			if (ClassUtils.isArray(o))
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
	 * @param array The array being wrapped in a <c>Set</c> interface.  Must not be <jk>null</jk> (a <jk>null</jk> argument throws {@link IllegalArgumentException}).
	 * @return The new set.
	 */
	public static <T> Set<T> toSet(T[] array) {
		assertArgNotNull(ARG_array, array);
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
						throw uoex();
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
	 * @return A new {@link TreeSet}, or an empty {@link TreeSet} if the input was <jk>null</jk>.
	 */
	public static <E> SortedSet<E> toSortedSet(Collection<E> value) {
		if (value == null)
			return new TreeSet<>();
		var l = new TreeSet<E>();
		value.forEach(l::add);
		return l;
	}

	/**
	 * Creates a new {@link TreeSet} from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param value The value to copy from.
	 * @param nullIfEmpty If <jk>true</jk> returns <jk>null</jk> if the collection is empty.
	 * @return A new {@link TreeSet}, or an empty {@link TreeSet} if the input was <jk>null</jk>, or <jk>null</jk> if nullIfEmpty and the collection is empty.
	 */
	@SuppressWarnings({
		"java:S1168"     // Intentional null when nullIfEmpty and empty.
	})
	public static <E> SortedSet<E> toSortedSet(Collection<E> value, boolean nullIfEmpty) {
		if (value == null)
			return new TreeSet<>();
		if (nullIfEmpty && value.isEmpty())
			return null;
		var l = new TreeSet<E>();
		value.forEach(l::add);
		return l;
	}

	/**
	 * Creates a new {@link TreeSet} containing a copy of the specified set.
	 *
	 * @param <T> The element type.
	 * @param copyFrom The set to copy from.
	 * @return A new {@link TreeSet}, or an empty {@link TreeSet} if the input was <jk>null</jk>.
	 */
	public static <T> SortedSet<T> toSortedSet(Set<T> copyFrom) {
		return copyFrom == null ? new TreeSet<>() : new TreeSet<>(copyFrom);
	}

	/**
	 * Converts an array to a stream of objects.
	 * @param array The array to convert.  Must not be <jk>null</jk> (a <jk>null</jk> or non-array argument throws {@link IllegalArgumentException}).
	 * @return A new stream.
	 */
	public static Stream<Object> toStream(Object array) {
		assertArg(ClassUtils.isArray(array), "Arg was not an array.  Type: %s", ClassUtils.className(array));
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
	 * @param c The collection to convert.  Must not be <jk>null</jk>.
	 * @return The collection as a string array.
	 */
	public static String[] toStringArray(Collection<?> c) {
		var r = new String[c.size()];
		var i = 0;
		for (var o : c)
			r[i++] = stringify(o);
		return r;
	}

	/**
	 * Traverses all elements in the specified object and executes a consumer for each leaf.
	 *
	 * <p>
	 * Input shapes are processed as follows:
	 * <ul>
	 * 	<li><b>Flattened (recursively):</b> {@link Iterable}, {@link Stream}, and any array type (including all 8 primitive
	 * 		array types via {@link ClassUtils#isArray}), including arbitrary nesting and mixing of these.
	 * 	<li><b>Leaf (consumer called once):</b> anything else — {@link String}, {@link Map}, custom beans, boxed primitives, etc.
	 * 	<li><b>{@code null} handling:</b> a {@code null} root, or {@code null} encountered mid-traversal, causes the
	 * 		consumer to be called zero times — no {@code null} elements are ever delivered.
	 * </ul>
	 *
	 * @param <T> The element type.
	 * @param o The object to traverse.  Can be <jk>null</jk> (the consumer is not called).
	 * @param c The consumer of the leaf elements.  Must not be <jk>null</jk>.
	 */
	public static <T> void traverse(Object o, Consumer<T> c) {
		CollectionUtils.<T>traverseToStream(o).forEach(c);
	}

	/**
	 * Shared recursive engine for {@link #traverse(Object, Consumer)}, {@link #accumulate(Object...)}, and
	 * {@link #deepStream(Object...)}.
	 *
	 * <p>
	 * Returns a lazily-evaluated {@link Stream} that recursively flattens {@link Iterable}, {@link Stream}, and
	 * array inputs (including all primitive array types), treating everything else as a single leaf element.
	 * {@code null} inputs return an empty stream.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires unchecked casts
	})
	private static <T> Stream<T> traverseToStream(Object o) {
		if (o == null)
			return Stream.empty();
		if (o instanceof Iterable<?> o2)
			return StreamSupport.stream(o2.spliterator(), false).flatMap(CollectionUtils::traverseToStream);
		if (o instanceof Stream<?> o2)
			return o2.flatMap(CollectionUtils::traverseToStream);
		if (ClassUtils.isArray(o))
			return toStream(o).flatMap(CollectionUtils::traverseToStream);
		return Stream.of((T)o);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Stream utilities
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Full-named factory/query methods (canonical forms; short-named methods delegate to these)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an array of objects (varargs form).
	 *
	 * @param <T> The component type of the array.
	 * @param x The objects to place in the array.  Can be <jk>null</jk> (returned as-is).
	 * @return A new array containing the specified objects.
	 */
	@SafeVarargs
	public static <T> T[] array(T...x) {
		return x;
	}

	/**
	 * Creates a 2-dimensional array.
	 *
	 * @param <E> The element type of the inner arrays.
	 * @param value The 1D arrays that will become rows in the 2D array.  Can be <jk>null</jk> (returned as-is).
	 * @return A 2D array containing the specified rows.
	 */
	@SafeVarargs
	public static <E> E[][] array2d(E[]...value) {
		return value;
	}

	/**
	 * Creates an Object array.
	 *
	 * @param value The objects to place in the array.  Can be <jk>null</jk> (returned as-is).
	 * @return A new {@code Object[]} containing the specified objects.
	 */
	public static Object[] objectArray(Object...value) {
		return value;
	}

	/**
	 * Returns null for the specified array type.
	 *
	 * <p>Intentional null return for assertion testing.
	 *
	 * @param <T> The component type.
	 * @param type The component type class.
	 * @return <jk>null</jk>.
	 */
	@SuppressWarnings({
		"java:S1168",  // Intentional null return for assertion testing.
		"java:S1172"   // Parameter type is used for type inference, not runtime behavior
	})
	public static <T> T[] nullArray(Class<T> type) {
		return null;
	}

	/**
	 * Creates a LinkedList from the specified values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the list.  Must not be <jk>null</jk> (a <jk>null</jk> array throws {@link NullPointerException}).
	 * @return A new modifiable LinkedList containing the specified values.
	 */
	@SafeVarargs
	public static <T> List<T> linkedList(T...values) {
		return new LinkedList<>(fixedSizeList(values));
	}

	/**
	 * Creates a HashSet from the specified values.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the set.  Must not be <jk>null</jk> (a <jk>null</jk> array throws {@link NullPointerException}).
	 * @return A new modifiable HashSet containing the specified values.
	 */
	@SafeVarargs
	public static <T> Set<T> hashSet(T...values) {
		return new HashSet<>(Arrays.asList(values));
	}

	/**
	 * Creates an unmodifiable view of the specified list.
	 *
	 * @param <T> The element type.
	 * @param value The list to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the list, or null if the input was null.
	 */
	public static <T> List<T> unmodifiable(List<? extends T> value) {
		return value == null ? null : Collections.unmodifiableList(value);
	}

	/**
	 * Creates an unmodifiable view of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The map to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the map, or null if the input was null.
	 */
	public static <K,V> Map<K,V> unmodifiable(Map<? extends K,? extends V> value) {
		return value == null ? null : Collections.unmodifiableMap(value);
	}

	/**
	 * Creates an unmodifiable view of the specified set.
	 *
	 * @param <T> The element type.
	 * @param value The set to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the set, or null if the input was null.
	 */
	public static <T> Set<T> unmodifiable(Set<? extends T> value) {
		return value == null ? null : Collections.unmodifiableSet(value);
	}

	/**
	 * Creates an unmodifiable view of the specified sorted set.
	 *
	 * @param <T> The element type.
	 * @param value The sorted set to make unmodifiable. Can be null.
	 * @return An unmodifiable view of the sorted set, or null if the input was null.
	 */
	public static <T> SortedSet<T> unmodifiable(SortedSet<T> value) {
		return value == null ? null : Collections.unmodifiableSortedSet(value);
	}

	/**
	 * Returns the element at the specified index, wrapped in an {@link Optional}.
	 *
	 * @param <E> The element type.
	 * @param l The list. Can be <jk>null</jk>.
	 * @param index The index to access.
	 * @return An Optional containing the element, or an empty Optional if null/out-of-bounds.
	 */
	public static <E> Optional<E> optionalAt(List<E> l, int index) {
		return Optional.ofNullable(elementAt(l, index));
	}

	/**
	 * Returns <jk>null</jk> if the specified map is <jk>null</jk> or empty, otherwise returns the map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param val The map to check.
	 * @return <jk>null</jk> if the map is <jk>null</jk> or empty, otherwise the map itself.
	 */
	@SuppressWarnings({ "java:S1168" // Intentional null return.
	})
	public static <K,V> Map<K,V> nullIfEmpty(Map<K,V> val) {
		return isEmpty(val) ? null : val;
	}

	/**
	 * Returns <jk>null</jk> if the specified list is <jk>null</jk> or empty, otherwise returns the list.
	 *
	 * @param <E> The element type.
	 * @param val The list to check.
	 * @return <jk>null</jk> if the list is <jk>null</jk> or empty, otherwise the list itself.
	 */
	@SuppressWarnings({ "java:S1168" // Intentional null return.
	})
	public static <E> List<E> nullIfEmpty(List<E> val) {
		return isEmpty(val) ? null : val;
	}

	/**
	 * Returns <jk>null</jk> if the specified set is <jk>null</jk> or empty, otherwise returns the set.
	 *
	 * @param <E> The element type.
	 * @param val The set to check.
	 * @return <jk>null</jk> if the set is <jk>null</jk> or empty, otherwise the set itself.
	 */
	@SuppressWarnings({ "java:S1168" // Intentional null return.
	})
	public static <E> Set<E> nullIfEmpty(Set<E> val) {
		return isEmpty(val) ? null : val;
	}

	/**
	 * Returns <jk>true</jk> if the specified collection is not null and not empty.
	 *
	 * @param c The collection to check.
	 * @return <jk>true</jk> if not null and not empty.
	 */
	public static boolean isNotEmpty(Collection<?> c) {
		return c != null && ! c.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified map is not null and not empty.
	 *
	 * @param m The map to check.
	 * @return <jk>true</jk> if not null and not empty.
	 */
	public static boolean isNotEmpty(Map<?,?> m) {
		return m != null && ! m.isEmpty();
	}

	/**
	 * Returns the element at the specified index.
	 *
	 * @param <E> The element type.
	 * @param l The list. Can be null.
	 * @param index The index to access.
	 * @return The element, or null if out-of-bounds/null list.
	 */
	@SuppressWarnings({ "java:S1168" // Intentional null return.
	})
	public static <E> E elementAt(List<E> l, int index) {
		if (l == null || index < 0 || index >= l.size())
			return null;
		return l.get(index);
	}

	/**
	 * Creates a new modifiable LinkedHashSet.
	 *
	 * @param <T> The element type.
	 * @param values The values to add to the set.  Must not be <jk>null</jk> (a <jk>null</jk> array throws {@link IllegalArgumentException}).
	 * @return A modifiable LinkedHashSet containing the specified values.
	 */
	@SafeVarargs
	public static <T> Set<T> set(T...values) {
		assertArgNotNull(ARG_values, values);
		return new LinkedHashSet<>(Arrays.asList(values));
	}

	/**
	 * Creates an int array.
	 *
	 * @param value The values.
	 * @return A new int array.
	 */
	public static int[] intArray(int...value) { return value; }

	/**
	 * Creates a long array.
	 *
	 * @param value The values.
	 * @return A new long array.
	 */
	public static long[] longArray(long...value) { return value; }

	/**
	 * Creates a byte array from ints.
	 *
	 * @param value The int values to convert to bytes.
	 * @return A new byte array.
	 */
	public static byte[] byteArray(int...value) {
		var result = new byte[value.length];
		for (var i = 0; i < value.length; i++)
			result[i] = (byte)value[i];
		return result;
	}

	/**
	 * Creates a char array.
	 *
	 * @param value The values.
	 * @return A new char array.
	 */
	public static char[] charArray(char...value) { return value; }

	/**
	 * Creates a float array.
	 *
	 * @param value The values.
	 * @return A new float array.
	 */
	public static float[] floatArray(float...value) { return value; }

	/**
	 * Creates a double array.
	 *
	 * @param value The values.
	 * @return A new double array.
	 */
	public static double[] doubleArray(double...value) { return value; }

	/**
	 * Creates a boolean array.
	 *
	 * @param value The values.
	 * @return A new boolean array.
	 */
	public static boolean[] booleanArray(boolean...value) { return value; }

	/**
	 * Creates a short array from ints.
	 *
	 * @param value The int values to convert to shorts.
	 * @return A new short array.
	 */
	public static short[] shortArray(int...value) {
		var result = new short[value.length];
		for (var i = 0; i < value.length; i++)
			result[i] = (short)value[i];
		return result;
	}

	private CollectionUtils() {}
}
