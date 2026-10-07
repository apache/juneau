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
 * P6 in the real browser: a double submit with one key returns one job, the job finishes, the polled jobs table
 * shows it, and an unknown id is 404.
 */
@Covers(value="async-job", pages="/console/ops/jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreAsyncJob_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String SUBMIT_TWICE = "(async () => {"
		+ " const key = 'browser-' + Date.now();"
		+ " const post = () => fetch('/console/ops/jobs/restock', {method: 'POST', credentials: 'same-origin',"
		+ "   headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},"
		+ "   body: JSON.stringify({perSpecies: {DOG: 1}, idempotencyKey: key})});"
		+ " const a = await post(), b = await post();"
		+ " const ja = await a.json(), jb = await b.json();"
		+ " window.__jobId = ja.jobId;"
		+ " let state = 'RUNNING';"
		+ " for (let i = 0; i < 100 && state === 'RUNNING'; i++) {"
		+ "   await new Promise(r => setTimeout(r, 100));"
		+ "   state = (await (await fetch('/console/ops/jobs/' + ja.jobId, {headers: {'Accept': 'application/json'}})).json()).state;"
		+ " }"
		+ " return {a: a.status, b: b.status, same: ja.jobId === jb.jobId, state};"
		+ "})()";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("async-job", base, List.of(
			Map.of("name", "jobs", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('#jobs-body table')"),
					Map.of("evaluate", "window.__submit = " + SUBMIT_TWICE),
					Map.of("waitFor", "window.__jobId && document.querySelector('#jobs-body').textContent.includes(window.__jobId)", "timeoutMs", 15000)),
				"queries", Map.of("submit", "window.__submit"),
				"fetches", Map.of("unknown", "/console/ops/jobs/no-such-job"))));
	}

	@Test void a01_doubleSubmitIsOneJobThatFinishes() {
		var c = assertClean(report, "jobs");
		assertBean(query(c, "submit"), "a,b,same,state", "202,202,true,DONE");
	}

	@Test void a02_pollingTableShowsTheJob() {
		assertClean(report, "jobs"); // timeouts is empty, so the waitFor on the table text succeeded
	}

	@SuppressWarnings("unchecked") // fetches values are {status, body}.
	@Test void a03_unknownJobIs404() {
		var f = (Map<String,Object>) ((Map<String,Object>) assertClean(report, "jobs").get("fetches")).get("unknown");
		assertBean(f, "status", "404");
		assertContains("Unknown job 'no-such-job'", f.get("body"));
	}
}
