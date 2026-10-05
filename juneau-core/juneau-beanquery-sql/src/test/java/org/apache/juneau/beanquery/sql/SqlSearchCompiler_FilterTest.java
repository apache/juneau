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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** Tests {@link SqlSearchCompiler#compile(Filter)} — the cross-column rendering path (design §7, §8). */
class SqlSearchCompiler_FilterTest {

	private static final SearchOperatorSet OPS = SearchOperatorSet.standard();

	private static final SqlDialect DIALECT = new SqlDialect() {
		@Override public String id() { return "fake"; }
		@Override public String quote(String identifier) { return identifier; }
		@Override public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) {
			return SqlFragment.of(columnSql + " = ?", value);
		}
		@Override public SearchSqlRenderer builtinRenderer(String operatorName) {
			return switch (operatorName) {
				case "$eq" -> (d, columnSql, type, args) -> SqlFragment.of(columnSql + " = ?", args.get(0));
				case "$gt" -> (d, columnSql, type, args) -> SqlFragment.of(columnSql + " > ?", args.get(0));
				case "$contains" -> (d, columnSql, type, args) -> SqlFragment.of(columnSql + " LIKE ?", "%" + args.get(0) + "%");
				default -> null;
			};
		}
		@Override public String renderLimitOffset(Long limit, Long offset) { return ""; }
	};

	// ColumnResolver is a one-method @FunctionalInterface (columnSql only) — identity mapping is enough here.
	private static final ColumnResolver COLUMNS = c -> c;

	private static Filter.Leaf leaf(String column, String expr) {
		return new Filter.Leaf(column, SearchExpressionParser.parse(expr, OPS), SearchType.TEXT);
	}

	private final SqlSearchCompiler compiler = SqlSearchCompiler.create(DIALECT, COLUMNS);

	@Test void a01_null_returnsNull() {
		assertNull(compiler.compile((Filter) null));
	}

	@Test void a02_leaf_rendersBare() {
		var f = compiler.compile(leaf("age", "$gt(\"5\")"));
		assertBean(f, "sql,binds", "age > ?,[5]");
	}

	@Test void a03_and_wrapsAndJoinsWithAnd() {
		var f = compiler.compile(new Filter.And(List.of(leaf("age", "$gt(\"5\")"), leaf("name", "$eq(\"bob\")"))));
		assertBean(f, "sql,binds", "(age > ? AND name = ?),[5,bob]");
	}

	@Test void a04_or_wrapsAndJoinsWithOr() {
		var f = compiler.compile(new Filter.Or(List.of(leaf("name", "$contains(\"bob\")"), leaf("email", "$contains(\"bob\")"))));
		assertBean(f, "sql,binds", "(name LIKE ? OR email LIKE ?),[%bob%,%bob%]");
	}

	@Test void a05_not_wrapsInNot() {
		var f = compiler.compile(new Filter.Not(leaf("age", "$gt(\"5\")")));
		assertEquals("(NOT COALESCE(age > ?, FALSE))", f.sql());
	}

	@Test void a06_nestedGroups() {
		var f = compiler.compile(new Filter.And(List.of(
			leaf("age", "$gt(\"5\")"),
			new Filter.Or(List.of(leaf("name", "$contains(\"bob\")"), leaf("email", "$contains(\"bob\")"))))));
		assertBean(f, "sql,binds", "(age > ? AND (name LIKE ? OR email LIKE ?)),[5,%bob%,%bob%]");
	}

	@Test void a07_emptyAnd_matchesEveryRow() {
		var f = compiler.compile(new Filter.And(List.of()));
		assertEquals("1=1", f.sql());
	}

	@Test void a08_emptyOr_matchesNoRows() {
		var f = compiler.compile(new Filter.Or(List.of()));
		assertEquals("1=0", f.sql());
	}

	@Test void a09_leaf_unrenderableOperator_throws() {
		var hoistedArg1 = leaf("age", "$lt(\"5\")");
		var e = assertThrows(BeanQuerySyntaxException.class, () -> compiler.compile(hoistedArg1));
		assertBean(e, "code,message", "BAD_VALUE,Operator '$lt' is not supported by the 'fake' SQL dialect.");
	}

	@Test void a10_regex_dialectWithoutRegex_rejected() {
		var hoistedArg2 = leaf("name", "$regex(\"a.*\")");
		var e = assertThrows(BeanQuerySyntaxException.class, () -> compiler.compile(hoistedArg2));
		assertBean(e, "code,message", "BAD_VALUE,Operator '$regex' is not supported by the 'fake' SQL dialect.");
	}

	@Test void a11_versionRange_dialectWithoutVersionOrdering_rejected() {
		for (var op : List.of("$gt(\"9.9\")", "$gte(\"9.9\")", "$lt(\"9.9\")", "$lte(\"9.9\")", "$between(\"9.9\",\"9.10\")")) {
			var hoistedArg3 = versionLeaf(op);
			var e = assertThrows(BeanQuerySyntaxException.class, () -> compiler.compile(hoistedArg3));
			assertBean(e, "code", "BAD_VALUE");
			assertTrue(e.getMessage().contains("not supported on version columns"), e.getMessage());
		}
	}

	@Test void a12_versionEquality_dialectWithoutVersionOrdering_allowed() {
		assertBean(compiler.compile(versionLeaf("$eq(\"9.9\")")), "sql,binds", "v = ?,[9.9]");
	}

	@Test void a13_versionRange_dialectWithVersionOrdering_allowed() {
		var c = SqlSearchCompiler.create(new SqlDialect() {
			@Override public String id() { return "ordered"; }
			@Override public String quote(String identifier) { return identifier; }
			@Override public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) { return SqlFragment.of("1=1"); }
			@Override public boolean supportsVersionOrdering() { return true; }
			@Override public SearchSqlRenderer builtinRenderer(String operatorName) { return (d, col, t, a) -> SqlFragment.of(col + " > ?", a.get(0)); }
			@Override public String renderLimitOffset(Long limit, Long offset) { return ""; }
		}, COLUMNS);
		assertBean(c.compile(versionLeaf("$gt(\"9.9\")")), "sql,binds", "v > ?,[9.9]");
	}

	private static Filter.Leaf versionLeaf(String expr) {
		return new Filter.Leaf("v", SearchExpressionParser.parse(expr, OPS), SearchType.VERSION);
	}
}
