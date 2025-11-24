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

class DoubleValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var v = DoubleValue.create();
		assertEquals(0.0, v.get());
	}

	@Test
	void a02_of() {
		var v = DoubleValue.of(3.14159);
		assertEquals(3.14159, v.get(), 0.00001);
	}

	@Test
	void a03_constructor() {
		var v = new DoubleValue(2.71828);
		assertEquals(2.71828, v.get(), 0.00001);
	}

	@Test
	void a04_constructor_withNull() {
		var v = new DoubleValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Double> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_set() {
		var v = DoubleValue.create();
		v.set(5.5);
		assertEquals(5.5, v.get(), 0.00001);
	}

	@Test
	void b02_setIfEmpty() {
		var v = new DoubleValue(null);
		v.setIfEmpty(1.5);
		assertEquals(1.5, v.get(), 0.00001);

		v.setIfEmpty(2.5);
		assertEquals(1.5, v.get(), 0.00001); // Should not change
	}

	@Test
	void b03_orElse() {
		var v = new DoubleValue(null);
		assertEquals(9.9, v.orElse(9.9), 0.00001);

		v.set(4.2);
		assertEquals(4.2, v.orElse(9.9), 0.00001);
	}

	@Test
	void b04_map() {
		var v = DoubleValue.of(5.0);
		var v2 = v.map(x -> x * 2);
		assertEquals(10.0, v2.get(), 0.00001);
	}

	@Test
	void b05_ifPresent() {
		var v = DoubleValue.of(7.5);
		var sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("7.5", sb.toString());
	}

	@Test
	void b06_isPresent() {
		var v = new DoubleValue(null);
		assertFalse(v.isPresent());

		v.set(1.0);
		assertTrue(v.isPresent());
	}

	@Test
	void b07_isEmpty() {
		var v = new DoubleValue(null);
		assertEmpty(v);

		v.set(1.0);
		assertNotEmpty(v);
	}

	@Test
	void b08_getAndSet() {
		var v = DoubleValue.of(1.5);
		assertEquals(1.5, v.getAndSet(2.5), 0.00001);
		assertEquals(2.5, v.get(), 0.00001);
	}

	@Test
	void b09_getAndUnset() {
		var v = DoubleValue.of(3.5);
		assertEquals(3.5, v.getAndUnset(), 0.00001);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_trackingPreciseSum() {
		var sum = DoubleValue.create();

		l(1.111, 2.222, 3.333, 4.444).forEach(x -> {
			sum.set(sum.get() + x);
		});

		assertEquals(11.11, sum.get(), 0.00001);
	}

	@Test
	void c02_trackingStatistics() {
		var sum = DoubleValue.create();
		var count = DoubleValue.create();

		l(10.5, 20.3, 15.7, 30.1).forEach(x -> {
			sum.set(sum.get() + x);
			count.set(count.get() + 1);
		});

		double average = sum.get() / count.get();
		assertEquals(19.15, average, 0.00001);
	}

	@Test
	void c03_trackingMaxValue() {
		var max = DoubleValue.of(Double.MIN_VALUE);

		l(5.5, 12.3, 3.8, 20.1, 1.2).forEach(x -> {
			if (x > max.get()) {
				max.set(x);
			}
		});

		assertEquals(20.1, max.get(), 0.00001);
	}

	@Test
	void c04_compoundInterestCalculation() {
		var principal = DoubleValue.of(1000.0);
		double rate = 0.05; // 5% interest

		// Apply interest 3 times
		for (var i = 0; i < 3; i++) {
			principal.set(principal.get() * (1 + rate));
		}

		assertEquals(1157.625, principal.get(), 0.001);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is(double, double) - precision-based equality
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_is_exactMatch() {
		var v = DoubleValue.of(3.14159);
		assertTrue(v.is(3.14159, 0.0));
		assertTrue(v.is(3.14159, 0.00001));
	}

	@Test
	void d02_is_withinPrecision() {
		var v = DoubleValue.of(3.14159);
		assertTrue(v.is(3.14, 0.01));
		assertTrue(v.is(3.15, 0.01));
		assertFalse(v.is(3.14, 0.001));
		assertFalse(v.is(3.15, 0.001));
	}

	@Test
	void d03_is_nullValue() {
		var v = new DoubleValue(null);
		assertFalse(v.is(3.14, 0.01));
		assertFalse(v.is(0.0, 0.01));
	}

	@Test
	void d04_is_zeroPrecision() {
		var v = DoubleValue.of(5.0);
		assertTrue(v.is(5.0, 0.0));
		assertFalse(v.is(5.0001, 0.0));
	}

	@Test
	void d05_is_largePrecision() {
		var v = DoubleValue.of(100.0);
		assertTrue(v.is(50.0, 100.0));
		assertTrue(v.is(150.0, 100.0));
		assertFalse(v.is(201.0, 100.0));
	}

	@Test
	void d06_is_floatingPointRoundingError() {
		var v = DoubleValue.of(0.1 + 0.2); // Actually 0.30000000000000004
		assertFalse(v.is(0.3, 0.0)); // Exact match fails
		assertTrue(v.is(0.3, 0.000001)); // But within small precision
	}

	@Test
	void d07_is_negativeValues() {
		var v = DoubleValue.of(-5.5);
		assertTrue(v.is(-5.5, 0.0));
		assertTrue(v.is(-5.4, 0.2));
		assertTrue(v.is(-5.6, 0.2));
		assertFalse(v.is(-5.0, 0.4));
	}

	@Test
	void d08_is_negativePrecision_throwsException() {
		var v = DoubleValue.of(3.14);
		assertThrows(IllegalArgumentException.class, () -> v.is(3.14, -0.01));
	}

	@Test
	void d09_is_boundaryValue() {
		var v = DoubleValue.of(10.0);
		assertTrue(v.is(10.5, 0.5)); // Exactly at boundary
		assertFalse(v.is(10.51, 0.5)); // Just outside boundary
	}

	@Test
	void d10_is_verySmallNumbers() {
		var v = DoubleValue.of(0.0000001);
		assertTrue(v.is(0.0000002, 0.00000015));
		assertFalse(v.is(0.0000002, 0.00000005));
	}

	@Test
	void d11_is_afterSet() {
		var v = DoubleValue.of(1.0);
		assertTrue(v.is(1.0, 0.01));

		v.set(2.0);
		assertFalse(v.is(1.0, 0.01));
		assertTrue(v.is(2.0, 0.01));
	}

	@Test
	void d12_is_scientificNotation() {
		var v = DoubleValue.of(1.23e10);
		assertTrue(v.is(1.23e10, 0.0));
		assertTrue(v.is(1.24e10, 1e8));
		assertFalse(v.is(1.24e10, 1e7));
	}
}

