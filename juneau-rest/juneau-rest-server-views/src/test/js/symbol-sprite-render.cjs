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
 * symbol-sprite-render.cjs - opt-in Chromium rasteriser for the icon sprite.
 *
 * Never runs in a default build.  Driven by SymbolSprite_Render_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node symbol-sprite-render.cjs <fixture.html> <request-json>
 *
 * Writes review sheets and a metric report into the request's outputDir, and prints a JSON report on stdout.
 *
 * WHY A REAL BROWSER.  These glyphs are painted at 12px and 16px, where the only question that matters is
 * whether a stroke centreline lands on a pixel centre or straddles two.  That is a rasteriser property: no fake
 * DOM has one, and no source-level review can see it.  The browser here is the one the js-tests profile pins,
 * which is what makes a "before" sheet and an "after" sheet comparable at all.
 *
 * EVERY GLYPH IS PAINTED THROUGH THE REAL REGISTRY.  Host markup comes from juneau-icons.js's own resolveIcon(),
 * so this file never re-spells the <svg><use/></svg> host shape.  The one deliberately unresolvable stem (the
 * request's bogusStem) is built by rewriting a real host's fragment id, so even that case goes through the real
 * markup rather than a hand-written imitation - it exists to prove the ink measurement can return zero.
 *
 * THE METRICS ARE A REPORT.  Nothing here is a threshold, and the Java side must not turn one into an assertion;
 * see that class's javadoc for why.  The four definitions are kept verbatim from the feasibility probe that
 * established them so the two sets of figures stay comparable:
 *
 *   ink       total coverage, sum over the box of (1 - luminance/255); comparable to a pixel count
 *   solid     pixels whose luminance is <= 60 (i.e. essentially full-strength ink)
 *   gradient  mean |luminance(x+1,y) - luminance(x,y)| over the box; higher means crisper edges
 *   mush      share of the box in the ambiguous 60..215 luminance band; higher means smeared strokes
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/** Sheet geometry, in CSS pixels of the composited canvas. */
const MAG = 10;                 // nearest-neighbour magnification factor for the true rasters
const CONTEXT_MAG = 4;          // the UI context strips are wide, so they magnify less
const LABEL_W = 132;
const ROW_PAD = 8;
const INK = '#16325c';
const RULE = '#d8dde6';

/** Builds the markup for one glyph host at a given pixel size, using the registry's own output. */
const RENDER_GLYPH = function (args) {
	const stage = document.getElementById('stage');
	stage.textContent = '';
	const box = document.createElement('div');
	box.id = 'shot';
	box.style.cssText = 'display:inline-block;line-height:0;background:#ffffff;color:' + args.ink;
	box.innerHTML = args.markup;
	const svg = box.querySelector('svg');
	svg.setAttribute('width', String(args.size));
	svg.setAttribute('height', String(args.size));
	svg.style.width = args.size + 'px';
	svg.style.height = args.size + 'px';
	svg.style.fill = 'currentColor';
	stage.appendChild(box);
	return { ok: true };
};

/** Two glyph hosts side by side at one size - the shape a 16px distinguishability check is made in. */
const RENDER_PAIR = function (args) {
	const stage = document.getElementById('stage');
	stage.textContent = '';
	const box = document.createElement('div');
	box.id = 'shot';
	box.style.cssText = 'display:inline-flex;gap:6px;align-items:center;line-height:0;'
		+ 'background:#ffffff;padding:2px;color:' + args.ink;
	args.markups.forEach(function (m) {
		const cell = document.createElement('span');
		cell.style.cssText = 'display:inline-block;line-height:0';
		cell.innerHTML = m;
		const svg = cell.querySelector('svg');
		svg.setAttribute('width', String(args.size));
		svg.setAttribute('height', String(args.size));
		svg.style.width = args.size + 'px';
		svg.style.height = args.size + 'px';
		svg.style.fill = 'currentColor';
		box.appendChild(cell);
	});
	stage.appendChild(box);
	return { ok: true };
};

/**
 * The two real UI contexts, painted with the served stylesheet.
 *
 * The CSS class names are the one thing this harness restates rather than deriving: building these strips from
 * the real emitters would mean booting the whole view runtime to look at eight buttons.  Every glyph inside them
 * still comes from the registry, and the sizes still come from the stylesheet's own custom properties.
 */
const RENDER_CONTEXT = function (args) {
	const stage = document.getElementById('stage');
	stage.textContent = '';
	const wrap = document.createElement('div');
	wrap.id = 'shot';
	wrap.style.cssText = 'display:inline-block;background:#ffffff;padding:6px';

	const btn = function (cls, markup, label) {
		const b = document.createElement('button');
		b.className = cls;
		b.type = 'button';
		b.title = label;
		b.setAttribute('aria-label', label);
		b.innerHTML = markup;
		return b;
	};

	if (args.kind === 'ribbon') {
		const row = document.createElement('div');
		row.className = 'juneau-view-ribbon';
		args.groups.forEach(function (g) {
			const group = document.createElement('div');
			group.className = 'juneau-view-ribbon-group';
			g.forEach(function (item) {
				group.appendChild(btn('juneau-view-ribbon-btn', item.markup, item.name));
			});
			row.appendChild(group);
		});
		wrap.appendChild(row);
	} else {
		const pill = document.createElement('div');
		pill.className = 'juneau-view-pagingpill';
		args.items.forEach(function (item) {
			if (item.kind === 'info') {
				const span = document.createElement('span');
				span.className = 'juneau-view-pagingpill-info';
				span.textContent = item.text;
				pill.appendChild(span);
				return;
			}
			if (item.kind === 'caret') {
				const wrapper = document.createElement('span');
				wrapper.className = 'juneau-view-pagingpill-menuwrap';
				const b = btn('juneau-view-pagingpill-btn juneau-view-pagingpill-menubtn', '', item.name);
				const caret = document.createElement('span');
				caret.className = 'juneau-view-pagingpill-caret';
				caret.innerHTML = item.markup;
				b.textContent = item.text;
				b.appendChild(caret);
				wrapper.appendChild(b);
				pill.appendChild(wrapper);
				return;
			}
			pill.appendChild(btn('juneau-view-pagingpill-btn', item.markup, item.name));
		});
		wrap.appendChild(pill);
	}
	stage.appendChild(wrap);
	return { ok: true };
};

/** Reads the registry the page actually loaded.  A null here means the sprite/registry wiring broke. */
const RESOLVE = function (names) {
	const icons = window.JuneauViews && window.JuneauViews.icons;
	if (!icons || typeof icons.resolveIcon !== 'function')
		throw new Error('juneau-icons.js did not install a resolveIcon');
	const out = {};
	names.forEach(function (n) { out[n] = icons.resolveIcon(n); });
	return out;
};

/** True once the sprite has been parsed and injected by the registry's own loader. */
const SPRITE_READY = function () {
	const sprite = document.getElementById('juneau-symbol-sprite');
	return !!sprite && sprite.querySelectorAll('symbol').length > 0;
};

/**
 * Measures one raster and returns it magnified.  Both happen in the page because a canvas is the only PNG
 * decoder available without adding a dependency, and because the magnification must be nearest-neighbour: a
 * smoothed enlargement would hide the very antialiasing this sheet exists to show.
 */
const MEASURE_AND_MAGNIFY = async function (args) {
	const img = new Image();
	await new Promise(function (resolve, reject) {
		img.onload = resolve;
		img.onerror = function () { reject(new Error('could not decode a raster')); };
		img.src = args.dataUrl;
	});
	const w = img.naturalWidth;
	const h = img.naturalHeight;

	const c = document.createElement('canvas');
	c.width = w;
	c.height = h;
	const ctx = c.getContext('2d', { willReadFrequently: true });
	ctx.drawImage(img, 0, 0);
	const px = ctx.getImageData(0, 0, w, h).data;

	const lum = new Float64Array(w * h);
	for (let i = 0; i < w * h; i++)
		lum[i] = 0.2126 * px[i * 4] + 0.7152 * px[i * 4 + 1] + 0.0722 * px[i * 4 + 2];

	let ink = 0;
	let solid = 0;
	let mush = 0;
	for (let i = 0; i < w * h; i++) {
		ink += 1 - lum[i] / 255;
		if (lum[i] <= 60) solid++;
		else if (lum[i] < 215) mush++;
	}
	let grad = 0;
	let gradN = 0;
	for (let y = 0; y < h; y++)
		for (let x = 0; x + 1 < w; x++, gradN++)
			grad += Math.abs(lum[y * w + x + 1] - lum[y * w + x]);

	const mc = document.createElement('canvas');
	mc.width = w * args.mag;
	mc.height = h * args.mag;
	const mctx = mc.getContext('2d');
	mctx.imageSmoothingEnabled = false;
	mctx.drawImage(img, 0, 0, mc.width, mc.height);

	return {
		width: w,
		height: h,
		ink: Math.round(ink * 100) / 100,
		solid: solid,
		gradient: gradN ? Math.round((grad / gradN) * 1000) / 1000 : 0,
		mush: (w * h) ? Math.round((mush / (w * h)) * 10000) / 10000 : 0,
		magnified: mc.toDataURL('image/png')
	};
};

/** Composites a sheet from already-rendered tiles.  Pure canvas drawing; no measurement happens here. */
const COMPOSE = async function (spec) {
	const load = function (dataUrl) {
		return new Promise(function (resolve, reject) {
			const i = new Image();
			i.onload = function () { resolve(i); };
			i.onerror = function () { reject(new Error('tile failed to decode')); };
			i.src = dataUrl;
		});
	};
	const c = document.createElement('canvas');
	c.width = spec.width;
	c.height = spec.height;
	const ctx = c.getContext('2d');
	ctx.fillStyle = '#ffffff';
	ctx.fillRect(0, 0, c.width, c.height);
	ctx.imageSmoothingEnabled = false;
	ctx.textBaseline = 'alphabetic';

	for (const op of spec.ops) {
		if (op.op === 'text') {
			ctx.fillStyle = op.color;
			ctx.font = op.font;
			ctx.textAlign = op.align || 'left';
			ctx.fillText(op.text, op.x, op.y);
		} else if (op.op === 'rule') {
			ctx.strokeStyle = op.color;
			ctx.lineWidth = 1;
			ctx.beginPath();
			ctx.moveTo(op.x1, op.y1 + 0.5);
			ctx.lineTo(op.x2, op.y1 + 0.5);
			ctx.stroke();
		} else {
			const img = await load(op.dataUrl);
			ctx.drawImage(img, op.x, op.y);
		}
	}
	return c.toDataURL('image/png');
};

function writePng(file, dataUrl) {
	const b64 = dataUrl.slice(dataUrl.indexOf(',') + 1);
	fs.writeFileSync(file, Buffer.from(b64, 'base64'));
}

async function settle(page) {
	await page.evaluate(() => new Promise(requestAnimationFrame));
	await page.evaluate(() => new Promise(requestAnimationFrame));
}

/** Renders whatever RENDER_* built and returns its raster as a data URL. */
async function shoot(page) {
	await settle(page);
	const el = await page.$('#shot');
	if (!el)
		throw new Error('nothing was staged to screenshot');
	const buf = await el.screenshot({ type: 'png' });
	return 'data:image/png;base64,' + buf.toString('base64');
}

(async () => {
	const [fixture, requestJson] = process.argv.slice(2);
	if (!fixture || !requestJson) {
		process.stderr.write('usage: node symbol-sprite-render.cjs <fixture.html> <request-json>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);
	const req = JSON.parse(requestJson);
	fs.mkdirSync(req.outputDir, { recursive: true });

	const sizes = req.sizes.slice().sort((a, b) => a - b);
	const small = req.pillSize;
	const ribbon = req.ribbonSize;

	const browser = await chromium.launch();
	try {
		// deviceScaleFactor 1 on purpose: the whole subject is what one CSS pixel of a 16px glyph looks like.
		const page = await browser.newPage({ viewport: { width: 1400, height: 900 }, deviceScaleFactor: 1 });
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto('file://' + path.resolve(fixture));
		await page.waitForFunction(SPRITE_READY, null, { timeout: 30000 });

		const stems = req.stems.slice();
		const markup = await page.evaluate(RESOLVE, stems);
		for (const s of stems)
			if (!markup[s])
				throw new Error('the icon registry has no host markup for stem "' + s + '"');

		// The unresolvable probe re-uses a real host and rewrites only its fragment id.
		const reference = stems[0];
		markup[req.bogusStem] = markup[reference]
			.replace('#juneau-sym-' + reference, '#juneau-sym-' + req.bogusStem);

		const all = stems.concat([req.bogusStem]);
		const tiles = {};   // tiles[stem][size] = { ...metrics, dataUrl, magnified }
		for (const stem of all) {
			tiles[stem] = {};
			for (const size of sizes) {
				await page.evaluate(RENDER_GLYPH, { markup: markup[stem], size: size, ink: INK });
				const dataUrl = await shoot(page);
				const m = await page.evaluate(MEASURE_AND_MAGNIFY, { dataUrl: dataUrl, mag: MAG });
				m.dataUrl = dataUrl;
				tiles[stem][size] = m;
			}
		}

		//--------------------------------------------------------------------------------------------------------
		// sheet.png - every stem, every size, true raster beside a 10x magnification.
		//--------------------------------------------------------------------------------------------------------
		const colX = {};
		let x = LABEL_W;
		for (const size of sizes) {
			colX[size] = x;
			x += Math.max(34, size + 18);
		}
		const magX16 = x + 10;
		const magX12 = magX16 + ribbon * MAG + 24;
		const rowH = Math.max(ribbon, small) * MAG + ROW_PAD * 2;
		const headerH = 46;
		const sheetOps = [];
		sheetOps.push({ op: 'text', text: 'juneau-symbols.svg - all ' + stems.length + ' stems', x: 12, y: 22,
			font: 'bold 15px sans-serif', color: INK });
		let legend = 'true rasters at ' + sizes.join('px / ') + 'px; then ' + ribbon + 'px and ' + small
			+ 'px magnified ' + MAG + 'x (nearest neighbour)';
		sheetOps.push({ op: 'text', text: legend, x: 12, y: 38, font: '11px sans-serif', color: INK });
		stems.forEach(function (stem, i) {
			const top = headerH + i * rowH;
			sheetOps.push({ op: 'rule', x1: 0, x2: magX12 + small * MAG + 12, y1: top, color: RULE });
			sheetOps.push({ op: 'text', text: stem, x: 12, y: top + rowH / 2 + 4, font: '12px sans-serif', color: INK });
			sizes.forEach(function (size) {
				const t = tiles[stem][size];
				sheetOps.push({ op: 'img', dataUrl: t.dataUrl, x: colX[size], y: top + (rowH - size) / 2 });
			});
			sheetOps.push({ op: 'img', dataUrl: tiles[stem][ribbon].magnified, x: magX16, y: top + ROW_PAD });
			sheetOps.push({ op: 'img', dataUrl: tiles[stem][small].magnified, x: magX12,
				y: top + ROW_PAD + (ribbon - small) * MAG / 2 });
		});
		writePng(path.join(req.outputDir, 'sheet.png'), await page.evaluate(COMPOSE, {
			width: magX12 + small * MAG + 16,
			height: headerH + stems.length * rowH + 8,
			ops: sheetOps
		}));

		//--------------------------------------------------------------------------------------------------------
		// family.png - the drift view: the document family as a set, plus the two named adjacency checks.
		//--------------------------------------------------------------------------------------------------------
		const pairs = [];
		for (const pair of req.adjacencies) {
			await page.evaluate(RENDER_PAIR, { markups: pair.map(p => markup[p]), size: ribbon, ink: INK });
			const dataUrl = await shoot(page);
			const m = await page.evaluate(MEASURE_AND_MAGNIFY, { dataUrl: dataUrl, mag: MAG });
			pairs.push({ names: pair, tile: m, dataUrl: dataUrl });
		}

		const famColW = ribbon * MAG + 40;
		const famOps = [];
		famOps.push({ op: 'text', text: 'document family as a SET (' + req.family.join(', ') + ') at '
			+ ribbon + 'px, magnified ' + MAG + 'x', x: 12, y: 22, font: 'bold 14px sans-serif', color: INK });
		req.family.forEach(function (stem, i) {
			const left = 12 + i * famColW;
			famOps.push({ op: 'text', text: stem, x: left, y: 44, font: '12px sans-serif', color: INK });
			famOps.push({ op: 'img', dataUrl: tiles[stem][ribbon].dataUrl, x: left, y: 52 });
			famOps.push({ op: 'img', dataUrl: tiles[stem][ribbon].magnified, x: left, y: 52 + ribbon + 8 });
		});
		const pairTop = 52 + ribbon + 8 + ribbon * MAG + 28;
		famOps.push({ op: 'text', text: 'named ' + ribbon + 'px distinguishability checks',
			x: 12, y: pairTop - 8, font: 'bold 14px sans-serif', color: INK });
		pairs.forEach(function (p, i) {
			const left = 12 + i * (p.tile.width * MAG + 40);
			famOps.push({ op: 'text', text: p.names.join(' vs '), x: left, y: pairTop + 14,
				font: '12px sans-serif', color: INK });
			famOps.push({ op: 'img', dataUrl: p.dataUrl, x: left, y: pairTop + 22 });
			famOps.push({ op: 'img', dataUrl: p.tile.magnified, x: left, y: pairTop + 22 + p.tile.height + 8 });
		});
		const famPairH = pairs.length ? pairs[0].tile.height * (MAG + 1) + 40 : 0;
		writePng(path.join(req.outputDir, 'family.png'), await page.evaluate(COMPOSE, {
			width: Math.max(req.family.length * famColW + 24, pairs.length * (ribbon * 2 * MAG + 60) + 24),
			height: pairTop + 22 + famPairH + 16,
			ops: famOps
		}));

		//--------------------------------------------------------------------------------------------------------
		// contexts.png - the two places a user actually sees these glyphs.
		//--------------------------------------------------------------------------------------------------------
		const item = function (name) { return { name: name, markup: markup[name] }; };
		const registered = await page.evaluate(RESOLVE,
			['first_page', 'chevron_left', 'chevron_right', 'last_page', 'expand_more']);
		for (const k of Object.keys(registered))
			if (!registered[k])
				throw new Error('the icon registry has no markup for the bundled name "' + k + '"');

		await page.evaluate(RENDER_CONTEXT, {
			kind: 'ribbon',
			groups: [
				['csv', 'spreadsheet', 'pdf', 'copy', 'download'].map(item),
				['toggle_column_search', 'collapse_all', 'columns', 'settings'].map(item),
				['refresh'].map(item)
			]
		});
		const ribbonShot = await shoot(page);
		const ribbonMag = await page.evaluate(MEASURE_AND_MAGNIFY, { dataUrl: ribbonShot, mag: CONTEXT_MAG });

		await page.evaluate(RENDER_CONTEXT, {
			kind: 'pill',
			items: [
				{ kind: 'btn', name: 'first_page', markup: registered.first_page },
				{ kind: 'btn', name: 'chevron_left', markup: registered.chevron_left },
				{ kind: 'info', text: '1-25 of 413' },
				{ kind: 'caret', name: 'page size', text: '25', markup: registered.expand_more },
				{ kind: 'btn', name: 'chevron_right', markup: registered.chevron_right },
				{ kind: 'btn', name: 'last_page', markup: registered.last_page }
			]
		});
		const pillShot = await shoot(page);
		const pillMag = await page.evaluate(MEASURE_AND_MAGNIFY, { dataUrl: pillShot, mag: CONTEXT_MAG });

		const ctxOps = [];
		ctxOps.push({ op: 'text', text: 'ribbon export cluster at ' + ribbon + 'px (real served CSS)',
			x: 12, y: 22, font: 'bold 14px sans-serif', color: INK });
		ctxOps.push({ op: 'img', dataUrl: ribbonShot, x: 12, y: 30 });
		ctxOps.push({ op: 'img', dataUrl: ribbonMag.magnified, x: 12, y: 30 + ribbonMag.height + 10 });
		const pillTop = 30 + ribbonMag.height + 10 + ribbonMag.height * CONTEXT_MAG + 34;
		ctxOps.push({ op: 'text', text: 'paging pill at ' + small
			+ 'px - mirrored chevron and the doubled first/last composition',
			x: 12, y: pillTop - 8, font: 'bold 14px sans-serif', color: INK });
		ctxOps.push({ op: 'img', dataUrl: pillShot, x: 12, y: pillTop });
		ctxOps.push({ op: 'img', dataUrl: pillMag.magnified, x: 12, y: pillTop + pillMag.height + 10 });
		writePng(path.join(req.outputDir, 'contexts.png'), await page.evaluate(COMPOSE, {
			width: Math.max(ribbonMag.width * CONTEXT_MAG, pillMag.width * CONTEXT_MAG) + 24,
			height: pillTop + pillMag.height + 10 + pillMag.height * CONTEXT_MAG + 16,
			ops: ctxOps
		}));

		//--------------------------------------------------------------------------------------------------------
		// metrics.txt - the numbers beside the pictures, plus the set-level spread the family view needs.
		//--------------------------------------------------------------------------------------------------------
		const pad = function (s, n) { return String(s).padEnd(n); };
		const num = function (v, n) { return String(v).padStart(n); };
		const lines = [];
		lines.push('juneau-symbols.svg rasterisation metrics');
		lines.push('');
		lines.push('ink       total coverage: sum over the box of (1 - luminance/255).  Comparable to a pixel count.');
		lines.push('solid     pixels at luminance <= 60 (essentially full-strength ink).');
		lines.push('gradient  mean |luminance(x+1,y) - luminance(x,y)| over the box.  Higher is crisper.');
		lines.push('mush      share of the box in the ambiguous 60..215 luminance band.  Higher is smearier.');
		lines.push('');
		lines.push('These are a REPORT, not thresholds.  They are coupled to the pinned Chromium build, so they are');
		lines.push('reproducible for a given Playwright pin and not stable across a pin bump.  Do not assert on them.');
		lines.push('');
		lines.push(pad('stem', 26) + pad('size', 6) + num('ink', 9) + num('solid', 7) + num('gradient', 10)
			+ num('mush', 8));
		lines.push('-'.repeat(66));
		for (const stem of all) {
			for (const size of sizes) {
				const t = tiles[stem][size];
				lines.push(pad(stem, 26) + pad(size + 'px', 6) + num(t.ink.toFixed(2), 9) + num(t.solid, 7)
					+ num(t.gradient.toFixed(3), 10) + num((t.mush * 100).toFixed(2) + '%', 8));
			}
		}
		lines.push('');
		lines.push('SET-LEVEL SPREAD across the document family (' + req.family.join(', ') + ')');
		lines.push('The number per-glyph review cannot see: how far apart the four are on each metric.');
		lines.push('');
		lines.push(pad('metric', 12) + pad('size', 6) + num('min', 10) + num('max', 10) + num('spread', 10));
		lines.push('-'.repeat(48));
		const spreads = {};
		for (const size of sizes) {
			for (const key of ['ink', 'solid', 'gradient', 'mush']) {
				const vals = req.family.map(function (s) { return tiles[s][size][key]; });
				const lo = Math.min.apply(null, vals);
				const hi = Math.max.apply(null, vals);
				spreads[key + size] = Math.round((hi - lo) * 1000) / 1000;
				lines.push(pad(key, 12) + pad(size + 'px', 6) + num(lo.toFixed(3), 10) + num(hi.toFixed(3), 10)
					+ num((hi - lo).toFixed(3), 10));
			}
		}
		lines.push('');
		fs.writeFileSync(path.join(req.outputDir, 'metrics.txt'), lines.join('\n'));

		//--------------------------------------------------------------------------------------------------------
		// The JSON report the Java side asserts on.
		//--------------------------------------------------------------------------------------------------------
		const glyphs = {};
		for (const stem of all) {
			const g = {};
			for (const size of sizes) {
				const t = tiles[stem][size];
				g['ink' + size] = t.ink;
				g['solid' + size] = t.solid;
				g['gradient' + size] = t.gradient;
				g['mush' + size] = t.mush;
			}
			glyphs[stem] = g;
		}
		process.stdout.write(JSON.stringify({
			stems: stems,
			bogusStem: req.bogusStem,
			sizes: sizes,
			glyphs: glyphs,
			familySpread: spreads,
			outputDir: req.outputDir,
			jsFailures: failures.slice()
		}, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
