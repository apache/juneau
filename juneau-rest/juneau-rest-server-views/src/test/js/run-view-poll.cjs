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
 * run-view-poll.cjs - the run-view module's network harness: poll() against a signal-honouring fake fetch and the
 * fake clock, mount() and the "run-view" region populator.
 *
 * Every case gets a FRESH environment (console-output-env.cjs) and passes by returning without throwing; the report
 * maps the case name to true, or to the failure's stack.
 *
 *   Usage:  node run-view-poll.cjs <renders> <views> <regions> <helpers> <run-view> <vectors.json>
 */
'use strict';

const path = require('node:path');
const E = require(path.join(__dirname, 'console-output-env.cjs'));

const argv = process.argv.slice(2);
if (argv.length < 6) {
	process.stderr.write('usage: run-view-poll.cjs <renders> <views> <regions> <helpers> <run-view> <vectors>\n');
	process.exit(2);
}

const cases = [];
function test(name, fn) { cases.push({ name: name, fn: fn }); }
function expect(cond, detail) {
	if (!cond) throw new Error(typeof detail === 'string' ? detail : JSON.stringify(detail));
}
function same(a, b) { return JSON.stringify(a) === JSON.stringify(b); }
function expectSame(actual, expected, what) {
	expect(same(actual, expected), (what || 'value') + ': expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual));
}

const TAG = '[juneau-run-view]';

/** Lets fetch -> text() -> JSON.parse -> the page handler run. */
function settle() { return E.flush(12); }

/** Rejects call `i` the way a failed network request does. */
function fail(calls, i, err) {
	const entry = calls[i];
	if (entry.settled) return false;
	entry.settled = true;
	entry._d.reject(err || new TypeError('Failed to fetch'));
	return true;
}

function page(o) {
	return Object.assign({ contractVersion: '1', events: [], next: '0', more: false, terminal: false }, o || {});
}

function stepEv(id, extra) { return Object.assign({ ev: 'step', id: id, title: id.toUpperCase() }, extra || {}); }

function mkHost(t) {
	const host = t.env.document.createElement('div');
	t.env.body.appendChild(host);
	return host;
}

/** create() + poll() on a fresh host; c.outcome becomes 'resolved' or 'rejected' when poll settles. */
function start(t, createOpts, pollOpts) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const api = t.NS.runView.create(host, createOpts || {});
	const q = function (sel) { return host.querySelector(sel); };
	const c = {
		host: host, api: api, calls: calls, q: q, outcome: null,
		head: function () { return q('.juneau-rv-headline').textContent; },
		steps: function () { return host.querySelectorAll('.juneau-rv-step').map(function (l) { return l.getAttribute('data-step'); }); },
		code: function () { const i = q('.jc-console-error-item'); return i ? i.getAttribute('data-juneau-error') : null; }
	};
	t.NS.runView.poll(api, Object.assign({ eventsUrl: '/x/events' }, pollOpts || {}))
		.then(function () { c.outcome = 'resolved'; }, function () { c.outcome = 'rejected'; });
	return c;
}

function ourErrors(t) { return t.rec.errorsMatching(TAG); }
function ourWarns(t) { return t.rec.warnsMatching(TAG); }

// @cases:poll

test('poll01_firstRequestHasNoQuery_thenAfterNext', async function (t) {
	const c = start(t);
	expect(c.calls.length === 1, 'the first request goes out at once');
	const init = c.calls[0].init;
	expect(c.calls[0].url === '/x/events', 'no query: ' + c.calls[0].url);
	expect(init.credentials === 'same-origin' && init.headers.Accept === 'application/json' && !!init.signal, 'request init');
	c.calls.resolve(0, page({ events: [stepEv('a')], next: '3' }));
	await settle();
	t.clock.advance(0);
	expectSame(c.steps(), ['a'], 'rendered');
	t.clock.advance(1999);
	expect(c.calls.length === 1, 'waits refreshMs');
	t.clock.advance(1);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/events?after=3', 'after=next: ' + c.calls[1].url);
	c.calls.resolve(1, page({ next: '3' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 3 && c.calls[2].url === '/x/events?after=3', 'next is reused after an empty page');
});

test('poll02_urlWithQueryUsesAmpersand', async function (t) {
	const c = start(t, {}, { eventsUrl: '/x/events?run=7' });
	expect(c.calls[0].url === '/x/events?run=7', c.calls[0].url);
	c.calls.resolve(0, page({ next: 'a.b~c' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls[1].url === '/x/events?run=7&after=a.b~c', 'joined with &: ' + c.calls[1].url);
});

test('poll03_moreRefetchesAtOnce', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a')], next: '1', more: true }));
	await settle();
	t.clock.advance(0);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/events?after=1', 'refetched with no wait: ' + c.calls.length);
});

test('poll04_idleWaitsRefreshMs', async function (t) {
	const c = start(t, {}, { refreshMs: 10 });
	c.calls.resolve(0, page({ next: '0' }));
	await settle();
	t.clock.advance(999);
	expect(c.calls.length === 1, 'floor is 1000');
	t.clock.advance(1);
	expect(c.calls.length === 2, 'floor honoured');
	const d = start(t, {}, { refreshMs: 5000 });
	d.calls.resolve(0, page({ next: '0' }));
	await settle();
	t.clock.advance(4999);
	expect(d.calls.length === 1, 'custom refreshMs');
	t.clock.advance(1);
	expect(d.calls.length === 2, 'custom refreshMs fires');
});

test('poll05_stopsOnTerminal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a')], next: '1', terminal: true }));
	await settle();
	t.clock.advance(0);
	expect(c.outcome === 'resolved', 'poll resolved at terminal: ' + c.outcome);
	expect(c.q('.juneau-rv').getAttribute('data-juneau-rv-status') === 'stopped' && c.head() === '⏸ Stopped', 'stopped, not running: ' + c.head());
	t.clock.advance(120000);
	expect(c.calls.length === 1, 'no request after terminal');
});

test('poll06_stopsOnDoneWithoutMore', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a'), { ev: 'done', status: 'ok' }], next: '2', more: true }));
	await settle();
	t.clock.advance(0);
	expect(c.calls.length === 2, 'done with more keeps going');
	c.calls.resolve(1, page({ events: [], next: '2' }));
	await settle();
	t.clock.advance(120000);
	expect(c.outcome === 'resolved' && c.calls.length === 2, 'done with no more stops');
	const d = start(t);
	d.calls.resolve(0, page({ events: [stepEv('a'), { ev: 'done', status: 'fail' }], next: '2' }));
	await settle();
	t.clock.advance(120000);
	expect(d.outcome === 'resolved' && d.calls.length === 1 && d.head().indexOf('Failed') >= 0, 'done stops at once');
});

test('poll07_abortStops', async function (t) {
	const ac = new AbortController();
	const c = start(t, {}, { signal: ac.signal });
	ac.abort();
	await settle();
	expect(c.calls[0].aborted === true, 'in-flight request aborted');
	expect(c.outcome === 'resolved', 'resolved on abort');
	expect(ourErrors(t).length === 0 && ourWarns(t).length === 0, 'no error and no warning');
	t.clock.advance(120000);
	expect(c.calls.length === 1, 'no further requests');
	const ac2 = new AbortController();
	ac2.abort();
	const d = start(t, {}, { signal: ac2.signal });
	await settle();
	expect(d.calls.length === 0 || d.calls[0].aborted, 'a pre-aborted signal sends nothing');
});

test('poll08_hiddenTabPausesAndVisibleResumes', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a')], next: '1' }));
	await settle();
	const base = t.listenerCount('visibilitychange');
	t.setHidden(true);
	t.clock.advance(2000);
	expect(c.calls.length === 1, 'deferred while hidden');
	expect(t.listenerCount('visibilitychange') === base + 1, 'waiting on visibilitychange');
	t.clock.advance(600000);
	expect(c.calls.length === 1, 'still deferred');
	t.setHidden(false);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/events?after=1', 'polls at once when visible');
	expect(t.listenerCount('visibilitychange') === base, 'listener removed');
});

test('poll09_backoffOnOutage_andRecovers', async function (t) {
	const c = start(t);
	fail(c.calls, 0);
	await settle();
	const conn = c.q('.juneau-rv-reconnect');
	expect(conn && conn.textContent === 'Reconnecting…' && conn.hidden !== true, 'shows Reconnecting');
	const waits = [4000, 8000, 16000, 32000, 60000, 60000];
	for (let i = 0; i < waits.length; i++) {
		t.clock.advance(waits[i] - 1);
		expect(c.calls.length === i + 1, 'no retry before ' + waits[i] + ' (step ' + i + ')');
		t.clock.advance(1);
		expect(c.calls.length === i + 2, 'retry at ' + waits[i] + ' (step ' + i + ')');
		if (i === 1) c.calls.resolve(i + 1, {}, { status: 503 });
		else fail(c.calls, i + 1);
		await settle();
	}
	expect(c.calls.every(function (x) { return x.url === '/x/events'; }), 'the first request is retried as-is');
	expect(t.rec.warnsMatching('E-RV-4').length === 1, 'E-RV-4 once per outage');
	expect(c.q('.jc-console-error') === null, 'an outage is not fatal');
	t.clock.advance(60000);
	c.calls.resolve(7, page({ events: [stepEv('a')], next: '1' }));
	await settle();
	t.clock.advance(0);
	expect(c.q('.juneau-rv-reconnect').hidden === true && c.steps().length === 1, 'recovered');
	t.clock.advance(2000);
	expect(c.calls.length === 9 && c.calls[8].url === '/x/events?after=1', 'resumes after next');
});

test('poll10_4xxIsFatal', async function (t) {
	const msgs = [];
	const host = mkHost(t);
	host.setAttribute('data-juneau-region', 'r');
	const calls = E.abortableFetch(t.env);
	const api = t.NS.runView.create(host, { id: 'r', emit: function (m) { msgs.push(m); } });
	let outcome = null;
	t.NS.runView.poll(api, { eventsUrl: '/x/events' }).then(function () { outcome = 'resolved'; });
	calls.resolve(0, page({ events: [stepEv('a')], next: '1' }));
	await settle();
	t.clock.advance(2000);
	calls.resolve(1, {}, { status: 404 });
	await settle();
	const banner = host.querySelector('.jc-console-error');
	expect(banner && banner.getAttribute('role') === 'alert', 'banner');
	expect(banner.textContent.indexOf('fetching events failed (HTTP 404)') >= 0, banner.textContent);
	expect(host.querySelector('.jc-console-error-item').getAttribute('data-juneau-error') === 'E-RV-4', 'code');
	expect(host.querySelectorAll('.juneau-rv-step').length === 1, 'content kept');
	expect(host.getAttribute('data-juneau-region-state') === 'error', 'host marked as errored');
	expect(outcome === 'resolved', 'resolved, not rejected');
	expect(t.rec.errorsMatching('E-RV-4').length === 1, 'one fatal E-RV-4');
	expectSame(msgs[msgs.length - 1], { kind: 'run-view.state', id: 'r', state: 'error', terminal: false }, 'bus message');
	t.clock.advance(120000);
	expect(calls.length === 2, 'polling stopped');
});

test('poll11_408And429Retry', async function (t) {
	const c = start(t);
	c.calls.resolve(0, {}, { status: 408 });
	await settle();
	expect(c.q('.jc-console-error') === null, '408 is transient');
	t.clock.advance(4000);
	expect(c.calls.length === 2, 'retry at 4000');
	c.calls.resolve(1, {}, { status: 429, headers: { 'Retry-After': '7' } });
	await settle();
	t.clock.advance(6999);
	expect(c.calls.length === 2, 'Retry-After seconds honoured');
	t.clock.advance(1);
	expect(c.calls.length === 3, 'retry at 7000');
	c.calls.resolve(2, {}, { status: 429, headers: { 'Retry-After': '0' } });
	await settle();
	t.clock.advance(1999);
	expect(c.calls.length === 3, 'never sooner than refreshMs');
	t.clock.advance(1);
	expect(c.calls.length === 4, 'retry at 2000');
	expect(c.q('.jc-console-error') === null, 'never fatal');
});

test('poll12_410WithTokenResetsAndRefetches', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a'), stepEv('b')], next: '2' }));
	await settle();
	t.clock.advance(2000);
	c.calls.resolve(1, {}, { status: 410 });
	await settle();
	t.clock.advance(0);
	expect(t.rec.warnsMatching('E-RV-8').length === 1, 'E-RV-8 on the console');
	expect(c.q('.jc-console-error') === null && ourErrors(t).length === 0, 'no banner, no error');
	expect(c.calls.length === 3 && c.calls[2].url === '/x/events', 'refetched from the start: ' + c.calls[2].url);
	expect(c.api.lastSeq() === 0 && c.steps().length === 0, 'view reset');
	c.calls.resolve(2, page({ events: [stepEv('z')], next: '1' }));
	await settle();
	t.clock.advance(0);
	expectSame(c.steps(), ['z'], 'new content');
});

test('poll13_410WithoutTokenIsFatal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, {}, { status: 410 });
	await settle();
	expect(c.code() === 'E-RV-4', 'fatal: ' + c.code());
	t.clock.advance(120000);
	expect(c.calls.length === 1 && c.outcome === 'resolved', 'no reset loop');
	expect(t.rec.warnsMatching('E-RV-8').length === 0, 'no reset logged');
});

test('poll14_contractMismatchIsFatal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ contractVersion: '2', events: [stepEv('a')], next: '1' }));
	await settle();
	t.clock.advance(0);
	expect(c.code() === 'E-RV-2' && c.q('.jc-console-error').textContent.indexOf("contractVersion '2'") >= 0, 'E-RV-2 banner');
	expect(c.steps().length === 0, 'the bad page is not applied');
	const b = start(t);
	b.calls.resolveText(0, '<html>oops</html>');
	await settle();
	expect(b.code() === 'E-RV-2', 'non-JSON body');
	const d = start(t);
	d.calls.resolve(0, page({ events: 'x' }));
	await settle();
	expect(d.code() === 'E-RV-2' && d.outcome === 'resolved', 'events must be an array');
});

test('poll15_nonIncreasingSeqIsFatal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a', { seq: 3 }), stepEv('b', { seq: 3 })], next: '3' }));
	await settle();
	expect(c.code() === 'E-RV-5' && c.steps().length === 0, 'E-RV-5, nothing applied');
});

test('poll16_badNextIsFatal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [stepEv('a')], next: 'bad token' }));
	await settle();
	expect(c.code() === 'E-RV-5' && c.steps().length === 0, 'E-RV-5 for a bad next');
	const d = start(t);
	d.calls.resolve(0, { contractVersion: '1', events: [] });
	await settle();
	expect(d.code() === 'E-RV-5', 'a missing next');
});

test('poll17_oneRequestInFlight', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ next: '0' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 2, 'second request');
	t.clock.advance(600000);
	expect(c.calls.length === 2, 'nothing else while one is pending');
});

test('poll18_neverRejects', async function (t) {
	const calls = E.abortableFetch(t.env);
	t.env.setFetch(function () { throw new TypeError('boom'); });
	const host = mkHost(t);
	const api = t.NS.runView.create(host, {});
	let outcome = 'pending';
	t.NS.runView.poll(api, { eventsUrl: '/x/events' }).then(function () { outcome = 'resolved'; }, function () { outcome = 'rejected'; });
	await settle();
	expect(outcome !== 'rejected', 'a throwing fetch never rejects poll: ' + outcome);
	api.destroy();
	await settle();
	expect(outcome === 'resolved', 'destroy resolves it');
	expect(calls.length === 0, 'stub unused');
});

test('poll19_badEventsUrlIsE1', async function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const api = t.NS.runView.create(host, {});
	for (const u of [undefined, '', 'https://elsewhere.example/e', '//elsewhere.example/e', '/x/{id}/events', 'javascript:x', 42]) {
		let outcome = 'pending';
		t.NS.runView.poll(api, { eventsUrl: u }).then(function () { outcome = 'resolved'; });
		await settle();
		expect(outcome === 'resolved', 'resolves for ' + JSON.stringify(u));
	}
	expect(calls.length === 0, 'no request');
	expect(t.rec.errorsMatching('E-RV-1').length === 7, 'E-RV-1 each: ' + t.rec.errorsMatching('E-RV-1').length);
});

test('poll20_emptyMorePageDoesNotSpin', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ events: [], more: true, next: '0' }));
	await settle();
	t.clock.advance(0);
	expect(c.calls.length === 1, 'not refetched immediately');
	expect(t.rec.warnsMatching('E-RV-3').length === 1, 'warned');
	t.clock.advance(2000);
	c.calls.resolve(1, page({ events: [], more: true, next: '0' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 3, 'keeps polling at refreshMs');
	expect(t.rec.warnsMatching('E-RV-3').length === 1, 'warned once');
	expect(c.q('.jc-console-error') === null, 'not fatal');
});

test('poll21_replayedPageIsHarmless', async function (t) {
	const c = start(t);
	const evs = [stepEv('a', { seq: 1 }), stepEv('b', { seq: 2 })];
	c.calls.resolve(0, page({ events: evs, next: '2' }));
	await settle();
	t.clock.advance(2000);
	c.calls.resolve(1, page({ events: evs, next: '2' }));
	await settle();
	t.clock.advance(0);
	expectSame(c.steps(), ['a', 'b'], 'no duplicates');
	expect(c.api.stats().skipped === 2, 'skipped counted: ' + c.api.stats().skipped);
});

// @cases:mount

test('mount01_returnsCleanupSynchronously', function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const cleanup = t.NS.runView.mount(host, { eventsUrl: '/x/events', title: 'T', compact: true }, {});
	expect(typeof cleanup === 'function', 'a function, not a promise');
	expect(calls.length === 1 && calls[0].url === '/x/events', 'polling started');
	const root = host.querySelector('.juneau-rv');
	expect(root.getAttribute('aria-label') === 'T' && root.className.indexOf('juneau-rv-compact') >= 0, 'options applied');
	cleanup();
});

test('mount02_cleanupTearsDownInOrder', async function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const base = t.listenerCount('visibilitychange');
	const cleanup = t.NS.runView.mount(host, { eventsUrl: '/x/events', id: 'gone' }, {});
	const api = t.NS.runView.of('gone');
	calls.resolve(0, page({ events: [stepEv('a')], next: '1' }));
	await settle();
	t.clock.advance(0);
	t.clock.advance(2000);
	expect(calls.length === 2, 'second request pending');
	cleanup();
	await settle();
	expect(calls[1].aborted === true, 'poll aborted');
	expect(host.childNodes.length === 0, 'DOM gone (tooltip and root)');
	expect(t.NS.runView.of('gone') === null && t.NS.runView.of(host) === null, 'registry entry dropped');
	expectSame(api.state().steps, [], 'model cleared');
	expect(t.listenerCount('visibilitychange') === base, 'no document listeners left');
	cleanup();
	t.clock.advance(120000);
	expect(calls.length === 2, 'second call is a no-op and nothing polls');
});

test('mount03_idSubstitution', function (t) {
	const calls = E.abortableFetch(t.env);
	t.NS.runView.mount(mkHost(t), { eventsUrl: '/runs/{id}/events' }, { rowId: 'a b/7' });
	expect(calls[0].url === '/runs/a%20b%2F7/events', 'substituted: ' + calls[0].url);
	const bad = [
		[{ eventsUrl: '/runs/{id}/events' }, {}],
		[{ eventsUrl: '/runs/{x}/events' }, { rowId: '7' }],
		[{ eventsUrl: 'https://elsewhere.example/e' }, {}],
		[{ eventsUrl: '//elsewhere.example/e' }, {}],
		[{}, {}],
		[{ eventsUrl: '' }, {}],
		[{ eventsUrl: '/x/e', refreshMs: 0 }, {}],
		[{ eventsUrl: '/x/e', refreshMs: '2000' }, {}],
		[{ eventsUrl: '/x/e', poll: 'no' }, {}],
		[{ eventsUrl: '/x/e', compact: 1 }, {}],
		[{ eventsUrl: '/x/e', rawHref: '/raw' }, {}]
	];
	const problems = [];
	for (const b of bad) {
		const host = mkHost(t);
		let err = null;
		try { t.NS.runView.mount(host, b[0], b[1]); } catch (e) { err = e; }
		if (!err || err.code !== 'E-RV-1' || err.name !== 'JuneauRunViewError') problems.push(JSON.stringify(b[0]) + ' -> ' + String(err));
		if (host.childNodes.length !== 0) problems.push(JSON.stringify(b[0]) + ' created DOM');
	}
	expect(problems.length === 0, problems.join('\n'));
	expect(calls.length === 1, 'nothing else fetched');
});

test('mount04_pollFalseNeverFetches', async function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const cleanup = t.NS.runView.mount(host, { poll: false, id: 'p' }, {});
	t.clock.advance(120000);
	expect(calls.length === 0, 'no request');
	t.NS.runView.of('p').append(stepEv('a'));
	t.clock.advance(0);
	expect(host.querySelectorAll('.juneau-rv-step').length === 1, 'append still works');
	cleanup();
	const h2 = mkHost(t);
	t.NS.runView.mount(h2, { poll: false, eventsUrl: '/x/events' }, {});
	expect(calls.length === 0, 'a URL with poll:false is validated but unused');
});

test('mount05_populatorRegistered', async function (t) {
	expect(typeof t.R.resolve('run-view') === 'function', 'populator registered');
	const calls = E.abortableFetch(t.env);
	const el = E.mkRegion(t.env, { id: 'run', type: 'card-body', populate: 'run-view',
		declared: { contractVersion: '1', params: { eventsUrl: '/x/events', title: 'Build', compact: true } } });
	t.R.initRegion(el);
	await settle();
	t.clock.advance(0);
	await settle();
	expect(calls.length === 1 && calls[0].url === '/x/events', 'params reached mount: ' + (calls[0] && calls[0].url));
	expect(el.querySelector('.juneau-rv').getAttribute('aria-label') === 'Build', 'title param');
	t.R.teardownRegion(el._juneauRegion);
	await settle();
	expect(calls[0].aborted === true && el.querySelector('.juneau-rv') === null, 'teardown cleans up');
	expect(el._juneauRegion === undefined, 'torn down normally');
});

test('mount06_emitsRunViewState', async function (t) {
	const got = [];
	t.R.register('watch', function (ctx) { ctx.on(function (m) { got.push(m); }); });
	t.R.initRegion(E.mkRegion(t.env, { id: 'w', type: 'card-body', populate: 'watch' }));
	const calls = E.abortableFetch(t.env);
	const el = E.mkRegion(t.env, { id: 'out', type: 'card-body', populate: 'run-view',
		declared: { contractVersion: '1', params: { eventsUrl: '/x/events' } } });
	t.R.initRegion(el);
	await settle();
	calls.resolve(0, page({ events: [stepEv('a')], next: '1' }));
	await settle();
	t.clock.advance(0);
	t.clock.advance(2000);
	calls.resolve(1, page({ events: [{ ev: 'end', id: 'a', status: 'fail' }, { ev: 'done', status: 'fail' }], next: '3' }));
	await settle();
	t.clock.advance(0);
	expectSame(got.map(function (m) { return m.kind + ':' + m.state + ':' + m.terminal; }),
		['run-view.state:running:false', 'run-view.state:fail:false'], 'bus messages');
	expect(got[0].id === 'out', 'id is the region id');
	const el2 = E.mkRegion(t.env, { id: 'bad', type: 'card-body', populate: 'run-view',
		declared: { contractVersion: '1', params: { eventsUrl: '/x/events' } } });
	t.R.initRegion(el2);
	await settle();
	calls.resolve(2, page({ contractVersion: '9' }));
	await settle();
	expect(el2.getAttribute('data-juneau-region-state') === 'error', 'region state error');
	expect(got.some(function (m) { return m.id === 'bad' && m.state === 'error'; }), 'error published');
});

// @cases:of

test('of01_byElementAndById', function (t) {
	const host = mkHost(t);
	t.NS.runView.mount(host, { poll: false, id: 'run' }, {});
	const a = t.NS.runView.of('run');
	expect(a !== null && t.NS.runView.of(host) === a, 'both lookups agree');
	expect(typeof a.append === 'function' && typeof a.destroy === 'function', 'the instance API');
});

test('of02_unknownIsNull', function (t) {
	expect(t.NS.runView.of('nope') === null && t.NS.runView.of(mkHost(t)) === null && t.NS.runView.of(null) === null
		&& t.NS.runView.of(undefined) === null && t.NS.runView.of(42) === null, 'null for anything unknown');
});

test('of03_sseStyleAppend', function (t) {
	const host = mkHost(t);
	t.NS.runView.mount(host, { poll: false, id: 'run' }, {});
	const msg = JSON.stringify({ ev: 'step', id: 'a', title: 'A', seq: 1 });
	t.NS.runView.of('run').append(JSON.parse(msg));
	t.clock.advance(0);
	expect(host.querySelectorAll('.juneau-rv-step').length === 1, 'rendered');
});

test('chk99_noStubsLeft', function (t) {
	const bad = [];
	for (const k of Object.keys(t.NS.runView))
		if (k !== '__test' && typeof t.NS.runView[k] !== 'function' && typeof t.NS.runView[k] !== 'string') bad.push(k);
	expect(bad.length === 0, 'stubs: ' + bad.join(','));
	expect(Object.isFrozen(t.NS.runView), 'frozen');
	expectSame(Object.keys(t.NS.runView).sort(), ['CONTRACT_VERSION', 'create', 'formatDuration', 'isSafeNoteHref', 'isSafeRawHref', 'mount', 'of', 'poll', 'validateEvent', '__test'].sort(), 'public surface');
});

// @cases:end

(async function main() {
	const out = {};
	for (const c of cases) {
		try {
			await c.fn(E.load(argv));
			out[c.name] = true;
		} catch (e) {
			out[c.name] = String((e && e.stack) || e);
		}
	}
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String((e && e.stack) || e));
	process.exit(1);
});
