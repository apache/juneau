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
 * badges.cjs - drives juneau-badges.js against a scripted fetch and a captured timer queue: label templates,
 * hide-at-zero and pulse, 401/403 removal, stale + backoff, visibleWhen gating, scope resolution, drain-triggered
 * card refresh, tooltip open/close, and click navigation.  Prints one JSON report.
 *
 *   Usage:  node badges.cjs <juneau-badges.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const badgesPath = process.argv[2];
if (!badgesPath) {
	console.error('usage: node badges.cjs <juneau-badges.js>');
	process.exit(2);
}
const src = fs.readFileSync(badgesPath, 'utf8');

/** Builds a fresh page + script instance.  `responses` is a queue of {status, body|throws} consumed per fetch. */
function setup(responses, opts) {
	opts = opts || {};
	const env = makeConsoleEnv(opts.location ? { location: opts.location } : undefined);
	const doc = env.document;
	const win = env.window;
	win.window = win;
	win.document = doc;
	const errors = [], urls = [], assigned = [], timers = [], reloaded = [];
	const header = doc.createElement('div');
	header.className = 'jc-header-actions';
	doc.body.appendChild(header);
	win.location.assign = u => assigned.push(u);
	if (opts.contract) {
		win.JuneauConsole = {
			contract: () => opts.contract,
			refreshCard: id => { reloaded.push(id); return Promise.resolve(); }
		};
	}
	if (opts.urlState) win.JuneauViews = { urlState: opts.urlState };
	const sandbox = {
		window: win, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams, Proxy, Promise, JSON, Object, Array, Error, String, Math, Set, Map,
		encodeURIComponent,
		fetch: u => {
			urls.push(u);
			const r = responses.shift();
			if (!r) return Promise.reject(new Error('no scripted response'));
			if (r.throws) return Promise.reject(new Error(r.throws));
			return Promise.resolve({ status: r.status || 200, json: () => Promise.resolve(r.body) });
		},
		setTimeout: (fn, ms) => { timers.push({ fn, ms }); return timers.length; },
		clearTimeout: () => {}
	};
	vm.runInNewContext(src, sandbox, { filename: 'juneau-badges.js' });
	return { env, doc, win, NS: win.JuneauConsoleBadges, errors, urls, assigned, timers, reloaded, header };
}

const flush = () => new Promise(r => setTimeout(r, 0));
const badge = (doc, id) => doc.querySelector('[data-juneau-badge-id=' + id + ']');

async function tick(h) {
	const t = h.timers.pop();
	t.fn();
	await flush();
	return t.ms;
}

(async () => {
	const report = {};

	// (a)-(c) labels, role/aria-live, singular, mine suffix
	{
		const h = setup([{ body: { total: 3 } }, { body: { total: 1 } }, { body: { total: 2, mine: 1 } }]);
		h.NS.mount([{ id: 'pending', src: '/p' }]);
		await flush();
		const b = badge(h.doc, 'pending');
		report.plainLabel = b.textContent;
		report.role = b.getAttribute('role');
		report.ariaLive = b.getAttribute('aria-live');
		report.inHeaderBadges = b.parentNode.className;
		report.defaultTone = b.getAttribute('data-juneau-badge-tone');
		await tick(h);
		report.singularLabel = b.textContent;
		await tick(h);
		report.mineLabel = b.textContent;
		report.noPulseOnDecreaseOrFirst = true;
	}

	// custom label templates, tone mapping
	{
		const h = setup([{ body: { total: 2 } }]);
		h.NS.mount([{ id: 'x', src: '/p', tone: 'danger', label: { other: '{total} open' } }]);
		await flush();
		const b = badge(h.doc, 'x');
		report.customLabel = b.textContent;
		report.dangerTone = b.getAttribute('data-juneau-badge-tone');
	}

	// (d) zero hides, then >0 un-hides and pulses
	{
		const h = setup([{ body: { total: 0 } }, { body: { total: 2 } }]);
		h.NS.mount([{ id: 'z', src: '/p' }]);
		await flush();
		const b = badge(h.doc, 'z');
		report.zeroHidden = b.hidden === true;
		await tick(h);
		report.reshown = b.hidden === false;
		report.pulsed = b.classList.contains('jc-count-badge--pulse');
	}

	// (e) 403 removes the badge, logs E-JS-65 once, stops polling
	{
		const h = setup([{ status: 403, body: {} }]);
		h.NS.mount([{ id: 'f', src: '/p' }]);
		await flush();
		report.removedOn403 = badge(h.doc, 'f') === null;
		report.e65Count = h.errors.filter(e => e.includes('E-JS-65')).length;
		report.noRescheduleOn403 = h.timers.length === 0;
		report.fetchCountAfter403 = h.urls.length;
	}

	// (f) no numeric total: stale + E-JS-66 + backoff doubles
	{
		const h = setup([{ body: { total: 'x' } }, { body: {} }, { throws: 'down' }]);
		h.NS.mount([{ id: 's', src: '/p', refreshMs: 10000 }]);
		await flush();
		const b = badge(h.doc, 's');
		report.stale = b.classList.contains('jc-count-badge--stale');
		report.e66 = h.errors.some(e => e.includes('E-JS-66'));
		const d1 = await tick(h);
		const d2 = await tick(h);
		report.backoff = [d1, d2];
		report.e65OnNetworkError = h.errors.some(e => e.includes('E-JS-65') && e.includes('down'));
	}

	// refreshMs below the floor is clamped when scheduling
	{
		const h = setup([{ body: { total: 1 } }]);
		h.NS.mount([{ id: 'c', src: '/p', refreshMs: 100 }]);
		await flush();
		report.clampedInterval = h.timers[h.timers.length - 1].ms;
	}

	// visibleWhen: gated by facts, never fetched when hidden
	{
		const contract = { activeNav: ['a'], facts: { viewer: { roles: ['oncall'] } } };
		const h = setup([{ body: { total: 1 } }], { contract });
		h.NS.mount([
			{ id: 'yes', src: '/yes', visibleWhen: [{ field: 'viewer.roles', op: 'contains', value: 'oncall' }] },
			{ id: 'no', src: '/no', visibleWhen: { field: 'viewer.roles', op: 'contains', value: 'admin' } }
		]);
		await flush();
		report.visibleWhenShown = badge(h.doc, 'yes') !== null;
		report.visibleWhenHiddenNotMounted = badge(h.doc, 'no') === null;
		report.visibleWhenFetched = h.urls;
	}

	// scope: nav leaf resolves values onto the request; no match hides and keeps polling
	{
		const contract = { activeNav: ['rules', 'suspensions'], facts: {} };
		const h = setup([{ body: { total: 2 } }], { contract });
		h.NS.mount([{ id: 'sc', src: '/p', params: { a: 'b c' }, scope: { param: 'beanType', by: 'nav', values: { suspensions: ['Suspension', 'Hold'] } } }]);
		await flush();
		report.scopedUrl = h.urls[0];
		const h2 = setup([], { contract: { activeNav: ['other'], facts: {} } });
		h2.NS.mount([{ id: 'sc2', src: '/p', scope: { param: 'beanType', values: { suspensions: ['Suspension'] } } }]);
		await flush();
		report.unscopedPageNoFetch = h2.urls.length === 0;
		report.unscopedPageReschedules = h2.timers.length === 1;
	}

	// drain detection refreshes linked cards, only on an item leaving
	{
		const contract = { activeNav: [], facts: {} };
		const h = setup([
			{ body: { total: 2, items: [{ id: '1' }, { id: '2' }] } },
			{ body: { total: 3, items: [{ id: '1' }, { id: '2' }, { id: '3' }] } },
			{ body: { total: 2, items: [{ id: '2' }, { id: '3' }] } }
		], { contract });
		h.NS.mount([{ id: 'd', src: '/p', refreshes: ['rules-table'] }]);
		await flush();
		await tick(h);
		report.reloadedOnGrowth = h.reloaded.length;
		await tick(h);
		report.reloadedOnDrain = h.reloaded.slice();
	}

	// tooltip: opens on focus with the item cap, Escape and blur close it
	{
		const items = [];
		for (let i = 1; i <= 4; i++) items.push({ id: String(i), label: 'item ' + i, href: '/i/' + i });
		const h = setup([{ body: { total: 4, items } }]);
		h.NS.mount([{ id: 't', src: '/p', tooltipMax: 3 }]);
		await flush();
		const b = badge(h.doc, 't');
		b.dispatch('focus');
		const pop = h.doc.querySelector('.jc-count-badge-popover');
		report.tooltipOpen = pop !== null;
		report.tooltipRole = pop && pop.getAttribute('role');
		report.tooltipItems = pop ? pop.querySelectorAll('.jc-count-badge-popover-item').length : -1;
		report.tooltipMore = pop ? pop.querySelector('.jc-count-badge-popover-more').textContent : null;
		report.describedBy = b.getAttribute('aria-describedby') === (pop && pop.id);
		h.env.dispatchDocument('keydown', { key: 'Escape' });
		report.escapeCloses = h.doc.querySelector('.jc-count-badge-popover') === null;
		b.dispatch('mouseenter');
		const reopened = h.doc.querySelector('.jc-count-badge-popover') !== null;
		b.dispatch('blur');
		report.blurCloses = reopened && h.doc.querySelector('.jc-count-badge-popover') === null;
	}

	// (g)/(h) click navigation
	{
		const encoded = [];
		const urlState = { encode: s => { encoded.push(s); return 'filter(beanType=' + s.filters[0].expr + ')'; } };
		const contract = { activeNav: ['suspensions'], facts: {} };
		const h = setup([{ body: { total: 1 } }, { body: { total: 1 } }], { contract, urlState });
		h.NS.mount([
			{ id: 'plain', src: '/p', href: '/ui/changes' },
			{ id: 'filt', src: '/p', href: '/ui/changes', stateFilter: { column: 'beanType' }, scope: { param: 'beanType', values: { suspensions: ['Suspension'] } } }
		]);
		await flush();
		badge(h.doc, 'plain').dispatch('click');
		badge(h.doc, 'filt').dispatch('click');
		report.plainNav = h.assigned[0];
		report.filteredNav = h.assigned[1];
		report.encodeArg = encoded[0] && encoded[0].filters[0];
	}

	console.log(JSON.stringify(report));
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });
