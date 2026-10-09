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
 * bus-wiring.cjs - Node harness for JuneauViews.bus.wiring.validate (message bus addendum, spec 6.1, 6.3, 11.2).
 * Runs every case of the shared R-10 / R-11 corpus (bus-wiring-corpus.json, which Java's BusWiring_Corpus_Test
 * also runs) and prints ONE JSON report; every assertion lives in ViewsJs_BusWiring_Test.
 *
 *   Usage:  node bus-wiring.cjs <juneau-bus.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { makeEnv, loadScripts } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const busJsPath = process.argv[2];
if (!busJsPath) {
	console.error('usage: node bus-wiring.cjs <juneau-bus.js>');
	process.exit(2);
}
const corpus = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../resources/bus-wiring-corpus.json'), 'utf8'));

// validate reports by return value only; anything it logs is a finding.
const consoleErrors = [];
console.error = function () { consoleErrors.push(Array.prototype.map.call(arguments, String).join(' ')); };

const bus = loadScripts([busJsPath], makeEnv()).NS.bus;
const validate = bus.wiring && bus.wiring.validate;

// The card-type table the shell builds (cardTypeTable()), with the stock types stubbed as Task 6 defines them:
// html accepts no roles beyond the shell's; datatables accepts 'filter' and handles the seven table ops.
// datatablesImplicit is NS.tableBus.implicitTopics (Task 6) and DatatablesCardType.implicitTopics (Task 4):
// the view is card.table when it is an object, else the card; no dataUrl means src-only, so all five topics.
const TABLE_OPS = ['reload', 'clear-selection', 'select', 'set-filter', 'pause-polling', 'resume-polling', 'collapse-all'];
function datatablesImplicit(card) {
	const v = card.table && typeof card.table === 'object' ? card.table : card;
	const all = v.dataUrl == null;
	const out = [];
	if (all || v.selection != null) out.push('selection:' + card.id);
	out.push('filter:' + card.id, 'redraw:' + card.id);
	if (all || v.detail != null) out.push('detail:' + card.id);
	if (all || v.bulk != null) out.push('bulk:' + card.id);
	return out;
}
const types = {
	html: { roles: [], ops: [], implicit: function () { return []; } },
	datatables: { roles: ['filter'], ops: TABLE_OPS, implicit: datatablesImplicit }
};
Object.keys(corpus.cardTypes).forEach(function (name) {
	const s = corpus.cardTypes[name];
	types[name] = { roles: s.roles, ops: s.ops, implicit: function () { return s.publishes; } };
});

const out = { hasValidate: typeof validate === 'function', cases: [] };
if (out.hasValidate) {
	out.emptyContract = validate({ contractVersion: '1', title: 'T', nav: [], activeNav: [], cards: [] }, types);
	corpus.cases.forEach(function (c) {
		const row = { name: c.name, expectJs: c.expectJs, got: null, threw: null };
		try {
			row.got = validate(c.contract, types);
		} catch (e) {
			row.threw = String(e && e.stack || e);
		}
		out.cases.push(row);
	});
}
out.consoleErrors = consoleErrors;
process.stdout.write(JSON.stringify(out));
