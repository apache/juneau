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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MultiMap_Test extends TestBase {

	//====================================================================================================
	// Basic functionality - get(Object key)
	//====================================================================================================

	@Test
	void a01_get_fromFirstMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals("value1", multiMap.get("key1"));
		assertEquals("value2", multiMap.get("key2"));
		assertEquals("value3", multiMap.get("key3"));
	}

	@Test
	void a02_get_duplicateKey_returnsFirstMatch() {
		var map1 = map("key1", "value1");
		var map2 = map("key1", "value2");
		var map3 = map("key1", "value3");
		var multiMap = new MultiMap<>(map1, map2, map3);

		assertEquals("value1", multiMap.get("key1")); // First match wins
	}

	@Test
	void a03_get_nonexistentKey_returnsNull() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertNull(multiMap.get("key3"));
	}

	@Test
	void a04_get_emptyMaps() {
		var map1 = map();
		var map2 = map();
		var multiMap = new MultiMap<>(map1, map2);

		assertNull(multiMap.get("key1"));
		assertTrue(multiMap.isEmpty());
	}

	//====================================================================================================
	// containsKey(Object key)
	//====================================================================================================

	@Test
	void b01_containsKey_existsInFirstMap() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.containsKey("key1"));
		assertTrue(multiMap.containsKey("key2"));
		assertFalse(multiMap.containsKey("key3"));
	}

	@Test
	void b02_containsKey_existsInSecondMap() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.containsKey("key2"));
	}

	@Test
	void b03_containsKey_duplicateKey_returnsTrue() {
		var map1 = map("key1", "value1");
		var map2 = map("key1", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.containsKey("key1"));
	}

	//====================================================================================================
	// containsValue(Object value)
	//====================================================================================================

	@Test
	void c01_containsValue_existsInAnyMap() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.containsValue("value1"));
		assertTrue(multiMap.containsValue("value2"));
		assertFalse(multiMap.containsValue("value3"));
	}

	@Test
	void c02_containsValue_duplicateValue_returnsTrue() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value1");
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.containsValue("value1"));
	}

	//====================================================================================================
	// size()
	//====================================================================================================

	@Test
	void d01_size_countsUniqueKeys() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals(3, multiMap.size());
	}

	@Test
	void d02_size_duplicateKeys_countedOnce() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key2", "value2b", "key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals(3, multiMap.size()); // key1, key2, key3 (key2 counted once)
	}

	@Test
	void d03_size_emptyMaps() {
		var map1 = map();
		var map2 = map();
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals(0, multiMap.size());
	}

	@Test
	void d04_size_singleMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var multiMap = new MultiMap<>(map1);

		assertEquals(2, multiMap.size());
	}

	//====================================================================================================
	// isEmpty()
	//====================================================================================================

	@Test
	void e01_isEmpty_allMapsEmpty() {
		var map1 = map();
		var map2 = map();
		var multiMap = new MultiMap<>(map1, map2);

		assertTrue(multiMap.isEmpty());
	}

	@Test
	void e02_isEmpty_someMapsHaveEntries() {
		Map<String, String> map1 = map();
		var map2 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1, map2);

		assertFalse(multiMap.isEmpty());
	}

	//====================================================================================================
	// entrySet()
	//====================================================================================================

	@Test
	void f01_entrySet_iteratesAllEntries() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		var entries = new ArrayList<Map.Entry<String, String>>();
		multiMap.entrySet().forEach(entries::add);

		assertEquals(3, entries.size());
		assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("key1") && e.getValue().equals("value1")));
		assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("key2") && e.getValue().equals("value2")));
		assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("key3") && e.getValue().equals("value3")));
	}

	@Test
	void f02_entrySet_duplicateKeys_onlyFirstIncluded() {
		var map1 = map("key1", "value1");
		var map2 = map("key1", "value2");
		var map3 = map("key2", "value3");
		var multiMap = new MultiMap<>(map1, map2, map3);

		var entries = new ArrayList<Map.Entry<String, String>>();
		multiMap.entrySet().forEach(entries::add);

		assertEquals(2, entries.size());
		// key1 should have value1 (from first map)
		var key1Entry = entries.stream().filter(e -> e.getKey().equals("key1")).findFirst().orElse(null);
		assertNotNull(key1Entry);
		assertEquals("value1", key1Entry.getValue());
	}

	@Test
	void f03_entrySet_iterator_remove() {
		var map1 = new LinkedHashMap<>(map("key1", "value1", "key2", "value2"));
		var map2 = new LinkedHashMap<>(map("key3", "value3"));
		var multiMap = new MultiMap<>(map1, map2);

		var iterator = multiMap.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			if (entry.getKey().equals("key2")) {
				iterator.remove();
			}
		}

		assertFalse(map1.containsKey("key2"));
		assertEquals(2, multiMap.size());
	}

	@Test
	void f04_entrySet_size() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key2", "value2b", "key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals(3, multiMap.entrySet().size()); // key1, key2, key3
	}

	//====================================================================================================
	// keySet()
	//====================================================================================================

	@Test
	void g01_keySet_containsAllKeys() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		var keySet = multiMap.keySet();
		assertTrue(keySet.contains("key1"));
		assertTrue(keySet.contains("key2"));
		assertTrue(keySet.contains("key3"));
		assertFalse(keySet.contains("key4"));
		assertEquals(3, keySet.size());
	}

	@Test
	void g02_keySet_duplicateKeys_countedOnce() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key2", "value2b", "key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		var keySet = multiMap.keySet();
		assertEquals(3, keySet.size()); // key1, key2, key3
		assertTrue(keySet.contains("key1"));
		assertTrue(keySet.contains("key2"));
		assertTrue(keySet.contains("key3"));
	}

	//====================================================================================================
	// values()
	//====================================================================================================

	@Test
	void h01_values_containsAllValues() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2);

		var values = multiMap.values();
		assertTrue(values.contains("value1"));
		assertTrue(values.contains("value2"));
		assertTrue(values.contains("value3"));
		assertEquals(3, values.size());
	}

	@Test
	void h02_values_duplicateKeys_usesFirstValue() {
		var map1 = map("key1", "value1");
		var map2 = map("key1", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		var values = multiMap.values();
		assertTrue(values.contains("value1"));
		assertFalse(values.contains("value2")); // value2 is not returned because key1 is in first map
		assertEquals(1, values.size());
	}

	//====================================================================================================
	// Unsupported operations
	//====================================================================================================

	@Test
	void i01_put_throwsUnsupportedOperationException() {
		var map1 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1);

		assertThrows(UnsupportedOperationException.class, () -> multiMap.put("key2", "value2"));
	}

	@Test
	void i02_remove_throwsUnsupportedOperationException() {
		var map1 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1);

		assertThrows(UnsupportedOperationException.class, () -> multiMap.remove("key1"));
	}

	@Test
	void i03_putAll_throwsUnsupportedOperationException() {
		var map1 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1);
		var map2 = map("key2", "value2");

		assertThrows(UnsupportedOperationException.class, () -> multiMap.putAll(map2));
	}

	@Test
	void i04_clear_throwsUnsupportedOperationException() {
		var map1 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1);

		assertThrows(UnsupportedOperationException.class, multiMap::clear);
	}

	//====================================================================================================
	// Edge cases
	//====================================================================================================

	@Test
	void j01_singleMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var multiMap = new MultiMap<>(map1);

		assertEquals(2, multiMap.size());
		assertEquals("value1", multiMap.get("key1"));
		assertEquals("value2", multiMap.get("key2"));
	}

	@Test
	void j02_threeMaps() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var map3 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2, map3);

		assertEquals(3, multiMap.size());
		assertEquals("value1", multiMap.get("key1"));
		assertEquals("value2", multiMap.get("key2"));
		assertEquals("value3", multiMap.get("key3"));
	}

	@Test
	void j03_nullValue() {
		Map<String, String> map1 = map("key1", null);
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertNull(multiMap.get("key1"));
		assertTrue(multiMap.containsKey("key1"));
		assertTrue(multiMap.containsValue(null));
	}

	@Test
	void j04_nullKey() {
		var map1 = new LinkedHashMap<String, String>();
		map1.put(null, "value1");
		var map2 = map("key2", "value2");
		var multiMap = new MultiMap<>(map1, map2);

		assertEquals("value1", multiMap.get(null));
		assertTrue(multiMap.containsKey(null));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================

	@Test
	void k01_toString_singleMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var multiMap = new MultiMap<>(map1);

		var expected = "[" + map1.toString() + "]";
		assertEquals(expected, multiMap.toString());
	}

	@Test
	void k02_toString_multipleMaps() {
		var map1 = map("key1", "value1");
		var map2 = map("key2", "value2");
		var map3 = map("key3", "value3");
		var multiMap = new MultiMap<>(map1, map2, map3);

		var expected = "[" + map1.toString() + ", " + map2.toString() + ", " + map3.toString() + "]";
		assertEquals(expected, multiMap.toString());
	}

	@Test
	void k03_toString_emptyMaps() {
		Map<String, String> map1 = map();
		Map<String, String> map2 = map();
		var multiMap = new MultiMap<>(map1, map2);

		var expected = "[" + map1.toString() + ", " + map2.toString() + "]";
		assertEquals(expected, multiMap.toString());
	}

	@Test
	void k04_toString_mixedEmptyAndNonEmpty() {
		Map<String, String> map1 = map();
		var map2 = map("key1", "value1");
		Map<String, String> map3 = map();
		var multiMap = new MultiMap<>(map1, map2, map3);

		var expected = "[" + map1.toString() + ", " + map2.toString() + ", " + map3.toString() + "]";
		assertEquals(expected, multiMap.toString());
	}

	//====================================================================================================
	// equals() and hashCode()
	//====================================================================================================

	@Test
	void l01_equals_sameContents() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap1 = new MultiMap<>(map1, map2);

		var map3 = map("key1", "value1", "key2", "value2");
		var map4 = map("key3", "value3");
		var multiMap2 = new MultiMap<>(map3, map4);

		assertTrue(multiMap1.equals(multiMap2));
		assertTrue(multiMap2.equals(multiMap1));
	}

	@Test
	void l02_equals_differentContents() {
		var map1 = map("key1", "value1");
		var multiMap1 = new MultiMap<>(map1);

		var map2 = map("key1", "value2");
		var multiMap2 = new MultiMap<>(map2);

		assertFalse(multiMap1.equals(multiMap2));
		assertFalse(multiMap2.equals(multiMap1));
	}

	@Test
	void l03_equals_regularMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var multiMap = new MultiMap<>(map1);

		var regularMap = new LinkedHashMap<>(map("key1", "value1", "key2", "value2"));

		assertTrue(multiMap.equals(regularMap));
		assertTrue(regularMap.equals(multiMap));
	}

	@Test
	void l04_equals_notAMap() {
		var map1 = map("key1", "value1");
		var multiMap = new MultiMap<>(map1);

		assertFalse(multiMap.equals(null));
	}

	@Test
	void l05_hashCode_sameContents() {
		var map1 = map("key1", "value1", "key2", "value2");
		var map2 = map("key3", "value3");
		var multiMap1 = new MultiMap<>(map1, map2);

		var map3 = map("key1", "value1", "key2", "value2");
		var map4 = map("key3", "value3");
		var multiMap2 = new MultiMap<>(map3, map4);

		assertEquals(multiMap1.hashCode(), multiMap2.hashCode());
	}

	@Test
	void l06_hashCode_regularMap() {
		var map1 = map("key1", "value1", "key2", "value2");
		var multiMap = new MultiMap<>(map1);

		var regularMap = new LinkedHashMap<>(map("key1", "value1", "key2", "value2"));

		assertEquals(multiMap.hashCode(), regularMap.hashCode());
	}

	//====================================================================================================
	// Additional coverage for specific lines
	//====================================================================================================

	@Test
	void m01_entrySet_iterator_emptyMaps() {
		// Line 218: if (m.length > 0) - when there are no maps
		Map<String, String> map1 = map();
		Map<String, String> map2 = map();
		var multiMap = new MultiMap<>(map1, map2);
		var iterator = multiMap.entrySet().iterator();
		assertFalse(iterator.hasNext());
	}

	@Test
	void m02_entrySet_iterator_next_throwsWhenNextEntryIsNull() {
		// Line 253: throw NoSuchElementException when nextEntry == null
		Map<String, String> map1 = map();
		var multiMap = new MultiMap<>(map1);
		var iterator = multiMap.entrySet().iterator();
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	void m03_entrySet_iterator_remove_throwsWhenCanRemoveIsFalse() {
		// Line 264: throw IllegalStateException when canRemove is false or lastIterator is null
		var map1 = new LinkedHashMap<>(map("key1", "value1"));
		var multiMap = new MultiMap<>(map1);
		var iterator = multiMap.entrySet().iterator();
		// Remove without calling next first
		assertThrows(IllegalStateException.class, iterator::remove);
	}

	@Test
	void m04_entrySet_iterator_remove_throwsWhenLastIteratorIsNull() {
		// Line 264: throw IllegalStateException when lastIterator is null
		var map1 = new LinkedHashMap<>(map("key1", "value1"));
		var multiMap = new MultiMap<>(map1);
		var iterator = multiMap.entrySet().iterator();
		iterator.next(); // Sets canRemove = true and lastIterator
		iterator.remove(); // First remove works
		// Now try to remove again without calling next
		assertThrows(IllegalStateException.class, iterator::remove);
	}

	@Test
	void m05_values_iterator_remove() {
		// Lines 350-351: entryIterator.remove() in values iterator
		// Test that remove() delegates to entryIterator.remove()
		var map1 = new LinkedHashMap<>(map("key1", "value1"));
		var multiMap = new MultiMap<>(map1);
		var valuesIterator = multiMap.values().iterator();
		
		// Get first value
		assertEquals("value1", valuesIterator.next());
		
		// Remove should work (delegates to entryIterator.remove() which calls entrySet iterator remove)
		// This covers lines 350-351
		valuesIterator.remove();
		
		// Verify the entry was removed from the underlying map
		assertFalse(map1.containsKey("key1"));
		assertTrue(map1.isEmpty());
	}
}

