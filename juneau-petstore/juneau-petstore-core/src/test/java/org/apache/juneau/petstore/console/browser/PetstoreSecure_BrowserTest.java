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
import static org.apache.juneau.test.bct.BctAssertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * P13 in the real browser: the "Try it" buttons get 401 without the demo token, 200 with it, 401 with a wrong one.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreSecure_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> click(String token) {
		return Map.of("name", token, "path", "/console/dev/secure", "allowFailedLoads", true,
			"actions", List.of(
				Map.of("click", "[data-secure-call=\"" + token + "\"]"),
				Map.of("waitFor", "document.getElementById('secure-result').getAttribute('data-status')")),
			"queries", Map.of(
				"status", "document.getElementById('secure-result').getAttribute('data-status')",
				"text", "document.getElementById('secure-result').textContent"));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("secure", base, List.of(click("none"), click("petstore-user"), click("wrong-token")));
	}

	@Test void a01_noToken401() {
		assertString("401", query(assertClean(report, "none"), "status"));
	}

	@Test void a02_demoToken200NamesAlice() {
		var c = assertClean(report, "petstore-user");
		assertString("200", query(c, "status"));
		assertContains("alice", query(c, "text"));
	}

	@Test void a03_wrongToken401() {
		assertString("401", query(assertClean(report, "wrong-token"), "status"));
	}
}
