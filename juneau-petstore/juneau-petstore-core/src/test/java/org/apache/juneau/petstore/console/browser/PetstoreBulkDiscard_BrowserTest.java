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
import java.net.http.HttpResponse.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The aggregate bulk Discard on Pending changes in the real browser: rows that are not pending can't be selected and
 * say why, a confirmed discard is one POST carrying the ids and an idempotency key, its summary counts the ids that
 * were gone or no longer pending, the failed ones stay selected, and a refused request leaves the selection intact
 * behind a sticky alert.
 *
 * <p>
 * The class seeds its own changes before the browser starts: four pending, one applied, and one failed (its pet
 * was deleted before it was applied).
 */
@Covers(value="bulk-select", pages="/console/pets/changes")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreBulkDiscard_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String READY = "document.querySelector('#changes tbody tr[data-juneau-row-id]')";
	private static final String HEADER_BOX = "#changes thead input.juneau-view-select-all-checkbox";
	private static final String PENDING_ONLY = "#changes-body .juneau-view-ribbon-btn[aria-label=\"Pending only\"]";
	private static final String DLG = "dialog.petstore-typed-confirm";
	private static final String OK = DLG + " .petstore-typed-confirm-ok";

	/** The selected ids, from the table's retained selection topic. */
	private static final String IDS = "((window.JuneauViews.bus.get('selection:changes') || {}).ids || [])";

	/** Picks Discard in the bulk action chooser. */
	private static final String CHOOSE_DISCARD = "(() => { const s = document.querySelector('#changes-body .juneau-view-bulk-select');"
		+ " s.value = 'discard'; s.dispatchEvent(new Event('change')); })()";

	/** Types the selection count into the typed confirm, which is what it asks for when several rows are selected. */
	private static final String TYPE_COUNT = "(() => { const i = document.querySelector('" + DLG + " input');"
		+ " i.value = String(" + IDS + ".length); i.dispatchEvent(new Event('input')); })()";

	/**
	 * Behind the page's back, discards the first selected change and applies the second, so the bulk run finds one
	 * gone and one no longer pending.  The setup discard carries its own key, so it can be told apart.
	 */
	private static final String RACE = "(async () => { const ids = " + IDS + "; window.__gone = ids[0]; window.__applied = ids[1];"
		+ " const h = {'Content-Type': 'application/json', 'Accept': 'application/json', 'X-Csrf-Token': document.body.dataset.juneauCsrf};"
		+ " await fetch('/console/pets/changes/discard', {method: 'POST', headers: h, body: JSON.stringify({ids: [ids[0]], idempotencyKey: 'setup'})});"
		+ " await fetch('/console/pets/changes/' + ids[1] + '/apply', {method: 'POST', headers: h});"
		+ " window.__raced = true; })()";

	/** Sends every discard to the demo failure toggle, so the server refuses it with a 503. */
	private static final String FAIL_DISCARDS = "const f = window.fetch; window.fetch = (u, o) =>"
		+ " f(String(u).endsWith('/discard') ? u + '?fail=discard' : u, o)";

	private static final String TOAST = ".juneau-view-bulk-toast";

	/** What the page logs and shows when the server refuses the whole discard. */
	private static final String REFUSED = "bulk action 'discard' on table 'changes' failed: 'HTTP 503'";

	private static Map<String,Map<String,Object>> report;
	private static List<String> pending;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		seed(base);
		var confirm = List.<Map<String,Object>>of(
			Map.of("waitFor", READY),
			Map.of("click", HEADER_BOX),
			Map.of("evaluate", "window.__selected = " + IDS + ".slice()"));
		var submit = List.<Map<String,Object>>of(
			Map.of("evaluate", CHOOSE_DISCARD),
			Map.of("click", "#changes-body .juneau-view-bulk-go"),
			Map.of("waitFor", "document.querySelector('" + DLG + "')"),
			Map.of("evaluate", TYPE_COUNT),
			Map.of("click", OK));
		report = run("bulk-discard", base, List.of(
			Map.of("name", "disabled", "path", "/console/pets/changes",
				"actions", List.of(
					Map.of("waitFor", READY),
					Map.of("click", PENDING_ONLY),
					Map.of("waitFor", "document.querySelector('#changes tbody input.juneau-view-select-checkbox:disabled')")),
				"queries", Map.of(
					"reasons", "Array.from(document.querySelectorAll('#changes tbody input.juneau-view-select-checkbox:disabled')).map(b => b.title)",
					"states", "Array.from(document.querySelectorAll('#changes tbody input.juneau-view-select-checkbox:disabled'))"
						+ ".map(b => b.closest('tr').textContent.includes('APPLIED') ? 'APPLIED' : b.closest('tr').textContent.includes('FAILED') ? 'FAILED' : '?')")),
			// Runs before 'discard', which changes the store; this one is refused and changes nothing.
			Map.of("name", "discard-fail", "path", "/console/pets/changes", "allowFailedLoads", true,
				"actions", concat(List.of(Map.of("evaluate", FAIL_DISCARDS)), confirm, submit,
					List.of(Map.of("waitFor", "document.querySelector('" + TOAST + "[role=alert]')"))),
				"queries", Map.of(
					"selected", "window.__selected",
					"alert", "document.querySelector('" + TOAST + "[role=alert]').textContent",
					"left", IDS)),
			Map.of("name", "discard", "path", "/console/pets/changes", "capture", "/console/pets/changes/discard",
				"actions", concat(confirm,
					List.of(Map.of("evaluate", RACE), Map.of("waitFor", "window.__raced === true")),
					submit,
					List.of(Map.of("waitFor", "document.querySelector('" + TOAST + "')"))),
				"queries", Map.of(
					"selected", "window.__selected",
					"gone", "window.__gone",
					"applied", "window.__applied",
					"toast", "document.querySelector('" + TOAST + "').textContent",
					"role", "document.querySelector('" + TOAST + "').getAttribute('role')",
					"left", IDS))));
	}

	@SafeVarargs
	private static List<Map<String,Object>> concat(List<Map<String,Object>>...parts) {
		var l = new ArrayList<Map<String,Object>>();
		for (var p : parts)
			l.addAll(p);
		return l;
	}

	/** Stages the changes the cases work on, through the same console endpoints the pages use. */
	private static void seed(String base) throws Exception {
		var http = HttpClient.newHttpClient();
		pending = new ArrayList<>();
		for (var i = 1; i <= 4; i++)
			pending.add(String.valueOf(stage(http, base, newPet(http, base, "Discardable " + i), "Renamed " + i)));
		var applied = stage(http, base, newPet(http, base, "Applied"), "Applied twice");
		assertEquals("APPLIED", post(http, base, "/console/pets/changes/" + applied + "/apply", "{}").getMap("row").getString("state"));
		var doomed = newPet(http, base, "Doomed");
		var failed = stage(http, base, doomed, "Never written");
		var del = http.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets/" + doomed)).DELETE().build(), BodyHandlers.ofString());
		assertEquals(200, del.statusCode(), del::body);
		assertEquals("FAILED", post(http, base, "/console/pets/changes/" + failed + "/apply", "{}").getMap("row").getString("state"));
	}

	private static long newPet(HttpClient http, String base, String name) throws Exception {
		return post(http, base, "/console/pets", "{\"name\":\"" + name + "\",\"price\":3}").getMap("row").getLong("id");
	}

	private static long stage(HttpClient http, String base, long petId, String name) throws Exception {
		return post(http, base, "/console/pets/" + petId + "/stage", "{\"field\":\"name\",\"value\":\"" + name + "\"}").getMap("row").getLong("id");
	}

	private static JsonMap post(HttpClient http, String base, String path, String json) throws Exception {
		var res = http.send(PetstoreTestServer.consolePost(base, path, json), BodyHandlers.ofString());
		assertEquals(200, res.statusCode(), res::body);
		return new JsonMap(res.body());
	}

	@SuppressWarnings("unchecked") // ids are a JSON array of strings.
	private static List<String> ids(Map<String,Object> c, String key) {
		return ((List<Object>) query(c, key)).stream().map(String::valueOf).toList();
	}

	@Test void a01_rowsThatAreNotPendingCannotBeSelectedAndSayWhy() {
		var c = assertClean(report, "disabled");
		assertList(new TreeSet<>(ids(c, "states")), "APPLIED", "FAILED");
		for (var r : ids(c, "reasons"))
			assertString("Only pending changes can be discarded or applied.", r);
	}

	@Test void a02_selectAllTakesOnlyThePendingRows() {
		var c = assertClean(report, "discard");
		assertEquals(new TreeSet<>(pending), new TreeSet<>(ids(c, "selected")));
	}

	@SuppressWarnings("unchecked") // requests is a JSON array of {url, method, postData}.
	@Test void b01_discardIsOnePostWithTheIdsAndAKey() throws Exception {
		var c = assertClean(report, "discard");
		var runs = ((List<Map<String,Object>>) c.get("requests")).stream()
			.filter(r -> "POST".equals(r.get("method")) && ! String.valueOf(r.get("postData")).contains("\"setup\"")).toList();
		assertSize(1, runs);
		var body = Json.to((String) runs.get(0).get("postData"), Map.class);
		assertEquals(new TreeSet<>(pending), new TreeSet<>((List<String>) body.get("ids")));
		assertNotEmpty(body.get("idempotencyKey"));
	}

	@Test void b02_summaryCountsTheGoneAndTheNoLongerPending() {
		var c = assertClean(report, "discard");
		assertString("Discard: 2 succeeded, 1 not found, 1 failed (ids: " + query(c, "applied") + ")Close", query(c, "toast"));
		assertString("alert", query(c, "role"));
	}

	@Test void b03_onlyTheFailedIdStaysSelected() {
		var c = assertClean(report, "discard");
		assertList(ids(c, "left"), String.valueOf(query(c, "applied")));
	}

	@Test void b04_aRefusedDiscardKeepsTheSelectionBehindAStickyAlert() {
		var c = report.get("discard-fail");
		assertNotNull(c, () -> "No report for case 'discard-fail'; cases: " + report.keySet());
		// The whole-run failure is logged to the console on purpose; nothing else may be.
		assertList(c.get("jsFailures"), REFUSED);
		assertEquals(List.of(), c.get("timeouts"), () -> "timed out waiting for: " + c.get("timeouts"));
		assertString(REFUSED + "Close", query(c, "alert"));
		assertEquals(new TreeSet<>(pending), new TreeSet<>(ids(c, "selected")));
		assertEquals(new TreeSet<>(pending), new TreeSet<>(ids(c, "left")));
	}
}
