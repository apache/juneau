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
 * console-dom-shim.cjs - extends views-dom-shim.cjs's makeEnv() with the handful of DOM primitives
 * juneau-console.js needs that no other harness exercises: a settable window.location (prefix-fallback matching),
 * window.CustomEvent + document.dispatchEvent (the "juneau:console-mounted" handshake), document.importNode +
 * document.createDocumentFragment (<template> cloning, including a BARE html card host), a listener-count
 * tracker for the keydown-listener-leak regression (WORK-J0559 Task 5 addendum), and a real `<template>.innerHTML`
 * setter (entity decode + nested-element parse) so renderFooter's trusted-HTML footer.text (WORK-J0559 P24
 * revised) actually parses under this harness instead of just storing a string.
 *
 * This file never modifies views-dom-shim.cjs - it only wraps the env that shim returns.
 *
 *   const { makeConsoleEnv } = require('./console-dom-shim.cjs');
 */
'use strict';

const shim = require('./views-dom-shim.cjs');

/**
 * Deep-clones a views-dom-shim node for `document.importNode`.  A `<template>.content` node (see
 * views-dom-shim.cjs's `el()`) is a regular element shaped like a `TEMPLATE-CONTENT` tag, not a real
 * DocumentFragment (nodeType 11) - importing it must yield a real fragment (its children flattened on append),
 * never a wrapper element literally named `template-content`.
 */
function cloneDeep(doc, node, deep) {
	if (node.nodeType === 3) return doc.createTextNode(node.textContent);
	if (node.nodeType === 11 || node.tagName === 'TEMPLATE-CONTENT') {
		const frag = doc.createDocumentFragment();
		if (deep) for (const c of (node.childNodes || [])) frag.appendChild(cloneDeep(doc, c, true));
		return frag;
	}
	const out = doc.createElement(node.tagName.toLowerCase());
	for (const k of Object.keys(node.attrs || {})) out.setAttribute(k, node.attrs[k]);
	if (deep) {
		// views-dom-shim.cjs's textContent setter (used by test fixtures such as `e.textContent = 'segment'`)
		// stores the string directly on `_text` rather than creating a real (nodeType 3) child text node; mirror
		// that shortcut here so a leaf element's text survives the clone even though childNodes stays empty.
		if ((node.childNodes || []).length === 0 && node._text) out.textContent = node._text;
		else for (const c of (node.childNodes || [])) out.appendChild(cloneDeep(doc, c, true));
	}
	return out;
}

/** Named-entity table for `decodeEntities` - just the handful real adopter footer/card markup is likely to use. */
const NAMED_ENTITIES = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: ' ', mdash: '—', ndash: '–', hellip: '…', copy: '©' };

/** Decodes `&name;`/`&#NN;`/`&#xHH;` references - the entity-decoding half of a real `<template>.innerHTML` parse. */
function decodeEntities(s) {
	return String(s).replaceAll(/&(#x?[0-9a-fA-F]+|[a-zA-Z]+);/g, function (m, body) {
		if (body[0] === '#') {
			const code = (body[1] === 'x' || body[1] === 'X') ? Number.parseInt(body.slice(2), 16) : Number.parseInt(body.slice(1), 10);
			return Number.isNaN(code) ? m : String.fromCodePoint(code);
		}
		return Object.hasOwn(NAMED_ENTITIES, body) ? NAMED_ENTITIES[body] : m;
	});
}

/**
 * Minimal well-formed-markup HTML-fragment parser backing the `innerHTML` setter `addTemplateInnerHtml` installs
 * on `<template>` elements - the one DOM primitive renderFooter's footer.text and htmlCard's fetched src both rely
 * on a real browser for. Not a general HTML5 parser (no void-element table, no malformed-markup recovery, attrs
 * dropped) - enough for the trusted, author-authored snippets those two sinks are designed to receive.
 */
function parseHtmlFragment(doc, html) {
	const frag = makeFragment();
	const stack = [frag];
	const tagRe = /<\/?[a-zA-Z][a-zA-Z0-9-]*[^>]*>/g;
	let last = 0, m;
	function appendText(text) {
		const decoded = decodeEntities(text);
		if (decoded) stack.at(-1).appendChild(doc.createTextNode(decoded));
	}
	for (m = tagRe.exec(html); m; m = tagRe.exec(html)) {
		appendText(html.slice(last, m.index));
		last = tagRe.lastIndex;
		const tag = m[0];
		if (tag.startsWith('</')) {
			if (stack.length > 1) stack.pop();
			continue;
		}
		const name = /^<([a-zA-Z][a-zA-Z0-9-]*)/.exec(tag)[1];
		const node = doc.createElement(name);
		stack.at(-1).appendChild(node);
		if (!tag.endsWith('/>')) stack.push(node);
	}
	appendText(html.slice(last));
	return frag;
}

/** Gives a `<template>` element a real `innerHTML` setter: parses into `.content` instead of storing a plain string. */
function addTemplateInnerHtml(doc, node) {
	let raw = '';
	Object.defineProperty(node, 'innerHTML', {
		get: function () { return raw; },
		set: function (html) {
			raw = html == null ? '' : String(html);
			node.content.childNodes.forEach(function (c) { c.parentNode = null; });
			node.content.childNodes.length = 0;
			// .slice(): appendChild below removes each node from the parsed fragment it currently sits in (the
			// shim's own appendChild always does `c.remove()` first) - iterating the live array in place would
			// skip every other node as the in-progress splice shifts the remaining indices under the iterator.
			for (const c of parseHtmlFragment(doc, raw).childNodes.slice()) node.content.appendChild(c);
		}
	});
}

/** A minimal (nodeType 11) DocumentFragment: `appendChild` flattens a nested fragment's children into itself. */
function makeFragment() {
	const frag = {
		nodeType: 11,
		childNodes: [],
		parentNode: null,
		remove: function () { /* no-op */ },
		removeChild: function (c) {
			const i = frag.childNodes.indexOf(c);
			if (i >= 0) { frag.childNodes.splice(i, 1); c.parentNode = null; }
			return c;
		},
		appendChild: function (c) {
			if (c?.nodeType === 11) {
				const kids = c.childNodes.slice();
				c.childNodes.length = 0;
				for (const k of kids) frag.appendChild(k);
				return c;
			}
			if (c && typeof c.remove === 'function') c.remove();
			frag.childNodes.push(c);
			c.parentNode = frag;
			return c;
		},
		get firstElementChild() {
			return frag.childNodes.find(function (c) { return c.nodeType === 1; }) || null;
		}
	};
	return frag;
}

/** Wraps `node`'s own appendChild/insertBefore so passing a (nodeType 11) fragment flattens its children in. */
function patchFragmentAwareness(node) {
	if (!node || node.__jcFragPatched) return node;
	node.__jcFragPatched = true;
	if (typeof node.appendChild === 'function') {
		const orig = node.appendChild.bind(node);
		node.appendChild = function (c) {
			if (c?.nodeType === 11) {
				const kids = c.childNodes.slice();
				c.childNodes.length = 0;
				for (const k of kids) orig(k);
				return c;
			}
			return orig(c);
		};
	}
	if (typeof node.insertBefore === 'function') {
		const origIns = node.insertBefore.bind(node);
		node.insertBefore = function (c, ref) {
			if (c?.nodeType === 11) {
				const kids = c.childNodes.slice();
				c.childNodes.length = 0;
				for (const k of kids) origIns(k, ref);
				return c;
			}
			return origIns(c, ref);
		};
	}
	return node;
}

/**
 * Builds a views-dom-shim env plus the console-shell-specific primitives above, and returns it (same shape as
 * `shim.makeEnv()`, with the additions below).
 *
 * @param opts Optional `{ location: { origin, pathname, search } }` override for `window.location`.
 */
function makeConsoleEnv(opts) {
	const env = shim.makeEnv();
	const doc = env.document;
	const win = env.window;

	win.location = { origin: 'http://juneau.test', pathname: '/', search: '', ...opts?.location }; // NOSONAR javascript:S5332 -- fake in-memory origin for the shim, never a real connection
	win.URLSearchParams = URLSearchParams;
	win.CustomEvent = function (type, init) {
		this.type = type;
		this.detail = init?.detail;
	};

	doc.createDocumentFragment = function () { return makeFragment(); };
	doc.importNode = function (node, deep) { return cloneDeep(doc, node, deep); };

	patchFragmentAwareness(doc.body);
	patchFragmentAwareness(doc.documentElement);
	const origCreateElement = doc.createElement.bind(doc);
	doc.createElement = function (tag) {
		const node = patchFragmentAwareness(origCreateElement(tag));
		if (String(tag).toLowerCase() === 'template') addTemplateInnerHtml(doc, node);
		return node;
	};

	// Listener bookkeeping kept ALONGSIDE (not instead of) the base shim's own dispatch registry: forwarding to the
	// original addEventListener keeps env.dispatchDocument()/doc.dispatchEvent() working, while this harness-only
	// map lets a test ask "how many listeners of type X are registered" - the console-ui keydown-listener-leak
	// regression (WORK-J0559 Task 5 addendum): mounting on two different roots of the SAME document must wire the
	// Escape-to-close-user-menu keydown listener exactly once, not once per mount.
	const listenerCounts = {};
	const origAddEventListener = doc.addEventListener.bind(doc);
	doc.addEventListener = function (type, fn) {
		(listenerCounts[type] = listenerCounts[type] || []).push(fn);
		origAddEventListener(type, fn);
	};
	doc.dispatchEvent = function (ev) {
		env.dispatchDocument(ev.type, ev);
		return true;
	};

	env.listenerCount = function (type) { return (listenerCounts[type] || []).length; };

	env.fireDOMContentLoaded = function () {
		doc.readyState = 'interactive';
		doc.dispatchEvent({ type: 'DOMContentLoaded' });
	};

	return env;
}

module.exports = { makeConsoleEnv: makeConsoleEnv, cloneDeep: cloneDeep };
