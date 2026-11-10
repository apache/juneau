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
 * regions-chart.cjs - Node harness for the "chart" region type: the descriptor read, the implicit built-in populator,
 * its own fetch, the row mapping into JuneauCharts.mount, the empty state and the absent-charting state.  Every
 * assertion lives in RegionsChart_Test.
 *
 *   Usage:  node regions-chart.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath || !helpersJsPath) {
	console.error('usage: node regions-chart.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js>');
	process.exit(2);
}

const out = {};
const DECLARED = {
	contractVersion: '1', dataUrl: '/pets/s-7/price-series',
	chart: { kind: 'line', x: 'day', series: [{ field: 'price', label: 'Price' }], height: 240 }
};
const RECORDS = [{ day: 'Mon', price: 1, noise: 'x' }, { day: 'Tue', price: 2, noise: 'y' }];

function statusText(el) { return el.querySelectorAll('[role="status"]').map(function (n) { return n.textContent; }).join('|'); }

async function scenario(opts) {
	const mounts = [];
	const loaded = H.load(rendersJsPath, viewsJsPath, regionsJsPath, {
		helpersJsPath: helpersJsPath,
		decorate: function (env) {
			if (!opts.noCharts)
				env.window.JuneauCharts = { mount: function (el, card, rows) { mounts.push({ el: el, card: card, rows: rows }); return {}; } };
		}
	});
	const { env, R, rec } = loaded;
	const fetches = [];
	env.setFetch(function (url, init) {
		fetches.push(String(url));
		return Promise.resolve({ ok: true, status: 200, text: function () {
			return Promise.resolve(JSON.stringify({ contractVersion: '1', fields: opts.records || RECORDS }));
		} });
	});
	const el = H.mkRegion(env, { id: 'price', type: 'chart', declared: { ...DECLARED, ...opts.declared } });
	R.initRegion(el);
	await H.flush(20);
	return { el: el, mounts: mounts, fetches: fetches, rec: rec, R: R };
}

(async function () {
	{
		const s = await scenario({});
		out.types = s.R.REGION_TYPES.join(',');
		out.fetches = s.fetches;
		out.mountCount = s.mounts.length;
		const m = s.mounts[0] || {};
		out.sameEl = m.el === s.el;
		out.rows = m.rows;
		out.card = m.card;
		out.state = s.el.dataset.juneauRegionState;
		out.errors = s.rec.errors;
	}
	{
		const s = await scenario({ records: [], declared: { chart: { ...DECLARED.chart, emptyText: 'No prices' } } });
		out.emptyMounts = s.mounts.length;
		out.emptyText = statusText(s.el);
	}
	{
		const s = await scenario({ records: [] });
		out.emptyDefault = statusText(s.el);
	}
	{
		const s = await scenario({ noCharts: true });
		out.absentText = statusText(s.el);
		out.absentErrors = s.rec.errors.length;
	}
	process.stdout.write(JSON.stringify(out) + '\n', function () { process.exit(0); });
})().catch(function (e) { out.crash = String(e?.stack || e); process.stdout.write(JSON.stringify(out) + '\n', function () { process.exit(0); }); });
