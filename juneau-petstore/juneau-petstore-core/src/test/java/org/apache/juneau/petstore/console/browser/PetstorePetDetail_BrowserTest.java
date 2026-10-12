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
 * P4 in the real browser: the Summary pencil opens its inline editor, the row menu omits the inline-only actions,
 * an order selects at nav depth 4, and a failed photo upload stays on the page with its error.
 */
@Covers(value="depth-4-nav", pages="/console/pets/1/orders")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstorePetDetail_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String CURRENT = "[...document.querySelectorAll('[data-juneau-nav-id][aria-current=\"page\"]')].map(a => a.getAttribute('data-juneau-nav-id'))";
	private static final String CHILD_ROWS = "document.querySelectorAll('.juneau-page-nav-children').length";
	private static final String PENCIL = "#pet-body [data-juneau-edit=\"rename\"]";
	private static final String MENU_ITEMS = "[...document.querySelectorAll('.juneau-view-action-item')].map(b => b.dataset.actionId)";

	// The PUT fails (a text file is not a photo); the form stays and shows the toolkit status line.
	private static final String UPLOAD_TEXT = """
		(() => {
			const f = document.createElement('form');
			f.setAttribute('data-petstore-upload', '/petstore/pets/1/photo');
			f.innerHTML = '<input type="file"><button type="submit">Upload</button>';
			document.body.appendChild(f);
			const dt = new DataTransfer();
			dt.items.add(new File(['not a photo'], 'notes.txt', { type: 'text/plain' }));
			f.querySelector('input').files = dt.files;
			f.querySelector('button').click();
		})()""";
	private static final String UPLOAD_STATUS = "document.querySelector('form[data-petstore-upload] [role=status]')?.textContent";

	private static Map<String,Map<String,Object>> report;
	private static long petId, orderId;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		var orders = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/petstore/orders")).header("Accept", "application/json").build(), BodyHandlers.ofString());
		var o = new JsonList(orders.body()).getMap(0);
		petId = o.getLong("petId");
		orderId = o.getLong("id");
		report = run("pet-detail", base, List.of(
			Map.of("name", "inline", "path", "/console/pets/1/summary",
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('" + PENCIL + "')"),
					Map.of("click", PENCIL),
					Map.of("waitFor", "document.querySelector('#pet-body .juneau-edit-input')")),
				"queries", Map.of("current", CURRENT, "rows", CHILD_ROWS, "editing", "document.querySelector('#pet-body .juneau-edit-input').value")),
			Map.of("name", "menu", "path", "/console/pets/1/summary",
				"actions", List.of(
					Map.of("waitFor", "document.querySelector('#pet-body .juneau-view-action-trigger')"),
					Map.of("click", "#pet-body .juneau-view-action-trigger"),
					Map.of("waitFor", "document.querySelector('.juneau-view-action-item')")),
				"queries", Map.of("items", MENU_ITEMS)),
			Map.of("name", "order", "path", "/console/pets/" + petId + "/orders/" + orderId, "queries", Map.of("current", CURRENT, "rows", CHILD_ROWS)),
			Map.of("name", "upload-error", "path", "/console/pets/1/photo", "allowFailedLoads", true,
				"actions", List.of(
					Map.of("evaluate", UPLOAD_TEXT),
					Map.of("waitFor", "(" + UPLOAD_STATUS + " || '').startsWith('Upload failed')")),
				"queries", Map.of("status", UPLOAD_STATUS, "path", "location.pathname"))));
	}

	@Test void a01_summaryIsDepthThree() {
		var c = assertClean(report, "inline");
		assertList(query(c, "current"), "pets", "p1", "summary");
		assertString("2", String.valueOf(query(c, "rows")));
	}

	// The submits (pencil Enter, dialog confirm) are not exercised: petstore pages carry no CSRF token, so the
	// toolkit refuses every row-action write client-side before it is sent.

	@Test void a02_pencilOpensTheEditorOnTheName() {
		assertString("Mr. Frisky", query(assertClean(report, "inline"), "editing"));
	}

	@Test void a03_rowMenuOmitsTheInlineOnlyActions() {
		assertList(query(assertClean(report, "menu"), "items"), "sell");
	}

	@Test void a04_orderIsDepthFour() {
		var c = assertClean(report, "order");
		assertList(query(c, "current"), "pets", "p" + petId, "orders", "o" + orderId);
		assertString("3", String.valueOf(query(c, "rows")));
	}

	@Test void a05_failedUploadShowsTheError() {
		var c = assertClean(report, "upload-error");
		assertContains("Upload failed (415)", query(c, "status"));
		assertString("/console/pets/1/photo", query(c, "path"));
	}
}
