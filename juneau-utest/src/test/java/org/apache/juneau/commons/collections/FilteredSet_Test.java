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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FilteredSet_Test extends TestBase {

	//====================================================================================================
	// Basic filtering - filter out null values
	//====================================================================================================

	@Test
	void a01_filterNullValues_add() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(set.add("value1"));  // Added
		assertFalse(set.add(null));      // Filtered out
		assertTrue(set.add("value3"));  // Added

		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains("value3"));
		assertFalse(set.contains(null));
	}

	@Test
	void a02_filterNullValues_addAll() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		var source = list("value1", null, "value3", null);

		assertTrue(set.addAll(source));

		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains("value3"));
		assertFalse(set.contains(null));
	}

	@Test
	void a03_filterNullValues_duplicateElements() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(set.add("value1"));  // Added
		assertFalse(set.add("value1")); // Already present, returns false
		assertTrue(set.add("value2"));  // Added

		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains("value2"));
	}

	//====================================================================================================
	// Filter based on value - positive numbers only
	//====================================================================================================

	@Test
	void b01_filterPositiveNumbers() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(set.add(5));   // Added
		assertFalse(set.add(-1));  // Filtered out
		assertFalse(set.add(0));   // Filtered out
		assertTrue(set.add(10));  // Added
		assertFalse(set.add(null)); // Filtered out

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
		assertFalse(set.contains(-1));
		assertFalse(set.contains(0));
	}

	//====================================================================================================
	// Filter based on string length
	//====================================================================================================

	@Test
	void c01_filterStringLength() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null && v.length() > 3)
			.build();

		assertTrue(set.add("short"));   // Added
		assertFalse(set.add("ab"));      // Filtered out (length <= 3)
		assertTrue(set.add("longer"));  // Added
		assertFalse(set.add(null));      // Filtered out

		assertSize(2, set);
		assertTrue(set.contains("short"));
		assertTrue(set.contains("longer"));
		assertFalse(set.contains("ab"));
	}

	//====================================================================================================
	// Custom set types - TreeSet
	//====================================================================================================

	@Test
	void d01_customSetType_TreeSet() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.inner(new TreeSet<>())
			.build();

		set.add(5);
		set.add(-1);  // Filtered out
		set.add(10);
		set.add(3);

		// TreeSet maintains sorted order
		var elements = new ArrayList<>(set);
		assertEquals(List.of(3, 5, 10), elements);
	}

	//====================================================================================================
	// Custom set types - CopyOnWriteArraySet
	//====================================================================================================

	@Test
	void d02_customSetType_CopyOnWriteArraySet() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.inner(new CopyOnWriteArraySet<>())
			.build();

		set.add("value1");
		set.add(null);  // Filtered out
		set.add("value3");

		assertSize(2, set);
	}

	//====================================================================================================
	// Set interface methods - contains, containsAll
	//====================================================================================================

	@Test
	void e01_setInterface_contains() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add(null);  // Filtered out

		assertTrue(set.contains("value1"));
		assertFalse(set.contains(null));  // null was filtered out
	}

	@Test
	void e02_setInterface_containsAll() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		set.add("value3");

		assertTrue(set.containsAll(List.of("value1", "value2")));
		assertFalse(set.containsAll(List.of("value1", "value4")));
		assertFalse(set.containsAll(Arrays.asList("value1", (String)null)));  // null was filtered out
	}

	@Test
	void e03_setInterface_size() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertEquals(0, set.size());
		set.add("value1");
		assertEquals(1, set.size());
		set.add(null);  // Filtered out
		assertEquals(1, set.size());
		set.add("value3");
		assertEquals(2, set.size());
		set.add("value1");  // Duplicate, not added
		assertEquals(2, set.size());
	}

	@Test
	void e04_setInterface_isEmpty() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(set.isEmpty());
		set.add("value1");
		assertFalse(set.isEmpty());
	}

	@Test
	void e05_setInterface_clear() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		assertSize(2, set);

		set.clear();
		assertTrue(set.isEmpty());
		assertSize(0, set);
	}

	@Test
	void e06_setInterface_remove() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");

		assertTrue(set.remove("value1"));
		assertFalse(set.remove("value1"));  // Already removed
		assertFalse(set.remove("nonexistent"));

		assertSize(1, set);
		assertTrue(set.contains("value2"));
	}

	@Test
	void e07_setInterface_removeAll() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		set.add("value3");

		assertTrue(set.removeAll(List.of("value1", "value2")));
		assertSize(1, set);
		assertTrue(set.contains("value3"));
	}

	@Test
	void e08_setInterface_retainAll() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		set.add("value3");

		assertTrue(set.retainAll(List.of("value1", "value2")));
		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains("value2"));
		assertFalse(set.contains("value3"));
	}

	//====================================================================================================
	// Edge cases - empty set
	//====================================================================================================

	@Test
	void f01_emptySet() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(set.isEmpty());
		assertSize(0, set);
		assertFalse(set.contains("any"));
	}

	//====================================================================================================
	// Builder validation
	//====================================================================================================

	@Test
	void g01_builder_noFilter_acceptsAllElements() {
		// Filter is optional - defaults to v -> true (accepts all elements)
		var set = FilteredSet.create(String.class)
			.build();

		assertNotNull(set);
		assertTrue(set.add("value1"));
		assertTrue(set.add(null));  // Should be accepted (no filter)
		assertTrue(set.add("value3"));

		assertSize(3, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains(null));
		assertTrue(set.contains("value3"));
	}

	@Test
	void g02_builder_nullFilter_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredSet.create(String.class).filter(null);
		});
	}

	@Test
	void g03_builder_nullInner_throwsException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "value", () -> {
			FilteredSet.create(String.class)
				.filter(v -> true)
				.inner(null);
		});
	}

	//====================================================================================================
	// Builder - create() without parameters
	//====================================================================================================

	@Test
	void h01_builder_createWithoutParameters() {
		var set = FilteredSet
			.<String>create()
			.filter(v -> v != null)
			.build();

		assertTrue(set.add("value1"));
		assertFalse(set.add(null));  // Filtered out

		assertSize(1, set);
		assertTrue(set.contains("value1"));
	}

	@Test
	void h02_builder_createWithoutParameters_usesObjectClass() {
		var set = FilteredSet
			.create()
			.filter(v -> v != null)
			.build();

		// Object.class accepts any type
		set.add("string");
		set.add(123);
		set.add(List.of(1, 2));

		assertSize(3, set);
		assertTrue(set.contains("string"));
		assertTrue(set.contains(123));
		assertTrue(set.contains(List.of(1, 2)));
	}

	//====================================================================================================
	// add() method - type validation with types specified
	//====================================================================================================

	@Test
	void i01_add_withTypeValidation_validTypes() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(set.add(5));   // Valid types, added
		assertTrue(set.add(10));  // Valid types, added
		assertFalse(set.add(-1));  // Valid types, but filtered out

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void i02_add_withTypeValidation_invalidType() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "could not be converted to element type", () -> {
			set.addConverted("value");  // Invalid type (String instead of Integer)
		});
	}

	@Test
	void i03_add_withTypeValidation_noTypesSpecified() {
		var set = FilteredSet
			.<Object>create()
			.filter(v -> v != null)
			.build();

		// Object.class is used when types not specified, which accepts any type
		set.add("value1");
		set.add(123);  // Different types, but Object.class accepts any type

		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains(123));
	}

	//====================================================================================================
	// add() method - with elementFunction
	//====================================================================================================

	@Test
	void j01_add_withElementFunction() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		assertTrue(set.addConverted("5"));   // Element converted from String to Integer
		assertTrue(set.addConverted("10"));  // Element converted from String to Integer
		assertFalse(set.addConverted("-1"));  // Element converted, but filtered out

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void j02_add_withElementFunction_noTypeSpecified() {
		var set = FilteredSet
			.<Integer>create()
			.filter(v -> v != null)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		assertTrue(set.addConverted("123"));  // Element converted using function

		assertSize(1, set);
		assertTrue(set.contains(123));
	}

	//====================================================================================================
	// addAll() method
	//====================================================================================================

	@Test
	void k01_addAll_withTypeValidation() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var source = List.of(5, -1, 10);

		assertTrue(set.addAll(source));

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void k02_addAll_withElementFunction() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.elementFunction(o -> Integer.parseInt(o.toString()))
			.build();

		var source = List.of("5", "-1", "10");

		set.addAllConverted(source);

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void k03_addAll_nullSource() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.addAllConverted(null);  // Should be no-op

		assertTrue(set.isEmpty());
		assertSize(0, set);
	}

	@Test
	void k04_addAll_emptySource() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertFalse(set.addAll(Set.of()));  // Empty set

		assertTrue(set.isEmpty());
		assertSize(0, set);
	}

	@Test
	void k05_addAll_duplicateElements() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		set.add(5);
		assertTrue(set.addAll(List.of(5, 10)));  // 5 already present, 10 added - returns true because set was modified
		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	//====================================================================================================
	// add() method - return value
	//====================================================================================================

	@Test
	void l01_add_returnValue_newElement() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		assertTrue(set.add("value1"));  // New element, returns true
	}

	@Test
	void l02_add_returnValue_duplicateElement() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		assertFalse(set.add("value1"));  // Duplicate, returns false
		assertSize(1, set);
	}

	@Test
	void l03_add_returnValue_filteredOut() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		assertFalse(set.add(null));  // Filtered out, returns false
		assertTrue(set.contains("value1"));  // Old value still there
	}

	//====================================================================================================
	// add() method - edge cases with functions
	//====================================================================================================

	@Test
	void m01_add_elementFunctionThrowsException() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null)
			.elementFunction(o -> {
				if (o == null)
					throw new IllegalArgumentException("Element cannot be null");
				return Integer.parseInt(o.toString());
			})
			.build();

		assertThrows(IllegalArgumentException.class, () -> {
			set.addConverted(null);
		});
	}

	@Test
	void m02_add_elementFunctionReturnsNull() {
		var set = FilteredSet
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

		assertTrue(set.addConverted("5"));   // Valid, added
		assertFalse(set.addConverted("abc")); // Function returns null, filtered out

		assertSize(1, set);
		assertTrue(set.contains(5));
	}

	//====================================================================================================
	// wouldAccept() method
	//====================================================================================================

	@Test
	void n01_wouldAccept_returnsTrue() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertTrue(set.wouldAccept(5));
		assertTrue(set.wouldAccept(10));
	}

	@Test
	void n02_wouldAccept_returnsFalse() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		assertFalse(set.wouldAccept(null));
		assertFalse(set.wouldAccept(-1));
		assertFalse(set.wouldAccept(0));
	}

	@Test
	void n03_wouldAccept_usedForPreValidation() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		// Pre-validate before adding
		if (set.wouldAccept(5)) {
			set.add(5);
		}
		if (set.wouldAccept(-1)) {
			set.add(-1);
		}

		assertSize(1, set);
		assertTrue(set.contains(5));
		assertFalse(set.contains(-1));
	}

	//====================================================================================================
	// getFilter() method
	//====================================================================================================

	@Test
	void o01_getFilter_returnsFilter() {
		var originalFilter = (Predicate<Integer>)(v -> v != null && v > 0);
		var set = FilteredSet
			.create(Integer.class)
			.filter(originalFilter)
			.build();

		var retrievedFilter = set.getFilter();
		assertNotNull(retrievedFilter);
		// The filter may be combined with the default filter, so test behavior instead of instance equality
		assertTrue(retrievedFilter.test(5));   // Should accept positive values
		assertFalse(retrievedFilter.test(-1)); // Should reject negative values
		assertFalse(retrievedFilter.test(null)); // Should reject null values
	}

	@Test
	void o02_getFilter_canBeUsedForOtherPurposes() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var filter = set.getFilter();

		// Use the filter independently
		assertTrue(filter.test(5));
		assertFalse(filter.test(-1));
	}

	//====================================================================================================
	// Null handling for primitive types
	//====================================================================================================

	@Test
	void p01_add_nullForPrimitiveType_throwsException() {
		var set = FilteredSet
			.create(int.class)
			.filter(v -> true)
			.build();

		assertThrowsWithMessage(RuntimeException.class, "Cannot set null element for primitive type", () -> {
			set.addConverted(null);
		});
	}

	@Test
	void p02_add_nullForWrapperType_allowed() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> true)  // Accept all, including null
			.build();

		assertTrue(set.add(null));  // Should work for wrapper types

		assertSize(1, set);
		assertTrue(set.contains(null));
	}

	//====================================================================================================
	// toString(), equals(), hashCode()
	//====================================================================================================

	@Test
	void q01_toString_delegatesToUnderlyingSet() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> true)
			.build();
		set.add("value1");
		set.add("value2");

		var underlyingSet = new LinkedHashSet<String>();
		underlyingSet.add("value1");
		underlyingSet.add("value2");

		assertEquals(underlyingSet.toString(), set.toString());
	}

	@Test
	void q02_equals_delegatesToUnderlyingSet() {
		var set1 = FilteredSet
			.create(String.class)
			.filter(v -> true)
			.build();
		set1.add("value1");
		set1.add("value2");

		var set2 = new LinkedHashSet<String>();
		set2.add("value1");
		set2.add("value2");

		assertEquals(set1.size(), set2.size());
		assertEquals(new LinkedHashSet<>(set1), set2);
		assertEquals(set2, new LinkedHashSet<>(set1));
	}

	@Test
	void q03_equals_differentContents_returnsFalse() {
		var set1 = FilteredSet
			.create(String.class)
			.filter(v -> true)
			.build();
		set1.add("value1");

		var set2 = new LinkedHashSet<String>();
		set2.add("value2");

		assertNotEquals(set1, set2);
		assertNotEquals(set2, set1);
	}

	@Test
	void q04_hashCode_delegatesToUnderlyingSet() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> true)
			.build();
		set.add("value1");
		set.add("value2");

		var underlyingSet = new LinkedHashSet<String>();
		underlyingSet.add("value1");
		underlyingSet.add("value2");

		assertEquals(underlyingSet.hashCode(), set.hashCode());
	}

	//====================================================================================================
	// addAny() method
	//====================================================================================================

	@Test
	void r01_addAny_withCollections() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var set1 = Set.of(5, -1);
		var set2 = Set.of(10);
		set.addAny(set1, set2);  // Adds 5, 10 (-1 filtered out)

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void r02_addAny_withArrays() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		var array1 = new Integer[]{5, -1};
		var array2 = new Integer[]{10};
		set.addAny(array1, array2);  // Adds 5, 10 (-1 filtered out)

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
	}

	@Test
	void r03_addAny_withMixedTypes() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null && v > 0)
			.build();

		set.addAny(5, Set.of(-1, 10), 15);  // Adds 5, 10, 15 (-1 filtered out)

		assertSize(3, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(10));
		assertTrue(set.contains(15));
	}

	@Test
	void r04_addAny_nullValues() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.addAny("value1", null, "value2", null);  // nulls ignored

		assertSize(2, set);
		assertTrue(set.contains("value1"));
		assertTrue(set.contains("value2"));
	}

	//====================================================================================================
	// Multiple filters
	//====================================================================================================

	@Test
	void t01_multipleFilters() {
		var set = FilteredSet
			.create(Integer.class)
			.filter(v -> v != null)           // First filter
			.filter(v -> v > 0)               // Second filter (ANDed with first)
			.filter(v -> v < 100)             // Third filter (ANDed with previous)
			.build();

		assertTrue(set.add(5));   // Passes all filters
		assertFalse(set.add(null)); // Fails first filter
		assertFalse(set.add(-1));  // Fails second filter
		assertFalse(set.add(0));   // Fails second filter
		assertFalse(set.add(100)); // Fails third filter
		assertTrue(set.add(50));   // Passes all filters

		assertSize(2, set);
		assertTrue(set.contains(5));
		assertTrue(set.contains(50));
	}

	//====================================================================================================
	// Iterator
	//====================================================================================================

	@Test
	void u01_iterator() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		set.add("value3");

		var iterator = set.iterator();
		var found = new HashSet<String>();
		while (iterator.hasNext()) {
			found.add(iterator.next());
		}

		assertSize(3, found);
		assertTrue(found.contains("value1"));
		assertTrue(found.contains("value2"));
		assertTrue(found.contains("value3"));
	}

	@Test
	void u02_iterator_remove() {
		var set = FilteredSet
			.create(String.class)
			.filter(v -> v != null)
			.build();

		set.add("value1");
		set.add("value2");
		set.add("value3");

		var iterator = set.iterator();
		iterator.next();
		iterator.remove();

		assertSize(2, set);
	}
}

