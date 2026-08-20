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
 * config-application.cjs - always-on Node harness for the juneau-config.js pure config-application layer
 * (TODO-444, slice 4): computeEffectiveColumns / validateView / serializeSavedView / deserializeSavedView /
 * dtIndex (+ buildOptsColumnSpace).
 *
 * No Playwright / Chromium / DOM — loads the real juneau-config.js IIFE against a minimal fake `window`, then
 * exercises the pure functions with plain data.  Driven by ViewsJs_ConfigApplication_Test (always-on when `node`
 * is on PATH; the Java side Assumptions.assumeTrue-skips when node is absent so an offline source-tarball build
 * never hard-depends on Node).
 *
 *   Usage:  node config-application.cjs <path-to-juneau-config.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test (this script only OBSERVES).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const configJsPath = process.argv[2];
if (!configJsPath) {
	console.error('usage: node config-application.cjs <juneau-config.js>');
	process.exit(2);
}

const source = fs.readFileSync(path.resolve(configJsPath), 'utf8');
const window = {};
vm.runInNewContext(source, { window: window, console: console }, { filename: 'juneau-config.js' });

const NS = window.JuneauViews;
const out = { hasConfig: !!(NS && NS.config) };
if (!out.hasConfig) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const C = NS.config;

// ---- catalog fixture (A, B, C) — C has selectable formats; B is hideable; A is pinned ----
const catalog = [
	{ data: 'A', title: 'Col A', pinned: true, render: { id: 'text', meta: { field: 'keep-me' } }, href: '/a/{id}' },
	{ data: 'B', title: 'Col B', defaultVisible: true, render: { id: 'text' } },
	{ data: 'C', title: 'Col C', defaultVisible: true, formats: ['date', 'datetime', 'ts-zulu'],
		render: { id: 'date', meta: { tz: 'UTC' } }, href: '/c/{id}' }
];

// ---- a) Default layering (null saved view) ----
const defaults = C.computeEffectiveColumns(catalog, null);
out.a_defaultOrder = defaults.map(function (c) { return c.data; });
out.a_defaultVisible = defaults.filter(function (c) { return c.visible; }).map(function (c) { return c.data; });
out.a_defaultTitles = defaults.map(function (c) { return c.title; });

// ---- b) Pinned always visible even when omitted from visible[] ----
const pinnedResult = C.validateView(
	{ schemaVersion: 1, visible: ['B', 'C'], order: ['B', 'A', 'C'], labels: {}, formats: {} },
	catalog
);
out.b_pinnedForcedVisible = pinnedResult.ok && pinnedResult.view.visible.indexOf('A') >= 0;
out.b_pinnedOk = pinnedResult.ok;

// ---- c) ≥1 visible — all-hidden blob is repaired (first catalog column forced visible) ----
const allHidden = C.validateView(
	{ schemaVersion: 1, visible: [], order: ['A', 'B', 'C'], labels: {}, formats: {} },
	catalog
);
out.c_atLeastOneVisible = allHidden.ok && allHidden.view.visible.length >= 1;
out.c_repairedVisible = allHidden.ok ? allHidden.view.visible.slice() : null;

// ---- d) Unknown column ids dropped ----
const unknownDropped = C.validateView(
	{ schemaVersion: 1, visible: ['A', 'GONE', 'C'], order: ['GONE', 'C', 'A', 'B'],
		labels: { GONE: 'x', C: 'See' }, formats: { GONE: 'date', C: 'ts-zulu' } },
	catalog
);
out.d_ok = unknownDropped.ok;
out.d_order = unknownDropped.ok ? unknownDropped.view.order : null;
out.d_visible = unknownDropped.ok ? unknownDropped.view.visible : null;
out.d_labels = unknownDropped.ok ? unknownDropped.view.labels : null;
out.d_formats = unknownDropped.ok ? unknownDropped.view.formats : null;

// ---- e) Duplicate order / visible rejected ----
out.e_dupOrder = C.validateView(
	{ schemaVersion: 1, visible: ['A'], order: ['A', 'B', 'A'], labels: {}, formats: {} }, catalog
);
out.e_dupVisible = C.validateView(
	{ schemaVersion: 1, visible: ['A', 'A'], order: ['A', 'B', 'C'], labels: {}, formats: {} }, catalog
);

// ---- f) Format constrained to declared list (undeclared override dropped) ----
const badFmt = C.validateView(
	{ schemaVersion: 1, visible: ['A', 'B', 'C'], order: ['A', 'B', 'C'],
		labels: {}, formats: { C: 'not-a-real-format', B: 'date' } },
	catalog
);
out.f_formatsAfterConstraint = badFmt.ok ? badFmt.view.formats : null;

// ---- g) Renderer meta/href preserved across format swap ----
const reformatted = C.computeEffectiveColumns(catalog, {
	schemaVersion: 1, visible: ['A', 'B', 'C'], order: ['A', 'B', 'C'],
	labels: {}, formats: { C: 'ts-zulu' }
});
const colC = reformatted.find(function (c) { return c.data === 'C'; });
out.g_renderId = colC && colC.render && colC.render.id;
out.g_renderMeta = colC && colC.render && colC.render.meta;
out.g_href = colC && colC.href;

// ---- h) Blank label reverts to catalog title ----
const relabeled = C.computeEffectiveColumns(catalog, {
	schemaVersion: 1, visible: ['A', 'B', 'C'], order: ['A', 'B', 'C'],
	labels: { B: '  ', C: 'Custom C' }, formats: {}
});
out.h_blankReverts = relabeled.find(function (c) { return c.data === 'B'; }).title;
out.h_customKept = relabeled.find(function (c) { return c.data === 'C'; }).title;

// ---- i) Reorder + hide via saved view ----
const reordered = C.computeEffectiveColumns(catalog, {
	schemaVersion: 1, visible: ['A', 'C'], order: ['C', 'A', 'B'], labels: {}, formats: {}
});
out.i_order = reordered.map(function (c) { return c.data; });
out.i_visibility = reordered.map(function (c) { return !!c.visible; });

// ---- j) New catalog column vs old blob — missing-from-order column appended with defaultVisible ----
const catalogWithD = catalog.concat([
	{ data: 'D', title: 'Col D', defaultVisible: false, render: { id: 'text' } }
]);
const oldBlob = C.computeEffectiveColumns(catalogWithD, {
	schemaVersion: 1, visible: ['A', 'B', 'C'], order: ['A', 'B', 'C'], labels: {}, formats: {}
});
out.j_orderIncludesD = oldBlob.map(function (c) { return c.data; });
out.j_D_visible = oldBlob.find(function (c) { return c.data === 'D'; }).visible;

// ---- k) (de)serialize round-trip ----
const draft = {
	visible: ['A', 'C'], order: ['C', 'A', 'B'],
	labels: { C: 'See', B: '' }, formats: { C: 'ts-zulu' }
};
const serialized = C.serializeSavedView(draft);
out.k_serialized = serialized;
const deser = C.deserializeSavedView(JSON.stringify(serialized));
out.k_deserialized = deser;
out.k_blankLabelOmitted = !Object.prototype.hasOwnProperty.call(serialized.labels, 'B');

// ---- l) LOAD-BEARING dtIndex fixture: [sel, A, B(hidden), C, actions] → C is 3, NOT 2 ----
const effForIndex = C.computeEffectiveColumns(catalog, {
	schemaVersion: 1, visible: ['A', 'C'], order: ['A', 'B', 'C'], labels: {}, formats: {}
});
const optsColumns = C.buildOptsColumnSpace(effForIndex, { hasSelection: true, hasActions: true });
out.l_optsDataKeys = optsColumns.map(function (c) { return c.data; });
out.l_optsVisible = optsColumns.map(function (c) { return c.visible; });
out.l_dtIndex_A = C.dtIndex('A', optsColumns);
out.l_dtIndex_B = C.dtIndex('B', optsColumns);
out.l_dtIndex_C = C.dtIndex('C', optsColumns);
out.l_dtIndex_missing = C.dtIndex('Z', optsColumns);
out.l_optsLength = optsColumns.length;

// ---- m) defaultVisible:false first visit (null saved view) ----
const catalogHiddenB = [
	{ data: 'A', title: 'A', pinned: true },
	{ data: 'B', title: 'B', defaultVisible: false },
	{ data: 'C', title: 'C' }
];
const firstVisit = C.computeEffectiveColumns(catalogHiddenB, null);
out.m_firstVisitVisible = firstVisit.filter(function (c) { return c.visible; }).map(function (c) { return c.data; });

process.stdout.write(JSON.stringify(out));
