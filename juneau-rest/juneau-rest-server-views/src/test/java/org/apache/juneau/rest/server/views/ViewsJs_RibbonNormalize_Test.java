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
 * The module's first behavioral DOM harness for {@code buildRibbon(...)} in {@code juneau-ribbon.js}, covering the
 * refresh-to-trailing-cluster normalization.
 *
 * <p>
 * The existing wiring canaries in {@code ViewsMixin_Serving_Test} (e.g. {@code f04_...}) assert only source
 * substrings - {@code function place(}, {@code juneau-view-ribbon-group}, {@code __ungrouped} - every one of which
 * survives this normalizer landing, so they stay green regardless of whether refresh actually moves. This class
 * asserts the rendered DOM shape instead: which buttons land in which {@code .juneau-view-ribbon-group} cluster,
 * and in what order, for each of the normalizer's specified cases.
 */
class ViewsJs_RibbonNormalize_Test extends TestBase {

	private static String ribbonJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RIBBON_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
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
		var ribbonFile = Files.createTempFile("juneau-ribbon-", ".js");
		try {
			Files.writeString(ribbonFile, ribbonJs(), UTF_8);
			report = Json.to(runNode(harness, ribbonFile), Map.class);
		} finally {
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
			var p = Path.of(basedir, "src/test/js/ribbon-normalize.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/ribbon-normalize.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/ribbon-normalize.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path ribbonJs) throws Exception {
		var stdout = Files.createTempFile("ribbon-normalize-stdout-", ".json");
		var stderr = Files.createTempFile("ribbon-normalize-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), ribbonJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("ribbon-normalize.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("ribbon-normalize.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or ribbon-normalize.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_harnessLoadedTheRibbonRuntime() {
		assertEquals(true, report().get("hasBuild"), report()::toString);
		assertEquals(true, report().get("hasNormalizeRibbon"), () -> "normalizeRibbon must be exported on NS.ribbon: " + report());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pure function: normalizeRibbon(actions) - DOM-free
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_noRefreshAction_isAnIdentityNoOp() {
		var r = report();
		assertEquals(true, r.get("pure_noRefresh_isSameReference"),
			() -> "with no refresh action, normalizeRibbon must return the SAME array reference, unperturbed: " + r);
	}

	@Test void b02_oneRefreshAction_movesLastIntoItsOwnGroup() {
		var r = report();
		assertEquals("export,refresh", r.get("pure_oneRefresh_order"), r::toString);
		assertEquals("__refresh", r.get("pure_oneRefresh_lastGroup"), r::toString);
		assertEquals(true, r.get("pure_oneRefresh_exportGroupUnset"),
			() -> "the export action itself must be untouched by the move: " + r);
	}

	@Test void b03_twoOrMoreRefreshActions_allMoveTogetherPreservingRelativeOrder() {
		var r = report();
		assertEquals("export,refresh:first,refresh:second", r.get("pure_twoRefresh_order"), r::toString);
		assertEquals(true, r.get("pure_twoRefresh_bothInRefreshGroup"),
			() -> "both refresh actions must land in the SAME trailing __refresh cluster: " + r);
		assertEquals(true, r.get("pure_twoRefresh_relativeOrderPreserved"), r::toString);
	}

	@Test void b04_explicitGroupOnRefresh_optsOutCompletely() {
		var r = report();
		assertEquals("refresh,export", r.get("pure_explicitGroup_order"),
			() -> "an explicitly-grouped refresh must NOT be relocated to the end: " + r);
		assertEquals(true, r.get("pure_explicitGroup_groupUnchanged"),
			() -> "an explicitly-grouped refresh must not be re-grouped into __refresh: " + r);
	}

	@Test void b05_danglingTrailingDividerIsDropped() {
		var r = report();
		assertEquals("export,refresh", r.get("pure_trailingDivider_order"),
			() -> "a divider stranded in trailing position by the move must be dropped: " + r);
		assertEquals(true, r.get("pure_trailingDivider_dropped"), r::toString);
		// A NON-trailing divider (still separating two other actions once refresh is gone) must survive.
		assertEquals("divider,export,refresh", r.get("pure_nonTrailingDivider_order"), r::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// DOM behavior: buildRibbon(viewDef, ctx), through the full render path
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_noRefreshAction_domIsUnaffected() {
		var r = report();
		assertEquals(1L, ((Number)r.get("dom_noRefresh_groupCount")).longValue(), r::toString);
		assertEquals(2L, ((Number)r.get("dom_noRefresh_onlyGroupButtonCount")).longValue(), r::toString);
	}

	@Test void c02_oneRefreshDeclaredFirst_rendersAsTwoClustersWithRefreshAloneAndLast() {
		var r = report();
		assertEquals(2L, ((Number)r.get("dom_oneRefresh_groupCount")).longValue(),
			() -> "must render TWO clusters (exports, refresh) - not one merged cluster: " + r);
		assertEquals(2L, ((Number)r.get("dom_oneRefresh_firstGroupButtonCount")).longValue(),
			() -> "the first cluster must be the two export buttons: " + r);
		assertEquals(1L, ((Number)r.get("dom_oneRefresh_lastGroupButtonCount")).longValue(),
			() -> "the last cluster must hold refresh ALONE: " + r);
		assertEquals(true, r.get("dom_oneRefresh_lastGroupIsRefreshGlyph"), r::toString);
		assertEquals(true, r.get("dom_oneRefresh_lastGroupIsLastChildOfBar"),
			() -> "the refresh cluster must be the rightmost element of the bar: " + r);
	}

	@Test void c03_twoRefreshActions_bothLandInOneTrailingClusterInRelativeOrder() {
		var r = report();
		assertEquals(2L, ((Number)r.get("dom_twoRefresh_groupCount")).longValue(), r::toString);
		assertEquals(2L, ((Number)r.get("dom_twoRefresh_lastGroupButtonCount")).longValue(),
			() -> "both refresh buttons must share ONE trailing cluster: " + r);
		assertEquals("A,B", r.get("dom_twoRefresh_lastGroupTitles"),
			() -> "relative order of the moved refresh actions must be preserved: " + r);
	}

	@Test void c04_explicitGroupOnRefresh_staysWithItsDeclaredNeighbourNotRelocated() {
		var r = report();
		assertEquals(2L, ((Number)r.get("dom_explicitGroup_groupCount")).longValue(), r::toString);
		assertEquals(2L, ((Number)r.get("dom_explicitGroup_firstGroupButtonCount")).longValue(), r::toString);
		assertEquals("Refresh,Column search", r.get("dom_explicitGroup_firstGroupTitles"),
			() -> "refresh must stay clustered with its declared group-mate, in declared order: " + r);
		assertEquals(true, r.get("dom_explicitGroup_isNotLastChildOfBar"),
			() -> "an opted-out refresh must NOT be relocated to the far right: " + r);
	}

	@Test void c05_danglingTrailingDivider_isDroppedFromTheRenderedBar() {
		var r = report();
		assertEquals(2L, ((Number)r.get("dom_trailingDivider_groupCount")).longValue(), r::toString);
		assertEquals(0L, ((Number)r.get("dom_trailingDivider_dividerCount")).longValue(),
			() -> "a divider stranded before the moved refresh must not render as an empty seam: " + r);
		assertEquals(1L, ((Number)r.get("dom_trailingDivider_lastGroupButtonCount")).longValue(), r::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// WORK-J0507 (Foundry WORK-P0063 toolbar follow-up): print export button + collapseAll action
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_printIconResolvesToItsOwnKey_notTheNeutralFallback() {
		var r = report();
		assertEquals("print", r.get("pure_print_icon"),
			() -> "resolveButtonIcon(null, 'print') must resolve via DEFAULT_ICONS, not fall back to 'tune': " + r);
	}

	@Test void d02_printSurvivesExportButtonResolutionWithNoExtraDepsPresent() {
		var r = report();
		assertEquals("copy,print", r.get("pure_print_resolvedFromAlwaysOnButtons"),
			() -> "print needs no extra dep (unlike excel/pdf), so it must resolve from an always-on `buttons` "
				+ "list even with jszip/pdfmake both absent: " + r);
	}

	@Test void d03_collapseIconIsWiredNotJustReserved() {
		var r = report();
		assertEquals("unfold_less", r.get("pure_collapse_icon"),
			() -> "resolveButtonIcon(null, 'collapse') must resolve to the wired icon key: " + r);
	}

	@Test void d04_printButtonRendersAsItsOwnButtonAlongsideCopy() {
		var r = report();
		assertEquals(1L, ((Number)r.get("dom_print_groupCount")).longValue(), r::toString);
		assertEquals(2L, ((Number)r.get("dom_print_buttonCount")).longValue(),
			() -> "print must render as its own button, not be silently dropped: " + r);
		assertEquals("copy,print", r.get("dom_print_buttonTitles"), r::toString);
	}

	@Test void d05_collapseAllRendersOneButtonThatInvokesCtxCollapseAllDetailRowsOnClick() {
		var r = report();
		assertEquals(1L, ((Number)r.get("dom_collapseAll_groupCount")).longValue(), r::toString);
		assertEquals("Collapse all", r.get("dom_collapseAll_title"), r::toString);
		assertEquals(1L, ((Number)r.get("dom_collapseAll_clickInvokedHook")).longValue(),
			() -> "clicking the collapseAll button must invoke ctx.collapseAllDetailRows() exactly once: " + r);
	}
}
