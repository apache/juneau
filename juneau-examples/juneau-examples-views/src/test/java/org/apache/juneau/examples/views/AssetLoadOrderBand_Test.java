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
package org.apache.juneau.examples.views;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * Build-time guard for the five-position stylesheet/script asset-order band every in-tree page emitter must
 * follow:
 *
 * <pre>
 * 1. vendor stylesheets
 * 2. juneau-views.css                                              (the views base layer)
 * 3. first-party widget stylesheets that build on the views layer  (e.g. juneau-calendar.css)
 * 4. consumer theme                                                (e.g. chrome.css)
 * 5. page-local &lt;style&gt;
 * </pre>
 *
 * <p>
 * The band is nowhere enforced today: each page template links its stylesheets/scripts in whatever order a
 * hand-written text block happens to declare, and nothing notices if that order regresses. This guard walks
 * each in-tree page emitter's actually-<b>emitted</b> {@code <link>} sequence &mdash; over real HTTP, against
 * the same in-process harness {@code ExampleViewsEndToEnd_Test}/{@code ExampleCalendarEndToEnd_Test} already
 * boot &mdash; and asserts the band positions it finds are non-decreasing. Only the positions a given page
 * actually links are asserted; a page with no widget-layer stylesheet is not required to invent one.
 *
 * <p>
 * The JS half of the same relationship &mdash; {@code juneau-views.js} must load before a first-party script
 * that depends on the shared layer stack it publishes &mdash; is asserted the same way, over the emitted
 * {@code <script src>} sequence.
 *
 * <p>
 * The {@code b0x} methods below are synthetic: they drive the same band-position/order-checking logic this
 * class uses against a fixed, in-memory string rather than a live server response, proving the guard can
 * actually fail. Without them, every {@code a0x} method passing would tell a reader nothing about whether the
 * check fires or merely never runs (the same reason {@code ChromeScale_ContractScan_Test} in the views module
 * carries its own synthetic negative case).
 */
class AssetLoadOrderBand_Test extends TestBase {

	// The CSS band's five positions (2026-08-28 ruling: extended from four to five).
	private static final int VENDOR = 1;
	private static final int VIEWS_BASE = 2;
	private static final int WIDGET = 3;
	private static final int CONSUMER_THEME = 4;
	private static final int PAGE_LOCAL = 5;

	private static ExampleViewsServer viewsServer;
	private static ExampleCalendarServer calendarServer;
	private static HttpClient http;

	@BeforeAll
	static void startServers() throws Exception {
		viewsServer = ExampleViewsServer.start(0);
		calendarServer = ExampleCalendarServer.start(0);
		http = HttpClient.newHttpClient();
	}

	@AfterAll
	static void stopServers() throws Exception {
		if (viewsServer != null)
			viewsServer.close();
		if (calendarServer != null)
			calendarServer.close();
	}

	private static String bodyOf(URI root, String path) throws Exception {
		var req = HttpRequest.newBuilder(root.resolve(path)).GET().build();
		return http.send(req, BodyHandlers.ofString()).body();
	}

	//------------------------------------------------------------------------------------------------------------------
	// The band-position/order-checking core - a pure function over HTML text, so it can be driven both by a live
	// page response (a0x below) and by a synthetic in-memory fixture that proves it can fail (b0x below).
	//------------------------------------------------------------------------------------------------------------------

	private static final Pattern LINK_HREF = Pattern.compile("<link\\s+rel=\"stylesheet\"\\s+href=\"([^\"]*)\"");
	private static final Pattern SCRIPT_SRC = Pattern.compile("<script\\s+src=\"([^\"]*)\"");
	private static final Pattern STYLE_TAG = Pattern.compile("<style>");

	/** One classified asset found in an emitted page, in document order: its band position and a failure-message label. */
	private record BandHit(int position, String label, int index) {}

	/** Classifies a stylesheet {@code href} onto the five-position band; throws for anything the band doesn't name. */
	private static int cssBandPosition(String href) {
		if (href.contains("dataTables.dataTables"))
			return VENDOR;
		if (href.contains("/juneau-views.css"))
			return VIEWS_BASE;
		if (href.contains("/juneau-calendar.css"))
			return WIDGET;
		if (href.contains("/chrome.css"))
			return CONSUMER_THEME;
		throw new AssertionError("Unclassified stylesheet - the band doesn't name this asset: " + href);
	}

	/** Walks an HTML page's stylesheet links plus its page-local {@code <style>} block (if any), in document order. */
	private static List<BandHit> cssBandSequence(String html) {
		var out = new ArrayList<BandHit>();
		var links = LINK_HREF.matcher(html);
		while (links.find())
			out.add(new BandHit(cssBandPosition(links.group(1)), links.group(1), links.start()));
		var style = STYLE_TAG.matcher(html);
		if (style.find())
			out.add(new BandHit(PAGE_LOCAL, "<style> (page-local)", style.start()));
		out.sort(Comparator.comparingInt(BandHit::index));
		return out;
	}

	/** Walks an HTML page's {@code <script src>} sequence, classifying only the roles named in {@code roleOf}. */
	private static List<BandHit> scriptSequence(String html, Map<String,Integer> roleOf) {
		var out = new ArrayList<BandHit>();
		var scripts = SCRIPT_SRC.matcher(html);
		while (scripts.find()) {
			var src = scripts.group(1);
			for (var e : roleOf.entrySet())
				if (src.contains(e.getKey()))
					out.add(new BandHit(e.getValue(), src, scripts.start()));
		}
		out.sort(Comparator.comparingInt(BandHit::index));
		return out;
	}

	/** Asserts a document-ordered sequence of band hits is non-decreasing, naming the first violating pair on failure. */
	private static void assertBandOrder(List<BandHit> hits, String pageLabel) {
		for (var i = 1; i < hits.size(); i++) {
			var prev = hits.get(i - 1);
			var cur = hits.get(i);
			assertTrue(prev.position() <= cur.position(),
				() -> pageLabel + ": asset order violates the band - \"" + cur.label() + "\" (position "
					+ cur.position() + ") must not load before \"" + prev.label() + "\" (position "
					+ prev.position() + ")");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// a0x - the live pages, over real HTTP, against the four in-tree templates the band governs.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_richViewsIndexPage_cssBandOrder_conforms() throws Exception {
		var body = bodyOf(viewsServer.getRootUrl(), "/");
		assertBandOrder(cssBandSequence(body), "Rich Views index");
	}

	@Test
	void a02_cardDashboardPage_cssBandOrder_conforms() throws Exception {
		var body = bodyOf(viewsServer.getRootUrl(), "/dashboard");
		assertBandOrder(cssBandSequence(body), "Card Dashboard");
	}

	@Test
	void a03_quickStatsOverviewPage_cssBandOrder_conforms() throws Exception {
		var body = bodyOf(viewsServer.getRootUrl(), "/overview");
		assertBandOrder(cssBandSequence(body), "QuickStats overview");
	}

	/** The calendar page is the only in-tree witness to band position 3 (first-party widget stylesheet). */
	@Test
	void a04_calendarIndexPage_cssBandOrder_conforms_andExercisesTheWidgetPosition() throws Exception {
		var body = bodyOf(calendarServer.getRootUrl(), "/");
		var hits = cssBandSequence(body);
		assertBandOrder(hits, "Calendar index");
		assertTrue(hits.stream().anyMatch(h -> h.position() == WIDGET),
			() -> "expected the calendar page to exercise the widget-layer band position: " + hits);
	}

	/** The icon registry must load before the cards script - the refresh button's glyph resolves from the registry. */
	@Test
	void a05_cardDashboardPage_scriptOrder_iconsBeforeCards() throws Exception {
		var body = bodyOf(viewsServer.getRootUrl(), "/dashboard");
		var hits = scriptSequence(body, Map.of("/juneau-icons.js", 1, "/juneau-cards.js", 2));
		assertEquals(2, hits.size(), () -> "expected both the icons and cards scripts to be linked: " + hits);
		assertBandOrder(hits, "Card Dashboard script order");
	}

	/** juneau-views.js must load before juneau-calendar.js - the calendar's popover registers on the shared stack. */
	@Test
	void a06_calendarIndexPage_scriptOrder_viewsBeforeCalendar() throws Exception {
		var body = bodyOf(calendarServer.getRootUrl(), "/");
		var hits = scriptSequence(body, Map.of("/juneau-views.js", 1, "/juneau-calendar.js", 2));
		assertEquals(2, hits.size(), () -> "expected both the views and calendar scripts to be linked: " + hits);
		assertBandOrder(hits, "Calendar script order");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - synthetic, in-memory: proves the guard can actually fail, and that a conforming full band is not flagged.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void b01_syntheticCssBandViolation_isCaught() {
		var reversed = "<link rel=\"stylesheet\" href=\"/juneau-calendar.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-views.css\">";
		var err = assertThrows(AssertionError.class, () -> assertBandOrder(cssBandSequence(reversed), "synthetic"));
		assertTrue(err.getMessage().contains("juneau-views.css"), err.getMessage());
	}

	@Test
	void b02_syntheticScriptBandViolation_isCaught() {
		var reversed = "<script src=\"/juneau-cards.js\"></script><script src=\"/juneau-icons.js\"></script>";
		var hits = scriptSequence(reversed, Map.of("/juneau-icons.js", 1, "/juneau-cards.js", 2));
		var err = assertThrows(AssertionError.class, () -> assertBandOrder(hits, "synthetic"));
		assertTrue(err.getMessage().contains("juneau-icons.js"), err.getMessage());
	}

	@Test
	void b03_conformingSyntheticFullBand_isNotFlagged() {
		var conforming = "<link rel=\"stylesheet\" href=\"https://cdn.example/dataTables.dataTables.min.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-views.css\">"
			+ "<link rel=\"stylesheet\" href=\"/juneau-calendar.css\">"
			+ "<link rel=\"stylesheet\" href=\"/chrome.css\">"
			+ "<style>body{}</style>";
		assertBandOrder(cssBandSequence(conforming), "synthetic conforming");
	}
}
