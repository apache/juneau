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
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** Tests that {@link SqlSearchCompiler} hands dialects and custom renderers typed values (design #4 D2, §6). */
class SqlSearchCompiler_Test {

	/** Records exactly what the compiler passes it, instead of producing real SQL. */
	private static final class RecordingDialect implements SqlDialect {
		Object lastBareValue;
		boolean lastBareQuoted;
		List<Object> lastRendererArgs;

		@Override public String id() { return "recording"; }
		@Override public String quote(String identifier) { return identifier; }

		@Override public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) {
			lastBareValue = value;
			lastBareQuoted = quoted;
			return SqlFragment.of(columnSql + " = ?", value);
		}

		@Override public SearchSqlRenderer builtinRenderer(String operatorName) {
			return (d, col, type, args) -> {
				lastRendererArgs = args;
				return SqlFragment.of(col + " " + operatorName + " ?", args.get(0));
			};
		}

		@Override public String renderLimitOffset(Long limit, Long offset) { return ""; }
	}

	private static SqlBeanQueryContext.Builder<Object> builder(SqlDialect dialect) {
		return SqlBeanQueryContext.create(Object.class)
			.dialect(dialect)
			.table("t")
			.column("age", NUMERIC)
			.column("active", BOOLEAN)
			.column("name", TEXT)
			.connectionSupplier(() -> { throw new UnsupportedOperationException("render-only test"); });
	}

	private static SqlFragment compile(SqlBeanQueryContext<Object> ctx, SqlDialect dialect, String search) {
		return SqlSearchCompiler.create(dialect, ctx).compile(ctx.resolve(new BeanQuery().setSearch(search)).filter());
	}

	@Test void a01_renderBare_receivesTypedValue() {
		var d = new RecordingDialect();
		var f = compile(builder(d).build(), d, "age=30");
		assertEquals(new BigDecimal("30"), d.lastBareValue);
		assertFalse(d.lastBareQuoted);
		assertBean(f, "sql,binds", "age = ?,[30]");
	}

	@Test void a02_renderBare_booleanIsTyped() {
		var d = new RecordingDialect();
		compile(builder(d).build(), d, "active=TRUE");
		assertEquals(Boolean.TRUE, d.lastBareValue);
	}

	@Test void a03_renderBare_quotedFlagPassedThrough() {
		var d = new RecordingDialect();
		compile(builder(d).build(), d, "name=\"a*\"");
		assertEquals("a*", d.lastBareValue);
		assertTrue(d.lastBareQuoted);
	}

	@Test void b01_builtinRenderer_receivesTypedArgs() {
		var d = new RecordingDialect();
		compile(builder(d).build(), d, "age=$gt(5)");
		assertEquals(List.of(new BigDecimal("5")), d.lastRendererArgs);
	}

	@Test void b02_builtinRenderer_textArgsStayStrings() {
		var d = new RecordingDialect();
		compile(builder(d).build(), d, "name=$contains(err)");
		assertEquals(List.of("err"), d.lastRendererArgs);
	}

	@Test void c01_customOperator_defaultsToLiteralStrings() {
		var seen = new ArrayList<Object>();
		SearchSqlRenderer r = (dialect, col, type, args) -> { seen.addAll(args); return SqlFragment.of("(" + col + " % 2 = 0)"); };
		var op = SearchOperator.create("$mod", "Mod. Example: $mod(2)").minArgs(1).maxArgs(1).types(NUMERIC)
			.extension(SearchSqlRenderer.class, r).build();
		var d = new RecordingDialect();
		compile(builder(d).operators(SearchOperatorSet.standard().with(op)).build(), d, "age=$mod(2)");
		assertEquals(List.of("2"), seen);
	}

	@Test void c02_customOperator_typedArgsOptIn() {
		var seen = new ArrayList<Object>();
		SearchSqlRenderer r = (dialect, col, type, args) -> { seen.addAll(args); return SqlFragment.of("(" + col + " % ? = 0)", args.get(0)); };
		var op = SearchOperator.create("$mod", "Mod. Example: $mod(2)").minArgs(1).maxArgs(1).types(NUMERIC)
			.typedArgs(true).extension(SearchSqlRenderer.class, r).build();
		var d = new RecordingDialect();
		var f = compile(builder(d).operators(SearchOperatorSet.standard().with(op)).build(), d, "age=$mod(2)");
		assertEquals(List.of(new BigDecimal("2")), seen);
		assertBean(f, "sql,binds", "(age % ? = 0),[2]");
	}
}
