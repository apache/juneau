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
 * cell-popover.cjs - always-on Node harness for CellPopover trigger + fill (createElement/textContent).
 *
 *   Usage:  node cell-popover.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node cell-popover.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		parentNode: null,
		className: '',
		get firstChild() { return this.childNodes[0] || null; },
		getAttribute: function (k) {
			if (k === 'class' && this.className) return this.className;
			return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null;
		},
		setAttribute: function (k, v) {
			this.attrs[k] = v == null ? '' : String(v);
			if (k === 'class') this.className = this.attrs[k];
		},
		removeAttribute: function (k) { delete this.attrs[k]; },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		removeChild: function (c) {
			const i = this.childNodes.indexOf(c);
			if (i >= 0) this.childNodes.splice(i, 1);
			return c;
		},
		replaceChildren: function () { this.childNodes.length = 0; },
		contains: function (n) {
			if (n === this) return true;
			for (let i = 0; i < this.childNodes.length; i++) {
				if (this.childNodes[i] === n) return true;
				if (this.childNodes[i].contains && this.childNodes[i].contains(n)) return true;
			}
			return false;
		},
		set textContent(v) { this.childNodes.length = 0; this._text = v == null ? '' : String(v); },
		get textContent() {
			if (this.childNodes.length === 0) return this._text || '';
			return this.childNodes.map(function (c) { return c.textContent; }).join('');
		}
	};
	node._text = '';
	return node;
}

function textNode(value) {
	return {
		nodeType: 3,
		nodeValue: value == null ? '' : String(value),
		childNodes: [],
		get textContent() { return this.nodeValue; }
	};
}

const VOID_TAGS = { br: 1, hr: 1, img: 1, input: 1, meta: 1, link: 1, base: 1 };

function parseAttrs(raw, node) {
	if (!raw) return;
	const re = /([:@\w-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g;
	let m;
	while ((m = re.exec(raw)))
		node.setAttribute(m[1], m[2] != null ? m[2] : (m[3] != null ? m[3] : (m[4] != null ? m[4] : '')));
}

function parseTestHtml(html) {
	const root = el('div');
	const stack = [root];
	const re = /<\/?([a-zA-Z][a-zA-Z0-9]*)\b([^>]*)\/?>|([^<]+)/g;
	let m;
	while ((m = re.exec(html))) {
		if (m[3] != null) {
			stack[stack.length - 1].appendChild(textNode(m[3]));
			continue;
		}
		const name = m[1];
		const closing = html.charAt(m.index + 1) === '/';
		if (closing) {
			if (stack.length > 1) stack.pop();
			continue;
		}
		const node = el(name);
		parseAttrs(m[2], node);
		stack[stack.length - 1].appendChild(node);
		const selfClosing = VOID_TAGS[name.toLowerCase()] || /\/\s*$/.test(m[2] || '');
		if (!selfClosing) stack.push(node);
	}
	return root;
}

function DOMParser() {}
DOMParser.prototype.parseFromString = function (str) {
	return { body: parseTestHtml(str) };
};

const byId = {};
const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function (id) { return byId[id] || null; },
	createElement: function (tag) {
		const n = el(tag);
		const orig = n.setAttribute;
		n.setAttribute = function (k, v) {
			orig.call(n, k, v);
			if (k === 'id') byId[v] = n;
		};
		return n;
	},
	createTextNode: function (v) { return textNode(v); },
	body: el('body')
};
document.body.appendChild = function (c) {
	el.prototype;
	this.childNodes.push(c);
	if (c.attrs && c.attrs.id) byId[c.attrs.id] = c;
	return c;
};
const window = { document: document, console: console, jQuery: undefined, DOMParser: DOMParser };
const sandbox = { window: window, document: document, console: console, DOMParser: DOMParser };
vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.appendPopoverTrigger === 'function' && typeof I.fillCellPopover === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const linkedHtml = '<a href="/x">Go</a>';
const withTrigger = I.appendPopoverTrigger(linkedHtml, { data: 'name' }, {
	popover: { title: 'More', fields: [{ data: 'id' }] }
});
out.trigger_hasButton = withTrigger.indexOf('jc-cell-popover-trigger') >= 0;
out.trigger_hasAnchor = withTrigger.indexOf('<a href="/x">Go</a>') >= 0;
out.trigger_buttonAfterAnchor = withTrigger.indexOf('<a href="/x">Go</a><button') >= 0;
out.trigger_noButtonWrap = withTrigger.indexOf('<button') > withTrigger.indexOf('</a>');
const hostileTitle = I.appendPopoverTrigger('x', { data: 'used' }, {
	popover: { title: '" onclick=alert(1)', fields: [{ data: 'a' }] }
});
out.trigger_titleEscaped = hostileTitle.indexOf('" onclick=') < 0 && hostileTitle.indexOf('&quot;') >= 0;
out.trigger_colEscaped = I.appendPopoverTrigger('x', { data: 'a&b' }, {
	popover: { fields: [{ data: 'a' }] }
}).indexOf('data-juneau-popover-col="a&amp;b"') >= 0
	|| I.appendPopoverTrigger('x', { data: 'a&b' }, { popover: { fields: [{ data: 'a' }] } })
		.indexOf('a&amp;b') >= 0;

const ts = NS.resolveRenderer('ts-zulu');
const tsHtml = String(ts.display('2026-08-20T20:11:00Z', {}, {}));
out.ts_hasDataAttr = tsHtml.indexOf('data-juneau-ts') >= 0;
out.ts_noTrigger = tsHtml.indexOf('jc-cell-popover-trigger') < 0;
out.ts_plusTrigger = I.appendPopoverTrigger(tsHtml, { data: 'created' }, {
	popover: { fields: [{ data: 'id' }] }
}).indexOf('jc-cell-popover-trigger') >= 0;

const pop = el('div');
pop.id = 'juneau-cell-popover';
pop.attrs.id = 'juneau-cell-popover';
byId['juneau-cell-popover'] = pop;
I.fillCellPopover(pop, {
	title: 'CPU',
	fields: [
		{ data: 'actual', title: 'Actual' },
		{ data: 'missing', title: 'Missing' },
		{ data: 'created', title: 'Created', render: { id: 'date' } },
		{ data: 'ts', title: 'TS', render: { id: 'ts-zulu' } }
	]
}, { actual: 12, created: '2026-08-20T20:11:00Z', ts: '2026-08-20T20:11:00Z' });
out.fill_title = pop.textContent.indexOf('CPU') >= 0;
out.fill_actual = pop.textContent.indexOf('12') >= 0;
out.fill_missingBlank = pop.textContent.indexOf('undefined') < 0 && pop.textContent.indexOf('null') < 0;
out.fill_noTsHost = (function () {
	function walk(n) {
		if (!n || n.nodeType !== 1) return false;
		if (n.getAttribute('data-juneau-ts')) return true;
		for (let i = 0; i < n.childNodes.length; i++) if (walk(n.childNodes[i])) return true;
		return false;
	}
	return !walk(pop);
})();

window.JuneauViews.registerRenderer('date', { display: function () { return '<img src=x onerror=alert(1)>'; } });
out.freeze_dateStillText = String(NS.resolveSinkRenderer('date').display('2026-08-20T20:11:00Z', {}, {})).indexOf('<img') < 0;

const hostile = el('div');
I.fillCellPopover(hostile, {
	fields: [{ data: 'v', render: { id: 'tag' } }]
}, { v: 'x' });
out.fill_htmlShapedFallsBack = hostile.textContent.indexOf('x') >= 0;
out.fill_noElementCopy = (function () {
	function walk(n) {
		if (!n || n.nodeType !== 1) return false;
		if (n !== hostile && n.tagName === 'SPAN') return true;
		for (let i = 0; i < n.childNodes.length; i++) if (walk(n.childNodes[i])) return true;
		return false;
	}
	return !walk(hostile);
})();

process.stdout.write(JSON.stringify(out));
