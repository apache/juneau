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
 * A thread-safe concurrent hash map that uses composite two-part keys for lookups.
 *
 * <p>
 * This class extends {@link ConcurrentHashMap} to provide efficient storage and retrieval of values
 * indexed by two separate key components. It's useful when you need to look up values based on a
 * combination of two keys without creating intermediate wrapper objects.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Two-Part Keys:</b> Values are indexed by a pair of keys (K1, K2) instead of a single key
 * 	<li><b>Thread-Safe:</b> Inherits all thread-safety guarantees from {@link ConcurrentHashMap}
 * 	<li><b>Null Keys Supported:</b> Both key parts can be <jk>null</jk>
 * 	<li><b>Optional Caching:</b> Can be disabled to always invoke a supplier function instead of caching
 * 	<li><b>Supplier Integration:</b> Optionally provides automatic value computation via a {@link Function2}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map for storing class metadata indexed by ClassLoader and Class</jc>
 * 	Concurrent2KeyHashMap&lt;ClassLoader,Class&lt;?&gt;,ClassMeta&gt; <jv>map</jv> =
 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;();
 *
 * 	<jc>// Store a value</jc>
 * 	<jv>map</jv>.put(<jv>classLoader</jv>, String.<jk>class</jk>, <jv>stringMeta</jv>);
 *
 * 	<jc>// Retrieve a value</jc>
 * 	ClassMeta <jv>meta</jv> = <jv>map</jv>.get(<jv>classLoader</jv>, String.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>With Supplier:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map that automatically computes values when missing</jc>
 * 	Concurrent2KeyHashMap&lt;String,Integer,String&gt; <jv>cache</jv> =
 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;(<jk>false</jk>, (name, id) -&gt;
 * 			fetchFromDatabase(name, id)
 * 		);
 *
 * 	<jc>// Automatically computes and caches if not present</jc>
 * 	String <jv>value</jv> = <jv>cache</jv>.get(<js>"user"</js>, 123);
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
 * 	Concurrent2KeyHashMap&lt;String,String,Config&gt; <jv>cache</jv> =
 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;(<jk>true</jk>, (env, name) -&gt;
 * 			loadConfig(env, name)
 * 		);
 *
 * 	<jc>// Always calls loadConfig(), never caches</jc>
 * 	Config <jv>config</jv> = <jv>cache</jv>.get(<js>"prod"</js>, <js>"app"</js>);
 * </p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Caching class metadata indexed by ClassLoader and Class
 * 	<li>Storing configuration values indexed by environment and name
 * 	<li>Mapping database entities by table name and primary key
 * 	<li>Maintaining translation caches indexed by locale and message key
 * 	<li>Tracking statistics indexed by date and metric name
 * </ul>
 *
 * <h5 class='section'>Key Hashing:</h5>
 * <p>
 * The composite key is hashed using the formula:
 * <p class='bjava'>
 * 	hash = 31 * k1.hashCode() + k2.hashCode()
 * </p>
 * <p>
 * Null keys are treated as having a hash code of 0. Keys are considered equal if both components
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key component type.
 * @param <K2> The second key component type.
 * @param <V> The value type.
 * @serial exclude
 */
public class Concurrent2KeyHashMap<K1,K2,V> extends ConcurrentHashMap<Tuple2<K1,K2>,V> {

	private static final long serialVersionUID = 1L;

	/** If true, get/put operations don't cache values. */
	private final boolean disabled;

	/** Optional supplier function for automatic value computation. */
	private final Function2<K1,K2,V> supplier;

	/**
	 * Constructs a new Concurrent2KeyHashMap with caching enabled and no supplier.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Concurrent2KeyHashMap&lt;String,String,Config&gt; <jv>map</jv> =
	 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;();
	 * </p>
	 */
	public Concurrent2KeyHashMap() {
		this(false, null);
	}

	/**
	 * Constructs a new Concurrent2KeyHashMap with optional caching and supplier.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// With supplier for automatic value computation</jc>
	 * 	Concurrent2KeyHashMap&lt;String,Integer,User&gt; <jv>userCache</jv> =
	 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;(<jk>false</jk>, (tenant, id) -&gt;
	 * 			userService.findUser(tenant, id)
	 * 		);
	 *
	 * 	<jc>// Disabled mode (no caching, always calls supplier)</jc>
	 * 	Concurrent2KeyHashMap&lt;String,String,Config&gt; <jv>configCache</jv> =
	 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;(<jk>true</jk>, (env, name) -&gt;
	 * 			configService.load(env, name)
	 * 		);
	 * </p>
	 *
	 * @param disabled If <jk>true</jk>, the map doesn't cache values. The {@link #get(Object, Object)} method
	 *                 will always invoke the supplier (if provided) instead of caching results.
	 * @param supplier Optional supplier function for automatic value computation when a key is not found.
	 *                 Called with both key components as arguments. Can be <jk>null</jk>.
	 */
	public Concurrent2KeyHashMap(boolean disabled, Function2<K1,K2,V> supplier) {
		this.disabled = disabled;
		this.supplier = supplier;
	}

	/**
	 * Retrieves the value associated with the specified two-part key.
	 *
	 * <p>
	 * If the key is not found and a supplier was provided in the constructor, the supplier
	 * is invoked to compute the value, which is then cached and returned.
	 *
	 * <p>
	 * If the map is disabled, the supplier is always invoked without caching, or <jk>null</jk>
	 * is returned if no supplier was provided.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Concurrent2KeyHashMap&lt;String,Integer,User&gt; <jv>userMap</jv> =
	 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;();
	 *
	 * 	<jv>userMap</jv>.put(<js>"tenant1"</js>, 123, <jv>user1</jv>);
	 *
	 * 	User <jv>found</jv> = <jv>userMap</jv>.get(<js>"tenant1"</js>, 123);      <jc>// Returns user1</jc>
	 * 	User <jv>notFound</jv> = <jv>userMap</jv>.get(<js>"tenant2"</js>, 456);  <jc>// Returns null</jc>
	 * </p>
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @return The value associated with the key, or <jk>null</jk> if not found and no supplier provided.
	 */
	public V get(K1 key1, K2 key2) {
		if (disabled)
			return (supplier == null ? null : supplier.apply(key1, key2));
		var key = Tuple2.of(key1, key2);
		var v = super.get(key);
		if (v == null && supplier != null) {
			v = supplier.apply(key1, key2);
			super.put(key, v);
		}
		return v;
	}

	/**
	 * Associates the specified value with the specified two-part key in this map.
	 *
	 * <p>
	 * If the map is disabled, this method does nothing and returns <jk>null</jk>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Concurrent2KeyHashMap&lt;String,Integer,User&gt; <jv>userMap</jv> =
	 * 		<jk>new</jk> Concurrent2KeyHashMap&lt;&gt;();
	 *
	 * 	User <jv>previousValue</jv> = <jv>userMap</jv>.put(<js>"tenant1"</js>, 123, <jv>newUser</jv>);
	 *
	 * 	<jc>// Update existing entry</jc>
	 * 	User <jv>oldUser</jv> = <jv>userMap</jv>.put(<js>"tenant1"</js>, 123, <jv>updatedUser</jv>);
	 * 	<jc>// oldUser contains the previous value (newUser)</jc>
	 * </p>
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping
	 *         for the key (or if the map is disabled).
	 */
	public V put(K1 key1, K2 key2, V value) {
		if (disabled)
			return null;
		var key = Tuple2.of(key1, key2);
		return super.put(key, value);
	}
}