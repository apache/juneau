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
 * chrome-tip.cjs - always-on Node harness for the instant cursor tooltip on ribbon/paging chrome.
 *
 *   Usage:  node chrome-tip.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { loadViews } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node chrome-tip.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const { env, I } = loadViews(rendersJsPath, viewsJsPath);
const out = {
	hasInit: typeof I?.initCursorTooltip === 'function'
};

function tipNode() {
	return env.document.getElementById('jc-cursor-tip');
}

function hover(target, x, y) {
	env.dispatchDocument('mouseover', { target: target, clientX: x, clientY: y });
}

function move(target, x, y) {
	env.dispatchDocument('mousemove', { target: target, clientX: x, clientY: y });
}

function leave(target, related) {
	env.dispatchDocument('mouseout', { target: target, relatedTarget: related || env.body, clientX: 1, clientY: 1 });
}

(function pagingNextPage() {
	const pill = env.document.createElement('div');
	pill.className = 'juneau-view-pagingpill';
	const btn = env.document.createElement('button');
	btn.className = 'juneau-view-pagingpill-btn';
	btn.title = 'Next page';
	btn.setAttribute('aria-label', 'Next page');
	pill.appendChild(btn);
	env.body.appendChild(pill);

	out.paging_titleBeforeHover = btn.getAttribute('title') === 'Next page';
	out.paging_ariaBeforeHover = btn.getAttribute('aria-label') === 'Next page';
	out.paging_noTipAttrBeforeHover = btn.getAttribute('data-jc-tip') == null;

	hover(btn, 40, 50);
	const tip = tipNode();
	out.paging_titleMovedOff = btn.getAttribute('title') == null && btn.title === '';
	out.paging_tipAttr = btn.getAttribute('data-jc-tip');
	out.paging_ariaKept = btn.getAttribute('aria-label') === 'Next page';
	out.paging_tipText = tip ? tip.textContent : null;
	out.paging_tipVisible = !!(tip && tip.style.display === 'block');
	out.paging_tipClass = tip ? tip.className : null;
	out.paging_tipLeft = tip ? tip.style.left : null;
	out.paging_tipTop = tip ? tip.style.top : null;

	move(btn, 80, 90);
	out.paging_followedCursor = tip && tip.style.left === '92px' && tip.style.top === '102px';

	leave(btn, env.body);
	out.paging_hiddenOnLeave = tip && tip.style.display === 'none';
})();

(function ribbonIconTitle() {
	const group = env.document.createElement('div');
	group.className = 'juneau-view-ribbon-group';
	const btn = env.document.createElement('button');
	btn.className = 'juneau-view-ribbon-btn';
	btn.title = 'Refresh';
	btn.setAttribute('aria-label', 'Refresh');
	group.appendChild(btn);
	env.body.appendChild(group);

	hover(btn, 10, 20);
	out.ribbon_titleMovedOff = btn.getAttribute('title') == null;
	out.ribbon_tipAttr = btn.getAttribute('data-jc-tip');
	out.ribbon_ariaKept = btn.getAttribute('aria-label') === 'Refresh';
	out.ribbon_tipText = tipNode() ? tipNode().textContent : null;
	out.ribbon_tipVisible = tipNode() && tipNode().style.display === 'block';
	leave(btn, env.body);
})();

(function formFieldOutsideChrome() {
	const input = env.document.createElement('input');
	input.title = 'Search help';
	input.setAttribute('aria-label', 'Search table');
	env.body.appendChild(input);

	hover(input, 15, 25);
	out.form_titleKept = input.getAttribute('title') === 'Search help';
	out.form_noTipAttr = input.getAttribute('data-jc-tip') == null;
	out.form_tipHidden = !tipNode() || tipNode().style.display === 'none';
	out.form_ariaKept = input.getAttribute('aria-label') === 'Search table';
})();

(function titledNodeOutsideChrome() {
	const span = env.document.createElement('span');
	span.title = 'Random page title';
	env.body.appendChild(span);

	hover(span, 5, 5);
	out.outside_titleKept = span.getAttribute('title') === 'Random page title';
	out.outside_noTipAttr = span.getAttribute('data-jc-tip') == null;
	out.outside_tipHidden = !tipNode() || tipNode().style.display === 'none';
})();

(function helperButtonRowTitle() {
	const row = env.document.createElement('div');
	row.className = 'juneau-view-helper-btn-row';
	const btn = env.document.createElement('button');
	btn.className = 'juneau-view-helper-btn';
	btn.title = 'Acknowledge';
	btn.setAttribute('aria-label', 'Acknowledge');
	row.appendChild(btn);
	env.body.appendChild(row);

	hover(btn, 10, 20);
	out.helper_titleMovedOff = btn.getAttribute('title') == null;
	out.helper_tipAttr = btn.getAttribute('data-jc-tip');
	out.helper_ariaKept = btn.getAttribute('aria-label') === 'Acknowledge';
	out.helper_tipText = tipNode() ? tipNode().textContent : null;
	out.helper_tipVisible = tipNode() && tipNode().style.display === 'block';
	leave(btn, env.body);
})();

(function explicitDataTip() {
	const span = env.document.createElement('span');
	span.setAttribute('data-jc-tip', 'Explicit label');
	env.body.appendChild(span);

	hover(span, 30, 40);
	out.explicit_tipText = tipNode() ? tipNode().textContent : null;
	out.explicit_tipVisible = tipNode() && tipNode().style.display === 'block';
	leave(span, env.body);
	out.explicit_hiddenOnLeave = tipNode() && tipNode().style.display === 'none';
})();

(function idempotentInit() {
	I.initCursorTooltip();
	I.initCursorTooltip();
	const pill = env.document.createElement('div');
	pill.className = 'juneau-view-pagingpill';
	const btn = env.document.createElement('button');
	btn.className = 'juneau-view-pagingpill-btn';
	btn.title = 'Previous page';
	btn.setAttribute('aria-label', 'Previous page');
	pill.appendChild(btn);
	env.body.appendChild(pill);
	hover(btn, 12, 12);
	out.reinit_stillWorks = btn.getAttribute('data-jc-tip') === 'Previous page'
		&& tipNode() && tipNode().textContent === 'Previous page'
		&& tipNode().style.display === 'block';
	leave(btn, env.body);
})();

(function pagingMenubtnEmitStamped() {
	const pill = env.document.createElement('div');
	pill.className = 'juneau-view-pagingpill';
	const btn = env.document.createElement('button');
	btn.className = 'juneau-view-pagingpill-menubtn';
	btn.setAttribute('data-jc-tip', 'Rows per page');
	btn.setAttribute('aria-label', 'Rows per page');
	pill.appendChild(btn);
	env.body.appendChild(pill);

	out.menubtn_noNativeTitleBeforeHover = btn.getAttribute('title') == null && (btn.title === '' || btn.title == null);
	hover(btn, 40, 50);
	out.menubtn_stillNoNativeTitle = btn.getAttribute('title') == null && (btn.title === '' || btn.title == null);
	out.menubtn_tipText = tipNode() ? tipNode().textContent : null;
	out.menubtn_tipVisible = !!(tipNode() && tipNode().style.display === 'block');
	leave(btn, env.body);
})();

process.stdout.write(JSON.stringify(out));
