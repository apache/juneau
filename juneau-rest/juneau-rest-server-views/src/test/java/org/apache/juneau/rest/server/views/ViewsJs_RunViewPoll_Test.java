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
 * Drives {@code run-view-poll.cjs}: polling against a fake fetch and clock, {@code mount}, the {@code run-view} region
 * populator and {@code of}.  One method per case, plus a catch-all that fails when the harness reports a case without
 * a method of its own or a method names a case the harness does not have.
 */
class ViewsJs_RunViewPoll_Test extends TestBase {

	static final String VECTORS = "/org/apache/juneau/rest/server/views/run-view-vectors.json";

	/** Every case name the harness is expected to report; each task appends its own. */
	private static final Set<String> EXPECTED = new TreeSet<>(List.of(
		"poll01_firstRequestHasNoQuery_thenAfterNext", "poll02_urlWithQueryUsesAmpersand", "poll03_moreRefetchesAtOnce",
		"poll04_idleWaitsRefreshMs", "poll05_stopsOnTerminal", "poll06_stopsOnDoneWithoutMore",
		"poll07_abortStops", "poll08_hiddenTabPausesAndVisibleResumes", "poll09_backoffOnOutage_andRecovers",
		"poll10_4xxIsFatal", "poll11_408And429Retry", "poll12_410WithTokenResetsAndRefetches",
		"poll13_410WithoutTokenIsFatal", "poll14_contractMismatchIsFatal", "poll15_nonIncreasingSeqIsFatal",
		"poll16_badNextIsFatal", "poll17_oneRequestInFlight", "poll18_neverRejects",
		"poll19_badEventsUrlIsE1", "poll20_emptyMorePageDoesNotSpin", "poll21_replayedPageIsHarmless",
		"mount01_returnsCleanupSynchronously", "mount02_cleanupTearsDownInOrder", "mount03_idSubstitution",
		"mount04_pollFalseNeverFetches", "mount05_populatorRegistered", "mount06_emitsRunViewState",
		"of01_byElementAndById", "of02_unknownIsNull", "of03_sseStyleAppend",
		"chk99_noStubsLeft"
	));

	static Map<?,?> report(String harness) {
		var r = RegionsHarness.reportWith(harness, ViewsMixin.HELPERS_JS_RESOURCE, ViewsMixin.RUN_VIEW_JS_RESOURCE, VECTORS);
		assumeTrue(r != null, "node not available or " + harness + " not found - skipped");
		return r;
	}

	static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static Map<?,?> r() { return report("run-view-poll.cjs"); }

	@Test void a01_firstRequestHasNoQuery_thenAfterNext() { assertAllTrue(r(), "poll01_firstRequestHasNoQuery_thenAfterNext"); }
	@Test void a02_urlWithQueryUsesAmpersand() { assertAllTrue(r(), "poll02_urlWithQueryUsesAmpersand"); }
	@Test void a03_moreRefetchesAtOnce() { assertAllTrue(r(), "poll03_moreRefetchesAtOnce"); }
	@Test void a04_idleWaitsRefreshMs() { assertAllTrue(r(), "poll04_idleWaitsRefreshMs"); }
	@Test void a05_stopsOnTerminal() { assertAllTrue(r(), "poll05_stopsOnTerminal"); }
	@Test void a06_stopsOnDoneWithoutMore() { assertAllTrue(r(), "poll06_stopsOnDoneWithoutMore"); }
	@Test void a07_abortStops() { assertAllTrue(r(), "poll07_abortStops"); }
	@Test void a08_hiddenTabPausesAndVisibleResumes() { assertAllTrue(r(), "poll08_hiddenTabPausesAndVisibleResumes"); }
	@Test void a09_backoffOnOutage_andRecovers() { assertAllTrue(r(), "poll09_backoffOnOutage_andRecovers"); }
	@Test void a10_4xxIsFatal() { assertAllTrue(r(), "poll10_4xxIsFatal"); }
	@Test void a11_408And429Retry() { assertAllTrue(r(), "poll11_408And429Retry"); }
	@Test void a12_410WithTokenResetsAndRefetches() { assertAllTrue(r(), "poll12_410WithTokenResetsAndRefetches"); }
	@Test void a13_410WithoutTokenIsFatal() { assertAllTrue(r(), "poll13_410WithoutTokenIsFatal"); }
	@Test void a14_contractMismatchIsFatal() { assertAllTrue(r(), "poll14_contractMismatchIsFatal"); }
	@Test void a15_nonIncreasingSeqIsFatal() { assertAllTrue(r(), "poll15_nonIncreasingSeqIsFatal"); }
	@Test void a16_badNextIsFatal() { assertAllTrue(r(), "poll16_badNextIsFatal"); }
	@Test void a17_oneRequestInFlight() { assertAllTrue(r(), "poll17_oneRequestInFlight"); }
	@Test void a18_neverRejects() { assertAllTrue(r(), "poll18_neverRejects"); }
	@Test void a19_badEventsUrlIsE1() { assertAllTrue(r(), "poll19_badEventsUrlIsE1"); }
	@Test void a20_emptyMorePageDoesNotSpin() { assertAllTrue(r(), "poll20_emptyMorePageDoesNotSpin"); }
	@Test void a21_replayedPageIsHarmless() { assertAllTrue(r(), "poll21_replayedPageIsHarmless"); }
	@Test void b01_returnsCleanupSynchronously() { assertAllTrue(r(), "mount01_returnsCleanupSynchronously"); }
	@Test void b02_cleanupTearsDownInOrder() { assertAllTrue(r(), "mount02_cleanupTearsDownInOrder"); }
	@Test void b03_idSubstitution() { assertAllTrue(r(), "mount03_idSubstitution"); }
	@Test void b04_pollFalseNeverFetches() { assertAllTrue(r(), "mount04_pollFalseNeverFetches"); }
	@Test void b05_populatorRegistered() { assertAllTrue(r(), "mount05_populatorRegistered"); }
	@Test void b06_emitsRunViewState() { assertAllTrue(r(), "mount06_emitsRunViewState"); }
	@Test void c01_byElementAndById() { assertAllTrue(r(), "of01_byElementAndById"); }
	@Test void c02_unknownIsNull() { assertAllTrue(r(), "of02_unknownIsNull"); }
	@Test void c03_sseStyleAppend() { assertAllTrue(r(), "of03_sseStyleAppend"); }
	@Test void d01_noStubsLeft() { assertAllTrue(r(), "chk99_noStubsLeft"); }

	@Test void z01_everyCasePassesAndNoneIsMissing() {
		var r = r();
		assertFalse(r.isEmpty(), "harness reported no cases");
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
		assertEquals(EXPECTED, new TreeSet<>(r.keySet().stream().map(String::valueOf).toList()), "harness case names");
	}
}
