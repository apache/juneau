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
 * view-settings-browser.cjs - real-browser prober for the View Settings dialog layout and behaviour: the tab order,
 * the saved-views bar between the header and the tabs, the footer order, the View tab's two panes and its six-column
 * grid (and the stacked three-column fallback at 400px), drag reorder, Find, select-versus-add on the Available list,
 * the restricted-tab dialog, and Rename's restore when deleting the old name fails.
 *
 * Never runs in a default build.  Driven by ViewSettings_BrowserTest under `mvn -Pjs-tests`.
 *
 *   Usage:  node view-settings-browser.cjs <fixture-dir> [<screenshot-dir>]
 *
 * <fixture-dir> holds one page per palette and scheme (<palette>-<scheme>.html).  With a screenshot directory, every
 * page also has the dialog captured on each tab, plus the View tab at 400px.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const GEAR = '.juneau-config-chooser-btn';
const DIALOG = '.juneau-config-dialog';
const PALETTES = ['open', 'gray', 'light-brown', 'light-red', 'red'];
const SCHEMES = ['light', 'dark'];
const TABS = ['view', 'search', 'sort', 'options'];

/** Mounts the gear for one table; {@code tabs} restricts the dialog's tabs. Self-contained for page.evaluate(). */
const MOUNT = (tabs) => {
	const NS = window.JuneauViews;
	localStorage.clear();
	const host = document.getElementById('host');
	const table = document.createElement('table');
	table.dataset.juneauView = 'vsbrowser';
	host.appendChild(table);
	const catalog = [
		{ data: 'name', title: 'Name', search: { type: 'text' }, orderable: true },
		{ data: 'status', title: 'Status', search: { type: 'id' }, orderable: true, formats: ['badge', 'plain'] },
		{ data: 'owner', title: 'Owner', search: { type: 'text' }, orderable: true },
		{ data: 'created', title: 'Created', orderable: true, defaultVisible: false },
		{ data: 'priority', title: 'Priority', orderable: true, defaultVisible: false },
		{ data: 'region', title: 'Region', defaultVisible: false }
	];
	const ctx = { viewDef: { id: 'vsbrowser', columns: catalog, columnConfig: tabs ? { tabs: tabs } : {} } };
	window.__ctx = ctx;
	NS.config.mountChooser(table, ctx);
};

/** Reads the dialog's static layout.  Self-contained for page.evaluate(). */
const LAYOUT = () => {
	const q = (s) => document.querySelector(s);
	const box = (s) => {
		const el = q(s);
		if (!el) return null;
		const r = el.getBoundingClientRect();
		return { left: r.left, right: r.right, top: r.top, bottom: r.bottom, width: r.width };
	};
	const hostEl = q('.juneau-config-body-host');
	const kids = hostEl ? Array.prototype.map.call(hostEl.children, (c) => c.className.split(' ').find((n) => n.startsWith('juneau-config-')) || c.className) : [];
	const actions = q('.juneau-config-dialog .juneau-view-dialog-actions');
	const footer = actions ? Array.prototype.filter.call(actions.children, (c) => c.tagName === 'BUTTON').map((c) => c.textContent) : [];
	const reset = q('.juneau-config-reset');
	const head = q('.juneau-config-col-head');
	const row = q('.juneau-config-col-row');
	const panes = q('.juneau-config-view-panes');
	const dialog = q('.juneau-config-dialog');
	return {
		tabs: Array.prototype.map.call(document.querySelectorAll('.juneau-config-tab'), (b) => b.textContent),
		nav: dialog?.dataset.nav ?? null,
		hostChildren: kids,
		headerBeforeBar: !!(q('.juneau-view-dialog-header') && q('.juneau-config-saved-bar')
			&& q('.juneau-view-dialog-header').getBoundingClientRect().bottom <= q('.juneau-config-saved-bar').getBoundingClientRect().top),
		barButtons: Array.prototype.map.call(document.querySelectorAll('.juneau-config-saved-menu > button'), (b) => b.textContent),
		deleteDanger: !!q('.juneau-config-delete')?.classList.contains('jc-btn-danger'),
		renameDisabled: !!q('.juneau-config-rename')?.disabled,
		deleteDisabled: !!q('.juneau-config-delete')?.disabled,
		footer: footer,
		resetLink: !!reset?.classList.contains('jc-btn-link'),
		// Computed faces against the resolved tokens (a probe element resolves var() to the same rgb() form).
		faces: (() => {
			const tok = (name, prop) => {
				const el = document.createElement('span');
				el.style.setProperty(prop, 'var(' + name + ')');
				dialog.appendChild(el);
				const v = getComputedStyle(el).getPropertyValue(prop);
				el.remove();
				return v;
			};
			const cs = (s) => getComputedStyle(q(s));
			return {
				applyBg: cs('.juneau-config-apply').backgroundColor,
				applyColor: cs('.juneau-config-apply').color,
				primaryBg: tok('--jc-btn-primary-bg', 'background-color'),
				onPrimary: tok('--jc-on-btn-primary', 'color'),
				resetColor: cs('.juneau-config-reset').color,
				resetBg: cs('.juneau-config-reset').backgroundColor,
				link: tok('--jc-link', 'color'),
				cancelBg: cs('.juneau-config-cancel').backgroundColor,
				controlBg: tok('--jc-control-bg', 'background-color')
			};
		})(),
		resetLeftGap: reset && actions ? reset.getBoundingClientRect().left - actions.getBoundingClientRect().left : null,
		resetToCancel: reset ? q('.juneau-config-cancel').getBoundingClientRect().left - reset.getBoundingClientRect().right : null,
		cancelToApply: q('.juneau-config-apply').getBoundingClientRect().left - q('.juneau-config-cancel').getBoundingClientRect().right,
		applyRightGap: actions.getBoundingClientRect().right - q('.juneau-config-apply').getBoundingClientRect().right,
		dialogWidth: dialog.getBoundingClientRect().width,
		viewportWidth: window.innerWidth,
		panesDirection: panes ? getComputedStyle(panes).flexDirection : null,
		availPane: box('.juneau-config-avail-pane'),
		selPane: box('.juneau-config-sel-pane'),
		headTransform: head ? getComputedStyle(head).textTransform : null,
		headColumns: head ? getComputedStyle(head).gridTemplateColumns : null,
		rowColumns: row ? getComputedStyle(row).gridTemplateColumns : null,
		dialogOverflowX: dialog.scrollWidth - dialog.clientWidth
	};
};

const shown = () => Array.prototype.map.call(document.querySelectorAll('.juneau-config-col-row'), (r) => r.dataset.col);
const available = () => Array.prototype.map.call(document.querySelectorAll('.juneau-config-avail'), (r) => r.dataset.col);

async function open(page, tabs) {
	await page.evaluate(MOUNT, tabs || null);
	await page.click(GEAR);
	await page.waitForSelector('.juneau-config-tab[aria-selected="true"]', { timeout: 5000 });
	// The saved-views listing resolves asynchronously; Rename/Delete settle once it has.
	await page.waitForFunction(() => document.querySelector('.juneau-config-view-select')?.options.length >= 1, null, { timeout: 5000 });
}

async function behaviour(page) {
	const out = {};
	out.initialShown = await page.evaluate(shown);
	out.initialAvailable = await page.evaluate(available);

	// Drag Owner onto the top half of Name: it moves before Name.
	await page.dragAndDrop('.juneau-config-col-row[data-col="owner"]', '.juneau-config-col-row[data-col="name"]', { targetPosition: { x: 40, y: 2 } });
	out.afterDrag = await page.evaluate(shown);

	// Find narrows the Available list; clearing it restores the list.
	await page.fill('.juneau-config-find', 'pri');
	out.findPri = await page.evaluate(available);
	await page.fill('.juneau-config-find', 'zzz');
	out.findNone = await page.evaluate(() => document.querySelector('.juneau-config-avail-list .juneau-config-empty-note')?.textContent ?? null);
	await page.fill('.juneau-config-find', '');
	out.findCleared = await page.evaluate(available);

	// A single click only selects; a double click adds; Enter adds the focused row.
	await page.click('.juneau-config-avail[data-col="region"]');
	out.clickSelected = await page.evaluate(() => document.querySelector('.juneau-config-avail[data-col="region"]')?.getAttribute('aria-selected'));
	out.clickShown = await page.evaluate(shown);
	await page.dblclick('.juneau-config-avail[data-col="created"]');
	out.dblclickShown = await page.evaluate(shown);
	await page.focus('.juneau-config-avail[data-col="priority"]');
	await page.keyboard.press('Enter');
	out.enterShown = await page.evaluate(shown);
	out.enterAvailable = await page.evaluate(available);
	return out;
}

async function rename(page) {
	const out = {};
	const status = () => document.querySelector('.juneau-config-status')?.textContent ?? '';
	const state = async () => page.evaluate(async () => {
		const NS = window.JuneauViews;
		const table = document.querySelector('table[data-juneau-view="vsbrowser"]');
		const l = await NS.persistence.list(table);
		return {
			views: l.views.map((v) => v.name).sort(),
			active: l.active,
			select: document.querySelector('.juneau-config-view-select').value,
			renameDisabled: document.querySelector('.juneau-config-rename').disabled,
			deleteDisabled: document.querySelector('.juneau-config-delete').disabled
		};
	});

	await page.click('.juneau-config-saved-more');
	await page.click('.juneau-config-saveas');
	out.saveAsFieldOpen = await page.evaluate(() => !document.querySelector('.juneau-config-name-field').hidden);
	await page.fill('.juneau-config-name-input', 'Mine');
	await page.click('.juneau-config-name-ok');
	await page.waitForFunction(() => document.querySelector('.juneau-config-view-select').value === 'Mine', null, { timeout: 5000 });
	out.afterSaveAs = await state();
	out.saveAsFieldClosed = await page.evaluate(() => document.querySelector('.juneau-config-name-field').hidden);

	// Deleting the old name fails: the new copy is removed again and the old view stays active.
	await page.evaluate(() => {
		const p = window.JuneauViews.persistence;
		window.__realDelete = p['delete'];
		p['delete'] = function (t, n) { return n === 'Mine' ? Promise.reject(new Error('storage is locked')) : window.__realDelete(t, n); };
	});
	await page.click('.juneau-config-rename');
	out.renameFieldValue = await page.evaluate(() => document.querySelector('.juneau-config-name-input').value);
	out.renameOkLabel = await page.evaluate(() => document.querySelector('.juneau-config-name-ok').textContent);
	await page.fill('.juneau-config-name-input', 'Theirs');
	await page.click('.juneau-config-name-ok');
	await page.waitForFunction(() => (document.querySelector('.juneau-config-status')?.textContent || '').startsWith('Rename failed'), null, { timeout: 5000 });
	out.failedStatus = await page.evaluate(status);
	out.afterFailedRename = await state();

	// With delete working again, the rename goes through.
	await page.evaluate(() => { window.JuneauViews.persistence['delete'] = window.__realDelete; });
	await page.fill('.juneau-config-name-input', 'Theirs');
	await page.click('.juneau-config-name-ok');
	await page.waitForFunction(() => document.querySelector('.juneau-config-view-select').value === 'Theirs', null, { timeout: 5000 });
	out.okStatus = await page.evaluate(status);
	out.afterRename = await state();
	return out;
}

async function dragAndDetail(page) {
	const out = {};
	const detail = () => page.evaluate(() => document.querySelector('.juneau-config-detail')?.textContent ?? null);
	out.findPlaceholder = await page.evaluate(() => document.querySelector('.juneau-config-find')?.placeholder ?? null);
	out.detailDefault = await detail();
	// A real mouse drag of an Available row onto the top half of a selected row inserts it before that row.
	await page.dragAndDrop('.juneau-config-avail[data-col="created"]', '.juneau-config-col-row[data-col="status"]', { targetPosition: { x: 40, y: 2 } });
	out.afterRowDrop = await page.evaluate(shown);
	// A real drag onto the bottom half of the last row inserts it after that row.
	const lastH = await page.evaluate(() => document.querySelector('.juneau-config-col-row[data-col="owner"]').getBoundingClientRect().height);
	await page.dragAndDrop('.juneau-config-avail[data-col="priority"]', '.juneau-config-col-row[data-col="owner"]', { targetPosition: { x: 40, y: lastH - 2 } });
	out.afterListDrop = await page.evaluate(shown);
	out.availAfterDrops = await page.evaluate(available);
	await page.click('.juneau-config-avail[data-col="region"]');
	out.detailAvail = await detail();
	await page.click('.juneau-config-col-row[data-col="name"] .juneau-config-col-name');
	out.detailSelected = await detail();
	out.resetIsLink = await page.evaluate(() => document.querySelector('.juneau-config-reset').classList.contains('jc-btn-link'));
	out.tabs = await page.evaluate(() => {
		const sel = document.querySelector('.juneau-config-tab[aria-selected="true"]');
		const un = document.querySelector('.juneau-config-tab[aria-selected="false"]');
		return { selBg: getComputedStyle(sel).backgroundColor, unBg: getComputedStyle(un).backgroundColor,
			dlgBg: getComputedStyle(document.querySelector('.juneau-config-dialog')).backgroundColor,
			wash: (function () { const p = document.createElement('div'); p.style.backgroundColor = 'var(--jc-accent-wash)'; document.querySelector('.juneau-config-dialog').appendChild(p); const c = getComputedStyle(p).backgroundColor; p.remove(); return c; })() };
	});
	return out;
}

async function capture(page, dir, name) {
	const dialog = await page.$(DIALOG);
	for (const t of TABS) {
		await page.click('.juneau-config-tab[data-tab="' + t + '"]');
		await page.evaluate(() => new Promise((r) => requestAnimationFrame(() => requestAnimationFrame(r))));
		await dialog.screenshot({ path: path.join(dir, name + '-' + t + '.png') });
	}
	await page.click('.juneau-config-tab[data-tab="view"]');
}

(async () => {
	const [fixtureDir, shotDir] = process.argv.slice(2);
	if (!fixtureDir) { process.stderr.write('usage: node view-settings-browser.cjs <fixture-dir> [<screenshot-dir>]\n'); process.exit(2); }
	if (shotDir) fs.mkdirSync(shotDir, { recursive: true });
	const url = (palette, scheme) => 'file://' + path.resolve(fixtureDir, palette + '-' + scheme + '.html');
	const browser = await chromium.launch();
	const report = { pages: {} };
	try {
		const run = async (name, width, height, fn) => {
			const context = await browser.newContext({ viewport: { width: width, height: height } });
			const page = await context.newPage();
			const errors = [];
			page.on('pageerror', (e) => errors.push(String(e)));
			page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
			try {
				return await fn(page, errors);
			} finally {
				await context.close();
			}
		};

		report.wide = await run('wide', 1280, 900, async (page, errors) => {
			await page.goto(url('open', 'light'));
			await open(page);
			const r = { layout: await page.evaluate(LAYOUT) };
			r.behaviour = await behaviour(page);
			r.rename = await rename(page);
			r.errors = errors;
			return r;
		});

		report.chooserLight = await run('chooserLight', 1280, 900, async (page, errors) => {
			await page.goto(url('open', 'light'));
			await open(page);
			const r = await dragAndDetail(page);
			if (shotDir) await page.screenshot({ path: path.join(shotDir, 'chooser-open-light.png') });
			r.errors = errors;
			return r;
		});

		report.chooserDark = await run('chooserDark', 1280, 900, async (page, errors) => {
			await page.goto(url('open', 'dark'));
			await open(page);
			const r = { tabs: await page.evaluate(() => {
				const sel = document.querySelector('.juneau-config-tab[aria-selected="true"]');
				const un = document.querySelector('.juneau-config-tab[aria-selected="false"]');
				return { selBg: getComputedStyle(sel).backgroundColor, unBg: getComputedStyle(un).backgroundColor,
					dlgBg: getComputedStyle(document.querySelector('.juneau-config-dialog')).backgroundColor,
			wash: (function () { const p = document.createElement('div'); p.style.backgroundColor = 'var(--jc-accent-wash)'; document.querySelector('.juneau-config-dialog').appendChild(p); const c = getComputedStyle(p).backgroundColor; p.remove(); return c; })() };
			}) };
			if (shotDir) await page.screenshot({ path: path.join(shotDir, 'chooser-open-dark.png') });
			r.errors = errors;
			return r;
		});

		report.narrow = await run('narrow', 400, 800, async (page, errors) => {
			await page.goto(url('open', 'light'));
			await open(page);
			const r = { layout: await page.evaluate(LAYOUT), errors: errors };
			if (shotDir) await page.screenshot({ path: path.join(shotDir, 'open-light-400px-view.png'), fullPage: true });
			return r;
		});

		report.restricted = await run('restricted', 1280, 900, async (page, errors) => {
			await page.goto(url('open', 'light'));
			await open(page, ['search', 'sort']);
			const l = await page.evaluate(LAYOUT);
			return { tabs: l.tabs, hostChildren: l.hostChildren, errors: errors };
		});

		for (const p of PALETTES)
			for (const s of SCHEMES)
				report.pages[p + '-' + s] = await run(p + '-' + s, 1280, 900, async (page, errors) => {
					await page.goto(url(p, s));
					await open(page);
					const r = { nav: await page.evaluate(() => document.querySelector('.juneau-config-dialog')?.dataset.nav ?? null) };
					if (shotDir) {
						await capture(page, shotDir, p + '-' + s);
						await page.setViewportSize({ width: 400, height: 800 });
						await page.evaluate(() => new Promise((res) => requestAnimationFrame(() => requestAnimationFrame(res))));
						await page.screenshot({ path: path.join(shotDir, p + '-' + s + '-400px-view.png'), fullPage: true });
					}
					r.errors = errors;
					return r;
				});
	} finally {
		await browser.close();
	}
	process.stdout.write(JSON.stringify(report, null, 2) + '\n');
})().catch((error) => {
	process.stderr.write(String(error?.stack || error) + '\n');
	process.exit(1);
});
