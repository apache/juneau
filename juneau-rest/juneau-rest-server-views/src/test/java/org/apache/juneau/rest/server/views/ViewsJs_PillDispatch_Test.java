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
 * Action-bound pill dispatch wired on {@code initRowActions} (NOT details-gated).  Source-shape checks always run; the
 * Node behavioral layer (DOM shim) runs when {@code node} is on {@code PATH}.  Proves an action pill submits its
 * RowAction through the same fail-closed handler the row-action menu uses on a grid with rowActions and NO row-detail
 * template, that Enter/Space activate it (Space {@code preventDefault}-ed), that {@code present=dialog} opens the modal
 * instead of submitting, and that a disabled / in-flight / unknown / display-only pill is inert.
 */
class ViewsJs_PillDispatch_Test extends TestBase {

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

	@Test void a01_sourceShape_pillDispatchHostedOnInitRowActions_notDetailsGated() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function activatePillAction("), body);
		// The pill click + keydown must be wired inside initRowActions (always available when rowActions exist),
		// never only inside initDetailsExpander (which returns early unless a row-detail template is present).
		var start = body.indexOf("function initRowActions(");
		assertTrue(start >= 0, "missing initRowActions");
		var end = body.indexOf("function removeEl(", start);
		var host = body.substring(start, end > start ? end : start + 2000);
		assertTrue(host.contains("addEventListener(\"keydown\""), "initRowActions must bind a table-level keydown");
		assertTrue(host.contains("[data-juneau-pill]"), "initRowActions must dispatch [data-juneau-pill]");
		assertTrue(host.contains("activatePillAction("), host);
	}

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
			var p = Path.of(basedir, "src/test/js/pill.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/pill.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/pill.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("pill-stdout-", ".json");
		var stderr = Files.createTempFile("pill-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("pill.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("pill.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or pill.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_clickSubmitsThroughTheHandler_noRowDetailTemplate() {
		var r = report();
		assertEquals(true, r.get("hasInit"));
		assertEquals(true, r.get("click_fetchIssued"));
		assertEquals("/x/ack", r.get("click_url"));
		assertEquals("POST", r.get("click_method"));
	}

	@Test void b02_keyboardEnterAndSpaceActivate_spacePreventsDefault() {
		var r = report();
		assertEquals(true, r.get("enter_prevented"));
		assertEquals(true, r.get("enter_fetchIssued"));
		assertEquals(true, r.get("space_prevented"));
		assertEquals(true, r.get("space_fetchIssued"));
		assertEquals(false, r.get("otherKey_prevented"));   // a non-activation key is left alone
		assertEquals(false, r.get("otherKey_fetchIssued"));
	}

	@Test void b03_presentDialogOpensModalInsteadOfSubmitting() {
		var r = report();
		assertEquals(false, r.get("dialog_fetchIssued"));   // confirm/dialog branch, not a direct submit
		assertEquals(true, r.get("dialog_layerOpen"));
	}

	@Test void b04_disabledInflightUnknownAndDisplayOnlyPillsAreInert() {
		var r = report();
		assertEquals(false, r.get("disabled_fetchIssued"));
		assertEquals(false, r.get("inflight_fetchIssued"));
		assertEquals(false, r.get("unknownAction_fetchIssued"));
		assertEquals(false, r.get("displayOnly_fetchIssued"));
	}

	@Test void b05_setActionRefEnabledReflectsDisabledStateViaAriaOnTheSpanButton() {
		var r = report();
		assertEquals(true, r.get("setDisabled_aria"));
		assertEquals(false, r.get("setDisabled_fetchIssued"));
		assertEquals(true, r.get("setEnabled_ariaCleared"));
		assertEquals(true, r.get("setEnabled_fetchIssued"));
	}

	@Test void b06_pillDispatchesEvenWhenDetailsExpanderAlsoWired() {
		assertEquals(true, report().get("withDetails_fetchIssued"));
	}

	/**
	 * Selection stays the checkbox protocol.  With {@code initSelection} wired on the same table, activating a pill by
	 * click, {@code Enter} or {@code Space} leaves {@code selectionState} untouched - a pill is a row action or pure
	 * presentation, never a selection affordance.  The checkbox assertion at the end is the positive control that
	 * makes the zeroes meaningful.
	 */
	@Test void b07_pillActivationNeverTogglesSelection() {
		var r = report();
		assertEquals(0, r.get("selection_afterPillClick"));
		assertEquals(0, r.get("selection_afterPillKeys"));
		assertEquals(0, r.get("selection_afterDisplayOnlyPillClick"));
		assertEquals(1, r.get("selection_afterCheckboxChange"));
		assertEquals(true, r.get("selection_checkboxSelectedR1"));
	}

	/**
	 * {@code RowAction.enabledWhen} draw-time visual pass ({@code applyRowActionPillGates}, the runtime's
	 * {@code createdRow}-time hook).  A failing rule disables the pill, sets a native {@code title} tooltip, and
	 * attaches an {@code aria-describedby} reason node carrying the same text - never hidden.  A passing rule leaves
	 * the pill exactly as an ungated action would render.
	 */
	@Test void b08_pillDrawTimeGate_disablesWithTitleAndDescNode_neverHidden() {
		var r = report();
		assertEquals(true, r.get("drawGate_failingAriaDisabled"));
		assertEquals("Only open items can be acknowledged.", r.get("drawGate_failingTitle"));
		assertEquals(true, r.get("drawGate_failingNeverHidden"));
		assertEquals(true, r.get("drawGate_failingDescNodeExists"));
		assertEquals(true, r.get("drawGate_failingDescNodeIdMatches"));
		assertEquals("Only open items can be acknowledged.", r.get("drawGate_failingDescText"));
		assertEquals(true, r.get("drawGate_passingAriaDisabledAbsent"));
		assertEquals(true, r.get("drawGate_passingNoTitle"));
	}

	/**
	 * When more than one rule fails, the first-declared rule wins, in either declared order - proven with the same
	 * two rules swapped so the outcome tracks declaration order, not field name or map iteration order.
	 */
	@Test void b09_firstDeclaredFailingRuleWins_inEitherDeclaredOrder() {
		var r = report();
		assertEquals("REASON-STATUS", r.get("firstFailing_forward"));
		assertEquals("REASON-OWNER", r.get("firstFailing_reversed"));
	}

	/**
	 * Fail-closed: a rule's field simply absent from the row payload, or a row payload that could not be resolved at
	 * all ({@code null}), both gate the action - mirrors the {@code ActionRef} evaluator's fail-closed semantics
	 * ({@code Object.hasOwn}) rather than treating a missing key as trivially non-matching.
	 */
	@Test void b10_failClosedOnMissingFieldOrUnresolvedRowData() {
		var r = report();
		assertEquals(true, r.get("failClosed_missingFieldDisabled"));
		assertEquals(true, r.get("failClosed_nullRowDataDisabled"));
	}

	/**
	 * {@code activatePillAction} re-checks the gate fresh at click time, independent of the draw-time visual pass -
	 * defense in depth so a gated-and-failing action can never fire even if its disabled state were somehow bypassed.
	 */
	@Test void b11_activatePillActionReChecksFreshAtClickTime() {
		var r = report();
		assertEquals(true, r.get("reCheck_failingNeverFires"));
		assertEquals(true, r.get("reCheck_passingStillFires"));
	}

	/**
	 * {@code buildRowActionMenu}: a gated-and-failing item is disabled and reasoned but still present in the menu
	 * (never removed/hidden), carries a real {@code aria-describedby} target node, and its click never fires. A
	 * gated-and-passing item renders enabled and its click fires normally.
	 */
	@Test void b12_menuGate_failingDisabledButPresent_passingEnabled() {
		var r = report();
		assertEquals(true, r.get("menu_failingItemDisabled"));
		assertEquals("Only open items can be acknowledged.", r.get("menu_failingItemTitle"));
		assertEquals(true, r.get("menu_failingItemStillPresent"));
		assertEquals(true, r.get("menu_failingItemNeverHidden"));
		assertEquals(true, r.get("menu_failingDescNodeExists"));
		assertEquals(true, r.get("menu_failingDescNodeIdMatches"));
		assertEquals(true, r.get("menu_failingClickNeverFires"));
		assertEquals(true, r.get("menu_passingItemEnabled"));
		assertEquals(true, r.get("menu_passingClickFires"));
	}

	/**
	 * Pins that {@code buildRowActionMenu} resolves per-row data via {@code ctx.dataTable.row(tr).data()} (the exact
	 * DataTables lookup {@code openCellPopover} also uses), passing the SAME {@code tr} it was given; and that it
	 * fails closed - the action renders disabled - when no {@code dataTable} is wired on {@code ctx} at all.
	 */
	@Test void b13_menuResolvesRowDataViaDataTableRowLookup_failsClosedWithoutOne() {
		var r = report();
		assertEquals(true, r.get("menu_dataTableRowCalledWithSameTr"));
		assertEquals(true, r.get("menu_noDataTableFailsClosed"));
	}

	/**
	 * {@code openFormActionDialog}: a gated-and-failing dialog action paints a visible refusal (naming the reason)
	 * into the current top dialog and opens no new dialog layer; a gated-and-passing dialog action opens its confirm
	 * dialog exactly as an ungated action would.
	 */
	@Test void b14_openFormActionDialogGate_failingRefusesInPlace_passingOpens() {
		var r = report();
		assertEquals(true, r.get("dialogGate_failingNoNewLayer"));
		var refusal = (String) r.get("dialogGate_failingRefusalText");
		assertTrue(refusal != null && refusal.contains("Only open items can be acknowledged."), refusal);
		assertEquals(true, r.get("dialogGate_passingOpensDialog"));
	}

	/**
	 * {@code initSelection} listens for {@code change} on the two checkbox classes and nothing else: no pill branch,
	 * no {@code data-juneau-pill-select} attribute, no click path.  Source-shape half of the lock above, so a
	 * re-grown select-pill protocol fails even if it happened to leave {@code selectionState} alone in the probe.
	 */
	@Test void a05_sourceShape_initSelectionIsCheckboxOnly() throws Exception {
		var body = viewsJs();
		assertFalse(body.contains("data-juneau-pill-select"), "No pill-select protocol may exist.");
		var start = body.indexOf("function initSelection(");
		assertTrue(start > 0, body);
		var fn = body.substring(start, body.indexOf("\n\t/**", start));
		assertTrue(fn.contains("addEventListener(\"change\""), fn);
		assertFalse(fn.contains("addEventListener(\"click\""), fn);
		assertFalse(fn.contains("addEventListener(\"keydown\""), fn);
		assertFalse(fn.contains("pill"), fn);
		assertFalse(fn.contains("data-juneau-action"), fn);
		assertTrue(fn.contains(".juneau-view-select-checkbox"), fn);
		assertTrue(fn.contains(".juneau-view-select-all-checkbox"), fn);
	}
}
