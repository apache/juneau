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
 * bus-frames.cjs - Node harness for the frame codec (spec §11.4). Runs every case of the shared
 * bus-frames-corpus.json (also read by the Java BusFrames_Corpus_Test, Task 14) through JuneauViews.bus.frames
 * and reports what the JS side actually produced, in corpus order. It also runs two JS-only groups the shared
 * corpus does not hold: the maxFrameBytes boundary and the SseParser cases. ViewsJs_BusFrames_Test compares the
 * report with the corpus and the expected values; the harness itself asserts nothing.
 *
 *   Usage:  node bus-frames.cjs <juneau-bus.js> [corpus.json]
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const busJs = process.argv[2];
if (!busJs) {
	console.error('usage: node bus-frames.cjs <juneau-bus.js> [corpus.json]');
	process.exit(2);
}
const corpusPath = process.argv[3]
	|| path.resolve(__dirname, '../../../../juneau-rest-server-bus/src/test/resources/bus-frames-corpus.json');
const corpus = JSON.parse(fs.readFileSync(corpusPath, 'utf8'));

const { NS } = loadScripts([busJs]);
const F = NS.bus.frames;

function attempt(fn) {
	try { return { ok: fn() }; }
	catch (e) { return { code: e.code || null, reason: e.detail ? e.detail.reason : null, message: e.message }; }
}

/** A downstream pub whose encoding is exactly `bytes` UTF-8 bytes long. */
function paddedPub(bytes) {
	const frame = { v: 1, type: 'pub', topic: 'app.big', payload: '', retained: false, seq: 1 };
	frame.payload = 'x'.repeat(bytes - F.utf8Length(F.encode(frame)));
	return frame;
}

/** JS-only SseParser cases: the chunk splits a real network read can produce. */
const SSE_CASES = [
	['event: bus\nid: 1\nda', 'ta: {"v":1,"type":"ping"}\n', '\n'],
	[': heartbeat\r\n\r\nevent: bus\r', '\ndata: {"v":1,"type":"ping"}\r\n\r\n'],
	['event: bus\ndata: {"v":1,\ndata: "type":"ping"}\n\n'],
	['data:x\n\n'],
	['event: bus\nid: 7\ndata: a\n\nevent: bus\ndata: b\n\n'],
	['event: bus\nid: 3\n\nretry: 10\n\n']
];

/** Oversized-event cases: one long line, a long line split over chunks, and many short lines that add up. */
const BIG = 'x'.repeat(70000);
const SSE_CAP_CASES = [
	['event: bus\nid: 4\ndata: ' + BIG + '\n\ndata: ok\n\n'],
	['data: ' + 'x'.repeat(40000), 'x'.repeat(40000), '\n\ndata: ok\n\n'],
	[Array.from({ length: 70 }, function () { return 'data: ' + 'x'.repeat(1000) + '\n'; }).join('') + '\ndata: ok\n\n']
];

const atLimit = paddedPub(F.MAX_FRAME_BYTES);
const overLimit = paddedPub(F.MAX_FRAME_BYTES + 1);
const atLimitText = attempt(function () { return F.encode(atLimit); });

const out = {
	version: corpus.version,
	maxFrameBytes: F.MAX_FRAME_BYTES,
	encode: corpus.encode.map(function (c) { return attempt(function () { return F.encode(c.frame); }); }),
	decode: corpus.decode.map(function (c) { return attempt(function () { return F.decode(c.text); }); }),
	roundTrip: corpus.encode.map(function (c) {
		return attempt(function () { return F.encode(F.decode(c.text)) === c.text; });
	}),
	bad: corpus.bad.map(function (c) { return attempt(function () { return F.decode(c.text); }); }),
	tooLarge: {
		atLimitBytes: atLimitText.ok ? F.utf8Length(atLimitText.ok) : null,
		encodeAtLimit: atLimitText.ok ? 'ok' : atLimitText.reason,
		decodeAtLimit: atLimitText.ok ? (attempt(function () { return F.decode(atLimitText.ok); }).ok ? 'ok' : 'bad') : null,
		encodeOverLimit: attempt(function () { return F.encode(overLimit); }).reason,
		decodeOverLimit: attempt(function () { return F.decode(JSON.stringify(overLimit)); }).reason,
		utf8: [F.utf8Length('a'), F.utf8Length('é'), F.utf8Length('✓'), F.utf8Length('🚀'), F.utf8Length('\ud800x')]
	},
	sseEndlessLine: (function () {
		const p = new F.SseParser();
		for (let i = 0; i < 10; i++) p.push('x'.repeat(40000));
		return { buffered: p.buf.length, after: p.push('\n\ndata: ok\n\n') };
	}()),
	sseCap: SSE_CAP_CASES.map(function (chunks) {
		const p = new F.SseParser();
		let events = [];
		chunks.forEach(function (ch) { events = events.concat(p.push(ch)); });
		return events.map(function (e) { return { event: e.event, id: e.id, tooLarge: !!e.tooLarge, data: e.data.length > 100 ? e.data.length : e.data }; });
	}),
	sse: SSE_CASES.map(function (chunks) {
		const p = new F.SseParser();
		let events = [];
		chunks.forEach(function (ch) { events = events.concat(p.push(ch)); });
		return events;
	})
};

process.stdout.write(JSON.stringify(out));
