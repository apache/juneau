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
	parseInt: Number.parseInt,
	JSON: JSON
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
// NOSONAR javascript:S1523 -- this is the test harness deliberately loading the real
// juneau-renders.js under test into an isolated vm sandbox; there is no untrusted input.
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

const pill = NS.resolveRenderer('pill');
function pillHtml(cell, meta) {
	return String(pill.display(cell, {}, meta || {}));
}
// Display-only chip: dot (no tone class) + raw value, .tag.<field>.<value> theming, data-juneau-pill.
out.pill_display = pillHtml('ok', { field: 'state' });
out.pill_class = String(pill['class']());
out.pill_dotOff = pillHtml('ok', { field: 'state', dot: 'off' });
// Tone class for the four coloured tones of the closed palette; neutral/absent/off-palette -> no tone class.
out.pill_tones = (NS._render.pillTones || []).slice().sort().join(',');
out.pill_toneInfo = pillHtml('open', { field: 'state', tone: 'info' }).indexOf('jc-pill-dot is-info') >= 0;
out.pill_toneSuccess = pillHtml('open', { field: 'state', tone: 'success' }).indexOf('jc-pill-dot is-success') >= 0;
out.pill_toneWarning = pillHtml('open', { field: 'state', tone: 'warning' }).indexOf('jc-pill-dot is-warning') >= 0;
out.pill_toneError = pillHtml('open', { field: 'state', tone: 'error' }).indexOf('jc-pill-dot is-error') >= 0;
out.pill_toneNeutralNoClass = pillHtml('open', { field: 'state', tone: 'neutral' }).indexOf('is-') < 0;
out.pill_toneAbsentNoClass = pillHtml('open', { field: 'state' }).indexOf('is-') < 0;
// The retired v1 tone names are off-palette now and must emit no modifier at all.
out.pill_toneV1NoClass = ['ok', 'warn', 'exceeds', 'accent', 'danger', 'INFO', 'Success'].every(function (t) {
	return pillHtml('open', { field: 'state', tone: t }).indexOf('is-') < 0;
});
// Action-bound variant adds role/tabindex/data-juneau-action ONLY when meta.action is present.
out.pill_action = pillHtml('open', { field: 'state', action: 'ack' });
out.pill_actionHasRole = out.pill_action.indexOf('role="button"') >= 0;
out.pill_actionHasTabindex = out.pill_action.indexOf('tabindex="0"') >= 0;
out.pill_actionHasId = out.pill_action.indexOf('data-juneau-action="ack"') >= 0;
out.pill_noActionNoRole = out.pill_display.indexOf('role=') < 0 && out.pill_display.indexOf('data-juneau-action') < 0;
// No selection/toggle protocol ever (B5/N1-fold).
out.pill_noSelect = (out.pill_display + out.pill_action).indexOf('aria-pressed') < 0
	&& (out.pill_display + out.pill_action).indexOf('data-juneau-pill-select') < 0;
out.pill_blank = pillHtml(null, { field: 'state' });
out.pill_empty = pillHtml('', { field: 'state' });
// Hostile label/field/action are escaped (display facet returns an escaped string, no innerHTML).
out.pill_hostileLabel = pillHtml('<img src=x onerror=alert(1)>', { field: 'state' });
out.pill_hostileField = pillHtml('open', { field: '"><b>' });
out.pill_hostileAction = pillHtml('open', { field: 'state', action: '"><script>alert(1)</script>' });
out.pill_hostileScript = String(out.pill_hostileLabel + out.pill_hostileField + out.pill_hostileAction)
	.toLowerCase().indexOf('<script') >= 0;
// The hostile label is escaped to text (&lt;img ...&gt;), so no raw <img tag survives into the markup.
out.pill_hostileEscaped = String(out.pill_hostileLabel).indexOf('&lt;img') >= 0
	&& String(out.pill_hostileLabel).indexOf('<img') < 0;
// pill IS a fill-sink built-in, but its sink renderer is a distinct DISPLAY-ONLY variant of the cell renderer.
const sinkPill = NS.resolveSinkRenderer('pill');
function sinkPillHtml(cell, meta) {
	return String(sinkPill.display(cell, {}, meta || {}));
}
out.sinkPill_resolves = sinkPill != null;
out.sinkPill_inFrozenIds = (NS._render.frozenBuiltinIds || []).indexOf('pill') >= 0;
out.sinkPill_class = String(sinkPill['class']());
out.sinkPill_display = sinkPillHtml('ok', { field: 'state' });
out.sinkPill_sameAsCellWhenDisplayOnly = out.sinkPill_display === out.pill_display;
out.sinkPill_keepsTone = sinkPillHtml('open', { field: 'state', tone: 'warning' }).indexOf('is-warning') >= 0;
// The action affordance can never appear on a sink pill, even if an author smuggles meta.action past the server.
out.sinkPill_actionSmuggled = sinkPillHtml('open', { field: 'state', action: 'ack' });
out.sinkPill_noRole = out.sinkPill_actionSmuggled.indexOf('role=') < 0;
out.sinkPill_noTabindex = out.sinkPill_actionSmuggled.indexOf('tabindex') < 0;
out.sinkPill_noActionAttr = out.sinkPill_actionSmuggled.indexOf('data-juneau-action') < 0;
out.sinkPill_noHandlerAttrs = /\son[a-z]+=/.test(out.sinkPill_actionSmuggled) === false;
// registerRenderer cannot swap the sink variant out (frozen), and cannot bleed the cell action branch onto it.
NS.registerRenderer('pill', { display: function () { return '<b onclick="x()">pwned</b>'; } });
out.sinkPill_frozenAgainstOverride = NS.resolveSinkRenderer('pill') === sinkPill
	&& sinkPillHtml('open', { field: 'state', action: 'ack' }).indexOf('role=') < 0;

const builtinTag = NS.resolveSinkRenderer('tag');
NS.registerRenderer('tag', { display: function () { return '<img src=x onerror=alert(1)>'; } });
out.freeze_cellHonorsOverride = String(NS.resolveRenderer('tag').display()).indexOf('<img') >= 0;
out.freeze_sinkStillBuiltin = builtinTag === NS.resolveSinkRenderer('tag');
out.freeze_sinkDisplaySafe = String(NS.resolveSinkRenderer('tag').display('Released', {}, { field: 'status' })).indexOf('class="tag') >= 0;
out.freeze_ids = (NS._render.frozenBuiltinIds || []).slice().sort().join(',');

// WORK-J0508 (Foundry WORK-P0063 row-detail-subtabs follow-up): `code` renderer - HTML-escaped, whitespace-
// preserving, monospace via `.juneau-code`; a frozen fill-sink built-in like `json`/`tag`/`pill` above.
out.code_escapesHtml = NS.resolveRenderer('code').display('<script>alert(1)</script>');
out.code_preservesWhitespaceAndNewlines = NS.resolveRenderer('code').display('line1\n  line2\tindented');
out.code_nullIsEmpty = NS.resolveRenderer('code').display(null);
out.code_sinkRendererExists = NS.resolveSinkRenderer('code') != null;
out.code_sinkMatchesCellOutput = NS.resolveSinkRenderer('code').display('<b>x</b>') === NS.resolveRenderer('code').display('<b>x</b>');

// normalizeTagToken: the emitted token shape, plus a javascript:S5852 linearity guard.  The old trim
// (`.replace(/^-+|-+$/g, "")`) retried its `-+$` alternative at every offset inside a dash run, which is
// quadratic once the run is bracketed by non-dashes; an all-dash string stayed linear, which is exactly why
// the shape went unnoticed.  "a" + 160,000 dashes + "a" took ~8.8s before the fix and ~0.1ms after.
const tagToken = NS._render.normalizeTagToken;
out.tagToken_plain = tagToken('Released');
out.tagToken_spaceRun = tagToken('In Progress');
out.tagToken_edgeDashesTrimmed = tagToken('---in---progress---');
out.tagToken_allDashes = tagToken('-----');
out.tagToken_empty = tagToken('');
out.tagToken_punctCollapses = tagToken('  Ready?  ');
out.tagToken_keepsInnerSeparators = tagToken('a-b_c9');
const dashRun = 'a' + '-'.repeat(160000) + 'a';
const dashStart = Date.now();
const dashToken = tagToken(dashRun);
out.tagToken_adversarialMs = Date.now() - dashStart;
out.tagToken_adversarialUnchanged = dashToken === dashRun;

console.log(JSON.stringify(out));
