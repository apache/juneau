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
 * Client-side coverage for the two runtime halves of a card that hosts a view and declares its own actions.
 *
 * <h5 class='section'>What is pinned here</h5>
 * <ul>
 * 	<li><b>Enhancement ownership.</b>  {@code juneau-cards.js} enhances a card's action catalog at card init.
 * 		{@code juneau-chrome.js}'s own {@code DOMContentLoaded} scan only walks app-headers and bar slots, so it never
 * 		reaches a card &mdash; and a card action needs no refresh wire, so a static card must be enhanced too.
 * 	<li><b>One popup owner.</b>  A card {@code MENU} opens on the ONE shared {@code juneau-views.js} layer stack (via
 * 		the chrome helpers that already speak it); the cards runtime defines no stack of its own.
 * 	<li><b>Card-scoped identity.</b>  A hosted table's sidecar key is the server-minted, card-qualified element id and
 * 		is resolved within the enclosing card, so two cards hosting the same authored view never cross-wire.  A table
 * 		outside a card resolves exactly what it always did.
 * 	<li><b>Hidden cards.</b>  A hosted table in a hidden card still initializes on the eager scan (a card is not a
 * 		{@code <template>}), while a refreshable sibling's poll timers stay suspended.
 * </ul>
 *
 * <p>Source-shape checks always run; the two Node harnesses run when {@code node} is on {@code PATH} (skipped
 * otherwise &mdash; no {@code -Pjs-tests} required).
 */
class ViewsJs_CardActions_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source-shape (always on)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_cardsRuntimeStandsUpNoLayerStackOfItsOwn() throws Exception {
		var body = resource(ViewsMixin.CARDS_JS_RESOURCE);
		assertFalse(body.contains("function pushLayer"), "the cards runtime must not define a layer stack");
		assertFalse(body.contains("function popLayer"), "the cards runtime must not define a layer stack");
		assertFalse(body.contains("pushLayer: pushLayer"), "the cards runtime must not export a layer stack");
	}

	@Test void a02_cardsRuntimeDelegatesActionWiringToTheChromeHelpers() throws Exception {
		var body = resource(ViewsMixin.CARDS_JS_RESOURCE);
		assertTrue(body.contains("window.JuneauChrome"), "card actions reuse the header vocabulary's wiring");
		assertTrue(body.contains("wireMenus"), "MENU wiring is delegated, never re-implemented");
		assertTrue(body.contains("wireSafeActions"), "SAFE wiring is delegated, never re-implemented");
		assertTrue(body.contains("enhanceCardActions: enhanceCardActions"), "the enhancement entry point is exported");
	}

	@Test void a03_actionAttributesMatchTheHeaderVocabulary() throws Exception {
		var body = resource(ViewsMixin.CARDS_JS_RESOURCE);
		assertTrue(body.contains("ACTION_MARKER = \"" + AppHeaderTable.ACTION_MARKER + "\""),
			"the card action marker must equal the server's header-action marker");
		// Cards delegate MENU/SAFE wiring to chrome, which owns the behavior attribute constant.
		var chrome = resource(ViewsMixin.CHROME_JS_RESOURCE);
		assertTrue(chrome.contains("BEHAVIOR_ATTR = \"" + AppHeaderTable.BEHAVIOR_ATTR + "\""),
			"the card behavior attribute must equal the server's header behavior attribute");
	}

	@Test void a04_cardScanIsPerCardNotPerRefreshableCard() throws Exception {
		// The old scan keyed on the refresh attribute, which would skip every action-only card, and on a grid
		// ancestor, which the per-card server entry point does not emit.
		var body = resource(ViewsMixin.CARDS_JS_RESOURCE);
		assertTrue(body.contains("querySelectorAll(\"[\" + CARD_MARKER + \"]\")"),
			"the DOMContentLoaded scan must select cards, not only refreshable ones");
	}

	@Test void a05_viewsRuntimeResolvesSidecarsByMintedIdScopedToTheCard() throws Exception {
		var body = resource(ViewsMixin.VIEWS_JS_RESOURCE);
		assertTrue(body.contains("CARD_MARKER = \"" + CardGridTable.CARD_MARKER + "\""),
			"the views runtime must know the card marker to scope its sidecar lookup");
		assertTrue(body.contains("viewSidecarKey: viewSidecarKey"), "the minted-key helper is exported");
		assertTrue(body.contains("findSidecarNode: findSidecarNode"), "the card-scoped lookup is exported");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral: run both node harnesses once, gated on node availability
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> actions;
	private static Map<?,?> hosted;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var renders = Files.createTempFile("juneau-renders-", ".js");
		var views = Files.createTempFile("juneau-views-", ".js");
		var chrome = Files.createTempFile("juneau-chrome-", ".js");
		var cards = Files.createTempFile("juneau-cards-", ".js");
		try {
			Files.writeString(renders, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(views, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(chrome, resource(ViewsMixin.CHROME_JS_RESOURCE), UTF_8);
			Files.writeString(cards, resource(ViewsMixin.CARDS_JS_RESOURCE), UTF_8);
			var actionsHarness = locateHarness("card-actions.cjs");
			if (actionsHarness != null)
				actions = Json.to(runNode(actionsHarness, renders, views, chrome, cards), Map.class);
			var hostedHarness = locateHarness("card-hosted-view.cjs");
			if (hostedHarness != null)
				hosted = Json.to(runNode(hostedHarness, renders, views), Map.class);
		} finally {
			Files.deleteIfExists(renders);
			Files.deleteIfExists(views);
			Files.deleteIfExists(chrome);
			Files.deleteIfExists(cards);
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
			var p = Path.of(basedir, "src/test/js/" + name);
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

	private static String runNode(Path harness, Path...args) throws Exception {
		var stdout = Files.createTempFile("card-actions-stdout-", ".json");
		var stderr = Files.createTempFile("card-actions-stderr-", ".txt");
		try {
			var cmd = new ArrayList<String>();
			cmd.add("node");
			cmd.add(harness.toString());
			for (var a : args)
				cmd.add(a.toString());
			var p = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
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

	private static Map<?,?> actions() {
		assumeTrue(actions != null, "node not available or card-actions.cjs not found — action layer skipped");
		return actions;
	}

	private static Map<?,?> hosted() {
		assumeTrue(hosted != null, "node not available or card-hosted-view.cjs not found — identity layer skipped");
		return hosted;
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Card actions (card-actions.cjs): ownership + the ONE shared layer stack
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_allThreeRuntimesLoadedIntoOneSandbox() {
		var r = actions();
		assertEquals(true, r.get("hasViews"), "juneau-views.js must publish the shared layer stack");
		assertEquals(true, r.get("hasChrome"), "juneau-chrome.js must publish the action wiring");
		assertEquals(true, r.get("hasCards"), "juneau-cards.js must publish its card runtime");
	}

	@Test void b02_cardsRuntimeIsTheEnhancementOwner() {
		var r = actions();
		// chrome's own scan walks headers/bar slots only - a card is neither, so it wires nothing here.
		assertEquals(true, r.get("chrome_scanFoundNoCards"));
		assertEquals(true, r.get("chrome_menuNotWiredByChromeScan"));
		// The card runtime's own scan is what wires the catalog.
		assertEquals(true, r.get("cards_wiredMenuTrigger"));
		assertEquals(true, r.get("cards_hydratedIcon"));
	}

	@Test void b03_cardMenuRidesTheSharedLayerStack() {
		var r = actions();
		assertEquals(true, r.get("cards_definesNoLayerStack"));
		assertEquals(true, r.get("menu_topIsMenu"));              // a kind:"menu" layer on the views stack
		assertEquals(true, r.get("menu_portalledToBody"));        // clip-free: reparented out of the card
		assertEquals(true, r.get("menu_escapedCard"));
		assertEquals(true, r.get("menu_positionFixed"));
		assertEquals("true", r.get("menu_ariaExpanded"));
		assertEquals(true, r.get("menu_notADialog"));             // never inflates the dialog-kind depth cap
	}

	@Test void b04_escapeUnwindsThroughThatSameStack() {
		var r = actions();
		assertEquals(true, r.get("menu_escClosed"));
		assertEquals("false", r.get("menu_escAriaReset"));
	}

	@Test void b05_twoCardsWithTheSameActionIdKeepSeparateMenus() {
		var r = actions();
		assertEquals(true, r.get("menu_scopedIds"));
		assertEquals("juneau-menu:g1:c1:more", r.get("menu_scopeA"));
		assertEquals("juneau-menu:g1:c2:more", r.get("menu_scopeB"));
		assertEquals(true, r.get("menu_secondCardOpensItsOwnList"));
	}

	@Test void b06_safeCardActionDispatchesTheSharedHostEvent() {
		var r = actions();
		assertEquals(1L, ((Number)r.get("safe_eventCount")).longValue());
		assertEquals("card-pin", r.get("safe_token"));
		assertEquals("pin", r.get("safe_actionId"));
		assertEquals(true, r.get("safe_rootIsTheCard"));
		assertEquals(true, r.get("safe_bubbles"));                // the host listens on an ancestor, so it must bubble
	}

	@Test void b07_actionOnlyAndGridlessCardsAreStillEnhanced() {
		var r = actions();
		assertEquals(true, r.get("static_cardEnhanced"));         // no refresh wire is needed to have actions
		assertEquals(true, r.get("gridless_cardEnhanced"));       // the per-card emit path mints no grid
		assertEquals(true, r.get("gridless_menuOpensOnSharedStack"));
	}

	@Test void b08_contractMismatchWithholdsActionWiringToo() {
		var r = actions();
		assertEquals(true, r.get("stale_notEnhanced"));
		assertEquals(true, r.get("stale_bannerShown"));
	}

	@Test void b09_hiddenCardStillInitsItsHostedTableWhilePollTimersSuspend() {
		var r = actions();
		assertEquals(true, r.get("hidden_cardIsHidden"));
		assertEquals(true, r.get("hidden_pollSuspended"));        // the refreshable sibling's timers stay off
		assertEquals(true, r.get("hidden_hostedTableInited"));    // a card is not a <template>: the table is live
		assertEquals("/data/hidden", r.get("hidden_hostedTableReadItsOwnSidecar"));
		assertEquals("orders", r.get("hidden_markerStaysAuthorId"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Hosted-view identity (card-hosted-view.cjs): minted key, card-scoped resolution, no cross-wiring
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_sidecarKeyIsTheMintedIdWhileTheMarkerStaysTheAuthorId() {
		var r = hosted();
		assertEquals(true, r.get("hasInit"));
		assertEquals("g1:c1:orders", r.get("key_card1"));
		assertEquals("g1:c2:orders", r.get("key_card2"));
		assertEquals("orders", r.get("marker_card1"));
		assertEquals("orders", r.get("marker_card2"));
	}

	@Test void c02_resolutionIsScopedToTheEnclosingCard() {
		var r = hosted();
		assertEquals(true, r.get("scoped_card1InsideCard1"));
		assertEquals(true, r.get("scoped_card2InsideCard2"));
		assertEquals(true, r.get("scoped_card1NotInCard2"));
	}

	@Test void c03_twoCardsSharingAViewIdDoNotCrossWire() {
		var r = hosted();
		assertEquals("/data/one", r.get("init_card1DataUrl"));
		assertEquals("/data/two", r.get("init_card2DataUrl"));
		assertEquals(true, r.get("init_neitherReadDecoy"));       // a page-level same-author-id sidecar is not consulted
		assertEquals(true, r.get("init_separateContexts"));
	}

	@Test void c04_perCardEmitPathOmitsTheGridFromTheMint() {
		var r = hosted();
		assertEquals("solo:orders", r.get("key_lone"));
		assertEquals("/data/solo", r.get("init_loneDataUrl"));
	}

	@Test void c05_aTableOutsideACardResolvesExactlyWhatItAlwaysDid() {
		var r = hosted();
		assertEquals("events", r.get("key_plain"));
		assertEquals("/data/events", r.get("init_plainDataUrl"));
		assertEquals("legacy", r.get("key_idlessFallsBackToMarker"));
		assertEquals("/data/legacy", r.get("init_idlessDataUrl"));
	}

	@Test void c06_missingCardScopedSidecarFailsClosed() {
		var r = hosted();
		assertEquals(true, r.get("init_missingSidecarRefused"));   // never a silent fallback to a foreign sidecar
	}
}
