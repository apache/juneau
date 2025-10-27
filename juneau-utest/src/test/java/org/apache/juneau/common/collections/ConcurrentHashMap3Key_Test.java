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
import org.junit.jupiter.api.*;

class ConcurrentHashMap3Key_Test extends TestBase {

	//====================================================================================================
	// a - Basic put and get operations
	//====================================================================================================

	@Test
	void a01_basicPutAndGet() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();

		x.put("en", "US", 1, "Hello");
		x.put("en", "US", 2, "Goodbye");
		x.put("fr", "FR", 1, "Bonjour");

		assertEquals("Hello", x.get("en", "US", 1));
		assertEquals("Goodbye", x.get("en", "US", 2));
		assertEquals("Bonjour", x.get("fr", "FR", 1));
	}

	@Test
	void a02_getWithNonExistentKey() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();

		x.put("en", "US", 1, "Hello");

		assertNull(x.get("en", "US", 999));
		assertNull(x.get("en", "UK", 1));
		assertNull(x.get("fr", "US", 1));
	}

	@Test
	void a03_updateExistingKey() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();

		assertNull(x.put("en", "US", 1, "Hello"));
		assertEquals("Hello", x.put("en", "US", 1, "Hi"));
		assertEquals("Hi", x.get("en", "US", 1));
	}

	//====================================================================================================
	// b - Null key validation
	//====================================================================================================

	@Test
	void b01_nullKeys_get() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.get(null, "US", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", null, 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", null));
		assertThrows(IllegalArgumentException.class, () -> x.get(null, null, null));
	}

	@Test
	void b02_nullKeys_put() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.put(null, "US", 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", null, 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", "US", null, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put(null, null, null, "value"));
	}

	//====================================================================================================
	// c - Key distinctness
	//====================================================================================================

	@Test
	void c01_keyOrdering() {
		var x = new ConcurrentHashMap3Key<String,String,String,String>();

		x.put("a", "b", "c", "ABC");
		x.put("b", "c", "a", "BCA");
		x.put("c", "a", "b", "CAB");

		assertEquals("ABC", x.get("a", "b", "c"));
		assertEquals("BCA", x.get("b", "c", "a"));
		assertEquals("CAB", x.get("c", "a", "b"));
	}

	//====================================================================================================
	// d - Size operations
	//====================================================================================================

	@Test
	void d01_sizeTracking() {
		var x = new ConcurrentHashMap3Key<String,String,Integer,String>();

		assertEquals(0, x.size());
		assertTrue(x.isEmpty());

		x.put("en", "US", 1, "Hello");
		assertEquals(1, x.size());
		assertFalse(x.isEmpty());

		x.put("en", "US", 2, "Goodbye");
		assertEquals(2, x.size());

		x.put("en", "US", 1, "Hi"); // Update
		assertEquals(2, x.size());

		x.clear();
		assertEquals(0, x.size());
		assertTrue(x.isEmpty());
	}
}

