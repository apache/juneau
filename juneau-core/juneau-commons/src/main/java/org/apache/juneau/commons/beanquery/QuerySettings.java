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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.util.*;

/**
 * The mutable settings shared by {@link BeanQueryContext.Builder} and {@link BeanQuerySession.Builder}.
 *
 * <p>
 * Builders mutate their own copy.  A built context or session holds a private copy that is never mutated again, so
 * the built objects are immutable.  Package-private: only the base classes and {@link QueryResolver} read the fields.
 */
final class QuerySettings {

	/** Longest {@code $regex} pattern accepted (spec D7). */
	static final int MAX_REGEX_LENGTH = 256;

	/** Declared columns in declaration order, each with its value type.  The allow-list for search, sort and view. */
	final LinkedHashMap<String,SearchType> columns = new LinkedHashMap<>();

	/** Per-column operator sets that replace {@link #defaultOperators} for that column. */
	final LinkedHashMap<String,SearchOperatorSet> columnOperators = new LinkedHashMap<>();

	SearchOperatorSet defaultOperators = SearchOperatorSet.standard();
	boolean allowRegex = true;
	Duration regexTimeout = Duration.ofMillis(50);
	CountPolicy countPolicy = CountPolicy.IF_REQUESTED;
	int defaultLimit = 100;
	Integer maxLimit = 1000;
	int maxSearchClauses = 64;
	int maxSortKeys = 8;
	int maxSearchLength = 4096;
	int maxExpressionDepth = 16;

	/**
	 * Returns an independent copy (maps copied; operator sets are immutable and shared).
	 *
	 * @return A new settings object.
	 */
	QuerySettings copy() {
		var s = new QuerySettings();
		s.columns.putAll(columns);
		s.columnOperators.putAll(columnOperators);
		s.defaultOperators = defaultOperators;
		s.allowRegex = allowRegex;
		s.regexTimeout = regexTimeout;
		s.countPolicy = countPolicy;
		s.defaultLimit = defaultLimit;
		s.maxLimit = maxLimit;
		s.maxSearchClauses = maxSearchClauses;
		s.maxSortKeys = maxSortKeys;
		s.maxSearchLength = maxSearchLength;
		s.maxExpressionDepth = maxExpressionDepth;
		return s;
	}

	/**
	 * Returns the operator set used to parse the specified column's expressions (including {@code $regex}; the regex
	 * gate is a separate §5 check so it can report its own message).
	 *
	 * @param column The column name.
	 * @return The column's set, else the default set.
	 */
	SearchOperatorSet operators(String column) {
		return columnOperators.getOrDefault(column, defaultOperators);
	}

	/**
	 * Checks the cross-setting rules that single setters cannot check.
	 *
	 * @throws IllegalStateException If no column is declared, or {@code maxLimit} is below {@code defaultLimit}.
	 */
	void validate() {
		if (columns.isEmpty())
			throw isex("At least one column must be declared.");
		if (maxLimit != null && maxLimit < defaultLimit)
			throw isex("maxLimit (%s) must not be less than defaultLimit (%s).", maxLimit, defaultLimit);
	}

	static int positive(String name, int value) {
		req(value > 0, "%s must be positive: %s", name, value);
		return value;
	}

	static Integer positiveOrNull(String name, Integer value) {
		req(value == null || value > 0, "%s must be positive or null: %s", name, value);
		return value;
	}

	static Duration positive(String name, Duration value) {
		reqnn(name, value);
		req(! value.isNegative() && ! value.isZero(), "%s must be positive: %s", name, value);
		return value;
	}
}
