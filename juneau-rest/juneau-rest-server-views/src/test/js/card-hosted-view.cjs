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
 * card-hosted-view.cjs - Node harness for the identity half of a view table hosted inside a card: the sidecar KEY is
 * the table's minted element id (card-qualified), resolution is scoped to the enclosing card <article>, and the
 * data-juneau-view marker stays the AUTHOR's ViewDef.id.
 *
 * The load-bearing case: two cards hosting the SAME authored view must each read their OWN sidecar - never each
 * other's, and never a same-author-id page-level sidecar.  A table outside any card must keep resolving exactly as it
 * always did (the default-preserving path).
 *
 *   Usage:  node card-hosted-view.cjs <juneau-renders.js> <juneau-views.js>
 *
 * No jQuery/DataTables: construction is stubbed to throw, so we read the ctx beginInitTable stamped BEFORE hand-off.
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node card-hosted-view.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

process.on('unhandledRejection', function () {});

const env = makeEnv();
const sandbox = {
	window: env.window, document: env.document, console: console,
	setTimeout: function (fn) { if (typeof fn === 'function') fn(); return 0; },
	clearTimeout: function () {}, setInterval: function () { return 0; }, clearInterval: function () {},
	Promise: Promise, Set: Set, JSON: JSON, Object: Object, Math: Math, Date: Date,
	fetch: function () { return env.callFetch.apply(env, arguments); }
};
vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const I = env.window.JuneauViews && env.window.JuneauViews.init;
const out = { hasInit: !!(I && typeof I.beginInitTable === 'function' && typeof I.viewSidecarKey === 'function') };
if (!out.hasInit) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

function el(tag, attrs) {
	const n = env.el(tag);
	if (attrs) for (const k in attrs) if (Object.hasOwn(attrs, k)) n.setAttribute(k, attrs[k]);
	return n;
}

// Read the VIEW_META contract off the runtime itself, so a server-side bump never silently turns these scenarios
// into handshake refusals that would still "pass" a naive no-init assertion.
const VIEW_CONTRACT = env.window.JuneauViews.CONTRACT_VERSION;

function sidecar(elementId, viewId, dataUrl) {
	const s = el('script', { 'id': elementId, 'type': 'application/json' });
	s.textContent = JSON.stringify({
		contractVersion: VIEW_CONTRACT, id: viewId, dataMode: 'CLIENT', dataUrl: dataUrl,
		columns: [{ data: 'ref', title: 'Ref' }]
	});
	return s;
}

/** A card <article> hosting one view table: sidecar + table, both keyed by the server-minted (card-qualified) id. */
function cardHosting(cardId, viewId, mintedId, dataUrl) {
	const card = el('article', { 'data-juneau-card': '1', 'data-juneau-card-id': cardId });
	const body = el('div', { 'data-juneau-card-body': '1' });
	card.appendChild(body);
	body.appendChild(sidecar('juneau-view:' + mintedId, viewId, dataUrl));
	const table = el('table', { 'id': mintedId, 'data-juneau-view': viewId });
	body.appendChild(table);
	card._table = table;
	return card;
}

// Construction throws at hand-off: everything before it (sidecar resolution, handshake, ctx) has already happened.
env.window.jQuery = function () { return { DataTable: function () { throw new Error('stub: no real DataTables'); } }; };
env.window.jQuery.fn = { DataTable: function () {}, dataTable: { isDataTable: function () { return false; } } };

// A page-level decoy carrying the AUTHOR id: the sidecar a naive document-wide lookup would find for both cards.
env.body.appendChild(sidecar('juneau-view:orders', 'orders', '/data/DECOY'));

const grid = el('section', { 'data-juneau-card-grid': '1', 'id': 'g1' });
env.body.appendChild(grid);
const c1 = cardHosting('c1', 'orders', 'g1:c1:orders', '/data/one');
const c2 = cardHosting('c2', 'orders', 'g1:c2:orders', '/data/two');
grid.appendChild(c1);
grid.appendChild(c2);

// A) The key IS the minted id, and the marker stays the author id (two different id spaces on one element).
out.key_card1 = I.viewSidecarKey(c1._table);
out.key_card2 = I.viewSidecarKey(c2._table);
out.marker_card1 = c1._table.getAttribute('data-juneau-view');
out.marker_card2 = c2._table.getAttribute('data-juneau-view');

// B) Resolution is scoped to the enclosing card: each table finds the sidecar in its OWN article.
const s1 = I.findSidecarNode('juneau-view:g1:c1:orders', c1._table);
const s2 = I.findSidecarNode('juneau-view:g1:c2:orders', c2._table);
out.scoped_card1InsideCard1 = s1 != null && c1.contains(s1);
out.scoped_card2InsideCard2 = s2 != null && c2.contains(s2);
out.scoped_card1NotInCard2 = s1 != null && !c2.contains(s1);

// C) No cross-wiring: each hosted table inits from its own config, and neither reads the page-level decoy.
try { I.beginInitTable(c1._table); } catch (e) { /* stubbed construction */ }
try { I.beginInitTable(c2._table); } catch (e) { /* stubbed construction */ }
out.init_card1DataUrl = c1._table.__juneauCtx ? c1._table.__juneauCtx.viewDef.dataUrl : null;
out.init_card2DataUrl = c2._table.__juneauCtx ? c2._table.__juneauCtx.viewDef.dataUrl : null;
out.init_neitherReadDecoy = out.init_card1DataUrl !== '/data/DECOY' && out.init_card2DataUrl !== '/data/DECOY';
out.init_separateContexts = c1._table.__juneauCtx !== c2._table.__juneauCtx;

// D) The public per-card emit path omits the grid from the mint; identity still separates two cards.
const lone = cardHosting('solo', 'orders', 'solo:orders', '/data/solo');
env.body.appendChild(lone);
out.key_lone = I.viewSidecarKey(lone._table);
try { I.beginInitTable(lone._table); } catch (e) { /* stubbed construction */ }
out.init_loneDataUrl = lone._table.__juneauCtx ? lone._table.__juneauCtx.viewDef.dataUrl : null;

// E) Default-preserving: a table OUTSIDE any card resolves its page-level sidecar exactly as it always did - both
//    when it carries the author id as its minted id (what the server emits) and, defensively, with no id at all.
const plainWrap = el('div', {});
env.body.appendChild(plainWrap);
plainWrap.appendChild(sidecar('juneau-view:events', 'events', '/data/events'));
const plain = el('table', { 'id': 'events', 'data-juneau-view': 'events' });
plainWrap.appendChild(plain);
out.key_plain = I.viewSidecarKey(plain);
try { I.beginInitTable(plain); } catch (e) { /* stubbed construction */ }
out.init_plainDataUrl = plain.__juneauCtx ? plain.__juneauCtx.viewDef.dataUrl : null;

const idlessWrap = el('div', {});
env.body.appendChild(idlessWrap);
idlessWrap.appendChild(sidecar('juneau-view:legacy', 'legacy', '/data/legacy'));
const idless = el('table', { 'data-juneau-view': 'legacy' });
idlessWrap.appendChild(idless);
out.key_idlessFallsBackToMarker = I.viewSidecarKey(idless);
try { I.beginInitTable(idless); } catch (e) { /* stubbed construction */ }
out.init_idlessDataUrl = idless.__juneauCtx ? idless.__juneauCtx.viewDef.dataUrl : null;

// F) A missing card-scoped sidecar fails closed (no init, no silent fallback to a same-author-id page sidecar).
const orphan = el('article', { 'data-juneau-card': '1', 'data-juneau-card-id': 'c9' });
env.body.appendChild(orphan);
const orphanTable = el('table', { 'id': 'g1:c9:orders', 'data-juneau-view': 'orders' });
orphan.appendChild(orphanTable);
try { I.beginInitTable(orphanTable); } catch (e) { /* no init expected */ }
out.init_missingSidecarRefused = orphanTable.__juneauCtx == null;

process.stdout.write(JSON.stringify(out));
