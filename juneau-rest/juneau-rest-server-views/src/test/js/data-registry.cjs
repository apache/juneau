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
'use strict';
// node data-registry.cjs <juneau-bus.js> <juneau-data.js>: prints one JSON report line per check.
const path = require('node:path');
const { makeEnv, loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));
const [busJs, dataJs] = process.argv.slice(2);
const out = {};
const env = loadScripts([busJs, dataJs], makeEnv());
const D = env.NS.data;
const f1 = { rowCount: 2, truncated: false, dims: [{ name: 'x', type: 'number' }], columns: { x: new Float64Array([1, 2]) } };
const v1 = D.put('s', f1);
out.a01_put_returnsVersion1 = v1;
out.a02_get_sameColumnsByReference = D.get({ source: 's', version: 1, status: 'ready', dims: f1.dims }).columns.x === f1.columns.x;
const v2 = D.put('s', { ...f1 });
out.a03_previousHeldDuringSwap = D.get({ source: 's', version: 1, status: 'ready', dims: [] }) !== null && v2 === 2;
D.put('s', { ...f1 });
out.a04_twoBackDropped = D.get({ source: 's', version: 1, status: 'ready', dims: [] }) === null;
out.a05_unknownSource_null = D.get({ source: 'nope', version: 1, status: 'ready', dims: [] }) === null;
D.building('s', f1);
out.a06_partial = D.get({ source: 's', version: 3, status: 'loading', dims: [], partial: true }) === f1;
D.building('s', null);
out.a07_partialCleared_null = D.get({ source: 's', version: 3, status: 'loading', dims: [], partial: true }) === null;
out.a08_isHandle = [
	D.isHandle({ schemaVersion: 1, source: 's', version: 0, status: 'loading', dims: [] }),
	D.isHandle({ source: 's', version: '1', status: 'ready', dims: [] }),
	D.isHandle({ source: 's', version: 1, status: 'weird', dims: [] }),
	D.isHandle(null)
];
D.release('s');
out.a09_release = D.get({ source: 's', version: 3, status: 'ready', dims: [] }) === null;
process.stdout.write(JSON.stringify(out) + '\n');
