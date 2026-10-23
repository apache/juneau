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

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The petstore charts in the real browser: the Store dashboard charts and revenue tile, and the Custom cards page's
 * stacked stock bands (an app decorator and a keyboard click published on the bus) and zoomable prices chart.
 */
@Covers(value="charts", pages={"/console/store", "/console/dev/cards"})
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreChart_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	/** The revenue the tile must show: the seeded total plus one priced pet delivered today before the page loads. */
	private static double pinned;

	private static String get(HttpClient http, String url) throws Exception {
		return http.send(HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString()).body();
	}

	/** Delivers one priced pet today, so a tile stuck at zero cannot pass. */
	private static void pinRevenue(String base) throws Exception {
		var http = HttpClient.newHttpClient();
		var before = new JsonMap(get(http, base + "/console/store/kpi/revenue")).get("value", Double.class);
		JsonMap pet = null;
		for (var p : new JsonList(get(http, base + "/petstore/pets")).elements(JsonMap.class))
			if (pet == null && p.get("price", Double.class) > 0)
				pet = p;
		assertNotNull(pet, "no priced pet to deliver");
		var today = LocalDate.ofInstant(PetstoreSeed.DEFAULT_CLOCK.instant(), ZoneOffset.UTC);
		var body = "{\"petId\":" + pet.getLong("id") + ",\"status\":\"DELIVERED\",\"shipDate\":\"" + today + "\"}";
		var res = http.send(HttpRequest.newBuilder(URI.create(base + "/petstore/orders")).header("Content-Type", "application/json").header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body)).build(), BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		pinned = Math.round((before + pet.get("price", Double.class)) * 100) / 100d;
	}

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
		pinRevenue(base);
		report = run("chart", base, List.of(
			Map.of("name", "store", "path", "/console/store",
				"actions", List.of(
					Map.of("waitFor", drawn("orders-trend", "by-species", "by-status", "pets-map")),
					Map.of("waitFor", "!!document.querySelector('#revenue-body .petstore-kpi-value')")),
				"queries", Map.of(
					"trend", option("orders-trend") + ".series.map(function (s) { return s.name; })",
					"speciesType", option("by-species") + ".series[0].type",
					"statusType", option("by-status") + ".series[0].type",
					"revenue", "document.querySelector('#revenue-body .petstore-kpi-value').textContent",
					"revenueLabel", "document.querySelector('#revenue-body .petstore-kpi-label').textContent",
					"live", "window.JuneauConsole.charts.__liveCount()"),
				"fetches", Map.of("revenue", "/console/store/kpi/revenue")),
			Map.of("name", "cards", "path", "/console/dev/cards",
				"actions", List.of(
					Map.of("waitFor", drawn("bands", "prices")),
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
		assertEquals(4, ((Number)query(c, "live")).intValue());
	}

	@Test void a02_revenueTileShowsTheServerValue() throws Exception {
		var c = assertClean(report, "store");
		assertTrue(pinned > 0, "no priced pet to deliver");
		assertEquals(pinned, new JsonMap(fetched(c, "revenue")).get("value", Double.class), 0.001);
		var text = String.valueOf(query(c, "revenue"));
		assertTrue(text.endsWith(" USD"), () -> "revenue has no unit: " + text);
		assertEquals(pinned, Double.parseDouble(text.substring(0, text.length() - 4)), 0.001);
		assertString("Revenue today", query(c, "revenueLabel"));
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
