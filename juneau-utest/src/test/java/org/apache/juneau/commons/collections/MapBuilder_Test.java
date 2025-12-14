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
package org.apache.juneau.commons.collections;

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

		// Without ordered(), order is not guaranteed (HashMap)
		assertSize(4, map);
		assertEquals(1, map.get("a"));
		assertEquals(10, map.get("x"));
		assertEquals(20, map.get("y"));
		assertEquals(2, map.get("b"));
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
	// Ordered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c00_ordered_preservesInsertionOrder() {
		var map = MapBuilder.create(String.class, Integer.class)
			.ordered()
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.build();

		assertSize(3, map);
		assertTrue(map instanceof LinkedHashMap);
		// With ordered(), insertion order is preserved
		var keys = new ArrayList<>(map.keySet());
		assertEquals("c", keys.get(0));
		assertEquals("a", keys.get(1));
		assertEquals("b", keys.get(2));
	}

	@Test
	void c00b_ordered_defaultIsHashMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.build();

		assertSize(2, map);
		// Without ordered(), should be HashMap (unordered)
		assertTrue(map instanceof HashMap);
		assertFalse(map instanceof LinkedHashMap);
		// Verify it's not ordered by checking key order is not preserved
		// (HashMap doesn't guarantee order, so we can't rely on it)
		assertEquals(1, map.get("a"));
		assertEquals(2, map.get("b"));
	}

	@Test
	void c00c_ordered_boolean() {
		var map1 = MapBuilder.create(String.class, Integer.class)
			.ordered(true)
			.add("a", 1)
			.build();
		assertTrue(map1 instanceof LinkedHashMap);

		var map2 = MapBuilder.create(String.class, Integer.class)
			.ordered(false)
			.add("a", 1)
			.build();
		assertTrue(map2 instanceof HashMap);
	}

	@Test
	void c00d_ordered_lastOneWins() {
		// If ordered() is called first, then sorted(), sorted() wins
		var map1 = MapBuilder.create(String.class, Integer.class)
			.ordered()
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.build();
		assertTrue(map1 instanceof TreeMap);
		assertList(map1.keySet(), "a", "b", "c");

		// If sorted() is called first, then ordered(), ordered() wins
		var map2 = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.ordered()
			.build();
		assertTrue(map2 instanceof LinkedHashMap);
		var keys2 = new ArrayList<>(map2.keySet());
		assertEquals("c", keys2.get(0));
		assertEquals("a", keys2.get(1));
		assertEquals("b", keys2.get(2));
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
	void g01b_multipleOperations_withOrdered() {
		var existing = new LinkedHashMap<String,Integer>();
		existing.put("x", 10);
		existing.put("y", 20);

		var map = MapBuilder.create(String.class, Integer.class)
			.ordered()
			.add("a", 1)
			.addAll(existing)
			.add("b", 2)
			.build();

		assertSize(4, map);
		assertTrue(map instanceof LinkedHashMap);
		// With ordered(), insertion order is preserved
		var keys = new ArrayList<>(map.keySet());
		assertEquals("a", keys.get(0));
		assertEquals("x", keys.get(1));
		assertEquals("y", keys.get(2));
		assertEquals("b", keys.get(3));
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

	//-----------------------------------------------------------------------------------------------------------------
	// Filtering
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_filtered_customPredicate() {
		var map = MapBuilder.create(String.class, String.class)
			.filtered((k, v) -> v != null && !v.equals(""))
			.add("a", "foo")
			.add("b", null)     // Not added
			.add("c", "")       // Not added
			.add("d", "bar")
			.build();

		assertMap(map, "a=foo", "d=bar");
	}

	@Test
	void i02_filtered_defaultFilter() {
		var map = MapBuilder.create(String.class, Object.class)
			.filtered()
			.add("name", "John")
			.add("age", -1)              // Not added
			.add("enabled", false)       // Not added
			.add("tags", new String[0])  // Not added
			.add("emptyMap", new LinkedHashMap<>())  // Not added
			.add("emptyList", new ArrayList<>())     // Not added
			.add("value", 42)
			.build();

		assertMap(map, "name=John", "value=42");
	}

	@Test
	void i03_filtered_rejectsValue() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null && v > 0)
			.add("a", 1)
			.add("b", -1)  // Not added
			.add("c", 0)   // Not added
			.add("d", 2)
			.build();

		assertMap(map, "a=1", "d=2");
	}

	@Test
	void i04_add_withFilter() {
		var map = MapBuilder.create(String.class, String.class)
			.filtered((k, v) -> v != null && v.length() > 2)
			.add("a", "foo")   // Added
			.add("b", "ab")    // Not added (length <= 2)
			.add("c", "bar")   // Added
			.build();

		assertMap(map, "a=foo", "c=bar");
	}

	@Test
	void i05_addAll_withFilter() {
		var existing = new LinkedHashMap<String,String>();
		existing.put("x", "longvalue");
		existing.put("y", "ab");  // Will be filtered out
		existing.put("z", "another");

		var map = MapBuilder.create(String.class, String.class)
			.filtered((k, v) -> v != null && v.length() > 2)
			.addAll(existing)
			.build();

		assertMap(map, "x=longvalue", "z=another");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Key/Value Functions
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_keyValueFunctions_notSet() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.build();

		assertMap(map, "a=1");
	}

	@Test
	void j02_keyValueFunctions_withValueFunction() {
		var inputMap = new LinkedHashMap<String,String>();
		inputMap.put("a", "1");
		inputMap.put("b", "2");

		var map = MapBuilder.create(String.class, Integer.class)
			.valueFunction(o -> o instanceof String ? Integer.parseInt((String)o) : (Integer)o)
			.addAny(inputMap)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void j03_keyValueFunctions_withBothFunctions() {
		var inputMap = new LinkedHashMap<Integer,String>();
		inputMap.put(1, "10");
		inputMap.put(2, "20");

		var map = MapBuilder.create(String.class, Integer.class)
			.keyFunction(o -> String.valueOf(o))
			.valueFunction(o -> o instanceof String ? Integer.parseInt((String)o) : (Integer)o)
			.addAny(inputMap)
			.build();

		assertMap(map, "1=10", "2=20");
	}

	@Test
	void j04_keyValueFunctions_functionsConvenienceMethod() {
		var inputMap = new LinkedHashMap<Integer,String>();
		inputMap.put(1, "10");
		inputMap.put(2, "20");

		var map = MapBuilder.create(String.class, Integer.class)
			.functions(
				o -> String.valueOf(o),  // key function
				o -> o instanceof String ? Integer.parseInt((String)o) : (Integer)o  // value function
			)
			.addAny(inputMap)
			.build();

		assertMap(map, "1=10", "2=20");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// AddAny
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_addAny_withMap() {
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", 2);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(inputMap)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k02_addAny_withMultipleMaps() {
		var map1 = new LinkedHashMap<String,Integer>();
		map1.put("a", 1);
		map1.put("b", 2);

		var map2 = new LinkedHashMap<String,Integer>();
		map2.put("c", 3);
		map2.put("d", 4);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(map1, map2)
			.build();

		assertMap(map, "a=1", "b=2", "c=3", "d=4");
	}

	@Test
	void k03_addAny_withNullMap() {
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", 2);

		var map = MapBuilder.create(String.class, Integer.class)
			.addAny(inputMap, null)  // null map should be ignored
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k03b_addAny_withNullValueInMap() {
		// addAny uses toType which doesn't handle null values
		var inputMap = new LinkedHashMap<String,Integer>();
		inputMap.put("a", 1);
		inputMap.put("b", null);

		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.addAny(inputMap)
				.build();
		});
	}

	@Test
	void k04_addAny_withTypeConversion() {
		var inputMap = new LinkedHashMap<Integer,String>();
		inputMap.put(1, "10");
		inputMap.put(2, "20");

		var map = MapBuilder.create(String.class, Integer.class)
			.keyFunction(o -> String.valueOf(o))
			.valueFunction(o -> o instanceof String ? Integer.parseInt((String)o) : (Integer)o)
			.addAny(inputMap)
			.build();

		assertMap(map, "1=10", "2=20");
	}

	@Test
	void k05_addAny_withStringToMapConversion() {
		// This test is no longer applicable since addAny only accepts Map instances
		// If we want to parse strings, we'd need to do it before calling addAny
		var parsedMap = new LinkedHashMap<String,String>();
		for (var pair : "a=1,b=2".split(",")) {
			var kv = pair.split("=");
			if (kv.length == 2) {
				parsedMap.put(kv[0], kv[1]);
			}
		}

		var map = MapBuilder.create(String.class, String.class)
			.addAny(parsedMap)
			.build();

		assertMap(map, "a=1", "b=2");
	}

	@Test
	void k06_addAny_withMultipleMaps_parsedFromStrings() {
		var map1 = new LinkedHashMap<String,String>();
		map1.put("x", "foo");
		var map2 = new LinkedHashMap<String,String>();
		map2.put("y", "bar");

		var map = MapBuilder.create(String.class, String.class)
			.addAny(map1, map2)
			.build();

		assertMap(map, "x=foo", "y=bar");
	}

	@Test
	void k07_addAny_noKeyOrValueType() {
		assertThrows(IllegalArgumentException.class, () -> new MapBuilder<String,Integer>(null, null));
		assertThrows(IllegalArgumentException.class, () -> new MapBuilder<String,Integer>(String.class, null));
		assertThrows(IllegalArgumentException.class, () -> new MapBuilder<String,Integer>(null, Integer.class));
	}

	@Test
	void k08_addAny_nonMapObject() {
		// addAny only accepts Map instances, non-Map objects throw exception
		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.addAny("not-a-map")
				.build();
		});
	}

	@Test
	void k09_addAny_valueFunctionReturnsNull() {
		// Value function that can't convert should throw exception
		var inputMap = new LinkedHashMap<String,String>();
		inputMap.put("a", "not-an-integer");

		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, Integer.class)
				.valueFunction(o -> {
					if (o instanceof String) {
						try {
							return Integer.parseInt((String)o);
						} catch (NumberFormatException e) {
							throw new RuntimeException("Cannot convert", e);
						}
					}
					return (Integer)o;
				})
				.addAny(inputMap)
				.build();
		});
	}

	@Test
	void k10_addAny_keyFunctionConversionFailure() {
		var inputMap = new LinkedHashMap<Object,String>();
		inputMap.put(new Object(), "value");  // Object can't be converted to String

		assertThrows(RuntimeException.class, () -> {
			MapBuilder.create(String.class, String.class)
				.keyFunction(o -> {
					if (o instanceof String)
						return (String)o;
					throw new RuntimeException("Cannot convert key");
				})
				.addAny(inputMap)
				.build();
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Concurrent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_concurrent_defaultHashMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.concurrent()
			.add("a", 1)
			.add("b", 2)
			.build();

		assertSize(2, map);
		assertTrue(map instanceof java.util.concurrent.ConcurrentHashMap);
	}

	@Test
	void o02_concurrent_ordered() {
		var map = MapBuilder.create(String.class, Integer.class)
			.ordered()
			.concurrent()
			.add("a", 1)
			.add("b", 2)
			.build();

		assertSize(2, map);
		// Should be a synchronized LinkedHashMap - verify by checking order is preserved
		var keys = new ArrayList<>(map.keySet());
		assertEquals("a", keys.get(0));
		assertEquals("b", keys.get(1));
		// Verify it's synchronized by checking it's not a ConcurrentHashMap
		assertFalse(map instanceof java.util.concurrent.ConcurrentHashMap);
	}

	@Test
	void o03_concurrent_sorted() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.concurrent()
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.build();

		assertSize(3, map);
		assertTrue(map instanceof java.util.concurrent.ConcurrentSkipListMap);
		assertList(map.keySet(), "a", "b", "c");
	}

	@Test
	void o04_concurrent_boolean() {
		var map1 = MapBuilder.create(String.class, Integer.class)
			.concurrent(true)
			.add("a", 1)
			.build();
		assertTrue(map1 instanceof java.util.concurrent.ConcurrentHashMap);

		var map2 = MapBuilder.create(String.class, Integer.class)
			.concurrent(false)
			.add("a", 1)
			.build();
		assertTrue(map2 instanceof HashMap);
	}

	@Test
	void o05_concurrent_unmodifiable() {
		var map = MapBuilder.create(String.class, Integer.class)
			.concurrent()
			.unmodifiable()
			.add("a", 1)
			.build();

		assertSize(1, map);
		assertThrows(UnsupportedOperationException.class, () -> map.put("b", 2));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Build edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_build_sparseWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sparse()
			.build();

		assertNull(map);
	}

	@Test
	void l03_build_notSparseWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		assertEmpty(map);
	}

	@Test
	void l04_build_sortedWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sorted()
			.build();

		assertNotNull(map);
		assertTrue(map instanceof TreeMap);
		assertEmpty(map);
	}

	@Test
	void l06_build_orderedWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.ordered()
			.build();

		assertNotNull(map);
		assertTrue(map instanceof LinkedHashMap);
		assertEmpty(map);
	}

	@Test
	void l07_build_defaultHashMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.build();

		assertNotNull(map);
		// Without ordered(), should be HashMap (unordered)
		assertTrue(map instanceof HashMap);
		assertFalse(map instanceof LinkedHashMap);
		assertEmpty(map);
	}

	@Test
	void l05_build_unmodifiableWithNullMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.unmodifiable()
			.build();

		assertNotNull(map);
		assertThrows(UnsupportedOperationException.class, () -> map.put("a", 1));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BuildFluent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_buildFluent_returnsFluentMap() {
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.buildFluent();

		assertNotNull(map);
		assertSize(2, map);
		assertMap(map, "a=1", "b=2");
	}

	@Test
	void m02_buildFluent_sparseEmpty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.sparse()
			.buildFluent();

		assertNull(map);
	}

	@Test
	void m03_buildFluent_withFiltering() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null && v > 0)
			.add("a", 1)
			.add("b", -1)  // Filtered out
			.buildFluent();

		assertNotNull(map);
		assertSize(1, map);
		assertMap(map, "a=1");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BuildFiltered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_buildFiltered_withFilter() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null && v > 0)
			.add("a", 1)
			.add("b", -1)  // Filtered out
			.add("c", 2)
			.buildFiltered();

		assertNotNull(map);
		assertSize(2, map);
		assertMap(map, "a=1", "c=2");
	}

	@Test
	void n02_buildFiltered_withoutFilter() {
		// buildFiltered() should work even without explicit filter (uses default filter)
		var map = MapBuilder.create(String.class, Integer.class)
			.add("a", 1)
			.add("b", 2)
			.buildFiltered();

		assertNotNull(map);
		assertSize(2, map);
	}

	@Test
	void n03_buildFiltered_sparseEmpty() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null && v > 0)
			.sparse()
			.buildFiltered();

		assertNull(map);
	}

	@Test
	void n04_buildFiltered_withSorted() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null && v > 0)
			.add("c", 3)
			.add("a", 1)
			.add("b", 2)
			.sorted()
			.buildFiltered();

		assertNotNull(map);
		assertList(map.keySet(), "a", "b", "c");
	}

	@Test
	void n05_buildFiltered_multipleFilters() {
		var map = MapBuilder.create(String.class, Integer.class)
			.filtered((k, v) -> v != null)
			.filtered((k, v) -> v > 0)
			.filtered((k, v) -> !k.startsWith("_"))
			.add("a", 1)
			.add("_b", 2)  // Filtered out (starts with _)
			.add("c", -1)  // Filtered out (not > 0)
			.add("d", 3)
			.buildFiltered();

		assertNotNull(map);
		assertMap(map, "a=1", "d=3");
	}
}