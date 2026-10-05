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

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class BeanQuery_GroupBuilder_Test extends TestBase {

	@Test
	void a01_or_twoItems() {
		var q = BeanQuery.create().or(g -> g.eq("a", 1).eq("b", 2)).build();
		assertEquals("$or(a=$eq(\"1\"),b=$eq(\"2\"))", q.getSearch());
	}

	@Test
	void a02_and_twoItems() {
		var q = BeanQuery.create().and(g -> g.eq("a", 1).eq("b", 2)).build();
		assertEquals("$and(a=$eq(\"1\"),b=$eq(\"2\"))", q.getSearch());
	}

	@Test
	void a03_or_singleItem_unwraps() {
		var q = BeanQuery.create().or(g -> g.eq("a", 1)).build();
		assertEquals("a=$eq(\"1\")", q.getSearch());
	}

	@Test
	void a04_or_empty_addsNothing() {
		var q = BeanQuery.create().eq("z", 9).or(g -> {}).build();
		assertEquals("z=$eq(\"9\")", q.getSearch());
	}

	@Test
	void a05_not_singleItem() {
		var q = BeanQuery.create().not(g -> g.eq("a", 1)).build();
		assertEquals("$not(a=$eq(\"1\"))", q.getSearch());
	}

	@Test
	void a06_not_empty_throws() {
		var hoistedTarget1 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget1.not(g -> {}));
	}

	@Test
	void a07_not_twoItems_throws() {
		var hoistedTarget2 = BeanQuery.create();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget2.not(g -> g.eq("a", 1).eq("b", 2)));
	}

	@Test
	void a08_not_wrapsNestedGroup() {
		var q = BeanQuery.create().not(g -> g.or(inner -> inner.eq("a", 1).eq("b", 2))).build();
		assertEquals("$not($or(a=$eq(\"1\"),b=$eq(\"2\")))", q.getSearch());
	}

	@Test
	void a09_groupBuilder_repeatedColumn_mergesWithinGroup() {
		var q = BeanQuery.create().or(g -> g.gt("a", 1).lt("a", 9).eq("b", 2)).build();
		assertEquals("$or(a=$and($gt(\"1\"),$lt(\"9\")),b=$eq(\"2\"))", q.getSearch());
	}

	@Test
	void a10_multipleGroups_appendAsDistinctSiblings() {
		var q = BeanQuery.create().or(g -> g.eq("a", 1).eq("b", 2)).and(g -> g.eq("c", 3).eq("d", 4)).build();
		assertEquals("$or(a=$eq(\"1\"),b=$eq(\"2\")),$and(c=$eq(\"3\"),d=$eq(\"4\"))", q.getSearch());
	}
}
