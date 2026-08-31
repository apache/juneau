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

import java.net.http.*;
import java.net.http.HttpResponse.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Proves the example actually works: boots {@link ExampleViewsServer} in-process on an ephemeral port and
 * drives real HTTP requests at it &mdash; the composed page, each of the three data endpoints, and the
 * first-party JS/CSS assets it links to &mdash; asserting that the sub-tabbed {@code PageDef}, request-resolved
 * asset URLs, and data for every {@code ViewDef} are actually reachable end-to-end, not just compilable.
 */
@SuppressWarnings({
	"resource" // server is opened in @BeforeAll/closed in @AfterAll.
})
class ExampleViewsEndToEnd_Test extends TestBase {

	private static ExampleViewsServer server;
	private static HttpClient http;

	@BeforeAll
	static void startServer() throws Exception {
		server = ExampleViewsServer.start(0);
		http = HttpClient.newHttpClient();
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (server != null)
			server.close();
	}

	private HttpResponse<String> get(String path) throws Exception {
		var uri = server.getRootUrl().resolve(path);
		var req = HttpRequest.newBuilder(uri).GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	/** Like {@link #get(String)}, but with the {@code Accept} header DataTables' own jQuery ajax call sends. */
	private HttpResponse<String> getJson(String path) throws Exception {
		var uri = server.getRootUrl().resolve(path);
		var req = HttpRequest.newBuilder(uri).header("Accept", "application/json").GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	@Test
	void a01_indexPage_rendersComposedPage_withBothTabsAndBothSubtabs() throws Exception {
		var res = get("/");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("data-juneau-page=\"widgets-demo\""), "page marker");
		assertTrue(body.contains("data-tab-id=\"catalog\""), "Catalog tab");
		assertTrue(body.contains("data-tab-id=\"audit\""), "Audit Log tab");
		assertTrue(body.contains("data-tab-id=\"alerts\""), "Alerts tab");
		assertTrue(body.contains("data-subtab-id=\"active\""), "Active sub-tab");
		assertTrue(body.contains("data-subtab-id=\"archived\""), "Archived sub-tab");
		assertTrue(body.contains("data-juneau-row-detail"), "row-detail template");
		assertTrue(body.contains("data-juneau-detail-url=\"/data/alerts/{id}\""), "alerts expand URL");
		assertTrue(body.contains("data-juneau-detail-url=\"/data/widgets/active/{id}\""), "widget expand URL");
		assertTrue(body.contains("data-juneau-action=\"ack\""), "ack ActionRef");
		assertTrue(body.contains("data-juneau-action=\"esc\""), "esc ActionRef");
		assertTrue(body.contains("data-juneau-safe=\"collapse\""), "COLLAPSE");
		assertTrue(body.contains("data-juneau-nested"), "nested-table shell in the alert expander");
		assertTrue(body.contains("data-juneau-nested-scope-param=\"alertId\""), "custom nested scope param");
		assertTrue(body.contains("data-juneau-nested-meta=\"alert-events\""), "nested sidecar keyed by author view id");
	}

	@Test
	void a02_indexPage_assetLinks_areResolvedFetchableUrls_notServletPrefixed() throws Exception {
		// viewAssetUrl(RestRequest, ...) must resolve "servlet:"-prefixed paths against the request's
		// own URI resolver into a real, browser-fetchable URL - a literal, unfetchable "servlet:" prefix leaking
		// into the page would be a defect. (The resolver returns an absolute-*path* URL here
		// since this example mounts at the server root; c01/c02 below prove the resolved link is actually fetchable.)
		var body = get("/").body();
		assertFalse(body.contains("servlet:"), "no unresolved servlet: URIs");
		assertTrue(body.contains("href=\"/juneau-views.css?v="), "resolved views.css URL");
		assertTrue(body.contains("src=\"/juneau-views.js?v="), "resolved views.js URL");
	}

	@Test
	void a03_indexPage_containsDeepLinkToArchivedSubtab() throws Exception {
		var body = get("/").body();
		assertTrue(body.contains("href=\"#widgets-demo/catalog/archived\""), "deep link present");
	}

	@Test
	void b01_activeWidgetsData_returnsNonEmptyRows_shapedForTheViewDefsColumns() throws Exception {
		// Accept: application/json mirrors what DataTables' own jQuery ajax call sends for a CLIENT-mode source.
		var res = getJson("/data/widgets/active");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"name\":\"widget-1\""));
		assertTrue(body.contains("\"status\""));
		assertTrue(body.contains("\"owner\""));
		assertTrue(body.contains("\"notes\""), "notes present in data even though it's not a table column");
	}

	@Test
	void b02_archivedWidgetsData_returnsNonEmptyRows() throws Exception {
		var res = getJson("/data/widgets/archived");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("\"widget-legacy-1\""));
	}

	@Test
	void b03_auditLogData_returnsNonEmptyRows_ofADifferentRowType() throws Exception {
		var res = getJson("/data/audit");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"actor\""));
		assertTrue(body.contains("\"action\""));
	}

	@Test
	void c01_viewsJsAsset_isReachable_throughTheResolvedUrl() throws Exception {
		var res = get("/juneau-views.js");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("initDetailsExpander"), "ships the details expander this example exercises");
	}

	@Test
	void c02_viewsCssAsset_isReachable() throws Exception {
		var res = get("/juneau-views.css");
		assertEquals(200, res.statusCode());
	}

	@Test
	void d01_alertsData_returnsNonEmptyRows() throws Exception {
		var res = getJson("/data/alerts");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"id\":\"ALRT-1\""));
		assertTrue(body.contains("\"severity\""));
		assertTrue(body.contains("\"assignee\""));
	}

	@Test
	void d02_widgetExpandGet_projectsNotesNotOnTheTable() throws Exception {
		var res = getJson("/data/widgets/active/widget-1");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"contractVersion\":\"1\""));
		assertTrue(body.contains("\"notes\""));
		assertTrue(body.contains("\"owner\""));
	}

	@Test
	void d03_alertExpandGet_returnsSectionFields() throws Exception {
		var res = getJson("/data/alerts/ALRT-1");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"contractVersion\":\"1\""));
		assertTrue(body.contains("\"severity\""));
		assertTrue(body.contains("\"summary\""));
		assertTrue(body.contains("\"assignee\""));
	}

	@Test
	void d04_unknownAlert_returns404() throws Exception {
		var res = getJson("/data/alerts/NO-SUCH");
		assertEquals(404, res.statusCode());
	}

	@Test
	void e01_dashboardPage_rendersCardGrid_withStaticAndRefreshableCards() throws Exception {
		var res = get("/dashboard");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("data-juneau-card-grid"), "grid marker");
		assertTrue(body.contains("data-juneau-card-grid-id=\"ops\""), "grid id");
		assertTrue(body.contains("data-juneau-card-id=\"fleet\""), "static card");
		assertTrue(body.contains("data-juneau-card-id=\"live\""), "refreshable card");
		// Only the live card carries the refresh wire; the static card must not.
		assertTrue(body.contains("data-juneau-card-refresh=\"/data/cards/summary\""), "refresh endpoint");
		assertTrue(body.contains("data-juneau-card-poll-ms=\"10000\""), "poll (>= 5s floor)");
		assertTrue(body.contains("data-juneau-card-contract=\"1\""), "per-card contract stamp");
		// The static Fleet Summary card is server-painted so it reads with JavaScript disabled.
		assertTrue(body.contains(">Total widgets<"), "static field label painted server-side");
	}

	@Test
	void e02_dashboardPage_linksIconsThenCardsAssets_noDataTables() throws Exception {
		var body = get("/dashboard").body();
		assertFalse(body.contains("servlet:"), "no unresolved servlet: URIs");
		var icons = body.indexOf("/juneau-icons.js?v=");
		var cards = body.indexOf("/juneau-cards.js?v=");
		assertTrue(icons >= 0, "icons.js linked");
		assertTrue(cards >= 0, "cards.js linked");
		assertTrue(icons < cards, "icons.js must load before cards.js (the refresh glyph comes from the icon registry)");
		assertFalse(body.contains("datatables"), "a card page carries no table, so no DataTables");
	}

	@Test
	void e03_cardsJsAsset_isReachable() throws Exception {
		var res = get("/juneau-cards.js");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("window.JuneauCards"), "ships the card runtime namespace");
	}

	@Test
	void e04_cardsSummaryEnvelope_isCardContract_withLiveAlertCounts() throws Exception {
		var res = getJson("/data/cards/summary");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"contractVersion\":\"1\""), body);
		assertTrue(body.contains("\"open\""), body);
		assertTrue(body.contains("\"acknowledged\""), body);
		assertTrue(body.contains("\"escalated\""), body);
		assertTrue(body.contains("\"asOf\""), body);
	}

	@Test
	void f01_overviewPage_paintsAQuickStatsStripAboveTheTable() throws Exception {
		var res = get("/overview");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("data-juneau-quickstats=\"alert-overview\""), "strip marker");
		assertTrue(body.contains("data-juneau-quickstats-contract=\"1\""), "QuickStats' own contract stamp");
		assertTrue(body.contains("data-juneau-stat=\"total\""), "scalar tile");
		assertTrue(body.contains("data-juneau-stat=\"critical\""), "meter");
		assertTrue(body.contains("data-juneau-stat=\"by-status\""), "segmented breakdown");
		// Server-painted once, so the figures read with JavaScript disabled.
		assertTrue(body.contains(">Total alerts<"), "tile label painted server-side");
		assertTrue(body.contains("jc-stat-fill"), "meter fill painted server-side");
		// The strip leads the wrapper, so it lands above the toolbar row DataTables grows at init.
		assertTrue(body.indexOf("jc-quickstats") < body.indexOf("<table"), "strip precedes the table");
	}

	@Test
	void f02_overviewPage_stripAndPillsUseTheFiveValueToneVocabulary() throws Exception {
		var body = get("/overview").body();
		// At least one of each coloured tone the page declares, and none of the retired v1 pill tone names.
		assertTrue(body.contains("is-info"), "info tone");
		assertTrue(body.contains("is-warning"), "warning tone");
		assertTrue(body.contains("is-error"), "error tone");
		assertFalse(body.contains("is-ok\""), "retired 'ok' tone");
		assertFalse(body.contains("is-exceeds"), "retired 'exceeds' tone");
		assertFalse(body.contains("is-accent"), "badge-palette 'accent' is not a status tone");
	}

	@Test
	void f03_overviewPage_quickStatsAndItsPillsCarryNoActionAffordance() throws Exception {
		var body = get("/overview").body();
		var strip = body.substring(body.indexOf("jc-quickstats"), body.indexOf("<table"));
		assertFalse(strip.contains("role="), "no button semantics on a stat");
		assertFalse(strip.contains("tabindex"), "a stat is not focusable");
		assertFalse(strip.contains("<a "), "a stat is not a link");
		assertFalse(strip.contains("data-juneau-action"), "a stat dispatches nothing");
		// The overview's own pills are display-only too: no action id anywhere in its sidecar.
		assertFalse(body.contains("\"action\""), "no pill action on this page's view");
		// And the sink pill is declared in the row-detail template rather than as a column renderer.
		assertTrue(body.contains("\"id\":\"pill\""), "pill renderer declared");
	}

	@Test
	void d05_ackAlert_mutatesStatus() throws Exception {
		var uri = server.getRootUrl().resolve("/data/alerts/ALRT-2/ack");
		var req = HttpRequest.newBuilder(uri)
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{}"))
			.build();
		var res = http.send(req, BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res.body());
		assertTrue(res.body().contains("\"outcome\":\"success\""), res.body());
		assertTrue(res.body().contains("\"acknowledged\""), res.body());
	}

	@Test
	void d06_ackForm_servesVersionStampedValidatedModal() throws Exception {
		// The present=dialog form GET returns the modal.checked() envelope: the CURRENT contract version on BOTH the
		// modal top-level and the nested form, the typed 6-type inputs (textarea/toggle/select/action), and the nested
		// action button targets the confirm-only "esc" action (modal-over-modal trigger).
		var res = getJson("/data/alerts/ALRT-2/ack-form");
		assertEquals(200, res.statusCode(), res.body());
		var body = res.body();
		// Read from the constants, not a literal: the runtime compares ONE baked-in literal against BOTH stamps, so a
		// half-done bump would make every form-bearing dialog refuse to open.  Asserting both occurrences here is what
		// makes that visible end-to-end.
		assertEquals(ModalDef.CONTRACT_VERSION, FormDef.CONTRACT_VERSION,
			"the modal and form contract versions must move in lockstep");
		var stamp = "\"contractVersion\":\"" + ModalDef.CONTRACT_VERSION + "\"";
		assertTrue(body.contains(stamp), body);
		assertEquals(2, body.split(java.util.regex.Pattern.quote(stamp), -1).length - 1,
			() -> "both the modal top-level and the nested form must carry " + stamp + ": " + body);
		assertTrue(body.contains("\"type\":\"textarea\""), body);
		assertTrue(body.contains("\"type\":\"toggle\""), body);
		assertTrue(body.contains("\"type\":\"select\""), body);
		assertTrue(body.contains("\"type\":\"action\""), body);
		assertTrue(body.contains("\"actionId\":\"esc\""), body);
		assertTrue(body.contains("\"idempotencyKey\""), body);
	}

	@Test
	void d07_ackForm_barSlotHost_ridesTheWireAsAnAdditiveField() throws Exception {
		// The example's ackForm() dialog also declares ModalDef.barSlot - the THIRD named BarSlot host.  Unlike
		// PageDef/RowDetailDef (Java-only, server-rendered), a dialog is a fetched JSON payload, so the bean must
		// actually be present on the wire for juneau-views.js to paint it client-side.  BarSlot carries its OWN
		// independently-frozen contract version (never bumped alongside ModalDef's), so this is a distinct stamp
		// from the modal/form one already asserted in d06.
		var res = getJson("/data/alerts/ALRT-2/ack-form");
		assertEquals(200, res.statusCode(), res.body());
		var body = res.body();
		assertTrue(body.contains("\"barSlot\":{"), body);
		assertTrue(body.contains("\"contractVersion\":\"" + BarSlot.CONTRACT_VERSION + "\""), body);
		assertTrue(body.contains("\"id\":\"ack-form-bar\""), body);
		// ALRT-2 (severities cycle critical/warning/info) is "info" severity, so the widget mirrors that AND the
		// non-critical (WARN, not DANGER) tone - proving the tone/text are actually DERIVED from the alert, not
		// a copy-pasted literal.
		assertTrue(body.contains("\"text\":\"Severity: info\""), body);
		assertTrue(body.contains("\"label\":\"other open at this severity\""), body);
		assertTrue(body.contains("\"tone\":\"WARN\""), body);
	}
}
