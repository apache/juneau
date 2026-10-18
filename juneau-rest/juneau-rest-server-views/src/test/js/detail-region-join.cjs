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
 * detail-region-join.cjs - always-on Node harness for the row-detail shared-envelope join.
 *
 *   Usage:  node detail-region-join.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 *
 * THE CLAIM.  A row-detail region whose declared dataUrl is the panel's own substituted endpoint reads the panel's
 * already-in-flight expand envelope instead of issuing a second GET - EVEN WHEN the region appends its `params` to
 * that URL: the join compares the substituted dataUrl BEFORE `region.params` are appended, so the params never
 * make a region fetch separately (and, when it joins, they are never sent).  Controls: a region with no params still
 * joins; a region with an explicit, different dataUrl - including one that differs only by the author's own query
 * such as `?v=2` - still fetches on its own (the priced opt-out).  The URL-resolution branch of the join key is pinned
 * with a stubbed `window.location` (the shim has none, so without the stub the plain-string fallback runs).
 */
'use strict';

const path = require('node:path');
const { load, flush } = require(path.join(__dirname, 'regions-harness.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
const regionsJsPath = process.argv[4];
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node detail-region-join.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};
const ROW_ID = 's-77';
const DETAIL_URL = '/scripts/{id}';

/** Expands one row whose panel carries a single region declared as `declared`; returns the fetch log. */
async function run(declared, href) {
	const { env, I } = load(rendersJsPath, viewsJsPath, regionsJsPath);
	if (href) env.window.location = { href: href };
	const fetches = [];
	let regionEl = null;
	env.setFetch(function (url) {
		fetches.push(String(url));
		const n = fetches.length;
		return Promise.resolve({
			ok: true, status: 200,
			headers: { get: function () { return null; } },
			text: function () { return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: { n: n } })); }
		});
	});

	const host = env.el('div');
	const table = env.el('table');
	const tbody = env.el('tbody');
	table.appendChild(tbody);
	host.appendChild(table);
	env.body.appendChild(host);

	const tpl = env.el('template');
	tpl.dataset.juneauRowDetail = '1';
	tpl.dataset.juneauDetailUrl = DETAIL_URL;
	host.appendChild(tpl);
	tpl.content = {
		cloneNode: function () {
			const wrap = env.el('div');
			const region = env.el('div');
			regionEl = region;
			region.dataset.juneauRegion = 'source';
			region.dataset.juneauRegionType = 'row-detail';
			region.dataset.juneauRegionDeclared = JSON.stringify(declared);
			wrap.appendChild(region);
			return wrap;
		}
	};

	const tr = env.el('tr');
	tr.className = 'juneau-view-detail-row';
	tr.dataset.juneauRowId = ROW_ID;
	const td = env.el('td');
	td.className = 'juneau-view-detail-control';
	tr.appendChild(td);
	tbody.appendChild(tr);

	const state = { shown: false, panel: null };
	const child = function (panel) {
		state.panel = panel;
		return {
			show: function () {
				const childTr = env.el('tr');
				const childTd = env.el('td');
				childTr.appendChild(childTd);
				childTd.appendChild(panel);
				tbody.appendChild(childTr);
				state.shown = true;
			}
		};
	};
	child.isShown = function () { return state.shown; };

	const ctx = { dataTable: { row: function () { return { length: 1, child: child }; } } };
	I.initDetailsExpander(table, ctx, { id: 'v1' });
	table.dispatch('click', { target: td, preventDefault: function () { /* no-op */ }, stopPropagation: function () { /* no-op */ } });
	await flush(12);
	return { fetches: fetches, panel: state.panel, regionEl: regionEl };
}

function count(fetches, prefix) {
	return fetches.filter(function (u) { return u === prefix || u.indexOf(prefix + '?') === 0; }).length;
}

(async function main() {
	const own = '/scripts/' + ROW_ID;

	const withParams = await run({ contractVersion: '1', dataUrl: DETAIL_URL, params: { a: '1', b: 'x y' } });
	out.hasFixture = !!withParams.panel;
	if (!out.hasFixture) { process.stdout.write(JSON.stringify(out)); return; }
	out.a01_withParamsSingleGet = withParams.fetches.length === 1 && count(withParams.fetches, own) === 1;
	out.a01_log = withParams.fetches.join(' ');

	const noParams = await run({ contractVersion: '1', dataUrl: DETAIL_URL });
	out.a02_noParamsSingleGet = noParams.fetches.length === 1 && count(noParams.fetches, own) === 1;

	// The author's own query is a different request: it must NOT join (the documented priced opt-out).
	const authorQuery = await run({ contractVersion: '1', dataUrl: DETAIL_URL + '?v=2', params: { a: '1' } });
	out.a03_authorQueryFetchesSeparately = count(authorQuery.fetches, own) === 2 && authorQuery.fetches.length === 2;

	const other = await run({ contractVersion: '1', dataUrl: '/scripts/{id}/source', params: { a: '1' } });
	out.a04_explicitOtherUrlStillFetches = count(other.fetches, own) === 1
		&& count(other.fetches, own + '/source') === 1 && other.fetches.length === 2;

	// A prefix match is not a join: /scripts/s-77x shares a prefix with the panel endpoint but is a different resource.
	const prefixOnly = await run({ contractVersion: '1', dataUrl: '/scripts/{id}x', params: { a: '1' } });
	out.a05_prefixIsNotAJoin = prefixOnly.fetches.length === 2;

	// Trailing slash is a different spelling the join does not normalize: fail-safe, it fetches twice.
	const trailing = await run({ contractVersion: '1', dataUrl: '/scripts/{id}/' });
	out.a06_trailingSlashFetchesTwice = trailing.fetches.length === 2;

	// URL-resolution branch (window.location stubbed): spellings that resolve to the panel's resource join ...
	const HREF = 'http://h/page'; // NOSONAR javascript:S5332 - test fixture URL, never fetched over the network
	const dotSegment = await run({ contractVersion: '1', dataUrl: '/scripts/./{id}', params: { a: '1' } }, HREF);
	out.a07_dotSegmentResolvesAndJoins = dotSegment.fetches.length === 1;
	const relative = await run({ contractVersion: '1', dataUrl: 'scripts/{id}', params: { a: '1' } }, HREF);
	out.a08_relativeResolvesAndJoins = relative.fetches.length === 1;
	const withParamsLoc = await run({ contractVersion: '1', dataUrl: DETAIL_URL, params: { a: '1' } }, HREF);
	out.a09_paramsJoinUnderLocation = withParamsLoc.fetches.length === 1;
	// A literal dataUrl (no {id}) skips the same-origin check, so absolute spellings reach the join key.
	const absSame = await run({ contractVersion: '1', dataUrl: 'http://h/scripts/s-77' }, HREF); // NOSONAR javascript:S5332 - test fixture URL, never fetched over the network
	out.a12_absoluteSameOriginJoins = absSame.fetches.length === 1;
	const absOther = await run({ contractVersion: '1', dataUrl: 'http://other/scripts/s-77' }, HREF); // NOSONAR javascript:S5332 - test fixture URL, never fetched over the network
	out.a13_crossOriginDoesNotJoin = absOther.fetches.length === 2;
	// ... and a relative path resolved against a DIFFERENT directory is a different resource.
	const otherDir = await run({ contractVersion: '1', dataUrl: 'scripts/{id}' }, 'http://h/a/page'); // NOSONAR javascript:S5332 - test fixture URL, never fetched over the network
	out.a10_relativeUnderOtherDirectoryDoesNotJoin = otherDir.fetches.length === 2;
	// The author's query still separates under URL resolution.
	const queryLoc = await run({ contractVersion: '1', dataUrl: DETAIL_URL + '?v=2' }, HREF);
	out.a11_authorQuerySeparatesUnderLocation = queryLoc.fetches.length === 2;

	// A re-populate (manual refresh) is NOT part of the expand: the initial populate joined the expand GET, but the
	// refresh must issue a real GET - with the region's params - and read the NEW body, not the memoized envelope.
	const refreshed = await run({ contractVersion: '1', dataUrl: DETAIL_URL, params: { a: '1' } });
	const joinedCount = refreshed.fetches.length;
	const refreshCtx = refreshed.regionEl._juneauRegion.ctx;
	refreshCtx.refresh();
	await flush(12);
	out.a14_expandIsOneGet = joinedCount === 1;
	out.a14_refreshIssuesSecondGet = refreshed.fetches.length === 2;
	out.a14_refreshSendsParams = refreshed.fetches[1] === own + '?a=1';
	const fresh = await refreshed.regionEl._juneauRegion.ctx.fetchDeclared();
	out.a14_refreshReadsNewBody = fresh?.n === 3;
	out.a14_log = refreshed.fetches.join(' ');

	process.stdout.write(JSON.stringify(out));
})();
