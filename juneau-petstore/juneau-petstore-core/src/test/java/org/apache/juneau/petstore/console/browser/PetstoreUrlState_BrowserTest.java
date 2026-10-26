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

import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * URL state in the real browser, through the Custom cards page's own {@code species} directive: {@code ?state=species(...)}
 * picks the gauge's species, survives a reload, and back/forward restores it.
 *
 * <p>
 * It also covers the pets table's {@code ids} directive: {@code ?state=ids(a,b)} sends {@code ids=a,b} with the table's
 * server requests and shows only those pets (still under the ribbon's default filters), survives a reload, and follows
 * history.  The built-in {@code day} and {@code window} directives are on the Orders page ({@link PetstoreOrdersDay_BrowserTest}),
 * so with {@code species} here every URL-state directive the petstore configures is covered, which retires the {@code url-state}
 * pending row in {@link PetstoreCoverage_Test}.
 */
@Covers(value="url-state", pages={"/console/pets", "/console/orders", "/console/dev/cards"})
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreUrlState_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String SPECIES = "document.querySelector('#gauge-body meter').getAttribute('data-gauge-species')";

	private static String showing(String species) {
		return "!!document.querySelector('#gauge-body meter[data-gauge-species=\"" + species + "\"]')";
	}

	private static final String PETS_QUERY = "/console/pets/query";
	private static final String PET_IDS = "[...document.querySelectorAll('#pets tbody tr[data-juneau-row-id]')].map(tr => tr.getAttribute('data-juneau-row-id')).sort((a, b) => a - b).join(',')";
	private static final String PET_COUNT = "document.querySelectorAll('#pets tbody tr[data-juneau-row-id]').length";

	private static Map<String,Map<String,Object>> report;

	/** Three pets the pets page's default ribbon filters (available mammals) show: a and b together, and c alone. */
	private static long petA, petB, petC;

	/** A sold pet, which the ribbon's available filter hides. */
	private static long petSold;

	private static String pets(long...ids) {
		return Arrays.stream(ids).sorted().mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
	}

	private static Map<String,Object> petsShowing(String ids) {
		return Map.of("waitFor", "(() => { try { return " + PET_IDS + " === '" + ids + "'; } catch (e) { return false; } })()", "timeoutMs", 15000);
	}

	private static void findPets(String base) throws Exception {
		var body = "{\"draw\":1,\"start\":0,\"length\":3,\"order\":[{\"column\":0,\"dir\":\"asc\"}],\"columns\":["
			+ "{\"data\":\"id\"},{\"data\":\"status\",\"search\":{\"value\":\"$eq(AVAILABLE)\"}},"
			+ "{\"data\":\"species\",\"search\":{\"value\":\"$in(DOG,CAT,MOUSE,RABBIT)\"}}]}";
		var res = HttpClient.newHttpClient().send(PetstoreTestServer.consolePost(base, PETS_QUERY, body), BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		var rows = new JsonMap(res.body()).getList("data");
		assertSize(3, rows);
		petA = rows.getMap(0).getLong("id");
		petB = rows.getMap(1).getLong("id");
		petC = rows.getMap(2).getLong("id");
		var soldBody = "{\"draw\":1,\"start\":0,\"length\":1,\"order\":[{\"column\":0,\"dir\":\"asc\"}],\"columns\":[{\"data\":\"id\"},{\"data\":\"status\",\"search\":{\"value\":\"$eq(SOLD)\"}}]}";
		var sold = HttpClient.newHttpClient().send(PetstoreTestServer.consolePost(base, PETS_QUERY, soldBody), BodyHandlers.ofString());
		assertEquals(200, sold.statusCode(), sold::body);
		petSold = new JsonMap(sold.body()).getList("data").getMap(0).getLong("id");
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		findPets(base);
		report = run("url-state", base, List.of(
			Map.of("name", "history", "waitUntil", "load", "path", "/console/dev/cards?state=species(fish)",
				"actions", List.of(
					Map.of("waitFor", showing("FISH")),
					Map.of("evaluate", "window.__first = " + SPECIES),
					Map.of("click", "[data-cards-action=\"cat\"]"),
					Map.of("waitFor", showing("CAT")),
					Map.of("evaluate", "window.__afterCat = location.search"),
					Map.of("back", true),
					Map.of("waitFor", showing("FISH")),
					Map.of("evaluate", "window.__afterBack = location.search"),
					Map.of("forward", true),
					Map.of("waitFor", showing("CAT"))),
				"queries", Map.of(
					"registered", "window.JuneauViews.urlState.hasDirective('species')",
					"first", "window.__first",
					"afterCat", "window.__afterCat",
					"afterBack", "window.__afterBack",
					"species", SPECIES,
					"api", "window.JuneauConsole.cardApi('gauge').species()")),
			Map.of("name", "reload", "waitUntil", "load", "path", "/console/dev/cards?state=species(cat)",
				"actions", List.of(
					Map.of("waitFor", showing("CAT")),
					Map.of("reload", true),
					Map.of("waitFor", showing("CAT"))),
				"queries", Map.of("search", "location.search", "species", SPECIES)),
			Map.of("name", "unknown", "waitUntil", "load", "path", "/console/dev/cards?state=species(dragon)",
				"actions", List.of(Map.of("waitFor", "!!document.querySelector('#gauge-body meter')")),
				"queries", Map.of("species", SPECIES)),
			Map.of("name", "reject", "waitUntil", "load", "path", "/console/dev/cards",
				"actions", List.of(
					Map.of("waitFor", showing("DOG")),
					Map.of("evaluate", "window.__length = history.length; window.JuneauConsole.cardApi('gauge').setSpecies('dragon').then(() => window.__rejected = 'no', e => window.__rejected = e.message)"),
					Map.of("waitFor", "!!window.__rejected")),
				"queries", Map.of("rejected", "window.__rejected", "search", "location.search", "grew", "history.length - window.__length", "species", SPECIES)),
			Map.of("name", "store", "path", "/console/store",
				"actions", List.of(Map.of("waitFor", "!!document.querySelector('#revenue-body .petstore-kpi-value')")),
				"queries", Map.of("registered", "window.JuneauViews.urlState.hasDirective('species')")),
			Map.of("name", "ids-link", "waitUntil", "load", "path", "/console/pets?state=ids(" + petA + "," + petB + ")", "capture", PETS_QUERY,
				"actions", List.of(petsShowing(pets(petA, petB))),
				"queries", Map.of(
					"registered", "window.JuneauViews.urlState.hasDirective('ids')",
					"ids", PET_IDS,
					"search", "location.search")),
			Map.of("name", "ids-none", "waitUntil", "load", "path", "/console/pets", "capture", PETS_QUERY,
				"actions", List.of(Map.of("waitFor", PET_COUNT + " > 2")),
				"queries", Map.of("count", PET_COUNT, "search", "location.search")),
			Map.of("name", "ids-history", "waitUntil", "load", "path", "/console/pets?state=ids(" + petA + "," + petB + ")", "capture", PETS_QUERY,
				"actions", List.of(
					petsShowing(pets(petA, petB)),
					// The page has no ids control, so the push is the one a link would make: a new entry, then the popstate the toolkit listens for.
					Map.of("evaluate", "history.pushState(null, '', location.pathname + '?state=ids(" + petC + ")'); window.dispatchEvent(new PopStateEvent('popstate'));"),
					petsShowing(pets(petC)),
					Map.of("evaluate", "window.__afterPush = location.search"),
					Map.of("back", true),
					petsShowing(pets(petA, petB)),
					Map.of("evaluate", "window.__afterBack = location.search"),
					Map.of("forward", true),
					petsShowing(pets(petC))),
				"queries", Map.of("afterPush", "window.__afterPush", "afterBack", "window.__afterBack", "search", "location.search", "ids", PET_IDS)),
			Map.of("name", "ids-reload", "waitUntil", "load", "path", "/console/pets?state=ids(" + petB + ")", "capture", PETS_QUERY,
				"actions", List.of(petsShowing(pets(petB)), Map.of("reload", true), petsShowing(pets(petB))),
				"queries", Map.of("search", "location.search", "ids", PET_IDS)),
			Map.of("name", "ids-garbage", "waitUntil", "load", "path", "/console/pets?state=ids(garbage)", "capture", PETS_QUERY,
				"actions", List.of(Map.of("waitFor", PET_COUNT + " > 2")),
				"queries", Map.of("count", PET_COUNT)),
			Map.of("name", "ids-ribbon", "waitUntil", "load", "path", "/console/pets?state=ids(" + petA + "," + petSold + ")", "capture", PETS_QUERY,
				"actions", List.of(petsShowing(pets(petA))),
				"queries", Map.of("ids", PET_IDS))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - the species directive
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_directiveIsRegistered() {
		assertEquals(true, query(assertClean(report, "history"), "registered"));
	}

	@Test void a02_linkPicksTheSpecies() {
		assertString("FISH", query(assertClean(report, "history"), "first"));
	}

	@Test void a03_settingTheSpeciesPushesAHistoryEntry() {
		var c = assertClean(report, "history");
		assertString("?state=species(CAT)", query(c, "afterCat"));
		assertString("?state=species(fish)", query(c, "afterBack"));
	}

	@Test void a04_forwardRestoresTheNewSpecies() {
		var c = assertClean(report, "history");
		assertString("CAT", query(c, "species"));
		assertString("CAT", query(c, "api"));
	}

	@Test void a05_reloadKeepsTheSpecies() {
		var c = assertClean(report, "reload");
		assertString("?state=species(cat)", query(c, "search"));
		assertString("CAT", query(c, "species"));
	}

	@Test void a06_unknownSpeciesFallsBackToTheCardBody() {
		assertString("DOG", query(assertClean(report, "unknown"), "species"));
	}

	@Test void a07_unknownSpeciesIsRejectedWithoutAHistoryEntry() {
		var c = assertClean(report, "reject");
		assertString("Unknown species 'dragon'", query(c, "rejected"));
		assertString("", query(c, "search"));
		assertEquals(0, ((Number) query(c, "grew")).intValue());
		assertString("DOG", query(c, "species"));
	}

	@Test void a08_pagesWithoutAGaugeDontRegisterTheDirective() {
		assertEquals(false, query(assertClean(report, "store"), "registered"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the pets table's ids directive
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"unchecked" // requests is a JSON array of {url, method, postData}.
	})
	private static List<String> idParams(Map<String,Object> c) {
		return ((List<Map<String,Object>>) c.get("requests")).stream().map(r -> URLDecoder.decode(String.valueOf(r.get("url")), java.nio.charset.StandardCharsets.UTF_8)).toList();
	}

	@Test void b01_linkShowsOnlyThoseRows() {
		var c = assertClean(report, "ids-link");
		assertEquals(true, query(c, "registered"));
		assertString(pets(petA, petB), query(c, "ids"));
		assertTrue(String.valueOf(query(c, "search")).contains("ids(" + petA + "," + petB + ")"), () -> String.valueOf(query(c, "search")));
	}

	@Test void b02_theRequestCarriesTheIds() {
		var c = assertClean(report, "ids-link");
		assertTrue(idParams(c).stream().anyMatch(u -> u.contains("ids=" + petA + "," + petB)), () -> idParams(c).toString());
	}

	@Test void b03_noDirectiveMeansNoFilter() {
		var c = assertClean(report, "ids-none");
		assertTrue(((Number) query(c, "count")).intValue() > 2);
		assertTrue(idParams(c).stream().noneMatch(u -> u.contains("ids=")), () -> idParams(c).toString());
	}

	@Test void b04_pushBackForwardFollowTheAddress() {
		var c = assertClean(report, "ids-history");
		assertTrue(String.valueOf(query(c, "afterPush")).contains("ids(" + petC + ")"), () -> String.valueOf(query(c, "afterPush")));
		assertTrue(String.valueOf(query(c, "afterBack")).contains("ids(" + petA + "," + petB + ")"), () -> String.valueOf(query(c, "afterBack")));
		assertTrue(String.valueOf(query(c, "search")).contains("ids(" + petC + ")"), () -> String.valueOf(query(c, "search")));
		assertString(pets(petC), query(c, "ids"));
	}

	@Test void b05_reloadKeepsTheIds() {
		var c = assertClean(report, "ids-reload");
		// The address carries the table's own tab and sort state alongside the ids.
		assertTrue(String.valueOf(query(c, "search")).contains("ids(" + petB + ")"), () -> String.valueOf(query(c, "search")));
		assertString(pets(petB), query(c, "ids"));
	}

	@Test void b06_unusableIdsAreIgnored() {
		var c = assertClean(report, "ids-garbage");
		assertTrue(((Number) query(c, "count")).intValue() > 2);
	}

	@Test void b07_idsAndWithTheRibbonDefaults() {
		// The sold pet is not available, so the ribbon's default filters still drop it from ids(a,sold).
		assertString(pets(petA), query(assertClean(report, "ids-ribbon"), "ids"));
	}
}
