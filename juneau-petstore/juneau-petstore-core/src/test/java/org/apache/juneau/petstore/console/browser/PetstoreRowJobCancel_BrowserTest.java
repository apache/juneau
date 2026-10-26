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
 * Cancelling a row job in the real browser: the species table's job-log detail region cancels a running restock
 * with its submit button, and a re-attached view (after a collapse and re-expand) cancels it too.  The server holds
 * a species restock for 7 s after its effect, so a cancel in either scenario lands as "after effect".
 */
@Covers(value="async-job", pages="/console/ops/jobs")
@HookCovers("jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreRowJobCancel_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture(PetstoreTestServer.CancelHoldTimings.class);

	private static Map<String,Map<String,Object>> report;

	/** JS: the species table's DOG row. */
	private static final String DOG_ROW = "[...document.querySelectorAll('#species-body tbody tr')].find(tr => [...tr.cells].some(td => td.textContent === 'DOG'))";

	/** The DOG panel's job-log region. */
	private static final String R = "#species-body .juneau-view-detail-panel .jc-joblog-region";

	private static Map<String,Object> wait(String js, int ms) {
		return Map.of("waitFor", js, "timeoutMs", ms);
	}

	private static Map<String,Object> phase(String p, int ms) {
		return wait("document.querySelector('" + R + "[data-phase=" + p + "]')", ms);
	}

	private static Map<String,Object> toggleDog() {
		return Map.of("evaluate", DOG_ROW + ".querySelector('.juneau-view-detail-toggle').click()");
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("row-job-cancel", base, List.of(
			// The running form's submit button is its cancel; the second click lands in the restock's hold.
			Map.of("name", "cancel", "path", "/console/ops/jobs",
				"actions", List.of(
					wait(DOG_ROW, 8000), toggleDog(), phase("idle", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("running", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("done", 15000)),
				"queries", Map.of("banner", "document.querySelector('" + R + " .jc-joblog-banner')?.textContent ?? null")),
			// One collapse and re-expand (the job's two stream slots); the re-attached view's button is the cancel.
			Map.of("name", "reattachCancel", "path", "/console/ops/jobs",
				"actions", List.of(
					wait(DOG_ROW, 8000), toggleDog(), phase("idle", 8000),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("running", 8000),
					toggleDog(), wait("!document.querySelector('" + R + "')", 4000),
					toggleDog(), phase("running", 8000),
					Map.of("evaluate", "window.__btn = (b => ({text: b.textContent, hidden: b.hidden, disabled: b.disabled}))(document.querySelector('" + R + " .jc-joblog-submit'))"),
					Map.of("click", R + " .jc-joblog-submit"),
					phase("done", 20000),
					Map.of("evaluate", "window.__active = JuneauViews.jobs.active(" + DOG_ROW + ") == null")),
				"queries", Map.of("btn", "window.__btn",
					"banner", "document.querySelector('" + R + " .jc-joblog-banner')?.textContent ?? null",
					"marker", DOG_ROW + ".hasAttribute('data-juneau-job')",
					"activeNull", "window.__active"))));
	}

	@Test void a01_cancelEndsTheRowJob() {
		assertString("Cancelled after the job had already made changes.", query(assertClean(report, "cancel"), "banner"));
	}

	@Test void a02_aReattachedPanelCancelsItsJob() {
		var c = assertClean(report, "reattachCancel");
		assertBean(query(c, "btn"), "text,hidden,disabled", "Cancel,false,false");
		assertString("Cancelled after the job had already made changes.", query(c, "banner"));
		assertEquals(false, query(c, "marker"));
		assertEquals(true, query(c, "activeNull"));
	}
}
