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
 * Always-on coverage for the dialog-hosted {@link org.apache.juneau.rest.server.widgets.BarSlot} runtime &mdash; the
 * <b>third</b> named host, {@code ModalDef.barSlot}: painting the region + widgets + sidecar from the fetched JSON
 * (there is no server-rendered pass to relocate, unlike the row-detail host), the {@code dialog-title} anchor, and
 * the REUSE (not reimplementation) of the row-detail host's clone-time id minting, enhance-on-insert, and collapse
 * teardown.
 *
 * <p>
 * Source-shape always runs; the behavioral {@code dialog-bar-slot.cjs} harness runs when {@code node} is on
 * {@code PATH} (skipped otherwise &mdash; no {@code -Pjs-tests} required).  That harness loads
 * {@code juneau-views.js} <b>and</b> {@code juneau-chrome.js} into one sandbox, exactly as
 * {@code ViewsJs_RowDetail_BarSlot_Test} does for the second host.
 */
class ViewsJs_Dialog_BarSlot_Test extends TestBase {

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
			"buildDialogBarSlotRegion: buildDialogBarSlotRegion",
			"insertDialogBarSlot: insertDialogBarSlot"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
	}

	@Test void a02_dialogOwnsItsOwnPlacement_notTheSharedStripBuilder() throws Exception {
		var body = viewsJs();
		// buildDialogOverlay calls the dialog's own insert helper; the generic strip builders must never learn a
		// dialog bar slot exists (FINISHED-J0445u/J0445w locks this the same way for the row-detail host).
		assertTrue(body.contains("insertDialogBarSlot(dialog, title, modal?.barSlot, seq)"), body);
		var buildRibbonAt = body.indexOf("function buildRibbonStrip(");
		var buildDetailAt = body.indexOf("function buildDetailStrip(");
		assertTrue(buildRibbonAt >= 0 && buildDetailAt >= 0, body);
		var ribbonBody = body.substring(buildRibbonAt, body.indexOf("\n\t}", buildRibbonAt));
		var detailStripBody = body.substring(buildDetailAt, body.indexOf("\n\t}", buildDetailAt));
		assertFalse(ribbonBody.contains("Dialog"), "buildRibbonStrip must not learn about dialogs");
		assertFalse(detailStripBody.contains("insertDialogBarSlot"), "buildDetailStrip must not learn about dialogs");
	}

	@Test void a03_identityMintingIsReusedVerbatim_notReimplemented() throws Exception {
		var body = viewsJs();
		// The dialog caller calls the SAME mintDetailBarSlotIdentity/teardownDetailBarSlot/enhanceChromeInPanel the
		// row-detail host uses - no dialog-specific mint/teardown/enhance function exists.
		assertTrue(body.contains("mintDetailBarSlotIdentity(dialog, \"dialog\", seq)"), body);
		assertTrue(body.contains("teardownDetailBarSlot(ui.dialog)"), body);
		assertTrue(body.contains("enhanceChromeInPanel(ui.dialog)"), body);
		assertFalse(body.contains("function mintDialogBarSlotIdentity("), body);
		assertFalse(body.contains("function teardownDialogBarSlot("), body);
	}

	@Test void a04_regionPaintedWithCreateElementAndTextContentOnly_neverInnerHtml() throws Exception {
		var body = viewsJs();
		var at = body.indexOf("function buildDialogBarSlotRegion(");
		var widgetAt = body.indexOf("function buildDialogBarWidgetEl(");
		var badgeAt = body.indexOf("function buildDialogBarBadgeEl(");
		assertTrue(at >= 0 && widgetAt >= 0 && badgeAt >= 0, body);
		var end = body.indexOf("function buildDialogOverlay(");
		assertTrue(end > at, body);
		var slice = body.substring(Math.min(at, Math.min(widgetAt, badgeAt)), end);
		assertFalse(slice.contains(".innerHTML"), "dialog bar-slot painting must never use innerHTML:\n" + slice);
	}

	@Test void a05_enhanceOnInsertGoesThroughTheExportedChromeEntry() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("window.JuneauChrome"), body);
		assertTrue(body.contains("initAll()"), body);
	}

	@Test void a06_chromeSharesOneWiredMarkerForSafeActions() throws Exception {
		var body = chromeJs();
		assertTrue(body.contains("SAFE_WIRED_ATTR = \"data-juneau-safe-wired\""), body);
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
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(chromeFile, chromeJs(), UTF_8);
			report = Json.to(runNode(harness, viewsFile, chromeFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(chromeFile);
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
			var p = Path.of(basedir, "src/test/js/dialog-bar-slot.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-bar-slot.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-bar-slot.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs, Path chromeJs) throws Exception {
		var stdout = Files.createTempFile("dialog-bar-slot-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-bar-slot-stderr-", ".txt");
		try {
			var cmd = List.of("node", harness.toString(), viewsJs.toString(), chromeJs.toString());
			var p = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-bar-slot.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-bar-slot.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or dialog-bar-slot.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_bothRuntimesLoadedAndHelpersPresent() {
		var r = report();
		assertEquals(true, r.get("hasViews"), r::toString);
		assertEquals(true, r.get("hasChrome"), r::toString);
		assertEquals(true, r.get("hasBuildRegion"), r::toString);
		assertEquals(true, r.get("hasInsert"), r::toString);
		assertEquals(true, r.get("hasMint"), r::toString);
		assertEquals(true, r.get("hasTeardown"), r::toString);
		assertEquals(true, r.get("hasEnhance"), r::toString);
	}

	@Test void b02_regionPaintedFromJson_immediatelyFollowingTheTitle() {
		var r = report();
		assertEquals(true, r.get("paint_inserted"), r::toString);
		assertEquals(1.0, ((Number)r.get("paint_regionCount")).doubleValue(), r::toString);
		assertEquals(true, r.get("paint_regionFollowsTitle"), r::toString);
		assertEquals(true, r.get("paint_hasBarSlotClass"), r::toString);
		assertEquals(true, r.get("paint_hasDialogSlotClass"), r::toString);
	}

	@Test void b03_regionCarriesTheDialogTitleAnchor() {
		var r = report();
		assertEquals(true, r.get("paint_anchorIsDialogTitle"), r::toString);
	}

	@Test void b04_widgetsPaintedWithTextContentOnly() {
		var r = report();
		assertEquals(true, r.get("paint_textWidgetPainted"), r::toString);
		assertEquals("state", r.get("paint_textWidgetMarker"), r::toString);
		assertEquals(true, r.get("paint_badgeLabelPainted"), r::toString);
		assertEquals(true, r.get("paint_badgeCountPainted"), r::toString);
		assertEquals(true, r.get("paint_badgeNamespaced"), r::toString);
	}

	@Test void b05_sidecarIsIdLess_andCarriesTheBarContractAndInitialCounts() {
		var r = report();
		assertEquals(true, r.get("paint_sidecarPresent"), r::toString);
		assertEquals(true, r.get("paint_sidecarJsonOk"), r::toString);
	}

	@Test void b06_cloneTimeIdMinting_reusedForTwoStackedDialogs() {
		var r = report();
		assertEquals("dialog:1", r.get("mint_markerA"), r::toString);
		assertEquals("dialog:2", r.get("mint_markerB"), r::toString);
		assertEquals(true, r.get("mint_markerA_isDialogSeq1"), r::toString);
		assertEquals(true, r.get("mint_markerB_isDialogSeq2"), r::toString);
		assertEquals(true, r.get("mint_distinct"), () -> "two stacked dialogs must never share an id: " + r);
		assertEquals("juneau-bar:dialog:1", r.get("mint_sidecarIdA"), r::toString);
		assertEquals("juneau-bar:dialog:2", r.get("mint_sidecarIdB"), r::toString);
		assertEquals(true, r.get("mint_sidecarIdA_isPrefixPlusMarker"), r::toString);
		assertEquals(true, r.get("mint_authorIdUnresolvable"),
			() -> "getElementById('juneau-bar:' + authorId) must not resolve after minting: " + r);
	}

	@Test void b07_eachMintedIdRoundTripsThroughReadSidecar() {
		var r = report();
		assertEquals(true, r.get("mint_roundTripA"), r::toString);
		assertEquals(true, r.get("mint_roundTripB"), r::toString);
	}

	@Test void b08_enhanceOnInsert_appliesCountsAndIsIdempotent_noDoubleBoundSafeClick() {
		var r = report();
		assertEquals(true, r.get("enh_seamCalled"), r::toString);
		assertEquals("3", r.get("enh_countBeforeA"), r::toString);
		assertEquals("3", r.get("enh_countAfterA"), r::toString);
		assertEquals("1", r.get("enh_wiredMarker"), () -> "the shared wired marker must be stamped: " + r);
		assertEquals(1.0, ((Number)r.get("enh_safeListenersAfterFirst")).doubleValue(), r::toString);
		assertEquals(1.0, ((Number)r.get("enh_safeListenersAfterSecond")).doubleValue(),
			() -> "a second enhance-on-insert must not double-bind the SAFE action: " + r);
		assertEquals("3", r.get("enh_countStillA"), r::toString);
		assertEquals(1.0, ((Number)r.get("enh_safeFires")).doubleValue(), r::toString);
	}

	@Test void b09_dialogWithNoBarSlot_enhanceIsACleanNoOp() {
		var r = report();
		assertEquals(false, r.get("enh_seamSkippedWithoutSlot"), r::toString);
	}

	@Test void b10_collapseTeardown_reusedVerbatim_leavesTheOtherStackedDialogAlone() {
		var r = report();
		assertEquals(true, r.get("tear_sidecarIdRemoved"), r::toString);
		assertEquals(true, r.get("tear_aUnresolvable"), r::toString);
		assertEquals(true, r.get("tear_bStillResolvable"), r::toString);
		assertEquals(true, r.get("tear_bStillRoundTrips"), r::toString);
		assertEquals(true, r.get("tear_noThrowOnNull"), r::toString);
	}

	@Test void b11_failClosedNoOp_missingMalformedOrEmptyBarSlotNeverBlocksTheDialog() {
		var r = report();
		assertEquals(false, r.get("noop_undeclared"), r::toString);
		assertEquals(0.0, ((Number)r.get("noop_undeclaredRegionCount")).doubleValue(), r::toString);
		assertEquals(false, r.get("noop_emptyWidgets"), r::toString);
		assertEquals(false, r.get("noop_badContractVersion"),
			() -> "a bar-slot contract mismatch must be refused, not painted: " + r);
		assertEquals(false, r.get("noop_missingId"), r::toString);
	}
}
