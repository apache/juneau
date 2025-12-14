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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FilteredList_Test extends TestBase {

	//====================================================================================================
	// Basic filtering - filter out null values
	//====================================================================================================

	@Test
	void a01_filterNullValues_add() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(list.add("value1"));  // Added
		assertFalse(list.add(null));      // Filtered out
		assertTrue(list.add("value3"));  // Added

		assertSize(2, list);
		assertEquals("value1", list.get(0));
		assertEquals("value3", list.get(1));
	}

	@Test
	void a02_filterNullValues_addAll() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		var source = list("value1", null, "value3", null);

		list.addAll(source);

		assertSize(2, list);
		assertEquals("value1", list.get(0));
		assertEquals("value3", list.get(1));
	}

	@Test
	void a03_filterNullValues_addAtIndex() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add(1, "value3");  // Added at index 1
		list.add(1, null);      // Filtered out, not added

		assertSize(3, list);
		assertEquals("value1", list.get(0));
		assertEquals("value3", list.get(1));
		assertEquals("value2", list.get(2));
	}

	//====================================================================================================
	// Filter based on value - positive numbers only
	//====================================================================================================

	@Test
	void b01_filterPositiveNumbers() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(list.add(5));   // Added
		assertFalse(list.add(-1));  // Filtered out
		assertFalse(list.add(0));   // Filtered out
		assertTrue(list.add(10));  // Added
		assertFalse(list.add(null)); // Filtered out

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	//====================================================================================================
	// Filter based on string length
	//====================================================================================================

	@Test
	void c01_filterStringLength() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null && v.length() > 3)
			.build();

		assertTrue(list.add("short"));   // Added
		assertFalse(list.add("ab"));      // Filtered out (length <= 3)
		assertTrue(list.add("longer"));  // Added
		assertFalse(list.add(null));      // Filtered out

		assertSize(2, list);
		assertEquals("short", list.get(0));
		assertEquals("longer", list.get(1));
	}

	//====================================================================================================
	// Custom list types - LinkedList
	//====================================================================================================

	@Test
	void d01_customListType_LinkedList() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.inner(new LinkedList<>())
			.build();

		list.add(5);
		list.add(-1);  // Filtered out
		list.add(10);

		assertSize(2, list);
	}

	//====================================================================================================
	// Custom list types - CopyOnWriteArrayList
	//====================================================================================================

	@Test
	void d02_customListType_CopyOnWriteArrayList() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.inner(new CopyOnWriteArrayList<>())
			.build();

		list.add("value1");
		list.add(null);  // Filtered out
		list.add("value3");

		assertSize(2, list);
	}

	//====================================================================================================
	// List interface methods - get, contains, indexOf
	//====================================================================================================

	@Test
	void e01_listInterface_get() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add(null);  // Filtered out

		assertEquals("value1", list.get(0));
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
	}

	@Test
	void e02_listInterface_contains() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add(null);  // Filtered out

		assertTrue(list.contains("value1"));
		assertFalse(list.contains(null));  // null was filtered out
	}

	@Test
	void e03_listInterface_indexOf() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add(null);  // Filtered out
		list.add("value1");

		assertEquals(0, list.indexOf("value1"));
		assertEquals(1, list.indexOf("value2"));
		assertEquals(-1, list.indexOf(null));  // null was filtered out
		assertEquals(2, list.lastIndexOf("value1"));
	}

	@Test
	void e04_listInterface_size() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertEquals(0, list.size());
		list.add("value1");
		assertEquals(1, list.size());
		list.add(null);  // Filtered out
		assertEquals(1, list.size());
		list.add("value3");
		assertEquals(2, list.size());
	}

	@Test
	void e05_listInterface_isEmpty() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(list.isEmpty());
		list.add("value1");
		assertFalse(list.isEmpty());
	}

	@Test
	void e06_listInterface_clear() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		assertSize(2, list);

		list.clear();
		assertTrue(list.isEmpty());
		assertSize(0, list);
	}

	@Test
	void e07_listInterface_remove() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");

		assertTrue(list.remove("value1"));
		assertFalse(list.remove("value1"));  // Already removed
		assertFalse(list.remove("nonexistent"));

		assertSize(1, list);
		assertEquals("value2", list.get(0));
	}

	@Test
	void e08_listInterface_removeAtIndex() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add("value3");

		assertEquals("value2", list.remove(1));
		assertSize(2, list);
		assertEquals("value1", list.get(0));
		assertEquals("value3", list.get(1));
	}

	@Test
	void e09_listInterface_set() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");

		assertEquals("value2", list.set(1, "value3"));  // Updated
		assertEquals("value3", list.get(1));

		assertEquals("value3", list.set(1, null));  // Filtered out, returns existing
		assertEquals("value3", list.get(1));  // Still has old value
	}

	//====================================================================================================
	// Edge cases - empty list
	//====================================================================================================

	@Test
	void f01_emptyList() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(list.isEmpty());
		assertSize(0, list);
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
		assertFalse(list.contains("any"));
	}

	//====================================================================================================
	// Builder validation
	//====================================================================================================

	@Test
	void g01_builder_noFilter_acceptsAllElements() {
		// Filter is optional - defaults to v -> true (accepts all elements)
		var list = FilteredList.create(String.class)
			.build();

		assertNotNull(list);
		assertTrue(list.add("value1"));
		assertTrue(list.add(null));  // Should be accepted (no filter)
		assertTrue(list.add("value3"));

		assertSize(3, list);
		assertList(list, "value1", "<null>", "value3");
	}

	@Test
	void g02_builder_nullFilter_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredList.create(String.class).filter(null);
		});
	}

	@Test
	void g03_builder_nullInner_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredList.create(String.class)
				.filter(v -> true)
				.inner(null);
		});
	}

	//====================================================================================================
	// Builder - create() without parameters
	//====================================================================================================

	@Test
	void h01_builder_createWithoutParameters() {
		var list = FilteredList
			.<String>create()
			.filter(v -> v != null)
			.build();

		assertTrue(list.add("value1"));
		assertFalse(list.add(null));  // Filtered out

		assertSize(1, list);
		assertEquals("value1", list.get(0));
	}

	@Test
	void h02_builder_createWithoutParameters_usesObjectClass() {
		var list = FilteredList
			.create()
			.filter(v -> v != null)
			.build();

		// Object.class accepts any type
		list.add("string");
		list.add(123);
		list.add(List.of(1, 2));

		assertSize(3, list);
		assertEquals("string", list.get(0));
		assertEquals(123, list.get(1));
		assertEquals(List.of(1, 2), list.get(2));
	}

	//====================================================================================================
	// add() method - type validation with types specified
	//====================================================================================================

	@Test
	void i01_add_withTypeValidation_validTypes() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(list.add(5));   // Valid types, added
		assertTrue(list.add(10));  // Valid types, added
		assertFalse(list.add(-1));  // Valid types, but filtered out

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void i02_add_withTypeValidation_invalidType() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "could not be converted to element type", () -> {
			list.addConverted("value");  // Invalid type (String instead of Integer)
		});
	}

	@Test
	void i03_add_withTypeValidation_noTypesSpecified() {
		var list = FilteredList
			.<Object>create()
			.filter(v -> v != null)
			.build();

		// Object.class is used when types not specified, which accepts any type
		list.add("value1");
		list.add(123);  // Different types, but Object.class accepts any type

		assertSize(2, list);
		assertEquals("value1", list.get(0));
		assertEquals(123, list.get(1));
	}

	//====================================================================================================
	// add() method - with elementFunction
	//====================================================================================================

	@Test
	void j01_add_withElementFunction() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		assertTrue(list.addConverted("5"));   // Element converted from String to Integer
		assertTrue(list.addConverted("10"));  // Element converted from String to Integer
		assertFalse(list.addConverted("-1"));  // Element converted, but filtered out

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void j02_add_withElementFunction_noTypeSpecified() {
		var list = FilteredList
			.<Integer>create()
			.filter(v -> v != null)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		assertTrue(list.addConverted("123"));  // Element converted using function

		assertSize(1, list);
		assertEquals(123, list.get(0));
	}

	//====================================================================================================
	// addAll() method
	//====================================================================================================

	@Test
	void k01_addAll_withTypeValidation() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var source = List.of(5, -1, 10);

		assertTrue(list.addAll(source));

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void k02_addAll_withElementFunction() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		var source = List.of("5", "-1", "10");

		list.addAllConverted(source);

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void k03_addAll_nullSource() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.addAllConverted(null);  // Should be no-op

		assertTrue(list.isEmpty());
		assertSize(0, list);
	}

	@Test
	void k04_addAll_emptySource() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertFalse(list.addAll(List.of()));  // Empty list

		assertTrue(list.isEmpty());
		assertSize(0, list);
	}

	@Test
	void k05_addAll_atIndex() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		list.add(1);
		list.add(5);

		assertTrue(list.addAll(1, List.of(2, 3, -1, 4)));  // -1 filtered out

		assertSize(5, list);
		assertEquals(1, list.get(0));
		assertEquals(2, list.get(1));
		assertEquals(3, list.get(2));
		assertEquals(4, list.get(3));
		assertEquals(5, list.get(4));
	}

	//====================================================================================================
	// add() method - return value
	//====================================================================================================

	@Test
	void l01_add_returnValue_newElement() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(list.add("value1"));  // New element, returns true
	}

	@Test
	void l02_add_returnValue_filteredOut() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		assertFalse(list.add(null));  // Filtered out, returns false
		assertEquals("value1", list.get(0));  // Old value still there
	}

	//====================================================================================================
	// add() method - edge cases with functions
	//====================================================================================================

	@Test
	void m01_add_elementFunctionThrowsException() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null)
			.elementFunction(o -> {
				if (o == null)
					throw new IllegalArgumentException("Element cannot be null");
				return Integer.parseInt(o.toString());
			})
			.build();

		assertThrows(IllegalArgumentException.class, () -> {
			list.addConverted(null);
		});
	}

	@Test
	void m02_add_elementFunctionReturnsNull() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.elementFunction(o -> {
				try {
					return Integer.parseInt(o.toString());
				} catch (NumberFormatException e) {
					return null;  // Return null for invalid numbers
				}
			})
			.build();

		assertTrue(list.addConverted("5"));   // Valid, added
		assertFalse(list.addConverted("abc")); // Function returns null, filtered out

		assertSize(1, list);
		assertEquals(5, list.get(0));
	}

	//====================================================================================================
	// wouldAccept() method
	//====================================================================================================

	@Test
	void n01_wouldAccept_returnsTrue() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(list.wouldAccept(5));
		assertTrue(list.wouldAccept(10));
	}

	@Test
	void n02_wouldAccept_returnsFalse() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertFalse(list.wouldAccept(null));
		assertFalse(list.wouldAccept(-1));
		assertFalse(list.wouldAccept(0));
	}

	@Test
	void n03_wouldAccept_usedForPreValidation() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		// Pre-validate before adding
		if (list.wouldAccept(5)) {
			list.add(5);
		}
		if (list.wouldAccept(-1)) {
			list.add(-1);
		}

		assertSize(1, list);
		assertTrue(list.contains(5));
		assertFalse(list.contains(-1));
	}

	//====================================================================================================
	// getFilter() method
	//====================================================================================================

	@Test
	void o01_getFilter_returnsFilter() {
		var originalFilter = (Predicate<Integer>)(v -> v != null && v > 0);
		var list = FilteredList
			.create(Integer.class)
			.filter(originalFilter)
			.build();

		var retrievedFilter = list.getFilter();
		assertNotNull(retrievedFilter);
		// The filter may be combined with the default filter, so test behavior instead of instance equality
		assertTrue(retrievedFilter.test(5));   // Should accept positive values
		assertFalse(retrievedFilter.test(-1)); // Should reject negative values
		assertFalse(retrievedFilter.test(null)); // Should reject null values
	}

	@Test
	void o02_getFilter_canBeUsedForOtherPurposes() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var filter = list.getFilter();

		// Use the filter independently
		assertTrue(filter.test(5));
		assertFalse(filter.test(-1));
	}

	//====================================================================================================
	// Null handling for primitive types
	//====================================================================================================

	@Test
	void p01_add_nullForPrimitiveType_throwsException() {
		var list = FilteredList
			.create(int.class)
			.filter(v -> true)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "Cannot set null element for primitive type", () -> {
			list.addConverted(null);
		});
	}

	@Test
	void p02_add_nullForWrapperType_allowed() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> true)  // Accept all, including null
			.build();

		assertTrue(list.add(null));  // Should work for wrapper types

		assertSize(1, list);
		assertNull(list.get(0));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void q01_toString_delegatesToUnderlyingList() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> true)
			.build();
		list.add("value1");
		list.add("value2");

		var underlyingList = new ArrayList<String>();
		underlyingList.add("value1");
		underlyingList.add("value2");

		assertEquals(underlyingList.toString(), list.toString());
	}

	@Test
	void q02_equals_delegatesToUnderlyingList() {
		var list1 = FilteredList
			.create(String.class)
			.filter(v -> true)
			.build();
		list1.add("value1");
		list1.add("value2");

		var list2 = new ArrayList<String>();
		list2.add("value1");
		list2.add("value2");

		assertTrue(list1.equals(list2));
		assertTrue(list2.equals(list1));
	}

	@Test
	void q03_equals_differentContents_returnsFalse() {
		var list1 = FilteredList
			.create(String.class)
			.filter(v -> true)
			.build();
		list1.add("value1");

		var list2 = new ArrayList<String>();
		list2.add("value2");

		assertFalse(list1.equals(list2));
		assertFalse(list2.equals(list1));
	}

	@Test
	void q04_hashCode_delegatesToUnderlyingList() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> true)
			.build();
		list.add("value1");
		list.add("value2");

		var underlyingList = new ArrayList<String>();
		underlyingList.add("value1");
		underlyingList.add("value2");

		assertEquals(underlyingList.hashCode(), list.hashCode());
	}

	//====================================================================================================
	// addAny() method
	//====================================================================================================

	@Test
	void r01_addAny_withCollections() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var list1 = List.of(5, -1);
		var list2 = List.of(10);
		list.addAny(list1, list2);  // Adds 5, 10 (-1 filtered out)

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void r02_addAny_withArrays() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var array1 = new Integer[]{5, -1};
		var array2 = new Integer[]{10};
		list.addAny(array1, array2);  // Adds 5, 10 (-1 filtered out)

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
	}

	@Test
	void r03_addAny_withMixedTypes() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		list.addAny(5, List.of(-1, 10), 15);  // Adds 5, 10, 15 (-1 filtered out)

		assertSize(3, list);
		assertEquals(5, list.get(0));
		assertEquals(10, list.get(1));
		assertEquals(15, list.get(2));
	}

	@Test
	void r04_addAny_nullValues() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.addAny("value1", null, "value2", null);  // nulls ignored

		assertSize(2, list);
		assertEquals("value1", list.get(0));
		assertEquals("value2", list.get(1));
	}

	//====================================================================================================
	// Multiple filters
	//====================================================================================================

	@Test
	void t01_multipleFilters() {
		var list = FilteredList
			.create(Integer.class)
			.filter(v -> v != null)           // First filter
			.filter(v -> v > 0)               // Second filter (ANDed with first)
			.filter(v -> v < 100)             // Third filter (ANDed with previous)
			.build();

		assertTrue(list.add(5));   // Passes all filters
		assertFalse(list.add(null)); // Fails first filter
		assertFalse(list.add(-1));  // Fails second filter
		assertFalse(list.add(0));   // Fails second filter
		assertFalse(list.add(100)); // Fails third filter
		assertTrue(list.add(50));   // Passes all filters

		assertSize(2, list);
		assertEquals(5, list.get(0));
		assertEquals(50, list.get(1));
	}

	//====================================================================================================
	// ListIterator
	//====================================================================================================

	@Test
	void u01_listIterator() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add("value3");

		var iterator = list.listIterator();
		assertTrue(iterator.hasNext());
		assertEquals("value1", iterator.next());
		assertEquals("value2", iterator.next());
		assertEquals("value3", iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test
	void u02_listIterator_atIndex() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add("value3");

		var iterator = list.listIterator(1);
		assertTrue(iterator.hasNext());
		assertEquals("value2", iterator.next());
		assertEquals("value3", iterator.next());
		assertFalse(iterator.hasNext());
	}

	//====================================================================================================
	// subList
	//====================================================================================================

	@Test
	void v01_subList() {
		var list = FilteredList
			.create(String.class)
			.filter(v -> v != null)
			.build();

		list.add("value1");
		list.add("value2");
		list.add("value3");
		list.add("value4");

		var subList = list.subList(1, 3);
		assertSize(2, subList);
		assertEquals("value2", subList.get(0));
		assertEquals("value3", subList.get(1));
	}
}

