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

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // StubSession/StubContext test doubles are closeable but hold nothing to release (close() is a no-op), so tests do not close them
})
class BeanQueryPipeline_Test extends TestBase {

	//====================================================================================================
	// Minimal test engine: records which hook ran and with what ResolvedQuery.
	//====================================================================================================

	static final class StubContext extends BeanQueryContext<Object> {

		static final class Builder extends BeanQueryContext.Builder<Object,Builder> {
			Builder() {}

			Builder(StubContext copyFrom) {
				super(copyFrom);
			}

			@Override /* BeanQueryContext.Builder */
			public StubContext build() {
				return new StubContext(this);
			}
		}

		StubContext(Builder b) {
			super(b);
		}

		@Override /* BeanQueryContext */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* BeanQueryContext */
		public StubSession.Builder createSession() {
			return new StubSession.Builder(this);
		}
	}

	static final class StubSession extends BeanQuerySession<Object> {

		static final class Builder extends BeanQuerySession.Builder<Object,Builder> {
			Builder(StubContext context) {
				super(context);
			}

			@Override /* BeanQuerySession.Builder */
			public StubSession build() {
				return new StubSession(this);
			}
		}

		String hook;
		ResolvedQuery last;

		StubSession(Builder b) {
			super(b);
		}

		private void capture(String name, ResolvedQuery q) {
			hook = name;
			last = q;
		}

		@Override protected Page<Object> doFind(ResolvedQuery q) { capture("doFind", q); return page(List.of(), q, () -> 10, () -> 3); }
		@Override protected Page<Map<String,Object>> doFindValues(ResolvedQuery q) { capture("doFindValues", q); return page(List.of(), q, () -> 10, () -> 3); }
		@Override protected Counts doCount(ResolvedQuery q) { capture("doCount", q); return Counts.of(10, 3); }
		@Override protected Stream<Object> doStream(ResolvedQuery q) { capture("doStream", q); return Stream.empty(); }
		@Override protected Stream<Map<String,Object>> doStreamValues(ResolvedQuery q) { capture("doStreamValues", q); return Stream.empty(); }
		@Override public void close() { /* Nothing to release. */ }
	}

	/** name TEXT, age NUMERIC. */
	private static StubContext.Builder ctx() {
		return new StubContext.Builder().column("name", TEXT).column("age", NUMERIC);
	}

	private static StubSession session(StubContext.Builder b) {
		return b.build().createSession().build();
	}

	private static ResolvedQuery find(StubContext.Builder b, BeanQuery q) {
		try (var s = session(b)) {
			s.find(q);
			return s.last;
		}
	}

	private static void assertSyntax(String message, StubContext.Builder b, BeanQuery q) {
		try (var s = session(b)) {
			assertThrowsWithMessage(BeanQuerySyntaxException.class, message, () -> s.find(q));
		}
	}

	//====================================================================================================
	// §5 table, one row each
	//====================================================================================================

	@Nested class A_pipeline {

		@Test void a01_searchLength() {
			assertSyntax("Search string exceeds 10 characters.", ctx().maxSearchLength(10), new BeanQuery().setSearch("name=" + "x".repeat(20)));
		}

		@Test void a02_clauseCount() {
			assertSyntax("Too many search clauses (max 1).", ctx().maxSearchClauses(1), new BeanQuery().setSearch("name=a,age=1"));
		}

		@Test void a03_unknownColumn() {
			assertSyntax("Unknown column 'pasword' in search.", ctx(), new BeanQuery().setSearch("pasword=x"));
		}

		@Test void a04_depth() {
			assertSyntax("Search expression nested too deeply (max 2).", ctx().maxExpressionDepth(2), new BeanQuery().setSearch("age=$not($not($eq(1)))"));
		}

		@Test void a05_regex() {
			assertSyntax("Regex search is not enabled for this list.", ctx().allowRegex(false), new BeanQuery().setSearch("name=$regex(a)"));
			assertSyntax("Regex pattern exceeds 256 characters.", ctx(), new BeanQuery().setSearch("name=$regex(" + "a".repeat(257) + ")"));
		}

		@Test void a06_operatorType() {
			assertSyntax("Operator $gt does not apply to text column 'name'.", ctx(), new BeanQuery().setSearch("name=$gt(5)"));
		}

		@Test void a07_sort() {
			assertSyntax("Too many sort keys (max 1).", ctx().maxSortKeys(1), new BeanQuery().setSort("name,age"));
			assertSyntax("Unknown sort column 'x'.", ctx(), new BeanQuery().setSort("x"));
		}

		@Test void a08_view() {
			assertSyntax("Unknown view column 'x'.", ctx(), new BeanQuery().setView("x"));
			assertList(find(ctx(), new BeanQuery()).view(), "name", "age");
		}

		@Test void a09_paging() {
			assertSyntax("Position must not be negative.", ctx(), new BeanQuery().setPosition(-1));
			assertEquals(100, find(ctx(), new BeanQuery()).limit().intValue());
			assertEquals(1000, find(ctx(), new BeanQuery().setLimit(-1)).limit().intValue());
			assertEquals(1000, find(ctx(), new BeanQuery().setLimit(5000)).limit().intValue());
			assertNull(find(ctx().maxLimit(null), new BeanQuery().setLimit(-1)).limit());
		}

		@Test void a10_counts() {
			assertEquals(CountPolicy.NONE, find(ctx(), new BeanQuery()).counts());
			assertEquals(CountPolicy.MATCHED, find(ctx(), new BeanQuery().setOpts("counts=matched")).counts());
			assertEquals(CountPolicy.BOTH, find(ctx(), new BeanQuery().setOpts("counts=true")).counts());
			assertEquals(CountPolicy.NONE, find(ctx().countPolicy(CountPolicy.NONE), new BeanQuery().setOpts("counts=both")).counts());
		}
	}

	//====================================================================================================
	// Session methods, page helper, overrides, builders
	//====================================================================================================

	@Nested class B_session {

		@Test void b01_finalMethodsDispatch() {
			try (var s = session(ctx())) {
				var q = new BeanQuery();
				s.find(q);
				assertEquals("doFind", s.hook);
				s.findValues(q);
				assertEquals("doFindValues", s.hook);
				assertEquals("Counts(total=10, matched=3)", s.count(q).toString());
				assertEquals("doCount", s.hook);
				s.stream(q);
				assertEquals("doStream", s.hook);
				s.streamValues(q);
				assertEquals("doStreamValues", s.hook);
			}
		}

		@Test void b02_pageHelperFollowsPolicy() {
			try (var s = session(ctx())) {
				assertTrue(s.find(new BeanQuery()).matched().isEmpty());
				var m = s.find(new BeanQuery().setOpts("counts=matched"));
				assertTrue(m.matched().isPresent());
				assertTrue(m.total().isEmpty());
				assertEquals(3, m.matched().getAsLong());
				var b = s.find(new BeanQuery().setOpts("counts=both"));
				assertEquals(10, b.total().getAsLong());
				assertEquals(3, b.matched().getAsLong());
			}
		}

		@Test void b03_sessionOverrides() {
			var c = ctx().build();
			try (var s = c.createSession().defaultLimit(5).maxLimit(5).countPolicy(CountPolicy.BOTH).build()) {
				s.find(new BeanQuery().setLimit(50));
				assertEquals(5, s.last.limit().intValue());
				assertEquals(CountPolicy.BOTH, s.last.counts());
			}
			try (var s = c.createSession().maxSearchLength(3).maxSearchClauses(1).maxSortKeys(1).maxExpressionDepth(1).regexTimeout(Duration.ofSeconds(1)).build()) {
				var hoistedArg1 = new BeanQuery().setSearch("name=x");
				assertThrowsWithMessage(BeanQuerySyntaxException.class, "Search string exceeds 3 characters.", () -> s.find(hoistedArg1));
			}
			var hoistedTarget1 = c.createSession().defaultLimit(2000);
			assertThrowsWithMessage(IllegalStateException.class, "maxLimit (1000) must not be less than defaultLimit (2000).", () -> hoistedTarget1.build());
		}

		@Test void b04_restrictColumnsNarrowsOnly() {
			var c = ctx().build();
			try (var s = c.createSession().restrictColumns("age", "name").restrictColumns("age").build()) {
				assertList(s.columns(), "age");
				var hoistedArg2 = new BeanQuery().setSearch("name=x");
				assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown column 'name' in search.", () -> s.find(hoistedArg2));
			}
			var hoistedTarget2 = c.createSession();
			assertThrowsWithMessage(IllegalArgumentException.class, "Column 'x' is not a column of this context; a session can only narrow the columns.", () -> hoistedTarget2.restrictColumns("x"));
			var hoistedTarget3 = c.createSession().restrictColumns();
			assertThrowsWithMessage(IllegalStateException.class, "At least one column must be declared.", () -> hoistedTarget3.build());
			assertList(c.columns(), "name", "age");  // The context is unchanged.
		}

		@Test void b05_resolveOnContextAndSession() {
			var c = ctx().build();
			assertEquals("age", c.resolve(new BeanQuery().setSort("age")).sort().get(0).column());
			try (var s = session(ctx())) {
				assertInstanceOf(Filter.Leaf.class, s.resolve(new BeanQuery().setSearch("name=x")).filter());
			}
		}
	}

	@Nested class C_builders {

		@Test void c01_copyRoundTrips() {
			var c = ctx().allowRegex(false).defaultLimit(7).columnOperators("age", SearchOperatorSet.standard().without("$eq")).build();
			var d = c.copy().build();
			assertList(d.columns(), "name", "age");
			assertFalse(d.operatorSet().contains("$regex"));  // allowRegex(false) survives the copy.
			assertFalse(d.operatorSet("age").contains("$eq"));
			assertEquals(7, d.resolve(new BeanQuery()).limit().intValue());
			var e = c.copy().defaultLimit(9).build();  // Changing the copy leaves the original alone.
			assertEquals(7, c.resolve(new BeanQuery()).limit().intValue());
			assertEquals(9, e.resolve(new BeanQuery()).limit().intValue());
		}

		@Test void c02_buildRejectsMissingOrBadSettings() {
			var hoistedTarget4 = new StubContext.Builder();
			assertThrowsWithMessage(IllegalStateException.class, "At least one column must be declared.", () -> hoistedTarget4.build());
			// defaultLimit is explicit here so maxLimit(10) can't auto-narrow past it -- it's still a real conflict.
			var hoistedTarget5 = ctx().defaultLimit(50).maxLimit(10);
			assertThrowsWithMessage(IllegalStateException.class, "must not be less than defaultLimit", () -> hoistedTarget5.build());
			var hoistedTarget6 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "defaultLimit must be positive: 0", () -> hoistedTarget6.defaultLimit(0));
			var hoistedTarget7 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "maxLimit must be positive or null: -1", () -> hoistedTarget7.maxLimit(-1));
			var hoistedTarget8 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "maxSortKeys must be positive: 0", () -> hoistedTarget8.maxSortKeys(0));
			var hoistedTarget9 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "regexTimeout must be positive", () -> hoistedTarget9.regexTimeout(Duration.ZERO));
			var hoistedTarget10 = ctx();
			assertThrows(IllegalArgumentException.class, () -> hoistedTarget10.column(null, TEXT));
			var hoistedTarget11 = ctx();
			assertThrows(IllegalArgumentException.class, () -> hoistedTarget11.column("x", null));
			var hoistedTarget12 = ctx();
			assertThrows(IllegalArgumentException.class, () -> hoistedTarget12.operators(null));
			var hoistedTarget13 = ctx();
			assertThrows(IllegalArgumentException.class, () -> hoistedTarget13.countPolicy(null));
			var hoistedTarget14 = ctx();
			var hoistedArg3 = SearchOperatorSet.standard();
			assertThrowsWithMessage(IllegalArgumentException.class, "Column 'x' is not declared.", () -> hoistedTarget14.columnOperators("x", hoistedArg3));
		}

		@Test void c03_columnsAndExclude() {
			assertList(ctx().columns("age", "name").build().columns(), "age", "name");
			assertList(ctx().exclude("name").build().columns(), "age");
			var hoistedTarget15 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "Column 'x' is not declared.", () -> hoistedTarget15.columns("x"));
			var hoistedTarget16 = ctx();
			assertThrowsWithMessage(IllegalArgumentException.class, "Column 'x' is not declared.", () -> hoistedTarget16.exclude("x"));
			var c = ctx().columnOperators("name", SearchOperatorSet.standard().without("$eq")).exclude("name").column("name", TEXT).build();
			assertTrue(c.operatorSet("name").contains("$eq"));  // exclude(...) also dropped the column's operator override.
		}

		@Test void c04_operatorSetHidesRegexWhenDisallowed() {
			assertTrue(ctx().build().operatorSet().contains("$regex"));  // Regex is on by default.
			assertTrue(ctx().build().operatorSet("name").contains("$regex"));
			assertFalse(ctx().allowRegex(false).build().operatorSet().contains("$regex"));
			assertFalse(ctx().allowRegex(false).build().operatorSet("name").contains("$regex"));
		}
	}

	//====================================================================================================
	// getColumnType / getMaxSearchClauses / getMaxSortKeys / getContext
	//====================================================================================================

	@Nested class D_accessors {

		@Test void d01_contextColumnTypeExplicitAndUnknown() {
			var c = ctx().build();
			assertEquals(TEXT, c.getColumnType("name"));
			assertEquals(NUMERIC, c.getColumnType("age"));
			assertThrowsWithMessage(IllegalArgumentException.class, "Unknown column 'pasword'.", () -> c.getColumnType("pasword"));
			assertThrowsWithMessage(IllegalArgumentException.class, "Unknown column 'null'.", () -> c.getColumnType(null));
		}

		@Test void d02_contextCaps() {
			assertEquals(64, ctx().build().getMaxSearchClauses());
			assertEquals(8, ctx().build().getMaxSortKeys());
			assertEquals(1, ctx().maxSearchClauses(1).build().getMaxSearchClauses());
			assertEquals(2, ctx().maxSortKeys(2).build().getMaxSortKeys());
		}

		@Test void d03_sessionGetContextReturnsTheCreatingContext() {
			var c = ctx().build();
			try (var s = c.createSession().build()) {
				assertSame(c, s.getContext());
			}
		}

		@Test void d04_sessionCapsDefaultToContextAndReflectOverrides() {
			var c = ctx().maxSearchClauses(5).maxSortKeys(3).build();
			try (var s = c.createSession().build()) {
				assertBean(s, "maxSearchClauses,maxSortKeys", "5,3");
			}
			try (var s = c.createSession().maxSearchClauses(9).maxSortKeys(7).build()) {
				assertBean(s, "maxSearchClauses,maxSortKeys", "9,7");
				assertBean(c, "maxSearchClauses,maxSortKeys", "5,3");  // The context is unchanged.
			}
		}
	}
}
