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
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * The count-badge runtime {@code juneau-badges.js}: labels, hide-at-zero, error handling, scope, visibility, drain
 * refresh, tooltip and click navigation.  Lives in this module because it owns the Node harness plumbing and the
 * DOM shims; runs when node is on PATH.
 */
class ConsoleBadges_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("badges.cjs", ConsoleChromeMixin.BADGES_JS_RESOURCE);
		assumeTrue(r != null, "node (or badges.cjs) not available - JS layer skipped");
		return r;
	}

	@Test void a01_labelsAndAria() {
		var r = report();
		assertEquals("3 changes pending", r.get("plainLabel"));
		assertEquals("1 change pending", r.get("singularLabel"));
		assertEquals("2 changes pending (1 yours)", r.get("mineLabel"));
		assertEquals("2 open", r.get("customLabel"));
		assertEquals("status", r.get("role"));
		assertEquals("polite", r.get("ariaLive"));
		assertEquals("jc-header-badges", r.get("inHeaderBadges"));
	}

	@Test void a02_toneDefaultsToWarningAndDangerPaintsAsError() {
		var r = report();
		assertEquals("warning", r.get("defaultTone"));
		assertEquals("error", r.get("dangerTone"));
	}

	@Test void a03_zeroHidesAndGrowthPulses() {
		var r = report();
		assertEquals(true, r.get("zeroHidden"));
		assertEquals(true, r.get("reshown"));
		assertEquals(true, r.get("pulsed"));
	}

	@Test void a04_forbiddenRemovesBadgeAndStopsPolling() {
		var r = report();
		assertEquals(true, r.get("removedOn403"));
		assertEquals(1, ((Number)r.get("e65Count")).intValue());
		assertEquals(true, r.get("noRescheduleOn403"));
		assertEquals(1, ((Number)r.get("fetchCountAfter403")).intValue());
	}

	@Test void a05_badResponseGoesStaleAndBacksOff() {
		var r = report();
		assertEquals(true, r.get("stale"));
		assertEquals(true, r.get("e66"));
		assertEquals(true, r.get("e65OnNetworkError"));
		assertEquals(List.of(10000, 20000), ((List<?>)r.get("backoff")).stream().map(o -> ((Number)o).intValue()).toList());
	}

	@Test void a06_refreshMsBelowFloorIsClamped() {
		assertEquals(5000, ((Number)report().get("clampedInterval")).intValue());
	}

	@Test void a07_visibleWhenGatesMountAndFetch() {
		var r = report();
		assertEquals(true, r.get("visibleWhenShown"));
		assertEquals(true, r.get("visibleWhenHiddenNotMounted"));
		assertEquals(List.of("/yes"), r.get("visibleWhenFetched"));
	}

	@Test void a08_scopeResolvesFromNavLeaf() {
		var r = report();
		assertEquals("/p?a=b%20c&beanType=Suspension,Hold", r.get("scopedUrl"));
		assertEquals(true, r.get("unscopedPageNoFetch"));
		assertEquals(true, r.get("unscopedPageReschedules"));
	}

	@Test void a09_drainRefreshesLinkedCardsOnlyWhenAnItemLeaves() {
		var r = report();
		assertEquals(0, ((Number)r.get("reloadedOnGrowth")).intValue());
		assertEquals(List.of("rules-table"), r.get("reloadedOnDrain"));
	}

	@Test void a10_tooltipHonorsCapAndCloses() {
		var r = report();
		assertEquals(true, r.get("tooltipOpen"));
		assertEquals("tooltip", r.get("tooltipRole"));
		assertEquals(3, ((Number)r.get("tooltipItems")).intValue());
		assertEquals("and 1 more", r.get("tooltipMore"));
		assertEquals(true, r.get("describedBy"));
		assertEquals(true, r.get("escapeCloses"));
		assertEquals(true, r.get("blurCloses"));
	}

	@Test void a11_clickNavigation() {
		var r = report();
		assertEquals("/ui/changes", r.get("plainNav"));
		assertEquals("/ui/changes?state=filter(beanType%3D%24eq(Suspension))", r.get("filteredNav"));
	}
}
