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

import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.internal.Utils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;

/**
 * Utility methods for collections.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class CollectionUtils {

	private CollectionUtils() {}

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
	 * Convenience method for creating an {@link ArrayList}.
	 *
	 * @param <E> The element type.
	 * @param values The values to initialize the list with.
	 * @return A new modifiable list.
	 */
	@SafeVarargs
	public static <E> List<E> list2(E...values) {
		ArrayList<E> l = new ArrayList<>(values.length);
		for (E v : values)
			l.add(v);
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
	 * @param elementType The element type.
	 * @param values The values to initialize the set with.
	 * @return A new modifiable set.
	 */
	@SafeVarargs
	public static <E> LinkedHashSet<E> setOf(Class<E> elementType, E...values) {
		return Utils.set(values);
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
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @return A new modifiable map.
	 */
	public static <K,V> LinkedHashMap<K,V> mapOf(Class<K> keyType, Class<V> valueType) {
		return map();
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
	public static <E> List<E> sortedList(E...values) {
		List<E> l = list(values);
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
	public static <E> List<E> sortedList(Comparator<E> comparator, E[] values) {
		List<E> l = list(values);
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
				value = Utils.set(entries);
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
	public static <E> List<E> addAll(List<E> value, List<E> entries) {
		if (entries != null) {
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
	 * Makes a deep copy of the specified map.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param m The map to copy.
	 * @param valueMapper The function to apply to each value in the map.
	 * @return A new map with the same keys as the specified map, but with values transformed by the specified function.  Null if the map being copied was null.
	 */
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V, ? extends V> valueMapper) {
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
	public static <K,V> Map<K,V> copyOf(Map<K,V> m, Function<? super V, ? extends V> valueMapper, Supplier<Map<K,V>> mapFactory) {
		if (m == null)
			return null;  // NOSONAR - Intentional.
		return m.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> valueMapper.apply(e.getValue()), (a, b) -> b, mapFactory));
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> List<E> copyOf(List<E> l, Function<? super E, ? extends E> valueMapper) {
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
	public static <E> List<E> copyOf(List<E> l, Function<? super E, ? extends E> valueMapper, Supplier<List<E>> listFactory) {
		if (l == null)
			return null;  // NOSONAR - Intentional.
		return l.stream().map(valueMapper).collect(toCollection(listFactory));
	}

	/**
	 * Makes a deep copy of the specified list.
	 *
	 * @param l The list to copy.
	 * @param valueMapper The function to apply to each value in the list.
	 * @param <E> The entry type.
	 * @return A new list with the same values as the specified list, but with values transformed by the specified function.  Null if the list being copied was null.
	 */
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E, ? extends E> valueMapper) {
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
	public static <E> Set<E> copyOf(Set<E> l, Function<? super E, ? extends E> valueMapper, Supplier<Set<E>> setFactory) {
		if (l == null)
			return null;  // NOSONAR - Intentional.
		return l.stream().map(valueMapper).collect(toCollection(setFactory));
	}
}