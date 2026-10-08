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
 * rules.cjs - runs visibility-corpus.json (shared with VisibilityRule_Corpus_Test) through JuneauViews.rules.test,
 * so the Java and JS evaluators are proven to agree on every case, then exercises the facts-aware row scope.
 *
 *   Usage:  node rules.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node rules.cjs <juneau-views.js>');
	process.exit(2);
}

const document = {
	readyState: 'loading',
	addEventListener: function () { /* no-op */ },
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () { return {}; },
	body: { appendChild: function () { /* no-op */ }, querySelectorAll: function () { return []; } }
};
let pageContract = null;
const errors = [];
const fakeConsole = { error: function (m) { errors.push(String(m)); }, warn: function () { /* no-op */ }, log: function () { /* no-op */ } };
const window = { document: document, console: fakeConsole, JuneauConsole: { contract: function () { return pageContract; } } };
const sandbox = { window: window, document: document, console: fakeConsole };
sandbox.globalThis = sandbox;

// NOSONAR javascript:S1523 -- the harness's purpose is to load the production runtime under test (a repo-local path
// from argv) into an isolated VM sandbox.
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const corpus = JSON.parse(fs.readFileSync(path.join(__dirname, 'visibility-corpus.json'), 'utf8'));
const out = {};
out.cases = corpus.map(function (c) { return { name: c.name, expected: c.expected, actual: NS.rules.test(c.rules, c.facts) }; });
out.frozen = Object.isFrozen(NS.rules);

pageContract = { facts: { viewer: { roles: ['admin'] } } };
out.factsRead = JSON.stringify(NS.rules.facts());
out.rowScopeSeesFacts = NS.rules.testRow([{ field: 'facts.viewer.roles', op: 'contains', value: 'admin' }], { status: 'open' });
out.rowScopeSeesRow = NS.rules.testRow([{ field: 'status', op: 'eq', value: 'open' }], { status: 'open' });
out.rowScopeBoth = NS.rules.testRow([{ field: 'status', op: 'eq', value: 'open' }, { field: 'facts.viewer.roles', op: 'contains', value: 'ops' }], { status: 'open' });
out.singleRuleObject = NS.rules.test({ field: 'a', op: 'present' }, { a: 1 });

pageContract = null;
out.noContractFacts = JSON.stringify(NS.rules.facts());
out.noContractFailsClosed = NS.rules.testRow([{ field: 'facts.viewer.roles', op: 'contains', value: 'admin' }], {});

const before = errors.length;
out.unknownOp = NS.rules.test([{ field: 'a', op: 'bogus' }], { a: 1 });
out.unknownOpLogged = errors.length === before + 1 && errors[errors.length - 1].includes('E-JS-73');

process.stdout.write(JSON.stringify(out));
