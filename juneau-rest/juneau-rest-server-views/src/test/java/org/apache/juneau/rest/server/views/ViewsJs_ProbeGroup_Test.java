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
 * Always-on coverage for the status probe group - the single-select status-chip control JuneauViews enhances in
 * place over server-painted markup.
 *
 * <p>Juneau owns SELECTION only (radiogroup/radio semantics + a roving tabindex): exactly one probe per group is
 * selected, and selecting one clears the rest.  The app keeps the click ACTION (a click can re-run the underlying
 * check), which repaints a chip's status class; selection is keyed to each probe's IDENTITY
 * ({@code data-juneau-probe}) so a repaint never disturbs it.  {@code onSelect} fires on a real selection CHANGE
 * only - never on init, on an imperative {@code select()}, or on a no-op re-click.  Disabled probes are skipped by
 * the roving focus and are not selectable; an empty group is a no-op.
 */
class ViewsJs_ProbeGroup_Test extends TestBase {

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

	//------------------------------------------------------------------------------------------------------------------
	// Source shape - the helper exists and is exported on the NS init object
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_helperExistsAndIsExported() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function enhanceProbeGroup(group, opts)"), body);
		assertTrue(body.contains("function initProbeGroups(root)"), body);
		assertTrue(body.contains("function probeTargetIndex(key, currentIndex, enabled)"), body);
		assertTrue(body.contains("enhanceProbeGroup: enhanceProbeGroup"), "enhanceProbeGroup must be exported on NS.init");
		assertTrue(body.contains("initProbeGroups: initProbeGroups"), "initProbeGroups must be exported on NS.init");
		assertTrue(body.contains("probeTargetIndex: probeTargetIndex"), "probeTargetIndex must be exported on NS.init");
	}

	@Test void a02_initAllScansProbeGroups() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("initProbeGroups(document);"),
			"initAll must scan page-wide for probe groups");
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
			var p = Path.of(basedir, "src/test/js/probe-group.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/probe-group.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/probe-group.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("probe-group-stdout-", ".json");
		var stderr = Files.createTempFile("probe-group-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("probe-group.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("probe-group.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or probe-group.cjs not found — behavioral layer skipped");
		return report;
	}

	private static void assertNum(long expected, Object actual) {
		assertInstanceOf(Number.class, actual, () -> "expected a number, got: " + actual);
		assertEquals(expected, ((Number)actual).longValue());
	}

	@Test void b01_harnessLoadedTheHelper() {
		assertEquals(true, report().get("hasInit"), report()::toString);
	}

	@Test void b02_initialPaint_rolesSelectionAndRovingTabindex() {
		var r = report();
		assertEquals("radiogroup", r.get("init_groupRole"), r::toString);
		assertEquals("radio,radio,radio", r.get("init_probeRoles"), r::toString);
		// The markup-declared aria-checked probe wins the initial selection, with a single roving tab stop.
		assertEquals("false,true,false", r.get("init_ariaChecked"), r::toString);
		assertEquals("-1,0,-1", r.get("init_tabindexes"), r::toString);
		assertEquals("warn", r.get("init_selectedId"), r::toString);
		// Init NEVER fires onSelect.
		assertNum(0, r.get("init_eventCount"));
	}

	@Test void b03_click_movesSelectionFocusAndFiresOnce_reclickIsNoop() {
		var r = report();
		assertEquals("false,false,true", r.get("click_ariaChecked"), r::toString);
		assertEquals("-1,-1,0", r.get("click_tabindexes"), r::toString);
		assertNum(1, r.get("click_eventCount"));
		assertEquals("fail", r.get("click_eventId"), r::toString);
		assertEquals(true, r.get("click_focus"), () -> "selection must carry focus (roving tabindex): " + r);
		// Re-clicking the selected probe leaves it selected and fires nothing.
		assertNum(0, r.get("reclick_eventCount"));
		assertEquals("fail", r.get("reclick_stillSelected"), r::toString);
	}

	@Test void b04_statusRepaint_preservesSelectionByIdentity_andEmitsNothing() {
		var r = report();
		assertEquals("fail", r.get("repaint_selectedId"),
			() -> "a status-class repaint must not disturb selection (keyed to identity): " + r);
		assertEquals("false,false,true", r.get("repaint_ariaChecked"), r::toString);
		assertNum(0, r.get("repaint_eventCount"));
	}

	@Test void b05_keyboard_wrapsSkipsDisabledAndHomeEndJump() {
		var r = report();
		assertEquals("a", r.get("kbd_initSelected"), () -> "first ENABLED probe is the initial selection: " + r);
		assertEquals("c", r.get("kbd_rightSkipsDisabled"), () -> "Right must skip a disabled probe: " + r);
		assertEquals("c", r.get("kbd_rightEventId"), r::toString);
		assertEquals(true, r.get("kbd_rightFocus"), () -> "arrow selection must carry focus: " + r);
		assertEquals("a", r.get("kbd_rightWrap"), () -> "Right must wrap past the end: " + r);
		assertEquals("c", r.get("kbd_leftWrap"), () -> "Left must wrap before the start: " + r);
		assertEquals("c", r.get("kbd_downSameAsRight"), () -> "Down mirrors Right: " + r);
		assertEquals("a", r.get("kbd_upSameAsLeft"), () -> "Up mirrors Left: " + r);
		assertEquals("a", r.get("kbd_home"), () -> "Home jumps to the first enabled: " + r);
		assertEquals("c", r.get("kbd_end"), () -> "End jumps to the last enabled: " + r);
		// A key the widget does not own changes nothing and fires nothing.
		assertNum(0, r.get("kbd_unhandledEventCount"));
		assertEquals(true, r.get("kbd_unhandledUnchanged"), r::toString);
	}

	@Test void b06_disabledProbes_notInRovingFocus_andNotSelectable() {
		var r = report();
		assertNum(-1, r.get("disabled_tabindex"));
		assertNum(0, r.get("disabled_clickEventCount"));
		assertEquals(true, r.get("disabled_clickUnchanged"),
			() -> "clicking a disabled probe must not change selection: " + r);
	}

	@Test void b07_imperativeSelect_movesSelectionWithoutEvent_rejectsDisabledAndUnknown() {
		var r = report();
		assertEquals(true, r.get("imperative_changed"), r::toString);
		assertNum(0, r.get("imperative_eventCount"));   // imperative select() never notifies
		assertEquals("a", r.get("imperative_selected"), r::toString);
		assertEquals(true, r.get("imperative_disabledRejected"),
			() -> "select() of a disabled probe must be rejected: " + r);
		assertEquals("a", r.get("imperative_selectedStill"), r::toString);
		assertEquals(true, r.get("imperative_unknownRejected"),
			() -> "select() of an unknown id must be rejected: " + r);
	}

	@Test void b08_groupsAreIndependent() {
		var r = report();
		assertEquals(true, r.get("groups_aSelected"), () -> "selecting in group B must not clear group A: " + r);
		assertEquals(true, r.get("groups_bSelected"), r::toString);
	}

	@Test void b09_degenerateGroups_emptyIsNoop_allDisabledSelectsNothing() {
		var r = report();
		assertEquals(true, r.get("empty_noSelected"), r::toString);
		assertEquals(true, r.get("empty_keydownNoThrow"),
			() -> "an empty group must not throw or focus a missing child: " + r);
		assertEquals(true, r.get("allDisabled_noSelected"), r::toString);
		assertEquals("-1,-1", r.get("allDisabled_tabindexes"), () -> "an all-disabled group has no tab stop: " + r);
		assertEquals("false,false", r.get("allDisabled_ariaChecked"), r::toString);
	}

	@Test void b10_idempotentEnhance_andPageWideInit() {
		var r = report();
		assertEquals(true, r.get("idempotent_sameCtl"),
			() -> "a second enhanceProbeGroup on the same group must return the first handle: " + r);
		assertEquals(true, r.get("initAll_enhanced"), r::toString);
		assertEquals("radiogroup", r.get("initAll_role"), r::toString);
		assertEquals(true, r.get("initAll_selected"), r::toString);
	}

	@Test void b11_probeTargetIndex_pureRovingContract() {
		var r = report();
		assertNum(2, r.get("pti_right"));       // 0 -> skip disabled 1 -> 2
		assertNum(0, r.get("pti_rightWrap"));   // 2 -> wrap -> 0
		assertNum(0, r.get("pti_left"));        // 2 -> skip disabled 1 -> 0
		assertNum(2, r.get("pti_leftWrap"));    // 0 -> wrap -> 2
		assertNum(2, r.get("pti_down"));        // Down mirrors Right
		assertNum(0, r.get("pti_up"));          // Up mirrors Left
		assertNum(0, r.get("pti_home"));        // first enabled
		assertNum(2, r.get("pti_end"));         // last enabled
		assertNum(-1, r.get("pti_unhandled"));  // a key the widget does not own
		assertNum(-1, r.get("pti_noneEnabled")); // nothing enabled
		assertNum(-1, r.get("pti_empty"));       // empty group
	}
}
