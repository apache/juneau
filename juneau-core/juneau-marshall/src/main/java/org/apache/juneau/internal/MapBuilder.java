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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;

/**
 * Builder for maps.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public final class MapBuilder<K,V> {

	private Map<K,V> map;
	private boolean unmodifiable = false, sparse = false;
	private Comparator<K> comparator = null;

	private Class<K> keyType;
	private Class<V> valueType;
	private Type[] valueTypeArgs;

	/**
	 * Constructor.
	 *
	 * @param keyType The key type.
	 * @param valueType The value type.
	 * @param valueTypeArgs The value type generic arguments if there are any.
	 */
	public MapBuilder(Class<K> keyType, Class<V> valueType, Type...valueTypeArgs) {
		this.keyType = keyType;
		this.valueType = valueType;
		this.valueTypeArgs = valueTypeArgs;
	}

	/**
	 * Constructor.
	 *
	 * @param addTo The map to add to.
	 */
	public MapBuilder(Map<K,V> addTo) {
		this.map = addTo;
	}

	/**
	 * Builds the map.
	 *
	 * @return A map conforming to the settings on this builder.
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
	 * Adds entries to this list via JSON object strings.
	 *
	 * @param values The JSON object strings to parse and add to this list.
	 * @return This object.
	 */
	public MapBuilder<K,V> addJson(String...values) {
		return addAny((Object[])values);
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
			throw new RuntimeException("Unknown key and value types.  Cannot use this method.");
		try {
			for (Object o : values) {
				if (o != null) {
					if (o instanceof Map) {
						((Map<Object,Object>)o).forEach((k,v) -> add(toType(k, keyType), toType(v, valueType, valueTypeArgs)));
					} else if (isJsonObject(o, false)) {
						JsonMap.ofJson(o.toString()).forEach((k,v) -> add(toType(k, keyType), toType(v, valueType, valueTypeArgs)));
					} else {
						throw new BasicRuntimeException("Invalid object type {0} passed to addAny()", className(o));
					}
				}
			}
		} catch (ParseException e) {
			throw asRuntimeException(e);
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
			throw new RuntimeException("Odd number of parameters passed into AMap.ofPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			add((K)pairs[i], (V)pairs[i+1]);
		return this;
	}
}
