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

/** Node behavioral checks for async jobs started through {@code JuneauViews.actions.run} (no row, banner-hosted) and for {@code cancelJob}'s contract. */
class ViewsJs_JobHost_Test {

	private static Map<?,?> r() {
		var r = NodeHarness.report("job-host.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or job-host.cjs) not available - behavioral layer skipped");
		return r;
	}

	private static String s(String k) { return (String)r().get(k); }
	private static void yes(String k) { assertEquals(true, r().get(k), k); }

	@Test void r01_runStaysPendingWhileTheJobStreamsThenResolvesWithItsResult() {
		yes("r01_pending"); yes("r01_sourceOpened"); yes("r01_progressShown"); yes("r01_cancelButton");
		assertEquals("Cancel Run job", s("r01_cancelLabel"));
		assertEquals("40%", s("r01_progressText"));
		assertEquals("success", s("r01_outcome"));
		assertEquals("all done", s("r01_message"));
		yes("r01_progressGone"); yes("r01_closed"); yes("r01_painted");
	}

	@Test void r02_streamErrorResolvesUnknown() {
		assertEquals("unknown", s("r02_outcome"));
		assertEquals("the progress stream was interrupted", s("r02_message"));
	}

	@Test void r03_noEventSourceResolvesUnknownWithTheBrowserNotice() {
		assertEquals("unknown", s("r03_outcome"));
		assertEquals("live progress is unavailable in this browser", s("r03_message"));
	}

	@Test void r04_offSiteStreamUrlOpensNothing() {
		assertEquals(0, ((Number)r().get("r04_opened")).intValue());
		assertEquals("unknown", s("r04_outcome"));
		assertEquals("the job started, but its progress URL is not on this site, so progress is not shown", s("r04_message"));
	}

	@Test void r05_throwingEventSourceResolvesUnknown() {
		assertEquals("unknown", s("r05_outcome"));
		assertEquals("the progress stream could not be opened", s("r05_message"));
	}

	@Test void r06_offSiteCancelUrlShowsNoCancelButton() { yes("r06_progress"); yes("r06_noCancel"); }

	@Test void r07_cancelPostsOnceWithCsrfAndDisablesTheButton() {
		assertEquals(1, ((Number)r().get("r07_cancelPosts")).intValue());
		yes("r07_csrf"); yes("r07_disabled");
		assertEquals("cancelled", s("r07_outcome"));
	}

	@Test void r08_failedCancelReEnablesAndIsNotTheRunOutcome() {
		yes("r08_reEnabled");
		assertEquals("success", s("r08_outcome"));
	}

	@Test void r09_releaseClosesTheStreamAndSettlesUnknown() {
		yes("r09_closed"); yes("r09_bannerGone"); yes("r09_noBannerHostRebuilt"); yes("r09_noLateText");
		assertEquals("unknown", s("r09_outcome"));
	}

	@Test void r10_secondRunOpensAFreshStream() {
		yes("r10_secondStream");
		assertEquals("failure", s("r10_outcome"));
	}

	@Test void r04b_aStreamUrlWithTrimmableCharactersOpensNothing() {
		assertEquals(0, ((Number)r().get("r04b_opened")).intValue());
		assertEquals("unknown", s("r04b_outcome"));
	}
	@Test void r11_eachJobKeepsItsOwnProgressElement() {
		assertEquals(2, ((Number)r().get("r11_count")).intValue());
		assertEquals("one-again,two", s("r11_texts"));
	}
	@Test void r12_unsafeJobUrlShapesAreNeverStreamedOrCancelled() {
		for (var k : new String[]{"backslash", "lead", "c0", "newline", "proto", "scheme"}) {
			var st = (java.util.Map<?,?>)r().get("r12_stream_" + k);
			assertEquals(0, ((Number)st.get("opened")).intValue(), k);
			assertEquals("unknown", st.get("outcome"), k);
			var ca = (java.util.Map<?,?>)r().get("r12_cancel_" + k);
			assertEquals(true, ca.get("noButton"), k);
			yes("r12_cancelJob_" + k);
			yes("r12_noCancelPost_" + k);
		}
		assertEquals(false, r().get("r12_missingCancel"));
		assertTrue(s("r12_untrustedCancelText").contains("not on this site"), s("r12_untrustedCancelText"));
	}
	@Test void t01_cancelJobReturnsAPromise() { yes("t01_returnsPromise"); }
	@Test void t02_acceptedCancelResolvesTrue() { yes("t02_okTrue"); }
	@Test void t03_untrustedCancelUrlResolvesFalse() { yes("t03_untrustedFalse"); }
	@Test void t04_nonOkCancelResolvesFalse() { yes("t04_nonOkFalse"); }
	@Test void m01_refusalMessageEndingInADotRendersOneTrailingPeriod() {
		assertEquals("Request refused (CSRF_TOKEN_MISMATCH): The token is not valid for this request.", s("m01_refusalDot"));
	}
	@Test void m02_successWithReplayPutsThePeriodAfterTheReplaySuffix() {
		assertEquals("Done: Saved (replay of a previous attempt).", s("m02_successReplayDot"));
	}
	@Test void m03_aRunOfTrailingDotsCollapsesToOne() { assertEquals("Failed: Gone.", s("m03_failureDots")); }
	@Test void m04_aMessageWithoutADotStillGetsOne() { assertEquals("Cancelled: stopped.", s("m04_noDot")); }
	@Test void m05_exclamationAndQuestionMarksAreNotAltered() {
		assertEquals("Failed: Boom!.", s("m05_bangKept"));
		assertEquals("Outcome unknown - the write may or may not have completed: Did it work?.", s("m06_questionKept"));
	}
	@Test void m07_noMessageRendersJustTheLeadAndPeriod() { assertEquals("Done.", s("m07_noMessage")); }
	@Test void m08_cancelledAfterEffectStripsToo() {
		assertEquals("Cancelled after a partial effect: Half done.", s("m08_afterEffectDot"));
	}
	@Test void m09_aMessageOfOnlyDotsRendersAsNoDetail() { assertEquals("Failed.", s("m09_dotsOnly")); }
}
