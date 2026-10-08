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
 * console-output.cjs - the console-output module's renderer harness (no network): checks, validateLine, rendering,
 * status, scrolling/anchors, earlier lines, tooltip and markers.  Polling and mount live in console-output-poll.cjs.
 *
 * Every case gets a FRESH environment (console-output-env.cjs) and passes by returning without throwing; the report
 * maps the case name to true, or to the failure's stack.
 *
 *   Usage:  node console-output.cjs <renders> <views> <regions> <helpers> <console-output> <vectors.json>
 */
'use strict';

const path = require('node:path');
const E = require(path.join(__dirname, 'console-output-env.cjs'));

const argv = process.argv.slice(2);
if (argv.length < 6) {
	process.stderr.write('usage: console-output.cjs <renders> <views> <regions> <helpers> <console-output> <vectors>\n');
	process.exit(2);
}

const cases = [];
function test(name, fn) { cases.push({ name: name, fn: fn }); }
function expect(cond, detail) {
	if (!cond) throw new Error(typeof detail === 'string' ? detail : JSON.stringify(detail));
}
function same(a, b) { return JSON.stringify(a) === JSON.stringify(b); }
function expectSame(actual, expected, what) {
	expect(same(actual, expected), (what || 'value') + ': expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual));
}
function codes(problems) { return problems.map(function (p) { return p.code + (p.kind ? ':' + p.kind : ''); }); }

// @cases:checks

test('chk01_vectors', function (t) {
	const CO = t.CO;
	const groups = { color: CO.isSafeColor, href: CO.isSafeLineHref, imageSrc: CO.isSafeLineImageSrc };
	const bad = [];
	for (const g of Object.keys(groups)) {
		for (const s of t.vectors[g].accept) if (groups[g](s) !== true) bad.push(g + ' should accept ' + JSON.stringify(s));
		for (const s of t.vectors[g].reject) if (groups[g](s) !== false) bad.push(g + ' should reject ' + JSON.stringify(s));
	}
	// Icon names have no exported predicate; the grammar is observable through validateLine keeping or removing ui.icon.
	for (const s of t.vectors.icon.accept)
		if (CO.validateLine({ text: '', ui: { icon: s } }).line.ui?.icon !== s) bad.push('icon should accept ' + JSON.stringify(s));
	for (const s of t.vectors.icon.reject)
		if (CO.validateLine({ text: '', ui: { icon: s } }).line.ui?.icon !== undefined) bad.push('icon should reject ' + JSON.stringify(s));
	for (const v of [null, undefined, 42, {}, []])
		if (CO.isSafeColor(v) || CO.isSafeLineHref(v) || CO.isSafeLineImageSrc(v)) bad.push('non-string accepted: ' + String(v));
	expect(bad.length === 0, bad.join('\n'));
});

test('chk02_rgbWhitespaceIsAsciiOnly', function (t) {
	// Java's \s is [ \t\n\x0B\f\r]; JS's \s also matches NBSP and U+2000-series spaces. The two must agree.
	expect(t.CO.isSafeColor('rgb(\t1,\n2,\f3\r)') === true, 'ASCII whitespace');
	expect(t.CO.isSafeColor('rgb( 1,2,3)') === false, 'NBSP must be rejected');
	expect(t.CO.isSafeColor('rgb(1, 2,3)') === false, 'EM SPACE must be rejected');
});

test('chk03_structuralDrops', function (t) {
	const V = t.CO.validateLine;
	const inputs = [
		null, 'text', 42, [],
		{}, { text: 'a', frags: [{ text: 'b' }] },
		{ text: 7 },
		{ text: 'a', n: 0 }, { text: 'a', n: 1.5 }, { text: 'a', n: '3' },
		{ text: 'a', level: 'DEBUG' }, { text: 'a', level: 'info' },
		{ frags: 'x' }, { frags: [] }, { frags: new Array(513).fill({ text: 'x' }) },
		{ frags: [{ block: true, text: 'x' }] }, { frags: [{ block: true, bold: true }] },
		{ frags: [{ text: 'x', tooltip: 't' }] }, { frags: [{ text: 'x', href: '#a' }] }, { frags: [{ text: 'x', label: 'l' }] },
		{ frags: [{ text: 'x', block: 'yes' }] }, { frags: [{ style: 'muted' }] }, { frags: [null] }
	];
	const kept = [];
	for (const i of inputs) {
		const r = V(i);
		if (r.line !== null || !same(codes(r.problems), ['E-CO-3'])) kept.push(JSON.stringify(i) + ' -> ' + JSON.stringify(r));
	}
	expect(kept.length === 0, kept.join('\n'));
	expect(V({ frags: new Array(512).fill({ text: 'x' }) }).line.frags.length === 512, '512 fragments allowed');
	expect(V({ text: '' }).line.text === '', 'empty text allowed');
	expect(V({ frags: [{ text: 'x', block: false }] }).line.frags[0].text === 'x', 'block:false is a text fragment');
});

test('chk04_memberRemoval', function (t) {
	const V = t.CO.validateLine;
	let r = V({ text: 'a', ui: { color: 'red', style: 'success' } });
	expectSame(r.line.ui, { style: 'success' }, 'bad ui.color removed, style kept');
	expectSame(codes(r.problems), ['E-CO-7:color']);
	r = V({ frags: [{ block: true, color: 'url(x)', href: 'javascript:alert(1)', tooltip: 't' }] });
	expectSame(r.line.frags[0], { block: true, tooltip: 't' }, 'bad block colour and href removed');
	expectSame(codes(r.problems), ['E-CO-7:color', 'E-CO-7:href']);
	r = V({ text: 'a', ui: { image: { src: 'https://e.x/a.png', alt: 'a' } } });
	expect(r.line.ui === undefined, 'unsafe image removed and empty ui omitted');
	expectSame(codes(r.problems), ['E-CO-7:image src']);
	r = V({ text: 'a', ui: { image: { src: '/a.png' } } });
	expect(r.line.ui === undefined, 'image without alt removed');
	expectSame(codes(r.problems), ['E-CO-7:image']);
	r = V({ text: 'a', ui: { icon: 'Bad Name' } });
	expect(r.line.ui === undefined, 'bad icon removed');
	expectSame(codes(r.problems), ['E-CO-6']);
	r = V({ text: 'a', instant: '2026-10-07 13:04:05', ui: { style: 'loud', marker: 'yes', image: { src: '/a.png', alt: '', width: 0 } },
		frags: undefined });
	expectSame(r.line, { level: 'INFO', text: 'a', ui: { image: { src: '/a.png', alt: '' } } }, 'silent removals');
	expectSame(r.problems, [], 'silent removals report nothing');
});

test('chk05_unknownMembersIgnored', function (t) {
	const r = t.CO.validateLine({ n: 3, text: 'a', extra: 1, ui: { style: 'muted', glow: true }, __proto__x: 1 });
	expectSame(r.line, { n: 3, level: 'INFO', text: 'a', ui: { style: 'muted' } });
	expectSame(r.problems, []);
	const f = t.CO.validateLine({ frags: [{ text: 'x', shadow: 1 }, { block: true, label: 'L', size: 9 }] });
	expectSame(f.line.frags, [{ text: 'x' }, { block: true, label: 'L' }]);
	expectSame(f.problems, []);
});

test('chk06_normalisationAndCopy', function (t) {
	const input = { n: 1, level: 'WARNING', instant: '2026-10-07T13:04:05.123Z',
		frags: [{ block: true, href: '#a\nb', tooltip: 'x'.repeat(3000), label: 'y'.repeat(300) }, { block: true, href: '/a\\b' }, { text: 'z', bold: true }],
		ui: { marker: true, image: { src: '/r\n/c.png', alt: 'c', width: 480 } } };
	const r = t.CO.validateLine(input);
	expect(r.line !== input && r.line.frags !== input.frags && r.line.ui !== input.ui, 'returns a fresh object');
	expect(r.line.frags[0].href === '#ab', 'fragment href stripped');
	expect(r.line.frags[0].tooltip.length === 2048 && r.line.frags[0].label.length === 256, 'tooltip/label truncated');
	expect(r.line.frags[1].href === '/a/b', 'path href folded');
	expect(r.line.frags[2].bold === true, 'bold kept');
	expectSame(r.line.ui, { image: { src: '/r/c.png', alt: 'c', width: 480 }, marker: true }, 'ui (allowlist order)');
	expect(r.line.instant === input.instant && r.line.level === 'WARNING', 'instant and level kept');
	expectSame(r.problems, []);
});

// @cases:core

function mkConsole(t, opts) {
	const host = t.env.document.createElement('div');
	t.env.body.appendChild(host);
	const api = t.CO.create(host, opts || {});
	const q = function (sel) { return host.querySelector(sel); };
	return {
		host: host, api: api, q: q, pane: q('.juneau-co-pane'),
		rows: function () { return host.querySelectorAll('.juneau-co-line'); },
		ns: function () { return host.querySelectorAll('.juneau-co-line').map(function (r) { return Number(r.getAttribute('data-n')); }); },
		row: function (n) { return host.querySelectorAll('.juneau-co-line').find(function (r) { return r.getAttribute('data-n') === String(n); }) || null; }
	};
}

test('core01_numbering', function (t) {
	const c = mkConsole(t);
	expect(c.api.append([{ text: 'a' }, { text: 'b' }]) === 2, 'two added');
	expect(c.api.append({ text: 'c' }) === 1, 'a single object is an array of one');
	expect(c.api.append([{ n: 2, text: 'dup' }, { n: 7, text: 'g' }, { text: 'h' }]) === 2, 'dup ignored; gap kept; auto-number after the gap');
	expect(c.api.append([{ n: 5, text: 'late' }]) === 0, 'a late gap-fill is ignored');
	expect(c.api.append([]) === 0, 'empty array');
	expectSame(c.ns(), [1, 2, 3, 7, 8], 'row numbers');
	expect(c.row(2).textContent.indexOf('dup') < 0, 'the original row 2 is kept');
});

test('core02_multiLineIsOneRow', function (t) {
	const c = mkConsole(t);
	c.api.append({ text: 'a\nb\nc' });
	c.api.append({ text: 'd' });
	expectSame(c.ns(), [1, 2]);
	expect(c.row(1).querySelector('.juneau-co-gutter').textContent === '1', 'gutter shows n once');
	expect(c.row(1).querySelector('.juneau-co-text').textContent === 'a\nb\nc', 'newlines kept in one row');
});

test('core03_colourPrecedence', function (t) {
	const c = mkConsole(t);
	c.api.append([
		{ text: 'a', level: 'SEVERE' },
		{ text: 'b', level: 'SEVERE', ui: { style: 'success' } },
		{ text: 'c', level: 'SEVERE', ui: { style: 'success', color: '#123456' } },
		{ text: 'd' },
		{ text: 'e', level: 'FINE' },
		{ level: 'WARNING', frags: [{ text: 'x' }, { text: 'y', style: 'accent' }, { text: 'z', color: '#abc' }] }
	]);
	const body = c.rows().map(function (r) { return r.querySelector('.juneau-co-text'); });
	const tone = function (n) { return (n.className.match(/juneau-co-s-\w+/g) || []).join(' '); };
	expect(tone(body[0]) === 'juneau-co-s-error', 'SEVERE default: ' + tone(body[0]));
	expect(tone(body[1]) === 'juneau-co-s-success', 'ui.style over level: ' + tone(body[1]));
	expect(body[2].style.color === '#123456' && tone(body[2]) === '', 'ui.color over ui.style');
	expect(tone(body[3]) === '' && !body[3].style.color, 'INFO inherits the console colour');
	expect(tone(body[4]) === 'juneau-co-s-muted', 'FINE is muted');
	const f = body[5].querySelectorAll('.juneau-co-f');
	expect(tone(body[5]) === 'juneau-co-s-warn', 'the line tone sits on the text span of a fragment line');
	expect(tone(f[0]) === '' && !f[0].style.color, 'a plain fragment inherits the line colour');
	expect(tone(f[1]) === 'juneau-co-s-accent' && f[2].style.color === '#abc', 'fragments override');
});

test('core04_blockMatrix', function (t) {
	const c = mkConsole(t);
	c.api.append({ frags: [
		{ block: true, href: '/run/7', label: 'Run 7' },
		{ block: true, href: '#step-3', tooltip: 'step 3' },
		{ block: true, tooltip: 'tip only', color: '#2e7d32' },
		{ block: true, label: 'label only', style: 'success' },
		{ block: true }
	] });
	const b = c.host.querySelectorAll('.juneau-co-block');
	expect(b.length === 5, 'five blocks');
	expect(b[0].tagName === 'A' && b[0].getAttribute('href') === '/run/7' && b[0].getAttribute('rel') === 'nofollow noreferrer'
		&& b[0].getAttribute('aria-label') === 'Run 7', 'path link');
	expect(b[1].tagName === 'A' && b[1].getAttribute('href') === '#step-3' && b[1].getAttribute('rel') === null
		&& b[1].getAttribute('aria-label') === 'step 3' && b[1].getAttribute('data-juneau-co-tip') === 'step 3', 'fragment link');
	expect(b[2].tagName === 'SPAN' && b[2].getAttribute('tabindex') === '0' && b[2].getAttribute('role') === 'img'
		&& b[2].getAttribute('aria-label') === 'tip only' && b[2].style.backgroundColor === '#2e7d32', 'tooltip-only block');
	expect(b[3].getAttribute('tabindex') === null && b[3].getAttribute('role') === 'img'
		&& b[3].getAttribute('aria-label') === 'label only' && b[3].classList.contains('juneau-co-fill-success'), 'label-only block');
	expect(b[4].getAttribute('aria-hidden') === 'true' && b[4].getAttribute('tabindex') === null
		&& b[4].classList.contains('juneau-co-block-empty'), 'decorative block');
});

test('core05_inertRendering', function (t) {
	const c = mkConsole(t);
	c.api.append([
		{ text: '<script>alert(1)</script>' },
		{ frags: [{ text: '<img src=x onerror=alert(1)>' }, { block: true, href: 'javascript:alert(1)', tooltip: '<b>t</b>' }] },
		{ text: 'x', ui: { image: { src: '/a.png', alt: '"><script>' } } }
	]);
	expect(c.host.querySelector('script') === null, 'no script element');
	expect(c.host.querySelectorAll('img').length === 1, 'only the validated image is an img');
	expect(c.row(1).querySelector('.juneau-co-text').textContent === '<script>alert(1)</script>', 'markup shown as text');
	const blk = c.host.querySelector('.juneau-co-block');
	expect(blk.tagName === 'SPAN' && blk.getAttribute('href') === null, 'an unsafe href renders without a link');
	expect(c.host.querySelector('.juneau-co-image').getAttribute('alt') === '"><script>', 'alt set as an attribute value');
});

test('core06_boldAndScreenReaderPrefix', function (t) {
	const c = mkConsole(t);
	c.api.append([{ level: 'SEVERE', frags: [{ text: 'b', bold: true }] }, { text: 'w', level: 'WARNING' }, { text: 'i' }]);
	expect(c.host.querySelector('.juneau-co-f').classList.contains('juneau-co-b'), 'bold class');
	const sr = c.rows().map(function (r) { const s = r.querySelector('.juneau-co-sr'); return s ? s.textContent : null; });
	expectSame(sr, ['Error: ', 'Warning: ', null], 'visually hidden level prefix');
});

test('core07_nonFatalWarnings', function (t) {
	const c = mkConsole(t, { id: 'build' });
	c.api.append([{ text: 1 }, { level: 'NOPE', text: 'x' }, { text: 'ok' }]);
	c.api.append([{ frags: [] }]);
	c.api.append([{ text: 'a', ui: { color: 'red' } }, { text: 'b', ui: { color: 'blue' } }, { frags: [{ block: true, href: 'http://e.x/' }] }]);
	c.api.append([{ text: 'c', ui: { icon: 'nosuch' } }, { text: 'd', ui: { icon: 'check' } }]);
	const w3 = t.rec.warnsMatching('E-CO-3');
	expect(w3.length === 1 && w3[0].indexOf("console-output region 'build': dropped 2 malformed line(s); first: {\"text\":1}") >= 0, w3);
	expect(t.rec.warnsMatching('E-CO-7').length === 2, 'E-CO-7 once per kind: ' + JSON.stringify(t.rec.warnsMatching('E-CO-7')));
	expect(t.rec.warnsMatching("unsafe color 'red' ignored").length === 1, 'colour warning text');
	expect(t.rec.warnsMatching("unknown icon 'nosuch' ignored").length === 1, 'E-CO-6 for an unregistered icon');
	expect(c.row(c.ns().at(-1)).querySelector('.juneau-co-icon') !== null, 'a registered icon renders');
	expect(c.row(c.ns().at(-2)).querySelector('.juneau-co-icon') === null, 'an unregistered icon is dropped');
	expect(t.rec.errors.length === 0, 'non-fatal codes never log errors: ' + JSON.stringify(t.rec.errors));
});

test('core08_configErrorThrowsBeforeDom', function (t) {
	const host = t.env.document.createElement('div');
	let err = null;
	try { t.CO.create(host, { id: 'x', anchorPrefix: '9bad' }); } catch (e) { err = e; }
	expect(err && err.name === 'JuneauConsoleOutputError' && err.code === 'E-CO-1', 'E-CO-1 thrown: ' + err);
	expect(err.message.indexOf("console-output region 'x': anchorPrefix") === 0, err.message);
	expect(host.childNodes.length === 0, 'nothing created');
	expect(t.rec.errorsMatching('E-CO-1').length === 1, 'logged once');
});

test('core09_anchorPrefix', function (t) {
	const grammar = /^[A-Za-z][A-Za-z0-9_-]{0,31}$/;
	const a = mkConsole(t, { id: '9lives' });
	a.api.append({ text: 'x' });
	expect(a.row(1).id === 'co-9lives-L1' && grammar.test('co-9lives-L'), 'digit-leading id: ' + a.row(1).id);
	expect(a.row(1).querySelector('.juneau-co-gutter').getAttribute('href') === '#co-9lives-L1', 'fragment gutter link');
	const longId = 'x'.repeat(40) + '/weird id';
	const b = mkConsole(t, { id: longId, rowId: 42 });
	b.api.append({ text: 'y' });
	const pfx = b.row(1).id.slice(0, -1);
	expect(grammar.test(pfx) && pfx.length <= 32, 'long id prefix: ' + pfx);
	const c1 = mkConsole(t, { id: 'dup', anchorPrefix: 'L' });
	const c2 = mkConsole(t, { id: 'dup2', anchorPrefix: 'L' });
	c2.api.append({ text: 'z' });
	expect(t.rec.warnsMatching("anchor prefix 'L' already in use on this page; anchors disabled for this console").length === 1, 'E-CO-9');
	expect(c2.row(1).getAttribute('id') === null && c2.row(1).querySelector('.juneau-co-gutter').getAttribute('href') === null, 'no ids or links');
	c1.api.destroy();
	const c3 = mkConsole(t, { id: 'dup3', anchorPrefix: 'L' });
	c3.api.append({ text: 'w' });
	expect(c3.row(1).id === 'L1', 'destroy frees the prefix');
});

test('core10_rowsAndCompact', function (t) {
	const v = function (opts) { return mkConsole(t, opts).pane.style.getPropertyValue('--juneau-co-rows'); };
	expectSame([v({}), v({ compact: true }), v({ compact: true, rows: 12 }), v({ rows: 3 })], ['20', '8', '12', '3']);
	const full = mkConsole(t, { title: 'Build output', downloadUrl: '/x/download' });
	expect(full.q('.juneau-co-title').textContent === 'Build output', 'title');
	expect(full.pane.getAttribute('aria-label') === 'Build output' && full.pane.getAttribute('role') === 'log'
		&& full.pane.getAttribute('tabindex') === '0', 'pane a11y');
	expect(full.q('.juneau-co-download').getAttribute('href') === '/x/download', 'download link');
	const compact = mkConsole(t, { title: 'Build output', compact: true, downloadUrl: '/x/download' });
	expect(compact.q('.juneau-co').classList.contains('juneau-co-compact') && compact.q('.juneau-co-title') === null, 'compact hides the title');
	expect(compact.q('.juneau-co-menu').querySelector('.juneau-co-download') !== null && compact.q('.juneau-co-menu').hidden === true, 'download in the overflow menu');
	const more = compact.q('.juneau-co-more');
	more.dispatch('click', {});
	expect(compact.q('.juneau-co-menu').hidden === false && more.getAttribute('aria-expanded') === 'true', 'menu opens');
	expect(mkConsole(t, {}).pane.getAttribute('aria-label') === 'Console output', 'default pane label');
});

test('core11_destroyIdempotent', function (t) {
	const c = mkConsole(t, { id: 'd' });
	c.api.append({ text: 'x' });
	const tip = c.q('.juneau-co-tooltip');
	expect(tip !== null, 'tooltip exists');
	c.api.destroy();
	c.api.destroy();
	expect(c.q('.juneau-co-tooltip') === null, 'tooltip removed');
	const left = Object.values(c.pane._listeners).reduce(function (n, l) { return n + l.length; }, 0);
	expect(left === 0, 'pane listeners removed: ' + left);
	expect(c.api.append({ text: 'y' }) === 0 && c.ns().length === 1, 'append after destroy is a no-op');
});

test('core12_image', function (t) {
	const c = mkConsole(t);
	c.api.append({ text: 'chart', ui: { image: { src: '/run/42/chart.png', alt: 'Coverage chart', width: 480 } } });
	const im = c.q('.juneau-co-image');
	expectSame([im.getAttribute('src'), im.getAttribute('alt'), im.getAttribute('loading'), im.getAttribute('decoding'),
		im.getAttribute('referrerpolicy'), im.getAttribute('width')],
		['/run/42/chart.png', 'Coverage chart', 'lazy', 'async', 'same-origin', '480']);
	expect(im.parentNode === c.row(1), 'image inside the row');
});

test('core13_showTime', function (t) {
	const c = mkConsole(t, { showTime: true });
	c.api.append([{ text: 'a', instant: '2026-10-07T13:04:05.123Z' }, { text: 'b', instant: '2026-10-07T13:04:05Z' }, { text: 'c' }]);
	const tm = c.rows().map(function (r) { const s = r.querySelector('.juneau-co-time'); return s ? s.textContent : null; });
	expectSame(tm, ['13:04:05.123', '13:04:05.000', null]);
	expect(c.row(1).querySelector('.juneau-co-gutter').getAttribute('title') === '2026-10-07T13:04:05.123Z', 'instant on the gutter title');
	expect(mkConsole(t, {}).api.append({ text: 'd', instant: '2026-10-07T13:04:05Z' }) === 1, 'showTime off');
});

// @cases:status

function iso(t, offsetMs) { return new t.sandbox.Date(t.sandbox.Date.now() + offsetMs).toISOString(); }

test('status01_pendingThenRunningWithZeroLines', function (t) {
	const c = mkConsole(t, { title: 'Build' });
	c.api.setStatus({ state: 'PENDING' });
	expect(c.q('.juneau-co-state').textContent === 'PENDING', 'state text');
	expect(c.q('.juneau-co-elapsed').textContent === 'Waiting to start…', 'pending shows waiting: ' + c.q('.juneau-co-elapsed').textContent);
	c.api.setStatus({ state: 'RUNNING', startedAt: iso(t, 0), now: iso(t, 0) });
	expect(c.q('.juneau-co-elapsed').textContent === 'Elapsed 00:00:00', 'running with no lines ticks from zero');
	expect(c.q('.juneau-co-status-live').textContent === 'Build: RUNNING', 'live region announces the change');
	expect(c.ns().length === 0, 'no rows needed');
});

test('status02_elapsedWithSkew', function (t) {
	const c = mkConsole(t);
	// The server clock runs 10 s ahead of the browser's; the job started 65 s ago by the server's clock.
	c.api.setStatus({ state: 'RUNNING', now: iso(t, 10000), startedAt: iso(t, 10000 - 65000) });
	const el = c.q('.juneau-co-elapsed');
	expect(el.textContent === 'Elapsed 00:01:05', el.textContent);
	t.clock.advance(1000);
	expect(el.textContent === 'Elapsed 00:01:06', el.textContent);
	t.clock.advance(2000);
	expect(el.textContent === 'Elapsed 00:01:08', el.textContent);
	expect(el.getAttribute('aria-hidden') === 'true', 'ticker hidden from assistive technology');
});

test('status03_durationForms', function (t) {
	const a = mkConsole(t);
	a.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 3723000 });
	expect(a.q('.juneau-co-elapsed').textContent === 'Duration 01:02:03', a.q('.juneau-co-elapsed').textContent);
	const b = mkConsole(t);
	b.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 100 * 3600000 + 5000 });
	expect(b.q('.juneau-co-elapsed').textContent === 'Duration 4d 04:00:05', b.q('.juneau-co-elapsed').textContent);
	const c = mkConsole(t);
	c.api.setStatus({ state: 'RUNNING', now: iso(t, 0), startedAt: iso(t, -42000) });
	t.clock.advance(1000);
	c.api.setStatus({ state: 'FAILED', terminal: true });
	expect(c.q('.juneau-co-elapsed').textContent === 'Duration 00:00:43', 'without durationMs: last elapsed: ' + c.q('.juneau-co-elapsed').textContent);
	const d = mkConsole(t);
	d.api.setStatus({ state: 'RUNNING', now: iso(t, 0), startedAt: iso(t, -(99 * 3600000 + 59 * 60000 + 59000)) });
	expect(d.q('.juneau-co-elapsed').textContent === 'Elapsed 99:59:59', d.q('.juneau-co-elapsed').textContent);
	t.clock.advance(1000);
	expect(d.q('.juneau-co-elapsed').textContent === 'Elapsed 4d 04:00:00', 'switches to the day form above 99 h: ' + d.q('.juneau-co-elapsed').textContent);
});

test('status04_tickerStopsOnTerminal', function (t) {
	const c = mkConsole(t);
	c.api.setStatus({ state: 'RUNNING', now: iso(t, 0), startedAt: iso(t, 0) });
	t.clock.advance(3000);
	c.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 3000 });
	t.clock.advance(5000);
	expect(c.q('.juneau-co-elapsed').textContent === 'Duration 00:00:03', 'terminal text is not overwritten: ' + c.q('.juneau-co-elapsed').textContent);
});

test('status05_liveRegionAndStateStyle', function (t) {
	const c = mkConsole(t, { title: 'Build' });
	const live = c.q('.juneau-co-status-live');
	expect(live.getAttribute('role') === 'status' && !c.pane.contains(live), 'status node outside the log pane');
	c.api.setStatus({ state: 'RUNNING', stateStyle: 'accent', now: iso(t, 0), startedAt: iso(t, 0) });
	live.textContent = '';
	c.api.setStatus({ state: 'RUNNING', stateStyle: 'accent', now: iso(t, 0), startedAt: iso(t, 0) });
	t.clock.advance(3000);
	expect(live.textContent === '', 'an unchanged state and the ticker never write the live node');
	c.api.setStatus({ state: 'SUCCEEDED', stateStyle: 'success', terminal: true, durationMs: 1 });
	expect(live.textContent === 'Build: SUCCEEDED', live.textContent);
	const st = c.q('.juneau-co-state');
	expect(st.classList.contains('juneau-co-s-success') && !st.classList.contains('juneau-co-s-accent'), st.className);
	const untitled = mkConsole(t);
	untitled.api.setStatus({ state: 'QUEUED' });
	expect(untitled.q('.juneau-co-status-live').textContent === 'QUEUED', 'no title prefix');
});

test('status06_busMessages', function (t) {
	const got = [];
	const c = mkConsole(t, { id: 'job', emit: function (m) { got.push(m); } });
	c.api.setStatus({ state: 'PENDING' });
	c.api.setStatus({ state: 'PENDING' });
	c.api.setStatus({ state: 'RUNNING', now: iso(t, 0), startedAt: iso(t, 0) });
	c.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 5 });
	c.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 5 });
	expectSame(got, [
		{ kind: 'console-output.state', id: 'job', state: 'PENDING', terminal: false },
		{ kind: 'console-output.state', id: 'job', state: 'RUNNING', terminal: false },
		{ kind: 'console-output.state', id: 'job', state: 'SUCCEEDED', terminal: true },
		{ kind: 'console-output.terminal', id: 'job', state: 'SUCCEEDED' }
	]);
	const bad = mkConsole(t, { id: 'b', emit: function () { throw new Error('boom'); } });
	bad.api.setStatus({ state: 'RUNNING' });
	expect(bad.q('.juneau-co-state').textContent === 'RUNNING', 'a throwing emit does not break the console');
	const none = mkConsole(t, { id: 'n' });
	none.api.setStatus({ state: 'RUNNING' });
	expect(true, 'no emit option publishes nothing');
});

test('status07_destroyStopsTicker', function (t) {
	const c = mkConsole(t);
	c.api.setStatus({ state: 'RUNNING', now: iso(t, 0), startedAt: iso(t, 0) });
	c.api.destroy();
	const before = c.q('.juneau-co-elapsed').textContent;
	t.clock.advance(5000);
	expect(c.q('.juneau-co-elapsed').textContent === before, 'no ticks after destroy');
	c.api.setStatus({ state: 'FAILED', terminal: true });
	expect(c.q('.juneau-co-state').textContent === 'RUNNING', 'setStatus after destroy is a no-op');
});

// @cases:scroll

function fill(c, from, to) {
	const lines = [];
	for (let n = from; n <= to; n++) lines.push({ n: n, text: 'line ' + n });
	return c.api.append(lines);
}

test('scroll01_sticksToBottom', function (t) {
	const c = mkConsole(t);
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	expect(c.pane.scrollTop === 20 * E.ROW_H - 100, 'pinned: ' + c.pane.scrollTop);
	fill(c, 21, 25);
	expect(c.pane.scrollTop === 25 * E.ROW_H - 100, 'still pinned: ' + c.pane.scrollTop);
	expect(c.q('.juneau-co-jump').hidden === true && c.pane.getAttribute('aria-live') === 'polite', 'no jump button; live');
});

test('scroll02_scrollUpPausesAndBackResticks', function (t) {
	const c = mkConsole(t);
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	E.userScroll(c.pane, 100);
	fill(c, 21, 23);
	const jump = c.q('.juneau-co-jump');
	expect(c.pane.scrollTop === 100, 'view stays put: ' + c.pane.scrollTop);
	expect(jump.hidden === false && jump.textContent === 'Jump to latest (3 new)', 'jump: ' + jump.textContent);
	expect(c.pane.getAttribute('aria-live') === 'off', 'live off while scrolled up');
	fill(c, 24, 24);
	expect(jump.textContent === 'Jump to latest (4 new)', jump.textContent);
	E.userScroll(c.pane, c.pane.scrollHeight);
	expect(jump.hidden === true && c.pane.getAttribute('aria-live') === 'polite', 'scrolling back re-sticks');
	fill(c, 25, 26);
	expect(c.pane.scrollTop === 26 * E.ROW_H - 100, 'following again');
});

test('scroll03_jumpButton', function (t) {
	const c = mkConsole(t);
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	E.userScroll(c.pane, 0);
	fill(c, 21, 22);
	c.q('.juneau-co-jump').dispatch('click', {});
	expect(c.pane.scrollTop === 22 * E.ROW_H - 100 && c.q('.juneau-co-jump').hidden === true, 'jumped');
	E.fireScroll(c.pane);
	fill(c, 23, 23);
	expect(c.pane.scrollTop === 23 * E.ROW_H - 100, 'the jump\'s own scroll event did not unstick');
});

test('scroll04_programmaticScrollIsGuarded', function (t) {
	const c = mkConsole(t);
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	E.fireScroll(c.pane);
	fill(c, 21, 21);
	expect(c.pane.scrollTop === 21 * E.ROW_H - 100, 'still stuck after the guarded event');
});

test('scroll05_anchorOnMountIsQueued', function (t) {
	t.env.window.location.hash = '#L5';
	const c = mkConsole(t, { anchorPrefix: 'L' });
	c.pane.clientHeight = 60;
	fill(c, 1, 3);
	expect(t.scrolled.length === 0, 'nothing to reveal yet');
	fill(c, 4, 8);
	expect(c.row(5).classList.contains('juneau-co-target'), 'row 5 highlighted');
	const last = t.scrolled[t.scrolled.length - 1];
	expect(last.n === '5' && last.block === 'center' && last.behavior === 'auto', JSON.stringify(last));
	expect(t.env.getActive() === c.row(5).querySelector('.juneau-co-gutter'), 'focus on the gutter link');
	const top = c.pane.scrollTop;
	fill(c, 9, 30);
	expect(c.pane.scrollTop === top, 'later appends do not yank the view away from the target');
});

test('scroll06_hashchange', function (t) {
	const c = mkConsole(t, { anchorPrefix: 'run-L' });
	c.pane.clientHeight = 100;
	fill(c, 1, 30);
	t.setHash('#run-L12');
	expect(c.row(12).classList.contains('juneau-co-target'), 'row 12');
	t.setHash('#other');
	t.setHash('#run-L7x');
	t.setHash('#run-L');
	expect(c.row(12).classList.contains('juneau-co-target') && !c.row(7).classList.contains('juneau-co-target'), 'non-matching hashes ignored');
	t.setHash('#run-L3');
	expect(c.row(3).classList.contains('juneau-co-target') && !c.row(12).classList.contains('juneau-co-target'), 'highlight moves');
});

test('scroll07_queuedTargetMissingAtTerminal', function (t) {
	const c = mkConsole(t, { id: 'j', anchorPrefix: 'L' });
	c.pane.clientHeight = 100;
	fill(c, 1, 10);
	expect(c.api.scrollToLine(50) === false, 'queued');
	c.api.setStatus({ state: 'SUCCEEDED', terminal: true, durationMs: 1 });
	expect(t.rec.warnsMatching("console-output region 'j': line 50 not found").length === 1, 'E-CO-8');
	expect(t.scrolled[t.scrolled.length - 1].n === '10', 'falls back to the last line');
	expect(c.api.scrollToLine(60) === false && t.rec.warnsMatching('line 60 not found').length === 1, 'after terminal: E-CO-8 at once');
});

test('scroll08_gapFallsToNextRow', function (t) {
	const c = mkConsole(t, { anchorPrefix: 'L' });
	c.api.append([{ n: 1, text: 'a' }, { n: 2, text: 'b' }, { n: 10, text: 'c' }]);
	c.api.scrollToLine(5);
	expect(t.rec.warnsMatching('line 5 not found').length === 1, 'E-CO-8');
	expect(c.row(10).classList.contains('juneau-co-target'), 'nearest following row');
	expect(c.api.scrollToLine(2) === true && c.row(2).classList.contains('juneau-co-target'), 'present row');
	expect(c.api.scrollToLine(0) === false && c.api.scrollToLine('2') === false, 'non-positive or non-integer ignored');
});

test('scroll09_holdUntilUserInput', function (t) {
	const c = mkConsole(t, { anchorPrefix: 'L' });
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	c.api.scrollToLine(20);
	fill(c, 21, 25);
	expect(c.q('.juneau-co-jump').hidden === false, 'held unstuck even though the target was at the bottom');
	c.pane.dispatch('wheel', {});
	E.userScroll(c.pane, c.pane.scrollHeight);
	fill(c, 26, 26);
	expect(c.pane.scrollTop === 26 * E.ROW_H - 100, 'user input released the hold');
});

test('scroll10_imageLoadRepins', function (t) {
	const c = mkConsole(t);
	c.pane.clientHeight = 100;
	fill(c, 1, 20);
	c.api.append({ text: 'chart', ui: { image: { src: '/c.png', alt: 'c' } } });
	const bottom = c.pane.scrollTop;
	c.pane.scrollTop = 0; // stands in for the image growing the row after the append pinned the pane
	c.q('.juneau-co-image').dispatch('load', {});
	expect(c.pane.scrollTop === bottom, 'load re-pinned while following');
	E.userScroll(c.pane, 0);
	c.api.append({ text: 'chart 2', ui: { image: { src: '/d.png', alt: 'd' } } });
	const imgs = E.byClass(c.host, 'juneau-co-image');
	expect(imgs.length === 2, 'two images');
	imgs[1].dispatch('load', {});
	expect(c.pane.scrollTop === 0, 'load does not move a scrolled-up pane');
});

test('scroll11_disabledAnchorsIgnoreTheHash', function (t) {
	const a = mkConsole(t, { anchorPrefix: 'L' });
	const b = mkConsole(t, { anchorPrefix: 'L' });
	fill(a, 1, 5);
	fill(b, 1, 5);
	t.setHash('#L3');
	expect(a.row(3).classList.contains('juneau-co-target') && !b.row(3).classList.contains('juneau-co-target'), 'only the owner reacts');
});

// @cases:earlier

/** A loadEarlier stand-in: every call returns a promise the test settles. */
function loader() {
	const calls = [];
	const fn = function (before) {
		let resolve, reject;
		const promise = new Promise(function (a, b) { resolve = a; reject = b; });
		calls.push({ before: before, resolve: resolve, reject: reject });
		return promise;
	};
	return { fn: fn, calls: calls };
}

function lines(from, to) {
	const out = [];
	for (let n = from; n <= to; n++) out.push({ n: n, text: 'line ' + n });
	return out;
}

function control(c) { return c.pane.firstChild && c.pane.firstChild.classList.contains('juneau-co-control') ? c.pane.firstChild : null; }

test('earlier01_seedingShowsAndRemovesTheControl', function (t) {
	const c = mkConsole(t, { loadEarlier: loader().fn });
	c.api.append(lines(41, 45));
	expect(control(c) === null, 'no control before seeding');
	expect(c.api.prepend([], { hasEarlier: true, before: '40' }) === 0, 'seeding adds nothing');
	const btn = control(c).querySelector('button.juneau-co-earlier');
	expect(btn && btn.textContent === 'Load earlier lines' && btn.getAttribute('type') === 'button', 'button');
	c.api.prepend([], { hasEarlier: false });
	expect(control(c) === null, 'hasEarlier:false removes the control');
});

test('earlier02_loadPreservesScrollAndFocus', async function (t) {
	const L = loader();
	const c = mkConsole(t, { loadEarlier: L.fn });
	c.pane.clientHeight = 60;
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	E.userScroll(c.pane, 40);
	const fromBottom = c.pane.scrollHeight - c.pane.scrollTop;
	const btn = control(c).querySelector('.juneau-co-earlier');
	btn.focus();
	btn.dispatch('click', {});
	expect(btn.textContent === 'Loading…' && btn.disabled === true, 'pending state');
	expectSame(L.calls.map(function (x) { return x.before; }), ['40'], 'one request with the before token');
	L.calls[0].resolve({ lines: lines(38, 40), hasEarlier: true, before: '37' });
	await E.flush();
	expectSame(c.ns(), [38, 39, 40, 41, 42, 43, 44, 45], 'rows inserted after the control');
	expect(c.pane.scrollHeight - c.pane.scrollTop === fromBottom, 'on-screen rows did not move');
	expect(btn.textContent === 'Load earlier lines' && btn.disabled === false, 'button restored');
	expect(t.env.getActive() === btn, 'focus stays on the button');
	expect(c.q('.juneau-co-announce').textContent === 'Loaded 3 earlier lines', 'announced');
	btn.dispatch('click', {});
	expect(L.calls[1].before === '37', 'next request uses the new token');
});

test('earlier03_oneRequestAtATime', function (t) {
	const L = loader();
	const c = mkConsole(t, { loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	const btn = control(c).querySelector('.juneau-co-earlier');
	btn.dispatch('click', {});
	btn.dispatch('click', {});
	expect(L.calls.length === 1, 'second click ignored while pending');
});

test('earlier04_lastPageRemovesControlAndMovesFocus', async function (t) {
	const L = loader();
	const c = mkConsole(t, { loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	const btn = control(c).querySelector('.juneau-co-earlier');
	btn.focus();
	btn.dispatch('click', {});
	L.calls[0].resolve({ lines: lines(39, 40), hasEarlier: false });
	await E.flush();
	expect(control(c) === null, 'control removed');
	expect(t.env.getActive() === c.row(39).querySelector('.juneau-co-gutter'), 'focus on the first new row');
});

test('earlier05_failureRetries', async function (t) {
	const L = loader();
	const c = mkConsole(t, { id: 'j', loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	const btn = control(c).querySelector('.juneau-co-earlier');
	btn.dispatch('click', {});
	L.calls[0].reject({ status: 503 });
	await E.flush();
	expect(btn.textContent === 'Load earlier lines (retry)' && btn.disabled === false, 'retry text');
	expect(t.rec.warnsMatching("console-output region 'j': fetching lines failed (HTTP 503)").length === 1, 'E-CO-4');
	btn.dispatch('click', {});
	L.calls[1].reject(new Error('net down'));
	await E.flush();
	expect(t.rec.warnsMatching('fetching lines failed').length === 1, 'E-CO-4 logged once');
	expect(c.q('.jc-console-error') === null, 'never fatal');
	expect(L.calls[1].before === '40', 'retry reuses the token');
});

test('earlier06_goneBecomesTheNotice', async function (t) {
	const L = loader();
	const c = mkConsole(t, { loadEarlier: L.fn, downloadUrl: '/dl' });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	control(c).querySelector('.juneau-co-earlier').dispatch('click', {});
	L.calls[0].reject({ status: 410 });
	await E.flush();
	const notice = control(c).querySelector('.juneau-co-earlier-notice');
	expect(notice && notice.textContent === 'Earlier lines not shown — Download full log', 'notice: ' + (notice && notice.textContent));
	const a = notice.querySelector('a');
	expect(a.getAttribute('href') === '/dl' && a.hasAttribute('download'), 'download link');
	expect(control(c).querySelector('.juneau-co-earlier') === null && t.rec.warnsMatching('fetching lines failed').length === 0, 'no button, no warning');
});

test('earlier07_abortIsSilent', async function (t) {
	const L = loader();
	const c = mkConsole(t, { loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	control(c).querySelector('.juneau-co-earlier').dispatch('click', {});
	c.api.destroy();
	L.calls[0].reject({ name: 'AbortError' });
	await E.flush();
	expect(t.rec.warnsMatching('console-output').length === 0 && c.q('.jc-console-error') === null, 'silent');
});

test('earlier08_nonAscendingIsFatal', function (t) {
	const c = mkConsole(t, { id: 'j' });
	c.api.append(lines(41, 42));
	expect(c.api.prepend([{ n: 39, text: 'a' }, { n: 38, text: 'b' }], { hasEarlier: false }) === 0, 'nothing inserted');
	expect(t.rec.errorsMatching("console-output region 'j': invalid continuation token or non-increasing line numbers").length === 1, 'E-CO-5');
	expect(c.q('.jc-console-error') !== null, 'banner');
	expectSame(c.ns(), [41, 42], 'rows kept');
});

test('earlier09_duplicatesIgnoredAndBadTokenFatal', function (t) {
	const c = mkConsole(t);
	c.api.append(lines(41, 42));
	expect(c.api.prepend(lines(39, 42), { hasEarlier: false }) === 2, 'only n < first inserted');
	expectSame(c.ns(), [39, 40, 41, 42]);
	const d = mkConsole(t, { id: 'k' });
	d.api.append(lines(5, 6));
	d.api.prepend([], { hasEarlier: true, before: 'a b' });
	expect(t.rec.errorsMatching("console-output region 'k': invalid continuation token").length === 1, 'bad before token is E-CO-5');
});

test('earlier10_appendTrimShowsTheNotice', function (t) {
	const c = mkConsole(t, { maxDomRows: 5, downloadUrl: '/dl', loadEarlier: loader().fn });
	c.api.append(lines(10, 12));
	c.api.prepend([], { hasEarlier: true, before: '9' });
	c.api.append(lines(13, 17));
	expectSame(c.ns(), [13, 14, 15, 16, 17], 'oldest rows trimmed');
	expect(control(c).querySelector('.juneau-co-earlier-notice').textContent === 'Earlier lines not shown — Download full log', 'notice');
	c.api.prepend([], { hasEarlier: true, before: '12' });
	expect(control(c).querySelector('.juneau-co-earlier') === null, 'load earlier withdrawn for good');
	const d = mkConsole(t, { maxDomRows: 2 });
	d.api.append(lines(1, 3));
	expect(control(d).querySelector('.juneau-co-earlier-notice').textContent === 'Earlier lines not shown'
		&& control(d).querySelector('a') === null, 'no link without downloadUrl');
});

test('earlier11_prependRespectsTheCap', async function (t) {
	const L = loader();
	const c = mkConsole(t, { maxDomRows: 5, loadEarlier: L.fn });
	c.api.append(lines(6, 8));
	c.api.prepend([], { hasEarlier: true, before: '5' });
	control(c).querySelector('.juneau-co-earlier').dispatch('click', {});
	L.calls[0].resolve({ lines: lines(1, 5), hasEarlier: true, before: '0' });
	await E.flush();
	expectSame(c.ns(), [4, 5, 6, 7, 8], 'only the newest slice that fits');
	expect(control(c).querySelector('.juneau-co-earlier-notice') !== null, 'notice');
});

test('earlier12_queuedTargetAutoLoads', async function (t) {
	const L = loader();
	const c = mkConsole(t, { anchorPrefix: 'L', loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	expect(c.api.scrollToLine(30) === false && L.calls.length === 1, 'auto-load started');
	L.calls[0].resolve({ lines: lines(36, 40), hasEarlier: true, before: '35' });
	await E.flush();
	expect(L.calls.length === 2 && L.calls[1].before === '35', 'kept loading');
	L.calls[1].resolve({ lines: lines(26, 35), hasEarlier: true, before: '25' });
	await E.flush();
	expect(L.calls.length === 2, 'stopped once the row exists');
	expect(c.row(30).classList.contains('juneau-co-target'), 'revealed');
	expect(t.rec.warnsMatching('not found').length === 0, 'no E-CO-8');
});

test('earlier13_autoLoadExhaustedFallsBack', async function (t) {
	const L = loader();
	const c = mkConsole(t, { anchorPrefix: 'L', loadEarlier: L.fn });
	c.api.append(lines(41, 45));
	c.api.prepend([], { hasEarlier: true, before: '40' });
	c.api.scrollToLine(10);
	L.calls[0].resolve({ lines: lines(30, 40), hasEarlier: false });
	await E.flush();
	expect(t.rec.warnsMatching('line 10 not found').length === 1, 'E-CO-8');
	expect(c.row(30).classList.contains('juneau-co-target'), 'nearest following row');
	const d = mkConsole(t, { anchorPrefix: 'M', maxDomRows: 3, downloadUrl: '/dl' });
	d.api.append(lines(1, 6));
	d.api.scrollToLine(2);
	expect(t.rec.warnsMatching('line 2 not found').length === 1, 'trimmed target');
	expect(t.env.getActive() === control(d).querySelector('a'), 'focus on the notice link');
});

// @cases:tooltip

function tipConsole(t, opts) {
	const c = mkConsole(t, opts);
	c.api.append({ frags: [{ text: 'step ' }, { block: true, style: 'success', tooltip: 'built\nin 3s' }, { block: true, href: '#L1', label: 'go' }] });
	c.tip = c.q('.juneau-co-tooltip');
	c.blk = c.q('[data-juneau-co-tip]');
	return c;
}

test('tip01_hoverShowsAndHides', function (t) {
	const c = tipConsole(t);
	expect(c.tip.hidden === true && c.tip.getAttribute('role') === 'tooltip', 'hidden at start');
	c.pane.dispatch('mouseover', { target: c.blk });
	expect(c.tip.hidden === false && c.tip.textContent === 'built\nin 3s', 'shown with newlines kept');
	expect(c.blk.getAttribute('aria-describedby') === c.tip.id, 'described by the tooltip');
	c.pane.dispatch('mouseout', { target: c.blk, relatedTarget: null });
	expect(c.tip.hidden === true && c.blk.getAttribute('aria-describedby') === null, 'hidden and unlinked');
});

test('tip02_focusAndEscape', function (t) {
	const c = tipConsole(t);
	c.pane.dispatch('focusin', { target: c.blk });
	expect(c.tip.hidden === false, 'focus shows');
	c.pane.dispatch('keydown', { target: c.blk, key: 'Escape' });
	expect(c.tip.hidden === true, 'Escape hides');
	c.pane.dispatch('focusin', { target: c.blk });
	c.pane.dispatch('focusout', { target: c.blk });
	expect(c.tip.hidden === true, 'blur hides');
});

test('tip03_positionStaysInTheViewport', function (t) {
	const c = tipConsole(t);
	t.env.window.innerWidth = 800;
	t.env.window.innerHeight = 600;
	c.tip.getBoundingClientRect = function () { return { left: 0, top: 0, right: 200, bottom: 40, width: 200, height: 40 }; };
	c.blk.getBoundingClientRect = function () { return { left: 790, top: 10, right: 800, bottom: 22, width: 10, height: 12 }; };
	c.pane.dispatch('mouseover', { target: c.blk });
	expect(c.tip.style.left === '596px' && c.tip.style.top === '26px', 'clamped right, below: ' + c.tip.style.left + ',' + c.tip.style.top);
	c.blk.getBoundingClientRect = function () { return { left: 1, top: 580, right: 11, bottom: 592, width: 10, height: 12 }; };
	c.pane.dispatch('mouseover', { target: c.blk });
	expect(c.tip.style.left === '4px' && c.tip.style.top === '536px', 'clamped left, flipped above: ' + c.tip.style.left + ',' + c.tip.style.top);
});

test('tip04_onlyTooltipBlocksTrigger', function (t) {
	const c = tipConsole(t);
	c.pane.dispatch('mouseover', { target: c.q('.juneau-co-gutter') });
	c.pane.dispatch('mouseover', { target: c.q('a.juneau-co-block') });
	expect(c.tip.hidden === true, 'a gutter or a label-only link shows nothing');
	c.pane.dispatch('mouseover', { target: c.blk });
	c.pane.dispatch('mouseout', { target: c.blk, relatedTarget: c.blk });
	expect(c.tip.hidden === false, 'moving within the block keeps it');
});

test('tip05_destroyUnlinks', function (t) {
	const c = tipConsole(t);
	c.pane.dispatch('mouseover', { target: c.blk });
	c.api.destroy();
	expect(c.q('.juneau-co-tooltip') === null && c.blk.getAttribute('aria-describedby') === null, 'tooltip removed and unlinked');
});

// @cases:markers

function root(c) { return c.q('.juneau-co'); }

test('mk01_toggleAppearsAfterFirstMarker', function (t) {
	const c = mkConsole(t, { id: 'j' });
	const btn = c.q('.juneau-co-markers');
	expect(root(c).classList.contains('juneau-co-markers-dim'), 'default mode dim');
	c.api.append({ text: 'plain' });
	expect(btn.hidden === true, 'hidden with no marker');
	c.api.append({ text: '##run step', ui: { marker: true } });
	expect(btn.hidden === false && btn.textContent === 'Hide markers' && btn.getAttribute('aria-pressed') === 'false', 'toggle shown');
	expect(c.row(2).classList.contains('juneau-co-marker') && c.row(2).hasAttribute('data-marker'), 'marker row');
});

test('mk02_toggleSwitchesAndPersists', function (t) {
	const c = mkConsole(t, { id: 'j' });
	c.api.append({ text: '##run', ui: { marker: true } });
	const btn = c.q('.juneau-co-markers');
	btn.dispatch('click', {});
	expect(root(c).classList.contains('juneau-co-markers-hide') && !root(c).classList.contains('juneau-co-markers-dim'), 'hide');
	expect(btn.getAttribute('aria-pressed') === 'true' && btn.textContent === 'Hide markers', 'pressed; label unchanged');
	expect(t.env.window.sessionStorage.getItem('juneau-co:markers:j') === 'hide', 'stored');
	btn.dispatch('click', {});
	expect(root(c).classList.contains('juneau-co-markers-dim') && t.env.window.sessionStorage.getItem('juneau-co:markers:j') === 'dim', 'back to dim');
	const s = mkConsole(t, { id: 'k', markers: 'show' });
	s.api.append({ text: '##run', ui: { marker: true } });
	s.q('.juneau-co-markers').dispatch('click', {});
	s.q('.juneau-co-markers').dispatch('click', {});
	expect(root(s).classList.contains('juneau-co-markers-show'), 'returns to the author mode');
});

test('mk03_storedChoiceWins', function (t) {
	t.env.window.sessionStorage.setItem('juneau-co:markers:j', 'hide');
	t.env.window.sessionStorage.setItem('juneau-co:markers:k', 'blink');
	const c = mkConsole(t, { id: 'j', markers: 'show' });
	expect(root(c).classList.contains('juneau-co-markers-hide') && c.q('.juneau-co-markers').getAttribute('aria-pressed') === 'true', 'restored');
	const d = mkConsole(t, { id: 'k', markers: 'show' });
	expect(root(d).classList.contains('juneau-co-markers-show'), 'an invalid stored value is ignored');
});

test('mk04_setMarkersApi', function (t) {
	const c = mkConsole(t, { id: 'j' });
	expect(c.api.setMarkers('show') === true && root(c).classList.contains('juneau-co-markers-show'), 'show');
	expect(c.api.setMarkers('loud') === false && root(c).classList.contains('juneau-co-markers-show'), 'invalid mode ignored');
	t.env.window.sessionStorage.setItem = function () { throw new Error('quota'); };
	expect(c.api.setMarkers('hide') === true && root(c).classList.contains('juneau-co-markers-hide'), 'storage failure ignored');
});

test('mk05_classify', function (t) {
	const c = mkConsole(t, { classify: function (l) { return l.text && l.text.startsWith('##') ? 'marker' : null; } });
	c.api.append([{ text: '##run a' }, { text: 'b' }, { text: 'c', ui: { marker: true } }]);
	expect(c.row(1).hasAttribute('data-marker') && !c.row(2).hasAttribute('data-marker') && c.row(3).hasAttribute('data-marker'),
		'classifier marks; server marker wins');
	const d = mkConsole(t, { classify: function () { throw new Error('boom'); } });
	d.api.append({ text: 'x' });
	expect(d.rows().length === 1 && !d.row(1).hasAttribute('data-marker') && t.rec.warnsMatching('classify() threw').length === 1, 'throwing classifier');
});

test('mk06_hiddenMarkerTargetIsRevealed', function (t) {
	const c = mkConsole(t, { anchorPrefix: 'L', markers: 'hide' });
	c.api.append([{ text: 'a' }, { text: '##run', ui: { marker: true } }, { text: 'c' }]);
	c.api.scrollToLine(2);
	expect(c.row(2).classList.contains('juneau-co-reveal') && c.row(2).classList.contains('juneau-co-target'), 'revealed');
	c.api.scrollToLine(3);
	expect(!c.row(2).classList.contains('juneau-co-reveal'), 'hidden again after the next navigation');
});

test('mk07_noStubsLeft', function (t) {
	const src = require('node:fs').readFileSync(argv[4], 'utf8');
	expect(src.indexOf('Forward declarations') < 0, 'the forward-declaration block is gone');
	const names = ['wire', 'isPaneStuck', 'afterAppend', 'resolveQueuedTarget', 'onTrimmed', 'markerSeen', 'prepend', 'setStatus',
		'scrollToLine', 'setMarkers', 'autoLoadEarlier', 'wireTooltip', 'wireMarkers'];
	const dup = names.filter(function (n) { return src.split('function ' + n + '(').length !== 2; });
	expect(dup.length === 0, 'each function declared exactly once: ' + dup.join(', '));
});

// @cases:end

(async function main() {
	const out = {};
	for (const c of cases) {
		try {
			await c.fn(E.load(argv));
			out[c.name] = true;
		} catch (e) {
			out[c.name] = String((e && e.stack) || e);
		}
	}
	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String((e && e.stack) || e));
	process.exit(1);
});
