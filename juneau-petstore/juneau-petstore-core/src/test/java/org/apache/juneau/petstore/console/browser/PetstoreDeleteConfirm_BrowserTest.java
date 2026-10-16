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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Users and Pets row Delete actions declare a {@code confirm} but no {@code present(DIALOG)}; in the real browser
 * each must still prompt with its row-filled text.  Cancel sends no DELETE; Confirm sends exactly one, for that row,
 * and the row leaves the table.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreDeleteConfirm_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String DLG = ".juneau-view-dialog";
	private static final String TITLE = DLG + " .juneau-view-dialog-title";
	private static final String NO_DLG = "!document.querySelector('" + DLG + "')";

	private static Map<String,Map<String,Object>> report;

	/** JS: some row in {@code body} has a cell whose text is exactly the quoted name (so "Rex 10" never matches "Rex 100"). */
	private static String rowWithName(String body) {
		return "Array.from(document.querySelectorAll('" + body + " tbody tr')).some(tr => Array.from(tr.cells).some(td => td.textContent.trim() === window.__name))";
	}

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 5000);
	}

	/**
	 * Opens the first row's action menu on {@code page}, clicks Delete, records the prompt (and the quoted name in it),
	 * then clicks {@code button} ({@code cancel} or {@code confirm}).
	 */
	private static Map<String,Object> theCase(String name, String page, String button) {
		var body = "#" + page + "-body";
		var actions = new ArrayList<Map<String,Object>>();
		actions.add(wait("document.querySelector('" + body + " .juneau-view-action-trigger')"));
		actions.add(Map.of("click", body + " .juneau-view-action-trigger"));
		actions.add(Map.of("click", ".juneau-view-action-item[data-action-id=delete]"));
		actions.add(wait("document.querySelector('" + TITLE + "')"));
		actions.add(Map.of("evaluate", "(() => { const t = document.querySelector('" + TITLE + "').textContent; window.__title = t;"
			+ " const m = /'([^']+)'/.exec(t); window.__name = m && m[1]; })()"));
		actions.add(Map.of("click", DLG + " .juneau-view-dialog-" + button));
		actions.add(wait(NO_DLG));
		if (button.equals("confirm"))
			actions.add(wait("!" + rowWithName(body)));
		return Map.of("name", name, "path", "/console/" + page, "capture", "/console/" + page + "/", "actions", actions,
			"queries", Map.of("title", "window.__title", "name", "window.__name",
				"rowStill", rowWithName(body)));
	}

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("delete-confirm", base, List.of(
			theCase("usersCancel", "users", "cancel"),
			theCase("usersConfirm", "users", "confirm"),
			theCase("petsCancel", "pets", "cancel"),
			theCase("petsConfirm", "pets", "confirm")));
	}

	@SuppressWarnings("unchecked") // requests is a JSON array of {url, method, postData}.
	private static List<String> deletes(Map<String,Object> c) {
		return ((List<Map<String,Object>>) c.get("requests")).stream()
			.filter(r -> "DELETE".equals(r.get("method")))
			.map(r -> String.valueOf(r.get("url")).replaceFirst("^https?://[^/]+", ""))
			.toList();
	}

	private static void assertCancelled(String name, String prompt) {
		var c = assertClean(report, name);
		var who = (String) query(c, "name");
		assertNotEmpty(who);
		assertEquals(prompt.replace("{}", who), query(c, "title"));
		assertEmpty(deletes(c));
		assertEquals(true, query(c, "rowStill"));
	}

	/** {@code url} is a glob in which {@code {}} stands for the quoted name the prompt showed. */
	private static void assertConfirmed(String name, String prompt, String url) {
		var c = assertClean(report, name);
		var who = (String) query(c, "name");
		assertNotEmpty(who);
		assertEquals(prompt.replace("{}", who), query(c, "title"));
		var d = deletes(c);
		assertSize(1, d);
		assertMatchesGlob(url.replace("{}", who), d.get(0));
		assertEquals(false, query(c, "rowStill"));
	}

	@Test void a01_usersDeletePromptsAndCancelSendsNothing() {
		assertCancelled("usersCancel", "Delete user '{}'?");
	}

	@Test void a02_usersDeleteConfirmSendsOneDelete() {
		assertConfirmed("usersConfirm", "Delete user '{}'?", "/console/users/{}");
	}

	@Test void a03_petsDeletePromptsAndCancelSendsNothing() {
		assertCancelled("petsCancel", "Delete '{}'?");
	}

	@Test void a04_petsDeleteConfirmSendsOneDelete() {
		assertConfirmed("petsConfirm", "Delete '{}'?", "/console/pets/*");
	}
}
