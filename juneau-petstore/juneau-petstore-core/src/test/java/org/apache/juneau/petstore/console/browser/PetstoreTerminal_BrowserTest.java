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

import java.net.http.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * In the real browser: the Jobs page's {@code <@card type="terminal">} cards mount through the shell's
 * built-in handler with no page script, and the in-memory terminal follows a running groom job.
 */
@Covers(value="terminal-card", pages="/console/ops/jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreTerminal_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String ROWS = "Array.from(document.querySelectorAll('#groom-output .xterm-rows > div')).map(d => d.textContent).join('\\n')";
	private static final String FILE_ROWS = "Array.from(document.querySelectorAll('#groom-file .xterm-rows > div')).map(d => d.textContent).join('\\n')";

	/** The card hosts, and the inline scripts that mention the terminal module (there must be none). */
	private static final String CARDS = "(() => ({"
		+ " main: document.querySelector('#groom-output .juneau-term') ? 'terminal' : null,"
		+ " file: document.querySelector('#groom-file .juneau-term') ? 'terminal' : null,"
		+ " pageScripts: [...document.querySelectorAll('script:not([src])')]"
		+ "   .filter(s => (s.type || 'text/javascript') !== 'application/json' && s.textContent.includes('JuneauTerminal')).length"
		+ "}))()";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		// A fresh, non-verbose run, so the page is opened while it is still RUNNING.
		var res = HttpClient.newHttpClient().send(PetstoreTestServer.consolePost(base, "/console/ops/jobs/groom", "{\"pet\":\"Browser\"}"), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		var runId = String.valueOf(((Map<?,?>) Json.to(res.body(), Map.class).get("row")).get("id"));
		report = run("terminal", base, List.of(
			Map.of("name", "jobs", "path", "/console/ops/jobs?run=" + runId,
				"actions", List.of(
					// The run takes about 8 s at the test server's 150 ms period; the terminals follow it to the last lines.
					Map.of("waitFor", ROWS + ".includes('Trimmed nail 12 of 12')", "timeoutMs", 30000),
					Map.of("waitFor", FILE_ROWS + ".includes('[51/52] step done')", "timeoutMs", 30000)),
				"queries", Map.of(
					"cards", CARDS,
					"rows", ROWS,
					"fileRows", FILE_ROWS))));
	}

	@Test void a01_ftlCardsMountWithNoPageScript() {
		var c = assertClean(report, "jobs");
		assertBean(query(c, "cards"), "main,file,pageScripts", "terminal,terminal,0");
	}

	@Test void a02_mainTerminalFollowsTheRunningJob() {
		var rows = String.valueOf(query(assertClean(report, "jobs"), "rows")); // timeouts is empty, so the waitFors succeeded
		assertTrue(rows.contains("Trimmed nail 12 of 12"), () -> rows);
	}

	@Test void a03_fileTerminalShowsTheTranscript() {
		assertTrue(String.valueOf(query(assertClean(report, "jobs"), "fileRows")).contains("[51/52] step done"));
	}
}
