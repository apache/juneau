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

class BooleanValue_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var v = BooleanValue.create();
		assertEquals(false, v.get());
	}

	@Test
	void a02_of() {
		var v = BooleanValue.of(true);
		assertEquals(true, v.get());
	}

	@Test
	void a03_constructor() {
		var v = new BooleanValue(true);
		assertEquals(true, v.get());
	}

	@Test
	void a04_constructor_withNull() {
		var v = new BooleanValue(null);
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isTrue() / isNotTrue()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_isTrue_whenTrue() {
		var v = BooleanValue.of(true);
		assertTrue(v.isTrue());
		assertFalse(v.isNotTrue());
	}

	@Test
	void b02_isTrue_whenFalse() {
		var v = BooleanValue.of(false);
		assertFalse(v.isTrue());
		assertTrue(v.isNotTrue());
	}

	@Test
	void b03_isTrue_whenNull() {
		var v = new BooleanValue(null);
		assertFalse(v.isTrue());
		assertTrue(v.isNotTrue());
	}

	@Test
	void b04_threeStates() {
		// Test that BooleanValue supports three states: true, false, null
		var vTrue = BooleanValue.of(true);
		var vFalse = BooleanValue.of(false);
		var vNull = new BooleanValue(null);

		assertTrue(vTrue.get());
		assertFalse(vFalse.get());
		assertNull(vNull.get());

		// Distinguished by isTrue()
		assertTrue(vTrue.isTrue());
		assertFalse(vFalse.isTrue());
		assertFalse(vNull.isTrue());

		// Distinguished by isNotTrue()
		assertFalse(vTrue.isNotTrue());
		assertTrue(vFalse.isNotTrue());
		assertTrue(vNull.isNotTrue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inherited Value<Boolean> methods
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_set() {
		var v = BooleanValue.create();
		v.set(true);
		assertEquals(true, v.get());
	}

	@Test
	void c02_setIfEmpty() {
		var v = new BooleanValue(null);
		v.setIfEmpty(true);
		assertEquals(true, v.get());

		v.setIfEmpty(false);
		assertEquals(true, v.get()); // Should not change
	}

	@Test
	void c03_orElse() {
		var v = new BooleanValue(null);
		assertEquals(true, v.orElse(true));

		v.set(false);
		assertEquals(false, v.orElse(true));
	}

	@Test
	void c04_map() {
		var v = BooleanValue.of(true);
		Value<Boolean> v2 = v.map(x -> !x);
		assertEquals(false, v2.get());
	}

	@Test
	void c05_ifPresent() {
		var v = BooleanValue.of(true);
		var sb = new StringBuilder();
		v.ifPresent(x -> sb.append(x));
		assertEquals("true", sb.toString());
	}

	@Test
	void c06_isPresent() {
		var v = new BooleanValue(null);
		assertFalse(v.isPresent());

		v.set(false);
		assertTrue(v.isPresent());
	}

	@Test
	void c07_isEmpty() {
		var v = new BooleanValue(null);
		assertEmpty(v);

		v.set(true);
		assertNotEmpty(v);
	}

	@Test
	void c08_getAndSet() {
		var v = BooleanValue.of(true);
		assertEquals(true, v.getAndSet(false));
		assertEquals(false, v.get());
	}

	@Test
	void c09_getAndUnset() {
		var v = BooleanValue.of(true);
		assertEquals(true, v.getAndUnset());
		assertNull(v.get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_trackingConditionMet() {
		var found = BooleanValue.of(false);

		l(1, 2, 3, 4, 5).forEach(x -> {
			if (x > 3) {
				found.set(true);
			}
		});

		assertTrue(found.isTrue());
	}

	@Test
	void d02_allConditionsMet() {
		var allValid = BooleanValue.of(true);

		l(2, 4, 6, 8, 10).forEach(x -> {
			if (x % 2 != 0) {
				allValid.set(false);
			}
		});

		assertTrue(allValid.isTrue());
	}

	@Test
	void d03_toggle() {
		var toggle = BooleanValue.of(false);

		// Toggle it 5 times
		for (var i = 0; i < 5; i++) {
			toggle.set(!toggle.get());
		}

		assertTrue(toggle.isTrue());
	}

	@Test
	void d04_nullStateTracking() {
		var state = new BooleanValue(null);

		// Uninitialized state (null)
		assertNull(state.get());
		assertTrue(state.isNotTrue());

		// Initialize to true
		state.set(true);
		assertTrue(state.isTrue());

		// Change to false
		state.set(false);
		assertFalse(state.isTrue());
		assertTrue(state.isNotTrue());

		// Reset to uninitialized (null)
		state.set(null);
		assertNull(state.get());
		assertTrue(state.isNotTrue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// is(Boolean) - equality comparison
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_is_whenTrue() {
		var v = BooleanValue.of(true);
		assertTrue(v.is(true));
		assertFalse(v.is(false));
		assertFalse(v.is(null));
	}

	@Test
	void e02_is_whenFalse() {
		var v = BooleanValue.of(false);
		assertFalse(v.is(true));
		assertTrue(v.is(false));
		assertFalse(v.is(null));
	}

	@Test
	void e03_is_whenNull() {
		var v = new BooleanValue(null);
		assertFalse(v.is(true));
		assertFalse(v.is(false));
		assertTrue(v.is(null));
	}

	@Test
	void e04_is_afterSet() {
		var v = BooleanValue.of(true);
		assertTrue(v.is(true));

		v.set(false);
		assertFalse(v.is(true));
		assertTrue(v.is(false));

		v.set(null);
		assertFalse(v.is(true));
		assertFalse(v.is(false));
		assertTrue(v.is(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isAny(Boolean...) - matching any value
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_isAny_whenTrue() {
		var v = BooleanValue.of(true);
		assertTrue(v.isAny(true, null));
		assertTrue(v.isAny(true, false));
		assertTrue(v.isAny(true));
		assertFalse(v.isAny(false, null));
		assertFalse(v.isAny(false));
		assertFalse(v.isAny());
	}

	@Test
	void f02_isAny_whenFalse() {
		var v = BooleanValue.of(false);
		assertTrue(v.isAny(false, null));
		assertTrue(v.isAny(true, false));
		assertTrue(v.isAny(false));
		assertFalse(v.isAny(true, null));
		assertFalse(v.isAny(true));
		assertFalse(v.isAny());
	}

	@Test
	void f03_isAny_whenNull() {
		var v = new BooleanValue(null);
		assertTrue(v.isAny((Boolean)null, true));
		assertTrue(v.isAny(false, (Boolean)null));
		assertTrue(v.isAny((Boolean)null));
		assertFalse(v.isAny(true, false));
		assertFalse(v.isAny(true));
		assertFalse(v.isAny(false));
		assertFalse(v.isAny());
		
		// Test that null array throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> v.isAny((Boolean[])null));
	}

	@Test
	void f04_isAny_afterSet() {
		var v = BooleanValue.of(true);
		assertTrue(v.isAny(true, false));

		v.set(false);
		assertTrue(v.isAny(true, false));
		assertFalse(v.isAny(true, null));

		v.set(null);
		assertTrue(v.isAny(null, true));
		assertFalse(v.isAny(true, false));
	}

	@Test
	void f05_isAny_allThreeStates() {
		var vTrue = BooleanValue.of(true);
		var vFalse = BooleanValue.of(false);
		var vNull = new BooleanValue(null);

		// All should match when all three states are provided
		assertTrue(vTrue.isAny(true, false, null));
		assertTrue(vFalse.isAny(true, false, null));
		assertTrue(vNull.isAny(true, false, null));
	}
}

