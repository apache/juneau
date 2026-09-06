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
 * WORK-J0522d, design test <b>35a</b> (§13.6): <b>a populate's own buttons do not reach {@code submitRowAction}</b>.
 *
 * <p>
 * {@code handleDetailActionRefClick} is bound at the <b>table</b> level and matches {@code [data-juneau-action]}
 * anywhere beneath it. A region container inside a detail panel <i>is</i> beneath it. So absent an explicit
 * exclusion, a populate that paints its own action button &mdash; the most ordinary thing a populate does &mdash;
 * has that click silently routed into the framework's write path: a POST with no dialog seam, no confirmation, no
 * idempotency key, and a {@code targetId} lifted from whatever row the panel belongs to.
 *
 * <p>
 * This is the click-side twin of the emit-side negative test, and it is specifically the test that makes the
 * downstream consumer's hand-rolled capture-phase intercept <b>deletable</b>. That intercept exists only to stop
 * this leak, so it cannot be retired on the strength of a source-shape pin &mdash; retiring it needs a real click
 * proving the framework stays out of the way.
 *
 * <p>
 * Behavioral layer, driven through the always-on Node harness {@code detail-region-action-isolation.cjs} against
 * the <b>real</b> {@code juneau-views.js}, in the shape {@link ViewsJs_DetailActionDialogRouting_Test} established:
 * it runs whenever {@code node} is on {@code PATH} and is skipped (not failed) otherwise, so the gate exercises it
 * on any developer or CI machine with node while never becoming a hard toolchain dependency.
 *
 * <h5 class='section'>Every claim here is paired with an inverted control</h5>
 * <p>
 * "No fetch happened" is the assertion a <b>broken harness</b> satisfies most easily &mdash; a fixture that never
 * wires the delegate, or an action id that never resolves, produces zero fetches for entirely the wrong reason. So
 * each no-fetch claim below has a sibling running the <i>same</i> fixture builder with the button moved out of the
 * region, asserting the framework <b>does</b> submit. The pair is the evidence; either half alone is not.
 */
class ViewsJs_DetailRegionActionIsolation_Test extends TestBase {

	private static final String HARNESS = "detail-region-action-isolation.cjs";

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
		var stdout = Files.createTempFile("detail-region-action-isolation-stdout-", ".json");
		var stderr = Files.createTempFile("detail-region-action-isolation-stderr-", ".txt");
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

	private static long num(Map<?,?> r, String k) {
		var v = r.get(k);
		assertNotNull(v, () -> "harness did not report '" + k + "'; it reported: " + r.keySet());
		return ((Number) v).longValue();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The harness reached the real runtime at all.  Without this, every no-fetch assertion below is vacuous.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_harnessLoadedTheRealRuntime() {
		assertEquals(true, report().get("hasInit"),
			"the harness could not see initDetailsExpander/submitRowAction on the real juneau-views.js, so nothing "
				+ "below is exercising the production click path");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) TEST 35a, THE CLAIM - and its inverted control, which is what makes the claim mean anything.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_populateButtonInsideADetailRegion_reachesNoWritePath() {
		var r = report();
		assertEquals(0L, num(r, "regionButton_fetches"),
			"a populate's own [data-juneau-action] button inside a detail-panel region must produce NO network "
				+ "request - reaching submitRowAction here is a POST with no dialog, no confirmation and no "
				+ "idempotency key, on a targetId the populate never chose");
		assertEquals(0L, num(r, "regionButton_dialogs"),
			"nor may it open the framework's action dialog: the framework does not act on author-drawn DOM at all");
	}

	@Test void b02_control_theSameButtonOutsideTheRegion_doesSubmit() {
		// The evidence for b01.  Same fixture builder, same action, button moved out of the region.  If this were
		// also zero, b01 would be passing because the harness never submits - not because the exclusion works.
		var r = report();
		assertEquals(1L, num(r, "control_panelButton_fetches"),
			"the control must submit; if it does not, the fixture is broken and b01 proves nothing");
		assertEquals(true, r.get("control_panelButton_prevented"),
			"the framework's own detail button is still framework-owned, so the delegate must claim the event");
	}

	@Test void b03_theFrameworkDeclinesToActWithoutAlsoInterfering() {
		// Returning "handled" while still calling preventDefault/stopPropagation would be the subtler bug: no POST,
		// but the populate's own click handler and the element's native behavior are dead too.  Declining to act
		// must also mean declining to interfere.
		var r = report();
		assertEquals(true, r.get("regionButton_notPrevented"),
			"preventDefault must NOT be called for a click the framework has decided is not its business");
		assertEquals(true, r.get("regionButton_notStopped"),
			"stopPropagation must NOT be called either, or the author's own listener never sees the click");
	}

	@Test void b04_theDialogPathIsExcludedToo_notJustTheDirectSubmit() {
		var r = report();
		assertEquals(0L, num(r, "regionDialogButton_fetches"));
		assertEquals(0L, num(r, "regionDialogButton_dialogs"),
			"a present=dialog action id colliding with a populate's own button must not open the framework dialog");
		assertEquals(1L, num(r, "control_panelDialogButton_dialogs"),
			"control: the same dialog action outside a region DOES open the dialog, so the exclusion is what "
				+ "suppressed it above and not a broken dialog seam");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) The two boundary conditions of the exclusion, which are the parts most likely to be "simplified" later.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_aButtonCarryingTheRegionMarkerItself_isStillAuthorDom() {
		// The self-match bypass.  An earlier form of the guard read `region !== el`, on the theory that a
		// self-match was degenerate.  It is not: a populate painting
		// `<button data-juneau-action="x" data-juneau-region="y">` self-matches, was therefore treated as NOT in a
		// region, and had its click routed straight into submitRowAction - a live POST.  Verified by running this
		// harness against a build with the old guard restored, where this value is 1 instead of 0.
		//
		// The guard cannot lose the self-match again without this failing.  Nothing the framework emits carries
		// both attributes (RegionTable.of() emits a bare <div>), so including the self-match costs nothing.
		assertEquals(0L, num(report(), "selfMarkedRegionButton_fetches"),
			"a button that itself carries data-juneau-region must be treated as inside a region, not as a "
				+ "degenerate self-match that escapes the exclusion");
	}

	@Test void c02_aRegionHostingTheWholeTable_doesNotDisableThePanelsOwnButtons() {
		// The scope bound, in the direction that breaks FEATURES rather than security.  A whole view may be hosted
		// inside a region (that is the point of the region primitive), and `closest` walks all the way to the
		// document.  An unscoped lookup would find that ancestor region and silently disable every detail action
		// button in every region-hosted view.  The `scopeEl.contains(region)` bound is what prevents it: only a
		// region NESTED INSIDE the panel suppresses, never one above the table.
		var r = report();
		assertEquals(1L, num(r, "regionHostedView_panelButtonStillFetches"),
			"a view hosted inside a region must keep working detail action buttons - only a region INSIDE the "
				+ "panel excludes, never an ancestor region above the table");
		assertEquals(true, r.get("regionHostedView_panelButtonStillPrevented"));
	}
}
