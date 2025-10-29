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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.common.utils.*;

/**
 * A fluent builder for constructing {@link Map} instances with various configuration options.
 *
 * <p>
 * This builder provides a flexible and type-safe way to construct maps with support for adding entries,
 * other maps, sorting by keys, and applying modifiers like unmodifiable or sparse modes. It's particularly
 * useful when constructing maps dynamically from multiple sources or with conditional entries.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Fluent API - all methods return <c>this</c> for method chaining
 * 	<li>Multiple add methods - single entries, pairs, other maps
 * 	<li>Arbitrary input support - automatic type conversion with {@link #addAny(Object...)}
 * 	<li>Pair adding - {@link #addPairs(Object...)} for varargs key-value pairs
 * 	<li>Sorting support - natural key order or custom {@link Comparator}
 * 	<li>Sparse mode - return <jk>null</jk> for empty maps
 * 	<li>Unmodifiable mode - create immutable maps
 * 	<li>Custom converters - type conversion via {@link Converter}
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Basic usage</jc>
 * 	Map&lt;String,Integer&gt; <jv>map</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.add(<js>"one"</js>, 1)
 * 		.add(<js>"two"</js>, 2)
 * 		.add(<js>"three"</js>, 3)
 * 		.build();
 *
 * 	<jc>// Using pairs</jc>
 * 	Map&lt;String,String&gt; <jv>props</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.addPairs(<js>"host"</js>, <js>"localhost"</js>, <js>"port"</js>, <js>"8080"</js>)
 * 		.build();
 *
 * 	<jc>// With sorting by key</jc>
 * 	Map&lt;String,Integer&gt; <jv>sorted</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.add(<js>"zebra"</js>, 3)
 * 		.add(<js>"apple"</js>, 1)
 * 		.add(<js>"banana"</js>, 2)
 * 		.sorted()
 * 		.build();  <jc>// Returns TreeMap with natural key order</jc>
 *
 * 	<jc>// Immutable map</jc>
 * 	Map&lt;String,String&gt; <jv>config</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.add(<js>"env"</js>, <js>"prod"</js>)
 * 		.add(<js>"region"</js>, <js>"us-west"</js>)
 * 		.unmodifiable()
 * 		.build();
 *
 * 	<jc>// From multiple sources</jc>
 * 	Map&lt;String,Integer&gt; <jv>existing</jv> = Map.of(<js>"a"</js>, 1, <js>"b"</js>, 2);
 * 	Map&lt;String,Integer&gt; <jv>combined</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>)
 * 		.addAll(<jv>existing</jv>)
 * 		.add(<js>"c"</js>, 3)
 * 		.build();
 *
 * 	<jc>// Sparse mode - returns null when empty</jc>
 * 	Map&lt;String,String&gt; <jv>maybeNull</jv> = MapBuilder.<jsm>create</jsm>(String.<jk>class</jk>, String.<jk>class</jk>)
 * 		.sparse()
 * 		.build();  <jc>// Returns null, not empty map</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is <b>not thread-safe</b>. Each builder instance should be used by a single thread or
 * properly synchronized.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * 	<li class='jc'>{@link ListBuilder}
 * 	<li class='jc'>{@link SetBuilder}
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
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

	/**
	 * Specifies the map to append to.
	 *
	 * <p>
	 * If not specified, uses a new {@link LinkedHashMap}.
	 *
	 * @param map The map to append to.
	 * @return This object.
	 */
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
		if (nn(value)) {
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
		for (var o : values) {
			if (nn(o)) {
				if (o instanceof Map) {
					((Map<Object,Object>)o).forEach((k, v) -> add(toType(keyType, k), toType(valueType, v)));
				} else {
					var m = converters.stream().map(x -> x.convertTo(Map.class, o)).filter(x -> nn(x)).findFirst().orElse(null);
					if (nn(m))
						addAny(m);
					else
						throw runtimeException("Object of type {0} could not be converted to type {1}", o.getClass().getName(), "Map");
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

	/**
	 * Converts an object to the specified type.
	 *
	 * @param <T> The type to convert to.
	 * @param c The type to convert to.
	 * @param o The object to convert.
	 * @return The converted object.
	 * @throws RuntimeException If the object cannot be converted to the specified type.
	 */
	private <T> T toType(Class<T> c, Object o) {
		if (c.isInstance(o))
			return c.cast(o);
		if (nn(converters)) {
			var e = converters.stream().map(x -> x.convertTo(c, o)).filter(x -> nn(x)).findFirst().orElse(null);
			if (nn(e))
				return e;
		}
		throw runtimeException("Object of type {0} could not be converted to type {1}", o.getClass().getName(), c);
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
		converters.addAll(l(values));
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
			if (nn(map) && map.isEmpty())
				map = null;
		} else {
			if (map == null)
				map = new LinkedHashMap<>();
		}
		if (nn(map)) {
			if (nn(comparator)) {
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
		if (nn(map))
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