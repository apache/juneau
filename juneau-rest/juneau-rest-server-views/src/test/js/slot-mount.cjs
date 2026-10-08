/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * slot-mount.cjs - always-on Node harness for table-in-slot: JuneauViews.init.mountTableSlot(slot, urlOrEnvelope).
 *
 *   Usage:  node slot-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';
// NOSONAR javascript:S3776 -- test harness encodes a fixture state machine; complexity is inherent.

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node slot-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

function slot(env, id) {
	const el = env.el('div');
	el.id = id;
	env.body.appendChild(el);
	return el;
}

function envelope(NS, extra) {
	const base = {
		contractVersion: NS.SLOT_CONTRACT_VERSION,
		layout: 'wide',
		view: {
			contractVersion: NS.CONTRACT_VERSION,
			id: extra?.viewId ? extra.viewId : 'releases',
			columns: [{ data: 'name', title: 'Name' }]
		}
	};
	if (!extra) return base;
	const viewId = extra.viewId;
	delete extra.viewId;
	const out = Object.assign(base, extra);
	if (viewId) out.view.id = viewId;
	return out;
}

(async function () { // NOSONAR javascript:S3776 -- linear test scenario
	const out = {};

	// =================================================================================================================
	// Inline envelope into an empty slot: table marker, thead, wrapper attrs; no region stamp on the slot.
	// =================================================================================================================
	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		out.t1_emptyBefore = incidents.childNodes.length === 0;
		await Promise.resolve(NS.init.mountTableSlot(incidents, envelope(NS)));
		const table = incidents.querySelector('table[data-juneau-view="releases"]');
		const wrap = incidents.querySelector('[data-juneau-slot-table]');
		out.t1_hasTable = table != null;
		out.t1_thead = table?.querySelector('thead') != null;
		out.t1_noRegionOnSlot = incidents.dataset.juneauRegion == null;
		out.t1_wrapperMarker = wrap?.dataset.juneauSlotTable === '1';
		out.t1_layoutWide = wrap?.dataset.juneauLayout === 'wide';
		out.t1_noErrors = rec.errors.length === 0;
		out.t1_initFromDefExported = typeof NS.init.initTableFromDef === 'function'
			&& typeof NS.init.mountTableSlot === 'function';
	}

	// =================================================================================================================
	// URL fetch; 500 / malformed / version mismatch banner in THAT slot; sibling region stays.
	// =================================================================================================================
	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		R.register('ssc-probes', function (ctx, container) {
			container.appendChild(env.el('span'));
		});
		const probes = slot(env, 'probes');
		const incidents = slot(env, 'incidents');
		const fetches = [];
		env.setFetch(function (url) {
			fetches.push(url);
			return Promise.resolve(H.jsonResponse({}, { status: 500 }));
		});
		const handles = await Promise.resolve(R.mount({ probes: 'ssc-probes' }));
		await Promise.resolve(NS.init.mountTableSlot(incidents, '/releases/slot'));
		out.t2_fetchedUrl = fetches[0] === '/releases/slot';
		out.t2_bannerInSlot = incidents.querySelector('.juneau-view-error') != null;
		out.t2_noTable = incidents.querySelector('table[data-juneau-view]') == null;
		out.t2_logged = rec.errorsMatching('envelope GET failed').length >= 1;
		out.t2_regionStayed = probes.dataset.juneauRegion === 'probes'
			&& probes.childNodes.length === 1;
		out.t2_handleCount = handles?.length === 1;
	}

	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		env.setFetch(function () { return Promise.resolve(H.jsonResponse('not-json{')); });
		await Promise.resolve(NS.init.mountTableSlot(incidents, '/releases/slot'));
		out.t3_malformedBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t3_malformedLogged = rec.errorsMatching('malformed JSON envelope').length >= 1;
	}

	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const bad = envelope(NS);
		bad.contractVersion = '99';
		await Promise.resolve(NS.init.mountTableSlot(incidents, bad));
		out.t4_versionBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t4_versionLogged = rec.errorsMatching('contract version mismatch').length >= 1;
		out.t4_noTable = incidents.querySelector('table[data-juneau-view]') == null;
	}

	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const fetches = [];
		env.setFetch(function (url) {
			fetches.push(url);
			return Promise.resolve(H.jsonResponse(envelope(NS)));
		});
		await Promise.resolve(NS.init.mountTableSlot(incidents, '/releases/slot'));
		out.t5_urlUsed = fetches.length === 1 && fetches[0] === '/releases/slot';
		out.t5_tableFromUrl = incidents.querySelector('table[data-juneau-view="releases"]') != null;
	}

	// =================================================================================================================
	// CSRF: ancestor copy; data-ssc-csrf is not a substitute; mutating submit fail-closed.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const shell = env.el('div');
		shell.dataset.juneauCsrf = 'tok-abc';
		shell.dataset.juneauCsrfHeader = 'X-CSRF-Token';
		const incidents = env.el('div');
		incidents.id = 'incidents';
		shell.appendChild(incidents);
		env.body.appendChild(shell);
		await Promise.resolve(NS.init.mountTableSlot(incidents, envelope(NS)));
		const table = incidents.querySelector('table[data-juneau-view]');
		out.t10_csrfCopied = table?.dataset.juneauCsrf === 'tok-abc';
		out.t10_csrfHeaderCopied = table?.dataset.juneauCsrfHeader === 'X-CSRF-Token';
	}

	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const shell = env.el('div');
		shell.dataset.sscCsrf = 'ssc-secret';
		shell.dataset.sscCsrfHeader = 'X-Ssc-Csrf';
		const incidents = env.el('div');
		incidents.id = 'incidents';
		shell.appendChild(incidents);
		env.body.appendChild(shell);
		const envl = envelope(NS);
		envl.view.rowActions = [{ id: 'ack', method: 'POST', endpoint: '/ack' }];
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		const table = incidents.querySelector('table[data-juneau-view]');
		const token = table ? table.dataset.juneauCsrf : 'leaked';
		out.t11_noJuneauToken = token == null || token === '';
		out.t11_didNotCopySsc = table?.dataset.sscCsrf == null;
		const req = NS.init.buildActionRequest(
			{ id: 'ack', method: 'POST', endpoint: '/ack' },
			token,
			table?.dataset.juneauCsrfHeader
		);
		out.t11_missingToken = !!(req?.refuse && req.reason === 'missing-token');
	}

	// =================================================================================================================
	// Detail template: existing expander DOM + declared {dataUrl} only; no REGION_META sidecar.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.detail = {
			contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION,
			endpoint: '/data/{id}',
			title: 'Incident #{number}',
			icon: 'alert',
			region: {
				id: 'pd-mine-detail',
				type: 'row-detail',
				populate: 'pagerduty-detail',
				dataUrl: '/data/{id}'
			},
			sections: [{
				id: 'details',
				title: 'Details',
				fields: [{ data: 'status', title: 'Status' }]
			}]
		};
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		const tpl = incidents.querySelector('template[data-juneau-row-detail]');
		const dest = tpl?.content ? tpl.content : null;
		out.t12_hasTemplate = tpl != null;
		out.t12_lightDomEmpty = tpl?.childNodes.length === 0;
		out.t12_contentHasChildren = dest?.childNodes.length > 0;
		out.t12_hasHeader = dest?.querySelector('.juneau-view-detail-header') != null;
		const region = dest?.querySelector('[data-juneau-region="pd-mine-detail"]');
		out.t12_regionType = region?.dataset.juneauRegionType === 'row-detail';
		let declared = null;
		try {
			declared = region ? JSON.parse(region.dataset.juneauRegionDeclared) : null;
		} catch (error) { declared = null; } // NOSONAR javascript:S2486 -- an unparsable declaration is recorded as null and asserted on
		out.t12_declaredDataUrlOnly = !!(declared?.dataUrl === '/data/{id}'
			&& Object.keys(declared).length === 1);
		out.t12_noRegionMeta = incidents.querySelector('[data-juneau-region-meta]') == null
			&& tpl?.querySelector('[data-juneau-region-meta]') == null
			&& dest?.querySelector('[data-juneau-region-meta]') == null;
		out.t12_regionContract = region?.dataset.juneauRegionContract === '1';
	}

	// =================================================================================================================
	// Leftover detail.sections[].table must not reconstruct a nested table (F24 host retired with .sections()).
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.detail = {
			contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION,
			endpoint: '/data/{id}',
			region: {
				id: 'pd-mine-detail',
				type: 'row-detail',
				dataUrl: '/data/{id}'
			},
			sections: [{
				id: 'children',
				title: 'Children',
				fields: [{ data: 'name', title: 'Name' }],
				table: {
					contractVersion: NS.NESTED_CONTRACT_VERSION,
					parentScopeParam: 'parentId',
					view: {
						contractVersion: NS.CONTRACT_VERSION,
						id: 'child-rows',
						columns: [{ data: 'name', title: 'Name' }]
					}
				}
			}]
		};
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		const tpl = incidents.querySelector('template[data-juneau-row-detail]');
		const dest = tpl?.content ? tpl.content : tpl;
		out.t13_noNested = dest?.querySelector('[data-juneau-nested]') == null;
		out.t13_hasRegion = dest?.querySelector('[data-juneau-region="pd-mine-detail"]') != null;
		out.t13_noSectionFrame = dest?.querySelector('[data-juneau-detail-section]') == null;
	}

	// =================================================================================================================
	// Bulk mismatch withholds bulk only.
	// =================================================================================================================
	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.selection = { rowIdField: 'id', selectAll: true };
		envl.bulk = { contractVersion: '99', actions: [{ id: 'ack' }] };
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		const table = incidents.querySelector('table[data-juneau-view]');
		out.t14_hasTable = table != null;
		out.t14_hasSelect = table?.dataset.juneauSelect === '1';
		out.t14_noBulk = table?.dataset.juneauBulk == null;
		out.t14_logged = rec.errorsMatching('bulk-actions contract version mismatch').length >= 1;
	}

	// =================================================================================================================
	// QuickStats painter + unknown item type fail-loud.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.quickStats = {
			contractVersion: '1',
			id: 'qs',
			items: [
				{ id: 'open', label: 'Open', value: '3' },
				{ id: 'warnTile', label: 'Warn', value: '1', tone: 'warning' },
				{ id: 'accentTile', label: 'Accent', value: '2', tone: 'accent' },
				{ id: 'used', label: 'Used', value: 2, max: 10 },
				{ id: 'mix', label: 'Mix', segments: [{ count: 1, label: 'a' }] }
			]
		};
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		out.t15_tile = incidents.querySelector('.jc-stat-tile') != null;
		out.t15_bar = incidents.querySelector('.jc-stat-bar') != null;
		out.t15_segments = incidents.querySelector('.jc-stat-segments') != null;
		out.t15_contract = incidents.querySelector('[data-juneau-quickstats-contract="1"]') != null;
		// A status-tone token paints `is-<tone>`; an off-palette value (the retired `accent`) paints no modifier at all.
		const toneValue = function (id) {
			const tile = incidents.querySelector('[data-juneau-stat="' + id + '"]');
			return tile?.querySelector('.jc-stat-value')?.className;
		};
		out.t15_toneWarningClass = toneValue('warnTile');
		out.t15_toneAccentClass = toneValue('accentTile');
	}

	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.quickStats = {
			contractVersion: '1',
			id: 'qs',
			items: [{ id: 'chart', type: 'chart', label: 'Nope' }]
		};
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		out.t16_unknownBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t16_unknownLogged = rec.errorsMatching('unknown item type').length >= 1;
		out.t16_noTable = incidents.querySelector('table[data-juneau-view]') == null;
	}

	// =================================================================================================================
	// rows-only table.  No dataUrl: no fetch, no opts.ajax, whole row objects handed over as opts.data,
	// plain-text columns forced through an escaping renderer; selection.rowIdField is kept on the table.
	// =================================================================================================================
	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		let fetched = 0;
		env.setFetch(function () { fetched++; return Promise.resolve(H.jsonResponse([])); });
		const rows = [
			{ id: 7, name: '<img src=x onerror=alert(1)>', hidden: 'kept' },
			{ id: 8, name: 'a&b', hidden: 'kept2' }
		];
		const envl = envelope(NS, { rows: rows, selection: { rowIdField: 'id', selectAll: true } });
		await Promise.resolve(NS.init.mountTableSlot(incidents, envl));
		const table = incidents.querySelector('table[data-juneau-view="releases"]');
		out.t17_hasTable = table != null;
		out.t17_noFetch = fetched === 0;
		out.t17_rowsStashed = table?.__juneauRows === rows;
		out.t17_rowIdField = table?.dataset.juneauRowIdField === 'id';
		out.t17_noDomRows = table?.querySelector('tbody') == null;
		const opts = NS.init.buildOptions(envl.view, {
			table: table, parseRenderId: NS.parseRenderId, resolveRenderer: NS.resolveRenderer, warn: function () {}
		});
		out.t17_noAjax = opts.ajax === undefined;
		out.t17_clientSide = opts.serverSide === false;
		out.t17_dataIsRows = opts.data === rows && opts.data[0].hidden === 'kept';
		const render = opts.columns[0].render;
		out.t17_hasRender = typeof render === 'function';
		out.t17_escapes = render(rows[0].name, 'display') === '&lt;img src=x onerror=alert(1)&gt;'
			&& render(rows[1].name, 'display') === 'a&amp;b';
		out.t17_rawForSort = render(rows[1].name, 'sort') === 'a&b';
		out.t17_nullBlank = render(null, 'display') === '';
		out.t17_noErrors = rec.errors.length === 0;

		// A dataUrl-bearing view keeps its ajax even when rows ride along (rows are then only the first-paint seed).
		const seedTable = env.el('table');
		const seeded = NS.init.buildOptions({ contractVersion: NS.CONTRACT_VERSION, id: 'seeded', dataUrl: '/d',
			columns: [{ data: 'name' }] }, { table: seedTable, parseRenderId: NS.parseRenderId,
			resolveRenderer: NS.resolveRenderer, warn: function () {} });
		out.t17_dataUrlKeepsAjax = seeded.ajax?.url === '/d' && seeded.data === undefined;
	}

	// =================================================================================================================
	// detail.endpoint must be same-origin; cross-origin warns and is not stamped.
	// =================================================================================================================
	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		env.window.location = { href: 'https://app.example.com/ctx/page', origin: 'https://app.example.com' };
		function mountOne(id, extra) {
			const el = slot(env, id);
			return Promise.resolve(NS.init.mountTableSlot(el, envelope(NS, { viewId: 'v' + id, ...extra }))).then(function () {
				return {
					detail: el.querySelector('template[data-juneau-row-detail]')?.dataset.juneauDetailUrl
				};
			});
		}
		function det(endpoint) {
			return { contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION, endpoint: endpoint };
		}
		const rel = await mountOne('s1', { detail: det('detail/{id}') });
		const abs = await mountOne('s2', {
			detail: det('https://app.example.com/ctx/d/{id}') });
		out.t18_relativeKept = rel.detail === 'detail/{id}';
		out.t18_sameOriginAbsKept = abs.detail === 'https://app.example.com/ctx/d/{id}';
		out.t18_noWarnYet = rec.warnsMatching('not same-origin').length === 0;
		const x1 = await mountOne('s3', {
			detail: det('https://evil.example.org/d/{id}') });
		const x2 = await mountOne('s4', { detail: det('//evil.example.org/d') });
		const x3 = await mountOne('s5', { detail: det('http://app.example.com/d/{id}') });
		out.t18_crossOriginNotStamped = x1.detail === undefined && x2.detail === undefined && x3.detail === undefined;
		out.t18_warned = rec.warnsMatching('detail.endpoint').length === 3;
	}

	// =================================================================================================================
	// The document-wide table scan leaves a slot-built table alone: the envelope is its definition (no sidecar), and
	// the slot mount already inits it.
	// =================================================================================================================
	{
		const { env, NS, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		await Promise.resolve(NS.init.mountTableSlot(incidents, envelope(NS)));
		out.t19_tableMounted = incidents.querySelector('table[data-juneau-view="releases"]') != null;
		NS.init.initAll();
		out.t19_scanLogsNoMissingSidecar = rec.errorsMatching('missing JSON sidecar').length === 0;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (error) {
	process.stderr.write(String(error?.stack ? error.stack : error));
	process.exit(1);
});
