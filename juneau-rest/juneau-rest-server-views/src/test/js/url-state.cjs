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
 * url-state.cjs - always-on Node harness for the shareable "Copy link" URL-state codec:
 * tab/filter/sort round-trip, nested-table + View-Settings omission, address-bar sync, clean-address, open
 * precedence.
 *
 * No Playwright / Chromium - loads the real juneau-urlstate.js IIFE against a minimal fake `window`.
 * Driven by ViewsJs_UrlState_Test (always-on when `node` is on PATH).
 *
 *   Usage:  node url-state.cjs <path-to-juneau-urlstate.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const urlStateJsPath = process.argv[2];
if (!urlStateJsPath) {
	console.error('usage: node url-state.cjs <juneau-urlstate.js>');
	process.exit(2);
}

const window = {};
// NOSONAR javascript:S1523 -- loading the production juneau-urlstate.js source into a VM sandbox is this harness's
// intended mechanism for exercising it against a minimal fake window; the input is a fixed local file path
// supplied by the test, never attacker-controlled data.
vm.runInNewContext(fs.readFileSync(path.resolve(urlStateJsPath), 'utf8'), { window: window, console: console }, { filename: 'juneau-urlstate.js' });

const NS = window.JuneauViews;
const out = { hasUrlState: !!(NS?.urlState) };
if (!out.hasUrlState) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

const U = NS.urlState;

// -------------------------------------------------------------------------------------------------------------
// a) round-trip: tab + multi-column filter (raw $-DSL) + sort
// -------------------------------------------------------------------------------------------------------------
const state = {
	tab: 'setup',
	filters: [
		{ column: 'status', expr: '$in(OPEN,CLOSED)' },
		{ column: 'name', expr: '$eq(a,b)' }
	],
	sort: { column: 'name', dir: 'desc' }
};
out.encoded = U.encode(state);
const decoded = U.decode(out.encoded);
out.decodedTab = decoded.tab;
out.decodedFilters = decoded.filters.map(function (f) { return f.column + '=' + f.expr; });
out.decodedSort = decoded.sort ? decoded.sort.column + '=' + decoded.sort.dir : null;

// The raw $-DSL body (with its own commas and nested parens) survives the round-trip intact.
out.rawFilterPreserved = decoded.filters[0].expr === '$in(OPEN,CLOSED)';

// -------------------------------------------------------------------------------------------------------------
// a2) ClauseParser grammar (shared with the one `search`/`opts` string): escaped separators in a column name
//     round-trip, a filter body may carry MULTIPLE comma-separated clauses, and only the FIRST unescaped `=` splits
// -------------------------------------------------------------------------------------------------------------
// A column name carrying the grammar's own comma/equals is escaped on encode and restored verbatim on decode.
out.escapedColEncoded = U.encode({ filters: [{ column: 'a,b=c', expr: '$eq(1)' }] });
out.escapedColDecoded = U.decode(out.escapedColEncoded).filters.map(function (f) { return f.column + '|' + f.expr; });

// A column name starting with "$" is escaped on encode (so it can never be misread as the start of a function-call
// token when the state value is re-parsed) and restored verbatim on decode.
out.dollarColEncoded = U.encode({ filters: [{ column: '$weird', expr: '$eq(1)' }] });
out.dollarColDecoded = U.decode(out.dollarColEncoded).filters.map(function (f) { return f.column + '|' + f.expr; });

// ONE filter body carrying several top-level clauses decodes to one filter each; a comma INSIDE $in(...) is
// protected and does not split the clause.
out.multiClauseFilters = U.decode('filter(status=$eq(OPEN),name=$in(a,b),score=$gt(5))')
	.filters.map(function (f) { return f.column + '=' + f.expr; });

// Only the FIRST UNESCAPED `=` splits key from value; a later `=` (here inside the $-expression) stays verbatim.
const kv = U.decode('filter(expr=$eq(a=b))').filters[0];
out.firstEqualsSplit = kv.column + '|' + kv.expr;

// -------------------------------------------------------------------------------------------------------------
// b) an empty state encodes to "" and empty facets are omitted, never emitted blank
// -------------------------------------------------------------------------------------------------------------
out.emptyEncodes = U.encode({ tab: null, filters: [], sort: null });
out.blankFacetsDropped = U.encode({ tab: '', filters: [{ column: 'x', expr: '' }, { column: '', expr: '$eq(1)' }], sort: { column: 'c', dir: '' } });
out.isEmptyOfDecodedEmpty = U.isEmptyState(U.decode(''));
out.isEmptyOfDecodedFull = U.isEmptyState(decoded);

// -------------------------------------------------------------------------------------------------------------
// c) unknown directives (e.g. an IRS `subview`, or anything View-Settings-shaped) are dropped on decode
// -------------------------------------------------------------------------------------------------------------
const hostile = U.decode('tab(main);subview(nested=xyz);visible(a,b);filter(status=$eq(OK));options(pageSize=50)');
out.hostileTab = hostile.tab;
out.hostileFilters = hostile.filters.map(function (f) { return f.column + '=' + f.expr; });
out.hostileSort = hostile.sort;
// Nothing named subview/visible/options ever became a facet.
out.hostileEncoded = U.encode(hostile);

// -------------------------------------------------------------------------------------------------------------
// d) readFromSearch pulls `state` out of a full query string; absent -> null
// -------------------------------------------------------------------------------------------------------------
out.readFromFull = (function () {
	const r = U.readFromSearch('?foo=1&state=tab(x);sort(name=asc)&bar=2');
	return { tab: r.tab, sort: r.sort ? r.sort.column + '=' + r.sort.dir : null };
})();
out.readAbsentIsNull = U.readFromSearch('?foo=1&bar=2') === null;
out.readEmptyIsNull = U.readFromSearch('') === null;
// A percent-encoded value still decodes.
out.readEncoded = (function () {
	const enc = 'state=' + encodeURIComponent('filter(status=$in(OPEN,CLOSED))');
	const r = U.readFromSearch('?' + enc);
	return r.filters[0].column + '=' + r.filters[0].expr;
})();

// -------------------------------------------------------------------------------------------------------------
// e) buildSearch replaces `state`, keeps other params, and drops it when empty
// -------------------------------------------------------------------------------------------------------------
out.buildReplaces = U.buildSearch('?foo=1&state=OLD&bar=2', 'tab(x)');
out.buildDropsWhenEmpty = U.buildSearch('?foo=1&state=OLD', '');
out.buildAddsToBare = U.buildSearch('', 'tab(x)');

// -------------------------------------------------------------------------------------------------------------
// f) writeToAddressBar syncs via history.replaceState; clean-address is a no-op
// -------------------------------------------------------------------------------------------------------------
const historyCalls = [];
const fakeHistory = { state: { some: 'prev' }, replaceState: function (s, t, url) { historyCalls.push(url); } };
const fakeLocation = { pathname: '/rest/reports', search: '?page=2', hash: '#frag', origin: 'https://h' };
out.wroteAddressBar = U.writeToAddressBar(fakeHistory, fakeLocation, { tab: 'x', filters: [], sort: null }, {});
out.addressBarUrl = historyCalls[0];
historyCalls.length = 0;
out.cleanIsNoOp = U.writeToAddressBar(fakeHistory, fakeLocation, { tab: 'x' }, { clean: true });
out.cleanWroteNothing = historyCalls.length === 0;

// -------------------------------------------------------------------------------------------------------------
// g) buildShareUrl ALWAYS carries ?state=, even under clean-address (that's the point of Copy link)
// -------------------------------------------------------------------------------------------------------------
out.shareUrl = U.buildShareUrl(fakeLocation, { tab: 'setup', filters: [{ column: 'status', expr: '$eq(OK)' }], sort: null });

// -------------------------------------------------------------------------------------------------------------
// h) copy(navigator, text) resolves true on success, false when the API is absent - never throws
// -------------------------------------------------------------------------------------------------------------
(async function () {
	let copied = null;
	const goodNav = { clipboard: { writeText: function (t) { copied = t; return Promise.resolve(); } } };
	out.copyOk = await U.copy(goodNav, 'HELLO');
	out.copiedText = copied;
	out.copyNoApi = await U.copy({}, 'x');
	const throwingNav = { clipboard: { writeText: function () { return Promise.reject(new Error('denied')); } } };
	out.copyRejected = await U.copy(throwingNav, 'x');

	// ---------------------------------------------------------------------------------------------------------
	// i) clean-address opt-in read from VIEW_META boolean or the host table's data-attribute
	// ---------------------------------------------------------------------------------------------------------
	out.cleanFromMeta = U.cleanAddressEnabled({ cleanAddress: true }, null);
	out.cleanFromAttr = U.cleanAddressEnabled(null, { dataset: { juneauCleanAddress: 'true' } });
	out.cleanDefaultFalse = U.cleanAddressEnabled({}, { dataset: {} });

	// ---------------------------------------------------------------------------------------------------------
	// j) open precedence: ?state= wins for tab/filter/sort; an empty URL state falls back to the store
	// ---------------------------------------------------------------------------------------------------------
	const urlState = U.decode('tab(fromUrl)');
	const storeState = { tab: 'fromStore' };
	out.precedenceUrlWins = U.resolveOpenState(urlState, storeState).tab;
	out.precedenceFallsBackToStore = U.resolveOpenState(U.decode(''), storeState).tab;
	out.precedenceNullWhenBothEmpty = U.resolveOpenState(U.decode(''), null);

	process.stdout.write(JSON.stringify(out));
})();
