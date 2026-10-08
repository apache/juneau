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
 * console-output-env.cjs - the console-output harnesses' environment: regions-harness.cjs's load() plus node
 * decorations the shared views shim deliberately does not carry (it is shared by every views harness, and none of
 * them needs geometry).
 *
 *   - document.createDocumentFragment(); appendChild/insertBefore splice a fragment's children in order.
 *   - children, previousSibling, hasAttribute on every element.
 *   - Scroll geometry: each visible element child is ROW_H px tall (hidden === true or style.display === 'none'
 *     takes none); scrollHeight sums them; clientHeight is a plain settable number (default 0); scrollTop clamps to
 *     [0, scrollHeight - clientHeight]; offsetTop is the sum of the visible preceding siblings.  A programmatic
 *     scrollTop write fires NO event (browsers fire scroll asynchronously); userScroll() writes AND fires,
 *     fireScroll() fires without writing.
 *   - scrollIntoView(opts) records {n: data-n, block} in t.scrolled and centres the node in its parent.
 *   - window: sessionStorage, location (hash only), add/removeEventListener + dispatchWindow().
 *   - document: hidden + visibilitychange, a working removeEventListener.
 *   - Date: a fake whose now() is BASE_MS + clock.now(), so status-header arithmetic follows the fake clock.
 *   - JuneauViews.icons: a stub registry that knows 'check' and 'cancel', so helpers.icon() returns a visible span
 *     for those and a hidden one for anything else.
 *
 * load(argv) takes the harness argv: renders, views, regions, helpers, console-output, vectors.json.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const ROW_H = 20;
const BASE_MS = Date.parse('2026-10-07T12:00:00.000Z');

function isVisible(n) {
	return n.nodeType === 1 && n.hidden !== true && n.style?.display !== 'none';
}

function decorateNode(n, t) {
	if (n.__coDecorated) return n;
	n.__coDecorated = true;
	const nativeAppend = n.appendChild;
	const nativeInsert = n.insertBefore;
	n.appendChild = function (c) {
		if (c && c.nodeType === 11) {
			for (const k of c.childNodes.slice()) nativeAppend.call(this, k);
			return c;
		}
		return nativeAppend.call(this, c);
	};
	n.insertBefore = function (c, ref) {
		if (c && c.nodeType === 11) {
			for (const k of c.childNodes.slice()) nativeInsert.call(this, k, ref);
			return c;
		}
		return nativeInsert.call(this, c, ref);
	};
	n.hasAttribute = function (k) { return k === 'class' ? !!this.className : Object.hasOwn(this.attrs, k); };
	let clientHeight = 0;
	let scrollTop = 0;
	Object.defineProperties(n, {
		children: { get: function () { return this.childNodes.filter(function (c) { return c.nodeType === 1; }); } },
		previousSibling: { get: function () {
			const sibs = this.parentNode?.childNodes;
			if (!sibs) return null;
			return sibs[sibs.indexOf(this) - 1] || null;
		} },
		clientHeight: { get: function () { return clientHeight; }, set: function (v) { clientHeight = Number(v) || 0; } },
		scrollHeight: { get: function () { return this.childNodes.filter(isVisible).length * ROW_H; } },
		scrollTop: {
			get: function () { return scrollTop; },
			set: function (v) {
				const max = Math.max(0, this.scrollHeight - clientHeight);
				scrollTop = Math.min(max, Math.max(0, Number(v) || 0));
			}
		},
		offsetTop: { get: function () {
			const sibs = this.parentNode?.childNodes || [];
			let y = 0;
			for (const s of sibs) { if (s === this) break; if (isVisible(s)) y += ROW_H; }
			return y;
		} }
	});
	n.scrollIntoView = function (opts) {
		t.scrolled.push({ n: this.getAttribute('data-n'), block: opts?.block || 'start', behavior: opts?.behavior || 'auto' });
		const p = this.parentNode;
		if (p) p.scrollTop = this.offsetTop - p.clientHeight / 2 + ROW_H / 2;
	};
	return n;
}

function makeDate(clock) {
	const RealDate = Date;
	function FakeDate(...a) {
		if (!new.target) return new RealDate(BASE_MS + clock.now()).toString();
		return a.length ? new RealDate(...a) : new RealDate(BASE_MS + clock.now());
	}
	FakeDate.now = function () { return BASE_MS + clock.now(); };
	FakeDate.parse = RealDate.parse;
	FakeDate.UTC = RealDate.UTC;
	FakeDate.prototype = RealDate.prototype;
	return FakeDate;
}

function makeStorage() {
	const m = {};
	return {
		getItem: function (k) { return Object.hasOwn(m, k) ? m[k] : null; },
		setItem: function (k, v) { m[k] = String(v); },
		removeItem: function (k) { delete m[k]; },
		_dump: function () { return { ...m }; }
	};
}

function load(argv, opts) {
	opts = opts || {};
	const [renders, views, regions, helpers, consoleOutput, vectorsPath] = argv;
	const t = { scrolled: [] };
	const winListeners = {};
	const docListeners = {};
	const loaded = H.load(renders, views, regions, {
		// Registry order: regions, console-output, helpers (helpers.icon is resolved at call time).
		extraJsPaths: opts.withoutModule ? [helpers] : [consoleOutput, helpers],
		decorate: function (env, sandbox, clock) {
			const doc = env.document;
			const nativeCreate = doc.createElement;
			doc.createElement = function (tag) { return decorateNode(nativeCreate.call(doc, tag), t); };
			const nativeEl = env.el;
			env.el = function (tag) { return decorateNode(nativeEl(tag), t); };
			decorateNode(env.body, t);
			doc.createDocumentFragment = function () {
				const f = decorateNode(nativeCreate.call(doc, '#document-fragment'), t);
				f.nodeType = 11;
				return f;
			};
			doc.hidden = false;
			doc.addEventListener = function (type, fn) { (docListeners[type] = docListeners[type] || []).push(fn); };
			doc.removeEventListener = function (type, fn) {
				const l = docListeners[type] || [];
				const i = l.indexOf(fn);
				if (i >= 0) l.splice(i, 1);
			};
			const w = env.window;
			w.sessionStorage = makeStorage();
			w.location = { hash: '', pathname: '/x', search: '', href: 'http://localhost/x', origin: 'http://localhost' };
			w.addEventListener = function (type, fn) { (winListeners[type] = winListeners[type] || []).push(fn); };
			w.removeEventListener = function (type, fn) {
				const l = winListeners[type] || [];
				const i = l.indexOf(fn);
				if (i >= 0) l.splice(i, 1);
			};
			w.JuneauViews = w.JuneauViews || {};
			sandbox.Date = makeDate(clock);
			sandbox.sessionStorage = w.sessionStorage;
			sandbox.location = w.location;
		}
	});
	Object.assign(t, loaded);
	// juneau-icons.js is not loaded here, so install a stub registry (helpers.icon reads it on every call).
	t.NS.icons = {
		resolveIcon: function (name) { return name === 'check' || name === 'cancel' ? '<svg data-icon="' + name + '"></svg>' : null; }
	};
	t.CO = t.NS.consoleOutput;
	t.vectors = vectorsPath ? JSON.parse(fs.readFileSync(vectorsPath, 'utf8')) : null;
	t.dispatchWindow = function (type, ev) { for (const fn of (winListeners[type] || []).slice()) fn(ev || {}); };
	t.dispatchDocument = function (type, ev) { for (const fn of (docListeners[type] || []).slice()) fn(ev || {}); };
	t.setHash = function (h) { t.env.window.location.hash = h; t.dispatchWindow('hashchange', {}); };
	t.setHidden = function (v) { t.env.document.hidden = !!v; t.dispatchDocument('visibilitychange', {}); };
	t.listenerCount = function (type) { return (winListeners[type] || []).length + (docListeners[type] || []).length; };
	return t;
}

function userScroll(node, top) {
	node.scrollTop = top;
	node.dispatch('scroll', { target: node });
}

function fireScroll(node) {
	node.dispatch('scroll', { target: node });
}

/** Every descendant element of `root` carrying class `cls` (single class, no selector syntax). */
function byClass(root, cls) {
	return root.querySelectorAll('.' + cls);
}

module.exports = {
	ROW_H: ROW_H,
	BASE_MS: BASE_MS,
	load: load,
	userScroll: userScroll,
	fireScroll: fireScroll,
	byClass: byClass,
	flush: H.flush,
	abortableFetch: H.abortableFetch,
	mkRegion: H.mkRegion,
	jsonResponse: H.jsonResponse
};
