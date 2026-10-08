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
 * Drives {@code console-output-poll.cjs}: {@code poll()} against a fake fetch and clock, the earlier-lines loader,
 * {@code mount()} and the {@code console-output} region populator.  One method per case, plus a catch-all that fails
 * on any case without a method of its own.
 */
class ViewsJs_ConsoleOutputPoll_Test extends TestBase {

	private static Map<?,?> r() { return report("console-output-poll.cjs"); }

	@Test void a01_firstRequestIsTailThenAfterNext() { assertAllTrue(r(), "p01_firstRequestIsTailThenAfterNext"); }
	@Test void a02_tailZeroAndAnExistingQuery() { assertAllTrue(r(), "p02_tailZeroAndAnExistingQuery"); }
	@Test void a03_moreRefetchesImmediately() { assertAllTrue(r(), "p03_moreRefetchesImmediately"); }
	@Test void a04_terminalStopsAndResolves() { assertAllTrue(r(), "p04_terminalStopsAndResolves"); }
	@Test void a05_oneRequestInFlight() { assertAllTrue(r(), "p05_oneRequestInFlight"); }
	@Test void a06_backoffSequenceAndCap() { assertAllTrue(r(), "p06_backoffSequenceAndCap"); }
	@Test void a07_retryAfter() { assertAllTrue(r(), "p07_retryAfter"); }
	@Test void a08_fatal4xx() { assertAllTrue(r(), "p08_fatal4xx"); }
	@Test void a09_gone410() { assertAllTrue(r(), "p09_gone410"); }
	@Test void a10_abortIsSilent() { assertAllTrue(r(), "p10_abortIsSilent"); }
	@Test void a11_destroyStopsPolling() { assertAllTrue(r(), "p11_destroyStopsPolling"); }
	@Test void a12_hiddenTabDefers() { assertAllTrue(r(), "p12_hiddenTabDefers"); }
	@Test void a13_emptyMorePageDoesNotSpin() { assertAllTrue(r(), "p13_emptyMorePageDoesNotSpin"); }
	@Test void a14_contractVersionFatalKeepsRows() { assertAllTrue(r(), "p14_contractVersionFatalKeepsRows"); }
	@Test void a15_badTokenOrOrderIsFatal() { assertAllTrue(r(), "p15_badTokenOrOrderIsFatal"); }
	@Test void a16_malformedBodyIsFatal() { assertAllTrue(r(), "p16_malformedBodyIsFatal"); }

	@Test void b01_earlierRequestOneAtATime() { assertAllTrue(r(), "q01_earlierRequestOneAtATime"); }
	@Test void b02_earlierRetryAnd410() { assertAllTrue(r(), "q02_earlierRetryAnd410"); }
	@Test void b03_earlierAbortedWithThePoll() { assertAllTrue(r(), "q03_earlierAbortedWithThePoll"); }
	@Test void b04_anchorAutoLoadsEarlier() { assertAllTrue(r(), "q04_anchorAutoLoadsEarlier"); }
	@Test void b05_anchorStopsWhenNoEarlier() { assertAllTrue(r(), "q05_anchorStopsWhenNoEarlier"); }
	@Test void b06_earlierWorksAfterTerminal() { assertAllTrue(r(), "q06_earlierWorksAfterTerminal"); }

	@Test void c01_returnsCleanupSynchronously() { assertAllTrue(r(), "m01_returnsCleanupSynchronously"); }
	@Test void c02_pollFalseMakesNoFetches() { assertAllTrue(r(), "m02_pollFalseMakesNoFetches"); }
	@Test void c03_urlErrorsThrowBeforeAnyDom() { assertAllTrue(r(), "m03_urlErrorsThrowBeforeAnyDom"); }
	@Test void c04_rowIdSubstitution() { assertAllTrue(r(), "m04_rowIdSubstitution"); }
	@Test void c05_paramRanges() { assertAllTrue(r(), "m05_paramRanges"); }
	@Test void c06_populatorPublishesOnTheBus() { assertAllTrue(r(), "m06_populatorPublishesOnTheBus"); }
	@Test void c07_teardownStopsPolling() { assertAllTrue(r(), "m07_teardownStopsPolling"); }
	@Test void c08_fatalInARegionKeepsCleanup() { assertAllTrue(r(), "m08_fatalInARegionKeepsCleanup"); }

	@Test void z01_everyCasePasses() {
		var r = r();
		assertFalse(r.isEmpty(), "harness reported no cases");
		for (var e : r.entrySet())
			assertEquals(true, e.getValue(), () -> e.getKey() + " -> " + e.getValue());
	}
}
