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
 * header-sort-search.cjs - always-on Node harness for the header sort + per-column search glyphs (design 7.1).
 * Builds DT2-shaped header cells (title then order span), drives wireHeaderSortSearch(...), and reports the
 * resulting glyph DOM order plus the active-state class.  Every assertion lives in the Java test.
 *
 *   Usage:  node header-sort-search.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node header-sort-search.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasWireHeaderSortSearch: !!(I && typeof I.wireHeaderSortSearch === 'function')
};
if (!out.hasWireHeaderSortSearch) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/** A DT2-shaped header cell: th > div.dt-column-header > (span.dt-column-title, span.dt-column-order). */
function headerCell(title, orderable) {
	const th = env.el('th');
	const flex = env.el('div');
	flex.className = 'dt-column-header';
	const titleSpan = env.el('span');
	titleSpan.className = 'dt-column-title';
	titleSpan.textContent = title;
	flex.appendChild(titleSpan);
	if (orderable) {
		const order = env.el('span');
		order.className = 'dt-column-order';
		flex.appendChild(order);
	}
	th.appendChild(flex);
	return th;
}

function fakeCol(idx, header, searchVal) {
	return {
		index: function () { return idx; },
		header: function () { return header; },
		search: function () { return searchVal || ''; }
	};
}

function fakeDt(cols) {
	return {
		columns: function () {
			return { every: function (fn) { cols.forEach(function (c) { fn.call(c); }); } };
		}
	};
}

/** Reduces a flex row's element children to identifying glyph tokens in DOM order. */
function glyphOrder(header) {
	const flex = header.querySelector('div.dt-column-header');
	const tokens = [];
	for (const c of flex.childNodes) {
		if (c.nodeType !== 1) continue;
		const cls = c.className || '';
		if (cls.indexOf('dt-column-order') >= 0) tokens.push('sort');
		else if (cls.indexOf('juneau-view-col-search-icon') >= 0) tokens.push('search');
		else if (cls.indexOf('dt-column-title') >= 0) tokens.push('title');
		else tokens.push('?');
	}
	return tokens;
}

function searchActive(header) {
	const icon = header.querySelector('.juneau-view-col-search-icon');
	return !!(icon && (icon.className || '').indexOf('is-active') >= 0);
}

// --- Column set: an orderable+searchable column with no filter, one WITH a filter, one non-orderable ---------
const h0 = headerCell('Name', true);
const h1 = headerCell('Status', true);
const h2 = headerCell('Notes', false);
const cols = [ fakeCol(0, h0, ''), fakeCol(1, h1, 'OPEN'), fakeCol(2, h2, '') ];
const table = env.el('table');
const ctx = {
	dataTable: fakeDt(cols),
	optsColumns: [
		{ data: 'name', title: 'Name' },
		{ data: 'status', title: 'Status' },
		{ data: 'notes', title: 'Notes', orderable: false }
	]
};

I.wireHeaderSortSearch(table, ctx);

// Design 7.1: sort glyph, then magnifying glass immediately beside it, then the title.
out.orderableColumnGlyphOrder = glyphOrder(h0);
// A non-orderable searchable column has no sort glyph, so the magnifying glass leads.
out.nonOrderableColumnGlyphOrder = glyphOrder(h2);
// Active-state reflects an already-applied filter on first render (no popover interaction needed).
out.unfilteredColumnActive = searchActive(h0);
out.filteredColumnActive = searchActive(h1);

process.stdout.write(JSON.stringify(out));
