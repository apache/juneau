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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The generated Swagger page in the real browser: its bundled stylesheet and script load, it logs no script errors,
 * and the script's toggles open and close an operation block.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreSwaggerUi_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String FIRST_OP = ".swagger-ui .op-block";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("swagger-ui", base, List.of(
			Map.of("name", "swagger", "path", "/petstore/api",
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('" + FIRST_OP + "')"),
					Map.of("evaluate", "window.__before = document.querySelector('" + FIRST_OP + "').className"),
					Map.of("click", FIRST_OP + " .op-block-summary")),
				"queries", Map.of(
					"styleLength", "document.querySelector('.swagger-ui style')?.textContent.length ?? 0",
					"scriptLength", "document.querySelector('.swagger-ui script')?.textContent.length ?? 0",
					"toggle", "typeof toggleOpBlock",
					"before", "window.__before",
					"after", "document.querySelector('" + FIRST_OP + "').className"))));
	}

	@Test void a01_loadsWithoutScriptErrors() {
		assertClean(report, "swagger");
	}

	@Test void a02_stylesheetAndScriptAreInlined() {
		var c = assertClean(report, "swagger");
		assertTrue(((Number) query(c, "styleLength")).intValue() > 0, "Swagger UI stylesheet is empty");
		assertTrue(((Number) query(c, "scriptLength")).intValue() > 0, "Swagger UI script is empty");
		assertString("function", query(c, "toggle"));
	}

	@Test void a03_operationBlockToggles() {
		var c = assertClean(report, "swagger");
		assertContains("op-block-closed", query(c, "before"));
		assertContains("op-block-open", query(c, "after"));
	}
}
