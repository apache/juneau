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
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Orders page's {@code day} URL-state directive in the real browser: {@code ?state=day(YYYY-MM-DD)} filters the
 * server-mode table to that day's orders, survives a reload, follows a history push and back/forward over it, composes with the ribbon's status
 * filter, and ignores a malformed value; and a click on the Store page's orders-trend chart lands there.
 *
 * <p>
 * The {@code window} directive on the same table is covered too: {@code ?state=window(start=...;end=...)} keeps the
 * orders whose ship date falls between the two days (either bound alone is open on the other side), survives a reload,
 * follows history, and composes with {@code day}.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreOrdersDay_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String QUERY = "/console/orders/query";
	private static final String ROW_IDS = "[...document.querySelectorAll('#orders tbody tr[data-juneau-row-id]')].map(tr => tr.getAttribute('data-juneau-row-id')).sort((a, b) => a - b).join(',')";
	private static final String PLOT = "[aria-labelledby=\"jc-chart-caption-orders-trend\"]";
	private static final String ROW_COUNT = "document.querySelectorAll('#orders tbody tr[data-juneau-row-id]').length";

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	private static Map<String,Map<String,Object>> report;

	/** Two days inside the trend chart's window, each carrying orders of our own, and the ids the table must show for each. */
	private static int daysBackA;
	private static String dayA;
	private static String dayB;
	private static String idsA;
	private static String idsB;
	private static String idsAPlaced;
	private static String allIds;

	/** Four orders of our own on far-future days, so a window over them is small and known: W1 and W2 inside {@code [W1, W3]}, W4 outside it. */
	private static final String W1 = "2033-01-10";
	private static final String W2 = "2033-01-12";
	private static final String W3 = "2033-01-14";
	private static final String W4 = "2033-01-20";
	private static String idsW1toW3;
	private static String idsFromW2;
	private static String idsW4;
	private static String idsW2;

	private static String send(String base, String path, String body) throws Exception {
		HttpRequest req;
		if (path.startsWith("/console/"))
			req = PetstoreTestServer.consolePost(base, path, body);
		else
			req = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json").header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
		var res = HTTP.send(req, BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return res.body();
	}

	private static void order(String base, String status, String day) throws Exception {
		send(base, "/petstore/orders", "{\"petId\":1,\"status\":\"" + status + "\",\"shipDate\":\"" + day + "\"}");
	}

	/** The ids, ascending and comma-joined, of every order the unfiltered query returns that passes {@code keep}. */
	private static String idsWhere(String base, java.util.function.Predicate<JsonMap> keep) throws Exception {
		var ids = new ArrayList<Long>();
		// The server caps a page, so walk the pages until one comes back short.
		for (var start = 0;; start += 25) {
			var body = send(base, QUERY, "{\"draw\":1,\"start\":" + start + ",\"length\":25,\"order\":[{\"column\":0,\"dir\":\"asc\"}],\"columns\":[{\"data\":\"id\"},{\"data\":\"status\"},{\"data\":\"shipDate\"}]}");
			var page = new JsonMap(body).getList("data").elements(JsonMap.class);
			var n = 0;
			for (var r : page) {
				n++;
				if (keep.test(r))
					ids.add(r.getLong("id"));
			}
			if (n < 25)
				break;
		}
		return ids.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
	}

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 15000);
	}

	private static Map<String,Object> eval(String js) {
		return Map.of("evaluate", js);
	}

	/** Waits for the table to show exactly {@code ids}. */
	private static Map<String,Object> showing(String ids) {
		return wait("(() => { try { return " + ROW_IDS + " === '" + ids + "'; } catch (e) { return false; } })()");
	}

	/** Appends the page's current address to a per-tab trail that survives the full page loads a chart click and Back/Forward cause. */
	private static Map<String,Object> trail() {
		return eval("sessionStorage.setItem('trail', (sessionStorage.getItem('trail') || '') + '|' + location.pathname + location.search)");
	}

	/** Waits for the Orders page to show {@code day}'s rows with that day in the address. */
	private static List<Map<String,Object>> ordersOn(String day, String ids) {
		return List.of(
			wait("location.pathname === '/console/orders' && location.search.includes('day(" + day + ")')"),
			showing(ids),
			trail());
	}

	/** Waits for the Store page to be back, fully loaded, with its revenue card rendered. */
	private static List<Map<String,Object>> storeAgain() {
		return List.of(wait("location.pathname === '/console/store' && document.readyState === 'complete'"), wait("!!document.querySelector('#revenue-body .petstore-kpi-value')"), trail());
	}

	/** Flattens actions and action lists, in order. */
	private static List<Map<String,Object>> join(Object... parts) {
		var out = new ArrayList<Map<String,Object>>();
		for (var p : parts) {
			if (p instanceof Map<?,?> m)
				out.add(cast(m));
			else
				for (var m : (List<?>) p)
					out.add(cast((Map<?,?>) m));
		}
		return out;
	}

	@SuppressWarnings({
		"unchecked" // Every action is a Map<String,Object>.
	})
	private static Map<String,Object> cast(Map<?,?> m) {
		return (Map<String,Object>) m;
	}

	private static Map<String,Object> chartQueries() {
		return Map.of("path", "location.pathname", "search", "location.search", "ids", ROW_IDS, "trail", "sessionStorage.getItem('trail')");
	}

	private static String inWindow(String from, String to) {
		return "window(" + (from == null ? "" : "start=" + from) + (from != null && to != null ? ";" : "") + (to == null ? "" : "end=" + to) + ")";
	}

	private static boolean shippedBetween(JsonMap r, String from, String to) {
		var d = r.getString("shipDate");
		if (d == null || d.length() < 10)
			return false;
		d = d.substring(0, 10);
		return (from == null || d.compareTo(from) >= 0) && (to == null || d.compareTo(to) <= 0);
	}

	private static String search(String day) {
		return "?state=day(" + day + ")";
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		var today = LocalDate.ofInstant(PetstoreSeed.DEFAULT_CLOCK.instant(), ZoneOffset.UTC);
		// Pick two quiet days in the chart's window (a page holds 25 rows), leaving room for the three orders added here.
		var perDay = new HashMap<String,Integer>();
		idsWhere(base, r -> { perDay.merge(String.valueOf(r.getString("shipDate")), 1, Integer::sum); return false; });
		var quiet = IntStream.rangeClosed(1, 25).boxed().filter(k -> perDay.getOrDefault(today.minusDays(k).toString(), 0) <= 21).toList();
		assertTrue(quiet.size() >= 2, perDay.toString());
		daysBackA = quiet.get(0);
		dayA = today.minusDays(daysBackA).toString();
		dayB = today.minusDays(quiet.get(1)).toString();
		order(base, "PLACED", dayA);
		order(base, "DELIVERED", dayA);
		order(base, "PLACED", dayB);
		idsA = idsWhere(base, r -> dayA.equals(r.getString("shipDate")));
		idsB = idsWhere(base, r -> dayB.equals(r.getString("shipDate")));
		idsAPlaced = idsWhere(base, r -> dayA.equals(r.getString("shipDate")) && "PLACED".equals(r.getString("status")));
		order(base, "PLACED", W1);
		order(base, "PLACED", W2);
		order(base, "DELIVERED", W3);
		order(base, "PLACED", W4);
		idsW1toW3 = idsWhere(base, r -> shippedBetween(r, W1, W3));
		idsFromW2 = idsWhere(base, r -> shippedBetween(r, W2, null));
		idsW2 = idsWhere(base, r -> shippedBetween(r, W2, W2));
		idsW4 = idsWhere(base, r -> shippedBetween(r, W4, W4));
		allIds = idsWhere(base, r -> true);
		assertEquals(3, idsW1toW3.split(",").length, idsW1toW3);
		assertEquals(3, idsFromW2.split(",").length, idsFromW2);
		assertTrue(idsA.split(",").length >= 2 && idsA.split(",").length <= 25, idsA);
		assertTrue(idsB.split(",").length >= 1 && idsB.split(",").length <= 25, idsB);
		assertNotEquals(idsA, idsB);

		var drawn = "(() => { try { const a = window.JuneauConsole.cardApi('orders-trend'); return !!a.chart() && !!a.getOption(); } catch (e) { return false; } })()";

		// The plot's keyboard walk: End jumps to today, and one ArrowLeft per day back reaches the day under test.
		var chartActions = new ArrayList<Map<String,Object>>();
		chartActions.add(wait(drawn));
		chartActions.add(Map.of("press", List.of(PLOT, "End")));
		for (var k = 0; k < daysBackA; k++)
			chartActions.add(Map.of("press", List.of(PLOT, "ArrowLeft")));
		chartActions.add(Map.of("press", List.of(PLOT, "Enter")));
		chartActions.add(wait("location.pathname === '/console/orders' && " + ROW_COUNT + " > 0"));
		chartActions.add(showing(idsA));

		report = run("orders-day", base, List.of(
			Map.of("name", "link", "path", "/console/orders" + search(dayA), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsA)),
				"queries", Map.of(
					"registered", "window.JuneauViews.urlState.hasDirective('day')",
					"ids", ROW_IDS,
					"search", "location.search")),
			Map.of("name", "none", "path", "/console/orders", "capture", QUERY, "waitUntil", "load",
				"actions", List.of(wait(ROW_COUNT + " > 0")),
				"queries", Map.of("search", "location.search", "count", ROW_COUNT)),
			Map.of("name", "malformed", "path", "/console/orders?state=day(not-a-date)", "capture", QUERY, "waitUntil", "load",
				"actions", List.of(wait(ROW_COUNT + " > 0")),
				"queries", Map.of("search", "location.search", "count", ROW_COUNT, "ids", ROW_IDS)),
			Map.of("name", "impossible", "path", "/console/orders?state=day(2031-02-30)", "capture", QUERY, "waitUntil", "load",
				"actions", List.of(wait(ROW_COUNT + " > 0")),
				"queries", Map.of("count", ROW_COUNT, "ids", ROW_IDS)),
			Map.of("name", "history", "path", "/console/orders" + search(dayA), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(
					showing(idsA),
					// The page has no day control, so the push is the one a link would make: a new entry, then the popstate the toolkit listens for.
					eval("history.pushState(null, '', location.pathname + '" + search(dayB) + "'); window.dispatchEvent(new PopStateEvent('popstate'));"),
					showing(idsB),
					eval("window.__afterPush = location.search"),
					Map.of("back", true),
					showing(idsA),
					eval("window.__afterBack = location.search"),
					Map.of("forward", true),
					showing(idsB)),
				"queries", Map.of(
					"afterPush", "window.__afterPush",
					"afterBack", "window.__afterBack",
					"search", "location.search",
					"ids", ROW_IDS)),
			Map.of("name", "reload", "path", "/console/orders" + search(dayB), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsB), Map.of("reload", true), showing(idsB)),
				"queries", Map.of("search", "location.search", "ids", ROW_IDS)),
			Map.of("name", "ribbon", "path", "/console/orders" + search(dayA), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(
					showing(idsA),
					Map.of("click", "#orders-body .juneau-view-ribbon-btn[aria-label=\"Placed\"]"),
					showing(idsAPlaced)),
				"queries", Map.of("search", "location.search", "ids", ROW_IDS)),
			Map.of("name", "window", "path", "/console/orders?state=" + inWindow(W1, W3), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsW1toW3)),
				"queries", Map.of(
					"registered", "window.JuneauViews.urlState.hasDirective('window')",
					"ids", ROW_IDS,
					"search", "location.search")),
			Map.of("name", "window-open", "path", "/console/orders?state=" + inWindow(W2, null), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsFromW2)),
				"queries", Map.of("ids", ROW_IDS)),
			Map.of("name", "window-history", "path", "/console/orders?state=" + inWindow(W1, W3), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(
					showing(idsW1toW3),
					eval("history.pushState(null, '', location.pathname + '?state=" + inWindow(W4, W4) + "'); window.dispatchEvent(new PopStateEvent('popstate'));"),
					showing(idsW4),
					eval("window.__afterPush = location.search"),
					Map.of("back", true),
					showing(idsW1toW3),
					eval("window.__afterBack = location.search"),
					Map.of("forward", true),
					showing(idsW4)),
				"queries", Map.of("afterPush", "window.__afterPush", "afterBack", "window.__afterBack", "search", "location.search", "ids", ROW_IDS)),
			Map.of("name", "window-reload", "path", "/console/orders?state=" + inWindow(W1, W3), "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsW1toW3), Map.of("reload", true), showing(idsW1toW3)),
				"queries", Map.of("search", "location.search", "ids", ROW_IDS)),
			Map.of("name", "window-day", "path", "/console/orders?state=" + inWindow(W1, W3) + ";day(" + W2 + ")", "capture", QUERY, "waitUntil", "load",
				"actions", List.of(showing(idsW2)),
				"queries", Map.of("ids", ROW_IDS)),
			Map.of("name", "window-bad", "path", "/console/orders?state=window(start=garbage;end=nonsense)", "capture", QUERY, "waitUntil", "load",
				"actions", List.of(wait(ROW_COUNT + " > 0")),
				"queries", Map.of("ids", ROW_IDS)),
			Map.of("name", "chart", "path", "/console/store", "waitUntil", "load",
				"actions", chartActions,
				"queries", Map.of("path", "location.pathname", "search", "location.search", "ids", ROW_IDS)),
			// The chart's activation is a full page load (location.assign), so Back and Forward are real navigations, not popstates.
			Map.of("name", "chartBack", "path", "/console/store", "waitUntil", "load",
				"actions", join(chartActions, trail(), Map.of("back", true), storeAgain(), Map.of("forward", true), ordersOn(dayA, idsA)),
				"queries", chartQueries()),
			// An in-page day change is a replaceState: it rewrites the Orders entry, so one Back still leaves for the Store and Forward returns to the new day.
			Map.of("name", "chartReplace", "path", "/console/store", "waitUntil", "load",
				"actions", join(chartActions, trail(),
					eval("window.JuneauViews.urlState.setDirective(document.getElementById('orders'), 'day', '" + dayB + "')"),
					ordersOn(dayB, idsB), Map.of("back", true), storeAgain(), Map.of("forward", true), ordersOn(dayB, idsB)),
				"queries", chartQueries()),
			// A history push made after the chart landing: popstate re-reads the day in the same document, then Back and Forward cross the full loads either side of it.
			Map.of("name", "chartPop", "path", "/console/store", "waitUntil", "load",
				"actions", join(chartActions, trail(),
					eval("history.pushState(null, '', location.pathname + '" + search(dayB) + "'); window.dispatchEvent(new PopStateEvent('popstate'));"),
					ordersOn(dayB, idsB), Map.of("back", true), ordersOn(dayA, idsA), Map.of("back", true), storeAgain(),
					Map.of("forward", true), ordersOn(dayA, idsA), Map.of("forward", true), ordersOn(dayB, idsB)),
				"queries", chartQueries())));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - the directive
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_directiveIsRegisteredAndALinkFiltersTheTable() {
		var c = assertClean(report, "link");
		assertEquals(true, query(c, "registered"));
		assertEquals(idsA, query(c, "ids"));
		assertDay(dayA, query(c, "search"));
	}

	@Test void a02_theRequestCarriesTheDay() {
		var c = assertClean(report, "link");
		assertTrue(urls(c).stream().anyMatch(u -> u.contains("day=" + dayA)), () -> urls(c).toString());
	}

	@Test void a03_noDirectiveMeansNoFilter() {
		var c = assertClean(report, "none");
		assertFalse(String.valueOf(query(c, "search")).contains("day("), () -> String.valueOf(query(c, "search")));
		assertTrue(urls(c).stream().noneMatch(u -> u.contains("day=")), () -> urls(c).toString());
	}

	@Test void a04_malformedValueIsIgnored() {
		var c = assertClean(report, "malformed");
		assertTrue(urls(c).stream().noneMatch(u -> u.contains("day=")), () -> urls(c).toString());
		// The same rows as the unfiltered first page (ids ascending), not merely as many.
		var firstPage = Arrays.stream(allIds.split(",")).limit(25).collect(Collectors.joining(","));
		assertEquals(firstPage, query(c, "ids"));
	}

	@Test void a05_anImpossibleDateIsIgnored() {
		var c = assertClean(report, "impossible");
		assertTrue(urls(c).stream().noneMatch(u -> u.contains("day=")), () -> urls(c).toString());
		assertEquals(Arrays.stream(allIds.split(",")).limit(25).collect(Collectors.joining(",")), query(c, "ids"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - history, reload, ribbon
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_pushBackForwardFollowTheAddress() {
		var c = assertClean(report, "history");
		assertDay(dayB, query(c, "afterPush"));
		assertDay(dayA, query(c, "afterBack"));
		assertDay(dayB, query(c, "search"));
		assertEquals(idsB, query(c, "ids"));
	}

	@Test void b02_reloadKeepsTheDay() {
		var c = assertClean(report, "reload");
		assertDay(dayB, query(c, "search"));
		assertEquals(idsB, query(c, "ids"));
	}

	@Test void b03_ribbonFilterAndsWithTheDay() {
		var c = assertClean(report, "ribbon");
		assertEquals(idsAPlaced, query(c, "ids"));
		assertTrue(String.valueOf(query(c, "search")).contains("day(" + dayA + ")"), () -> String.valueOf(query(c, "search")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - the window directive
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_windowLinkKeepsTheOrdersBetweenTheBounds() {
		var c = assertClean(report, "window");
		assertEquals(true, query(c, "registered"));
		assertEquals(idsW1toW3, query(c, "ids"));
		assertTrue(String.valueOf(query(c, "search")).contains(inWindow(W1, W3)), () -> String.valueOf(query(c, "search")));
		assertTrue(urls(c).stream().anyMatch(u -> u.contains("start=" + W1) && u.contains("end=" + W3)), () -> urls(c).toString());
	}

	@Test void d02_oneBoundIsOpenOnTheOtherSide() {
		var c = assertClean(report, "window-open");
		assertEquals(idsFromW2, query(c, "ids"));
		assertTrue(urls(c).stream().anyMatch(u -> u.contains("start=" + W2) && ! u.contains("end=")), () -> urls(c).toString());
	}

	@Test void d03_pushBackForwardFollowTheAddress() {
		var c = assertClean(report, "window-history");
		assertTrue(String.valueOf(query(c, "afterPush")).contains(inWindow(W4, W4)), () -> String.valueOf(query(c, "afterPush")));
		assertTrue(String.valueOf(query(c, "afterBack")).contains(inWindow(W1, W3)), () -> String.valueOf(query(c, "afterBack")));
		assertTrue(String.valueOf(query(c, "search")).contains(inWindow(W4, W4)), () -> String.valueOf(query(c, "search")));
		assertEquals(idsW4, query(c, "ids"));
	}

	@Test void d04_reloadKeepsTheWindow() {
		var c = assertClean(report, "window-reload");
		assertTrue(String.valueOf(query(c, "search")).contains(inWindow(W1, W3)), () -> String.valueOf(query(c, "search")));
		assertEquals(idsW1toW3, query(c, "ids"));
	}

	@Test void d05_windowAndsWithTheDay() {
		assertEquals(idsW2, query(assertClean(report, "window-day"), "ids"));
	}

	@Test void d06_unusableBoundsAreIgnored() {
		var firstPage = Arrays.stream(allIds.split(",")).limit(25).collect(Collectors.joining(","));
		assertEquals(firstPage, query(assertClean(report, "window-bad"), "ids"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - the trend chart
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_clickingTheTrendLandsOnThatDay() {
		var c = assertClean(report, "chart");
		assertEquals("/console/orders", query(c, "path"));
		assertDay(dayA, query(c, "search"));
		assertEquals(idsA, query(c, "ids"));
	}

	@Test void c02_backAndForwardAfterAChartClickRoundTrip() {
		var c = assertClean(report, "chartBack");
		assertTrail(c, "/console/orders:" + dayA, "/console/store", "/console/orders:" + dayA);
		assertEquals("/console/orders", query(c, "path"));
		assertDay(dayA, query(c, "search"));
		assertEquals(idsA, query(c, "ids"));
	}

	@Test void c03_aReplacedDayIsWhatForwardReturnsTo() {
		var c = assertClean(report, "chartReplace");
		assertTrail(c, "/console/orders:" + dayA, "/console/orders:" + dayB, "/console/store", "/console/orders:" + dayB);
		assertDay(dayB, query(c, "search"));
		assertEquals(idsB, query(c, "ids"));
	}

	@Test void c04_popAfterAChartLandingThenBackAndForwardAcrossTheLoads() {
		var c = assertClean(report, "chartPop");
		assertTrail(c, "/console/orders:" + dayA, "/console/orders:" + dayB, "/console/orders:" + dayA, "/console/store", "/console/orders:" + dayA, "/console/orders:" + dayB);
		assertDay(dayB, query(c, "search"));
		assertEquals(idsB, query(c, "ids"));
	}

	/** The visited addresses in order; an expected {@code path:day} entry means that path with {@code day(day)} in its search, and a bare path means that path only. */
	private static void assertTrail(Map<String,Object> c, String... expected) {
		var actual = String.valueOf(query(c, "trail")).substring(1).split("\\|");
		assertEquals(expected.length, actual.length, () -> Arrays.toString(actual));
		for (var i = 0; i < expected.length; i++) {
			var parts = expected[i].split(":");
			final var at = actual[i];
			assertTrue(at.startsWith(parts[0]), at);
			if (parts.length > 1)
				assertDay(parts[1], at);
		}
	}

	/** The address carries {@code day(day)} among whatever other state the page keeps there. */
	private static void assertDay(String day, Object search) {
		assertTrue(String.valueOf(search).contains("day(" + day + ")"), () -> String.valueOf(search));
	}

	@SuppressWarnings({
		"unchecked" // requests is a JSON array of {url, method, postData}.
	})
	private static List<String> urls(Map<String,Object> c) {
		return ((List<Map<String,Object>>) c.get("requests")).stream().map(r -> String.valueOf(r.get("url"))).toList();
	}
}
