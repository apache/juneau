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

import static org.apache.juneau.commons.lang.TriState.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.junit.bct.annotations.*;
import org.junit.jupiter.api.*;

class Sets_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = Sets.create(String.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var set = Sets.create(String.class)
			.add("a")
			.build();

		assertList(set, "a");
	}

	@Test
	void a03_addMultiple() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void a04_addAll() {
		var existing = l("x", "y", "z");
		var set = Sets.create(String.class)
			.ordered()
			.add("a")
			.addAll(existing)
			.add("b")
			.build();

		assertList(set, "a", "x", "y", "z", "b");
	}

	@Test
	void a05_addAllNull() {
		var set = Sets.create(String.class)
			.add("a")
			.addAll(null)
			.add("b")
			.build();

		assertList(set, "a", "b");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Deduplication
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_duplicates() {
		var set = Sets.create(String.class)
			.add("a", "b", "a", "c", "b", "a")
			.build();

		assertList(set, "a", "b", "c");  // Duplicates removed
	}

	@Test
	void b02_addAllWithDuplicates() {
		var existing = l("a", "b", "c");
		var set = Sets.create(String.class)
			.add("a", "b")  // a and b already in next collection
			.addAll(existing)
			.add("c", "d")  // c already exists
			.build();

		assertSize(4, set);  // a, b, c, d (no duplicates)
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Conditional adding
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_addIf_true() {
		var set = Sets.create(String.class)
			.add("a")
			.addIf(true, "b")
			.add("c")
			.build();

		assertSize(3, set);
		assertTrue(set.contains("b"));
	}

	@Test
	void c02_addIf_false() {
		var set = Sets.create(String.class)
			.add("a")
			.addIf(false, "b")
			.add("c")
			.build();

		assertSize(2, set);
		assertTrue(set.contains("a"));
		assertFalse(set.contains("b"));
		assertTrue(set.contains("c"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sorting
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_sorted_naturalOrder() {
		var set = Sets.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.build();

		assertList(set, "a", "b", "c");
		assertTrue(set instanceof TreeSet);
	}

	@Test
	void d02_sorted_customComparator() {
		var set = Sets.create(String.class)
			.add("a", "bb", "ccc")
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(set, "a", "bb", "ccc");
	}

	@Test
	void d03_sorted_integers() {
		var set = Sets.create(Integer.class)
			.add(5, 2, 8, 1, 9)
			.sorted()
			.build();

		assertList(set, 1, 2, 5, 8, 9);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sparse mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_sparse_empty() {
		var set = Sets.create(String.class)
			.sparse()
			.build();

		assertNull(set);
	}

	@Test
	void e02_sparse_notEmpty() {
		var set = Sets.create(String.class)
			.add("a")
			.sparse()
			.build();

		assertNotNull(set);
		assertSize(1, set);
	}

	@Test
	void e03_notSparse_empty() {
		var set = Sets.create(String.class)
			.build();

		assertNotNull(set);
		assertEmpty(set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_unmodifiable() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.build();

		assertSize(3, set);
		assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
	}

	@Test
	void f02_modifiable() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.build();

		set.add("d");
		assertSize(4, set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Element type
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_elementType() {
		var b = new Sets<>(String.class);
		b.elementType(String.class);

		var set = b.add("a", "b").build();
		assertSize(2, set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Complex scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_multipleOperations() {
		var existing = l("x", "y");
		var set = Sets.create(String.class)
			.add("a")
			.addAll(existing)
			.addIf(true, "b")
			.addIf(false, "skip")
			.add("c", "d", "a")  // "a" is duplicate
			.sorted()
			.build();

		// Should be sorted, no "skip", no duplicate "a"
		assertList(set, "a", "b", "c", "d", "x", "y");
	}

	@Test
	void i02_sortedAndUnmodifiable() {
		var set = Sets.create(Integer.class)
			.add(3, 1, 2)
			.sorted()
			.unmodifiable()
			.build();

		assertList(set, 1, 2, 3);
		assertThrows(UnsupportedOperationException.class, () -> set.add(4));
	}

	@Test
	void i03_sparseAndSorted() {
		var set1 = Sets.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.sparse()
			.build();

		assertNotNull(set1);
		assertList(set1, "a", "b", "c");

		var set2 = Sets.create(String.class)
			.sorted()
			.sparse()
			.build();

		assertNull(set2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_buildEmptySet() {
		var set = Sets.create(String.class)
			.build();

		assertNotNull(set);
		assertEmpty(set);
	}

	@Test
	void j02_addNullElement() {
		var set = Sets.create(String.class)
			.ordered()
			.add("a")
			.add((String)null)
			.add("b")
			.build();

		assertList(set, "a", "<null>", "b");
	}


	//-----------------------------------------------------------------------------------------------------------------
	// AddAll edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_addAll_whenSetIsNull() {
		// Test addAll when set is null (should create new LinkedHashSet from collection)
		var existing = l("a", "b", "c");
		var set = Sets.create(String.class)
			.addAll(existing)
			.build();

		assertList(set, "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ElementFunction
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_elementFunction_withFunction() {
		var set = Sets.create(Integer.class)
			.elementFunction(o -> {
				if (o instanceof String) {
					return Integer.parseInt((String)o);
				}
				return null;
			})
			.addAny("1", "2", "3")
			.build();

		assertList(set, 1, 2, 3);
	}

	@Test
	void l02_elementFunction_withConverter() {
		var converter = new org.apache.juneau.commons.utils.Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var set = Sets.create(Integer.class)
			.elementFunction(o -> converter.convertTo(Integer.class, o))
			.addAny("1", "2", "3")
			.build();

		assertList(set, 1, 2, 3);
	}

	@Test
	void l03_elementFunction_multipleConverters() {
		var converter1 = new org.apache.juneau.commons.utils.Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				return null;  // Doesn't handle this
			}
		};

		var converter2 = new org.apache.juneau.commons.utils.Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var set = Sets.create(Integer.class)
			.elementFunction(o -> {
				Integer result = converter1.convertTo(Integer.class, o);
				if (result != null) return result;
				return converter2.convertTo(Integer.class, o);
			})
			.addAny("1", "2")
			.build();

		assertList(set, 1, 2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// AddAny
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_addAny_withDirectValues() {
		var set = Sets.create(String.class)
			.addAny("a", "b", "c")
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void m02_addAny_withCollection() {
		var collection = l("a", "b", "c");
		var set = Sets.create(String.class)
			.addAny(collection)
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void m03_addAny_withArray() {
		var array = new String[]{"a", "b", "c"};
		var set = Sets.create(String.class)
			.addAny((Object)array)
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void m04_addAny_withNestedCollection() {
		var nested = l(l("a", "b"), l("c", "d"));
		var set = Sets.create(String.class)
			.addAny(nested)
			.build();

		assertList(set, "a", "b", "c", "d");
	}

	@Test
	void m05_addAny_withNestedArray() {
		var nested = new Object[]{new String[]{"a", "b"}, new String[]{"c", "d"}};
		var set = Sets.create(String.class)
			.addAny(nested)
			.build();

		assertList(set, "a", "b", "c", "d");
	}

	@Test
	void m06_addAny_withNullValues() {
		var set = Sets.create(String.class)
			.addAny("a", null, "b", null, "c")
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void m07_addAny_withTypeConversion() {
		var set = Sets.create(Integer.class)
			.elementFunction(o -> {
				if (o instanceof String) {
					return Integer.parseInt((String)o);
				}
				return null;
			})
			.addAny("1", "2", "3")
			.build();

		assertList(set, 1, 2, 3);
	}

	@Test
	void m08_addAny_withFunctionToCollection() {
		// This test verifies that addAny can handle collections directly.
		// Since elementFunction is for converting to the element type, not to collections,
		// we test that addAny works with collections directly.
		var set = Sets.create(String.class)
			.addAny(l("a", "b", "c"))
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void m09_addAny_noElementType() {
		assertThrows(IllegalArgumentException.class, () -> new Sets<String>(null));
	}

	@Test
	void m10_addAny_noElementFunction_throwsException() {
		// Test line 698: convertElement returns null when elementFunction is null and element can't be converted
		// This causes line 242 to throw an exception
		// When elementFunction is null and we try to add a non-matching type, it should throw
		assertThrows(RuntimeException.class, () -> {
			Sets.create(Integer.class)
				.addAny("not-an-integer")
				.build();
		});
	}

	@Test
	void m11_addAny_elementFunctionReturnsNull() {
		// ElementFunction exists but returns null (can't convert)
		// Should throw RuntimeException when elementFunction can't convert
		assertThrows(RuntimeException.class, () -> {
			Sets.create(Integer.class)
				.elementFunction(o -> null)  // Can't convert
				.addAny("not-an-integer")
				.build();
		});
	}

	@Test
	void m12_addAny_withNullArray() {
		var set = Sets.create(String.class)
			.addAny((Object[])null)
			.build();

		assertEmpty(set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// AddJson
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_addJson() {
		// addJson is a wrapper around addAny, so it should work similarly
		// Note: This will depend on converters being able to parse JSON strings
		// For now, we'll test that it calls addAny correctly
		var set = Sets.create(String.class)
			.addJson("a", "b", "c")
			.build();

		// Since there's no JSON parser converter by default, these are treated as strings
		assertList(set, "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Build edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_build_sparseWithNullSet() {
		var set = Sets.create(String.class)
			.sparse()
			.build();

		assertNull(set);
	}


	@Test
	void o03_build_notSparseWithNullSet() {
		var set = Sets.create(String.class)
			.build();

		assertNotNull(set);
		assertEmpty(set);
	}

	@Test
	void o04_build_sortedWithNullSet() {
		var set = Sets.create(String.class)
			.sorted()
			.build();

		assertNotNull(set);
		assertTrue(set instanceof TreeSet);
		assertEmpty(set);
	}

	@Test
	void o05_build_unmodifiableWithNullSet() {
		var set = Sets.create(String.class)
			.unmodifiable()
			.build();

		assertNotNull(set);
		assertThrows(UnsupportedOperationException.class, () -> set.add("a"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BuildFluent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_buildFluent_returnsFluentSet() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.buildFluent();

		assertNotNull(set);
		assertSize(3, set);
		assertList(set, "a", "b", "c");
	}

	@Test
	void p02_buildFluent_sparseEmpty() {
		var set = Sets.create(String.class)
			.sparse()
			.buildFluent();

		assertNull(set);
	}

	@Test
	void p03_buildFluent_withSorted() {
		var set = Sets.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.buildFluent();

		assertNotNull(set);
		assertList(set, "a", "b", "c");
	}

	@Test
	void p04_buildFluent_withUnmodifiable() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.buildFluent();

		assertNotNull(set);
		assertSize(3, set);
		assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
	}

	@Test
	void p05_buildFluent_fluentMethods() {
		var set = Sets.create(String.class)
			.add("a", "b")
			.buildFluent();

		assertNotNull(set);
		// Test that FluentSet methods work
		set.a("c").aa(l("d", "e"));
		assertList(set, "a", "b", "c", "d", "e");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// buildFiltered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void q01_buildFiltered_returnsFilteredSet() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.buildFiltered();

		assertNotNull(set);
		assertSize(3, set);
		assertList(set, "a", "b", "c");
	}

	@Test
	void q02_buildFiltered_sparseEmpty() {
		var set = Sets.create(String.class)
			.sparse()
			.buildFiltered();

		assertNull(set);
	}

	@Test
	void q03_buildFiltered_withFiltering() {
		var set = Sets.create(Integer.class)
			.filtered(v -> v != null && v > 0)
			.add(5, -1, 10, 0)
			.buildFiltered();

		assertNotNull(set);
		assertList(set, 5, 10);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// concurrent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void r01_concurrent_createsSynchronizedSet() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.concurrent()
			.build();

		assertNotNull(set);
		assertSize(3, set);
	}

	@Test
	void r02_concurrent_withSorted() {
		var set = Sets.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.concurrent()
			.build();

		assertNotNull(set);
		assertList(set, "a", "b", "c");
	}

	@Test
	void r03_concurrent_withOrdered() {
		var set = Sets.create(String.class)
			.add("c", "a", "b")
			.ordered()
			.concurrent()
			.build();

		assertNotNull(set);
		assertSize(3, set);
		assertTrue(set.contains("a"));
		assertTrue(set.contains("b"));
		assertTrue(set.contains("c"));
		// Order may not be preserved when concurrent is set, so we just verify all elements are present
	}

	//-----------------------------------------------------------------------------------------------------------------
	// concurrent(boolean)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void s01_concurrent_boolean_true() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.concurrent(true)
			.build();

		assertNotNull(set);
		assertSize(3, set);
	}

	@Test
	void s02_concurrent_boolean_false() {
		var set = Sets.create(String.class)
			.add("a", "b", "c")
			.concurrent(false)
			.build();

		assertNotNull(set);
		assertSize(3, set);
		// Should not be synchronized when false
	}

	@Test
	void s03_concurrent_boolean_withSorted() {
		var set = Sets.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.concurrent(true)
			.build();

		assertNotNull(set);
		assertList(set, "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// filtered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void t01_filtered_defaultFiltering() {
		var set = Sets.create(Object.class)
			.filtered()
			.add("a", null, false, -1, new String[0], l(), m())
			.build();

		assertList(set, "a");
	}

	@Test
	void t02_filtered_withBooleanFalse() {
		var set = Sets.create(Boolean.class)
			.filtered()
			.add(true, false, true)
			.build();

		assertList(set, true);
	}

	@Test
	void t03_filtered_withNumberMinusOne() {
		var set = Sets.create(Integer.class)
			.filtered()
			.add(1, -1, 2, -1, 3)
			.build();

		assertList(set, 1, 2, 3);
	}

	@Test
	void t04_filtered_withEmptyArray() {
		var set = Sets.create(Object.class)
			.filtered()
			.add("a", new String[0], "b")
			.build();

		assertList(set, "a", "b");
	}

	@Test
	void t05_filtered_withEmptyMap() {
		var set = Sets.create(Object.class)
			.filtered()
			.add("a", m(), "b")
			.build();

		assertList(set, "a", "b");
	}

	@Test
	void t06_filtered_withEmptyCollection() {
		var set = Sets.create(Object.class)
			.filtered()
			.add("a", l(), "b")
			.build();

		assertList(set, "a", "b");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// filtered(Predicate)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void u01_filtered_withPredicate() {
		var set = Sets.create(Integer.class)
			.filtered(v -> v != null && v > 0)
			.add(5, -1, 10, 0, 15)
			.build();

		assertList(set, 5, 10, 15);
	}

	@Test
	@BctConfig(sortCollections = TRUE)
	void u02_filtered_multipleFilters() {
		var set = Sets.create(Integer.class)
			.filtered(v -> v != null)
			.filtered(v -> v > 0)
			.filtered(v -> v < 100)
			.add(5, -1, 150, 0, 50, null)
			.build();

		assertList(set, 5, 50);
	}

	@Test
	void u03_filtered_withStringPredicate() {
		var set = Sets.create(String.class)
			.filtered(s -> s != null && s.length() > 2)
			.add("a", "ab", "abc", "abcd", "")
			.build();

		assertList(set, "abc", "abcd");
	}
}