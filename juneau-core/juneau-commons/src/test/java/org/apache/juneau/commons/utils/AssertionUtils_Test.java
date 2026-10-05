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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
class AssertionUtils_Test extends TestBase {

	//====================================================================================================
	// assertOneOf(T, T...)
	//====================================================================================================
	@Test
	void a009_assertOneOf() {
		// Should return actual when it matches
		assertEquals("test", assertOneOf("test", "test", "other"));
		assertEquals(123, assertOneOf(123, 123, 456));
		assertEquals("a", assertOneOf("a", "a", "b", "c"));

		// Exact match
		assertEquals("test", assertOneOf("test", "test"));
		assertEquals(1, assertOneOf(1, 1, 2, 3));

		// Match in middle
		assertEquals(2, assertOneOf(2, 1, 2, 3));

		// Match at end
		assertEquals(3, assertOneOf(3, 1, 2, 3));

		// Should handle nulls
		assertNull(assertOneOf(null, null, "test"));
		assertNull(assertOneOf(null, "test", null));

		// Should return same instance
		var value = "test";
		var result = assertOneOf(value, "test", "other");
		assertSame(value, result);

		// Should work with objects
		var obj1 = new Object();
		var obj2 = new Object();
		var result2 = assertOneOf(obj1, obj1, obj2);
		assertSame(obj1, result2);

		// Should throw when value doesn't match
		assertThrowsWithMessage(AssertionError.class, fixedSizeList("Invalid value specified", "test"), () -> {
			assertOneOf("test", "other");
		});

		assertThrowsWithMessage(AssertionError.class, fixedSizeList("Invalid value specified", "test"), () -> {
			assertOneOf("test", "a", "b", "c");
		});

		assertThrows(AssertionError.class, () -> assertOneOf(10, 1, 2, 3, 4, 5));

		// Should throw with empty expected
		assertThrowsWithMessage(AssertionError.class, "Invalid value specified", () -> {
			assertOneOf("test");
		});
	}
}
