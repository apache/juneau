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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
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
		LongValue v = new LongValue(100L);
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
		assertTrue(v.isEmpty());
		
		v.set(1L);
		assertFalse(v.isEmpty());
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
		
		Utils.list(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(x -> {
			if (x % 2 == 0) {
				counter.getAndIncrement();
			}
		});
		
		assertEquals(5L, counter.get());
	}

	@Test
	void d02_trackingBytesProcessed() {
		var bytesProcessed = LongValue.create();
		
		Utils.list(1024L, 2048L, 512L, 4096L).forEach(bytes -> {
			bytesProcessed.set(bytesProcessed.get() + bytes);
		});
		
		assertEquals(7680L, bytesProcessed.get());
	}

	@Test
	void d03_trackingMaxTimestamp() {
		var maxTimestamp = LongValue.of(0L);
		
		Utils.list(1000L, 5000L, 3000L, 8000L, 2000L).forEach(timestamp -> {
			if (timestamp > maxTimestamp.get()) {
				maxTimestamp.set(timestamp);
			}
		});
		
		assertEquals(8000L, maxTimestamp.get());
	}
}

