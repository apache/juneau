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
 * row-detail-tabs.cjs - always-on Node harness for tabbed row details: the template branch, inline and bean
 * expand, the sidecar append and the per-kind panes.  Every assertion lives in the paired Java test.
 *
 *   Usage:  node row-detail-tabs.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js>
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const [rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath] = process.argv.slice(2);
if (!helpersJsPath) {
	console.error('usage: node row-detail-tabs.cjs <renders> <views> <regions> <helpers>');
	process.exit(2);
}

const out = {};

function tabsOf(panel) { return panel.querySelectorAll('[role="tab"]'); }
function textsOf(nodes) { return nodes.map(n => n.textContent); }

/** Builds a table with a tabbed detail template and expands row `rowId` through the production click path. */
async function scenario(detail, opts) {
	opts = opts || {};
	const { env, NS, I } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath });
	const fetches = [];
	const signals = [];
	env.setFetch(function (url, init) {
		fetches.push(String(url));
		signals.push(init && init.signal);
		if (opts.hang && String(url).includes('/tab/')) return new Promise(function () {});
		return Promise.resolve({
			ok: true, status: 200,
			headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify(opts.response || { contractVersion: '1', fields: {} })); }
		});
	});
	const host = env.el('div');
	const table = env.el('table');
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	env.body.appendChild(host);

	const tpl = I.buildDetailTemplate(detail, table);
	tpl.content = {
		cloneNode: function () {
			const wrap = env.el('div');
			I.buildDetailTemplate(detail, table).content.childNodes.slice().forEach(function (k) { wrap.appendChild(k); });
			return wrap;
		}
	};
	host.appendChild(tpl);

	const rowData = opts.rowData || { id: 's-7', name: 'Rex', hidden: 'secret' };
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = 's-7';
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);

	const state = { shown: false, panel: null };
	const child = function (panel) {
		state.panel = panel;
		return {
			show: function () {
				const childTr = env.el('tr');
				const childTd = env.el('td');
				childTr.appendChild(childTd);
				childTd.appendChild(panel);
				tbody.appendChild(childTr);
				state.shown = true;
			},
			hide: function () { state.shown = false; }
		};
	};
	child.isShown = function () { return state.shown; };
	child.hide = function () { state.shown = false; };
	const ctx = {
		viewDef: { id: 'v1', columns: opts.columns || [{ data: 'name', title: 'Name' }] },
		dataTable: { row: function () { return { length: 1, child: child, data: function () { return rowData; } }; } }
	};
	I.initDetailsExpander(table, ctx, ctx.viewDef);
	table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);
	return { env, NS, I, fetches, signals, panel: state.panel, tpl, ctx, table, tr, td, click: function () { table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} }); } };
}

(async function main() {
	// --- untabbed detail: no tab shell, no mode marker
	{
		const { I, env } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath });
		const t = I.buildDetailTemplate({ endpoint: '/x/{id}', title: 'T' }, env.el('table'));
		out.untabbed_noShell = t.content.querySelectorAll('[data-juneau-detail-tabs]').length === 0 && t.dataset.juneauDetailMode === undefined;
	}

	// --- inline mode: no fetch, tabs painted from row data (hidden columns included)
	const inline = await scenario({
		mode: 'inline', title: 'Pet {id}',
		tabs: [
			{ id: 'a', kind: 'fields', title: 'Alpha', fields: ['name', 'hidden'], groups: [{ title: 'G', fields: ['name'] }] },
			{ id: 'b', kind: 'fields', title: 'Beta', fields: ['name'] }
		]
	});
	out.inline_ok = !!inline.panel;
	out.inline_noFetch = inline.fetches.length === 0;
	out.inline_tabLabels = textsOf(tabsOf(inline.panel)).join('|');
	const slots = inline.panel.querySelectorAll('[data-juneau-field]');
	out.inline_values = slots.map(s => s.getAttribute('data-juneau-field') + '=' + s.textContent).join('|');
	out.inline_groupTitle = textsOf(inline.panel.querySelectorAll('.juneau-view-detail-group-title')).join('|');
	out.inline_state = inline.panel.dataset.juneauDetailState;
	out.inline_labelFromColumn = textsOf(inline.panel.querySelectorAll('.juneau-view-detail-field-title')).join('|');

	// --- all-empty fields tab
	const empty = await scenario({
		mode: 'inline', tabs: [{ id: 'a', kind: 'fields', title: 'A', fields: ['nothing'] }]
	});
	out.empty_message = empty.panel.querySelectorAll('.juneau-view-detail-empty').map(n => n.textContent).join('|');

	// --- bean mode: exactly one fetch of the substituted endpoint; counts land in labels
	const bean = await scenario({
		mode: 'bean', endpoint: '/rd/pet/{id}',
		tabs: [{ id: 'a', kind: 'fields', title: 'Alpha', fields: ['name'] }, { id: 'b', kind: 'fields', title: 'Beta', fields: ['name'] }]
	}, { response: { contractVersion: '1', fields: { name: 'Rex' }, counts: { b: 3 } } });
	out.bean_fetches = bean.fetches.join(',');
	out.bean_labels = textsOf(tabsOf(bean.panel)).join('|');

	// --- bean mode without an endpoint fails closed
	const noUrl = await scenario({ mode: 'bean', tabs: [{ id: 'a', kind: 'fields', title: 'A', fields: ['name'] }] });
	out.bean_noUrl_state = noUrl.panel.dataset.juneauDetailState;

	// --- unknown kind: error pane, strip still works
	const unk = await scenario({ mode: 'inline', tabs: [{ id: 'a', kind: 'mystery', title: 'A' }, { id: 'b', kind: 'fields', title: 'B', fields: ['name'] }] });
	out.unknownKind_strip = tabsOf(unk.panel).length === 2;
	out.unknownKind_message = unk.panel.querySelectorAll('[role="status"]').map(n => n.textContent).join('|').includes('mystery');

	// --- tabs not an array: header only, one console error, no strip
	{
		const { I, env, rec } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath });
		const t = I.buildDetailTemplate({ title: 'T', tabs: 'nope' }, env.el('table'));
		out.badTabs_noShell = t.content.querySelectorAll('[data-juneau-detail-tabs]').length === 0;
		out.badTabs_hasHeader = t.content.querySelectorAll('.juneau-view-detail-header').length === 1;
		out.badTabs_errors = rec.errorsMatching('detail.tabs must be an array').length;
	}

	// --- header actions: link substituted and encoded; unsafe refused
	const act = await scenario({
		mode: 'inline', title: 'T',
		actions: [
			{ id: 'open', type: 'link', label: 'Open', href: '/pets/{id}/{name}' },
			{ id: 'bad', type: 'link', label: 'Bad', href: 'https://evil.example/x' },
			{ id: 'rm', type: 'row-action', label: 'Remove' }
		],
		tabs: [{ id: 'a', kind: 'fields', title: 'A', fields: ['name'] }]
	}, { rowData: { id: 's-7', name: 'a b<c' } });
	const links = act.panel.querySelectorAll('a');
	out.actions_linkCount = links.length;
	out.actions_linkHref = links[0] ? (links[0].getAttribute('href') || links[0].href) : null;
	out.actions_buttonCount = act.panel.querySelectorAll('[data-juneau-detail-header-action]').length;

	// --- script payloads inert
	const xss = await scenario({
		mode: 'inline', title: '<img src=x onerror=alert(1)>',
		tabs: [{ id: 'a', kind: 'fields', title: '<script>alert(1)</script>', fields: ['name'] }]
	}, { rowData: { id: 's-7', name: '<script>alert(2)</script>' } });
	out.xss_noScriptEl = xss.panel.querySelectorAll('script').length === 0 && xss.panel.querySelectorAll('img').length === 0;
	out.xss_valueIsText = xss.panel.querySelectorAll('[data-juneau-field]').map(n => n.textContent).join('') === '<script>alert(2)</script>';

	// --- nested sidecar helper
	{
		const { I, env } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath });
		const wrap = env.el('div');
		I.appendNestedSidecar(wrap, { view: { id: 'orders' }, meta: { contractVersion: '5', dataUrl: '/o' } });
		const sc = wrap.querySelectorAll('[data-juneau-nested-meta]');
		out.sidecar_count = sc.length;
		out.sidecar_id = sc[0] && sc[0].getAttribute('data-juneau-nested-meta');
		out.sidecar_json = sc[0] ? JSON.parse(sc[0].textContent).dataUrl : null;
		out.sidecar_type = sc[0] && sc[0].type;
	}

	// --- lazy load, per-row cache, cache none, invalidate, abort, polling idle, remembered tab, re-open
	const lazyDetail = function (cache) {
		return { mode: 'bean', endpoint: '/rd/pet/{id}', cache: cache,
			tabs: [
				{ id: 'a', kind: 'fields', title: 'A', fields: ['name'], refreshOnPoll: true },
				{ id: 'b', kind: 'fields', title: 'B', fields: ['other'], refreshOnPoll: true }
			] };
	};
	const tabFetches = function (sc) { return sc.fetches.filter(function (u) { return u.includes('/tab/'); }).length; };
	const clickTab = async function (sc, i) {
		const btn = tabsOf(sc.panel)[i];
		sc.panel.querySelector('[role="tablist"]').dispatch('click', { target: btn, preventDefault: function () {} });
		await flush(12);
	};
	const lazy = await scenario(lazyDetail());
	out.lazy_noTabFetchBeforeActivation = tabFetches(lazy) === 0;
	await clickTab(lazy, 1);
	out.lazy_firstActivationFetches = tabFetches(lazy) === 1;
	await clickTab(lazy, 0);
	await clickTab(lazy, 1);
	out.cache_perRowNoRefetch = tabFetches(lazy) === 1;
	out.remembered_tab = lazy.ctx._rememberedTab === 'b';

	// polling: a skipped tick idles non-active refreshOnPoll tabs
	const paneB = lazy.panel.querySelectorAll('[role="tabpanel"]')[1];
	await clickTab(lazy, 0);
	lazy.I.idleStaleDetailTabs(lazy.table);
	out.poll_idleClearsNonActive = paneB.childNodes.length === 0;
	await clickTab(lazy, 1);
	out.poll_idleRefetchesOnActivation = tabFetches(lazy) === 2;

	const none = await scenario(lazyDetail('none'));
	await clickTab(none, 1);
	await clickTab(none, 0);
	await clickTab(none, 1);
	out.cache_noneRefetches = tabFetches(none) === 2;

	out.invalidate_present = typeof lazy.panel.invalidateTab === 'function' && typeof lazy.panel.invalidateAll === 'function';
	const fetchesBeforeInvalidate = tabFetches(lazy);
	lazy.panel.invalidateAll();
	await flush(12);
	out.invalidate_activeRepopulated = lazy.panel.querySelectorAll('[role="tabpanel"]')[1].childNodes.length > 0 && tabFetches(lazy) === fetchesBeforeInvalidate + 1 && lazy.fetches[lazy.fetches.length - 1].includes('/tab/b');

	// collapse aborts in-flight tab fetches
	const hung = await scenario(lazyDetail(), { hang: true });
	await clickTab(hung, 1);
	const tabSignal = hung.signals[hung.signals.length - 1];
	out.abort_inflightBefore = !!tabSignal && tabSignal.aborted === false;
	hung.click();
	await flush(12);
	out.abort_onCollapse = !!tabSignal && tabSignal.aborted === true;

	// re-open by row id with the remembered active tab
	const re = await scenario(lazyDetail());
	await clickTab(re, 1);
	re.click();
	await flush(12);
	const before = re.fetches.length;
	const oldPanel = re.tr._juneauDetailPanel;
	re.ctx._reopenList = [{ rowId: 's-7', activeTabId: 'b' }];
	re.I.reopenDetailRows(re.table, re.ctx, null, [{ key: 's-7', el: re.tr }]);
	await flush(12);
	out.reopen_expanded = !!re.tr._juneauDetailPanel && re.tr._juneauDetailPanel !== oldPanel && re.tr._juneauDetailPanel.querySelectorAll('[role="tab"]').length === 2;
	out.reopen_activeTab = re.tr._juneauDetailPanel._juneauStrip.activeId() === 'b';
	out.reopen_fetched = re.fetches.length > before;
	const afterReopen = re.fetches.length;
	const panelAfterReopen = re.tr._juneauDetailPanel;
	re.ctx._reopenList = [{ rowId: 'nope', activeTabId: 'a' }];
	re.I.reopenDetailRows(re.table, re.ctx, null, []);
	await flush(12);
	out.reopen_gone = re.fetches.length === afterReopen && re.tr._juneauDetailPanel === panelAfterReopen && re.ctx._reopenList.length === 0;

	// with a model table in a reopen, the held handles are re-attached to the new panel instead of re-made
	{
		const mr = await scenario(lazyDetail());
		const calls = [];
		const fake = { _reopenPending: true, reattachDetail: function (id, el) { calls.push('reattach:' + id + ':' + (el === mr.tr._juneauDetailPanel)); return true; }, _rowExpanded: function () { calls.push('expanded'); }, _rowCollapsed: function () {} };
		mr.NS.model = { Table: { fromContext: function () { return fake; } } };
		mr.table.__juneauCtx = mr.ctx;
		mr.ctx._reopenList = [{ rowId: 's-7', activeTabId: 'a' }];
		mr.tr._juneauDetailPanel = null;
		mr.I.reopenDetailRows(mr.table, mr.ctx, fake, [{ key: 's-7', el: mr.tr }]);
		await flush(12);
		out.reopen_model = calls.join('|');
	}

	// --- tab-level error: Retry rebuilds only that tab and returns focus to its button
	const bad = await scenario({
		mode: 'bean', endpoint: '/rd/pet/{id}',
		tabs: [{ id: 'a', kind: 'fields', title: 'A', fields: ['name'] }, { id: 'b', kind: 'fields', title: 'B', fields: ['other'] }]
	});
	let failTabs = true;
	bad.env.setFetch(function (url) {
		bad.fetches.push(String(url));
		if (failTabs) return Promise.resolve({ ok: false, status: 500, headers: { get: function () { return null; } }, text: function () { return Promise.resolve(''); } });
		return Promise.resolve({ ok: true, status: 200, headers: { get: function () { return null; } }, text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: { other: 'x' } })); } });
	});
	await clickTab(bad, 1);
	const paneBad = bad.panel.querySelectorAll('[role="tabpanel"]')[1];
	const retryBtn = paneBad.querySelector('.juneau-view-detail-retry');
	out.error_retryShown = !!retryBtn && paneBad.querySelectorAll('[role="status"]').length === 1;
	const otherBefore = bad.panel.querySelectorAll('[role="tabpanel"]')[0].childNodes.length;
	failTabs = false;
	retryBtn.dispatch('click', { preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);
	out.error_retryRepaints = !paneBad.querySelector('.juneau-view-detail-retry') && paneBad.querySelectorAll('[data-juneau-field]').length === 1;
	out.error_retryOnlyThatTab = bad.panel.querySelectorAll('[role="tabpanel"]')[0].childNodes.length === otherBefore;
	out.error_retryFocus = bad.env.getActive() === tabsOf(bad.panel)[1];

	process.stdout.write(JSON.stringify(out));
})();
