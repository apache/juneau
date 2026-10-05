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
package org.apache.juneau.rest.server.datatables.adapter;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.DataTablesRequest.Column;
import org.apache.juneau.rest.server.datatables.DataTablesRequest.Order;
import org.apache.juneau.rest.server.datatables.DataTablesRequest.Search;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link DataTablesQuery} adapter: {@code toBeanQuery} mapping (D3&ndash;D6), {@code run}/{@code
 * runArrays} response population, and that a malformed query propagates {@link BeanQuerySyntaxException} uncaught
 * (central HTTP&nbsp;400 + {@code X-BeanQuery-Error} mapping; 2026-10-05 override of the original 200+{@code error} design D2).
 */
class DataTablesQuery_Test extends TestBase {

	/** {@code name}&rarr;TEXT, {@code age}&rarr;NUMERIC, {@code active}&rarr;BOOLEAN; declared in this order. */
	public static class Rec {
		private final String name;
		private final int age;
		private final boolean active;

		Rec(String name, int age, boolean active) {
			this.name = name;
			this.age = age;
			this.active = active;
		}

		public String getName() { return name; }
		public int getAge() { return age; }
		public boolean isActive() { return active; }

		@Override public String toString() { return name; }
	}

	private static final List<Rec> ROWS = List.of(
		new Rec("Alice", 30, true),
		new Rec("Bob", 25, false),
		new Rec("Carol", 40, true),
		new Rec("Dave", 17, false)
	);

	private static InMemoryBeanQueryContext<Rec> context() {
		return InMemoryBeanQueryContext.create(Rec.class).columns("name", "age", "active").build();
	}

	/** A two-column (name, age) descriptor block, both searchable and orderable, with no search terms. */
	private static List<Column> cols() {
		return new ArrayList<>(List.of(new Column().setData("name"), new Column().setData("age")));
	}

	/** A three-column (name, age, active) descriptor block, for the global-search type-exclusion tests. */
	private static List<Column> colsWithActive() {
		return new ArrayList<>(List.of(new Column().setData("name"), new Column().setData("age"), new Column().setData("active")));
	}

	private static Search search(String value) {
		return new Search().setValue(value);
	}

	private static List<String> names(DataTablesResults<Rec> r) {
		return r.getData().stream().map(Rec::getName).toList();
	}

	private static DataTablesResults<Rec> run(DataTablesRequest req) {
		try (var s = context().getSession(ROWS)) {
			return DataTablesQuery.run(req, s);
		}
	}

	private static BeanQuery toBeanQuery(DataTablesRequest req) {
		return DataTablesQuery.toBeanQuery(req, context());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Envelope shape (via run)
	//------------------------------------------------------------------------------------------------------------------

	@Nested class A_envelope {

		@Test void a01_drawEchoedAndCounts() {
			var r = run(new DataTablesRequest().setDraw(7).setStart(0).setLength(10).setColumns(cols()));
			assertBean(r, "draw,recordsTotal,recordsFiltered,data{length}", "7,4,4,{4}");
		}

		@Test void a02_errorOmittedWhenNull() {
			assertNull(run(new DataTablesRequest().setColumns(cols())).getError());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Per-column search (via run; AND across columns)
	//------------------------------------------------------------------------------------------------------------------

	@Nested class B_search {

		@Test void b01_perColumnSearch() {
			var columns = cols();
			columns.get(0).setSearch(search("$eq(Alice)"));
			var r = run(new DataTablesRequest().setLength(10).setColumns(columns));
			assertList(names(r), "Alice");
			assertBean(r, "recordsTotal,recordsFiltered", "4,1");
		}

		@Test void b02_columnsAreAnded() {
			var columns = cols();
			columns.get(0).setSearch(search("$prefix(Car)"));
			columns.get(1).setSearch(search("$gt(26)"));
			assertList(names(run(new DataTablesRequest().setLength(10).setColumns(columns))), "Carol");
		}

		@Test void b03_nonSearchableColumnIgnored() {
			var columns = cols();
			columns.get(0).setSearchable(false).setSearch(search("$eq(Alice)"));
			assertSize(4, run(new DataTablesRequest().setLength(10).setColumns(columns)).getData());
		}

		@Test void b04_blankTermIgnored() {
			var columns = cols();
			columns.get(0).setSearch(search("   "));
			assertSize(4, run(new DataTablesRequest().setLength(10).setColumns(columns)).getData());
		}

		@Test void b05_ribbonOnlyColumnFilterStillFilters() {
			// a ribbon filter carried in the POST body as that column's own search.value is just an
			// ordinary per-column search as far as DataTablesQuery is concerned.
			var columns = cols();
			columns.get(1).setSearch(search("$eq(25)"));
			assertList(names(run(new DataTablesRequest().setLength(10).setColumns(columns))), "Bob");
		}

		@Test void b06_ribbonFilterAndedWithUserSearchOnSameColumn() {
			var columns = cols();
			columns.get(0).setSearch(search("$and($prefix(Ca),$contains(ol))"));
			assertList(names(run(new DataTablesRequest().setLength(10).setColumns(columns))), "Carol");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Global search (D4, via toBeanQuery's built wire string)
	//------------------------------------------------------------------------------------------------------------------

	@Nested class C_globalSearch {

		@Test void c01_singleEligibleColumnUnwrapped() {
			var req = new DataTablesRequest().setColumns(cols()).setSearch(search("ali"));
			req.getColumns().get(1).setSearchable(false);  // Only "name" is left eligible.
			assertEquals("name=$contains(\"ali\")", toBeanQuery(req).getSearch());
		}

		@Test void c02_textAndParsingNumericBothEligible_ored() {
			// "42" is TEXT-eligible unconditionally and parses as NUMERIC; "active" (BOOLEAN) does not parse it.
			var req = new DataTablesRequest().setColumns(colsWithActive()).setSearch(search("42"));
			assertEquals("$or(name=$contains(\"42\"),age=$eq(\"42\"))", toBeanQuery(req).getSearch());
		}

		@Test void c03_numericColumnAloneMatchesFromGlobalBox_endToEnd() {
			var columns = colsWithActive();
			columns.get(0).setSearchable(false);  // name excluded.
			columns.get(2).setSearchable(false);  // active excluded.
			var req = new DataTablesRequest().setLength(10).setColumns(columns).setSearch(search("25"));
			assertList(names(run(req)), "Bob");  // Bob is the only row with age == 25.
		}

		@Test void c04_booleanColumnMatchesFromGlobalBoxWhenTermParses() {
			var columns = colsWithActive();
			columns.get(0).setSearchable(false);  // name excluded.
			columns.get(1).setSearchable(false);  // age excluded.
			var req = new DataTablesRequest().setLength(10).setColumns(columns).setSearch(search("true"));
			assertList(names(run(req)), "Alice", "Carol");
		}

		/** {@code status} auto-infers {@link SearchType#ENUM} from the Java enum type. */
		public enum Status { OPEN, CLOSED }

		public static class StatusRec {
			private final String name;
			private final Status status;

			StatusRec(String name, Status status) {
				this.name = name;
				this.status = status;
			}

			public String getName() { return name; }
			public Status getStatus() { return status; }
		}

		@Test void c05_enumColumnMatchesViaEqic() {
			var ctx = InMemoryBeanQueryContext.create(StatusRec.class).build();
			var rows = List.of(new StatusRec("a", Status.OPEN), new StatusRec("b", Status.CLOSED));
			var columns = List.of(new Column().setData("name").setSearchable(false), new Column().setData("status"));
			var req = new DataTablesRequest().setLength(10).setColumns(columns).setSearch(search("open"));
			assertEquals("status=$eqic(\"open\")", DataTablesQuery.toBeanQuery(req, ctx).getSearch());
			try (var s = ctx.getSession(rows)) {
				assertList(DataTablesQuery.run(req, s).getData().stream().map(StatusRec::getName).toList(), "a");
			}
		}

		@Test void c06_numericBooleanExcludedWhenTermDoesNotParseForThatType() {
			var columns = colsWithActive();
			columns.get(0).setSearchable(false);  // name excluded: not searchable.
			var req = new DataTablesRequest().setColumns(columns).setSearch(search("x"));
			// "x" parses as neither NUMERIC nor BOOLEAN -> nothing eligible -> no search string at all.
			assertNull(toBeanQuery(req).getSearch());
		}

		@Test void c07_noneEligibleAddsNoSearch() {
			var columns = cols();
			columns.forEach(c -> c.setSearchable(false));
			assertNull(toBeanQuery(new DataTablesRequest().setColumns(columns).setSearch(search("x"))).getSearch());
		}

		@Test void c08_regexFlagIgnored() {
			var req = new DataTablesRequest().setColumns(cols()).setSearch(new Search().setValue("ali").setRegex(true));
			req.getColumns().get(1).setSearchable(false);
			assertEquals("name=$contains(\"ali\")", toBeanQuery(req).getSearch());
		}

		@Test void c09_valueWithCommaQuoteDollarStaysLiteral() {
			var req = new DataTablesRequest().setColumns(cols()).setSearch(search("a,\"b\",$c"));
			req.getColumns().get(1).setSearchable(false);
			var r = run(req);  // Must not throw; no row's name literally contains that text.
			assertSize(0, r.getData());
		}

		@Test void c10_globalPlusPerColumnMergeOnSameKey() {
			var columns = cols();
			columns.get(0).setSearch(search("$prefix(A)"));
			columns.get(1).setSearchable(false);
			var req = new DataTablesRequest().setColumns(columns).setSearch(search("li"));
			assertEquals("name=$and($prefix(A),$contains(\"li\"))", toBeanQuery(req).getSearch());
		}

		@Test void c13_globalPlusPerColumnOnSameKeyRunsWithoutDuplicateColumnError() {
			var columns = cols();
			columns.get(0).setSearch(search("$prefix(A)"));
			columns.get(1).setSearchable(false);
			var r = run(new DataTablesRequest().setLength(10).setColumns(columns).setSearch(search("li")));
			assertBean(r, "error", "<null>");
			assertList(names(r), "Alice");
		}

		/** Carry-over 2: Java's {@code Double.parseDouble} accepts these, the engine's {@code BigDecimal} does not. */
		@Test void c11_javaOnlyDoubleFormsAreNotNumeric() {
			for (var term : List.of("1d", "1f", "NaN", "Infinity", "-Infinity", "0x1p3")) {
				var columns = colsWithActive();
				columns.get(0).setSearchable(false);  // name excluded.
				columns.get(2).setSearchable(false);  // active excluded -> only the NUMERIC column is left.
				var req = new DataTablesRequest().setLength(10).setColumns(columns).setSearch(search(term));
				assertNull(toBeanQuery(req).getSearch(), term);
				var r = run(req);
				assertBean(r, "error,data{length}", "<null>,{4}");  // 200 with the unfiltered page, never an exception.
			}
		}

		@Test void c12_plainNumericFormsStillEligible() {
			for (var term : List.of("42", "-3.5", "1e3", " 7 ")) {
				var columns = colsWithActive();
				columns.get(0).setSearchable(false);
				columns.get(2).setSearchable(false);
				var req = new DataTablesRequest().setColumns(columns).setSearch(search(term));
				assertNotNull(toBeanQuery(req).getSearch(), term);
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sort / page
	//------------------------------------------------------------------------------------------------------------------

	@Nested class D_sortPage {

		private DataTablesRequest order(int col, String dir, int start, int length) {
			return new DataTablesRequest()
				.setStart(start)
				.setLength(length)
				.setColumns(cols())
				.setOrder(new ArrayList<>(List.of(new Order().setColumn(col).setDir(dir))));
		}

		@Test void d01_sortAsc() {
			assertList(names(run(order(1, "asc", 0, -1))), "Dave", "Bob", "Alice", "Carol");
		}

		@Test void d02_sortDescCaseInsensitive() {
			assertList(names(run(order(1, "DESC", 0, -1))), "Carol", "Alice", "Bob", "Dave");
		}

		@Test void d03_outOfRangeColumnSkipped() {
			assertList(names(run(order(5, "asc", 0, -1))), "Alice", "Bob", "Carol", "Dave");
		}

		@Test void d04_nonOrderableColumnSkipped() {
			var columns = cols();
			columns.get(1).setOrderable(false);
			var req = new DataTablesRequest().setLength(-1).setColumns(columns)
				.setOrder(new ArrayList<>(List.of(new Order().setColumn(1).setDir("asc"))));
			assertList(names(run(req)), "Alice", "Bob", "Carol", "Dave");
		}

		@Test void d05_blankDataColumnSkipped() {
			var columns = cols();
			columns.get(1).setData("");
			var req = new DataTablesRequest().setLength(-1).setColumns(columns)
				.setOrder(new ArrayList<>(List.of(new Order().setColumn(1).setDir("asc"))));
			assertList(names(run(req)), "Alice", "Bob", "Carol", "Dave");
		}

		@Test void d06_duplicateSortKeysKeepFirstOccurrence() {
			var req = new DataTablesRequest().setLength(-1).setColumns(cols()).setOrder(new ArrayList<>(List.of(
				new Order().setColumn(1).setDir("asc"),
				new Order().setColumn(1).setDir("desc"))));
			assertEquals("age", toBeanQuery(req).getSort());
		}

		@Test void d07_paging() {
			assertList(names(run(order(1, "asc", 1, 2))), "Bob", "Alice");
		}

		@Test void d08_lengthMinusOneReturnsAll() {
			assertSize(4, run(order(1, "asc", 0, -1)).getData());
		}

		@Test void d09_negativeStartClampedToZero() {
			var req = new DataTablesRequest().setStart(-5).setLength(10).setColumns(cols());
			assertEquals(0, toBeanQuery(req).getPosition().intValue());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// toBeanQuery mapping (direct wire-string assertions)
	//------------------------------------------------------------------------------------------------------------------

	@Nested class E_mapping {

		@Test void e01_declaredNameUsedAsIs() {
			var columns = cols();
			columns.get(0).setSearch(search("$eq(Alice)"));
			assertEquals("name=$eq(Alice)", toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e02_allDigitsResolvesToNthDeclaredColumn() {
			var columns = List.of(new Column().setData("1").setSearch(search("$gt(20)")));
			assertEquals("age=$gt(20)", toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e03_outOfRangeDigitErrorsOnAUsedColumn() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("9").setSearch(search("$eq(1)"))));
			var e = assertThrows(BeanQuerySyntaxException.class, () -> toBeanQuery(req));
			assertBean(e, "message,code", "Unknown column '9'.,UNKNOWN_COLUMN");
		}

		@Test void e04_nullOrBlankDataSkippedEvenWhenSearchableAndNonBlankSearch() {
			var columns = List.of(
				new Column().setData(null).setSearch(search("$eq(1)")),
				new Column().setData("  ").setSearch(search("$eq(2)")));
			assertNull(toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e05_nameSetButDataBlankStillSkipped() {
			var columns = List.of(new Column().setName("Age").setData("").setSearch(search("$eq(1)")));
			assertNull(toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e06_dottedPathOnUsedColumnErrors() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("author.name").setSearch(search("$eq(x)"))));
			var e = assertThrows(BeanQuerySyntaxException.class, () -> toBeanQuery(req));
			assertEquals("Unknown column 'author.name'.", e.getMessage());
		}

		@Test void e16_hugeAllDigitsDataOnUsedColumnIsUnknownColumn() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("99999999999").setSearch(search("$eq(1)"))));
			var e = assertThrows(BeanQuerySyntaxException.class, () -> toBeanQuery(req));
			assertBean(e, "message,code", "Unknown column '99999999999'.,UNKNOWN_COLUMN");
			var e2 = assertThrows(BeanQuerySyntaxException.class, () -> run(req));
			assertEquals("Unknown column '99999999999'.", e2.getMessage());
		}

		@Test void e17_hugeAllDigitsDataIgnoredByGlobalSearch() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("99999999999"))).setSearch(search("x"));
			assertNull(toBeanQuery(req).getSearch());
			assertBean(run(req), "error", "<null>");
		}

		@Test void e18_nonAsciiDigitsAreNotAnIndex() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("\u0661").setSearch(search("$eq(1)"))));
			var e = assertThrows(BeanQuerySyntaxException.class, () -> toBeanQuery(req));
			assertEquals("UNKNOWN_COLUMN", e.code().name());
		}

		@Test void e07_unknownNameOnAnUnusedColumnIsNotAnError() {
			var columns = List.of(new Column().setData("author.name"));  // No search term, no order entry -> unused.
			assertDoesNotThrow(() -> toBeanQuery(new DataTablesRequest().setColumns(columns)));
		}

		@Test void e08_leadingTrailingWhitespaceTrimmed() {
			var columns = List.of(new Column().setData("  name  ").setSearch(search("$eq(Bob)")));
			assertEquals("name=$eq(Bob)", toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e09_sameKeyFromTwoColumnsMergedWithAnd() {
			var columns = List.of(
				new Column().setData("name").setSearch(search("$prefix(A)")),
				new Column().setData("0").setSearch(search("$suffix(e)")));
			assertEquals("name=$and($prefix(A),$suffix(e))", toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e10_rawExpressionPassedThroughUnquoted() {
			var columns = List.of(new Column().setData("age").setSearch(search("$between(20,35)")));
			assertEquals("age=$between(20,35)", toBeanQuery(new DataTablesRequest().setColumns(columns)).getSearch());
		}

		@Test void e11_countsAlwaysBoth() {
			assertEquals("counts=both", toBeanQuery(new DataTablesRequest().setColumns(cols())).getOpts());
		}

		@Test void e12_tooManyColumnsCapped() {
			var ctx = context();
			var columns = new ArrayList<Column>();
			for (var i = 0; i <= ctx.getMaxSearchClauses(); i++)
				columns.add(new Column().setData("name"));
			var req = new DataTablesRequest().setColumns(columns);
			var e = assertThrows(BeanQuerySyntaxException.class, () -> DataTablesQuery.toBeanQuery(req, ctx));
			assertBean(e, "message,code", "Too many columns (max " + ctx.getMaxSearchClauses() + ").,TOO_MANY_CLAUSES");
		}

		@Test void e13_tooManySortKeysCapped() {
			var ctx = context();
			var order = new ArrayList<Order>();
			for (var i = 0; i <= ctx.getMaxSortKeys(); i++)
				order.add(new Order().setColumn(0).setDir("asc"));
			var req = new DataTablesRequest().setColumns(cols()).setOrder(order);
			var e = assertThrows(BeanQuerySyntaxException.class, () -> DataTablesQuery.toBeanQuery(req, ctx));
			assertBean(e, "message,code", "Too many sort keys (max " + ctx.getMaxSortKeys() + ").,TOO_MANY_SORT_KEYS");
		}

		@Test void e14_exactlyAtCapAccepted() {
			var ctx = context();
			var columns = new ArrayList<Column>();
			for (var i = 0; i < ctx.getMaxSearchClauses(); i++)
				columns.add(new Column().setData("name"));
			assertDoesNotThrow(() -> DataTablesQuery.toBeanQuery(new DataTablesRequest().setColumns(columns), ctx));
		}

		@Test void e15_capsCheckedBeforeResolutionReportsCapNotTheUnknownColumn() {
			var ctx = context();
			var columns = new ArrayList<Column>();
			for (var i = 0; i <= ctx.getMaxSearchClauses(); i++)
				columns.add(new Column().setData("nope" + i).setSearch(search("$eq(1)")));
			var req = new DataTablesRequest().setColumns(columns);
			var e = assertThrows(BeanQuerySyntaxException.class, () -> DataTablesQuery.toBeanQuery(req, ctx));
			assertEquals("Too many columns (max " + ctx.getMaxSearchClauses() + ").", e.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Errors (2026-10-05): a syntax error propagates from run/runArrays as BeanQuerySyntaxException (mapped to HTTP 400 centrally).
	//------------------------------------------------------------------------------------------------------------------

	@Nested class F_errors {

		@Test void f01_malformedExpressionPropagatesBeanQuerySyntaxException() {
			var columns = cols();
			columns.get(0).setSearch(search("$noSuchOp(1)"));
			var req = new DataTablesRequest().setLength(10).setColumns(columns);
			assertThrows(BeanQuerySyntaxException.class, () -> run(req));
		}

		@Test void f02_unknownColumnPropagatesBeanQuerySyntaxException() {
			var columns = List.of(new Column().setData("nope").setSearch(search("$eq(1)")));
			var req = new DataTablesRequest().setDraw(3).setColumns(columns);
			var e = assertThrows(BeanQuerySyntaxException.class, () -> run(req));
			assertBean(e, "message,code", "Unknown column 'nope'.,UNKNOWN_COLUMN");
		}

		@Test void f03_tooManyColumnsPropagatesBeanQuerySyntaxException() {
			try (var s = context().getSession(ROWS)) {
				var ctx = s.getContext();
				var columns = new ArrayList<Column>();
				for (var i = 0; i <= ctx.getMaxSearchClauses(); i++)
					columns.add(new Column().setData("name"));
				var req = new DataTablesRequest().setColumns(columns);
				var e = assertThrows(BeanQuerySyntaxException.class, () -> DataTablesQuery.run(req, s));
				assertEquals("Too many columns (max " + ctx.getMaxSearchClauses() + ").", e.getMessage());
			}
		}

		/** A bean whose {@code getAge()} throws - exercises the pipeline's "failed to read column" wrapping into
		 * {@code BeanQueryExecutionException}, which must propagate (HTTP 500), like the syntax error above. */
		public static class BoomRec {
			public String getName() { return "Zoe"; }
			public int getAge() { throw new RuntimeException("boom"); }
		}

		@Test void f04_executionExceptionPropagates() {
			var ctx = InMemoryBeanQueryContext.create(BoomRec.class).build();
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("age").setSearch(search("$gt(0)"))));
			try (var s = ctx.getSession(List.of(new BoomRec()))) {
				assertThrows(BeanQueryExecutionException.class, () -> DataTablesQuery.run(req, s));
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// runArrays: array rows in context.columns() order
	//------------------------------------------------------------------------------------------------------------------

	@Nested class G_runArrays {

		@Test void g01_rowsAreListsInDeclaredColumnOrder() {
			var req = new DataTablesRequest().setLength(10).setColumns(
				List.of(new Column().setData("0"), new Column().setData("1"), new Column().setData("2")));
			try (var s = context().getSession(ROWS)) {
				var r = DataTablesQuery.runArrays(req, s);
				assertList(context().columns(), "name", "age", "active");
				assertList(r.getData().get(0), "Alice", 30, true);
			}
		}

		@Test void g02_dataIntegerSortsByThatCell() {
			var req = new DataTablesRequest().setLength(-1)
				.setColumns(List.of(new Column().setData("0"), new Column().setData("1")))
				.setOrder(new ArrayList<>(List.of(new Order().setColumn(1).setDir("asc"))));
			try (var s = context().getSession(ROWS)) {
				var r = DataTablesQuery.runArrays(req, s);
				assertList(r.getData().stream().map(row -> row.get(0)).toList(), "Dave", "Bob", "Alice", "Carol");
			}
		}

		@Test void g03_drawEchoedAndCountsPopulated() {
			var req = new DataTablesRequest().setDraw(4).setLength(10)
				.setColumns(List.of(new Column().setData("0"), new Column().setData("1")));
			try (var s = context().getSession(ROWS)) {
				assertBean(DataTablesQuery.runArrays(req, s), "draw,recordsTotal,recordsFiltered", "4,4,4");
			}
		}

		@Test void g04_errorPathMatchesRun() {
			var req = new DataTablesRequest().setColumns(List.of(new Column().setData("9").setSearch(search("$eq(1)"))));
			try (var s = context().getSession(ROWS)) {
				var e = assertThrows(BeanQuerySyntaxException.class, () -> DataTablesQuery.runArrays(req, s));
				assertEquals("Unknown column '9'.", e.getMessage());
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Counts (D6): BOTH / MATCHED / NONE policy interaction
	//------------------------------------------------------------------------------------------------------------------

	@Nested class H_counts {

		@Test void h01_bothPresentByDefault() {
			assertBean(run(new DataTablesRequest().setLength(10).setColumns(cols())), "recordsTotal,recordsFiltered", "4,4");
		}

		@Test void h02_matchedOnlyPolicyFillsBothFieldsFromMatched() {
			var ctx = InMemoryBeanQueryContext.create(Rec.class).columns("name", "age", "active").countPolicy(CountPolicy.MATCHED).build();
			var columns = cols();
			columns.get(0).setSearch(search("$eq(Alice)"));
			var req = new DataTablesRequest().setLength(10).setColumns(columns);
			try (var s = ctx.getSession(ROWS)) {
				assertBean(DataTablesQuery.run(req, s), "recordsTotal,recordsFiltered", "1,1");
			}
		}

		@Test void h03_nonePolicyLeavesBothAtZero() {
			var ctx = InMemoryBeanQueryContext.create(Rec.class).columns("name", "age", "active").countPolicy(CountPolicy.NONE).build();
			var req = new DataTablesRequest().setLength(10).setColumns(cols());
			try (var s = ctx.getSession(ROWS)) {
				assertBean(DataTablesQuery.run(req, s), "recordsTotal,recordsFiltered", "0,0");
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// JSON input: the request bean deserializes from the DataTables JSON shape.
	//------------------------------------------------------------------------------------------------------------------

	@Nested class I_jsonInput {

		@Test void i01_deserializesFromJson() throws Exception {
			var json = "{draw:5,start:0,length:10,columns:[{data:'name',searchable:true,orderable:true,search:{value:'$eq(Bob)'}},{data:'age'}]}";
			var req = Json5.to(json, DataTablesRequest.class);
			assertEquals(5, req.getDraw());
			var r = run(req);
			assertList(names(r), "Bob");
			assertBean(r, "draw,recordsFiltered", "5,1");
		}
	}
}
