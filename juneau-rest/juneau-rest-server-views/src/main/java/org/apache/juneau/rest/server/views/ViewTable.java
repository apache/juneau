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
	 * Marker attribute stamped when a {@link SelectionDef} is declared ({@code TODO-428}). Pure DOM signaling
	 * &mdash; never part of the {@code VIEW_META} wire contract; see {@link SelectionDef}'s class javadoc.
	 */
	public static final String SELECT_ATTR = "data-juneau-select";

	/** Attribute carrying {@link SelectionDef#rowIdField()} &mdash; the row-data key the runtime stamps as each row's stable id. */
	public static final String ROW_ID_FIELD_ATTR = "data-juneau-row-id-field";

	/** Attribute carrying {@code "1"}/{@code "0"} for {@link SelectionDef#selectAll()}. */
	public static final String SELECT_ALL_ATTR = "data-juneau-select-all";

	/**
	 * Marker attribute stamped when a {@link BulkMutateDef} is declared ({@code TODO-428}); pairs with the
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
	 * Full-real-estate layout hint stamped on the wrapper {@code <div>} (TODO-445n Goal 1).  A first-class public
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
		return emit(MarshallingContext.DEFAULT, viewDef, null, csrfToken(req), null, null, savedViewsBase(req), req);
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
		return emit(MarshallingContext.DEFAULT, viewDef, rows, csrfToken(req), null, null, savedViewsBase(req), req);
	}

	/**
	 * Builds the view-table shell with row selection enabled (design doc §9.3; {@code TODO-428}), auto-embedding
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
		return emit(MarshallingContext.DEFAULT, viewDef, rows, csrfToken(req), selection, null, savedViewsBase(req), req);
	}

	/**
	 * Builds the view-table shell with row selection AND bulk mutation enabled (design doc §9.3; {@code
	 * TODO-428}), auto-embedding the request's CSRF token.
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
	 * 	or one of the pre-{@code TODO-428} overloads, for a table with no bulk mutation).
	 * @return A new {@link Div} carrying the {@code <table data-juneau-view data-juneau-select data-juneau-bulk>},
	 * 	its VIEW_META sidecar, AND the independently-versioned bulk-actions sidecar.
	 */
	public static Div of(HttpServletRequest req, ViewDef viewDef, Collection<?> rows, BulkMutateDef bulkMutate) {
		if (bulkMutate == null)
			throw iaex("bulkMutate must not be null; use the SelectionDef overload for selection without bulk mutation.");
		return emit(MarshallingContext.DEFAULT, viewDef, rows, csrfToken(req), bulkMutate.selection(), bulkMutate,
			savedViewsBase(req), req);
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
	 * {@link BulkMutateDef} &mdash; the shared core every public overload (pre- and post-{@code TODO-428})
	 * ultimately delegates to.
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
		return emit(ctx, viewDef, rows, csrfToken, selection, bulkMutate, savedViewsBase, null);
	}

	/**
	 * The shared core.  Validates and reconciles selection/bulk-mutate, then &mdash; when {@code viewDef} declares
	 * {@link ViewDef#serverValues} and {@code req} is a {@link RestRequest} &mdash; resolves the declared
	 * {@code $FV} chrome (titles/labels) against a per-response <b>sibling</b> {@link VarResolverSession} before
	 * building both the painted {@code <th>}/labels and the VIEW_META sidecar, so the two agree (W1).  The
	 * resolved chrome is a per-response snapshot: the author's {@code $FV{...}} templates are restored on the
	 * shared {@link ViewDef} once this response's markup has captured the resolved strings.
	 */
	private static Div emit(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows, String csrfToken,
			SelectionDef selection, BulkMutateDef bulkMutate, String savedViewsBase, HttpServletRequest req) {
		viewDef.validate();
		if (bulkMutate != null) {
			if (selection != null && selection != bulkMutate.selection())
				throw iaex("selection must be exactly bulkMutate.selection() when both are supplied; "
					+ "a BulkMutateDef can only render the SelectionDef it was constructed against.");
			selection = bulkMutate.selection();
		}

		if (viewDef.serverValues != null && req instanceof RestRequest rr) {
			var session = serverValuesSession(rr, viewDef.serverValues);
			// A shared ViewDef may be rendered concurrently; the mutate-serialize-restore window is guarded so
			// two responses cannot interleave resolved chrome onto the same instance.
			synchronized (viewDef) {
				var restore = resolveChrome(viewDef, session);
				try {
					return build(ctx, viewDef, rows, csrfToken, selection, bulkMutate, savedViewsBase);
				} finally {
					restore.run();
				}
			}
		}
		return build(ctx, viewDef, rows, csrfToken, selection, bulkMutate, savedViewsBase);
	}

	private static Div build(MarshallingContext ctx, ViewDef viewDef, Collection<?> rows, String csrfToken,
			SelectionDef selection, BulkMutateDef bulkMutate, String savedViewsBase) {
		var id = viewDef.id;
		var cols = viewDef.columns == null ? List.<Column>of() : viewDef.columns;

		// <thead> of column titles (falling back to the data key when no title was set).  Leading synthetic
		// columns are dedicated cells that never share the first data column: expander (when details is set),
		// then a selection checkbox.
		var headerCells = new ArrayList<>(cols.size() + 2);
		if (viewDef.details != null)
			headerCells.add(th().attr("class", DETAIL_TH_CLASS).attr("aria-label", "Expand"));
		if (selection != null)
			headerCells.add(th().attr("class", "juneau-view-select-th").attr("aria-label", "Select"));
		for (var c : cols)
			headerCells.add(th(c.title == null ? c.data : c.title));

		var tableChildren = new ArrayList<>();
		tableChildren.add(thead(tr(headerCells.toArray())));

		if (rows != null) {
			var bodyRows = new ArrayList<>(rows.size());
			for (var row : rows) {
				var cells = new ArrayList<>(cols.size() + 2);
				if (viewDef.details != null)
					cells.add(td().attr("class", DETAIL_CONTROL_CLASS));
				if (selection != null)
					cells.add(td().attr("class", "juneau-view-select-cell"));
				for (var c : cols) {
					var v = value(ctx, row, c.data);
					cells.add(td(v == null ? "" : v));
				}
				bodyRows.add(tr(cells.toArray()));
			}
			tableChildren.add(tbody(bodyRows.toArray()));
		}

		var table = table(tableChildren.toArray()).id(id).attr(MARKER_ATTR, id);

		// Auto-embed the CSRF token (MED-10/HIGH-1) so a row-action submit can attach it; a blank token stamps
		// nothing, so the runtime fails closed rather than shipping an empty header the boundary would 403.
		if (csrfToken != null && ! csrfToken.isBlank())
			table.attr(CSRF_ATTR, csrfToken);

		// Selection opt-in: pure DOM-attribute signaling (never VIEW_META/ViewDef.CONTRACT_VERSION - see
		// SelectionDef's class javadoc). Absent entirely when selection is null, so juneau-views.js renders no
		// checkbox column at all for an ordinary table - the separability guarantee's "off by default" half.
		if (selection != null) {
			table.attr(SELECT_ATTR, "1");
			table.attr(ROW_ID_FIELD_ATTR, selection.rowIdField());
			table.attr(SELECT_ALL_ATTR, selection.selectAll() ? "1" : "0");
		}

		// Sidecar: serialize the VIEW_META, neutralize script break-outs, then insert as RAW content (class javadoc).
		var json = escapeForScript(Json.of(viewDef));
		var sidecar = script().type("application/json").id(SIDECAR_ID_PREFIX + id).text(rawText(json));

		var children = new ArrayList<>();
		children.add(table);
		if (viewDef.details != null)
			children.add(emitDetailTemplate(viewDef));

		// Bulk-mutate opt-in: its OWN independently-versioned sidecar (BulkMutateDef.CONTRACT_VERSION), never
		// merged into VIEW_META - a version bump here can never force a ViewDef.CONTRACT_VERSION bump (R2).
		// Absent entirely when bulkMutate is null, so an ordinary or selection-only table carries no bulk affordance.
		if (bulkMutate != null) {
			table.attr(BULK_ATTR, "1");
			var bulkJson = escapeForScript(Json.of(bulkMutate));
			children.add(script().type("application/json").id(BULK_SIDECAR_ID_PREFIX + id).text(rawText(bulkJson)));
		}

		children.add(sidecar);
		var wrapper = div(children.toArray());
		// Full-real-estate stamp (TODO-445n Goal 1 / N2 A): the ONE stamp node is this wrapper <div>. The console
		// chrome full-bleed :has() rules widen the enclosing .jc-card/.jc-main off this attribute.
		wrapper.attr(LAYOUT_ATTR, LAYOUT_WIDE);
		if (savedViewsBase != null && ! savedViewsBase.isBlank())
			wrapper.attr(SAVED_VIEWS_ATTR, savedViewsBase);
		return wrapper;
	}

	/**
	 * Emits the one {@code <template data-juneau-row-detail>} sibling: empty field slots, {@link ActionRef}
	 * buttons initially disabled, {@link org.apache.juneau.rest.server.widgets.SafeAction#COLLAPSE} enabled.
	 * Labels are HtmlBuilder text children (never poured in as markup).
	 */
	private static Template emitDetailTemplate(ViewDef viewDef) {
		var d = viewDef.details;
		var children = new ArrayList<>();
		if (hasDetailHeader(d))
			children.add(emitDetailHeader(d, viewDef.rowActions));
		for (var s : d.sections) {
			var kids = new ArrayList<>();
			kids.add(h2(s.title == null || s.title.isBlank() ? s.id : s.title)
				.class_("juneau-view-detail-section-title"));
			if (s.actions != null && s.actions.items != null && !s.actions.items.isEmpty())
				kids.add(emitActionBar(s.actions, viewDef.rowActions));
			var fieldSlots = new ArrayList<>();
			if (s.fields != null) {
				for (var f : s.fields) {
					fieldSlots.add(emitDetailField(f));
				}
			}
			kids.add(div(fieldSlots.toArray())
				.class_("juneau-view-detail-fields")
				.attr("style", "grid-template-columns:repeat(" + s.columns + ",minmax(0,1fr))"));
			// Nested read-only table, appended last (after the fields grid) - the runtime instantiates it once
			// the detail GET succeeds and this section's pane is visible (TODO fold g5 DOM order).
			if (s.table != null)
				kids.add(emitNestedTable(s.table));
			children.add(section(kids.toArray())
				.attr(DETAIL_SECTION_ATTR, s.id)
				.class_("juneau-view-detail-section"));
		}
		return template()
			.attr(DETAIL_TEMPLATE_ATTR, "1")
			.attr(DETAIL_CONTRACT_ATTR, RowDetailDef.CONTRACT_VERSION)
			.attr(DETAIL_URL_ATTR, d.endpoint)
			.children(children.toArray());
	}

	private static boolean hasDetailHeader(RowDetailDef d) {
		var titled = d.title != null && !d.title.isBlank();
		var icon = d.icon != null && !d.icon.isBlank();
		var actions = d.headerActions != null && d.headerActions.items != null && !d.headerActions.items.isEmpty();
		return titled || icon || actions;
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
		if (d.headerActions != null && d.headerActions.items != null && !d.headerActions.items.isEmpty())
			kids.add(emitActionBar(d.headerActions, rowActions));
		return div(kids.toArray()).class_("juneau-view-detail-header").attr(DETAIL_HEADER_ATTR, "1");
	}

	/**
	 * Emits the nested-table shell inside a detail section: a {@code data-juneau-view} {@code <table>} with a
	 * {@code <thead>} of the nested view's column titles (no HTML {@code id} &mdash; a {@code <template>} clone would
	 * collide; the runtime mints one at instantiation), plus a sibling, {@code id}-less VIEW_META sidecar found via
	 * {@link #NESTED_META_ATTR}.  The wrapper carries the marker, the independent {@link NestedTableDef#CONTRACT_VERSION},
	 * and the parent-scope parameter name.
	 */
	private static Div emitNestedTable(NestedTableDef nt) {
		var v = nt.view;
		var cols = v.columns == null ? List.<Column>of() : v.columns;
		var headerCells = new ArrayList<>(cols.size());
		for (var c : cols)
			headerCells.add(th(c.title == null ? c.data : c.title));
		var table = table(thead(tr(headerCells.toArray()))).attr(MARKER_ATTR, v.id);

		// Sidecar: same VIEW_META contract as a top-level view; neutralize break-outs, insert as RAW (class javadoc).
		var json = escapeForScript(Json.of(v));
		var sidecar = script().type("application/json").attr(NESTED_META_ATTR, v.id).text(rawText(json));

		return div(table, sidecar)
			.class_("juneau-view-detail-nested")
			.attr(NESTED_ATTR, "1")
			.attr(NESTED_CONTRACT_ATTR, NestedTableDef.CONTRACT_VERSION)
			.attr(NESTED_SCOPE_PARAM_ATTR, nt.parentScopeParam);
	}

	private static Div emitDetailField(DetailField f) {
		var rendered = f.render != null;
		var markdown = !rendered && f.format == DetailField.Format.MARKDOWN;
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
		if (hideTitle)
			return div(valueSlot).class_("juneau-view-detail-field juneau-view-detail-field-markdown");
		var label = f.title == null || f.title.isBlank() ? f.data : f.title;
		return div(
			div(label).class_("juneau-view-detail-field-title"),
			valueSlot
		).class_(markdown ? "juneau-view-detail-field juneau-view-detail-field-markdown" : "juneau-view-detail-field");
	}

	private static Div emitActionBar(org.apache.juneau.rest.server.widgets.ActionBar bar, List<RowAction> rowActions) {
		var buttons = new ArrayList<>();
		for (var item : bar.items) {
			if (item instanceof org.apache.juneau.rest.server.widgets.ActionRef ar) {
				var label = actionLabel(ar.id, rowActions);
				buttons.add(button("button", label)
					.attr(DETAIL_ACTION_ATTR, ar.id)
					.attr("class", "juneau-view-detail-action")
					.disabled(true));
			} else if (item instanceof org.apache.juneau.rest.server.widgets.SafeAction sa) {
				buttons.add(button("button", sa.label())
					.attr(DETAIL_SAFE_ATTR, sa.wire())
					.attr("class", "juneau-view-detail-action juneau-view-detail-safe"));
			}
		}
		return div(buttons.toArray()).class_("juneau-view-detail-actions");
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
	 */
	private static VarResolverSession serverValuesSession(RestRequest rr, ServerValues serverValues) {
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
		if (viewDef.columns != null)
			for (var c : viewDef.columns)
				if (c != null)
					resolveField(restores, session, c.title, v -> c.title = v);
		if (viewDef.rowActions != null)
			for (var a : viewDef.rowActions)
				if (a != null)
					resolveField(restores, session, a.label, v -> a.label = v);
		if (viewDef.ribbon != null)
			for (var r : viewDef.ribbon) {
				if (r == null)
					continue;
				resolveField(restores, session, r.title, v -> r.title = v);
				if (r.options != null)
					for (var o : r.options)
						if (o != null)
							resolveField(restores, session, o.title, v -> o.title = v);
			}
		return () -> {
			for (var i = restores.size() - 1; i >= 0; i--)
				restores.get(i).run();
		};
	}

	/** Resolves one chrome field through {@code session}; on change, applies the resolved value and records a restore. */
	private static void resolveField(List<Runnable> restores, VarResolverSession session, String current,
			Consumer<String> setter) {
		if (current == null || current.indexOf('$') < 0)
			return;
		var resolved = session.resolve(current);
		if (Objects.equals(resolved, current))
			return;
		setter.accept(resolved);
		restores.add(() -> setter.accept(current));
	}

	/** Reads a column value from a row: a direct key lookup for a {@code Map}, a bean-property read otherwise. */
	private static Object value(MarshallingContext ctx, Object row, String key) {
		if (row instanceof java.util.Map<?,?> m)
			return m.get(key);
		return ctx.toBeanMap(row).get(key);
	}
}
