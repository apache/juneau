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
 * Always-on coverage for the View-tab chooser in {@code juneau-config.js} (slice 6): source-shape pins
 * for the XSS {@code textContent} mandate, last-column refusal, pinned-disabled, and the
 * {@code mountChooser} seam, plus a Node behavioral harness when {@code node} is on PATH.
 */
class ViewsJs_ConfigChooser_Test extends TestBase {

	private static String configJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CONFIG_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.CONFIG_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var i = body.indexOf('{', start);
		assertTrue(i >= 0, () -> "'" + signature + "' has no opening brace:\n" + body);
		var depth = 0;
		var j = i;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) { if (body.charAt(j) == '\\') j++; j++; }
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '/') {
				while (j < body.length() && body.charAt(j) != '\n') j++;
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '*') {
				j = body.indexOf("*/", j) + 1;
			}
		}
		return body.substring(start, j);
	}

	@Test void a01_paintUserText_usesTextContentOnly() throws Exception {
		var fn = functionBody(configJs(), "function paintUserText(");
		assertTrue(fn.contains("el.textContent"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
		assertFalse(fn.contains(".html("), fn);
	}

	@Test void a02_paintUserInput_usesValueOnly() throws Exception {
		var fn = functionBody(configJs(), "function paintUserInput(");
		assertTrue(fn.contains("el.value"), fn);
		assertFalse(fn.contains("innerHTML"), fn);
	}

	@Test void a03_canHideColumn_refusesPinnedAndLastVisible() throws Exception {
		var fn = functionBody(configJs(), "function canHideColumn(");
		assertTrue(fn.contains("col?.pinned"), fn);
		assertTrue(fn.contains("visibleCount(draft) > 1"), fn);
	}

	@Test void a04_sanitizeAndPaintHeaders_neverHandTitleToDataTables() throws Exception {
		var body = configJs();
		var san = functionBody(body, "function sanitizeColumnTitlesForDataTables(");
		assertTrue(san.contains("c.title = \"\""), san);
		var paint = functionBody(body, "function paintHeaderTitles(");
		assertTrue(paint.contains("paintUserText(titleEl || th, label)"), paint);
	}

	@Test void a05_chooserExportedOnNsConfig() throws Exception {
		var body = configJs();
		for (var name : new String[]{
			"NS.config.mountChooser = mountChooser",
			"NS.config.paintUserText = paintUserText",
			"NS.config.canHideColumn = canHideColumn",
			"NS.config.sanitizeColumnTitlesForDataTables = sanitizeColumnTitlesForDataTables",
			"NS.config.paintHeaderTitles = paintHeaderTitles",
			"NS.config.applyDraft = applyDraft"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "':\n" + body);
	}

	@Test void a06_viewsJsSeam_mountsChooserWhenColumnConfigAndConfigJsPresent() throws Exception {
		var fn = functionBody(viewsJs(), "function constructTable(");
		assertTrue(fn.contains("viewDef.columnConfig"), fn);
		assertTrue(fn.contains("NS.config.mountChooser"), fn);
		assertTrue(fn.contains("sanitizeColumnTitlesForDataTables"), fn);
		assertTrue(fn.contains("paintHeaderTitles"), fn);
	}

	@Test void a07_saveAsDefault_goesThroughValidateNameBasic() throws Exception {
		var fn = functionBody(configJs(), "function openChooser(");
		assertTrue(fn.contains("validateNameBasic(name)"), fn);
		assertTrue(fn.contains("Save as"), fn);
		assertTrue(fn.contains("paintUserText"), fn);
	}

	@Test void a08_configJs_containsNoInnerHtmlAssignment() throws Exception {
		var body = configJs();
		assertFalse(body.contains(".innerHTML ="), body);
		assertFalse(body.contains(".html("), body);
		assertTrue(body.contains(".textContent"), body);
	}

	@Test void a09_applyDraft_callsApplyView() throws Exception {
		var fn = functionBody(configJs(), "function applyDraft(");
		assertTrue(fn.contains("applyView(table, saved, {"), fn);
		assertTrue(fn.contains("applyRestoredOptionsToLiveGrid"), fn);
		assertTrue(fn.contains("in-flight"), fn);
	}

	@Test void a10_chooserBackdrop_distinctFromActionDialogTeardownClass() throws Exception {
		var body = configJs();
		assertTrue(body.contains("CHOOSER_BACKDROP_CLASS = \"juneau-config-dialog-backdrop\""), body);
		assertFalse(body.contains("CHOOSER_BACKDROP_CLASS = \"juneau-view-dialog-backdrop\""), body);
	}

	@Test void a11_mountChooser_stampsChromeTipNotNativeTitle() throws Exception {
		var fn = functionBody(configJs(), "function mountChooser(");
		assertTrue(fn.contains("stampChromeTip(btn, \"Columns\")"), fn);
		assertFalse(fn.contains("btn.title = \"Columns\""), fn);
	}

	@Test void a12_viewSettings_dialogHasFourTabs() throws Exception {
		var body = configJs();
		assertTrue(body.contains("CONFIG_TABS = [\"view\", \"search\", \"sort\", \"options\"]"), body);
		assertTrue(body.contains("NS.config.CONFIG_TABS = CONFIG_TABS"), body);
	}

	@Test void a13_optionsTab_exposesExactlyPageSizeWrapDensity() throws Exception {
		var fn = functionBody(configJs(), "function buildOptionsTabBody(");
		// The three committed Options controls (design §3.4) and nothing else.
		assertTrue(fn.contains("options.pageSize"), fn);
		assertTrue(fn.contains("options.wrap"), fn);
		assertTrue(fn.contains("options.density"), fn);
	}

	@Test void a14_searchAndSortTabs_editMembershipOnly() throws Exception {
		var body = configJs();
		// Membership rows toggle the facet array on the draft; they never touch search operators or sort direction.
		var fn = functionBody(body, "function buildMembershipRow(");
		assertTrue(fn.contains("_configDraft[facetKey]"), fn);
		assertTrue(body.contains("searchCapableColumns"), body);
		assertTrue(body.contains("sortCapableColumns"), body);
	}

	@Test void a15_applyDraft_persistsViewSettingsToPageState() throws Exception {
		var fn = functionBody(configJs(), "function applyDraft(");
		assertTrue(fn.contains("writeViewSettings(table, viewSettingsFromDraft("), fn);
	}

	@Test void a16_openChooser_seedsDraftFromPersistedViewSettings() throws Exception {
		var fn = functionBody(configJs(), "function openChooser(");
		assertTrue(fn.contains("resolveLastAppliedViewSettings(table, ctx)"), fn);
		// The dialog's committed facets are re-read on open; View Settings are never sourced from the URL.
		assertFalse(fn.contains("location.search"), fn);
	}

	@Test void a17_viewSettings_exportedOnNsConfig() throws Exception {
		var body = configJs();
		for (var name : new String[]{
			"NS.config.viewSettingsFromDraft = viewSettingsFromDraft",
			"NS.config.draftFromViewSettings = draftFromViewSettings",
			"NS.config.readViewSettings = readViewSettings",
			"NS.config.writeViewSettings = writeViewSettings",
			"NS.config.normalizeOptions = normalizeOptions",
			"NS.config.intersectMembership = intersectMembership",
			"NS.config.searchCapableColumns = searchCapableColumns",
			"NS.config.sortCapableColumns = sortCapableColumns",
			"NS.config.selectConfigTab = selectConfigTab",
			"NS.config.resetDraftToDefaults = resetDraftToDefaults"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "':\n" + body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral — Node harness (skipped when node is absent)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var configFile = Files.createTempFile("juneau-config-", ".js");
		try {
			Files.writeString(configFile, configJs(), UTF_8);
			report = Json.to(runNode(harness, configFile), Map.class);
		} finally {
			Files.deleteIfExists(configFile);
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
			var p = Path.of(basedir, "src/test/js/config-chooser.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/config-chooser.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/config-chooser.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path configJs) throws Exception {
		var stdout = Files.createTempFile("config-chooser-stdout-", ".json");
		var stderr = Files.createTempFile("config-chooser-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), configJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("config-chooser.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("config-chooser.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or config-chooser.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_xssPaint_setsTextContent_leavesInnerHtmlUntouched() {
		var r = report();
		assertBean(r, "xssTextContent,xssInnerHtmlUntouched,xssInputValue,xssInputInnerHtmlUntouched",
			"<img src=x onerror=alert(1)>,true,<img src=x onerror=alert(1)>,true");
	}

	@Test void b02_lastVisibleAndPinned_cannotHide() {
		var r = report();
		assertBean(r, "lastCannotHideA,pinnedCannotHide,unpinnedCanHide,alreadyHiddenCanShow", "true,true,true,true");
	}

	@Test void b03_saveAsDefault_refused() {
		var r = report();
		assertBean(r, "defaultReserved,defaultReservedCase,saveAsDefaultRefused", "true,true,true");
	}

	@Test void b04_sanitizeTitles_andPaintHeadersViaTextContent() {
		var r = report();
		assertBean(r, "sanitizedDataTitleBlank,sanitizedSelectionUntouched,headerAText,headerAInnerHtmlUntouched,headerBText",
			"true,true,<img src=x onerror=alert(1)>,true,Col B");
	}

	@Test void b04b_paintHeaderTitles_skipsHiddenColumns_andKeepsSortControl() {
		var r = report();
		assertBean(r, "hiddenSkipTitleA,hiddenSkipOrderKept,hiddenSkipThC", "Col A,ORDER,Col C");
	}

	@Test void b05_defaultDraftAndReorder() {
		var r = report();
		assertBean(r, "defaultDraftOrder,defaultDraftVisible,moved,orderAfterMove", "[A,B,C],[A,B,C],true,[A,C,B]");
	}

	@Test void b06_fourTabsAndDefaultMembership() {
		var r = report();
		// Only data-backed, capability-flagged columns default into the Search/Sort facets.
		assertBean(r, "configTabs,searchCapable,sortCapable,draftSearch,draftSort",
			"[view,search,sort,options],[name,status],[name,status],[name,status],[{column=name,dir=asc},{column=status,dir=asc}]");
	}

	@Test void b07_optionsDefaultsAndClamping() {
		var r = report();
		// Out-of-range page size, non-boolean wrap, and unknown density all fall back to defaults. The embedded
		// maps render in insertion order (pageSize, wrap, density, autoRefreshMs), not alphabetically.
		assertBean(r, "draftOptions,normOptionsClamped,normOptionsOk",
			"{pageSize=25,wrap=false,density=comfortable,autoRefreshMs=0},{pageSize=25,wrap=false,density=comfortable,autoRefreshMs=0},{pageSize=50,wrap=true,density=compact,autoRefreshMs=0}");
	}

	@Test void b08_membershipIntersectionDropsUnknownKeys() {
		var r = report();
		// An absent facet array means "all capable columns", not "none". overlaidOptions renders in insertion
		// order too, see note on b07 above.
		assertBean(r, "intersectDropsUnknown,intersectAbsentIsAll,overlaidSearch,overlaidSort,overlaidOptions",
			"[name,status],[name,status],[name],[{column=status,dir=desc}],{pageSize=100,wrap=true,density=compact,autoRefreshMs=0}");
	}

	@Test void g01_visibleConfigTabsRestrictsAndFallsBackToAllFour() {
		var r = report();
		// Tab order is CONFIG_TABS' fixed order, never the input array's order; an empty or all-unknown tabs
		// array must not silently produce a chooser with no tabs at all.
		assertBean(r,
			"visibleTabsDefaultTrue,visibleTabsDefaultAbsent,visibleTabsRestricted,visibleTabsOrderIsFixed,"
				+ "visibleTabsUnknownNamesDropped,visibleTabsEmptyArrayIsAll,visibleTabsAllUnknownIsAll",
			"[view,search,sort,options],[view,search,sort,options],[search,sort],[view,options],[search],"
				+ "[view,search,sort,options],[view,search,sort,options]");
	}

	@Test void g02_sortTabIsAnOrderedDirectionalPriorityList() {
		var r = report();
		assertBean(r,
			"defaultSortOrderIsCatalogOrderAscending,moveSortEntryUp,moveSortEntryPastEdgeFails,"
				+ "intersectSortOrderDropsUnknownAndKeepsDir,intersectSortOrderCoercesBadDirToAsc,"
				+ "intersectSortOrderDropsDuplicates,intersectSortOrderAbsentIsCatalogOrder",
			"[{column=name,dir=asc},{column=status,dir=asc},{column=created,dir=asc}],"
				+ "{moved=true,order=[name,status]},{moved=false,order=[status,name]},"
				+ "[{column=created,dir=desc},{column=name,dir=desc}],[{column=name,dir=asc}],[{column=name,dir=asc}],"
				+ "[{column=name,dir=asc},{column=status,dir=asc}]");
	}

	@Test void b09_applyPersistsViewSettingsPerTableToPageState() {
		var r = report();
		// A second table on the same page keeps its own keyed slot; the first is not clobbered.  A table with no
		// view id has no page-state scope, so the write is a silent no-op (reads back null).
		assertBean(r, "viewSettingsKeys,persistedKeys,readbackSearch,readbackResetFalseOnValidBlob,twoTableKeys,noIdReadback",
			"[formats,labels,options,order,schemaVersion,search,sort,visible],[releases.viewSettings],[name,status],true,[releases.viewSettings,users.viewSettings],<null>");
	}

	@Test void q01_schemaVersionMismatchDiscardsTheWholeBlobAndReportsReset() {
		assertBean(report(), "mismatchSettingsNull,mismatchResetTrue,mismatchBlobDeleted", "true,true,true");
	}

	@Test void q02_nonObjectStoredValueIsTreatedAsAMismatch() {
		assertBean(report(), "nonObjectSettingsNull,nonObjectResetTrue", "true,true");
	}

	@Test void q03_nothingStoredIsAbsentNotAReset() {
		assertBean(report(), "absentSettingsNull,absentResetFalse", "true,true");
	}

	@Test void q04_draftFromViewSettingsVersionChecksItselfDirectly() {
		assertBean(report(), "v2DraftMatchesDefaults", "true");
	}
}
