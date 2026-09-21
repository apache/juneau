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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.releng.release.Release;
import org.apache.juneau.releng.release.ReleaseListService;
import org.apache.juneau.rest.mock.MockRestClient;
import org.apache.juneau.rest.server.filter.LoopbackBoundary;
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
	 * {@link MockRestClient#create(Object)} caches its {@code RestContext} per resource class, so a second
	 * {@code ReleaseRest} instance built with different test data would silently dispatch against the
	 * <em>first</em> instance ever created for this class in the JVM. Passing a (no-op) {@link StackOverlay}
	 * as the overriding bean store opts out of that cache — see {@code MockRestClient.Builder#overridingBeanStore}.
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
	 * {@code /{version}/{rc}} route, guarding against the exact failure this endpoint originally hit:
	 * the request fell through to {@code RestContext.handleNotFound} with a stray {@code 200} already set
	 * ("Invalid method response: 200") because no correctly declared op returned a renderable View for it.
	 * A direct call to {@link ReleaseRest#detail} alone wouldn't exercise Juneau's own path-matching/dispatch,
	 * so this is the layer that actually proves the route is reachable and renders.
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
	 * The Releases page is now a {@code <@page toolkit="views">} call: the views toolkit pack supplies the runtime
	 * JS in load order (renders → icons → ribbon → views → config → regions → helpers → page-cards), and the table
	 * is a {@code <@card type="datatables" id="releases">} the page-cards runtime mounts from its sidecar — there is
	 * no {@code table-slot.js} and no {@code /view} envelope fetch. Asserts the served page carries {@code juneau-icons.js}
	 * ordered before {@code juneau-ribbon.js} (the ribbon resolves glyphs from the icon registry as it builds its
	 * buttons, so the registry must already exist), the page-cards runtime + sidecar mount, the shared chrome nav with
	 * Releases current, CSRF wiring, the title, and the Detail View populator.
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
				// The datatables card mount + page-cards runtime + sidecar (replaces the retired empty-slot + table-slot.js).
				assertTrue(body.contains("id=\"releases\""), "Missing releases card mount: " + body);
				assertTrue(body.contains("data-juneau-card=\"datatables\""), "Missing datatables card markup: " + body);
				assertTrue(body.contains("data-juneau-card-sidecar=\"releases\""), "Missing page-cards sidecar: " + body);
				assertTrue(body.contains("juneau-page-cards.js"), "Missing page-cards runtime: " + body);
				assertFalse(body.contains("/js/table-slot.js"), "Retired table-slot.js is still wired: " + body);
				assertFalse(body.contains("juneau-view:releases"), "ViewTable sidecar leaked: " + body);
				assertFalse(body.contains("data-juneau-view"), "Marker table leaked: " + body);
				assertTrue(body.contains("juneau-regions.js"), "Missing regions runtime: " + body);
				assertTrue(body.contains("data-juneau-csrf="), body);
				assertTrue(body.contains("data-juneau-csrf-header=\"" + LoopbackBoundary.DEFAULT_CSRF_HEADER + "\""), body);
				// Chrome now authors the primary nav once as an <@navigation> landmark; the Releases node is current.
				assertTrue(body.contains("class=\"juneau-page-nav\""), "Chrome must render the shared page nav: " + body);
				assertTrue(body.contains("aria-current=\"page\""), "Releases nav node must be current: " + body);
				assertTrue(body.contains("class=\"jc-page-header\""), body);
				assertTrue(body.contains("<h1>All Releases</h1>"), body);
				assertTrue(body.contains("Every Apache Juneau release"), body);
				assertFalse(body.contains("rm-card-sub"), body);
				assertTrue(body.contains("/js/releases-detail.js"), body);
			}
		}
	}

	/**
	 * The Excel/PDF export buttons are declared {@code .optional("excel","pdf")} on {@link ReleaseRest} and are
	 * feature-detected off {@code window.JSZip} / {@code window.pdfMake} by {@code juneau-ribbon.js}, so they only
	 * render when JSZip + pdfMake are on the page. DataTables Buttons' HTML5 export reads those globals as
	 * {@code buttons.html5.min.js} initializes, so all three export-dependency scripts must be included and ordered
	 * before it. Asserts the served page carries the JSZip, pdfMake, and pdfMake {@code vfs_fonts} includes, each
	 * ahead of {@code buttons.html5.min.js}.
	 */
	@Test
	void b02_pageIncludesExportDependencyScriptsBeforeButtonsHtml5() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				var jszipIdx = body.indexOf("jszip.min.js");
				var pdfmakeIdx = body.indexOf("pdfmake.min.js");
				var vfsIdx = body.indexOf("vfs_fonts.min.js");
				var buttonsHtml5Idx = body.indexOf("buttons.html5.min.js");
				assertTrue(jszipIdx >= 0, "Missing jszip.min.js script include: " + body);
				assertTrue(pdfmakeIdx >= 0, "Missing pdfmake.min.js script include: " + body);
				assertTrue(vfsIdx >= 0, "Missing vfs_fonts.min.js script include: " + body);
				assertTrue(buttonsHtml5Idx >= 0, "Missing buttons.html5.min.js script include: " + body);
				assertTrue(jszipIdx < buttonsHtml5Idx,
					"jszip.min.js must be included before buttons.html5.min.js: " + body);
				assertTrue(pdfmakeIdx < buttonsHtml5Idx,
					"pdfmake.min.js must be included before buttons.html5.min.js: " + body);
				assertTrue(vfsIdx < buttonsHtml5Idx,
					"vfs_fonts.min.js must be included before buttons.html5.min.js: " + body);
			}
		}
	}

	/**
	 * The {@code /data} endpoint speaks the DataTables server-side-processing contract: given a request carrying
	 * DataTables params it returns a {@code DataTablesResults} envelope ({@code {draw, recordsTotal, recordsFiltered,
	 * data}}) with server-side per-column filtering applied &mdash; not the bare {@code List<Release>} array it used
	 * to return. Wired via {@link ReleaseRest#queryableSettings()} (a {@code DataTablesQueryProtocol}) +
	 * {@code ProtocolQueryable}; this proves the envelope shape and that filtering happens on the server.
	 */
	@Test
	void c01_dataReturnsDataTablesEnvelopeWithServerSideFilterApplied() throws Exception {
		var releases = List.of(release("9.2.1", "RELEASED"), release("9.3.0", "VOTING"));
		try (var client = client(rest(releases))) {
			try (var resp = client.request("GET",
					"/data?draw=3&start=0&length=10&columns[0][data]=status&columns[0][search][value]=RELEASED").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertFalse(body.trim().startsWith("["), "Expected an envelope object, not a bare array: " + body);
				assertTrue(body.contains("recordsTotal"), "Missing recordsTotal: " + body);
				assertTrue(body.contains("recordsFiltered"), "Missing recordsFiltered: " + body);
				assertTrue(body.contains("draw"), "Missing draw: " + body);
				assertTrue(body.contains("9.2.1"), "Filtered-in row missing: " + body);
				assertFalse(body.contains("9.3.0"), "Filtered-out row present: " + body);
				assertTrue(body.contains(release("9.2.1", "RELEASED").rowId()), body);
			}
		}
	}

	/**
	 * The table catalog now rides in the page itself: the {@code <@card type="datatables" id="releases">} escape
	 * hatch lifts its FTL JSON5 body into a page-cards sidecar (VIEW_META with {@code contractVersion} + {@code view}
	 * passed through unchanged) rather than being served from a Java {@code ViewDef} at {@code /view}. Asserts the
	 * served page's sidecar carries the view id + version-cell column, declares the Detail View expand endpoint /
	 * populator (not a version hyperlink), and renders Status/Stage as pills, not tag chips.
	 */
	@Test
	void d01_pageCardSidecarCarriesTheReleasesCatalog() throws Exception {
		try (var client = client(rest(List.of(release("9.2.1", "RELEASED"))))) {
			try (var resp = client.request("GET", "/").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("data-juneau-card-sidecar=\"releases\""), body);
				assertTrue(body.contains("\"contractVersion\":\"4\""), "Missing lifted VIEW_META view contract: " + body);
				assertTrue(body.contains("\"id\":\"releases\""), "Missing view id in the lifted catalog: " + body);
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
