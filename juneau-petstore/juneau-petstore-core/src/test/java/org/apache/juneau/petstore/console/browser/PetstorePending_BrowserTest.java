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

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The header pending-changes badge in the real browser: hidden while nothing is staged, counting a change the pencil
 * stages (without touching the pet), linking to Pending changes, and hidden again once that change is discarded.
 */
@Covers(value="pending-changes-badge", pages={"/console/store", "/console/pets/1/summary", "/console/pets/changes"})
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstorePending_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String BADGE = "[data-juneau-badge-id=pending]";
	private static final String PENCIL = "#pet-body [data-juneau-edit=\"rename\"]";
	private static final String INPUT = "#pet-body .juneau-edit-input";
	private static final String TYPED = "dialog.petstore-typed-confirm";
	private static final String STATE = "(() => { const b = document.querySelector('" + BADGE + "'); return b ? {hidden: b.hidden, text: b.textContent, total: b.getAttribute('data-jc-total')} : null; })()";
	private static final String PENDING = "fetch('/console/pets/changes/pending', {headers: {Accept: 'application/json'}}).then(r => r.json()).then(b => b.total)";

	// Flags the page once its discard POST has come back.
	private static final String DISCARDED = "(() => { const f = window.fetch; window.fetch = async (u, o) => { const r = await f(u, o);"
		+ " if (o && o.method === 'POST' && String(u).includes('/discard')) window.__discarded = r.status; return r; }; })()";

	// Records the total of every badge poll that has come back, so a case can tell a polled "nothing staged" (the badge
	// rendered hidden) from a page whose poll has not answered yet.  Runs at the start of every document.
	private static final String POLLS = "(() => { window.__polls = []; const f = window.fetch; window.fetch = async (u, o) => {"
		+ " const r = await f(u, o);"
		+ " if (String(u).includes('/console/pets/changes/pending') && !(o && o.method && o.method !== 'GET'))"
		+ "   r.clone().json().then(b => window.__polls.push(b.total), () => window.__polls.push('error'));"
		+ " return r; }; })()";

	// A badge poll has answered and the badge has rendered that answer.
	private static final String POLLED = "window.__polls && window.__polls.length > 0 && document.querySelector('" + BADGE + "')";

	// The total the most recent badge poll answered (the badge renders the latest one; a page may poll more than once).
	private static final String LAST_POLL = "window.__polls[window.__polls.length - 1]";

	private static String base, before;
	private static long pet;
	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 8000);
	}

	@BeforeAll
	static void probe() throws Exception {
		base = PetstoreTestServer.awaitReady(server);
		for (var o : new JsonList(get("/petstore/pets").body()))
			if ("AVAILABLE".equals(((JsonMap)o).getString("status"))) {
				pet = ((JsonMap)o).getLong("id");
				break;
			}
		before = petName();
		var row = "[...document.querySelectorAll('#changes-body tbody tr')].find(tr => tr.textContent.includes('Badged " + pet + "'))";
		report = run("pending", base, List.of(
			Map.of("name", "empty", "path", "/console/store", "initScript", POLLS,
				"actions", List.of(wait(POLLED)),
				"queries", Map.of("badge", STATE, "lastPoll", LAST_POLL)),
			Map.of("name", "staged", "path", "/console/pets/" + pet + "/summary",
				"actions", List.of(
					wait("document.querySelector('" + PENCIL + "')"),
					Map.of("click", PENCIL),
					wait("document.querySelector('" + INPUT + "')"),
					Map.of("fill", List.of(INPUT, "Badged " + pet)),
					Map.of("press", List.of(INPUT, "Enter")),
					wait("document.querySelector('.juneau-view-bulk-toast')"),
					Map.of("goto", "/console/store"),
					wait("document.querySelector('" + BADGE + ":not([hidden])')")),
				"queries", Map.of("badge", STATE)),
			Map.of("name", "follow", "path", "/console/store",
				"actions", List.of(
					wait("document.querySelector('" + BADGE + ":not([hidden])')"),
					Map.of("click", BADGE),
					wait("location.pathname === '/console/pets/changes'")),
				"queries", Map.of("path", "location.pathname")),
			Map.of("name", "cleared", "path", "/console/pets/changes", "initScript", POLLS,
				"actions", List.of(
					wait(row),
					Map.of("evaluate", DISCARDED),
					Map.of("evaluate", row + ".querySelector('.juneau-view-select-checkbox').click()"),
					wait("document.querySelector('.juneau-view-bulk-toolbar:not([hidden]) .juneau-view-bulk-select')"),
					Map.of("evaluate", "(() => { const s = document.querySelector('.juneau-view-bulk-select'); s.value = 'discard'; s.dispatchEvent(new Event('change', {bubbles: true})); })()"),
					Map.of("click", ".juneau-view-bulk-go"),
					wait("document.querySelector('" + TYPED + "')"),
					Map.of("evaluate", "(() => { const i = document.querySelector('" + TYPED + " input'); i.value = /Type (.+) to confirm/.exec(i.getAttribute('aria-label'))[1]; i.dispatchEvent(new Event('input')); })()"),
					Map.of("click", TYPED + " .petstore-typed-confirm-ok"),
					wait("window.__discarded"),
					Map.of("goto", "/console/store"),
					wait(POLLED)),
				"queries", Map.of("badge", STATE, "lastPoll", LAST_POLL, "pending", PENDING))));
	}

	private static HttpResponse<String> get(String path) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString());
	}

	private static String petName() throws Exception {
		return new JsonMap(get("/petstore/pets/" + pet).body()).getString("name");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - badge
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_hiddenWhileNothingIsStaged() {
		var c = assertClean(report, "empty");
		assertString("0", String.valueOf(query(c, "lastPoll")));   // a poll answered "nothing staged"...
		assertBean(query(c, "badge"), "hidden", "true");   // ...and the badge rendered that, not a pre-poll default
	}

	@Test void a02_countsTheStagedChange() {
		assertBean(query(assertClean(report, "staged"), "badge"), "hidden,text,total", "false,1 change pending (1 yours),1");
	}

	@Test void a03_stagingLeavesThePetAlone() throws Exception {
		assertClean(report, "staged");
		assertString(before, petName());
	}

	@Test void a04_linksToPendingChanges() {
		assertString("/console/pets/changes", query(assertClean(report, "follow"), "path"));
	}

	@Test void a05_hiddenAgainOnceDiscarded() {
		var c = assertClean(report, "cleared");
		assertString("0", String.valueOf(query(c, "pending")));
		assertString("0", String.valueOf(query(c, "lastPoll")));   // the store page's own poll saw the discard
		assertBean(query(c, "badge"), "hidden", "true");
	}
}
