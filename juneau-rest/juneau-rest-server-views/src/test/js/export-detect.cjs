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
 * export-detect.cjs - always-on Node harness for the "export loud" contract: a HARD `buttons` entry whose required
 * library is absent renders DISABLED with a tip naming the library and logs E-JS-69 once per table, while an
 * `optional` entry whose library is absent stays a silent omission.  Every assertion lives in the Java test.
 *
 *   Usage: node export-detect.cjs <juneau-ribbon.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const ribbonJsPath = process.argv[2];
if (!ribbonJsPath) { console.error('usage: node export-detect.cjs <juneau-ribbon.js>'); process.exit(2); }
const ribbonJsSource = fs.readFileSync(path.resolve(ribbonJsPath), 'utf8');

/** Loads a fresh juneau-ribbon.js; `opts.buttons === false` simulates the Buttons extension being absent. */
function loadRibbon(opts) {
	opts = opts || {};
	const env = makeEnv();
	if (opts.buttons !== false) env.window.jQuery = { fn: { dataTable: { Buttons: function () {} } } };
	if (opts.jszip) env.window.JSZip = {};
	if (opts.pdfmake) env.window.pdfMake = {};
	const errors = [];
	const sandbox = {
		window: env.window, document: env.document,
		console: { error: function (msg) { errors.push(String(msg)); }, log: function () {}, warn: function () {} },
		setTimeout: function () { return 0; }, clearTimeout: function () {},
		setInterval: function () { return 0; }, clearInterval: function () {}
	};
	// NOSONAR javascript:S1523 -- loading the production script into a VM sandbox is this harness's intended mechanism.
	vm.runInNewContext(ribbonJsSource, sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	return { env: env, NS: env.window.JuneauViews, errors: errors };
}

const out = {};
const probe = loadRibbon({ buttons: true });
out.hasResolveExportButtons = typeof probe.NS?.ribbon?.resolveExportButtons === 'function';
out.hasBuild = typeof probe.NS?.ribbon?.build === 'function';
if (!out.hasResolveExportButtons || !out.hasBuild) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function exportAction(extra) { return Object.assign({ type: 'export' }, extra || {}); }
function resolve(scenario, action) {
	const r = loadRibbon(scenario);
	return r.NS.ribbon.resolveExportButtons(action, r.NS.ribbon.detectExportFeatures(r.env.window));
}
function disabledButtons(node) {
	const found = [];
	(function walk(n) {
		if (n.tagName === 'BUTTON' && n.disabled) found.push(n);
		(n.childNodes || []).forEach(walk);
	})(node);
	return found;
}

out.noButtonsExtension = resolve({ buttons: false }, exportAction({ buttons: ['copy', 'excel'] }));
out.plainButtons = resolve({ buttons: true }, exportAction({ buttons: ['copy', 'csv'] }));
out.hardExcelNoJszip = resolve({ buttons: true }, exportAction({ buttons: ['copy', 'excel'] }));
out.optionalExcelNoJszip = resolve({ buttons: true }, exportAction({ optional: ['excel'] }));
out.hardPdfWithPdfmake = resolve({ buttons: true, pdfmake: true }, exportAction({ buttons: ['pdf'] }));
out.optionalExcelWithJszip = resolve({ buttons: true, jszip: true }, exportAction({ optional: ['excel'] }));

{
	const r = loadRibbon({ buttons: true });
	const ctx = { dataTable: null, redraw: function () {} };
	const bar = r.NS.ribbon.build({ ribbon: [exportAction({ buttons: ['copy', 'excel'] }), exportAction({ buttons: ['pdf'] })] }, ctx);
	const disabled = disabledButtons(bar);
	out.disabledButtonCount = disabled.length;
	out.disabledButtonTip = disabled.length ? (disabled[0].dataset.jcTip || '') : null;
	out.disabledTitleAttr = disabled.length ? (disabled[0].getAttribute('title') ?? null) : null;
	out.errorCountAcrossBothActions = r.errors.filter(function (m) { return m.indexOf('E-JS-69') >= 0; }).length;
	out.errorText = r.errors.find(function (m) { return m.indexOf('E-JS-69') >= 0; }) ?? null;
	out.ctxFlagSet = ctx.__exportLibWarned === true;
}

process.stdout.write(JSON.stringify(out));
