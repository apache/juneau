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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * A parameterized SQL predicate fragment: a boolean SQL expression plus the ordered bind values its {@code ?}
 * placeholders consume.
 *
 * <p>
 * A {@link SearchSqlRenderer} for a custom operator returns one of these; the SQL context splices the {@link #sql()}
 * into the assembled {@code WHERE} clause and appends the {@link #binds()} to the prepared-statement parameter list in
 * order.  Values are always bound as parameters (never string-concatenated) so a custom operator cannot open an
 * injection hole.
 *
 * @since 10.0.0
 */
public final class SqlFragment {

	private final String sql;
	private final List<Object> binds;

	private SqlFragment(String sql, List<Object> binds) {
		this.sql = sql;
		this.binds = binds;
	}

	/**
	 * Creates a fragment.
	 *
	 * @param sql The boolean SQL expression, with {@code ?} placeholders for each bind value.  Must not be
	 * 	<jk>null</jk> or blank.
	 * @param binds The bind values, in placeholder order.  May be empty; must not be <jk>null</jk>.
	 * @return A new fragment.
	 */
	public static SqlFragment of(String sql, List<Object> binds) {
		req(inb(sql), "SqlFragment sql must not be null or blank.");
		req(binds != null, "SqlFragment binds must not be null (use an empty list).");
		return new SqlFragment(sql, List.copyOf(binds));
	}

	/**
	 * Creates a fragment from a varargs bind list.
	 *
	 * @param sql The boolean SQL expression, with {@code ?} placeholders for each bind value.  Must not be
	 * 	<jk>null</jk> or blank.
	 * @param binds The bind values, in placeholder order.
	 * @return A new fragment.
	 */
	public static SqlFragment of(String sql, Object...binds) {
		return of(sql, Arrays.asList(binds));
	}

	/**
	 * The boolean SQL expression.
	 *
	 * @return The SQL, never <jk>null</jk> or blank.
	 */
	public String sql() {
		return sql;
	}

	/**
	 * The bind values in placeholder order.
	 *
	 * @return An unmodifiable list of bind values, never <jk>null</jk>.
	 */
	public List<Object> binds() {
		return binds;
	}

	@Override /* Object */
	public String toString() {
		return sql + " " + binds;
	}
}
