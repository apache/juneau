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

import static org.apache.juneau.common.collections.CacheMode.*;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.SystemUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.common.function.*;

/**
 * Simple in-memory cache for storing and retrieving objects by five-part keys.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * This class extends {@link ConcurrentHashMap5Key} to provide a thread-safe caching layer with automatic
 * value computation, cache eviction, and statistics tracking for five-part composite keys. It's designed for
 * caching expensive-to-compute or frequently-accessed objects indexed by five keys.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Thread-safe concurrent access without external synchronization
 * 	<li>Five-part composite key support
 * 	<li>Automatic cache eviction when maximum size is reached
 * 	<li>Lazy computation via {@link Function5} supplier pattern
 * 	<li>Default supplier support for simplified access
 * 	<li>Built-in hit/miss statistics tracking
 * 	<li>Optional logging of cache statistics on JVM shutdown
 * 	<li>Can be disabled entirely via builder or system property
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Cache}
 * 	<li class='jc'>{@link Cache2}
 * 	<li class='jc'>{@link Cache3}
 * 	<li class='jc'>{@link Cache4}
 * 	<li class='jc'>{@link ConcurrentHashMap5Key}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key type.
 * @param <K2> The second key type.
 * @param <K3> The third key type.
 * @param <K4> The fourth key type.
 * @param <K5> The fifth key type.
 * @param <V> The value type.
 */
public class Cache5<K1,K2,K3,K4,K5,V> {

	// Internal map with Tuple5 keys for content-based equality (especially for arrays)
	private final java.util.Map<Tuple5<K1,K2,K3,K4,K5>,V> map;

	/**
	 * Builder for creating configured {@link Cache5} instances.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <K4> The fourth key type.
	 * @param <K5> The fifth key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K1,K2,K3,K4,K5,V> {
		CacheMode cacheMode;
		int maxSize;
		String id;
		boolean logOnExit;
		Function5<K1,K2,K3,K4,K5,V> supplier;

		Builder() {
			cacheMode = CacheMode.parse(env("juneau.cache.mode", "FULL"));
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
			id = "Cache5";
		}

		/**
		 * Builds a new {@link Cache5} instance with the configured settings.
		 *
		 * @return A new immutable {@link Cache5} instance.
		 */
		public Cache5<K1,K2,K3,K4,K5,V> build() {
			return new Cache5<>(this);
		}

		/**
		 * Sets the caching mode for this cache.
		 *
		 * <p>
		 * Available modes:
		 * <ul>
		 * 	<li>{@link CacheMode#NONE NONE} - No caching (always invoke supplier)
		 * 	<li>{@link CacheMode#WEAK WEAK} - Weak caching (uses {@link WeakHashMap})
		 * 	<li>{@link CacheMode#FULL FULL} - Full caching (uses {@link ConcurrentHashMap}, default)
		 * </ul>
		 *
		 * @param value The caching mode.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,K4,K5,V> cacheMode(CacheMode value) {
			cacheMode = value;
			return this;
		}

		/**
		 * Enables logging of cache statistics when the JVM exits.
		 *
		 * @param id The identifier to use in the log message.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,K4,K5,V> logOnExit(String id) {
			this.id = id;
			this.logOnExit = true;
			return this;
		}

		/**
		 * Conditionally enables logging of cache statistics when the JVM exits.
		 *
		 * <p>
		 * When enabled, the cache will register a shutdown hook that logs the cache name,
		 * total cache hits, and total cache misses (size of cache) to help analyze cache effectiveness.
		 *
		 * @param value Whether to enable logging on exit.
		 * @param id The identifier to use in the log message.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,K4,K5,V> logOnExit(boolean value, String id) {
			this.id = id;
			this.logOnExit = value;
			return this;
		}

		/**
		 * Specifies the maximum number of entries allowed in this cache.
		 *
		 * @param value The maximum number of cache entries. Must be positive.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,K4,K5,V> maxSize(int value) {
			maxSize = value;
			return this;
		}

		/**
		 * Specifies the default supplier function for computing values when keys are not found.
		 *
		 * @param value The default supplier function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,K4,K5,V> supplier(Function5<K1,K2,K3,K4,K5,V> value) {
			supplier = value;
			return this;
		}
	}

	/**
	 * Creates a new {@link Builder} for constructing a cache with explicit type parameters.
	 *
	 * <p>
	 * This variant allows you to specify the cache's generic types explicitly without passing
	 * the class objects, which is useful when working with complex parameterized types.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <K4> The fourth key type.
	 * @param <K5> The fifth key type.
	 * @param <V> The value type.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,K3,K4,K5,V> Builder<K1,K2,K3,K4,K5,V> create() {
		return new Builder<>();
	}

	/**
	 * Creates a new {@link Builder} for constructing a cache.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <K4> The fourth key type.
	 * @param <K5> The fifth key type.
	 * @param <V> The value type.
	 * @param key1 The first key type class (used for type safety).
	 * @param key2 The second key type class (used for type safety).
	 * @param key3 The third key type class (used for type safety).
	 * @param key4 The fourth key type class (used for type safety).
	 * @param key5 The fifth key type class (used for type safety).
	 * @param type The value type class.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,K3,K4,K5,V> Builder<K1,K2,K3,K4,K5,V> of(Class<K1> key1, Class<K2> key2, Class<K3> key3, Class<K4> key4, Class<K5> key5, Class<V> type) {
		return new Builder<>();
	}

	private final int maxSize;
	private final CacheMode cacheMode;
	private final Function5<K1,K2,K3,K4,K5,V> supplier;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache5(Builder<K1,K2,K3,K4,K5,V> builder) {
		this.maxSize = builder.maxSize;
		this.cacheMode = builder.cacheMode;
		this.supplier = builder.supplier;
		if (builder.cacheMode == WEAK) {
			this.map = Collections.synchronizedMap(new WeakHashMap<>());
		} else {
			this.map = new ConcurrentHashMap<>();
		}
		if (builder.logOnExit) {
			shutdownMessage(() -> builder.id + ":  hits=" + cacheHits.get() + ", misses: " + size());
		}
	}

	/**
	 * Retrieves a cached value by five-part key using the default supplier.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param key4 Fourth key component. Can be <jk>null</jk>.
	 * @param key5 Fifth key component. Can be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws NullPointerException if no default supplier was configured.
	 * 
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
		return get(key1, key2, key3, key4, key5, () -> supplier.apply(key1, key2, key3, key4, key5));
	}

	/**
	 * Retrieves a cached value by five-part key, computing it if necessary using the provided supplier.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param key4 Fourth key component. Can be <jk>null</jk>.
	 * @param key5 Fifth key component. Can be <jk>null</jk>.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * 
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, java.util.function.Supplier<V> supplier) {
		assertArgNotNull("supplier", supplier);
		if (cacheMode == NONE)
			return supplier.get();
		var wrapped = Tuple5.of(key1, key2, key3, key4, key5);
		V v = map.get(wrapped);
		if (v == null) {
			if (size() > maxSize)
				clear();
			v = supplier.get();
			if (v == null)
				map.remove(wrapped);
			else
				map.putIfAbsent(wrapped, v);
		} else {
			cacheHits.incrementAndGet();
		}
		return v;
	}

	/**
	 * Associates the specified value with the specified five-part key.
	 *
	 * @param key1 The first key.
	 * @param key2 The second key.
	 * @param key3 The third key.
	 * @param key4 The fourth key.
	 * @param key5 The fifth key.
	 * @param value The value to associate with the five-part key.
	 * @return The previous value associated with the five-part key, or <jk>null</jk> if there was no mapping.
	 * 
	 */
	public V put(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, V value) {
		if (value == null)
			return map.remove(Tuple5.of(key1, key2, key3, key4, key5));
		return map.put(Tuple5.of(key1, key2, key3, key4, key5), value);
	}

	/**
	 * Removes the entry for the specified five-part key from the cache.
	 *
	 * @param key1 The first key.
	 * @param key2 The second key.
	 * @param key3 The third key.
	 * @param key4 The fourth key.
	 * @param key5 The fifth key.
	 * @return The previous value associated with the five-part key, or <jk>null</jk> if there was no mapping.
	 * 
	 */
	public V remove(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
		return map.remove(Tuple5.of(key1, key2, key3, key4, key5));
	}

	/**
	 * Returns <jk>true</jk> if the cache contains a mapping for the specified five-part key.
	 *
	 * @param key1 The first key.
	 * @param key2 The second key.
	 * @param key3 The third key.
	 * @param key4 The fourth key.
	 * @param key5 The fifth key.
	 * @return <jk>true</jk> if the cache contains the five-part key.
	 */
	public boolean containsKey(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
		return map.containsKey(Tuple5.of(key1, key2, key3, key4, key5));
	}

	/**
	 * Returns <jk>true</jk> if the cache contains one or more entries with the specified value.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the cache contains the value.
	 */
	public boolean containsValue(V value) {
		return map.containsValue(value);
	}

	/**
	 * Returns the number of entries in the cache.
	 *
	 * @return The number of cached entries.
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Returns <jk>true</jk> if the cache contains no entries.
	 *
	 * @return <jk>true</jk> if the cache is empty.
	 */
	public boolean isEmpty() { return map.isEmpty(); }

	/**
	 * Removes all entries from the cache.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Returns the total number of cache hits since this cache was created.
	 *
	 * @return The total number of cache hits since creation.
	 */
	public int getCacheHits() { return cacheHits.get(); }
}
