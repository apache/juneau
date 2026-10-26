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
 * same-origin-path.cjs - runs the shared same-origin path corpus (same-origin-path-corpus.json, which Java's
 * SameOriginPath_Corpus_Test also runs) through the SHIPPED JS guards.  Every guard is extracted, as text, from the
 * production file and evaluated in a bare context, so a rewritten copy cannot stand in for it.  Prints ONE JSON
 * report; every assertion lives in ViewsJs_SameOriginPath_Test.
 *
 *   Usage:  node same-origin-path.cjs <juneau-views.js> <juneau-console.js> <juneau-charts.js> <juneau-joblog.js>
 *                                     <juneau-terminal-view.js> <juneau-calendar.js> <juneau-badges.js> <juneau-data.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const [viewsPath, consolePath, chartsPath, joblogPath, terminalViewPath, calendarPath, badgesPath, dataPath] = process.argv.slice(2);
if (!dataPath) {
	console.error('usage: node same-origin-path.cjs <views> <console> <charts> <joblog> <terminal-view> <calendar> <badges> <data>');
	process.exit(2);
}
const corpus = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../resources/same-origin-path-corpus.json'), 'utf8'));
const BASE = 'https://app.example/base/page';
const src = {
	views: fs.readFileSync(viewsPath, 'utf8'),
	console: fs.readFileSync(consolePath, 'utf8'),
	charts: fs.readFileSync(chartsPath, 'utf8'),
	joblog: fs.readFileSync(joblogPath, 'utf8'),
	terminalView: fs.readFileSync(terminalViewPath, 'utf8'),
	calendar: fs.readFileSync(calendarPath, 'utf8'),
	badges: fs.readFileSync(badgesPath, 'utf8'),
	data: fs.readFileSync(dataPath, 'utf8')
};

// A function declared at one tab of indent ends at the next line that is exactly one tab and "}" (or on its own line).
function fnText(file, name) {
	const start = src[file].indexOf('\n\tfunction ' + name + '(');
	if (start < 0) throw new Error(file + ': no function ' + name);
	const from = start + 1;
	const eol = src[file].indexOf('\n', from);
	const first = src[file].slice(from, eol);
	if (/\}\s*$/.test(first) && first.indexOf('{') > 0 && (first.match(/\{/g) || []).length === (first.match(/\}/g) || []).length)
		return first;
	const end = src[file].indexOf('\n\t}\n', from);
	if (end < 0) throw new Error(file + ': unterminated function ' + name);
	return src[file].slice(from, end + 3);
}
function constText(file, name) {
	const m = new RegExp(String.raw`\n\tconst ` + name + String.raw` = [^\n]*;`).exec(src[file]);
	if (!m) throw new Error(file + ': no const ' + name);
	return m[0].slice(1);
}

// One evaluated guard: the named production functions, plus any production consts they close over.
function load(file, fns, consts, target, extraGlobals) {
	const text = (consts || []).map(c => constText(file, c)).concat(fns.map(f => fnText(file, f))).join('\n');
	const ctx = vm.createContext({ URL: URL, ...extraGlobals || {} });
	vm.runInContext(text + '\nthis.__guard = ' + target + ';', ctx); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	return { fn: ctx.__guard, text: fnText(file, target) };
}

const HELPER = 'hasUnsafeUrlChars';
const joblogWindow = { location: { href: BASE, origin: new URL(BASE).origin, pathname: '/base/page' } };

const GUARDS = {
	// exact SafePathTemplate parity
	chartsIsSafeHref: { kind: 'core', g: load('charts', [HELPER, 'isSafeHref'], [], 'isSafeHref'), file: 'charts' },
	viewsIsSafeDetailUrl: { kind: 'core', g: load('views', [HELPER, 'hasDotDotSegment', 'isSafeDetailUrl'], [], 'isSafeDetailUrl'), file: 'views' },
	calendarIsSafeDocumentUrl: { kind: 'core', g: load('calendar', [HELPER, 'isSafeDocumentUrl'], [], 'isSafeDocumentUrl'), file: 'calendar' },
	terminalViewIsSafePath: { kind: 'core', g: load('terminalView', [HELPER, 'isSafePath'], [], 'isSafePath'), file: 'terminalView' },
	badgesIsSafeBadgePath: { kind: 'core', g: load('badges', [HELPER, 'isSafeBadgePath'], [], 'isSafeBadgePath'), file: 'badges' },
	dataIsSafeDataPath: { kind: 'core', g: load('data', [HELPER, 'isSafeDataPath'], [], 'isSafeDataPath'), file: 'data' },
	// SafePathTemplate parity, plus "no placeholder"
	consoleIsSafeChromeEndpoint: { kind: 'chrome', g: load('console', [HELPER, 'isSafeChromeEndpoint'], [], 'isSafeChromeEndpoint'), file: 'console' },
	// strip tab/CR/LF, then the core
	terminalViewIsSafeLineHref: { kind: 'line', g: load('terminalView', [HELPER, 'isStr', 'stripTabCrLf', 'isSafePath', 'isSafeLineHref'], ['FRAGMENT_HREF'], 'isSafeLineHref'), file: 'terminalView' },
	// prefix-reject, then the guard's own allowlist
	consoleIsSafeHref: { kind: 'allow', g: load('console', [HELPER, 'isProtocolRelativeUrl', 'isSafeHref'], [], 'isSafeHref'), file: 'console' },
	markdownIsSafeHref: { kind: 'allow', g: load('views', [HELPER, 'isProtocolRelativeUrl', 'isSafeMarkdownHref'], [], 'isSafeMarkdownHref'), file: 'views' },
	imageIsSafeSrc: { kind: 'allow', g: load('views', [HELPER, 'isProtocolRelativeUrl', 'isSafeImageSrc'], [], 'isSafeImageSrc'), file: 'views' }
};
const OVERRIDE_KEY = { line: 'isSafeLineHref' };
// A same-origin path, or an absolute http(s) URL on this page's origin: what fetchJson and the post-fill recheck use.
const FETCH_ORIGIN = 'https://app.example';
const isSafeFetchUrl = load('console', [HELPER, 'isSafeChromeEndpoint', 'isSafeFetchUrl'], [], 'isSafeFetchUrl',
	{ window: { location: { origin: FETCH_ORIGIN } } });
const FETCH_CASES = [
	['same-origin absolute', FETCH_ORIGIN + '/x', true],
	['same-origin absolute with query', FETCH_ORIGIN + '/x?a=b', true],
	['same-origin path', '/x', true],
	['other host', 'https://evil.test/x', false],
	['other scheme, same host', 'http://app.example/x', false],
	['scheme without slashes', 'http:evil.test', false],
	['same host, other port', 'https://app.example:8443/x', false],
	['userinfo trick', 'https://app.example@evil.test/x', false],
	['protocol-relative', '//evil.test/x', false],
	['backslash host', String.raw`https:\\evil.test/x`, false],
	['leading space', ' ' + FETCH_ORIGIN + '/x', false],
	['trailing newline', FETCH_ORIGIN + '/x\n', false],
	['javascript scheme', 'javascript:alert(1)', false],
	['non-string', null, false]
];
const fetchUrl = FETCH_CASES.map(c => ({ name: c[0], input: c[1], expected: c[2], got: isSafeFetchUrl.fn(c[1]) }));

const joblog = load('joblog', [HELPER, 'isSameOrigin', 'isSafeEndpoint'], [], 'isSafeEndpoint', { window: joblogWindow });

function crossHostOf(input) { // NOSONAR javascript:S3800 - returns the string 'parseError' as a distinct label alongside the boolean
	try {
		return new URL(input, BASE).origin !== new URL(BASE).origin;
	} catch (e) { // NOSONAR javascript:S2486 - the failure is an expected outcome here and the fallback handles it
		return 'parseError';
	}
}

const mismatches = [];
const crossHostOverrides = [];
const labelMismatch = [];
const joblogAccepted = [];
const joblogTrailingAccepted = [];
for (const row of corpus.cases) {
	const computed = crossHostOf(row.input);
	if (computed !== row.crossHost) labelMismatch.push({ row: row.name, label: row.crossHost, computed: computed });
	for (const name of Object.keys(GUARDS)) {
		const spec = GUARDS[name];
		const overrides = row.guards || {};
		const key = OVERRIDE_KEY[spec.kind] || name;
		const expected = Object.hasOwn(overrides, key) ? overrides[key] : row.accepted;
		if (row.crossHost === true && expected === true) crossHostOverrides.push({ guard: name, row: row.name });
		let got;
		try { got = spec.g.fn(row.input); } catch (e) { got = 'threw: ' + e.message; }
		if (got !== expected) mismatches.push({ guard: name, row: row.name, expected: expected, got: got });
	}
	if (row.crossHost === true && joblog.fn(row.input) !== false) joblogAccepted.push(row.name);
	// A trailing space or control character is refused by Java and by the shared helper, so joblog must refuse it too.
	if (row.input.length > 0 && row.input.codePointAt(row.input.length - 1) <= 32 && joblog.fn(row.input) !== false) joblogTrailingAccepted.push(row.name);
}

// The shared prefix rule must be one comment-identical helper in each file that applies it.
const helperFiles = ['views', 'console', 'charts', 'terminalView', 'calendar', 'badges', 'joblog', 'data'];
const helperTexts = helperFiles.map(f => fnText(f, HELPER));
const helperIdentical = helperTexts.every(t => t === helperTexts[0]);
// A guard that never calls the helper is a stub or a regression; the line guards reach it through isSafePath.
const notUsingHelper = Object.keys(GUARDS).filter(n => GUARDS[n].kind !== 'line' && GUARDS[n].g.text.indexOf(HELPER + '(') < 0);

const out = {
	count: corpus.cases.length,
	guards: Object.keys(GUARDS),
	mismatches: mismatches,
	labelMismatch: labelMismatch,
	crossHostOverrides: crossHostOverrides,
	fetchUrl: fetchUrl,
	joblogAccepted: joblogAccepted,
	joblogTrailingAccepted: joblogTrailingAccepted,
	helperIdentical: helperIdentical,
	helperText: helperTexts[0],
	notUsingHelper: notUsingHelper,
	nodeFoldsBackslash: crossHostOf(String.raw`\\evil.test/x`) === true && crossHostOf(String.raw`/\evil.test`) === true,
	nodeStripsLeadingSpace: crossHostOf(' //evil.test') === true
};
process.stdout.write(JSON.stringify(out));
