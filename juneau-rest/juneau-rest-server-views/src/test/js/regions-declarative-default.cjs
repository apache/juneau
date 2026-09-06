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
 * regions-declarative-default.cjs - always-on Node harness for WORK-J0522c: the `RegionDef` descriptor's client
 * twin (`ctx.declared`), `ctx.fetchDeclared()`'s handshake-checked fetch, the reserved `"default"` populator's
 * §8.5-normative algorithm, R14a's pre-fetch matrix, the poll lifecycle (§8.3.1), and - the acceptance gate for
 * this whole child - L12's non-privilege proof (tests 11 and 12).
 *
 * Loads juneau-helpers.js IN ADDITION to the three assets regions-primitive/-bus/-barrier load: the reserved
 * default paints through `ctx.helpers[ctx.declared.renderer]`, so this harness alone needs the helper library.
 *
 *   Usage:  node regions-declarative-default.cjs <juneau-renders.js> <juneau-views.js> <juneau-regions.js> <juneau-helpers.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const H = require(path.join(__dirname, 'regions-harness.cjs'));

const [, , rendersJsPath, viewsJsPath, regionsJsPath, helpersJsPath] = process.argv;
if (!rendersJsPath || !viewsJsPath || !regionsJsPath || !helpersJsPath) {
	console.error('usage: node regions-declarative-default.cjs <juneau-renders.js> <juneau-views.js>'
		+ ' <juneau-regions.js> <juneau-helpers.js>');
	process.exit(2);
}

const out = {};

// The DOM shim (views-dom-shim.cjs) has no innerHTML/outerHTML - it is deliberately minimal (see its own header).
// "Byte-identical DOM" for tests 10a/10b/11 is proven instead against a canonical serialization: tag name,
// attributes sorted by key (so attribute-set equality is order-independent, matching two real DOM trees painted
// by different code paths that may set attributes in a different order), and children recursively - which is a
// stricter, not a looser, check than a literal innerHTML byte-compare would be for two independently-built trees.
function escapeText(s) {
	return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
function escapeAttr(s) {
	return escapeText(s).replace(/"/g, '&quot;');
}
function serializeNode(n) {
	if (n.nodeType === 3) return escapeText(n.textContent);
	if (n.nodeType !== 1) return '';
	const tag = n.tagName.toLowerCase();
	// The shim (views-dom-shim.cjs) only mirrors `class` into `attrs.class` for a `setAttribute('class', ...)`
	// call - a direct `el.className = "..."` assignment (what every helper actually does) lands ONLY on the
	// `className` data property, never on `attrs`.  Folding it in here is required for the comparison to see the
	// class at all; every helper's whole visual identity is carried by class names, so omitting it would make
	// this "byte-identical" check blind to the one attribute two independently-built trees are most likely to
	// differ on.
	const attrMap = Object.assign({}, n.attrs || {});
	if (n.className) attrMap.class = n.className; else delete attrMap.class;
	const attrs = Object.keys(attrMap).sort()
		.map(function (k) { return ' ' + k + '="' + escapeAttr(attrMap[k]) + '"'; }).join('');
	// The shim's `el.textContent = v` setter (views-dom-shim.cjs) stores the string on a private `_text` field
	// rather than minting a real child text node, so a leaf whose content was set that way has EMPTY childNodes -
	// falling through to just the tag+attrs would silently drop it from the comparison and let two trees that
	// differ only in painted VALUES pass as "byte-identical".  `el.textContent` (the getter) already knows this
	// rule, so read the leaf's text through it rather than through childNodes directly.
	const children = (n.childNodes || []).length > 0
		? n.childNodes.map(serializeNode).join('')
		: escapeText(n.textContent);
	return '<' + tag + attrs + '>' + children + '</' + tag + '>';
}
function serializeChildren(el) {
	return (el.childNodes || []).map(serializeNode).join('');
}

function load(opts) {
	return H.load(rendersJsPath, viewsJsPath, regionsJsPath, Object.assign({ helpersJsPath: helpersJsPath }, opts));
}

(async function () {

	// =================================================================================================================
	// Test 2 (declarative) - `ctx.declared` is populated from the placeholder descriptor attribute, and an absent
	// descriptor falls through to the identity-only shape with per-type `lazy` defaults (F9).
	// =================================================================================================================
	{
		const { env, R } = load();
		let ctx = null;
		R.register('capture', function (c) { ctx = c; });
		const el = H.mkRegion(env, {
			id: 'd1', type: 'card-body', populate: 'capture',
			declared: {
				contractVersion: '1', dataUrl: '/api/widgets/42/status', renderer: 'fieldGrid',
				lazy: false, refreshMs: 30000, fields: [{ data: 'status', label: 'Status' }],
				titleFields: ['status'], params: { verbose: true }
			}
		});
		const calls = H.abortableFetch(env); // dataUrl is set, so R14a's pre-fetch will run before `capture` fires
		R.initRegion(el);
		calls.resolve(0, { contractVersion: '1', fields: { status: 'ok' } });
		await H.flush();
		out.t2_declaredKeys = Object.keys(ctx.declared).sort();
		out.t2_dataUrl = ctx.declared.dataUrl;
		out.t2_renderer = ctx.declared.renderer;
		out.t2_lazy = ctx.declared.lazy;
		out.t2_refreshMs = ctx.declared.refreshMs;
		out.t2_fields = ctx.declared.fields;
		out.t2_titleFields = ctx.declared.titleFields;
		out.t2_params = ctx.params;
	}
	{
		// Absent descriptor: identity-only, per-type lazy defaults (row-detail/card-body=false, tab-body=true).
		const { env, R } = load();
		const seen = {};
		R.register('capture2', function (c) { seen[c.type] = c.declared; });
		for (const type of ['row-detail', 'card-body', 'tab-body']) {
			const el = H.mkRegion(env, { id: 'nd-' + type, type: type, populate: 'capture2' });
			R.initRegion(el);
		}
		out.t2b_lazyDefaults = {
			'row-detail': seen['row-detail'].lazy,
			'card-body': seen['card-body'].lazy,
			'tab-body': seen['tab-body'].lazy
		};
		out.t2b_dataUrlNull = Object.values(seen).every(function (d) { return d.dataUrl === null; });
	}
	{
		// A contractVersion MISMATCH on the descriptor logs one error and falls through to identity-only.
		const { env, R, rec } = load();
		let ctx = null;
		R.register('capture3', function (c) { ctx = c; });
		const el = H.mkRegion(env, {
			id: 'd3', type: 'card-body', populate: 'capture3',
			declared: { contractVersion: '99', dataUrl: '/x', renderer: 'fieldGrid' }
		});
		R.initRegion(el);
		out.t2c_fallsThroughToIdentityOnly = ctx.declared.dataUrl === null;
		out.t2c_loggedMismatch = rec.errorsMatching('contract').length === 1;
	}

	// =================================================================================================================
	// Test 13 - ctx.fetchDeclared()'s handshake: success unwraps to the VALUES MAP; 404 -> kind:"empty"; any other
	// non-ok -> kind:"error"; a contractVersion mismatch (or unparseable/non-object body) -> kind:"error"; no
	// declared dataUrl -> resolves null (a legal state, not a throw).  Calling it fetches EVERY time (no caching,
	// per its own doc) - so the failure-kind cases below reuse the SAME already-populated region/ctx and issue a
	// fresh manual `ctx.fetchDeclared()` call each time, resolving each call's own fetch entry in turn.
	// =================================================================================================================
	{
		const { env, R } = load();
		let ctx = null;
		R.register('cap13', function (c) { ctx = c; });
		const el = H.mkRegion(env, {
			id: 't13', type: 'card-body', populate: 'cap13',
			declared: { dataUrl: '/api/widgets/1', renderer: 'fieldGrid' }
		});
		const calls = H.abortableFetch(env);
		R.initRegion(el); // the region's own R14a pre-fetch issues calls[0]
		out.t13_fetchUrl = calls[0] && calls[0].url;
		calls.resolve(0, { contractVersion: '1', fields: { status: 'ok', count: 3 } });
		await H.flush();
		out.t13_valuesMap = ctx.data; // the pre-fetch's own resolution, already unwrapped onto ctx.data

		// A manual second call: fetches again (calls[1]), and resolves to the values map again.
		const p1 = ctx.fetchDeclared();
		calls.resolve(1, { contractVersion: '1', fields: { status: 'ok', count: 3 } });
		out.t13_secondCallValuesMap = await p1;

		// 404 -> kind:"empty".
		const p2 = ctx.fetchDeclared();
		calls.resolve(2, { contractVersion: '1', fields: {} }, { status: 404 });
		try { await p2; out.t13_404kind = 'did-not-reject'; } catch (e) { out.t13_404kind = e.kind; }

		// Any other non-ok -> kind:"error".
		const p3 = ctx.fetchDeclared();
		calls.resolve(3, { contractVersion: '1', fields: {} }, { status: 500 });
		try { await p3; out.t13_500kind = 'did-not-reject'; } catch (e) { out.t13_500kind = e.kind; }

		// A contractVersion mismatch -> kind:"error".
		const p4 = ctx.fetchDeclared();
		calls.resolve(4, { contractVersion: '999', fields: { a: 1 } });
		try { await p4; out.t13_mismatchKind = 'did-not-reject'; } catch (e) { out.t13_mismatchKind = e.kind; }
	}
	{
		const { env, R } = load();
		let ctx = null;
		R.register('cap13e', function (c) { ctx = c; });
		const el = H.mkRegion(env, { id: 't13e', type: 'card-body', populate: 'cap13e' }); // no declared at all
		R.initRegion(el);
		out.t13_noDataUrlResolvesNull = await ctx.fetchDeclared();
	}

	// =================================================================================================================
	// Test 15a - R14a's pre-fetch matrix: `dataUrl` set -> the FIRST populate call pre-fetches and hands the
	// values map to the populate as `ctx.data` BEFORE the populate is invoked, for BOTH the reserved default and an
	// ordinary consumer-registered populator (this is test 11's non-privilege half: pre-fetch is keyed on the
	// descriptor, never on which populator resolved).  A SECOND populate call (a message-driven re-populate here)
	// does NOT pre-fetch: `ctx.data` is null and a populate that wants the payload calls `ctx.fetchDeclared()`
	// itself.
	// =================================================================================================================
	{
		const { env, R } = load();
		const seenCtxData = [];
		R.register('cap15a', function (c) { seenCtxData.push(c.data); });
		const el = H.mkRegion(env, {
			id: 't15a', type: 'card-body', populate: 'cap15a',
			declared: { dataUrl: '/api/widgets/5', renderer: 'fieldGrid' }
		});
		const calls = H.abortableFetch(env);
		R.initRegion(el);
		out.t15a_fetchCountBeforeFirstPopulate = calls.length; // populate has NOT run yet - fetch is in flight
		calls.resolve(0, { contractVersion: '1', fields: { status: 'ok' } });
		await H.flush();
		out.t15a_ctxDataOnFirstPopulate = seenCtxData[0];
		// A refresh-driven re-populate: ctx.data must be null this time, and NO second fetch is issued unless the
		// populate itself calls fetchDeclared() (this populate does not).
		el._juneauRegion.ctx.refresh();
		await H.flush();
		out.t15a_ctxDataOnRepopulate = seenCtxData[1];
		out.t15a_fetchCountAfterRepopulate = calls.length; // still 1: no second pre-fetch was issued
	}

	// =================================================================================================================
	// Test 16g - the reserved default's algorithm calls `fetchDeclared()` from EXACTLY ONE call site, guarded by the
	// nullish-coalesce over `ctx.data` - asserted at the SOURCE level (a privileged second path could not be a
	// second visible call site without giving itself away) and behaviorally (with `ctx.data` pre-set, `fetchDeclared`
	// must not be invoked a second time by the default itself).
	// =================================================================================================================
	{
		const src = fs.readFileSync(path.resolve(regionsJsPath), 'utf8');
		const bodyMatch = src.match(/function runDefaultPopulate\(ctx, container\) \{[\s\S]*?\n\t\}/);
		const body = bodyMatch ? bodyMatch[0] : '';
		const fetchCallSites = (body.match(/fetchDeclared\(\)/g) || []).length;
		out.t16g_oneFetchDeclaredCallSite = fetchCallSites;
		out.t16g_guardedByNullish = /ctx\.data\s*!=\s*null[\s\S]*?:\s*ctx\.fetchDeclared\(\)/.test(body)
			|| /ctx\.data\s*\?\?\s*[\s\S]*?fetchDeclared/.test(body);
	}
	{
		// Behavioral half: pre-set ctx.data (as R14a's own pre-fetch would), call the default directly, and count
		// how many times fetchDeclared would have been invoked - zero, because ctx.data is non-null.
		const { env, R } = load();
		let fetchDeclaredCalls = 0;
		const container = env.el('div');
		const ctx = {
			declared: { dataUrl: '/x', renderer: 'fieldGrid', fields: [{ data: 'a', label: 'A' }] },
			data: { a: 'preset-value' },
			fetchDeclared: function () { fetchDeclaredCalls++; return Promise.resolve({ a: 'from-fetch' }); },
			helpers: env.window.JuneauViews.helpers
		};
		await R.defaultPopulate(ctx, container);
		out.t16g_fetchDeclaredNotCalledWhenCtxDataSet = fetchDeclaredCalls === 0;
		out.t16g_paintedFromCtxData = container.textContent.indexOf('preset-value') >= 0;
	}

	// =================================================================================================================
	// Test 16h - the payload shape contract: `ctx.data`/`fetchDeclared()` resolve to a VALUES MAP, never the
	// `{contractVersion,fields}` envelope and never the `{status,ok,text}` transport triple.
	// =================================================================================================================
	{
		const { env, R } = load();
		let ctx = null;
		R.register('cap16h', function (c) { ctx = c; });
		const el = H.mkRegion(env, { id: 't16h', type: 'card-body', populate: 'cap16h',
			declared: { dataUrl: '/api/widgets/6', renderer: 'fieldGrid' } });
		const calls = H.abortableFetch(env);
		R.initRegion(el);
		calls.resolve(0, { contractVersion: '1', fields: { status: 'ok', count: 3 } });
		await H.flush();
		const keys = Object.keys(ctx.data).sort();
		out.t16h_ctxDataKeys = keys; // must be exactly ['count','status'] - no contractVersion/status(transport)/ok/text
		out.t16h_noEnvelopeLeak = keys.indexOf('contractVersion') < 0 && keys.indexOf('fields') < 0;
		out.t16h_noTransportLeak = keys.indexOf('ok') < 0 && keys.indexOf('text') < 0;
	}

	// =================================================================================================================
	// Test 14a - poll lifecycle (§8.3.1): one timer per region ever; clear-then-set on every settle; skipped (not
	// queued) while a populate is in flight; kept armed after an error settle.
	// =================================================================================================================
	{
		const { env, R, clock } = load();
		R.register('cap14a', function () {});
		const el = H.mkRegion(env, { id: 't14a', type: 'card-body', populate: 'cap14a',
			declared: { refreshMs: 30000 } });
		R.initRegion(el);
		await H.flush();
		// Drains the barrier's own 0ms backstop-check timer (armBackstop's fire(), unrelated to this region's
		// poll): it self-cancels as a no-op once the barrier has already lifted, but until it fires it is a
		// SECOND page-wide pending timer that would otherwise be miscounted as a second poll timer.
		clock.advance(0);
		await H.flush();
		out.t14a_oneTimerAfterInitial = clock.pending();
		// Ten re-populates (via ctx.refresh()) must still end with exactly one live timer.
		for (let i = 0; i < 10; i++) { el._juneauRegion.ctx.refresh(); await H.flush(); }
		out.t14a_oneTimerAfterTenRepopulates = clock.pending();
		// Advance to the tick: onPollTick re-populates and re-arms - still one timer.
		clock.advance(30000);
		await H.flush();
		out.t14a_oneTimerAfterTick = clock.pending();
	}
	{
		// Skip-while-in-flight (source-shape, not behavioral): `disposeInvocation`'s step 0 clears the CURRENT
		// pollTimer at the START of every `runPopulate` call regardless of trigger, so under this harness's
		// single-threaded, deterministic fake clock there is no way to force a tick to fire while its OWN
		// region's `pollTimer` is simultaneously live and `invoking`/`inFlight` is true - the state the guard
		// protects against cannot be manufactured from outside without reaching into the closure.  Asserted at
		// the source level instead: `onPollTick` checks the busy flags BEFORE calling `runPopulate`, and on the
		// busy branch it re-arms rather than dropping the region's poll forever.
		const src = fs.readFileSync(path.resolve(regionsJsPath), 'utf8');
		const bodyMatch = src.match(/function onPollTick\(region\) \{[\s\S]*?\n\t\}/);
		const body = bodyMatch ? bodyMatch[0] : '';
		out.t14b_checksBusyBeforeRepopulate = /if \(region\.invoking \|\| region\.inFlight\)/.test(body);
		out.t14b_reArmsOnBusySkip = /armPollTimer\(region\)/.test(body) && /return;/.test(body);
		out.t14b_dropsNotQueues = !/pendingReason/.test(body); // a skip must not fall through to the coalesce path
	}
	{
		// Kept-on-error: a populate that throws still gets its timer armed by finishInvocation's own settle.
		const { env, R, clock } = load();
		R.register('cap14c', function () { throw new Error('boom'); });
		const el = H.mkRegion(env, { id: 't14c', type: 'card-body', populate: 'cap14c',
			declared: { refreshMs: 10000 } });
		R.initRegion(el);
		await H.flush();
		clock.advance(0); // drains the barrier's own 0ms backstop-check timer - see the t14a comment
		await H.flush();
		out.t14c_state = el.getAttribute('data-juneau-region-state');
		out.t14c_timerArmedAfterError = clock.pending();
	}

	// =================================================================================================================
	// TEST 11 (L12 STOP GATE) - a consumer-registered populator that DELEGATES to `ctx.defaultPopulate` reproduces
	// the reserved default's own output byte-for-byte, proving there is no privileged path through the reserved
	// name: the exact identical algorithm is reachable through an ordinary registry entry.  A SECOND consumer
	// populator that re-implements the same algorithm by hand (reading ctx.data/declared/helpers itself, with NO
	// call to ctx.defaultPopulate at all) ALSO reproduces the same output - proving the algorithm has no hidden
	// framework-only step, because a consumer using only the documented ctx contract can reproduce it exactly.
	// =================================================================================================================
	{
		const descriptor = { dataUrl: '/api/widgets/7', renderer: 'fieldGrid',
			fields: [{ data: 'status', label: 'Status' }, { data: 'count', label: 'Count' }] };
		const body = { contractVersion: '1', fields: { status: 'ok', count: 7 } };

		// (a) the reserved default, reached via the ordinary "default" name.
		const w1 = load();
		const elA = H.mkRegion(w1.env, { id: 'ta', type: 'card-body', declared: descriptor }); // no populate attr -> "default"
		const callsA = H.abortableFetch(w1.env);
		w1.R.initRegion(elA);
		callsA.resolve(0, body);
		await H.flush();
		const htmlDefault = serializeChildren(elA);

		// (b) a consumer-registered populator that DELEGATES: `return ctx.defaultPopulate(ctx, container)`.
		const w2 = load();
		w2.R.register('delegate-to-default', function (ctx, container) { return ctx.defaultPopulate(ctx, container); });
		const elB = H.mkRegion(w2.env, { id: 'tb', type: 'card-body', populate: 'delegate-to-default', declared: descriptor });
		const callsB = H.abortableFetch(w2.env);
		w2.R.initRegion(elB);
		callsB.resolve(0, body);
		await H.flush();
		const htmlDelegate = serializeChildren(elB);

		// (c) a consumer populator that HAND-REPRODUCES the algorithm using only the documented ctx contract - no
		// call to ctx.defaultPopulate and no reach into framework internals.
		const w3 = load();
		w3.R.register('hand-rolled', function (ctx, container) {
			const payload = ctx.data != null ? Promise.resolve(ctx.data) : ctx.fetchDeclared();
			return payload.then(function (values) {
				container.appendChild(ctx.helpers[ctx.declared.renderer](ctx.declared.fields, { values: values }));
			});
		});
		const elC = H.mkRegion(w3.env, { id: 'tc', type: 'card-body', populate: 'hand-rolled', declared: descriptor });
		const callsC = H.abortableFetch(w3.env);
		w3.R.initRegion(elC);
		callsC.resolve(0, body);
		await H.flush();
		const htmlHandRolled = serializeChildren(elC);

		out.t11_defaultVsDelegateIdentical = htmlDefault === htmlDelegate && htmlDefault.length > 0;
		out.t11_defaultVsHandRolledIdentical = htmlDefault === htmlHandRolled;
		out.t11_sample = htmlDefault;
	}

	// =================================================================================================================
	// TEST 12 (L12 STOP GATE) - the reserved default runs correctly against a HAND-BUILT `ctx` with NO region, NO
	// mintRegion/initRegion, and NO DOM ancestry for its container: it reads only the portable ctx contract
	// (`declared`, `data`, `fetchDeclared`, `helpers`) and never reaches into framework internals (no `region`
	// object, no `el`, no `_juneauRegion`, nothing keyed off liveRegions).
	// =================================================================================================================
	{
		const { env, R } = load();
		const detachedContainer = env.el('div'); // deliberately never appended to env.body/document
		let fetchDeclaredCalled = 0;
		const handBuiltCtx = {
			// Only the members the default's own doc says it reads - nothing else.
			declared: { dataUrl: '/unused', renderer: 'fieldGrid', fields: [{ data: 'status', label: 'Status' }] },
			data: null,
			fetchDeclared: function () { fetchDeclaredCalled++; return Promise.resolve({ status: 'from-hand-built-fetch' }); },
			helpers: env.window.JuneauViews.helpers
		};
		out.t12_containerConnectedBeforeCall = env.document.contains(detachedContainer);
		await R.defaultPopulate(handBuiltCtx, detachedContainer);
		out.t12_fetchDeclaredCalledOnce = fetchDeclaredCalled === 1;
		out.t12_painted = detachedContainer.textContent.indexOf('from-hand-built-fetch') >= 0;
		out.t12_containerStillDetached = !env.document.contains(detachedContainer);
		out.t12_html = serializeChildren(detachedContainer);
	}

	// =================================================================================================================
	// Test 16a (parity) - serializeParams: the JS twin's output for the closed rule set (design §8.2.1), for direct
	// comparison against RegionDef.serializeParams's identical golden cases (RegionDef_Test.java g10-g16) from the
	// Java side.  g17 (nested map) is DELIBERATELY excluded from the parity claim: the server throws
	// IllegalArgumentException there (RegionDef_Test#g17), while the client silently drops a nested value in
	// defensive depth (documented in juneau-regions.js's own serializeParams doc) since a validated descriptor can
	// never reach the client carrying one - so the two sides' behavior diverges ONLY on an input that cannot occur
	// on the wire, and that divergence is intentional, not a parity gap.
	// =================================================================================================================
	{
		const { R } = load();
		out.t16a_null = R.serializeParams(null);
		out.t16a_nullValueOmitsKey = R.serializeParams({ a: '1', b: null });
		out.t16a_emptyStringIsKeyEquals = R.serializeParams({ a: '' });
		out.t16a_booleanAndNumber = R.serializeParams({ on: true, n: 42 });
		out.t16a_collectionRepeatsKey = R.serializeParams({ tag: ['a', 'b'] });
		out.t16a_keyOrderIsIterationOrder = R.serializeParams({ z: '1', a: '2' });
		out.t16a_spaceIsPercent20 = R.serializeParams({ q: 'a b' });
		// The documented divergence: a nested map does not throw client-side, it is dropped.
		out.t16a_nestedMapDroppedNotThrown = R.serializeParams({ a: { nested: 1 } });
	}

	process.stdout.write(JSON.stringify(out));
})().catch(function (e) {
	process.stderr.write(String(e && e.stack ? e.stack : e));
	process.exit(1);
});
