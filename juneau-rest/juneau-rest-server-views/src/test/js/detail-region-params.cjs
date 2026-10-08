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
 * detail-region-params.cjs - always-on Node harness: a datatables catalog's detail.region.params reach the region.
 *
 *   Usage:  node detail-region-params.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node detail-region-params.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

function slot(env, id) {
	const el = env.el('div');
	el.id = id;
	env.body.appendChild(el);
	return el;
}

function envelope(NS, region) {
	return {
		contractVersion: NS.SLOT_CONTRACT_VERSION,
		layout: 'wide',
		view: {
			contractVersion: NS.CONTRACT_VERSION,
			id: 'runs',
			columns: [{ data: 'name', title: 'Name' }]
		},
		detail: {
			contractVersion: NS.ROW_DETAIL_CONTRACT_VERSION,
			endpoint: '/runs/{id}',
			title: 'Run {name}',
			region: region,
			sections: [{ id: 'details', title: 'Details', fields: [{ data: 'name', title: 'Name' }] }]
		}
	};
}

/** Mounts one catalog and returns the row-detail region element inside the detail template (or null). */
async function build(region) {
	const ctx = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
	const host = slot(ctx.env, 'runs-slot');
	await Promise.resolve(ctx.NS.init.mountTableSlot(host, envelope(ctx.NS, region)));
	const tpl = host.querySelector('template[data-juneau-row-detail]');
	const content = tpl && tpl.content ? tpl.content : null;
	ctx.region = content ? content.querySelector('[data-juneau-region="' + region.id + '"]') : null;
	return ctx;
}

function declaredOf(el) {
	try {
		return el ? JSON.parse(el.dataset.juneauRegionDeclared) : null;
	} catch (error) { // NOSONAR javascript:S2486 -- an unparsable declaration is recorded as null and asserted on
		return null;
	}
}

const out = {};

(async function () { // NOSONAR javascript:S3776 -- linear test scenario
	const params = { linesUrl: '/runs/{id}/lines', compact: true, rows: 8, markers: 'hide' };

	{
		const c = await build({ id: 'output', type: 'row-detail', populate: 'console-output', params: params });
		const d = declaredOf(c.region);
		out.a01_paramsCopied = !!(d && JSON.stringify(d.params) === JSON.stringify(params));
		out.a01_noDataUrl = !!(d && !Object.hasOwn(d, 'dataUrl'));
		out.a01_populate = c.region?.dataset.juneauRegionPopulate === 'console-output';
	}

	{
		const c = await build({ id: 'both', type: 'row-detail', populate: 'p', dataUrl: '/runs/{id}/d', params: { a: 1 } });
		const d = declaredOf(c.region);
		out.a02_dataUrlAndParams = !!(d && d.dataUrl === '/runs/{id}/d' && d.params && d.params.a === 1
			&& Object.keys(d).length === 2);
	}

	{
		const c = await build({ id: 'none', type: 'row-detail', populate: 'p', dataUrl: '/runs/{id}/d' });
		const d = declaredOf(c.region);
		out.a03_noParamsUnchanged = !!(d && Object.keys(d).length === 1 && d.dataUrl === '/runs/{id}/d');
	}

	{
		const arr = await build({ id: 'arr', type: 'row-detail', populate: 'p', params: ['x'] });
		const nul = await build({ id: 'nul', type: 'row-detail', populate: 'p', params: null });
		const str = await build({ id: 'str', type: 'row-detail', populate: 'p', params: 'linesUrl=/x' });
		out.a04_nonObjectsDropped = [arr, nul, str].every(function (c) {
			const d = declaredOf(c.region);
			return d !== null && !Object.hasOwn(d, 'params');
		});
	}

	{
		const c = await build({ id: 'empty', type: 'row-detail', populate: 'p', params: {} });
		const d = declaredOf(c.region);
		out.a05_emptyObjectWritten = !!(d && d.params && Object.keys(d.params).length === 0);
	}

	{
		// Round trip: the declared params reach the populator's ctx.params.
		const c = await build({ id: 'probe', type: 'row-detail', populate: 'probe', params: params });
		let seen = null;
		c.R.register('probe', function (ctx) { seen = ctx.params; });
		if (c.region) {
			c.env.body.appendChild(c.region);
			c.R.initRegion(c.region);
		}
		await Promise.resolve();
		out.a06_roundTrip = !!(seen && seen.linesUrl === '/runs/{id}/lines' && seen.compact === true && seen.rows === 8
			&& seen.markers === 'hide');
		out.a06_noErrors = c.rec.errors.length === 0;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (error) {
	process.stderr.write(String(error?.stack ? error.stack : error));
	process.exit(1);
});
