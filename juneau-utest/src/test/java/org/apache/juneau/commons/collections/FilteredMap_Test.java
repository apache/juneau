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

import static org.apache.juneau.TestUtils.assertThrowsWithMessage;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FilteredMap_Test extends TestBase {

	//====================================================================================================
	// Basic filtering - filter out null values
	//====================================================================================================

	@Test
	void a01_filterNullValues_put() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertNull(map.put("key1", "value1"));  // Added
		assertNull(map.put("key2", null));      // Filtered out
		assertNull(map.put("key3", "value3"));  // Added

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertNull(map.get("key2"));
		assertFalse(map.containsKey("key2"));
		assertEquals("value3", map.get("key3"));
	}

	@Test
	void a02_filterNullValues_putAll() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		var source = map("key1", "value1", "key2", null, "key3", "value3", "key4", null);

		map.putAll(source);

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value3", map.get("key3"));
		assertFalse(map.containsKey("key2"));
		assertFalse(map.containsKey("key4"));
	}

	@Test
	void a03_filterNullValues_updateExisting() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		assertEquals("value1", map.put("key1", "value2"));  // Update existing
		assertEquals("value2", map.put("key1", null));  // Filtered out, returns previous value

		assertSize(1, map);
		assertEquals("value2", map.get("key1"));  // Still has old value
	}

	//====================================================================================================
	// Filter based on value - positive numbers only
	//====================================================================================================

	@Test
	void b01_filterPositiveNumbers() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		map.put("a", 5);   // Added
		map.put("b", -1);  // Filtered out
		map.put("c", 0);   // Filtered out
		map.put("d", 10);  // Added
		map.put("e", null); // Filtered out

		assertSize(2, map);
		assertEquals(5, map.get("a"));
		assertEquals(10, map.get("d"));
		assertFalse(map.containsKey("b"));
		assertFalse(map.containsKey("c"));
		assertFalse(map.containsKey("e"));
	}

	//====================================================================================================
	// Filter based on key - exclude keys starting with underscore
	//====================================================================================================

	@Test
	void c01_filterKeysStartingWithUnderscore() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> k != null && !k.startsWith("_"))
			.build();

		map.put("key1", "value1");   // Added
		map.put("_key2", "value2");  // Filtered out
		map.put("key3", "value3");   // Added
		map.put("_key4", "value4");  // Filtered out

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertEquals("value3", map.get("key3"));
		assertFalse(map.containsKey("_key2"));
		assertFalse(map.containsKey("_key4"));
	}

	//====================================================================================================
	// Filter based on both key and value
	//====================================================================================================

	@Test
	void d01_filterKeyAndValue() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> k != null && !k.startsWith("_") && v != null && v > 0)
			.build();

		map.put("key1", 5);    // Added
		map.put("_key2", 10);  // Filtered out (key starts with _)
		map.put("key3", -1);   // Filtered out (value <= 0)
		map.put("key4", 20);   // Added
		map.put(null, 5);      // Filtered out (null key)

		assertSize(2, map);
		assertEquals(5, map.get("key1"));
		assertEquals(20, map.get("key4"));
	}

	//====================================================================================================
	// Custom map types - TreeMap
	//====================================================================================================

	@Test
	void e01_customMapType_TreeMap() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.creator(() -> new TreeMap<>())
			.build();

		map.put("zebra", 3);
		map.put("apple", 1);
		map.put("banana", 2);

		// TreeMap maintains sorted order
		var keys = new ArrayList<>(map.keySet());
		assertEquals(List.of("apple", "banana", "zebra"), keys);
	}

	//====================================================================================================
	// Custom map types - ConcurrentHashMap
	//====================================================================================================

	@Test
	void e02_customMapType_ConcurrentHashMap() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.creator(() -> new ConcurrentHashMap<>())
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out
		map.put("key3", "value3");

		assertSize(2, map);
	}

	//====================================================================================================
	// Map interface methods - get, containsKey, containsValue
	//====================================================================================================

	@Test
	void f01_mapInterface_get() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out

		assertEquals("value1", map.get("key1"));
		assertNull(map.get("key2"));  // Not in map
		assertNull(map.get("nonexistent"));
	}

	@Test
	void f02_mapInterface_containsKey() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out

		assertTrue(map.containsKey("key1"));
		assertFalse(map.containsKey("key2"));
		assertFalse(map.containsKey("nonexistent"));
	}

	@Test
	void f03_mapInterface_containsValue() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out

		assertTrue(map.containsValue("value1"));
		assertFalse(map.containsValue(null));  // null was filtered out
	}

	@Test
	void f04_mapInterface_size() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertEquals(0, map.size());
		map.put("key1", "value1");
		assertEquals(1, map.size());
		map.put("key2", null);  // Filtered out
		assertEquals(1, map.size());
		map.put("key3", "value3");
		assertEquals(2, map.size());
	}

	@Test
	void f05_mapInterface_isEmpty() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertTrue(map.isEmpty());
		map.put("key1", "value1");
		assertFalse(map.isEmpty());
	}

	@Test
	void f06_mapInterface_clear() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", "value2");
		assertSize(2, map);

		map.clear();
		assertTrue(map.isEmpty());
		assertSize(0, map);
	}

	@Test
	void f07_mapInterface_remove() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", "value2");

		assertEquals("value1", map.remove("key1"));
		assertNull(map.remove("key1"));  // Already removed
		assertNull(map.remove("nonexistent"));

		assertSize(1, map);
		assertEquals("value2", map.get("key2"));
	}

	@Test
	void f08_mapInterface_keySet() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out
		map.put("key3", "value3");

		var keySet = map.keySet();
		assertSize(2, keySet);
		assertTrue(keySet.contains("key1"));
		assertTrue(keySet.contains("key3"));
		assertFalse(keySet.contains("key2"));
	}

	@Test
	void f09_mapInterface_values() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out
		map.put("key3", "value3");

		var values = map.values();
		assertSize(2, values);
		assertTrue(values.contains("value1"));
		assertTrue(values.contains("value3"));
		assertFalse(values.contains(null));
	}

	@Test
	void f10_mapInterface_entrySet() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out
		map.put("key3", "value3");

		var entrySet = map.entrySet();
		assertSize(2, entrySet);

		var found = new HashSet<String>();
		for (var entry : entrySet) {
			found.add(entry.getKey() + "=" + entry.getValue());
		}
		assertTrue(found.contains("key1=value1"));
		assertTrue(found.contains("key3=value3"));
		assertFalse(found.contains("key2=null"));
	}

	//====================================================================================================
	// Edge cases - null keys
	//====================================================================================================

	@Test
	void g01_nullKey_allowed() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.put(null, "value1");  // null key is allowed if filter passes
		map.put(null, null);      // Filtered out (null value)

		assertSize(1, map);
		assertEquals("value1", map.get(null));
		assertTrue(map.containsKey(null));
	}

	@Test
	void g02_nullKey_filtered() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> k != null && v != null)
			.build();

		map.put(null, "value1");  // Filtered out (null key)
		map.put("key1", "value1"); // Added

		assertSize(1, map);
		assertFalse(map.containsKey(null));
		assertEquals("value1", map.get("key1"));
	}

	//====================================================================================================
	// Edge cases - empty map
	//====================================================================================================

	@Test
	void h01_emptyMap() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertTrue(map.isEmpty());
		assertSize(0, map);
		assertNull(map.get("any"));
		assertFalse(map.containsKey("any"));
	}

	//====================================================================================================
	// Builder validation
	//====================================================================================================

	@Test
	void i01_builder_noFilter_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "filter", () -> {
			FilteredMap.create(String.class, String.class).build();
		});
	}

	@Test
	void i02_builder_nullFilter_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredMap.create(String.class, String.class).filter(null);
		});
	}

	@Test
	void i03_builder_nullCreator_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredMap.create(String.class, String.class)
				.filter((k, v) -> true)
				.creator(null);
		});
	}

	//====================================================================================================
	// Builder - create() without parameters
	//====================================================================================================

	@Test
	void j01_builder_createWithoutParameters() {
		var map = FilteredMap
			.<String,String>create()
			.filter((k, v) -> v != null)
			.build();

		map.put("key1", "value1");
		map.put("key2", null);  // Filtered out

		assertSize(1, map);
		assertEquals("value1", map.get("key1"));
	}

	@Test
	void j02_builder_createWithoutParameters_usesObjectClass() {
		var map = FilteredMap
			.create()
			.filter((k, v) -> v != null)
			.build();

		// Object.class accepts any type
		map.add("string", 123);           // String key, Integer value
		map.add(456, "value");            // Integer key, String value
		map.add(List.of(1, 2), Map.of()); // List key, Map value

		assertSize(3, map);
		assertEquals(123, map.get("string"));
		assertEquals("value", map.get(456));
		assertEquals(Map.of(), map.get(List.of(1, 2)));
	}

	//====================================================================================================
	// Complex filter - string length
	//====================================================================================================

	@Test
	void k01_filterStringLength() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null && v.length() > 3)
			.build();

		map.put("key1", "short");   // Added
		map.put("key2", "ab");      // Filtered out (length <= 3)
		map.put("key3", "longer");  // Added
		map.put("key4", null);      // Filtered out

		assertSize(2, map);
		assertEquals("short", map.get("key1"));
		assertEquals("longer", map.get("key3"));
	}

	//====================================================================================================
	// add() method - type validation with types specified
	//====================================================================================================

	@Test
	void l01_add_withTypeValidation_validTypes() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		map.add("key1", 5);   // Valid types, added
		map.add("key2", 10);  // Valid types, added
		map.add("key3", -1);  // Valid types, but filtered out

		assertSize(2, map);
		assertEquals(5, map.get("key1"));
		assertEquals(10, map.get("key2"));
	}

	@Test
	void l02_add_withTypeValidation_invalidKeyType() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "could not be converted to key type", () -> {
			map.add(123, 5);  // Invalid key type (Integer instead of String)
		});
	}

	@Test
	void l03_add_withTypeValidation_invalidValueType() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "could not be converted to value type", () -> {
			map.add("key", "value");  // Invalid value type (String instead of Integer)
		});
	}

	@Test
	void l04_add_withTypeValidation_noTypesSpecified() {
		var map = FilteredMap
			.<Object,Object>create()
			.filter((k, v) -> v != null)
			.build();

		// Object.class is used when types not specified, which accepts any type
		map.add("key1", "value1");
		map.add(123, 456);  // Different types, but Object.class accepts any type

		assertSize(2, map);
		assertEquals("value1", map.get("key1"));
		assertEquals(456, map.get(123));
	}

	//====================================================================================================
	// add() method - with keyFunction
	//====================================================================================================

	@Test
	void m01_add_withKeyFunction() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.keyFunction(o -> o.toString())
			.build();

		map.add(123, 5);   // Key converted from Integer to String
		map.add(456, 10);  // Key converted from Integer to String
		map.add(789, -1);  // Key converted, but value filtered out

		assertSize(2, map);
		assertEquals(5, map.get("123"));
		assertEquals(10, map.get("456"));
		assertFalse(map.containsKey("789"));
	}

	@Test
	void m02_add_withKeyFunction_noTypeSpecified() {
		var map = FilteredMap
			.<String,String>create()
			.filter((k, v) -> v != null)
			.keyFunction(o -> o.toString())
			.build();

		map.add(123, "value1");  // Key converted using function

		assertSize(1, map);
		assertEquals("value1", map.get("123"));
	}

	//====================================================================================================
	// add() method - with valueFunction
	//====================================================================================================

	@Test
	void n01_add_withValueFunction() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.valueFunction(o -> Integer.parseInt(o.toString()))
			.build();

		map.add("key1", "5");   // Value converted from String to Integer
		map.add("key2", "10");  // Value converted from String to Integer
		map.add("key3", "-1");  // Value converted, but filtered out

		assertSize(2, map);
		assertEquals(5, map.get("key1"));
		assertEquals(10, map.get("key2"));
		assertFalse(map.containsKey("key3"));
	}

	@Test
	void n02_add_withValueFunction_noTypeSpecified() {
		var map = FilteredMap
			.<String,Integer>create()
			.filter((k, v) -> v != null && v > 0)
			.valueFunction(o -> Integer.parseInt(o.toString()))
			.build();

		map.add("key1", "5");  // Value converted using function

		assertSize(1, map);
		assertEquals(5, map.get("key1"));
	}

	//====================================================================================================
	// add() method - with both keyFunction and valueFunction
	//====================================================================================================

	@Test
	void o01_add_withBothFunctions() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.keyFunction(o -> o.toString())
			.valueFunction(o -> Integer.parseInt(o.toString()))
			.build();

		map.add(123, "5");   // Both key and value converted
		map.add(456, "10");  // Both key and value converted
		map.add(789, "-1");  // Both converted, but value filtered out

		assertSize(2, map);
		assertEquals(5, map.get("123"));
		assertEquals(10, map.get("456"));
		assertFalse(map.containsKey("789"));
	}

	//====================================================================================================
	// addAll() method
	//====================================================================================================

	@Test
	void p01_addAll_withTypeValidation() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		var source = Map.of(
			"key1", 5,
			"key2", -1,  // Filtered out
			"key3", 10
		);

		map.addAll(source);

		assertSize(2, map);
		assertEquals(5, map.get("key1"));
		assertEquals(10, map.get("key3"));
		assertFalse(map.containsKey("key2"));
	}

	@Test
	void p02_addAll_withValueFunction() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.valueFunction(o -> Integer.parseInt(o.toString()))
			.build();

		var source = Map.of(
			"key1", "5",
			"key2", "-1",  // Converted then filtered out
			"key3", "10"
		);

		map.addAll(source);

		assertSize(2, map);
		assertEquals(5, map.get("key1"));
		assertEquals(10, map.get("key3"));
		assertFalse(map.containsKey("key2"));
	}

	@Test
	void p03_addAll_withKeyFunction() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.keyFunction(o -> o.toString())
			.build();

		var source = Map.of(
			123, 5,
			456, -1,  // Filtered out
			789, 10
		);

		map.addAll(source);

		assertSize(2, map);
		assertEquals(5, map.get("123"));
		assertEquals(10, map.get("789"));
		assertFalse(map.containsKey("456"));
	}

	@Test
	void p04_addAll_nullSource() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.addAll(null);  // Should be no-op

		assertTrue(map.isEmpty());
		assertSize(0, map);
	}

	@Test
	void p05_addAll_emptySource() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.addAll(Map.of());  // Empty map

		assertTrue(map.isEmpty());
		assertSize(0, map);
	}

	//====================================================================================================
	// add() method - return value
	//====================================================================================================

	@Test
	void q01_add_returnValue_newEntry() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertNull(map.add("key1", "value1"));  // New entry, returns null
	}

	@Test
	void q02_add_returnValue_updateExisting() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.add("key1", "value1");
		assertEquals("value1", map.add("key1", "value2"));  // Update, returns old value
	}

	@Test
	void q03_add_returnValue_filteredOut() {
		var map = FilteredMap
			.create(String.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		map.add("key1", "value1");
		assertNull(map.add("key1", null));  // Filtered out, returns null (not old value)
		assertEquals("value1", map.get("key1"));  // Old value still there
	}

	//====================================================================================================
	// add() method - edge cases with functions
	//====================================================================================================

	@Test
	void r01_add_keyFunctionThrowsException() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null)
			.keyFunction(o -> {
				if (o == null)
					throw new IllegalArgumentException("Key cannot be null");
				return o.toString();
			})
			.build();

		assertThrows(IllegalArgumentException.class, () -> {
			map.add(null, 5);
		});
	}

	@Test
	void r02_add_valueFunctionThrowsException() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null)
			.valueFunction(o -> {
				if (o == null)
					throw new IllegalArgumentException("Value cannot be null");
				return Integer.parseInt(o.toString());
			})
			.build();

		assertThrows(IllegalArgumentException.class, () -> {
			map.add("key", null);
		});
	}

	@Test
	void r03_add_valueFunctionReturnsNull() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.valueFunction(o -> {
				try {
					return Integer.parseInt(o.toString());
				} catch (NumberFormatException e) {
					return null;  // Return null for invalid numbers
				}
			})
			.build();

		map.add("key1", "5");   // Valid, added
		map.add("key2", "abc"); // Function returns null, filtered out

		assertSize(1, map);
		assertEquals(5, map.get("key1"));
		assertFalse(map.containsKey("key2"));
	}

	//====================================================================================================
	// wouldAccept() method
	//====================================================================================================

	@Test
	void s01_wouldAccept_returnsTrue() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		assertTrue(map.wouldAccept("key1", 5));
		assertTrue(map.wouldAccept("key2", 10));
	}

	@Test
	void s02_wouldAccept_returnsFalse() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		assertFalse(map.wouldAccept("key1", null));
		assertFalse(map.wouldAccept("key2", -1));
		assertFalse(map.wouldAccept("key3", 0));
	}

	@Test
	void s03_wouldAccept_usedForPreValidation() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		// Pre-validate before adding
		if (map.wouldAccept("key1", 5)) {
			map.put("key1", 5);
		}
		if (map.wouldAccept("key2", -1)) {
			map.put("key2", -1);
		}

		assertSize(1, map);
		assertTrue(map.containsKey("key1"));
		assertFalse(map.containsKey("key2"));
	}

	//====================================================================================================
	// getFilter() method
	//====================================================================================================

	@Test
	void t01_getFilter_returnsFilter() {
		var originalFilter = (BiPredicate<String, Integer>)((k, v) -> v != null && v > 0);
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter(originalFilter)
			.build();

		var retrievedFilter = map.getFilter();
		assertNotNull(retrievedFilter);
		assertSame(originalFilter, retrievedFilter);  // Should be the same instance
	}

	@Test
	void t02_getFilter_canBeUsedForOtherPurposes() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.build();

		var filter = map.getFilter();

		// Use the filter independently
		assertTrue(filter.test("key1", 5));
		assertFalse(filter.test("key2", -1));
	}

	//====================================================================================================
	// Builder.functions() convenience method
	//====================================================================================================

	@Test
	void u01_builder_functions_setsBothAtOnce() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null && v > 0)
			.functions(
				o -> o.toString(),                    // Key function
				o -> Integer.parseInt(o.toString())   // Value function
			)
			.build();

		map.add(123, "5");   // Both key and value converted
		map.add(456, "10");  // Both key and value converted

		assertSize(2, map);
		assertEquals(5, map.get("123"));
		assertEquals(10, map.get("456"));
	}

	@Test
	void u02_builder_functions_withNullFunctions() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> v != null)
			.functions(null, null)  // Both functions null
			.build();

		map.add("key1", 5);  // No conversion, direct add

		assertSize(1, map);
		assertEquals(5, map.get("key1"));
	}

	//====================================================================================================
	// Null handling for primitive types
	//====================================================================================================

	@Test
	void v01_add_nullKeyForPrimitiveType_throwsException() {
		var map = FilteredMap
			.create(int.class, String.class)
			.filter((k, v) -> v != null)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "Cannot set null key for primitive type", () -> {
			map.add(null, "value");
		});
	}

	@Test
	void v02_add_nullValueForPrimitiveType_throwsException() {
		var map = FilteredMap
			.create(String.class, int.class)
			.filter((k, v) -> true)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "Cannot set null value for primitive type", () -> {
			map.add("key", null);
		});
	}

	@Test
	void v03_add_nullForWrapperType_allowed() {
		var map = FilteredMap
			.create(String.class, Integer.class)
			.filter((k, v) -> true)  // Accept all, including null
			.build();

		map.add("key1", null);  // Should work for wrapper types
		map.add(null, 5);       // Should work for wrapper types

		assertSize(2, map);
		assertNull(map.get("key1"));
		assertEquals(5, map.get(null));
	}
}

