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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * WORK-J0522d, design test <b>53</b> for §11.1a: the ten-tab region-hosted detail example, stood up as a
 * <b>live page</b> and driven over real HTTP &mdash; plus the <b>dual-hat label build gate</b> the design demands be
 * a mechanism rather than an editor note.
 *
 * <p>
 * §11.1a is the affordability proof the region design rests on and the template that downstream migrations get
 * written from, so "it compiles" is explicitly <b>not</b> the property that matters. What matters is that the page
 * serves, that the panel is genuinely a bare region rather than a disguised section panel, that every one of the
 * eleven declared payloads is reachable in the shape the author's loader expects, and that the labels shipping in
 * this Apache tree carry no observed-product vocabulary.
 *
 * <h5 class='section'>What this test does and does not cover, stated plainly</h5>
 * <p>
 * Covered here: the served page, the emitted region container and its absent section chrome, the shared expand
 * envelope, both map-shaped tab payloads, all seven list-shaped ones (including the empty-state arm), and the
 * label gate.
 *
 * <p>
 * <b>Not</b> covered here, and not claimed: the four <i>runtime</i> behaviors of the design's test-53 wording
 * &mdash; that the eager tab paints on expand, that a lazy tab paints on first activation and not before, that a
 * re-activated tab does not refetch, and that collapsing aborts in-flight tabs with no {@code AbortError}
 * surfacing. Those are browser behaviors of {@code tabStrip}/{@code dataPane}, they need a driven DOM, and the
 * mechanisms behind each are unit-covered in the views module's own region and helper suites. Asserting them from
 * here would require a Playwright layer this module does not have; claiming them without one would be worse than
 * leaving the gap visible.
 */
@SuppressWarnings({
	"resource" // server is opened in @BeforeAll / closed in @AfterAll.
})
class ExampleInstancesRegion_Test extends TestBase {

	private static ExampleViewsServer server;
	private static HttpClient http;
	private static String page;

	@BeforeAll
	static void startServer() throws Exception {
		server = ExampleViewsServer.start(0);
		http = HttpClient.newHttpClient();
		page = get("instances/").body();
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (server != null)
			server.close();
	}

	private static HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder(server.getRootUrl().resolve(path)).GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	/**
	 * Like {@link #get(String)} but asking for JSON, which is what the author's {@code fetch(...)} does.
	 *
	 * <p>
	 * Necessary rather than incidental: these resources content-negotiate, and a bare {@code GET} from this client
	 * gets Juneau's HTML rendering of the same payload. Asserting shapes against that HTML would test the wrong
	 * serializer &mdash; and would have quietly passed a {@code startsWith("[")} check for entirely wrong reasons.
	 */
	private static HttpResponse<String> getJson(String path) throws Exception {
		var req = HttpRequest.newBuilder(server.getRootUrl().resolve(path))
			.header("Accept", "application/json")
			.GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The page serves, and the panel is a BARE REGION - which is the whole claim of SD-3's detail arm.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_thePageServes() throws Exception {
		var r = get("instances/");
		assertEquals(200, r.statusCode(), () -> "the 11.1a page did not serve: " + r.body());
		assertTrue(r.body().contains("Ten Tabs, Zero Server Knowledge"), "served something other than the example");
	}

	@Test void a02_theRowDetailTemplateCarriesExactlyOneRegionContainer() {
		var count = page.split("data-juneau-region=\"detail\"", -1).length - 1;
		assertEquals(1, count,
			() -> "the detail template must carry EXACTLY ONE empty region container (SD-3 / design item 10); found "
				+ count);
		assertTrue(page.contains("data-juneau-region-type=\"row-detail\""),
			"the region's ctx.type must be row-detail (SF-G)");
		assertTrue(page.contains("data-juneau-region-contract=\"1\""),
			"RegionDef.CONTRACT_VERSION stays \"1\" and must be stamped on the container");
	}

	@Test void a03_theRegionPanelHasNoSectionChromeAndNoFieldSlots() {
		// This is the assertion that fails if the emitter quietly kept emitting the old shape alongside the new one.
		// A region panel with section frames would render its body TWICE - the exact blank-or-double-render window
		// SF-E's staged emitter table exists to close.
		assertFalse(page.contains("data-juneau-detail-section"),
			"a .region(...) panel must emit NO section frames - no strip is drawn for it and a frame would be a "
				+ "second body");
		assertFalse(page.contains("data-juneau-field="),
			"a .region(...) panel must emit NO field slots - the author's populate paints every value");
	}

	@Test void a04_thePageLoadsTheRegionRuntimeAndHelpersAfterTheViewRuntime() {
		// Load ORDER is load-bearing: regions.register and every h.* name are theirs, and the populate below is
		// registered at parse time.  A page that loads them first silently registers nothing.
		var views = page.indexOf("juneau-views.js");
		var regions = page.indexOf("juneau-regions.js");
		var helpers = page.indexOf("juneau-helpers.js");
		assertTrue(views > 0 && regions > 0 && helpers > 0,
			() -> "expected all three runtimes linked; views=" + views + " regions=" + regions + " helpers=" + helpers);
		assertTrue(regions > views, "juneau-regions.js must load AFTER juneau-views.js");
		assertTrue(helpers > views, "juneau-helpers.js must load AFTER juneau-views.js");
	}

	@Test void a05_theAuthorRegistersExactlyThePopulatorTheViewNames() {
		assertTrue(page.contains("JuneauViews.regions.register(\"" + ExampleInstancesRest.POPULATOR + "\""),
			"the page must register the populator name the RegionDef declares, or the panel paints nothing");
		assertTrue(page.contains("data-juneau-region-populate=\"" + ExampleInstancesRest.POPULATOR + "\""),
			"the emitted container must carry the populator name the author registered");
	}

	@Test void a06_tenTabsAreDeclaredClientSideAndNoneServerSide() {
		// The measurable form of "one line per tab, zero lines of Java".
		for (var id : List.of("details", "core-metrics", "extra-metrics", "suspensions", "directives", "releases",
				"org-requests", "pending-changes", "checks", "audit-trail"))
			assertTrue(page.contains("id: \"" + id + "\""), () -> "missing client-side tab declaration: " + id);
		assertTrue(page.contains("lazy: false"),
			"the Details tab must opt OUT of lazy so it paints eagerly; the other nine inherit lazy=true");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Every one of the eleven payloads is reachable, in the shape the author's loader expects (§8.5).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_theSharedExpandEnvelopeServesAndIsContractStamped() throws Exception {
		var r = getJson("instances/data/instances/INST-1");
		assertEquals(200, r.statusCode(), r::body);
		assertTrue(r.body().contains("contractVersion"),
			"the expand GET is the SHARED envelope the eager tab joins, so it must be contract-stamped");
		assertTrue(r.body().contains("\"fields\""),
			"the envelope must carry its values map under the \"fields\" key - the SAME key the client region "
				+ "readers unwrap (juneau-regions.js's resolveDeclaredEnvelope / juneau-views.js's toValuesMap "
				+ "both read body.fields, never body.values)");
		assertTrue(r.body().contains("dbVendor"),
			"the Details tab reads expand-GET-only fields; if they are absent the eager pane renders blanks");
	}

	@Test void b02_bothMapShapedTabPayloadsServeAsBareValuesMaps() throws Exception {
		for (var kind : List.of("core", "extra")) {
			var r = getJson("instances/data/instances/INST-1/metrics/" + kind);
			assertEquals(200, r.statusCode(), r::body);
			assertTrue(r.body().startsWith("{"),
				() -> "a map pane's loader resolves a BARE values map - no envelope, no items wrapper: " + r.body());
			assertFalse(r.body().contains("contractVersion"),
				() -> "a tab payload is not an envelope and must not be stamped like one: " + r.body());
		}
	}

	@Test void b03_allSevenListShapedTabPayloadsServeAsArrays() throws Exception {
		for (var tab : List.of("suspensions", "directives", "releases", "org-requests", "pending-changes", "checks",
				"audit-trail")) {
			var r = getJson("instances/data/instances/INST-1/" + tab);
			assertEquals(200, r.statusCode(), () -> tab + " -> " + r.body());
			assertTrue(r.body().startsWith("["),
				() -> "a list pane's loader resolves an ARRAY of values maps: " + tab + " -> " + r.body());
		}
	}

	@Test void b04_theEmptyStateArmIsReachedForReal() throws Exception {
		// dataPane's empty state is a real arm of the design's shape table, and an example that never returns empty
		// leaves it untested on the page that is supposed to demonstrate it.  INST-1 is healthy -> no suspensions.
		var r = getJson("instances/data/instances/INST-1/suspensions");
		assertEquals(200, r.statusCode(), r::body);
		assertEquals("[]", r.body().replace(" ", ""),
			() -> "expected the empty-state arm for a healthy instance, got: " + r.body());
	}

	@Test void b05_anUnknownTabIs404_whichTheAuthorsLoaderMapsToEmptyNotError() throws Exception {
		// The 404 arm of the author's loader is load-bearing rather than defensive (it resolves null -> empty pane,
		// where the obvious r.ok check would paint the ERROR pane).  That arm is only exercised if 404 is real.
		assertEquals(404, getJson("instances/data/instances/INST-1/no-such-tab").statusCode());
	}

	@Test void b06_theTableRowsServe() throws Exception {
		var r = getJson("instances/data/instances");
		assertEquals(200, r.statusCode(), r::body);
		assertTrue(r.body().contains("INST-1"), r::body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) THE DUAL-HAT BUILD GATE.  A note is exactly what a copy-paste defeats, so this is a mechanism.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Vocabulary that must never appear in this Apache-tree example: observed-product acronyms and label spellings,
	 * Salesforce marks, and SLDS/lightning design-system tokens.
	 */
	private static final List<String> DENIED = List.of(
		"uip", "slds-", "salesforce", "lightning", "sfdc", "einstein", "hyperforce",
		"salesforce sans", "trailhead", "chatter", "sandbox support console");

	private static Path exampleSource() {
		for (var rel : List.of(
				"src/main/java/org/apache/juneau/examples/views/ExampleInstancesRest.java",
				"juneau-examples/juneau-examples-views/src/main/java/org/apache/juneau/examples/views/ExampleInstancesRest.java")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p;
		}
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/main/java/org/apache/juneau/examples/views/ExampleInstancesRest.java");
			if (Files.isRegularFile(p))
				return p;
		}
		return null;
	}

	@Test void c01_theExampleSourceCarriesNoObservedProductVocabulary() throws Exception {
		var src = exampleSource();
		assertNotNull(src, "could not locate the example source; the dual-hat gate would scan nothing and 'pass'");
		var lower = Files.readString(src, UTF_8).toLowerCase(Locale.ROOT);
		var hits = DENIED.stream().filter(lower::contains).toList();
		assertTrue(hits.isEmpty(),
			() -> "this example ships in apache/juneau, so its vocabulary is an Apache-tree string.  Denied terms "
				+ "found in the source: " + hits + ".  Genericize them - the transferable part of 11.1a is the "
				+ "SHAPE (one region, one strip call, two pane recipes), never the vocabulary.");
	}

	@Test void c02_theServedLabelsCarryNoObservedProductVocabulary() {
		// The source gate above and this one are not redundant: the served page is what a reader copies, and a label
		// could arrive from a constant defined elsewhere.
		var lower = page.toLowerCase(Locale.ROOT);
		var hits = DENIED.stream().filter(lower::contains).toList();
		assertTrue(hits.isEmpty(), () -> "denied vocabulary reached the SERVED page: " + hits);
	}

	@Test void c03_theGateIsNotVacuous() throws Exception {
		// Every assertion above is satisfied most easily by a scan that reads nothing.  Prove the scan reads the real
		// file, and that the denylist would actually fire on it.
		var src = exampleSource();
		assertNotNull(src);
		var text = Files.readString(src, UTF_8);
		assertTrue(text.contains("Extra Metrics"),
			"expected the genericized label the design mandates in place of the observed acronym");
		assertTrue(DENIED.stream().anyMatch(d -> ("x " + text + " uip").toLowerCase(Locale.ROOT).contains(d)),
			"the denylist must be capable of matching at all");
		assertTrue(DENIED.size() >= 8, "the denylist was emptied out; it is the gate");
	}

	@Test void c04_theTenLabelsAreTheGenericSet() {
		var labels = Stream.of("Details", "Core Metrics", "Extra Metrics", "Suspensions", "Directives", "Releases",
			"Org Requests", "Pending Changes", "Checks", "Audit Trail").toList();
		for (var l : labels)
			assertTrue(page.contains("label: \"" + l + "\""), () -> "missing generic label: " + l);
	}
}
