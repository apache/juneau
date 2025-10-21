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
package org.apache.juneau.common.collections;

import java.util.*;

import org.apache.juneau.common.utils.*;


/**
 * Builder for maps with fluent convenience methods.
 *
 * <p>
 * Supports adding entries and other maps, arbitrary inputs (maps/convertible values), optional sorting by key,
 * sparse output (return null when empty), and unmodifiable output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class MapBuilder<K,V> {

	private Map<K,V> map;
	private boolean unmodifiable = false, sparse = false;
	private Comparator<K> comparator;

	private Class<K> keyType;
	private Class<V> valueType;
	private List<Converter> converters;

    /**
     * Static creator.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @param keyType The key type.
     * @param valueType The value type.
     * @return A new builder.
     */
	public static <K,V> MapBuilder<K,V> create(Class<K> keyType, Class<V> valueType) {
		return new MapBuilder<>(keyType, valueType);
	}

	public MapBuilder<K,V> to(Map<K,V> map) {
		this.map = map;
		return this;
	}

    /**
     * Constructor.
     *
     * @param keyType The key type.
     * @param valueType The value type.
     */
	public MapBuilder(Class<K> keyType, Class<V> valueType) {
		this.keyType = keyType;
		this.valueType = valueType;
	}

	/**
	 * Adds a single entry to this map.
	 *
	 * @param key The map key.
	 * @param value The map value.
	 * @return This object.
	 */
	public MapBuilder<K,V> add(K key, V value) {
		if (map == null)
			map = new LinkedHashMap<>();
		map.put(key, value);
		return this;
	}

	/**
	 * Appends the contents of the specified map into this map.
	 *
	 * <p>
	 * This is a no-op if the value is <jk>null</jk>.
	 *
	 * @param value The map to add to this map.
	 * @return This object.
	 */
	public MapBuilder<K,V> addAll(Map<K,V> value) {
		if (value != null) {
			if (map == null)
				map = new LinkedHashMap<>(value);
			else
				map.putAll(value);
		}
		return this;
	}

	/**
	 * Adds arbitrary values to this list.
	 *
	 * <p>
	 * Objects can be any of the following:
	 * <ul>
	 * 	<li>Maps of key/value types convertible to the key/value types of this map.
	 * 	<li>JSON object strings parsed and convertible to the key/value types of this map.
	 * </ul>
	 *
	 * @param values The values to add.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public MapBuilder<K,V> addAny(Object...values) {
		if (keyType == null || valueType == null)
			throw new IllegalStateException("Unknown key and value types. Cannot use this method.");
		for (Object o : values) {
			if (o != null) {
				if (o instanceof Map) {
					((Map<Object,Object>)o).forEach((k, v) -> add(toType(keyType, k), toType(valueType, v)));
				} else {
					var m = converters.stream().map(x -> x.convertTo(Map.class, o)).filter(x -> x != null).findFirst().orElse(null);
					if (m != null)
						addAny(m);
					else
						throw ThrowableUtils.runtimeException("Object of type {0} could not be converted to type {1}", o.getClass().getName(), "Map");
				}
			}
		}
		return this;
	}

	/**
	 * Adds a list of key/value pairs to this map.
	 *
	 * @param pairs The pairs to add.
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public MapBuilder<K,V> addPairs(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new IllegalArgumentException("Odd number of parameters passed into AMap.ofPairs()");
		for (int i = 0; i < pairs.length; i += 2)
			add((K)pairs[i], (V)pairs[i + 1]);
		return this;
	}

	private <T> T toType(Class<T> c, Object o) {
		if (c.isInstance(o))
			return c.cast(o);
		if (converters != null) {
			var e = converters.stream().map(x -> x.convertTo(c, o)).filter(x -> x != null).findFirst().orElse(null);
			if (e != null)
				return e;
		}
		throw ThrowableUtils.runtimeException("Object of type {0} could not be converted to type {1}", o.getClass().getName(), c);
	}

	/**
     * Registers value converters that can adapt incoming values in {@link #addAny(Object...)}.
     *
     * @param values Converters to register. Ignored if {@code null}.
     * @return This object.
     */
    public MapBuilder<K,V> converters(Converter...values) {
    	if (values.length == 0)
    		return this;
		if (converters == null)
			converters = new ArrayList<>();
		converters.addAll(Arrays.asList(values));
		return this;
	}

 	/**
	 * Builds the map.
	 *
	 * @return A map conforming to the settings on this builder.
	 */
    /**
     * Builds the map.
     *
     * <p>
     * Applies sorting/unmodifiable/sparse options.
     *
     * @return The built map or {@code null} if {@link #sparse()} is set and the map is empty.
     */
    public Map<K,V> build() {
		if (sparse) {
			if (map != null && map.isEmpty())
				map = null;
		} else {
			if (map == null)
				map = new LinkedHashMap<>();
		}
		if (map != null) {
			if (comparator != null) {
				Map<K,V> m2 = new TreeMap<>(comparator);
				m2.putAll(map);
				map = m2;
			}
			if (unmodifiable)
				map = Collections.unmodifiableMap(map);
		}
		return map;
	}

	/**
	 * Forces the existing set to be copied instead of appended to.
	 *
	 * @return This object.
	 */
	public MapBuilder<K,V> copy() {
		if (map != null)
			map = new LinkedHashMap<>(map);
		return this;
	}

	/**
	 * Converts the set into a {@link SortedMap}.
	 *
	 * @return This object.
	 */
	@SuppressWarnings("unchecked")
	public MapBuilder<K,V> sorted() {
		return sorted((Comparator<K>)Comparator.naturalOrder());
	}

	/**
	 * Converts the set into a {@link SortedMap} using the specified comparator.
	 *
	 * @param comparator The comparator to use for sorting.
	 * @return This object.
	 */
	public MapBuilder<K,V> sorted(Comparator<K> comparator) {
		this.comparator = comparator;
		return this;
	}

	/**
	 * When specified, the {@link #build()} method will return <jk>null</jk> if the map is empty.
	 *
	 * <p>
	 * Otherwise {@link #build()} will never return <jk>null</jk>.
	 *
	 * @return This object.
	 */
	public MapBuilder<K,V> sparse() {
		this.sparse = true;
		return this;
	}

	/**
	 * When specified, {@link #build()} will return an unmodifiable map.
	 *
	 * @return This object.
	 */
	public MapBuilder<K,V> unmodifiable() {
		this.unmodifiable = true;
		return this;
	}
}