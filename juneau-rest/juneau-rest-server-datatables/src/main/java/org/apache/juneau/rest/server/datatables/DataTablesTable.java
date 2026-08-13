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
package org.apache.juneau.rest.server.datatables;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;

import java.util.*;

import org.apache.juneau.bean.html5.Table;
import org.apache.juneau.marshall.*;

/**
 * Builds a DataTables-ready HTML {@link Table} DOM bean from a collection of rows &mdash; a server-rendered "plain
 * table" that the browser upgrades in place with {@code $('#id').DataTable()}.
 *
 * <p>
 * The generated table carries:
 * <ul class='spaced-list'>
 * 	<li>a stable {@code id} attribute (the selector the browser initializer targets),
 * 	<li>a {@code data-juneau-datatable} marker attribute (the hook the shipped {@code juneau-datatables.js} glue
 * 		auto-initializes &mdash; see {@link DataTablesMixin}),
 * 	<li>a {@code <thead>} of column titles (from {@link DataTablesColumns}), and
 * 	<li>a {@code <tbody>} of one {@code <tr>} per row, with cells pulled positionally from the column {@code data} keys.
 * </ul>
 *
 * <p>
 * Because it returns a Juneau {@link Table} DOM bean (not a raw HTML string), the markup is produced by the normal
 * {@code HtmlSerializer} with its usual escaping &mdash; there's no bespoke string concatenation to get wrong.  This
 * helper is intentionally scoped to the {@code juneau-rest-server-datatables} module so the marshall core and
 * {@code HtmlDocSerializer} stay decoupled from any DataTables-specific rendering.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Server-render the rows; the browser calls $('#releases').DataTable() to upgrade it.</jc>
 * 	Table <jv>t</jv> = DataTablesTable.<jsm>of</jsm>(<js>"releases"</js>, <jv>releases</jv>, Release.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DataTablesColumns}
 * 	<li class='jc'>{@link DataTablesMixin}
 * 	<li class='link'><a class="doclink" href="https://datatables.net/examples/data_sources/dom">DataTables from a pre-rendered DOM table</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class DataTablesTable {

	/** Marker attribute the shipped {@code juneau-datatables.js} glue looks for to auto-initialize a table. */
	public static final String MARKER_ATTR = "data-juneau-datatable";

	private DataTablesTable() {}

	/**
	 * Builds a DataTables-ready table, deriving the columns from the specified row bean type via
	 * {@link DataTablesColumns#of(Class)}.
	 *
	 * @param id The table's DOM {@code id} (the {@code $('#id')} selector).  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Must not be <jk>null</jk>; may be empty.
	 * @param rowType The row bean type used to derive the columns.  Must not be <jk>null</jk> and must be a bean.
	 * @return A new {@link Table} DOM bean.
	 */
	public static Table of(String id, Collection<?> rows, Class<?> rowType) {
		return of(MarshallingContext.DEFAULT, id, rows, rowType);
	}

	/**
	 * Builds a DataTables-ready table using the specified marshalling context, deriving the columns from the specified
	 * row bean type via {@link DataTablesColumns#of(MarshallingContext, Class)}.
	 *
	 * <p>
	 * Using one context for both column derivation and cell reads keeps the {@code data} keys and the row values
	 * consistent when a resource applies custom bean settings.
	 *
	 * @param ctx The marshalling context supplying bean metadata (property naming/ordering) and cell reads.  Must not be <jk>null</jk>.
	 * @param id The table's DOM {@code id} (the {@code $('#id')} selector).  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Must not be <jk>null</jk>; may be empty.
	 * @param rowType The row bean type used to derive the columns.  Must not be <jk>null</jk> and must be a bean.
	 * @return A new {@link Table} DOM bean.
	 */
	public static Table of(MarshallingContext ctx, String id, Collection<?> rows, Class<?> rowType) {
		return of(ctx, id, rows, DataTablesColumns.of(ctx, rowType));
	}

	/**
	 * Builds a DataTables-ready table from an explicit column list (e.g. one produced by {@link DataTablesColumns} and
	 * then adjusted).
	 *
	 * @param id The table's DOM {@code id} (the {@code $('#id')} selector).  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Must not be <jk>null</jk>; may be empty.
	 * @param columns The column descriptors (each carrying at least {@code data} and {@code title}).  Must not be
	 * 	<jk>null</jk>.
	 * @return A new {@link Table} DOM bean.
	 */
	public static Table of(String id, Collection<?> rows, List<Map<String,Object>> columns) {
		return of(MarshallingContext.DEFAULT, id, rows, columns);
	}

	/**
	 * Builds a DataTables-ready table from an explicit column list using the specified marshalling context for
	 * bean-property cell reads.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param id The table's DOM {@code id} (the {@code $('#id')} selector).  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Must not be <jk>null</jk>; may be empty.
	 * @param columns The column descriptors (each carrying at least {@code data} and {@code title}).  Must not be
	 * 	<jk>null</jk>.
	 * @return A new {@link Table} DOM bean.
	 */
	public static Table of(MarshallingContext ctx, String id, Collection<?> rows, List<Map<String,Object>> columns) {
		var headerCells = new ArrayList<>(columns.size());
		for (var col : columns)
			headerCells.add(th(String.valueOf(col.get("title"))));

		var bodyRows = new ArrayList<>(rows.size());
		for (var row : rows) {
			var cells = new ArrayList<>(columns.size());
			for (var col : columns) {
				var v = value(ctx, row, String.valueOf(col.get("data")));
				cells.add(td(v == null ? "" : v));
			}
			bodyRows.add(tr(cells.toArray()));
		}

		return table(thead(tr(headerCells.toArray())), tbody(bodyRows.toArray()))
			.id(id)
			.attr(MARKER_ATTR, "");
	}

	/** Reads a column value from a row: a direct key lookup for a {@code Map}, a bean-property read otherwise. */
	private static Object value(MarshallingContext ctx, Object row, String key) {
		if (row instanceof Map<?,?> m)
			return m.get(key);
		return ctx.toBeanMap(row).get(key);
	}
}
