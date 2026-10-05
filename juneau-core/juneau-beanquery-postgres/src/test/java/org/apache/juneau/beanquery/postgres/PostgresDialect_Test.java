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
package org.apache.juneau.beanquery.postgres;

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.beanquery.sql.*;
import org.junit.jupiter.api.*;

class PostgresDialect_Test {

	private static final SqlDialect PG = PostgresDialect.INSTANCE;

	private static SqlBeanQueryContext.Builder<Object> builder() {
		return SqlBeanQueryContext.create(Object.class)
			.dialect(PG)
			.table("tasks")
			.column("name", TEXT)
			.column("age", NUMERIC)
			.column("version", VERSION)
			.column("status", ENUM)
			.connectionSupplier(() -> { throw new UnsupportedOperationException("render-only test"); });
	}

	private static SqlBeanQueryContext<Object> context() {
		return builder().build();
	}

	private static SqlFragment compile(SqlBeanQueryContext<Object> ctx, BeanQuery q) {
		return SqlSearchCompiler.create(PG, ctx).compile(ctx.resolve(q).filter());
	}

	/** The column-as-tuple expression {@code PostgresDialect} renders for VERSION column {@code version}. */
	private static final String VCOL = "(CASE WHEN \"version\" ~ E'^\\\\s*[0-9]{1,9}(\\\\s*\\\\.\\\\s*[0-9]{1,9})*\\\\s*$' THEN string_to_array(\"version\", '.')::int[] END)";

	/** The bind-side tuple expression. */
	private static final String VARG = "string_to_array(?, '.')::int[]";

	/** The blank-trim character set (ASCII whitespace, matching {@code String.isBlank()}). */
	private static final String BLANKSET = "E' \\t\\n\\x0B\\f\\r\\x1C\\x1D\\x1E\\x1F'";

	private static SqlFragment where(String column, String expr) {
		return compile(context(), new BeanQuery().setSearch(column + "=" + expr));
	}

	//====================================================================================================
	// Built-in leaf operators
	//====================================================================================================

	@Test
	void a01_eqText_exact() {
		var f = where("name", "$eq(OPEN)");
		assertBean(f, "sql,binds", "\"name\" = ?,[OPEN]");
	}

	@Test
	void a02_eqNumeric_typedBind() {
		var f = where("age", "$eq(30)");
		assertBean(f, "sql,binds", "\"age\" = ?,[30]");
	}

	@Test
	void a03_contains() {
		var f = where("name", "$contains(err)");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[%err%]");
	}

	@Test
	void a04_prefixText() {
		var f = where("name", "$prefix(ab)");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[ab%]");
	}

	@Test
	void a05_prefixVersion() {
		var f = where("version", "$prefix(10.0)");
		assertEquals(VCOL + "[1:2] = " + VARG, f.sql());
		assertEquals(List.of("10.0"), f.binds());
	}

	@Test
	void a06_in_exact() {
		var f = where("status", "$in(OPEN,CLOSED)");
		assertBean(f, "sql,binds", "\"status\"::text IN (?, ?),[OPEN,CLOSED]");
	}

	@Test
	void a07_ne_text_exact_keepsBlank() {
		var f = where("status", "$ne(OPEN)");
		assertBean(f, "sql,binds", "(\"status\" IS NULL OR btrim(\"status\"::text, " + BLANKSET + ") = '' OR \"status\"::text NOT IN (?)),[OPEN]");
	}

	@Test
	void a08_comparisons() {
		assertEquals("\"age\" > ?", where("age", "$gt(100)").sql());
		assertEquals("\"age\" >= ?", where("age", "$gte(100)").sql());
		assertEquals("\"age\" < ?", where("age", "$lt(100)").sql());
		assertEquals("\"age\" <= ?", where("age", "$lte(100)").sql());
		assertEquals(List.of(new BigDecimal("100")), where("age", "$gt(100)").binds());
	}

	@Test
	void a09_between() {
		var f = where("age", "$between(1,100)");
		assertBean(f, "sql,binds", "\"age\" BETWEEN ? AND ?,[1,100]");
	}

	@Test
	void a10_blank() {
		var f = where("name", "$blank()");
		assertBean(f, "sql,binds{size}", "(\"name\" IS NULL OR btrim(\"name\", " + BLANKSET + ") = ''),{0}");
	}

	@Test
	void a11_regexDefaultCaseInsensitiveAnchored() {
		var f = where("name", "$regex(er.*)");
		assertBean(f, "sql,binds", "\"name\" ~* ?,[(?p)\\A(?:er.*)\\Z]");
	}

	@Test
	void a12_regexFlagsCaseSensitiveMultiline() {
		var f = where("name", "$regex(er.*,flags=m)");
		assertBean(f, "sql,binds", "\"name\" ~ ?,[(?n)\\A(?:er.*)\\Z]");
	}

	//====================================================================================================
	// Combinators, bare patterns, AND across columns
	//====================================================================================================

	@Test
	void b01_or() {
		var f = where("status", "$or($eq(OPEN),$eq(CLOSED))");
		assertBean(f, "sql,binds", "(\"status\"::text = ? OR \"status\"::text = ?),[OPEN,CLOSED]");
	}

	@Test
	void b02_not() {
		var f = where("name", "$not($eq(x))");
		assertBean(f, "sql,binds", "(NOT COALESCE(\"name\" = ?, FALSE)),[x]");
	}

	@Test
	void b03_bareText() {
		var f = where("name", "Bill");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[%Bill%]");
	}

	@Test
	void b04_bareWildcard() {
		var f = where("name", "*Bi*ll*");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[%Bi%ll%]");
	}

	@Test
	void b05_columnsAnded() {
		// Routed through compile(Filter): a multi-item top-level Filter.And always parenthesizes, unlike
		// the old per-column compile(ResolvedQuery), which AND-ed terms unwrapped.
		var f = compile(context(), new BeanQuery().setSearch("name=$eq(a),age=$gt(1)"));
		assertBean(f, "sql,binds", "(\"name\" = ? AND \"age\" > ?),[a,1]");
	}

	@Test
	void b06_noSearch_rendersAlwaysTrue() {
		// ResolvedQuery.filter() is never null (an empty search resolves to an empty Filter.And), and
		// compile(Filter) renders an empty top-level And as always-true "1=1" per its own contract; the caller
		// (SqlBeanQueryContext.appendWhere) is the one that special-cases this to omit a spurious WHERE clause.
		var f = compile(context(), new BeanQuery());
		assertBean(f, "sql,binds", "1=1,[]");
	}

	//====================================================================================================
	// Full statement rendering
	//====================================================================================================

	@Test
	void c01_renderRows_viewSortPage() {
		var ctx = context();
		var q = new BeanQuery().setSearch("age=$gt(1)").setView("name,age").setSort("age:desc").setPosition(10).setLimit(5);
		var s = ctx.renderRows(ctx.resolve(q));
		assertBean(s, "sql,binds,columns",
			"SELECT \"name\", \"age\" FROM \"tasks\" WHERE \"age\" > ? ORDER BY \"age\" DESC LIMIT 5 OFFSET 10,[1],[name,age]");
	}

	@Test
	void c02_renderRows_defaultColumnsAndLimit() {
		var ctx = context();
		var s = ctx.renderRows(ctx.resolve(new BeanQuery()));
		assertEquals("SELECT \"name\", \"age\", \"version\", \"status\" FROM \"tasks\" LIMIT 100", s.sql());
	}

	@Test
	void c03_renderCount_totalVsMatched_withGuard() {
		var ctx = context().copy().guard(SqlFragment.of("\"tenant\" = ?", "t1")).build();
		var q = ctx.resolve(new BeanQuery().setSearch("age=$gt(1)"));
		var total = ctx.renderCount(q, false);
		assertBean(total, "sql,binds", "SELECT count(*) FROM \"tasks\" WHERE (\"tenant\" = ?),[t1]");
		var matched = ctx.renderCount(q, true);
		assertBean(matched, "sql,binds", "SELECT count(*) FROM \"tasks\" WHERE (\"tenant\" = ?) AND \"age\" > ?,[t1,1]");
	}

	@Test
	void c04_renderRows_guardAndCrossColumnOrSearch_singleParenAroundOr() {
		// Confirms the appendWhere fix: guard stays defensively parenthesized, but the cross-column $or search --
		// already self-parenthesized by SqlSearchCompiler.compile(Filter)'s own top-level And/Or rendering -- is no
		// longer wrapped a second time by appendWhere.
		var ctx = context().copy().guard(SqlFragment.of("\"tenant\" = ?", "t1")).build();
		var q = ctx.resolve(new BeanQuery().setSearch("$or(name=$eq(Alice),age=$gt(35))"));
		var s = ctx.renderRows(q);
		assertBean(s, "sql,binds",
			"SELECT \"name\", \"age\", \"version\", \"status\" FROM \"tasks\" WHERE (\"tenant\" = ?) AND "
				+ "(\"name\" = ? OR \"age\" > ?) LIMIT 100,[t1,Alice,35]");
	}

	@Test
	void c05_renderRows_regexSearch_sqlTextUnchangedByStatementTimeoutFix() {
		// Golden: the $regex statement-timeout fallback lives entirely in the dialect-agnostic
		// juneau-beanquery-sql module and must never alter the SQL text PostgresDialect renders for $regex.
		var ctx = context();
		var s = ctx.renderRows(ctx.resolve(new BeanQuery().setSearch("name=$regex(er.*)")));
		assertBean(s, "sql,binds", "SELECT \"name\", \"age\", \"version\", \"status\" FROM \"tasks\" WHERE \"name\" ~* ? LIMIT 100,[(?p)\\A(?:er.*)\\Z]");
	}

	//====================================================================================================
	// Failure modes
	//====================================================================================================

	@Test
	void d01_customOperatorWithoutSqlRenderer_fails() {
		var even = SearchOperator.create("$even", "Even numbers. Example: $even()").minArgs(0).maxArgs(0).types(NUMERIC)
			.predicate((cell, args) -> true).build();
		var ctx = context().copy().operators(SearchOperatorSet.standard().with(even)).build();
		var q = new BeanQuery().setSearch("age=$even()");
		var e = assertThrows(IllegalStateException.class, () -> compile(ctx, q));
		assertTrue(e.getMessage().contains("has no SQL renderer"), e.getMessage());
	}

	@Test
	void d02_customOperatorRendererDeclinesDialect_fails() {
		SearchSqlRenderer renderer = (dialect, col, type, args) -> null;  // Declines every dialect.
		var even = SearchOperator.create("$even", "Even numbers. Example: $even()").minArgs(0).maxArgs(0).types(NUMERIC)
			.extension(SearchSqlRenderer.class, renderer).build();
		var ctx = context().copy().operators(SearchOperatorSet.standard().with(even)).build();
		var q = new BeanQuery().setSearch("age=$even()");
		var e = assertThrows(IllegalStateException.class, () -> compile(ctx, q));
		assertTrue(e.getMessage().contains("returned no SQL"), e.getMessage());
	}

	@Test
	void d03_customOperatorRenderer_used() {
		SearchSqlRenderer renderer = (dialect, col, type, args) -> SqlFragment.of("(" + col + " % 2 = 0)");
		var even = SearchOperator.create("$even", "Even numbers. Example: $even()").minArgs(0).maxArgs(0).types(NUMERIC)
			.extension(SearchSqlRenderer.class, renderer).build();
		var ctx = context().copy().operators(SearchOperatorSet.standard().with(even)).build();
		var f = compile(ctx, new BeanQuery().setSearch("age=$even()"));
		assertEquals("(\"age\" % 2 = 0)", f.sql());
	}

	@Test
	void d04_malformedExpression_rejectedByResolve() {
		var q = new BeanQuery().setSearch("name=$nope(x)");
		var hoistedTarget1 = context();
		assertThrows(BeanQuerySyntaxException.class, () -> hoistedTarget1.resolve(q));
	}

	//====================================================================================================
	// e - $eqic, ?/* wildcards, quoted bare (D1, D2)
	//====================================================================================================

	@Test
	void e01_eqicText_caseInsensitive() {
		var f = where("name", "$eqic(OPEN)");
		assertBean(f, "sql,binds", "lower(\"name\") = lower(?),[OPEN]");
	}

	@Test
	void e02_eqicEnum_caseInsensitive() {
		var f = where("status", "$eqic(open)");
		assertBean(f, "sql,binds", "lower(\"status\"::text) = lower(?),[open]");
	}

	@Test
	void e03_bareQuestionWildcard_mapsToUnderscore() {
		var f = where("name", "a?c");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[a_c]");
	}

	@Test
	void e04_bareStarAndQuestionCombine() {
		var f = where("name", "a*b?c");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[a%b_c]");
	}

	@Test
	void e05_quotedBare_bypassesWildcardMapping() {
		var f = where("name", "\"a*\"");
		assertBean(f, "sql,binds", "\"name\" ILIKE ?,[%a*%]");  // * is not a Postgres LIKE metachar, so it's left as literal text here
	}

	@Test
	void e06_bareEnum_isCaseInsensitiveEquals() {
		var f = where("status", "OPEN");
		assertBean(f, "sql,binds", "lower(\"status\"::text) = lower(?),[OPEN]");
	}

	//====================================================================================================
	// f - TIMESTAMP literals bind as the typed value QueryResolver produced (design #4 §6): a LocalDate stays a LocalDate
	//====================================================================================================

	private static SqlBeanQueryContext<Object> timestampContext() {
		return builder().column("due", TIMESTAMP).build();
	}

	@Test
	void f01_bareDate_bindsLocalDate() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=2024-01-01"));
		assertBean(f, "sql,binds", "\"due\" = ?,[2024-01-01]");
	}

	@Test
	void f02_eqOperator_bareDateArgument_bindsLocalDate() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=$eq(2024-01-01)"));
		assertBean(f, "sql,binds", "\"due\" = ?,[2024-01-01]");
	}

	@Test
	void f03_betweenDates_bindBothEndpointsAsLocalDates() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=$between(2024-01-01, 2024-01-02)"));
		assertBean(f, "sql,binds", "\"due\" BETWEEN ? AND ?,[2024-01-01,2024-01-02]");
	}

	@Test
	void f04_malformedDate_isBadValueAtResolve() {
		var hoistedArg1 = timestampContext();
		var hoistedArg2 = new BeanQuery().setSearch("due=2024-13-40");
		var e = assertThrows(BeanQuerySyntaxException.class,
			() -> compile(hoistedArg1, hoistedArg2));  // not a real calendar date
		assertBean(e, "code,message", "BAD_VALUE,Value '2024-13-40' is not a valid timestamp for column 'due'.");
	}

	@Test
	void f05_fullIsoInstant_bindsOffsetDateTime() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=2024-01-01T08:00:00Z"));
		assertBean(f, "sql,binds", "\"due\" = ?,[2024-01-01T08:00Z]");
	}

	//====================================================================================================
	// g - ENUM behaves like the in-memory engine: text-cast column, bare wildcards, exact $eq/$in/$ne
	//====================================================================================================

	@Test
	void g01_bareEnumStar_isWholeStringCaseInsensitiveGlobOverCastColumn() {
		var f = where("status", "op*");
		assertEquals("\"status\"::text ILIKE ?", f.sql());
		assertEquals(List.of("op%"), f.binds());
	}

	@Test
	void g02_bareEnumQuestion_mapsToUnderscore() {
		var f = where("status", "OPE?");
		assertEquals("\"status\"::text ILIKE ?", f.sql());
		assertEquals(List.of("OPE_"), f.binds());
	}

	@Test
	void g03_bareEnumWildcard_escapesLikeMetacharacters() {
		var f = where("status", "a_b*");
		assertEquals(List.of("a\\_b%"), f.binds());
	}

	@Test
	void g04_quotedBareEnum_isLiteralEqualityNotWildcard() {
		var f = where("status", "\"op*\"");
		assertBean(f, "sql,binds", "lower(\"status\"::text) = lower(?),[op*]");
	}

	@Test
	void g05_eqEnum_exactOverCastColumn() {
		var f = where("status", "$eq(OPEN)");
		assertBean(f, "sql,binds", "\"status\"::text = ?,[OPEN]");
	}

	@Test
	void g06_textColumnIsNeverCast() {
		assertEquals("lower(\"name\") = lower(?)", where("name", "$eqic(x)").sql());
	}

	//====================================================================================================
	// h - VERSION stored as dotted text compares as a tuple of integers (InMemoryMatch.cmpVersion)
	//====================================================================================================

	@Test
	void h01_eqVersion_tupleEquality() {
		var f = where("version", "$eq(1.2)");
		assertEquals(VCOL + " = " + VARG, f.sql());
		assertEquals(List.of("1.2"), f.binds());
	}

	@Test
	void h02_comparisons_useTupleOrderNotTextOrder() {
		assertEquals(VCOL + " > " + VARG, where("version", "$gt(1.10)").sql());
		assertEquals(VCOL + " >= " + VARG, where("version", "$gte(1.10)").sql());
		assertEquals(VCOL + " < " + VARG, where("version", "$lt(1.10)").sql());
		assertEquals(VCOL + " <= " + VARG, where("version", "$lte(1.10)").sql());
		assertEquals(List.of("1.10"), where("version", "$gt(1.10)").binds());
	}

	@Test
	void h02b_versionOrdering_declaredSupported_soRangesAreNotRejected() {
		assertTrue(PG.supportsVersionOrdering());
	}

	@Test
	void h03_between() {
		var f = where("version", "$between(1.2,1.10)");
		assertEquals(VCOL + " BETWEEN " + VARG + " AND " + VARG, f.sql());
		assertEquals(List.of("1.2", "1.10"), f.binds());
	}

	@Test
	void h04_in_orsTupleEqualities() {
		var f = where("version", "$in(1.2,2.0)");
		assertEquals("(" + VCOL + " = " + VARG + " OR " + VCOL + " = " + VARG + ")", f.sql());
		assertEquals(List.of("1.2", "2.0"), f.binds());
	}

	@Test
	void h05_ne_keepsBlankAndUnparseableCells() {
		var f = where("version", "$ne(1.2)");
		assertEquals("(\"version\" IS NULL OR btrim(\"version\", " + BLANKSET + ") = '' OR NOT COALESCE(" + VCOL + " = " + VARG + ", FALSE))", f.sql());
		assertEquals(List.of("1.2"), f.binds());
	}

	@Test
	void h06_bareVersion_isTuplePrefix() {
		var f = where("version", "10");
		assertEquals(VCOL + "[1:1] = " + VARG, f.sql());
		assertEquals(List.of("10"), f.binds());
	}

	@Test
	void h07_prefixLength_followsNumberOfParts() {
		assertEquals(VCOL + "[1:3] = " + VARG, where("version", "$prefix(1.2.3)").sql());
	}

	@Test
	void h08_unparseableArgument_neverMatches() {
		var f = where("version", "$eq(abc)");
		assertBean(f, "sql,binds{size}", "(1 = 0),{0}");
		assertEquals("(1 = 0)", where("version", "$gt(1.x)").sql());
		assertEquals("(1 = 0)", where("version", "abc").sql());
	}

	@Test
	void h09_neWithOnlyUnparseableArguments_matchesEverything() {
		var f = where("version", "$ne(abc)");
		assertBean(f, "sql,binds{size}", "(\"version\" IS NULL OR btrim(\"version\", " + BLANKSET + ") = '' OR 1 = 1),{0}");
	}

	//====================================================================================================
	// i - $regex flags mirror java.util.regex (InMemoryMatch): i / m / s, full-string anchoring
	//====================================================================================================

	private static void assertRegex(String args, String operator, String bind) {
		var f = where("name", "$regex(" + args + ")");
		assertEquals("\"name\" " + operator + " ?", f.sql());
		assertEquals(List.of(bind), f.binds());
	}

	@Test
	void i01_flagsWithoutI_areCaseSensitive() {
		assertRegex("x,flags=s", "~", "\\A(?:x)\\Z");
	}

	@Test
	void i02_dotall_isPostgresDefault() {
		assertRegex("x,flags=is", "~*", "\\A(?:x)\\Z");
	}

	@Test
	void i03_multilineAndDotall_isNewlineSensitiveWithDotMatchingNewline() {
		assertRegex("x,flags=ims", "~*", "(?w)\\A(?:x)\\Z");
	}

	@Test
	void i04_multilineOnly_isNewlineSensitive() {
		assertRegex("x,flags=im", "~*", "(?n)\\A(?:x)\\Z");
	}

	@Test
	void i05_iOnly_dotStopsAtNewline() {
		assertRegex("x,flags=i", "~*", "(?p)\\A(?:x)\\Z");
	}

	//====================================================================================================
	// j - typed values arrive from the resolver and are bound as-is; bad values never reach the dialect
	//====================================================================================================

	@Test
	void j01_bareNumeric_isTypedEquality() {
		var f = where("age", "30");
		assertEquals("\"age\" = ?", f.sql());
		assertEquals(List.of(new BigDecimal("30")), f.binds());
	}

	@Test
	void j02_bareBoolean_isTypedEquality() {
		var f = compile(builder().column("active", BOOLEAN).build(), new BeanQuery().setSearch("active=TRUE"));
		assertEquals("\"active\" = ?", f.sql());
		assertEquals(List.of(Boolean.TRUE), f.binds());
	}

	@Test
	void j03_bareTimestamp_bindsTypedLocalDate() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=2024-01-01"));
		assertInstanceOf(java.time.LocalDate.class, f.binds().get(0));
	}

	@Test
	void j04_isoInstant_bindsOffsetDateTime() {
		var f = compile(timestampContext(), new BeanQuery().setSearch("due=$gt(2024-01-01T08:00:00Z)"));
		assertInstanceOf(java.time.OffsetDateTime.class, f.binds().get(0));
	}

	@Test
	void j05_malformedNumeric_isBadValueBeforeTheDialect() {
		var e = assertThrows(BeanQuerySyntaxException.class, () -> where("age", "$eq(notanumber)"));
		assertBean(e, "code", "BAD_VALUE");
	}

	@Test
	void j06_malformedBoolean_isBadValueBeforeTheDialect() {
		var ctx = builder().column("active", BOOLEAN).build();
		var hoistedArg3 = new BeanQuery().setSearch("active=$eq(maybe)");
		var e = assertThrows(BeanQuerySyntaxException.class, () -> compile(ctx, hoistedArg3));
		assertBean(e, "code", "BAD_VALUE");
	}

	@Test
	void j07_textPathRejectsNonStringValue() {
		var contains = PG.builtinRenderer("$contains");
		var hoistedArg4 = List.<Object>of(5);
		var e = assertThrows(IllegalStateException.class, () -> contains.render(PG, "\"name\"", TEXT, hoistedArg4));
		assertTrue(e.getMessage().contains("java.lang.Integer"), e.getMessage());
		assertThrows(IllegalStateException.class, () -> PG.renderBare("\"name\"", TEXT, 5, false));
	}

	@Test
	void j08_customTypedOperator_receivesBigDecimal() {
		var seen = new ArrayList<Object>();
		SearchSqlRenderer renderer = (dialect, col, type, args) -> { seen.addAll(args); return SqlFragment.of("(" + col + " % ? = 0)", args.get(0)); };
		var mod = SearchOperator.create("$mod", "Divisible. Example: $mod(2)").minArgs(1).maxArgs(1).types(NUMERIC)
			.typedArgs(true).extension(SearchSqlRenderer.class, renderer).build();
		var ctx = context().copy().operators(SearchOperatorSet.standard().with(mod)).build();
		var f = compile(ctx, new BeanQuery().setSearch("age=$mod(2)"));
		assertEquals(List.of(new BigDecimal("2")), seen);
		assertBean(f, "sql,binds", "(\"age\" % ? = 0),[2]");
	}

	//====================================================================================================
	// k - review fixes: VERSION int range, $regex validation, blank definition, $not null semantics
	//====================================================================================================

	@Test
	void k01_versionPartsOverNineDigits_neverMatch() {
		assertEquals("(1 = 0)", where("version", "$eq(1234567890)").sql());
		assertEquals("(1 = 0)", where("version", "$gt(1.12345678901)").sql());
		assertEquals("(1 = 0)", where("version", "$prefix(1.2345678901)").sql());
		var f = where("version", "$eq(123456789.000000001)");
		assertEquals(VCOL + " = " + VARG, f.sql());
		assertEquals(List.of("123456789.1"), f.binds());
	}

	@Test
	void k02_versionArgNormalizedThroughParseInt() {
		assertEquals(List.of("1.2.3"), where("version", "$eq( 01 . 02 . 3 )").binds());
	}

	@Test
	void k03_versionGuard_usesEscapeStringAndNoPlaceholderChar() {
		assertFalse(VCOL.contains("bigint"));
		assertTrue(VCOL.contains("~ E'"));
		assertEquals(-1, VCOL.indexOf('?'));
	}

	@Test
	void k04_invalidRegex_rendersNever() {
		assertBean(where("name", "$regex(\"x)|(y\")"), "sql,binds{size}", "(1 = 0),{0}");
		assertEquals("(1 = 0)", where("name", "$regex([a-)").sql());
		assertEquals("(1 = 0)", where("name", "$regex(\"x)|(y\",flags=i)").sql());
	}

	@Test
	void k05_validRegex_stillRenders() {
		assertEquals("\"name\" ~* ?", where("name", "$regex(a|b)").sql());
	}

	@Test
	void k06_blank_coversAsciiWhitespace() {
		assertTrue(where("name", "$blank()").sql().contains("\\t\\n\\x0B\\f\\r"));
		assertTrue(where("name", "$ne(x)").sql().contains("btrim(\"name\", E'"));
	}

	@Test
	void k07_columnLevelNot_coalescesInnerPredicate() {
		assertBean(where("age", "$not($gt(5))"), "sql,binds", "(NOT COALESCE(\"age\" > ?, FALSE)),[5]");
		assertEquals("(NOT COALESCE(\"name\" ILIKE ?, FALSE))", where("name", "$not($contains(x))").sql());
	}

	@Test
	void k08_topLevelFilterNot_coalescesInnerPredicate() {
		var ctx = context();
		var inner = ctx.resolve(new BeanQuery().setSearch("status=$eq(OPEN)")).filter();
		var f = SqlSearchCompiler.create(PG, ctx).compile(new Filter.Not(inner));
		assertEquals("(NOT COALESCE(\"status\"::text = ?, FALSE))", f.sql());
	}

	@Test
	void k09_relativeDurationLiteral_timestampColumn_bindsOffsetDateTime_notIntervalSql() {
		var ctx = builder().column("createdAt", TIMESTAMP).build();
		var f = compile(ctx, new BeanQuery().setSearch("createdAt=$gt(-24h)"));
		assertTrue(f.sql().endsWith(" > ?"), f.sql());
		assertFalse(f.sql().toLowerCase().contains("interval") || f.sql().toLowerCase().contains("now()"), f.sql());
		assertEquals(1, f.binds().size());
		assertInstanceOf(java.time.OffsetDateTime.class, f.binds().get(0));
	}

	@Test
	void k10_isoDurationLiteral_timestampColumn_bindsOffsetDateTime_sameShapeAsRelativeLiteral() {
		var ctx = builder().column("createdAt", TIMESTAMP).build();
		var iso = compile(ctx, new BeanQuery().setSearch("createdAt=$gt(-PT24H)"));
		var rel = compile(ctx, new BeanQuery().setSearch("createdAt=$gt(-24h)"));
		assertEquals(rel.sql(), iso.sql());
		assertFalse(iso.sql().toLowerCase().contains("interval") || iso.sql().toLowerCase().contains("now()"), iso.sql());
		assertEquals(1, iso.binds().size());
		assertInstanceOf(java.time.OffsetDateTime.class, iso.binds().get(0));
	}
}
