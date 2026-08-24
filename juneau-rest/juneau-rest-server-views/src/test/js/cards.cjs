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
 * cards.cjs - always-on Node harness for the juneau-cards.js PURE helpers + fillCardFields:
 * clampPollInterval / formatStalenessAge / scalarFieldValue / isSafeCardEndpoint / envelopeContractOk /
 * nextPollDelay / fillCardFields.
 *
 *   Usage:  node cards.cjs <path-to-juneau-cards.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const cardsJsPath = process.argv[2];
if (!cardsJsPath) {
	console.error('usage: node cards.cjs <juneau-cards.js>');
	process.exit(2);
}

/** Minimal field slot: getAttribute + a textContent that records the last set value (never interprets markup). */
function slot(key) {
	return {
		nodeType: 1,
		attrs: { 'data-juneau-card-field': key },
		_text: 'OLD',
		getAttribute: function (k) { return Object.hasOwn(this.attrs, k) ? this.attrs[k] : null; },
		set textContent(v) { this._text = v == null ? '' : String(v); },
		get textContent() { return this._text; }
	};
}

// document.readyState !== 'loading' would fire initAll immediately; keep it 'loading' so the module only registers a
// listener (never invoked here) and we can exercise the exported pure helpers in isolation.
const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; }
};
const window = { document: document, console: console };
const sandbox = { window: window, document: document, console: console };
vm.runInNewContext(fs.readFileSync(path.resolve(cardsJsPath), 'utf8'), sandbox, { filename: 'juneau-cards.js' });

const NS = window.JuneauCards;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.fillCardFields === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

out.contractVersion = I.JUNEAU_CARDS_CONTRACT_VERSION;
out.nsContractVersion = NS.CONTRACT_VERSION;
out.minPoll = I.MIN_POLL_INTERVAL_MS;

// clampPollInterval: below floor -> floor; at/above -> unchanged.
out.clamp_below = I.clampPollInterval(1000);
out.clamp_at = I.clampPollInterval(5000);
out.clamp_above = I.clampPollInterval(30000);

// formatStalenessAge.
out.age_now = I.formatStalenessAge(500);
out.age_secs = I.formatStalenessAge(5000);
out.age_mins = I.formatStalenessAge(120000);
out.age_hours = I.formatStalenessAge(3600000);

// scalarFieldValue.
out.scalar_str = I.scalarFieldValue('hi');
out.scalar_num = I.scalarFieldValue(7);
out.scalar_bool = I.scalarFieldValue(true);
out.scalar_null = I.scalarFieldValue(null);
out.scalar_obj = I.scalarFieldValue({ x: 1 });
out.scalar_arr = I.scalarFieldValue([1]);

// isSafeCardEndpoint: same-origin AND non-templated (NOT the row-detail rule that permits {id}).
out.ep_pathOk = I.isSafeCardEndpoint('/data/summary');
out.ep_relativeOk = I.isSafeCardEndpoint('data/summary');
out.ep_templated = I.isSafeCardEndpoint('/cards/{id}');
out.ep_templatedAny = I.isSafeCardEndpoint('/x/{anything}');
out.ep_absolute = I.isSafeCardEndpoint('http://evil/x');
out.ep_protoRel = I.isSafeCardEndpoint('//evil/x');
out.ep_scheme = I.isSafeCardEndpoint('servlet:/data/x');
out.ep_js = I.isSafeCardEndpoint('javascript:alert(1)');
out.ep_dotdot = I.isSafeCardEndpoint('/data/../x');
out.ep_leadingDotdot = I.isSafeCardEndpoint('../x');
out.ep_empty = I.isSafeCardEndpoint('');

// envelopeContractOk.
out.env_ok = I.envelopeContractOk({ contractVersion: '1', fields: {} }, '1');
out.env_bad = I.envelopeContractOk({ contractVersion: '2', fields: {} }, '1');
out.env_missing = I.envelopeContractOk({ fields: {} }, '1');
out.env_null = I.envelopeContractOk(null, '1');

// nextPollDelay: interval (clamped) + jitter in [0, POLL_JITTER_MS).
out.delay_zeroJitter = I.nextPollDelay(30000, 0);
const dMax = I.nextPollDelay(30000, 0.999);
out.delay_maxJitterInRange = dMax >= 30000 && dMax < 31000;
out.delay_clampsBelowFloor = I.nextPollDelay(1000, 0);   // -> floor 5000

// fillCardFields: textContent-only fill, unknown keys dropped, missing -> "", xss never interpreted.
const xss = '<img src=x onerror="window.__juneauCardXss=1">';
const name = slot('name');
const status = slot('status');
const extra = slot('extra');
const body = {
	querySelectorAll: function (sel) {
		return sel === '[data-juneau-card-field]' ? [name, status, extra] : [];
	}
};
I.fillCardFields(body, { name: xss, status: 42, unknownKey: 'dropped' });
out.fill_xss = name.textContent;
out.fill_xssNotInterpreted = name.textContent === xss;
out.fill_num = status.textContent;
out.fill_missing = extra.textContent;

process.stdout.write(JSON.stringify(out));
