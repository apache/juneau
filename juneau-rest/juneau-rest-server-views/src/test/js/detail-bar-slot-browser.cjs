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
 * detail-bar-slot-browser.cjs - opt-in Chromium layout canary for the detail-hosted bar slot.
 *
 * Never runs in a default build.  Driven by RowDetail_BarSlot_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node detail-bar-slot-browser.cjs <page.html>
 *
 * Loads the REAL served juneau-views.js + juneau-views.css in headless Chromium, runs buildDetailStrip over a
 * server-shaped 2-section detail panel, and measures the result: the BarBadge must be visible BESIDE the ribbon at a
 * wide viewport, and must never overlap a ribbon tab at a narrow one (it wraps below instead).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const I = NS?.init;
	const out = { hasInit: !!(I && typeof I.buildDetailStrip === 'function') };
	if (!out.hasInit) return out;

	const strip = I.buildDetailStrip(document.getElementById('panel'), null);
	out.stripBuilt = !!strip;
	out.stripTrailed = strip?.dataset.juneauStripTrailed ?? null;

	const slot = document.querySelector('[data-juneau-bar-slot]');
	const badge = document.querySelector('[data-juneau-badge]');
	out.slotTrailsStrip = strip?.nextSibling === slot;

	function rects() {
		const tabs = Array.prototype.map.call(strip.querySelectorAll('[role="tab"]'),
			function (t) { return t.getBoundingClientRect(); });
		return { slot: slot.getBoundingClientRect(), badge: badge.getBoundingClientRect(), tabs: tabs };
	}

	// NOSONAR javascript:S7721 -- must stay nested: page.evaluate(PROBE) ships only PROBE's own source into the
	// browser context, so a helper hoisted to this file's Node module scope would be undefined in the page.
	function overlapsAnyTab(r) {
		return r.tabs.some(function (t) {
			return r.slot.left < t.right && t.left < r.slot.right && r.slot.top < t.bottom && t.top < r.slot.bottom;
		});
	}

	const wide = rects();
	out.wide_badgeVisible = wide.badge.width > 0 && wide.badge.height > 0;
	out.wide_slotVisible = wide.slot.width > 0 && wide.slot.height > 0;
	out.wide_slotBesideRibbon = wide.tabs.length > 0 && wide.slot.left >= wide.tabs.at(-1).right - 1;
	out.wide_sameLineAsRibbon = wide.tabs.length > 0
		&& wide.slot.top < wide.tabs[0].bottom && wide.tabs[0].top < wide.slot.bottom;
	out.wide_noOverlap = !overlapsAnyTab(wide);
	out.wide_computedDisplay = window.getComputedStyle(slot).display;
	return out;
};

const NARROW_PROBE = function () {
	const strip = document.querySelector('[data-juneau-strip-mode="tab"]');
	const slot = document.querySelector('[data-juneau-bar-slot]');
	const badge = document.querySelector('[data-juneau-badge]');
	const tabs = Array.prototype.map.call(strip.querySelectorAll('[role="tab"]'),
		function (t) { return t.getBoundingClientRect(); });
	const s = slot.getBoundingClientRect();
	const overlap = tabs.some(function (t) {
		return s.left < t.right && t.left < s.right && s.top < t.bottom && t.top < s.bottom;
	});
	const b = badge.getBoundingClientRect();
	return {
		narrow_badgeVisible: b.width > 0 && b.height > 0,
		narrow_noOverlap: !overlap,
		narrow_slotWithinViewport: s.right <= window.innerWidth + 1
	};
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node detail-bar-slot-browser.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage({ viewport: { width: 1200, height: 800 } });
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));
		const report = await page.evaluate(PROBE);
		await page.setViewportSize({ width: 320, height: 800 });
		await page.evaluate(() => new Promise(requestAnimationFrame));
		Object.assign(report, await page.evaluate(NARROW_PROBE));
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
