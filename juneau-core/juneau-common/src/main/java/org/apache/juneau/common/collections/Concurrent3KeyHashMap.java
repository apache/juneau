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

import java.util.concurrent.*;

import org.apache.juneau.common.function.*;

/**
 * A thread-safe concurrent hash map that uses composite three-part keys for lookups.
 *
 * <p>
 * This class extends {@link ConcurrentHashMap} to provide efficient storage and retrieval of values
 * indexed by three separate key components. It's useful when you need to look up values based on a
 * combination of three keys without creating intermediate wrapper objects.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Three-Part Keys:</b> Values are indexed by a triplet of keys (K1, K2, K3) instead of a single key
 * 	<li><b>Thread-Safe:</b> Inherits all thread-safety guarantees from {@link ConcurrentHashMap}
 * 	<li><b>Null Keys Supported:</b> All key parts can be <jk>null</jk>
 * 	<li><b>Optional Caching:</b> Can be disabled to always invoke a supplier function instead of caching
 * 	<li><b>Supplier Integration:</b> Optionally provides automatic value computation via a {@link Function3}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map for caching translations by language, country, and key</jc>
 * 	Concurrent3KeyHashMap&lt;String,String,String,String&gt; <jv>translations</jv> =
 * 		<jk>new</jk> Concurrent3KeyHashMap&lt;&gt;();
 *
 * 	<jc>// Store a value</jc>
 * 	<jv>translations</jv>.put(<js>"en"</js>, <js>"US"</js>, <js>"greeting"</js>, <js>"Hello"</js>);
 * 	<jv>translations</jv>.put(<js>"fr"</js>, <js>"FR"</js>, <js>"greeting"</js>, <js>"Bonjour"</js>);
 *
 * 	<jc>// Retrieve values</jc>
 * 	String <jv>enGreeting</jv> = <jv>translations</jv>.get(<js>"en"</js>, <js>"US"</js>, <js>"greeting"</js>);
 * </p>
 *
 * <h5 class='section'>With Supplier:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map that automatically loads translations</jc>
 * 	Concurrent3KeyHashMap&lt;String,String,String,String&gt; <jv>cache</jv> =
 * 		<jk>new</jk> Concurrent3KeyHashMap&lt;&gt;(<jk>false</jk>, (lang, country, key) -&gt;
 * 			translationService.load(lang, country, key)
 * 		);
 *
 * 	<jc>// Automatically loads and caches if not present</jc>
 * 	String <jv>translation</jv> = <jv>cache</jv>.get(<js>"de"</js>, <js>"DE"</js>, <js>"greeting"</js>);
 * </p>
 *
 * <h5 class='section'>Disabled Mode:</h5>
 * <p>
 * When constructed with <c>disabled=<jk>true</jk></c>, the map doesn't actually store any values.
 * Instead, it always invokes the supplier function (if provided) or returns <jk>null</jk>.
 * This is useful for testing or development scenarios where caching should be bypassed.
 * </p>
 *
 * <p class='bjava'>
 * 	<jc>// Create a disabled cache (useful for testing)</jc>
 * 	Concurrent3KeyHashMap&lt;String,String,String,String&gt; <jv>cache</jv> =
 * 		<jk>new</jk> Concurrent3KeyHashMap&lt;&gt;(<jk>true</jk>, (lang, country, key) -&gt;
 * 			translationService.load(lang, country, key)
 * 		);
 *
 * 	<jc>// Always calls translationService.load(), never caches</jc>
 * 	String <jv>translation</jv> = <jv>cache</jv>.get(<js>"de"</js>, <js>"DE"</js>, <js>"greeting"</js>);
 * </p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Caching translations indexed by language, country, and message key
 * 	<li>Storing metrics indexed by date, category, and metric name
 * 	<li>Mapping configuration values by environment, tenant, and config key
 * 	<li>Caching query results indexed by database, schema, and query name
 * 	<li>Tracking permissions by user, resource, and action
 * </ul>
 *
 * <h5 class='section'>Key Hashing:</h5>
 * <p>
 * The composite key is hashed using the formula:
 * <p class='bjava'>
 * 	hash = 31 * (31 * k1.hashCode() + k2.hashCode()) + k3.hashCode()
 * </p>
 * <p>
 * Null keys are treated as having a hash code of 0. Keys are considered equal if all components
 * are equal according to {@link Object#equals(Object)}.
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class inherits all thread-safety properties from {@link ConcurrentHashMap}. Multiple threads
 * can safely read and write to the map concurrently without external synchronization.
 *
 * <h5 class='section'>Performance:</h5>
 * <ul class='spaced-list'>
 * 	<li>Average O(1) lookup and insertion time (inherited from {@link ConcurrentHashMap})
 * 	<li>Minimal object allocation: composite keys are created only during put/get operations
 * 	<li>Lock-free reads for existing entries (inherited from {@link ConcurrentHashMap})
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Concurrent2KeyHashMap}
 * 	<li class='jc'>{@link Concurrent4KeyHashMap}
 * 	<li class='jc'>{@link Concurrent5KeyHashMap}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key component type.
 * @param <K2> The second key component type.
 * @param <K3> The third key component type.
 * @param <V> The value type.
 * @serial exclude
 */
public class Concurrent3KeyHashMap<K1,K2,K3,V> extends ConcurrentHashMap<Tuple3<K1,K2,K3>,V> {

	private static final long serialVersionUID = 1L;
	private final boolean disabled;
	private final Function3<K1,K2,K3,V> supplier;

	/**
	 * Constructs a new Concurrent3KeyHashMap with caching enabled and no supplier.
	 */
	public Concurrent3KeyHashMap() {
		this(false, null);
	}

	/**
	 * Constructs a new Concurrent3KeyHashMap with optional caching and supplier.
	 *
	 * @param disabled If <jk>true</jk>, the map doesn't cache values.
	 * @param supplier Optional supplier function for automatic value computation.
	 */
	public Concurrent3KeyHashMap(boolean disabled, Function3<K1,K2,K3,V> supplier) {
		this.disabled = disabled;
		this.supplier = supplier;
	}

	/**
	 * Retrieves the value associated with the specified three-part key.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @return The value associated with the key, or <jk>null</jk> if not found.
	 */
	public V get(K1 key1, K2 key2, K3 key3) {
		if (disabled)
			return (supplier == null ? null : supplier.apply(key1, key2, key3));
		var key = Tuple3.of(key1, key2, key3);
		var v = super.get(key);
		if (v == null && supplier != null) {
			v = supplier.apply(key1, key2, key3);
			super.put(key, v);
		}
		return v;
	}

	/**
	 * Associates the specified value with the specified three-part key in this map.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 */
	public V put(K1 key1, K2 key2, K3 key3, V value) {
		if (disabled)
			return null;
		var key = Tuple3.of(key1, key2, key3);
		return super.put(key, value);
	}
}
