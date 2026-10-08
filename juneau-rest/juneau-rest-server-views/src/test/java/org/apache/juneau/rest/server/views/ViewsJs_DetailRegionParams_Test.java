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
 * A datatables catalog's {@code detail.region.params} are copied into the row-detail region's declared descriptor,
 * so a row-detail console-output region receives its params.  Drives {@code detail-region-params.cjs}.
 */
class ViewsJs_DetailRegionParams_Test extends TestBase {

	private static Map<?,?> report() {
		var r = RegionsHarness.report("detail-region-params.cjs");
		assumeTrue(r != null, "node not available or detail-region-params.cjs not found - behavioral layer skipped");
		return r;
	}

	private static void assertAllTrue(String... keys) {
		var r = report();
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	@Test void a01_paramsCopiedWithoutDataUrl() { assertAllTrue("a01_paramsCopied", "a01_noDataUrl", "a01_populate"); }
	@Test void a02_dataUrlAndParamsTogether() { assertAllTrue("a02_dataUrlAndParams"); }
	@Test void a03_noParamsLeavesTheDeclarationUnchanged() { assertAllTrue("a03_noParamsUnchanged"); }
	@Test void a04_nonObjectParamsAreDropped() { assertAllTrue("a04_nonObjectsDropped"); }
	@Test void a05_emptyObjectIsWritten() { assertAllTrue("a05_emptyObjectWritten"); }
	@Test void a06_paramsReachThePopulator() { assertAllTrue("a06_roundTrip", "a06_noErrors"); }
}
