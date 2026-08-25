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
 * nested-table.cjs - always-on Node harness for the juneau-views.js nested table helpers: applyNestedScope (both
 * ajax branches) / findNestedSidecar / nestedTableDepth + the depth cap / prepareNestedTable fail-closed handshakes,
 * parent-only clamp, live selection state and parent init path / mintNestedIdentity per-row identity /
 * depth-first teardownNestedTables / activateNestedTablesInPane + initNestedTablesInVisiblePanes routing /
 * the table-ownership helpers that keep a parent from wiring itself to a nested table's rows.
 *
 *   Usage:  node nested-table.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.  No jQuery/DataTables: the happy-path
 * DataTable construction is stubbed to throw so we can read the state prepareNestedTable stamps BEFORE it hands off
 * to buildTable (parent-id, init marker, clamp, ctx, bound listeners), while the pure/DOM helpers are exercised
 * directly.
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
function elMatchesCompound(node, sel) {
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

/**
 * The above plus the descendant combinator (`thead tr`, `tbody tr[data-juneau-row-id]`): the rightmost compound must
 * match `node` and each compound to its left must match some ancestor, in order.
 */
function elMatches(node, sel) {
	const parts = String(sel).trim().split(/\s+/);
	if (!elMatchesCompound(node, parts[parts.length - 1])) return false;
	let n = node.parentNode;
	for (let i = parts.length - 2; i >= 0; i--) {
		while (n && n.nodeType === 1 && !elMatchesCompound(n, parts[i])) n = n.parentNode;
		if (!n || n.nodeType !== 1) return false;
		n = n.parentNode;
	}
	return true;
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
// teardownTable clears poll timers and closes job streams; record both so "zero live handles after teardown" is
// observable rather than swallowed by teardownNestedTables' defensive catch.
const clearedIntervals = [];
const sandbox = {
	window: window, document: document, console: console,
	setInterval: function () { return 0; },
	clearInterval: function (id) { clearedIntervals.push(id); }
};
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.prepareNestedTable === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

out.nestedContractVersion = I.JUNEAU_NESTED_CONTRACT_VERSION;
out.maxNestedDepth = I.MAX_NESTED_DEPTH;
out.hasApplyNestedScope = typeof I.applyNestedScope === 'function';
out.hasFindNestedSidecar = typeof I.findNestedSidecar === 'function';
out.hasActivate = typeof I.activateNestedTablesInPane === 'function';
out.hasInitVisible = typeof I.initNestedTablesInVisiblePanes === 'function';
out.hasTeardown = typeof I.teardownNestedTables === 'function';
out.hasMint = typeof I.mintNestedIdentity === 'function';
out.hasDepth = typeof I.nestedTableDepth === 'function';

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
	rowActions: [{ id: 'ack', label: 'Ack', method: 'POST', endpoint: '/data/events/{id}/ack' }],
	columnConfig: { a: 1 }, pollIntervalMs: 9999, details: { endpoint: '/data/events/{id}' }
});

/** A mismatched shell-contract value - deliberately not the real one, whatever the real one currently is. */
const WRONG_CONTRACT = '9';

function nestedWrapper(o) {
	o = o || {};
	const wrap = el('div');
	wrap.className = 'juneau-view-detail-nested';
	wrap.setAttribute('data-juneau-nested', '1');
	wrap.setAttribute('data-juneau-nested-contract', o.contract != null ? o.contract : I.JUNEAU_NESTED_CONTRACT_VERSION);
	wrap.setAttribute('data-juneau-nested-scope-param', o.scopeParam || 'parentId');
	const table = el('table');
	table.setAttribute('data-juneau-view', o.viewId || 'events');
	if (o.inited) table.setAttribute('data-juneau-nested-init', '1');
	if (o.selection) {
		table.setAttribute('data-juneau-select', '1');
		table.setAttribute('data-juneau-row-id-field', o.rowIdField || 'id');
		table.setAttribute('data-juneau-select-all', '1');
	}
	if (o.csrf) table.setAttribute('data-juneau-csrf', o.csrf);
	wrap.appendChild(table);
	if (o.detailTemplate) {
		const tpl = el('template');
		tpl.setAttribute('data-juneau-row-detail', '1');
		tpl.setAttribute('data-juneau-detail-url', '/data/events/{id}');
		tpl.content = el('div');
		wrap.appendChild(tpl);
	}
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
const badContract = nestedWrapper({ contract: WRONG_CONTRACT });
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
// prepareNestedTable - happy path up to hand-off: stamp parent-id + init marker + parent-only clamp + scope seam +
// the parent init path.  DataTable construction is stubbed to throw; we read the state stamped BEFORE buildTable.
// ----------------------------------------------------------------------------------------------------------------

window.jQuery = function () { return { DataTable: function () { throw new Error('stub: no real DataTables'); } }; };
window.jQuery.fn = { DataTable: function () {} };   // truthy so the jQuery/buildTable gates pass

const okW = nestedWrapper({ viewId: 'events', scopeParam: 'alertId', selection: true, detailTemplate: true });
let threw = false;
try { I.prepareNestedTable(okW.wrap, 'a1'); } catch (e) { threw = true; }
out.pnt_ok_construction_attempted = threw;   // proves it reached buildTable -> $(table).DataTable
out.pnt_ok_initMarked = inited(okW.table);
out.pnt_ok_parentStamped = okW.table.getAttribute('data-juneau-parent-id') === 'a1';
const okCtx = okW.table.__juneauCtx;
out.pnt_ok_hasCtx = okCtx != null;
out.pnt_ok_nested = !!(okCtx && okCtx.nested);
out.pnt_ok_depth = okCtx ? okCtx.nestedDepth : null;
out.pnt_ok_scopeParam = okCtx && okCtx.nestedScope ? okCtx.nestedScope.param : null;
out.pnt_ok_scopeReadsAttr = okCtx && okCtx.nestedScope ? okCtx.nestedScope.parentId() : null;
// Parent-only clamp: the chooser and the poll timer stay on the enclosing table...
out.pnt_ok_clampColumnConfig = !!okCtx && okCtx.viewDef.columnConfig === null;
out.pnt_ok_clampPoll = !!okCtx && okCtx.viewDef.pollIntervalMs === null;
// ...while row actions and detail sections survive at depth 2 (nulling them would make the server widening a no-op).
out.pnt_ok_keepsRowActions = !!okCtx && Array.isArray(okCtx.viewDef.rowActions) && okCtx.viewDef.rowActions.length === 1;
out.pnt_ok_keepsDetails = !!okCtx && okCtx.viewDef.details != null;
// Live selection state, read from the stamped attributes exactly as a root table reads them (never hardcoded null).
out.pnt_ok_selectionLive = !!(okCtx && okCtx.selectionState);
out.pnt_ok_selectionRowIdField = okCtx && okCtx.selectionState ? okCtx.selectionState.rowIdField : null;
out.pnt_ok_selectionEmpty = !!(okCtx && okCtx.selectionState && okCtx.selectionState.selected.size === 0);
// Bulk mutation is NOT part of the nested init path (it stays bound to the enclosing table's id).
out.pnt_ok_noBulkDef = !!okCtx && okCtx._bulkDef === undefined;
// The parent init path really ran: details expander + cell popover + row actions bound click listeners, row actions
// also bound keydown, and selection bound change.
out.pnt_ok_clickListeners = (okW.table._listeners.click || []).length;
out.pnt_ok_keydownListeners = (okW.table._listeners.keydown || []).length;
out.pnt_ok_changeListeners = (okW.table._listeners.change || []).length;
out.pnt_ok_popoverBound = okW.table._juneauCellPopoverBound === true;
out.pnt_ok_detailInflight = !!(okCtx && okCtx._detailInflight);
// Idempotent: a second call on an already-inited table is a no-op (no second construction throw).
let threw2 = false;
try { I.prepareNestedTable(okW.wrap, 'a1'); } catch (e) { threw2 = true; }
out.pnt_ok_idempotent = threw2 === false;

// A nested table with no selection stamp gets no selection state and binds no change listener.
const noSelW = nestedWrapper({ viewId: 'events' });
try { I.prepareNestedTable(noSelW.wrap, 'a1'); } catch (e) { /* stubbed construction */ }
out.pnt_noSel_selectionNull = noSelW.table.__juneauCtx.selectionState === null;
out.pnt_noSel_noChangeListener = (noSelW.table._listeners.change || []).length === 0;

// ----------------------------------------------------------------------------------------------------------------
// Depth cap - the root table is depth 1, a nested wrapper is depth 2, a wrapper inside a wrapper is depth 3 and is
// refused in JS exactly as the server's validate() refuses it.
// ----------------------------------------------------------------------------------------------------------------

const d2 = nestedWrapper({ viewId: 'events' });
out.depth_loneWrapperIsTwo = I.nestedTableDepth(d2.wrap) === 2;

// Depth 3: a nested shell cloned into a depth-2 table's OWN detail panel.
const outerW = nestedWrapper({ viewId: 'events' });
const innerPanel = el('div');
innerPanel.className = 'juneau-view-detail-panel';
const d3 = nestedWrapper({ viewId: 'hosts' });
innerPanel.appendChild(d3.wrap);
outerW.wrap.appendChild(innerPanel);
out.depth_nestedInNestedIsThree = I.nestedTableDepth(d3.wrap) === 3;

I.prepareNestedTable(d3.wrap, 'a1');
out.depth_threeRefused_banner = /exceeds the maximum/.test(String(bannerText(d3.table)));
out.depth_threeRefused_noInit = !inited(d3.table);
out.depth_threeRefused_noCtx = d3.table.__juneauCtx == null;

// ----------------------------------------------------------------------------------------------------------------
// mintNestedIdentity - per-expanded-row DOM identity, so two rows of the same parent never collide, and a nested
// sidecar can never shadow a page-level juneau-view:<id> lookup for a root table sharing the author id.
// ----------------------------------------------------------------------------------------------------------------

function panelWithNested(viewId) {
	const p = el('div');
	p.className = 'juneau-view-detail-panel';
	const nw = nestedWrapper({ viewId: viewId });
	p.appendChild(nw.wrap);
	return { panel: p, wrap: nw.wrap, table: nw.table };
}

const rowA = panelWithNested('events');
const rowB = panelWithNested('events');
I.mintNestedIdentity(rowA.panel, 'a1', 2);
I.mintNestedIdentity(rowB.panel, 'a2', 2);
const idA = rowA.table.getAttribute('id');
const idB = rowB.table.getAttribute('id');
out.mint_tableIdA = idA;
out.mint_tableIdB = idB;
out.mint_tableIdsUnique = idA !== idB;
const scA = I.findNestedSidecar(rowA.wrap, 'events').getAttribute('id');
const scB = I.findNestedSidecar(rowB.wrap, 'events').getAttribute('id');
out.mint_sidecarIdA = scA;
out.mint_sidecarIdsUnique = scA !== scB;
// Never the bare page-level id: a page sibling's document.getElementById("juneau-view:events") still resolves to
// the page's own sidecar, so a shared data-juneau-view cannot cross-wire the two.
out.mint_sidecarNotBarePageId = scA !== 'juneau-view:events' && scB !== 'juneau-view:events';
// The author id on the wire is untouched (it is the ViewDef.id, not an identity).
out.mint_authorIdKept = rowA.table.getAttribute('data-juneau-view') === 'events';

// ----------------------------------------------------------------------------------------------------------------
// Two rows of the same parent expanded simultaneously: both init, each against its OWN parent row, and redrawing
// one touches only its own DataTables instance.
// ----------------------------------------------------------------------------------------------------------------

const sim1 = panelWithNested('events');
const sim2 = panelWithNested('events');
try { I.prepareNestedTable(sim1.wrap, 'a1'); } catch (e) { /* stubbed construction */ }
try { I.prepareNestedTable(sim2.wrap, 'a2'); } catch (e) { /* stubbed construction */ }
const c1 = sim1.table.__juneauCtx;
const c2 = sim2.table.__juneauCtx;
out.sim_bothInited = inited(sim1.table) && inited(sim2.table);
out.sim_distinctCtx = !!c1 && !!c2 && c1 !== c2;
out.sim_distinctViewDefs = !!c1 && !!c2 && c1.viewDef !== c2.viewDef;
out.sim_scope1 = c1 ? c1.nestedScope.parentId() : null;
out.sim_scope2 = c2 ? c2.nestedScope.parentId() : null;
let reload1 = 0;
let reload2 = 0;
c1.dataTable = { ajax: { reload: function () { reload1++; } } };
c2.dataTable = { ajax: { reload: function () { reload2++; } } };
c1.redraw();
out.sim_redrawIsolated = reload1 === 1 && reload2 === 0;

window.jQuery = undefined;   // back to no-jQuery for the routing tests below

// ----------------------------------------------------------------------------------------------------------------
// Table ownership - a depth-2 nested table lives inside its parent's child row, so the parent's delegated listeners
// and row scans must not claim it.
// ----------------------------------------------------------------------------------------------------------------

const ownWrapper = el('div');
const parentTable = el('table');
parentTable.setAttribute('data-juneau-view', 'alerts');
ownWrapper.appendChild(parentTable);
const pBody = el('tbody');
parentTable.appendChild(pBody);
const pRow = el('tr');
pRow.setAttribute('data-juneau-row-id', 'a1');
pBody.appendChild(pRow);
// The child row holding the expanded panel, with a nested table of its own.
const childRow = el('tr');
pBody.appendChild(childRow);
const childCell = el('td');
childRow.appendChild(childCell);
const ownPanel = panelWithNested('events');
childCell.appendChild(ownPanel.panel);
const nBody = el('tbody');
ownPanel.table.appendChild(nBody);
const nRow = el('tr');
nRow.setAttribute('data-juneau-row-id', 'n1');
nBody.appendChild(nRow);
const nCheckbox = el('input');
nRow.appendChild(nCheckbox);

out.own_parentRowsExcludeNested = I.ownRowsWithId(parentTable).length === 1
	&& I.ownRowsWithId(parentTable)[0] === pRow;
out.own_nestedRowsAreItsOwn = I.ownRowsWithId(ownPanel.table).length === 1
	&& I.ownRowsWithId(ownPanel.table)[0] === nRow;
out.own_owningTableOfNestedNode = I.owningViewTable(nCheckbox) === ownPanel.table;
out.own_parentIgnoresNestedEvent = I.isOwnTableEvent(parentTable, { target: nCheckbox }) === false;
out.own_nestedClaimsItsOwnEvent = I.isOwnTableEvent(ownPanel.table, { target: nCheckbox }) === true;
out.own_parentClaimsItsOwnEvent = I.isOwnTableEvent(parentTable, { target: pRow }) === true;

// findRowDetailTemplate must skip a nested view's template cloned into one of this table's expanded panels and
// return the table's OWN sibling template.
const parentTpl = el('template');
parentTpl.setAttribute('data-juneau-row-detail', '1');
ownWrapper.appendChild(parentTpl);
const nestedTpl = el('template');
nestedTpl.setAttribute('data-juneau-row-detail', '1');
ownPanel.wrap.appendChild(nestedTpl);
out.own_parentTemplateNotTheNestedOne = I.findRowDetailTemplate(parentTable) === parentTpl;
out.own_nestedTemplateIsItsOwn = I.findRowDetailTemplate(ownPanel.table) === nestedTpl;

// ----------------------------------------------------------------------------------------------------------------
// teardownNestedTables - destroys every inited nested DataTable in a subtree, clears the marker.
// ----------------------------------------------------------------------------------------------------------------

const destroyOrder = [];

function initedTableWithSpy(root, label) {
	const wrap = el('div');
	wrap.setAttribute('data-juneau-nested', '1');
	const table = el('table');
	table.setAttribute('data-juneau-view', 'events');
	table.setAttribute('data-juneau-nested-init', '1');
	let destroyed = 0;
	let closed = 0;
	const timerId = 'timer-' + (label || 'x');
	table.__juneauCtx = {
		viewDef: {},
		dataTable: { destroy: function () { destroyed++; destroyOrder.push(label); } },
		_pollTimers: [timerId],
		_jobSources: new Set([{ close: function () { closed++; } }])
	};
	wrap.appendChild(table);
	root.appendChild(wrap);
	return {
		table: table, wrap: wrap, timerId: timerId,
		destroyed: function () { return destroyed; },
		closed: function () { return closed; }
	};
}
const tdRoot = el('div');
const t1 = initedTableWithSpy(tdRoot, 't1');
const t2 = initedTableWithSpy(tdRoot, 't2');
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
// Zero live handles afterwards: every poll timer cleared, every job stream closed.
out.td_timersCleared = clearedIntervals.indexOf(t1.timerId) >= 0 && clearedIntervals.indexOf(t2.timerId) >= 0;
out.td_streamsClosed = t1.closed() === 1 && t2.closed() === 1;

// Depth-first: a table inside another table's open detail panel is destroyed BEFORE the table that holds it.
const dfRoot = el('div');
const outerT = initedTableWithSpy(dfRoot, 'outer');
const outerPanel = el('div');
outerPanel.className = 'juneau-view-detail-panel';
outerT.table.appendChild(outerPanel);
const innerT = initedTableWithSpy(outerPanel, 'inner');
destroyOrder.length = 0;
I.teardownNestedTables(dfRoot);
out.td_depthFirstOrder = destroyOrder.join(',');
out.td_depthFirstBothDestroyed = outerT.destroyed() === 1 && innerT.destroyed() === 1;

// ----------------------------------------------------------------------------------------------------------------
// Routing: initNestedTablesInVisiblePanes skips hidden panes; activateNestedTablesInPane inits or re-measures.
// prepareNestedTable is observed via its fail-loud banner (mismatched contract -> banner iff it was invoked).
// ----------------------------------------------------------------------------------------------------------------

function paneWithNested(sid, hidden, contract) {
	const sec = el('section');
	sec.setAttribute('data-juneau-detail-section', sid);
	sec.hidden = !!hidden;
	const nw = nestedWrapper({ contract: contract != null ? contract : WRONG_CONTRACT });   // mismatch -> banner on invoke
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
