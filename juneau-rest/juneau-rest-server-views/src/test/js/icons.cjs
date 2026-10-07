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
 * icons.cjs - always-on Node harness for juneau-icons.js's page-level sprite layering API.
 *
 * Loads the REAL juneau-icons.js into a fresh vm sandbox per scenario over a small SVG-sprite shim (a
 * regex DOMParser that surfaces <symbol id="juneau-sym-*"> plus createElementNS/importNode), drives its
 * fetch with a per-scenario URL map, and reports the load Promise's result so the Java test can assert
 * the locked lookup order (override -> replacement -> shipped), partial replacement, failed-layer skip,
 * late-registration ignore, name<->stem publishing, and prefixed-id equivalence.
 *
 *   Usage:  node icons.cjs <juneau-icons.js> <juneau-symbols.svg>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const iconsJsPath = process.argv[2];
const symbolsSvgPath = process.argv[3];
if (!iconsJsPath || !symbolsSvgPath) {
	console.error('usage: node icons.cjs <juneau-icons.js> <juneau-symbols.svg>');
	process.exit(2);
}

const ICONS_JS = fs.readFileSync(path.resolve(iconsJsPath), 'utf8');
const SHIPPED_SVG = fs.readFileSync(path.resolve(symbolsSvgPath), 'utf8');

/** A single <symbol> node: enough of the DOM surface juneau-icons.js touches (id read, append, remove). */
function symbolNode(id, innerHTML) {
	return {
		nodeType: 1, tagName: 'SYMBOL', attrs: { id: id }, childNodes: [], parentNode: null, innerHTML: innerHTML || '',
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = String(v); },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		remove: function () { if (this.parentNode) this.parentNode.removeChild(this); /* NOSONAR javascript:S7762 -- this IS the shim's remove(); it must delegate to the shim's own removeChild(). */ }
	};
}

/** A regex DOMParser sufficient for the flat, single-level sprite files: extracts <symbol id="..."> nodes. */
function FakeDOMParser() { /* no-op */ }
FakeDOMParser.prototype.parseFromString = function (xml, type) {
	const isSvg = /<svg[\s>]/.test(String(xml));
	const symbols = [];
	const re = /<symbol\b([^>]*)>([\s\S]*?)<\/symbol>/g;
	let m;
	for (m = re.exec(xml); m; m = re.exec(xml)) {
		const idm = /id\s*=\s*"([^"]*)"/.exec(m[1]);
		symbols.push(symbolNode(idm ? idm[1] : '', m[2]));
	}
	const root = isSvg ? {
		nodeName: 'svg',
		getElementsByTagName: function (t) { return t === 'symbol' ? symbols.slice() : []; },
		querySelector: function (sel) { return null; }
	} : {
		nodeName: 'html',
		getElementsByTagName: function () { return []; },
		querySelector: function (sel) { return sel === 'parsererror' ? {} : null; }
	};
	return { documentElement: root };
};

/**
 * Runs juneau-icons.js in a fresh sandbox for one scenario and resolves to { NS, report, warns }.
 * opts: { replacementUrl, overrideUrl, replacementSvg, overrideSvg, failReplacement, notFoundReplacement, dev, useAttrs }.
 */
async function runScenario(opts) {
	opts = opts || {};
	const env = makeEnv();
	env.document.readyState = opts.readyState || 'loading';

	// SVG-DOM surface juneau-icons.js needs beyond the shared shim.
	// The injected merged sprite is a small stand-in that remembers its symbol children so a scenario can read
	// back which symbol actually won a stem.
	let injectedSprite = null;
	let spriteBuilds = 0;
	env.document.createElementNS = function (ns, tag) {
		if (tag !== 'svg') return env.el(tag);
		const svg = {
			nodeType: 1, tagName: 'SVG', attrs: {}, childNodes: [], parentNode: null,
			setAttribute: function (k, v) { this.attrs[k] = String(v); },
			getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
			appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
			remove: function () { /* the injected sprite is never attached to a real parent; nothing to detach */ },
			querySelector: function (sel) {
				const id = sel.charAt(0) === '#' ? sel.slice(1) : null;
				return this.childNodes.find(function (c) { return c.attrs?.id === id; }) || null;
			}
		};
		injectedSprite = svg;
		spriteBuilds++;
		return svg;
	};
	env.document.documentElement.appendChild = function (c) { return c; };
	const baseGetById = env.document.getElementById;
	env.document.getElementById = function (id) {
		return id === 'juneau-symbol-sprite' ? injectedSprite : baseGetById.call(env.document, id);
	};
	env.document.importNode = function (node) { return node; };

	// The icons <script> tag: src (so spriteUrl() resolves the sibling sprite) plus optional layer attrs.
	const attrs = {};
	if (opts.useAttrs) {
		if (opts.replacementUrl) attrs['data-juneau-icon-replacement'] = opts.replacementUrl;
		if (opts.overrideUrl) attrs['data-juneau-icon-override'] = opts.overrideUrl;
	}
	const iconScript = {
		src: 'https://host/app/juneau-icons.js',
		getAttribute: function (k) { return Object.hasOwn(attrs, k) ? attrs[k] : null; },
		dataset: new Proxy({}, {
			get: (t, prop) => {
				if (typeof prop !== 'string') return undefined;
				const k = 'data-' + prop.replaceAll(/[A-Z]/g, (c) => '-' + c.toLowerCase());
				return Object.hasOwn(attrs, k) ? attrs[k] : undefined;
			}
		})
	};
	env.document.getElementsByTagName = function (t) { return t === 'script' ? [iconScript] : []; };

	env.setFetch(function (url) {
		url = String(url);
		if (url.indexOf('repl') !== -1) {
			if (opts.failReplacement) return Promise.reject(new Error('network blocked'));
			if (opts.notFoundReplacement) return Promise.resolve(jsonResponse('', { status: 404 }));
			return Promise.resolve(jsonResponse(opts.replacementSvg || ''));
		}
		if (url.indexOf('ovr') !== -1) return Promise.resolve(jsonResponse(opts.overrideSvg || ''));
		if (url.indexOf('juneau-symbols-material.svg') !== -1)
			return Promise.resolve(opts.materialGate).then(function () { return jsonResponse(MATERIAL_SVG); });
		if (url.indexOf('juneau-symbols.svg') !== -1) return Promise.resolve(jsonResponse(SHIPPED_SVG));
		return Promise.resolve(jsonResponse('', { status: 404 }));
	});

	if (opts.dev) env.window.JuneauViews = { dev: true };

	const warns = [];
	// deferTimers: a 0ms timer is queued (flushed by the scenario) instead of running inline, so the scenario can
	// act "between" the icons script's evaluation and the timer, as a deferred/module script would.
	const timers = [];
	const sandboxConsole = {
		log: function () { /* no-op */ }, error: function () { /* no-op */ },
		warn: function () { warns.push(Array.prototype.join.call(arguments, ' ')); }
	};
	const sandbox = {
		window: env.window, document: env.document, console: sandboxConsole,
		setTimeout: function (fn) {
			if (typeof fn === 'function') {
				if (opts.deferTimers) timers.push(fn); else fn();
			}
			return 0;
		},
		clearTimeout: function () { /* no-op */ },
		Promise: Promise,
		fetch: function (...args) { return env.callFetch(...args); },
		DOMParser: FakeDOMParser
	};
	// NOSONAR javascript:S1523 -- loading the production juneau-icons.js source into a VM sandbox is this
	// harness's intended mechanism for exercising it under the SVG-sprite shim; the path is a fixed local file.
	vm.runInNewContext(ICONS_JS, sandbox, { filename: 'juneau-icons.js' }); // NOSONAR javascript:S1523 -- harness evaluates the module's own bundled script, a fixed local file

	const NS = env.window.JuneauViews;
	const ctx = {
		NS: NS, warns: warns, env: env,
		spriteBuilds: function () { return spriteBuilds; },
		sprite: function () { return injectedSprite; },
		flushTimers: function () { while (timers.length) timers.shift()(); }
	};
	if (opts.manual)
		return ctx;
	let report;
	if (opts.useAttrs) {
		env.dispatchDocument('DOMContentLoaded');
		report = await NS.icons.loadSymbolSprite();
	} else {
		report = await NS.icons.sprites({ replacementUrl: opts.replacementUrl, overrideUrl: opts.overrideUrl });
	}
	ctx.report = report;
	return ctx;
}

// A stand-in Material pack with a single symbol (the real file is irrelevant to the pack() race scenario).
const MATERIAL_SVG =
	'<svg xmlns="http://www.w3.org/2000/svg" display="none">'
	+ '<symbol id="juneau-sym-copy" viewBox="0 0 24 24"><rect x="0" y="0" width="24" height="24"/></symbol>'
	+ '</svg>';

// A partial replacement (the app's set): only two names, so the rest must fall through to shipped.  It also
// carries a stem the shipped set does NOT have, to prove an app-supplied stem resolves through the layers.
const REPL_SVG =
	'<svg xmlns="http://www.w3.org/2000/svg" display="none">'
	+ '<symbol id="juneau-sym-search" viewBox="0 0 24 24"><rect x="0" y="0" width="24" height="24"/></symbol>'
	+ '<symbol id="juneau-sym-settings" viewBox="0 0 24 24"><rect x="0" y="0" width="24" height="24"/></symbol>'
	+ '<symbol id="juneau-sym-brandnew" viewBox="0 0 24 24"><rect x="0" y="0" width="24" height="24"/></symbol>'
	+ '</svg>';
// The override (a few names on top): one name, which must beat the replacement's copy of it.
const OVR_SVG =
	'<svg xmlns="http://www.w3.org/2000/svg" display="none">'
	+ '<symbol id="juneau-sym-settings" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/></symbol>'
	+ '</svg>';

(async function main() {
	const out = {};

	// Sanity: the API surface is present.
	const probe = await runScenario({});
	out.hasIcons = !!(probe.NS?.icons);
	out.exports = probe.NS?.icons ? Object.keys(probe.NS.icons).sort((a, b) => Number(a > b) - Number(a < b)) : [];

	// Scenario 1 - shipped only (no layers configured).
	{
		const r = probe.report;
		out.s_shipped = {
			layers: r.layers,
			w_search: r.stems.search,
			w_settings: r.stems.settings,
			namesCount: r.names.length,
			hasSort: r.names.indexOf('sort') !== -1,
			hasFirstPage: r.names.indexOf('first_page') !== -1,
			hasLastPage: r.names.indexOf('last_page') !== -1,
			hasChevronleft: r.names.indexOf('chevronleft') !== -1,
			hasSearchStem: r.names.indexOf('search') !== -1,
			stemsCount: probe.NS.icons.stems().length,
			nameToStem: {
				content_copy: probe.NS.icons.nameToStem().content_copy,
				table: probe.NS.icons.nameToStem().table,
				picture_as_pdf: probe.NS.icons.nameToStem().picture_as_pdf,
				manage_search: probe.NS.icons.nameToStem().manage_search
			}
		};
	}

	// Scenario 2 - partial replacement: two names replaced, the rest fall through to shipped.
	{
		const s = await runScenario({ replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG });
		out.s_repl = {
			layers: s.report.layers,
			w_search: s.report.stems.search,
			w_settings: s.report.stems.settings,
			w_close: s.report.stems.close,
			w_brandnew: s.report.stems.brandnew,
			hasClose: s.report.names.indexOf('close') !== -1
		};
	}

	// Scenario 3 - override + replacement both define settings: override wins; search stays replacement.
	{
		const s = await runScenario({
			replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG,
			overrideUrl: '/app/ovr.svg', overrideSvg: OVR_SVG
		});
		out.s_ovr = {
			layers: s.report.layers,
			w_settings: s.report.stems.settings,
			w_search: s.report.stems.search,
			w_close: s.report.stems.close
		};
	}

	// Scenario 4 - replacement fetch fails (blocked): layer skipped, override that loaded still wins, shipped fills.
	{
		const s = await runScenario({
			replacementUrl: '/app/repl.svg', failReplacement: true,
			overrideUrl: '/app/ovr.svg', overrideSvg: OVR_SVG,
			dev: true
		});
		out.s_fail = {
			layers: s.report.layers,
			w_settings: s.report.stems.settings,
			w_search: s.report.stems.search,
			warned: s.warns.some(function (w) { return w.indexOf('replacement sprite failed') !== -1; })
		};
	}

	// Scenario 4b - replacement 404s (not ok): same skip behavior via the non-2xx path.
	{
		const s = await runScenario({ replacementUrl: '/app/repl.svg', notFoundReplacement: true });
		out.s_notfound = { replacement: s.report.layers.replacement, w_search: s.report.stems.search };
	}

	// Scenario 5 - resolve equivalence, name->stem map, unknown id warns, app-supplied stem resolves.
	{
		const s = await runScenario({ replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG, dev: true });
		const icons = s.NS.icons;
		out.s_resolve = {
			searchEqPrefixed: icons.resolveIcon('search') === icons.resolveIcon('juneau-sym-search'),
			searchNotNull: icons.resolveIcon('search') != null,
			unknownNull: icons.resolveIcon('definitely-not-an-icon') === null,
			unknownWarned: s.warns.some(function (w) { return w.indexOf("unknown icon 'definitely-not-an-icon'") !== -1; }),
			newStemResolved: typeof icons.resolveIcon('brandnew') === 'string' && icons.resolveIcon('brandnew').indexOf('#juneau-sym-brandnew') !== -1
		};
	}

	// Scenario 6 - a sprites(...) call after first paint is ignored and warns in dev; the report is unchanged.
	{
		const s = await runScenario({ replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG, dev: true });
		const before = s.report;
		const lateReport = await s.NS.icons.sprites({ replacementUrl: '/app/late.svg' });
		out.s_late = {
			sameReport: lateReport === before,
			w_search_unchanged: lateReport.stems.search,
			warned: s.warns.some(function (w) { return w.indexOf('after the sprite load began') !== -1; })
		};
	}

	// Scenario 7 - the documented path: layer URLs supplied as script-tag attributes, read before first paint.
	{
		const s = await runScenario({ useAttrs: true, replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG });
		out.s_attrs = { replacement: s.report.layers.replacement, w_search: s.report.stems.search };
	}

	// Scenario 8 - sort is an ordinary <use> host of #juneau-sym-sort, resolved BEFORE the sprite has loaded (no
	// inline fallback copy of the art), and the override sprite wins once it is merged in.
	{
		const sortOvr = '<svg xmlns="http://www.w3.org/2000/svg" display="none">'
			+ '<symbol id="juneau-sym-sort" viewBox="0 0 24 24"><path d="M1 1"/></symbol></svg>';
		const s = await runScenario({ manual: true, overrideSvg: sortOvr });
		const pending = s.NS.icons.sprites({ overrideUrl: '/app/ovr.svg' });   // load in flight, promise unresolved
		s.env.document.getElementById = function () { return null; };           // no sprite in the DOM yet
		const before = s.NS.icons.resolveIcon('sort');
		const beforePrefixed = s.NS.icons.resolveIcon('juneau-sym-sort');
		const report = await pending;
		const after = s.NS.icons.resolveIcon('sort');
		const winner = s.sprite().childNodes.find(function (c) { return c.attrs.id === 'juneau-sym-sort'; });
		out.s_sort = {
			useBeforeLoad: before.indexOf('<use href="#juneau-sym-sort"') !== -1,
			noInlineArtBeforeLoad: before.indexOf('<path') === -1 && before.indexOf(' d=') === -1,
			prefixedSame: before === beforePrefixed,
			sameMarkupAfterLoad: before === after,
			winnerLayer: report.stems.sort,
			spriteHoldsOverrideArt: winner?.innerHTML.indexOf('M1 1') !== -1
		};
	}

	// Scenario 9 - an unknown name draws nothing (null) and a non-dev page stays silent.
	{
		const s = await runScenario({});
		const icons = s.NS.icons;
		out.s_unknown_prod = {
			unknownNull: icons.resolveIcon('definitely-not-an-icon') === null,
			silent: s.warns.length === 0
		};
	}

	// Scenario 10 - stems() is the SHIPPED catalog: a replacement adding `brandnew` does not grow it, yet
	// `brandnew` still resolves through the merged layers.
	{
		const shipped = [];
		const re = /<symbol\s+id="juneau-sym-([^"]+)"/g;
		let m;
		for (m = re.exec(SHIPPED_SVG); m; m = re.exec(SHIPPED_SVG)) shipped.push(m[1]);
		const s = await runScenario({ replacementUrl: '/app/repl.svg', replacementSvg: REPL_SVG });
		const stems = s.NS.icons.stems().slice();
		out.s_catalog = {
			stemsMatchShipped: JSON.stringify(stems.toSorted((a, b) => Number(a > b) - Number(a < b))) === JSON.stringify(shipped.toSorted((a, b) => Number(a > b) - Number(a < b))),
			brandnewInStems: stems.indexOf('brandnew') !== -1,
			brandnewResolves: typeof s.NS.icons.resolveIcon('brandnew') === 'string'
		};
	}

	// Scenario 11 - a sprites(...) call from a deferred / module script (readyState "interactive", after the icons
	// script evaluated but before DOMContentLoaded) still lands: the boot load is deferred past it.
	{
		const s = await runScenario({ manual: true, readyState: 'interactive', deferTimers: true, dev: true, replacementSvg: REPL_SVG });
		const started = s.spriteBuilds();
		const pending = s.NS.icons.sprites({ replacementUrl: '/app/repl.svg' });
		s.env.dispatchDocument('DOMContentLoaded');
		s.flushTimers();
		const report = await pending;
		out.s_deferred = {
			bootDidNotPreempt: started === 0,
			replacement: report.layers.replacement,
			w_search: report.stems.search,
			noLateWarn: s.warns.length === 0
		};
	}

	// Scenario 12 - pack() while a load is in flight: the stale load is dropped (no state overwrite, no second
	// injection) and the late-registration gate does not reopen.
	{
		let release;
		const gate = new Promise(function (resolve) { release = resolve; });
		const s = await runScenario({ materialGate: gate, dev: true });
		const built = s.spriteBuilds();                       // the awaited boot load injected once
		const stale = s.NS.icons.pack('material');            // generation 1: blocked on the gated material fetch
		const fresh = s.NS.icons.pack('original');            // generation 2: supersedes it
		const freshReport = await fresh;
		const afterFresh = s.spriteBuilds();
		release();
		const staleReport = await stale;
		const lateReport = await s.NS.icons.sprites({ replacementUrl: '/app/repl.svg' });
		out.s_pack = {
			freshInjectedOnce: afterFresh === built + 1,
			staleNotInjected: s.spriteBuilds() === afterFresh,
			staleResolvesToCurrent: staleReport === freshReport,
			stemsStillShipped: s.NS.icons.stems().length,
			spriteSymbols: s.sprite().childNodes.length,
			gateStaysClosed: lateReport === freshReport && s.warns.some(function (w) { return w.indexOf('after the sprite load began') !== -1; })
		};
	}

	// Scenario 13 - pack("material") layers Material as the REPLACEMENT over the real shipped sprite: a stem only
	// shipped carries (search) still resolves from shipped; a stem both carry (copy) resolves from Material.
	{
		const s = await runScenario({ dev: true });
		const report = await s.NS.icons.pack('material');
		out.s_materialOverShipped = {
			replacementLayer: report.layers.replacement,
			searchWinner: report.stems.search,
			copyWinner: report.stems.copy,
			spriteHasSearch: s.NS.icons.stems().indexOf('search') !== -1,
			noWarn: s.warns.length === 0
		};
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (error) {
	console.error(error?.stack ? error.stack : error);
	process.exit(1);
});
