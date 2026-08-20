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
}
