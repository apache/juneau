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
 * scheme-script.cjs - runs the console's pre-paint color-scheme script over its full precedence matrix.
 *
 *   Usage:  node scheme-script.cjs <script-file>
 *
 * The script file holds the exact inline script text (the Java test writes ConsoleChromeMixin.SCHEME_SCRIPT, or a
 * deliberately broken copy of it).  Each row runs that text in a fresh VM context holding only a minimal document
 * shim (the <html> element's attributes), a localStorage stand-in and a matchMedia stand-in, then reads back the
 * data-jc-scheme attribute the script set.
 *
 * Matrix rows are keyed "toggle=<no|yes> stored=<none|light|dark|garbage> pin=<none|light|dark> os=<light|dark>".
 * Failure rows (keyed by a name) cover a localStorage that throws on access and a missing matchMedia.
 *
 * Prints {"rows": {key: scheme}, "failures": {name: scheme}}.
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const KEY = 'juneau-console:scheme';

/** The <html> element: attribute access only, which is all the script touches. */
function htmlElement(attrs) {
	const a = { ...attrs };
	return {
		hasAttribute: n => Object.hasOwn(a, n),
		getAttribute: n => (Object.hasOwn(a, n) ? a[n] : null),
		setAttribute: (n, v) => { a[n] = String(v); },
		attrs: a
	};
}

/**
 * Runs the script once.  opts: {toggle, stored, pin, os, storageThrows, noMatchMedia}.  Returns the resolved
 * data-jc-scheme value, or null when the script set none.
 */
function run(src, opts) {
	const attrs = {};
	if (opts.toggle) attrs['data-jc-scheme-toggle'] = '';
	if (opts.pin) {
		attrs['data-jc-scheme'] = opts.pin;
		attrs['data-jc-scheme-pin'] = opts.pin;
	}
	const html = htmlElement(attrs);
	const store = {};
	if (opts.stored) store[KEY] = opts.stored;
	const sandbox = { document: { documentElement: html } };
	if (opts.storageThrows)
		Object.defineProperty(sandbox, 'localStorage', { get() { throw new Error('SecurityError: storage is disabled'); } });
	else
		sandbox.localStorage = { getItem: k => (Object.hasOwn(store, k) ? store[k] : null) };
	if (!opts.noMatchMedia)
		sandbox.matchMedia = q => ({ matches: q === '(prefers-color-scheme: dark)' && opts.os === 'dark', media: q });
	vm.runInNewContext(src, sandbox, { timeout: 1000 }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
	return html.getAttribute('data-jc-scheme'); // NOSONAR javascript:S7761 -- DOM fake has no dataset
}

const [scriptFile] = process.argv.slice(2);
if (!scriptFile) {
	process.stderr.write('usage: node scheme-script.cjs <script-file>\n');
	process.exit(2);
}
const src = fs.readFileSync(scriptFile, 'utf8');

const storedValue = stored => {
	if (stored === 'none') return null;
	return stored === 'garbage' ? 'blue' : stored;
};

const rows = {};
for (const toggle of ['no', 'yes'])
	for (const stored of ['none', 'light', 'dark', 'garbage'])
		for (const pin of ['none', 'light', 'dark'])
			for (const os of ['light', 'dark'])
				rows['toggle=' + toggle + ' stored=' + stored + ' pin=' + pin + ' os=' + os] = run(src, {
					toggle: toggle === 'yes',
					stored: storedValue(stored),
					pin: pin === 'none' ? null : pin,
					os: os
				});

const failures = {
	// Storage throws on a toggle page with a pin: the throw is swallowed and the pin still wins.
	storageThrowsFallsToPin: run(src, { toggle: true, stored: 'light', pin: 'dark', os: 'light', storageThrows: true }),
	// Storage throws, unpinned: falls through to the OS.
	storageThrowsFallsToOs: run(src, { toggle: true, stored: 'light', pin: null, os: 'dark', storageThrows: true }),
	// No matchMedia at all, unpinned and nothing stored: light.
	matchMediaMissing: run(src, { toggle: false, stored: null, pin: null, os: 'dark', noMatchMedia: true }),
	// No matchMedia, but a pin: the pin is used before the OS is ever consulted.
	matchMediaMissingPinned: run(src, { toggle: false, stored: null, pin: 'dark', os: 'light', noMatchMedia: true })
};

process.stdout.write(JSON.stringify({ rows, failures }, null, 2) + '\n');
