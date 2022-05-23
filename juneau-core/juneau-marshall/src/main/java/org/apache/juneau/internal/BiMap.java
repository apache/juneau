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

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.util.*;

import org.apache.juneau.utils.*;

/**
 * A linked hashmap with reverse key lookup by value.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class BiMap<K,V> implements Map<K,V> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Create a new builder for this class.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new builder.
	 */
	public static <K,V> Builder<K,V> create() {
		return new Builder<>();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K,V> {
		final HashMap<K,V> map = new LinkedHashMap<>();
		boolean unmodifiable;

		/**
		 * Adds a value to this map.
		 *
		 * @param key The key.
		 * @param value The value.
		 * @return This object.
		 */
		public Builder<K,V> add(K key, V value) {
			map.put(key, value);
			return this;
		}

		/**
		 * Makes this map unmodifiable.
		 *
		 * @return This object.
		 */
		public Builder<K,V> unmodifiable() {
			unmodifiable = true;
			return this;
		}

		/**
		 * Build the differences.
		 *
		 * @return A new {@link BeanDiff} object.
		 */
		public BiMap<K,V> build() {
			return new BiMap<>(this);
		}

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Map<K,V> forward;
	private final Map<V,K> reverse;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public BiMap(Builder<K,V> builder) {
		Map<K,V> forward = builder.map.entrySet().stream().filter(x -> x.getKey() != null && x.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<V,K> reverse = builder.map.entrySet().stream().filter(x -> x.getKey() != null && x.getValue() != null).collect(toMap(Map.Entry::getValue, Map.Entry::getKey));
		this.forward = builder.unmodifiable ? unmodifiableMap(forward) : forward;
		this.reverse = builder.unmodifiable ? unmodifiableMap(reverse) : reverse;
	}

	/**
	 * Gets the key that is currently mapped to the specified value.
	 *
	 * @param value The value to return.
	 * @return The key matching the value.
	 */
	public K getKey(V value) {
		return reverse.get(value);
	}


	@Override /* Map */
	public int size() {
		return forward.size();
	}

	@Override /* Map */
	public boolean isEmpty() {
		return forward.isEmpty();
	}

	@Override /* Map */
	public boolean containsKey(Object key) {
		return forward.containsKey(key);
	}

	@Override /* Map */
	public boolean containsValue(Object value) {
		return reverse.containsKey(value);
	}

	@Override /* Map */
	public V get(Object key) {
		return forward.get(key);
	}

	@Override /* Map */
	public V put(K key, V value) {
		reverse.put(value, key);
		return forward.put(key, value);
	}

	@Override /* Map */
	public V remove(Object key) {
		V value = forward.remove(key);
		reverse.remove(value);
		return value;
	}

	@Override /* Map */
	public void putAll(Map<? extends K,? extends V> m) {
		forward.putAll(m);
		m.entrySet().forEach(x -> reverse.put(x.getValue(), x.getKey()));
	}

	@Override /* Map */
	public void clear() {
		forward.clear();
		reverse.clear();
	}

	@Override /* Map */
	public Set<K> keySet() {
		return forward.keySet();
	}

	@Override /* Map */
	public Collection<V> values() {
		return forward.values();
	}

	@Override /* Map */
	public Set<Entry<K,V>> entrySet() {
		return forward.entrySet();
	}
}
