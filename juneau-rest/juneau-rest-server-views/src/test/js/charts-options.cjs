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
 * charts-options.cjs - option snapshots for juneau-charts.js: one JSON snapshot per helper and variant, the patch
 * merge-by-id cases, the large-data defaults, the legacy form, the decorators and the built-in fns.  Prints
 * {goldens: {<name>: <pretty JSON text>}, caseWarnings: {<name>: [warning]}, thrown: {<name>: message}, checks:
 * {<name>: boolean}, errors, warnings}; ConsoleChartsOptions_Test compares each golden with
 * src/test/resources/charts-goldens/<name>.json and asserts the per-case warnings, thrown errors and checks.  The fn outputs depend on the locale and time zone, so the test runs
 * node with LC_ALL=en_US.UTF-8 and TZ=UTC.
 *
 *   Usage:  node charts-options.cjs <juneau-charts.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'juneau-charts-harness.cjs'));

const chartsPath = process.argv[2];
if (!chartsPath) {
	console.error('usage: node charts-options.cjs <juneau-charts.js>');
	process.exit(2);
}

const DAYS = [{ name: 'day', type: 'string' }, { name: 'placed', type: 'number' }, { name: 'delivered', type: 'number' }];
const ROWS = [{ day: 'Mon', placed: 3, delivered: 1 }, { day: 'Tue', placed: 5, delivered: 2 }, { day: 'Wed', placed: 4, delivered: 4 }];
const SERIES = [{ field: 'placed', label: 'Placed' }, { field: 'delivered', label: 'Delivered', tone: 'ok' }];
const SLICES = [{ name: 's', type: 'string' }, { name: 'n', type: 'number' }];
const SLICE_ROWS = [{ s: 'a', n: 50 }, { s: 'b', n: 47 }, { s: 'c', n: 3 }];
const CELLS = [{ name: 'hour', type: 'string' }, { name: 'dow', type: 'string' }, { name: 'n', type: 'number' }];
const CELL_ROWS = [{ hour: '9', dow: 'Mon', n: 2 }, { hour: '10', dow: 'Mon', n: 7 }, { hour: '9', dow: 'Tue', n: 4 }];
const POINTS = [{ name: 't', type: 'time' }, { name: 'ms', type: 'number' }, { name: 'size', type: 'number' }];
const POINT_ROWS = [{ t: '2026-10-01T00:00:00Z', ms: 120, size: 1 }, { t: '2026-10-02T00:00:00Z', ms: 80, size: 9 }];
const BOXES = [{ name: 'day', type: 'string' }].concat(['lo', 'q1', 'med', 'q3', 'hi'].map(name => ({ name, type: 'number' })));
const BOX_ROWS = [{ day: 'Mon', lo: 10, q1: 20, med: 30, q3: 40, hi: 55 }, { day: 'Tue', lo: 12, q1: 18, med: 25, q3: 33, hi: 41 }];

// [name, card, dims, rows]
const CASES = [
	['line', { id: 'c', helper: { kind: 'line', x: { field: 'day', label: 'Day' }, y: { label: 'Orders', min: 0 }, series: SERIES } }, DAYS, ROWS],
	['bar-stacked', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES, stacked: true, legend: 'right' } }, DAYS, ROWS],
	['bar-horizontal', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES, orientation: 'horizontal', legend: 'none' } }, DAYS, ROWS],
	['donut-center-total', { id: 'c', helper: { kind: 'donut', label: 's', value: 'n', center: 'total' } }, SLICES, SLICE_ROWS],
	['donut-center-literal', { id: 'c', helper: { kind: 'donut', label: 's', value: 'n', center: 'Mix' } }, SLICES, SLICE_ROWS],
	['pie', { id: 'c', helper: { kind: 'pie', label: 's', value: 'n' } }, SLICES, SLICE_ROWS],
	['heatmap-extent', { id: 'c', helper: { kind: 'heatmap', x: { field: 'hour' }, y: { field: 'dow' }, value: 'n' } }, CELLS, CELL_ROWS],
	['heatmap-scale', { id: 'c', helper: { kind: 'heatmap', x: { field: 'hour' }, y: { field: 'dow' }, value: 'n', scale: { min: 0, max: 10 } } }, CELLS, CELL_ROWS],
	['scatter', { id: 'c', helper: { kind: 'scatter', x: { field: 't', type: 'time' }, series: [{ field: 'ms', label: 'Latency' }] } }, POINTS, POINT_ROWS],
	// The legacy flat form (ChartDef.line/bar/donut output): the same option as the helper form.
	['legacy-line', { id: 'c', kind: 'line', x: { field: 'day', label: 'Day' }, y: { label: 'Orders', min: 0 }, series: SERIES }, DAYS, ROWS],
	['raw-own-dataset', { id: 'c', option: { dataset: { id: 'mine', source: [[1, 2]] }, series: [{ type: 'line', datasetIndex: 0 }, { type: 'bar', datasetId: 'mine' }] } }, DAYS, ROWS],
	['raw-no-binding', { id: 'c', option: { dataset: { id: 'mine', source: [[1, 2]] }, series: [{ type: 'line', datasetId: 'mine' }] } }, DAYS, ROWS],
	['patch-by-id', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		patch: { series: [{ id: 'placed', smooth: true }, { id: 'extra', type: 'line', data: [{ $fn: 'compact' }] }], legend: { top: 0 } } }, DAYS, ROWS],
	['patch-no-ids-replaces', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		patch: { series: [{ type: 'bar', encode: { x: 'day', y: 'placed' } }] } }, DAYS, ROWS],
	['option-over-helper', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES },
		option: { grid: { left: 8 }, yAxis: { name: 'Raw' }, aria: { enabled: false } } }, DAYS, ROWS],
	['render-mode-html-kept', { id: 'c', option: { tooltip: { renderMode: 'html', formatter: { $fn: 'app.tip' } }, series: [] } }, DAYS, ROWS],
	// Boxes: a box plot is a raw option passed through, with no helper.
	['raw-boxplot', { id: 'c', option: { xAxis: { type: 'category' }, yAxis: {},
		series: [{ type: 'boxplot', encode: { x: 'day', y: ['lo', 'q1', 'med', 'q3', 'hi'] } }] } }, BOXES, BOX_ROWS]
];

// Render-mode guard: ECharts renders html for every renderMode except the exact string "richText", so only "richText" and
// an html tooltip with a $fn formatter may reach it.
const tip = (extra) => Object.assign({ formatter: '<b>{b}</b>' }, extra);
CASES.push(
	['render-mode-html-string-formatter', { id: 'c', option: { tooltip: tip({ renderMode: 'html' }), series: [] } }, DAYS, ROWS],
	['render-mode-auto', { id: 'c', option: { tooltip: tip({ renderMode: 'auto' }), series: [] } }, DAYS, ROWS],
	['render-mode-empty', { id: 'c', option: { tooltip: tip({ renderMode: '' }), series: [] } }, DAYS, ROWS],
	['render-mode-false', { id: 'c', option: { tooltip: tip({ renderMode: false }), series: [] } }, DAYS, ROWS],
	['render-mode-upper', { id: 'c', option: { tooltip: tip({ renderMode: 'HTML' }), series: [] } }, DAYS, ROWS],
	['render-mode-array', { id: 'c', option: { tooltip: [{ renderMode: 'html', formatter: { $fn: 'app.tip' } }, tip({ renderMode: 'html' })], series: [] } }, DAYS, ROWS],
	['render-mode-base-option', { id: 'c', option: { baseOption: { tooltip: tip({ renderMode: 'html' }), series: [] } } }, DAYS, ROWS],
	['render-mode-media', { id: 'c', option: { baseOption: { series: [] },
		media: [{ query: { maxWidth: 500 }, option: { tooltip: tip({ renderMode: 'html' }) } }] } }, DAYS, ROWS],
	['render-mode-options', { id: 'c', option: { baseOption: { series: [] },
		options: [{ tooltip: tip({ renderMode: 'auto' }) }, { tooltip: [tip({ renderMode: 'html' })] }] } }, DAYS, ROWS],
	['raw-own-dataset-transform', { id: 'c', option: { dataset: [{ id: 'mine', source: [[1, 2]] },
		{ fromDatasetIndex: 0, transform: { type: 'filter', config: { dimension: 0, gt: 0 } } }], series: [{ type: 'line', datasetIndex: 1 }] } }, DAYS, ROWS],
	['y-format-integer', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, y: { format: 'integer' }, series: SERIES } }, DAYS, ROWS],
	['y-format-unknown', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, y: { format: 'toString' }, series: SERIES } }, DAYS, ROWS],
	['axis-type-unknown', { id: 'c', helper: { kind: 'line', x: { field: 'day', type: 'constructor' }, series: SERIES } }, DAYS, ROWS],
	['tone-numeric', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: [{ field: 'placed', tone: 3 }, { field: 'delivered', tone: 9 }] } }, DAYS, ROWS],
	['marker-extra-key', { id: 'c', option: { tooltip: { formatter: { $fn: 'app.tip', extra: 1 } }, series: [] } }, DAYS, ROWS],
	['scatter-symbol-size', { id: 'c', helper: { kind: 'scatter', x: { field: 't', type: 'time' }, series: [{ field: 'ms' }],
		symbolSize: { $fn: 'app.size', args: { field: 'size' } } } }, POINTS, POINT_ROWS],
	['legacy-scatter-symbol-size', { id: 'c', kind: 'scatter', x: { field: 't', type: 'time' }, series: [{ field: 'ms' }],
		symbolSize: { $fn: 'app.size', args: { field: 'size' } } }, POINTS, POINT_ROWS],
	// A bool column keeps its booleans and a dimension type of "number" (pinned as-is).
	// (A null string cell decodes to null, not to the first dictionary entry.)
	['bool-column', { id: 'c', option: { series: [{ type: 'bar', encode: { x: 'day', y: 'ok' } }] } },
		[{ name: 'day', type: 'string' }, { name: 'ok', type: 'bool' }], [{ day: 'Mon', ok: true }, { day: 'Tue', ok: false }, { day: null }]],
	// A null number is NaN in the frame and is skipped by the donut's centre total (50 + 47 + 3).
	['donut-center-total-skips-null', { id: 'c', helper: { kind: 'donut', label: 's', value: 'n', center: 'total' } }, SLICES, SLICE_ROWS.concat([{ s: 'd', n: null }])],
	// Error paths: the pipeline rejects, ConsoleChartsOptions_Test asserts the message.
	['error-kind-unknown', { id: 'c', helper: { kind: 'nope' } }, DAYS, ROWS],
	['error-kind-constructor', { id: 'c', helper: { kind: 'constructor' } }, DAYS, ROWS],
	['error-kind-to-string', { id: 'c', helper: { kind: 'toString' } }, DAYS, ROWS],
	['error-kind-proto', { id: 'c', helper: { kind: '__proto__' } }, DAYS, ROWS],
	['error-fn-unknown', { id: 'c', option: { tooltip: { formatter: { $fn: 'app.nope' } }, series: [] } }, DAYS, ROWS],
	['error-decorator-unknown', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES }, decorators: [{ use: 'app.nope' }] }, DAYS, ROWS]
);

// The large-data cases build a 6000-row frame (above the 5000 default threshold).
const BIG = [], BIG_POINTS = [];
for (let i = 0; i < 6000; i++) {
	BIG.push({ day: 'd' + i, placed: i, delivered: i % 7 });
	BIG_POINTS.push({ t: new Date(Date.UTC(2026, 9, 1) + i * 60000).toISOString(), ms: i % 90, size: i % 5 });
}
CASES.push(
	['large-on', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES } }, DAYS, BIG],
	['large-explicit-wins', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES },
		patch: { animation: true, series: [{ id: 'placed', large: false, progressive: 100 }] } }, DAYS, BIG],
	['large-off-below-threshold', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES }, largeData: { threshold: 10000 } }, DAYS, BIG],
	['large-progressive-override', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES }, largeData: { threshold: 100, progressive: 2000 } }, DAYS, BIG],
	// The row count equals the threshold: not above it, so no large-data defaults.
	['large-off-at-threshold', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES }, largeData: { threshold: 6000 } }, DAYS, BIG],
	['large-progressive-false', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES }, largeData: { threshold: 100, progressive: false } }, DAYS, BIG],
	['large-scatter', { id: 'c', helper: { kind: 'scatter', x: { field: 't', type: 'time' }, series: [{ field: 'ms', label: 'Latency' }] } }, POINTS, BIG_POINTS]
);

// The built-in fn cases: built-in fns, the four decorators and the annotation presets.
const STAMPS = [{ name: 't', type: 'time' }, { name: 'n', type: 'number' }];
const STAMP_ROWS = [{ t: 0, n: 1 }, { t: 3600000, n: 0 }, { t: 7200000, n: 0 }, { t: 10800000, n: 4 }];
CASES.push(
	['y-format', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, y: { format: 'compact' }, series: SERIES } }, DAYS, ROWS],
	['scatter-size', { id: 'c', helper: { kind: 'scatter', x: { field: 't', type: 'time' }, series: [{ field: 'ms', label: 'Latency' }], symbolSize: { $fn: 'symbolSizeBy', args: { field: 'size' } } } }, POINTS, POINT_ROWS],
	['fn-in-option', { id: 'c', option: { yAxis: { axisLabel: { formatter: { $fn: 'percent', args: { digits: 0 } } } }, series: [] } }, DAYS, ROWS],
	['markers-values', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'markers', values: [{ at: 'Tue', label: 'Deploy' }], topic: 'deploys' }] }, DAYS, ROWS],
	['bands-gaps', { id: 'c', helper: { kind: 'line', x: { field: 't', type: 'time' }, series: [{ field: 'n', label: 'N' }] },
		decorators: [{ use: 'bands', auto: 'gaps', label: 'No data' }] }, STAMPS, STAMP_ROWS],
	['bands-values', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'bands', values: [{ start: 'Mon', end: 'Tue', label: 'Outage' }] }] }, DAYS, ROWS],
	['thresholds', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'thresholds', values: [{ y: 4, label: 'SLO', tone: 'danger' }, { y: 1, label: 'Floor' }] }] }, DAYS, ROWS],
	['annotations-presets', { id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'annotations', presets: ['max', 'min'], label: '{value}' }] }, DAYS, ROWS],
	['annotations-values', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'annotations', values: [{ x: 'Wed', y: 4, label: 'Peak' }] }] }, DAYS, ROWS],
	['decorators-order', { id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES },
		decorators: [{ use: 'thresholds', values: [{ y: 2 }] }, { use: 'markers', values: [{ at: 'Mon', label: 'A' }] }],
		patch: { series: [{ id: 'juneau-markers-1', z: 9 }] } }, DAYS, ROWS],
	// ECharts renders optionToContent's result as html, so a $fn there is refused (E-JS-98).
	['error-fn-option-to-content', { id: 'c', option: { toolbox: { feature: { dataView: { optionToContent: { $fn: 'app.tip' } } } }, series: [] } }, DAYS, ROWS]
);

// The fn outputs: [name, $fn marker, renderMode, the call's arguments].
const FN_CASES = [
	['number', { $fn: 'number' }, 'richText', [1234.5]],
	['number-digits', { $fn: 'number', args: { digits: 0 } }, 'richText', [1234.5]],
	['percent', { $fn: 'percent' }, 'richText', [0.125]],
	['percent-scale', { $fn: 'percent', args: { scale: 100 } }, 'richText', [12.5]],
	['compact', { $fn: 'compact' }, 'richText', [15300]],
	['bytes', { $fn: 'bytes' }, 'richText', [1500000]],
	['bytes-binary', { $fn: 'bytes', args: { binary: true } }, 'richText', [1048576]],
	['duration', { $fn: 'duration' }, 'richText', [3723000]],
	['datetime-utc', { $fn: 'datetime', args: { utc: true, pattern: 'date' } }, 'richText', [Date.UTC(2026, 9, 11)]],
	['template-rich', { $fn: 'template', args: { text: '{seriesName}: {value}' } }, 'richText', [{ seriesName: 'a{b}', value: 3 }]],
	['template-html', { $fn: 'template', args: { text: '{seriesName}' } }, 'html', [{ seriesName: '<b>&' }]],
	['template-formats', { $fn: 'template', args: { text: '{value}', formats: { value: { $fn: 'percent' } } } }, 'richText', [{ value: 0.5 }]],
	['symbol-size-max', { $fn: 'symbolSizeBy', args: { field: 'size', min: 4, max: 24 } }, 'richText', [[0, 9], { dataIndex: 1 }]],
	['symbol-size-min', { $fn: 'symbolSizeBy', args: { field: 'size' } }, 'richText', [[0, 1], { dataIndex: 0 }]],
	['bytes-below-1000', { $fn: 'bytes' }, 'richText', [999]],
	['bytes-tier-boundary', { $fn: 'bytes' }, 'richText', [999999]],
	['bytes-negative', { $fn: 'bytes' }, 'richText', [-1500]],
	['duration-below-1s', { $fn: 'duration' }, 'richText', [999]],
	['duration-unit-s', { $fn: 'duration', args: { unit: 's' } }, 'richText', [90]],
	['duration-tier-boundary', { $fn: 'duration' }, 'richText', [59950]],
	['duration-negative', { $fn: 'duration' }, 'richText', [-5000]],
	['number-nan', { $fn: 'number' }, 'richText', [NaN]],
	['datetime-null', { $fn: 'datetime' }, 'richText', [null]],
	['throwing-fn-blank', { $fn: 'app.boom' }, 'richText', [1]],
	['unknown-e-js-90', { $fn: 'app.missing' }, 'richText', [1]]
];

const goldens = {}, caseWarnings = {}, thrown = {}, checks = {};
const pretty = v => JSON.stringify(v, null, 2) + '\n';
let tokensFrozen = null;
// The stand-in network: every fetch lands in fetched[]; /api/fail answers HTTP 500, every other path a JSON array of rows.
const fetched = [];
const fakeFetch = (url, init) => {
	fetched.push({ url: String(url), signal: init && init.signal, credentials: init && init.credentials });
	if (String(url) === '/api/fail') return Promise.resolve({ ok: false, status: 500, json: () => Promise.resolve({ message: 'nope' }) });
	if (String(url) === '/api/rows-fn') return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([{ at: { $fn: 'nope.x' }, label: { $fn: 'compact' } }]) });
	if (String(url) === '/api/rows-object') return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve({ rows: [{ at: 'Wed', label: 'FromRows' }] }) });
	return Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([{ at: 'Tue', label: 'Deploy', start: 'Mon', end: 'Tue' }]) });
};
let stampFrame = null, rawResult = null;
const REGISTRATIONS = [
	['fn', 'app.tip', () => p => String(p)],
	['fn', 'app.boom', () => () => { throw new Error('boom'); }],
	['fn', 'app.mode', (a, c) => () => c.renderMode],
	['fn', 'app.size', () => () => 1],
	['decorator', 'app.tokens', (o, f, spec, ctx) => { tokensFrozen = Object.isFrozen(ctx.tokens); return o; }],
	['decorator', 'app.protoRoot', () => JSON.parse('{"__proto__":{"polluted":1},"series":[]}')],
	['decorator', 'app.protoDeep', () => JSON.parse('{"grid":{"__proto__":{"polluted":1}},"series":[]}')],
	// Decorators that call ctx.fetch directly: rawResult keeps what the last app.rawFetch saw.
	['decorator', 'app.rawFetch', (o, f, spec, ctx) => ctx.fetch(spec.url).then(() => { rawResult = 'ok'; return o; }, e => { rawResult = e.message; return o; })],
	['decorator', 'app.twoFetch', (o, f, spec, ctx) => Promise.all([ctx.fetch('/api/a'), ctx.fetch('/api/b')]).then(() => o)],
	['decorator', 'app.retry', (o, f, spec, ctx) => ctx.fetch('/api/fail').catch(() => 0).then(() => ctx.fetch('/api/fail')).catch(() => 0).then(() => o)],
	['decorator', 'app.protoList', () => JSON.parse('{"series":[{"type":"line","__proto__":{"polluted":1}}]}')]
];

(async () => {
	const env = H.load(chartsPath, { fetch: fakeFetch, registrations: REGISTRATIONS,
		window: { JuneauViews: { data: { builtins: [], isHandle: h => !!h && !!h.frame, get: h => h.frame }, bus: { get: t => t === 'data:rows' ? { frame: stampFrame } : undefined } } } });
	stampFrame = H.frame(env, STAMPS, STAMP_ROWS);
	for (const [name, card, dims, rows] of CASES) {
		const w = env.warnings.length;
		try {
			goldens[name] = pretty(H.snapshot(await H.runPipeline(env, card, H.frame(env, dims, rows))));
		} catch (e) {
			// The list of registered names changes as tasks add built-ins; the golden keeps only the stable part.
			thrown[name] = e.message.replace(/; registered(?: kinds)?: '.*$/, '; registered: <list>');
			goldens[name] = pretty({ error: thrown[name] });
		}
		caseWarnings[name] = env.warnings.slice(w);
	}
	if (typeof FN_CASES !== 'undefined') {
		const f = H.frame(env, POINTS, POINT_ROWS);
		const fns = {}, w0 = env.warnings.length;
		for (const [name, marker, mode, args] of FN_CASES) {
			try {
				const option = await H.runPipeline(env, { id: 'c', option: { tooltip: { renderMode: mode, formatter: marker }, series: [] } }, f);
				const fn = option.tooltip.formatter;
				fns[name] = typeof fn === 'function' ? fn(...args) : { notFn: H.snapshot(fn) };
			} catch (e) {
				fns[name] = { error: e.message.replace(/; registered(?: kinds)?: '.*$/, '; registered: <list>') };
			}
		}
		goldens.fns = pretty(fns);
		caseWarnings.fns = env.warnings.slice(w0);
	}
	await runChecks(env);
	process.stdout.write(JSON.stringify({ goldens, caseWarnings, thrown, checks, errors: env.errors, warnings: env.warnings }));
})().catch(e => { console.error(e && e.stack || e); process.exit(1); });

// Behaviours a golden cannot show: the mode a $fn is built in, build()'s promise, frozen tokens, __proto__ keys.
async function runChecks(env) {
	const fr = H.frame(env, DAYS, ROWS);
	const run = (card) => H.runPipeline(env, card, fr);
	const modeOf = async (option, pick) => pick(await run({ id: 'c', option: option }))();
	const mk = (renderMode) => ({ renderMode: renderMode, formatter: { $fn: 'app.mode' } });
	// "html" is the only value that reaches ECharts as html; every other value must build the fn in richText mode.
	checks.fnModeHtmlKept = await modeOf({ tooltip: mk('html'), series: [] }, o => o.tooltip.formatter) === 'html';
	for (const bad of ['', false, 'HTML', 'auto', 0, ' html', 'foo', true])
		checks['fnModeDowngraded:' + JSON.stringify(bad)] = await modeOf({ tooltip: mk(bad), series: [] }, o => o.tooltip.formatter) === 'richText';
	checks.fnModeMissing = await modeOf({ tooltip: { formatter: { $fn: 'app.mode' } }, series: [] }, o => o.tooltip.formatter) === 'richText';
	checks.fnModeBaseOption = await modeOf({ baseOption: { tooltip: mk('HTML'), series: [] } }, o => o.baseOption.tooltip.formatter) === 'richText';
	checks.fnModeBaseOptionHtml = await modeOf({ baseOption: { tooltip: mk('html'), series: [] } }, o => o.baseOption.tooltip.formatter) === 'html';
	checks.fnModeMedia = await modeOf({ baseOption: { series: [] }, media: [{ option: { tooltip: mk('') } }] }, o => o.media[0].option.tooltip.formatter) === 'richText';
	checks.fnModeOptions = await modeOf({ baseOption: { series: [] }, options: [{ tooltip: [mk(false)] }] }, o => o.options[0].tooltip[0].formatter) === 'richText';
	checks.fnModeMediaHtml = await modeOf({ baseOption: { series: [] }, media: [{ option: { tooltip: mk('html') } }] }, o => o.media[0].option.tooltip.formatter) === 'html';
	checks.fnModeOptionsHtml = await modeOf({ baseOption: { series: [] }, options: [{ tooltip: [mk('html')] }] }, o => o.options[0].tooltip[0].formatter) === 'html';
	// ECharts uses ONE render mode for the whole chart (the first tooltip of the merged model), so every tooltip $fn in every
	// scope and every array entry is built in that one mode: "html" when any scope's first tooltip is html, else "richText".
	checks.fnModeArray = await modeOf({ tooltip: [mk('html'), mk('auto')], series: [] }, o => o.tooltip[1].formatter) === 'html';
	const sf = { type: 'line', tooltip: { formatter: { $fn: 'app.mode' } } };
	checks.fnModeHtmlTopOptionsSeries = await modeOf({ tooltip: mk('html'), series: [], options: [{ series: [sf] }] }, o => o.options[0].series[0].tooltip.formatter) === 'html';
	checks.fnModeHtmlTopMediaSeries = await modeOf({ tooltip: mk('html'), series: [], media: [{ option: { series: [sf] } }] }, o => o.media[0].option.series[0].tooltip.formatter) === 'html';
	checks.fnModeHtmlBaseMediaSeries = await modeOf({ baseOption: { tooltip: mk('html'), series: [] }, media: [{ option: { series: [sf] } }] }, o => o.media[0].option.series[0].tooltip.formatter) === 'html';
	checks.fnModeArrayRichEntry = await modeOf({ tooltip: [mk('html'), mk('richText')], series: [] }, o => o.tooltip[1].formatter) === 'html';
	checks.fnModeRichTopHtmlOptionsStep = await modeOf({ tooltip: { renderMode: 'richText' }, series: [sf], options: [{ tooltip: mk('html') }] }, o => o.series[0].tooltip.formatter) === 'html';
	checks.fnModeLegendToolboxHtml = await modeOf({ tooltip: mk('html'), legend: { tooltip: { formatter: { $fn: 'app.mode' } } }, series: [] }, o => o.legend.tooltip.formatter) === 'html';
	checks.fnModeAllRich = await modeOf({ tooltip: mk('richText'), series: [sf], options: [{ series: [sf] }] }, o => o.options[0].series[0].tooltip.formatter) === 'richText';
	// build() always returns a promise: a synchronous throw becomes a rejection.
	let promise = null, message = null;
	try {
		promise = env.charts.build({ id: 'c', get helper() { throw new Error('sync boom'); } }, fr);
	} catch (e) {
		message = 'threw';
	}
	checks.buildReturnsPromise = promise !== null && typeof promise.then === 'function';
	if (promise) await promise.then(() => { message = 'resolved'; }, e => { message = e.message; });
	checks.buildRejectsSyncThrow = message === 'sync boom';
	// a ctx whose getter throws (decoratorCtx copies it) is a rejection too, never a synchronous throw.
	let ctxPromise = null, ctxMessage = null;
	try {
		ctxPromise = env.charts.build({ id: 'c', option: { series: [] } }, fr, { get tokens() { throw new Error('ctx boom'); } });
	} catch (e) {
		ctxMessage = 'threw';
	}
	checks.buildReturnsPromiseForThrowingCtx = ctxPromise !== null && typeof ctxPromise.then === 'function';
	if (ctxPromise) await ctxPromise.then(() => { ctxMessage = 'resolved'; }, e => { ctxMessage = e.message; });
	checks.buildRejectsThrowingCtx = ctxMessage === 'ctx boom';
	// The tokens a decorator gets are frozen.
	await run({ id: 'c', option: { series: [] }, decorators: [{ use: 'app.tokens' }] });
	checks.tokensFrozen = tokensFrozen === true;
	await env.charts.build({ id: 'c', option: { series: [] }, decorators: [{ use: 'app.tokens' }] }, fr, { tokens: { '--jc-chart-1': '#000' } });
	checks.callerTokensFrozen = tokensFrozen === true;
	// A frame counts the rows its builder was handed that are not objects, and a build shares the frame's decoded source.
	const counted = H.frame(env, DAYS, ROWS.concat([null, 'x']));
	checks.droppedRowsCounted = counted.droppedRows === 2 && counted.rowCount === 3;
	const first = await run({ id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES } });
	const second = await run({ id: 'c', helper: { kind: 'line', x: { field: 'day' }, series: SERIES } });
	checks.sourceSharedAcrossBuilds = first.dataset[0].source === second.dataset[0].source && first.dataset[0].source.day === second.dataset[0].source.day;
	// Decorator fetch: a same-origin src is fetched once per build (cached), and the rows reach the decorator.
	const line = { kind: 'line', x: { field: 'day' }, series: SERIES };
	fetched.length = 0;
	const dep = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', src: '/api/deploys' }, { use: 'markers', src: '/api/deploys' }] });
	checks.decoratorFetchRows = dep.series.find(x => x.id === 'juneau-markers-0').markLine.data[0].name === 'Deploy';
	checks.decoratorFetchCachedPerBuild = fetched.filter(x => x.url === '/api/deploys').length === 1;
	// Every URL a decorator can fetch goes through the same-origin rule: nothing else reaches fetch().
	fetched.length = 0;
	let unsafeOk = true;
	for (const bad of ['//evil.example/x', 'https://evil.example/x', 'http:/x', '/a/../b', '/x\\y', ' /x', '/x ', '/x\ny', '/x\ty', 'javascript:alert(1)', 'x:y', '..', '', '\\\\evil.example', '/x\u007f']) {
		for (const via of ['spec', 'ctx']) {
			rawResult = null;
			let msg = null;
			try {
				if (via === 'spec') await run({ id: 'c', helper: line, decorators: [{ use: 'markers', src: bad }] });
				else { await run({ id: 'c', helper: line, decorators: [{ use: 'app.rawFetch', url: bad }] }); msg = rawResult; }
			} catch (e) { msg = e.message; }
			if (!(typeof msg === 'string' && msg.includes('is not a same-origin path'))) { unsafeOk = false; console.error('unsafe url not refused (' + via + '): ' + JSON.stringify(bad) + ' -> ' + msg); }
		}
	}
	checks.decoratorFetchRefusesUnsafeUrls = unsafeOk && fetched.length === 0;
	rawResult = null;
	await run({ id: 'c', helper: line, decorators: [{ use: 'app.rawFetch' }] });
	checks.decoratorFetchRefusesNonString = rawResult === 'the url is not a string' || (typeof rawResult === 'string' && rawResult.includes('is not a same-origin path'));
	// Non-aborting: a second fetch never aborts the first, and a failed fetch is not cached.
	fetched.length = 0;
	await run({ id: 'c', helper: line, decorators: [{ use: 'app.twoFetch' }] });
	checks.decoratorFetchesDoNotAbortEachOther = fetched.length === 2 && fetched.every(x => x.signal && x.signal.aborted === false);
	fetched.length = 0;
	await run({ id: 'c', helper: line, decorators: [{ use: 'app.retry' }] });
	checks.decoratorFailedFetchNotCached = fetched.length === 2;
	// ... but the card's own signal (superseded draw or destroyed card) aborts them.
	const ac = new AbortController();
	fetched.length = 0;
	await env.charts.build({ id: 'c', helper: line, decorators: [{ use: 'app.twoFetch' }] }, fr, { signal: ac.signal });
	ac.abort();
	checks.decoratorFetchAbortedWithCard = fetched.length === 2 && fetched.every(x => x.signal.aborted === true);
	// A decorator can read the current frame of a data: topic.
	const bd = await run({ id: 'c', helper: { kind: 'line', x: { field: 't', type: 'time' }, series: [{ field: 'n' }] },
		decorators: [{ use: 'bands', source: 'data:rows', start: 't', end: 't' }] });
	checks.decoratorSourceRows = bd.series.find(x => x.id === 'juneau-bands-0').markArea.data.length === 4;
	// A __proto__ key from JSON.parse is dropped, not turned into a prototype.
	const root = await run({ id: 'c', decorators: [{ use: 'app.protoRoot' }] });
	checks.protoKeyRootDropped = !('polluted' in root);
	const deep = await run({ id: 'c', decorators: [{ use: 'app.protoDeep' }] });
	checks.protoKeyNestedDropped = !('polluted' in deep.grid);
	const list = await run({ id: 'c', decorators: [{ use: 'app.protoList' }] });
	checks.protoKeyInSeriesDropped = !('polluted' in list.series[0]);
	// A patch key __proto__ must not become the prototype of the working option (here it would hide the large-data default).
	const big = H.frame(env, DAYS, BIG);
	const patched = await H.runPipeline(env, JSON.parse('{"id":"c","helper":{"kind":"line","x":{"field":"day"},"series":[{"field":"placed"}]},"patch":{"__proto__":{"animation":true}}}'), big);
	checks.protoKeyInPatchNoEffect = patched.animation === false;
	await runEscapeChecks(env);
	await runCacheCheck();
	await runDecoratorChecks(env, line);
}

// every string a built-in fn produces is escaped for its render mode, fallbacks included.
async function runEscapeChecks(env) {
	const fr = H.frame(env, DAYS, ROWS);
	const X = '<img src=x onerror=alert(1)>', XE = '&lt;img src=x onerror=alert(1)&gt;';
	const fnIn = async (marker, mode, frame) => (await H.runPipeline(env, { id: 'c', option: { tooltip: { renderMode: mode, formatter: marker }, series: [] } }, frame || fr)).tooltip.formatter;
	for (const name of ['number', 'percent', 'compact', 'bytes', 'duration', 'datetime']) {
		const f = await fnIn({ $fn: name }, 'html');
		checks['fnEscapesHtml:' + name] = f(X) === XE && f({ value: X }) === XE && f([{ value: [X, X], encode: { y: [1] } }]) === XE && f('a&b"c\'') === 'a&amp;b&quot;c&#39;';
		const r = await fnIn({ $fn: name }, 'richText');
		checks['fnEscapesRich:' + name] = r('a{b}') === 'a\uff5bb\uff5d';
	}
	// A real value still formats in html mode, and the formatted text is escaped as a whole.
	const fh = await fnIn({ $fn: 'bytes' }, 'html');
	checks.fnHtmlFormatsValue = fh(1500000) === '1.5 MB';
	// template escapes once: its formats sub-fns resolve unescaped, so nothing is double escaped.
	const tf = await fnIn({ $fn: 'template', args: { text: '[{value}] {name}', formats: { value: { $fn: 'bytes' } } } }, 'html');
	checks.fnTemplateEscapesOnce = tf({ value: '<&>', name: '<&>' }) === '[&lt;&amp;&gt;] &lt;&amp;&gt;';
	const tr = await fnIn({ $fn: 'template', args: { text: '[{value}]', formats: { value: { $fn: 'bytes' } } } }, 'richText');
	checks.fnTemplateEscapesOnceRich = tr({ value: 'a{b}' }) === '[a\uff5bb\uff5d]';
	// A fn nested under formats resolves in raw mode (a probe fn echoes its mode).
	const tm = await fnIn({ $fn: 'template', args: { text: '{value}', formats: { value: { $fn: 'app.mode' } } } }, 'html');
	checks.fnTemplateSubFnRaw = tm({ value: 1 }) === 'raw';
	// No value is "", never "NaN" / 1970.
	for (const name of ['number', 'percent', 'compact', 'bytes', 'duration', 'datetime']) {
		const f = await fnIn({ $fn: name }, 'richText');
		checks['fnNaNBlank:' + name] = f(NaN) === '' && f({ value: NaN }) === '' && f(null) === '' && f(undefined) === '';
	}
	const dt = await fnIn({ $fn: 'datetime' }, 'richText');
	checks.datetimeNullBlank = dt(null) === '' && dt({ value: null }) === '' && dt(NaN) === '';
	// Tier boundaries and negatives.
	const by = await fnIn({ $fn: 'bytes' }, 'richText'), byb = await fnIn({ $fn: 'bytes', args: { binary: true } }, 'richText');
	checks.bytesTierBoundary = by(999999) === '1.0 MB' && by(999949) === '999.9 kB' && by(999) === '999 B' && by(999.6) === '1.0 kB' && by(1000) === '1.0 kB' && byb(1048575) === '1.0 MiB' && by(-1500) === '-1.5 kB' && by(-999999) === '-1.0 MB' && by(0) === '0 B';
	const du = await fnIn({ $fn: 'duration' }, 'richText');
	checks.durationTierBoundary = du(59950) === '1m 00s' && du(59940) === '59.9 s' && du(999) === '999 ms' && du(-5000) === '-5.0 s' && du(0) === '0 ms' && du(3599950) === '59m 59s';
	const dus = await fnIn({ $fn: 'duration', args: { unit: 's' } }, 'richText');
	checks.durationUnitSeconds = dus(90) === '1m 30s' && dus(0.5) === '500 ms';
}

// a cache entry made by an aborted draw is a miss, so the next draw fetches again instead of inheriting an AbortError.
async function runCacheCheck() {
	const fs = require('node:fs'), vm = require('node:vm');
	let src = fs.readFileSync(chartsPath, 'utf8');
	const end = src.lastIndexOf('})();');
	src = src.slice(0, end) + 'window.__t = { perDrawFetch, cachedFetch };\n' + src.slice(end);
	const calls = [];
	const fetch = (url, init) => { calls.push(url); return new Promise((res, rej) => init.signal.addEventListener('abort', () => rej(Object.assign(new Error('a'), { name: 'AbortError' })))); };
	const window = { echarts: {}, JuneauViews: {} };
	window.window = window;
	vm.runInContext(src, vm.createContext({ window, console: { error() {}, warn() {}, log() {} }, setTimeout, clearTimeout, AbortController, fetch }));
	const { perDrawFetch, cachedFetch } = window.__t;
	const cache = new Map();
	const f1 = perDrawFetch(null), p1 = cachedFetch(cache, f1)('/api/m');
	p1.catch(() => {});
	f1.abort();
	const f2 = perDrawFetch(null), p2 = cachedFetch(cache, f2)('/api/m');
	let state = 'pending';
	p2.then(() => { state = 'ok'; }, e => { state = e.name; });
	f2.abort();
	await new Promise(r => setTimeout(r, 0));
	// draw 2 fetched on its own (2 calls) and so only its own abort (not draw 1's) rejected it.
	checks.cachedFetchAbortedEntryIsMiss = calls.length === 2 && p1 !== p2 && f1.aborted === true && f2.aborted === true && state === 'AbortError';
	// A live (not aborted) entry is still shared.
	const f3 = perDrawFetch(null), g3 = cachedFetch(new Map(), f3);
	const a = g3('/api/n'), b = g3('/api/n');
	a.catch(() => {});
	checks.cachedFetchLiveEntryShared = a === b && calls.length === 3;
	f3.abort();
	// The parent signal aborts the controller, and aborting the fetcher itself is idempotent.
	const ac = new AbortController(), f4 = perDrawFetch(ac.signal);
	ac.abort();
	checks.perDrawFetchParentAbort = f4.aborted === true;
	f4.abort();
	checks.perDrawFetchPreAborted = perDrawFetch(ac.signal).aborted === true;
	// A non-string url (even an exotic one) is a clean rejection.
	const f5 = perDrawFetch(null);
	let msg = null;
	await f5.json(Object.create(null)).catch(e => { msg = e.message; });
	checks.perDrawFetchExoticUrl = msg === 'the url is not a string';
}

// Decorator row data, the decorators' fields and the surviving mutations.
async function runDecoratorChecks(env, line) {
	const fr = H.frame(env, DAYS, ROWS);
	const run = card => H.runPipeline(env, card, fr);
	const sid = (o, id) => o.series.find(x => x.id === id);
	// decorator row data (key meta) is data: a $fn inside it stays inert and cannot abort the draw.
	const mk = rows => ({ id: 'c', helper: line, decorators: [{ use: 'markers', values: rows }] });
	let o = await run(mk([{ at: 'Mon', label: 'a', meta: { $fn: 'compact' } }]));
	checks.metaFnInert = isPlain(sid(o, 'juneau-markers-0').markLine.data[0].meta) && sid(o, 'juneau-markers-0').markLine.data[0].meta.$fn === 'compact';
	let unknownOk = false;
	try { o = await run(mk([{ at: 'Mon', label: 'a', meta: { $fn: 'nope.x' } }])); unknownOk = sid(o, 'juneau-markers-0').markLine.data[0].meta.$fn === 'nope.x'; } catch (e) { unknownOk = false; }
	checks.metaUnknownFnInert = unknownOk;
	let otcOk = false;
	try { o = await run(mk([{ at: 'Mon', label: 'a', meta: { optionToContent: { $fn: 'compact' } } }])); otcOk = isPlain(sid(o, 'juneau-markers-0').markLine.data[0].meta.optionToContent); } catch (e) { otcOk = false; }
	checks.metaOptionToContentInert = otcOk;
	const bm = await run({ id: 'c', helper: line, decorators: [{ use: 'bands', values: [{ start: 'Mon', end: 'Tue', meta: { $fn: 'nope.x' } }] }] });
	checks.metaInertInBands = isPlain(sid(bm, 'juneau-bands-0').markArea.data[0][0].meta);
	// A decorator value that comes from DATA (at/start/end/label) is a scalar (string, number, Date); an object such as
	// {$fn} is undefined, so a data row neither aborts the draw (E-JS-90 for an unknown name) nor injects a function marker.
	const unk = { $fn: 'nope.x' }, reg = { $fn: 'compact' };
	const isDate = x => Object.prototype.toString.call(x) === '[object Date]';
	let scalarOk = false;
	try {
		o = await run(mk([{ at: unk, label: reg }, { at: reg, label: unk }, { at: [1], label: true }, { at: true, label: null }, { at: null, label: [1] }]));
		const d = sid(o, 'juneau-markers-0').markLine.data;
		scalarOk = d.length === 5 && d.every(x => x.xAxis === undefined && x.name === '');
	} catch (e) { scalarOk = false; }
	checks.dataRowNonScalarMarkers = scalarOk;
	o = await run(mk([{ at: 'Tue', label: 'D' }, { at: 5, label: 7 }, { at: new Date(0), label: 'x' }]));
	const sd = sid(o, 'juneau-markers-0').markLine.data;
	checks.dataRowScalarsKeptMarkers = sd[0].xAxis === 'Tue' && sd[0].name === 'D' && sd[1].xAxis === 5 && sd[1].name === '7' && isDate(sd[2].xAxis) && sd[2].xAxis.getTime() === 0;
	let bandOk = false;
	try {
		o = await run({ id: 'c', helper: line, decorators: [{ use: 'bands', label: 'T', values: [{ start: unk, end: reg, label: unk }, { start: [1], end: false, label: reg }] }] });
		const bd = sid(o, 'juneau-bands-0').markArea.data;
		bandOk = bd.length === 2 && bd.every(p => p[0].xAxis === undefined && p[1].xAxis === undefined && p[0].name === 'T');
	} catch (e) { bandOk = false; }
	checks.dataRowNonScalarBands = bandOk;
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'bands', values: [{ start: 'Mon', end: 5, label: 'Out' }, { start: new Date(0), end: 'Wed' }] }] });
	const bs = sid(o, 'juneau-bands-0').markArea.data;
	checks.dataRowScalarsKeptBands = bs[0][0].xAxis === 'Mon' && bs[0][1].xAxis === 5 && bs[0][0].name === 'Out' && isDate(bs[1][0].xAxis) && bs[1][1].xAxis === 'Wed';
	// The same through a fetched src (data from the network, not author config).
	fetched.length = 0;
	let srcOk = false;
	try {
		o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', src: '/api/rows-fn' }] });
		const fd = sid(o, 'juneau-markers-0').markLine.data;
		srcOk = fd.length === 1 && fd[0].xAxis === undefined && fd[0].name === '';
	} catch (e) { srcOk = false; }
	checks.dataRowNonScalarFromSrc = srcOk;
	// no $fn marker anywhere under toolbox.feature.dataView.
	for (const [n, dv] of [['optionToContent', { optionToContent: { $fn: 'compact' } }], ['title', { title: { $fn: 'compact' } }], ['lang', { lang: [{ $fn: 'compact' }, 'x'] }], ['nested', { x: { y: { $fn: 'compact' } } }]]) {
		let msg = null;
		try { await run({ id: 'c', option: { toolbox: { feature: { dataView: dv } }, series: [] } }); } catch (e) { msg = e.message; }
		checks['dataViewRefusesFn:' + n] = typeof msg === 'string' && msg.includes('E-JS-98') && msg.includes('a $fn may not format');
	}
	// Label text: goldens print [fn template], so evaluate the label fns.
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', values: [{ at: 'Tue', label: 'Deploy' }] }, { use: 'bands', values: [{ start: 'Mon', end: 'Tue', label: 'Out' }] },
		{ use: 'thresholds', values: [{ y: 3, label: 'SLO' }] }] });
	const lf = id => (sid(o, id).markLine || sid(o, id).markArea || sid(o, id).markPoint).label.formatter;
	checks.labelTextMarkers = lf('juneau-markers-0')({ name: 'Deploy' }) === 'Deploy' && lf('juneau-markers-0')({ name: 'a{b}' }) === 'a\uff5bb\uff5d';
	checks.labelTextBands = lf('juneau-bands-1')({ name: 'Out' }) === 'Out';
	checks.labelTextThresholds = lf('juneau-thresholds-2')({ name: 'SLO' }) === 'SLO';
	// Markers.style and the default.
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', values: [{ at: 'Tue', label: 'D' }], style: 'solid' }] });
	checks.markersStyle = sid(o, 'juneau-markers-0').markLine.lineStyle.type === 'solid';
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', values: [{ at: 'Tue', label: 'D' }] }] });
	checks.markersStyleDefault = sid(o, 'juneau-markers-0').markLine.lineStyle.type === 'dashed';
	// Markers.now / nowAt: a time axis gets the epoch millis, a category axis today's date.
	const stamps = H.frame(env, STAMPS, STAMP_ROWS);
	const t0 = Date.now();
	o = await H.runPipeline(env, { id: 'c', helper: { kind: 'line', x: { field: 't', type: 'time' }, series: [{ field: 'n' }] }, decorators: [{ use: 'markers', values: [], now: { label: 'Now' } }] }, stamps);
	const nowItem = sid(o, 'juneau-markers-0').markLine.data.find(d => d.name === 'Now');
	checks.markersNowTime = !!nowItem && typeof nowItem.xAxis === 'number' && nowItem.xAxis >= t0 && nowItem.xAxis <= Date.now();
	const today = () => { const d = new Date(), two = n => (n < 10 ? '0' : '') + n; return d.getFullYear() + '-' + two(d.getMonth() + 1) + '-' + two(d.getDate()); };
	const todayUtc = () => new Date().toISOString().slice(0, 10);
	const d0 = today(), u0 = todayUtc();
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', values: [], now: { label: 'Now' } }] });
	const catItem = sid(o, 'juneau-markers-0').markLine.data.find(d => d.name === 'Now');
	checks.markersNowCategory = !!catItem && (catItem.xAxis === d0 || catItem.xAxis === today());
	o = await run({ id: 'c', option: { useUTC: true, xAxis: { type: 'category' }, series: [] }, decorators: [{ use: 'markers', values: [], now: { label: 'Now' } }] });
	const catUtc = sid(o, 'juneau-markers-0').markLine.data.find(d => d.name === 'Now');
	checks.markersNowCategoryUtc = !!catUtc && (catUtc.xAxis === u0 || catUtc.xAxis === todayUtc());
	// datetime pattern "auto" follows the span of the frame's time column.
	const tsf = ms => H.frame(env, STAMPS, [{ t: 0, n: 1 }, { t: ms, n: 2 }]);
	const dtIn = async frame => (await H.runPipeline(env, { id: 'c', option: { tooltip: { renderMode: 'richText', formatter: { $fn: 'datetime', args: { utc: true } } }, series: [] } }, frame)).tooltip.formatter;
	const dayMs = 86400000;
	checks.datetimeAuto = (await dtIn(tsf(3 * 3600000)))(3600000) === '01:00 AM'
		&& (await dtIn(tsf(10 * dayMs)))(dayMs) === 'Jan 2, 1970'
		&& (await dtIn(tsf(200 * dayMs)))(dayMs) === 'Jan 1970'
		&& (await dtIn(H.frame(env, DAYS, ROWS)))(dayMs) === 'Jan 2, 1970, 12:00 AM';
	// Annotations: the average preset, label on presets and on explicit values.
	o = await run({ id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES }, decorators: [{ use: 'annotations', presets: ['average', 'bogus'] }] });
	checks.annotationsAverage = JSON.stringify(o.series[0].markPoint.data) === JSON.stringify([{ type: 'average', name: 'average' }]);
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'annotations', values: [{ x: 'Wed', y: 4, label: 'Peak' }], label: '{name}!' }] });
	checks.annotationsValuesLabel = sid(o, 'juneau-annotations-0').markPoint.label.formatter({ name: 'Peak' }) === 'Peak!';
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'annotations', values: [{ x: 'Wed', y: 4, label: 'Peak' }] }] });
	checks.annotationsValuesLabelDefault = sid(o, 'juneau-annotations-0').markPoint.label.formatter({ name: 'Peak' }) === 'Peak';
	o = await run({ id: 'c', helper: { kind: 'bar', x: { field: 'day' }, series: SERIES }, decorators: [{ use: 'annotations', presets: ['max'], label: 'top {value}' }] });
	checks.annotationsPresetLabel = o.series[0].markPoint.label.formatter({ value: 5 }) === 'top 5';
	// a single-object option.series still gets the presets.
	o = await run({ id: 'c', option: { xAxis: { type: 'category' }, yAxis: {}, series: { type: 'bar', id: 's1', data: [1, 2] } }, decorators: [{ use: 'annotations', presets: ['min'] }] });
	const one = Array.isArray(o.series) ? o.series[0] : o.series;
	checks.annotationsSingleObjectSeries = one.id === 's1' && Array.isArray(one.markPoint && one.markPoint.data) && one.markPoint.data[0].type === 'min';
	// Bands minGap: a one-bucket gap is dropped when minGap is 2.
	const gapRows = [{ t: 0, n: 1 }, { t: 3600000, n: 0 }, { t: 7200000, n: 2 }, { t: 10800000, n: 0 }, { t: 14400000, n: 0 }, { t: 18000000, n: 3 }];
	const gf = H.frame(env, STAMPS, gapRows), gcard = md => ({ id: 'c', helper: { kind: 'line', x: { field: 't', type: 'time' }, series: [{ field: 'n' }] }, decorators: [{ use: 'bands', auto: 'gaps', minGap: md }] });
	const g1 = await H.runPipeline(env, gcard(1), gf), g2 = await H.runPipeline(env, gcard(2), gf), g3 = await H.runPipeline(env, gcard(3), gf);
	checks.bandsMinGap = sid(g1, 'juneau-bands-0').markArea.data.length === 2 && sid(g2, 'juneau-bands-0').markArea.data.length === 1 && sid(g3, 'juneau-bands-0').markArea.data.length === 0
		&& sid(g2, 'juneau-bands-0').markArea.data[0][0].xAxis === 9000000;
	// A src answering {rows: [...]} works like an array; credentials stay same-origin.
	fetched.length = 0;
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', src: '/api/rows-object' }] });
	checks.srcRowsObject = sid(o, 'juneau-markers-0').markLine.data[0].name === 'FromRows';
	checks.fetchCredentialsSameOrigin = fetched.length === 1 && fetched[0].credentials === 'same-origin';
	// an author field name never reads an Object member of a row.
	o = await run({ id: 'c', helper: line, decorators: [{ use: 'markers', values: [{ at: 'Mon', label: 'x' }], at: 'constructor', label: 'toString' },
		{ use: 'bands', values: [{ start: 'Mon', end: 'Tue' }], start: 'constructor', end: 'toString' }] });
	const md = sid(o, 'juneau-markers-0').markLine.data[0], bd = sid(o, 'juneau-bands-1').markArea.data[0];
	checks.fieldNamesHasOwn = md.xAxis === undefined && md.name === '' && bd[0].xAxis === undefined && bd[1].xAxis === undefined;
}

function isPlain(v) {
	return v !== null && typeof v === 'object' && !Array.isArray(v);
}
