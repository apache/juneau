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
 * juneau-charts.js - the console chart card (type "chart"): line, bar and donut kinds drawn as inline SVG, plus an
 * open kind registry.
 *
 *   JuneauCharts.register(kind, renderer)   renderer(plotEl, rows, card, c) -> optional {update(rows), destroy()}
 *   JuneauCharts.kinds()                    sorted registered kinds
 *   window.JuneauConsoleCharts = [[kind, renderer], ...]   queue drained when this script loads
 *
 * The renderer's helper object c: document, width, height, pad, svg(host, label?), node(parent, tag, cls, attrs),
 * html(tag, cls), scaleLinear(d0, d1, r0, r1), niceMax(v), ticks(lo, hi), tone(series, i), fmt(v), fmtX(v),
 * legend([{label, color}]), click(row, seriesField), clickable, fail(msg).  When clickable is false (no onClick.href),
 * marks are not focusable and not role=button; the svg is then role=img, otherwise role=group.  svg(host) without a
 * label is labelled by the figure caption (aria-labelledby), so the summary is read once.
 */
(function () {
	"use strict";

	const SVG_NS = "http://www.w3.org/2000/svg";
	const MIN_REFRESH_MS = 5000;
	const WIDTH = 640, DEFAULT_HEIGHT = 240, MIN_HEIGHT = 120;
	const PAD = { top: 12, right: 16, bottom: 32, left: 52 };
	const KIND_RE = /^[a-z][a-z0-9-]{0,31}$/;
	const MAX_MARKERS = 60, MAX_X_LABELS = 12, FOLD_BELOW = 0.02;
	const PALETTE = ["#1b6ac9", "#e07a1f", "#2e8540", "#9b3fb5", "#c23934", "#0e8a8a", "#7a6a00", "#5a5a5a"];
	const TONES = {
		danger: "var(--jc-danger, #c23934)",
		ok: "var(--jc-success, #2e8540)",
		warning: "var(--jc-warn, #b86e00)"
	};
	const MSG = {
		"E-JS-60": "chart card '%s' has unknown kind '%s'; registered kinds: '%s'",
		"E-JS-62": "chart card '%s' fetch failed: '%s'",
		"E-JS-63": "chart card '%s' series field '%s' is not in the data rows",
		"E-JS-64": "chart kind '%s' is already registered",
		"E-JS-75": "'%s' refreshMs '%s' is below the minimum '%s'; using the minimum",
		"E-JS-80": "chart card '%s' kind '%s' renderer failed: '%s'",
		"E-JS-79": "chart card '%s' onClick.href resolved to a disallowed URL '%s'"
	};

	const kinds = Object.create(null);
	const waiting = [];
	const states = new WeakMap();

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

	function report(st, e) {
		log(e);
		st.ctx.paintError(st.body, e);
		e.cardReported = true;
		return e;
	}

	function num(v) {
		return typeof v === "number" && isFinite(v) ? v : null;
	}

	function round(v) {
		return Math.round(v * 10) / 10;
	}

	function clear(n) {
		while (n.firstChild) n.removeChild(n.firstChild);
	}

	function xField(card) {
		return typeof card.x === "string" ? card.x : (card.x && card.x.field) || null;
	}

	function xLabel(card) {
		return (card.x && typeof card.x === "object" && card.x.label) || xField(card);
	}

	function seriesOf(card) {
		return (Array.isArray(card.series) ? card.series : []).map(s => typeof s === "string" ? { field: s } : s);
	}

	function isCartesian(card) {
		return card.kind === "line" || card.kind === "bar";
	}

	function isClickable(card) {
		return !!(card.onClick && typeof card.onClick.href === "string");
	}

	function fieldsOf(card) {
		if (isCartesian(card)) return [xField(card)].concat(seriesOf(card).map(s => s.field));
		if (card.kind === "donut") return [card.label, card.value];
		return [];
	}

	function missingField(card, rows) {
		for (const f of fieldsOf(card))
			if (f != null && !rows.some(r => r && Object.hasOwn(r, f))) return f;
		return null;
	}

	function columns(card, rows) {
		if (isCartesian(card))
			return [{ field: xField(card), label: xLabel(card), x: true }].concat(seriesOf(card).map(s => ({ field: s.field, label: s.label || s.field })));
		if (card.kind === "donut") return [{ field: card.label, label: card.label }, { field: card.value, label: card.value }];
		return Object.keys(rows[0] || {}).map(k => ({ field: k, label: k }));
	}

	function formatValue(card, v) {
		if (typeof v !== "number") return v == null ? "" : String(v);
		const f = card.y && card.y.format;
		const o = f === "integer" ? { maximumFractionDigits: 0 }
			: f === "percent" ? { style: "percent", maximumFractionDigits: 1 }
			: f === "compact" ? { notation: "compact" }
			: { maximumFractionDigits: 2 };
		return new Intl.NumberFormat(undefined, o).format(v);
	}

	function formatX(card, v) {
		const type = card.x && typeof card.x === "object" ? card.x.type : null;
		if (type === "time") {
			const d = new Date(v);
			if (!isNaN(d.getTime())) return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", timeZone: "UTC" }).format(d);
		}
		if (type === "number" && typeof v === "number") return formatValue({}, v);
		return v == null ? "" : String(v);
	}

	function summary(card, rows) {
		const name = card.kind.charAt(0).toUpperCase() + card.kind.slice(1) + " chart";
		const n = rows.length;
		if (card.kind === "donut") return name + ", " + n + (n === 1 ? " slice" : " slices");
		if (!isCartesian(card)) return name + ", " + n + (n === 1 ? " row" : " rows");
		const s = seriesOf(card).length;
		let out = name + ", " + s + " series, " + n + (n === 1 ? " point" : " points");
		const xf = xField(card);
		if (n) out += ", " + xLabel(card) + " from '" + formatX(card, rows[0][xf]) + "' to '" + formatX(card, rows[n - 1][xf]) + "'";
		return out;
	}

	function niceMax(v) {
		if (!(v > 0)) return 1;
		const p = Math.pow(10, Math.floor(Math.log10(v)));
		const f = v / p;
		return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * p;
	}

	function ticks(lo, hi) {
		const span = hi - lo || 1;
		const m = span / Math.pow(10, Math.floor(Math.log10(span)));
		const n = Math.abs(m - 2) < 1e-9 ? 4 : 5;
		const out = [];
		for (let i = 0; i <= n; i++) out.push(lo + span * i / n);
		return out;
	}

	function scaleLinear(d0, d1, r0, r1) {
		const span = (d1 - d0) || 1;
		return v => r0 + (v - d0) / span * (r1 - r0);
	}

	function tone(s, i) {
		if (s && typeof s.tone === "number" && s.tone >= 1 && s.tone <= 8) return "var(--jc-chart-" + s.tone + ", " + PALETTE[s.tone - 1] + ")";
		if (s && typeof s.tone === "string" && TONES[s.tone]) return TONES[s.tone];
		const k = i % 8;
		return "var(--jc-chart-" + (k + 1) + ", " + PALETTE[k] + ")";
	}

	function svgNode(doc, tag, cls, attrs) {
		const n = doc.createElementNS ? doc.createElementNS(SVG_NS, tag) : doc.createElement(tag);
		if (cls) n.setAttribute("class", cls);
		if (attrs) for (const k of Object.keys(attrs)) n.setAttribute(k, String(attrs[k]));
		return n;
	}

	function htmlNode(doc, tag, cls) {
		const n = doc.createElement(tag);
		if (cls) n.className = cls;
		return n;
	}

	function resolveHref(tpl, row) {
		return tpl.replace(/\{([^}]+)\}/g, (m, f) => encodeURIComponent(row && row[f] != null ? String(row[f]) : ""));
	}

	/**
	 * The one prefix rule every same-origin / allowlist URL guard applies to the ORIGINAL string, before anything else
	 * (never normalise and accept): true when the first or last character is a space or control character, or any
	 * character is a backslash, a C0 control character or DEL.  A browser strips leading/trailing C0 and space,
	 * deletes tab/CR/LF anywhere and reads a backslash as a slash, so each of those can turn a value that looks like a
	 * same-origin path into "//host".  The text of this function is identical in every file that carries it
	 * (checked by ViewsJs_SameOriginPath_Test) and mirrors SafePathTemplate.isSameOriginPath.  Callers handle the empty string.
	 */
	function hasUnsafeUrlChars(s) {
		return s.charCodeAt(0) <= 32 || s.charCodeAt(s.length - 1) <= 32 || /[\\\u0000-\u001f\u007f]/.test(s);
	}

	// Exact parity with SafePathTemplate.isSameOriginPath (juneau-commons): the original string is refused outright when
	// it has a leading/trailing space or control character, a backslash, or a control character anywhere.
	function isSafeHref(u) {
		const s = String(u);
		if (!s || hasUnsafeUrlChars(s) || s.includes("://") || s.startsWith("//")) return false;
		const colon = s.indexOf(":"), slash = s.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;
		return s.split("/").indexOf("..") < 0;
	}

	function clickHandler(st) {
		return function (row, series) {
			const card = st.card;
			const detail = { card: card.id, row: row, series: series == null ? null : series };
			if (typeof st.el.dispatchEvent === "function")
				st.el.dispatchEvent(new CustomEvent("juneau:chart-click", { detail: detail, bubbles: true }));
			const tpl = card.onClick && card.onClick.href;
			if (typeof tpl !== "string") return;
			const url = resolveHref(tpl, row);
			if (!isSafeHref(url)) {
				log(chartError("E-JS-79", card.id, url));
				return;
			}
			window.location.assign(url);
		};
	}

	function paintLegend(st, items) {
		const doc = st.ctx.document;
		clear(st.legend);
		if (st.card.legend === "none") return;
		const ul = htmlNode(doc, "ul", "jc-chart-legend-list");
		for (const it of items) {
			const li = htmlNode(doc, "li", "jc-chart-legend-item");
			const sw = htmlNode(doc, "span", "jc-chart-swatch");
			sw.setAttribute("aria-hidden", "true");
			sw.style.background = it.color;
			const lb = htmlNode(doc, "span", "jc-chart-legend-label");
			lb.textContent = it.label;
			li.appendChild(sw);
			li.appendChild(lb);
			ul.appendChild(li);
		}
		st.legend.appendChild(ul);
	}

	function chartContext(st) {
		const doc = st.ctx.document, card = st.card;
		const height = Math.max(MIN_HEIGHT, Number(card.height) || DEFAULT_HEIGHT);
		return {
			document: doc, width: WIDTH, height: height, pad: PAD,
			svg: (host, label) => {
				const s = svgNode(doc, "svg", "jc-chart-svg", {
					viewBox: "0 0 " + WIDTH + " " + height, role: isClickable(card) ? "group" : "img",
					preserveAspectRatio: "xMidYMid meet"
				});
				if (label) s.setAttribute("aria-label", label);
				else s.setAttribute("aria-labelledby", st.caption.id);
				host.appendChild(s);
				return s;
			},
			node: (parent, tag, cls, attrs) => {
				const n = svgNode(doc, tag, cls, attrs);
				parent.appendChild(n);
				return n;
			},
			html: (tag, cls) => htmlNode(doc, tag, cls),
			scaleLinear: scaleLinear, niceMax: niceMax, ticks: ticks, tone: tone,
			fmt: v => formatValue(card, v),
			fmtX: v => formatX(card, v),
			legend: items => paintLegend(st, items),
			click: clickHandler(st),
			clickable: isClickable(card),
			fail: msg => { throw new Error(msg); }
		};
	}

	function fillTable(st) {
		const doc = st.ctx.document, card = st.card, rows = st.rows;
		clear(st.table);
		const cols = columns(card, rows);
		const thead = htmlNode(doc, "thead"), hr = htmlNode(doc, "tr");
		for (const c of cols) {
			const th = htmlNode(doc, "th");
			th.setAttribute("scope", "col");
			th.textContent = c.label;
			hr.appendChild(th);
		}
		thead.appendChild(hr);
		const tbody = htmlNode(doc, "tbody");
		for (const r of rows) {
			const tr = htmlNode(doc, "tr");
			for (const c of cols) {
				const td = htmlNode(doc, "td");
				td.textContent = c.x ? formatX(card, r[c.field]) : formatValue(card, r[c.field]);
				tr.appendChild(td);
			}
			tbody.appendChild(tr);
		}
		st.table.appendChild(thead);
		st.table.appendChild(tbody);
	}

	function destroyInst(st) {
		if (st.inst && typeof st.inst.destroy === "function") {
			try { st.inst.destroy(); } catch (e) { log(e); }
		}
		st.inst = null;
	}

	// Draws the figure (or the empty state).  Returns a reported error, or null.
	function draw(st) {
		const doc = st.ctx.document, card = st.card, rows = st.rows;
		destroyInst(st);
		clear(st.body);
		st.fig = null;
		st.caption = null;
		st.drawn = true;
		if (!rows.length) {
			const p = htmlNode(doc, "p", "jc-chart-empty");
			p.setAttribute("role", "status");
			p.textContent = typeof card.emptyText === "string" ? card.emptyText : "No data.";
			st.body.appendChild(p);
			return null;
		}
		const miss = missingField(card, rows);
		if (miss) return report(st, chartError("E-JS-63", card.id, miss));
		const fig = htmlNode(doc, "figure", "jc-chart jc-chart--" + card.kind + (card.legend === "right" ? " jc-chart--legend-right" : ""));
		const cap = htmlNode(doc, "figcaption", "jc-chart-caption");
		cap.id = "jc-chart-caption-" + card.id;
		cap.textContent = summary(card, rows);
		const plot = htmlNode(doc, "div", "jc-chart-plot");
		const legend = htmlNode(doc, "div", "jc-chart-legend");
		if (card.legend === "none") legend.hidden = true;
		const toggle = htmlNode(doc, "button", "jc-chart-toggle");
		toggle.setAttribute("type", "button");
		toggle.setAttribute("aria-expanded", "false");
		toggle.textContent = "Show data";
		const box = htmlNode(doc, "div", "jc-chart-table-box");
		box.hidden = true;
		const table = htmlNode(doc, "table", "jc-chart-table");
		box.appendChild(table);
		toggle.addEventListener("click", () => {
			const open = toggle.getAttribute("aria-expanded") !== "true";
			toggle.setAttribute("aria-expanded", String(open));
			toggle.textContent = open ? "Hide data" : "Show data";
			box.hidden = !open;
		});
		fig.appendChild(cap);
		fig.appendChild(plot);
		fig.appendChild(legend);
		fig.appendChild(toggle);
		fig.appendChild(box);
		st.body.appendChild(fig);
		st.fig = fig;
		st.caption = cap;
		st.legend = legend;
		st.table = table;
		fillTable(st);
		try {
			st.inst = kinds[card.kind](plot, rows, card, chartContext(st)) || null;
		} catch (e) {
			return report(st, chartError("E-JS-80", card.id, card.kind, e && e.message));
		}
		return null;
	}

	function load(st) {
		const card = st.card;
		if (Array.isArray(card.data)) return Promise.resolve(card.data.slice());
		return st.ctx.fetchJson(card.src).then(d => {
			if (Array.isArray(d)) return d;
			if (d && Array.isArray(d.rows)) return d.rows;
			throw new Error("the response is not an array of rows");
		});
	}

	function startPolling(st) {
		const card = st.card;
		if (st.poll || card.refreshMs == null || Array.isArray(card.data)) return;
		let ms = Number(card.refreshMs);
		if (!(ms >= MIN_REFRESH_MS)) {
			log(chartError("E-JS-75", card.id, card.refreshMs, MIN_REFRESH_MS));
			ms = MIN_REFRESH_MS;
		}
		st.poll = st.ctx.every(ms, () => { st.ctx.refresh(); });
	}

	function stopPolling(st) {
		if (st.poll) {
			st.poll.stop();
			st.poll = null;
		}
	}

	// A refresh that cannot draw fails the card the way a first render does: no stale styling over the error, no more
	// polling, and a cardReported rejection so the shell fires juneau:card-failed without a page-level E-JS-8.
	function refreshFailed(st, e) {
		setStale(st, null);
		stopPolling(st);
		destroyInst(st);
		throw e;
	}

	function setStale(st, e) {
		if (e) {
			st.body.classList.add("jc-chart--stale");
			st.body.setAttribute("title", "Showing the last data; the refresh failed: " + (e.message || e));
		} else {
			st.body.classList.remove("jc-chart--stale");
			st.body.removeAttribute("title");
		}
	}

	function start(st) {
		st.waiting = false;
		const card = st.card;
		if (!Array.isArray(card.data)) st.ctx.paintLoading(st.body);
		return load(st).then(rows => {
			if (states.get(st.el) !== st) return;
			st.rows = rows;
			const err = draw(st);
			if (err) throw err;
			startPolling(st);
		}, e => {
			if (st.ctx.isAbort(e) || states.get(st.el) !== st) return;
			throw report(st, chartError("E-JS-62", card.id, e && e.message));
		});
	}

	function wait(st) {
		st.waiting = true;
		waiting.push(st);
		st.ctx.paintLoading(st.body);
		st.ctx.onReady(() => {
			if (!st.waiting || (st.ctx.signal && st.ctx.signal.aborted)) return;
			report(st, chartError("E-JS-60", st.card.id, st.card.kind, Object.keys(kinds).sort().join(", ")));
		});
	}

	function teardown(el) {
		const st = states.get(el);
		if (!st) return;
		states.delete(el);
		st.waiting = false;
		const i = waiting.indexOf(st);
		if (i >= 0) waiting.splice(i, 1);
		stopPolling(st);
		destroyInst(st);
	}

	function aliasOnConsole() {
		const jc = window.JuneauConsole;
		if (jc && jc.charts !== api) {
			try { jc.charts = api; } catch (e) { /* a frozen JuneauConsole keeps only window.JuneauCharts */ }
		}
	}

	function render(card, el, ctx) {
		teardown(el);
		const doc = ctx.document;
		for (const c of Array.from(el.childNodes))
			if (!(c.classList && c.classList.contains("jc-card-title"))) el.removeChild(c);
		const body = htmlNode(doc, "div", "jc-card-body jc-chart-body");
		el.appendChild(body);
		const st = { card: card, ctx: ctx, el: el, body: body, rows: [], poll: null, inst: null, fig: null, caption: null, drawn: false, waiting: false };
		states.set(el, st);
		ctx.setApi({ rows: () => st.rows.slice(), refresh: () => ctx.refresh() });
		aliasOnConsole();
		if (!kinds[card.kind]) {
			wait(st);
			return undefined;
		}
		return start(st);
	}

	function refresh(card, el, ctx) {
		const st = states.get(el);
		if (!st || st.waiting || !st.drawn || st.card.src !== card.src || st.card.kind !== card.kind) return render(card, el, ctx);
		st.card = card;
		return load(st).then(rows => {
			if (states.get(el) !== st) return;
			st.rows = rows;
			if (st.inst && typeof st.inst.update === "function" && st.caption && rows.length && !missingField(card, rows)) {
				try {
					st.inst.update(rows);
				} catch (e) {
					refreshFailed(st, report(st, chartError("E-JS-80", card.id, card.kind, e && e.message)));
				}
				st.caption.textContent = summary(card, rows);
				fillTable(st);
			} else {
				const err = draw(st);
				if (err) refreshFailed(st, err);
			}
			setStale(st, null);
		}, e => {
			if (ctx.isAbort(e)) return;
			log(chartError("E-JS-62", card.id, e && e.message));
			setStale(st, e);
		});
	}

	function destroy(card, el) {
		teardown(el);
	}

	function cartesian(isBar) {
		return function (plot, rows0, card, c) {
			const series = seriesOf(card), xf = xField(card);
			const horizontal = isBar && card.orientation === "horizontal";
			let hits = [], cur = 0;    // the previous paint's hit rects and the roving-tabindex index, kept across repaints
			function paint(rows) {
				const refocus = hits.indexOf(c.document.activeElement) >= 0;
				hits = [];
				clear(plot);
				const svg = c.svg(plot);
				const W = c.width, H = c.height, P = c.pad, n = rows.length;
				let hi = 0, lo = 0;
				for (const r of rows) {
					if (isBar && card.stacked) {
						let pos = 0, neg = 0;
						for (const s of series) {
							const v = num(r[s.field]);
							if (v == null) continue;
							if (v > 0) pos += v; else neg += v;
						}
						hi = Math.max(hi, pos);
						lo = Math.min(lo, neg);
					} else {
						for (const s of series) {
							const v = num(r[s.field]);
							if (v == null) continue;
							hi = Math.max(hi, v);
							lo = Math.min(lo, v);
						}
					}
				}
				const top = niceMax(hi);
				const bottom = card.y && typeof card.y.min === "number" ? Math.min(card.y.min, lo) : (lo < 0 ? -niceMax(-lo) : 0);
				const x0 = P.left, x1 = W - P.right, y0 = P.top, y1 = H - P.bottom;
				const val = horizontal ? c.scaleLinear(bottom, top, x0, x1) : c.scaleLinear(bottom, top, y1, y0);
				const band = (horizontal ? (y1 - y0) : (x1 - x0)) / Math.max(n, 1);
				const cat = i => (horizontal ? y0 : x0) + band * (i + 0.5);

				const grid = c.node(svg, "g", "jc-chart-grid");
				for (const t of c.ticks(bottom, top)) {
					const p = round(val(t));
					if (horizontal) c.node(grid, "line", "jc-chart-gridline", { x1: p, x2: p, y1: y0, y2: y1 });
					else c.node(grid, "line", "jc-chart-gridline", { x1: x0, x2: x1, y1: p, y2: p });
					const lbl = c.node(grid, "text", "jc-chart-tick",
						horizontal ? { x: p, y: y1 + 16, "text-anchor": "middle" } : { x: x0 - 6, y: p + 4, "text-anchor": "end" });
					lbl.textContent = c.fmt(t);
				}
				const axis = c.node(svg, "g", "jc-chart-axis");
				const step = Math.max(1, Math.ceil(n / MAX_X_LABELS));
				rows.forEach((r, i) => {
					if (i % step) return;
					const t = c.node(axis, "text", "jc-chart-xlabel",
						horizontal ? { x: x0 - 6, y: round(cat(i)) + 4, "text-anchor": "end" } : { x: round(cat(i)), y: y1 + 16, "text-anchor": "middle" });
					t.textContent = c.fmtX(r[xf]);
				});

				const pointLabel = r => c.fmtX(r[xf]) + ": " + series.map(s => (s.label || s.field) + " " + c.fmt(r[s.field])).join(", ");
				const hitLayer = () => {
					const g = c.node(svg, "g", "jc-chart-hits");
					rows.forEach((r, i) => {
						const a = horizontal
							? { x: x0, y: round(cat(i) - band / 2), width: x1 - x0, height: round(band) }
							: { x: round(cat(i) - band / 2), y: y0, width: round(band), height: y1 - y0 };
						const h = c.node(g, "rect", "jc-chart-hit", a);
						hits.push(h);
						h.addEventListener("click", () => c.click(r, null));
						if (!c.clickable) return;
						h.setAttribute("tabindex", i === Math.min(cur, n - 1) ? "0" : "-1");
						h.setAttribute("role", "button");
						h.setAttribute("aria-label", pointLabel(r));
						h.addEventListener("keydown", ev => {
							const k = ev.key;
							if (k === "Enter" || k === " ") {
								if (ev.preventDefault) ev.preventDefault();
								c.click(r, null);
								return;
							}
							let j = -1;
							if (k === "ArrowRight" || k === "ArrowDown") j = Math.min(hits.length - 1, i + 1);
							else if (k === "ArrowLeft" || k === "ArrowUp") j = Math.max(0, i - 1);
							if (j < 0 || j === i) return;
							if (ev.preventDefault) ev.preventDefault();
							hits[i].setAttribute("tabindex", "-1");
							hits[j].setAttribute("tabindex", "0");
							cur = j;
							hits[j].focus();
						});
					});
					if (refocus && c.clickable && hits.length) hits[Math.min(cur, hits.length - 1)].focus();
				};

				if (isBar) {
					hitLayer();
					const g = c.node(svg, "g", "jc-chart-bars");
					const k = series.length;
					rows.forEach((r, i) => {
						let pos = 0, neg = 0;
						series.forEach((s, si) => {
							const v = num(r[s.field]);
							if (v == null) return;
							let a, b;
							if (card.stacked) {
								if (v >= 0) { a = pos; b = pos + v; pos = b; } else { a = neg + v; b = neg; neg = a; }
							} else {
								a = Math.min(0, v);
								b = Math.max(0, v);
							}
							const thick = card.stacked ? band * 0.7 : band * 0.7 / k;
							const off = card.stacked ? -band * 0.35 : -band * 0.35 + thick * si;
							const c0 = round(cat(i) + off);
							const attrs = horizontal
								? { x: round(val(a)), y: c0, width: round(Math.max(0, val(b) - val(a))), height: round(thick) }
								: { x: c0, y: round(val(b)), width: round(thick), height: round(Math.max(0, val(a) - val(b))) };
							const bar = c.node(g, "rect", "jc-chart-bar", attrs);
							bar.style.fill = c.tone(s, si);
							bar.addEventListener("click", () => c.click(r, s.field));
						});
					});
				} else {
					const g = c.node(svg, "g", "jc-chart-lines");
					series.forEach((s, si) => {
						let d = "", pen = false;
						rows.forEach((r, i) => {
							const v = num(r[s.field]);
							if (v == null) { pen = false; return; }
							d += (pen ? "L" : "M") + round(cat(i)) + " " + round(val(v)) + " ";
							pen = true;
						});
						const path = c.node(g, "path", "jc-chart-line", { d: d.trim(), fill: "none" });
						path.style.stroke = c.tone(s, si);
						if (n <= MAX_MARKERS) {
							rows.forEach((r, i) => {
								const v = num(r[s.field]);
								if (v == null) return;
								const m = c.node(g, "circle", "jc-chart-mark", { cx: round(cat(i)), cy: round(val(v)), r: 3 });
								m.style.fill = c.tone(s, si);
							});
						}
					});
					hitLayer();
				}
				c.legend(series.map((s, si) => ({ label: s.label || s.field, color: c.tone(s, si) })));
			}
			paint(rows0);
			return { update: paint, destroy: () => clear(plot) };
		};
	}

	function fold(slices) {
		const total = slices.reduce((a, s) => a + s.value, 0);
		if (!(total > 0)) return slices;
		const small = slices.filter(s => s.value / total < FOLD_BELOW);
		if (small.length < 2) return slices;
		const sum = small.reduce((a, s) => a + s.value, 0);
		return slices.filter(s => small.indexOf(s) < 0).concat([{ row: null, label: "Other", value: sum }]);
	}

	function arc(cx, cy, R, r0, a0, a1) {
		const pt = (r, a) => round(cx + r * Math.cos(a)) + " " + round(cy + r * Math.sin(a));
		if (a1 - a0 >= Math.PI * 2 - 1e-6) {
			return "M" + pt(R, 0) + " A" + R + " " + R + " 0 1 1 " + pt(R, Math.PI) + " A" + R + " " + R + " 0 1 1 " + pt(R, 0)
				+ " M" + pt(r0, 0) + " A" + r0 + " " + r0 + " 0 1 0 " + pt(r0, Math.PI) + " A" + r0 + " " + r0 + " 0 1 0 " + pt(r0, 0) + " Z";
		}
		const large = a1 - a0 > Math.PI ? 1 : 0;
		return "M" + pt(R, a0) + " A" + R + " " + R + " 0 " + large + " 1 " + pt(R, a1)
			+ " L" + pt(r0, a1) + " A" + r0 + " " + r0 + " 0 " + large + " 0 " + pt(r0, a0) + " Z";
	}

	function donut(plot, rows0, card, c) {
		function paint(rows) {
			clear(plot);
			const svg = c.svg(plot);
			const slices = fold(rows.map(r => ({
				row: r,
				label: r[card.label] == null ? "" : String(r[card.label]),
				value: Math.max(0, num(r[card.value]) || 0)
			})));
			for (const s of slices) {
				if (s.row === null) {
					s.other = true;
					s.row = {};
					s.row[card.label] = s.label;
					s.row[card.value] = s.value;
				}
			}
			const total = slices.reduce((a, s) => a + s.value, 0);
			const cx = c.width / 2, cy = c.height / 2;
			const R = Math.min(c.height / 2 - 8, 110), r0 = round(R * 0.6);
			const g = c.node(svg, "g", "jc-chart-slices");
			let a = -Math.PI / 2;
			slices.forEach((s, i) => {
				const sweep = total > 0 ? s.value / total * Math.PI * 2 : 0;
				if (sweep <= 0) return;
				const p = c.node(g, "path", "jc-chart-slice", { d: arc(cx, cy, R, r0, a, a + sweep), "fill-rule": "evenodd" });
				p.style.fill = c.tone(null, i);
				a += sweep;
				// The folded Other slice stands for several rows, so there is no single row to click through to.
				if (s.other) return;
				p.addEventListener("click", () => c.click(s.row, null));
				if (!c.clickable) return;
				p.setAttribute("tabindex", "0");
				p.setAttribute("role", "button");
				p.setAttribute("aria-label", s.label + ": " + c.fmt(s.value));
				p.addEventListener("keydown", ev => {
					if (ev.key !== "Enter" && ev.key !== " ") return;
					if (ev.preventDefault) ev.preventDefault();
					c.click(s.row, null);
				});
			});
			if (card.center != null) {
				const t = c.node(svg, "text", "jc-chart-center", { x: cx, y: cy + 6, "text-anchor": "middle", "pointer-events": "none" });
				t.textContent = card.center === "total" ? c.fmt(total) : String(card.center);
			}
			c.legend(slices.map((s, i) => ({ label: s.label, color: c.tone(null, i) })));
		}
		paint(rows0);
		return { update: paint, destroy: () => clear(plot) };
	}

	function register(kind, renderer) {
		if (typeof kind !== "string" || !KIND_RE.test(kind))
			throw new TypeError("JuneauCharts.register: kind must match " + KIND_RE + "; got '" + kind + "'");
		if (typeof renderer !== "function")
			throw new TypeError("JuneauCharts.register: the renderer for '" + kind + "' must be a function");
		if (kinds[kind]) {
			const e = chartError("E-JS-64", kind);
			log(e);
			throw e;
		}
		kinds[kind] = renderer;
		for (const st of waiting.slice()) {
			if (st.card.kind !== kind) continue;
			waiting.splice(waiting.indexOf(st), 1);
			if ((st.ctx.signal && st.ctx.signal.aborted) || states.get(st.el) !== st) continue;
			start(st).catch(() => { /* already painted and logged */ });
		}
	}

	function registerEntry(entry) {
		if (!Array.isArray(entry) || entry.length !== 2) {
			log(new TypeError("JuneauConsoleCharts entries are [kind, renderer] pairs"));
			return;
		}
		try {
			register(entry[0], entry[1]);
		} catch (e) {
			if (!e.code) log(e);
		}
	}

	kinds.line = cartesian(false);
	kinds.bar = cartesian(true);
	kinds.donut = donut;

	const api = Object.freeze({
		register: register,
		kinds: () => Object.keys(kinds).sort(),
		version: "1"
	});
	window.JuneauCharts = api;
	aliasOnConsole();

	const queued = Array.isArray(window.JuneauConsoleCharts) ? window.JuneauConsoleCharts.slice() : [];
	window.JuneauConsoleCharts = { push: registerEntry };
	queued.forEach(registerEntry);

	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["chart", { render: render, refresh: refresh, destroy: destroy }]);
})();
