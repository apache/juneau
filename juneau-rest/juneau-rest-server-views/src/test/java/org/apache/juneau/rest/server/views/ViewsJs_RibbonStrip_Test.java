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
 * Always-on coverage for the SHARED ribbon-format strip builder and its two callers.
 *
 * <p>The strip used to exist only inside the row-detail expander.  It is now {@code buildRibbonStrip(items, opts)} -
 * given an ordered {@code [{id, label, pane}]} it wires {@code role=tab} / {@code aria-selected} /
 * {@code aria-controls} / {@code role=tabpanel} / a roving tabindex, moves selection on Left/Right/Home/End, and
 * fires {@code onActivate} once per activation.  Two callers use it: {@code buildDetailStrip} (row details) and
 * {@code appendSectionedDialogForm} (a sectioned dialog form).
 *
 * <p>Two things here are load-bearing and deliberately strict:
 *
 * <ul>
 *    <li>{@code buildDetailStrip} is on the shipped row-detail path, so the refactor must be DOM-invisible.  The
 *        harness serializes a whole built panel - elements, attributes in insertion order, and the properties the
 *        runtime sets instead of attributes - and {@link #c01_detailStripDom_isByteForByteUnchanged()} pins that
 *        serialization against an exact expected string.  A reordered attribute or a dropped property fails it.
 *    <li>A dialog must NEVER reuse the detail strip.  The harness spies the {@code buildDetailStrip} export and
 *        requires zero calls while painting sectioned dialogs, and
 *        {@link #a04_dialogPathNeverRoutesThroughTheDetailStrip()} additionally pins that the runtime has exactly
 *        one textual call site for it - the row-detail expander.
 * </ul>
 */
class ViewsJs_RibbonStrip_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** The body of a top-level {@code function name(} up to the next top-level {@code \n\tfunction } / {@code \n\t/**}. */
	private static String bodyOf(String src, String fn) {
		var start = src.indexOf("\tfunction " + fn + "(");
		assertTrue(start >= 0, () -> "no top-level function " + fn + " in juneau-views.js");
		var end = src.indexOf("\n\t}\n", start);
		assertTrue(end > start, () -> "unterminated function " + fn);
		return src.substring(start, end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Source shape - the module boundary between the generic builder and its callers
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_genericBuilderExistsAndIsExported() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function buildRibbonStrip(items, opts)"), body);
		assertTrue(body.contains("buildRibbonStrip: buildRibbonStrip"), "buildRibbonStrip must be exported on NS.init");
	}

	@Test void a02_detailStripIsAThinCaller_notASecondStripImplementation() throws Exception {
		var detail = bodyOf(viewsJs(), "buildDetailStrip");
		assertTrue(detail.contains("buildRibbonStrip(items, {"), () -> "buildDetailStrip must delegate: " + detail);
		// The strip element, its role/tablist/mode attributes, the tab buttons and the keyboard listeners are the
		// GENERIC builder's job now - none of them may be re-implemented in the detail caller.
		for (var forbidden : new String[]{
			"createElement(\"div\")", "createElement(\"button\")", "\"tablist\"", "data-juneau-strip-mode",
			"addEventListener(\"keydown\"", "addEventListener(\"click\"", "detailTabTargetIndex("
		})
			assertFalse(detail.contains(forbidden),
				() -> "buildDetailStrip must not re-implement '" + forbidden + "': " + detail);
	}

	@Test void a03_theEscapeHatchStaysInTheDetailCaller() throws Exception {
		var detail = bodyOf(viewsJs(), "buildDetailStrip");
		var generic = bodyOf(viewsJs(), "buildRibbonStrip");
		// Title borrowing + hiding, per-expand id minting, the insertion point, and u's bar-slot relocate are DETAIL
		// concerns; the {id, label, pane} model cannot express them and the generic builder must not learn them.
		assertTrue(detail.contains(".juneau-view-detail-section-title"), detail);
		assertTrue(detail.contains("titleEl.hidden = true"), detail);
		assertTrue(detail.contains("++detailStripSeq"), detail);
		assertTrue(detail.contains("juneau-detail-tab-\" + seq"), detail);
		assertTrue(detail.contains("juneau-detail-pane-\" + seq"), detail);
		assertTrue(detail.contains(".juneau-view-detail-header"), detail);
		assertTrue(detail.contains("relocateDetailBarSlot(panel, strip)"), detail);
		// detailTabTargetIndex / activateDetailTab keep their historical names (the detail strip was the first
		// caller) and ARE generic; what must not leak in is anything that only a row detail has.
		for (var leaked : new String[]{
			"data-juneau-detail-section", "juneau-view-detail-section-title", "juneau-view-detail-header",
			"detailStripSeq", "relocateDetailBarSlot", "bar-slot"
		})
			assertFalse(generic.contains(leaked),
				() -> "the generic builder must not know about '" + leaked + "': " + generic);
	}

	@Test void a04_dialogPathNeverRoutesThroughTheDetailStrip() throws Exception {
		var body = viewsJs();
		var sectioned = bodyOf(body, "appendSectionedDialogForm");
		assertTrue(sectioned.contains("buildRibbonStrip(items, {"), sectioned);
		assertFalse(sectioned.contains("buildDetailStrip"),
			() -> "a dialog must never call the row-detail strip: " + sectioned);
		// One definition, one export, and exactly ONE call site - the row-detail expander.  A second call site
		// appearing here is exactly the "reuse shortcut" this slice prohibits.
		assertEquals(1, count(body, "\tfunction buildDetailStrip("), body);
		assertEquals(1, count(body, "\t\tbuildDetailStrip: buildDetailStrip,"), body);
		assertEquals(2, count(body, "buildDetailStrip(panel"),
			() -> "buildDetailStrip must have exactly one definition and one call site: " + body);
		assertTrue(body.contains("\t\tbuildDetailStrip(panel, function (sid, pane) {"),
			() -> "the one call site must be the row-detail expander: " + body);
	}

	@Test void a05_theInDialogStripOpensNoLayer() throws Exception {
		var generic = bodyOf(viewsJs(), "buildRibbonStrip");
		var sectioned = bodyOf(viewsJs(), "appendSectionedDialogForm");
		// A strip is layout on the surface it sits on: it must not push a layer, so a sectioned dialog stays ONE
		// dialog against MAX_DIALOG_DEPTH.
		assertFalse(generic.contains("pushLayer("), generic);
		assertFalse(sectioned.contains("pushLayer("), sectioned);
	}

	@Test void a06_sectionedFormIsMutuallyExclusiveWithFlatFields() throws Exception {
		var painter = bodyOf(viewsJs(), "appendDialogForm");
		assertTrue(painter.contains("if (form.sections && form.sections.length)"), painter);
		assertTrue(painter.contains("appendSectionedDialogForm(dialog, form, table, tr, ctx, seq); return;"), painter);
	}

	private static int count(String haystack, String needle) {
		var n = 0;
		for (var i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length()))
			n++;
		return n;
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
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
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
			var p = Path.of(basedir, "src/test/js/ribbon-strip.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/ribbon-strip.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/ribbon-strip.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("ribbon-strip-stdout-", ".json");
		var stderr = Files.createTempFile("ribbon-strip-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("ribbon-strip.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("ribbon-strip.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or ribbon-strip.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_harnessLoadedTheGenericBuilder() {
		assertEquals(true, report().get("hasInit"), report()::toString);
	}

	@Test void b02_genericBuilder_stripAndAriaWiring() {
		var r = report();
		assertEquals(true, r.get("generic_returnsStripTabsActivate"), r::toString);
		assertEquals("tablist", r.get("generic_stripRole"), r::toString);
		assertEquals("tab", r.get("generic_stripMode"), r::toString);
		assertEquals("test-strip", r.get("generic_stripTestId"), r::toString);
		assertEquals("juneau-view-ribbon-group juneau-view-test-strip", r.get("generic_stripClass"), r::toString);
		assertNum(3, r.get("generic_tabCount"));
		assertEquals("One,Two,Three", r.get("generic_labels"), r::toString);
		assertEquals("juneau-view-ribbon-btn", r.get("generic_btnClass"), r::toString);
		assertEquals("tt-0,tt-1,tt-2", r.get("generic_tabIds"), r::toString);
		assertEquals("pp-0,pp-1,pp-2", r.get("generic_paneIds"), r::toString);
		assertEquals("pp-0,pp-1,pp-2", r.get("generic_ariaControls"), r::toString);
		assertEquals("tabpanel,tabpanel,tabpanel", r.get("generic_paneRoles"), r::toString);
		assertEquals("tt-0,tt-1,tt-2", r.get("generic_paneLabelledby"), r::toString);
		assertEquals("0,0,0", r.get("generic_paneTabindexAttr"), r::toString);
		// Exactly one selected tab, a roving tabindex, and exactly one visible pane.
		assertEquals("true,false,false", r.get("generic_ariaSelected"), r::toString);
		assertEquals("0,-1,-1", r.get("generic_tabindexes"), r::toString);
		assertEquals("false,true,true", r.get("generic_paneHidden"), r::toString);
		// The builder is position-agnostic: the caller decides where the strip goes.
		assertEquals(true, r.get("generic_stripDetached"), () -> "the builder must not insert the strip: " + r);
		assertEquals(true, r.get("generic_stripHasNoDetailTestId"), r::toString);
	}

	@Test void b03_genericBuilder_keyboardNavigationWrapsAndHomeEndJump() {
		var r = report();
		assertEquals("false,true,false", r.get("kbd_right_selected"), r::toString);
		assertEquals(true, r.get("kbd_right_focus"), () -> "selection must carry focus (roving tabindex): " + r);
		assertEquals("false,false,true", r.get("kbd_end_selected"), r::toString);
		assertEquals("true,false,false", r.get("kbd_rightWrap_selected"), () -> "Right must wrap past the end: " + r);
		assertEquals("false,false,true", r.get("kbd_leftWrap_selected"), () -> "Left must wrap before the start: " + r);
		assertEquals("true,false,false", r.get("kbd_home_selected"), r::toString);
		// A key the widget does not own changes nothing and fires nothing.
		assertEquals("true,false,false", r.get("kbd_unhandled_selected"), r::toString);
		assertNum(0, r.get("kbd_unhandled_activationCount"));
	}

	@Test void b04_genericBuilder_activationCallbackFiresExactlyOncePerActivation() {
		var r = report();
		assertNum(1, r.get("kbd_right_activationCount"));
		assertEquals("two", r.get("kbd_right_activationId"), r::toString);
		assertEquals(true, r.get("kbd_right_activationPane"), () -> "onActivate must receive the activated pane: " + r);
		assertNum(1, r.get("kbd_end_activationCount"));
		assertNum(1, r.get("kbd_home_activationCount"));
		assertNum(1, r.get("click_activationCount"));
		assertEquals("three", r.get("click_activationId"), r::toString);
		assertEquals("false,false,true", r.get("click_selected"), r::toString);
		assertEquals(true, r.get("click_paneVisible"), r::toString);
		assertEquals(true, r.get("click_othersHidden"), r::toString);
	}

	@Test void b05_genericBuilder_optionsAndDegenerateInputs() {
		var r = report();
		assertEquals("false,true", r.get("activeIndex_selected"), () -> "activeIndex must pick the open item: " + r);
		assertEquals(true, r.get("paneless_built"), () -> "a strip may be pure navigation with no panes: " + r);
		assertEquals(true, r.get("paneless_tabControlsMintedId"), r::toString);
		assertEquals(true, r.get("empty_returnsNull"), r::toString);
		assertEquals(true, r.get("null_returnsNull"), r::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The detail strip's DOM, pinned
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The exact serialization {@code buildDetailStrip} produced BEFORE it was reduced to a caller of the generic
	 * builder, for a two-section panel.  Attributes appear in insertion order and a leading {@code .} marks a
	 * property the runtime sets directly rather than through {@code setAttribute} - so a reordered attribute, a
	 * class moved onto {@code setAttribute}, or a lost {@code hidden} / roving {@code tabindex} all fail here.
	 */
	private static final String DETAIL_STRIP_DOM = String.join("\n",
		"<div .class=\"juneau-view-detail-panel\">",
		"  <div data-juneau-strip-mode=\"tab\" role=\"tablist\" data-testid=\"detail-tabs\" .class=\"juneau-view-ribbon-group juneau-view-detail-tabs\">",
		"    <button id=\"juneau-detail-tab-1-0\" role=\"tab\" data-juneau-strip-tab=\"overview\" aria-controls=\"juneau-detail-pane-1-0\" aria-selected=\"true\" .class=\"juneau-view-ribbon-btn\" .tabindex=0 .type=\"button\">Overview",
		"    <button id=\"juneau-detail-tab-1-1\" role=\"tab\" data-juneau-strip-tab=\"context\" aria-controls=\"juneau-detail-pane-1-1\" aria-selected=\"false\" .class=\"juneau-view-ribbon-btn\" .tabindex=-1 .type=\"button\">Context",
		"  <section data-juneau-detail-section=\"overview\" id=\"juneau-detail-pane-1-0\" role=\"tabpanel\" aria-labelledby=\"juneau-detail-tab-1-0\" tabindex=\"0\" .class=\"juneau-view-detail-section\">",
		"    <h2 .class=\"juneau-view-detail-section-title\" .hidden>Overview",
		"  <section data-juneau-detail-section=\"context\" id=\"juneau-detail-pane-1-1\" role=\"tabpanel\" aria-labelledby=\"juneau-detail-tab-1-1\" tabindex=\"0\" .class=\"juneau-view-detail-section\" .hidden>",
		"    <h2 .class=\"juneau-view-detail-section-title\" .hidden>Context",
		"");

	@Test void c01_detailStripDom_isByteForByteUnchanged() {
		var r = report();
		assertEquals(DETAIL_STRIP_DOM, r.get("detailStrip_dump"),
			"buildDetailStrip's DOM changed - the row-detail strip is on the shipped path and must be "
			+ "DOM-invisible across the generic-builder refactor");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Sectioned dialog form
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_sectionedForm_paintsARibbonStripOverOnePanePerSection() {
		var r = report();
		assertEquals(true, r.get("sectioned_wrapPresent"), r::toString);
		assertEquals(true, r.get("sectioned_stripPresent"), r::toString);
		assertEquals("tablist", r.get("sectioned_stripRole"), r::toString);
		assertEquals("tab", r.get("sectioned_stripMode"), r::toString);
		assertEquals("juneau-view-ribbon-group juneau-view-dialog-sections", r.get("sectioned_stripClass"), r::toString);
		assertEquals(true, r.get("sectioned_stripIsFirstChildOfWrap"), () -> "the ribbon leads the form body: " + r);
		assertNum(2, r.get("sectioned_tabCount"));
		assertEquals("Basics,Advanced", r.get("sectioned_labels"), r::toString);
		assertEquals("juneau-dialog-section-tab-7-0,juneau-dialog-section-tab-7-1", r.get("sectioned_tabIds"),
			() -> "section tab ids must be namespaced by the dialog seq: " + r);
		assertNum(2, r.get("sectioned_paneCount"));
		assertEquals("basics,advanced", r.get("sectioned_paneIds"), r::toString);
		assertEquals("tabpanel", r.get("sectioned_pane0Role"), r::toString);
		assertEquals(true, r.get("sectioned_pane0Visible"), r::toString);
		assertEquals(true, r.get("sectioned_pane1Hidden"), r::toString);
	}

	@Test void d02_sectionedForm_usesTheGenericBuilderNotTheDetailStrip() {
		var r = report();
		assertEquals(true, r.get("sectioned_buildDetailStripNotCalled"),
			() -> "the spied buildDetailStrip export must stay at zero calls: " + r);
		assertEquals(true, r.get("sectioned_noDetailTestId"),
			() -> "a dialog must not paint the row-detail strip's test id: " + r);
	}

	@Test void d03_sectionedForm_rowsAreHiddenNotRecreated() {
		var r = report();
		// Every section is painted up front, so a value typed into one survives a trip through another and each
		// control's error sibling stays inside its own section.
		assertEquals(true, r.get("sectioned_fieldIdShape"),
			() -> "field element ids stay the flat form's, so collection and validation are unchanged: " + r);
		assertEquals(true, r.get("sectioned_errorSiblingsPerSection"), r::toString);
		assertEquals(true, r.get("sectioned_valuePreservedAcrossSwitch"), r::toString);
		assertEquals(true, r.get("sectioned_collectSpansHiddenSections"), r::toString);
	}

	@Test void d04_confirmTimeValidation_revealsTheOwningSectionBeforeFocusing() {
		var r = report();
		assertEquals(true, r.get("sectioned_validateFailsFromHiddenSection"),
			() -> "a required control emptied in a hidden section must still block submit: " + r);
		assertEquals(true, r.get("sectioned_errorAttachedToOwnSection"), r::toString);
		assertEquals(true, r.get("sectioned_ownSectionRevealed"),
			() -> "focus must never land on a control in a hidden pane: " + r);
	}

	@Test void d05_flatFormIsUntouchedByTheSectionedPath() {
		var r = report();
		assertEquals(true, r.get("flat_noStrip"), r::toString);
		assertEquals(true, r.get("flat_noSectionPanes"), r::toString);
		assertEquals(true, r.get("flat_stillPaintsTheRow"), r::toString);
		assertEquals(true, r.get("flat_buildDetailStripStillNotCalled"), r::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Depth accounting
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_aSectionedDialogIsOneDialog_andTheCapStillRefusesInDialog() {
		var r = report();
		assertEquals(true, r.get("depth_capIsTwo"), r::toString);
		assertEquals(true, r.get("depth_sectionedIsOneLayer"),
			() -> "a ribbon is layout, not a layer - a sectioned dialog must cost exactly one slot: " + r);
		assertEquals(true, r.get("depth_sectionedDialogHasStrip"), r::toString);
		assertNum(2, r.get("depth_sectionedDialogTabCount"));
		assertEquals(true, r.get("depth_secondStacks"), r::toString);
		assertEquals(true, r.get("depth_outerStillOpen"), r::toString);
		assertEquals(true, r.get("depth_thirdRefused"), () -> "the third dialog must not open: " + r);
		assertEquals(true, r.get("depth_refusalInTopDialog"),
			() -> "the refusal is painted INSIDE the current top dialog, never as a third overlay: " + r);
		assertEquals(true, r.get("depth_refusalNamesTheCap"), r::toString);
		assertEquals(true, r.get("depth_buildDetailStripNeverCalled"), r::toString);
	}
}
