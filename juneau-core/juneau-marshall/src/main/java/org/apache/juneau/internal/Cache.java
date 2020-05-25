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

import java.util.concurrent.*;

/**
 * Simple in-memory cache of objects.
 *
 * <p>
 * Essentially just a wrapper around a ConcurrentHashMap.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class Cache<K,V> {
	private final boolean nocache;
	private final int maxSize;
	private final ConcurrentHashMap<K,V> cache;

	/**
	 * Constructor.
	 *
	 * @param disabled If <jk>true</jk> then the cache is disabled.
	 * @param maxSize The maximum size of the cache.  If this threshold is reached, the cache is flushed.
	 */
	public Cache(boolean disabled, int maxSize) {
		this.nocache = disabled;
		this.maxSize = maxSize;
		if (! nocache)
			cache = new ConcurrentHashMap<>();
		else
			cache = null;
	}

	/**
	 * Retrieves the value with the specified key from this cache.
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the value is not in the cache, or the cache is disabled.
	 */
	public V get(K key) {
		if (nocache || key == null)
			return null;
		return cache.get(key);
	}

	/**
	 * Adds the value with the specified key to this cache.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return
	 * 	Either the value already in the cache if it already exists, or the same value passed in.
	 * 	Always returns the same value if the cache is disabled.
	 */
	public V put(K key, V value) {
		if (nocache || key == null)
			return value;

		// Prevent OOM in case of DDOS
		if (cache.size() > maxSize)
			cache.clear();

		while (true) {
			V v = cache.get(key);
			if (v != null)
				return v;
			cache.putIfAbsent(key, value);
			return value;
		}
	}
}
