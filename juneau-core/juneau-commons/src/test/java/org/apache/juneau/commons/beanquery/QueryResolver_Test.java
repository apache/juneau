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
import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class QueryResolver_Test extends TestBase {

	/** name TEXT, age NUMERIC, version VERSION; all other settings at their defaults. */
	private static QuerySettings settings() {
		var s = new QuerySettings();
		s.columns.put("name", TEXT);
		s.columns.put("age", NUMERIC);
		s.columns.put("version", VERSION);
		return s;
	}

	private static ResolvedQuery resolve(QuerySettings s, BeanQuery q) {
		return QueryResolver.resolve(q, s, null);
	}

	private static ResolvedQuery search(String search) {
		return resolve(settings(), new BeanQuery().setSearch(search));
	}

	private static void assertSyntax(String message, QuerySettings s, BeanQuery q) {
		assertThrowsWithMessage(BeanQuerySyntaxException.class, message, () -> QueryResolver.resolve(q, s, null));
	}

	private static void assertSyntax(String message, BeanQuery q) {
		assertSyntax(message, settings(), q);
	}

	@Nested class A_settings {

		@Test void a01_defaults() {
			var s = new QuerySettings();
			assertTrue(s.allowRegex);
			assertEquals(50, s.regexTimeout.toMillis());
			assertEquals(CountPolicy.IF_REQUESTED, s.countPolicy);
			assertEquals(100, s.defaultLimit);
			assertEquals(1000, s.maxLimit.intValue());
			assertEquals(64, s.maxSearchClauses);
			assertEquals(8, s.maxSortKeys);
			assertEquals(4096, s.maxSearchLength);
			assertEquals(16, s.maxExpressionDepth);
			assertSame(SearchOperatorSet.standard(), s.defaultOperators);
		}

		@Test void a02_validate() {
			var hoistedTarget1 = new QuerySettings();
			assertThrowsWithMessage(IllegalStateException.class, "At least one column must be declared.", () -> hoistedTarget1.validate());
			var s = settings();
			s.maxLimit = 50;
			assertThrowsWithMessage(IllegalStateException.class, "maxLimit (50) must not be less than defaultLimit (100).", s::validate);
			s.maxLimit = null;
			assertDoesNotThrow(s::validate);
		}

		@Test void a03_copyIsDeep() {
			var s = settings();
			var c = s.copy();
			c.columns.remove("age");
			c.columnOperators.put("name", SearchOperatorSet.of());
			assertTrue(s.columns.containsKey("age"));
			assertTrue(s.columnOperators.isEmpty());
		}

		@Test void a04_positiveChecks() {
			assertThrowsWithMessage(IllegalArgumentException.class, "defaultLimit must be positive: 0", () -> QuerySettings.positive("defaultLimit", 0));
			assertThrowsWithMessage(IllegalArgumentException.class, "maxLimit must be positive or null: 0", () -> QuerySettings.positiveOrNull("maxLimit", 0));
			assertNull(QuerySettings.positiveOrNull("maxLimit", null));
			assertThrowsWithMessage(IllegalArgumentException.class, "regexTimeout must be positive", () -> QuerySettings.positive("regexTimeout", java.time.Duration.ZERO));
		}
	}

	@Nested class B_search {

		@Test void b01_searchLength() {
			var s = settings();
			s.maxSearchLength = 10;
			assertSyntax("Search string exceeds 10 characters.", s, new BeanQuery().setSearch("name=" + "x".repeat(20)));
		}

		@Test void b02_clauseCount() {
			var s = settings();
			s.maxSearchClauses = 1;
			assertSyntax("Too many search clauses (max 1).", s, new BeanQuery().setSearch("name=a,age=1"));
		}

		@Test void b03_unknownColumn() {
			assertSyntax("Unknown column 'pasword' in search.", new BeanQuery().setSearch("pasword=x"));
			// Column validation runs on every leaf before the blank-value drop (design §7 row 3: "Applied to every
			// leaf"), so an unknown column with a blank value still throws, same as a non-blank value.
			assertSyntax("Unknown column 'pasword' in search.", new BeanQuery().setSearch("pasword="));
		}

		@Test void b04_blankValueSkipped() {
			assertEquals(new Filter.And(List.of()), search("name=").filter());
		}

		@Test void b05_depth() {
			var s = settings();
			s.maxExpressionDepth = 2;
			assertDoesNotThrow(() -> resolve(s, new BeanQuery().setSearch("age=$not($eq(1))")));
			assertSyntax("Search expression nested too deeply (max 2).", s, new BeanQuery().setSearch("age=$not($not($eq(1)))"));
		}

		@Test void b06_regexGate() {
			// SPEC CHANGE: $regex is enabled by default; allowRegex(false) disables it. The gate's error message is
			// exercised with allowRegex explicitly turned off.
			var s = settings();
			s.allowRegex = false;
			assertSyntax("Regex search is not enabled for this list.", s, new BeanQuery().setSearch("name=$regex(a.*)"));
			assertSyntax("Regex search is not enabled for this list.", s, new BeanQuery().setSearch("name=$not($regex(a.*))"));
			assertDoesNotThrow(() -> search("name=$regex(a.*)"));
			assertSyntax("Regex pattern exceeds 256 characters.", new BeanQuery().setSearch("name=$regex(" + "a".repeat(257) + ")"));
		}

		@Test void b07_operatorType() {
			assertSyntax("Operator $gt does not apply to text column 'name'.", new BeanQuery().setSearch("name=$gt(5)"));
			assertSyntax("Operator $contains does not apply to numeric column 'age'.", new BeanQuery().setSearch("age=$or($eq(1),$contains(x))"));
			assertDoesNotThrow(() -> search("age=$or($eq(1),$gt(5))"));  // Combinators apply to every type.
		}

		@Test void b08_filterResolved() {
			var q = search("name=Bob,age=$gt(1)");
			var f = (Filter.And) q.filter();
			assertEquals(2, f.items().size());
			var leaf = (Filter.Leaf) f.items().get(1);
			assertBean(leaf, "column,type", "age,NUMERIC");
			assertEquals("$gt", leaf.expression().name());
		}

		@Test void b09_columnOperatorsUsed() {
			var s = settings();
			s.columnOperators.put("name", SearchOperatorSet.standard().without("$eq"));
			assertSyntax("Unknown search operator", s, new BeanQuery().setSearch("name=$eq(x)"));
			assertDoesNotThrow(() -> resolve(s, new BeanQuery().setSearch("age=$eq(1)")));
		}

		// A leaf whose raw expression text opens a quote it never closes (SearchParser leaves this
		// unvalidated — see SearchParser.scanBalancedUntilDelimiter's Javadoc) must surface as UNTERMINATED_QUOTE once
		// the expression actually reaches SearchExpressionParser here, not be silently accepted as a literal filter.
		@Test void b10_unterminatedQuoteInLeaf_throws() {
			var hoistedSettings = settings();
			var hoistedQuery = new BeanQuery().setSearch("name=\"abc");
			var e = assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unterminated quoted value", () -> resolve(hoistedSettings, hoistedQuery));
			assertEquals(BeanQuerySyntaxException.Code.UNTERMINATED_QUOTE, e.code());
		}
	}

	@Nested class C_sortView {

		@Test void c01_sortKeys() {
			var q = resolve(settings(), new BeanQuery().setSort("age:desc, name, version-"));
			assertEquals(3, q.sort().size());
			assertBean(q.sort().get(0), "column,descending,type", "age,true,NUMERIC");
			assertFalse(q.sort().get(1).descending());
			assertTrue(q.sort().get(2).descending());
		}

		@Test void c02_sortLimits() {
			var s = settings();
			s.maxSortKeys = 1;
			assertSyntax("Too many sort keys (max 1).", s, new BeanQuery().setSort("name,age"));
			assertSyntax("Unknown sort column 'x'.", new BeanQuery().setSort("x"));
			assertSyntax("Unknown sort column 'toString'.", new BeanQuery().setSort("toString:asc"));
		}

		@Test void c03_view() {
			assertList(resolve(settings(), new BeanQuery()).view(), "name", "age", "version");
			assertList(resolve(settings(), new BeanQuery().setView("age, age ,name")).view(), "age", "name");
			assertSyntax("Unknown view column 'x'.", new BeanQuery().setView("name,x"));
		}

		@Test void c04_otherStringLengths() {
			var s = settings();
			s.maxSearchLength = 5;
			assertSyntax("Sort string exceeds 5 characters.", s, new BeanQuery().setSort("name,age"));
			assertSyntax("View string exceeds 5 characters.", s, new BeanQuery().setView("name,age"));
			assertSyntax("Options string exceeds 5 characters.", s, new BeanQuery().setOpts("counts=both"));
		}
	}

	@Nested class D_paging {

		@Test void d01_position() {
			assertEquals(0, resolve(settings(), new BeanQuery()).position());
			assertEquals(7, resolve(settings(), new BeanQuery().setPosition(7)).position());
			assertSyntax("Position must not be negative.", new BeanQuery().setPosition(-1));
		}

		@Test void d02_limit() {
			assertEquals(100, resolve(settings(), new BeanQuery()).limit().intValue());
			assertEquals(7, resolve(settings(), new BeanQuery().setLimit(7)).limit().intValue());
			assertEquals(0, resolve(settings(), new BeanQuery().setLimit(0)).limit().intValue());
			assertEquals(1000, resolve(settings(), new BeanQuery().setLimit(5000)).limit().intValue());
			assertEquals(1000, resolve(settings(), new BeanQuery().setLimit(-1)).limit().intValue());
		}

		@Test void d03_uncapped() {
			var s = settings();
			s.maxLimit = null;
			assertNull(resolve(s, new BeanQuery().setLimit(-1)).limit());
			assertEquals(5000, resolve(s, new BeanQuery().setLimit(5000)).limit().intValue());
			assertEquals(100, resolve(s, new BeanQuery()).limit().intValue());
		}
	}

	@Nested class E_counts {

		private CountPolicy counts(String opts) {
			return resolve(settings(), new BeanQuery().setOpts(opts)).counts();
		}

		@Test void e01_ifRequested() {
			assertEquals(CountPolicy.NONE, counts(null));
			assertEquals(CountPolicy.MATCHED, counts("counts=matched"));
			assertEquals(CountPolicy.BOTH, counts("counts=both"));
			assertEquals(CountPolicy.BOTH, counts("counts=TRUE"));
			assertEquals(CountPolicy.NONE, counts("counts=yes"));
			assertEquals(CountPolicy.NONE, counts("foo=bar"));  // Unknown opts keys are ignored.
		}

		@Test void e02_fixedPolicyIgnoresOpts() {
			var s = settings();
			s.countPolicy = CountPolicy.NONE;
			assertEquals(CountPolicy.NONE, resolve(s, new BeanQuery().setOpts("counts=both")).counts());
			s.countPolicy = CountPolicy.BOTH;
			assertEquals(CountPolicy.BOTH, resolve(s, new BeanQuery()).counts());
		}
	}

	@Nested class G_typedValues {

		private static QuerySettings typedSettings() {
			var s = settings();
			s.columns.put("active", BOOLEAN);
			s.columns.put("createdAt", TIMESTAMP);
			return s;
		}

		private static SearchExpression expr(QuerySettings s, String search) {
			var f = resolve(s, new BeanQuery().setSearch(search)).filter();
			if (f instanceof Filter.And and && and.items().size() == 1)
				f = and.items().get(0);
			return ((Filter.Leaf)f).expression();
		}

		@Test void g01_bareLiteralTyped_numeric() {
			assertEquals(new java.math.BigDecimal("30"), expr(settings(), "age=30").typedValue());
		}

		@Test void g02_bareLiteralTyped_boolean() {
			assertEquals(Boolean.TRUE, expr(typedSettings(), "active=true").typedValue());
		}

		@Test void g03_bareLiteralUntyped_textPassesThrough() {
			assertEquals("Bob", expr(settings(), "name=Bob").typedValue());
		}

		@Test void g04_valueOperatorArgTyped() {
			assertList(expr(settings(), "age=$gt(5)").typedArgs(), new java.math.BigDecimal("5"));
		}

		@Test void g05_betweenBothArgsTyped() {
			assertList(expr(settings(), "age=$between(1,100)").typedArgs(), new java.math.BigDecimal("1"), new java.math.BigDecimal("100"));
		}

		@Test void g06_combinatorChildrenTypedRecursively() {
			var children = expr(settings(), "age=$or($eq(1),$gt(5))").args();
			assertList(children.get(0).typedArgs(), new java.math.BigDecimal("1"));
			assertList(children.get(1).typedArgs(), new java.math.BigDecimal("5"));
		}

		@Test void g07_nonValueOperatorArgNotTyped() {
			assertList(expr(settings(), "name=$contains(err)").typedArgs(), "err");
		}

		@Test void g08_badBareLiteral_columnQualifiedMessage() {
			assertSyntax("Value 'abc' is not a valid numeric for column 'age'.", new BeanQuery().setSearch("age=abc"));
		}

		@Test void g09_badOperatorArg_columnQualifiedMessage() {
			assertSyntax("Value 'xyz' is not a valid numeric for column 'age'.", new BeanQuery().setSearch("age=$gt(xyz)"));
		}

		@Test void g10_badTimestamp_columnQualifiedMessage() {
			assertSyntax("Value 'not-a-date' is not a valid timestamp for column 'createdAt'.", typedSettings(), new BeanQuery().setSearch("createdAt=not-a-date"));
		}

		@Test void g11_badValue_hasBadValueCode() {
			var hoistedSettings = settings();
			var hoistedQuery = new BeanQuery().setSearch("age=abc");
			var e = assertThrows(BeanQuerySyntaxException.class, () -> resolve(hoistedSettings, hoistedQuery));
			assertEquals(BeanQuerySyntaxException.Code.BAD_VALUE, e.code());
		}

		@Test void g12_operatorTypeWinsOverBadValue() {
			// $contains does not apply to numeric; its (would-be-bad) argument is never typed, so OPERATOR_TYPE is reported.
			var hoistedSettings = settings();
			var hoistedQuery = new BeanQuery().setSearch("age=$contains(abc)");
			var e = assertThrows(BeanQuerySyntaxException.class, () -> resolve(hoistedSettings, hoistedQuery));
			assertEquals(BeanQuerySyntaxException.Code.OPERATOR_TYPE, e.code());
		}

		@Test void g12b_operatorTypeWinsOverBadValue_boolean() {
			// $gt does not apply to boolean; its bad argument is never typed, so OPERATOR_TYPE is reported, not BAD_VALUE.
			var hoistedSettings = typedSettings();
			var hoistedQuery = new BeanQuery().setSearch("active=$gt(xyz)");
			var e = assertThrows(BeanQuerySyntaxException.class, () -> resolve(hoistedSettings, hoistedQuery));
			assertEquals(BeanQuerySyntaxException.Code.OPERATOR_TYPE, e.code());
		}

		@Test void g14_bareLiteralInCombinator_typed() {
			var children = expr(settings(), "age=$or(1,$gt(5))").args();
			assertEquals(new java.math.BigDecimal("1"), children.get(0).typedValue());
			assertList(children.get(1).typedArgs(), new java.math.BigDecimal("5"));
		}

		@Test void g15_badBareLiteralInCombinator_badValue() {
			var hoistedSettings = settings();
			var hoistedQuery = new BeanQuery().setSearch("age=$or(abc,$gt(5))");
			var e = assertThrows(BeanQuerySyntaxException.class, () -> resolve(hoistedSettings, hoistedQuery));
			assertEquals(BeanQuerySyntaxException.Code.BAD_VALUE, e.code());
			assertEquals("Value 'abc' is not a valid numeric for column 'age'.", e.getMessage());
		}

		@Test void g16_localDateTimeInNot_typed() {
			assertEquals(java.time.LocalDateTime.parse("2026-06-01T05:00"), expr(typedSettings(), "createdAt=$not(2026-06-01T05:00)").args().get(0).typedValue());
		}

		@Test void g17_textColumnNotTyped() {
			assertEquals("abc", expr(settings(), "name=$or(abc,$contains(x))").args().get(0).typedValue());
		}

		@Test void g13_customOperator_typedOnlyWhenOptedIn() {
			var plain = SearchOperator.create("$near", "near").minArgs(1).maxArgs(1).types(NUMERIC).predicate((c, a) -> true).build();
			var typed = SearchOperator.create("$near", "near").minArgs(1).maxArgs(1).types(NUMERIC).predicate((c, a) -> true).typedArgs(true).build();
			for (var op : List.of(plain, typed)) {
				var s = settings();
				s.defaultOperators = s.defaultOperators.with(op);
				if (op.typedArgs())
					assertSyntax("Value 'x' is not a valid numeric for column 'age'.", s, new BeanQuery().setSearch("age=$near(x)"));
				else
					assertList(expr(s, "age=$near(x)").typedArgs(), "x");
			}
		}
	}

	@Nested class H_relativeDuration {

		private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneOffset.UTC);

		private static QuerySettings timestampSettings() {
			var s = settings();
			s.columns.put("createdAt", TIMESTAMP);
			s.columns.put("endedAt", TIMESTAMP);
			return s;
		}

		private static List<Filter.Leaf> leaves(String search, QuerySettings s, Clock clock) {
			var out = new ArrayList<Filter.Leaf>();
			collect(QueryResolver.resolve(new BeanQuery().setSearch(search), s, null, clock).filter(), out);
			return out;
		}

		private static void collect(Filter f, List<Filter.Leaf> out) {
			if (f instanceof Filter.Leaf l)
				out.add(l);
			else if (f instanceof Filter.And and)
				and.items().forEach(i -> collect(i, out));
			else if (f instanceof Filter.Or or)
				or.items().forEach(i -> collect(i, out));
		}

		@Test void h01_timestampColumn_resolvesAgainstTheSuppliedClock() {
			assertBean(leaves("createdAt=-24h", timestampSettings(), CLOCK).get(0).expression(), "typedValue", "2026-09-29T12:00Z");
		}

		@Test void h02_numericColumn_isPlainSignedMillis() {
			assertList(leaves("age=$gte(-24h)", settings(), CLOCK).get(0).expression().typedArgs(), new BigDecimal("-86400000"));
		}

		@Test void h03_twoLeavesInOneQuery_shareOneInstant() {
			var l = leaves("createdAt=-1h,endedAt=+1h", timestampSettings(), CLOCK);
			assertBean(l.get(0).expression(), "typedValue", "2026-09-30T11:00Z");
			assertBean(l.get(1).expression(), "typedValue", "2026-09-30T13:00Z");
		}

		@Test void h04_clockReadExactlyOnce_perResolve() {
			var reads = new int[] {0};
			var clock = new Clock() {
				@Override public ZoneId getZone() { return ZoneOffset.UTC; }
				@Override public Clock withZone(ZoneId zone) { return this; }
				@Override public Instant instant() { return Instant.parse("2026-09-30T12:00:00Z").plusSeconds(60L * reads[0]++); }
			};
			var l = leaves("createdAt=$between(-7d,-1d),endedAt=-1d", timestampSettings(), clock);
			assertEquals(1, reads[0]);
			var between = l.get(0).expression().typedArgs();
			assertEquals(OffsetDateTime.parse("2026-09-23T12:00:00Z"), between.get(0));
			assertEquals(OffsetDateTime.parse("2026-09-29T12:00:00Z"), between.get(1));
			assertEquals(OffsetDateTime.parse("2026-09-29T12:00:00Z"), l.get(1).expression().typedValue());
		}

		@Test void h05_quotedBuilderLiteral_resolves() {
			var q = BeanQuery.create().gte("createdAt", RelativeDuration.ofHours(-24)).build();
			var l = QueryResolver.resolve(q, timestampSettings(), null, CLOCK).filter();
			var leaf = new ArrayList<Filter.Leaf>();
			collect(l, leaf);
			assertList(leaf.get(0).expression().typedArgs(), OffsetDateTime.parse("2026-09-29T12:00:00Z"));
		}

		@Test void h06_defaultResolve_usesSystemClock() {
			var before = Instant.now().minusSeconds(5);
			var s = timestampSettings();
			var leaf = new ArrayList<Filter.Leaf>();
			collect(QueryResolver.resolve(new BeanQuery().setSearch("createdAt=+0ms"), s, null).filter(), leaf);
			var v = (OffsetDateTime)leaf.get(0).expression().typedValue();
			assertFalse(v.toInstant().isBefore(before) || v.toInstant().isAfter(Instant.now().plusSeconds(5)));
		}

		@Test void h07_relativeOnBooleanColumn_isBadValue() {
			var s = settings();
			s.columns.put("active", BOOLEAN);
			assertSyntax("Value '-24h' is not a valid boolean for column 'active'.", s, new BeanQuery().setSearch("active=-24h"));
		}

		@Test void h08_textColumn_relativeLiteralStaysText() {
			assertEquals("-24h", leaves("name=-24h", settings(), CLOCK).get(0).expression().typedValue());
		}

		@Test void h09_nullClock_rejected() {
			var hoistedArg1 = new BeanQuery();
			var hoistedArg2 = settings();
			assertThrows(IllegalArgumentException.class, () -> QueryResolver.resolve(hoistedArg1, hoistedArg2, null, null));
		}
	}

	@Test void f01_nullQueryRejected() {
		var hoistedArg3 = settings();
		assertThrows(IllegalArgumentException.class, () -> QueryResolver.resolve(null, hoistedArg3, null));
	}
}
