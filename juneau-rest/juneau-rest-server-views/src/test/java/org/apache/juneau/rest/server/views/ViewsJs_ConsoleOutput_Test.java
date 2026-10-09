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

import static org.apache.juneau.rest.server.views.ViewsJs_ConsoleOutputEnv_Test.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Drives {@code console-output.cjs}: the console-output renderer (checks, validateLine, rendering, status,
 * scrolling and anchors, earlier lines, tooltip, markers).  One method per case, plus a catch-all that fails on any
 * case without a method of its own.
 */
class ViewsJs_ConsoleOutput_Test extends TestBase {

	private static Map<?,?> r() { return report("console-output.cjs"); }

	@Test void a01_vectors() { assertAllTrue(r(), "chk01_vectors"); }
	@Test void a02_rgbWhitespaceIsAsciiOnly() { assertAllTrue(r(), "chk02_rgbWhitespaceIsAsciiOnly"); }
	@Test void a03_structuralDrops() { assertAllTrue(r(), "chk03_structuralDrops"); }
	@Test void a04_memberRemoval() { assertAllTrue(r(), "chk04_memberRemoval"); }
	@Test void a05_unknownMembersIgnored() { assertAllTrue(r(), "chk05_unknownMembersIgnored"); }
	@Test void a06_normalisationAndCopy() { assertAllTrue(r(), "chk06_normalisationAndCopy"); }
	@Test void b01_numbering() { assertAllTrue(r(), "core01_numbering"); }
	@Test void b02_multiLineIsOneRow() { assertAllTrue(r(), "core02_multiLineIsOneRow"); }
	@Test void b03_colourPrecedence() { assertAllTrue(r(), "core03_colourPrecedence"); }
	@Test void b04_blockMatrix() { assertAllTrue(r(), "core04_blockMatrix"); }
	@Test void b05_inertRendering() { assertAllTrue(r(), "core05_inertRendering"); }
	@Test void b06_boldAndScreenReaderPrefix() { assertAllTrue(r(), "core06_boldAndScreenReaderPrefix"); }
	@Test void b07_nonFatalWarnings() { assertAllTrue(r(), "core07_nonFatalWarnings"); }
	@Test void b08_configErrorThrowsBeforeDom() { assertAllTrue(r(), "core08_configErrorThrowsBeforeDom"); }
	@Test void b09_anchorPrefix() { assertAllTrue(r(), "core09_anchorPrefix"); }
	@Test void b10_rowsAndCompact() { assertAllTrue(r(), "core10_rowsAndCompact"); }
	@Test void b11_destroyIdempotent() { assertAllTrue(r(), "core11_destroyIdempotent"); }
	@Test void b12_image() { assertAllTrue(r(), "core12_image"); }
	@Test void b13_showTime() { assertAllTrue(r(), "core13_showTime"); }
	@Test void f01_hoverShowsAndHides() { assertAllTrue(r(), "tip01_hoverShowsAndHides"); }
	@Test void f02_focusAndEscape() { assertAllTrue(r(), "tip02_focusAndEscape"); }
	@Test void f03_positionStaysInTheViewport() { assertAllTrue(r(), "tip03_positionStaysInTheViewport"); }
	@Test void f04_onlyTooltipBlocksTrigger() { assertAllTrue(r(), "tip04_onlyTooltipBlocksTrigger"); }
	@Test void f05_destroyUnlinks() { assertAllTrue(r(), "tip05_destroyUnlinks"); }
	@Test void g01_toggleAppearsAfterFirstMarker() { assertAllTrue(r(), "mk01_toggleAppearsAfterFirstMarker"); }
	@Test void g02_toggleSwitchesAndPersists() { assertAllTrue(r(), "mk02_toggleSwitchesAndPersists"); }
	@Test void g03_storedChoiceWins() { assertAllTrue(r(), "mk03_storedChoiceWins"); }
	@Test void g04_setMarkersApi() { assertAllTrue(r(), "mk04_setMarkersApi"); }
	@Test void g05_classify() { assertAllTrue(r(), "mk05_classify"); }
	@Test void g06_hiddenMarkerTargetIsRevealed() { assertAllTrue(r(), "mk06_hiddenMarkerTargetIsRevealed"); }
	@Test void g07_noStubsLeft() { assertAllTrue(r(), "mk07_noStubsLeft"); }
	@Test void c01_pendingThenRunningWithZeroLines() { assertAllTrue(r(), "status01_pendingThenRunningWithZeroLines"); }
	@Test void c02_elapsedWithSkew() { assertAllTrue(r(), "status02_elapsedWithSkew"); }
	@Test void c03_durationForms() { assertAllTrue(r(), "status03_durationForms"); }
	@Test void c04_tickerStopsOnTerminal() { assertAllTrue(r(), "status04_tickerStopsOnTerminal"); }
	@Test void c05_liveRegionAndStateStyle() { assertAllTrue(r(), "status05_liveRegionAndStateStyle"); }
	@Test void c06_busMessages() { assertAllTrue(r(), "status06_busMessages"); }
	@Test void c07_destroyStopsTicker() { assertAllTrue(r(), "status07_destroyStopsTicker"); }
	@Test void d01_sticksToBottom() { assertAllTrue(r(), "scroll01_sticksToBottom"); }
	@Test void d02_scrollUpPausesAndBackResticks() { assertAllTrue(r(), "scroll02_scrollUpPausesAndBackResticks"); }
	@Test void d03_jumpButton() { assertAllTrue(r(), "scroll03_jumpButton"); }
	@Test void d04_programmaticScrollIsGuarded() { assertAllTrue(r(), "scroll04_programmaticScrollIsGuarded"); }
	@Test void d05_anchorOnMountIsQueued() { assertAllTrue(r(), "scroll05_anchorOnMountIsQueued"); }
	@Test void d06_hashchange() { assertAllTrue(r(), "scroll06_hashchange"); }
	@Test void d07_queuedTargetMissingAtTerminal() { assertAllTrue(r(), "scroll07_queuedTargetMissingAtTerminal"); }
	@Test void d08_gapFallsToNextRow() { assertAllTrue(r(), "scroll08_gapFallsToNextRow"); }
	@Test void d09_holdUntilUserInput() { assertAllTrue(r(), "scroll09_holdUntilUserInput"); }
	@Test void d10_imageLoadRepins() { assertAllTrue(r(), "scroll10_imageLoadRepins"); }
	@Test void d11_disabledAnchorsIgnoreTheHash() { assertAllTrue(r(), "scroll11_disabledAnchorsIgnoreTheHash"); }
	@Test void e01_seedingShowsAndRemovesTheControl() { assertAllTrue(r(), "earlier01_seedingShowsAndRemovesTheControl"); }
	@Test void e02_loadPreservesScrollAndFocus() { assertAllTrue(r(), "earlier02_loadPreservesScrollAndFocus"); }
	@Test void e03_oneRequestAtATime() { assertAllTrue(r(), "earlier03_oneRequestAtATime"); }
	@Test void e04_lastPageRemovesControlAndMovesFocus() { assertAllTrue(r(), "earlier04_lastPageRemovesControlAndMovesFocus"); }
	@Test void e05_failureRetries() { assertAllTrue(r(), "earlier05_failureRetries"); }
	@Test void e06_goneBecomesTheNotice() { assertAllTrue(r(), "earlier06_goneBecomesTheNotice"); }
	@Test void e07_abortIsSilent() { assertAllTrue(r(), "earlier07_abortIsSilent"); }
	@Test void e08_nonAscendingIsFatal() { assertAllTrue(r(), "earlier08_nonAscendingIsFatal"); }
	@Test void e09_duplicatesIgnoredAndBadTokenFatal() { assertAllTrue(r(), "earlier09_duplicatesIgnoredAndBadTokenFatal"); }
	@Test void e10_appendTrimShowsTheNotice() { assertAllTrue(r(), "earlier10_appendTrimShowsTheNotice"); }
	@Test void e11_prependRespectsTheCap() { assertAllTrue(r(), "earlier11_prependRespectsTheCap"); }
	@Test void e12_queuedTargetAutoLoads() { assertAllTrue(r(), "earlier12_queuedTargetAutoLoads"); }
	@Test void e13_autoLoadExhaustedFallsBack() { assertAllTrue(r(), "earlier13_autoLoadExhaustedFallsBack"); }

	@Test void h01_sameNReplacesTheOpenRow() { assertAllTrue(r(), "open01_sameNReplacesTheOpenRow"); }
	@Test void h02_closedRowIsNeverReplaced() { assertAllTrue(r(), "open02_closedRowIsNeverReplaced"); }
	@Test void h03_openRowIsNotAnnounced() { assertAllTrue(r(), "open03_openRowIsNotAnnounced"); }
	@Test void h04_tailFollowKept() { assertAllTrue(r(), "open04_tailFollowKept"); }
	@Test void h05_validateKeepsOnlyOpenTrue() { assertAllTrue(r(), "open05_validateKeepsOnlyOpenTrue"); }
	@Test void h06_patchKeepsTargetState() { assertAllTrue(r(), "open06_patchKeepsTargetState"); }

	@Test void z01_everyCasePasses() {
		var r = r();
		assertFalse(r.isEmpty(), "harness reported no cases");
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
	}
}
