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
import static java.util.Collections.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.common.function.*;

/**
 * Simple in-memory cache for storing and retrieving objects by key.
 *
 * <h5 class='section'>Overview:</h5>
 * <p>
 * This class uses {@link ConcurrentHashMap1Key} internally to provide a thread-safe caching layer with automatic
 * value computation, cache eviction, and statistics tracking. It's designed for caching expensive-to-compute
 * or frequently-accessed objects to improve performance.
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>Thread-safe concurrent access without external synchronization
 * 	<li>Automatic cache eviction when maximum size is reached
 * 	<li>Lazy computation via {@link Supplier} pattern
 * 	<li>Default supplier support for simplified access
 * 	<li>Built-in hit/miss statistics tracking
 * 	<li>Optional logging of cache statistics on JVM shutdown
 * 	<li>Can be disabled entirely via builder or system property
 * 	<li><b>Array Support:</b> Arrays can be used as keys with proper content-based hashing and equality
 * </ul>
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a cache with default supplier</jc>
 * 	Cache&lt;String,Pattern&gt; <jv>patternCache</jv> = Cache
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
 * 		.maxSize(100)
 * 		.supplier(Pattern::compile)
 * 		.build();
 *
 * 	<jc>// Retrieve using default supplier</jc>
 * 	Pattern <jv>pattern1</jv> = <jv>patternCache</jv>.get(<js>"[a-z]+"</js>);
 *
 * 	<jc>// Or override the supplier</jc>
 * 	Pattern <jv>pattern2</jv> = <jv>patternCache</jv>.get(<js>"[0-9]+"</js>, () -&gt; Pattern.compile(<js>"[0-9]+"</js>, Pattern.CASE_INSENSITIVE));
 * </p>
 *
 * <h5 class='section'>Array Support:</h5>
 * <p>
 * Unlike standard {@link java.util.HashMap} which uses identity-based equality for array keys,
 * this class properly handles arrays using content-based comparison via {@link ConcurrentHashMap1Key}:
 *
 * <p class='bjava'>
 * 	<jc>// Arrays work correctly as keys</jc>
 * 	Cache&lt;String[],Result&gt; <jv>cache</jv> = Cache.<jsm>of</jsm>(String[].<jk>class</jk>, Result.<jk>class</jk>).build();
 * 	<jv>cache</jv>.get(<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>}, () -&gt; computeResult());
 * 	Result <jv>r</jv> = <jv>cache</jv>.get(<jk>new</jk> String[]{<js>"a"</js>, <js>"b"</js>}, () -&gt; computeResult());  <jc>// Cache hit!</jc>
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
 * 	<li>The {@link #get(Object, Supplier)} method uses {@link ConcurrentHashMap#putIfAbsent(Object, Object)}
 * 		to minimize redundant computation in concurrent scenarios
 * 	<li>When max size is exceeded, the entire cache is cleared in a single operation
 * 	<li>Statistics tracking uses {@link AtomicInteger} for thread-safe counting without locking
 * 	<li>For arrays, content-based hashing via {@link java.util.Arrays#hashCode(Object[])} ensures proper cache hits
 * </ul>
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Simple cache with defaults using of()</jc>
 * 	Cache&lt;String,Integer&gt; <jv>cache</jv> = Cache.<jsm>of</jsm>(String.<jk>class</jk>, Integer.<jk>class</jk>).build();
 *
 * 	<jc>// Cache with custom configuration using of()</jc>
 * 	Cache&lt;Class&lt;?&gt;,ClassMeta&gt; <jv>classMetaCache</jv> = Cache
 * 		.<jsm>of</jsm>(Class.<jk>class</jk>, ClassMeta.<jk>class</jk>)
 * 		.maxSize(500)
 * 		.logOnExit(<js>"ClassMeta"</js>)
 * 		.build();
 *
 * 	<jc>// Complex generics using create()</jc>
 * 	Cache&lt;Class&lt;?&gt;,List&lt;AnnotationInfo&lt;Annotation&gt;&gt;&gt; <jv>annotationsCache</jv> =
 * 		Cache.&lt;Class&lt;?&gt;,List&lt;AnnotationInfo&lt;Annotation&gt;&gt;&gt;<jsm>create</jsm>()
 * 			.supplier(<jk>this</jk>::findClassAnnotations)
 * 			.build();
 *
 * 	<jc>// Disabled cache for testing</jc>
 * 	Cache&lt;String,Object&gt; <jv>disabledCache</jv> = Cache
 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Object.<jk>class</jk>)
 * 		.disableCaching()
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K> The key type. Can be an array type for content-based key matching.
 * @param <V> The value type.
 */
public class Cache<K,V> {

	/**
	 * Builder for creating configured {@link Cache} instances.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Using of() for simple types</jc>
	 * 	Cache&lt;String,Pattern&gt; <jv>cache1</jv> = Cache
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
	 * 		.maxSize(200)
	 * 		.logOnExit(<js>"Pattern"</js>)
	 * 		.build();
	 *
	 * 	<jc>// Using create() for complex generics</jc>
	 * 	Cache&lt;Class&lt;?&gt;,List&lt;Method&gt;&gt; <jv>cache2</jv> =
	 * 		Cache.&lt;Class&lt;?&gt;,List&lt;Method&gt;&gt;<jsm>create</jsm>()
	 * 			.supplier(<jk>this</jk>::findMethods)
	 * 			.build();
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link Cache#of(Class, Class)}
	 * 	<li class='jm'>{@link Cache#create()}
	 * </ul>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K,V> {
		CacheMode cacheMode;
		int maxSize;
		String id;
		boolean logOnExit;
		boolean threadLocal;
		Function<K,V> supplier;

		Builder() {
			cacheMode = CacheMode.parse(env("juneau.cache.mode", "FULL"));
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
			id = "Cache";
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
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// No caching for testing</jc>
		 * 	Cache&lt;String,Object&gt; <jv>cache1</jv> = Cache.<jsm>of</jsm>(String.<jk>class</jk>, Object.<jk>class</jk>)
		 * 		.cacheMode(CacheMode.<jsf>NONE</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Weak caching for Class metadata</jc>
		 * 	Cache&lt;Class&lt;?&gt;,ClassMeta&gt; <jv>cache2</jv> = Cache.<jsm>of</jsm>(Class.<jk>class</jk>, ClassMeta.<jk>class</jk>)
		 * 		.cacheMode(CacheMode.<jsf>WEAK</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Full caching (default)</jc>
		 * 	Cache&lt;String,Pattern&gt; <jv>cache3</jv> = Cache.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
		 * 		.cacheMode(CacheMode.<jsf>FULL</jsf>)
		 * 		.build();
		 * </p>
		 *
		 * @param value The caching mode.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> cacheMode(CacheMode value) {
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
		public Builder<K,V> logOnExit(boolean value, String id) {
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
		 * @param id The identifier to use in the log message.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> logOnExit(String id) {
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
		public Builder<K,V> maxSize(int value) {
			maxSize = value;
			return this;
		}

		/**
		 * Specifies the default supplier function for computing values when keys are not found.
		 *
		 * <p>
		 * This supplier will be used by {@link Cache#get(Object)} when a key is not in the cache.
		 * Individual lookups can override this supplier using {@link Cache#get(Object, Supplier)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	Cache&lt;String,Pattern&gt; <jv>cache</jv> = Cache
		 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
		 * 		.supplier(Pattern::compile)
		 * 		.build();
		 *
		 * 	<jc>// Uses default supplier</jc>
		 * 	Pattern <jv>p</jv> = <jv>cache</jv>.get(<js>"[a-z]+"</js>);
		 * </p>
		 *
		 * @param value The default supplier function. Can be <jk>null</jk>.
		 * @return This object for method chaining.
		 */
		public Builder<K,V> supplier(Function<K,V> value) {
			supplier = value;
			return this;
		}

		/**
		 * Enables thread-local caching.
		 *
		 * <p>
		 * When enabled, each thread gets its own separate cache instance. This is useful for
		 * thread-unsafe objects like {@link java.text.MessageFormat} that need to be cached per thread.
		 *
		 * <p>
		 * This is a shortcut for wrapping a cache in a {@link ThreadLocal}, but provides a cleaner API.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Thread-local cache for MessageFormat instances</jc>
		 * 	Cache&lt;String,MessageFormat&gt; <jv>cache</jv> = Cache
		 * 		.<jsm>of</jsm>(String.<jk>class</jk>, MessageFormat.<jk>class</jk>)
		 * 		.maxSize(100)
		 * 		.<jsm>threadLocal</jsm>()
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This is equivalent to:
		 * <p class='bjava'>
		 * 	ThreadLocal&lt;Cache&lt;String,MessageFormat&gt;&gt; <jv>cache</jv> =
		 * 		ThreadLocal.<jsm>withInitial</jsm>(() -&gt; Cache
		 * 			.<jsm>of</jsm>(String.<jk>class</jk>, MessageFormat.<jk>class</jk>)
		 * 			.maxSize(100)
		 * 			.build());
		 * </p>
		 *
		 * @return This object for method chaining.
		 */
		public Builder<K,V> threadLocal() {
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
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Weak caching for Class metadata</jc>
		 * 	Cache&lt;Class&lt;?&gt;,ClassMeta&gt; <jv>cache</jv> = Cache.<jsm>of</jsm>(Class.<jk>class</jk>, ClassMeta.<jk>class</jk>)
		 * 		.<jsm>weak</jsm>()
		 * 		.build();
		 * </p>
		 *
		 * @return This object for method chaining.
		 * @see #cacheMode(CacheMode)
		 */
		public Builder<K,V> weak() {
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
	 * 	Cache&lt;Class&lt;?&gt;,List&lt;AnnotationInfo&lt;Annotation&gt;&gt;&gt; <jv>cache</jv> =
	 * 		Cache.&lt;Class&lt;?&gt;,List&lt;AnnotationInfo&lt;Annotation&gt;&gt;&gt;<jsm>create</jsm>()
	 * 			.supplier(<jv>key</jv> -&gt; findAnnotations(<jv>key</jv>))
	 * 			.build();
	 * </p>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @return A new builder for configuring the cache.
	 */
	public static <K,V> Builder<K,V> create() {
		return new Builder<>();
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
	 * @param type The value type class.
	 * @return A new builder for configuring the cache.
	 */
	public static <K,V> Builder<K,V> of(Class<K> key, Class<V> type) {
		return new Builder<>();
	}

	// Internal map with Tuple1 keys for content-based equality (especially for arrays)
	// If threadLocal is true, this is null and threadLocalMap is used instead
	private final Map<Tuple1<K>,V> map;
	private final ThreadLocal<Map<Tuple1<K>,V>> threadLocalMap;

	private final boolean isThreadLocal;

	/**
	 * Cache of Tuple1 wrapper objects to minimize object creation on repeated get/put calls.
	 *
	 * <p>
	 * Uses WeakHashMap so wrappers can be GC'd when keys are no longer referenced.
	 * This provides a significant performance improvement for caches with repeated key access.
	 * If threadLocal is true, this is null and threadLocalWrapperCache is used instead.
	 */
	private final Map<K,Tuple1<K>> wrapperCache;

	private final ThreadLocal<Map<K,Tuple1<K>>> threadLocalWrapperCache;

	private final int maxSize;

	private final boolean disableCaching;

	private final Function<K,V> supplier;

	private final AtomicInteger cacheHits = new AtomicInteger();
	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache(Builder<K,V> builder) {
		this.maxSize = builder.maxSize;
		this.disableCaching = builder.cacheMode == NONE;
		this.supplier = builder.supplier != null ? builder.supplier : (K) -> null;
		this.isThreadLocal = builder.threadLocal;

		if (isThreadLocal) {
			// Thread-local mode: each thread gets its own map
			if (builder.cacheMode == WEAK) {
				this.threadLocalMap = ThreadLocal.withInitial(() -> synchronizedMap(new WeakHashMap<>()));
				this.threadLocalWrapperCache = ThreadLocal.withInitial(() -> synchronizedMap(new WeakHashMap<>()));
			} else {
				this.threadLocalMap = ThreadLocal.withInitial(() -> new ConcurrentHashMap<>());
				this.threadLocalWrapperCache = ThreadLocal.withInitial(() -> synchronizedMap(new WeakHashMap<>()));
			}
			this.map = null;
			this.wrapperCache = null;
		} else {
			// Normal mode: shared map across all threads
			if (builder.cacheMode == WEAK) {
				this.map = synchronizedMap(new WeakHashMap<>());
				this.wrapperCache = synchronizedMap(new WeakHashMap<>());
			} else {
				this.map = new ConcurrentHashMap<>();
				this.wrapperCache = synchronizedMap(new WeakHashMap<>());
			}
			this.threadLocalMap = null;
			this.threadLocalWrapperCache = null;
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
		getWrapperCache().clear(); // Clean up wrapper cache
	}
	/**
	 * Returns <jk>true</jk> if the cache contains a mapping for the specified key.
	 *
	 * @param key The key to check.
	 * @return <jk>true</jk> if the cache contains the key.
	 */
	public boolean containsKey(K key) {
		return getMap().containsKey(wrap(key));
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
	 * Retrieves a cached value by key using the default supplier.
	 *
	 * <p>
	 * This method uses the default supplier configured via {@link Builder#supplier(Function)}.
	 * If no default supplier was configured, this method will throw a {@link NullPointerException}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Cache&lt;String,Pattern&gt; <jv>cache</jv> = Cache
	 * 		.<jsm>of</jsm>(String.<jk>class</jk>, Pattern.<jk>class</jk>)
	 * 		.supplier(Pattern::compile)
	 * 		.build();
	 *
	 * 	<jc>// Uses default supplier</jc>
	 * 	Pattern <jv>p</jv> = <jv>cache</jv>.get(<js>"[0-9]+"</js>);
	 * </p>
	 *
	 * @param key The cache key. Can be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws NullPointerException if no default supplier was configured.
	 */
	public V get(K key) {
		return get(key, () -> supplier.apply(key));
	}

	/**
	 * Retrieves a cached value by key, computing it if necessary using the provided supplier.
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
	 * 	<li>If the cache exceeds {@link Builder#maxSize(int)}, clears all entries before storing the new value
	 * 	<li>Thread-safe: Multiple threads can safely call this method concurrently
	 * 	<li>The supplier may be called multiple times for the same key in concurrent scenarios
	 * 		(due to {@link ConcurrentHashMap#putIfAbsent(Object, Object)} semantics)
	 * 	<li><b>Array Keys:</b> Arrays are matched by content, not identity
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
	 * @param key The cache key. Can be <jk>null</jk>.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 */
	public V get(K key, Supplier<V> supplier) {
		assertArgNotNull("supplier", supplier);
		if (disableCaching)
			return supplier.get();
		var m = getMap();
		Tuple1<K> wrapped = wrap(key);
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
	 * A cache hit occurs when {@link #get(Object)} or {@link #get(Object, Supplier)} finds an existing
	 * cached value for the requested key, avoiding the need to invoke the supplier.
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
	 * Associates the specified value with the specified key in this cache.
	 *
	 * @param key The cache key. Can be <jk>null</jk>.
	 * @param value The value to associate with the key.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 */
	public V put(K key, V value) {
		var m = getMap();
		if (value == null) {
			Tuple1<K> wrapped = wrap(key);
			V result = m.remove(wrapped);
			getWrapperCache().remove(key); // Clean up wrapper cache
			return result;
		}
		return m.put(wrap(key), value);
	}

	/**
	 * Removes the entry for the specified key from the cache.
	 *
	 * @param key The key to remove. Can be <jk>null</jk>.
	 * @return The previous value associated with the key, or <jk>null</jk> if there was no mapping.
	 */
	public V remove(K key) {
		var m = getMap();
		var wrapped = wrap(key);
		V result = m.remove(wrapped);
		getWrapperCache().remove(key); // Clean up wrapper cache
		return result;
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
	private Map<Tuple1<K>,V> getMap() { return isThreadLocal ? threadLocalMap.get() : map; }

	/**
	 * Gets the wrapper cache for the current thread.
	 *
	 * @return The wrapper cache for the current thread.
	 */
	private Map<K,Tuple1<K>> getWrapperCache() { return isThreadLocal ? threadLocalWrapperCache.get() : wrapperCache; }

	/**
	 * Gets or creates a Tuple1 wrapper for the given key.
	 *
	 * <p>
	 * The Tuple1 wrapper provides content-based equality for arrays and other objects.
	 * By caching these wrappers, we avoid creating new Tuple1 objects on every cache access.
	 *
	 * @param key The key to wrap.
	 * @return A cached or new Tuple1 wrapper for the key.
	 */
	private Tuple1<K> wrap(K key) {
		return getWrapperCache().computeIfAbsent(key, k -> Tuple1.of(k));
	}
}
