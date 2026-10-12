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
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * {@code juneau-charts.js} in a real browser: a src-backed line chart, a donut whose slice click navigates through
 * {@code onClick.href}, computed series colors, the data-table toggle and a failing src.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views -am test` from the repo root")
class ChartCard_BrowserTest extends TestBase {

	private static final String CHARTS = "/juneau-console/juneau-charts.js";
	private static final String AFTER = "<script src=\"" + CHARTS + "\"></script>";
	private static final String PROBE = """
		<script>
		window.__probe = {
		  get stroke() { var p = document.querySelector('#trend .jc-chart-line'); return p ? getComputedStyle(p).stroke : null; },
		  get aria() { var s = document.querySelector('#trend svg'); var id = s && s.getAttribute('aria-labelledby'); var n = id && document.getElementById(id); return n ? n.textContent : null; },
		  get role() { var s = document.querySelector('#trend svg'); return s ? s.getAttribute('role') : null; },
		  get expanded() { var b = document.querySelector('#trend .jc-chart-toggle'); return b ? b.getAttribute('aria-expanded') : null; },
		  get tableVisible() { var t = document.querySelector('#trend .jc-chart-table-box'); return !!t && !t.hidden; },
		  get hash() { return location.hash; },
		  clicks: []
		};
		document.addEventListener('juneau:chart-click', function (e) { window.__probe.clicks.push({ card: e.detail.card, row: e.detail.row }); });
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void setup() throws Exception {
		var trend = "{\"id\":\"trend\",\"type\":\"chart\",\"title\":\"Orders\",\"kind\":\"line\",\"src\":\"/stats/daily\","
			+ "\"x\":{\"field\":\"day\",\"label\":\"Day\"},\"series\":[{\"field\":\"placed\",\"label\":\"Placed\"}]}";
		// Slice a stays under 50%: Playwright clicks a slice's bbox center, which for a >50% arc lands in the donut hole.
		var mix = "{\"id\":\"mix\",\"type\":\"chart\",\"kind\":\"donut\",\"label\":\"s\",\"value\":\"n\",\"center\":\"total\","
			+ "\"onClick\":{\"href\":\"#picked-{s}\"},\"data\":[{\"s\":\"a\",\"n\":1},{\"s\":\"b\",\"n\":3}]}";
		var broken = "{\"id\":\"broken\",\"type\":\"chart\",\"kind\":\"bar\",\"src\":\"/stats/missing\",\"x\":\"k\",\"series\":[\"v\"]}";
		report = ConsoleBrowserFixture.create("chart-card")
			.asset(CHARTS, resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE), "text/javascript")
			.asset("/stats/daily", "[{\"day\":\"Mon\",\"placed\":3},{\"day\":\"Tue\",\"placed\":5}]", "application/json")
			.page("charts", ORIGIN + "/charts",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + trend + "," + mix + "]}", "", PROBE, AFTER),
				"center", "#mix .jc-chart-center", "caption", "#trend .jc-chart-caption")
			.waitFor("#trend .jc-chart-line")
			.click("#trend .jc-chart-toggle")
			.click("#mix .jc-chart-slice")
			.page("broken", ORIGIN + "/broken",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + broken + "]}", "", "", AFTER),
				"error", "#broken .jc-card-error")
			.waitFor("#broken .jc-card-error")
			.run();
	}

	@Test void a01_lineFromSrc_accessibleAndColored() {
		var r = report.get("charts");
		assertNoShellErrors(r);
		var p = probe(r);
		assertEquals("Line chart, 1 series, 2 points, Day from 'Mon' to 'Tue'", p.get("aria"));
		assertEquals("img", p.get("role"));
		assertEquals(p.get("aria"), queries(r).get("caption"));
		assertEquals("rgb(27, 106, 201)", p.get("stroke"));
	}

	@Test void a02_tableToggle() {
		var p = probe(report.get("charts"));
		assertEquals("true", p.get("expanded"));
		assertEquals(true, p.get("tableVisible"));
	}

	@Test void a03_donutClick_firesEventAndNavigates() {
		var r = report.get("charts");
		assertEquals("4", queries(r).get("center"));
		var p = probe(r);
		assertEquals("#picked-a", p.get("hash"));
		assertEquals(List.of(Map.of("card", "mix", "row", Map.of("s", "a", "n", 1))), p.get("clicks"));
	}

	@Test void a04_failingSrc_paintsCardError() {
		var r = report.get("broken");
		assertEquals("E-JS-62: chart card 'broken' fetch failed: 'HTTP 404'", queries(r).get("error"));
	}
}
