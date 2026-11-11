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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The petstore charts in the real browser: the Store dashboard charts, and the Custom cards page's
 * stacked stock bands (an app decorator and a keyboard click published on the bus) and zoomable prices chart.
 */
@Covers(value="charts", pages={"/console/store", "/console/dev/cards"})
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreChart_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	/**
	 * The vertical distances between consecutive top-level cards, and the card gap token resolved to pixels through a
	 * probe element (the token may be a var() chain).
	 */
	private static final String GAPS = "(function () {"
		+ " var p = document.createElement('div'); p.style.marginTop = 'var(--jc-card-gap)'; document.body.appendChild(p);"
		+ " var want = parseFloat(getComputedStyle(p).marginTop); p.remove();"
		+ " var cs = [...document.querySelectorAll('.jc-main > .jc-card')];"
		+ " var got = []; for (var i = 1; i < cs.length; i++) got.push(cs[i].getBoundingClientRect().top - cs[i - 1].getBoundingClientRect().bottom);"
		+ " return { want: want, got: got }; })()";

	/** The decorated option a chart card drew, as a JS expression. */
	private static String option(String id) {
		return "window.JuneauConsole.cardApi('" + id + "').getOption()";
	}

	/** True once every named chart card has drawn. */
	private static String drawn(String...ids) {
		var sb = new StringBuilder("(function () { try { return ");
		for (var i = 0; i < ids.length; i++)
			sb.append(i == 0 ? "" : " && ").append("!!window.JuneauConsole.cardApi('").append(ids[i]).append("').chart() && !!").append(option(ids[i]));
		return sb.append("; } catch (e) { return false; } })()").toString();
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("chart", base, List.of(
			Map.of("name", "store", "path", "/console/store",
				"actions", List.of(
					Map.of("waitFor", "!!window.JuneauConsole.cardApi('store-sw')"),
					// Switcher views are built on first selection, so select each before reading its chart.
					Map.of("evaluate", select("store-sw", "revenue")),
					Map.of("waitFor", drawn("revenue-chart")),
					Map.of("evaluate", select("store-sw", "orders")),
					Map.of("waitFor", drawn("orders-trend")),
					Map.of("evaluate", select("store-sw", "pets.species")),
					Map.of("waitFor", drawn("by-species")),
					Map.of("evaluate", select("store-sw", "pets.status")),
					Map.of("waitFor", drawn("by-status")),
					Map.of("evaluate", select("store-sw", "pets.treemap")),
					Map.of("waitFor", drawn("pets-map")),
					Map.of("evaluate", select("store-sw", "pets.species")),
					Map.of("waitFor", "!!document.querySelector('#by-species')"),
					Map.of("waitFor", "!!document.querySelector('.jc-logo-symbol use')")),
				"queries", Map.ofEntries(
					Map.entry("logoHref", "document.querySelector('.jc-logo-symbol use').getAttribute('href')"),
					Map.entry("trend", option("orders-trend") + ".series.map(function (s) { return s.name; })"),
					Map.entry("speciesType", option("by-species") + ".series[0].type"),
					Map.entry("statusType", option("by-status") + ".series[0].type"),
					Map.entry("live", "window.JuneauConsole.charts.__liveCount()"),
					Map.entry("toggles", "document.querySelectorAll('.jc-chart-toggle').length"),
					Map.entry("showData", "[...document.querySelectorAll('button')].filter(b => /show data/i.test(b.textContent)).length"),
					Map.entry("speciesTabs", "[...document.querySelectorAll('#by-species [role=tab]')].map(t => t.textContent.trim())"),
					Map.entry("compact", "!!document.querySelector('#by-species [data-juneau-strip-compact], [data-juneau-strip-compact] #by-species')"),
					Map.entry("gaps", GAPS)),
				"fetches", Map.of()),
			Map.of("name", "cards", "path", "/console/dev/cards",
				"actions", List.of(
					Map.of("waitFor", drawn("bands")),
					Map.of("evaluate", PetstoreBrowser.select("cards-sw", "prices")),
					Map.of("waitFor", drawn("prices")),
					Map.of("evaluate", PetstoreBrowser.select("cards-sw", "bands")),
					Map.of("evaluate", "window.__picked = []; window.JuneauViews.bus.subscribe('app.band-picked', function (v) { window.__picked.push(v); }, { retained: false });"),
					Map.of("press", List.of("#bands .jc-chart-plot", "ArrowRight")),
					Map.of("press", List.of("#bands .jc-chart-plot", "Enter")),
					Map.of("waitFor", "window.__picked.length > 0"),
					Map.of("evaluate", "window.JuneauConsole.cardApi('prices').chart().dispatchAction({ type: 'dataZoom', dataZoomIndex: 0, start: 0, end: 50 })"),
					Map.of("waitFor", "!!window.JuneauViews.bus.get('zoom:prices')")),
				"queries", Map.of(
					"stacks", option("bands") + ".series.map(function (s) { return s.stack; })",
					"subtext", option("bands") + ".title.subtext",
					"picked", "window.__picked.map(function (v) { return { card: v.card, species: v.row && v.row.species }; })",
					"zoom", "window.JuneauViews.bus.get('zoom:prices')"),
				"fetches", Map.of("pets", "/petstore/pets"))));
	}

	/** The body of a {@code fetches} entry, after asserting its status is 200. */
	@SuppressWarnings({
		"unchecked" // fetches are {status, body} objects.
	})
	private static String fetched(Map<String,Object> c, String key) {
		var f = (Map<String,Object>)((Map<String,Object>)c.get("fetches")).get(key);
		assertEquals(200, ((Number)f.get("status")).intValue(), () -> key + ": " + f);
		return String.valueOf(f.get("body"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - Store dashboard
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_storeChartsDraw() {
		var c = assertClean(report, "store");
		assertList(query(c, "trend"), "Placed", "Delivered");
		assertString("pie", query(c, "speciesType"));
		assertString("bar", query(c, "statusType"));
		// Revenue, orders and the three pets views: a view the switcher has built stays alive when hidden.
		assertEquals(5, ((Number)query(c, "live")).intValue());
	}

	@Test void a02_headerDrawsThePawPrintLogo() {
		assertTrue(String.valueOf(query(assertClean(report, "store"), "logoHref")).endsWith("#juneau-sym-paw_print"));
	}

	@Test void a03_petsChartsHaveDataTabsAndNoShowDataButton() {
		var c = assertClean(report, "store");
		assertEquals(0, ((Number)query(c, "toggles")).intValue(), "no chart toggle button");
		assertEquals(0, ((Number)query(c, "showData")).intValue(), "no Show data button");
		var tabs = String.valueOf(query(c, "speciesTabs"));
		assertContains("Chart", tabs);
		assertContains("Data", tabs);
		assertEquals(Boolean.TRUE, query(c, "compact"), "the nested Data tabs are compact");
	}

	@SuppressWarnings({
		"unchecked" // gaps is a {want, got[]} object.
	})
	@Test void a04_topLevelCardsAreSeparatedByTheCardGap() {
		var g = (Map<String,Object>)query(assertClean(report, "store"), "gaps");
		var want = ((Number)g.get("want")).doubleValue();
		var got = (List<Object>)g.get("got");
		assertEquals(1, got.size(), "welcome and the switcher card");
		for (var d : got)
			assertEquals(want, ((Number)d).doubleValue(), 1.0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - Custom cards charts
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_bandsAreStacked() {
		assertList(query(assertClean(report, "cards"), "stacks"), "total", "total", "total");
	}

	@Test void b02_appDecoratorTotalsThePets() throws Exception {
		var c = assertClean(report, "cards");
		var pets = new JsonList(fetched(c, "pets")).size();
		assertString(pets + " pets in all", query(c, "subtext"));
	}

	@Test void b03_keyboardClickPublishesTheBand() {
		assertList(query(assertClean(report, "cards"), "picked"), "{card=bands,species=BIRD}");
	}

	@Test void b04_zoomWindowIsPublished() {
		@SuppressWarnings({
			"unchecked" // the retained value is a JSON object.
		})
		var z = (Map<String,Object>)query(assertClean(report, "cards"), "zoom");
		assertNotNull(z, "no zoom:prices value");
		assertEquals(0, ((Number)z.get("start")).intValue());
		assertEquals(50, ((Number)z.get("end")).intValue());
	}
}
