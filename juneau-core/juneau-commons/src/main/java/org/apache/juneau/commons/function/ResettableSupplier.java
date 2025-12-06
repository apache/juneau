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
package org.apache.juneau.commons.function;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * A thread-safe supplier that caches the result of the first call and supports resetting the cache.
 *
 * <p>
 * This class extends {@link OptionalSupplier} to provide both standard {@link Supplier#get()} functionality
 * and a {@link #reset()} method to clear the cache, forcing recomputation on the next call to {@link #get()}.
 * It also inherits all Optional-like convenience methods from {@link OptionalSupplier}.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a resettable supplier</jc>
 * 	ResettableSupplier&lt;String&gt; <jv>supplier</jv> = <jk>new</jk> ResettableSupplier&lt;&gt;(() -&gt; expensiveComputation());
 *
 * 	<jc>// First call computes and caches</jc>
 * 	String <jv>result1</jv> = <jv>supplier</jv>.get();
 *
 * 	<jc>// Subsequent calls return cached value</jc>
 * 	String <jv>result2</jv> = <jv>supplier</jv>.get();
 *
 * 	<jc>// Use Optional-like methods</jc>
 * 	<jk>if</jk> (<jv>supplier</jv>.isPresent()) {
 * 		String <jv>upper</jv> = <jv>supplier</jv>.map(String::toUpperCase).orElse(<js>"default"</js>);
 * 	}
 *
 * 	<jc>// Reset forces recomputation on next get()</jc>
 * 	<jv>supplier</jv>.reset();
 * 	String <jv>result3</jv> = <jv>supplier</jv>.get();  <jc>// Recomputes</jc>
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe for both {@link #get()} and {@link #reset()} operations.
 * If multiple threads call get() simultaneously after a reset, the supplier may be invoked
 * multiple times, but only one result will be cached.
 *
 * <h5 class='section'>Notes:</h5>
 * <ul>
 * 	<li>The supplier may be called multiple times if threads race, but only one result is cached.
 * 	<li>The cached value can be <jk>null</jk> if the supplier returns <jk>null</jk>.
 * 	<li>After reset, the next get() will recompute the value.
 * 	<li>This is particularly useful for testing when configuration changes and cached values need to be recalculated.
 * 	<li>Inherits all Optional-like methods from {@link OptionalSupplier} (isPresent(), isEmpty(), map(), orElse(), etc.).
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jm'>{@link org.apache.juneau.commons.utils.Utils#memoizeResettable(Supplier)}
 * 	<li class='jc'>{@link OptionalSupplier} - Base interface providing Optional-like methods
 * </ul>
 *
 * @param <T> The type of value supplied.
 */
public class ResettableSupplier<T> implements OptionalSupplier<T> {
	private final Supplier<T> supplier;
	private final AtomicReference<Optional<T>> cache = new AtomicReference<>();

	/**
	 * Constructor.
	 *
	 * @param supplier The underlying supplier to call when computing values.  Must not be <jk>null</jk>.
	 */
	public ResettableSupplier(Supplier<T> supplier) {
		this.supplier = assertArgNotNull("supplier", supplier);
	}

	/**
	 * Returns the cached value if present, otherwise computes it using the underlying supplier.
	 *
	 * @return The cached or newly computed value.
	 */
	@Override
	public T get() {
		Optional<T> h = cache.get();
		if (h == null) {
			h = opt(supplier.get());
			if (! cache.compareAndSet(null, h)) {
				// Another thread beat us, use their value
				h = cache.get();
			}
		}
		return h.orElse(null);
	}

	/**
	 * Clears the cached value, forcing the next call to {@link #get()} to recompute the value.
	 *
	 * <p>
	 * This method is thread-safe and can be called from multiple threads. After reset,
	 * the next get() call will invoke the underlying supplier to compute a fresh value.
	 */
	public void reset() {
		cache.set(null);
	}

	/**
	 * Sets the cached value directly without invoking the underlying supplier.
	 *
	 * <p>
	 * This method allows you to override the cached value, bypassing the supplier.
	 * Subsequent calls to {@link #get()} will return the set value until {@link #reset()} is called
	 * or the value is set again.
	 *
	 * <p>
	 * This method is thread-safe and is particularly useful for testing when you need to
	 * inject a specific value without invoking the supplier.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a supplier</jc>
	 * 	ResettableSupplier&lt;String&gt; <jv>supplier</jv> = <jk>new</jk> ResettableSupplier&lt;&gt;(() -&gt; <js>"computed"</js>);
	 *
	 * 	<jc>// Set a value directly without invoking the supplier</jc>
	 * 	<jv>supplier</jv>.<jsm>set</jsm>(<js>"injected"</js>);
	 *
	 * 	<jc>// get() returns the injected value</jc>
	 * 	assertEquals(<js>"injected"</js>, <jv>supplier</jv>.<jsm>get</jsm>());
	 *
	 * 	<jc>// Reset clears the cache, next get() will invoke the supplier</jc>
	 * 	<jv>supplier</jv>.<jsm>reset</jsm>();
	 * 	assertEquals(<js>"computed"</js>, <jv>supplier</jv>.<jsm>get</jsm>());
	 * </p>
	 *
	 * @param value The value to cache. Can be <jk>null</jk>.
	 */
	public void set(T value) {
		cache.set(opt(value));
	}
}
