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
import java.util.regex.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.widgets.Op;

/**
 * Java description of one datatables view: data source, columns, ribbon, row styling, row actions, row detail,
 * selection and bulk actions. It replaces hand-written {@code <@card type="datatables">} JSON5
 * for apps that build pages in Java, and {@link #toCardBody()} serializes to exactly the card body the FTL path
 * produces. All behavior (paging, search, rendering, actions) stays in {@code juneau-views.js}; this type never
 * calls a card-type handler directly — see the class's rendering note on {@link #toCardBody()}.
 *
 * <p>
 * {@link Column#defaultVisible(boolean) Column.defaultVisible(false)} only hides a column when the client's
 * {@code columnConfig} (juneau-config.js) is on.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	TableSpec <jv>t</jv> = TableSpec.<jsm>create</jsm>(<js>"slo"</js>)
 * 		.rowType(SloPod.<jk>class</jk>)
 * 		.dataUrl(<js>"/rest/slo/data"</js>)
 * 		.defaultOrder(<js>"pod"</js>, TableSpec.Sort.<jsf>ASC</jsf>)
 * 		.columns(
 * 			Column.<jsm>create</jsm>(<js>"pod"</js>).label(<js>"Pod"</js>).className(<js>"ssc-id-cell"</js>),
 * 			Column.<jsm>create</jsm>(<js>"incidentKey"</js>).label(<js>"Incident"</js>).render(<js>"linked"</js>).href(<js>"{incidentUrl}"</js>),
 * 			Column.<jsm>create</jsm>(<js>"rootCause"</js>).label(<js>"Root cause"</js>).defaultVisible(<jk>false</jk>))
 * 		.ribbon(RibbonItem.<jsm>refresh</jsm>(), RibbonItem.<jsm>export</jsm>(<js>"copy"</js>, <js>"csv"</js>))
 * 		.rowClass(<js>"analysisState"</js>, Op.<jsf>EQ</jsf>, <js>"degraded"</js>, <js>"row-degraded"</js>)
 * 		.rowActions(RowAction.<jsm>create</jsm>(<js>"analyze"</js>).label(<js>"Analyze pod"</js>)
 * 			.endpoint(<js>"/rest/slo/analyze"</js>).method(RowAction.Method.<jsf>POST</jsf>))
 * 		.detail(RowDetail.<jsm>create</jsm>(<js>"/rest/slo/data/{id}"</js>).title(<js>"{pod}"</js>)
 * 			.region(RegionDef.<jsm>create</jsm>(<js>"slo-detail"</js>).populate(<js>"slo-detail"</js>)));
 * 	<jv>t</jv>.validate();
 * 	Map&lt;String,Object&gt; <jv>body</jv> = <jv>t</jv>.toCardBody();   // the unwrapped catalog
 * </p>
 *
 * @since 10.0.0
 */
public final class TableSpec {

	/** Where a view's rows come from. Wire tokens are lowercase. */
	public enum DataMode {

		/** Paging, sorting and search run client-side, against the full {@code dataUrl} payload. */
		CLIENT("client"),

		/** Paging, sorting and search are delegated to {@code dataUrl} on every draw. */
		SERVER("server");

		private final String wire;

		DataMode(String wire) {
			this.wire = wire;
		}

		/** @return The lowercase wire token. */
		public String wire() {
			return wire;
		}
	}

	/** A default-sort direction. Wire tokens are lowercase. */
	public enum Sort {

		/** Ascending. */
		ASC("asc"),

		/** Descending. */
		DESC("desc");

		private final String wire;

		Sort(String wire) {
			this.wire = wire;
		}

		/** @return The lowercase wire token. */
		public String wire() {
			return wire;
		}
	}

	/** Id grammar shared with cards and nav nodes. Mirrors {@code $defs/id} in {@code juneau-page.schema.json}. */
	private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");

	private final String id;
	private String rowType;
	private String dataUrl;
	private DataMode dataMode;
	private final List<JsonMap> defaultOrder = new ArrayList<>();
	private final List<Column> columns = new ArrayList<>();
	private SearchOperatorSet operators;
	private final List<RibbonItem> ribbon = new ArrayList<>();
	private final List<JsonMap> rowClassRules = new ArrayList<>();
	private final List<RowAction> rowActions = new ArrayList<>();
	private RowDetail detail;
	private SelectionDef selection;
	private BulkMutateDef bulk;
	private Long pollIntervalMs;
	private String quickStatsId;
	private List<QuickStat> quickStats;
	private boolean primary;
	private String cssClass;

	private TableSpec(String id) {
		this.id = id;
	}

	/**
	 * Starts a new table definition.
	 *
	 * @param id The table's card id. Must not be <jk>null</jk> or blank, and must match
	 * 	{@code ^[A-Za-z][A-Za-z0-9_-]{0,63}$}.
	 * @return A new {@link TableSpec}, with no columns or {@code dataUrl} yet set.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk>, blank, or malformed.
	 */
	public static TableSpec create(String id) {
		if (id == null || id.isBlank())
			throw iaex("TableSpec id must not be null or blank.");
		if (! ID_PATTERN.matcher(id).matches())
			throw iaex("TableSpec id '%s' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.", id);
		return new TableSpec(id);
	}

	/**
	 * Sets the row type by its simple name, as {@code TableCatalog} did — documentation metadata only; it has no
	 * effect on rendering or validation.
	 *
	 * @param type The row type. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public TableSpec rowType(Class<?> type) {
		rowType = type == null ? null : type.getSimpleName();
		return this;
	}

	/**
	 * Sets the row type by name directly.
	 *
	 * @param name The row type name. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public TableSpec rowType(String name) {
		rowType = name;
		return this;
	}

	/**
	 * Sets the table's data source URL.
	 *
	 * @param url The URL. Required by {@link #validate()}.
	 * @return This object.
	 */
	public TableSpec dataUrl(String url) {
		dataUrl = url;
		return this;
	}

	/**
	 * Sets whether paging/sorting/search run client-side or against {@code dataUrl} on every draw.
	 *
	 * @param mode The mode. Defaults to {@link DataMode#CLIENT} when never called (omitted from the wire, since
	 * 	{@code juneau-views.js} already defaults to client mode).
	 * @return This object.
	 */
	public TableSpec dataMode(DataMode mode) {
		dataMode = mode;
		return this;
	}

	/**
	 * Appends one default sort column. Repeatable — later calls add secondary sort keys.
	 *
	 * @param column The column name. Must not be <jk>null</jk> or blank.
	 * @param dir The sort direction. Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code column} is <jk>null</jk>/blank, or {@code dir} is <jk>null</jk>.
	 */
	public TableSpec defaultOrder(String column, Sort dir) {
		if (column == null || column.isBlank())
			throw iaex("TableSpec defaultOrder column must not be null or blank.");
		if (dir == null)
			throw iaex("TableSpec defaultOrder direction must not be null.");
		var m = new JsonMap();
		m.put("key", column);
		m.put("dir", dir.wire());
		defaultOrder.add(m);
		return this;
	}

	/**
	 * Appends columns, in display order.
	 *
	 * @param cols The columns. Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code cols} names a {@link Column#name()} already appended by an
	 * 	earlier call.
	 */
	public TableSpec columns(Column...cols) {
		for (var c : cols) {
			for (var existing : columns)
				if (existing.name().equals(c.name()))
					throw iaex("TableSpec '%s' declares column '%s' twice.", id, c.name());
			columns.add(c);
		}
		return this;
	}

	/**
	 * Sets the table-level default {@link SearchOperatorSet}, inherited by any searchable column that declares
	 * none of its own.
	 *
	 * @param table The operator set. Can be <jk>null</jk> to clear it back to absent.
	 * @return This object.
	 */
	public TableSpec operators(SearchOperatorSet table) {
		operators = table;
		return this;
	}

	/**
	 * Appends ribbon (toolbar) items, in display order.
	 *
	 * @param items The items. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public TableSpec ribbon(RibbonItem...items) {
		ribbon.addAll(List.of(items));
		return this;
	}

	/**
	 * Appends a value-based row-class rule ({@code juneau-views.js}'s {@code evaluateRowClassRules}):
	 * a row whose {@code field} is {@link Op#EQ eq}/{@link Op#NE ne} the given value gets {@code cssClass} added
	 * to its {@code <tr>}.
	 *
	 * @param field The row-data field to test. Must not be <jk>null</jk> or blank.
	 * @param op The operator. Must be {@link Op#EQ} or {@link Op#NE}.
	 * @param value The comparison value. Must not be <jk>null</jk>.
	 * @param cssClass The CSS class to add on a match (serialized as the wire key {@code class}). Must not be
	 * 	<jk>null</jk> or blank.
	 * @return This object.
	 * @throws IllegalArgumentException If any argument is missing, or {@code op} does not take a value.
	 */
	public TableSpec rowClass(String field, Op op, Object value, String cssClass) {
		checkRowClassCommon(field, op, cssClass);
		if (! op.requiresValue())
			throw iaex("TableSpec '%s' rowClass on '%s': op '%s' takes no value.", id, field, op.wire());
		if (value == null)
			throw iaex("TableSpec '%s' rowClass on '%s': op '%s' requires a value.", id, field, op.wire());
		var m = new JsonMap();
		m.put("field", field);
		m.put("op", op.wire());
		m.put("value", value);
		m.put("class", cssClass);
		rowClassRules.add(m);
		return this;
	}

	/**
	 * Appends a presence-based row-class rule: a row whose {@code field} is {@link Op#PRESENT present}/
	 * {@link Op#ABSENT absent} gets {@code cssClass} added to its {@code <tr>}.
	 *
	 * @param field The row-data field to test. Must not be <jk>null</jk> or blank.
	 * @param op The operator. Must be {@link Op#PRESENT} or {@link Op#ABSENT}.
	 * @param cssClass The CSS class to add on a match (serialized as the wire key {@code class}). Must not be
	 * 	<jk>null</jk> or blank.
	 * @return This object.
	 * @throws IllegalArgumentException If any argument is missing, or {@code op} requires a value.
	 */
	public TableSpec rowClass(String field, Op op, String cssClass) {
		checkRowClassCommon(field, op, cssClass);
		if (op.requiresValue())
			throw iaex("TableSpec '%s' rowClass on '%s': op '%s' requires a value.", id, field, op.wire());
		var m = new JsonMap();
		m.put("field", field);
		m.put("op", op.wire());
		m.put("class", cssClass);
		rowClassRules.add(m);
		return this;
	}

	private void checkRowClassCommon(String field, Op op, String cssClass) {
		if (field == null || field.isBlank())
			throw iaex("TableSpec '%s' rowClass field must not be null or blank.", id);
		if (op == null)
			throw iaex("TableSpec '%s' rowClass op must not be null.", id);
		if (op.isCollectionOp())
			throw iaex("TableSpec '%s' rowClass on '%s': op '%s' is not supported; use eq, ne, present or absent.", id, field, op.wire());
		if (cssClass == null || cssClass.isBlank())
			throw iaex("TableSpec '%s' rowClass cssClass must not be null or blank.", id);
	}

	/**
	 * Appends row actions, in menu order.
	 *
	 * @param actions The actions. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public TableSpec rowActions(RowAction...actions) {
		rowActions.addAll(List.of(actions));
		return this;
	}

	/**
	 * Appends row actions from a list, in menu order.
	 *
	 * @param actions The actions. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public TableSpec rowActions(List<RowAction> actions) {
		rowActions.addAll(actions);
		return this;
	}

	/**
	 * Sets the per-row detail panel.
	 *
	 * @param value The detail config. Can be <jk>null</jk> to clear it.
	 * @return This object.
	 */
	public TableSpec detail(RowDetail value) {
		detail = value;
		return this;
	}

	/**
	 * Sets the row-selection opt-in.
	 *
	 * @param value The selection config. Can be <jk>null</jk> to clear it.
	 * @return This object.
	 */
	public TableSpec selection(SelectionDef value) {
		selection = value;
		return this;
	}

	/**
	 * Sets the bulk-mutate opt-in. Requires {@link #selection(SelectionDef)} to also be set.
	 *
	 * @param value The bulk config. Can be <jk>null</jk> to clear it.
	 * @return This object.
	 */
	public TableSpec bulk(BulkMutateDef value) {
		bulk = value;
		return this;
	}

	/**
	 * Sets the auto-refresh poll interval.
	 *
	 * @param ms The interval in milliseconds. Must be positive.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code ms} is not positive.
	 */
	public TableSpec pollIntervalMs(long ms) {
		if (ms <= 0)
			throw iaex("TableSpec '%s' pollIntervalMs must be positive.", id);
		pollIntervalMs = ms;
		return this;
	}

	/**
	 * Sets the table's quick-stats strip.
	 *
	 * @param id The strip's own id, serialized as {@code quickStats.id}. Must not be <jk>null</jk> or blank.
	 * @param items The tiles, in display order. Must not be empty.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk>/blank, or {@code items} is empty.
	 */
	public TableSpec quickStats(String id, QuickStat...items) {
		if (id == null || id.isBlank())
			throw iaex("TableSpec quickStats id must not be null or blank.");
		if (items.length == 0)
			throw iaex("TableSpec quickStats requires at least one item.");
		quickStatsId = id;
		quickStats = List.of(items);
		return this;
	}

	/**
	 * Sets whether this table is the page's primary table (affects layout only).
	 *
	 * @param v The new value.
	 * @return This object.
	 */
	public TableSpec primary(boolean v) {
		primary = v;
		return this;
	}

	/**
	 * Sets the CSS class applied to this table's card.  It is carried on the card placement, not in the card body
	 * returned by {@link #toCardBody()}.
	 *
	 * @param value The class list.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public TableSpec cssClass(String value) {
		cssClass = value;
		return this;
	}

	/** @return This table's id. */
	public String id() {
		return id;
	}

	/** @return The columns, in display order. */
	public List<Column> columns() {
		return Collections.unmodifiableList(columns);
	}

	/** @return The row actions, in menu order. */
	public List<RowAction> rowActions() {
		return Collections.unmodifiableList(rowActions);
	}

	/** @return The card CSS class, or <jk>null</jk>. */
	public String cssClass() {
		return cssClass;
	}

	/** @return The per-row detail panel, if set. */
	public Optional<RowDetail> detail() {
		return Optional.ofNullable(detail);
	}

	/**
	 * Validates this table.  A duplicate column and a rowClass op/value mismatch are checked eagerly by
	 * {@link #columns(Column...)} and {@link #rowClass(String,Op,Object,String) rowClass}, and an unknown export button
	 * by {@link RibbonItem#export(String...)}, so none of those can be seen here.
	 *
	 * @throws IllegalStateException If any check fails.
	 */
	public void validate() {
		if (dataUrl == null || dataUrl.isBlank())
			throw isex("TableSpec '%s' requires dataUrl.", id);
		if (columns.isEmpty())
			throw isex("TableSpec '%s' requires at least one column.", id);
		var names = columns.stream().map(Column::name).toList();
		for (var o : defaultOrder)
			if (! names.contains(o.get("key")))
				throw isex("TableSpec '%s' defaultOrder column '%s' is not a column; columns: '%s'.",
					id, o.get("key"), String.join(", ", names));
		for (var item : ribbon) {
			var m = item.toMap();
			if ("dialog".equals(m.get("type")) && (m.get("form") == null || m.get("endpoint") == null))
				throw isex("TableSpec '%s' ribbon dialog '%s' requires form and endpoint.", id, m.get("id"));
		}
		var actionIds = new HashSet<String>();
		for (var a : rowActions)
			if (! actionIds.add(a.id))
				throw isex("TableSpec '%s' declares row action '%s' twice.", id, a.id);
		if (bulk != null && selection == null)
			throw isex("TableSpec '%s' sets bulk without selection.", id);
		if (bulk != null && (bulk.actions == null || bulk.actions.isEmpty()))
			throw isex("TableSpec '%s' sets bulk with no actions.", id);
		for (var a : rowActions)
			if (a.endpoint == null || a.endpoint.isBlank())
				throw isex("TableSpec '%s' row action '%s' requires an endpoint.", id, a.id);
		if (bulk != null && bulk.actions != null)
			for (var a : bulk.actions)
				if (a.endpoint == null || a.endpoint.isBlank())
					throw isex("TableSpec '%s' bulk action '%s' requires an endpoint.", id, a.id);
		for (var c : columns)
			if (c.searchable() && dataMode == DataMode.SERVER && c.operators() == null && operators == null)
				throw isex("TableSpec '%s' column '%s' sets searchType but dataMode is '%s' and the table "
					+ "declares no operators.", id, c.name(), dataMode.wire());
	}

	/**
	 * Builds this table's catalog-form card body, with no {@code contractVersion} anywhere (the client stamps every
	 * contract version).  This is the only place in this type that knows the datatables wire shape.  The result is
	 * the unwrapped catalog the {@code datatables} card type reads, and is rendered the same single way
	 * {@code <@card>} is, by the page mixin's {@code cardTypes.toCard(CardSource.create("datatables", id())
	 * .bodyMap(toCardBody()).build())}; this type never calls a card-type handler directly.
	 *
	 * <p>
	 * The body holds only plain data (maps, lists, strings, numbers, booleans), the same shape a parsed body has.
	 *
	 * <p>
	 * Each column carries its effective operator set (its own, else {@link #operators(SearchOperatorSet)}).
	 *
	 * @return The catalog, with keys in a fixed order and absent optional values omitted.
	 */
	public JsonMap toCardBody() {
		var t = new JsonMap();
		t.put("dataUrl", dataUrl);
		t.put("columns", plain(columns.stream().map(c -> c.toCatalogMap(operators)).toList()));
		if (dataMode != null)
			t.put("dataMode", dataMode.wire());
		if (! defaultOrder.isEmpty())
			t.put("defaultOrder", List.copyOf(defaultOrder));
		if (! ribbon.isEmpty())
			t.put("ribbon", ribbon.stream().map(RibbonItem::toMap).toList());
		if (rowType != null)
			t.put("rowType", rowType);
		if (pollIntervalMs != null)
			t.put("pollIntervalMs", pollIntervalMs);
		if (primary)
			t.put("primary", true);
		if (! rowClassRules.isEmpty())
			t.put("rowClassRules", List.copyOf(rowClassRules));
		if (! rowActions.isEmpty())
			t.put("rowActions", plain(rowActions));
		if (selection != null) {
			var s = new JsonMap();
			s.put("rowIdField", selection.rowIdField());
			s.put("selectAll", selection.selectAll());
			s.put("scope", selection.scope().wire());
			if (selection.labelField() != null)
				s.put("labelField", selection.labelField());
			if (selection.selectableWhen() != null)
				s.put("selectableWhen", plain(selection.selectableWhen()));
			t.put("selection", s);
		}
		if (bulk != null) {
			var b = new JsonMap();
			b.put("actions", plain(bulk.actions));
			t.put("bulk", b);
		}
		if (detail != null)
			t.put("detail", detail.toMap());
		if (quickStats != null) {
			var q = new JsonMap();
			q.put("id", quickStatsId);
			q.put("items", quickStats.stream().map(QuickStat::toMap).toList());
			t.put("quickStats", q);
		}
		return t;
	}

	/** Converts a bean (or list of beans) to the plain maps, lists and scalars a parsed JSON body would hold. */
	private static Object plain(Object bean) {
		return Json.to(Json.of(bean), Object.class);
	}
}
