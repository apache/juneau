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
 * detail-region-visible-when.cjs - always-on Node harness: a row-detail region's visibleWhen is evaluated against the
 * PARENT row, not against the child row DataTables wraps the panel in.
 *
 *   Usage:  node detail-region-visible-when.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 *
 * The panel sits in a sibling child <tr> inside the view table's own tbody, so the nearest body row of a region
 * element is that child row, whose row data is nothing.  hookContextOf (rowActions.contextOf) must step to the panel's
 * _juneauParentTr instead; otherwise every rule reads an empty row and a region gated on a row field never shows.
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node detail-region-visible-when.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

function region(env, id, status) {
	const el = env.el('div');
	el.dataset.juneauRegion = id;
	el.dataset.juneauRegionType = 'row-detail';
	el.dataset.juneauRegionDeclared = JSON.stringify({
		contractVersion: '1',
		dataUrl: '/pets/{id}/' + id,
		visibleWhen: [{ field: 'status', op: 'eq', value: status }]
	});
	return el;
}

(async function main() {
	const { env, NS } = load(rendersJsPath, viewsJsPath, regionsJsPath);
	const fetches = [];
	env.setFetch(function (url) {
		fetches.push(String(url));
		return Promise.resolve({
			ok: true, status: 200,
			headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: {} })); }
		});
	});

	const table = env.el('table');
	table.dataset.juneauView = 'pets';
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	env.body.appendChild(table);
	const tr = env.el('tr');
	tr.dataset.juneauRowId = 'p1';
	tbody.appendChild(tr);
	const childTr = env.el('tr');
	const childTd = env.el('td');
	const panel = env.el('div');
	panel.className = 'juneau-view-detail-panel';
	panel.dataset.juneauRowDetail = '';
	panel.dataset.juneauRowId = 'p1';
	panel._juneauParentTr = tr;
	tr._juneauDetailPanel = panel;
	childTd.appendChild(panel);
	childTr.appendChild(childTd);
	tbody.appendChild(childTr);
	const parentRow = { id: 'p1', status: 'OPEN' };
	table.__juneauCtx = {
		table: table, viewDef: { id: 'pets' },
		dataTable: { row: function (t) { return { data: function () { return t === tr ? parentRow : undefined; } }; } }
	};

	const open = region(env, 'open', 'OPEN');
	const closed = region(env, 'closed', 'CLOSED');
	panel.appendChild(open);
	panel.appendChild(closed);
	out.contextIsParentRow = NS.rowActions.contextOf(open)?.tr === tr;
	NS.regions.enrolIn(panel);
	await flush(12);

	out.matchingRegionShown = open.hidden !== true && open.dataset.juneauRegionHidden === undefined;
	out.matchingRegionFetched = fetches.indexOf('/pets/p1/open') >= 0;
	out.otherRegionHidden = closed.hidden === true && closed.dataset.juneauRegionHidden === 'rule';
	out.otherRegionNotFetched = fetches.indexOf('/pets/p1/closed') < 0;

	process.stdout.write(JSON.stringify(out));
})();
