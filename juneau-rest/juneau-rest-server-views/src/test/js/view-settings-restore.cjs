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
 * view-settings-restore.cjs - always-on Node harness for Gap 1: persisted View Settings (page-state store) are
 * applied to the LIVE grid at construction time, in both client and server data modes.
 *
 * Regression for the JRM e2e "hidden column comes back after reload": initTableFromDef's go() consulted only the
 * named-saved-view path, never the View Settings blob written by every dialog Apply, so a reload re-rendered the
 * catalog-default columns.  Loads the real pagestate/renders/config/views scripts against the shared DOM shim with a
 * recording fake jQuery/DataTables, runs initTableFromDef against a pre-seeded store (a "reload"), and reports the
 * options handed to the DataTable constructor.
 *
 *   Usage:  node view-settings-restore.cjs <pagestate.js> <renders.js> <config.js> <views.js>
 *
 * Prints ONE JSON object to stdout; every assertion lives in the Java test (this script only OBSERVES).
 */
'use strict';

const { makeEnv, loadScripts } = require('./views-dom-shim.cjs');

const paths = process.argv.slice(2);
if (paths.length < 4) {
	console.error('usage: node view-settings-restore.cjs <pagestate.js> <renders.js> <config.js> <views.js>');
	process.exit(2);
}

const CATALOG = [
	{ data: 'name', title: 'Name', pinned: true, search: { type: 'text' } },
	{ data: 'stage', title: 'Stage', search: { type: 'text' } },
	{ data: 'status', title: 'Status', search: { type: 'text' } },
	{ data: 'hidden', title: 'Hidden', defaultVisible: false }
];

/**
 * Loads a fresh env with the scripts, seeds localStorage with {@code blob} under the table's View Settings key, then
 * runs initTableFromDef.  Returns what the recording DataTable constructor saw.
 */
function reload(dataMode, blob, viewDefExtra) {
	const env = makeEnv();
	const captured = { opts: null, pageLen: null, drawn: 0, error: null };
	function fakeDt() {
		const dt = {
			page: Object.assign(function () { return 0; }, {
				len: function (n) {
					if (n === undefined) {
						return captured.pageLen == null ? 10 : captured.pageLen;
					}
					captured.pageLen = n;
					return { draw: function () { captured.drawn++; } };
				},
				info: function () { return { page: 0, pages: 1, length: 25, recordsTotal: 0, recordsDisplay: 0, start: 0, end: 0 }; }
			}),
			on: function () { return dt; }, off: function () { return dt; },
			columns: function () { return { every: function () { /* no-op */ }, visible: function () { /* no-op */ } }; },
			column: function () { return { visible: function () { /* no-op */ }, header: function () { return env.el('th'); } }; },
			ajax: { reload: function () { /* no-op */ } }, draw: function () { /* no-op */ }, destroy: function () { /* no-op */ }
		};
		return dt;
	}
	const $ = function () {
		return {
			DataTable: function (opts) { captured.opts = opts; return fakeDt(); },
			on: function () { /* no-op */ }, off: function () { /* no-op */ }, find: function () { return []; }
		};
	};
	$.fn = { DataTable: function () { /* no-op */ }, dataTable: { isDataTable: function () { return false; } } };
	env.window.jQuery = $;
	env.window.JuneauDataTables = { ajax: function () { return { url: '/x' }; } };
	const { NS } = loadScripts(paths, env);

	const table = env.el('table');
	table.dataset.juneauView = 'rel';
	env.body.appendChild(table);
	if (blob) {
		// Same key shape pagestate.js writes via NS.pageState.table('rel').set('viewSettings', blob).
		NS.pageState.table('rel').set('viewSettings', blob);
	}
	const viewDef = {
		contractVersion: '5', id: 'rel', dataMode: dataMode, dataUrl: '/x', columns: CATALOG,
		columnConfig: {}, defaultOrder: [{ data: 'name', dir: 'asc' }],
		...viewDefExtra
	};
	try {
		const p = NS.init.initTableFromDef(table, viewDef, {});
		return Promise.resolve(p).catch(function (error) { captured.error = String(error?.message || error); }).then(function () {
			return { captured: captured, NS: NS, table: table, env: env };
		});
	} catch (error) {
		captured.error = String(error?.message || error);
		return Promise.resolve({ captured: captured, NS: NS, table: table });
	}
}

function colInfo(opts) {
	return (opts?.columns || []).filter(function (c) { return c.data != null; }).map(function (c) {
		return { data: c.data, visible: c.visible !== false, searchable: c.searchable !== false };
	});
}

const STAGE_HIDDEN = {
	schemaVersion: 2, visible: ['name', 'status'], order: ['name', 'stage', 'status', 'hidden'],
	labels: {}, formats: {},
	search: ['name', 'stage', 'status'],
	sort: [{ column: 'status', dir: 'desc' }],
	options: { pageSize: 50, wrap: true, density: 'compact', autoRefreshMs: 0 }
};

(async function () {
	const out = {};
	for (const mode of ['client', 'server']) {
		const r = await reload(mode, STAGE_HIDDEN);
		out[mode + '_columns'] = colInfo(r.captured.opts);
		out[mode + '_order'] = r.captured.opts?.order || null;
		out[mode + '_pageLen'] = r.captured.pageLen;
		out[mode + '_wrapClass'] = r.table.classList.contains('juneau-view-wrap');
		out[mode + '_error'] = r.captured.error;
	}
	// No blob at all: catalog defaults untouched (regression guard against over-eager restore).
	const none = await reload('server', null);
	out.noBlob_columns = colInfo(none.captured.opts);
	// Tab-restricted view ('view' tab absent): a stale blob must NOT hide Stage.
	const restricted = await reload('server', STAGE_HIDDEN, { columnConfig: { tabs: ['options'] } });
	out.restricted_columns = colInfo(restricted.captured.opts);
	out.restricted_order = restricted.captured.opts?.order || null;
	// Stale schemaVersion: whole blob discarded (defaults painted), the one-time reset flag is captured on ctx and the
	// stored blob is gone, so the dialog seed (same memoized read) can still show the notice exactly once.
	const stale = await reload('server', { ...STAGE_HIDDEN, schemaVersion: 1 });
	out.stale_columns = colInfo(stale.captured.opts);
	out.stale_reset = stale.table.__juneauCtx._lastAppliedViewSettings.reset;
	out.stale_blobRemoved = stale.NS.pageState.table('rel').get('viewSettings') == null;
	// Options facet drives auto-refresh wiring at construction (Gap 7).
	const auto = await reload('server', {
		...STAGE_HIDDEN,
		options: { pageSize: 25, wrap: false, density: 'comfortable', autoRefreshMs: 30000 }
	});
	out.auto_timerWired = auto.table.__juneauCtx._autoRefreshTimerId != null;
	out.noAuto_timerWired = (await reload('server', STAGE_HIDDEN)).table.__juneauCtx._autoRefreshTimerId != null;
	// Finding 1: the chooser's draft is seeded from the restored settings in ONE place (mountChooser, via the memoized
	// read), so the dialog opens on the stored (unchecked) column, not on catalog defaults.
	const seeded = await reload('server', STAGE_HIDDEN);
	const sctx = seeded.table.__juneauCtx;
	out.seed_draftVisible = sctx._configDraft ? sctx._configDraft.visible : null;
	seeded.NS.config.openChooser(seeded.table, sctx);
	out.seed_dialogStageChecked = (function () {
		const cb = (function () { const r = seeded.env.document.querySelector('.juneau-config-col-row[data-col="stage"]'); return r ? r.querySelector('.juneau-config-col-vis') : null; })();
		return cb ? !!cb.checked : null;
	})();
	out.seed_ariaLabels = (function () {
		const cb = (function () { const r = seeded.env.document.querySelector('.juneau-config-col-row[data-col="stage"]'); return r ? r.querySelector('.juneau-config-col-vis') : null; })();
		return cb ? cb.getAttribute('aria-label') : null;
	})();
	// Stale schemaVersion: the reset notice reaches the dialog exactly once.
	const st = await reload('server', { ...STAGE_HIDDEN, schemaVersion: 1 });
	const stctx = st.table.__juneauCtx;
	out.stale_noticeArmed = stctx._configResetNotice === true;
	st.NS.config.openChooser(st.table, stctx);
	out.stale_noticeText = stctx._configStatusEl ? stctx._configStatusEl.textContent : null;
	out.stale_noticeCleared = stctx._configResetNotice === false;
	// Finding 2: the Sort draft is seeded from viewDef.defaultOrder (not every sort-capable column).
	const noBlobCtx = none.table.__juneauCtx;
	out.sortSeed_noBlob = noBlobCtx._configDraft.sort;
	out.sortSeed_persisted = none.NS.config.viewSettingsFromDraft(noBlobCtx._configDraft).sort;
	const noOrder = await reload('server', null, { defaultOrder: [] });
	out.sortSeed_emptyDefault = noOrder.table.__juneauCtx._configDraft.sort;
	// Finding 3: teardownTable clears the auto-refresh timer.
	const actx = auto.table.__juneauCtx;
	auto.NS.init.teardownTable(auto.table, actx);
	out.auto_timerAfterTeardown = actx._autoRefreshTimerId == null;
	// Finding 9: a table without columnConfig never applies (or deletes) a stored blob.
	const noCfg = await reload('server', STAGE_HIDDEN, { columnConfig: null });
	out.noColumnConfig_columns = colInfo(noCfg.captured.opts);
	out.noColumnConfig_blobKept = noCfg.NS.pageState.table('rel').get('viewSettings') != null;
	process.stdout.write(JSON.stringify(out));
})();
