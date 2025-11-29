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

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Utils_Test extends TestBase {

	//====================================================================================================
	// nn(Object)
	//====================================================================================================
	@Test
	void a01_nn_single() {
		assertTrue(nn("test"));
		assertTrue(nn(123));
		assertTrue(nn(new Object()));
		assertTrue(nn(""));
		assertTrue(nn(0));
		assertTrue(nn(false));

		assertFalse(nn(null));
	}

	//====================================================================================================
	// f(String, Object...)
	//====================================================================================================
	@Test
	void a02_f() {
		// Basic formatting
		assertEquals("Hello John, you have 5 items", f("Hello %s, you have %d items", "John", 5));
		assertEquals("Hello world", f("Hello %s", "world"));

		// Floating point
		assertEquals("Price: $19.99", f("Price: $%.2f", 19.99));
		assertEquals("Value: 3.14", f("Value: %.2f", 3.14159));

		// Multiple arguments
		assertEquals("Name: John, Age: 30, Salary: $50000.00",
			f("Name: %s, Age: %d, Salary: $%.2f", "John", 30, 50000.0));

		// Null handling
		assertEquals("Value: null", f("Value: %s", (String)null));
		assertThrows(IllegalArgumentException.class, ()->f(null, "test"));
		assertEquals("test", f("test"));
	}

	//====================================================================================================
	// fs(String, Object...)
	//====================================================================================================
	@Test
	void a03_fs() {
		// Basic supplier
		var supplier = fs("Hello %s, you have %d items", "John", 5);
		assertNotNull(supplier);
		assertEquals("Hello John, you have 5 items", supplier.get());

		// Lazy evaluation - format only when get() is called
		var lazySupplier = fs("Price: $%.2f", 19.99);
		assertEquals("Price: $19.99", lazySupplier.get());

		// Multiple calls return same result
		var supplier2 = fs("Value: %s", "test");
		assertEquals("Value: test", supplier2.get());
		assertEquals("Value: test", supplier2.get());

		// Null handling
		var nullSupplier = fs(null, "test");
		assertThrows(IllegalArgumentException.class, ()->nullSupplier.get());

		// Empty pattern
		var emptySupplier = fs("");
		assertEquals("", emptySupplier.get());
	}
}

