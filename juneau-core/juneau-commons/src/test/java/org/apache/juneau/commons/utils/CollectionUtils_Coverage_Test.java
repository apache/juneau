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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Additional coverage tests for {@link CollectionUtils} targeting branches/lines not exercised by
 * {@link CollectionUtils_Test}, e.g. {@code null}-argument edge cases, {@code removeNegations} token
 * classification, and defensive null-skipping loops.
 */
class CollectionUtils_Coverage_Test extends TestBase {

	//====================================================================================================
	// accumulate(Object...) - null varargs array (as opposed to a single null element)
	//====================================================================================================
	@Test
	void a01_accumulate_nullVarargsArray_returnsEmptyList() {
		// Passing (Object[])null makes the varargs array itself null (distinct from accumulate((Object)null),
		// which wraps the null in a 1-element array) - exercises the "o != null" false branch.
		List<Object> result = accumulate((Object[])null);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	//====================================================================================================
	// next(Iterator<? extends E>)
	//====================================================================================================
	@Test
	void a02_next_nullIterator_returnsEmptyOptional() {
		assertTrue(next(null).isEmpty());
	}

	@Test
	void a03_next_exhaustedIterator_returnsEmptyOptional() {
		Iterator<String> it = list("a").iterator();
		it.next();
		assertTrue(next(it).isEmpty());
	}

	@Test
	void a04_next_nonEmptyIterator_returnsNextElement() {
		Iterator<String> it = list("a", "b").iterator();
		assertEquals("a", next(it).get());
		assertEquals("b", next(it).get());
		assertTrue(next(it).isEmpty());
	}

	//====================================================================================================
	// removeNegations(List<String>)
	//====================================================================================================
	@Test
	void b01_removeNegations_noNegationTokens_returnsSameListInstance() {
		var input = list("a", "b", "c");
		var result = removeNegations(input);
		assertSame(input, result);
	}

	@Test
	void b02_removeNegations_negationRemovesFirstPriorOccurrence() {
		// Javadoc example: "-a" removes the first prior "a", leaving ["b", "c"].
		var result = removeNegations(list("a", "b", "-a", "c"));
		assertEquals(list("b", "c"), result);
	}

	@Test
	void b03_removeNegations_negationOfAbsentToken_isNoOp() {
		var result = removeNegations(list("a", "-z"));
		assertEquals(list("a"), result);
	}

	@Test
	void b04_removeNegations_nullAndBorderlineTokens_areTreatedAsLiterals() {
		// Exercises every branch of the not-null / length-over-one / leading-dash negation-token check
		// in both loops, using these deliberately borderline inputs:
		//   null   -> short-circuits on the not-null check
		//   "-"    -> length one, fails the length-over-one check
		//   "ab"   -> length over one but doesn't start with a dash
		//   "-a"   -> the one genuine negation token, present to force the second loop to run
		var input = new ArrayList<String>();
		input.add(null);
		input.add("-");
		input.add("ab");
		input.add("a");
		input.add("b");
		input.add("-a");
		input.add("c");

		var result = removeNegations(input);

		// null, "-", and "ab" are passed through as literals; "-a" removes the earlier "a".
		var expected = new ArrayList<String>();
		expected.add(null);
		expected.add("-");
		expected.add("ab");
		expected.add("b");
		expected.add("c");
		assertEquals(expected, result);
	}

	@Test
	void b05_removeNegations_emptyList_returnsSameListInstance() {
		List<String> input = list();
		var result = removeNegations(input);
		assertSame(input, result);
	}

	@Test
	void b06_removeNegations_nullInput_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'input' cannot be null.", () -> removeNegations(null));
	}

	//====================================================================================================
	// treeSet(Comparator<? super E>, Collection<? extends E>)
	//====================================================================================================
	@Test
	void c01_treeSet_nullElements_returnsEmptyTreeSet() {
		SortedSet<String> result = treeSet(Comparator.naturalOrder(), null);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void c02_treeSet_elementsContainingNulls_skipsNulls() {
		var elements = new ArrayList<String>();
		elements.add("b");
		elements.add(null);
		elements.add("a");
		SortedSet<String> result = treeSet(Comparator.naturalOrder(), elements);
		assertEquals(list("a", "b"), new ArrayList<>(result));
	}

	@Test
	void c03_treeSet_nullComparator_throwsIllegalArgumentException() {
		assertThrowsWithMessage(IllegalArgumentException.class, "Argument 'comparator' cannot be null.", () -> treeSet(null, list("a")));
	}

	//====================================================================================================
	// sortedSet(E...) - null-skipping within the varargs array
	//====================================================================================================
	@Test
	void d01_sortedSet_varargsContainingNull_skipsNull() {
		SortedSet<String> result = sortedSet("b", null, "a");
		assertEquals(list("a", "b"), new ArrayList<>(result));
	}

	//====================================================================================================
	// toSortedSet(Collection<E>, boolean) - nullIfEmpty=true with a non-null, non-empty collection
	//====================================================================================================
	@Test
	void e01_toSortedSet_nullIfEmptyTrue_nonEmptyCollection_returnsPopulatedSet() {
		SortedSet<String> result = toSortedSet(list("c", "a", "b"), true);
		assertNotNull(result);
		assertEquals(list("a", "b", "c"), new ArrayList<>(result));
	}

	//====================================================================================================
	// unmodifiable(SortedSet<T>)
	//====================================================================================================
	@Test
	void f01_unmodifiable_sortedSet_returnsUnmodifiableView() {
		SortedSet<String> set = sortedSet("a", "b");
		SortedSet<String> result = unmodifiable(set);
		assertNotNull(result);
		assertEquals(list("a", "b"), new ArrayList<>(result));
		assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
	}

	@Test
	void f02_unmodifiable_sortedSet_null_returnsNull() {
		assertNull(unmodifiable((SortedSet<String>)null));
	}

	//====================================================================================================
	// elementAt(List<E>, int)
	//====================================================================================================
	@Test
	void g01_elementAt_nullList_returnsNull() {
		assertNull(elementAt(null, 0));
	}

	@Test
	void g02_elementAt_negativeIndex_returnsNull() {
		assertNull(elementAt(list("a", "b"), -1));
	}

	@Test
	void g03_elementAt_indexAtOrBeyondSize_returnsNull() {
		assertNull(elementAt(list("a", "b"), 2));
	}

	@Test
	void g04_elementAt_validIndex_returnsElement() {
		assertEquals("b", elementAt(list("a", "b", "c"), 1));
	}
}
