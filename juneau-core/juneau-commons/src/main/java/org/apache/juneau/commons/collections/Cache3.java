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

import static org.apache.juneau.commons.collections.CacheMode.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.SystemUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.function.*;

/**
 * Simple in-memory cache for storing and retrieving objects by three-part keys.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * This class extends {@link ConcurrentHashMap3Key} to provide a thread-safe caching layer with automatic
 * value computation, cache eviction, and statistics tracking for three-part composite keys. It's designed for
 * caching expensive-to-compute or frequently-accessed objects indexed by three keys.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Thread-safe concurrent access without external synchronization
 * 	<li>Three-part composite key support
 * 	<li>Automatic cache eviction when maximum size is reached
 * 	<li>Lazy computation via {@link Function3} supplier pattern
 * 	<li>Default supplier support for simplified access
 * 	<li>Built-in hit/miss statistics tracking
 * 	<li>Optional logging of cache statistics on JVM shutdown
 * 	<li>Can be disabled entirely via builder or system property
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a cache with default supplier</jc>
 * 	Cache3&lt;String,String,String,Translation&gt; <jv>translationCache</jv> = Cache3
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>, Translation.<jk>class</jk>)
 * 		.maxSize(500)
 * 		.supplier((lang, country, key) -&gt; loadTranslation(lang, country, key))
 * 		.build();
 *
 * 	<jc>// Retrieve using default supplier</jc>
 * 	Translation <jv>t1</jv> = <jv>translationCache</jv>.get(<js>"en"</js>, <js>"US"</js>, <js>"greeting"</js>);
 *
 * 	<jc>// Or override the supplier</jc>
 * 	Translation <jv>t2</jv> = <jv>translationCache</jv>.get(<js>"fr"</js>, <js>"FR"</js>, <js>"greeting"</js>, () -&gt; customTranslation);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Cache}
 * 	<li class='jc'>{@link Cache2}
 * 	<li class='jc'>{@link ConcurrentHashMap3Key}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K1> The first key type.
 * @param <K2> The second key type.
 * @param <K3> The third key type.
 * @param <V> The value type.
 */
public class Cache3<K1,K2,K3,V> {

	/**
	 * Builder for creating configured {@link Cache3} instances.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K1,K2,K3,V> {
		CacheMode cacheMode;
		int maxSize;
		String id;
		boolean logOnExit;
		boolean threadLocal;
		Function3<K1,K2,K3,V> supplier;

		Builder() {
			cacheMode = CacheMode.parse(env("juneau.cache.mode", "FULL"));
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
			id = "Cache3";
		}

		/**
		 * Builds a new {@link Cache3} instance with the configured settings.
		 *
		 * @return A new immutable {@link Cache3} instance.
		 */
		public Cache3<K1,K2,K3,V> build() {
			return new Cache3<>(this);
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
		public Builder<K1,K2,K3,V> cacheMode(CacheMode value) {
			cacheMode = value;
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
		public Builder<K1,K2,K3,V> logOnExit(boolean value, String id) {
			this.id = id;
			this.logOnExit = value;
			return this;
		}

		/**
		 * Enables logging of cache statistics when the JVM exits.
		 *
		 * @param id The identifier to use in the log message.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> logOnExit(String id) {
			this.id = id;
			this.logOnExit = true;
			return this;
		}

		/**
		 * Specifies the maximum number of entries allowed in this cache.
		 *
		 * @param value The maximum number of cache entries. Must be positive.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> maxSize(int value) {
			maxSize = value;
			return this;
		}

		/**
		 * Specifies the default supplier function for computing values when keys are not found.
		 *
		 * @param value The default supplier function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> supplier(Function3<K1,K2,K3,V> value) {
			supplier = value;
			return this;
		}

		/**
		 * Enables thread-local caching.
		 *
		 * <p>
		 * When enabled, each thread gets its own separate cache instance. This is useful for
		 * thread-unsafe objects that need to be cached per thread.
		 *
		 * <p>
		 * This is a shortcut for wrapping a cache in a {@link ThreadLocal}, but provides a cleaner API.
		 *
		 * @return This object for method chaining.
		 * @see Cache.Builder#threadLocal()
		 */
		public Builder<K1,K2,K3,V> threadLocal() {
			threadLocal = true;
			return this;
		}

		/**
		 * Sets the caching mode to {@link CacheMode#WEAK WEAK}.
		 *
		 * <p>
		 * This is a shortcut for calling <c>cacheMode(CacheMode.WEAK)</c>.
		 *
		 * <p>
		 * Weak caching uses {@link WeakHashMap} for storage, allowing cache entries to be
		 * garbage collected when keys are no longer strongly referenced elsewhere.
		 *
		 * @return This object for method chaining.
		 * @see #cacheMode(CacheMode)
		 */
		public Builder<K1,K2,K3,V> weak() {
			return cacheMode(WEAK);
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
	 * @param <V> The value type.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,K3,V> Builder<K1,K2,K3,V> create() {
		return new Builder<>();
	}

	/**
	 * Creates a new {@link Builder} for constructing a cache.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <V> The value type.
	 * @param key1 The first key type class (used for type safety).
	 * @param key2 The second key type class (used for type safety).
	 * @param key3 The third key type class (used for type safety).
	 * @param type The value type class.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,K3,V> Builder<K1,K2,K3,V> of(Class<K1> key1, Class<K2> key2, Class<K3> key3, Class<V> type) {
		return new Builder<>();
	}

	// Internal map with Tuple3 keys for content-based equality (especially for arrays)
	// If threadLocal is true, this is null and threadLocalMap is used instead
	private final java.util.Map<Tuple3<K1,K2,K3>,V> map;

	private final ThreadLocal<Map<Tuple3<K1,K2,K3>,V>> threadLocalMap;

	private final boolean isThreadLocal;

	private final int maxSize;
	private final CacheMode cacheMode;
	private final Function3<K1,K2,K3,V> supplier;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache3(Builder<K1,K2,K3,V> builder) {
		this.maxSize = builder.maxSize;
		this.cacheMode = builder.cacheMode;
		this.supplier = builder.supplier;
		this.isThreadLocal = builder.threadLocal;

		if (isThreadLocal) {
			// Thread-local mode: each thread gets its own map
			if (builder.cacheMode == WEAK) {
				this.threadLocalMap = ThreadLocal.withInitial(() -> Collections.synchronizedMap(new WeakHashMap<>()));
			} else {
				this.threadLocalMap = ThreadLocal.withInitial(() -> new ConcurrentHashMap<>());
			}
			this.map = null;
		} else {
			// Normal mode: shared map across all threads
			if (builder.cacheMode == WEAK) {
				this.map = Collections.synchronizedMap(new WeakHashMap<>());
			} else {
				this.map = new ConcurrentHashMap<>();
			}
			this.threadLocalMap = null;
		}
		if (builder.logOnExit) {
			shutdownMessage(() -> builder.id + ":  hits=" + cacheHits.get() + ", misses: " + size());
		}
	}

	/**
	 * Removes all entries from the cache.
	 */
	public void clear() {
		getMap().clear();
	}

	/**
	 * Returns <jk>true</jk> if the cache contains a mapping for the specified key triplet.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @param key3 The third key. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the cache contains the key triplet.
	 */
	public boolean containsKey(K1 key1, K2 key2, K3 key3) {
		return getMap().containsKey(Tuple3.of(key1, key2, key3));
	}

	/**
	 * Returns <jk>true</jk> if the cache contains one or more entries with the specified value.
	 *
	 * @param value The value to check.
	 * @return <jk>true</jk> if the cache contains the value.
	 */
	public boolean containsValue(V value) {
		// ConcurrentHashMap doesn't allow null values, so null can never be in the cache
		if (value == null)
			return false;
		return getMap().containsValue(value);
	}

	/**
	 * Retrieves a cached value by key triplet using the default supplier.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws NullPointerException if no default supplier was configured.
	 */
	public V get(K1 key1, K2 key2, K3 key3) {
		return get(key1, key2, key3, () -> supplier.apply(key1, key2, key3));
	}

	/**
	 * Retrieves a cached value by key triplet, computing it if necessary using the provided supplier.
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param key3 Third key component. Can be <jk>null</jk>.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 */
	public V get(K1 key1, K2 key2, K3 key3, java.util.function.Supplier<V> supplier) {
		assertArgNotNull("supplier", supplier);
		if (cacheMode == NONE)
			return supplier.get();
		Map<Tuple3<K1,K2,K3>,V> m = getMap();
		var wrapped = Tuple3.of(key1, key2, key3);
		V v = m.get(wrapped);
		if (v == null) {
			if (size() > maxSize)
				clear();
			v = supplier.get();
			if (v == null)
				m.remove(wrapped);
			else
				m.putIfAbsent(wrapped, v);
		} else {
			cacheHits.incrementAndGet();
		}
		return v;
	}

	/**
	 * Returns the total number of cache hits since this cache was created.
	 *
	 * @return The total number of cache hits since creation.
	 */
	public int getCacheHits() { return cacheHits.get(); }

	/**
	 * Returns <jk>true</jk> if the cache contains no entries.
	 *
	 * @return <jk>true</jk> if the cache is empty.
	 */
	public boolean isEmpty() { return getMap().isEmpty(); }

	/**
	 * Associates the specified value with the specified key triplet.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @param key3 The third key. Can be <jk>null</jk>.
	 * @param value The value to associate with the key triplet.
	 * @return The previous value associated with the key triplet, or <jk>null</jk> if there was no mapping.
	 */
	public V put(K1 key1, K2 key2, K3 key3, V value) {
		java.util.Map<Tuple3<K1,K2,K3>,V> m = getMap();
		if (value == null)
			return m.remove(Tuple3.of(key1, key2, key3));
		return m.put(Tuple3.of(key1, key2, key3), value);
	}

	/**
	 * Removes the entry for the specified key triplet from the cache.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @param key3 The third key. Can be <jk>null</jk>.
	 * @return The previous value associated with the key triplet, or <jk>null</jk> if there was no mapping.
	 */
	public V remove(K1 key1, K2 key2, K3 key3) {
		return getMap().remove(Tuple3.of(key1, key2, key3));
	}

	/**
	 * Returns the number of entries in the cache.
	 *
	 * @return The number of cached entries.
	 */
	public int size() {
		return getMap().size();
	}

	/**
	 * Gets the map for the current thread.
	 *
	 * @return The map for the current thread.
	 */
	private Map<Tuple3<K1,K2,K3>,V> getMap() { return isThreadLocal ? threadLocalMap.get() : map; }
}
