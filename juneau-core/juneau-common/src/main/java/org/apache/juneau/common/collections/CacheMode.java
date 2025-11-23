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

/**
 * Cache modes for {@link Cache} and related cache classes.
 *
 * <p>
 * Defines how cache storage is implemented and when cached entries may be evicted.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Cache}
 * 	<li class='jc'>{@link Cache2}
 * 	<li class='jc'>{@link Cache3}
 * 	<li class='jc'>{@link Cache4}
 * 	<li class='jc'>{@link Cache5}
 * </ul>
 */
public enum CacheMode {

	/**
	 * No caching - all lookups invoke the supplier.
	 *
	 * <p>
	 * When this mode is used, the cache will not store any values. Every call to
	 * {@link Cache#get(Object)} or {@link Cache#get(Object, java.util.function.Supplier)}
	 * will invoke the supplier to compute the value.
	 *
	 * <p>
	 * This mode is useful for:
	 * <ul>
	 * 	<li>Testing scenarios where you want fresh computation each time
	 * 	<li>Development environments where caching might hide issues
	 * 	<li>Temporarily disabling caching without code changes
	 * </ul>
	 */
	NONE,

	/**
	 * Weak caching - uses {@link java.util.WeakHashMap} for storage.
	 *
	 * <p>
	 * Cache entries can be garbage collected when keys are no longer strongly referenced elsewhere.
	 * The WeakHashMap is wrapped with {@link java.util.Collections#synchronizedMap(java.util.Map)}
	 * for thread safety.
	 *
	 * <p>
	 * This mode is useful for:
	 * <ul>
	 * 	<li>Caching metadata about objects that may be unloaded (e.g., {@link Class} objects)
	 * 	<li>Memory-sensitive applications where cache entries should not prevent garbage collection
	 * 	<li>Scenarios where keys have limited lifetimes
	 * </ul>
	 *
	 * <p>
	 * <b>Note:</b> Weak caching comes with performance trade-offs:
	 * <ul>
	 * 	<li>Slightly slower access due to synchronization overhead
	 * 	<li>Entries may be removed unpredictably by the garbage collector
	 * 	<li>Not suitable for high-concurrency scenarios
	 * </ul>
	 */
	WEAK,

	/**
	 * Full caching - uses {@link java.util.concurrent.ConcurrentHashMap} for storage.
	 *
	 * <p>
	 * Provides the best performance with lock-free reads and writes. Cached entries
	 * will remain in memory until explicitly removed or the cache is cleared due to
	 * exceeding the maximum size.
	 *
	 * <p>
	 * This is the default and recommended mode for most use cases, offering:
	 * <ul>
	 * 	<li>Excellent performance with no synchronization overhead for reads
	 * 	<li>Thread-safe concurrent access
	 * 	<li>Predictable memory usage (entries stay until evicted)
	 * 	<li>Suitable for high-concurrency scenarios
	 * </ul>
	 */
	FULL;

	/**
	 * Parses a string value into a {@link CacheMode}.
	 *
	 * <p>
	 * Performs case-insensitive matching against the enum constant names.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	CacheMode.<jsm>parse</jsm>(<js>"none"</js>);  <jc>// Returns NONE</jc>
	 * 	CacheMode.<jsm>parse</jsm>(<js>"WEAK"</js>);  <jc>// Returns WEAK</jc>
	 * 	CacheMode.<jsm>parse</jsm>(<js>"Full"</js>);  <jc>// Returns FULL</jc>
	 * 	CacheMode.<jsm>parse</jsm>(<jk>null</jk>);    <jc>// Returns FULL (default)</jc>
	 * 	CacheMode.<jsm>parse</jsm>(<js>"invalid"</js>); <jc>// Returns FULL (default)</jc>
	 * </p>
	 *
	 * @param value The string value to parse. Can be <jk>null</jk>.
	 * @return The corresponding {@link CacheMode}, or {@link #FULL} if the value is <jk>null</jk> or invalid.
	 */
	public static CacheMode parse(String value) {
		if (value == null)
			return FULL;
		return switch (value.toUpperCase()) {
			case "NONE" -> NONE;
			case "WEAK" -> WEAK;
			case "FULL" -> FULL;
			default -> FULL;
		};
	}
}
