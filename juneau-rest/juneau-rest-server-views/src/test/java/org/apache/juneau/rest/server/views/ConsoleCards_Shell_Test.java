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
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Node-sandbox coverage for the open card registry and the {@code CardContext} members of {@code juneau-console.js}:
 * {@code console-cards.cjs} (registerCard shape and duplicate checks, the deferred {@code window.JuneauConsoleCards}
 * queue, pending cards, refresh/destroy/cardApi/cardTypes, card events) and {@code console-card-context.cjs}
 * ({@code fetchJson}, {@code every}, {@code prefs}, paint helpers, {@code onDestroy}).
 *
 * <p>
 * Gated on {@code node} being on {@code PATH} (skipped otherwise - no {@code -Pjs-tests} required).
 */
class ConsoleCards_Shell_Test extends TestBase {

	private static Map<?,?> cardsReport;
	private static Map<?,?> contextReport;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var cards = locateHarness("console-cards.cjs");
		var context = locateHarness("console-card-context.cjs");
		if (cards == null || context == null)
			return;
		var shellFile = Files.createTempFile("juneau-console-", ".js");
		try {
			try (var in = ConsoleChromeMixin.class.getResourceAsStream(ConsoleChromeMixin.CONSOLE_JS_RESOURCE)) {
				assertNotNull(in, "missing classpath resource: " + ConsoleChromeMixin.CONSOLE_JS_RESOURCE);
				Files.writeString(shellFile, new String(in.readAllBytes(), UTF_8), UTF_8);
			}
			cardsReport = Json.to(runNode(cards, shellFile), Map.class);
			contextReport = Json.to(runNode(context, shellFile), Map.class);
		} finally {
			Files.deleteIfExists(shellFile);
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
		for (var rel : List.of("src/test/js/" + name, "juneau-rest/juneau-rest-server-views/src/test/js/" + name)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path shell) throws Exception {
		var stdout = Files.createTempFile("juneau-console-stdout-", ".json");
		var stderr = Files.createTempFile("juneau-console-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), shell.toString()))
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

	private static Map<?,?> cards(String key) {
		assumeTrue(cardsReport != null, "node not available or console-cards.cjs not found - shell integration skipped");
		return (Map<?,?>)cardsReport.get(key);
	}

	private static Map<?,?> context(String key) {
		assumeTrue(contextReport != null, "node not available or console-card-context.cjs not found - shell integration skipped");
		return (Map<?,?>)contextReport.get(key);
	}

	private static Map<?,?> map(Object o) { return (Map<?,?>)o; }

	private static List<?> list(Object o) { return (List<?>)o; }

	//------------------------------------------------------------------------------------------------------------------
	// console-cards.cjs
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_queueDrainedBeforeMount() {
		var c = cards("queueBefore");
		assertNull(c.get("threw"));
		assertEquals(List.of(), c.get("errors"));
		assertEquals("alpha:a1", c.get("a1"));
		assertEquals("beta:b1", c.get("b1"));
		assertEquals(List.of("alpha", "beta", "console-output", "html", "run-view"), c.get("types"));
	}

	@Test void a02_queueDrainedAfterLoad() {
		var c = cards("queueAfter");
		assertNull(c.get("threw"));
		assertEquals(List.of(), c.get("errors"));
		assertEquals("gamma:g1", c.get("g1"));
		assertEquals(List.of("console-output", "gamma", "html", "run-view"), c.get("types"));
	}

	@Test void a03_pendingCardRendersOnRegister() {
		var c = cards("pending");
		assertEquals(Map.of("p1Pending", true, "p1Loading", true, "p1Title", true, "p3Pending", true), c.get("beforeReg"));
		assertEquals(List.of("p1", "p2"), c.get("order"));
		assertEquals("P1delta:p1", c.get("p1Rendered"));
		assertEquals(true, c.get("p1LoadingGone"));
		assertEquals(false, c.get("p1StillPending"));
		assertEquals(true, c.get("p3StillPending"));
		assertEquals(List.of("mounted", "mounted"), c.get("mountedEventKinds"));
		assertEquals(List.of(), c.get("errors"));
	}

	@Test void a04_domContentLoadedFailsStillPendingCards() {
		var c = cards("domContentLoadedPending");
		assertEquals(List.of("E-JS-4", "E-JS-10", "E-JS-10"), c.get("codes"));
		var errors = list(c.get("errors"));
		assertEquals("[juneau-console] card 'z1' has unknown type 'zeta'; registered types: 'html, console-output, run-view'", errors.get(0));
		assertEquals("[juneau-console] datatables card 't1' needs JuneauViews.regions; load the views toolkit", errors.get(1));
		assertEquals("[juneau-console] datatables card 't2' needs JuneauViews.regions; load the views toolkit", errors.get(2));
		assertEquals(List.of("z1", "t1", "t2"), c.get("failedEvents"));
	}

	@Test void a05_registerCardErrorsVerbatim() {
		var c = cards("registerErrors");
		assertEquals(Map.of("name", "JuneauConsoleError", "code", "E-JS-20", "message", "card type 'Bad_Type' must match /^[a-z][a-z0-9-]{0,31}$/"), c.get("e20"));
		assertEquals(Map.of("name", "JuneauConsoleError", "code", "E-JS-21", "message", "card type 'ok1' handler must be a function or an object with render(); got 'string'"), c.get("e21a"));
		assertEquals(Map.of("name", "JuneauConsoleError", "code", "E-JS-21", "message", "card type 'ok2' handler must be a function or an object with render(); got 'object'"), c.get("e21b"));
		var e23 = map(c.get("e23"));
		var threw = map(e23.get("threw"));
		assertEquals("E-JS-23", threw.get("code"));
		assertEquals("JuneauConsoleCards entry '[\"oops\",\"nope\",\"extra\"]' is not a [type, handler] pair", threw.get("message"));
		assertEquals("obj:o1", c.get("objFormText"));
	}

	@Test void a06_refreshCoalesces() {
		var c = cards("refreshCoalesce");
		assertEquals(1, ((Number)c.get("callsAtQueueTime")).intValue());
		assertEquals(3, ((Number)c.get("callsAfter")).intValue());
		assertEquals("r3", c.get("text"));
	}

	@Test void a07_destroyOnceThenE22() {
		var c = cards("destroyOnce");
		assertEquals("pong:w1", c.get("apiPing"));
		assertEquals(1, ((Number)c.get("destroyCount")).intValue());
		for (var k : List.of("e22", "e22b", "e22c"))
			assertEquals("E-JS-22", map(c.get(k)).get("code"), k);
		assertEquals("card 'nope-never-mounted' is not mounted", map(c.get("e22b")).get("message"));
		assertEquals(List.of(), c.get("errors"));
		assertNull(c.get("banner"));
	}

	@Test void a08_cardTypesSortedAndEventsCarryDetail() {
		var c = cards("typesAndEvents");
		assertEquals(List.of("aah", "console-output", "html", "mid", "run-view", "zed"), c.get("types"));
		var events = list(c.get("events"));
		assertEquals(3, events.size());
		assertEquals(Map.of("kind", "mounted", "id", "a", "type", "aah", "hasEl", true), events.get(0));
		assertEquals(Map.of("kind", "failed", "id", "m", "type", "mid", "hasEl", false, "hasError", true), events.get(1));
		assertEquals(Map.of("kind", "mounted", "id", "z", "type", "zed", "hasEl", true), events.get(2));
	}

	@Test void a09_cardReportedSkipsPageBanner() {
		var c = cards("cardReported");
		assertEquals(List.of(), c.get("errors"));
		assertNull(c.get("banner"));
		assertEquals(List.of("failed"), c.get("events"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// console-card-context.cjs
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_fetchJsonSupersedeAborts() {
		var c = context("supersede");
		assertEquals(List.of("/a", "/b"), c.get("seen"));
		assertEquals(Map.of("aborted", true), c.get("r1"));
		assertEquals(Map.of("v", "/b"), c.get("r2"));
	}

	@Test void b02_csrfHeaderOnNonGetOnly() {
		var calls = list(context("csrf").get("calls"));
		assertEquals(Map.of("url", "/get", "method", "GET", "headers", Map.of("Accept", "application/json")), calls.get(0));
		assertEquals(Map.of("url", "/post", "method", "POST", "headers", Map.of("Accept", "application/json", "X-My-Csrf", "tok-123")), calls.get(1));
	}

	@Test void b03_errorMessagePrecedence() {
		assertEquals(List.of("bad input", "HTTP 500", "Request failed"), contextReport == null ? null : contextReport.get("errorPrecedence"));
	}

	@Test void b04_paintLoadingThenPaintError() {
		var c = context("paint");
		assertEquals("status", c.get("loadingRole"));
		assertEquals(true, c.get("loadingThenError"));
		assertEquals("alert", c.get("errorRole"));
		assertEquals("boom", c.get("errorText"));
	}

	@Test void b05_everyPausesHiddenAndPauseWhenStopsOnDestroy() {
		var c = context("every");
		assertEquals(Map.of("afterOne", 1, "whileHidden", 1, "whilePaused", 1, "afterDestroy", 1), c);
	}

	@Test void b06_prefsNamespaceCorruptAndThrowingStorage() {
		var c = context("prefs");
		assertEquals("juneau-card:pc1:color", c.get("key"));
		assertEquals("juneau-card:pc1:cols:w", c.get("nsKey"));
		assertEquals("blue", c.get("storedValue"));
		assertEquals("dflt", c.get("corruptValue"));
		assertNull(c.get("throwingThrew"));
		assertEquals("fallback", c.get("throwingValue"));
	}

	@Test void b07_onDestroyCallbacksRun() {
		assertEquals(List.of("a", "b"), context("onDestroy").get("ran"));
	}
}
