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

/**
 * Utility methods for collections.
 */
public final class CollectionUtils {

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
	 * Creates an iterator over a list of iterable objects.
	 *
	 * @param <E> The element type.
	 * @param l The iterables to iterate over.
	 * @return A new iterator.
	 */
	public static <E> Iterator<E> iterator(final List<Iterable<E>> l) {
		return new Iterator<E>() {
			Iterator<Iterable<E>> i1 = l.iterator();
			Iterator<E> i2 = i1.hasNext() ? i1.next().iterator() : null;

			@Override /* Iterator */
			public boolean hasNext() {
				while (i2 != null && ! i2.hasNext())
					i2 = (i1.hasNext() ? i1.next().iterator() : null);
				return (i2 != null);
			}

			@Override /* Iterator */
			public E next() {
				hasNext();
				if (i2 == null)
					throw new NoSuchElementException();
				return i2.next();
			}

			@Override /* Iterator */
			public void remove() {
				if (i2 == null)
					throw new NoSuchElementException();
				i2.remove();
			}
		};
	}

	/**
	 * Creates a new list from the specified collection.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link ArrayList}, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> newList(Collection<T> val) {
		return val == null ? null : new ArrayList<>(val);
	}

	/**
	 * Creates a new list from the specified collection.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link ArrayList}, or <jk>null</jk> if the input was null.
	 */
	public static <T> List<T> newList(List<T> val) {
		return val == null ? null : new ArrayList<>(val);
	}

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <T> Set<T> newSet(Collection<T> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Creates a new set from the specified collection.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashSet}, or <jk>null</jk> if the input was null.
	 */
	public static <T> Set<T> newSet(Set<T> val) {
		return val == null ? null : new LinkedHashSet<>(val);
	}

	/**
	 * Creates a new map from the specified map.
	 *
	 * @param val The value to copy from.
	 * @return A new {@link LinkedHashMap}, or <jk>null</jk> if the input was null.
	 */
	public static <K,V> Map<K,V> newMap(Map<K,V> val) {
		return val == null ? null : new LinkedHashMap<>(val);
	}

	/**
	 * Instantiates a new builder on top of the specified map.
	 *
	 * @param addTo The map to add to.
	 * @return A new builder on top of the specified map.
	 */
	public static <K,V> MapBuilder<K,V> mapBuilder(Map<K,V> addTo) {
		return new MapBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified map type.
	 *
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
	 * @param addTo The list to add to.
	 * @return A new builder on top of the specified list.
	 */
	public static <E> ListBuilder<E> listBuilder(List<E> addTo) {
		return new ListBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified list type.
	 *
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
	 * @param addTo The set to add to.
	 * @return A new builder on top of the specified set.
	 */
	public static <E> SetBuilder<E> setBuilder(Set<E> addTo) {
		return new SetBuilder<>(addTo);
	}

	/**
	 * Instantiates a new builder of the specified set.
	 *
	 * @param elementType The element type.
	 * @param elementTypeArgs The element type args.
	 * @return A new builder on top of the specified set.
	 */
	public static <E> SetBuilder<E> setBuilder(Class<E> elementType, Type...elementTypeArgs) {
		return new SetBuilder<>(elementType, elementTypeArgs);
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
