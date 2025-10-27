package org.apache.juneau.common.utils;

import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.collections.*;

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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">juneau-common</a>
 * </ul>
 */
public class CollectionUtils {

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
		if (l == null)
			return null;  // NOSONAR - Intentional.
		return l.stream().map(valueMapper).collect(toCollection(listFactory));
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
		if (m == null)
			return null;  // NOSONAR - Intentional.
		return m.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> valueMapper.apply(e.getValue()), (a, b) -> b, mapFactory));
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
		if (l == null)
			return null;  // NOSONAR - Intentional.
		return l.stream().map(valueMapper).collect(toCollection(setFactory));
	}

	/**
	 * Iterates the specified array in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The array to iterate.
	 * @param action The action to perform.
	 */
	public static <E> void forEachReverse(E[] value, Consumer<E> action) {
		for (int i = value.length - 1; i >= 0; i--)
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
			for (int i = value.size() - 1; i >= 0; i--)
				action.accept(value.get(i));
		} else {
			ListIterator<E> i = value.listIterator(value.size());
			while (i.hasPrevious())
				action.accept(i.previous());
		}
	}

	/**
	 * Returns the last entry in an array.
	 *
	 * @param <E> The element type.
	 * @param l The array.
	 * @return The last element, or <jk>null</jk> if the array is <jk>null</jk> or empty.
	 */
	public static <E> E last(E[] l) {
		if (l == null || l.length == 0)
			return null;
		return l[l.length - 1];
	}

	/**
	 * Returns the last entry in a list.
	 *
	 * @param <E> The element type.
	 * @param l The list.
	 * @return The last element, or <jk>null</jk> if the list is <jk>null</jk> or empty.
	 */
	public static <E> E last(List<E> l) {
		if (l == null || l.isEmpty())
			return null;
		return l.get(l.size() - 1);
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
		ArrayList<E> l = new ArrayList<>();
		value.forEach(x -> l.add(x));
		return l;
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
	 * Convenience method for creating a {@link LinkedHashMap}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> map() {
		LinkedHashMap<K,V> m = new LinkedHashMap<>();
		return m;
	}

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
		LinkedHashMap<K,V> m = new LinkedHashMap<>();
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
		LinkedHashMap<K,V> m = new LinkedHashMap<>();
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
		LinkedHashMap<K,V> m = new LinkedHashMap<>();
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
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> mapOf(Class<K> keyType, Class<V> valueType) {
		return map();
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
				value.addAll(0, alist(entries));
		}
		return value;
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
		TreeSet<E> l = new TreeSet<>();
		for (E v : values)
			l.add(v);
		return l;
	}

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
		TreeSet<E> l = new TreeSet<>();
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
		TreeSet<E> l = new TreeSet<>();
		value.forEach(x -> l.add(x));
		return l;
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
	 * Creates a new {@link TreeSet} containing a copy of the specified set.
	 *
	 * @param <T> The element type.
	 * @param copyFrom The set to copy from.
	 * @return A new {@link TreeSet}, or <jk>null</jk> if the set was <jk>null</jk>.
	 */
	public static <T> TreeSet<T> toSortedSet(Set<T> copyFrom) {
		return copyFrom == null ? null : new TreeSet<>(copyFrom);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Array utilities
	//-----------------------------------------------------------------------------------------------------------------

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
		T[] a = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + newElements.length);
		for (int i = 0; i < array.length; i++)
			a[i] = array[i];
		for (int i = 0; i < newElements.length; i++)
			a[i + array.length] = newElements[i];
		return a;
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
	public static <T> Set<T> toSet(final T[] array) {
		AssertionUtils.assertArgNotNull("array", array);
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
						throw new UnsupportedOperationException("Not supported.");
					}
				};
			}

			@Override /* Overridden from Set */
			public int size() {
				return array.length;
			}
		};
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
		AssertionUtils.assertArgNotNull("arrays", arrays);
		int l = 0;
		E[] a1 = null;
		for (E[] a : arrays) {
			if (a1 == null && nn(a))
				a1 = a;
			l += (a == null ? 0 : a.length);
		}
		if (a1 == null)
			return null;
		E[] a = (E[])Array.newInstance(a1.getClass().getComponentType(), l);
		int i = 0;
		for (E[] aa : arrays)
			if (nn(aa))
				for (E t : aa)
					a[i++] = t;
		return a;
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
		for (int i = 0; i < array.length; i++)
			if (eq(element, array[i]))
				return i;
		return -1;
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
	 * Returns <jk>true</jk> if the following sorted arrays are equals.
	 *
	 * @param a1 Array #1.
	 * @param a2 Array #2.
	 * @return <jk>true</jk> if the following sorted arrays are equals.
	 */
	public static boolean equals(String[] a1, String[] a2) {
		if (a1.length != a2.length)
			return false;
		for (int i = 0; i < a1.length; i++)
			if (! eq(a1[i], a2[i]))
				return false;
		return true;
	}

	/**
	 * Reverses the entries in an array.
	 *
	 * @param <E> The element type.
	 * @param array The array to reverse.
	 * @return The same array.
	 */
	public static <E> E[] reverse(E[] array) {
		for (int i = 0; i < array.length / 2; i++) {
			E temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
		return array;
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
		Object a = Array.newInstance(elementType, c.size());
		Iterator<?> it = c.iterator();
		int i = 0;
		while (it.hasNext())
			Array.set(a, i++, it.next());
		return a;
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
		List<E> l = new ArrayList<>(Array.getLength(array));
		for (int i = 0; i < Array.getLength(array); i++)
			l.add((E)Array.get(array, i));
		return l;
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
			int length = Array.getLength(array);
			for (int i = 0; i < length; i++)
				list.add(Array.get(array, i));
		}
		return list;
	}

	/**
	 * Recursively converts the specified array into a list of objects.
	 *
	 * @param array The array to convert.
	 * @return A new {@link ArrayList}
	 */
	public static List<Object> toObjectList(Object array) {
		List<Object> l = new ArrayList<>(Array.getLength(array));
		for (int i = 0; i < Array.getLength(array); i++) {
			Object o = Array.get(array, i);
			if (isArray(o))
				o = toObjectList(o);
			l.add(o);
		}
		return l;
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
		String[] r = new String[c.size()];
		int i = 0;
		for (Object o : c)
			r[i++] = s(o);
		return r;
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

	private CollectionUtils() {}
}
