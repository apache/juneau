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

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class Filter_Test extends TestBase {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static SearchExpression expr(String raw) {
		return SearchExpressionParser.parse(raw, OPS);
	}

	@Test
	void a01_leaf_fields() {
		var leaf = new Filter.Leaf("name", expr("$eq(\"bob\")"), SearchType.TEXT);
		assertBean(leaf, "column,type", "name,TEXT");
		assertTrue(leaf.expression().isFunction());
	}

	@Test
	void a02_and_defensiveCopy() {
		var items = new ArrayList<Filter>();
		items.add(new Filter.Leaf("a", expr("$eq(\"1\")"), SearchType.TEXT));
		items.add(new Filter.Leaf("b", expr("$eq(\"2\")"), SearchType.TEXT));
		var and = new Filter.And(items);
		items.add(new Filter.Leaf("c", expr("$eq(\"3\")"), SearchType.TEXT));
		assertEquals(2, and.items().size());
		var hoistedTarget1 = and.items();
		var hoistedArg1 = new Filter.Leaf("d", expr("$eq(\"4\")"), SearchType.TEXT);
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget1.add(hoistedArg1));
	}

	@Test
	void a03_or_and_not_equality() {
		var leaf = new Filter.Leaf("a", expr("$blank()"), SearchType.TEXT);
		assertEquals(new Filter.Not(leaf), new Filter.Not(leaf));
		var or1 = new Filter.Or(List.of(leaf, leaf));
		var or2 = new Filter.Or(List.of(leaf, leaf));
		assertEquals(or1, or2);
	}

	@Test
	void a04_emptyAnd_matchesEveryRow_byConvention() {
		// An empty top-level And is what an empty search resolves to (design §7); Filter itself carries no such
		// short-circuit — that behavior lives in the evaluator (InMemoryBeanQuerySession / SqlSearchCompiler).
		var and = new Filter.And(List.of());
		assertTrue(and.items().isEmpty());
	}
}
