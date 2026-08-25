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
 * config-chooser.cjs - always-on Node harness for the View-tab chooser:
 * last-column-hide refusal, pinned-unhideable, XSS textContent paint, Default-name reserved,
 * DataTables title sanitization.
 *
 * No Playwright / Chromium — loads the real juneau-config.js IIFE against a minimal fake `window`/`document`.
 * Driven by ViewsJs_ConfigChooser_Test (always-on when `node` is on PATH).
 *
 *   Usage:  node config-chooser.cjs <path-to-juneau-config.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const configJsPath = process.argv[2];
if (!configJsPath) {
	console.error('usage: node config-chooser.cjs <juneau-config.js>');
	process.exit(2);
}

function el(tag) {
	return {
		tagName: String(tag || 'div').toUpperCase(),
		children: [],
		textContent: '',
		innerHTML: '',
		value: '',
		hidden: false,
		disabled: false,
		checked: false,
		className: '',
		style: {},
		classList: { toggle: function () {} },
		setAttribute: function () {},
		getAttribute: function () { return null; },
		addEventListener: function () {},
		appendChild: function (c) { this.children.push(c); return c; },
		removeChild: function (c) {
			this.children = this.children.filter(function (x) { return x !== c; });
			return c;
		},
		querySelector: function () { return null; },
		querySelectorAll: function () { return []; }
	};
}

const body = el('body');
body.appendChild = function (c) { this.children.push(c); c.parentNode = this; return c; };

const document = {
	body: body,
	createElement: el,
	querySelector: function () { return null; },
	querySelectorAll: function () { return []; }
};

const window = { document: document, console: console, prompt: function () { return null; }, confirm: function () { return true; } };
// NOSONAR javascript:S1523 -- loading the production juneau-config.js source into a VM sandbox is this harness's
// intended mechanism for exercising it against a minimal fake window/document; the input is a fixed local file
// path supplied by the test, never attacker-controlled data.
vm.runInNewContext(fs.readFileSync(path.resolve(configJsPath), 'utf8'), { window: window, document: document, console: console }, { filename: 'juneau-config.js' });

const NS = window.JuneauViews;
const out = { hasConfig: !!NS?.config };
if (!out.hasConfig) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const C = NS.config;
out.hasPaintUserText = typeof C.paintUserText === 'function';
out.hasCanHide = typeof C.canHideColumn === 'function';
out.hasMountChooser = typeof C.mountChooser === 'function';
out.hasSanitize = typeof C.sanitizeColumnTitlesForDataTables === 'function';

const catalog = [
	{ data: 'A', title: 'Col A', pinned: true },
	{ data: 'B', title: 'Col B', defaultVisible: true },
	{ data: 'C', title: 'Col C', defaultVisible: true, formats: ['date', 'ts-zulu'] }
];

const oneVisible = { visible: ['A'], order: ['A', 'B', 'C'], labels: {}, formats: {} };
out.lastCannotHideA = C.canHideColumn(oneVisible, catalog, 'A') === false;
out.lastCannotHidePinned = C.canHideColumn(oneVisible, catalog, 'A') === false;

const twoVisible = { visible: ['A', 'B'], order: ['A', 'B', 'C'], labels: {}, formats: {} };
out.pinnedCannotHide = C.canHideColumn(twoVisible, catalog, 'A') === false;
out.unpinnedCanHide = C.canHideColumn(twoVisible, catalog, 'B') === true;
out.alreadyHiddenCanShow = C.canHideColumn(twoVisible, catalog, 'C') === true;

const xssEl = el('span');
xssEl.innerHTML = 'UNTOUCHED';
C.paintUserText(xssEl, '<img src=x onerror=alert(1)>');
out.xssTextContent = xssEl.textContent;
out.xssInnerHtmlUntouched = xssEl.innerHTML === 'UNTOUCHED';

const xssInp = el('input');
xssInp.innerHTML = 'UNTOUCHED';
C.paintUserInput(xssInp, '<img src=x onerror=alert(1)>');
out.xssInputValue = xssInp.value;
out.xssInputInnerHtmlUntouched = xssInp.innerHTML === 'UNTOUCHED';

out.defaultReserved = C.isReservedName('Default') === true;
out.defaultReservedCase = C.isReservedName('DEFAULT') === true;
const basic = C.validateNameBasic('Default');
out.saveAsDefaultRefused = basic?.ok === false;

const cols = [
	{ data: 'A', title: '<img src=x onerror=alert(1)>' },
	{ data: 'B', title: 'Safe', _juneau: 'selection' }
];
C.sanitizeColumnTitlesForDataTables(cols);
out.sanitizedDataTitleBlank = cols[0].title === '';
out.sanitizedSelectionUntouched = cols[1].title === 'Safe';

const table = el('table');
const thead = el('thead');
const tr = el('tr');
const thSel = el('th');
const thA = el('th');
thA.innerHTML = 'UNTOUCHED';
const thB = el('th');
tr.children = [thSel, thA, thB];
thead.children = [tr];
table.querySelector = function (sel) { return sel === 'thead tr' ? tr : null; };
const effective = [
	{ data: 'A', title: '<img src=x onerror=alert(1)>' },
	{ data: 'B', title: 'Col B' }
];
C.paintHeaderTitles(table, effective, { selectionState: {} });
out.headerAText = thA.textContent;
out.headerAInnerHtmlUntouched = thA.innerHTML === 'UNTOUCHED';
out.headerBText = thB.textContent;

const draft = C.defaultDraftFromCatalog(catalog);
out.defaultDraftOrder = draft.order.slice();
out.defaultDraftVisible = draft.visible.slice();
out.moved = C.moveColumn(draft, 'C', -1);
out.orderAfterMove = draft.order.slice();

process.stdout.write(JSON.stringify(out));
