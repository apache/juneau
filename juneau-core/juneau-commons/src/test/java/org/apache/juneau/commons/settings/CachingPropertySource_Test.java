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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link CachingPropertySource}.
 */
class CachingPropertySource_Test extends TestBase {

	/** Delegate that counts calls per name; names starting with "present." resolve to themselves, "nullval." to an explicit null. */
	private static final class CountingDelegate implements PropertySource {
		final ConcurrentHashMap<String,AtomicInteger> calls = new ConcurrentHashMap<>();
		final AtomicInteger total = new AtomicInteger();

		@Override
		public PropertyLookupResult get(String name) {
			total.incrementAndGet();
			if (name != null)
				calls.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
			if (name == null)
				return PropertyLookupResult.missing();
			if (name.startsWith("present."))
				return PropertyLookupResult.present(o(name));
			if (name.startsWith("nullval."))
				return PropertyLookupResult.present(Optional.<String>empty());
			return PropertyLookupResult.missing();
		}

		int callsFor(String name) {
			var c = calls.get(name);
			return c == null ? 0 : c.get();
		}
	}

	@Test void a01_hitAndMiss_eachServedFromCacheAfterFirstCall() {
		var d = new CountingDelegate();
		var s = CachingPropertySource.of(d);
		for (var i = 0; i < 5; i++) {
			assertEquals("present.a", s.get("present.a").value().orElse(null));
			assertFalse(s.get("absent.a").isPresent());
		}
		assertEquals(1, d.callsFor("present.a"));
		assertEquals(1, d.callsFor("absent.a"));
	}

	@Test void a02_explicitNull_cachedAsPresent() {
		var d = new CountingDelegate();
		var s = CachingPropertySource.of(d);
		for (var i = 0; i < 3; i++) {
			var r = s.get("nullval.a");
			assertTrue(r.isPresent());
			assertTrue(r.value().isEmpty());
		}
		assertEquals(1, d.callsFor("nullval.a"));
	}

	@Test void a03_capReached_newNamesPassThrough_firstSeenStayCached() {
		var d = new CountingDelegate();
		var s = CachingPropertySource.of(d);
		for (var i = 0; i < CachingPropertySource.MAX_ENTRIES; i++)
			s.get("present.n" + i);
		assertEquals(CachingPropertySource.MAX_ENTRIES, s.size());
		// A name beyond the cap is answered correctly, uncached: the delegate is called every time.
		for (var i = 0; i < 3; i++)
			assertEquals("present.extra", s.get("present.extra").value().orElse(null));
		assertEquals(3, d.callsFor("present.extra"));
		assertEquals(CachingPropertySource.MAX_ENTRIES, s.size());
		// A first-seen name is still served from cache.
		s.get("present.n0");
		s.get("present.n0");
		assertEquals(1, d.callsFor("present.n0"));
	}

	@Test void a04_nullName_skipsCache_reachesDelegate() {
		var d = new CountingDelegate();
		var s = CachingPropertySource.of(d);
		assertFalse(s.get(null).isPresent());
		assertFalse(s.get(null).isPresent());
		assertEquals(2, d.total.get());
		assertEquals(0, s.size());
		var thrower = CachingPropertySource.of(name -> {
			if (name == null)
				throw new IllegalStateException("null name");
			return PropertyLookupResult.missing();
		});
		assertThrows(IllegalStateException.class, () -> thrower.get(null));
	}

	@Test void a05_concurrent_firstLookups_andSoftCapBound() throws Exception {
		var threads = 8;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			// Part 1: N threads race on one name; all get an equal result and it is cached afterwards.
			var d1 = new CountingDelegate();
			var s1 = CachingPropertySource.of(d1);
			var start1 = new CountDownLatch(1);
			var futures = new ArrayList<Future<String>>();
			for (var t = 0; t < threads; t++)
				futures.add(pool.submit(() -> {
					start1.await();
					return s1.get("present.race").value().orElse(null);
				}));
			start1.countDown();
			for (var f : futures)
				assertEquals("present.race", f.get(30, TimeUnit.SECONDS));
			var before = d1.callsFor("present.race");
			assertTrue(before >= 1 && before <= threads);
			s1.get("present.race");
			assertEquals(before, d1.callsFor("present.race"), "cached after the race");

			// Part 2: N threads fill the cache at once; the documented soft cap bounds the overshoot by the thread count.
			var d2 = new CountingDelegate();
			var s2 = CachingPropertySource.of(d2);
			var start2 = new CountDownLatch(1);
			var fills = new ArrayList<Future<?>>();
			for (var t = 0; t < threads; t++) {
				var id = t;
				fills.add(pool.submit(() -> {
					start2.await();
					for (var i = 0; i < CachingPropertySource.MAX_ENTRIES * 2; i++)
						s2.get("present.t" + id + "." + i);
					return null;
				}));
			}
			start2.countDown();
			for (var f : fills)
				f.get(60, TimeUnit.SECONDS);
			assertTrue(s2.size() <= CachingPropertySource.MAX_ENTRIES + threads, "size=" + s2.size());
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void a06_of_null_throws() {
		assertThrowsWithMessage(IllegalArgumentException.class, "delegate", () -> CachingPropertySource.of(null));
	}

	@Test void a07_delegate_returnsWrappedSource() {
		PropertySource inner = name -> PropertyLookupResult.missing();
		assertSame(inner, CachingPropertySource.of(inner).delegate());
	}
}
