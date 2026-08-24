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
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the {@code juneau-chrome.js} page-chrome runtime.  Source-shape + contract lockstep always
 * run; the behavioral Node harness ({@code header.cjs}) runs when {@code node} is on {@code PATH} (skipped otherwise
 * &mdash; no {@code -Pjs-tests} required).
 *
 * <p>
 * Pins the TODO-445m locked rules on the client side: two <b>distinct</b> baked contract constants
 * ({@code JUNEAU_HEADER_CONTRACT_VERSION}/{@code JUNEAU_BAR_CONTRACT_VERSION}) kept in lockstep with the server bean
 * constants, {@code window.JuneauChrome} namespacing, a thin {@code pushLayer} <b>forward</b> to
 * {@code window.JuneauViews.init.pushLayer} (never a competing definition &mdash; m3 B), textContent-only count
 * apply, same-origin/non-templated refresh endpoints, format-validated SAFE tokens (L11), and no poller.
 */
class ViewsJs_Chrome_Test extends TestBase {

	private static String chromeJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.CHROME_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.CHROME_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source-shape + contract lockstep (always on)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_namespaceIsChromeNotViews() throws Exception {
		var body = chromeJs();
		assertTrue(body.contains("window.JuneauChrome"), "runtime must be namespaced as window.JuneauChrome");
	}

	@Test void a02_bakedContractsMatchServerBeans_andAreDistinct() throws Exception {
		var body = chromeJs();
		assertTrue(body.contains("JUNEAU_HEADER_CONTRACT_VERSION = \"" + AppHeaderDef.CONTRACT_VERSION + "\""),
			"baked header contract must equal AppHeaderDef.CONTRACT_VERSION");
		assertTrue(body.contains("JUNEAU_BAR_CONTRACT_VERSION = \"" + BarSlot.CONTRACT_VERSION + "\""),
			"baked bar contract must equal BarSlot.CONTRACT_VERSION");
		assertEquals(AppHeaderDef.CONTRACT_VERSION, ViewsMixin.HEADER_CONTRACT_VERSION);
		assertEquals(BarSlot.CONTRACT_VERSION, ViewsMixin.BAR_CONTRACT_VERSION);
		// Header and bar envelopes are separately versionable; neither is aliased to the VIEW_META contract.
		assertNotSame(ViewsMixin.CONTRACT_VERSION, ViewsMixin.HEADER_CONTRACT_VERSION);
		assertNotSame(ViewsMixin.CONTRACT_VERSION, ViewsMixin.BAR_CONTRACT_VERSION);
	}

	@Test void a03_pushLayerIsForwardNotCompetingDefinition() throws Exception {
		var body = chromeJs();
		// m3 B: must CALL window.JuneauViews.init.pushLayer (thin forward), never define its own layer stack.
		assertTrue(body.contains("window.JuneauViews.init.pushLayer") || body.contains("views.pushLayer"),
			"pushLayer must forward to the shared views layer manager");
		assertFalse(body.contains("popupLayerStack"), "chrome.js must not carry a competing layer stack");
	}

	@Test void a04_noPoller_countApplyIsTextContentOnly() throws Exception {
		var body = chromeJs();
		assertFalse(body.contains("setInterval"), "L11: no poller in the page-chrome runtime");
		assertTrue(body.contains("b.textContent = clampCount("), "counts painted via textContent, never innerHTML");
	}

	@Test void a05_menusWaitOn445h_noFakeDisclosure() throws Exception {
		var body = chromeJs();
		// The runtime never opens a menu (server emits MENU triggers disabled); no <details>/role=menu construction.
		assertFalse(body.contains("role=\"menu\""), "no fake role=menu construction");
		assertFalse(body.contains("createElement(\"details\")"), "no fake <details> disclosure");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Behavioral (header.cjs), gated on node availability
	//------------------------------------------------------------------------------------------------------------------

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness("header.cjs");
		if (harness == null)
			return;
		var chromeFile = Files.createTempFile("juneau-chrome-", ".js");
		try {
			Files.writeString(chromeFile, chromeJs(), UTF_8);
			report = Json.to(runNode(harness, chromeFile), Map.class);
		} finally {
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

	private static String runNode(Path harness, Path chromeJs) throws Exception {
		var stdout = Files.createTempFile("chrome-stdout-", ".json");
		var stderr = Files.createTempFile("chrome-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), chromeJs.toString()))
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

	private static Map<?,?> r() {
		assumeTrue(report != null, "node not available or header.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_contractConstants() {
		var r = r();
		assertEquals("1", r.get("headerContract"));
		assertEquals("1", r.get("barContract"));
		assertEquals("1", r.get("nsHeaderContract"));
		assertEquals("1", r.get("nsBarContract"));
	}

	@Test void b02_isSafeChromeEndpoint_sameOriginNonTemplated() {
		var r = r();
		assertEquals(true, r.get("ep_pathOk"));
		assertEquals(true, r.get("ep_relativeOk"));
		assertEquals(false, r.get("ep_templated"));     // a chrome region is not row-scoped: {id} rejected
		assertEquals(false, r.get("ep_absolute"));
		assertEquals(false, r.get("ep_protoRel"));
		assertEquals(false, r.get("ep_scheme"));
		assertEquals(false, r.get("ep_js"));
		assertEquals(false, r.get("ep_dotdot"));
		assertEquals(false, r.get("ep_empty"));
	}

	@Test void b03_isSafeToken_matchesServerPattern() {
		var r = r();
		assertEquals(true, r.get("tok_ok"));
		assertEquals(true, r.get("tok_single"));
		assertEquals(false, r.get("tok_upper"));
		assertEquals(false, r.get("tok_leadingDigit"));
		assertEquals(false, r.get("tok_space"));
		assertEquals(false, r.get("tok_tooLong"));      // > 64 chars
	}

	@Test void b04_envelopeHandshakeAndClamp() {
		var r = r();
		assertEquals(true, r.get("env_ok"));
		assertEquals(false, r.get("env_bad"));
		assertEquals(false, r.get("env_missing"));
		assertEquals(false, r.get("env_null"));
		assertEquals("5", r.get("clamp_under"));
		assertEquals("99+", r.get("clamp_over"));
		assertEquals("7", r.get("clamp_noMax"));
		assertNum(3, r.get("badges_extract"));
		assertEquals("{}", r.get("badges_emptyIsObj"));
	}

	@Test void b05_iconHydration_singleSink_registryOnly() {
		var r = r();
		assertEquals("<svg>refresh</svg>", r.get("icon_injected"));   // resolved from juneau-icons.js registry
		assertEquals(true, r.get("icon_unknownUntouched"));           // unknown name -> no innerHTML write
	}

	@Test void b06_applyCounts_namespacedClampedTextContentOnly() {
		var r = r();
		assertEquals("99+", r.get("count_clamped"));      // 120 > max 99, re-clamped client-side
		assertEquals("4", r.get("count_plain"));
		assertEquals("keep", r.get("count_unknownUntouched"));   // non-number / unmapped id left untouched
	}

	@Test void b07_safeAction_dispatchesCustomEvent_malformedRefused() {
		var r = r();
		assertEquals(true, r.get("safe_dispatched"));
		assertEquals(true, r.get("safe_bubbles"));
		assertEquals(true, r.get("safe_prevented"));
		assertEquals(true, r.get("safe_malformedNotWired"));   // a hand-edited malformed token is refused, not wired
	}

	@Test void b08_avatarImageFallbackToInitials() {
		var r = r();
		assertEquals(true, r.get("avatar_imgHidden"));
		assertEquals(true, r.get("avatar_initialsShown"));
	}

	@Test void b09_pushLayerForwardsElseNoop() {
		var r = r();
		assertEquals(true, r.get("push_forwarded"));   // forwards to window.JuneauViews.init.pushLayer when present
		assertEquals(true, r.get("push_noop"));        // else a null no-op (never a competing layer manager)
	}

	@Test void b10_initHeaderHandshake() {
		var r = r();
		assertEquals(true, r.get("init_mismatchNull"));       // wrong sidecar contract -> refuse to enhance
		assertEquals(true, r.get("init_mismatchNoApply"));
		assertEquals(true, r.get("init_okCtl"));
		assertEquals("50", r.get("init_okApplied"));          // initial counts applied from the sidecar
	}

	@Test void b11_demandRefreshHandshake_noStaleOnMismatch() {
		var r = r();
		assertEquals(true, r.get("refresh_okReturn"));
		assertEquals("99+", r.get("refresh_okApplied"));
		assertNum(1, r.get("refresh_okFetched"));
		assertEquals("same-origin|no-store|GET", r.get("refresh_fetchOpts"));
		assertEquals(false, r.get("refresh_mismatchReturn"));
		assertEquals(true, r.get("refresh_mismatchNoApply"));   // a bad envelope never paints stale/foreign data
		assertEquals(false, r.get("refresh_unsafeReturn"));
		assertEquals(true, r.get("refresh_unsafeNoFetch"));     // unsafe endpoint -> no fetch at all
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}
}
