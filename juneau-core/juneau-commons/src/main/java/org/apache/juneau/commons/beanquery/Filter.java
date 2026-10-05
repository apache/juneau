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

import java.util.*;

/**
 * A resolved, engine-neutral search tree (design §7) &mdash; the {@code ResolvedQuery.filter()} replacement for
 * the earlier flat {@code Term}/{@code terms()} list.
 *
 * <p>
 * {@link QueryResolver} builds this tree from a request's raw {@code search} string: it parses the string with
 * {@link SearchParser} into a {@link SearchItem} tree, then resolves every leaf's column (against the context's
 * allow-list) and expression (with {@link SearchExpressionParser}, using that column's operator set), carrying the
 * column's {@link SearchType} along for the engine to use. An empty search resolves to an empty {@link Filter.And}
 * (design §7) &mdash; not <jk>null</jk> &mdash; so every engine can treat "no search" as just another tree to
 * evaluate, one that matches every row.
 *
 * <p>
 * Both engines walk this same shape: {@link InMemoryBeanQuerySession} evaluates it recursively against one row;
 * {@code SqlSearchCompiler} (not linkable from here &mdash; it lives in the separate {@code juneau-beanquery-sql}
 * module, which this one does not depend on) renders it to a parenthesized SQL boolean expression. Neither engine
 * ever sees a raw request string or an unresolved column name.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// ageExpr/nameExpr/emailExpr are already-resolved SearchExpressions, e.g. from SearchExpressionParser.parse()</jc>
 * 	<jc>// (age &gt; 21) AND (name CONTAINS "bob" OR email CONTAINS "bob")</jc>
 * 	Filter <jv>f</jv> = <jk>new</jk> Filter.And(List.<jsm>of</jsm>(
 * 		<jk>new</jk> Filter.Leaf(<js>"age"</js>, ageExpr, SearchType.<jsf>NUMERIC</jsf>),
 * 		<jk>new</jk> Filter.Or(List.<jsm>of</jsm>(
 * 			<jk>new</jk> Filter.Leaf(<js>"name"</js>, nameExpr, SearchType.<jsf>TEXT</jsf>),
 * 			<jk>new</jk> Filter.Leaf(<js>"email"</js>, emailExpr, SearchType.<jsf>TEXT</jsf>)))));
 * </p>
 *
 * <p>
 * Unlike the package-private {@link SearchItem} it mirrors, {@code Filter} is public: {@code SqlSearchCompiler}
 * (above) lives in {@code juneau-beanquery-sql}, a separate module downstream of this one, and must be able to
 * pattern-match over this tree directly.
 *
 * @since 10.0.0
 */
public sealed interface Filter {

	/**
	 * One resolved {@code column=expression} leaf, with the column's {@link SearchType} carried alongside for the
	 * engine.
	 *
	 * <p>
	 * Note: {@link SearchExpression} has no value-based {@code equals}/{@code hashCode} of its own, so two
	 * {@code Leaf} instances built from separately-parsed-but-identical expression text are <b>not</b>
	 * {@link #equals(Object) equal} unless they share the same {@code expression} instance.
	 */
	record Leaf(String column, SearchExpression expression, SearchType type) implements Filter {}

	/** An AND of two or more (or, at the top level, zero) sub-filters.  An empty {@code And} matches every row. */
	record And(List<Filter> items) implements Filter {
		public And(List<Filter> items) {
			this.items = List.copyOf(items);
		}
	}

	/** An OR of two or more sub-filters. */
	record Or(List<Filter> items) implements Filter {
		public Or(List<Filter> items) {
			this.items = List.copyOf(items);
		}
	}

	/** The negation of exactly one sub-filter. */
	record Not(Filter item) implements Filter {}
}
