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
 * bus-fake-clock.cjs - shared helpers for the bridge harnesses: a manual clock that replaces
 * JuneauViews.bus.config.timers (setTimeout/clearTimeout/now/random), and settle(), which drains pending promise
 * callbacks so a fake fetch/stream chain runs to its next await.
 */
'use strict';

function fakeClock() {
	let now = 1759312345000;
	let seq = 0;
	const pending = new Map();
	const clock = {
		random: 0.5,
		timers: {
			setTimeout: function (fn, ms) { const id = ++seq; pending.set(id, { at: now + (ms || 0), fn: fn }); return id; },
			clearTimeout: function (id) { pending.delete(id); },
			now: function () { return now; },
			random: function () { return clock.random; }
		},
		/** Timers set and not yet fired or cleared. */
		pending: function () { return pending.size; },
		/** Runs every timer due within `ms`, in due order, then sets the clock to now + ms. */
		advance: function (ms) {
			const end = now + ms;
			for (;;) {
				let nextId = null;
				let next = null;
				for (const [id, p] of pending)
					if (p.at <= end && (!next || p.at < next.at)) { next = p; nextId = id; }
				if (!next) break;
				pending.delete(nextId);
				now = next.at;
				next.fn();
			}
			now = end;
		}
	};
	return clock;
}

async function settle() {
	for (let i = 0; i < 25; i++) await new Promise(function (r) { setImmediate(r); });
}

module.exports = { fakeClock: fakeClock, settle: settle };
