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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link VarResolver#resolveSupplier(String)} and {@link VarResolverSession#resolveSupplier(String)}.
 *
 * <p>
 * Verifies the two threadsafety contracts:
 * <ul>
 * 	<li>{@link VarResolver#resolveSupplier(String)} — fresh session per {@code .get()}, threadsafe.
 * 	<li>{@link VarResolverSession#resolveSupplier(String)} — session-bound, not threadsafe.
 * </ul>
 */
class VarResolver_ResolveSupplier_Test extends TestBase {

	public static class CounterVar extends SimpleVar {
		public CounterVar() { super("C"); }
		@Override
		public String resolve(VarResolverSession session, String arg) {
			var c = session.getBean(AtomicInteger.class).orElse(null);
			if (c == null) return arg;
			return arg + "-" + c.incrementAndGet();
		}
	}

	@Test void a01_supplierResolvesEachCall() {
		var vr = VarResolver.create().vars(CounterVar.class).build();
		var counter = new AtomicInteger();
		vr.addBean(AtomicInteger.class, counter);
		var sup = vr.resolveSupplier("$C{x}");
		assertEquals("x-1", sup.get());
		assertEquals("x-2", sup.get());
		assertEquals("x-3", sup.get());
	}

	@Test void a02_supplierForLiteralReturnsConstant() {
		var vr = VarResolver.create().build();
		var sup = vr.resolveSupplier("plain text");
		assertEquals("plain text", sup.get());
		assertEquals("plain text", sup.get());
	}

	@Test void a03_sessionSupplierShareesSession() {
		var vr = VarResolver.create().vars(CounterVar.class).build();
		var counter = new AtomicInteger();
		var s = vr.createSession();
		s.bean(AtomicInteger.class, counter);
		var sup = s.resolveSupplier("$C{x}");
		assertEquals("x-1", sup.get());
		assertEquals("x-2", sup.get());
	}

	/**
	 * Cross-thread test: {@link VarResolver#resolveSupplier(String)} returns a Supplier whose
	 * {@code .get()} is safe to call concurrently from many threads. Verifies no shared mutable
	 * session state surfaces (each {@code .get()} opens a fresh session).
	 */
	@Test void a04_supplierIsThreadsafe() throws Exception {
		var vr = VarResolver.create().vars(CounterVar.class).build();
		var counter = new AtomicInteger();
		// Use the resolver-level bean store so every session sees the same counter.
		vr.addBean(AtomicInteger.class, counter);
		var sup = vr.resolveSupplier("$C{x}");

		var threadCount = 16;
		var iterations = 100;
		var pool = Executors.newFixedThreadPool(threadCount);
		try {
			var latch = new CountDownLatch(threadCount);
			var seen = ConcurrentHashMap.<String>newKeySet();
			for (var t = 0; t < threadCount; t++) {
				pool.submit(() -> {
					try {
						for (var i = 0; i < iterations; i++)
							seen.add(sup.get());
					} finally {
						latch.countDown();
					}
				});
			}
			assertTrue(latch.await(15, TimeUnit.SECONDS), "Threads did not complete in time");
			// Every call should produce a unique counter value, never duplicated, never null.
			assertEquals(threadCount * iterations, seen.size(), "Counter values should be unique across threads");
		} finally {
			pool.shutdownNow();
		}
	}
}
