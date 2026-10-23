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

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.util.*;

import javax.imageio.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.microservice.*;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.http.*;

/**
 * Console writes in the real browser, through the server's CSRF boundary: the page's token rides on every write, so
 * the P4 pencil stages a change, Pending changes applies and discards them, the Pets dialogs add and edit a pet, bulk
 * Mark sold sells one, and the Photo tab uploads a photo, which the API refuses without its token.  A form on another
 * origin that posts to the API changes nothing.
 */
@Covers(value="inline-edit", pages="/console/pets/1/summary")
@HookCovers("status")
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
@SuppressWarnings({
	"resource" // The connector belongs to the attacker server, which closes it; servlet response writers belong to the container.
})
class PetstoreConsoleWrites_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	// Records every non-GET fetch the page makes, with its status and body, so a case can assert what the server said.
	private static final String RECORD = "(() => { window.__writes = []; const f = window.fetch;"
		+ " window.fetch = async (u, o) => { const r = await f(u, o); const m = (o && o.method) || 'GET';"
		+ " if (m !== 'GET') window.__writes.push({url: String(u), method: m, status: r.status, body: await r.clone().text()});"
		+ " return r; }; })()";
	private static final String DLG = "[role=dialog]";
	private static final String CONFIRM = DLG + " .juneau-view-dialog-confirm";
	private static final String TYPED = "dialog.petstore-typed-confirm";

	private static String base, renamed, attackOrigin;
	private static long renamePet, repricePet;
	private static int petsBefore, petsAfter;
	private static Server attacker;
	private static Map<String,Map<String,Object>> report, attack;

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 8000);
	}

	private static Map<String,Object> eval(String js) {
		return Map.of("evaluate", js);
	}

	/** JS: a recorded write to a URL containing {@code part} has come back. */
	private static String wrote(String method, String part) {
		return "(window.__writes || []).some(w => w.method === '" + method + "' && w.url.includes('" + part + "'))";
	}

	/** Opens a pencil on a pet's Summary, types a value and presses Enter. */
	private static Map<String,Object> pencil(String name, long pet, String action, String value) {
		var pencil = "#pet-body [data-juneau-edit=\"" + action + "\"]";
		var input = "#pet-body .juneau-edit-input";
		return Map.of("name", name, "path", "/console/pets/" + pet + "/summary",
			"actions", List.of(
				wait("document.querySelector('" + pencil + "')"),
				eval(RECORD),
				Map.of("click", pencil),
				wait("document.querySelector('" + input + "')"),
				Map.of("fill", List.of(input, value)),
				Map.of("press", List.of(input, "Enter")),
				wait(wrote("POST", "/stage")),
				wait("!document.querySelector('" + input + "')"),
				wait("document.querySelector('.juneau-view-bulk-toast')")),
			"queries", Map.of("writes", "window.__writes",
				"toast", "document.querySelector('.juneau-view-bulk-toast')?.textContent ?? null",
				"error", "document.querySelector('#pet-body .juneau-edit-error:not([hidden])')?.textContent ?? null"));
	}

	/** Selects the Pending changes row whose summary contains {@code text} and runs the {@code action} bulk action. */
	private static List<Map<String,Object>> bulkOnChange(String text, String action) {
		var row = "[...document.querySelectorAll('#changes-body tbody tr')].find(tr => tr.textContent.includes(" + jsString(text) + "))";
		return List.of(
			wait(row),
			eval(RECORD),
			eval(row + ".querySelector('.juneau-view-select-checkbox').click()"),
			wait("document.querySelector('.juneau-view-bulk-toolbar:not([hidden]) .juneau-view-bulk-select')"),
			eval("(() => { const s = document.querySelector('.juneau-view-bulk-select'); s.value = '" + action + "'; s.dispatchEvent(new Event('change', {bubbles: true})); })()"),
			Map.of("click", ".juneau-view-bulk-go"));
	}

	private static String jsString(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	// Statements for an async script with a form `f` in scope: copy the API's CSRF token off the admin Photo page
	// (as the server stamps it on its upload form) onto `f`, so the page's own petstore.js sends it in the X-Csrf-Token
	// header; a08 succeeding through the API boundary is what proves that header name.
	private static final String STAMP = """
			const html = await fetch(location.pathname, { headers: { Authorization: 'Bearer petstore-admin', Accept: 'text/html' } }).then(r => r.text());
			f.setAttribute('data-petstore-csrf', /data-petstore-csrf="([0-9a-f]+)"/.exec(html)[1]);
""";

	// Draws a 7x5 PNG in the page and submits it through a Photo-tab upload form.
	private static final String UPLOAD_PNG = """
		(async () => {
			const c = document.createElement('canvas');
			c.width = 7; c.height = 5;
			c.getContext('2d').fillRect(0, 0, 7, 5);
			const blob = await new Promise(r => c.toBlob(r, 'image/png'));
			const f = document.createElement('form');
			f.setAttribute('data-petstore-upload', '/petstore/pets/1/photo');
			f.innerHTML = '<input type="file"><button type="submit">Upload</button>';
			document.body.appendChild(f);
""" + STAMP + """
			const dt = new DataTransfer();
			dt.items.add(new File([blob], 'photo.png', { type: 'image/png' }));
			f.querySelector('input').files = dt.files;
			sessionStorage.setItem('uploaded', 'pending');
			f.querySelector('button').click();
		})()""";

	// Submits bytes that are not an image, typed image/png, through a Photo-tab upload form: the API answers 400 and
	// the page shows that on its status line (JuneauViews.status) instead of reloading.
	private static final String UPLOAD_BAD = """
		(async () => {
			const f = document.createElement('form');
			f.setAttribute('data-petstore-upload', '/petstore/pets/1/photo');
			f.innerHTML = '<input type="file"><button type="submit">Upload</button>';
			document.body.appendChild(f);
""" + STAMP + """
			const dt = new DataTransfer();
			dt.items.add(new File(['not an image'], 'photo.png', { type: 'image/png' }));
			f.querySelector('input').files = dt.files;
			sessionStorage.setItem('badUpload', 'pending');
			f.querySelector('button').click();
		})()""";

	// The same upload with no token stamped on the form, of an 11x13 PNG (the other scenario sends 7x5): the API refuses it.
	private static final String UPLOAD_NO_TOKEN = """
		(async () => {
			const c = document.createElement('canvas');
			c.width = 11; c.height = 13;
			c.getContext('2d').fillRect(0, 0, 11, 13);
			const blob = await new Promise(r => c.toBlob(r, 'image/png'));
			const f = document.createElement('form');
			f.setAttribute('data-petstore-upload', '/petstore/pets/1/photo');
			f.innerHTML = '<input type="file"><button type="submit">Upload</button>';
			document.body.appendChild(f);
			const dt = new DataTransfer();
			dt.items.add(new File([blob], 'photo.png', { type: 'image/png' }));
			f.querySelector('input').files = dt.files;
			f.querySelector('button').click();
		})()""";

	@BeforeAll
	static void probe() throws Exception {
		base = PetstoreTestServer.awaitReady(server);
		var available = new ArrayList<Long>();
		for (var o : new JsonList(get("/petstore/pets", "application/json").body()))
			if ("AVAILABLE".equals(((JsonMap)o).getString("status")))
				available.add(((JsonMap)o).getLong("id"));
		renamePet = available.get(0);
		repricePet = available.get(1);
		renamed = "Renamed " + renamePet;
		report = run("console-writes", base, List.of(
			pencil("rename", renamePet, "rename", renamed),
			pencil("reprice", repricePet, "reprice", "77.5"),
			Map.of("name", "apply", "path", "/console/pets/changes",
				"actions", concat(bulkOnChange(renamed, "apply"),
					wait("document.querySelector('" + CONFIRM + "')"),
					Map.of("click", CONFIRM),
					wait(wrote("POST", "/apply"))),
				"queries", Map.of("writes", "window.__writes"),
				"fetches", Map.of("pet", "/petstore/pets/" + renamePet)),
			Map.of("name", "discard", "path", "/console/pets/changes",
				"actions", concat(bulkOnChange("77.5", "discard"),
					wait("document.querySelector('" + TYPED + "')"),
					eval("(() => { const i = document.querySelector('" + TYPED + " input'); i.value = /Type (.+) to confirm/.exec(i.getAttribute('aria-label'))[1]; i.dispatchEvent(new Event('input')); })()"),
					Map.of("click", TYPED + " .petstore-typed-confirm-ok"),
					wait(wrote("POST", "/discard"))),
				"queries", Map.of("writes", "window.__writes")),
			Map.of("name", "add", "path", "/console/pets",
				"actions", List.of(
					wait("[...document.querySelectorAll('.juneau-view-ribbon-btn')].find(b => b.getAttribute('aria-label') === '+ Add pet')"),
					eval(RECORD),
					eval("[...document.querySelectorAll('.juneau-view-ribbon-btn')].find(b => b.getAttribute('aria-label') === '+ Add pet').click()"),
					wait("document.querySelector('" + DLG + " [data-juneau-form-field=name]')"),
					Map.of("fill", List.of(DLG + " [data-juneau-form-field=name]", "Added in browser")),
					eval("(() => { const s = document.querySelector('" + DLG + " [data-juneau-form-field=species]'); s.value = 'DOG'; s.dispatchEvent(new Event('change', {bubbles: true})); })()"),
					Map.of("fill", List.of(DLG + " [data-juneau-form-field=price]", "12.5")),
					Map.of("click", CONFIRM),
					wait(wrote("POST", "/console/pets"))),
				"queries", Map.of("writes", "window.__writes")),
			Map.of("name", "edit", "path", "/console/pets",
				"actions", List.of(
					wait("document.querySelector('#pets-body .juneau-view-action-trigger')"),
					eval(RECORD),
					Map.of("click", "#pets-body .juneau-view-action-trigger"),
					Map.of("click", ".juneau-view-action-item[data-action-id=edit]"),
					wait("document.querySelector('" + DLG + " [data-juneau-form-field=name]')?.value"),
					Map.of("fill", List.of(DLG + " [data-juneau-form-field=name]", "Edited in browser")),
					Map.of("click", CONFIRM),
					wait(wrote("PUT", "/console/pets/"))),
				"queries", Map.of("writes", "window.__writes")),
			Map.of("name", "sell", "path", "/console/pets",
				"actions", List.of(
					wait("document.querySelector('#pets-body .juneau-view-select-checkbox:not(:disabled)')"),
					eval(RECORD),
					eval("(() => { const cb = document.querySelector('#pets-body .juneau-view-select-checkbox:not(:disabled)'); window.__sold = cb.closest('tr').cells[1].textContent.trim(); cb.click(); })()"),
					wait("document.querySelector('.juneau-view-bulk-toolbar:not([hidden]) .juneau-view-bulk-action-btn')"),
					Map.of("click", ".juneau-view-bulk-action-btn"),
					wait("document.querySelector('" + CONFIRM + "')"),
					Map.of("click", CONFIRM),
					wait("document.querySelector('.juneau-view-bulk-toast')")),
				"queries", Map.of("writes", "window.__writes", "sold", "window.__sold",
					"toast", "document.querySelector('.juneau-view-bulk-toast')?.textContent")),
			// The tab's <object> asks for the photo before there is one, so its 404 load is expected.
			Map.of("name", "upload", "waitUntil", "networkidle", "path", "/console/pets/1/photo", "allowFailedLoads", true,
				"actions", List.of(
					eval(UPLOAD_PNG),
					wait("performance.getEntriesByType('navigation')[0].type === 'reload'")),
				"queries", Map.of("status", "document.querySelector('form[data-petstore-upload] [role=status]')?.textContent ?? null",
					"marker", "sessionStorage.getItem('uploaded')")),
			Map.of("name", "uploadNoToken", "waitUntil", "networkidle", "path", "/console/pets/1/photo", "allowFailedLoads", true,
				"actions", List.of(
					eval(UPLOAD_NO_TOKEN),
					wait("document.querySelector('form[data-petstore-upload] [role=status]')")),
				"queries", Map.of("status", "document.querySelector('form[data-petstore-upload] [role=status]')?.textContent ?? null",
					"navigation", "performance.getEntriesByType('navigation')[0].type")),
			Map.of("name", "uploadFails", "waitUntil", "networkidle", "path", "/console/pets/1/photo", "allowFailedLoads", true,
				"actions", List.of(
					eval(UPLOAD_BAD),
					wait("document.querySelector('form[data-petstore-upload] [role=status]')")),
				"queries", Map.of("status", "document.querySelector('form[data-petstore-upload] [role=status]')?.textContent ?? null",
					"navigation", "performance.getEntriesByType('navigation')[0].type"))));

		// A page on another origin posts a urlencoded form at the API as soon as it loads; the browser sends it with the
		// attacker's Origin, and the API's boundary must refuse it.
		attacker = new Server();
		var connector = new ServerConnector(attacker);
		connector.setHost("127.0.0.1");
		connector.setPort(0);
		attacker.addConnector(connector);
		connector.open();
		var attackCtx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		attackCtx.setContextPath("/");
		attackCtx.addServlet(new ServletHolder(new AttackServlet(base)), "/attack");
		attacker.setHandler(attackCtx);
		attacker.start();
		attackOrigin = "http://127.0.0.1:" + connector.getLocalPort();
		petsBefore = new JsonList(get("/petstore/pets", "application/json").body()).size();
		attack = run("console-writes-cross-origin", attackOrigin, List.of(
			Map.of("name", "crossOriginForm", "path", "/attack", "allowFailedLoads", true, "waitUntil", "load",
				"actions", List.of(
					wait("location.origin === '" + base + "'")),
				"queries", Map.of("landed", "location.origin", "path", "location.pathname"))));
		petsAfter = new JsonList(get("/petstore/pets", "application/json").body()).size();
	}

	@AfterAll
	static void stopAttacker() throws Exception {
		if (attacker != null)
			attacker.stop();
	}

	/** Serves the attacker's page: a urlencoded form aimed at the API's pet collection, submitted on load. */
	static final class AttackServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private final String app;

		AttackServlet(String app) {
			this.app = app;
		}

		@Override /* HttpServlet */
		protected void doGet(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
			res.setContentType("text/html;charset=utf-8");
			res.getWriter().write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><link rel=\"icon\" href=\"data:,\"></head><body>"
				+ "<form id=\"f\" method=\"post\" action=\"" + app + "/petstore/pets\">"
				+ "<input name=\"name\" value=\"Planted\"><input name=\"species\" value=\"DOG\"><input name=\"price\" value=\"1\"></form>"
				+ "<script>document.getElementById('f').submit();</script></body></html>");
		}
	}

	@SafeVarargs
	private static List<Map<String,Object>> concat(List<Map<String,Object>> head, Map<String,Object>...tail) {
		var l = new ArrayList<>(head);
		l.addAll(List.of(tail));
		return l;
	}

	private static HttpResponse<String> get(String path, String accept) throws Exception {
		return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", accept).GET().build(), BodyHandlers.ofString());
	}

	private static JsonMap pet(Object id) throws Exception {
		return new JsonMap(get("/petstore/pets/" + id, "application/json").body());
	}

	@SuppressWarnings({
		"unchecked" // writes is a JSON array of {url, method, status, body}.
	})
	private static Map<String,Object> theWrite(String name, String method, String part) {
		var c = assertClean(report, name);
		var w = ((List<Map<String,Object>>) query(c, "writes")).stream()
			.filter(x -> method.equals(x.get("method")) && String.valueOf(x.get("url")).contains(part)).toList();
		assertSize(1, w);
		assertEquals(200, ((Number) w.get(0).get("status")).intValue(), () -> name + ": " + w.get(0));
		return w.get(0);
	}

	@Test void a01_pencilSaveStagesTheRename() {
		theWrite("rename", "POST", "/console/pets/" + renamePet + "/stage");
		assertContains("Staged", (String) query(report.get("rename"), "toast"));
		assertNull(query(report.get("rename"), "error"));
	}

	@Test void a02_pencilSaveStagesTheReprice() {
		theWrite("reprice", "POST", "/console/pets/" + repricePet + "/stage");
		assertContains("Staged", (String) query(report.get("reprice"), "toast"));
	}

	@SuppressWarnings({
		"unchecked" // fetches is a JSON object of {status, body}.
	})
	@Test void a03_applyWritesTheStagedChange() throws Exception {
		assertContains("Applied", theWrite("apply", "POST", "/apply").get("body"));
		var pet = (Map<String,Object>) ((Map<String,Object>) report.get("apply").get("fetches")).get("pet");
		assertString(renamed, new JsonMap(String.valueOf(pet.get("body"))).getString("name"));
	}

	@Test void a04_discardDropsTheStagedChange() throws Exception {
		assertContains("\"succeeded\"", theWrite("discard", "POST", "/console/pets/changes/discard").get("body"));
		assertNotEquals("77.5", String.valueOf(pet(repricePet).get("price")));
	}

	@Test void a05_addPetDialogCreatesAPet() throws Exception {
		assertContains("Added 'Added in browser'", theWrite("add", "POST", "/console/pets").get("body"));
		assertContains("Added in browser", get("/petstore/pets", "application/json").body());
	}

	@Test void a06_editPetDialogReplacesThePet() throws Exception {
		theWrite("edit", "PUT", "/console/pets/");
		assertContains("Edited in browser", get("/petstore/pets", "application/json").body());
	}

	@Test void a07_bulkMarkSoldSellsTheSelectedPet() throws Exception {
		var c = assertClean(report, "sell");
		theWrite("sell", "POST", "/sell");
		assertNotContains("fail", String.valueOf(query(c, "toast")).toLowerCase());
		assertString("SOLD", pet(query(c, "sold")).getString("status"));
	}

	@Test void a08_photoUploadSucceedsAndReloads() throws Exception {
		var c = assertClean(report, "upload");
		assertString("pending", query(c, "marker"));
		assertNull(query(c, "status"));
		var r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets/1/photo")).header("Accept", "image/png").GET().build(), BodyHandlers.ofByteArray());
		assertEquals(200, r.statusCode());
		var img = ImageIO.read(new ByteArrayInputStream(r.body()));
		assertEquals(7, img.getWidth());
		assertEquals(5, img.getHeight());
	}

	@Test void a09_failedPhotoUploadShowsTheStatusLineAndStaysOnThePage() {
		var c = assertClean(report, "uploadFails");
		assertContains("Upload failed (400)", String.valueOf(query(c, "status")));
		assertString("navigate", query(c, "navigation"));
	}

	@Test void a10_photoUploadWithoutTheTokenIsRefusedAndStaysOnThePage() throws Exception {
		var c = assertClean(report, "uploadNoToken");
		assertContains("Upload failed (403)", String.valueOf(query(c, "status")));
		assertString("navigate", query(c, "navigation"));
		var r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/petstore/pets/1/photo")).header("Accept", "image/png").GET().build(), BodyHandlers.ofByteArray());
		// Whatever the pet's photo is, it is not the refused upload's 11x13 image (there may be none at all).
		var img = r.statusCode() == 200 ? ImageIO.read(new ByteArrayInputStream(r.body())) : null;
		assertFalse(img != null && img.getWidth() == 11 && img.getHeight() == 13, "the refused upload was stored");
	}

	@Test void a11_crossOriginFormPostToTheApiChangesNothing() throws Exception {
		var c = assertClean(attack, "crossOriginForm");
		assertString(base, query(c, "landed"));
		assertEquals(petsBefore, petsAfter);
		assertNotContains("Planted", get("/petstore/pets", "application/json").body());
	}
}
