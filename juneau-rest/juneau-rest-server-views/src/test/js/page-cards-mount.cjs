/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * page-cards-mount.cjs - always-on Node round-trip for the page-cards sidecar scanner (F2 / F10).
 *
 * Three things are proven end-to-end against the REAL runtime (juneau-page-cards.js booted over the real
 * JuneauViews.regions pipeline, all under the DOM shim):
 *
 *   t_table_*    - the SLOT_META datatables envelope CardEnvelope.liftTable emits (byte-for-byte the JSON the
 *                  Java golden in CardDirective_Test asserts) is accepted by regions.mount and paints a table.
 *   t_populate_* - a `{contractVersion,id,populate}` sidecar boots into a regions.mount populate hookup: the
 *                  populator registered by name actually runs and paints the slot.
 *   t_template_* - a name-only `{contractVersion,id,template}` sidecar (no source) looks the template up in
 *                  JuneauPage.templates and sets the slot's innerHTML from it - no regions.mount for this one.
 *   t_pill_*     - the `pill` catalog renderer (Task 15): its display facet emits `.jc-pill`/`.jc-pill-dot` chip
 *                  markup with ZERO inline hex (colour comes from the --jc-pill-* palette classes), the render id
 *                  `pill` resolves through parseRenderId/resolveRenderer (no unknown-id fallback), and a datatables
 *                  SLOT_META carrying a `render:'pill'` column mounts cleanly (table paints, no error, no console error).
 *
 *   Usage:  node page-cards-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-page-cards.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath, pageCardsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath || !pageCardsJsPath) {
	console.error('usage: node page-cards-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-page-cards.js>');
	process.exit(2);
}

// The lifted SLOT_META CardEnvelope.liftTable emits for:
//   <@card type="datatables" id="releases">{ dataUrl:'/rest/releases/data', columns:[{key:'name',label:'Name'}] }
// Contract versions are hard-coded to the same literals the Java golden asserts (ViewSlot "1" / ViewDef "4").
const LIFTED_TABLE = {
	contractVersion: '1',
	layout: 'wide',
	view: {
		contractVersion: '4',
		id: 'releases',
		dataUrl: '/rest/releases/data',
		columns: [{ data: 'name', title: 'Name' }]
	}
};

// The sidecar contract version juneau-page-cards.js's collect() handshake demands (== CardDirectiveModel's).
const SIDECAR_CONTRACT_VERSION = '1';

function slot(env, id) {
	const el = env.el('div');
	el.id = id;
	env.body.appendChild(el);
	return el;
}

/** Emits the strict-JSON sidecar the Java side writes: <script class="juneau-card-sidecar">{...}</script>. */
function sidecar(env, obj) {
	const s = env.el('script');
	s.className = 'juneau-card-sidecar';
	s.setAttribute('type', 'application/json');
	s.textContent = JSON.stringify(obj);
	env.body.appendChild(s);
	return s;
}

(async function () {
	const out = {};

	// =================================================================================================================
	// t_table_lift: mount the lifted envelope into an empty #releases slot; a real table paints, no error banner.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { pageCardsJsPath });
		const releases = slot(env, 'releases');
		out.t_emptyBefore = releases.childNodes.length === 0;
		await Promise.resolve(R.mount({ releases: { table: LIFTED_TABLE } }));
		const table = releases.querySelector('table[data-juneau-view="releases"]');
		out.t_hasTable = table != null;
		out.t_thead = table != null && table.querySelector('thead') != null;
		out.t_noError = releases.querySelector('.juneau-view-error') == null;
		out.t_noConsoleErrors = rec.errors.length === 0;
	}

	// =================================================================================================================
	// t_populate: a {contractVersion,id,populate} sidecar boots into a regions.mount populate hookup - the populator
	// registered under that name actually runs (F10: real scanner -> real mount, not a Java string pin).
	// =================================================================================================================
	{
		const { env, R, P, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { pageCardsJsPath });
		let ran = 0;
		R.register('probes', function (ctx, container) {
			ran++;
			const span = env.el('span');
			span.className = 'probe-painted';
			span.textContent = 'probes ran';
			container.appendChild(span);
		});
		const probes = slot(env, 'probes');
		sidecar(env, { contractVersion: SIDECAR_CONTRACT_VERSION, id: 'probes', populate: 'probes' });
		out.tp_bootIsFunction = typeof P.boot === 'function';
		await Promise.resolve(P.boot());
		out.tp_populatorRanOnce = ran === 1;
		const painted = probes.querySelector('span.probe-painted');
		out.tp_painted = painted != null && painted.textContent === 'probes ran';
		out.tp_noConsoleErrors = rec.errors.length === 0;
	}

	// =================================================================================================================
	// t_template: a name-only {contractVersion,id,template} sidecar (no source) is looked up in JuneauPage.templates
	// and its return value becomes the slot's innerHTML (F8/F10: name-only lookup, no regions.mount).
	// =================================================================================================================
	{
		const { env, P, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { pageCardsJsPath });
		P.templates.register('dialogBody', function () { return '<p>hello from template</p>'; });
		const dlg = slot(env, 'dlg');
		sidecar(env, { contractVersion: SIDECAR_CONTRACT_VERSION, id: 'dlg', template: 'dialogBody' });
		await Promise.resolve(P.boot());
		out.tt_innerHtmlFromTemplate = dlg.innerHTML === '<p>hello from template</p>';
		out.tt_noConsoleErrors = rec.errors.length === 0;
	}

	// =================================================================================================================
	// t_pill: the `pill` catalog renderer (Task 15).
	//   (a) its display facet emits .jc-pill/.jc-pill-dot chip markup with NO inline hex (colour is palette-class-driven);
	//   (b) the render id `pill` resolves through parseRenderId/resolveRenderer with no unknown-id fallback;
	//   (c) a datatables SLOT_META carrying a render:'pill' column mounts cleanly (table paints, no error, no console error).
	// =================================================================================================================
	{
		const { NS, R, rec, env } = H.load(rendersJsPath, viewsJsPath, regionsJsPath, { pageCardsJsPath });

		// (a) Direct display facet: a status pill from the closed tone palette.
		const pill = NS._renderers && NS._renderers['pill'];
		out.pill_isRegistered = pill != null && typeof pill.display === 'function';
		const markup = pill ? pill.display('open', {}, { field: 'status', tone: 'success' }) : '';
		out.pill_hasJcPillClass = markup.indexOf('class="jc-pill tag') >= 0;
		out.pill_hasDot = markup.indexOf('jc-pill-dot') >= 0;
		out.pill_hasTone = markup.indexOf('is-success') >= 0;
		out.pill_hasValue = markup.indexOf('open') >= 0;
		// The chip carries its own colour ONLY via CSS classes reading --jc-pill-*; the renderer emits no inline hex.
		out.pill_noInlineHex = markup.indexOf('#') < 0;
		out.pill_noSlds = markup.toLowerCase().indexOf('slds') < 0;

		// (b) The render id resolves without falling back to the raw-value warning.
		const spec = typeof NS.parseRenderId === 'function' ? NS.parseRenderId('pill') : null;
		out.pill_parseRenderId = spec != null && spec.id === 'pill';
		out.pill_resolves = typeof NS.resolveRenderer === 'function' && NS.resolveRenderer('pill') != null;

		// (c) A datatables SLOT_META whose column binds render:'pill' mounts cleanly.
		const releases = slot(env, 'releases');
		const table = {
			contractVersion: '1',
			layout: 'wide',
			view: {
				contractVersion: '4',
				id: 'releases',
				dataUrl: '/rest/releases/data',
				columns: [{ data: 'status', title: 'Status', render: 'pill' }]
			}
		};
		await Promise.resolve(R.mount({ releases: { table: table } }));
		const el = releases.querySelector('table[data-juneau-view="releases"]');
		out.pill_mountHasTable = el != null;
		out.pill_mountThead = el != null && el.querySelector('thead') != null;
		out.pill_mountNoError = releases.querySelector('.juneau-view-error') == null;
		out.pill_mountNoConsoleErrors = rec.errors.length === 0;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e?.stack ? e.stack : e));
	process.exit(1);
});
