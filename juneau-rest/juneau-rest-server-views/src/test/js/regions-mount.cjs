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
 * regions-mount.cjs - always-on Node harness for JuneauViews.regions.mount: HTML-slot hookup that stamps
 * enrolment attributes from JS, binds ids to registered region populators, fails loud on a missing id or
 * unregistered name (no silent defaultPopulate), and keeps bus targeting by slot id.
 *
 *   Usage:  node regions-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>
 */
'use strict';

const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath) {
	console.error('usage: node regions-mount.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js>');
	process.exit(2);
}

const out = {};

function slot(env, id) {
	const el = env.el('div');
	el.id = id;
	env.body.appendChild(el);
	return el;
}

(async function () {

	// =================================================================================================================
	// Happy path: mount stamps enrolment attrs, enrols, and populate fills the BODY only.
	// =================================================================================================================
	{
		const { env, NS, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const painted = {};
		R.register('ssc-probes', function (ctx, container) {
			painted.probes = { id: ctx.id, key: ctx.key, childCountBefore: container.childNodes.length };
			container.appendChild(env.el('span'));
		});
		R.register('ssc-probe-details', function (ctx, container) {
			painted.details = { id: ctx.id, key: ctx.key };
			container.appendChild(env.el('p'));
		});
		const probes = slot(env, 'probes');
		const details = slot(env, 'details');
		const mounted = R.mount({ probes: 'ssc-probes', details: 'ssc-probe-details' });
		out.t1_thenable = !!(mounted && typeof mounted.then === 'function');
		out.t1_regionEnrolmentWasSync = probes.getAttribute('data-juneau-region') === 'probes'
			&& details.getAttribute('data-juneau-region') === 'details';
		const handles = await Promise.resolve(mounted);
		out.t1_mountIsOnRegions = typeof R.mount === 'function' && NS.regions.mount === R.mount;
		out.t1_notOnPages = NS.pages == null || NS.pages.mount == null;
		out.t1_handleCount = handles.length;
		out.t1_probesAttr = probes.getAttribute('data-juneau-region');
		out.t1_probesPopulate = probes.getAttribute('data-juneau-region-populate');
		out.t1_detailsAttr = details.getAttribute('data-juneau-region');
		out.t1_detailsPopulate = details.getAttribute('data-juneau-region-populate');
		out.t1_probesKeyIsSlotId = painted.probes && painted.probes.key === 'probes';
		out.t1_detailsKeyIsSlotId = painted.details && painted.details.key === 'details';
		out.t1_containerEmptyAtPopulate = painted.probes && painted.probes.childCountBefore === 0;
		out.t1_probesPainted = probes.childNodes.length === 1;
		out.t1_detailsPainted = details.childNodes.length === 1;
		out.t1_noErrors = rec.errors.length === 0;
	}

	// =================================================================================================================
	// Missing element id fails loud and enrols nothing (atomic: a later miss does not leave an earlier stamp).
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let defaultRan = false;
		R.register('ssc-probes', function () { /* author populator */ });
		R.builtins.default = function () { defaultRan = true; };
		const probes = slot(env, 'probes');
		let threw = false;
		let message = '';
		try {
			R.mount({ probes: 'ssc-probes', details: 'ssc-probe-details' });
		} catch (e) {
			threw = true;
			message = String(e && e.message ? e.message : e);
		}
		out.t2_threw = threw;
		out.t2_namesMissingId = message.indexOf("id 'details'") >= 0;
		out.t2_consoleError = rec.errorsMatching("id 'details'").length >= 1;
		out.t2_probesNotStamped = probes.getAttribute('data-juneau-region') == null;
		out.t2_probesNotEnrolled = probes.getAttribute('data-juneau-region-state') == null;
		out.t2_defaultDidNotRun = defaultRan === false;
	}

	// =================================================================================================================
	// Unregistered populator name fails loud; does not fall through to defaultPopulate.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let defaultRan = false;
		R.register('ssc-probes', function () { /* present so a mixed map still fails on the bad name */ });
		R.builtins.default = function () { defaultRan = true; };
		const probes = slot(env, 'probes');
		const details = slot(env, 'details');
		let threw = false;
		let message = '';
		try {
			R.mount({ probes: 'ssc-probes', details: 'no-such-populator' });
		} catch (e) {
			threw = true;
			message = String(e && e.message ? e.message : e);
		}
		out.t3_threw = threw;
		out.t3_namesBadPopulator = message.indexOf('no-such-populator') >= 0;
		out.t3_consoleError = rec.errorsMatching('no-such-populator').length >= 1;
		out.t3_probesNotStamped = probes.getAttribute('data-juneau-region') == null;
		out.t3_detailsNotStamped = details.getAttribute('data-juneau-region') == null;
		out.t3_defaultDidNotRun = defaultRan === false;
	}

	// =================================================================================================================
	// Blank populator name fails loud (the RegionTable omit-name hole mount exists to close).
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let defaultRan = false;
		R.builtins.default = function () { defaultRan = true; };
		slot(env, 'probes');
		let threw = false;
		try {
			R.mount({ probes: '  ' });
		} catch (e) {
			threw = true;
		}
		out.t4_threw = threw;
		out.t4_consoleError = rec.errorsMatching('blank or missing populator').length >= 1;
		out.t4_defaultDidNotRun = defaultRan === false;
	}

	// =================================================================================================================
	// Bus targeting after mount uses the slot id.  No Java RegionDef in the fixture.
	// =================================================================================================================
	{
		const { env, R } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		const received = { probes: [], details: [] };
		const ctxs = {};
		R.register('ssc-probes', function (ctx) {
			ctxs.probes = ctx;
			ctx.on(function (msg) { received.probes.push(msg.kind); });
		});
		R.register('ssc-probe-details', function (ctx) {
			ctxs.details = ctx;
			ctx.on(function (msg) { received.details.push(msg.kind); });
		});
		slot(env, 'probes');
		slot(env, 'details');
		R.mount({ probes: 'ssc-probes', details: 'ssc-probe-details' });
		ctxs.probes.emit({ kind: 'probe-selected' }, { to: 'details' });
		out.t5_detailsGotTargeted = received.details[0] === 'probe-selected';
		out.t5_probesDidNotGetOwn = received.probes.length === 0;
		out.t5_broadcastStillWorks = (function () {
			ctxs.details.emit({ kind: 'hello-all' });
			return received.probes[0] === 'hello-all';
		}());
	}

	// =================================================================================================================
	// Page nav: authors mark aria-current="page"; the runtime does not stamp selected from the URL.
	// No hash-swap, no pill classes, no selected-class double-encoding.
	// =================================================================================================================
	{
		const { env, NS } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		env.window.location = { pathname: '/runtime/settings/edit' };
		const nav = env.el('nav');
		nav.className = 'juneau-page-nav';
		const sections = env.el('div');
		sections.className = 'juneau-page-nav-sections';
		const runtime = env.el('a');
		runtime.className = 'juneau-page-nav-section';
		runtime.setAttribute('href', '/runtime');
		runtime.setAttribute('aria-current', 'page');
		const instances = env.el('a');
		instances.className = 'juneau-page-nav-section';
		instances.setAttribute('href', '/instances');
		sections.appendChild(runtime);
		sections.appendChild(instances);
		const children = env.el('div');
		children.className = 'juneau-page-nav-children';
		const dash = env.el('a');
		dash.className = 'juneau-page-nav-child';
		dash.setAttribute('href', '/runtime');
		const settings = env.el('a');
		settings.className = 'juneau-page-nav-child';
		settings.setAttribute('href', '/runtime/settings');
		settings.setAttribute('aria-current', 'page');
		children.appendChild(dash);
		children.appendChild(settings);
		nav.appendChild(sections);
		nav.appendChild(children);
		env.body.appendChild(nav);
		if (typeof NS.init !== 'undefined' && typeof NS.init.initAll === 'function')
			NS.init.initAll();
		out.t6_pageNavNotExported = NS.pageNav == null;
		out.t6_authorAriaPreserved = runtime.getAttribute('aria-current') === 'page'
			&& settings.getAttribute('aria-current') === 'page'
			&& instances.getAttribute('aria-current') !== 'page'
			&& dash.getAttribute('aria-current') !== 'page';
		out.t6_noSelectedClass = runtime.className.indexOf('selected') < 0
			&& settings.className.indexOf('selected') < 0
			&& instances.className.indexOf('selected') < 0
			&& dash.className.indexOf('selected') < 0;
		out.t6_noPillClass = nav.className.indexOf('jc-subtab') < 0 && settings.className.indexOf('jc-subtab') < 0;
	}

	// =================================================================================================================
	// String "juneau-table" throws a NEW message pointing at { table: url }; enrols nothing.
	// =================================================================================================================
	{
		const { env, R, rec } = H.load(rendersJsPath, viewsJsPath, regionsJsPath);
		let defaultRan = false;
		R.builtins.default = function () { defaultRan = true; };
		const incidents = slot(env, 'incidents');
		let threw = false;
		let message = '';
		try {
			R.mount({ incidents: 'juneau-table' });
		} catch (e) {
			threw = true;
			message = String(e && e.message ? e.message : e);
		}
		out.t7_threw = threw;
		out.t7_pointsAtTableUrl = message.indexOf('{ table: url }') >= 0;
		out.t7_notUnregisteredName = message.indexOf('no populator is registered under the name') < 0;
		out.t7_consoleError = rec.errorsMatching('juneau-table').length >= 1;
		out.t7_notStamped = incidents.getAttribute('data-juneau-region') == null;
		out.t7_defaultDidNotRun = defaultRan === false;
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
