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
 * juneau-console.cjs - Node harness for juneau-console.js. Builds #juneau-page contract
 * pages, loads the real production script into a fresh vm sandbox PER CASE (via console-dom-shim.cjs), and prints
 * ONE JSON report that ConsoleJs_Shell_Test.java asserts against.
 *
 * Usage: node juneau-console.cjs <path-to-juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require('./console-dom-shim.cjs');

const shellPath = process.argv[2];
if (!shellPath) {
	process.stderr.write('usage: node juneau-console.cjs <path-to-juneau-console.js>\n');
	process.exit(2);
}
// NOSONAR javascript:S1523 -- loading the production juneau-console.js source into a VM sandbox is this harness's
// intended mechanism for exercising it under the DOM shim; the path is a fixed local file supplied by the test.
const shellSrc = fs.readFileSync(path.resolve(shellPath), 'utf8');

/** Shallow helper: wraps a bare object as a frozen-looking page contract with version "1" unless overridden. */
function C(partial) {
	return { contractVersion: '1', ...partial };
}

/** Appends a `<template data-{attr}="{id}">` to doc.body, with `build(doc, templateContentNode)` filling it in. */
function addTpl(doc, attr, id, build) {
	const t = doc.createElement('template');
	t.setAttribute(attr, id);
	if (build) build(doc, t.content);
	doc.body.appendChild(t);
	return t;
}

/** A `<p>{text}</p>` template-content builder, for addTpl's `build` parameter. */
function p(text) {
	return function (doc, frag) {
		const e = doc.createElement('p');
		e.textContent = text;
		frag.appendChild(e);
	};
}

/** Reads a querySelector(sel) match's textContent, or null when absent. */
function txt(doc, sel) {
	const n = doc.querySelector(sel);
	return n ? n.textContent : null;
}

/** Reads the `.jc-console-error` banner's textContent (all `fail()` calls on this doc so far), or null. */
function bannerText(doc) {
	return txt(doc, '.jc-console-error');
}

/**
 * Builds a fresh env + sandbox, loads the shell, and (unless `contract === undefined`) auto-mounts a
 * `#juneau-page` script island carrying `contract`. Returns `{ env, doc, errors, threw, result, JC }`.
 *
 * @param contract Object (JSON-stringified) or raw string (used verbatim as island textContent); undefined to
 *   skip the island entirely (manual-mount cases call `r.JC.mount(...)` themselves afterwards).
 * @param templates Array of `[attr, id, build]` triples, each appended via addTpl before the shell loads.
 * @param opts Optional `{ location, before(sandbox), after(sandbox, doc) }`.
 */
function run(contract, templates, opts) {
	const env = makeConsoleEnv(opts);
	const doc = env.document;
	const errors = [];
	const body = doc.body;

	if (contract !== undefined) {
		const island = doc.createElement('script');
		island.setAttribute('type', 'application/json');
		island.id = 'juneau-page';
		island.textContent = typeof contract === 'string' ? contract : JSON.stringify(contract);
		body.appendChild(island);
	}
	for (const [attr, id, build] of templates || []) addTpl(doc, attr, id, build);

	doc.readyState = 'loading';
	let threw = null, result = null;
	const sandbox = {
		window: env.window,
		document: doc,
		console: { error: m => errors.push(String(m)), log: () => { /* no-op */ }, warn: () => { /* no-op */ } },
		URLSearchParams: env.window.URLSearchParams,
		CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, Number, AbortController,
		fetch: () => Promise.reject(new Error('no fetch in harness')),
		setTimeout: fn => fn()
	};
	env.window.window = env.window;
	env.window.document = doc;

	if (opts?.before) opts.before(sandbox, env);
	doc.addEventListener('juneau:console-mounted', ev => { result = ev.detail; });

	try {
		// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
		vm.runInNewContext(shellSrc, sandbox, { filename: 'juneau-console.js' }); // NOSONAR javascript:S1523 -- harness evaluates the module's own bundled script, a fixed local file
	} catch (error) {
		threw = { name: error.name, code: error.code, message: error.message };
	}

	if (opts?.after) opts.after(sandbox, doc);
	env.fireDOMContentLoaded();

	return { env, doc, errors, threw, result, JC: sandbox.window.JuneauConsole };
}

const out = {};

//----------------------------------------------------------------------------------------------------------------
// Golden case (header.chrome wrapper, a slotted banner, a bare html card from a <template data-card>, a footer)
//----------------------------------------------------------------------------------------------------------------
(function golden() {
	const contract = C({
		activeNav: ['home', 'setup'],
		nav: [
			{ id: 'home', label: 'Home', href: '/', children: [
				{ id: 'about', label: 'About', href: '/about' },
				{ id: 'setup', label: 'Setup', href: '/setup' }
			] },
			{ id: 'docs', label: 'Docs', href: '/docs' }
		],
		header: { title: 'Juneau Console', chrome: true, slots: { banner: 'header.banner' } },
		cards: [{ id: 'jc-seg-1', type: 'html', template: 'jc-seg-1', bare: true }],
		footer: { text: 'footer text' }
	});
	const r = run(contract, [
		['data-slot', 'header.banner', p('banner')],
		['data-card', 'jc-seg-1', p('segment')]
	]);
	out.golden = { threw: r.threw, errors: r.errors };
	const topLevel = r.doc.body.childNodes.filter(n => n.nodeType === 1);
	out.golden.order = topLevel.map(n => {
		const cls = n.getAttribute('class');
		return n.tagName.toLowerCase() + (cls ? '.' + cls.split(' ')[0] : '') + (n.id ? '#' + n.id : '');
	});
	out.golden.rows = r.doc.querySelectorAll('.juneau-page-nav-children').length;
	out.golden.mainText = txt(r.doc, 'main.jc-main');
	out.golden.templatesLeft = r.doc.querySelectorAll('template').length;
	out.golden.cardType = r.JC.card('jc-seg-1')?.type;
	out.golden.activeNavSource = r.result?.activeNavSource;
	out.golden.activeNav = r.result?.activeNav;
	out.golden.contractHeaderTitle = r.JC.contract()?.header.title;
})();

//----------------------------------------------------------------------------------------------------------------
// footer.text renders as trusted HTML (P24 revised 2026-10-02): an entity decodes and inline markup becomes a
// real element, not literal escaped text.
//----------------------------------------------------------------------------------------------------------------
(function footerHtml() {
	const contract = C({
		footer: { text: 'Copyright &mdash; <strong>Juneau</strong> contributors' }
	});
	const r = run(contract, []);
	out.footerHtml = {
		threw: r.threw,
		errors: r.errors,
		text: txt(r.doc, 'footer.jc-page-footer')
	};
	// Scoped to the footer element itself (not a 'footer.jc-page-footer strong' descendant selector on the
	// document) because this harness's compileSelector (views-dom-shim.cjs) matches simple compound selectors
	// only - it has no descendant-combinator support, so a space-joined selector would just drop the " strong".
	const footer = r.doc.querySelector('footer.jc-page-footer');
	const strong = footer?.querySelector('strong');
	out.footerHtml.strongTagName = strong ? strong.tagName.toLowerCase() : null;
	out.footerHtml.strongText = strong ? strong.textContent : null;
})();

//----------------------------------------------------------------------------------------------------------------
// Nav depth 1-6
//----------------------------------------------------------------------------------------------------------------
(function navDepth() {
	out.depth = {};
	for (let depth = 1; depth <= 6; depth++) {
		const ids = [];
		for (let i = 0; i < depth; i++) ids.push('d' + i);
		let leaf = { id: ids[depth - 1], label: 'L' + (depth - 1) };
		for (let i = depth - 2; i >= 0; i--) leaf = { id: ids[i], label: 'L' + i, children: [leaf] };
		const contract = C({ activeNav: ids, nav: [leaf] });
		const r = run(contract, []);
		const rowCount = r.doc.querySelectorAll('.juneau-page-nav-sections, .juneau-page-nav-children').length;
		const current = r.doc.querySelectorAll('[aria-current="page"]').length;
		out.depth[depth] = { threw: r.threw, rowCount, current, activeNav: r.result?.activeNav };
	}
})();

//----------------------------------------------------------------------------------------------------------------
// Nav aria-label: header.title, else contract title, else attribute omitted
//----------------------------------------------------------------------------------------------------------------
(function navAriaLabel() {
	const nav = [{ id: 'home', label: 'Home', href: '/' }];
	const label = c => {
		const r = run(C(c), []);
		const n = r.doc.querySelector('nav.juneau-page-nav');
		return { threw: r.threw, present: !!n, label: n ? n.getAttribute('aria-label') : null, has: n ? n.getAttribute('aria-label') !== null : null };
	};
	out.navLabel = {
		header: label({ activeNav: ['home'], nav, title: 'Page Title', header: { title: 'Header Title' } }),
		contract: label({ activeNav: ['home'], nav, title: 'Page Title' }),
		none: label({ activeNav: ['home'], nav, title: '', header: { title: '' } })
	};
})();

//----------------------------------------------------------------------------------------------------------------
// Prefix-fallback matching (no activeNav supplied; window.location drives the match)
//----------------------------------------------------------------------------------------------------------------
(function prefixFallback() {
	const contract = C({
		nav: [
			{ id: 'admin', label: 'Admin', href: '/admin', children: [
				{ id: 'users', label: 'Users', href: '/admin/users' }
			] },
			{ id: 'home', label: 'Home', href: '/' }
		]
	});
	const r = run(contract, [], { location: { pathname: '/admin/users', search: '?tab=active' } });
	out.fallback = {
		threw: r.threw,
		activeNav: r.result?.activeNav,
		activeNavSource: r.result?.activeNavSource
	};
})();

//----------------------------------------------------------------------------------------------------------------
// E-JS-1 .. E-JS-14
//----------------------------------------------------------------------------------------------------------------
out.errors = {};

// E-JS-1: unparseable island JSON (fatal, thrown out of auto-mount).
(function () {
	const r = run('{not valid json', []);
	out.errors['1'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-2: unsupported contract version (fatal).
(function () {
	const r = run({ contractVersion: '9' }, []);
	out.errors['2'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-2 (renamed): a leftover pre-rename 'version' key gets a dedicated diagnostic (fatal; never read for behavior).
(function () {
	const r = run({ version: '1' }, []);
	out.errors['2r'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-3: card references a template id that was never registered (fatal; caught by checkRefs() before the
// cards loop even runs, so the handler's own ctx.template() throw is never reached).
(function () {
	const r = run(C({ cards: [{ id: 'c', type: 'html', template: 'missing' }] }), []);
	out.errors['3'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-4: a card type still unregistered at DOMContentLoaded (not fatal; the card stays in result.cards as a pending host).
(function () {
	const r = run(C({ cards: [{ id: 'k', type: 'kpi' }] }), []);
	out.errors['4'] = {
		threw: r.threw, errors: r.errors, banner: bannerText(r.doc),
		resultCards: r.result && Object.keys(r.result.cards)
	};
})();

// E-JS-5: activeNav path that does not exist in the nav tree (fatal).
(function () {
	const r = run(C({ nav: [{ id: 'a', label: 'A' }], activeNav: ['a', 'b'] }), []);
	out.errors['5'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-6: duplicate nav id (fatal).
(function () {
	const r = run(C({ nav: [{ id: 'a', label: 'A1' }, { id: 'a', label: 'A2' }] }), []);
	out.errors['6'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-7: duplicate <template data-card="..."> (fatal).
(function () {
	const r = run(C({}), [
		['data-card', 'c', p('one')],
		['data-card', 'c', p('two')]
	]);
	out.errors['7'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-8: a registered handler throws (not fatal; the card host stays in result.cards). registerCard() must run against
// the SAME sandbox/realm the shell auto-mounted into, so this drives mount() manually (no island) rather than
// trying to register the type before an auto-mounting load.
(function () {
	const r = run(undefined, []);
	r.JC.registerCard('boom', function () { throw new Error('kaput'); });
	let threw2 = null, result2 = null;
	try {
		result2 = r.JC.mount(C({ cards: [{ id: 'b', type: 'boom' }] }), { root: r.doc.body, document: r.doc });
	} catch (error) {
		threw2 = { name: error.name, code: error.code, message: error.message };
	}
	out.errors['8'] = {
		threw: threw2,
		errors: r.errors,
		banner: bannerText(r.doc),
		resultCards: result2 && Object.keys(result2.cards)
	};

	// registerCard contract: bad type name.
	let bad = null;
	try { r.JC.registerCard('Bad', () => { /* no-op */ }); } catch (error) { bad = { name: error.name, code: error.code }; }
	out.registerBadType = bad;
})();

// E-JS-9: unsafe nav href (not fatal; link element is never appended).
(function () {
	const r = run(C({ nav: [{ id: 'bad', label: 'X', href: 'javascript:alert(1)' }] }), []);
	out.errors['9'] = {
		threw: r.threw,
		errors: r.errors,
		banner: bannerText(r.doc),
		linkRendered: !!r.doc.querySelector('[data-juneau-nav-id="bad"]')
	};
})();

// E-JS-10: datatables card still pending at DOMContentLoaded (not fatal).
(function () {
	const r = run(C({ cards: [{ id: 't', type: 'datatables', table: {} }] }), []);
	out.errors['10'] = { threw: r.threw, errors: r.errors, banner: bannerText(r.doc) };
})();

// E-JS-11: JuneauConsole.mount called twice on the same root (fatal).
(function () {
	const r = run(undefined, []);
	let threw2 = null;
	r.JC.mount(C({}), { root: r.doc.body, document: r.doc });
	try {
		r.JC.mount(C({}), { root: r.doc.body, document: r.doc });
	} catch (error) {
		threw2 = { name: error.name, code: error.code, message: error.message };
	}
	out.errors['11'] = { threw: threw2 };
})();

// E-JS-12: registerCard called twice for the same type (fatal).
(function () {
	const r = run(undefined, []);
	r.JC.registerCard('dup-type', function () { /* no-op */ });
	let threw2 = null;
	try {
		r.JC.registerCard('dup-type', function () { /* no-op */ });
	} catch (error) {
		threw2 = { name: error.name, code: error.code, message: error.message };
	}
	out.errors['12'] = { threw: threw2 };
})();

// E-JS-13: console-output card with no JuneauViews.consoleOutput loaded (not fatal; deferred to DOMContentLoaded).
(function () {
	const r = run(C({ cards: [{ id: 'co', type: 'console-output', output: { linesUrl: '/runs/1/lines' } }] }), []);
	out.errors['13'] = {
		threw: r.threw,
		errors: r.errors,
		banner: bannerText(r.doc),
		bodyRendered: !!r.doc.querySelector('#co-body.jc-card-body')
	};
})();

// E-JS-14: run-view card with no JuneauViews.runView loaded (not fatal; deferred to DOMContentLoaded).
(function () {
	const r = run(C({ cards: [{ id: 'rv', type: 'run-view', runView: { eventsUrl: '/runs/1/events' } }] }), []);
	out.errors['14'] = {
		threw: r.threw,
		errors: r.errors,
		banner: bannerText(r.doc),
		bodyRendered: !!r.doc.querySelector('#rv-body.jc-card-body')
	};
})();

// E-JS-8 (console-output): JuneauViews.consoleOutput.mount throws synchronously inside onReady.  Not fatal; the card
// host stays in the DOM because the throw happens after mount() returned.
(function () {
	const r = run(C({ cards: [{ id: 'co', type: 'console-output', output: {} }] }), [], {
		before: sandbox => {
			sandbox.window.JuneauViews = {
				consoleOutput: {
					mount: function () { throw new Error("console-output region 'co-body': linesUrl is required"); }
				}
			};
		}
	});
	out.errors['8co'] = {
		threw: r.threw,
		errors: r.errors,
		banner: bannerText(r.doc),
		hostRendered: !!r.doc.querySelector('#co')
	};
})();

//----------------------------------------------------------------------------------------------------------------
// Custom card type registration
//----------------------------------------------------------------------------------------------------------------
(function customCardType() {
	const r = run(undefined, []);
	r.JC.registerCard('kpi', function (card, el0) {
		el0.textContent = card.title + ': ' + card.value;
	});
	const result = r.JC.mount(C({ cards: [{ id: 'k1', type: 'kpi', title: 'Users', value: 42 }] }), {
		root: r.doc.body, document: r.doc
	});
	out.custom = {
		cardText: txt(r.doc, '#k1'),
		resultHasCard: !!result.cards.k1
	};
})();

//----------------------------------------------------------------------------------------------------------------
// Console-output bridge: with JuneauViews.consoleOutput stubbed, each console-output card calls mount() once on
// DOMContentLoaded with (its own "<id>-body" element, a copy of card.output, {}).
//----------------------------------------------------------------------------------------------------------------
(function consoleOutputBridge() {
	const calls = [];
	let callsBeforeReady = -1;
	const r = run(C({ cards: [
		{ id: 'co1', type: 'console-output', title: 'Output', output: { linesUrl: '/runs/1/lines', rows: 20, title: 'Build' } },
		{ id: 'co2', type: 'console-output', output: { linesUrl: '/runs/2/lines' } }
	] }), [], {
		before: sandbox => {
			sandbox.window.JuneauViews = {
				consoleOutput: {
					mount: function (el, options, ctx) {
						calls.push({
							elId: el.id,
							elClass: el.className,
							hostId: el.parentNode ? el.parentNode.id : null,
							options: options,
							ctx: ctx
						});
						return function () { /* cleanup is discarded by the shell */ };
					}
				}
			};
		},
		after: () => { callsBeforeReady = calls.length; }
	});
	const idCounts = {};
	for (const e of r.doc.querySelectorAll('[id]')) idCounts[e.id] = (idCounts[e.id] || 0) + 1;
	out.consoleOutput = {
		threw: r.threw,
		errors: r.errors,
		callsBeforeReady: callsBeforeReady,
		calls: calls,
		cardTitle: txt(r.doc, '.jc-card-title'),
		resultCards: r.result ? Object.keys(r.result.cards) : null,
		duplicateIds: Object.keys(idCounts).filter(k => idCounts[k] > 1)
	};
})();

//----------------------------------------------------------------------------------------------------------------
// Run-view bridge: with JuneauViews.runView stubbed, each run-view card calls mount() once on DOMContentLoaded with
// (its own "<id>-body" element, a copy of card.runView, {}), and a throwing mount is an E-JS-8 that keeps the host.
//----------------------------------------------------------------------------------------------------------------
(function runViewBridge() {
	const calls = [];
	let callsBeforeReady = -1;
	const r = run(C({ cards: [
		{ id: 'rv1', type: 'run-view', title: 'Run', runView: { eventsUrl: '/runs/1/events', compact: true } },
		{ id: 'rv2', type: 'run-view', runView: { poll: false } }
	] }), [], {
		before: sandbox => {
			sandbox.window.JuneauViews = {
				runView: {
					mount: function (el, options, ctx) {
						calls.push({
							elId: el.id,
							elClass: el.className,
							hostId: el.parentNode ? el.parentNode.id : null,
							options: options,
							ctx: ctx
						});
						return function () { /* cleanup is discarded by the shell */ };
					}
				}
			};
		},
		after: () => { callsBeforeReady = calls.length; }
	});
	const idCounts = {};
	for (const e of r.doc.querySelectorAll('[id]')) idCounts[e.id] = (idCounts[e.id] || 0) + 1;
	out.runView = {
		threw: r.threw,
		errors: r.errors,
		callsBeforeReady: callsBeforeReady,
		calls: calls,
		cardTitle: txt(r.doc, '.jc-card-title'),
		resultCards: r.result ? Object.keys(r.result.cards) : null,
		duplicateIds: Object.keys(idCounts).filter(k => idCounts[k] > 1)
	};
})();

(function runViewThrows() {
	const r = run(C({ cards: [{ id: 'rv', type: 'run-view', runView: {} }] }), [], {
		before: sandbox => {
			sandbox.window.JuneauViews = {
				runView: { mount: function () { throw new Error("run-view region 'rv-body': eventsUrl is required"); } }
			};
		}
	});
	out.errors['8rv'] = {
		threw: r.threw,
		errors: r.errors,
		banner: bannerText(r.doc),
		hostRendered: !!r.doc.querySelector('#rv')
	};
})();

//----------------------------------------------------------------------------------------------------------------
// ADDENDUM 1: href safety, exercised indirectly via nav-link rendering (isSafeHref/isProtocolRelativeUrl are not
// exported - see the Task 5 addendum).  A rejected href never gets a DOM element at all (see link()'s
// `if (!a) continue;` in renderNav's row() closure); an accepted href keeps the exact original string.
//----------------------------------------------------------------------------------------------------------------
(function hrefSafetyNav() {
	const rejectHrefs = [
		'jav\tascript:alert(1)', 'java\nscript:alert(1)', '\u0001javascript:alert(1)', 'JaVaScRiPt:alert(1)',
		'data:text/html,<script>alert(1)</script>', 'vbscript:msgbox(1)', '//evil.example/x', String.raw`/\evil.example/x`
	];
	const acceptHrefs = ['foo/bar', '/path/to/thing', '#frag', 'https://example.com/ok', 'mailto:a@example.com'];
	const cases = rejectHrefs.map((h, i) => ({ id: 'rej' + i, href: h, expect: 'reject' }))
		.concat(acceptHrefs.map((h, i) => ({ id: 'acc' + i, href: h, expect: 'accept' })));
	const contract = C({ nav: cases.map(c => ({ id: c.id, label: c.id, href: c.href })) });
	const r = run(contract, []);
	const results = {};
	for (const c of cases) {
		const a = r.doc.querySelector('[data-juneau-nav-id="' + c.id + '"]');
		results[c.id] = {
			expect: c.expect,
			rendered: !!a,
			href: a ? a.getAttribute('href') : null
		};
	}
	out.hrefSafety = { nav: results, threw: r.threw, errorCount: r.errors.length };
})();

//----------------------------------------------------------------------------------------------------------------
// ADDENDUM 2: htmlCard()'s same-origin src gate also uses isSafeHref - a bare template-less html card with an
// unsafe `src` must surface as a card-level E-JS-8 ("... is not same-origin"), never fetch().
//----------------------------------------------------------------------------------------------------------------
(function hrefSafetyHtmlCardSrc() {
	const srcCases = ['javascript:alert(1)', '//evil.example/x'];
	const results = {};
	srcCases.forEach((src, i) => {
		const r = run(C({ cards: [{ id: 'hs' + i, type: 'html', src: src }] }), []);
		results[src] = {
			errors: r.errors,
			// The failed card's host stays in <main>, but nothing is painted inside it.
			hostContent: ((r.doc.querySelector('main.jc-main') || { childNodes: [] }).childNodes.find(n => n.id === 'hs' + i) || { childNodes: [] }).childNodes.length
		};
	});
	out.hrefSafety.htmlCardSrc = results;
})();

//----------------------------------------------------------------------------------------------------------------
// ADDENDUM 3: mounting on two DIFFERENT roots of the SAME document (header.chrome + userMenu on both, so
// renderUserMenu -> wireFallbackEscape(doc) runs twice) must wire the keydown listener exactly ONCE, not twice -
// the fallbackEscapeWired WeakSet-keyed-by-doc guard in juneau-console.js is the fix this regression test pins.
//----------------------------------------------------------------------------------------------------------------
(function keydownListenerLeak() {
	const r = run(undefined, []);
	const rootA = r.doc.createElement('div');
	const rootB = r.doc.createElement('div');
	r.doc.body.appendChild(rootA);
	r.doc.body.appendChild(rootB);
	const mkContract = label => C({
		header: { title: label, chrome: true, userMenu: { label: label, items: [] } }
	});
	r.JC.mount(mkContract('A'), { root: rootA, document: r.doc });
	r.JC.mount(mkContract('B'), { root: rootB, document: r.doc });
	out.hrefSafety.keydownListenerCountAfterTwoMounts = r.env.listenerCount('keydown');
})();

process.stdout.write(JSON.stringify(out));
