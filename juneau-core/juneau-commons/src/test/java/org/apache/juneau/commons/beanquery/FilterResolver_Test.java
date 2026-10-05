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
package org.apache.juneau.commons.beanquery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class FilterResolver_Test extends TestBase {

	/** A resolver that wraps the raw expression string as a fake leaf (no real {@link SearchExpressionParser} call), so these tests are independent of the operator grammar. */
	private static Filter.Leaf fakeLeaf(String column, String expression) {
		return new Filter.Leaf(column, SearchExpressionParser.parse(expression, SearchOperatorSet.standard()), SearchType.TEXT);
	}

	/** A resolver like {@link #fakeLeaf} that returns <jk>null</jk> (the drop signal) for the sentinel expression
	 * {@code "DROP"}, to test null-as-drop-signal propagation through {@code $and}/{@code $or}/{@code $not} groups. */
	private static Filter.Leaf dropLeaf(String column, String expression) {
		return "DROP".equals(expression) ? null : fakeLeaf(column, expression);
	}

	@Test
	void a01_empty_resolvesToEmptyAnd() {
		assertEquals(new Filter.And(List.of()), FilterResolver.resolve(List.of(), FilterResolver_Test::fakeLeaf));
	}

	@Test
	void a02_singleLeaf_resolvesUnwrapped() {
		var items = SearchParser.parse("a=$eq(\"1\")");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.Leaf.class, filter);
		assertEquals("a", ((Filter.Leaf) filter).column());
	}

	@Test
	void a03_multipleTopLevelLeaves_foldIntoAnd() {
		var items = SearchParser.parse("a=$eq(\"1\"),b=$eq(\"2\")");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.And.class, filter);
		assertEquals(2, ((Filter.And) filter).items().size());
	}

	@Test
	void a04_nestedGroup_preservesShape() {
		var items = SearchParser.parse("$or(a=$eq(\"1\"),b=$eq(\"2\"))");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.Or.class, filter);
	}

	@Test
	void a05_not_preservesShape() {
		var items = SearchParser.parse("$not(a=$eq(\"1\"))");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.Not.class, filter);
		assertInstanceOf(Filter.Leaf.class, ((Filter.Not) filter).item());
	}

	@Test
	void a06_andGroup_preservesShape() {
		var items = SearchParser.parse("$and(a=$eq(\"1\"),b=$eq(\"2\"))");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.And.class, filter);
		assertEquals(2, ((Filter.And) filter).items().size());
	}

	@Test
	void a07_not_wrappingGroup_preservesShape() {
		var items = SearchParser.parse("$not($or(a=$eq(\"1\"),b=$eq(\"2\")))");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.Not.class, filter);
		assertInstanceOf(Filter.Or.class, ((Filter.Not) filter).item());
	}

	@Test
	void a08_mixedNesting_preservesShape() {
		var items = SearchParser.parse("$or($and(a=$eq(\"1\"),b=$eq(\"2\")),c=$eq(\"3\"))");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::fakeLeaf);
		assertInstanceOf(Filter.Or.class, filter);
		var orItems = ((Filter.Or) filter).items();
		assertEquals(2, orItems.size());
		assertInstanceOf(Filter.And.class, orItems.get(0));
		assertInstanceOf(Filter.Leaf.class, orItems.get(1));
	}

	@Test
	void b01_countLeaves_flat() {
		assertEquals(2, FilterResolver.countLeaves(SearchParser.parse("a=$eq(\"1\"),b=$eq(\"2\")")));
	}

	@Test
	void b02_countLeaves_countsInsideGroups() {
		assertEquals(3, FilterResolver.countLeaves(SearchParser.parse("a=$eq(\"1\"),$or(b=$eq(\"2\"),c=$eq(\"3\"))")));
	}

	@Test
	void b03_countLeaves_empty() {
		assertEquals(0, FilterResolver.countLeaves(List.of()));
	}

	@Test
	void c01_orGroup_allChildrenDropped_groupItselfDropped() {
		// Both children of the $or resolve to null (the drop signal), so the group itself resolves to null and the
		// whole thing collapses to the empty-And, matches-everything result -- same as if the search were absent.
		var items = SearchParser.parse("$or(a=DROP,b=DROP)");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::dropLeaf);
		assertEquals(new Filter.And(List.of()), filter);
	}

	@Test
	void c02_notWrappingDroppedGroup_propagatesDrop() {
		// The $or inside the $not collapses to null (both its children are dropped), so Filter.Not of a dropped
		// item is itself dropped, and only the sibling leaf survives, unwrapped (not folded into a now-pointless And).
		var items = SearchParser.parse("$not($or(a=DROP,b=DROP)),c=$eq(\"1\")");
		var filter = FilterResolver.resolve(items, FilterResolver_Test::dropLeaf);
		assertInstanceOf(Filter.Leaf.class, filter);
		assertEquals("c", ((Filter.Leaf) filter).column());
	}
}
