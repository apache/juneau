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
 * Row-details expander: Option-A (content-substring + function-body-extraction) coverage for
 * {@code juneau-views.js}/{@code juneau-views.css}, mirroring {@code TablePolling_Wiring_Test}'s established idiom.
 */
@SuppressWarnings({
	"resource", // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
	"java:S5976" // Each test targets a distinct function/behavior via the shared functionBody(...) idiom; collapsing
					// into one @ParameterizedTest would obscure which specific behavior failed.
})
class RowDetailsExpander_Wiring_Test extends TestBase {

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
	// Pure layer: URL safety / scalar fill
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_isSafeDetailUrl_rejectsAbsoluteDotDotAndScheme() throws Exception {
		var fnBody = functionBody(cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString(),
			"function isSafeDetailUrl(");
		assertTrue(fnBody.contains("://"), fnBody);
		assertTrue(fnBody.contains("\\.\\.") || fnBody.contains(".."), fnBody);
	}

	@Test void a02_substituteDetailUrl_usesEncodeURIComponent() throws Exception {
		var fnBody = functionBody(cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString(),
			"function substituteDetailUrl(");
		assertTrue(fnBody.contains("encodeURIComponent"), fnBody);
		assertTrue(fnBody.contains("split(\"{id}\")"), fnBody);
	}

	@Test void a03_scalarFieldValue_nonScalarsBecomeEmpty() throws Exception {
		var fnBody = functionBody(cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString(),
			"function scalarFieldValue(");
		assertTrue(fnBody.contains("typeof v"), fnBody);
		assertFalse(fnBody.contains("JSON.stringify"), fnBody);
	}

	@Test void a04_fillDetailSlots_usesTextContentOnly_neverInnerHTML() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function fillDetailSlots(");
		assertTrue(fnBody.contains("paintDetailFieldSlot(slot, map)"), fnBody);
		assertTrue(fnBody.contains("paintDetailTitleSlot(el, map)"), fnBody);
		assertFalse(fnBody.contains("innerHTML"), fnBody);
		var titleFn = functionBody(body, "function paintDetailTitleSlot(");
		assertTrue(titleFn.contains("el.textContent"), titleFn);
		assertFalse(titleFn.contains("innerHTML"), titleFn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Binding layer: initDetailsExpander / createdRow
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_expandClonesTemplate_marksPanelForTestSelection() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function expandDetailRow(");
		assertTrue(fnBody.contains("tpl.content.cloneNode(true)"), fnBody);
		assertTrue(fnBody.contains("juneau-view-detail-panel"), fnBody);
		assertTrue(fnBody.contains("panel.dataset.testid = \"detail-panel\""), fnBody);
		assertFalse(fnBody.contains("innerHTML"), fnBody);
	}

	@Test void b02_initDetailsExpander_delegatesOneClickListener_offTheDetailRowMarkerClass() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initDetailsExpander(");
		assertTrue(fnBody.contains("table.addEventListener(\"click\""), fnBody);
		assertTrue(fnBody.contains("toggleDetailRow("), fnBody);
		var toggleFn = functionBody(body, "function toggleDetailRow(");
		assertTrue(toggleFn.contains("tr.juneau-view-detail-row"), toggleFn);
	}

	@Test void b03_initDetailsExpander_usesDataTablesNativeChildRowApi() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var initFn = functionBody(body, "function initDetailsExpander(");
		var toggleFn = functionBody(body, "function toggleDetailRow(");
		var collapseFn = functionBody(body, "function handleDetailSafeCollapseClick(");
		var expandFn = functionBody(body, "function expandDetailRow(");
		assertTrue(toggleFn.contains("row.child.isShown()"), toggleFn);
		assertTrue(toggleFn.contains("row.child.hide()"), toggleFn);
		assertTrue(collapseFn.contains("row.child.hide()"), collapseFn);
		assertTrue(expandFn.contains("row.child(panel).show()"), expandFn);
		assertTrue(initFn.contains("ctx.dataTable"), initFn);
		assertTrue(expandFn.contains("encodeURIComponent") || body.contains("substituteDetailUrl"), expandFn);
	}

	@Test void b04_initDetailsExpander_togglesAnOpenMarkerClass_forTheCssGlyphOnly() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var toggleFn = functionBody(body, "function toggleDetailRow(");
		var collapseFn = functionBody(body, "function handleDetailSafeCollapseClick(");
		var expandFn = functionBody(body, "function expandDetailRow(");
		assertTrue(expandFn.contains("tr.classList.add(\"juneau-view-detail-open\")"), expandFn);
		assertTrue(toggleFn.contains("tr.classList.remove(\"juneau-view-detail-open\")"), toggleFn);
		assertTrue(collapseFn.contains("parentTr.classList.remove(\"juneau-view-detail-open\")"), collapseFn);
	}

	@Test void b05_createdRow_marksExpandableRows_whenTemplatePresent() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildOptions(");
		assertTrue(fnBody.contains("if (deps.hasRowDetail)"), fnBody);
		assertTrue(fnBody.contains("juneau-view-detail-row"), fnBody);
	}

	@Test void b06_initTable_onlyWiresTheExpanderWhenTemplatePresent() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(functionBody(body, "function beginInitTable(").contains("initTableWidgets("), body);
		var widgets = functionBody(body, "function initTableWidgets(");
		assertTrue(widgets.contains("if (findRowDetailTemplate(table))"), widgets);
		assertTrue(widgets.contains("initDetailsExpander(table, ctx, viewDef)"), widgets);
	}

	@Test void b07_actionSubmitUsesParentTr_notJson() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function handleDetailActionRefClick(");
		assertTrue(fnBody.contains("submitRowAction(action, table, parentTr, ctx)"), fnBody);
		assertTrue(fnBody.contains("panel?._juneauParentTr"), fnBody);
	}

	@Test void b08_constructTable_clearsCoalesceMapOnDraw() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function constructTable(");
		assertTrue(fn.contains("bindDetailInflightDrawGuards("), fn);
		var guards = functionBody(body, "function bindDetailInflightDrawGuards(");
		assertTrue(guards.contains("draw.dt"), guards);
		assertTrue(guards.contains("_detailInflight"), guards);
	}

	@Test void b09_assembleFullColumnArray_prependsDedicatedExpanderColumn() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function assembleFullColumnArray(");
		assertTrue(fn.contains("buildDetailsControlColumnDef()"), fn);
		assertTrue(fn.contains("_juneau = \"detail\""), fn);
		assertTrue(body.contains("function detailsControlCellMarkup("), body);
		assertTrue(body.contains("juneau-view-detail-control"), body);
		assertTrue(body.contains("juneau-view-detail-glyphs"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CSS shape (neutral, no palette color; dual-chevron swap on a dedicated expander column)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_viewsCss_detailRowIsMarkedClickable() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-row {"), body);
		var start = body.indexOf(".juneau-view-detail-row {");
		var end = body.indexOf("}", start);
		assertTrue(body.substring(start, end).contains("cursor: pointer"), body.substring(start, end));
	}

	@Test void c02_viewsCss_openStateFlipsGlyph_onDedicatedExpanderColumn() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-control {") || body.contains(".juneau-view-detail-th,"), body);
		assertTrue(body.contains(".juneau-view-detail-expanded { display: none; }")
			|| body.contains(".juneau-view-detail-expanded {display: none;}"), body);
		assertTrue(body.contains(".juneau-view-detail-open .juneau-view-detail-collapsed"), body);
		assertTrue(body.contains(".juneau-view-detail-open .juneau-view-detail-expanded"), body);
		assertTrue(body.contains("width: 20px"), body);
		assertFalse(body.contains("td:first-child::before"), body);
		assertFalse(body.contains("url("), body);
	}

	@Test void c03_viewsCss_detailPanelShapeIsNeutral_noPaletteColor() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-panel {"), body);
		var start = body.indexOf(".juneau-view-detail-panel {");
		var end = body.indexOf("}", start);
		var rule = body.substring(start, end);
		assertFalse(rule.contains("color:"), rule);
		assertFalse(rule.contains("background"), rule);
	}
}
