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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * A {@code PageSpec}-built {@code table(...)} card (parity case 03, {@code pagespec-corpus/03-table-every-feature.json})
 * mounts a live DataTable in Chromium: a header cell for every visible column, none for the {@code defaultVisible(false)}
 * column, the ribbon's buttons, the {@code linked} render and the {@code rowClass} rule applied to a fetched row. The contract
 * shape is owned by {@code PageSpec_Parity_Test}; this only confirms the browser agrees.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class PageSpecTable_BrowserTest extends TestBase {

	private static final String PROBE = """
		<script>
		window.__probe = {
		  get ths() { return Array.from(document.querySelectorAll("#slo-body table thead th")).map(function (e) { return e.textContent.trim(); }); },
		  get ribbon() { return Array.from(document.querySelectorAll("#slo-body [data-testid=ribbon] button")).map(function (e) { return e.getAttribute("aria-label"); }); }
		};
		</script>
		""";

	private static final String ROWS = """
		[{"id":"1","pod":"p1","incidentKey":"I-1","incidentUrl":"/inc/1","rootCause":"disk","analysisState":"degraded"}]
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("pagespec-table")
			.asset("/rest/slo/data", ROWS, "application/json")
			.page("table", ORIGIN + "/page", corpusPage("03-table-every-feature", PROBE, viewsPack()),
				"row", "#slo-body tbody tr.row-degraded td:nth-child(2)",
				"link", "#slo-body tbody tr.row-degraded td:nth-child(3)")
			.waitFor("#slo-body tbody tr.row-degraded")
			.run();
	}

	@SuppressWarnings("unchecked")
	private static List<String> probeList(String key) {
		return (List<String>)ConsoleBrowserFixture.probe(report.get("table")).get(key);
	}

	@Test void a01_mountsWithoutErrors() {
		assertNoShellErrors(report.get("table"));
	}

	// defaultVisible(false) only takes effect with columnConfig on (juneau-config.js); the plain table keeps every column.
	@Test void a02_headerHasEveryColumn_withLabelsLiftedToTitles() {
		var ths = probeList("ths");
		assertTrue(ths.containsAll(List.of("Pod", "Incident", "Root cause")), ths::toString);
	}

	@Test void a03_ribbonButtonsPresent() {
		var labels = probeList("ribbon");
		assertTrue(labels.contains("Refresh"), () -> "ribbon buttons: " + report.get("table"));
	}

	@Test void a04_fetchedRow_isRenderedWithRowClassAndLinkedRender() {
		var q = queries(report.get("table"));
		assertEquals("p1", q.get("row"));
		assertEquals("I-1", q.get("link"));
	}
}
