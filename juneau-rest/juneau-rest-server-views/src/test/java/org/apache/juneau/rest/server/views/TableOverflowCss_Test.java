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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Pins the table-overflow-discipline CSS contract in {@code juneau-views.css}: the DT2 "Approach D"
 * scoped overflow box, the {@code min-width:0} constraint chain (INV-3), and the DT1 "Approach B" wrap rule.
 *
 * <p>
 * The jsdom Node harness cannot measure {@code scrollWidth}; the browser suite proves the actual scroll behavior.
 * This CSS-presence guard is the always-on floor - it fails if the overflow box or the min-width chain is dropped.
 */
class TableOverflowCss_Test extends TestBase {

	private static String css() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_CSS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/**
	 * Strips {@code &#47;* ... *&#47;} comments, then collapses runs of whitespace, so selector/declaration matching
	 * is layout-insensitive AND sees only real rules - not prose that merely mentions a selector (e.g. the
	 * "an unscoped .dt-layout-cell { overflow }" caveat comment above the scoped rule).
	 */
	private static String flat() throws IOException {
		return css().replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("\\s+", " ");
	}

	/** Approach D (DT2): the scroll box is the flex {@code .dt-layout-cell}, scoped to the table's layout row. */
	@Test void a01_dt2ScrollBoxIsScopedLayoutCell() throws Exception {
		var c = flat();
		assertTrue(c.contains(".dt-layout-row.dt-layout-table > .dt-layout-cell"),
			"the overflow box must be the SCOPED .dt-layout-row.dt-layout-table > .dt-layout-cell (N-P4-S2)");
	}

	/** The overflow selector must be scoped - an unscoped {@code .dt-layout-cell { overflow }} would scroll toolbar cells. */
	@Test void a02_noUnscopedLayoutCellOverflow() throws Exception {
		var c = flat();
		// There must be no bare ".dt-layout-cell {" rule that declares overflow (only the scoped child selector).
		// A match preceded by a ">" child combinator is the SCOPED box (".dt-layout-table > .dt-layout-cell"),
		// not a bare rule - the substring ".dt-layout-cell {" is the tail of that scoped selector, so skip it.
		var idx = c.indexOf(".dt-layout-cell {");
		while (idx >= 0) {
			var j = idx - 1;
			while (j >= 0 && c.charAt(j) == ' ') j--;
			var scoped = j >= 0 && c.charAt(j) == '>';
			if (! scoped) {
				var end = c.indexOf('}', idx);
				var rule = c.substring(idx, end < 0 ? c.length() : end);
				assertFalse(rule.contains("overflow"),
					"an unscoped .dt-layout-cell overflow rule would scroll toolbar cells (N-P4-S2): " + rule);
			}
			idx = c.indexOf(".dt-layout-cell {", idx + 1);
		}
	}

	/** Approach B (DT1 wrap) box carries overflow-x + min-width:0. */
	@Test void a03_dt1WrapBox() throws Exception {
		var c = flat();
		assertTrue(c.contains(".juneau-view-table-scroll"), "missing the DT1 wrap rule .juneau-view-table-scroll");
	}

	/** INV-3: the min-width:0 constraint chain reaches the DataTables wrapper + the DT2 layout rows/cell. */
	@Test void a04_minWidthChain() throws Exception {
		var c = flat();
		assertTrue(c.contains(".dataTables_wrapper") || c.contains(".dt-container"),
			"min-width:0 chain must reach the DataTables wrapper (.dataTables_wrapper / .dt-container)");
		// The scroll box and the constraint links declare min-width:0 so the box, not the card, absorbs overflow.
		assertTrue(c.contains("overflow-x: auto") || c.contains("overflow-x:auto"),
			"the scroll box must declare overflow-x:auto");
	}

	/** Neutral - the overflow discipline adds no color/token (the file's colours-in-chrome split, Decision #6). */
	@Test void a05_noNewToken() throws Exception {
		var c = css();
		assertFalse(c.contains("--jc-table-scroll"), "the overflow discipline must not introduce a new --jc-* token");
	}

	//------------------------------------------------------------------------------------------------------------------
	// The 10.0 one-overflow-contract half: cell CONTENT clips by default on BOTH generations, while the two scroll
	// boxes above stay exactly as shipped.  The clip selector is NAMED (.juneau-view-table td) - never a global
	// unnamed `td` rule - and `juneau-cell-wrap` is the opt-out that restores wrap.
	//------------------------------------------------------------------------------------------------------------------

	/** Reads the declaration block of the first rule whose selector list matches {@code selector} exactly. */
	private static String ruleBody(String flat, String selector) {
		var idx = flat.indexOf(selector + " {");
		if (idx < 0) return null;
		var end = flat.indexOf('}', idx);
		return flat.substring(idx + selector.length() + 2, end < 0 ? flat.length() : end);
	}

	/** The clip default is the NAMED table-cell selector with a constrained box + hidden overflow + ellipsis + nowrap. */
	@Test void a06_clipDefaultIsNamedTableCellSelector() throws Exception {
		var c = flat();
		var body = ruleBody(c, ".juneau-view-table td");
		assertNotNull(body, () -> "missing the named clip rule `.juneau-view-table td` (rec N / rec U): " + c);
		assertTrue(body.contains("overflow: hidden"), () -> "clip rule must declare overflow:hidden: " + body);
		assertTrue(body.contains("text-overflow: ellipsis"), () -> "clip rule must declare text-overflow:ellipsis: " + body);
		assertTrue(body.contains("white-space: nowrap"), () -> "clip rule must declare white-space:nowrap: " + body);
		// "Constrained box": without a max-width the cell cannot actually clip - the table would just grow.
		assertTrue(body.contains("max-width"), () -> "clip rule needs a constrained box (max-width): " + body);
	}

	/** Opt-out: `juneau-cell-wrap` on the cell restores wrap (rec N). */
	@Test void a07_optOutClassRestoresWrap() throws Exception {
		var c = flat();
		var body = ruleBody(c, ".juneau-view-table td.juneau-cell-wrap");
		assertNotNull(body, () -> "missing the opt-out rule `.juneau-view-table td.juneau-cell-wrap` (rec N): " + c);
		assertTrue(body.contains("white-space: normal"), () -> "the opt-out must restore wrapping: " + body);
		assertTrue(body.contains("overflow: visible"), () -> "the opt-out must un-clip the cell: " + body);
		assertTrue(body.contains("text-overflow: clip"), () -> "the opt-out must drop the ellipsis: " + body);
		assertTrue(body.contains("max-width: none"), () -> "the opt-out must release the constrained box: " + body);
	}

	/**
	 * The reviewer's blast-radius objection (rec N / rec G): the clip default must never be written as a global
	 * unnamed {@code td} rule - only cells inside a Juneau-emitted {@code .juneau-view-table} clip.
	 */
	@Test void a08_noGlobalUnnamedTdRule() throws Exception {
		var c = flat();
		var idx = c.indexOf("td {");
		while (idx >= 0) {
			// A match preceded by anything other than a selector boundary ("}" or start-of-file) is a QUALIFIED
			// selector (".juneau-view-table td {"), which is exactly what rec N asks for.
			var j = idx - 1;
			while (j >= 0 && c.charAt(j) == ' ') j--;
			var qualified = j >= 0 && c.charAt(j) != '}' && c.charAt(j) != ';';
			if (! qualified) {
				var end = c.indexOf('}', idx);
				var rule = c.substring(idx, end < 0 ? c.length() : end);
				fail("a global unnamed `td` rule has table-wide blast radius (rec N / rec G): " + rule);
			}
			idx = c.indexOf("td {", idx + 1);
		}
	}

	/** Rec U: the emitted top-level and nested {@code <table>}s both carry {@code class="juneau-view-table"}. */
	@Test void a09_emittedTablesCarryTheNamedClass() {
		var nested = ViewDef.create("events")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/events")
			.columns(Column.of("when").title("When"))
			.build();
		var view = ViewDef.create("alerts")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.sections(DetailSection.create("related", "Related events")
					.fields(DetailField.of("owner").title("Owner"))
					.table(NestedTableDef.create(nested))))
			.build();
		var html = Html.of(ViewTable.of(view));
		// Both the top-level table and the nested-table shell clip by the same named selector.
		assertEquals(2, countTablesWithClass(html), () -> "both emitted <table>s must carry class=\"juneau-view-table\": " + html);
	}

	/** Counts {@code <table ...>} open tags carrying the named clip class. */
	private static int countTablesWithClass(String html) {
		var n = 0;
		var i = html.indexOf("<table");
		while (i >= 0) {
			var end = html.indexOf('>', i);
			var tag = html.substring(i, end < 0 ? html.length() : end);
			if (tag.contains("class='juneau-view-table'") || tag.contains("class=\"juneau-view-table\"")) n++;
			i = html.indexOf("<table", i + 1);
		}
		return n;
	}

	/**
	 * Rec U: the NAMED emitters opt out of the clip default, because their markup is a chip/bar/link that must not be
	 * ellipsised.  Pinned against {@code juneau-renders.js} source, which is where the {@code class} facets live.
	 */
	@Test void a10_namedEmittersStampTheOptOut() throws Exception {
		String js;
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			js = new String(in.readAllBytes(), UTF_8);
		}
		for (var cls : List.of("progress-cell", "pill-cell", "tag-cell"))
			assertTrue(js.contains(cls + " juneau-cell-wrap"),
				() -> "the `" + cls + "` emitter must stamp the juneau-cell-wrap opt-out (rec U)");
		// `linked` has no cell class of its own - the opt-out IS its class facet.
		assertTrue(js.contains("registerRenderer(\"linked\""), js);
		var linkedAt = js.indexOf("registerRenderer(\"linked\"");
		var linkedEnd = js.indexOf("registerRenderer(", linkedAt + 1);
		var linked = js.substring(linkedAt, linkedEnd < 0 ? js.length() : linkedEnd);
		assertTrue(linked.contains("juneau-cell-wrap"),
			() -> "the `linked` emitter must stamp the juneau-cell-wrap opt-out (rec U): " + linked);
	}
}
