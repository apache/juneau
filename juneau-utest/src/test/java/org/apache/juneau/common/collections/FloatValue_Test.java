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

class FloatValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var v = FloatValue.create();
		assertEquals(0.0f, v.get());
	}

	@Test
	void a02_of() {
		var v = FloatValue.of(3.14f);
		assertEquals(3.14f, v.get(), 0.001f);
	}

	@Test
	void a03_constructor() {
		var v = new FloatValue(2.71f);
		assertEquals(2.71f, v.get(), 0.001f);
	}

	@Test
	void a04_constructor_withNull() {
		var v = new FloatValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Float> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_set() {
		var v = FloatValue.create();
		v.set(5.5f);
		assertEquals(5.5f, v.get(), 0.001f);
	}

	@Test
	void b02_setIfEmpty() {
		var v = new FloatValue(null);
		v.setIfEmpty(1.5f);
		assertEquals(1.5f, v.get(), 0.001f);

		v.setIfEmpty(2.5f);
		assertEquals(1.5f, v.get(), 0.001f); // Should not change
	}

	@Test
	void b03_orElse() {
		var v = new FloatValue(null);
		assertEquals(9.9f, v.orElse(9.9f), 0.001f);

		v.set(4.2f);
		assertEquals(4.2f, v.orElse(9.9f), 0.001f);
	}

	@Test
	void b04_map() {
		var v = FloatValue.of(5.0f);
		var v2 = v.map(x -> x * 2);
		assertEquals(10.0f, v2.get(), 0.001f);
	}

	@Test
	void b05_ifPresent() {
		var v = FloatValue.of(7.5f);
		var sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("7.5", sb.toString());
	}

	@Test
	void b06_isPresent() {
		var v = new FloatValue(null);
		assertFalse(v.isPresent());

		v.set(1.0f);
		assertTrue(v.isPresent());
	}

	@Test
	void b07_isEmpty() {
		var v = new FloatValue(null);
		assertEmpty(v);

		v.set(1.0f);
		assertNotEmpty(v);
	}

	@Test
	void b08_getAndSet() {
		var v = FloatValue.of(1.5f);
		assertEquals(1.5f, v.getAndSet(2.5f), 0.001f);
		assertEquals(2.5f, v.get(), 0.001f);
	}

	@Test
	void b09_getAndUnset() {
		var v = FloatValue.of(3.5f);
		assertEquals(3.5f, v.getAndUnset(), 0.001f);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_trackingRunningAverage() {
		var sum = FloatValue.create();
		var count = FloatValue.create();

		l(1.5f, 2.5f, 3.5f, 4.5f).forEach(x -> {
			sum.set(sum.get() + x);
			count.set(count.get() + 1);
		});

		float average = sum.get() / count.get();
		assertEquals(3.0f, average, 0.001f);
	}

	@Test
	void c02_accumulatingValues() {
		var total = FloatValue.create();

		l(0.1f, 0.2f, 0.3f, 0.4f).forEach(x -> {
			total.set(total.get() + x);
		});

		assertEquals(1.0f, total.get(), 0.001f);
	}

	@Test
	void c03_trackingMinValue() {
		var min = FloatValue.of(Float.MAX_VALUE);

		l(5.5f, 1.2f, 3.8f, 0.9f, 2.1f).forEach(x -> {
			if (x < min.get()) {
				min.set(x);
			}
		});

		assertEquals(0.9f, min.get(), 0.001f);
	}

	@Test
	void c04_multiplierChaining() {
		var multiplier = FloatValue.of(1.0f);

		l(2.0f, 3.0f, 1.5f).forEach(x -> {
			multiplier.set(multiplier.get() * x);
		});

		assertEquals(9.0f, multiplier.get(), 0.001f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is(float, float) - precision-based equality
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_is_exactMatch() {
		var v = FloatValue.of(3.14f);
		assertTrue(v.is(3.14f, 0.0f));
		assertTrue(v.is(3.14f, 0.001f));
	}

	@Test
	void d02_is_withinPrecision() {
		var v = FloatValue.of(3.14f);
		assertTrue(v.is(3.14f, 0.01f));
		assertTrue(v.is(3.15f, 0.02f));
		assertFalse(v.is(3.15f, 0.001f));
	}

	@Test
	void d03_is_nullValue() {
		var v = new FloatValue(null);
		assertFalse(v.is(3.14f, 0.01f));
		assertFalse(v.is(0.0f, 0.01f));
	}

	@Test
	void d04_is_zeroPrecision() {
		var v = FloatValue.of(5.0f);
		assertTrue(v.is(5.0f, 0.0f));
		assertFalse(v.is(5.001f, 0.0f));
	}

	@Test
	void d05_is_largePrecision() {
		var v = FloatValue.of(100.0f);
		assertTrue(v.is(50.0f, 100.0f));
		assertTrue(v.is(150.0f, 100.0f));
		assertFalse(v.is(201.0f, 100.0f));
	}

	@Test
	void d06_is_precisionComparison() {
		// Demonstrates precision-based comparison
		var v = FloatValue.of(3.14159f);
		assertFalse(v.is(3.14f, 0.001f)); // Not within 0.001
		assertTrue(v.is(3.14f, 0.002f));  // Within 0.002
	}

	@Test
	void d07_is_negativeValues() {
		var v = FloatValue.of(-5.5f);
		assertTrue(v.is(-5.5f, 0.0f));
		assertTrue(v.is(-5.4f, 0.2f));
		assertTrue(v.is(-5.6f, 0.2f));
		assertFalse(v.is(-5.0f, 0.4f));
	}

	@Test
	void d08_is_negativePrecision_throwsException() {
		var v = FloatValue.of(3.14f);
		assertThrows(IllegalArgumentException.class, () -> v.is(3.14f, -0.01f));
	}

	@Test
	void d09_is_boundaryValue() {
		var v = FloatValue.of(10.0f);
		assertTrue(v.is(10.5f, 0.5f)); // Exactly at boundary
		assertFalse(v.is(10.51f, 0.5f)); // Just outside boundary
	}

	@Test
	void d10_is_verySmallNumbers() {
		var v = FloatValue.of(0.0001f);
		assertTrue(v.is(0.0002f, 0.00015f));
		assertFalse(v.is(0.0002f, 0.00005f));
	}

	@Test
	void d11_is_afterSet() {
		var v = FloatValue.of(1.0f);
		assertTrue(v.is(1.0f, 0.01f));

		v.set(2.0f);
		assertFalse(v.is(1.0f, 0.01f));
		assertTrue(v.is(2.0f, 0.01f));
	}

	@Test
	void d12_is_scientificNotation() {
		var v = FloatValue.of(1.23e5f);
		assertTrue(v.is(1.23e5f, 0.0f));
		assertTrue(v.is(1.24e5f, 1000.0f));
		assertFalse(v.is(1.24e5f, 100.0f));
	}
}

