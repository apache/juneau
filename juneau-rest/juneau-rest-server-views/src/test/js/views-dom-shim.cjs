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
 * views-dom-shim.cjs - a small, dependency-free DOM shim shared by the always-on Node behavioral harnesses for
 * TODO-445h (dialog forms, inline validation, the shared popup layer stack).  Rich enough for the code paths under
 * test: element/document event listeners with dispatch, focus/activeElement tracking, a class/attr/tag
 * querySelector(All), closest(), style objects, and a getComputedStyle that returns nothing (so the runtime's
 * --jc-* token reads fall back to their baked-in numeric defaults).
 *
 *   const { makeEnv, loadViews } = require('./views-dom-shim.cjs');
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

/** Parses ONE simple selector token ("tag", ".cls", "[attr]", "[attr=\"v\"]", with optional :not([disabled])). */
function parseSimple(sel) {
	sel = sel.trim();
	const notDisabled = /:not\(\[disabled\]\)/.test(sel);
	sel = sel.replace(/:not\(\[disabled\]\)/g, '');
	let tag = null, cls = null, attr = null, attrVal = null;
	const tagM = /^([a-zA-Z][\w-]*)/.exec(sel);
	if (tagM) { tag = tagM[1].toUpperCase(); sel = sel.slice(tagM[0].length); }
	const clsM = /\.([\w-]+)/.exec(sel);
	if (clsM) cls = clsM[1];
	const attrM = /\[([\w-]+)(?:=["']?([^\]"']*)["']?)?\]/.exec(sel);
	if (attrM) { attr = attrM[1]; attrVal = attrM[2] != null ? attrM[2] : undefined; }
	return { tag, cls, attr, attrVal, notDisabled };
}

/** Splits a selector list on commas and returns an array of matcher functions. */
function compileSelector(selector) {
	return String(selector).split(',').map(function (part) {
		const s = parseSimple(part);
		return function (n) {
			if (!n || n.nodeType !== 1) return false;
			if (s.tag && n.tagName !== s.tag) return false;
			if (s.cls && !((' ' + (n.className || '') + ' ').indexOf(' ' + s.cls + ' ') >= 0)) return false;
			if (s.attr) {
				const has = n.attrs && Object.hasOwn(n.attrs, s.attr);
				if (!has) return false;
				if (s.attrVal !== undefined && String(n.attrs[s.attr]) !== s.attrVal) return false;
			}
			if (s.notDisabled && n.disabled) return false;
			return true;
		};
	});
}

function matchesAny(matchers, n) {
	for (let i = 0; i < matchers.length; i++) if (matchers[i](n)) return true;
	return false;
}

function makeEnv() {
	const listeners = { keydown: [], pointerdown: [], click: [] };
	const byId = {};
	let activeElement = null;

	function el(tag) {
		const node = {
			nodeType: 1,
			tagName: String(tag).toUpperCase(),
			childNodes: [],
			attrs: {},
			parentNode: null,
			className: '',
			style: {},
			disabled: false,
			checked: false,
			required: false,
			_value: '',
			_type: null,
			_listeners: {},
			_text: '',
			get value() { return this._value; },
			set value(v) { this._value = v == null ? '' : String(v); },
			get type() { return this._type != null ? this._type : (this.tagName === 'TEXTAREA' ? 'textarea' : 'text'); },
			set type(v) { this._type = v; },
			get id() { return this.attrs.id || ''; },
			set id(v) { this.setAttribute('id', v); },
			get name() { return this.attrs.name || ''; },
			set name(v) { this.setAttribute('name', v); },
			get firstChild() { return this.childNodes[0] || null; },
			getAttribute: function (k) {
				if (k === 'class') return this.className || null;
				return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null;
			},
			setAttribute: function (k, v) {
				this.attrs[k] = v == null ? '' : String(v);
				if (k === 'class') this.className = this.attrs[k];
				if (k === 'id') byId[this.attrs[k]] = this;
			},
			removeAttribute: function (k) { delete this.attrs[k]; if (k === 'class') this.className = ''; },
			appendChild: function (c) {
				if (c.parentNode) c.parentNode.removeChild(c);
				this.childNodes.push(c); c.parentNode = this;
				if (c.attrs && c.attrs.id) byId[c.attrs.id] = c;
				return c;
			},
			removeChild: function (c) {
				const i = this.childNodes.indexOf(c);
				if (i >= 0) { this.childNodes.splice(i, 1); c.parentNode = null; }
				return c;
			},
			insertBefore: function (c, ref) {
				if (c.parentNode) c.parentNode.removeChild(c);
				const i = this.childNodes.indexOf(ref);
				if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
				c.parentNode = this;
				return c;
			},
			replaceChildren: function () {
				this.childNodes.forEach(function (c) { c.parentNode = null; });
				this.childNodes.length = 0;
			},
			contains: function (n) {
				if (n === this) return true;
				for (let i = 0; i < this.childNodes.length; i++) {
					const c = this.childNodes[i];
					if (c === n) return true;
					if (c.contains && c.contains(n)) return true;
				}
				return false;
			},
			get lastElementChild() {
				for (let i = this.childNodes.length - 1; i >= 0; i--)
					if (this.childNodes[i].nodeType === 1) return this.childNodes[i];
				return null;
			},
			querySelectorAll: function (selector) {
				const matchers = compileSelector(selector);
				const out = [];
				(function walk(n) {
					for (let i = 0; i < n.childNodes.length; i++) {
						const c = n.childNodes[i];
						if (c.nodeType !== 1) continue;
						if (matchesAny(matchers, c)) out.push(c);
						walk(c);
					}
				})(this);
				return out;
			},
			querySelector: function (selector) {
				const all = this.querySelectorAll(selector);
				return all.length ? all[0] : null;
			},
			closest: function (selector) {
				const matchers = compileSelector(selector);
				let n = this;
				while (n && n.nodeType === 1) {
					if (matchesAny(matchers, n)) return n;
					n = n.parentNode;
				}
				return null;
			},
			addEventListener: function (type, fn) {
				(this._listeners[type] = this._listeners[type] || []).push(fn);
			},
			removeEventListener: function (type, fn) {
				const l = this._listeners[type];
				if (!l) return;
				const i = l.indexOf(fn);
				if (i >= 0) l.splice(i, 1);
			},
			dispatch: function (type, ev) {
				ev = ev || {};
				ev.target = ev.target || this;
				const l = this._listeners[type] || [];
				l.slice().forEach(function (fn) { fn(ev); });
			},
			focus: function () { activeElement = this; },
			getBoundingClientRect: function () { return { left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 }; },
			get offsetWidth() { return 0; },
			get offsetHeight() { return 0; },
			set textContent(v) {
				this.childNodes.forEach(function (c) { c.parentNode = null; });
				this.childNodes.length = 0;
				this._text = v == null ? '' : String(v);
			},
			get textContent() {
				if (this.childNodes.length === 0) return this._text || '';
				return this.childNodes.map(function (c) { return c.textContent; }).join('');
			}
		};
		return node;
	}

	const body = el('body');
	const documentElement = el('html');

	const document = {
		readyState: 'complete',
		documentElement: documentElement,
		body: body,
		addEventListener: function (type, fn) { (listeners[type] = listeners[type] || []).push(fn); },
		removeEventListener: function () {},
		getElementById: function (id) { return byId[id] || null; },
		createElement: function (tag) { return el(tag); },
		querySelector: function (selector) { return body.querySelector(selector); },
		querySelectorAll: function (selector) { return body.querySelectorAll(selector); },
		contains: function (n) { return body.contains(n) || n === body; },
		get activeElement() { return activeElement; }
	};

	function dispatchDocument(type, ev) {
		ev = ev || {};
		ev.preventDefault = ev.preventDefault || function () { ev.defaultPrevented = true; };
		(listeners[type] || []).slice().forEach(function (fn) { fn(ev); });
		return ev;
	}

	const window = {
		document: document,
		console: console,
		jQuery: undefined,
		innerWidth: 1024,
		innerHeight: 768,
		getComputedStyle: function () { return { getPropertyValue: function () { return ''; } }; },
		addEventListener: function () {},
		matchMedia: function () { return { matches: false, addEventListener: function () {} }; }
	};

	// A test-controllable fetch: the harness installs an impl via setFetch(); the default rejects (no network).
	let fetchImpl = function () { return Promise.reject(new Error('no fetch impl installed')); };

	return {
		el: el, document: document, window: window, body: body, byId: byId,
		dispatchDocument: dispatchDocument,
		getActive: function () { return activeElement; },
		setActive: function (n) { activeElement = n; },
		setFetch: function (fn) { fetchImpl = fn; },
		callFetch: function () { return fetchImpl.apply(null, arguments); }
	};
}

/** Builds a fetch Response-like object for the harnesses: ok/status/headers.get + a text() that resolves `body`. */
function jsonResponse(body, opts) {
	opts = opts || {};
	const status = opts.status != null ? opts.status : 200;
	const headers = opts.headers || {};
	return {
		ok: status >= 200 && status < 300,
		status: status,
		headers: { get: function (k) { return Object.hasOwn(headers, k) ? headers[k] : null; } },
		text: function () { return Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)); }
	};
}

/** Loads juneau-renders.js then juneau-views.js into a fresh env, returns { env, NS, I }. */
function loadViews(rendersJsPath, viewsJsPath, env) {
	env = env || makeEnv();
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function (fn) { if (typeof fn === 'function') fn(); return 0; },
		clearTimeout: function () {},
		setInterval: function () { return 0; },
		clearInterval: function () {},
		Promise: Promise,
		fetch: function () { return env.callFetch.apply(env, arguments); }
	};
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
	vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });
	const NS = env.window.JuneauViews;
	return { env: env, NS: NS, I: NS && NS.init };
}

module.exports = { makeEnv: makeEnv, loadViews: loadViews, compileSelector: compileSelector, jsonResponse: jsonResponse };
