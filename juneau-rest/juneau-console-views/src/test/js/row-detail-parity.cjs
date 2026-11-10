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
 * row-detail-parity.cjs - loads the Java-serialised detail golden into the shipped views runtime, expands a row and
 * reports what was painted.  Assertions live in the paired Java test.
 *
 *   Usage:  node row-detail-parity.cjs <renders> <views> <regions> <helpers> <golden.json>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const [rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath, goldenPath] = process.argv.slice(2);
if (!goldenPath) {
	console.error('usage: node row-detail-parity.cjs <renders> <views> <regions> <helpers> <golden.json>');
	process.exit(2);
}

(async function main() {
	const detail = JSON.parse(fs.readFileSync(goldenPath, 'utf8'));
	const { env, I } = load(rendersJsPath, viewsJsPath, regionsJsPath, { helpersJsPath: helpersJsPath });
	const fetches = [];
	env.setFetch(function (url) { fetches.push(String(url)); return Promise.reject(new Error('unexpected fetch')); });

	const out = { kinds: I.detailTabKinds.slice() };
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
	const rowData = { id: 's-7', name: 'Rex', status: 'ok' };
	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = 's-7';
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);
	let panel = null, shown = false;
	const child = function (p) {
		panel = p;
		return { show: function () { const r = env.el('tr'), c = env.el('td'); r.appendChild(c); c.appendChild(p); tbody.appendChild(r); shown = true; }, hide: function () { shown = false; } };
	};
	child.isShown = function () { return shown; };
	child.hide = function () { shown = false; };
	const ctx = {
		viewDef: { id: 'pets', columns: [{ data: 'name', title: 'Name' }] },
		dataTable: { row: function () { return { length: 1, child: child, data: function () { return rowData; } }; } }
	};
	I.initDetailsExpander(table, ctx, ctx.viewDef);
	table.dispatch('click', { target: td, preventDefault: function () {}, stopPropagation: function () {} });
	await flush(12);

	const tabs = panel.querySelectorAll('[role="tab"]');
	out.tabIds = tabs.map(function (t) { return t.getAttribute('data-tab-id'); }).join('|');
	out.tabLabels = tabs.map(function (t) { return t.textContent; }).join('|');
	const link = panel.querySelectorAll('a')[0];
	out.headerLinkId = link ? link.getAttribute('data-juneau-detail-link-id') : null;
	out.headerLinkHref = link ? (link.getAttribute('href') || link.href) : null;
	out.noFetch = fetches.length === 0;
	process.stdout.write(JSON.stringify(out));
})();
