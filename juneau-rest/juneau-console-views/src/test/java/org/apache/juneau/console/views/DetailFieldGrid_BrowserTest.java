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

import static org.apache.juneau.console.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The field grid of a real row detail in Chromium: each label sits beside its value on one row, and rows are
 * separated by a hairline.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class DetailFieldGrid_BrowserTest extends org.apache.juneau.TestBase {

	private static final String PAGE = """
		{"contractVersion":"1","title":"Pets","cards":[
		 {"id":"t1","type":"datatables","table":{
		   "rows":[{"id":"p1","name":"Rex","species":"dog"}],
		   "columns":[{"key":"id","label":"Id"},{"key":"name","label":"Name"},{"key":"species","label":"Species"}],
		   "detail":{"mode":"inline","title":"Pet {id}","tabs":[
		     {"id":"main","kind":"fields","title":"Main","fields":["name","species"]}]}}}]}
		""";

	private static final String PROBE = """
		(async function () {
		  var p = window.__probe = window.__probe || {};
		  try {
		    for (var i = 0; i < 100 && !document.querySelector('#t1-body .juneau-view-detail-field'); i++)
		      await new Promise(function (r) { setTimeout(r, 50); });
		    var f = document.querySelector('#t1-body .juneau-view-detail-field');
		    var t = f.querySelector('.juneau-view-detail-field-title').getBoundingClientRect();
		    var v = f.querySelector('.juneau-view-detail-field-value').getBoundingClientRect();
		    var cs = getComputedStyle(f);
		    p.labelTop = t.top; p.valueTop = v.top; p.labelRight = t.right; p.valueLeft = v.left;
		    p.sepWidth = cs.borderBottomWidth; p.sepColor = cs.borderBottomColor;
		    p.gridClass = f.parentElement.className;
		    p.done = true;
		  } catch (e) { p.err = String(e && e.stack || e); }
		})();
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void run() throws Exception {
		var views = viewsPack();
		report = ConsoleBrowserFixture.create("detail-field-grid")
			.page("grid", ORIGIN + "/grid", contractPage(PAGE, "", "", views))
			.waitFor("#t1-body tbody td.sorting_1")
			.click("#t1-body .juneau-view-detail-toggle")
			.eval(PROBE)
			.waitFor("body")
			.run();
	}

	private static Map<String,Object> p() {
		var r = report.get("grid");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals(Boolean.TRUE, p.get("done"), () -> p.get("err") + " " + p);
		return p;
	}

	private static double d(Map<String,Object> p, String k) {
		return ((Number)p.get(k)).doubleValue();
	}

	@Test void a01_labelAndValueShareARow() {
		var p = p();
		assertEquals(d(p, "labelTop"), d(p, "valueTop"), 2.0, p::toString);
		assertTrue(d(p, "valueLeft") >= d(p, "labelRight"), p::toString);
	}

	@Test void a02_rowsHaveASeparator() {
		var p = p();
		assertEquals("1px", p.get("sepWidth"), p::toString);
		assertNotEquals("rgba(0, 0, 0, 0)", p.get("sepColor"), p::toString);
	}

	@Test void a03_gridCarriesTheInlineLayout() {
		var p = p();
		assertTrue(String.valueOf(p.get("gridClass")).contains("juneau-view-detail-fields-inline"), p::toString);
	}
}
