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
 * console-shell.cjs - opt-in Chromium prober for the console shell (juneau-console.js).
 *
 * Never runs in a default build.  Driven by the Console*_BrowserTest classes under `mvn -Pjs-tests`.
 *
 *   Usage:  node console-shell.cjs <cases.json>
 *
 * cases.json is {assets: {path: {type, body}}, sequences: {path: [{type, body}]},
 * cases: [{name, url, pages: {path: {type, body}}, queries, computed}]}.
 * Every request to http://juneau.test/ is answered from a sequence (the n-th request to a sequenced path in a case
 * gets its n-th response, then 404), then from a case's pages, then from the shared assets, else 404.  Each sequenced
 * request is recorded in the case's report as requests: [{path, search, lastEventId}].
 * Optional per-case options:
 *   firstFrame: true   records the root data-jc-scheme attribute and the body background in the first animation frame
 *                      (which runs before the first paint) into the report's firstFrame entry.
 *   contrast: [{name, kind, selector}]   measures WCAG contrast after every click and step (see CONTRAST below) into
 *                      the report's contrast entry, keyed by name.
 *   watchRequests: true   logs every request as {path, phase} into the report's requestLog entry: phase is "load"
 *                      until the page has loaded, any wait has matched and the network is quiet, then "steps"; after
 *                      the steps the run waits for the network to go quiet again, so an in-flight request is logged.
 * Prints one report object per case name.
 */
'use strict';

const fs = require('node:fs');
const { chromium } = require('playwright');

const ORIGIN = /^http:\/\/juneau\.test\//;

// Must equal the baseline probe (nav-computed-baseline.json).
const SELECTORS = ['nav.juneau-page-nav', '.juneau-page-nav-sections', '.juneau-page-nav-children',
	'.juneau-page-nav-section[aria-current]', '.juneau-page-nav-section:not([aria-current])',
	'.juneau-page-nav-child[aria-current]', '.juneau-page-nav-child:not([aria-current])'];
const PROPS = ['display', 'flex-direction', 'gap', 'padding-top', 'padding-right', 'padding-bottom', 'padding-left',
	'margin-top', 'margin-bottom', 'border-bottom-width', 'border-bottom-style', 'border-bottom-color',
	'color', 'background-color', 'font-size', 'font-weight', 'line-height', 'text-decoration-line', 'position', 'top'];

const PROBE = function (arg) {
	const out = {};
	const banner = document.querySelector('.jc-console-error');
	out.banner = banner
		? Array.prototype.map.call(banner.querySelectorAll('.jc-console-error-item'),
			function (i) { return { code: i.dataset.juneauError ?? null, text: i.textContent }; })
		: [];
	out.current = Array.prototype.map.call(document.querySelectorAll('nav.juneau-page-nav [aria-current="page"]'),
		function (a) { return a.dataset.juneauNavId || a.getAttribute('href'); });
	const navEl = document.querySelector('nav.juneau-page-nav');
	out.navLabel = navEl ? navEl.getAttribute('aria-label') : null;
	out.childRows = document.querySelectorAll('nav.juneau-page-nav .juneau-page-nav-children').length;
	out.probe = window.__probe === undefined ? null : JSON.parse(JSON.stringify(window.__probe)); // NOSONAR javascript:S7784 -- deliberate JSON round-trip: drops non-serialisable members (functions/undefined) that structuredClone would throw on
	out.mounted = window.__mounted || null;
	out.queries = {};
	for (const k of Object.keys(arg.queries)) {
		const el = document.querySelector(arg.queries[k]);
		out.queries[k] = el ? el.textContent : null;
	}
	if (arg.computed) {
		out.computed = {};
		for (const s of arg.selectors) {
			const el = document.querySelector(s);
			if (!el) {
				out.computed[s] = null;
				continue;
			}
			const cs = getComputedStyle(el);
			out.computed[s] = {};
			for (const p of arg.props) out.computed[s][p] = cs.getPropertyValue(p);
		}
	}
	return out;
};

/*
 * Contrast probe.  For each entry, every element matching the selector is measured and the worst one is reported.
 * kind: "text" (the color), "placeholder" (the ::placeholder color), "border" (the first painted border side's color)
 * "outline" (the element is focused first, in keyboard modality, then its outline color is read) or "focus-border"
 * (focused the same way, then the border is read, for a control whose focus indicator is its border).
 * Only rendered elements (a non-empty box, not visibility:hidden) are measured; hidden matches are counted.
 * The background is found by walking up from the element (from its parent for "outline", which paints outside it),
 * alpha-compositing each background-color until one is opaque.  An ancestor whose background-image is a gradient
 * contributes both of its end stops, and the worse result counts.  The foreground alpha, multiplied by the element's
 * effective opacity, is composited over that background.  ratio is the WCAG 2 contrast ratio, rounded to 2 places.
 * Each result is {count, hidden, worst: {fg, bg, ratio, fgRaw, text}}, without worst when nothing rendered matched
 * or every rendered match was an error.  ratio is null when the element paints no such colour (for example no outline).
 * It fails closed: a background colour, gradient stop or foreground colour the probe cannot parse (for example a
 * color() or lab() value) is never treated as transparent or as no colour; the element is skipped and the result
 * carries errors: ["<text>: unparseable background|foreground <value>"].
 */
const CONTRAST = function (entries) { // NOSONAR javascript:S3776 - branching mirrors the behavior being implemented; splitting it would obscure the flow
	const parse = function (s) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		const m = /rgba?\(\s*([\d.]+)[,\s]+([\d.]+)[,\s]+([\d.]+)(?:\s*[,/]\s*([\d.]+%?))?\s*\)/.exec(s || ''); // NOSONAR javascript:S5843 - one colour-function pattern; splitting it would obscure the grammar
		if (!m) return null;
		let a = 1;
		if (m[4] !== undefined) a = m[4].endsWith('%') ? Number.parseFloat(m[4]) / 100 : Number.parseFloat(m[4]);
		return [Number(m[1]), Number(m[2]), Number(m[3]), a];
	};
	// Thrown for a colour the probe cannot measure, so the element is reported rather than measured wrongly.
	const unparseable = function (what, value) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		return { unparseable: 'unparseable ' + what + ' ' + value };
	};
	const NON_RGB = /\b(?:color|lab|lch|oklab|oklch|hwb|hsla?|color-mix)\(|#[0-9a-f]{3,8}\b/i;
	const stops = function (img) {
		if (!img || img.indexOf('gradient(') < 0) return null;
		const all = img.match(/rgba?\([^)]*\)/g) || [];
		if (all.length < 2 || NON_RGB.test(img)) throw unparseable('background', img);
		return [parse(all[0]), parse(all.at(-1))];
	};
	const over = function (top, under) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		const a = top[3] + under[3] * (1 - top[3]);
		if (a === 0) return [0, 0, 0, 0];
		return [0, 1, 2].map(function (i) { return (top[i] * top[3] + under[i] * under[3] * (1 - top[3])) / a; }).concat([a]);
	};
	// The candidate effective backgrounds behind el: one per gradient end stop combination (usually exactly one).
	const backgrounds = function (el) {
		const layers = [];
		for (let n = el; n?.nodeType === 1; n = n.parentElement) {
			const cs = getComputedStyle(n);
			const g = stops(cs.backgroundImage);
			const c = parse(cs.backgroundColor);
			if (!c) throw unparseable('background', cs.backgroundColor);
			const opts = g ? g.map(function (s) { return over(s, c); }) : [c];
			layers.push(opts);
			if (opts.every(function (o) { return o[3] >= 1; })) break;
		}
		let res = [[255, 255, 255, 1]];
		for (let i = layers.length - 1; i >= 0; i--) {
			const next = [];
			for (const base of res) for (const o of layers[i]) next.push(over(o, base));
			res = next;
		}
		return res;
	};
	const lum = function (c) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		const f = function (v) { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; // NOSONAR javascript:S7721 -- must stay nested: the enclosing function is serialized into the browser
		return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]);
	};
	const ratio = function (a, b) {
		const x = lum(a), y = lum(b);
		return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
	};
	const hex = function (c) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		return '#' + c.slice(0, 3).map(function (v) { return Math.round(v).toString(16).padStart(2, '0'); }).join('');
	};
	const opacity = function (el) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		let o = 1;
		for (let n = el; n?.nodeType === 1; n = n.parentElement) o *= Number.parseFloat(getComputedStyle(n).opacity);
		return o;
	};
	const foreground = function (el, kind) { // NOSONAR javascript:S7721 - must stay nested: the enclosing probe is serialized into the browser
		if (kind === 'placeholder') return getComputedStyle(el, '::placeholder').color;
		if (kind === 'outline') {
			el.focus();
			const cs = getComputedStyle(el);
			return cs.outlineStyle === 'none' ? null : cs.outlineColor;
		}
		if (kind === 'border' || kind === 'focus-border') {
			if (kind === 'focus-border') el.focus();
			const cs = getComputedStyle(el);
			for (const side of ['Top', 'Right', 'Bottom', 'Left'])
				if (cs['border' + side + 'Style'] !== 'none' && Number.parseFloat(cs['border' + side + 'Width']) > 0)
					return cs['border' + side + 'Color'];
			return null;
		}
		return getComputedStyle(el).color;
	};
	const out = {};
	for (const e of entries) {
		const els = Array.prototype.slice.call(document.querySelectorAll(e.selector));
		let worst = null, hidden = 0;
		const errors = [];
		for (const el of els) {
			const r = el.getBoundingClientRect();
			if (!(r.width > 0 && r.height > 0 && getComputedStyle(el).visibility !== 'hidden')) {
				hidden++;
				continue;
			}
			const raw = foreground(el, e.kind);
			const f = parse(raw);
			const item = { fgRaw: raw, text: (el.textContent || el.getAttribute('placeholder') || '').trim().slice(0, 40) };
			if (!f && raw !== null) {
				errors.push(item.text + ': unparseable foreground ' + raw);
				continue;
			}
			if (!f) {
				item.ratio = null;
				if (!worst) worst = item;
				continue;
			}
			f[3] *= opacity(el);
			const start = e.kind === 'outline' ? el.parentElement : el;
			let bgs;
			try {
				bgs = backgrounds(start);
			} catch (error_) {
				if (!error_?.unparseable) throw error_;
				errors.push(item.text + ': ' + error_.unparseable);
				continue;
			}
			for (const bg of bgs) {
				const fg = over(f, bg);
				const rt = Math.round(ratio(fg, bg) * 100) / 100;
				if (!worst || worst.ratio === null || rt < worst.ratio)
					worst = { ...item, fg: hex(fg), bg: hex(bg), ratio: rt };
			}
		}
		out[e.name] = { count: els.length - hidden, hidden: hidden };
		if (worst) out[e.name].worst = worst;
		if (errors.length) out[e.name].errors = errors;
	}
	return out;
};

(async () => { // NOSONAR javascript:S3776 - branching mirrors the behavior being implemented; splitting it would obscure the flow
	const [casesFile] = process.argv.slice(2);
	if (!casesFile) {
		process.stderr.write('usage: node console-shell.cjs <cases.json>\n');
		process.exit(2);
	}
	if (!fs.existsSync(casesFile))
		throw new Error('cases file not found: ' + casesFile);
	const spec = JSON.parse(fs.readFileSync(casesFile, 'utf8'));

	const browser = await chromium.launch();
	const report = {};
	try {
		for (const c of spec.cases) {
			const context = await browser.newContext({ viewport: { width: 1280, height: 800 }, ...(c.colorScheme ? { colorScheme: c.colorScheme } : {}) });
			if (c.storage)
				await context.addInitScript(seed => {
					try { for (const k of Object.keys(seed)) localStorage.setItem(k, seed[k]); } catch (e) { /* opaque origin */ } // NOSONAR javascript:S2486 - the failure is an expected outcome here and the fallback handles it
				}, c.storage);
			if (c.firstFrame)
				await context.addInitScript(() => {
					requestAnimationFrame(() => {
						const b = document.body;
						window.__firstFrame = {
							scheme: document.documentElement.dataset.jcScheme ?? null,
							bodyBg: b ? getComputedStyle(b).backgroundColor : null,
							hasBody: !!b
						};
					});
				});
			await context.addInitScript(() => {
				document.addEventListener('juneau:console-mounted', e => {
					window.__mounted = {
						activeNav: e.detail.activeNav,
						activeNavSource: e.detail.activeNavSource,
						cards: Object.keys(e.detail.cards)
					};
				});
			});
			const requests = [];
			const served = {};
			const log = [];
			let phase = 'load', inFlight = 0;
			await context.route(ORIGIN, async route => {
				const u = new URL(route.request().url());
				const p = u.pathname;
				log.push({ path: p, phase: phase });
				inFlight++;
				try {
					return await answer(route, u, p);
				} finally {
					inFlight--;
				}
			});
			// Resolves once no request has started or been in flight for 300ms.
			const quiet = async () => {
				for (;;) {
					const n = log.length;
					await new Promise(r => setTimeout(r, 300));
					if (n === log.length && inFlight === 0) return;
				}
			};
			const answer = async (route, u, p) => {
				const seq = spec.sequences?.[p];
				if (seq) {
					requests.push({ path: p, search: u.search, lastEventId: route.request().headers()['last-event-id'] ?? null });
					const n = served[p] = (served[p] || 0) + 1;
					const s = seq[n - 1];
					return s
						? route.fulfill({ status: 200, contentType: s.type, body: s.body })
						: route.fulfill({ status: 404, contentType: 'text/plain', body: 'sequence used up: ' + p });
				}
				const f = (c.pages?.[p]) || spec.assets[p];
				if (f?.delayMs)
					await new Promise(r => setTimeout(r, f.delayMs));
				return f
					? route.fulfill({ status: 200, contentType: f.type, body: f.body })
					: route.fulfill({ status: 404, contentType: 'text/plain', body: 'not found: ' + p });
			};
			const page = await context.newPage();
			const consoleErrors = [];
			const pageErrors = [];
			page.on('pageerror', e => pageErrors.push(String(e?.message || e)));
			page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });
			await page.goto(c.url);
			if (c.waitFor)
				await page.waitForSelector(c.waitFor, { state: 'attached', timeout: 10000 });
			if (c.watchRequests) {
				await quiet();
				phase = 'steps';
			}
			for (const sel of c.clicks || [])
				await page.click(sel, { timeout: 10000 });
			for (const st of c.steps || []) {
				if (st.kind === 'press') await page.keyboard.press(st.value);
				else if (st.kind === 'emulate') {
					await page.emulateMedia({ colorScheme: st.value });
					await page.evaluate(() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r))));
				}
				else if (st.kind === 'forcedColors') {
					await page.emulateMedia({ forcedColors: st.value });
					await page.evaluate(() => new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r))));
				}
				else if (st.kind === 'eval') await page.evaluate(st.value);
			}
			await page.evaluate(() => new Promise(requestAnimationFrame));
			if (c.watchRequests) await quiet();
			// A real key press puts the page in keyboard modality, so a programmatic focus() matches :focus-visible.
			if (c.contrast?.some(e => e.kind === 'outline' || e.kind === 'focus-border')) await page.keyboard.press('Shift');
			const contrast = c.contrast ? await page.evaluate(CONTRAST, c.contrast) : undefined;
			const r = await page.evaluate(PROBE, { queries: c.queries || {}, computed: !!c.computed, selectors: SELECTORS, props: PROPS });
			if (c.firstFrame) r.firstFrame = await page.evaluate(() => window.__firstFrame || null);
			if (contrast) r.contrast = contrast;
			r.consoleErrors = consoleErrors.slice();
			r.pageErrors = pageErrors.slice();
			r.requests = requests.slice();
			if (c.watchRequests) r.requestLog = log.slice();
			report[c.name] = r;
			await context.close();
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report, null, 2) + '\n');
})().catch(error => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
