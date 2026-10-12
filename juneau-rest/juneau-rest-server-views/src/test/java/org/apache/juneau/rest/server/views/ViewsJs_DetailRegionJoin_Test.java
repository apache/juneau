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
 * A row-detail region joins the panel's in-flight expand envelope (one GET per expand) even when the region appends
 * its {@code params} to the projected {@code dataUrl}: the join compares the substituted {@code dataUrl} before the
 * params are appended (an author-written query is kept in the comparison and does not join).
 * Drives {@code detail-region-join.cjs}.
 */
class ViewsJs_DetailRegionJoin_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("detail-region-join.cjs");
		assumeTrue(r != null, "node not available or detail-region-join.cjs not found - behavioral layer skipped");
		assertEquals(true, r.get("hasFixture"), () -> "the harness never built a detail panel: " + r);
		return r;
	}

	private static void assertAllTrue(String... keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@Test void a01_regionWithParamsJoinsTheExpandGet() { assertAllTrue("a01_withParamsSingleGet"); }
	@Test void a02_regionWithoutParamsJoins() { assertAllTrue("a02_noParamsSingleGet"); }
	@Test void a03_authorQueryOnTheDataUrlIsNotAJoin() { assertAllTrue("a03_authorQueryFetchesSeparately"); }
	@Test void a04_explicitOtherUrlStillFetchesItself() { assertAllTrue("a04_explicitOtherUrlStillFetches"); }
	@Test void a05_prefixIsNotAJoin() { assertAllTrue("a05_prefixIsNotAJoin"); }
	@Test void a06_trailingSlashMismatchFailsSafeToTwoGets() { assertAllTrue("a06_trailingSlashFetchesTwice"); }
	@Test void a07_dotSegmentResolvesToTheSameResource() { assertAllTrue("a07_dotSegmentResolvesAndJoins"); }
	@Test void a08_relativePathResolvesAgainstThePage() { assertAllTrue("a08_relativeResolvesAndJoins"); }
	@Test void a09_paramsStillJoinWhenThePageHasALocation() { assertAllTrue("a09_paramsJoinUnderLocation"); }
	@Test void a10_relativePathUnderAnotherDirectoryIsNotAJoin() { assertAllTrue("a10_relativeUnderOtherDirectoryDoesNotJoin"); }
	@Test void a11_authorQuerySeparatesUnderLocation() { assertAllTrue("a11_authorQuerySeparatesUnderLocation"); }
	@Test void a12_absoluteSameOriginLiteralJoins() { assertAllTrue("a12_absoluteSameOriginJoins"); }
	@Test void a13_crossOriginLiteralDoesNotJoin() { assertAllTrue("a13_crossOriginDoesNotJoin"); }
	@Test void a14_refreshAfterAJoinedExpandIssuesARealGet() {
		assertAllTrue("a14_expandIsOneGet", "a14_refreshIssuesSecondGet", "a14_refreshSendsParams", "a14_refreshReadsNewBody");
	}
}
