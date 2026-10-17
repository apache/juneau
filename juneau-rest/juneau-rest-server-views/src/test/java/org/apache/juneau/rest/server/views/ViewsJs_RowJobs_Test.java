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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Node behavioral checks for {@code JuneauViews.jobs} (row-bound jobs from a row or its detail panel), the shared row
 * resolver behind {@code hookContextOf}, and {@code helpers.jobButton}.
 */
class ViewsJs_RowJobs_Test {

	private static Map<?,?> r() {
		var r = NodeHarness.report("jobs.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.HELPERS_JS_RESOURCE);
		assumeTrue(r != null, "node (or jobs.cjs) not available - behavioral layer skipped");
		return r;
	}

	private static Object o(String k) { return r().get(k); }
	private static String s(String k) { return (String)r().get(k); }
	private static int n(String k) { return ((Number)r().get(k)).intValue(); }
	private static void yes(String...keys) { for (var k : keys) assertEquals(true, r().get(k), k); }

	@Test void j01_actionEnvelopeFromAPanelKeysSubmitsStreamsFromStartAndReleases() {
		yes("j01_triggerDisabled", "j01_pendingHoldsPoll", "j01_keyCall", "j01_keyCsrf");
		assertEquals("{}", s("j01_keyBody"));
		assertEquals("/species/r1/restock", s("j01_submitUrl"));
		assertEquals(Map.of("action", "restock-species", "targetId", "r1", "idempotencyKey", "k-1", "fields", Map.of("count", 2)), o("j01_submitBody"));
		assertEquals("/s/j-1?lastEventId=0", s("j01_streamUrl"));
		yes("j01_marker", "j01_activeMatches", "j01_activeOtherAction", "j01_activeOtherRow", "j01_runningHoldsPoll");
		assertEquals("succeeded", s("j01_state"));
		assertEquals("onStart,onProgress,onResult,onEnd", s("j01_order"));
		assertEquals("half way", s("j01_progressArg"));
		assertEquals("restocked", s("j01_resultMessage"));
		yes("j01_endFrozen", "j01_handleFrozen", "j01_markerCleared", "j01_activeAfter", "j01_pollReleased", "j01_triggerEnabled", "j01_closed");
	}

	@Test void j02_flatEnvelopeWithAGivenKeySettlesASynchronousResult() {
		assertEquals("succeeded", s("j02_state"));
		assertEquals(1, n("j02_calls"));
		assertEquals(Map.of("action", "job", "a", 1, "idempotencyKey", "given-1"), o("j02_body"));
		assertEquals("onResult,onEnd", s("j02_order"));
		yes("j02_noStream");
	}

	@Test void j03_onlyTheIdTokenSubstitutes() {
		assertEquals("refused", s("j03_state"));
		assertEquals("unresolved-endpoint", s("j03_reason"));
		assertEquals(0, n("j03_calls"));
		assertEquals("unresolved-endpoint", s("j03_keyReason"));
		assertEquals(0, n("j03_keyCalls"));
		assertEquals("/species/a%20b/x", s("j03_encoded"));
		assertEquals("empty-substitution", s("j03_rowlessToken"));
	}

	@Test void j04_noRowIdRefusesAnUnstampedRowAndAMismatchedPanel() {
		assertEquals("no-row-id", s("j04_unstamped"));
		assertEquals("no-row-id", s("j04_mismatch"));
		assertEquals("refused", s("j04_state"));
		assertEquals(0, n("j04_calls"));
	}

	@Test void j05_oneJobPerRow() {
		assertEquals("row-job-running", s("j05_reason"));
		yes("j05_firstPending", "j05_otherRowNotRefused");
	}

	@Test void j06_missingTokenRefusesBeforeAnyRequestAndFreesTheRow() {
		assertEquals("missing-token", s("j06_reason"));
		assertEquals(0, n("j06_calls"));
		yes("j06_released");
	}

	@Test void j07_fetchKeyChecksTheAnswer() {
		assertEquals("k-1", s("j07_ok"));
		for (var k : List.of("j07_otherRow", "j07_wrongAction", "j07_wrongContract", "j07_blankKey", "j07_http", "j07_outsideRow"))
			assertEquals("E-JS-109", s(k), k);
		assertEquals(6, n("j07_logged"));
		assertEquals("key-refused", s("j07_runReason"));
		yes("j07_runNoSubmit");
	}

	@Test void j08_keyIsKeptOnTransportFailureAndDiscardedOnAnAnswer() {
		assertEquals("request-failed", s("j08_firstReason"));
		assertEquals("http:500", s("j08_secondReason"));
		assertEquals(1, n("j08_keysAfterTwo"));
		assertEquals("succeeded", s("j08_thirdState"));
		assertEquals(2, n("j08_keysAfterThree"));
		yes("j08_retryReusedKey", "j08_freshKeyAfterAnswer");
	}

	@Test void j09_typedRefusalAndFailureMapToTheirStates() {
		assertEquals("refused", s("j09_refused"));
		assertEquals("This key is not valid for this row.", s("j09_refusedResult"));
		assertEquals("failed", s("j09_failed"));
	}

	@Test void j10_paintNoneDrawsNothingButStillMarksTheRow() {
		yes("j10_noProgress", "j10_marker", "j10_noOutcome");
		assertEquals("succeeded", s("j10_state"));
	}

	@Test void j11_aThrowingHookIsLoggedAndTheJobContinues() {
		yes("j11_logged");
		assertEquals("succeeded", s("j11_state"));
		assertEquals("succeeded", s("j11_onEnd"));
	}

	@Test void j12_aRedrawReappliesTheMarkerToTheNewRow() {
		yes("j12_newRowMarked", "j12_otherRowUnmarked", "j12_newTriggerDisabled", "j12_active", "j12_newRowCleared");
	}

	@Test void j13_collapsedRowHoldsNoPollAndReconnectIsTolerated() {
		yes("j13_collapsedNoHold", "j13_reconnecting");
		assertEquals("error", s("j13_state"));
		assertEquals("stream-interrupted", s("j13_reason"));
	}

	@Test void j14_cancelPostsTheCancelUrl() {
		assertEquals(false, o("j14_cancelBeforeStart"));
		yes("j14_cancelPosted");
	}

	@Test void j15_abortEndsTheRunAndFreesTheRow() {
		assertEquals("aborted", s("j15_reason"));
		yes("j15_noCalls", "j15_streaming", "j15_streamClosed", "j15_released");
		assertEquals("aborted", s("j15_streamAborted"));
	}

	@Test void j16_jobButtonRunsThroughJobsAndLogsWhenItIsMissing() {
		yes("j16_class", "j16_posted", "j16_bodyHasNoLabel", "j16_missingLogged", "j16_missingSentNothing");
	}

	@Test void j17_pendingResolvesToTheRefOnceStartedOrNullWhenItEndsFirst() {
		yes("j17_noneBefore", "j17_pendingIsPromise", "j17_otherAction", "j17_noneWhileRunning", "j17_endedFirst");
		assertEquals("j-1", s("j17_ref"));
	}

	@Test void j18_anExpandedPanelsJobHoldsTheTimersDraw_andThePillSaysPaused_withoutTheOptIn() {
		assertEquals("fresh", s("j18_idleState"));
		yes("j18_unexpandedRowNoHold", "j18_pendingHolds", "j18_pendingTimerDrawCancelled", "j18_runningHolds",
			"j18_runningTimerDrawCancelled", "j18_userReloadNotHeld", "j18_otherPanelStillHeld", "j18_collapsedReleases",
			"j18_reexpandHolds", "j18_endReleases", "j18_endDraws");
		assertEquals("paused", s("j18_pendingState"));
		assertTrue(s("j18_runningLabel").startsWith("Paused"), s("j18_runningLabel"));
		assertEquals("fresh", s("j18_endState"));
	}

	@Test void j19_aRowActionsStreamNeverAdoptsAPendingRunsEntry() {
		yes("j19_notAdopted", "j19_stillPending", "j19_stillHolds", "j19_triggerStillDisabled", "j19_panelStreams", "j19_marker");
		assertEquals("j-1", s("j19_adopted"));
		assertEquals("j-1", s("j19_active"));
		assertEquals("succeeded", s("j19_state"));
	}

	@Test void j20_aRowActionsJobBlocksARunButHoldsNoPoll() {
		yes("j20_marker", "j20_noHold", "j20_released");
		assertEquals("row-job-running", s("j20_runRefused"));
	}

	@Test void h01_hookContextOfResolvesAPanelElementToItsParentRow() {
		yes("h01_parentTr", "h01_table", "h01_plainRow", "h01_nestedTr", "h01_resolverNested", "h01_resolverPanel");
		assertEquals("cat", s("h01_parentRow"));
		assertEquals("nested", s("h01_nestedRow"));
	}

	@Test void u01_historyUrlAndJobStates() {
		assertEquals("/s/j-1?lastEventId=0", s("u01_plain"));
		assertEquals("/s/j-1?a=1&lastEventId=0", s("u01_query"));
		assertEquals("succeeded,failed,refused,cancelled,error,error", s("u01_stateOf"));
	}
}
