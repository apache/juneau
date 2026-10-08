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

/** Drives {@code console-output-env-check.cjs}: the console-output test environment's own decorations. */
class ViewsJs_ConsoleOutputEnv_Test extends TestBase {

	static final String VECTORS = "/org/apache/juneau/rest/server/views/console-output-vectors.json";

	static Map<?,?> report(String harness) {
		var r = RegionsHarness.reportWith(harness,
			ViewsMixin.HELPERS_JS_RESOURCE, ViewsMixin.CONSOLE_OUTPUT_JS_RESOURCE, VECTORS);
		assumeTrue(r != null, "node not available or " + harness + " not found - skipped");
		return r;
	}

	static void assertAllTrue(Map<?,?> r, String... keys) {
		for (var k : keys)
			assertEquals(true, r.get(k), () -> k + " -> " + r.get(k) + " in " + r);
	}

	private static Map<?,?> r() { return report("console-output-env-check.cjs"); }

	@Test void a01_fragments() { assertAllTrue(r(), "t01_fragments"); }
	@Test void a02_geometry() { assertAllTrue(r(), "t02_geometry"); }
	@Test void a03_scrollIntoView() { assertAllTrue(r(), "t03_scrollIntoView"); }
	@Test void a04_scrollEvents() { assertAllTrue(r(), "t04_scrollEvents"); }
	@Test void a05_windowState() { assertAllTrue(r(), "t05_windowState"); }
	@Test void a06_fakeDate() { assertAllTrue(r(), "t06_fakeDate"); }
	@Test void a07_loaded() { assertAllTrue(r(), "t07_loaded"); }
}
