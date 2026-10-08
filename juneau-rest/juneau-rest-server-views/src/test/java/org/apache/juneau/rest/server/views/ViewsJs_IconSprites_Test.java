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
 * Always-on coverage for {@code juneau-icons.js}'s page-level sprite layering API.
 *
 * <p>The runtime resolves every chrome glyph through up to three sprite layers - per-icon override, then the
 * app's replacement sprite, then Juneau's shipped set - merged once into a single in-document
 * {@code <symbol id="juneau-sym-{stem}">} sprite so hosts can {@code <use href="#juneau-sym-{stem}"/>}.  This
 * test pins the module boundary with always-on source-shape assertions, then exercises the actual resolution,
 * failure, and late-registration behavior in a Node VM sandbox (skipped when Node is absent) over the REAL
 * shipped sprite so the fixtures cannot drift from what ships.
 */
class ViewsJs_IconSprites_Test extends TestBase {

	private static String iconsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.ICONS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String symbolsSvg() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.SYMBOLS_SVG_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source shape - the public API surface and the documented registration attributes
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_theLayeringApiIsExported() throws Exception {
		var body = iconsJs();
		for (var member : new String[]{
			"registerIcon: registerIcon", "resolveIcon: resolveIcon", "loadSymbolSprite: loadSymbolSprite",
			"pack: pack", "sprites: sprites", "stems: stems", "nameToStem: nameToStem"
		})
			assertTrue(body.contains(member), () -> "juneau-icons.js must export '" + member + "': " + member);
	}

	@Test void a02_theDocumentedRegistrationIsScriptTagAttributes() throws Exception {
		var body = iconsJs();
		// The primary, documented path is attributes on the icons script tag, read before first paint.
		assertTrue(body.contains("data-juneau-icon-replacement"), body);
		assertTrue(body.contains("data-juneau-icon-override"), body);
		// The equivalent JS call and its options are the second documented path.
		assertTrue(body.contains("function sprites(opts)"), body);
		assertTrue(body.contains("replacementUrl"), body);
		assertTrue(body.contains("overrideUrl"), body);
	}

	@Test void a03_layersMergeIntoOneSprite() throws Exception {
		var body = iconsJs();
		// One consolidated in-document sprite (a single id) so a <use> reference resolves to the layer winner.
		assertTrue(body.contains("juneau-symbol-sprite"), body);
		assertTrue(body.contains("function injectMerged(merged)"), body);
		assertTrue(body.contains("function fetchLayer(url, layerName)"), body);
		// The three layer names the report keys on.
		for (var layer : new String[]{"\"shipped\"", "\"replacement\"", "\"override\""})
			assertTrue(body.contains(layer), () -> "missing layer name " + layer);
	}

	@Test void a04_devModeIsExplicitAndFailuresAreDevGated() throws Exception {
		var body = iconsJs();
		assertTrue(body.contains("function isDevMode()"), body);
		assertTrue(body.contains("data-juneau-dev"), body);
		// A failed / unknown / late path warns THROUGH the dev gate - never an unconditional console.error.
		assertTrue(body.contains("function warn(msg, err)"), body);
		assertFalse(body.contains("console.error"),
			() -> "a production console.error would surface a user-facing error on a failed layer: " + body);
	}

	@Test void a05_theShippedSpriteHasTheExpectedStems() throws Exception {
		var svg = symbolsSvg();
		// The stems the name->stem map targets must actually exist in the shipped sprite (else they would silently
		// fall through to nothing).  Spot-check the ones with a non-identity author name.
		for (var stem : new String[]{
			"copy", "csv", "spreadsheet", "pdf", "print", "refresh", "toggle_column_search", "collapse_all",
			"settings", "columns", "chevronright", "chevronleft", "chevronup", "chevrondown",
			"first_page", "last_page", "more", "sort", "filter", "search", "close", "download", "edit"
		})
			assertTrue(svg.contains("id=\"juneau-sym-" + stem + "\""),
				() -> "shipped sprite is missing symbol juneau-sym-" + stem);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral harness (Node VM sandbox over the real shipped sprite)
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var iconsFile = Files.createTempFile("juneau-icons-", ".js");
		var symbolsFile = Files.createTempFile("juneau-symbols-", ".svg");
		try {
			Files.writeString(iconsFile, iconsJs(), UTF_8);
			Files.writeString(symbolsFile, symbolsSvg(), UTF_8);
			report = Json.to(runNode(harness, iconsFile, symbolsFile), Map.class);
		} finally {
			Files.deleteIfExists(iconsFile);
			Files.deleteIfExists(symbolsFile);
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
			var p = Path.of(basedir, "src/test/js/icons.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/icons.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/icons.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path iconsJs, Path symbolsSvg) throws Exception {
		var stdout = Files.createTempFile("icons-stdout-", ".json");
		var stderr = Files.createTempFile("icons-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), iconsJs.toString(), symbolsSvg.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("icons.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("icons.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or icons.cjs not found — behavioral layer skipped");
		return report;
	}

	@SuppressWarnings({
		"unchecked" // The harness emits each scenario as a nested JSON object; Json.to gives us Map<?,?>.
	})
	private static Map<String,Object> scenario(String key) {
		var s = report().get(key);
		assertInstanceOf(Map.class, s, () -> "scenario '" + key + "' missing from report: " + report());
		return (Map<String,Object>)s;
	}

	@Test void b01_harnessLoadedTheApi() {
		var r = report();
		assertBean(r, "hasIcons", "true");
		assertList((List<?>)r.get("exports"), "loadSymbolSprite", "nameToStem", "pack", "registerIcon", "resolveIcon", "sprites", "stems");
	}

	@Test void b02_shippedOnly_everyNameResolvesToTheShippedLayer() {
		var s = scenario("s_shipped");
		var layers = (Map<?,?>) s.get("layers");
		assertBean(layers, "shipped,replacement,override", "loaded,absent,absent");
		// sort / first_page / last_page / chevron directions are real shipped stems.
		assertBean(s, "w_search,w_settings,hasSearchStem,hasSort,hasFirstPage,hasLastPage,hasChevronleft",
			"shipped,shipped,true,true,true,true,true");
	}

	@Test void b03_shippedOnly_publishesTheNameToStemMapAndStemCatalog() {
		var s = scenario("s_shipped");
		var n2s = (Map<?,?>) s.get("nameToStem");
		assertBean(n2s, "content_copy,table,picture_as_pdf,manage_search", "copy,spreadsheet,pdf,toggle_column_search");
		// stems() returns the shipped stem catalog after load; it matches the injected sprite's symbol count.
		assertBean(s, "stemsCount,namesCount", "33,33");
	}

	@Test void b04_partialReplacement_replacedNamesWinAndTheRestFallThrough() {
		var s = scenario("s_repl");
		var layers = (Map<?,?>) s.get("layers");
		assertBean(layers, "shipped,replacement", "loaded,loaded");
		// The two names the partial replacement carries win; a name it omits falls through to shipped.  A stem
		// present only in the replacement layer still resolves (an app may add a role Juneau lacks).
		assertBean(s, "w_search,w_settings,w_close,hasClose,w_brandnew", "replacement,replacement,shipped,true,replacement");
	}

	@Test void b05_overrideBeatsReplacementBeatsShipped() {
		var s = scenario("s_ovr");
		assertBean((Map<?,?>) s.get("layers"), "override", "loaded");
		// Override and replacement both define `settings`; the override must win.  `search` is only in the
		// replacement, so it stays replacement.  `close` is in neither app layer, so it stays shipped.
		assertBean(s, "w_settings,w_search,w_close", "override,replacement,shipped");
	}

	@Test void b06_aFailedLayerIsSkipped_loadedOverrideStillWins() {
		var s = scenario("s_fail");
		var layers = (Map<?,?>) s.get("layers");
		assertBean(layers, "replacement,override", "failed,loaded");
		// The override that DID load still wins; a name the failed replacement would have carried uses shipped.
		// Dev mode was on for this scenario, so the failed fetch warns.
		assertBean(s, "w_settings,w_search,warned", "override,shipped,true");
	}

	@Test void b07_aNonOkLayerFetchIsAlsoTreatedAsFailed() {
		var s = scenario("s_notfound");
		assertBean(s, "replacement,w_search", "failed,shipped");
	}

	@Test void b08_resolveIcon_prefixEquivalenceMapAndUnknownWarn() {
		var s = scenario("s_resolve");
		// `search` and `juneau-sym-search` resolve to the same glyph (U3).  An app-supplied stem present in a
		// loaded layer resolves to a <use> host even without explicit reg().  An unknown name returns null
		// (caller draws nothing) and warns in dev - never the raw name (U11).
		assertBean(s, "searchEqPrefixed,searchNotNull,newStemResolved,unknownNull,unknownWarned", "true,true,true,true,true");
	}

	@Test void b09_aLateSpritesCallIsIgnoredAndWarns() {
		var s = scenario("s_late");
		// A sprites(...) after first paint must return the same report, not reload, and a late registration warns.
		assertBean(s, "sameReport,w_search_unchanged,warned", "true,replacement,true");
	}

	@Test void b10_scriptTagAttributesConfigureBeforeFirstPaint() {
		var s = scenario("s_attrs");
		// The documented path: layer URLs read from the script tag, applied on the boot-time load.
		assertBean(s, "replacement,w_search", "loaded,replacement");
	}

	@Test void c01_sortIsAUseHostResolvedBeforeTheSpriteLoadsAndTheOverrideWins() {
		// resolveIcon('sort') is called while the load is still in flight: it must already be a <use> of the merged
		// sprite's #juneau-sym-sort (no inline fallback art), identical before and after the load, and the override
		// sprite's symbol is what the merged sprite holds once loaded.
		assertBean(scenario("s_sort"),
			"useBeforeLoad,noInlineArtBeforeLoad,prefixedSame,sameMarkupAfterLoad,winnerLayer,spriteHoldsOverrideArt",
			"true,true,true,true,override,true");
	}

	@Test void c02_unknownNameInProductionDrawsNothingAndIsSilent() {
		assertBean(scenario("s_unknown_prod"), "unknownNull,silent", "true,true");
	}

	@Test void c03_stemsIsTheShippedCatalogNotTheMergedSet() {
		assertBean(scenario("s_catalog"), "stemsMatchShipped,brandnewInStems,brandnewResolves", "true,false,true");
	}

	@Test void c04_aSpritesCallFromADeferredScriptStillLandsBeforeTheBootLoad() {
		// readyState "interactive": the boot load waits for DOMContentLoaded / a timer, so the app's call is not "late".
		assertBean(scenario("s_deferred"), "bootDidNotPreempt,replacement,w_search,noLateWarn", "true,loaded,replacement,true");
	}

	@Test void c05_packDuringAnInFlightLoadDropsTheStaleLoadAndKeepsTheGateClosed() {
		assertBean(scenario("s_pack"),
			"freshInjectedOnce,staleNotInjected,staleResolvesToCurrent,stemsStillShipped,spriteSymbols,gateStaysClosed",
			"true,true,true,33,33,true");
	}

	@Test void c06_materialPackLayersOverTheShippedSpriteSoMissingStemsFallBack() {
		assertBean(scenario("s_materialOverShipped"),
			"replacementLayer,searchWinner,copyWinner,spriteHasSearch,noWarn",
			"loaded,shipped,replacement,true,true");
	}
}
