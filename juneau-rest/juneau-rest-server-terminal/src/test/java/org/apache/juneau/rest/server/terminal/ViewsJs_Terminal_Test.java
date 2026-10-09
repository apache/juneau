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
 * fakes, plus one case against the real xterm.js bundle.  One method per case, plus a catch-all that fails on any
 * case without a method of its own.
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

	@Test void z01_everyCasePasses() {
		var r = r();
		assertEquals(31, r.size(), () -> "case count changed; add a method per new case: " + r.keySet());
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
	}
}
