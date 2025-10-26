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

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;

import java.util.*;

/**
 * A bidirectional map with reverse key lookup by value.
 *
 * <p>
 * This implementation provides efficient bidirectional lookups by maintaining two internal maps:
 * a forward map for key-to-value lookups and a reverse map for value-to-key lookups.
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 * 	<li>Implements the standard {@link Map} interface for forward key→value lookups
 * 	<li>Provides {@link #getKey(Object)} method for reverse value→key lookups
 * 	<li>Maintains insertion order using {@link LinkedHashMap} internally
 * 	<li>Automatically filters out null keys and values
 * 	<li>Supports both mutable and unmodifiable instances via the builder
 * 	<li>Thread-safety: Not thread-safe by default; external synchronization required if accessed by multiple threads
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a bidirectional map</jc>
 * 	BidiMap&lt;String,Integer&gt; <jv>map</jv> = BidiMap.<jsm>create</jsm>()
 * 		.add(<js>"one"</js>, 1)
 * 		.add(<js>"two"</js>, 2)
 * 		.add(<js>"three"</js>, 3)
 * 		.build();
 *
 * 	<jc>// Forward lookup: key → value</jc>
 * 	Integer <jv>value</jv> = <jv>map</jv>.get(<js>"two"</js>);  <jc>// Returns 2</jc>
 *
 * 	<jc>// Reverse lookup: value → key</jc>
 * 	String <jv>key</jv> = <jv>map</jv>.getKey(2);  <jc>// Returns "two"</jc>
 * </p>
 *
 * <h5 class='section'>Null Handling:</h5>
 * <p>
 * This map automatically filters out entries with null keys or values during construction.
 * Attempting to add null keys or values via {@link #put(Object, Object)} or {@link #putAll(Map)}
 * after construction will result in them being stored in the forward map but not the reverse map.
 *
 * <h5 class='section'>Unmodifiable Instances:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an unmodifiable bidirectional map</jc>
 * 	BidiMap&lt;String,Integer&gt; <jv>map</jv> = BidiMap.<jsm>create</jsm>()
 * 		.add(<js>"one"</js>, 1)
 * 		.add(<js>"two"</js>, 2)
 * 		.unmodifiable()
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class BidiMap<K,V> implements Map<K,V> {

	/**
	 * Builder class for {@link BidiMap}.
	 *
	 * <p>
	 * Provides a fluent API for constructing bidirectional maps.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BidiMap&lt;String,Integer&gt; <jv>map</jv> = BidiMap.<jsm>create</jsm>()
	 * 		.add(<js>"one"</js>, 1)
	 * 		.add(<js>"two"</js>, 2)
	 * 		.unmodifiable()
	 * 		.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K,V> {
		final HashMap<K,V> map = new LinkedHashMap<>();
		final Set<V> values = new HashSet<>();
		boolean unmodifiable;

		/**
		 * Adds a key-value pair to this map.
		 *
		 * <p>
		 * Null keys and values are allowed in the builder but will be filtered out
		 * during the {@link #build()} operation.
		 *
		 * @param key The key. Can be <jk>null</jk>.
		 * @param value The value. Can be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException if the value already exists mapped to a different key.
		 */
		public Builder<K,V> add(K key, V value) {
			if (key != null && value != null) {
				var existingValue = map.get(key);
				if (existingValue != null && ! existingValue.equals(value)) {
					// Key is being overwritten with a different value, remove old value from tracking
					values.remove(existingValue);
				}
				if (values.contains(value) && ! value.equals(existingValue)) {
					throw illegalArg("Value ''{0}'' is already mapped to a different key in this BidiMap.", value);
				}
				values.add(value);
			}
			map.put(key, value);
			return this;
		}

		/**
		 * Builds a new {@link BidiMap} from the entries added to this builder.
		 *
		 * <p>
		 * Null keys and values are automatically filtered out during construction.
		 *
		 * @return A new {@link BidiMap} instance.
		 */
		public BidiMap<K,V> build() {
			return new BidiMap<>(this);
		}

		/**
		 * Makes the resulting map unmodifiable.
		 *
		 * <p>
		 * When set, the built map will be wrapped with {@link Collections#unmodifiableMap(Map)},
		 * preventing any modifications after construction.
		 *
		 * @return This object.
		 */
		public Builder<K,V> unmodifiable() {
			unmodifiable = true;
			return this;
		}
	}

	/**
	 * Creates a new builder for this class.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new {@link Builder} instance.
	 */
	public static <K,V> Builder<K,V> create() {
		return new Builder<>();
	}

	private final Map<K,V> forward;
	private final Map<V,K> reverse;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Constructs a bidirectional map from the provided builder, automatically filtering
	 * out any entries with null keys or values.
	 *
	 * @param builder The builder containing the initial entries.
	 */
	public BidiMap(Builder<K,V> builder) {
		var forward = builder.map.entrySet().stream().filter(x -> x.getKey() != null && x.getValue() != null).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
		var reverse = builder.map.entrySet().stream().filter(x -> x.getKey() != null && x.getValue() != null).collect(toMap(Map.Entry::getValue, Map.Entry::getKey, (a, b) -> b, LinkedHashMap::new));
		this.forward = builder.unmodifiable ? unmodifiableMap(forward) : forward;
		this.reverse = builder.unmodifiable ? unmodifiableMap(reverse) : reverse;
	}

	/**
	 * Removes all key-value mappings from this map.
	 *
	 * <p>
	 * Clears both the forward and reverse maps.
	 *
	 * @throws UnsupportedOperationException if the map is unmodifiable.
	 */
	@Override /* Map */
	public void clear() {
		forward.clear();
		reverse.clear();
	}

	/**
	 * Returns <jk>true</jk> if this map contains a mapping for the specified key.
	 *
	 * @param key The key to check for.
	 * @return <jk>true</jk> if this map contains a mapping for the specified key.
	 */
	@Override /* Map */
	public boolean containsKey(Object key) {
		return forward.containsKey(key);
	}

	/**
	 * Returns <jk>true</jk> if this map maps one or more keys to the specified value.
	 *
	 * <p>
	 * This implementation uses the reverse map for efficient lookup.
	 *
	 * @param value The value to check for.
	 * @return <jk>true</jk> if this map maps one or more keys to the specified value.
	 */
	@Override /* Map */
	public boolean containsValue(Object value) {
		return reverse.containsKey(value);
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 *
	 * @return A set view of the mappings contained in this map.
	 */
	@Override /* Map */
	public Set<Entry<K,V>> entrySet() {
		return forward.entrySet();
	}

	/**
	 * Returns the value to which the specified key is mapped, or <jk>null</jk> if this map contains no mapping for the key.
	 *
	 * @param key The key whose associated value is to be returned.
	 * @return The value to which the specified key is mapped, or <jk>null</jk> if this map contains no mapping for the key.
	 */
	@Override /* Map */
	public V get(Object key) {
		return forward.get(key);
	}

	/**
	 * Returns the key that is currently mapped to the specified value.
	 *
	 * <p>
	 * This is the reverse lookup operation that makes this a bidirectional map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BidiMap&lt;String,Integer&gt; <jv>map</jv> = BidiMap.<jsm>create</jsm>().add(<js>"two"</js>, 2).build();
	 * 	String <jv>key</jv> = <jv>map</jv>.getKey(2);  <jc>// Returns "two"</jc>
	 * </p>
	 *
	 * @param value The value whose associated key is to be returned.
	 * @return The key to which the specified value is mapped, or <jk>null</jk> if this map contains no mapping for the value.
	 */
	public K getKey(V value) {
		return reverse.get(value);
	}

	/**
	 * Returns <jk>true</jk> if this map contains no key-value mappings.
	 *
	 * @return <jk>true</jk> if this map contains no key-value mappings.
	 */
	@Override /* Map */
	public boolean isEmpty() { return forward.isEmpty(); }

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 *
	 * @return A set view of the keys contained in this map.
	 */
	@Override /* Map */
	public Set<K> keySet() {
		return forward.keySet();
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 *
	 * <p>
	 * This operation updates both the forward map (key→value) and the reverse map (value→key).
	 *
	 * @param key The key with which the specified value is to be associated.
	 * @param value The value to be associated with the specified key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping for the key.
	 * @throws UnsupportedOperationException if the map is unmodifiable.
	 * @throws IllegalArgumentException if the value already exists mapped to a different key.
	 */
	@Override /* Map */
	public V put(K key, V value) {
		var existingKeyForValue = reverse.get(value);
		if (existingKeyForValue != null && ! existingKeyForValue.equals(key)) {
			throw illegalArg("Value ''{0}'' is already mapped to key ''{1}'' in this BidiMap.", value, existingKeyForValue);
		}
		var oldValue = forward.put(key, value);
		if (oldValue != null) {
			reverse.remove(oldValue);
		}
		reverse.put(value, key);
		return oldValue;
	}

	/**
	 * Copies all mappings from the specified map to this map.
	 *
	 * <p>
	 * This operation updates both the forward and reverse maps.
	 *
	 * @param m Mappings to be stored in this map.
	 * @throws UnsupportedOperationException if the map is unmodifiable.
	 * @throws IllegalArgumentException if any value in the map already exists mapped to a different key.
	 */
	@Override /* Map */
	public void putAll(Map<? extends K,? extends V> m) {
		// Check for duplicate values before making any changes
		for (var entry : m.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue();
			var existingKeyForValue = reverse.get(value);
			if (existingKeyForValue != null && ! existingKeyForValue.equals(key)) {
				throw illegalArg("Value ''{0}'' is already mapped to key ''{1}'' in this BidiMap.", value, existingKeyForValue);
			}
		}
		// All checks passed, now perform the updates
		for (var entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Removes the mapping for a key from this map if it is present.
	 *
	 * <p>
	 * This operation removes the entry from both the forward and reverse maps.
	 *
	 * @param key The key whose mapping is to be removed from the map.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping for the key.
	 * @throws UnsupportedOperationException if the map is unmodifiable.
	 */
	@Override /* Map */
	public V remove(Object key) {
		var value = forward.remove(key);
		reverse.remove(value);
		return value;
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return The number of key-value mappings in this map.
	 */
	@Override /* Map */
	public int size() {
		return forward.size();
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 *
	 * @return A collection view of the values contained in this map.
	 */
	@Override /* Map */
	public Collection<V> values() {
		return forward.values();
	}
}