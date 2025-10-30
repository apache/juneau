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

class SetBuilder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_create() {
		var b = SetBuilder.create(String.class);
		assertNotNull(b);
	}

	@Test
	void a02_addSingle() {
		var set = SetBuilder.create(String.class)
			.add("a")
			.build();

		assertList(set, "a");
	}

	@Test
	void a03_addMultiple() {
		var set = SetBuilder.create(String.class)
			.add("a", "b", "c")
			.build();

		assertList(set, "a", "b", "c");
	}

	@Test
	void a04_addAll() {
		var existing = l("x", "y", "z");
		var set = SetBuilder.create(String.class)
			.add("a")
			.addAll(existing)
			.add("b")
			.build();

		assertList(set, "a", "x", "y", "z", "b");
	}

	@Test
	void a05_addAllNull() {
		var set = SetBuilder.create(String.class)
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
		var set = SetBuilder.create(String.class)
			.add("a", "b", "a", "c", "b", "a")
			.build();

		assertList(set, "a", "b", "c");  // Duplicates removed
	}

	@Test
	void b02_addAllWithDuplicates() {
		var existing = l("a", "b", "c");
		var set = SetBuilder.create(String.class)
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
		var set = SetBuilder.create(String.class)
			.add("a")
			.addIf(true, "b")
			.add("c")
			.build();

		assertSize(3, set);
		assertTrue(set.contains("b"));
	}

	@Test
	void c02_addIf_false() {
		var set = SetBuilder.create(String.class)
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
		var set = SetBuilder.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.build();

		assertList(set, "a", "b", "c");
		assertTrue(set instanceof TreeSet);
	}

	@Test
	void d02_sorted_customComparator() {
		var set = SetBuilder.create(String.class)
			.add("a", "bb", "ccc")
			.sorted(Comparator.comparing(String::length))
			.build();

		assertList(set, "a", "bb", "ccc");
	}

	@Test
	void d03_sorted_integers() {
		var set = SetBuilder.create(Integer.class)
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
		var set = SetBuilder.create(String.class)
			.sparse()
			.build();

		assertNull(set);
	}

	@Test
	void e02_sparse_notEmpty() {
		var set = SetBuilder.create(String.class)
			.add("a")
			.sparse()
			.build();

		assertNotNull(set);
		assertSize(1, set);
	}

	@Test
	void e03_notSparse_empty() {
		var set = SetBuilder.create(String.class)
			.build();

		assertNotNull(set);
		assertEmpty(set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_unmodifiable() {
		var set = SetBuilder.create(String.class)
			.add("a", "b", "c")
			.unmodifiable()
			.build();

		assertSize(3, set);
		assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
	}

	@Test
	void f02_modifiable() {
		var set = SetBuilder.create(String.class)
			.add("a", "b", "c")
			.build();

		set.add("d");
		assertSize(4, set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Copy mode
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_copy() {
		var original = new LinkedHashSet<String>();
		original.add("a");

		var set = SetBuilder.create(String.class)
			.to(original)
			.add("b")
			.copy()
			.add("c")
			.build();

		assertSize(3, set);
		assertSize(2, original);  // Original has "a" and "b" added before copy()
		assertNotSame(original, set);  // After copy(), they're different sets
	}

	@Test
	void g02_noCopy() {
		var original = new LinkedHashSet<String>();
		original.add("a");

		var set = SetBuilder.create(String.class)
			.to(original)
			.add("b")
			.build();

		assertSize(2, set);
		assertSize(2, original);  // Original modified
		assertSame(original, set);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Element type
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_elementType() {
		var b = new SetBuilder<String>(null);
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
		var set = SetBuilder.create(String.class)
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
		var set = SetBuilder.create(Integer.class)
			.add(3, 1, 2)
			.sorted()
			.unmodifiable()
			.build();

		assertList(set, 1, 2, 3);
		assertThrows(UnsupportedOperationException.class, () -> set.add(4));
	}

	@Test
	void i03_sparseAndSorted() {
		var set1 = SetBuilder.create(String.class)
			.add("c", "a", "b")
			.sorted()
			.sparse()
			.build();

		assertNotNull(set1);
		assertList(set1, "a", "b", "c");

		var set2 = SetBuilder.create(String.class)
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
		var set = SetBuilder.create(String.class)
			.build();

		assertNotNull(set);
		assertEmpty(set);
	}

	@Test
	void j02_addNullElement() {
		var set = SetBuilder.create(String.class)
			.add("a")
			.add((String)null)
			.add("b")
			.build();

		assertList(set, "a", "<null>", "b");
	}

	@Test
	void j03_toExistingSet() {
		var existing = new LinkedHashSet<String>();
		existing.add("x");

		var set = SetBuilder.create(String.class)
			.to(existing)
			.add("y")
			.build();

		assertList(set, "x", "y");
		assertSame(existing, set);
	}
}