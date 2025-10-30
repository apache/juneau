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

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ConcurrentHashMap2Key_Test extends TestBase {

	//====================================================================================================
	// a - Basic put and get operations
	//====================================================================================================

	@Test
	void a01_basicPutAndGet() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		x.put("user", 123, "Alice");
		x.put("user", 456, "Bob");
		x.put("admin", 123, "Charlie");

		assertEquals("Alice", x.get("user", 123));
		assertEquals("Bob", x.get("user", 456));
		assertEquals("Charlie", x.get("admin", 123));
	}

	@Test
	void a02_getWithNonExistentKey() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		x.put("user", 123, "Alice");

		assertNull(x.get("user", 999));
		assertNull(x.get("admin", 123));
	}

	@Test
	void a03_updateExistingKey() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		assertNull(x.put("user", 123, "Alice"));
		assertEquals("Alice", x.put("user", 123, "AliceUpdated"));
		assertEquals("AliceUpdated", x.get("user", 123));
	}

	//====================================================================================================
	// b - Null key validation
	//====================================================================================================

	@Test
	void b01_nullKey1_get() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.get(null, 123));
	}

	@Test
	void b02_nullKey2_get() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.get("user", null));
	}

	@Test
	void b03_bothKeysNull_get() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.get(null, null));
	}

	@Test
	void b04_nullKey1_put() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.put(null, 123, "value"));
	}

	@Test
	void b05_nullKey2_put() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.put("user", null, "value"));
	}

	@Test
	void b06_bothKeysNull_put() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		assertThrows(IllegalArgumentException.class, () -> x.put(null, null, "value"));
	}

	//====================================================================================================
	// c - Key equality and hashing
	//====================================================================================================

	@Test
	void c01_keyEquality_differentKeyParts() {
		var x = new ConcurrentHashMap2Key<String,String,String>();

		x.put("a", "b", "AB");
		x.put("b", "a", "BA");

		assertEquals("AB", x.get("a", "b"));
		assertEquals("BA", x.get("b", "a"));
	}

	@Test
	void c02_keyEquality_hashCollisions() {
		// Strings with same hash code
		var x = new ConcurrentHashMap2Key<String,String,String>();

		x.put("Aa", "BB", "value1");
		x.put("BB", "Aa", "value2");

		// Despite potential hash collision, should maintain distinct entries
		assertEquals("value1", x.get("Aa", "BB"));
		assertEquals("value2", x.get("BB", "Aa"));
	}

	//====================================================================================================
	// d - Size and containment
	//====================================================================================================

	@Test
	void d01_sizeTracking() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		assertEmpty(x);

		x.put("user", 123, "Alice");
		assertSize(1, x);

		x.put("user", 456, "Bob");
		assertSize(2, x);

		x.put("admin", 123, "Charlie");
		assertSize(3, x);

		// Updating existing key doesn't change size
		x.put("user", 123, "AliceUpdated");
		assertSize(3, x);
	}

	@Test
	void d02_isEmpty() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		assertEmpty(x);

		x.put("user", 123, "Alice");
		assertNotEmpty(x);

		x.clear();
		assertEmpty(x);
	}

	//====================================================================================================
	// e - Thread safety
	//====================================================================================================

	@Test
	void e01_concurrentAccess() throws Exception {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();
		var executor = Executors.newFixedThreadPool(10);

		// Submit 100 concurrent put operations
		var futures = new CompletableFuture[100];
		for (var i = 0; i < 100; i++) {
			final int index = i;
			futures[i] = CompletableFuture.runAsync(() -> {
				x.put("key" + (index % 10), index, "value" + index);
			}, executor);
		}

		// Wait for all to complete
		CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

		// Verify all values are present
		for (var i = 0; i < 100; i++) {
			var value = x.get("key" + (i % 10), i);
			assertEquals("value" + i, value);
		}

		executor.shutdown();
	}

	//====================================================================================================
	// f - Clear operation
	//====================================================================================================

	@Test
	void f01_clear() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		x.put("user", 123, "Alice");
		x.put("user", 456, "Bob");
		x.put("admin", 123, "Charlie");

		assertSize(3, x);

		x.clear();

		assertEmpty(x);
		assertNull(x.get("user", 123));
		assertNull(x.get("user", 456));
		assertNull(x.get("admin", 123));
	}

	//====================================================================================================
	// g - Different key types
	//====================================================================================================

	@Test
	void g01_integerKeys() {
		var x = new ConcurrentHashMap2Key<Integer,Integer,String>();

		x.put(1, 2, "1:2");
		x.put(2, 1, "2:1");

		assertEquals("1:2", x.get(1, 2));
		assertEquals("2:1", x.get(2, 1));
		assertNull(x.get(1, 1));
	}

	@Test
	void g02_mixedKeyTypes() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		x.put("string", 123, "mixed1");
		x.put("another", 456, "mixed2");

		assertEquals("mixed1", x.get("string", 123));
		assertEquals("mixed2", x.get("another", 456));
	}

	//====================================================================================================
	// h - Edge cases
	//====================================================================================================

	@Test
	void h01_emptyMap() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		assertEmpty(x);
		assertEmpty(x);
	}

	@Test
	void h02_singleEntry() {
		var x = new ConcurrentHashMap2Key<String,Integer,String>();

		x.put("only", 1, "single");

		assertSize(1, x);
		assertEquals("single", x.get("only", 1));
		assertNull(x.get("only", 2));
	}
}

