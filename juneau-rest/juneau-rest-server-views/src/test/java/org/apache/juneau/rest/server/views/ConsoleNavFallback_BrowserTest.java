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
 * With an empty {@code activeNav}, the shell picks the longest segment-wise prefix match against the location, with
 * every query parameter of the href required (the JRM {@code ?tab=} shape). No match is not an error.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ConsoleNavFallback_BrowserTest extends TestBase {

	private static final String NAV = """
		{"version":"1","title":"Fallback","activeNav":[],"cards":[],"nav":[
		 {"id":"home","label":"Home","href":"/home","children":[
		  {"id":"about","label":"About","href":"/home/about"},
		  {"id":"setup","label":"Setup","href":"/home/setup"}]},
		 {"id":"runs","label":"Runs","href":"/rest/runs","children":[
		  {"id":"input","label":"Input","href":"/rest/runs?tab=input"},
		  {"id":"exec","label":"Exec","href":"/rest/runs?tab=exec"}]}]}
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		report = ConsoleBrowserFixture.create("nav-fallback")
			.page("prefix", ORIGIN + "/home/setup/step2", contractPage(NAV, ""))
			.page("query", ORIGIN + "/rest/runs?tab=exec", contractPage(NAV, ""))
			.page("query-other-param", ORIGIN + "/rest/runs?tab=exec&page=2", contractPage(NAV, ""))
			.page("segment-boundary", ORIGIN + "/homes", contractPage(NAV, ""))
			.page("no-match", ORIGIN + "/docs/x", contractPage(NAV, ""))
			.run();
	}

	private static void assertActive(String caseName, List<String> current, String source) {
		var r = report.get(caseName);
		assertNoShellErrors(r);
		assertList(() -> caseName, r.get("current"), current.toArray());
		assertEquals(source, mounted(r).get("activeNavSource"), caseName);
	}

	@Test void a01_pathPrefix() {
		assertActive("prefix", List.of("home", "setup"), "prefix");
	}

	@Test void a02_queryParamsMustMatch() {
		assertActive("query", List.of("runs", "exec"), "prefix");
		assertActive("query-other-param", List.of("runs", "exec"), "prefix");
	}

	@Test void a03_segmentBoundary_noMatch() {
		assertActive("segment-boundary", List.of(), "none");
	}

	@Test void a04_noMatch_noBanner() {
		assertActive("no-match", List.of(), "none");
	}
}
