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

import static org.apache.juneau.console.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Layout invariant for a page that stacks a hero block and a wide table card in a flex-column {@code .jc-main} (the
 * Support Console contract): sibling blocks never overlap, and the hero keeps its content height however tall the
 * card's content is.  The consumer's rule for the hero is {@code flex: 0 0 auto}; only the card flexes.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-console-views/pom.xml test`")
class PageFillLayout_BrowserTest extends TestBase {

	private static final String CONTRACT = """
		{"contractVersion":"1","title":"Layout","cards":[]}
		""";

	// Builds the consumer shape in the real page, then records every rectangle into window.__probe.
	private static final String BUILD = """
		(function () {
		  var main = document.createElement('main');
		  main.className = 'pf-main';
		  main.style.cssText = 'display:flex;flex-direction:column;height:420px;overflow:hidden;padding:0;margin:0';
		  main.innerHTML = '<div class="hero" style="flex:0 0 auto;height:65px">hero</div>'
		    + '<div class="card" style="flex:1 1 auto;min-height:0;overflow:auto"><div style="height:775px">rows</div></div>';
		  document.body.appendChild(main);
		  var r = function (s) { var b = main.querySelector(s).getBoundingClientRect(); return { top: b.top, bottom: b.bottom, height: b.height }; };
		  window.__probe = { hero: r('.hero'), card: r('.card'), main: main.getBoundingClientRect().height };
		})()
		""";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void load() throws Exception {
		report = ConsoleBrowserFixture.create("page-fill-layout")
			.page("layout", ORIGIN + "/layout", contractPage(CONTRACT, "", "", viewsPack()))
			.eval(BUILD)
			.run();
	}

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map shape.
	})
	private static Map<String,Number> rect(String key) {
		return (Map<String,Number>)probe(report.get("layout")).get(key);
	}

	@Test void a01_heroKeepsItsContentHeight() {
		assertEquals(65.0, rect("hero").get("height").doubleValue(), 0.5, () -> report.get("layout").toString());
	}

	@Test void a02_cardStartsBelowTheHero() {
		assertTrue(rect("card").get("top").doubleValue() >= rect("hero").get("bottom").doubleValue() - 1, () -> report.get("layout").toString());
	}

	@Test void a03_onlyTheCardAbsorbsTheRemainingHeight() {
		var card = rect("card").get("height").doubleValue();
		var hero = rect("hero").get("height").doubleValue();
		assertEquals(420.0, hero + card, 1.5, () -> report.get("layout").toString());
	}
}
