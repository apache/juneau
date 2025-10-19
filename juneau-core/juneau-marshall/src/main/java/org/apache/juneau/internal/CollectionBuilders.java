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
package org.apache.juneau.internal;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * Utility methods for collections.
 *
 * <h5 class='section'>See Also:</h5><ul>

 * </ul>
 */
public class CollectionBuilders {

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
	 * Wraps the specified set in {@link Collections#unmodifiableSet(Set)}.
	 *
	 * @param <E> The element type.
	 * @param value The set to wrap.
	 * @return The wrapped set.
	 */
	public static <E> Set<E> synced(Set<E> value) {
		return CollectionUtils2.synced(value);
	}

	private CollectionBuilders() {}
}