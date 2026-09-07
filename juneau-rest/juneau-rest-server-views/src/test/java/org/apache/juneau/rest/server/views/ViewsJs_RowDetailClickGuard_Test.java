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
 * A click on one of a row's OWN interactive controls (the row-actions "..." trigger, a rendered link, a
 * selection checkbox) must NOT ALSO toggle the row's detail expansion.
 *
 * <p>
 * Bug report: clicking the row-level actions "..." button on a table with {@link RowDetailDef} both opened the
 * action menu AND expanded the row's detail - because {@code actionTriggerMarkup}'s trigger {@code <button>}
 * carries neither {@code data-juneau-action} nor {@code data-juneau-safe}, the two attributes the pre-existing
 * click-side guards ({@code handleDetailActionRefClick}, {@code handleDetailSafeCollapseClick}) recognize ahead
 * of {@code toggleDetailRow}. Its click fell through to {@code toggleDetailRow} completely unguarded. A
 * selection checkbox and a rendered {@code Column.href} link in the row body had the same gap.
 *
 * <p>
 * The fix is deliberately NOT "expand only via the chevron" - {@code RowDetailsExpander_Wiring_Test}'s
 * {@code c01_viewsCss_detailRowIsMarkedClickable} (whole-row {@code cursor: pointer}) and
 * {@code b02_initDetailsExpander_delegatesOneClickListener_offTheDetailRowMarkerClass} (the delegate scoped to
 * {@code tr.juneau-view-detail-row}, not a chevron-only target) pin whole-row-click-to-expand as the existing,
 * tested design. Instead, {@code isInteractiveRowControl} adds one guard in {@code toggleDetailRow}: a click
 * landing on a {@code button}, {@code a[href]}, {@code [role="button"]}, {@code input}, {@code select},
 * {@code textarea}, or {@code label} never ALSO toggles the row - leaving the rest of the row (including the
 * chevron cell, which carries no special click handling of its own) clicking exactly as before.
 *
 * <p>
 * Behavioral layer, driven through the always-on Node harness {@code row-detail-click-guard.cjs} against the
 * <b>real</b> {@code juneau-views.js}, in the shape {@link ViewsJs_DetailRegionActionIsolation_Test} established:
 * it runs whenever {@code node} is on {@code PATH} and is skipped (not failed) otherwise.
 *
 * <h5 class='section'>Every claim here is paired with an inverted control</h5>
 * <p>
 * "The gate was not reached" is the assertion a <b>broken harness</b> satisfies most easily - a fixture that
 * never wires the delegate, or never dispatches the click, produces zero gate calls for entirely the wrong
 * reason. So each no-toggle claim below has a sibling proving the SAME fixture shape DOES reach the gate when
 * the click lands on plain row body (or the chevron cell) instead.
 */
class ViewsJs_RowDetailClickGuard_Test extends TestBase {

	private static final String HARNESS = "row-detail-click-guard.cjs";

	private static Map<?,?> report;

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

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
			var p = Path.of(basedir, "src/test/js/" + HARNESS);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/" + HARNESS,
			"juneau-rest/juneau-rest-server-views/src/test/js/" + HARNESS
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("row-detail-click-guard-stdout-", ".json");
		var stderr = Files.createTempFile("row-detail-click-guard-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(HARNESS + " did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(HARNESS + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or " + HARNESS + " not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_harnessLoadedTheRealRuntime() {
		assertEquals(true, report().get("hasInit"),
			"the harness could not see initDetailsExpander on the real juneau-views.js, so nothing below is "
				+ "exercising the production click path");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Controls first: without these, every "does not toggle" claim below would pass against a harness that never
	// reaches the gate for ANY click.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_control_plainRowBodyClickStillReachesTheToggleGate() {
		assertEquals(true, report().get("control_plainCellClick_reachesGate"),
			"a bare click on plain row body must still reach toggleDetailRow's dt.row(tr) gate - the pinned "
				+ "whole-row-click design must be untouched by this guard");
	}

	@Test void b02_control_chevronCellClickAlsoReachesTheGate() {
		assertEquals(true, report().get("chevronClick_reachesGate"),
			"the dedicated chevron cell is not a specially-recognized target - it is ordinary row body that "
				+ "happens not to be an interactive control, so a click there must reach the gate exactly like "
				+ "any other row-body click");
	}

	//------------------------------------------------------------------------------------------------------------------
	// THE BUG and its two siblings (link, checkbox) - the claims the controls above make meaningful.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_rowActionsTriggerClickDoesNotAlsoToggleTheRow() {
		assertEquals(true, report().get("triggerClick_doesNotReachGate"),
			"clicking the row-actions \"...\" trigger must open ONLY its menu - it must never also toggle the "
				+ "row's detail expansion");
	}

	@Test void c02_linkClickDoesNotAlsoToggleTheRow() {
		assertEquals(true, report().get("linkClick_doesNotReachGate"),
			"a rendered link in the row body must navigate ONLY - it must never also toggle the row's detail "
				+ "expansion");
	}

	@Test void c03_checkboxClickDoesNotAlsoToggleTheRow() {
		assertEquals(true, report().get("checkboxClick_doesNotReachGate"),
			"a selection checkbox in the row body must select ONLY - it must never also toggle the row's detail "
				+ "expansion");
	}
}
