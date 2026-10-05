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

class BeanQuery_Builder_Test extends TestBase {

	@Test
	void a01_eq() {
		assertEquals("a=$eq(\"1\")", BeanQuery.create().eq("a", 1).build().getSearch());
	}

	@Test
	void a02_ne() {
		assertEquals("a=$ne(\"1\",\"2\")", BeanQuery.create().ne("a", 1, 2).build().getSearch());
	}

	@Test
	void a03_in() {
		assertEquals("a=$in(\"1\",\"2\",\"3\")", BeanQuery.create().in("a", 1, 2, 3).build().getSearch());
	}

	@Test
	void a04_contains() {
		assertEquals("a=$contains(\"x\")", BeanQuery.create().contains("a", "x").build().getSearch());
	}

	@Test
	void a05_prefix() {
		assertEquals("a=$prefix(\"x\")", BeanQuery.create().prefix("a", "x").build().getSearch());
	}

	@Test
	void a06_blank() {
		assertEquals("a=$blank()", BeanQuery.create().blank("a").build().getSearch());
	}

	@Test
	void a07_notBlank() {
		assertEquals("a=$not($blank())", BeanQuery.create().notBlank("a").build().getSearch());
	}

	@Test
	void a08_gt_gte_lt_lte() {
		assertEquals("a=$gt(\"1\")", BeanQuery.create().gt("a", 1).build().getSearch());
		assertEquals("a=$gte(\"1\")", BeanQuery.create().gte("a", 1).build().getSearch());
		assertEquals("a=$lt(\"1\")", BeanQuery.create().lt("a", 1).build().getSearch());
		assertEquals("a=$lte(\"1\")", BeanQuery.create().lte("a", 1).build().getSearch());
	}

	@Test
	void a09_between() {
		assertEquals("a=$between(\"1\",\"9\")", BeanQuery.create().between("a", 1, 9).build().getSearch());
	}

	@Test
	void a10_regex_twoArg() {
		assertEquals("a=$regex(\"^x\")", BeanQuery.create().regex("a", "^x").build().getSearch());
	}

	@Test
	void a11_regex_threeArg() {
		assertEquals("a=$regex(\"^x\",\"i\")", BeanQuery.create().regex("a", "^x", "i").build().getSearch());
	}

	@Test
	void a12_op_escapeHatch() {
		assertEquals("a=$startsWith(\"x\")", BeanQuery.create().op("a", "$startsWith", "x").build().getSearch());
	}

	@Test
	void a13_op_rejectsNameWithoutDollar() {
		var hoistedTarget1 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget1.op("a", "startsWith", "x"));
	}

	@Test
	void a14_search_escapeHatch() {
		assertEquals("a=$eq(\"x\")", BeanQuery.create().search("a", "$eq(\"x\")").build().getSearch());
	}

	@Test
	void b01_sameColumnTwice_wrapsInAnd() {
		var q = BeanQuery.create().gt("age", 5).lt("age", 10).build();
		assertEquals("age=$and($gt(\"5\"),$lt(\"10\"))", q.getSearch());
	}

	@Test
	void b02_sameColumnThreeTimes_appendsIntoSameAnd() {
		var q = BeanQuery.create().gt("age", 5).lt("age", 10).ne("age", 7).build();
		assertEquals("age=$and($gt(\"5\"),$lt(\"10\"),$ne(\"7\"))", q.getSearch());
	}

	@Test
	void c01_quote_doublesEmbeddedQuote() {
		var q = BeanQuery.create().eq("name", "a\"b").build();
		assertEquals("name=$eq(\"a\"\"b\")", q.getSearch());
	}

	@Test
	void c02_nullColumn_throws() {
		var hoistedTarget2 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget2.eq(null, "x"));
	}

	@Test
	void c03_blankColumn_throws() {
		var hoistedTarget3 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget3.eq(" ", "x"));
	}

	@Test
	void d01_view_nullElement_throws() {
		var hoistedTarget4 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget4.view("a", null, "b"));
	}

	@Test
	void d02_opt_nullValue_throws() {
		var hoistedTarget5 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget5.opt("x", null));
	}

	@Test
	void e01_relativeDuration_roundTripsThroughGte() {
		var q = BeanQuery.create().gte("lastSeen", RelativeDuration.of(-24, RelativeDuration.Unit.H)).build();
		assertEquals("lastSeen=$gte(\"-24h\")", q.getSearch());
	}

	@Test
	void f01_copy_trimsAndDropsBlankViewAndSortTokens() {
		var q = new BeanQuery().setView("a, b ,c,, ").setSort(" x , y ").copy().build();
		assertList(List.of(q.getView().split(",")), "a", "b", "c");
		assertList(List.of(q.getSort().split(",")), "x", "y");
	}
}
