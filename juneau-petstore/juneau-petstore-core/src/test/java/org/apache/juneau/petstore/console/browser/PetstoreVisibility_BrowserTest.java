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
 * Visibility rules in the real browser: a pet's Summary offers Mark sold and the pencils only while the pet is not
 * sold, and they go once the row redraws as sold; the Photo tab's upload card is rendered for an admin and omitted
 * for everyone else.
 */
@Covers(value="visibility-rules", pages={"/console/pets/1/summary", "/console/pets/1/photo"})
@HookCovers("rules")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreVisibility_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String TRIGGER = "#pet-body .juneau-view-action-trigger";
	private static final String MENU_ITEMS = "[...document.querySelectorAll('.juneau-view-action-item')].map(b => b.dataset.actionId)";
	private static final String PENCILS = "[...document.querySelectorAll('#pet-body [data-juneau-edit]')].filter(e => !e.hidden).map(e => e.getAttribute('data-juneau-edit'))";
	private static final String CONFIRM = ".juneau-view-dialog-confirm";
	private static final String UPLOAD = "form[data-petstore-upload]";
	// Whether the Photo tab, fetched with the given bearer token, carries the upload form (inside its card <template>).
	private static final String UPLOAD_FOR = "(token => fetch(location.pathname, {headers: token ? {Authorization: 'Bearer ' + token} : {}})"
		+ ".then(r => r.text()).then(t => t.includes('data-petstore-upload=')))";

	private static String base;
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
		report = run("visibility", base, List.of(
			Map.of("name", "sell", "path", "/console/pets/" + pet + "/summary",
				"actions", List.of(
					wait("document.querySelector('" + TRIGGER + "')"),
					Map.of("evaluate", "window.__before = {pencils: " + PENCILS + "}"),
					Map.of("click", TRIGGER),
					wait("document.querySelector('.juneau-view-action-item')"),
					Map.of("evaluate", "window.__before.menu = " + MENU_ITEMS),
					Map.of("click", ".juneau-view-action-item[data-action-id=sell]"),
					wait("document.querySelector('" + CONFIRM + "')"),
					Map.of("click", CONFIRM),
					wait("document.querySelector('#pet-body tbody')?.textContent.toUpperCase().includes('SOLD')")),
				"queries", Map.of("before", "window.__before", "pencils", PENCILS,
					"trigger", "(() => { const t = document.querySelector('" + TRIGGER + "'); return !!t && !t.hidden && !t.disabled; })()")),
			Map.of("name", "photo", "waitUntil", "networkidle", "path", "/console/pets/1/photo", "allowFailedLoads", true,
				"queries", Map.of("viewer", "!!document.querySelector('" + UPLOAD + "')",
					"admin", UPLOAD_FOR + "('petstore-admin')", "user", UPLOAD_FOR + "('petstore-user')"))));
	}

	private static HttpResponse<String> get(String path) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - row actions
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_unsoldPetOffersSellAndThePencils() {
		assertBean(query(assertClean(report, "sell"), "before"), "menu,pencils", "[sell],[rename,reprice]");
	}

	@Test void a02_soldPetLosesThem() throws Exception {
		var c = assertClean(report, "sell");
		assertString("SOLD", new JsonMap(get("/petstore/pets/" + pet).body()).getString("status"));
		assertEmpty(query(c, "pencils"));
		assertEquals(false, query(c, "trigger"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - cards
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_uploadCardIsAdminOnly() {
		assertBean(assertClean(report, "photo").get("queries"), "viewer,user,admin", "false,false,true");
	}
}
