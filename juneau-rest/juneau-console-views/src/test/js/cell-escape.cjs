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
 * cell-escape.cjs - always-on Node harness for how juneau-views.js renders DataTables cell data.  Builds the
 * DataTables options for a client-mode, a server-mode and an inline-rows view and reports what each column's
 * render function returns for a markup-bearing value.  Every assertion lives in the Java test (ViewsJs_CellEscape_Test).
 *
 *   Usage:  node cell-escape.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node cell-escape.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
env.window.JuneauDataTables = { ajax: function () { return { url: '/d' }; } };

const MARKUP = '<b>x</b>&"\'';
const columns = [{ data: 'plain' }, { data: 'raw', html: true }];
const out = {};

function cells(opts) {
	return opts.columns.map(function (c) { return c.render ? c.render(MARKUP, 'display', {}) : MARKUP; });
}

const client = cells(I.buildOptions({ contractVersion: '5', id: 'c', dataUrl: '/d', columns: columns }, {}));
const server = cells(I.buildOptions({ contractVersion: '5', id: 's', dataMode: 'server', dataUrl: '/d', columns: columns }, {}));
const inline = cells(I.buildOptions({ contractVersion: '5', id: 'i', dataMode: 'inline', rows: [], columns: columns }, {}));
out.clientPlain = client[0];
out.clientRaw = client[1];
out.serverPlain = server[0];
out.serverRaw = server[1];
out.inlinePlain = inline[0];
out.inlineRaw = inline[1];

// The sort/filter facets of an escaped column still see the raw value.
const sortOpts = I.buildOptions({ contractVersion: '5', id: 'c2', dataUrl: '/d', columns: [{ data: 'plain' }] }, {});
out.sortFacet = sortOpts.columns[0].render(MARKUP, 'sort', {});

process.stdout.write(JSON.stringify(out));
