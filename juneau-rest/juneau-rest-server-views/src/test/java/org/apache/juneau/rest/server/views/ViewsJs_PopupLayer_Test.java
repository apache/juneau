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
 * Always-on coverage for the shared popup layer stack: push/pop ordering, top-layer-only Escape and
 * outside-click, per-dialog backdrop teardown, focus trap and restore, per-depth inline z-index, the
 * dialog-kind depth cap of two, and the registry-regression case (a dialog plus a separately triggered
 * popover are two stack entries but only one dialog, so the popover does not consume the dialog cap).
 */
class ViewsJs_PopupLayer_Test extends TestBase {

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

	@Test void a01_sourceShape_singleLayerStackOwnerAndDocumentListeners() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("popupLayerStack = [];"), body);
		assertTrue(body.contains("function pushLayer("), body);
		assertTrue(body.contains("function popLayer("), body);
		assertTrue(body.contains("function dialogLayerCount("), body);
		assertTrue(body.contains("function topLayer("), body);
		assertTrue(body.contains("MAX_DIALOG_DEPTH"), body);
		assertTrue(body.contains("function bindLayerStackDocumentListeners("), body);
		assertTrue(body.contains("_layerListenersBound"), body);
	}

	/**
	 * The cap is 2 and is <b>not</b> a deferred promise.  The stale "Unlimited dialog depth is 10.0." comment
	 * advertised a follow-on that is not coming: the answer to "I need more depth" is a sectioned dialog, not a
	 * deeper stack.  Asserting the removed sentence alongside the unchanged value is what keeps a future reader from
	 * re-introducing either half.
	 */
	@Test void a01b_sourceShape_depthCapIsTwoAndPromisesNoUnlimitedFollowOn() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("const MAX_DIALOG_DEPTH = 2;"), "the cap must still be exactly 2");
		assertFalse(body.contains("Unlimited dialog depth"),
			"the unlimited-dialog-depth follow-on comment must be gone, not re-worded");
		assertFalse(body.contains("10.0"), "no 10.0 follow-on promise anywhere in the runtime");
	}

	@Test void a02_sourceShape_timestampIsNotALayerStackClient() throws Exception {
		// SF-3: the timestamp popup stays a plain show/hide element - it must never become a pushLayer client.
		var body = viewsJs();
		assertTrue(body.contains("function hideTsPopupIfPresent("), body);
		assertFalse(body.contains("kind:\"timestamp\"") || body.contains("kind: \"timestamp\""),
			"timestamp popup must not register as a layer-stack kind");
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
			var p = Path.of(basedir, "src/test/js/popup-layer.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/popup-layer.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/popup-layer.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("popup-layer-stdout-", ".json");
		var stderr = Files.createTempFile("popup-layer-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("popup-layer.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("popup-layer.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or popup-layer.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_pushPopOrdering_andPerDepthZIndex() {
		var r = report();
		assertEquals(true, r.get("push_topIsB"));
		assertEquals(true, r.get("push_portalledToBody"));
		assertEquals(true, r.get("z_increasesPerDepth"));
		assertEquals(true, r.get("z_dataLayerIndex"));
		assertEquals(true, r.get("pop_topIsA"));
		assertEquals(true, r.get("pop_empty"));
	}

	@Test void b02_escapeDismissesOnlyTheTopLayer() {
		var r = report();
		assertEquals(1.0, toD(r.get("dc_afterOuter")));
		assertEquals(2.0, toD(r.get("dc_afterInner")));
		assertEquals(true, r.get("esc_preventDefault"));
		assertEquals(true, r.get("esc_popsOne"));
		assertEquals(true, r.get("esc_innerDetached"));
		assertEquals(true, r.get("esc_outerStays"));
	}

	@Test void b03_perDialogPopDoesNotRemoveSiblingBackdrop() {
		var r = report();
		assertEquals(true, r.get("sibling_outerBackdropSurvives"));
	}

	@Test void b04_outsideClickDismissesOnlyLightDismissTop() {
		var r = report();
		assertEquals(true, r.get("modal_notLightDismissed"));
		assertEquals(true, r.get("popover_lightDismissed"));
	}

	@Test void b05_focusTrapEntryAndRestoreOnPop() {
		var r = report();
		assertEquals(true, r.get("trap_focusMovedIntoDialog"));
		assertEquals(true, r.get("focus_restoredToTrigger"));
	}

	@Test void b06_registryRegression_popoverDoesNotConsumeDialogCap() {
		var r = report();
		assertEquals(true, r.get("reg_dialogCountStill1"));
		assertEquals(true, r.get("reg_topIsPopover"));
		assertEquals(true, r.get("reg_dialogRemains"));
	}

	@Test void b07_nestedActionOpensSecondDialog_thirdRefusedInsideTop() {
		var r = report();
		assertEquals(1.0, toD(r.get("nested_before")));
		assertEquals(2.0, toD(r.get("nested_after")));
		assertEquals(true, r.get("nested_outerStillOpen"));
		assertEquals(2.0, toD(r.get("cap_max")));
		assertEquals(true, r.get("cap_staysAt2"));
		assertEquals(true, r.get("cap_refusalInTopDialog"));
		assertEquals(true, r.get("missing_actionRefusal"));
	}

	@Test void b08_rowActionMenuInScrollBoxIsPortalledOutAndDismissesViaStack() {
		// A row-action menu opened from a trigger inside a DT2 .dt-layout-cell overflow box must be portalled to the
		// body as a kind:"menu" position:fixed layer, so the scroll box's overflow cannot clip it, and its dismissal
		// must run through the shared stack (not a parallel closer).  A menu must never inflate the dialog-kind cap.
		var r = report();
		assertEquals(true, r.get("rowmenu_opened"), () -> "the row-action menu did not open: " + r);
		assertEquals(true, r.get("rowmenu_onBody"), () -> "the menu was not portalled to body: " + r);
		assertEquals(true, r.get("rowmenu_notInScrollBox"), () -> "the menu is still inside the .dt-layout-cell clip box: " + r);
		assertEquals(true, r.get("rowmenu_notInCell"), () -> "the menu is still inside its <td>: " + r);
		assertEquals(true, r.get("rowmenu_positionFixed"), () -> "the menu is not position:fixed: " + r);
		assertEquals(true, r.get("rowmenu_isMenuLayer"), () -> "the menu did not register as a kind:menu stack layer: " + r);
		assertEquals(true, r.get("rowmenu_notADialog"), () -> "the menu inflated the dialog-kind depth cap: " + r);
		assertEquals(true, r.get("rowmenu_closedViaStack"), () -> "an outside click did not dismiss the menu via the stack: " + r);
	}

	private static double toD(Object o) {
		return ((Number)o).doubleValue();
	}
}
