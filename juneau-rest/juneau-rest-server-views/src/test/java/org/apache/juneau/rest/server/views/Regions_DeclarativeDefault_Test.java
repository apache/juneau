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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the declarative default: the {@code RegionDef} descriptor's client twin ({@code ctx.declared}),
 * {@code ctx.fetchDeclared()}'s handshake-checked fetch, the reserved {@code "default"} populator's
 * &sect;8.5-normative algorithm, R14a's pre-fetch matrix, the poll lifecycle (&sect;8.3.1), and - the ACCEPTANCE
 * GATE for this child - L12's non-privilege proof.
 *
 * <p>
 * <b>TESTS 11 AND 12 ARE A STOP GATE.</b> If either fails, the declarative default has a privileged path that an
 * ordinary consumer populator cannot reach, and every child built on top of this one (host enrolment walks, the
 * three real callers, the deprecation-window close) is building on a broken foundation.
 *
 * <p>
 * The behavioral half runs the real runtime source - {@code juneau-regions.js} AND {@code juneau-helpers.js}, since
 * the reserved default paints through {@code ctx.helpers[...]} - under a DOM shim and a controllable clock (see
 * {@code src/test/js/regions-declarative-default.cjs}).
 */
class Regions_DeclarativeDefault_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.reportWithHelpers("regions-declarative-default.cjs");
		assumeTrue(r != null, "node not available or regions-declarative-default.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static int intOf(Map<?,?> r, String key) {
		return ((Number)r.get(key)).intValue();
	}

	// =================================================================================================================
	// ctx.declared: the descriptor's client twin.
	// =================================================================================================================

	/** The exact {@code declared} key set - a golden, like the ctx key set itself (test 2 of the primitive suite). */
	@Test void t02_declared_keySetIsGolden() {
		var r = report();
		assertEquals(List.of("dataUrl", "fields", "lazy", "refreshMs", "renderer", "titleFields"),
			r.get("t2_declaredKeys"), r::toString);
	}

	@Test void t02_declared_membersMatchTheAttachedDescriptor() {
		var r = report();
		assertBean(r, "t2_dataUrl,t2_renderer,t2_lazy,t2_refreshMs,t2_fields,t2_titleFields,t2_params",
			"/api/widgets/42/status,fieldGrid,false,30000,[{data=status,label=Status}],[status],{verbose=true}");
	}

	/** F9: the per-type {@code lazy} default - row-detail/card-body false, tab-body true - for an identity-only region. */
	@Test void t02b_declared_perTypeLazyDefaultsForAnIdentityOnlyRegion() {
		var r = report();
		assertBean(r, "t2b_lazyDefaults{row-detail,card-body,tab-body}", "{false,false,true}");
		assertAllTrue(r, "t2b_dataUrlNull");
	}

	/** A {@code contractVersion} mismatch on the descriptor is a fail-safe fall-through, logged once, never a throw. */
	@Test void t02c_declared_contractVersionMismatchFallsThroughToIdentityOnlyAndLogsOnce() {
		var r = report();
		assertAllTrue(r, "t2c_fallsThroughToIdentityOnly", "t2c_loggedMismatch");
	}

	// =================================================================================================================
	// ctx.fetchDeclared(): the handshake.
	// =================================================================================================================

	@Test void t13_fetchDeclared_resolvesTheUnwrappedValuesMapOnSuccess() {
		var r = report();
		// t13_secondCallValuesMap: no caching - a second call fetches again and resolves the same shape.
		assertBean(r, "t13_fetchUrl,t13_valuesMap,t13_secondCallValuesMap",
			"/api/widgets/1,{status=ok,count=3},{status=ok,count=3}");
	}

	@Test void t13_fetchDeclared_404IsKindEmpty() {
		assertEquals("empty", report().get("t13_404kind"));
	}

	@Test void t13_fetchDeclared_otherNonOkIsKindError() {
		assertEquals("error", report().get("t13_500kind"));
	}

	@Test void t13_fetchDeclared_contractVersionMismatchIsKindError() {
		assertEquals("error", report().get("t13_mismatchKind"));
	}

	@Test void t13_fetchDeclared_noDeclaredDataUrlResolvesNullNotAThrow() {
		assertNull(report().get("t13_noDataUrlResolvesNull"));
	}

	// =================================================================================================================
	// R14a: the pre-fetch matrix.
	// =================================================================================================================

	/**
	 * `dataUrl` set -> the region's OWN first populate call issues the fetch BEFORE the populate ever runs, and hands
	 * the unwrapped values map to the populate as {@code ctx.data}.  A later re-populate (here, {@code ctx.refresh()})
	 * does NOT pre-fetch: {@code ctx.data} is null and no second fetch is issued unless the populate asks for one.
	 */
	@Test void t15a_preFetch_firstPopulateGetsCtxDataBeforeItRuns_repopulateDoesNot() {
		var r = report();
		assertEquals(1, intOf(r, "t15a_fetchCountBeforeFirstPopulate"),
			"the pre-fetch must already be IN FLIGHT before the first populate call runs");
		assertEquals(Map.of("status", "ok"), r.get("t15a_ctxDataOnFirstPopulate"), r::toString);
		assertNull(r.get("t15a_ctxDataOnRepopulate"), "R14a: only the first-ever populate is pre-fetched");
		assertEquals(1, intOf(r, "t15a_fetchCountAfterRepopulate"),
			"a re-populate must not trigger a second automatic pre-fetch");
	}

	// =================================================================================================================
	// Test 16g: ONE fetchDeclared() call site in the default, guarded by the nullish check over ctx.data.
	// =================================================================================================================

	@Test void t16g_theDefaultCallsFetchDeclaredFromExactlyOneCallSite() {
		var r = report();
		assertEquals(1, intOf(r, "t16g_oneFetchDeclaredCallSite"));
		assertAllTrue(r, "t16g_guardedByNullish");
	}

	@Test void t16g_theDefaultDoesNotCallFetchDeclaredWhenCtxDataIsAlreadySet() {
		assertAllTrue(report(), "t16g_fetchDeclaredNotCalledWhenCtxDataSet", "t16g_paintedFromCtxData");
	}

	// =================================================================================================================
	// Test 16h: the payload shape contract - a VALUES MAP, never the envelope, never the transport triple.
	// =================================================================================================================

	@Test void t16h_ctxDataIsTheValuesMapAlone() {
		var r = report();
		assertEquals(List.of("count", "status"), r.get("t16h_ctxDataKeys"), r::toString);
		assertAllTrue(r, "t16h_noEnvelopeLeak", "t16h_noTransportLeak");
	}

	// =================================================================================================================
	// Poll lifecycle (design §8.3.1).
	// =================================================================================================================

	@Test void t14a_poll_oneTimerEverAcrossTenRepopulatesAndATick() {
		var r = report();
		// Collapsed via BCT: all three are simple int-equality checks on the same map, and the generic BCT
		// failure message (which names the failing property) is diagnostic enough on its own.
		assertBean(r, "t14a_oneTimerAfterInitial,t14a_oneTimerAfterTenRepopulates,t14a_oneTimerAfterTick", "1,1,1");
	}

	/**
	 * Skip-while-in-flight is asserted at the source level (see the harness's own comment on this test): the
	 * runtime's own clear-then-set discipline makes the race {@code onPollTick}'s guard protects against
	 * unreachable from outside via a deterministic single fake clock, so the guard's PRESENCE - checked before
	 * re-invoking, and re-arming rather than dropping the region's poll forever on the busy branch - is the
	 * assertion.
	 */
	@Test void t14b_poll_onPollTickSkipsNotQueuesWhileBusyAndStillReArms() {
		assertAllTrue(report(), "t14b_checksBusyBeforeRepopulate", "t14b_reArmsOnBusySkip", "t14b_dropsNotQueues");
	}

	@Test void t14c_poll_timerStaysArmedAfterAnErrorSettle() {
		var r = report();
		assertEquals("error", r.get("t14c_state"));
		assertEquals(1, intOf(r, "t14c_timerArmedAfterError"),
			"a transient failure must be a retry, not a dead card - the poll timer must still be armed");
	}

	// =================================================================================================================
	// L12 STOP GATE - test 11: no privileged path through the reserved name.
	// =================================================================================================================

	/**
	 * A consumer-registered populator that DELEGATES to {@code ctx.defaultPopulate} reproduces the reserved
	 * default's own DOM byte-for-byte, AND a second consumer populator that hand-reproduces the exact same
	 * algorithm using only the documented ctx contract (no call to {@code ctx.defaultPopulate} at all) ALSO
	 * reproduces it.  If the reserved default had a privileged path - a framework-internal field, a branch keyed on
	 * the reserved name, anything not on the documented ctx contract - one of these two ordinary registry entries
	 * would fail to reproduce the output and this test would catch it.
	 *
	 * <p>
	 * STOP GATE: if this fails, do not proceed to d/e - the layering L12 exists to prove is wrong.
	 */
	@Test void t11_STOP_GATE_consumerPopulatorsReproduceTheDefaultByteForByte() {
		var r = report();
		assertAllTrue(r, "t11_defaultVsDelegateIdentical", "t11_defaultVsHandRolledIdentical");
		// Sanity: the compared output is non-trivial (a real painted field grid), not two empty strings agreeing.
		assertTrue(((String)r.get("t11_sample")).contains("juneau-view-detail-field"), r::toString);
		assertTrue(((String)r.get("t11_sample")).contains(">Status<"), r::toString);
	}

	// =================================================================================================================
	// L12 STOP GATE - test 12: the default runs against a hand-built ctx with no DOM ancestry.
	// =================================================================================================================

	/**
	 * The reserved default is called directly - not through {@code mintRegion}/{@code initRegion}, not through any
	 * region at all - against a hand-built {@code ctx} object containing ONLY the four members its own doc says it
	 * reads ({@code declared}, {@code data}, {@code fetchDeclared}, {@code helpers}) and a container that was never
	 * appended to the document.  It must still resolve the payload through the hand-built {@code fetchDeclared} and
	 * paint into the detached container, proving it reads only the portable ctx contract and never reaches into
	 * framework internals (no region object, no element, no live-region registry).
	 *
	 * <p>
	 * STOP GATE: if this fails, do not proceed to d/e - the layering L12 exists to prove is wrong.
	 */
	@Test void t12_STOP_GATE_theDefaultRunsAgainstAHandBuiltCtxWithNoDomAncestry() {
		var r = report();
		assertEquals(false, r.get("t12_containerConnectedBeforeCall"), "the container must start detached");
		assertAllTrue(r, "t12_fetchDeclaredCalledOnce", "t12_painted", "t12_containerStillDetached");
		assertTrue(((String)r.get("t12_html")).contains("from-hand-built-fetch"), r::toString);
	}

	// =================================================================================================================
	// Test 16a: serializeParams golden cases.  juneau-regions.js is the only implementation; these literals were
	// captured from the deleted Java RegionDef.serializeParams on 2026-09-30 so the wire encoding did not change.
	// =================================================================================================================

	@Test void t16a_serializeParams_goldenCases() {
		var r = report();
		assertEquals("", r.get("t16a_null"));
		assertEquals("a=1", r.get("t16a_nullValueOmitsKey"));          // input: a is "1", b is null
		assertEquals("a=", r.get("t16a_emptyStringIsKeyEquals"));      // input: a is the empty string
		assertEquals("on=true&n=42", r.get("t16a_booleanAndNumber"));  // input: on is true, n is 42
		assertEquals("tag=a&tag=b", r.get("t16a_collectionRepeatsKey")); // input: tag is the collection a, b
		assertEquals("z=1&a=2", r.get("t16a_keyOrderIsIterationOrder")); // input: z is "1", then a is "2"
		assertEquals("q=a%20b", r.get("t16a_spaceIsPercent20"));       // input: q is "a b"
	}

	/**
	 * A nested map is not a valid wire input (RegionDef.validate() rejects it server-side), so the client drops it
	 * silently rather than throwing.
	 */
	@Test void t16a_serializeParams_nestedMapIsDroppedClientSideNotThrown() {
		assertEquals("", report().get("t16a_nestedMapDroppedNotThrown"));
	}
}
