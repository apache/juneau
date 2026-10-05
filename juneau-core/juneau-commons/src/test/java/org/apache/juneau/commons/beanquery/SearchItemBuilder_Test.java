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

class SearchItemBuilder_Test extends TestBase {

	@Test
	void a01_addLeaf_simple() {
		var b = new SearchItemBuilder();
		b.addLeaf("name", "$eq(\"bob\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$eq(\"bob\")")), b.items());
	}

	@Test
	void a02_addLeaf_sameColumnTwice_wrapsInAnd() {
		var b = new SearchItemBuilder();
		b.addLeaf("age", "$gt(\"5\")");
		b.addLeaf("age", "$lt(\"10\")");
		assertEquals(List.of(new SearchItem.Leaf("age", "$and($gt(\"5\"),$lt(\"10\"))")), b.items());
	}

	@Test
	void a03_addLeaf_sameColumnThreeTimes_appendsIntoSameAnd() {
		var b = new SearchItemBuilder();
		b.addLeaf("age", "$gt(\"5\")");
		b.addLeaf("age", "$lt(\"10\")");
		b.addLeaf("age", "$ne(\"7\")");
		assertEquals(List.of(new SearchItem.Leaf("age", "$and($gt(\"5\"),$lt(\"10\"),$ne(\"7\"))")), b.items());
	}

	@Test
	void a04_addLeaf_differentColumns_bothKept_inOrder() {
		var b = new SearchItemBuilder();
		b.addLeaf("name", "$eq(\"bob\")");
		b.addLeaf("age", "$gt(\"5\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$eq(\"bob\")"), new SearchItem.Leaf("age", "$gt(\"5\")")), b.items());
	}

	@Test
	void a05_addGroup_appendsDistinctSiblings_neverMerges() {
		var b = new SearchItemBuilder();
		var or1 = new SearchItem.Or(List.of(new SearchItem.Leaf("a", "$eq(\"1\")"), new SearchItem.Leaf("b", "$eq(\"2\")")));
		b.addGroup(or1);
		b.addGroup(or1);
		assertEquals(List.of(or1, or1), b.items());
	}

	@Test
	void a06_addGroup_null_addsNothing() {
		var b = new SearchItemBuilder();
		b.addGroup(null);
		assertEquals(List.of(), b.items());
	}

	@Test
	void a07_seed_roundTrips() {
		var parsed = SearchParser.parse("name=$eq(\"bob\"),$or(a=$eq(\"1\"),b=$eq(\"2\"))");
		var b = new SearchItemBuilder();
		b.seed(parsed);
		assertEquals(parsed, b.items());
	}

	@Test
	void a08_seed_thenAddLeaf_mergesWithSeededColumn() {
		var b = new SearchItemBuilder();
		b.seed(SearchParser.parse("age=$gt(\"5\")"));
		b.addLeaf("age", "$lt(\"10\")");
		assertEquals(List.of(new SearchItem.Leaf("age", "$and($gt(\"5\"),$lt(\"10\"))")), b.items());
	}

	@Test
	void a09_seed_thenAddLeafTwice_appendsIntoSameAnd() {
		var b = new SearchItemBuilder();
		b.seed(SearchParser.parse("age=$gt(\"5\")"));
		b.addLeaf("age", "$lt(\"10\")");
		b.addLeaf("age", "$ne(\"7\")");
		assertEquals(List.of(new SearchItem.Leaf("age", "$and($gt(\"5\"),$lt(\"10\"),$ne(\"7\"))")), b.items());
	}
}
