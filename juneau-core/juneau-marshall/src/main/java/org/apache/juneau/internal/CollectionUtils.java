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
	 * Reverses the order of a {@link LinkedHashMap}.
	 *
	 * @param in The map to reverse the order on.
	 * @return A new {@link LinkedHashMap} with keys in reverse order.
	 */
	public static <K,V> LinkedHashMap<K,V> reverse(Map<K,V> in) {
		if (in == null)
			return null;
		LinkedHashMap<K,V> m = new LinkedHashMap<>();

		// Note:  Entry objects are reusable in an entry set, so we simply can't
		// create a reversed iteration of that set.
		List<K> keys = new ArrayList<>(in.keySet());
		List<V> values = new ArrayList<>(in.values());
		for (int i = in.size()-1; i >= 0; i--)
			m.put(keys.get(i), values.get(i));

		return m;
	}

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
	 * Returns a reverse iterable of the specified collection.
	 * 
	 * @param c The collection to iterate over.
	 * @return An iterable over the collection in reverse order.
	 */
	public static <T> Iterable<T> reverseIterable(final Collection<T> c) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				if (c == null)
					return Collections.EMPTY_LIST.iterator();
				ArrayList<T> l = new ArrayList<>(c);
				Collections.reverse(l);
				return l.iterator();
			}
		};
	}

	/**
	 * Same as {@link Collections#reverse(List)}, but returns the list.
	 * 
	 * @param l The list being reversed
	 * @return The same list.
	 */
	public static <T> List<T> reverse(List<T> l) {
		Collections.reverse(l);
		return l;
	}
	
	/**
	 * Creates a new copy of a list in reverse order.
	 * 
	 * @param l The old list.
	 * @return 
	 * 	A new list with reversed entries.
	 * 	<br>Returns <jk>null</jk> if the list was <jk>null</jk>.
	 * 	<br>Returns the same list if the list is empty.
	 */
	public static <T> List<T> reverseCopy(List<T> l) {
		if (l == null || l.isEmpty())
			return l;
		List<T> l2 = new ArrayList<>(l);
		Collections.reverse(l2);
		return l2;
	}
}
