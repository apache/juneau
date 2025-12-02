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
 * Simple in-memory cache for storing and retrieving objects by two-part keys.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * This class extends {@link ConcurrentHashMap2Key} to provide a thread-safe caching layer with automatic
 * value computation, cache eviction, and statistics tracking for two-part composite keys. It's designed for
 * caching expensive-to-compute or frequently-accessed objects indexed by two keys.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Thread-safe concurrent access without external synchronization
 * 	<li>Two-part composite key support
 * 	<li>Automatic cache eviction when maximum size is reached
 * 	<li>Lazy computation via {@link Function2} supplier pattern
 * 	<li>Default supplier support for simplified access
 * 	<li>Built-in hit/miss statistics tracking
 * 	<li>Optional logging of cache statistics on JVM shutdown
 * 	<li>Can be disabled entirely via builder or system property
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a cache with default supplier</jc>
 * 	Cache2&lt;ClassLoader,Class&lt;?&gt;,ClassMeta&gt; <jv>metaCache</jv> = Cache2
 * 		.<jsm>of</jsm>(ClassLoader.<jk>class</jk>, Class.<jk>class</jk>, ClassMeta.<jk>class</jk>)
 * 		.maxSize(500)
 * 		.supplier((cl, c) -&gt; createClassMeta(cl, c))
 * 		.build();
 *
 * 	<jc>// Retrieve using default supplier</jc>
 * 	ClassMeta <jv>meta1</jv> = <jv>metaCache</jv>.get(<jv>classLoader</jv>, String.<jk>class</jk>);
 *
 * 	<jc>// Or override the supplier</jc>
 * 	ClassMeta <jv>meta2</jv> = <jv>metaCache</jv>.get(<jv>classLoader</jv>, Integer.<jk>class</jk>, () -&gt; customMeta);
 * </p>
 *
 * <h5 class='section'>Cache Behavior:</h5>
 * <ul class='spaced-list'>
 * 	<li>When a key pair is requested:
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
 * 	<li><c>juneau.cache.mode</c> - Cache mode: NONE/WEAK/FULL (default: FULL, case-insensitive)
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
 * 	<li>The {@link #get(Object, Object, java.util.function.Supplier)} method uses
 * 		{@link java.util.concurrent.ConcurrentHashMap#putIfAbsent(Object, Object)}
 * 		to minimize redundant computation in concurrent scenarios
 * 	<li>When max size is exceeded, the entire cache is cleared in a single operation
 * 	<li>Statistics tracking uses {@link AtomicInteger} for thread-safe counting without locking
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple cache with defaults</jc>
 * 	Cache2&lt;String,String,Config&gt; <jv>cache</jv> = Cache2.<jsm>of</jsm>(String.<jk>class</jk>, String.<jk>class</jk>, Config.<jk>class</jk>).build();
 *
 * 	<jc>// Cache with custom configuration</jc>
 * 	Cache2&lt;String,Integer,User&gt; <jv>userCache</jv> = Cache2
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>)
 * 		.maxSize(500)
 * 		.logOnExit()
 * 		.supplier((tenant, id) -&gt; userService.findUser(tenant, id))
 * 		.build();
 *
 * 	<jc>// Disabled cache for testing</jc>
 * 	Cache2&lt;String,String,Object&gt; <jv>disabledCache</jv> = Cache2
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, String.<jk>class</jk>, Object.<jk>class</jk>)
 * 		.disableCaching()
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Cache}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-commons">Overview &gt; juneau-commons</a>
 * </ul>
 *
 * @param <K1> The first key type. Can be an array type for content-based key matching.
 * @param <K2> The second key type. Can be an array type for content-based key matching.
 * @param <V> The value type.
 */
public class Cache2<K1,K2,V> {

	/**
	 * Builder for creating configured {@link Cache2} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache2&lt;String,Integer,User&gt; <jv>cache</jv> = Cache2
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>)
	 * 		.maxSize(200)
	 * 		.logOnExit()
	 * 		.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link Cache2#of(Class, Class, Class)}
	 * </ul>
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K1,K2,V> {
		CacheMode cacheMode;
		int maxSize;
		String id;
		boolean logOnExit;
		boolean threadLocal;
		Function2<K1,K2,V> supplier;

		Builder() {
			cacheMode = CacheMode.parse(env("juneau.cache.mode", "FULL"));
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
			id = "Cache2";
		}

		/**
		 * Builds a new {@link Cache2} instance with the configured settings.
		 *
		 * @return A new immutable {@link Cache2} instance.
		 */
		public Cache2<K1,K2,V> build() {
			return new Cache2<>(this);
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
		public Builder<K1,K2,V> cacheMode(CacheMode value) {
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
		public Builder<K1,K2,V> logOnExit(boolean value, String id) {
			this.id = id;
			this.logOnExit = value;
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
		 * 	ClassMeta cache:  hits=1523, misses: 47
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
		 * @param id The identifier to use in the log message.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,V> logOnExit(String id) {
			this.id = id;
			this.logOnExit = true;
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
		public Builder<K1,K2,V> maxSize(int value) {
			maxSize = value;
			return this;
		}

		/**
		 * Specifies the default supplier function for computing values when keys are not found.
		 *
		 * <p>
		 * This supplier will be used by {@link Cache2#get(Object, Object)} when a key pair is not in the cache.
		 * Individual lookups can override this supplier using
		 * {@link Cache2#get(Object, Object, java.util.function.Supplier)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	Cache2&lt;String,Integer,User&gt; <jv>cache</jv> = Cache2
		 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>)
		 * 		.supplier((tenant, id) -&gt; userService.findUser(tenant, id))
		 * 		.build();
		 *
		 * 	<jc>// Uses default supplier</jc>
		 * 	User <jv>u</jv> = <jv>cache</jv>.get(<js>"tenant1"</js>, 123);
		 * </p>
		 *
		 * @param value The default supplier function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,V> supplier(Function2<K1,K2,V> value) {
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
		public Builder<K1,K2,V> threadLocal() {
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
		public Builder<K1,K2,V> weak() {
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
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Working with complex generic types</jc>
	 * 	Cache2&lt;Class&lt;?&gt;,Class&lt;? extends Annotation&gt;,List&lt;Annotation&gt;&gt; <jv>cache</jv> =
	 * 		Cache2.&lt;Class&lt;?&gt;,Class&lt;? extends Annotation&gt;,List&lt;Annotation&gt;&gt;<jsm>create</jsm>()
	 * 			.supplier((k1, k2) -&gt; findAnnotations(k1, k2))
	 * 			.build();
	 * </p>
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <V> The value type.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,V> Builder<K1,K2,V> create() {
		return new Builder<>();
	}

	/**
	 * Creates a new {@link Builder} for constructing a cache.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache2&lt;String,Integer,User&gt; <jv>cache</jv> = Cache2
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>)
	 * 		.maxSize(100)
	 * 		.build();
	 * </p>
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <V> The value type.
	 * @param key1 The first key type class (used for type safety).
	 * @param key2 The second key type class (used for type safety).
	 * @param type The value type class.
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,V> Builder<K1,K2,V> of(Class<K1> key1, Class<K2> key2, Class<V> type) {
		return new Builder<>();
	}

	// Internal map with Tuple2 keys for content-based equality (especially for arrays)
	// If threadLocal is true, this is null and threadLocalMap is used instead
	private final java.util.Map<Tuple2<K1,K2>,V> map;

	private final ThreadLocal<Map<Tuple2<K1,K2>,V>> threadLocalMap;

	private final boolean isThreadLocal;

	private final int maxSize;
	private final CacheMode cacheMode;
	private final Function2<K1,K2,V> supplier;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache2(Builder<K1,K2,V> builder) {
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
	 * Returns <jk>true</jk> if the cache contains a mapping for the specified key pair.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the cache contains the key pair.
	 */
	public boolean containsKey(K1 key1, K2 key2) {
		return getMap().containsKey(Tuple2.of(key1, key2));
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
	 * Retrieves a cached value by key pair using the default supplier.
	 *
	 * <p>
	 * This method uses the default supplier configured via {@link Builder#supplier(Function2)}.
	 * If no default supplier was configured, this method will throw a {@link NullPointerException}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache2&lt;String,Integer,User&gt; <jv>cache</jv> = Cache2
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>)
	 * 		.supplier((tenant, id) -&gt; userService.findUser(tenant, id))
	 * 		.build();
	 *
	 * 	<jc>// Uses default supplier</jc>
	 * 	User <jv>u</jv> = <jv>cache</jv>.get(<js>"tenant1"</js>, 123);
	 * </p>
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws NullPointerException if no default supplier was configured.
	 */
	public V get(K1 key1, K2 key2) {
		return get(key1, key2, () -> supplier.apply(key1, key2));
	}

	/**
	 * Retrieves a cached value by key pair, computing it if necessary using the provided supplier.
	 *
	 * <p>
	 * This method implements the cache-aside pattern:
	 * <ol>
	 * 	<li>If the key pair exists in the cache, return the cached value (cache hit)
	 * 	<li>If the key pair doesn't exist, invoke the supplier to compute the value
	 * 	<li>Store the computed value in the cache using
	 * 		{@link java.util.concurrent.ConcurrentHashMap#putIfAbsent(Object, Object)}
	 * 	<li>Return the value
	 * </ol>
	 *
	 * <h5 class='section'>Behavior:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>If the cache is disabled, always invokes the supplier without caching
	 * 	<li>If the cache exceeds {@link Builder#maxSize(int)}, clears all entries before storing the new value
	 * 	<li>Thread-safe: Multiple threads can safely call this method concurrently
	 * 	<li>The supplier may be called multiple times for the same key pair in concurrent scenarios
	 * 		(due to {@link java.util.concurrent.ConcurrentHashMap#putIfAbsent(Object, Object)} semantics)
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache2&lt;String,Integer,User&gt; <jv>cache</jv> = Cache2.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>, User.<jk>class</jk>).build();
	 *
	 * 	<jc>// First call: fetches user and caches it</jc>
	 * 	User <jv>u1</jv> = <jv>cache</jv>.get(<js>"tenant1"</js>, 123, () -&gt; userService.findUser(<js>"tenant1"</js>, 123));
	 *
	 * 	<jc>// Second call: returns cached user instantly</jc>
	 * 	User <jv>u2</jv> = <jv>cache</jv>.get(<js>"tenant1"</js>, 123, () -&gt; userService.findUser(<js>"tenant1"</js>, 123));
	 *
	 * 	<jsm>assert</jsm> <jv>u1</jv> == <jv>u2</jv>;  <jc>// Same instance</jc>
	 * </p>
	 *
	 * @param key1 First key component. Can be <jk>null</jk>.
	 * @param key2 Second key component. Can be <jk>null</jk>.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 */
	public V get(K1 key1, K2 key2, java.util.function.Supplier<V> supplier) {
		assertArgNotNull("supplier", supplier);
		if (cacheMode == NONE)
			return supplier.get();
		var m = getMap();
		var wrapped = Tuple2.of(key1, key2);
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
	 * <p>
	 * A cache hit occurs when {@link #get(Object, Object)} or
	 * {@link #get(Object, Object, java.util.function.Supplier)} finds an existing cached value
	 * for the requested key pair, avoiding the need to invoke the supplier.
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

	/**
	 * Returns <jk>true</jk> if the cache contains no entries.
	 *
	 * @return <jk>true</jk> if the cache is empty.
	 */
	public boolean isEmpty() { return getMap().isEmpty(); }

	/**
	 * Associates the specified value with the specified key pair.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @param value The value to associate with the key pair.
	 * @return The previous value associated with the key pair, or <jk>null</jk> if there was no mapping.
	 */
	public V put(K1 key1, K2 key2, V value) {
		var m = getMap();
		if (value == null)
			return m.remove(Tuple2.of(key1, key2));
		return m.put(Tuple2.of(key1, key2), value);
	}

	/**
	 * Removes the entry for the specified key pair from the cache.
	 *
	 * @param key1 The first key. Can be <jk>null</jk>.
	 * @param key2 The second key. Can be <jk>null</jk>.
	 * @return The previous value associated with the key pair, or <jk>null</jk> if there was no mapping.
	 */
	public V remove(K1 key1, K2 key2) {
		return getMap().remove(Tuple2.of(key1, key2));
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
	private Map<Tuple2<K1,K2>,V> getMap() { return isThreadLocal ? threadLocalMap.get() : map; }
}
