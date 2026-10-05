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

import java.util.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * The SQL callback an application supplies for a <b>custom</b> search operator (design §5.3) so a SQL context can
 * render it into a {@code WHERE} clause.
 *
 * <p>
 * The renderer receives the target {@link SqlDialect}, the already-quoted column reference, the column's
 * {@link SearchType value type} (so it can bind values as the right JDBC type), and the operator's arguments (typed
 * values when the operator asks for them, otherwise the literal strings; see {@link org.apache.juneau.commons.beanquery.SearchOperator.Builder#typedArgs(boolean)}),
 * and returns a {@link SqlFragment} (a boolean SQL expression plus its ordered bind values).  It is the
 * SQL-side analog of {@link SearchPredicate} (the in-memory evaluator): built-in operators do not use it &mdash; their
 * SQL comes from {@link SqlDialect#builtinRenderer(String)}.
 *
 * <p>
 * A renderer is attached to a {@link SearchOperator} as a typed extension keyed by this interface, with
 * {@link org.apache.juneau.commons.beanquery.SearchOperator.Builder#extension(Class, Object)
 * extension(SearchSqlRenderer.class, renderer)}; the context retrieves it with {@link SearchOperator#extension(Class)},
 * so commons never depends on SQL dialect types.  A
 * renderer that does not support the supplied dialect returns <jk>null</jk>; the context then fails with a clear error
 * rather than emitting wrong SQL.  Values must be returned as {@link SqlFragment} binds, never concatenated into the
 * SQL text.
 *
 * <p>
 * For the built-in value operators, and for a custom operator built with {@code typedArgs(true)}, each argument arrives
 * already parsed by {@link SearchType#parse(String)} during query resolution: a {@link java.math.BigDecimal} for a
 * {@code NUMERIC} column, a {@link Boolean} for {@code BOOLEAN}, and an {@link java.time.OffsetDateTime},
 * {@link java.time.LocalDateTime} or {@link java.time.LocalDate} for {@code TIMESTAMP}.  Text-like types
 * ({@code TEXT}, {@code ID}, {@code ENUM}, {@code VERSION}) are always {@link String}.  A malformed value never reaches a
 * renderer; it fails resolution with code {@code BAD_VALUE}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A custom "$even" operator rendered as a modulo check (one bind-free fragment).</jc>
 * 	SearchSqlRenderer <jv>renderer</jv> = (<jv>dialect</jv>, <jv>col</jv>, <jv>type</jv>, <jv>args</jv>) -&gt;
 * 		SqlFragment.<jsm>of</jsm>(<js>"("</js> + <jv>col</jv> + <js>" % 2 = 0)"</js>);
 *
 * 	<jc>// A typed-args operator: args.get(0) is a BigDecimal on a NUMERIC column, bound as-is.</jc>
 * 	SearchSqlRenderer <jv>divisibleBy</jv> = (<jv>dialect</jv>, <jv>col</jv>, <jv>type</jv>, <jv>args</jv>) -&gt;
 * 		SqlFragment.<jsm>of</jsm>(<js>"("</js> + <jv>col</jv> + <js>" % ? = 0)"</js>, <jv>args</jv>.get(0));
 *
 * 	SearchOperator <jv>even</jv> = SearchOperator
 * 		.<jsm>create</jsm>(<js>"$even"</js>, <js>"Matches even numbers. Example: $even()"</js>)
 * 		.minArgs(0).maxArgs(0)
 * 		.extension(SearchSqlRenderer.<jk>class</jk>, <jv>renderer</jv>)
 * 		.build();
 * </p>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface SearchSqlRenderer {

	/**
	 * Renders this custom operator into a SQL predicate fragment.
	 *
	 * @param dialect The target SQL dialect.  Never <jk>null</jk>.
	 * @param columnSql The dialect-quoted column reference the predicate applies to (for example {@code "\"age\""}).
	 * 	Never <jk>null</jk>.
	 * @param type The column's value type, so the renderer can bind values as the right JDBC type.  Never <jk>null</jk>.
	 * @param args The operator's arguments (never <jk>null</jk>; may be empty): {@link SearchExpression#typedArgs() typed
	 * 	values} ({@link java.math.BigDecimal}, {@link Boolean}, {@link java.time.OffsetDateTime},
	 * 	{@link java.time.LocalDateTime}, {@link java.time.LocalDate}, or {@link String} for text-like types) for a built-in
	 * 	value operator or a custom operator with {@code typedArgs(true)}; plain {@link String} literals otherwise.
	 * @return The predicate fragment, or <jk>null</jk> if this renderer does not support {@code dialect}.
	 */
	SqlFragment render(SqlDialect dialect, String columnSql, SearchType type, List<Object> args);
}
