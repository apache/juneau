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
/*
 * bus-wraps.cjs - always-on Node harness for the JuneauViews.bus wraps in juneau-views.js (probe group, async job).
 *
 *   - probe wrap: probe:<group> is published ONLY on a user-driven selection change (click / arrow key) - never for
 *     the default selection at enhance time, an imperative select(), a no-op re-click, or a status repaint;
 *     the group key is data-juneau-probe-group's value, else the element id, else nothing is published;
 *   - job wrap: publishJobEvent maps start/progress/result onto job:<jobId> payloads, and is a silent no-op with no
 *     bus, a malformed jobId, or a job:<jobId> topic another owner (a bridge) already claimed;
 *   - both wraps leave onSelect and the no-bus behaviour unchanged.
 *
 *   Usage:  node bus-wraps.cjs <juneau-renders.js> <juneau-views.js> <juneau-bus.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in ViewsJs_BusWraps_Test.
 */
'use strict';

const path = require('node:path');
const { makeEnv, loadScripts, loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [rendersJsPath, viewsJsPath, busJsPath] = process.argv.slice(2);
if (!rendersJsPath || !viewsJsPath || !busJsPath) {
	console.error('usage: node bus-wraps.cjs <juneau-renders.js> <juneau-views.js> <juneau-bus.js>');
	process.exit(2);
}

// The bus first, then views, into ONE shared window - the production load order.
const env = makeEnv();
loadScripts([busJsPath], env);
const { I, NS } = loadViews(rendersJsPath, viewsJsPath, env);
const bus = NS && NS.bus;
const out = {
	hasBus: !!bus,
	hasHelpers: typeof I?.probeGroupTopicKey === 'function' && typeof I?.publishJobEvent === 'function'
};
if (!out.hasBus || !out.hasHelpers) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

// history() entries carry no payload, so payloads are captured by a subscriber registered before the publish.
const seen = {};
function watch(topic) {
	seen[topic] = [];
	bus.subscribe(topic, function (p) { seen[topic].push(p); });
}
function probe(e, id, opts) {
	opts = opts || {};
	const el = e.el('button');
	el.className = 'jc-probe jc-probe-ok' + (opts.disabled ? ' is-disabled' : '');
	el.setAttribute('data-juneau-probe', id);
	if (opts.checked) el.setAttribute('aria-checked', 'true');
	if (opts.disabled) el.setAttribute('aria-disabled', 'true');
	return el;
}
function group(e, probes, attrs) {
	const g = e.el('div');
	g.className = 'jc-probe-group';
	g.setAttribute('data-juneau-probe-group', (attrs && attrs.groupAttr) || '');
	if (attrs && attrs.id) g.setAttribute('id', attrs.id);
	probes.forEach(function (p) { g.appendChild(p); });
	e.body.appendChild(g);
	return g;
}
function probeHistory(topic) {
	return bus.history().filter(function (h) { return h.topic === topic; });
}
function anyProbeHistory() {
	return bus.history().filter(function (h) { return String(h.topic).indexOf('probe:') === 0; }).length;
}
const key = function (k) { return { key: k, preventDefault: function () {} }; };

// ---- 1) The default selection publishes nothing (markup aria-checked, then the first-enabled fallback). --------
watch('probe:grp-a');
const a1 = probe(env, 'ok'), a2 = probe(env, 'warn', { checked: true }), a3 = probe(env, 'fail');
const gA = group(env, [a1, a2, a3], { id: 'grp-a' });
let selA = [];
const ctlA = I.enhanceProbeGroup(gA, { onSelect: function (id) { selA.push(id); } });
out.init_checked_historyCount = probeHistory('probe:grp-a').length;
out.init_checked_get = bus.get('probe:grp-a') === undefined ? null : bus.get('probe:grp-a');
out.init_checked_subscriberCalls = seen['probe:grp-a'].length;
out.init_checked_onSelect = selA.length;

const b1 = probe(env, 'x'), b2 = probe(env, 'y');
const gB = group(env, [b1, b2], { id: 'grp-b' });
I.enhanceProbeGroup(gB, {});
out.init_fallback_historyCount = probeHistory('probe:grp-b').length;

// ---- 2) Imperative select() publishes nothing. -----------------------------------------------------------------
ctlA.select('ok');
out.select_historyCount = probeHistory('probe:grp-a').length;
out.select_onSelect = selA.length;

// ---- 3) A click on a different probe publishes exactly once, with the group payload, from owner probe:<group>. --
selA = [];
gA.dispatch('click', { target: a3 });
const hClick = probeHistory('probe:grp-a');
out.click_historyCount = hClick.length;
out.click_payload = seen['probe:grp-a'].length ? seen['probe:grp-a'][0] : null;
out.click_from = hClick.length ? hClick[0].from : null;
out.click_subscriberCalls = seen['probe:grp-a'].length;
out.click_onSelect = selA.join(',');

// ---- 3b) Ordering: onSelect runs BEFORE the bus message, so a subscriber can never pre-empt the app's own handler. --
const o1 = probe(env, 'm'), o2 = probe(env, 'n');
const gO = group(env, [o1, o2], { id: 'grp-o' });
const historyAtOnSelect = [];
I.enhanceProbeGroup(gO, { onSelect: function () { historyAtOnSelect.push(probeHistory('probe:grp-o').length); } });
gO.dispatch('click', { target: o2 });
out.order_historyAtOnSelect = historyAtOnSelect.join(',');
out.order_historyAfter = probeHistory('probe:grp-o').length;

// ---- 4) A no-op re-click and a status repaint publish nothing more. --------------------------------------------
gA.dispatch('click', { target: a3 });
a3.className = 'jc-probe jc-probe-warn';
out.reclick_repaint_historyCount = probeHistory('probe:grp-a').length;

// ---- 5) Keyboard selection is user-driven and publishes. -------------------------------------------------------
gA.dispatch('keydown', key('ArrowRight'));   // fail -> ok (wraps)
out.key_historyCount = probeHistory('probe:grp-a').length;
out.key_lastId = seen['probe:grp-a'].length ? seen['probe:grp-a'][seen['probe:grp-a'].length - 1].id : null;

// ---- 6) Group key: the attribute value wins over the element id. ------------------------------------------------
const c1 = probe(env, 'p'), c2 = probe(env, 'q');
const gC = group(env, [c1, c2], { groupAttr: 'named', id: 'other' });
I.enhanceProbeGroup(gC, {});
gC.dispatch('click', { target: c2 });
out.named_historyCount = probeHistory('probe:named').length;
out.named_idTopicCount = probeHistory('probe:other').length;
out.key_attr = I.probeGroupTopicKey(gC);

// ---- 7) No key (no attribute value, no id) or a malformed key: nothing is published, onSelect still fires. -----
const before = anyProbeHistory();
const d1 = probe(env, 'p'), d2 = probe(env, 'q');
const gD = group(env, [d1, d2], {});
const selD = [];
I.enhanceProbeGroup(gD, { onSelect: function (id) { selD.push(id); } });
gD.dispatch('click', { target: d2 });
const e1 = probe(env, 'p'), e2 = probe(env, 'q');
const gE = group(env, [e1, e2], { id: 'has space' });
const selE = [];
I.enhanceProbeGroup(gE, { onSelect: function (id) { selE.push(id); } });
gE.dispatch('click', { target: e2 });
out.nokey_published = anyProbeHistory() - before;
out.nokey_onSelect = selD.join(',');
out.badkey_onSelect = selE.join(',');
out.key_none = I.probeGroupTopicKey(gD);
out.key_bad = I.probeGroupTopicKey(gE);

// ---- 8) The SSC shape: <div id="ssc-probe-row" data-juneau-probe-group> publishes probe:ssc-probe-row. --------
watch('probe:ssc-probe-row');
const s1 = probe(env, 'dns', { checked: true }), s2 = probe(env, 'tls');
const gS = group(env, [s1, s2], { id: 'ssc-probe-row' });
I.enhanceProbeGroup(gS, {});
out.ssc_initCount = probeHistory('probe:ssc-probe-row').length;
gS.dispatch('click', { target: s2 });
out.ssc_clickPayload = seen['probe:ssc-probe-row'][0] || null;

// ---- 9) Job wrap: start / progress / result mapping. -----------------------------------------------------------
function jobHistory(jobId) { return probeHistory('job:' + jobId); }
['j-1', 'j-null', 'j-srv', 'j-k'].forEach(function (id) { watch('job:' + id); });
['cancelled', 'cancelled-after-effect', 'failure', 'refusal', 'unknown'].forEach(function (o, i) { watch('job:j-o' + i); });
I.publishJobEvent({ jobId: 'j-1' }, 'start');
I.publishJobEvent({ jobId: 'j-1' }, 'progress', 'Copying 3/10');
I.publishJobEvent({ jobId: 'j-1' }, 'result', { outcome: 'success', message: 'done' });
out.job_j1 = seen['job:j-1'];
out.job_from = (jobHistory('j-1')[0] || {}).from || null;
const outcomes = ['cancelled', 'cancelled-after-effect', 'failure', 'refusal', 'unknown'];
out.job_states = outcomes.map(function (o, i) {
	I.publishJobEvent({ jobId: 'j-o' + i }, 'result', { outcome: o });
	const p = seen['job:j-o' + i];
	return p.length ? p[0].state : null;
});
I.publishJobEvent({ jobId: 'j-null' }, 'result', null);
out.job_nullResult = seen['job:j-null'];

// ---- 10) Job wrap guards: bad jobId, claimed elsewhere (a bridge carries job:*), unknown kind. -----------------
let threw = null;
try {
	I.publishJobEvent({ jobId: 'a b' }, 'start');
	I.publishJobEvent({}, 'start');
	I.publishJobEvent(null, 'start');
	bus.claim('job:j-srv', 'bridge:ops');
	I.publishJobEvent({ jobId: 'j-srv' }, 'progress', 'local');
	I.publishJobEvent({ jobId: 'j-k' }, 'bogus', 'x');
} catch (e) { threw = String(e && e.message); }
out.job_guard_threw = threw;
out.job_badId_count = bus.history().filter(function (h) { return h.topic === 'job:a b' || h.topic === 'job:undefined' || h.topic === 'job:null'; }).length;
out.job_claimed_localCount = bus.history().filter(function (h) { return h.topic === 'job:j-srv' && h.from !== 'bridge:ops'; }).length;
out.job_bogusKind_count = seen['job:j-k'].length;

// ---- 10b) A bus that throws never reaches the host (publishJobEvent runs inside startJobStream). -----------------
{
	const realOwner = bus.owner;
	let thrown = null;
	bus.owner = function () { throw new Error('boom'); };
	try { I.publishJobEvent({ jobId: 'j-throw' }, 'start'); } catch (e) { thrown = String(e && e.message); }
	bus.owner = realOwner;
	out.job_busThrows_threw = thrown;
}

// ---- 10c) startJobStream against a fake EventSource: duplicate result, finish-then-publish, late progress, reuse. --
{
	const env2 = makeEnv();
	const sources = [];
	function FakeEventSource(url) { this.url = url; this.listeners = {}; this.closed = false; sources.push(this); }
	FakeEventSource.prototype.addEventListener = function (type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); };
	FakeEventSource.prototype.close = function () { this.closed = true; };
	FakeEventSource.prototype.fire = function (type, data) { (this.listeners[type] || []).forEach(function (fn) { fn({ data: data }); }); };
	env2.EventSource = FakeEventSource;
	loadScripts([busJsPath], env2);
	const loaded = loadViews(rendersJsPath, viewsJsPath, env2);
	const bus2 = loaded.NS.bus, I2 = loaded.I;

	// Runs one job; every publish also records whether the row had already settled (finish() clears the running marker).
	function run(jobId) {
		const tr = env2.el('tr');
		const rec = { states: [], settledAtPublish: [] };
		bus2.subscribe('job:' + jobId, function (p) {
			rec.states.push(p.state);
			rec.settledAtPublish.push(!tr.dataset.juneauJob);
		});
		I2.startJobStream({ jobId: jobId, streamUrl: '/s/' + jobId }, { id: 'a' }, env2.el('table'), tr, null);
		rec.es = sources[sources.length - 1];
		return rec;
	}
	const cancelled = JSON.stringify({ outcome: 'cancelled', message: 'stopped' });
	const r1 = run('s-1');
	out.stream_afterStart = r1.states.join(',');
	r1.es.fire('progress', 'half');
	r1.es.fire('result', cancelled);
	r1.es.fire('result', JSON.stringify({ outcome: 'failure', message: 'dup' }));   // differs: the bus dedups identical state
	r1.es.fire('progress', 'late');
	out.stream_states = r1.states.join(',');
	out.stream_settledAtPublish = r1.settledAtPublish.join(',');
	out.stream_closed = r1.es.closed;

	// The same job id again after the terminal result: a fresh run publishes normally.
	const r2 = run('s-1');
	r2.es.fire('result', cancelled);
	out.stream_reuseStates = r2.states.join(',');

	// A stream error publishes no terminal state (the plan: an interrupted stream says nothing about the job).
	const r3 = run('s-err');
	r3.es.fire('error');
	out.stream_errorStates = r3.states.join(',');
	out.stream_errorSettled = r3.es.closed;
}

// ---- 11) No bus on the page: every path is a silent no-op and onSelect is unchanged. ----------------------------
const plain = loadViews(rendersJsPath, viewsJsPath);
const pe = plain.env, PI = plain.I;
const n1 = probe(pe, 'p'), n2 = probe(pe, 'q');
const gN = group(pe, [n1, n2], { id: 'grp-n' });
const selN = [];
let nobusThrew = null;
try {
	PI.enhanceProbeGroup(gN, { onSelect: function (id) { selN.push(id); } });
	gN.dispatch('click', { target: n2 });
	PI.publishJobEvent({ jobId: 'j-1' }, 'start');
} catch (e) { nobusThrew = String(e && e.message); }
out.nobus_hasBus = !!(plain.NS && plain.NS.bus);
out.nobus_onSelect = selN.join(',');
out.nobus_threw = nobusThrew;

process.stdout.write(JSON.stringify(out));
