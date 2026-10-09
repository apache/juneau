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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * A datatable view definition &mdash; an ordered set of {@link Column} definitions carrying each column's search
 * configuration (design §4.3).  This is the bean the {@code VIEW_META} search block is serialized from.
 *
 * <p>
 * A view may also carry a table-level {@link SearchOperatorSet} (design §4.3): it is inherited by every column that
 * does not supply its own set (see {@link Column#effectiveOperators(SearchOperatorSet)} and
 * {@link Column#searchMeta(SearchOperatorSet)}).  A column that supplies its own set entirely replaces the
 * table-level set for that column.
 *
 * <h5 class='section'>Mutability</h5>
 * <p>
 * Deliberately mutable, like {@link Column} and {@link RegionDef}: a {@link ViewDef} is a server-side authoring
 * bean built up once via fluent setters and {@link #column(Column)} calls while constructing a view (design
 * §4.1&ndash;§4.3), not an immutable value object.  Callers must not mutate one concurrently from more than one
 * thread, and must not reuse one across requests unless every request treats it as read-only after construction.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A view with a table-level $eq-only default, and one column ("status") that replaces it with $in-only.</jc>
 * 	ViewDef <jv>view</jv> = ViewDef.<jsm>create</jsm>(<js>"releases"</js>)
 * 		.operators(SearchOperatorSet.<jsm>of</jsm>(SearchOperatorSet.<jsm>standard</jsm>().get(<js>"$eq"</js>)))
 * 		.column(Column.<jsm>create</jsm>(<js>"name"</js>).label(<js>"Name"</js>).searchType(SearchType.<jsf>TEXT</jsf>))
 * 		.column(Column.<jsm>create</jsm>(<js>"status"</js>).label(<js>"Status"</js>).searchType(SearchType.<jsf>ID</jsf>)
 * 			.operators(SearchOperatorSet.<jsm>of</jsm>(SearchOperatorSet.<jsm>standard</jsm>().get(<js>"$in"</js>))));
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Column}
 * </ul>
 *
 * @since 10.0.0
 */
public final class ViewDef {

	private final String id;
	private final Map<String,Column> columns = new LinkedHashMap<>();
	private SearchOperatorSet operators;

	private ViewDef(String id) {
		this.id = id;
	}

	/**
	 * Starts a new view definition.
	 *
	 * @param id The view id (matches the datatable's id on the page).  Must not be <jk>null</jk> or blank.
	 * @return A new, empty view definition.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank.
	 */
	public static ViewDef create(String id) {
		req(inb(id), "ViewDef id must not be null or blank.");
		return new ViewDef(id);
	}

	/**
	 * Adds a column definition, in order.
	 *
	 * @param value The column.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public ViewDef column(Column value) {
		req(value != null, "ViewDef '%s' column must not be null.", id);
		columns.put(value.name(), value);
		return this;
	}

	/**
	 * Sets the table-level {@link SearchOperatorSet} (design §4.3) &mdash; inherited by every column that does not
	 * supply its own set.
	 *
	 * @param value The operator set.  Can be <jk>null</jk> to clear it (columns without their own set then fall
	 * 	back to {@link SearchOperatorSet#standard()}).
	 * @return This object.
	 */
	public ViewDef operators(SearchOperatorSet value) {
		operators = value;
		return this;
	}

	/** @return The view id. */
	public String id() {
		return id;
	}

	/**
	 * The column definitions, in insertion order.
	 *
	 * @return An unmodifiable list of the columns.
	 */
	public List<Column> columns() {
		return List.copyOf(columns.values());
	}

	/**
	 * Looks up a column by name.
	 *
	 * @param name The column name.  Can be <jk>null</jk>.
	 * @return The column, or <jk>null</jk> if no column has that name.
	 */
	public Column findColumn(String name) {
		return name == null ? null : columns.get(name);
	}

	/**
	 * This view's table-level operator set, or <jk>null</jk> if absent (columns without their own set fall back to
	 * {@link SearchOperatorSet#standard()}).
	 *
	 * @return The operator set, or <jk>null</jk>.
	 */
	public SearchOperatorSet operators() {
		return operators;
	}
}
