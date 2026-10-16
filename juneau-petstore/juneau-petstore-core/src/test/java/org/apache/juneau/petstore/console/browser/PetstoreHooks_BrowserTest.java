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
import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * The Orders row actions end to end in the real browser: Approve shows only on a placed order and redraws the row
 * into an approved one, Cancel opens its server-built form dialog and sends the reason with its {@code DELETE}, and
 * Delete goes through the petstore {@code typed} confirm renderer before it deletes.  The renderer's own rules are in
 * {@link PetstoreTypedConfirm_BrowserTest}.  The table's View Settings dialog offers only the tabs the page's
 * {@code columnConfig.tabs} names (a per-view tab restriction), not the ribbon's tabs.
 */
@Covers(value={"confirm-dialogs", "per-view-tab-restriction"}, pages="/console/orders")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreHooks_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	// Records every non-GET fetch the page makes, with its status and body.
	private static final String RECORD = "(() => { window.__writes = []; const f = window.fetch;"
		+ " window.fetch = async (u, o) => { const r = await f(u, o); const m = (o && o.method) || 'GET';"
		+ " if (m !== 'GET') window.__writes.push({url: String(u), method: m, status: r.status, sent: o.body || null, body: await r.clone().text()});"
		+ " return r; }; })()";
	private static final String DLG = "[role=dialog]";
	private static final String TYPED = "dialog.petstore-typed-confirm";
	private static final String MENU_ITEMS = "[...document.querySelectorAll('.juneau-view-action-item')].map(b => b.dataset.actionId)";

	private static String base;
	private static long approved, cancelled, deleted;
	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 8000);
	}

	private static String row(long id) {
		return "#orders-body tr[data-juneau-row-id='" + id + "']";
	}

	private static String wrote(String method, long id) {
		return "(window.__writes || []).some(w => w.method === '" + method + "' && w.url.includes('/console/orders/" + id + "'))";
	}

	/** Opens a row's action menu and records which actions it offers. */
	private static List<Map<String,Object>> openMenu(long id) {
		return List.of(
			wait("document.querySelector(\"" + row(id) + " .juneau-view-action-trigger\")"),
			Map.of("evaluate", RECORD),
			Map.of("click", row(id) + " .juneau-view-action-trigger"),
			wait("document.querySelector('.juneau-view-action-item')"),
			Map.of("evaluate", "window.__menu = " + MENU_ITEMS));
	}

	@SafeVarargs
	private static List<Map<String,Object>> concat(List<Map<String,Object>> head, Map<String,Object>...tail) {
		var l = new ArrayList<>(head);
		l.addAll(List.of(tail));
		return l;
	}

	@BeforeAll
	static void probe() throws Exception {
		base = PetstoreTestServer.awaitReady(server);
		// One order to approve (the only placed one), and two others to cancel and to delete.
		var ids = new ArrayList<Long>();
		for (var o : new JsonList(get("/petstore/orders").body())) {
			var m = (JsonMap)o;
			if ("PLACED".equals(m.getString("status")) && approved == 0)
				approved = m.getLong("id");
			else
				ids.add(m.getLong("id"));
		}
		Collections.sort(ids);
		cancelled = ids.get(0);
		deleted = ids.get(1);
		report = run("hooks", base, List.of(
			Map.of("name", "approve", "path", "/console/orders",
				"actions", concat(openMenu(approved),
					Map.of("click", ".juneau-view-action-item[data-action-id=approve]"),
					wait(wrote("POST", approved)),
					Map.of("evaluate", "document.body.click()"),
					wait("!document.querySelector('.juneau-view-action-item')"),
					wait("document.querySelector(\"" + row(approved) + "\")?.textContent.toUpperCase().includes('APPROVED')"),
					Map.of("click", row(approved) + " .juneau-view-action-trigger"),
					wait("document.querySelector('.juneau-view-action-item')")),
				"queries", Map.of("before", "window.__menu", "after", MENU_ITEMS, "writes", "window.__writes")),
			Map.of("name", "cancel", "path", "/console/orders",
				"actions", concat(openMenu(cancelled),
					Map.of("click", ".juneau-view-action-item[data-action-id=cancel]"),
					wait("document.querySelector('" + DLG + " [data-juneau-form-field=reason]')"),
					Map.of("evaluate", "window.__dialog = {title: document.querySelector('" + DLG + " .juneau-view-dialog-title').textContent,"
						+ " fields: [...document.querySelectorAll('" + DLG + " [data-juneau-form-field]')].map(f => f.getAttribute('data-juneau-form-field'))}"),
					Map.of("fill", List.of(DLG + " [data-juneau-form-field=reason]", "Customer changed their mind")),
					Map.of("click", DLG + " .juneau-view-dialog-confirm"),
					wait(wrote("DELETE", cancelled)),
					wait("!document.querySelector(\"" + row(cancelled) + "\")")),
				"queries", Map.of("dialog", "window.__dialog", "writes", "window.__writes")),
			Map.of("name", "delete", "path", "/console/orders",
				"actions", concat(openMenu(deleted),
					Map.of("click", ".juneau-view-action-item[data-action-id=delete]"),
					wait("document.querySelector('" + TYPED + "')"),
					Map.of("evaluate", "window.__label = document.querySelector('" + TYPED + " input').getAttribute('aria-label')"),
					Map.of("fill", List.of(TYPED + " input", String.valueOf(deleted))),
					Map.of("click", TYPED + " .petstore-typed-confirm-ok"),
					wait(wrote("DELETE", deleted)),
					wait("!document.querySelector(\"" + row(deleted) + "\")")),
				"queries", Map.of("label", "window.__label", "writes", "window.__writes")),
			Map.of("name", "settings", "path", "/console/orders",
				"actions", List.of(
					wait("document.querySelector('#orders-body .juneau-config-chooser-btn')"),
					Map.of("click", "#orders-body .juneau-config-chooser-btn"),
					wait("document.querySelector('.juneau-config-dialog [role=tab]')")),
				"queries", Map.of(
					"tabs", "[...document.querySelectorAll('.juneau-config-dialog [role=tab]')].map(t => t.textContent.trim())",
					"names", "[...document.querySelectorAll('.juneau-config-dialog [role=tab]')].map(t => t.dataset.tab)"))));
	}

	private static HttpResponse<String> get(String path) throws Exception {
		return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString());
	}

	@SuppressWarnings("unchecked") // writes is a JSON array of {url, method, status, sent, body}.
	private static Map<String,Object> theWrite(Map<String,Object> c, String method) {
		var w = ((List<Map<String,Object>>) query(c, "writes")).stream().filter(x -> method.equals(x.get("method"))).toList();
		assertSize(1, w);
		assertEquals(200, ((Number) w.get(0).get("status")).intValue(), () -> String.valueOf(w.get(0)));
		return w.get(0);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - approve
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_approveShowsOnlyOnAPlacedOrder() {
		var c = assertClean(report, "approve");
		assertList(query(c, "before"), "approve", "cancel", "delete");
	}

	@Test void a02_approveRedrawsTheRow() throws Exception {
		var c = assertClean(report, "approve");
		theWrite(c, "POST");
		assertList(query(c, "after"), "deliver", "cancel", "delete");
		assertString("APPROVED", new JsonMap(get("/petstore/orders/" + approved).body()).getString("status"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - cancel dialog
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_cancelOpensTheServerForm() {
		assertBean(query(assertClean(report, "cancel"), "dialog"), "title,fields", "Cancel order '" + cancelled + "',[reason,notify]");
	}

	@Test void b02_cancelSendsTheReason() throws Exception {
		var w = theWrite(assertClean(report, "cancel"), "DELETE");
		assertContains("Customer changed their mind", w.get("sent"));
		assertContains("Cancelled order '" + cancelled + "'", w.get("body"));
		assertEquals(404, get("/petstore/orders/" + cancelled).statusCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - typed confirm
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_deleteAsksForTheOrderId() {
		assertString("Type " + deleted + " to confirm", query(assertClean(report, "delete"), "label"));
	}

	@Test void c02_deleteDeletesOnceConfirmed() throws Exception {
		theWrite(assertClean(report, "delete"), "DELETE");
		assertEquals(404, get("/petstore/orders/" + deleted).statusCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - View Settings tab restriction
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_viewSettingsOffersOnlyTheNamedTabs() {
		var c = assertClean(report, "settings");
		assertList(query(c, "tabs"), "View", "Sort");
		assertList(query(c, "names"), "view", "sort");
	}
}
