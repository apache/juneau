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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the page-scoped INITIAL-BROADCAST BARRIER and its bounded replay window.
 *
 * <p>
 * The barrier exists for one shape: a control region that emits during its own {@code reason:"initial"} populate,
 * before the regions that care have subscribed.  Buffering that emit is the easy half.  The hard half is RELEASING
 * the buffer, and the failure the design rates worse than the bug it replaces is a barrier that never lifts - a page
 * where all subsequent user-driven traffic is frozen for the life of the tab, with a clean console.  That is why
 * three separate non-settlement modes each have their own test, why the arming protocol has a loud backstop, and why
 * several assertions here are against the CLOCK rather than against delivery order: for a hidden enrolled card,
 * delivery is correct under both the right and the wrong reading and only the latency differs, so an order-based
 * assertion passes either way.
 *
 * <p>
 * Behavioral half: {@code src/test/js/regions-barrier.cjs}.
 */
class Regions_Barrier_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("regions-barrier.cjs");
		assumeTrue(r != null, "node not available or regions-barrier.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static void assertNothingReceived(Map<?,?> r, String key) {
		assertEquals(List.of(), r.get(key), () -> key + " must have received nothing, got " + r.get(key));
	}

	/** Every reading in {@code key} is the given clock time - the assertion that catches a needless page-wide wait. */
	private static void assertDeliveredAt(Map<?,?> r, String key, int expectedMs) {
		var readings = (List<?>)r.get(key);
		assertFalse(readings.isEmpty(), () -> key + " recorded no delivery at all");
		for (var at : readings)
			assertEquals(expectedMs, ((Number)at).intValue(),
				() -> key + " delivered at " + readings + ", expected every delivery at " + expectedMs + "ms");
	}

	// =================================================================================================================
	// 49 - the case the barrier exists for.
	// =================================================================================================================

	/**
	 * A control region that emits during its own {@code reason:"initial"} populate AND INITIALIZES FIRST still reaches
	 * three chart regions that subscribe during their own later initial populates - including one whose populate
	 * returns a Promise settling after the control's, which is what proves the barrier waits on ASYNC populates and
	 * not merely on synchronous return.  A region activated LATER receives nothing retroactively: the design declines
	 * replay-on-subscribe for a region that was never a barrier member.
	 */
	@Test void b49_aControlEmittingDuringItsOwnInitialPopulateReachesLaterSubscribers() {
		var r = report();
		assertEquals(List.of(List.of(), List.of(), List.of()), r.get("t49a_beforeAsyncSettle"),
			"nothing may be delivered while an initial populate is still in flight");
		assertEquals(List.of(List.of("M1"), List.of("M1"), List.of("M1")), r.get("t49a_afterAsyncSettle"), r::toString);
		assertDeliveredAt(r, "t49a_drainedAtZeroNotDeadline", 0);
		assertAllTrue(r, "t49a_noDeadlineWarning", "t49a_controlDidNotReceiveItsOwn");
		assertNothingReceived(r, "t49b_lateActivatedGetsNothing");
	}

	// =================================================================================================================
	// 49a / 49b / 49c - the three non-settlement modes, each with its own test.
	// =================================================================================================================

	/** Mode 1: a SYNCHRONOUSLY-THROWING initial populate releases the barrier. */
	@Test void b49a_aSynchronousThrowReleasesTheBarrier() {
		var r = report();
		assertEquals(List.of(List.of(), List.of()), r.get("t49a1_heldUntilThirdSettles"), r::toString);
		assertEquals(List.of(List.of("M1"), List.of("M1")), r.get("t49a1_drainedOnThirdSettle"), r::toString);
		assertDeliveredAt(r, "t49a1_drainedAtZero", 0);
		assertEquals("error", r.get("t49a1_throwerErrored"));
		assertEquals(true, r.get("t49a1_noDeadlineNeeded"), "a throw must not make the page wait for the deadline");
	}

	/**
	 * Mode 2: a REJECTED initial populate releases the barrier.  Worth a separate test from the synchronous throw
	 * because the natural implementation - a {@code try} around the call - catches the throw and NOT the rejection,
	 * and the symptom of missing the second is a page-wide freeze with a green-looking console.
	 */
	@Test void b49b_aRejectedPromiseReleasesTheBarrier() {
		var r = report();
		assertNothingReceived(r, "t49b_heldWhileRejectionPending");
		assertEquals(List.of("M1"), r.get("t49b_drainedOnRejection"), r::toString);
		assertDeliveredAt(r, "t49b_drainedAtZero", 0);
		assertEquals("error", r.get("t49b_rejectorErrored"));
		assertEquals(true, r.get("t49b_noDeadlineNeeded"), r::toString);
	}

	/**
	 * Mode 3: a NEVER-SETTLING initial populate releases the barrier AT THE DEADLINE, with a warning naming the
	 * region.  The barrier is an ORDERING device and not a lifecycle one, so the hung populate is not aborted and its
	 * container is left exactly as it left it; and traffic AFTER the release flows normally rather than staying frozen
	 * for the life of the page.
	 */
	@Test void b49c_aNeverSettlingPopulateReleasesTheBarrierAtTheDeadline() {
		var r = report();
		assertNothingReceived(r, "t49c_heldBeforeDeadline");
		assertNothingReceived(r, "t49c_stillHeldAt1999");
		assertEquals(List.of("M1"), r.get("t49c_drainedAtDeadline"), r::toString);
		assertDeliveredAt(r, "t49c_deliveredAt", 2000);
		assertAllTrue(r, "t49c_warningNamesRegion", "t49c_oneWarningForThePage");
		assertAllTrue(r, "t49c_hungNotAborted", "t49c_hungContainerUntouched");
		assertEquals("loading", r.get("t49c_hungStillLoading"));
		assertEquals(List.of("M1", "M2"), r.get("t49c_laterTrafficFlows"),
			"traffic after the release must flow, not stay frozen for the life of the page");
	}

	/**
	 * The deadline is a CLAMPED CONSTANT: not settable through the published namespace, a global, or an attribute on
	 * the region or its host.  An author-settable barrier deadline is a way to configure a page-wide freeze.
	 */
	@Test void b49d_theDeadlineIsNotConfigurable() {
		var r = report();
		assertEquals(2000, ((Number)r.get("t49d_value")).intValue());
		assertNothingReceived(r, "t49d_notReleasedAtFive");
		assertEquals(List.of("M1"), r.get("t49d_releasedAtTwoThousand"), r::toString);
		assertDeliveredAt(r, "t49d_deliveredAt", 2000);
	}

	// =================================================================================================================
	// 49e / 49f - page scope and the arming protocol.
	// =================================================================================================================

	/**
	 * THE TWO-HOST CASE.  A page carrying both a card grid and a tabbed page shell, whose two runtimes boot
	 * independently and hold no reference to each other: the barrier is PAGE-scoped, so the grid's control card still
	 * reaches a chart in the page shell's initial panel.  Under a host-scoped barrier this fails, and it fails
	 * INTERMITTENTLY - on boot order - which is the worst way for it to fail.  Late activation gets nothing whether
	 * the late region is a naked tab body or a card in a non-initial panel, so that rule is about activation and not
	 * about which runtime enrolled the region.
	 */
	@Test void b49e_theBarrierIsPageScopedAcrossTwoIndependentRuntimes() {
		var r = report();
		assertNothingReceived(r, "t49e_notDrainedAfterFirstRuntime");
		assertEquals(List.of("M1"), r.get("t49e_i_gridChart"), r::toString);
		assertEquals(List.of("M1"), r.get("t49e_i_panelChart"),
			"a page-scoped barrier reaches the other runtime's regions");
		assertDeliveredAt(r, "t49e_i_drainedAtZero", 0);
		assertNothingReceived(r, "t49e_ii_lazyTab");
		assertNothingReceived(r, "t49e_iii_lazyCard");
	}

	/**
	 * THE ARMING PROTOCOL.  (i) An EMPTY outstanding set is not enough - the barrier holds until every registered
	 * runtime has declared its enrolment walk complete, because "no regions outstanding" and "no regions enrolled
	 * yet" are indistinguishable from the barrier's side.  (ii) A page with NO regions pays nothing: nothing arms, no
	 * timer is scheduled, no document listener is added.  (iii) A registered runtime that NEVER reports has the
	 * barrier lift one macrotask after DOMContentLoaded with exactly one error naming it - it degrades LOUDLY, which
	 * is the inverse of the failure mode a clock-only design was rejected for.
	 */
	@Test void b49f_theArmingProtocolHoldsOnAnEmptySetAndHasALoudBackstop() {
		var r = report();
		assertNothingReceived(r, "t49f_i_heldOnEmptyOutstanding");
		assertEquals(List.of("M1"), r.get("t49f_i_liftedOnSecondReport"), r::toString);
		assertDeliveredAt(r, "t49f_i_liftedAtZero", 0);

		assertAllTrue(r, "t49f_ii_noTimers", "t49f_ii_noDocumentListeners", "t49f_ii_assetLoadedButUnarmed");

		assertNothingReceived(r, "t49f_iii_heldBeforeMacrotask");
		assertEquals(List.of("M1"), r.get("t49f_iii_liftedAtMacrotask"), r::toString);
		assertDeliveredAt(r, "t49f_iii_liftedAtZeroNotDeadline", 0);
		assertAllTrue(r, "t49f_iii_oneErrorNamingRuntime", "t49f_iii_errorMentionsReady");
	}

	// =================================================================================================================
	// 49g / 49h / 49i / 49l - the released-region replay window.
	// =================================================================================================================

	/**
	 * THE REPLAY WINDOW DELIVERS.  The healthy regions receive the broadcast at the deadline - no page-wide wait -
	 * and the slow region's LATER {@code ctx.on} receives the same messages, in the same order, exactly once.  The
	 * assertion that distinguishes a bounded replay window from a retained cache is the last one: a subsequent LIVE
	 * emit arrives after the replayed ones and is not duplicated.
	 */
	@Test void b49g_aReleasedRegionsLaterSubscribeReceivesWhatItMissed() {
		var r = report();
		assertNothingReceived(r, "t49g_heldBeforeDeadline");
		assertEquals(List.of("M1"), r.get("t49g_healthyGotItAtDeadline"), r::toString);
		assertDeliveredAt(r, "t49g_healthyAt", 2000);
		assertNothingReceived(r, "t49g_slowNothingYet");
		assertEquals(List.of("M1"), r.get("t49g_slowReplayed"), r::toString);
		assertEquals(true, r.get("t49g_slowReplayedOnce"), r::toString);
		assertEquals(List.of("M1", "M2"), r.get("t49g_slowAfterLiveEmit"),
			"a live emit lands after the replayed ones and is not duplicated");
		assertEquals(List.of("M1", "M2"), r.get("t49g_healthyAfterLiveEmit"), r::toString);
	}

	/**
	 * THE WINDOW CLOSES, AND IS NOT A RETAINED CACHE.  Four negatives, because each is a way the replay could grow
	 * into the retained-cache option the design rejected: a region enrolled after the lift gets nothing; a released
	 * region that subscribes after its OWN populate settles gets nothing; the buffer is discarded once the last
	 * window closes; and the replay reads NO payload property - which is what keeps payload opacity true on the
	 * second delivery path, and precisely the objection the retained-cache option failed.
	 */
	@Test void b49h_theReplayWindowClosesAndIsNotARetainedCache() {
		var r = report();
		assertNothingReceived(r, "t49h_i_lateEnrolledGetsNothing");
		assertNothingReceived(r, "t49h_ii_afterOwnSettleGetsNothing");
		assertNothingReceived(r, "t49h_iii_freshRegionGetsNothing");
		assertAllTrue(r, "t49h_iv_replayReadsNothing", "t49h_iv_replayedIdentityPreserved");
		assertEquals(List.of("M1"), r.get("t49h_iv_replayed"), r::toString);
	}

	/**
	 * A REPLAY IS A FAN-OUT: a replayed handler that emits has its emission QUEUED like any other, and a second
	 * released region whose window is still open receives it.  Run as two arms of the same page - one where both
	 * regions are deadline-released and one where both settle before the deadline - and the expected sequences are
	 * DERIVED by requiring the two arms to agree, rather than asserted as literals.  That is what keeps the replay
	 * path from drifting out of agreement with ordinary delivery: a literal would let both drift together.
	 */
	@Test void b49i_replayIsAFanOutAndAgreesWithOrdinaryDelivery() {
		var r = report();
		assertEquals(List.of("A-had-M2-at-emit-return:false"), r.get("t49i_i_emitWasQueuedNotLive"),
			"an emit from a replayed handler must be queued, not delivered live inside the replay");
		assertEquals(List.of("M1", "M2"), r.get("t49i_ii_cReceivedBoth"),
			"a still-open second window receives what the replay emits");
		assertEquals(true, r.get("t49i_iii_identicalSequences"),
			() -> "released arm " + r.get("t49i_iii_released") + " != control arm " + r.get("t49i_iii_control"));
		assertNothingReceived(r, "t49i_iv_lateEnrolled");
		assertNothingReceived(r, "t49i_iv_hiddenAtBoot");
		assertEquals(true, r.get("t49i_noCycleError"), r::toString);
	}

	/**
	 * THE LAST-WINDOW CLOSE, with a genuinely SYNCHRONOUS settle - a thenable whose {@code then} runs the framework's
	 * continuation in the caller's own turn, so a released region's close condition becomes true PARTWAY THROUGH a
	 * live fan-out.  Resolving an ordinary promise from inside a subscriber cannot reach this case at all: resolving
	 * queues a microtask while the fan-out is synchronous, so the continuation runs after the drain has finished and
	 * the assertion silently tests the ordinary case instead.
	 *
	 * <p>
	 * Membership is snapshotted at the fan-out's START, so the message IS in the buffer that region replays - had the
	 * close taken effect immediately it would have replayed nothing - and the buffer's contents depend on message
	 * order and member-open-time rather than on the order subscribers happen to run in, which is what the two
	 * subscription-order arms check.
	 */
	@Test void b49l_aWindowThatClosesMidFanOutStillCountedForThatFanOut() {
		var r = report();
		assertEquals(List.of("M3"), r.get("t49l_i_cReplayedM3"), r::toString);
		assertEquals(true, r.get("t49l_i_exactlyOnce"), r::toString);
		assertEquals(true, r.get("t49l_ii_orderIndependent"),
			() -> "the two subscription-order arms disagreed: " + r.get("t49l_ii_arms"));
		assertEquals(List.of("M3", "M4"), r.get("t49l_iii_cGotM4Live"), r::toString);
		assertEquals(true, r.get("t49l_iv_bufferDiscarded"),
			"the buffer must be discarded once the last window closes");
	}

	// =================================================================================================================
	// 49j / 49k - membership axes and the two independent caps.
	// =================================================================================================================

	/**
	 * ENROLLED-BUT-HIDDEN IS NOT A BARRIER MEMBER: enrolment and barrier membership are different axes, and getting
	 * them confused means every tabbed card page waits the full deadline before the visible card gets its initial
	 * broadcasts.  The first assertion is the one with teeth and it is against the CLOCK, because delivery is correct
	 * under both readings and only the latency differs.  The hidden region IS enrolled - it has a handle and a
	 * reachable teardown - it has simply had no populate at all, which is WHY it is not outstanding rather than a
	 * special case; and activating it later produces exactly one {@code reason:"activate"} populate that receives
	 * nothing retroactively.
	 */
	@Test void b49j_anEnrolledButHiddenRegionIsNotABarrierMember() {
		var r = report();
		assertDeliveredAt(r, "t49j_i_deliveredAt", 0);
		assertEquals(List.of("M1"), r.get("t49j_i_visibleGotIt"), r::toString);
		assertEquals(0, ((Number)r.get("t49j_i_clockAtDelivery")).intValue(),
			"a hidden card must not delay the drain to the deadline");
		assertEquals(true, r.get("t49j_i_noDeadlineWarning"), r::toString);

		assertAllTrue(r, "t49j_ii_enrolled", "t49j_ii_teardownReachable");
		assertEquals("hiddenCard", r.get("t49j_ii_hasKey"));
		assertEquals("idle", r.get("t49j_iii_state"));
		assertEquals(true, r.get("t49j_iii_noContent"), r::toString);

		assertEquals(List.of("activate"), r.get("t49j_iv_reasons"), r::toString);
		assertNothingReceived(r, "t49j_iv_receivedNothing");
		assertEquals(true, r.get("t49j_iv_activateOnce"), r::toString);
	}

	/**
	 * THE REPLAY BUFFER'S OVERFLOW IS NOT A CYCLE ERROR.  257 legitimate, non-cyclic, user-driven broadcasts, each
	 * from a different emitter at the top of its own turn, so no fan-out is ever nested.
	 *
	 * <p>
	 * (i) No cycle error, at any point.  Under a single shared cap this page raises one at append 65 - a false
	 * diagnostic naming a defect that does not exist, which is worse than silence.  (ii) A region that settles at
	 * append 200 replays exactly the first 200 in order, exactly once, so two regions on the same page reach two
	 * different outcomes.  (iii) At append 257 the buffer is ABANDONED WHOLE and loudly: the still-open window
	 * receives NO replay rather than a truncated one, because a run of messages with a hole is the outcome the design
	 * rates worse than none - the region cannot detect it - and exactly one error names the affected key, the cap, the
	 * exactly-once failure, and the fact that this is not a cycle.  (iv) The two caps are independent in BOTH
	 * directions: this page overflowed its buffer without a cycle error, and a genuinely nesting page raises the cycle
	 * error without overflowing a buffer.  Under one shared constant one of those two must fail.
	 */
	@Test void b49k_theTwoCapsAreIndependentAndOverflowIsNotReportedAsACycle() {
		var r = report();
		assertEquals(true, r.get("t49k_bothReleased"), r::toString);

		assertEquals(true, r.get("t49k_i_noCycleError"),
			"257 top-of-turn broadcasts contain no cycle and must not be reported as one");

		assertEquals(200, ((Number)r.get("t49k_ii_dReplayCount")).intValue(), r::toString);
		assertEquals("m1", r.get("t49k_ii_dReplayFirst"));
		assertEquals("m200", r.get("t49k_ii_dReplayLast"));
		assertEquals(true, r.get("t49k_ii_dInOrder"), r::toString);

		assertAllTrue(r, "t49k_iii_exactlyOneOverflowError", "t49k_iii_namesKey", "t49k_iii_namesCap",
			"t49k_iii_namesExactlyOnce", "t49k_iii_saysNotACycle", "t49k_iii_namesOnlyTheOpenWindow",
			"t49k_iii_noPartialReplay");
		assertNothingReceived(r, "t49k_iii_cReceivedNoReplay");

		assertAllTrue(r, "t49k_iv_bufferOverflowedWithoutCycleError", "t49k_iv_capsDiffer",
			"t49k_iv_nestingRaisesCycleError", "t49k_iv_nestingRaisedNoOverflowError");
	}
}
