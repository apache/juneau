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

/**
 * A fully-rendered SQL statement: the text (with {@code ?} placeholders) plus its ordered bind values, and the
 * selected column names in {@code SELECT} order.
 *
 * <p>
 * {@link SqlBeanQueryContext} renders these purely (no {@code DataSource} needed), so callers &mdash; and tests
 * &mdash; can inspect the generated SQL and binds before any execution.
 *
 * @since 10.0.0
 */
public final class SqlStatement {

	private final String sql;
	private final List<Object> binds;
	private final List<String> columns;
	private final boolean hasRegex;

	SqlStatement(String sql, List<Object> binds, List<String> columns, boolean hasRegex) {
		this.sql = sql;
		this.binds = List.copyOf(binds);
		this.columns = List.copyOf(columns);
		this.hasRegex = hasRegex;
	}

	// Package-private: lets SqlBeanQuerySession.prepare() apply a bounded default statement timeout when this
	// statement's search includes $regex and no explicit queryTimeout was configured.
	boolean hasRegex() {
		return hasRegex;
	}

	/**
	 * The SQL text, with a {@code ?} placeholder for each bind value.
	 *
	 * @return The SQL text.
	 */
	public String sql() {
		return sql;
	}

	/**
	 * The bind values in placeholder order.
	 *
	 * @return An unmodifiable list of bind values.
	 */
	public List<Object> binds() {
		return binds;
	}

	/**
	 * The selected column names in {@code SELECT} order (empty for a {@code COUNT} statement).
	 *
	 * @return An unmodifiable list of column names.
	 */
	public List<String> columns() {
		return columns;
	}

	@Override /* Object */
	public String toString() {
		return sql + " " + binds;
	}
}
