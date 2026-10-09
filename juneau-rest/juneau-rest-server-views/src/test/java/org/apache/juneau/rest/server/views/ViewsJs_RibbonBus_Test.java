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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The ribbon as messages (message bus addendum, spec §5.5): every bus-routed item sends {@code cmd:<target>}, toggle
 * state renders from {@code filter:<target>}, the {@code publish} item publishes its topic, a plain page runs
 * own-table ops directly and refuses cross-card targets loudly, and a SERVER-mode column-scoped option still reaches
 * the request through the new path.  Runs {@code ribbon-bus.cjs} through {@link BusHarness}.
 */
class ViewsJs_RibbonBus_Test extends TestBase {

	static Map<String,Object> report;

	@BeforeAll static void runHarness() {
		report = BusHarness.run("ribbon-bus.cjs",
			ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.RIBBON_JS_RESOURCE);
	}

	@BeforeEach void needsNode() {
		assumeTrue(report != null, "node not on PATH");
		assertNull(report.get("crash"), () -> "harness crashed: " + report.get("crash"));
		assertEquals(true, report.get("hasTableBus"));
	}

	@Test void a01_optionClickSendsSetFilterAndPaintsFromFilterTopic() {
		var m = map(report.get("open"));
		assertEquals("{\"schemaVersion\":1,\"op\":\"set-filter\",\"options\":{\"openOnly\":true}}", m.get("cmd"));
		assertEquals(1, m.get("rowFilterRefreshes"));
		assertEquals(1, m.get("draws"), "CLIENT mode draws in memory");
		assertEquals(0, m.get("reloads"));
		assertEquals("true", m.get("pressed"));
		assertEquals(true, m.get("filterOpen"));
	}

	@Test void a02_optionGroupClickSendsMemberAndTablePersists() {
		var m = map(report.get("all"));
		assertEquals("{\"schemaVersion\":1,\"op\":\"set-filter\",\"options\":{\"age\":\"all\"}}", m.get("cmd"));
		assertEquals("false", m.get("new"));
		assertEquals("true", m.get("all"));
		assertEquals("all", m.get("stored"), "the target table's set-filter op owns persistence");
	}

	@Test void a03_refreshReloadsWithoutResettingThePage() {
		var m = map(report.get("refresh"));
		assertEquals("{\"schemaVersion\":1,\"op\":\"reload\"}", m.get("cmd"));
		assertEquals("[false]", m.get("reloads"));
	}

	@Test void a04_pauseResumeCollapse() {
		assertEquals(Map.of("cmd", "pause-polling", "paused", true, "pressed", "true"), report.get("pause"));
		assertEquals(Map.of("cmd", "resume-polling", "paused", false, "pressed", "false"), report.get("resume"));
		assertEquals(Map.of("cmd", "collapse-all", "collapses", 1), report.get("collapse"));
	}

	@Test void b01_publishItemPublishesItsPayload() {
		var m = map(report.get("publish"));
		assertEquals("{\"region\":\"east\"}", m.get("payload"));
		assertEquals("east", m.get("filterRegion"));
	}

	@Test void c01_twoRibbonsOneTableStayInSync() {
		assertEquals("true", report.get("sideInitial"), "a cross-card ribbon paints from the retained filter:<target>");
		var m = map(report.get("side"));
		assertEquals("{\"schemaVersion\":1,\"op\":\"set-filter\",\"options\":{\"openOnly\":false}}", m.get("cmd"));
		assertEquals(false, m.get("mainOpenOnly"));
		assertEquals("false", m.get("mainPressed"));
		assertEquals("false", m.get("sidePressed"));
		assertEquals("{}", m.get("sideActiveState"), "a cross-card toggle never writes the sender's own state");
		assertEquals("[false]", report.get("sideRefresh"));
	}

	@Test void c02_rebuildDisposesThePreviousRibbonsListeners() {
		assertEquals(Map.of("oldBar", "false", "newBar", "true"), report.get("rebuild"));
	}

	@Test void d01_publishStateRepublishesFilterAndRedraw() {
		assertEquals(true, report.get("hasPublishState"));
		assertEquals(Map.of("filters", 1, "redraws", 1, "openOnly", false, "pressed", "false"), report.get("publishState"));
		assertEquals(Map.of("localFilterCalls", 1), report.get("publishStateUnchanged"), "an unchanged state still re-emits to local listeners");
	}

	@Test void d02_applyViewPublishesThroughBindTable() throws Exception {
		// applyView/applyDraft -> buildTable -> constructTable, which ends in bindTable: it resets the filter dedupe and
		// publishes filter:<id> for the rebuilt grid.  Pin that call so a refactor cannot drop it.
		var fn = functionBody(resource(ViewsMixin.VIEWS_JS_RESOURCE), "function constructTable(");
		assertTrue(fn.contains("bindTable(table, ctx);"), fn);
	}

	@Test void e01_plainPageRunsOwnOpsAndRefusesCrossCardLoudly() {
		var m = map(report.get("plain"));
		assertNull(m.get("bus"));
		assertEquals(1, m.get("rowFilterRefreshes"), "the own-table command ran directly");
		assertEquals("{\"age\":\"new\",\"openOnly\":true}", m.get("activeState"), "the cross-card click changed nothing");
		assertEquals("true", m.get("pressed"));
		assertEquals(false, m.get("publishRendered"));
		var errs = (List<?>)m.get("errors");
		assertTrue(errs.stream().anyMatch(e -> e.toString().contains("targets card 'other'")), () -> errs.toString());
		assertTrue(errs.stream().anyMatch(e -> e.toString().contains("ribbon publish item 'Nowhere'")), () -> errs.toString());
	}

	@Test void e02_ribbonAloneAppliesItsOwnCommandLocally() {
		assertEquals(Map.of("pressed", "true", "activeState", "{\"mine\":true}", "redraws", 2, "stored", "true"), report.get("solo"));
	}

	@Test void f01_dc55_serverModeColumnScopedOptionReachesTheRequest() {
		// The explicit optionGroup default is already in the first request.
		assertEquals("[[3,\"^yes$\"]]", report.get("srvFirstRequest"));
		var m = map(report.get("srv"));
		assertEquals("{\"schemaVersion\":1,\"op\":\"set-filter\",\"options\":{\"openOnly\":true}}", m.get("cmd"));
		assertEquals(1, m.get("reloads"));
		assertEquals("[]", m.get("columnSearches"), "SERVER mode never column-searches in the browser");
		assertEquals("[[2,\"open\"],[3,\"^yes$\"]]", m.get("request"));
	}

	@SuppressWarnings("unchecked")
	static Map<String,Object> map(Object o) { return (Map<String,Object>)o; }

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Balanced-brace extractor. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found");
		var i = body.indexOf('{', start);
		var depth = 0;
		for (var j = i; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') depth++;
			else if (c == '}' && --depth == 0) return body.substring(start, j + 1);
		}
		return fail("'" + signature + "' is unbalanced");
	}
}
