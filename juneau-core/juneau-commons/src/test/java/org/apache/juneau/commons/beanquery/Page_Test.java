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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class Page_Test extends TestBase {

	@Test
	void a01_noCounts() {
		var p = Page.of(List.of("a", "b"));
		assertList(p.rows(), "a", "b");
		assertBean(p, "total,matched", "OptionalLong.empty,OptionalLong.empty");
	}

	@Test
	void a02_bothCounts() {
		var p = Page.of(List.of("a"), 4, 2);
		assertBean(p, "total,matched", "OptionalLong[4],OptionalLong[2]");
	}

	@Test
	void a03_matchedOnly() {
		var p = Page.ofMatched(List.of("a"), 2);
		assertBean(p, "total,matched", "OptionalLong.empty,OptionalLong[2]");
	}

	@Test
	void a04_rowsAreCopied() {
		var l = new ArrayList<>(List.of("a"));
		var p = Page.of(l);
		l.add("b");
		assertList(p.rows(), "a");
		var hoistedTarget1 = p.rows();
		assertThrows(UnsupportedOperationException.class, () -> hoistedTarget1.add("c"));
	}

	@Test
	void a05_executionExceptionKeepsCause() {
		var cause = new IllegalStateException("boom");
		var e = new BeanQueryExecutionException(cause, "Query execution failed for table '%s'.", "person");
		assertEquals("Query execution failed for table 'person'.", e.getMessage());
		assertSame(cause, e.getCause());
	}

	@Test
	void a06_countPolicyValues() {
		assertList(Arrays.asList(CountPolicy.values()), "NONE", "MATCHED", "BOTH", "IF_REQUESTED");
	}

	@Test
	void a07_nullRowFailsClearly() {
		var hoistedArg1 = Arrays.asList("a", null);
		assertThrowsWithMessage(BeanQueryExecutionException.class, "Row 1 is null.", () -> Page.of(hoistedArg1));
	}

	@Test
	void a08_equalsHashCode() {
		assertEquals(Page.of(List.of("a"), 100L, 25L), Page.of(List.of("a"), 100L, 25L));
		assertEquals(Page.of(List.of("a"), 100L, 25L).hashCode(), Page.of(List.of("a"), 100L, 25L).hashCode());
		assertNotEquals(Page.of(List.of("a")), Page.of(List.of("a"), 100L, 25L));
	}

	@Test
	void a09_toString_omitsAbsentCounts_neverDumpsRows() {
		assertEquals("Page[rows=1]", Page.of(List.of("secret-row")).toString());
		assertEquals("Page[rows=25, total=1203, matched=87]",
			Page.of(java.util.stream.IntStream.range(0, 25).boxed().toList(), 1203L, 87L).toString());
	}

	@Test
	void a10_toString_matchedOnly_omitsTotal() {
		assertEquals("Page[rows=1, matched=2]", Page.ofMatched(List.of("a"), 2).toString());
	}
}
