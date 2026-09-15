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
 * paging-pill-draw.cjs - always-on Node harness for buildPagingPill's DataTables nav handlers.
 * A fake DataTables API models 2.1.8 draw(resetPaging): omitted/true jumps back to page 0; false holds
 * the page that page("next"|"last"|"previous"|"first") just set.  Every assertion lives in the Java test.
 *
 *   Usage:  node paging-pill-draw.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node paging-pill-draw.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasBuildPagingPill: !!(I && typeof I.buildPagingPill === 'function')
};
if (!out.hasBuildPagingPill) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

/**
 * DataTables-shaped API: page("next") updates the page, then draw() with the 2.1.8 default
 * (resetPaging=true when the arg is omitted) snaps _iDisplayStart back to 0.
 */
function fakeDt(total, pageLen) {
	let currentPage = 0;
	let length = pageLen;
	const drawArgs = [];
	let drawHandler = null;
	const table = env.el('table');

	function pageCount() {
		if (length < 0) return 1;
		return Math.max(1, Math.ceil(total / length));
	}

	function applyPage(cmd) {
		const pgs = pageCount();
		if (cmd === 'first') currentPage = 0;
		else if (cmd === 'previous') currentPage = Math.max(0, currentPage - 1);
		else if (cmd === 'next') currentPage = Math.min(pgs - 1, currentPage + 1);
		else if (cmd === 'last') currentPage = Math.max(0, pgs - 1);
	}

	const api = {};
	function draw(resetPaging) {
		drawArgs.push(resetPaging);
		if (resetPaging !== false && resetPaging !== 'page') currentPage = 0;
		if (drawHandler) drawHandler({ target: table });
		return api;
	}
	function page(cmd) {
		applyPage(cmd);
		return api;
	}
	page.info = function () {
		const size = length < 0 ? total : length;
		const start = currentPage * size;
		const end = Math.min(start + size, total);
		return { page: currentPage, pages: pageCount(), start: start, end: end, recordsDisplay: total, recordsTotal: total };
	};
	page.len = function (n) {
		if (n === undefined) return length;
		length = n;
		return api;
	};
	api.page = page;
	api.draw = draw;
	api.on = function (event, fn) {
		if (event === 'draw.dt') drawHandler = fn;
		return api;
	};
	api.table = table;
	api.drawArgs = drawArgs;
	return api;
}

function click(el) { el.dispatch('click', {}); }

function btn(pill, label) {
	return pill.querySelector('[aria-label="' + label + '"]');
}

function optionEl(pill, label) {
	const opts = pill.querySelectorAll('[role="option"]');
	for (const o of opts) if (o.textContent === label) return o;
	return null;
}

// --- Control: the fake itself reproduces the 2.1.8 reset vs hold fork ---------------------------------
(function control() {
	const dt = fakeDt(69, 25);
	dt.page('next').draw();
	out.control_drawNoArg_page = dt.page.info().page;
	dt.page('next').draw(false);
	out.control_drawFalse_page = dt.page.info().page;
	dt.page('last').draw();
	out.control_lastDrawNoArg_page = dt.page.info().page;
	dt.page('last').draw(false);
	out.control_lastDrawFalse_page = dt.page.info().page;
})();

// --- Pill: 69 rows / length 25 / 3 pages, matching the Scripts catalog reproduction -------------------
const dt = fakeDt(69, 25);
const ctx = { dataTable: dt, table: dt.table };
const pill = I.buildPagingPill({}, ctx);
env.body.appendChild(pill);

out.initial_page = dt.page.info().page;
out.initial_summary = pill.querySelector('.juneau-view-pagingpill-info').textContent;
out.initial_nextDisabled = btn(pill, 'Next page').disabled === true;
out.initial_lastDisabled = btn(pill, 'Last page').disabled === true;

click(btn(pill, 'Next page'));
out.next_page = dt.page.info().page;
out.next_drawArgIsFalse = dt.drawArgs.at(-1) === false;
out.next_summary = pill.querySelector('.juneau-view-pagingpill-info').textContent;

click(btn(pill, 'Last page'));
out.last_page = dt.page.info().page;
out.last_drawArgIsFalse = dt.drawArgs.at(-1) === false;
out.last_summary = pill.querySelector('.juneau-view-pagingpill-info').textContent;

click(btn(pill, 'Previous page'));
out.prev_page = dt.page.info().page;
out.prev_drawArgIsFalse = dt.drawArgs.at(-1) === false;

click(btn(pill, 'First page'));
out.first_page = dt.page.info().page;
out.first_drawArgIsFalse = dt.drawArgs.at(-1) === false;

out.navDrawArgsAllFalse = dt.drawArgs.length === 4 && dt.drawArgs.every(function (a) { return a === false; });

// Walk to page 1, then change page size — length change must reset to page 0 (default draw()).
click(btn(pill, 'Next page'));
out.beforeSizeChange_page = dt.page.info().page;
click(optionEl(pill, '100 rows'));
out.pageSize_page = dt.page.info().page;
out.pageSize_drawArgIsFalse = dt.drawArgs.at(-1) === false;
out.pageSize_drawArgOmitted = dt.drawArgs.at(-1) === undefined;

process.stdout.write(JSON.stringify(out));
