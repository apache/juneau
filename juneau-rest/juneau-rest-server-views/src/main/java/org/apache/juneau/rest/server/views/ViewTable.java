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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;

/**
 * Builds the HTML delivery shell for a {@link ViewDef} &mdash; the {@code data-juneau-view} table plus the
 * {@code <script type="application/json">} VIEW_META sidecar the {@code juneau-views.js} runtime consumes (design
 * doc §6.1).
 *
 * <p>
 * Mirrors the sibling {@link org.apache.juneau.rest.server.datatables.DataTablesTable DataTablesTable} pattern, but
 * emits the <b>distinct</b> {@link #MARKER_ATTR data-juneau-view} marker (never {@code data-juneau-datatable}) so
 * the VIEW_META path and the plain-DataTables path never collide on the same node.  The returned {@link Div} carries:
 * <ul class='spaced-list'>
 * 	<li>a {@code <table>} with a stable {@code id} (the {@code $('#id')} selector) + the {@code data-juneau-view="<id>"}
 * 		marker and a {@code <thead>} of column titles, and
 * 	<li>a sibling {@code <script type="application/json" id="juneau-view:<id>">} sidecar carrying the serialized
 * 		{@link ViewDef}.
 * </ul>
 *
 * <p>
 * When {@code rows} are supplied (client-side datasets), they are rendered into a {@code <tbody>} up front; in the
 * server-side dogfood path {@code rows} is omitted and the rows arrive via ajax draws against
 * {@link ViewDef#dataUrl}.
 *
 * <h5 class='section'>Escaping contract (security-critical &mdash; design doc §6.1):</h5>
 * <p>
 * The VIEW_META JSON is emitted as the text content of a {@code <script type="application/json">} element.  Per the
 * HTML spec such content is <b>raw text</b> (HTML entities are NOT decoded inside it) and must not contain the
 * substring {@code </} (nor {@code <!--}), or it would prematurely terminate the element.  This emitter therefore
 * hands the serialized JSON to {@link StringUtils#escapeForScript(String)} <b>before</b> insertion, which
 * neutralizes {@code </script>},
 * {@code <script}, and {@code <!--} break-outs while keeping the payload valid, round-trippable JSON.  That method
 * is the single, shared, publicly reusable implementation &mdash; see its javadoc for the exact vectors covered, and
 * reuse it rather than hand-rolling an escaper for your own sidecar.  The JSON is inserted as verbatim raw content
 * (via {@link org.apache.juneau.bean.html5.HtmlBuilder#rawText(String) rawText}) so Juneau's normal XML/HTML text
 * entity-encoding does not corrupt the {@code application/json} payload (it would otherwise turn {@code &}/{@code >}
 * into {@code &amp;}/{@code &gt;}, which browsers do NOT decode inside a raw-text {@code <script>}).  Because
 * {@code rawText} is backed by a {@code String} (not a one-shot {@link java.io.Reader}), the returned bean is fully
 * re-serializable &mdash; it survives a serialize&rarr;object&rarr;serialize cycle and the full {@code HtmlDoc} page
 * path.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link org.apache.juneau.rest.server.datatables.DataTablesTable}
 * </ul>
 *
 * @since 10.0.0
 */
public class ViewTable {

	/** Marker attribute the {@code juneau-views.js} runtime looks for to auto-initialize a view table. */
	public static final String MARKER_ATTR = "data-juneau-view";

	/** Prefix of the sidecar {@code <script>} element id: {@code juneau-view:<viewId>}. */
	public static final String SIDECAR_ID_PREFIX = "juneau-view:";

	private ViewTable() {}

	/**
	 * Builds the view-table shell for a server-side view (no up-front rows), using the default marshalling context.
	 *
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	public static Div of(ViewDef viewDef) {
		return of(MarshallingContext.DEFAULT, viewDef, null);
	}

	/**
	 * Builds the view-table shell and renders {@code rows} into the {@code <tbody>} (client-side datasets), using the
	 * default marshalling context.
	 *
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, and the
	 * 	JSON sidecar.
	 */
	public static Div of(ViewDef viewDef, Collection<?> rows) {
		return of(MarshallingContext.DEFAULT, viewDef, rows);
	}

	/**
	 * Builds the view-table shell using the specified marshalling context for bean-property cell reads.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, and the
	 * 	JSON sidecar.
	 */
	public static Div of(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows) {
		var id = viewDef.id;
		var cols = viewDef.columns == null ? List.<Column>of() : viewDef.columns;

		// <thead> of column titles (falling back to the data key when no title was set).
		var headerCells = new ArrayList<>(cols.size());
		for (var c : cols)
			headerCells.add(th(c.title == null ? c.data : c.title));

		var tableChildren = new ArrayList<>();
		tableChildren.add(thead(tr(headerCells.toArray())));

		if (rows != null) {
			var bodyRows = new ArrayList<>(rows.size());
			for (var row : rows) {
				var cells = new ArrayList<>(cols.size());
				for (var c : cols) {
					var v = value(ctx, row, c.data);
					cells.add(td(v == null ? "" : v));
				}
				bodyRows.add(tr(cells.toArray()));
			}
			tableChildren.add(tbody(bodyRows.toArray()));
		}

		var table = table(tableChildren.toArray()).id(id).attr(MARKER_ATTR, id);

		// Sidecar: serialize the VIEW_META, neutralize script break-outs, then insert as RAW content (class javadoc).
		var json = escapeForScript(Json.of(viewDef));
		var sidecar = script().type("application/json").id(SIDECAR_ID_PREFIX + id).text(rawText(json));

		return div(table, sidecar);
	}

	/** Reads a column value from a row: a direct key lookup for a {@code Map}, a bean-property read otherwise. */
	private static Object value(MarshallingContext ctx, Object row, String key) {
		if (row instanceof java.util.Map<?,?> m)
			return m.get(key);
		return ctx.toBeanMap(row).get(key);
	}
}
