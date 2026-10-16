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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * {@code juneau-charts.js} in a real browser against the real ECharts WebJar: a src-backed line chart that is
 * accessible and themed, the data-table toggle, a click that publishes and navigates, a failing src, a page without
 * ECharts (E-JS-61), resize, the theme-changed re-theme in place, and typed-array keyed columns from the DataRegistry (C3).
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views -am test` from the repo root")
class ChartCard_BrowserTest extends TestBase {

	private static final String CHARTS = "/juneau-console/juneau-charts.js";
	private static final String ECHARTS = "/webjars/echarts/6.1.0/dist/echarts.min.js";
	private static final String ECHARTS_RESOURCE = "/META-INF/resources" + ECHARTS;
	// A page-level override (html:root, to match the theme's specificity) of the first series colour, different from the glue's built-in fallback #1b6ac9.
	private static final String TOKEN_OVERRIDE = "<style>html:root { --jc-chart-1: #123456; }</style>\n";
	private static final String AFTER = "<script src=\"" + CHARTS + "\"></script>";

	// The page script: waits for each step, records it on window.__probe, then adds #done for the fixture to wait on.
	private static final String STEPS = """
		<script>
		(function () {
		  var p = window.__probe = {
		    clicks: [],
		    get expanded() { var b = document.querySelector('#trend .jc-chart-toggle'); return b ? b.getAttribute('aria-expanded') : null; },
		    get tableRows() { return document.querySelectorAll('#trend .jc-chart-table tbody tr').length; },
		    get tableVisible() { var t = document.querySelector('#trend .jc-chart-table-box'); return !!t && !t.hidden; },
		    get hash() { return location.hash; }
		  }, bus = window.JuneauViews.bus;
		  bus.subscribe('app.picked', function (v) { p.clicks.push({ card: v.card, name: v.name, row: v.row }); }, { retained: false });
		  function until(test, ms) {
		    var end = Date.now() + (ms || 5000);
		    return new Promise(function (res) {
		      (function poll() { var v = test(); if (v || Date.now() > end) res(v); else requestAnimationFrame(poll); })();
		    });
		  }
		  function chartOf(id) { var a = window.JuneauConsole.cardApi(id); return a && a.chart(); }
		  document.addEventListener('DOMContentLoaded', async function () {
		    var c = await until(function () { return chartOf('trend') && document.querySelector('#trend canvas') && chartOf('trend'); });
		    var plot = document.querySelector('#trend .jc-chart-plot'), cap = plot && document.getElementById(plot.getAttribute('aria-labelledby'));
		    p.canvas = !!document.querySelector('#trend canvas');
		    p.role = plot && plot.getAttribute('role');
		    p.aria = cap ? cap.textContent : null;
		    p.color0 = c ? c.getOption().color[0] : null;
		    p.token = getComputedStyle(document.documentElement).getPropertyValue('--jc-chart-1').trim();
		    var w0 = c.getWidth();
		    document.getElementById('trend').style.width = '300px';
		    await until(function () { return c.getWidth() !== w0; }, 2000);
		    p.resized = c.getWidth() < w0;
		    var z = await until(function () { return chartOf('zl'); });
		    function view(i) { var o = i.getOption(), dz = o.dataZoom && o.dataZoom[0], lg = o.legend && o.legend[0];
		      return { start: dz ? dz.start : null, end: dz ? dz.end : null, delivered: lg && lg.selected ? lg.selected.Delivered : null }; }
		    z.dispatchAction({ type: 'dataZoom', start: 20, end: 60 });
		    z.dispatchAction({ type: 'legendUnSelect', name: 'Delivered' });
		    p.viewBefore = view(z);
		    document.documentElement.style.setProperty('--jc-chart-1', '#654321');
		    document.dispatchEvent(new CustomEvent('juneau:theme-changed'));
		    await until(function () { var n = chartOf('trend'); return n && n.getOption().color[0] === '#654321'; }, 2000);
		    await until(function () { var n = chartOf('zl'); return n && n.getOption().color[0] === '#654321'; }, 2000);
		    p.viewAfter = Object.assign(view(chartOf('zl')), { same: chartOf('zl') === z, color0: chartOf('zl').getOption().color[0] });
		    var c2 = chartOf('trend');
		    p.rethemed = { same: c2 === c && window.echarts.getInstanceByDom(document.querySelector('#trend .jc-chart-canvas')) === c,
		      disposed: !!c.isDisposed(), color0: c2 ? c2.getOption().color[0] : null };
		    bus.declare('data:t', { retain: true, by: 'page' });
		    var data = window.JuneauViews.data, own = bus.owner('page');
		    var dims = [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }];
		    var v = data.put('t', { dims: dims, rowCount: 3, truncated: false,
		      columns: { day: { codes: Int32Array.of(0, 1, 2), dict: ['Mon', 'Tue', 'Wed'] }, placed: Float64Array.of(3, 5, 4) } });
		    own.publish('data:t', { schemaVersion: 1, source: 't', version: v, status: 'ready', progress: { loaded: 3 },
		      rowCount: 3, truncated: false, dims: dims });
		    var t = await until(function () { return chartOf('typed'); });
		    var d = t && t.getModel().getSeriesByIndex(0).getData();
		    p.typed = d ? { count: d.count(), second: d.get('placed', 1), dayCode: d.get('day', 1),
		      days: t.getModel().getComponent('xAxis', 0).getOrdinalMeta().categories.map(String) } : null;
		    var done = document.createElement('i');
		    done.id = 'done';
		    document.body.appendChild(done);
		  });
		})();
		</script>
		""";

	// The treemap page script: lays out the tiles from the drawn tree, then drives wheel, click and key input on the canvas.
	private static final String TREEMAP_STEPS = """
		<script>
		(function () {
		  var p = window.__probe = { clicks: [], raw: [], highlights: [], errors: [] }, bus = window.JuneauViews.bus;
		  bus.subscribe('app.picked', function (v) { p.clicks.push({ card: v.card, name: v.name, row: v.row, meta: v.meta }); }, { retained: false });
		  window.addEventListener('error', function (e) { p.errors.push(String(e.message)); });
		  function until(test, ms) {
		    var end = Date.now() + (ms || 5000);
		    return new Promise(function (res) {
		      (function poll() { var v = test(); if (v || Date.now() > end) res(v); else requestAnimationFrame(poll); })();
		    });
		  }
		  function tree(c) { return c.getModel().getSeriesByIndex(0).getData().tree; }
		  function count(c) { return c.getModel().getSeriesByIndex(0).getData().count(); }
		  // Every node in dataIndex order, with the pixel box ECharts laid it out in.
		  // The tile's rectangle in canvas pixels, from the drawn element.
		  function boxOf(c, i) {
		    var el = c.getModel().getSeriesByIndex(0).getData().getItemGraphicEl(i);
		    if (!el) return null;
		    var r = el.getBoundingRect().clone(), m = el.getComputedTransform();
		    if (m) r.applyTransform(m);
		    return { x: r.x, y: r.y, w: r.width, h: r.height };
		  }
		  function nodes(c) {
		    var t = tree(c), out = [];
		    for (var i = 0; i < count(c); i++) {
		      var n = t.getNodeByDataIndex(i), l = n && n.getLayout();
		      if (n && l) out.push({ i: i, name: n.name, leaf: n.children.length === 0, x: l.x, y: l.y, w: l.width, h: l.height, box: boxOf(c, i) });
		    }
		    return out;
		  }
		  function fire(c, type, x, y, init) {
		    var el = c.getZr().painter.getViewportRoot(), r = el.getBoundingClientRect();
		    var o = Object.assign({ bubbles: true, cancelable: true, clientX: r.left + x, clientY: r.top + y }, init || {});
		    el.dispatchEvent(type === 'wheel' ? new WheelEvent(type, o) : new MouseEvent(type, o));
		  }
		  function click(c, x, y) { fire(c, 'mousemove', x, y); fire(c, 'mousedown', x, y); fire(c, 'mouseup', x, y); fire(c, 'click', x, y); }
		  function key(k) {
		    document.querySelector('#tm .jc-chart-plot').dispatchEvent(new KeyboardEvent('keydown', { key: k, bubbles: true, cancelable: true }));
		  }
		  function zoomOf(c) { var s = c.getModel().getSeriesByIndex(0), l = s.getData().tree.root.getLayout(); return l ? l.width / s.layoutInfo.width : null; }
		  function view(c) { var v = c.getModel().getSeriesByIndex(0).getViewRoot(); return v ? { name: v.name, i: v.dataIndex } : null; }
		  document.addEventListener('DOMContentLoaded', async function () { try {
		    var api = window.JuneauConsole.cardApi;
		    var c = await until(function () { var a = api('tm'), ch = a && a.chart(); return ch && document.querySelector('#tm canvas') && nodes(ch).length > 6 && ch; });
		    p.canvas = !!document.querySelector('#tm canvas');
		    p.type = c.getOption().series[0].type;
		    var all = nodes(c);
		    p.nodes = all.map(function (n) { return n.name; });
		    p.leaves = all.filter(function (n) { return n.leaf; }).map(function (n) { return n.name; });
		    p.blank = (function () {
		      var cv = document.querySelector('#tm canvas'), d = cv.getContext('2d').getImageData(0, 0, cv.width, cv.height).data, seen = {}, k = 0;
		      for (var i = 0; i < d.length && k < 3; i += 4 * 97) { var key = d[i] + ',' + d[i + 1] + ',' + d[i + 2] + ',' + d[i + 3]; if (!seen[key]) { seen[key] = 1; k++; } }
		      return k < 3;
		    })();
		    // b03: a leaf click publishes; a group header click publishes nothing and does not zoom.
		    all = nodes(c);
		    c.on('click', function (e) { p.raw.push({ name: e.name, i: e.dataIndex, group: !!(e.data && e.data.children && e.data.children.length) }); });
		    var leaf = all.find(function (n) { return n.name === 'h6'; });
		    click(c, leaf.box.x + leaf.box.w / 2, leaf.box.y + leaf.box.h / 2);
		    await new Promise(function (r) { setTimeout(r, 300); });
		    p.afterLeaf = p.clicks.length;
		    var group = all.filter(function (n) { return !n.leaf && n.name === 'c2'; }).pop();
		    var before = view(c);
		    click(c, group.box.x + group.box.w / 2, group.box.y + 3);
		    await new Promise(function (r) { setTimeout(r, 300); });
		    p.afterGroup = p.clicks.length;
		    p.groupView = { before: before, after: view(c) };
		    // b02 wheel zoom: a leaf's box grows.
		    var h1 = all.find(function (n) { return n.name === 'h1'; });
		    var s0 = zoomOf(c);
		    fire(c, 'mousemove', h1.box.x + h1.box.w / 2, h1.box.y + h1.box.h / 2);
		    fire(c, 'wheel', h1.box.x + h1.box.w / 2, h1.box.y + h1.box.h / 2, { deltaY: 300 });
		    await until(function () { return Math.abs(zoomOf(c) - s0) > 0.01; }, 3000);
		    p.zoom = { before: s0, after: zoomOf(c) };
		    // b04 keys.
		    p.zoomTargets = [];
		    c.on('treemapzoomtonode', function (e) { p.zoomTargets.push(e.targetNodeId); });
		    c.on('highlight', function (e) { (e.batch || [e]).forEach(function (b) { var n = tree(c).getNodeByDataIndex(b.dataIndex); p.highlights.push(n ? n.name : null); }); });
		    document.querySelector('#tm .jc-chart-plot').focus();
		    key('ArrowRight'); key('ArrowRight');
		    await new Promise(function (r) { setTimeout(r, 200); });
		    key('+');
		    await new Promise(function (r) { setTimeout(r, 400); });
		    p.zoomedTo = view(c);
		    p.zoomedBy = zoomOf(c);
		    p.firstZoomTargets = p.zoomTargets.slice();
		    key('-');
		    await new Promise(function (r) { setTimeout(r, 400); });
		    p.zoomedOut = view(c);
		    key('+'); key('+');
		    await new Promise(function (r) { setTimeout(r, 300); });
		    key('0');
		    await new Promise(function (r) { setTimeout(r, 400); });
		    p.reset = view(c);
		    } catch (e) { p.failed = String(e && e.stack || e); }
		    var done = document.createElement('i');
		    done.id = 'done';
		    document.body.appendChild(done);
		  });
		})();
		</script>
		""";


	// The action page script: a fake network (fetch and EventSource) answers the endpoints the cards' actions name, and the
	// script clicks the real canvases and records what went out and what the card showed.
	private static final String ACTION_STEPS = """
		<script>
		(function () {
		  var p = window.__probe = { posts: [], gets: [], sources: [], cancels: [], failed: null };
		  document.body.setAttribute('data-juneau-csrf', 'tok-7');
		  var realFetch = window.fetch;
		  function json(body, status) { return Promise.resolve(new Response(JSON.stringify(body), { status: status || 200, headers: { 'Content-Type': 'application/json' } })); }
		  window.fetch = function (url, init) {
		    var u = String(url), m = (init && init.method) || 'GET', h = (init && init.headers) || {};
		    var tok = h['X-Csrf-Token'] || null;
		    if (m === 'GET' && u.indexOf('/t/form') === 0) { p.gets.push(u); return json({ contractVersion: '2', title: 'Edit', form: { contractVersion: '2', fields: [{ name: 'note', type: 'text', label: 'Note' }] } }); }
		    if (m !== 'GET') {
		      p.posts.push({ url: u, method: m, csrf: tok });
		      if (u === '/t/requeue/a') return tok ? json({ outcome: 'success', message: 'requeued' }) : json({}, 403);
		      if (u.indexOf('/t/item/') === 0) return json({ outcome: 'success', message: 'saved' });
		      if (u === '/t/job') return json({ jobId: 'job-1', streamUrl: '/jobs/job-1/stream', cancelUrl: '/jobs/job-1/cancel' });
		      if (u === '/jobs/job-1/cancel') { p.cancels.push(tok); return json({}); }
		    }
		    return realFetch.apply(window, arguments);
		  };
		  function ES(url) { this.url = url; this.l = {}; this.closed = false; p.sources.push(this); }
		  ES.prototype.addEventListener = function (t, f) { (this.l[t] = this.l[t] || []).push(f); };
		  ES.prototype.close = function () { this.closed = true; };
		  ES.prototype.fire = function (t, d) { (this.l[t] || []).forEach(function (f) { f({ data: d }); }); };
		  window.EventSource = ES;
		  function until(test, ms) {
		    var end = Date.now() + (ms || 5000);
		    return new Promise(function (res) {
		      (function poll() { var v = test(); if (v || Date.now() > end) res(v); else requestAnimationFrame(poll); })();
		    });
		  }
		  function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
		  function q(sel) { return document.querySelector(sel); }
		  function plot(id) { return q('#' + id + ' .jc-chart-plot'); }
		  function banner(id) { var b = q('#' + id + ' [data-testid="chart-action-banner"]'); return b ? b.textContent : null; }
		  function chartOf(id) { var a = window.JuneauConsole.cardApi(id); return a && a.chart(); }
		  function clickCenter(id) {
		    var c = chartOf(id), el = c.getZr().painter.getViewportRoot(), r = el.getBoundingClientRect();
		    var o = { bubbles: true, cancelable: true, clientX: r.left + r.width / 2, clientY: r.top + r.height / 2 };
		    ['mousemove', 'mousedown', 'mouseup', 'click'].forEach(function (t) { el.dispatchEvent(new MouseEvent(t, o)); });
		  }
		  document.addEventListener('DOMContentLoaded', async function () { try {
		    var ids = ['direct', 'dlg', 'job'];
		    for (var i = 0; i < ids.length; i++) await until(function () { return chartOf(ids[i]) && q('#' + ids[i] + ' canvas'); });
		    // a11y: an action plot is an operable group with a hint, and a keyboard walk is announced in the live region
		    var pl = plot('direct'), hintEl = pl && document.getElementById(pl.getAttribute('aria-describedby'));
		    p.a11y = { role: pl && pl.getAttribute('role'), hint: hintEl ? hintEl.textContent : null };
		    pl.focus();
		    pl.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true, cancelable: true }));
		    p.a11y.live = q('#direct .jc-chart-live') && q('#direct .jc-chart-live').textContent;
		    p.a11y.liveAttr = q('#direct .jc-chart-live') && q('#direct .jc-chart-live').getAttribute('aria-live');
		    pl.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
		    p.a11y.liveAfterEscape = q('#direct .jc-chart-live').textContent;
		    // direct
		    clickCenter('direct');
		    await until(function () { return banner('direct'); });
		    p.direct = { banner: banner('direct'), posts: p.posts.slice(), busy: plot('direct').getAttribute('aria-busy') };
		    // dialog
		    clickCenter('dlg');
		    var ok = await until(function () { return q('[data-testid="dialog-confirm"]') || q('.juneau-view-dialog-confirm'); });
		    p.dialog = { opened: !!ok, gets: p.gets.slice(), backdrops: document.querySelectorAll('.juneau-view-dialog-backdrop').length };
		    if (ok) ok.click();
		    await until(function () { return banner('dlg') && !q('.juneau-view-dialog-backdrop'); });
		    p.dialog.banner = banner('dlg');
		    p.dialog.put = p.posts.filter(function (x) { return x.method === 'PUT'; });
		    p.dialog.backdropsAfter = document.querySelectorAll('.juneau-view-dialog-backdrop').length;
		    // job
		    clickCenter('job');
		    await until(function () { return p.sources.length === 1 && q('#job [data-testid="job-progress"]'); });
		    var before = p.posts.length;
		    clickCenter('job');
		    await sleep(200);
		    p.job = { progressInBanner: !!q('#job [data-testid="chart-action-banner"] [data-testid="job-progress"]'), busy: plot('job').getAttribute('aria-busy'), cardBusy: q('#job').getAttribute('aria-busy'),
		      secondClickPosts: p.posts.length - before, sources: p.sources.length, cancelLabel: q('#job .juneau-view-job-cancel') && q('#job .juneau-view-job-cancel').getAttribute('aria-label') };
		    p.sources[0].fire('progress', 'half way');
		    p.job.progressText = q('#job .juneau-view-job-progress-msg').textContent;
		    q('#job .juneau-view-job-cancel').click();
		    await until(function () { return p.cancels.length; });
		    p.job.cancels = p.cancels.slice();
		    p.sources[0].fire('result', JSON.stringify({ outcome: 'cancelled', message: 'stopped' }));
		    await until(function () { return !plot('job').getAttribute('aria-busy'); });
		    p.job.finalBanner = banner('job');
		    p.job.progressGone = !q('#job [data-testid="job-progress"]');
		    p.job.busyAfter = plot('job').getAttribute('aria-busy');
		    p.job.closed = p.sources[0].closed;
		  } catch (e) { p.failed = String(e && e.stack || e); }
		    var done = document.createElement('i');
		    done.id = 'done';
		    document.body.appendChild(done);
		  });
		})();
		</script>
		""";

	private static Map<String,Map<String,Object>> report;

	// A one-bar card that fills its canvas and runs `onClick` (a JSON object) when clicked.
	private static String bar(String id, String onClick, String actions) {
		return "{\"id\":\"" + id + "\",\"type\":\"chart\",\"title\":\"" + id + "\",\"helper\":{\"kind\":\"bar\",\"x\":\"k\",\"series\":[{\"field\":\"v\"}]},"
			+ "\"data\":[{\"k\":\"a\",\"v\":4}],\"onClick\":" + onClick + ",\"on\":{\"click\":\"app.act-" + id + "\"},"
			+ (actions == null ? "" : "\"actions\":" + actions + ",")
			+ "\"publishes\":[{\"topic\":\"app.act-" + id + "\",\"retain\":false}],"
			+ "\"patch\":{\"animation\":false,\"grid\":{\"top\":0,\"bottom\":0,\"left\":0,\"right\":0},\"legend\":{\"show\":false},"
			+ "\"xAxis\":{\"show\":false},\"yAxis\":{\"show\":false,\"max\":4},\"series\":[{\"id\":\"v\",\"barWidth\":\"90%\"}]}}";
	}

	private static String scripts(String... resources) throws Exception {
		var sb = new StringBuilder();
		for (var r : resources)
			sb.append("<script>\n").append(resource(r)).append("\n</script>\n");
		return sb.toString();
	}

	@BeforeAll
	static void setup() throws Exception {
		var trend = "{\"id\":\"trend\",\"type\":\"chart\",\"title\":\"Orders\",\"kind\":\"line\",\"src\":\"/stats/daily\","
			+ "\"x\":{\"field\":\"day\",\"label\":\"Day\"},\"series\":[{\"field\":\"placed\",\"label\":\"Placed\"}]}";
		// One bar that fills the canvas, with no grow-in animation, so the fixture's centre click lands on it.
		var pick = "{\"id\":\"pick\",\"type\":\"chart\",\"title\":\"Pick\",\"helper\":{\"kind\":\"bar\",\"x\":\"k\",\"series\":[{\"field\":\"v\"}]},"
			+ "\"data\":[{\"k\":\"a\",\"v\":4}],\"onClick\":{\"href\":\"#picked-{k}\"},\"on\":{\"click\":\"app.picked\"},"
			+ "\"publishes\":[{\"topic\":\"app.picked\",\"retain\":false}],"
			+ "\"patch\":{\"animation\":false,\"grid\":{\"top\":0,\"bottom\":0,\"left\":0,\"right\":0},\"legend\":{\"show\":false},"
			+ "\"xAxis\":{\"show\":false},\"yAxis\":{\"show\":false,\"max\":4},\"series\":[{\"id\":\"v\",\"barWidth\":\"90%\"}]}}";
		// Two series, zoomable, so a zoom range and a legend deselect can be set before a theme change.
		var zl = "{\"id\":\"zl\",\"type\":\"chart\",\"title\":\"Zoom\",\"kind\":\"line\",\"x\":{\"field\":\"day\",\"label\":\"Day\"},"
			+ "\"series\":[{\"field\":\"placed\",\"label\":\"Placed\"},{\"field\":\"delivered\",\"label\":\"Delivered\"}],"
			+ "\"data\":[{\"day\":\"Mon\",\"placed\":3,\"delivered\":1},{\"day\":\"Tue\",\"placed\":5,\"delivered\":2},"
			+ "{\"day\":\"Wed\",\"placed\":4,\"delivered\":4},{\"day\":\"Thu\",\"placed\":6,\"delivered\":3},{\"day\":\"Fri\",\"placed\":2,\"delivered\":2}],"
			+ "\"patch\":{\"animation\":false,\"dataZoom\":[{\"type\":\"inside\"}]}}";
		var typed = "{\"id\":\"typed\",\"type\":\"chart\",\"title\":\"Typed\",\"source\":\"data:t\",\"subscribes\":[{\"topic\":\"data:t\",\"as\":\"data\"}],"
			+ "\"helper\":{\"kind\":\"line\",\"x\":\"day\",\"series\":[{\"field\":\"placed\"}]}}";
		// A string helper.x is the field name: the glue's fieldOf accepts a string or {field}.
		var topics = "\"topics\":[{\"topic\":\"app.picked\",\"retain\":false},{\"topic\":\"data:t\",\"retain\":true,\"publisher\":\"script\"}]";
		var broken = "{\"id\":\"broken\",\"type\":\"chart\",\"kind\":\"bar\",\"src\":\"/stats/missing\",\"x\":\"k\",\"series\":[\"v\"]}";
		var bare = "{\"id\":\"bare\",\"type\":\"chart\",\"kind\":\"line\",\"x\":\"k\",\"series\":[\"v\"],\"data\":[{\"k\":\"a\",\"v\":1}]}";
		var rows = "[{\"region\":\"eu\",\"cluster\":\"c1\",\"host\":\"h1\",\"health\":\"ok\",\"cpu\":4},"
			+ "{\"region\":\"eu\",\"cluster\":\"c1\",\"host\":\"h2\",\"health\":\"down\",\"cpu\":5},"
			+ "{\"region\":\"eu\",\"cluster\":\"c2\",\"host\":\"h3\",\"health\":\"ok\",\"cpu\":40},"
			+ "{\"region\":\"us\",\"cluster\":\"c1\",\"host\":\"h4\",\"health\":\"ok\",\"cpu\":50},"
			+ "{\"region\":\"us\",\"cluster\":\"c1\",\"host\":\"h5\",\"health\":\"down\",\"cpu\":30},"
			+ "{\"region\":\"us\",\"cluster\":\"c2\",\"host\":\"h6\",\"health\":\"ok\",\"cpu\":60}]";
		var tm = "{\"id\":\"tm\",\"type\":\"chart\",\"title\":\"Fleet\",\"helper\":{\"kind\":\"treemap\",\"groupBy\":[\"region\",\"cluster\"],"
			+ "\"label\":\"host\",\"size\":\"cpu\",\"color\":{\"field\":\"health\",\"legend\":[{\"value\":\"ok\",\"label\":\"Healthy\",\"tone\":\"ok\"},"
			+ "{\"value\":\"down\",\"label\":\"Down\",\"tone\":\"danger\"}]},\"tileFields\":[\"host\",\"health\",\"cpu\"]},"
			+ "\"data\":" + rows + ",\"on\":{\"click\":\"app.picked\"},\"publishes\":[{\"topic\":\"app.picked\",\"retain\":false}],"
			+ "\"patch\":{\"animation\":false}}";
		var directCard = bar("direct", "{\"action\":{\"id\":\"requeue\",\"label\":\"Requeue\",\"endpoint\":\"/t/requeue/{k}\",\"method\":\"POST\"}}", null);
		var dlgCard = bar("dlg", "{\"action\":{\"id\":\"edit\",\"label\":\"Edit\",\"endpoint\":\"/t/item/{k}\",\"method\":\"PUT\",\"present\":\"dialog\",\"form\":\"/t/form?id={k}\"}}", null);
		var jobCard = bar("job", "{\"action\":{\"id\":\"start\",\"label\":\"Start job\",\"endpoint\":\"/t/job\",\"method\":\"POST\"}}", null);
		var actTopics = "\"topics\":[{\"topic\":\"app.act-direct\",\"retain\":false},{\"topic\":\"app.act-dlg\",\"retain\":false},{\"topic\":\"app.act-job\",\"retain\":false}]";
		var before = scripts(ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.DATA_JS_RESOURCE) + "<script src=\"" + ECHARTS + "\"></script>\n";
		var actBefore = ConsoleBrowserFixture.viewsPack() + scripts(ViewsMixin.DATA_JS_RESOURCE) + "<script src=\"" + ECHARTS + "\"></script>\n";
		report = ConsoleBrowserFixture.create("chart-card")
			.asset(CHARTS, resource(ConsoleChromeMixin.CHARTS_JS_RESOURCE), "text/javascript")
			.asset(ECHARTS, resource(ECHARTS_RESOURCE), "text/javascript")
			.asset("/stats/daily", "[{\"day\":\"Mon\",\"placed\":3},{\"day\":\"Tue\",\"placed\":5}]", "application/json")
			.page("charts", ORIGIN + "/charts",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\"," + topics + ",\"cards\":[" + trend + "," + pick + "," + typed + "," + zl + "]}", "",
					TOKEN_OVERRIDE + before + STEPS, AFTER),
				"caption", "#trend .jc-chart-caption")
			.waitFor("#done")
			.click("#trend .jc-chart-toggle")
			.click("#pick .jc-chart-canvas")
			.page("treemap", ORIGIN + "/treemap",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\"," + topics + ",\"cards\":[" + tm + "]}", "", before + TREEMAP_STEPS, AFTER),
				"legend", "#tm .jc-chart-caption")
			.waitFor("#done")
			.page("actions", ORIGIN + "/actions",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\"," + actTopics + ",\"cards\":[" + directCard + "," + dlgCard + "," + jobCard + "]}", "",
					actBefore + ACTION_STEPS, AFTER),
				"direct", "#direct .jc-chart-caption")
			.waitFor("#done")
			.page("broken", ORIGIN + "/broken",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + broken + "]}", "", before, AFTER),
				"error", "#broken .jc-card-error")
			.waitFor("#broken .jc-card-error")
			// No echarts script: the 'echarts' pack is missing from the page.
			.page("noEcharts", ORIGIN + "/bare",
				contractPage("{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + bare + "]}", "",
					scripts(ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.DATA_JS_RESOURCE), AFTER),
				"error", "#bare .jc-card-error")
			.waitFor("#bare .jc-card-error")
			.run();
	}

	private static List<String> chartErrors(Map<String,Object> r) {
		return ((List<?>)r.get("consoleErrors")).stream().map(String::valueOf).filter(e -> e.contains("[juneau-charts]")).toList();
	}

	@Test void a01_lineFromSrc_accessibleAndThemed() {
		var r = report.get("charts");
		assertNoShellErrors(r);
		assertEquals(List.of(), chartErrors(r));
		assertEquals(List.of(), r.get("pageErrors"));
		var p = probe(r);
		assertEquals(true, p.get("canvas"));
		assertEquals("img", p.get("role"));
		assertEquals("Line chart, 1 series, 2 points, Mon to Tue", p.get("aria"));
		assertEquals(p.get("aria"), queries(r).get("caption"));
		// The ECharts theme's first colour is the resolved --jc-chart-1 token: the page override, not the glue fallback.
		assertEquals("#123456", p.get("token"));
		assertEquals("#123456", p.get("color0"));
	}

	@Test void a02_tableToggle() {
		var p = probe(report.get("charts"));
		assertEquals("true", p.get("expanded"));
		assertEquals(true, p.get("tableVisible"));
		assertEquals(2, p.get("tableRows"));
	}

	@Test void a03_click_publishesAndNavigates() {
		var p = probe(report.get("charts"));
		assertEquals(List.of(Map.of("card", "pick", "name", "a", "row", Map.of("k", "a", "v", 4))), p.get("clicks"));
		assertEquals("#picked-a", p.get("hash"));
	}

	@Test void a04_failingSrc_paintsCardError() {
		assertEquals("E-JS-62: chart card 'broken' fetch failed: 'HTTP 404'", queries(report.get("broken")).get("error"));
	}

	@Test void a05_noEcharts_isE_JS_61() {
		assertEquals("E-JS-61: chart card 'bare' needs ECharts (window.echarts), which did not load; the 'echarts' toolkit pack serves org.webjars.npm:echarts",
			queries(report.get("noEcharts")).get("error"));
	}

	@Test void a06_resize_followsTheHost() {
		assertEquals(true, probe(report.get("charts")).get("resized"));
	}

	@Test void a07_themeChanged_rethemesTheSameInstance() {
		assertEquals(Map.of("same", true, "disposed", false, "color0", "#654321"), probe(report.get("charts")).get("rethemed"));
	}

	@Test void a07b_themeChanged_keepsZoomAndLegendSelection() {
		var p = probe(report.get("charts"));
		assertEquals(Map.of("start", 20, "end", 60, "delivered", false), p.get("viewBefore"));
		assertEquals(Map.of("start", 20, "end", 60, "delivered", false, "same", true, "color0", "#654321"), p.get("viewAfter"));
	}

	@Test void a08_typedArrayKeyedColumns_draw() {
		assertEquals(Map.of("count", 3, "second", 5, "dayCode", 1, "days", List.of("Mon", "Tue", "Wed")), probe(report.get("charts")).get("typed"));
	}

	@Test void b01_treemap_rendersNestedRectangles() {
		var r = report.get("treemap");
		assertNoShellErrors(r);
		assertEquals(List.of(), chartErrors(r));
		assertEquals(List.of(), r.get("pageErrors"));
		var p = probe(r);
		assertEquals(true, p.get("canvas"));
		assertNull(p.get("failed"));
		assertEquals("treemap", p.get("type"));
		assertEquals(false, p.get("blank"));
		assertEquals(List.of("h1", "h2", "h3", "h4", "h5", "h6"), p.get("leaves"));
		assertTrue(((List<?>)p.get("nodes")).containsAll(List.of("eu", "us", "c1", "c2")), String.valueOf(p.get("nodes")));
	}

	@Test void b02_treemap_wheelZoomsTheTiles() {
		var z = (Map<?,?>)probe(report.get("treemap")).get("zoom");
		assertNotEquals(z.get("before"), z.get("after"), String.valueOf(z));
		assertTrue(Math.abs(((Number)z.get("after")).doubleValue() - ((Number)z.get("before")).doubleValue()) > 0.05, String.valueOf(z));
	}

	@Test void b03_treemap_leafClickPublishes_groupClickDoesNot() {
		var p = probe(report.get("treemap"));
		assertEquals(1, p.get("afterLeaf"));
		var c = (Map<?,?>)((List<?>)p.get("clicks")).get(0);
		assertEquals("tm", c.get("card"));
		assertEquals("h6", c.get("name"));
		assertNull(c.get("row"));
		assertEquals(Map.of("host", "h6", "health", "ok", "cpu", 60), c.get("meta"));
		// The group header click really reached ECharts (a non-leaf node), so the unchanged publish count is the card's doing.
		var raw = (List<?>)p.get("raw");
		assertEquals(2, raw.size(), String.valueOf(raw));
		assertEquals(Map.of("name", "h6", "i", ((Map<?,?>)raw.get(0)).get("i"), "group", false), raw.get(0));
		var g = (Map<?,?>)raw.get(1);
		assertEquals("c2", g.get("name"));
		assertEquals(true, g.get("group"));
		assertEquals(1, p.get("afterGroup"));
		var v = (Map<?,?>)p.get("groupView");
		assertEquals(v.get("before"), v.get("after"));
	}

	@Test void b04_treemap_zoomKeys() {
		var p = probe(report.get("treemap"));
		// Two ArrowRights walk h1 then h2: the highlighted tiles are those leaves, which pins the dataIndex mapping.
		var h = (List<?>)p.get("highlights");
		assertEquals(List.of("h1", "h2"), h.subList(0, 2));
		assertTrue(((Number)p.get("zoomedBy")).doubleValue() > 1.05, "+ zooms to the walked leaf's group: " + p.get("zoomedBy"));
		// The zoom target is the walked leaf's group.
		assertEquals(List.of("eu/c1"), p.get("firstZoomTargets"));
		assertEquals("eu", ((Map<?,?>)p.get("zoomedOut")).get("name"));
		assertEquals(1, ((Number)((Map<?,?>)p.get("reset")).get("i")).intValue());
	}

	private static Map<?,?> actions() {
		var r = report.get("actions");
		assertNull(probe(r).get("failed"));
		return probe(r);
	}

	@Test void c01_barClick_directAction_postCarriesCsrf_outcomeInChartBanner() {
		var r = report.get("actions");
		assertEquals(List.of(), chartErrors(r));
		assertEquals(List.of(), r.get("pageErrors"));
		var d = (Map<?,?>)actions().get("direct");
		var post = (Map<?,?>)((List<?>)d.get("posts")).get(0);
		assertEquals("/t/requeue/a", post.get("url"));
		assertEquals("POST", post.get("method"));
		assertEquals("tok-7", post.get("csrf"));
		assertTrue(String.valueOf(d.get("banner")).contains("requeued"), String.valueOf(d));
	}

	@Test void c02_dialogAction_formFromPoint_putWithToken_noBackdropLeft_outcomeInChartBanner() {
		var d = (Map<?,?>)actions().get("dialog");
		assertEquals(true, d.get("opened"), String.valueOf(d));
		assertEquals(List.of("/t/form?id=a"), d.get("gets"));
		var put = (Map<?,?>)((List<?>)d.get("put")).get(0);
		assertEquals("/t/item/a", put.get("url"));
		assertEquals("tok-7", put.get("csrf"));
		assertEquals(0, ((Number)d.get("backdropsAfter")).intValue());
		assertTrue(String.valueOf(d.get("banner")).contains("saved"), String.valueOf(d));
	}

	@Test void c04_asyncJob_progressInChartBanner_secondClickIgnored_terminalReplacesProgress() {
		var j = (Map<?,?>)actions().get("job");
		assertEquals(true, j.get("progressInBanner"), String.valueOf(j));
		assertEquals("true", j.get("busy"));
		// aria-busy is on the plot, not the card that holds the live progress banner.
		assertNull(j.get("cardBusy"));
		assertEquals(0, ((Number)j.get("secondClickPosts")).intValue());
		assertEquals(1, ((Number)j.get("sources")).intValue());
		assertEquals("half way", j.get("progressText"));
		assertEquals(true, j.get("progressGone"));
		assertNull(j.get("busyAfter"));
		assertEquals(true, j.get("closed"));
	}

	@Test void c06_actionPlot_isAGroupWithHint_keyboardWalkIsAnnounced() {
		var a = (Map<?,?>)actions().get("a11y");
		assertEquals("group", a.get("role"), String.valueOf(a));
		assertTrue(String.valueOf(a.get("hint")).contains("Enter"), String.valueOf(a));
		assertEquals("polite", a.get("liveAttr"));
		assertEquals("k a, v 4", a.get("live"));
		assertEquals("", a.get("liveAfterEscape"));
	}

	@Test void c05_asyncJob_cancel_yieldsCancelled() {
		var j = (Map<?,?>)actions().get("job");
		assertEquals("Cancel Start job", j.get("cancelLabel"));
		assertEquals(List.of("tok-7"), j.get("cancels"));
		assertTrue(String.valueOf(j.get("finalBanner")).toLowerCase().contains("cancelled"), String.valueOf(j));
	}
}
