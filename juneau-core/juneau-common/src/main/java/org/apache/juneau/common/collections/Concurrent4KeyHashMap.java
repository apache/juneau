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
 * A thread-safe concurrent hash map that uses composite four-part keys for lookups.
 *
 * <p>
 * This class extends {@link ConcurrentHashMap} to provide efficient storage and retrieval of values
 * indexed by four separate key components. It's useful when you need to look up values based on a
 * combination of four keys without creating intermediate wrapper objects.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Four-Part Keys:</b> Values are indexed by four keys (K1, K2, K3, K4) instead of a single key
 * 	<li><b>Thread-Safe:</b> Inherits all thread-safety guarantees from {@link ConcurrentHashMap}
 * 	<li><b>Null Keys Supported:</b> All key parts can be <jk>null</jk>
 * 	<li><b>Optional Caching:</b> Can be disabled to always invoke a supplier function instead of caching
 * 	<li><b>Supplier Integration:</b> Optionally provides automatic value computation via a {@link Function4}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map for caching permissions</jc>
 * 	Concurrent4KeyHashMap&lt;String,String,String,String,Boolean&gt; <jv>permissions</jv> =
 * 		<jk>new</jk> Concurrent4KeyHashMap&lt;&gt;();
 *
 * 	<jc>// Store permissions: tenant, user, resource, action</jc>
 * 	<jv>permissions</jv>.put(<js>"tenant1"</js>, <js>"user123"</js>, <js>"document"</js>, <js>"read"</js>, <jk>true</jk>);
 * 	<jv>permissions</jv>.put(<js>"tenant1"</js>, <js>"user123"</js>, <js>"document"</js>, <js>"write"</js>, <jk>false</jk>);
 *
 * 	<jc>// Check permission</jc>
 * 	Boolean <jv>canRead</jv> = <jv>permissions</jv>.get(<js>"tenant1"</js>, <js>"user123"</js>, <js>"document"</js>, <js>"read"</js>);
 * </p>
 *
 * <h5 class='section'>With Supplier:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map that automatically checks permissions</jc>
 * 	Concurrent4KeyHashMap&lt;String,String,String,String,Boolean&gt; <jv>permCache</jv> =
 * 		<jk>new</jk> Concurrent4KeyHashMap&lt;&gt;(<jk>false</jk>, (tenant, user, resource, action) -&gt;
 * 			permissionService.check(tenant, user, resource, action)
 * 		);
 *
 * 	<jc>// Automatically checks and caches if not present</jc>
 * 	Boolean <jv>allowed</jv> = <jv>permCache</jv>.get(<js>"tenant1"</js>, <js>"user123"</js>, <js>"document"</js>, <js>"write"</js>);
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
 * 	Concurrent4KeyHashMap&lt;String,String,String,String,Boolean&gt; <jv>permCache</jv> =
 * 		<jk>new</jk> Concurrent4KeyHashMap&lt;&gt;(<jk>true</jk>, (tenant, user, resource, action) -&gt;
 * 			permissionService.check(tenant, user, resource, action)
 * 		);
 *
 * 	<jc>// Always calls permissionService.check(), never caches</jc>
 * 	Boolean <jv>allowed</jv> = <jv>permCache</jv>.get(<js>"tenant1"</js>, <js>"user123"</js>, <js>"document"</js>, <js>"write"</js>);
 * </p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Caching permissions indexed by tenant, user, resource, and action
 * 	<li>Storing metrics indexed by environment, service, instance, and metric name
 * 	<li>Mapping configuration by tenant, environment, service, and config key
 * 	<li>Tracking analytics by date, source, campaign, and metric
 * 	<li>Caching query results indexed by database, schema, table, and query type
 * </ul>
 *
 * <h5 class='section'>Key Hashing:</h5>
 * <p>
 * The composite key is hashed using the formula:
 * <p class='bjava'>
 * 	hash = 31 * (31 * (31 * k1.hashCode() + k2.hashCode()) + k3.hashCode()) + k4.hashCode()
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
 * 	<li class='jc'>{@link Concurrent3KeyHashMap}
 * 	<li class='jc'>{@link Concurrent5KeyHashMap}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key component type.
 * @param <K2> The second key component type.
 * @param <K3> The third key component type.
 * @param <K4> The fourth key component type.
 * @param <V> The value type.
 * @serial exclude
 */
public class Concurrent4KeyHashMap<K1,K2,K3,K4,V> extends ConcurrentHashMap<Tuple4<K1,K2,K3,K4>,V> {

	private static final long serialVersionUID = 1L;
	private final boolean disabled;
	private final Function4<K1,K2,K3,K4,V> supplier;

	/**
	 * Constructs a new Concurrent4KeyHashMap with caching enabled and no supplier.
	 */
	public Concurrent4KeyHashMap() {
		this(false, null);
	}

	/**
	 * Constructs a new Concurrent4KeyHashMap with optional caching and supplier.
	 *
	 * @param disabled If <jk>true</jk>, the map doesn't cache values.
	 * @param supplier Optional supplier function for automatic value computation.
	 */
	public Concurrent4KeyHashMap(boolean disabled, Function4<K1,K2,K3,K4,V> supplier) {
		this.disabled = disabled;
		this.supplier = supplier;
	}

	/**
	 * Retrieves the value associated with the specified four-part key.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param key4 Fourth key component. Can be <jk>null</jk>.
	 * @return The value associated with the key, or <jk>null</jk> if not found.
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4) {
		if (disabled)
			return (supplier == null ? null : supplier.apply(key1, key2, key3, key4));
		var key = Tuple4.of(key1, key2, key3, key4);
		var v = super.get(key);
		if (v == null && supplier != null) {
			v = supplier.apply(key1, key2, key3, key4);
			super.put(key, v);
		}
		return v;
	}

	/**
	 * Associates the specified value with the specified four-part key in this map.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param key4 Fourth key component. Can be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 */
	public V put(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
		if (disabled)
			return null;
		var key = Tuple4.of(key1, key2, key3, key4);
		return super.put(key, value);
	}
}
