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

class ClauseParser_Test extends TestBase {

	//====================================================================================================
	// Splitting / structure
	//====================================================================================================

	@Test void a01_nullOrBlank_emptyMap() {
		assertTrue(ClauseParser.parse(null).isEmpty());
		assertTrue(ClauseParser.parse("").isEmpty());
		assertTrue(ClauseParser.parse("   ").isEmpty());
	}

	@Test void a02_singleClause() {
		assertEquals(Map.of("name", "$eq(a)"), ClauseParser.parse("name=$eq(a)"));
	}

	@Test void a03_multiClause_topLevelCommaSplit() {
		var m = ClauseParser.parse("name=$eq(a),age=$gt(1)");
		assertMap(m, "name=$eq(a)", "age=$gt(1)");
	}

	@Test void a04_parenProtectedComma_notAClauseBoundary() {
		var m = ClauseParser.parse("status=$in(OPEN,CLOSED)");
		assertMap(m, "status=$in(OPEN,CLOSED)");
	}

	@Test void a05_nestedParens_protected() {
		var m = ClauseParser.parse("status=$or($eq(A),$eq(B))");
		assertMap(m, "status=$or($eq(A),$eq(B))");
	}

	@Test void a06_firstUnescapedEquals_furtherEqualsKeptInValue() {
		assertEquals("a=b=c", ClauseParser.parse("k=a=b=c").get("k"));
	}

	@Test void a07_equalsInsideParens_valueVerbatim() {
		assertEquals("$regex(er.*,flags=m)", ClauseParser.parse("name=$regex(er.*,flags=m)").get("name"));
	}

	@Test void a08_noEquals_blankValue() {
		assertEquals("", ClauseParser.parse("solo").get("solo"));
	}

	@Test void a09_keyStrippedOfWhitespace() {
		assertEquals("$eq(a)", ClauseParser.parse("  name  =$eq(a)").get("name"));
	}

	@Test void a10_blankClausesIgnored() {
		var m = ClauseParser.parse("a=1,,b=2,");
		assertMap(m, "a=1", "b=2");
	}

	@Test void a11_duplicateKey_laterWins() {
		var m = ClauseParser.parse("k=1,k=2");
		assertMap(m, "k=2");
	}

	@Test void a12_insertionOrderPreserved() {
		assertEquals(List.of("b", "a", "c"), new ArrayList<>(ClauseParser.parse("b=1,a=2,c=3").keySet()));
	}

	//====================================================================================================
	// Backslash escaping
	//====================================================================================================

	@Test void b01_escapedComma_isLiteralNotABoundary() {
		var m = ClauseParser.parse("k=a\\,b");
		assertMap(m, "k=a,b");
	}

	@Test void b02_escapedEquals_notASeparator() {
		var m = ClauseParser.parse("a\\=b=v");
		assertEquals("v", m.get("a=b"));
	}

	@Test void b03_escapedBackslash_isLiteral() {
		assertEquals("a\\b", ClauseParser.parse("k=a\\\\b").get("k"));
	}

	@Test void b04_unknownBackslash_leftAsIs() {
		assertEquals("a\\b", ClauseParser.parse("k=a\\b").get("k"));
	}

	@Test void b05_escapedDollar_isLiteral() {
		var m = ClauseParser.parse("\\$or=v");
		assertMap(m, "$or=v");
	}

	//====================================================================================================
	// escape() and round-trip
	//====================================================================================================

	@Test void c01_escape_null() {
		assertNull(ClauseParser.escape(null));
	}

	@Test void c02_escape_escapesSpecials() {
		assertEquals("a\\\\b\\,c\\=d", ClauseParser.escape("a\\b,c=d"));
	}

	@Test void c03_escape_roundTrips_awkwardKey() {
		var key = "we,ird=key\\end";
		var m = ClauseParser.parse(ClauseParser.escape(key) + "=v");
		assertMap(m, key + "=v");
	}

	@Test void c04_escape_leadingDollar() {
		assertEquals("\\$or", ClauseParser.escape("$or"));
		assertEquals("a$b", ClauseParser.escape("a$b")); // only a LEADING $ is escaped
	}
}
