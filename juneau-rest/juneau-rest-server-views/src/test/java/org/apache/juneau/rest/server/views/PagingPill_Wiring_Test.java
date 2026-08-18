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
 * Unified paging-pill construction/wiring function assertions for {@code juneau-views.js} (DataTables ribbon
 * visual-parity design doc §4.C, plan Tasks 8/9/10). Option-A (content-substring + function-body-extraction)
 * coverage, mirroring {@code ViewsMixin_Serving_Test}'s established idiom - the pill's DataTables-API-surface
 * wiring (`page.len`, `page(...)`, `draw.dt`) and the cross-version native-control-hide CSS selector list.
 */
class PagingPill_Wiring_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Task 8 - pure pillState(pageInfo, pageLength)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_pillState_firstAndLastPageDisabledConditions() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function pillState(");
		assertTrue(fnBody.contains("page === 0"), fnBody);
		assertTrue(fnBody.contains("page === pages - 1"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Task 9 - buildPagingPill(viewDef, ctx) DOM construction + wiring
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_buildPagingPill_wiresDataTablesPagingApiSurface() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("function buildPagingPill("), body);
		assertTrue(body.contains(".page.len("), body);
		assertTrue(body.contains(".page(\"first\")"), body);
		assertTrue(body.contains(".page(\"previous\")"), body);
		assertTrue(body.contains(".page(\"next\")"), body);
		assertTrue(body.contains(".page(\"last\")"), body);
		assertTrue(body.contains("\"draw.dt\""), body);
		assertTrue(body.contains("juneau-view-pagingpill"), body);
		assertTrue(body.contains("juneau-view-pagingpill-btn"), body);
		// The unified paging ribbon's central segment is a page-size MENU BUTTON now - the old standalone
		// <select> is gone (Task: consolidate page-size + nav into one control).
		assertFalse(body.contains("juneau-view-pagingpill-select"), body);
		assertTrue(body.contains("juneau-view-pagingpill-menubtn"), body);
		assertTrue(body.contains("juneau-view-pagingpill-menu"), body);
		// buildPagingPill returns the bare pill element - it no longer inserts its own row/moves the native info
		// node (that node doesn't exist any more: buildOptions() now sets `info: false`); buildToolbarRow(...)
		// owns all toolbar-row DOM insertion (paging pill placement regression, top-toolbar-row fix).
		var fnBody = functionBody(body, "function buildPagingPill(");
		assertTrue(fnBody.trim().endsWith("return pill;"), fnBody);
		assertFalse(fnBody.contains("juneau-view-pagingpill-row"), fnBody);
		assertFalse(fnBody.contains(".dataTables_info"), fnBody);
		// Segment order: First, Prev, the page-size menu, Next, Last - a SINGLE unified segmented control.
		var firstIdx = fnBody.indexOf("pill.appendChild(firstBtn)");
		var prevIdx = fnBody.indexOf("pill.appendChild(prevBtn)");
		var menuIdx = fnBody.indexOf("pill.appendChild(sizeMenu.el)");
		var nextIdx = fnBody.indexOf("pill.appendChild(nextBtn)");
		var lastIdx = fnBody.indexOf("pill.appendChild(lastBtn)");
		assertTrue(firstIdx >= 0 && prevIdx > firstIdx && menuIdx > prevIdx && nextIdx > menuIdx && lastIdx > nextIdx, fnBody);
	}

	@Test void b03_buildPagingPill_carriesPagingDataTestId() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildPagingPill(");
		assertTrue(fnBody.contains("pill.setAttribute(\"data-testid\", \"paging\")"), fnBody);
	}

	/**
	 * The page-size menu (formerly a standalone {@code <select>}) is a proper WAI-ARIA menu button: {@code
	 * aria-haspopup="listbox"}/{@code aria-expanded} on the button, {@code role="option"} entries with {@code
	 * aria-selected} reflecting the current DataTables page length, keyboard-operable (Enter/Space/ArrowDown
	 * opens; ArrowUp/ArrowDown move; Enter/Space selects; Escape closes and returns focus to the button), and
	 * selecting an option drives the SAME {@code page.len(n).draw()} API the old {@code <select>} used.
	 */
	@Test void b04_buildPageSizeMenu_isAnAccessibleKeyboardOperableMenuButton() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("function buildPageSizeMenu("), body);
		var fnBody = functionBody(body, "function buildPageSizeMenu(");
		assertTrue(fnBody.contains("\"aria-haspopup\", \"listbox\""), fnBody);
		assertTrue(fnBody.contains("\"aria-expanded\""), fnBody);
		assertTrue(fnBody.contains("\"role\", \"option\""), fnBody);
		assertTrue(fnBody.contains("\"aria-selected\""), fnBody);
		assertTrue(fnBody.contains("\"ArrowDown\""), fnBody);
		assertTrue(fnBody.contains("\"ArrowUp\""), fnBody);
		assertTrue(fnBody.contains("\"Escape\""), fnBody);
		assertTrue(fnBody.contains("ctx.dataTable.page.len("), fnBody);
		assertTrue(fnBody.contains(".focus()"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Top-toolbar-row regression (paging pill must sit ABOVE the table, in the SAME row as search + ribbon)
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Regression: the pill previously inserted its own ".juneau-view-pagingpill-row" next to DataTables' native
	 * info node, which (a) put it at the BOTTOM of the table by default, and (b) broke once `info:false` removed
	 * that info node entirely (the pill's insertion fallback appended to the END of the wrapper - still the
	 * bottom). buildToolbarRow(...) now owns ALL toolbar placement: pill + native search + ribbon assembled into
	 * ONE row and inserted as the wrapper's first child, i.e. above the table.
	 */
	@Test void b02_buildToolbarRow_assemblesPillSearchAndRibbonAboveTheTable() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildToolbarRow(");
		assertTrue(fnBody.contains("pill"), fnBody);
		assertTrue(fnBody.contains("bar"), fnBody);
		assertTrue(fnBody.contains(".querySelector(\".dataTables_filter, .dt-search\")"), fnBody);
		assertTrue(fnBody.contains("insertBefore(row, wrapper.firstChild)"), fnBody);
		// Control-row layout: a LEFT cluster (pill only) and a RIGHT cluster (search + ribbon + extras).
		assertTrue(fnBody.contains("juneau-view-toolbar-left"), fnBody);
		assertTrue(fnBody.contains("juneau-view-toolbar-right"), fnBody);
		assertTrue(fnBody.contains("left.appendChild(pill)"), fnBody);

		var initBody = functionBody(body, "function initTable(");
		assertTrue(initBody.contains("buildPagingPill("), initBody);
		assertTrue(initBody.contains("buildToolbarRow(wrapper, pill, bar)"), initBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Task 10 - paging-pill shape CSS + native-control-hide selectors (DT1+DT2)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_viewsCss_hidesNativeLengthAndPaginationControlsBothDtGenerations() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".dataTables_length, .dt-length, .dataTables_paginate, .dt-paging {"), body);
		var hideStart = body.indexOf(".dataTables_length, .dt-length, .dataTables_paginate, .dt-paging {");
		var hideEnd = body.indexOf("}", hideStart);
		assertTrue(body.substring(hideStart, hideEnd).contains("display: none"), body);
	}

	@Test void c02_viewsCss_hasPagingPillShapeAndDisabledDim() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-pagingpill {"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-btn {"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-btn:disabled { opacity:"), body);
	}

	/**
	 * Regression: the DataTables info line ("Showing 0 to 0 of 0 entries...") must be disabled AT THE SOURCE
	 * (`info: false` in buildOptions(), juneau-views.js) rather than CSS-hidden - so there must be no
	 * ".dataTables_info"/".dt-info" hide rule left over in the CSS (a leftover rule would be dead code masking the
	 * fact the node no longer exists at all).
	 */
	@Test void c03_viewsCss_hasNoDeadInfoLineHideRule() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertFalse(body.contains(".dataTables_info, .dt-info {"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Item 2 - ribbon container must lay its buttons out horizontally (design doc §4.A regression)
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Regression: the ribbon CONTAINER (the div buildRibbon(...) in juneau-ribbon.js creates with class
	 * "juneau-view-ribbon") shipped with no CSS rule of its own - only ".juneau-view-ribbon-btn" was styled, which
	 * only centers a button's OWN icon and does nothing for its siblings - so buttons rendered one per line
	 * instead of as a horizontal ribbon. Asserts the CSS selector for the container that juneau-ribbon.js actually
	 * emits is present and lays out as a horizontal flex row.
	 */
	@Test void c04_viewsCss_ribbonContainerIsHorizontalFlexAndMatchesEmittedClassName() throws Exception {
		var css = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		var js = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(js.contains("bar.className = \"juneau-view-ribbon\";"), js);
		assertTrue(css.contains(".juneau-view-ribbon {"), css);
		var start = css.indexOf(".juneau-view-ribbon {");
		var end = css.indexOf("}", start);
		var rule = css.substring(start, end);
		assertTrue(rule.contains("display: flex"), rule);
	}

	/**
	 * Regression (item 5's visual-feedback fix): a persisted `option`/`optionGroup` toggle (e.g. the release-manager
	 * "dropped-only" filter, `persist(true)`) can be active with NO visible affordance on the button - so a
	 * still-active filter left over from a prior session silently keeps filtering out every row, and looks
	 * identical to a bug. Asserts the neutral `aria-pressed="true"` shape rule exists here (console-ui's chrome.css
	 * layers the themed accent color on the SAME selector).
	 */
	@Test void c05_viewsCss_hasNeutralAriaPressedShapeForActiveToggle() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-ribbon-btn[aria-pressed=\"true\"] {"), body);
	}

	/**
	 * Unified paging ribbon: the old standalone page-size {@code <select>} rule is gone; the central segment's
	 * menu-button/popup/option shapes exist instead.
	 */
	@Test void c06_viewsCss_hasPageSizeMenuShapeAndNoStandaloneSelect() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertFalse(body.contains(".juneau-view-pagingpill-select"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-menubtn {"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-menu {"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-menu-option {"), body);
		assertTrue(body.contains(".juneau-view-pagingpill-menu-option[aria-selected=\"true\"]"), body);
	}

	/** data-testid hooks (selector-ambiguity removal): the unified paging ribbon and the right actions ribbon. */
	@Test void c07_pagingAndRibbonContainers_carryDataTestIdHooks() throws Exception {
		var viewsBody = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var pillFnBody = functionBody(viewsBody, "function buildPagingPill(");
		assertTrue(pillFnBody.contains("pill.setAttribute(\"data-testid\", \"paging\")"), pillFnBody);

		var ribbonBody = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var ribbonFnBody = functionBody(ribbonBody, "function buildRibbon(");
		assertTrue(ribbonFnBody.contains("bar.setAttribute(\"data-testid\", \"ribbon\")"), ribbonFnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Control-row layout item 3 - the "n-n of n" paging summary (pure formatThousands/pagingSummaryText + wiring)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_formatThousands_insertsSeparatorsEveryThreeDigits() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function formatThousands(");
		assertTrue(fnBody.contains("% 3 === 0"), fnBody);
	}

	@Test void e02_pagingSummaryText_matchesIrsOneBasedInclusiveRangeAndEmptyCase() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function pagingSummaryText(");
		assertTrue(fnBody.contains("pageInfo.start + 1"), fnBody);
		assertTrue(fnBody.contains("pageInfo.end"), fnBody);
		assertTrue(fnBody.contains("formatThousands(total)"), fnBody);
		assertTrue(fnBody.contains("\"0-0 of 0\""), fnBody);
	}

	@Test void e03_buildPagingPill_rendersSummaryTextInsideThePageSizeMenu() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var pillFnBody = functionBody(body, "function buildPagingPill(");
		assertTrue(pillFnBody.contains("sizeMenu.refresh(pagingSummaryText(info)"), pillFnBody);
		var menuFnBody = functionBody(body, "function buildPageSizeMenu(");
		assertTrue(menuFnBody.contains("juneau-view-pagingpill-info"), menuFnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unified paging ribbon regression - the old right-hand "page-controls-ribbon" (compact, ribbon-styled
	// prev/next duplicate) is GONE; paging exists in exactly ONE place.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e04_buildCompactPagingRibbon_wasRemoved_pagingExistsInExactlyOnePlace() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertFalse(body.contains("function buildCompactPagingRibbon("), body);
		assertFalse(body.contains("juneau-view-pagingribbon"), body);
		var initBody = functionBody(body, "function initTable(");
		assertFalse(initBody.contains("pagingRibbon"), initBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Control-row layout item 4 - the per-column search row buildColumnSearchRow(...) inserts/wires
	//------------------------------------------------------------------------------------------------------------------

	@Test void e05_buildColumnSearchRow_insertsHiddenPerColumnInputsWiredToColumnSearch() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildColumnSearchRow(");
		assertTrue(fnBody.contains("juneau-view-columnsearch-row"), fnBody);
		assertTrue(fnBody.contains("data-testid\", \"col-search-row\""), fnBody);
		assertTrue(fnBody.contains("juneau-view-columnsearch-input"), fnBody);
		assertTrue(fnBody.contains("row.style.display = \"none\""), fnBody);
		assertTrue(fnBody.contains("dt.column(idx).search(input.value).draw()"), fnBody);
		assertTrue(fnBody.contains("col.searchable !== false"), fnBody);

		// initTable(...) must actually wire ctx.onColumnSearchToggle to show/hide + clear-and-redraw this row -
		// the root cause of the previously-broken toggle was that nothing ever assigned this callback.
		var initBody = functionBody(body, "function initTable(");
		assertTrue(initBody.contains("buildColumnSearchRow("), initBody);
		assertTrue(initBody.contains("ctx.onColumnSearchToggle = function"), initBody);
		assertTrue(initBody.contains("dt.columns().search(\"\").draw()"), initBody);
	}
}
