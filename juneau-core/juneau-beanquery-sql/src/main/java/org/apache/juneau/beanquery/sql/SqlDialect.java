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

import org.apache.juneau.commons.beanquery.*;

/**
 * The SQL-generation strategy for a SQL {@link BeanQueryContext}: it quotes identifiers, renders each built-in
 * {@code $}-operator into a dialect-correct predicate, and renders pagination.
 *
 * <p>
 * The shared JDBC context (the {@code juneau-beanquery-sql} module) is dialect-agnostic plumbing: it walks a parsed
 * search tree, delegates every leaf to the dialect, and combines leaves with {@code AND}/{@code OR}/{@code NOT} itself.
 * All dialect-specific SQL text &mdash; identifier quoting, the built-in operator predicates, the {@code LIMIT}/
 * {@code OFFSET} clause &mdash; lives here.  A custom operator instead carries its own {@link SearchSqlRenderer}
 * (attached with {@link org.apache.juneau.commons.beanquery.SearchOperator.Builder#extension(Class, Object) SearchOperator.Builder.extension}); the
 * context calls that renderer, passing this dialect so it can branch on {@link #id()}.
 *
 * <p>
 * Juneau ships exactly one dialect today (Postgres, in {@code juneau-beanquery-postgres}).  A dialect returns
 * <jk>null</jk> from {@link #builtinRenderer(String)} for any built-in it cannot express (for example {@code $regex} on a
 * database without regex support), and the context then rejects the query with
 * {@link org.apache.juneau.commons.beanquery.BeanQuerySyntaxException.Code#BAD_VALUE BAD_VALUE} rather than emitting SQL that differs from the in-memory
 * result.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SqlDialect <jv>dialect</jv> = PostgresDialect.<jsf>INSTANCE</jsf>;
 * 	SqlFragment <jv>frag</jv> = <jv>dialect</jv>.renderBare(<jv>dialect</jv>.quote(<js>"status"</js>), SearchType.<jsf>ENUM</jsf>, <js>"OPEN"</js>, <jk>false</jk>);
 * 	<jc>// frag.sql() == "lower(\"status\"::text) = lower(?)", frag.binds() == ["OPEN"]</jc>
 *
 * 	<jc>// A NUMERIC column receives the already-typed value, not the raw token.</jc>
 * 	SqlFragment <jv>age</jv> = <jv>dialect</jv>.renderBare(<jv>dialect</jv>.quote(<js>"age"</js>), SearchType.<jsf>NUMERIC</jsf>, <jk>new</jk> BigDecimal(<js>"30"</js>), <jk>false</jk>);
 * 	<jc>// age.sql() == "\"age\" = ?", age.binds() == [30]</jc>
 * </p>
 *
 * @since 10.0.0
 */
public interface SqlDialect {

	/**
	 * The stable lowercase dialect id (for example {@code "postgres"}).
	 *
	 * @return The dialect id, never <jk>null</jk> or blank.
	 */
	String id();

	/**
	 * Quotes a column or table identifier for this dialect (for example {@code age} &rarr; {@code "age"}).
	 *
	 * @param identifier The raw identifier.  Must not be <jk>null</jk>.
	 * @return The quoted identifier.
	 */
	String quote(String identifier);

	/**
	 * Renders a bare pattern term (design §5.1) &mdash; text with optional {@code *}/{@code ?} wildcards, a version
	 * prefix, or an exact value for the other types &mdash; into a predicate.
	 *
	 * @param columnSql The dialect-quoted column reference.  Must not be <jk>null</jk>.
	 * @param type The column's value type.  Must not be <jk>null</jk>.
	 * @param value The literal's already-typed value ({@link SearchExpression#typedValue()}): a
	 * 	{@link java.math.BigDecimal} for {@code NUMERIC}, a {@link Boolean} for {@code BOOLEAN}, an
	 * 	{@link java.time.OffsetDateTime}, {@link java.time.LocalDateTime} or {@link java.time.LocalDate} for
	 * 	{@code TIMESTAMP}, and the already-{@code $$}-decoded {@link String} for {@code TEXT}, {@code ID}, {@code ENUM}
	 * 	and {@code VERSION}.  Must not be <jk>null</jk>.
	 * @param quoted Whether the literal was quoted in the search string; a quoted literal is never given
	 * 	wildcard interpretation, even if it contains {@code *} or {@code ?}.
	 * @return The predicate fragment.
	 */
	SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted);

	/**
	 * The renderer for a built-in leaf operator (for example {@code "$eq"}), or <jk>null</jk> if this dialect cannot
	 * express it.
	 *
	 * <p>
	 * The context calls this only for built-in leaves; it handles the {@code $and}/{@code $or}/{@code $not} combinators
	 * itself.  A <jk>null</jk> return makes the context fail with a clear error rather than emit wrong SQL.
	 *
	 * @param operatorName The built-in operator name, including the leading {@code $}.  Must not be <jk>null</jk>.
	 * @return The renderer, or <jk>null</jk> if unsupported.
	 */
	SearchSqlRenderer builtinRenderer(String operatorName);

	/**
	 * Whether this dialect orders {@link SearchType#VERSION} columns as dotted integer tuples, like the in-memory engine
	 * (so {@code 9.10} is greater than {@code 9.9}).
	 *
	 * <p>
	 * When <jk>false</jk> (the default), a range or ordering operator ({@code $gt}, {@code $gte}, {@code $lt},
	 * {@code $lte}, {@code $between}) on a {@code VERSION} column is rejected with
	 * {@link org.apache.juneau.commons.beanquery.BeanQuerySyntaxException.Code#BAD_VALUE BAD_VALUE} instead of silently comparing text (where {@code 9.10}
	 * sorts before {@code 9.9}).  Equality, {@code $in}, {@code $ne} and {@code $prefix} are not affected.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// A dialect that compares versions as integer tuples opts in.</jc>
	 * 	<ja>@Override</ja> <jk>public boolean</jk> supportsVersionOrdering() { <jk>return true</jk>; }
	 * </p>
	 *
	 * @return <jk>true</jk> if version range comparisons match the in-memory semantics.
	 */
	default boolean supportsVersionOrdering() {
		return false;
	}

	/**
	 * Renders the pagination clause appended to a {@code SELECT} (for example {@code " LIMIT 10 OFFSET 20"}).
	 *
	 * <p>
	 * The limit/offset are inlined as integer literals (never bind values), so the returned text carries no
	 * placeholders.
	 *
	 * @param limit The maximum row count, or <jk>null</jk> for no limit.
	 * @param offset The zero-based start offset, or <jk>null</jk>/{@code 0} for none.
	 * @return The pagination clause (possibly empty, never <jk>null</jk>).
	 */
	String renderLimitOffset(Long limit, Long offset);
}
