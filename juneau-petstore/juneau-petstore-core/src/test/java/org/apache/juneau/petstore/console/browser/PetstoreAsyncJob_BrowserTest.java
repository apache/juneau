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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * P6 in the real browser: a double submit with one key returns one job, the job finishes, the polled jobs table
 * shows it, and an unknown id is 404.  The {@code restock} job-log card streams a restock to its result, re-attaches
 * after a reload, tells a cancel before the effect from one after it, shows the hard timeout and the job limit, and
 * the stream refuses a subscriber over its cap.
 */
@Covers(value="async-job", pages="/console/ops/jobs")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreAsyncJob_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String SUBMIT_TWICE = "(async () => {"
		+ " const key = 'browser-' + Date.now();"
		+ " const post = () => fetch('/console/ops/jobs/restock', {method: 'POST', credentials: 'same-origin',"
		+ "   headers: {'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Csrf-Token': document.body.dataset.juneauCsrf},"
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

	// The job-log card's root, whichever element carries the card id.
	private static final String J = ":is(#restock, #restock-body) .jc-joblog";
	private static final String CSRF = "{'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Csrf-Token': document.body.dataset.juneauCsrf}";

	/** JS: starts a restock by fetch and resolves to its job ref. */
	private static String start(int slowMs) {
		return "fetch('/console/ops/jobs/restock', {method: 'POST', credentials: 'same-origin', headers: " + CSRF + ","
			+ " body: JSON.stringify({DOG: 1, slowMs: " + slowMs + ", idempotencyKey: 'browser-' + Math.random()})}).then(r => r.json())";
	}

	/** JS: cancels the job refs in {@code window.__refs}, with the same request the job-log card sends. */
	private static final String CANCEL_REFS = "Promise.all((window.__refs || []).map(r => fetch(r.cancelUrl, {method: 'POST', credentials: 'same-origin', headers: " + CSRF + ", body: '{}'})))";

	/** JS: resolves to the number of pets in the store. */
	private static final String PET_COUNT = "fetch('/petstore/pets', {headers: {Accept: 'application/json'}}).then(r => r.json()).then(p => p.length)";

	// Holds the stream's two subscriber slots open on a slow job, then asks for a third.
	private static final String OVER_THE_CAP = "(async () => {"
		+ " const ref = await " + start(4000) + ";"
		+ " window.__refs = [ref];"
		+ " const open = () => new Promise((res, rej) => { const es = new EventSource(ref.streamUrl);"
		+ "   es.onopen = () => res(es); setTimeout(() => { es.close(); rej(new Error('stream did not open')); }, 8000); });"
		+ " const a = await open(), b = await open();"
		+ " const ctl = new AbortController();"
		+ " const third = await fetch(ref.streamUrl, {headers: {Accept: 'text/event-stream'}, signal: ctl.signal});"
		+ " const out = {open: [a.readyState, b.readyState], third: third.status};"
		+ " ctl.abort(); a.close(); b.close();"
		+ " await " + CANCEL_REFS + ";"
		+ " return out; })()";

	private static Map<String,Object> wait(String js, int ms) {
		return Map.of("waitFor", js, "timeoutMs", ms);
	}

	private static Map<String,Object> phase(String p, int ms) {
		return wait("document.querySelector('" + J + "[data-phase=" + p + "]')", ms);
	}

	private static Map<String,Object> slowMs(int ms) {
		return Map.of("fill", List.of(J + " .jc-joblog-input[name=slowMs]", String.valueOf(ms)));
	}

	private static Map<String,Object> holdMs(int ms) {
		return Map.of("fill", List.of(J + " .jc-joblog-input[name=holdMs]", String.valueOf(ms)));
	}

	private static final Map<String,Object> SUBMIT = Map.of("click", J + " .jc-joblog-submit");

	/** JS: some output line contains {@code text}. */
	private static Map<String,Object> line(String text, int ms) {
		return wait("[...document.querySelectorAll('" + J + " .jc-joblog-line')].some(l => l.textContent.includes('" + text + "'))", ms);
	}

	private static final Map<String,Object> CARD_QUERIES = Map.of(
		"lines", "[...document.querySelectorAll('" + J + " .jc-joblog-line')].map(l => l.textContent)",
		"banner", "document.querySelector('" + J + " .jc-joblog-banner')?.textContent ?? null",
		"search", "location.search");

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("async-job", base, List.of(
			Map.of("name", "jobs", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('#jobs-body table')"),
					Map.of("evaluate", "window.__submit = " + SUBMIT_TWICE),
					Map.of("waitFor", "window.__jobId && document.querySelector('#jobs-body').textContent.includes(window.__jobId)", "timeoutMs", 15000)),
				"queries", Map.of("submit", "window.__submit", "jobId", "window.__jobId",
					"row", "[...document.querySelectorAll('#jobs-body tbody tr')].find(tr => tr.textContent.includes(window.__jobId))?.textContent ?? null"),
				"fetches", Map.of("unknown", "/console/ops/jobs/no-such-job")),
			// The unknown stream's 404 is expected.
			Map.of("name", "card-submit", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(phase("idle", 8000), SUBMIT, phase("done", 15000)),
				"queries", CARD_QUERIES,
				"fetches", Map.of("unknownStream", "/console/ops/jobs/juneau-jobs/no-such-job/stream")),
			Map.of("name", "reattach", "path", "/console/ops/jobs",
				"actions", List.of(phase("idle", 8000), slowMs(1500), SUBMIT,
					wait("location.search.includes('job=')", 8000), line("Waiting", 8000),
					Map.of("reload", true),
					phase("done", 15000)),
				"queries", CARD_QUERIES),
			Map.of("name", "cancel-early", "path", "/console/ops/jobs",
				"actions", List.of(phase("idle", 8000),
					Map.of("evaluate", PET_COUNT + ".then(n => { window.__before = n; })"), wait("window.__before != null", 8000),
					slowMs(3000), Map.of("evaluate", "window.__posted = Date.now()"), SUBMIT, line("Waiting", 8000),
					wait("!document.querySelector('" + J + " .jc-joblog-submit').disabled", 8000), SUBMIT,
					phase("done", 10000),
					// A worker that ignored the cancel would restock once its wait ran out, so count only after that.
					wait("Date.now() - window.__posted > 3500", 8000),
					Map.of("evaluate", PET_COUNT + ".then(n => { window.__after = n; })"), wait("window.__after != null", 8000)),
				"queries", Map.of("lines", CARD_QUERIES.get("lines"), "banner", CARD_QUERIES.get("banner"),
					"pets", "[window.__before, window.__after]")),
			Map.of("name", "cancel-late", "path", "/console/ops/jobs",
				// The 6 s hold keeps the job cancellable well past the Added line and ends 6 s short of the 12 s hard
				// timeout, so the cancel always lands in the hold.
				"actions", List.of(phase("idle", 8000), holdMs(6000), SUBMIT, line("Added", 8000), SUBMIT, phase("done", 10000)),
				"queries", CARD_QUERIES),
			// The third subscriber's 429 is expected.
			Map.of("name", "subscriber-cap", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(phase("idle", 8000), Map.of("evaluate", "window.__cap = " + OVER_THE_CAP)),
				"queries", Map.of("cap", "window.__cap")),
			// The job would run 20 s; the 12 s timeout lands in its hold.  This passes even against a scheduler that fires
			// its timeout task a little late; the early- and on-the-deadline firing it once hung on is pinned
			// deterministically by AsyncJobRegistry_Test's k group.
			Map.of("name", "timeout", "path", "/console/ops/jobs",
				"actions", List.of(phase("idle", 8000), slowMs(10000), holdMs(10000), SUBMIT, phase("done", 25000)),
				"queries", CARD_QUERIES),
			// The two fetched jobs fill the limit, so the card's own submit is refused with 429.
			Map.of("name", "limit", "path", "/console/ops/jobs", "allowFailedLoads", true,
				"actions", List.of(phase("idle", 8000),
					Map.of("evaluate", "window.__started = Promise.all([" + start(4000) + ", " + start(4000) + "]).then(r => { window.__refs = r; })"),
					wait("window.__refs && window.__refs.length === 2", 8000),
					SUBMIT,
					wait("document.querySelector('" + J + " .jc-joblog-banner.jc-joblog-error:not([hidden])')", 8000),
					Map.of("evaluate", CANCEL_REFS)),
				"queries", CARD_QUERIES)));
	}

	@Test void a01_doubleSubmitIsOneJobThatFinishes() {
		var c = assertClean(report, "jobs");
		assertBean(query(c, "submit"), "a,b,same,state", "202,202,true,DONE");
	}

	@Test void a02_pollingTableShowsTheJob() {
		var c = assertClean(report, "jobs");
		var id = String.valueOf(query(c, "jobId"));
		assertNotNull(query(c, "row"), () -> "no Jobs table row for job '" + id + "'");
		assertContains(id, query(c, "row"));
	}

	@SuppressWarnings("unchecked") // fetches values are {status, body}.
	@Test void a03_unknownJobIs404() {
		var f = (Map<String,Object>) ((Map<String,Object>) assertClean(report, "jobs").get("fetches")).get("unknown");
		assertBean(f, "status", "404");
		assertContains("Unknown job 'no-such-job'", f.get("body"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the restock job-log card
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked") // lines is a JSON array of strings.
	private static List<String> lines(Map<String,Object> c) {
		return (List<String>) query(c, "lines");
	}

	@Test void b01_cardSubmitStreamsProgressToDone() {
		var c = assertClean(report, "card-submit");
		assertContains("Done. Restocked 1 pets", query(c, "banner"));
		assertContains("Restocking 1 pets", lines(c));
		assertTrue(lines(c).stream().anyMatch(x -> x.startsWith("Added DOG #")), () -> "no Added line: " + lines(c));
		assertContains("job=", query(c, "search"));
	}

	@SuppressWarnings("unchecked") // fetches values are {status, body}.
	@Test void b02_unknownStreamIs404() {
		var f = (Map<String,Object>) ((Map<String,Object>) assertClean(report, "card-submit").get("fetches")).get("unknownStream");
		assertBean(f, "status", "404");
	}

	@Test void b03_reloadReattachesAndReplaysTheLog() {
		var c = assertClean(report, "reattach");
		assertContains("job=", query(c, "search"));
		assertTrue(lines(c).stream().anyMatch(x -> x.startsWith("Waiting 1500 ms")), () -> "no replayed history: " + lines(c));
		assertContains("Done. Restocked 1 pets", query(c, "banner"));
	}

	@Test void b04_cancelBeforeTheEffectChangesNothing() {
		var c = assertClean(report, "cancel-early");
		assertString("Cancelled.", query(c, "banner"));
		assertTrue(lines(c).stream().noneMatch(x -> x.startsWith("Added")), () -> "restocked anyway: " + lines(c));
		var pets = (List<?>) query(c, "pets");
		assertNotNull(pets.get(0));
		assertEquals(pets.get(0), pets.get(1), "the pet count changed after a cancel before the effect");
	}

	@Test void b05_cancelAfterTheEffectSaysSo() {
		assertString("Cancelled after the job had already made changes.", query(assertClean(report, "cancel-late"), "banner"));
	}

	@Test void b06_streamRefusesASubscriberOverTheCap() {
		assertBean(query(assertClean(report, "subscriber-cap"), "cap"), "open,third", "[1,1],429");
	}

	@Test void b07_hardTimeoutEndsAsUnknown() {
		assertString("The job ended with an unknown outcome. app:hard-timeout-exceeded", query(assertClean(report, "timeout"), "banner"));
	}

	@Test void b08_jobLimitIsShownOnTheCard() {
		assertString("Job limit reached; '2' running", query(assertClean(report, "limit"), "banner"));
	}
}
