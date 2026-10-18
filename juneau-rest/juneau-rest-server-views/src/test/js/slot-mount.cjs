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
	// Leftover detail.sections[].table must not reconstruct a nested table (host retired with .sections()).
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
	// Header title placeholders: a header-only detail allowlists exactly the {key}s its title names; a region's
	// titleFields still apply; a title with no placeholders allowlists nothing.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let n = 0;
		async function titleFor(detail, fields) {
			const el = slot(env, 'tf' + (++n));
			await Promise.resolve(NS.init.mountTableSlot(el, envelope(NS, { viewId: 'tf' + n, detail: detail })));
			const tpl = el.querySelector('template[data-juneau-row-detail]');
			const h2 = tpl?.content?.querySelector('[data-juneau-detail-title]');
			if (!h2) return { allow: null, text: null };
			const wrap = env.el('div');
			const allow = tpl.dataset.juneauTitleFields;
			if (allow != null) wrap.dataset.juneauTitleFields = allow;
			wrap.appendChild(h2);
			NS.init.fillDetailSlots(wrap, fields);
			return { allow: allow == null ? null : allow, text: h2.textContent };
		}
		function det(extra) {
			return { contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION, endpoint: '/o/{id}', ...extra };
		}
		const hdr = await titleFor(det({ title: 'Order {id}' }), { id: '42', secret: 'leak' });
		out.tf_headerOnlyRendersId = hdr.text === 'Order 42';
		out.tf_headerOnlyAllowlist = hdr.allow === 'id';
		const two = await titleFor(det({ title: '{id} for {customer}' }), { id: '7', customer: 'Ann', secret: 'leak' });
		out.tf_headerOnlyNamedKeysOnly = two.text === '7 for Ann' && two.text.indexOf('leak') < 0;
		const unnamed = await titleFor(det({ title: 'Order {id}' }), { secret: 'leak', id: '' });
		out.tf_unnamedFieldNotLeaked = unnamed.text === 'Order ' && unnamed.text.indexOf('leak') < 0;
		const unnamed2 = await titleFor(det({ title: 'Order {id} {other}' }), { id: '1', other: 'x', secret: 'leak' });
		out.tf_unlistedPlaceholderInDataNotAllowlisted = unnamed2.allow === 'id,other' && unnamed2.text.indexOf('leak') < 0;
		const reg = await titleFor(det({ title: 'Order {id}',
			region: { id: 'r', type: 'row-detail', titleFields: ['status'] } }), { id: '5', status: 'open' });
		out.tf_regionTitleFieldsStillWork = reg.allow !== null && reg.allow.split(',').indexOf('status') >= 0
			&& reg.text.indexOf('open') < 0;
		const regOnly = await titleFor(det({ title: 'State {status}',
			region: { id: 'r', type: 'row-detail', titleFields: ['status'] } }), { status: 'open', secret: 'leak' });
		out.tf_regionTitleRenders = regOnly.text === 'State open';
		const plain = await titleFor(det({ title: 'Details' }), { id: '1' });
		out.tf_noPlaceholders = plain.text === 'Details' && (plain.allow === null || plain.allow === '');
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

	// =================================================================================================================
	// A datatables card's API resolves the slot-built table: selection() reads its selected ids and reload() redraws it.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const host = slot(env, 'pets');
		let api = null;
		const cardCtx = {
			document: env.document,
			setApi: function (a) { api = a; },
			onDestroy: function () {},
			isAbort: function () { return false; },
			fetchJson: function () { return Promise.resolve({}); },
			paintError: function () {}
		};
		const card = { id: 'pets', type: 'datatables' };
		card.table = { rows: [{ id: 'a', name: 'A' }, { id: 'b', name: 'B' }], columns: [{ data: 'name', title: 'Name' }],
			selection: { rowIdField: 'id' } };
		await NS.card.mountDatatables(card, host, cardCtx);
		const table = host.querySelector('table[data-juneau-view]');
		let redraws = 0;
		table.__juneauCtx = { selectionState: { selected: new Set(['a', 'b']) }, redraw: function () { redraws++; } };
		let sel = null;
		let err = null;
		try { sel = api.selection(); await api.reload(); } catch (e) { err = String(e?.message); }
		out.t20_noThrow = err === null;
		out.t20_selection = JSON.stringify(sel) === '["a","b"]';
		out.t20_reloadRedraws = redraws === 1;
	}

	// =================================================================================================================
	// QuickStat kinds: tile, meter and segmented paint distinct markup; malformed or ambiguous items fail closed.
	// =================================================================================================================
	async function mountStats(items, opts) {
		const loaded = H.load(rendersJsPath, viewsJsPath, regionsJsPath, opts);
		const host = slot(loaded.env, 'incidents');
		const envl = envelope(loaded.NS);
		envl.quickStats = { contractVersion: '1', id: 'qs', items: items };
		await Promise.resolve(loaded.NS.init.mountTableSlot(host, envl));
		return { host: host, rec: loaded.rec, NS: loaded.NS, env: loaded.env };
	}
	const stat = function (host, id) { return host.querySelector('[data-juneau-stat="' + id + '"]'); };

	{
		const r = await mountStats([
			{ id: 'tile', label: 'Open', value: 3 },
			{ id: 'nullTile', label: null, value: null },
			{ id: 'meter', type: 'meter', label: 'Used', value: 2, max: 10 },
			{ id: 'seg', type: 'segmented', label: 'Mix', segments: [{ count: 1, label: 'a' }] },
			{ id: 'impliedMeter', label: 'Impl', value: 1, max: 4 },
			{ id: 'impliedSeg', label: 'ImplS', segments: [{ count: 1, label: 'a' }] }
		]);
		out.t21_tile = stat(r.host, 'tile')?.className === 'jc-stat jc-stat-tile' && stat(r.host, 'tile').getAttribute('role') == null;
		out.t21_nullTile = stat(r.host, 'nullTile')?.querySelector('.jc-stat-value')?.textContent === '';
		out.t21_meterRole = stat(r.host, 'meter')?.getAttribute('role') === 'meter';
		out.t21_segRole = stat(r.host, 'seg')?.getAttribute('role') === 'group';
		out.t21_impliedMeter = stat(r.host, 'impliedMeter')?.getAttribute('role') === 'meter';
		out.t21_impliedSeg = stat(r.host, 'impliedSeg')?.getAttribute('role') === 'group';
		out.t21_noErrors = r.rec.errors.length === 0;
	}

	{
		const bad = {
			t22_tileWithMax: { id: 'x', type: 'tile', label: 'L', value: 1, max: 5 },
			t22_meterNoMax: { id: 'x', type: 'meter', label: 'L', value: 1 },
			t22_segNoSegments: { id: 'x', type: 'segmented', label: 'L' },
			t22_meterWithSegments: { id: 'x', type: 'meter', label: 'L', value: 1, max: 5, segments: [{ count: 1, label: 'a' }] },
			t22_segmentedWithMax: { id: 'x', type: 'segmented', label: 'L', max: 5, segments: [{ count: 1, label: 'a' }] },
			t22_tileWithSegments: { id: 'x', type: 'tile', label: 'L', value: 1, segments: [{ count: 1, label: 'a' }] },
			t22_both: { id: 'x', label: 'L', max: 5, value: 1, segments: [{ count: 1, label: 'a' }] },
			t22_unknownType: { id: 'x', type: 'gauge', label: 'L', value: 1 },
			t22_zeroMax: { id: 'x', label: 'L', value: 1, max: 0 },
			t22_negMax: { id: 'x', label: 'L', value: 1, max: -3 },
			t22_stringValue: { id: 'x', label: 'L', value: '1', max: 5 },
			t22_negValue: { id: 'x', label: 'L', value: -1, max: 5 },
			t22_emptySegments: { id: 'x', label: 'L', segments: [] },
			t22_meterNoLabel: { id: 'x', type: 'meter', value: 1, max: 5 },
			t22_meterBlankLabel: { id: 'x', type: 'meter', label: '  ', value: 1, max: 5 },
			t22_meterNullLabel: { id: 'x', label: null, value: 1, max: 5 },
			t22_segNoLabel: { id: 'x', type: 'segmented', segments: [{ count: 1, label: 'a' }] },
			t22_segBlankLabel: { id: 'x', type: 'segmented', label: '', segments: [{ count: 1, label: 'a' }] },
			t22_segNullLabel: { id: 'x', label: null, segments: [{ count: 1, label: 'a' }] }
		};
		for (const key of Object.keys(bad)) {
			const r = await mountStats([bad[key]]);
			out[key] = r.host.querySelector('.juneau-view-error') != null && r.rec.errors.length >= 1
				&& r.host.querySelector('[data-juneau-stat]') == null;
		}
	}

	{
		// Meter detail: fill geometry, ARIA and over-limit state.
		const r = await mountStats([
			{ id: 'm', type: 'meter', label: 'Used', value: 2, max: 10, tone: 'warning' },
			{ id: 'zero', type: 'meter', label: 'Zero', value: 0, max: 10 },
			{ id: 'tiny', type: 'meter', label: 'Tiny', value: 1, max: 1000 },
			{ id: 'over', type: 'meter', label: 'Over', value: 15, max: 10 },
			{ id: 'err', type: 'meter', label: 'Err', value: 5, max: 10, tone: 'error' },
			{ id: 'txt', type: 'meter', label: 'Txt', value: 5, max: 10, text: '5 of 10 seats' },
			{ id: 'blank', type: 'meter', label: 'Blank', value: 5, max: 10, text: '   ' },
			{ id: 'off', type: 'meter', label: 'Off', value: 5, max: 10, tone: 'accent' },
			{ id: 'overErr', type: 'meter', label: 'OverErr', value: 15, max: 10, tone: 'error' }
		]);
		const fill = function (id) { return stat(r.host, id)?.querySelector('.jc-stat-fill'); };
		out.t23_width = fill('m')?.style.width === '20%';
		out.t23_ariaNow = stat(r.host, 'm')?.getAttribute('aria-valuenow') === '2'
			&& stat(r.host, 'm').getAttribute('aria-valuemin') === '0' && stat(r.host, 'm').getAttribute('aria-valuemax') === '10';
		out.t23_valuetextWarning = stat(r.host, 'm')?.getAttribute('aria-valuetext') === '2 / 10, warning';
		out.t23_fillTone = fill('m')?.className === 'jc-stat-fill is-warning';
		out.t23_zeroNoMin = fill('zero')?.style.width === '0%' && !fill('zero').style.minWidth;
		out.t23_tinyMin = fill('tiny')?.style.minWidth === '2px';
		out.t23_over = stat(r.host, 'over')?.className.includes('is-over') === true && stat(r.host, 'over').dataset.juneauOver === 'true'
			&& fill('over').style.width === '100%' && stat(r.host, 'over').getAttribute('aria-valuenow') === '10'
			&& stat(r.host, 'over').getAttribute('aria-valuetext') === '15 / 10, over limit';
		out.t23_notOver = stat(r.host, 'm')?.className.includes('is-over') === false && stat(r.host, 'm').dataset.juneauOver == null;
		out.t23_errTrack = stat(r.host, 'err')?.querySelector('.jc-stat-track')?.className === 'jc-stat-track is-error'
			&& stat(r.host, 'err').getAttribute('aria-valuetext') === '5 / 10, error';
		out.t23_text = stat(r.host, 'txt')?.getAttribute('aria-valuetext') === '5 of 10 seats'
			&& stat(r.host, 'txt').querySelector('.jc-stat-value')?.textContent === '5 of 10 seats';
		out.t23_blankText = stat(r.host, 'blank')?.getAttribute('aria-valuetext') === '5 / 10';
		out.t23_offPaletteTone = fill('off')?.className === 'jc-stat-fill';
		out.t23_hiddenParts = stat(r.host, 'm')?.querySelector('.jc-stat-label')?.getAttribute('aria-hidden') === 'true'
			&& stat(r.host, 'm').querySelector('.jc-stat-track')?.getAttribute('aria-hidden') === 'true'
			&& stat(r.host, 'm').getAttribute('aria-label') === 'Used';
		out.t23_valueHidden = stat(r.host, 'm')?.querySelector('.jc-stat-value')?.getAttribute('aria-hidden') === 'true';
		out.t23_overErrorOrder = stat(r.host, 'overErr')?.getAttribute('aria-valuetext') === '15 / 10, over limit, error';
	}

	{
		// The painters must never use a raw HTML-sink assignment or a style attribute: stub both to throw at load time.
		const poison = function (env) {
			const orig = env.document.createElement;
			env.document.createElement = function (tag) {
				const e = orig.call(env.document, tag);
				return new Proxy(e, {
					set: function (t, k, v) {
						if (k === 'inner' + 'HTML' || k === 'outer' + 'HTML') throw new Error('raw HTML-sink assignment');
						t[k] = v;
						return true;
					},
					get: function (t, k) {
						if (k === 'setAttribute') return function (n, v) {
							if (n === 'style') throw new Error('style attribute');
							return t.setAttribute(n, v);
						};
						if (k === 'insertAdjacentHTML') throw new Error('raw HTML-sink assignment');
						return t[k];
					}
				});
			};
		};
		const r = await mountStats([
			{ id: 'm', type: 'meter', label: '<b>x</b>', value: 1, max: 2 },
			{ id: 's', type: 'segmented', label: 'S', segments: [{ count: 1, label: '<i>y</i>' }] }
		], { decorate: poison });
		out.t23b_noThrow = r.rec.errors.length === 0 && r.host.querySelector('.juneau-view-error') == null;
		out.t23b_painted = stat(r.host, 'm') != null && stat(r.host, 's') != null;
		out.t23b_textOnly = stat(r.host, 'm')?.querySelector('.jc-stat-label')?.textContent === '<b>x</b>';
		out.t23b_segmentTextOnly = stat(r.host, 's')?.querySelector('.jc-stat-segment-label')?.textContent === '<i>y</i>'
			&& stat(r.host, 's').querySelector('i') == null;
	}

	{
		const r = await mountStats([
			{ id: 'seg', type: 'segmented', label: 'Mix', segments: [
				{ count: 2, label: 'a' }, { count: 0, label: 'z' }, { count: 3, label: 'b', tone: 'error' },
				{ count: 1, label: 'c' }, { count: 4, label: 'd', tone: 'accent' } ] }
		]);
		const seg = stat(r.host, 'seg');
		const fills = Array.from(seg?.querySelectorAll('.jc-stat-segfill') || []);
		const legend = Array.from(seg?.querySelectorAll('.jc-stat-segment') || []);
		out.t24_fillsSkipZero = fills.length === 4;
		out.t24_legendHasZero = legend.length === 5 && legend[1].querySelector('.jc-stat-segment-label')?.textContent === 'z'
			&& legend[1].querySelector('.jc-stat-segment-count')?.textContent === '0';
		out.t24_flexGrow = fills.map(function (f) { return f.style.flexGrow; }).join(',') === '2,3,1,4';
		// Untoned segments count positionally among untoned ones only; an off-palette tone counts as untoned.
		out.t24_colours = legend.map(function (l) { return l.className; }).join('|')
			=== 'jc-stat-segment is-c1|jc-stat-segment is-c2|jc-stat-segment is-error|jc-stat-segment is-c3|jc-stat-segment is-c4';
		out.t24_fillColours = fills.map(function (f) { return f.className.replace('jc-stat-segfill ', ''); }).join(',') === 'is-c1,is-error,is-c3,is-c4';
		out.t24_swatchHidden = legend[0].querySelector('.jc-stat-swatch')?.getAttribute('aria-hidden') === 'true';
		out.t24_barHidden = seg?.querySelector('.jc-stat-segbar')?.getAttribute('aria-hidden') === 'true';
		out.t24_labelHidden = seg?.querySelector('.jc-stat-label')?.getAttribute('aria-hidden') === 'true';
		out.t24_group = seg?.getAttribute('role') === 'group' && seg.getAttribute('aria-label') === 'Mix';
		const many = [];
		for (let i = 0; i < 10; i++) many.push({ count: 1, label: 's' + i });
		const r2 = await mountStats([{ id: 'many', type: 'segmented', label: 'M', segments: many }]);
		const cls = Array.from(stat(r2.host, 'many').querySelectorAll('.jc-stat-segment')).map(function (l) { return l.className.replace('jc-stat-segment ', ''); });
		out.t24_paletteCapsAtEight = cls.join(',') === 'is-c1,is-c2,is-c3,is-c4,is-c5,is-c6,is-c7,is-c8,is-c8,is-c8';
	}

	{
		// src= envelope: quick-stats items reach the client without a server-side check, so they fail closed there.
		const loaded = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const mk = function (items, viewId) {
			return { contractVersion: loaded.NS.SLOT_CONTRACT_VERSION, layout: 'wide',
				view: { contractVersion: loaded.NS.CONTRACT_VERSION, id: viewId, columns: [{ data: 'name', title: 'Name' }] },
				quickStats: { contractVersion: '1', id: 'qs', items: items } };
		};
		const ctxFor = function (json) {
			return { document: loaded.env.document, setApi: function () {}, onDestroy: function () {},
				isAbort: function () { return false; }, fetchJson: function () { return Promise.resolve(json); },
				paintError: function () {} };
		};
		const host = slot(loaded.env, 'pets');
		await loaded.NS.card.mountDatatables({ id: 'pets', type: 'datatables', src: '/x' }, host,
			ctxFor(mk([{ id: 'seg', type: 'segmented', label: 'S', segments: [{ count: 1, label: 'a' }] }], 'pets'))).catch(function () { /* asserted below */ });
		out.t25_segmentedMounts = host.querySelector('[data-juneau-stat="seg"][role="group"]') != null;
		const host2 = slot(loaded.env, 'pets2');
		let rejected = false;
		await loaded.NS.card.mountDatatables({ id: 'pets2', type: 'datatables', src: '/y' }, host2,
			ctxFor(mk([{ id: 'm', type: 'meter', label: 'M', value: 1 }], 'pets2'))).catch(function () { rejected = true; });
		out.t25_malformedFailsClosed = rejected && host2.querySelector('[data-juneau-stat]') == null;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (error) {
	process.stderr.write(String(error?.stack ? error.stack : error));
	process.exit(1);
});
