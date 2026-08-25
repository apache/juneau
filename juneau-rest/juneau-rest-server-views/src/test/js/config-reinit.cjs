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
 * config-reinit.cjs - always-on Node harness for the destroy/reinit index rebind (slice 5):
 * resolveOrder against the live opts.columns array via dtIndex, plus applyView / resolveActiveView
 * export presence.
 *
 * No Playwright / Chromium / DataTables — loads the real juneau-config.js then juneau-views.js IIFEs
 * against a minimal fake window/document.  Driven by ViewsJs_Reinit_Test.
 *
 *   Usage:  node config-reinit.cjs <path-to-juneau-config.js> <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test (this script only OBSERVES).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const configJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!configJsPath || !viewsJsPath) {
	console.error('usage: node config-reinit.cjs <juneau-config.js> <juneau-views.js>');
	process.exit(2);
}

const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () {
		return {
			setAttribute: function () {},
			appendChild: function () {},
			querySelector: function () { return null; },
			querySelectorAll: function () { return []; }
		};
	},
	body: { appendChild: function () {}, querySelectorAll: function () { return []; } }
};

const window = { document: document, console: console, jQuery: undefined };
const sandbox = { window: window, document: document, console: console };
// NOSONAR javascript:S1523 -- this is the test harness deliberately loading the real
// juneau-config.js under test into an isolated vm sandbox; there is no untrusted input.
vm.runInNewContext(fs.readFileSync(path.resolve(configJsPath), 'utf8'), sandbox, { filename: 'juneau-config.js' });
// NOSONAR javascript:S1523 -- same rationale: deliberately loading the real juneau-views.js under test
// into an isolated vm sandbox; there is no untrusted input.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const out = {
	hasConfig: !!NS?.config,
	hasInit: !!NS?.init,
	hasDtIndex: typeof NS?.config?.dtIndex === 'function',
	hasApplyView: typeof NS?.config?.applyView === 'function',
	hasResolveActiveView: typeof NS?.config?.resolveActiveView === 'function',
	hasBuildTable: typeof NS?.init?.buildTable === 'function',
	hasResolveOrder: typeof NS?.init?.resolveOrder === 'function'
};

if (!out.hasConfig || !out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const C = NS.config;
const I = NS.init;

const catalog = [
	{ data: 'A', title: 'Col A', orderable: true },
	{ data: 'B', title: 'Col B', orderable: true, visible: false },
	{ data: 'C', title: 'Col C', orderable: true }
];
const effective = C.computeEffectiveColumns(catalog, {
	schemaVersion: 1, visible: ['A', 'C'], order: ['A', 'B', 'C'], labels: {}, formats: {}
});
const optsColumns = C.buildOptsColumnSpace(effective, { hasSelection: true, hasActions: true });
optsColumns.forEach(function (c) {
	if (c.data === 'B') c.visible = false;
	if (c.data && c.data !== 'B') { c.visible = true; c.orderable = true; }
});

const viewDef = { columns: catalog, defaultOrder: [{ data: 'C', dir: 'asc' }] };
const order = I.resolveOrder(viewDef, optsColumns);
out.order = order;
out.orderIndex = order?.[0] ? order[0][0] : null;
out.orderDir = order?.[0] ? order[0][1] : null;
out.dtIndexC = C.dtIndex('C', optsColumns);

const hiddenOrder = I.resolveOrder(
	{ columns: catalog, defaultOrder: [{ data: 'B', dir: 'desc' }] },
	optsColumns
);
out.hiddenFallbackIndex = hiddenOrder?.[0] ? hiddenOrder[0][0] : null;
out.hiddenFallbackData = hiddenOrder?.[0]
	? optsColumns[hiddenOrder[0][0]]?.data
	: null;

out.applyViewNotInit = C.applyView({}, null);

process.stdout.write(JSON.stringify(out));
