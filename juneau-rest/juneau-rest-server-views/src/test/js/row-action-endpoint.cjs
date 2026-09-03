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
 * row-action-endpoint.cjs - always-on Node harness for the WORK-J0509 RowAction.endpoint `{property}`
 * substitution: buildActionRequest(action, token, headerName, extra, rowData) and its helper
 * substituteRowActionEndpoint(endpoint, rowData), both pure/DOM-fetch-free.
 *
 *   Usage:  node row-action-endpoint.cjs <path-to-juneau-views.js> [path-to-juneau-renders.js]
 *
 * Loads juneau-views.js (and, when given, juneau-renders.js - the module substituteRowActionEndpoint delegates
 * to for the actual `interpolateHref` token replace) into an isolated vm sandbox with a minimal window/document
 * stub, exactly like row-detail.cjs does for the sibling row-detail helpers.  No browser, no jQuery, no
 * DataTables - buildActionRequest never touches any of them.  Prints ONE JSON object to stdout; every assertion
 * lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node row-action-endpoint.cjs <juneau-views.js> [juneau-renders.js]');
	process.exit(2);
}
const rendersJsPath = process.argv[3];

const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () { return {}; },
	body: { appendChild: function () {}, querySelectorAll: function () { return []; } }
};
const window = { document: document, console: console };
const sandbox = { window: window, document: document, console: console };
sandbox.globalThis = sandbox;

// NOSONAR javascript:S1523 -- this harness's entire purpose is to load the production runtime under test (a
// repo-local file path from argv, not attacker-controlled input) into an isolated VM sandbox; that IS the test.
if (rendersJsPath)
	vm.runInNewContext(fs.readFileSync(path.resolve(rendersJsPath), 'utf8'), sandbox, { filename: 'juneau-renders.js' });
// NOSONAR javascript:S1523 -- same rationale: loading the production juneau-views.js under test into the sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS?.init;
const out = { hasInit: !!(typeof I?.buildActionRequest === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}
out.hasSubstituteHelper = typeof I.substituteRowActionEndpoint === 'function';
out.hasInterpolateHref = typeof NS._render?.interpolateHref === 'function';

const TOKEN = 'tok-123';
const ackAction = { id: 'ack', label: 'Acknowledge', endpoint: 'servlet:/incidents/{id}/ack', method: 'POST' };
const literalAction = { id: 'ack', label: 'Acknowledge', endpoint: 'servlet:/incidents/ack', method: 'POST' };
const multiAction = { id: 'ack', endpoint: '/x/{id}/status/{status}', method: 'POST' };

// Case 1: a `{id}` template resolves against the current row - same token grammar as Column.href.
out.idTemplate_resolved = I.buildActionRequest(ackAction, TOKEN, null, null, { id: 'a1' }).url;

// Case 2: backward compatibility - a literal endpoint with NO `{...}` token is preserved byte-identical,
// with or without a rowData argument at all.
out.literal_withRowData = I.buildActionRequest(literalAction, TOKEN, null, null, { id: 'a1' }).url;
out.literal_noRowData = I.buildActionRequest(literalAction, TOKEN, null, null).url;
out.literal_preFeatureCallSignature = I.buildActionRequest(literalAction, TOKEN, null).url;   // pre-J0509 4-arg call

// Case 3: no-id / null-id row behavior mirrors Column.href's interpolateHref exactly - missing key, explicit
// null, and no rowData at all every substitute to "" (never a thrown error, never a literal `{id}` surviving).
out.noId_missingKey = I.buildActionRequest(ackAction, TOKEN, null, null, {}).url;
out.noId_explicitNull = I.buildActionRequest(ackAction, TOKEN, null, null, { id: null }).url;
out.noId_absentRowData = I.buildActionRequest(ackAction, TOKEN, null, null, null).url;
out.noId_undefinedRowData = I.buildActionRequest(ackAction, TOKEN, null, null, undefined).url;

// Case 4: the substituted value is URL-encoded per-token, exactly like Column.href's interpolateHref.
out.encoded_slashAndSpace = I.buildActionRequest(ackAction, TOKEN, null, null, { id: 'a/1 b' }).url;

// Case 5: more than one `{property}` token resolves - proves this is the SAME generic `{property}` grammar
// Column.href uses, not a hardcoded `{id}`-only special case.
out.multiToken_resolved = I.buildActionRequest(multiAction, TOKEN, null, null, { id: 'a1', status: 'open' }).url;

// Case 6: a null/undefined `action.endpoint` itself is returned as-is by the helper - never stringified to
// the literal text "undefined"/"null".
out.nullEndpoint_helper = I.substituteRowActionEndpoint(null, { id: 'a1' });
out.undefinedEndpointIsUndefined_helper = I.substituteRowActionEndpoint(undefined, { id: 'a1' }) === undefined;

// Case 7: a refusal (safe method / blank token) never even reaches substitution - the refusal marker carries
// no `url` at all, so a refused action can never leak a substituted endpoint.
out.refusal_safeMethod = I.buildActionRequest({ id: 'g', endpoint: '/x/{id}', method: 'GET' }, TOKEN, null, null, { id: 'a1' });
out.refusal_blankToken = I.buildActionRequest(ackAction, '   ', null, null, { id: 'a1' });

// Case 8: substituteRowActionEndpoint itself, direct - the exact helper buildActionRequest delegates to.
out.helper_direct = I.substituteRowActionEndpoint('/x/{id}', { id: 'a1' });
out.helper_noToken = I.substituteRowActionEndpoint('/x/ack', { id: 'a1' });

console.log(JSON.stringify(out));
