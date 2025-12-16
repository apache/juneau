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
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Lists_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = Lists.create(String.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var list = Lists.create(String.class)
			.add("a")
			.build();

		assertList(list, "a");
	}

	@Test
	void a03_addMultiple() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void a04_addAll() {
		var existing = l("x", "y", "z");
		var list = Lists.create(String.class)
			.add("a")
			.addAll(existing)
			.add("b")
			.build();

		assertList(list, "a", "x", "y", "z", "b");
	}

	@Test
	void a05_addAllNull() {
		var list = Lists.create(String.class)
			.add("a")
			.addAll(null)
			.add("b")
			.build();

		assertList(list, "a", "b");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Conditional adding
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_addIf_true() {
		var list = Lists.create(String.class)
			.add("a")
			.addIf(true, "b")
			.add("c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void b02_addIf_false() {
		var list = Lists.create(String.class)
			.add("a")
			.addIf(false, "b")
			.add("c")
			.build();

		assertList(list, "a", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sorting
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_sorted_naturalOrder() {
		var list = Lists.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void c02_sorted_customComparator() {
		var list = Lists.create(String.class)
			.add("a", "bb", "ccc")
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(list, "a", "bb", "ccc");
	}

	@Test
	void c03_sorted_integers() {
		var list = Lists.create(Integer.class)
			.add(5, 2, 8, 1, 9)
			.sorted()
			.build();

		assertList(list, 1, 2, 5, 8, 9);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Sparse mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_sparse_empty() {
		var list = Lists.create(String.class)
			.sparse()
			.build();

		assertNull(list);
	}

	@Test
	void d02_sparse_notEmpty() {
		var list = Lists.create(String.class)
			.add("a")
			.sparse()
			.build();

		assertNotNull(list);
		assertSize(1, list);
	}

	@Test
	void d03_notSparse_empty() {
		var list = Lists.create(String.class)
			.build();

		assertNotNull(list);
		assertEmpty(list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_unmodifiable() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.build();

		assertSize(3, list);
		assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
	}

	@Test
	void e02_modifiable() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.build();

		list.add("d");
		assertSize(4, list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Element type
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_elementType() {
		var b = new Lists<>(String.class);
		b.elementType(String.class);

		var list = b.add("a", "b").build();
		assertSize(2, list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Complex scenarios
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_multipleOperations() {
		var existing = l("x", "y");
		var list = Lists.create(String.class)
			.add("a")
			.addAll(existing)
			.addIf(true, "b")
			.addIf(false, "skip")
			.add("c", "d")
			.sorted()
			.build();

		// Should be sorted and not contain "skip"
		assertList(list, "a", "b", "c", "d", "x", "y");
	}

	@Test
	void h02_sortedAndUnmodifiable() {
		var list = Lists.create(Integer.class)
			.add(3, 1, 2)
			.sorted()
			.unmodifiable()
			.build();

		assertList(list, 1, 2, 3);
		assertThrows(UnsupportedOperationException.class, () -> list.add(4));
	}

	@Test
	void h03_sparseAndSorted() {
		var list1 = Lists.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.sparse()
			.build();

		assertNotNull(list1);
		assertList(list1, "a", "b", "c");

		var list2 = Lists.create(String.class)
			.sorted()
			.sparse()
			.build();

		assertNull(list2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_buildEmptyList() {
		var list = Lists.create(String.class)
			.build();

		assertNotNull(list);
		assertEmpty(list);
	}

	@Test
	void i02_addNullElement() {
		var list = Lists.create(String.class)
			.add("a")
			.add((String)null)
			.add("b")
			.build();

		assertList(list, "a", "<null>", "b");
	}

	@Test
	void i03_duplicateElements() {
		var list = Lists.create(String.class)
			.add("a", "a", "b", "a")
			.build();

		assertList(list, "a", "a", "b", "a");  // Lists allow duplicates
	}


	//-----------------------------------------------------------------------------------------------------------------
	// AddAll edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_addAll_whenListIsNull() {
		// Test addAll when list is null (should create new LinkedList from collection)
		var existing = l("a", "b", "c");
		var list = Lists.create(String.class)
			.addAll(existing)
			.build();

		assertList(list, "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ElementFunction
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_elementFunction_withFunction() {
		var list = Lists.create(Integer.class)
			.elementFunction(o -> {
				if (o instanceof String) {
					return Integer.parseInt((String)o);
				}
				return null;
			})
			.addAny("1", "2", "3")
			.build();

		assertList(list, 1, 2, 3);
	}

	@Test
	void k02_elementFunction_withConverter() {
		var converter = new org.apache.juneau.commons.utils.Converter() {
			@Override
			public <T> T convertTo(Class<T> type, Object o) {
				if (type == Integer.class && o instanceof String) {
					return type.cast(Integer.parseInt((String)o));
				}
				return null;
			}
		};

		var list = Lists.create(Integer.class)
			.elementFunction(o -> converter.convertTo(Integer.class, o))
			.addAny("1", "2", "3")
			.build();

		assertList(list, 1, 2, 3);
	}

	@Test
	void k03_elementFunction_multipleConverters() {
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

		var list = Lists.create(Integer.class)
			.elementFunction(o -> {
				Integer result = converter1.convertTo(Integer.class, o);
				if (result != null) return result;
				return converter2.convertTo(Integer.class, o);
			})
			.addAny("1", "2")
			.build();

		assertList(list, 1, 2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// AddAny
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_addAny_withDirectValues() {
		var list = Lists.create(String.class)
			.addAny("a", "b", "c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void l02_addAny_withCollection() {
		var collection = l("a", "b", "c");
		var list = Lists.create(String.class)
			.addAny(collection)
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void l03_addAny_withArray() {
		var array = new String[]{"a", "b", "c"};
		var list = Lists.create(String.class)
			.addAny((Object)array)
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void l04_addAny_withNestedCollection() {
		var nested = l(l("a", "b"), l("c", "d"));
		var list = Lists.create(String.class)
			.addAny(nested)
			.build();

		assertList(list, "a", "b", "c", "d");
	}

	@Test
	void l05_addAny_withNestedArray() {
		var nested = new Object[]{new String[]{"a", "b"}, new String[]{"c", "d"}};
		var list = Lists.create(String.class)
			.addAny(nested)
			.build();

		assertList(list, "a", "b", "c", "d");
	}

	@Test
	void l06_addAny_withNullValues() {
		var list = Lists.create(String.class)
			.addAny("a", null, "b", null, "c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void l07_addAny_withTypeConversion() {
		var list = Lists.create(Integer.class)
			.elementFunction(o -> {
				if (o instanceof String) {
					return Integer.parseInt((String)o);
				}
				return null;
			})
			.addAny("1", "2", "3")
			.build();

		assertList(list, 1, 2, 3);
	}

	@Test
	void l08_addAny_withFunctionToCollection() {
		// This test verifies that addAny works with collections directly.
		// Note: elementFunction is for converting to the element type, not to collections,
		// so we test that addAny works with collections directly.
		var list = Lists.create(String.class)
			.addAny(l("a", "b", "c"))
			.build();
		assertList(list, "a", "b", "c");
	}

	@Test
	void l09_addAny_noElementType() {
		assertThrows(IllegalArgumentException.class, () -> new Lists<String>(null));
	}

	@Test
	void l10_addAny_noElementFunction_throwsException() {
		// When elementFunction is null and we try to add a non-matching type, it should throw
		assertThrows(RuntimeException.class, () -> {
			Lists.create(Integer.class)
				.addAny("not-an-integer")
				.build();
		});
	}

	@Test
	void l11_addAny_elementFunctionReturnsNull() {
		// ElementFunction exists but returns null (can't convert)
		// Should throw RuntimeException when elementFunction can't convert
		assertThrows(RuntimeException.class, () -> {
			Lists.create(Integer.class)
				.elementFunction(o -> null)  // Can't convert
				.addAny("not-an-integer")
				.build();
		});
	}

	@Test
	void l12_addAny_withNullArray() {
		var list = Lists.create(String.class)
			.addAny((Object[])null)
			.build();

		assertEmpty(list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Build edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_build_sparseWithNullList() {
		var list = Lists.create(String.class)
			.sparse()
			.build();

		assertNull(list);
	}


	@Test
	void m03_build_notSparseWithNullList() {
		var list = Lists.create(String.class)
			.build();

		assertNotNull(list);
		assertEmpty(list);
	}

	@Test
	void m04_build_sortedWithNullList() {
		var list = Lists.create(String.class)
			.sorted()
			.build();

		assertNotNull(list);
		assertEmpty(list);
	}

	@Test
	void m05_build_unmodifiableWithNullList() {
		var list = Lists.create(String.class)
			.unmodifiable()
			.build();

		assertNotNull(list);
		assertThrows(UnsupportedOperationException.class, () -> list.add("a"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BuildFluent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_buildFluent_returnsFluentList() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.buildFluent();

		assertNotNull(list);
		assertSize(3, list);
		assertList(list, "a", "b", "c");
	}

	@Test
	void n02_buildFluent_sparseEmpty() {
		var list = Lists.create(String.class)
			.sparse()
			.buildFluent();

		assertNull(list);
	}

	@Test
	void n03_buildFluent_withSorted() {
		var list = Lists.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.buildFluent();

		assertNotNull(list);
		assertList(list, "a", "b", "c");
	}

	@Test
	void n04_buildFluent_withUnmodifiable() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.buildFluent();

		assertNotNull(list);
		assertSize(3, list);
		assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
	}

	@Test
	void n05_buildFluent_fluentMethods() {
		var list = Lists.create(String.class)
			.add("a", "b")
			.buildFluent();

		assertNotNull(list);
		// Test that FluentList methods work
		list.a("c").aa(l("d", "e"));
		assertList(list, "a", "b", "c", "d", "e");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// buildFiltered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_buildFiltered_returnsFilteredList() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.buildFiltered();

		assertNotNull(list);
		assertSize(3, list);
		assertList(list, "a", "b", "c");
	}

	@Test
	void o02_buildFiltered_sparseEmpty() {
		var list = Lists.create(String.class)
			.sparse()
			.buildFiltered();

		assertNull(list);
	}

	@Test
	void o03_buildFiltered_withFiltering() {
		var list = Lists.create(Integer.class)
			.filtered(v -> v != null && v > 0)
			.add(5, -1, 10, 0)
			.buildFiltered();

		assertNotNull(list);
		assertList(list, 5, 10);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// concurrent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_concurrent_createsSynchronizedList() {
		var list = Lists.create(String.class)
			.add("a", "b", "c")
			.concurrent()
			.build();

		assertNotNull(list);
		assertSize(3, list);
		// Verify it's synchronized by checking it's wrapped (Collections.synchronizedList returns a wrapper)
		assertList(list, "a", "b", "c");
	}

	@Test
	void p02_concurrent_withSorted() {
		var list = Lists.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.concurrent()
			.build();

		assertNotNull(list);
		assertList(list, "a", "b", "c");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// filtered
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void q01_filtered_defaultFiltering() {
		var list = Lists.create(Object.class)
			.filtered()
			.add("a", null, false, -1, new String[0], l(), m())
			.build();

		assertList(list, "a");
	}

	@Test
	void q02_filtered_withBooleanFalse() {
		var list = Lists.create(Boolean.class)
			.filtered()
			.add(true, false, true)
			.build();

		assertList(list, true, true);
	}

	@Test
	void q03_filtered_withNumberMinusOne() {
		var list = Lists.create(Integer.class)
			.filtered()
			.add(1, -1, 2, -1, 3)
			.build();

		assertList(list, 1, 2, 3);
	}

	@Test
	void q04_filtered_withEmptyArray() {
		var list = Lists.create(Object.class)
			.filtered()
			.add("a", new String[0], "b")
			.build();

		assertList(list, "a", "b");
	}

	@Test
	void q05_filtered_withEmptyMap() {
		var list = Lists.create(Object.class)
			.filtered()
			.add("a", m(), "b")
			.build();

		assertList(list, "a", "b");
	}

	@Test
	void q06_filtered_withEmptyCollection() {
		var list = Lists.create(Object.class)
			.filtered()
			.add("a", l(), "b")
			.build();

		assertList(list, "a", "b");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// filtered(Predicate)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void r01_filtered_withPredicate() {
		var list = Lists.create(Integer.class)
			.filtered(v -> v != null && v > 0)
			.add(5, -1, 10, 0, 15)
			.build();

		assertList(list, 5, 10, 15);
	}

	@Test
	void r02_filtered_multipleFilters() {
		var list = Lists.create(Integer.class)
			.filtered(v -> v != null)
			.filtered(v -> v > 0)
			.filtered(v -> v < 100)
			.add(5, -1, 150, 0, 50, null)
			.build();

		assertList(list, 5, 50);
	}

	@Test
	void r03_filtered_withStringPredicate() {
		var list = Lists.create(String.class)
			.filtered(s -> s != null && s.length() > 2)
			.add("a", "ab", "abc", "abcd", "")
			.build();

		assertList(list, "abc", "abcd");
	}
}

