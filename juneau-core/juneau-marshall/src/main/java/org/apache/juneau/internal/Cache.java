// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.internal;

import static org.apache.juneau.internal.SystemEnv.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.common.internal.*;

/**
 * Simple in-memory cache of objects.
 *
 * <p>
 * Essentially just a wrapper around a ConcurrentHashMap.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class Cache<K,V> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param key The key type.
	 * @param type The value type.
	 * @return A new builder for this object.
	 */
	public static <K,V> Builder<K,V> of(Class<K> key, Class<V> type) {
		return new Builder<>(type);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
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
		 * Disables this cache.
		 *
		 * @return This object.
		 */
		public Builder<K,V> disabled() {
			disabled = true;
			return this;
		}

		/**
		 * When enabled, logs cache hit statistics on this cache.
		 *
		 * @return This object.
		 */
		public Builder<K,V> logOnExit() {
			logOnExit = true;
			return this;
		}

		/**
		 * Specifies the maximum size of this cache.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder<K,V> maxSize(int value) {
			maxSize = value;
			return this;
		}

		/**
		 * Builds this object.
		 *
		 * @return A new cache.
		 */
		public Cache<K,V> build() {
			return new Cache<>(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final int maxSize;
	private final ConcurrentHashMap<K,V> cache;
	private final AtomicInteger cacheHits = new AtomicInteger();

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.
	 */
	protected Cache(Builder<K,V> builder) {
		cache = builder.disabled ? null : new ConcurrentHashMap<>();
		maxSize = builder.maxSize;
		if (builder.logOnExit) {
			SystemUtils.shutdownMessage(()->builder.type.getSimpleName() + " cache:  hits=" + cacheHits.get() + ", misses: " + cache.size());
		}
	}
	/**
	 * Retrieves the value with the specified key from this cache.
	 *
	 * @param key The key.
	 * @param supplier The supplier for creating this object if it's not found in the cache.
	 * @return The value.
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
}
