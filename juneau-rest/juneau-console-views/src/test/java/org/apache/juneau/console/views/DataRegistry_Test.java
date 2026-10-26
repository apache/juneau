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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** The DataRegistry {@code juneau-data.js} under the views DOM shim.  Runs when node is on PATH. */
class DataRegistry_Test extends TestBase {
	@Test void a01_registry() {
		var r = NodeHarness.report("data-registry.cjs", ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.DATA_JS_RESOURCE);
		assumeTrue(r != null, "node (or data-registry.cjs) not available - JS layer skipped");
		assertEquals(1, ((Number)r.get("a01_put_returnsVersion1")).intValue());
		for (var k : List.of("a02_get_sameColumnsByReference", "a03_previousHeldDuringSwap", "a04_twoBackDropped",
				"a05_unknownSource_null", "a06_partial", "a07_partialCleared_null", "a09_release"))
			assertEquals(true, r.get(k), k);
		assertEquals(List.of(true, false, false, false), r.get("a08_isHandle"));
	}
}
