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
 * calendar-browser.cjs - real-browser prober for the calendar's timed/spanning/legend-filter behaviors.
 *
 * Never runs in a default build.  Driven by Calendar_BrowserTest under `mvn -Pjs-tests`; see that class's javadoc
 * and the profile comment in this module's pom.xml.
 *
 *   Usage:  node calendar-browser.cjs <page.html>
 *
 * Loads <page.html> - the REAL served juneau-views.js followed by the REAL served juneau-calendar.js, then a real
 * server-painted CalendarTable - in headless Chromium and measures the three things only a browser can prove:
 * legend toggles are keyboard-reachable and operable with aria-pressed tracking the filter; a spanning bar's href
 * and tooltip work from ANY of its segments; and the "+N more" popover is a layer on the ONE shared stack (portalled,
 * z-stamped, Escape pops it, focus returns to the trigger).  Prints ONE JSON object to stdout; assertions are in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const LEGEND_REVIEW = '.jc-cal-legend-toggle[data-juneau-calendar-cat="review"]';
const REVIEW_CHIPS = '.jc-cal-day-events [data-juneau-calendar-cat="review"]';
const BAR_SEGMENTS = '.jc-cal-bar[data-juneau-calendar-event-id="span1"]';
const MORE = '.jc-cal-more';

/** Counts elements matching a selector; runs inside the page. */
const COUNT = sel => document.querySelectorAll(sel).length;

/** Reads a legend toggle's pressed state plus whether it is the focused element; runs inside the page. */
const LEGEND_STATE = sel => {
	const el = document.querySelector(sel);
	return {
		present: !!el,
		pressed: el ? el.getAttribute('aria-pressed') : null,
		focused: el ? document.activeElement === el : false,
		// A <button> is in the tab order by default; an explicit -1 would take it out of it.
		tabbable: el ? el.tagName === 'BUTTON' && el.getAttribute('tabindex') !== '-1' : false
	};
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node calendar-browser.cjs <page.html>\n'); process.exit(2); }
	if (!fs.existsSync(fixture)) throw new Error('fixture not found: ' + fixture);
	const url = 'file://' + path.resolve(fixture);
	const browser = await chromium.launch();
	try {
		const page = await browser.newPage();
		const failures = [];
		page.on('pageerror', e => failures.push(String(e)));
		page.on('console', m => { if (m.type() === 'error') failures.push(m.text()); });
		await page.goto(url);
		await page.evaluate(() => new Promise(requestAnimationFrame));

		const report = await page.evaluate(() => ({
			hasViews: !!window.JuneauViews?.init,
			hasCalendar: !!window.JuneauCalendar,
			// Recorded by an inline script BETWEEN the two <script> tags: the load-order contract, observed.
			viewsBeforeCalendar: window.__viewsBeforeCalendar === true,
			contract: window.JuneauCalendar?.CONTRACT_VERSION ?? null
		}));

		// ---- Block A: the legend toggle is keyboard-reachable and Enter operates it, aria-pressed tracking state ----
		const chipsBefore = await page.evaluate(COUNT, REVIEW_CHIPS);
		await page.focus(LEGEND_REVIEW);
		const focused = await page.evaluate(LEGEND_STATE, LEGEND_REVIEW);
		await page.keyboard.press('Enter');
		const hidden = await page.evaluate(LEGEND_STATE, LEGEND_REVIEW);
		const chipsHidden = await page.evaluate(COUNT, REVIEW_CHIPS);
		const barsWhileHidden = await page.evaluate(COUNT, BAR_SEGMENTS);
		await page.keyboard.press('Enter');
		const revealed = await page.evaluate(LEGEND_STATE, LEGEND_REVIEW);
		const chipsRevealed = await page.evaluate(COUNT, REVIEW_CHIPS);
		report.legend = {
			reachable: focused.focused,
			tabbable: focused.tabbable,
			pressedInitially: focused.pressed,
			pressedAfterEnter: hidden.pressed,
			pressedAfterSecondEnter: revealed.pressed,
			chipsBefore: chipsBefore,
			chipsWhileHidden: chipsHidden,
			chipsAfterReveal: chipsRevealed,
			// The other category's spanning bar must survive a review-only filter.
			barsWhileHidden: barsWhileHidden
		};

		// ---- Block B: a spanning bar's href and tooltip work from ANY segment (they share the event) ----
		report.bar = await page.evaluate(sel => {
			const segs = Array.prototype.slice.call(document.querySelectorAll(sel));
			return {
				segments: segs.length,
				allAnchors: segs.length > 0 && segs.every(s => s.tagName === 'A'),
				hrefs: segs.map(s => s.getAttribute('href')),
				tooltips: segs.map(s => s.getAttribute('title')),
				// A clipped edge is flagged so the user can tell the bar continues past the week boundary.
				continuations: segs.map(s => (s.className.indexOf('continues-right') >= 0 ? 'r' : '')
					+ (s.className.indexOf('continues-left') >= 0 ? 'l' : ''))
			};
		}, BAR_SEGMENTS);

		// ---- Block C: "+N more" is a layer on the ONE shared stack - portalled, z-stamped, Escape-popped ----
		await page.click(MORE);
		report.popover = await page.evaluate(() => {
			const init = window.JuneauViews.init;
			const top = init.topLayer();
			const pop = document.querySelector('.jc-cal-popover');
			return {
				opened: !!pop,
				onSharedStack: !!(top && pop && top.el === pop),
				kind: top?.kind ?? null,
				lightDismiss: !!top?.lightDismiss,
				portalledToBody: pop ? pop.parentElement === document.body : false,
				positionFixed: pop ? pop.style.position === 'fixed' : false,
				hasZIndex: pop ? !!pop.style.zIndex : false,
				dataLayer: pop?.dataset.juneauLayer ?? null,
				triggerExpanded: document.querySelector('.jc-cal-more')?.getAttribute('aria-expanded') ?? null
			};
		});
		await page.keyboard.press('Escape');
		const after = await page.evaluate(() => {
			const init = window.JuneauViews.init;
			const more = document.querySelector('.jc-cal-more');
			return {
				detached: !document.querySelector('.jc-cal-popover'),
				stackEmpty: init.topLayer() === null,
				focusRestored: !!more && document.activeElement === more,
				triggerCollapsed: more ? more.getAttribute('aria-expanded') : null
			};
		});
		report.escape = after;

		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => { process.stderr.write(String(e?.stack || e) + '\n'); process.exit(1); });
