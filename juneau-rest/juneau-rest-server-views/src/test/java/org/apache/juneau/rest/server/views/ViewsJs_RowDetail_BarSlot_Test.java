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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the detail-hosted {@link org.apache.juneau.rest.server.widgets.BarSlot} runtime: the
 * relocate step that survives the client-built ribbon, clone-time id minting, enhance-on-insert through
 * {@code JuneauChrome.init.initAll()}, demand-only refresh, and collapse teardown.
 *
 * <p>
 * Source-shape always runs; the behavioral {@code detail-bar-slot.cjs} harness runs when {@code node} is on
 * {@code PATH} (skipped otherwise &mdash; no {@code -Pjs-tests} required).  That harness loads
 * {@code juneau-views.js} <b>and</b> {@code juneau-chrome.js} into one sandbox, because this slice is exactly the
 * seam between them.
 */
class ViewsJs_RowDetail_BarSlot_Test extends TestBase {

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		return resource(ViewsMixin.VIEWS_JS_RESOURCE);
	}

	private static String chromeJs() throws IOException {
		return resource(ViewsMixin.CHROME_JS_RESOURCE);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = viewsJs();
		for (var name : new String[]{
			"relocateDetailBarSlot: relocateDetailBarSlot",
			"mintDetailBarSlotIdentity: mintDetailBarSlotIdentity",
			"teardownDetailBarSlot: teardownDetailBarSlot",
			"enhanceChromeInPanel: enhanceChromeInPanel"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
	}

	@Test void a02_relocateLivesInTheDetailCallerNotTheGenericBuilder() throws Exception {
		var body = viewsJs();
		// buildDetailStrip calls the relocate helper; the helper is a PEER function, so slice `w` can lift a generic
		// strip builder out of buildDetailStrip without inheriting the detail-only bar-slot step.
		assertTrue(body.contains("relocateDetailBarSlot(panel, strip)"), body);
		assertTrue(body.contains("function relocateDetailBarSlot("), body);
		var builderAt = body.indexOf("function buildDetailStrip(");
		var relocateDefAt = body.indexOf("function relocateDetailBarSlot(");
		assertTrue(relocateDefAt >= 0 && builderAt >= 0, body);
		assertTrue(relocateDefAt < builderAt, "relocateDetailBarSlot must be its own function, defined outside buildDetailStrip");
	}

	@Test void a03_parentIdIsTheMintedTableIdNotTheAuthorViewId() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("mintDetailBarSlotIdentity(panel, viewSidecarKey(table), rowId)"), body);
	}

	@Test void a04_enhanceOnInsertGoesThroughTheExportedChromeEntry() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("window.JuneauChrome"), body);
		assertTrue(body.contains("initAll()"), body);
	}

	@Test void a05_noPollerIsIntroducedByEitherRuntime() throws Exception {
		assertFalse(chromeJs().contains("setInterval"), "juneau-chrome.js must stay poller-free");
		assertTrue(chromeJs().contains("There is NO poller"), "the poller-free note must survive");
	}

	@Test void a06_chromeSharesOneWiredMarkerForSafeActions() throws Exception {
		var body = chromeJs();
		assertTrue(body.contains("SAFE_WIRED_ATTR = \"data-juneau-safe-wired\""), body);
		assertTrue(body.contains("getAttribute(SAFE_WIRED_ATTR) === \"1\""), body);
		assertTrue(body.contains("setAttribute(SAFE_WIRED_ATTR, \"1\")"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral harness
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var chromeFile = Files.createTempFile("juneau-chrome-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(chromeFile, chromeJs(), UTF_8);
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, viewsFile, chromeFile, rendersFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(chromeFile);
			Files.deleteIfExists(rendersFile);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/detail-bar-slot.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/detail-bar-slot.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/detail-bar-slot.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path chromeJs, Path rendersJs) throws Exception {
		var stdout = Files.createTempFile("detail-bar-slot-stdout-", ".json");
		var stderr = Files.createTempFile("detail-bar-slot-stderr-", ".txt");
		try {
			var cmd = List.of("node", harness.toString(), viewsJs.toString(), chromeJs.toString(), rendersJs.toString());
			var p = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("detail-bar-slot.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("detail-bar-slot.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or detail-bar-slot.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_bothRuntimesLoadedAndHelpersPresent() {
		var r = report();
		assertEquals(true, r.get("hasViews"), r::toString);
		assertEquals(true, r.get("hasChrome"), r::toString);
		assertEquals(true, r.get("hasRelocate"), r::toString);
		assertEquals(true, r.get("hasMint"), r::toString);
		assertEquals(true, r.get("hasTeardown"), r::toString);
		assertEquals(true, r.get("hasEnhance"), r::toString);
	}

	@Test void b02_twoSections_relocatedToTheRibbonsTrailingPosition() {
		var r = report();
		assertEquals(true, r.get("two_regionStartsLast"), r::toString);
		assertEquals(true, r.get("two_stripBuilt"), r::toString);
		assertEquals("tab", r.get("two_stripMode"), r::toString);
		assertEquals(true, r.get("two_regionTrailsStrip"), r::toString);
		assertNum(1, r.get("two_regionCount"));
		assertEquals(true, r.get("two_sameNode"), () -> "the region must be MOVED, not re-created: " + r);
		assertEquals(true, r.get("two_regionStillInPanel"), r::toString);
		assertNum(((Number)r.get("two_stripIndex")).longValue() + 1, r.get("two_regionIndex"));
		assertEquals("1", r.get("two_stripTrailedMarker"),
			() -> "the ribbon must record that a slot trails it, so CSS can share the line: " + r);
	}

	@Test void b03_relocateIsIdempotent_noDuplicateNoOrphan() {
		var r = report();
		assertEquals(false, r.get("two_secondRelocateMoved"), () -> "a second relocate must be a no-op: " + r);
		assertNum(1, r.get("two_regionCountAfterSecond"));
		assertEquals(r.get("two_regionIndex"), r.get("two_regionIndexAfterSecond"), r::toString);
		assertEquals(true, r.get("two_regionTrailsStripAfterSecond"), r::toString);
	}

	@Test void b04_relocateIsIdempotentAcrossARerender() {
		var r = report();
		assertNum(1, r.get("two_rerenderRegionCount"));
		assertEquals(true, r.get("two_rerenderTrailsStrip"), r::toString);
		assertNum(1, r.get("two_firstPanelRegionCount"));
		assertEquals(true, r.get("two_firstPanelStillTrails"), () -> "a re-render must not disturb the live panel: " + r);
	}

	@Test void b05_headerlessPanel_regionStillTrailsThePrependedStrip() {
		var r = report();
		assertEquals(true, r.get("twoNoHeader_stripIsFirst"), r::toString);
		assertEquals(true, r.get("twoNoHeader_regionTrailsStrip"), r::toString);
	}

	@Test void b06_noSlotDeclared_relocateIsACleanNoOp() {
		var r = report();
		assertEquals(true, r.get("twoNoSlot_stripBuilt"), r::toString);
		assertNum(0, r.get("twoNoSlot_regionCount"));
		assertEquals(false, r.get("twoNoSlot_relocateMoved"), r::toString);
		assertEquals(true, r.get("twoNoSlot_stripUnmarked"),
			() -> "a ribbon with no trailing slot must keep its full-width track: " + r);
	}

	@Test void b07_oneSection_noRibbonSynthesized_regionStaysAtItsAnchor() {
		var r = report();
		assertEquals(true, r.get("one_stripIsNull"), r::toString);
		assertEquals(true, r.get("one_noRibbonSynthesized"), () -> "a 1-section detail must not grow a ribbon: " + r);
		assertEquals(true, r.get("one_noTablist"), r::toString);
		assertNum(1, r.get("one_regionCount"));
		assertEquals("section-title", r.get("one_regionAnchor"), r::toString);
		assertEquals(true, r.get("one_regionParentIsSection"), r::toString);
		assertEquals(true, r.get("one_regionFollowsTitle"), r::toString);
		assertEquals(true, r.get("one_anchorSelectorResolves"), r::toString);
		assertEquals(false, r.get("one_relocateMoved"), r::toString);
		assertEquals(true, r.get("one_regionStillFollowsTitle"), r::toString);
	}

	@Test void b08_cloneTimeIdMinting_markerIsSuffixOnly_sidecarCarriesThePrefix() {
		var r = report();
		assertEquals("juneau-view-alerts-1:a1", r.get("mint_markerA"), r::toString);
		assertEquals("juneau-view-alerts-1:b2", r.get("mint_markerB"), r::toString);
		assertEquals("juneau-bar:juneau-view-alerts-1:a1", r.get("mint_sidecarIdA"), r::toString);
		assertEquals("juneau-bar:juneau-view-alerts-1:b2", r.get("mint_sidecarIdB"), r::toString);
		assertEquals(true, r.get("mint_markerHasNoPrefix"), () -> "the marker must NOT carry 'juneau-bar:': " + r);
		assertEquals(true, r.get("mint_sidecarIdHasPrefix"), r::toString);
		assertEquals(true, r.get("mint_sidecarIdIsPrefixPlusMarker"), r::toString);
		assertEquals(true, r.get("mint_markerUsesMintedParentId"), r::toString);
		assertEquals(true, r.get("mint_distinct"), () -> "two expanded rows must never share an id: " + r);
	}

	@Test void b09_eachMintedIdRoundTripsThroughReadSidecar_authorIdDoesNot() {
		var r = report();
		assertEquals(true, r.get("mint_roundTripA"), r::toString);
		assertEquals(true, r.get("mint_roundTripB"), r::toString);
		assertEquals(true, r.get("mint_authorIdUnresolvable"),
			() -> "getElementById('juneau-bar:' + authorId) must not resolve after clone: " + r);
		assertEquals(true, r.get("mint_authorSidecarUnresolvable"), r::toString);
	}

	@Test void b10_clonedSlotsAreEnhancedByInitAll() {
		var r = report();
		assertNum(2, r.get("enh_barsFound"));
		assertNum(1, r.get("enh_headersFound"));
		assertEquals("0", r.get("enh_countBeforeA"), r::toString);
		assertEquals("3", r.get("enh_countAfterA"), r::toString);
		assertEquals("7", r.get("enh_countAfterB"), r::toString);
	}

	@Test void b11_initAllIsIdempotent_noDoubleBoundSafeClick() {
		var r = report();
		assertNum(2, r.get("enh_barsFoundSecond"));
		assertNum(1, r.get("enh_safeListenersAfterFirst"));
		assertNum(1, r.get("enh_safeListenersAfterSecond"));
		assertEquals("1", r.get("enh_wiredMarker"), () -> "the shared wired marker must be stamped: " + r);
		assertEquals("3", r.get("enh_countStillA"), r::toString);
		assertNum(1, r.get("enh_safeFires"));
	}

	@Test void b12_viewsSeamEnhancesAFreshlyInsertedClone() {
		var r = report();
		assertEquals("0", r.get("enh_seamBefore"), r::toString);
		assertEquals(true, r.get("enh_seamCalled"), r::toString);
		assertEquals("5", r.get("enh_seamAfter"), r::toString);
		assertNum(1, r.get("enh_seamSafeListeners"));
		assertEquals(false, r.get("enh_seamSkippedWithoutSlot"), r::toString);
	}

	@Test void b13_demandRefreshFetchesExactlyOnce_andNoIntervalTimerExists() {
		var r = report();
		assertEquals(true, r.get("refresh_result"), r::toString);
		assertNum(1, r.get("refresh_fetchCalls"));
		assertEquals("/chrome/bar-counts", r.get("refresh_url"), r::toString);
		assertEquals("11", r.get("refresh_countAfter"), r::toString);
		assertEquals("7", r.get("refresh_otherRowUntouched"), r::toString);
		assertNum(0, r.get("intervalCalls"));
	}

	@Test void b14_collapseTeardownDetachesCleanly_withoutTouchingTheSiblingRow() {
		var r = report();
		assertEquals(true, r.get("tear_sidecarIdRemoved"), r::toString);
		assertEquals(true, r.get("tear_aUnresolvable"), r::toString);
		assertEquals(true, r.get("tear_bStillResolvable"), r::toString);
		assertEquals(true, r.get("tear_bStillRoundTrips"), r::toString);
		assertNum(1, r.get("tear_bRegionStillPresent"));
		assertEquals(true, r.get("tear_noThrowOnNull"), r::toString);
	}

	@Test void b15_contractHandshakesUnchanged() {
		var r = report();
		assertEquals("1", r.get("rowDetailContract"), r::toString);
		assertEquals(RowDetailDef.CONTRACT_VERSION, r.get("rowDetailContract"), r::toString);
		assertEquals("1", r.get("barContract"), r::toString);
		assertEquals(true, r.get("handshakeOk"), () -> "an existing expand GET must still handshake: " + r);
		assertEquals(false, r.get("handshakeRejectsOther"), r::toString);
	}
}
