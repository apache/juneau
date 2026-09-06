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
 * Always-on coverage for the region MESSAGE BUS in {@code juneau-regions.js}: broadcast by default, opt-in targeted
 * delivery, opaque payloads, per-subscriber error isolation, subscription-ordered synchronous delivery with
 * emit-during-delivery queued rather than nested, unsubscribe on teardown, the closed set of framework-emitted
 * schemas, and both directions of SD-2's emit-ownership invariant.
 *
 * <p>
 * The behavioral half runs the real runtime source under a DOM shim (see {@code src/test/js/regions-bus.cjs}).
 */
class Regions_Bus_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("regions-bus.cjs");
		assumeTrue(r != null, "node not available or regions-bus.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@SuppressWarnings("unchecked")
	private static List<String> list(Map<?,?> r, String key) {
		return (List<String>)r.get(key);
	}

	// =================================================================================================================
	// Source shape.
	// =================================================================================================================

	/**
	 * SD-2, THE EMIT-OWNERSHIP INVARIANT, as a source scan: the region runtime installs NO delegated listener over
	 * author-painted content.  Chrome auto-emits because the framework owns the chrome; content emits explicitly
	 * because the author owns the content.  A "helpful" click delegate over a region container is exactly how this
	 * invariant gets broken later, and it would break it silently - so the negative is pinned here as well as
	 * behaviorally.
	 */
	@Test void a01_sourceShape_theRuntimeBindsNoDelegateOverAuthorContent() throws Exception {
		var body = RegionsHarness.regionsJs();
		assertFalse(body.contains("addEventListener(\"click\""),
			"the region runtime must not bind a click listener - content emits explicitly (SD-2)");
		assertFalse(body.contains("addEventListener(\"change\""), body);
		assertFalse(body.contains("addEventListener(\"input\""), body);
		assertFalse(body.contains("addEventListener(\"submit\""), body);
		// The whole listener budget is TWO, and neither is bound on a region's contents: the parent->child abort
		// linkage is on an AbortSignal, and the barrier's backstop is on the document.  Pinning the COUNT is what
		// makes a third listener - which is how a content delegate would arrive - fail here rather than pass because
		// it used an event name this test did not think to name above.
		assertEquals(2, body.split("addEventListener\\(", -1).length - 1,
			"the region runtime's only listeners are the parent->child abort linkage and the barrier backstop");
		assertTrue(body.contains("parent.addEventListener(\"abort\""), body);
		assertTrue(body.contains("window.document.addEventListener(\"DOMContentLoaded\""), body);
	}

	/**
	 * Payload OPACITY as a source scan, to complement the behavioral trap: the framework neither clones, serializes
	 * nor routes on the payload, so a payload carrying a DOM node or a class instance survives delivery unchanged and
	 * the payload's shape is a contract between the emitting and receiving regions rather than with the framework.
	 */
	@Test void a02_sourceShape_thePayloadIsNeverClonedOrInspected() throws Exception {
		var body = RegionsHarness.regionsJs();
		for (var forbidden : List.of("structuredClone", "JSON.parse(JSON.stringify", "Object.assign({}, msg",
				"msg.kind ===", "message.kind ==="))
			assertFalse(body.contains(forbidden), "the bus must not " + forbidden + " a payload");
	}

	// =================================================================================================================
	// Delivery.
	// =================================================================================================================

	/**
	 * A broadcast reaches every subscriber EXCEPT the emitter, so a self-reflecting control cannot learn its own state
	 * from the bus and has to keep it in local state - which is the intended discipline, not an oversight.
	 */
	@Test void b17_broadcastReachesEveryoneButTheEmitter() {
		var r = report();
		assertEquals(List.of(), r.get("t17_a"), "the emitter must not receive its own broadcast");
		assertEquals(List.of("ping"), r.get("t17_b"), r::toString);
		assertEquals(List.of("ping"), r.get("t17_c"), r::toString);
		assertEquals("a", r.get("t17_metaFrom"));
		assertAllTrue(r, "t17_metaToNullOnBroadcast", "t17_synchronous");
	}

	/**
	 * Targeted delivery reaches EXACTLY one region, resolving either a short id within the emitter's own host or a
	 * fully-qualified {@code host/id} key; an unknown target is a no-op plus a warning rather than a throw, because
	 * one region's stale target must not take down the emitting region's populate.
	 */
	@Test void b18_targetedDeliveryResolvesShortIdsAndQualifiedKeys() {
		var r = report();
		assertEquals(List.of("short"), r.get("t18_shortB"), r::toString);
		assertEquals(List.of(), r.get("t18_shortC"), "a targeted emit must not broadcast");
		assertEquals("b", r.get("t18_metaTo"));
		assertEquals(List.of("qualified"), r.get("t18_qualifiedC"), r::toString);
		assertEquals(List.of("short"), r.get("t18_qualifiedB"), "the qualified target must not reach b");
		assertEquals(List.of("grid/a", "grid/b", "grid/c"), r.get("t18_keys"), r::toString);
	}

	/** An unknown - or self-addressed - target warns and delivers nothing, and never throws. */
	@Test void b19_unknownAndSelfAddressedTargetsWarnWithoutThrowing() {
		var r = report();
		assertAllTrue(r, "t19_noThrow", "t19_warned", "t19_nobodyGotIt",
			"t19_selfAddressWarned", "t19_selfAddressNotDelivered");
	}

	/**
	 * PAYLOAD OPACITY, behaviorally: a getter-trapped payload arrives having recorded ZERO property reads, on both
	 * the broadcast and the targeted path, and every subscriber receives the SAME OBJECT rather than a copy.
	 */
	@Test void b20_payloadArrivesUnreadAndUnclonedS() {
		var r = report();
		assertEquals(List.of(), r.get("t20_reads"), () -> "the framework read: " + r.get("t20_reads"));
		assertAllTrue(r, "t20_zeroReads", "t20_zeroReadsTargeted", "t20_identityShared");
	}

	/**
	 * A throwing subscriber does not block the rest of the fan-out and IS reported.  It is deliberately NOT
	 * auto-unsubscribed after repeated throws: silently dropping a subscriber turns a loud bug into a mysterious one,
	 * so it keeps receiving and keeps reporting.
	 */
	@Test void b21_aThrowingSubscriberIsIsolatedReportedAndNotDropped() {
		var r = report();
		assertEquals(List.of("m1"), r.get("t21_cStillGotIt"), r::toString);
		assertEquals(List.of("m1", "m2"), r.get("t21_stillSubscribed"), "a thrower must not be auto-unsubscribed");
		assertEquals(List.of("m1", "m2"), r.get("t21_cGotBoth"), r::toString);
		assertAllTrue(r, "t21_reported", "t21_reportedTwice");
	}

	/** Delivery order is SUBSCRIPTION order. */
	@Test void b22_deliveryOrderIsSubscriptionOrder() {
		var r = report();
		assertEquals(List.of("z", "m", "a"), r.get("t22_order"), r::toString);
	}

	/**
	 * An emit DURING delivery is QUEUED, not nested - asserted by CALL-ORDER RECORDING rather than by
	 * absence-of-crash, because a nested implementation does not crash: it just re-enters a subscriber mid-paint, and
	 * the only visible difference is the order the handlers ran in.  The nested signature would interleave the second
	 * message's handlers inside the first's; the queued signature completes the first fan-out entirely first.
	 */
	@Test void b23_emitDuringDeliveryIsQueuedNotNested() {
		var r = report();
		assertEquals(List.of("enter:b:m1", "exit:b:m1", "enter:c:m1", "exit:c:m1",
			"enter:c:m2", "exit:c:m2", "enter:driver:m2", "exit:driver:m2"), r.get("t23_trace"), r::toString);
		assertAllTrue(r, "t23_notNested", "t23_m1FanOutCompletedFirst");
		assertEquals(List.of("m1", "m2"), r.get("t23_cGot"), r::toString);
	}

	/**
	 * An emit cycle terminates the drain and errors loudly, PRINTING THE CYCLE - the shortest repeating
	 * {@code from -> to} suffix - rather than an arbitrary pair of keys.  Asserted with a THREE-region ring as well
	 * as a two-region one, because the two-region case cannot tell "prints the cycle" apart from "prints the last two
	 * hops", which is the mistake worth catching: a wrong cycle print sends the reader to the wrong region.
	 */
	@Test void b24_anEmitCycleIsTerminatedAndPrintsTheActualCycle() {
		var r = report();
		assertAllTrue(r, "t24_2_exactlyOneError", "t24_2_terminated", "t24_2_namesEveryRegion",
			"t24_3_exactlyOneError", "t24_3_terminated", "t24_3_namesEveryRegion");
		assertEquals(2, ((Number)r.get("t24_2_hopCount")).intValue(), r::toString);
		assertEquals(3, ((Number)r.get("t24_3_hopCount")).intValue(),
			"a three-region cycle must print three hops, not the last two");
	}

	/**
	 * THE LEAK TEST: a torn-down region receives nothing even though its handler is still reachable from the test's
	 * own closure - so the unsubscribe is real and not merely a dropped reference.  {@code ctx.on}'s returned
	 * unsubscribe works independently, leaving the region itself live and healthy.
	 */
	@Test void b25_teardownUnsubscribesAndTheReturnedUnsubscribeIsIndependent() {
		var r = report();
		assertEquals(List.of("before"), r.get("t25_beforeTeardown"), r::toString);
		assertEquals(List.of("before"), r.get("t25_afterTeardown"), "a torn-down region must receive nothing");
		assertEquals(List.of("before", "after"), r.get("t25_siblingUnaffected"), r::toString);
		assertEquals(List.of("one"), r.get("t26_receivedBeforeOff"), r::toString);
		assertEquals("ok", r.get("t26_stateStillOk"));
		assertEquals(true, r.get("t26_regionStillLive"), r::toString);
	}

	/**
	 * Teardown step 4: a message the torn-down region had already QUEUED, but which has not been delivered yet, is
	 * DISCARDED.  A region that is going away should not still be driving the page after it is gone.
	 */
	@Test void b25a_aQueuedMessageFromATornDownRegionIsDiscarded() {
		var r = report();
		assertEquals(List.of("m1"), r.get("t_step4_cGot"), r::toString);
		assertEquals(true, r.get("t_step4_queuedMessageDropped"), r::toString);
	}

	/**
	 * The framework-emitted set is SMALL AND CLOSED - selection changed, detail toggled, table redrew - every kind is
	 * namespaced, and 27a/27c's chrome half of SD-2 holds: a region beside the chrome is driven with NO author emit
	 * code at all, only a {@code ctx.on} plus a {@code msg.viewId} filter.
	 */
	@Test void b27_theFrameworkEmittedSetIsClosedAndChromeAutoEmits() {
		var r = report();
		assertEquals(List.of("juneau:selection-changed", "juneau:detail-toggled", "juneau:table-redrew"),
			r.get("t27_kinds"), r::toString);
		assertEquals("juneau:framework", r.get("t27_senderIsFramework"));
		assertEquals(true, r.get("t27_allNamespaced"), r::toString);
		assertEquals(List.of("juneau:selection-changed", "juneau:detail-toggled", "juneau:table-redrew"),
			r.get("t27a_drivenWithNoAuthorEmit"), "chrome auto-emits: the consumer wrote no emit call");
	}

	/**
	 * A GOLDEN per framework-emitted message - kind, {@code schemaVersion} and the whole field set - plus the
	 * two-tables-on-one-page disambiguation case.  {@code viewId} on the payload is what makes that case answerable
	 * at all: {@code meta.from} names the framework rather than a table, and a framework broadcast has no host for a
	 * short target to resolve against, so without {@code viewId} a subscriber cannot tell WHICH table's selection
	 * changed.
	 */
	@Test void b51_frameworkMessageSchemasAreGoldenAndCarryViewId() {
		var r = report();
		assertEquals(List.of("added", "ids", "kind", "removed", "rows", "schemaVersion", "viewId"),
			r.get("t51_selectionKeys"), r::toString);
		assertEquals(List.of("expanded", "generation", "kind", "rowId", "schemaVersion", "viewId"),
			r.get("t51_detailKeys"), r::toString);
		assertEquals(List.of("kind", "nested", "page", "rowCount", "schemaVersion", "viewId"),
			r.get("t51_redrewKeys"), r::toString);
		assertEquals(true, r.get("t51_schemaVersionsIndependentOfContract"),
			"a message schema version is its own, not the region contract version");
		assertEquals(true, r.get("t51_filteredByViewId"), r::toString);
		assertEquals(List.of("gacks", "other"), r.get("t51_viewIdsSeen"), r::toString);
	}

	/**
	 * SD-2's NEGATIVE HALF: CONTENT DOES NOT AUTO-EMIT.  A populate paints three clickable elements with its own
	 * listeners, calls no emit, and clicking them produces ZERO bus traffic - and the framework has bound no listener
	 * of its own beside the author's, on the buttons or on the region container.  This is the test that stops someone
	 * later adding a convenience click delegate over author DOM, which would break the invariant in a way no positive
	 * test notices.
	 */
	@Test void b27b_contentDoesNotAutoEmit() {
		var r = report();
		assertAllTrue(r, "t27b_authorHandlersRan", "t27b_zeroBusTraffic",
			"t27b_containerHasNoFrameworkListener", "t27b_stillZeroAfterContainerClick");
		assertEquals(List.of("click:1", "click:1", "click:1"), r.get("t27b_listenerCounts"),
			"each painted node must carry the author's listener and no framework listener");
	}

	/**
	 * OUT-OF-ORDER SETTLE, the flagship shape for update-in-place regions.  Three rapid messages with responses
	 * returning in the order 3, 1, 2: the container holds the LAST REQUESTED probe, every superseded fetch's signal
	 * is aborted, a superseded response can no longer settle at all, and no {@code AbortError} ever surfaces as a
	 * region error - the swallow the design points at as the established idiom.
	 */
	@Test void b50_outOfOrderSettleLeavesTheLastRequestedResponsePainted() {
		var r = report();
		assertAllTrue(r, "t50_twoFetches", "t50_firstSignalAborted", "t50_secondSignalLive",
			"t50_secondSettled", "t50_staleFirstCannotSettle", "t50_noRegionError");
		assertEquals("probe-2", r.get("t50_containerHoldsSecond"), "a stale response must not overwrite a fresh one");
		assertEquals("probe-5", r.get("t50a_holdsLast"), r::toString);
		assertEquals(List.of(true, true, false), r.get("t50a_earlierSignalsAborted"), r::toString);
		assertEquals(List.of(false, false), r.get("t50a_supersededCannotSettle"), r::toString);
		assertEquals(true, r.get("t50a_noAbortErrorSurfaced"), r::toString);
	}
}
