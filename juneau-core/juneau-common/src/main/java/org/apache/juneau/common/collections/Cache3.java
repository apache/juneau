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
import static org.apache.juneau.common.utils.SystemUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.common.function.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-common">Overview &gt; juneau-common</a>
 * </ul>
 *
 * @param <K1> The first key type.
 * @param <K2> The second key type.
 * @param <K3> The third key type.
 * @param <V> The value type.
 */
public class Cache3<K1,K2,K3,V> extends ConcurrentHashMap3Key<K1,K2,K3,V> {

	private static final long serialVersionUID = 1L;

	/**
	 * Builder for creating configured {@link Cache3} instances.
	 *
	 * @param <K1> The first key type.
	 * @param <K2> The second key type.
	 * @param <K3> The third key type.
	 * @param <V> The value type.
	 */
	public static class Builder<K1,K2,K3,V> {
		boolean disableCaching, logOnExit;
		int maxSize;
		Class<V> type;
		Function3<K1,K2,K3,V> supplier;

		Builder(Class<V> type) {
			this.type = type;
			disableCaching = env("juneau.cache.disable", false);
			maxSize = env("juneau.cache.maxSize", 1000);
			logOnExit = env("juneau.cache.logOnExit", false);
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
		 * Disables caching entirely.
		 *
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> disableCaching() {
			disableCaching = true;
			return this;
		}

		/**
		 * Optionally disables caching based on the provided flag.
		 *
		 * @param value If <jk>true</jk>, disables caching.
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> disableCaching(boolean value) {
			disableCaching = value;
			return this;
		}

		/**
		 * Enables logging of cache statistics when the JVM exits.
		 *
		 * @return This object for method chaining.
		 */
		public Builder<K1,K2,K3,V> logOnExit() {
			logOnExit = true;
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
	 * @param type The value type class (used for logging and type safety).
	 * @return A new builder for configuring the cache.
	 */
	public static <K1,K2,K3,V> Builder<K1,K2,K3,V> of(Class<K1> key1, Class<K2> key2, Class<K3> key3, Class<V> type) {
		return new Builder<>(type);
	}

	private final int maxSize;
	private final boolean disableCaching;
	private final Function3<K1,K2,K3,V> supplier;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing configuration settings.
	 */
	protected Cache3(Builder<K1,K2,K3,V> builder) {
		this.maxSize = builder.maxSize;
		this.disableCaching = builder.disableCaching;
		this.supplier = builder.supplier;
		if (builder.logOnExit) {
			shutdownMessage(() -> scn(builder.type) + " cache:  hits=" + cacheHits.get() + ", misses: " + size());
		}
	}

	/**
	 * Retrieves a cached value by key triplet using the default supplier.
	 *
	 * @param key1 First key component. Must not be <jk>null</jk>.
	 * @param key2 Second key component. Must not be <jk>null</jk>.
	 * @param key3 Third key component. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws NullPointerException if no default supplier was configured.
	 * @throws IllegalArgumentException if any key is <jk>null</jk>.
	 */
	@Override /* ConcurrentHashMap3Key */
	public V get(K1 key1, K2 key2, K3 key3) {
		assertArgsNotNull("key1", key1, "key2", key2, "key3", key3);
		return get(key1, key2, key3, () -> supplier.apply(key1, key2, key3));
	}

	/**
	 * Retrieves a cached value by key triplet, computing it if necessary using the provided supplier.
	 *
	 * @param key1 First key component. Must not be <jk>null</jk>.
	 * @param key2 Second key component. Must not be <jk>null</jk>.
	 * @param key3 Third key component. Must not be <jk>null</jk>.
	 * @param supplier The supplier to compute the value if it's not in the cache. Must not be <jk>null</jk>.
	 * @return The cached or computed value. May be <jk>null</jk> if the supplier returns <jk>null</jk>.
	 * @throws IllegalArgumentException if any key is <jk>null</jk>.
	 */
	public V get(K1 key1, K2 key2, K3 key3, java.util.function.Supplier<V> supplier) {
		assertArgsNotNull("key1", key1, "key2", key2, "key3", key3);
		if (disableCaching)
			return supplier.get();
		V v = super.get(key1, key2, key3);
		if (v == null) {
			if (size() > maxSize)
				clear();
			v = supplier.get();
			super.put(key1, key2, key3, v);
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
}

