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
 * data.cjs - Node harness for the data-source loader in juneau-data.js: config and lookup, URLs and the fetch helper,
 * the parsers, the columnar builder, chunk lists, the cache, DataSource, Derived and boot.  It runs the real bus and
 * juneau-data.js under the views DOM shim, with a manual clock, a scripted fetch, a fake IndexedDB and Node's
 * WebCrypto.  Each section returns {check: true | {actual, expected}}; ConsoleData_Test fails on any non-true check.
 *
 *   Usage:  node data.cjs <juneau-bus.js> <juneau-data.js>
 */
'use strict';

const path = require('node:path');
const { webcrypto } = require('node:crypto');
const { makeEnv, loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));
const { fakeClock, settle } = require(path.join(__dirname, 'bus-fake-clock.cjs'));

const [busPath, dataPath] = process.argv.slice(2);
if (!busPath || !dataPath) {
	console.error('usage: node data.cjs <juneau-bus.js> <juneau-data.js>');
	process.exit(2);
}

// The bus logs through console; keep its lines for the report instead of the test's stderr.
const busLogs = [];
for (const k of ['error', 'warn', 'debug']) console[k] = (...a) => busLogs.push(k + ': ' + a.join(' '));

const enc = new TextEncoder();

/** A fetch Response: body text or byte parts; o: {status, headers, parts, noStream}. */
function textResponse(body, o) {
	o = o || {};
	const status = o.status || 200;
	const bytes = (o.parts || [body]).map(p => typeof p === 'string' ? enc.encode(p) : p);
	const headers = o.headers || {};
	const res = {
		ok: status >= 200 && status < 300, status: status, cancelled: false,
		headers: { get: k => Object.prototype.hasOwnProperty.call(headers, k) ? headers[k] : null },
		text: () => Promise.resolve(Buffer.concat(bytes.map(b => Buffer.from(b))).toString('utf8'))
	};
	if (!o.noStream) {
		res.body = {
			cancel: () => { res.cancelled = true; return Promise.resolve(); },
			getReader() {
				let i = 0;
				return {
					read: () => Promise.resolve(i < bytes.length ? { done: false, value: bytes[i++] } : { done: true, value: undefined }),
					cancel: () => { i = bytes.length; res.cancelled = true; return Promise.resolve(); },
					releaseLock() {}
				};
			}
		};
	}
	return res;
}

function deferred() {
	let resolve, reject;
	const promise = new Promise((a, b) => { resolve = a; reject = b; });
	return { promise, resolve, reject };
}

/** fetch over routes {path: (url, init) => Response | Promise}; the path is the URL before '?'.  Unknown paths 404. */
function makeFetch(routes, fetches) {
	return function (url, init) {
		init = init || {};
		const rec = { url: String(url), accept: init.headers && init.headers.Accept, credentials: init.credentials,
			method: init.method, aborted: false };
		fetches.push(rec);
		return new Promise((resolve, reject) => {
			// Like a real fetch, a signal that is already aborted rejects at once.
			if (init.signal && init.signal.aborted) {
				rec.aborted = true;
				reject(new DOMException('The operation was aborted.', 'AbortError'));
				return;
			}
			if (init.signal) init.signal.addEventListener('abort', () => {
				rec.aborted = true;
				reject(new DOMException('The operation was aborted.', 'AbortError'));
			});
			const route = routes[rec.url.split('?')[0]];
			Promise.resolve(typeof route === 'function' ? route(rec.url, init) : textResponse('not found', { status: 404 }))
				.then(resolve, reject);
		});
	};
}

const FakeKeyRange = { bound: (lower, upper) => ({ lower, upper }), upperBound: upper => ({ lower: -Infinity, upper }) };

/**
 * An in-memory IndexedDB with the surface juneau-data.js uses: open (with upgradeneeded), one store, transactions that
 * complete when their last request settles, get/put/delete(range)/openCursor.  Values are structured-cloned on put and
 * on read, as a real database does.  A failed request fires the transaction's onerror while tx.error is still null,
 * then sets tx.error just before onabort, as browsers do.  tx.abort() skips the requests not yet run.
 * o: {failOpen, hangOpen, lateOpen: {clock, ms} (answers ms after open, on that fake clock), quotaOnPut, seed: [record...]}.
 */
function fakeIdb(o) {
	o = o || {};
	const stores = new Map();
	const stats = { opens: 0, txs: 0, gets: 0, puts: 0, deletes: 0, aborts: 0 };
	const dbs = [];
	let version = 0;
	if (o.seed) {
		stores.set('chunks', new Map(o.seed.map(r => [r.key, structuredClone(r)])));
		version = 1;
	}
	const later = fn => setImmediate(fn);
	const inRange = (k, r) => r && typeof r === 'object' ? k >= r.lower && k <= r.upper : k === r;
	function transaction(names, mode) {
		stats.txs++;
		const tx = {
			mode, pending: 0, done: false, error: null, failure: null, aborted: false, oncomplete: null, onerror: null, onabort: null,
				abort() {
					if (tx.done) throw new DOMException('The transaction has finished.', 'InvalidStateError');
					stats.aborts++;
					tx.aborted = true;
					tx.failure = tx.failure || new DOMException('The transaction was aborted.', 'AbortError');
					tx.maybeDone();
				},
			maybeDone() {
				later(() => {
					if (tx.done || tx.pending > 0) return;
					tx.done = true;
					if (tx.failure) {
						if (tx.onerror) tx.onerror({ target: tx });
						tx.error = tx.failure;
						if (tx.onabort) tx.onabort({ target: tx });
					} else if (tx.oncomplete) tx.oncomplete({ target: tx });
				});
			},
			objectStore(name) {
				const m = stores.get(name);
				return {
					index: () => ({
						openKeyCursor: range => {
							const entries = Array.from(m.values()).filter(v => inRange(v.fetchedAt, range))
								.sort((a, b) => a.fetchedAt - b.fetchedAt).map(v => [v.fetchedAt, v.key]);
							const outer = { result: null, onsuccess: null, onerror: null };
							let i = 0;
							const step = () => {
								const r = request(tx, () => {
									const e = entries[i++];
									return e ? { key: e[0], primaryKey: e[1], continue: step } : null;
								});
								r.onsuccess = () => { outer.result = r.result; if (outer.onsuccess) outer.onsuccess({ target: outer }); };
							};
							step();
							return outer;
						}
					}),
					get: key => request(tx, () => { stats.gets++; return m.has(key) ? structuredClone(m.get(key)) : undefined; }),
					put: value => {
						const copy = structuredClone(value);
						return request(tx, () => {
							if (o.quotaOnPut) throw new DOMException('quota exceeded', 'QuotaExceededError');
							stats.puts++;
							m.set(copy.key, copy);
							return copy.key;
						});
					},
					delete: range => request(tx, () => {
						for (const k of Array.from(m.keys())) if (inRange(k, range)) { m.delete(k); stats.deletes++; }
					}),
					openCursor: () => {
						const keys = Array.from(m.keys()).sort();
						const outer = { result: null, onsuccess: null, onerror: null };
						let i = 0;
						const step = () => {
							const r = request(tx, () => {
								while (i < keys.length && !m.has(keys[i])) i++;
								const k = keys[i++];
								return k === undefined ? null : { key: k, value: structuredClone(m.get(k)),
									delete: () => { m.delete(k); stats.deletes++; }, continue: step };
							});
							r.onsuccess = () => { outer.result = r.result; if (outer.onsuccess) outer.onsuccess({ target: outer }); };
						};
						step();
						return outer;
					}
				};
			}
		};
		tx.maybeDone();
		return tx;
	}
	function request(tx, run) {
		const req = { result: undefined, error: null, onsuccess: null, onerror: null };
		tx.pending++;
		later(() => {
			try {
				if (tx.aborted) throw tx.failure;
				req.result = run();
				if (req.onsuccess) req.onsuccess({ target: req });
			} catch (e) {
				req.error = e;
				tx.failure = tx.failure || e;
				if (req.onerror) req.onerror({ target: req, preventDefault() {} });
			}
			tx.pending--;
			tx.maybeDone();
		});
		return req;
	}
	return {
		stores, stats, dbs,
		records: () => Array.from((stores.get('chunks') || new Map()).values()),
		open(name, v) {
			stats.opens++;
			const req = { result: null, error: null, onsuccess: null, onerror: null, onupgradeneeded: null, onblocked: null };
			if (o.hangOpen) return req;
			(o.lateOpen ? fn => o.lateOpen.clock.timers.setTimeout(fn, o.lateOpen.ms) : later)(() => {
				if (o.failOpen) {
					req.error = new DOMException('open refused', 'UnknownError');
					if (req.onerror) req.onerror({ target: req });
					return;
				}
				req.result = { name, objectStoreNames: { contains: n => stores.has(n) }, closed: false, onversionchange: null,
					createObjectStore: n => { stores.set(n, new Map()); return { createIndex() {} }; }, transaction,
					close() { this.closed = true; } };
				dbs.push(req.result);
				if (v > version) {
					version = v;
					if (req.onupgradeneeded) req.onupgradeneeded({ target: req, oldVersion: 0 });
				}
				if (req.onsuccess) req.onsuccess({ target: req });
			});
			return req;
		}
	};
}

/**
 * A fresh page: the bus and juneau-data.js in one sandbox, every timer on one fake clock, the platform faked.
 * opts: {before(window), routes, idb, noSubtle, clock}.
 */
function fresh(opts) {
	opts = opts || {};
	const env = makeEnv();
	if (opts.before) opts.before(env.window);
	const { NS } = loadScripts([busPath, dataPath], env);
	const D = NS.data, bus = NS.bus;
	const clock = opts.clock || fakeClock();
	Object.assign(bus.config.timers, clock.timers);
	Object.assign(D.config.timers, { setTimeout: clock.timers.setTimeout, clearTimeout: clock.timers.clearTimeout, now: clock.timers.now });
	const fetches = [], routes = opts.routes || {}, errors = [], warns = [];
	// webcrypto.subtle.digest completes on libuv's thread pool, in real time, so no number of setImmediate turns
	// is guaranteed to cover it on a slow or loaded CI runner.  Track the in-flight digests so idle() can await them.
	const digests = new Set();
	const subtle = opts.noSubtle ? null : {
		digest(...a) {
			const p = webcrypto.subtle.digest(...a);
			digests.add(p);
			const done = () => digests.delete(p);
			p.then(done, done);
			return p;
		}
	};
	Object.assign(D.config.platform, {
		fetch: makeFetch(routes, fetches), TextDecoder, TextEncoder, AbortController, URLSearchParams, IDBKeyRange: FakeKeyRange,
		indexedDB: opts.idb || null, subtle
	});
	D.config.log.error = m => errors.push(m);
	D.config.log.warn = m => warns.push(m);
	return { env, NS, D, U: D.util, bus, clock, fetches, routes, errors, warns, digests };
}

/**
 * Runs the clock forward by ms, then drains promise and setImmediate work (fake IDB, fake fetch) to quiet.  Digests still
 * running on the thread pool are awaited, and the drain repeats until none is pending, since each can start more work.
 */
async function idle(h, ms) {
	h.clock.advance(ms || 0);
	for (let i = 0; i < 8; i++) {
		await settle();
		h.clock.advance(0);
	}
	for (let i = 0; i < 100 && h.digests && h.digests.size; i++) {
		await Promise.allSettled(Array.from(h.digests));
		for (let j = 0; j < 4; j++) {
			await settle();
			h.clock.advance(0);
		}
	}
}

/** true when actual and expected have the same JSON, else both (the Java side prints the pair). */
function eq(actual, expected) {
	return JSON.stringify(actual) === JSON.stringify(expected) ? true : { actual: actual === undefined ? null : actual, expected };
}

const sections = [];
function section(name, fn) { sections.push([name, fn]); }

// ---- a_config: section A-B ------------------------------------------------------------------------------------------

section('a_config', async () => {
	const h = fresh();
	const out = {};
	// Add 'boot' here when the loader exports it.
	out.exportKeys = eq(Object.keys(h.D).sort(),
		['boot', 'building', 'builtins', 'config', 'get', 'isHandle', 'put', 'release', 'util']);
	out.frozen = eq([Object.isFrozen(h.D), Object.isFrozen(h.D.builtins), Object.isFrozen(h.U), Object.isFrozen(h.D.config)],
		[true, true, true, false]);
	out.problem = eq(h.U.problem('E-JS-100', 'orders', '//evil.example/x'),
		{ code: 'E-JS-100', message: "data source 'orders' url is not a same-origin path: '//evil.example/x'" });
	out.fmtExtraArgs = eq(h.U.fmt("'%s' %s", 'a', 'b', 'c'), "'a' b");

	// The pre-load queue: read without being consumed, each bad entry logged once, non-data registries skipped.
	const tsv = { create() { return { push: () => [], end: () => [] }; } };
	const q = fresh({ before: w => { w.JuneauConsoleCharts = [['parser', 'app.tsv', tsv], ['parser', 'tsv', tsv],
		['transform', 'app.noop', 5], ['decorator', 'app.d', () => {}]]; } });
	const found = q.U.lookup('parser', 'app.tsv');
	q.U.lookup('parser', 'app.tsv');
	q.U.lookup('transform', 'app.noop');
	out.queueFound = eq(found.impl === tsv, true);
	out.queueNames = eq(found.names.indexOf('app.tsv') >= 0 && found.names.indexOf('tsv') < 0, true);
	out.queueKept = eq(q.env.window.JuneauConsoleCharts.length, 4);
	out.queueLogs = eq(q.errors, [
		"E-JS-97: JuneauCharts.parser.register: name 'tsv' is bare; app names are dotted ('app.tsv')",
		"E-JS-97: JuneauCharts.transform.register: name 'app.noop' is not a function"]);
	out.unknown = eq(q.U.lookup('builder', 'app.none').impl, null);

	// With juneau-charts.js loaded, its registry answers first and the names are the union.
	const x = { create() { return {}; } };
	const reg = names => ({ get: n => n === 'app.x' ? x : null, names: () => names });
	const c = fresh({ before: w => { w.JuneauCharts = { parser: reg(['app.x']), builder: reg([]), transform: reg([]) }; } });
	const viaCharts = c.U.lookup('parser', 'app.x');
	out.chartsImpl = eq(viaCharts.impl === x, true);
	out.chartsNames = eq(viaCharts.names.indexOf('app.x') >= 0, true);
	out.chartsUnknown = eq(c.U.lookup('parser', 'app.y').impl, null);
	// A partial JuneauCharts registry (no names) is not used, rather than throwing.
	const p = fresh({ before: w => { w.JuneauCharts = { parser: { get: () => x } }; } });
	out.chartsPartial = eq(p.U.lookup('parser', 'app.x').impl, null);
	return out;
});

// ---- c_urls: section C ----------------------------------------------------------------------------------------------

section('c_urls', async () => {
	const h = fresh();
	const U = h.U, out = {};
	const msg = fn => { try { fn(); return null; } catch (e) { return e.message; } };
	const amsg = p => p.then(() => null, e => e.message);
	const E100 = u => "E-JS-100: data source 's' url is not a same-origin path: '" + u + "'";

	out.pathToken = eq(U.resolveUrl('s', '/rest/{org}/series', { org: 'a b/c' }), { url: '/rest/a%20b%2Fc/series' });
	out.querySorted = eq(U.resolveUrl('s', '/s', { b: 2, a: 'x y' }), { url: '/s?a=x+y&b=2' });
	out.arrayRepeats = eq(U.resolveUrl('s', '/s', { r: ['a', 'b', null], z: null }), { url: '/s?r=a&r=b' });
	out.extraWins = eq(U.resolveUrl('s', '/s?f=1', { start: 'x' }, { start: '2026-10-01T00:00:00Z' }),
		{ url: '/s?f=1&start=2026-10-01T00%3A00%3A00Z' });
	out.templateQueryFirst = eq(U.resolveUrl('s', '/s?b=1&z=0', { b: 2, a: 3 }), { url: '/s?b=1&z=0&a=3&b=2' });
	out.fragment = eq(msg(() => U.resolveUrl('s', '/s?a=1#top', {})), E100('/s?a=1#top'));
	out.missingToken = eq(U.resolveUrl('s', '/r/{org}', {}), { missing: true });
	out.arrayInPath = eq(msg(() => U.resolveUrl('s', '/r/{org}', { org: ['a'] })), E100('/r/{org}'));
	out.objectInQuery = eq(msg(() => U.resolveUrl('s', '/r', { f: { a: 1 } })), E100('/r'));
	out.protocolRelative = eq(msg(() => U.resolveUrl('s', '//evil.example/x', {})), E100('//evil.example/x'));
	out.dotDotToken = eq(msg(() => U.resolveUrl('s', '/a/{p}/b', { p: '..' })), E100('/a/../b'));
	out.notString = eq(msg(() => U.resolveUrl('s', null, {})), E100('null'));
	out.inheritedNameIsMissing = eq(U.resolveUrl('s', '/r/{constructor}/{toString}', {}), { missing: true });
	out.ownNameWorks = eq(U.resolveUrl('s', '/r/{constructor}', { constructor: 'x' }), { url: '/r/x' });
	out.iso = eq([U.iso(Date.UTC(2026, 9, 1)), U.iso(Date.UTC(2026, 9, 1, 0, 0, 0, 5))],
		['2026-10-01T00:00:00Z', '2026-10-01T00:00:00.005Z']);

	// The fetch helper: one GET with same-origin credentials and the format's Accept, then the body as text pieces.
	const pieces = [];
	h.routes['/d'] = () => textResponse('a\n1\n');
	const res = await U.fetchData('s', '/d', null, U.ACCEPT.csv);
	await U.readBody(res, t => pieces.push(t));
	out.fetchInit = eq(h.fetches[0], { url: '/d', accept: 'text/csv', credentials: 'same-origin', method: 'GET', aborted: false });
	out.body = eq(pieces.join(''), 'a\n1\n');
	out.accept = eq(U.ACCEPT, { csv: 'text/csv', ndjson: 'application/x-ndjson', json: 'application/json' });

	h.routes['/503'] = () => textResponse('', { status: 503 });
	out.http503 = eq(await amsg(U.fetchData('s', '/503', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'HTTP 503'");
	h.routes['/down'] = () => Promise.reject(new TypeError('Failed to fetch'));
	out.network = eq(await amsg(U.fetchData('s', '/down', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'Failed to fetch'");

	// A multi-byte character split across two reads decodes once, whole.
	const bytes = new TextEncoder().encode('x,é\n');
	h.routes['/utf8'] = () => textResponse(null, { parts: [bytes.slice(0, 3), bytes.slice(3)] });
	const split = [];
	await U.readBody(await U.fetchData('s', '/utf8', null, 'text/csv'), t => split.push(t));
	out.splitUtf8 = eq(split, ['x,', 'é\n']);
	h.routes['/flat'] = () => textResponse('a\nb', { noStream: true });
	const flat = [];
	await U.readBody(await U.fetchData('s', '/flat', null, 'text/csv'), t => flat.push(t));
	out.noStream = eq(flat, ['a\nb']);

	// A throw from the text callback cancels the reader and passes through.
	const streamed = textResponse(null, { parts: ['a', 'b', 'c'] });
	h.routes['/cancel'] = () => streamed;
	const thrown = await amsg(U.readBody(await U.fetchData('s', '/cancel', null, 'text/csv'), () => { throw new Error('stop'); }));
	out.cancelOnThrow = eq([thrown, streamed.cancelled], ['stop', true]);

	// A cancel that itself rejects must not surface as an unhandled rejection (the process would exit non-zero).
	const badCancel = textResponse(null, { parts: ['a', 'b'] });
	const realGet = badCancel.body.getReader;
	badCancel.body.getReader = () => Object.assign(realGet(), { cancel: () => Promise.reject(new Error('already closed')) });
	h.routes['/badcancel'] = () => badCancel;
	out.cancelRejects = eq(await amsg(U.readBody(await U.fetchData('s', '/badcancel', null, 'text/csv'), () => { throw new Error('stop2'); })), 'stop2');
	await settle();

	// An abort is not an E-JS-62: the AbortError passes through, so the source can drop it silently.
	const ac = new AbortController();
	h.routes['/slow'] = () => deferred().promise;
	const pending = U.fetchData('s', '/slow', ac.signal, 'text/csv').then(() => null, e => [e.name, e.problem === undefined]);
	await settle();
	ac.abort();
	out.abortPassesThrough = eq(await pending, ['AbortError', true]);
	out.abortSeen = eq(h.fetches[h.fetches.length - 1].aborted, true);

	const before = h.fetches.length;
	out.unsafeAtFetch = eq(await amsg(U.fetchData('s', '//x/y', null, 'text/csv')), E100('//x/y'));
	out.noFetchWhenUnsafe = eq(h.fetches.length, before);

	// Hardening: unusable values, boundaries and transports.
	out.nanQuery = eq(msg(() => U.resolveUrl('s', '/r', { n: NaN })), E100('/r'));
	out.infinityPath = eq(msg(() => U.resolveUrl('s', '/r/{n}', { n: Infinity })), E100('/r/{n}'));
	out.nanInArray = eq(msg(() => U.resolveUrl('s', '/r', { n: [1, NaN] })), E100('/r'));
	// An empty-string path token is "no value yet" (a half-filled picker), like null; an empty query value is kept.
	out.emptyTokenMissing = eq(U.resolveUrl('s', '/rest/{org}/series', { org: '' }), { missing: true });
	out.emptyQueryKept = eq(U.resolveUrl('s', '/s', { f: '' }), { url: '/s?f=' });
	out.missingPlusNonScalar = eq(msg(() => U.resolveUrl('s', '/r/{a}/{b}', { b: ['x'] })), E100('/r/{a}/{b}'));
	out.trailingQuestion = eq(U.resolveUrl('s', '/s?', { a: 1 }), { url: '/s?a=1' });
	out.trailingAmpNoQuery = eq(U.resolveUrl('s', '/a&b&', { x: 1 }), { url: '/a&b&?x=1' });
	out.trailingAmp = eq(U.resolveUrl('s', '/s?x=1&', { a: 1 }), { url: '/s?x=1&a=1' });

	const nf = h.fetches.length;
	out.undefinedUrl = eq(await amsg(U.fetchData('s', undefined, null, 'text/csv')), E100('undefined'));
	out.missingResultUrl = eq(await amsg(U.fetchData('s', U.resolveUrl('s', '/r/{o}', {}).url, null, 'text/csv')), E100('undefined'));
	out.nonStringUrlNoFetch = eq(h.fetches.length, nf);
	out.default404 = eq(await amsg(U.fetchData('s', '/nowhere', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'HTTP 404'");

	// A non-OK body is released, and a cancel that rejects is not an unhandled rejection.
	const notOk = textResponse('oops', { status: 500 });
	h.routes['/500'] = () => notOk;
	await amsg(U.fetchData('s', '/500', null, 'text/csv'));
	out.nonOkCancelled = eq(notOk.cancelled, true);
	const badBody = textResponse('x', { status: 502 });
	badBody.body.cancel = () => Promise.reject(new Error('closed'));
	h.routes['/502'] = () => badBody;
	out.nonOkBadCancel = eq(await amsg(U.fetchData('s', '/502', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'HTTP 502'");
	await settle();

	// Only an AbortError passes through; a timeout or a custom abort reason is a real failure, E-JS-62.
	h.routes['/timeout'] = () => Promise.reject(new DOMException('signal timed out', 'TimeoutError'));
	out.timeoutIs62 = eq(await amsg(U.fetchData('s', '/timeout', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'signal timed out'");
	h.routes['/reason'] = () => Promise.reject('custom reason');
	out.customReasonIs62 = eq(await amsg(U.fetchData('s', '/reason', null, 'text/csv')), "E-JS-62: data source 's' fetch failed: 'custom reason'");

	// A signal that is already aborted rejects with the AbortError.
	const pre = new AbortController();
	pre.abort();
	out.preAborted = eq(await U.fetchData('s', '/d', pre.signal, 'text/csv').then(() => null, e => [e.name, e.problem === undefined]), ['AbortError', true]);

	// An empty body calls onText zero times on both paths; an abort during the body read passes through raw.
	const emptyS = [], emptyF = [];
	h.routes['/empty'] = () => textResponse('');
	await U.readBody(await U.fetchData('s', '/empty', null, 'text/csv'), t => emptyS.push(t));
	h.routes['/emptyflat'] = () => textResponse('', { noStream: true });
	await U.readBody(await U.fetchData('s', '/emptyflat', null, 'text/csv'), t => emptyF.push(t));
	out.emptyBody = eq([emptyS, emptyF], [[], []]);
	const abortRead = textResponse(null, { parts: ['a'] });
	const ag = abortRead.body.getReader;
	abortRead.body.getReader = () => Object.assign(ag(), { read: () => Promise.reject(new DOMException('aborted', 'AbortError')) });
	h.routes['/abortread'] = () => abortRead;
	out.abortDuringRead = eq(await U.readBody(await U.fetchData('s', '/abortread', null, 'text/csv'), () => {}).then(() => null, e => [e.name, e.problem === undefined]),
		['AbortError', true]);
	return out;
});

// ---- d_parsers: section D ------------------------------------------------------------------------------------

/** Runs a parser over the pieces: {records, dims, layout, truncated} or {error: [record, reason]}. */
function parseAll(create, pieces, opts) {
	const p = create(opts || {});
	let records = [];
	try {
		for (const s of pieces) records = records.concat(p.push(s));
		records = records.concat(p.end());
		return { records, dims: p.dims(), layout: p.layout(), truncated: p.truncated() };
	} catch (e) {
		return { error: [e.record, e.message] };
	}
}

/** Every split of text into three pieces parses the same as the whole text: the count of splits that differ. */
function everySplit(create, text, opts) {
	const whole = JSON.stringify(parseAll(create, [text], opts));
	let bad = 0, first = null;
	for (let i = 0; i <= text.length; i++)
		for (let j = i; j <= text.length; j++) {
			const got = JSON.stringify(parseAll(create, [text.slice(0, i), text.slice(i, j), text.slice(j)], opts));
			if (got !== whole && bad++ === 0) first = [i, j, got];
		}
	return { bad, first };
}

const CSV_FIX = 'start,status,ms\r\n2026-10-01T00:00:00Z,"open, waiting",12\n2026-10-01T01:00:00Z,"say ""hi""",\n'
	+ '2026-10-01T02:00:00Z,"",3.5\n2026-10-01T03:00:00Z,"two\nlines",-1e3\n#truncated,4\n';
const NDJSON_FIX = '{"t":1,"s":"a","ok":true}\n\r\n{"t":2,"s":null,"ok":false}\r\n{"$truncated":2}\n';

section('d_parsers', async () => {
	const h = fresh();
	const U = h.U, out = {};
	const csv = U.csvParser, nd = U.ndjsonParser, js = U.jsonParser;

	out.csvWhole = eq(parseAll(csv, [CSV_FIX]), {
		records: [['2026-10-01T00:00:00Z', 'open, waiting', '12'], ['2026-10-01T01:00:00Z', 'say "hi"', null],
			['2026-10-01T02:00:00Z', '', '3.5'], ['2026-10-01T03:00:00Z', 'two\nlines', '-1e3']],
		dims: [{ name: 'start', type: 'string' }, { name: 'status', type: 'string' }, { name: 'ms', type: 'number' }],
		layout: ['start', 'status', 'ms'], truncated: 4 });
	out.csvEverySplit = eq(everySplit(csv, CSV_FIX), { bad: 0, first: null });
	out.csvFieldCount = eq(parseAll(csv, ['a,b\n1\n']).error, [1, 'the record has 1 fields; the header has 2']);
	out.csvAfterSentinel = eq(parseAll(csv, ['a\n1\n#truncated,5\n2\n']).error, [2, 'a record follows the #truncated sentinel']);
	out.csvQuoteInside = eq(parseAll(csv, ['a\nx"y\n']).error, [1, 'a quote inside an unquoted field']);
	out.csvTextAfterQuote = eq(parseAll(csv, ['a\n"x"y\n']).error, [1, 'text follows a closing quote']);
	out.csvUnterminated = eq(parseAll(csv, ['a\n"x\n']).error, [1, 'an unterminated quoted field']);
	// A quoted "#truncated" is data, and so is a non-numeric cap.
	out.csvQuotedSentinel = eq(parseAll(csv, ['a,b\n"#truncated",5\n#truncated,x\n']).records, [['#truncated', '5'], ['#truncated', 'x']]);
	// Blank lines are skipped, CRLF ends a record, and the last record needs no newline.
	out.csvBlankLines = eq(parseAll(csv, ['a\n\n1\n\r\n2']).records, [['1'], ['2']]);
	out.csvInference = eq([parseAll(csv, ['n,s,e\n1,x,\n,y,\n2.5,z,\n']).dims, parseAll(csv, ['n\n1\nabc\n']).dims], [
		[{ name: 'n', type: 'number' }, { name: 's', type: 'string' }, { name: 'e', type: 'string' }], [{ name: 'n', type: 'string' }]]);
	const cols = [{ name: 'start', type: 'time' }, { name: 'ms', type: 'number' }];
	out.csvDeclared = eq(parseAll(csv, ['ms,start\n5,2026-10-01T00:00:00Z\n'], { columns: cols }),
		{ records: [['5', '2026-10-01T00:00:00Z']], dims: cols, layout: ['ms', 'start'], truncated: null });
	// Without declared columns, the first 100 rows are held for inference, then released together.
	const p = csv({});
	const first = p.push('n\n' + '1\n'.repeat(99));
	const dimsBefore = p.dims();
	const second = p.push('2\n3\n');
	out.csvHolds100 = eq([first.length, dimsBefore, second.length, p.dims()], [0, null, 101, [{ name: 'n', type: 'number' }]]);
	out.csvEmptyBody = eq(parseAll(csv, ['']), { records: [], dims: [], layout: null, truncated: null });

	out.ndWhole = eq(parseAll(nd, [NDJSON_FIX]), {
		records: [{ t: 1, s: 'a', ok: true }, { t: 2, s: null, ok: false }],
		dims: [{ name: 't', type: 'number' }, { name: 's', type: 'string' }, { name: 'ok', type: 'bool' }], layout: null, truncated: 2 });
	out.ndEverySplit = eq(everySplit(nd, NDJSON_FIX), { bad: 0, first: null });
	out.ndNotJson = eq(parseAll(nd, ['{"a":1}\nnope\n']).error, [2, 'the line is not JSON']);
	out.ndNotObject = eq(parseAll(nd, ['[1]\n']).error, [1, 'the line is not a JSON object']);
	out.ndAfterSentinel = eq(parseAll(nd, ['{"$truncated":1}\n{"a":1}\n']).error, [2, 'a record follows the $truncated sentinel']);
	out.ndMixedIsString = eq(parseAll(nd, ['{"a":1}\n{"a":"x"}']).dims, [{ name: 'a', type: 'string' }]);

	out.jsArray = eq(parseAll(js, ['[{"a":1}', ',{"a":2}]']), { records: [{ a: 1 }, { a: 2 }], dims: [{ name: 'a', type: 'number' }],
		layout: null, truncated: null });
	out.jsRowsTruncated = eq([parseAll(js, ['{"rows":[{"a":1}],"truncated":true}']).truncated,
		parseAll(js, ['{"rows":[],"truncated":50}']).truncated, parseAll(js, ['{"rows":[],"truncated":false}']).truncated], [1, 50, null]);
	out.jsNotJson = eq(parseAll(js, ['nope']).error, [0, 'the body is not JSON']);
	out.jsNotRows = eq(parseAll(js, ['{"x":1}']).error, [0, 'the body is not an array or {rows}']);
	out.jsRowNotObject = eq(parseAll(js, ['[{"a":1},3]']).error, [2, 'the row is not a JSON object']);

	out.builtinParsers = eq(h.D.builtins.filter(t => t[0] === 'parser').map(t => t[1]), ['csv', 'ndjson', 'json']);
	out.lookupCsv = eq(typeof U.lookup('parser', 'csv').impl.create, 'function');
	// A declared column list survives an empty body.
	out.csvDeclaredEmpty = eq(parseAll(csv, [''], { columns: cols }).dims, cols);
	// Header faults are record 0; blank and duplicate names are refused.
	out.csvHeaderFaults = eq([parseAll(csv, ['a,a\n1,2\n']).error, parseAll(csv, ['a,\n1,2\n']).error,
		parseAll(csv, ['a,""\n1,2\n']).error, parseAll(csv, ['"a"b\n']).error, parseAll(csv, ['a"\n']).error],
		[[0, "duplicate column 'a'"], [0, 'empty column name'], [0, 'empty column name'],
			[0, 'text follows a closing quote'], [0, 'a quote inside an unquoted field']]);
	// A leading byte-order mark is dropped, once, in every parser, wherever the first piece ends.
	out.bomCsv = eq(parseAll(csv, ['\uFEFF', 'a\n1\n']), { records: [['1']], dims: [{ name: 'a', type: 'number' }],
		layout: ['a'], truncated: null });
	out.bomNdjson = eq(parseAll(nd, ['\uFEFF{"a":1}\n']).records, [{ a: 1 }]);
	out.bomJson = eq(parseAll(js, ['\uFEFF[{"a":1}]']).records, [{ a: 1 }]);
	// A sentinel count is a non-negative integer in every format; anything else is data.
	out.sentinelCounts = eq([parseAll(nd, ['{"$truncated":-5}\n']).truncated, parseAll(nd, ['{"$truncated":-5}\n']).records,
		parseAll(nd, ['{"$truncated":0}\n']).truncated, parseAll(js, ['{"rows":[],"truncated":-5}']).truncated,
		parseAll(js, ['{"rows":[],"truncated":2.5}']).truncated, parseAll(csv, ['a,b\n#truncated,-5\n']).records],
		[null, [{ $truncated: -5 }], 0, null, null, [['#truncated', '-5']]]);
	// One long line over many pieces parses the same, and fast (linear in the line length).
	const longLine = '{"a":"' + 'x'.repeat(2000000) + '"}\n';
	const pieces = [];
	for (let i = 0; i < longLine.length; i += 64) pieces.push(longLine.slice(i, i + 64));
	const t0 = Date.now(), longRes = parseAll(nd, pieces);
	out.ndLongLine = eq([longRes.records.length, longRes.records[0].a.length, Date.now() - t0 < 2000], [1, 2000000, true]);
	return out;
});

// ---- e_frames: section E -------------------------------------------------------------------------------------

const T0 = Date.UTC(2026, 9, 1);
const DIMS4 = [{ name: 't', type: 'time' }, { name: 's', type: 'string' }, { name: 'n', type: 'number' }, { name: 'b', type: 'bool' }];

section('e_frames', async () => {
	const h = fresh();
	const U = h.U, out = {};
	const build = (dims, opts, records) => {
		const b = U.columnarBuilder(dims, opts);
		records.forEach(r => b.onRecord(r));
		return b.finish();
	};

	// Array records by layout (CSV): a missing position is null, an unparseable time is NaN, strings are interned.
	const fa = build(DIMS4, { layout: ['n', 't', 's', 'b', 'extra'] }, [
		['1', '2026-10-01T00:00:00Z', 'a', 'true', 'x'], [null, 'bad', 'a', '0'], ['2.5', T0 + 1, null, null, 'y']]);
	out.arrayRows = eq(U.rowsOf(fa), [{ t: T0, s: 'a', n: 1, b: true }, { t: null, s: 'a', n: null, b: false },
		{ t: T0 + 1, s: null, n: 2.5, b: null }]);
	out.arrayShape = eq([fa.rowCount, fa.truncated, Array.from(fa.columns.s.codes), fa.columns.s.dict, Array.from(fa.columns.b)],
		[3, false, [0, 0, -1], ['a'], [1, 0, -1]]);
	// Object records by own key: missing keys are null, extra keys ignored.
	const fo = build(DIMS4, {}, [{ t: T0, s: 'x', n: 3, b: false, extra: 1 }, {}]);
	out.objectRows = eq(U.rowsOf(fo), [{ t: T0, s: 'x', n: 3, b: false }, { t: null, s: null, n: null, b: null }]);
	// Doubling growth, trimmed by a copy at finish.
	const many = U.columnarBuilder([{ name: 'n', type: 'number' }], {});
	for (let i = 0; i < 3000; i++) many.onRecord({ n: i });
	const fm = many.finish();
	out.growth = eq([fm.rowCount, fm.columns.n.length, fm.columns.n.buffer.byteLength, fm.columns.n[2999]], [3000, 3000, 24000, 2999]);
	// A snapshot is a copy: later records do not reach it.
	const snap = many.snapshot();
	many.onRecord({ n: -1 });
	out.snapshotCopies = eq([snap.rowCount, many.rowCount(), many.finish().rowCount], [3000, 3001, 3001]);

	// concatFrames: by dim name, re-interned, nulls for a missing or mistyped dim, truncated ORed, fresh arrays.
	const dims3 = DIMS4.slice(0, 3);
	const A = build(dims3, {}, [{ t: T0, s: 'x', n: 1 }, { t: T0 + 1, s: 'y', n: 2 }]);
	const B = Object.assign(build([{ name: 't', type: 'time' }, { name: 's', type: 'string' }, { name: 'n', type: 'string' }], {},
		[{ t: T0 + 2, s: 'y', n: 'q' }, { t: T0 + 3, s: 'z', n: 'r' }]), { truncated: true });
	const C = build([{ name: 't', type: 'time' }], {}, [{ t: T0 + 4 }]);
	const all = U.concatFrames(dims3, [A, B, C]);
	out.concatRows = eq(U.rowsOf(all), [{ t: T0, s: 'x', n: 1 }, { t: T0 + 1, s: 'y', n: 2 }, { t: T0 + 2, s: 'y', n: null },
		{ t: T0 + 3, s: 'z', n: null }, { t: T0 + 4, s: null, n: null }]);
	out.concatShape = eq([all.rowCount, all.truncated, all.columns.s.dict], [5, true, ['x', 'y', 'z']]);
	out.concatCopies = eq([all.columns.t.buffer !== A.columns.t.buffer, U.concatFrames(dims3, [A]).columns.n !== A.columns.n], [true, true]);
	out.concatEmpty = eq(U.concatFrames(dims3, []), { dims: dims3, rowCount: 0, truncated: false,
		columns: { t: {}, s: { codes: {}, dict: [] }, n: {} } });

	// The aggregate response: every contract fault has its own reason (E-JS-96).
	const good = () => ({ schemaVersion: 1, truncated: false,
		dims: [{ name: 'bucket', type: 'time' }, { name: 'status', type: 'string' }, { name: 'count', type: 'number' }],
		columns: { bucket: [1789084800000, 1789084800000], status: ['open', 'closed'], count: [12, 40] } });
	out.aggregateGood = eq([U.aggregateProblem(good()), U.rowsOf(U.frameFromColumns(good()))],
		[null, [{ bucket: 1789084800000, status: 'open', count: 12 }, { bucket: 1789084800000, status: 'closed', count: 40 }]]);
	const bad = (edit) => { const b = good(); edit(b); return U.aggregateProblem(b); };
	// A group the server could not name (a row with no value) arrives as JSON null: accepted, coded -1, read back as null.
	const nullGroup = good();
	nullGroup.columns.status = [null, 'closed'];
	const nullGroupFrame = U.frameFromColumns(nullGroup);
	out.aggregateNullGroup = eq([U.aggregateProblem(nullGroup), Array.from(nullGroupFrame.columns.status.codes), nullGroupFrame.columns.status.dict,
		U.rowsOf(nullGroupFrame).map(r => r.status)], [null, [-1, 0], ['closed'], [null, 'closed']]);
	out.aggregateProblems = eq([
		U.aggregateProblem([]),
		bad(b => { b.schemaVersion = 2; }),
		bad(b => { b.dims = {}; }),
		bad(b => { delete b.columns; }),
		bad(b => { delete b.truncated; }),
		bad(b => { b.dims.push({ name: 'x', type: 'date' }); }),
		bad(b => { b.dims.push({ name: 'x', type: 'number' }); }),
		bad(b => { b.columns.count.push(1); }),
		bad(b => { b.columns.count[1] = '40'; }),
		bad(b => { b.dims[0].name = 'start'; b.columns.start = b.columns.bucket; })
	], ['the body is not an object', 'schemaVersion is not 1', 'dims is not an array', 'columns is not an object',
		'truncated is not a boolean', 'a dim is not {name, type}', "dim 'x' has no column", 'the columns have unequal lengths',
		"column 'count' holds a value that is not a number", 'there is no bucket time dim']);
	// A header that names built-in object keys keeps its columns through the builder, rowsOf and concatFrames, also
	// after a structured clone (a worker hand-off).
	const pcsv = U.csvParser({}), prec = pcsv.push('__proto__,constructor\n1,x\n2,y\n').concat(pcsv.end());
	const pf = build(pcsv.dims(), { layout: pcsv.layout() }, prec);
	const pdims = pcsv.dims();
	const expectP = [{ ['__proto__']: 1, constructor: 'x' }, { ['__proto__']: 2, constructor: 'y' }].map(r => JSON.stringify(r));
	const viaJson = r => JSON.stringify(Object.fromEntries(Object.entries(r)));
	out.protoColumns = eq([Object.keys(pf.columns), U.rowsOf(pf).map(r => Object.keys(r).join())],
		[['__proto__', 'constructor'], ['__proto__,constructor', '__proto__,constructor']]);
	const pc = U.concatFrames(pdims, [pf, structuredClone(pf)]);
	out.protoConcat = eq([Object.keys(pc.columns), pc.rowCount, Array.from(pc.columns['__proto__']),
		U.rowsOf(pc).map(r => r.constructor)], [['__proto__', 'constructor'], 4, [1, 2, 1, 2], ['x', 'y', 'x', 'y']]);
	// A dim the columns object only inherits is no column.
	const inherited = good();
	inherited.dims.push({ name: 'constructor', type: 'number' });
	const inheritedProto = good();
	inheritedProto.dims.push({ name: '__proto__', type: 'number' });
	out.inheritedColumn = eq([U.aggregateProblem(inherited), U.aggregateProblem(inheritedProto)],
		["dim 'constructor' has no column", "dim '__proto__' has no column"]);
	const ownProto = JSON.parse('{"schemaVersion":1,"truncated":false,"dims":[{"name":"bucket","type":"time"},'
		+ '{"name":"__proto__","type":"number"}],"columns":{"bucket":[1],"__proto__":[7]}}');
	out.ownProtoColumn = eq([U.aggregateProblem(ownProto), Object.keys(U.frameFromColumns(ownProto).columns)],
		[null, ['bucket', '__proto__']]);
	out.duplicateDim = eq(bad(b => { b.dims.push({ name: 'count', type: 'number' }); }), "duplicate dim 'count'");

	// Time cells: numeric text is epoch ms, like a number; other text must be ISO-8601 or it is null.
	const tf = build([{ name: 't', type: 'time' }], {}, [{ t: '1789084800000' }, { t: '12' }, { t: '2026' }, { t: 'Oct 1' },
		{ t: '2026-10-01' }, { t: '2026-10-01T00:00:00Z' }, { t: 5 }, { t: Infinity }, { t: '' }]);
	out.timeCells = eq(U.rowsOf(tf).map(r => r.t), [1789084800000, 12, 2026, null, T0, T0, 5, null, null]);
	// A date and time with no zone is UTC, whatever the host's zone (run this harness under TZ=America/New_York too).
	const zf = build([{ name: 't', type: 'time' }], {}, [{ t: '2026-10-01T00:00:00' }, { t: '2026-10-01 00:00:00' },
		{ t: '2026-10-01T00:00' }, { t: '2026-10-01T00:00:00.5' }, { t: '2026-10-01T02:00:00+02:00' }]);
	out.zoneLessTimeIsUtc = eq(U.rowsOf(zf).map(r => r.t), [T0, T0, T0, T0 + 500, T0]);
	const zp = U.csvParser({}), zr = zp.push('t\n2026-10-01T00:00:00\n').concat(zp.end());
	const zb = U.columnarBuilder([{ name: 't', type: 'time' }], { layout: zp.layout() });
	zr.forEach(r => zb.onRecord(r));
	out.csvZoneLessTimeIsUtc = eq(U.rowsOf(zb.finish()).map(r => r.t), [Date.UTC(2026, 9, 1)]);
	// Number cells: numeric text only, and finite.
	const nf = build([{ name: 'n', type: 'number' }], {}, [{ n: '  ' }, { n: '0x10' }, { n: 'Infinity' }, { n: '1e999' },
		{ n: true }, { n: [7] }, { n: '-1.5e2' }, { n: Infinity }, { n: '7' }]);
	out.numberCells = eq(U.rowsOf(nf).map(r => r.n), [null, null, null, null, null, null, -150, null, 7]);
	// An object or array in a string column is its JSON text.
	const sf = build([{ name: 's', type: 'string' }], {}, [{ s: { a: [1] } }, { s: [1, 2] }, { s: 3 }]);
	out.nestedStrings = eq(sf.columns.s.dict, ['{"a":[1]}', '[1,2]', '3']);
	out.builtinBuilder = eq(h.D.builtins.filter(t => t[0] === 'builder').map(t => t[1]), ['columnar']);
	return out;
});

// ---- f_chunks: section F ----------------------------------------------------------------------------------------------

const HOUR = 3600000, DAY = 24 * HOUR;
// 2026-10-01 is a Thursday; 2026-10-05 is a Monday.
const OCT1 = Date.UTC(2026, 9, 1), OCT5 = Date.UTC(2026, 9, 5);

section('f_chunks', async () => {
	const h = fresh();
	const U = h.U, out = {};
	const keys = list => list.map(c => [c.key, c.full, c.closed]);

	out.parseTime = eq([U.parseTime('2026-10-01T00:00:00Z'), U.parseTime(OCT1), U.parseTime(String(OCT1)), U.parseTime('nope'),
		U.parseTime(null), U.parseTime(''), U.parseTime(NaN), U.parseTime('2026-10-01'), U.parseTime('2026-10-01T00:00:00'),
		U.parseTime('2026-10-01T02:00:00+02:00'), U.parseTime('Oct 1 2026'), U.parseTime(true)],
		[OCT1, OCT1, OCT1, null, null, null, null, OCT1, OCT1, OCT1, null, null]);
	out.floor = eq([U.floorUnit(OCT1 + 90 * 60000, 'hour'), U.floorUnit(OCT1 + 5 * HOUR, 'day'), U.floorUnit(OCT1, 'week'),
		U.floorUnit(OCT1, 'week', 0), U.floorUnit(OCT5, 'week'), U.floorUnit(OCT1 + 20 * DAY, 'month')],
		[OCT1 + HOUR, OCT1, OCT1 - 3 * DAY, OCT1 - 4 * DAY, OCT5, OCT1]);
	out.floorBeforeEpoch = eq([U.floorUnit(-1, 'hour'), U.floorUnit(-1, 'day'), U.floorUnit(-1, 'week'), U.floorUnit(-1, 'month')],
		[-HOUR, -DAY, Date.UTC(1969, 11, 29), Date.UTC(1969, 11, 1)]);
	// A weekStart outside 0-6 (or not an integer) is the Monday default.
	out.weekStartValidated = eq([U.floorUnit(Date.UTC(2024, 0, 7, 5), 'week', 8), U.floorUnit(Date.UTC(2024, 0, 7, 5), 'week', 1.5),
		U.floorUnit(Date.UTC(2024, 0, 7, 5), 'week', -1), U.floorUnit(Date.UTC(2024, 0, 7, 5), 'week', 0)],
		[Date.UTC(2024, 0, 1), Date.UTC(2024, 0, 1), Date.UTC(2024, 0, 1), Date.UTC(2024, 0, 7)]);
	out.next = eq([U.nextUnit(OCT1, 'hour'), U.nextUnit(OCT1, 'day'), U.nextUnit(OCT5, 'week'), U.nextUnit(OCT1, 'month'),
		U.nextUnit(Date.UTC(2026, 11, 1), 'month'), U.nextUnit(Date.UTC(2026, 0, 1), 'month'), U.nextUnit(Date.UTC(2028, 1, 1), 'month')],
		[OCT1 + HOUR, OCT1 + DAY, OCT5 + 7 * DAY, Date.UTC(2026, 10, 1), Date.UTC(2027, 0, 1), Date.UTC(2026, 1, 1),
		Date.UTC(2028, 2, 1)]);
	// The server's rule (SeriesAggregator.autoUnit): the smallest unit with at most 1000 buckets, else month.
	out.autoUnit = eq([U.autoUnit(OCT1, OCT1 + 1000 * HOUR), U.autoUnit(OCT1, OCT1 + 1001 * HOUR), U.autoUnit(OCT1, OCT1 + 1001 * DAY),
		U.autoUnit(OCT1, OCT1 + 30000 * DAY)], ['hour', 'day', 'week', 'month']);

	// byDay over a window clipped at both ends; now inside the third day.
	const days = U.chunkList(OCT1 + 6 * HOUR, OCT1 + 3 * DAY + 2 * HOUR, 'day', OCT1 + 2 * DAY + HOUR);
	out.byDay = eq(keys(days), [[(OCT1 + 6 * HOUR) + '-' + (OCT1 + DAY), false, true], [String(OCT1 + DAY), true, true],
		[String(OCT1 + 2 * DAY), true, false], [(OCT1 + 3 * DAY) + '-' + (OCT1 + 3 * DAY + 2 * HOUR), false, false]]);
	out.bounds = eq([days[0].start, days[0].end, days[3].start, days[3].end, days[1].label],
		[OCT1 + 6 * HOUR, OCT1 + DAY, OCT1 + 3 * DAY, OCT1 + 3 * DAY + 2 * HOUR, '2026-10-02T00:00:00Z']);
	// byHour: the end of a chunk equal to now is closed.
	out.byHour = eq(keys(U.chunkList(OCT1, OCT1 + 3 * HOUR, 'hour', OCT1 + 2 * HOUR)),
		[[String(OCT1), true, true], [String(OCT1 + HOUR), true, true], [String(OCT1 + 2 * HOUR), true, false]]);
	// byWeek: weeks start Monday; 2026-10-01 falls in the week of 2026-09-28.
	out.byWeek = eq(keys(U.chunkList(OCT1 - 3 * DAY, OCT5 + 7 * DAY, 'week', OCT5 + 30 * DAY)),
		[[String(OCT1 - 3 * DAY), true, true], [String(OCT5), true, true]]);
	// A month is not a fixed length: February 2028 has 29 days.
	out.byMonth = eq(keys(U.chunkList(Date.UTC(2028, 1, 1), Date.UTC(2028, 3, 1), 'month', 0)),
		[[String(Date.UTC(2028, 1, 1)), true, false], [String(Date.UTC(2028, 2, 1)), true, false]]);
	out.emptyWindow = eq([U.chunkList(OCT1, OCT1, 'day', 0), U.chunkList(OCT1 + DAY, OCT1, 'day', 0),
		U.chunkList(OCT1 + 6 * HOUR, OCT1 + 3 * HOUR, 'day', 0)], [[], [], []]);

	const chunk = { unit: 'day', start: 'from', end: 'to' };
	out.window = eq(U.windowOf({ from: '2026-10-01T00:00:00Z', to: OCT1 + DAY }, chunk), { start: OCT1, end: OCT1 + DAY });
	out.windowMissing = eq([U.windowOf({ from: OCT1 }, chunk), U.windowOf({ from: 'x', to: OCT1 }, chunk)], [{ missing: true }, { missing: true }]);
	// Only own params count, and the params object may have no prototype.
	const np = Object.create(null);
	np.from = OCT1;
	np.to = OCT1 + DAY;
	out.windowOwnOnly = eq([U.windowOf(np, chunk), U.windowOf({}, { unit: 'day', start: 'constructor', end: 'toString' })],
		[{ start: OCT1, end: OCT1 + DAY }, { missing: true }]);
	return out;
});

// ---- g_cache: section G -----------------------------------------------------------------------------------------------

/** A chunk frame of number rows. */
function numFrame(U, values) {
	const b = U.columnarBuilder([{ name: 'v', type: 'number' }], {});
	values.forEach(v => b.onRecord({ v }));
	return b.finish();
}

section('g_cache', async () => {
	const out = {};
	const DEF = { url: '/rest/x/{org}', parser: 'csv', columns: null, chunk: { unit: 'day', start: 'start', end: 'end' } };
	const day = k => ({ key: String(OCT1 + k * DAY), start: OCT1 + k * DAY });
	const E101 = r => "E-JS-101: data source 's' cache unavailable (" + r + '); using memory';

	const idb = fakeIdb();
	const h = fresh({ idb });
	const U = h.U;
	const c = U.sourceCache('s', 'indexedDb');
	await c.open();
	const ns = await c.namespace(DEF, { org: 'a', start: 1, end: 2, q: ['x'] });
	out.nsHex = eq(/^[0-9a-f]{32}$/.test(ns), true);
	out.nsIgnoresWindowNullsAndOrder = eq(await c.namespace(DEF, { q: ['x'], end: 9, org: 'a', z: null }), ns);
	out.nsSeesParamsUrlAndColumns = eq([await c.namespace(DEF, { org: 'b', q: ['x'] }) !== ns,
		await c.namespace(Object.assign({}, DEF, { url: '/rest/y/{org}' }), { org: 'a', q: ['x'] }) !== ns,
		await c.namespace(Object.assign({}, DEF, { columns: [{ name: 'v', type: 'number' }] }), { org: 'a', q: ['x'] }) !== ns,
		await c.namespace(Object.assign({}, DEF, { chunk: { unit: 'week', start: 'start', end: 'end' } }), { org: 'a', q: ['x'] }) !== ns,
		await c.namespace(Object.assign({}, DEF, { chunk: { unit: 'day', start: 'from', end: 'to' } }), { org: 'a', q: ['x'] }) === ns],
		[true, true, true, true, true]);
	out.canonical = eq(U.canonicalJson({ b: [2, { d: 1, c: null }], a: 'x' }), '{"a":"x","b":[2,{"c":null,"d":1}]}');

	// Two closed days (one empty) written; three read in one transaction.
	const recs = [U.cacheRecord(ns, day(0), numFrame(U, [1, 2]), 'e1'), U.cacheRecord(ns, day(1), numFrame(U, []), 'e1')];
	out.recordShape = eq(Object.keys(recs[0]).sort(),
		['chunkStart', 'columns', 'dims', 'epoch', 'fetchedAt', 'key', 'ns', 'rowCount', 'schemaVersion']);
	out.recordKey = eq([recs[0].key, recs[0].chunkStart, recs[0].fetchedAt, recs[0].schemaVersion],
		[ns + '|' + OCT1, OCT1, h.clock.timers.now(), 1]);
	await c.write(recs);
	out.stored = eq(idb.records().map(r => [r.key, r.rowCount]), [[ns + '|' + OCT1, 2], [ns + '|' + (OCT1 + DAY), 0]]);
	const txs = idb.stats.txs;
	const got = await c.read(ns, [day(0), day(1), day(2)], 'e1');
	out.readOneTx = eq(idb.stats.txs - txs, 1);
	out.readHits = eq([Array.from(got.keys()), U.rowsOf(got.get(String(OCT1))), got.get(String(OCT1 + DAY)).rowCount],
		[[String(OCT1), String(OCT1 + DAY)], [{ v: 1 }, { v: 2 }], 0]);
	out.kind = eq(c.kind(), 'indexedDb');

	// An epoch mismatch drops this namespace only; a null epoch is not checked; a record without an epoch misses.
	await c.write([U.cacheRecord('other', day(0), numFrame(U, [9]), 'e1')]);
	out.epochMismatchMisses = eq((await c.read(ns, [day(0)], 'e2')).size, 0);
	out.epochDropsNamespace = eq(idb.records().map(r => r.key), ['other|' + OCT1]);
	out.nullEpochUnchecked = eq((await c.read('other', [day(0)], null)).size, 1);
	await c.write([U.cacheRecord(ns, day(0), numFrame(U, [1]), null)]);
	out.recordWithoutEpochMisses = eq((await c.read(ns, [day(0)], 'e1')).size, 0);
	await c.drop('other');
	out.drop = eq(idb.records().length, 0);
	out.openedOnce = eq([idb.stats.opens, U.sourceCache('t', 'indexedDb').open() === U.sourceCache('u', 'indexedDb').open()], [1, false]);
	out.noWarnings = eq(h.warns, []);

	// A stored record that is not a usable chunk (another schema version, no columns) is a miss, not a crash.
	const bad = U.cacheRecord('bad', day(0), numFrame(U, [1]), null);
	await c.write([Object.assign({}, bad, { key: 'bad|1', schemaVersion: 2 }), Object.assign({}, bad, { key: 'bad|2', columns: null })]);
	out.unusableRecordsMiss = eq((await c.read('bad', [{ key: '1' }, { key: '2' }], null)).size, 0);

	// The 30-day sweep at open.
	const now = h.clock.timers.now();
	const old = fakeIdb({ seed: [{ key: 'n|1', ns: 'n', fetchedAt: now - 31 * DAY }, { key: 'n|2', ns: 'n', fetchedAt: now - 29 * DAY }] });
	const hold = fresh({ idb: old });
	await hold.U.sourceCache('s', 'indexedDb').open();
	await idle(hold, 0);
	out.sweep = eq(old.records().map(r => r.key), ['n|2']);

	// Fallbacks: one E-JS-101 per page, whichever source meets it first.
	const seen = [];
	for (const o of [{ idb: null }, { idb: fakeIdb({ failOpen: true }) }, { idb: fakeIdb(), noSubtle: true }]) {
		const hf = fresh(o);
		const a = hf.U.sourceCache('s', 'indexedDb'), b = hf.U.sourceCache('t', 'indexedDb');
		await a.open();
		await b.open();
		seen.push([a.kind(), b.kind(), hf.warns]);
	}
	out.fallbacks = eq(seen, [['memory-fallback', 'memory-fallback', [E101('no indexedDB')]],
		['memory-fallback', 'memory-fallback', [E101('open failed: open refused')]],
		['memory-fallback', 'memory-fallback', [E101('no crypto.subtle')]]]);
	const hn = fresh({ noSubtle: true });
	out.nsWithoutSubtle = eq(await hn.U.sourceCache('s', 'memory').namespace(DEF, { org: 'a' }),
		'{"chunk":"day","columns":null,"format":"csv","params":{"org":"a"},"schemaVersion":1,"url":"/rest/x/{org}"}');

	// A quota error on write falls back, and the records land in memory.
	const qi = fakeIdb({ quotaOnPut: true });
	const hq = fresh({ idb: qi });
	const cq = hq.U.sourceCache('s', 'indexedDb');
	const nq = await cq.namespace(DEF, {});
	await cq.write([hq.U.cacheRecord(nq, day(0), numFrame(hq.U, [5]), null)]);
	out.quota = eq([cq.kind(), qi.records().length, hq.U.rowsOf((await cq.read(nq, [day(0)], null)).get(String(OCT1))), hq.warns],
		['memory-fallback', 0, [{ v: 5 }], [E101('quota exceeded')]]);

	// A synchronous put failure aborts the transaction, so none of its records survive in IndexedDB, and all go to memory.
	const si = fakeIdb();
	const hs = fresh({ idb: si });
	const cs = hs.U.sourceCache('s', 'indexedDb');
	const good = hs.U.cacheRecord('sp', day(0), numFrame(hs.U, [7]), null);
	const odd = Object.assign(hs.U.cacheRecord('sp', day(1), numFrame(hs.U, [8]), null), { odd: () => 1 });
	await cs.write([good, odd]);
	out.syncPutFailureAborts = eq([si.stats.aborts, si.records().length, cs.kind(), hs.warns.length,
		hs.U.rowsOf((await cs.read('sp', [day(0), day(1)], null)).get(String(OCT1)))], [1, 0, 'memory-fallback', 1, [{ v: 7 }]]);

	// The sweep uses the fetchedAt index and does not hold up open: a stale record is still there when open resolves.
	const sw = fakeIdb({ seed: [{ key: 'n|1', ns: 'n', fetchedAt: now - 31 * DAY }, { key: 'n|2', ns: 'n', fetchedAt: now - 29 * DAY }] });
	const hw = fresh({ idb: sw });
	await hw.U.sourceCache('s', 'indexedDb').open();
	const beforeSweep = sw.records().map(r => r.key);
	await idle(hw, 0);
	out.openDoesNotWaitForSweep = eq([beforeSweep, sw.records().map(r => r.key)], [['n|1', 'n|2'], ['n|2']]);
	out.sweepReadsNoRecords = eq(sw.stats.gets, 0);
	out.versionChangeCloses = eq([typeof sw.dbs[0].onversionchange, (sw.dbs[0].onversionchange(), sw.dbs[0].closed)], ['function', true]);

	// An open that never answers falls back to memory after the open timeout; a normal open leaves no timer behind.
	const hang = fresh({ idb: fakeIdb({ hangOpen: true }) });
	const ch = hang.U.sourceCache('s', 'indexedDb');
	const pend = ch.open();
	hang.clock.advance(4999);
	const early = ch.kind();
	hang.clock.advance(1);
	await pend;
	out.openTimeout = eq([early, ch.kind(), hang.warns], ['indexedDb', 'memory-fallback', [E101('open failed: timeout')]]);
	// A database that opens after the timeout is closed, and the source stays on memory.
	const lclock = fakeClock();
	const lidb = fakeIdb({ lateOpen: { clock: lclock, ms: 6000 } });
	const hl = fresh({ idb: lidb, clock: lclock });
	const cl = hl.U.sourceCache('s', 'indexedDb');
	const lp = cl.open();
	hl.clock.advance(5000);
	await lp;
	const openedBeforeLate = lidb.dbs.length;
	hl.clock.advance(1000);
	out.lateOpenIsClosed = eq([openedBeforeLate, lidb.dbs.length, lidb.dbs[0] && lidb.dbs[0].closed, cl.kind()],
		[0, 1, true, 'memory-fallback']);
	const hok = fresh({ idb: fakeIdb() });
	await hok.U.sourceCache('s', 'indexedDb').open();
	out.openClearsTimer = eq(hok.clock.pending(), 0);

	// cache: memory and cache: none.
	const hm = fresh();
	const cm = hm.U.sourceCache('s', 'memory'), cn = hm.U.sourceCache('n', 'none');
	await cm.write([hm.U.cacheRecord('m', day(0), numFrame(hm.U, [3]), null)]);
	await cn.write([hm.U.cacheRecord('m2', day(0), numFrame(hm.U, [3]), null)]);
	out.memoryAndNone = eq([cm.kind(), (await cm.read('m', [day(0)], null)).size, cn.kind(), (await cn.read('m2', [day(0)], null)).size,
		hm.warns], ['memory', 1, null, 0, []]);

	// The LRU, with small caps: by count, by typed-array bytes, and drop by prefix.
	const lru = hm.U.memoryLru(2, 100);
	const f8 = n => ({ columns: { v: new Float64Array(n) } });
	lru.set('a', f8(1));
	lru.set('b', f8(1));
	lru.get('a');
	lru.set('c', f8(1));
	out.lruByCount = eq([lru.get('a') != null, lru.get('b'), lru.get('c') != null, lru.size()], [true, undefined, true, 2]);
	lru.set('big', f8(12));
	out.lruByBytes = eq([lru.size(), lru.bytes(), lru.get('big') != null], [1, 96, true]);
	lru.set('p|1', { columns: { s: { codes: new Int32Array(1), dict: ['x'] } } });
	lru.drop('big');
	out.lruDrop = eq([lru.size(), lru.bytes()], [1, 4]);
	// An entry bigger than the byte cap is not stored, and evicts nothing.
	const lru2 = hm.U.memoryLru(5, 100);
	lru2.set('a', f8(1));
	lru2.set('huge', f8(20));
	out.lruSkipsOversized = eq([lru2.get('huge'), lru2.get('a') != null, lru2.size(), lru2.bytes()], [undefined, true, 1, 8]);
	lru2.set('a', f8(20));
	out.lruOversizedReplacementDropsOld = eq([lru2.get('a'), lru2.size(), lru2.bytes()], [undefined, 0, 0]);
	return out;
});

// ---- h_source: section H ----------------------------------------------------------------------------------------------

const TV = [{ name: 't', type: 'time' }, { name: 'v', type: 'number' }];
const BY_DAY = { unit: 'day', start: 'start', end: 'end' };
const RANGE = { topic: 'filter:range', as: 'params', map: { start: 'start', end: 'end' } };
// Thursday 2026-10-04 12:00 UTC: Oct 1-3 are closed days, Oct 4 is the open one.
const NOW = OCT1 + 3 * DAY + 12 * HOUR;

/** A source def with every default written. */
function SRC(o) {
	return Object.assign({ id: 's', kind: 'source', url: '/rest/s', parser: 'csv', columns: TV, subscribes: [RANGE], chunk: BY_DAY,
		concurrency: 1, debounceMs: 250, cache: 'memory', mode: 'raw', builder: 'columnar' }, o);
}

/** A page at NOW: every data:s handle is kept in h.handles, and h.range(start, end) publishes filter:range. */
function page(opts) {
	const h = fresh(opts);
	h.clock.advance(NOW - h.clock.timers.now());
	h.handles = [];
	h.bus.subscribe('data:s', p => h.handles.push(p));
	const range = h.bus.owner('test');
	h.range = (start, end) => range.publish('filter:range', { schemaVersion: 1, start: start, end: end });
	h.last = () => h.handles[h.handles.length - 1];
	h.rows = () => h.U.rowsOf(h.D.get(h.last()));
	return h;
}

/** A handle as [status, version, progress.loaded, rowCount, mode, cache, error code]. */
function brief(p) {
	return [p.status, p.version, p.progress ? p.progress.loaded : null, p.rowCount, p.mode || null, p.cache || null,
		p.error ? p.error.code : null];
}

/** n rows of CSV for the day starting at s: t is s plus i hours, v is i. */
function dayCsv(U, s, n) {
	let text = 't,v\n';
	for (let i = 0; i < n; i++) text += U.iso(s + i * HOUR) + ',' + i + '\n';
	return text;
}

/** Routes path by the request's start param: body(dayStart, url) gives the Response (or a promise of one). */
function byStart(h, path, body) {
	h.routes[path] = url => body(Date.parse(new URLSearchParams(url.split('?')[1] || '').get('start')), url);
}

/** The day index (0 for Oct 1) of each fetch of path, in request order. */
function fetchedDays(h, path) {
	return h.fetches.filter(f => f.url.split('?')[0] === path)
		.map(f => (Date.parse(new URLSearchParams(f.url.split('?')[1]).get('start')) - OCT1) / DAY);
}

section('h_source', async () => {
	const out = {};

	// Debounce, newest-first fetch, time-ordered frame, the cache serving closed days on refresh (memory cache).
	const h = page();
	byStart(h, '/rest/s', s => textResponse(dayCsv(h.U, s, (s - OCT1) / DAY + 1)));
	h.range(OCT1, OCT1 + 4 * DAY);
	h.U.createSource(SRC(), h.bus, {});
	await idle(h, 249);
	out.debounced = eq([h.fetches.length, h.handles.map(brief)], [0, [['loading', 0, 0, 0, 'raw', 'memory', null]]]);
	await idle(h, 1);
	out.newestFirst = eq(fetchedDays(h, '/rest/s'), [3, 2, 1, 0]);
	out.chunkUrl = eq(h.fetches[0].url, '/rest/s?end=2026-10-05T00%3A00%3A00Z&start=2026-10-04T00%3A00%3A00Z');
	out.firstLoad = eq(h.handles.map(brief), [['loading', 0, 0, 0, 'raw', 'memory', null], ['loading', 0, 10, 0, 'raw', 'memory', null],
		['ready', 1, null, 10, 'raw', 'memory', null]]);
	out.readyHandle = eq([h.last().dims, h.last().truncated, h.last().truncatedAt, h.last().progress, h.last().loadedAt, h.last().error,
		h.last().schemaVersion], [TV, false, null, null, h.U.iso(NOW + 250), null, 1]);
	out.timeOrder = eq(h.rows().map(r => [(r.t - OCT1) / HOUR, r.v]),
		[[0, 0], [24, 0], [25, 1], [48, 0], [49, 1], [50, 2], [72, 0], [73, 1], [74, 2], [75, 3]]);
	const cmd = h.bus.owner('ribbon');
	cmd.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(h, 250);
	out.refreshUsesCache = eq([fetchedDays(h, '/rest/s').slice(4), brief(h.last()), h.rows().length], [[3], ['ready', 2, null, 10, 'raw', 'memory', null], 10]);
	cmd.publish('cmd:s', { schemaVersion: 1, op: 'reload', options: { clearCache: true } });
	await idle(h, 250);
	out.clearCacheRefetches = eq(fetchedDays(h, '/rest/s').slice(5), [3, 2, 1, 0]);
	cmd.publish('cmd:s', { schemaVersion: 1, op: 'zoomTo' });
	await idle(h, 250);
	out.otherOpsIgnored = eq(h.fetches.length, 9);

	// Evicting every cached chunk leaves the live frame's columns intact.
	const before = JSON.stringify(h.rows());
	for (let i = 0; i < 64; i++) h.U.memoryCache.set('junk|' + i, { columns: { v: new Float64Array(1) } });
	out.evictionKeepsFrame = eq([h.U.memoryCache.size(), JSON.stringify(h.rows()) === before], [64, true]);

	// Awaiting: no window yet, so no request.
	const w = page();
	w.U.createSource(SRC(), w.bus, {});
	await idle(w, 300);
	out.awaiting = eq([w.fetches.length, w.handles.map(brief)], [0, [['loading', 0, 0, 0, 'raw', 'memory', null]]]);

	// A late response from a superseded generation is dropped (no AbortController, so the fetch is not cancelled).
	const late = page();
	late.D.config.platform.AbortController = null;
	const waits = [];
	byStart(late, '/rest/s', () => { const d = deferred(); waits.push(d); return d.promise; });
	late.range(OCT1, OCT1 + DAY);
	late.U.createSource(SRC(), late.bus, {});
	await idle(late, 250);
	late.range(OCT1 + DAY, OCT1 + 2 * DAY);
	await idle(late, 250);
	waits[1].resolve(textResponse(dayCsv(late.U, OCT1 + DAY, 2)));
	await idle(late, 0);
	const settled = late.handles.length;
	waits[0].resolve(textResponse(dayCsv(late.U, OCT1, 5)));
	await idle(late, 0);
	out.lateDropped = eq([waits.length, settled, late.handles.length, brief(late.last())], [2, 3, 3, ['ready', 1, null, 2, 'raw', 'memory', null]]);

	// A new generation aborts the running fetch, and the aborted generation publishes nothing.
	const ab = page();
	byStart(ab, '/rest/s', () => deferred().promise);
	ab.range(OCT1, OCT1 + DAY);
	ab.U.createSource(SRC(), ab.bus, {});
	await idle(ab, 250);
	const n0 = ab.handles.length;
	ab.range(OCT1 + DAY, OCT1 + 2 * DAY);
	await idle(ab, 100);
	out.abort = eq([ab.fetches.map(f => f.aborted), ab.handles.length - n0], [[true], 0]);

	// Progress: at most every 250 ms plus once at the end, total from X-Total-Rows (unchunked), partial snapshots.
	const pr = page();
	const parts = ['t,v\n'].concat(Array.from({ length: 10 }, (_, i) => pr.U.iso(OCT1 + i * HOUR) + ',' + i + '\n'));
	pr.routes['/rest/p'] = () => {
		const res = textResponse(null, { parts: parts, headers: { 'X-Total-Rows': '10' } });
		const getReader = res.body.getReader;
		res.body.getReader = () => {
			const r = getReader();
			const read = r.read;
			r.read = () => { pr.clock.advance(100); return read(); };
			return r;
		};
		return res;
	};
	const seen = [];
	pr.bus.subscribe('data:s', p => {
		const b = pr.D.get(Object.assign({}, p, { partial: true }));
		seen.push([p.status, p.progress ? p.progress.loaded : null, p.progress ? p.progress.total : null, b ? b.rowCount : null]);
	});
	pr.U.createSource(SRC({ url: '/rest/p', subscribes: [], chunk: null }), pr.bus, { partial: true });
	await idle(pr, 250);
	out.progress = eq(seen, [['loading', 0, null, null], ['loading', 2, 10, 2], ['loading', 5, 10, 5], ['loading', 8, 10, 8],
		['loading', 10, 10, 10], ['ready', null, null, null]]);
	out.unchunkedUrl = eq(pr.fetches.map(f => f.url), ['/rest/p']);

	// A parse error fails its chunk only: the good closed days are cached, the next refresh re-fetches one day.
	const idb = fakeIdb();
	const pe = page({ idb });
	let broken = true;
	byStart(pe, '/rest/s', s => textResponse(s === OCT1 + DAY && broken ? 't,v\n"x\n' : dayCsv(pe.U, s, 2)));
	pe.range(OCT1, OCT1 + 3 * DAY);
	pe.U.createSource(SRC({ cache: 'indexedDb' }), pe.bus, {});
	await idle(pe, 250);
	const E99 = "data source 's' chunk '2026-10-02T00:00:00Z' failed to parse at record 1: 'an unterminated quoted field'";
	out.parseError = eq([brief(pe.last()), pe.last().error.message, pe.errors.slice()], [['error', 0, null, 0, null, 'indexedDb', 'E-JS-99'], E99, ['E-JS-99: ' + E99]]);
	out.goodChunksCached = eq(idb.records().map(r => (r.chunkStart - OCT1) / DAY).sort(), [0, 2]);
	broken = false;
	pe.bus.owner('ribbon').publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(pe, 250);
	out.refetchFailedOnly = eq([fetchedDays(pe, '/rest/s').slice(3), brief(pe.last())], [[1], ['ready', 1, null, 6, 'raw', 'indexedDb', null]]);

	// The write rule: closed days are written, an empty one too; open and truncated days are not.  E-JS-102 once.
	const wi = fakeIdb();
	const wr = page({ idb: wi });
	byStart(wr, '/rest/s', s => textResponse(s === OCT1 + DAY ? 't,v\n' : s === OCT1 + 2 * DAY ? dayCsv(wr.U, s, 2) + '#truncated,2\n'
		: dayCsv(wr.U, s, 1)));
	wr.range(OCT1, OCT1 + 4 * DAY);
	wr.U.createSource(SRC({ cache: 'indexedDb' }), wr.bus, {});
	await idle(wr, 250);
	out.writeRule = eq(wi.records().map(r => [(r.chunkStart - OCT1) / DAY, r.rowCount, r.epoch]).sort(), [[0, 1, null], [1, 0, null]]);
	out.truncated = eq([brief(wr.last()), wr.last().truncated, wr.last().truncatedAt, wr.warns.slice()],
		[['ready', 1, null, 4, 'raw', 'indexedDb', null], true, 2, ["E-JS-102: data source 's' truncated at 2 rows"]]);

	// The epoch: records carry it; a changed epoch drops the namespace, so every closed day is fetched again.
	const ei = fakeIdb();
	const ep = page({ idb: ei });
	let epoch = 'v1';
	ep.routes['/rest/epoch'] = () => textResponse(JSON.stringify({ session: { epoch: epoch } }));
	byStart(ep, '/rest/s', s => textResponse(dayCsv(ep.U, s, 1)));
	ep.range(OCT1, OCT1 + 2 * DAY);
	ep.U.createSource(SRC({ cache: 'indexedDb', epoch: { url: '/rest/epoch', path: 'session.epoch' } }), ep.bus, {});
	await idle(ep, 250);
	out.epochStored = eq(ei.records().map(r => r.epoch), ['v1', 'v1']);
	const cmdE = ep.bus.owner('ribbon');
	cmdE.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(ep, 250);
	epoch = 'v2';
	cmdE.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(ep, 250);
	out.epochChange = eq([fetchedDays(ep, '/rest/s'), ei.records().map(r => r.epoch), ep.fetches.filter(f => f.url === '/rest/epoch').length],
		[[1, 0, 1, 0], ['v2', 'v2'], 3]);

	// AUTO: past maxRows the source aborts, keeps the committed day, and loads AGGREGATE for the same window.
	const ai = fakeIdb();
	const au = page({ idb: ai });
	byStart(au, '/rest/s', s => textResponse(dayCsv(au.U, s, 3)));
	au.routes['/rest/agg'] = () => textResponse(JSON.stringify({ schemaVersion: 1, truncated: false,
		dims: [{ name: 'bucket', type: 'time' }, { name: 'count', type: 'number' }], columns: { bucket: [OCT1, OCT1 + DAY, OCT1 + 2 * DAY], count: [3, 3, 3] } }));
	au.range(OCT1, OCT1 + 3 * DAY);
	au.U.createSource(SRC({ cache: 'indexedDb', mode: 'auto', maxRows: 5, aggregateUrl: '/rest/agg',
		aggregate: { bucket: 'day', groupBy: ['org'], measures: ['count', 'sum(ms)'] } }), au.bus, {});
	await idle(au, 250);
	out.autoRequests = eq([fetchedDays(au, '/rest/s'), au.fetches[2].url], [[2, 1],
		'/rest/agg?bucket=day&end=2026-10-04T00%3A00%3A00Z&groupBy=org&measure=count&measure=sum%28ms%29&start=2026-10-01T00%3A00%3A00Z&tz=UTC']);
	out.autoHandles = eq(au.handles.map(brief), [['loading', 0, 0, 0, 'raw', 'indexedDb', null], ['loading', 0, 0, 0, 'aggregate', 'indexedDb', null],
		['loading', 0, 3, 0, 'aggregate', 'indexedDb', null], ['ready', 1, null, 3, 'aggregate', 'indexedDb', null]]);
	out.autoCommittedStays = eq(ai.records().map(r => (r.chunkStart - OCT1) / DAY), [2]);
	out.autoFrame = eq([au.D.get(au.last()).bucketUnit, au.D.get(au.last()).mode, au.rows()[0], au.warns],
		['day', 'aggregate', { bucket: OCT1, count: 3 }, []]);
	// A truncated chunk switches too, without E-JS-102.
	const at = page();
	byStart(at, '/rest/s', s => textResponse(dayCsv(at.U, s, 1) + '#truncated,1\n'));
	at.routes['/rest/agg'] = au.routes['/rest/agg'];
	at.range(OCT1, OCT1 + 3 * DAY);
	at.U.createSource(SRC({ mode: 'auto', maxRows: 1000, aggregateUrl: '/rest/agg', aggregate: { bucket: 'auto', groupBy: [], measures: [] } }), at.bus, {});
	await idle(at, 250);
	out.autoOnTruncated = eq([fetchedDays(at, '/rest/s'), at.D.get(at.last()).bucketUnit, brief(at.last()), at.warns],
		[[2], 'hour', ['ready', 1, null, 3, 'aggregate', 'memory', null], []]);
	// AUTO without an aggregate endpoint stays raw past maxRows and on a truncated chunk; it never raises E-JS-100.
	const nm = page();
	byStart(nm, '/rest/s', s => textResponse(dayCsv(nm.U, s, 3)));
	nm.range(OCT1, OCT1 + 3 * DAY);
	nm.U.createSource(SRC({ mode: 'auto', maxRows: 5 }), nm.bus, {});
	await idle(nm, 250);
	out.autoNoEndpointMaxRows = eq([fetchedDays(nm, '/rest/s'), brief(nm.last()), nm.errors],
		[[2, 1, 0], ['ready', 1, null, 9, 'raw', 'memory', null], []]);
	const nt = page();
	byStart(nt, '/rest/s', s => textResponse(dayCsv(nt.U, s, 1) + '#truncated,1\n'));
	nt.range(OCT1, OCT1 + 3 * DAY);
	nt.U.createSource(SRC({ mode: 'auto', maxRows: 1000 }), nt.bus, {});
	await idle(nt, 250);
	out.autoNoEndpointTruncated = eq([nt.last().status, nt.last().mode, nt.last().error,
		nt.errors.some(e => e.indexOf('E-JS-100') >= 0)], ['ready', 'raw', null, false]);
	// AGGREGATE: a body that breaks the contract is E-JS-96.
	const ag = page();
	ag.routes['/rest/agg'] = () => textResponse('{"schemaVersion":2}');
	ag.range(OCT1, OCT1 + DAY);
	ag.U.createSource(SRC({ mode: 'aggregate', aggregateUrl: '/rest/agg', aggregate: { bucket: 'day', groupBy: [], measures: [] } }), ag.bus, {});
	await idle(ag, 250);
	out.aggregateContract = eq([brief(ag.last()), ag.last().error.message], [['error', 0, null, 0, null, 'memory', 'E-JS-96'],
		"data source 's' aggregate response violates the contract: schemaVersion is not 1"]);

	// concurrency 3: chunks finish out of order, and the frame is still in time order.
	const cc = page();
	const pend = new Map();
	byStart(cc, '/rest/s', s => { const d = deferred(); pend.set((s - OCT1) / DAY, d); return d.promise; });
	cc.range(OCT1, OCT1 + 4 * DAY);
	cc.U.createSource(SRC({ concurrency: 3 }), cc.bus, {});
	await idle(cc, 250);
	const inFlight = Array.from(pend.keys());
	for (const k of [1, 3, 2]) {
		pend.get(k).resolve(textResponse(dayCsv(cc.U, OCT1 + k * DAY, 1)));
		await idle(cc, 0);
	}
	pend.get(0).resolve(textResponse(dayCsv(cc.U, OCT1, 1)));
	await idle(cc, 0);
	out.concurrency = eq([inFlight, fetchedDays(cc, '/rest/s'), cc.rows().map(r => (r.t - OCT1) / DAY), brief(cc.last())],
		[[3, 2, 1], [3, 2, 1, 0], [0, 1, 2, 3], ['ready', 1, null, 4, 'raw', 'memory', null]]);
	// An abort mid-load (two of three chunks done) publishes nothing, and nothing is put.
	const mid = page();
	const md = new Map();
	byStart(mid, '/rest/s', s => { const d = deferred(); md.set((s - OCT1) / DAY, d); return d.promise; });
	mid.range(OCT1, OCT1 + 3 * DAY);
	mid.U.createSource(SRC({ concurrency: 3 }), mid.bus, {});
	await idle(mid, 250);
	md.get(2).resolve(textResponse(dayCsv(mid.U, OCT1 + 2 * DAY, 1)));
	md.get(0).resolve(textResponse(dayCsv(mid.U, OCT1, 1)));
	await idle(mid, 0);
	const m0 = mid.handles.length;
	mid.range(OCT1, OCT1 + DAY);
	await idle(mid, 100);
	out.abortMidLoad = eq([mid.handles.length - m0, mid.D.get(Object.assign({}, mid.last(), { version: 1 })), mid.fetches.length,
		mid.fetches[1].aborted], [0, null, 3, true]);

	// A failed fetch: error with no frame, stale once one exists (the frame is still served); E-JS-62 is logged.
	const fl = page();
	let status = 503;
	byStart(fl, '/rest/s', s => status === 200 ? textResponse(dayCsv(fl.U, s, 1)) : textResponse('', { status: status }));
	fl.range(OCT1, OCT1 + DAY);
	fl.U.createSource(SRC({ cache: 'none' }), fl.bus, {});
	await idle(fl, 250);
	const E62 = "data source 's' fetch failed: 'HTTP 503'";
	out.fetchError = eq([brief(fl.last()), fl.last().error, fl.errors.slice()], [['error', 0, null, 0, null, null, 'E-JS-62'], { code: 'E-JS-62', message: E62 },
		['E-JS-62: ' + E62]]);
	status = 200;
	const cmdF = fl.bus.owner('ribbon');
	cmdF.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(fl, 250);
	status = 503;
	cmdF.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(fl, 250);
	out.stale = eq([brief(fl.last()), fl.rows().length, 'cache' in fl.last()], [['stale', 1, null, 1, 'raw', null, 'E-JS-62'], 1, false]);

	// Unknown parser (E-JS-92); dispose releases the frames.
	const up = page();
	up.range(OCT1, OCT1 + DAY);
	up.U.createSource(SRC({ parser: 'app.nope' }), up.bus, {});
	await idle(up, 250);
	out.unknownParser = eq(up.last().error.message, "data source 's' uses unknown parser 'app.nope'; registered: 'csv, json, ndjson'");
	const dsrc = h.U.createSource(SRC({ id: 't' }), h.bus, {});
	await idle(h, 250);
	const tHandle = h.bus.get('data:t');
	dsrc.dispose();
	out.dispose = eq([tHandle.status, h.D.get(tHandle), h.bus.get('data:t')], ['ready', null, undefined]);
	// More than MAX_CHUNKS chunks is counted, never listed.
	const U = h.U, lim = U.MAX_CHUNKS;
	out.chunkLimit = eq([lim, U.overChunkLimit(OCT1, OCT1 + lim * DAY, 'day', lim),
		U.overChunkLimit(OCT1, OCT1 + (lim + 1) * DAY, 'day', lim), U.overChunkLimit(OCT1 + 1, OCT1 + lim * DAY + 1, 'day', lim),
		U.overChunkLimit(OCT1, OCT1, 'day', lim), U.overChunkLimit(OCT1 + DAY, OCT1, 'day', lim),
		U.overChunkLimit(Date.UTC(2026, 9, 1), Date.UTC(2026, 9 + lim, 1), 'month', lim),
		U.overChunkLimit(Date.UTC(2026, 9, 1), Date.UTC(2026, 9 + lim, 1) + DAY, 'month', lim),
		U.overChunkLimit(0, 1e300, 'hour', lim), U.overChunkLimit(0, 8.64e15, 'month', lim)],
		[10000, false, true, true, false, false, false, true, true, true]);
	const wide = (o) => {
		const x = page(o);
		x.routes['/rest/agg'] = () => textResponse(JSON.stringify({ schemaVersion: 1, truncated: false,
			dims: [{ name: 'bucket', type: 'time' }, { name: 'count', type: 'number' }], columns: { bucket: [OCT1], count: [7] } }));
		byStart(x, '/rest/s', s => textResponse(dayCsv(x.U, s, 1)));
		x.range(OCT1, OCT1 + (lim + 1) * DAY);
		return x;
	};
	// RAW refuses a window of more than MAX_CHUNKS chunks before any request, through the failure path (logged once).
	const wr1 = wide();
	wr1.U.createSource(SRC({ mode: 'raw', aggregateUrl: '/rest/agg' }), wr1.bus, {});
	await idle(wr1, 250);
	const E107 = "data source 's' window spans more than 10000 chunks";
	out.rawOverLimit = eq([wr1.fetches.length, brief(wr1.last()), wr1.last().error.message, wr1.errors], [0,
		['error', 0, null, 0, null, 'memory', 'E-JS-107'], E107, ['E-JS-107: ' + E107]]);
	// AUTO aggregates the same window at once; without an aggregate endpoint it fails like RAW.
	const wa = wide();
	wa.U.createSource(SRC({ mode: 'auto', maxRows: 1000, aggregateUrl: '/rest/agg',
		aggregate: { bucket: 'auto', groupBy: [], measures: [] } }), wa.bus, {});
	await idle(wa, 250);
	out.autoOverLimit = eq([wa.fetches.map(f => f.url.split('?')[0]), wa.D.get(wa.last()).bucketUnit, brief(wa.last()), wa.errors],
		[['/rest/agg'], 'month', ['ready', 1, null, 1, 'aggregate', 'memory', null], []]);
	const wn = wide();
	wn.U.createSource(SRC({ mode: 'auto', maxRows: 1000 }), wn.bus, {});
	await idle(wn, 250);
	out.autoOverLimitNoEndpoint = eq([wn.fetches.length, brief(wn.last())], [0, ['error', 0, null, 0, null, 'memory', 'E-JS-107']]);
	// A window that still lacks a token waits instead of failing.
	const wt = wide();
	wt.U.createSource(SRC({ url: '/rest/{org}/s' }), wt.bus, {});
	await idle(wt, 250);
	out.overLimitWaitsForToken = eq([wt.fetches.length, brief(wt.last())], [0, ['loading', 0, 0, 0, 'raw', 'memory', null]]);

	// A clearCache request that is superseded before it reaches the drop is not lost.
	const kc = fakeIdb();
	const kp = page({ idb: kc });
	const epWaits = [];
	let holdEpoch = false;
	kp.routes['/rest/epoch'] = () => {
		if (!holdEpoch) return textResponse('{"e":"v1"}');
		const d = deferred();
		epWaits.push(d);
		return d.promise;
	};
	byStart(kp, '/rest/s', s => textResponse(dayCsv(kp.U, s, 1)));
	kp.range(OCT1, OCT1 + 2 * DAY);
	kp.U.createSource(SRC({ cache: 'indexedDb', epoch: { url: '/rest/epoch', path: 'e' } }), kp.bus, {});
	await idle(kp, 250);
	holdEpoch = true;
	const kcmd = kp.bus.owner('ribbon');
	kcmd.publish('cmd:s', { schemaVersion: 1, op: 'reload', options: { clearCache: true } });
	await idle(kp, 250);
	kcmd.publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(kp, 250);
	holdEpoch = false;
	epWaits.forEach(d => d.resolve(textResponse('{"e":"v1"}')));
	await idle(kp, 0);
	out.clearSurvivesSupersede = eq([epWaits.length, fetchedDays(kp, '/rest/s'), brief(kp.last())],
		[2, [1, 0, 1, 0], ['ready', 2, null, 2, 'raw', 'indexedDb', null]]);

	// A body that finishes after its generation was superseded publishes nothing.
	const lb = page();
	const gate = deferred();
	lb.routes['/rest/agg'] = () => {
		const res = textResponse(JSON.stringify({ schemaVersion: 1, truncated: false,
			dims: [{ name: 'bucket', type: 'time' }], columns: { bucket: [OCT1] } }));
		const getReader = res.body.getReader;
		res.body.getReader = () => {
			const r = getReader(), read = r.read;
			let n = 0;
			r.read = () => ++n === 2 ? gate.promise.then(() => read()) : read();
			return r;
		};
		return res;
	};
	lb.range(OCT1, OCT1 + DAY);
	lb.U.createSource(SRC({ mode: 'aggregate', aggregateUrl: '/rest/agg', aggregate: { bucket: 'day', groupBy: [], measures: [] } }),
		lb.bus, {});
	await idle(lb, 250);
	lb.range(OCT1 + DAY, OCT1 + 2 * DAY);
	const lb0 = lb.handles.length;
	gate.resolve();
	await idle(lb, 0);
	out.lateBodyDropped = eq([lb.handles.length - lb0, lb.last().status, lb.last().version], [0, 'loading', 0]);
	await idle(lb, 250);
	out.nextGenerationRuns = eq([brief(lb.last()), lb.fetches.length], [['ready', 1, null, 1, 'aggregate', 'memory', null], 2]);

	// Dispose cancels the debounce and a running load; nothing is published, put or logged afterwards.
	const d1 = page();
	byStart(d1, '/rest/s', () => deferred().promise);
	d1.range(OCT1, OCT1 + DAY);
	const timers0 = d1.clock.pending();
	d1.U.createSource(SRC(), d1.bus, {}).dispose();
	await idle(d1, 300);
	out.disposeBeforeDebounce = eq([d1.clock.pending(), d1.fetches.length, d1.bus.get('data:s')], [timers0, 0, undefined]);
	const d2 = page();
	d2.D.config.platform.AbortController = null;
	const dw = [];
	byStart(d2, '/rest/s', () => { const d = deferred(); dw.push(d); return d.promise; });
	d2.range(OCT1, OCT1 + DAY);
	const src2 = d2.U.createSource(SRC(), d2.bus, {});
	await idle(d2, 250);
	src2.dispose();
	dw[0].resolve(textResponse(dayCsv(d2.U, OCT1, 3)));
	await idle(d2, 300);
	out.disposeMidLoad = eq([d2.bus.get('data:s'), d2.D.get({ schemaVersion: 1, source: 's', version: 1, status: 'ready', dims: [] }),
		d2.errors, d2.warns, d2.clock.pending()], [undefined, null, [], [], timers0]);

	// The pool never runs more than six requests at once, however high concurrency is declared.
	const cp = page();
	const cpend = [];
	byStart(cp, '/rest/s', s => { const d = deferred(); cpend.push((s - OCT1) / DAY); return d.promise; });
	cp.range(OCT1, OCT1 + 10 * DAY);
	cp.U.createSource(SRC({ concurrency: 99 }), cp.bus, {});
	await idle(cp, 250);
	out.concurrencyCapped = eq(cpend, [9, 8, 7, 6, 5, 4]);

	// Several truncated chunks report the largest cap, whichever finishes first.
	const tc = page();
	const tpend = new Map();
	byStart(tc, '/rest/s', s => { const d = deferred(); tpend.set((s - OCT1) / DAY, d); return d.promise; });
	tc.range(OCT1, OCT1 + 2 * DAY);
	tc.U.createSource(SRC({ concurrency: 2 }), tc.bus, {});
	await idle(tc, 250);
	tpend.get(0).resolve(textResponse(dayCsv(tc.U, OCT1, 1) + '#truncated,2\n'));
	await idle(tc, 0);
	tpend.get(1).resolve(textResponse(dayCsv(tc.U, OCT1 + DAY, 1) + '#truncated,5\n'));
	await idle(tc, 0);
	out.largestCap = eq([tc.last().truncatedAt, tc.warns], [5, ["E-JS-102: data source 's' truncated at 5 rows"]]);

	// An AbortError the generation did not cause (its signal is not aborted) is a failed fetch, never a silent hang.
	const fa = page();
	fa.routes['/rest/s'] = () => Promise.reject(new DOMException('aborted by the page', 'AbortError'));
	fa.range(OCT1, OCT1 + DAY);
	fa.U.createSource(SRC(), fa.bus, {});
	await idle(fa, 250);
	out.foreignAbortFetch = eq([brief(fa.last()), fa.errors.length],
		[['error', 0, null, 0, null, 'memory', 'E-JS-62'], 1]);
	const fb = page();
	fb.routes['/rest/s'] = () => {
		const res = textResponse('t,v\n');
		res.body.getReader = () => ({ read: () => Promise.reject(new DOMException('stream aborted', 'AbortError')),
			cancel: () => Promise.resolve(), releaseLock() {} });
		return res;
	};
	fb.range(OCT1, OCT1 + DAY);
	fb.U.createSource(SRC(), fb.bus, {});
	await idle(fb, 250);
	out.foreignAbortBody = eq([brief(fb.last()), fb.errors.length], [['error', 0, null, 0, null, 'memory', 'E-JS-62'], 1]);

	// Disposing twice must not release a later source's frames.
	const dd = page();
	byStart(dd, '/rest/s', s => textResponse(dayCsv(dd.U, s, 2)));
	dd.range(OCT1, OCT1 + DAY);
	const da = dd.U.createSource(SRC(), dd.bus, {});
	await idle(dd, 250);
	da.dispose();
	dd.U.createSource(SRC(), dd.bus, {});
	await idle(dd, 250);
	const db = dd.last();
	da.dispose();
	out.disposeTwice = eq([db.status, dd.D.get(db) !== null], ['ready', true]);

	// A clearCache request survives the AUTO to AGGREGATE switch when the switch comes before the drop (a window over the
	// chunk bound), and is not repeated when the drop already happened (rows past maxRows).
	const AGG = () => textResponse(JSON.stringify({ schemaVersion: 1, truncated: false,
		dims: [{ name: 'bucket', type: 'time' }], columns: { bucket: [OCT1] } }));
	const AGG_DEF = { mode: 'auto', maxRows: 1000, aggregateUrl: '/rest/agg', aggregate: { bucket: 'auto', groupBy: [], measures: [] } };
	const cs = page();
	byStart(cs, '/rest/s', s => textResponse(dayCsv(cs.U, s, 1)));
	cs.routes['/rest/agg'] = AGG;
	cs.range(OCT1, OCT1 + 4 * DAY);
	cs.U.createSource(SRC(AGG_DEF), cs.bus, {});
	await idle(cs, 250);
	cs.range(OCT1, OCT1 + (cs.U.MAX_CHUNKS + 1) * DAY);
	await idle(cs, 250);
	cs.bus.owner('ribbon').publish('cmd:s', { schemaVersion: 1, op: 'reload', options: { clearCache: true } });
	await idle(cs, 250);
	cs.range(OCT1, OCT1 + 4 * DAY);
	await idle(cs, 250);
	out.clearSurvivesOverLimitSwitch = eq([fetchedDays(cs, '/rest/s'), cs.fetches.filter(f => f.url.startsWith('/rest/agg')).length],
		[[3, 2, 1, 0, 3, 2, 1, 0], 2]);
	const cm = page();
	const mw = new Map();
	let small = true;
	byStart(cm, '/rest/s', s => {
		if (small) return textResponse(dayCsv(cm.U, s, 1));
		const d = deferred();
		mw.set((s - OCT1) / DAY, d);
		return d.promise;
	});
	cm.routes['/rest/agg'] = AGG;
	cm.range(OCT1, OCT1 + 4 * DAY);
	cm.U.createSource(SRC(Object.assign({}, AGG_DEF, { maxRows: 6, concurrency: 2 })), cm.bus, {});
	await idle(cm, 250);
	small = false;
	cm.bus.owner('ribbon').publish('cmd:s', { schemaVersion: 1, op: 'reload', options: { clearCache: true } });
	await idle(cm, 250);
	mw.get(2).resolve(textResponse(dayCsv(cm.U, OCT1 + 2 * DAY, 1)));
	await idle(cm, 0);
	mw.get(3).resolve(textResponse(dayCsv(cm.U, OCT1 + 3 * DAY, 8)));
	await idle(cm, 0);
	small = true;
	cm.bus.owner('ribbon').publish('cmd:s', { schemaVersion: 1, op: 'refresh' });
	await idle(cm, 250);
	out.clearNotRepeated = eq([fetchedDays(cm, '/rest/s').slice(7), brief(cm.last())],
		[[3, 1, 0], ['ready', 3, null, 4, 'raw', 'memory', null]]);

	// The raw partial frame is dropped when AUTO switches to the aggregate request.
	const pp = page();
	const pw = new Map(), pg = deferred();
	byStart(pp, '/rest/s', s => {
		const d = deferred();
		pw.set((s - OCT1) / DAY, d);
		return d.promise;
	});
	pp.routes['/rest/agg'] = () => pg.promise;
	pp.range(OCT1, OCT1 + 4 * DAY);
	pp.U.createSource(SRC(Object.assign({}, AGG_DEF, { maxRows: 5 })), pp.bus, { partial: true });
	const PH = { schemaVersion: 1, source: 's', version: 0, status: 'loading', dims: [], partial: true };
	await idle(pp, 250);
	await idle(pp, 300);
	pw.get(3).resolve(textResponse(dayCsv(pp.U, OCT1 + 3 * DAY, 4)));
	await idle(pp, 0);
	out.partialWhileRaw = eq(pp.D.get(PH) !== null, true);
	pw.get(2).resolve(textResponse(dayCsv(pp.U, OCT1 + 2 * DAY, 3)));
	await idle(pp, 0);
	out.partialDroppedOnSwitch = eq([pp.fetches.filter(f => f.url.startsWith('/rest/agg')).length, pp.D.get(PH)], [1, null]);
	pg.resolve(AGG());
	await idle(pp, 0);

	// A params message that leaves the mapped values unchanged (arrays compared by value) does not restart the load.
	const op = page();
	byStart(op, '/rest/s', s => textResponse(dayCsv(op.U, s, 1)));
	const opo = op.bus.owner('tp');
	const pub = (tags, note) => opo.publish('filter:range', { schemaVersion: 1, start: OCT1, end: OCT1 + 2 * DAY, tags: tags, note: note });
	pub(['a', 'b'], 1);
	op.U.createSource(SRC({ subscribes: [{ topic: 'filter:range', as: 'params',
		map: { start: 'start', end: 'end', tags: 'tags' } }] }), op.bus, {});
	await idle(op, 250);
	const op0 = op.handles.length;
	pub(['a', 'b'], 2);
	await idle(op, 250);
	out.unchangedParamsNoRestart = eq([op.handles.length - op0, brief(op.last())], [0, ['ready', 1, null, 2, 'raw', 'memory', null]]);
	pub(['a', 'c'], 2);
	await idle(op, 250);
	out.changedParamsRestart = eq(brief(op.last()), ['ready', 2, null, 2, 'raw', 'memory', null]);

	// The epoch is only probed when a cache store will use it.
	const ne = page();
	ne.routes['/rest/epoch'] = () => textResponse('', { status: 500 });
	byStart(ne, '/rest/s', s => textResponse(dayCsv(ne.U, s, 1)));
	ne.range(OCT1, OCT1 + DAY);
	ne.U.createSource(SRC({ cache: 'none', epoch: { url: '/rest/epoch', path: 'e' } }), ne.bus, {});
	await idle(ne, 250);
	out.noEpochWithoutCache = eq([ne.fetches.filter(f => f.url.startsWith('/rest/epoch')).length, brief(ne.last())],
		[0, ['ready', 1, null, 1, 'raw', null, null]]);

	// A record handed to an app builder keeps a header named like an Object.prototype member as an own key.
	const recs = [];
	const rb = page({ before: w => { w.JuneauConsoleCharts = [['builder', 'app.rec', { create: () => ({
		onRecord: r => recs.push(r), finish: () => ({ dims: [], rowCount: 0, truncated: false }) }) }]]; } });
	rb.routes['/rest/s'] = () => textResponse('a,__proto__\n1,2\n');
	rb.U.createSource(SRC({ builder: 'app.rec', columns: null, chunk: null, subscribes: [], cache: 'none' }), rb.bus, {});
	await idle(rb, 250);
	out.recordKeepsProtoKey = eq([recs.length, recs[0] && Object.keys(recs[0]).sort(), recs[0] && Object.getPrototypeOf(recs[0])],
		[1, ['__proto__', 'a'], null]);

	// An unexpected exception outside any builder is a neutral E-JS-98, not a builder failure.
	const bp = page({ before: w => { w.JuneauConsoleCharts = [['parser', 'app.bad', { create: () => { throw new Error('boom'); } }]]; } });
	byStart(bp, '/rest/s', s => textResponse(dayCsv(bp.U, s, 1)));
	bp.range(OCT1, OCT1 + DAY);
	bp.U.createSource(SRC({ parser: 'app.bad' }), bp.bus, {});
	await idle(bp, 250);
	out.neutralFallback = eq([brief(bp.last()), bp.last().error.message],
		[['error', 0, null, 0, null, 'memory', 'E-JS-98'], "'s' source '/rest/s' failed: 'boom'"]);

	// A hand-written def that leaves out parser, builder, cache and debounceMs loads like the generated one.
	const hw = page();
	byStart(hw, '/rest/s', s => textResponse(dayCsv(hw.U, s, 2)));
	hw.range(OCT1, OCT1 + 2 * DAY);
	hw.U.createSource({ id: 's', kind: 'source', url: '/rest/s', columns: TV, subscribes: [RANGE], chunk: BY_DAY },
		hw.bus, {});
	await idle(hw, 249);
	const hwEarly = hw.fetches.length;
	await idle(hw, 1);
	out.handWrittenDefaults = eq([hwEarly, brief(hw.last()), hw.rows().length],
		[0, ['ready', 1, null, 4, 'raw', 'memory', null], 4]);

	return out;
});

// ---- i_derived: section I ---------------------------------------------------------------------------------------------

/** A frame from object rows through the columnar builder, with extra keys (bucketUnit, truncated) merged in. */
function frameOf(U, dims, rows, extra) {
	const b = U.columnarBuilder(dims, {});
	rows.forEach(r => b.onRecord(r));
	return Object.assign(b.finish(), extra);
}

/** A frame's dims, rows and flags as JSON text, for "the input was not touched" checks. */
function frameText(U, f) {
	return JSON.stringify({ dims: f.dims, rowCount: f.rowCount, truncated: f.truncated, bucketUnit: f.bucketUnit,
		columns: Object.keys(f.columns).map(k => [k, Array.from(f.columns[k].codes || f.columns[k]), f.columns[k].dict]) });
}

/** The frame with every container frozen (typed arrays cannot be), so a write in strict mode throws. */
function frozenFrame(f) {
	f.dims.forEach(d => Object.freeze(d));
	Object.freeze(f.dims);
	Object.values(f.columns).forEach(c => { if (c.dict) { Object.freeze(c.dict); Object.freeze(c); } });
	Object.freeze(f.columns);
	return Object.freeze(f);
}

const OSD = [{ name: 't', type: 'time' }, { name: 'org', type: 'string' }, { name: 'status', type: 'string' }, { name: 'ms', type: 'number' }];

section('i_derived', async () => {
	const h = fresh();
	const U = h.U, out = {};
	const T = h.D.builtins.filter(t => t[0] === 'transform').reduce((m, t) => Object.assign(m, { [t[1]]: t[2] }), {});
	const err = fn => { try { fn(); return null; } catch (e) { return e.message; } };
	out.builtinTransforms = eq(Object.keys(T), ['filter', 'bucket', 'groupBy', 'topN']);

	const rows = [
		{ t: OCT1 + HOUR, org: 'a', status: 'Open', ms: 10 }, { t: OCT1 + 2 * HOUR, org: 'b', status: 'closed', ms: 20 },
		{ t: OCT1 + DAY, org: 'a', status: 'open', ms: null }, { t: OCT1 + DAY + HOUR, org: null, status: 'held', ms: 40 },
		{ t: OCT5, org: 'a', status: 'Reopened', ms: 50 }];
	const F = frameOf(U, OSD, rows);
	const cols = (f, ...names) => U.rowsOf(f).map(r => names.map(n => r[n]));

	// filter: in/notIn on dict strings, case-insensitive contains, inclusive min/max, time values as ISO, nulls pass notIn only.
	out.filterIn = eq(cols(T.filter(F, { field: 'status', in: ['open', 'held'] }), 'status'), [['open'], ['held']]);
	out.filterNotIn = eq(cols(T.filter(F, { field: 'org', notIn: ['a'] }), 'org'), [['b'], [null]]);
	out.filterContains = eq(cols(T.filter(F, { field: 'status', contains: 'OPEN' }), 'status'), [['Open'], ['open'], ['Reopened']]);
	out.filterMinMax = eq(cols(T.filter(F, { field: 'ms', min: 20, max: 40 }), 'ms'), [[20], [40]]);
	out.filterTime = eq(cols(T.filter(F, { field: 't', min: '2026-10-02T00:00:00Z' }), 'ms'), [[null], [40], [50]]);
	out.filterNullIn = eq(T.filter(F, { field: 'org', in: ['a', 'b'] }).rowCount, 4);
	out.filterNullNotIn = eq(T.filter(F, { field: 'org', notIn: ['b'] }).rowCount, 4);
	out.filterNullRange = eq(T.filter(F, { field: 'ms', min: 0 }).rowCount, 4);
	const kept = T.filter(F, { field: 'org', in: ['b'] });
	out.filterShape = eq([kept.dims, kept.columns.org.dict, F.rowCount, kept.columns.ms !== F.columns.ms], [OSD, ['a', 'b'], 5, true]);
	out.filterErrors = eq([err(() => T.filter(F, { field: 'nope', in: [] })), err(() => T.filter(F, { field: 'org', min: 1 })),
		err(() => T.filter(F, { field: 'ms', contains: '1' })), err(() => T.filter(F, {})),
		err(() => T.filter(F, { field: 'org', in: 'a' })), err(() => T.filter(F, { field: 't', min: 'garbage' })),
		err(() => T.filter(F, { field: 'ms', max: 'x' }))], ["the frame has no field 'nope'", 'min and max need a number or time field',
		'contains needs a string field', "'field' is required", "'in' must be a list", "'garbage' is not a time", "'x' is not a number"]);
	const BD = [{ name: 'k', type: 'string' }, { name: 'ok', type: 'bool' }];
	const bools = frameOf(U, BD, [{ k: 'a', ok: true }, { k: 'b', ok: false }, { k: 'c', ok: null }]);
	out.filterBool = eq([cols(T.filter(bools, { field: 'ok', in: [true] }), 'k'), cols(T.filter(bools, { field: 'ok', notIn: [true] }), 'k')],
		[[['a']], [['b'], ['c']]]);

	// bucket: rows to UTC days with groupBy and every measure, as SeriesAggregator does.
	const day = T.bucket(F, { field: 't', unit: 'day', groupBy: ['org'], measures: ['count', 'sum(ms)', 'avg(ms)', 'min(ms)', 'max(ms)'] });
	out.bucketDims = eq([day.dims.map(d => d.name + ':' + d.type), day.bucketUnit], [['bucket:time', 'org:string', 'count:number',
		'sum(ms):number', 'avg(ms):number', 'min(ms):number', 'max(ms):number'], 'day']);
	out.bucketRows = eq(cols(day, 'bucket', 'org', 'count', 'sum(ms)', 'avg(ms)', 'min(ms)', 'max(ms)'), [
		[OCT1, 'a', 1, 10, 10, 10, 10], [OCT1, 'b', 1, 20, 20, 20, 20], [OCT1 + DAY, null, 1, 40, 40, 40, 40],
		[OCT1 + DAY, 'a', 1, 0, null, null, null], [OCT5, 'a', 1, 50, 50, 50, 50]]);
	out.bucketDefaults = eq([cols(T.bucket(F, { field: 't', unit: 'week' }), 'bucket', 'count'), T.bucket(F, { field: 't', unit: 'auto' }).bucketUnit,
		T.bucket(F, { field: 't' }).bucketUnit], [[[OCT1 - 3 * DAY, 4], [OCT5, 1]], 'hour', 'hour']);
	out.bucketWeekStart = eq(cols(T.bucket(F, { field: 't', unit: 'week', weekStart: 'sunday' }), 'bucket', 'count'), [[OCT1 - 4 * DAY, 4], [OCT5 - DAY, 1]]);
	out.bucketErrors = eq([err(() => T.bucket(F, { field: 'org', unit: 'day' })), err(() => T.bucket(F, { field: 't', unit: 'year' })),
		err(() => T.bucket(F, { field: 't', unit: 'day', measures: ['median(ms)'] })), err(() => T.bucket(F, { field: 't', unit: 'week', weekStart: 'funday' })),
		err(() => T.bucket(F, { unit: 'day' })), err(() => T.bucket(F, { field: 't', unit: 'day', measures: 'count' })),
		err(() => T.bucket(F, { field: 't', unit: 'day', measures: ['count', 'count'] })),
		err(() => T.bucket(F, { field: 't', unit: 'day', groupBy: ['bucket'] })), err(() => T.bucket(F, { field: 't', unit: 'day', groupBy: ['org', 'org'] })),
		err(() => T.bucket(F, { field: 't', unit: 'day', measures: ['sum(org)'] }))],
		["field 'org' is not a time field", "'year' is not a bucket unit", "'median(ms)' is not a measure", "'funday' is not a weekday",
		"'field' is required", "'measures' must be a list", "duplicate measure 'count'", "groupBy field 'bucket' is reserved",
		"duplicate groupBy field 'org'", "'sum(org)' needs a number or time field"]);

	// Month and long-window edges, the empty frame, and the unit a bucketed frame carries.
	const wide = frameOf(U, OSD, [{ t: OCT1, ms: 1 }, { t: Date.UTC(2026, 9, 31, 23, 59, 59, 999), ms: 2 }, { t: Date.UTC(2026, 10, 1), ms: 4 },
		{ t: OCT1 + 100 * DAY, ms: 8 }]);
	out.bucketMonth = eq(cols(T.bucket(wide, { field: 't', unit: 'month', measures: ['sum(ms)'] }), 'bucket', 'sum(ms)'),
		[[OCT1, 3], [Date.UTC(2026, 10, 1), 4], [Date.UTC(2027, 0, 1), 8]]);
	out.bucketAutoWide = eq(T.bucket(wide, { field: 't', unit: 'auto' }).bucketUnit, 'day');
	const none = T.bucket(frameOf(U, OSD, []), { field: 't', unit: 'auto' });
	out.bucketEmpty = eq([none.rowCount, none.bucketUnit, none.dims.map(d => d.name)], [0, 'hour', ['bucket', 'count']]);

	// Numbers as the server computes them: only finite numbers are measured, a row with no usable time is skipped,
	// sum of no values is 0, avg/min/max of none are null, an overflowing sum is null.
	const NUM = [{ name: 't', type: 'time' }, { name: 'g', type: 'string' }, { name: 'v', type: 'number' }];
	const edge = frameOf(U, NUM, [{ t: OCT1, g: 'x', v: 1e308 }, { t: OCT1, g: 'x', v: 1e308 }, { t: OCT1 + HOUR, g: 'y', v: null },
		{ t: null, g: 'y', v: 5 }, { t: 'garbage', g: 'y', v: 6 }, { t: OCT1, g: 'y', v: 'abc' }]);
	out.bucketNumbers = eq(cols(T.bucket(edge, { field: 't', unit: 'day', groupBy: ['g'], measures: ['count', 'sum(v)', 'avg(v)', 'min(v)', 'max(v)'] }),
		'bucket', 'g', 'count', 'sum(v)', 'avg(v)', 'min(v)', 'max(v)'), [[OCT1, 'x', 2, null, null, 1e308, 1e308], [OCT1, 'y', 2, 0, null, null, null]]);

	// Re-bucketing an aggregate frame: coarser nested units only; avg weighted by count.
	const AGD = [{ name: 'bucket', type: 'time' }, { name: 'status', type: 'string' }, { name: 'count', type: 'number' },
		{ name: 'sum(ms)', type: 'number' }, { name: 'avg(ms)', type: 'number' }, { name: 'max(ms)', type: 'number' }];
	const agg = frameOf(U, AGD, [
		{ bucket: OCT1, status: 'open', count: 1, 'sum(ms)': 10, 'avg(ms)': 10, 'max(ms)': 10 },
		{ bucket: OCT1 + DAY, status: 'open', count: 3, 'sum(ms)': 60, 'avg(ms)': 20, 'max(ms)': 30 },
		{ bucket: OCT1 + DAY, status: 'closed', count: 2, 'sum(ms)': 2, 'avg(ms)': 1, 'max(ms)': 1 },
		{ bucket: OCT5, status: 'open', count: 1, 'sum(ms)': 5, 'avg(ms)': null, 'max(ms)': null }], { bucketUnit: 'day' });
	const wk = T.bucket(agg, { field: 'bucket', unit: 'week' });
	out.rebucket = eq([wk.bucketUnit, wk.dims, cols(wk, 'bucket', 'status', 'count', 'sum(ms)', 'avg(ms)', 'max(ms)')], ['week', AGD, [
		[OCT1 - 3 * DAY, 'closed', 2, 2, 1, 1], [OCT1 - 3 * DAY, 'open', 4, 70, 17.5, 30], [OCT5, 'open', 1, 5, null, null]]]);
	out.rebucketIdentity = eq([T.bucket(agg, { field: 'bucket', unit: 'day' }) === agg, T.bucket(agg, { field: 'bucket', unit: 'auto' }) === agg], [true, true]);
	out.rebucketWeekStart = eq(cols(T.bucket(agg, { field: 'bucket', unit: 'week', weekStart: 'sunday' }), 'bucket', 'status', 'count'),
		[[OCT1 - 4 * DAY, 'closed', 2], [OCT1 - 4 * DAY, 'open', 4], [OCT5 - DAY, 'open', 1]]);
	const hourly = frameOf(U, AGD, [{ bucket: OCT1 + HOUR, count: 2, 'sum(ms)': null, 'avg(ms)': 4 }, { bucket: OCT1 + 2 * HOUR, count: 2, 'sum(ms)': 9, 'avg(ms)': 8 },
		{ bucket: Date.UTC(2026, 10, 3), count: 1, 'sum(ms)': 1, 'avg(ms)': 1 }], { bucketUnit: 'hour', truncated: true });
	const mo = T.bucket(hourly, { field: 'bucket', unit: 'month' });
	out.rebucketMonthNullSum = eq([mo.truncated, mo.bucketUnit, cols(mo, 'bucket', 'count', 'sum(ms)', 'avg(ms)')],
		[true, 'month', [[OCT1, 4, null, 6], [Date.UTC(2026, 10, 1), 1, 1, 1]]]);
	out.rebucketRefused = eq([err(() => T.bucket(agg, { field: 'bucket', unit: 'hour' })),
		err(() => T.bucket(Object.assign({}, agg, { bucketUnit: 'week' }), { field: 'bucket', unit: 'month' })),
		err(() => T.bucket(frameOf(U, AGD.filter(d => d.name !== 'count'), [], { bucketUnit: 'day' }), { field: 'bucket', unit: 'week' })),
		err(() => T.bucket(Object.assign({}, agg, { bucketUnit: 'bogus' }), { field: 'bucket', unit: 'week' }))],
		["cannot re-bucket 'day' buckets to the finer 'hour'", "'week' buckets do not nest in 'month' buckets", 'avg needs a count column to re-bucket',
		"'bogus' is not a bucket unit"]);

	// groupBy and topN.
	out.groupBy = eq(cols(T.groupBy(F, { by: ['org'], measures: ['count', 'sum(ms)'] }), 'org', 'count', 'sum(ms)'),
		[[null, 1, 40], ['a', 3, 60], ['b', 1, 20]]);
	const byNum = T.groupBy(frameOf(U, NUM, [{ v: 2 }, { v: null }, { v: 1 }, { v: 2 }]), { by: ['v'] });
	out.groupByNumber = eq([byNum.dims, cols(byNum, 'v', 'count'), byNum.bucketUnit], [[{ name: 'v', type: 'number' }, { name: 'count', type: 'number' }],
		[[null, 1], [1, 1], [2, 2]], undefined]);
	out.groupByErrors = eq([err(() => T.groupBy(F, { by: ['org', 'org'] })), err(() => T.groupBy(F, { by: ['org'], measures: ['sum(ms)', 'sum(ms)'] })),
		err(() => T.groupBy(frameOf(U, [{ name: 'count', type: 'string' }], []), { by: ['count'] })), err(() => T.groupBy(F, { by: ['nope'] }))],
		["duplicate groupBy field 'org'", "duplicate measure 'sum(ms)'", "groupBy field 'count' collides with a measure", "the frame has no field 'nope'"]);
	out.groupByNoBy = eq(cols(T.groupBy(F, { measures: ['sum(ms)'] }), 'sum(ms)'), [[120]]);
	out.topN = eq(cols(T.topN(F, { by: 'ms', n: 2 }), 'ms'), [[50], [40]]);
	out.topNOther = eq(cols(T.topN(F, { by: 'ms', n: 2, other: 'Other' }), 't', 'org', 'status', 'ms'),
		[[OCT5, 'a', 'Reopened', 50], [OCT1 + DAY + HOUR, null, 'held', 40], [null, 'Other', 'Other', 30]]);
	out.topNTies = eq(cols(T.topN(frameOf(U, [{ name: 'k', type: 'string' }, { name: 'v', type: 'number' }],
		[{ k: 'x', v: 1 }, { k: 'y', v: 2 }, { k: 'z', v: 1 }]), { by: 'v', n: 3, other: 'Other' }), 'k'), [['y'], ['x'], ['z']]);
	const KV = [{ name: 'k', type: 'string' }, { name: 'v', type: 'number' }];
	const gaps = frameOf(U, KV, [{ k: 'a', v: null }, { k: 'b', v: 5 }, { k: 'c', v: null }, { k: 'd', v: 5 }]);
	out.topNNullsLastStable = eq([cols(T.topN(gaps, { by: 'v', n: 3 }), 'k'), cols(T.topN(gaps, { by: 'v', n: 0, other: 'rest' }), 'k', 'v'),
		T.topN(gaps, { by: 'v', n: -2 }).rowCount, T.topN(gaps, { by: 'v', n: '1' }).rowCount], [[['b'], ['d'], ['a']], [['rest', 10]], 0, 1]);
	out.topNErrors = eq([err(() => T.topN(F, { by: 'org', n: 1 })), err(() => T.topN(F, { by: 'ms', n: 'x' })), err(() => T.topN(F, { n: 1 })),
		err(() => T.topN(F, { by: 'nope', n: 1 }))], ['by needs a number or time field', 'n is not a number', "'by' is required",
		"the frame has no field 'nope'"]);

	// A column named like an Object.prototype member survives every transform.
	const odd = frameOf(U, [{ name: '__proto__', type: 'string' }, { name: 'toString', type: 'number' }],
		[{ ['__proto__']: 'p', toString: 3 }, { ['__proto__']: 'q', toString: 4 }]);
	out.protoNamedColumns = eq([cols(T.filter(odd, { field: '__proto__', in: ['q'] }), '__proto__', 'toString'),
		cols(T.groupBy(odd, { by: ['__proto__'], measures: ['sum(toString)'] }), '__proto__', 'sum(toString)'),
		cols(T.topN(odd, { by: 'toString', n: 1, other: 'o' }), '__proto__', 'toString')],
		[[['q', 4]], [['p', 3], ['q', 4]], [['q', 4], ['o', 3]]]);

	// Purity: frozen inputs are never written, and the truncated flag and the unit pass through.
	const purity = [[F, 'filter', { field: 'status', in: ['open'] }], [F, 'bucket', { field: 't', unit: 'day', groupBy: ['org'], measures: ['sum(ms)'] }],
		[agg, 'bucket', { field: 'bucket', unit: 'week' }], [agg, 'bucket', { field: 'bucket', unit: 'day' }], [F, 'groupBy', { by: ['org'] }],
		[F, 'topN', { by: 'ms', n: 2, other: 'Other' }]];
	const marked = purity.map(p => Object.assign(frameOf(U, p[0].dims, U.rowsOf(p[0]), { bucketUnit: p[0].bucketUnit }), { truncated: true }));
	const before = marked.map(f => frameText(U, f));
	let wrote = null;
	marked.forEach((f, i) => { frozenFrame(f); try { T[purity[i][1]](f, purity[i][2]); } catch (e) { wrote = wrote || e.message; } });
	out.transformsArePure = eq([wrote, marked.map(f => frameText(U, f)) + '' === before + ''], [null, true]);
	out.truncatedPassesThrough = eq(marked.map((f, i) => T[purity[i][1]](f, purity[i][2]).truncated), purity.map(() => true));

	// Whole-string tokens, an array spread inside a list, {missing} while a token has no value, and no prototype lookups.
	out.fillArgs = eq([U.fillArgs([{ use: 'filter', args: { field: 's', in: ['{r}', 'x'], n: '{n}', label: 'a {n}' } }], { r: ['p', 'q'], n: 3 }),
		U.fillArgs({ a: '{gone}' }, {}), U.fillArgs({ a: '{toString}' }, {})], [{ value: [{ use: 'filter', args: { field: 's', in: ['p', 'q', 'x'], n: 3, label: 'a {n}' } }] },
		{ missing: true }, { missing: true }]);

	// Derived over a scripted input: mirrors loading, runs on ready, re-runs on a params change, forwards cmd.
	const src = h.bus.owner('data-source:src');
	const handle = (status, version, extra) => Object.assign({ schemaVersion: 1, source: 'src', version: version, status: status,
		rowCount: 0, truncated: false, dims: OSD, error: null, mode: 'raw', cache: 'memory' }, extra);
	const seen = [];
	h.bus.subscribe('data:d', p => seen.push(p));
	const forwarded = [];
	h.bus.subscribe('cmd:src', p => forwarded.push(p), { retained: false });
	const def = { id: 'd', kind: 'derived', from: 'src', subscribes: [{ topic: 'filter:status', as: 'params', map: { keep: 'values' } }],
		transforms: [{ use: 'filter', args: { field: 'status', in: ['{keep}'] } }, { use: 'groupBy', args: { by: ['org'], measures: ['count'] } }] };
	const derived = U.createDerived(def, h.bus);
	const status = h.bus.owner('picker');
	src.publish('data:src', handle('loading', 0, { progress: { loaded: 7 } }));
	out.derivedLoading = eq(brief(seen[seen.length - 1]), ['loading', 0, 7, 0, 'raw', 'memory', null]);
	h.D.put('src', F);
	src.publish('data:src', handle('ready', 1, { rowCount: 5 }));
	out.derivedAwaitsToken = eq(brief(seen[seen.length - 1]), ['loading', 0, 0, 0, 'raw', 'memory', null]);
	status.publish('filter:status', { schemaVersion: 1, values: ['open', 'held'] });
	const d1 = seen[seen.length - 1];
	out.derivedReady = eq([brief(d1), d1.dims, U.rowsOf(h.D.get(d1)), d1.progress, d1.truncatedAt], [['ready', 1, null, 2, 'raw', 'memory', null],
		[{ name: 'org', type: 'string' }, { name: 'count', type: 'number' }], [{ org: null, count: 1 }, { org: 'a', count: 1 }], null, null]);
	status.publish('filter:status', { schemaVersion: 1, values: ['closed'] });
	out.derivedParamsRerun = eq([seen[seen.length - 1].version, U.rowsOf(h.D.get(seen[seen.length - 1]))], [2, [{ org: 'b', count: 1 }]]);
	const n = seen.length;
	src.publish('data:src', handle('ready', 1, { rowCount: 5, loadedAt: 'x' }));
	// Same input version and args: no new frame (the version stays 2); only the handle's loadedAt follows the input's.
	out.derivedSkipsSameInput = eq([seen.length - n, seen[seen.length - 1].version, seen[seen.length - 1].loadedAt], [1, 2, 'x']);
	src.publish('data:src', handle('stale', 1, { rowCount: 5, error: { code: 'E-JS-62', message: 'm' } }));
	out.derivedStale = eq([brief(seen[seen.length - 1]), seen[seen.length - 1].error], [['stale', 2, null, 1, 'raw', 'memory', 'E-JS-62'],
		{ code: 'E-JS-62', message: 'm' }]);
	src.publish('data:src', handle('error', 1, { error: { code: 'E-JS-62', message: 'gone' } }));
	out.derivedErrorKeepsFrame = eq([brief(seen[seen.length - 1]), U.rowsOf(h.D.get(seen[seen.length - 1])).length], [['stale', 2, null, 1, 'raw', 'memory', 'E-JS-62'], 1]);
	h.bus.owner('card').publish('cmd:d', { schemaVersion: 1, op: 'refresh', options: { clearCache: true } });
	h.bus.owner('card').publish('cmd:d', { schemaVersion: 1, op: 'reload' });
	h.bus.owner('card').publish('cmd:d', { schemaVersion: 1, op: 'zoomTo' });
	out.derivedForwardsRefresh = eq(forwarded, [{ schemaVersion: 1, op: 'refresh', options: { clearCache: true } }, { schemaVersion: 1, op: 'reload' }]);
	derived.dispose();
	derived.dispose();
	const m = seen.length;
	status.publish('filter:status', { schemaVersion: 1, values: ['open'] });
	out.derivedDispose = eq([seen.length - m, h.D.get(seen[seen.length - 1])], [0, null]);

	// A truncated input's cap is copied only when the result is itself truncated.
	const tr = fresh(), trs = [];
	tr.bus.subscribe('data:t', p => trs.push(p));
	tr.U.createDerived({ id: 't', kind: 'derived', from: 'src', subscribes: [], transforms: [{ use: 'topN', args: { by: 'ms', n: 1 } }] }, tr.bus);
	tr.D.put('src', Object.assign(frameOf(tr.U, OSD, rows), { truncated: true }));
	tr.bus.owner('data-source:src').publish('data:src', handle('ready', 1, { truncated: true, truncatedAt: 5 }));
	out.derivedTruncatedAt = eq([trs[trs.length - 1].truncated, trs[trs.length - 1].truncatedAt], [true, 5]);

	// E-JS-94 and E-JS-98; an input error with no frame yet is error.
	const bad = (use, args) => {
		const g = fresh();
		const s = g.bus.owner('data-source:src'), got = [];
		g.bus.subscribe('data:e', p => got.push(p));
		g.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [{ use: use, args: args }] }, g.bus);
		g.D.put('src', frameOf(g.U, OSD, rows));
		s.publish('data:src', handle('ready', 1));
		s.publish('data:src', handle('ready', 1, { loadedAt: 'again' }));
		return [brief(got[got.length - 1]), got[got.length - 1].error.message, g.errors];
	};
	out.unknownTransform = eq(bad('app.nope', {}), [['error', 0, null, 0, 'raw', 'memory', 'E-JS-94'],
		"data source 'e' uses unknown transform 'app.nope'; registered: 'bucket, filter, groupBy, topN'",
		["E-JS-94: data source 'e' uses unknown transform 'app.nope'; registered: 'bucket, filter, groupBy, topN'"]]);
	const thrown = bad('bucket', { field: 't', unit: 'year' });
	out.transformThrows = eq([thrown[1], thrown[2].length], ["'e' transform 'bucket' failed: ''year' is not a bucket unit'", 1]);
	const notFrame = fresh(), nfs = [];
	notFrame.bus.subscribe('data:e', p => nfs.push(p));
	notFrame.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [{ use: 'app.junk', args: {} }] }, notFrame.bus);
	notFrame.env.window.JuneauConsoleCharts = [['transform', 'app.junk', () => ({ rows: [] })]];
	notFrame.D.put('src', frameOf(notFrame.U, OSD, rows));
	notFrame.bus.owner('data-source:src').publish('data:src', handle('ready', 1));
	out.resultNotAFrame = eq(nfs[nfs.length - 1].error.message, "'e' transform 'app.junk' failed: 'the result is not a frame'");
	const ge = fresh(), gs = [];
	ge.bus.subscribe('data:e', p => gs.push(p));
	ge.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [] }, ge.bus);
	ge.bus.owner('data-source:src').publish('data:src', handle('error', 0, { error: { code: 'E-JS-62', message: 'm' } }));
	out.inputError = eq(brief(gs[gs.length - 1]), ['error', 0, null, 0, 'raw', 'memory', 'E-JS-62']);

	// A derived source that reads itself, or two that read each other, publishes loading and goes nowhere.
	const cy = fresh(), cys = [];
	cy.bus.subscribe('data:c', p => cys.push(p.status));
	cy.U.createDerived({ id: 'c', kind: 'derived', from: 'c', subscribes: [], transforms: [] }, cy.bus);
	cy.U.createDerived({ id: 'p', kind: 'derived', from: 'q', subscribes: [], transforms: [] }, cy.bus);
	cy.U.createDerived({ id: 'q', kind: 'derived', from: 'p', subscribes: [], transforms: [] }, cy.bus);
	out.cyclesDoNotHang = eq([cys, cy.errors], [['loading'], []]);

	// ---- Edge cases: late registration, out-of-range times, null rules, loadedAt ----
	const lastOf = a => a[a.length - 1];
	// A transform registered after the failure is found on the next republish; an unchanged miss is logged once.
	const late = fresh(), lates = [], lateSrc = late.bus.owner('data-source:src');
	late.bus.subscribe('data:e', p => lates.push(p));
	late.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [{ use: 'app.late', args: {} }] }, late.bus);
	late.D.put('src', frameOf(late.U, OSD, rows));
	lateSrc.publish('data:src', handle('ready', 1));
	lateSrc.publish('data:src', handle('ready', 1, { loadedAt: 'again' }));
	const lateBefore = [lastOf(lates).error.code, late.errors.length];
	late.env.window.JuneauConsoleCharts = [['transform', 'app.late', f => late.D.builtins.find(t => t[1] === 'topN')[2](f, { by: 'ms', n: 1 })]];
	lateSrc.publish('data:src', handle('ready', 1, { loadedAt: 'third' }));
	out.lateTransformRecovers = eq([lateBefore, brief(lastOf(lates))], [['E-JS-94', 1], ['ready', 1, null, 1, 'raw', 'memory', null]]);

	// A time outside the Date range is a row with no time: skipped, never a null or out-of-range bucket.
	const wild = frameOf(U, OSD, [{ t: 9e15, ms: 1 }, { t: OCT1, ms: 2 }, { t: -9e15, ms: 4 }, { t: 9e15, ms: 8 }]);
	const tame = frameOf(U, OSD, [{ t: OCT1, ms: 2 }]);
	out.bucketOutOfRange = eq(['month', 'day', 'week', 'hour'].map(u => cols(T.bucket(wild, { field: 't', unit: u }), 'bucket', 'count')),
		['month', 'day', 'week', 'hour'].map(u => cols(T.bucket(tame, { field: 't', unit: u }), 'bucket', 'count')));
	out.bucketOutOfRangeAuto = eq((a => [a.bucketUnit, cols(a, 'bucket', 'count')])(T.bucket(wild, { field: 't' })),
		(a => [a.bucketUnit, cols(a, 'bucket', 'count')])(T.bucket(tame, { field: 't' })));
	const aggWild = frameOf(U, AGD, U.rowsOf(agg).concat([{ bucket: 9e15, status: 'open', count: 5, 'sum(ms)': 1, 'avg(ms)': 1, 'max(ms)': 1 },
		{ bucket: -9e15, status: 'open', count: 5, 'sum(ms)': 1, 'avg(ms)': 1, 'max(ms)': 1 }]), { bucketUnit: 'day' });
	out.rebucketOutOfRange = eq(['week', 'month'].map(u => cols(T.bucket(aggWild, { field: 'bucket', unit: u }), 'bucket', 'status', 'count')),
		['week', 'month'].map(u => cols(T.bucket(agg, { field: 'bucket', unit: u }), 'bucket', 'status', 'count')));

	// groupBy with no by is always one cell, an empty frame included.
	const zero = T.groupBy(frameOf(U, OSD, []), { measures: ['count', 'sum(ms)', 'avg(ms)', 'min(ms)', 'max(ms)'] });
	out.groupByEmptyNoBy = eq([cols(zero, 'count', 'sum(ms)', 'avg(ms)', 'min(ms)', 'max(ms)'), T.groupBy(frameOf(U, OSD, []), { by: ['org'] }).rowCount],
		[[[0, 0, null, null, null]], 0]);

	// One null rule for in/notIn on every dim type: a null entry is the null cell; strings match strictly.
	const NS = frameOf(U, [{ name: 's', type: 'string' }, { name: 'n', type: 'number' }, { name: 'b', type: 'bool' }, { name: 't', type: 'time' }],
		[{ s: 'null', n: 1, b: true, t: OCT1 }, { s: '1', n: 2, b: false, t: OCT1 + HOUR }, { s: null, n: null, b: null, t: null },
		{ s: 'a', n: 3, b: true, t: OCT5 }]);
	const keep = (field, a) => { try { return cols(T.filter(NS, Object.assign({ field: field }, a)), 's').map(r => r[0]); } catch (e) { return 'throws: ' + e.message; } };
	out.filterNullRule = eq([keep('s', { in: [null] }), keep('s', { notIn: [null] }), keep('s', { in: ['a', null] }), keep('s', { in: [1] }),
		keep('s', { notIn: [undefined, 'a'] }), keep('n', { in: [null] }), keep('n', { notIn: [null] }), keep('n', { in: [null, 3] }),
		keep('b', { in: [null] }), keep('b', { notIn: [null] }), keep('t', { in: [null, OCT5] }), keep('t', { notIn: [null] }),
		keep('s', { in: [null], contains: 'a' }), keep('n', { in: [null], min: 0 })],
		[[null], ['null', '1', 'a'], [null, 'a'], [], ['null', '1'], [null], ['null', '1', 'a'], [null, 'a'], [null], ['null', '1', 'a'], [null, 'a'],
		['null', '1', 'a'], [], []]);

	// topN other: additive columns summed, avg/min/max columns null.
	const topAgg = T.topN(agg, { by: 'count', n: 1, other: 'rest' });
	out.topNOtherNonAdditive = eq(cols(topAgg, 'bucket', 'status', 'count', 'sum(ms)', 'avg(ms)', 'max(ms)'),
		[[OCT1 + DAY, 'open', 3, 60, 20, 30], [null, 'rest', 4, 17, null, null]]);

	// weekStart: a day name or a getUTCDay index, checked only where a week can matter.
	const safe = fn => { try { return fn(); } catch (e) { return 'throws: ' + e.message; } };
	out.weekStartIndex = eq([safe(() => cols(T.bucket(F, { field: 't', unit: 'week', weekStart: 0 }), 'bucket', 'count')),
		safe(() => cols(T.bucket(agg, { field: 'bucket', unit: 'week', weekStart: 0 }), 'bucket', 'count')),
		safe(() => T.bucket(F, { field: 't', unit: 'day', weekStart: 'funday' }).rowCount), err(() => T.bucket(F, { field: 't', weekStart: 'funday' })),
		err(() => T.bucket(F, { field: 't', unit: 'week', weekStart: 7 })), err(() => T.bucket(F, { field: 't', unit: 'week', weekStart: 1.5 }))],
		[[[OCT1 - 4 * DAY, 4], [OCT5 - DAY, 1]], [[OCT1 - 4 * DAY, 2], [OCT1 - 4 * DAY, 4], [OCT5 - DAY, 1]], 3, "'funday' is not a weekday", "'7' is not a weekday",
		"'1.5' is not a weekday"]);

	// The derived handle carries the input's loadedAt; a re-created input with a reused version re-runs; a missing frame is an error.
	const fx = fresh(), fxs = [], fxSrc = fx.bus.owner('data-source:src');
	fx.bus.subscribe('data:e', p => fxs.push(p));
	fx.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [{ use: 'filter', args: { field: 'ms', min: 0 } }] }, fx.bus);
	fx.D.put('src', frameOf(fx.U, OSD, rows));
	fxSrc.publish('data:src', handle('ready', 1, { loadedAt: '2026-10-01T00:00:00.000Z' }));
	const fx1 = lastOf(fxs);
	fx.D.release('src');
	fx.D.put('src', frameOf(fx.U, OSD, rows.slice(0, 2)));
	fxSrc.publish('data:src', handle('ready', 1, { loadedAt: '2026-10-02T00:00:00.000Z' }));
	out.derivedLoadedAt = eq([fx1.loadedAt, lastOf(fxs).loadedAt], ['2026-10-01T00:00:00.000Z', '2026-10-02T00:00:00.000Z']);
	out.derivedReusedVersionReruns = eq([fx1.rowCount, lastOf(fxs).rowCount, lastOf(fxs).version], [4, 2, 2]);
	fx.D.release('src');
	fxSrc.publish('data:src', handle('ready', 3, { loadedAt: 'x' }));
	out.derivedMissingFrame = eq([lastOf(fxs).status, lastOf(fxs).error, fx.errors],
		['stale', { code: 'E-JS-98', message: "'e' transform input 'src' failed: 'the input frame is not available'" },
		["E-JS-98: 'e' transform input 'src' failed: 'the input frame is not available'"]]);
	// Same episode: published again, logged once; a held frame ends it, and the next loss logs again.
	fx.fxCount = fx.errors.length;
	fxSrc.publish('data:src', handle('ready', 3, { loadedAt: 'y' }));
	const quiet = [lastOf(fxs).status, fx.errors.length - fx.fxCount];
	fx.D.put('src', frameOf(fx.U, OSD, rows));
	fxSrc.publish('data:src', handle('ready', 1, { loadedAt: 'z' }));
	const heldAgain = lastOf(fxs);
	fx.D.release('src');
	fxSrc.publish('data:src', handle('ready', 1, { loadedAt: 'w' }));
	out.derivedMissingFrameOncePerEpisode = eq([quiet, heldAgain.status, heldAgain.loadedAt, fx.errors.length - fx.fxCount], [['stale', 0], 'ready', 'z', 1]);
	// The held frame's loadedAt follows the input on a skipped republish.
	const ld = fresh(), lds = [], ldSrc = ld.bus.owner('data-source:src');
	ld.bus.subscribe('data:e', p => lds.push(p));
	ld.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [] }, ld.bus);
	ld.D.put('src', frameOf(ld.U, OSD, rows));
	ldSrc.publish('data:src', handle('ready', 1, { loadedAt: 'a' }));
	ldSrc.publish('data:src', handle('ready', 1, { loadedAt: 'b' }));
	out.derivedHeldLoadedAtFollows = eq([lastOf(lds).loadedAt, ld.D.get(lastOf(lds)).loadedAt], ['b', 'b']);
	// After the input errors, the same frame is run again; after dispose nothing is kept.
	ldSrc.publish('data:src', handle('error', 1, { error: { code: 'E-JS-62', message: 'm' } }));
	const vBefore = lastOf(lds).version;
	ldSrc.publish('data:src', handle('ready', 1, { loadedAt: 'c' }));
	out.derivedErrorReleasesFrame = eq([vBefore, lastOf(lds).version], [1, 2]);
	const lone = fresh(), lones = [];
	lone.bus.subscribe('data:e', p => lones.push(p));
	lone.U.createDerived({ id: 'e', kind: 'derived', from: 'src', subscribes: [], transforms: [] }, lone.bus);
	lone.bus.owner('data-source:src').publish('data:src', handle('ready', 1));
	out.derivedMissingFrameFirst = eq(brief(lastOf(lones)), ['error', 0, null, 0, 'raw', 'memory', 'E-JS-98']);

	// A token holding a very long array is spread without a stack overflow.
	const big = Array.from({ length: 200000 }, (v, i) => i);
	out.fillArgsLongList = eq(safe(() => U.fillArgs({ in: ['{r}'] }, { r: big }).value.in.length), 200000);
	return out;
});

// ---- j_boot: section J -------------------------------------------------------------------------------------------

section('j_boot', async () => {
	const out = {};
	const h = fresh();
	const csv = 't,v\n' + [1, 3, 2].map((v, i) => h.U.iso(OCT1 + i * HOUR) + ',' + v + '\n').join('');
	h.routes['/rest/s'] = () => textResponse(csv);
	h.routes['/rest/t'] = () => textResponse(csv);
	const seen = { s: [], t: [] };
	for (const id of Object.keys(seen))
		h.bus.subscribe('data:' + id,
			p => p && seen[id].push([p.status, !!h.D.get(Object.assign({}, p, { partial: true }))]));
	const contract = {
		cards: [{ id: 'c', type: 'chart', source: 'data:s', partialFrames: true },
			{ id: 'e', type: 'chart', source: 'data:t' }],
		dataSources: [SRC({ subscribes: [], chunk: null }),
			SRC({ id: 't', url: '/rest/t', subscribes: [], chunk: null }),
			{ id: 'd', kind: 'derived', from: 's', subscribes: [],
				transforms: [{ use: 'topN', args: { by: 'v', n: 1 } }] }]
	};
	const made = h.D.boot(contract);
	out.created = eq(made.map(m => m.id), ['s', 't', 'd']);
	out.firstHandles = eq(['s', 't', 'd'].map(id => h.bus.get('data:' + id).status),
		['loading', 'loading', 'loading']);
	await idle(h, 250);
	const partialLoading = id => seen[id].some(x => x[0] === 'loading' && x[1]);
	out.partialOnlyWhereACardAsks = eq([partialLoading('s'), partialLoading('t')], [true, false]);
	const d = h.bus.get('data:d');
	out.derivedReady = eq([d.status, h.U.rowsOf(h.D.get(d))], ['ready', [{ t: OCT1 + HOUR, v: 3 }]]);
	made.forEach(m => m.dispose());
	out.empty = eq([h.D.boot({}), h.D.boot({ dataSources: [] })], [[], []]);
	out.quiet = eq([h.errors, h.warns], [[], []]);

	// Bad entries (a source, then a derived) between good ones: boot throws the first error after trying every entry,
	// the good ones run, and neither bad one leaves a retained handle.
	const iso = fresh();
	iso.routes['/rest/s'] = () => textResponse(csv);
	iso.routes['/rest/t'] = () => textResponse(csv);
	const bad = SRC({ id: 'k', url: '/rest/s', subscribes: [{ topic: 'Not A Topic!', as: 'params' }], chunk: null });
	const badDerived = { id: 'dk', kind: 'derived', from: 's', transforms: [],
		subscribes: [{ topic: 'Bad 2!', as: 'params' }] };
	let thrown = null;
	try {
		iso.D.boot({ dataSources: [SRC({ subscribes: [], chunk: null }), bad, badDerived,
			SRC({ id: 't', url: '/rest/t', subscribes: [], chunk: null })] });
	} catch (e) { thrown = e; }
	await idle(iso, 250);
	const unretained = id => iso.bus.get('data:' + id) == null;
	out.badEntryIsolated = eq([thrown !== null, iso.bus.get('data:s').status, iso.bus.get('data:t').status,
		unretained('k')], [true, 'ready', 'ready', true]);
	out.firstErrorRethrown = eq([thrown && thrown.code, String(thrown && thrown.message).includes('Not A Topic!'),
		String(thrown && thrown.message).includes('Bad 2!')], ['E-JS-40', true, false]);
	out.badDerivedUnwound = eq(unretained('dk'), true);
	return out;
});

// ---- runner (sections above this line) ----

// A section that awaits a promise that never settles leaves node with nothing to do and exit code 0; name it instead.
let current = null, finished = false;
process.on('beforeExit', () => {
	if (finished) return;
	process.stdout.write(JSON.stringify({ [current]: { $error: 'section never settled' } }) + '\n');
	process.exitCode = 1;
});

(async () => {
	const out = {};
	for (const [name, fn] of sections) {
		current = name;
		try {
			out[name] = await fn();
		} catch (e) {
			out[name] = { $error: String(e && e.stack || e) };
		}
	}
	out.$bus = busLogs;
	finished = true;
	process.stdout.write(JSON.stringify(out) + '\n');
})();
