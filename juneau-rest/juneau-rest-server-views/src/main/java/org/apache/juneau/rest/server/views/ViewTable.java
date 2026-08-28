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
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.widgets.*;

import jakarta.servlet.http.*;

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
 * <h5 class='section'>Cell overflow contract:</h5>
 * <p>
 * Every emitted {@code <table>} &mdash; top-level and nested alike &mdash; carries {@link #TABLE_CLASS
 * class="juneau-view-table"}, and {@code juneau-views.css} clips cell content off that class:
 * {@code .juneau-view-table td} declares a constrained box plus {@code overflow: hidden}, {@code text-overflow:
 * ellipsis}, and {@code white-space: nowrap}.  <b>Clip with an ellipsis is the default, not wrap</b>, and it is the
 * same policy on both DataTables generations, so a table does not change shape depending on which generation the
 * host application supplied.
 *
 * <p>
 * Wrapping is available per cell as an <b>opt-out</b>: {@link #CELL_WRAP_CLASS class="juneau-cell-wrap"} on a
 * {@code <td>} restores the wrap.  The named renderers whose output is a chip / bar / link rather than prose
 * ({@code progress}, {@code pill}, {@code tag}, {@code linked}) stamp it themselves through their {@code class}
 * facet, and an author who wants a wrapping prose column stamps it through that column's own class.  The
 * {@code truncate} renderer is unaffected and composes with this: it shortens the value, while the CSS clips
 * whatever still overflows the box.
 *
 * <p>
 * The selector is deliberately named rather than a global unnamed {@code td} rule, so the contract reaches only
 * tables this emitter produced.  Note this governs cell <i>content</i> only &mdash; the table's own horizontal
 * scroll region is separate and unchanged.
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

	/**
	 * Class stamped on every emitted {@code <table>} &mdash; top-level and nested alike &mdash; and the anchor of the
	 * toolkit's cell-overflow contract.
	 *
	 * <p>
	 * {@code juneau-views.css} clips cell content through the <b>named</b> selector {@code .juneau-view-table td}
	 * rather than a global unnamed {@code td} rule, so the contract reaches only tables this emitter produced and
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
	 * Attribute the auto-embedded CSRF token is stamped into on the emitted {@code <table>}.
	 *
	 * <p>
	 * On an allowed request the {@link LoopbackBoundaryFilter} publishes the process's CSRF token under
	 * {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE}; the request-bearing {@code of(...)} overloads read it and
	 * stamp it here so the {@code juneau-views.js} row-action submit can attach it without the host having to
	 * remember a step.  A host may also set this attribute (or a documented init call) itself as an
	 * override/fallback.  The runtime <b>fails closed</b> when this attribute is absent, empty, or whitespace: it
	 * visibly refuses to issue any row-action request rather than sending one the server would 403.
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
	 * Attribute the resolved, context-path-aware saved-views REST base is stamped onto on the wrapper
	 * {@code <div>} (standalone tables) so {@code juneau-config.js} can locate it via
	 * {@code table.closest('[data-juneau-saved-views]')}.
	 *
	 * <p>
	 * The mount is fixed at {@link SavedViewsMixin#SAVED_VIEWS_PREFIX}; only the resolved URL varies with the
	 * servlet context path.  Page-embedded tables find the same attribute on the enclosing {@link PageTable}
	 * shell instead.  Absent/blank means the JS server-provider is unavailable for this table (fail closed).
	 */
	public static final String SAVED_VIEWS_ATTR = "data-juneau-saved-views";

	/**
	 * Full-real-estate layout hint stamped on the wrapper {@code <div>} (design doc §"Full real estate" Goal 1).
	 * A first-class public
	 * {@code data-juneau-*} convention: the toolkit stamps {@link #LAYOUT_WIDE} on the one stamp node
	 * {@code ViewTable} already returns so consumer chrome (the console {@code chrome.css} full-bleed {@code :has}
	 * rules) can widen the enclosing card/main out of its default centered {@code max-width}.  Never stamped on
	 * the {@code <table>}, {@code .jc-card}, or {@code .jc-main} (this emitter produces none of those classes).
	 */
	public static final String LAYOUT_ATTR = "data-juneau-layout";

	/** The only {@link #LAYOUT_ATTR} value in v1: request full horizontal real estate for the wrapper's content. */
	public static final String LAYOUT_WIDE = "wide";

	/**
	 * CSS class on the dedicated row-expand header cell.  Emitted whenever {@link ViewDef#details} is set so the
	 * expander glyph never shares the first data column (a dedicated {@code .juneau-view-detail-control} column).
	 */
	public static final String DETAIL_TH_CLASS = "juneau-view-detail-th";

	/**
	 * CSS class on the dedicated row-expand body cell (and the DataTables column {@code className}).
	 */
	public static final String DETAIL_CONTROL_CLASS = "juneau-view-detail-control";

	/** Marker attribute on the row-detail {@code <template>} sibling of the view table. */
	public static final String DETAIL_TEMPLATE_ATTR = "data-juneau-row-detail";

	/** Attribute carrying {@link RowDetailDef#CONTRACT_VERSION} on the row-detail template. */
	public static final String DETAIL_CONTRACT_ATTR = "data-juneau-detail-contract";

	/** Attribute carrying the server-stamped expand GET path template on the row-detail template. */
	public static final String DETAIL_URL_ATTR = "data-juneau-detail-url";

	/** Attribute carrying a {@link DetailSection#id} on each {@code <section>}. */
	public static final String DETAIL_SECTION_ATTR = "data-juneau-detail-section";

	/** Attribute carrying a {@link DetailField#data} key on each empty field slot. */
	public static final String DETAIL_FIELD_ATTR = "data-juneau-field";

	/** Marker on the optional detail-panel header (title + header actions). */
	public static final String DETAIL_HEADER_ATTR = "data-juneau-detail-header";

	/** Marker on the header title element filled from a <code>{field}</code> template. */
	public static final String DETAIL_TITLE_ATTR = "data-juneau-detail-title";

	/** Attribute carrying the header title template (placeholders filled at expand time). */
	public static final String DETAIL_TITLE_TEMPLATE_ATTR = "data-juneau-detail-title-template";

	/** Attribute carrying the header icon registry name. */
	public static final String DETAIL_ICON_ATTR = "data-juneau-detail-icon";

	/**
	 * Attribute carrying a {@link DetailField.Format} wire token.  Omitted for {@link DetailField.Format#TEXT}
	 * (the default).
	 */
	public static final String DETAIL_FIELD_FORMAT_ATTR = "data-juneau-field-format";

	/** Attribute carrying a {@link DetailField#render} id.  Omitted when render is unset. */
	public static final String DETAIL_FIELD_RENDER_ATTR = "data-juneau-field-render";

	/**
	 * Attribute carrying JSON-encoded {@link Render#meta}.  Omitted when meta is null or empty.
	 */
	public static final String DETAIL_FIELD_RENDER_META_ATTR = "data-juneau-field-render-meta";

	/** Attribute carrying a {@link DetailField#href} template.  Omitted when href is unset. */
	public static final String DETAIL_FIELD_RENDER_HREF_ATTR = "data-juneau-field-render-href";

	/** Attribute carrying an {@link org.apache.juneau.rest.server.widgets.ActionRef} id on a write button. */
	public static final String DETAIL_ACTION_ATTR = "data-juneau-action";

	/** Attribute carrying a {@link org.apache.juneau.rest.server.widgets.SafeAction#wire()} token. */
	public static final String DETAIL_SAFE_ATTR = "data-juneau-safe";

	/**
	 * Attribute carrying an {@link ActionRef}'s row-state rules as a JSON array, in the author's declared order.
	 * Omitted entirely for an ungated action.
	 *
	 * <p>
	 * Each entry is <c>{"field":..., "op":..., "value"?:..., "reason":...}</c> &mdash; the same
	 * JSON-in-a-data-attribute shape {@link #DETAIL_FIELD_RENDER_META_ATTR} uses.  Declaration order is the array
	 * order, because the first failing rule is the one whose reason an operator sees.
	 */
	public static final String DETAIL_ACTION_RULES_ATTR = "data-juneau-action-rules";

	/**
	 * Attribute on the hidden node a gated {@link ActionRef}'s disabled reason is painted into, carrying that
	 * action's id.  Emitted only alongside {@link #DETAIL_ACTION_RULES_ATTR}.
	 *
	 * <p>
	 * The node exists because {@code aria-describedby} needs something real to point at.  It carries the native
	 * HTML {@code hidden} attribute rather than a utility class, so no stylesheet rule is needed to keep it out of
	 * view and none is added.  It carries no element {@code id} here either: this whole subtree is cloned per
	 * expanded row, so the runtime mints the row-unique one.
	 */
	public static final String DETAIL_ACTION_DESC_ATTR = "data-juneau-action-desc";

	/**
	 * Marker attribute on the nested-table wrapper {@code <div>} inside a {@link DetailSection} (value {@code "1"}).
	 *
	 * <p>
	 * The {@code juneau-views.js} runtime auto-init pass skips any {@code data-juneau-view} table under this marker
	 * &mdash; a nested table is instantiated only after the enclosing row's detail GET succeeds and its pane becomes
	 * visible, scoped to the parent row (see {@link NestedTableDef}).
	 */
	public static final String NESTED_ATTR = "data-juneau-nested";

	/**
	 * Attribute on the nested VIEW_META sidecar {@code <script>}, carrying the nested {@link ViewDef#id}.
	 *
	 * <p>
	 * The nested sidecar carries no HTML {@code id} (a {@code <template>} clone would collide); the runtime finds it
	 * as a sibling of the nested {@code <table>} by this attribute instead.
	 */
	public static final String NESTED_META_ATTR = "data-juneau-nested-meta";

	/** Attribute carrying {@link NestedTableDef#CONTRACT_VERSION} on the nested-table wrapper. */
	public static final String NESTED_CONTRACT_ATTR = "data-juneau-nested-contract";

	/** Attribute carrying {@link NestedTableDef#parentScopeParam} on the nested-table wrapper. */
	public static final String NESTED_SCOPE_PARAM_ATTR = "data-juneau-nested-scope-param";

	/**
	 * Attribute carrying a {@link DetailSection#count} on a detail {@code <section>}.  Omitted when the count is
	 * <jk>null</jk>, so a section that declares none serializes exactly as it did before.
	 */
	private static final String DETAIL_COUNT_ATTR = "data-juneau-detail-count";

	/** The widest step of the fields-grid column ladder in {@code juneau-views.css}. */
	private static final int DETAIL_MAX_COLUMNS = 4;

	/** MIME type of the VIEW_META/bulk-actions/nested-VIEW_META sidecars. */
	private static final String JSON_CONTENT_TYPE = "application/json";

	/** The HTML {@code class} attribute name, as passed to {@link org.apache.juneau.bean.html5.HtmlBuilder}'s {@code attr(...)}. */
	private static final String CLASS_ATTR = "class";

	/** The HTML {@code aria-label} attribute name. */
	private static final String ARIA_LABEL_ATTR = "aria-label";

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
	 * Builds the view-table shell for a server-side view, auto-embedding the request's CSRF token so a declared
	 * row action can submit with it.
	 *
	 * <p>
	 * The token is read from {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} &mdash; the value the boundary filter
	 * stamps on every allowed request &mdash; and stamped into {@link #CSRF_ATTR} on the emitted {@code <table>}.
	 * When the request carries no such token (no boundary filter in front of this application, or the attribute is
	 * blank), no attribute is emitted and the runtime fails closed on any row-action attempt.  This is the
	 * auto-embed entry point of the token contract; a {@link #CSRF_ATTR} the host sets itself is the
	 * override/fallback.
	 *
	 * @param req The current request, whose {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} supplies the token.
	 * 	Can be <jk>null</jk> (no token embedded).
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	public static Div of(HttpServletRequest req, ViewDef viewDef) {
		return emit(MarshallingContext.DEFAULT, viewDef, req,
			new RenderOptions(null, csrfToken(req), null, null, savedViewsBase(req), null));
	}

	/**
	 * Builds the view-table shell for a server-side view from a {@link RestRequest}, auto-embedding the CSRF token
	 * and resolving {@code $FV} chrome against a per-response sibling session.
	 *
	 * <p>
	 * The RestRequest counterpart of {@link #of(HttpServletRequest, ViewDef)}.  CSRF, saved-views, and {@code $FV}
	 * resolution are identical; this overload exists so callers can name the RestRequest host path directly
	 * (see {@link PageTable#of(RestRequest, PageDef)}).
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	public static Div of(RestRequest req, ViewDef viewDef) {
		return of((HttpServletRequest) req, viewDef);
	}

	/**
	 * Builds the view-table shell for a server-side view whose emitted DOM identity is qualified by an enclosing
	 * host, so two tables built from the SAME {@link ViewDef} can coexist on one page.
	 *
	 * <p>
	 * Identical to {@link #of(HttpServletRequest, ViewDef)} except that the emitted {@code <table>} html {@code id}
	 * and both sidecar element ids become {@code <qualifier>:<viewId>} instead of the bare {@link ViewDef#id}.  The
	 * {@link #MARKER_ATTR} attribute is <b>not</b> qualified: it stays the author's own {@link ViewDef#id}, which is
	 * what the serialized VIEW_META carries and what author-keyed runtime lookups resolve against.  The two id
	 * spaces therefore diverge on purpose &mdash; minted identity is for the DOM's uniqueness rules, authored
	 * identity is for the contract.
	 *
	 * <p>
	 * A host that uses this must scope its own runtime lookups to its own subtree; a qualified id is not an
	 * invitation to reach for it document-wide.
	 *
	 * @param req The current request, whose {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} supplies the token.
	 * 	Can be <jk>null</jk> (no token embedded).
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param idQualifier The host-supplied id prefix, or <jk>null</jk>/blank to mint exactly the unqualified ids
	 * 	{@link #of(HttpServletRequest, ViewDef)} mints.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	static Div of(HttpServletRequest req, ViewDef viewDef, String idQualifier) {
		return emit(MarshallingContext.DEFAULT, viewDef, req,
			new RenderOptions(null, csrfToken(req), null, null, savedViewsBase(req), idQualifier));
	}

	/**
	 * Builds the view-table shell for a view rendered inside an enclosing shell, keeping that shell's marshalling
	 * context <b>and</b> propagating its request.
	 *
	 * <p>
	 * The entry point {@link PageTable} uses for each child view.  A {@code null} {@code req} makes this exactly
	 * equivalent to {@link #of(MarshallingContext, ViewDef, Collection) of(ctx, viewDef, null)} &mdash; no token, no
	 * saved-views stamp, no {@code $FV} resolution &mdash; so a request-free host emits byte-identical output to what
	 * it always has, and only a request-bearing host gains the request-scoped behavior.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param req The enclosing request, or <jk>null</jk> for a request-free emit.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	static Div of(MarshallingContext ctx, HttpServletRequest req, ViewDef viewDef) {
		return emit(ctx, viewDef, req, new RenderOptions(null, csrfToken(req), null, null, savedViewsBase(req), null));
	}

	/**
	 * Builds the view-table shell and renders {@code rows}, auto-embedding the request's CSRF token.
	 *
	 * <p>
	 * The request-bearing counterpart of {@link #of(ViewDef, Collection)}; see {@link #of(HttpServletRequest, ViewDef)}
	 * for the token-embed and fail-closed contract.
	 *
	 * @param req The current request, whose {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} supplies the token.
	 * 	Can be <jk>null</jk> (no token embedded).
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, and the
	 * 	JSON sidecar.
	 */
	public static Div of(HttpServletRequest req, ViewDef viewDef, Collection<?> rows) {
		return emit(MarshallingContext.DEFAULT, viewDef, req,
			new RenderOptions(rows, csrfToken(req), null, null, savedViewsBase(req), null));
	}

	/**
	 * Builds the view-table shell and renders {@code rows} from a {@link RestRequest}, auto-embedding the CSRF token
	 * and resolving {@code $FV} chrome against a per-response sibling session.
	 *
	 * <p>
	 * The RestRequest counterpart of {@link #of(HttpServletRequest, ViewDef, Collection)}.
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, and the
	 * 	JSON sidecar.
	 */
	public static Div of(RestRequest req, ViewDef viewDef, Collection<?> rows) {
		return of((HttpServletRequest) req, viewDef, rows);
	}

	/**
	 * Builds the view-table shell with row selection enabled (design doc §9.3), auto-embedding
	 * the request's CSRF token.
	 *
	 * <p>
	 * This overload has no code path that can render a bulk-mutate control &mdash; a {@link SelectionDef} alone
	 * can only ever add per-row checkboxes (and, per {@link SelectionDef#selectAll()}, a select-all header
	 * checkbox). Use {@link #of(HttpServletRequest, ViewDef, Collection, BulkMutateDef)} when bulk mutation is
	 * also required (that overload requires its own {@link SelectionDef}, supplied via
	 * {@link BulkMutateDef#create(WritePermit, SelectionDef)}).
	 *
	 * @param req The current request, whose {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} supplies the CSRF
	 * 	token. Can be <jk>null</jk> (no token embedded).
	 * @param viewDef The built view definition. Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps). Can be <jk>null</jk> (server-side mode) or empty.
	 * @param selection The selection opt-in. Must not be <jk>null</jk> (use one of the other overloads for a table
	 * 	with no selection).
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view data-juneau-select>}, its VIEW_META
	 * 	sidecar, and no bulk-actions sidecar.
	 */
	public static Div of(HttpServletRequest req, ViewDef viewDef, Collection<?> rows, SelectionDef selection) {
		if (selection == null)
			throw iaex("selection must not be null; use one of the other of(...) overloads for a table with no selection.");
		return emit(MarshallingContext.DEFAULT, viewDef, req,
			new RenderOptions(rows, csrfToken(req), selection, null, savedViewsBase(req), null));
	}

	/**
	 * Builds the view-table shell with row selection AND bulk mutation enabled (design doc §9.3), auto-embedding
	 * the request's CSRF token.
	 *
	 * <p>
	 * The selection this table renders is {@link BulkMutateDef#selection()} &mdash; the one {@code bulkMutate} was
	 * itself constructed against ({@link BulkMutateDef#create(WritePermit, SelectionDef)} requires one) &mdash; so
	 * there is exactly one {@link SelectionDef} in play and no way for it to disagree with what the bulk actions
	 * target.
	 *
	 * @param req The current request, whose {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} supplies the CSRF
	 * 	token. Can be <jk>null</jk> (no token embedded).
	 * @param viewDef The built view definition. Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps). Can be <jk>null</jk> (server-side mode) or empty.
	 * @param bulkMutate The bulk-mutate opt-in. Must not be <jk>null</jk> (use the {@link SelectionDef} overload,
	 * 	or one of the earlier, selection-only overloads, for a table with no bulk mutation).
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view data-juneau-select data-juneau-bulk>},
	 * 	its VIEW_META sidecar, AND the independently-versioned bulk-actions sidecar.
	 */
	public static Div of(HttpServletRequest req, ViewDef viewDef, Collection<?> rows, BulkMutateDef bulkMutate) {
		if (bulkMutate == null)
			throw iaex("bulkMutate must not be null; use the SelectionDef overload for selection without bulk mutation.");
		return emit(MarshallingContext.DEFAULT, viewDef, req,
			new RenderOptions(rows, csrfToken(req), bulkMutate.selection(), bulkMutate, savedViewsBase(req), null));
	}

	/**
	 * Builds the view-table shell with row selection enabled, using the default marshalling context and no CSRF
	 * token embed (test/no-request convenience; see {@link #of(HttpServletRequest, ViewDef, Collection, SelectionDef)}
	 * for the auto-embedding counterpart).
	 *
	 * @param viewDef The built view definition. Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps). Can be <jk>null</jk> (server-side mode) or empty.
	 * @param selection The selection opt-in. Must not be <jk>null</jk>.
	 * @return A new {@link Div} as described above.
	 */
	public static Div of(ViewDef viewDef, Collection<?> rows, SelectionDef selection) {
		if (selection == null)
			throw iaex("selection must not be null; use one of the other of(...) overloads for a table with no selection.");
		return of(MarshallingContext.DEFAULT, viewDef, rows, null, selection, null);
	}

	/**
	 * Builds the view-table shell with row selection AND bulk mutation enabled, using the default marshalling
	 * context and no CSRF token embed (test/no-request convenience; see
	 * {@link #of(HttpServletRequest, ViewDef, Collection, BulkMutateDef)} for the auto-embedding counterpart).
	 *
	 * @param viewDef The built view definition. Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps). Can be <jk>null</jk> (server-side mode) or empty.
	 * @param bulkMutate The bulk-mutate opt-in. Must not be <jk>null</jk>.
	 * @return A new {@link Div} as described above.
	 */
	public static Div of(ViewDef viewDef, Collection<?> rows, BulkMutateDef bulkMutate) {
		if (bulkMutate == null)
			throw iaex("bulkMutate must not be null; use the SelectionDef overload for selection without bulk mutation.");
		return of(MarshallingContext.DEFAULT, viewDef, rows, null, bulkMutate.selection(), bulkMutate);
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
		return of(ctx, viewDef, rows, null);
	}

	/**
	 * Builds the view-table shell, optionally stamping a pre-resolved CSRF token onto the emitted {@code <table>}.
	 *
	 * <p>
	 * The shared core the request-bearing and context-only overloads delegate to.  A non-blank {@code csrfToken}
	 * is stamped into {@link #CSRF_ATTR}; a blank or {@code null} token stamps nothing, leaving the runtime to
	 * fail closed on any row-action attempt.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @param csrfToken The CSRF token to embed, or <jk>null</jk>/blank to embed none.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, and the
	 * 	JSON sidecar.
	 */
	public static Div of(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows, String csrfToken) {
		return of(ctx, viewDef, rows, csrfToken, null, null);
	}

	/**
	 * Builds the view-table shell, optionally stamping a CSRF token, a {@link SelectionDef}, and/or a
	 * {@link BulkMutateDef} &mdash; the shared core every public overload ultimately delegates to.
	 *
	 * <p>
	 * {@code selection}/{@code bulkMutate} are independent opt-ins (design doc §9.3; HIGH-5): passing
	 * {@code selection} alone renders per-row checkboxes and nothing else &mdash; there is no branch below that
	 * can render a bulk-mutate control from {@code selection} alone. When {@code bulkMutate} is non-<jk>null</jk>,
	 * its own {@link BulkMutateDef#selection()} is what selection-related markup is rendered from; a caller-passed
	 * {@code selection} that is not that SAME instance is rejected, so the two can never silently disagree about
	 * which rows a bulk action targets.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @param csrfToken The CSRF token to embed, or <jk>null</jk>/blank to embed none.
	 * @param selection The selection opt-in, or <jk>null</jk> for none. When {@code bulkMutate} is non-<jk>null</jk>,
	 * 	must be either <jk>null</jk> or exactly {@code bulkMutate.selection()}.
	 * @param bulkMutate The bulk-mutate opt-in, or <jk>null</jk> for none.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>}, an optional {@code <tbody>}, the
	 * 	VIEW_META sidecar, and &mdash; only when {@code bulkMutate} is supplied &mdash; the independently-versioned
	 * 	bulk-actions sidecar.
	 * @throws IllegalArgumentException If {@code selection} and {@code bulkMutate} are both non-<jk>null</jk> but
	 * 	{@code selection} is not {@code bulkMutate.selection()}.
	 */
	public static Div of(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows, String csrfToken,
			SelectionDef selection, BulkMutateDef bulkMutate) {
		return of(ctx, viewDef, rows, csrfToken, selection, bulkMutate, null);
	}

	/**
	 * Builds the view-table shell, optionally stamping a CSRF token, selection, bulk-mutate, AND a resolved
	 * saved-views REST base onto the wrapper {@code <div>}.
	 *
	 * <p>
	 * A non-blank {@code savedViewsBase} is stamped into {@link #SAVED_VIEWS_ATTR} on the wrapper (not the
	 * {@code <table>}) so page-embedded tables still discover a page-shell stamp via {@code closest(...)}
	 * without this emitter having to thread a per-child request into {@link PageTable}.
	 *
	 * @param ctx The marshalling context used to read bean-property cell values.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @param rows The rows to render (beans or maps).  Can be <jk>null</jk> (server-side mode) or empty.
	 * @param csrfToken The CSRF token to embed, or <jk>null</jk>/blank to embed none.
	 * @param selection The selection opt-in, or <jk>null</jk> for none. When {@code bulkMutate} is non-<jk>null</jk>,
	 * 	must be either <jk>null</jk> or exactly {@code bulkMutate.selection()}.
	 * @param bulkMutate The bulk-mutate opt-in, or <jk>null</jk> for none.
	 * @param savedViewsBase The already-resolved saved-views REST base, or <jk>null</jk>/blank to stamp none.
	 * @return A new {@link Div} carrying the table, sidecars, and optional {@link #SAVED_VIEWS_ATTR} stamp.
	 * @throws IllegalArgumentException If {@code selection} and {@code bulkMutate} are both non-<jk>null</jk> but
	 * 	{@code selection} is not {@code bulkMutate.selection()}.
	 */
	public static Div of(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows, String csrfToken,
			SelectionDef selection, BulkMutateDef bulkMutate, String savedViewsBase) {
		return emit(ctx, viewDef, null, new RenderOptions(rows, csrfToken, selection, bulkMutate, savedViewsBase, null));
	}

	/**
	 * The shared core.  Validates and reconciles selection/bulk-mutate, then &mdash; when a {@code $FV} host is
	 * declared and {@code req} is a {@link RestRequest} &mdash; resolves that host's declared chrome
	 * (titles/labels) against a per-response <b>sibling</b> {@link VarResolverSession} before building both the
	 * painted {@code <th>}/labels and the VIEW_META sidecar, so the two agree (W1).  The resolved chrome is a
	 * per-response snapshot: each host's author {@code $FV{...}} templates are restored once this response's markup
	 * has captured the resolved strings.
	 *
	 * <h5 class='section'>Two hosts, one written lock order:</h5>
	 * <p>
	 * Two definitions reachable from here can host {@link ServerValues}: {@link ViewDef#serverValues} (the shipped
	 * host, whose chrome is the column/action/ribbon titles) and {@link RowDetailDef#serverValues} (the row-detail
	 * panel's own titles, painted into the {@code <template>} below).  They are resolved in the order
	 * <b>{@link RowDetailDef} then {@link ViewDef}</b> &mdash; the tail of the toolkit-wide
	 * {@code PageDef} &rarr; {@code RowDetailDef} &rarr; {@code ViewDef} order that {@link PageTable} opens.
	 * Because that order is the same on every path that can reach either lock, no two threads can take the pair in
	 * opposite orders, so the nested emit below (which resolves nothing of its own) cannot deadlock against a
	 * concurrent {@code ViewTable.of} on any view involved.
	 *
	 * <p>
	 * The two windows are <b>siblings, not nested sessions</b>: each builds its own {@link VarResolverSession} with
	 * its own registry, each resolves only its own allowlisted fields (there is no inheritance either way), and each
	 * restores strictly LIFO in a {@code finally} before its caller returns &mdash; so the outer host can never
	 * observe the inner host's resolved strings, and a throwing provider still leaves the author's templates intact.
	 *
	 * <p>
	 * {@code idQualifier} is the optional host-supplied DOM-identity prefix (see
	 * {@link #of(HttpServletRequest, ViewDef, String)}); <jk>null</jk> on every non-hosted path, which is what keeps
	 * an ordinary table's emitted ids exactly what they have always been.
	 */
	private static Div emit(MarshallingContext ctx, ViewDef viewDef, HttpServletRequest req, RenderOptions opts) {
		viewDef.validate();
		opts = opts.reconciled();

		var rr = req instanceof RestRequest r ? r : null;
		var detail = viewDef.details;
		if (rr != null && detail != null && detail.serverValues != null) {
			var session = serverValuesSession(rr, detail.serverValues);
			// The row-detail host takes its lock OUTSIDE the view host's, per the written order.
			synchronized (detail.lock) {
				var restore = resolveDetailChrome(detail, session);
				try {
					return emitViewHost(ctx, viewDef, rr, opts);
				} finally {
					restore.run();
				}
			}
		}
		return emitViewHost(ctx, viewDef, rr, opts);
	}

	/**
	 * The innermost {@code $FV} host: {@link ViewDef#serverValues} resolved around {@link #build}.
	 *
	 * <p>
	 * A shared {@link ViewDef} may be rendered concurrently, so the mutate-serialize-restore window is guarded and
	 * two responses cannot interleave resolved chrome onto the same instance.
	 */
	private static Div emitViewHost(MarshallingContext ctx, ViewDef viewDef, RestRequest req, RenderOptions opts) {
		if (viewDef.serverValues != null && req != null) {
			var session = serverValuesSession(req, viewDef.serverValues);
			synchronized (viewDef.lock) {
				var restore = resolveChrome(viewDef, session);
				try {
					return build(ctx, viewDef, opts);
				} finally {
					restore.run();
				}
			}
		}
		return build(ctx, viewDef, opts);
	}

	/**
	 * The emitted DOM identity of a table: the author's {@link ViewDef#id} unless a host qualified it, in which case
	 * {@code <qualifier>:<viewId>}.  A blank qualifier is treated as absent, so a host that has nothing to qualify
	 * with cannot accidentally mint a leading-colon id.
	 */
	private static String mintedId(String idQualifier, String viewId) {
		return idQualifier == null || idQualifier.isBlank() ? viewId : idQualifier + ":" + viewId;
	}

	private static Div build(MarshallingContext ctx, ViewDef viewDef, RenderOptions opts) {
		var id = mintedId(opts.idQualifier(), viewDef.id);
		var cols = viewDef.columns == null ? List.<Column>of() : viewDef.columns;
		var selection = opts.selection();

		// <thead> of column titles (falling back to the data key when no title was set).  Leading synthetic
		// columns are dedicated cells that never share the first data column: expander (when details is set),
		// then a selection checkbox.
		var tableChildren = new ArrayList<>();
		tableChildren.add(thead(tr(headerCells(viewDef, selection, cols).toArray())));
		if (opts.rows() != null)
			tableChildren.add(tbody(bodyRows(ctx, viewDef, selection, cols, opts.rows()).toArray()));

		// The html id is the minted (possibly host-qualified) identity; the marker attribute stays the AUTHOR's id,
		// which is what the VIEW_META sidecar carries and what author-keyed lookups resolve against.  With no
		// qualifier the two are the same string, so an ordinary table emits exactly what it always has.
		var table = table(tableChildren.toArray()).id(id).attr(MARKER_ATTR, viewDef.id).class_(TABLE_CLASS);
		stampTableAttrs(table, opts.csrfToken(), selection, opts.bulkMutate());

		// Sidecar: serialize the VIEW_META, neutralize script break-outs, then insert as RAW content (class javadoc).
		var json = escapeForScript(Json.of(viewDef));
		var sidecar = script().type(JSON_CONTENT_TYPE).id(SIDECAR_ID_PREFIX + id).text(rawText(json));

		var wrapper = div(wrapperChildren(viewDef, table, opts, id, sidecar).toArray());
		// Full-real-estate stamp (design doc §"Full real estate" Goal 1 / N2 A): the ONE stamp node is this wrapper
		// <div>. The console chrome full-bleed :has() rules widen the enclosing .jc-card/.jc-main off this attribute.
		wrapper.attr(LAYOUT_ATTR, LAYOUT_WIDE);
		if (opts.savedViewsBase() != null && ! opts.savedViewsBase().isBlank())
			wrapper.attr(SAVED_VIEWS_ATTR, opts.savedViewsBase());
		return wrapper;
	}

	/** Builds the {@code <thead>} row's cells: optional expander/selection cells, then one per declared column. */
	private static List<Object> headerCells(ViewDef viewDef, SelectionDef selection, List<Column> cols) {
		var headerCells = new ArrayList<Object>(cols.size() + 2);
		if (viewDef.details != null)
			headerCells.add(th().attr(CLASS_ATTR, DETAIL_TH_CLASS).attr(ARIA_LABEL_ATTR, "Expand"));
		if (selection != null)
			headerCells.add(th().attr(CLASS_ATTR, "juneau-view-select-th").attr(ARIA_LABEL_ATTR, "Select"));
		for (var c : cols)
			headerCells.add(th(c.title == null ? c.data : c.title));
		return headerCells;
	}

	/** Builds one {@code <tr>} per row, each with the same leading synthetic cells as {@link #headerCells}. */
	private static List<Object> bodyRows(MarshallingContext ctx, ViewDef viewDef, SelectionDef selection,
			List<Column> cols, Collection<?> rows) {
		var bodyRows = new ArrayList<Object>(rows.size());
		for (var row : rows) {
			var cells = new ArrayList<Object>(cols.size() + 2);
			if (viewDef.details != null)
				cells.add(td().attr(CLASS_ATTR, DETAIL_CONTROL_CLASS));
			if (selection != null)
				cells.add(td().attr(CLASS_ATTR, "juneau-view-select-cell"));
			for (var c : cols) {
				var v = value(ctx, row, c.data);
				cells.add(td(v == null ? "" : v));
			}
			bodyRows.add(tr(cells.toArray()));
		}
		return bodyRows;
	}

	/**
	 * Stamps the CSRF/selection/bulk DOM attributes onto the emitted {@code <table>} (never VIEW_META &mdash; see
	 * {@link SelectionDef}'s class javadoc).  A blank token or absent opt-in stamps nothing, so an ordinary table's
	 * markup is unaffected and the runtime fails closed rather than shipping an empty header the boundary would 403.
	 */
	private static void stampTableAttrs(Table table, String csrfToken, SelectionDef selection,
			BulkMutateDef bulkMutate) {
		if (csrfToken != null && ! csrfToken.isBlank())
			table.attr(CSRF_ATTR, csrfToken);
		if (selection != null) {
			table.attr(SELECT_ATTR, "1");
			table.attr(ROW_ID_FIELD_ATTR, selection.rowIdField());
			table.attr(SELECT_ALL_ATTR, selection.selectAll() ? "1" : "0");
		}
		if (bulkMutate != null)
			table.attr(BULK_ATTR, "1");
	}

	/**
	 * Builds the wrapper {@code <div>}'s children: the optional quick-stats strip, the {@code <table>}, the
	 * optional row-detail template, the optional bulk-actions sidecar (its OWN independently-versioned sidecar,
	 * {@link BulkMutateDef#CONTRACT_VERSION}, never merged into VIEW_META &mdash; a version bump here can never
	 * force a {@code ViewDef.CONTRACT_VERSION} bump), and finally the VIEW_META sidecar.
	 */
	private static List<Object> wrapperChildren(ViewDef viewDef, Table table, RenderOptions opts, String id,
			Script sidecar) {
		var children = new ArrayList<Object>();
		if (viewDef.quickStats != null)
			children.add(QuickStatsTable.of(viewDef.quickStats));
		children.add(table);
		if (viewDef.details != null)
			children.add(emitDetailTemplate(viewDef, opts.csrfToken()));
		if (opts.bulkMutate() != null) {
			var bulkJson = escapeForScript(Json.of(opts.bulkMutate()));
			children.add(script().type(JSON_CONTENT_TYPE).id(BULK_SIDECAR_ID_PREFIX + id).text(rawText(bulkJson)));
		}
		children.add(sidecar);
		return children;
	}

	/**
	 * Bundles the render-time options threaded through {@link #emit}/{@link #emitViewHost}/{@link #build} so those
	 * methods stay under the parameter-count ceiling.  A purely internal parameter object: never serialized, and
	 * no part of any wire contract.
	 */
	private record RenderOptions(Collection<?> rows, String csrfToken, SelectionDef selection,
			BulkMutateDef bulkMutate, String savedViewsBase, String idQualifier) {

		/**
		 * Reconciles {@code selection} against a declared {@code bulkMutate}'s own {@link BulkMutateDef#selection()}
		 * (design doc §9.3; HIGH-5): a caller-passed {@code selection} that is not that SAME instance is rejected,
		 * so the two can never silently disagree about which rows a bulk action targets.
		 *
		 * @throws IllegalArgumentException If {@code selection} and {@code bulkMutate} are both non-<jk>null</jk>
		 * 	but {@code selection} is not {@code bulkMutate.selection()}.
		 */
		RenderOptions reconciled() {
			if (bulkMutate == null)
				return this;
			if (selection != null && selection != bulkMutate.selection())
				throw iaex("selection must be exactly bulkMutate.selection() when both are supplied; "
					+ "a BulkMutateDef can only render the SelectionDef it was constructed against.");
			return new RenderOptions(rows, csrfToken, bulkMutate.selection(), bulkMutate, savedViewsBase, idQualifier);
		}
	}

	/**
	 * Emits the one {@code <template data-juneau-row-detail>} sibling: empty field slots, {@link ActionRef}
	 * buttons initially disabled, {@link org.apache.juneau.rest.server.widgets.SafeAction#COLLAPSE} enabled.
	 * Labels are HtmlBuilder text children (never poured in as markup).
	 *
	 * <p>
	 * {@code csrfToken} is the enclosing response's token; it is painted onto any nested table in this template so a
	 * nested row action can submit with it.  Once that token has rotated the nested action fails closed through the
	 * ordinary 403 path &mdash; a nested table never mints or refreshes a token of its own.
	 *
	 * <p>
	 * A declared {@link RowDetailDef#barSlot} adds two children: the region, at whichever of the two anchors applies
	 * ({@link BarSlotTable#ANCHOR_RIBBON} for a multi-section detail, {@link BarSlotTable#ANCHOR_SECTION_TITLE} for a
	 * single-section one), and its {@code id}-less {@link BarSlotTable#detailSidecar sidecar}.  Neither reaches the
	 * archived {@code .juneau-view-toolbar-*} control row and neither is a nav tab.
	 */
	private static Template emitDetailTemplate(ViewDef viewDef, String csrfToken) {
		var d = viewDef.details;
		var children = new ArrayList<>();
		if (hasDetailHeader(d))
			children.add(emitDetailHeader(d, viewDef.rowActions));
		// The detail ribbon is assembled CLIENT-side, and only from two or more sections, so the bar-slot region has
		// two server anchors: a ribbon-anchored last direct child the runtime relocates, or - with no ribbon to trail
		// and none synthesized for it - a section-title-anchored child of the lone section.
		var ribbonAnchored = d.barSlot != null && d.sections.size() > 1;
		var sectionAnchored = d.barSlot != null && !ribbonAnchored;
		for (var s : d.sections)
			children.add(buildDetailSection(viewDef, d, s, sectionAnchored, csrfToken));
		if (ribbonAnchored)
			children.add(BarSlotTable.detailRegion(d.barSlot, BarSlotTable.ANCHOR_RIBBON));
		// The sidecar is id-less and found by attribute, exactly like the nested-table VIEW_META sidecar above: this
		// whole subtree is cloned per expanded row, so the runtime mints the document-unique id.
		if (d.barSlot != null)
			children.add(BarSlotTable.detailSidecar(d.barSlot));
		return template()
			.attr(DETAIL_TEMPLATE_ATTR, "1")
			.attr(DETAIL_CONTRACT_ATTR, RowDetailDef.CONTRACT_VERSION)
			.attr(DETAIL_URL_ATTR, d.endpoint)
			.children(children.toArray());
	}

	/**
	 * Builds one detail {@code <section>}: the title, an optional section-anchored bar-slot region, an optional
	 * action bar, the fields grid, and &mdash; appended last, after the fields grid &mdash; an optional nested
	 * table (the runtime instantiates it only once the detail GET succeeds and this section's pane is visible).
	 */
	private static Section buildDetailSection(ViewDef viewDef, RowDetailDef d, DetailSection s,
			boolean sectionAnchored, String csrfToken) {
		var kids = new ArrayList<>();
		kids.add(h2(s.title == null || s.title.isBlank() ? s.id : s.title)
			.class_("juneau-view-detail-section-title"));
		if (sectionAnchored)
			kids.add(BarSlotTable.detailRegion(d.barSlot, BarSlotTable.ANCHOR_SECTION_TITLE));
		if (hasActionBarItems(s.actions))
			kids.add(emitActionBar(s.actions, viewDef.rowActions));
		kids.add(buildFieldsGrid(s, viewDef.rowActions));
		if (s.table != null)
			kids.add(emitNestedTable(s.table, csrfToken));
		var out = section(kids.toArray())
			.attr(DETAIL_SECTION_ATTR, s.id)
			.class_("juneau-view-detail-section");
		if (s.count != null)
			out.attr(DETAIL_COUNT_ATTR, s.count.toString());
		return out;
	}

	/**
	 * Builds a section's fields grid: one empty field slot per {@link DetailSection#fields} entry.
	 *
	 * <p>
	 * The column count is a class rather than an inline {@code grid-template-columns}, because an inline style
	 * cannot be stepped down by a container query &mdash; it out-ranks every rule in the stylesheet, so a
	 * three-column section declared here would stay three columns in a 320px-wide panel.  The class names the
	 * author's <b>cap</b>; {@code juneau-views.css} decides how many of those columns a given panel width can
	 * actually afford.  The ladder tops out at {@value #DETAIL_MAX_COLUMNS}.
	 */
	private static Div buildFieldsGrid(DetailSection s, List<RowAction> rowActions) {
		var fieldSlots = new ArrayList<>();
		if (s.fields != null)
			for (var f : s.fields)
				fieldSlots.add(emitDetailField(f, rowActions));
		var cols = Math.min(Math.max(s.columns, 1), DETAIL_MAX_COLUMNS);
		var layout = s.layout == FieldLayout.STACKED ? "stacked" : "inline";
		return div(fieldSlots.toArray())
			.class_("juneau-view-detail-fields juneau-view-detail-fields-" + layout
				+ " juneau-view-detail-fields-cols-" + cols);
	}

	/** Whether an {@link ActionBar} has at least one item to render (a <jk>null</jk> bar has none). */
	private static boolean hasActionBarItems(ActionBar bar) {
		return bar != null && bar.items != null && !bar.items.isEmpty();
	}

	private static boolean hasDetailHeader(RowDetailDef d) {
		var titled = d.title != null && !d.title.isBlank();
		var icon = d.icon != null && !d.icon.isBlank();
		return titled || icon || hasActionBarItems(d.headerActions);
	}

	private static Div emitDetailHeader(RowDetailDef d, List<RowAction> rowActions) {
		var kids = new ArrayList<>();
		if (d.icon != null && !d.icon.isBlank())
			kids.add(span().attr(DETAIL_ICON_ATTR, d.icon).class_("juneau-view-detail-icon"));
		if (d.title != null && !d.title.isBlank())
			kids.add(h2(d.title)
				.attr(DETAIL_TITLE_ATTR, "1")
				.attr(DETAIL_TITLE_TEMPLATE_ATTR, d.title)
				.class_("juneau-view-detail-title"));
		if (hasActionBarItems(d.headerActions))
			kids.add(emitActionBar(d.headerActions, rowActions));
		return div(kids.toArray()).class_("juneau-view-detail-header").attr(DETAIL_HEADER_ATTR, "1");
	}

	/**
	 * Emits the nested-table shell inside a detail section: a {@code data-juneau-view} {@code <table>} with a
	 * {@code <thead>} of the nested view's column titles (no HTML {@code id} &mdash; a {@code <template>} clone would
	 * collide; the runtime mints a row-qualified one per expanded row), plus a sibling, {@code id}-less VIEW_META
	 * sidecar found via {@link #NESTED_META_ATTR}.  The wrapper carries the marker, the independent
	 * {@link NestedTableDef#CONTRACT_VERSION}, and the parent-scope parameter name.
	 *
	 * <p>
	 * The nested table gets the same leading synthetic header cells the enclosing table gets &mdash; an expander cell
	 * when the nested view declares its own detail sections, then a selection cell when {@link NestedTableDef#selection}
	 * is declared &mdash; plus its own row-detail {@code <template>} when it declares one.  It deliberately gets
	 * <b>no</b> column-chooser host and <b>no</b> bulk sidecar: both stay bound to the enclosing table's id, so two
	 * expanded rows share one parent affordance instead of minting one per nested table.
	 *
	 * <p>
	 * {@code csrfToken} is the enclosing response's token.  When it is absent (a non-request {@code of(...)} overload
	 * emitted this shell) the nested sidecar is serialized with {@link ViewDef#rowActions} withheld, so the runtime
	 * cannot paint an action affordance that has no token to submit with &mdash; the fail-closed half of the token
	 * contract, one step earlier than the runtime's own visible refusal.
	 */
	private static Div emitNestedTable(NestedTableDef nt, String csrfToken) {
		var v = nt.view;
		var cols = v.columns == null ? List.<Column>of() : v.columns;
		var headerCells = new ArrayList<>(cols.size() + 2);
		if (v.details != null)
			headerCells.add(th().attr(CLASS_ATTR, DETAIL_TH_CLASS).attr(ARIA_LABEL_ATTR, "Expand"));
		if (nt.selection != null)
			headerCells.add(th().attr(CLASS_ATTR, "juneau-view-select-th").attr(ARIA_LABEL_ATTR, "Select"));
		for (var c : cols)
			headerCells.add(th(c.title == null ? c.data : c.title));
		var table = table(thead(tr(headerCells.toArray()))).attr(MARKER_ATTR, v.id).class_(TABLE_CLASS);

		var tokenless = csrfToken == null || csrfToken.isBlank();
		if (! tokenless)
			table.attr(CSRF_ATTR, csrfToken);

		if (nt.selection != null) {
			table.attr(SELECT_ATTR, "1");
			table.attr(ROW_ID_FIELD_ATTR, nt.selection.rowIdField());
			table.attr(SELECT_ALL_ATTR, nt.selection.selectAll() ? "1" : "0");
		}

		// Sidecar: same VIEW_META contract as a top-level view; neutralize break-outs, insert as RAW (class javadoc).
		var sidecar = script().type(JSON_CONTENT_TYPE).attr(NESTED_META_ATTR, v.id)
			.text(rawText(escapeForScript(nestedJson(v, tokenless))));

		var children = new ArrayList<>();
		children.add(table);
		if (v.details != null)
			children.add(emitDetailTemplate(v, csrfToken));
		children.add(sidecar);

		return div(children.toArray())
			.class_("juneau-view-detail-nested")
			.attr(NESTED_ATTR, "1")
			.attr(NESTED_CONTRACT_ATTR, NestedTableDef.CONTRACT_VERSION)
			.attr(NESTED_SCOPE_PARAM_ATTR, nt.parentScopeParam);
	}

	/**
	 * Serializes a nested view's VIEW_META, withholding {@link ViewDef#rowActions} on the token-less path.
	 *
	 * <p>
	 * A shared {@link ViewDef} may be rendered concurrently, so the withhold is a guarded
	 * mutate&rarr;serialize&rarr;restore window (the same discipline the {@code $FV} chrome resolution uses) rather
	 * than a lasting edit of the author's definition.
	 */
	private static String nestedJson(ViewDef v, boolean tokenless) {
		if (! tokenless || v.rowActions == null)
			return Json.of(v);
		synchronized (v.lock) {
			var restore = v.rowActions;
			v.rowActions = null;
			try {
				return Json.of(v);
			} finally {
				v.rowActions = restore;
			}
		}
	}

	/**
	 * Builds one empty field slot: a title div plus a value div, whatever the section's {@link FieldLayout} is
	 * &mdash; the arrangement is a property of the grid, so it is styled from the grid's class rather than
	 * changing the shape emitted here.
	 *
	 * <p>
	 * A {@link DetailField#actions} bar is appended as a plain <b>sibling</b> of the value slot rather than the two
	 * being wrapped together.  That keeps the {@code [data-juneau-field]} node exactly where the expand-fill
	 * painter already looks for it, so a field-hosted bar needs no runtime wiring of its own: the fill path never
	 * sees it, and the panel-scoped {@code [data-juneau-action]} lifecycles reach it unchanged.  Seating the third
	 * child in the value column is the stylesheet's job.
	 */
	private static Div emitDetailField(DetailField f, List<RowAction> rowActions) {
		var rendered = f.render != null;
		var markdown = !rendered && f.format == DetailField.Format.MARKDOWN;
		// A markdown field spans by default rather than by a parallel hardcoded CSS rule that happens to do the
		// same thing by a different route -- one mechanism, one job.
		var span = markdown || f.span == FieldSpan.FULL ? " juneau-view-detail-field-span-full" : "";
		var valueSlot = div().attr(DETAIL_FIELD_ATTR, f.data);
		if (rendered) {
			valueSlot.attr(DETAIL_FIELD_RENDER_ATTR, f.render.id);
			if (f.render.meta != null && !f.render.meta.isEmpty())
				valueSlot.attr(DETAIL_FIELD_RENDER_META_ATTR, Json.of(f.render.meta));
			if (f.href != null)
				valueSlot.attr(DETAIL_FIELD_RENDER_HREF_ATTR, f.href);
			valueSlot.class_("juneau-view-detail-field-value");
		} else if (markdown) {
			valueSlot.attr(DETAIL_FIELD_FORMAT_ATTR, DetailField.Format.MARKDOWN.wire());
			valueSlot.class_("juneau-view-detail-field-value juneau-view-detail-markdown jc-prose");
		} else {
			valueSlot.class_("juneau-view-detail-field-value");
		}
		var hideTitle = markdown && f.title != null && f.title.isEmpty();
		var kids = new ArrayList<>();
		if (! hideTitle) {
			var label = f.title == null || f.title.isBlank() ? f.data : f.title;
			kids.add(div(label).class_("juneau-view-detail-field-title"));
		}
		kids.add(valueSlot);
		if (hasActionBarItems(f.actions))
			kids.add(emitActionBar(f.actions, rowActions));
		return div(kids.toArray())
			.class_((markdown ? "juneau-view-detail-field juneau-view-detail-field-markdown" : "juneau-view-detail-field") + span);
	}

	private static Div emitActionBar(org.apache.juneau.rest.server.widgets.ActionBar bar, List<RowAction> rowActions) {
		var buttons = new ArrayList<>();
		for (var item : bar.items) {
			if (item instanceof org.apache.juneau.rest.server.widgets.ActionRef ar) {
				var label = actionLabel(ar.id, rowActions);
				var cls = ar.emphasis == org.apache.juneau.rest.server.widgets.ActionRef.Emphasis.PRIMARY
					? "juneau-view-detail-action juneau-view-detail-action-primary"
					: "juneau-view-detail-action";
				var btn = button("button", label)
					.attr(DETAIL_ACTION_ATTR, ar.id)
					.attr(CLASS_ATTR, cls)
					.disabled(true);
				var gated = ar.enabledWhen != null && !ar.enabledWhen.isEmpty();
				if (gated)
					btn.attr(DETAIL_ACTION_RULES_ATTR, Json.of(enabledRulesJson(ar)));
				buttons.add(btn);
				// The reason node is a sibling rather than a child so the button's accessible NAME stays the
				// label: this is a description, and aria-describedby is how it is reached.
				if (gated)
					buttons.add(span().attr(DETAIL_ACTION_DESC_ATTR, ar.id).attr("hidden", "hidden"));
			} else if (item instanceof org.apache.juneau.rest.server.widgets.SafeAction sa) {
				buttons.add(button("button", sa.label())
					.attr(DETAIL_SAFE_ATTR, sa.wire())
					.attr(CLASS_ATTR, "juneau-view-detail-action juneau-view-detail-safe"));
			}
		}
		return div(buttons.toArray()).class_("juneau-view-detail-actions");
	}

	/**
	 * Renders an {@link ActionRef}'s rules as the list this emitter JSON-encodes into
	 * {@link #DETAIL_ACTION_RULES_ATTR}, preserving declaration order.
	 *
	 * <p>
	 * The operator travels as its lowercase wire token rather than the enum name, matching the {@code op} token the
	 * row-decorator rules already put on the wire, and {@code value} is omitted rather than emitted as {@code null}
	 * for the presence-based operators that do not take one.
	 */
	private static List<java.util.Map<String,Object>> enabledRulesJson(org.apache.juneau.rest.server.widgets.ActionRef ar) {
		var out = new ArrayList<java.util.Map<String,Object>>();
		for (var r : ar.enabledWhen) {
			var m = new LinkedHashMap<String,Object>();
			m.put("field", r.field);
			m.put("op", r.op.wire());
			if (r.value != null)
				m.put("value", r.value);
			m.put("reason", r.reason);
			out.add(m);
		}
		return out;
	}

	private static String actionLabel(String id, List<RowAction> rowActions) {
		if (rowActions != null) {
			for (var a : rowActions) {
				if (a != null && id.equals(a.id) && a.label != null && !a.label.isBlank())
					return a.label;
			}
		}
		return id;
	}

	/** Reads the boundary-stamped CSRF token off the request, or {@code null} when absent. */
	private static String csrfToken(HttpServletRequest req) {
		if (req == null)
			return null;
		var v = req.getAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE);
		return v == null ? null : v.toString();
	}

	/**
	 * Resolves the saved-views REST base when {@code req} is a {@link RestRequest}; otherwise {@code null}
	 * (a plain {@link HttpServletRequest} has no URI resolver).
	 */
	private static String savedViewsBase(HttpServletRequest req) {
		if (!(req instanceof RestRequest rr))
			return null;
		return SavedViewsMixin.resolvedBaseUrl(rr);
	}

	/**
	 * Builds a per-response <b>sibling</b> {@link VarResolverSession} carrying a fresh {@link ServerValuesRegistry}.
	 *
	 * <p>
	 * Mirrors {@link RestRequest#getVarResolverSession()}'s recipe (same {@link RestContext} resolver, same
	 * request-scoped {@code RestRequest}/{@code RestSession} beans, request bean store as parent) but never mutates
	 * the cached request session, so provider values cannot leak across requests.
	 *
	 * <p>
	 * Package-private rather than private because {@link PageTable} hosts {@link PageDef#serverValues} on the same
	 * terms: sharing this one recipe is what keeps the sibling sessions from drifting apart.
	 */
	@SuppressWarnings({
		"resource" // False positive: RestSession/RestContext owns the parent BeanStore; this sibling session must not close it.
	})
	static VarResolverSession serverValuesSession(RestRequest rr, ServerValues serverValues) {
		var restSession = rr.getVarResolverSession().getBean(RestSession.class).orElse(null);
		var parentStore = restSession != null ? restSession.getBeanStore() : rr.getContext().getBeanStore();
		var s = rr.getContext().getVarResolver().createSession(parentStore).bean(RestRequest.class, rr);
		if (restSession != null)
			s.bean(RestSession.class, restSession);
		return s.bean(ServerValuesRegistry.class, ServerValuesRegistry.of(serverValues));
	}

	/**
	 * Resolves the {@code $FV} chrome fields (the closed title/label list: {@link Column#title},
	 * {@link RowAction#label}, {@link RibbonAction#title} and its {@link RibbonAction.Opt#title}) in place on the
	 * shared {@code viewDef} so both the painted chrome and the VIEW_META sidecar carry the same resolved strings
	 * (W1).  Returns a {@link Runnable} that restores every mutated field to its author {@code $FV{...}} template.
	 */
	private static Runnable resolveChrome(ViewDef viewDef, VarResolverSession session) {
		var restores = new ArrayList<Runnable>();
		resolveColumnsChrome(restores, session, viewDef.columns);
		resolveRowActionsChrome(restores, session, viewDef.rowActions);
		resolveRibbonChrome(restores, session, viewDef.ribbon);
		return lifoRestore(restores);
	}

	/** Resolves every declared column's {@link Column#title}. */
	private static void resolveColumnsChrome(List<Runnable> restores, VarResolverSession session, List<Column> columns) {
		if (columns == null)
			return;
		for (var c : columns)
			if (c != null)
				resolveField(restores, session, c.title, v -> c.title = v);
	}

	/** Resolves every declared row action's {@link RowAction#label}. */
	private static void resolveRowActionsChrome(List<Runnable> restores, VarResolverSession session,
			List<RowAction> rowActions) {
		if (rowActions == null)
			return;
		for (var a : rowActions)
			if (a != null)
				resolveField(restores, session, a.label, v -> a.label = v);
	}

	/** Resolves every declared ribbon action's {@link RibbonAction#title} and its options' {@link RibbonAction.Opt#title}. */
	private static void resolveRibbonChrome(List<Runnable> restores, VarResolverSession session,
			List<RibbonAction> ribbon) {
		if (ribbon == null)
			return;
		for (var r : ribbon) {
			if (r == null)
				continue;
			resolveField(restores, session, r.title, v -> r.title = v);
			if (r.options != null)
				for (var o : r.options)
					if (o != null)
						resolveField(restores, session, o.title, v -> o.title = v);
		}
	}

	/**
	 * Resolves the row-detail panel's own {@code $FV} chrome (the closed title list: {@link RowDetailDef#title},
	 * {@link DetailSection#title}, {@link DetailField#title}) in place on the shared {@code detail}, so the
	 * server-emitted {@code <template>} below is painted with the resolved strings and the expand GET is left
	 * carrying row data only.
	 *
	 * <p>
	 * Deliberately narrower than it could be: {@link RowDetailDef#icon} is an icon-registry name,
	 * {@link org.apache.juneau.rest.server.widgets.ActionRef} is an id, and
	 * {@link org.apache.juneau.rest.server.widgets.SafeAction} is an enum, so none of them are interpolated.  The
	 * allowlist is hard-coded on purpose &mdash; there is no reflective bean walk and no author-extensible field set.
	 *
	 * @return A {@link Runnable} restoring every mutated field to its author {@code $FV{...}} template, LIFO.
	 */
	private static Runnable resolveDetailChrome(RowDetailDef detail, VarResolverSession session) {
		var restores = new ArrayList<Runnable>();
		resolveField(restores, session, detail.title, v -> detail.title = v);
		resolveDetailSectionsChrome(restores, session, detail.sections);
		return lifoRestore(restores);
	}

	/** Resolves every declared section's {@link DetailSection#title} and, in turn, each of its fields' titles. */
	private static void resolveDetailSectionsChrome(List<Runnable> restores, VarResolverSession session,
			List<DetailSection> sections) {
		if (sections == null)
			return;
		for (var s : sections) {
			if (s == null)
				continue;
			resolveField(restores, session, s.title, v -> s.title = v);
			resolveDetailFieldsChrome(restores, session, s.fields);
		}
	}

	/** Resolves every declared field's {@link DetailField#title}. */
	private static void resolveDetailFieldsChrome(List<Runnable> restores, VarResolverSession session,
			List<DetailField> fields) {
		if (fields == null)
			return;
		for (var f : fields)
			if (f != null)
				resolveField(restores, session, f.title, v -> f.title = v);
	}

	/**
	 * Resolves one chrome field through {@code session}; on change, applies the resolved value and records a restore.
	 *
	 * <p>
	 * Package-private so {@link PageTable}'s host resolves its own allowlist through the identical rule &mdash;
	 * including the cheap {@code indexOf('$')} skip, which is what keeps a field with no template from paying for a
	 * resolve or recording a restore.
	 */
	static void resolveField(List<Runnable> restores, VarResolverSession session, String current,
			Consumer<String> setter) {
		if (current == null || current.indexOf('$') < 0)
			return;
		var resolved = session.resolve(current);
		if (Objects.equals(resolved, current))
			return;
		setter.accept(resolved);
		restores.add(() -> setter.accept(current));
	}

	/**
	 * Wraps recorded restores into a single strictly-LIFO undo, so a field touched twice ends on its author value.
	 *
	 * <p>
	 * Package-private for the same reason as {@link #resolveField}: every host unwinds its window identically.
	 */
	static Runnable lifoRestore(List<Runnable> restores) {
		return () -> {
			for (var i = restores.size() - 1; i >= 0; i--)
				restores.get(i).run();
		};
	}

	/** Reads a column value from a row: a direct key lookup for a {@code Map}, a bean-property read otherwise. */
	private static Object value(MarshallingContext ctx, Object row, String key) {
		if (row instanceof java.util.Map<?,?> m)
			return m.get(key);
		return ctx.toBeanMap(row).get(key);
	}
}
