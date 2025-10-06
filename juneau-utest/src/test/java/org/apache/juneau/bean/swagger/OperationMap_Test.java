// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.juneau.TestUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link OperationMap}.
 */
class OperationMap_Test extends TestBase {

	/**
	 * Test method for operation ordering.
	 */
	@Test void a01_operationOrdering() {
		var map = operationMap();

		// Add operations in random order
		map
			.append("custom", operation())
			.append("post", operation())
			.append("delete", operation())
			.append("get", operation())
			.append("options", operation())
			.append("head", operation())
			.append("patch", operation())
			.append("put", operation());

		// Verify ordering.
		assertList(map.keySet(), "get", "put", "post", "delete", "options", "head", "patch", "custom");
	}

	/**
	 * Test method for case-insensitive ordering.
	 */
	@Test void a02_caseInsensitiveOrdering() {
		var map = operationMap();

		// Add operations with different cases
		map.append("DELETE", operation()).append("Get", operation()).append("POST", operation()).append("put", operation());

		// Verify ordering is case-insensitive - all keys should be normalized to lowercase
		assertList(map.keySet(), "get", "put", "post", "delete");
	}

	/**
	 * Test method for custom methods.
	 */
	@Test void a04_customMethods() {
		var map = operationMap();

		// Add standard methods
		map.put("get", operation().setSummary("a"));
		map.put("post", operation().setSummary("b"));

		// Add custom methods
		map.put("custom1", operation().setSummary("c"));
		map.put("custom2", operation().setSummary("d"));

		// Verify custom methods come after standard methods
		assertBeans(map, "key,value{summary}", "get,{a}", "post,{b}", "custom1,{c}", "custom2,{d}");
	}

	/**
	 * Test method for empty map.
	 */
	@Test void a05_emptyMap() {
		var map = operationMap();
		assertTrue(map.isEmpty());
		assertEquals(0, map.size());
	}

	/**
	 * Test method for duplicate keys.
	 */
	@Test void a06_duplicateKeys() {
		var map = operationMap();

		map.put("get", operation().setSummary("a"));
		map.put("get", operation().setSummary("b"));

		// Verify the second operation overwrites the first
		assertBeans(map, "key,value{summary}", "get,{b}");
	}

	/**
	 * Test method for null values.
	 */
	@Test void a07_nullValues() {
		var map = operationMap();

		// Should not throw exception when adding null value
		assertDoesNotThrow(() -> map.put("get", null));
		assertTrue(map.containsKey("get"));
		assertNull(map.get("get"));
	}

	/**
	 * Test method for serialization.
	 */
	@Test void a08_serialization() {
		var map = operationMap();
		map.put("get", operation().setSummary("a"));
		map.put("post", operation().setSummary("b"));

		// Test that it can be serialized to JSON
		var json = json(map);
		assertNotNull(json);
		assertEquals("{get:{summary:'a'},post:{summary:'b'}}", json);
	}

	/**
	 * Test method for deserialization.
	 */
	@Test void a09_deserialization() {
		var json = "{get:{summary:'a'},post:{summary:'b'}}";
		var map = json(json, OperationMap.class);
		assertBeans(map, "key,value{summary}", "get,{a}", "post,{b}");
	}

	/**
	 * Test method for iteration order.
	 */
	@Test void a10_iterationOrder() {
		var map = operationMap();
		map.put("delete", operation());
		map.put("get", operation());
		map.put("post", operation());
		map.put("put", operation());

		assertList(map.keySet(), "get", "put", "post", "delete");
	}

	/**
	 * Test method for key normalization in put method.
	 */
	@Test void a11_keyNormalization() {
		var map = operationMap();

		// Test that put() normalizes keys to lowercase
		map.put("GET", operation().setSummary("a"));
		map.put("Post", operation().setSummary("b"));
		map.put("DELETE", operation().setSummary("c"));


		// Verify that keys are stored in lowercase
		assertTrue(map.containsKey("get"));
		assertTrue(map.containsKey("post"));
		assertTrue(map.containsKey("delete"));

		// Verify that original case keys are not present (TreeMap with custom comparator may be case-insensitive)
		// Since our comparator normalizes keys, containsKey() may also be case-insensitive
		// Let's verify the actual keys in the map instead
		var keys = new ArrayList<>(map.keySet());
		assertTrue(keys.contains("get"));
		assertTrue(keys.contains("post"));
		assertTrue(keys.contains("delete"));
		assertFalse(keys.contains("GET"));
		assertFalse(keys.contains("Post"));
		assertFalse(keys.contains("DELETE"));

		// Verify that we can retrieve using lowercase keys
		assertBeans(map, "value{summary}", "{a}", "{b}", "{c}");
	}
}
