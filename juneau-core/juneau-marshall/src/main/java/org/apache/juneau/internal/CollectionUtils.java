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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Utility methods for collections.
 */
public final class CollectionUtils {

	/**
	 * Add a value to a list if the value is not null.
	 *
	 * @param l The list to add to.
	 * @param o The element to add.
	 * @return The same list.
	 */
	public static <T> List<T> addIfNotNull(List<T> l, T o) {
		if (o != null)
			l.add(o);
		return l;
	}

	/**
	 * Adds the contents of one list to the other in reverse order.
	 *
	 * <p>
	 * i.e. add values from 2nd list from end-to-start order to the end of the 1st list.
	 *
	 * @param list The list to append to.
	 * @param append Contains the values to append to the list.
	 * @return The same list.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<?> addReverse(List list, List append) {
		for (ListIterator i = append.listIterator(append.size()); i.hasPrevious();)
			list.add(i.previous());
		return list;
	}

	/**
	 * Adds the contents of the array to the list in reverse order.
	 *
	 * <p>
	 * i.e. add values from the array from end-to-start order to the end of the list.
	 *
	 * @param list The list to append to.
	 * @param append Contains the values to append to the list.
	 * @return The same list.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<?> addReverse(List list, Object[] append) {
		for (int i = append.length - 1; i >= 0; i--)
			list.add(append[i]);
		return list;
	}

	/**
	 * Returns an iterable over the specified collection.
	 *
	 * @param c The collection to iterate over.
	 * @param reverse If <jk>true</jk>, iterate in reverse order.
	 * @return An iterable over the collection.
	 */
	public static <T> Iterable<T> iterable(final Collection<T> c, boolean reverse) {
		if (reverse)
			return new ReverseIterable<>(c instanceof List ? (List<T>)c : new ArrayList<>(c));
		return c;
	}

	/**
	 * Returns an iterable over the specified list.
	 *
	 * @param c The collection to iterate over.
	 * @param reverse If <jk>true</jk>, iterate in reverse order.
	 * @return An iterable over the collection.
	 */
	public static <T> Iterable<T> iterable(final List<T> c, boolean reverse) {
		if (reverse)
			return new ReverseIterable<>(c);
		return c;
	}

	/**
	 * Returns an iterable over the specified array.
	 *
	 * @param c The collection to iterate over.
	 * @param reverse If <jk>true</jk>, iterate in reverse order.
	 * @return An iterable over the collection.
	 */
	public static <T> Iterable<T> iterable(T[] c, boolean reverse) {
		if (reverse)
			return new ReverseIterable<>(Arrays.asList(c));
		return Arrays.asList(c);
	}

	/**
	 * Creates an unmodifiable list from the specified list.
	 *
	 * @param l The collection to copy from.
	 * @return An unmodifiable view of the list, or a {@link Collections#emptyList()}
	 * 	if the list was empty or <jk>null</jk>.
	 */
	public static <T> List<T> unmodifiableList(List<T> l) {
		if (l == null || l.isEmpty())
			return Collections.emptyList();
		return Collections.unmodifiableList(l);
	}

	/**
	 * Creates an unmodifiable list from the specified array.
	 *
	 * @param l The collection to copy from.
	 * @return An unmodifiable view of the list, or a {@link Collections#emptyList()}
	 * 	if the list was empty or <jk>null</jk>.
	 */
	public static <T> List<T> unmodifiableList(T[] l) {
		if (l == null || l.length == 0)
			return Collections.emptyList();
		return Collections.unmodifiableList(Arrays.asList(l));
	}

	/**
	 * Creates an immutable list from the specified collection.
	 *
	 * @param l The collection to copy from.
	 * @return An unmodifiable {@link ArrayList} copy of the collection, or a {@link Collections#emptyList()}
	 * 	if the collection was empty or <jk>null</jk>.
	 */
	public static <T> List<T> immutableList(Collection<T> l) {
		if (l == null || l.isEmpty())
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(l));
	}

	/**
	 * Creates an immutable list from the specified array.
	 *
	 * @param l The array to copy from.
	 * @return An unmodifiable {@link ArrayList} copy of the collection, or a {@link Collections#emptyList()}
	 * 	if the collection was empty or <jk>null</jk>.
	 */
	public static <T> List<T> immutableList(T[] l) {
		if (l == null || l.length == 0)
			return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(l)));
	}

	/**
	 * Creates an immutable map from the specified map.
	 *
	 * @param m The map to copy from.
	 * @return An unmodifiable {@link LinkedHashMap} copy of the collection, or a {@link Collections#emptyMap()}
	 * 	if the collection was empty or <jk>null</jk>.
	 */
	public static <K,V> Map<K,V> immutableMap(Map<K,V> m) {
		if (m == null || m.isEmpty())
			return Collections.emptyMap();
		return Collections.unmodifiableMap(new LinkedHashMap<>(m));
	}

	/**
	 * Creates an unmodifiable map from the specified map.
	 *
	 * @param m The map to copy from.
	 * @return An unmodifiable view of the collection, or a {@link Collections#emptyMap()}
	 * 	if the collection was empty or <jk>null</jk>.
	 */
	public static <K,V> Map<K,V> unmodifiableMap(Map<K,V> m) {
		if (m == null || m.isEmpty())
			return Collections.emptyMap();
		return Collections.unmodifiableMap(m);
	}

	/**
	 * Adds a set of values to an existing list.
	 *
	 * @param appendTo
	 * 	The list to append to.
	 * 	<br>If <jk>null</jk>, a new {@link ArrayList} will be created.
	 * @param values The values to add.
	 * @param type The data type of the elements.
	 * @param args The generic type arguments of the data type.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> addToList(List<T> appendTo, Object[] values, Class<T> type, Type...args) {
		if (values == null)
			return appendTo;
		try {
			List<T> l = appendTo;
			if (appendTo == null)
				l = new ArrayList<>();
			for (Object o : values) {
				if (o != null) {
					if (isObjectList(o, false)) {
						for (Object o2 : new ObjectList(o.toString()))
							l.add(toType(o2, type, args));
					} else if (o instanceof Collection) {
						for (Object o2 : (Collection<?>)o)
							l.add(toType(o2, type, args));
					} else if (o.getClass().isArray()) {
						for (int i = 0; i < Array.getLength(o); i++)
							l.add(toType(Array.get(o, i), type, args));
					} else {
						l.add(toType(o, type, args));
					}
				}
			}
			return l.isEmpty() ? null : l;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds a set of values to an existing map.
	 *
	 * @param appendTo
	 * 	The map to append to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param values The values to add.
	 * @param keyType The data type of the keys.
	 * @param valueType The data type of the values.
	 * @param valueTypeArgs The generic type arguments of the data type of the values.
	 * @return The converted value, or <jk>null</jk> if the input was null.
	 */
	@SuppressWarnings("unchecked")
	public static <K,V> Map<K,V> addToMap(Map<K,V> appendTo, Object[] values, Class<K> keyType, Class<V> valueType, Type...valueTypeArgs) {
		if (values == null)
			return appendTo;
		try {
			Map<K,V> m = appendTo;
			if (m == null)
				m = new LinkedHashMap<>();
			for (Object o : values) {
				if (o != null) {
					if (isObjectMap(o, false)) {
						for (Map.Entry<String,Object> e : new ObjectMap(o.toString()).entrySet())
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else if (o instanceof Map) {
						for (Map.Entry<Object,Object> e : ((Map<Object,Object>)o).entrySet())
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else {
						throw new FormattedRuntimeException("Invalid object type {0} passed to addToMap()", o.getClass().getName());
					}
				}
			}
			return m.isEmpty() ? null : m;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new list from the specified collection.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link ArrayList}, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> newList(Collection<T> val) {
		if (val == null)
			return null;
		return new ArrayList<>(val);
	}

	/**
	 * Copies the specified values into an existing list.
	 *
	 * @param l
	 * 	The list to add to.
	 * 	<br>If <jk>null</jk>, a new {@link ArrayList} will be created.
	 * @param val The values to add.
	 * @return The list with values copied into it.
	 */
	public static <T> List<T> addToList(List<T> l, Collection<T> val) {
		if (val != null) {
			if (l == null)
				l = new ArrayList<>(val);
			else
				l.addAll(val);
		}
		return l;
	}

	/**
	 * Creates a new map from the specified map.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> newMap(Map<K,V> val) {
		if (val == null)
			return null;
		return new LinkedHashMap<>(val);
	}

	/**
	 * Copies the specified values into an existing map.
	 *
	 * @param m
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param val The values to add.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToMap(Map<K,V> m, Map<K,V> val) {
		if (val != null) {
			if (m == null)
				m = new LinkedHashMap<>(val);
			else
				m.putAll(val);
		}
		return m;
	}

	/**
	 * Adds a single entry into an existing map.
	 *
	 * @param m
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param key The entry key.
	 * @param value The entry value.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToMap(Map<K,V> m, K key, V value) {
		if (m == null)
			m = new LinkedHashMap<>();
		m.put(key, value);
		return m;
	}

	/**
	 * Creates a new map from the specified map.
	 *
	 * @param val The value to copy from.
	 * @param comparator The key comparator to use, or <jk>null</jk> to use natural ordering.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> newSortedMap(Map<K,V> val, Comparator<K> comparator) {
		if (val == null)
			return null;
		Map<K,V> m = new TreeMap<>(comparator);
		m.putAll(val);
		return m;
	}

	/**
	 * Copies the specified values into an existing map.
	 *
	 * @param m
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param val The values to add.
	 * @param comparator The key comparator to use, or <jk>null</jk> to use natural ordering.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToSortedMap(Map<K,V> m, Map<K,V> val, Comparator<K> comparator) {
		if (val != null) {
			if (m == null) {
				m = new TreeMap<>(comparator);
				m.putAll(val);
			} else {
				m.putAll(val);
			}
		}
		return m;
	}

	/**
	 * Adds a single entry into an existing map.
	 *
	 * @param m
	 * 	The map to add to.
	 * 	<br>If <jk>null</jk>, a new {@link LinkedHashMap} will be created.
	 * @param key The entry key.
	 * @param value The entry value.
	 * @param comparator The key comparator to use, or <jk>null</jk> to use natural ordering.
	 * @return The list with values copied into it.
	 */
	public static <K,V> Map<K,V> addToSortedMap(Map<K,V> m, K key, V value, Comparator<K> comparator) {
		if (m == null)
			m = new TreeMap<>(comparator);
		m.put(key, value);
		return m;
	}
}
