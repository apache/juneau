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
 * slot-mount.cjs - always-on Node harness for table-in-slot: JuneauViews.regions.mount({ slotId: { table } }).
 *
 *   Usage:  node slot-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

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
			id: extra && extra.viewId ? extra.viewId : 'releases',
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

(async function () {
	const out = {};

	// =================================================================================================================
	// Inline envelope into an empty slot: table marker, thead, wrapper attrs; no region stamp on the slot.
	// =================================================================================================================
	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		out.t1_emptyBefore = incidents.childNodes.length === 0;
		await Promise.resolve(R.mount({ incidents: { table: envelope(NS) } }));
		const table = incidents.querySelector('table[data-juneau-view="releases"]');
		const wrap = incidents.querySelector('[data-juneau-slot-table]');
		out.t1_hasTable = table != null;
		out.t1_thead = table != null && table.querySelector('thead') != null;
		out.t1_noRegionOnSlot = incidents.getAttribute('data-juneau-region') == null;
		out.t1_wrapperMarker = wrap != null && wrap.getAttribute('data-juneau-slot-table') === '1';
		out.t1_layoutWide = wrap != null && wrap.getAttribute('data-juneau-layout') === 'wide';
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
		const handles = await Promise.resolve(R.mount({
			probes: 'ssc-probes',
			incidents: { table: '/releases/slot' }
		}));
		out.t2_fetchedUrl = fetches[0] === '/releases/slot';
		out.t2_bannerInSlot = incidents.querySelector('.juneau-view-error') != null;
		out.t2_noTable = incidents.querySelector('table[data-juneau-view]') == null;
		out.t2_logged = rec.errorsMatching('envelope GET failed').length >= 1;
		out.t2_regionStayed = probes.getAttribute('data-juneau-region') === 'probes'
			&& probes.childNodes.length === 1;
		out.t2_handleCount = handles && handles.length === 1;
	}

	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		env.setFetch(function () { return Promise.resolve(H.jsonResponse('not-json{')); });
		await Promise.resolve(R.mount({ incidents: { table: '/releases/slot' } }));
		out.t3_malformedBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t3_malformedLogged = rec.errorsMatching('malformed JSON envelope').length >= 1;
	}

	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const bad = envelope(NS);
		bad.contractVersion = '99';
		await Promise.resolve(R.mount({ incidents: { table: bad } }));
		out.t4_versionBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t4_versionLogged = rec.errorsMatching('contract version mismatch').length >= 1;
		out.t4_noTable = incidents.querySelector('table[data-juneau-view]') == null;
	}

	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const fetches = [];
		env.setFetch(function (url) {
			fetches.push(url);
			return Promise.resolve(H.jsonResponse(envelope(NS)));
		});
		await Promise.resolve(R.mount({ incidents: { table: '/releases/slot' } }));
		out.t5_urlUsed = fetches.length === 1 && fetches[0] === '/releases/slot';
		out.t5_tableFromUrl = incidents.querySelector('table[data-juneau-view="releases"]') != null;
	}

	// =================================================================================================================
	// Blank / whitespace table URL throws at validate; no fetch("").
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const fetches = [];
		env.setFetch(function (url) {
			fetches.push(url);
			return Promise.resolve(H.jsonResponse({}, { status: 200 }));
		});
		let threw = false;
		let message = '';
		try {
			R.mount({ incidents: { table: '' } });
		} catch (e) {
			threw = true;
			message = String(e && e.message ? e.message : e);
		}
		out.t6_blankThrew = threw;
		out.t6_blankNamesUrl = message.indexOf('blank or missing table URL') >= 0;
		out.t6_blankNoFetch = fetches.length === 0;
		out.t6_blankLogged = rec.errorsMatching('blank or missing table URL').length >= 1;
		out.t6_blankNotStamped = incidents.getAttribute('data-juneau-region') == null;
	}

	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		slot(env, 'incidents');
		const fetches = [];
		env.setFetch(function (url) {
			fetches.push(url);
			return Promise.resolve(H.jsonResponse({}, { status: 200 }));
		});
		let threw = false;
		try {
			R.mount({ incidents: { table: '   ' } });
		} catch (e) {
			threw = true;
		}
		out.t7_wsThrew = threw;
		out.t7_wsNoFetch = fetches.length === 0;
	}

	// =================================================================================================================
	// Missing slot id / bad shape fails the whole mount (nothing enrolled).
	// =================================================================================================================
	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		R.register('ssc-probes', function () { /* present so mixed map still fails on the missing id */ });
		const probes = slot(env, 'probes');
		let threw = false;
		try {
			R.mount({ probes: 'ssc-probes', incidents: { table: envelope(NS) } });
		} catch (e) {
			threw = true;
		}
		out.t8_missingIdThrew = threw;
		out.t8_probesNotStamped = probes.getAttribute('data-juneau-region') == null;
	}

	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		R.register('ssc-probes', function () {});
		const probes = slot(env, 'probes');
		slot(env, 'incidents');
		let threw = false;
		try {
			R.mount({ probes: 'ssc-probes', incidents: { foo: 1 } });
		} catch (e) {
			threw = true;
		}
		out.t9_badShapeThrew = threw;
		out.t9_probesNotStamped = probes.getAttribute('data-juneau-region') == null;
	}

	// =================================================================================================================
	// CSRF: ancestor copy; data-ssc-csrf is not a substitute; mutating submit fail-closed.
	// =================================================================================================================
	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const shell = env.el('div');
		shell.setAttribute('data-juneau-csrf', 'tok-abc');
		shell.setAttribute('data-juneau-csrf-header', 'X-CSRF-Token');
		const incidents = env.el('div');
		incidents.id = 'incidents';
		shell.appendChild(incidents);
		env.body.appendChild(shell);
		await Promise.resolve(R.mount({ incidents: { table: envelope(NS) } }));
		const table = incidents.querySelector('table[data-juneau-view]');
		out.t10_csrfCopied = table != null && table.getAttribute('data-juneau-csrf') === 'tok-abc';
		out.t10_csrfHeaderCopied = table != null && table.getAttribute('data-juneau-csrf-header') === 'X-CSRF-Token';
	}

	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const shell = env.el('div');
		shell.setAttribute('data-ssc-csrf', 'ssc-secret');
		shell.setAttribute('data-ssc-csrf-header', 'X-Ssc-Csrf');
		const incidents = env.el('div');
		incidents.id = 'incidents';
		shell.appendChild(incidents);
		env.body.appendChild(shell);
		const envl = envelope(NS);
		envl.view.rowActions = [{ id: 'ack', method: 'POST', endpoint: '/ack' }];
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		const table = incidents.querySelector('table[data-juneau-view]');
		const token = table ? table.getAttribute('data-juneau-csrf') : 'leaked';
		out.t11_noJuneauToken = token == null || token === '';
		out.t11_didNotCopySsc = table != null && table.getAttribute('data-ssc-csrf') == null;
		const req = NS.init.buildActionRequest(
			{ id: 'ack', method: 'POST', endpoint: '/ack' },
			token,
			table && table.getAttribute('data-juneau-csrf-header')
		);
		out.t11_missingToken = !!(req && req.refuse && req.reason === 'missing-token');
	}

	// =================================================================================================================
	// Detail template: existing expander DOM + declared {dataUrl} only; no REGION_META sidecar.
	// =================================================================================================================
	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
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
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		const tpl = incidents.querySelector('template[data-juneau-row-detail]');
		out.t12_hasTemplate = tpl != null;
		out.t12_hasHeader = tpl != null && tpl.querySelector('.juneau-view-detail-header') != null;
		const region = tpl && tpl.querySelector('[data-juneau-region="pd-mine-detail"]');
		out.t12_regionType = region != null && region.getAttribute('data-juneau-region-type') === 'row-detail';
		let declared = null;
		try {
			declared = region ? JSON.parse(region.getAttribute('data-juneau-region-declared')) : null;
		} catch (e) { declared = null; }
		out.t12_declaredDataUrlOnly = !!(declared && declared.dataUrl === '/data/{id}'
			&& Object.keys(declared).length === 1);
		out.t12_noRegionMeta = incidents.querySelector('[data-juneau-region-meta]') == null
			&& (tpl == null || tpl.querySelector('[data-juneau-region-meta]') == null);
		out.t12_regionContract = region != null && region.getAttribute('data-juneau-region-contract') === '1';
	}

	// =================================================================================================================
	// Nested: contract "2", no html id on the nested table.
	// =================================================================================================================
	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.detail = {
			contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION,
			endpoint: '/data/{id}',
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
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		const nested = incidents.querySelector('[data-juneau-nested]');
		const nestedTable = nested && nested.querySelector('table[data-juneau-view="child-rows"]');
		out.t13_nestedContract = nested != null && nested.getAttribute('data-juneau-nested-contract') === '2';
		out.t13_nestedNoHtmlId = nestedTable != null && !nestedTable.getAttribute('id');
		out.t13_scopeParam = nested != null && nested.getAttribute('data-juneau-nested-scope-param') === 'parentId';
	}

	// =================================================================================================================
	// Bulk mismatch withholds bulk only.
	// =================================================================================================================
	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.selection = { rowIdField: 'id', selectAll: true };
		envl.bulk = { contractVersion: '99', actions: [{ id: 'ack' }] };
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		const table = incidents.querySelector('table[data-juneau-view]');
		out.t14_hasTable = table != null;
		out.t14_hasSelect = table != null && table.getAttribute('data-juneau-select') === '1';
		out.t14_noBulk = table != null && table.getAttribute('data-juneau-bulk') == null;
		out.t14_logged = rec.errorsMatching('bulk-actions contract version mismatch').length >= 1;
	}

	// =================================================================================================================
	// QuickStats painter + unknown item type fail-loud.
	// =================================================================================================================
	{
		const { env, NS, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.quickStats = {
			contractVersion: '1',
			id: 'qs',
			items: [
				{ id: 'open', label: 'Open', value: '3' },
				{ id: 'used', label: 'Used', value: 2, max: 10 },
				{ id: 'mix', label: 'Mix', segments: [{ count: 1, label: 'a' }] }
			]
		};
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		out.t15_tile = incidents.querySelector('.jc-stat-tile') != null;
		out.t15_bar = incidents.querySelector('.jc-stat-bar') != null;
		out.t15_segments = incidents.querySelector('.jc-stat-segments') != null;
		out.t15_contract = incidents.querySelector('[data-juneau-quickstats-contract="1"]') != null;
	}

	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const incidents = slot(env, 'incidents');
		const envl = envelope(NS);
		envl.quickStats = {
			contractVersion: '1',
			id: 'qs',
			items: [{ id: 'chart', type: 'chart', label: 'Nope' }]
		};
		await Promise.resolve(R.mount({ incidents: { table: envl } }));
		out.t16_unknownBanner = incidents.querySelector('.juneau-view-error') != null;
		out.t16_unknownLogged = rec.errorsMatching('unknown item type').length >= 1;
		out.t16_noTable = incidents.querySelector('table[data-juneau-view]') == null;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
