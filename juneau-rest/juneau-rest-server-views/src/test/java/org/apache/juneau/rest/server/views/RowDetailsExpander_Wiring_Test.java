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
	// Pure layer: buildDetailFields
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_buildDetailFields_projectsDataAndTitle_fromRowData() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildDetailFields(");
		assertTrue(fnBody.contains("rowData[d.data]"), fnBody);
		assertTrue(fnBody.contains("title: d.title || d.data"), fnBody);
	}

	@Test void a02_buildDetailFields_nullOrUndefinedValue_rendersAsEmptyString_notNullOrUndefined() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildDetailFields(");
		assertTrue(fnBody.contains("v == null ? \"\" : String(v)"), fnBody);
	}

	@Test void a03_buildDetailFields_isPure_noDomOrDateAccess() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildDetailFields(");
		assertFalse(fnBody.contains("document."), fnBody);
		assertFalse(fnBody.contains("Date.now()"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Binding layer: buildDetailPanel / initDetailsExpander
	//------------------------------------------------------------------------------------------------------------------

	/** Client-rendered by default: the panel is built with textContent only, never innerHTML. */
	@Test void b01_buildDetailPanel_usesTextContentOnly_neverInnerHTML() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildDetailPanel(");
		assertTrue(fnBody.contains("dtEl.textContent = f.title"), fnBody);
		assertTrue(fnBody.contains("ddEl.textContent = f.value"), fnBody);
		assertFalse(fnBody.contains("innerHTML"), fnBody);
	}

	@Test void b02_buildDetailPanel_marksItselfForTestSelection() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildDetailPanel(");
		assertTrue(fnBody.contains("juneau-view-detail-panel"), fnBody);
		assertTrue(fnBody.contains("\"data-testid\", \"detail-panel\""), fnBody);
	}

	@Test void b03_initDetailsExpander_delegatesOneClickListener_offTheDetailRowMarkerClass() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initDetailsExpander(");
		assertTrue(fnBody.contains("table.addEventListener(\"click\""), fnBody);
		assertTrue(fnBody.contains("tr.juneau-view-detail-row"), fnBody);
	}

	@Test void b04_initDetailsExpander_usesDataTablesNativeChildRowApi_notItsOwnDom() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initDetailsExpander(");
		assertTrue(fnBody.contains("row.child.isShown()"), fnBody);
		assertTrue(fnBody.contains("row.child(buildDetailPanel(buildDetailFields(viewDef.details, row.data()))).show()"), fnBody);
		assertTrue(fnBody.contains("row.child.hide()"), fnBody);
	}

	/** Expanding a row does not survive a redraw - a toggle-open marker class is used purely for the CSS glyph,
	 *  not to re-open the panel across a draw; DataTables' own child-row lifecycle (not this file) is what
	 *  actually drops the panel on the next draw.dt. */
	@Test void b05_initDetailsExpander_togglesAnOpenMarkerClass_forTheCssGlyphOnly() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function initDetailsExpander(");
		assertTrue(fnBody.contains("tr.classList.add(\"juneau-view-detail-open\")"), fnBody);
		assertTrue(fnBody.contains("tr.classList.remove(\"juneau-view-detail-open\")"), fnBody);
	}

	@Test void b06_createdRow_marksExpandableRows_onlyWhenViewDeclaresNonEmptyDetails() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, "function buildOptions(");
		assertTrue(fnBody.contains("if (viewDef.details && viewDef.details.length)"), fnBody);
		assertTrue(fnBody.contains("juneau-view-detail-row"), fnBody);
	}

	@Test void b07_initTable_onlyWiresTheExpanderWhenViewDeclaresNonEmptyDetails() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var initBody = functionBody(body, "function initTable(");
		assertTrue(initBody.contains("if (viewDef.details && viewDef.details.length)"), initBody);
		assertTrue(initBody.contains("initDetailsExpander(table, dt, viewDef)"), initBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CSS shape (neutral, no palette color; open/closed glyph is a bare content-string, not an icon dependency)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_viewsCss_detailRowIsMarkedClickable() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-row {"), body);
		var start = body.indexOf(".juneau-view-detail-row {");
		var end = body.indexOf("}", start);
		assertTrue(body.substring(start, end).contains("cursor: pointer"), body.substring(start, end));
	}

	@Test void c02_viewsCss_openStateFlipsGlyph_withoutAnIconDependency() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains(".juneau-view-detail-row > td:first-child::before {"), body);
		assertTrue(body.contains(".juneau-view-detail-open > td:first-child::before {"), body);
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
