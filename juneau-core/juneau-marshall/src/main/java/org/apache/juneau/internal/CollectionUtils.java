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

import java.util.*;

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
}
