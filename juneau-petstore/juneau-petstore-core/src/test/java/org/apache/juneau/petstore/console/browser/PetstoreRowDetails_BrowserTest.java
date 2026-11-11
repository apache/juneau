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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * Expandable row details in the real browser: a Pets row expands into a tabbed panel (Details, Orders, History),
 * selecting a nested-table tab shows its pane, and an Orders row expands into Details and Pet.
 */
@Covers(value={"related-list"}, pages={"/console/pets","/console/orders"})
@HookCovers("details")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreRowDetails_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String TABS = "[...document.querySelectorAll('%s [role=tab]')].map(t => t.textContent.trim().replace(/\\s*\\d+$/, ''))";
	private static final String SELECTED = "[...document.querySelectorAll('%s [role=tab]')].filter(t => t.getAttribute('aria-selected') === 'true').map(t => t.textContent.trim().replace(/\\s*\\d+$/, ''))";

	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> expand(String name, String path, String body) {
		var toggle = body + " .juneau-view-detail-toggle";
		return Map.of("name", name, "path", path,
			"actions", List.of(
				Map.of("waitFor", "document.querySelector('" + toggle + "')"),
				Map.of("click", toggle),
				Map.of("waitFor", "document.querySelectorAll('" + body + " [role=tab]').length >= 2"),
				Map.of("evaluate", "sessionStorage.setItem('first', JSON.stringify(" + String.format(SELECTED, body) + "))"),
				Map.of("click", body + " [role=tab] >> nth=1"),
				Map.of("waitFor", "document.querySelector('" + body + " [role=tab][aria-selected=true]')?.textContent.includes('" + ("pets".equals(body.substring(1, 5)) ? "Orders" : "Pet") + "')")),
			"queries", Map.of("tabs", String.format(TABS, body), "first", "JSON.parse(sessionStorage.getItem('first'))", "selected", String.format(SELECTED, body)));
	}

	private static Map<String,Object> usersExpand() {
		var body = "#users-body";
		var toggle = body + " .juneau-view-detail-toggle";
		return Map.of("name", "users", "path", "/console/users",
			"actions", List.of(
				Map.of("waitFor", "document.querySelector('" + toggle + "')"),
				Map.of("click", toggle),
				Map.of("waitFor", "document.querySelectorAll('" + body + " [role=tab]').length >= 2")),
			"queries", Map.of("tabs", String.format(TABS, body),
				"checkboxes", "document.querySelectorAll('" + body + " .juneau-view-select-checkbox').length"));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("row-details", base, List.of(
			expand("pets", "/console/pets", "#pets-body"),
			expand("orders", "/console/orders", "#orders-body"),
			usersExpand()));
	}

	@Test void a01_petRowExpandsIntoThreeTabs() {
		var c = assertClean(report, "pets");
		assertList(query(c, "tabs"), "Details", "Orders", "History");
		assertList(query(c, "first"), "Details");
	}

	@Test void a02_selectingTheOrdersTabSwitchesPane() {
		assertList(query(assertClean(report, "pets"), "selected"), "Orders");
	}

	@Test void b01_orderRowExpandsIntoDetailsAndPet() {
		var c = assertClean(report, "orders");
		assertList(query(c, "tabs"), "Details", "Pet");
		assertList(query(c, "selected"), "Pet");
	}

	@Test void c01_usersTableHasNoSelectionColumnButDetailTabsLoad() {
		var c = assertClean(report, "users");
		assertList(query(c, "tabs"), "Details", "Orders");
		org.junit.jupiter.api.Assertions.assertEquals(0, ((Number)query(c, "checkboxes")).intValue());
	}
}
