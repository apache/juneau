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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.concurrent.*;

/**
 * Memoizing decorator for a {@link PropertySource} whose answers can never change after construction.
 *
 * <p>
 * Every lookup result, hits and misses alike, is cached in a {@link ConcurrentHashMap}, so a repeated lookup costs one
 * map read instead of whatever the delegate does (for example the relaxed environment source tokenizes the name and
 * probes up to three environment variables).
 *
 * <h5 class='section'>Contract:</h5>
 * <p>
 * <b>Wrap only sources whose answers cannot change after construction, including their misses.</b>
 * A wrapped source that answers {@link PropertyLookupResult#missing()} once, for any reason, answers it forever.
 * Never wrap system properties, config files, Spring environments, stores, or any source that can return
 * {@code missing()} without memoizing (for example during a static initializer).
 * Do not wrap a {@code SensitivePropertySource} either: the wrapper hides the redaction marker and keeps secret values in
 * memory for its lifetime.
 *
 * <h5 class='section'>Soft cap:</h5>
 * <p>
 * The cache holds at most {@link #MAX_ENTRIES} names. Once full, new names pass through to the delegate uncached; there
 * is no eviction, so the first names seen win. The check is not locked: concurrent first lookups can overshoot the cap
 * by at most the number of racing threads.
 *
 * <h5 class='section'>Null names:</h5>
 * <p>
 * A {@code null} name skips the cache (it cannot hold a {@code null} key) and goes to the delegate, so each delegate
 * keeps its own {@code null} behavior.
 *
 * @since 10.0.0
 */
public final class CachingPropertySource implements PropertySource {

	/**
	 * The maximum number of names cached by one instance.
	 */
	public static final int MAX_ENTRIES = 4096;

	private final PropertySource delegate;
	private final ConcurrentHashMap<String,PropertyLookupResult> cache = new ConcurrentHashMap<>();

	/**
	 * Wraps the specified source.
	 *
	 * @param delegate The source to wrap. Its answers must never change. Must not be <jk>null</jk>.
	 * @return A new caching source.
	 */
	public static CachingPropertySource of(PropertySource delegate) {
		return new CachingPropertySource(reqnn("delegate", delegate));
	}

	private CachingPropertySource(PropertySource delegate) {
		this.delegate = delegate;
	}

	@Override /* PropertySource */
	public PropertyLookupResult get(String name) {
		if (name == null)
			return delegate.get(null);
		var cached = cache.get(name);
		if (cached != null)
			return cached;
		var result = delegate.get(name);
		if (cache.size() < MAX_ENTRIES) {
			var previous = cache.putIfAbsent(name, result);
			if (previous != null)
				return previous;
		}
		return result;
	}

	/** Returns the wrapped source. For tests that check which sources are wrapped; not public API. */
	PropertySource delegate() {
		return delegate;
	}

	/** Returns the number of cached names. For tests; not public API. */
	int size() {
		return cache.size();
	}
}
