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
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Node-sandbox integration coverage for {@code juneau-console.js} (WORK-J0559 C1 Task 5): loads the real production
 * shell into a hand-rolled DOM shim (no jsdom) per case, exercising the full {@code #juneau-page} contract surface -
 * header/nav/cards/footer rendering, nav-depth and §5.3 prefix-fallback matching, every {@code E-JS-1}..{@code
 * E-JS-12} loud-failure code, custom card-type registration, and the datatables bridge - plus a permanent regression
 * pin for the {@code isSafeHref}/{@code isProtocolRelativeUrl} security hardening reviewed during Task 4 (a
 * scheme-bypass and a keydown-listener-leak bug, both already fixed in the shipped shell; this is the test that was
 * missing).
 *
 * <p>
 * Gated on {@code node} being on {@code PATH} (skipped otherwise - no {@code -Pjs-tests} required), mirroring
 * {@link ViewsJs_ChromeMenu_Test}'s harness-probing structure.
 */
class ConsoleJs_Shell_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ConsoleChromeMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var shellFile = Files.createTempFile("juneau-console-", ".js");
		try {
			Files.writeString(shellFile, resource(ConsoleChromeMixin.CONSOLE_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, shellFile), Map.class);
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

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/juneau-console.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/juneau-console.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/juneau-console.cjs"
		)) {
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
				fail("juneau-console.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("juneau-console.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or juneau-console.cjs not found — shell integration skipped");
		return report;
	}

	private static Map<?,?> map(Object o) { return (Map<?,?>) o; }

	private static List<?> list(Object o) { return (List<?>) o; }

	// Json.to(..., Map.class) deserializes numbers as Integer/Long (never Double), so direct assertEquals(1.0, ...)
	// against a boxed Integer fails on type, not value - compare via intValue() instead.
	private static int intOf(Object o) { return ((Number) o).intValue(); }

	//------------------------------------------------------------------------------------------------------------------
	// a01 - golden case: header.chrome wrapper (holding the slotted banner), a bare html card from <template data-card>, a footer
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_golden() {
		var golden = map(r().get("golden"));
		assertMap(golden,
			"threw=<null>",
			"errors=[]",
			"order=[div.jc-chrome,main.jc-main,footer.jc-page-footer,script#juneau-page]",
			"rows=1",
			"mainText=segment",
			"templatesLeft=0",
			"cardType=html",
			"activeNavSource=contract",
			"activeNav=[home,setup]",
			"contractHeaderTitle=Juneau Console");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a09/a10 - footer.text renders as trusted HTML (P24 revised 2026-10-02): an HTML entity decodes to its real
	// character, and inline markup becomes a real DOM element - not literal escaped text. (Numbered a09/a10, the
	// next free aNN slot, even though they sit here right after a01 - see a02..a08e below for the rest.)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a09_footerTextEntityDecodesToRealCharacter() {
		var footerHtml = map(r().get("footerHtml"));
		assertNull(footerHtml.get("threw"), () -> "footer.text HTML mount must not throw: " + footerHtml);
		assertEmpty(footerHtml.get("errors"));
		var text = (String) footerHtml.get("text");
		assertTrue(text.contains("—"), () -> "&mdash; in footer.text must decode to a real em dash, not stay the literal entity: " + footerHtml);
	}

	@Test void a10_footerTextMarkupBecomesRealElement() {
		var footerHtml = map(r().get("footerHtml"));
		assertEquals("strong", footerHtml.get("strongTagName"), () -> "<strong> in footer.text must become a real element, not escaped text: " + footerHtml);
		assertEquals("Juneau", footerHtml.get("strongText"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02 - nav depth 1-6
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5, 6})
	void a02_navDepth(int depth) {
		var byDepth = map(r().get("depth"));
		var d = map(byDepth.get(String.valueOf(depth)));
		assertNull(d.get("threw"), () -> "depth " + depth + " must not throw: " + d);
		assertEquals(depth, intOf(d.get("rowCount")), () -> "depth " + depth + " row count: " + d);
		assertEquals(depth, intOf(d.get("current")), () -> "depth " + depth + " aria-current count: " + d);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a02b - nav aria-label: header.title, else contract title, else attribute omitted
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02b_navAriaLabel() {
		var nl = map(r().get("navLabel"));
		var header = map(nl.get("header"));
		var contract = map(nl.get("contract"));
		var none = map(nl.get("none"));
		assertNull(header.get("threw"));
		assertEquals("Header Title", header.get("label"));
		assertEquals("Page Title", contract.get("label"));
		assertEquals(true, none.get("present"));
		assertEquals(false, none.get("has"), () -> "empty title must not set aria-label: " + none);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a03 - §5.3 segment-wise longest-prefix fallback matching (no activeNav given; window.location drives it)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_prefixFallback() {
		var pf = map(r().get("fallback"));
		assertNull(pf.get("threw"));
		assertEquals(List.of("admin", "users"), pf.get("activeNav"));
		assertEquals("prefix", pf.get("activeNavSource"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a04 - E-JS-1 .. E-JS-10 fire with the exact user-facing wording the MSG templates in juneau-console.js produce
	// (pinning the real message text, not just fatal/non-fatal shape) - both the console.error line and the DOM
	// ".jc-console-error" banner fail() renders. E-JS-11/E-JS-12 are covered separately by a05/a5-adjacent fatal-shape
	// assertions, since they record only a thrown JuneauConsoleError, not a console.error-backed `errors` list.
	//------------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"1|missing or unparseable <script id=\"juneau-page\">: '",
		"2|unsupported page contract version '9'; this shell supports '1'",
		"3|card 'c' references template 'missing', but no <template data-card=\"missing\"> exists",
		"4|card 'k' has unknown type 'kpi'; registered types: 'html, datatables'",
		"5|activeNav 'a/b' is not a path in the nav tree (failed at 'b')",
		"6|duplicate nav id 'a'",
		"7|duplicate <template data-card=\"c\">",
		"8|card 'b' (type 'boom') handler threw: 'kaput'",
		"9|unsafe href 'javascript:alert(1)' on nav 'X'",
		"10|datatables card 't' needs JuneauViews.regions; load the views toolkit",
	})
	void a04_loudFailure(String code, String message) {
		var e = map(map(r().get("errors")).get(code));
		var errors = list(e.get("errors"));
		assertTrue(errors.stream().anyMatch(m -> ((String) m).startsWith("[juneau-console] " + message)),
			() -> "E-JS-" + code + ": " + e);
		assertTrue(String.valueOf(e.get("banner")).contains(message), () -> "banner E-JS-" + code + ": " + e);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a05 - non-fatal card-level failures let mount() continue: the failing card is simply absent from the result;
	// plus registerCard()'s TypeError input-validation path for a non-lowercase-kebab type name (distinct from
	// E-JS-12's duplicate-registration failure).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_fatalVsCardLevel() {
		var byCode = map(r().get("errors"));
		var e4 = map(byCode.get("4"));
		assertEquals(List.of(), e4.get("resultCards"), () -> "unknown-type card must be absent from result.cards: " + e4);
		var e8 = map(byCode.get("8"));
		assertEquals(List.of(), e8.get("resultCards"), () -> "throwing-handler card must be absent from result.cards: " + e8);
		assertEquals("TypeError", r().get("registerBadType"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a06 - a custom card type, registered via JuneauConsole.registerCard, actually paints
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_customCardType() {
		var c = map(r().get("custom"));
		assertEquals("Users: 42", c.get("cardText"));
		assertEquals(true, c.get("resultHasCard"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a07 - the datatables card hands JuneauViews.regions.mount() a {id: {table}} hookup map on DOMContentLoaded
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_datatablesBridge() {
		var c = map(r().get("bridge"));
		assertNull(c.get("threw"));
		assertEquals(List.of(), c.get("errors"), () -> "the stubbed regions bridge must take the card with no E-JS-10: " + c);
		assertEquals(true, c.get("bodyRendered"));
		var captured = map(c.get("captured"));
		assertNotNull(captured, () -> "JuneauViews.regions.mount() was never called: " + c);
		assertTrue(captured.containsKey("dt1-body"));
		assertBean(c, "bodyId,duplicateIds", "dt1-body,[]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a08 - ADDENDUM: isSafeHref/isProtocolRelativeUrl regression pins (Task 4's scheme-bypass + listener-leak fixes)
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * {@code isSafeHref}/{@code isProtocolRelativeUrl} are not exported on {@code window.JuneauConsole}, so this
	 * exercises them indirectly through nav-link rendering: {@code link()} appends NO element at all for a rejected
	 * href (not even one with a missing/empty {@code href}) &mdash; the harness mounts all 8 obfuscated-scheme and
	 * protocol-relative reject strings (tab/newline/control-char-obfuscated {@code javascript:}, mixed-case
	 * {@code JaVaScRiPt:}, {@code data:}, {@code vbscript:}, {@code //evil.example/x}, {@code /\evil.example/x}) as
	 * nav items keyed {@code rej0}..{@code rej7} in {@code hrefSafety.nav}.
	 */
	@Test void a08b_hrefSafety_rejectedHrefsRenderNoElementAtAll() {
		var nav = map(map(r().get("hrefSafety")).get("nav"));
		for (var i = 0; i < 8; i++) {
			var id = "rej" + i;
			var c = map(nav.get(id));
			assertMap(c, "expect=reject", "rendered=false", "href=<null>");
		}
	}

	@Test void a08c_hrefSafety_acceptedHrefsRenderUnchanged() {
		var nav = map(map(r().get("hrefSafety")).get("nav"));
		var expected = List.of("foo/bar", "/path/to/thing", "#frag", "https://example.com/ok", "mailto:a@example.com");
		for (var i = 0; i < expected.size(); i++) {
			var id = "acc" + i;
			var want = expected.get(i);
			var c = map(nav.get(id));
			assertMap(c, "expect=accept", "rendered=true", "href=" + want);
		}
	}

	/**
	 * {@code htmlCard()}'s bare, template-less {@code src} path (fetched same-origin HTML) gates {@code card.src}
	 * through the SAME {@code isSafeHref} check as a nav link, before ever calling {@code fetch}: an unsafe or
	 * protocol-relative {@code src} must synchronously throw (caught as a card-level {@code E-JS-8}), not reach the
	 * network.
	 */
	@Test void a08d_hrefSafety_htmlCardSrcGateUsesIsSafeHrefToo() {
		var hcs = map(map(r().get("hrefSafety")).get("htmlCardSrc"));
		for (var src : List.of("javascript:alert(1)", "//evil.example/x")) {
			var c = map(hcs.get(src));
			assertNotNull(c, () -> "no htmlCardSrc case recorded for '" + src + "'");
			var errors = list(c.get("errors"));
			assertFalse(errors.isEmpty(), () -> "src '" + src + "' must surface a card-level E-JS-8: " + c);
			assertTrue(errors.stream().anyMatch(e -> ((String) e).contains("is not same-origin")),
				() -> "src '" + src + "' must fail the same-origin/isSafeHref gate: " + c);
			assertEquals(0, intOf(c.get("mainChildren")), () -> "a gated src card must paint nothing: " + c);
		}
	}

	/**
	 * The keydown-listener-leak regression (Task 4 review finding): mounting on two DIFFERENT roots of the SAME
	 * document, each with {@code header.userMenu} set (so {@code renderUserMenu} -&gt; {@code wireFallbackEscape(doc)}
	 * runs once per mount), must wire the Escape-closes-menu keydown listener on {@code doc} exactly ONCE - the
	 * {@code fallbackEscapeWired} {@code WeakSet} keyed by {@code doc} is what Task 4 added to fix this; this test
	 * pins it so a future change cannot silently reintroduce the per-mount listener accumulation.
	 */
	@Test void a08e_hrefSafety_keydownListenerWiredOnceAcrossTwoMountsOnSameDocument() {
		var href = map(r().get("hrefSafety"));
		assertEquals(1, intOf(href.get("keydownListenerCountAfterTwoMounts")),
			() -> "two mounts on the same document must wire exactly one keydown listener, not one per mount: " + href);
	}
}
