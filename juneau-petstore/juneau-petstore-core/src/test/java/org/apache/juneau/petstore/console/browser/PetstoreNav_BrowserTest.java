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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Nav in the real browser: one children row per selected ancestor and {@code aria-current} on each path link.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreNav_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String CURRENT = "[...document.querySelectorAll('[data-juneau-nav-id][aria-current=\"page\"]')].map(a => a.getAttribute('data-juneau-nav-id'))";
	private static final String CHILD_ROWS = "document.querySelectorAll('.juneau-page-nav-children').length";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("nav", base, List.of(
			Map.of("name", "flavor-html", "path", "/console/dev/flavors/html", "queries", Map.of("current", CURRENT, "rows", CHILD_ROWS)),
			Map.of("name", "about", "path", "/console/about", "queries", Map.of("current", CURRENT, "rows", CHILD_ROWS)),
			Map.of("name", "click-dev", "path", "/console/store",
				"actions", List.of(
					Map.of("click", "[data-juneau-nav-id=\"dev\"]"),
					Map.of("waitFor", "location.pathname.startsWith('/console/dev/')")),
				"queries", Map.of("current", CURRENT, "path", "location.pathname"))));
	}

	@Test void a01_depth3PathIsCurrentAtEachLevel() {
		var c = assertClean(report, "flavor-html");
		assertList(query(c, "current"), "dev", "flavors", "html");
		assertString("2", String.valueOf(query(c, "rows")));
	}

	@Test void a02_leafSectionHasNoChildRow() {
		var c = assertClean(report, "about");
		assertList(query(c, "current"), "about");
		assertString("0", String.valueOf(query(c, "rows")));
	}

	@Test void a03_clickingASectionNavigates() {
		var c = assertClean(report, "click-dev");
		assertString("/console/dev/cards", query(c, "path"));
		assertList(query(c, "current"), "dev", "cards");
	}
}
