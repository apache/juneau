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
package org.apache.juneau.console.views;

import static org.apache.juneau.console.views.ConsoleBusBrowserSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Where a refused resolved URL (E-JS-83) is painted, in a real browser.  A card whose {@code {token}} params fill to an
 * empty string (so {@code /{seg}/{leaf}} becomes the protocol-relative {@code //changes}) gets the error inside its own
 * host, visible, with its earlier render torn down and no fetch issued.  A bare card has no element of its own (its host is a
 * document fragment), so its error goes to the page banner instead.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusResolvedUrl_BrowserTest extends TestBase {

	private static Map<String,Object> report;

	@BeforeAll
	static void probe() throws Exception {
		report = run("resolved-url", "resolved-url.json", true, "{\"declare\":[{\"topic\":\"app.target\",\"retain\":true}]}", "");
	}

	@Test void a01_errorIsInsideTheCardHostNotTheBanner() {
		var c = map(report.get("card"));
		assertEquals(Boolean.TRUE, c.get("found"), () -> report.toString());
		assertEquals("E-JS-83", c.get("code"));
		assertEquals(Boolean.TRUE, c.get("inHost"), () -> report.toString());
		assertEquals(Boolean.FALSE, c.get("inBanner"), () -> report.toString());
		assertEquals(1, num(c.get("count")), () -> "only the host card paints inline; the bare card uses the banner: " + report);
	}

	@Test void a02_errorIsVisibleAndNamesTheCardFieldAndUrl() {
		var c = map(report.get("card"));
		assertEquals(Boolean.TRUE, c.get("visible"), () -> report.toString());
		var t = c.get("text").toString();
		assertTrue(t.contains("'tasks'"), () -> t);
		assertTrue(t.contains("table.dataUrl"), () -> t);
		assertTrue(t.contains("'//changes'"), () -> t);
	}

	@Test void a03_earlierRenderIsTornDownAndNothingIsFetched() {
		var before = map(report.get("before"));
		assertEquals(Boolean.TRUE, before.get("hasRow"), () -> "the good target must render first: " + report);
		assertEquals(1, num(before.get("tables")), () -> report.toString());
		assertTrue(list(report.get("urlsBefore")).stream().anyMatch(u -> u.toString().endsWith("/api/changes")), () -> "the URL recording must capture the good fetch: " + report);
		var after = map(report.get("after"));
		assertEquals(Boolean.FALSE, after.get("hasRow"), () -> report.toString());
		assertEquals(0, num(after.get("tables")), () -> report.toString());
		assertTrue(list(report.get("fetchedAfter")).stream().noneMatch(u -> u.toString().contains("changes")), () -> "a refused URL must not be fetched: " + report);
	}

	@Test void a04_errorIsLoggedThroughConsoleError() {
		assertTrue(list(report.get("consoleErrors")).stream().anyMatch(l -> l.toString().startsWith("[juneau-console] ") && l.toString().contains("'tasks'") && l.toString().contains("//changes")),
			() -> report.toString());
	}

	@Test void a05_bareCardHasNoHostSoItsErrorGoesToThePageBanner() {
		var p = map(report.get("page"));
		assertTrue(list(p.get("codes")).contains("E-JS-83"), () -> report.toString());
		assertTrue(p.get("text").toString().contains("'note'"), () -> report.toString());
		assertEquals(1, num(map(report.get("bareCard")).get("inlineErrorsOnPage")), () -> "the bare card must add no inline error: " + report);
	}
}
