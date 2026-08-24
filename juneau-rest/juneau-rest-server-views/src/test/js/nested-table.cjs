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
 * nested-table.cjs - always-on Node harness for the juneau-views.js nested read-only table helpers:
 * applyNestedScope (both ajax branches) / findNestedSidecar / prepareNestedTable fail-closed handshakes +
 * read-only clamp + parent-id stamping / teardownNestedTables / activateNestedTablesInPane +
 * initNestedTablesInVisiblePanes routing.
 *
 *   Usage:  node nested-table.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.  No jQuery/DataTables: the happy-path
 * DataTable construction is stubbed to throw so we can read the state prepareNestedTable stamps BEFORE it hands off
 * to buildTable (parent-id, init marker, read-only clamp), while the pure/DOM helpers are exercised directly.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node nested-table.cjs <juneau-views.js>');
	process.exit(2);
}

/**
 * Minimal compound CSS-selector matcher for the subset the harness needs: an optional tag prefix followed by any
 * number of `.class` and `[attr]` / `[attr="v"]` conditions (e.g. `table[data-juneau-view][data-juneau-nested-init]`).
 */
function elMatches(node, sel) {
	if (!node || node.nodeType !== 1) return false;
	const re = /\.[\w-]+|\[[\w:-]+(?:="[^"]*")?\]|^[a-zA-Z][\w-]*/g;
	let m;
	let matchedSomething = false;
	while ((m = re.exec(sel))) {
		const tok = m[0];
		matchedSomething = true;
		if (tok.charAt(0) === '.') {
			const raw = ' ' + (node.className || node.getAttribute('class') || '') + ' ';
			if (raw.indexOf(' ' + tok.slice(1) + ' ') < 0) return false;
		} else if (tok.charAt(0) === '[') {
			const am = /^\[([\w:-]+)(?:="([^"]*)")?\]$/.exec(tok);
			const v = node.getAttribute(am[1]);
			if (am[2] == null ? v == null : v !== am[2]) return false;
		} else {
			if (node.tagName !== tok.toUpperCase()) return false;
		}
	}
	return matchedSomething;
}

function elWalk(node, sel, acc) {
	for (let i = 0; i < node.childNodes.length; i++) {
		const c = node.childNodes[i];
		if (c.nodeType === 1) {
			if (elMatches(c, sel)) acc.push(c);
			elWalk(c, sel, acc);
		}
	}
	return acc;
}

function el(tag) {
	const node = {
		nodeType: 1,
		tagName: String(tag).toUpperCase(),
		childNodes: [],
		attrs: {},
		className: '',
		parentNode: null,
		style: {},
		_listeners: {},
		get firstChild() { return this.childNodes[0] || null; },
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		setAttribute: function (k, v) { this.attrs[k] = v == null ? '' : String(v); },
		removeAttribute: function (k) { delete this.attrs[k]; },
		hasAttribute: function (k) { return Object.hasOwn(this.attrs, k); },
		appendChild: function (c) { this.childNodes.push(c); c.parentNode = this; return c; },
		insertBefore: function (c, ref) {
			const i = ref ? this.childNodes.indexOf(ref) : -1;
			if (i < 0) this.childNodes.push(c); else this.childNodes.splice(i, 0, c);
			c.parentNode = this;
			return c;
		},
		removeChild: function (c) { const i = this.childNodes.indexOf(c); if (i >= 0) this.childNodes.splice(i, 1); return c; },
		createCaption: function () { return null; },   // force renderBanner down the document.createElement path
		querySelectorAll: function (sel) { return elWalk(this, sel, []); },
		querySelector: function (sel) { const r = elWalk(this, sel, []); return r.length ? r[0] : null; },
		closest: function (sel) { let n = this; while (n && n.nodeType === 1) { if (elMatches(n, sel)) return n; n = n.parentNode; } return null; },
		addEventListener: function (type, fn) { (this._listeners[type] = this._listeners[type] || []).push(fn); },
		focus: function () {},
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
	return { nodeType: 3, nodeValue: value == null ? '' : String(value), childNodes: [], get textContent() { return this.nodeValue; } };
}

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
const window = { document: document, console: console, jQuery: undefined };
const sandbox = { window: window, document: document, console: console };
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.prepareNestedTable === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

out.nestedContractVersion = I.JUNEAU_NESTED_CONTRACT_VERSION;
out.hasApplyNestedScope = typeof I.applyNestedScope === 'function';
out.hasFindNestedSidecar = typeof I.findNestedSidecar === 'function';
out.hasActivate = typeof I.activateNestedTablesInPane === 'function';
out.hasInitVisible = typeof I.initNestedTablesInVisiblePanes === 'function';
out.hasTeardown = typeof I.teardownNestedTables === 'function';

// ----------------------------------------------------------------------------------------------------------------
// applyNestedScope - merges ONE parent-scope param into opts.ajax.data in BOTH data modes.
// ----------------------------------------------------------------------------------------------------------------

// Server-mode-like: a pre-existing data fn (ribbon merge) is preserved AND the scope param added.
const optsS = { ajax: { data: function (d) { d.ribbon = 'r'; return d; } } };
I.applyNestedScope(optsS, { param: 'parentId', parentId: function () { return 'a1'; } });
const ds = optsS.ajax.data({});
out.scope_server_ribbonKept = ds.ribbon === 'r';
out.scope_server_paramAdded = ds.parentId === 'a1';

// Client-mode-like: no pre-existing data fn -> a data fn is installed that adds the scope param.
const optsC = { ajax: { dataSrc: '' } };
I.applyNestedScope(optsC, { param: 'parentId', parentId: 'a1' });
out.scope_client_paramAdded = optsC.ajax.data({}).parentId === 'a1';

// Blank/absent parent id contributes NOTHING (unscoped request rather than an empty param).
const optsB = { ajax: {} };
I.applyNestedScope(optsB, { param: 'parentId', parentId: '' });
out.scope_blank_absent = !Object.hasOwn(optsB.ajax.data({}), 'parentId');

// The getter is read at REQUEST time, so a re-parented nested table stays correct.
let pid = 'a1';
const optsG = { ajax: {} };
I.applyNestedScope(optsG, { param: 'p', parentId: function () { return pid; } });
const gFirst = optsG.ajax.data({}).p;
pid = 'b2';
const gSecond = optsG.ajax.data({}).p;
out.scope_getter_first = gFirst;
out.scope_getter_second = gSecond;

// Custom scope-param name is honored.
const optsN = { ajax: {} };
I.applyNestedScope(optsN, { param: 'alertId', parentId: 'a1' });
out.scope_customName = optsN.ajax.data({}).alertId === 'a1';

// ----------------------------------------------------------------------------------------------------------------
// buildOptions integration - the scope param rides both real ajax branches.
// ----------------------------------------------------------------------------------------------------------------

const serverView = { contractVersion: '4', id: 'events', dataMode: 'server', dataUrl: '/d', columns: [{ data: 'when' }] };
const optsSv = I.buildOptions(serverView, {
	ribbonParams: function () { return { rb: '1' }; },
	nestedScope: { param: 'parentId', parentId: function () { return 'a1'; } }
});
const dsv = optsSv.ajax.data({});
out.bo_server_serverSide = optsSv.serverSide === true;
out.bo_server_ribbon = dsv.rb === '1';
out.bo_server_scope = dsv.parentId === 'a1';

const clientView = { contractVersion: '4', id: 'events', dataMode: 'client', dataUrl: '/d', columns: [{ data: 'when' }] };
const optsCv = I.buildOptions(clientView, { nestedScope: { param: 'parentId', parentId: 'a1' } });
out.bo_client_serverSide = optsCv.serverSide === false;
out.bo_client_dataSrc = optsCv.ajax.dataSrc === '';
out.bo_client_scope = optsCv.ajax.data({}).parentId === 'a1';

// A top-level view (no nestedScope) merges no parent param: server keeps only its plain data fn; client gets none.
const optsTop = I.buildOptions(clientView, {});
out.bo_top_client_noDataFn = typeof optsTop.ajax.data !== 'function';

// ----------------------------------------------------------------------------------------------------------------
// findNestedSidecar - sibling [data-juneau-nested-meta] matched by author view id (id-less clone-safe lookup).
// ----------------------------------------------------------------------------------------------------------------

const VALID = JSON.stringify({
	contractVersion: '4', id: 'events', dataMode: 'client', dataUrl: '/data/events',
	columns: [{ data: 'when' }, { data: 'what' }],
	rowActions: ['x'], columnConfig: { a: 1 }, pollIntervalMs: 9999, details: { z: 1 }
});

function nestedWrapper(o) {
	o = o || {};
	const wrap = el('div');
	wrap.className = 'juneau-view-detail-nested';
	wrap.setAttribute('data-juneau-nested', '1');
	wrap.setAttribute('data-juneau-nested-contract', o.contract != null ? o.contract : '1');
	wrap.setAttribute('data-juneau-nested-scope-param', o.scopeParam || 'parentId');
	const table = el('table');
	table.setAttribute('data-juneau-view', o.viewId || 'events');
	if (o.inited) table.setAttribute('data-juneau-nested-init', '1');
	wrap.appendChild(table);
	if (!o.noSidecar) {
		const sc = el('script');
		sc.setAttribute('data-juneau-nested-meta', o.metaId != null ? o.metaId : (o.viewId || 'events'));
		sc.textContent = o.json != null ? o.json : VALID;
		wrap.appendChild(sc);
	}
	return { wrap: wrap, table: table };
}

const fnsW = nestedWrapper({ viewId: 'events' });
out.find_byId = I.findNestedSidecar(fnsW.wrap, 'events') != null
	&& I.findNestedSidecar(fnsW.wrap, 'events').getAttribute('data-juneau-nested-meta') === 'events';
// id skew -> falls back to first meta node (present, not silent no-init).
const skewW = nestedWrapper({ viewId: 'events', metaId: 'other' });
out.find_fallbackFirst = I.findNestedSidecar(skewW.wrap, 'events') != null;
const noneW = nestedWrapper({ viewId: 'events', noSidecar: true });
out.find_noneNull = I.findNestedSidecar(noneW.wrap, 'events') === null;

// ----------------------------------------------------------------------------------------------------------------
// prepareNestedTable - fail-closed handshakes (all observable without jQuery via the in-table banner).
// ----------------------------------------------------------------------------------------------------------------

function bannerText(table) {
	const cap = table.querySelector('.juneau-view-error');
	return cap ? cap.textContent : null;
}
function inited(table) { return table.getAttribute('data-juneau-nested-init') === '1'; }

// Shell-contract mismatch -> banner, no init, no ctx.
const badContract = nestedWrapper({ contract: '2' });
I.prepareNestedTable(badContract.wrap, 'a1');
out.pnt_contractMismatch_banner = /contract version mismatch/.test(String(bannerText(badContract.table)));
out.pnt_contractMismatch_noInit = !inited(badContract.table);
out.pnt_contractMismatch_noCtx = badContract.table.__juneauCtx == null;

// Malformed sidecar JSON -> banner, no init.
const badJson = nestedWrapper({ json: '{not json' });
I.prepareNestedTable(badJson.wrap, 'a1');
out.pnt_malformed_banner = /malformed/.test(String(bannerText(badJson.table)));
out.pnt_malformed_noInit = !inited(badJson.table);

// View-contract mismatch (sidecar contractVersion != runtime) -> banner, no init.
const badView = nestedWrapper({ json: JSON.stringify({ contractVersion: '3', id: 'events', dataMode: 'client', dataUrl: '/d', columns: [{ data: 'when' }] }) });
I.prepareNestedTable(badView.wrap, 'a1');
out.pnt_viewContract_banner = /view contract version mismatch/.test(String(bannerText(badView.table)));
out.pnt_viewContract_noInit = !inited(badView.table);

// Missing sidecar -> no init, no banner (logged only).
const noSc = nestedWrapper({ noSidecar: true });
I.prepareNestedTable(noSc.wrap, 'a1');
out.pnt_noSidecar_noInit = !inited(noSc.table);
out.pnt_noSidecar_noBanner = bannerText(noSc.table) == null;

// Valid everything but no jQuery -> warn + bail BEFORE stamping (no init, no ctx).
const noJq = nestedWrapper({});
I.prepareNestedTable(noJq.wrap, 'a1');
out.pnt_noJq_noInit = !inited(noJq.table);
out.pnt_noJq_noCtx = noJq.table.__juneauCtx == null;

// ----------------------------------------------------------------------------------------------------------------
// prepareNestedTable - happy path up to hand-off: stamp parent-id + init marker + read-only clamp + scope seam.
// DataTable construction is stubbed to throw; we read the state stamped BEFORE buildTable.
// ----------------------------------------------------------------------------------------------------------------

window.jQuery = function () { return { DataTable: function () { throw new Error('stub: no real DataTables'); } }; };
window.jQuery.fn = { DataTable: function () {} };   // truthy so the jQuery/buildTable gates pass

const okW = nestedWrapper({ viewId: 'events', scopeParam: 'alertId' });
let threw = false;
try { I.prepareNestedTable(okW.wrap, 'a1'); } catch (e) { threw = true; }
out.pnt_ok_construction_attempted = threw;   // proves it reached buildTable -> $(table).DataTable
out.pnt_ok_initMarked = inited(okW.table);
out.pnt_ok_parentStamped = okW.table.getAttribute('data-juneau-parent-id') === 'a1';
const okCtx = okW.table.__juneauCtx;
out.pnt_ok_hasCtx = okCtx != null;
out.pnt_ok_nested = !!(okCtx && okCtx.nested);
out.pnt_ok_scopeParam = okCtx && okCtx.nestedScope ? okCtx.nestedScope.param : null;
out.pnt_ok_scopeReadsAttr = okCtx && okCtx.nestedScope ? okCtx.nestedScope.parentId() : null;
// Read-only clamp: forbidden fields nulled before build (defensive; server already rejects them).
out.pnt_ok_clampRowActions = okCtx && okCtx.viewDef.rowActions === null;
out.pnt_ok_clampColumnConfig = okCtx && okCtx.viewDef.columnConfig === null;
out.pnt_ok_clampPoll = okCtx && okCtx.viewDef.pollIntervalMs === null;
out.pnt_ok_clampDetails = okCtx && okCtx.viewDef.details === null;
// Idempotent: a second call on an already-inited table is a no-op (no second construction throw).
let threw2 = false;
try { I.prepareNestedTable(okW.wrap, 'a1'); } catch (e) { threw2 = true; }
out.pnt_ok_idempotent = threw2 === false;

window.jQuery = undefined;   // back to no-jQuery for the routing tests below

// ----------------------------------------------------------------------------------------------------------------
// teardownNestedTables - destroys every inited nested DataTable in a subtree, clears the marker.
// ----------------------------------------------------------------------------------------------------------------

function initedTableWithSpy(root) {
	const wrap = el('div');
	wrap.setAttribute('data-juneau-nested', '1');
	const table = el('table');
	table.setAttribute('data-juneau-view', 'events');
	table.setAttribute('data-juneau-nested-init', '1');
	let destroyed = 0;
	table.__juneauCtx = { dataTable: { destroy: function () { destroyed++; } }, _pollTimers: [], _jobSources: new Set() };
	wrap.appendChild(table);
	root.appendChild(wrap);
	return { table: table, destroyed: function () { return destroyed; } };
}
const tdRoot = el('div');
const t1 = initedTableWithSpy(tdRoot);
const t2 = initedTableWithSpy(tdRoot);
// A non-inited nested table must be left untouched.
const plainWrap = el('div'); plainWrap.setAttribute('data-juneau-nested', '1');
const plainTable = el('table'); plainTable.setAttribute('data-juneau-view', 'events');
plainWrap.appendChild(plainTable); tdRoot.appendChild(plainWrap);

I.teardownNestedTables(tdRoot);
out.td_destroyed1 = t1.destroyed() === 1;
out.td_destroyed2 = t2.destroyed() === 1;
out.td_markerCleared1 = t1.table.getAttribute('data-juneau-nested-init') == null;
out.td_ctxNulled1 = t1.table.__juneauCtx == null;
out.td_plainUntouched = plainTable.__juneauCtx == null && plainTable.getAttribute('data-juneau-nested-init') == null;

// ----------------------------------------------------------------------------------------------------------------
// Routing: initNestedTablesInVisiblePanes skips hidden panes; activateNestedTablesInPane inits or re-measures.
// prepareNestedTable is observed via its fail-loud banner (mismatched contract -> banner iff it was invoked).
// ----------------------------------------------------------------------------------------------------------------

function paneWithNested(sid, hidden, contract) {
	const sec = el('section');
	sec.setAttribute('data-juneau-detail-section', sid);
	sec.hidden = !!hidden;
	const nw = nestedWrapper({ contract: contract != null ? contract : '2' });   // mismatched -> banner on invoke
	sec.appendChild(nw.wrap);
	return { sec: sec, table: nw.table };
}

const panel = el('div');
panel.className = 'juneau-view-detail-panel';
const visPane = paneWithNested('a', false);
const hidPane = paneWithNested('b', true);
panel.appendChild(visPane.sec);
panel.appendChild(hidPane.sec);

I.initNestedTablesInVisiblePanes(panel, 'a1');
out.route_visibleInvoked = bannerText(visPane.table) != null;    // visible pane's nested table was prepared
out.route_hiddenSkipped = bannerText(hidPane.table) == null;     // hidden pane's nested table was NOT prepared

// Now activate the hidden pane -> its nested table gets prepared (banner appears).
I.activateNestedTablesInPane(hidPane.sec, 'a1');
out.route_activateHiddenNow = bannerText(hidPane.table) != null;

// activateNestedTablesInPane on an ALREADY-inited nested table calls columns.adjust(), not prepareNestedTable.
const adjPane = el('section');
adjPane.setAttribute('data-juneau-detail-section', 'c');
const adjWrap = el('div'); adjWrap.setAttribute('data-juneau-nested', '1');
const adjTable = el('table');
adjTable.setAttribute('data-juneau-view', 'events');
adjTable.setAttribute('data-juneau-nested-init', '1');   // already inited
let adjusted = 0;
adjTable.__juneauCtx = { dataTable: { columns: { adjust: function () { adjusted++; } } } };
adjWrap.appendChild(adjTable);
adjPane.appendChild(adjWrap);
I.activateNestedTablesInPane(adjPane, 'a1');
out.route_adjustCalled = adjusted === 1;
out.route_adjustNoBanner = bannerText(adjTable) == null;   // not re-prepared

process.stdout.write(JSON.stringify(out));
