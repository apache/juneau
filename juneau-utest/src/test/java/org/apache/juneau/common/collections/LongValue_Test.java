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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class LongValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var v = LongValue.create();
		assertEquals(0L, v.get());
	}

	@Test
	void a02_of() {
		var v = LongValue.of(42L);
		assertEquals(42L, v.get());
	}

	@Test
	void a03_constructor() {
		var v = new LongValue(100L);
		assertEquals(100L, v.get());
	}

	@Test
	void a04_constructor_withNull() {
		var v = new LongValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// getAndIncrement()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_getAndIncrement_basic() {
		var v = LongValue.of(5L);
		assertEquals(5L, v.getAndIncrement());
		assertEquals(6L, v.get());
	}

	@Test
	void b02_getAndIncrement_fromZero() {
		var v = LongValue.create();
		assertEquals(0L, v.getAndIncrement());
		assertEquals(1L, v.get());
	}

	@Test
	void b03_getAndIncrement_multiple() {
		var v = LongValue.of(1L);
		assertEquals(1L, v.getAndIncrement());
		assertEquals(2L, v.getAndIncrement());
		assertEquals(3L, v.getAndIncrement());
		assertEquals(4L, v.get());
	}

	@Test
	void b04_getAndIncrement_withNull() {
		var v = new LongValue(null);
		assertEquals(0L, v.getAndIncrement());
		assertEquals(1L, v.get());
	}

	@Test
	void b05_getAndIncrement_largeValues() {
		var v = LongValue.of(Long.MAX_VALUE - 2);
		assertEquals(Long.MAX_VALUE - 2, v.getAndIncrement());
		assertEquals(Long.MAX_VALUE - 1, v.getAndIncrement());
		assertEquals(Long.MAX_VALUE, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Long> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_set() {
		var v = LongValue.create();
		v.set(50L);
		assertEquals(50L, v.get());
	}

	@Test
	void c02_setIfEmpty() {
		var v = new LongValue(null);
		v.setIfEmpty(10L);
		assertEquals(10L, v.get());

		v.setIfEmpty(20L);
		assertEquals(10L, v.get()); // Should not change
	}

	@Test
	void c03_orElse() {
		var v = new LongValue(null);
		assertEquals(99L, v.orElse(99L));

		v.set(42L);
		assertEquals(42L, v.orElse(99L));
	}

	@Test
	void c04_map() {
		var v = LongValue.of(5L);
		var v2 = v.map(x -> x * 2);
		assertEquals(10L, v2.get());
	}

	@Test
	void c05_ifPresent() {
		var v = LongValue.of(7L);
		var sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("7", sb.toString());
	}

	@Test
	void c06_isPresent() {
		var v = new LongValue(null);
		assertFalse(v.isPresent());

		v.set(1L);
		assertTrue(v.isPresent());
	}

	@Test
	void c07_isEmpty() {
		var v = new LongValue(null);
		assertEmpty(v);

		v.set(1L);
		assertNotEmpty(v);
	}

	@Test
	void c08_getAndSet() {
		var v = LongValue.of(10L);
		assertEquals(10L, v.getAndSet(20L));
		assertEquals(20L, v.get());
	}

	@Test
	void c09_getAndUnset() {
		var v = LongValue.of(15L);
		assertEquals(15L, v.getAndUnset());
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_countingInLambda() {
		var counter = LongValue.create();

		l(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(x -> {
			if (x % 2 == 0) {
				counter.getAndIncrement();
			}
		});

		assertEquals(5L, counter.get());
	}

	@Test
	void d02_trackingBytesProcessed() {
		var bytesProcessed = LongValue.create();

		l(1024L, 2048L, 512L, 4096L).forEach(bytes -> {
			bytesProcessed.set(bytesProcessed.get() + bytes);
		});

		assertEquals(7680L, bytesProcessed.get());
	}

	@Test
	void d03_trackingMaxTimestamp() {
		var maxTimestamp = LongValue.of(0L);

		l(1000L, 5000L, 3000L, 8000L, 2000L).forEach(timestamp -> {
			if (timestamp > maxTimestamp.get()) {
				maxTimestamp.set(timestamp);
			}
		});

		assertEquals(8000L, maxTimestamp.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Increment/Decrement operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_increment() {
		var v = LongValue.of(5L);
		v.increment();
		assertEquals(6L, v.get());
	}

	@Test
	void e02_increment_fromNull() {
		var v = new LongValue(null);
		v.increment();
		assertEquals(1L, v.get());  // null treated as 0, so 0+1 = 1
	}

	@Test
	void e03_increment_largeValue() {
		var v = LongValue.of(Long.MAX_VALUE - 1);
		v.increment();
		assertEquals(Long.MAX_VALUE, v.get());
	}

	@Test
	void e04_decrement() {
		var v = LongValue.of(5L);
		v.decrement();
		assertEquals(4L, v.get());
	}

	@Test
	void e05_decrement_fromNull() {
		var v = new LongValue(null);
		v.decrement();
		assertEquals(-1L, v.get());  // null treated as 0, so 0-1 = -1
	}

	@Test
	void e06_decrement_minValue() {
		var v = LongValue.of(Long.MIN_VALUE + 1);
		v.decrement();
		assertEquals(Long.MIN_VALUE, v.get());
	}

	@Test
	void e07_incrementAndGet() {
		var v = LongValue.of(5L);
		var result = v.incrementAndGet();
		assertEquals(6L, result);
		assertEquals(6L, v.get());
	}

	@Test
	void e08_incrementAndGet_fromNull() {
		var v = new LongValue(null);
		var result = v.incrementAndGet();
		assertEquals(1L, result);
		assertEquals(1L, v.get());
	}

	@Test
	void e09_decrementAndGet() {
		var v = LongValue.of(5L);
		var result = v.decrementAndGet();
		assertEquals(4L, result);
		assertEquals(4L, v.get());
	}

	@Test
	void e10_decrementAndGet_fromNull() {
		var v = new LongValue(null);
		var result = v.decrementAndGet();
		assertEquals(-1L, result);
		assertEquals(-1L, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_add() {
		var v = LongValue.of(10L);
		v.add(5L);
		assertEquals(15L, v.get());
	}

	@Test
	void f02_add_withNullValue() {
		var v = LongValue.of(10L);
		v.add(null);
		assertEquals(10L, v.get());  // null treated as 0, so 10+0 = 10
	}

	@Test
	void f03_add_toNullValue() {
		var v = new LongValue(null);
		v.add(5L);
		assertEquals(5L, v.get());  // null treated as 0, so 0+5 = 5
	}

	@Test
	void f04_add_bothNull() {
		var v = new LongValue(null);
		v.add(null);
		assertEquals(0L, v.get());  // null+null = 0+0 = 0
	}

	@Test
	void f05_add_largeValues() {
		var v = LongValue.of(Long.MAX_VALUE - 100);
		v.add(50L);
		assertEquals(Long.MAX_VALUE - 50, v.get());
	}

	@Test
	void f06_addAndGet() {
		var v = LongValue.of(10L);
		var result = v.addAndGet(5L);
		assertEquals(15L, result);
		assertEquals(15L, v.get());
	}

	@Test
	void f07_addAndGet_withNullValue() {
		var v = LongValue.of(10L);
		var result = v.addAndGet(null);
		assertEquals(10L, result);
		assertEquals(10L, v.get());
	}

	@Test
	void f08_addAndGet_toNullValue() {
		var v = new LongValue(null);
		var result = v.addAndGet(5L);
		assertEquals(5L, result);
		assertEquals(5L, v.get());
	}

	@Test
	void f09_addAndGet_negative() {
		var v = LongValue.of(10L);
		var result = v.addAndGet(-3L);
		assertEquals(7L, result);
		assertEquals(7L, v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Comparison operations
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_is() {
		var v = LongValue.of(42L);
		assertTrue(v.is(42L));
		assertFalse(v.is(43L));
		assertFalse(v.is(null));
	}

	@Test
	void g02_is_withNullValue() {
		var v = new LongValue(null);
		assertFalse(v.is(42L));
		assertTrue(v.is(null));
	}

	@Test
	void g03_isAny() {
		var v = LongValue.of(5L);
		assertTrue(v.isAny(3L, 5L, 7L));
		assertTrue(v.isAny(5L));
		assertFalse(v.isAny(1L, 2L));
		assertFalse(v.isAny());
	}

	@Test
	void g04_isAny_nullValue() {
		var v = new LongValue(null);
		assertFalse(v.isAny(1L, 2L, 3L));
		assertTrue(v.isAny((Long)null));
		assertTrue(v.isAny(1L, null, 2L));
	}

	@Test
	void g05_isAny_largeValues() {
		var v = LongValue.of(Long.MAX_VALUE);
		assertTrue(v.isAny(Long.MAX_VALUE, 1L, 2L));
		assertFalse(v.isAny(Long.MIN_VALUE, 0L, 1L));
	}
}

