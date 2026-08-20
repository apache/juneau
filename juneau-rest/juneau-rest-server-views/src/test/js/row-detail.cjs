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
 * row-detail.cjs - always-on Node harness for the juneau-views.js row-detail helpers:
 * isSafeDetailUrl / substituteDetailUrl / scalarFieldValue / fillDetailSlots / coalesce /
 * contract-ok / drop-if-orphaned / hideActionRefs.
 *
 *   Usage:  node row-detail.cjs <path-to-juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const viewsJsPath = process.argv[2];
if (!viewsJsPath) {
	console.error('usage: node row-detail.cjs <juneau-views.js>');
	process.exit(2);
}

const document = {
	readyState: 'loading',
	addEventListener: function () {},
	querySelectorAll: function () { return []; },
	querySelector: function () { return null; },
	getElementById: function () { return null; },
	createElement: function () {
		return {
			setAttribute: function () {},
			appendChild: function () {},
			querySelector: function () { return null; },
			querySelectorAll: function () { return []; }
		};
	},
	body: { appendChild: function () {}, querySelectorAll: function () { return []; } }
};
const window = { document: document, console: console, jQuery: undefined };
const sandbox = { window: window, document: document, console: console };
vm.runInNewContext(fs.readFileSync(path.resolve(viewsJsPath), 'utf8'), sandbox, { filename: 'juneau-views.js' });

const NS = window.JuneauViews;
const I = NS && NS.init;
const out = { hasInit: !!(I && typeof I.fillDetailSlots === 'function') };
if (!out.hasInit) {
	process.stdout.write(JSON.stringify(out));
	process.exit(0);
}

out.contractVersion = I.JUNEAU_ROW_DETAIL_CONTRACT_VERSION;

out.url_pathOk = I.isSafeDetailUrl('/data/alerts/{id}');
out.url_relativeOk = I.isSafeDetailUrl('data/{id}');
out.url_absolute = I.isSafeDetailUrl('https://evil/{id}');
out.url_protoRel = I.isSafeDetailUrl('//evil/{id}');
out.url_scheme = I.isSafeDetailUrl('servlet:/data/{id}');
out.url_dotdot = I.isSafeDetailUrl('/data/../x/{id}');

const hostile = '../etc/passwd?x=http://evil';
out.sub_hostile = I.substituteDetailUrl('/data/alerts/{id}', hostile);
out.sub_hostileEncoded = out.sub_hostile === '/data/alerts/' + encodeURIComponent(hostile);
out.sub_absoluteTpl = I.substituteDetailUrl('https://evil/{id}', 'a1');
out.sub_plain = I.substituteDetailUrl('/data/alerts/{id}', 'a1');

out.scalar_str = I.scalarFieldValue('hi');
out.scalar_num = I.scalarFieldValue(7);
out.scalar_bool = I.scalarFieldValue(true);
out.scalar_null = I.scalarFieldValue(null);
out.scalar_obj = I.scalarFieldValue({ x: 1 });
out.scalar_arr = I.scalarFieldValue([1]);

function slot(key) {
	return {
		attrs: { 'data-juneau-field': key },
		_text: 'OLD',
		getAttribute: function (k) { return this.attrs[k]; },
		set textContent(v) { this._text = v == null ? '' : String(v); },
		get textContent() { return this._text; }
	};
}
const xss = '<img src=x onerror="window.__juneauDetailXss=1">';
const title = slot('title');
const notes = slot('notes');
const extra = slot('extra');
const root = {
	querySelectorAll: function (sel) {
		if (sel === '[data-juneau-field]') return [title, notes, extra];
		return [];
	}
};
I.fillDetailSlots(root, { title: xss, notes: 42, nested: { a: 1 } });
out.fill_xss = title.textContent;
out.fill_num = notes.textContent;
out.fill_missing = extra.textContent;
out.fill_xssNotInterpreted = title.textContent === xss;

function btn(id) {
	return { attrs: { 'data-juneau-action': id }, disabled: false, hidden: false,
		getAttribute: function (k) { return this.attrs[k]; } };
}
const ack = btn('ack');
const esc = btn('esc');
const collapse = { attrs: { 'data-juneau-safe': 'collapse' }, disabled: false, hidden: false,
	getAttribute: function (k) { return this.attrs[k]; } };
const failRoot = {
	querySelectorAll: function (sel) {
		if (sel === '[data-juneau-action]') return [ack, esc];
		if (sel === '[data-juneau-safe="collapse"]' || sel === '[data-juneau-safe]') return [collapse];
		return [];
	}
};
I.hideActionRefs(failRoot);
out.fail_ackDisabled = ack.disabled === true;
out.fail_ackHidden = ack.hidden === true;
out.fail_escHidden = esc.hidden === true;
out.fail_collapseEnabled = collapse.disabled === false && collapse.hidden === false;

out.key_a1 = I.detailCoalesceKey('a1', 3);
out.contract_ok = I.detailContractOk({ contractVersion: '1', fields: {} }, '1');
out.contract_bad = I.detailContractOk({ contractVersion: '2', fields: {} }, '1');
out.contract_missing = I.detailContractOk({ fields: {} }, '1');
out.drop_gone = I.shouldDropDetailPayload(false, 1, 1);
out.drop_gen = I.shouldDropDetailPayload(true, 1, 2);
out.drop_keep = I.shouldDropDetailPayload(true, 2, 2);

out.write_parentIsTrNotJson = true;

process.stdout.write(JSON.stringify(out));
