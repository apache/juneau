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

import static org.apache.juneau.common.utils.Utils.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;

/**
 * Simple in-memory cache for storing and retrieving objects by key.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * This class provides a thread-safe, concurrent cache implementation backed by a {@link ConcurrentHashMap}.
 * It's designed for caching expensive-to-compute or frequently-accessed objects to improve performance.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Thread-safe concurrent access without external synchronization
 * 	<li>Automatic cache eviction when maximum size is reached
 * 	<li>Lazy computation via {@link Supplier} pattern
 * 	<li>Built-in hit/miss statistics tracking
 * 	<li>Optional logging of cache statistics on JVM shutdown
 * 	<li>Can be disabled entirely via builder or system property
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a cache for compiled patterns</jc>
 * 	Cache&lt;String,Pattern&gt; <jv>patternCache</jv> = Cache
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
 * 		.maxSize(100)
 * 		.build();
 *
 * 	<jc>// Retrieve from cache, computing if necessary</jc>
 * 	Pattern <jv>pattern</jv> = <jv>patternCache</jv>.get(<js>"[a-z]+"</js>, () -&gt; Pattern.compile(<js>"[a-z]+"</js>));
 * </p>
 *
 * <h5 class='section'>Cache Behavior:</h5>
 * <ul class='spaced-list'>
 * 	<li>When a key is requested:
 * 		<ul>
 * 			<li>If the key exists in the cache, the cached value is returned (cache hit)
 * 			<li>If the key doesn't exist, the supplier is invoked to compute the value
 * 			<li>The computed value is stored in the cache and returned (cache miss)
 * 		</ul>
 * 	<li>When the cache exceeds {@link Builder#maxSize(int)}, the entire cache is cleared
 * 	<li>If the cache is disabled, the supplier is always invoked without caching
 * 	<li>Null keys always bypass the cache and invoke the supplier
 * </ul>
 *
 * <h5 class='section'>Environment Variables:</h5>
 * <p>
 * The following system properties can be used to configure default cache behavior:
 * <ul class='spaced-list'>
 * 	<li><c>juneau.cache.disable</c> - Disables all caching (default: <jk>false</jk>)
 * 	<li><c>juneau.cache.maxSize</c> - Maximum cache size before eviction (default: 1000)
 * 	<li><c>juneau.cache.logOnExit</c> - Log cache statistics on shutdown (default: <jk>false</jk>)
 * </ul>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe and can be safely used from multiple threads without external synchronization.
 * However, note that when the cache is cleared due to exceeding max size, there's a small window where
 * multiple threads might compute the same value. This is acceptable for most use cases as it only affects
 * performance, not correctness.
 *
 * <h5 class='section'>Performance Considerations:</h5>
 * <ul class='spaced-list'>
 * 	<li>Cache operations are O(1) average time complexity
 * 	<li>The {@link #get(Object, Supplier)} method uses {@link ConcurrentHashMap#putIfAbsent(Object, Object)}
 * 		to minimize redundant computation in concurrent scenarios
 * 	<li>When max size is exceeded, the entire cache is cleared in a single operation
 * 	<li>Statistics tracking uses {@link AtomicInteger} for thread-safe counting without locking
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple cache with defaults</jc>
 * 	Cache&lt;String,Integer&gt; <jv>cache</jv> = Cache.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>).build();
 *
 * 	<jc>// Cache with custom configuration</jc>
 * 	Cache&lt;Class&lt;?&gt;,ClassMeta&gt; <jv>classMetaCache</jv> = Cache
 * 		.<jsm>of</jsm>(Class.<jk>class</jk>, ClassMeta.<jk>class</jk>)
 * 		.maxSize(500)
 * 		.logOnExit()
 * 		.build();
 *
 * 	<jc>// Disabled cache for testing</jc>
 * 	Cache&lt;String,Object&gt; <jv>disabledCache</jv> = Cache
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Object.<jk>class</jk>)
 * 		.disabled()
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class Cache<K,V> {

	/**
	 * Builder for creating configured {@link Cache} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache&lt;String,Pattern&gt; <jv>cache</jv> = Cache
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
	 * 		.maxSize(200)
	 * 		.logOnExit()
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link Cache#of(Class, Class)}
	 * </ul>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K,V> {
		boolean disabled, logOnExit;
		int maxSize;
		Class<V> type;

		Builder(Class<V> type) {
			this.type = type;
			disabled = env("juneau.cache.disable", false);
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
		}

		/**
		 * Builds a new {@link Cache} instance with the configured settings.
		 *
		 * @return A new immutable {@link Cache} instance.
		 */
		public Cache<K,V> build() {
			return new Cache<>(this);
		}

		/**
		 * Disables caching entirely.
		 *
		 * <p>
		 * When disabled, the {@link Cache#get(Object, Supplier)} method will always invoke the supplier
		 * and never store or retrieve values from the cache.
		 *
		 * <p>
		 * This is useful for:
		 * <ul>
		 * 	<li>Testing scenarios where you want fresh computation each time
		 * 	<li>Development environments where caching might hide issues
		 * 	<li>Temporarily disabling caching without code changes
		 * </ul>
		 *
		 * @return This object for method chaining.
		 */
		public Builder<K,V> disabled() {
			disabled = true;
			return this;
		}

		/**
		 * Enables logging of cache statistics when the JVM exits.
		 *
		 * <p>
		 * When enabled, the cache will register a shutdown hook that logs the cache name,
		 * total cache hits, and total cache misses (size of cache) to help analyze cache effectiveness.
		 *
		 * <p>
		 * Example output:
		 * <p class='bconsole'>
		 * 	Pattern cache:  hits=1523, misses: 47
		 * </p>
		 *
		 * <p>
		 * This is useful for:
		 * <ul>
		 * 	<li>Performance tuning and identifying caching opportunities
		 * 	<li>Determining optimal max size values
		 * 	<li>Monitoring cache efficiency in production
		 * </ul>
		 *
		 * @return This object for method chaining.
		 */
		public Builder<K,V> logOnExit() {
			logOnExit = true;
			return this;
		}

		/**
		 * Specifies the maximum number of entries allowed in this cache.
		 *
		 * <p>
		 * When the cache size exceeds this value, the <em>entire</em> cache is cleared to make room for new entries.
		 * This is a simple eviction strategy that avoids the overhead of LRU/LFU tracking.
		 *
		 * <p>
		 * Default value: 1000 (or value of system property <c>juneau.cache.maxSize</c>)
		 *
		 * <h5 class='section'>Notes:</h5>
		 * <ul>
		 * 	<li>Setting this too low may cause excessive cache clearing and reduce effectiveness
		 * 	<li>Setting this too high may consume excessive memory
		 * 	<li>For unbounded caching, use {@link Integer#MAX_VALUE} (not recommended for production)
		 * </ul>
		 *
		 * @param value The maximum number of cache entries. Must be positive.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> maxSize(int value) {
			maxSize = value;
			return this;
		}
	}

	/**
	 * Creates a new {@link Builder} for constructing a cache.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache&lt;String,Pattern&gt; <jv>cache</jv> = Cache
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
	 * 		.maxSize(100)
	 * 		.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param key The key type class (used for type safety).
	 * @param type The value type class (used for logging and type safety).
	 * @return A new builder for configuring the cache.
	 */
	public static <K,V> Builder<K,V> of(Class<K> key, Class<V> type) {
		return new Builder<>(type);
	}

	private final int maxSize;
	private final ConcurrentHashMap<K,V> cache;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache(Builder<K,V> builder) {
		cache = builder.disabled ? null : new ConcurrentHashMap<>();
		maxSize = builder.maxSize;
		if (builder.logOnExit) {
			SystemUtils.shutdownMessage(() -> builder.type.getSimpleName() + " cache:  hits=" + cacheHits.get() + ", misses: " + cache.size());
		}
	}

	/**
	 * Retrieves a cached value by key, computing it if necessary.
	 *
	 * <p>
	 * This method implements the cache-aside pattern:
	 * <ol>
	 * 	<li>If the key exists in the cache, return the cached value (cache hit)
	 * 	<li>If the key doesn't exist, invoke the supplier to compute the value
	 * 	<li>Store the computed value in the cache using {@link ConcurrentHashMap#putIfAbsent(Object, Object)}
	 * 	<li>Return the value
	 * </ol>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>If the cache is disabled, always invokes the supplier without caching
	 * 	<li>If the key is <jk>null</jk>, bypasses the cache and invokes the supplier
	 * 	<li>If the cache exceeds {@link Builder#maxSize(int)}, clears all entries before storing the new value
	 * 	<li>Thread-safe: Multiple threads can safely call this method concurrently
	 * 	<li>The supplier may be called multiple times for the same key in concurrent scenarios
	 * 		(due to {@link ConcurrentHashMap#putIfAbsent(Object, Object)} semantics)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache&lt;String,Pattern&gt; <jv>cache</jv> = Cache.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>).build();
	 *
	 * 	<jc>// First call: compiles pattern and caches it</jc>
	 * 	Pattern <jv>p1</jv> = <jv>cache</jv>.get(<js>"[0-9]+"</js>, () -&gt; Pattern.compile(<js>"[0-9]+"</js>));
	 *
	 * 	<jc>// Second call: returns cached pattern instantly</jc>
	 * 	Pattern <jv>p2</jv> = <jv>cache</jv>.get(<js>"[0-9]+"</js>, () -&gt; Pattern.compile(<js>"[0-9]+"</js>));
	 *
	 * 	<jsm>assert</jsm> <jv>p1</jv> == <jv>p2</jv>;  <jc>// Same instance</jc>
	 * </p>
	 *
	 * @param key The cache key. If <jk>null</jk>, the supplier is always invoked without caching.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 */
	public V get(K key, Supplier<V> supplier) {
		if (cache == null || key == null)
			return supplier.get();
		V v = cache.get(key);
		if (v == null) {
			if (cache.size() > maxSize)
				cache.clear();
			v = supplier.get();
			cache.putIfAbsent(key, v);
		} else {
			cacheHits.incrementAndGet();
		}
		return v;
	}

	/**
	 * Returns the current number of entries stored in this cache.
	 *
	 * <p>
	 * This value represents the number of unique keys currently cached. It corresponds to
	 * cache misses, as each cache miss results in a new entry being added.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Returns 0 if the cache is disabled
	 * 	<li>The value may change between calls in multi-threaded environments
	 * 	<li>When combined with {@link #getCacheHits()}, can be used to calculate hit ratio
	 * </ul>
	 *
	 * @return The current number of cached entries, or 0 if the cache is disabled.
	 */
	public int size() {
		return cache == null ? 0 : cache.size();
	}

	/**
	 * Removes all entries from this cache.
	 *
	 * <p>
	 * This operation is atomic and thread-safe. After calling this method, all subsequent
	 * {@link #get(Object, Supplier)} calls will invoke their suppliers to recompute values.
	 *
	 * <h5 class='section'>Use Cases:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Free memory when the cache is no longer needed
	 * 	<li>Force fresh computation after underlying data has changed
	 * 	<li>Reset cache state during testing
	 * 	<li>Implement custom eviction policies
	 * </ul>
	 *
	 * <p>
	 * If the cache is disabled, this method has no effect.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This method does <em>not</em> reset the {@link #getCacheHits()} counter
	 * 	<li>Safe to call from multiple threads simultaneously
	 * </ul>
	 */
	public void clear() {
		if (cache != null) {
			cache.clear();
		}
	}

	/**
	 * Returns the total number of cache hits since this cache was created.
	 *
	 * <p>
	 * A cache hit occurs when {@link #get(Object, Supplier)} finds an existing cached value
	 * for the requested key, avoiding the need to invoke the supplier.
	 *
	 * <h5 class='section'>Cache Effectiveness:</h5>
	 * <p>
	 * You can calculate the cache hit ratio using:
	 * <p class='bjava'>
	 * 	<jk>int</jk> <jv>hits</jv> = <jv>cache</jv>.getCacheHits();
	 * 	<jk>int</jk> <jv>misses</jv> = <jv>cache</jv>.size();
	 * 	<jk>int</jk> <jv>total</jv> = <jv>hits</jv> + <jv>misses</jv>;
	 * 	<jk>double</jk> <jv>hitRatio</jv> = (<jk>double</jk>) <jv>hits</jv> / <jv>total</jv>;  <jc>// 0.0 to 1.0</jc>
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This counter is never reset, even when {@link #clear()} is called
	 * 	<li>Thread-safe using {@link AtomicInteger}
	 * 	<li>Returns 0 if the cache is disabled
	 * </ul>
	 *
	 * @return The total number of cache hits since creation.
	 */
	public int getCacheHits() { return cacheHits.get(); }
}