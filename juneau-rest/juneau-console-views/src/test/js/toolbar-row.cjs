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
 * toolbar-row.cjs - where buildToolbarRow(...) puts the DataTables search box among the ribbon groups.  The paging
 * pill stays alone in the left cluster; the search box takes the canonical `search` slot among the tagged groups
 * (after entity/filter, before viewing/page/status), and an untagged ribbon keeps today's search-then-ribbon order.
 *
 * Usage:  node toolbar-row.cjs <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv, loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const ribbonJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !ribbonJsPath) {
	console.error('usage: node toolbar-row.cjs <juneau-renders.js> <juneau-views.js> <juneau-ribbon.js>');
	process.exit(2);
}
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

function loadBoth() {
	const env = makeEnv();
	const loaded = loadViews(rendersJsPath, viewsJsPath, env);
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () { /* no-op */ },
		setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ }
	};
	// NOSONAR javascript:S1523 -- loading the production juneau-ribbon.js source into a VM sandbox is this
	// harness's intended mechanism; the path is a fixed local file supplied by the test.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	return { env: env, NS: env.window.JuneauViews, I: loaded.I };
}

function action(type, extra) { return { type: type, ...extra }; }

/** Builds a toolbar row from a fresh wrapper holding a native search box, and names the right cluster's layout. */
function layout(ribbon) {
	const { env, NS, I } = loadBoth();
	const wrapper = env.el('div');
	wrapper.className = 'dt-container';
	const search = env.el('div');
	search.className = 'dt-search';
	search.appendChild(env.el('input'));
	wrapper.appendChild(search);
	const pill = env.el('div');
	pill.className = 'juneau-view-pagingpill';
	const bar = ribbon ? NS.ribbon.build({ columnConfig: true, ribbon: ribbon }, { hasRowDetail: true, dataTable: {}, redraw: function () { /* no-op */ } }) : null;
	const row = I.buildToolbarRow(wrapper, pill, bar);
	const left = row.childNodes[0];
	const right = row.childNodes[1];
	const names = [];
	(function walk(n) {
		n.childNodes.forEach(function (c) {
			if (c.className === 'dt-search') names.push('search');
			else if (c.className === 'juneau-view-ribbon') walk(c);
			else if (c.className === 'juneau-view-ribbon-group') names.push(c.getAttribute('data-juneau-group') || 'untagged');
		});
	})(right);
	return {
		right: names.join(' '),
		leftIsPillOnly: left.childNodes.length === 1 && left.childNodes[0] === pill,
		rowIsFirstChild: wrapper.firstChild === row,
		searchLabelled: search.childNodes[0].getAttribute('aria-label') === 'Search table'
	};
}

const out = {};
{
	const probe = loadBoth();
	out.hasSeam = typeof probe.I?.buildToolbarRow === 'function' && typeof probe.NS?.ribbon?.build === 'function';
}
if (out.hasSeam) {
	// Untagged ribbon - today's order: search, then the ribbon.
	const untagged = layout([action('collapseAll'), action('option', { id: 'mine', title: 'Mine' })]);
	out.untagged_right = untagged.right;
	out.untagged_leftIsPillOnly = untagged.leftIsPillOnly;
	out.untagged_rowIsFirstChild = untagged.rowIsFirstChild;
	out.untagged_searchLabelled = untagged.searchLabelled;

	// Tagged groups on both sides of the search slot - search lands between filter and viewing.
	out.tagged_right = layout([
		action('option', { id: 'v', group: 'viewing', title: 'V' }), action('resetDefaults', { group: 'page' }),
		action('option', { id: 'f', group: 'filter', title: 'F' }), action('option', { id: 'e', group: 'entity', title: 'E' })
	]).right;

	// Only entity/filter groups - search goes last, after them.
	out.onlyBefore_right = layout([action('option', { id: 'f', group: 'filter', title: 'F' })]).right;

	// Only groups ranked after search - search goes first.
	out.onlyAfter_right = layout([action('refresh'), action('resetDefaults', { group: 'page' })]).right;

	// A ribbon `search` item leaves a placeholder: the filter box replaces it in place; no placeholder is left over.
	{
		const { env, NS, I } = loadBoth();
		const wrapper = env.el('div');
		const search = env.el('div');
		search.className = 'dt-search';
		search.appendChild(env.el('input'));
		wrapper.appendChild(search);
		const bar = NS.ribbon.build({ ribbon: [
			action('search', { id: 'search', group: 'search' }), action('option', { id: 'f', group: 'filter', title: 'F' })
		] }, { dataTable: {}, redraw: function () { /* no-op */ } });
		const row = I.buildToolbarRow(wrapper, null, bar);
		const right = row.childNodes[1];
		out.slot_filterInRibbon = bar.querySelector('.dt-search') === search;
		out.slot_noPlaceholderLeft = bar.querySelector('[data-juneau-slot="search"]') === null;
		out.slot_notDuplicatedInRight = right.childNodes.filter(function (c) { return c.className === 'dt-search'; }).length === 0;
		out.slot_filterTagged = search.getAttribute('data-juneau-ribbon-id');
		out.slot_inSearchGroup = search.parentNode.getAttribute('data-juneau-group');
	}
	// No filter box (searching:false): the placeholder is removed and nothing empty remains.
	{
		const { env, NS, I } = loadBoth();
		const wrapper = env.el('div');
		const bar = NS.ribbon.build({ ribbon: [action('search', { id: 'search', group: 'search' })] },
			{ dataTable: {}, redraw: function () { /* no-op */ } });
		I.buildToolbarRow(wrapper, null, bar);
		out.slot_removedWithoutFilter = bar.querySelector('[data-juneau-slot="search"]') === null;
	}

	// No ribbon at all - the right cluster is the search box alone.
	out.noRibbon_right = layout(null).right;
}

process.stdout.write(JSON.stringify(out));
