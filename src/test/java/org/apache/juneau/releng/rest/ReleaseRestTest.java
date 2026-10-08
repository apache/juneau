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

package org.apache.juneau.releng.rest;

import static org.apache.juneau.rest.server.console.test.PageContractAssert.assertPage;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.entity.StringBody;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.datatables.DataTablesRequest;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class ReleaseRestTest {

	private ReleaseRest rest(List<Release> releases) {
		return new ReleaseRest(new ReleaseListService(List::of, List::of, () -> releases));
	}

	private static Release release(String version, String status) {
		return new Release(version, status, "state");
	}

	/**
	 * Builds a {@link MockRestClient} wired to a fresh {@code ReleaseRest}, using a no-op {@link StackOverlay}
	 * as the overriding bean store so it does not reuse a cached {@code RestContext} from another test.
	 */
	@SuppressWarnings({
		"resource" // Caller owns and closes the returned MockRestClient (via try-with-resources); Eclipse JDT @Owning warning is by design.
	})
	private static MockRestClient client(ReleaseRest rest) {
		return MockRestClient.builder(rest).overridingBeanStore(new StackOverlay()).build();
	}

	/**
	 * A request with no loopback-boundary token attribute, which is what a direct call (no servlet filter in the
	 * path) sees. {@code ConsolePage} renders the token empty in that case rather than failing.
	 */
	private static HttpServletRequest req() {
		return mock(HttpServletRequest.class);
	}

	@Test
	void a01_detailReturnsAViewCarryingTheMatchingRelease() {
		var rest = rest(List.of(release("9.2.1", "RELEASED")));
		var view = rest.detail("9.2.1", "1", req());
		assertNotNull(view);
	}

	@Test
	void a02_detailForAnUnknownVersionIs404() {
		var rest = rest(List.of(release("9.2.1", "RELEASED")));
		var httpReq = req();
		var ex = assertThrows(NotFound.class, () -> rest.detail("9.9.9", "1", httpReq));
		assertEquals(404, ex.getStatusCode());
	}

	/**
	 * Real HTTP dispatch (via {@code juneau-rest-mock}, in-process, no socket) through the two-segment
	 * {@code /{version}/{rc}} route; a direct call to {@link ReleaseRest#detail} alone wouldn't exercise
	 * Juneau's own path-matching/dispatch.
	 */
	@Test
	void a03_detailRendersOverRealHttpDispatch() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/9.2.1/1").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("9.2.1"), "Expected the release version in the rendered page: " + body);
			}
		}
	}

	@Test
	void a04_detailForAnUnknownVersionIs404OverRealHttpDispatch() throws Exception {
		try (var client = client(rest(List.of()))) {
			try (var resp = client.request("GET", "/9.9.9/1").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}

	/**
	 * Asserts the served Releases page carries {@code juneau-icons.js} ordered before {@code juneau-ribbon.js}
	 * (the ribbon resolves glyphs from the icon registry as it builds its buttons, so the registry must
	 * already exist), the releases datatables card in the {@code #juneau-page} contract (mounted client-side by
	 * {@code juneau-console.js}, not server-rendered), the shared chrome nav with Releases current, the title, and
	 * the Detail View populator.
	 */
	@Test
	void b01_pageIncludesIconsJsScriptBeforeRibbonJs() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("juneau-icons.js"), "Missing juneau-icons.js script include: " + body);
				var iconsIdx = body.indexOf("juneau-icons.js");
				var ribbonIdx = body.indexOf("juneau-ribbon.js");
				assertTrue(ribbonIdx >= 0, "Missing juneau-ribbon.js script include: " + body);
				assertTrue(iconsIdx < ribbonIdx,
					"juneau-icons.js must be included before juneau-ribbon.js: " + body);
				// The datatables card is in the page contract; juneau-console.js mounts it.
				assertPage(body).isValid().hasCard("releases", "datatables");
				assertFalse(body.contains("juneau-page-cards.js"), "page-cards runtime was removed in C1: " + body);
				assertTrue(body.contains("/juneau-console/juneau-console.js"), "Missing console shell: " + body);
				assertFalse(body.contains("/js/table-slot.js"), "Retired table-slot.js is still wired: " + body);
				assertFalse(body.contains("juneau-view:releases"), "ViewTable sidecar leaked: " + body);
				assertFalse(body.contains("data-juneau-view"), "Marker table leaked: " + body);
				assertTrue(body.contains("juneau-regions.js"), "Missing regions runtime: " + body);
				// CSRF is now Juneau's <@console> contract: the shell emits the csrf-token meta and the
				// data-juneau-csrf body attributes only when the LoopbackBoundaryFilter published a token.
				// MockRestClient dispatches straight at the servlet with no boundary filter, so no token is
				// published and no CSRF markup is emitted here.
				assertFalse(body.contains("data-juneau-csrf="),
					"MockRestClient runs without the boundary filter, so <@console> must emit no CSRF attrs: " + body);
				// Chrome now authors the primary nav once in the #juneau-page contract; the Releases node is current.
				assertPage(body).hasActiveNav("releases");
				assertTrue(body.contains("class=\"jc-page-header\""), body);
				assertTrue(body.contains("<h1>All Releases</h1>"), body);
				assertTrue(body.contains("Every Apache Juneau release"), body);
				assertFalse(body.contains("rm-card-sub"), body);
				assertTrue(body.contains("/js/releases-detail.js"), body);
			}
		}
	}

	/**
	 * The page gets jQuery, DataTables and Buttons from the asset packs its card declares (served as WebJars),
	 * each exactly once and in dependency order, with nothing bundled or fetched from a CDN. Excel/PDF export
	 * still loads JSZip and pdfMake through {@code init=}; Buttons reads them lazily, when an export runs.
	 */
	@Test
	void b02_pageLoadsPackAssetsOnceInDependencyOrder() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				var jqueryIdx = body.indexOf("jquery.min.js");
				var dataTablesIdx = body.indexOf("dataTables.min.js");
				var buttonsIdx = body.indexOf("dataTables.buttons.min.js");
				var buttonsHtml5Idx = body.indexOf("buttons.html5.min.js");
				assertTrue(jqueryIdx >= 0, "Missing jquery.min.js script include: " + body);
				assertTrue(dataTablesIdx >= 0, "Missing dataTables.min.js script include: " + body);
				assertTrue(buttonsIdx >= 0, "Missing dataTables.buttons.min.js script include: " + body);
				assertTrue(buttonsHtml5Idx >= 0, "Missing buttons.html5.min.js script include: " + body);
				assertTrue(jqueryIdx < dataTablesIdx, "jQuery must precede DataTables: " + body);
				assertTrue(dataTablesIdx < buttonsIdx, "DataTables must precede Buttons: " + body);
				assertEquals(jqueryIdx, body.lastIndexOf("jquery.min.js"), "jQuery loaded more than once: " + body);
				assertEquals(dataTablesIdx, body.lastIndexOf("dataTables.min.js"), "DataTables loaded more than once: " + body);
				assertTrue(body.contains("/webjars/"), "Pack assets should be served as WebJars: " + body);
				assertFalse(body.contains("cdn.datatables.net"), "Buttons must not come from the CDN: " + body);
				assertFalse(body.contains("/datatables/jquery.min.js"), "Bundled jQuery should be gone: " + body);
				var jszipIdx = body.indexOf("jszip.min.js");
				var pdfmakeIdx = body.indexOf("pdfmake.min.js");
				var vfsIdx = body.indexOf("vfs_fonts.js");
				assertTrue(jszipIdx >= 0, "Missing jszip.min.js script include: " + body);
				assertTrue(pdfmakeIdx >= 0, "Missing pdfmake.min.js script include: " + body);
				assertTrue(vfsIdx >= 0, "Missing vfs_fonts.js script include: " + body);
				assertTrue(buttonsHtml5Idx < jszipIdx, "Buttons must precede JSZip: " + body);
				assertTrue(jszipIdx < pdfmakeIdx && pdfmakeIdx < vfsIdx, "Export libraries out of order: " + body);
				assertEquals(jszipIdx, body.lastIndexOf("jszip.min.js"), "JSZip loaded more than once: " + body);
				assertEquals(pdfmakeIdx, body.lastIndexOf("pdfmake.min.js"), "pdfmake loaded more than once: " + body);
				assertEquals(vfsIdx, body.lastIndexOf("vfs_fonts.js"), "vfs_fonts loaded more than once: " + body);
				assertTrue(body.contains("/webjars/jszip/3.10.1/dist/jszip.min.js"), "JSZip should be served as a WebJar: " + body);
				assertTrue(body.contains("/webjars/pdfmake/0.2.7/build/pdfmake.min.js"), "pdfmake should be served as a WebJar: " + body);
				assertFalse(body.contains("cdnjs"), "No cdnjs URL should remain: " + body);
			}
		}
	}

	/**
	 * The {@code /data} endpoint speaks the DataTables server-side-processing contract: given a POSTed
	 * {@code DataTablesRequest} JSON body it returns a {@code DataTablesResults} envelope ({@code {draw, recordsTotal,
	 * recordsFiltered, data}}) with server-side per-column filtering applied. Wired via
	 * {@link ReleaseRest#data(DataTablesRequest)}.
	 */
	@Test
	void c01_dataReturnsDataTablesEnvelopeWithServerSideFilterApplied() throws Exception {
		var releases = List.of(release("9.2.1", "RELEASED"), release("9.3.0", "VOTING"));
		try (var client = client(rest(releases))) {
			var request = "{\"draw\":3,\"start\":0,\"length\":10,\"columns\":[{\"data\":\"status\",\"searchable\":true,\"orderable\":true,\"search\":{\"value\":\"RELEASED\"}}]}";
			try (var resp = client.request("POST", "/data").header("Accept", "application/json").body(StringBody.of(request, "application/json")).run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				var envelope = Json.to(body, Map.class);
				assertBean(envelope, "draw,recordsTotal,recordsFiltered", "3,2,1");
				assertList(versions(envelope), "9.2.1");
				assertTrue(body.contains(release("9.2.1", "RELEASED").rowId()), body);
			}
		}
	}

	private static List<String> versions(Map<?,?> envelope) {
		return ((List<?>)envelope.get("data")).stream().map(r -> String.valueOf(((Map<?,?>)r).get("version"))).toList();
	}

	/**
	 * Ordering by a VERSION column is semantic ({@code 9.10.0} after {@code 9.3.0}), not lexicographic.
	 */
	@Test
	void c02_dataOrdersVersionColumnSemantically() throws Exception {
		var releases = List.of(release("9.2.1", "RELEASED"), release("9.10.0", "VOTING"), release("9.3.0", "RELEASED"));
		try (var client = client(rest(releases))) {
			var request = "{\"draw\":1,\"start\":0,\"length\":10,\"columns\":[{\"data\":\"version\",\"searchable\":true,\"orderable\":true}],\"order\":[{\"column\":0,\"dir\":\"desc\"}]}";
			try (var resp = client.request("POST", "/data").header("Accept", "application/json").body(StringBody.of(request, "application/json")).run()) {
				assertEquals(200, resp.getStatusCode());
				var envelope = Json.to(resp.getBodyAsString(), Map.class);
				assertList(versions(envelope), "9.10.0", "9.3.0", "9.2.1");
			}
		}
	}

	/**
	 * The DataTables global search narrows the returned rows.
	 */
	@Test
	void c03_dataGlobalSearchNarrowsRows() throws Exception {
		var releases = List.of(release("9.2.1", "RELEASED"), release("9.3.0", "VOTING"));
		try (var client = client(rest(releases))) {
			var request = "{\"draw\":1,\"start\":0,\"length\":10,\"search\":{\"value\":\"VOTING\"},\"columns\":[{\"data\":\"status\",\"searchable\":true,\"orderable\":true}]}";
			try (var resp = client.request("POST", "/data").header("Accept", "application/json").body(StringBody.of(request, "application/json")).run()) {
				assertEquals(200, resp.getStatusCode());
				var envelope = Json.to(resp.getBodyAsString(), Map.class);
				assertBean(envelope, "recordsTotal,recordsFiltered", "2,1");
				assertList(versions(envelope), "9.3.0");
			}
		}
	}

	/**
	 * Asserts the served page's {@code <@card type="datatables" id="releases">} sidecar carries the view id +
	 * version-cell column, declares the Detail View expand endpoint/populator (not a version hyperlink), and
	 * renders Status/Stage as pills, not tag chips.
	 */
	@Test
	void d01_releasesCardCarriesTheCatalogInTheContract() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				var table = assertPage(body).isValid().hasCard("releases", "datatables").card("releases").getMap("table");
				assertNull(table.get("contractVersion"), "Catalog-form cards carry no hand-written contract version: " + table);
				assertNull(table.get("view"), "Catalog-form cards carry no pre-built view: " + table);
				assertNull(table.get("layout"), "Catalog-form cards carry no layout: " + table);
				assertNull(table.getMap("detail").get("contractVersion"), "The console stamps detail's contract version: " + table);
				assertEquals("/rest/releases/data", table.getString("dataUrl"), body);
				assertTrue(body.contains("version-cell"), body);
				assertTrue(body.contains("/rest/releases/expand/{id}"), body);
				assertTrue(body.contains("releases-detail"), body);
				assertFalse(body.contains("/rest/releases/{version}/1"), "Version cell must not be a hyperlink: " + body);
				assertTrue(body.contains("\"id\":\"pill\""), "Status/Stage must render as pills: " + body);
				assertFalse(body.contains("\"id\":\"tag\""), "tag renderer must be migrated to pill: " + body);
			}
		}
	}

	@Test
	void d02_viewEndpointIsGone() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/view").header("Accept", "application/json").run()) {
				assertEquals(404, resp.getStatusCode(), "The Java /view slot envelope must be retired (catalog is now FTL).");
			}
		}
	}

	@Test
	void e01_expandReturnsJiraVersionAndKnownLinks() throws Exception {
		var row = release("9.2.1", "RELEASED");
		row.githubReleaseUrl = "https://github.com/apache/juneau/releases/tag/juneau-9.2.1";
		try (var client = client(rest(List.of(row)))) {
			try (var resp = client.request("GET", "/expand/" + URLEncoder.encode(row.rowId(), StandardCharsets.UTF_8))
					.header("Accept", "application/json").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("contractVersion"), body);
				assertTrue(body.contains("jiraVersionUrl"), body);
				assertTrue(body.contains("issues.apache.org/jira"), body);
				assertTrue(body.contains("fixVersion"), body);
				assertTrue(body.contains("9.2.1"), body);
				assertTrue(body.contains("githubReleaseUrl"), body);
				assertTrue(body.contains(Release.DIST_RELEASE_PREFIX + "9.2.1/"), body);
				assertTrue(body.contains(Release.RELEASE_NOTES_URL), body);
			}
		}
	}

	@Test
	void e02_expandUnknownIs404() throws Exception {
		try (var client = client(rest(List.of()))) {
			try (var resp = client.request("GET", "/expand/nope").header("Accept", "application/json").run()) {
				assertEquals(404, resp.getStatusCode());
			}
		}
	}
}
