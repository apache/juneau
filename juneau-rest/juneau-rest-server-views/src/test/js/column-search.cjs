/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * column-search.cjs - always-on Node parity harness for juneau-search.js.
 *
 * Loads the served juneau-search.js asset (path: process.argv[2]) into a vm sandbox with a minimal `window`
 * global, then runs the SAME shared JSON corpus (path: process.argv[3]) the Java-side SearchCorpus_Test runs
 * against the Java engine, emitting one pass/fail flag per case id. ColumnSearch_Parity_Test asserts every id is
 * true, so the client-mode grid is proven to filter byte-for-byte the way the server would.
 *
 * Each corpus case is self-contained: rows, column types, and the search string. Multi-column cases (group
 * I_multiColumn) carry object rows and a comma-joined "col=expr,col2=expr2" search string; single-column cases
 * carry scalar rows (wrapped here into {v: value} objects) and a "v=expr" search string. Row matching is checked
 * by reference equality against the SAME row objects passed into engine.rows(...), exactly mirroring how
 * engine.search(...) itself returns row references, never copies.
 *
 *   Usage:  node column-search.cjs <juneau-search.js> <search-corpus.json>
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const searchJsPath = process.argv[2];
const corpusPath = process.argv[3];
if (!searchJsPath || !corpusPath) {
	console.error('usage: node column-search.cjs <juneau-search.js> <search-corpus.json>');
	process.exit(2);
}

const context = vm.createContext({ window: {} });
// NOSONAR javascript:S1523 -- loading a production JS source into a VM sandbox is this harness's intended mechanism; the input is a fixed local file supplied by the test.
vm.runInContext(fs.readFileSync(searchJsPath, 'utf8'), context, { filename: 'juneau-search.js' });
const S = context.window.JuneauViews?.search;
if (!S || typeof S.createEngine !== 'function' || typeof S.parseExprStrict !== 'function' || typeof S.resolveStrict !== 'function') {
	console.error('juneau-search.js did not publish window.JuneauViews.search');
	process.exit(1);
}

const cases = JSON.parse(fs.readFileSync(corpusPath, 'utf8'));

// -----------------------------------------------------------------------------------------------------------------
// Report plumbing.
// -----------------------------------------------------------------------------------------------------------------

const report = {};
const failures = [];
let count = 0;

/** Records one case's pass/fail flag. */
function checkBool(id, pass) {
	count++;
	report[id] = !!pass;
	if (!pass)
		failures.push({ id, pass });
}

function arrEq(a, b) {
	if (a.length !== b.length)
		return false;
	for (let i = 0; i < a.length; i++)
		if (a[i] !== b[i])
			return false;
	return true;
}

// -----------------------------------------------------------------------------------------------------------------
// Row/term plumbing - mirror of SearchCorpus_Test's rowsOf(...)/contextFor(...)/terms-building.
// -----------------------------------------------------------------------------------------------------------------

function isPlainObject(x) {
	return x !== null && typeof x === 'object' && !Array.isArray(x);
}

/** Multi-column cases already carry object rows; single-column cases wrap scalars as {v: value}. */
function normalizeRows(rows) {
	if (rows.length && rows.every(isPlainObject))
		return rows;
	return rows.map(v => ({ v }));
}

/** Depth-aware split: ignores `sep` while inside parentheses, so operator-arg commas are not term separators. */
function splitTopLevel(s, sep) {
	const out = [];
	let depth = 0;
	let start = 0;
	for (let i = 0; i < s.length; i++) {
		const c = s[i];
		if (c === '(') depth++;
		else if (c === ')') depth--;
		else if (c === sep && depth === 0) {
			out.push(s.slice(start, i));
			start = i + 1;
		}
	}
	out.push(s.slice(start));
	return out;
}

/** Builds engine.search(...)'s `terms` array from a case's "col=expr[,col2=expr2...]" search string. */
function termsOf(kase) {
	const search = (kase.search || '');
	if (search.trim() === '')
		return [];
	return splitTopLevel(search, ',').map(piece => {
		const idx = piece.indexOf('=');
		const column = piece.slice(0, idx);
		const expression = piece.slice(idx + 1);
		return { column, type: String(kase.types[column] || '').toLowerCase(), expression };
	});
}

/** The only shared custom operator in the corpus; registered to mirror SearchCorpus_Test's STARTS_CI exactly. */
function registerCustoms(engine, operators) {
	if (!operators?.custom)
		return;
	operators.custom.forEach(name => {
		if (name === '$startsCI') {
			engine.custom('$startsCI', (cell, args) => String(cell).toLowerCase().startsWith(String(args[0]).toLowerCase()));
		} else {
			throw new Error('column-search.cjs: no JS predicate registered for custom operator ' + name);
		}
	});
}

function rowsMatch(kase) {
	const rows = normalizeRows(kase.rows);
	const cols = Object.keys(kase.types || {});
	const engine = S.createEngine().rows(rows);
	cols.forEach(col => {
		engine.accessor(col, r => r[col]);
	});
	registerCustoms(engine, kase.operators);
	const actual = engine.search(termsOf(kase));
	const expected = (kase.matches || []).map(i => rows[i]);
	return arrEq(actual, expected);
}

// -----------------------------------------------------------------------------------------------------------------
// Tree/error plumbing - mirror of CorpusTrees.of(...).
// -----------------------------------------------------------------------------------------------------------------

/** Converts a parseExprStrict node ({name, value, args}) to the corpus's canonical {lit}/{op,args} shape. */
function treeOf(node) {
	if (node.name === null)
		return { lit: node.value };
	return { op: node.name, args: node.args.map(treeOf) };
}

function deepEqual(a, b) {
	if (a === b)
		return true;
	if (a === null || b === null || typeof a !== 'object' || typeof b !== 'object')
		return false;
	if (Array.isArray(a) !== Array.isArray(b))
		return false;
	if (Array.isArray(a)) {
		if (a.length !== b.length)
			return false;
		for (let i = 0; i < a.length; i++)
			if (!deepEqual(a[i], b[i]))
				return false;
		return true;
	}
	const ka = Object.keys(a), kb = Object.keys(b);
	if (ka.length !== kb.length)
		return false;
	for (const k of ka) {
		if (!Object.hasOwn(b, k) || !deepEqual(a[k], b[k]))
			return false;
	}
	return true;
}

function exprStringOf(search) {
	return search.slice(search.indexOf('=') + 1);
}

/** The column SearchType wire token for a single-column case ("v=expr"), or null for multi-column searches. */
function typeOf(kase) {
	const cols = Object.keys(kase.types || {});
	return cols.length === 1 ? String(kase.types[cols[0]]).toLowerCase() : null;
}

/** Mirror of QueryResolver: syntax errors, then OPERATOR_TYPE, then BAD_VALUE. */
function throwsWithCode(expr, code, type) {
	try { S.resolveStrict(expr, type); return false; }
	catch (error) { return error.code === code; }
}

/** True when a non-error single-column case resolves without throwing (Java's ctx.resolve must not throw either). */
function resolvesCleanly(kase) {
	const type = typeOf(kase);
	if (type === null || (kase.operators?.custom))
		return true;  // Custom operators are unknown to the builtin-only strict path.
	try { S.resolveStrict(exprStringOf(kase.search), type); return true; }
	catch (error) { return false; }
}

// -----------------------------------------------------------------------------------------------------------------
// Per-case dispatch.
// -----------------------------------------------------------------------------------------------------------------

function runCase(kase) {
	let pass = true;
	if (kase.error) {
		pass = throwsWithCode(exprStringOf(kase.search), kase.error.code, typeOf(kase));
	} else {
		pass = resolvesCleanly(kase);
		if (kase.tree) {
			let ok = false;
			try { ok = deepEqual(treeOf(S.parseExprStrict(exprStringOf(kase.search))), kase.tree.expr); }
			catch (error) { ok = false; }
			pass = pass && ok;
		}
		if (kase.rows)
			pass = pass && rowsMatch(kase);
	}
	checkBool(kase.id, pass);
}

cases.forEach(runCase);

// -----------------------------------------------------------------------------------------------------------------
// Fixed request-time checks (the corpus cannot express a request time): relative-duration literals on a TIMESTAMP
// column resolve against ONE instant, exactly like Java's QueryResolver (mirrored by QueryResolver_Test.H_relativeDuration).
// These are reported under their own "rt_*" keys and do NOT count toward caseCount (which tracks corpus ids only).
// -----------------------------------------------------------------------------------------------------------------

function checkExtra(name, pass) {
	report[name] = !!pass;
	if (!pass)
		failures.push({ id: name, pass: false });
}

const T0 = Date.parse('2026-09-30T12:00:00Z');
const HOUR = 3600000;

checkExtra('rt_matches_gteMinus24h', S.matches('$gte(-24h)', T0 - 23 * HOUR, 'timestamp', null, T0) === true
	&& S.matches('$gte(-24h)', T0 - 25 * HOUR, 'timestamp', null, T0) === false);
checkExtra('rt_matches_dateObjectAnchor', S.matches('$lt(+1h)', T0, 'timestamp', null, new Date(T0)) === true
	&& S.matches('$lt(+1h)', T0 + 2 * HOUR, 'timestamp', null, new Date(T0)) === false);
checkExtra('rt_matches_eqExactInstant', S.matches('-24h', T0 - 24 * HOUR, 'timestamp', null, T0) === true);
checkExtra('rt_matches_numericIgnoresRequestTime', S.matches('$gte(-24h)', -86400000, 'numeric', null, T0) === true
	&& S.matches('$gte(-24h)', -86400001, 'numeric', null, T0 + 999999) === false);
checkExtra('rt_matches_bareNegativeIsEpochMillis', S.matches('-5', -5, 'timestamp', null, T0) === true);
{
	// $between(-7d,-1d): both ends share the engine's single request time.
	const DAY = 24 * HOUR;
	const rows = [{ t: T0 - 8 * DAY }, { t: T0 - 3 * DAY }, { t: T0 - DAY / 2 }];
	const found = S.createEngine().rows(rows).accessor('t', r => r.t).requestTime(T0)
		.search([{ column: 't', type: 'timestamp', expression: '$between(-7d,-1d)' }]);
	checkExtra('rt_engine_between', found.length === 1 && found[0] === rows[1]);
}
checkExtra('rt_validity_relativeOnlyForNumericAndTimestamp', S.isValidValue('numeric', '-24h') && S.isValidValue('timestamp', '+90ms')
	&& ! S.isValidValue('timestamp', '+1H') && ! S.isValidValue('numeric', '-1M') && ! S.isValidValue('timestamp', '+106751991167d') && ! S.isValidValue('boolean', '-24h') && ! S.isValidValue('timestamp', '-24x')
	&& ! S.isValidValue('timestamp', '+24') && S.isValidValue('text', '-24h'));

checkExtra('rt_iso_validity', S.isValidValue('numeric', 'PT24H') && S.isValidValue('numeric', '-PT24H') && S.isValidValue('timestamp', '+PT30M')
	&& S.isValidValue('timestamp', 'P1D') && S.isValidValue('numeric', 'P1DT1H1M1.5S') && S.isValidValue('timestamp', 'pt1,5s')
	&& ! S.isValidValue('numeric', 'PTX') && ! S.isValidValue('numeric', 'P') && ! S.isValidValue('numeric', 'PT') && ! S.isValidValue('numeric', 'P1DT')
	&& ! S.isValidValue('numeric', 'PT-1H') && ! S.isValidValue('numeric', '-PT-1H') && ! S.isValidValue('timestamp', 'P1Y') && ! S.isValidValue('timestamp', 'P1M')
	&& ! S.isValidValue('numeric', 'PT9223372036854775807H') && ! S.isValidValue('timestamp', 'PT24') && ! S.isValidValue('boolean', 'PT24H') && S.isValidValue('text', 'PT24H'));
checkExtra('rt_iso_matchesParityWithRelativeLiteral', S.matches('$gte(-PT24H)', T0 - 23 * HOUR, 'timestamp', null, T0) === true
	&& S.matches('$gte(-PT24H)', T0 - 25 * HOUR, 'timestamp', null, T0) === false
	&& S.matches('-PT24H', T0 - 24 * HOUR, 'timestamp', null, T0) === true
	&& S.matches('$gte(-PT24H)', -86400000, 'numeric', null, T0) === true
	&& S.matches('$gte(P1D)', 86400000, 'numeric', null, T0) === true
	&& S.matches('$lt(P1D)', 86400000, 'numeric', null, T0) === false);

report.caseCount = count;
report.allPass = failures.length === 0;
report.failures = failures;
process.stdout.write(JSON.stringify(report));
