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
 * visibility-wiring.cjs - always-on Node harness for where visibleWhen takes effect on a row action: the menu omits
 * a hidden item, an action-bound pill (and the menu trigger, when nothing is left) is hidden at draw time, and a
 * hidden action never fires even when its pill is activated directly.  Every assertion lives in the Java test.
 *
 *   Usage:  node visibility-wiring.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node visibility-wiring.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = { hasInit: !!(I && typeof I.buildRowActionMenu === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

env.window.JuneauConsole = { contract: function () { return { facts: { viewer: { roles: ['oncall'] } } }; } };

const fetchCalls = [];
env.setFetch(function (url, opts) { fetchCalls.push({ url: url, opts: opts }); return Promise.resolve(jsonResponse({}, { status: 200 })); });

const OPEN_ONLY = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST',
	visibleWhen: [{ field: 'status', op: 'eq', value: 'open' }] };
const ONCALL_ONLY = { id: 'page', label: 'Page', endpoint: '/x/page', method: 'POST',
	visibleWhen: [{ field: 'facts.viewer.roles', op: 'contains', value: 'oncall' }] };
const ADMIN_ONLY = { id: 'purge', label: 'Purge', endpoint: '/x/purge', method: 'POST',
	visibleWhen: [{ field: 'facts.viewer.roles', op: 'contains', value: 'admin' }] };
const ALWAYS = { id: 'view', label: 'View', endpoint: '/x/view', method: 'POST' };

function fixture(rowData, viewDef) {
	const table = env.el('table');
	table.dataset.juneauView = 'v';
	table.dataset.juneauCsrf = 'tok-123';
	const tbody = env.el('tbody');
	const tr = env.el('tr');
	const td = env.el('td');
	tr.appendChild(td);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	env.body.appendChild(table);
	const ctx = { viewDef: viewDef, dataTable: { row: function () { return { data: function () { return rowData; } }; } } };
	return { table: table, tr: tr, td: td, ctx: ctx };
}

function pillIn(f, actionId) {
	const pill = env.el('span');
	pill.dataset.juneauPill = '';
	pill.setAttribute('role', 'button');
	pill.dataset.juneauAction = actionId;
	f.td.appendChild(pill);
	return pill;
}

function menuIds(rowData, rowActions) {
	const viewDef = { rowActions: rowActions };
	const f = fixture(rowData, viewDef);
	const menu = I.buildRowActionMenu(viewDef, f.table, f.tr, f.ctx);
	return menu.querySelectorAll('.juneau-view-action-item').map(function (b) { return b.dataset.actionId; });
}

out.menuOpenRow = menuIds({ status: 'open' }, [OPEN_ONLY, ONCALL_ONLY, ADMIN_ONLY, ALWAYS]);
out.menuClosedRow = menuIds({ status: 'closed' }, [OPEN_ONLY, ONCALL_ONLY, ADMIN_ONLY, ALWAYS]);

// Pill gating at draw time.
{
	const viewDef = { rowActions: [OPEN_ONLY, ALWAYS] };
	const f = fixture({ status: 'closed' }, viewDef);
	const hiddenPill = pillIn(f, 'ack');
	const shownPill = pillIn(f, 'view');
	I.applyRowActionPillGates(f.tr, { status: 'closed' }, viewDef);
	out.pillHidden = !!hiddenPill.hidden;
	out.pillShown = !shownPill.hidden;
}

// Trigger hidden when every action is hidden for the row.
{
	const viewDef = { rowActions: [OPEN_ONLY] };
	const f = fixture({ status: 'closed' }, viewDef);
	const trigger = env.el('button');
	trigger.className = 'juneau-view-action-trigger';
	f.td.appendChild(trigger);
	I.applyRowActionPillGates(f.tr, { status: 'closed' }, viewDef);
	out.triggerHiddenWhenNothingLeft = !!trigger.hidden;
}
{
	const viewDef = { rowActions: [OPEN_ONLY, ALWAYS] };
	const f = fixture({ status: 'closed' }, viewDef);
	const trigger = env.el('button');
	trigger.className = 'juneau-view-action-trigger';
	f.td.appendChild(trigger);
	I.applyRowActionPillGates(f.tr, { status: 'closed' }, viewDef);
	out.triggerShownWhenSomethingLeft = !trigger.hidden;
}

// A hidden action never fires, even when its pill is activated directly; a visible one does.
{
	const viewDef = { rowActions: [OPEN_ONLY] };
	let f = fixture({ status: 'closed' }, viewDef);
	let pill = pillIn(f, 'ack');
	let before = fetchCalls.length;
	I.activatePillAction(pill, f.table, viewDef, f.ctx);
	out.hiddenActionFired = fetchCalls.length > before;
	f = fixture({ status: 'open' }, viewDef);
	pill = pillIn(f, 'ack');
	before = fetchCalls.length;
	I.activatePillAction(pill, f.table, viewDef, f.ctx);
	out.visibleActionFired = fetchCalls.length > before;
}

process.stdout.write(JSON.stringify(out));
