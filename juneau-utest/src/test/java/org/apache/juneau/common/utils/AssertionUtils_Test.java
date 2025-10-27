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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class AssertionUtils_Test extends TestBase {

	//====================================================================================================
	// assertOneOf(T, T...)
	//====================================================================================================
	@Test
	void a01_assertOneOf() {
		assertEquals("test", assertOneOf("test", "test", "other"));
		assertEquals(123, assertOneOf(123, 123, 456));
		assertEquals("a", assertOneOf("a", "a", "b", "c"));
	}

	@Test
	void a02_assertOneOf_matches() {
		// Exact match
		assertEquals("test", assertOneOf("test", "test"));
		assertEquals(1, assertOneOf(1, 1, 2, 3));
		
		// Match in middle
		assertEquals(2, assertOneOf(2, 1, 2, 3));
		
		// Match at end
		assertEquals(3, assertOneOf(3, 1, 2, 3));
	}

	@Test
	void a03_assertOneOf_nulls() {
		assertNull(assertOneOf(null, null, "test"));
		assertNull(assertOneOf(null, "test", null));
	}

	@Test
	void a04_assertOneOf_fails() {
		AssertionError e = assertThrows(AssertionError.class, () -> assertOneOf("test", "other"));
		assertTrue(e.getMessage().contains("Invalid value specified"));
		assertTrue(e.getMessage().contains("test"));
	}

	@Test
	void a05_assertOneOf_fails_multiple() {
		AssertionError e = assertThrows(AssertionError.class, () -> assertOneOf("test", "a", "b", "c"));
		assertTrue(e.getMessage().contains("Invalid value specified"));
		assertTrue(e.getMessage().contains("test"));
	}

	@Test
	void a06_assertOneOf_numbers() {
		assertEquals(5, assertOneOf(5, 1, 2, 3, 4, 5));
		assertThrows(AssertionError.class, () -> assertOneOf(10, 1, 2, 3, 4, 5));
	}
}

