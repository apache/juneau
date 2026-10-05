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

class SearchParser_Test extends TestBase {

	@Test
	void a01_emptyOrBlank() {
		assertEquals(List.of(), SearchParser.parse(null));
		assertEquals(List.of(), SearchParser.parse(""));
		assertEquals(List.of(), SearchParser.parse("   "));
	}

	@Test
	void a02_singleLeaf() {
		var items = SearchParser.parse("name=$eq(\"bob\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$eq(\"bob\")")), items);
	}

	@Test
	void a03_multipleTopLevelLeaves() {
		var items = SearchParser.parse("name=$eq(\"bob\"),age=$gt(\"5\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$eq(\"bob\")"), new SearchItem.Leaf("age", "$gt(\"5\")")), items);
	}

	@Test
	void a04_orGroup() {
		var items = SearchParser.parse("$or(name=$contains(\"x\"),email=$contains(\"x\"))");
		assertEquals(List.of(new SearchItem.Or(List.of(
			new SearchItem.Leaf("name", "$contains(\"x\")"),
			new SearchItem.Leaf("email", "$contains(\"x\")")))), items);
	}

	@Test
	void a05_notGroup() {
		var items = SearchParser.parse("$not(name=$blank())");
		assertEquals(List.of(new SearchItem.Not(new SearchItem.Leaf("name", "$blank()"))), items);
	}

	@Test
	void a06_nestedGroups() {
		var items = SearchParser.parse("$and(a=$eq(\"1\"),$or(b=$eq(\"2\"),c=$eq(\"3\")))");
		assertEquals(List.of(new SearchItem.And(List.of(
			new SearchItem.Leaf("a", "$eq(\"1\")"),
			new SearchItem.Or(List.of(new SearchItem.Leaf("b", "$eq(\"2\")"), new SearchItem.Leaf("c", "$eq(\"3\")")))))), items);
	}

	@Test
	void a07_orArityError() {
		var e = assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("$or(a=$eq(\"1\"))"));
		assertTrue(e.getMessage().contains("$or takes at least two items"));
	}

	@Test
	void a08_andArityError() {
		assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("$and(a=$eq(\"1\"))"));
	}

	@Test
	void a09_notArityError() {
		var e = assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("$not(a=$eq(\"1\"),b=$eq(\"2\"))"));
		assertTrue(e.getMessage().contains("$not takes exactly one item"));
	}

	@Test
	void a10_unbalancedGroup_unclosed() {
		assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("$or(a=$eq(\"1\"),b=$eq(\"2\")"));
	}

	@Test
	void a11_unbalancedGroup_extraClose() {
		assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("$or(a=$eq(\"1\"),b=$eq(\"2\")))"));
	}

	@Test
	void a12_duplicateTopLevelColumn_rejected() {
		var e = assertThrows(BeanQuerySyntaxException.class, () -> SearchParser.parse("name=$eq(\"1\"),name=$eq(\"2\")"));
		assertTrue(e.getMessage().contains("appears more than once"));
	}

	@Test
	void a13_duplicateColumnInsideGroup_allowed() {
		assertDoesNotThrow(() -> SearchParser.parse("$or(name=$eq(\"1\"),name=$eq(\"2\"))"));
	}

	@Test
	void a14_duplicateColumn_topPlusInsideGroup_allowed() {
		assertDoesNotThrow(() -> SearchParser.parse("name=$eq(\"1\"),$or(name=$eq(\"2\"),email=$eq(\"3\"))"));
	}

	@Test
	void a15_blankLeaf_preserved() {
		var items = SearchParser.parse("name=,age=$gt(\"5\")");
		assertEquals(List.of(new SearchItem.Leaf("name", ""), new SearchItem.Leaf("age", "$gt(\"5\")")), items);
	}

	@Test
	void a16_allBlankGroup_preserved() {
		var items = SearchParser.parse("$or(a=,b=),age=$gt(\"5\")");
		assertEquals(List.of(
			new SearchItem.Or(List.of(new SearchItem.Leaf("a", ""), new SearchItem.Leaf("b", ""))),
			new SearchItem.Leaf("age", "$gt(\"5\")")), items);
	}

	@Test
	void a17_wholeSearchBlank_parsesAsLeaf() {
		assertEquals(List.of(new SearchItem.Leaf("name", "")), SearchParser.parse("name="));
	}

	@Test
	void a18_whitespaceIgnored() {
		var items = SearchParser.parse(" $or( a=$eq(\"1\") , b=$eq(\"2\") ) ");
		assertEquals(List.of(new SearchItem.Or(List.of(new SearchItem.Leaf("a", "$eq(\"1\")"), new SearchItem.Leaf("b", "$eq(\"2\")")))), items);
	}

	@Test
	void a19_commaInsideQuotedValue_notASplit() {
		var items = SearchParser.parse("name=$contains(\"a,b\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$contains(\"a,b\")")), items);
	}

	@Test
	void a20_doubledQuoteInsideValue_notASplit() {
		var items = SearchParser.parse("name=$eq(\"a\"\"b\"),age=$gt(\"5\")");
		assertEquals(List.of(new SearchItem.Leaf("name", "$eq(\"a\"\"b\")"), new SearchItem.Leaf("age", "$gt(\"5\")")), items);
	}

	@Test
	void a21_escapedLeadingDollarColumn() {
		var items = SearchParser.parse("\\$or=$eq(\"1\")");
		assertEquals(List.of(new SearchItem.Leaf("$or", "$eq(\"1\")")), items);
	}

	@Test
	void a22_renderRoundTrip() {
		List<SearchItem> items = List.of(
			new SearchItem.Leaf("name", "$eq(\"bob\")"),
			new SearchItem.Or(List.of(new SearchItem.Leaf("a", "$eq(\"1\")"), new SearchItem.Leaf("b", "$eq(\"2\")"))),
			new SearchItem.Not(new SearchItem.Leaf("c", "$blank()")));
		String rendered = SearchParser.render(items);
		assertEquals("name=$eq(\"bob\"),$or(a=$eq(\"1\"),b=$eq(\"2\")),$not(c=$blank())", rendered);
		assertEquals(items, SearchParser.parse(rendered));
	}

	@Test
	void a23_renderEscapesLeadingDollarColumn() {
		assertEquals("\\$or=$eq(\"1\")", SearchParser.render(List.of(new SearchItem.Leaf("$or", "$eq(\"1\")"))));
	}
}
