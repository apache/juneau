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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The sectioned-dialog half of the module's <b>JavaScript-execution harness</b>: runs the REAL served
 * {@code juneau-views.js} <b>and</b> {@code juneau-views.css} in a real headless browser and asserts a
 * {@code FormDef.sections} dialog behaves as a user experiences it &mdash; exactly one section renders, a value typed
 * into one survives a round trip through another, an inline error stays inside the section that owns it, confirm
 * reveals the offending section before focusing it, Tab runs ribbon-then-body without leaking into a hidden section,
 * and Escape closes the whole <b>dialog</b> through the shared layer stack while restoring focus to the invoker.
 *
 * <h5 class='section'>Why this exists (beyond the always-on source-shape + Node harness tests):</h5>
 * <p>
 * {@link ViewsJs_RibbonStrip_Test} proves the shipped script builds the ribbon and hides the right panes under a DOM
 * shim, where {@code hidden} is just a property.  In a real browser it is a cascade question, and the pane rule is
 * {@code display: flex} &mdash; an <b>author</b> rule that outranks the UA stylesheet's
 * {@code [hidden] { display: none }}.  Only a real browser with the real stylesheet can prove an unselected section is
 * genuinely not rendered rather than merely flagged, and only a real browser proves the focus trap, real Tab order and
 * real Escape.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does.  It reuses that profile's provisioned Node + Playwright browser and derives its own prober
 * ({@code sectioned-dialog-browser.cjs}) from the profile's {@code juneau.jsTests.harness} directory, so no pom change
 * is needed.
 */
@EnabledIfSystemProperty(named=SectionedDialog_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class SectionedDialog_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent()
			.resolve("sectioned-dialog-browser.cjs");

		// The CSS is load-bearing here, not decoration: the pane's author-level `display: flex` competes with the UA
		// `[hidden]` rule, so a fixture without the real stylesheet would prove nothing about the shipped contract.
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>\n"
			+ resource(ViewsMixin.VIEWS_CSS_RESOURCE)
			+ "\n</style></head><body>\n<script>\n"
			+ resource(ViewsMixin.RENDERS_JS_RESOURCE)
			+ "\n</script>\n<script>\n"
			+ resource(ViewsMixin.VIEWS_JS_RESOURCE)
			+ "\n</script></body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("sectioned-dialog.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("sectioned-dialog-stdout.json");
		var stderr = dir.resolve("sectioned-dialog-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));

		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> sub(String key) {
		return (Map<String,Object>) report.get(key);
	}

	private static long num(Map<String,Object> m, String key) {
		var v = m.get(key);
		assertInstanceOf(Number.class, v, () -> "expected a number at '" + key + "', got: " + v);
		return ((Number) v).longValue();
	}

	@Test void a01_runtimeLoadedWithoutErrors() {
		assertEquals(Boolean.TRUE, report.get("hasInit"), () -> "juneau-views.js did not populate JuneauViews.init: " + report);
		assertEquals(List.of(), report.get("jsFailures"), () -> "the runtime logged errors: " + report.get("jsFailures"));
	}

	@Test void b01_exactlyOneSectionRenders_andTheRealCssHonorsHidden() {
		var p = sub("paint");
		assertEquals(Boolean.TRUE, p.get("stripVisible"), () -> report.toString());
		assertEquals("tablist", p.get("stripRole"), () -> report.toString());
		assertEquals(2L, num(p, "tabCount"), () -> report.toString());
		assertEquals("Basics,Advanced", p.get("tabLabels"), () -> report.toString());
		assertEquals(2L, num(p, "paneCount"), () -> report.toString());
		assertEquals("flex", p.get("pane0Display"), () -> report.toString());
		assertEquals("none", p.get("pane1Display"),
			() -> "the author-level `display: flex` beat `[hidden]` - every section would render at once: " + report);
		assertEquals(Boolean.TRUE, p.get("pane0Shown"), () -> report.toString());
		assertEquals(Boolean.FALSE, p.get("pane1Shown"), () -> report.toString());
		assertEquals("true,false", p.get("ariaSelected"), () -> report.toString());
	}

	@Test void b02_aSectionedDialogIsStillExactlyOneDialog() {
		var p = sub("paint");
		assertEquals(1L, num(p, "dialogLayers"),
			() -> "the ribbon is layout on the dialog surface and must not push a layer: " + report);
		assertEquals(1L, num(p, "backdropCount"), () -> report.toString());
		assertEquals(1L, num(p, "afterSwitch_dialogLayers"),
			() -> "switching sections must not open or close a layer: " + report);
	}

	@Test void b03_clickingATabSwapsWhichSectionRenders() {
		var p = sub("paint");
		assertEquals(Boolean.FALSE, p.get("afterSwitch_pane0Shown"), () -> report.toString());
		assertEquals(Boolean.TRUE, p.get("afterSwitch_pane1Shown"), () -> report.toString());
		assertEquals("false,true", p.get("afterSwitch_ariaSelected"), () -> report.toString());
	}

	@Test void c01_valuesSurviveARoundTripThroughAnotherSection() {
		var v = sub("values");
		assertEquals(Boolean.TRUE, v.get("titleKept"), () -> "a typed text value was lost on a section switch: " + report);
		assertEquals(Boolean.TRUE, v.get("sevKept"), () -> "a select choice was lost on a section switch: " + report);
		assertEquals(Boolean.TRUE, v.get("notesKept"), () -> "a typed textarea value was lost on a section switch: " + report);
		assertEquals(Boolean.TRUE, v.get("sameTitleNode"),
			() -> "panes must be hidden, never re-painted (re-painting is how values get lost): " + report);
		assertEquals(Boolean.TRUE, v.get("sameNotesNode"), () -> report.toString());
		// Collection spans every section, including the hidden one, and keeps the boolean-string convention.
		var collected = String.valueOf(v.get("collected"));
		assertTrue(collected.contains("\"title\":\"Typed in Basics\""), () -> collected);
		assertTrue(collected.contains("\"sev\":\"critical\""), () -> collected);
		assertTrue(collected.contains("\"notes\":\"Typed in Advanced\""),
			() -> "a hidden section's value must still be submitted: " + collected);
		assertTrue(collected.contains("\"notify\":\"false\""), () -> collected);
	}

	@Test void d01_confirmIsBlockedAndTheErrorStaysInItsOwnSection() {
		var i = sub("invalid");
		assertEquals(Boolean.TRUE, i.get("submitBlocked"),
			() -> "a required control empty in a HIDDEN section must still block submit: " + report);
		assertEquals(Boolean.TRUE, i.get("dialogStillOpen"), () -> report.toString());
		assertEquals(Boolean.TRUE, i.get("notesAriaInvalid"), () -> report.toString());
		assertEquals(Boolean.TRUE, i.get("notesErrorInOwnSection"), () -> report.toString());
		assertEquals(Boolean.TRUE, i.get("notesErrorNotInOtherSection"),
			() -> "an error must never be painted into a section that does not own the control: " + report);
		assertEquals(Boolean.TRUE, i.get("titleErrorEmpty"),
			() -> "the valid section's error sibling must stay empty: " + report);
	}

	@Test void d02_confirmRevealsTheOffendingSectionBeforeFocusingIt() {
		var i = sub("invalid");
		assertEquals(Boolean.TRUE, i.get("offendingSectionRevealed"), () -> report.toString());
		assertEquals("false,true", i.get("ariaSelectedAfterReveal"),
			() -> "the ribbon must follow the revealed section: " + report);
		assertEquals(Boolean.TRUE, i.get("focusOnFirstInvalid"), () -> report.toString());
		assertEquals(Boolean.TRUE, i.get("focusIsVisible"),
			() -> "focus landed on a control the user cannot see: " + report);
	}

	@Test void e01_tabOrderRunsRibbonThenBody_andSkipsHiddenSections() {
		var f = sub("focus");
		assertEquals(1L, num(f, "stripTabsInSequence"),
			() -> "a roving tabindex means exactly ONE ribbon tab is in the Tab sequence: " + report);
		assertEquals(Boolean.TRUE, f.get("stripPrecedesBody"),
			() -> "the ribbon must come before the form body in the Tab order: " + report);
		// The ribbon tab, then the visible section's pane (role=tabpanel is itself tabbable), then that section's
		// controls, then the dialog's own buttons.  The hidden section contributes nothing.
		assertEquals("basics,pane:basics,title,sev,juneau-view-dialog-cancel,juneau-view-dialog-confirm",
			f.get("tabbableOrder"), () -> "unexpected real Tab order: " + report);
		assertEquals(0L, num(f, "hiddenSectionControlsReachable"),
			() -> "a hidden section's controls must not be reachable by Tab: " + report);
		assertEquals(0L, num(f, "hiddenPanesReachable"),
			() -> "a hidden pane must not be reachable by Tab despite its role=tabpanel tabindex: " + report);
	}

	/**
	 * The focus trap only intercepts Tab at the two boundaries and lets the browser walk the middle, so the boundary
	 * elements themselves have to be rendered.  If a hidden section's control were first or last, a Tab wrap would
	 * try to focus something invisible and focus would silently go nowhere.
	 */
	@Test void e01b_theTrapsBoundaryElementsAreThemselvesRendered() {
		var f = sub("focus");
		assertEquals(Boolean.TRUE, f.get("trapFirstIsRendered"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("trapLastIsRendered"), () -> report.toString());
	}

	@Test void e02_focusTrapAndRibbonArrowKeysWorkForReal() {
		var f = sub("focus");
		assertEquals(Boolean.TRUE, f.get("focusTrappedIntoDialog"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("tabWrapsToFirst"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("tabKeepsFocusInDialog"), () -> "Tab escaped the trapping layer: " + report);
		assertEquals(Boolean.TRUE, f.get("arrowMovesSelection"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("arrowMovesFocus"), () -> report.toString());
		assertEquals(Boolean.TRUE, f.get("arrowRevealsPane"), () -> report.toString());
	}

	@Test void f01_escapeClosesTheDialogNotTheSection_andRestoresFocus() {
		var e = sub("escape");
		assertEquals(1L, num(e, "openedLayers"), () -> report.toString());
		assertEquals(Boolean.TRUE, e.get("secondSectionShowing"), () -> report.toString());
		assertEquals(0L, num(e, "layersAfterEscape"),
			() -> "Escape must close the DIALOG through the shared stack, not merely the section: " + report);
		assertEquals(0L, num(e, "backdropsAfterEscape"), () -> report.toString());
		assertEquals(Boolean.TRUE, e.get("dialogGone"), () -> report.toString());
		assertEquals(Boolean.TRUE, e.get("focusRestoredToInvoker"),
			() -> "focus-restore still comes from the shared layer stack: " + report);
	}

	@Test void g01_anActionInsideAPaneStacksARealDialog_andTheCapStillRefusesInDialog() {
		var n = sub("nested");
		assertEquals(2L, num(n, "cap"), () -> "MAX_DIALOG_DEPTH must still be 2: " + report);
		assertEquals(Boolean.TRUE, n.get("escVisible"), () -> report.toString());
		assertEquals(1L, num(n, "before"), () -> report.toString());
		assertEquals(2L, num(n, "after"),
			() -> "a type=action inside a section pane must stack a real nested dialog: " + report);
		assertEquals(2L, num(n, "twoBackdrops"), () -> report.toString());
		assertEquals(2L, num(n, "afterThird"), () -> "the third dialog must be refused, not stacked: " + report);
		assertEquals(2L, num(n, "stillTwoBackdrops"), () -> "a third overlay was created: " + report);
		assertEquals(Boolean.TRUE, n.get("refusalInTopDialog"),
			() -> "the refusal must be painted inside the current top dialog: " + report);
	}
}
