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

import java.util.function.*;

/**
 * Evaluates a {@link Filter} tree against one row in memory, delegating each leaf to the existing
 * {@code InMemoryMatch.matches(SearchExpression, Object, SearchType, RegexBudget)} and recursing across
 * {@code $and}/{@code $or} (short-circuiting) and {@code $not} (negating) groups. {@code InMemoryBeanQuerySession}
 * is expected to apply its row guard before calling this (guard first, cheaper and failure-safe-by-default), then
 * call this once per row with a column-value accessor appropriate to the row type (a bean-property accessor or a
 * {@code Map} lookup), reusing one {@link RegexBudget} across every row/column in the call.
 */
final class FilterEvaluator {

	private FilterEvaluator() {}

	/**
	 * Tests whether one row matches {@code filter}.
	 *
	 * @param filter The resolved filter.  <jk>null</jk> is treated as an empty filter (matches every row) as a
	 * 	defensive convenience, though {@link FilterResolver#resolve} and {@link QueryResolver} never produce
	 * 	<jk>null</jk> &mdash; an empty search resolves to an empty {@link Filter.And} instead.
	 * @param cellAccessor Looks up the row's value for a column name.
	 * @param budget One deadline shared across the whole row-matching call (not reset per leaf) — construct once
	 * 	per query call and reuse for every row/column it checks.
	 * @return <jk>true</jk> if the row matches.
	 */
	static boolean matches(Filter filter, Function<String,Object> cellAccessor, RegexBudget budget) {
		if (filter == null)
			return true;
		if (filter instanceof Filter.Leaf leaf)
			return InMemoryMatch.matches(leaf.expression(), cellAccessor.apply(leaf.column()), leaf.type(), budget);
		if (filter instanceof Filter.Not not)
			return ! matches(not.item(), cellAccessor, budget);
		if (filter instanceof Filter.And and) {
			for (var item : and.items())
				if (! matches(item, cellAccessor, budget))
					return false;
			return true;
		}
		if (filter instanceof Filter.Or or) {
			for (var item : or.items())
				if (matches(item, cellAccessor, budget))
					return true;
			return false;
		}
		throw new IllegalStateException("Unknown Filter type: " + filter);
	}
}
