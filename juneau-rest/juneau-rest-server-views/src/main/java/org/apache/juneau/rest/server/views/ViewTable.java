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

import org.apache.juneau.rest.server.filter.*;

/**
 * Public DOM/CSS contract for a {@code data-juneau-view} table.
 *
 * <p>
 * Table authoring is FTL {@code <@card type="datatables">} (JSON5 catalog + {@code juneau-page-cards.js}).
 * This type is the stable set of marker attributes and class names the runtime and consumer chrome still
 * stamp or select: CSRF on a shell ancestor, layout hint, selection/bulk markers, row-detail template
 * attributes, and the named overflow class. It is not a Java page-authoring factory.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewsMixin}
 * 	<li class='jc'>{@link SelectionDef}
 * 	<li class='jc'>{@link BulkMutateDef}
 * </ul>
 *
 * @since 10.0.0
 */
public class ViewTable {

	/** Marker attribute the {@code juneau-views.js} runtime looks for to auto-initialize a view table. */
	public static final String MARKER_ATTR = "data-juneau-view";

	/** Prefix of the sidecar {@code <script>} element id: {@code juneau-view:<viewId>}. */
	public static final String SIDECAR_ID_PREFIX = "juneau-view:";

	/**
	 * Class stamped on every emitted {@code <table>} &mdash; top-level and nested alike &mdash; and the anchor of the
	 * toolkit's cell-overflow contract.
	 *
	 * <p>
	 * {@code juneau-views.css} clips cell content through the <b>named</b> selector {@code .juneau-view-table td}
	 * rather than a global unnamed {@code td} rule, so the contract reaches only tables this toolkit produced and
	 * never app markup the toolkit does not own.  See {@link #CELL_WRAP_CLASS} for the opt-out.
	 */
	public static final String TABLE_CLASS = "juneau-view-table";

	/**
	 * Per-cell opt-out from the clip/ellipsis default: {@code .juneau-view-table td.juneau-cell-wrap} restores the
	 * pre-10.0 wrapping behavior.
	 *
	 * <p>
	 * The named renderers whose output is a chip / bar / link rather than prose ({@code progress}, {@code pill},
	 * {@code tag}, {@code linked}) stamp this themselves through their {@code class} facet; an author who wants a
	 * wrapping prose column stamps it through that column's own class.
	 */
	public static final String CELL_WRAP_CLASS = "juneau-cell-wrap";

	/**
	 * Attribute the CSRF token is stamped into on a shell ancestor (typically {@code <main>}) so
	 * {@code juneau-views.js} can attach it to mutating requests.
	 *
	 * <p>
	 * On an allowed request the {@link LoopbackBoundaryFilter} publishes the process's CSRF token under
	 * {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE}; the host copies it here. The runtime <b>fails closed</b>
	 * when this attribute is absent, empty, or whitespace: it visibly refuses to issue any row-action request
	 * rather than sending one the server would 403.
	 */
	public static final String CSRF_ATTR = "data-juneau-csrf";

	/**
	 * Marker attribute stamped when a {@link SelectionDef} is declared. Pure DOM signaling
	 * &mdash; never part of the {@code VIEW_META} wire contract; see {@link SelectionDef}'s class javadoc.
	 */
	public static final String SELECT_ATTR = "data-juneau-select";

	/** Attribute carrying {@link SelectionDef#rowIdField()} &mdash; the row-data key the runtime stamps as each row's stable id. */
	public static final String ROW_ID_FIELD_ATTR = "data-juneau-row-id-field";

	/** Attribute carrying {@code "1"}/{@code "0"} for {@link SelectionDef#selectAll()}. */
	public static final String SELECT_ALL_ATTR = "data-juneau-select-all";

	/**
	 * Marker attribute stamped when a {@link BulkMutateDef} is declared; pairs with the
	 * {@link #BULK_SIDECAR_ID_PREFIX} sidecar carrying the actual bulk-action list.
	 */
	public static final String BULK_ATTR = "data-juneau-bulk";

	/** Prefix of the bulk-actions sidecar {@code <script>} element id: {@code juneau-view-bulk:<viewId>}. */
	public static final String BULK_SIDECAR_ID_PREFIX = "juneau-view-bulk:";

	/**
	 * Attribute the resolved, context-path-aware saved-views REST base is stamped onto on a wrapper
	 * so {@code juneau-config.js} can locate it via {@code table.closest('[data-juneau-saved-views]')}.
	 *
	 * <p>
	 * The mount is fixed at {@link SavedViewsMixin#SAVED_VIEWS_PREFIX}; only the resolved URL varies with the
	 * servlet context path. Absent/blank means the JS server-provider is unavailable for this table (fail closed).
	 */
	public static final String SAVED_VIEWS_ATTR = "data-juneau-saved-views";

	/**
	 * Full-real-estate layout hint. A first-class public {@code data-juneau-*} convention: consumer chrome
	 * (the console {@code chrome.css} full-bleed {@code :has} rules) can widen the enclosing card/main out of
	 * its default centered {@code max-width}.
	 */
	public static final String LAYOUT_ATTR = "data-juneau-layout";

	/** The only {@link #LAYOUT_ATTR} value in v1: request full horizontal real estate for the wrapper's content. */
	public static final String LAYOUT_WIDE = "wide";

	/**
	 * CSS class on the dedicated row-expand header cell so the expander glyph never shares the first data column.
	 */
	public static final String DETAIL_TH_CLASS = "juneau-view-detail-th";

	/**
	 * CSS class on the dedicated row-expand body cell (and the DataTables column {@code className}).
	 */
	public static final String DETAIL_CONTROL_CLASS = "juneau-view-detail-control";

	/** Marker attribute on the row-detail {@code <template>} sibling of the view table. */
	public static final String DETAIL_TEMPLATE_ATTR = "data-juneau-row-detail";

	/** Attribute carrying the row-detail contract version on the row-detail template. */
	public static final String DETAIL_CONTRACT_ATTR = "data-juneau-detail-contract";

	/** Attribute carrying the server-stamped expand GET path template on the row-detail template. */
	public static final String DETAIL_URL_ATTR = "data-juneau-detail-url";

	/** Historical section-id marker; no longer emitted (row-detail bodies are one region). */
	public static final String DETAIL_SECTION_ATTR = "data-juneau-detail-section";

	/** Marker on a client-painted field-grid value slot ({@code fieldGrid} catalog {@code data} key). */
	public static final String DETAIL_FIELD_ATTR = "data-juneau-field";

	/** Marker on the optional detail-panel header (title + header actions). */
	public static final String DETAIL_HEADER_ATTR = "data-juneau-detail-header";

	/** Marker on the header title element filled from a <code>{field}</code> template. */
	public static final String DETAIL_TITLE_ATTR = "data-juneau-detail-title";

	/** Attribute carrying the header title template (placeholders filled at expand time). */
	public static final String DETAIL_TITLE_TEMPLATE_ATTR = "data-juneau-detail-title-template";

	/**
	 * Comma-separated {@link RegionDef#titleFields} allowlist stamped on the row-detail {@code <template>}.
	 * Copied onto the cloned panel at expand time; {@code paintDetailTitleSlot} substitutes only these keys.
	 */
	public static final String DETAIL_TITLE_FIELDS_ATTR = "data-juneau-title-fields";

	/** Attribute carrying the header icon registry name. */
	public static final String DETAIL_ICON_ATTR = "data-juneau-detail-icon";

	/**
	 * Attribute carrying a {@link FieldFormat} wire token.  Omitted for {@link FieldFormat#TEXT}
	 * (the default).
	 */
	public static final String DETAIL_FIELD_FORMAT_ATTR = "data-juneau-field-format";

	/** Attribute carrying a field-grid render id.  Omitted when render is unset. */
	public static final String DETAIL_FIELD_RENDER_ATTR = "data-juneau-field-render";

	/**
	 * Attribute carrying JSON-encoded {@link Render#meta}.  Omitted when meta is null or empty.
	 */
	public static final String DETAIL_FIELD_RENDER_META_ATTR = "data-juneau-field-render-meta";

	/** Attribute carrying a field-grid href template.  Omitted when href is unset. */
	public static final String DETAIL_FIELD_RENDER_HREF_ATTR = "data-juneau-field-render-href";

	/** Attribute carrying an {@link org.apache.juneau.rest.server.widgets.ActionRef} id on a write button. */
	public static final String DETAIL_ACTION_ATTR = "data-juneau-action";

	/** Attribute carrying a {@link org.apache.juneau.rest.server.widgets.SafeAction#wire()} token. */
	public static final String DETAIL_SAFE_ATTR = "data-juneau-safe";

	/**
	 * Attribute carrying an {@link org.apache.juneau.rest.server.widgets.ActionRef}'s row-state rules as a JSON
	 * array, in the author's declared order. Omitted entirely for an ungated action.
	 *
	 * <p>
	 * Each entry is <c>{"field":..., "op":..., "value"?:..., "reason":...}</c> &mdash; the same
	 * JSON-in-a-data-attribute shape {@link #DETAIL_FIELD_RENDER_META_ATTR} uses.  Declaration order is the array
	 * order, because the first failing rule is the one whose reason an operator sees.
	 */
	public static final String DETAIL_ACTION_RULES_ATTR = "data-juneau-action-rules";

	/**
	 * Attribute on the hidden node a gated action's disabled reason is painted into, carrying that action's id.
	 * Emitted only alongside {@link #DETAIL_ACTION_RULES_ATTR}.
	 *
	 * <p>
	 * The node exists because {@code aria-describedby} needs something real to point at.  It carries the native
	 * HTML {@code hidden} attribute rather than a utility class, so no stylesheet rule is needed to keep it out of
	 * view and none is added.  It carries no element {@code id} here either: this whole subtree is cloned per
	 * expanded row, so the runtime mints the row-unique one.
	 */
	public static final String DETAIL_ACTION_DESC_ATTR = "data-juneau-action-desc";

	/**
	 * Marker attribute on the nested-table wrapper {@code <div>} (value {@code "1"}).
	 *
	 * <p>
	 * Nested-table seeding inside a row-detail panel is deferred (F24); this marker remains so a later host can
	 * skip auto-init of a nested {@code data-juneau-view} table until its pane is visible.
	 */
	public static final String NESTED_ATTR = "data-juneau-nested";

	/**
	 * Attribute on the nested VIEW_META sidecar {@code <script>}, carrying the nested view id.
	 *
	 * <p>
	 * The nested sidecar carries no HTML {@code id} (a {@code <template>} clone would collide); the runtime finds it
	 * as a sibling of the nested {@code <table>} by this attribute instead.
	 */
	public static final String NESTED_META_ATTR = "data-juneau-nested-meta";

	/** Attribute carrying the nested-table contract version on the nested-table wrapper. */
	public static final String NESTED_CONTRACT_ATTR = "data-juneau-nested-contract";

	/** Attribute carrying the nested-table parent-scope query parameter name on the nested-table wrapper. */
	public static final String NESTED_SCOPE_PARAM_ATTR = "data-juneau-nested-scope-param";

	private ViewTable() {}
}
