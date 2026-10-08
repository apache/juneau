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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The {@code datatables} card type end to end in a real page: registered through the {@code window.JuneauConsoleCards}
 * queue by the real views pack (before or after the shell), each card painting a real DataTable into its own
 * {@code #<id>-body}, both from an inline author catalog and from a {@code src} URL; and a page without the views
 * pack failing loudly with {@code E-JS-10}, once per still-pending card.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleDatatablesCard_BrowserTest extends TestBase {

	private static final String TABLES = """
		{"contractVersion":"1","title":"Tables","cards":[
		 {"id":"t1","type":"datatables","table":{"rows":[{"name":"alpha"}],"columns":[{"key":"name","label":"Name"}]}},
		 {"id":"t2","type":"datatables","src":"/rest/b"}]}
		""";

	/** What {@code src} answers: a SLOT_META envelope, which {@code mountDatatables} paints without a lift. */
	private static final String T2_SLOT = """
		{"contractVersion":"1","layout":"wide","rows":[{"name":"beta"}],
		 "view":{"contractVersion":"VIEW_VERSION","id":"t2","columns":[{"data":"name","title":"Name"}]}}
		""";

	private static final String NEVER_LOADS = """
		{"contractVersion":"1","title":"Tables","cards":[
		 {"id":"t9","type":"datatables","table":{"rows":[],"columns":[{"key":"name","label":"Name"}]}}]}
		""";

	// console-shell.cjs copies window.__probe into the report's "probe" entry.
	private static final String PROBE_EVENTS = """
		<script>
		window.__probe = { events: [] };
		document.addEventListener('juneau:card-mounted', function (e) { window.__probe.events.push({ kind: 'mounted', id: e.detail.id }); });
		document.addEventListener('juneau:card-failed', function (e) { window.__probe.events.push({ kind: 'failed', id: e.detail.id, error: e.detail.error }); });
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var views = viewsPack();
		report = ConsoleBrowserFixture.create("datatables-card")
			.asset("/rest/b", T2_SLOT.replace("VIEW_VERSION", viewContractVersion()), "application/json")
			.page("views-after", ORIGIN + "/after", contractPage(TABLES, "", PROBE_EVENTS, views),
				"t1", "#t1-body table[data-juneau-view=\"t1\"] tbody td",
				"t2", "#t2-body table[data-juneau-view=\"t2\"] tbody td",
				"t1Head", "#t1-body table[data-juneau-view=\"t1\"] thead th")
			.page("views-before", ORIGIN + "/before", contractPage(TABLES, "", PROBE_EVENTS + views, ""),
				"t1", "#t1-body table[data-juneau-view=\"t1\"] tbody td")
			.page("views-missing", ORIGIN + "/missing", contractPage(NEVER_LOADS, ""))
			.run();
	}

	/** The live JUNEAU_VIEW_CONTRACT_VERSION, read from the served juneau-views.js so this test never pins it. */
	private static String viewContractVersion() throws IOException {
		var m = Pattern.compile("const JUNEAU_VIEW_CONTRACT_VERSION = \"([^\"]+)\";")
			.matcher(resource(ViewsMixin.VIEWS_JS_RESOURCE));
		assertTrue(m.find(), "JUNEAU_VIEW_CONTRACT_VERSION not found in juneau-views.js");
		return m.group(1);
	}

	@Test void a01_viewsAfterShell_eachCardPaintsItsOwnTable() {
		var r = report.get("views-after");
		assertNoShellErrors(r);
		assertEquals("alpha", queries(r).get("t1"), () -> "inline catalog lifted and painted: " + r);
		assertEquals("Name", queries(r).get("t1Head"), () -> "label lifted to title: " + r);
		assertEquals("beta", queries(r).get("t2"), () -> "src envelope fetched and painted: " + r);
		assertEquals(Set.of("t1", "t2"), Set.copyOf(eventIds(r, "mounted")), () -> r.toString());
	}

	@Test void a02_viewsBeforeShell_queuedRegistrationPaintsTheSame() {
		var r = report.get("views-before");
		assertNoShellErrors(r);
		assertEquals("alpha", queries(r).get("t1"), () -> r.toString());
	}

	@Test void a03_viewsNeverLoads_failsLoudlyWithE_JS_10() {
		assertFailure(report.get("views-missing"), "E-JS-10", "datatables card 't9' needs JuneauViews.regions; load the views toolkit");
	}

	@SuppressWarnings("unchecked")
	private static List<String> eventIds(Map<String,Object> r, String kind) {
		var events = (List<Map<String,Object>>)ConsoleBrowserFixture.probe(r).get("events");
		return events.stream().filter(e -> kind.equals(e.get("kind"))).map(e -> String.valueOf(e.get("id"))).toList();
	}
}
