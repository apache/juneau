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

/** A minimal H2-compatible dialect for this module's tests. */
final class TestDialect implements SqlDialect {

	static final TestDialect INSTANCE = new TestDialect();

	@Override /* SqlDialect */
	public String id() {
		return "test";
	}

	@Override /* SqlDialect */
	public String quote(String identifier) {
		return '"' + identifier.replace("\"", "\"\"") + '"';
	}

	@Override /* SqlDialect */
	public SqlFragment renderBare(String columnSql, SearchType type, Object value, boolean quoted) {
		if (type == SearchType.NUMERIC)
			return SqlFragment.of(columnSql + " = ?", value);
		return SqlFragment.of("LOWER(" + columnSql + ") LIKE ?", "%" + String.valueOf(value).toLowerCase(Locale.ROOT) + "%");
	}

	@Override /* SqlDialect */
	public SearchSqlRenderer builtinRenderer(String operatorName) {
		switch (operatorName) {
			case "$eq": return (d, c, t, a) -> SqlFragment.of(c + " = ?", a.get(0));
			case "$gt": return (d, c, t, a) -> SqlFragment.of(c + " > ?", a.get(0));
			case "$lt": return (d, c, t, a) -> SqlFragment.of(c + " < ?", a.get(0));
			case "$regex": return (d, c, t, a) -> SqlFragment.of(c + " REGEXP ?", a.get(0));
			default:    return null;
		}
	}

	@Override /* SqlDialect */
	public String renderLimitOffset(Long limit, Long offset) {
		var sb = new StringBuilder();
		if (limit != null)
			sb.append(" LIMIT ").append(limit);
		if (offset != null && offset > 0)
			sb.append(" OFFSET ").append(offset);
		return sb.toString();
	}
}
