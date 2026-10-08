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

import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class InMemoryMatch_Test extends TestBase {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static SearchExpression bare(String value, boolean quoted) {
		return SearchExpression.literal(value, quoted);
	}

	private static SearchExpression fn(String opName, String...args) {
		var op = OPS.get(opName);
		var argNodes = new ArrayList<SearchExpression>();
		for (var a : args)
			argNodes.add(SearchExpression.literal(a, false));
		return SearchExpression.func(op, argNodes);
	}

	//====================================================================================================
	// a - ? and * wildcards on bare TEXT/ID
	//====================================================================================================

	@Test
	void a01_star_matchesAnyRun() {
		assertTrue(InMemoryMatch.matches(bare("a*c", false), "abc", SearchType.TEXT));
		assertTrue(InMemoryMatch.matches(bare("a*c", false), "ac", SearchType.TEXT));  // * matches empty too
		assertFalse(InMemoryMatch.matches(bare("a*c", false), "abd", SearchType.TEXT));
	}

	@Test
	void a02_question_matchesExactlyOneChar() {
		assertTrue(InMemoryMatch.matches(bare("a?c", false), "abc", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(bare("a?c", false), "ac", SearchType.TEXT));   // ? does not match empty
		assertFalse(InMemoryMatch.matches(bare("a?c", false), "abbc", SearchType.TEXT));
	}

	@Test
	void a03_starAndQuestion_combine() {
		assertTrue(InMemoryMatch.matches(bare("a*?c", false), "abbbc", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(bare("a*?c", false), "ac", SearchType.TEXT));  // ?c needs >=1 char before c
	}

	@Test
	void a04_leadingAndTrailingWildcard() {
		assertTrue(InMemoryMatch.matches(bare("*bc", false), "abc", SearchType.ID));
		assertTrue(InMemoryMatch.matches(bare("ab*", false), "abc", SearchType.ID));
		assertTrue(InMemoryMatch.matches(bare("?bc", false), "abc", SearchType.ID));
		assertFalse(InMemoryMatch.matches(bare("?bc", false), "bc", SearchType.ID));
	}

	@Test
	void a05_noWildcard_isCiSubstring() {
		assertTrue(InMemoryMatch.matches(bare("OpEn", false), "the case is open now", SearchType.TEXT));
	}

	//====================================================================================================
	// b - quoted bare bypasses wildcard interpretation
	//====================================================================================================

	@Test
	void b01_quotedStar_isLiteralSubstring() {
		assertTrue(InMemoryMatch.matches(bare("a*c", true), "xx a*c xx", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(bare("a*c", true), "abc", SearchType.TEXT));  // no longer a wildcard
	}

	@Test
	void b02_quotedEnum_isCiEqualsEvenWithWildcardChars() {
		assertTrue(InMemoryMatch.matches(bare("a*c", true), "A*C", SearchType.ENUM));
		assertFalse(InMemoryMatch.matches(bare("a*c", true), "abc", SearchType.ENUM));
	}

	@Test
	void b03_quotedVersion_isUnaffected() {
		assertTrue(InMemoryMatch.matches(bare("1.2", true), "1.2.3", SearchType.VERSION));
	}

	//====================================================================================================
	// c - $eq / $ne / $in exact, $eqic case-insensitive (D2)
	//====================================================================================================

	@Test
	void c01_eq_isExact() {
		assertTrue(InMemoryMatch.matches(fn("$eq", "OPEN"), "OPEN", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(fn("$eq", "OPEN"), "open", SearchType.TEXT));
	}

	@Test
	void c02_eqic_isCaseInsensitive() {
		assertTrue(InMemoryMatch.matches(fn("$eqic", "OPEN"), "open", SearchType.TEXT));
		assertTrue(InMemoryMatch.matches(fn("$eqic", "open"), "OPEN", SearchType.ENUM));
	}

	@Test
	void c03_in_isExactPerValue() {
		assertTrue(InMemoryMatch.matches(fn("$in", "OPEN", "CLOSED"), "OPEN", SearchType.ENUM));
		assertFalse(InMemoryMatch.matches(fn("$in", "OPEN", "CLOSED"), "open", SearchType.ENUM));
	}

	@Test
	void c04_ne_isExactPerValue_blankKept() {
		assertTrue(InMemoryMatch.matches(fn("$ne", "OPEN", "CLOSED"), "open", SearchType.ENUM));  // case differs -> kept
		assertFalse(InMemoryMatch.matches(fn("$ne", "OPEN", "CLOSED"), "OPEN", SearchType.ENUM));
		assertTrue(InMemoryMatch.matches(fn("$ne", "OPEN", "CLOSED"), "", SearchType.ENUM));  // blank always kept
	}

	@Test
	void c05_eq_numericUnaffectedByExactness() {
		assertTrue(InMemoryMatch.matches(fn("$eq", "42"), 42, SearchType.NUMERIC));
	}

	//====================================================================================================
	// d - bare ENUM gains the wildcard/no-wildcard split
	//====================================================================================================

	@Test
	void d01_enumBare_noWildcard_isCiEquals() {
		assertTrue(InMemoryMatch.matches(bare("open", false), "OPEN", SearchType.ENUM));
		assertFalse(InMemoryMatch.matches(bare("op", false), "OPEN", SearchType.ENUM));  // equals, not substring
	}

	@Test
	void d02_enumBare_withWildcard_isCiFullMatch() {
		assertTrue(InMemoryMatch.matches(bare("op*", false), "OPEN", SearchType.ENUM));
		assertFalse(InMemoryMatch.matches(bare("op*", false), "REOPEN", SearchType.ENUM));  // full match, not substring
	}

	//====================================================================================================
	// e - regression smoke: unchanged operators still work
	//====================================================================================================

	@Test
	void e01_contains_treatsWildcardCharsAsLiteral() {
		assertTrue(InMemoryMatch.matches(fn("$contains", "a*c"), "xx a*c xx", SearchType.TEXT));
		assertFalse(InMemoryMatch.matches(fn("$contains", "a*c"), "abc", SearchType.TEXT));
	}

	@Test
	void e02_blank_and_between_unaffected() {
		assertTrue(InMemoryMatch.matches(fn("$blank"), "", SearchType.TEXT));
		assertTrue(InMemoryMatch.matches(fn("$between", "1", "5"), 3, SearchType.NUMERIC));
	}

	//====================================================================================================
	// f - bare TIMESTAMP date coerces to an exact UTC-midnight instant
	//====================================================================================================

	@Test
	void f01_bareDate_matchesExactUtcMidnightInstant() {
		assertTrue(InMemoryMatch.matches(bare("2024-01-01", false), "2024-01-01T00:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f02_bareDate_doesNotMatchLaterSameDayInstant() {
		assertFalse(InMemoryMatch.matches(bare("2024-01-01", false), "2024-01-01T08:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f03_bareDate_doesNotMatchDifferentDay() {
		assertFalse(InMemoryMatch.matches(bare("2024-01-01", false), "2024-01-02T00:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f04_eqOperator_bareDateArgument_sameExactSemantics() {
		assertTrue(InMemoryMatch.matches(fn("$eq", "2024-01-01"), "2024-01-01T00:00:00Z", SearchType.TIMESTAMP));
		assertFalse(InMemoryMatch.matches(fn("$eq", "2024-01-01"), "2024-01-01T08:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f05_betweenDateAndNextDay_coversTheWholeDay() {
		assertTrue(InMemoryMatch.matches(fn("$between", "2024-01-01", "2024-01-02"), "2024-01-01T08:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f06_malformedDate_stillReturnsNoMatch_notAnException() {
		assertFalse(InMemoryMatch.matches(bare("2024-13-40", false), "2024-01-01T00:00:00Z", SearchType.TIMESTAMP));  // not a real calendar date
	}

	@Test
	void f07_fullIsoInstant_unaffectedByThisChange() {
		assertTrue(InMemoryMatch.matches(bare("2024-01-01T08:00:00Z", false), "2024-01-01T08:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f08_epochMillis_unaffectedByThisChange() {
		assertTrue(InMemoryMatch.matches(bare("0", false), 0L, SearchType.TIMESTAMP));
	}

	@Test
	void f09_dayOfMonthOverflow_doesNotRolloverToNextMonth() {
		// LocalDate.parse rejects "2024-02-30" outright (no such day), unlike a naive Date.UTC(y, m, d)-style
		// construction, which would silently roll it over to 2024-03-01. Guards the exact gap found in the JS port.
		assertFalse(InMemoryMatch.matches(bare("2024-02-30", false), "2024-03-01T00:00:00Z", SearchType.TIMESTAMP));
	}

	@Test
	void f10_bareDate_yearUnder100_resolvesToCorrectCentury() {
		// Guards the JS port's Date.UTC(2000,...)-plus-setUTCFullYear(y) workaround for years<100:
		// a naive implementation could land on 1999/2099 instead of year 99 itself.
		var instant = LocalDate.of(99, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
		assertTrue(InMemoryMatch.matches(bare("0099-01-01", false), instant.toString(), SearchType.TIMESTAMP));
		assertFalse(InMemoryMatch.matches(bare("0099-01-01", false), instant.plusSeconds(1).toString(), SearchType.TIMESTAMP));
	}

	//====================================================================================================
	// g - compareCells (sort) stays transitive even when one side resists coercion
	//====================================================================================================

	@Test
	void g01_numericNonCoercibleSortsAfterEveryCoercibleValue() {
		assertTrue(InMemoryMatch.compareCells("9", "5x", SearchType.NUMERIC) < 0);
		assertTrue(InMemoryMatch.compareCells("10", "5x", SearchType.NUMERIC) < 0);
		assertTrue(InMemoryMatch.compareCells("5x", "9", SearchType.NUMERIC) > 0);
	}

	@Test
	void g02_numericCoercibleOrderIsUnaffected() {
		assertTrue(InMemoryMatch.compareCells("9", "10", SearchType.NUMERIC) < 0);
	}

	@Test
	void g03_numericBothNonCoercible_fallsBackToCaseInsensitiveString() {
		assertTrue(InMemoryMatch.compareCells("abc", "ABD", SearchType.NUMERIC) < 0);
	}

	@Test
	void g04_previouslyCyclicTripleIsNowAConsistentTotalOrder() {
		// Before this fix: compareCells("10","5x")<0, compareCells("5x","9")<0, but compareCells("9","10")<0 too --
		// a 3-cycle (10 < "5x" < 9 < 10) that violates Comparator's transitivity contract. After the fix, "5x" (not
		// coercible) sorts after both 9 and 10 (coercible), giving one consistent order.
		var values = new ArrayList<>(List.of("10", "5x", "9"));
		values.sort((a, b) -> InMemoryMatch.compareCells(a, b, SearchType.NUMERIC));
		assertEquals(List.of("9", "10", "5x"), values);
	}

	@Test
	void g05_timestampNonCoercibleSortsAfterCoercible() {
		assertTrue(InMemoryMatch.compareCells("2024-01-01T00:00:00Z", "not-a-date", SearchType.TIMESTAMP) < 0);
		assertTrue(InMemoryMatch.compareCells("not-a-date", "2024-01-01T00:00:00Z", SearchType.TIMESTAMP) > 0);
	}

	@Test
	void g06_versionNonCoercibleSortsAfterCoercible() {
		assertTrue(InMemoryMatch.compareCells("1.2.3", "latest", SearchType.VERSION) < 0);
		assertTrue(InMemoryMatch.compareCells("latest", "1.2.3", SearchType.VERSION) > 0);
	}

	//====================================================================================================
	// h - operator x type coverage matrix
	//====================================================================================================

	/** One value per type that every applicable operator can be driven against without throwing. */
	private static Object matchingCell(SearchType type) {
		return switch (type) {
			case TEXT, ID -> "needle";
			case NUMERIC -> 5;
			case TIMESTAMP -> "2024-06-15T00:00:00Z";
			case VERSION -> "1.2.3";
			case ENUM -> "OPEN";
			case BOOLEAN -> true;
		};
	}

	private static String matchingArg(SearchType type) {
		return switch (type) {
			case TEXT, ID -> "needle";
			case ENUM -> "OPEN";  // Matches matchingCell(ENUM) for consistency.
			case NUMERIC -> "5";
			case TIMESTAMP -> "2024-06-15T00:00:00Z";
			case VERSION -> "1.2.3";
			case BOOLEAN -> "true";
		};
	}

	@Test
	void h01_everyApplicableOperatorTypeCombinationRunsWithoutThrowing() {
		for (var op : OPS.operators()) {
			if (op.isCombinator())
				continue;  // Combinators wrap sub-expressions; exercised structurally by sections a-e, not per-leaf.
			for (var type : SearchType.values()) {
				if (! op.appliesTo(type))
					continue;
				var cell = matchingCell(type);
				var arg = matchingArg(type);
				SearchExpression node;
				if (op.minArgs() == 0)
					node = fn(op.name());
				else if (op.name().equals("$between"))
					node = fn(op.name(), arg, arg);
				else
					node = fn(op.name(), arg);
				// Not asserting the boolean result here (each operator already has its own dedicated test in
				// sections a-e above) -- this matrix's job is purely to drive every applicable (operator, type)
				// pair through matches() at least once, covering the per-type branches in eq()/contains()/
				// wildcard()/prefix()/between()/cmp() that today's hand-picked tests do not all reach.
				assertDoesNotThrow(() -> InMemoryMatch.matches(node, cell, type), () -> op.name() + " on " + type);
			}
		}
	}

	//====================================================================================================
	// i - matchLeaf's default case must only mean "$between"; a custom=false operator that reaches in-memory
	// evaluation under a name matchLeaf doesn't recognize must fail clearly instead of silently being treated
	// as $between. This used to be reachable via copy()+rename of a real built-in, but SearchOperator.Builder#
	// build() now rejects renaming a built-in copy (design ruling, see SearchOperator_Test#f01), so the only way
	// left to construct a custom=false operator under an unrecognized name is the package-private
	// SearchOperator#builtin(...) factory directly (this test class shares its package). This covers both ways
	// the old unconditional fallthrough was wrong: an unrecognized operator with FEWER than 2 args crashed
	// (args.get(1) on a too-short list, i01); one with EXACTLY 2 args never crashed at all and would have
	// silently been evaluated as if it were a real $between call (i02) - the more dangerous of the two, since it
	// fails silently rather than loudly.
	//====================================================================================================

	@Test
	void i01_unrecognizedBuiltinName_fewerArgsThanBetween_throwsClearException() {
		// Shaped like $gt (1 arg); matchLeaf's default must not fall into the "$between" branch and call
		// args.get(1) on a 1-element list.
		var unrecognized = SearchOperator.builtin("$notreal1", 1, 1, false, SearchType.NUMERIC);
		var node = SearchExpression.func(unrecognized, List.of(SearchExpression.literal("5", false)));
		var e = assertThrows(IllegalArgumentException.class, () -> InMemoryMatch.matches(node, 10, SearchType.NUMERIC));
		assertEquals("Unsupported operator '$notreal1' for in-memory evaluation.", e.getMessage());
	}

	@Test
	void i02_unrecognizedBuiltinName_sameArityAsBetween_doesNotSilentlyMatchAsBetween() {
		// Shaped like $between (2 args), so the old default branch wouldn't have crashed at all -- it would have
		// silently evaluated this operator as if it were a real $between call. The fix's unconditional throw must
		// catch this case too, not just the lower-arity crash in i01.
		var unrecognized = SearchOperator.builtin("$notreal2", 2, 2, false, SearchType.NUMERIC);
		var node = SearchExpression.func(unrecognized, List.of(SearchExpression.literal("1", false), SearchExpression.literal("5", false)));
		var e = assertThrows(IllegalArgumentException.class, () -> InMemoryMatch.matches(node, 3, SearchType.NUMERIC));
		assertEquals("Unsupported operator '$notreal2' for in-memory evaluation.", e.getMessage());
	}

	//====================================================================================================
	// j - typed values (design #4 D2): typed args compared directly; millis() widened to java.time cell types
	//====================================================================================================

	/** Types a bare literal or a leaf operator's arguments the way QueryResolver does, to test InMemoryMatch in isolation. */
	private static SearchExpression typed(SearchExpression node, SearchType type) {
		if (node.isLiteral())
			return node.withTypedValue(type.parse(node.value()));
		var newArgs = new ArrayList<SearchExpression>();
		for (var a : node.args())
			newArgs.add(a.isLiteral() ? a.withTypedValue(type.parse(a.value())) : typed(a, type));
		return SearchExpression.func(node.operator(), newArgs);
	}

	@Test
	void j01_eqNumeric_typedCellAndArg() {
		var e = typed(fn("$eq", "30"), SearchType.NUMERIC);
		assertTrue(InMemoryMatch.matches(e, new java.math.BigDecimal("30"), SearchType.NUMERIC));
		assertTrue(InMemoryMatch.matches(e, 30, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(e, 31, SearchType.NUMERIC));
	}

	@Test
	void j02_gtAndBetweenNumeric() {
		var gt = typed(fn("$gt", "5"), SearchType.NUMERIC);
		assertTrue(InMemoryMatch.matches(gt, 6, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(gt, 5, SearchType.NUMERIC));
		var between = typed(fn("$between", "1", "100"), SearchType.NUMERIC);
		assertTrue(InMemoryMatch.matches(between, 50, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(between, 200, SearchType.NUMERIC));
	}

	@Test
	void j03_bareLiteralTypedBoolean() {
		var e = typed(bare("true", false), SearchType.BOOLEAN);
		assertTrue(InMemoryMatch.matches(e, Boolean.TRUE, SearchType.BOOLEAN));
		assertTrue(InMemoryMatch.matches(e, "true", SearchType.BOOLEAN));
		assertFalse(InMemoryMatch.matches(e, Boolean.FALSE, SearchType.BOOLEAN));
	}

	@Test
	void j04_timestamp_typedArg_variousCellShapes() {
		var e = typed(fn("$gt", "2026-01-01T00:00:00Z"), SearchType.TIMESTAMP);
		assertTrue(InMemoryMatch.matches(e, OffsetDateTime.parse("2026-06-01T00:00:00Z"), SearchType.TIMESTAMP));
		assertTrue(InMemoryMatch.matches(e, LocalDate.of(2026, 6, 1), SearchType.TIMESTAMP));
		assertTrue(InMemoryMatch.matches(e, LocalDateTime.of(2026, 6, 1, 0, 0), SearchType.TIMESTAMP));
		assertTrue(InMemoryMatch.matches(e, ZonedDateTime.parse("2026-06-01T00:00:00Z"), SearchType.TIMESTAMP));
		assertFalse(InMemoryMatch.matches(e, LocalDate.of(2025, 1, 1), SearchType.TIMESTAMP));
	}

	@Test
	void j05_containsUnaffectedByTyping() {
		assertTrue(InMemoryMatch.matches(fn("$contains", "err"), "server error", SearchType.TEXT));
	}

	@Test
	void j06_compareCells_timestampWidenedTypes() {
		assertTrue(InMemoryMatch.compareCells(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), SearchType.TIMESTAMP) < 0);
		assertTrue(InMemoryMatch.compareCells(LocalDateTime.of(2026, 6, 1, 0, 0), OffsetDateTime.parse("2026-01-01T00:00:00Z"), SearchType.TIMESTAMP) > 0);
	}

	@Test
	void j07_unparseableCell_isNoMatch_notAnError() {
		var e = typed(fn("$gt", "5"), SearchType.NUMERIC);
		assertFalse(InMemoryMatch.matches(e, "abc", SearchType.NUMERIC));
	}

	@Test
	void j08_numericCell_nanAndInfinity_areUnparseable() {
		var e = typed(fn("$gt", "5"), SearchType.NUMERIC);
		assertFalse(InMemoryMatch.matches(e, Double.NaN, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(e, Double.POSITIVE_INFINITY, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(e, Float.NaN, SearchType.NUMERIC));
		assertFalse(InMemoryMatch.matches(e, Float.NEGATIVE_INFINITY, SearchType.NUMERIC));
	}

	@Test
	void j09_compareCells_numericNanSortsAfterEveryNumber() {
		assertTrue(InMemoryMatch.compareCells(5, Double.NaN, SearchType.NUMERIC) < 0);
		assertTrue(InMemoryMatch.compareCells(Double.POSITIVE_INFINITY, 5, SearchType.NUMERIC) > 0);
		assertTrue(InMemoryMatch.compareCells(5, "abc", SearchType.NUMERIC) < 0);
	}

	@Test
	void j10_localDateArg_matchesUtcMidnightInstant() {
		var e = typed(fn("$eq", "2026-06-01"), SearchType.TIMESTAMP);
		assertTrue(InMemoryMatch.matches(e, Instant.parse("2026-06-01T00:00:00Z"), SearchType.TIMESTAMP));
		assertFalse(InMemoryMatch.matches(e, Instant.parse("2026-06-01T05:00:00Z"), SearchType.TIMESTAMP));
	}

	@Test
	void j11_localDateTimeArg_matchesUtcInstant() {
		var e = typed(fn("$eq", "2026-06-01T05:00"), SearchType.TIMESTAMP);
		assertTrue(InMemoryMatch.matches(e, Instant.parse("2026-06-01T05:00:00Z"), SearchType.TIMESTAMP));
		assertFalse(InMemoryMatch.matches(e, Instant.parse("2026-06-01T06:00:00Z"), SearchType.TIMESTAMP));
	}

	@Test
	void j12_stringCell_sharesLiteralParser() {
		var e = typed(fn("$eq", "2026-06-01T05:00:00Z"), SearchType.TIMESTAMP);
		assertTrue(InMemoryMatch.matches(e, "2026-06-01T05:00:00+00:00", SearchType.TIMESTAMP));   // offset form
		assertTrue(InMemoryMatch.matches(e, "2026-06-01T05:00:00", SearchType.TIMESTAMP));         // local date-time = UTC
		assertTrue(InMemoryMatch.matches(e, "1780290000000", SearchType.TIMESTAMP));               // epoch millis string
		assertFalse(InMemoryMatch.matches(e, "+1780290000000", SearchType.TIMESTAMP));             // '+' not accepted
		assertFalse(InMemoryMatch.matches(e, "garbage", SearchType.TIMESTAMP));
	}

	@Test
	void j13_stringCellDuration_isUnparseableNotNowRelative() {
		var e = typed(fn("$lte", "+100000d"), SearchType.TIMESTAMP);
		assertFalse(InMemoryMatch.matches(e, "-24h", SearchType.TIMESTAMP));  // Unparseable cell, never "now minus a day".
		assertTrue(InMemoryMatch.compareCells("-24h", Instant.parse("2026-06-01T05:00:00Z"), SearchType.TIMESTAMP) > 0);  // Sorts last.
	}
}
