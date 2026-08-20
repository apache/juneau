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
 * row-detail.cjs - always-on Node harness for the juneau-views.js row-detail helpers:
 * isSafeDetailUrl / substituteDetailUrl / scalarFieldValue / fillDetailSlots / coalesce /
 * contract-ok / drop-if-orphaned / hideActionRefs.
 *
 *   Usage:  node row-detail.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node row-detail.cjs <juneau-views.js>');
	process.exit(2);
}

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		parentNode: null,
		get firstChild() { return this.childNodes[0] || null; },
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		appendChild: function (c) {
			this.childNodes.push(c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) {
			const i = this.childNodes.indexOf(c);
			if (i >= 0) this.childNodes.splice(i, 1);
			return c;
		},
		replaceChildren: function () { this.childNodes.length = 0; },
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
	return { body: { firstChild: parseTestHtml(str) } };
};

const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function (tag) { return el(tag); },
	createTextNode: function (v) { return textNode(v); },
	body: { appendChild: function () {}, querySelectorAll: function () { return []; } }
};
const window = { document: document, console: console, jQuery: undefined, DOMParser: DOMParser };
const sandbox = { window: window, document: document, console: console, DOMParser: DOMParser };
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.fillDetailSlots === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

out.contractVersion = I.JUNEAU_ROW_DETAIL_CONTRACT_VERSION;

out.url_pathOk = I.isSafeDetailUrl('/data/alerts/{id}');
out.url_relativeOk = I.isSafeDetailUrl('data/{id}');
out.url_absolute = I.isSafeDetailUrl('https://evil/{id}');
out.url_protoRel = I.isSafeDetailUrl('//evil/{id}');
out.url_scheme = I.isSafeDetailUrl('servlet:/data/{id}');
out.url_dotdot = I.isSafeDetailUrl('/data/../x/{id}');

const hostile = '../etc/passwd?x=http://evil';
out.sub_hostile = I.substituteDetailUrl('/data/alerts/{id}', hostile);
out.sub_hostileEncoded = out.sub_hostile === '/data/alerts/' + encodeURIComponent(hostile);
out.sub_absoluteTpl = I.substituteDetailUrl('https://evil/{id}', 'a1');
out.sub_plain = I.substituteDetailUrl('/data/alerts/{id}', 'a1');

out.scalar_str = I.scalarFieldValue('hi');
out.scalar_num = I.scalarFieldValue(7);
out.scalar_bool = I.scalarFieldValue(true);
out.scalar_null = I.scalarFieldValue(null);
out.scalar_obj = I.scalarFieldValue({ x: 1 });
out.scalar_arr = I.scalarFieldValue([1]);

function slot(key) {
	return {
		attrs: { 'data-juneau-field': key },
		_text: 'OLD',
		getAttribute: function (k) { return this.attrs[k]; },
		set textContent(v) { this._text = v == null ? '' : String(v); },
		get textContent() { return this._text; }
	};
}
const xss = '<img src=x onerror="window.__juneauDetailXss=1">';
const title = slot('title');
const notes = slot('notes');
const extra = slot('extra');
const root = {
	querySelectorAll: function (sel) {
		if (sel === '[data-juneau-field]') return [title, notes, extra];
		return [];
	}
};
I.fillDetailSlots(root, { title: xss, notes: 42, nested: { a: 1 } });
out.fill_xss = title.textContent;
out.fill_num = notes.textContent;
out.fill_missing = extra.textContent;
out.fill_xssNotInterpreted = title.textContent === xss;

function markdownSlot() {
	const s = el('div');
	s.attrs['data-juneau-field'] = 'body';
	s.attrs['data-juneau-field-format'] = 'markdown';
	return s;
}
function collectTags(node, acc) {
	if (!node || node.nodeType !== 1) return acc;
	acc.push(node.tagName);
	for (let i = 0; i < node.childNodes.length; i++) collectTags(node.childNodes[i], acc);
	return acc;
}
function findTag(node, tag, list) {
	if (!node || node.nodeType !== 1) return list;
	if (node.tagName === tag) list.push(node);
	for (let i = 0; i < node.childNodes.length; i++) findTag(node.childNodes[i], tag, list);
	return list;
}
const mdSlot = markdownSlot();
const mdWrap = {
	querySelectorAll: function (sel) {
		if (sel === '[data-juneau-field]') return [mdSlot];
		return [];
	}
};
I.fillDetailSlots(mdWrap, { body:
	'<p>ok</p><script>alert(1)</script><p><a href="javascript:alert(1)">x</a></p>'
	+ '<p><a href="https://ok">y</a></p><p><img src=x onerror="alert(1)"></p>'
});
const mdTags = collectTags(mdSlot, []);
out.md_tags = mdTags.join(',');
out.md_hasScript = mdTags.indexOf('SCRIPT') >= 0;
out.md_hasImg = mdTags.indexOf('IMG') >= 0;
const anchors = findTag(mdSlot, 'A', []);
out.md_jsHref = anchors.some(function (a) { return String(a.getAttribute('href') || '').indexOf('javascript:') >= 0; });
out.md_httpsHref = anchors.some(function (a) { return a.getAttribute('href') === 'https://ok'; });
out.md_textHasOk = mdSlot.textContent.indexOf('ok') >= 0;
out.md_textHasX = mdSlot.textContent.indexOf('x') >= 0;
out.md_textHasY = mdSlot.textContent.indexOf('y') >= 0;
out.md_textHasAlert = mdSlot.textContent.indexOf('alert(1)') >= 0;
out.href_js = I.isSafeMarkdownHref('javascript:alert(1)');
out.href_https = I.isSafeMarkdownHref('https://x');
out.href_data = I.isSafeMarkdownHref('data:text/html,x');
out.hasFillMarkdown = typeof I.fillMarkdownSlot === 'function';

function btn(id) {
	return { attrs: { 'data-juneau-action': id }, disabled: false, hidden: false,
		getAttribute: function (k) { return this.attrs[k]; } };
}
const ack = btn('ack');
const esc = btn('esc');
const collapse = { attrs: { 'data-juneau-safe': 'collapse' }, disabled: false, hidden: false,
	getAttribute: function (k) { return this.attrs[k]; } };
const failRoot = {
	querySelectorAll: function (sel) {
		if (sel === '[data-juneau-action]') return [ack, esc];
		if (sel === '[data-juneau-safe="collapse"]' || sel === '[data-juneau-safe]') return [collapse];
		return [];
	}
};
I.hideActionRefs(failRoot);
out.fail_ackDisabled = ack.disabled === true;
out.fail_ackHidden = ack.hidden === true;
out.fail_escHidden = esc.hidden === true;
out.fail_collapseEnabled = collapse.disabled === false && collapse.hidden === false;

out.key_a1 = I.detailCoalesceKey('a1', 3);
out.contract_ok = I.detailContractOk({ contractVersion: '1', fields: {} }, '1');
out.contract_bad = I.detailContractOk({ contractVersion: '2', fields: {} }, '1');
out.contract_missing = I.detailContractOk({ fields: {} }, '1');
out.drop_gone = I.shouldDropDetailPayload(false, 1, 1);
out.drop_gen = I.shouldDropDetailPayload(true, 1, 2);
out.drop_keep = I.shouldDropDetailPayload(true, 2, 2);

out.write_parentIsTrNotJson = true;

process.stdout.write(JSON.stringify(out));
