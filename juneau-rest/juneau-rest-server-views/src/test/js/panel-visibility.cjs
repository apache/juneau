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
 * panel-visibility.cjs - real-browser prober for the juneau-pages.js panel-visibility + lazy-init contracts.
 *
 * Never runs in a default build.  It is driven by PagePanelVisibility_BrowserTest, which itself only runs under
 * `mvn -Pjs-tests`; see that class's javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node panel-visibility.cjs <page.html> <attrsJson> <hash>...
 *
 * Loads <page.html> - a self-contained fixture the Java test writes from the REAL PageTable emitter output, the REAL
 * served juneau-views.css, and the REAL served juneau-pages.js - in headless Chromium, then visits each <hash> in
 * turn (the first via a navigation, the rest by assigning location.hash, which exercises the runtime's `hashchange`
 * path rather than re-running its bootstrap).  Prints ONE JSON array to stdout: one report per hash, in order.
 *
 * <attrsJson> is {"panelTab":..,"panelSubtab":..,"tabId":..,"subtabId":..} - every attribute name this probe needs,
 * passed in rather than hard-coded so that this file does not become a THIRD independent spelling of them: the Java
 * caller sources them from PageTable's public constants, and juneau-pages.js's own copy - the one duplication no
 * build can remove - is pinned against those same constants by PageTable_SubtabPanelContract_Test.
 *
 * The active-state class names below are the exception, and are hard-coded, because they have no Java side to be
 * passed from: the emitter never writes them.  That correspondence is pinned instead between this runtime and the
 * stylesheet, by the same test.
 *
 * DIVISION OF LABOUR: this script only OBSERVES; every assertion lives in the Java test.  That keeps the failure
 * diagnostics in JUnit, keeps the expectations next to the emitter they constrain, and keeps this file free of the
 * fixture-specific knowledge that would otherwise have to be maintained in two languages.  Consequently the probe
 * is generic - it enumerates whatever panels the document happens to contain rather than a hard-coded list.
 *
 * WHY A REAL BROWSER: the contract under test is "is this panel actually rendered", which depends on the CSS cascade
 * through ANCESTORS (a sub-panel can carry .jc-active and still be invisible because the panel wrapping it does
 * not).  Emulated DOMs get this wrong - HtmlUnit reports every panel visible for both the fixed and the broken
 * runtime - so a harness built on one would pass against known-broken code, which is worse than no harness at all.
 * Visibility is therefore measured as a non-empty layout box, the same thing a user sees.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

/*
 * Stands in for juneau-views.js, which the fixture deliberately does NOT load: the seam under test is that a panel
 * lazy-inits the view tables it OWNS and no others, so the probe records every initTable(...) call instead of
 * really booting DataTables.  Installed before any page script runs, and juneau-pages.js adopts a pre-existing
 * window.JuneauViews rather than replacing it, so the stub survives.
 */
const INSTRUMENTATION = function () {
	window.__juneauInited = [];
	window.JuneauViews = {
		init: {
			initTable: function (t) { window.__juneauInited.push(t.getAttribute('data-juneau-view')); }
		}
	};
};

/* Reads the state the runtime produced: which panels are really rendered, which tab/sub-tab is marked active. */
const PROBE = function (attrs) {
	const rendered = el => {
		const r = el.getBoundingClientRect();
		return r.width > 0 && r.height > 0;
	};
	const panels = Array.prototype.map.call(document.querySelectorAll('[' + attrs.panelTab + ']'), el => ({
		tab: el.getAttribute(attrs.panelTab),
		subtab: el.getAttribute(attrs.panelSubtab),
		classes: el.getAttribute('class'),
		visible: rendered(el)
	}));
	const attr = (sel, name) => {
		const el = document.querySelector(sel);
		return el ? el.getAttribute(name) : null;
	};
	// Tracked separately from the panels because the sub-tab bar is the one part of a sub-tabbed tab that lives in
	// the OUTER panel: if it is invisible the user has no way to reach the other sub-tabs even if one renders.
	const bars = Array.prototype.map.call(document.querySelectorAll('.jc-subtab-bar'), rendered);
	return {
		panels: panels,
		activeTab: attr('.jc-tab-active', attrs.tabId),
		activeSubtab: attr('.jc-subtab-active', attrs.subtabId),
		visibleSubtabBars: bars.filter(Boolean).length,
		initedViews: window.__juneauInited.slice(),
		errorBanners: Array.prototype.map.call(document.querySelectorAll('.jc-page-error'), el => el.textContent)
	};
};

(async () => {
	const [fixture, attrsJson, ...hashes] = process.argv.slice(2);
	if (!fixture || !attrsJson || !hashes.length) {
		process.stderr.write('usage: node panel-visibility.cjs <page.html> <attrsJson> <hash>...\n');
		process.exit(2);
	}
	const attrs = JSON.parse(attrsJson);
	for (const k of ['panelTab', 'panelSubtab', 'tabId', 'subtabId'])
		if (!attrs[k])
			throw new Error('missing attribute name "' + k + '" in: ' + attrsJson);
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	const reports = [];
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.addInitScript(INSTRUMENTATION);

		for (let i = 0; i < hashes.length; i++) {
			if (i === 0)
				await page.goto(url + hashes[i]);
			else
				await page.evaluate(h => { window.location.hash = h; }, hashes[i]);
			// The runtime reacts synchronously to load/hashchange; one frame is enough for layout to settle.
			await page.evaluate(() => new Promise(requestAnimationFrame));
			const report = await page.evaluate(PROBE, attrs);
			report.hash = hashes[i];
			report.jsFailures = failures.slice();
			reports.push(report);
		}
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(reports, null, 2) + '\n');
})().catch(e => {
	process.stderr.write(String((e && e.stack) || e) + '\n');
	process.exit(1);
});
