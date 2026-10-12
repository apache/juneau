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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The petstore {@code typed} confirm renderer in the real browser: what the user must type (the row count, one
 * row's id, or a ribbon-hosted action's own id), the fail-closed refusal when there is nothing to type, Cancel and
 * Escape resolving false, and the Orders row Delete requiring its exact order id.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreTypedConfirm_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String DLG = "dialog.petstore-typed-confirm";
	private static final String INPUT = DLG + " input";
	private static final String OK = DLG + " button:nth-of-type(1)";
	private static final String CANCEL = DLG + " button:nth-of-type(2)";

	// Records the typed dialog's label and Confirm state under a key, so a case can observe several steps.
	private static final String SNAP = "window.__snap = k => {"
		+ " const d = document.querySelector('" + DLG + "');"
		+ " (window.__log = window.__log || {})[k] = d ? {label: d.querySelector('input').getAttribute('aria-label'), title: d.querySelector('h2').textContent,"
		+ "   disabled: d.querySelectorAll('button')[0].disabled} : null; }";

	private static String open(String request) {
		return "(() => { window.__res = 'pending'; window.__warns = []; console.warn = (...a) => window.__warns.push(a.join(' '));"
			+ " JuneauViews.dialogs.confirm(Object.assign({renderer: 'typed', title: 'Sure?', confirmLabel: 'Go'}, " + request + "))"
			+ ".then(v => { window.__res = v; }, () => { window.__res = 'rejected'; }); })()";
	}

	private static Map<String,Object> theCase(String name, String request, List<Map<String,Object>> steps) {
		var actions = new ArrayList<Map<String,Object>>();
		actions.add(Map.of("waitFor", "window.JuneauViews && JuneauViews.dialogs"));
		actions.add(Map.of("evaluate", SNAP));
		actions.add(Map.of("evaluate", open(request)));
		actions.addAll(steps);
		return Map.of("name", name, "path", "/console/orders", "actions", actions,
			"queries", Map.of("log", "window.__log", "res", "window.__res", "warns", "window.__warns", "open", "!!document.querySelector('" + DLG + "')"));
	}

	private static Map<String,Object> snap(String k) {
		return Map.of("evaluate", "window.__snap('" + k + "')");
	}

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 5000);
	}

	private static final String RES_TRUE = "window.__res === true";
	private static final String RES_FALSE = "window.__res === false";

	private static Map<String,Map<String,Object>> report;
	private static Map<String,Object> orders;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("typed-confirm", base, List.of(
			theCase("count", "{rows: [{id: 1}, {id: 2}, {id: 3}]}", List.of(
				wait("document.querySelector('" + DLG + "')"), snap("start"),
				Map.of("fill", List.of(INPUT, "2")), snap("wrong"),
				Map.of("fill", List.of(INPUT, "3")), snap("right"),
				Map.of("click", OK), wait(RES_TRUE))),
			theCase("oneRow", "{rows: [{id: 42}]}", List.of(
				wait("document.querySelector('" + DLG + "')"), snap("start"),
				Map.of("fill", List.of(INPUT, "4")), snap("wrong"),
				Map.of("fill", List.of(INPUT, "42")), snap("right"),
				Map.of("click", OK), wait(RES_TRUE))),
			theCase("noRow", "{rows: [], action: {id: 'reset'}}", List.of(
				wait("document.querySelector('" + DLG + "')"), snap("start"),
				Map.of("fill", List.of(INPUT, "rese")), snap("wrong"),
				Map.of("fill", List.of(INPUT, "reset")), snap("right"),
				Map.of("click", OK), wait(RES_TRUE))),
			theCase("empty", "{rows: []}", List.of(
				wait(RES_FALSE))),
			theCase("nullId", "{rows: [{id: null}]}", List.of(wait(RES_FALSE))),
			theCase("missingId", "{rows: [{name: 'no id here'}]}", List.of(wait(RES_FALSE))),
			theCase("blankId", "{rows: [{id: '   '}]}", List.of(wait(RES_FALSE))),
			theCase("cancel", "{rows: [{id: 7}]}", List.of(
				wait("document.querySelector('" + DLG + "')"),
				Map.of("fill", List.of(INPUT, "7")),
				Map.of("click", CANCEL), wait(RES_FALSE))),
			theCase("escape", "{rows: [{id: 7}]}", List.of(
				wait("document.querySelector('" + DLG + "')"),
				Map.of("press", List.of(INPUT, "Escape")), wait(RES_FALSE))),
			Map.of("name", "ordersDelete", "path", "/console/orders", "capture", "/console/orders/",
				"actions", List.of(
					Map.of("evaluate", SNAP),
					wait("document.querySelector('#orders-body .juneau-view-action-trigger')"),
					Map.of("click", "#orders-body .juneau-view-action-trigger"),
					Map.of("click", ".juneau-view-action-item[data-action-id=delete]"),
					wait("document.querySelector('" + DLG + "')"), snap("start"),
					Map.of("fill", List.of(INPUT, "not-the-id")), snap("wrong"),
					// Type the id the dialog names in its own prompt, one character short, then exactly.
					Map.of("evaluate", "(() => { const m = /'([^']+)'/.exec(document.querySelector('" + DLG + " h2').textContent); window.__id = m && m[1]; })()"),
					Map.of("evaluate", "(() => { const i = document.querySelector('" + DLG + " input'); i.value = window.__id.slice(0, -1); i.dispatchEvent(new Event('input')); })()"), snap("short"),
					Map.of("evaluate", "(() => { const i = document.querySelector('" + DLG + " input'); i.value = window.__id; i.dispatchEvent(new Event('input')); })()"), snap("right"),
					Map.of("click", CANCEL), wait("!document.querySelector('" + DLG + "')")),
				"queries", Map.of("log", "window.__log", "id", "window.__id"))));
	}

	@SuppressWarnings("unchecked") // log is a JSON object of {label, disabled} snapshots.
	private static Map<String,Object> log(String name, String key) {
		var log = (Map<String,Object>) query(assertClean(report, name), "log");
		return (Map<String,Object>) log.get(key);
	}

	@Test void a01_severalRowsTypeTheCount() {
		assertBean(log("count", "start"), "label,disabled", "Type 3 to confirm,true");
		assertBean(log("count", "wrong"), "disabled", "true");
		assertBean(log("count", "right"), "disabled", "false");
		assertEquals(true, query(assertClean(report, "count"), "res"));
	}

	@Test void a02_oneRowTypesItsId() {
		assertBean(log("oneRow", "start"), "label,disabled", "Type 42 to confirm,true");
		assertBean(log("oneRow", "wrong"), "disabled", "true");
		assertBean(log("oneRow", "right"), "disabled", "false");
		assertEquals(true, query(assertClean(report, "oneRow"), "res"));
	}

	@Test void a03_noRowTypesTheActionId() {
		assertBean(log("noRow", "start"), "label,disabled", "Type reset to confirm,true");
		assertBean(log("noRow", "wrong"), "disabled", "true");
		assertBean(log("noRow", "right"), "disabled", "false");
		assertEquals(true, query(assertClean(report, "noRow"), "res"));
	}

	@Test void a04_nothingToTypeFailsClosed() {
		var c = assertClean(report, "empty");
		assertEquals(false, query(c, "res"));
		assertEquals(false, query(c, "open"));
		assertContains("typed confirm", String.valueOf(query(c, "warns")));
	}

	@Test void a04b_aRowWithoutAUsableIdFailsClosed() {
		for (var name : List.of("nullId", "missingId", "blankId")) {
			var c = assertClean(report, name);
			assertEquals(false, query(c, "res"), name);
			assertEquals(false, query(c, "open"), name);
			assertContains("typed confirm", String.valueOf(query(c, "warns")));
		}
	}

	@Test void a05_cancelResolvesFalse() {
		assertEquals(false, query(assertClean(report, "cancel"), "res"));
	}

	@Test void a06_escapeResolvesFalse() {
		assertEquals(false, query(assertClean(report, "escape"), "res"));
	}

	@SuppressWarnings("unchecked") // requests is a JSON array of {url, method, postData}.
	@Test void a07_ordersDeleteNeedsTheExactId() {
		var c = assertClean(report, "ordersDelete");
		var id = (String) query(c, "id");
		assertNotEmpty(id);
		assertBean(log("ordersDelete", "start"), "label,disabled", "Type " + id + " to confirm,true");
		assertBean(log("ordersDelete", "wrong"), "disabled", "true");
		assertBean(log("ordersDelete", "short"), "disabled", "true");
		assertBean(log("ordersDelete", "right"), "disabled", "false");
		for (var r : (List<Map<String,Object>>) c.get("requests"))
			assertNotEquals("DELETE", r.get("method"), "Cancel must not send the delete");
	}
}
