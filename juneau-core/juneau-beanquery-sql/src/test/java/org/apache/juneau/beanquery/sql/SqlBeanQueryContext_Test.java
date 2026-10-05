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

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** Tests {@link SqlBeanQueryContext}'s guard+search {@code WHERE}-clause assembly (design §8). */
class SqlBeanQueryContext_Test {

	private static final SqlDialect DIALECT = new SqlDialect() {
		@Override public String id() { return "fake"; }
		@Override public String quote(String identifier) { return identifier; }
		@Override public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) {
			return SqlFragment.of(columnSql + " = ?", value);
		}
		@Override public SearchSqlRenderer builtinRenderer(String operatorName) {
			return "$eq".equals(operatorName) ? (d, columnSql, type, args) -> SqlFragment.of(columnSql + " = ?", args.get(0)) : null;
		}
		@Override public String renderLimitOffset(Long limit, Long offset) { return ""; }
	};

	private static SqlBeanQueryContext.Builder<Object> builder() {
		return SqlBeanQueryContext.create(Object.class)
			.dialect(DIALECT)
			.table("people")
			.column("name", SearchType.TEXT)
			.connectionSupplier(() -> { throw new UnsupportedOperationException("render-only test"); });
	}

	@Test void a01_guardAndSearch_guardParenthesizedSearchUnwrapped() {
		// Guard is raw caller SQL, so it stays defensively parenthesized; search is a single top-level Filter.Leaf,
		// which SqlSearchCompiler.compile(Filter) already renders as one atomic, unwrapped unit, so appendWhere
		// does not add a redundant extra pair of parens around it.
		var ctx = builder().guard(SqlFragment.of("a = ? OR b = ?", 1, 2)).build();
		var stmt = ctx.renderRows(ctx.resolve(BeanQuery.create().eq("name", "bob").build()));
		assertBean(stmt, "sql,binds", "SELECT name FROM people WHERE (a = ? OR b = ?) AND name = ?,[1,2,bob]");
	}

	@Test void a02_guardOnly_alwaysWrapped() {
		var ctx = builder().guard(SqlFragment.of("a = ?", 1)).build();
		var stmt = ctx.renderRows(ctx.resolve(BeanQuery.create().build()));
		assertBean(stmt, "sql,binds", "SELECT name FROM people WHERE (a = ?),[1]");
	}

	@Test void a03_searchOnly_unwrappedForSingleLeaf() {
		var ctx = builder().build();
		var stmt = ctx.renderRows(ctx.resolve(BeanQuery.create().eq("name", "bob").build()));
		assertBean(stmt, "sql,binds", "SELECT name FROM people WHERE name = ?,[bob]");
	}

	@Test void a04_neither_noWhereClause() {
		var ctx = builder().build();
		var stmt = ctx.renderRows(ctx.resolve(BeanQuery.create().build()));
		assertEquals("SELECT name FROM people", stmt.sql());
	}
}
