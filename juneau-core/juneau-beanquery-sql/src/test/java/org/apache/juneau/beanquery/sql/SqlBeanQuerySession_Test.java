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
package org.apache.juneau.beanquery.sql;

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1172", // Fixtures: unused parameters are signature-only.
	"java:S1186" // Fixtures: empty constructors are signature-only.
})
class SqlBeanQuerySession_Test {

	private static final String URL = "jdbc:h2:mem:SqlBeanQuerySession_Test;DB_CLOSE_DELAY=-1";

	record Person(String name, int age) {}

	@BeforeAll
	static void createTable() throws SQLException {
		try (var c = DriverManager.getConnection(URL); var s = c.createStatement()) {
			s.execute("CREATE TABLE \"person\" (\"name\" VARCHAR(20), \"age\" INT, \"tenant\" VARCHAR(10))");
			s.execute("INSERT INTO \"person\" VALUES ('Alice', 30, 't1'), ('Bob', 25, 't1'), ('Carol', 40, 't2'), ('Dave', 17, 't1')");
			s.execute("CREATE TABLE \"nullable\" (\"name\" VARCHAR(20), \"age\" INT)");
			s.execute("INSERT INTO \"nullable\" VALUES ('Alice', 30), (NULL, NULL), ('Bob', 3)");
		}
	}

	private static Connection open() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	private final Recorder r = new Recorder();
	private final AtomicInteger acquired = new AtomicInteger();

	private SqlBeanQueryContext.Builder<Person> builder() {
		return SqlBeanQueryContext.create(Person.class)
			.dialect(TestDialect.INSTANCE)
			.table("person")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.column("tenant", TEXT)
			.rowMapper((rs, cols) -> new Person(rs.getString("name"), rs.getInt("age")))
			.connectionSupplier(() -> {
				acquired.incrementAndGet();
				return r.wrap(open());
			});
	}

	private static List<String> names(List<Person> rows) {
		return rows.stream().map(Person::name).toList();
	}

	/** Asserts the body fails with a {@link BeanQueryExecutionException} and that the session logged the failure once. */
	private static BeanQueryExecutionException assertFailsLogged(org.junit.jupiter.api.function.Executable body) {
		var thrown = new BeanQueryExecutionException[1];
		var records = LogRecordCapture.quietly(SqlBeanQuerySession.class, () -> thrown[0] = assertThrows(BeanQueryExecutionException.class, body));
		assertEquals(1, records.size(), records::toString);
		assertTrue(records.get(0).getMessage().startsWith("Bean query failed: "), records.get(0).getMessage());
		return thrown[0];
	}

	@Test
	void s01_findSortsAndPages() {
		try (var s = builder().build().getSession()) {
			assertEquals(List.of("Bob", "Alice"), names(s.find(new BeanQuery().setSort("age").setPosition(1).setLimit(2)).rows()));
		}
	}

	@Test
	void s02_searchIsBound() {
		try (var s = builder().build().getSession()) {
			assertEquals(List.of("Alice", "Carol"), names(s.find(new BeanQuery().setSearch("age=$gt(26)").setSort("name")).rows()));
			assertEquals(List.of("Carol"), names(s.find(new BeanQuery().setSearch("name=car")).rows()));
		}
	}

	@Test
	void s03_counts() {
		try (var s = builder().build().getSession()) {
			var q = new BeanQuery().setSearch("age=$gt(26)");
			assertTrue(s.find(q).matched().isEmpty());
			var p = s.find(new BeanQuery().setSearch("age=$gt(26)").setOpts("counts=both"));
			assertBean(p, "total,matched", "4,2");
			assertEquals("Counts(total=4, matched=2)", s.count(q).toString());
		}
	}

	@Test
	void s04_contextAndSessionGuardsAreAnded() {
		var c = builder().guard(SqlFragment.of("\"tenant\" = ?", "t1")).build();
		try (var s = c.createSession().guard(SqlFragment.of("\"age\" >= ?", 18)).build()) {
			var p = s.find(new BeanQuery().setSort("name").setOpts("counts=both"));
			// Carol: other tenant.  Dave: under 18.
			assertBean(p, "rows{#{name}},total", "{[{Alice},{Bob}]},2");
		}
	}

	@Test
	void s05_findValuesInViewOrder() {
		try (var s = builder().build().getSession()) {
			var row = s.findValues(new BeanQuery().setSearch("name=$eq(Bob)").setView("age,name")).rows().get(0);
			assertEquals(List.of("age", "name"), new ArrayList<>(row.keySet()));
			assertEquals(25, row.get("age"));
		}
	}

	@Test
	void s06_releasesTheConnectionItAcquired() {
		var s = builder().build().getSession();
		s.find(new BeanQuery());
		assertEquals(1, acquired.get());
		assertTrue(r.events("conn.close").isEmpty());
		s.close();
		assertEquals(List.of("conn.close"), r.events("conn.close"));
		s.close();  // Idempotent.
		assertEquals(1, r.events("conn.close").size());
	}

	@Test
	void s07_callerOwnedConnectionIsNeverClosed() throws SQLException {
		try (var conn = r.wrap(open())) {
			try (var s = builder().build().createSession().connection(conn).build()) {
				s.find(new BeanQuery());
			}
			assertEquals(0, acquired.get());
			assertFalse(conn.isClosed());
			assertTrue(r.events("conn.close").isEmpty());
		}
	}

	@Test
	void s08_streamIsLazyAndClosingItReleasesTheCursor() {
		try (var s = builder().build().getSession()) {
			try (Stream<Person> st = s.stream(new BeanQuery().setSort("age"))) {
				var it = st.iterator();
				assertEquals("Dave", it.next().name());
				assertTrue(r.events("rs.close").isEmpty());  // Still open: rows are read on demand.
			}
			assertEquals(List.of("rs.close", "ps.close"), r.events.stream().filter(e -> e.endsWith(".close")).toList());
		}
	}

	@Test
	void s09_sessionCloseClosesOpenStreamsFirst() {
		var s = builder().build().getSession();
		var st = s.streamValues(new BeanQuery());
		st.iterator().next();
		s.close();
		assertEquals(List.of("rs.close", "ps.close", "conn.close"), r.events.stream().filter(e -> e.endsWith(".close")).toList());
	}

	@Test
	void s10_fetchSizeAndQueryTimeoutApplied() {
		try (var s = builder().fetchSize(50).queryTimeout(Duration.ofMillis(1500)).build().getSession()) {
			s.find(new BeanQuery().setOpts("counts=both"));  // One row statement, two count statements.
		}
		assertMapped(r, Recorder::events, "fetchSize,queryTimeout",
			"[fetchSize 50,fetchSize 50,fetchSize 50],[queryTimeout 2,queryTimeout 2,queryTimeout 2]");
		try (var s = builder().build().getSession()) {
			s.find(new BeanQuery());
		}
		assertEquals("fetchSize 500", r.events("fetchSize").get(3));  // The default.
		assertEquals(3, r.events("queryTimeout").size());  // No timeout unless configured.
	}

	@Test
	void s11_rejectedQueriesNeverReachTheDatabase() {
		try (var s = builder().build().getSession()) {
			var hoistedArg1 = new BeanQuery().setSearch("password=x");
			assertThrows(BeanQuerySyntaxException.class, () -> s.find(hoistedArg1));
			var hoistedArg2 = new BeanQuery().setView("password");
			assertThrows(BeanQuerySyntaxException.class, () -> s.find(hoistedArg2));
		}
		assertTrue(r.events("prepare").isEmpty());
	}

	@Test
	void s12_sqlFailureIsGenericExecutionError() {
		try (var s = builder().table("missing").build().getSession()) {
			var hoistedArg3 = new BeanQuery();
			var e = assertFailsLogged(() -> s.find(hoistedArg3));
			assertEquals("Query execution failed for table 'missing'.", e.getMessage());
			assertInstanceOf(SQLException.class, e.getCause());
		}
	}

	@Test
	void s13_rowMapperNeededOnlyForBeans() {
		// Object has no default row mapper (a record such as Person would default to SqlRowMapper.bean).
		var c = SqlBeanQueryContext.create(Object.class).dialect(TestDialect.INSTANCE).table("person")
			.column("name", TEXT).connectionSupplier(SqlBeanQuerySession_Test::open).build();
		assertSame(Object.class, c.getType());
		try (var s = c.getSession()) {
			assertEquals(4, s.findValues(new BeanQuery()).rows().size());
			var hoistedArg4 = new BeanQuery();
			var e = assertThrows(IllegalStateException.class, () -> s.find(hoistedArg4));
			assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream.", e.getMessage());
		}
	}

	@Test
	void s14_closedSessionRejectsQueries() {
		var s = builder().build().getSession();
		s.close();
		var prepared = r.events("prepare").size();
		var hoistedArg5 = new BeanQuery();
		var e = assertThrows(IllegalStateException.class, () -> s.find(hoistedArg5));
		assertEquals("SqlBeanQuerySession is closed.", e.getMessage());
		var hoistedArg6 = new BeanQuery();
		assertThrows(IllegalStateException.class, () -> s.streamValues(hoistedArg6));
		var hoistedArg7 = new BeanQuery();
		assertThrows(IllegalStateException.class, () -> s.count(hoistedArg7));
		assertEquals(prepared, r.events("prepare").size());  // Nothing reached the connection after close().
	}

	@Test
	void s15_getContextIsCovariantAndReturnsTheCreatingContext() {
		var c = builder().build();
		try (var s = c.getSession()) {
			SqlBeanQueryContext<Person> back = s.getContext();  // Covariant return type; no cast needed.
			assertSame(c, back);
		}
	}

	@Test
	void s16_sessionCapsDefaultToContextAndReflectOverrides() {
		var c = builder().maxSearchClauses(5).maxSortKeys(3).build();
		try (var s = c.getSession()) {
			assertBean(s, "maxSearchClauses,maxSortKeys", "5,3");
		}
		try (var s = c.createSession().maxSearchClauses(9).maxSortKeys(7).build()) {
			assertBean(s, "maxSearchClauses,maxSortKeys", "9,7");
		}
		assertBean(c, "maxSearchClauses,maxSortKeys", "5,3");  // The context is unchanged.
	}

	@Test
	void s17_releaseFailureIsExecutionError() {
		var s = builder().build().getSession();
		r.failCloses = true;
		var e = assertThrows(BeanQueryExecutionException.class, s::close);
		assertEquals("Failed to release a database connection.", e.getMessage());
		assertInstanceOf(SQLException.class, e.getCause());
	}

	@Test
	void s18_cursorCloseFailureIsSwallowed() {
		try (var s = builder().build().getSession()) {
			var st = s.streamValues(new BeanQuery());
			st.iterator().next();
			r.failCloses = true;
			assertDoesNotThrow(st::close);  // closeQuietly: a close failure must not mask the caller's result.
			r.failCloses = false;
		}
	}

	private SqlBeanQueryContext<Person> throwingRowMapperContext(RuntimeException toThrow) {
		return SqlBeanQueryContext.create(Person.class)
			.dialect(TestDialect.INSTANCE)
			.table("person")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.rowMapper((rs, cols) -> { throw toThrow; })
			.connectionSupplier(() -> r.wrap(open()))
			.build();
	}

	@Test
	void s19_rowMapperFailureInFindIsExecutionError() {
		try (var s = throwingRowMapperContext(new RuntimeException("boom")).getSession()) {
			var hoistedArg8 = new BeanQuery();
			var e = assertFailsLogged(() -> s.find(hoistedArg8));
			assertBean(e, "message,cause{message}", "Query execution failed for table 'person'.,{boom}");
		}
	}

	@Test
	void s20_rowMapperFailureInStreamClosesTheCursor() {
		try (var s = throwingRowMapperContext(new RuntimeException("boom")).getSession()) {
			var hoistedTarget1 = s.stream(new BeanQuery()).iterator();
			var e = assertFailsLogged(hoistedTarget1::next);
			assertBean(e, "message,cause{message}", "Query execution failed for table 'person'.,{boom}");
			assertEquals(List.of("rs.close", "ps.close"), r.events.stream().filter(x -> x.endsWith(".close")).toList());  // The cursor closed itself; the session's own connection is still open.
		}
	}

	@Test
	void s21_rowMapperIllegalStateExceptionIsStillWrapped() {
		// IllegalStateException from the rowMapper itself is a query-execution failure like any other RuntimeException
		// it threw; it must be wrapped, consistent with Cursor.tryAdvance(). Only a bare, closed-session
		// IllegalStateException (from assertOpen(), checked before the try) passes through unwrapped - see s14.
		try (var s = throwingRowMapperContext(new IllegalStateException("boom")).getSession()) {
			var hoistedArg9 = new BeanQuery();
			var e = assertFailsLogged(() -> s.find(hoistedArg9));
			assertBean(e, "message,cause{message}", "Query execution failed for table 'person'.,{boom}");
			assertInstanceOf(IllegalStateException.class, e.getCause());
		}
	}

	@Test
	void s22_callerPipelineExceptionDuringStreamPropagatesUnwrapped() {
		// action.accept() in Cursor.tryAdvance() runs outside the try: an exception thrown by the caller's own
		// stream-pipeline code (e.g. a downstream Collectors.toMap duplicate-key check) is not ours to catch, and
		// must come back exactly as thrown - not wrapped as a fake SQL/row-mapping failure.
		try (var s = builder().build().getSession()) {
			var mine = new IllegalStateException("mine");
			try (var st = s.stream(new BeanQuery())) {
				var e = assertThrows(IllegalStateException.class, () -> st.forEach(p -> { throw mine; }));
				assertSame(mine, e);
			}
		}
	}

	@Test
	void s23_crossColumnOr() {
		try (var s = builder().build().getSession()) {
			// Alice by name, Carol by age — two different columns combined with $or, rendered as one SQL OR.
			var q = new BeanQuery().setSearch("$or(name=$eq(Alice),age=$gt(35))").setSort("name").setOpts("counts=both");
			var p = s.find(q);
			assertBean(p, "rows{#{name}},matched", "{[{Alice},{Carol}]},2");
		}
	}

	@Test
	void s24_crossColumnNot() {
		try (var s = builder().build().getSession()) {
			var q = new BeanQuery().setSearch("$not(age=$gt(26))").setSort("name").setOpts("counts=both");
			var p = s.find(q);
			assertBean(p, "rows{#{name}},matched", "{[{Bob},{Dave}]},2");
		}
	}

	@Test
	void s25_crossColumnOrGuardStillApplies() {
		// Same guard-before-filter check as j03, on the SQL engine: the guard and the compiled Filter are ANDed,
		// both parenthesized (design §8), so an $or across columns cannot escape the guard.
		var c = builder().guard(SqlFragment.of("\"tenant\" = ?", "t2")).build();
		try (var s = c.getSession()) {
			var q = new BeanQuery().setSearch("$or(name=$eq(Alice),age=$gt(35))");  // Matches Alice and Carol by filter.
			assertEquals(List.of("Carol"), names(s.find(q).rows()));  // Only Carol is tenant t2.
		}
	}

	@Test
	void s26_noSearchNoGuardStillOmitsWhereClause() {
		// Guards against the appendWhere regression this task must not introduce: an empty top-level Filter.And
		// (no search) must not render a spurious "WHERE (1=1)".
		try (var s = builder().build().getSession()) {
			assertEquals(4, s.find(new BeanQuery()).rows().size());
		}
	}

	@Test
	void s27_regexSearch_appliesDefaultTimeoutWhenNoneConfigured() {
		try (var s = builder().build().getSession()) {
			s.find(new BeanQuery().setSearch("name=$regex(^A)"));
		}
		assertEquals(List.of("queryTimeout 5"), r.events("queryTimeout"));  // DEFAULT_REGEX_TIMEOUT (5s), nothing configured.
	}

	@Test
	void s28_regexSearch_explicitQueryTimeoutWins() {
		try (var s = builder().queryTimeout(Duration.ofMillis(1500)).build().getSession()) {
			s.find(new BeanQuery().setSearch("name=$regex(^A)"));
		}
		assertEquals(List.of("queryTimeout 2"), r.events("queryTimeout"));  // The explicit setting, not the regex default.
	}

	@Test
	void s29_queryTimeoutOverflowIsClampedNotThrown() {
		try (var s = builder().queryTimeout(Duration.ofSeconds(Long.MAX_VALUE)).build().getSession()) {
			s.find(new BeanQuery());  // Must not throw ArithmeticException from Duration.toMillis() overflowing, nor
			// trip a driver's own internal seconds*1000 overflow (confirmed real in H2 2.3.232's setQueryTimeout(int)).
		}
		// Clamp bound is Integer.MAX_VALUE / 1000, not Integer.MAX_VALUE: some JDBC drivers multiply the seconds
		// value by 1000 internally in 32-bit int math, so clamping at the full Integer.MAX_VALUE would overflow
		// that downstream multiplication. Integer.MAX_VALUE / 1000 (~24.8 days) is still effectively "no timeout".
		assertEquals(List.of("queryTimeout " + (Integer.MAX_VALUE / 1000)), r.events("queryTimeout"));
	}

	@Test
	void s30_queryTimeoutRoundsUpToTheNextWholeSecond() {
		try (var s = builder().queryTimeout(Duration.ofMillis(1)).build().getSession()) {
			s.find(new BeanQuery());
		}
		assertEquals(List.of("queryTimeout 1"), r.events("queryTimeout"));  // Unchanged from today's rounding.
	}

	@Test
	void s31_queryTimeoutSecondsPlusOneDoesNotOverflowAtLongMaxSeconds() {
		try (var s = builder().queryTimeout(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999)).build().getSession()) {
			s.find(new BeanQuery());  // getSeconds()==Long.MAX_VALUE and getNano()>0: the "+ 1" must not wrap to
			// Long.MIN_VALUE, which would otherwise survive Math.max(1, ...) as a silent 1-second timeout.
		}
		assertList(r.events("queryTimeout"), "queryTimeout " + (Integer.MAX_VALUE / 1000));
	}

	@Test
	void s32_streamAfterSessionCloseThrowsInsteadOfSilentlyEnding() {
		var s = builder().build().getSession();
		var st = s.streamValues(new BeanQuery());
		var it = st.iterator();
		it.next();  // Pulls one row; the cursor is now open with more rows still unread (4 total, see s09).
		s.close();  // Force-closes the still-open cursor, same as s09.
		assertThrows(IllegalStateException.class, it::hasNext);  // Must not look like the stream simply ran out.
	}

	@Test
	void s33_streamThatRunsOutNaturallyStillJustEnds() {
		try (var s = builder().build().getSession()) {
			var st = s.streamValues(new BeanQuery().setSearch("name=$eq(Bob)"));  // Exactly one matching row.
			var it = st.iterator();
			it.next();
			assertFalse(it.hasNext());  // Natural EOF: no exception, unchanged from today.
		}
	}

	@Test
	void s34_streamAfterSessionCloseThrowsConsistentlyOnRepeatedCalls() {
		var s = builder().build().getSession();
		var st = s.streamValues(new BeanQuery());
		var it = st.iterator();
		it.next();
		s.close();
		assertThrows(IllegalStateException.class, it::hasNext);
		assertThrows(IllegalStateException.class, it::hasNext);  // Still throws, not just once.
	}

	@Test
	void s35_renderRowsRejectsAResolvedQueryFromAnotherContext() {
		var ctxA = builder().build();
		var ctxB = builder().build();
		var query = ctxA.resolve(new BeanQuery());
		var e = assertThrows(IllegalArgumentException.class, () -> ctxB.renderRows(query));
		assertTrue(e.getMessage().contains("This ResolvedQuery was not produced by this context"));
	}

	@Test
	void s36_renderCountRejectsAResolvedQueryFromAnotherContext() {
		var ctxA = builder().build();
		var ctxB = builder().build();
		var query = ctxA.resolve(new BeanQuery());
		var e = assertThrows(IllegalArgumentException.class, () -> ctxB.renderCount(query, true));
		assertTrue(e.getMessage().contains("This ResolvedQuery was not produced by this context"));
	}

	@Test
	void s37_renderRowsAcceptsAResolvedQueryFromASessionBuiltOnThisContext() {
		var ctx = builder().build();
		try (var s = ctx.getSession()) {
			var query = s.resolve(new BeanQuery());  // Resolved through the session, not the context directly.
			assertDoesNotThrow(() -> ctx.renderRows(query));  // The session's owning context is this context.
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Default row mappers, end to end
	//-----------------------------------------------------------------------------------------------------------------

	static class NotPlannablePerson {
		NotPlannablePerson(String name) { /* Intentionally empty test fixture. */ }
	}

	public static class Human {
		private String fullName;
		private int age;
		public Human() { /* Intentionally empty test fixture. */ }
		public void setFullName(String fullName) { this.fullName = fullName; }
		public void setAge(int age) { this.age = age; }
		public String getFullName() { return fullName; }
		public int getAge() { return age; }
	}

	private <X> SqlBeanQueryContext.Builder<X> defaultBuilder(Class<X> type) {
		return SqlBeanQueryContext.create(type)
			.dialect(TestDialect.INSTANCE)
			.table("person")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.connectionSupplier(() -> r.wrap(open()));
	}

	@Test
	void t01_findWithoutRowMapperDefaultsToBeanMapper() {
		try (var s = defaultBuilder(Person.class).build().getSession()) {
			var p = s.find(new BeanQuery().setSort("age").setPosition(1).setLimit(2));
			assertBean(p, "rows{#{name,age}}", "{[{Bob,25},{Alice,30}]}");
		}
	}

	@Test
	void t02_streamWithoutRowMapperDefaultsToBeanMapper() {
		try (var s = defaultBuilder(Person.class).build().getSession(); var rows = s.stream(new BeanQuery().setSearch("age=$gt(26)").setSort("name"))) {
			assertEquals(List.of("Alice", "Carol"), rows.map(Person::name).toList());
		}
	}

	@Test
	void t03_mapTypeDefaultsToMapMapperInViewOrder() {
		try (var s = defaultBuilder(Map.class).build().getSession()) {
			var rows = s.find(new BeanQuery().setSearch("name=$eq(Alice)").setView("age,name")).rows();
			assertEquals(1, rows.size());
			assertList(rows.get(0).keySet(), "age", "name");
			assertBean(rows.get(0), "age,name", "30,Alice");
		}
	}

	@Test
	void t04_renamedColumnMatchesPropertyByLogicalName() {
		try (var s = SqlBeanQueryContext.create(Human.class).dialect(TestDialect.INSTANCE).table("person")
				.column("fullName", TEXT).column("age", NUMERIC).columnSql("fullName", "name")
				.connectionSupplier(() -> r.wrap(open())).build().getSession()) {
			var p = s.find(new BeanQuery().setSort("age").setLimit(2));
			assertBean(p, "rows{#{fullName,age}}", "{[{Dave,17},{Bob,25}]}");
		}
	}

	@Test
	void t05_unplannableTypeThrowsAtFindTimeWithKeptReason() {
		try (var s = defaultBuilder(NotPlannablePerson.class).build().getSession()) {
			var hoistedArg10 = new BeanQuery();
			var e = assertThrows(IllegalStateException.class, () -> s.find(hoistedArg10));
			assertEquals("SqlBeanQueryContext requires a rowMapper for find and stream: cannot map rows to 'NotPlannablePerson': no public no-arg constructor.", e.getMessage());
			assertEquals(4, s.findValues(new BeanQuery()).rows().size());  // Values never need a mapper.
		}
	}

	enum Color { RED }

	record BadName(Color name) {}  // Alice is not a Color constant.

	@Test
	void t06_rowMappingFailureHasColumnAndNoSql() {
		try (var s = defaultBuilder(BadName.class).build().getSession()) {
			var hoistedArg11 = new BeanQuery();
			var e = assertThrows(BeanQueryExecutionException.class, () -> s.find(hoistedArg11));
			assertEquals("Failed to map column 'name' to BadName.", e.getMessage());
			assertFalse(e.getMessage().contains("SELECT"));
		}
	}

	//====================================================================================================
	// NOT over a NULL cell: two-valued (in-memory) semantics
	//====================================================================================================

	private SqlBeanQueryContext<Person> nullableContext() {
		return SqlBeanQueryContext.create(Person.class)
			.dialect(TestDialect.INSTANCE)
			.table("nullable")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.rowMapper((rs, cols) -> new Person(rs.getString("name"), rs.getInt("age")))
			.connectionSupplier(SqlBeanQuerySession_Test::open)
			.build();
	}

	@Test
	void n01_columnLevelNot_matchesNullCell() {
		try (var s = nullableContext().getSession()) {
			// In-memory: $gt(5) is false for a null cell, so $not($gt(5)) is true for it.
			assertEquals(Arrays.asList(null, "Bob"), names(s.find(new BeanQuery().setSearch("age=$not($gt(5))").setSort("age")).rows()));
			assertEquals(Arrays.asList(null, "Bob"), names(s.find(new BeanQuery().setSearch("name=$not($eq(Alice))").setSort("age")).rows()));
		}
	}

	@Test
	void n02_topLevelFilterNot_matchesNullCell() throws SQLException {
		var ctx = nullableContext();
		var op = SearchOperatorSet.standard();
		var leaf = new Filter.Leaf("name", SearchExpressionParser.parse("$eq(Alice)", op), SearchType.TEXT);
		var where = SqlSearchCompiler.create(TestDialect.INSTANCE, ctx).compile(new Filter.Not(leaf));
		var names = new ArrayList<String>();
		try (var c = open(); var ps = c.prepareStatement("SELECT \"name\", \"age\" FROM \"nullable\" WHERE " + where.sql() + " ORDER BY \"age\"")) {
			ps.setObject(1, where.binds().get(0));
			try (var rs = ps.executeQuery()) {
				while (rs.next())
					names.add(rs.getString(1));
			}
		}
		assertEquals(Arrays.asList(null, "Bob"), names);
	}
}
