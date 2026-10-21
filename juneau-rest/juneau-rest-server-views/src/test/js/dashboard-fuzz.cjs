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
 * dashboard-fuzz.cjs - seeded share-link fuzz for juneau-dashboard.js: decodeShareLink, resolveInitial and the section
 * validators over 2400 reproducible cases, plus the banned-key, depth, cycle and size-cap edge cases.  Every
 * assertion lives in ConsoleDashboardFuzz_Test.
 *
 *   Usage:  node dashboard-fuzz.cjs <juneau-dashboard.js>
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const [dashPath] = process.argv.slice(2);
if (!dashPath) {
	console.error('usage: node dashboard-fuzz.cjs <juneau-dashboard.js>');
	process.exit(2);
}
const src = fs.readFileSync(dashPath, 'utf8');

function load() {
	const ctx = {
		console: { error: () => {}, warn: () => {}, log: () => {} },
		TextEncoder, TextDecoder, btoa, atob,
		setTimeout: () => 0, clearTimeout: () => {}
	};
	ctx.window = ctx;
	vm.createContext(ctx);
	vm.runInContext(src, ctx, { filename: 'juneau-dashboard.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	return { w: ctx, I: ctx.JuneauDashboard._internals };
}

const out = {};

	// 1. The seeded fuzz.
	try {
		const r = load(), I = r.I;
		let seed = 0x5eed0694;
		const rnd = () => {   // mulberry32
			seed = (seed + 0x6D2B79F5) | 0; // NOSONAR javascript:S7767 - 32-bit wrap is the PRNG's design
			let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
			t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
			return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
		};
		const int = n => Math.floor(rnd() * n);
		const pick = a => a[int(a.length)];
		const b64 = s => Buffer.from(s, 'utf8').toString('base64url');
		const P = { ns: 'app.dash', version: 2, topics: ['filter:range', 'filter:q', 'filter:reasons', 'zoom:daily', 'legend:daily'], shareLink: true, decoders: [] };
		const range = { id: 'range', kind: 'range', topic: 'filter:range', presets: ['7d', '30d'], custom: true, defaultPreset: '30d' };
		const choice = { id: 'reasons', kind: 'choice', topic: 'filter:reasons', source: 'orders', dim: 'reason', max: 50 };
		const BANNED = ['__proto__', 'constructor', 'prototype'];
		const word = () => pick(['x', 'late', '7d', '30d', 'custom', '', 'é', '\u0000', '2026-01-01T00:00:00Z']);
		const anyValue = d => {
			const k = int(d > 3 ? 5 : 8);
			if (k === 0) return null;
			if (k === 1) return int(3) ? int(200) - 50 : pick([Number.NaN, Infinity, -0, 1e308]);
			if (k === 2) return word();
			if (k === 3) return !!int(2);
			if (k === 4) return undefined;
			if (k < 7) { const a = []; for (let i = int(4); i > 0; i--) { a.push(anyValue(d + 1)); } return a; }
			const o = {}; for (let i = int(4); i > 0; i--) { o[pick(['schemaVersion', 'viewId', 'search', 'columns', 'options', 'start', 'end', 'selected', 'range', 'reason', 'zz'])] = anyValue(d + 1); } return o;
		};
		// A payload close to valid, so the validators see near misses as well as junk.
		const payload = topic => {
			const f = topic.slice(0, topic.indexOf(':'));
			let p;
			if (f === 'zoom') p = { schemaVersion: 1, start: pick([0, 10, 100, 120, -1, null, 'a']), end: pick([50, 100, null]), startValue: pick(['Mon', 5, null, {}]), endValue: pick([7, null]) };
			else if (f === 'legend') p = { schemaVersion: 1, selected: pick([{ a: true }, { a: 'yes' }, [], null]) };
			else p = { schemaVersion: pick([1, 1, 1, 2, '1']), viewId: pick(['d', 'd', 7]), search: pick([undefined, 'x', 3]),
				columns: pick([undefined, { reason: ['late'] }, { reason: [1] }, { other: ['a'] }, []]),
				options: pick([undefined, { range: '7d' }, { range: '90d' }, { range: 'custom' }]),
				start: pick([undefined, '2026-01-01T00:00:00Z', '2026-02-01T00:00:00Z', 'nope']), end: pick([undefined, '2026-01-15T00:00:00Z', 'nope']) };
			if (!int(4)) p['zz' + int(9)] = anyValue(2);   // unknown keys are dropped
			return p;
		};
		const topicMap = () => {
			const t = {};
			for (const topic of P.topics) if (int(2)) t[topic] = payload(topic);
			if (!int(3)) t[pick(['filter:other', 'cmd:x', 'zoom:nope', 'nocolon'])] = payload('filter:q');   // unknown topics are ignored
			return t;
		};
		const nested = (key, depth) => {
			let s = '{"' + key + '":1}';
			for (let i = 0; i < depth; i++) s = '{"a":' + s + '}';
			return s;
		};
		const goodLink = () => I.encodeLink(P, topicMap());
		const HASH = [
			// [name, bad, make]
			['randomBytes', true, () => { let s = '#s='; for (let i = int(60); i > 0; i--) { s += String.fromCodePoint(int(256)); } return s; }],
			['truncatedBase64', true, () => { const g = goodLink(); return g.slice(0, 3 + int(Math.max(1, g.length - 4))) + pick(['', '=', '%', '*']); }],
			['notJson', true, () => '#s=' + b64(pick(['{', '{"v":2,', 'undefined', "{'v':2}", '[1,]']) + word())],
			['wrongType', true, () => '#s=' + b64(JSON.stringify(pick([[1], 'x', 3, null, true, { v: 2, ns: 'app.dash', t: [1] }, { v: 2, ns: 'app.dash' }, { v: 2, ns: 'app.dash', t: 'x' }])))],
			['oversize', true, () => '#s=' + 'A'.repeat(8190 + int(5000))],
			['wrongNsOrV', true, () => I.encodeLink(pick([{ ns: 'other', version: 2 }, { ns: 'app.dash', version: 1 }, { ns: 'app.dash', version: '2' }].map(x => ({ ...P, ...x }))), topicMap())],
			['bannedKey', true, () => {
				const json = JSON.stringify({ v: 2, ns: 'app.dash', t: { 'filter:q': { schemaVersion: 1, viewId: 'd', options: JSON.parse(nested('x', int(5))) } } });
				return '#s=' + b64(json.replace('"x":1', '"' + pick(BANNED) + '":' + pick(['1', '{}', '{"polluted":true}'])));
			}],
			['tooDeep', true, () => '#s=' + b64('{"v":2,"ns":"app.dash","t":' + nested('y', 65 + int(200)) + '}')],
			['notMine', true, () => pick(['', '#', '#other', '#s', '#state=x'])],
			['unknownOnly', false, () => '#s=' + b64(JSON.stringify({ v: 2, ns: 'app.dash', t: { 'filter:other': payload('filter:q') }, extra: 1 }))],
			['sectionRules', false, goodLink],   // structurally valid; the owners' rules decide
			['valid', false, goodLink]
		];
		const DEC = {
			'app.throws': () => { throw new Error('boom'); },
			'app.null': () => null,
			'app.empty': () => ({}),
			'app.array': () => [{ 'filter:q': {} }],
			'app.number': () => 7,
			'app.proto': () => JSON.parse('{"filter:q":{"schemaVersion":1,"viewId":"d","search":"p","' + pick(BANNED) + '":{}}}'),
			'app.foreign': () => ({ 'filter:zzz': payload('filter:q') }),
			'app.wins': () => topicMap()
		};
		const decoders = new Map(Object.entries(DEC));
		const NAMES = Object.keys(DEC).concat(['app.missing']);
		const WARNS = new Set(['app.throws', 'app.proto', 'app.missing']);
		const SAVED = [
			['valid', () => ({ v: 2, topics: topicMap() })],
			['oldVersion', () => ({ v: 1, topics: topicMap() })],
			['junk', () => pick(['junk', 3, [], { v: 2 }, { v: 2, topics: [] }])],
			['banned', () => JSON.parse('{"v":2,"topics":{"filter:q":{"schemaVersion":1,"viewId":"d","' + pick(BANNED) + '":1}}}')],
			['none', () => null]
		];
		const clean = (v, depth) => {
			if (v === null || typeof v !== 'object') return true;
			if (!Array.isArray(v) && Object.getPrototypeOf(v) !== null) return false;
			for (const k of Object.keys(v)) if (BANNED.indexOf(k) >= 0 || !clean(v[k], depth + 1)) return false;
			return true;
		};

		const failures = [], sources = {}, perCategory = {};
		const CASES = 2400;
		for (let n = 0; n < CASES; n++) {
			const [cat, bad, make] = HASH[n % HASH.length];
			perCategory[cat] = (perCategory[cat] || 0) + 1;
			const fail = why => { if (failures.length < 20) failures.push('case ' + n + ' (' + cat + '): ' + why); };
			try {
				const hash = make();
				const names = NAMES.filter(() => !int(4));
				const persist = { ...P, decoders: [...names].sort(() => rnd() - 0.5) };
				const [savedCat, savedMake] = pick(SAVED);
				const saved = savedMake();

				const d = I.decodeShareLink(hash.indexOf('#s=') === 0 ? hash : '#s=' + hash.slice(1), P);
				const dk = Object.keys(d);
				if (!(dk.length === 1 && (dk[0] === 'error' ? typeof d.error === 'string' : dk[0] === 't' && Object.getPrototypeOf(d.t) === null)))
					fail('decodeShareLink returned ' + JSON.stringify(d));
				if (bad && cat !== 'notMine' && !d.error) fail('a bad link decoded');

				const warns = [];
				const res = I.resolveInitial({ persist, hash, saved, decoders, location: {}, warn: m => warns.push(m) });

				if (warns.some(w => w.indexOf('E-JS-104: ') !== 0)) fail('a warning without E-JS-104: ' + warns);
				if (warns.filter(w => w.indexOf(': the link ') > 0).length > 1) fail('more than one link warning: ' + warns);
				for (const name of names)
					if (warns.filter(w => w.indexOf("decoder '" + name + "'") >= 0).length > 1) fail('more than one warning for ' + name);
				if (warns.length > 1 + names.filter(x => WARNS.has(x)).length) fail('too many warnings: ' + warns);

				const src = res.source;
				let sourceKey = src;
				if (src === null) sourceKey = 'null';
				else if (src.startsWith('decoder:')) sourceKey = 'decoder';
				sources[sourceKey] = true;
				if (!(src === null || src === 'link' || src === 'saved' || (typeof src === 'string' && names.indexOf(src.slice(8)) >= 0 && src.startsWith('decoder:'))))
					fail('source ' + src);
				if (bad && src === 'link') fail('a bad link won');
				const decoderWon = typeof src === 'string' && src.startsWith('decoder:');
				if (!decoderWon && src !== 'link') {
					if (savedCat === 'valid' && src !== 'saved') fail('valid saved state lost to ' + src);
					if (savedCat !== 'valid' && src !== null) fail('source ' + src + ' with ' + savedCat + ' saved state');
					if (savedCat !== 'valid' && savedCat !== 'none' && !res.deleteSaved) fail('invalid saved state was kept');
				}
				const topics = res.topics || {};
				for (const t of Object.keys(topics)) {
					if (P.topics.indexOf(t) < 0) fail('unlisted topic ' + t);
					if (I.checkPayload(t, topics[t]) === null) fail('restored ' + t + ' fails checkPayload');
					if (!clean(topics[t], 0)) fail('restored ' + t + ' has a prototype or a banned key');
				}
				if (topics['filter:range']) I.BUILTIN.range.validate(topics['filter:range'], range);
				if (topics['filter:reasons']) I.BUILTIN.choice.validate(topics['filter:reasons'], choice);
				if (topics['filter:q']) I.BUILTIN.search.validate(topics['filter:q']);
			} catch (e) {
				fail('threw ' + String(e?.stack || e));
			}
		}
		out.fuzz = { cases: CASES, failures, sources: Object.keys(sources).sort((a, b) => (a < b ? -1 : Number(a > b))), perCategory,
			polluted: ({}).polluted === undefined && Object.prototype.polluted === undefined && vm.runInContext('Object.prototype.polluted', r.w) === undefined }; // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	} catch (e) { out.fuzz = { crash: String(e?.stack || e) }; }

	// 2. Edge cases, by hand: every banned key, depth and cycle rejection, and the size caps.
	try {
		const I = load().I;
		const P = { ns: 'app.dash', version: 2, topics: ['filter:q', 'filter:r'], shareLink: true, decoders: [] };
		const b64 = s => Buffer.from(s, 'utf8').toString('base64url');
		const good = { schemaVersion: 1, viewId: 'd', search: 'x' };
		const resolve = (extra, decoders) => {
			const warns = [];
			const persist = { ...P, ...extra.persist || {} };
			const res = I.resolveInitial({ persist, hash: '', saved: null, decoders: decoders || new Map(), location: {}, warn: m => warns.push(m), ...extra.env || {} });
			return { res, warns };
		};
		const e = out.edges = {};
		const BANNED = ['__proto__', 'constructor', 'prototype'];
		e.linkBanned = BANNED.map(k => I.decodeShareLink('#s=' + b64('{"v":2,"ns":"app.dash","t":{"filter:q":{"a":{"' + k + '":1}}}}'), P).error);
		e.savedBanned = BANNED.map(k => {
			const x = resolve({ env: { saved: JSON.parse('{"v":2,"topics":{"filter:q":{"schemaVersion":1,"viewId":"d","' + k + '":1}}}') } });
			return { source: x.res.source, deleteSaved: x.res.deleteSaved };
		});
		e.decoderBanned = BANNED.map(k => {
			const x = resolve({ persist: { decoders: ['app.d'] } }, new Map([['app.d', () => JSON.parse('{"filter:q":{"schemaVersion":1,"viewId":"d","' + k + '":1}}')]]));
			return { source: x.res.source, warns: x.warns };
		});

		const deep = n => { let o = { schemaVersion: 1, viewId: 'd' }; for (let i = 0; i < n; i++) { o = { a: o }; } return o; };
		const viaDecoder = v => {
			const x = resolve({ persist: { decoders: ['app.d'] } }, new Map([['app.d', () => v]]));
			return { source: x.res.source, warns: x.warns };
		};
		const cyc = { 'filter:q': { schemaVersion: 1, viewId: 'd' } };
		cyc['filter:q'].self = cyc;
		const cycArr = { 'filter:q': { schemaVersion: 1, viewId: 'd', list: [] } };
		cycArr['filter:q'].list.push(cycArr);
		e.cycle = viaDecoder(cyc);
		e.cycleArray = viaDecoder(cycArr);
		e.deepDecoder = viaDecoder({ 'filter:q': deep(70) });
		e.shallowDecoder = viaDecoder({ 'filter:q': { schemaVersion: 1, viewId: 'd', options: deep(40) } });
		e.deepSaved = (() => { const x = resolve({ env: { saved: { v: 2, topics: { 'filter:q': deep(70) } } } }); return { source: x.res.source, deleteSaved: x.res.deleteSaved }; })();
		const nest = n => { let s = '1'; for (let i = 0; i < n; i++) { s = '{"a":' + s + '}'; } return s; };
		e.deepLink = I.decodeShareLink('#s=' + b64('{"v":2,"ns":"app.dash","t":' + nest(70) + '}'), P).error;
		e.shallowLink = Object.keys(I.decodeShareLink('#s=' + b64('{"v":2,"ns":"app.dash","t":{"filter:q":' + nest(40) + '}}'), P));

		// Size caps: 4096 bytes of payload JSON per topic, 8192 characters of fragment.
		const sized = (bytes, ch) => {   // a filter payload whose JSON is exactly `bytes` UTF-8 bytes, ending in ch-sized filler
			const base = Buffer.byteLength(JSON.stringify({ schemaVersion: 1, viewId: 'd', search: '' }), 'utf8');
			let room = bytes - base, s = '';
			const w = Buffer.byteLength(ch, 'utf8');
			while (room >= w) { s += ch; room -= w; }
			s += 'a'.repeat(room);
			const p = { schemaVersion: 1, viewId: 'd', search: s };
			return p;
		};
		const bytes = p => Buffer.byteLength(JSON.stringify(p), 'utf8');
		const keptTopics = (cur) => { const l = I.encodeLink(P, cur); return l === null ? null : Object.keys(I.decodeShareLink(l, P).t).sort((a, b) => (a < b ? -1 : Number(a > b))); };
		const at = sized(4096, '\u00e9'), over = sized(4097, '\u00e9'), chars = { schemaVersion: 1, viewId: 'd', search: '\u00e9'.repeat(2049) };
		const three = sized(4096, '\u20ac'), fourByte = sized(4096, '\ud83d\ude00');
		e.cap = {
			atBytes: bytes(at), overBytes: bytes(over), charsBytes: bytes(chars), charsLength: JSON.stringify(chars).length,
			threeBytes: bytes(three), fourBytes: bytes(fourByte),
			at: keptTopics({ 'filter:q': at }), over: keptTopics({ 'filter:q': over, 'filter:r': good }), chars: keptTopics({ 'filter:q': chars, 'filter:r': good }),
			three: keptTopics({ 'filter:q': three }), four: keptTopics({ 'filter:q': fourByte }),
			atRoundTrip: (() => { const t = I.decodeShareLink(I.encodeLink(P, { 'filter:q': at }), P).t; return t['filter:q'].search === at.search && I.checkPayload('filter:q', t['filter:q']) !== null; })(),
			overCheck: I.checkPayload('filter:q', over) === null && I.checkPayload('filter:q', at) !== null,
			tooBig: I.encodeLink(P, { 'filter:q': at, 'filter:r': sized(4096, 'a') }),
			twoSmall: keptTopics({ 'filter:q': sized(1000, 'a'), 'filter:r': sized(1000, 'a') })
		};
		e.cap.tooBigIsNull = e.cap.tooBig === null;
		delete e.cap.tooBig;
		e.fragmentCap = {
			at: I.decodeShareLink('#s=' + 'A'.repeat(8189), P).error,
			over: I.decodeShareLink('#s=' + 'A'.repeat(8190), P).error
		};
		e.polluted = ({}).polluted === undefined;
	} catch (err) { out.edges = { crash: String(err?.stack || err) }; }

	process.stdout.write(JSON.stringify(out));
