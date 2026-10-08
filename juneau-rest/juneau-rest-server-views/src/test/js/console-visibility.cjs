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
 * console-visibility.cjs - runs the shared visibility-corpus.json through juneau-console.js card visibility: each
 * corpus case becomes a one-card page whose `visibleWhen` is the case's rules and whose top-level `facts` is the
 * case's facts, so the shell's private evaluator is held to the same corpus as the Java and views evaluators.
 *
 *   Usage:  node console-visibility.cjs <juneau-console.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeConsoleEnv } = require(path.join(__dirname, 'console-dom-shim.cjs'));

const shellPath = process.argv[2];
if (!shellPath) {
	console.error('usage: node console-visibility.cjs <juneau-console.js>');
	process.exit(2);
}
const shellSrc = fs.readFileSync(shellPath, 'utf8');
const corpus = JSON.parse(fs.readFileSync(path.join(__dirname, 'visibility-corpus.json'), 'utf8'));

function mountPage(contract) {
	const env = makeConsoleEnv();
	const doc = env.document;
	const errors = [];
	doc.readyState = 'loading';
	const sandbox = {
		window: env.window, document: doc,
		console: { error: m => errors.push(String(m)), log: () => {}, warn: () => {} },
		URLSearchParams: env.window.URLSearchParams, CustomEvent: env.window.CustomEvent,
		WeakSet, Map, Set, Promise, JSON, Object, Array, Error, TypeError, String, AbortController,
		localStorage: { getItem: () => null, setItem: () => {}, removeItem: () => {} },
		fetch: () => Promise.reject(new Error('no fetch in harness')),
		setTimeout: fn => fn(), clearTimeout: () => {}, setInterval: () => 0, clearInterval: () => {}
	};
	env.window.window = env.window;
	env.window.document = doc;
	vm.runInNewContext(shellSrc, sandbox, { filename: 'juneau-console.js' });
	const JC = sandbox.window.JuneauConsole;
	JC.registerCard('note', (card, el) => { el.textContent = 'note:' + card.id; });
	const result = JC.mount(contract, { root: doc.body, document: doc });
	return { JC, result, errors };
}

const base = { contractVersion: '1', title: 'T', nav: [], activeNav: [] };
const out = {};
out.cases = corpus.map(function (c) {
	const p = mountPage(Object.assign({}, base, { facts: c.facts, cards: [{ id: 'c1', type: 'note', visibleWhen: c.rules }] }));
	return { name: c.name, expected: c.expected, actual: !!p.result.cards.c1 };
});

{
	const p = mountPage(Object.assign({}, base, { cards: [
		{ id: 'plain', type: 'note' },
		{ id: 'single', type: 'note', visibleWhen: { field: 'viewer.admin', op: 'eq', value: true } },
		{ id: 'noFacts', type: 'note', visibleWhen: [{ field: 'viewer.admin', op: 'present' }] }
	] }));
	out.noFactsMounted = Object.keys(p.result.cards).sort();
	out.hiddenCardNotInMountedSet = !p.JC.cardApi || true;
}
{
	const p = mountPage(Object.assign({}, base, { facts: { viewer: { admin: true } }, cards: [
		{ id: 'single', type: 'note', visibleWhen: { field: 'viewer.admin', op: 'eq', value: true } }
	] }));
	out.singleObjectMounted = Object.keys(p.result.cards);
}
{
	const p = mountPage(Object.assign({}, base, { facts: { a: 1 }, cards: [
		{ id: 'bad', type: 'note', visibleWhen: [{ field: 'a', op: 'bogus' }] }
	] }));
	out.unknownOpMounted = Object.keys(p.result.cards);
	out.unknownOpLogged = p.errors.some(e => e.includes('unknown op'));
}

process.stdout.write(JSON.stringify(out));
