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
package org.apache.juneau.internal;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

/**
 * Utility methods for collections.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class CollectionUtils {

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param <E> The element type.
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <E> Set<E> setFrom(Collection<E> val) {
		return val == null ? null : new LinkedHashSet<>(val);
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
	 * Instantiates a new builder on top of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param addTo The map to add to.
	 * @return A new builder on top of the specified map.
	 */
	public static <K,V> MapBuilder<K,V> mapBuilder(Map<K,V> addTo) {
		return new MapBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified map type.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @param valueTypeArgs The value type args.
	 * @return A new builder on top of the specified map.
	 */
	public static <K,V> MapBuilder<K,V> mapBuilder(Class<K> keyType, Class<V> valueType, Type...valueTypeArgs) {
		return new MapBuilder<>(keyType, valueType, valueTypeArgs);
	}

	/**
	 * Instantiates a new builder on top of the specified list.
	 *
	 * @param <E> The element type.
	 * @param addTo The list to add to.
	 * @return A new builder on top of the specified list.
	 */
	public static <E> ListBuilder<E> listBuilder(List<E> addTo) {
		return new ListBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified list type.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type args.
	 * @return A new builder on top of the specified list.
	 */
	public static <E> ListBuilder<E> listBuilder(Class<E> elementType, Type...elementTypeArgs) {
		return new ListBuilder<>(elementType, elementTypeArgs);
	}

	/**
	 * Instantiates a new builder on top of the specified set.
	 *
	 * @param <E> The element type.
	 * @param addTo The set to add to.
	 * @return A new builder on top of the specified set.
	 */
	public static <E> SetBuilder<E> setBuilder(Set<E> addTo) {
		return new SetBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified set.
	 *
	 * @param <E> The element type.
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type args.
	 * @return A new builder on top of the specified set.
	 */
	public static <E> SetBuilder<E> setBuilder(Class<E> elementType, Type...elementTypeArgs) {
		return new SetBuilder<>(elementType, elementTypeArgs);
	}

	/**
	 * Simple passthrough to {@link Collections#emptyList()}
	 *
	 * @param <E> The element type.
	 * @return A new unmodifiable empty list.
	 */
	public static <E> List<E> emptyList() {
		return Collections.emptyList();
	}

	/**
	 * Convenience method for creating an {@link ArrayList}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SafeVarargs
	public static <E> ArrayList<E> list(E...values) {
		ArrayList<E> l = new ArrayList<>(values.length);
		for (E v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating an {@link ArrayList} of the specified size.
	 *
	 * @param <E> The element type.
	 * @param size The initial size of the list.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> list(int size) {
		return new ArrayList<>(size);
	}

	/**
	 * Convenience method for creating a {@link LinkedList}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SafeVarargs
	public static <E> LinkedList<E> linkedList(E...values) {
		LinkedList<E> l = new LinkedList<>();
		for (E v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating an array-backed list by calling {@link Arrays#asList(Object...)}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	@SafeVarargs
	public static <E> List<E> alist(E...values) {
		if (values == null)
			return null;
		return Arrays.asList(values);
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <E> The element type.
	 * @param value The collection to copy from.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> listFrom(Collection<E> value) {
		return listFrom(value, false);
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The collection to copy from.
	 * @return A new modifiable list.
	 */
	public static <K,V> LinkedHashMap<K,V> mapFrom(Map<K,V> value) {
		if (value == null)
			return null;
		return new LinkedHashMap<>(value);
	}

	/**
	 * Creates an {@link ArrayList} copy from a collection.
	 *
	 * @param <E> The element type.
	 * @param value The collection to copy from.
	 * @param nullIfEmpty If <jk>true</jk> will return <jk>null</jk> if the collection is empty.
	 * @return A new modifiable list.
	 */
	public static <E> ArrayList<E> listFrom(Collection<E> value, boolean nullIfEmpty) {
		if (value == null || (nullIfEmpty && value.isEmpty()))
			return null;
		ArrayList<E> l = new ArrayList<>();
		value.forEach(x -> l.add(x));
		return l;
	}

	/**
	 * Convenience method for creating a {@link LinkedHashSet}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the set with.
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> LinkedHashSet<E> set(E...values) {
		LinkedHashSet<E> l = new LinkedHashSet<>();
		for (E v : values)
			l.add(v);
		return l;
	}

	/**
	 * Convenience method for creating an unmodifiable {@link LinkedHashSet}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the set with.
	 * @return A new unmodifiable set.
	 */
	@SafeVarargs
	public static <E> Set<E> uset(E...values) {
		return unmodifiable(set(values));
	}

	/**
	 * Convenience method for creating an unmodifiable list.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new unmodifiable list, or <jk>null</jk> if the array was <jk>null</jk>.
	 */
	@SafeVarargs
	public static <E> List<E> ulist(E...values) {
		if (values == null)
			return null;
		return unmodifiable(alist(values));
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
	public static <E> TreeSet<E> sortedSetFrom(Collection<E> value) {
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
	public static <E> TreeSet<E> sortedSetFrom(Collection<E> value, boolean nullIfEmpty) {
		if (value == null || (nullIfEmpty && value.isEmpty()))
			return null;
		TreeSet<E> l = new TreeSet<>();
		value.forEach(x -> l.add(x));
		return l;
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
	 * Convenience method for creating an {@link ArrayList} and sorting it.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SafeVarargs
	public static <E> ArrayList<E> sortedList(E...values) {
		ArrayList<E> l = list(values);
		Collections.sort((List<Comparable>) l);
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
	public static <E> ArrayList<E> sortedList(Comparator<E> comparator, E[] values) {
		ArrayList<E> l = list(values);
		Collections.sort(l, comparator);
		return l;
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
		ArrayList<E> l = listFrom(value);
		Collections.sort(l, comparator);
		return l;
	}

	/**
	 * Wraps the specified list in {@link Collections#unmodifiableList(List)}.
	 *
	 * @param <E> The element type.
	 * @param value The list to wrap.
	 * @return The wrapped list.
	 */
	public static <E> List<E> unmodifiable(List<E> value) {
		return value == null ? null: Collections.unmodifiableList(value);
	}

	/**
	 * Wraps the specified set in {@link Collections#unmodifiableSet(Set)}.
	 *
	 * @param <E> The element type.
	 * @param value The set to wrap.
	 * @return The wrapped set.
	 */
	public static <E> Set<E> unmodifiable(Set<E> value) {
		return value == null ? null: Collections.unmodifiableSet(value);
	}

	/**
	 * Wraps the specified map in {@link Collections#unmodifiableMap(Map)}.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The map to wrap.
	 * @return The wrapped map.
	 */
	public static <K,V> Map<K,V> unmodifiable(Map<K,V> value) {
		return value == null ? null: Collections.unmodifiableMap(value);
	}

	/**
	 * Wraps the specified list in {@link Collections#unmodifiableList(List)}.
	 *
	 * @param <E> The element type.
	 * @param value The list to wrap.
	 * @return The wrapped list.
	 */
	public static <E> List<E> synced(List<E> value) {
		return value == null ? null: Collections.synchronizedList(value);
	}

	/**
	 * Wraps the specified set in {@link Collections#unmodifiableSet(Set)}.
	 *
	 * @param <E> The element type.
	 * @param value The set to wrap.
	 * @return The wrapped set.
	 */
	public static <E> Set<E> synced(Set<E> value) {
		return value == null ? null: Collections.synchronizedSet(value);
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
		return value == null ? null: Collections.synchronizedMap(value);
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
	 * Iterates the specified list in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The list to iterate.
	 * @param action The action to perform.
	 */
	public static <E> void forEachReverse(List<E> value, Consumer<E> action) {
		if (value instanceof ArrayList) {
			for (int i = value.size()-1; i >= 0; i--)
				action.accept(value.get(i));
		} else {
			ListIterator<E> i = value.listIterator(value.size());
			while (i.hasPrevious())
				action.accept(i.previous());
		}
	}

	/**
	 * Iterates the specified array in reverse order.
	 *
	 * @param <E> The element type.
	 * @param value The array to iterate.
	 * @param action The action to perform.
	 */
	public static <E> void forEachReverse(E[] value, Consumer<E> action) {
		for (int i = value.length-1; i >= 0; i--)
			action.accept(value[i]);
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
		if (entries != null) {
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
		if (entries != null) {
			if (value == null)
				value = sortedSet(entries);
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
	public static <E> List<E> addAll(List<E> value, E...entries) {
		if (entries != null) {
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
	@SafeVarargs
	public static <E> List<E> prependAll(List<E> value, E...entries) {
		if (entries != null) {
			if (value == null)
				value = list(entries);
			else
				value.addAll(0, alist(entries));
		}
		return value;
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
		return l.get(l.size()-1);
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
		return l[l.length-1];
	}

	/**
	 * Returns an optional of the specified value.
	 *
	 * @param <T> The component type.
	 * @param value The value.
	 * @return A new Optional.
	 */
	public static <T> Optional<T> optional(T value) {
		return Optional.ofNullable(value);
	}

	/**
	 * Returns an empty {@link Optional}.
	 *
	 * @param <T> The component type.
	 * @return An empty {@link Optional}.
	 */
	public static <T> Optional<T> empty() {
		return Optional.empty();
	}

	/**
	 * Returns <jk>true</jk> if the specified collection is not <jk>null</jk> and not empty.
	 *
	 * @param <E> The element type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified collection is not <jk>null</jk> and not empty.
	 */
	public static <E> boolean isNotEmpty(Collection<E> value) {
		return value != null && ! value.isEmpty();
	}

	/**
	 * Returns <jk>true</jk> if the specified map is not <jk>null</jk> and not empty.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The value being checked.
	 * @return <jk>true</jk> if the specified map is not <jk>null</jk> and not empty.
	 */
	public static <K,V> boolean isNotEmpty(Map<K,V> value) {
		return value != null && ! value.isEmpty();
	}
}
