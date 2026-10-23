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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Smoke test for the browser harness: one console page loads in Chromium from the live test server with no
 * script errors.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreHarness_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("harness", base, List.of(
			Map.of("name", "store", "waitUntil", "networkidle", "path", "/console/store",
				"queries", Map.of("title", "document.title", "pages", "document.querySelectorAll('#juneau-page').length")),
			Map.of("name", "actions", "path", "/console/about", "viewport", List.of(800, 600), "waitUntil", "load",
				"initScript", "window.__init = (window.__init || 0) + 1;",
				"actions", List.of(
					// The nav badge polls on every page; a reload that lands mid-fetch aborts it and logs E-JS-65 'Failed to fetch'.
					// A 'load' case therefore waits for the badge's fetch to finish before it navigates away.
					Map.of("waitFor", "performance.getEntriesByType('resource').some(r => r.name.includes('/changes/pending') && r.responseEnd > 0)"),
					Map.of("evaluate", "sessionStorage.setItem('h', '1')"),
					Map.of("reload", true),
					Map.of("goto", "/console/about?x=1"),
					Map.of("follow", "location.origin + '/console/about?y=2'")),
				"queries", Map.of("width", "window.innerWidth", "init", "window.__init", "search", "location.search",
					"kept", "sessionStorage.getItem('h')"))));
	}

	@Test void a01_storeLoadsCleanly() {
		var c = assertClean(report, "store");
		assertString("1", String.valueOf(query(c, "pages")));
		assertContains("Juneau Petstore", query(c, "title"));
	}

	@Test void a02_caseKeysAndNavigationActions() {
		var c = assertClean(report, "actions");
		assertString("800", String.valueOf(query(c, "width")));
		assertString("1", String.valueOf(query(c, "init")));   // the init script ran again in the new document
		assertString("?y=2", query(c, "search"));
		assertString("1", query(c, "kept"));   // reload, goto and follow stay in the case's one context
	}

	@Test void a03_everyCaseReportsItsWallTime() {
		assertTrue(((Number) assertClean(report, "store").get("ms")).longValue() > 0);
	}
}
