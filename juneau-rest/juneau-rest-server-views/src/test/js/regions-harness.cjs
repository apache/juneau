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
 * regions-harness.cjs - the shared loader for the always-on Node behavioral harnesses that exercise
 * juneau-regions.js: the populate primitive, the message bus, SD-2's emit-ownership invariant, and the
 * initial-broadcast barrier.
 *
 * It layers three things on top of views-dom-shim.cjs, each of which a region test cannot do without:
 *
 *   1. A CONTROLLABLE CLOCK.  The shim's own loadViews() sandbox runs setTimeout callbacks IMMEDIATELY and makes
 *      clearTimeout a no-op, which is fine for the runtimes it was written for and useless here: the barrier's
 *      whole contract is "drains at 0ms when every region settled, drains at the 2000ms deadline when one did
 *      not", and asserting that needs the clock to be an input rather than a fact.  Several assertions in the
 *      design's test list are explicitly against the clock rather than against delivery order, because delivery
 *      is correct under both readings and only the latency differs.
 *   2. A RECORDING CONSOLE, because "exactly one error naming X" is an assertion in five of these tests.
 *   3. AN INJECTABLE AbortController, so the baseline-fails-loud test can load a page into an environment that
 *      genuinely does not have one - a `delete globalThis.AbortController` in-process would not be observable
 *      by an already-loaded runtime, since the runtime feature-tests exactly once at load.
 *
 *   const { load, mkRegion, flush } = require('./regions-harness.cjs');
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const { makeEnv, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

/**
 * A fake macrotask clock.  `advance(ms)` fires every timer due at or before the new time, in due-time order and
 * then in scheduling order, which is what makes "the drain happened at 0ms" and "the drain happened at 2000ms"
 * two distinguishable outcomes.
 */
function makeClock() {
	let now = 0;
	let seq = 0;
	let timers = [];
	return {
		now: function () { return now; },
		pending: function () { return timers.length; },
		set: function (fn, ms) {
			const id = ++seq;
			timers.push({ id: id, at: now + (ms || 0), seq: id, fn: fn });
			return id;
		},
		clear: function (id) { timers = timers.filter(function (t) { return t.id !== id; }); },
		/** Fires everything due within `ms` of now.  `advance(0)` fires the zero-delay macrotasks and nothing else. */
		advance: function (ms) {
			const until = now + (ms || 0);
			for (;;) {
				const due = timers.filter(function (t) { return t.at <= until; });
				if (due.length === 0) break;
				due.sort(function (a, b) { return a.at - b.at || a.seq - b.seq; });
				const t = due[0];
				timers = timers.filter(function (x) { return x !== t; });
				now = Math.max(now, t.at);
				t.fn();
			}
			now = until;
		}
	};
}

/** A console that records instead of printing, so "exactly one error naming X" is assertable. */
function makeConsole() {
	const rec = { errors: [], warns: [], logs: [] };
	rec.console = {
		error: function (...a) { rec.errors.push(a.map(String).join(' ')); },
		warn: function (...a) { rec.warns.push(a.map(String).join(' ')); },
		log: function (...a) { rec.logs.push(a.map(String).join(' ')); },
		info: function (...a) { rec.logs.push(a.map(String).join(' ')); },
		debug: function (...a) { rec.logs.push(a.map(String).join(' ')); }
	};
	rec.errorsMatching = function (needle) {
		return rec.errors.filter(function (e) { return e.indexOf(needle) >= 0; });
	};
	rec.warnsMatching = function (needle) {
		return rec.warns.filter(function (e) { return e.indexOf(needle) >= 0; });
	};
	return rec;
}

/**
 * Loads juneau-renders.js, juneau-views.js and juneau-regions.js into one fresh environment.
 *
 * `opts.noAbortController` withholds the platform baseline for the fail-loud test.
 */
function load(rendersJsPath, viewsJsPath, regionsJsPath, opts) {
	opts = opts || {};
	const env = makeEnv();
	const rec = makeConsole();
	const clock = makeClock();

	env.window.console = rec.console;
	env.window.setTimeout = clock.set;
	env.window.clearTimeout = clock.clear;

	const sandbox = {
		window: env.window,
		document: env.document,
		console: rec.console,
		setTimeout: clock.set,
		clearTimeout: clock.clear,
		setInterval: function () { return 0; },
		clearInterval: function () {},
		Promise: Promise,
		fetch: function (...args) { return env.callFetch(...args); }
	};
	if (!opts.noAbortController) {
		sandbox.AbortController = AbortController;
		sandbox.AbortSignal = AbortSignal;
		sandbox.DOMException = DOMException;
	}

	for (const file of [rendersJsPath, viewsJsPath, regionsJsPath]) {
		// NOSONAR javascript:S1523 -- loading the production juneau-renders.js/juneau-views.js/juneau-regions.js
		// sources into a VM sandbox is this harness's intended mechanism for exercising them under the DOM shim;
		// inputs are fixed local file paths supplied by the test, never attacker-controlled data.
		vm.runInNewContext(fs.readFileSync(path.resolve(file), 'utf8'), sandbox, { filename: path.basename(file) });
	}

	const NS = env.window.JuneauViews;
	return { env: env, NS: NS, R: NS?.regions, I: NS?.init, rec: rec, clock: clock };
}

/**
 * Builds a region container and appends it (default: to `body`).  `opts.hiddenPanel` wraps it in a non-active
 * `.jc-panel`, which is the live shape a card in a non-initial tab has and the one the runtime's own visibility
 * predicate reports as hidden.
 */
function mkRegion(env, opts) {
	const el = env.el('div');
	el.setAttribute('data-juneau-region', opts.id);
	if (opts.type) el.setAttribute('data-juneau-region-type', opts.type);
	if (opts.host) el.setAttribute('data-juneau-region-host', opts.host);
	if (opts.populate) el.setAttribute('data-juneau-region-populate', opts.populate);
	let parent = opts.parent || env.body;
	if (opts.hiddenPanel) {
		const panel = env.el('div');
		panel.className = 'jc-panel';
		parent.appendChild(panel);
		el.__panel = panel;
		parent = panel;
	}
	parent.appendChild(el);
	return el;
}

/** Marks a `.jc-panel` wrapper active, so the region inside it becomes visible. */
function activatePanel(el) {
	if (el.__panel) el.__panel.className = 'jc-panel jc-active';
}

/** Drains the real microtask queue `n` times, so a chain of promise continuations has all run. */
async function flush(n) {
	for (let i = 0; i < (n || 6); i++) await Promise.resolve();
}

/**
 * Installs a fetch that HONORS ITS SIGNAL, and returns the live call log.
 *
 * The fidelity matters more than it looks: a fake that resolves whatever the test resolves, signal or no signal, lets
 * a stale response overwrite a fresh one and makes the out-of-order-settle tests pass for the wrong reason - they
 * would be asserting the test's own resolve order rather than the runtime's cancellation.  A real fetch rejects with
 * `signal.reason` the moment the signal aborts and ignores the response that arrives afterwards, so this one does
 * too: `resolve(entry, body)` on an already-aborted call is a no-op, exactly as a socket read on a cancelled request
 * is.
 */
function abortableFetch(env) {
	const calls = [];
	env.setFetch(function (url, init) {
		const d = deferred();
		const entry = { url: url, init: init, aborted: false, settled: false, _d: d };
		const abort = function () {
			entry.aborted = true;
			entry.settled = true;
			d.reject(init.signal.reason);
		};
		calls.push(entry);
		if (init?.signal) {
			if (init.signal.aborted) abort();
			else init.signal.addEventListener('abort', abort, { once: true });
		}
		return d.promise;
	});
	calls.resolve = function (i, body, opts) {
		const entry = calls[i];
		if (entry.settled) return false;
		entry.settled = true;
		entry._d.resolve(jsonResponse(body, opts));
		return true;
	};
	calls.resolveText = function (i, text) {
		const entry = calls[i];
		if (entry.settled) return false;
		entry.settled = true;
		entry._d.resolve({ ok: true, status: 200, text: function () { return Promise.resolve(text); } });
		return true;
	};
	return calls;
}

/** A deferred whose promise settles only when the test says so. */
function deferred() {
	let resolve, reject;
	const promise = new Promise(function (res, rej) { resolve = res; reject = rej; });
	return { promise: promise, resolve: resolve, reject: reject };
}

/**
 * A thenable whose `then` is SYNCHRONOUS: calling `settle()` runs the framework's continuation in the caller's own
 * turn rather than on a microtask.  This is the only construction that can make a released region's populate settle
 * partway through a fan-out, which is the last-window-close case; resolving an ordinary promise from inside a
 * subscriber queues a microtask, so the continuation runs after the synchronous drain has already finished and the
 * assertion silently tests the ordinary case instead.
 */
function syncThenable() {
	const box = { settle: null };
	box.thenable = {
		then: function (res) { box.settle = function (v) { res(v); }; }
	};
	return box;
}

module.exports = {
	load: load,
	mkRegion: mkRegion,
	activatePanel: activatePanel,
	flush: flush,
	deferred: deferred,
	abortableFetch: abortableFetch,
	syncThenable: syncThenable,
	jsonResponse: jsonResponse
};
