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

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/** Supplier factories: memoizing wrappers moved from the former {@code Utils}. */
@SuppressWarnings({ "java:S1118" // Utility class with static methods only.
})
public class Suppliers {

	private static final String ARG_supplier = "supplier";

	/** Constructor — this class is meant to be subclassed. */
	protected Suppliers() {}

	/** Thread-safe memoizing supplier (computes once, caches). */
	@SuppressWarnings({ "java:S2789" // AtomicReference uses null for "uninitialized"; intentional.
	})
	public static <T> NullableSupplier<T> memoize(Supplier<T> supplier) {
		assertArgNotNull(ARG_supplier, supplier);
		var cache = new AtomicReference<Optional<T>>();
		return () -> {
			var h = cache.get();
			if (h == null) {
				h = Optional.ofNullable(supplier.get());
				if (! cache.compareAndSet(null, h)) h = cache.get();
			}
			return h.orElse(null);
		};
	}

	/** Resettable memoizing supplier. */
	public static <T> Memoizer<T> memoizer(Supplier<T> supplier) {
		assertArgNotNull(ARG_supplier, supplier);
		return new Memoizer<>(supplier);
	}
}
