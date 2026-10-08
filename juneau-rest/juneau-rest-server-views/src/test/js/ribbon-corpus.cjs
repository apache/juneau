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
 * ribbon-corpus.cjs - JS layer of the ribbon parity corpus.
 *
 * Usage:  node ribbon-corpus.cjs <juneau-ribbon.js> <juneau-search.js> <ribbon-corpus.json>
 *
 * For every case, computes the ribbon encoding, the user/ribbon merge (through the real mergeColumnSearches), the
 * derived default state and the client-mode row filter, and prints {cases:[{name, columnSearchesOk, queryParamsOk,
 * mergedOk, rowIdsOk, consoleErrorOk, stateOk, actual}]}.  Every case gets a FRESH environment so localStorage and
 * module state cannot leak between cases.  Every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const [ribbonJs, searchJs, corpusFile] = process.argv.slice(2);
if (!corpusFile) {
	console.error('usage: node ribbon-corpus.cjs <juneau-ribbon.js> <juneau-search.js> <ribbon-corpus.json>');
	process.exit(2);
}
const corpus = JSON.parse(fs.readFileSync(corpusFile, 'utf8'));
const sources = [searchJs, ribbonJs].map(function (f) { return { file: f, code: fs.readFileSync(path.resolve(f), 'utf8') }; });

// Stable JSON for comparison (sorted keys; the corpus keys are dtIndex strings or ids, so order is not meaningful).
function canon(v) {
	if (v == null) return 'null';
	if (Array.isArray(v)) return '[' + v.map(canon).join(',') + ']';
	if (typeof v === 'object') return '{' + Object.keys(v).sort().map(function (k) { return JSON.stringify(k) + ':' + canon(v[k]); }).join(',') + '}';
	return JSON.stringify(v);
}
function same(a, b) { return canon(a) === canon(b); }

function load(errors) {
	const env = makeEnv();
	const sandbox = {
		window: env.window, document: env.document,
		console: { error: function () { errors.push(Array.prototype.join.call(arguments, ' ')); }, warn: function () { /* ignored */ }, log: function () { /* ignored */ } },
		setTimeout: function () { return 0; }, clearTimeout: function () { /* no-op */ },
		setInterval: function () { return 0; }, clearInterval: function () { /* no-op */ }
	};
	sources.forEach(function (s) {
		vm.runInNewContext(s.code, sandbox, { filename: path.basename(s.file) }); // NOSONAR javascript:S1523 -- the harness evaluates the module's own bundled script
	});
	return { env: env, NS: env.window.JuneauViews };
}

function runCase(c) {
	const errors = [];
	try {
		const { env, NS } = load(errors);
		const R = NS.ribbon;
		const columns = c.columns || corpus.columns;
		const viewDef = { id: 'corpus', columns: columns, ribbon: c.ribbon };

		let state = c.state;
		if (c.persisted) {
			Object.keys(c.persisted).forEach(function (id) {
				env.window.localStorage.setItem(R.ribbonStorageKey('corpus', id), c.persisted[id]);
			});
			state = R.loadPersistedState(viewDef);
		}

		const columnSearches = R.ribbonColumnSearches(viewDef, state, columns);
		const queryParams = R.ribbonQueryParams(viewDef, state);
		const merged = R.mergeColumnSearches(c.userSearch || {}, columnSearches);
		const expectedMerged = c.expect.merged || Object.assign({}, c.userSearch || {}, c.expect.columnSearches || {});

		let rowIds = null;
		if (c.expect.rowIds != null) {
			if (typeof R.clientRowFilter !== 'function') rowIds = 'clientRowFilter missing';
			else {
				const filter = R.clientRowFilter(merged, columns, NS.search.createEngine);
				rowIds = corpus.rows.filter(function (row) { return filter == null || filter(row); }).map(function (row) { return row.id; });
			}
		}

		return {
			name: c.name,
			stateOk: c.expect.state == null || same(state, c.expect.state),
			columnSearchesOk: same(columnSearches, c.expect.columnSearches || {}),
			queryParamsOk: same(queryParams, c.expect.queryParams || {}),
			mergedOk: same(merged, expectedMerged),
			rowIdsOk: c.expect.rowIds == null || same(rowIds, c.expect.rowIds),
			consoleErrorOk: c.expect.consoleError == null
				? errors.length === 0
				: errors.some(function (e) { return e.indexOf(c.expect.consoleError) >= 0; }),
			actual: { state: state, columnSearches: columnSearches, queryParams: queryParams, merged: merged, rowIds: rowIds, errors: errors }
		};
	} catch (e) {
		return { name: c.name, threw: String(e && e.stack || e) };
	}
}

process.stdout.write(JSON.stringify({ cases: corpus.cases.map(runCase) }));
