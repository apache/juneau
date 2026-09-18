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
 * Header sort + per-column search icons (WORK-J0547). Option-A content-substring coverage: sort
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

	@Test void a05_searchPopover_appliesColumnSearch() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function openColumnSearchPopover(");
		assertTrue(fn.contains("col.search(v).draw()"), fn);
		assertTrue(fn.contains("juneau-view-col-search-popover"), fn);
		assertTrue(fn.contains("is-active"), fn);
		assertFalse(fn.contains("btn-primary"), fn);
		assertFalse(fn.contains("btn-secondary"), fn);
	}

	@Test void a06_teardown_closesPopoverAndClearsGuard() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_JS_PATH).run().assertStatus(200).getContent().asString();
		var fn = functionBody(body, "function teardownTable(");
		assertTrue(fn.contains("closeColumnSearchPopover(ctx)"), fn);
		assertTrue(fn.contains("delete table.dataset.juneauHeaderSortSearch"), fn);
	}

	@Test void b01_viewsCss_idleSortChevronsAreVisible() throws Exception {
		var body = cWithMixin.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("span.dt-column-order:before"), body);
		assertTrue(body.contains("opacity: 0.45;"), body);
		assertTrue(body.contains(".juneau-view-col-search-icon"), body);
		assertTrue(body.contains("cursor: default;"), body);
		assertTrue(body.contains("cursor: pointer;"), body);
		assertFalse(body.contains("span.col-search-icon"), body);
	}
}
