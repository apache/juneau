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
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.vars.*;
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
 * <h5 class='section'>Page layout vs HTML renderer:</h5>
 * <p>
 * A table that <i>is</i> a page body is {@link ViewSlot#envelope(RestRequest, ViewDef)} plus
 * {@code JuneauViews.regions.mount({ id: { table: url } })}.  {@code of} is the HTML renderer for tests,
 * nested emit, and deprecated {@link PageTable}/{@link CardGridTable} children.  It is not
 * {@code @Deprecated}: the renderer and those hosts still call it.
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link ViewSlot}
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
	 * Marker attribute on the nested-table wrapper {@code <div>} (value {@code "1"}).
	 *
	 * <p>
	 * Nested-table seeding inside a row-detail panel is deferred (F24); this marker remains so a later host can
	 * skip auto-init of a nested {@code data-juneau-view} table until its pane is visible.
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param req The current request.  Must not be <jk>null</jk>.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	public static Div of(RestRequest req, ViewDef viewDef) {
		return of((HttpServletRequest) req, viewDef);
	}

	/**
	 * Builds the view-table shell with no request in hand, resolving {@code $L} chrome against a caller-supplied,
	 * already-locale-bound {@link Messages} bean (view-def string i18n, LD-4: the request-free localization seam).
	 *
	 * <p>
	 * There is no {@link RestRequest} on this path, so the CSRF auto-embed, saved-views stamp, and {@code $FV}
	 * server-values resolution the request-bearing overloads provide are not available here &mdash; only
	 * {@code $L{key}} chrome (see {@link Column#titleKey}) resolves, against {@code messages}. {@code $L} resolved
	 * this way is non-recursive (a resolved bundle value is emitted literally, never re-parsed as SVL); with
	 * {@code messages} <jk>null</jk>, or for any chrome field with no {@code $L{...}} template, this is exactly
	 * {@link #of(ViewDef) of(viewDef)} &mdash; no resolution, no lock, byte-identical output.
	 *
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
	 *
	 * @param messages The locale-bound message bundle to resolve {@code $L{...}} chrome against, or <jk>null</jk>
	 * 	for none.
	 * @param viewDef The built view definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view>} and the JSON sidecar.
	 */
	public static Div of(Messages messages, ViewDef viewDef) {
		return emit(MarshallingContext.DEFAULT, viewDef, null, messages,
			new RenderOptions(null, null, null, null, null, null));
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * The RestRequest counterpart of {@link #of(HttpServletRequest, ViewDef, Collection)}.  HTML renderer, not a
	 * page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)} plus
	 * {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * <p>
	 * HTML renderer, not a page-body factory.  Page-layout callers use {@link ViewSlot#envelope(RestRequest, ViewDef)}
	 * plus {@code JuneauViews.regions.mount({ id: { table: url } })}.  Not {@code @Deprecated}.
	 * </p>
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
	 * Runs {@code action} inside the same {@code $FV}/{@code $L} chrome-resolution window
	 * {@link #of(RestRequest, ViewDef)} uses, then restores author templates before returning.
	 *
	 * <p>
	 * Package-private so {@link ViewSlot} can snapshot resolved titles onto the slot envelope without inventing a
	 * second chrome host.  {@code action} must not retain live builder beans that would observe the restore.
	 *
	 * @param <T> The snapshot type {@code action} returns.
	 * @param viewDef The view being serialized.  Must not be <jk>null</jk>.
	 * @param req The current request, or <jk>null</jk>.
	 * @param messages A locale-bound bundle for the request-free {@code $L} seam, or <jk>null</jk>.
	 * @param action Runs while resolved chrome is visible on {@code viewDef} (and its detail, when set).
	 * @return Whatever {@code action} returns.
	 */
	static <T> T withResolvedChrome(ViewDef viewDef, RestRequest req, Messages messages, Supplier<T> action) {
		var detail = viewDef.details;
		if (detail != null) {
			synchronized (detail.lock) {
				if (detailChromeHasVar(detail)) {
					var session = chromeSession(req, detail.serverValues, messages);
					if (session != null) {
						var restore = resolveDetailChrome(detail, session);
						try {
							return withResolvedViewChrome(viewDef, req, messages, action);
						} finally {
							restore.run();
						}
					}
				}
			}
		}
		return withResolvedViewChrome(viewDef, req, messages, action);
	}

	private static <T> T withResolvedViewChrome(ViewDef viewDef, RestRequest req, Messages messages, Supplier<T> action) {
		synchronized (viewDef.lock) {
			if (chromeHasVar(viewDef)) {
				var session = chromeSession(req, viewDef.serverValues, messages);
				if (session != null) {
					var restore = resolveChrome(viewDef, session);
					try {
						return action.get();
					} finally {
						restore.run();
					}
				}
			}
		}
		return action.get();
	}

	/**
	 * The shared core, request/{@code Messages}-free overload.  Delegates to the {@code messages}-carrying core
	 * with no message bundle, so every pre-existing caller keeps its exact current behavior.
	 */
	private static Div emit(MarshallingContext ctx, ViewDef viewDef, HttpServletRequest req, RenderOptions opts) {
		return emit(ctx, viewDef, req, null, opts);
	}

	/**
	 * The shared core.  Validates and reconciles selection/bulk-mutate, then &mdash; when a chrome-resolution
	 * session is available (see {@link #chromeSession}) <b>and</b> that host's allowlisted chrome actually
	 * contains a {@code $}-prefixed template (the pre-scan, view-def string i18n LD-1) &mdash; resolves
	 * that host's declared chrome (titles/labels) against a per-response <b>sibling</b> {@link VarResolverSession}
	 * before building both the painted {@code <th>}/labels and the VIEW_META sidecar, so the two agree (W1).  The
	 * resolved chrome is a per-response snapshot: each host's author {@code $FV{...}}/{@code $L{...}} templates
	 * are restored once this response's markup has captured the resolved strings.
	 *
	 * <p>
	 * Resolution is no longer conditioned on {@link ViewDef#serverValues} being declared (LD-1): the pre-scan
	 * alone gates it, so a def whose chrome carries only {@code $L{...}} (localization, no server-values provider)
	 * is resolved exactly like one that also declares {@code serverValues}.  A def with no {@code $} anywhere in
	 * its allowlisted chrome is never resolved and never mutated &mdash; the {@code indexOf('$') < 0}
	 * guard in {@link #resolveField} plus this pre-scan mean that is structural, not merely expected (byte
	 * stability).  The pre-scan itself runs <b>under that host's monitor</b>, because it reads the same fields
	 * resolution mutates in place: scanned unlocked, it can observe a concurrent response's resolved chrome, find
	 * no template, and then build the shared def with no guard at all.  A def found template-free under the
	 * monitor is one no response can mutate, so it is built after releasing it.
	 *
	 * <h5 class='section'>Two hosts, one written lock order:</h5>
	 * <p>
	 * Two definitions reachable from here can host chrome resolution: {@link ViewDef} (the shipped host, whose
	 * chrome is the column/action/ribbon titles) and {@link RowDetailDef} (the row-detail panel's own titles,
	 * painted into the {@code <template>} below).  They are resolved in the order
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
	private static Div emit(MarshallingContext ctx, ViewDef viewDef, HttpServletRequest req, Messages messages,
			RenderOptions opts) {
		viewDef.validate();
		opts = opts.reconciled();

		var rr = req instanceof RestRequest r ? r : null;
		var detail = viewDef.details;
		if (detail != null) {
			// The row-detail host takes its lock OUTSIDE the view host's, per the written order.
			synchronized (detail.lock) {
				if (detailChromeHasVar(detail)) {
					var session = chromeSession(rr, detail.serverValues, messages);
					if (session != null) {
						var restore = resolveDetailChrome(detail, session);
						try {
							return emitViewHost(ctx, viewDef, rr, messages, opts);
						} finally {
							restore.run();
						}
					}
				}
			}
		}
		return emitViewHost(ctx, viewDef, rr, messages, opts);
	}

	/**
	 * The innermost chrome host: {@link ViewDef}'s own column/action/ribbon titles resolved around {@link #build}.
	 *
	 * <p>
	 * A shared {@link ViewDef} may be rendered concurrently, so the mutate-serialize-restore window is guarded and
	 * two responses cannot interleave resolved chrome onto the same instance.
	 */
	private static Div emitViewHost(MarshallingContext ctx, ViewDef viewDef, RestRequest req, Messages messages,
			RenderOptions opts) {
		synchronized (viewDef.lock) {
			if (chromeHasVar(viewDef)) {
				var session = chromeSession(req, viewDef.serverValues, messages);
				if (session != null) {
					var restore = resolveChrome(viewDef, session);
					try {
						return build(ctx, viewDef, opts);
					} finally {
						restore.run();
					}
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
			children.add(emitDetailTemplate(viewDef));
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
	 * Emits the one {@code <template data-juneau-row-detail>} sibling: header chrome plus exactly one empty
	 * region container.  Labels are HtmlBuilder text children (never poured in as markup).
	 *
	 * <p>
	 * A declared {@link RowDetailDef#barSlot} adds two children: the region, anchored at
	 * {@link BarSlotTable#ANCHOR_SECTION_TITLE} (a region panel has no framework ribbon), and its {@code id}-less
	 * {@link BarSlotTable#detailSidecar sidecar}.  Neither reaches the archived {@code .juneau-view-toolbar-*}
	 * control row and neither is a nav tab.
	 */
	private static Template emitDetailTemplate(ViewDef viewDef) {
		var d = viewDef.details;
		var children = new ArrayList<>();
		if (hasDetailHeader(d))
			children.add(emitDetailHeader(d));
		// Chrome plus EXACTLY ONE empty region container.  No section frames, no field slots, and no framework
		// strip: the author paints the body (and their own strip via helpers.tabStrip if they want one).
		//
		// No CSRF stamp on this container: a row-detail region sits beneath the view table, which already
		// carries the response's token, and a second stamp on the same subtree is the F22 hazard rather than the
		// fix for it.  The non-table hosts (card/tab bodies) are the ones that need it.
		children.add(RegionTable.of(projectDetailRegion(d), null));
		if (d.barSlot != null)
			children.add(BarSlotTable.detailRegion(d.barSlot, BarSlotTable.ANCHOR_SECTION_TITLE));
		children.add(RegionTable.detailSidecar(List.of(projectDetailRegion(d))));
		if (d.barSlot != null)
			children.add(BarSlotTable.detailSidecar(d.barSlot));
		var tpl = template()
			.attr(DETAIL_TEMPLATE_ATTR, "1")
			.attr(DETAIL_CONTRACT_ATTR, RowDetailDef.CONTRACT_VERSION)
			.attr(DETAIL_URL_ATTR, d.endpoint)
			.children(children.toArray());
		if (d.region != null && d.region.titleFields != null && !d.region.titleFields.isEmpty())
			tpl.attr(DETAIL_TITLE_FIELDS_ATTR, String.join(",", d.region.titleFields));
		return tpl;
	}

	/**
	 * Projects {@link RowDetailDef#endpoint} onto the panel's one region as its {@link RegionDef#dataUrl}, unless the
	 * author set one explicitly.
	 *
	 * <p>
	 * Without this projection the flagship shape paints <b>nothing</b>: the Java sets an {@code endpoint} and no
	 * {@code dataUrl}, the JS does {@code ctx.data ?? await ctx.fetchDeclared()}, and {@code fetchDeclared()}
	 * resolves {@code null} for an unset {@code dataUrl} &mdash; so the region has no payload and the panel is blank.
	 * Projecting the endpoint is what makes {@code ctx.declared.dataUrl} non-null for a row-detail region under a
	 * {@link RowDetailDef} that has an endpoint.
	 *
	 * <p>
	 * The projected value is the endpoint <b>template</b>, {@code {id}} and all: row substitution is the client's
	 * existing {@code substituteDetailUrl} / {@code isSafeDetailUrl} path, which already runs per expanded row, and
	 * the server has no row to substitute at emit time.  Because the projected URL is then <i>equal to</i> the
	 * panel's own substituted endpoint, the region's declared fetch <b>joins</b> the panel's in-flight expand
	 * envelope instead of issuing a second request &mdash; one GET per panel, and chrome and content read the same
	 * body.  An author who sets an explicit {@code dataUrl} opts out and gets a genuinely separate GET; that is the
	 * priced opt-out, and it is the only one (there is deliberately no {@code shared} flag).
	 *
	 * <p>
	 * Returns a <b>copy</b> rather than mutating the author's bean: this runs on every emit, and writing a projected
	 * {@code dataUrl} back onto a shared {@link RegionDef} would make the author's own "did I set this?" state
	 * depend on how many times the view had been served.
	 *
	 * @param d The detail definition.  Must declare a {@link RowDetailDef#region}.
	 * @return The region to emit, with {@code dataUrl} projected when the author left it unset.
	 */
	private static RegionDef projectDetailRegion(RowDetailDef d) {
		var r = d.region;
		if (r.dataUrl != null && ! r.dataUrl.isBlank())
			return r;
		var copy = RegionDef.create(r.id);
		copy.populate = r.populate;
		copy.type = r.type;
		copy.dataUrl = d.endpoint;
		copy.params = r.params;
		copy.renderer = r.renderer;
		copy.lazy = r.lazy;
		copy.refreshMs = r.refreshMs;
		copy.fields = r.fields;
		copy.titleFields = r.titleFields;
		copy.allowedPopulators = r.allowedPopulators;
		return copy;
	}

	/** Whether an expander panel should emit a header (title and/or icon). */
	private static boolean hasDetailHeader(RowDetailDef d) {
		var titled = d.title != null && !d.title.isBlank();
		var icon = d.icon != null && !d.icon.isBlank();
		return titled || icon;
	}

	private static Div emitDetailHeader(RowDetailDef d) {
		var kids = new ArrayList<>();
		if (d.icon != null && !d.icon.isBlank())
			kids.add(span().attr(DETAIL_ICON_ATTR, d.icon).class_("juneau-view-detail-icon"));
		if (d.title != null && !d.title.isBlank())
			kids.add(h2(d.title)
				.attr(DETAIL_TITLE_ATTR, "1")
				.attr(DETAIL_TITLE_TEMPLATE_ATTR, d.title)
				.class_("juneau-view-detail-title"));
		return div(kids.toArray()).class_("juneau-view-detail-header").attr(DETAIL_HEADER_ATTR, "1");
	}

	/**
	 * Reads the boundary-stamped CSRF token off the request, or {@code null} when absent.
	 *
	 * <p>
	 * {@link RestRequest#getAttribute(String)} covariantly narrows the servlet contract to a
	 * {@link org.apache.juneau.rest.server.httppart.RequestAttribute} wrapper: it is never {@code null} (an absent
	 * attribute yields a wrapper holding a {@code null} value) and its {@code toString()} is
	 * {@code "name=value"}.  Reading it through the {@link HttpServletRequest} declaration would stamp that whole
	 * name/value pair as the token &mdash; and stamp a non-blank placeholder even when there is no token at all,
	 * defeating the runtime's fail-closed refusal.  Aiming the read at the wrapped servlet request instead makes
	 * both request flavors yield exactly the value {@link LoopbackBoundaryFilter} published.
	 */
	private static String csrfToken(HttpServletRequest req) {
		if (req == null)
			return null;
		var req2 = req instanceof RestRequest req3 ? req3.getHttpServletRequest() : req;
		var v = req2.getAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE);
		return v == null ? null : v.toString();
	}

	/**
	 * Resolves the saved-views REST base when {@code req} is a {@link RestRequest}; otherwise {@code null}
	 * (a plain {@link HttpServletRequest} has no URI resolver).
	 */
	static String savedViewsBase(HttpServletRequest req) {
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
		var s = chromeResolver(rr.getContext().getVarResolver()).createSession(parentStore).bean(RestRequest.class, rr);
		if (restSession != null)
			s.bean(RestSession.class, restSession);
		// serverValues is null whenever the host declares no $FV provider (view-def string i18n LD-1 decoupled
		// resolution from that condition); with no registry bound, $FV{...} simply reports canResolve()==false
		// and stays literal (ServerValuesVar's own fail-open contract) -- $L{...} localization is unaffected.
		if (serverValues != null)
			s.bean(ServerValuesRegistry.class, ServerValuesRegistry.of(serverValues));
		return s;
	}

	/**
	 * The request-free {@code $L} localization seam (view-def string i18n LD-4): a chrome-resolution session bound
	 * to a caller-supplied, already-locale-bound {@link Messages} bean, with no {@link RestRequest} in hand.
	 * Carries only the chrome-scoped, non-recursive {@code $L} var (LD-2, see {@link #chromeResolver}); there is no
	 * {@link ServerValuesRegistry} on this path, so {@code $FV} chrome does not resolve here (unchanged &mdash;
	 * {@code $FV} has always required a request).
	 */
	private static final VarResolver MESSAGES_ONLY_RESOLVER = VarResolver.create().vars(new ChromeLocalizationVar()).build();

	private static VarResolverSession messagesSession(Messages messages) {
		return MESSAGES_ONLY_RESOLVER.createSession().bean(Messages.class, messages);
	}

	/**
	 * Resolves the chrome-resolution session to use for one host, or {@code null} when there is nothing to resolve
	 * against.  View-def string i18n LD-1 decouples this from {@code serverValues != null}: a
	 * {@link RestRequest} alone is now enough (the pre-scans in {@link #chromeHasVar}/{@link #detailChromeHasVar}
	 * gate whether this is even called).  LD-4 adds the {@code req == null} fallback: with no request, a
	 * caller-supplied {@link Messages} bean still resolves {@code $L{...}}, via {@link #messagesSession}.  With
	 * neither, {@code null} &mdash; the caller skips locking/resolving entirely, so a request-free,
	 * {@code Messages}-free render is exactly as before this item.
	 *
	 * @param req The request, or <jk>null</jk>.
	 * @param serverValues The host's own declared {@code $FV} provider, or <jk>null</jk> (LD-1: no longer gates
	 * 	whether a session is built at all &mdash; only whether {@code $FV} itself can resolve within it).
	 * @param messages A locale-bound message bundle for the {@code req == null} path, or <jk>null</jk>.
	 * @return A session to resolve chrome against, or {@code null} if there is no context to resolve with.
	 */
	static VarResolverSession chromeSession(RestRequest req, ServerValues serverValues, Messages messages) {
		if (req != null)
			return serverValuesSession(req, serverValues);
		if (messages != null)
			return messagesSession(messages);
		return null;
	}

	private static final ConcurrentHashMap<VarResolver, VarResolver> CHROME_RESOLVERS = new ConcurrentHashMap<>();

	/**
	 * Returns a chrome-resolution variant of {@code base} with {@code $L} swapped to {@link ChromeLocalizationVar}
	 * (view-def string i18n LD-2, security).  Scoped to sessions built through this method (and
	 * {@link #MESSAGES_ONLY_RESOLVER}, built the same way) only: {@code base} itself, and any session built
	 * directly from it anywhere else in the framework, keeps {@code $L}'s ordinary recursive default &mdash; this
	 * is not a change to {@link LocalizationVar#allowRecurse()} itself. {@link VarResolver.Builder#copy()} copies
	 * {@code base}'s registered vars in order, and later-registered same-name vars win
	 * ({@code VarResolver}'s own var-map construction), so appending {@link ChromeLocalizationVar} here overrides
	 * exactly {@code base}'s own {@code "L"} registration in the copy, nothing else. Cached per {@code base}
	 * (typically one {@link RestContext}'s memoized resolver) so the swap is paid at most once per resource.
	 */
	private static VarResolver chromeResolver(VarResolver base) {
		return CHROME_RESOLVERS.computeIfAbsent(base, b -> b.copy().vars(new ChromeLocalizationVar()).build());
	}

	/**
	 * {@code $L}, scoped non-recursive (view-def string i18n LD-2, security): a resolved bundle value is emitted
	 * literally and never re-parsed as SVL, matching {@link ServerValuesVar#allowRecurse()}'s own precedent
	 * (untrusted/locale-dependent bundle content must not be recursively resolved). Registered only into the
	 * resolvers {@link #chromeResolver} and {@link #MESSAGES_ONLY_RESOLVER} build for the view-def
	 * chrome-resolution path; {@code $L} used anywhere else in the framework keeps its ordinary recursive default.
	 */
	private static final class ChromeLocalizationVar extends LocalizationVar {
		@Override /* Overridden from Var */
		protected boolean allowRecurse() {
			return false;
		}
	}

	/**
	 * Resolves the {@code $FV} chrome fields (the closed title/label list: {@link Column#title}, its cell
	 * popover's {@link CellPopover#title} and {@link PopoverField#title}, {@link RowAction#label},
	 * {@link RowAction#confirm}, {@link RibbonAction#title} and its {@link RibbonAction.Opt#title}) in place on the
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

	/** Resolves every declared column's {@link Column#title} and the chrome of its cell popover, if any. */
	private static void resolveColumnsChrome(List<Runnable> restores, VarResolverSession session, List<Column> columns) {
		if (columns == null)
			return;
		for (var c : columns)
			if (c != null) {
				resolveField(restores, session, c.title, v -> c.title = v);
				resolvePopoverChrome(restores, session, c.render);
			}
	}

	/** Resolves every declared row action's {@link RowAction#label} and {@link RowAction#confirm}. */
	private static void resolveRowActionsChrome(List<Runnable> restores, VarResolverSession session,
			List<RowAction> rowActions) {
		if (rowActions == null)
			return;
		for (var a : rowActions)
			if (a != null) {
				resolveField(restores, session, a.label, v -> a.label = v);
				resolveField(restores, session, a.confirm, v -> a.confirm = v);
			}
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
	 * Resolves the row-detail panel's own {@code $FV} chrome (the closed title list: {@link RowDetailDef#title})
	 * in place on the shared {@code detail}, so the server-emitted {@code <template>} below is painted with the
	 * resolved strings and the expand GET is left carrying row data only.
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
		return lifoRestore(restores);
	}

	/**
	 * Resolves a cell popover's own chrome &mdash; {@link CellPopover#title} and each
	 * {@link PopoverField#title} &mdash; reached through the owning {@link Column}'s {@link Render}.
	 *
	 * <p>
	 * The popover's data bindings ({@link PopoverField#data}) and nested per-field {@link PopoverField#render}
	 * are deliberately not touched: only the two heading strings a reader actually sees are chrome.
	 */
	private static void resolvePopoverChrome(List<Runnable> restores, VarResolverSession session, Render render) {
		if (render == null || render.popover == null)
			return;
		var popover = render.popover;
		resolveField(restores, session, popover.title, v -> popover.title = v);
		if (popover.fields != null)
			for (var f : popover.fields)
				if (f != null)
					resolveField(restores, session, f.title, v -> f.title = v);
	}

	/**
	 * The pre-scan (view-def string i18n LD-1 §5.1): {@code true} only if {@code viewDef}'s allowlisted
	 * chrome ({@link #resolveChrome}'s exact field set: column/action/ribbon titles) contains at least one
	 * {@code $}-prefixed template.  Read-only and session-free, but <b>not lock-free</b>: it reads exactly the
	 * fields {@link #resolveChrome} mutates in place, so callers must hold {@code viewDef.lock} (see
	 * {@link #emitViewHost}).  This is what lets {@link #emitViewHost} skip resolving entirely for a def with no
	 * template anywhere (byte stability), and is what decouples resolution from {@link ViewDef#serverValues}
	 * being declared: a def with a bare {@code $L{...}} title and no server-values provider now resolves too.
	 * Must stay in lock-step with
	 * {@link #resolveChrome}'s own field walk &mdash; every field the latter can mutate, this must also inspect.
	 */
	private static boolean chromeHasVar(ViewDef viewDef) {
		return columnsChromeHasVar(viewDef.columns) || rowActionsChromeHasVar(viewDef.rowActions)
			|| ribbonChromeHasVar(viewDef.ribbon);
	}

	private static boolean columnsChromeHasVar(List<Column> columns) {
		if (columns == null)
			return false;
		for (var c : columns)
			if (c != null && (hasVar(c.title) || popoverChromeHasVar(c.render)))
				return true;
		return false;
	}

	private static boolean rowActionsChromeHasVar(List<RowAction> rowActions) {
		if (rowActions == null)
			return false;
		for (var a : rowActions)
			if (a != null && (hasVar(a.label) || hasVar(a.confirm)))
				return true;
		return false;
	}

	private static boolean ribbonChromeHasVar(List<RibbonAction> ribbon) {
		if (ribbon == null)
			return false;
		for (var r : ribbon) {
			if (r == null)
				continue;
			if (hasVar(r.title))
				return true;
			if (r.options != null)
				for (var o : r.options)
					if (o != null && hasVar(o.title))
						return true;
		}
		return false;
	}

	/**
	 * The row-detail counterpart of {@link #chromeHasVar}: {@code true} only if {@code detail}'s allowlisted
	 * chrome ({@link #resolveDetailChrome}'s exact field set: {@link RowDetailDef#title}) contains at least one
	 * {@code $}-prefixed template.  Callers must hold {@code detail.lock} for the same reason
	 * {@link #chromeHasVar}'s callers must hold the view's.  Must stay in lock-step with
	 * {@link #resolveDetailChrome}'s own field walk for the same reason {@link #chromeHasVar} must.
	 */
	private static boolean detailChromeHasVar(RowDetailDef detail) {
		return hasVar(detail.title);
	}

	/** The pre-scan counterpart of {@link #resolvePopoverChrome}, walking the identical two-title field set. */
	private static boolean popoverChromeHasVar(Render render) {
		if (render == null || render.popover == null)
			return false;
		if (hasVar(render.popover.title))
			return true;
		if (render.popover.fields != null)
			for (var f : render.popover.fields)
				if (f != null && hasVar(f.title))
					return true;
		return false;
	}

	/**
	 * The cheap allowlist pre-scan primitive (view-def string i18n LD-1 §5.1): {@code true} only if {@code s} is
	 * non-<jk>null</jk> and carries a {@code $}-prefixed template.  The exact same structural check
	 * {@link #resolveField} already uses to skip a template-free field; shared here so pre-scan and resolve can
	 * never disagree about what counts as "has a template."  Package-private so {@link PageTable}'s own
	 * page-chrome pre-scan uses the identical rule.
	 */
	static boolean hasVar(String s) {
		return s != null && s.indexOf('$') >= 0;
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
