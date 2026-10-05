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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The nav looks the same after the C1 CSS merge (P5): the Task 0 probe page and a shell-rendered page with the same
 * nav must both match the computed styles captured before C1 changed any CSS.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleVisual_BrowserTest extends TestBase {

	private static final String PROBE_CONTRACT = """
		{"version":"1","title":"Probe","header":{"title":"Probe","chrome":true},
		 "nav":[
		  {"id":"home","label":"Home","href":"/home","children":[
		   {"id":"about","label":"About","href":"/home/about"},
		   {"id":"setup","label":"Setup","href":"/home/setup"}]},
		  {"id":"slo","label":"SLO","href":"/slo"}],
		 "activeNav":["home","setup"],
		 "cards":[{"id":"c","type":"html","template":"c"}]}
		""";

	private static Map<String,Map<String,Object>> report;
	private static Map<?,?> baseline;

	@BeforeAll
	static void probe() throws Exception {
		var probeHtml = resource("/console-visual/probe.html")
			.replace("@@CHROME@@", CHROME_CSS).replace("@@THEME@@", THEME_CSS).replace("@@VIEWS@@", VIEWS_CSS);
		baseline = org.apache.juneau.marshall.marshaller.Json.to(resource("/console-visual/nav-computed-baseline.json"), Map.class);
		report = ConsoleBrowserFixture.create("visual")
			.computedPage("static", ORIGIN + "/probe", probeHtml)
			.computedPage("shell", ORIGIN + "/probe", contractPage(PROBE_CONTRACT, "<template data-card=\"c\">x</template>"))
			.run();
	}

	private static void assertMatchesBaseline(String caseName) {
		var computed = (Map<?,?>)report.get(caseName).get("computed");
		var diffs = new ArrayList<String>();
		for (var sel : baseline.keySet()) {
			var want = (Map<?,?>)baseline.get(sel);
			var got = (Map<?,?>)computed.get(sel);
			if (got == null) {
				diffs.add(sel + ": no element");
				continue;
			}
			for (var prop : want.keySet())
				if (neq(want.get(prop), got.get(prop)))
					diffs.add(sel + " " + prop + ": expected '" + want.get(prop) + "' but was '" + got.get(prop) + "'");
		}
		assertEquals(7, baseline.size(), "the baseline has 7 selectors");
		assertEmpty(() -> caseName + " differs from the pre-C1 baseline:\n" + String.join("\n", diffs), diffs);
	}

	@Test void a01_probePage_matchesBaseline() {
		assertMatchesBaseline("static");
	}

	@Test void a02_shellRenderedNav_matchesBaseline() {
		assertNoShellErrors(report.get("shell"));
		assertMatchesBaseline("shell");
	}
}
