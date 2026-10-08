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
 * run-view.cjs - the run-view module's harness (no network): checks, the reducer, rendering, tooltip, notes,
 * incremental rendering and compact mode.  Polling and mount live in run-view-poll.cjs.
 *
 * Every case gets a FRESH environment (console-output-env.cjs; the run-view module takes the slot that file calls
 * "console-output") and passes by returning without throwing; the report maps the case name to true, or to the
 * failure's stack.
 *
 *   Usage:  node run-view.cjs <renders> <views> <regions> <helpers> <run-view> <vectors.json>
 */
'use strict';

const path = require('node:path');
const E = require(path.join(__dirname, 'console-output-env.cjs'));

const argv = process.argv.slice(2);
if (argv.length < 6) {
	process.stderr.write('usage: run-view.cjs <renders> <views> <regions> <helpers> <run-view> <vectors>\n');
	process.exit(2);
}

const cases = [];
function test(name, fn) { cases.push({ name: name, fn: fn }); }
function expect(cond, detail) {
	if (!cond) throw new Error(typeof detail === 'string' ? detail : JSON.stringify(detail));
}
function canon(v) {
	if (Array.isArray(v)) return v.map(canon);
	if (v && typeof v === 'object') { const o = {}; for (const k of Object.keys(v).sort()) o[k] = canon(v[k]); return o; }
	return v;
}
function same(a, b) { return JSON.stringify(canon(a)) === JSON.stringify(canon(b)); }
function expectSame(actual, expected, what) {
	expect(same(actual, expected), (what || 'value') + ': expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual));
}
function codes(problems) { return problems.map(function (p) { return p.code; }); }

function fresh() { const t = E.load(argv); t.RV = t.NS.runView; return t; }

// @cases:checks

test('chk01_vectors', function (t) {
	const RV = t.RV, bad = [];
	const groups = { noteHref: RV.isSafeNoteHref, rawHref: RV.isSafeRawHref, duration: RV.formatDuration };
	for (const g of Object.keys(groups))
		for (const row of t.vectors[g])
			if (groups[g](row[0]) !== row[1]) bad.push(g + '(' + JSON.stringify(row[0]) + ') should be ' + JSON.stringify(row[1]));
	for (const v of [null, undefined, 42, {}, []])
		if (RV.isSafeNoteHref(v) || RV.isSafeRawHref(v)) bad.push('non-string accepted: ' + String(v));
	expect(bad.length === 0, bad.join('\n'));
});

const VALID = [
	{ ev: 'step', seq: 1, id: 'build', title: 'mvn package', n: 4, status: 'waiting', rawLine: 3 },
	{ ev: 'end', seq: 2, id: 'build', status: 'fail', ms: 1200, exit: 1 },
	{ ev: 'suite', seq: 3, step: 'build', fw: 'surefire', suite: 'com.example.A', counts: { pass: 1, fail: 2, skip: 3 }, rawLine: 9 },
	{ ev: 'test', seq: 4, step: 'build', fw: 'jest', suite: 'a.test.js', name: 'adds', status: 'error', ms: 5, msg: 'boom', trace: 'at x', rawLine: 11 },
	{ ev: 'replace', seq: 5, step: 'build' },
	{ ev: 'note', seq: 6, level: 'warn', text: 'heads up', href: 'https://example.org/x', step: 'build' },
	{ ev: 'done', seq: 7, status: 'cancelled' }
];

test('chk02_validateEventAccepts', function (t) {
	for (const e of VALID) {
		const r = t.RV.validateEvent(Object.assign({ extra: 1 }, e));
		expect(r.event !== null && r.problems.length === 0 && !r.ignored, 'accepts ' + e.ev + ': ' + JSON.stringify(r));
		expectSame(r.event, e, e.ev);
	}
	expectSame(t.RV.validateEvent({ ev: 'step', id: 'a', title: 'T' }).event, { ev: 'step', id: 'a', title: 'T' }, 'no seq stays absent');
});

test('chk03_validateEventDrops', function (t) {
	const s = VALID[0], te = VALID[3];
	const inputs = [
		null, 'x', 42, [], {}, { seq: 1 }, { ev: 7 },
		Object.assign({}, s, { id: '-bad' }), Object.assign({}, s, { id: 'a b' }), Object.assign({}, s, { id: undefined }),
		Object.assign({}, s, { title: '' }), Object.assign({}, s, { title: 3 }),
		Object.assign({}, s, { n: 0 }), Object.assign({}, s, { n: 10000 }), Object.assign({}, s, { n: 1.5 }),
		Object.assign({}, s, { status: 'ok' }), Object.assign({}, s, { rawLine: 0 }), Object.assign({}, s, { seq: 0 }),
		Object.assign({}, s, { seq: 1.5 }), Object.assign({}, s, { seq: '1' }),
		{ ev: 'end', id: 'a' }, { ev: 'end', id: 'a', status: 'running' }, { ev: 'end', id: 'a', status: 'ok', ms: -1 },
		Object.assign({}, VALID[2], { counts: undefined }), Object.assign({}, VALID[2], { counts: { pass: 1, fail: 1 } }),
		Object.assign({}, VALID[2], { counts: { pass: 1, fail: -1, skip: 0 } }),
		Object.assign({}, VALID[2], { counts: { pass: 1000000001, fail: 0, skip: 0 } }),
		Object.assign({}, VALID[2], { fw: 'Jest' }), Object.assign({}, VALID[2], { suite: '' }),
		Object.assign({}, te, { status: 'bogus' }), Object.assign({}, te, { name: '' }), Object.assign({}, te, { rawLine: 0 }),
		Object.assign({}, te, { ms: -1 }), Object.assign({}, te, { msg: 5 }), Object.assign({}, te, { step: 'a b' }),
		{ ev: 'replace' }, { ev: 'replace', step: '' },
		{ ev: 'note', level: 'fatal', text: 'x' }, { ev: 'note', level: 'info', text: '' }, { ev: 'note', level: 'info', text: 'x', step: '!' },
		{ ev: 'done', status: 'maybe' }, { ev: 'done' }
	];
	const bad = [];
	for (const i of inputs) {
		const r = t.RV.validateEvent(i);
		if (r.event !== null || r.ignored || !same(codes(r.problems), ['E-RV-3'])) bad.push(JSON.stringify(i) + ' -> ' + JSON.stringify(r));
	}
	expect(bad.length === 0, bad.join('\n'));
});

test('chk04_unknownEvIsIgnored', function (t) {
	expectSame(t.RV.validateEvent({ ev: 'telemetry', seq: 3, x: 1 }), { event: null, problems: [], ignored: true });
});

test('chk05_unknownMembersIgnored', function (t) {
	const r = t.RV.validateEvent({ ev: 'done', status: 'ok', colour: 'red', extra: { a: 1 } });
	expect(r.problems.length === 0 && same(r.event, { ev: 'done', status: 'ok' }), JSON.stringify(r));
});

test('chk06_overlongStringsTruncated', function (t) {
	const r = t.RV.validateEvent({ ev: 'test', step: 's', fw: 'jest', suite: 'x', name: 'n'.repeat(600), status: 'pass' });
	expect(r.event.name.length === 512 && r.event.name.endsWith('…') && r.event.name.startsWith('n'.repeat(511)), 'cut to 512: ' + r.event.name.length);
	const m = t.RV.validateEvent({ ev: 'test', step: 's', fw: 'jest', suite: 'x', name: 'n', status: 'fail', msg: 'm'.repeat(2001), trace: 't'.repeat(8001) });
	expect(m.event.msg.length === 2000 && m.event.trace.length === 8000, 'msg and trace caps');
	expect(t.RV.validateEvent({ ev: 'step', id: 'a', title: 'x'.repeat(201) }).event.title.length === 200, 'title cap');
	expect(t.RV.validateEvent({ ev: 'note', level: 'info', text: 'x'.repeat(1001) }).event.text.length === 1000, 'note cap');
});

test('chk07_noteHrefRemovedNotDropped', function (t) {
	const r = t.RV.validateEvent({ ev: 'note', level: 'info', text: 'see', href: 'javascript:x' });
	expect(same(r.event, { ev: 'note', level: 'info', text: 'see' }) && same(r.problems, [{ code: 'E-RV-7', value: 'javascript:x' }]), JSON.stringify(r));
});

// @cases:reducer

function RT(t) { return t.RV.__test; }
function mkModel(t) { const R = RT(t); return { R: R, m: R.newModel() }; }
function feed(x, evs) { return [].concat(evs).map(function (e) { return x.R.reduce(x.m, e); }); }
function stepEv(id, extra) { return Object.assign({ ev: 'step', id: id, title: 'T ' + id }, extra); }
function testEv(step, name, status, extra) { return Object.assign({ ev: 'test', step: step, fw: 'jest', suite: 'a.test.js', name: name, status: status }, extra); }
function stepOf(x, id) { return x.m.stepIndex.get(id); }
function onlySuite(x, id) { return Array.from(stepOf(x, id).suites.values())[0]; }

test('red01_stepUpsert', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a', { n: 1, rawLine: 4 }), stepEv('b')]);
	expectSame(x.m.steps.map(function (s) { return s.id; }), ['a', 'b'], 'order');
	feed(x, stepEv('a', { title: 'New' }));
	const a = stepOf(x, 'a');
	expect(a.title === 'New' && a.n === 1 && a.rawLine === 4 && a.status === 'running', 'absent members keep stored values');
	feed(x, stepEv('a', { n: 2, rawLine: 7, status: 'waiting' }));
	expect(a.n === 2 && a.rawLine === 7 && a.status === 'waiting' && x.m.steps.length === 2, 'present members update');
});

test('red02_waitingThenRunning', function (t) {
	const x = mkModel(t);
	feed(x, stepEv('g', { status: 'waiting' }));
	expect(stepOf(x, 'g').status === 'waiting', 'waiting');
	feed(x, stepEv('g', { status: 'running' }));
	expect(stepOf(x, 'g').status === 'running', 'running');
});

test('red03_stepAfterEndDropped', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), { ev: 'end', id: 'a', status: 'ok' }]);
	const r = feed(x, stepEv('a', { title: 'Late' }))[0];
	expect(!r.applied && codes(r.problems).join() === 'E-RV-6' && /ended step/.test(r.problems[0].detail), JSON.stringify(r));
	expect(stepOf(x, 'a').title === 'T a' && x.m.stats.dropped === 1, 'unchanged and counted');
	const r2 = feed(x, stepEv('a'))[0];
	expect(r2.problems.length === 0, 'reported once');
});

test('red04_endSetsFinalAndReplacementEndWins', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), { ev: 'end', id: 'a', status: 'fail', ms: 100, exit: 2 }]);
	const a = stepOf(x, 'a');
	expect(a.status === 'fail' && a.ended && a.ms === 100 && a.exit === 2, 'first end');
	feed(x, { ev: 'end', id: 'a', status: 'ok', ms: 200 });
	expect(a.status === 'ok' && a.ms === 200 && a.exit === undefined && a.ended, 'second end replaces');
});

test('red05_endUnknownStepDropped', function (t) {
	const x = mkModel(t);
	const r = feed(x, { ev: 'end', id: 'zz', status: 'ok' })[0];
	expect(!r.applied && codes(r.problems).join() === 'E-RV-6' && /unknown step/.test(r.problems[0].detail), JSON.stringify(r));
	for (const e of [{ ev: 'suite', step: 'zz', fw: 'jest', suite: 's', counts: { pass: 1, fail: 0, skip: 0 } }, testEv('zz', 'n', 'pass'), { ev: 'replace', step: 'zz' }])
		expect(!feed(x, e)[0].applied, e.ev + ' for unknown step');
});

test('red06_suitePlaceholderThenReplaceCounts', function (t) {
	const x = mkModel(t);
	const sv = function (c) { return { ev: 'suite', step: 'a', fw: 'jest', suite: 'a.test.js', counts: c, rawLine: 3 }; };
	feed(x, [stepEv('a'), sv({ pass: 1, fail: 0, skip: 0 })]);
	feed(x, sv({ pass: 4, fail: 1, skip: 2 }));
	const s = onlySuite(x, 'a');
	expect(stepOf(x, 'a').suites.size === 1 && s.pass === 4 && s.fail === 1 && s.skip === 2 && !s.detailed && s.rawLine === 3, JSON.stringify(s));
});

test('red07_testsWinOverPlaceholders', function (t) {
	const x = mkModel(t);
	const sv = function (c) { return { ev: 'suite', step: 'a', fw: 'jest', suite: 'a.test.js', counts: c }; };
	feed(x, [stepEv('a'), sv({ pass: 9, fail: 9, skip: 9 }), testEv('a', 'n1', 'pass')]);
	let s = onlySuite(x, 'a');
	expect(s.detailed && s.pass === 1 && s.fail === 0 && s.skip === 0 && s.tests.length === 1, 'converted, placeholder counts discarded');
	const r = feed(x, sv({ pass: 50, fail: 0, skip: 0 }))[0];
	expect(!r.applied && r.problems.length === 0 && s.pass === 1 && x.m.stats.ignored === 1, 'late placeholder ignored');
});

test('red08_testsNotDeduplicated', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), testEv('a', 'same', 'pass'), testEv('a', 'same', 'pass')]);
	const s = onlySuite(x, 'a');
	expect(s.tests.length === 2 && s.pass === 2, 'both kept');
});

test('red09_replaceClearsStep', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), stepEv('b'), testEv('a', 'n1', 'pass'), testEv('a', 'n2', 'fail'), testEv('b', 'm1', 'pass')]);
	x.m.expanded = new Map([['a/jest/a.test.js', true]]);
	const ok = x.m.retainedOk, bad = x.m.retainedBad;
	feed(x, { ev: 'replace', step: 'a' });
	expect(stepOf(x, 'a').suites.size === 0 && stepOf(x, 'a').suiteOrder.length === 0, 'suites cleared');
	expect(stepOf(x, 'b').suites.size === 1, 'other step untouched');
	expect(x.m.retainedOk === ok - 1 && x.m.retainedBad === bad - 1, 'retention pools released');
	expect(x.m.expanded.get('a/jest/a.test.js') === true, 'expansion state untouched');
	feed(x, testEv('a', 'n3', 'pass'));
	expect(onlySuite(x, 'a').pass === 1, 'fresh set follows');
});

test('red10_noteAttachment', function (t) {
	const x = mkModel(t);
	feed(x, stepEv('a'));
	const r1 = feed(x, { ev: 'note', level: 'info', text: 'one', step: 'a' })[0];
	expect(r1.applied && r1.problems.length === 0 && stepOf(x, 'a').notes.length === 1 && x.m.runNotes.length === 0, 'known step');
	const r2 = feed(x, { ev: 'note', level: 'warn', text: 'two', step: 'nope' })[0];
	expect(r2.applied && codes(r2.problems).join() === 'E-RV-6' && /note for unknown step/.test(r2.problems[0].detail), JSON.stringify(r2));
	expect(x.m.runNotes.length === 1 && x.m.notes.length === 2, 'shown under the run');
	feed(x, { ev: 'note', level: 'info', text: 'three' });
	expect(x.m.runNotes.length === 2, 'no step goes under the run');
});

test('red11_doneLatestWinsAndLateNoteApplies', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), { ev: 'done', status: 'fail' }, { ev: 'done', status: 'ok' }]);
	expect(x.m.done === 'ok', 'latest wins');
	const r = feed(x, { ev: 'note', level: 'info', text: 'late' })[0];
	expect(r.applied && x.m.notes.length === 1, 'late note applies');
});

test('red12_derivedClosing', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), stepEv('b', { status: 'waiting' }), stepEv('c'), { ev: 'end', id: 'c', status: 'ok' }]);
	const shown = function () { return RT(t).derive(x.m).steps.map(function (s) { return s.shown; }); };
	expectSame(shown(), ['running', 'waiting', 'ok'], 'no done');
	feed(x, { ev: 'done', status: 'fail' });
	expectSame(shown(), ['fail', 'fail', 'ok'], 'done fail');
	feed(x, { ev: 'done', status: 'ok' });
	expectSame(shown(), ['skip', 'skip', 'ok'], 'done ok');
	feed(x, { ev: 'done', status: 'cancelled' });
	expectSame(shown(), ['skip', 'skip', 'ok'], 'done cancelled');
	expect(stepOf(x, 'a').status === 'running' && !stepOf(x, 'a').ended, 'stored state unchanged');
	feed(x, { ev: 'end', id: 'a', status: 'ok' });
	expectSame(shown(), ['ok', 'skip', 'ok'], 'a later end still applies');
});

test('red13_runStatusTable', function (t) {
	const R = RT(t);
	const st = function (evs, terminal) { const x = mkModel(t); feed(x, evs); x.m.terminal = !!terminal; return R.derive(x.m); };
	let d = st([]);
	expect(d.status === 'empty' && d.headline === 'Waiting for events…', 'empty ' + JSON.stringify(d.status));
	d = st([stepEv('a', { status: 'waiting' })]);
	expect(d.status === 'waiting' && d.headline === 'Waiting at T a', 'waiting only');
	d = st([stepEv('a', { status: 'waiting' }), stepEv('b')]);
	expect(d.status === 'running' && d.headline === 'Running', 'waiting plus running is running');
	d = st([stepEv('a'), { ev: 'done', status: 'ok' }]);
	expect(d.status === 'ok' && d.headline === 'Passed', 'ok');
	d = st([stepEv('a'), stepEv('b'), { ev: 'end', id: 'a', status: 'ok' }, { ev: 'done', status: 'fail' }]);
	expect(d.status === 'fail' && d.headline === 'Failed at T b', 'fail names the first failed step: ' + d.headline);
	d = st([stepEv('a'), { ev: 'end', id: 'a', status: 'ok' }, { ev: 'done', status: 'fail' }]);
	expect(d.status === 'fail' && d.headline === 'Failed', 'fail with no failed step');
	d = st([stepEv('a'), { ev: 'done', status: 'cancelled' }]);
	expect(d.status === 'cancelled' && d.headline === 'Cancelled', 'cancelled');
});

test('red14_seqIdempotent', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a', { seq: 5 }), stepEv('b', { seq: 6 })]);
	const r = feed(x, [stepEv('c', { seq: 6 }), stepEv('d', { seq: 3 })]);
	expect(r.every(function (v) { return !v.applied && v.problems.length === 0; }) && x.m.steps.length === 2 && x.m.stats.skipped === 2, 'replays skipped');
	feed(x, stepEv('e'));
	expect(x.m.lastSeq === 7 && x.m.steps.length === 3, 'bare event gets lastSeq + 1');
	feed(x, stepEv('f', { seq: 20 }));
	expect(x.m.lastSeq === 20, 'gaps allowed');
});

test('red15_retentionCaps', function (t) {
	const x = mkModel(t);
	feed(x, stepEv('a'));
	for (let i = 0; i < 20001; i++) x.R.reduce(x.m, testEv('a', 'p' + (i % 7), 'pass'));
	const s = onlySuite(x, 'a');
	expect(s.pass === 20001 && s.tests.length === 20000 && s.dropped === 1 && x.m.stats.droppedTests === 1, 'passing capped, counts exact');
	feed(x, testEv('a', 'f0', 'fail'));
	expect(s.fail === 1 && s.tests.length === 20001 && x.m.retainedBad === 1, 'failure still retained from its own pool');
	for (let i = 1; i < 5001; i++) x.R.reduce(x.m, testEv('a', 'f' + i, i % 2 ? 'fail' : 'error'));
	expect(s.fail === 5001 && x.m.retainedBad === 5000 && s.dropped === 2 && x.m.stats.droppedTests === 2, 'failures capped at 5000, counted');
});

test('red16_structuralCaps', function (t) {
	let x = mkModel(t);
	const probs = [];
	for (let i = 0; i < 1001; i++) probs.push.apply(probs, x.R.reduce(x.m, stepEv('s' + i)).problems);
	expect(x.m.steps.length === 1000 && codes(probs).join() === 'E-RV-9', 'steps: ' + x.m.steps.length + ' ' + codes(probs));
	x = mkModel(t);
	feed(x, stepEv('a'));
	const sp = [];
	for (let i = 0; i < 5001; i++) sp.push.apply(sp, x.R.reduce(x.m, { ev: 'suite', step: 'a', fw: 'jest', suite: 's' + i, counts: { pass: 1, fail: 0, skip: 0 } }).problems);
	expect(stepOf(x, 'a').suites.size === 5000 && codes(sp).join() === 'E-RV-9', 'suites per step');
	x = mkModel(t);
	const np = [];
	for (let i = 0; i < 1001; i++) np.push.apply(np, x.R.reduce(x.m, { ev: 'note', level: 'info', text: 'n' + i }).problems);
	expect(x.m.notes.length === 1000 && codes(np).join() === 'E-RV-9', 'notes');
});

test('red17_countsExactPerFw', function (t) {
	const x = mkModel(t);
	feed(x, [stepEv('a'), stepEv('b'),
		{ ev: 'suite', step: 'a', fw: 'surefire', suite: 'A', counts: { pass: 3, fail: 1, skip: 0 } },
		testEv('b', 'j1', 'pass'), testEv('b', 'j2', 'fail'), testEv('b', 'j3', 'error'), testEv('b', 'j4', 'skip'),
		{ ev: 'suite', step: 'b', fw: 'surefire', suite: 'B', counts: { pass: 2, fail: 0, skip: 1 } }]);
	const d = RT(t).derive(x.m);
	expectSame(d.counts, [{ fw: 'surefire', pass: 5, fail: 1, skip: 1 }, { fw: 'jest', pass: 1, fail: 2, skip: 1 }], 'counts');
	expect(d.failures.length === 3 && d.failures[0].test === null && d.failures[1].test.name === 'j2' && d.failures[2].test.name === 'j3', 'failures: placeholder, then tests in order');
});

test('red18_stepCapDropsOnlyNewIds', function (t) {
	const x = mkModel(t);
	for (let i = 0; i < 1000; i++) x.R.reduce(x.m, stepEv('s' + i));
	const r = feed(x, [stepEv('s5', { title: 'Updated' }), { ev: 'end', id: 's5', status: 'ok' }, stepEv('extra')]);
	expect(r[0].applied && r[1].applied && !r[2].applied && stepOf(x, 's5').title === 'Updated' && stepOf(x, 's5').ended, 'upserts and ends still apply');
	expect(x.m.steps.length === 1000 && !x.m.stepIndex.has('extra'), 'new id dropped');
});

test('red19_terminalWithoutDoneIsStopped', function (t) {
	const x = mkModel(t);
	feed(x, stepEv('a'));
	x.m.terminal = true;
	let d = RT(t).derive(x.m);
	expect(d.status === 'stopped' && d.headline === 'Stopped' && d.glyph === '⏸', JSON.stringify([d.status, d.headline, d.glyph]));
	feed(x, { ev: 'done', status: 'ok' });
	expect(RT(t).derive(x.m).status === 'ok', 'a done wins over terminal');
});

// @cases:core

function mkRun(t, opts) {
	const host = t.env.document.createElement('div');
	t.env.body.appendChild(host);
	const api = t.RV.create(host, opts || {});
	const q = function (sel) { return host.querySelector(sel); };
	const all = function (sel) { return host.querySelectorAll(sel); };
	const flush = function () { t.clock.advance(0); };
	return {
		host: host, api: api, q: q, all: all, flush: flush,
		root: function () { return q('.juneau-rv'); },
		head: function () { return q('.juneau-rv-headline').textContent; },
		step: function (id) { return all('.juneau-rv-step').find(function (l) { return l.getAttribute('data-step') === id; }) || null; },
		send: function (evs) { const r = api.append(evs); flush(); return r; }
	};
}

test('core01_createAndRoot', function (t) {
	const c = mkRun(t, { id: 'run1', title: 'Build 7' });
	const r = c.root();
	expect(r && r.tagName === 'SECTION' && r.getAttribute('role') === 'region', 'region root');
	expect(r.getAttribute('aria-label') === 'Build 7', 'aria-label from title');
	expect(r.getAttribute('data-juneau-rv-status') === 'empty', 'starts empty');
	expect(c.head() === '⋯ Waiting for events…', 'headline: ' + c.head());
	const h = c.q('.juneau-rv-headline');
	expect(h.getAttribute('aria-live') === 'polite' && h.getAttribute('aria-atomic') === 'true', 'live region');
	expect(c.q('.juneau-co-tooltip').hasAttribute('hidden') || c.q('.juneau-co-tooltip').hidden === true, 'tooltip hidden');
	expect(t.RV.of('run1') === c.api && t.RV.of(c.host) === c.api && t.RV.of('nope') === null, 'registry');
	expect(c.api.lastSeq() === 0, 'lastSeq 0');
	expect(mkRun(t, {}).root().getAttribute('aria-label') === 'Run', 'default title');
});

test('core02_badOptionsThrowBeforeDom', function (t) {
	const bad = [{ id: '' }, { id: 7 }, { compact: 'yes' }, { title: 5 }, { rawHref: 'javascript:x{line}' }, { rawHref: '/a' }, { emit: 3 }];
	for (const o of bad) {
		const host = t.env.document.createElement('div');
		let err = null;
		try { t.RV.create(host, o); } catch (e) { err = e; }
		expect(err && err.code === 'E-RV-1' && err.name === 'JuneauRunViewError', 'throws E-RV-1 for ' + JSON.stringify(o));
		expect(host.children.length === 0, 'no DOM for ' + JSON.stringify(o));
	}
	expect(t.rec.errorsMatching('E-RV-1').length === bad.length, 'one console.error each');
	let e2 = null;
	try { t.RV.create(null, {}); } catch (e) { e2 = e; }
	expect(e2 && e2.code === 'E-RV-1', 'no container');
});

test('core03_oneRenderPerBatch', function (t) {
	const c = mkRun(t);
	const n0 = t.RV.__test.renders(c.api);
	const r = c.api.append([{ ev: 'step', id: 'a', title: 'A' }, { ev: 'step', id: 'b', title: 'B' }, { ev: 'bogus', x: 1 }, { ev: 'step', id: '-x', title: 'bad' }]);
	c.api.append({ ev: 'step', id: 'c', title: 'C' });
	expect(t.RV.__test.renders(c.api) === n0, 'render is deferred');
	expectSame(r, { applied: 2, skipped: 0, dropped: 1 }, 'counts (unknown ev is ignored, bad id dropped)');
	c.flush();
	expect(t.RV.__test.renders(c.api) === n0 + 1, 'one render for the batch');
	expect(c.all('.juneau-rv-step').length === 3, 'three steps');
	expect(c.api.append({ ev: 'step', seq: 1, id: 'z', title: 'Z' }).skipped === 1, 'old seq skipped');
	c.flush();
	expect(t.RV.__test.renders(c.api) === n0 + 1, 'nothing applied, nothing scheduled');
});

test('core04_destroyIsIdempotent', function (t) {
	const c = mkRun(t, { id: 'gone' });
	c.api.append({ ev: 'step', id: 'a', title: 'A' });
	c.api.destroy();
	c.api.destroy();
	expect(c.q('.juneau-rv') === null && c.host.children.length === 0, 'DOM removed');
	expect(t.RV.of('gone') === null && t.RV.of(c.host) === null, 'unregistered');
	expectSame(c.api.append({ ev: 'step', id: 'b', title: 'B' }), { applied: 0, skipped: 0, dropped: 0 }, 'append is a no-op');
	c.flush();
	expect(c.q('.juneau-rv') === null, 'no resurrection');
	const again = t.RV.create(c.host, { id: 'gone' });
	expect(t.RV.of('gone') === again, 'id reusable');
});

// @cases:sum

function stepOk(id, extra) { return Object.assign({ ev: 'step', id: id, title: id.toUpperCase() }, extra || {}); }

test('sum01_headlineAndStatus', function (t) {
	const c = mkRun(t);
	const status = function () { return c.root().getAttribute('data-juneau-rv-status'); };
	c.send(stepOk('a'));
	expect(status() === 'running' && c.head() === '⋯ Running', 'running: ' + c.head());
	c.send({ ev: 'step', id: 'w', title: 'Approve', status: 'waiting' });
	c.send({ ev: 'end', id: 'a', status: 'ok' });
	expect(status() === 'waiting' && c.head() === '⏸ Waiting at Approve', 'waiting: ' + c.head());
	c.send({ ev: 'done', status: 'fail' });
	expect(status() === 'fail' && c.head().indexOf('✗ Failed') === 0, 'failed: ' + c.head());
	const d = mkRun(t);
	d.send([stepOk('a'), { ev: 'done', status: 'ok' }]);
	expect(d.head() === '✓ Passed' && d.root().getAttribute('data-juneau-rv-status') === 'ok', 'passed');
	const e = mkRun(t);
	e.send([stepOk('a'), { ev: 'done', status: 'cancelled' }]);
	expect(e.head() === '○ Cancelled', 'cancelled');
});

test('sum02_countsUseBuiltInLabels', function (t) {
	const c = mkRun(t);
	expect(c.q('.juneau-rv-counts').hidden === true, 'hidden while empty');
	c.send([stepOk('a'),
		{ ev: 'suite', step: 'a', fw: 'surefire', suite: 'S1', counts: { pass: 3, fail: 1, skip: 0 } },
		{ ev: 'suite', step: 'a', fw: 'junit-xml', suite: 'S2', counts: { pass: 2, fail: 0, skip: 1 } },
		{ ev: 'suite', step: 'a', fw: 'weird', suite: 'S3', counts: { pass: 1, fail: 0, skip: 0 } }]);
	const txt = c.q('.juneau-rv-counts').textContent;
	expect(txt === 'Surefire 3 ✓ 1 ✗ · JUnit 2 ✓ 1 ○ · weird 1 ✓', 'counts: ' + txt);
	expect(c.q('.juneau-rv-counts').hidden !== true, 'shown');
});

test('sum03_failuresListCaps', function (t) {
	const evs = [stepOk('a')];
	for (let i = 0; i < 60; i++)
		evs.push({ ev: 'test', step: 'a', fw: 'jest', suite: 's', name: 't' + i, status: 'fail', msg: 'bad ' + i + '\nsecond line' });
	const c = mkRun(t);
	c.send(evs);
	expect(c.all('.juneau-rv-failure').length === 50, 'full mode shows 50');
	const more = c.q('.juneau-rv-failures').querySelectorAll('li').filter(function (l) { return l.textContent === '…and 10 more'; });
	expect(more.length === 1 && more[0].querySelector('button') === null, 'overflow line is not a button');
	expect(c.all('.juneau-rv-failure')[0].textContent === 's › t0 – bad 0', 'first line only: ' + c.all('.juneau-rv-failure')[0].textContent);
	const k = mkRun(t, { compact: true });
	k.send(evs);
	expect(k.all('.juneau-rv-failure').length === 3, 'compact shows 3');
	expect(k.root().className.indexOf('juneau-rv-compact') >= 0, 'compact class');
});

test('sum04_busEmitsOnStatusChangeOnly', function (t) {
	const got = [];
	const c = mkRun(t, { id: 'bus', emit: function (m) { got.push(m); } });
	c.send(stepOk('a'));
	c.send({ ev: 'step', id: 'b', title: 'B' });
	c.send({ ev: 'done', status: 'ok' });
	expectSame(got.map(function (m) { return m.state; }), ['running', 'ok'], 'states');
	expectSame(got[1], { kind: 'run-view.state', id: 'bus', state: 'ok', terminal: false }, 'payload');
	const boom = mkRun(t, { emit: function () { throw new Error('x'); } });
	boom.send(stepOk('a'));
	expect(boom.head() === '⋯ Running', 'a throwing emit does not break rendering');
});

// @cases:step

test('step01_createdOnceUpdatedInPlace', function (t) {
	const c = mkRun(t);
	c.send(stepOk('a', { n: 2 }));
	const li = c.step('a');
	expect(li.querySelector('.juneau-rv-title').textContent === '2. A', 'numbered title');
	expect(li.getAttribute('data-state') === 'running' && li.querySelector('.juneau-rv-glyph').textContent === '⋯', 'running glyph');
	c.send({ ev: 'end', id: 'a', status: 'ok', ms: 1500 });
	expect(c.step('a') === li, 'same li');
	expect(li.getAttribute('data-state') === 'ok' && li.querySelector('.juneau-rv-glyph').textContent === '✓', 'ok glyph');
	expect(li.querySelector('.juneau-rv-dur').textContent === '1.5 s', 'duration: ' + li.querySelector('.juneau-rv-dur').textContent);
	expect(li.querySelector('.juneau-co-sr').textContent === 'passed: ', 'sr text');
	expect(li.querySelector('.juneau-rv-glyph').getAttribute('aria-hidden') === 'true', 'glyph hidden from AT');
});

test('step02_rawLinkNeedsLineAndTemplate', function (t) {
	const c = mkRun(t, { rawHref: '/raw?line={line}' });
	c.send([stepOk('a', { rawLine: 12 }), stepOk('b')]);
	const a = c.step('a').querySelector('.juneau-rv-title');
	expect(a.tagName === 'A' && a.getAttribute('href') === '/raw?line=12' && a.getAttribute('rel') === 'nofollow noreferrer', 'link with rel');
	expect(c.step('b').querySelector('.juneau-rv-title').tagName === 'SPAN', 'no rawLine, no link');
	const f = mkRun(t, { rawHref: '#L{line}' });
	f.send(stepOk('a', { rawLine: 3 }));
	const fa = f.step('a').querySelector('.juneau-rv-title');
	expect(fa.getAttribute('href') === '#L3' && fa.getAttribute('rel') === null, 'fragment link has no rel');
	const g = mkRun(t, {});
	g.send(stepOk('a', { rawLine: 3 }));
	expect(g.step('a').querySelector('.juneau-rv-title').tagName === 'SPAN', 'no template, no link');
});

test('step03_exitCodeTooltip', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), { ev: 'end', id: 'a', status: 'fail', exit: 2 }, stepOk('b'), { ev: 'end', id: 'b', status: 'ok' }]);
	const ha = c.step('a').querySelector('.juneau-rv-step-head'), hb = c.step('b').querySelector('.juneau-rv-step-head');
	expect(ha.getAttribute('data-juneau-co-tip') === 'exit 2' && ha.getAttribute('tabindex') === '0', 'exit tip + focusable');
	expect(hb.getAttribute('data-juneau-co-tip') === null && hb.getAttribute('tabindex') === null, 'no exit, no tip');
});

test('step04_doneClosesOpenSteps', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), stepOk('b'), { ev: 'end', id: 'a', status: 'ok' }]);
	c.send({ ev: 'done', status: 'fail' });
	expect(c.step('a').getAttribute('data-state') === 'ok', 'ended step untouched');
	expect(c.step('b').getAttribute('data-state') === 'fail', 'open step shown as failed');
	const d = mkRun(t);
	d.send([stepOk('a'), { ev: 'done', status: 'cancelled' }]);
	expect(d.step('a').getAttribute('data-state') === 'skip', 'cancelled shows skipped');
	expect(d.api.state().steps[0].status === 'skip', 'state() agrees');
});

test('step05_notes', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'),
		{ ev: 'note', step: 'a', level: 'warn', text: 'careful' },
		{ ev: 'note', step: 'a', level: 'info', text: 'more', href: 'https://example.org/x' },
		{ ev: 'note', step: 'a', level: 'error', text: 'inner', href: '/log#L3' },
		{ ev: 'note', level: 'info', text: 'run level' }]);
	const ns = c.step('a').querySelectorAll('.juneau-rv-note');
	expect(ns.length === 3, 'three step notes');
	expect(ns[0].className.indexOf('juneau-rv-note-warn') >= 0 && ns[0].querySelector('.juneau-rv-note-glyph').textContent === '⚠', 'warn glyph');
	expect(ns[0].querySelector('.juneau-co-sr').textContent === 'warning: ', 'sr level');
	const a1 = ns[1].querySelector('a'), a2 = ns[2].querySelector('a');
	expect(a1.getAttribute('target') === '_blank' && a1.getAttribute('rel') === 'noopener noreferrer', 'external link opens safely');
	expect(a2.getAttribute('href') === '/log#L3' && a2.getAttribute('target') === null, 'path link is plain');
	const rn = c.q('.juneau-rv-run-notes');
	expect(rn.hidden !== true && rn.querySelectorAll('.juneau-rv-note').length === 1, 'run note shown');
	c.send({ ev: 'note', step: 'a', level: 'info', text: 'bad', href: 'javascript:alert(1)' });
	expect(c.all('a').every(function (a) { return a.getAttribute('href').indexOf('javascript') < 0; }), 'unsafe href never rendered');
	expect(t.rec.warnsMatching('E-RV-7').length === 1, 'E-RV-7 warned');
});

test('esc01_hostileTextIsInert', function (t) {
	const h = '<img src=x onerror=alert(1)>';
	const c = mkRun(t, { title: h, rawHref: '/raw/{line}' });
	c.send([{ ev: 'step', id: 'a', title: h, rawLine: 1 }, { ev: 'suite', step: 'a', fw: 'jest', suite: h, counts: { pass: 0, fail: 1, skip: 0 } },
		{ ev: 'note', level: 'warn', text: h }]);
	expect(c.host.querySelector('img') === null && c.host.querySelector('script') === null, 'no injected elements');
	expect(c.root().getAttribute('aria-label') === h, 'title is attribute text');
	expect(c.step('a').querySelector('.juneau-rv-title').textContent === h, 'title is text');
	expect(c.q('.juneau-rv-suite-name').textContent === h, 'suite name shown as text');
});

test('api01_resetStateStats', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), { ev: 'bogus' }, { ev: 'step', id: '-', title: 'x' }]);
	expect(c.api.lastSeq() === 1, 'lastSeq');
	const st = c.api.stats();
	expect(st.applied === 1 && st.ignored === 1 && st.dropped === 1, 'stats ' + JSON.stringify(st));
	st.applied = 99;
	expect(c.api.stats().applied === 1, 'stats is a copy');
	const s = c.api.state();
	expect(s.status === 'running' && s.steps.length === 1 && s.steps[0].id === 'a', 'state snapshot');
	s.steps.length = 0;
	expect(c.api.state().steps.length === 1, 'state is a copy');
	c.api.reset();
	expect(c.api.lastSeq() === 0 && c.all('.juneau-rv-step').length === 0, 'reset clears');
	expect(c.root().getAttribute('data-juneau-rv-status') === 'empty' && c.head() === '⋯ Waiting for events…', 'reset to empty');
	c.send(stepOk('a'));
	expect(c.all('.juneau-rv-step').length === 1, 'usable after reset');
});

// @cases:suites

function testEvs(step, fw, suite, specs) {
	return specs.map(function (sp) {
		return Object.assign({ ev: 'test', step: step, fw: fw, suite: suite, name: sp[0], status: sp[1] }, sp[2] || {});
	});
}
function suiteRows(c) { return c.all('.juneau-rv-suite'); }
function rowOf(c, name) {
	return suiteRows(c).find(function (r) { return (r.querySelector('.juneau-rv-suite-toggle') || r.querySelector('.juneau-rv-suite-name')).textContent === name; });
}
function toggleOf(c, name) { return rowOf(c, name).querySelector('.juneau-rv-suite-toggle'); }
function click(c, el) { c.root().dispatch('click', { target: el }); }

test('suite01_cleanCollapsedFailingExpanded', function (t) {
	const c = mkRun(t);
	const clean = [];
	for (let i = 0; i < 12; i++) clean.push(['ok' + i, 'pass']);
	clean.push(['s1', 'skip'], ['s2', 'skip'], ['s3', 'skip']);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 'clean', clean), testEvs('a', 'jest', 'broken', [['p', 'pass'], ['f', 'fail', { msg: 'x' }]])));
	const r1 = rowOf(c, 'clean'), r2 = rowOf(c, 'broken');
	expect(r1.querySelector('.juneau-rv-suite-sum').textContent === '12 ✓ 0 ✗ 3 ○ ▸'.replace(' 0 ✗', '') , 'clean sum: ' + r1.querySelector('.juneau-rv-suite-sum').textContent);
	expect(r1.querySelectorAll('.juneau-co-block').length === 0 && toggleOf(c, 'clean').getAttribute('aria-expanded') === 'false', 'collapsed, no blocks');
	expect(toggleOf(c, 'broken').getAttribute('aria-expanded') === 'true' && r2.querySelectorAll('.juneau-co-block').length === 2, 'failing expanded with a strip');
	expect(r2.querySelector('.juneau-rv-suite-sum').textContent === '1 ✓ 1 ✗ ▾', 'failing sum: ' + r2.querySelector('.juneau-rv-suite-sum').textContent);
});

test('suite02_toggleAndPersistence', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 'clean', [['a', 'pass'], ['b', 'pass']])));
	expect(c.all('.juneau-co-block').length === 0, 'collapsed');
	click(c, toggleOf(c, 'clean'));
	expect(toggleOf(c, 'clean').getAttribute('aria-expanded') === 'true' && c.all('.juneau-co-block').length === 2, 'expanded builds blocks');
	c.send(testEvs('a', 'jest', 'other', [['x', 'pass']]));
	expect(toggleOf(c, 'clean').getAttribute('aria-expanded') === 'true', 'survives a re-render');
	c.send({ ev: 'replace', step: 'a' });
	c.send(testEvs('a', 'jest', 'clean', [['a', 'pass']]));
	expect(toggleOf(c, 'clean').getAttribute('aria-expanded') === 'true', 'survives a replace');
	click(c, toggleOf(c, 'clean'));
	expect(toggleOf(c, 'clean').getAttribute('aria-expanded') === 'false' && c.all('.juneau-co-block').length === 0, 'collapses again');
});

test('suite03_placeholderShowsCounts', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), { ev: 'suite', step: 'a', fw: 'surefire', suite: 'P', counts: { pass: 12, fail: 1, skip: 0 } }]);
	const r = rowOf(c, 'P');
	expect(r.querySelector('.juneau-rv-suite-sum').textContent === '12 ✓ 1 ✗ 0 ○', 'counts: ' + r.querySelector('.juneau-rv-suite-sum').textContent);
	expect(r.querySelector('.juneau-rv-running') !== null && r.querySelector('.juneau-rv-suite-toggle') === null, 'running marker, no toggle');
	expect(r.getAttribute('tabindex') === '-1' && r.querySelectorAll('.juneau-co-block').length === 0, 'focus target, no blocks');
	c.send({ ev: 'end', id: 'a', status: 'fail' });
	expect(rowOf(c, 'P').querySelector('.juneau-rv-running') === null, 'marker gone when the step ends');
});

test('blk01_blockClassesAndAttributes', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [
		['good', 'pass', { ms: 1500 }], ['bad', 'fail', { msg: 'first\nsecond', ms: 20 }], ['err', 'error', { msg: 'first\nsecond' }], ['skp', 'skip']])));
	const b = c.all('.juneau-co-block');
	expect(b.length === 4, 'four blocks');
	expect(b[0].className.indexOf('juneau-co-fill-success') >= 0 && b[1].className.indexOf('juneau-co-fill-error') >= 0
		&& b[3].className.indexOf('juneau-co-block-empty') >= 0, 'fill classes');
	expect(b[0].getAttribute('data-juneau-co-tip') === 'good\npass · 1.5 s', 'tip: ' + JSON.stringify(b[0].getAttribute('data-juneau-co-tip')));
	expect(b[1].getAttribute('data-juneau-co-tip') === 'bad\nfail · 20 ms\nfirst', 'tip with msg: ' + JSON.stringify(b[1].getAttribute('data-juneau-co-tip')));
	expect(b[3].getAttribute('data-juneau-co-tip') === 'skp\nskip', 'empty lines omitted');
	expect(b[0].getAttribute('aria-label') === 'good, pass, 1.5 s' && b[3].getAttribute('aria-label') === 'skp, skip', 'aria-label');
	expect(b[0].getAttribute('tabindex') === '0' && b[0].getAttribute('role') === 'img', 'span block focusable');
	expect(b[1].getAttribute('data-juneau-co-tip') !== b[2].getAttribute('data-juneau-co-tip')
		&& b[1].className === b[2].className, 'error and fail differ in tooltip only');
});

test('blk02_anchorOnlyWithRawLineAndTemplate', function (t) {
	const c = mkRun(t, { rawHref: '#raw-L{line}' });
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['with', 'pass', { rawLine: 12 }], ['without', 'pass']])));
	click(c, toggleOf(c, 's'));
	const b = c.all('.juneau-co-block');
	expect(b[0].tagName === 'A' && b[0].getAttribute('href') === '#raw-L12' && b[0].getAttribute('rel') === null, 'fragment anchor');
	expect(b[1].tagName === 'SPAN', 'no rawLine, no anchor');
	const d = mkRun(t, { rawHref: '/raw?l={line}' });
	d.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['with', 'pass', { rawLine: 12 }]])));
	click(d, toggleOf(d, 's'));
	expect(d.all('.juneau-co-block')[0].getAttribute('rel') === 'nofollow noreferrer', 'path anchor has rel');
	const e = mkRun(t, {});
	e.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['with', 'pass', { rawLine: 12 }]])));
	click(e, toggleOf(e, 's'));
	expect(e.all('.juneau-co-block')[0].tagName === 'SPAN', 'no template, no anchor');
});

test('blk03_eventCannotSupplyHref', function (t) {
	const c = mkRun(t, { rawHref: '/raw#{line}' });
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['x', 'pass', { rawLine: 4, href: 'https://evil.example/', rawHref: 'https://evil.example/{line}' }]])));
	click(c, toggleOf(c, 's'));
	const b = c.all('.juneau-co-block')[0];
	expect(b.getAttribute('href') === '/raw#4', 'href comes from the instance template: ' + b.getAttribute('href'));
	expect(c.all('a').every(function (a) { return String(a.getAttribute('href')).indexOf('evil') < 0; }), 'nothing from the event');
});

test('blk04_traceDisclosure', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['bad', 'fail', { msg: 'boom', trace: 'at x.js:1' }], ['fine', 'pass']])));
	const blk = function (n) { return c.all('.juneau-co-block')[n]; };
	expect(c.all('.juneau-rv-trace').length === 0, 'closed at first');
	c.root().dispatch('keydown', { target: blk(0), key: 'Enter', preventDefault: function () {} });
	const d = c.q('.juneau-rv-trace');
	expect(d && d.tagName === 'DETAILS' && d.querySelectorAll('pre').length === 2, 'Enter opens a trace');
	expect(d.querySelector('.juneau-rv-trace-text').textContent === 'at x.js:1', 'trace text');
	c.root().dispatch('keydown', { target: blk(0), key: ' ', preventDefault: function () {} });
	expect(c.all('.juneau-rv-trace').length === 0, 'Space closes it');
	click(c, blk(0));
	expect(c.all('.juneau-rv-trace').length === 1, 'click opens it');
	click(c, blk(1));
	expect(c.all('.juneau-rv-trace').length === 1, 'a passing block does not toggle');
	const k = mkRun(t, { rawHref: '/r#{line}' });
	k.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['bad', 'fail', { msg: 'boom', trace: 'tr', rawLine: 3 }]])));
	click(k, k.all('.juneau-co-block')[0]);
	expect(k.all('.juneau-rv-trace').length === 0, 'an anchor block navigates, it does not toggle');
});

test('blk05_traceReachableFromFailuresList', function (t) {
	const c = mkRun(t, { rawHref: '/r#{line}' });
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['bad', 'fail', { msg: 'boom', trace: 'tr', rawLine: 3 }]])));
	const tb = c.q('.juneau-rv-failure-trace');
	expect(tb !== null && tb.tagName === 'BUTTON', 'a separate trace button exists even when the block is a link');
	click(c, tb);
	expect(c.all('.juneau-rv-trace').length === 1, 'trace shown');
	expect(c.q('.juneau-rv-failure-trace') !== null, 'button survives the re-render');
	const k = mkRun(t);
	k.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['bad', 'fail', { msg: 'boom' }]])));
	expect(k.q('.juneau-rv-failure-trace') === null, 'no trace, no button');
	click(k, k.q('.juneau-rv-failure'));
	expect(t.scrolled.length >= 1 || k.all('.juneau-co-block').length === 1, 'the failure jump reveals the block');
});

// @cases:tooltip

function tipRun(t) {
	const c = mkRun(t);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', [['bad', 'fail', { msg: 'boom' }]])));
	c.blk = c.all('.juneau-co-block')[0];
	c.tip = c.q('.juneau-co-tooltip');
	return c;
}

test('tip01_showHide', function (t) {
	const c = tipRun(t);
	c.root().dispatch('mouseover', { target: c.blk });
	expect(c.tip.hidden === false && c.tip.textContent === 'bad\nfail\nboom', 'shown: ' + JSON.stringify(c.tip.textContent));
	expect(c.blk.getAttribute('aria-describedby') === c.tip.id, 'described by');
	c.root().dispatch('mouseout', { target: c.blk, relatedTarget: null });
	expect(c.tip.hidden === true && c.blk.getAttribute('aria-describedby') === null, 'hidden');
	c.root().dispatch('focusin', { target: c.blk });
	expect(c.tip.hidden === false, 'focus shows');
	c.root().dispatch('keydown', { target: c.blk, key: 'Escape' });
	expect(c.tip.hidden === true, 'Escape hides');
	c.root().dispatch('focusin', { target: c.blk });
	c.root().dispatch('focusout', { target: c.blk });
	expect(c.tip.hidden === true, 'blur hides');
	c.root().dispatch('mouseover', { target: c.q('.juneau-rv-headline') });
	expect(c.tip.hidden === true, 'only tip blocks trigger');
	c.root().dispatch('mouseover', { target: c.blk });
	c.root().dispatch('mouseout', { target: c.blk, relatedTarget: c.blk });
	expect(c.tip.hidden === false, 'moving within the block keeps it');
	c.api.destroy();
	expect(c.q('.juneau-co-tooltip') === null && c.blk.getAttribute('aria-describedby') === null, 'destroy unlinks');
});

test('tip02_positionStaysInViewport', function (t) {
	const c = tipRun(t);
	t.env.window.innerWidth = 800;
	t.env.window.innerHeight = 600;
	c.tip.getBoundingClientRect = function () { return { left: 0, top: 0, right: 200, bottom: 40, width: 200, height: 40 }; };
	c.blk.getBoundingClientRect = function () { return { left: 790, top: 10, right: 800, bottom: 22, width: 10, height: 12 }; };
	c.root().dispatch('mouseover', { target: c.blk });
	expect(c.tip.style.left === '596px' && c.tip.style.top === '26px', 'clamped right, below: ' + c.tip.style.left + ',' + c.tip.style.top);
	c.blk.getBoundingClientRect = function () { return { left: 1, top: 580, right: 11, bottom: 592, width: 10, height: 12 }; };
	c.root().dispatch('mouseover', { target: c.blk });
	expect(c.tip.style.left === '4px' && c.tip.style.top === '536px', 'clamped left, flipped above: ' + c.tip.style.left + ',' + c.tip.style.top);
});

test('tip03_stepExitTooltip', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), { ev: 'end', id: 'a', status: 'fail', exit: 3 }]);
	const head = c.step('a').querySelector('.juneau-rv-step-head');
	c.root().dispatch('mouseover', { target: head });
	expect(c.q('.juneau-co-tooltip').hidden === false && c.q('.juneau-co-tooltip').textContent === 'exit 3', 'exit tip');
});

// @cases:caps

test('cap01_domBounds', function (t) {
	const c = mkRun(t);
	const specs = [];
	for (let i = 0; i < 400; i++) specs.push(['p' + i, i === 399 ? 'fail' : 'pass']);
	specs[100][1] = 'fail';
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's', specs)));
	const names = c.all('.juneau-co-block').map(function (b) { return b.__rv.test.name; });
	expect(names.length === 300, 'capped at 300: ' + names.length);
	expect(names.indexOf('p100') >= 0 && names.indexOf('p399') >= 0, 'failures always shown');
	const idx = names.map(function (n) { return Number(n.slice(1)); });
	expect(idx.every(function (v, i) { return i === 0 || idx[i - 1] < v; }), 'arrival order kept');
	expect(c.all('.juneau-rv-more').some(function (m) { return m.textContent === '+100 more'; }), '+N more');
	const d = mkRun(t);
	const many = [];
	for (let i = 0; i < 2001; i++) many.push({ ev: 'suite', step: 'a', fw: 'jest', suite: 'q' + i, counts: { pass: 1, fail: 0, skip: 0 } });
	d.send([stepOk('a')].concat(many));
	expect(d.all('.juneau-rv-suite').length === 2000, '2000 rows');
	expect(d.all('.juneau-rv-more').some(function (m) { return m.textContent === '…1 more suites'; }), 'overflow line');
});

test('cap02_collapsedBuildsNoBlocks', function (t) {
	const c = mkRun(t);
	const evs = [stepOk('a')];
	for (let s = 0; s < 12; s++)
		for (let i = 0; i < 1750; i++)
			evs.push({ ev: 'test', step: 'a', fw: 'jest', suite: 's' + s, name: 't' + i, status: 'pass' });
	c.send(evs);
	expect(c.all('.juneau-co-block').length === 0, 'no blocks in collapsed suites');
	expect(c.all('.juneau-rv-suite').length === 12, 'twelve rows');
	const dropped = c.api.stats().droppedTests;
	expect(dropped > 0, 'records beyond the pool are counted: ' + dropped);
	click(c, toggleOf(c, 's11'));
	expect(c.all('.juneau-rv-more').some(function (m) { return m.textContent.indexOf('more tests not shown') >= 0; }), 'dropped note when expanded');
});

// @cases:incremental

test('inc01_untouchedStepKeepsItsNode', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), stepOk('b')]);
	const li = c.step('a');
	li.marker = 42;
	c.send({ ev: 'test', step: 'b', fw: 'jest', suite: 's', name: 'x', status: 'pass' });
	expect(c.step('a') === li && li.marker === 42, 'step a node is untouched');
});

test('inc02_rowRebuiltOnlyWhenSuiteChanged', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 's1', [['a', 'pass']]), testEvs('a', 'jest', 's2', [['b', 'pass']])));
	const r1 = rowOf(c, 's1'), r2 = rowOf(c, 's2');
	c.send(testEvs('a', 'jest', 's2', [['c', 'pass']]));
	expect(rowOf(c, 's1') === r1, 's1 row reused');
	expect(rowOf(c, 's2') !== r2 && rowOf(c, 's2').querySelector('.juneau-rv-suite-sum').textContent === '2 ✓ ▸', 's2 row rebuilt');
	click(c, toggleOf(c, 's1'));
	expect(rowOf(c, 's1') !== r1, 'an expansion change rebuilds that row');
});

// @cases:compact

test('cmp01_compactMode', function (t) {
	const c = mkRun(t, { compact: true });
	c.send([stepOk('a')].concat(testEvs('a', 'jest', 'c1', [['a', 'pass']]), testEvs('a', 'jest', 'c2', [['a', 'pass'], ['b', 'pass']]),
		testEvs('a', 'jest', 'bad', [['x', 'fail', { msg: 'm' }]])));
	const line = c.q('.juneau-rv-clean-line');
	expect(line && line.textContent.indexOf('2 suites') === 0 && line.querySelector('.juneau-rv-suite-sum').textContent === ' · 3 ✓ ▸', 'one line for clean suites: ' + (line && line.textContent));
	expect(rowOf(c, 'c1') === undefined && toggleOf(c, 'bad').getAttribute('aria-expanded') === 'true', 'clean rows hidden, failing expanded');
	click(c, line.querySelector('.juneau-rv-suite-toggle'));
	expect(rowOf(c, 'c1') !== undefined && rowOf(c, 'c2') !== undefined, 'toggle reveals the clean rows');
	click(c, c.q('.juneau-rv-clean-line').querySelector('.juneau-rv-suite-toggle'));
	expect(rowOf(c, 'c1') === undefined, 'toggle hides them again');
});

// @cases:golden

function serialize(n) {
	const keys = Object.keys(n.attrs || {}).filter(function (k) { return k !== 'class' && k !== 'id'; }).sort();
	const cls = String(n.className || '').split(/\s+/).filter(Boolean).sort().join('.');
	let s = n.tagName.toLowerCase() + (cls ? '.' + cls : '');
	if (keys.length) s += '[' + keys.map(function (k) { return k + '=' + n.attrs[k]; }).join(',') + ']';
	if (n.hidden === true) s += '(hidden)';
	const els = n.childNodes.filter(function (c) { return c.nodeType === 1; });
	return s + '{' + (els.length ? els.map(serialize).join('') : String(n.textContent || '').replace(/\n/g, '\\n')) + '}';
}

function goldRun() {
	const e = [
		{ ev: 'step', id: 'compile', title: 'Compile', n: 1 },
		{ ev: 'end', id: 'compile', status: 'ok', ms: 1200 },
		{ ev: 'step', id: 'test', title: 'Test', n: 2, rawLine: 5 },
		{ ev: 'suite', step: 'test', fw: 'surefire', suite: 'com.acme.A', counts: { pass: 8, fail: 0, skip: 1 } },
		{ ev: 'suite', step: 'test', fw: 'surefire', suite: 'com.acme.B', counts: { pass: 3, fail: 0, skip: 0 } },
		{ ev: 'note', step: 'test', level: 'info', text: 'retried once', href: 'https://example.org/log' },
		{ ev: 'end', id: 'test', status: 'fail', ms: 5300, exit: 1 }
	];
	for (let i = 0; i < 22; i++) e.push({ ev: 'test', step: 'test', fw: 'jest', suite: 'web.test.js', name: 'case ' + i, status: i === 7 ? 'fail' : i === 9 ? 'skip' : 'pass', ms: 10 + i,
		msg: i === 7 ? 'expected 1\nreceived 2' : undefined, trace: i === 7 ? 'at web.test.js:7' : undefined, rawLine: i + 1 });
	for (let i = 0; i < 6; i++) e.push({ ev: 'test', step: 'test', fw: 'jest', suite: 'api.test.js', name: 'api ' + i, status: 'pass' });
	e.push({ ev: 'step', id: 'deploy', title: 'Deploy', n: 3, status: 'waiting' });
	e.push({ ev: 'note', level: 'warn', text: 'run note' });
	e.push({ ev: 'done', status: 'fail' });
	return e;
}

const GOLD = {};

function golden(t, name, opts, evs) {
	const c = mkRun(t, opts);
	c.send(evs);
	const got = serialize(c.root());
	if (process.env.UPDATE_GOLDEN) { process.stderr.write('GOLD ' + name + ' ' + JSON.stringify(got) + '\n'); return; }
	expect(got === GOLD[name], name + ' differs:\n' + got);
	return c;
}

test('gold01_fullGolden', function (t) {
	golden(t, 'full', { id: 'g', rawHref: '/raw#L{line}' }, goldRun());
});

test('gold02_compactGolden', function (t) {
	golden(t, 'compact', { id: 'g', compact: true, rawHref: '/raw#L{line}' }, goldRun());
});

test('gold03_replaceFlow', function (t) {
	const c = mkRun(t);
	c.send([stepOk('a'), { ev: 'suite', step: 'a', fw: 'jest', suite: 'S', counts: { pass: 2, fail: 1, skip: 0 } }]);
	expect(c.head() === '⋯ Running' && c.all('.juneau-co-block').length === 0, 'live: counts only');
	c.send({ ev: 'end', id: 'a', status: 'fail' });
	c.send({ ev: 'replace', step: 'a' });
	c.send(testEvs('a', 'jest', 'S', [['one', 'pass'], ['two', 'pass'], ['three', 'fail', { msg: 'no' }]]));
	c.send({ ev: 'done', status: 'fail' });
	expect(c.head() === '✗ Failed at A', 'headline: ' + c.head());
	expect(c.all('.juneau-co-block').length === 3 && toggleOf(c, 'S').getAttribute('aria-expanded') === 'true', 'blocks replace the counts');
	golden(t, 'replace', { id: 'g' }, [stepOk('a'), { ev: 'suite', step: 'a', fw: 'jest', suite: 'S', counts: { pass: 2, fail: 1, skip: 0 } },
		{ ev: 'end', id: 'a', status: 'fail' }, { ev: 'replace', step: 'a' }].concat(testEvs('a', 'jest', 'S', [['one', 'pass'], ['two', 'pass'], ['three', 'fail', { msg: 'no' }]]), [{ ev: 'done', status: 'fail' }]));
});

test('gold04_idempotentAppend', function (t) {
	const c = mkRun(t);
	const evs = goldRun().map(function (e, i) { return Object.assign({ seq: i + 1 }, e); });
	c.send(evs);
	const first = serialize(c.root());
	c.send(evs);
	expect(serialize(c.root()) === first, 'DOM identical');
	expect(c.api.stats().skipped === evs.length, 'skipped ' + c.api.stats().skipped + ' of ' + evs.length);
});


// @golden

// Reviewed by eye against the render design; regenerate with UPDATE_GOLDEN=1 (prints GOLD lines to stderr).
GOLD.full = "section.juneau-rv[aria-label=Run,data-juneau-rv-status=fail,role=region]{div.juneau-rv-summary{p.juneau-rv-headline[aria-atomic=true,aria-live=polite]{✗ Failed at Test}p.juneau-rv-counts{Surefire 11 ✓ 1 ○ · Jest 26 ✓ 1 ✗ 1 ○}ul.juneau-rv-failures{li.juneau-rv-failure-item{button.juneau-rv-failure[data-juneau-rv-act=failure,type=button]{web.test.js › case 7 – expected 1}button.juneau-rv-failure-trace[aria-label=Show details for case 7,data-juneau-rv-act=failure-trace,type=button]{details}}}}ol.juneau-rv-steps{li.juneau-rv-step[data-state=ok,data-step=compile]{div.juneau-rv-step-head{span.juneau-rv-glyph.juneau-rv-state-ok[aria-hidden=true]{✓}span.juneau-co-sr{passed: }span.juneau-rv-title{1. Compile}span.juneau-rv-dur{1.2 s}}div.juneau-rv-suites{}ul.juneau-rv-notes(hidden){}}li.juneau-rv-step[data-state=fail,data-step=test]{div.juneau-rv-step-head[data-juneau-co-tip=exit 1,tabindex=0]{span.juneau-rv-glyph.juneau-rv-state-fail[aria-hidden=true]{✗}span.juneau-co-sr{failed: }a.juneau-rv-title[href=/raw#L5,rel=nofollow noreferrer]{2. Test}span.juneau-rv-dur{5.3 s}}div.juneau-rv-suites{div.juneau-rv-suite[data-juneau-rv-key=test/surefire/com.acme.A,tabindex=-1]{span.juneau-rv-suite-name{com.acme.A}span.juneau-rv-suite-sum{8 ✓ 0 ✗ 1 ○}}div.juneau-rv-suite[data-juneau-rv-key=test/surefire/com.acme.B,tabindex=-1]{span.juneau-rv-suite-name{com.acme.B}span.juneau-rv-suite-sum{3 ✓ 0 ✗ 0 ○}}div.juneau-rv-suite[data-juneau-rv-key=test/jest/web.test.js,tabindex=-1]{button.juneau-rv-suite-toggle[aria-expanded=true,data-juneau-rv-act=toggle-suite,data-juneau-rv-key=test/jest/web.test.js,type=button]{web.test.js}span.juneau-rv-suite-sum{20 ✓ 1 ✗ 1 ○ ▾}div.juneau-rv-strip{a.juneau-co-block.juneau-co-fill-success[aria-label=case 0, pass, 10 ms,data-juneau-co-tip=case 0\npass · 10 ms,href=/raw#L1,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 1, pass, 11 ms,data-juneau-co-tip=case 1\npass · 11 ms,href=/raw#L2,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 2, pass, 12 ms,data-juneau-co-tip=case 2\npass · 12 ms,href=/raw#L3,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 3, pass, 13 ms,data-juneau-co-tip=case 3\npass · 13 ms,href=/raw#L4,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 4, pass, 14 ms,data-juneau-co-tip=case 4\npass · 14 ms,href=/raw#L5,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 5, pass, 15 ms,data-juneau-co-tip=case 5\npass · 15 ms,href=/raw#L6,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 6, pass, 16 ms,data-juneau-co-tip=case 6\npass · 16 ms,href=/raw#L7,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-error[aria-label=case 7, fail, 17 ms,data-juneau-co-tip=case 7\nfail · 17 ms\nexpected 1,href=/raw#L8,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 8, pass, 18 ms,data-juneau-co-tip=case 8\npass · 18 ms,href=/raw#L9,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-block-empty[aria-label=case 9, skip, 19 ms,data-juneau-co-tip=case 9\nskip · 19 ms,href=/raw#L10,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 10, pass, 20 ms,data-juneau-co-tip=case 10\npass · 20 ms,href=/raw#L11,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 11, pass, 21 ms,data-juneau-co-tip=case 11\npass · 21 ms,href=/raw#L12,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 12, pass, 22 ms,data-juneau-co-tip=case 12\npass · 22 ms,href=/raw#L13,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 13, pass, 23 ms,data-juneau-co-tip=case 13\npass · 23 ms,href=/raw#L14,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 14, pass, 24 ms,data-juneau-co-tip=case 14\npass · 24 ms,href=/raw#L15,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 15, pass, 25 ms,data-juneau-co-tip=case 15\npass · 25 ms,href=/raw#L16,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 16, pass, 26 ms,data-juneau-co-tip=case 16\npass · 26 ms,href=/raw#L17,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 17, pass, 27 ms,data-juneau-co-tip=case 17\npass · 27 ms,href=/raw#L18,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 18, pass, 28 ms,data-juneau-co-tip=case 18\npass · 28 ms,href=/raw#L19,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 19, pass, 29 ms,data-juneau-co-tip=case 19\npass · 29 ms,href=/raw#L20,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 20, pass, 30 ms,data-juneau-co-tip=case 20\npass · 30 ms,href=/raw#L21,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 21, pass, 31 ms,data-juneau-co-tip=case 21\npass · 31 ms,href=/raw#L22,rel=nofollow noreferrer]{}}}div.juneau-rv-suite[data-juneau-rv-key=test/jest/api.test.js,tabindex=-1]{button.juneau-rv-suite-toggle[aria-expanded=false,data-juneau-rv-act=toggle-suite,data-juneau-rv-key=test/jest/api.test.js,type=button]{api.test.js}span.juneau-rv-suite-sum{6 ✓ ▸}}}ul.juneau-rv-notes{li.juneau-rv-note.juneau-rv-note-info{span.juneau-rv-note-glyph[aria-hidden=true]{ⓘ}span.juneau-co-sr{info: }a[href=https://example.org/log,rel=noopener noreferrer,target=_blank]{retried once}}}}li.juneau-rv-step[data-state=fail,data-step=deploy]{div.juneau-rv-step-head{span.juneau-rv-glyph.juneau-rv-state-fail[aria-hidden=true]{✗}span.juneau-co-sr{failed: }span.juneau-rv-title{3. Deploy}}div.juneau-rv-suites{}ul.juneau-rv-notes(hidden){}}}ul.juneau-rv-run-notes{li.juneau-rv-note.juneau-rv-note-warn{span.juneau-rv-note-glyph[aria-hidden=true]{⚠}span.juneau-co-sr{warning: }span{run note}}}div.juneau-co-tooltip[role=tooltip](hidden){}}";
GOLD.compact = "section.juneau-rv.juneau-rv-compact[aria-label=Run,data-juneau-rv-status=fail,role=region]{div.juneau-rv-summary{p.juneau-rv-headline[aria-atomic=true,aria-live=polite]{✗ Failed at Test}p.juneau-rv-counts{Surefire 11 ✓ 1 ○ · Jest 26 ✓ 1 ✗ 1 ○}ul.juneau-rv-failures{li.juneau-rv-failure-item{button.juneau-rv-failure[data-juneau-rv-act=failure,type=button]{web.test.js › case 7 – expected 1}button.juneau-rv-failure-trace[aria-label=Show details for case 7,data-juneau-rv-act=failure-trace,type=button]{details}}}}ol.juneau-rv-steps{li.juneau-rv-step[data-state=ok,data-step=compile]{div.juneau-rv-step-head{span.juneau-rv-glyph.juneau-rv-state-ok[aria-hidden=true]{✓}span.juneau-co-sr{passed: }span.juneau-rv-title{1. Compile}span.juneau-rv-dur{1.2 s}}div.juneau-rv-suites{}ul.juneau-rv-notes(hidden){}}li.juneau-rv-step[data-state=fail,data-step=test]{div.juneau-rv-step-head[data-juneau-co-tip=exit 1,tabindex=0]{span.juneau-rv-glyph.juneau-rv-state-fail[aria-hidden=true]{✗}span.juneau-co-sr{failed: }a.juneau-rv-title[href=/raw#L5,rel=nofollow noreferrer]{2. Test}span.juneau-rv-dur{5.3 s}}div.juneau-rv-suites{div.juneau-rv-clean-line.juneau-rv-suite{button.juneau-rv-suite-toggle[aria-expanded=false,data-juneau-rv-act=toggle-clean,data-juneau-rv-key=test,type=button]{3 suites}span.juneau-rv-suite-sum{ · 17 ✓ ▸}}div.juneau-rv-suite[data-juneau-rv-key=test/jest/web.test.js,tabindex=-1]{button.juneau-rv-suite-toggle[aria-expanded=true,data-juneau-rv-act=toggle-suite,data-juneau-rv-key=test/jest/web.test.js,type=button]{web.test.js}span.juneau-rv-suite-sum{20 ✓ 1 ✗ 1 ○ ▾}div.juneau-rv-strip{a.juneau-co-block.juneau-co-fill-success[aria-label=case 0, pass, 10 ms,data-juneau-co-tip=case 0\npass · 10 ms,href=/raw#L1,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 1, pass, 11 ms,data-juneau-co-tip=case 1\npass · 11 ms,href=/raw#L2,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 2, pass, 12 ms,data-juneau-co-tip=case 2\npass · 12 ms,href=/raw#L3,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 3, pass, 13 ms,data-juneau-co-tip=case 3\npass · 13 ms,href=/raw#L4,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 4, pass, 14 ms,data-juneau-co-tip=case 4\npass · 14 ms,href=/raw#L5,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 5, pass, 15 ms,data-juneau-co-tip=case 5\npass · 15 ms,href=/raw#L6,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 6, pass, 16 ms,data-juneau-co-tip=case 6\npass · 16 ms,href=/raw#L7,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-error[aria-label=case 7, fail, 17 ms,data-juneau-co-tip=case 7\nfail · 17 ms\nexpected 1,href=/raw#L8,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 8, pass, 18 ms,data-juneau-co-tip=case 8\npass · 18 ms,href=/raw#L9,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-block-empty[aria-label=case 9, skip, 19 ms,data-juneau-co-tip=case 9\nskip · 19 ms,href=/raw#L10,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 10, pass, 20 ms,data-juneau-co-tip=case 10\npass · 20 ms,href=/raw#L11,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 11, pass, 21 ms,data-juneau-co-tip=case 11\npass · 21 ms,href=/raw#L12,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 12, pass, 22 ms,data-juneau-co-tip=case 12\npass · 22 ms,href=/raw#L13,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 13, pass, 23 ms,data-juneau-co-tip=case 13\npass · 23 ms,href=/raw#L14,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 14, pass, 24 ms,data-juneau-co-tip=case 14\npass · 24 ms,href=/raw#L15,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 15, pass, 25 ms,data-juneau-co-tip=case 15\npass · 25 ms,href=/raw#L16,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 16, pass, 26 ms,data-juneau-co-tip=case 16\npass · 26 ms,href=/raw#L17,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 17, pass, 27 ms,data-juneau-co-tip=case 17\npass · 27 ms,href=/raw#L18,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 18, pass, 28 ms,data-juneau-co-tip=case 18\npass · 28 ms,href=/raw#L19,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 19, pass, 29 ms,data-juneau-co-tip=case 19\npass · 29 ms,href=/raw#L20,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 20, pass, 30 ms,data-juneau-co-tip=case 20\npass · 30 ms,href=/raw#L21,rel=nofollow noreferrer]{}a.juneau-co-block.juneau-co-fill-success[aria-label=case 21, pass, 31 ms,data-juneau-co-tip=case 21\npass · 31 ms,href=/raw#L22,rel=nofollow noreferrer]{}}}}ul.juneau-rv-notes{li.juneau-rv-note.juneau-rv-note-info{span.juneau-rv-note-glyph[aria-hidden=true]{ⓘ}span.juneau-co-sr{info: }a[href=https://example.org/log,rel=noopener noreferrer,target=_blank]{retried once}}}}li.juneau-rv-step[data-state=fail,data-step=deploy]{div.juneau-rv-step-head{span.juneau-rv-glyph.juneau-rv-state-fail[aria-hidden=true]{✗}span.juneau-co-sr{failed: }span.juneau-rv-title{3. Deploy}}div.juneau-rv-suites{}ul.juneau-rv-notes(hidden){}}}ul.juneau-rv-run-notes{li.juneau-rv-note.juneau-rv-note-warn{span.juneau-rv-note-glyph[aria-hidden=true]{⚠}span.juneau-co-sr{warning: }span{run note}}}div.juneau-co-tooltip[role=tooltip](hidden){}}";
GOLD.replace = "section.juneau-rv[aria-label=Run,data-juneau-rv-status=fail,role=region]{div.juneau-rv-summary{p.juneau-rv-headline[aria-atomic=true,aria-live=polite]{✗ Failed at A}p.juneau-rv-counts{Jest 2 ✓ 1 ✗}ul.juneau-rv-failures{li.juneau-rv-failure-item{button.juneau-rv-failure[data-juneau-rv-act=failure,type=button]{S › three – no}}}}ol.juneau-rv-steps{li.juneau-rv-step[data-state=fail,data-step=a]{div.juneau-rv-step-head{span.juneau-rv-glyph.juneau-rv-state-fail[aria-hidden=true]{✗}span.juneau-co-sr{failed: }span.juneau-rv-title{A}}div.juneau-rv-suites{div.juneau-rv-suite[data-juneau-rv-key=a/jest/S,tabindex=-1]{button.juneau-rv-suite-toggle[aria-expanded=true,data-juneau-rv-act=toggle-suite,data-juneau-rv-key=a/jest/S,type=button]{S}span.juneau-rv-suite-sum{2 ✓ 1 ✗ ▾}div.juneau-rv-strip{span.juneau-co-block.juneau-co-fill-success[aria-label=one, pass,data-juneau-co-tip=one\npass,role=img,tabindex=0]{}span.juneau-co-block.juneau-co-fill-success[aria-label=two, pass,data-juneau-co-tip=two\npass,role=img,tabindex=0]{}span.juneau-co-block.juneau-co-fill-error[aria-label=three, fail,data-juneau-co-tip=three\nfail\nno,role=img,tabindex=0]{}}}}ul.juneau-rv-notes(hidden){}}}ul.juneau-rv-run-notes(hidden){}div.juneau-co-tooltip[role=tooltip](hidden){}}";

// @cases:end

(async function main() {
	const out = {};
	for (const c of cases) {
		try {
			await c.fn(fresh());
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
