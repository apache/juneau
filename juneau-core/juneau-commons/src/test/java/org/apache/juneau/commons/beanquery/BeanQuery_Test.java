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

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class BeanQuery_Test extends TestBase {

	@Test
	void a01_create_returnsBuilder() {
		assertInstanceOf(BeanQuery.Builder.class, BeanQuery.create());
	}

	@Test
	void a02_build_withNoConditions_searchIsNull() {
		var q = BeanQuery.create().build();
		assertBean(q, "search,view,sort,position,limit,opts", "<null>,<null>,<null>,<null>,<null>,<null>");
	}

	@Test
	void a03_build_singleCondition() {
		var q = BeanQuery.create().eq("name", "bob").build();
		assertEquals("name=$eq(\"bob\")", q.getSearch());
	}

	@Test
	void a04_build_viewAppends() {
		var q = BeanQuery.create().view("a", "b").view("c").build();
		assertEquals("a,b,c", q.getView());
	}

	@Test
	void a05_build_sortAppends_ascAndDesc() {
		var q = BeanQuery.create().sort("name").sortDesc("age").build();
		assertEquals("name,age:desc", q.getSort());
	}

	@Test
	void a06_build_position_and_limit() {
		var q = BeanQuery.create().position(5).limit(20).build();
		assertBean(q, "position,limit", "5,20");
	}

	@Test
	void a07_build_negativeLimit_clampsToMinusOne() {
		var q1 = BeanQuery.create().limit(-7).build();
		assertEquals(-1, q1.getLimit());
		var q2 = BeanQuery.create().page(0, -1).build();
		assertEquals(-1, q2.getLimit());
	}

	@Test
	void a08_build_position_negative_throws() {
		var hoistedTarget1 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget1.position(-1));
	}

	@Test
	void a09_build_opt_and_countsMatched() {
		var q = BeanQuery.create().opt("x", "y").counts(CountRequest.MATCHED).build();
		assertEquals("x=y,counts=matched", q.getOpts());
	}

	@Test
	void a10_build_countsBoth() {
		var q = BeanQuery.create().counts(CountRequest.BOTH).build();
		assertEquals("counts=both", q.getOpts());
	}

	@Test
	void a11_build_countsNone_removesOpt() {
		var q = BeanQuery.create().counts(CountRequest.BOTH).counts(CountRequest.NONE).build();
		assertNull(q.getOpts());
	}

	@Test
	void a12_build_returnsNewInstanceEachTime() {
		var b = BeanQuery.create().eq("a", "1");
		var q1 = b.build();
		var q2 = b.eq("b", "2").build();
		assertNotSame(q1, q2);
		assertEquals("a=$eq(\"1\")", q1.getSearch());
		assertEquals("a=$eq(\"1\"),b=$eq(\"2\")", q2.getSearch());
	}

	@Test
	void b01_copy_seedsAllFields() {
		var q1 = BeanQuery.create().eq("age", 5).view("name").sort("name").position(0).limit(10).opt("x", "y").build();
		var q2 = q1.copy().build();
		assertEquals(q1, q2);
	}

	@Test
	void b02_copy_thenAddCondition_mergesWithSeededColumn() {
		var q1 = BeanQuery.create().eq("age", 5).build();
		var q2 = q1.copy().eq("age", 1).build();
		assertEquals("age=$and($eq(\"5\"),$eq(\"1\"))", q2.getSearch());
	}

	@Test
	void b03_copy_preservesUnrelatedGroup() {
		var q1 = BeanQuery.create().eq("age", 5).or(g -> g.eq("a", 1).eq("b", 2)).build();
		var q2 = q1.copy().build();
		assertEquals(q1.getSearch(), q2.getSearch());
	}

	@Test
	void c01_equals_and_hashCode() {
		var q1 = BeanQuery.create().eq("a", 1).view("v").build();
		var q2 = BeanQuery.create().eq("a", 1).view("v").build();
		assertEquals(q1, q2);
		assertEquals(q1.hashCode(), q2.hashCode());
	}

	@Test
	void c02_toString() {
		var q = BeanQuery.create().eq("a", 1).build();
		assertEquals("BeanQuery[search=a=$eq(\"1\"), view=null, sort=null, position=null, limit=null, opts=null]", q.toString());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// quote() escapes a literal '$' (round-trips a value containing "$$" through the parser's decode).
	// -----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(strings = {
		"a$$b",
		"a$$\"b",
		"$$",
		"a$$$b",
		"a$eq"  // A single '$' followed by letters that spell an operator name must still be a literal '$' once escaped: quote() doubles it regardless of what follows.
	})
	void d01_build_valueWithDollars_roundTripsThroughParser(String value) {
		var q = BeanQuery.create().eq("v", value).build();
		var expr = SearchExpressionParser.parse(exprOf(q), SearchOperatorSet.standard());
		assertEquals(value, expr.literalArgs().get(0));
	}

	/** Strips the leading {@code "v="} column prefix from a single-column query's search string. */
	private static String exprOf(BeanQuery q) {
		var search = q.getSearch();
		return search.substring(search.indexOf('=') + 1);
	}
}
