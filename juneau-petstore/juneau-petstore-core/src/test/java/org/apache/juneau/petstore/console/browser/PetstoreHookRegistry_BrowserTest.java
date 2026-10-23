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
package org.apache.juneau.petstore.console.browser;

import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Hook coverage: reads {@code JuneauViews.HOOK_NAMESPACES} from the toolkit a shipped console page loads, and checks
 * that every name in it is claimed by a {@link HookCovers} browser test or listed in
 * {@link PetstoreCoverage_Test#HOOK_EXEMPT} with a reason.  The checks themselves are in
 * {@link PetstoreCoverage_Test#hookProblems}; this class only supplies the live registry.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreHookRegistry_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("hook-registry", base, List.of(
			Map.of("name", "registry", "path", "/console/store",
				"actions", List.of(Map.of("waitFor", "window.JuneauViews && JuneauViews.HOOK_NAMESPACES", "timeoutMs", 8000)),
				"queries", Map.of("names", "Array.from(JuneauViews.HOOK_NAMESPACES)", "frozen", "Object.isFrozen(JuneauViews.HOOK_NAMESPACES)"))));
	}

	@SuppressWarnings({
		"unchecked" // names is a JSON array of strings.
	})
	private static List<String> registry() {
		return (List<String>)query(assertClean(report, "registry"), "names");
	}

	@Test void a01_registryIsReadFromThePage() {
		assertFalse(registry().isEmpty(), "JuneauViews.HOOK_NAMESPACES is empty");
		assertEquals(Boolean.TRUE, query(assertClean(report, "registry"), "frozen"));
	}

	@Test void c01_everyHookNamespaceIsExercised() {
		var problems = PetstoreCoverage_Test.hookProblems(registry(), PetstoreCoverage_Test.hookClaims(), PetstoreCoverage_Test.HOOK_EXEMPT);
		assertEquals("", String.join("\n", problems));
	}
}
