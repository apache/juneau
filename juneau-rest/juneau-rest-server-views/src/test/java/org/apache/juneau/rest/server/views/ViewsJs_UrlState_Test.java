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
 * Always-on coverage for the shareable "Copy link" URL-state codec in {@code juneau-urlstate.js} and the live
 * T17–T19 wire in {@code juneau-views.js} / {@code juneau-config.js}: source-shape pins for the tab/filter/sort-only
 * grammar, the {@code NS.urlState} export surface, the views/config call sites, plus Node behavioral harnesses
 * (when {@code node} is on PATH) that prove encode/decode, nested/View-Settings drop, address-bar / clean-address /
 * open-precedence glue, and the live wire that actually syncs the primary table.
 */
class ViewsJs_UrlState_Test extends TestBase {

	private static String urlStateJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.URLSTATE_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.URLSTATE_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String configJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CONFIG_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.CONFIG_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Source-shape — runs with no Node at all
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_attachesToJuneauViewsNamespace() throws Exception {
		var body = urlStateJs();
		assertTrue(body.contains("window.JuneauViews = window.JuneauViews || {}"), body);
		assertTrue(body.contains("NS.urlState = {"), body);
	}

	@Test void a02_theOneQueryParamIsNamedState() throws Exception {
		var body = urlStateJs();
		assertTrue(body.contains("STATE_PARAM = 'state'"), body);
	}

	@Test void a03_grammarIsTabFilterSortOnly() throws Exception {
		var body = urlStateJs();
		// The three - and only three - directive names the codec emits and recognizes.
		assertTrue(body.contains("'tab('"), body);
		assertTrue(body.contains("'filter('"), body);
		assertTrue(body.contains("'sort('"), body);
		// View-Settings / nested-table shapes are never part of the grammar.
		assertFalse(body.contains("'subview('"), body);
		assertFalse(body.contains("'visible('"), body);
		assertFalse(body.contains("'options('"), body);
	}

	@Test void a04_exportedOnNsUrlState() throws Exception {
		var body = urlStateJs();
		for (var name : new String[]{
			"encode: encodeState",
			"decode: decodeState",
			"isEmptyState: isEmptyState",
			"readFromSearch: readFromSearch",
			"buildSearch: buildSearch",
			"writeToAddressBar: writeToAddressBar",
			"buildShareUrl: buildShareUrl",
			"copy: copy",
			"cleanAddressEnabled: cleanAddressEnabled",
			"resolveOpenState: resolveOpenState"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "':\n" + body);
	}

	@Test void a05_dependencyFree_noViewSettingsFacetKeys() throws Exception {
		var body = urlStateJs();
		// The codec must never reach into the page-state store or reference View-Settings facet keys - those live in
		// a separate store facet the URL never carries.
		assertFalse(body.contains("pageState"), body);
		assertFalse(body.contains("viewSettings"), body);
	}

	@Test void a06_viewsWiresLiveShareableUrlStateFromConstructTable() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function wireShareableUrlState("), body);
		assertTrue(body.contains("wireShareableUrlState(table, ctx)"), body);
		assertTrue(body.contains("function syncShareableUrlState("), body);
		assertTrue(body.contains("function applyShareableOpenState("), body);
		assertTrue(body.contains("function buildShareableUrl("), body);
		// View Settings stay out of the URL facet.
		assertTrue(body.contains("View Settings stay in the page-state store"), body);
	}

	@Test void a07_configExportsResolveAndCopyShareLink() throws Exception {
		var body = configJs();
		assertTrue(body.contains("function resolveShareableOpenState("), body);
		assertTrue(body.contains("function copyShareLink("), body);
		assertTrue(body.contains("NS.config.resolveShareableOpenState = resolveShareableOpenState"), body);
		assertTrue(body.contains("NS.config.copyShareLink = copyShareLink"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral — Node harness (skipped when node is absent)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;
	private static Map<?,?> liveReport;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness("url-state.cjs");
		if (harness != null) {
			var jsFile = Files.createTempFile("juneau-urlstate-", ".js");
			try {
				Files.writeString(jsFile, urlStateJs(), UTF_8);
				report = Json.to(runNode(harness, List.of(jsFile.toString())), Map.class);
			} finally {
				Files.deleteIfExists(jsFile);
			}
		}
		var live = locateHarness("url-state-live.cjs");
		if (live != null) {
			var urlState = writeTempJs("juneau-urlstate-", urlStateJs());
			var renders = writeTempResource("juneau-renders-", ViewsMixin.RENDERS_JS_RESOURCE);
			var views = writeTempResource("juneau-views-", ViewsMixin.VIEWS_JS_RESOURCE);
			try {
				liveReport = Json.to(runNode(live, List.of(
					urlState.toString(), renders.toString(), views.toString()
				)), Map.class);
			} finally {
				Files.deleteIfExists(urlState);
				Files.deleteIfExists(renders);
				Files.deleteIfExists(views);
			}
		}
	}

	private static Path writeTempJs(String prefix, String body) throws IOException {
		var f = Files.createTempFile(prefix, ".js");
		Files.writeString(f, body, UTF_8);
		return f;
	}

	private static Path writeTempResource(String prefix, String resource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, () -> "missing classpath resource: " + resource);
			var f = Files.createTempFile(prefix, ".js");
			Files.write(f, in.readAllBytes());
			return f;
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

	private static Path locateHarness(String name) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js", name);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/" + name,
			"juneau-rest/juneau-rest-server-views/src/test/js/" + name
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, List<String> args) throws Exception {
		var stdout = Files.createTempFile("url-state-stdout-", ".json");
		var stderr = Files.createTempFile("url-state-stderr-", ".txt");
		try {
			var cmd = new ArrayList<String>();
			cmd.add("node");
			cmd.add(harness.toString());
			cmd.addAll(args);
			var pb = new ProcessBuilder(cmd)
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(harness.getFileName() + " did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(harness.getFileName() + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or url-state.cjs not found — behavioral layer skipped");
		return report;
	}

	private static Map<?,?> liveReport() {
		assumeTrue(liveReport != null, "node not available or url-state-live.cjs not found — live wire skipped");
		return liveReport;
	}

	@Test void b01_roundTripsTabMultiFilterAndSort() {
		var r = report();
		// The raw $-DSL body (its own commas and nested parens) is preserved verbatim across the round-trip.
		assertBean(r, "encoded,decodedTab,decodedFilters,decodedSort,rawFilterPreserved",
			"tab(setup);filter(status=$in(OPEN,CLOSED));filter(name=$eq(a,b));sort(name=desc),setup,"
				+ "[status=$in(OPEN,CLOSED),name=$eq(a,b)],name=desc,true");
	}

	@Test void b01a_clauseGrammar_escapesRoundTrip_multiClauseBody_firstUnescapedEquals() {
		var r = report();
		// A column name carrying the grammar's own comma/equals is escaped on encode and restored on decode.
		// One filter body with multiple top-level clauses -> one filter each; a comma inside $in(...) is protected.
		// Only the FIRST unescaped '=' splits key/value; a later '=' stays verbatim in the value.
		assertBean(r, "escapedColEncoded,escapedColDecoded,multiClauseFilters,firstEqualsSplit",
			"filter(a\\,b\\=c=$eq(1)),[a,b=c|$eq(1)],[status=$eq(OPEN),name=$in(a,b),score=$gt(5)],expr|$eq(a=b)");
	}

	@Test void b01b_dollarPrefixedColumnName_escapedOnEncode_restoredOnDecode() {
		var r = report();
		// Task 5 (ClauseParser.escape) escapes a LEADING '$' in a key; this is its JS-side mirror.
		assertBean(r, "dollarColEncoded,dollarColDecoded", "filter(\\$weird=$eq(1)),[$weird|$eq(1)]");
	}

	@Test void b02_emptyAndBlankFacetsEncodeToNothing() {
		var r = report();
		assertBean(r, "emptyEncodes,blankFacetsDropped,isEmptyOfDecodedEmpty,isEmptyOfDecodedFull", ",,true,false");
	}

	@Test void b03_unknownDirectivesAreDropped_soNestedAndViewSettingsNeverLeakIn() {
		var r = report();
		// subview / visible / options were all discarded; only the tab and the real filter remain.
		assertBean(r, "hostileTab,hostileFilters,hostileSort,hostileEncoded",
			"main,[status=$eq(OK)],<null>,tab(main);filter(status=$eq(OK))");
	}

	@Test void b04_readFromSearchExtractsStateParam() {
		var r = report();
		// A percent-encoded value still decodes back to the raw expression.
		assertBean(r, "readFromFull{tab,sort},readAbsentIsNull,readEmptyIsNull,readEncoded",
			"{x,name=asc},true,true,status=$in(OPEN,CLOSED)");
	}

	@Test void b05_buildSearchReplacesStateAndKeepsOtherParams() {
		var r = report();
		assertBean(r, "buildReplaces,buildDropsWhenEmpty,buildAddsToBare",
			"?foo=1&bar=2&state=tab(x),?foo=1,?state=tab(x)");
	}

	@Test void b06_writeToAddressBarSyncs_cleanAddressIsANoOp() {
		var r = report();
		// Under the clean-address option the address bar is deliberately not touched.
		assertBean(r, "wroteAddressBar,addressBarUrl,cleanIsNoOp,cleanWroteNothing",
			"true,/rest/reports?page=2&state=tab(x)#frag,false,true");
	}

	@Test void b07_buildShareUrlAlwaysCarriesState() {
		var r = report();
		assertEquals("https://h/rest/reports?page=2&state=tab(setup);filter(status=$eq(OK))#frag", r.get("shareUrl"));
	}

	@Test void b08_copyResolvesTrueOnSuccessFalseOtherwise_neverThrows() {
		var r = report();
		assertBean(r, "copyOk,copiedText,copyNoApi,copyRejected", "true,HELLO,false,false");
	}

	@Test void b09_cleanAddressReadFromMetaOrAttribute() {
		var r = report();
		assertBean(r, "cleanFromMeta,cleanFromAttr,cleanDefaultFalse", "true,true,false");
	}

	@Test void b10_openPrecedence_urlWinsElseStore() {
		var r = report();
		assertBean(r, "precedenceUrlWins,precedenceFallsBackToStore,precedenceNullWhenBothEmpty",
			"fromUrl,fromStore,<null>");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Live wire — Node harness proving T17–T19 are invoked from the views runtime
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_liveWireExportsArePresent() {
		var r = liveReport();
		assertBean(r, "hasWire,hasCollect,hasApply,hasSync,hasUrlState", "true,true,true,true,true");
	}

	@Test void c02_primaryTableFilterAndSortUpdateAddressBar() {
		var r = liveReport();
		assertBean(r, "isPrimary,syncedAfterFilter,syncedAfterSort,shareUrlCarriesState", "true,true,true,true");
	}

	@Test void c03_cleanAddressLeavesBarClean_shareUrlStillCarriesState() {
		var r = liveReport();
		assertBean(r, "cleanAddressBarUntouched,cleanShareUrlHasState", "true,true");
	}

	@Test void c04_openStateWinsWithoutClobberingViewSettings() {
		var r = liveReport();
		assertBean(r, "openAppliedStatus,openAppliedSort,openViewSettingsIntact", "true,true,true");
	}

	@Test void c05_incompleteFilterSkipped_siblingStillApplies() {
		var r = liveReport();
		assertBean(r, "incompleteSkippedKeptPrior,validSiblingApplied", "true,true");
	}

	@Test void c06_nestedTableIsNeverPrimary() {
		var r = liveReport();
		assertEquals(true, r.get("nestedNotPrimary"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) F1 - ?state= tab lookup never builds a selector from input
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_f03_tabLookupNeverThrowsClicksOnlyExactMatchesAndNeverBuildsASelectorFromInput() {
		var r = liveReport();
		// f1NoThrowOnUnusualTabIds: a tab id containing \, ", ], newline, or * must never throw.
		// f1ClickedExactlyMatchingTabs: each call must click exactly the tab whose dataset.juneauStripTab equals the tab id.
		// f1NoSelectorConcatenation: applyShareableOpenState must not concatenate untrusted input into a querySelector(All)? call.
		assertBean(r::toString, r, "f1NoThrowOnUnusualTabIds,f1ClickedExactlyMatchingTabs,f1NoSelectorConcatenation", "true,true,true");
	}
}
