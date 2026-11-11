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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Header sort + per-column search icons. Option-A content-substring coverage: sort
 * only from {@code span.dt-column-order} / {@code .juneau-view-col-sort-icon}, searchable columns
 * get a Juneau {@code search} glyph, not a copy of a third-party icon.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_HeaderSortSearch_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	@Test void a01_buildOptions_setsOrderCellsTop() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function buildOptions(");
		assertTrue(fn.contains("opts.orderCellsTop = true"), fn);
	}

	@Test void a02_constructTable_wiresHeaderSortSearchAfterPaint() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("wireHeaderSortSearch(table, ctx)"), body);
		var fn = functionBody(body, "function constructTable(");
		var paint = fn.indexOf("paintHeaderTitles");
		var wire = fn.indexOf("wireHeaderSortSearch(table, ctx)");
		assertTrue(paint >= 0 && wire > paint, fn);
	}

	@Test void a03_headerClick_stopsUnlessSortOrSearchControl() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function wireHeaderSortSearch(");
		assertTrue(fn.contains("stopImmediatePropagation"), fn);
		assertTrue(fn.contains("span.dt-column-order"), fn);
		assertTrue(fn.contains(".juneau-view-col-search-icon"), fn);
		assertTrue(fn.contains(".juneau-view-col-sort-icon"), fn);
		assertTrue(fn.contains("}, true);"), fn);  // capture-phase click listener
	}

	@Test void a04_searchIcon_usesJuneauSearchGlyphNotThirdPartyClass() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function renderHeaderSearchIcon(");
		assertTrue(fn.contains("resolveIcon?.(\"search\")"), fn);
		assertTrue(fn.contains("juneau-view-col-search-icon"), fn);
		assertFalse(body.contains("span.col-search-icon"), body);
		DualHatGuard.assertClean(body);
	}

	// Header order: title, then the magnifying glass inserted just before the sort glyph; on a non-sortable column it
	// is appended last.
	@Test void a04b_searchIcon_insertedBeforeSortGlyphAfterTitle() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function renderHeaderSearchIcon(");
		assertTrue(fn.contains("insertBefore(icon, orderSpan)"), fn);
		assertTrue(fn.contains("flex.appendChild(icon)"), fn);
		assertFalse(fn.contains("orderSpan.nextSibling"), fn);
	}

	// Design 7.1 active state: a column that already carries a search value (restored from saved state or the URL)
	// paints the glyph active on first render, not only after a popover edit.
	@Test void a04c_searchIcon_activeWhenColumnAlreadyFiltered() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function renderHeaderSearchIcon(");
		// read through the store adapter, so a client-filtered DSL column (col.search() === "") still paints active.
		assertTrue(fn.contains("getColumnExpr(ctx, col)"), fn);
		assertTrue(fn.contains("classList.add(\"is-active\")"), fn);
	}

	@Test void a05_searchPopover_appliesColumnSearch() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function openColumnSearchPopover(");
		// every write goes through the store adapter (native col.search(value) off the DSL path).
		assertTrue(fn.contains("setColumnExpr(ctx, col, value)"), fn);
		assertTrue(fn.contains("drawColumnExpr(ctx, r)"), fn);
		assertTrue(fn.contains("juneau-view-col-search-popover"), fn);
		assertTrue(fn.contains("is-active"), fn);
		assertFalse(fn.contains("btn-primary"), fn);
		assertFalse(fn.contains("btn-secondary"), fn);
	}

	// Commit semantics live in openColumnSearchPopover: help rendered from the VIEW_META operator
	// list, no live preview (Apply/Enter/Clear commit), an out-of-set operator rejected, and Esc/Cancel/click-away
	// leaving the applied filter untouched.
	@Test void a05b_searchPopover_helpNoPreviewAndReject() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function openColumnSearchPopover(");
		assertTrue(fn.contains("juneau-view-col-search-popover-help"), fn);   // operator help list
		assertTrue(fn.contains("helpMeta.operators"), fn);                     // help sourced from VIEW_META, or the type fallback
		assertTrue(fn.contains("evaluateColumnSearchDraft"), fn);              // classify draft (dollar/reject/incomplete)
		assertTrue(fn.contains("Not a valid search for this column."), fn);    // reject message
		assertFalse(fn.contains("if (!serverSide) applyValue"), fn);           // no live preview
		assertFalse(fn.contains("drawColumnExpr(ctx, setColumnExpr(ctx, col, current))"), fn);   // nothing to revert
	}

	// The draft classifier gates commit timing: leading "$" => deferred; an unparseable expression OR one naming an
	// operator not on the column's effective set => rejected.
	@Test void a05c_evaluateColumnSearchDraft_gatesDollarAndEffectiveSet() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function evaluateColumnSearchDraft(");
		assertTrue(fn.contains("search.parse"), fn);
		assertTrue(fn.contains("desc?.invalid"), fn);
		assertTrue(fn.contains("desc?.incomplete"), fn);
		assertTrue(fn.contains("usedOperatorNames"), fn);
		assertTrue(fn.contains("if (!allowed[n]) rejected = true"), fn);
	}

	@Test void a06_teardown_closesPopoverAndClearsGuard() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function teardownTable(");
		assertTrue(fn.contains("closeColumnSearchPopover(ctx)"), fn);
		assertTrue(fn.contains("delete table.dataset.juneauHeaderSortSearch"), fn);
		// a rebuild drops the client-mode DSL store along with the native filters destroy() discards.
		assertTrue(fn.contains("ctx._colExprs = {}"), fn);
	}

	// the store adapter keeps native col.search() for server mode / no-metadata columns and routes a
	// client-filtered DSL column through ONE named search.fixed predicate.
	@Test void a06b_setColumnExpr_routesDslColumnsThroughSearchFixed() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function setColumnExpr(");
		assertTrue(fn.contains("if (!info) return { ok: true, error: null, api: col.search(value) };"), fn);
		assertTrue(fn.contains("col.search.fixed(DSL_FIXED_SEARCH, fn)"), fn);
		assertTrue(fn.contains("col.search.fixed(DSL_FIXED_SEARCH, null)"), fn);
		assertTrue(body.contains("const DSL_FIXED_SEARCH = \"juneau-dsl\";"), body);
	}

	@Test void a07_ensureHeaderSortControl_injectsSvgOnExistingDtOrderSpan() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function ensureHeaderSortControl(");
		assertTrue(fn.contains("classList.add(\"juneau-view-col-sort-icon\")"), fn);
		assertTrue(fn.contains("insertAdjacentHTML"), fn);
		assertTrue(fn.contains("querySelector(\"svg\")"), fn);
		assertTrue(fn.contains("resolveIcon?.(\"sort\")"), fn);
		assertTrue(fn.indexOf("classList.add") > fn.indexOf("if (!orderSpan)"), fn);
		// Header order is title, search icon, sort control: the sort control stays last.
		assertTrue(fn.contains("flex.appendChild(orderSpan)"), fn);
		assertFalse(fn.contains("insertBefore(orderSpan, flex.firstChild)"), fn);
	}

	@Test void b01_viewsCss_idleSortChevronsAreVisible() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("span.dt-column-order:before"), body);
		assertTrue(body.contains("--jc-sort-asc-fill"), body);
		assertTrue(body.contains("opacity: 0.45;"), body);
		assertTrue(body.contains(".juneau-view-col-search-icon"), body);
		assertTrue(body.contains("cursor: default;"), body);
		assertTrue(body.contains("cursor: pointer;"), body);
		assertFalse(body.contains("span.col-search-icon"), body);
	}

	@Test void b02_viewsCss_sortControlDoesNotRenderTickOrDtTriangles() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("span.dt-column-order:before"), body);
		assertTrue(body.contains("content: none !important"),
			"DT ::before/::after content must be none !important so dataTables.dataTables.css cannot win:\n" + excerptSort(body));
		assertFalse(body.contains(":not(:has(svg)):before"),
			":not(:has(svg)) chevrons lost to DT content:▲ when an SVG is present:\n" + excerptSort(body));
		assertFalse(hasContentValue(body, "▲"), "DT2 unicode up-triangle must not remain as content");
		assertFalse(hasContentValue(body, "▼"), "DT2 unicode down-triangle must not remain as content");
		assertFalse(hasContentValue(body, "✓"), body);
		assertFalse(hasContentValue(body, "✔"), body);
		assertFalse(body.contains("content: \"\\25"), body);
	}

	private static String excerptSort(String css) {
		var start = css.indexOf("span.dt-column-order");
		return start < 0 ? css : css.substring(start, Math.min(css.length(), start + 1800));
	}

	private static boolean hasContentValue(String css, String glyph) {
		return css.contains("content: \"" + glyph + "\"") || css.contains("content: '" + glyph + "'");
	}

	@Test void b03_viewsCss_headerBordersAndTokens() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("--jc-table-head-border-top: #e5e5e5;"), body);
		assertTrue(body.contains("--jc-table-head-border-bottom: #6b6b6b;"), body);
		assertTrue(body.contains("border-width: 2px 0 1px 0;"), body);
		assertTrue(body.contains("border-color: var(--jc-table-head-border-top, #e5e5e5) transparent var(--jc-table-head-border-bottom, #6b6b6b) transparent;"), body);
		assertTrue(body.contains("--jc-detail-control-color: #818181;"), body);
		assertTrue(body.contains("color: var(--jc-detail-control-color, #818181);"), body);
		assertTrue(body.contains("color: var(--jc-detail-control-hover, #0d6efd);"), body);
		// Body cells keep their left/right grid lines.
		assertTrue(body.contains("border-left-width: 1px;"), body);
	}
}
