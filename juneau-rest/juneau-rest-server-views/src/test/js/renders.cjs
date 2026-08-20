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
 * renders.cjs - always-on Node harness for juneau-renders.js timestamp formatters (local + California).
 *
 *   Usage:  node renders.cjs <path-to-juneau-renders.js>
 *
 * The Java test sets TZ=America/New_York so local-time assertions are deterministic.  Prints ONE JSON
 * object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const vm = require('node:vm');

const rendersJsPath = process.argv[2];
if (!rendersJsPath) {
	console.error('usage: node renders.cjs <juneau-renders.js>');
	process.exit(2);
}

const code = fs.readFileSync(rendersJsPath, 'utf8');
const sandbox = {
	window: {},
	console: console,
	Intl: Intl,
	Date: Date,
	Number: Number,
	String: String,
	Object: Object,
	Array: Array,
	parseInt: parseInt,
	JSON: JSON
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
vm.runInNewContext(code, sandbox);

const NS = sandbox.window.JuneauViews;
const R = NS._render;
const ts = NS.resolveRenderer('ts-zulu');
const datetime = NS.resolveRenderer('datetime');

const INSTANT = '2026-08-20T20:11:00Z';
const d = new Date(INSTANT);
const lines = R.popupLines(d);
const htmlOn = String(ts.display(INSTANT, {}, {}));
const htmlOff = String(ts.display(INSTANT, {}, { popup: 'off' }));
const dtDefault = String(datetime.display(INSTANT, {}, {}));
const dtPopup = String(datetime.display(INSTANT, {}, { popup: 'on' }));

const out = {
	utc: R.formatUtcZulu(d),
	local: R.formatLocalTime(d),
	california: R.formatCalifornia(d),
	popupLocal: lines.local,
	popupCalifornia: lines.california,
	janCalifornia: R.formatCalifornia(new Date('2026-01-15T20:11:00Z')),
	rolloverLocal: R.formatLocalTime(new Date('2026-08-20T03:11:00Z')),
	displayHasSpan: htmlOn.indexOf('data-juneau-ts') >= 0,
	displayIsoAttr: htmlOn.indexOf('2026-08-20T20:11:00.000Z') >= 0,
	displayOffIsPlain: htmlOff.indexOf('<') < 0,
	displayOffUtc: htmlOff,
	datetimeDefaultHasSpan: dtDefault.indexOf('data-juneau-ts') >= 0,
	datetimePopupHasSpan: dtPopup.indexOf('data-juneau-ts') >= 0,
	xss: String(ts.display('<img src=x onerror=alert(1)>', {}, {})),
	blank: String(ts.display(null, {}, {}))
};

const progress = NS.resolveRenderer('progress');
const WIDTH_RE = /style="width:(0|[1-9]\d?|100)%"/;

function phtml(cell, meta) {
	return String(progress.display(cell, {}, meta || {}));
}
function emptyTrack(html) {
	return html.indexOf('jc-progress-bar') < 0
		&& html.indexOf('style=') < 0
		&& html.indexOf('jc-progress-label') < 0
		&& html.indexOf('is-ok') < 0
		&& html.indexOf('is-warn') < 0
		&& html.indexOf('is-exceeds') < 0;
}
function widthOf(html) {
	const m = WIDTH_RE.exec(html);
	return m ? m[1] : null;
}
function hasState(html, state) {
	return html.indexOf('jc-progress-bar ' + state) >= 0 || html.indexOf('jc-progress-bar is-' + state.replace('is-', '')) >= 0;
}

out.progress_null = emptyTrack(phtml(null, {}));
out.progress_empty = emptyTrack(phtml('', {}));
out.progress_ws = emptyTrack(phtml('   ', {}));
out.progress_arr = emptyTrack(phtml([], {}));
out.progress_obj = emptyTrack(phtml({}, {}));
out.progress_true = emptyTrack(phtml(true, {}));
out.progress_false = emptyTrack(phtml(false, {}));
out.progress_inf = emptyTrack(phtml(Infinity, {}));
out.progress_ninf = emptyTrack(phtml(-Infinity, {}));
out.progress_foo = emptyTrack(phtml('foo', {}));
out.progress_badMax = emptyTrack(phtml(50, { max: '0' }));
out.progress_emptyMax = emptyTrack(phtml(50, { max: '' }));
out.progress_zeroWidth = widthOf(phtml(0, {}));
out.progress_zeroLabel = phtml(0, {}).indexOf('>0%</span>') >= 0;
out.progress_strZeroWidth = widthOf(phtml('0', {}));
out.progress_mid = widthOf(phtml(50, { max: '100' }));
out.progress_eqMax = widthOf(phtml(100, { max: '100' }));
out.progress_eqMaxOk = phtml(100, { max: '100' }).indexOf('is-ok') >= 0;
out.progress_overWidth = widthOf(phtml(130, { max: '100' }));
out.progress_overLabel = phtml(130, { max: '100' }).indexOf('>130%</span>') >= 0;
out.progress_overExceeds = phtml(130, { max: '100' }).indexOf('is-exceeds') >= 0;
out.progress_negWidth = widthOf(phtml(-10, { max: '100' }));
out.progress_negLabel = phtml(-10, { max: '100' }).indexOf('>-10%</span>') >= 0;
out.progress_round = widthOf(phtml(2, { max: '3' })); // 67
out.progress_roundLabel = phtml(2, { max: '3' }).indexOf('>67%</span>') >= 0;
out.progress_warnEq = phtml(80, { max: '100', warn: '80' }).indexOf('is-warn') >= 0;
out.progress_exceedsEq = phtml(90, { max: '100', exceeds: '90' }).indexOf('is-exceeds') >= 0;
out.progress_warnEqExceeds = phtml(80, { max: '100', warn: '80', exceeds: '80' }).indexOf('is-exceeds') >= 0;
out.progress_exceedsBelowWarn = phtml(60, { max: '100', warn: '80', exceeds: '50' }).indexOf('is-exceeds') >= 0;
out.progress_units = phtml(80, { max: '200', warn: '80' }).indexOf('is-warn') >= 0;
out.progress_labelNone = phtml(50, { label: 'none' }).indexOf('jc-progress-label') < 0;
out.progress_labelValue = phtml(50, { label: 'value' }).indexOf('>50</span>') >= 0;
out.progress_labelBogus = phtml(50, { label: 'nope' }).indexOf('>50%</span>') >= 0;
out.progress_field = phtml(50, { field: 'cpu' });
out.progress_fieldHasProgress = out.progress_field.indexOf(' progress ') >= 0 || out.progress_field.indexOf('class="jc-progress progress cpu"') >= 0;
out.progress_fieldHasCpu = out.progress_field.indexOf('cpu') >= 0;
out.progress_noField = phtml(50, {}).indexOf(' progress') < 0;
out.progress_columnIgnored = phtml(50, { column: 'cpu' }).indexOf('cpu') < 0;
out.progress_sort = progress.sort(42);
out.progress_hostileCell = phtml('<img src=x onerror=alert(1)>', {});
out.progress_hostileMax = phtml(50, { max: '100" onmouseover=alert(1)' });
out.progress_hostileField = phtml(50, { field: '"><b>' });
out.progress_hostileWarn = phtml(50, { warn: '<script>' });
out.progress_script = String(out.progress_hostileCell + out.progress_hostileMax + out.progress_hostileField + out.progress_hostileWarn).toLowerCase().indexOf('<script') >= 0;
out.progress_onerror = String(out.progress_hostileCell).indexOf('onerror=') >= 0;
out.progress_onmouse = String(out.progress_hostileMax).indexOf('onmouseover=') >= 0;
out.progress_hostileEmpty = emptyTrack(out.progress_hostileCell);
out.progress_widthCanary = WIDTH_RE.test(phtml(50, {})) && phtml(50, {}).indexOf('style="width:50%"') >= 0;

const builtinTag = NS.resolveSinkRenderer('tag');
NS.registerRenderer('tag', { display: function () { return '<img src=x onerror=alert(1)>'; } });
out.freeze_cellHonorsOverride = String(NS.resolveRenderer('tag').display()).indexOf('<img') >= 0;
out.freeze_sinkStillBuiltin = builtinTag === NS.resolveSinkRenderer('tag');
out.freeze_sinkDisplaySafe = String(NS.resolveSinkRenderer('tag').display('Released', {}, { field: 'status' })).indexOf('class="tag') >= 0;
out.freeze_ids = (NS._render.frozenBuiltinIds || []).slice().sort().join(',');

console.log(JSON.stringify(out));
