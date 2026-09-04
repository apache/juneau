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
 * selftargeted-idempotency.cjs - always-on Node harness for WORK-J0512's B2 tripwire: what `submitActionDialog`
 * ACTUALLY TRANSMITS as `extra.targetId`, for each branch of the opt-in-gated precedence rule
 *
 *     const targetId = (modal?.selfTargeted && modal?.idempotencyKey != null)
 *         ? modal.idempotencyKey
 *         : (tr?.dataset?.juneauRowId ?? null);
 *
 * This harness exists because a source-text check cannot tell a correct implementation from a regressed one: the
 * literals "idempotencyKey" and "targetId" appear in the function body under EITHER an opt-in-gated rule or a
 * blanket "the key always wins" one, so the presence check that used to carry this property would have stayed
 * green through the exact regression it was supposed to catch.  These three cases INVOKE the function and read
 * the transmitted request body.
 *
 *   (i)   selfTargeted + a key + a row whose id is DIFFERENT -> the KEY is transmitted (the opt-in works).
 *   (ii)  a key with NO selfTargeted + a row (the example app's ack-form shape) -> the ROW's id is transmitted
 *         (an artifact-bound key's real target is not silently discarded).
 *   (iii) selfTargeted + a key + NO ROW AT ALL -> the KEY is transmitted (the row-less case B2 exists for; a
 *         `tr == null` guard reintroduced around the opt-in branch would revive the original bug, and cases
 *         (i)/(ii) alone would both stay green while it did).
 *
 * Plus the fallbacks that must not move: no key at all, and an opt-in with no key to honour it with.
 *
 * Usage:  node selftargeted-idempotency.cjs <juneau-renders.js> <juneau-views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test.
 */
'use strict';

const path = require('node:path');
const { loadViews, jsonResponse } = require(path.join(__dirname, 'views-dom-shim.cjs'));

const rendersJsPath = process.argv[2];
const viewsJsPath = process.argv[3];
if (!rendersJsPath || !viewsJsPath) {
	console.error('usage: node selftargeted-idempotency.cjs <juneau-renders.js> <juneau-views.js>');
	process.exit(2);
}

const out = {};
{
	const probe = loadViews(rendersJsPath, viewsJsPath);
	out.hasSubmitActionDialog = typeof probe.I?.submitActionDialog === 'function';
}
if (!out.hasSubmitActionDialog) { process.stdout.write(JSON.stringify(out)); process.exit(0); }

const KEY = 'k-0123456789abcdef';
const ROW_ID = 'row-42';

/**
 * Invokes submitActionDialog(...) once and returns the parsed request body it transmitted (or null when nothing
 * was sent).  `withRow` false is the row-less case - the dialog is opened from a ribbon, so there is no `<tr>`.
 */
function transmit(modal, withRow) {
	const { env, I } = loadViews(rendersJsPath, viewsJsPath);
	const table = env.el('table');
	table.setAttribute('data-juneau-csrf', 'tok-1');
	env.body.appendChild(table);

	let tr = null;
	if (withRow) {
		tr = env.el('tr');
		tr.dataset.juneauRowId = ROW_ID;
		table.appendChild(tr);
	}

	const bodies = [];
	env.setFetch(function (url, opts) {
		bodies.push(opts && opts.body);
		return Promise.resolve(jsonResponse({ outcome: 'success' }));
	});

	const action = { id: 'create', label: 'Create', method: 'POST', endpoint: '/projects' };
	I.submitActionDialog(modal, action, table, tr, { table: table, viewDef: { rowActions: [action] } }, null);
	return bodies.length === 1 ? JSON.parse(bodies[0]) : null;
}

// (i) The opt-in wins over a real, DIFFERENT row id.
{
	const body = transmit({ selfTargeted: true, idempotencyKey: KEY }, true);
	out.optIn_withRow_targetId = body ? body.targetId : null;
	out.optIn_withRow_idempotencyKey = body ? body.idempotencyKey : null;
	out.optIn_withRow_rowIdWasDifferent = KEY !== ROW_ID;
}

// (ii) No opt-in: an artifact-bound key keeps sending the ROW's real id (the ack-form shape - unchanged today).
{
	const body = transmit({ idempotencyKey: KEY }, true);
	out.noOptIn_withRow_targetId = body ? body.targetId : null;
	out.noOptIn_withRow_idempotencyKey = body ? body.idempotencyKey : null;
}

// (iii) The opt-in with NO ROW: the key is transmitted with nothing to fall back to.  Before B2, a row-less
// submit omitted targetId entirely and failed closed on the server's binding check.
{
	const body = transmit({ selfTargeted: true, idempotencyKey: KEY }, false);
	out.optIn_rowless_targetId = body ? body.targetId : null;
	out.optIn_rowless_idempotencyKey = body ? body.idempotencyKey : null;
}

// Fallbacks that must not move.
{
	const noKeyWithRow = transmit({ title: 'Confirm?' }, true);
	out.noKey_withRow_targetId = noKeyWithRow ? noKeyWithRow.targetId : null;
	out.noKey_withRow_hasIdempotencyKey = noKeyWithRow != null && Object.hasOwn(noKeyWithRow, 'idempotencyKey');

	const noKeyRowless = transmit({ title: 'Confirm?' }, false);
	out.noKey_rowless_hasTargetId = noKeyRowless != null && Object.hasOwn(noKeyRowless, 'targetId');

	// An opt-in with no key to honour: the gate requires BOTH, so this falls back to the row id.
	const optInNoKeyWithRow = transmit({ selfTargeted: true }, true);
	out.optInNoKey_withRow_targetId = optInNoKeyWithRow ? optInNoKeyWithRow.targetId : null;

	const optInNoKeyRowless = transmit({ selfTargeted: true }, false);
	out.optInNoKey_rowless_hasTargetId = optInNoKeyRowless != null && Object.hasOwn(optInNoKeyRowless, 'targetId');
}

process.stdout.write(JSON.stringify(out));
