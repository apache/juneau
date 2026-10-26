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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/** Node behavioral checks for {@code JuneauViews.actions.run} and {@code release}: the hook a chart card uses to run an action for a clicked point. */
class ViewsJs_ActionsRun_Test {

	private static Map<?,?> r() {
		var r = NodeHarness.report("actions-run.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or actions-run.cjs) not available - behavioral layer skipped");
		return r;
	}

	private static Map<?,?> m(String k) { return (Map<?,?>)r().get(k); }
	private static int i(String k) { return ((Number)r().get(k)).intValue(); }
	private static String s(String k) { return (String)r().get(k); }

	@Test void a01_directPostSubstitutesRowAndSendsCsrf() {
		assertEquals(1, i("a01_calls"));
		assertEquals("/x/INC-7/ack", s("a01_url"));
		assertEquals("POST", s("a01_method"));
		assertEquals(true, r().get("a01_csrf"));
		assertEquals("success", s("a01_outcome"));
		assertEquals("done", s("a01_message"));
	}

	@Test void a02_csrfFallsBackToBodyAndFailsClosed() {
		assertEquals(true, r().get("a02_bodyFallbackSent"));
		assertEquals(0, i("a02_missingSent"));
		assertEquals("refusal", s("a02_missingOutcome"));
	}

	@Test void a03_refusalsSendNothingAndPaintAnAlert() {
		for (var k : List.of("emptySub", "safeMethod", "noEndpoint")) {
			var c = m("a03_" + k);
			assertEquals(0, ((Number)c.get("sent")).intValue(), k);
			assertEquals("refusal", c.get("outcome"), k);
			assertEquals(true, c.get("alert"), k);
		}
	}

	@Test void a04_transportRefusalsAreNamed() {
		for (var st : List.of("403", "415", "421")) {
			var c = m("a04_" + st);
			assertEquals("refusal", c.get("outcome"), st);
			assertTrue(((String)c.get("text")).contains(st), st);
		}
	}

	@Test void a05_failureOutcomeIsPaintedAndReturned() {
		var c = m("a05_failure");
		assertEquals("failure", c.get("outcome"));
		assertEquals("nope", c.get("message"));
		assertTrue(((String)c.get("text")).contains("nope"));
	}

	@Test void a06_redrawOnlyWhenTheActionAsksForIt() {
		assertEquals(1, i("a06_redraws"));
		assertEquals(0, i("a06_plainRedraws"));
	}

	@Test void a07_bannerIsRebuiltAfterTheHostIsWiped() {
		assertEquals(true, r().get("a07_rebuilt"));
		assertEquals(1, i("a07_oneBanner"));
	}

	@Test void a08_rowDataSeamServesTheOverrideOnly() {
		assertEquals("{\"a\":1}", s("a08_override"));
		assertEquals(true, r().get("a08_noRow"));
	}

	@Test void a09_extraFieldsTravelWithTheRequest() {
		assertEquals("k-1", s("a09_extra"));
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource({
		"b01, Acknowledge is not available for this item", // visible-when failure refuses with the fixed text
		"b02, West only", // enabled-when failure shows the rule's reason
		"b04, Acknowledge is not available for this item", // single-object visible-when fails closed
		"b05, West only" // single-object enabled-when fails closed with its reason
	})
	void b_gateRefusals(String key, String text) {
		var c = m(key);
		assertEquals(0, ((Number)c.get("sent")).intValue());
		assertEquals("refusal", c.get("outcome"));
		assertEquals(text, c.get("text"));
	}

	@Test void b03_passingGateSends() {
		var c = m("b03");
		assertEquals(1, ((Number)c.get("sent")).intValue());
		assertEquals("success", c.get("outcome"));
	}

	@Test void c01_dialogRunStaysPendingUntilTheSubmitSettles() {
		assertEquals(true, r().get("c01_pendingWhileOpen"));
		assertEquals(true, r().get("c01_hasConfirm"));
		assertEquals("success", s("c01_outcome"));
		assertEquals(true, r().get("c01_closed"));
	}

	@Test void c02_dismissedDialogResolvesCancelledAndSendsNothing() {
		assertEquals("cancelled", s("c02_outcome"));
		assertEquals(0, i("c02_writes"));
	}

	@Test void c03_secondRunWhileOpenIsRefusedAndFirstSurvives() {
		assertEquals("refusal", s("c03_secondOutcome"));
		assertEquals(true, r().get("c03_firstStillOpen"));
	}

	@Test void c03b_aRefusedSecondRunDoesNotSwapTheLiveRunsRow() {
		assertEquals("refusal", s("c03b_secondOutcome"));
		assertEquals("/x/FIRST/edit", s("c03b_url"));
	}

	@Test void c04_declinedConfirmRendererCancelsAndAcceptedSendsOnce() {
		assertEquals("cancelled", s("c04_outcome"));
		assertEquals(0, i("c04_writes"));
		assertEquals("success", s("c04b_outcome"));
		assertEquals(1, i("c04b_writes"));
	}

	@Test void c06_failedFormFetchResolvesRefusal() {
		assertEquals("refusal", s("c06_outcome"));
	}

	@Test void g01_aPlainConfirmPromptsBeforeSending() {
		assertEquals(0, i("g01_writesBefore"));
		assertEquals(true, r().get("g01_promptOpen"), r()::toString);
		assertEquals(true, r().get("g01_pending"), r()::toString);
		assertEquals(1, i("g01_writesAfter"));
		assertEquals("success", s("g01_outcome"));
	}

	@Test void g02_aBareSuccessResolvesSuccessAndAFailedFetchResolvesUnknown() {
		assertEquals("success", s("g02_outcome"));
		assertEquals("unknown", s("g03_outcome"));
		assertEquals("unknown", s("g03b_outcome"));
	}

	@Test void g04_theCatalogServesDialogButtons() {
		assertEquals(true, r().get("g04_buttonFound"), r()::toString);
		assertEquals(true, r().get("g04_enabled"), r()::toString);
		assertEquals(true, r().get("g04_missingDisabled"), r()::toString);
	}

	@Test void g05_aDuplicateCatalogIdWarnsOncePerHost() {
		assertEquals(1, i("g05_warnCount"));
	}

	@Test void g06_releasingAnIdleHostLeavesOtherDialogsAlone() {
		assertEquals(true, r().get("g06_openBefore"), r()::toString);
		assertEquals(true, r().get("g06_openAfter"), r()::toString);
		assertEquals(true, r().get("g06_foreignKept"), r()::toString);
	}

	@Test void g07_aDuplicatedCatalogIdOpensTheFirstEntry() {
		assertEquals(1, i("g07_aForms"), r()::toString);
		assertEquals(0, i("g07_bForms"), r()::toString);
	}

	@Test void h01_aJobPointerAfterReleaseOpensNoStream() {
		assertEquals(0, i("h01_sources"));
		assertEquals(false, r().get("h01_leaked"), r()::toString);
		assertEquals(false, r().get("h01_progress"), r()::toString);
	}

	@Test void h02_aSuccessAfterReleaseRedrawsAndPaintsNothing() {
		assertEquals(0, i("h02_redraws"));
		assertEquals(false, r().get("h02_leaked"), r()::toString);
		assertEquals(false, r().get("h02_text"), r()::toString);
	}

	@Test void h03_aFormResponseAfterReleaseOpensNoDialog() {
		assertEquals(0, i("h03_layers"));
		assertEquals(false, r().get("h03_leaked"), r()::toString);
	}

	@Test void i01_aResultFormUrlFromTheBodyMustBeTrusted() {
		for (var k : new String[]{"evil", "backslash", "lead", "trail", "proto"}) {
			assertEquals(0, i("i_" + k + "_gets"), k);
			assertEquals(true, r().get("i_" + k + "_notice"), k);
		}
	}

	@Test void i02_aTrustedResultFormIsFetchedAndTheRunSettlesWhenTheReceiptCloses() {
		assertEquals(1, i("i_control_gets"));
		assertEquals(true, r().get("i_control_pendingWhileReceiptOpen"), r()::toString);
		assertEquals("success", s("i_control_outcome"));
	}

	@Test void j01_outcomesDoNotLeakAcrossRunsOnOneHost() {
		assertEquals("cancelled,refusal,success", s("j01_sequence"));
	}

	@Test void j02_anUnreadableBodySettlesUnknown() {
		assertEquals("unknown", s("j02_outcome"));
	}

	@Test void k01_unsafeUrlShapesAreRefusedVisiblyBeforeSending() {
		for (var k : new String[]{"dotdot", "residual"}) {
			var m = (java.util.Map<?,?>)r().get("k_" + k);
			assertEquals(0, ((Number)m.get("sent")).intValue(), k);
			assertEquals("refusal", m.get("outcome"), k);
			assertEquals(true, m.get("alert"), k);
			assertTrue(String.valueOf(m.get("text")).contains("not sent"), k);
		}
	}

	@Test void e01_releaseClosesDialogsRemovesBannerAndSettlesUnknown() {
		assertEquals(1, i("e01_openBefore"));
		assertEquals(0, i("e01_openAfter"));
		assertEquals("unknown", s("e01_outcome"));
		assertEquals(true, r().get("e01_bannerGone"));
	}

	@Test void e02_releaseOfAnIdleOrMissingHostIsANoop() { assertEquals(true, r().get("e02_noop")); }

	@Test void e03_hookIsFrozen() { assertEquals(true, r().get("e03_frozen")); }

	@Test void e04_badArgumentsResolveRefusals() { assertEquals("refusal,refusal", s("e04_bad")); }

	@Test void f01_nextRunClearsThePreviousRunsMessages() {
		assertEquals(true, r().get("f01_noStaleText"));
		assertEquals(true, r().get("f01_hasNew"));
	}

	@Test void j03_aThrowingSuccessHandlerStillSettlesTheRun() {
		assertEquals("unknown", s("j03_outcome"), r()::toString);
	}

	@Test void m01_releasingOneCardLeavesAnotherCardsDialogOpen() {
		assertEquals(2, i("m01_before"));
		assertEquals(1, i("m01_after"), r()::toString);
		assertEquals(1, i("m01_backdrops"));
		assertEquals("unknown", s("m01_aOutcome"));
		assertEquals(true, r().get("m01_bPending"));
		assertEquals("0", s("m01_survivorLayer"), "the survivor is re-stamped to its new stack position");
		assertEquals("1000", s("m01_survivorZ"));
		assertEquals(true, r().get("m01_focusInSurvivor"), "focus stays inside the surviving modal");
	}

	@Test void m02_committingOneCardsDialogLeavesAnotherCardsDialogOpen() {
		assertEquals(1, i("m02_after"), r()::toString);
		assertEquals(1, i("m02_backdrops"));
		assertEquals("success", s("m02_aOutcome"));
		assertEquals(true, r().get("m02_bPending"));
		assertEquals("0", s("m02_survivorLayer"));
		assertEquals(true, r().get("m02_focusInSurvivor"));
	}

	@Test void m03_oneCardsDialogDoesNotConsumeAnotherCardsDepthBudget() {
		assertEquals(3, i("m03_count"), r()::toString);
		assertEquals(0, i("m03_refused"));
	}

	@Test void m04_thePageWideCeilingStillRefuses() {
		var ceiling = i("m04_ceiling");
		assertEquals(ceiling, i("m04_count"), r()::toString);
		assertEquals(1, i("m04_refused"));
		assertEquals("Too many dialogs are open on this page.", s("m04_message"));
		assertEquals(0, i("m04_noticeInDialog"), "the ceiling refusal is not painted into another card's dialog");
		assertEquals(1, i("m04_noticeInCard"), r()::toString);
	}

	@Test void m05_interleavedStacksCloseOnlyTheirOwnLayers() {
		assertEquals(3, i("m05_before"), r()::toString);
		assertEquals(3, i("m05_aCount"));
		assertEquals(1, i("m05_after"), r()::toString);
		assertEquals(true, r().get("m05_survivorIsB"));
		assertEquals(true, r().get("m05_bPending"));
		assertEquals("0", s("m05_survivorLayer"));
		assertEquals(3, i("m05b_before"), r()::toString);
		assertEquals(1, i("m05b_after"), r()::toString);
		assertEquals(true, r().get("m05b_survivorIsB"));
		assertEquals("0", s("m05b_survivorLayer"));
	}

	@Test void m06_aTransientAboveASurvivingDialogStaysWithIt() {
		assertEquals(true, r().get("m06_popoverKept"), r()::toString);
		assertEquals(1, i("m06_dialogs"));
	}

	@Test void m07_escapeClosesOnlyTheTopDialog() {
		assertEquals(1, i("m07_count"));
		assertEquals(true, r().get("m07_remainingIsA"));
		assertEquals("cancelled", s("m07_bOutcome"));
		assertEquals(true, r().get("m07_aPending"));
	}

	@Test void m08_aCtxWithNoDialogYetOwnsNone() {
		assertEquals(2, i("m08_page"));
		assertEquals(0, i("m08_unseeded"), r()::toString);
		assertEquals(false, r().get("m08_exceeded"));
		assertEquals(true, r().get("m08_nullCtx"));
	}

	@Test void m09_aCardStillStacksToItsOwnCapWhileOthersAreOpen() {
		assertEquals(3, i("m09_count"), r()::toString);
		assertEquals(3, i("m09_countAfter"), "the third stacked dialog of one card is refused");
		assertEquals(1, i("m09_noticeInA1"), "the refusal is painted in the owning card's top dialog");
		assertEquals(0, i("m09_noticeInB"));
		assertEquals("Only 2 stacked dialogs are supported.", s("m09_noticeText"));
	}

	@Test void m11_aCommittedStackedStepDoesNotPullFocusBehindAnotherCardsModal() {
		assertEquals(true, r().get("m11_confirmFound"));
		assertEquals(true, r().get("m11_focusInBSync"), r()::toString);
		assertEquals(true, r().get("m11_focusInB"));
		assertEquals(true, r().get("m11_bOpen"));
	}

	@Test void m10_noticesLandInTheOwningCardsDialog() {
		assertEquals(true, r().get("m10_heldInA"));
		assertEquals(false, r().get("m10_heldInB"));
		assertEquals(true, r().get("m10_draftInA"));
		assertEquals(false, r().get("m10_draftInB"));
	}
}
