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

// Case 1: a `{id}` template resolves against the current row - same token grammar as Column.href.  The whole
// marker is captured (not `.url`) because this SAME call, re-run by the harness a second time with
// juneau-renders.js absent, becomes a WORK-J0521/S5 refusal (`unresolved-endpoint`) rather than a fired URL.
out.idTemplate_resolved = I.buildActionRequest(ackAction, TOKEN, null, null, { id: 'a1' });

// Case 2: backward compatibility - a literal endpoint with NO `{...}` token is preserved byte-identical,
// with or without a rowData argument at all.
out.literal_withRowData = I.buildActionRequest(literalAction, TOKEN, null, null, { id: 'a1' }).url;
out.literal_noRowData = I.buildActionRequest(literalAction, TOKEN, null, null).url;
out.literal_preFeatureCallSignature = I.buildActionRequest(literalAction, TOKEN, null).url;   // pre-J0509 4-arg call

// Case 3 (WORK-J0521, B1b flip): a missing/null/absent-rowData row value for a `{property}` token now REFUSES
// the write (empty-substitution would otherwise collapse the URL to a malformed "/x//y") rather than firing the
// substituted-to-empty-string URL - so the whole marker is captured, not `.url` (a refusal carries no `url`).
out.noId_missingKey = I.buildActionRequest(ackAction, TOKEN, null, null, {});
out.noId_explicitNull = I.buildActionRequest(ackAction, TOKEN, null, null, { id: null });
out.noId_absentRowData = I.buildActionRequest(ackAction, TOKEN, null, null, null);
out.noId_undefinedRowData = I.buildActionRequest(ackAction, TOKEN, null, null, undefined);

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

// -----------------------------------------------------------------------------------------------------------
// WORK-J0521: write-path URL-safety hardening for RowAction.endpoint substitution - B1 (`..` path-walk +
// empty-substitution refusal), S3 (row-less inheritance, closed for free), S5 (residual-token refusal).
// -----------------------------------------------------------------------------------------------------------

// Case 9 (B1a, the headline): a row value of `..` rides encodeURIComponent unescaped (`.` is RFC 3986
// unreserved) and must be refused, not fired at the browser-normalized `/ack`.
out.dotdot_rowValue = I.buildActionRequest(ackAction, TOKEN, null, null, { id: '..' });

// Case 10: an author-declared `..` in the template itself, no row value involved - the deliberate consequence
// noted in the design (§4.5): the resolved-URL check also catches a nonsensical declared endpoint.
out.dotdot_inTemplate = I.buildActionRequest(
	{ id: 'ack', endpoint: 'servlet:/incidents/../ack', method: 'POST' }, TOKEN, null, null, { id: 'a1' });

// Case 11 (B1b trim widening): a whitespace-only row value is blank per isBlankToken, so it refuses exactly
// like a missing/null value - never a real target.
out.whitespaceOnly_rowValue = I.buildActionRequest(ackAction, TOKEN, null, null, { id: '   ' });

// Case 12 (B1c, the no-endpoint guard): a blank/absent/whitespace action.endpoint refuses before any
// substitution work, mirroring buildJobCancelRequest's no-cancel-url precedent.
out.blankEndpoint_absent = I.buildActionRequest({ id: 'ack', method: 'POST' }, TOKEN, null, null, { id: 'a1' });
out.blankEndpoint_null = I.buildActionRequest({ id: 'ack', endpoint: null, method: 'POST' }, TOKEN, null, null, { id: 'a1' });
out.blankEndpoint_empty = I.buildActionRequest({ id: 'ack', endpoint: '', method: 'POST' }, TOKEN, null, null, { id: 'a1' });
out.blankEndpoint_whitespace = I.buildActionRequest({ id: 'ack', endpoint: '   ', method: 'POST' }, TOKEN, null, null, { id: 'a1' });

// Case 13 (Terra should-fix, query-context coverage): a row value cannot inject `&`/`=`/`#` into a query
// string - encodeURIComponent already blocks this; pinning it as explicit coverage per the design's §8.1.
out.queryContext_resolved = I.buildActionRequest(
	{ id: 'ack', endpoint: 'servlet:/incidents/ack?target={id}', method: 'POST' }, TOKEN, null, null, { id: 'a&b=c#d' });

// Case 14 (S3, row-less ribbon-dialog inheritance): rowData === null, matching rowDataForTr(ctx, null) on the
// row-less ribbon-dialog seam - the SAME guard (empty-substitution) that b03/noId_absentRowData proves, named
// separately so a reader tracing S3 finds a case for it.
out.rowLess_idTemplate = I.buildActionRequest(ackAction, TOKEN, null, null, null);

// Case 15: `..` INSIDE an encodeURIComponent-escaped value must NOT refuse - proves hasDotDotSegment is not
// over-broad (a careless `url.includes("..")` implementation would get this wrong; the encoded slashes mean
// there is no `..` PATH SEGMENT in the resolved URL).
out.dotdot_encodedInsideValue = I.buildActionRequest(ackAction, TOKEN, null, null, { id: 'a/../b' });

// Case 16: one blank token among several - the guard is per-token, not "the first token".
out.multiToken_oneBlank = I.buildActionRequest(multiAction, TOKEN, null, null, { id: 'a1' });

console.log(JSON.stringify(out));
