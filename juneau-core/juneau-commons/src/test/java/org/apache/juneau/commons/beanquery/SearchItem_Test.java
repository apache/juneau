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

class SearchItem_Test extends TestBase {

	@Test
	void a01_leaf_equality() {
		var a = new SearchItem.Leaf("name", "$eq(\"x\")");
		var b = new SearchItem.Leaf("name", "$eq(\"x\")");
		assertEquals(a, b);
		assertBean(a, "column,expression", "name,$eq(\"x\")");
	}

	@Test
	void a02_and_or_defensiveCopy() {
		var items = new ArrayList<SearchItem>();
		items.add(new SearchItem.Leaf("a", "$eq(\"1\")"));
		items.add(new SearchItem.Leaf("b", "$eq(\"2\")"));
		var and = new SearchItem.And(items);
		items.add(new SearchItem.Leaf("c", "$eq(\"3\")"));
		assertEquals(2, and.items().size());
		var hoistedTarget1 = and.items();
		var hoistedArg1 = new SearchItem.Leaf("d", "$eq(\"4\")");
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget1.add(hoistedArg1));
	}

	@Test
	void a03_not_wrapsOneItem() {
		var not = new SearchItem.Not(new SearchItem.Leaf("a", "$blank()"));
		assertEquals(new SearchItem.Leaf("a", "$blank()"), not.item());
	}

	@Test
	void a04_or_equality() {
		var or1 = new SearchItem.Or(List.of(new SearchItem.Leaf("a", "$eq(\"1\")"), new SearchItem.Leaf("b", "$eq(\"2\")")));
		var or2 = new SearchItem.Or(List.of(new SearchItem.Leaf("a", "$eq(\"1\")"), new SearchItem.Leaf("b", "$eq(\"2\")")));
		assertEquals(or1, or2);
	}
}
