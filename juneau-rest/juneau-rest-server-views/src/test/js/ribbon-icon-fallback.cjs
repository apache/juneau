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
 * ribbon-icon-fallback.cjs - proves an icon name found in none of the sprite layers draws NOTHING on a ribbon
 * button (WORK-J0557 U11): no raw-label text fallback, while the label stays the accessible name (aria-label) and
 * the custom tooltip (data-jc-tip).  Also runs a control with a resolving registry so the test cannot pass merely
 * because the button never draws.
 *
 * Usage:  node ribbon-icon-fallback.cjs <juneau-ribbon.js>
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
	console.error('usage: node ribbon-icon-fallback.cjs <juneau-ribbon.js>');
	process.exit(2);
}
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

/** Loads a fresh juneau-ribbon.js with the given stand-in icon registry, builds a one-refresh ribbon, reports its button. */
function probe(resolveIcon) {
	const env = makeEnv();
	const sandbox = {
		window: env.window, document: env.document, console: console,
		setTimeout: function () { return 0; }, clearTimeout: function () { /* no-op */ },
		setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ }
	};
	// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	const NS = env.window.JuneauViews;
	NS.icons = { resolveIcon: resolveIcon };
	const bar = NS.ribbon.build({ ribbon: [{ type: 'refresh', title: 'Reload it' }] }, { dataTable: {}, redraw: function () { /* no-op */ } });
	const group = bar.childNodes.find(function (c) { return c.className === 'juneau-view-ribbon-group'; });
	const b = group ? group.childNodes[0] : null;
	if (!b) return { built: false };
	return {
		built: true,
		text: b.textContent || '',
		aria: b.getAttribute('aria-label'),
		tip: b.dataset ? b.dataset.jcTip : null,
		empty: !b.innerHTML
	};
}

const out = {};
out.unknown = probe(function () { return null; });
out.known = probe(function () { return '<svg data-marker="x"></svg>'; });
process.stdout.write(JSON.stringify(out));
