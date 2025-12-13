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
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.commons.utils.Utils;
import java.util.stream.Collectors;

/**
 * A composite map that presents multiple maps as a single unified map.
 *
 * <p>
 * This class allows multiple maps to be viewed and accessed as if they were merged into
 * a single map, without actually copying the entries. When the same key exists in multiple maps,
 * the value from the first map (in constructor order) is returned.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Zero-Copy Composition:</b> No data is copied when creating a MultiMap; it simply wraps the provided maps
 * 	<li><b>Transparent Access:</b> Accessing entries by key seamlessly searches all underlying maps in order
 * 	<li><b>First-Wins Semantics:</b> When a key exists in multiple maps, the value from the first map is returned
 * 	<li><b>Modification Support:</b> Entries can be removed via the iterator's {@link Iterator#remove()} method
 * 	<li><b>Efficient Size Calculation:</b> The size is computed by counting unique keys across all maps
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a MultiMap from three separate maps</jc>
 * 	Map&lt;String, String&gt; <jv>map1</jv> = Map.of(<js>"key1"</js>, <js>"value1"</js>, <js>"key2"</js>, <js>"value2"</js>);
 * 	Map&lt;String, String&gt; <jv>map2</jv> = Map.of(<js>"key3"</js>, <js>"value3"</js>, <js>"key2"</js>, <js>"value2b"</js>);
 * 	Map&lt;String, String&gt; <jv>map3</jv> = Map.of(<js>"key4"</js>, <js>"value4"</js>);
 *
 * 	MultiMap&lt;String, String&gt; <jv>multiMap</jv> = <jk>new</jk> MultiMap&lt;&gt;(<jv>map1</jv>, <jv>map2</jv>, <jv>map3</jv>);
 *
 * 	<jc>// Access entries by key</jc>
 * 	<jv>multiMap</jv>.get(<js>"key1"</js>);  <jc>// Returns "value1" from map1</jc>
 * 	<jv>multiMap</jv>.get(<js>"key2"</js>);  <jc>// Returns "value2" from map1 (first match wins)</jc>
 * 	<jv>multiMap</jv>.get(<js>"key3"</js>);  <jc>// Returns "value3" from map2</jc>
 *
 * 	<jc>// Iterate over all entries from all maps</jc>
 * 	<jk>for</jk> (Map.Entry&lt;String, String&gt; <jv>entry</jv> : <jv>multiMap</jv>.entrySet()) {
 * 		System.<jsf>out</jsf>.println(<jv>entry</jv>); <jc>// Prints entries from all maps</jc>
 * 	}
 *
 * 	<jc>// Get total size (unique keys across all maps)</jc>
 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiMap</jv>.size(); <jc>// Returns: 4 (key1, key2, key3, key4)</jc>
 * </p>
 *
 * <h5 class='section'>Behavior Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>The order of key lookup follows the order of maps as provided in the constructor
 * 	<li>When a key exists in multiple maps, {@link #get(Object)} returns the value from the first map containing that key
 * 	<li>The underlying maps must not be <jk>null</jk>, but can be empty
 * 	<li>Modifications via {@link Iterator#remove()} are delegated to the underlying map's entry set iterator
 * 	<li>This class does not support {@link #put(Object, Object)}, {@link #remove(Object)}, or {@link #clear()} operations
 * 	<li>The {@link #size()} method recomputes the count of unique keys each time it's called (not cached)
 * 	<li>The {@link #entrySet()} iterator only returns each key once (first occurrence), even if it exists in multiple maps
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is not inherently thread-safe. If the underlying maps are modified concurrently
 * during iteration or access, the behavior is undefined. Synchronization must be handled externally if needed.
 *
 * <h5 class='section'>Example - Processing Multiple Configuration Sources:</h5>
 * <p class='bjava'>
 * 	<jc>// Combine configuration from system properties, environment, and defaults</jc>
 * 	Map&lt;String, String&gt; <jv>systemProps</jv> = getSystemProperties();
 * 	Map&lt;String, String&gt; <jv>envVars</jv> = getEnvironmentVariables();
 * 	Map&lt;String, String&gt; <jv>defaults</jv> = getDefaultConfig();
 *
 * 	MultiMap&lt;String, String&gt; <jv>config</jv> = <jk>new</jk> MultiMap&lt;&gt;(<jv>systemProps</jv>, <jv>envVars</jv>, <jv>defaults</jv>);
 *
 * 	<jc>// Access configuration (system props take precedence, then env vars, then defaults)</jc>
 * 	String <jv>host</jv> = <jv>config</jv>.get(<js>"host"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K> The key type of this map.
 * @param <V> The value type of this map.
 */
public class MultiMap<K,V> extends AbstractMap<K,V> {

	/**
	 * The underlying maps being wrapped by this MultiMap.
	 * <p>
	 * These maps are accessed directly during key lookup and iteration without copying.
	 */
	final Map<K,V>[] m;

	/**
	 * Creates a new MultiMap that presents the specified maps as a single unified map.
	 *
	 * <p>
	 * The maps are stored by reference (not copied), so modifications made through the
	 * MultiMap's iterator will affect the original maps.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String, String&gt; <jv>map1</jv> = <jk>new</jk> LinkedHashMap&lt;&gt;(Map.of(<js>"a"</js>, <js>"1"</js>));
	 * 	Map&lt;String, String&gt; <jv>map2</jv> = <jk>new</jk> LinkedHashMap&lt;&gt;(Map.of(<js>"b"</js>, <js>"2"</js>));
	 *
	 * 	MultiMap&lt;String, String&gt; <jv>multiMap</jv> = <jk>new</jk> MultiMap&lt;&gt;(<jv>map1</jv>, <jv>map2</jv>);
	 * 	<jc>// multiMap now represents all entries from both maps</jc>
	 * </p>
	 *
	 * @param maps Zero or more maps to combine into this map. Must not be <jk>null</jk>,
	 *           and no individual map can be <jk>null</jk> (but maps can be empty).
	 * @throws IllegalArgumentException if the maps array or any map within it is <jk>null</jk>.
	 */
	@SafeVarargs
	public MultiMap(Map<K,V>...maps) {
		assertArgNotNull("maps", maps);
		for (var map : maps)
			assertArgNotNull("maps", map);
		m = maps;
	}

	/**
	 * Returns the value to which the specified key is mapped, or <jk>null</jk> if this map contains no mapping for the key.
	 *
	 * <p>
	 * This method searches the underlying maps in the order they were provided to the constructor.
	 * The first map containing the key determines the returned value.
	 *
	 * @param key The key whose associated value is to be returned.
	 * @return The value to which the specified key is mapped, or <jk>null</jk> if this map contains no mapping for the key.
	 */
	@Override
	public V get(Object key) {
		for (var map : m) {
			if (map.containsKey(key))
				return map.get(key);
		}
		return null;
	}

	/**
	 * Returns <jk>true</jk> if this map contains a mapping for the specified key.
	 *
	 * @param key The key whose presence in this map is to be tested.
	 * @return <jk>true</jk> if this map contains a mapping for the specified key.
	 */
	@Override
	public boolean containsKey(Object key) {
		for (var map : m) {
			if (map.containsKey(key))
				return true;
		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this map maps one or more keys to the specified value.
	 *
	 * @param value The value whose presence in this map is to be tested.
	 * @return <jk>true</jk> if this map maps one or more keys to the specified value.
	 */
	@Override
	public boolean containsValue(Object value) {
		for (var map : m) {
			if (map.containsValue(value))
				return true;
		}
		return false;
	}

	/**
	 * Returns a {@link Set} view of the mappings contained in this map.
	 *
	 * <p>
	 * The returned set is a composite view of all underlying maps. When a key exists in multiple maps,
	 * only the entry from the first map (in constructor order) is included.
	 *
	 * <p>
	 * The iterator supports the {@link Iterator#remove()} operation, which removes the entry
	 * from its underlying map.
	 *
	 * @return A set view of the mappings contained in this map.
	 */
	@Override
	public Set<Entry<K,V>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<K,V>> iterator() {
				return new Iterator<>() {
					int mapIndex = 0;
					Iterator<Entry<K,V>> currentIterator;
					Set<K> seenKeys = new HashSet<>();
					Entry<K,V> nextEntry;
					Iterator<Entry<K,V>> lastIterator; // Iterator that produced the last entry
					boolean canRemove = false; // Whether remove() can be called

					{
						// Initialize to first map's iterator
						if (m.length > 0) {
							currentIterator = m[0].entrySet().iterator();
							advance();
						}
					}

					private void advance() {
						nextEntry = null;
						while (currentIterator != null) {
							while (currentIterator.hasNext()) {
								var entry = currentIterator.next();
								if (!seenKeys.contains(entry.getKey())) {
									seenKeys.add(entry.getKey());
									nextEntry = entry;
									return;
								}
							}
							// Move to next map
							mapIndex++;
							if (mapIndex < m.length) {
								currentIterator = m[mapIndex].entrySet().iterator();
							} else {
								currentIterator = null;
							}
						}
					}

					@Override
					public boolean hasNext() {
						return nextEntry != null;
					}

					@Override
					public Entry<K,V> next() {
						if (nextEntry == null)
							throw new NoSuchElementException();
						var result = nextEntry;
						lastIterator = currentIterator; // Store the iterator that produced this entry
						canRemove = true;
						advance();
						return result;
					}

					@Override
					public void remove() {
						if (!canRemove || lastIterator == null)
							throw new IllegalStateException();
						lastIterator.remove();
						canRemove = false;
					}
				};
			}

			@Override
			public int size() {
				return MultiMap.this.size();
			}
		};
	}

	/**
	 * Returns the number of unique key-value mappings in this map.
	 *
	 * <p>
	 * This method computes the size by counting unique keys across all underlying maps.
	 * If a key exists in multiple maps, it is counted only once. The size is recalculated
	 * each time this method is called (it is not cached).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String, String&gt; <jv>map1</jv> = Map.of(<js>"a"</js>, <js>"1"</js>, <js>"b"</js>, <js>"2"</js>);  <jc>// size = 2</jc>
	 * 	Map&lt;String, String&gt; <jv>map2</jv> = Map.of(<js>"b"</js>, <js>"3"</js>, <js>"c"</js>, <js>"4"</js>);  <jc>// size = 2</jc>
	 * 	MultiMap&lt;String, String&gt; <jv>multiMap</jv> = <jk>new</jk> MultiMap&lt;&gt;(<jv>map1</jv>, <jv>map2</jv>);
	 *
	 * 	<jk>int</jk> <jv>totalSize</jv> = <jv>multiMap</jv>.size(); <jc>// Returns: 3 (a, b, c - b is counted only once)</jc>
	 * </p>
	 *
	 * @return The number of unique key-value mappings in this map.
	 */
	@Override
	public int size() {
		var seenKeys = new HashSet<>();
		for (var map : m) {
			seenKeys.addAll(map.keySet());
		}
		return seenKeys.size();
	}

	/**
	 * Returns <jk>true</jk> if this map contains no key-value mappings.
	 *
	 * @return <jk>true</jk> if this map contains no key-value mappings.
	 */
	@Override
	public boolean isEmpty() {
		for (var map : m) {
			if (!map.isEmpty())
				return false;
		}
		return true;
	}

	/**
	 * Returns a {@link Collection} view of the values contained in this map.
	 *
	 * <p>
	 * The returned collection is a view of the values from the entries in {@link #entrySet()}.
	 * When a key exists in multiple maps, only the value from the first map (in constructor order)
	 * is included.
	 *
	 * @return A collection view of the values contained in this map.
	 */
	@Override
	public Collection<V> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<>() {
					private final Iterator<Entry<K,V>> entryIterator = entrySet().iterator();

					@Override
					public boolean hasNext() {
						return entryIterator.hasNext();
					}

					@Override
					public V next() {
						return entryIterator.next().getValue();
					}

					@Override
					public void remove() {
						entryIterator.remove();
					}
				};
			}

			@Override
			public int size() {
				return MultiMap.this.size();
			}
		};
	}

	// Unsupported operations
	@Override
	public V put(K key, V value) {
		throw unsupportedOp("put() not supported on MultiMap. Use underlying maps directly.");
	}

	@Override
	public V remove(Object key) {
		throw unsupportedOp("remove() not supported on MultiMap. Use underlying maps directly.");
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw unsupportedOp("putAll() not supported on MultiMap. Use underlying maps directly.");
	}

	@Override
	public void clear() {
		throw unsupportedOp("clear() not supported on MultiMap. Use underlying maps directly.");
	}

	/**
	 * Returns a string representation of this MultiMap.
	 *
	 * <p>
	 * The format is <c>"[{...},{...},...]"</c> where each <c>{...}</c> is the standard
	 * {@link Map#toString()} representation of each underlying map.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String, String&gt; <jv>map1</jv> = Map.of(<js>"a"</js>, <js>"1"</js>);
	 * 	Map&lt;String, String&gt; <jv>map2</jv> = Map.of(<js>"b"</js>, <js>"2"</js>);
	 * 	MultiMap&lt;String, String&gt; <jv>multiMap</jv> = <jk>new</jk> MultiMap&lt;&gt;(<jv>map1</jv>, <jv>map2</jv>);
	 * 	<jv>multiMap</jv>.toString(); <jc>// Returns: "[{a=1}, {b=2}]"</jc>
	 * </p>
	 *
	 * @return A string representation of this MultiMap.
	 */
	@Override
	public String toString() {
		return Arrays.stream(m).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * Compares the specified object with this map for equality.
	 *
	 * <p>
	 * Returns <jk>true</jk> if the given object is also a map and the two maps represent the same
	 * mappings. More formally, two maps <c>m1</c> and <c>m2</c> represent the same mappings if
	 * <c>m1.entrySet().equals(m2.entrySet())</c>.
	 *
	 * <p>
	 * This implementation compares the entry sets of the two maps.
	 *
	 * @param o Object to be compared for equality with this map.
	 * @return <jk>true</jk> if the specified object is equal to this map.
	 */
	@Override
	public boolean equals(Object o) {
		return (o instanceof Map o2) && Utils.eq(this, o2, (x, y) -> x.entrySet().equals(y.entrySet()));
	}

	/**
	 * Returns the hash code value for this map.
	 *
	 * <p>
	 * The hash code of a map is defined to be the sum of the hash codes of each entry in the map's
	 * <c>entrySet()</c> view. This ensures that <c>m1.equals(m2)</c> implies that
	 * <c>m1.hashCode()==m2.hashCode()</c> for any two maps <c>m1</c> and <c>m2</c>, as required
	 * by the general contract of {@link Object#hashCode()}.
	 *
	 * <p>
	 * This implementation computes the hash code from the entry set.
	 *
	 * @return The hash code value for this map.
	 */
	@Override
	public int hashCode() {
		return entrySet().hashCode();
	}
}

