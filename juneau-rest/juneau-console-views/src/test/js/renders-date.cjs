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
 * renders-date.cjs - Node harness for the `date` renderer's time-zone behavior.
 *
 *   Usage:  node renders-date.cjs <path-to-juneau-renders.js>      (TZ comes from the environment)
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const rendersJsPath = process.argv[2];
if (!rendersJsPath) {
	console.error('usage: node renders-date.cjs <juneau-renders.js>');
	process.exit(2);
}

const sandbox = { console: console, Intl: Intl, Date: Date, Number: Number, String: String, Object: Object, Array: Array,
	parseInt: Number.parseInt, JSON: JSON };
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
// NOSONAR javascript:S1523 -- test harness loading the real juneau-renders.js into an isolated vm sandbox.
vm.runInNewContext(fs.readFileSync(rendersJsPath, 'utf8'), sandbox); // NOSONAR javascript:S1523

const date = sandbox.window.JuneauViews.resolveRenderer('date');

process.stdout.write(JSON.stringify({
	tz: Intl.DateTimeFormat().resolvedOptions().timeZone,
	dateOnly: date.display('2031-03-14'),
	dateOnlyPadded: date.display('  2031-03-14 '),
	dateTime: date.display('2031-03-14T03:00:00Z'),
	dateObject: date.display(new Date('2031-03-14T03:00:00Z')),
	epoch: date.display(Date.parse('2031-03-14T03:00:00Z'))
}));
