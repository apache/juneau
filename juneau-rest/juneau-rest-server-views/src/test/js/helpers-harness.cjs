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
 * helpers-harness.cjs - the shared loader for the always-on Node behavioral harness that exercises
 * juneau-helpers.js (WORK-J0522b).
 *
 * Deliberately loads only juneau-renders.js + juneau-views.js + juneau-helpers.js - NO juneau-regions.js.
 * Design §9.2 requires every helper to be testable with no region at all, so a harness that pulled in the
 * region runtime would be testing a stronger environment than the invariant promises.
 *
 *   const { load, flush } = require('./helpers-harness.cjs');
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const { makeEnv } = require(path.join(__dirname, 'views-dom-shim.cjs'));

/** Loads juneau-renders.js, juneau-views.js and juneau-helpers.js into one fresh environment. */
function load(rendersJsPath, viewsJsPath, helpersJsPath) {
	const env = makeEnv();
	const sandbox = {
		window: env.window,
		document: env.document,
		console: console,
		setTimeout: function (fn) { if (typeof fn === 'function') fn(); return 0; },
		clearTimeout: function () {},
		setInterval: function () { return 0; },
		clearInterval: function () {},
		Promise: Promise,
		Math: Math,
		AbortController: AbortController,
		AbortSignal: AbortSignal,
		DOMException: DOMException,
		fetch: function (...args) { return env.callFetch(...args); }
	};

	for (const file of [rendersJsPath, viewsJsPath, helpersJsPath]) {
		// NOSONAR javascript:S1523 -- loading the production juneau-renders.js/juneau-views.js/juneau-helpers.js
		// sources into a VM sandbox is this harness's intended mechanism for exercising them under the DOM shim;
		// inputs are fixed local file paths supplied by the test, never attacker-controlled data.
		vm.runInNewContext(fs.readFileSync(path.resolve(file), 'utf8'), sandbox, { filename: path.basename(file) });
	}

	const NS = env.window.JuneauViews;
	return { env: env, NS: NS, I: NS?.init, H: NS?.helpers };
}

/** Drains the real microtask queue `n` times, so a chain of promise continuations has all run. */
async function flush(n) {
	for (let i = 0; i < (n || 8); i++) await Promise.resolve();
}

module.exports = { load: load, flush: flush };
