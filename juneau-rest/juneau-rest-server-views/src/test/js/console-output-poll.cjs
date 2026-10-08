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
 * console-output-poll.cjs - the console-output module's network harness: poll() against a signal-honouring fake
 * fetch and the fake clock, the ?before= loader, mount() and the "console-output" region populator.
 *
 * Every case gets a FRESH environment (console-output-env.cjs) and passes by returning without throwing; the report
 * maps the case name to true, or to the failure's stack.
 *
 *   Usage:  node console-output-poll.cjs <renders> <views> <regions> <helpers> <console-output> <vectors.json>
 */
'use strict';

const path = require('node:path');
const E = require(path.join(__dirname, 'console-output-env.cjs'));

const argv = process.argv.slice(2);
if (argv.length < 6) {
	process.stderr.write('usage: console-output-poll.cjs <renders> <views> <regions> <helpers> <console-output> <vectors>\n');
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

const GONE = 'the log was replaced or truncated (410 Gone); reload to view it from the start';
const TAG = '[juneau-console-output]';

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
	return Object.assign({ contractVersion: '1', lines: [], next: '0', more: false, hasEarlier: false, state: 'RUNNING', terminal: false }, o || {});
}

function earlierPage(o) {
	return Object.assign({ contractVersion: '1', lines: [], more: false, hasEarlier: false }, o || {});
}

function lines(from, to) {
	const out = [];
	for (let n = from; n <= to; n++) out.push({ n: n, text: 'line ' + n });
	return out;
}

function mkHost(t) {
	const host = t.env.document.createElement('div');
	t.env.body.appendChild(host);
	return host;
}

/** create() + poll() on a fresh host; c.outcome becomes 'resolved' or 'rejected' when poll settles. */
function start(t, createOpts, pollOpts) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const api = t.CO.create(host, createOpts || {});
	const q = function (sel) { return host.querySelector(sel); };
	const c = {
		host: host, api: api, calls: calls, q: q, outcome: null,
		pane: q('.juneau-co-pane'),
		ns: function () { return host.querySelectorAll('.juneau-co-line').map(function (r) { return Number(r.getAttribute('data-n')); }); },
		row: function (n) { return host.querySelectorAll('.juneau-co-line').find(function (r) { return r.getAttribute('data-n') === String(n); }) || null; },
		state: function () { return q('.juneau-co-state').textContent; },
		code: function () { const i = q('.jc-console-error-item'); return i ? i.getAttribute('data-juneau-error') : null; }
	};
	t.CO.poll(api, Object.assign({ linesUrl: '/x/lines' }, pollOpts || {}))
		.then(function () { c.outcome = 'resolved'; }, function () { c.outcome = 'rejected'; });
	return c;
}

function ourErrors(t) { return t.rec.errorsMatching(TAG); }
function ourWarns(t) { return t.rec.warnsMatching(TAG); }

// @cases:poll

test('p01_firstRequestIsTailThenAfterNext', async function (t) {
	const c = start(t);
	expect(c.calls.length === 1, 'the first request goes out at once');
	const init = c.calls[0].init;
	expect(c.calls[0].url === '/x/lines?tail=5000', 'default tail: ' + c.calls[0].url);
	expect(init.credentials === 'same-origin' && init.headers.Accept === 'application/json' && !!init.signal, 'request init');
	c.calls.resolve(0, page({ lines: lines(1, 3), next: '3' }));
	await settle();
	expectSame(c.ns(), [1, 2, 3], 'rows');
	expect(c.state() === 'RUNNING', 'status applied');
	t.clock.advance(1999);
	expect(c.calls.length === 1, 'waits refreshMs');
	t.clock.advance(1);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/lines?after=3', 'after=next: ' + c.calls[1].url);
	c.calls.resolve(1, page({ lines: [], next: '3' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 3 && c.calls[2].url === '/x/lines?after=3', 'next is reused after an empty page');
});

test('p02_tailZeroAndAnExistingQuery', async function (t) {
	const c = start(t, {}, { linesUrl: '/x/lines?job=7', tail: 0 });
	expect(c.calls[0].url === '/x/lines?job=7', 'tail:0 sends no parameter: ' + c.calls[0].url);
	c.calls.resolve(0, page({ next: 'a.b~c' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls[1].url === '/x/lines?job=7&after=a.b~c', 'joined with &: ' + c.calls[1].url);
});

test('p03_moreRefetchesImmediately', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(1, 2), next: '2', more: true }));
	await settle();
	expect(c.pane.getAttribute('aria-live') === 'off', 'live region off during a backfill burst');
	t.clock.advance(0);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/lines?after=2', 'refetched without waiting');
	c.calls.resolve(1, page({ lines: lines(3, 3), next: '3' }));
	await settle();
	expect(c.pane.getAttribute('aria-live') === 'polite', 'polite again once caught up');
	t.clock.advance(0);
	expect(c.calls.length === 2, 'more:false waits refreshMs');
});

test('p04_terminalStopsAndResolves', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(1, 1), next: '1', state: 'DONE', terminal: true, durationMs: 5000 }));
	await settle();
	expect(c.outcome === 'resolved', 'poll resolved at terminal: ' + c.outcome);
	expect(c.q('.juneau-co-elapsed').textContent === 'Duration 00:00:05', 'duration shown');
	t.clock.advance(120000);
	expect(c.calls.length === 1, 'no request after terminal');
});

test('p05_oneRequestInFlight', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ next: '0' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 2, 'second request');
	t.clock.advance(600000);
	expect(c.calls.length === 2, 'nothing else while one is pending');
});

test('p06_backoffSequenceAndCap', async function (t) {
	const c = start(t);
	fail(c.calls, 0);
	await settle();
	expect(c.state() === 'Reconnecting…', 'header shows Reconnecting…: ' + c.state());
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
	expect(c.calls.every(function (x) { return x.url === '/x/lines?tail=5000'; }), 'the tail request is retried as-is');
	expect(t.rec.warnsMatching('E-CO-4').length === 1, 'E-CO-4 once per outage');
	expect(c.q('.jc-console-error') === null, 'an outage is not fatal');
	t.clock.advance(60000);
	c.calls.resolve(7, page({ lines: lines(1, 1), next: '1' }));
	await settle();
	expect(c.state() === 'RUNNING', 'recovered');
	t.clock.advance(2000);
	expect(c.calls.length === 9 && c.calls[8].url === '/x/lines?after=1', 'back to refreshMs');
	fail(c.calls, 8);
	await settle();
	expect(t.rec.warnsMatching('E-CO-4').length === 2, 'a new outage warns again');
	t.clock.advance(3999);
	expect(c.calls.length === 9, 'backoff restarts at 2 x refreshMs');
	t.clock.advance(1);
	expect(c.calls.length === 10, 'retry at 4000');
});

test('p07_retryAfter', async function (t) {
	const c = start(t);
	c.calls.resolve(0, {}, { status: 429 });
	await settle();
	t.clock.advance(3999);
	expect(c.calls.length === 1, 'no Retry-After: normal backoff');
	t.clock.advance(1);
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
	const at = new Date(E.BASE_MS + t.clock.now() + 10000).toUTCString();
	c.calls.resolve(3, {}, { status: 429, headers: { 'Retry-After': at } });
	await settle();
	t.clock.advance(9999);
	expect(c.calls.length === 4, 'Retry-After HTTP-date honoured');
	t.clock.advance(1);
	expect(c.calls.length === 5, 'retry at the date');
	expect(c.q('.jc-console-error') === null, '429 is not fatal');
});

test('p08_fatal4xx', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(1, 2), next: '2' }));
	await settle();
	t.clock.advance(2000);
	c.calls.resolve(1, {}, { status: 408 });
	await settle();
	expect(c.q('.jc-console-error') === null, '408 is transient');
	t.clock.advance(4000);
	expect(c.calls.length === 3, 'retried after 408');
	c.calls.resolve(2, {}, { status: 404 });
	await settle();
	const banner = c.q('.jc-console-error');
	expect(banner && banner.getAttribute('role') === 'alert', 'banner');
	expect(c.code() === 'E-CO-4' && banner.textContent.indexOf('fetching lines failed (HTTP 404)') >= 0, banner && banner.textContent);
	expectSame(c.ns(), [1, 2], 'rows kept');
	expect(c.host.getAttribute('data-juneau-region-state') === 'error', 'host marked as errored');
	expect(c.outcome === 'resolved', 'poll resolved, not rejected: ' + c.outcome);
	expect(t.rec.errorsMatching('E-CO-4').length === 1, 'one fatal E-CO-4');
	t.clock.advance(120000);
	expect(c.calls.length === 3, 'polling stopped');
});

test('p09_gone410', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(1, 1), next: '1' }));
	await settle();
	t.clock.advance(2000);
	c.calls.resolve(1, {}, { status: 410 });
	await settle();
	expect(c.code() === 'E-CO-4' && c.q('.jc-console-error').textContent.indexOf(GONE) >= 0, 'gone banner');
	expect(c.outcome === 'resolved', 'resolved');
	t.clock.advance(120000);
	expect(c.calls.length === 2, 'stopped');
});

test('p10_abortIsSilent', async function (t) {
	const ac = new AbortController();
	const c = start(t, {}, { signal: ac.signal });
	ac.abort();
	await settle();
	expect(c.calls[0].aborted === true, 'in-flight request aborted');
	expect(c.outcome === 'resolved', 'resolved on abort');
	expect(ourErrors(t).length === 0 && ourWarns(t).length === 0, 'no error and no warning');
	t.clock.advance(120000);
	expect(c.calls.length === 1, 'no further requests');
});

test('p11_destroyStopsPolling', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ next: '0' }));
	await settle();
	c.api.destroy();
	await settle();
	expect(c.outcome === 'resolved', 'resolved on destroy');
	t.clock.advance(120000);
	expect(c.calls.length === 1, 'the scheduled poll was cancelled');
});

test('p12_hiddenTabDefers', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(1, 1), next: '1' }));
	await settle();
	const base = t.listenerCount('visibilitychange');
	t.setHidden(true);
	t.clock.advance(2000);
	expect(c.calls.length === 1, 'deferred while hidden');
	expect(t.listenerCount('visibilitychange') === base + 1, 'waiting on visibilitychange');
	t.clock.advance(600000);
	expect(c.calls.length === 1, 'still deferred');
	t.setHidden(false);
	expect(c.calls.length === 2 && c.calls[1].url === '/x/lines?after=1', 'polls at once when visible');
	expect(t.listenerCount('visibilitychange') === base, 'listener removed');
});

test('p13_emptyMorePageDoesNotSpin', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: [], more: true, next: '0' }));
	await settle();
	t.clock.advance(0);
	expect(c.calls.length === 1, 'not refetched immediately');
	expect(t.rec.warnsMatching('E-CO-5').length === 1, 'warned');
	t.clock.advance(2000);
	c.calls.resolve(1, page({ lines: [], more: true, next: '0' }));
	await settle();
	t.clock.advance(2000);
	expect(c.calls.length === 3, 'keeps polling at refreshMs');
	expect(t.rec.warnsMatching('E-CO-5').length === 1, 'warned once');
	expect(c.q('.jc-console-error') === null, 'not fatal');
});

test('p14_contractVersionFatalKeepsRows', async function (t) {
	const msgs = [];
	const c = start(t, { id: 'job', emit: function (m) { msgs.push(m); } });
	c.calls.resolve(0, page({ lines: lines(1, 2), next: '2' }));
	await settle();
	t.clock.advance(2000);
	c.calls.resolve(1, page({ contractVersion: '2', lines: lines(3, 3), next: '3' }));
	await settle();
	expect(c.code() === 'E-CO-2' && c.q('.jc-console-error').textContent.indexOf("contractVersion '2'") >= 0, 'E-CO-2 banner');
	expectSame(c.ns(), [1, 2], 'rows kept, the bad page not applied');
	expect(c.host.getAttribute('data-juneau-region-state') === 'error', 'errored');
	expect(c.outcome === 'resolved', 'resolved');
	const last = msgs[msgs.length - 1];
	expect(last.kind === 'console-output.state' && last.state === 'error' && last.id === 'job', 'state message: ' + JSON.stringify(last));
	t.clock.advance(120000);
	expect(c.calls.length === 2, 'stopped');
	c.api.destroy();
	c.api.destroy();
});

test('p15_badTokenOrOrderIsFatal', async function (t) {
	const a = start(t, { id: 'a' });
	a.calls.resolve(0, page({ lines: lines(1, 1), next: 'bad token' }));
	await settle();
	expect(a.code() === 'E-CO-5' && a.ns().length === 0, 'bad next token');
	const b = start(t, { id: 'b' });
	b.calls.resolve(0, page({ lines: [{ n: 3, text: 'c' }, { n: 2, text: 'b' }], next: '3' }));
	await settle();
	expect(b.code() === 'E-CO-5' && b.ns().length === 0, 'decreasing n');
	const d = start(t, { id: 'd' });
	d.calls.resolve(0, page({ lines: lines(41, 41), next: '41', hasEarlier: true, before: 'no good' }));
	await settle();
	expect(d.code() === 'E-CO-5', 'bad before token on the tail page');
	expect(a.outcome === 'resolved' && b.outcome === 'resolved' && d.outcome === 'resolved', 'all resolved');
});

test('p16_malformedBodyIsFatal', async function (t) {
	const a = start(t, { id: 'a' });
	a.calls.resolveText(0, '<html>oops</html>');
	await settle();
	expect(a.code() === 'E-CO-2', 'non-JSON body');
	const b = start(t, { id: 'b' });
	b.calls.resolveText(0, '[1,2]');
	await settle();
	expect(b.code() === 'E-CO-2', 'a JSON array is not a page');
	const d = start(t, { id: 'd' });
	d.calls.resolve(0, page({ lines: 'x' }));
	await settle();
	expect(d.code() === 'E-CO-2', 'lines must be an array');
	expect(a.outcome === 'resolved' && b.outcome === 'resolved' && d.outcome === 'resolved', 'all resolved');
});

// @cases:earlier

function tailWithEarlier(t, createOpts, pollOpts) {
	const c = start(t, createOpts, pollOpts);
	c.calls.resolve(0, page({ lines: lines(41, 45), next: '45', hasEarlier: true, before: '40' }));
	return c;
}

test('q01_earlierRequestOneAtATime', async function (t) {
	const c = tailWithEarlier(t, {}, { earlierLimit: 100 });
	await settle();
	const btn = c.q('button.juneau-co-earlier');
	btn.dispatch('click', {});
	expect(c.calls.length === 2 && c.calls[1].url === '/x/lines?before=40&limit=100', 'before request: ' + (c.calls[1] && c.calls[1].url));
	expect(btn.disabled === true && btn.textContent === 'Loading…', 'pending button');
	btn.dispatch('click', {});
	expect(c.calls.length === 2, 'one before request at a time');
	c.calls.resolve(1, earlierPage({ lines: lines(31, 40), hasEarlier: true, before: '30' }));
	await settle();
	expectSame(c.ns(), lines(31, 45).map(function (l) { return l.n; }), 'prepended');
	expect(btn.disabled === false && btn.textContent === 'Load earlier lines', 'button restored');
	btn.dispatch('click', {});
	expect(c.calls[2].url === '/x/lines?before=30&limit=100', 'uses the new before token');
	c.calls.resolve(2, earlierPage({ lines: lines(21, 30) }));
	await settle();
	expect(c.q('.juneau-co-control') === null, 'hasEarlier:false removes the control');
});

test('q02_earlierRetryAnd410', async function (t) {
	const c = tailWithEarlier(t);
	await settle();
	const btn = c.q('button.juneau-co-earlier');
	btn.dispatch('click', {});
	expect(c.calls[1].url === '/x/lines?before=40&limit=2000', 'default limit: ' + c.calls[1].url);
	c.calls.resolve(1, {}, { status: 503 });
	await settle();
	expect(btn.textContent === 'Load earlier lines (retry)' && btn.disabled === false, 'retry text');
	expect(t.rec.warnsMatching('E-CO-4').length === 1, 'E-CO-4 once');
	btn.dispatch('click', {});
	expect(c.calls.length === 3 && c.calls[2].url === '/x/lines?before=40&limit=2000', 'retried');
	c.calls.resolve(2, {}, { status: 410 });
	await settle();
	expect(c.q('button.juneau-co-earlier') === null && c.q('.juneau-co-earlier-notice').textContent === 'Earlier lines not shown', 'notice');
	expect(c.q('.jc-console-error') === null, 'an earlier-lines failure is never fatal');
	t.clock.advance(2000);
	expect(c.calls.length === 4 && c.calls[3].url === '/x/lines?after=45', 'forward polling carries on');
});

test('q03_earlierAbortedWithThePoll', async function (t) {
	const ac = new AbortController();
	const c = tailWithEarlier(t, {}, { signal: ac.signal });
	await settle();
	c.q('button.juneau-co-earlier').dispatch('click', {});
	ac.abort();
	await settle();
	expect(c.calls[1].aborted === true, 'the before request was aborted');
	expect(ourErrors(t).length === 0 && ourWarns(t).length === 0, 'silently');
	expect(c.outcome === 'resolved', 'poll resolved');
});

test('q04_anchorAutoLoadsEarlier', async function (t) {
	t.env.window.location.hash = '#L33';
	const c = tailWithEarlier(t, { id: 'a', anchorPrefix: 'L' }, { earlierLimit: 5 });
	await settle();
	expect(c.calls.length === 2 && c.calls[1].url === '/x/lines?before=40&limit=5', 'auto-load started');
	c.calls.resolve(1, earlierPage({ lines: lines(36, 40), hasEarlier: true, before: '35' }));
	await settle();
	expect(c.calls.length === 3 && c.calls[2].url === '/x/lines?before=35&limit=5', 'continues towards 33');
	c.calls.resolve(2, earlierPage({ lines: lines(31, 35), hasEarlier: true, before: '30' }));
	await settle();
	expect(c.calls.length === 3, 'stops once the row exists');
	expect(c.row(33).classList.contains('juneau-co-target'), 'highlighted');
	expect(t.scrolled.some(function (s) { return s.n === '33'; }), 'scrolled to 33');
	expect(t.rec.warnsMatching('E-CO-8').length === 0, 'found, so no E-CO-8');
});

test('q05_anchorStopsWhenNoEarlier', async function (t) {
	t.env.window.location.hash = '#L5';
	const c = tailWithEarlier(t, { id: 'a', anchorPrefix: 'L' });
	await settle();
	c.calls.resolve(1, earlierPage({ lines: lines(36, 40) }));
	await settle();
	expect(c.calls.length === 2, 'no further before requests');
	expect(t.rec.warnsMatching('E-CO-8').length === 1, 'E-CO-8');
	expect(c.row(36).classList.contains('juneau-co-target'), 'falls back to the next following row');
});

test('q06_earlierWorksAfterTerminal', async function (t) {
	const c = start(t);
	c.calls.resolve(0, page({ lines: lines(41, 45), next: '45', hasEarlier: true, before: '40', state: 'DONE', terminal: true }));
	await settle();
	expect(c.outcome === 'resolved', 'loop finished');
	c.q('button.juneau-co-earlier').dispatch('click', {});
	expect(c.calls.length === 2 && c.calls[1].aborted === false, 'the loader is not tied to the finished loop');
	c.calls.resolve(1, earlierPage({ lines: lines(36, 40) }));
	await settle();
	expectSame(c.ns(), [36, 37, 38, 39, 40, 41, 42, 43, 44, 45], 'prepended after terminal');
});

// @cases:mount

test('m01_returnsCleanupSynchronously', function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	const off = t.CO.mount(host, { linesUrl: '/x/lines' }, {});
	expect(typeof off === 'function' && typeof off.then !== 'function', 'a function, never a promise');
	expect(calls.length === 1 && calls[0].url === '/x/lines?tail=5000', 'polling started');
	expect(host.querySelector('.juneau-co-pane') !== null, 'console created');
	off();
	expect(calls[0].aborted === true, 'cleanup aborts the in-flight request');
	off();
});

test('m02_pollFalseMakesNoFetches', function (t) {
	const calls = E.abortableFetch(t.env);
	const off = t.CO.mount(mkHost(t), { linesUrl: '/x/lines', poll: false });
	t.clock.advance(60000);
	expect(typeof off === 'function' && calls.length === 0, 'no fetch');
	off();
});

test('m03_urlErrorsThrowBeforeAnyDom', function (t) {
	const calls = E.abortableFetch(t.env);
	const bad = [
		[{}, undefined],
		[{ linesUrl: '' }, undefined],
		[{ linesUrl: 42 }, undefined],
		[{ linesUrl: '/x/{id}/lines' }, undefined],
		[{ linesUrl: '/x/{id}/lines' }, { rowId: '' }],
		[{ linesUrl: '/x/{id}/lines' }, { rowId: '..' }],
		[{ linesUrl: '/x/{job}/lines' }, undefined],
		[{ linesUrl: '/x/../lines' }, undefined],
		[{ linesUrl: '//evil/lines' }, undefined],
		[{ linesUrl: 'https://evil/lines' }, undefined],
		[{ linesUrl: '/x/lines', downloadUrl: 'javascript:alert(1)' }, undefined],
		[{ linesUrl: '/x/lines', downloadUrl: '/x/{id}/download' }, undefined],
		[{ linesUrl: '/x/lines', anchorPrefix: '1bad' }, undefined]
	];
	const problems = [];
	for (const [opts, ctx] of bad) {
		const host = mkHost(t);
		let err = null;
		try {
			t.CO.mount(host, opts, ctx);
		} catch (e) {
			err = e;
		}
		if (!err || err.name !== 'JuneauConsoleOutputError' || err.code !== 'E-CO-1')
			problems.push(JSON.stringify(opts) + ' -> ' + String(err));
		if (host.childNodes.length !== 0)
			problems.push(JSON.stringify(opts) + ' created DOM');
	}
	expect(problems.length === 0, problems.join('\n'));
	expect(calls.length === 0, 'nothing fetched');
	expect(t.rec.errorsMatching('E-CO-1').length === bad.length, 'one E-CO-1 per case');
});

test('m04_rowIdSubstitution', function (t) {
	const calls = E.abortableFetch(t.env);
	const host = mkHost(t);
	t.CO.mount(host, { linesUrl: '/runs/{id}/lines', downloadUrl: '/runs/{id}/download' }, { rowId: 'a b/7' });
	expect(calls[0].url === '/runs/a%20b%2F7/lines?tail=5000', 'lines: ' + calls[0].url);
	expect(host.querySelector('.juneau-co-download').getAttribute('href') === '/runs/a%20b%2F7/download', 'download');
});

test('m05_paramRanges', async function (t) {
	const calls = E.abortableFetch(t.env);
	const bad = [{ tail: -1 }, { tail: 10001 }, { tail: 1.5 }, { tail: '5' }, { earlierLimit: 0 }, { earlierLimit: 10001 },
		{ rows: 2 }, { rows: 201 }, { refreshMs: 0 }, { refreshMs: -5 }, { refreshMs: '2000' }];
	const problems = [];
	for (const extra of bad) {
		const host = mkHost(t);
		let code = null;
		try {
			t.CO.mount(host, Object.assign({ linesUrl: '/x/lines' }, extra));
		} catch (e) {
			code = e.code;
		}
		if (code !== 'E-CO-1' || host.childNodes.length !== 0)
			problems.push(JSON.stringify(extra) + ' -> ' + code);
	}
	expect(problems.length === 0, problems.join('\n'));
	expect(calls.length === 0, 'nothing fetched');
	t.CO.mount(mkHost(t), { linesUrl: '/x/lines', tail: 10000, earlierLimit: 1, rows: 200, refreshMs: 500 });
	expect(calls.length === 1 && calls[0].url === '/x/lines?tail=10000', 'edge values accepted');
	calls.resolve(0, page({ next: '0' }));
	await settle();
	t.clock.advance(999);
	expect(calls.length === 1, 'refreshMs raised to the 1000 ms floor');
	t.clock.advance(1);
	expect(calls.length === 2, 'polled at 1000');
});

function regionPage(t) {
	const got = [];
	t.R.register('watch', function (ctx) { ctx.on(function (m) { got.push(m); }); });
	t.R.initRegion(E.mkRegion(t.env, { id: 'w', type: 'card-body', populate: 'watch' }));
	return got;
}

function consoleRegion(t, params) {
	const el = E.mkRegion(t.env, { id: 'out', type: 'card-body', populate: 'console-output',
		declared: { contractVersion: '1', params: params } });
	t.R.initRegion(el);
	return el;
}

async function settleRegion(t) {
	await settle();
	t.clock.advance(0);
	await settle();
}

test('m06_populatorPublishesOnTheBus', async function (t) {
	expect(typeof t.R.resolve('console-output') === 'function', 'populator registered');
	const calls = E.abortableFetch(t.env);
	const got = regionPage(t);
	const el = consoleRegion(t, { linesUrl: '/x/lines', title: 'Build', tail: 0 });
	await settleRegion(t);
	expect(calls.length === 1 && calls[0].url === '/x/lines', 'params reached mount: ' + (calls[0] && calls[0].url));
	expect(el.querySelector('.juneau-co-title').textContent === 'Build', 'title param');
	calls.resolve(0, page({ lines: lines(1, 1), next: '1', state: 'RUNNING' }));
	await settleRegion(t);
	t.clock.advance(2000);
	calls.resolve(1, page({ next: '1', state: 'DONE', terminal: true }));
	await settleRegion(t);
	expectSame(got.map(function (m) { return m.kind + ':' + m.state; }),
		['console-output.state:RUNNING', 'console-output.state:DONE', 'console-output.terminal:DONE'], 'bus messages');
	expect(got[0].id === 'out', 'id is the region id');
});

test('m07_teardownStopsPolling', async function (t) {
	const calls = E.abortableFetch(t.env);
	const el = consoleRegion(t, { linesUrl: '/x/lines' });
	await settleRegion(t);
	calls.resolve(0, page({ next: '0' }));
	await settleRegion(t);
	t.clock.advance(2000);
	expect(calls.length === 2, 'second request in flight');
	t.R.teardownRegion(el._juneauRegion);
	await settle();
	expect(calls[1].aborted === true, 'teardown aborted it');
	t.clock.advance(120000);
	expect(calls.length === 2, 'no further requests');
	expect(ourErrors(t).length === 0, 'no error');
});

test('m08_fatalInARegionKeepsCleanup', async function (t) {
	const calls = E.abortableFetch(t.env);
	const got = regionPage(t);
	const el = consoleRegion(t, { linesUrl: '/x/lines' });
	await settleRegion(t);
	calls.resolve(0, page({ lines: lines(1, 2), next: '2' }));
	await settleRegion(t);
	t.clock.advance(2000);
	calls.resolve(1, page({ contractVersion: '9', next: '2' }));
	await settleRegion(t);
	expect(el.getAttribute('data-juneau-region-state') === 'error', 'region state error');
	expect(el.querySelector('.jc-console-error') !== null, 'banner inside the region');
	expect(el.querySelectorAll('.juneau-co-line').length === 2, 'rows kept');
	expect(got.some(function (m) { return m.kind === 'console-output.state' && m.state === 'error'; }), 'error state published');
	t.R.teardownRegion(el._juneauRegion);
	expect(el._juneauRegion === undefined, 'torn down normally');
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
