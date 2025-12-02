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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * A lightweight, fixed-size map implementation backed by parallel key and value arrays.
 *
 * <p>
 * This class provides a simple, efficient map implementation for scenarios where the set of keys
 * is known in advance and doesn't need to change. It's particularly useful for small maps (typically
 * less than 10 entries) where the overhead of a {@link HashMap} is not justified.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Fixed Keys:</b> Keys are set at construction time and cannot be added or removed
 * 	<li><b>Mutable Values:</b> Values can be updated via {@link #put(Object, Object)} or {@link java.util.Map.Entry#setValue(Object)}
 * 	<li><b>Array-Backed:</b> Uses simple arrays for storage, avoiding hash computation overhead
 * 	<li><b>Memory Efficient:</b> Minimal memory footprint compared to {@link HashMap}
 * 	<li><b>Predictable Performance:</b> O(n) lookup time, but faster than {@link HashMap} for small n
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Configuration maps with a fixed set of known keys
 * 	<li>Small lookup tables (e.g., enum-to-value mappings)
 * 	<li>Temporary maps for method parameter passing
 * 	<li>Situations where map size is small and keys are known at compile time
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a SimpleMap with fixed keys</jc>
 * 	String[] <jv>keys</jv> = {<js>"host"</js>, <js>"port"</js>, <js>"timeout"</js>};
 * 	Object[] <jv>values</jv> = {<js>"localhost"</js>, 8080, 30000};
 *
 * 	SimpleMap&lt;String,Object&gt; <jv>config</jv> = <jk>new</jk> SimpleMap&lt;&gt;(<jv>keys</jv>, <jv>values</jv>);
 *
 * 	<jc>// Get values</jc>
 * 	String <jv>host</jv> = (String)<jv>config</jv>.get(<js>"host"</js>);     <jc>// Returns: "localhost"</jc>
 * 	Integer <jv>port</jv> = (Integer)<jv>config</jv>.get(<js>"port"</js>);   <jc>// Returns: 8080</jc>
 *
 * 	<jc>// Update values</jc>
 * 	<jv>config</jv>.put(<js>"port"</js>, 9090);  <jc>// Updates value in underlying array</jc>
 *
 * 	<jc>// Cannot add new keys</jc>
 * 	<jv>config</jv>.put(<js>"user"</js>, <js>"admin"</js>);  <jc>// Throws IllegalArgumentException</jc>
 * </p>
 *
 * <h5 class='section'>Restrictions:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Fixed Size:</b> Cannot add or remove entries after construction
 * 	<li><b>Unique Keys:</b> Keys must be unique (no duplicates)
 * 	<li><b>Array Length:</b> Keys and values arrays must have the same length
 * 	<li><b>Existing Keys Only:</b> {@link #put(Object, Object)} only works for existing keys
 * </ul>
 *
 * <h5 class='section'>Performance Characteristics:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>get(key):</b> O(n) - Linear search through keys array
 * 	<li><b>put(key, value):</b> O(n) - Linear search to find key, then update
 * 	<li><b>size():</b> O(1) - Direct array length access
 * 	<li><b>Memory:</b> Lower overhead than {@link HashMap} for small maps
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not thread-safe. If multiple threads access a SimpleMap concurrently, and at least
 * one thread modifies the map structurally (via {@link #put(Object, Object)}), it must be
 * synchronized externally.
 *
 * <h5 class='section'>Example - Enum Configuration:</h5>
 * <p class='bjava'>
 * 	<jc>// Map configuration keys to default values</jc>
 * 	<jk>enum</jk> ConfigKey { HOST, PORT, TIMEOUT }
 *
 * 	ConfigKey[] <jv>keys</jv> = ConfigKey.values();
 * 	Object[] <jv>defaults</jv> = {<js>"localhost"</js>, 8080, 5000};
 *
 * 	SimpleMap&lt;ConfigKey,Object&gt; <jv>configMap</jv> = <jk>new</jk> SimpleMap&lt;&gt;(<jv>keys</jv>, <jv>defaults</jv>);
 *
 * 	<jc>// Update from user settings</jc>
 * 	<jv>configMap</jv>.put(ConfigKey.PORT, userSettings.getPort());
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class SimpleMap<K,V> extends AbstractMap<K,V> {

	/**
	 * Inner class representing a single key-value entry in this map.
	 * <p>
	 * This entry is backed by the underlying key and value arrays, so changes
	 * made via {@link #setValue(Object)} directly modify the underlying value array.
	 */
	class SimpleMapEntry implements Map.Entry<K,V> {

		/** The index into the keys/values arrays for this entry. */
		private final int index;

		/**
		 * Constructor.
		 * @param index The array index for this entry.
		 */
		SimpleMapEntry(int index) {
			this.index = index;
		}

		@Override /* Map.Entry */
		public K getKey() { return keys[index]; }

		@Override /* Map.Entry */
		public V getValue() { return values[index]; }

		@Override /* Map.Entry */
		public V setValue(V val) {
			var v = values[index];
			values[index] = val;
			return v;
		}
	}

	/** The array of keys. Keys are immutable after construction. */
	final K[] keys;

	/** The array of values. Values can be modified via {@link #put(Object, Object)}. */
	final V[] values;

	/** Pre-constructed entries array for {@link #entrySet()}. */
	final SimpleMapEntry[] entries;

	/**
	 * Constructs a new SimpleMap with the specified keys and values.
	 *
	 * <p>
	 * The keys and values arrays are stored by reference (not copied), so external
	 * modifications to the arrays after construction will be reflected in the map's behavior.
	 * However, the keys array should be treated as immutable after construction.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>keys</jv> = {<js>"name"</js>, <js>"age"</js>, <js>"city"</js>};
	 * 	Object[] <jv>values</jv> = {<js>"John"</js>, 30, <js>"NYC"</js>};
	 *
	 * 	SimpleMap&lt;String,Object&gt; <jv>person</jv> = <jk>new</jk> SimpleMap&lt;&gt;(<jv>keys</jv>, <jv>values</jv>);
	 * </p>
	 *
	 * @param keys The map keys. Must not be <jk>null</jk>. Individual keys can be <jk>null</jk>.
	 * @param values The map values. Must not be <jk>null</jk> and must have the same length as keys.
	 *               Individual values can be <jk>null</jk>.
	 * @throws IllegalArgumentException if:
	 *         <ul>
	 *         	<li>The keys array is <jk>null</jk>
	 *         	<li>The values array is <jk>null</jk>
	 *         	<li>The keys and values arrays have different lengths
	 *         	<li>Any key appears more than once (duplicate keys)
	 *         </ul>
	 */
	@SuppressWarnings("unchecked")
	public SimpleMap(K[] keys, V[] values) {
		assertArgsNotNull("keys", keys, "values", values);
		assertArg(keys.length == values.length, "keys ''{0}'' and values ''{1}'' array lengths differ", keys.length, values.length);

		// Check for duplicate keys
		for (var i = 0; i < keys.length; i++) {
			for (var j = i + 1; j < keys.length; j++) {
				assertArg(ne(keys[i], keys[j]), "Duplicate key found: {0}", keys[i]);
			}
		}

		this.keys = keys;
		this.values = values;
		entries = (SimpleMapEntry[])Array.newInstance(SimpleMapEntry.class, keys.length);
		for (var i = 0; i < keys.length; i++) {
			entries[i] = new SimpleMapEntry(i);
		}
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 *
	 * <p>
	 * The returned set is backed by the underlying entries array, so changes to the entry
	 * values via {@link java.util.Map.Entry#setValue(Object)} will be reflected in the map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>},
	 * 		<jk>new</jk> Integer[]{1, 2}
	 * 	);
	 *
	 * 	<jk>for</jk> (Map.Entry&lt;String,Integer&gt; <jv>entry</jv> : <jv>map</jv>.entrySet()) {
	 * 		<jv>entry</jv>.setValue(<jv>entry</jv>.getValue() * 10);  <jc>// Modifies underlying array</jc>
	 * 	}
	 * </p>
	 *
	 * @return A set view of the mappings in this map.
	 */
	@Override /* Map */
	public Set<Map.Entry<K,V>> entrySet() {
		return toSet(entries);
	}

	/**
	 * Returns the value associated with the specified key.
	 *
	 * <p>
	 * This method performs a linear search through the keys array, using {@link Object#equals(Object)}
	 * for comparison. For small maps (typically less than 10 entries), this is often faster than
	 * the hash lookup in {@link HashMap}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"age"</js>, <js>"score"</js>},
	 * 		<jk>new</jk> Integer[]{25, 100}
	 * 	);
	 *
	 * 	Integer <jv>age</jv> = <jv>map</jv>.get(<js>"age"</js>);     <jc>// Returns: 25</jc>
	 * 	Integer <jv>height</jv> = <jv>map</jv>.get(<js>"height"</js>); <jc>// Returns: null (key not found)</jc>
	 * </p>
	 *
	 * @param key The key whose associated value is to be returned.
	 * @return The value associated with the specified key, or <jk>null</jk> if the key is not found
	 *         (or if the key is mapped to <jk>null</jk>).
	 */
	@Override /* Map */
	public V get(Object key) {
		for (var i = 0; i < keys.length; i++)
			if (eq(keys[i], key))
				return values[i];
		return null;
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 *
	 * <p>
	 * The returned set is backed by the underlying keys array. Since the keys are immutable
	 * after construction, this set is effectively read-only.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"x"</js>, <js>"y"</js>, <js>"z"</js>},
	 * 		<jk>new</jk> Integer[]{1, 2, 3}
	 * 	);
	 *
	 * 	Set&lt;String&gt; <jv>keys</jv> = <jv>map</jv>.keySet();  <jc>// Returns: [x, y, z]</jc>
	 * </p>
	 *
	 * @return A set view of the keys in this map.
	 */
	@Override /* Map */
	public Set<K> keySet() {
		return toSet(keys);
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 *
	 * <p>
	 * This method can only update values for existing keys. It cannot add new keys to the map.
	 * If the specified key doesn't exist in the map, an {@link IllegalArgumentException} is thrown.
	 *
	 * <p>
	 * The value is updated in the underlying values array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"count"</js>, <js>"limit"</js>},
	 * 		<jk>new</jk> Integer[]{0, 100}
	 * 	);
	 *
	 * 	<jv>map</jv>.put(<js>"count"</js>, 5);       <jc>// Returns: 0 (previous value)</jc>
	 * 	<jv>map</jv>.put(<js>"newKey"</js>, 10);     <jc>// Throws IllegalArgumentException</jc>
	 * </p>
	 *
	 * @param key The key whose value is to be updated. Must be an existing key in this map.
	 * @param value The new value to associate with the key.
	 * @return The previous value associated with the specified key.
	 * @throws IllegalArgumentException if the specified key doesn't exist in this map.
	 */
	@Override /* Map */
	public V put(K key, V value) {
		for (var i = 0; i < keys.length; i++) {
			if (eq(keys[i], key)) {
				var v = values[i];
				values[i] = value;
				return v;
			}
		}
		throw illegalArg("No key ''{0}'' defined in map", key);
	}
}