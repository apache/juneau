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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * A {@code TableSpec}-built table card mounts the View Settings gear in Chromium when {@code columnConfig(true)} is set
 * and has no gear when it is unset. {@code PageSpec.table(...)} passes {@link TableSpec#toCardBody()} through unchanged,
 * so the page contract here is the parity corpus case with its table body replaced by the {@code TableSpec} output.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class TableSpecColumnConfig_BrowserTest extends TestBase {

	private static final String PROBE = """
		<script>
		window.__fetches = 0;
		(function () {
		  var open = XMLHttpRequest.prototype.open;
		  XMLHttpRequest.prototype.open = function (m, u) { if (String(u).indexOf("/rest/slo/data") >= 0) window.__fetches++; return open.apply(this, arguments); };
		  var f = window.fetch;
		  window.fetch = function (u) { if (String(u && u.url || u).indexOf("/rest/slo/data") >= 0) window.__fetches++; return f.apply(this, arguments); };
		})();
		window.__probe = {
		  get fetches() { return window.__fetches; },
		  get gears() { return document.querySelectorAll(".juneau-config-chooser-btn").length; },
		  get ths() { return Array.from(document.querySelectorAll("#slo-body table thead th")).map(function (e) { return e.textContent.trim(); }); }
		};
		</script>
		""";

	private static final String ROWS = """
		[{"id":"1","pod":"p1","cause":"disk"}]
		""";

	private static Map<String,Map<String,Object>> report;

	private static String page(TableSpec table, boolean shellFirst) throws Exception {
		return page(table, shellFirst, viewsPack());
	}

	@SuppressWarnings("unchecked")
	private static String page(TableSpec table, boolean shellFirst, String pack) throws Exception {
		var corpus = (Map<String,Object>)Json.to(resource("/pagespec-corpus/03-table-every-feature.json"), Map.class);
		var contract = (Map<String,Object>)corpus.get("contract");
		var card = (Map<String,Object>)((List<?>)contract.get("cards")).get(0);
		card.put("table", table.toCardBody());
		// shellFirst is the order a real page emits (shell, then the toolkit pack); the other loads the pack first.
		return shellFirst
			? contractPage(Json.of(contract), "", PROBE, pack)
			: contractPage(Json.of(contract), "", pack + PROBE, "");
	}

	@BeforeAll
	static void probe() throws Exception {
		var gear = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"), Column.create("cause").label("Cause").defaultVisible(false)).columnConfig(true);
		var plain = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"));
		report = ConsoleBrowserFixture.create("tablespec-column-config")
			.asset("/rest/slo/data", ROWS, "application/json")
			.page("gear", ORIGIN + "/gear", page(gear, false))
			.page("gearShellFirst", ORIGIN + "/gear-sf", page(gear, true))
			.page("noConfigJs", ORIGIN + "/no-config", page(gear, true, viewsPack().replace("<script>\n" + resource(ViewsMixin.CONFIG_JS_RESOURCE) + "\n</script>\n", "")))
			.page("plain", ORIGIN + "/plain", page(plain, false))
			.waitFor("#slo-body tbody tr")
			.run();
	}

	@Test void a01_columnConfigSet_mountsGear() {
		assertNoShellErrors(report.get("gear"));
		assertEquals(1, ((Number)ConsoleBrowserFixture.probe(report.get("gear")).get("gears")).intValue(), () -> report.get("gear").toString());
	}

	@Test void a03_columnConfigSet_shellFirst_mountsGearAndHidesDefaultInvisibleColumn() {
		var r = report.get("gearShellFirst");
		assertNoShellErrors(r);
		var probe = ConsoleBrowserFixture.probe(r);
		assertEquals(1, ((Number)probe.get("gears")).intValue(), r::toString);
		assertEquals(List.of("pod"), probe.get("ths"), r::toString);
	}

	@Test void a05_shellFirst_fetchesRowsOnce() {
		var r = report.get("gearShellFirst");
		assertEquals(1, ((Number)ConsoleBrowserFixture.probe(r).get("fetches")).intValue(), r::toString);
	}

	@Test void a06_viewsPackWithoutConfigJs_stillRendersEveryColumn() {
		var r = report.get("noConfigJs");
		assertNoShellErrors(r);
		var probe = ConsoleBrowserFixture.probe(r);
		assertEquals(0, ((Number)probe.get("gears")).intValue(), r::toString);
		assertEquals(List.of("pod", "Cause"), probe.get("ths"), r::toString);
	}

	@Test void a04_columnConfigSet_packFirst_hidesDefaultInvisibleColumn() {
		var r = report.get("gear");
		assertEquals(List.of("pod"), ConsoleBrowserFixture.probe(r).get("ths"), r::toString);
	}

	@Test void a02_columnConfigUnset_hasNoGear() {
		assertNoShellErrors(report.get("plain"));
		assertEquals(0, ((Number)ConsoleBrowserFixture.probe(report.get("plain")).get("gears")).intValue(), () -> report.get("plain").toString());
	}
}
