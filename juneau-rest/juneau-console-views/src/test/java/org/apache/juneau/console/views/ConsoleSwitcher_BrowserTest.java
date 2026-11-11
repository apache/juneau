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
package org.apache.juneau.console.views;

import static org.apache.juneau.console.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The switcher card in real Chromium against the real shell, views pack, charts runtime and ECharts: a chart that
 * redraws after its view is hidden and shown again, view and section choice kept in the address and in preferences
 * (the address winning), two switchers sharing one address directive, keyboard and labelling, and the folder-tab look
 * of the section strip.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-console-views -am test` from the repo root")
class ConsoleSwitcher_BrowserTest extends TestBase {

	private static final String CHARTS = "/juneau-console/juneau-charts.js";
	private static final String ECHARTS = "/webjars/echarts/6.1.0/dist/echarts.min.js";
	private static final String ECHARTS_RESOURCE = "/META-INF/resources" + ECHARTS;
	private static final String VIEW_PREF = "juneau-card:sw1:switcher:view";
	private static final String SECTION_PREF = "juneau-card:sw1:switcher:section.multi";

	private static final String LEAF = "<script>JuneauConsole.registerCard('leaf', class extends JuneauViews.model.Card { render() { this.el.textContent = 'leaf:' + this.spec.id; } });</script>\n";

	// Shared helpers for the page scripts.
	private static final String HELPERS = """
		function until(test, ms) {
		  var end = Date.now() + (ms || 5000);
		  return new Promise(function (res) {
		    (function poll() { var v = test(); if (v || Date.now() > end) res(v); else requestAnimationFrame(poll); })();
		  });
		}
		function frames() { return new Promise(function (r) { requestAnimationFrame(function () { requestAnimationFrame(r); }); }); }
		function tab(id) { return document.querySelector('[data-tab-id="' + id + '"]'); }
		function accent() {
		  var t = document.createElement('i'); t.style.borderTop = '1px solid var(--jc-accent)'; document.body.appendChild(t);
		  var v = getComputedStyle(t).borderTopColor; t.remove(); return v;
		}
		""";

	private static final String MAIN_SCRIPT = "<script>\n(function () {\n" + HELPERS + """
		var p = window.__probe = { aria: [] };
		function rec() {
		  var d = document.getElementById('sw1-display'), sel = document.querySelector('#sw1 .juneau-switcher-selector [aria-selected="true"]');
		  p.aria.push({ labelled: d.getAttribute('aria-labelledby'), selected: sel && sel.id });
		}
		function chartState() {
		  var c = window.JuneauConsole.cardApi('c-chart').chart(), el = document.querySelector('#c-chart canvas');
		  return { w: el ? el.getBoundingClientRect().width : 0, cw: c.getWidth(), count: c.getModel().getSeriesByIndex(0).getData().count() };
		}
		document.addEventListener('DOMContentLoaded', async function () { try {
		  await until(function () { try { return window.JuneauConsole.cardApi('c-chart').chart(); } catch (e) { return null; } });
		  p.initial = window.JuneauConsole.cardApi('sw1').activeView;
		  p.addrStart = location.search;
		  p.chart0 = chartState(); rec();
		  tab('data').click(); await frames(); rec();
		  p.addrData = decodeURIComponent(location.search);
		  p.prefData = localStorage.getItem('juneau-card:sw1:switcher:view');
		  tab('chart').click(); await frames(); await frames();
		  p.chart1 = chartState(); p.addrDefault = decodeURIComponent(location.search); rec();
		  tab('multi').click(); await frames(); rec();
		  p.addrMulti = decodeURIComponent(location.search);
		  p.subStrips = document.querySelectorAll('#sw1-display [data-juneau-strip-mode="tab"]').length;
		  tab('s2').click(); await frames();
		  p.addrSection = decodeURIComponent(location.search);
		  p.prefSection = localStorage.getItem('juneau-card:sw1:switcher:section.multi');
		  var sel = tab('s2'), uns = tab('s1'), panes = document.querySelector('#sw1 .juneau-view-helper-tabstrip-panes');
		  var accentColor = accent(), cs = getComputedStyle(sel), cu = getComputedStyle(uns);
		  p.look = { accent: accentColor, selWeight: cs.fontWeight, selBorderTop: cs.borderTopColor, selBorderTopWidth: cs.borderTopWidth, selShadow: cs.boxShadow, selBg: cs.backgroundColor,
		    unsBg: cu.backgroundColor, unsWeight: cu.fontWeight, panes: panes ? getComputedStyle(panes).borderTopColor : null };
		  } catch (e) { window.__probe.error = String(e && e.stack || e); }
		  var done = document.createElement('i'); done.id = 'done'; document.body.appendChild(done);
		});
		})();
		</script>
		""";

	// The keyboard page opens on the multi-section view; steps drive real key presses and record where focus lands.
	private static String rec(String key) {
		return "var a = document.activeElement; window.__probe." + key + " = { tab: a.getAttribute('data-tab-id'), id: a.id, tabindex: a.getAttribute('tabindex'), "
			+ "offset: getComputedStyle(a).outlineOffset };";
	}

	private static final String KBD_SCRIPT = "<script>\n(function () {\n" + HELPERS + """
		window.__probe = {};
		document.addEventListener('DOMContentLoaded', async function () { try {
		  await until(function () { return tab('mv'); });
		  await frames();
		  } catch (e) { window.__probe.error = String(e && e.stack || e); }
		  var done = document.createElement('i'); done.id = 'done'; document.body.appendChild(done);
		});
		})();
		</script>
		""";

	private static final String TWO_SCRIPT = "<script>\n(function () {\n" + HELPERS + """
		var p = window.__probe = {};
		document.addEventListener('DOMContentLoaded', async function () { try {
		  await until(function () { return tab('a2') && tab('b3'); });
		  tab('a2').click(); await frames(); tab('b3').click(); await frames();
		  p.addr = decodeURIComponent(location.search);
		  p.active = [window.JuneauConsole.cardApi('swa').activeView, window.JuneauConsole.cardApi('swb').activeView];
		  tab('a1').click(); await frames();
		  p.addrAfterDefault = decodeURIComponent(location.search);
		  } catch (e) { window.__probe.error = String(e && e.stack || e); }
		  var done = document.createElement('i'); done.id = 'done'; document.body.appendChild(done);
		});
		})();
		</script>
		""";

	private static final String PROBE_SCRIPT = "<script>\n(function () {\n" + HELPERS + """
		var p = window.__probe = {};
		document.addEventListener('DOMContentLoaded', async function () { try {
		  await until(function () { return document.getElementById('sw1-display'); });
		  await frames();
		  p.active = window.JuneauConsole.cardApi('sw1').activeView;
		  var sec = document.querySelector('#sw1-display [data-juneau-strip-mode="tab"] [aria-selected="true"]');
		  p.section = sec ? sec.getAttribute('data-tab-id') : null;
		  } catch (e) { window.__probe.error = String(e && e.stack || e); }
		  var done = document.createElement('i'); done.id = 'done'; document.body.appendChild(done);
		});
		})();
		</script>
		""";

	// Layout probes: slider thumb, borderless nested host, unclipped selected tab, card gap, and a datatables card in a switcher.
	private static final String LAYOUT_SCRIPT = "<script>\n(function () {\n" + HELPERS + """
		var p = window.__probe = {};
		function wait(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
		document.addEventListener('DOMContentLoaded', async function () { try {
		  await until(function () { return tab('b') && tab('s2') && tab('slb'); });
		  await frames();
		  var t1 = document.getElementById('top1').getBoundingClientRect(), t2 = document.getElementById('top2').getBoundingClientRect();
		  var g = document.createElement('i'); g.style.display = 'block'; g.style.height = 'var(--jc-card-gap, 1rem)'; document.body.appendChild(g);
		  p.gap = { actual: t2.top - t1.bottom, token: g.getBoundingClientRect().height }; g.remove();
		  var host = document.querySelector('.juneau-switcher-view .jc-card[data-juneau-in-switcher]'), hs = getComputedStyle(host);
		  p.nested = { shadow: hs.boxShadow, padding: hs.paddingTop + ' ' + hs.paddingLeft, bg: hs.backgroundColor, attr: host.hasAttribute('data-juneau-in-switcher') };
		  tab('s2').click(); await frames();
		  var sel = tab('s2').getBoundingClientRect(), panes = document.querySelector('#swm .juneau-view-helper-tabstrip-panes').getBoundingClientRect();
		  var grp = document.querySelector('#swm [data-juneau-strip-mode="tab"]');
		  p.clip = { selBottom: sel.bottom, panesTop: panes.top, overflowY: getComputedStyle(grp).overflowY };
		  tab('slb').click(); await wait(600); await frames();
		  var th = document.querySelector('#sws .juneau-switcher-thumb'), st = tab('slb');
		  p.thumb = { thumbX: th.getBoundingClientRect().left, tabX: st.getBoundingClientRect().left };
		  tab('b').click(); await frames();
		  } catch (e) { window.__probe.error = String(e && e.stack || e); }
		  var done = document.createElement('i'); done.id = 'done'; document.body.appendChild(done);
		});
		})();
		</script>
		""";

	private static String nestedTable() {
		var t = TableSpec.create("nt").dataMode(TableSpec.DataMode.CLIENT).dataUrl("/api/nt")
			.columns(Column.create("id").label("Id"), Column.create("name").label("Name"))
			.ribbon(RibbonItem.collapseAll())
			.quickStats("qs", QuickStat.of("q1", "Open", 3))
			.detail(RowDetail.create("/api/nt/{id}/detail").title("Row {id}").tab(DetailTab.fields("Info", "name").id("info")));
		var card = new LinkedHashMap<String,Object>();
		card.put("id", "nt");
		card.put("type", "datatables");
		card.put("table", t.toCardBody());
		return Json.of(card);
	}

	private static String leaf(String id) {
		return "{\"id\":\"" + id + "\",\"type\":\"leaf\"}";
	}

	private static String switcher(String id, String initial, String... views) {
		return "{\"id\":\"" + id + "\",\"type\":\"switcher\",\"title\":\"" + id + "\"" + (initial == null ? "" : ",\"initialView\":\"" + initial + "\"")
			+ ",\"views\":[" + String.join(",", views) + "]}";
	}

	private static String view(String id, String card) {
		return "{\"id\":\"" + id + "\",\"label\":\"" + id + "\",\"card\":" + card + "}";
	}

	private static String sections(String id, String... cards) {
		var sb = new StringBuilder("{\"id\":\"" + id + "\",\"label\":\"" + id + "\",\"sections\":[");
		for (var i = 0; i < cards.length; i++)
			sb.append(i == 0 ? "" : ",").append("{\"id\":\"s").append(i + 1).append("\",\"label\":\"S").append(i + 1).append("\",\"card\":").append(cards[i]).append('}');
		return sb.append("]}").toString();
	}

	private static final String CHART = "{\"id\":\"c-chart\",\"type\":\"chart\",\"kind\":\"line\",\"x\":{\"field\":\"day\"},\"series\":[{\"field\":\"n\"}],"
		+ "\"data\":[{\"day\":\"Mon\",\"n\":3},{\"day\":\"Tue\",\"n\":5},{\"day\":\"Wed\",\"n\":4}],\"patch\":{\"animation\":false}}";

	private static Map<String,Map<String,Object>> report;

	private static String page(String cards, String script) throws Exception {
		var before = "<script src=\"" + ECHARTS + "\"></script>\n";
		var after = ConsoleBrowserFixture.viewsPack() + LEAF + "<script src=\"" + CHARTS + "\"></script>\n" + ConsoleBrowserFixture.switcherScript() + script;
		return contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + cards + "]}", "", before, after);
	}

	private static String main() {
		return switcher("sw1", null, view("chart", CHART), view("data", leaf("d1")), sections("multi", leaf("m1"), leaf("m2")));
	}

	private static String layoutCards() {
		return leaf("top1") + "," + leaf("top2") + ","
			+ switcher("swm", null, sections("multi", leaf("m1"), leaf("m2")))
			+ "," + "{\"id\":\"sws\",\"type\":\"switcher\",\"style\":\"slider\",\"views\":["
			+ "{\"id\":\"sla\",\"label\":\"A\",\"card\":" + leaf("sa1") + "},{\"id\":\"slb\",\"label\":\"B\",\"card\":" + leaf("sb1") + "}]}"
			+ "," + switcher("swn", null, view("a", leaf("na1")), view("b", nestedTable()));
	}

	@BeforeAll
	static void runPages() throws Exception {
		var kbd = switcher("sw1", "mv", sections("mv", leaf("m1"), leaf("m2")), view("other", leaf("o1")));
		var two = switcher("swa", null, view("a1", leaf("x1")), view("a2", leaf("x2")), view("a3", leaf("x3")))
			+ "," + switcher("swb", null, view("b1", leaf("y1")), view("b2", leaf("y2")), view("b3", leaf("y3")));
		var seededView = Map.of(VIEW_PREF, "\"data\"");
		var seededSection = Map.of(VIEW_PREF, "\"multi\"", SECTION_PREF, "\"s2\"");
		var urlBeatsPrefs = Map.of(VIEW_PREF, "\"multi\"");
		report = ConsoleBrowserFixture.create("switcher")
			.asset(CHARTS, resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE), "text/javascript")
			.asset(ECHARTS, resource(ECHARTS_RESOURCE), "text/javascript")
			.page("main", ORIGIN + "/main", page(main(), MAIN_SCRIPT))
			.waitFor("#done")
			.page("kbd", ORIGIN + "/kbd", page(kbd, KBD_SCRIPT))
			.waitFor("#done")
			.eval("document.querySelector('#sw1 .juneau-switcher-selector [aria-selected=\"true\"]').focus();" + rec("k0"))
			.press("ArrowRight")
			.eval(rec("k1"))
			.press("Tab")
			.eval(rec("k2"))
			.eval("JuneauConsole.cardApi('sw1').select('mv'); document.querySelector('#sw1 [data-juneau-strip-mode=\"tab\"] [data-tab-id=\"s1\"]').focus();")
			.press("Shift")
			.eval(rec("k3"))
			.asset("/api/nt", "[{\"id\":\"p1\",\"name\":\"Rex\"}]", "application/json")
			.asset("/api/nt/p1/detail", "{\"contractVersion\":\"1\",\"fields\":{\"name\":\"Rex\"}}", "application/json")
			.page("layout", ORIGIN + "/layout", page(layoutCards(), LAYOUT_SCRIPT))
			.waitFor("#done")
			.waitFor("table[data-juneau-view=\"nt\"] tbody td.sorting_1")
			.eval("var host = document.querySelector('.juneau-switcher-view [data-juneau-in-switcher] table[data-juneau-view=\"nt\"]');"
				+ "window.__probe.inSwitcher = !!host;"
				+ "window.__probe.statStrip = !!document.querySelector('[data-juneau-stat=\"q1\"]');"
				+ "window.__probe.ribbon = !!document.querySelector('[data-juneau-ribbon-id=collapseAll]');"
				+ "document.querySelector('table[data-juneau-view=\"nt\"] .juneau-view-detail-toggle').click();")
			.eval("(async function () { await new Promise(function (r) { setTimeout(r, 1500); });"
				+ "window.__probe.detailPanels = document.querySelectorAll('.juneau-view-detail-panel').length;"
				+ "})()")
			.page("two", ORIGIN + "/two", page(two, TWO_SCRIPT))
			.waitFor("#done")
			.page("prefView", ORIGIN + "/prefview", page(main(), PROBE_SCRIPT))
			.storage(seededView)
			.waitFor("#done")
			.page("prefSection", ORIGIN + "/prefsection", page(main(), PROBE_SCRIPT))
			.storage(seededSection)
			.waitFor("#done")
			.page("sharedUrl", ORIGIN + "/shared?state=" + URLEncoder.encode("sw(sw1:data)", StandardCharsets.UTF_8), page(main(), PROBE_SCRIPT))
			.storage(urlBeatsPrefs)
			.waitFor("#done")
			.run();
	}

	private static Map<String,Object> p(String name) {
		var r = report.get(name);
		assertNoShellErrors(r);
		return probe(r);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> m(Object o) {
		return (Map<String,Object>)o;
	}

	@Test void a01_chartRedrawsAfterSwitchBack() {
		var p = p("main");
		assertEquals("chart", p.get("initial"), p::toString);
		for (var k : List.of("chart0", "chart1")) {
			var c = m(p.get(k));
			assertTrue(((Number)c.get("w")).doubleValue() > 0, p::toString);
			assertTrue(((Number)c.get("cw")).intValue() > 0, p::toString);
			assertEquals(3, ((Number)c.get("count")).intValue(), p::toString);
		}
	}

	@Test void b01_reloadRestoresViewFromPreferences() {
		assertEquals("\"data\"", p("main").get("prefData"));
		assertEquals("data", p("prefView").get("active"));
	}

	@Test void b02_sharedUrlBeatsPreferences() {
		assertEquals("data", p("sharedUrl").get("active"));
	}

	@Test void b03_selectingWritesTheAddressAndTheDefaultViewRemovesIt() {
		var p = p("main");
		assertEquals("", p.get("addrStart"), p::toString);
		assertTrue(String.valueOf(p.get("addrData")).contains("sw(sw1:data)"), p::toString);
		assertFalse(String.valueOf(p.get("addrDefault")).contains("sw("), p::toString);
	}

	@Test void b04_tablelessPageWritesTheAddress() {
		assertTrue(String.valueOf(p("main").get("addrMulti")).contains("sw(sw1:multi"), p("main")::toString);
	}

	@Test void c01_multiSectionShowsSubtabsAndKeepsTheSectionChoice() {
		var p = p("main");
		assertEquals(1, ((Number)p.get("subStrips")).intValue(), p::toString);
		assertTrue(String.valueOf(p.get("addrSection")).contains("sw(sw1:multi.s2)"), p::toString);
		assertEquals("\"s2\"", p.get("prefSection"), p::toString);
		var r = p("prefSection");
		assertEquals("multi", r.get("active"), r::toString);
		assertEquals("s2", r.get("section"), r::toString);
	}

	@Test void d01_keyboardMovesBetweenViewTabsAndTabReachesTheDisplay() {
		var p = p("kbd");
		assertEquals("mv", m(p.get("k0")).get("tab"), p::toString);
		assertEquals("other", m(p.get("k1")).get("tab"), p::toString);
		assertEquals("-3px", m(p.get("k3")).get("offset"), p::toString);
		assertEquals("sw1-display", m(p.get("k2")).get("id"), p::toString);
		assertEquals("0", m(p.get("k2")).get("tabindex"), p::toString);
	}

	@Test void d02_displayIsLabelledByTheSelectedTab() {
		var aria = (List<Map<String,Object>>)p("main").get("aria");
		assertEquals(4, aria.size());
		for (var a : aria)
			assertEquals(a.get("selected"), a.get("labelled"), aria::toString);
	}

	@Test void e01_twoSwitchersShareOneDirective() {
		var p = p("two");
		assertEquals(List.of("a2", "b3"), p.get("active"), p::toString);
		assertTrue(String.valueOf(p.get("addr")).contains("sw(swa:a2~swb:b3)"), p::toString);
		assertTrue(String.valueOf(p.get("addrAfterDefault")).contains("sw(swb:b3)"), p::toString);
	}

	@Test void f01_sectionStripUsesTheFolderTabLook() {
		var look = m(p("main").get("look"));
		// The shipped selected folder tab is semibold (600); the stylesheet is the source of truth.
		assertEquals("600", look.get("selWeight"), look::toString);
		assertEquals("1px", look.get("selBorderTopWidth"), look::toString);
		assertTrue(String.valueOf(look.get("selShadow")).contains(String.valueOf(look.get("accent"))), look::toString);
		assertNotEquals(look.get("selBg"), look.get("unsBg"), look::toString);
		assertEquals(look.get("panes"), look.get("selBorderTop"), look::toString);
	}

	@Test void slider_thumbMovesWithSelection() {
		var t = m(p("layout").get("thumb"));
		assertEquals(((Number)t.get("tabX")).doubleValue(), ((Number)t.get("thumbX")).doubleValue(), 1.0, t::toString);
	}

	@Test void tabs_nestedHostIsBorderless() {
		var n = m(p("layout").get("nested"));
		assertEquals(true, n.get("attr"), n::toString);
		assertEquals("none", n.get("shadow"), n::toString);
		assertEquals("0px 0px", n.get("padding"), n::toString);
		assertEquals("rgba(0, 0, 0, 0)", n.get("bg"), n::toString);
	}

	@Test void selectedTabIsNotClipped() {
		var c = m(p("layout").get("clip"));
		assertTrue(((Number)c.get("selBottom")).doubleValue() >= ((Number)c.get("panesTop")).doubleValue() + 1, c::toString);
		assertNotEquals("hidden", c.get("overflowY"), c::toString);
	}

	@Test void topLevelCards_gapEqualsToken() {
		var g = m(p("layout").get("gap"));
		assertEquals(((Number)g.get("token")).doubleValue(), ((Number)g.get("actual")).doubleValue(), 1.0, g::toString);
	}

	@Test void nestedDatatablesKeepsRowDetailRibbonQuickStats() {
		var p = p("layout");
		assertEquals(true, p.get("statStrip"), p::toString);
		assertEquals(true, p.get("ribbon"), p::toString);
		assertEquals(true, p.get("inSwitcher"), p::toString);
		assertTrue(((Number)p.get("detailPanels")).intValue() >= 1, p::toString);
	}
}
