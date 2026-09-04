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
 * sanitized-html-browser.cjs - opt-in Chromium XSS canary for the SANITIZED_HTML detail-field format
 * (DetailField.Format.SANITIZED_HTML), driven by SanitizedHtml_BrowserTest under `mvn -Pjs-tests`.
 *
 * Never runs in a default build.  ViewsJs_RowDetail_Test's b17* battery already proves the copier's
 * behavior against this module's own regex-fixture DOMParser shim (row-detail.cjs) - a hand-rolled test
 * parser, not a browser HTML parser (see the fidelity note at the top of that file's SANITIZED_HTML
 * section). This harness proves the SAME never-executes guarantee against a REAL browser HTML parser and
 * a real DOM, which that shim cannot stand in for.
 *
 *   Usage:  node sanitized-html-browser.cjs <page.html>
 *
 * Loads the REAL served juneau-views.js in headless Chromium and asserts that a hostile expand-JSON
 * SANITIZED_HTML value is copied through the allowlist (no script/handler executes), while benign markup
 * (a <b> and a <table>) survives as real elements.
 *
 * WORK-J0517 extends this file (beyond the WORK-J0515 XSS/benign smoke pair above) with four more
 * real-browser-parser behaviors a regex shim cannot prove:
 *   - HTML entity decoding (decoded text must stay text, never re-parsed into an element).
 *   - <table> foster-parenting (misplaced non-table-structure content is relocated by the HTML5 tree
 *     builder BEFORE the copier ever walks the tree; each "b0N_..." pair below first inspects the real
 *     parser's own output as ground truth, then checks the copier mirrors that shape rather than assuming
 *     a naive nested read).
 *   - <template> (content lives in an inert DocumentFragment, never the element's own childNodes) and
 *     <noscript> (parsed as REAL child elements, not raw text, because a DOMParser document has no
 *     browsing context and therefore scripting disabled - the opposite of what a scripting-enabled page
 *     would do) - both are SANITIZED_HTML_DROP_TAGS, so both must vanish (tag AND children) regardless.
 *   - Namespace/foreign content (<svg><script>...) - the foreign-content algorithm parses real SVG-namespace
 *     nodes; the copier's tag-name-only allowlist match must not smuggle a foreign-namespace node through
 *     just because its local name collides with an allowed HTML tag (e.g. an SVG-namespace <a>).
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

const PROBE = async function () {
	const NS = window.JuneauViews;
	const I = NS?.init;
	const out = { hasFillSanitizedHtmlSlot: typeof I?.fillSanitizedHtmlSlot === 'function' };
	if (!out.hasFillSanitizedHtmlSlot) return out;

	window.__juneauSanitizedHtmlXss = 0;

	const slot = document.createElement('div');
	slot.dataset.juneauField = 'body';
	slot.setAttribute('data-juneau-field-format', 'sanitizedHtml');
	document.body.appendChild(slot);

	// Hostile half: a real <script> plus an <img onerror> - neither may produce an executable node, and a
	// benign <b> alongside them must still survive (proves the copier, not just an empty result).
	const XSS = '<script>window.__juneauSanitizedHtmlXss=1</script>'
		+ '<img src=x onerror="window.__juneauSanitizedHtmlXss=1">'
		+ '<b>survivor</b>';
	I.fillSanitizedHtmlSlot(slot, XSS);
	out.xssFired = window.__juneauSanitizedHtmlXss === 1;
	out.hasScript = slot.querySelectorAll('script').length > 0;
	out.hasImg = slot.querySelectorAll('img').length > 0;
	out.survivorBold = slot.querySelectorAll('b').length === 1 && slot.textContent.indexOf('survivor') >= 0;

	// Benign half: a <b>/<table> must render as real elements, not escaped text.
	const OK = '<p>Hello <b>world</b></p><table><tr><td>1</td><td>2</td></tr></table>';
	I.fillSanitizedHtmlSlot(slot, OK);
	out.okHasTable = slot.querySelectorAll('table').length === 1;
	out.okHasBold = slot.querySelectorAll('b').length === 1;
	out.okTextHasWorld = slot.textContent.indexOf('world') >= 0;
	out.okTextHasMarkup = slot.textContent.indexOf('<b>') >= 0;

	// -------------------------------------------------------------------------------------------------
	// WORK-J0517: entity decoding - "&lt;script&gt;" in text content must decode to LITERAL text
	// ("<script>alert(1)</script>" as characters), never be re-parsed into an actual element.  The
	// copier cannot "re-parse" this even in principle (it copies the already-decoded text-node value via
	// createTextNode), but the regex shim's naive substring view could plausibly get this wrong, so this
	// proves the real-parser + real-copier pairing behaves as intended end to end.
	// -------------------------------------------------------------------------------------------------
	const ENTITY = '<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>';
	I.fillSanitizedHtmlSlot(slot, ENTITY);
	out.entityNoScriptElement = slot.querySelectorAll('script').length === 0;
	out.entityPSurvived = slot.querySelectorAll('p').length === 1;
	out.entityTextIsLiteral = slot.textContent.indexOf('<script>alert(1)</script>') >= 0;

	// -------------------------------------------------------------------------------------------------
	// WORK-J0517: <table> foster-parenting.  A non-table-structure element opened directly inside <table>
	// (before any <tr>) is relocated ("foster-parented") by the HTML5 tree builder to become a PRECEDING
	// SIBLING of the table, not a descendant - this happens during parsing, before the copier ever sees
	// the tree.  First capture the real parser's own ground-truth shape (mirroring the module's own
	// parseSanitizedHtmlWrap: `new DOMParser().parseFromString("<div>"+html+"</div>", "text/html")`),
	// then check the copier's output mirrors that already-fostered shape (not a naive nested read that
	// would expect <b> nested inside <table>).
	// -------------------------------------------------------------------------------------------------
	const FOSTER = '<table><b>oops</b><tr><td>cell</td></tr></table><span>after</span>';
	const fosterSrcWrap = new DOMParser().parseFromString('<div>' + FOSTER + '</div>', 'text/html').body.firstChild;
	const fosterSrcTags = Array.from(fosterSrcWrap.children).map(e => e.tagName);
	out.fosterSrcOrder = fosterSrcTags;
	out.fosterSrcBBeforeTable = fosterSrcTags.indexOf('B') >= 0 && fosterSrcTags.indexOf('B') < fosterSrcTags.indexOf('TABLE');

	I.fillSanitizedHtmlSlot(slot, FOSTER);
	out.fosterCopyOrder = Array.from(slot.children).map(e => e.tagName);
	out.fosterCopyBBeforeTable = out.fosterCopyOrder.indexOf('B') >= 0
		&& out.fosterCopyOrder.indexOf('B') < out.fosterCopyOrder.indexOf('TABLE');
	out.fosterCopyBNotNestedInTable = slot.querySelector('table b') === null;
	out.fosterCopyCellSurvived = (() => {
		const td = slot.querySelector('table td');
		return !!td && td.textContent === 'cell';
	})();
	out.fosterCopySpanSurvived = slot.textContent.indexOf('after') >= 0;

	// -------------------------------------------------------------------------------------------------
	// WORK-J0517: <template> - its content lives in an INERT DocumentFragment (`.content`), never in the
	// element's own `.childNodes` - a real-parser property the regex shim's substring view cannot model.
	// TEMPLATE is a SANITIZED_HTML_DROP_TAGS entry, so the copier must drop the tag AND its content
	// wholesale (never execute the nested <script>, never leak the nested <b> text) while a sibling
	// element still survives.
	// -------------------------------------------------------------------------------------------------
	window.__juneauSanitizedHtmlTplExec = 0;
	const TPL = '<template><script>window.__juneauSanitizedHtmlTplExec=1</script><b>hiddentplchild</b></template><span>aftertpl</span>';
	const tplSrcEl = new DOMParser().parseFromString('<div>' + TPL + '</div>', 'text/html').body.firstChild.querySelector('template');
	out.templateOwnChildNodesEmpty = !!tplSrcEl && tplSrcEl.childNodes.length === 0;
	out.templateContentHasChildren = !!tplSrcEl && !!tplSrcEl.content && tplSrcEl.content.childNodes.length > 0;

	I.fillSanitizedHtmlSlot(slot, TPL);
	out.templateNotExecuted = window.__juneauSanitizedHtmlTplExec === 0;
	out.templateNoTemplateTag = slot.querySelectorAll('template').length === 0;
	out.templateNoScriptTag = slot.querySelectorAll('script').length === 0;
	out.templateHiddenTextNotLeaked = slot.textContent.indexOf('hiddentplchild') < 0;
	out.templateSiblingSurvived = slot.textContent.indexOf('aftertpl') >= 0;

	// -------------------------------------------------------------------------------------------------
	// WORK-J0517: <noscript> - a DOMParser-created document has no browsing context, so scripting is
	// DISABLED, and per the HTML5 spec that means <noscript> content is parsed as REAL child elements
	// (not raw text the way a scripting-ENABLED page would treat it) - the opposite of the naive
	// assumption. NOSCRIPT is also a SANITIZED_HTML_DROP_TAGS entry, so regardless of that real-element
	// parse, the copier must drop the tag and its (now-real-element) children wholesale.
	// -------------------------------------------------------------------------------------------------
	window.__juneauSanitizedHtmlNsExec = 0;
	const NOS = '<noscript><script>window.__juneauSanitizedHtmlNsExec=1</script><i>hiddensnschild</i></noscript><span>afterns</span>';
	const nsSrcEl = new DOMParser().parseFromString('<div>' + NOS + '</div>', 'text/html').body.firstChild.querySelector('noscript');
	out.noscriptParsedAsRealElements = !!nsSrcEl && !!nsSrcEl.firstElementChild
		&& nsSrcEl.firstElementChild.tagName === 'SCRIPT';

	I.fillSanitizedHtmlSlot(slot, NOS);
	out.noscriptNotExecuted = window.__juneauSanitizedHtmlNsExec === 0;
	out.noscriptNoScriptTag = slot.querySelectorAll('script').length === 0;
	out.noscriptHiddenTextNotLeaked = slot.textContent.indexOf('hiddensnschild') < 0;
	out.noscriptSiblingSurvived = slot.textContent.indexOf('afterns') >= 0;

	// -------------------------------------------------------------------------------------------------
	// WORK-J0517: namespace / foreign content.  <svg> switches the HTML5 tree builder into the
	// foreign-content algorithm, producing REAL SVG-namespace nodes (including an SVG-namespace <script>
	// and <a>, both distinct nodes from their HTML-namespace counterparts even though `tagName` collides
	// case-insensitively).  SVG is a SANITIZED_HTML_DROP_TAGS entry so the whole foreign subtree - script,
	// nested <a href="javascript:...">, and its text - must be dropped wholesale by tag-name match alone,
	// never partially unwrapped/smuggled through by virtue of a same-named allowed HTML tag underneath.
	// -------------------------------------------------------------------------------------------------
	window.__juneauSanitizedHtmlSvgExec = 0;
	const SVGX = '<svg><script>window.__juneauSanitizedHtmlSvgExec=1</script><a href="javascript:alert(1)">clickme</a></svg><span>aftersvg</span>';
	const svgSrcEl = new DOMParser().parseFromString('<div>' + SVGX + '</div>', 'text/html').body.firstChild.querySelector('svg');
	const SVG_NS = 'http://www.w3.org/2000/svg';
	out.svgIsRealForeignNamespace = !!svgSrcEl && svgSrcEl.namespaceURI === SVG_NS;
	out.svgScriptIsForeignNamespace = !!svgSrcEl && !!svgSrcEl.querySelector('script')
		&& svgSrcEl.querySelector('script').namespaceURI === SVG_NS;

	I.fillSanitizedHtmlSlot(slot, SVGX);
	out.svgNotExecuted = window.__juneauSanitizedHtmlSvgExec === 0;
	out.svgNoScriptTag = slot.querySelectorAll('script').length === 0;
	out.svgNoAnchorSmuggled = slot.querySelectorAll('a').length === 0;
	out.svgClickTextNotLeaked = slot.textContent.indexOf('clickme') < 0;
	out.svgSiblingSurvived = slot.textContent.indexOf('aftersvg') >= 0;

	return out;
};

(async () => {
	const [fixture] = process.argv.slice(2);
	if (!fixture) {
		process.stderr.write('usage: node sanitized-html-browser.cjs <page.html>\n');
		process.exit(2);
	}
	if (!fs.existsSync(fixture))
		throw new Error('fixture not found: ' + fixture);

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
})().catch(e => {
	process.stderr.write(String(e?.stack || e) + '\n');
	process.exit(1);
});
