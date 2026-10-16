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
package org.apache.juneau.petstore.console.browser;

import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Every console page's builder twin ({@code ?builder}) renders the same DOM state in the real browser as its
 * FreeMarker template: the nav ids and the current path, the mounted cards in order with their titles, and the table
 * headers.  {@code PetstoreTwins_Test} compares the server-side contracts; this compares what the toolkit builds.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreTwins_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	/** Normalized DOM state: nav ids, current path, mounted cards (id, title) in order, and table headers. */
	private static final String STATE = "JSON.stringify({"
		+ " nav: [...document.querySelectorAll('[data-juneau-nav-id]')].map(a => a.getAttribute('data-juneau-nav-id')),"
		+ " current: [...document.querySelectorAll('[data-juneau-nav-id][aria-current=\"page\"]')].map(a => a.getAttribute('data-juneau-nav-id')),"
		+ " cards: [...document.querySelectorAll('.jc-card, .jc-card-bare')].map(c => [c.id, c.querySelector(':scope > .jc-card-title')?.textContent ?? null]),"
		+ " columns: [...document.querySelectorAll('.jc-card thead th, .jc-card-bare thead th')].map(th => th.textContent.trim())"
		+ "})";
	/*
	 * Settled means every card has finished its own first render, not just left the loading state: each view table has
	 * been wrapped by DataTables and has its header painted, and each chart has a drawn ECharts surface and a filled
	 * caption (both written by the first draw, together with the chart's "Show data" table header).
	 */
	private static final String SETTLED = "!!document.querySelector('.jc-card, .jc-card-bare')"
		+ " && !document.querySelector('[data-juneau-card-pending], .jc-card-loading')"
		+ " && [...document.querySelectorAll('table.juneau-view-table')].every(t => t.closest('.dt-container, .dataTables_wrapper') && t.querySelector('thead th'))"
		+ " && [...document.querySelectorAll('.jc-chart')].every(f => f.querySelector('.jc-chart-canvas canvas, .jc-chart-canvas svg') && f.querySelector('.jc-chart-caption')?.textContent.trim())";

	private static Map<String,Map<String,Object>> report;

	static List<String> pages() {
		return PetstorePages_ContractTest.PAGE_PATHS;
	}

	private static Map<String,Object> theCase(String name, String path) {
		// The Photo tab's <object> asks for the photo before there is one, so its 404 load is expected there only.
		return Map.of("name", name, "path", path, "allowFailedLoads", path.contains("/photo"),
			"actions", List.of(Map.of("waitFor", SETTLED, "timeoutMs", 8000)),
			"queries", Map.of("state", STATE));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		var cases = new ArrayList<Map<String,Object>>();
		for (var p : pages()) {
			cases.add(theCase("ftl " + p, p));
			cases.add(theCase("builder " + p, p + (p.contains("?") ? "&" : "?") + "builder"));
		}
		report = run("twins", base, cases);
	}

	@ParameterizedTest(name="{0}")
	@MethodSource("pages")
	void a01_builderDomEqualsFtlDom(String path) {
		var ftl = query(assertClean(report, "ftl " + path), "state");
		var builder = query(assertClean(report, "builder " + path), "state");
		assertNotNull(ftl);
		assertEquals(ftl, builder, () -> String.format("Twin DOM differs on '%s'", path));
	}
}
