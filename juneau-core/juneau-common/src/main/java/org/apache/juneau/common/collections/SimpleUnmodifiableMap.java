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

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * An unmodifiable, fixed-size map implementation backed by parallel key and value arrays.
 *
 * <p>
 * This class provides a simple, efficient, immutable map implementation for scenarios where the
 * set of keys and values is known in advance and doesn't need to change. It's particularly useful
 * for small maps (typically less than 10 entries) where the overhead of a {@link HashMap} is not
 * justified.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Unmodifiable:</b> All mutation operations throw {@link UnsupportedOperationException}
 * 	<li><b>Fixed Keys and Values:</b> Keys and values are set at construction time
 * 	<li><b>Insertion Order:</b> Preserves the order of keys as provided in the constructor
 * 	<li><b>Null Support:</b> Supports null keys and values (unlike {@link Map#of()})
 * 	<li><b>Array-Backed:</b> Uses simple arrays for storage, avoiding hash computation overhead
 * 	<li><b>Memory Efficient:</b> Minimal memory footprint compared to {@link HashMap}
 * 	<li><b>Predictable Performance:</b> O(n) lookup time, but faster than {@link HashMap} for small n
 * </ul>
 *
 * <h5 class='section'>Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Immutable configuration maps with a fixed set of known keys
 * 	<li>Small lookup tables that should not be modified
 * 	<li>Method return values where immutability is desired
 * 	<li>Situations where map size is small and keys are known at compile time
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create an unmodifiable map with fixed keys and values</jc>
 * 	String[] <jv>keys</jv> = {<js>"host"</js>, <js>"port"</js>, <js>"timeout"</js>};
 * 	Object[] <jv>values</jv> = {<js>"localhost"</js>, 8080, 30000};
 *
 * 	SimpleUnmodifiableMap&lt;String,Object&gt; <jv>config</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(<jv>keys</jv>, <jv>values</jv>);
 *
 * 	<jc>// Get values</jc>
 * 	String <jv>host</jv> = (String)<jv>config</jv>.get(<js>"host"</js>);     <jc>// Returns: "localhost"</jc>
 * 	Integer <jv>port</jv> = (Integer)<jv>config</jv>.get(<js>"port"</js>);   <jc>// Returns: 8080</jc>
 *
 * 	<jc>// Cannot modify</jc>
 * 	<jv>config</jv>.put(<js>"port"</js>, 9090);  <jc>// Throws UnsupportedOperationException</jc>
 * </p>
 *
 * <h5 class='section'>Restrictions:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Immutable:</b> Cannot add, remove, or modify entries after construction
 * 	<li><b>Fixed Size:</b> Size is determined at construction time
 * 	<li><b>Unique Keys:</b> Keys must be unique (no duplicates)
 * 	<li><b>Array Length:</b> Keys and values arrays must have the same length
 * </ul>
 *
 * <h5 class='section'>Performance Characteristics:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>get(key):</b> O(n) - Linear search through keys array
 * 	<li><b>size():</b> O(1) - Direct array length access
 * 	<li><b>Memory:</b> Lower overhead than {@link HashMap} for small maps
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe since it's immutable after construction. Multiple threads can safely
 * read from a SimpleUnmodifiableMap concurrently without synchronization.
 *
 * <h5 class='section'>Example - Configuration Map:</h5>
 * <p class='bjava'>
 * 	<jc>// Create immutable configuration</jc>
 * 	SimpleUnmodifiableMap&lt;String,Object&gt; <jv>defaults</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(
 * 		<jk>new</jk> String[]{<js>"maxConnections"</js>, <js>"timeout"</js>, <js>"retries"</js>},
 * 		<jk>new</jk> Object[]{100, 5000, 3}
 * 	);
 *
 * 	<jc>// Safe to share across threads</jc>
 * 	<jk>return</jk> <jv>defaults</jv>;
 * </p>
 *
 * <h5 class='section'>Comparison with Map.of():</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Null Support:</b> SimpleUnmodifiableMap supports null keys/values, Map.of() does not
 * 	<li><b>Insertion Order:</b> SimpleUnmodifiableMap preserves insertion order, Map.of() does not
 * 	<li><b>Size Limit:</b> Map.of() limited to 10 entries, SimpleUnmodifiableMap has no limit
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link SimpleMap} - The modifiable version of this class
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class SimpleUnmodifiableMap<K,V> extends AbstractMap<K,V> {

	/**
	 * Inner class representing a single key-value entry in this map.
	 * <p>
	 * This entry is unmodifiable - calling {@link #setValue(Object)} will throw
	 * {@link UnsupportedOperationException}.
	 */
	class SimpleUnmodifiableMapEntry implements Map.Entry<K,V> {

		/** The index into the keys/values arrays for this entry. */
		private final int index;

		/**
		 * Constructor.
		 * @param index The array index for this entry.
		 */
		SimpleUnmodifiableMapEntry(int index) {
			this.index = index;
		}

		@Override /* Map.Entry */
		public K getKey() { return keys[index]; }

		@Override /* Map.Entry */
		public V getValue() { return values[index]; }

		@Override /* Map.Entry */
		public V setValue(V val) {
			throw unsupportedOp("Map is unmodifiable");
		}
	}

	/** The array of keys. Keys are immutable after construction. */
	final K[] keys;

	/** The array of values. Values are immutable after construction. */
	final V[] values;

	/** Pre-constructed entries array for {@link #entrySet()}. */
	final SimpleUnmodifiableMapEntry[] entries;

	/**
	 * Constructs a new SimpleUnmodifiableMap with the specified keys and values.
	 *
	 * <p>
	 * The keys and values arrays are stored by reference (not copied), so external
	 * modifications to the arrays after construction will affect the map's behavior.
	 * For true immutability, ensure the arrays are not modified after passing them
	 * to this constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	String[] <jv>keys</jv> = {<js>"name"</js>, <js>"age"</js>, <js>"city"</js>};
	 * 	Object[] <jv>values</jv> = {<js>"John"</js>, 30, <js>"NYC"</js>};
	 *
	 * 	SimpleUnmodifiableMap&lt;String,Object&gt; <jv>person</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(<jv>keys</jv>, <jv>values</jv>);
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
	public SimpleUnmodifiableMap(K[] keys, V[] values) {
		assertArgsNotNull("keys", keys, "values", values);
		assertArg(keys.length == values.length, "keys ''{0}'' and values ''{1}'' array lengths differ", keys.length, values.length);

		// Check for duplicate keys
		for (var i = 0; i < keys.length; i++) {
			for (var j = i + 1; j < keys.length; j++) {
				if (eq(keys[i], keys[j])) {
					throw illegalArg("Duplicate key found: {0}", keys[i]);
				}
			}
		}

		this.keys = keys;
		this.values = values;
		entries = (SimpleUnmodifiableMapEntry[])Array.newInstance(SimpleUnmodifiableMapEntry.class, keys.length);
		for (var i = 0; i < keys.length; i++) {
			entries[i] = new SimpleUnmodifiableMapEntry(i);
		}
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 *
	 * <p>
	 * The returned set is unmodifiable and backed by the underlying entries array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleUnmodifiableMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>},
	 * 		<jk>new</jk> Integer[]{1, 2}
	 * 	);
	 *
	 * 	<jk>for</jk> (Map.Entry&lt;String,Integer&gt; <jv>entry</jv> : <jv>map</jv>.entrySet()) {
	 * 		System.<jsf>out</jsf>.println(<jv>entry</jv>.getKey() + <js>"="</js> + <jv>entry</jv>.getValue());
	 * 	}
	 * </p>
	 *
	 * @return An unmodifiable set view of the mappings in this map.
	 */
	@Override /* Map */
	public Set<Map.Entry<K,V>> entrySet() {
		return Collections.unmodifiableSet(toSet(entries));
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
	 * 	SimpleUnmodifiableMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(
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
	 * The returned set is unmodifiable and backed by the underlying keys array.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	SimpleUnmodifiableMap&lt;String,Integer&gt; <jv>map</jv> = <jk>new</jk> SimpleUnmodifiableMap&lt;&gt;(
	 * 		<jk>new</jk> String[]{<js>"x"</js>, <js>"y"</js>, <js>"z"</js>},
	 * 		<jk>new</jk> Integer[]{1, 2, 3}
	 * 	);
	 *
	 * 	Set&lt;String&gt; <jv>keys</jv> = <jv>map</jv>.keySet();  <jc>// Returns: [x, y, z]</jc>
	 * </p>
	 *
	 * @return An unmodifiable set view of the keys in this map.
	 */
	@Override /* Map */
	public Set<K> keySet() {
		return Collections.unmodifiableSet(toSet(keys));
	}

	/**
	 * Throws {@link UnsupportedOperationException} as this map is unmodifiable.
	 *
	 * @param key Ignored.
	 * @param value Ignored.
	 * @return Never returns normally.
	 * @throws UnsupportedOperationException Always thrown, as this map cannot be modified.
	 */
	@Override /* Map */
	public V put(K key, V value) {
		throw unsupportedOp("Map is unmodifiable");
	}
}

