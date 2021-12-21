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
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class CollectionUtils {

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
	 * Simple passthrough to {@link Collections#emptyList()}
	 *
	 * @return A new unmodifiable empty list.
	 */
	public static <T> List<T> emptyList() {
		return Collections.emptyList();
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
