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
 * The row-less (ribbon-hosted) dialog seam: a {@link RibbonAction#dialog(String) dialog} ribbon action opened with
 * NO {@code <tr>} behind it, reusing the whole already-reviewed dialog machinery.
 *
 * <p>Two layers, same as {@code ViewsJs_SelfTargetedIdempotency_Test}:
 * <ul class='spaced-list'>
 * 	<li><b>Default-gate source pins</b> (a-group): the one-way module hop the ribbon runtime makes, the catalog
 * 		isolation that keeps a ribbon action out of every row's action menu, and - the guard that matters most -
 * 		the fact that the ROW render paths were NOT made null-tolerant to accommodate a row-less caller.
 * 	<li><b>A behavioral harness</b> ({@code ribbon-dialog.cjs}) that loads BOTH runtimes into one window and drives
 * 		real clicks, since the seam spans the two files and the interesting failures (a refusal painting into a
 * 		row's actions cell that does not exist) are DOM-shaped, not source-shaped.  Runs when {@code node} is on
 * 		{@code PATH}, skipped otherwise.
 * </ul>
 */
class ViewsJs_RibbonDialog_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException { return resource(ViewsMixin.VIEWS_JS_RESOURCE); }
	private static String ribbonJs() throws IOException { return resource(ViewsMixin.RIBBON_JS_RESOURCE); }
	private static String rendersJs() throws IOException { return resource(ViewsMixin.RENDERS_JS_RESOURCE); }

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Default-gate source pins
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ribbonRuntimeHopsToTheViewRuntimesResolver() throws Exception {
		var ribbon = functionBody(ribbonJs(), "function buildRibbon(");
		assertTrue(ribbon.contains("a.type === \"dialog\""), ribbon);
		// The same one-way NS.<module> hop this file already makes for NS.config / NS.persistence, optional-chained
		// so a page that loaded the ribbon runtime without the view runtime renders an inert button, not a throw.
		assertTrue(ribbon.contains("NS.init?.openRibbonDialog"), ribbon);
		assertTrue(ribbon.contains("typeof NS.init?.openRibbonDialog === \"function\""),
			() -> "the hop must be guarded, not assumed:\n" + ribbon);
	}

	@Test void a02_catalogsStayDisjoint() throws Exception {
		var views = viewsJs();
		// The ribbon resolver reads the RIBBON catalog and only the ribbon catalog...
		var ribbonResolver = functionBody(views, "function findRibbonDialogAction(");
		assertTrue(ribbonResolver.contains("ctx?.viewDef?.ribbon"), ribbonResolver);
		assertFalse(ribbonResolver.contains("rowActions"),
			() -> "a ribbon dialog action must not be resolvable from the row-action catalog:\n" + ribbonResolver);
		// ...and the row-action resolver was left alone, so a ribbon action can never surface in a row menu or pill.
		var rowResolver = functionBody(views, "function dialogActionIsOpenable(");
		assertTrue(rowResolver.contains("ctx?.viewDef?.rowActions"), rowResolver);
		assertFalse(rowResolver.contains("viewDef?.ribbon"),
			() -> "widening the ROW catalog to the ribbon is exactly what the separate resolver avoids:\n" + rowResolver);
	}

	@Test void a03_rowRenderPathsWereNotMadeNullTolerant() throws Exception {
		var views = viewsJs();
		// The row renderers keep dereferencing `tr` unguarded: a row-less caller must be ROUTED to the row-less
		// host, not silently absorbed by a null-tolerant row renderer that paints nowhere.
		assertTrue(functionBody(views, "function renderRowActionRefusal(").contains("tr.querySelector"), views);
		assertTrue(functionBody(views, "function renderActionOutcome(").contains("tr.querySelector"), views);
		// The routing lives in the dispatchers instead.
		var refusalFor = functionBody(views, "function renderActionRefusalFor(");
		assertTrue(refusalFor.contains("if (tr) renderRowActionRefusal(tr, action, reason);"), refusalFor);
		assertTrue(refusalFor.contains("else renderRibbonActionRefusal(ctx, action, reason);"), refusalFor);
		var outcomeFor = functionBody(views, "function renderActionOutcomeFor(");
		assertTrue(outcomeFor.contains("if (tr) renderActionOutcome(tr, cls);"), outcomeFor);
		assertTrue(outcomeFor.contains("else renderRibbonActionOutcome(ctx, cls);"), outcomeFor);
		// A row-less mergeRow can't merge (mergeRowFromResult dereferences `tr`), so it redraws instead.
		var apply = functionBody(views, "function applySuccessBehavior(");
		assertTrue(apply.contains("&& tr"), () -> "mergeRow must be gated on having a row:\n" + apply);
	}

	@Test void a04_theRibbonAnchoredHostIsSweptByTeardown() throws Exception {
		// A re-init must not leave a stale banner host behind (and must not sweep a NESTED table's host either -
		// the same ownership scoping the toolbar row already gets).
		var strip = functionBody(viewsJs(), "function stripGeneratedDom(");
		assertTrue(strip.contains("juneau-view-ribbon-banner-host") || strip.contains("RIBBON_BANNER_HOST_CLASS"), strip);
		assertTrue(strip.contains("isInside(table, el)") || strip.contains("isInside("),
			() -> "the sweep must stay ownership-scoped:\n" + strip);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral harness (both runtimes in one window)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var ribbonFile = Files.createTempFile("juneau-ribbon-", ".js");
		try {
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(ribbonFile, ribbonJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile, ribbonFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(ribbonFile);
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
			var p = Path.of(basedir, "src/test/js/ribbon-dialog.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/ribbon-dialog.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/ribbon-dialog.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs, Path ribbonJs) throws Exception {
		var stdout = Files.createTempFile("ribbon-dialog-stdout-", ".json");
		var stderr = Files.createTempFile("ribbon-dialog-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of(
					"node", harness.toString(), rendersJs.toString(), viewsJs.toString(), ribbonJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("ribbon-dialog.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("ribbon-dialog.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or ribbon-dialog.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertAllTrue(String... keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " was " + r.get(k) + " in " + r);
	}

	@Test void b00_bothRuntimesLoadedAndTheSeamIsWired() {
		assertEquals(true, report().get("hasSeam"), report()::toString);
	}

	@Test void b01_aRibbonDialogClickOpensTheDialogWithNoRow() {
		assertAllTrue("open_ribbonRenderedOneButton", "open_buttonNameIsTitleNotId", "open_dialogOpenedWithNoRow",
			"open_noFetchBeforeConfirm", "open_dialogTitleFromWidenedNameRead");
	}

	@Test void b02_theConfirmedSubmitSettlesIntoTheRibbonAnchoredHost() {
		assertAllTrue("submit_firedOnConfirm", "submit_bodyCarriesAction", "submit_bodyHasNoTargetId",
			"submit_outcomeLandedInRibbonHost", "submit_hostAnchoredAfterToolbarRow", "submit_noRowBannerAnywhere");
		var r = report();
		assertEquals("success", r.get("submit_outcomeState"), r::toString);
		assertEquals("status", r.get("submit_outcomeRole"), () -> "a success is a status, not an alert: " + r);
		assertEquals("ribbon-action-outcome", r.get("submit_outcomeTestid"), r::toString);
	}

	/** The two OPEN-path calls that used to die on a null {@code tr} before the submit path was ever reached. */
	@Test void b03_openPathFailuresLandInTheSameHost() {
		assertAllTrue("openFail_noDialogOpened", "openFail_transportRefusalInRibbonHost",
			"openFail_noRowBannerAnywhere", "openParse_refusalInRibbonHost", "openParse_refusalNamesActionByTitle",
			"openParse_noRowBannerAnywhere");
		var r = report();
		assertEquals("refusal", r.get("openFail_transportRefusalState"), r::toString);
		assertEquals("alert", r.get("openFail_transportRefusalRole"), r::toString);
	}

	@Test void b04_aPreflightSubmitRefusalIsVisibleAndNamesTheAction() {
		assertAllTrue("submitRefusal_nothingSent", "submitRefusal_inRibbonHost",
			"submitRefusal_namesActionAndReason", "submitRefusal_noRowBannerAnywhere");
	}

	@Test void b05_catalogIsolationHoldsAtRuntimeToo() {
		assertAllTrue("catalog_ribbonResolverFindsIt", "catalog_rowResolverDoesNot",
			"catalog_findRibbonDialogActionIgnoresNonDialogTypes", "catalog_rowMenuExcludesRibbonDialog");
		assertEquals("ack", report().get("catalog_rowMenuIds"),
			() -> "the row menu must contain the row action and nothing from the ribbon: " + report());
	}

	@Test void b06_anUnresolvableRibbonIdIsAVisibleRefusalNotASilentNoOp() {
		assertAllTrue("unknownId_refusalInRibbonHost", "unknownId_refusalNamesTheId", "unknownId_noDialogOpened");
	}

	@Test void b07_aRowlessMergeRowRedrawsInsteadOfMerging() {
		assertAllTrue("mergeRowless_redrawCalledOnce", "mergeRowless_outcomeStillRendered");
	}
}
