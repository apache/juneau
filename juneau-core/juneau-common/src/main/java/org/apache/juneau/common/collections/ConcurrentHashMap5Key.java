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
import org.apache.juneau.common.utils.*;

/**
 * A thread-safe concurrent hash map that uses composite five-part keys for lookups.
 *
 * <p>
 * This class extends {@link ConcurrentHashMap} to provide efficient storage and retrieval of values
 * indexed by five separate key components. It's useful when you need to look up values based on a
 * combination of five keys without creating intermediate wrapper objects.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Five-Part Keys:</b> Values are indexed by five keys (K1, K2, K3, K4, K5) instead of a single key
 * 	<li><b>Thread-Safe:</b> Inherits all thread-safety guarantees from {@link ConcurrentHashMap}
 * 	<li><b>Null Keys Supported:</b> All key parts can be <jk>null</jk>
 * 	<li><b>Optional Caching:</b> Can be disabled to always invoke a supplier function instead of caching
 * 	<li><b>Supplier Integration:</b> Optionally provides automatic value computation via a {@link Function5}
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map for caching complex queries</jc>
 * 	ConcurrentHashMap5Key&lt;String,String,String,String,String,ResultSet&gt; <jv>queryCache</jv> =
 * 		<jk>new</jk> ConcurrentHashMap5Key&lt;&gt;();
 *
 * 	<jc>// Store results: environment, database, schema, table, operation</jc>
 * 	<jv>queryCache</jv>.put(<js>"prod"</js>, <js>"db1"</js>, <js>"public"</js>, <js>"users"</js>, <js>"select"</js>, <jv>results</jv>);
 *
 * 	<jc>// Retrieve results</jc>
 * 	ResultSet <jv>cached</jv> = <jv>queryCache</jv>.get(<js>"prod"</js>, <js>"db1"</js>, <js>"public"</js>, <js>"users"</js>, <js>"select"</js>);
 * </p>
 *
 * <h5 class='section'>With Supplier:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a map that automatically executes queries</jc>
 * 	ConcurrentHashMap5Key&lt;String,String,String,String,String,ResultSet&gt; <jv>cache</jv> =
 * 		<jk>new</jk> ConcurrentHashMap5Key&lt;&gt;(<jk>false</jk>, (env, db, schema, table, op) -&gt;
 * 			queryService.execute(env, db, schema, table, op)
 * 		);
 *
 * 	<jc>// Automatically executes and caches if not present</jc>
 * 	ResultSet <jv>results</jv> = <jv>cache</jv>.get(<js>"prod"</js>, <js>"db1"</js>, <js>"public"</js>, <js>"users"</js>, <js>"select"</js>);
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
 * 	ConcurrentHashMap5Key&lt;String,String,String,String,String,ResultSet&gt; <jv>cache</jv> =
 * 		<jk>new</jk> ConcurrentHashMap5Key&lt;&gt;(<jk>true</jk>, (env, db, schema, table, op) -&gt;
 * 			queryService.execute(env, db, schema, table, op)
 * 		);
 *
 * 	<jc>// Always calls queryService.execute(), never caches</jc>
 * 	ResultSet <jv>results</jv> = <jv>cache</jv>.get(<js>"prod"</js>, <js>"db1"</js>, <js>"public"</js>, <js>"users"</js>, <js>"select"</js>);
 * </p>
 *
 * <h5 class='section'>Common Use Cases:</h5>
 * <ul class='spaced-list'>
 * 	<li>Caching database query results with complex composite keys
 * 	<li>Storing multi-dimensional analytics data
 * 	<li>Mapping highly granular configuration settings
 * 	<li>Tracking detailed audit logs or activity records
 * 	<li>Caching results from complex multi-parameter computations
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ConcurrentTwoKeyHashMap}
 * 	<li class='jc'>{@link ConcurrentThreeKeyHashMap}
 * 	<li class='jc'>{@link ConcurrentFourKeyHashMap}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key component type.
 * @param <K2> The second key component type.
 * @param <K3> The third key component type.
 * @param <K4> The fourth key component type.
 * @param <K5> The fifth key component type.
 * @param <V> The value type.
 * @serial exclude
 */
public class ConcurrentHashMap5Key<K1,K2,K3,K4,K5,V> extends ConcurrentHashMap<Tuple5<K1,K2,K3,K4,K5>,V> {

	private static final long serialVersionUID = 1L;

	/**
	 * Retrieves the value associated with the specified five-part key.
	 *
	 * @param key1 First key component. Must not be <jk>null</jk>.
	 * @param key2 Second key component. Must not be <jk>null</jk>.
	 * @param key3 Third key component. Must not be <jk>null</jk>.
	 * @param key4 Fourth key component. Must not be <jk>null</jk>.
	 * @param key5 Fifth key component. Must not be <jk>null</jk>.
	 * @return The value associated with the key, or <jk>null</jk> if not found.
	 * @throws IllegalArgumentException if any key is <jk>null</jk>.
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
		AssertionUtils.assertArgsNotNull("key1", key1, "key2", key2, "key3", key3, "key4", key4, "key5", key5);
		return super.get(Tuple5.of(key1, key2, key3, key4, key5));
	}

	/**
	 * Associates the specified value with the specified five-part key in this map.
	 *
	 * @param key1 First key component. Must not be <jk>null</jk>.
	 * @param key2 Second key component. Must not be <jk>null</jk>.
	 * @param key3 Third key component. Must not be <jk>null</jk>.
	 * @param key4 Fourth key component. Must not be <jk>null</jk>.
	 * @param key5 Fifth key component. Must not be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 * @throws IllegalArgumentException if any key is <jk>null</jk>.
	 */
	public V put(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, V value) {
		AssertionUtils.assertArgsNotNull("key1", key1, "key2", key2, "key3", key3, "key4", key4, "key5", key5);
		return super.put(Tuple5.of(key1, key2, key3, key4, key5), value);
	}
}
