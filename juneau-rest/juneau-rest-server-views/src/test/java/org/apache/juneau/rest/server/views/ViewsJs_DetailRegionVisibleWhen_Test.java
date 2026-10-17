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

import org.junit.jupiter.api.*;

/**
 * Node behavioral check: a row-detail region's {@code visibleWhen} reads the panel's PARENT row (through
 * {@code hookContextOf}), not the empty child row DataTables wraps the panel in.
 */
class ViewsJs_DetailRegionVisibleWhen_Test {

	private static final String HARNESS = "detail-region-visible-when.cjs";

	private static Map<?,?> report() {
		var r = RegionsHarness.report(HARNESS);
		assumeTrue(r != null, "node not available or " + HARNESS + " not found - behavioral layer skipped");
		return r;
	}

	private static void yes(String...keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " in " + r);
	}

	@Test void a01_contextOfAPanelRegionIsTheParentRow() { yes("contextIsParentRow"); }

	@Test void a02_aRegionWhoseRuleMatchesTheParentRowShowsAndPopulates() { yes("matchingRegionShown", "matchingRegionFetched"); }

	@Test void a03_aRegionWhoseRuleDoesNotMatchIsHiddenAndNeverFetches() { yes("otherRegionHidden", "otherRegionNotFetched"); }
}
