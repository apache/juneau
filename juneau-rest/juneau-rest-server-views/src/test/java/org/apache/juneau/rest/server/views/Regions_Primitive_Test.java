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
 * Always-on coverage for the {@code populate(ctx, container)} primitive in {@code juneau-regions.js}.
 *
 * <p>
 * The primitive is the one member of the region contract that every populator sees on every invocation, so its
 * surface is pinned as a GOLDEN rather than spot-checked: the whole {@code ctx} key set, the whole {@code declared}
 * key set and the whole {@code ids} key set are compared against literals, and a member that changes shape breaks
 * this test rather than surfacing later as an author-visible contract break.  The behavioral half runs the real
 * runtime source under a DOM shim and a controllable clock (see {@code src/test/js/regions-primitive.cjs}).
 */
class Regions_Primitive_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("regions-primitive.cjs");
		assumeTrue(r != null, "node not available or regions-primitive.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	// =================================================================================================================
	// Source shape.
	// =================================================================================================================

	/**
	 * The AbortController baseline is tested EXACTLY ONCE, at load.  A per-call feature test is what makes a degraded
	 * mode possible, and the design's answer to a missing AbortController is a refusal rather than a degraded mode.
	 */
	@Test void a01_sourceShape_abortControllerTestedOnceAtLoad() throws Exception {
		var body = RegionsHarness.regionsJs();
		var occurrences = body.split("typeof AbortController", -1).length - 1;
		assertEquals(1, occurrences, "the AbortController baseline must be feature-tested exactly once, at load");
		assertTrue(body.contains("const HAS_ABORT_CONTROLLER"), body);
	}

	/**
	 * {@code fork} is attached with {@code Object.defineProperty} on the INSTANCE and the region runtime never touches
	 * {@code AbortSignal.prototype}.  The lazy way to implement {@code fork()} is a prototype patch, which mutates a
	 * platform type for every script on the page including ones that have never heard of Juneau.
	 */
	@Test void a02_sourceShape_forkIsAnInstancePropertyNeverAPrototypePatch() throws Exception {
		var body = RegionsHarness.regionsJs();
		assertTrue(body.contains("Object.defineProperty(signal, \"fork\""), body);
		// The prose above the attachment says "NEVER on AbortSignal.prototype", so the scan is for a MEMBER ACCESS on
		// that prototype rather than for the phrase.
		assertFalse(body.contains("AbortSignal.prototype."), "the region runtime must never patch AbortSignal.prototype");
		assertFalse(body.contains("AbortSignal.prototype["), "the region runtime must never patch AbortSignal.prototype");
	}

	/**
	 * The barrier deadline is a clamped constant with no setter and no configuration read: it is never resolved from
	 * an attribute, a global or a descriptor field, because an author-settable barrier deadline is a way to configure
	 * a page-wide freeze.
	 */
	@Test void a03_sourceShape_barrierDeadlineIsNotConfigurable() throws Exception {
		var body = RegionsHarness.regionsJs();
		assertTrue(body.contains("const BARRIER_DEADLINE_MS = 2000;"), body);
		// Exactly one assignment in the whole file - the declaration - so there is no setter and no reassignment...
		assertEquals(1, body.split("BARRIER_DEADLINE_MS = ", -1).length - 1,
			"BARRIER_DEADLINE_MS must be assigned exactly once, at its declaration");
		// ...and the deadline is never resolved from configuration: no attribute read, no global, no descriptor field.
		for (var configPath : List.of(
			"data-juneau-region-barrier", "data-juneau-barrier", "JUNEAU_BARRIER", "barrierMs", "barrierDeadline"))
			assertFalse(body.contains(configPath), "the barrier deadline must not be readable from " + configPath);
	}

	/**
	 * The two caps are DECLARED SEPARATELY with different values and different units, and neither is defined in terms
	 * of the other.  Conflating them was the defect this pair of constants exists to prevent: a shared ceiling
	 * reports a cycle for legitimate non-cyclic traffic.
	 */
	@Test void a04_sourceShape_theTwoCapsAreIndependentConstants() throws Exception {
		var body = RegionsHarness.regionsJs();
		assertTrue(body.contains("const BUS_QUEUE_DEPTH_CAP = 64;"), body);
		assertTrue(body.contains("const REPLAY_BUFFER_CAP = 256;"), body);
		assertFalse(body.contains("REPLAY_BUFFER_CAP = BUS_QUEUE_DEPTH_CAP"), body);
		assertFalse(body.contains("BUS_QUEUE_DEPTH_CAP * "), body);
	}

	// =================================================================================================================
	// Behavior.
	// =================================================================================================================

	/** The container is EMPTY and IN-DOCUMENT at the first call, for each of the three region types. */
	@Test void b01_containerIsEmptyAndInDocumentAtFirstCall() {
		var r = report();
		assertEquals(List.of("card-body", "row-detail", "tab-body"), r.get("t1_types"), r::toString);
		assertAllTrue(r, "t1_allEmpty", "t1_allConnected", "t1_allSameNode", "t1_loadingViaAttrNotDom");
	}

	/**
	 * THE {@code ctx} GOLDEN.  The full key set, the full {@code declared} key set and the full {@code ids} key set,
	 * asserted as literals - including SD-3's rule that a member absent for one region type is present and null
	 * rather than missing, so a populator can read {@code ctx.ids.sectionId} without a type check.
	 */
	@Test void b02_ctxFieldSetIsAnExactGolden() {
		var r = report();
		assertEquals(List.of(
			"contractVersion", "data", "declared", "defaultPopulate", "emit", "fetchDeclared", "generation",
			"helpers", "host", "id", "ids", "key", "messageSignal", "on", "params", "reason", "refresh",
			"selection", "signal", "type", "write"), r.get("t2_keys"), r::toString);
		assertEquals(List.of("dataUrl", "fields", "lazy", "refreshMs", "renderer", "titleFields"),
			r.get("t2_declaredKeys"), r::toString);
		assertEquals(List.of("cardId", "gridId", "pageId", "rowId", "sectionId", "subtabId", "tabId", "viewId"),
			r.get("t2_idsKeys"), r::toString);

		var types = (Map<?,?>)r.get("t2_types");
		for (var fn : List.of("defaultPopulate", "emit", "fetchDeclared", "on", "refresh", "write"))
			assertEquals("function", types.get(fn), () -> fn + " must be a function on ctx");
		assertEquals("string", types.get("contractVersion"));
		assertEquals("number", types.get("generation"));

		assertEquals("1", r.get("t2_contractVersion"));
		assertEquals("row-detail", r.get("t2_type"));
		assertEquals("diagnose", r.get("t2_id"));
		assertEquals("gacks/row-42", r.get("t2_host"));
		assertEquals("gacks/row-42/diagnose", r.get("t2_key"), "key is host + '/' + id");
		assertEquals("initial", r.get("t2_reason"));
		assertEquals(0, ((Number)r.get("t2_generation")).intValue());
		assertAllTrue(r, "t2_messageSignalNullOnInitial", "t2_dataNull", "t2_selectionNull", "t2_paramsIsObject");
		assertEquals("glance", r.get("t2_idsGrid"));
		assertEquals("posture", r.get("t2_idsCard"));
		assertAllTrue(r, "t2_idsSectionNullOnRowDetail", "t2_declaredFieldsNull", "t2_declaredTitleFieldsNull");
	}

	/**
	 * {@code ctx.signal.fork()}'s ATTACHMENT MECHANICS, in six parts.  {@code fork} is a non-standard method on a
	 * standard type, so each of these is a distinct way to get it subtly wrong: a look-alike object the platform's own
	 * {@code fetch} would reject, an enumerable property that leaks into serialization, a prototype patch that
	 * mutates {@code AbortSignal} for every script on the page, a chainable fork that lets the tree grow past two
	 * levels, a supersede that leaves the previous child live, and a parent abort that misses its children.
	 */
	@Test void b08a_forkAttachmentMechanics() {
		var r = report();
		assertAllTrue(r,
			"t8a_isRealSignal", "t8a_signalMembers", "t8a_acceptedByRealFetch",
			"t8a_own", "t8a_notInOwnKeys", "t8a_notOnPrototype",
			"t8a_childIsSignal", "t8a_childHasNoFork",
			"t8a_firstChildAborted", "t8a_secondChildLive", "t8a_generationIncremented",
			"t8a_parentAbortKillsChild");
		assertEquals("function", r.get("t8a_forkTypeof"));
		assertEquals(false, r.get("t8a_enumerable"), "fork must be non-enumerable");
		assertEquals(false, r.get("t8a_writable"), "fork must be non-writable");
	}

	/** A second enrolment walk over the same node re-populates NOTHING, and a re-populate leaves one copy, not two. */
	@Test void b03_repopulateIsIdempotent() {
		var r = report();
		assertEquals(true, r.get("t3_sameHandle"), r::toString);
		assertEquals(1, ((Number)r.get("t3_callsAfterSecondInit")).intValue(), "initRegion must be idempotent");
		assertEquals(2, ((Number)r.get("t3_callsAfterRefresh")).intValue());
		assertEquals(1, ((Number)r.get("t3_spanCount")).intValue(), "the container is cleared before every call");
	}

	/**
	 * A throwing populate is CONTAINED - the region errors, its siblings stay healthy - and an unknown populator name
	 * fails VISIBLY rather than blank.  A region has no raw value to fall back to, unlike a renderer, so a blank
	 * container with no error would read to the user as "no data" rather than "broken page".
	 */
	@Test void b04_throwingPopulateIsContainedAndUnknownNameFailsVisibly() {
		var r = report();
		assertEquals("error", r.get("t4_badState"));
		assertEquals("ok", r.get("t4_goodState"));
		assertAllTrue(r, "t4_goodHasContent", "t4_errorNamesRegion", "t4_errorVisibleInContainer");
		assertEquals("error", r.get("t4_unknownState"));
		assertAllTrue(r, "t4_unknownErrorNamesName", "t4_reservedRefused", "t4_reservedErrorLogged");
	}

	/** A Promise-returning populate goes loading -> ok only AFTER the settle. */
	@Test void b05_promiseReturningPopulateStaysLoadingUntilSettle() {
		var r = report();
		assertEquals("loading", r.get("t5_stateWhileInFlight"));
		assertEquals("ok", r.get("t5_stateAfterSettle"));
	}

	/**
	 * The four return-value shapes, and specifically {@code Promise<() => void>}: a cleanup RESOLVED FROM a promise is
	 * registered and invoked once, while a REJECTED populate registers none - a populate that failed before it
	 * finished allocating cannot be trusted to know what to release.
	 */
	@Test void b06a_cleanupResolvedFromAPromiseIsInvokedExactlyOnce() {
		var r = report();
		assertEquals(0, ((Number)r.get("t6a_cleanupsBeforeTeardown")).intValue());
		assertEquals(1, ((Number)r.get("t6a_cleanupsAfterTeardown")).intValue());
		assertEquals(1, ((Number)r.get("t6a_cleanupsAfterSecondTeardown")).intValue(), "teardown is idempotent");
		assertEquals("error", r.get("t6a_rejectedState"));
		assertEquals(true, r.get("t6a_rejectedRegistersNoCleanup"), r::toString);
	}

	/**
	 * THE TEARDOWN ORDER, as an ORDER-RECORDING test rather than a set of independent "was it done" checks.  The five
	 * steps are individually plausible in any order and each one is still observably "done" under a reordering, so
	 * only the recorded sequence catches a refactor that reorders them: the poll timer must be cleared BEFORE the
	 * abort or a timer can tick after teardown, the cleanup must run BEFORE the container is cleared or an author's
	 * cleanup reads an emptied container, and the unsubscribe must run BEFORE the queue is cleared or the
	 * subscription leaks.
	 */
	@Test void b07_teardownStepsRunInTheirFixedOrder() {
		var r = report();
		assertEquals(List.of("abort", "cleanup"), r.get("t7_order"), r::toString);
		assertAllTrue(r,
			"t7_pollTimerClearedBeforeAbort", "t7_pollTimerCleared", "t7_pollNeverFired",
			"t6_cleanupsExactlyOnce", "t6_containerNotYetClearedAtCleanup", "t7_stillSubscribedAtCleanup",
			"t7_containerClearedAfter", "t7_markRemoved", "t7_unsubscribedAfterTeardown");
	}

	/** {@code ctx.signal} aborts on collapse, TRANSITIVELY killing every {@code fork()} child. */
	@Test void b08_signalAbortsTransitivelyOnCollapse() {
		var r = report();
		assertAllTrue(r, "t8_liveBeforeTeardown", "t8_topLevelAborted", "t8_forkChildAborted",
			"t8_abortReasonNamesTeardown");
	}

	/**
	 * {@code reason} tracks the CAUSAL EVENT, not the API that was invoked: an identical {@code ctx.refresh()} call is
	 * {@code "message"} from inside a bus handler and {@code "refresh"} from a timer, and {@code messageSignal} is
	 * non-null in exactly the first case.  A populate that calls {@code refresh()} unconditionally spins exactly once
	 * - two invocations total - rather than forever.
	 */
	@Test void b09a_reasonTracksTheCausalEventNotTheApi() {
		var r = report();
		assertEquals("initial", r.get("t9a_initial"));
		assertEquals("message", r.get("t9a_fromHandler"));
		assertEquals("refresh", r.get("t9a_fromTimer"));
		assertAllTrue(r, "t9a_messageSignalNonNullOnMessage", "t9a_messageSignalNullOnRefresh",
			"t9a_priorMessageSignalAborted");
		assertEquals(2, ((Number)r.get("t9a_selfRefreshSpinsOnce")).intValue(),
			"a populate that refreshes itself spins exactly once");
	}

	/** A region in flight is not RE-ENTERED: concurrent triggers coalesce into one follow-up run. */
	@Test void b09b_concurrentTriggersCoalesceRatherThanReenter() {
		var r = report();
		assertEquals(1, ((Number)r.get("t_coalesce_callsWhileInFlight")).intValue());
		assertEquals(2, ((Number)r.get("t_coalesce_callsAfterSettle")).intValue(), "three triggers, one follow-up run");
		assertEquals(List.of("initial", "refresh"), r.get("t_coalesce_reasons"), r::toString);
		assertEquals(true, r.get("t_coalesce_inFlightSignalAborted"), r::toString);
	}

	/**
	 * The enrolment walk: synchronous, idempotent, and orthogonal to barrier membership.  A HIDDEN region is
	 * ENROLLED but not populated, and its first populate is {@code "activate"} and never {@code "initial"} - the
	 * distinction that keeps a tabbed page from waiting on cards the user cannot see.
	 */
	@Test void b10_enrolmentAndActivationAreSeparateFromPopulation() {
		var r = report();
		assertEquals(2, ((Number)r.get("t_enrol_count")).intValue());
		assertEquals(List.of("initial"), r.get("t_enrol_visiblePopulated"), r::toString);
		assertAllTrue(r, "t_enrol_hiddenNotPopulated", "t_enrol_hiddenIsEnrolled");
		assertEquals("idle", r.get("t_enrol_hiddenState"));
		assertEquals(List.of("activate"), r.get("t_enrol_hiddenReasonOnActivate"), r::toString);
		assertAllTrue(r, "t_enrol_activateOnce", "t_teardown_swept", "t_teardown_idempotent", "t_orphan_errorLogged");
	}

	/**
	 * {@code ctx.write} from a NON-TABLE region, which is the case that has no table to inherit a CSRF token from.
	 * The three refusals - safe method, cross-origin URL, traversal - each issue ZERO network traffic, and a missing
	 * token is a refusal rather than a silent unauthenticated send.  None of the four touches the region's
	 * loading/ok/error state: a write is not a paint.
	 */
	@Test void b50b_writeFromANonTableRegionStampsRefusesAndSupersedes() {
		var r = report();
		assertEquals("POST", r.get("t50b_method"));
		assertAllTrue(r, "t50b_headerStamped", "t50b_contentType", "t50b_idempotencyMerged", "t50b_notBareCtxSignal",
			"t50b_priorWriteAborted", "t50b_secondWriteLive", "t50b_nonTwoHundredResolvesNotRejects",
			"t50b_refusalsIssuedNoTraffic", "t50b_refusalVisible", "t50b_stateUntouched");
		assertEquals(Map.of("safeMethod", "safe-method", "crossOrigin", "unsafe-url", "dotDot", "unsafe-url"),
			r.get("t50b_refusalReasons"), r::toString);
		assertEquals("missing-token", r.get("t50b_missingTokenReason"));
	}

	/**
	 * THE {@code AbortController} BASELINE FAILS LOUD.  A page of three regions in an environment without
	 * AbortController populates nothing, errors every region, logs EXACTLY ONE console error for the page rather than
	 * one per region, and hands out NO {@code ctx} at all - so there is no invocation whose {@code signal} is
	 * undefined or a polyfilled look-alike for a populator to trip over.  The design's answer here is a refusal, not a
	 * degraded mode, because a degraded mode's failure is silent.
	 */
	@Test void b08b_missingAbortControllerRefusesTheWholePageLoudlyAndExactlyOnce() {
		var r = report();
		assertEquals(0, ((Number)r.get("t8b_populates")).intValue(), "no populator may run without AbortController");
		assertEquals(List.of("error", "error", "error"), r.get("t8b_allErrored"), r::toString);
		assertEquals(1, ((Number)r.get("t8b_totalErrors")).intValue(), "one error for the page, not one per region");
		assertAllTrue(r, "t8b_noHandles", "t8b_exactlyOneError", "t8b_noCtxHandedOut", "t8b_messageVisible");
	}

	/**
	 * The published surface, and the CONTRACT VERSION's independence.  {@code RegionDef.CONTRACT_VERSION} is the
	 * region envelope's own and is not tied to the view or row-detail envelopes: the three evolve separately, and
	 * tying them would force a version bump on every consumer of either.
	 */
	@Test void b11_publishedSurfaceAndIndependentContractVersion() {
		var r = report();
		assertEquals(List.of("function", "function", "function", "function"), r.get("tx_exports"), r::toString);
		assertEquals(RegionDef.CONTRACT_VERSION, r.get("tx_contractVersion"),
			"the JS contract version must match RegionDef.CONTRACT_VERSION");
		assertEquals(List.of("row-detail", "card-body", "tab-body"), r.get("tx_regionTypes"), r::toString);
		assertEquals(List.of("default"), r.get("tx_reserved"), r::toString);
		assertEquals(true, r.get("tx_publishedOnSharedNamespace"), r::toString);
		var caps = (Map<?,?>)r.get("tx_caps");
		assertEquals(64, ((Number)caps.get("queue")).intValue());
		assertEquals(256, ((Number)caps.get("replay")).intValue());
		assertEquals(2000, ((Number)caps.get("deadline")).intValue());
	}
}
