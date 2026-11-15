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

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Numeric comparison and formatting in bean queries treat decimals beyond the size limits of
 * <c>StringUtils.parseBigDecimal(String)</c> (more than 1,000 characters, or a
 * decimal exponent larger than 10,000 in magnitude) as non-numeric.
 */
class BeanQuery_NumberLimits_Test extends TestBase {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static final String LARGE_EXPONENT = "1e100000";
	private static final String MAX_EXPONENT = "1e10000";
	private static final String LONG_DIGITS = "1".repeat(1001);
	private static final String MAX_DIGITS = "1".repeat(1000);

	private static SearchExpression typedFn(String opName, String...args) {
		var argNodes = new ArrayList<SearchExpression>();
		for (var a : args)
			argNodes.add(SearchExpression.literal(a, false).withTypedValue(SearchType.NUMERIC.parse(a)));
		return SearchExpression.func(OPS.get(opName), argNodes);
	}

	//====================================================================================================
	// a - InMemoryMatch comparison
	//====================================================================================================

	@Test
	void a01_gt_largeExponentCell_doesNotMatch() {
		var gt = typedFn("$gt", "5");
		assertFalse(InMemoryMatch.matches(gt, LARGE_EXPONENT, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(gt, "-" + LARGE_EXPONENT, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(typedFn("$lt", "5"), "1e-100000", SearchType.NUMERIC));
	}

	@Test
	void a02_gt_tooLongCell_doesNotMatch() {
		assertFalse(InMemoryMatch.matches(typedFn("$gt", "5"), LONG_DIGITS, SearchType.NUMERIC));
	}

	@Test
	void a03_between_overLimitCell_doesNotMatch() {
		var between = typedFn("$between", "1", "100");
		assertFalse(InMemoryMatch.matches(between, LARGE_EXPONENT, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(between, LONG_DIGITS, SearchType.NUMERIC));
	}

	@Test
	void a04_gt_atLimitCell_stillCompares() {
		var gt = typedFn("$gt", "5");
		assertTrue(InMemoryMatch.matches(gt, MAX_EXPONENT, SearchType.NUMERIC));
		assertTrue(InMemoryMatch.matches(gt, MAX_DIGITS, SearchType.NUMERIC));
	}

	@Test
	void a05_compareCells_overLimitSortsAfterNumbers() {
		assertTrue(InMemoryMatch.compareCells("9", LARGE_EXPONENT, SearchType.NUMERIC) < 0);
		assertTrue(InMemoryMatch.compareCells(LARGE_EXPONENT, "9", SearchType.NUMERIC) > 0);
		assertTrue(InMemoryMatch.compareCells("9", LONG_DIGITS, SearchType.NUMERIC) < 0);
		assertTrue(InMemoryMatch.compareCells(MAX_EXPONENT, "9", SearchType.NUMERIC) > 0);  // In range: compared by value.
	}

	//====================================================================================================
	// b - ValueFormat formatting
	//====================================================================================================

	@Test
	void b01_format_largeExponent_rejected() {
		var e = assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(new BigDecimal(LARGE_EXPONENT)));
		assertTrue(e.getMessage().contains("Unsupported search value type"), e.getMessage());
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(new BigDecimal("1e-100000")));
	}

	@Test
	void b02_format_tooLong_rejected() {
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(new BigDecimal(LONG_DIGITS)));
	}

	@Test
	void b03_format_atLimit_rendersPlain() {
		assertEquals(MAX_DIGITS, ValueFormat.format(new BigDecimal(MAX_DIGITS)));
		assertEquals("1" + "0".repeat(20), ValueFormat.format(new BigDecimal("1e20")));
	}
}
