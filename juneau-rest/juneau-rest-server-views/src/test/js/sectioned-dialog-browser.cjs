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
 * sectioned-dialog-browser.cjs - real-browser prober for a SECTIONED dialog form (FormDef.sections) and its ribbon.
 *
 * Never runs in a default build.  Driven by SectionedDialog_BrowserTest under `mvn -Pjs-tests`; see that class's
 * javadoc and the profile comment in this module's pom.xml.
 *
 *   Usage:  node sectioned-dialog-browser.cjs <page.html>
 *
 * The fixture carries the real stylesheet as well as the real scripts, which matters: the pane rule is
 * `display: flex`, an AUTHOR rule that outranks the UA stylesheet's `[hidden] { display: none }`, so only a real
 * browser with the real CSS can prove an unselected section is actually not rendered rather than merely flagged.
 *
 * What a shim cannot prove and this does: real computed visibility per section, that a value typed into a section
 * survives being switched away from and back, that an inline error stays inside its OWN section, that real Tab order
 * runs strip-then-body and stays inside the trapping layer, and that a real Escape closes the whole DIALOG through
 * the shared layer stack (never merely the section) and restores focus to the invoker.
 *
 * Prints ONE JSON object to stdout; every assertion lives in Java.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const init = NS && NS.init;
	const out = { hasInit: !!init };
	if (!init) return out;

	const tick = () => new Promise(r => setTimeout(r, 0));
	function shown(el) { if (!el) return false; const r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; }
	function drain() { while (init.topLayer()) init.popLayer(); }
	function displayOf(el) { return el ? window.getComputedStyle(el).display : null; }

	function makeRow(rowId) {
		const table = document.createElement('table');
		const tbody = document.createElement('tbody');
		const tr = document.createElement('tr');
		tr.setAttribute('data-juneau-row-id', rowId);
		const td = document.createElement('td');
		td.className = 'juneau-view-actions-cell';
		tr.appendChild(td);
		tbody.appendChild(tr);
		table.appendChild(tbody);
		document.body.appendChild(table);
		return { table: table, tr: tr, td: td };
	}

	const V = init.JUNEAU_DIALOG_FORM_CONTRACT_VERSION;
	const ctx = { viewDef: { rowActions: [{ id: 'esc', present: 'dialog', label: 'Escalate', confirm: 'Escalate?' }] } };
	const action = { id: 'ack', label: 'Acknowledge', endpoint: '/x/ack', method: 'POST', present: 'dialog' };

	function sectionedForm() {
		return {
			title: 'Acknowledge?',
			idempotencyKey: 'key-ack',
			form: {
				contractVersion: V,
				sections: [
					{ id: 'basics', label: 'Basics', fields: [
						{ name: 'title', label: 'Title', type: 'text', required: true, value: '' },
						{ name: 'sev', label: 'Severity', type: 'select', value: 'warning',
							options: [{ value: 'critical', label: 'Critical' }, { value: 'warning', label: 'Warning' }] }
					] },
					{ id: 'advanced', label: 'Advanced', fields: [
						{ name: 'notes', label: 'Notes', type: 'textarea', required: true, value: '' },
						{ name: 'notify', label: 'Notify', type: 'toggle', value: 'true' }
					] }
				]
			}
		};
	}

	function parts() {
		const backdrop = document.querySelector('.juneau-view-dialog-backdrop');
		const wrap = backdrop ? backdrop.querySelector('.juneau-view-dialog-form') : null;
		const strip = wrap ? wrap.querySelector('[data-testid="dialog-sections"]') : null;
		const tabs = strip ? Array.from(strip.querySelectorAll('[role="tab"]')) : [];
		const panes = wrap ? Array.from(wrap.querySelectorAll('[data-juneau-form-section]')) : [];
		return { backdrop: backdrop, wrap: wrap, strip: strip, tabs: tabs, panes: panes };
	}

	// ---- Block A: one visible pane, and the real CSS actually hides the other ----
	{
		const dom = makeRow('INC-1');
		init.showActionDialog(sectionedForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		out.paint = {
			stripVisible: shown(p.strip),
			stripRole: p.strip ? p.strip.getAttribute('role') : null,
			tabCount: p.tabs.length,
			tabLabels: p.tabs.map(t => t.textContent).join(','),
			paneCount: p.panes.length,
			// The real stylesheet's `display: flex` must not defeat `[hidden]`.
			pane0Display: displayOf(p.panes[0]),
			pane1Display: displayOf(p.panes[1]),
			pane0Shown: shown(p.panes[0]),
			pane1Shown: shown(p.panes[1]),
			ariaSelected: p.tabs.map(t => t.getAttribute('aria-selected')).join(','),
			// A single dialog: the ribbon is layout on this surface, not a layer of its own.
			dialogLayers: init.dialogLayerCount(),
			backdropCount: document.querySelectorAll('.juneau-view-dialog-backdrop').length
		};

		// Clicking the second tab really swaps which pane renders.
		p.tabs[1].click();
		await tick();
		out.paint.afterSwitch_pane0Shown = shown(p.panes[0]);
		out.paint.afterSwitch_pane1Shown = shown(p.panes[1]);
		out.paint.afterSwitch_ariaSelected = p.tabs.map(t => t.getAttribute('aria-selected')).join(',');
		out.paint.afterSwitch_dialogLayers = init.dialogLayerCount();
		drain();
	}

	// ---- Block B: values typed into a section survive a round trip through another ----
	{
		const dom = makeRow('INC-2');
		init.showActionDialog(sectionedForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		const title = p.wrap.querySelector('[data-juneau-form-field="title"]');
		const sev = p.wrap.querySelector('[data-juneau-form-field="sev"]');
		title.value = 'Typed in Basics';
		sev.value = 'critical';
		p.tabs[1].click();
		await tick();
		const notes = p.wrap.querySelector('[data-juneau-form-field="notes"]');
		notes.value = 'Typed in Advanced';
		p.wrap.querySelector('[data-juneau-form-field="notify"]').checked = false;
		p.tabs[0].click();
		await tick();
		p.tabs[1].click();
		await tick();
		p.tabs[0].click();
		await tick();
		out.values = {
			titleKept: title.value === 'Typed in Basics',
			sevKept: sev.value === 'critical',
			notesKept: notes.value === 'Typed in Advanced',
			// Same nodes throughout - panes are hidden, never re-painted.
			sameTitleNode: p.wrap.querySelector('[data-juneau-form-field="title"]') === title,
			sameNotesNode: p.wrap.querySelector('[data-juneau-form-field="notes"]') === notes,
			collected: JSON.stringify(init.collectDialogFormFields(p.backdrop))
		};
		drain();
	}

	// ---- Block C: an inline error stays inside its own section; confirm reveals the offending one ----
	{
		const dom = makeRow('INC-3');
		const fetchCalls = [];
		const realFetch = window.fetch;
		window.fetch = function (u, o) {
			fetchCalls.push({ u: u, o: o });
			return Promise.resolve({ ok: true, status: 200, headers: { get: () => null }, text: () => Promise.resolve('{}') });
		};
		const ui = init.showActionDialog(sectionedForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		// Fill only 'basics'; leave the required 'notes' in the HIDDEN 'advanced' section empty, then switch back to
		// 'basics' so the offending control is out of sight when confirm runs.
		p.wrap.querySelector('[data-juneau-form-field="title"]').value = 'Filled';
		p.tabs[1].click();
		await tick();
		p.tabs[0].click();
		await tick();
		dom.table.setAttribute('data-juneau-csrf', 'tok');
		ui.confirmBtn.click();
		await tick(); await tick();
		window.fetch = realFetch;
		const notes = p.wrap.querySelector('[data-juneau-form-field="notes"]');
		const notesErr = p.panes[1].querySelector('[data-juneau-error-for="notes"]');
		const titleErr = p.panes[0].querySelector('[data-juneau-error-for="title"]');
		out.invalid = {
			submitBlocked: fetchCalls.length === 0,
			dialogStillOpen: !!document.querySelector('.juneau-view-dialog-backdrop'),
			notesAriaInvalid: notes.getAttribute('aria-invalid') === 'true',
			// The error is painted in the section that owns the control, and the other section stays clean.
			notesErrorInOwnSection: !!notesErr && notesErr.textContent.length > 0,
			notesErrorNotInOtherSection: !p.panes[0].querySelector('[data-juneau-error-for="notes"]'),
			titleErrorEmpty: !!titleErr && titleErr.textContent === '',
			// Confirm revealed the owning section so the focus target is actually visible.
			offendingSectionRevealed: shown(p.panes[1]) && !shown(p.panes[0]),
			focusOnFirstInvalid: document.activeElement === notes,
			focusIsVisible: shown(document.activeElement),
			ariaSelectedAfterReveal: p.tabs.map(t => t.getAttribute('aria-selected')).join(',')
		};
		drain();
	}

	// ---- Block D: real Tab order is strip-then-body and stays inside the trapping layer ----
	{
		const dom = makeRow('INC-4');
		init.showActionDialog(sectionedForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		function label(n) {
			return n.getAttribute('data-juneau-strip-tab')
				|| n.getAttribute('data-juneau-form-field')
				|| (n.hasAttribute('data-juneau-form-section') ? 'pane:' + n.getAttribute('data-juneau-form-section') : null)
				|| (n.className || n.tagName);
		}
		function inHiddenSection(n) {
			const pane = n.closest('[data-juneau-form-section]');
			return pane != null && pane.hasAttribute('hidden');
		}
		// What a naive selector considers focusable, and what the BROWSER actually walks on Tab: an element inside a
		// `display: none` section has no client rects and is skipped, which is why the real order has to be measured
		// against rendering rather than against the selector.
		const candidates = Array.from(p.backdrop.querySelectorAll(
			'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]'
		)).filter(n => n.getAttribute('tabindex') !== '-1');
		const tabbables = candidates.filter(n => n.getClientRects().length > 0);
		const order = tabbables.map(label);
		out.focus = {
			candidateOrder: candidates.map(label).join(','),
			tabbableOrder: order.join(','),
			// A roving tabindex means exactly ONE ribbon tab is in the sequence.
			stripTabsInSequence: tabbables.filter(n => n.getAttribute('role') === 'tab').length,
			// Nothing inside a hidden section is reachable, and the hidden pane itself is not either.
			hiddenSectionControlsReachable: tabbables.filter(inHiddenSection).length,
			hiddenPanesReachable: tabbables.filter(n => n.hasAttribute('data-juneau-form-section') && n.hasAttribute('hidden')).length,
			// The trap only intervenes at the boundaries, so BOTH boundary elements must themselves be rendered -
			// otherwise a Tab wrap would try to focus something invisible and focus would go nowhere.
			trapFirstIsRendered: candidates.length > 0 && candidates[0].getClientRects().length > 0,
			trapLastIsRendered: candidates.length > 0 && candidates[candidates.length - 1].getClientRects().length > 0,
			stripPrecedesBody: order.indexOf('basics') === 0 && order.indexOf('title') > order.indexOf('basics'),
			focusTrappedIntoDialog: p.backdrop.contains(document.activeElement)
		};
		// Real Tab from the last tabbable wraps back to the first, inside the layer.
		const first = candidates[0], last = candidates[candidates.length - 1];
		if (last && last.focus) last.focus();
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }));
		out.focus.tabWrapsToFirst = document.activeElement === first;
		out.focus.tabKeepsFocusInDialog = p.backdrop.contains(document.activeElement);
		// Real Left/Right on the ribbon move selection and carry focus with them.
		p.tabs[0].focus();
		p.strip.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true, cancelable: true }));
		out.focus.arrowMovesSelection = p.tabs[1].getAttribute('aria-selected') === 'true';
		out.focus.arrowMovesFocus = document.activeElement === p.tabs[1];
		out.focus.arrowRevealsPane = shown(p.panes[1]) && !shown(p.panes[0]);
		drain();
	}

	// ---- Block E: Escape closes the DIALOG (not the section) through the shared stack, and restores focus ----
	{
		const dom = makeRow('INC-5');
		const invoker = document.createElement('button');
		invoker.textContent = 'Open';
		dom.td.appendChild(invoker);
		invoker.focus();
		const before = document.activeElement;
		init.showActionDialog(sectionedForm(), action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		// Switch to the second section first, so Escape has a "close the section" temptation to get wrong.
		p.tabs[1].click();
		await tick();
		out.escape = {
			openedLayers: init.dialogLayerCount(),
			secondSectionShowing: shown(p.panes[1])
		};
		document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
		await tick();
		out.escape.layersAfterEscape = init.dialogLayerCount();
		out.escape.backdropsAfterEscape = document.querySelectorAll('.juneau-view-dialog-backdrop').length;
		out.escape.dialogGone = document.querySelector('.juneau-view-dialog-backdrop') === null;
		out.escape.focusRestoredToInvoker = document.activeElement === before;
		drain();
	}

	// ---- Block F: a type=action inside a pane stacks a real second dialog, and the cap still refuses in-dialog ----
	{
		const dom = makeRow('INC-6');
		const modal = sectionedForm();
		modal.form.sections[1].fields.push({ name: 'esc', label: 'Escalate', type: 'action', actionId: 'esc' });
		init.showActionDialog(modal, action, dom.table, dom.tr, ctx);
		await tick();
		const p = parts();
		p.tabs[1].click();
		await tick();
		const escBtn = p.wrap.querySelector('button[data-juneau-form-field="esc"]');
		out.nested = { cap: init.MAX_DIALOG_DEPTH, before: init.dialogLayerCount(), escVisible: shown(escBtn) };
		escBtn.click();
		await tick();
		out.nested.after = init.dialogLayerCount();
		out.nested.twoBackdrops = document.querySelectorAll('.juneau-view-dialog-backdrop').length;
		// A third is refused INSIDE the current top dialog rather than stacked.
		init.openFormActionDialog('esc', dom.table, dom.tr, ctx);
		await tick();
		out.nested.afterThird = init.dialogLayerCount();
		out.nested.stillTwoBackdrops = document.querySelectorAll('.juneau-view-dialog-backdrop').length;
		const top = init.topLayer();
		out.nested.refusalInTopDialog = !!(top && top.el.querySelector('.juneau-view-dialog-depth-refusal'));
		drain();
	}

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) { process.stderr.write('usage: node sectioned-dialog-browser.cjs <page.html>\n'); process.exit(2); }
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
		const report = await page.evaluate(PROBE);
		report.jsFailures = failures.slice();
		process.stdout.write(JSON.stringify(report, null, 2) + '\n');
	} finally {
		await browser.close();
	}
})().catch(e => { process.stderr.write(String((e && e.stack) || e) + '\n'); process.exit(1); });
