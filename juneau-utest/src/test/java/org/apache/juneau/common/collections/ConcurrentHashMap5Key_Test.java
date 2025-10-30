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

import static org.apache.juneau.junit.bct.BctAssertions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ConcurrentHashMap5Key_Test extends TestBase {

	//====================================================================================================
	// a - Basic put and get operations
	//====================================================================================================

	@Test
	void a01_basicPutAndGet() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();

		x.put("en", "US", "west", "formal", 1, "Hello");
		x.put("en", "US", "west", "informal", 1, "Hi");
		x.put("fr", "FR", "north", "formal", 1, "Bonjour");

		assertEquals("Hello", x.get("en", "US", "west", "formal", 1));
		assertEquals("Hi", x.get("en", "US", "west", "informal", 1));
		assertEquals("Bonjour", x.get("fr", "FR", "north", "formal", 1));
	}

	@Test
	void a02_getWithNonExistentKey() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();

		x.put("en", "US", "west", "formal", 1, "Hello");

		assertNull(x.get("en", "US", "west", "formal", 999));
		assertNull(x.get("en", "US", "west", "informal", 1));
		assertNull(x.get("en", "US", "east", "formal", 1));
		assertNull(x.get("en", "UK", "west", "formal", 1));
		assertNull(x.get("fr", "US", "west", "formal", 1));
	}

	@Test
	void a03_updateExistingKey() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();

		assertNull(x.put("en", "US", "west", "formal", 1, "Hello"));
		assertEquals("Hello", x.put("en", "US", "west", "formal", 1, "Greetings"));
		assertEquals("Greetings", x.get("en", "US", "west", "formal", 1));
	}

	//====================================================================================================
	// b - Null key validation
	//====================================================================================================

	@Test
	void b01_nullKeys_get() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.get(null, "US", "west", "formal", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", null, "west", "formal", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", null, "formal", 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", "west", null, 1));
		assertThrows(IllegalArgumentException.class, () -> x.get("en", "US", "west", "formal", null));
		assertThrows(IllegalArgumentException.class, () -> x.get(null, null, null, null, null));
	}

	@Test
	void b02_nullKeys_put() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.put(null, "US", "west", "formal", 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", null, "west", "formal", 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", "US", null, "formal", 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", "US", "west", null, 1, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put("en", "US", "west", "formal", null, "value"));
		assertThrows(IllegalArgumentException.class, () -> x.put(null, null, null, null, null, "value"));
	}

	//====================================================================================================
	// c - Key distinctness
	//====================================================================================================

	@Test
	void c01_keyOrdering() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,String,String>();

		x.put("a", "b", "c", "d", "e", "ABCDE");
		x.put("e", "d", "c", "b", "a", "EDCBA");
		x.put("c", "a", "e", "b", "d", "CAEBD");

		assertEquals("ABCDE", x.get("a", "b", "c", "d", "e"));
		assertEquals("EDCBA", x.get("e", "d", "c", "b", "a"));
		assertEquals("CAEBD", x.get("c", "a", "e", "b", "d"));
	}

	//====================================================================================================
	// d - Size operations
	//====================================================================================================

	@Test
	void d01_sizeTracking() {
		var x = new ConcurrentHashMap5Key<String,String,String,String,Integer,String>();

		assertEmpty(x);
		assertEmpty(x);

		x.put("en", "US", "west", "formal", 1, "Hello");
		assertSize(1, x);
		assertNotEmpty(x);

		x.put("en", "US", "west", "informal", 1, "Hi");
		assertSize(2, x);

		x.put("en", "US", "west", "formal", 1, "Greetings"); // Update
		assertSize(2, x);

		x.clear();
		assertEmpty(x);
		assertEmpty(x);
	}
}

