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
 * Playwright driver for the console-bus browser suite (message bus Task 18).
 *
 *   node console-bus.cjs <scenario> <fixture.html> <assets.json> [query]
 *
 * Serves the fixture at http://bus.test/console-bus.html (a real http origin, so same-origin checks and location-derived
 * URLs behave as in production), answers /api/ requests itself (DataTables fetches through XMLHttpRequest, so this is
 * Playwright routing; the session POST and the SSE streams are faked in-page by console-bus/prelude.js), runs one
 * scenario, and prints one JSON report:
 *   { ...scenario fields, jsFailures, consoleErrors, busDebug }
 * It asserts nothing; the Java *_BrowserTest asserts.
 */
'use strict';
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROWS = [
	{ id: 'c-17', owner: 'owner-marker-7731', state: 'open', region: 'east' },
	{ id: 'c-21', owner: 'ab', state: 'closed', region: 'west' },
	{ id: 'c/9 x', owner: 'cd', state: 'open', region: 'west' }
];

// ---- In-page helpers: installed once, before the scenario (evaluated in the page, so self-contained) ----
function installHelpers() {
	const P = window.__probe = {};
	P.bus = () => window.JuneauViews && window.JuneauViews.bus;
	P.tick = () => new Promise(r => setTimeout(r, 0));
	P.settle = async () => { for (let i = 0; i < 5; i++) await P.tick(); await new Promise(requestAnimationFrame); };
	P.waitFor = async (pred, ms) => {
		const end = Date.now() + (ms || 5000);
		while (Date.now() < end) {
			try { if (pred()) return true; } catch (e) { /* not ready */ }
			await new Promise(r => setTimeout(r, 20));
		}
		throw new Error('waitFor timed out after ' + (ms || 5000) + 'ms: ' + pred);
	};
	P.card = id => document.getElementById(id);
	// The card's host: the parent of its body once mounted; the id-bearing element while a params card is still awaiting.
	// A mounted datatables card hands its id to the table and never gets it back, so the host is cached the first time it is found.
	const hosts = {};
	P.host = id => {
		if (hosts[id] && hosts[id].isConnected) return hosts[id];
		const b = document.getElementById(id + '-body');
		const h = b ? b.parentElement : document.getElementById(id);
		if (h && h.tagName !== 'TABLE') hosts[id] = h;
		return h;
	};
	P.empty = id => { const h = P.host(id); const e = h && h.querySelector('.jc-card-empty'); return e ? e.textContent.trim() : null; };
	P.text = id => { const h = P.host(id); return h ? h.textContent.trim() : null; };
	P.cardError = id => {
		const host = P.host(id);
		const e = (host || document).querySelector('.jc-card-bus-error');
		return e ? { code: e.getAttribute('data-juneau-error'), text: e.textContent.trim() } : null;
	};
	P.pageError = () => {
		const b = document.querySelector('.jc-console-error');
		if (!b) return null;
		const items = Array.from(b.querySelectorAll('.jc-console-error-item'));
		return { codes: items.map(e => e.getAttribute('data-juneau-error')), text: items.map(e => e.textContent.trim()).join(' | '), role: b.getAttribute('role') };
	};
	P.row = (cardId, rowId) => {
		const body = document.getElementById(cardId + '-body');
		return body ? Array.from(body.querySelectorAll('tr[data-juneau-row-id]')).find(r => r.getAttribute('data-juneau-row-id') === rowId) || null : null;
	};
	P.hasRow = (cardId, rowId) => !!P.row(cardId, rowId);
	P.selectRow = (cardId, rowId) => {
		const r = P.row(cardId, rowId);
		const cb = r && r.querySelector('.juneau-view-select-checkbox, input[type="checkbox"]');
		if (!cb) return false;
		cb.click();
		return true;
	};
	P.clickControl = (cardId, label) => {
		for (const root of [document.getElementById(cardId + '-body'), document]) {
			if (!root) continue;
			for (const el of root.querySelectorAll('button, [role="button"], [role="menuitemcheckbox"], [role="menuitemradio"], [role="menuitem"], a, label, input[type="checkbox"]')) {
				const t = (el.getAttribute('aria-label') || el.getAttribute('title') || el.textContent || '').trim();
				if (t === label) { el.click(); return true; }
			}
		}
		return false;
	};
	P.controls = cardId => {
		const c = document.getElementById(cardId + '-body');
		return c ? Array.from(c.querySelectorAll('button, [role="button"], [role="menuitemcheckbox"], [role="menuitemradio"], [role="menuitem"]'))
			.map(el => (el.getAttribute('aria-label') || el.getAttribute('title') || el.textContent || '').trim()) : [];
	};
	P.pressed = (cardId, label) => {
		const b = Array.from(document.getElementById(cardId + '-body').querySelectorAll('.juneau-view-ribbon-btn')).find(e => e.getAttribute('aria-label') === label);
		return b ? b.getAttribute('aria-pressed') : null;
	};
	P.fetches = pred => window.__busTest.fetches.filter(pred);
	P.lastFetch = pred => { const f = P.fetches(pred); return f.length ? f[f.length - 1] : null; };
	// columns[i].search.value for the column whose data/name is `key`, from the DataTables JSON request.
	P.columnSearch = (req, key) => {
		if (!req || !Array.isArray(req.columns)) return null;
		const c = req.columns.find(c => c && (c.data === key || c.name === key));
		return c && c.search ? c.search.value : null;
	};
	['changes', 'tasks', 'peers', 'byOwner'].forEach(P.host);
	P.sse = (seq, frame) => 'event: bus\nid: ' + seq + '\ndata: ' + JSON.stringify(Object.assign({ seq }, frame)) + '\n\n';
	P.isTasksFetch = f => /^\/api\/changes\/[^/]+\/tasks$/.test(f.path);
	P.isChangesFetch = f => f.path === '/api/changes';
	P.isPeersFetch = f => f.path === '/api/peers';
}

// ---- Scenarios (each evaluated in the page; they read window.__probe and window.__busTest) ----
const SCENARIOS = {
	'master-detail': async () => {
		const P = window.__probe, bus = P.bus(), out = {};
		await P.waitFor(() => P.hasRow('changes', 'c-17'));
		await P.settle();
		out.cardStates = ['changes', 'tasks', 'byOwner'].map(id => { const s = bus.get('card:' + id); return s && s.state; });
		out.initial = {
			tasksEmpty: P.empty('tasks'),
			tasksFetches: P.fetches(P.isTasksFetch).length,
			selection: bus.get('selection:changes')
		};
		out.selectClicked = P.selectRow('changes', 'c-17');
		await P.waitFor(() => P.fetches(P.isTasksFetch).length > 0);
		await P.waitFor(() => P.hasRow('tasks', 't-1'));
		await P.settle();
		out.selected = {
			tasksPaths: P.fetches(P.isTasksFetch).map(f => f.path),
			selection: bus.get('selection:changes'),
			tasksHasRow: P.hasRow('tasks', 't-1')
		};
		P.selectRow('changes', 'c-17');                 // toggle off: the selection empties
		await P.waitFor(() => P.empty('tasks') !== null);
		await P.settle();
		out.cleared = {
			tasksEmpty: P.empty('tasks'),
			tasksFetches: P.fetches(P.isTasksFetch).length,
			selection: bus.get('selection:changes')
		};
		P.selectRow('changes', 'c/9 x');                // an id that needs URL encoding
		await P.waitFor(() => P.fetches(P.isTasksFetch).length > out.cleared.tasksFetches);
		out.encoded = { lastTasksPath: P.lastFetch(P.isTasksFetch).path };
		return out;
	},

	ribbon: async () => {
		const P = window.__probe, T = window.__busTest, bus = P.bus(), out = {};
		await P.waitFor(() => P.hasRow('changes', 'c-17'));
		out.controls = P.controls('changes');
		P.selectRow('changes', 'c-17');
		await P.waitFor(() => P.fetches(P.isTasksFetch).length > 0);
		await P.settle();

		const tasksBefore = P.fetches(P.isTasksFetch).length;
		out.refreshTasksClicked = P.clickControl('changes', 'Refresh tasks');
		await P.waitFor(() => P.fetches(P.isTasksFetch).length > tasksBefore);
		out.refreshTasks = {
			tasksBefore,
			tasksAfter: P.fetches(P.isTasksFetch).length,
			cmdTasksInHistory: bus.history().some(h => h.topic === 'cmd:tasks')
		};

		let changesBefore = P.fetches(P.isChangesFetch).length;
		const rolesBefore = T.roleCalls.length;
		out.mineClicked = P.clickControl('changes', 'Mine only');
		await P.waitFor(() => { const f = bus.get('filter:changes'); return f && f.options && f.options.mine === true; });
		await P.settle();
		const byOwner = T.roleCalls.slice(rolesBefore).filter(c => c.card === 'byOwner' && c.role === 'filter');
		out.mine = {
			filter: bus.get('filter:changes'),
			pressed: P.pressed('changes', 'Mine only'),
			byOwnerLast: byOwner.length ? byOwner[byOwner.length - 1].payload : null,
			cmdChangesInHistory: bus.history().some(h => h.topic === 'cmd:changes')
		};

		changesBefore = P.fetches(P.isChangesFetch).length;
		out.openOnlyClicked = P.clickControl('changes', 'Open only');
		await P.waitFor(() => P.fetches(P.isChangesFetch).length > changesBefore);
		await P.settle();
		out.columnScoped = { stateSearch: P.columnSearch(P.lastFetch(P.isChangesFetch).request, 'state') };

		const peersBefore = P.fetches(P.isPeersFetch).length;
		out.focusEastClicked = P.clickControl('changes', 'Focus east');
		await P.waitFor(() => P.fetches(P.isPeersFetch).length > peersBefore);
		out.focusEast = {
			retained: bus.get('app.region-picked'),
			peersFetches: P.fetches(P.isPeersFetch).length,
			regionSearch: P.columnSearch(P.lastFetch(P.isPeersFetch).request, 'region')
		};
		return out;
	},

	trace: async () => {
		const P = window.__probe, bus = P.bus(), out = {};
		await P.waitFor(() => P.hasRow('changes', 'c-17'));
		P.selectRow('changes', 'c-17');
		await P.waitFor(() => P.fetches(P.isTasksFetch).length > 0);
		await P.settle();
		out.history = bus.history();
		return out;
	},

	'loud-failure': async () => {
		const P = window.__probe, out = {};
		await P.waitFor(() => P.cardError('tasks') !== null || P.pageError() !== null);
		await P.settle();
		out.tasksError = P.cardError('tasks');
		out.changesError = P.cardError('changes');
		out.pageError = P.pageError();
		return out;
	},

	'missing-bus': async () => {
		const P = window.__probe, out = {};
		await P.waitFor(() => P.pageError() !== null);
		await P.settle();
		out.pageError = P.pageError();
		out.hasBus = !!P.bus();
		out.dataFetches = P.fetches(f => f.path.startsWith('/api/')).length;
		return out;
	},

	bridge: async () => {
		const P = window.__probe, T = window.__busTest, bus = P.bus(), out = {};
		const state = () => { const b = bus.get('bridge:ops'); return b && b.state; };
		const resync = (s, seq, running) => {
			s.push(P.sse(seq, { v: 1, type: 'resync-begin' }));
			s.push(P.sse(seq + 1, { v: 1, type: 'pub', topic: 'ops.jobs', payload: { schemaVersion: 1, running }, retained: true }));
			s.push(P.sse(seq + 2, { v: 1, type: 'resync-end' }));
		};

		await P.waitFor(() => T.streams.length === 1);
		const post = P.lastFetch(f => f.path.endsWith('/juneau-bus/session'));
		out.sessionPost = { path: post.path, method: post.method, csrf: post.headers['x-csrf-token'] || null, body: post.request };

		resync(T.streams[0], 1, []);
		await P.waitFor(() => state() === 'open');
		const r0 = T.renders.jobs || 0;
		out.firstOpen = { bridge: bus.get('bridge:ops'), opsJobs: bus.get('ops.jobs'), jobsRenders: r0 };

		T.streams[0].push(P.sse(4, { v: 1, type: 'pub', topic: 'ops.jobs', retained: true,
			payload: { schemaVersion: 1, running: [{ jobId: 'j-1', title: 'Rebuild', state: 'running', percent: 10 }] } }));
		await P.waitFor(() => (T.renders.jobs || 0) > r0);
		out.live = { opsJobs: bus.get('ops.jobs'), jobsRendersBefore: r0, jobsRendersAfter: T.renders.jobs };

		T.streams[0].end();                                           // drop: stream end is retryable
		await P.waitFor(() => state() === 'reconnecting');
		out.dropped = { bridge: bus.get('bridge:ops'), streams: T.streams.length };

		T.advance(bus.get('bridge:ops').nextRetryMs);                 // the virtual clock fires the reconnect timer
		await P.waitFor(() => T.streams.length === 2);
		resync(T.streams[1], 5, []);                                  // j-1 finished while the stream was down
		await P.waitFor(() => state() === 'open');
		out.recovered = {
			bridge: bus.get('bridge:ops'),
			opsJobs: bus.get('ops.jobs'),
			sessionPosts: P.fetches(f => f.path.endsWith('/juneau-bus/session')).length,
			streamPaths: P.fetches(f => f.path.includes('/juneau-bus/stream/')).map(f => f.path)
		};
		out.states = T.roleCalls.filter(c => c.card === 'opsStatus').map(c => c.payload && c.payload.state);
		out.statusError = P.pageError();
		return out;
	},

	'bridge-denied': async () => {
		const P = window.__probe, T = window.__busTest, bus = P.bus(), out = {};
		await P.waitFor(() => P.pageError() !== null);
		await P.settle();
		out.pageError = P.pageError();
		out.bridge = bus.get('bridge:ops');
		out.streams = T.streams.length;
		return out;
	}
};

function answer(urlPath, req) {
	const draw = Number(req.draw || 1);
	let rows = [];
	if (urlPath === '/api/changes' || urlPath === '/api/peers')
		rows = ROWS;
	else {
		const m = urlPath.match(/^\/api\/changes\/([^/]+)\/tasks$/);
		if (m)
			rows = [{ id: 't-1', name: 'task for ' + decodeURIComponent(m[1]) }];
	}
	return { draw, recordsTotal: rows.length, recordsFiltered: rows.length, data: rows };
}

(async () => {
	const [scenario, fixture, assetsFile, query] = process.argv.slice(2);
	if (!scenario || !fixture || !SCENARIOS[scenario]) {
		process.stderr.write('usage: node console-bus.cjs <' + Object.keys(SCENARIOS).join('|') + '> <page.html> <assets.json> [query]\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const html = fs.readFileSync(path.resolve(fixture), 'utf8');
	const assets = JSON.parse(fs.readFileSync(path.resolve(assetsFile), 'utf8'));
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const jsFailures = [], consoleErrors = [], busDebug = [];
		page.on('response', r => { if (r.status() >= 400) consoleErrors.push('HTTP ' + r.status() + ' ' + r.url()); });
		page.on('pageerror', e => jsFailures.push(String(e)));
		page.on('console', m => {
			if (m.type() === 'error') consoleErrors.push(m.text());
			if (m.type() === 'debug' && m.text().startsWith('[bus]')) busDebug.push(m.text());
		});
		await page.route('http://bus.test/**', async route => {
			const req = route.request();
			const u = new URL(req.url());
			if (u.pathname === '/juneau-symbols.svg') {
				await route.fulfill({ status: 200, contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg"/>' });
			} else if (u.pathname.startsWith('/api/')) {
				const body = req.postData();
				let parsed = {};
				try { parsed = body ? JSON.parse(body) : {}; } catch (e) { parsed = {}; }
				const headers = {};
				for (const [k, v] of Object.entries(req.headers())) headers[k.toLowerCase()] = v;
				const rec = { url: u.pathname + u.search, path: u.pathname, method: req.method(), headers, body, request: parsed };
				await page.evaluate(r => window.__busTest.fetches.push(r), rec).catch(() => {});
				await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(answer(u.pathname, parsed)) });
			} else if (assets[u.pathname]) {
				await route.fulfill({ status: 200, contentType: assets[u.pathname].type, body: assets[u.pathname].body });
			} else if (u.pathname === '/console-bus.html') {
				await route.fulfill({ status: 200, contentType: 'text/html', body: html });
			} else {
				await route.fulfill({ status: 404, contentType: 'text/plain', body: 'not found: ' + u.pathname });
			}
		});
		await page.goto('http://bus.test/console-bus.html' + (query || ''));
		await page.evaluate(() => new Promise(requestAnimationFrame));
		await page.evaluate(installHelpers);
		const report = await page.evaluate(SCENARIOS[scenario]);
		report.jsFailures = jsFailures.slice();
		report.consoleErrors = consoleErrors.slice();
		report.busDebug = busDebug.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
