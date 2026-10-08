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
import static org.apache.juneau.test.bct.BctAssertions.*;
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
		var r = report();
		assertBean(r, "hasBuild,hasNormalizeRibbon", "true,true");
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
		// pure_oneRefresh_order's value embeds a comma ("export,refresh") - harmless, assertBean compares the whole
		// joined string, not a re-split per field.
		assertBean(r, "pure_oneRefresh_order,pure_oneRefresh_lastGroup,pure_oneRefresh_exportGroupUnset",
			"export,refresh,__refresh,true");
	}

	@Test void b03_twoOrMoreRefreshActions_allMoveTogetherPreservingRelativeOrder() {
		var r = report();
		assertBean(r, "pure_twoRefresh_order,pure_twoRefresh_bothInRefreshGroup,pure_twoRefresh_relativeOrderPreserved",
			"export,refresh:first,refresh:second,true,true");
	}

	@Test void b04_explicitGroupOnRefresh_optsOutCompletely() {
		var r = report();
		// an explicitly-grouped refresh must NOT be relocated to the end, and must not be re-grouped into __refresh.
		assertBean(r, "pure_explicitGroup_order,pure_explicitGroup_groupUnchanged", "refresh,export,true");
	}

	@Test void b05_danglingTrailingDividerIsDropped() {
		var r = report();
		// A divider stranded in trailing position by the move must be dropped; a NON-trailing divider (still
		// separating two other actions once refresh is gone) must survive.
		assertBean(r, "pure_trailingDivider_order,pure_trailingDivider_dropped,pure_nonTrailingDivider_order",
			"export,refresh,true,divider,export,refresh");
	}

	//------------------------------------------------------------------------------------------------------------------
	// DOM behavior: buildRibbon(viewDef, ctx), through the full render path
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_noRefreshAction_domIsUnaffected() {
		var r = report();
		assertBean(r, "dom_noRefresh_groupCount,dom_noRefresh_onlyGroupButtonCount", "1,2");
	}

	@Test void c02_oneRefreshDeclaredFirst_rendersAsTwoClustersWithRefreshAloneAndLast() {
		var r = report();
		// must render TWO clusters (exports, refresh) - not one merged cluster; the first cluster is the two export
		// buttons; the last cluster holds refresh ALONE and must be the rightmost element of the bar.
		assertBean(r, "dom_oneRefresh_groupCount,dom_oneRefresh_firstGroupButtonCount,dom_oneRefresh_lastGroupButtonCount,"
			+ "dom_oneRefresh_lastGroupIsRefreshGlyph,dom_oneRefresh_lastGroupIsLastChildOfBar", "2,2,1,true,true");
	}

	@Test void c03_twoRefreshActions_bothLandInOneTrailingClusterInRelativeOrder() {
		var r = report();
		// dom_twoRefresh_lastGroupTitles's value embeds a comma ("A,B") - harmless, see note on b02 above.
		assertBean(r, "dom_twoRefresh_groupCount,dom_twoRefresh_lastGroupButtonCount,dom_twoRefresh_lastGroupTitles",
			"2,2,A,B");
	}

	@Test void c04_explicitGroupOnRefresh_staysWithItsDeclaredNeighbourNotRelocated() {
		var r = report();
		// dom_explicitGroup_firstGroupTitles's value embeds a comma ("Refresh,Collapse all") - harmless, see note
		// on b02 above.
		assertBean(r, "dom_explicitGroup_groupCount,dom_explicitGroup_firstGroupButtonCount,"
			+ "dom_explicitGroup_firstGroupTitles,dom_explicitGroup_isNotLastChildOfBar", "2,2,Refresh,Collapse all,true");
	}

	@Test void c05_danglingTrailingDivider_isDroppedFromTheRenderedBar() {
		var r = report();
		assertBean(r, "dom_trailingDivider_groupCount,dom_trailingDivider_dividerCount,"
			+ "dom_trailingDivider_lastGroupButtonCount", "2,0,1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// print export button + collapseAll action
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
		// print must render as its own button, not be silently dropped. dom_print_buttonTitles's value embeds a
		// comma ("copy,print") - harmless, see note on b02 above.
		assertBean(r, "dom_print_groupCount,dom_print_buttonCount,dom_print_buttonTitles", "1,2,copy,print");
	}

	@Test void d05_collapseAllRendersOneButtonThatInvokesCtxCollapseAllDetailRowsOnClick() {
		var r = report();
		// clicking the collapseAll button must invoke ctx.collapseAllDetailRows() exactly once.
		assertBean(r, "dom_collapseAll_groupCount,dom_collapseAll_title,dom_collapseAll_clickInvokedHook",
			"1,Collapse all,1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) The ninth type: a row-less, ribbon-hosted dialog trigger
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_dialogIsNormalizerNeutral_onlyRefreshRelocates() {
		var r = report();
		// a dialog action keeps its declared position; only refresh relocates, and the normalizer must not rewrite
		// a dialog action's own object. pure_dialog_order's value embeds a comma ("dialog,refresh") - harmless.
		assertBean(r, "pure_dialog_order,pure_dialog_groupUnset,pure_dialog_actionObjectUnchanged",
			"dialog,refresh,true,true");
	}

	@Test void e02_dialogIconIsNamed_notTheColumnChooserGear() {
		var r = report();
		assertEquals("new", r.get("pure_dialog_icon"),
			() -> "resolveButtonIcon must resolve 'dialog' via DEFAULT_ICONS to the already-registered 'new' "
				+ "glyph, not fall through to 'tune' (which paints the column chooser's gear): " + r);
	}

	@Test void e03_dialogRendersOneNamedButtonThatHandsItsIdToTheViewRuntime() {
		var r = report();
		// Ribbon buttons are icon-only by construction, so the name lives entirely in title + aria-label. The click
		// must hand the action id to the view runtime's ribbon-catalog resolver, and the table it was built for, so
		// the resolver can find that view's own host.
		assertBean(r, "dom_dialog_groupCount,dom_dialog_buttonCount,dom_dialog_title,dom_dialog_ariaLabel,"
			+ "dom_dialog_clickHandedOffActionId,dom_dialog_clickHandedOffTable",
			"1,1,Add project,Add project,add-project,true");
	}

	@Test void e04_dialogButtonIsInertRatherThanThrowingWithNoViewRuntimeLoaded() {
		var r = report();
		// the NS.init hop is optional-chained precisely so this degrades quietly.
		assertBean(r, "dom_dialog_noViewRuntime_buttonRendered,dom_dialog_noViewRuntime_clickDidNotThrow", "true,true");
	}

	@Test void e05_anUntitledDialogNamesItselfByIdRatherThanRenderingNameless() {
		assertEquals(true, report().get("dom_dialog_untitled_nameFallsBackToId"), report()::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) the flat query-param form joins multiple clauses into the ONE `search` string (design §5.3): the wire
	//    carries a single search parameter, never repeated params.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_multipleActiveTogglesJoinIntoOneSearchString() {
		var r = report();
		// Left as 3 separate assertEquals, not collapsed into one assertBean: each checks a distinct §5.3 join
		// rule (search-clause join, opt-clause join, column-scoped exemption) and keeps its own explanatory
		// diagnostic message rather than sharing one generic BCT failure message.
		// Two active `search`-param toggles fold into ONE comma-joined search string (declared order preserved)...
		assertEquals("status=$eq(OPEN),owner=$eq(me)", r.get("pure_joinSearchClauses"),
			() -> "repeated `search` params are not the API - the browser must join the clauses into one string: " + r);
		// ...while `opt` is single-valued: the last contribution wins.
		assertEquals("density=compact", r.get("pure_joinOptClauses"),
			() -> "opt is no longer comma-joined (BQ#5 renamed it to opts; nothing joins on it): last contribution wins: " + r);
		// A column-scoped toggle is untouched by the join: it stays the native per-index DataTables param.
		assertEquals("$eq(OPEN)", r.get("pure_joinColumnStillNative"),
			() -> "column-scoped options keep their native columns[N][search][value] shape (folded server-side): " + r);
	}

	@Test void f02_singleToggleHasNoLeadingOrTrailingSeparator() {
		var r = report();
		assertEquals("status=$eq(OPEN)", r.get("pure_joinSingleSearchNoComma"),
			() -> "one active toggle must yield the bare clause, with no dangling separator comma: " + r);
	}

	@Test void f03_theJoinOnlyInsertsATopLevelSeparator_innerParenCommasSurvive() {
		var r = report();
		assertEquals("status=$in(OPEN,CLOSED),tier=$eq(gold)", r.get("pure_joinProtectsInnerCommas"),
			() -> "a comma inside a clause value's $in(...) belongs to that clause; the join adds only a top-level "
				+ "separator between clauses: " + r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) The ribbon splits into column searches (per dtIndex) and query params, merged with user searches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_columnScopedOptionsBecomePerIndexSearches_sameColumnAnds() throws Exception {
		assertEquals("{\"1\":\"$eq(DROPPED)\",\"2\":\"$and($in(Waiting,\\\"Ready to push\\\"),$ne(Waiting))\"}",
			Json.of(report().get("split_columnSearches")));
	}

	@Test void g02_paramScopedOptionsBecomeQueryParams() throws Exception {
		assertEquals("{\"owner\":\"me\"}", Json.of(report().get("split_queryParams")));
	}

	@Test void g03_mergeDropsBlankUserValues_andAndsASharedColumn() throws Exception {
		assertEquals("{\"1\":\"$and($eq(ACTIVE),$eq(DROPPED))\",\"2\":\"$ne(Waiting)\"}", Json.of(report().get("split_merged")));
	}

	@Test void g04_ribbonToQueryParamsWrapperRemoved() throws Exception {
		assertEquals(Boolean.TRUE, report().get("split_wrapperRemoved"), () -> report().toString());
	}
}
