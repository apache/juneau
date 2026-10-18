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
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Store page's pets treemap in the real browser: one tile per pet grouped by species then status, a pet tile
 * click (or Enter on the keyboard-walked tile) opens the pet edit dialog, saving it sends a CSRF-protected PUT and
 * redraws the treemap, and a group tile click does nothing.
 */
@Covers(value="charts", pages="/console/store")
@HookCovers({"actions", "csrf"})
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreTreemap_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String DLG = "[role=dialog]";
	private static final String CONFIRM = DLG + " .juneau-view-dialog-confirm";
	private static final String NAME = DLG + " [data-juneau-form-field=name]";

	// Records every non-GET fetch the page makes, with its status and its CSRF header.
	private static final String RECORD = "(() => { window.__writes = []; const f = window.fetch;"
		+ " window.fetch = async (u, o) => { const r = await f(u, o); const m = (o && o.method) || 'GET';"
		+ " if (m !== 'GET') { const h = (o && o.headers) || {}; const t = h instanceof Headers ? h.get('X-Csrf-Token') : (h['X-Csrf-Token'] || h['x-csrf-token'] || null);"
		+ " window.__writes.push({url: String(u), method: m, status: r.status, csrf: t, body: String(o && o.body || '')}); }"
		+ " return r; }; })()";

	// Reads the drawn treemap: every node in data-index order with its pixel box, and clicks by real mouse events on the canvas.
	private static final String TM = """
		(() => {
		  const chart = () => window.JuneauConsole.cardApi('pets-map').chart();
		  const data = c => c.getModel().getSeriesByIndex(0).getData();
		  const box = (c, i) => { const el = data(c).getItemGraphicEl(i); if (!el) return null;
		    const r = el.getBoundingRect().clone(), m = el.getComputedTransform(); if (m) r.applyTransform(m);
		    return { x: r.x, y: r.y, w: r.width, h: r.height }; };
		  const nodes = () => { const c = chart(), d = data(c), out = [];
		    for (let i = 0; i < d.count(); i++) { const n = d.tree.getNodeByDataIndex(i), l = n && n.getLayout();
		      if (n && l) out.push({ i, name: n.name, leaf: n.children.length === 0, status: n.parentNode ? n.parentNode.name : null, species: n.parentNode && n.parentNode.parentNode ? n.parentNode.parentNode.name : null, box: box(c, i) }); }
		    return out; };
		  const fire = (type, x, y) => { const el = chart().getZr().painter.getViewportRoot(), r = el.getBoundingClientRect();
		    el.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, clientX: r.left + x, clientY: r.top + y })); };
		  const click = (x, y) => { fire('mousemove', x, y); fire('mousedown', x, y); fire('mouseup', x, y); fire('click', x, y); };
		  const leaf = name => nodes().find(n => n.leaf && n.name === name);
		  window.__tm = {
		    nodes,
		    placed: () => nodes().filter(n => n.leaf).map(n => ({ name: n.name, status: n.status, species: n.species })),
		    leaves: () => nodes().filter(n => n.leaf).map(n => n.name),
		    groups: () => nodes().filter(n => !n.leaf && n.name).map(n => n.name),
		    speciesOf: name => { const n = leaf(name); return n ? n.species : null; },
		    clickLeaf: name => { const n = leaf(name); click(n.box.x + n.box.w / 2, n.box.y + n.box.h / 2); },
		    clickGroup: name => { const n = nodes().find(x => !x.leaf && x.name === name); click(n.box.x + n.box.w / 2, n.box.y + 3); }
		  };
		})()""";

	private static final String READY = "(() => { try { const t = window.__tm; return !!t && t.leaves().length > 3 && !!document.querySelector('#pets-map canvas'); } catch (e) { return false; } })()";
	private static final String DRAWN = "(() => { try { const a = window.JuneauConsole.cardApi('pets-map'); return !!a.chart() && !!a.getOption() && !!document.querySelector('#pets-map canvas'); } catch (e) { return false; } })()";

	private static String base;
	private static JsonList pets;
	private static JsonMap target;
	private static String moveTo;
	private static Map<String,Map<String,Object>> report;

	private static Map<String,Object> wait(String js) {
		return Map.of("waitFor", js, "timeoutMs", 15000);
	}

	private static Map<String,Object> eval(String js) {
		return Map.of("evaluate", js);
	}

	private static String js(String s) {
		return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	private static String get(String path) throws Exception {
		return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "application/json").GET().build(), BodyHandlers.ofString()).body();
	}

	/** The steps that wait for the treemap to draw and install the page helper. */
	private static List<Map<String,Object>> open() {
		return List.of(wait(DRAWN), eval(TM), wait(READY), eval(RECORD));
	}

	@SafeVarargs
	private static List<Map<String,Object>> then(List<Map<String,Object>> first, Map<String,Object>...more) {
		var l = new ArrayList<>(first);
		l.addAll(Arrays.asList(more));
		return l;
	}

	private static Map<String,Object> pick(String name, List<Map<String,Object>> steps, Map<String,String> queries) {
		return pick(name, "/console/store", steps, queries);
	}

	private static Map<String,Object> pick(String name, String path, List<Map<String,Object>> steps, Map<String,String> queries) {
		return Map.of("name", name, "path", path, "actions", steps, "queries", queries);
	}

	/** Edits the target pet's species through its tile, then waits for the redrawn tree to show it under {@code species}. */
	private static Map<String,Object> save(String name, String path, String species) {
		var n = js(target.getString("name"));
		return pick(name, path, then(open(),
			eval("window.__tm.clickLeaf(" + n + ")"),
			wait("document.querySelector('" + NAME + "')?.value"),
			eval("(() => { const s = document.querySelector('" + DLG + " [data-juneau-form-field=species]'); s.value = '" + species + "'; s.dispatchEvent(new Event('change', {bubbles: true})); })()"),
			Map.of("click", CONFIRM),
			wait("(window.__writes || []).some(w => w.method === 'PUT')"),
			wait("document.querySelectorAll('" + DLG + "').length === 0"),
			wait("(() => { try { return window.__tm.speciesOf(" + n + ") === '" + species + "'; } catch (e) { return false; } })()")), Map.of(
			"writes", "window.__writes",
			"dialogs", "document.querySelectorAll('" + DLG + "').length",
			"backdrops", "document.querySelectorAll('dialog[open], .juneau-view-dialog-backdrop').length",
			"species", "window.__tm.speciesOf(" + n + ")",
			"banner", "(document.querySelector('#pets-map [data-testid=chart-action-banner]') || {}).textContent || ''"));
	}

	@BeforeAll
	static void probe() throws Exception {
		base = PetstoreTestServer.awaitReady(server);
		pets = new JsonList(get("/console/store/stats/pets"));
		var names = new HashMap<String,Integer>();
		for (var p : pets.elements(JsonMap.class))
			names.merge(p.getString("name"), 1, Integer::sum);
		for (var p : pets.elements(JsonMap.class))
			if (target == null && names.get(p.getString("name")) == 1 && "AVAILABLE".equals(p.getString("status")))
				target = p;
		assertNotNull(target, "no uniquely named available pet");
		var from = target.getString("species");
		moveTo = "FISH".equals(from) ? "BIRD" : "FISH";
		var n = js(target.getString("name"));

		report = run("treemap", base, List.of(
			pick("render", open(), Map.of(
				"leaves", "window.__tm.leaves()",
				"groups", "window.__tm.groups()",
				"placed", "window.__tm.placed()",
				"live", "window.JuneauConsole.charts.__liveCount()")),
			pick("open", then(open(),
				eval("window.__tm.clickLeaf(" + n + ")"),
				wait("document.querySelector('" + NAME + "')?.value")), Map.of(
				"dialog", "document.querySelector('" + DLG + "').textContent",
				"nameValue", "document.querySelector('" + NAME + "').value",
				"speciesValue", "document.querySelector('" + DLG + " [data-juneau-form-field=species]').value",
				"writes", "window.__writes")),
			save("save", "/console/store", moveTo),
			save("saveBuilder", "/console/store?builder", from),
			pick("group", then(open(),
				eval("window.__clicks = []; window.__groupHits = 0;"
					+ " window.JuneauViews.bus.subscribe('pets-map.click', function (v) { window.__clicks.push(v); }, { retained: false });"
					+ " window.JuneauConsole.cardApi('pets-map').chart().on('click', function (p) { if (p.data && p.data.children && p.data.children.length) window.__groupHits++; });"),
				eval("window.__tm.clickGroup(" + js(from) + ")"),
				wait("window.__groupHits >= 1"),
				eval("new Promise(r => setTimeout(r, 800))")), Map.of(
				"dialogs", "document.querySelectorAll('" + DLG + "').length",
				"writes", "window.__writes",
				"hits", "window.__groupHits",
				"clicks", "window.__clicks",
				"banner", "(document.querySelector('#pets-map [data-testid=chart-action-banner]') || {}).textContent || ''")),
			pick("key", then(open(),
				eval("window.__first = window.__tm.leaves()[0]"),
				Map.of("press", List.of("#pets-map .jc-chart-plot", "ArrowRight")),
				Map.of("press", List.of("#pets-map .jc-chart-plot", "Enter")),
				wait("document.querySelector('" + NAME + "')?.value")), Map.of(
				"first", "window.__first",
				"nameValue", "document.querySelector('" + NAME + "').value",
				"dialog", "document.querySelector('" + DLG + "').textContent"))));
	}

	@Test void a01_storePage_rendersPetsTreemap_groupedBySpeciesThenStatus() {
		var c = assertClean(report, "render");
		var leaves = new TreeSet<String>();
		for (var p : pets.elements(JsonMap.class))
			leaves.add(p.getString("name"));
		assertEquals(leaves, new TreeSet<>(asStrings(query(c, "leaves"))));
		@SuppressWarnings("unchecked") // placed is a JSON array of {name, status, species}.
		var placed = (List<Map<String,Object>>)query(c, "placed");
		for (var p : pets.elements(JsonMap.class))
			assertTrue(placed.stream().anyMatch(x -> p.getString("name").equals(x.get("name"))
				&& p.getString("status").equals(x.get("status")) && p.getString("species").equals(x.get("species"))),
				() -> "pet " + p.getString("name") + " is not under status " + p.getString("status") + " under species " + p.getString("species") + ": " + placed);
		assertEquals(4, ((Number)query(c, "live")).intValue());
	}

	@Test void a02_clickPetTile_opensEditDialog_prefilledWithThatPet() {
		var c = assertClean(report, "open");
		assertString(target.getString("name"), query(c, "nameValue"));
		assertString(target.getString("species"), query(c, "speciesValue"));
		assertContains("Edit '" + target.getString("name") + "'", String.valueOf(query(c, "dialog")));
		assertEquals(List.of(), query(c, "writes"));
	}

	@Test void a03_submitEdit_putWithCsrf_dialogCloses_treemapRedrawsWithNewSpecies() {
		assertSaved("save", moveTo);
	}

	@Test void a03b_builderPage_submitEdit_redrawsTheTreemapToo() throws Exception {
		assertSaved("saveBuilder", target.getString("species"));
		assertEquals(target.getString("species"), new JsonMap(get("/petstore/pets/" + target.getLong("id"))).getString("species"));
	}

	private void assertSaved(String name, String species) {
		var c = assertClean(report, name);
		@SuppressWarnings("unchecked") // writes is a JSON array of {url, method, status, csrf, body}.
		var w = (List<Map<String,Object>>)query(c, "writes");
		assertSize(1, w);
		assertString("PUT", w.get(0).get("method"));
		assertContains("/console/pets/" + target.getLong("id"), String.valueOf(w.get(0).get("url")));
		assertEquals(200, ((Number)w.get(0).get("status")).intValue(), String.valueOf(w.get(0)));
		assertNotNull(w.get(0).get("csrf"), "the PUT carried no CSRF token");
		assertFalse(String.valueOf(w.get(0).get("csrf")).isBlank());
		assertContains("\"" + species + "\"", String.valueOf(w.get(0).get("body")));
		assertEquals(0, ((Number)query(c, "dialogs")).intValue());
		assertEquals(0, ((Number)query(c, "backdrops")).intValue());
		assertContains("Saved '" + target.getString("name") + "'", String.valueOf(query(c, "banner")));
		assertString(species, query(c, "species"));
	}

	@Test void a04_clickGroupTile_doesNotOpenDialog() {
		var c = assertClean(report, "group");
		assertEquals(0, ((Number)query(c, "dialogs")).intValue());
		assertEquals(List.of(), query(c, "writes"));
		assertTrue(((Number)query(c, "hits")).intValue() >= 1, "the group click never reached the chart");
		assertEquals(List.of(), query(c, "clicks"));
		assertEquals("", String.valueOf(query(c, "banner")));
	}

	@Test void a05_keyboardEnterOnLeaf_opensSameDialog() {
		var c = assertClean(report, "key");
		assertString(String.valueOf(query(c, "first")), query(c, "nameValue"));
		assertContains("Edit '" + query(c, "first") + "'", String.valueOf(query(c, "dialog")));
	}

	@SuppressWarnings("unchecked") // a JSON array of strings.
	private static List<String> asStrings(Object o) {
		return (List<String>)o;
	}
}
