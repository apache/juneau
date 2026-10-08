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
 * console-output-env-check.cjs - proves the console-output test environment's own decorations (fragments, scroll
 * geometry, scrollIntoView, sessionStorage, location/hashchange, visibility, fake Date) before any module test relies
 * on them.
 *
 *   Usage:  node console-output-env-check.cjs <renders> <views> <regions> <helpers> <console-output> <vectors.json>
 */
'use strict';

const path = require('node:path');
const E = require(path.join(__dirname, 'console-output-env.cjs'));

const argv = process.argv.slice(2);
if (argv.length < 6) {
	process.stderr.write('usage: console-output-env-check.cjs <renders> <views> <regions> <helpers> <console-output> <vectors>\n');
	process.exit(2);
}

const out = {};
const t = E.load(argv);
const doc = t.env.document;

// t01 fragments: appending a fragment moves its children, in order, and leaves it empty.
(function () {
	const host = doc.createElement('div');
	const f = doc.createDocumentFragment();
	const a = doc.createElement('p'); a.textContent = 'a';
	const b = doc.createElement('p'); b.textContent = 'b';
	f.appendChild(a); f.appendChild(b);
	host.appendChild(f);
	const ref = doc.createElement('p'); ref.textContent = 'z';
	host.appendChild(ref);
	const g = doc.createDocumentFragment();
	const c = doc.createElement('p'); c.textContent = 'c';
	g.appendChild(c);
	host.insertBefore(g, ref);
	out.t01_fragments = host.textContent === 'abcz' && f.childNodes.length === 0 && a.parentNode === host
		&& host.children.length === 4 && c.previousSibling === b && c.nextSibling === ref;
})();

// t02 scroll geometry: ROW_H per visible element child; scrollTop clamps; hidden children take no height.
(function () {
	const pane = doc.createElement('div');
	pane.clientHeight = 100;
	for (let i = 0; i < 10; i++) pane.appendChild(doc.createElement('div'));
	const h1 = pane.scrollHeight;
	pane.scrollTop = 9999;
	const clamped = pane.scrollTop;
	pane.children[0].hidden = true;
	const h2 = pane.scrollHeight;
	out.t02_geometry = h1 === 10 * E.ROW_H && clamped === 10 * E.ROW_H - 100 && h2 === 9 * E.ROW_H
		&& pane.children[2].offsetTop === 1 * E.ROW_H;
})();

// t03 scrollIntoView records and centres the row in its parent.
(function () {
	const pane = doc.createElement('div');
	pane.clientHeight = 100;
	for (let i = 0; i < 20; i++) { const r = doc.createElement('div'); r.setAttribute('data-n', String(i + 1)); pane.appendChild(r); }
	pane.children[10].scrollIntoView({ block: 'center' });
	const rec = t.scrolled[t.scrolled.length - 1];
	out.t03_scrollIntoView = rec.n === '11' && rec.block === 'center' && pane.scrollTop === 10 * E.ROW_H - 50 + E.ROW_H / 2;
})();

// t04 userScroll fires a scroll event; fireScroll fires one without moving.
(function () {
	const pane = doc.createElement('div');
	pane.clientHeight = 40;
	for (let i = 0; i < 10; i++) pane.appendChild(doc.createElement('div'));
	let seen = [];
	pane.addEventListener('scroll', function () { seen.push(pane.scrollTop); });
	E.userScroll(pane, 60);
	E.fireScroll(pane);
	out.t04_scrollEvents = seen.length === 2 && seen[0] === 60 && seen[1] === 60;
})();

// t05 sessionStorage, location.hash + hashchange, document.hidden + visibilitychange.
(function () {
	const w = t.env.window;
	w.sessionStorage.setItem('k', 'v');
	let hashes = 0, vis = 0;
	w.addEventListener('hashchange', function () { hashes++; });
	doc.addEventListener('visibilitychange', function () { vis++; });
	t.setHash('#L5');
	t.setHidden(true);
	const hiddenNow = doc.hidden === true;
	t.setHidden(false);
	const removed = function () { hashes += 100; };
	w.addEventListener('hashchange', removed);
	w.removeEventListener('hashchange', removed);
	t.setHash('#L6');
	out.t05_windowState = w.sessionStorage.getItem('k') === 'v' && w.location.hash === '#L6' && hashes === 2
		&& hiddenNow && vis === 2 && doc.hidden === false;
})();

// t06 fake Date follows the fake clock; Date.parse still works.
(function () {
	const D = t.sandbox.Date;
	const a = D.now();
	t.clock.advance(1500);
	const b = D.now();
	out.t06_fakeDate = b - a === 1500 && D.parse('2026-10-07T00:00:00.000Z') === 1791331200000
		&& new D().getTime() === b;
})();

// t07 the module and vectors were loaded.
out.t07_loaded = typeof t.NS.consoleOutput === 'object' && Array.isArray(t.vectors.color.accept);

process.stdout.write(JSON.stringify(out));
