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
 * juneau-charts.js - the console chart card (type "chart"), drawn with Apache ECharts (window.echarts, served by the
 * "echarts" toolkit pack).  Every draw runs one pipeline: frame -> bound dataset -> helper/option -> decorators ->
 * patch -> render-mode rule -> large-data defaults -> $fn -> setOption.
 *
 *   JuneauCharts.fn         .register(name, factory)       factory(args, {renderMode, frame, useUTC, resolve}) -> function
 *                                                          renderMode is "richText" or "html": escape what the function returns for it
 *                                                          (the built-ins do).  A custom fn that the built-in "template" fn runs as one of
 *                                                          its "formats" receives renderMode "raw" and must NOT escape: the template escapes
 *                                                          the substituted value once, so escaping twice would print "&amp;lt;".
 *   JuneauCharts.decorator  .register(name, fn)            fn(option, frame, spec, ctx) -> option | Promise<option>
 *   JuneauCharts.parser     .register(name, {create})      create(opts) -> {push(chunkText), end()}
 *   JuneauCharts.builder    .register(name, {create})      create(dims, opts) -> {onRecord(record), finish()}
 *   JuneauCharts.transform  .register(name, fn)            fn(frame, args) -> frame
 *   JuneauCharts.<registry> .get(name), .names()
 *   JuneauCharts.build(card, frame, ctx?)                  -> Promise<option> (the pipeline without setOption)
 *   window.JuneauConsoleCharts = [[registry, name, impl], ...]   queue drained when this script loads
 *
 * App names are dotted ("app.calendarGrid"); bare names are the built-ins (E-JS-97).  No raw HTML-sink assignment:
 * every node is built with createElement, textContent and setAttribute.
 */
(function () {
	"use strict";

	// ---- A. constants ---------------------------------------------------------------------------------------------

	const MIN_REFRESH_MS = 5000;
	const DEFAULT_HEIGHT = 240, MIN_HEIGHT = 120;
	const DECORATOR_TIMEOUT_MS = 10000;
	const ZOOM_DEBOUNCE_MS = 150;
	const TABLE_MAX = 500;
	const INFER_ROWS = 100;
	const LARGE = { threshold: 5000, progressive: 5000, progressiveThreshold: 10000 };
	// The same literal as BusWiringValidator.EXTENSION_NAME (BusWiringValidator_Test e01 pins them equal).
	const EXTENSION_NAME_RE = /^[a-z][a-zA-Z0-9]*(\.[a-z][a-zA-Z0-9-]*)*$/;
	const EVENT_NAME_RE = /^[a-z][a-z0-9_]{0,31}$/;
	const REGISTRIES = ["fn", "decorator", "parser", "builder", "transform"];
	// The theme tokens the ECharts theme is built from (ConsoleChromeMixin_Test c03 reads this list).
	const THEME_TOKENS = ["--jc-chart-1", "--jc-chart-2", "--jc-chart-3", "--jc-chart-4", "--jc-chart-5", "--jc-chart-6",
		"--jc-chart-7", "--jc-chart-8", "--jc-chart-bg", "--jc-chart-text", "--jc-chart-axis", "--jc-chart-grid",
		"--jc-chart-tooltip-bg", "--jc-chart-tooltip-text", "--jc-chart-mark", "--jc-chart-band", "--jc-chart-heat-low",
		"--jc-chart-heat-high"];
	const TOKEN_FALLBACKS = Object.freeze({
		"--jc-chart-1": "#1b6ac9", "--jc-chart-2": "#e07a1f", "--jc-chart-3": "#2e8540", "--jc-chart-4": "#9b3fb5",
		"--jc-chart-5": "#c23934", "--jc-chart-6": "#0e8a8a", "--jc-chart-7": "#7a6a00", "--jc-chart-8": "#5a5a5a",
		"--jc-chart-bg": "transparent", "--jc-chart-text": "#080707", "--jc-chart-axis": "#dddbda",
		"--jc-chart-grid": "#ececec", "--jc-chart-tooltip-bg": "#ffffff", "--jc-chart-tooltip-text": "#080707",
		"--jc-chart-mark": "#5a5a5a", "--jc-chart-band": "rgba(0, 0, 0, 0.06)", "--jc-chart-heat-low": "#e8f1fb",
		"--jc-chart-heat-high": "#1b6ac9", "--jc-danger": "#c23934", "--jc-success": "#2e8540", "--jc-warn": "#b86e00",
		"--jc-danger-text": "#c23934", "--jc-success-text": "#2e8540"
	});
	// Lookup tables are null-prototype: an author key such as "constructor" or "toString" must miss, not find an Object member.
	const table = o => Object.assign(Object.create(null), o);
	// A tone colours text and thin marks, so it reads the text role (legible on the scheme's surface), not the badge fill.
	const TONES = table({ danger: "--jc-danger-text", ok: "--jc-success-text", warning: "--jc-warn" });
	// A text role the page does not declare takes the page's fill value, so a theme that only sets the fill still drives the tone.
	const TEXT_ROLE_FILLS = table({ "--jc-danger-text": "--jc-danger", "--jc-success-text": "--jc-success" });
	const HELPER_KEYS = ["x", "y", "series", "stacked", "orientation", "legend", "label", "value", "center", "scale", "symbolSize", "groupBy", "size", "color", "tileFields"];
	const AXIS_TYPE = table({ category: "category", number: "value", time: "time" });
	const Y_FORMAT = table({
		integer: { $fn: "number", args: { digits: 0 } },
		decimal: { $fn: "number", args: { digits: 2 } },
		percent: { $fn: "percent" },
		compact: { $fn: "compact" }
	});
	const MSG = {
		"E-JS-60": "chart card '%s' has unknown kind '%s'; registered kinds: '%s'",
		"E-JS-61": "chart card '%s' needs ECharts (window.echarts), which did not load; the 'echarts' toolkit pack serves org.webjars.npm:echarts",
		"E-JS-62": "chart card '%s' fetch failed: '%s'",
		"E-JS-63": "chart card '%s' series field '%s' is not in the data rows",
		"E-JS-75": "'%s' refreshMs '%s' is below the minimum '%s'; using the minimum",
		"E-JS-79": "chart card '%s' onClick.href resolved to a disallowed URL '%s'",
		"E-JS-80": "chart card '%s' setOption failed: '%s'",
		"E-JS-90": "'%s' references unknown $fn '%s'; registered: '%s'",
		"E-JS-91": "chart card '%s' uses unknown decorator '%s'; registered: '%s'",
		"E-JS-95": "topic '%s' carries a malformed data handle: %s",
		"E-JS-97": "%s.register: name '%s' %s",
		"E-JS-98": "'%s' %s '%s' failed: '%s'",
		"E-JS-103": "chart card '%s' ignored cmd op '%s': %s",
		"E-JS-108": "chart card '%s' action hook JuneauViews.actions is unavailable"
	};

	function fmt(t) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return t.replace(/%s/g, () => String(args[i++]));
	}

	function chartError(code) {
		const e = new Error(code + ": " + fmt.apply(null, [MSG[code]].concat(Array.prototype.slice.call(arguments, 1))));
		e.code = code;
		return e;
	}

	function log(e) {
		console.error("[juneau-charts] " + (e && e.message || e));
	}

	function warn(msg) {
		console.warn("[juneau-charts] " + msg);
	}

	function isObj(v) {
		return v !== null && typeof v === "object" && !Array.isArray(v) && !ArrayBuffer.isView(v);
	}

	function arr(v) {
		return Array.isArray(v) ? v : [];
	}

	function msgOf(e) {
		return e && e.message || String(e);
	}

	// ---- B. registries --------------------------------------------------------------------------------------------

	const impls = Object.create(null);
	for (const k of REGISTRIES) impls[k] = new Map();
	// [{kind, name, fn}] callbacks run when a name is registered (late registration through the queue).
	const waiters = [];

	function register(kind, name, impl, builtin) {
		let reason = null;
		if (typeof name !== "string" || !EXTENSION_NAME_RE.test(name)) reason = "is not a valid name";
		else if (!builtin && name.indexOf(".") < 0) reason = "is bare; app names are dotted ('app." + name + "')";
		else if (impls[kind].has(name)) reason = "is already registered";
		else if (kind === "parser" || kind === "builder") {
			if (!impl || typeof impl.create !== "function") reason = "has no create function";
		} else if (typeof impl !== "function") reason = "is not a function";
		if (reason) {
			const e = chartError("E-JS-97", "JuneauCharts." + kind, name, reason);
			log(e);
			throw e;
		}
		impls[kind].set(name, impl);
		wake(kind, name);
	}

	function registryApi(kind) {
		return Object.freeze({
			register: (name, impl) => register(kind, name, impl, false),
			get: name => impls[kind].get(name) || null,
			names: () => Array.from(impls[kind].keys()).sort()
		});
	}

	function waitFor(kind, name, fn, owner) {
		waiters.push({ kind: kind, name: name, fn: fn, owner: owner });
	}

	// A destroyed card's pending waiters are dropped, not kept alive until the name is registered.
	function dropWaiters(owner) {
		for (let i = waiters.length - 1; i >= 0; i--) if (waiters[i].owner === owner) waiters.splice(i, 1);
	}

	function wake(kind, name) {
		for (const w of waiters.slice()) {
			if (w.kind !== kind || w.name !== name) continue;
			waiters.splice(waiters.indexOf(w), 1);
			try { w.fn(); } catch (e) { log(e); }
		}
	}

	function queueEntry(entry) {
		if (!Array.isArray(entry) || entry.length !== 3 || REGISTRIES.indexOf(entry[0]) < 0) {
			log(new TypeError("JuneauConsoleCharts entries are [registry, name, impl] triples; registry is one of " + REGISTRIES.join(", ")));
			return;
		}
		try {
			register(entry[0], entry[1], entry[2], false);
		} catch (e) {
			if (!e.code) log(e);
		}
	}

	// ---- C. frames: the built-in "rows" builder and the binder ------------------------------------------------------

	function sniff(v) {
		if (typeof v === "number") return "number";
		if (typeof v === "boolean") return "bool";
		return "string";
	}

	function toNumber(v, type) {
		if (v == null || v === "") return NaN;
		if (type === "time" && typeof v === "string") return Date.parse(v);
		const n = typeof v === "number" ? v : Number(v);
		return isFinite(n) ? n : NaN;
	}

	function frameOf(dims, records) {
		const n = records.length, columns = {};
		for (const d of dims) {
			if (d.type === "string") {
				const codes = new Int32Array(n), dict = [], index = new Map();
				for (let i = 0; i < n; i++) {
					const v = records[i][d.name];
					if (v == null) { codes[i] = -1; continue; }
					const s = String(v);
					let c = index.get(s);
					if (c === undefined) { c = dict.length; dict.push(s); index.set(s, c); }
					codes[i] = c;
				}
				columns[d.name] = { codes: codes, dict: dict };
			} else if (d.type === "bool") {
				const col = new Int8Array(n);
				for (let i = 0; i < n; i++) {
					const v = records[i][d.name];
					col[i] = v == null ? -1 : v ? 1 : 0;
				}
				columns[d.name] = col;
			} else {
				const col = new Float64Array(n);
				for (let i = 0; i < n; i++) col[i] = toNumber(records[i][d.name], d.type);
				columns[d.name] = col;
			}
		}
		return { rowCount: n, truncated: false, dims: dims, columns: columns };
	}

	const rowsBuilder = {
		create: function (dims) {
			const records = [];
			let dropped = 0;
			return {
				onRecord: r => { if (isObj(r)) records.push(r); else dropped++; },
				finish: () => Object.assign(frameOf(dims, records), { droppedRows: dropped })
			};
		}
	};

	function fieldOf(v) {
		return typeof v === "string" ? v : isObj(v) && typeof v.field === "string" ? v.field : null;
	}

	function seriesOf(h) {
		return arr(h.series).map(s => typeof s === "string" ? { field: s } : s).filter(isObj);
	}

	// [{name, type}] for the fields a helper reads, in order.
	function helperFields(h) {
		const out = [];
		const add = (name, type) => { if (name && !out.some(d => d.name === name)) out.push({ name: name, type: type }); };
		const xType = isObj(h.x) ? h.x.type : null;
		if (h.kind === "line" || h.kind === "bar") {
			add(fieldOf(h.x), xType === "time" ? "time" : xType === "number" ? "number" : "string");
			for (const s of seriesOf(h)) add(s.field, "number");
		} else if (h.kind === "scatter") {
			add(fieldOf(h.x), xType === "time" ? "time" : "number");
			for (const s of seriesOf(h)) add(s.field, "number");
			const size = isObj(h.symbolSize) && isObj(h.symbolSize.args) ? h.symbolSize.args.field : null;
			add(size, "number");
		} else if (h.kind === "donut" || h.kind === "pie") {
			add(h.label, "string");
			add(h.value, "number");
		} else if (h.kind === "heatmap") {
			add(fieldOf(h.x), "string");
			add(fieldOf(h.y), "string");
			add(h.value, "number");
		} else if (h.kind === "treemap") {
			// size first: a field that is both a weight and a label or group stays numeric.
			if (typeof h.size === "string") add(h.size, "number");
			for (const g of arr(h.groupBy)) if (typeof g === "string") add(g, "string");
			add(h.label, "string");
			if (isObj(h.color) && typeof h.color.field === "string") add(h.color.field, "string");
		}
		return out;
	}

	function inferDims(rows, h) {
		const dims = h ? helperFields(h) : [];
		const seen = new Set(dims.map(d => d.name));
		for (const r of rows.slice(0, INFER_ROWS)) {
			if (!isObj(r)) continue;
			for (const k of Object.keys(r)) {
				if (seen.has(k) || r[k] == null) continue;
				seen.add(k);
				dims.push({ name: k, type: sniff(r[k]) });
			}
		}
		// A tile field that is null or absent in the sniffed rows is still a column, so later rows can fill meta.
		if (h && h.kind === "treemap")
			for (const f of arr(h.tileFields))
				if (typeof f === "string" && !seen.has(f)) { seen.add(f); dims.push({ name: f, type: "string" }); }
		return dims;
	}

	// Rows (inline data or a src response) to a frame.  E-JS-63 when rows exist but none has a helper field.
	function rowsFrame(card, rows) {
		const h = helperOf(card);
		const objects = rows.filter(isObj);
		if (h && objects.length) {
			for (const d of helperFields(h))
				if (!objects.some(r => Object.hasOwn(r, d.name))) throw chartError("E-JS-63", card.id, d.name);
		}
		const b = impls.builder.get("rows").create(inferDims(rows, h), {});
		for (const r of rows) b.onRecord(r);
		return b.finish();
	}

	function cell(frame, d, i) {
		const col = frame.columns[d.name];
		if (!col) return null;
		if (d.type === "string") {
			const c = col.codes[i];
			return c < 0 ? null : col.dict[c];
		}
		if (d.type === "bool") return col[i] < 0 ? null : col[i] === 1;
		const v = col[i];
		return v === v ? v : null;
	}

	function rowAt(frame, i) {
		const out = {};
		for (const d of frame.dims) out[d.name] = cell(frame, d, i);
		return out;
	}

	// The keyed columns ECharts reads: numeric columns by reference, string and bool columns decoded once per frame.
	const keyed = new WeakMap();

	function keyedSource(frame) {
		let src = keyed.get(frame);
		if (src) return src;
		src = {};
		for (const d of frame.dims) {
			const col = frame.columns[d.name];
			if (!col) continue;
			if (d.type === "string" || d.type === "bool") {
				const out = new Array(frame.rowCount);
				for (let i = 0; i < frame.rowCount; i++) out[i] = cell(frame, d, i);
				src[d.name] = out;
			} else {
				src[d.name] = col;
			}
		}
		keyed.set(frame, src);
		return src;
	}

	function datasetOf(frame) {
		return {
			id: "juneau",
			dimensions: frame.dims.map(d => ({ name: d.name, type: d.type === "time" ? "time" : d.type === "string" ? "ordinal" : "number" })),
			source: keyedSource(frame)
		};
	}

	// Datasets inside baseOption, media[*].option and options[*] are not bound or index-shifted (unsupported).
	function bind(option, frame) {
		const ds = datasetOf(frame);
		if (option.dataset == null) {
			option.dataset = [ds];
			return option;
		}
		const own = Array.isArray(option.dataset) ? option.dataset : [option.dataset];
		const series = Array.isArray(option.series) ? option.series : isObj(option.series) ? [option.series] : [];
		const wants = series.some(s => isObj(s) && (s.datasetId === "juneau" || (s.datasetId == null && s.data == null)));
		if (!wants) return option;
		option.dataset = [ds].concat(own);
		// The prepended dataset shifts every index-based reference by one.
		for (const s of series)
			if (isObj(s) && typeof s.datasetIndex === "number") s.datasetIndex += 1;
		for (const d of own)
			if (isObj(d) && typeof d.fromDatasetIndex === "number") d.fromDatasetIndex += 1;
		return option;
	}

	// ---- D. helpers ----------------------------------------------------------------------------------------------

	// The card's helper: card.helper, or the legacy flat form ({kind, x, series, ...} at the top level).
	function helperOf(card) {
		if (isObj(card.helper)) return card.helper;
		if (typeof card.kind !== "string") return null;
		const h = { kind: card.kind };
		for (const k of HELPER_KEYS) if (card[k] !== undefined) h[k] = card[k];
		return h;
	}

	function legendOf(h) {
		if (h.legend === "none") return { show: false };
		if (h.legend === "right") return { right: 0, orient: "vertical" };
		return { bottom: 0 };
	}

	// A pie or donut keeps its outside slice labels clear of the legend: the legend gets a gap between entries and the ring is
	// lifted (bottom legend) or shifted left (right legend) and shrunk so the bottom or right label has room.
	function pieLegendOf(h) {
		const legend = legendOf(h);
		if (legend.show === false) return { legend: legend, center: ["50%", "50%"], radius: 0.7 };
		legend.itemGap = 16;
		if (h.legend === "right") return { legend: legend, center: ["40%", "50%"], radius: 0.7 };
		legend.bottom = 8;
		return { legend: legend, center: ["50%", "42%"], radius: 0.6 };
	}

	function toneColor(tone, tokens) {
		if (typeof tone === "number" && tone >= 1 && tone <= 8) return tokens["--jc-chart-" + tone];
		if (typeof tone === "string" && TONES[tone]) return tokens[TONES[tone]];
		return null;
	}

	function yAxisOf(h) {
		const y = isObj(h.y) ? h.y : {};
		const axis = { type: "value" };
		if (y.label != null) axis.name = String(y.label);
		if (typeof y.min === "number") axis.min = y.min;
		if (Y_FORMAT[y.format]) axis.axisLabel = { formatter: copyValue(Y_FORMAT[y.format]) };
		return axis;
	}

	function cartesian(type) {
		return function (h, frame, tokens) {
			const x = isObj(h.x) ? h.x : { field: fieldOf(h.x) };
			const xAxis = { type: AXIS_TYPE[x.type] || "category" };
			if (x.label != null) xAxis.name = String(x.label);
			const yAxis = yAxisOf(h);
			const horizontal = h.orientation === "horizontal";
			const series = seriesOf(h).map(s => {
				const out = { id: s.field, name: s.label || s.field, type: type,
					encode: horizontal ? { y: x.field, x: s.field } : { x: x.field, y: s.field } };
				if (type === "bar" && h.stacked === true) out.stack = "total";
				const color = toneColor(s.tone, tokens);
				if (color) {
					out.itemStyle = { color: color };
					if (type === "line") out.lineStyle = { color: color };
				}
				return out;
			});
			return { tooltip: { trigger: "axis" }, legend: legendOf(h),
				xAxis: horizontal ? yAxis : xAxis, yAxis: horizontal ? xAxis : yAxis, series: series };
		};
	}

	function pieHelper(donut) {
		return function (h, frame, tokens) {
			const fit = pieLegendOf(h);
			const outer = Math.round(fit.radius * 100);
			const series = { id: "slices", type: "pie", radius: donut ? [Math.round(outer * 0.65) + "%", outer + "%"] : outer + "%",
				center: fit.center, minShowLabelAngle: 7.2, encode: { itemName: h.label, value: h.value } };
			const option = { tooltip: { trigger: "item" }, legend: fit.legend, series: [series] };
			if (donut && h.center != null) {
				let text = String(h.center);
				if (h.center === "total") {
					const col = frame.columns[h.value];
					let total = 0;
					if (col) for (let i = 0; i < frame.rowCount; i++) if (col[i] === col[i]) total += col[i];
					text = new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(total);
				}
				// A zero-size group sits on the ring's centre; ECharts places a text element's bounding-box edge, not its middle, at left/top.
				option.graphic = [{ id: "juneau-center", type: "group", left: fit.center[0], top: fit.center[1], bounding: "raw", silent: true,
					children: [{ type: "text", style: { text: text, fill: tokens["--jc-chart-text"], fontSize: 16, textAlign: "center", textVerticalAlign: "middle" } }] }];
			}
			return option;
		};
	}

	function heatmap(h, frame, tokens) {
		const xf = fieldOf(h.x), yf = fieldOf(h.y);
		let min = 0, max = 1;
		if (isObj(h.scale) && typeof h.scale.min === "number" && typeof h.scale.max === "number") {
			min = h.scale.min;
			max = h.scale.max;
		} else {
			const col = frame.columns[h.value];
			let lo = Infinity, hi = -Infinity;
			if (col) for (let i = 0; i < frame.rowCount; i++) if (col[i] === col[i]) { lo = Math.min(lo, col[i]); hi = Math.max(hi, col[i]); }
			if (lo <= hi) { min = lo; max = hi; }
		}
		const dim = frame.dims.findIndex(d => d.name === h.value);
		return {
			tooltip: { trigger: "item" },
			xAxis: { type: "category" },
			yAxis: { type: "category" },
			visualMap: { type: "continuous", show: false, min: min, max: max, dimension: dim < 0 ? 0 : dim,
				inRange: { color: [tokens["--jc-chart-heat-low"], tokens["--jc-chart-heat-high"]] } },
			series: [{ id: "cells", type: "heatmap", encode: { x: xf, y: yf, value: h.value } }]
		};
	}

	function scatter(h) {
		const x = isObj(h.x) ? h.x : { field: fieldOf(h.x) };
		const xAxis = { type: x.type === "time" ? "time" : "value" };
		if (x.label != null) xAxis.name = String(x.label);
		const series = seriesOf(h).map(s => {
			const out = { id: s.field, name: s.label || s.field, type: "scatter", encode: { x: x.field, y: s.field } };
			if (h.symbolSize != null) out.symbolSize = copyValue(h.symbolSize);
			return out;
		});
		return { tooltip: { trigger: "item" }, legend: legendOf(h), xAxis: xAxis, yAxis: yAxisOf(h), series: series };
	}

	const TREEMAP_ROOT = "jc-root";
	// ECharts looks levels up by node depth and counts its own virtual root as depth 0, so the invisible top node is level 1.
	const TREEMAP_LEVELS = [
		{},
		{ itemStyle: { borderWidth: 0, gapWidth: 0 }, upperLabel: { show: false } },
		{ itemStyle: { borderWidth: 3, gapWidth: 3 } },
		{ itemStyle: { borderWidth: 2, gapWidth: 2 } },
		{ itemStyle: { borderWidth: 1, gapWidth: 1 } }
	];
	const PALETTE_SLOT_RE = /^--jc-chart-[1-8]$/;

	// Treemap tones: the chart tone names or a palette slot token; anything else is neutral.
	function treemapTone(tone, tokens) {
		if (typeof tone === "string" && PALETTE_SLOT_RE.test(tone)) return tokens[tone] || null;
		return typeof tone === "string" ? toneColor(tone, tokens) : null;
	}

	// Frame rows to nested nodes: one level per groupBy field, one leaf per row, all under one invisible top node so that
	// "fit" has a target.  Ids are encoded group paths, so they are stable across refreshes.
	function treemapTree(h, frame, tokens) {
		const dims = new Map(frame.dims.map(d => [d.name, d]));
		const val = (name, i) => dims.has(name) ? cell(frame, dims.get(name), i) : null;
		// An encoded segment never contains a bare "%", so "%" stands for a null or empty value and "jc-root%" for a value
		// that would otherwise equal the synthetic top id.
		const seg = v => {
			const e = v == null || String(v) === "" ? "%" : encodeURIComponent(String(v));
			return e === TREEMAP_ROOT ? e + "%" : e;
		};
		const groups = arr(h.groupBy).filter(g => typeof g === "string");
		const color = isObj(h.color) ? h.color : {};
		const tones = new Map(arr(color.legend).filter(isObj).map(e => [String(e.value), e.tone]));
		const tiles = arr(h.tileFields).filter(f => typeof f === "string");
		const top = { id: TREEMAP_ROOT, name: "", children: [] };
		const byId = new Map(), used = new Set([TREEMAP_ROOT]);
		for (let i = 0; i < frame.rowCount; i++) {
			let parent = top, path = "";
			groups.forEach((g, d) => {
				const v = val(g, i);
				path = d === 0 ? seg(v) : path + "/" + seg(v);
				let node = byId.get(path);
				if (!node) {
					node = { id: path, name: v == null ? "" : String(v), children: [] };
					byId.set(path, node);
					used.add(path);
					parent.children.push(node);
				}
				parent = node;
			});
			const label = val(h.label, i), base = (groups.length ? path + "/" : "") + seg(label);
			let id = base;
			for (let n = 2; used.has(id); n++) id = base + "~" + n;
			used.add(id);
			const meta = {};
			for (const f of tiles) meta[f] = val(f, i);
			const w = typeof h.size === "string" ? val(h.size, i) : 1;
			const leaf = { id: id, name: label == null ? "" : String(label), value: Number(w) > 0 && Number.isFinite(Number(w)) ? Number(w) : 0, meta: meta };
			const c = typeof color.field === "string" ? treemapTone(tones.get(String(val(color.field, i))), tokens) : null;
			if (c) leaf.itemStyle = { color: c };
			parent.children.push(leaf);
		}
		return [top];
	}

	// The colour key: ECharts drops legend names that match no series name, so it is one text graphic along the bottom.
	function treemapKey(h, tokens) {
		const entries = arr(isObj(h.color) ? h.color.legend : null).filter(isObj);
		if (!entries.length) return null;
		const clean = s => String(s == null ? "" : s).replace(/[{}|]/g, "");
		const rich = {};
		const text = entries.map((e, i) => {
			rich["k" + i] = { fill: treemapTone(e.tone, tokens) || tokens["--jc-chart-mark"] };
			return "{k" + i + "|\u25A0} " + clean(e.label != null ? e.label : e.value);
		}).join("    ");
		return { id: "juneau-treemap-key", type: "text", left: "center", bottom: 4, silent: true,
			style: { text: text, rich: rich, fill: tokens["--jc-chart-text"], fontSize: 12 } };
	}

	function treemap(h, frame, tokens) {
		const tiles = arr(h.tileFields).filter(f => typeof f === "string");
		const tip = tiles.length ? tiles.map(f => f + ": {meta." + f + "}").join("\n") : "{name}";
		const key = treemapKey(h, tokens);
		const series = { id: "tiles", type: "treemap", roam: true, nodeClick: false, breadcrumb: { show: false },
			upperLabel: { show: true }, levels: copyValue(TREEMAP_LEVELS), data: treemapTree(h, frame, tokens) };
		if (key) series.bottom = 28;
		const option = { tooltip: { trigger: "item", renderMode: "richText", formatter: labelFn(tip) }, series: [series] };
		if (key) option.graphic = [key];
		return option;
	}

	const HELPERS = table({
		line: cartesian("line"), bar: cartesian("bar"), donut: pieHelper(true), pie: pieHelper(false),
		heatmap: heatmap, scatter: scatter, treemap: treemap
	});

	// ---- E. copy, merge, $fn, render mode, large-data defaults, the pipeline --------------------------------------

	// Deep copy of JSON-ish values; functions and typed arrays are kept by reference, a Date is copied as a Date.
	function copyValue(v) {
		if (Array.isArray(v)) return v.map(copyValue);
		if (Object.prototype.toString.call(v) === "[object Date]") return new Date(v.getTime());
		if (!isObj(v)) return v;
		const out = {};
		for (const k of Object.keys(v)) if (k !== "__proto__") out[k] = copyValue(v[k]);
		return out;
	}

	// A structural copy of an option that keeps every dataset[*].source and series[*].data by reference.
	function copyOption(o) {
		const out = {};
		for (const k of Object.keys(o)) {
			if (k === "__proto__") continue;
			if (k === "dataset" || k === "series") out[k] = copyList(o[k], k === "dataset" ? "source" : "data");
			else out[k] = copyValue(o[k]);
		}
		return out;
	}

	function copyList(v, opaque) {
		const one = e => {
			if (!isObj(e)) return copyValue(e);
			const out = {};
			for (const k of Object.keys(e)) if (k !== "__proto__") out[k] = k === opaque ? e[k] : copyValue(e[k]);
			return out;
		};
		return Array.isArray(v) ? v.map(one) : one(v);
	}

	// Umbrella section 4 step 5: objects merge by key; arrays merge by element id (no ids: b replaces a).
	function mergeOption(a, b) {
		const out = Object.assign({}, a);
		for (const k of Object.keys(b)) {
			if (k === "__proto__") continue;
			const opaque = k === "dataset" ? "source" : k === "series" ? "data" : null;
			out[k] = Object.hasOwn(a, k) ? merge(a[k], b[k], opaque) : (opaque ? copyList(b[k], opaque) : copyValue(b[k]));
		}
		return out;
	}

	function merge(a, b, opaque) {
		if (Array.isArray(a) && Array.isArray(b)) return mergeList(a, b, opaque);
		if (isObj(a) && isObj(b)) {
			const out = Object.assign({}, a);
			for (const k of Object.keys(b))
				if (k !== "__proto__") out[k] = k === opaque ? b[k] : Object.hasOwn(a, k) ? merge(a[k], b[k], null) : copyValue(b[k]);
			return out;
		}
		return opaque && isObj(b) ? copyList(b, opaque) : copyValue(b);
	}

	function mergeList(a, b, opaque) {
		if (!b.some(e => isObj(e) && e.id != null)) return opaque ? copyList(b, opaque) : copyValue(b);
		const out = a.slice();
		for (const e of b) {
			const i = isObj(e) && e.id != null ? out.findIndex(x => isObj(x) && x.id === e.id) : -1;
			if (i >= 0) out[i] = merge(out[i], e, opaque);
			else out.push(opaque && isObj(e) ? copyList(e, opaque) : copyValue(e));
		}
		return out;
	}

	function isMarker(v) {
		if (!isObj(v) || typeof v.$fn !== "string") return false;
		for (const k of Object.keys(v)) if (k !== "$fn" && k !== "args") return false;
		return true;
	}

	// ECharts 6.1.0 renders every tooltip of a chart in ONE mode: the renderMode of the first tooltip component of the merged
	// model (renderMode on series.tooltip, legend.tooltip and the later entries of a tooltip array is never read).  Option
	// scopes (the option, baseOption, media[*].option, options[*]) merge into that one model, so the mode is chart-wide:
	// "html" when any scope's first tooltip says so, else "richText".  enforceRenderMode has already normalised every value
	// to "richText" or an html tooltip with a $fn formatter.  Every tooltip $fn is built in this one mode, so a $fn built
	// unescaped (richText) can never end up rendered as html.
	function chartTooltipMode(option) {
		const scopes = [option];
		if (isObj(option.baseOption)) scopes.push(option.baseOption);
		if (Array.isArray(option.media)) for (const m of option.media) if (isObj(m) && isObj(m.option)) scopes.push(m.option);
		if (Array.isArray(option.options)) for (const e of option.options) if (isObj(e)) scopes.push(e);
		for (const o of scopes) {
			const first = Array.isArray(o.tooltip) ? o.tooltip[0] : o.tooltip;
			if (isObj(first) && first.renderMode === "html") return "html";
		}
		return "richText";
	}

	function makeFn(card, marker, mode, fctx) {
		const name = marker.$fn;
		const factory = impls.fn.get(name);
		if (!factory) throw chartError("E-JS-90", card.id, name, impls.fn.size ? Array.from(impls.fn.keys()).sort().join(", ") : "none");
		let f;
		try {
			f = factory(isObj(marker.args) ? marker.args : {},
				{ renderMode: mode, frame: fctx.frame, useUTC: fctx.useUTC, resolve: (m, rawMode) => makeFn(card, m, rawMode || mode, fctx) });
		} catch (e) {
			throw chartError("E-JS-98", card.id, "fn", name, msgOf(e));
		}
		if (typeof f !== "function") throw chartError("E-JS-98", card.id, "fn", name, "the factory did not return a function");
		const wrapped = function () {
			try {
				return f.apply(this, arguments);
			} catch (e) {
				log(chartError("E-JS-98", card.id, "fn", name, msgOf(e)));
				return "";
			}
		};
		wrapped.juneauFn = name;
		return wrapped;
	}

	function refuseFns(card, v, key) {
		if (isMarker(v)) throw chartError("E-JS-98", card.id, "fn", v.$fn, "a $fn may not format " + key + " (ECharts renders its result as html)");
		if (Array.isArray(v)) for (const e of v) refuseFns(card, e, key);
		else if (isObj(v)) for (const k of Object.keys(v)) refuseFns(card, v[k], k);
	}

	// Umbrella section 4 step 6: one walk, in place on the copy, skipping dataset[*].source and series[*].data.  Each option
	// scope (the option, baseOption, media[*].option, options[*]) resolves its tooltip fns in the one chart-wide mode.
	function resolveFns(card, option, frame) {
		const fctx = { frame: frame, useUTC: option.useUTC === true };
		const tipMode = chartTooltipMode(option);
		const scope = o => {
			const html = tipMode;
			const walk = (v, mode, opaque) => {
				if (isMarker(v)) return makeFn(card, v, mode, fctx);
				if (Array.isArray(v)) {
					for (let i = 0; i < v.length; i++) v[i] = walk(v[i], mode, opaque);
					return v;
				}
				if (!isObj(v)) return v;
				for (const k of Object.keys(v)) {
					if (k === opaque) continue;
					// Decorator row data (markLine/markArea/markPoint items carry the author's row under "meta") is data, never an option:
					// a {$fn} inside it stays inert and cannot abort the draw.
					if (k === "meta") continue;
					// ECharts writes the result of toolbox.feature.dataView (optionToContent, title, lang, ...) as html, and a $fn result
					// is never escaped for it, so no $fn may appear anywhere under it.
					if (k === "optionToContent" || k === "dataView") refuseFns(card, v[k], k);
					v[k] = walk(v[k], k === "tooltip" ? html : mode, null);
				}
				return v;
			};
			for (const k of Object.keys(o)) {
				if (k === "baseOption" && isObj(o[k])) scope(o[k]);
				else if (k === "media" && Array.isArray(o[k])) o[k].forEach(m => { if (isObj(m) && isObj(m.option)) scope(m.option); });
				else if (k === "options" && Array.isArray(o[k])) o[k].forEach(e => { if (isObj(e)) scope(e); });
				else if (k === "tooltip") {
					// The tooltip component itself: every entry renders in the chart-wide mode (the first entry decides).
					if (Array.isArray(o[k])) o[k] = o[k].map(t => walk(t, tipMode, null));
					else o[k] = walk(o[k], tipMode, null);
				} else {
					const opaque = k === "dataset" ? "source" : k === "series" ? "data" : null;
					if (opaque) {
						const list = Array.isArray(o[k]) ? o[k] : [o[k]];
						for (const e of list) if (isObj(e)) walk(e, "richText", opaque);
					} else {
						o[k] = walk(o[k], "richText", null);
					}
				}
			}
		};
		scope(option);
		return option;
	}

	function hasFnFormatter(t) {
		return isObj(t) && (isMarker(t.formatter) || typeof t.formatter === "function");
	}

	// Umbrella section 7: the tooltip renders rich text unless an html tooltip has a $fn (escaping) formatter.  ECharts
	// renders html for every renderMode except the exact string "richText" (and an unset one), so the rule is an allowlist:
	// a missing renderMode becomes "richText", and anything but "richText" or "html" with a $fn formatter is downgraded.
	// Every option scope is guarded: the option, baseOption, media[*].option and options[*].
	function enforceRenderMode(card, option) {
		const list = Array.isArray(option.tooltip) ? option.tooltip : isObj(option.tooltip) ? [option.tooltip] : [];
		// No tooltip component: nothing renders html.
		for (const t of list) {
			if (!isObj(t)) continue;
			if (t.renderMode == null) t.renderMode = "richText";
			else if (t.renderMode !== "richText" && !(t.renderMode === "html" && hasFnFormatter(t))) {
				warn(fmt("chart card '%s' tooltip renderMode '%s' needs a $fn formatter; using 'richText'", card.id, t.renderMode));
				t.renderMode = "richText";
			}
		}
		if (isObj(option.baseOption)) enforceRenderMode(card, option.baseOption);
		if (Array.isArray(option.media)) for (const m of option.media) if (isObj(m) && isObj(m.option)) enforceRenderMode(card, m.option);
		if (Array.isArray(option.options)) for (const e of option.options) if (isObj(e)) enforceRenderMode(card, e);
		return option;
	}

	// C4: above the threshold, set the large-data options only where the option leaves them unset.
	function applyLarge(card, option, frame) {
		const ld = isObj(card.largeData) ? card.largeData : {};
		const threshold = typeof ld.threshold === "number" ? ld.threshold : LARGE.threshold;
		if (!(frame.rowCount > threshold)) return option;
		const progressive = ld.progressive === false ? 0 : typeof ld.progressive === "number" ? ld.progressive : LARGE.progressive;
		const unset = (o, k, v) => { if (o[k] === undefined) o[k] = v; };
		unset(option, "animation", false);
		const series = Array.isArray(option.series) ? option.series : isObj(option.series) ? [option.series] : [];
		for (const s of series) {
			if (!isObj(s)) continue;
			if (s.type === "line") unset(s, "sampling", "lttb");
			if (s.type === "bar" || s.type === "scatter") unset(s, "large", true);
			unset(s, "progressive", progressive);
			unset(s, "progressiveThreshold", LARGE.progressiveThreshold);
		}
		return option;
	}

	function withTimeout(p, card, name) {
		let timer;
		const t = new Promise((res, rej) => {
			timer = setTimeout(() => rej(new Error("timed out after " + DECORATOR_TIMEOUT_MS + " ms")), DECORATOR_TIMEOUT_MS);
		});
		return Promise.race([Promise.resolve(p), t]).then(v => { clearTimeout(timer); return v; },
			e => { clearTimeout(timer); throw chartError("E-JS-98", card.id, "decorator", name, msgOf(e)); });
	}

	// The ctx a decorator gets: the card ctx plus {card, publish, data, registry, tokens, fetch}.
	// fetch is the per-draw helper behind the per-card src cache.  It only fetches same-origin paths: every URL
	// goes through isSafeHref in perDrawFetch.json, so a decorator cannot reach another origin.
	function decoratorCtx(card, ctx, fetcher, cache) {
		const base = ctx || {}, views = window.JuneauViews;
		return Object.assign({}, base, {
			card: card,
			publish: typeof base.publish === "function" ? base.publish : () => {},
			data: views && views.data || null,
			registry: window.JuneauCharts,
			tokens: base.tokens ? Object.freeze(Object.assign({}, base.tokens)) : TOKEN_FALLBACKS,
			fetch: cachedFetch(cache || new Map(), fetcher || perDrawFetch(base.signal))
		});
	}

	// Steps 3-7.  Returns {marked, option}: marked keeps the $fn markers (getOption), option has functions.
	function pipeline(card, frame, dctx) {
		try {
			return runPipeline(card, frame, dctx);
		} catch (e) {
			return Promise.reject(e);
		}
	}

	function runPipeline(card, frame, dctx) {
		const tokens = dctx.tokens;
		const h = helperOf(card);
		let base = {};
		if (h) {
			const make = HELPERS[h.kind];
			if (!make) return Promise.reject(chartError("E-JS-60", card.id, h.kind, Object.keys(HELPERS).sort().join(", ")));
			base = make(h, frame, tokens);
		}
		if (isObj(card.option)) base = mergeOption(base, card.option);
		if (base.aria === undefined) base.aria = { enabled: true };
		let option = bind(base, frame);
		let chain = Promise.resolve(option);
		arr(card.decorators).forEach((spec, index) => {
			chain = chain.then(o => {
				const name = isObj(spec) ? spec.use : null;
				const d = impls.decorator.get(name);
				if (!d) throw chartError("E-JS-91", card.id, name, impls.decorator.size ? Array.from(impls.decorator.keys()).sort().join(", ") : "none");
				let r;
				try {
					r = d(copyOption(o), frame, spec, Object.assign({}, dctx, { index: index }));
				} catch (e) {
					throw chartError("E-JS-98", card.id, "decorator", name, msgOf(e));
				}
				return withTimeout(r, card, name).then(v => {
					if (!isObj(v)) throw chartError("E-JS-98", card.id, "decorator", name, "it returned no option");
					return v;
				});
			});
		});
		return chain.then(o => {
			if (isObj(card.patch)) o = mergeOption(o, card.patch);
			const marked = applyLarge(card, enforceRenderMode(card, o), frame);
			return { marked: marked, option: resolveFns(card, copyOption(marked), frame) };
		});
	}

	// ---- H. same-origin href (verbatim; ViewsJs_SameOriginPath_Test checks the copies) ---------------------------

	function resolveHref(tpl, row) {
		return tpl.replace(/\{([^}]+)\}/g, (m, f) => encodeURIComponent(row && row[f] != null ? String(row[f]) : ""));
	}

	/**
	 * The one prefix rule every same-origin / allowlist URL guard applies to the ORIGINAL string, before anything else
	 * (never normalise and accept): true when the first or last character is a space or control character, or any
	 * character is a backslash, a C0 control character or DEL.  A browser strips leading/trailing C0 and space,
	 * deletes tab/CR/LF anywhere and reads a backslash as a slash, so each of those can turn a value that looks like a
	 * same-origin path into "//host".  The text of this function is identical in every file that carries it
	 * (checked by ViewsJs_SameOriginPath_Test) and mirrors SafePathTemplate.isSameOriginPath.  Callers handle the empty
	 * string.
	 */
	function hasUnsafeUrlChars(s) {
		return s.charCodeAt(0) <= 32 || s.charCodeAt(s.length - 1) <= 32 || /[\\\u0000-\u001f\u007f]/.test(s);
	}

	// Exact parity with SafePathTemplate.isSameOriginPath (juneau-commons): the original string is refused outright
	// when it has a leading/trailing space or control character, a backslash, or a control character anywhere.
	function isSafeHref(u) {
		const s = String(u);
		if (!s || hasUnsafeUrlChars(s) || s.includes("://") || s.startsWith("//")) return false;
		const colon = s.indexOf(":"), slash = s.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;
		return s.split("/").indexOf("..") < 0;
	}

	// ---- F. built-in fns (bare names) ----------------------------------------------------------------------------

	const BINARY_UNITS = ["B", "KiB", "MiB", "GiB", "TiB", "PiB"], DECIMAL_UNITS = ["B", "kB", "MB", "GB", "TB", "PB"];
	const DATE_PATTERNS = table({
		date: { year: "numeric", month: "short", day: "numeric" },
		time: { hour: "2-digit", minute: "2-digit" },
		datetime: { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" },
		month: { year: "numeric", month: "short" }
	});

	// An axis formatter gets a value; a tooltip or label formatter gets params ({value, ...}).
	function plainValue(p) {
		const v = Array.isArray(p) ? p[0] : p;
		if (!isObj(v)) return v;
		const val = v.value;
		if (Array.isArray(val) && v.encode) {
			const k = v.encode.y || v.encode.value;
			if (Array.isArray(k)) return val[k[0]];
		}
		return val;
	}

	// What a fn prints for a value it cannot format: nothing for null/undefined/NaN, else the value as text.
	function plainText(v) {
		return v == null || (typeof v === "number" && v !== v) ? "" : String(v);
	}

	function numberText(nf, fctx) {
		return p => {
			const v = plainValue(p);
			return escapeFor(fctx.renderMode, typeof v === "number" && isFinite(v) ? nf.format(v) : plainText(v));
		};
	}

	function digitsOf(args, dflt) {
		return typeof args.digits === "number" ? args.digits : dflt;
	}

	// Spec section 5: every string a built-in fn produces (fallbacks included) is escaped for the render mode it is used in:
	// html entities in "html", the rich-text braces in "richText"; "raw" (a template sub-fn) leaves it to the template.
	function escapeFor(mode, s) {
		s = String(s);
		if (mode === "raw") return s;   // template's formats sub-fns: only the outermost template escapes
		if (mode === "html")
			return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
		return s.replace(/\{/g, "\uff5b").replace(/\}/g, "\uff5d");
	}

	// A numeric (typed-array) column of the frame, or null: an author field such as "constructor" must not find an Object member.
	function numericColumn(frame, name) {
		const col = frame && typeof name === "string" && Object.hasOwn(frame.columns, name) ? frame.columns[name] : null;
		return col && ArrayBuffer.isView(col) ? col : null;
	}

	function timeSpan(frame) {
		const d = frame && frame.dims.find(x => x.type === "time");
		if (!d) return null;
		const col = numericColumn(frame, d.name);
		if (!col) return null;
		let lo = Infinity, hi = -Infinity;
		for (let i = 0; i < frame.rowCount; i++) if (col[i] === col[i]) { lo = Math.min(lo, col[i]); hi = Math.max(hi, col[i]); }
		return lo <= hi ? hi - lo : null;
	}

	const DAY_MS = 86400000;

	const BUILTIN_FNS = {
		number: (args, fctx) => numberText(new Intl.NumberFormat(args.locale, { maximumFractionDigits: digitsOf(args, 2), minimumFractionDigits: typeof args.digits === "number" ? args.digits : 0 }), fctx),
		percent: (args, fctx) => {
			const nf = new Intl.NumberFormat(args.locale, { maximumFractionDigits: digitsOf(args, 1) });
			const k = args.scale === 100 ? 1 : 100;
			return p => {
				const v = plainValue(p);
				return escapeFor(fctx.renderMode, typeof v === "number" && isFinite(v) ? nf.format(v * k) + "%" : plainText(v));
			};
		},
		compact: (args, fctx) => numberText(new Intl.NumberFormat(args.locale, { notation: "compact", maximumFractionDigits: digitsOf(args, 1) }), fctx),
		// Tiers: a value moves up a unit as soon as its printed form would reach the base ("999999" is "1.0 MB", never "1000.0 kB").
		// A negative value keeps its sign and is tiered by magnitude ("-1500" is "-1.5 kB"); beyond PB the last unit is kept.
		bytes: (args, fctx) => {
			const base = args.binary === true ? 1024 : 1000, units = base === 1024 ? BINARY_UNITS : DECIMAL_UNITS;
			return p => {
				let v = plainValue(p);
				if (typeof v !== "number" || !isFinite(v)) return escapeFor(fctx.renderMode, plainText(v));
				let u = 0;
				const printed = () => (u === 0 ? Math.round(v) : Number(v.toFixed(1)));
				while (u < units.length - 1 && Math.abs(printed()) >= base) { v /= base; u++; }
				return escapeFor(fctx.renderMode, (u === 0 ? String(printed()) : v.toFixed(1)) + " " + units[u]);
			};
		},
		// Tiers: under 1 s "ms", then "s" with one decimal, "m", "h", "d".  59950 ms would print "60.0 s", so it rolls to "1m 00s".
		// A negative duration keeps its sign and is tiered by magnitude ("-5000" is "-5.0 s").
		duration: (args, fctx) => {
			const k = args.unit === "s" ? 1000 : 1;
			const two = n => (n < 10 ? "0" : "") + n;
			return p => {
				const v = plainValue(p);
				if (typeof v !== "number" || !isFinite(v)) return escapeFor(fctx.renderMode, plainText(v));
				const total = Math.round(v * k), sign = total < 0 ? "-" : "", ms = Math.abs(total);
				let s = Math.floor(ms / 1000), out;
				if (ms < 1000) out = ms + " ms";
				else {
					if (s < 60 && (ms / 1000).toFixed(1) === "60.0") s = 60;
					if (s < 60) out = (ms / 1000).toFixed(1) + " s";
					else if (s < 3600) out = Math.floor(s / 60) + "m " + two(s % 60) + "s";
					else if (s < 86400) out = Math.floor(s / 3600) + "h " + two(Math.floor(s % 3600 / 60)) + "m";
					else out = Math.floor(s / 86400) + "d " + two(Math.floor(s % 86400 / 3600)) + "h";
				}
				return escapeFor(fctx.renderMode, sign + out);
			};
		},
		datetime: (args, fctx) => {
			let pattern = args.pattern || "auto";
			if (pattern === "auto") {
				const span = timeSpan(fctx.frame);
				pattern = span == null ? "datetime" : span <= DAY_MS ? "time" : span <= 90 * DAY_MS ? "date" : "month";
			}
			const utc = typeof args.utc === "boolean" ? args.utc : fctx.useUTC;
			const df = new Intl.DateTimeFormat(args.locale, Object.assign({}, DATE_PATTERNS[pattern] || DATE_PATTERNS.datetime, utc ? { timeZone: "UTC" } : {}));
			return p => {
				const v = plainValue(p);
				// new Date(null) is the epoch, so null and NaN are "no value", never 1970.
				if (v == null || v !== v) return "";
				const d = new Date(typeof v === "string" ? Date.parse(v) : v);
				return escapeFor(fctx.renderMode, isNaN(d.getTime()) ? plainText(v) : df.format(d));
			};
		},
		template: (args, fctx) => {
			const text = typeof args.text === "string" ? args.text : "";
			const formats = Object.create(null);   // null-prototype: a token such as "constructor" must miss
			if (isObj(args.formats))
				for (const k of Object.keys(args.formats)) if (isMarker(args.formats[k])) formats[k] = fctx.resolve(args.formats[k], "raw");
			return p => {
				const q = Array.isArray(p) ? p[0] : p;
				const params = isObj(q) ? q : { value: q, name: q };
				return text.replace(/\{([A-Za-z0-9_.-]+)\}/g, (m, tok) => {
					let v;
					if (tok === "value") v = plainValue(params);
					else if (tok === "name" || tok === "seriesName") v = params[tok];
					else if (tok.indexOf("meta.") === 0) v = isObj(params.data) && isObj(params.data.meta) && Object.hasOwn(params.data.meta, tok.slice(5)) ? params.data.meta[tok.slice(5)] : undefined;
					else {
						const d = fctx.frame && fctx.frame.dims.find(x => x.name === tok);
						v = d && typeof params.dataIndex === "number" ? cell(fctx.frame, d, params.dataIndex) : undefined;
					}
					if (formats[tok]) v = formats[tok](v);
					return v == null ? "" : escapeFor(fctx.renderMode, v);
				});
			};
		},
		symbolSizeBy: (args, fctx) => {
			const min = typeof args.min === "number" ? args.min : 4, max = typeof args.max === "number" ? args.max : 24;
			const col = numericColumn(fctx.frame, args.field);
			let lo = Infinity, hi = -Infinity;
			if (col) for (let i = 0; i < fctx.frame.rowCount; i++) if (col[i] === col[i]) { lo = Math.min(lo, col[i]); hi = Math.max(hi, col[i]); }
			return (value, params) => {
				const i = params && typeof params.dataIndex === "number" ? params.dataIndex : -1;
				const v = col && i >= 0 ? col[i] : NaN;
				if (!(v === v) || !(hi > lo)) return (min + max) / 2;
				return min + Math.sqrt((v - lo) / (hi - lo)) * (max - min);
			};
		}
	};

	for (const name of Object.keys(BUILTIN_FNS)) register("fn", name, BUILTIN_FNS[name], true);

	// ---- G. the per-draw fetch helper and the built-in decorators --------------------------------------------------

	function isAbortError(e) {
		return !!e && e.name === "AbortError";
	}

	// One AbortController per draw, aborted only when the draw is superseded or the card is destroyed.
	// Unlike ctx.fetchJson, a call never aborts an earlier call, so decorators and the card src load side by side.
	function perDrawFetch(signal) {
		const controller = new AbortController();
		const onParentAbort = () => controller.abort();
		if (signal) {
			if (signal.aborted) controller.abort();
			else signal.addEventListener("abort", onParentAbort, { once: true });
		}
		return {
			// True once this draw's fetches are aborted (draw superseded or card destroyed): a cache entry made by it is dead.
			get aborted() { return controller.signal.aborted; },
			abort: () => {
				if (signal) signal.removeEventListener("abort", onParentAbort);
				controller.abort();
			},
			json: url => {
				if (typeof url !== "string") return Promise.reject(new Error("the url is not a string"));
				if (!isSafeHref(url)) return Promise.reject(new Error("'" + url + "' is not a same-origin path"));
				return fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" }, signal: controller.signal }).then(r => {
					if (r.ok) return r.json();
					return r.json().then(b => { throw new Error(b && b.message || "HTTP " + r.status); },
						e => { if (isAbortError(e)) throw e; throw new Error("HTTP " + r.status); });
				}, e => {
					if (isAbortError(e)) throw e;
					throw new Error("Request failed");
				});
			}
		};
	}

	// A src fetched by a decorator is cached (per card) until the card refreshes; a failed fetch is not cached.
	// An entry remembers the fetcher that made it: once that draw is aborted its promise is an AbortError, so it counts as a miss
	// (the next draw fetches again instead of inheriting the dead promise).
	function cachedFetch(cache, fetcher) {
		return url => {
			let e = cache.get(url);
			if (!e || e.fetcher.aborted === true) {
				const p = fetcher.json(url);
				e = { fetcher: fetcher, promise: p };
				cache.set(url, e);
				p.catch(() => { if (cache.get(url) === e) cache.delete(url); });
			}
			return e.promise;
		};
	}

	function framesRows(f) {
		const out = [];
		for (let i = 0; i < f.rowCount; i++) out.push(rowAt(f, i));
		return out;
	}

	// A decorator's rows: values, a same-origin src, or the current frame of a data: topic.
	function valuesOf(spec, ctx) {
		if (Array.isArray(spec.values)) return Promise.resolve(spec.values.filter(isObj));
		if (typeof spec.src === "string")
			return ctx.fetch(spec.src).then(d => {
				if (Array.isArray(d)) return d.filter(isObj);
				if (d && Array.isArray(d.rows)) return d.rows.filter(isObj);
				throw new Error("the response is not an array of rows");
			});
		if (typeof spec.source === "string") {
			const views = window.JuneauViews, h = views && views.bus ? views.bus.get(spec.source) : undefined;
			const f = h && ctx.data && ctx.data.isHandle(h) ? ctx.data.get(h) : null;
			return Promise.resolve(f ? framesRows(f) : []);
		}
		return Promise.resolve([]);
	}

	function seriesList(option) {
		return Array.isArray(option.series) ? option.series : isObj(option.series) ? [option.series] : [];
	}

	// A decorator value that comes from DATA (a fetched or topic row) is a scalar: a string, a number or a Date.  Anything else
	// (an object such as {"$fn": ...}, an array, a boolean, null) is undefined, so a data row can neither smuggle a function
	// marker into the option nor abort the draw with E-JS-90.  (Date is matched by tag: a row may come from another realm.)
	function scalarOf(v) {
		return typeof v === "string" || typeof v === "number" || Object.prototype.toString.call(v) === "[object Date]" ? v : undefined;
	}

	// An author field name picks a row cell only when the row owns it ("constructor" must miss), and only a scalar counts.
	function cellOf(r, k) {
		return Object.hasOwn(r, k) ? scalarOf(r[k]) : undefined;
	}

	function addSeries(option, s) {
		option.series = seriesList(option).concat([s]);
		return option;
	}

	function labelFn(text) {
		return { $fn: "template", args: { text: text } };
	}

	function firstAxisType(option, key) {
		const a = Array.isArray(option[key]) ? option[key][0] : option[key];
		return isObj(a) ? a.type : null;
	}

	// C8: "now" on a category axis is today's date (UTC when the option's useUTC is true, else local).
	function nowAt(option) {
		const t = Date.now();
		if (firstAxisType(option, "xAxis") !== "category") return t;
		const d = new Date(t), utc = option.useUTC === true;
		const two = n => (n < 10 ? "0" : "") + n;
		return utc ? d.getUTCFullYear() + "-" + two(d.getUTCMonth() + 1) + "-" + two(d.getUTCDate())
			: d.getFullYear() + "-" + two(d.getMonth() + 1) + "-" + two(d.getDate());
	}

	function markers(option, frame, spec, ctx) {
		const at = spec.at || "at", label = spec.label || "label";
		return valuesOf(spec, ctx).then(rows => {
			const data = rows.map(r => ({ xAxis: cellOf(r, at), name: cellOf(r, label) == null ? "" : String(cellOf(r, label)), meta: isObj(r.meta) ? r.meta : r }));
			if (isObj(spec.now)) data.push({ xAxis: nowAt(option), name: spec.now.label == null ? "" : String(spec.now.label), meta: null });
			const silent = !spec.topic;
			return addSeries(option, { id: "juneau-markers-" + ctx.index, type: "line", data: [], silent: silent,
				markLine: { symbol: "none", silent: silent, lineStyle: { type: spec.style || "dashed", color: ctx.tokens["--jc-chart-mark"] },
					label: { position: "end", formatter: labelFn("{name}") }, data: data } });
		});
	}

	function firstSeriesFields(option) {
		const s = seriesList(option).find(x => isObj(x) && !(typeof x.id === "string" && x.id.indexOf("juneau-") === 0) && isObj(x.encode));
		return s ? { x: s.encode.x, y: s.encode.y } : null;
	}

	// No-data bands: runs of buckets where the first series is null or 0, widened by half a bucket.
	function gapBands(option, frame, spec) {
		const f = firstSeriesFields(option);
		const xd = f && frame.dims.find(d => d.name === f.x), ycol = f && numericColumn(frame, f.y);
		if (!xd || !ycol || frame.rowCount < 2) return [];
		const numeric = xd.type !== "string", xs = numericColumn(frame, xd.name);
		if (numeric && !xs) return [];
		const xAt = i => numeric ? xs[i] : cell(frame, xd, i);
		let step = 0;
		if (numeric) {
			const diffs = [];
			for (let i = 1; i < frame.rowCount; i++) diffs.push(xs[i] - xs[i - 1]);
			diffs.sort((a, b) => a - b);
			step = diffs[Math.floor(diffs.length / 2)] || 0;
		}
		const minGap = typeof spec.minGap === "number" ? spec.minGap : 1;
		const out = [];
		let start = -1;
		for (let i = 0; i <= frame.rowCount; i++) {
			const empty = i < frame.rowCount && (!(ycol[i] === ycol[i]) || ycol[i] === 0);
			if (empty && start < 0) start = i;
			if (!empty && start >= 0) {
				if (i - start >= minGap)
					out.push(numeric ? { start: xAt(start) - step / 2, end: xAt(i - 1) + step / 2 } : { start: xAt(start), end: xAt(i - 1) });
				start = -1;
			}
		}
		return out;
	}

	function bands(option, frame, spec, ctx) {
		const s = spec.start || "start", e = spec.end || "end";
		const rows = spec.auto === "gaps" ? Promise.resolve(gapBands(option, frame, spec)) : valuesOf(spec, ctx);
		return rows.then(list => {
			const text = spec.label == null ? "" : String(spec.label);
			const data = list.map(r => {
				const a = spec.auto === "gaps" ? r.start : cellOf(r, s), b = spec.auto === "gaps" ? r.end : cellOf(r, e);   // gap rows are computed (string or number)
				const lab = cellOf(r, "label");
				const name = lab != null ? String(lab) : text;
				return [{ xAxis: a, name: name, meta: spec.auto === "gaps" ? null : r }, { xAxis: b }];
			});
			const silent = !spec.topic;
			return addSeries(option, { id: "juneau-bands-" + ctx.index, type: "line", data: [], silent: silent,
				markArea: { silent: silent, itemStyle: { color: ctx.tokens["--jc-chart-band"] },
					label: { formatter: labelFn("{name}") }, data: data } });
		});
	}

	function thresholds(option, frame, spec, ctx) {
		const data = arr(spec.values).filter(isObj).map(v => {
			const out = { yAxis: v.y, name: v.label == null ? "" : String(v.label) };
			const color = toneColor(v.tone, ctx.tokens);
			if (color) out.lineStyle = { color: color };
			return out;
		});
		return addSeries(option, { id: "juneau-thresholds-" + ctx.index, type: "line", data: [], silent: true,
			markLine: { symbol: "none", silent: true, lineStyle: { type: "solid", color: ctx.tokens["--jc-chart-mark"] },
				label: { position: "end", formatter: labelFn("{name}") }, data: data } });
	}

	function annotations(option, frame, spec, ctx) {
		const label = typeof spec.label === "string" ? { formatter: labelFn(spec.label) } : undefined;
		const presets = arr(spec.presets).filter(p => p === "max" || p === "min" || p === "average");
		if (presets.length) {
			for (const s of seriesList(option)) {
				if (!isObj(s) || (typeof s.id === "string" && s.id.indexOf("juneau-") === 0)) continue;
				if (s.type !== "line" && s.type !== "bar" && s.type !== "scatter") continue;
				const mp = isObj(s.markPoint) ? s.markPoint : {};
				mp.data = arr(mp.data).concat(presets.map(p => ({ type: p, name: p })));
				if (label) mp.label = label;
				s.markPoint = mp;
			}
		}
		const values = arr(spec.values).filter(isObj);
		if (!values.length) return option;
		return addSeries(option, { id: "juneau-annotations-" + ctx.index, type: "line", data: [], silent: true,
			markPoint: { label: { formatter: labelFn(typeof spec.label === "string" ? spec.label : "{name}") },
				data: values.map(v => ({ coord: [v.x, v.y], name: v.label == null ? "" : String(v.label) })) } });
	}

	register("decorator", "markers", markers, true);
	register("decorator", "bands", bands, true);
	register("decorator", "thresholds", thresholds, true);
	register("decorator", "annotations", annotations, true);

	// ---- I. the theme bridge -------------------------------------------------------------------------------

	// {name, tokens, registered}: read once per page, and again after juneau:theme-changed.
	let theme = null;

	// juneau-theme-<name>.css on the first matching stylesheet link names the ECharts theme juneau-<name>.  The dark scheme
	// layer (juneau-theme-dark.css) is linked ahead of the palette and is not a palette, so it is skipped.
	function themeName(doc) {
		const links = doc && doc.querySelectorAll ? Array.from(doc.querySelectorAll("link")) : [];
		for (const l of links) {
			if (String(l.getAttribute("rel") || "").toLowerCase() !== "stylesheet") continue;
			const m = /(?:^|\/)juneau-theme-([a-z0-9-]+)\.css(?:[?#]|$)/.exec(String(l.getAttribute("href") || ""));
			if (m && m[1] !== "dark") return "juneau-" + m[1];
		}
		return "juneau-open";
	}

	// The computed style of the root element, or null when the page cannot give one.
	function rootStyle(doc) {
		const view = doc && doc.defaultView || window;
		try {
			return view && typeof view.getComputedStyle === "function" ? view.getComputedStyle(doc.documentElement) : null;
		} catch (e) {
			return null;
		}
	}

	function cssValue(cs, name) {
		return cs ? String(cs.getPropertyValue(name) || "").trim() : "";
	}

	function readTokens(doc) {
		const out = Object.assign({}, TOKEN_FALLBACKS);
		const cs = rootStyle(doc);
		if (cs) {
			for (const k of Object.keys(out)) {
				const v = cssValue(cs, k);
				if (v) out[k] = v;
			}
			for (const k of Object.keys(TEXT_ROLE_FILLS))
				if (!cssValue(cs, k)) out[k] = out[TEXT_ROLE_FILLS[k]];
		}
		return out;
	}

	function themeObject(t) {
		const palette = [];
		for (let i = 1; i <= 8; i++) palette.push(t["--jc-chart-" + i]);
		const axis = {
			axisLine: { lineStyle: { color: t["--jc-chart-axis"] } },
			axisTick: { lineStyle: { color: t["--jc-chart-axis"] } },
			axisLabel: { color: t["--jc-chart-text"] },
			splitLine: { lineStyle: { color: t["--jc-chart-grid"] } }
		};
		// The markLine/markArea defaults the decorators also set explicitly.
		const marks = {
			markLine: { lineStyle: { color: t["--jc-chart-mark"] }, label: { color: t["--jc-chart-text"] } },
			markArea: { itemStyle: { color: t["--jc-chart-band"] }, label: { color: t["--jc-chart-text"] } }
		};
		return {
			color: palette,
			backgroundColor: t["--jc-chart-bg"],
			textStyle: { color: t["--jc-chart-text"] },
			title: { textStyle: { color: t["--jc-chart-text"] } },
			legend: { textStyle: { color: t["--jc-chart-text"] } },
			categoryAxis: axis, valueAxis: axis, timeAxis: axis, logAxis: axis,
			tooltip: { backgroundColor: t["--jc-chart-tooltip-bg"], borderColor: t["--jc-chart-axis"], textStyle: { color: t["--jc-chart-tooltip-text"] } },
			visualMap: { inRange: { color: [t["--jc-chart-heat-low"], t["--jc-chart-heat-high"]] } },
			// Left out, these keep ECharts' built-in light colours on a dark page.
			toolbox: { iconStyle: { borderColor: t["--jc-chart-text"] } },
			dataZoom: { textStyle: { color: t["--jc-chart-text"] }, borderColor: t["--jc-chart-axis"], fillerColor: t["--jc-chart-band"] },
			brush: { brushStyle: { color: t["--jc-chart-band"], borderColor: t["--jc-chart-axis"] } },
			line: marks, bar: marks, scatter: marks
		};
	}

	function currentTheme(doc) {
		if (!theme) theme = { name: themeName(doc), tokens: readTokens(doc), registered: false };
		return theme;
	}

	// Before the first init on the page (and after a theme change): registerTheme with the resolved tokens.
	function registerTheme(ec, doc) {
		const t = currentTheme(doc);
		if (!t.registered) {
			ec.registerTheme(t.name, themeObject(t.tokens));
			t.registered = true;
		}
		return t;
	}

	// ECharts 6.1.0 bundles the EN and ZH locales.
	function localeOf(doc) {
		const root = doc && doc.documentElement;
		const lang = root && typeof root.getAttribute === "function" ? String(root.getAttribute("lang") || "") : "";
		return /^zh(-|$)/i.test(lang) ? "ZH" : "EN";
	}

	// ---- J. the card lifecycle ---------------------------------------------------------------------

	const MAX_REFRESH_MS = 2147483647;
	const states = new WeakMap();
	const live = new Set();
	const mapLoads = new Map();
	const KIND_NAMES = { line: "Line chart", bar: "Bar chart", donut: "Donut chart", pie: "Pie chart", heatmap: "Heatmap", scatter: "Scatter chart", treemap: "Treemap" };
	const OPS = ["highlight", "downplay", "showTip", "hideTip", "zoomTo", "legendSelect", "export"];
	const META_MAX = 4096, ROW_FIELDS_MAX = 64, ROW_STRING_MAX = 1024;

	function htmlNode(doc, tag, cls) {
		const n = doc.createElement(tag);
		if (cls) n.className = cls;
		return n;
	}

	function clear(n) {
		while (n.firstChild) n.removeChild(n.firstChild);
	}

	function count(n) {
		return String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");
	}

	function report(st, e) {
		log(e);
		st.ctx.paintError(st.body, e);
		e.cardReported = true;
		return e;
	}

	// Only a RETAINED published topic is a framework state topic (the Java ChartCardType.implicitTopics and bus-wiring.cjs rule).
	function publishesRetained(card, topic) {
		return arr(card.publishes).some(p => isObj(p) && p.topic === topic && p.retain === true);
	}

	// A draw-phase failure that carries no code of its own (the load is E-JS-62 and setOption E-JS-80, both coded at their source).
	function drawError(card, e) {
		return e && e.code ? e : chartError("E-JS-98", card.id, "draw", "pipeline", msgOf(e));
	}

	function isCarrier(s) {
		return isObj(s) && typeof s.id === "string" && s.id.indexOf("juneau-") === 0;
	}

	function dataSeries(option) {
		const list = Array.isArray(option.series) ? option.series : isObj(option.series) ? [option.series] : [];
		const out = [];
		list.forEach((s, i) => { if (isObj(s) && !isCarrier(s)) out.push({ s: s, index: i }); });
		return out;
	}

	function heightOf(card) {
		return Math.max(MIN_HEIGHT, Number(card.height) || DEFAULT_HEIGHT);
	}

	function a11yOf(card) {
		return isObj(card.a11y) ? card.a11y : {};
	}

	// ---- J1. chrome: caption, banner, plot, overlay, the "Show data" table ----

	function chrome(st) {
		const doc = st.ctx.document, card = st.card;
		disposeInst(st);
		clear(st.body);
		const fig = htmlNode(doc, "figure", "jc-chart");
		const cap = htmlNode(doc, "figcaption", "jc-chart-caption");
		cap.id = "jc-chart-caption-" + idrefOf(card.id);
		const banner = htmlNode(doc, "p", "jc-chart-banner");
		banner.setAttribute("role", "status");
		banner.hidden = true;
		const plot = htmlNode(doc, "div", "jc-chart-plot");
		plot.setAttribute("tabindex", "0");
		plot.setAttribute("aria-labelledby", cap.id);
		// A plot that runs something on Enter is an operable surface, not a picture: it is a group that says how to use it,
		// and the keyboard walk announces the focused point through the live region below.
		const interactive = isOperable(card);
		let live = null;
		if (interactive) {
			plot.setAttribute("role", "group");
			const hint = htmlNode(doc, "span", "jc-chart-hint");
			hint.id = "jc-chart-hint-" + idrefOf(card.id);
			hint.hidden = true;
			const h = helperOf(card);
			hint.textContent = "Arrow keys walk the points, Enter activates the focused point, Escape ends the walk."
				+ (h && h.kind === "treemap" ? " Plus and minus zoom, 0 resets." : "");
			plot.setAttribute("aria-describedby", hint.id);
			plot.appendChild(hint);
			live = htmlNode(doc, "p", "jc-chart-live");
			live.setAttribute("role", "status");
			live.setAttribute("aria-live", "polite");
		} else plot.setAttribute("role", "img");
		if (st.actionBusy) plot.setAttribute("aria-busy", "true");
		const canvas = htmlNode(doc, "div", "jc-chart-canvas");
		canvas.style.height = heightOf(card) + "px";
		const overlay = htmlNode(doc, "div", "jc-chart-overlay");
		overlay.setAttribute("role", "status");
		overlay.hidden = true;
		plot.appendChild(canvas);
		plot.appendChild(overlay);
		fig.appendChild(cap);
		fig.appendChild(banner);
		fig.appendChild(plot);
		if (live) fig.appendChild(live);
		st.toggle = st.box = st.note = st.table = null;
		st.tableOpen = false;
		if (a11yOf(card).table !== false) {
			const toggle = htmlNode(doc, "button", "jc-chart-toggle");
			toggle.setAttribute("type", "button");
			toggle.setAttribute("aria-expanded", "false");
			toggle.textContent = "Show data";
			const box = htmlNode(doc, "div", "jc-chart-table-box");
			box.id = "jc-chart-table-box-" + idrefOf(card.id);
			toggle.setAttribute("aria-controls", box.id);
			box.hidden = true;
			const note = htmlNode(doc, "p", "jc-chart-table-note");
			note.hidden = true;
			const table = htmlNode(doc, "table", "jc-chart-table");
			table.setAttribute("aria-labelledby", cap.id);
			box.appendChild(note);
			box.appendChild(table);
			// A fixed label; aria-expanded carries the state.  The table is built on open, not on every draw.
			toggle.addEventListener("click", () => {
				const open = toggle.getAttribute("aria-expanded") !== "true";
				toggle.setAttribute("aria-expanded", String(open));
				box.hidden = !open;
				st.tableOpen = open;
				if (open) buildTable(st);
			});
			fig.appendChild(toggle);
			fig.appendChild(box);
			Object.assign(st, { toggle: toggle, box: box, note: note, table: table });
		}
		plot.addEventListener("keydown", ev => onKey(st, ev));
		plot.addEventListener("blur", () => clearWalk(st));
		st.body.appendChild(fig);
		Object.assign(st, { fig: fig, caption: cap, banner: banner, plot: plot, canvas: canvas, overlay: overlay, keyIndex: -1, live: live });
	}

	function ensureChrome(st) {
		if (!st.fig || st.fig.parentNode !== st.body) chrome(st);
	}

	function empty(st) {
		disposeInst(st);
		clear(st.body);
		st.fig = null;
		const p = htmlNode(st.ctx.document, "p", "jc-chart-empty");
		p.setAttribute("role", "status");
		p.textContent = typeof st.card.emptyText === "string" ? st.card.emptyText : "No data.";
		st.body.appendChild(p);
	}

	function xDimOf(card, frame) {
		const h = helperOf(card);
		const f = h && (h.kind === "donut" || h.kind === "pie" ? h.label : fieldOf(h.x));
		return frame.dims.find(d => d.name === f) || null;
	}

	// An out-of-range or non-finite time is shown as its own text, never a RangeError that fails the whole draw.
	function isoOf(v) {
		const t = new Date(v);
		return isFinite(t.getTime()) ? t.toISOString() : String(v);
	}

	function cellText(frame, d, i) {
		const v = cell(frame, d, i);
		if (v == null) return "";
		return d.type === "time" ? isoOf(v) : String(v);
	}

	// The default caption: "Line chart, 3 series, 120 points, 2026-09-11 to 2026-10-11", unless a11y.summary is set.
	function summary(card, frame, option) {
		const a = a11yOf(card);
		if (typeof a.summary === "string") return a.summary;
		const h = helperOf(card);
		const parts = [h && KIND_NAMES[h.kind] || "Chart"];
		const n = dataSeries(option).length;
		if (!h || (h.kind !== "donut" && h.kind !== "pie" && h.kind !== "treemap")) parts.push(n + " series");
		parts.push(count(frame.rowCount) + (frame.rowCount === 1 ? " point" : " points"));
		let text = parts.join(", ");
		const x = xDimOf(card, frame);
		if (x && frame.rowCount > 1) {
			const iso = v => { const t = new Date(v); return isFinite(t.getTime()) ? t.toISOString().slice(0, 10) : String(v); };
			if (x.type === "time" || x.type === "number") {
				const col = frame.columns[x.name];
				let lo = Infinity, hi = -Infinity;
				for (let i = 0; i < frame.rowCount; i++) if (isFinite(col[i])) { lo = Math.min(lo, col[i]); hi = Math.max(hi, col[i]); }
				if (hi >= lo) text += ", " + (x.type === "time" ? iso(lo) + " to " + iso(hi) : lo + " to " + hi);
			} else if (h && h.kind !== "donut" && h.kind !== "pie") {
				text += ", " + cellText(frame, x, 0) + " to " + cellText(frame, x, frame.rowCount - 1);
			}
		}
		return text;
	}

	// The data table is lazy: a draw only records its frame; the DOM is built when the table is open.
	function fillTable(st, frame) {
		st.tableFrame = frame;
		if (st.table && st.tableOpen) buildTable(st);
	}

	function buildTable(st) {
		const frame = st.tableFrame;
		if (!st.table || !frame) return;
		const doc = st.ctx.document, a = a11yOf(st.card);
		const max = typeof a.tableMax === "number" && a.tableMax > 0 ? Math.floor(a.tableMax) : TABLE_MAX;
		const rows = Math.min(frame.rowCount, max);
		clear(st.table);
		const thead = htmlNode(doc, "thead"), hr = htmlNode(doc, "tr");
		for (const d of frame.dims) {
			const th = htmlNode(doc, "th");
			th.setAttribute("scope", "col");
			th.textContent = d.name;
			hr.appendChild(th);
		}
		thead.appendChild(hr);
		const tbody = htmlNode(doc, "tbody");
		for (let i = 0; i < rows; i++) {
			const tr = htmlNode(doc, "tr");
			for (const d of frame.dims) {
				const td = htmlNode(doc, "td");
				td.textContent = cellText(frame, d, i);
				tr.appendChild(td);
			}
			tbody.appendChild(tr);
		}
		st.table.appendChild(thead);
		st.table.appendChild(tbody);
		st.note.hidden = frame.rowCount <= max;
		st.note.textContent = frame.rowCount <= max ? "" : "Showing " + count(max) + " of " + count(frame.rowCount) + " rows";
	}

	function setBanner(st, frame) {
		st.banner.hidden = !frame.truncated;
		st.banner.textContent = frame.truncated ? "Showing the first " + count(frame.rowCount) + " rows; narrow the range" : "";
	}

	function setOverlay(st, handle) {
		if (!st.overlay) return;
		if (!handle || handle.status !== "loading") {
			st.overlay.hidden = true;
			st.overlay.textContent = "";
			return;
		}
		const p = isObj(handle.progress) ? handle.progress : {};
		st.overlay.hidden = false;
		st.overlay.textContent = typeof p.total === "number" && p.total > 0 && typeof p.loaded === "number"
			? "Loading " + Math.min(100, Math.floor(100 * p.loaded / p.total)) + "%"
			: "Loading " + count(typeof p.loaded === "number" ? p.loaded : 0) + " rows";
	}

	function setStale(st, e) {
		if (e) {
			st.body.classList.add("jc-chart--stale");
			st.body.setAttribute("title", "Showing the last data; the refresh failed: " + msgOf(e));
		} else {
			st.body.classList.remove("jc-chart--stale");
			st.body.removeAttribute("title");
		}
	}

	// ---- J2. drawing ----

	// A new draw: the previous draw's fetches are aborted, and later results of older draws are ignored.
	function beginDraw(st) {
		if (st.fetcher) st.fetcher.abort();
		st.fetcher = perDrawFetch(st.ctx.signal);
		st.pending = true;
		return ++st.seq;
	}

	// The draw `seq` ended (settled, failed or was superseded by nothing newer).
	function endDraw(st, seq) {
		if (seq === st.seq) drawEnded(st);
	}

	// Nothing is in flight any more, however the draw ended: a re-theme that arrived during it runs now.
	function drawEnded(st) {
		st.pending = false;
		if (st.rethemePending) retheme(st);
	}

	// A picture the user is looking at: a chart that has completed a draw and whose chrome is still in the body.
	function hasPicture(st) {
		return !!(st.inst && st.frame && st.fig && st.fig.parentNode === st.body);
	}

	function loadRows(st) {
		const card = st.card;
		if (Array.isArray(card.data)) return Promise.resolve(card.data.slice());
		// A src that never settles would stall the card's serialized refreshes: abort it after the same bound a decorator gets.
		const fetcher = st.fetcher;
		let timer;
		const hung = new Promise((res, rej) => {
			timer = setTimeout(() => {
				fetcher.abort();
				rej(new Error("timed out after " + DECORATOR_TIMEOUT_MS + " ms"));
			}, DECORATOR_TIMEOUT_MS);
		});
		const fetched = fetcher.json(card.src);
		fetched.catch(() => {});
		return Promise.race([fetched, hung]).then(d => { clearTimeout(timer); return d; }, e => { clearTimeout(timer); throw e; }).then(d => {
			if (Array.isArray(d)) return d;
			if (d && Array.isArray(d.rows)) return d.rows;
			throw new Error("the response is not an array of rows");
		});
	}

	// Each map once per page, through the non-aborting fetch helper.
	function loadMaps(st, ec) {
		const maps = isObj(st.card.maps) ? st.card.maps : {};
		return Promise.all(Object.keys(maps).map(name => {
			let p = mapLoads.get(name);
			if (!p) {
				p = perDrawFetch(null).json(maps[name])
					.catch(e => { throw e && e.code ? e : chartError("E-JS-62", st.card.id, msgOf(e)); })
					.then(geo => { ec.registerMap(name, geo); });
				mapLoads.set(name, p);
				p.catch(() => { if (mapLoads.get(name) === p) mapLoads.delete(name); });
			}
			return p;
		}));
	}

	function foldState(option, st) {
		const z = st.zoom;
		if (z) {
			const list = Array.isArray(option.dataZoom) ? option.dataZoom : isObj(option.dataZoom) ? [option.dataZoom] : [];
			for (const d of list)
				if (isObj(d)) for (const k of ["start", "end", "startValue", "endValue"]) if (z[k] != null) d[k] = z[k];
		}
		if (st.legend && isObj(st.legend.selected)) {
			const l = Array.isArray(option.legend) ? option.legend[0] : option.legend;
			if (isObj(l)) l.selected = Object.assign({}, st.legend.selected);
		}
		return option;
	}

	// True once a draw is no longer the card's current one: superseded by a later draw, the card destroyed, or its signal aborted.
	// Every async continuation checks this, never the AbortError name (a decorator's aborted fetch surfaces as E-JS-98).
	function stale(st, seq) {
		return seq !== st.seq || st.destroyed || !!(st.ctx.signal && st.ctx.signal.aborted);
	}

	// rethemed: this draw re-applies the page theme to a drawn chart (see retheme).
	function drawFrame(st, frame, seq, rethemed) {
		const card = st.card, ec = window.echarts;
		if (frame.rowCount === 0) {
			st.frame = frame;
			empty(st);
			return Promise.resolve();
		}
		// This draw's own fetcher: a later beginDraw replaces st.fetcher, and the cache entries made here die with this one.
		const fetcher = st.fetcher;
		// The ctx is built INSIDE the promise chain: a throwing getter is a rejection of this draw, never a synchronous throw.
		let t = null;
		return Promise.resolve().then(() => {
			t = currentTheme(st.ctx.document);
			const dctx = decoratorCtx(card, Object.assign({}, st.ctx, { tokens: t.tokens }), fetcher, st.cache);
			return Promise.all([pipeline(card, frame, dctx), loadMaps(st, ec)]);
		}).then(r => {
			if (stale(st, seq)) return;
			paint(st, frame, r[0], rethemed ? t : null);
		});
	}

	// themed: on a re-theme draw, the theme the option's colours were baked from; the instance takes it in the same task as the
	// option, so nothing is painted in between.
	function paint(st, frame, r, themed) {
		const card = st.card;
		ensureChrome(st);
		setBanner(st, frame);
		const first = !st.inst;
		if (first) initChart(st);
		const inPlace = themed && !first;
		// setTheme rebuilds the stored option, dropping a zoom range or legend selection the card does not publish: the view on
		// screen is read back first and folded into the new option.
		const option = foldState(r.option, inPlace ? viewState(st) : st);
		try {
			if (inPlace) {
				// setTheme rebuilds the stored option under the new theme; the components whose colours the option itself
				// carries (tones, marks, the heatmap range, graphics) are then replaced rather than merged.
				st.inst.setTheme(themeObject(themed.tokens));
				st.inst.setOption(option, { replaceMerge: ["series", "dataset", "graphic", "visualMap"] });
			} else {
				// The first draw replaces everything; replaceMerge is only for redraws.
				st.inst.setOption(option, first ? { notMerge: true } : { replaceMerge: ["series", "dataset"] });
			}
		} catch (e) {
			throw chartError("E-JS-80", card.id, msgOf(e));
		}
		// The theme is applied once the option baked from the page's current tokens is on screen; an older token read leaves it owed.
		if (themed && themed === theme) st.rethemePending = false;
		st.frame = frame;
		st.marked = foldState(r.marked, st);
		st.caption.textContent = summary(card, frame, r.marked);
		fillTable(st, frame);
		// A treemap's keyIndex is a leaf position and leaf order changes when a tile moves group, so a redraw ends the walk.
		if (st.keyIndex >= frame.rowCount || treemapEntry(st)) { st.keyIndex = -1; announce(st, ""); }
		// A redraw replaces the treemap series, so a zoom from the keyboard starts over.
		st.treemapRoot = TREEMAP_ROOT;
		if (first) for (const topic of st.restored.splice(0)) st.ctx.publish(topic, topic.indexOf("zoom:") === 0 ? st.zoom : st.legend);
	}

	function initChart(st) {
		const ec = window.echarts, card = st.card, doc = st.ctx.document;
		const t = registerTheme(ec, doc);
		st.inst = ec.init(st.canvas, typeof card.theme === "string" ? card.theme : t.name,
			{ renderer: card.renderer === "svg" ? "svg" : "canvas", locale: localeOf(doc) });
		if (typeof card.group === "string") {
			st.inst.group = card.group;
			ec.connect(card.group);
		}
		wireEvents(st);
		if (!st.ro && typeof ResizeObserver === "function") {
			st.ro = new ResizeObserver(() => {
				if (st.raf) return;
				st.raf = requestAnimationFrame(() => {
					st.raf = 0;
					if (st.inst) st.inst.resize();
				});
			});
			st.ro.observe(st.plot);
		}
	}

	// Also releases the resize observer and a pending frame: initChart observes the NEW plot.
	function disposeInst(st) {
		if (st.ro) { st.ro.disconnect(); st.ro = null; }
		if (st.raf && typeof cancelAnimationFrame === "function") cancelAnimationFrame(st.raf);
		st.raf = 0;
		if (st.inst) {
			try { st.inst.dispose(); } catch (e) { log(e); }
		}
		st.inst = null;
	}

	// One load and draw from card src or data.  A failure after the first good draw marks the card stale.
	function cycle(st, isRefresh) {
		const card = st.card, seq = beginDraw(st);
		if (!st.inst && !Array.isArray(card.data)) st.ctx.paintLoading(st.body);
		// Only the load is E-JS-62; anything else uncoded is a draw-phase failure (E-JS-98).
		const loaded = loadRows(st).catch(e => { throw e && e.code ? e : chartError("E-JS-62", card.id, msgOf(e)); });
		return loaded.then(rows => {
			if (stale(st, seq)) return undefined;
			return drawFrame(st, rowsFrame(card, rows), seq).then(() => { if (!stale(st, seq)) setStale(st, null); });
		}).catch(e => {
			if (stale(st, seq)) return;
			const err = drawError(card, e);
			if (isRefresh && hasPicture(st)) {
				log(err);
				setStale(st, err);
				return;
			}
			throw report(st, err);
		}).finally(() => endDraw(st, seq));
	}

	function startPolling(st) {
		const card = st.card;
		if (st.poll || card.refreshMs == null || typeof card.src !== "string") return;
		let ms = Number(card.refreshMs);
		if (!(ms >= MIN_REFRESH_MS)) {
			log(chartError("E-JS-75", card.id, card.refreshMs, MIN_REFRESH_MS));
			ms = MIN_REFRESH_MS;
		}
		// A timer delay above 2^31-1 fires at once and repeatedly.
		ms = Math.min(ms, MAX_REFRESH_MS);
		st.poll = st.ctx.every(ms, () => { st.ctx.refresh(); });
	}

	// ---- J3. a source card: the data role ----

	function onHandle(handle, meta, card, el) {
		const st = states.get(el);
		if (!st || st.destroyed || handle == null) return undefined;
		const data = window.JuneauViews && window.JuneauViews.data;
		if (!data || !data.isHandle(handle) || handle.source !== String(card.source).slice(5)) {
			log(chartError("E-JS-95", card.source, JSON.stringify(handle)));
			return undefined;
		}
		st.handle = handle;
		// A card still waiting for a $fn/decorator to be registered draws nothing yet (start() seeds from the bus once it can).
		if (st.waiting) return undefined;
		if (handle.status === "error") {
			const e = chartError("E-JS-62", card.id, isObj(handle.error) && handle.error.message || "the source failed");
			// The error supersedes any draw still in flight (it would otherwise repaint over the error and clear the stale mark).
			if (st.fetcher) st.fetcher.abort();
			++st.seq;
			if (st.inst) { log(e); setStale(st, e); } else report(st, e);
			drawEnded(st);
			return undefined;
		}
		const partial = card.partialFrames === true && handle.status === "loading" ? data.get(Object.assign({}, handle, { partial: true })) : null;
		const frame = partial || data.get(handle);
		const settle = () => {
			setOverlay(st, handle);
			setStale(st, handle.status === "stale" ? new Error("the source is stale") : null);
		};
		if (!frame || (!partial && handle.version === st.version && st.inst)) {
			if (!st.inst && handle.status === "loading") st.ctx.paintLoading(st.body);
			settle();
			return undefined;
		}
		st.version = partial ? -1 : handle.version;
		const seq = beginDraw(st);
		return drawFrame(st, frame, seq).then(() => { if (!stale(st, seq)) settle(); }, e => {
			if (stale(st, seq)) return;
			const err = drawError(card, e);
			// A failed redraw keeps the last picture and marks it stale.
			if (hasPicture(st)) { log(err); setStale(st, err); return; }
			// Painted and logged here.  Not rethrown: the shell's role path (unlike render/refresh) does not honour cardReported
			// and would log a second, banner-level E-JS-8.
			report(st, err);
		}).finally(() => endDraw(st, seq));
	}

	// ---- J4. events, clicks and the keyboard ----

	const JSON_NODES_MAX = 5000;

	// A JSON-safe copy of event data (Date to ISO text, non-finite numbers to null, functions/DOM/typed arrays dropped), bounded in
	// depth (8) and in total nodes, scalars included (JSON_NODES_MAX; over it the rest is dropped), and never carrying a "__proto__" key.
	function jsonCopy(v, depth, budget) {
		budget = budget || { n: JSON_NODES_MAX };
		if (--budget.n < 0) return undefined;
		if (v == null || typeof v === "string" || typeof v === "boolean") return v;
		if (typeof v === "number") return isFinite(v) ? v : null;
		if (typeof v !== "object" || depth > 8 || typeof v.nodeType === "number" || ArrayBuffer.isView(v)) return undefined;
		if (Object.prototype.toString.call(v) === "[object Date]") return isFinite(v.getTime()) ? v.toISOString() : null;
		if (Array.isArray(v)) {
			const list = [];
			for (let i = 0; i < v.length && budget.n > 0; i++) { const c = jsonCopy(v[i], depth + 1, budget); list.push(c === undefined ? null : c); }
			return list;
		}
		const out = {};
		for (const k of Object.keys(v)) {
			if (budget.n <= 0) break;
			if (k === "__proto__") continue;
			const c = jsonCopy(v[k], depth + 1, budget);
			if (c !== undefined) out[k] = c;
		}
		return out;
	}

	function capped(v) {
		const c = jsonCopy(v, 0);
		if (c === undefined) return null;
		return JSON.stringify(c).length <= META_MAX ? c : null;
	}

	// A legend selection map within the state size cap.  Over the cap, only the deselected names are kept (a name absent from the map
	// reads as selected, so the compact form restores and folds the same way); null when even that does not fit.
	function legendSelected(selected) {
		const full = capped(selected);
		if (full) return full;
		if (!isObj(selected)) return null;
		const off = {};
		for (const k of Object.keys(selected)) if (selected[k] === false) off[k] = false;
		return capped(off);
	}

	function rowJson(frame, i) {
		const out = {};
		for (const d of frame.dims.slice(0, ROW_FIELDS_MAX)) {
			const v = cell(frame, d, i);
			out[d.name] = typeof v === "string" && v.length > ROW_STRING_MAX ? v.slice(0, ROW_STRING_MAX) : v;
		}
		return out;
	}

	// True when the card runs something on a point: an href, or an action bound to a topic (onClick or topicActions).
	function isInteractive(card) {
		return actionBindings(card).length > 0 || isObj(card.onClick) && typeof card.onClick.href === "string";
	}

	// True when Enter on the focused point does something: it runs something, or it publishes a click topic.  Such a plot is
	// an operable group with a hint and a live region, not a picture.
	function isOperable(card) {
		return isInteractive(card) || isObj(card.on) && typeof card.on.click === "string";
	}

	// Says `text` through the card's polite live region (if it has one); empty clears it.
	function announce(st, text) {
		if (st.live) st.live.textContent = text;
	}

	function pointText(frame, i) {
		return frame.dims.slice(0, ROW_FIELDS_MAX).map(d => [d.name, cellText(frame, d, i)]).filter(e => e[1] !== "")
			.map(e => e[0] + " " + e[1]).join(", ");
	}

	function leafText(node) {
		const meta = isObj(node.meta) ? node.meta : {};
		return [node.name].concat(Object.keys(meta).filter(k => meta[k] !== node.name).map(k => k + " " + meta[k])).join(", ");
	}

	// A treemap group (a node with children) is not a row: clicking one navigates the view, it publishes nothing.
	function isTreemapGroup(p) {
		return p.seriesType === "treemap" && isObj(p.data) && Array.isArray(p.data.children) && p.data.children.length > 0;
	}

	// Umbrella 3.1: only the listed fields leave the chart.
	function sanitize(st, event, p) {
		p = isObj(p) ? p : {};
		const out = { schemaVersion: 1, card: st.card.id, event: event };
		if (typeof p.seriesId === "string") out.seriesId = p.seriesId;
		if (typeof p.seriesName === "string") out.seriesName = p.seriesName;
		if (typeof p.dataIndex === "number") out.dataIndex = p.dataIndex;
		if (typeof p.name === "string") out.name = p.name;
		if (p.value !== undefined) out.value = jsonCopy(p.value, 0);
		const f = st.frame;
		// A treemap tile carries its fields in meta (tileFields); its dataIndex is a tree position, not a frame row.
		const tree = p.seriesType === "treemap";
		out.row = !tree && f && p.componentType === "series" && !isCarrier({ id: p.seriesId }) && typeof p.dataIndex === "number"
			&& p.dataIndex >= 0 && p.dataIndex < f.rowCount ? rowJson(f, p.dataIndex) : null;
		out.meta = isObj(p.data) && p.data.meta !== undefined ? capped(p.data.meta) : null;
		const b = Array.isArray(p.batch) && isObj(p.batch[0]) ? p.batch[0] : p;
		if (event === "datazoom") for (const k of ["start", "end", "startValue", "endValue"]) out[k] = b[k] === undefined ? null : jsonCopy(b[k], 0);
		if (event === "legendselectchanged") out.selected = legendSelected(p.selected);
		if (event === "brushselected") out.areas = arr(b.areas).map(a => ({ coordRange: jsonCopy(isObj(a) ? a.coordRange : null, 0) }));
		return out;
	}

	function navigate(st, payload, p) {
		const tpl = isObj(st.card.onClick) ? st.card.onClick.href : null;
		if (typeof tpl !== "string") return;
		const lookup = Object.assign(Object.create(null), isObj(payload.meta) ? payload.meta : {}, isObj(payload.row) ? payload.row : {});
		const url = resolveHref(tpl, lookup);
		if (!isSafeHref(url)) {
			log(chartError("E-JS-79", st.card.id, url));
			return;
		}
		const ev = p && p.event && p.event.event || p && p.domEvent || null;
		if (ev && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.button === 1)) window.open(url, "_blank", "noopener");
		else window.location.assign(url);
	}

	// A group header runs nothing (the click only zooms), so a card that runs something on a tile says so for a moment.
	function hintTile(st) {
		if (!isInteractive(st.card) || !st.banner || st.frame && st.frame.truncated) return;
		st.banner.textContent = "Select a tile, not a group, to open it.";
		st.banner.hidden = false;
		clearTimeout(st.hintTimer);
		st.hintTimer = setTimeout(() => {
			if (!st.destroyed && st.banner && !(st.frame && st.frame.truncated)) { st.banner.hidden = true; st.banner.textContent = ""; }
		}, 4000);
	}

	function onClick(st, p) {
		if (isObj(p) && isTreemapGroup(p)) { hintTile(st); return; }
		const on = isObj(st.card.on) ? st.card.on : {};
		const payload = sanitize(st, "click", p);
		if (typeof on.click === "string") st.ctx.publish(on.click, payload);
		navigate(st, payload, p);
	}

	// The zoom range and legend selection on screen, in the shape foldState reads.
	function viewState(st) {
		const o = st.inst.getOption(), l = o && Array.isArray(o.legend) ? o.legend[0] : null;
		return { zoom: zoomState(st), legend: isObj(l) && isObj(l.selected) ? { selected: Object.assign({}, l.selected) } : null };
	}

	function zoomState(st) {
		const o = st.inst && st.inst.getOption();
		const d = o && Array.isArray(o.dataZoom) ? o.dataZoom[0] : null;
		if (!isObj(d)) return null;
		const v = k => typeof d[k] === "number" || typeof d[k] === "string" ? d[k] : null;
		return { schemaVersion: 1, start: v("start"), end: v("end"), startValue: v("startValue"), endValue: v("endValue") };
	}

	function wireEvents(st) {
		const inst = st.inst, card = st.card, on = isObj(card.on) ? card.on : {};
		for (const ev of Object.keys(on)) {
			if (ev === "click" || !EVENT_NAME_RE.test(ev) || typeof on[ev] !== "string") continue;
			inst.on(ev, p => { if (!(isObj(p) && isTreemapGroup(p))) st.ctx.publish(on[ev], sanitize(st, ev, p)); });
		}
		inst.on("click", p => onClick(st, p));
		const zoom = "zoom:" + card.id, legend = "legend:" + card.id;
		if (publishesRetained(card, zoom))
			inst.on("datazoom", () => {
				clearTimeout(st.zoomTimer);
				st.zoomTimer = setTimeout(() => {
					const z = zoomState(st);
					if (z && !st.destroyed) { st.zoom = z; st.ctx.publish(zoom, z); }
				}, ZOOM_DEBOUNCE_MS);
			});
		if (publishesRetained(card, legend))
			inst.on("legendselectchanged", p => {
				const sel = legendSelected(p && p.selected);
				if (!sel) return;
				st.legend = { schemaVersion: 1, selected: sel };
				st.ctx.publish(legend, st.legend);
			});
	}

	// The treemap series, when the drawn option has one.
	function treemapEntry(st) {
		return st.marked ? dataSeries(st.marked).find(e => e.s.type === "treemap") || null : null;
	}

	// The leaves of the treemap series in pre-order.  dataIndex is the leaf's position in the option tree counting ECharts' own
	// virtual root as 0, so the top node is 1.  parentId is the enclosing group, which a zoom targets.
	function treemapLeaves(st, entry) {
		if (st.leafCache && st.leafCache.of === st.marked) return st.leafCache.leaves;
		const leaves = [];
		let n = 1;
		const walk = (nodes, parentId) => {
			for (const node of arr(nodes)) {
				if (!isObj(node)) continue;
				const dataIndex = n++;
				if (arr(node.children).length) walk(node.children, node.id);
				else leaves.push({ node: node, parentId: parentId, dataIndex: dataIndex });
			}
		};
		walk(entry.s.data, TREEMAP_ROOT);
		st.leafCache = { of: st.marked, leaves: leaves };
		return leaves;
	}

	// The enclosing group of a node id: the id minus its last (encoded) segment, or the top node.
	function treemapParent(id) {
		const at = String(id).lastIndexOf("/");
		return at < 0 ? TREEMAP_ROOT : id.slice(0, at);
	}

	// Arrow keys, Home and End walk dataIndex on the first data series; Enter is a click; Escape and blur end the walk.
	// On a treemap the walk is over the leaves and keyIndex is a leaf position.
	function clearWalk(st) {
		if (st.keyIndex < 0) return;
		const tree = st.inst && st.marked ? treemapEntry(st) : null;
		const first = tree || (st.inst && st.marked ? dataSeries(st.marked)[0] : null);
		if (first) {
			const leaf = tree ? treemapLeaves(st, tree)[st.keyIndex] : null;
			if (!tree || leaf)
				st.inst.dispatchAction({ type: "downplay", seriesIndex: first.index, dataIndex: tree ? leaf.dataIndex : st.keyIndex });
			st.inst.dispatchAction({ type: "hideTip" });
		}
		st.keyIndex = -1;
		announce(st, "");
	}

	function onTreemapKey(st, ev, entry) {
		const leaves = treemapLeaves(st, entry), n = leaves.length, k = ev.key;
		const at = st.keyIndex >= 0 && st.keyIndex < n ? leaves[st.keyIndex] : null;
		const root = st.treemapRoot || TREEMAP_ROOT;
		if (k === "Escape") { clearWalk(st); return; }
		if (k === "Enter") {
			if (!at) return;
			ev.preventDefault();
			onClick(st, { componentType: "series", seriesType: "treemap", seriesIndex: entry.index, seriesId: entry.s.id, seriesName: entry.s.name,
				dataIndex: at.dataIndex, name: at.node.name, value: at.node.value, data: { id: at.node.id, meta: at.node.meta }, domEvent: ev });
			return;
		}
		if (k === "+" || k === "=") {
			if (!at) return;
			ev.preventDefault();
			st.treemapRoot = at.parentId;
			st.inst.dispatchAction({ type: "treemapZoomToNode", seriesIndex: entry.index, targetNodeId: at.parentId });
			announce(st, "Zoomed to " + (at.parentId === TREEMAP_ROOT ? "all" : at.parentId));
			return;
		}
		if (k === "-" || k === "_") {
			ev.preventDefault();
			if (root === TREEMAP_ROOT) return;
			st.treemapRoot = treemapParent(root);
			st.inst.dispatchAction({ type: "treemapRootToNode", seriesIndex: entry.index, targetNodeId: st.treemapRoot });
			announce(st, "Zoomed out to " + (st.treemapRoot === TREEMAP_ROOT ? "all" : st.treemapRoot));
			return;
		}
		if (k === "0") {
			ev.preventDefault();
			st.treemapRoot = TREEMAP_ROOT;
			st.inst.dispatchAction({ type: "treemapRootToNode", seriesIndex: entry.index, targetNodeId: TREEMAP_ROOT });
			announce(st, "Zoom reset");
			return;
		}
		const step = k === "ArrowRight" || k === "ArrowDown" ? 1 : k === "ArrowLeft" || k === "ArrowUp" ? -1 : 0;
		if (!n || (!step && k !== "Home" && k !== "End")) return;
		ev.preventDefault();
		let next;
		if (k === "Home") next = 0;
		else if (k === "End") next = n - 1;
		else next = st.keyIndex < 0 ? (step > 0 ? 0 : n - 1) : Math.max(0, Math.min(n - 1, st.keyIndex + step));
		if (at) st.inst.dispatchAction({ type: "downplay", seriesIndex: entry.index, dataIndex: at.dataIndex });
		st.keyIndex = next;
		st.inst.dispatchAction({ type: "highlight", seriesIndex: entry.index, dataIndex: leaves[next].dataIndex });
		st.inst.dispatchAction({ type: "showTip", seriesIndex: entry.index, dataIndex: leaves[next].dataIndex });
		announce(st, leafText(leaves[next].node));
	}

	function onKey(st, ev) {
		if (!st.inst || !st.frame || !st.marked) return;
		const tree = treemapEntry(st);
		if (tree) { onTreemapKey(st, ev, tree); return; }
		const first = dataSeries(st.marked)[0];
		if (!first) return;
		const n = st.frame.rowCount, k = ev.key;
		if (k === "Escape") { clearWalk(st); return; }
		if (k === "Enter") {
			if (st.keyIndex < 0) return;
			ev.preventDefault();
			onClick(st, { componentType: "series", seriesIndex: first.index, seriesId: first.s.id, seriesName: first.s.name,
				dataIndex: st.keyIndex, domEvent: ev });
			return;
		}
		const step = k === "ArrowRight" || k === "ArrowDown" ? 1 : k === "ArrowLeft" || k === "ArrowUp" ? -1 : 0;
		if (!n || (!step && k !== "Home" && k !== "End")) return;
		ev.preventDefault();
		let next;
		if (k === "Home") next = 0;
		else if (k === "End") next = n - 1;
		else next = st.keyIndex < 0 ? (step > 0 ? 0 : n - 1) : Math.max(0, Math.min(n - 1, st.keyIndex + step));
		if (st.keyIndex >= 0) st.inst.dispatchAction({ type: "downplay", seriesIndex: first.index, dataIndex: st.keyIndex });
		st.keyIndex = next;
		st.inst.dispatchAction({ type: "highlight", seriesIndex: first.index, dataIndex: st.keyIndex });
		st.inst.dispatchAction({ type: "showTip", seriesIndex: first.index, dataIndex: st.keyIndex });
		announce(st, pointText(st.frame, st.keyIndex));
	}

	// ---- J5. commands and the card API ----

	function pick(o, keys) {
		const out = {};
		for (const k of keys) if (typeof o[k] === "string" || typeof o[k] === "number") out[k] = o[k];
		return out;
	}

	function exportImage(st, type, o) {
		if (!st.inst) return Promise.reject(new Error("the chart is not drawn yet"));
		const t = type === "svg" ? "svg" : "png";
		if (t === "svg" && st.card.renderer !== "svg") return Promise.reject(new Error("svg export needs renderer 'svg'"));
		const opts = isObj(o) ? o : {};
		// The pixel ratio is clamped (a bus publisher must not size a huge canvas); a throwing getDataURL rejects.
		const ratio = typeof opts.pixelRatio === "number" && isFinite(opts.pixelRatio) ? Math.min(4, Math.max(1, opts.pixelRatio)) : 1;
		// A transparent chart background would export light ink on nothing under a dark scheme: the image takes the surface instead.
		const doc = st.ctx.document, bg = currentTheme(doc).tokens["--jc-chart-bg"];
		const backgroundColor = bg === "transparent" && cssValue(rootStyle(doc), "--jc-surface") || bg;
		return new Promise(resolve => resolve(st.inst.getDataURL({ type: t, pixelRatio: ratio, backgroundColor: backgroundColor })));
	}

	function download(st, url, type) {
		// Only the image data URL ECharts produced is linked; anything else (a hostile or buggy getDataURL) is refused.
		if (typeof url !== "string" || !/^data:image\//.test(url)) throw new Error("the export is not an image data URL");
		const a = htmlNode(st.ctx.document, "a");
		a.setAttribute("href", url);
		a.setAttribute("download", st.card.id + "." + (type === "svg" ? "svg" : "png"));
		st.body.appendChild(a);
		a.click();
		st.body.removeChild(a);
	}

	function applyOp(st, op, o) {
		const inst = st.inst;
		if (op === "highlight" || op === "downplay")
			return inst.dispatchAction(Object.assign({ type: op }, pick(o, ["seriesId", "seriesName", "dataIndex", "name"])));
		if (op === "showTip" || op === "hideTip")
			return inst.dispatchAction(Object.assign({ type: op }, pick(o, ["seriesIndex", "dataIndex", "name"])));
		if (op === "zoomTo")
			return inst.dispatchAction(Object.assign({ type: "dataZoom" }, pick(o, ["start", "end", "startValue", "endValue"])));
		if (op === "legendSelect") {
			if (typeof o.name === "string")
				return inst.dispatchAction({ type: o.selected === false ? "legendUnSelect" : "legendSelect", name: o.name });
			if (!isObj(o.selected)) throw new Error("legendSelect needs a name or a selected map");
			for (const name of Object.keys(o.selected))
				inst.dispatchAction({ type: o.selected[name] === false ? "legendUnSelect" : "legendSelect", name: name });
			return undefined;
		}
		return exportImage(st, o.type, o).then(url => download(st, url, o.type));
	}

	function runOp(op, cmd, el) {
		const st = states.get(el);
		if (!st || st.destroyed) return undefined;
		const fail = reason => { log(chartError("E-JS-103", st.card.id, op, reason)); };
		const o = cmd.options === undefined ? {} : cmd.options;
		if (!isObj(o)) return fail("options must be an object");
		let targets = [st];
		if (cmd.target === "group") {
			if (typeof st.card.group !== "string") return fail("target 'group' needs a card group");
			targets = Array.from(live).filter(s => s.card.group === st.card.group && !s.destroyed);
		}
		const runs = [];
		for (const s of targets) {
			if (!s.inst) { if (s === st) fail("the chart is not drawn yet"); continue; }
			try {
				const r = applyOp(s, op, o);
				if (r && typeof r.then === "function") runs.push(r.catch(e => fail(msgOf(e))));
			} catch (e) {
				fail(msgOf(e));
			}
		}
		return runs.length ? Promise.all(runs).then(() => undefined) : undefined;
	}

	const ops = {};
	for (const op of OPS) ops[op] = (cmd, card, el) => runOp(op, cmd, el);

	// ---- J6. render, refresh, destroy ----

	// A published zoom:/legend: value restored by the dashboard, folded in before the first draw.
	function restore(st) {
		const dash = window.JuneauViews && window.JuneauViews.dashboard;
		if (!dash || typeof dash.initial !== "function") return;
		for (const topic of ["zoom:" + st.card.id, "legend:" + st.card.id]) {
			if (!publishesRetained(st.card, topic)) continue;
			let v = null;
			try { v = dash.initial(topic); } catch (e) { log(e); }
			if (!isObj(v)) continue;
			if (topic.indexOf("zoom:") === 0) st.zoom = Object.assign({ schemaVersion: 1 }, pick(v, ["start", "end", "startValue", "endValue"]));
			else {
				const sel = isObj(v.selected) ? legendSelected(v.selected) : null;
				if (!sel) continue;
				st.legend = { schemaVersion: 1, selected: sel };
			}
			st.restored.push(topic);
		}
	}

	// Names the card uses that are not registered yet.
	function missingNames(card) {
		const out = [];
		const add = (kind, name) => { if (!impls[kind].has(name) && !out.some(m => m.kind === kind && m.name === name)) out.push({ kind: kind, name: name }); };
		const walk = (v, depth) => {
			if (depth > 32) return;
			if (isMarker(v)) {
				add("fn", v.$fn);
				if (isObj(v.args)) walk(v.args, depth + 1);
				return;
			}
			if (Array.isArray(v)) { for (const x of v) walk(x, depth + 1); return; }
			if (isObj(v)) for (const k of Object.keys(v)) walk(v[k], depth + 1);
		};
		walk(card.helper, 0);
		walk(card.option, 0);
		walk(card.patch, 0);
		for (const d of arr(card.decorators)) {
			if (isObj(d) && typeof d.use === "string") add("decorator", d.use);
			walk(d, 0);
		}
		return out;
	}

	// A source card draws from its current bus handle (a refresh or a late registration re-seeds it); null when there is none.
	function seedFromBus(st) {
		const bus = window.JuneauViews && window.JuneauViews.bus;
		const h = bus && typeof bus.get === "function" ? bus.get(st.card.source) : null;
		if (!h) return null;
		st.version = -1;
		return Promise.resolve(onHandle(h, null, st.card, st.el));
	}

	function start(st) {
		st.waiting = false;
		if (typeof st.card.source === "string") {
			const seeded = seedFromBus(st);
			if (seeded) return seeded;
			st.ctx.paintLoading(st.body);
			return undefined;
		}
		startPolling(st);
		return cycle(st, false);
	}

	function wait(st, missing) {
		st.waiting = true;
		st.ctx.paintLoading(st.body);
		const resume = () => {
			if (!st.waiting || st.destroyed || missingNames(st.card).length) return;
			// A source card's start() may return nothing.
			Promise.resolve(start(st)).catch(() => { /* already painted and logged */ });
		};
		for (const m of missing) waitFor(m.kind, m.name, resume, st);
		st.ctx.onReady(() => {
			if (!st.waiting || st.destroyed || (st.ctx.signal && st.ctx.signal.aborted)) return;
			const m = missingNames(st.card)[0];
			if (!m) return;
			const names = Array.from(impls[m.kind].keys()).sort().join(", ") || "none";
			report(st, m.kind === "fn" ? chartError("E-JS-90", st.card.id, m.name, names) : chartError("E-JS-91", st.card.id, m.name, names));
		});
	}

	// The ordered (topic, action) pairs a card runs: its onClick action on the click topic, plus each topicActions entry.
	function actionBindings(card) {
		const out = [], on = isObj(card.on) ? card.on : {};
		if (isObj(card.onClick) && isObj(card.onClick.action) && typeof on.click === "string")
			out.push({ topic: on.click, action: card.onClick.action });
		for (const t of arr(card.topicActions))
			if (isObj(t) && typeof t.topic === "string" && isObj(t.action)) out.push({ topic: t.topic, action: t.action });
		return out;
	}

	// Subscribes the card to each action topic; echo is on because the card's own click publish is what triggers it.
	function wireActions(st) {
		if (st.destroyed) return;
		st.actionOffs = actionBindings(st.card).map(b =>
			st.ctx.subscribe(b.topic, payload => runAction(st, b.action, payload), { echo: true }));
	}

	// Marks the card busy while an action is pending: a data flag for tests on the card, and on the plot aria-busy for assistive tech
	// and a progress cursor.  aria-busy stays off the card: the action's progress and outcome banner lives inside it, and a busy
	// subtree can hold back the live-region announcements that banner exists to make.
	function setBusy(st, on) {
		st.actionBusy = on;
		const el = st.el, plot = el.querySelector(".jc-chart-plot") || st.plot;
		if (on) el.setAttribute("data-juneau-action-busy", "");
		else el.removeAttribute("data-juneau-action-busy");
		if (plot && plot.setAttribute) {
			if (on) plot.setAttribute("aria-busy", "true");
			else plot.removeAttribute("aria-busy");
		}
		if (plot && plot.style) plot.style.cursor = on ? "progress" : "";
	}

	// Runs one action for a clicked point through the views hook; one run at a time per card, released when the run settles.
	function runAction(st, action, payload) {
		if (st.destroyed || st.actionBusy) return;
		const hook = window.JuneauViews && window.JuneauViews.actions;
		if (!hook || typeof hook.run !== "function") {
			if (!st.warnedNoHook) { st.warnedNoHook = true; log(chartError("E-JS-108", st.card.id)); }
			return;
		}
		const p = isObj(payload) ? payload : {};
		const row = Object.assign({}, { name: p.name, value: p.value, seriesName: p.seriesName },
			isObj(p.row) ? p.row : {}, isObj(p.meta) ? p.meta : {});
		setBusy(st, true);
		let pending;
		try {
			pending = Promise.resolve(hook.run(action, { host: st.el, row: row, onRedraw: () => { if (!st.destroyed) st.ctx.refresh(); }, catalog: st.card.actions }));
		} catch (e) {
			setBusy(st, false);
			return;
		}
		pending.then(() => {}, () => {}).finally(() => setBusy(st, false));
	}

	function teardown(el) {
		const st = states.get(el);
		if (!st) return;
		st.destroyed = true;
		st.waiting = false;
		states.delete(el);
		live.delete(st);
		if (st.poll) { st.poll.stop(); st.poll = null; }
		if (st.fetcher) st.fetcher.abort();
		clearTimeout(st.zoomTimer);
		clearTimeout(st.hintTimer);
		dropWaiters(st);
		for (const off of st.actionOffs || []) off();
		st.actionOffs = null;
		const hook = window.JuneauViews && window.JuneauViews.actions;
		if (hook && typeof hook.release === "function") hook.release(el);
		disposeInst(st);
	}

	function render(card, el, ctx) {
		teardown(el);
		const doc = ctx.document;
		for (const c of Array.from(el.childNodes))
			if (!(c.classList && c.classList.contains("jc-card-title"))) el.removeChild(c);
		const body = htmlNode(doc, "div", "jc-card-body jc-chart-body");
		el.appendChild(body);
		const st = {
			card: card, ctx: ctx, el: el, body: body, inst: null, frame: null, marked: null, handle: null, version: 0,
			seq: 0, fetcher: null, cache: new Map(), poll: null, zoom: null, legend: null, zoomTimer: 0, restored: [],
			ro: null, raf: 0, keyIndex: -1, waiting: false, destroyed: false,
			fig: null, caption: null, banner: null, plot: null, canvas: null, overlay: null, toggle: null, box: null, note: null, table: null
		};
		states.set(el, st);
		live.add(st);
		ctx.setApi({
			export: (type, opts) => exportImage(st, type, opts),
			getOption: () => st.marked ? copyOption(st.marked) : null,
			chart: () => st.inst
		});
		wireActions(st);
		aliasOnConsole();
		const ec = window.echarts;
		if (!ec || typeof ec.init !== "function") return Promise.reject(report(st, chartError("E-JS-61", card.id)));
		restore(st);
		const missing = missingNames(card);
		if (missing.length) {
			wait(st, missing);
			return undefined;
		}
		return start(st);
	}

	function sameShape(a, b) {
		return a.src === b.src && a.source === b.source && a.renderer === b.renderer && a.theme === b.theme && a.group === b.group
			&& isOperable(a) === isOperable(b);
	}

	function refresh(card, el, ctx) {
		const st = states.get(el);
		if (!st || st.waiting || !st.inst || !sameShape(st.card, card)) return render(card, el, ctx);
		const rebind = JSON.stringify(actionBindings(st.card)) !== JSON.stringify(actionBindings(card));
		st.card = card;
		// The subscriptions close over their action: resubscribe when a topic or an action differs.
		if (rebind) {
			for (const off of st.actionOffs || []) off();
			st.actionOffs = null;
			wireActions(st);
		}
		st.cache.clear();
		if (typeof card.source === "string") {
			// A source card redraws from its current handle; the source refreshes itself.
			return seedFromBus(st) || undefined;
		}
		return cycle(st, true);
	}

	function destroy(card, el) {
		teardown(el);
	}

	// Implicit framework topics the shell claims for the card: its opt-in zoom:/legend: state topics.
	// An IDREF is one token: whitespace in a card id would split aria-controls / aria-labelledby into several references.
	function idrefOf(id) { return String(id).replace(/\s+/g, "_"); }

	function implicit(card) {
		return ["zoom:" + card.id, "legend:" + card.id].filter(t => publishesRetained(card, t));
	}

	// Re-themes a drawn chart in place: one redraw of its current frame through the pipeline, so the colours baked into the
	// option are re-read, painted together with setTheme on the same instance (zoom, legend selection and identity are kept).
	// ECharts without setTheme (an app pinning ECharts 5) gets a fresh instance instead.
	function retheme(st) {
		if (st.destroyed || !st.inst || !st.frame) {
			// A chart not drawn yet initialises under the theme registered for the page now.
			st.rethemePending = false;
			return;
		}
		// The theme this draw starts from: a failure leaves the re-theme owed only if the theme has moved on since.  A throwing
		// ctx getter fails the draw itself (below), so it is not read here.
		const want = theme;
		if (typeof st.inst.setTheme !== "function") disposeInst(st);
		const seq = beginDraw(st);
		// paint clears the debt once the theme it baked from is still the page's (a new instance initialises under it).
		drawFrame(st, st.frame, seq, true).then(() => endDraw(st, seq), e => {
			if (!stale(st, seq)) {
				if (want === null || want === theme) st.rethemePending = false;
				const err = drawError(st.card, e);
				// As a failed refresh: the last picture stays, marked stale.
				if (hasPicture(st)) { log(err); setStale(st, err); } else report(st, err);
			}
			endDraw(st, seq);
		});
	}

	if (typeof document !== "undefined" && document && typeof document.addEventListener === "function")
		document.addEventListener("juneau:theme-changed", () => {
			theme = null;
			// Registered again with the tokens as they are now, so a chart initialised later gets them too.
			if (window.echarts && typeof window.echarts.registerTheme === "function") registerTheme(window.echarts, document);
			for (const st of Array.from(live)) {
				// An author-chosen ECharts theme is not built from the tokens, so it is not ours to replace.
				if (typeof st.card.theme === "string") continue;
				st.rethemePending = true;
				// setTheme must not run inside the ECharts update cycle, and a draw in flight would paint its older colours
				// over the new theme: the re-theme waits for the draw to end (endDraw runs it).
				if (st.pending) continue;
				retheme(st);
			}
		});

	// ---- K. the public API and the pre-load queue ------------------------------------------------------------------

	register("builder", "rows", rowsBuilder, true);

	// The data-source built-ins ([registry, name, impl] triples on JuneauViews.data.builtins) may use bare names.
	function drainBuiltins() {
		const views = window.JuneauViews;
		for (const t of arr(views && views.data && views.data.builtins)) {
			try {
				if (Array.isArray(t) && t.length === 3 && REGISTRIES.indexOf(t[0]) >= 0) register(t[0], t[1], t[2], true);
			} catch (e) {
				if (!e.code) log(e);
			}
		}
	}

	const api = Object.freeze({
		fn: registryApi("fn"),
		decorator: registryApi("decorator"),
		parser: registryApi("parser"),
		builder: registryApi("builder"),
		transform: registryApi("transform"),
		// The pipeline without setOption: the option a card would draw, with $fn markers resolved.
		// A throwing ctx (a getter) is a rejection too: build() always returns a promise.
		build: (card, frame, ctx) => {
			try {
				return pipeline(card, frame, decoratorCtx(card, ctx)).then(r => r.option);
			} catch (e) {
				return Promise.reject(e);
			}
		},
		version: "2",
		// TEST-ONLY and UNSTABLE: not part of the public API; may change or vanish without notice.
		// The number of chart instances the glue is currently tracking (cards rendered and not yet destroyed).
		__liveCount: () => live.size
	});

	function aliasOnConsole() {
		const jc = window.JuneauConsole;
		if (jc && jc.charts !== api) {
			try { jc.charts = api; } catch (e) { /* a frozen JuneauConsole keeps only window.JuneauCharts */ }
		}
	}

	window.JuneauCharts = api;
	aliasOnConsole();
	drainBuiltins();

	const queued = Array.isArray(window.JuneauConsoleCharts) ? window.JuneauConsoleCharts.slice() : [];
	window.JuneauConsoleCharts = { push: queueEntry };
	queued.forEach(queueEntry);

	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["chart",
		{ render: render, refresh: refresh, destroy: destroy, roles: { data: onHandle }, ops: ops, implicit: implicit }]);
})();
