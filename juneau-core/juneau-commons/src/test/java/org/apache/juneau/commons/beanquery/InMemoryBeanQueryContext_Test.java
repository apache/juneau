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

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

class InMemoryBeanQueryContext_Test extends TestBase {

	public static class Rec {
		private final String name;
		private final int age;
		private final String version;

		Rec(String name, int age, String version) {
			this.name = name;
			this.age = age;
			this.version = version;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
		public String getVersion() { return version; }

		@Override public String toString() { return name; }
	}

	private static final List<Rec> ROWS = List.of(
		new Rec("Alice", 30, "10.0.1"),
		new Rec("Bob", 25, "10.1.0"),
		new Rec("Carol", 40, "9.5.0"),
		new Rec("Dave", 17, "10.0.0")
	);

	/** Bean-derived columns age (NUMERIC), name (TEXT), version (overridden to VERSION). */
	private static InMemoryBeanQueryContext.Builder<Rec> builder() {
		return InMemoryBeanQueryContext.create(Rec.class).column("version", SearchType.VERSION);
	}

	private static InMemoryBeanQueryContext<Rec> context() {
		return builder().build();
	}

	private static List<String> names(List<Rec> rows) {
		return rows.stream().map(Rec::getName).toList();
	}

	//====================================================================================================
	// Search (ported)
	//====================================================================================================

	@Test
	void a01_eqicText_caseInsensitive() {
		// $eq is exact (case-sensitive); $eqic is the case-insensitive equals operator.
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().op("name", "$eqic", "alice").build();
			assertList(names(s.find(q).rows()), "Alice");
		}
	}

	@Test
	void a01b_eqText_isExact() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().eq("name", "alice").build();
			assertList(names(s.find(q).rows()));  // No case-exact match -> empty.
			var q2 = BeanQuery.create().eq("name", "Alice").build();
			assertList(names(s.find(q2).rows()), "Alice");
		}
	}

	@Test
	void a02_gtNumeric() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().gt("age", "26").build();
			assertList(names(s.find(q).rows()), "Alice", "Carol");
		}
	}

	@Test
	void a03_versionPrefix() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().prefix("version", "10.0").build();
			assertList(names(s.find(q).rows()), "Alice", "Dave");
		}
	}

	@Test
	void a04_columnsAreAnded() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().gt("age", "26").prefix("name", "Car").build();
			assertList(names(s.find(q).rows()), "Carol");
		}
	}

	@Test
	void a05_orWithinColumn() {
		try (var s = context().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("name=$or($eq(Bob),$eq(Dave))");
			assertList(names(s.find(q).rows()), "Bob", "Dave");
		}
	}

	//====================================================================================================
	// Sort / page / view (ported)
	//====================================================================================================

	@Test
	void b01_sortAsc() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().sort("age").build();
			assertList(names(s.find(q).rows()), "Dave", "Bob", "Alice", "Carol");
		}
	}

	@Test
	void b02_sortDesc() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().sortDesc("age").build();
			assertList(names(s.find(q).rows()), "Carol", "Alice", "Bob", "Dave");
		}
	}

	@Test
	void b03_page() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().sort("age").page(1, 2).build();
			assertList(names(s.find(q).rows()), "Bob", "Alice");
		}
	}

	@Test
	void b04_viewValuesOrderedKeys() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().eq("name", "Bob").view("name", "age").build();
			var row = s.findValues(q).rows().get(0);
			// assertMap alone isn't enough here: it compares contents only (sorting both sides
			// alphabetically), so it can't catch an order regression - this test's whole point, per its
			// name, is that the view's requested order ("name","age") is preserved in row.keySet(), not
			// just that the right entries are present. Same pattern as SqlBeanQuerySession_Test.s05.
			assertList(new ArrayList<>(row.keySet()), "name", "age");
			assertMap(row, "age=25", "name=Bob");
		}
	}

	//====================================================================================================
	// Counts / guard (ported)
	//====================================================================================================

	@Test
	void c01_countsOptIn() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().gt("age", "26").counts(CountRequest.BOTH).build();
			var page = s.find(q);
			assertBean(page, "total,matched", "OptionalLong[4],OptionalLong[2]");
		}
	}

	@Test
	void c02_noCounts_hasCountsFalse() {
		try (var s = context().getSession(ROWS)) {
			var page = s.find(new BeanQuery());
			assertBean(page, "total,matched", "OptionalLong.empty,OptionalLong.empty");
		}
	}

	@Test
	void c03_countMethod() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().gt("age", "26").build();
			var c = s.count(q);
			assertBean(c, "total,matched", "4,2");
		}
	}

	@Test
	void c04_guardScopesTotal() {
		var ctx = builder().guard(r -> r.getAge() >= 18).countPolicy(CountPolicy.BOTH).build();
		try (var s = ctx.getSession(ROWS)) {
			var page = s.find(new BeanQuery());
			assertBean(page, "total,matched", "OptionalLong[3],OptionalLong[3]");  // Dave (17) is out of scope.
			assertFalse(names(page.rows()).contains("Dave"));
		}
	}

	//====================================================================================================
	// Custom operators / errors / streaming (ported)
	//====================================================================================================

	@Test
	void d01_customOperatorPredicate() {
		var even = SearchOperator.create("$even", "Matches even numbers. Example: $even()")
			.minArgs(0).maxArgs(0).types(SearchType.NUMERIC)
			.predicate((cell, args) -> cell instanceof Number n && n.intValue() % 2 == 0).build();
		var ctx = builder().operators(SearchOperatorSet.standard().with(even)).build();
		try (var s = ctx.getSession(ROWS)) {
			var q = new BeanQuery().setSearch("age=$even()");
			assertList(names(s.find(q).rows()), "Alice", "Carol");  // 30, 40
		}
	}

	@Test
	void d02_syntaxExceptionPropagates() {
		try (var s = context().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("name=$nope(x)");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown search operator", () -> s.find(q));
		}
	}

	@Test
	void d02b_syntaxExceptionMessageUsesSingleQuotes() {
		// Exact-message check: the operator name must be wrapped in a single pair of single-quotes, not a doubled
		// pair (printf-style StringFormat treats '' as two literal apostrophes, unlike java.text.MessageFormat).
		try (var s = context().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("name=$nope(x)");
			var e = assertThrows(BeanQuerySyntaxException.class, () -> s.find(q));
			assertEquals("Unknown search operator: '$nope'", e.getMessage());
		}
	}

	@Test
	void d03_stream() {
		try (var s = context().getSession(ROWS)) {
			var q = BeanQuery.create().sort("age").page(0, 2).build();
			assertList(s.stream(q).map(Rec::getName).toList(), "Dave", "Bob");
			assertList(s.streamValues(q.setView("name")).map(m -> m.get("name")).toList(), "Dave", "Bob");
		}
	}

	@Test
	void d04_blankExpressionIgnored() {
		try (var s = context().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("name=");
			assertEquals(4, s.find(q).rows().size());
		}
	}

	@Test
	void d05_mapRows() {
		List<Map<String,Object>> rows = List.of(
			new LinkedHashMap<>(Map.of("k", "x")),
			new LinkedHashMap<>(Map.of("k", "y"))
		);
		var ctx = InMemoryBeanQueryContext.<Map<String,Object>>create().column("k", SearchType.TEXT).build();
		try (var s = ctx.getSession(rows)) {
			var q = BeanQuery.create().eq("k", "x").build();
			assertEquals(1, s.find(q).rows().size());
		}
	}

	//====================================================================================================
	// Column allow-list (new)
	//====================================================================================================

	@Test
	void e01_viewClassRejected() {
		try (var s = context().getSession(ROWS)) {
			var hoistedArg1 = new BeanQuery().setView("class");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown view column 'class'.", () -> s.findValues(hoistedArg1));
		}
	}

	@Test
	void e02_sortToStringRejected() {
		try (var s = context().getSession(ROWS)) {
			var hoistedArg2 = new BeanQuery().setSort("toString");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown sort column 'toString'.", () -> s.find(hoistedArg2));
		}
	}

	@Test
	void e03_bareMethodNameRejectedInSearch() {
		try (var s = context().getSession(ROWS)) {
			var hoistedArg3 = new BeanQuery().setSearch("toString=x");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown column 'toString' in search.", () -> s.find(hoistedArg3));
			var hoistedArg4 = new BeanQuery().setSearch("hashCode=1");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown column 'hashCode' in search.", () -> s.find(hoistedArg4));
		}
	}

	@Test
	void e04_columnsDerivedFromBean() {
		var ctx = InMemoryBeanQueryContext.create(Rec.class).build();
		assertList(ctx.columns(), "age", "name", "version");
		try (var s = ctx.getSession(ROWS)) {
			assertEquals(SearchType.NUMERIC, s.resolve(new BeanQuery().setSort("age")).sort().get(0).type());
		}
	}

	@Test
	void e05_excludeAndColumns() {
		assertList(builder().exclude("version").build().columns(), "age", "name");
		assertList(builder().columns("name", "age").build().columns(), "name", "age");
		var hoistedTarget1 = builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Column 'nope' is not declared.", () -> hoistedTarget1.exclude("nope"));
	}

	@Test
	void e06_derivedColumnNeedsAccessor() {
		var hoistedTarget2 = builder().column("initial", SearchType.TEXT);
		assertThrowsWithMessage(IllegalArgumentException.class, "Column 'initial' is not a readable bean property of", () -> hoistedTarget2.build());
		var ctx = builder().column("initial", SearchType.TEXT).accessor("initial", r -> r.getName().substring(0, 1)).build();
		try (var s = ctx.getSession(ROWS)) {
			assertList(names(s.find(new BeanQuery().setSearch("initial=$eq(C)")).rows()), "Carol");
		}
		var hoistedTarget3 = builder();
		assertThrowsWithMessage(IllegalArgumentException.class, "Column 'nope' is not declared.", () -> hoistedTarget3.accessor("nope", r -> null));
	}

	@Test
	void e07_defaultViewIsDeclaredOrder() {
		try (var s = context().getSession(ROWS)) {
			var row = s.findValues(new BeanQuery().setLimit(1)).rows().get(0);
			assertList(new ArrayList<>(row.keySet()), "age", "name", "version");
		}
	}

	@Test
	void e08_mapRowsRequireColumns() {
		var hoistedTarget4 = InMemoryBeanQueryContext.<Map<String,Object>>create();
		assertThrowsWithMessage(IllegalStateException.class, "At least one column must be declared.", () -> hoistedTarget4.build());
	}

	@Test
	void e09_copyRoundTrips() {
		var ctx = builder().guard(r -> r.getAge() >= 18).column("initial", SearchType.TEXT).accessor("initial", r -> r.getName().substring(0, 1)).build();
		var copy = ctx.copy().build();
		assertList(copy.columns(), "age", "name", "version", "initial");
		try (var s = copy.getSession(ROWS)) {
			assertTrue(s.find(new BeanQuery().setSearch("initial=$eq(D)")).rows().isEmpty());  // Guard kept: Dave is excluded.
		}
	}

	//====================================================================================================
	// Session guard / restriction (new)
	//====================================================================================================

	@Test
	void f01_guardsAreAnded() {
		var ctx = builder().guard(r -> r.getAge() >= 18).build();
		try (var s = ctx.createSession().rows(ROWS).guard(r -> r.getName().compareTo("B") > 0).build()) {
			var page = s.find(new BeanQuery().setOpts("counts=both"));
			assertList(names(page.rows()), "Bob", "Carol");  // Alice fails the session guard, Dave the context guard.
			assertEquals(2, page.total().getAsLong());
		}
	}

	@Test
	void f02_restrictColumns() {
		try (var s = context().createSession().rows(ROWS).restrictColumns("name").build()) {
			assertList(s.columns(), "name");
			var hoistedArg5 = new BeanQuery().setSearch("age=$gt(1)");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown column 'age' in search.", () -> s.find(hoistedArg5));
			var row = s.findValues(new BeanQuery().setLimit(1)).rows().get(0);
			assertList(new ArrayList<>(row.keySet()), "name");
		}
		var hoistedTarget5 = context().createSession();
		assertThrowsWithMessage(IllegalArgumentException.class, "is not a column of this context", () -> hoistedTarget5.restrictColumns("nope"));
	}

	@Test
	void f03_rowsRequired() {
		var hoistedTarget6 = context().createSession();
		assertThrowsWithMessage(IllegalStateException.class, "InMemoryBeanQuerySession requires rows.", () -> hoistedTarget6.build());
		var hoistedTarget7 = context();
		assertThrows(IllegalArgumentException.class, () -> hoistedTarget7.getSession(null));
	}

	@Test
	void f04_guardThrowIsExecutionError() {
		var session = context().createSession().rows(ROWS).guard(r -> { throw new IllegalStateException("boom"); });
		var e = assertThrows(BeanQueryExecutionException.class, session::build);
		assertEquals("Failed to apply guard.", e.getMessage());
		assertEquals("boom", e.getCause().getMessage());
	}

	//====================================================================================================
	// Regex / limits (new)
	//====================================================================================================

	@Test
	void g01_regexDisallowed() {
		var ctx = builder().allowRegex(false).build();
		assertFalse(ctx.operatorSet().contains("$regex"));
		try (var s = ctx.getSession(ROWS)) {
			var hoistedArg6 = new BeanQuery().setSearch("name=$regex(a.*)");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Regex search is not enabled for this list.", () -> s.find(hoistedArg6));
		}
	}

	@Test
	void g02_regexOnByDefault() {
		var ctx = context();
		assertTrue(ctx.operatorSet().contains("$regex"));
		try (var s = ctx.getSession(ROWS)) {
			assertList(names(s.find(new BeanQuery().setSearch("name=$regex(a.*)")).rows()), "Alice");
		}
	}

	@Test
	@Timeout(10)
	void g03_catastrophicRegexStopsAtDeadline() {
		// A backreference disables the JDK's group memoization, so (a+)+\1b backtracks exponentially on a run of a's.
		var ctx = builder().allowRegex(true).build();
		try (var s = ctx.getSession(List.of(new Rec("a".repeat(32), 1, "1.0")))) {
			var q = new BeanQuery().setSearch("name=$regex((a+)+\\1b)");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Regex search took too long.", () -> s.find(q));
		}
	}

	@Test
	void g04_limitClamped() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		try (var s = context().getSession(many)) {
			assertEquals(100, s.find(new BeanQuery()).rows().size());          // defaultLimit
			assertEquals(150, s.find(new BeanQuery().setLimit(-1)).rows().size());  // maxLimit 1000 > 150
		}
		try (var s = builder().maxLimit(120).build().getSession(many)) {
			assertEquals(120, s.find(new BeanQuery().setLimit(5000)).rows().size());
			assertEquals(120, s.find(new BeanQuery().setLimit(-1)).rows().size());
		}
		try (var s = context().createSession().rows(many).defaultLimit(10).build()) {
			assertEquals(10, s.find(new BeanQuery()).rows().size());
		}
	}

	@Test
	@Timeout(5)
	void g05_wildcardCatastrophicPatternIsLinearAndNoMatch() {
		// Previously built as a regex (a*a*a*...*b joined with Pattern.quote/".*"), this backtracks exponentially
		// against a long run of 'a's.  The glob matcher is linear, so this must complete well within the timeout.
		var row = new Rec("a".repeat(200), 1, "1.0");
		try (var s = context().getSession(List.of(row))) {
			var q = new BeanQuery().setSearch("name=a*a*a*a*a*a*a*a*a*a*a*a*b");
			assertTrue(s.find(q).rows().isEmpty());
		}
	}

	@Test
	void g06_wildcardGlobSemantics() {
		try (var s = context().getSession(ROWS)) {
			assertList(names(s.find(new BeanQuery().setSearch("name=Ali*")).rows()), "Alice");   // trailing *
			assertList(names(s.find(new BeanQuery().setSearch("name=*ice")).rows()), "Alice");   // leading *
			assertList(names(s.find(new BeanQuery().setSearch("name=*lic*")).rows()), "Alice");  // middle *
			assertList(names(s.find(new BeanQuery().setSearch("name=A**e")).rows()), "Alice");   // adjacent/empty segments
			assertList(names(s.find(new BeanQuery().setSearch("name=al*")).rows()), "Alice");    // case-insensitive
			assertList(names(s.find(new BeanQuery().setSearch("name=Alice")).rows()), "Alice");  // no star: contains-match unaffected
			assertTrue(s.find(new BeanQuery().setSearch("name=z*")).rows().isEmpty());           // no match
		}
	}

	//====================================================================================================
	// Count policy / execution errors / type check (new)
	//====================================================================================================

	@Test
	void h01_matchedPolicy() {
		try (var s = builder().countPolicy(CountPolicy.MATCHED).build().getSession(ROWS)) {
			var page = s.find(BeanQuery.create().gt("age", "26").build());
			assertBean(page, "total,matched", "OptionalLong.empty,OptionalLong[2]");
		}
	}

	@Test
	void h02_throwingAccessorIsExecutionError() {
		var ctx = builder().accessor("name", r -> { throw new IllegalStateException("boom"); }).build();
		try (var s = ctx.getSession(ROWS)) {
			var hoistedArg7 = new BeanQuery().setSearch("name=x");
			var e = assertThrows(BeanQueryExecutionException.class, () -> s.find(hoistedArg7));
			assertEquals("Failed to read column 'name'.", e.getMessage());
			assertEquals("boom", e.getCause().getMessage());
		}
	}

	@Test
	void h03_nonePolicyIgnoresOpts() {
		try (var s = builder().countPolicy(CountPolicy.NONE).build().getSession(ROWS)) {
			assertTrue(s.find(new BeanQuery().setOpts("counts=both")).matched().isEmpty());
		}
	}

	@Test
	void h04_operatorTypeChecked() {
		try (var s = context().getSession(ROWS)) {
			var hoistedArg8 = new BeanQuery().setSearch("name=$gt(5)");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Operator $gt does not apply to text column 'name'.", () -> s.find(hoistedArg8));
		}
	}

	//====================================================================================================
	// getColumnType / getMaxSearchClauses / getMaxSortKeys / getContext
	//====================================================================================================

	@Test
	void i01_columnTypeExplicitAndInferred() {
		var ctx = context();
		assertEquals(SearchType.VERSION, ctx.getColumnType("version"));  // Builder.column(name, type) override.
		assertEquals(SearchType.NUMERIC, ctx.getColumnType("age"));      // Inferred from the getter's return type.
		assertEquals(SearchType.TEXT, ctx.getColumnType("name"));
	}

	@Test
	void i02_columnTypeUnknownColumnThrows() {
		var hoistedTarget8 = context();
		assertThrowsWithMessage(IllegalArgumentException.class, "Unknown column 'nope'.", () -> hoistedTarget8.getColumnType("nope"));
	}

	@Test
	void i03_contextCaps() {
		assertEquals(64, context().getMaxSearchClauses());
		assertEquals(8, context().getMaxSortKeys());
		assertEquals(2, builder().maxSearchClauses(2).build().getMaxSearchClauses());
		assertEquals(3, builder().maxSortKeys(3).build().getMaxSortKeys());
	}

	@Test
	void i04_sessionGetContextIsCovariantAndReturnsTheCreatingContext() {
		var ctx = context();
		try (var s = ctx.getSession(ROWS)) {
			InMemoryBeanQueryContext<Rec> back = s.getContext();  // Covariant return type; no cast needed.
			assertSame(ctx, back);
		}
	}

	@Test
	void i05_sessionCapsDefaultToContextAndReflectOverrides() {
		var ctx = builder().maxSearchClauses(5).maxSortKeys(3).build();
		try (var s = ctx.getSession(ROWS)) {
			assertBean(s, "maxSearchClauses,maxSortKeys", "5,3");
		}
		try (var s = ctx.createSession().rows(ROWS).maxSearchClauses(9).maxSortKeys(7).build()) {
			assertBean(s, "maxSearchClauses,maxSortKeys", "9,7");
		}
		assertBean(ctx, "maxSearchClauses,maxSortKeys", "5,3");  // The context is unchanged.
	}

	//====================================================================================================
	// Cross-column $or/$not via Filter
	//====================================================================================================

	@Test
	void j01_crossColumnOr() {
		try (var s = context().getSession(ROWS)) {
			// Alice by name, Carol by age — two different columns combined with $or.
			var q = new BeanQuery().setSearch("$or(name=$eq(Alice),age=$gt(35))").setSort("name");
			assertList(names(s.find(q).rows()), "Alice", "Carol");
			assertEquals(2, s.count(q).matched());
		}
	}

	@Test
	void j02_crossColumnNot() {
		try (var s = context().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("$not(age=$gt(26))").setSort("name");
			assertList(names(s.find(q).rows()), "Bob", "Dave");
			assertEquals(2, s.count(q).matched());
		}
	}

	@Test
	void j03_crossColumnOrGuardStillApplies() {
		// The guard is tested before the Filter tree (design §8): an $or across columns cannot pull in a row the
		// guard already excluded.
		try (var s = builder().guard(r -> r.getName().startsWith("A") || r.getName().startsWith("B")).build().getSession(ROWS)) {
			var q = new BeanQuery().setSearch("$or(name=$eq(Carol),age=$gt(35))");
			assertTrue(s.find(q).rows().isEmpty());  // Carol/age>35 would match the filter, but neither passes the guard.
		}
	}

	//====================================================================================================
	// k - session maxLimit auto-narrows an inherited (non-explicit) defaultLimit, never throws
	//====================================================================================================

	@Test
	void k01_sessionMaxLimitBelowInheritedDefaultLimitAutoNarrows() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		var ctx = context();  // defaultLimit 100, maxLimit 1000 (both the library defaults).
		// Narrowing ONLY maxLimit, below the context's inherited defaultLimit, must not throw: defaultLimit was
		// never explicitly chosen for this session, so it auto-narrows alongside maxLimit.
		try (var s = ctx.createSession().rows(many).maxLimit(50).build()) {
			assertEquals(50, s.find(new BeanQuery()).rows().size());  // defaultLimit auto-narrowed to 50, not left at 100.
		}
	}

	@Test
	void k02_explicitSessionDefaultLimitAboveNarrowedMaxLimitStillThrows() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		var ctx = context();
		// An EXPLICIT, contradictory choice is still the caller's own error -- not auto-fixed.
		var contradictory = ctx.createSession().rows(many).defaultLimit(200).maxLimit(50);
		assertThrowsWithMessage(IllegalStateException.class, "maxLimit (50) must not be less than defaultLimit (200).", contradictory::build);
	}

	@Test
	void k03_sessionDefaultLimitExplicitlySetBeforeNarrowingMaxLimitIsUnaffected() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		var ctx = context();
		// Explicit defaultLimit, already consistent with the later maxLimit narrowing: no auto-narrowing applied.
		try (var s = ctx.createSession().rows(many).defaultLimit(30).maxLimit(50).build()) {
			assertEquals(30, s.find(new BeanQuery()).rows().size());
		}
	}

	//====================================================================================================
	// l - context maxLimit auto-narrows an inherited (non-explicit) defaultLimit, never throws
	//====================================================================================================

	@Test
	void l01_freshBuilderMaxLimitBelowDefaultBaselineAutoNarrows() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		// defaultLimit was never touched on this builder -- it is still QuerySettings' own baseline default (100).
		try (var s = builder().maxLimit(50).build().getSession(many)) {
			assertEquals(50, s.find(new BeanQuery()).rows().size());  // No exception; auto-narrowed, not left at 100.
		}
	}

	@Test
	void l02_copiedBuilderMaxLimitBelowInheritedDefaultLimitAutoNarrows() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		var base = context();  // defaultLimit 100 (never explicitly chosen on `base` either, but that's not the point
		                        // under test here -- what matters is the COPY never explicitly choosing it).
		// copy() is the documented way to derive a variant (see the class Javadoc example); narrowing only maxLimit
		// on the copy, without repeating defaultLimit, must not throw.
		try (var s = base.copy().maxLimit(50).build().getSession(many)) {
			assertEquals(50, s.find(new BeanQuery()).rows().size());
		}
	}

	@Test
	void l03_explicitContextDefaultLimitAboveNarrowedMaxLimitStillThrows() {
		// An EXPLICIT, contradictory choice is still the caller's own error -- not auto-fixed, same as k02.
		var contradictory = builder().defaultLimit(200).maxLimit(50);
		assertThrowsWithMessage(IllegalStateException.class, "maxLimit (50) must not be less than defaultLimit (200).", contradictory::build);
	}

	@Test
	void l04_contextDefaultLimitExplicitlySetBeforeNarrowingMaxLimitIsUnaffected() {
		var many = IntStream.range(0, 150).mapToObj(i -> new Rec("n" + i, i, "1.0")).toList();
		// Explicit defaultLimit, already consistent with the later maxLimit narrowing: no auto-narrowing applied.
		try (var s = builder().defaultLimit(30).maxLimit(50).build().getSession(many)) {
			assertEquals(30, s.find(new BeanQuery()).rows().size());
		}
	}

	//====================================================================================================
	// Juneau bean parity (phase 1)
	//====================================================================================================

	public static class Fielded {
		public String name;
		public int age;
		@BeanIgnore public String ssn;
		@BeanProp("when") public Instant created;

		Fielded(String name, int age, String ssn) {
			this.name = name;
			this.age = age;
			this.ssn = ssn;
			created = Instant.EPOCH;
		}
	}

	public record Pet(String name, int legs) {}

	@Test
	void p01_publicFieldsAreColumns() {
		var ctx = InMemoryBeanQueryContext.create(Fielded.class).build();
		assertList(ctx.columns(), "age", "name", "when");
		try (var s = ctx.getSession(List.of(new Fielded("Alice", 30, "1"), new Fielded("Bob", 25, "2")))) {
			assertList(s.find(new BeanQuery().setSearch("age=$gt(26)")).rows().stream().map(x -> x.name).toList(), "Alice");
			assertList(s.findValues(new BeanQuery().setView("when").setLimit(1)).rows().stream().map(r -> r.get("when")).toList(), Instant.EPOCH);
		}
	}

	@Test
	void p02_beanIgnoreIsNotAColumn() {
		var ctx = InMemoryBeanQueryContext.create(Fielded.class).build();
		try (var s = ctx.getSession(List.of(new Fielded("Alice", 30, "1")))) {
			var q = new BeanQuery().setSearch("ssn=1");
			assertThrowsWithMessage(BeanQuerySyntaxException.class, "Unknown column 'ssn'", () -> s.find(q));
		}
	}

	@Test
	void p03_recordComponentsAreColumns() {
		var ctx = InMemoryBeanQueryContext.create(Pet.class).build();
		assertList(ctx.columns(), "legs", "name");
		try (var s = ctx.getSession(List.of(new Pet("Rex", 4), new Pet("Polly", 2)))) {
			assertList(s.find(new BeanQuery().setSort("legs")).rows().stream().map(Pet::name).toList(), "Polly", "Rex");
		}
	}

	@Test
	void p04_beanConfigNamer() {
		var config = BeanConfigContext.create().propertyNamer(PropertyNamerDLC.INSTANCE).build();
		assertList(InMemoryBeanQueryContext.create(Pet.class, config).build().columns(), "legs", "name");
		var ctx = InMemoryBeanQueryContext.create(Rec.class, config).column("version", SearchType.VERSION).build();
		assertList(ctx.columns(), "age", "name", "version");
	}

	@Test
	void p05_createFromBeanMeta() {
		var ctx = InMemoryBeanQueryContext.create(BeanMeta.of(Pet.class)).build();
		assertList(ctx.columns(), "legs", "name");
	}

	@Test
	void p06_nullFactoryArgs() {
		assertThrows(IllegalArgumentException.class, () -> InMemoryBeanQueryContext.create((Class<Rec>)null));
		assertThrows(IllegalArgumentException.class, () -> InMemoryBeanQueryContext.create(Rec.class, null));
		assertThrows(IllegalArgumentException.class, () -> InMemoryBeanQueryContext.create((BeanMeta<Rec>)null));
	}

	public static class Opaque {
		final String label;

		Opaque(String label) { this.label = label; }
	}

	@Test
	void p07_nonBeanWithDerivedColumn() {
		var ctx = InMemoryBeanQueryContext.create(Opaque.class)
			.column("label", SearchType.TEXT)
			.accessor("label", o -> o.label)
			.build();
		assertList(ctx.columns(), "label");
		try (var s = ctx.getSession(List.of(new Opaque("a"), new Opaque("b")))) {
			assertList(s.findValues(new BeanQuery().setView("label").setSearch("label=b")).rows().stream().map(r -> r.get("label")).toList(), "b");
		}
	}
}
