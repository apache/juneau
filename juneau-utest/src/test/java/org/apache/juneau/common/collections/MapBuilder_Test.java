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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MapBuilder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = MapBuilder.create(String.class, Integer.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.build();

		assertMap(map, "a=1");
	}

	@Test
	void a03_addMultiple() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.add("c", 3)
			.build();

		assertMap(map, "a=1", "b=2", "c=3");
	}

	@Test
	void a04_addAll() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);
		existing.put("y", 20);

		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(existing)
			.add("b", 2)
			.build();

		assertMap(map, "a=1", "x=10", "y=20", "b=2");
	}

	@Test
	void a05_addAllNull() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(null)
			.add("b", 2)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Add pairs
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_addPairs() {
		var map = MapBuilder.create(String.class, String.class)
			.addPairs("host", "localhost", "port", "8080", "protocol", "https")
			.build();

		assertMap(map, "host=localhost", "port=8080", "protocol=https");
	}

	@Test
	void b02_addPairs_oddNumber() {
		var b = MapBuilder.create(String.class, String.class);
		assertThrows(IllegalArgumentException.class, () -> b.addPairs("a", "b", "c"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sorting
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_sorted_naturalOrder() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.build();

		assertSize(3, map);
		assertList(map.keySet(), "a", "b", "c");
		assertTrue(map instanceof TreeMap);
	}

	@Test
	void c02_sorted_customComparator() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("bb", 2)
			.add("ccc", 3)
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(map.keySet(), "a", "bb", "ccc");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sparse mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_sparse_empty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sparse()
			.build();

		assertNull(map);
	}

	@Test
	void d02_sparse_notEmpty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.sparse()
			.build();

		assertNotNull(map);
		assertSize(1, map);
	}

	@Test
	void d03_notSparse_empty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_unmodifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.unmodifiable()
			.build();

		assertSize(2, map);
		assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));
	}

	@Test
	void e02_modifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.build();

		map.put("b", 2);
		assertSize(2, map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_copy() {
		var original = new LinkedHashMap<String,Integer>();
		original.put("a", 1);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(original)
			.add("b", 2)
			.copy()
			.add("c", 3)
			.build();

		assertSize(3, map);
		assertSize(2, original);  // Original has "a" and "b" added before copy()
		assertNotSame(original, map);  // After copy(), they're different maps
	}

	@Test
	void f02_noCopy() {
		var original = new LinkedHashMap<String,Integer>();
		original.put("a", 1);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(original)
			.add("b", 2)
			.build();

		assertSize(2, map);
		assertSize(2, original);  // Original modified
		assertSame(original, map);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Complex scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_multipleOperations() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);
		existing.put("y", 20);

		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.addAll(existing)
			.add("b", 2)
			.sorted()
			.build();

		assertSize(4, map);
		// Should be sorted by key
		assertList(map.keySet(), "a", "b", "x", "y");
	}

	@Test
	void g02_sortedAndUnmodifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.unmodifiable()
			.build();

		assertSize(3, map);
		assertList(map.keySet(), "a", "b", "c");
		assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
	}

	@Test
	void g03_sparseAndSorted() {
		var map1 = MapBuilder.create(String.class, Integer.class)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.sparse()
			.build();

		assertNotNull(map1);
		assertSize(3, map1);

		var map2 = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.sparse()
			.build();

		assertNull(map2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_buildEmptyMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	@Test
	void h02_addNullKey() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add(null, 1)
			.add("a", 2)
			.build();

		assertMap(map, "<null>=1", "a=2");
	}

	@Test
	void h03_addNullValue() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", null)
			.add("c", 3)
			.build();

		assertMap(map, "a=1", "b=<null>", "c=3");
	}

	@Test
	void h04_duplicateKeys() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("a", 2)  // Overwrites first value
			.add("a", 3)  // Overwrites again
			.build();

		assertSize(1, map);  // Only one entry for key "a"
		assertMap(map, "a=3");  // Last value wins
	}

	@Test
	void h05_toExistingMap() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);

		var map = MapBuilder.create(String.class, Integer.class)
			.to(existing)
			.add("y", 20)
			.build();

		assertSize(2, map);
		assertMap(map, "x=10", "y=20");
		assertSame(existing, map);
	}
}