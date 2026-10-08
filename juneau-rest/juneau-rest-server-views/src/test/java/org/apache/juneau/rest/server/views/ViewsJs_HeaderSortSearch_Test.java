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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Header sort + per-column search icons. Option-A content-substring coverage: sort
 * only from {@code span.dt-column-order} / {@code .juneau-view-col-sort-icon}, searchable columns
 * get a Juneau {@code search} glyph, not an IRS/SLDS copy.
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

	@Test void a04_searchIcon_usesJuneauSearchGlyphNotIrsClass() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function renderHeaderSearchIcon(");
		assertTrue(fn.contains("resolveIcon?.(\"search\")"), fn);
		assertTrue(fn.contains("juneau-view-col-search-icon"), fn);
		assertFalse(body.contains("span.col-search-icon"), body);
		assertFalse(body.contains("slds-"), body);
	}

	// Design 7.1 order: the magnifying glass is inserted immediately after the sort glyph (its nextSibling slot),
	// ahead of the title; on a non-sortable column it becomes the leftmost control instead.  The old
	// "before the order span" placement (which put search left of sort) must be gone.
	@Test void a04b_searchIcon_insertedAfterSortGlyphAheadOfTitle() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function renderHeaderSearchIcon(");
		assertTrue(fn.contains("insertBefore(icon, orderSpan.nextSibling)"), fn);
		assertTrue(fn.contains("insertBefore(icon, flex.firstChild)"), fn);
		assertFalse(fn.contains("orderSpan.before(icon)"), fn);
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

	// Design §4.5 / §5 commit semantics live in openColumnSearchPopover: help rendered from the VIEW_META operator
	// list, a `$`-expression deferred to Enter/Apply (never previewed), a bare quick-filter previewed live only off
	// the server dataMode branch, an out-of-set operator rejected, and Esc/click-away reverting the live preview.
	@Test void a05b_searchPopover_helpDeferralRejectAndRevert() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function openColumnSearchPopover(");
		assertTrue(fn.contains("juneau-view-col-search-popover-help"), fn);   // operator help list
		assertTrue(fn.contains("meta.operators"), fn);                         // help sourced from VIEW_META
		assertTrue(fn.contains("evaluateColumnSearchDraft"), fn);              // classify draft (dollar/reject/incomplete)
		assertTrue(fn.contains("if (!serverSide) applyValue"), fn);            // bare preview only on client tables
		assertTrue(fn.contains("if (d.dollar)"), fn);                          // $-expression is NOT previewed
		assertTrue(fn.contains("Not a valid search for this column."), fn);    // reject message
		assertTrue(fn.contains("drawColumnExpr(ctx, setColumnExpr(ctx, col, current))"), fn); // revert to opened-with value on dismiss
	}

	// The draft classifier gates commit timing: leading "$" => deferred; an unparseable expression OR one naming an
	// operator not on the column's effective set => rejected (design §4.3 - a reject, not a silent rewrite).
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
		// Design 7.1: the DT2 order span (appended after the title) is pulled to the front so the sort glyph leads.
		assertTrue(fn.contains("insertBefore(orderSpan, flex.firstChild)"), fn);
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
			"J0548 :not(:has(svg)) chevrons lost to DT content:▲ when an SVG is present:\n" + excerptSort(body));
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
}
