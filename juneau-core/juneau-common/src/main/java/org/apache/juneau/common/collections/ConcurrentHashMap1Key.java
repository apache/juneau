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

import java.util.concurrent.*;

import org.apache.juneau.common.function.*;

/**
 * A thread-safe concurrent hash map that uses a single-value wrapper key for lookups.
 *
 * <p>
 * This class extends {@link ConcurrentHashMap} to provide efficient storage and retrieval of values
 * indexed by a single key component wrapped in a {@link Tuple1}. The primary use case is when you need
 * content-based equality for keys that would otherwise use identity-based equality (such as arrays).
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Content-Based Keys:</b> Keys use content-based equality via {@link Tuple1}
 * 	<li><b>Array Support:</b> Arrays can be used as keys with proper content-based hashing and equality
 * 	<li><b>Thread-Safe:</b> Inherits all thread-safety guarantees from {@link ConcurrentHashMap}
 * 	<li><b>Null Keys Supported:</b> Key can be <jk>null</jk>
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map for caching results indexed by array keys</jc>
 * 	ConcurrentHashMap1Key&lt;String[],Result&gt; <jv>cache</jv> =
 * 		<jk>new</jk> ConcurrentHashMap1Key&lt;&gt;();
 *
 * 	<jc>// Store a value</jc>
 * 	<jv>cache</jv>.put(<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>}, <jv>result1</jv>);
 *
 * 	<jc>// Retrieve a value (works with different array instance but same content)</jc>
 * 	Result <jv>cached</jv> = <jv>cache</jv>.get(<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>});  <jc>// Returns result1</jc>
 * </p>
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike standard {@link java.util.HashMap} which uses identity-based equality for array keys,
 * this class properly handles arrays using content-based comparison:
 *
 * <p class='bjava'>
 * 	<jc>// Arrays work correctly as keys</jc>
 * 	ConcurrentHashMap1Key&lt;int[],String&gt; <jv>map</jv> = <jk>new</jk> ConcurrentHashMap1Key&lt;&gt;();
 * 	<jv>map</jv>.put(<jk>new</jk> <jk>int</jk>[]{1,2,3}, <js>"foo"</js>);
 * 	String <jv>result</jv> = <jv>map</jv>.get(<jk>new</jk> <jk>int</jk>[]{1,2,3});  <jc>// Returns "foo"</jc>
 * </p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Caching results indexed by array keys (e.g., annotation traversal patterns)
 * 	<li>Storing configuration indexed by enum arrays
 * 	<li>Mapping results by method parameter arrays
 * 	<li>Any scenario requiring content-based equality for otherwise identity-based types
 * </ul>
 *
 * <h5 class='section'>Key Hashing:</h5>
 * <p>
 * Keys are wrapped in {@link Tuple1} which provides content-based hashing.
 * For arrays, {@link java.util.Arrays#hashCode(Object[])} is used to ensure
 * consistent hashing based on array contents rather than identity.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class inherits all thread-safety properties from {@link ConcurrentHashMap}. Multiple threads
 * can safely read and write to the map concurrently without external synchronization.
 *
 * <h5 class='section'>Performance:</h5>
 * <ul class='spaced-list'>
 * 	<li>Average O(1) lookup and insertion time (inherited from {@link ConcurrentHashMap})
 * 	<li>Minimal object allocation: tuple keys are created only during put/get operations
 * 	<li>Lock-free reads for existing entries (inherited from {@link ConcurrentHashMap})
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ConcurrentHashMap2Key}
 * 	<li class='jc'>{@link ConcurrentHashMap3Key}
 * 	<li class='jc'>{@link ConcurrentHashMap4Key}
 * 	<li class='jc'>{@link ConcurrentHashMap5Key}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 * @serial exclude
 */
public class ConcurrentHashMap1Key<K,V> extends ConcurrentHashMap<Tuple1<K>,V> {

	private static final long serialVersionUID = 1L;

	/**
	 * Retrieves the value associated with the specified key.
	 *
	 * @param key Key component. Must not be <jk>null</jk>.
	 * @return The value associated with the key, or <jk>null</jk> if not found.
	 * @throws IllegalArgumentException if key is <jk>null</jk>.
	 */
	public V getKey(K key) {
		assertArgNotNull("key", key);
		return super.get(Tuple1.of(key));
	}

	/**
	 * Associates the specified value with the specified key in this map.
	 *
	 * @param key Key component. Must not be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 * @throws IllegalArgumentException if key is <jk>null</jk>.
	 */
	public V putKey(K key, V value) {
		assertArgNotNull("key", key);
		return super.put(Tuple1.of(key), value);
	}
}

