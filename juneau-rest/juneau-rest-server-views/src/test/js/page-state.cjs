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
 * page-state.cjs - always-on Node harness for the general page-state store (design section 6.2).  Loads
 * juneau-pagestate.js standalone (it attaches to window.JuneauViews without views/renders), then exercises: two
 * tables keeping separate per-table keys, a replaced store implementation actually being used, and a blocked store
 * degrading to a no-op without throwing.  Every assertion lives in the Java test.
 *
 *   Usage:  node page-state.cjs <juneau-pagestate.js>
 */
'use strict';

const path = require('node:path');
const { loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const pageStateJsPath = process.argv[2];
if (!pageStateJsPath) {
	console.error('usage: node page-state.cjs <juneau-pagestate.js>');
	process.exit(2);
}

const { env, NS } = loadScripts([pageStateJsPath]);
const ps = NS?.pageState;
const out = { hasPageState: !!(ps && typeof ps.table === 'function' && typeof ps.page === 'function') };
if (!out.hasPageState) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

// --- Two tables keep SEPARATE per-table keys (no clobber), and page-level state is independent again ----------
const t1 = ps.table('releases');
const t2 = ps.table('users');
t1.set('visibleCols', ['name', 'status']);
t2.set('visibleCols', ['id']);              // same NAME, different table key
ps.page('nav').set('tab', 'setup');
out.table1Cols = t1.get('visibleCols');     // expect ['name','status']
out.table2Cols = t2.get('visibleCols');     // expect ['id'] - not clobbered by table1
out.pageTab = ps.page('nav').get('tab');    // expect 'setup'
// The default store is the shim's localStorage: the two tables must occupy DISTINCT keys.
out.storeKeys = env.window.localStorage._dump ? Object.keys(env.window.localStorage._dump()).sort((a, b) => Number(a > b) - Number(a < b)) : [];

// --- A JSON value round-trips through the default store; a missing key reads as null -------------------------
t1.set('density', { rows: 'compact', wrap: false });
out.roundTrip = t1.get('density');
out.missingKey = t1.get('nope');            // expect null

// --- A replaced store implementation is actually used ---------------------------------------------------------
const recorded = {};
ps.useStore({
	getItem: function (k) { return Object.hasOwn(recorded, k) ? recorded[k] : null; },
	setItem: function (k, v) { recorded[k] = v; },
	removeItem: function (k) { delete recorded[k]; }
});
ps.table('orders').set('pageIndex', 3);
out.replacedStoreRecorded = Object.keys(recorded).length > 0;
out.replacedStoreValue = ps.table('orders').get('pageIndex');   // expect 3, read back through the custom store
// The write landed in the custom store, NOT in the (now-bypassed) default localStorage.
out.defaultStoreUntouchedByReplaced =
	Object.keys(env.window.localStorage._dump()).every(function (k) { return k.indexOf('orders') < 0; });

// --- A blocked store degrades to a no-op WITHOUT throwing -----------------------------------------------------
ps.useStore({
	getItem: function () { throw new Error('blocked'); },
	setItem: function () { throw new Error('blocked'); },
	removeItem: function () { throw new Error('blocked'); }
});
let threw = false;
try {
	ps.table('blocked').set('x', 1);
	out.blockedGet = ps.table('blocked').get('x');   // expect null - blocked read swallowed
} catch (error) {
	threw = true;
}
out.blockedNoThrow = !threw;

process.stdout.write(JSON.stringify(out));
