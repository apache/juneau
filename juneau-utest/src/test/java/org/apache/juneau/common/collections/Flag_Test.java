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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Flag_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var a = Flag.create();
		assertFalse(a.isSet());
		assertTrue(a.isUnset());
	}

	@Test
	void a02_of_false() {
		var a = Flag.of(false);
		assertFalse(a.isSet());
		assertTrue(a.isUnset());
	}

	@Test
	void a03_of_true() {
		var a = Flag.of(true);
		assertTrue(a.isSet());
		assertFalse(a.isUnset());
	}

	@Test
	void a04_set() {
		var a = Flag.create();
		var b = a.set();
		assertSame(a, b);
		assertTrue(a.isSet());
		assertFalse(a.isUnset());
	}

	@Test
	void a05_unset() {
		var a = Flag.of(true);
		var b = a.unset();
		assertSame(a, b);
		assertFalse(a.isSet());
		assertTrue(a.isUnset());
	}

	@Test
	void a06_setIf_false() {
		var a = Flag.create();
		a.setIf(false);
		assertFalse(a.isSet());
	}

	@Test
	void a07_setIf_true() {
		var a = Flag.create();
		a.setIf(true);
		assertTrue(a.isSet());
	}

	@Test
	void a08_setIf_logicalOr() {
		// Test that setIf uses logical OR - once set, remains set
		var a = Flag.create();
		a.setIf(false);
		assertFalse(a.isSet());
		a.setIf(true);
		assertTrue(a.isSet());
		a.setIf(false);  // Should remain true
		assertTrue(a.isSet());
	}

	@Test
	void a09_getAndSet() {
		var a = Flag.create();
		assertFalse(a.getAndSet());
		assertTrue(a.isSet());
		assertTrue(a.getAndSet());
		assertTrue(a.isSet());
	}

	@Test
	void a10_getAndUnset() {
		var a = Flag.of(true);
		assertTrue(a.getAndUnset());
		assertFalse(a.isSet());
		assertFalse(a.getAndUnset());
		assertFalse(a.isSet());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Conditional execution tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_ifSet_whenSet() {
		var a = Flag.of(true);
		var b = Flag.create();

		a.ifSet(() -> b.set());

		assertTrue(b.isSet());
	}

	@Test
	void b02_ifSet_whenUnset() {
		var a = Flag.create();
		var b = Flag.create();

		a.ifSet(() -> b.set());

		assertFalse(b.isSet());
	}

	@Test
	void b03_ifNotSet_whenSet() {
		var a = Flag.of(true);
		var b = Flag.create();

		a.ifNotSet(() -> b.set());

		assertFalse(b.isSet());
	}

	@Test
	void b04_ifNotSet_whenUnset() {
		var a = Flag.create();
		var b = Flag.create();

		a.ifNotSet(() -> b.set());

		assertTrue(b.isSet());
	}

	@Test
	void b05_ifSet_chaining() {
		var a = Flag.of(true);
		var b = a.ifSet(() -> {});
		assertSame(a, b);
	}

	@Test
	void b06_ifNotSet_chaining() {
		var a = Flag.create();
		var b = a.ifNotSet(() -> {});
		assertSame(a, b);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Use case scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_initializeOnce() {
		var a = Flag.create();

		// Simulate initializing only once
		for (var i = 0; i < 5; i++) {
			a.ifNotSet(() -> a.set());
		}

		assertTrue(a.isSet());
	}

	@Test
	void c02_lambdaUsage() {
		var a = Flag.create();

		// Simulate using flag in a lambda
		var list = l("a", "b", "c");
		list.forEach(x -> {
			if ("b".equals(x)) {
				a.set();
			}
		});

		assertTrue(a.isSet());
	}

	@Test
	void c03_getAndSet_togglePattern() {
		var a = Flag.create();

		// First time - not set, so do something
		if (!a.getAndSet()) {
			// First execution
			assertTrue(true);
		}

		// Second time - already set, so skip
		if (!a.getAndSet()) {
			// Should not execute
			fail("Should not execute");
		}
	}

	@Test
	void c04_multipleConditions() {
		var a = Flag.create();
		var b = Flag.create();
		var c = Flag.create();

		a.setIf(true);
		b.setIf(false);
		c.setIf(true);

		assertTrue(a.isSet());
		assertFalse(b.isSet());
		assertTrue(c.isSet());

		// Count how many are set
		var d = 0;
		if (a.isSet()) d++;
		if (b.isSet()) d++;
		if (c.isSet()) d++;

		assertEquals(2, d);
	}
}