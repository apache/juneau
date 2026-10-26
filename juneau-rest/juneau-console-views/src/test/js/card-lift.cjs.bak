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
 * card-lift.cjs - plain-Node harness for NS.card.liftCatalog over card-lift-corpus.json (shared with CardLift_Parity_Test).
 *
 *   Usage:  node card-lift.cjs <juneau-renders.js> <juneau-views.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node card-lift.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { NS } = loadViews(rendersJsPath, viewsJsPath);
assert.ok(NS.card && typeof NS.card.liftCatalog === 'function', 'NS.card.liftCatalog must be exported');
const corpus = JSON.parse(fs.readFileSync(path.join(__dirname, '../resources/card-lift-corpus.json'), 'utf8'));
let failures = 0;
for (const c of corpus.cases) {
	try {
		if (c.expectedError) {
			assert.throws(() => NS.card.liftCatalog(c.name, c.catalog), e => e.message.includes(c.expectedError), c.name);
		} else {
			// Round-trip through JSON: the sandbox's objects come from another realm, so deepStrictEqual's prototype check would fail.
			assert.deepStrictEqual(JSON.parse(JSON.stringify(NS.card.liftCatalog(c.name, c.catalog))), c.expected, c.name + ': lift mismatch');
		}
		console.log('ok - ' + c.name);
	} catch (e) {
		failures++;
		console.error('not ok - ' + c.name + ': ' + e.message);
	}
}
if (failures) { console.error(failures + ' failure(s)'); process.exit(1); }
console.log('all ' + corpus.cases.length + ' card-lift cases passed');
