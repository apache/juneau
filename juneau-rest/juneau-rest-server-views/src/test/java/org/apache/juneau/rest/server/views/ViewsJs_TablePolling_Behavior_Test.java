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
 * Always-on BEHAVIORAL coverage for poll suspension, driving the real {@code initPolling} /
 * {@code isPollSuspended} through {@code src/test/js/table-polling.cjs} against a controllable clock.
 *
 * <p>Deliberately separate from {@link TablePolling_Wiring_Test}, which pins the SHAPE of the same code by
 * substring. That idiom is the module's convention and is kept, but it cannot answer the two questions this
 * feature actually turns on:
 * <ul>
 * 	<li>Does a view that opted into NOTHING &mdash; i.e. every pre-existing consumer of this shared toolkit
 * 		&mdash; really still poll, with an open detail panel and an open dialog sitting right there? A wiring
 * 		test can confirm the opt-in check is written; only running it confirms the check is right.
 * 	<li>Does the age label really keep ADVANCING while suspended? A frozen clock is exactly how a BROKEN poll
 * 		looks, and keeping those two apart is the whole reason {@code initPolling} is shaped the way it is. No
 * 		substring assertion can distinguish a label that ticks from one that does not.
 * </ul>
 */
class ViewsJs_TablePolling_Behavior_Test extends TestBase {

	private static Map<?,?> report;

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
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
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			report = Json.to(runNode(harness, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
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
			var p = Path.of(basedir, "src/test/js/table-polling.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/table-polling.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/table-polling.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("table-polling-stdout-", ".json");
		var stderr = Files.createTempFile("table-polling-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("table-polling.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("table-polling.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or table-polling.cjs not found - behavioral layer skipped");
		return report;
	}

	//------------------------------------------------------------------------------------------------------------------
	// The pre-existing consumer that opted into nothing.  This is the multi-consumer safety claim, executed.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * A view declaring neither {@code pausePollingWhileEditing} nor a {@code pausePolling} toggle keeps polling
	 * through an open detail panel AND an open dialog. If this fails, IRS and the support console silently stopped
	 * refreshing tables on a row expansion.
	 */
	@Test void a01_aViewThatOptedIntoNothingStillPollsWithEditorsOpen() {
		var r = report();
		assertEquals(true, r.get("default_notSuspendedWithPanelAndDialogOpen"));
		assertEquals(true, r.get("default_pollStillFetchesWithEditorsOpen"));
	}

	/** ...and its pill still reads exactly what it always read, in the state it always read. */
	@Test void a02_aViewThatOptedIntoNothingKeepsItsExactPillText() {
		var r = report();
		assertEquals("Updated just now", r.get("default_initialLabel"));
		assertEquals("fresh", r.get("default_initialState"));
		assertEquals("Updated 7s ago", r.get("default_labelAfter7s"));
		assertEquals("fresh", r.get("default_stateAfter7s"));
		assertEquals(true, r.get("default_neverSaysPaused"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The opted-in view.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_anOpenDetailPanelSuspendsTheFetch_andClosingItResumes() {
		var r = report();
		assertEquals(true, r.get("optedIn_notSuspendedWhileIdle"));
		assertEquals(true, r.get("optedIn_pollsWhileIdle"));
		assertEquals(true, r.get("optedIn_suspendedWithPanelOpen"));
		assertEquals(true, r.get("optedIn_pollSkippedWithPanelOpen"));
		assertEquals(true, r.get("optedIn_resumedAfterPanelClosed"));
		// "Closing the editor resumes normal polling on the next interval without a manual refresh."
		assertEquals(true, r.get("optedIn_pollResumesAfterClose"));
	}

	@Test void b02_theSuspendedPillSaysPausedInADistinctState() {
		var r = report();
		assertEquals("Paused \u2014 updated 42s ago", r.get("paused_label"));
		assertEquals("paused", r.get("paused_state"));
	}

	/** The acceptance criterion that cannot be substring-tested: no frozen clock while held. */
	@Test void b03_theAgeKeepsAdvancingWhileSuspended() {
		var r = report();
		assertEquals(true, r.get("paused_ageAdvancesWhileHeld"));
		assertEquals("Paused \u2014 updated 52s ago", r.get("paused_labelLater"));
		assertEquals(true, r.get("render_tickKeepsRunningWhilePaused"));
		assertEquals(
			List.of("Paused \u2014 updated 1s ago", "Paused \u2014 updated 2s ago",
				"Paused \u2014 updated 3s ago", "Paused \u2014 updated 4s ago"),
			r.get("render_labelsWhilePaused"));
		// The render tick keeps running, but not one fetch went out.
		assertEquals(true, r.get("render_pollNeverFetchedWhilePaused"));
	}

	@Test void b04_resumingReturnsToTheOrdinaryFreshPill() {
		var r = report();
		assertEquals("fresh", r.get("resumed_stateAfterDraw"));
		assertEquals("Updated just now", r.get("resumed_labelAfterDraw"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The stale-marker hazard.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The regression this feature's first cut shipped and round-1 review caught. The
	 * {@code .juneau-view-detail-open} marker class is cleared only by the collapse/toggle CLICK handlers, never on
	 * {@code draw.dt}, and DataTables reuses row nodes in client-side mode - so after a refresh-button press, a
	 * sort, a search or a page the class outlives the panel it described. Suspending on the class froze the view's
	 * polling permanently with the pill stuck on "Paused - updated 14m ago": stale, and lying about being held on
	 * purpose. The first two assertions establish that the hazard is REAL (the class does survive, the panel does
	 * not), so the third is not vacuous.
	 */
	@Test void c01_anOrphanedDetailOpenMarkerDoesNotFreezePollingForever() {
		var r = report();
		assertEquals(true, r.get("stale_markerClassSurvivedTheRedraw"));
		assertEquals(true, r.get("stale_panelIsGone"));
		assertEquals(true, r.get("stale_notSuspendedByTheOrphanedMarker"));
		assertEquals(true, r.get("stale_pollStillRunsAfterOrphanedMarker"));
		assertEquals(true, r.get("stale_pillIsNotStuckOnPaused"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The dialog source and the manual toggle.
	//------------------------------------------------------------------------------------------------------------------

	/** A detached backdrop is a LEAKED stack entry, not an open dialog; counting it would freeze the view. */
	@Test void d01_anOpenDialogSuspends_butALeakedDetachedEntryDoesNot() {
		var r = report();
		assertEquals(true, r.get("dialog_suspendsWhileAttached"));
		assertEquals(true, r.get("dialog_leakedDetachedEntryDoesNotSuspend"));
		assertEquals(true, r.get("dialog_emptyStackDoesNotSuspend"));
	}

	@Test void d02_theManualToggleWorksWithoutTheEditorOptIn() {
		var r = report();
		assertEquals(true, r.get("manual_notSuspendedBeforePress"));
		assertEquals(true, r.get("manual_suspendsWithoutTheOptInFlag"));
		assertEquals(true, r.get("manual_pollSkipped"));
		assertEquals(true, r.get("manual_repaintHookInstalled"));
		assertEquals("Paused \u2014 updated just now", r.get("manual_labelAfterPress"));
		assertEquals("paused", r.get("manual_stateAfterPress"));
		assertEquals("fresh", r.get("manual_stateAfterResume"));
		assertEquals("Updated just now", r.get("manual_labelAfterResume"));
	}

	/**
	 * An explicit refresh (the ribbon button, paging, sorting, searching) is deliberately NOT gated by the pause -
	 * only the timer is. The pill has to stay honest about both facts at once: freshly drawn, still held.
	 */
	@Test void d03_anExplicitRefreshWhilePausedRedrawsAndStaysHonestlyPaused() {
		var r = report();
		assertEquals("Paused \u2014 updated just now", r.get("manual_labelAfterExplicitRedraw"));
		assertEquals("paused", r.get("manual_stateAfterExplicitRedraw"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error precedence.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * A pause the operator chose must not paper over a failure that happened to them: while both are true the pill
	 * keeps reporting the failure, which is the more urgent and the less obvious of the two. The error state stays
	 * reachable and stays recoverable - a successful draw clears it even without resuming.
	 */
	@Test void e01_aFailedPollKeepsSayingFailedWhilePaused_andStillRecovers() {
		var r = report();
		assertEquals("error", r.get("error_stateBeforePause"));
		assertEquals("Refresh failed - last updated just now", r.get("error_labelBeforePause"));
		assertEquals("error", r.get("errorAndPaused_state"));
		assertEquals("Refresh failed - last updated 5s ago", r.get("errorAndPaused_label"));
		assertEquals("paused", r.get("errorThenDraw_state"));
		assertEquals("Paused \u2014 updated just now", r.get("errorThenDraw_label"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// The in-flight race.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * The hole the three tick-time guards cannot close, and the one the feature most needs closed: a reload that
	 * left BEFORE the row was expanded is already gone, so skipping the next tick does not help - the damage is
	 * done by the previous one. Expanding during the last round trip of a 5s cycle otherwise looks like "expand
	 * did nothing", and anything typed in that window goes with the child row.
	 */
	@Test void f01_aReloadAlreadyInFlightCannotPaintOverAJustOpenedEditor() {
		var r = report();
		assertEquals(true, r.get("inflight_pollWentOut"));
		assertEquals(true, r.get("inflight_drawWasCancelled"));
		assertEquals(true, r.get("inflight_panelSurvived"));
	}

	/**
	 * A cancelled draw fires neither {@code draw.dt} nor {@code error.dt}. That is why the draw is dropped at
	 * {@code preDraw.dt} instead of aborting the XHR: an abort arrives as {@code error.dt} and would paint
	 * "Refresh failed" over a refresh that did not fail. The clock simply keeps climbing, which is the honest
	 * reading - the visible table really was not updated.
	 */
	@Test void f02_aCancelledDrawIsNotReportedAsAFailure() {
		var r = report();
		assertEquals("paused", r.get("inflight_stateAfterCancel"));
		assertEquals("Paused \u2014 updated 2s ago", r.get("inflight_labelAfterCancel"));
		assertEquals(true, r.get("inflight_drawLandsOnceEditorClosed"));
		assertEquals("fresh", r.get("inflight_stateAfterResume"));
	}

	/**
	 * The three things the cancel must never eat. Each is a way the fix could have become a worse bug than the
	 * race it closes: swallowing a refresh the operator explicitly asked for, silently dropping redraws for the
	 * consumers who opted into nothing, or extending the manual pause into a draw block when refresh-while-paused
	 * is exactly what that toggle is for.
	 */
	@Test void f03_theCancelIsNarrowlyScopedToTimerDrawsOnOptedInViews() {
		var r = report();
		assertEquals(true, r.get("explicit_userRefreshStillPaintsWithPanelOpen"));
		assertEquals(true, r.get("default_inflightDrawStillPaints"));
		assertEquals(true, r.get("manualPause_drawNotCancelledWithoutAnOpenPanel"));
	}
}
