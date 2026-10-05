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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class SearchOperatorSet_Test extends TestBase {

	@Test
	void a01_withoutRemovesOneOperator() {
		var std = SearchOperatorSet.standard();
		var s = std.without("$regex");
		assertFalse(s.contains("$regex"));
		assertTrue(s.contains("$eq"));
		assertEquals(std.operators().size() - 1, s.operators().size());
		assertTrue(std.contains("$regex"));  // The original is unchanged.
	}

	@Test
	void a02_withoutUnknownOrNullReturnsSameSet() {
		var std = SearchOperatorSet.standard();
		assertSame(std, std.without("$nope"));
		assertSame(std, std.without(null));
	}

	//====================================================================================================
	// b - case-insensitive lookup (D6)
	//====================================================================================================

	@Test
	void b01_get_caseInsensitive() {
		var std = SearchOperatorSet.standard();
		assertSame(std.get("$eq"), std.get("$EQ"));
		assertSame(std.get("$eq"), std.get("$Eq"));
	}

	@Test
	void b02_contains_caseInsensitive() {
		var std = SearchOperatorSet.standard();
		assertTrue(std.contains("$Between"));
	}

	@Test
	void b03_without_caseInsensitive() {
		var std = SearchOperatorSet.standard();
		var s = std.without("$REGEX");
		assertFalse(s.contains("$regex"));
	}

	@Test
	void b04_get_returnsCanonicalCasing() {
		var std = SearchOperatorSet.standard();
		assertEquals("$eq", std.get("$EQ").name());
	}

	//====================================================================================================
	// c - of()/with() case-only-duplicate rejection (D6)
	//====================================================================================================

	@Test
	void c01_of_rejectsExactDuplicate() {
		var a = SearchOperator.create("$near", "help").predicate((cell, args) -> true).build();
		var b = SearchOperator.create("$near", "help2").predicate((cell, args) -> true).build();
		assertThrows(IllegalArgumentException.class, () -> SearchOperatorSet.of(a, b));
	}

	@Test
	void c02_of_rejectsCaseOnlyDuplicate() {
		var a = SearchOperator.create("$near", "help").predicate((cell, args) -> true).build();
		var b = SearchOperator.create("$Near", "help2").predicate((cell, args) -> true).build();
		assertThrows(IllegalArgumentException.class, () -> SearchOperatorSet.of(a, b));
	}

	@Test
	void c03_with_replacesIgnoringCase() {
		var a = SearchOperator.create("$near", "help").predicate((cell, args) -> true).build();
		var set = SearchOperatorSet.of(a);
		var b = SearchOperator.create("$Near", "help2").predicate((cell, args) -> true).build();
		var replaced = set.with(b);
		assertEquals(1, replaced.operators().size());
		assertEquals("$Near", replaced.get("$near").name());
	}

	//====================================================================================================
	// d - $eq / $eqic / $in / $ne (D2)
	//====================================================================================================

	@Test
	void d01_standard_hasEqic() {
		var std = SearchOperatorSet.standard();
		assertTrue(std.contains("$eqic"));
		var eqic = std.get("$eqic");
		assertBean(eqic, "minArgs,maxArgs", "1,1");
		assertTrue(eqic.appliesTo(SearchType.TEXT));
		assertTrue(eqic.appliesTo(SearchType.ID));
		assertTrue(eqic.appliesTo(SearchType.ENUM));
		assertFalse(eqic.appliesTo(SearchType.NUMERIC));
	}

	@Test
	void d02_eqicPlacedRightAfterEq() {
		var names = SearchOperatorSet.standard().operators().stream().map(SearchOperator::name).toList();
		assertEquals(names.indexOf("$eq") + 1, names.indexOf("$eqic"));
	}

	@Test
	void d03_eq_helpTextIsExact() {
		assertEquals("Exact, case-sensitive match. Example: $eq(OPEN)", SearchOperatorSet.standard().get("$eq").help());
	}

	@Test
	void d04_eqic_helpText() {
		assertEquals("Case-insensitive match. Example: $eqic(open)", SearchOperatorSet.standard().get("$eqic").help());
	}

	@Test
	void d05_in_helpTextMentionsCaseSensitive() {
		assertEquals("Any of the given values, case-sensitive (1 or more). Example: $in(OPEN,CLOSED)",
			SearchOperatorSet.standard().get("$in").help());
	}

	@Test
	void d06_ne_helpTextMentionsCaseSensitive() {
		assertEquals("None of the given values, case-sensitive (1 or more). Blank cells are kept. Example: $ne(OPEN,CLOSED)",
			SearchOperatorSet.standard().get("$ne").help());
	}

	//====================================================================================================
	// e - of/with/get/contains/forType edge cases
	//====================================================================================================

	@Test
	void e01_ofBuildsExactSetNoBuiltinsImplied() {
		var eq = SearchOperatorSet.standard().get("$eq");
		var s = SearchOperatorSet.of(eq);
		assertEquals(1, s.operators().size());
		assertFalse(s.contains("$ne"));
	}

	@Test
	void e02_ofRejectsNullOperatorsArray() {
		assertThrows(IllegalArgumentException.class, () -> SearchOperatorSet.of((SearchOperator[]) null));
	}

	@Test
	void e03_ofRejectsNullElement() {
		assertThrows(IllegalArgumentException.class, () -> SearchOperatorSet.of((SearchOperator) null));
	}

	@Test
	void e04_ofRejectsDuplicateOperatorName() {
		var eq = SearchOperatorSet.standard().get("$eq");
		assertThrows(IllegalArgumentException.class, () -> SearchOperatorSet.of(eq, eq));
	}

	@Test
	void e05_withRejectsNull() {
		var hoistedTarget1 = SearchOperatorSet.standard();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget1.with(null));
	}

	@Test
	void e06_withReplacesSameNamedOperator() {
		var std = SearchOperatorSet.standard();
		var s = std.with(std.get("$eq"));
		assertEquals(std.operators().size(), s.operators().size());  // Same count: replace, not add.
	}

	@Test
	void e07_getNullReturnsNull() {
		assertNull(SearchOperatorSet.standard().get(null));
	}

	@Test
	void e08_getUnknownReturnsNull() {
		assertNull(SearchOperatorSet.standard().get("$nope"));
	}

	@Test
	void e09_containsNullIsFalse() {
		assertFalse(SearchOperatorSet.standard().contains(null));
	}

	@Test
	void e10_forTypeNullReturnsEmptyList() {
		assertTrue(SearchOperatorSet.standard().forType(null).isEmpty());
	}

	@Test
	void e11_forTypeIncludesCombinatorsForEveryType() {
		for (var type : SearchType.values())
			assertTrue(SearchOperatorSet.standard().forType(type).stream().anyMatch(op -> op.name().equals("$and")));
	}

	@Test
	void e12_forTypeExcludesOperatorsThatDoNotApply() {
		assertFalse(SearchOperatorSet.standard().forType(SearchType.BOOLEAN).stream().anyMatch(op -> op.name().equals("$contains")));
	}
}
