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

import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end proof of relative-duration literals (design #4 D4, IRS parity gap 2): {@code -24h}/{@code +7d} binds
 * one request-time instant computed once per query - a {@link BigDecimal} signed-millisecond number on a NUMERIC
 * column, an {@link OffsetDateTime} on a TIMESTAMP column - and never emits SQL text such as {@code now() +/- interval}.
 *
 * <p>
 * The live-session tests build their fixture rows relative to the real clock, with wide margins either side of the
 * boundary under test, because {@code find} has no clock-injection seam; the fixed-clock seam is unit-tested in
 * {@code QueryResolver_Test}.
 */
class RelativeDurationLiteral_Test {

	private static final String URL = "jdbc:h2:mem:RelativeDurationLiteral_" + UUID.randomUUID().toString().replace('-', '_') + ";DB_CLOSE_DELAY=-1";

	@BeforeAll
	static void createTable() throws SQLException {
		var now = Instant.now();
		var recent = OffsetDateTime.ofInstant(now.minus(Duration.ofHours(22)), ZoneOffset.UTC);  // Inside -24h.
		var old = OffsetDateTime.ofInstant(now.minus(Duration.ofHours(26)), ZoneOffset.UTC);     // Outside -24h.
		try (var c = DriverManager.getConnection(URL); var s = c.createStatement()) {
			s.execute("CREATE TABLE \"event\" (\"id\" INT, \"happenedAt\" TIMESTAMP WITH TIME ZONE, \"elapsedMs\" BIGINT)");
		}
		try (var c = DriverManager.getConnection(URL); var ins = c.prepareStatement("INSERT INTO \"event\" VALUES (?, ?, ?)")) {
			ins.setInt(1, 1); ins.setObject(2, recent); ins.setLong(3, 90_000_000L); ins.executeUpdate();  // 25h in ms.
			ins.setInt(1, 2); ins.setObject(2, old); ins.setLong(3, 1_000L); ins.executeUpdate();
		}
	}

	private static Connection open() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	private static SqlBeanQueryContext.Builder<Integer> builder() {
		return SqlBeanQueryContext.create(Integer.class)
			.dialect(TestDialect.INSTANCE)
			.table("event")
			.column("id", NUMERIC)
			.column("happenedAt", TIMESTAMP)
			.column("elapsedMs", NUMERIC)
			.rowMapper((rs, cols) -> rs.getInt("id"))
			.connectionSupplier(RelativeDurationLiteral_Test::open);
	}

	private static SqlFragment compile(String search) {
		var ctx = builder().build();
		return SqlSearchCompiler.create(TestDialect.INSTANCE, ctx).compile(ctx.resolve(new BeanQuery().setSearch(search)).filter());
	}

	@Test void a01_timestampColumn_bindsOffsetDateTime_neverSqlNow() {
		var f = compile("happenedAt=$gt(-24h)");
		assertEquals(1, f.binds().size());
		assertInstanceOf(OffsetDateTime.class, f.binds().get(0));
		var sql = f.sql().toLowerCase();
		assertFalse(sql.contains("now") || sql.contains("interval") || sql.contains("24h"), f.sql());
	}

	@Test void a02_numericColumn_bindsSignedMillisBigDecimal() {
		var f = compile("elapsedMs=$gt(-24h)");
		assertList(f.binds(), "-86400000");
		assertFalse(f.sql().contains("24h"), f.sql());
	}

	@Test void a03_relativeAndAbsolute_bindTheSameClass() {
		var rel = compile("happenedAt=$gt(-24h)").binds().get(0);
		var abs = compile("happenedAt=$gt(2026-09-30T12:00:00Z)").binds().get(0);
		assertEquals(abs.getClass(), rel.getClass());
	}

	@Test void a04_andedBounds_shareOneRequestTimeInstant() {
		// Both operands resolve against the same captured instant, so they are exactly six days apart.
		var ctx = builder().build();
		var f = SqlSearchCompiler.create(TestDialect.INSTANCE, ctx).compile(ctx.resolve(new BeanQuery().setSearch("happenedAt=$and($gt(-7d),$lt(-1d))")).filter());
		assertEquals(2, f.binds().size());
		var lo = (OffsetDateTime)f.binds().get(0);
		var hi = (OffsetDateTime)f.binds().get(1);
		assertEquals(Duration.ofDays(6), Duration.between(lo, hi));
	}

	@Test void a05_malformedUnit_isBadValue() {
		var ex = assertThrows(BeanQuerySyntaxException.class, () -> compile("happenedAt=$gt(-24x)"));
		assertBean(ex, "code", "BAD_VALUE");
	}

	@Test void b01_find_timestampColumn_matchesRowsWithinTheWindow() {
		try (var s = builder().build().getSession()) {
			assertList(s.find(new BeanQuery().setSearch("happenedAt=$gt(-24h)").setSort("id")).rows(), "1");
		}
	}

	@Test void b02_find_timestampColumn_excludesRowsOutsideTheWindow() {
		try (var s = builder().build().getSession()) {
			assertList(s.find(new BeanQuery().setSearch("happenedAt=$lt(-24h)").setSort("id")).rows(), "2");
		}
	}

	@Test void b03_find_numericColumn_isPlainMillis_notRelativeToNow() {
		try (var s = builder().build().getSession()) {
			assertList(s.find(new BeanQuery().setSearch("elapsedMs=$gt(+1h)").setSort("id")).rows(), "1");
		}
	}

	@Test void a06_isoDuration_numericColumn_bindsSameMillisAsRelativeLiteral() {
		assertList(compile("elapsedMs=$gt(-PT24H)").binds(), "-86400000");
		assertList(compile("elapsedMs=$gt(+PT30M)").binds(), "1800000");
		assertList(compile("elapsedMs=$gt(P1D)").binds(), "86400000");
		assertEquals(compile("elapsedMs=$gt(-24h)").binds(), compile("elapsedMs=$gt(-PT24H)").binds());
	}

	@Test void a07_isoDuration_timestampColumn_bindsOffsetDateTime_neverSqlNow() {
		var f = compile("happenedAt=$gt(-PT24H)");
		assertEquals(1, f.binds().size());
		assertInstanceOf(OffsetDateTime.class, f.binds().get(0));
		var sql = f.sql().toLowerCase();
		assertFalse(sql.contains("now") || sql.contains("interval") || sql.contains("pt24h"), f.sql());
	}

	@Test void a08_isoDuration_andedBounds_shareOneRequestTimeInstant() {
		var ctx = builder().build();
		var f = SqlSearchCompiler.create(TestDialect.INSTANCE, ctx).compile(ctx.resolve(new BeanQuery().setSearch("happenedAt=$and($gt(-P7D),$lt(-P1D))")).filter());
		assertEquals(2, f.binds().size());
		assertEquals(Duration.ofDays(6), Duration.between((OffsetDateTime)f.binds().get(0), (OffsetDateTime)f.binds().get(1)));
	}

	@Test void a09_isoDuration_junk_isBadValue() {
		assertBean(assertThrows(BeanQuerySyntaxException.class, () -> compile("happenedAt=$gt(PTX)")), "code", "BAD_VALUE");
		assertBean(assertThrows(BeanQuerySyntaxException.class, () -> compile("elapsedMs=$gt(PT-1H)")), "code", "BAD_VALUE");
	}

	@Test void b04_find_isoDuration_matchesSameRowsAsRelativeLiteral() {
		try (var s = builder().build().getSession()) {
			assertList(s.find(new BeanQuery().setSearch("happenedAt=$gt(-PT24H)").setSort("id")).rows(), "1");
			assertList(s.find(new BeanQuery().setSearch("happenedAt=$lt(-PT24H)").setSort("id")).rows(), "2");
			assertList(s.find(new BeanQuery().setSearch("elapsedMs=$gt(PT1H)").setSort("id")).rows(), "1");
		}
	}
}
