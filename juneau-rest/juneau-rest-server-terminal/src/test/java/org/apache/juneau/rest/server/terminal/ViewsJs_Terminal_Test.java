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
package org.apache.juneau.rest.server.terminal;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Drives {@code terminal.cjs}: the terminal engine (poll loop, write queue, step-line map, truncation, retries) against
 * fakes, plus one case against the real xterm.js bundle, and the DOM-free input state machine.  One method per case,
 * plus a catch-all that fails on any case without a method of its own.
 */
class ViewsJs_Terminal_Test extends TestBase {

	private static Map<?,?> r() {
		var r = TerminalHarness.nodeReport();
		assumeTrue(r != null, "node not available - skipped");
		return r;
	}

	private static void ok(String key) {
		var r = r();
		assertEquals(true, r.get(key), () -> key + " -> " + r.get(key));
	}

	@Test void a01_fontFitArithmeticAndClamp() { ok("f01_fontFitArithmeticAndClamp"); }
	@Test void a02_parseHashAndFormatSize() { ok("f02_parseHashAndFormatSize"); }
	@Test void a03_findNextWrapsBothWays() { ok("f03_findNextWrapsBothWays"); }
	@Test void a04_cardHandlerIsQueued() { ok("c01_cardHandlerIsQueued"); }
	@Test void a05_linkHandlerOpensOnlyHttp() { ok("f04_linkHandlerOpensOnlyHttp"); }
	@Test void b01_writesAreSerialized() { ok("w01_writesAreSerialized"); }
	@Test void b02_catchUpFetchesAtOnce() { ok("p01_catchUpFetchesAtOnce"); }
	@Test void b03_idleBacksOffAfterThreeEmptyPolls() { ok("p02_idleBacksOffAfterThreeEmptyPolls"); }
	@Test void b04_stopsOnDone() { ok("p03_stopsOnDone"); }
	@Test void b05_exitBadgeFromEvents() { ok("p04_exitBadgeFromEvents"); }
	@Test void b06_bytesDoneWaitsForTheEvents() { ok("p05_bytesDoneWaitsForTheEvents"); }
	@Test void b07_hiddenTabPausesAndResumes() { ok("v01_hiddenTabPausesAndResumes"); }
	@Test void b08_eventsThatNeverFinishGiveUp() { ok("p06_eventsThatNeverFinishGiveUp"); }
	@Test void b09_zeroProgressCatchUpStops() { ok("p07_zeroProgressCatchUpStops"); }
	@Test void a06_handleIsNullUntilMounted() { ok("h01_handleIsNullUntilMounted"); }
	@Test void c01_retryLadderKeepsTheTerminal() { ok("r01_retryLadderKeepsTheTerminal"); }
	@Test void c02_fiveHundredRetriesAndFourHundredStops() { ok("r02_fiveHundredRetriesAndFourHundredStops"); }
	@Test void c03_416ResetsAndReplays() { ok("r03_416ResetsAndReplays"); }
	@Test void c04_notFoundAndGone() { ok("r04_notFoundAndGone"); }
	@Test void c05_events410RefetchesFromTheStart() { ok("r05_events410RefetchesFromTheStart"); }
	@Test void c06_eventsUnavailableTurnsStepsOff() { ok("r06_eventsUnavailableTurnsStepsOff"); }
	@Test void c07_malformedEventsPageTurnsStepsOff() { ok("r07_malformedEventsPageTurnsStepsOff"); }
	@Test void d01_truncationSequenceBytes() { ok("t01_truncationSequenceBytes"); }
	@Test void d02_trimmedStartBannerAndLoadAll() { ok("t02_trimmedStartBannerAndLoadAll"); }
	@Test void d03_slowLoadAllWarning() { ok("t03_slowLoadAllWarning"); }
	@Test void e01_splitAtOffsetsRecordsTheLine() { ok("s01_splitAtOffsetsRecordsTheLine"); }
	@Test void e02_scrollToOffsetPicksTheNearestStepAtOrBelow() { ok("s02_scrollToOffsetPicksTheNearestStepAtOrBelow"); }
	@Test void e03_lateStepUsesTheNearestCheckpoint() { ok("s03_lateStepUsesTheNearestCheckpoint"); }
	@Test void e04_scrollBeforeTheTrimLoadsAll() { ok("s04_scrollBeforeTheTrimLoadsAll"); }
	@Test void e05_followScrollsToTheBottom() { ok("s05_followScrollsToTheBottom"); }
	@Test void e06_deepLinkBeforeTheFirstPollLoadsAll() { ok("s06_deepLinkBeforeTheFirstPollLoadsAll"); }
	@Test void f01_realXtermLinesAndCells() { ok("x01_realXtermLinesAndCells"); }
	@Test void g01_pressProbes() { ok("i01_pressProbes"); }
	@Test void g02_liveArms() { ok("i02_liveArms"); }
	@Test void g03_deniedDisablesTheToggle() { ok("i03_deniedDisablesTheToggle"); }
	@Test void g04_probeTimeout() { ok("i04_probeTimeout"); }
	@Test void g05_batching() { ok("i05_batching"); }
	@Test void g06_largePasteSplitsAt4096() { ok("i06_largePasteSplitsAt4096"); }
	@Test void g07_splitIsCodePointSafe() { ok("i07_splitIsCodePointSafe"); }
	@Test void g08_escEscDisarms() { ok("i08_escEscDisarms"); }
	@Test void g09_busyShowsTheThrottledChip() { ok("i09_busyShowsTheThrottledChip"); }
	@Test void g10_disconnectDropsKeys() { ok("i10_disconnectDropsKeys"); }
	@Test void g11_runEndedDisablesTheToggle() { ok("i11_runEndedDisablesTheToggle"); }
	@Test void g12_otherIdIgnored() { ok("i12_otherIdIgnored"); }
	@Test void g13_closedWhileArmed() { ok("i13_closedWhileArmed"); }
	@Test void g14_disarmFlushesAtMostOneFrame() { ok("i14_disarmFlushesAtMostOneFrame"); }
	@Test void g15_linkForPicksTheBridge() { ok("l01_linkForPicksTheBridge"); }
	@Test void g16_reconnectClearsTheDisconnectedChip() { ok("i15_reconnectClearsTheDisconnectedChip"); }
	@Test void g17_pressWhileDisconnected() { ok("i16_pressWhileDisconnected"); }
	@Test void g18_pressWhileProbing() { ok("i17_pressWhileProbing"); }
	@Test void g19_disposeDuringProbing() { ok("i18_disposeDuringProbing"); }
	@Test void g20_queueAtTheCapIsAccepted() { ok("i19_queueAtTheCapIsAccepted"); }
	@Test void g21_oneByteOverTheCapIsRefused() { ok("i20_oneByteOverTheCapIsRefused"); }
	@Test void g22_capCountsUtf8BytesNotChars() { ok("i21_capCountsUtf8BytesNotChars"); }
	@Test void g23_queuedKeysStillDrainAfterARefusal() { ok("i22_queuedKeysStillDrainAfterARefusal"); }
	@Test void g24_refusalChipSurvivesTheDrain() { ok("i23_refusalChipSurvivesTheDrain"); }
	@Test void g25_throttledChipSurvivesTheDrain() { ok("i24_throttledChipSurvivesTheDrain"); }
	@Test void h01_nudgeAheadFetchesNow() { ok("n01_nudgeAheadFetchesNow"); }
	@Test void h02_nudgeBehindIgnored() { ok("n02_nudgeBehindIgnored"); }
	@Test void h03_inFlightOneFollowUp() { ok("n03_inFlightOneFollowUp"); }
	@Test void h04_inFlightFailureOneRetry() { ok("n03b_inFlightFailureOneRetry"); }
	@Test void h05_nudgeModeSlowsPoll() { ok("n04_nudgeModeSlowsPoll"); }
	@Test void h06_disconnectRestores() { ok("n05_disconnectRestores"); }
	@Test void h07_disconnectWhileIdleKeepsIdle() { ok("n05b_disconnectWhileIdleKeepsIdle"); }
	@Test void h08_connectAloneNoSlowdown() { ok("n06_connectAloneNoSlowdown"); }
	@Test void h09_doneNudge() { ok("n07_doneNudge"); }
	@Test void h10_badPayloadIgnored() { ok("n08_badPayloadIgnored"); }
	@Test void h11_stoppedIgnores() { ok("n09_stoppedIgnores"); }
	@Test void h12_unprobedIgnored() { ok("n10_unprobedIgnored"); }
	@Test void h13_refreshAboveFiveKept() { ok("n11_refreshAboveFiveKept"); }
	@Test void h14_hiddenTabDefersNudge() { ok("n12_hiddenTabDefersNudge"); }
	@Test void h15_nudgeLinkFor() { ok("n13_nudgeLinkFor"); }
	@Test void h16_followUpOnlyWhenStillBehind() { ok("n03c_followUpOnlyWhenStillBehind"); }
	@Test void h17_replayInFlightNoDoubleTick() { ok("n14_replayInFlightNoDoubleTick"); }
	@Test void h17b_replayDropsLeftoverNudgeEnd() { ok("n14b_replayDropsLeftoverNudgeEnd"); }
	@Test void h18_nudgeDuringRetryKeepsBackoff() { ok("n15_nudgeDuringRetryKeepsBackoff"); }
	@Test void h19_disconnectDuringRetryKeepsBackoff() { ok("n16_disconnectDuringRetryKeepsBackoff"); }

	@Test void i01_convertEolReachesTheTerminal() { ok("o01_convertEolReachesTheTerminal"); }
	@Test void i02_unknownKeyIsIgnored() { ok("o02_unknownKeyIsIgnored"); }
	@Test void i03_juneauOwnedKeysCannotBeOverridden() { ok("o03_juneauOwnedKeysCannotBeOverridden"); }
	@Test void i04_wrongTypesAreIgnored() { ok("o04_wrongTypesAreIgnored"); }
	@Test void i05_validScrollbackAndFontFamilyMerge() { ok("o05_validScrollbackAndFontFamilyMerge"); }
	@Test void i06_absentTerminalCopiesBase() { ok("o06_absentTerminalCopiesBase"); }

	@Test void j01_termThemeFromTokens() { ok("t01_termThemeFromTokens"); }
	@Test void j02_fallbacks() { ok("t02_fallbacks"); }
	@Test void j03_cardCannotOverrideTheme() { ok("t03_cardCannotOverrideTheme"); }
	@Test void j04_themeChangedUpdates() { ok("t04_themeChangedUpdates"); }
	@Test void j05_listenerRemovedOnCleanup() { ok("t05_listenerRemovedOnCleanup"); }

	@Test void z01_everyCasePasses() {
		var r = r();
		assertEquals(88, r.size(), () -> "case count changed; add a method per new case: " + r.keySet());
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
	}
}
