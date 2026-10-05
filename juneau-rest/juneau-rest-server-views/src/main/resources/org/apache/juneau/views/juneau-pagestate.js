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
 * juneau-pagestate.js - the general browser page-state store (design section 6.2).
 *
 * A single, dependency-free key/value store for PAGE state - not View-Settings only.  Its first user is the
 * datatable View Settings dialog, but selected tab, selected probe, table page index, and any other page
 * preference share the same store under distinct key namespaces.  It attaches to window.JuneauViews.pageState
 * (mirroring window.JuneauViews.icons / .search), so it is loaded as a plain <script> BEFORE juneau-views.js and
 * juneau-config.js, which consume it.
 *
 * Keying (design section 6.2):
 *   - PER-TABLE keys, so two tables on one page never clobber each other:  pageState.table(tableKey).set(name, v)
 *   - PAGE-LEVEL keys for non-table state, under their own namespace:      pageState.page(namespace).set(name, v)
 *
 * Backing store:
 *   - Default is window.localStorage.
 *   - Replaceable: an app may install an alternate implementation via pageState.useStore({getItem,setItem,removeItem}).
 *   - Storage-blocked (private mode, disabled storage, quota): every call is guarded, so a blocked store degrades
 *     to a silent no-op (get returns null, set/remove do nothing) rather than throwing at the caller.
 */
(function () {
	'use strict';

	const NS = window.JuneauViews = window.JuneauViews || {};
	const PREFIX = 'juneau.pagestate.';

	// A store used when browser storage is blocked/absent, so callers never throw.
	const NOOP_STORE = {
		getItem: function () { return null; },
		setItem: function () {},
		removeItem: function () {}
	};

	// The default backing store is window.localStorage when reachable AND writable; some privacy modes throw on the
	// property access itself, others only on setItem - both fall back to the no-op store (design section 6.2).
	function defaultStore() {
		try {
			const ls = window.localStorage;
			if (! ls) return NOOP_STORE;
			const probe = PREFIX + '__probe__';
			ls.setItem(probe, '1');   // a blocked-storage browser throws here, not on the property read above.
			ls.removeItem(probe);
			return ls;
		} catch (e) {
			// Storage is blocked: degrade to the no-op store (the error itself carries nothing actionable).
			return NOOP_STORE;
		}
	}

	let store = defaultStore();

	// Every store call is guarded: a store that starts fine but later throws (quota, eviction) degrades to a no-op
	// for THAT call rather than surfacing to the caller.
	function rawGet(key) { try { return store.getItem(key); } catch (e) { return null; } }
	function rawSet(key, value) { try { store.setItem(key, value); } catch (e) { /* no-op */ } }
	function rawRemove(key) { try { store.removeItem(key); } catch (e) { /* no-op */ } }

	/** Reads and JSON-decodes `key`; a missing key OR an undecodable value both read as null. */
	function get(key) {
		const raw = rawGet(key);
		if (raw == null) return null;
		try { return JSON.parse(raw); } catch (e) { return null; }
	}

	/** JSON-encodes and writes `value` under `key`; an `undefined` value (or an unserializable one) clears it. */
	function set(key, value) {
		if (value === undefined) { rawRemove(key); return; }
		let enc;
		try { enc = JSON.stringify(value); } catch (e) { return; }
		rawSet(key, enc);
	}

	function remove(key) { rawRemove(key); }

	function tableKeyFor(tableKey, name) { return PREFIX + 'table.' + tableKey + '.' + name; }
	function pageKeyFor(namespace, name) { return PREFIX + 'page.' + namespace + '.' + name; }

	/** A per-table namespace: two tables with distinct keys never clobber each other's stored state. */
	function tableScope(tableKey) {
		return {
			get: function (name) { return get(tableKeyFor(tableKey, name)); },
			set: function (name, value) { set(tableKeyFor(tableKey, name), value); },
			remove: function (name) { remove(tableKeyFor(tableKey, name)); }
		};
	}

	/** A page-level namespace for non-table state (selected tab, selected probe, etc.). */
	function pageScope(namespace) {
		return {
			get: function (name) { return get(pageKeyFor(namespace, name)); },
			set: function (name, value) { set(pageKeyFor(namespace, name), value); },
			remove: function (name) { remove(pageKeyFor(namespace, name)); }
		};
	}

	NS.pageState = {
		get: get,
		set: set,
		remove: remove,
		table: tableScope,
		page: pageScope,
		// Install an alternate backing store (design section 6.2 "replaceable"): any {getItem,setItem,removeItem}.
		useStore: function (impl) { store = impl || NOOP_STORE; },
		// Re-resolve the default (localStorage-or-no-op) store; primarily for tests after a useStore() swap.
		resetStore: function () { store = defaultStore(); },
		// The live backing store - exposed for tests/introspection, not part of the app-facing contract.
		_store: function () { return store; }
	};
})();
