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
 * detail-field-grid-browser.cjs - opt-in Chromium layout canary for the detail field grid.
 *
 * Never runs in a default build.  Driven by RowDetail_FieldGrid_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node detail-field-grid-browser.cjs <page.html>
 *
 * Everything measured here is unreachable from a fake DOM: a container query, a cascade, generated content, and
 * an overflow measurement all require a real layout engine.
 *
 * THE VIEWPORT NEVER CHANGES.  Only the panel's host element is resized, at a fixed 1200px viewport.  That is
 * the whole point: if `container-type` were declared on :root instead of on .juneau-view-detail-panel, every
 * unnamed @container query would resolve against the 1200px viewport and the column count would never move.  A
 * ladder measured by shrinking the WINDOW would pass either way and would prove nothing.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/** Fills the client-painted slots the way the runtime does, so the grid measures against real content. */
const SETUP = function (longValue) {
	const q = function (sel) { return document.querySelector(sel); };
	q('[data-juneau-field="name"]').textContent = 'alerts-primary';
	q('[data-juneau-field="owner"]').textContent = 'Platform';
	q('[data-juneau-field="region"]').textContent = 'us-east-1';
	q('[data-juneau-field="state"]').textContent = 'Active';
	q('[data-juneau-field="url"]').textContent = longValue;
	// "summary" is the FULL-span field and "missing" is the unset one - both stay empty on purpose.
	return { setup: true };
};

const MEASURE = function (hostWidth) {
	document.getElementById('host').style.width = hostWidth + 'px';
	return { resized: hostWidth };
};

const READ = function () {
	const grid = document.querySelector('#panel .juneau-view-detail-fields');
	const fieldOf = function (data) {
		return document.querySelector('[data-juneau-field="' + data + '"]').closest('.juneau-view-detail-field');
	};
	const panel = document.getElementById('panel');
	const tracks = window.getComputedStyle(grid).gridTemplateColumns.trim().split(/\s+/).map(parseFloat);
	const cols = tracks.length;
	const gridBox = grid.getBoundingClientRect();
	const span = fieldOf('summary').getBoundingClientRect();
	const one = fieldOf('name').getBoundingClientRect();
	const missing = document.querySelector('[data-juneau-field="missing"]');
	const nameField = fieldOf('name');
	const cs = window.getComputedStyle(fieldOf('missing'));
	return {
		cols: cols,
		// A span must never CREATE a track.  The auto-fit candidate this design rejected failed exactly here:
		// it raised the repetition count to satisfy the span and left a ~25px orphan track with a field clipped
		// inside it, so counting tracks alone would have passed it.  The narrowest track is the discriminator.
		narrowestTrackPx: Math.min.apply(null, tracks),
		// A FULL span occupies the grid's whole content width at every step, including the one-column step,
		// where it must be indistinguishable from an unspanned field.
		spanIsFullWidth: Math.abs(span.width - gridBox.width) <= 1,
		spanEqualsOne: Math.abs(span.width - one.width) <= 1,
		// The panel is nested in a table cell; a value that cannot break must not push the grid wider than
		// its own box, or the overflow propagates out to the host table.
		noOverflow: grid.scrollWidth <= grid.clientWidth + 1,
		noPanelOverflow: panel.scrollWidth <= panel.clientWidth + 1,
		// An unset field still occupies its row and still paints its separator.
		emptySeparator: cs.borderBottomStyle + ' ' + cs.borderBottomWidth,
		emptyAfter: window.getComputedStyle(missing, '::after').content,
		// An empty value div contributes no line box of its own, so without a min-height an unset field measures
		// short of a filled one.  Compared value-slot to value-slot rather than block to block: a field BLOCK is
		// stretched by the tallest item in its grid ROW, which says nothing about the empty case.
		emptyValueHeight: missing.getBoundingClientRect().height,
		filledValueHeight: document.querySelector('[data-juneau-field="state"]').getBoundingClientRect().height,
		// The label is the smaller of the pair, inverting what the panel used to do.
		labelPx: parseFloat(window.getComputedStyle(nameField.querySelector('.juneau-view-detail-field-title')).fontSize),
		valuePx: parseFloat(window.getComputedStyle(document.querySelector('[data-juneau-field="name"]')).fontSize),
		// INLINE puts the label beside the value, not above it.
		labelBesideValue: (function () {
			const t = nameField.querySelector('.juneau-view-detail-field-title').getBoundingClientRect();
			const v = document.querySelector('[data-juneau-field="name"]').getBoundingClientRect();
			return t.right <= v.left + 1 && t.top < v.bottom && v.top < t.bottom;
		})()
	};
};

const STACKED_READ = function () {
	const field = document.querySelector('#stacked .juneau-view-detail-field');
	const t = field.querySelector('.juneau-view-detail-field-title').getBoundingClientRect();
	const v = field.querySelector('.juneau-view-detail-field-value').getBoundingClientRect();
	return { stackedLabelAboveValue: t.bottom <= v.top + 1 };
};

const VIEWPORT_ONLY_READ = function () {
	// The container is the panel, so a panel pinned wide keeps its column count no matter how narrow the WINDOW
	// gets.  This is the assertion that fails if container-type ever migrates to :root.
	const grid = document.querySelector('#panel .juneau-view-detail-fields');
	return { colsWithWideHostNarrowWindow: window.getComputedStyle(grid).gridTemplateColumns.trim().split(/\s+/).length };
};

async function settle(page) {
	await page.evaluate(() => new Promise(requestAnimationFrame));
	await page.evaluate(() => new Promise(requestAnimationFrame));
}

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node detail-field-grid-browser.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage({ viewport: { width: 1200, height: 900 } });
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await settle(page);

		const report = {};
		report.setup = await page.evaluate(SETUP, 'https://example.invalid/'
			+ 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
			+ 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'
			+ 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc');

		// The widths G-1 measured on master, re-checked in the delivered tree.
		for (const w of [1200, 900, 700, 520, 380]) {
			await page.evaluate(MEASURE, w);
			await settle(page);
			report['w' + w] = await page.evaluate(READ);
		}

		await page.evaluate(MEASURE, 1200);
		await settle(page);
		Object.assign(report, await page.evaluate(STACKED_READ));

		// Host stays pinned at 1200px while the window shrinks to 400px.
		await page.setViewportSize({ width: 400, height: 900 });
		await settle(page);
		Object.assign(report, await page.evaluate(VIEWPORT_ONLY_READ));

		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
