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

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class FilterEvaluator_Test extends TestBase {

	/** A generous budget shared by every test &mdash; these tests are not exercising the regex deadline itself. */
	private static final RegexBudget BUDGET = new RegexBudget(Duration.ofSeconds(5));

	private static Filter.Leaf leaf(String column, String expression) {
		return new Filter.Leaf(column, SearchExpressionParser.parse(expression, SearchOperatorSet.standard()), SearchType.TEXT);
	}

	private static Map<String,Object> row(Object... kvs) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kvs.length; i += 2)
			m.put((String) kvs[i], kvs[i + 1]);
		return m;
	}

	@Test
	void a01_nullFilter_matchesEverything() {
		assertTrue(FilterEvaluator.matches(null, row()::get, BUDGET));
	}

	@Test
	void a02_leaf_matches() {
		assertTrue(FilterEvaluator.matches(leaf("name", "$eq(\"bob\")"), row("name", "bob")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(leaf("name", "$eq(\"bob\")"), row("name", "alice")::get, BUDGET));
	}

	@Test
	void a03_and_shortCircuits_requiresBoth() {
		var filter = new Filter.And(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")")));
		assertTrue(FilterEvaluator.matches(filter, row("a", "1", "b", "2")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(filter, row("a", "1", "b", "x")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(filter, row("a", "x", "b", "2")::get, BUDGET));
	}

	@Test
	void a04_or_matchesEither() {
		var filter = new Filter.Or(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")")));
		assertTrue(FilterEvaluator.matches(filter, row("a", "1", "b", "x")::get, BUDGET));
		assertTrue(FilterEvaluator.matches(filter, row("a", "x", "b", "2")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(filter, row("a", "x", "b", "y")::get, BUDGET));
	}

	@Test
	void a05_not_negates() {
		var filter = new Filter.Not(leaf("a", "$eq(\"1\")"));
		assertFalse(FilterEvaluator.matches(filter, row("a", "1")::get, BUDGET));
		assertTrue(FilterEvaluator.matches(filter, row("a", "2")::get, BUDGET));
	}

	@Test
	void a06_nestedGroups() {
		var filter = new Filter.And(List.of(
			leaf("status", "$eq(\"open\")"),
			new Filter.Or(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")")))));
		assertTrue(FilterEvaluator.matches(filter, row("status", "open", "a", "1", "b", "x")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(filter, row("status", "closed", "a", "1", "b", "x")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(filter, row("status", "open", "a", "x", "b", "y")::get, BUDGET));
	}

	@Test
	void a07_not_wrappingGroup_negatesWholeGroup() {
		var filter = new Filter.Not(new Filter.Or(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")"))));
		assertFalse(FilterEvaluator.matches(filter, row("a", "1", "b", "x")::get, BUDGET));  // Or matches -> Not flips to false.
		assertTrue(FilterEvaluator.matches(filter, row("a", "x", "b", "y")::get, BUDGET));  // Or doesn't match -> Not flips to true.
	}

	@Test
	void a08_andOr_threeOrMoreItems() {
		var and = new Filter.And(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")"), leaf("c", "$eq(\"3\")")));
		assertTrue(FilterEvaluator.matches(and, row("a", "1", "b", "2", "c", "3")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(and, row("a", "1", "b", "2", "c", "x")::get, BUDGET));

		var or = new Filter.Or(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")"), leaf("c", "$eq(\"3\")")));
		assertTrue(FilterEvaluator.matches(or, row("a", "x", "b", "y", "c", "3")::get, BUDGET));
		assertFalse(FilterEvaluator.matches(or, row("a", "x", "b", "y", "c", "z")::get, BUDGET));
	}

	@Test
	void a09_or_shortCircuits_neverQueriesLaterBranchOnceEarlierMatches() {
		var filter = new Filter.Or(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")")));
		var r = row("a", "1", "b", "2");
		var queried = new ArrayList<String>();
		Function<String,Object> accessor = col -> { queried.add(col); return r.get(col); };
		assertTrue(FilterEvaluator.matches(filter, accessor, BUDGET));
		assertEquals(List.of("a"), queried);  // "b" never looked up: the first branch already matched.
	}

	@Test
	void a10_and_shortCircuits_neverQueriesLaterBranchOnceEarlierFails() {
		var filter = new Filter.And(List.of(leaf("a", "$eq(\"1\")"), leaf("b", "$eq(\"2\")")));
		var r = row("a", "x", "b", "2");
		var queried = new ArrayList<String>();
		Function<String,Object> accessor = col -> { queried.add(col); return r.get(col); };
		assertFalse(FilterEvaluator.matches(filter, accessor, BUDGET));
		assertEquals(List.of("a"), queried);  // "b" never looked up: the first branch already failed.
	}
}
