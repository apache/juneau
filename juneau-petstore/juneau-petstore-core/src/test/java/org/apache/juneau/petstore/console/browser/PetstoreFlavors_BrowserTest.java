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
 * P12 in the real browser: each server flavor's fragment renders the same pets inside the console chrome with no
 * second chrome, and the React subtab iframes {@code /petstore-ui}.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreFlavors_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String TABLE = "#flavor table.petstore-flavor";

	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> serverFlavor(String engine) {
		return Map.of("name", engine, "path", "/console/dev/flavors/" + engine,
			"actions", List.of(Map.of("waitFor", "document.querySelector('" + TABLE + " td')")),
			"queries", Map.of(
				"names", "[...document.querySelectorAll('" + TABLE + " tr td:first-child')].map(td => td.textContent.trim())",
				"flavor", "document.querySelector('" + TABLE + "').getAttribute('data-flavor')",
				"islands", "document.querySelectorAll('#juneau-page').length",
				"navs", "document.querySelectorAll('nav.juneau-page-nav').length"));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("flavors", base, List.of(
			serverFlavor("html"), serverFlavor("freemarker"), serverFlavor("mustache"),
			Map.of("name", "react", "path", "/console/dev/flavors/react",
				"actions", List.of(Map.of("waitFor", "document.querySelector('#flavor iframe')")),
				"queries", Map.of(
					"frame", "new URL(document.querySelector('#flavor iframe').src).pathname",
					"title", "document.querySelector('#flavor iframe').title"))));
	}

	@SuppressWarnings("unchecked") // names is a JSON array of strings.
	private static List<String> names(String engine) {
		return (List<String>) query(assertClean(report, engine), "names");
	}

	@Test void a01_threeFlavorsShowTheSameTenPets() {
		var html = names("html");
		assertSize(10, html);
		assertList(() -> "freemarker differs from html", names("freemarker"), html.toArray());
		assertList(() -> "mustache differs from html", names("mustache"), html.toArray());
	}

	@Test void a02_eachFragmentIsItsOwnFlavorWithNoSecondChrome() {
		for (var e : List.of("html", "freemarker", "mustache")) {
			var c = assertClean(report, e);
			assertString(e, query(c, "flavor"));
			assertString("1", String.valueOf(query(c, "islands")));
			assertString("1", String.valueOf(query(c, "navs")));
		}
	}

	@Test void a03_reactIframesTheApp() {
		var c = assertClean(report, "react");
		assertString("/petstore-ui", query(c, "frame"));
		assertString("Petstore React app", query(c, "title"));
	}
}
