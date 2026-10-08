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
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Spec 11.4 a11 in the real browser: the Jobs page's {@code <@card type="console-output">} cards mount through the
 * shell's built-in handler with no page script, and the in-memory console tails a running groom job.
 */
@Covers(value="job-log-card", pages="/console/ops/jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreConsoleOutput_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String LINES = "document.querySelectorAll('#groom-output-body .juneau-co-line').length";
	private static final String FILE_LINES = "document.querySelectorAll('#groom-file-body .juneau-co-line').length";

	/** The card hosts, and the inline scripts that mention the console-output module (there must be none). */
	private static final String CARDS = "(() => ({"
		+ " main: document.getElementById('groom-output')?.getAttribute('data-juneau-card'),"
		+ " file: document.getElementById('groom-file')?.getAttribute('data-juneau-card'),"
		+ " pageScripts: [...document.querySelectorAll('script:not([src])')]"
		+ "   .filter(s => (s.type || 'text/javascript') !== 'application/json' && s.textContent.includes('consoleOutput')).length"
		+ "}))()";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		// A fresh, non-verbose run, so the page is opened while it is still RUNNING.
		var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/console/ops/jobs/groom"))
			.header("Content-Type", "application/json").header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"pet\":\"Browser\"}")).build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		var runId = String.valueOf(((Map<?,?>) Json.to(res.body(), Map.class).get("row")).get("id"));
		report = run("console-output", base, List.of(
			Map.of("name", "jobs", "path", "/console/ops/jobs?run=" + runId,
				"actions", List.of(
					Map.of("waitFor", LINES + " > 0", "timeoutMs", 15000),
					Map.of("evaluate", "window.__n0 = " + LINES),
					Map.of("waitFor", LINES + " > window.__n0", "timeoutMs", 15000),
					Map.of("waitFor", FILE_LINES + " > 0", "timeoutMs", 15000)),
				"queries", Map.of(
					"cards", CARDS,
					"n0", "window.__n0",
					"lines", LINES,
					"fileLines", FILE_LINES))));
	}

	@Test void a01_ftlCardsMountWithNoPageScript() {
		var c = assertClean(report, "jobs");
		assertBean(query(c, "cards"), "main,file,pageScripts", "console-output,console-output,0");
	}

	@Test void a02_mainConsoleTailsTheRunningJob() {
		var c = assertClean(report, "jobs"); // timeouts is empty, so the growth waitFor succeeded
		var n0 = ((Number) query(c, "n0")).intValue();
		assertTrue(n0 > 0, () -> "n0=" + n0);
		assertTrue(((Number) query(c, "lines")).intValue() > n0, () -> "lines=" + query(c, "lines") + ", n0=" + n0);
	}

	@Test void a03_fileConsoleShowsTheTranscript() {
		assertTrue(((Number) query(assertClean(report, "jobs"), "fileLines")).intValue() > 0);
	}
}
