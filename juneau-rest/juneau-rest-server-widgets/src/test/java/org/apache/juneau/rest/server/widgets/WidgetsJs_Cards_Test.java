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
package org.apache.juneau.rest.server.widgets;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Always-on coverage for the {@code juneau-cards.js} card-layout runtime owned by this module.
 * Source-shape always runs; the two behavioral Node harnesses ({@code cards.cjs} pure helpers,
 * {@code cards-browser.cjs} DOM binding) run when {@code node} is on {@code PATH} (skipped otherwise
 * &mdash; no {@code -Pjs-tests} required).
 */
class WidgetsJs_Cards_Test extends TestBase {

	private static String cardsJs() throws IOException {
		try (var in = WidgetsMixin.class.getResourceAsStream(WidgetsMixin.CARDS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + WidgetsMixin.CARDS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source-shape (always on): NS.init export surface + the baked contract version
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_helpersExportedOnNsInit() throws Exception {
		var body = cardsJs();
		for (var name : new String[]{
			"JUNEAU_CARDS_CONTRACT_VERSION: JUNEAU_CARDS_CONTRACT_VERSION",
			"MIN_POLL_INTERVAL_MS: MIN_POLL_INTERVAL_MS",
			"clampPollInterval: clampPollInterval",
			"formatStalenessAge: formatStalenessAge",
			"scalarFieldValue: scalarFieldValue",
			"isSafeCardEndpoint: isSafeCardEndpoint",
			"envelopeContractOk: envelopeContractOk",
			"nextPollDelay: nextPollDelay",
			"fillCardFields: fillCardFields",
			"showCardBanner: showCardBanner",
			"isElementHidden: isElementHidden",
			"renderStatus: renderStatus",
			"initCard: initCard",
			"observeGrid: observeGrid",
			"initAll: initAll"
		})
			assertTrue(body.contains(name), () -> "missing export '" + name + "'");
	}

	@Test void a02_namespaceIsCardsNotViews() throws Exception {
		var body = cardsJs();
		assertTrue(body.contains("window.JuneauCards"), "runtime must be namespaced as window.JuneauCards");
		// A refresh-envelope re-fill NEVER assigns innerHTML from server/wire data - textContent only (S-escaping).
		assertTrue(body.contains("slot.textContent ="), "field fill must be textContent-only");
	}

	@Test void a03_bakedContractMatchesCardModel() throws Exception {
		var body = cardsJs();
		assertTrue(body.contains("JUNEAU_CARDS_CONTRACT_VERSION = \"" + CardFieldList.CONTRACT_VERSION + "\""),
			"baked card contract must equal CardFieldList.CONTRACT_VERSION");
		assertEquals(CardFieldList.CONTRACT_VERSION, WidgetsMixin.CARDS_CONTRACT_VERSION);
	}

	@Test void a04_hiddenCheckKnowsPagesTabPanels() throws Exception {
		// The poll-teardown path must recognize the juneau-pages.js tab-hide markers (an inactive .jc-panel/.jc-subpanel
		// that lacks .jc-active is CSS-hidden above the grid).  A light wiring pin - not a whole-file string match.
		var body = cardsJs();
		assertTrue(body.contains("jc-panel"), "hidden-check must recognize the pages tab panel class");
		assertTrue(body.contains("jc-subpanel"), "hidden-check must recognize the pages tab subpanel class");
		assertTrue(body.contains("jc-active"), "hidden-check must recognize the pages tab active marker");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral: run both node harnesses once, gated on node availability
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> pure;
	private static Map<?,?> dom;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var cardsFile = Files.createTempFile("juneau-cards-", ".js");
		try {
			Files.writeString(cardsFile, cardsJs(), UTF_8);
			var pureHarness = locateHarness("cards.cjs");
			if (pureHarness != null)
				pure = Json.to(runNode(pureHarness, cardsFile), Map.class);
			var domHarness = locateHarness("cards-browser.cjs");
			if (domHarness != null)
				dom = Json.to(runNode(domHarness, cardsFile), Map.class);
		} finally {
			Files.deleteIfExists(cardsFile);
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
			"juneau-rest/juneau-rest-server-widgets/src/test/js/" + name
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path cardsJs) throws Exception {
		var stdout = Files.createTempFile("cards-stdout-", ".json");
		var stderr = Files.createTempFile("cards-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), cardsJs.toString()))
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

	private static Map<?,?> pure() {
		assumeTrue(pure != null, "node not available or cards.cjs not found — pure-helper layer skipped");
		return pure;
	}

	private static Map<?,?> dom() {
		assumeTrue(dom != null, "node not available or cards-browser.cjs not found — DOM layer skipped");
		return dom;
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pure helpers (cards.cjs)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_contractAndPollFloor() {
		var r = pure();
		assertEquals("1", r.get("contractVersion"));
		assertEquals("1", r.get("nsContractVersion"));
		assertNum(5000, r.get("minPoll"));
	}

	@Test void b02_clampPollInterval() {
		var r = pure();
		assertNum(5000, r.get("clamp_below"));    // below floor -> floor
		assertNum(5000, r.get("clamp_at"));
		assertNum(30000, r.get("clamp_above"));
	}

	@Test void b03_formatStalenessAge() {
		var r = pure();
		assertEquals("just now", r.get("age_now"));
		assertEquals("5s ago", r.get("age_secs"));
		assertEquals("2m ago", r.get("age_mins"));
		assertEquals("1h ago", r.get("age_hours"));
	}

	@Test void b04_scalarFieldValue() {
		var r = pure();
		assertEquals("hi", r.get("scalar_str"));
		assertEquals("7", r.get("scalar_num"));
		assertEquals("true", r.get("scalar_bool"));
		assertEquals("", r.get("scalar_null"));
		assertEquals("", r.get("scalar_obj"));    // objects/arrays never render [object Object]
		assertEquals("", r.get("scalar_arr"));
	}

	@Test void b05_isSafeCardEndpoint_sameOriginAndNonTemplated() {
		var r = pure();
		assertEquals(true, r.get("ep_pathOk"));
		assertEquals(true, r.get("ep_relativeOk"));
		assertEquals(false, r.get("ep_templated"));      // a field-list is NOT row-scoped: {id} rejected (not row-detail's rule)
		assertEquals(false, r.get("ep_templatedAny"));
		assertEquals(false, r.get("ep_absolute"));
		assertEquals(false, r.get("ep_protoRel"));
		assertEquals(false, r.get("ep_scheme"));
		assertEquals(false, r.get("ep_js"));
		assertEquals(false, r.get("ep_dotdot"));
		assertEquals(false, r.get("ep_leadingDotdot"));
		assertEquals(false, r.get("ep_empty"));
	}

	@Test void b06_envelopeContractOk() {
		var r = pure();
		assertEquals(true, r.get("env_ok"));
		assertEquals(false, r.get("env_bad"));
		assertEquals(false, r.get("env_missing"));
		assertEquals(false, r.get("env_null"));
	}

	@Test void b07_nextPollDelay_clampPlusBoundedJitter() {
		var r = pure();
		assertNum(30000, r.get("delay_zeroJitter"));
		assertEquals(true, r.get("delay_maxJitterInRange"));    // in [interval, interval+POLL_JITTER_MS)
		assertNum(5000, r.get("delay_clampsBelowFloor"));
	}

	@Test void b08_fillCardFields_textContentOnly_xssNeverInterpreted() {
		var r = pure();
		assertEquals(true, r.get("fill_xssNotInterpreted"));
		assertTrue(String.valueOf(r.get("fill_xss")).contains("<img"));   // stored verbatim as text, not parsed
		assertEquals("42", r.get("fill_num"));
		assertEquals("", r.get("fill_missing"));                // a key absent from the envelope clears to ""
	}

	//------------------------------------------------------------------------------------------------------------------
	// DOM binding (cards-browser.cjs)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_refreshWiring_fetchOptionsAndFieldFill() {
		var r = dom();
		assertEquals(true, r.get("a_ctl"));
		assertEquals("Refresh Live", r.get("a_btnAria"));       // aria-label composed from the card title
		assertEquals(true, r.get("a_iconInjected"));            // glyph resolved from juneau-icons.js registry
		assertEquals(true, r.get("a_iconResolved"));
		assertEquals("status", r.get("a_statusRole"));
		assertNum(1, r.get("a_fetchCount"));
		assertEquals("/data/summary", r.get("a_fetchUrl"));
		assertEquals(true, r.get("a_cacheNoStore"));            // cache:"no-store"
		assertEquals("same-origin", r.get("a_credentials"));
		assertEquals("GET", r.get("a_method"));
		assertEquals("FRESH", r.get("a_fieldFilled"));
		assertEquals(true, r.get("a_ariaBusyCleared"));
	}

	@Test void c02_contractMismatch_bannerAndRefuse_noFetch() {
		var r = dom();
		assertEquals(true, r.get("b_noCtl"));
		assertEquals(true, r.get("b_bannerShown"));
		assertEquals(true, r.get("b_bannerText"));
		assertEquals(true, r.get("b_noFetch"));
	}

	@Test void c03_unsafeEndpoint_clientRecheckRefuses_noFetch() {
		var r = dom();
		assertEquals(true, r.get("c_noCtl"));
		assertEquals(true, r.get("c_bannerShown"));
		assertEquals(true, r.get("c_noFetch"));
	}

	/**
	 * A bad envelope never paints stale/foreign data (wire-contract mismatch), a static card is never enhanced
	 * (no fetch), and {@code isElementHidden} correctly walks ancestors - three independent DOM-layer checks that
	 * each assert exactly two boolean flags from the harness result.
	 */
	@ParameterizedTest
	@MethodSource("c04_domTwoTrueFlagsProvider")
	void c04_domResultHasTwoTrueFlags(String key1, String key2) {
		var r = dom();
		assertEquals(true, r.get(key1));
		assertEquals(true, r.get(key2));
	}

	static Stream<Arguments> c04_domTwoTrueFlagsProvider() {
		return Stream.of(
			Arguments.of("d_fieldUnchanged", "d_statusError"),
			Arguments.of("e_noCtl", "e_noFetch"),
			Arguments.of("h_hiddenViaAncestor", "h_shown"));
	}

	@Test void c06_concurrentClicksCoalesce() {
		var r = dom();
		assertEquals(true, r.get("f_secondDropped"));
		assertNum(1, r.get("f_fetchCount"));
	}

	@Test void c07_pollTeardownAndRestartViaObserver() {
		var r = dom();
		assertEquals(true, r.get("g_startedRunning"));
		assertEquals(true, r.get("g_observerInstalled"));
		assertEquals(true, r.get("g_stoppedWhenHidden"));       // hidden card -> timers stop
		assertEquals(true, r.get("g_restartedWhenShown"));      // re-shown -> timers restart
	}

	@Test void c09_pagesTabHide_inactivePanelStopsAndRestartsPoll() {
		// The real juneau-pages.js hide path: a card inside an ancestor .jc-panel that lacks .jc-active is CSS-hidden,
		// and pages toggles .jc-active on that ancestor panel (never the card subtree).  isElementHidden must see it,
		// the poll must not start there, and the observer must stop/restart timers as .jc-active toggles.
		var r = dom();
		assertEquals(true, r.get("i_hiddenInInactivePanel"));       // ancestor .jc-panel without .jc-active => hidden
		assertEquals(true, r.get("i_notStartedInInactivePanel"));   // poll never starts under a hidden tab
		assertEquals(true, r.get("i_observerInstalled"));
		assertEquals(true, r.get("i_startedWhenActivated"));        // pages adds .jc-active -> timers restart
		assertEquals(true, r.get("i_shownWhenActive"));
		assertEquals(true, r.get("i_stoppedWhenDeactivated"));      // pages removes .jc-active -> timers stop
	}
}
