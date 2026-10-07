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
 * P9 in the real browser: each stock theme resolves to a distinct set of token values, the custom theme's leaf and
 * alias resolve, and every theme is screenshotted to {@code target/js/snapshots} (CI artifacts only, OQ9).
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreThemes_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final List<String> STOCK = List.of("open", "light-red", "light-brown", "red", "gray");
	// Tokens every stock pack defines and that differ between open and gray (checked against juneau-theme-*.css).
	private static final String TOKENS = "(() => { const s = getComputedStyle(document.documentElement);"
		+ " return ['--jc-chrome-bg', '--jc-border', '--jc-text', '--jc-accent-selected', '--jc-pill-red-bg'].map(n => s.getPropertyValue(n).trim()); })()";
	// The custom theme's own list: --jc-tab-bar-bg is an alias declared only by the custom <@theme> block.
	private static final String CUSTOM_TOKENS = "(() => { const s = getComputedStyle(document.documentElement);"
		+ " return ['--jc-pill-red-bg', '--jc-tab-bar-bg', '--jc-card-bg'].map(n => s.getPropertyValue(n).trim()); })()";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		var cases = new ArrayList<Map<String,Object>>();
		for (var t : concat(STOCK, "custom"))
			cases.add(Map.of("name", t, "path", "/console/dev/themes?theme=" + t,
				"actions", List.of(Map.of("screenshot", "theme-" + t + ".png")),
				"queries", Map.of("tokens", "custom".equals(t) ? CUSTOM_TOKENS : TOKENS)));
		report = run("themes", base, cases);
	}

	private static List<String> concat(List<String> l, String x) {
		var out = new ArrayList<>(l);
		out.add(x);
		return out;
	}

	@SuppressWarnings("unchecked") // tokens is a JSON array of strings.
	private static List<String> tokens(String theme) {
		return (List<String>) query(assertClean(report, theme), "tokens");
	}

	@Test void a01_everyTokenResolvesInEveryTheme() {
		for (var t : concat(STOCK, "custom"))
			for (var v : tokens(t))
				assertNotEmpty(() -> "Theme '" + t + "' left a token unresolved: " + tokens(t), v);
	}

	@Test void a02_stockThemesAreDistinct() {
		var seen = new HashSet<List<String>>();
		for (var t : STOCK)
			seen.add(tokens(t));
		assertSize(STOCK.size(), seen);
	}

	@Test void a03_customLeafAndAliasResolve() {
		var c = tokens("custom");
		assertString("#fdeceb", c.get(0));  // --jc-pill-red-bg leaf override
		assertString(c.get(2), c.get(1));   // --jc-tab-bar-bg follows --jc-card-bg
	}
}
