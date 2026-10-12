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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Custom cards page in the real browser: both custom types render, the card API works, each card mounts once,
 * and a second registration of a card type fails loudly.
 *
 * <p>
 * The {@code ctx.every} pause while the page is hidden is not asserted here: the gauge polls every 30 s and the shell
 * has no test hook for a shorter period, so a short wait could not tell a paused timer from a running one.  The
 * console UI's own tests cover the pause.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreCustomCard_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	private static final String RENDERED = "document.querySelector('#gauge-body meter') && document.querySelector('#available-body .petstore-kpi-value')";

	// Stubs fetch so the gauge reads window.__stock and the kpi's read aborts, spies on announcements, and puts a
	// decoy element carrying the gauge body's id ahead of the card in the document.
	private static final String STUB = String.join("\n",
		"window.__ann = [];",
		"window.JuneauViews = window.JuneauViews || {};",
		"window.JuneauViews.announce = function (t) { window.__ann.push(t); };",
		"window.__stock = 50;",
		"var real = window.fetch;",
		"window.fetch = function (url, opts) {",
		"  var u = String(url);",
		"  if (u.indexOf('/inventory?species=') >= 0)",
		"    return Promise.resolve(new Response(JSON.stringify({species:'DOG', available:window.__stock, pending:0, sold:0}),",
		"      {status:200, headers:{'Content-Type':'application/json'}}));",
		"  if (u.indexOf('/stats/available') >= 0) return Promise.reject(new DOMException('aborted', 'AbortError'));",
		"  return real.apply(this, arguments);",
		"};",
		"var d = document.createElement('div');",
		"d.id = 'gauge-body';",
		"d.setAttribute('data-decoy', '');",
		"document.body.insertBefore(d, document.body.firstChild);");

	// Stock 50, then 5 three times, back to 50, then 5 again: two crossings below low (20).
	private static final String READS = String.join("\n",
		"var api = window.JuneauConsole.cardApi('gauge');",
		"var at = function (n) { return function () { window.__stock = n; return api.reload(); }; };",
		"Promise.resolve().then(at(50)).then(at(5)).then(at(5)).then(at(5)).then(at(50)).then(at(5))",
		"  .then(function () { window.__gaugeDone = true; });",
		"Promise.resolve(window.JuneauConsole.refreshCard('available'))",
		"  .then(function () { window.__kpiDone = true; }, function () { window.__kpiDone = true; });");

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("custom-card", base, List.of(
			Map.of("name", "cards", "path", "/console/dev/cards",
				"actions", List.of(
					Map.of("waitFor", RENDERED),
					Map.of("evaluate", "window.__mountedBefore = window.petstoreMounted.slice()"),
					Map.of("click", "[data-cards-action=\"refresh\"]"),
					Map.of("waitFor", "window.JuneauConsole.cardApi('gauge').reads() >= 2"),
					Map.of("click", "[data-cards-action=\"api\"]")),
				"queries", Map.of(
					"kpi", "document.querySelector('#available-body .petstore-kpi-value').textContent",
					"kpiLabel", "document.querySelector('#available-body .petstore-kpi-label').textContent",
					"gaugeSpecies", "document.querySelector('#gauge-body meter').getAttribute('data-gauge-species')",
					"gaugeText", "document.querySelector('#gauge-body .petstore-gauge-text').textContent",
					"apiText", "document.getElementById('cards-api-result').textContent",
					"mountedBefore", "window.__mountedBefore",
					"types", "window.JuneauConsole.cardTypes()")),
			Map.of("name", "dup", "path", "/console/dev/cards", "allowFailedLoads", true,
				"actions", List.of(
					Map.of("waitFor", RENDERED),
					Map.of("evaluate", "try { window.JuneauConsole.registerCard('kpi', {render(){}}); window.__dup = 'none'; } catch (e) { window.__dup = e.code + ' ' + e.message; }")),
				"queries", Map.of("dup", "window.__dup")),
			Map.of("name", "stubbed", "path", "/console/dev/cards", "allowFailedLoads", true,
				"actions", List.of(
					Map.of("waitFor", RENDERED),
					Map.of("evaluate", STUB),
					Map.of("evaluate", READS),
					Map.of("waitFor", "window.__gaugeDone === true && window.__kpiDone === true")),
				"queries", Map.of(
					"lowStock", "window.__ann.filter(function (t) { return t.indexOf('DOG stock is below') === 0; }).length",
					"decoyChildren", "document.querySelector('[data-decoy]').childElementCount",
					"gaugeText", "document.querySelector('#gauge .petstore-gauge-text').textContent",
					"kpiErrors", "document.querySelectorAll('#available .jc-card-error').length"))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - rendering
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_kpiRendersTheServerCount() {
		var c = assertClean(report, "cards");
		var kpi = String.valueOf(query(c, "kpi"));
		assertTrue(kpi.matches("\\d+"), () -> "kpi value is not a count: " + kpi);
		assertString("Pets available", query(c, "kpiLabel"));
	}

	@Test void a02_gaugeRendersTheJavaShapedFragment() {
		var c = assertClean(report, "cards");
		assertString("DOG", query(c, "gaugeSpecies"));
		assertMatchesGlob("* DOG available (*%)", query(c, "gaugeText"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - card API
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_refreshCardAndCardApi() {
		assertMatchesGlob("DOG, * reads", query(assertClean(report, "cards"), "apiText"));
	}

	@Test void b02_eachCardMountsOnce() {
		@SuppressWarnings("unchecked") // the query returns a JSON array.
		var m = (List<String>) query(assertClean(report, "cards"), "mountedBefore");
		assertList(m.stream().filter(x -> x.startsWith("gauge:")).toList(), "gauge:inventory-gauge");
		assertList(m.stream().filter(x -> x.startsWith("available:")).toList(), "available:kpi");
	}

	@Test void b03_bothTypesRegistered() {
		assertContainsAll(String.valueOf(query(assertClean(report, "cards"), "types")), "kpi", "inventory-gauge");
	}

	@Test void b04_handlerDrawsInsideItsOwnCard() {
		var c = report.get("stubbed");
		assertEquals(0, ((Number)query(c, "decoyChildren")).intValue(), "the gauge drew into a same-id element outside its card");
		assertString("5 DOG available (0%)", query(c, "gaugeText"));
	}

	@Test void b05_lowStockIsAnnouncedOncePerCrossing() {
		assertEquals(2, ((Number)query(report.get("stubbed"), "lowStock")).intValue());
	}

	@Test void b06_abortedKpiReadPaintsNoError() {
		assertEquals(0, ((Number)query(report.get("stubbed"), "kpiErrors")).intValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - fail loud
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_secondRegistrationOfATypeFails() {
		assertString("E-JS-12 card type 'kpi' is already registered", query(report.get("dup"), "dup"));
	}
}
