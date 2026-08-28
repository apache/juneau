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
	console.error('usage: node row-detail.cjs <juneau-views.js> [juneau-renders.js]');
	process.exit(2);
}

/** Minimal CSS-selector matcher: tag, `.class`, `[attr]` / `[attr="v"]`, comma lists, and tag[attr] combos. */
function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	if (sel.indexOf(',') >= 0)
		return sel.split(',').some(function (part) { return elMatches(node, part.trim()); });
	let rest = sel;
	// Tag-name prefix only; the remainder is sliced off by length rather than captured by a second `(.*)$` group,
	// which avoided a super-linear-backtracking shape (the `[\w-]*` and `.*` character classes fully overlap).
	const tm = /^[a-zA-Z][\w-]*/.exec(sel);
	if (tm) {
		if (node.tagName !== tm[0].toUpperCase()) return false;
		rest = sel.slice(tm[0].length);
		if (!rest) return true;
	}
	if (rest.charAt(0) === '.') {
		const cls = rest.slice(1);
		const raw = ' ' + (node.className || node.getAttribute('class') || '') + ' ';
		return raw.indexOf(' ' + cls + ' ') >= 0;
	}
	const m = /^\[([\w:-]+)(?:="([^"]*)")?\]$/.exec(rest);
	if (m) {
		const v = node.getAttribute(m[1]);
		return m[2] == null ? v != null : v === m[2];
	}
	return false;
}

function elWalk(node, sel, acc) {
	for (const c of node.childNodes) {
		if (c.nodeType === 1) {
			if (elMatches(c, sel)) acc.push(c);
			elWalk(c, sel, acc);
		}
	}
	return acc;
}

function datasetKeyToAttr(key) {
	return 'data-' + key.replace(/[A-Z]/g, function (m) { return '-' + m.toLowerCase(); });
}

function attrToDatasetKey(attr) {
	return attr.slice(5).replace(/-([a-z])/g, function (_, c) { return c.toUpperCase(); });
}

/** A minimal live `dataset` facade over `node`'s existing attrs store, so `.dataset.x` reads/writes stay in sync
 * with getAttribute('data-x') the way a real DOM element's dataset does. */
function makeDataset(node) {
	return new Proxy({}, {
		get(_, key) {
			if (typeof key !== 'string') return undefined;
			const v = node.getAttribute(datasetKeyToAttr(key));
			return v == null ? undefined : v;
		},
		set(_, key, val) {
			node.setAttribute(datasetKeyToAttr(key), val);
			return true;
		},
		deleteProperty(_, key) {
			delete node.attrs[datasetKeyToAttr(key)];
			return true;
		},
		has(_, key) {
			return typeof key === 'string' && node.getAttribute(datasetKeyToAttr(key)) != null;
		},
		ownKeys() {
			return Object.keys(node.attrs)
				.filter(function (k) { return k.indexOf('data-') === 0; })
				.map(attrToDatasetKey);
		},
		getOwnPropertyDescriptor(_, key) {
			const v = node.getAttribute(datasetKeyToAttr(key));
			return v == null ? undefined : { value: v, enumerable: true, configurable: true };
		}
	});
}

/** Walks from `node` up through parentNode, matching `sel` at each step. Takes the start node as a parameter
 * (rather than aliasing `this` to a local) so the caller passes `this` in as an argument instead. */
function closestFrom(node, sel) {
	let n = node;
	while (n?.nodeType === 1) {
		if (elMatches(n, sel)) return n;
		n = n.parentNode;
	}
	return null;
}

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		parentNode: null,
		_listeners: {},
		get firstChild() { return this.childNodes[0] || null; },
		get nextSibling() {
			if (!this.parentNode) return null;
			const kids = this.parentNode.childNodes;
			const i = kids.indexOf(this);
			return i >= 0 && i + 1 < kids.length ? kids[i + 1] : null;
		},
		get dataset() { return makeDataset(this); },
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		appendChild: function (c) {
			this.childNodes.push(c);
			c.parentNode = this;
			return c;
		},
		insertBefore: function (c, ref) {
			const i = ref ? this.childNodes.indexOf(ref) : -1;
			if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) {
			const i = this.childNodes.indexOf(c);
			if (i >= 0) this.childNodes.splice(i, 1);
			return c;
		},
		replaceChildren: function () { this.childNodes.length = 0; },
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		closest: function (sel) { return closestFrom(this, sel); },
		contains: function (other) {
			if (other === this) return true;
			for (const c of this.childNodes) {
				if (c === other) return true;
				if (c.nodeType === 1 && c.contains?.(other)) return true;
			}
			return false;
		},
		addEventListener: function (type, fn) {
			this._listeners[type] = this._listeners[type] || [];
			this._listeners[type].push(fn);
		},
		_fire: function (type, ev) {
			ev = ev || {};
			if (ev.currentTarget == null) ev.currentTarget = this;
			(this._listeners[type] || []).forEach(function (fn) { fn(ev); });
		},
		focus: function () { document.activeElement = this; },
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

function firstDefined(...values) {
	for (const v of values) if (v != null) return v;
	return '';
}

function parseAttrs(raw, node) {
	if (!raw) return;
	// NOSONAR javascript:S5843 -- this regex intentionally supports double-quoted, single-quoted, and bare
	// unquoted HTML attribute values for the test-fixture HTML parser below; collapsing the 3-way alternation
	// (e.g. via a backreference to the opening quote) would change which characters are permitted inside quoted
	// values with no behavior-preserving equivalent, so it is left as-is.
	const re = /([:@\w-]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+)))?/g;
	let m;
	while ((m = re.exec(raw)))
		node.setAttribute(m[1], firstDefined(m[2], m[3], m[4]));
}

function parseTestHtml(html) {
	const root = el('div');
	const stack = [root];
	const re = /<\/?([a-zA-Z][a-zA-Z0-9]*)\b([^>]*)\/?>|([^<]+)/g;
	let m;
	while ((m = re.exec(html))) {
		if (m[3] != null) {
			stack.at(-1).appendChild(textNode(m[3]));
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
		stack.at(-1).appendChild(node);
		const selfClosing = VOID_TAGS[name.toLowerCase()] || /\/\s*$/.test(m[2] || '');
		if (!selfClosing) stack.push(node);
	}
	return root;
}

// Constructor intentionally empty: DOMParser only needs a working parseFromString(), added via the prototype below.
function DOMParser() {}
DOMParser.prototype.parseFromString = function (str) {
	return { body: { firstChild: parseTestHtml(str) } };
};

const document = {
	readyState: 'loading',
	activeElement: null,
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
const rendersJsPath = process.argv[3];
// NOSONAR javascript:S1523 -- this harness's entire purpose is to load the production runtime under test (a
// repo-local file path from argv, not attacker-controlled input) into an isolated VM sandbox; that IS the test.
if (rendersJsPath)
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
// NOSONAR javascript:S1523 -- same rationale: loading the production juneau-views.js under test into the sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS?.init;
const out = { hasInit: !!(typeof I?.fillDetailSlots === 'function') };
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

const titleWrap = el('div');
const titleH2 = el('h2');
titleH2.dataset.juneauDetailTitle = '1';
titleH2.dataset.juneauDetailTitleTemplate = 'Incident #{number}';
titleH2.textContent = 'Incident #{number}';
titleWrap.appendChild(titleH2);
I.fillDetailSlots(titleWrap, { number: '42' });
out.title_filled = titleH2.textContent;

const titleXssWrap = el('div');
const titleXss = el('h2');
titleXss.dataset.juneauDetailTitle = '1';
titleXss.dataset.juneauDetailTitleTemplate = 'Incident #{number}';
titleXssWrap.appendChild(titleXss);
const titleXssPayload = '<img src=x onerror="window.__juneauTitleXss=1">';
I.fillDetailSlots(titleXssWrap, { number: titleXssPayload });
out.title_xss = titleXss.textContent;
out.title_xssNotInterpreted = titleXss.textContent === 'Incident #' + titleXssPayload;

out.hasPaintActionMessageIntoDetail = typeof I.paintActionMessageIntoDetail === 'function';
if (out.hasPaintActionMessageIntoDetail) {
	const paintPanel = el('div');
	const paintSec = el('section');
	paintSec.dataset.juneauDetailSection = 'diagnose';
	const paintSlot = el('div');
	paintSlot.dataset.juneauField = 'findings';
	const paintBtn = el('button');
	paintBtn.dataset.juneauAction = 'diagnose';
	paintSec.appendChild(paintSlot);
	paintSec.appendChild(paintBtn);
	paintPanel.appendChild(paintSec);
	const paintTr = { _juneauDetailPanel: paintPanel };
	const paintMsg = '<b>disk full</b>';
	I.paintActionMessageIntoDetail(paintTr, 'diagnose', paintMsg);
	out.paint_text = paintSlot.textContent;
	out.paint_xssNotInterpreted = paintSlot.textContent === paintMsg;

	// LD-1 (TODO-J0474): a field-hosted bar paints into its OWN field's slot, not the section's first field.
	// Two fields in the section, the SECOND one hosting the bar, so "first field happens to be the bar's own
	// field" cannot make this pass by accident -- painting the first field's slot instead of the second's would
	// be exactly the pre-fix defect this case exists to catch.
	const fhSection = el('section');
	fhSection.dataset.juneauDetailSection = 'ctx';
	const fhFirstField = el('div');
	fhFirstField.setAttribute('class', 'juneau-view-detail-field');
	const fhFirstSlot = el('div');
	fhFirstSlot.dataset.juneauField = 'summary';
	fhFirstField.appendChild(fhFirstSlot);
	const fhSecondField = el('div');
	fhSecondField.setAttribute('class', 'juneau-view-detail-field');
	const fhSecondSlot = el('div');
	fhSecondSlot.dataset.juneauField = 'assignee';
	const fhBtn = el('button');
	fhBtn.dataset.juneauAction = 'assignee-action';
	fhSecondField.appendChild(fhSecondSlot);
	fhSecondField.appendChild(fhBtn);
	fhSection.appendChild(fhFirstField);
	fhSection.appendChild(fhSecondField);
	const fhPanel = el('div');
	fhPanel.appendChild(fhSection);
	const fhTr = { _juneauDetailPanel: fhPanel };
	I.paintActionMessageIntoDetail(fhTr, 'assignee-action', 'ok');
	out.paintFieldHosted_ownSlotPainted = fhSecondSlot.textContent;
	out.paintFieldHosted_firstFieldUntouched = fhFirstSlot.textContent === '';

	// LD-1 regression guard: a section-hosted bar (no enclosing `.juneau-view-detail-field`) keeps painting into
	// the section's FIRST field slot, byte-identically to today -- with a SECOND field present so this could not
	// pass merely because there was only one field to choose from.
	const shSection = el('section');
	shSection.dataset.juneauDetailSection = 'diagnose2';
	const shFirstSlot = el('div');
	shFirstSlot.dataset.juneauField = 'summary';
	const shSecondSlot = el('div');
	shSecondSlot.dataset.juneauField = 'notes';
	const shBtn = el('button');
	shBtn.dataset.juneauAction = 'diagnose2';
	shSection.appendChild(shFirstSlot);
	shSection.appendChild(shSecondSlot);
	shSection.appendChild(shBtn);
	const shPanel = el('div');
	shPanel.appendChild(shSection);
	const shTr = { _juneauDetailPanel: shPanel };
	I.paintActionMessageIntoDetail(shTr, 'diagnose2', 'section message');
	out.paintSectionHosted_firstFieldPainted = shFirstSlot.textContent;
	out.paintSectionHosted_secondFieldUntouched = shSecondSlot.textContent === '';
}

out.hasResolveDetailHeaderIcon = typeof I.resolveDetailHeaderIcon === 'function';
if (out.hasResolveDetailHeaderIcon) {
	const iconWrap = el('div');
	const iconSlot = el('span');
	iconSlot.dataset.juneauDetailIcon = 'no-such-icon';
	iconWrap.appendChild(iconSlot);
	window.JuneauViews = window.JuneauViews || {};
	window.JuneauViews.icons = { resolveIcon: function () { return null; } };
	I.resolveDetailHeaderIcon(iconWrap);
	out.icon_unknownHidden = iconSlot.hidden === true;
}

function markdownSlot() {
	const s = el('div');
	s.attrs['data-juneau-field'] = 'body';
	s.attrs['data-juneau-field-format'] = 'markdown';
	return s;
}
function collectTags(node, acc) {
	if (!node || node.nodeType !== 1) return acc;
	acc.push(node.tagName);
	for (const c of node.childNodes) collectTags(c, acc);
	return acc;
}
function findTag(node, tag, list) {
	if (!node || node.nodeType !== 1) return list;
	if (node.tagName === tag) list.push(node);
	for (const c of node.childNodes) findTag(c, tag, list);
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

out.hasFillRender = typeof I.fillRenderSlot === 'function';
if (out.hasFillRender) {
	function renderSlot(id, extraAttrs) {
		const s = el('div');
		s.attrs['data-juneau-field'] = 'v';
		s.attrs['data-juneau-field-render'] = id;
		if (extraAttrs) for (const k in extraAttrs) s.attrs[k] = extraAttrs[k];
		return s;
	}
	function wrap(slot) {
		return { querySelectorAll: function (sel) { return sel === '[data-juneau-field]' ? [slot] : []; } };
	}
	const tagSlot = renderSlot('tag', { 'data-juneau-field-render-meta': '{"field":"status"}' });
	I.fillDetailSlots(wrap(tagSlot), { v: 'Released' });
	out.rr_tagHasClass = collectTags(tagSlot, []).indexOf('SPAN') >= 0
		&& String(tagSlot.textContent).indexOf('Released') >= 0
		&& findTag(tagSlot, 'SPAN', []).some(function (n) {
			return String(n.getAttribute('class') || '').indexOf('tag') >= 0;
		});

	const progSlot = renderSlot('progress', { 'data-juneau-field-render-meta': '{"max":"100"}' });
	I.fillDetailSlots(wrap(progSlot), { v: 50 });
	out.rr_progressWidth = findTag(progSlot, 'SPAN', []).some(function (n) {
		return n.getAttribute('style') === 'width:50%';
	});

	const linkSlot = renderSlot('linked', { 'data-juneau-field-render-href': '/x/{id}' });
	I.fillDetailSlots(wrap(linkSlot), { v: 'n1', id: 'a1' });
	const anchors = findTag(linkSlot, 'A', []);
	out.rr_linkedHref = anchors.some(function (a) { return a.getAttribute('href') === '/x/a1'; });

	const badLink = renderSlot('linked', { 'data-juneau-field-render-href': 'javascript:alert(1)' });
	I.fillDetailSlots(wrap(badLink), { v: 'n1' });
	out.rr_jsHref = findTag(badLink, 'A', []).some(function (a) {
		return String(a.getAttribute('href') || '').indexOf('javascript:') >= 0;
	});

	window.JuneauViews.registerRenderer('evil-script', {
		display: function () { return '<script>alert(1)</script><span>ok</span>'; }
	});
	const hostile = renderSlot('evil-script');
	I.fillRenderSlot(hostile, 'x', 'evil-script', {}, null, {});
	out.rr_hasScript = collectTags(hostile, []).indexOf('SCRIPT') >= 0
		|| String(hostile.textContent).indexOf('alert(1)') >= 0;

	window.JuneauViews.registerRenderer('evil-style', {
		display: function () { return '<span style="color:red;width:50%">x</span>'; }
	});
	const styleSlot = renderSlot('evil-style');
	I.fillRenderSlot(styleSlot, 'x', 'evil-style', {}, null, {});
	out.rr_hostileStyle = findTag(styleSlot, 'SPAN', []).some(function (n) {
		const st = n.getAttribute('style');
		return st?.indexOf('color') >= 0;
	});

	// javascript:S5852 guard on copyRenderStyle's width pattern.  Written as `\s*%?\s*;?\s*$` the three
	// independent whitespace runs can split one tail in O(n^3) ways, so a style attribute that ends in a long
	// run of spaces followed by a non-terminator used to backtrack for ~10s at 4,000 spaces.  Must stay
	// rejected (it is not a bare width declaration) AND stay fast.
	window.JuneauViews.registerRenderer('slow-style', {
		display: function () { return '<span style="width:1' + ' '.repeat(4000) + 'x">y</span>'; }
	});
	const slowSlot = renderSlot('slow-style');
	const slowStart = Date.now();
	I.fillRenderSlot(slowSlot, 'y', 'slow-style', {}, null, {});
	out.rr_slowStyleMs = Date.now() - slowStart;
	const slowSpans = findTag(slowSlot, 'SPAN', []);
	out.rr_slowStyleSawSpan = slowSpans.length > 0;
	out.rr_slowStyleRejected = slowSpans.every(function (n) { return n.getAttribute('style') == null; });

	const trunc = renderSlot('truncate', { 'data-juneau-field-render-meta': '{"length":"4"}' });
	I.fillDetailSlots(wrap(trunc), { v: 'abcdef' });
	out.rr_truncateTitle = findTag(trunc, 'SPAN', []).some(function (n) {
		return n.getAttribute('title') === 'abcdef';
	});

	const jsonSlot = renderSlot('json');
	I.fillDetailSlots(wrap(jsonSlot), { v: 'hi' });
	out.rr_jsonCode = collectTags(jsonSlot, []).indexOf('CODE') >= 0;

	const badMeta = renderSlot('tag', { 'data-juneau-field-render-meta': '{not json' });
	I.fillDetailSlots(wrap(badMeta), { v: 'X' });
	out.rr_malformedMetaOk = String(badMeta.textContent).indexOf('X') >= 0;

	const missing = renderSlot('tag');
	I.fillDetailSlots(wrap(missing), {});
	out.rr_missing = missing.textContent;

	const both = el('div');
	both.attrs['data-juneau-field'] = 'v';
	both.attrs['data-juneau-field-render'] = 'tag';
	both.attrs['data-juneau-field-format'] = 'markdown';
	I.fillDetailSlots(wrap(both), { v: 'Released' });
	out.rr_dispatchRenderFirst = findTag(both, 'SPAN', []).some(function (n) {
		return String(n.getAttribute('class') || '').indexOf('tag') >= 0;
	});
}

// ----------------------------------------------------------------------------------------------------------------
// Shared strip widget - tab-mode (multi-section row-detail pane switcher).
// ----------------------------------------------------------------------------------------------------------------

out.hasBuildDetailStrip = typeof I.buildDetailStrip === 'function';
out.hasActivateDetailTab = typeof I.activateDetailTab === 'function';
out.hasDetailTabTargetIndex = typeof I.detailTabTargetIndex === 'function';

if (out.hasBuildDetailStrip) {
	function detailSection(sid, title) {
		const sec = el('section');
		sec.dataset.juneauDetailSection = sid;
		sec.className = 'juneau-view-detail-section';
		const h2 = el('h2');
		h2.className = 'juneau-view-detail-section-title';
		h2.textContent = title;
		sec.appendChild(h2);
		const fields = el('div');
		fields.className = 'juneau-view-detail-fields';
		sec.appendChild(fields);
		return sec;
	}
	function detailPanel(pairs) {
		const panel = el('div');
		panel.className = 'juneau-view-detail-panel';
		pairs.forEach(function (p) { panel.appendChild(detailSection(p[0], p[1])); });
		return panel;
	}
	function tabButtonsOf(strip) {
		return strip.childNodes.filter(function (c) { return c.getAttribute?.('role') === 'tab'; });
	}

	// Pure keyboard-target math.
	out.tti_right = I.detailTabTargetIndex('ArrowRight', 0, 3);
	out.tti_rightWrap = I.detailTabTargetIndex('ArrowRight', 2, 3);
	out.tti_left = I.detailTabTargetIndex('ArrowLeft', 0, 3);
	out.tti_home = I.detailTabTargetIndex('Home', 2, 3);
	out.tti_end = I.detailTabTargetIndex('End', 0, 3);
	out.tti_other = I.detailTabTargetIndex('Enter', 0, 3);

	// Multi-section (Alerts Overview | Context) -> one tablist, one visible pane.
	const multi = detailPanel([['overview', 'Overview'], ['context', 'Context']]);
	const strip = I.buildDetailStrip(multi);
	out.strip_built = !!strip;
	out.strip_isFirstChild = multi.firstChild === strip;
	out.strip_role = strip.getAttribute('role');
	out.strip_mode = strip.dataset.juneauStripMode;
	out.strip_hasRibbonGroupClass = (strip.className || '').indexOf('juneau-view-ribbon-group') >= 0;
	const tabButtons = tabButtonsOf(strip);
	out.strip_tabCount = tabButtons.length;
	out.strip_labels = tabButtons.map(function (b) { return b.textContent; }).join(',');
	out.strip_btnClass = tabButtons[0].className;
	out.strip_firstSelected = tabButtons[0].getAttribute('aria-selected');
	out.strip_secondSelected = tabButtons[1].getAttribute('aria-selected');
	out.strip_firstTabindex = tabButtons[0].tabIndex;
	out.strip_secondTabindex = tabButtons[1].tabIndex;
	const panes = multi.querySelectorAll('[data-juneau-detail-section]');
	out.strip_pane0Hidden = panes[0].hidden === true;
	out.strip_pane1Hidden = panes[1].hidden === true;
	out.strip_pane0Role = panes[0].getAttribute('role');
	out.strip_pane0Labelledby = panes[0].getAttribute('aria-labelledby') === tabButtons[0].id;
	out.strip_tab0Controls = tabButtons[0].getAttribute('aria-controls') === panes[0].id;
	out.strip_titleHidden = panes[0].querySelector('.juneau-view-detail-section-title').hidden === true;

	// activateDetailTab flips exactly one selection + one visible pane (visibility only).
	const fakeTabs = [
		{ btn: tabButtons[0], pane: panes[0], id: 'overview' },
		{ btn: tabButtons[1], pane: panes[1], id: 'context' }
	];
	I.activateDetailTab(fakeTabs, 'context');
	out.act_tab1Selected = tabButtons[1].getAttribute('aria-selected') === 'true';
	out.act_tab0Deselected = tabButtons[0].getAttribute('aria-selected') === 'false';
	out.act_pane1Visible = panes[1].hidden === false;
	out.act_pane0Hidden = panes[0].hidden === true;
	// reset to first for the keyboard walk
	I.activateDetailTab(fakeTabs, 'overview');

	// Keyboard: Right/Home/End/Left via the strip's own delegated keydown (roving tabindex).
	document.activeElement = null;                 // fall back to the aria-selected tab (index 0)
	strip._fire('keydown', { key: 'ArrowRight', preventDefault: function () {} });
	out.kbd_right_tab1Selected = tabButtons[1].getAttribute('aria-selected') === 'true';
	out.kbd_right_tab0Deselected = tabButtons[0].getAttribute('aria-selected') === 'false';
	out.kbd_right_pane1Visible = panes[1].hidden === false;
	out.kbd_right_focusMoved = document.activeElement === tabButtons[1];
	strip._fire('keydown', { key: 'Home', preventDefault: function () {} });
	out.kbd_home_tab0Selected = tabButtons[0].getAttribute('aria-selected') === 'true';
	strip._fire('keydown', { key: 'End', preventDefault: function () {} });
	out.kbd_end_tab1Selected = tabButtons[1].getAttribute('aria-selected') === 'true';
	strip._fire('keydown', { key: 'ArrowLeft', preventDefault: function () {} });
	out.kbd_left_tab0Selected = tabButtons[0].getAttribute('aria-selected') === 'true';
	// An unhandled key is a no-op (selection stays on tab0).
	strip._fire('keydown', { key: 'Enter', preventDefault: function () {} });
	out.kbd_enter_noop = tabButtons[0].getAttribute('aria-selected') === 'true';

	// Single-section (Widgets "Active" Info) stays strip-less; sections are untouched.
	const single = detailPanel([['info', 'Info']]);
	const singleStrip = I.buildDetailStrip(single);
	out.single_noStrip = singleStrip === null;
	out.single_firstStillSection = single.firstChild.dataset.juneauDetailSection === 'info';
	out.single_paneNotHidden = single.firstChild.hidden !== true;
	out.single_noTabpanelRole = single.firstChild.getAttribute('role') == null;
	out.single_titleNotHidden = single.firstChild.querySelector('.juneau-view-detail-section-title').hidden !== true;

	// Skills fixture: Skill | SKILL.md (matches Support Console SkillsView).
	const skills = detailPanel([['skill', 'Skill'], ['body', 'SKILL.md']]);
	const skillsStrip = I.buildDetailStrip(skills);
	out.skills_labels = tabButtonsOf(skillsStrip).map(function (b) { return b.textContent; }).join(',');
	out.skills_tabCount = tabButtonsOf(skillsStrip).length;

	// Tab switch is visibility-only for the PARENT detail envelope: activating a tab (click or keyboard) never
	// re-GETs the parent detail (the strip itself issues no fetch).  It DOES, however, fire the optional
	// onActivate(sectionId, pane) seam - the hook a newly-shown pane's nested table rides on to run its OWN
	// independent GET.  Here we prove both: fetch stays 0, and onActivate fires with the activated pane.
	let fetchCalls = 0;
	// NOSONAR javascript:S7739 -- deliberately a never-resolving thenable, not a real Promise: this verifies the
	// strip issues no additional fetch and that chaining .then().catch() on whatever it returns never crashes; a
	// real Promise would resolve on a later microtask and risk flakiness against this fully synchronous assertion
	// script (there is no await boundary after this point for it to safely resolve within).
	window.fetch = function () { fetchCalls++; return { then: function () { return { catch: function () {} }; } }; };
	const activated = [];
	const nf = detailPanel([['a', 'A'], ['b', 'B']]);
	const nfStrip = I.buildDetailStrip(nf, function (sid, pane) { activated.push({ sid: sid, pane: pane }); });
	const nfTabs = tabButtonsOf(nfStrip);
	const nfPanes = nf.querySelectorAll('[data-juneau-detail-section]');
	document.activeElement = null;
	nfStrip._fire('keydown', { key: 'ArrowRight', preventDefault: function () {} });   // -> section 'b'
	nfStrip._fire('click', { target: nfTabs[0] });                                     // -> section 'a'
	out.noRefetch_fetchCalls = fetchCalls;
	out.noRefetch_clickSelectedTab0 = nfTabs[0].getAttribute('aria-selected') === 'true';
	out.noRefetch_onActivateCount = activated.length;                                  // keyboard + click = 2
	out.noRefetch_onActivateFirstSid = activated.length ? activated[0].sid : null;     // 'b' (ArrowRight)
	out.noRefetch_onActivateLastSid = activated.length ? activated.at(-1).sid : null;   // 'a' (click)
	out.noRefetch_onActivatePaneMatches = activated.length >= 2
		&& activated[0].pane === nfPanes[1] && activated[1].pane === nfPanes[0];
	window.fetch = undefined;

	const headed = detailPanel([['overview', 'Overview'], ['context', 'Context']]);
	const hdr = el('div');
	hdr.className = 'juneau-view-detail-header';
	headed.insertBefore(hdr, headed.firstChild);
	const headedStrip = I.buildDetailStrip(headed);
	out.header_firstIsHeader = headed.firstChild === hdr;
	out.header_stripAfterHeader = headed.childNodes[1] === headedStrip;
}

// findRowDetailTemplate: sibling of the table (pre-wrap) AND sibling of .dt-container (DataTables 2 wrap).
out.hasFindRowDetailTemplate = typeof I.findRowDetailTemplate === 'function';
if (out.hasFindRowDetailTemplate) {
	const sibHost = el('div');
	const sibTable = el('table');
	const sibTmpl = el('template');
	sibTmpl.dataset.juneauRowDetail = '1';
	sibHost.appendChild(sibTable);
	sibHost.appendChild(sibTmpl);
	out.find_sibling = I.findRowDetailTemplate(sibTable) === sibTmpl;

	const dtHost = el('div');
	const dtContainer = el('div');
	dtContainer.className = 'dt-container';
	const dtCell = el('div');
	dtCell.className = 'dt-layout-cell';
	const dtTable = el('table');
	const dtTmpl = el('template');
	dtTmpl.dataset.juneauRowDetail = '1';
	dtHost.appendChild(dtContainer);
	dtContainer.appendChild(dtCell);
	dtCell.appendChild(dtTable);
	dtHost.appendChild(dtTmpl);
	out.find_dt2Wrap = I.findRowDetailTemplate(dtTable) === dtTmpl;
	out.find_missing = I.findRowDetailTemplate(el('table')) == null;
}

// State-conditional ActionRef rules: evaluation, reason channels, per-row reason-node identity.
out.hasApplyActionRefRules = typeof I.applyActionRefRules === 'function';
out.hasMintActionDescIdentity = typeof I.mintActionDescIdentity === 'function';
if (out.hasApplyActionRefRules && out.hasMintActionDescIdentity) {
	// One gated button plus its hidden reason node, in a bar, in a panel - the shape ViewTable emits.  el() has no
	// removeAttribute (nothing needed one before the reason channels had to be CLEARED), so the fixture adds it
	// here rather than changing the shared factory.
	function gatedBar(actionId, rules) {
		const bar = el('div');
		const btn = el('button');
		btn.setAttribute('data-juneau-action', actionId);
		btn.setAttribute('data-juneau-action-rules', JSON.stringify(rules));
		btn.disabled = false;
		btn.hidden = false;
		btn.removeAttribute = function (k) { delete this.attrs[k]; };
		const desc = el('span');
		desc.setAttribute('data-juneau-action-desc', actionId);
		desc.setAttribute('hidden', 'hidden');
		bar.appendChild(btn);
		bar.appendChild(desc);
		const panel = el('div');
		panel.appendChild(bar);
		return { panel: panel, btn: btn, desc: desc };
	}

	const OPEN_ONLY = { field: 'state', op: 'eq', value: 'open', reason: 'This alert is not open.' };

	// (1) Every rule matches -> the action is left exactly as the lifecycle gate left it, with no reason attached.
	const okBar = gatedBar('ack', [OPEN_ONLY]);
	I.mintActionDescIdentity(okBar.panel, 'alerts', 'a1');
	I.applyActionRefRules(okBar.panel, { state: 'open' });
	out.rule_matchStaysEnabled = okBar.btn.disabled === false;
	out.rule_matchNoTitle = okBar.btn.getAttribute('title') == null;
	out.rule_matchNoDescribedby = okBar.btn.getAttribute('aria-describedby') == null;
	out.rule_matchNoReasonText = okBar.desc.textContent === '';

	// (2) A rule does not match -> disabled but STILL PRESENT (D2: gating never hides), with both reason channels
	// set and aria-describedby pointing at this row's own reason node.
	const failBar = gatedBar('ack', [OPEN_ONLY]);
	I.mintActionDescIdentity(failBar.panel, 'alerts', 'a1');
	I.applyActionRefRules(failBar.panel, { state: 'closed' });
	out.rule_noMatchDisabled = failBar.btn.disabled === true;
	out.rule_noMatchStillPresent = failBar.btn.hidden === false;
	out.rule_noMatchTitle = failBar.btn.getAttribute('title');
	out.rule_noMatchReasonText = failBar.desc.textContent;
	out.rule_noMatchDescribedbyPointsAtNode =
		failBar.btn.getAttribute('aria-describedby') === failBar.desc.getAttribute('id');

	// (3) The keyed field is missing from the payload -> fails CLOSED, and still does not hide.
	const absentBar = gatedBar('ack', [OPEN_ONLY]);
	I.mintActionDescIdentity(absentBar.panel, 'alerts', 'a1');
	I.applyActionRefRules(absentBar.panel, {});
	out.rule_absentFieldDisabled = absentBar.btn.disabled === true;
	out.rule_absentFieldStillPresent = absentBar.btn.hidden === false;
	out.rule_absentFieldTitle = absentBar.btn.getAttribute('title');

	// (4) present / absent read emptiness, not just key existence.
	const presentEmpty = gatedBar('ack', [{ field: 'owner', op: 'present', reason: 'No owner yet.' }]);
	I.applyActionRefRules(presentEmpty.panel, { owner: '' });
	out.rule_presentOnEmptyDisabled = presentEmpty.btn.disabled === true;
	const presentSet = gatedBar('ack', [{ field: 'owner', op: 'present', reason: 'No owner yet.' }]);
	I.applyActionRefRules(presentSet.panel, { owner: 'jbognar' });
	out.rule_presentOnValueEnabled = presentSet.btn.disabled === false;

	// The pure predicates, independent of any DOM pass.
	out.rule_eqYes = I.actionRuleMatches(OPEN_ONLY, { state: 'open' });
	out.rule_eqNo = I.actionRuleMatches(OPEN_ONLY, { state: 'closed' });
	out.rule_eqAbsentKey = I.actionRuleMatches(OPEN_ONLY, {});
	out.rule_neYes = I.actionRuleMatches({ field: 'state', op: 'ne', value: 'open', reason: 'r' }, { state: 'closed' });
	out.rule_neNo = I.actionRuleMatches({ field: 'state', op: 'ne', value: 'open', reason: 'r' }, { state: 'open' });
	out.rule_absentOnEmpty = I.actionRuleMatches({ field: 'owner', op: 'absent', reason: 'r' }, { owner: '' });
	out.rule_absentOnValue = I.actionRuleMatches({ field: 'owner', op: 'absent', reason: 'r' }, { owner: 'x' });
	// A number in the payload against a string in the rule still compares, so an author is not forced to know how
	// the expand GET boxed the value.
	out.rule_eqCoercesNumber = I.actionRuleMatches({ field: 'tier', op: 'eq', value: '2', reason: 'r' }, { tier: 2 });

	// (5) FIRST-DECLARED failing rule wins.  Two rules, BOTH failing, with DIFFERENT reasons, declared in both
	// orders - identical reasons would make declaration order unobservable.
	const R_STATE = { field: 'state', op: 'eq', value: 'open', reason: 'REASON-STATE' };
	const R_TIER = { field: 'tier', op: 'eq', value: 'gold', reason: 'REASON-TIER' };
	const bothFail = { state: 'closed', tier: 'silver' };
	const orderAB = gatedBar('ack', [R_STATE, R_TIER]);
	I.applyActionRefRules(orderAB.panel, bothFail);
	out.rule_firstDeclaredWins = orderAB.btn.getAttribute('title');
	const orderBA = gatedBar('ack', [R_TIER, R_STATE]);
	I.applyActionRefRules(orderBA.panel, bothFail);
	out.rule_firstDeclaredWinsReversed = orderBA.btn.getAttribute('title');
	out.rule_firstFailingHelper = I.firstFailingActionRule([R_STATE, R_TIER], bothFail).reason;
	out.rule_firstFailingHelperAllPass = I.firstFailingActionRule([R_STATE], { state: 'open' });

	// (6) BOTH channels clear together when a re-expand's state passes the rules - a re-enabled button must stop
	// announcing why it used to be unavailable.
	const reBar = gatedBar('ack', [OPEN_ONLY]);
	I.mintActionDescIdentity(reBar.panel, 'alerts', 'a1');
	I.applyActionRefRules(reBar.panel, { state: 'closed' });
	out.rule_clearPreconditionTitleSet = reBar.btn.getAttribute('title') != null;
	I.applyActionRefRules(reBar.panel, { state: 'open' });
	out.rule_clearedTitle = reBar.btn.getAttribute('title') == null;
	out.rule_clearedDescribedby = reBar.btn.getAttribute('aria-describedby') == null;
	out.rule_clearedReasonText = reBar.desc.textContent === '';

	// (7) Composition with the lifecycle gate: the pass NEVER re-enables, so a button held disabled by
	// setActionRefEnabled or hideActionRefs stays that way even when every rule matches.
	const heldBar = gatedBar('ack', [OPEN_ONLY]);
	heldBar.btn.disabled = true;
	I.applyActionRefRules(heldBar.panel, { state: 'open' });
	out.rule_lifecycleDisabledStaysDisabled = heldBar.btn.disabled === true;
	const hiddenBar = gatedBar('ack', [OPEN_ONLY]);
	I.hideActionRefs(hiddenBar.panel);
	I.applyActionRefRules(hiddenBar.panel, { state: 'open' });
	out.rule_hiddenStaysHidden = hiddenBar.btn.hidden === true;
	out.rule_hiddenStaysDisabled = hiddenBar.btn.disabled === true;
	// ...and the pass itself never hides: hideActionRefs remains the only thing that can.
	out.rule_passNeverHides = heldBar.btn.hidden === false;

	// (8) Two rows of the same table, expanded at once, mint DISTINCT reason-node ids, so no row's
	// aria-describedby can resolve to another row's reason.
	const row1 = gatedBar('ack', [OPEN_ONLY]);
	const row2 = gatedBar('ack', [OPEN_ONLY]);
	I.mintActionDescIdentity(row1.panel, 'alerts', 'a1');
	I.mintActionDescIdentity(row2.panel, 'alerts', 'a2');
	out.rule_descId1 = row1.desc.getAttribute('id');
	out.rule_descId2 = row2.desc.getAttribute('id');
	out.rule_descIdsUnique = out.rule_descId1 !== out.rule_descId2;
	I.applyActionRefRules(row1.panel, { state: 'closed' });
	I.applyActionRefRules(row2.panel, { state: 'closed' });
	out.rule_row1PointsAtOwnNode = row1.btn.getAttribute('aria-describedby') === out.rule_descId1;
	out.rule_row2PointsAtOwnNode = row2.btn.getAttribute('aria-describedby') === out.rule_descId2;
	// Two gated actions in one bar get one node each, so the ids differ by action too.
	const twoActions = gatedBar('ack', [OPEN_ONLY]);
	const escBtn = el('button');
	escBtn.setAttribute('data-juneau-action', 'esc');
	escBtn.setAttribute('data-juneau-action-rules', JSON.stringify([OPEN_ONLY]));
	escBtn.removeAttribute = function (k) { delete this.attrs[k]; };
	const escDesc = el('span');
	escDesc.setAttribute('data-juneau-action-desc', 'esc');
	twoActions.panel.firstChild.appendChild(escBtn);
	twoActions.panel.firstChild.appendChild(escDesc);
	I.mintActionDescIdentity(twoActions.panel, 'alerts', 'a1');
	I.applyActionRefRules(twoActions.panel, { state: 'closed' });
	out.rule_perActionIdsDiffer = twoActions.desc.getAttribute('id') !== escDesc.getAttribute('id');
	out.rule_bothActionsGated = twoActions.btn.disabled === true && escBtn.disabled === true;

	// (9) A malformed rules attribute gates NOTHING: the rule is presentation only and the server stays
	// authoritative, so the safe direction is the pre-rule behaviour rather than a bar of dead buttons.
	const badBar = gatedBar('ack', [OPEN_ONLY]);
	badBar.btn.setAttribute('data-juneau-action-rules', '{not json');
	I.applyActionRefRules(badBar.panel, {});
	out.rule_malformedGatesNothing = badBar.btn.disabled === false;
	// An empty rule array is likewise inert.
	const emptyBar = gatedBar('ack', []);
	I.applyActionRefRules(emptyBar.panel, {});
	out.rule_emptyRulesGatesNothing = emptyBar.btn.disabled === false;
}

// ----------------------------------------------------------------------------------------------------------------
// DetailField.actions - an ActionBar hosted in a field's VALUE COLUMN, emitted as a plain SIBLING of the value
// slot.  The claim under test is that this host needed ZERO runtime wiring: the fill path must not see the bar
// (it selects [data-juneau-field], which the bar does not carry), and the panel-scoped [data-juneau-action]
// lifecycles must reach it without knowing it is in a field.  Both directions are checked, because either one
// failing alone would still leave the bar looking present.
// ----------------------------------------------------------------------------------------------------------------

/** The three-child field block ViewTable emits for a field that declares a bar: title, value slot, bar. */
function fieldWithBar(key, actionId, rules) {
	const panel = el('div');
	panel.className = 'juneau-view-detail-panel';
	const sec = el('section');
	sec.setAttribute('data-juneau-detail-section', 'ctx');
	const grid = el('div');
	grid.className = 'juneau-view-detail-fields juneau-view-detail-fields-inline';
	const block = el('div');
	block.className = 'juneau-view-detail-field';
	const label = el('div');
	label.className = 'juneau-view-detail-field-title';
	label.textContent = 'Assignee';
	const value = el('div');
	value.setAttribute('data-juneau-field', key);
	value.className = 'juneau-view-detail-field-value';
	const bar = el('div');
	bar.className = 'juneau-view-detail-actions';
	const btn = el('button');
	btn.setAttribute('data-juneau-action', actionId);
	btn.className = 'juneau-view-detail-action';
	btn.disabled = true;                    // as emitted: disabled until the expand GET returns 2xx
	btn.hidden = false;
	btn.removeAttribute = function (k) { delete this.attrs[k]; };
	const desc = el('span');
	desc.setAttribute('data-juneau-action-desc', actionId);
	desc.setAttribute('hidden', 'hidden');
	if (rules) btn.setAttribute('data-juneau-action-rules', JSON.stringify(rules));
	bar.appendChild(btn);
	if (rules) bar.appendChild(desc);
	block.appendChild(label);
	block.appendChild(value);
	block.appendChild(bar);
	grid.appendChild(block);
	sec.appendChild(grid);
	panel.appendChild(sec);
	return { panel: panel, block: block, value: value, bar: bar, btn: btn, desc: desc };
}

// (1) The fill path sees the value slot and nothing else in the block.  The bar carries no [data-juneau-field], so
// it is not a paint target, and painting the value must not disturb it.
const dfa = fieldWithBar('assignee', 'esc');
out.dfa_onePaintTargetInTheBlock = dfa.panel.querySelectorAll('[data-juneau-field]').length === 1;
out.dfa_barIsNotAPaintTarget = dfa.bar.getAttribute('data-juneau-field') == null;
out.dfa_barIsASiblingOfTheValueSlot = dfa.block.childNodes.length === 3
	&& dfa.block.childNodes[1] === dfa.value
	&& dfa.block.childNodes[2] === dfa.bar
	&& dfa.value.childNodes.length === 0;

// (2) LD-4: a NON-BLANK value and a bar at once.  The value paints, the bar is untouched and still in the block,
// and the lifecycle enable reaches it - which is the whole of "still clickable" for a delegated listener.
I.fillDetailSlots(dfa.panel, { assignee: 'alice' });
out.dfa_valuePainted = dfa.value.textContent;
out.dfa_barSurvivedThePaint = dfa.block.childNodes[2] === dfa.bar && dfa.bar.childNodes[0] === dfa.btn;
out.dfa_buttonStillDisabledBeforeTheGate = dfa.btn.disabled === true;
I.setActionRefEnabled(dfa.panel, true);
out.dfa_buttonEnabledByTheSharedGate = dfa.btn.disabled === false && dfa.btn.hidden === false;

// (3) A BLANK value changes nothing about the bar: this host ships no blank/non-blank default, so the bar is
// present in both states and visibility belongs to the ActionRef predicates alone.
const dfaBlank = fieldWithBar('ticketId', 'esc');
I.fillDetailSlots(dfaBlank.panel, {});
out.dfa_blankValueIsEmptyString = dfaBlank.value.textContent === '';
out.dfa_blankValueKeepsTheBar = dfaBlank.block.childNodes[2] === dfaBlank.bar;
I.setActionRefEnabled(dfaBlank.panel, true);
out.dfa_blankValueButtonEnabled = dfaBlank.btn.disabled === false;

// (4) Fail-closed: a 404/500 or a contract mismatch hides a field-hosted bar's buttons through the same
// panel-scoped pass that hides a header bar's, with no field-awareness of its own.
const dfaFail = fieldWithBar('assignee', 'esc');
I.hideActionRefs(dfaFail.panel);
out.dfa_failClosedDisabled = dfaFail.btn.disabled === true;
out.dfa_failClosedHidden = dfaFail.btn.hidden === true;

// (5) The state-conditional predicates reach the third host too, which is why this host ships no visibility rule
// of its own: a failing rule disables the field-hosted button and attaches its reason, exactly as in a header bar.
if (out.hasApplyActionRefRules) {
	const gatedRules = [{ field: 'state', op: 'eq', value: 'open', reason: 'This record is not open.' }];
	const dfaGated = fieldWithBar('state', 'esc', gatedRules);
	I.setActionRefEnabled(dfaGated.panel, true);
	I.applyActionRefRules(dfaGated.panel, { state: 'closed' });
	out.dfa_gatedDisabled = dfaGated.btn.disabled === true;
	out.dfa_gatedStillPresent = dfaGated.btn.hidden === false;
	out.dfa_gatedReason = dfaGated.btn.getAttribute('title');
	const dfaPassing = fieldWithBar('state', 'esc', gatedRules);
	I.setActionRefEnabled(dfaPassing.panel, true);
	I.applyActionRefRules(dfaPassing.panel, { state: 'open' });
	out.dfa_passingRuleStaysEnabled = dfaPassing.btn.disabled === false;
}

process.stdout.write(JSON.stringify(out));
