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
 * datatables-error.cjs - Node behavioral check of juneau-datatables.js error handling for HTTP 400 +
 * X-BeanQuery-Error.  Usage: node datatables-error.cjs <juneau-datatables.js>.  Prints ONE JSON object; every
 * assertion lives in DataTablesClientErrors_Test.
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const alerts = [];
const events = [];
const bound = {};
const table = {};
const $ = function (node) {
	return {
		off: () => ({ on: (name, fn) => { bound[name] = fn; } }),
		trigger: (name, args) => { events.push({ name, msg: args[2] }); }
	};
};
$.fn = { dataTable: { ext: { errMode: 'alert' } }, DataTable: undefined };

const win = { jQuery: $, alert: m => alerts.push(m) };
win.window = win;
const ctx = vm.createContext({ window: win, document: { readyState: 'complete' }, JSON, Object, Error });
vm.runInContext(fs.readFileSync(process.argv[2], 'utf8'), ctx);

const settings = { nTable: table, sTableId: 'rel' };
const xhr = (status, header, body) => ({
	status, responseText: body,
	getResponseHeader: n => (n === 'X-BeanQuery-Error' ? header : null)
});
const fire = (x, json) => bound['xhr.dt.juneauErr']({}, settings, json === undefined ? null : json, x);

const out = {};
const opts = win.JuneauDataTables.ajax('/q');
opts.data({ draw: 1 }, settings);
out.bound = typeof bound['xhr.dt.juneauErr'] === 'function';
out.bodyString = JSON.parse(opts.data({ draw: 2 }, settings)).draw;

out.handled = fire(xhr(400, 'UNKNOWN_COLUMN', JSON.stringify({ message: "Unknown column 'x'.", status: 400 })));
out.alert1 = alerts[0];
out.event1 = events[0]?.name;

out.handledPlain = fire(xhr(400, 'BAD_OPERATOR', "Bad op."));
out.alert2 = alerts[1];

out.handledEmpty = fire(xhr(400, 'TOO_MANY_CLAUSES', ''));
out.alert3 = alerts[2];

out.ignoredNoHeader = fire(xhr(400, null, 'x')) === undefined;
out.ignored500 = fire(xhr(500, null, 'x')) === undefined;
out.ignoredSuccess = fire(xhr(200, 'UNKNOWN_COLUMN', 'x'), { data: [] }) === undefined;
out.alertCount = alerts.length;

$.fn.dataTable.ext.errMode = 'none';
fire(xhr(400, 'UNKNOWN_COLUMN', 'quiet'));
out.noneAlerts = alerts.length;
const seen = [];
$.fn.dataTable.ext.errMode = (s, tn, m) => seen.push(m);
fire(xhr(400, 'UNKNOWN_COLUMN', 'fn'));
out.fnMode = seen[0];

out.helperMessage = win.JuneauDataTables.errorMessage(xhr(400, 'C', '<html>err</html>'), 'C');
console.log(JSON.stringify(out));
