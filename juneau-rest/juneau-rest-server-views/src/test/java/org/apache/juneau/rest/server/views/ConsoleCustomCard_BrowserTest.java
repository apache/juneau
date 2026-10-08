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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * A real page registering a custom card type end-to-end: before the shell loads (through the deferred queue),
 * after it loads (registerCard on a card already mounted as pending), and a card whose type never registers failing
 * loudly at DOMContentLoaded. Confirms juneau:card-mounted fires with the documented detail shape.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleCustomCard_BrowserTest extends TestBase {

	// console-shell.cjs copies window.__probe into the report's "probe" entry.
	private static final String PROBE_EVENTS = """
		<script>
		window.__probe = { events: [] };
		document.addEventListener('juneau:card-mounted', function (e) { window.__probe.events.push({ kind: 'mounted', id: e.detail.id, type: e.detail.type }); });
		document.addEventListener('juneau:card-failed', function (e) { window.__probe.events.push({ kind: 'failed', id: e.detail.id, type: e.detail.type, error: e.detail.error }); });
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		// JuneauConsole does not exist before the shell loads, so pre-load registration goes through the queue.
		var beforeShell = PROBE_EVENTS
			+ "<script>(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(['kpi', function (card, el) { el.textContent = 'kpi:' + card.id; }]);</script>";
		// After load the card is already mounted as pending; registerCard renders it.
		var afterShell = "<script>JuneauConsole.registerCard('widget', { render: function (card, el) { el.textContent = 'widget:' + card.id; } });</script>";
		report = ConsoleBrowserFixture.create("custom-card")
			.page("before", ORIGIN + "/before",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[{\"id\":\"k1\",\"type\":\"kpi\",\"title\":\"K\"}]}", "", beforeShell, ""),
				"k1", "#k1.jc-card")
			.page("after", ORIGIN + "/after",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[{\"id\":\"w1\",\"type\":\"widget\"}]}", "", PROBE_EVENTS, afterShell),
				"w1", "#w1.jc-card")
			.page("never", ORIGIN + "/never",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[{\"id\":\"n1\",\"type\":\"nope\"}]}", ""))
			.run();
	}

	@Test void a01_queuedBeforeLoad_rendersAndFiresMounted() {
		var r = report.get("before");
		assertNoShellErrors(r);
		assertTrue(String.valueOf(queries(r).get("k1")).contains("kpi:k1"), () -> r.toString());
		assertEquals(List.of("k1"), eventIds(r, "mounted"), () -> r.toString());
	}

	@Test void a02_registeredAfterLoad_rendersPendingCardAndFiresMounted() {
		var r = report.get("after");
		assertNoShellErrors(r);
		assertTrue(String.valueOf(queries(r).get("w1")).contains("widget:w1"), () -> r.toString());
		assertEquals(List.of("w1"), eventIds(r, "mounted"), () -> r.toString());
	}

	@Test void a03_neverRegistered_failsLoudlyAtDomContentLoaded() {
		assertFailure(report.get("never"), "E-JS-4", "card 'n1' has unknown type 'nope'; registered types: 'html, console-output, run-view'");
	}

	@SuppressWarnings("unchecked")
	private static List<String> eventIds(Map<String,Object> r, String kind) {
		var events = (List<Map<String,Object>>)ConsoleBrowserFixture.probe(r).get("events");
		return events.stream().filter(e -> kind.equals(e.get("kind"))).map(e -> String.valueOf(e.get("id"))).toList();
	}
}
