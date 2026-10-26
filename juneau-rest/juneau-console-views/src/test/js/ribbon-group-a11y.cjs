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
 * ribbon-group-a11y.cjs - proves an optionGroup's wrapper is an accessible group: role="group" plus aria-label
 * taken from the group's title (aria only: no visible caption child), and that an untitled group gets neither.
 *
 * Usage:  node ribbon-group-a11y.cjs <juneau-ribbon.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const ribbonJsPath = process.argv[2];
if (!ribbonJsPath) {
	console.error('usage: node ribbon-group-a11y.cjs <juneau-ribbon.js>');
	process.exit(2);
}
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

/** Builds a ribbon holding one optionGroup and reports its wrapper's accessibility attributes. */
function probe(groupItem) {
	const env = makeEnv();
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () { /* no-op */ },
		setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ }
	};
	// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	const NS = env.window.JuneauViews;
	NS.icons = { resolveIcon: function () { return null; } };
	const bar = NS.ribbon.build({ ribbon: [groupItem] }, { dataTable: {}, redraw: function () { /* no-op */ } });
	// The optionGroup wrapper is the group whose children are the member buttons (the cluster wrapper holds that wrapper).
	function find(node) {
		if (!node.childNodes) return null;
		for (const c of node.childNodes) {
			if (c.className === 'juneau-view-ribbon-group' && c.childNodes.length && c.childNodes.every(function (k) { return k.tagName === 'BUTTON'; }))
				return c;
			const hit = find(c);
			if (hit) return hit;
		}
		return null;
	}
	const wrap = find(bar);
	if (!wrap) return { built: false };
	return {
		built: true,
		role: wrap.getAttribute('role'),
		aria: wrap.getAttribute('aria-label'),
		members: wrap.childNodes.length,
		text: wrap.textContent || ''
	};
}

const members = [{ id: 'a', title: 'Alpha' }, { id: 'b', title: 'Beta' }];
const out = {};
out.titled = probe({ type: 'optionGroup', id: 'phase', title: 'Phase', options: members });
out.untitled = probe({ type: 'optionGroup', id: 'phase', options: members });
process.stdout.write(JSON.stringify(out));
