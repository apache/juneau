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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ListBuilder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = ListBuilder.create(String.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var list = ListBuilder.create(String.class)
			.add("a")
			.build();

		assertList(list, "a");
	}

	@Test
	void a03_addMultiple() {
		var list = ListBuilder.create(String.class)
			.add("a", "b", "c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void a04_addAll() {
		var existing = l("x", "y", "z");
		var list = ListBuilder.create(String.class)
			.add("a")
			.addAll(existing)
			.add("b")
			.build();

		assertList(list, "a", "x", "y", "z", "b");
	}

	@Test
	void a05_addAllNull() {
		var list = ListBuilder.create(String.class)
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
		var list = ListBuilder.create(String.class)
			.add("a")
			.addIf(true, "b")
			.add("c")
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void b02_addIf_false() {
		var list = ListBuilder.create(String.class)
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
		var list = ListBuilder.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.build();

		assertList(list, "a", "b", "c");
	}

	@Test
	void c02_sorted_customComparator() {
		var list = ListBuilder.create(String.class)
			.add("a", "bb", "ccc")
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(list, "a", "bb", "ccc");
	}

	@Test
	void c03_sorted_integers() {
		var list = ListBuilder.create(Integer.class)
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
		var list = ListBuilder.create(String.class)
			.sparse()
			.build();

		assertNull(list);
	}

	@Test
	void d02_sparse_notEmpty() {
		var list = ListBuilder.create(String.class)
			.add("a")
			.sparse()
			.build();

		assertNotNull(list);
		assertSize(1, list);
	}

	@Test
	void d03_notSparse_empty() {
		var list = ListBuilder.create(String.class)
			.build();

		assertNotNull(list);
		assertSize(0, list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_unmodifiable() {
		var list = ListBuilder.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.build();

		assertSize(3, list);
		assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
	}

	@Test
	void e02_modifiable() {
		var list = ListBuilder.create(String.class)
			.add("a", "b", "c")
			.build();

		list.add("d");
		assertSize(4, list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_copy() {
		var original = new ArrayList<String>();
		original.add("a");

		var list = ListBuilder.create(String.class)
			.to(original)
			.add("b")
			.copy()
			.add("c")
			.build();

		assertSize(3, list);
		assertSize(2, original);  // Original has "a" and "b" added before copy()
		assertNotSame(original, list);  // After copy(), they're different lists
	}

	@Test
	void f02_noCopy() {
		var original = new ArrayList<String>();
		original.add("a");

		var list = ListBuilder.create(String.class)
			.to(original)
			.add("b")
			.build();

		assertSize(2, list);
		assertSize(2, original);  // Original modified
		assertSame(original, list);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Element type
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_elementType() {
		var b = new ListBuilder<String>(null);
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
		var list = ListBuilder.create(String.class)
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
		var list = ListBuilder.create(Integer.class)
			.add(3, 1, 2)
			.sorted()
			.unmodifiable()
			.build();

		assertList(list, 1, 2, 3);
		assertThrows(UnsupportedOperationException.class, () -> list.add(4));
	}

	@Test
	void h03_sparseAndSorted() {
		var list1 = ListBuilder.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.sparse()
			.build();

		assertNotNull(list1);
		assertList(list1, "a", "b", "c");

		var list2 = ListBuilder.create(String.class)
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
		var list = ListBuilder.create(String.class)
			.build();

		assertNotNull(list);
		assertSize(0, list);
	}

	@Test
	void i02_addNullElement() {
		var list = ListBuilder.create(String.class)
			.add("a")
			.add((String)null)
			.add("b")
			.build();

		assertList(list, "a", "<null>", "b");
	}

	@Test
	void i03_duplicateElements() {
		var list = ListBuilder.create(String.class)
			.add("a", "a", "b", "a")
			.build();

		assertList(list, "a", "a", "b", "a");  // Lists allow duplicates
	}

	@Test
	void i04_toExistingList() {
		var existing = new ArrayList<String>();
		existing.add("x");

		var list = ListBuilder.create(String.class)
			.to(existing)
			.add("y")
			.build();

		assertSize(2, list);
		assertTrue(list.contains("x"));
		assertTrue(list.contains("y"));
		assertSame(existing, list);
	}
}

