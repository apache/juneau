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
import org.apache.juneau.collections.*;
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
	 * Returns an iterable over the specified enumeration.
	 *
	 * @param e The collection to iterate over.
	 * @return An iterable over the enumeration.
	 */
	public static <E> Iterable<E> iterable(final Enumeration<E> e) {
		if (e == null)
			return null;
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				return new Iterator<E>() {
					@Override
					public boolean hasNext() {
						return e.hasMoreElements();
					}
					@Override
					public E next() {
						return e.nextElement();
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
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
					if (isJsonArray(o, false)) {
						for (Object o2 : new OList(o.toString()))
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
					if (isJsonObject(o, false)) {
						for (Map.Entry<String,Object> e : OMap.ofJson(o.toString()).entrySet())
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else if (o instanceof Map) {
						for (Map.Entry<Object,Object> e : ((Map<Object,Object>)o).entrySet())
							m.put(toType(e.getKey(), keyType), toType(e.getValue(), valueType, valueTypeArgs));
					} else {
						throw new BasicRuntimeException("Invalid object type {0} passed to addToMap()", o.getClass().getName());
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
	public static <T> AList<T> newList(Collection<T> val) {
		return AList.nullable(val);
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
	 * Creates a case-insensitive ordered set out of the specified string values.
	 *
	 * @param values The values to populate the set with.
	 * @return A new ordered set.
	 */
	public static Set<String> newSortedCaseInsensitiveSet(String...values) {
		Set<String> s = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER) {
			private static final long serialVersionUID = 1L;
			@Override
			public boolean contains(Object v) {
				return v == null ? false : super.contains(v);
			}
		};
		for (String v : values)
			if (v != null)
				s.add(v);
		return s;
	}

	/**
	 * Creates a case-insensitive ordered set out of the specified string values.
	 *
	 * @param values
	 * 	A comma-delimited list of the values to populate the set with.
	 * @return A new ordered set.
	 */
	public static Set<String> newSortedCaseInsensitiveSet(String values) {
		return newSortedCaseInsensitiveSet(StringUtils.split(StringUtils.emptyIfNull(values)));
	}

	/**
	 * Same as {@link #newSortedCaseInsensitiveSet(String)} but makes the set unmodifiable.
	 *
	 * @param values
	 * 	A comma-delimited list of the values to populate the set with.
	 * @return A new ordered set.
	 */
	public static Set<String> newUnmodifiableSortedCaseInsensitiveSet(String values) {
		return Collections.unmodifiableSet(newSortedCaseInsensitiveSet(StringUtils.split(StringUtils.emptyIfNull(values))));
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

	/**
	 * Simple passthrough to {@link Collections#emptySet()}
	 *
	 * @return A new unmodifiable empty set.
	 */
	public static <T> Set<T> emptySet() {
		return Collections.emptySet();
	}

	/**
	 * Simple passthrough to {@link Collections#emptyList()}
	 *
	 * @return A new unmodifiable empty list.
	 */
	public static <T> List<T> emptyList() {
		return Collections.emptyList();
	}

	/**
	 * Simple passthrough to {@link Collections#emptyMap()}
	 *
	 * @return A new unmodifiable empty set.
	 */
	public static <K,V> Map<K,V> emptyMap() {
		return Collections.emptyMap();
	}

	/**
	 * Returns the last entry in a list.
	 *
	 * @param <T> The element type.
	 * @param l The list.
	 * @return The last element, or <jk>null</jk> if the list is <jk>null</jk> or empty.
	 */
	public static <T> T last(List<T> l) {
		if (l == null || l.isEmpty())
			return null;
		return l.get(l.size()-1);
	}
}
