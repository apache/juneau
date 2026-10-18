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
 * juneau-charts-harness.cjs - a reusable Node harness for juneau-charts.js: a recording fake echarts, a loader, and
 * runPipeline, which returns the option a card would draw as JSON-safe data.  Apps point it at their own decorators
 * and fns.
 *
 *   const H = require('./juneau-charts-harness.cjs');
 *   const env = H.load('/path/juneau-charts.js', {registrations: [['decorator', 'app.grid', fn]]});
 *   const option = await H.runPipeline(env, card, H.frame([{name: 'day', type: 'string'}], rows));
 *   H.snapshot(option);   // functions become "[fn <name>]", typed arrays "[Float64Array <n>: 3,5,4]"
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

// A recording fake of the echarts 6.1.0 surface juneau-charts.js uses.  Every call lands in calls[].
function fakeEcharts() {
	const calls = [], instances = [];
	const echarts = {
		init(el, theme, opts) {
			const handlers = {};
			let last = null;
			const inst = {
				el, theme, opts, group: '', disposed: false, options: [], optionOpts: [], themes: [],
				setOption(o, so) { calls.push(['setOption', o, so]); inst.options.push(o); inst.optionOpts.push(so); last = o; },
				getOption() { calls.push(['getOption']); return last; },
				setTheme(t) { calls.push(['setTheme', t]); inst.themes.push(t); },
				on(ev, fn) { calls.push(['on', ev]); handlers[ev] ||= []; handlers[ev].push(fn); },
				off(ev) { calls.push(['off', ev]); delete handlers[ev]; },
				dispatchAction(a) { calls.push(['dispatchAction', a]); },
				resize() { calls.push(['resize']); },
				dispose() { calls.push(['dispose']); inst.disposed = true; },
				getDataURL(o) { calls.push(['getDataURL', o]); return 'data:image/' + (o?.type || 'png') + ';base64,RkFLRQ=='; },
				fire(ev, params) { for (const fn of handlers[ev] || []) fn(params); }
			};
			calls.push(['init', theme, opts]);
			instances.push(inst);
			return inst;
		},
		connect(group) { calls.push(['connect', group]); },
		registerTheme(name, theme) { calls.push(['registerTheme', name, theme]); },
		registerMap(name, geo) { calls.push(['registerMap', name]); }
	};
	return { echarts, calls, instances };
}

// Loads the glue into a fresh context.  opts.window extends the window; opts.registrations are queue triples (or a
// function given window.JuneauCharts) applied after load.
function load(chartsPath, opts) {
	opts = opts || {};
	const errors = [], warnings = [];
	const fake = fakeEcharts();
	const window = { echarts: fake.echarts, JuneauViews: {}, ...opts.window || {} };
	const sandbox = {
		window, console: { error: m => errors.push(String(m)), warn: m => warnings.push(String(m)), log: () => {} },
		setTimeout, clearTimeout, AbortController,
		fetch: opts.fetch || (() => Promise.reject(new TypeError('no fetch')))
	};
	window.window = window;
	if (Array.isArray(opts.queue)) window.JuneauConsoleCharts = opts.queue.slice();
	const ctx = vm.createContext(sandbox);
	if (opts.spy) opts.spy(ctx);
	vm.runInContext(fs.readFileSync(chartsPath, 'utf8'), ctx, { filename: 'juneau-charts.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	const regs = opts.registrations;
	if (typeof regs === 'function') regs(window.JuneauCharts);
	else for (const t of regs || []) window.JuneauConsoleCharts.push(t);
	return { window, charts: window.JuneauCharts, errors, warnings, fake, context: ctx };
}

// A frame from rows, through the glue's own built-in "rows" builder.
function frame(env, dims, rows) {
	const b = env.charts.builder.get('rows').create(dims, {});
	for (const r of rows) b.onRecord(r);
	return b.finish();
}

function runPipeline(env, card, f, ctx) {
	return env.charts.build(card, f, ctx);
}

// Arrays and typed arrays of up to INLINE_MAX elements print inline; longer ones print as the length plus the first and
// last 3 elements.  NaN prints as null.
const INLINE_MAX = 16;

function cellText(x) {
	if (typeof x === 'number') return Number.isNaN(x) ? 'null' : String(x);
	if (x === null || x === undefined) return 'null';
	return typeof x === 'object' ? JSON.stringify(x) : String(x);
}

function listText(name, list) {
	const items = Array.from(list, cellText);
	const body = items.length <= INLINE_MAX ? items.join(',')
		: items.slice(0, 3).join(',') + ' ... ' + items.slice(-3).join(',');
	return '[' + name + ' ' + items.length + ': ' + body + ']';
}

function snapshot(option) {
	return JSON.parse(JSON.stringify(option, function (k, v) {
		if (typeof v === 'function') return '[fn ' + (v.juneauFn || 'anonymous') + ']';
		if (v && typeof v === 'object' && ArrayBuffer.isView(v)) return listText(v.constructor.name, v);
		if (Array.isArray(v) && v.length > INLINE_MAX) return listText('Array', v);
		return v;
	}));
}

module.exports = { fakeEcharts, load, frame, runPipeline, snapshot };
