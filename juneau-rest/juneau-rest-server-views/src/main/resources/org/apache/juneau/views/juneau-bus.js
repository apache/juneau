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
 * juneau-bus.js - dependency-free, in-page publish/subscribe message bus for the Apache Juneau console and rich-view
 * toolkit (message bus addendum, spec sections 3, 6.3 and 7).  Publishes window.JuneauViews.bus and touches
 * nothing else on window.JuneauViews.  MUST load before juneau-views.js, juneau-regions.js and juneau-console.js.
 *
 * Topics are "family" or "family:key".  State topics keep their last value and replay it to new subscribers.
 * Delivery is synchronous, queued (never re-entrant), breadth-first, and capped per drain.  Payloads are JSON only and
 * are delivered as one deep-frozen copy.
 */
(function () {
	'use strict';

	const NS = window.JuneauViews = window.JuneauViews || {};
	if (NS.bus && NS.bus.CONTRACT_VERSION) return;   // loaded twice: keep the first instance and its retained state

	/** Bus API contract version; bumps only on a breaking change to the public surface below. */
	const CONTRACT_VERSION = '1';
	/** The reserved families (spec 3.1); a bare family outside this list is E-JS-40. */
	const FRAMEWORK_FAMILIES = Object.freeze(['card', 'selection', 'filter', 'redraw', 'detail', 'bulk', 'cmd', 'probe',
		'job', 'badge', 'bridge']);
	/** Kind of each framework family (spec 3.2): state is retained and single-owner, event is owner-only, command is open. */
	const FAMILY_KIND = Object.freeze({
		card: 'state', selection: 'state', filter: 'state', probe: 'state', job: 'state', badge: 'state', bridge: 'state',
		redraw: 'event', detail: 'event', bulk: 'event',
		cmd: 'command'
	});
	/** The only framework payload schemaVersion this build understands (spec 3.4, E-JS-43). */
	const FRAMEWORK_SCHEMA_VERSION = 1;
	/** Publishes delivered in one drain before E-JS-44 (=== the region bus's BUS_QUEUE_DEPTH_CAP). */
	const DRAIN_CAP = 64;
	/** Entries kept by history(). */
	const HISTORY_CAP = 256;
	/** Hops kept for the E-JS-44 cycle report. */
	const CYCLE_TRACE_CAP = 256;
	/** `meta.from` of framework-originated publishes (the region runtime's FRAMEWORK_SENDER_KEY). */
	const FRAMEWORK_SENDER_KEY = 'juneau:framework';
	/** Canonical-JSON characters a trace line shows of a payload. */
	const TRACE_SUMMARY_MAX = 120;
	const TRACE_FLAG = 'juneau-bus-trace';
	const TRACE_FLAG_RE = /[?&]juneau-bus-trace(=|&|$)/;
	const NAME_RE = /^[a-z][a-z0-9-]{0,31}$/;
	const KEY_RE = /^[A-Za-z0-9_.-]{1,128}$/;
	const ARRAY_INDEX_RE = /^(0|[1-9][0-9]*)$/;

	/** Injectable clock: harnesses replace members of `NS.bus.config.timers`, so always read through `config`. */
	const config = {
		timers: {
			setTimeout: function (fn, ms) { return setTimeout(fn, ms); },
			clearTimeout: function (id) { clearTimeout(id); },
			now: function () { return Date.now(); },
			random: function () { return Math.random(); }
		}
	};

	function now() { return config.timers.now(); }

	/** `%s` substitution, the message convention every bus error uses. */
	function fmt(template) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return template.replace(/%s/g, function () { return String(args[i++]); });
	}

	function describeError(e) { return e && e.message ? e.message : String(e); }

	/**
	 * A bus failure.  `code` is the spec's 'E-JS-4x' / 'E-JS-5x'; `detail` always has `topic` when there is one and,
	 * for reported errors, `paintOn`: the owner id whose card shows the error, or null for console-only.
	 */
	class BusError extends Error {
		constructor(code, message, detail) {
			super(message);
			this.name = 'BusError';
			this.code = code;
			this.detail = detail || {};
		}
	}

	// ---- error reporting -----------------------------------------------------------------------------------------

	const errorSinks = [];

	/** Logs a reported (not thrown) error and hands it to every sink; a sink that throws is logged, never propagated. */
	function reportError(err) {
		console.error('[juneau-bus] ' + err.code + ': ' + err.message);
		errorSinks.slice().forEach(function (fn) {
			try { fn(err); } catch (e) { console.error('[juneau-bus] error sink threw: ' + describeError(e)); }
		});
	}

	function onError(fn) {
		if (typeof fn !== 'function') throw new TypeError('onError(fn): fn must be a function');
		errorSinks.push(fn);
		let registered = true;
		return function () {
			if (!registered) return;
			registered = false;
			const i = errorSinks.indexOf(fn);
			if (i >= 0) errorSinks.splice(i, 1);
		};
	}

	// ---- topic grammar (spec 3.1) ----------------------------------------------------------------------------------

	function badTopic(topic, why) {
		return new BusError('E-JS-40', fmt("invalid topic '%s': %s", topic, why), { topic: topic });
	}

	/**
	 * Parses `family[:key]`.  `{pattern: true}` also accepts `family:*` (declarations only).
	 *
	 * @example
	 * JuneauViews.bus.util.parseTopic('ssc.focus:triage');  // {family:'ssc.focus', key:'triage', framework:false, kind:'custom'}
	 * JuneauViews.bus.util.parseTopic('focus');             // throws BusError E-JS-40 ... (did you mean 'app.focus'?)
	 */
	function parseTopic(topic, opts) {
		if (typeof topic !== 'string' || topic === '') throw badTopic(topic, 'a topic is a non-empty string');
		const pattern = !!(opts && opts.pattern);
		const colon = topic.indexOf(':');
		const family = colon < 0 ? topic : topic.slice(0, colon);
		const key = colon < 0 ? null : topic.slice(colon + 1);
		const framework = FRAMEWORK_FAMILIES.indexOf(family) >= 0;
		if (!framework) {
			const dot = family.indexOf('.');
			if (dot < 0) {
				throw badTopic(topic, NAME_RE.test(family)
					? fmt("'%s' is not a framework family; a custom topic is 'ns.name' (did you mean 'app.%s'?)", family, family)
					: "a family is a framework family or 'ns.name'");
			}
			if (!NAME_RE.test(family.slice(0, dot)) || !NAME_RE.test(family.slice(dot + 1)))
				throw badTopic(topic, 'ns and name must each match ^[a-z][a-z0-9-]{0,31}$');
		}
		if (key === null) {
			if (framework) throw badTopic(topic, fmt("framework family '%s' needs a key ('%s:<id>')", family, family));
		} else if (key === '*') {
			if (!pattern) throw badTopic(topic, "'family:*' is allowed only in a declaration; publishes and subscriptions are concrete");
		} else if (!KEY_RE.test(key)) {
			throw badTopic(topic, 'the key must match ^[A-Za-z0-9_.-]{1,128}$');
		}
		return { family: family, key: key, framework: framework, kind: framework ? FAMILY_KIND[family] : 'custom' };
	}

	/** True when `topic` is `pattern`, or `pattern` is `family:*` and `topic` is a keyed topic of that family. */
	function topicMatches(pattern, topic) {
		if (pattern === topic) return true;
		if (typeof pattern !== 'string' || typeof topic !== 'string' || pattern.slice(-2) !== ':*') return false;
		const prefix = pattern.slice(0, -1);
		return topic.length > prefix.length && topic.slice(0, prefix.length) === prefix;
	}

	// ---- payloads (spec 3.4) ---------------------------------------------------------------------------------------

	/** Sorted-key JSON; only ever called on values that passed jsonProblem. */
	function canonicalJson(v) {
		if (v === null || typeof v !== 'object') return JSON.stringify(v);
		if (Array.isArray(v)) return '[' + v.map(canonicalJson).join(',') + ']';
		return '{' + Object.keys(v).sort().map(function (k) { return JSON.stringify(k) + ':' + canonicalJson(v[k]); }).join(',') + '}';
	}

	/** Why `v` is not plain JSON (`'payload.a[0] is a function'`), or null.  `stack` holds the current ancestors. */
	function jsonProblem(v, path, stack) {
		if (v === null) return null;
		const t = typeof v;
		if (t === 'string' || t === 'boolean') return null;
		if (t === 'number') return isFinite(v) ? null : path + ' is ' + v;
		if (t !== 'object') return path + ' is ' + (t === 'undefined' ? 'undefined' : 'a ' + t);
		if (typeof v.nodeType === 'number' && (typeof v.nodeName === 'string' || typeof v.tagName === 'string'))
			return path + ' is a DOM node';
		if (stack.indexOf(v) >= 0) return path + ' is a cycle';
		stack.push(v);
		try {
			if (Array.isArray(v)) {
				for (let i = 0; i < v.length; i++) {
					const p = jsonProblem(v[i], path + '[' + i + ']', stack);
					if (p) return p;
				}
				return null;
			}
			const keys = Object.keys(v);
			for (let i = 0; i < keys.length; i++) {
				const p = jsonProblem(v[keys[i]], path + '.' + keys[i], stack);
				if (p) return p;
			}
			return null;
		} finally {
			stack.pop();
		}
	}

	function deepFreeze(v) {
		if (v !== null && typeof v === 'object' && !Object.isFrozen(v)) {
			Object.freeze(v);
			Object.keys(v).forEach(function (k) { deepFreeze(v[k]); });
		}
		return v;
	}

	/**
	 * Dotted-path lookup (`'ids.0'`, `'a.b.2.c'`); array segments must be decimal indexes.  Undefined when any segment
	 * is missing.  This is the `map` path language of declared subscriptions (spec 5.3).
	 *
	 * @example
	 * JuneauViews.bus.util.resolvePath({ids: ['c-17']}, 'ids.0');   // 'c-17'
	 */
	function resolvePath(value, path) {
		if (typeof path !== 'string' || path === '') return undefined;
		const parts = path.split('.');
		let v = value;
		for (let i = 0; i < parts.length; i++) {
			const p = parts[i];
			if (p === '' || v === null || typeof v !== 'object') return undefined;
			if (Array.isArray(v) && !ARRAY_INDEX_RE.test(p)) return undefined;
			if (!Object.hasOwn(v, p)) return undefined;
			v = v[p];
		}
		return v;
	}

	// ---- declarations, retention and ownership ----------------------------------------------------------------------

	/** topic or 'family:*' -> {retain, by: string[]} */
	const declarations = new Map();
	/** framework state/event topic -> owner id */
	const claims = new Map();
	/** topic -> Array<{fn, owner, echo, live}> */
	const subscriptions = new Map();
	/** retained topic -> its last accepted message */
	const retainedValues = new Map();
	/** every topic published, subscribed, declared or claimed, for topics() */
	const knownTopics = new Set();

	function declaredEntry(topic) {
		if (declarations.has(topic)) return declarations.get(topic);
		const colon = topic.indexOf(':');
		return colon < 0 ? null : declarations.get(topic.slice(0, colon) + ':*') || null;
	}

	function retains(topic, parsed) {
		if (parsed.framework) return parsed.kind === 'state';
		const d = declaredEntry(topic);
		return !!(d && d.retain);
	}

	function declare(topic, opts) {
		const parsed = parseTopic(topic, { pattern: true });
		if (!opts || typeof opts.retain !== 'boolean')
			throw new TypeError(fmt("declare('%s', opts): opts.retain must be true or false", topic));
		let was;
		if (parsed.framework) {
			was = parsed.kind === 'state';
		} else {
			declarations.forEach(function (d, t) {
				if (was === undefined && (t === topic || topicMatches(t, topic) || topicMatches(topic, t))) was = d.retain;
			});
		}
		if (was !== undefined && was !== opts.retain)
			throw new BusError('E-JS-50', fmt("topic '%s' re-declared with retain=%s (was %s)", topic, opts.retain, was), { topic: topic });
		const by = typeof opts.by === 'string' && opts.by ? opts.by : 'page';
		const d = declarations.get(topic) || { retain: opts.retain, by: [] };
		if (d.by.indexOf(by) < 0) d.by.push(by);
		declarations.set(topic, d);
		knownTopics.add(topic);
	}

	function ownedKind(parsed) { return parsed.framework && parsed.kind !== 'command'; }

	function ownershipError(topic, parsed, who) {
		const owner = claims.get(topic) || parsed.key;
		return new BusError('E-JS-49',
			fmt("'%s' is owned by '%s'; '%s' may not publish or clear it (publish cmd:%s instead)", topic, owner, who, parsed.key),
			{ topic: topic, owner: owner, from: who, paintOn: who === 'page' ? null : who });
	}

	/** Null when `ownerId` may write `topic` (claiming it on an owner-bound first write), else the E-JS-49 error. */
	function writeDenied(topic, parsed, ownerId, who) {
		if (!ownedKind(parsed)) return null;
		const current = claims.get(topic);
		if (ownerId && (current === undefined || current === ownerId)) {
			claims.set(topic, ownerId);
			return null;
		}
		return ownershipError(topic, parsed, who);
	}

	function requireOwnerId(id) {
		if (typeof id !== 'string' || id === '') throw new TypeError('a bus owner id is a non-empty string');
	}

	function claim(topic, ownerId) {
		const parsed = parseTopic(topic);
		requireOwnerId(ownerId);
		if (!ownedKind(parsed)) throw new TypeError(fmt("claim('%s'): only framework state and event topics have an owner", topic));
		const current = claims.get(topic);
		if (current !== undefined && current !== ownerId) throw ownershipError(topic, parsed, ownerId);
		claims.set(topic, ownerId);
		knownTopics.add(topic);
		return true;
	}

	// ---- delivery (spec 7.1) ---------------------------------------------------------------------------------------

	const queue = [];
	/** {from, topic} of every accepted publish in the current drain, for the E-JS-44 cycle text */
	const hops = [];
	const historyRing = [];
	let draining = false;
	let drainCount = 0;
	let drainOverflowed = false;
	/** the message currently being fanned out (trace depth) */
	let delivering = null;
	let seq = 0;
	let tracing = initialTrace();

	function liveSubscriberCount(topic) {
		const list = subscriptions.get(topic);
		return list ? list.length : 0;
	}

	function newMessage(topic, payload, meta, ownerId, retain) {
		return { topic: topic, payload: payload, meta: Object.freeze(meta), ownerId: ownerId, retain: retain,
			canon: canonicalJson(payload), depth: delivering ? delivering.depth + 1 : 0 };
	}

	function writerFrom(ownerId, opts) {
		return opts && typeof opts.from === 'string' && opts.from ? opts.from : (ownerId || 'page');
	}

	function baseMeta(topic, from, opts) {
		const meta = { topic: topic, from: from, seq: ++seq, ts: now(), retained: false };
		if (from === 'server' && opts && typeof opts.bridge === 'string') meta.bridge = opts.bridge;
		return meta;
	}

	function publishAs(ownerId, topic, payload, opts) {
		const parsed = parseTopic(topic);
		const problem = jsonProblem(payload, 'payload', []);
		if (problem)
			throw new BusError('E-JS-42', fmt("payload for '%s' is not JSON-serializable: %s", topic, problem), { topic: topic });
		if (parsed.framework && !(payload !== null && typeof payload === 'object' && payload.schemaVersion === FRAMEWORK_SCHEMA_VERSION)) {
			const got = payload !== null && typeof payload === 'object'
				? JSON.stringify(payload.schemaVersion)
				: 'a ' + (payload === null ? 'null' : typeof payload) + ' payload';
			throw new BusError('E-JS-43', fmt("framework topic '%s' needs schemaVersion %s, got %s", topic, FRAMEWORK_SCHEMA_VERSION, got),
				{ topic: topic });
		}
		const from = writerFrom(ownerId, opts);
		const denied = writeDenied(topic, parsed, ownerId, ownerId || from);
		if (denied) {
			reportError(denied);
			return false;
		}
		const frozen = deepFreeze(JSON.parse(JSON.stringify(payload)));
		return accept(newMessage(topic, frozen, baseMeta(topic, from, opts), ownerId, retains(topic, parsed)));
	}

	function clearAs(ownerId, topic, opts) {
		const parsed = parseTopic(topic);
		const who = ownerId || 'page';
		if (ownedKind(parsed)) {
			const current = claims.get(topic);
			if (!ownerId || (current !== undefined && current !== ownerId)) {
				reportError(ownershipError(topic, parsed, who));
				return false;
			}
		}
		if (!retainedValues.has(topic)) return false;
		const meta = baseMeta(topic, writerFrom(ownerId, opts), opts);
		meta.cleared = true;
		return accept(newMessage(topic, null, meta, ownerId, true));
	}

	function accept(msg) {
		knownTopics.add(msg.topic);
		if (msg.retain && !msg.meta.cleared) {
			const prev = retainedValues.get(msg.topic);
			if (prev && prev.canon === msg.canon) {
				record(msg, 'suppressed');
				return false;
			}
		}
		// A clear is never droppable: a retained value must not outlive its owner because the drain was overflowed.
		if (msg.meta.cleared) retainedValues.delete(msg.topic);
		if (drainOverflowed) {
			record(msg, 'dropped');
			return false;
		}
		hops.push({ from: msg.meta.from, topic: msg.topic });
		if (hops.length > CYCLE_TRACE_CAP) hops.shift();
		if (draining && drainCount >= DRAIN_CAP) {
			overflow(msg);
			return false;
		}
		if (msg.retain && !msg.meta.cleared) {
			msg.prev = retainedValues.get(msg.topic);
			retainedValues.set(msg.topic, msg);
		}
		record(msg, null);
		queue.push(msg);
		drainCount++;
		drain();
		return true;
	}

	function overflow(msg) {
		// Roll back the retained values of the purged publishes (newest first) so get() and distinct-until-changed
		// agree with what subscribers saw, then delete every topic with a purged clear (or whose clear is `msg`
		// itself): a clear is never undone.  A subscriber added mid-drain may already have been shown a replayed value
		// that this rolls back; that is accepted.
		const cleared = new Set();
		if (msg.meta.cleared) cleared.add(msg.topic);
		queue.slice().reverse().forEach(function (q) {
			markDropped(q);
			if (q.meta.cleared) {
				cleared.add(q.topic);
			} else if (q.retain) {
				if (q.prev === undefined) retainedValues.delete(q.topic);
				else retainedValues.set(q.topic, q.prev);
			}
		});
		cleared.forEach(function (t) { retainedValues.delete(t); });
		queue.length = 0;
		drainOverflowed = true;
		record(msg, 'dropped');
		reportError(new BusError('E-JS-44', fmt('bus drain cap (%s) exceeded; likely cycle: %s', DRAIN_CAP, likelyCycle()), {
			topic: msg.topic, from: msg.meta.from, paintOn: msg.ownerId,
			hops: hops.map(function (h) { return h.from + ' -> ' + h.topic; })
		}));
	}

	/** Re-records an already-accepted (now purged) message in the history ring as dropped. */
	function markDropped(msg) {
		for (let i = historyRing.length - 1; i >= 0; i--) {
			const e = historyRing[i];
			if (e.seq === msg.meta.seq) {
				historyRing[i] = Object.freeze(Object.assign({}, e, { dropped: true }));
				return;
			}
		}
	}

	function renderHops(list) {
		return list.map(function (h) { return h.from + ' -> ' + h.topic; }).join(' -> ');
	}

	/** The shortest repeating suffix of the hop ring, closed back to its first sender; else the last 8 hops. */
	function likelyCycle() {
		const n = hops.length;
		for (let p = 1; p * 2 <= n; p++) {
			let repeats = true;
			for (let i = 1; i <= p && repeats; i++)
				repeats = hops[n - i].from === hops[n - i - p].from && hops[n - i].topic === hops[n - i - p].topic;
			if (repeats) return renderHops(hops.slice(n - p)) + ' -> ' + hops[n - p].from;
		}
		return renderHops(hops.slice(Math.max(0, n - 8)));
	}

	function drain() {
		if (draining) return;
		draining = true;
		try {
			while (queue.length) {
				const msg = queue.shift();
				const list = subscriptions.get(msg.topic);
				if (!list) continue;
				delivering = msg;
				list.slice().forEach(function (sub) { deliverTo(sub, msg); });
			}
		} finally {
			draining = false;
			delivering = null;
			drainCount = 0;
			drainOverflowed = false;
			hops.length = 0;
			queue.length = 0;
		}
	}

	function deliverTo(sub, msg) {
		if (!sub.live) return;
		if (msg.meta.seq <= sub.after) return;    // already seen through the retained replay
		if (sub.owner && sub.owner === msg.ownerId && !sub.echo) return;    // self-echo (spec 3.5)
		try {
			sub.fn(msg.payload, msg.meta);
		} catch (e) {
			reportError(new BusError('E-JS-45',
				fmt("subscriber for '%s' (owner '%s') threw: %s", msg.topic, sub.owner || 'page', describeError(e)),
				{ topic: msg.topic, owner: sub.owner || 'page', from: msg.meta.from, paintOn: sub.owner || null, cause: e }));
		}
	}

	function subscribeAs(ownerId, topic, fn, opts) {
		parseTopic(topic);
		if (typeof fn !== 'function') throw new TypeError(fmt("subscribe('%s', fn): fn must be a function", topic));
		const sub = { fn: fn, owner: ownerId, echo: !!(opts && opts.echo), live: true, after: 0 };
		let list = subscriptions.get(topic);
		if (!list) {
			list = [];
			subscriptions.set(topic, list);
		}
		list.push(sub);
		knownTopics.add(topic);
		const stored = retainedValues.get(topic);
		if (stored && !(opts && opts.retained === false)) {
			deliverTo(sub, { topic: topic, payload: stored.payload, ownerId: stored.ownerId,
				meta: Object.freeze(Object.assign({}, stored.meta, { retained: true })) });
			sub.after = stored.meta.seq;    // skip queued copies of this value and anything older
		}
		return function unsubscribe() {
			if (!sub.live) return;
			sub.live = false;
			const l = subscriptions.get(topic);
			const i = l ? l.indexOf(sub) : -1;
			if (i >= 0) l.splice(i, 1);
			if (l && !l.length) subscriptions.delete(topic);
		};
	}

	// ---- history and the dev trace (spec 7.3) ----------------------------------------------------------------------

	function initialTrace() {
		try {
			const search = window.location && typeof window.location.search === 'string' ? window.location.search : '';
			if (TRACE_FLAG_RE.test(search)) return true;
		} catch (e) {
			// no usable location: fall through to storage
		}
		try {
			return !!(window.localStorage && window.localStorage.getItem(TRACE_FLAG) === '1');
		} catch (e) {
			return false;    // blocked storage is not an error
		}
	}

	function trace(on) {
		const was = tracing;
		if (arguments.length) tracing = !!on;
		return was;
	}

	function withoutRows(v) {
		if (v === null || typeof v !== 'object') return v;
		if (Array.isArray(v)) return v.map(withoutRows);
		const o = {};
		Object.keys(v).forEach(function (k) {
			o[k] = k === 'rows' && Array.isArray(v[k]) ? '<' + v[k].length + ' rows>' : withoutRows(v[k]);
		});
		return o;
	}

	function summarize(payload) {
		const s = canonicalJson(withoutRows(payload));
		return s.length > TRACE_SUMMARY_MAX ? s.slice(0, TRACE_SUMMARY_MAX) + '…' : s;
	}

	function record(msg, flag) {
		const entry = Object.freeze({
			seq: msg.meta.seq, ts: msg.meta.ts, topic: msg.topic, from: msg.meta.from,
			subscribers: liveSubscriberCount(msg.topic), suppressed: flag === 'suppressed', dropped: flag === 'dropped',
			size: msg.canon.length
		});
		historyRing.push(entry);
		if (historyRing.length > HISTORY_CAP) historyRing.shift();
		if (tracing) traceLine(msg, entry);
	}

	function traceLine(msg, e) {
		let line = '[bus] ' + '  '.repeat(msg.depth) + '#' + e.seq + ' ' + e.topic + '  from=' + e.from;
		if (e.suppressed) line += '  suppressed (unchanged)';
		else if (e.dropped) line += '  dropped (drain cap)';
		else line += '  subs=' + e.subscribers + (msg.retain ? ' retained' : '') + (msg.meta.cleared ? ' cleared' : '') + '  ' + summarize(msg.payload);
		console.debug(line);
	}

	function topics() {
		return Array.from(knownTopics).sort().map(function (t) {
			const parsed = parseTopic(t, { pattern: true });
			const d = declarations.get(t) || (parsed.key === '*' ? null : declaredEntry(t));
			return Object.freeze({
				topic: t,
				kind: parsed.kind,
				retain: parsed.framework ? parsed.kind === 'state' : !!(d && d.retain),
				declared: parsed.framework || !!d,
				retained: retainedValues.has(t),
				subscribers: liveSubscriberCount(t),
				owner: claims.get(t) || null,
				declaredBy: d ? d.by.slice() : []
			});
		});
	}

	// ---- owner-bound facade ------------------------------------------------------------------------------------------

	/**
	 * An owner-bound view of the bus: `meta.from` defaults to `ownerId`, the owner never hears its own publishes
	 * (unless `{echo:true}`), and `dispose()` unsubscribes everything and clears every topic the owner holds.
	 *
	 * @example
	 * var me = JuneauViews.bus.owner('changes');
	 * me.publish('selection:changes', {schemaVersion: 1, ids: ['c-17'], count: 1});   // claims selection:changes
	 * me.subscribe('cmd:changes', function (cmd) { ... });
	 * me.dispose();   // late subscribers to selection:changes now see nothing
	 */
	function owner(ownerId) {
		requireOwnerId(ownerId);
		const offs = [];
		let disposed = false;
		function ignored(what) {
			console.warn(fmt("[juneau-bus] owner '%s' is disposed; %s ignored", ownerId, what));
			return false;
		}
		return Object.freeze({
			id: ownerId,
			publish: function (topic, payload, opts) { return disposed ? ignored('publish') : publishAs(ownerId, topic, payload, opts); },
			subscribe: function (topic, fn, opts) {
				if (disposed) {
					ignored('subscribe');
					return function () {};
				}
				const off = subscribeAs(ownerId, topic, fn, opts);
				offs.push(off);
				return function unsubscribe() {
					// Forget the handle too, so an owner that subscribes once per re-populate (a region) does not
					// accumulate dead handles for the life of the page.
					const i = offs.indexOf(off);
					if (i >= 0) offs.splice(i, 1);
					off();
				};
			},
			clear: function (topic, opts) { return disposed ? ignored('clear') : clearAs(ownerId, topic, opts); },
			claim: function (topic) { return disposed ? ignored('claim') : claim(topic, ownerId); },
			dispose: function () {
				if (disposed) return;
				disposed = true;
				offs.splice(0).forEach(function (off) { off(); });
				const mine = [];
				claims.forEach(function (o, t) { if (o === ownerId) mine.push(t); });
				mine.forEach(function (t) {
					clearAs(ownerId, t);
					claims.delete(t);
				});
			}
		});
	}

	/**
	 * The page's in-memory publish/subscribe bus. Topics are "family" or "family:key" (spec 3.1); state topics keep
	 * their last value and replay it to new subscribers; delivery is synchronous, breadth-first, and loop-guarded.
	 *
	 * @example
	 * // Page script: follow a table's selection (retained, so this sees the current value immediately).
	 * var off = JuneauViews.bus.subscribe('selection:changes', function (sel, meta) {
	 *   showCount(sel.count + ' selected');
	 * });
	 *
	 * @example
	 * // Page script: drive a table without touching its DOM.
	 * JuneauViews.bus.publish('cmd:changes', {schemaVersion: 1, op: 'set-filter', options: {mine: true}});
	 *
	 * @example
	 * // A custom topic must be declared (in the contract `topics`, or here) before DOMContentLoaded.
	 * JuneauViews.bus.declare('app.region-picked', {retain: true});
	 * JuneauViews.bus.publish('app.region-picked', {region: 'east'});
	 *
	 * @example
	 * // Support: what is wired, and what just happened.
	 * console.table(JuneauViews.bus.topics());
	 * console.table(JuneauViews.bus.history());
	 * JuneauViews.bus.trace(true);
	 */
	NS.bus = {
		publish: function (topic, payload, opts) { return publishAs(null, topic, payload, opts); },
		subscribe: function (topic, fn, opts) { return subscribeAs(null, topic, fn, opts); },
		get: function (topic) {
			parseTopic(topic);
			const m = retainedValues.get(topic);
			return m ? m.payload : undefined;
		},
		declare: declare,
		clear: function (topic, opts) { return clearAs(null, topic, opts); },
		topics: topics,
		trace: trace,
		history: function () { return historyRing.slice(); },
		owner: owner,
		claim: claim,
		onError: onError,
		util: { parseTopic: parseTopic, canonicalJson: canonicalJson, topicMatches: topicMatches, resolvePath: resolvePath },
		config: config,
		BusError: BusError,
		CONTRACT_VERSION: CONTRACT_VERSION,
		FRAMEWORK_FAMILIES: FRAMEWORK_FAMILIES,
		FRAMEWORK_SENDER_KEY: FRAMEWORK_SENDER_KEY,
		DRAIN_CAP: DRAIN_CAP,
		HISTORY_CAP: HISTORY_CAP,
		CYCLE_TRACE_CAP: CYCLE_TRACE_CAP
	};

	// ---- declarative wiring check (Task 5; spec 6.1, 11.2) ---------------------------------------------------------

	/**
	 * The page-load half of R-10 / R-11 (the same rule as Java's BusWiringValidator, pinned by bus-wiring-corpus.json).
	 * Returns only what the running page acts on: E-JS-41 (painted on the subscriber), E-JS-47 (painted on the card)
	 * and E-JS-52 (bridge and R-11 problems, painted as a page banner).  Shape and declaration errors that Java owns
	 * (E-40, E-42, E-43, E-45, E-46, E-48, E-49) silence the subscription they affect and are not repeated here.
	 *
	 * @example
	 * var problems = JuneauViews.bus.wiring.validate(contract, {
	 *   datatables: {roles: ['filter'], ops: ['reload'], implicit: function (card) { return ['filter:' + card.id]; }},
	 *   kpi:        {roles: null, ops: [], implicit: function () { return ['kpi.threshold-crossed']; }}
	 * });
	 * // [{code: 'E-JS-41', card: 'tasks', message: "card 'tasks' subscribes to 'selection:changes' but nothing publishes it"}]
	 */
	NS.bus.wiring = NS.bus.wiring || {};
	NS.bus.wiring.validate = (function () {
		const TOPIC_RE = /^(card|selection|filter|redraw|detail|bulk|cmd|probe|job|badge|bridge|[a-z][a-z0-9-]{0,31}\.[a-z][a-z0-9-]{0,31})(:[A-Za-z0-9_.-]{1,128})?$/;
		const CUSTOM_DECL_RE = /^[a-z][a-z0-9-]{0,31}\.[a-z][a-z0-9-]{0,31}(:([A-Za-z0-9_.-]{1,128}|\*))?$/;
		const BRIDGE_ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;
		const SAME_ORIGIN_PATH_RE = /^\/(?![/\\])\S*$/;
		const CARD_FAMILIES = ['card', 'selection', 'filter', 'redraw', 'detail', 'bulk', 'cmd'];
		const SHELL_ROLES = ['params', 'refresh'];
		const DATATABLES_OPT_IN = ['selection', 'detail', 'bulk'];
		const SUGGEST_MAX_DISTANCE = 3;
		const E41 = "card '%s' subscribes to '%s' but nothing publishes it%s";
		const E47 = "card '%s' (type '%s') does not accept role '%s'";
		const E52 = "bridge '%s': %s";

		function arr(v) { return Array.isArray(v) ? v : []; }
		function objs(v) { return arr(v).filter(function (o) { return o && typeof o === 'object' && !Array.isArray(o); }); }
		function strs(v) { return arr(v).filter(function (s) { return typeof s === 'string'; }); }
		function str(v) { return typeof v === 'string' ? v : null; }
		function family(t) { const i = t.indexOf(':'); return i < 0 ? t : t.slice(0, i); }
		function key(t) { const i = t.indexOf(':'); return i < 0 ? null : t.slice(i + 1); }
		function view(card) { return card.table && typeof card.table === 'object' ? card.table : card; }
		function isFramework(f) { return FRAMEWORK_FAMILIES.indexOf(f) >= 0; }

		function distance(a, b) {
			let prev = [];
			for (let j = 0; j <= b.length; j++) prev[j] = j;
			for (let i = 1; i <= a.length; i++) {
				const cur = [i];
				for (let j = 1; j <= b.length; j++)
					cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + (a[i - 1] === b[j - 1] ? 0 : 1));
				prev = cur;
			}
			return prev[b.length];
		}

		return function validate(contract, cardTypes) {
			const out = [];
			const types = cardTypes || {};
			const topics = objs(contract && contract.topics);
			const bridges = objs(contract && contract.bridges);
			const cards = new Map();
			objs(contract && contract.cards).forEach(function (c) {
				if (typeof c.id === 'string' && !cards.has(c.id)) cards.set(c.id, c);
			});
			function typeOf(card) { return Object.prototype.hasOwnProperty.call(types, card.type) ? types[card.type] : null; }

			// R-10 publishers (spec 6.1 rules 2-6), the candidate set for "did you mean".
			const published = new Set();
			function addCustom(t) { if (typeof t === 'string' && CUSTOM_DECL_RE.test(t)) published.add(t); }
			cards.forEach(function (c, id) {
				published.add('card:' + id);
				const t = typeOf(c);
				if (t && typeof t.implicit === 'function') {
					let implicit = [];
					try { implicit = strs(t.implicit(c)); } catch (e) { /* a throwing third-party implicit() is reported by the shell's wiring */ }
					implicit.forEach(function (x) { published.add(x); });
				}
				objs(c.publishes).forEach(function (p) { addCustom(p.topic); });
				objs(view(c).ribbon).forEach(function (item) { if (item.type === 'publish') addCustom(item.topic); });
			});
			topics.forEach(function (t) { if (t.topic === 'job:*') published.add(t.topic); else addCustom(t.topic); });
			bridges.forEach(function (b) {
				if (typeof b.id === 'string') published.add('bridge:' + b.id);
				strs(b.downstream).forEach(function (d) { if (d.indexOf('job:') === 0) published.add(d); });
			});
			const sorted = Array.from(published).sort();
			function isPublished(t) { return sorted.some(function (p) { return topicMatches(p, t); }); }
			function suggest(t) {
				let best = null;
				let bestDistance = SUGGEST_MAX_DISTANCE + 1;
				sorted.forEach(function (p) {
					const d = distance(t, p);
					if (d < bestDistance) { best = p; bestDistance = d; }
				});
				return best === null ? '' : fmt("; did you mean '%s'?", best);
			}

			function checkPublisher(id, t) {
				const fam = family(t);
				const k = key(t);
				if (isFramework(fam) && k !== null) {
					if (CARD_FAMILIES.indexOf(fam) >= 0) {
						if (k === id) return;                              // E-49 (Java)
						const target = cards.get(k);
						if (!target) return;                               // E-42 (Java)
						if (fam === 'card' || fam === 'cmd') return;
						if (!typeOf(target) || published.has(t)) return;
						if (target.type === 'datatables' && DATATABLES_OPT_IN.indexOf(fam) >= 0)
							out.push({ code: 'E-JS-41', card: id, message: fmt(E41, id, t, fmt("; card '%s' does not enable %s", k, fam)) });
						return;                                            // otherwise E-43 (Java)
					}
					if (fam === 'probe' || fam === 'badge') return;
				}
				if (!isPublished(t)) out.push({ code: 'E-JS-41', card: id, message: fmt(E41, id, t, suggest(t)) });
			}

			cards.forEach(function (c, id) {
				objs(c.subscribes).forEach(function (s) {
					if (typeof s.topic === 'string' && TOPIC_RE.test(s.topic)) checkPublisher(id, s.topic);
					const as = str(s.as);
					const t = typeOf(c);
					if (as === null || SHELL_ROLES.indexOf(as) >= 0 || !t || !Array.isArray(t.roles) || t.roles.indexOf(as) >= 0) return;
					out.push({ code: 'E-JS-47', card: id, message: fmt(E47, id, c.type, as) });
				});
			});

			// Bridges (Java E-51..E-54, all one JS code because the bridge runtime refuses the whole entry).
			function declaredCustom(t, publishers, cardPublishes) {
				const inTopics = topics.some(function (d) {
					return typeof d.topic === 'string' && publishers.indexOf(d.publisher) >= 0 && topicMatches(d.topic, t);
				});
				if (inTopics || !cardPublishes) return inTopics;
				let found = false;
				cards.forEach(function (c) {
					objs(c.publishes).forEach(function (p) { if (typeof p.topic === 'string' && topicMatches(p.topic, t)) found = true; });
				});
				return found;
			}
			function bridgeProblem(name, why) { out.push({ code: 'E-JS-52', message: fmt(E52, name, why) }); }
			const seen = new Set();
			bridges.forEach(function (b) {
				const name = typeof b.id === 'string' ? b.id : '?';
				if (typeof b.id !== 'string' || !BRIDGE_ID_RE.test(b.id)) bridgeProblem(name, 'id must match ' + BRIDGE_ID_RE.source);
				else if (seen.has(b.id)) bridgeProblem(name, 'duplicate id');
				else seen.add(b.id);
				if (b.transport !== 'sse' && b.transport !== 'websocket') bridgeProblem(name, fmt("unknown transport '%s'", b.transport));
				if (typeof b.session !== 'string' || !SAME_ORIGIN_PATH_RE.test(b.session))
					bridgeProblem(name, fmt("session must be a same-origin path, got '%s'", b.session));
				if (b.maxAttempts !== undefined && b.maxAttempts !== null && !(Number.isInteger(b.maxAttempts) && b.maxAttempts >= 1))
					bridgeProblem(name, 'maxAttempts must be an integer of at least 1');
				const down = strs(b.downstream);
				const up = strs(b.upstream);
				if (up.length && b.transport !== 'websocket') bridgeProblem(name, 'upstream is only allowed on a websocket bridge');
				down.forEach(function (d) {
					if (d !== 'job:*' && !TOPIC_RE.test(d) && !CUSTOM_DECL_RE.test(d)) return bridgeProblem(name, fmt("invalid topic '%s'", d));
					const fam = family(d);
					if (isFramework(fam)) {
						if (fam === 'job' || (fam === 'cmd' && key(d) !== null)) return;   // a missing cmd: card is E-42 (Java)
						return out.push({ code: 'E-JS-52', message: fmt("bridge '%s' may not carry framework state topic '%s' (only job:*, and cmd:<cardId> downstream)", name, d) });
					}
					if (!declaredCustom(d, ['server'], false))
						bridgeProblem(name, fmt("downstream '%s' is not declared in topics with publisher=server", d));
				});
				up.forEach(function (u) {
					if (!TOPIC_RE.test(u) && !CUSTOM_DECL_RE.test(u)) return bridgeProblem(name, fmt("invalid topic '%s'", u));
					if (isFramework(family(u)))
						return out.push({ code: 'E-JS-52', message: fmt("bridge '%s' may not carry framework state topic '%s' (only job:*, and cmd:<cardId> downstream)", name, u) });
					if (!declaredCustom(u, ['script', 'ribbon'], true))
						out.push({ code: 'E-JS-52', message: fmt("bridge '%s' sends '%s' upstream, but no topics entry or card publishes declares it", name, u) });
				});
				down.forEach(function (d) { if (up.indexOf(d) >= 0) bridgeProblem(name, fmt("topic '%s' is both downstream and upstream", d)); });
			});

			// R-11: every server topic rides some bridge downstream.
			topics.forEach(function (t) {
				if (typeof t.topic !== 'string' || t.publisher !== 'server') return;
				const covered = bridges.some(function (b) { return strs(b.downstream).some(function (d) { return topicMatches(d, t.topic); }); });
				if (!covered)
					out.push({ code: 'E-JS-52', message: fmt("topic '%s' is declared publisher=server but no bridge carries it downstream", t.topic) });
			});
			return out;
		};
	})();

	// ---- bridge runtime (spec §11.5, §11.6) ----------------------------------------------------------------------

	/** An `open` connection that lasts this long resets the backoff attempt counter (§11.6). */
	const BRIDGE_RESET_AFTER_MS = 30000;
	/** SSE 404 / WS 4401 this many times in a row with no `open` between them is E-JS-57 (§11.6). */
	const CAPABILITY_REFUSAL_LIMIT = 3;
	/** Bridge codes that leave the bridge unusable; the shell paints these on the page banner (§6.3). */
	const BRIDGE_BANNER_CODES = Object.freeze({ 'E-JS-52': true, 'E-JS-53': true, 'E-JS-56': true, 'E-JS-57': true });
	/** Framework families a bridge may carry downstream (§11.2): job state and card commands. */
	const BRIDGEABLE_FAMILIES = Object.freeze({ job: true, cmd: true });

	/** bridge id -> runtime record, for duplicate-id detection (E-JS-52). */
	const bridges = {};

	function bridgeTimers() { return NS.bus.config.timers; }

	function errText(e) { return e && e.message ? e.message : String(e); }

	/**
	 * Backoff before reconnect attempt `attempt` (1-based): `min(30 s, 1 s * 2^(attempt-1))` with +/-20% uniform
	 * jitter, `rand` in [0, 1).
	 *
	 * @example
	 * JuneauViews.bus.util.backoffDelay(1, 0.5);   // 1000
	 * JuneauViews.bus.util.backoffDelay(6, 0.5);   // 30000 (capped)
	 * JuneauViews.bus.util.backoffDelay(1, 0);     // 800  (-20%)
	 */
	function backoffDelay(attempt, rand) {
		const base = Math.min(30000, 1000 * Math.pow(2, Math.max(1, attempt) - 1));
		return Math.round(base * (0.8 + 0.4 * rand));
	}
	NS.bus.util.backoffDelay = backoffDelay;

	/** Builds and reports one bridge error; `text` is everything after `bridge '<id>'`. */
	function bridgeError(rec, code, text, from) {
		const err = new NS.bus.BusError(code, "bridge '" + rec.id + "'" + text,
			{ bridge: rec.id, banner: !!BRIDGE_BANNER_CODES[code], from: from || null });
		reportError(err);
		return { code: code, message: err.message };
	}

	function validBridgeId(id) {
		if (typeof id !== 'string' || id.indexOf(':') >= 0 || id.indexOf('*') >= 0) return false;
		try { NS.bus.util.parseTopic('bridge:' + id); return true; } catch (e) { return false; }
	}

	/** A downstream pattern: a concrete topic, or `family:*`; framework families other than job/cmd are refused. */
	function downstreamPatternProblem(p) {
		if (typeof p !== 'string') return 'downstream entries must be strings';
		const wild = p.slice(-2) === ':*';
		let t;
		try { t = NS.bus.util.parseTopic(wild ? p.slice(0, -2) + ':x' : p); }
		catch (e) { return "downstream '" + p + "' is not a valid topic pattern"; }
		if (t.framework && !BRIDGEABLE_FAMILIES[t.family])
			return "downstream '" + p + "' is a framework state topic owned by the page";
		if (wild && t.family === 'cmd') return "downstream '" + p + "' must name one card (cmd:<cardId>)";
		return null;
	}

	function grants(patterns, topic) {
		for (let i = 0; i < patterns.length; i++)
			if (NS.bus.util.topicMatches(patterns[i], topic)) return true;
		return false;
	}

	/** Returns the reason a source object is unusable, or null. */
	function sourceProblem(source) {
		if (!source || typeof source !== 'object') return 'source must be an object';
		if (!validBridgeId(source.id)) return "id '" + source.id + "' is not a valid bridge id";
		if (typeof source.connect !== 'function') return 'connect must be a function';
		if (!Array.isArray(source.downstream) || !source.downstream.length)
			return 'downstream must be a non-empty array of topic patterns';
		for (let i = 0; i < source.downstream.length; i++) {
			const p = downstreamPatternProblem(source.downstream[i]);
			if (p) return p;
		}
		const up = source.upstream == null ? [] : source.upstream;
		if (!Array.isArray(up)) return 'upstream must be an array of topics';
		for (let i = 0; i < up.length; i++) {
			let t;
			try { t = NS.bus.util.parseTopic(up[i]); } catch (e) { return "upstream '" + up[i] + "' is not a valid topic"; }
			if (t.framework) return "upstream '" + up[i] + "' must be a custom topic";
			if (grants(source.downstream, up[i])) return "topic '" + up[i] + "' is both downstream and upstream";
		}
		if (source.maxAttempts != null && !(Number.isInteger(source.maxAttempts) && source.maxAttempts > 0))
			return 'maxAttempts must be a positive integer';
		const live = bridges[source.id];
		if (live && live.state !== 'failed' && live.state !== 'closed') return "duplicate bridge id '" + source.id + "'";
		return null;
	}

	function newRecord(source) {
		const rec = {
			id: source.id, source: source,
			transport: typeof source.transport === 'string' ? source.transport : 'custom',
			upstream: source.upstream || [],
			owner: NS.bus.owner('bridge-runtime:' + source.id),
			state: null, attempt: 0, nextRetryMs: 0, gap: false, since: 0, error: null,
			everOpened: false, refusals: 0, conn: null, gen: 0, deadGen: -1,
			retainedPublished: new Set(), resyncSeen: null, claimed: new Set(),
			openTimer: null, retryTimer: null, upstreamOffs: [], disposed: false
		};
		const old = bridges[source.id];
		if (old) disposeRecord(old);
		bridges[source.id] = rec;
		rec.owner.claim('bridge:' + rec.id);
		return rec;
	}

	/** Publishes `bridge:<id>` (framework state, owner = this runtime). */
	function setState(rec, state) {
		rec.state = state;
		rec.since = bridgeTimers().now();
		const p = { schemaVersion: 1, id: rec.id, transport: rec.transport, state: state, attempt: rec.attempt,
			nextRetryMs: rec.nextRetryMs, gap: rec.gap, since: rec.since };
		if (rec.error) p.error = rec.error;
		rec.owner.publish('bridge:' + rec.id, p);
	}

	function clearTimers(rec) {
		const t = bridgeTimers();
		if (rec.openTimer) { t.clearTimeout(rec.openTimer); rec.openTimer = null; }
		if (rec.retryTimer) { t.clearTimeout(rec.retryTimer); rec.retryTimer = null; }
	}

	function safeClose(conn) {
		if (!conn || typeof conn.close !== 'function') return;
		try { conn.close(); } catch (e) { console.warn('[juneau-bus] connection close threw: ' + errText(e)); }
	}

	/** Per-message failure (E-JS-54/55/58/59): reported, recorded on bridge:<id>, connection stays up. */
	function perMessage(rec, code, text, from) {
		rec.error = bridgeError(rec, code, text, from);
		setState(rec, rec.state);
	}

	function fail(rec, code, text) {
		rec.error = bridgeError(rec, code, text);
		clearTimers(rec);
		rec.deadGen = rec.gen;
		safeClose(rec.conn);
		rec.conn = null;
		rec.nextRetryMs = 0;
		setState(rec, 'failed');
	}

	function claimIfFramework(rec, topic) {
		if (rec.claimed.has(topic)) return;
		if (NS.bus.util.parseTopic(topic).family === 'job') {
			rec.owner.claim(topic);
			rec.claimed.add(topic);
		}
	}

	function downstreamPublish(rec, topic, payload, retained) {
		if (!grants(rec.source.downstream, topic))
			return perMessage(rec, 'E-JS-54', " delivered '" + topic + "', which it was not granted; dropped");
		try {
			claimIfFramework(rec, topic);
			rec.owner.publish(topic, payload, { from: 'server', bridge: rec.id });
		} catch (e) {
			return perMessage(rec, e.code || 'E-JS-58', ': ' + errText(e));
		}
		// Retained either because the source says so or because the bus keeps this topic (a retain-declared custom topic).
		if (retained || retains(topic, parseTopic(topic))) {
			rec.retainedPublished.add(topic);
			if (rec.resyncSeen) rec.resyncSeen.add(topic);
		}
	}

	function downstreamClear(rec, topic) {
		if (!grants(rec.source.downstream, topic))
			return perMessage(rec, 'E-JS-54', " delivered '" + topic + "', which it was not granted; dropped");
		try {
			claimIfFramework(rec, topic);
			rec.owner.clear(topic);
		} catch (e) {
			return perMessage(rec, e.code || 'E-JS-58', ': ' + errText(e));
		}
		rec.retainedPublished.delete(topic);
	}

	/** At resync-end: a retained topic this bridge published earlier but did not re-send was cleared server-side. */
	function finishResync(rec) {
		const seen = rec.resyncSeen;
		rec.resyncSeen = null;
		if (!seen) return;
		Array.from(rec.retainedPublished).forEach(function (t) {
			if (seen.has(t)) return;
			try { rec.owner.clear(t); } catch (e) { reportError(e); }
			rec.retainedPublished.delete(t);
		});
	}

	function opened(rec) {
		rec.gap = rec.everOpened;
		rec.everOpened = true;
		rec.refusals = 0;
		rec.nextRetryMs = 0;
		rec.error = null;
		setState(rec, 'open');
		rec.openTimer = bridgeTimers().setTimeout(function () {
			rec.openTimer = null;
			if (rec.state === 'open' && rec.attempt) {
				rec.attempt = 0;
				setState(rec, 'open');                               // subscribers must not keep a stale attempt
			}
		}, BRIDGE_RESET_AFTER_MS);
	}

	/**
	 * A source reported its connection gone. `info` = {retryable, retryAfterMs?, capability?, quiet?, error?:
	 * {code, message}} where error.message is the text after "bridge '<id>': ".
	 */
	function closed(rec, gen, info) {
		if (rec.state === 'closed' || rec.state === 'failed') return;
		rec.deadGen = gen;
		rec.conn = null;
		rec.resyncSeen = null;
		const t = bridgeTimers();
		if (rec.openTimer) { t.clearTimeout(rec.openTimer); rec.openTimer = null; }
		const e = info.error || { code: 'E-JS-56', message: "transport '" + rec.transport + "' unavailable: connection closed" };
		if (info.quiet) {
			console.warn("[juneau-bus] bridge '" + rec.id + "': " + e.message);
			rec.error = { code: e.code, message: "bridge '" + rec.id + "': " + e.message };
			rec.nextRetryMs = 0;
			return setState(rec, 'closed');
		}
		if (!info.retryable) return fail(rec, e.code, ': ' + e.message);
		rec.error = { code: e.code, message: "bridge '" + rec.id + "': " + e.message };   // informational only
		if (info.capability) {
			rec.refusals++;
			if (rec.refusals >= CAPABILITY_REFUSAL_LIMIT)
				return fail(rec, 'E-JS-57', ' gave up after ' + rec.refusals + ' attempts: capability refused '
					+ rec.refusals + ' times in a row');
			rec.nextRetryMs = 0;
		} else {
			rec.attempt++;
			const max = rec.source.maxAttempts;
			if (max && rec.attempt > max)
				return fail(rec, 'E-JS-57', ' gave up after ' + max + ' attempts: ' + e.message);
			rec.nextRetryMs = Math.max(backoffDelay(rec.attempt, t.random()), info.retryAfterMs || 0);
		}
		setState(rec, 'reconnecting');
		rec.retryTimer = t.setTimeout(function () { rec.retryTimer = null; connect(rec); }, rec.nextRetryMs);
	}

	function makeCtx(rec, gen) {
		function current() { return gen === rec.gen && rec.deadGen !== gen; }
		function live() { return current() && (rec.state === 'connecting' || rec.state === 'open'); }
		return {
			publish: function (topic, payload, opts) { if (live()) downstreamPublish(rec, topic, payload, !!(opts && opts.retained)); },
			clear: function (topic) { if (live()) downstreamClear(rec, topic); },
			resyncBegin: function () { if (live()) rec.resyncSeen = new Set(); },
			resyncEnd: function () {
				if (!live()) return;
				finishResync(rec);
				if (rec.state === 'connecting') opened(rec);
			},
			state: function (s, info) {
				if (!current()) return;
				if (s === 'open') { if (rec.state === 'connecting') opened(rec); }
				else if (s === 'closed') closed(rec, gen, info || {});
			},
			error: function (code, text) { if (live()) perMessage(rec, code, ': ' + text); }
		};
	}

	function connect(rec) {
		const gen = ++rec.gen;
		rec.resyncSeen = null;
		setState(rec, 'connecting');
		let conn;
		try {
			conn = rec.source.connect(makeCtx(rec, gen));
		} catch (e) {
			return fail(rec, 'E-JS-52', ': connect threw: ' + errText(e));
		}
		if (gen !== rec.gen || rec.deadGen === gen) { safeClose(conn); return; }
		if (!conn || typeof conn.close !== 'function')
			return fail(rec, 'E-JS-52', ': connect must return {close, send?}');
		if (rec.upstream.length && typeof conn.send !== 'function') {
			safeClose(conn);
			return fail(rec, 'E-JS-52', ': send missing for a source with upstream topics');
		}
		rec.conn = conn;
	}

	function startUpstream(rec) {
		rec.upstream.forEach(function (topic) {
			rec.upstreamOffs.push(NS.bus.subscribe(topic, function (payload, meta) {
				if (meta && meta.bridge === rec.id) return;            // never echo this bridge's own downstream
				const from = meta && meta.from;
				if (rec.state !== 'open' || !rec.conn)
					return perMessage(rec, 'E-JS-55', ": upstream '" + topic + "' was refused by the server: bridge not open", from);
				try {
					rec.conn.send({ v: 1, type: 'pub', topic: topic, payload: payload });
				} catch (e) {
					perMessage(rec, 'E-JS-55', ": upstream '" + topic + "' was refused by the server: " + errText(e), from);
				}
			}, { retained: false }));
		});
	}

	function disposeRecord(rec) {
		if (rec.disposed) return;
		rec.disposed = true;
		rec.gen++;
		clearTimers(rec);
		safeClose(rec.conn);
		rec.conn = null;
		rec.upstreamOffs.forEach(function (off) { off(); });
		rec.upstreamOffs = [];
		rec.retainedPublished.forEach(function (t) {
			try { rec.owner.clear(t); } catch (e) { reportError(e); }
		});
		rec.retainedPublished.clear();
		if (rec.state !== 'closed') {
			rec.nextRetryMs = 0;
			setState(rec, 'closed');
		}
		rec.owner.dispose();                                         // clears bridge:<id> and claimed job: topics
		if (bridges[rec.id] === rec) delete bridges[rec.id];
	}

	/**
	 * Attaches a server source to the page bus (spec §11.5). The bus validates the source (E-JS-52), enforces its
	 * downstream grant (E-JS-54), owns reconnect/backoff/resync, publishes `bridge:<id>`, and forwards each
	 * `upstream` topic to `connection.send` while the bridge is open (E-JS-55 otherwise). Never throws.
	 *
	 * @param {BusSource} source `{id, downstream, upstream?, maxAttempts?, transport?, connect(ctx)}`.
	 * @returns {{detach(): void}} closes the connection, clears this source's retained topics and `bridge:<id>`.
	 *
	 * @example
	 * // A custom source (a test double, or a non-Juneau server): the bus still enforces grants and lifecycle.
	 * var link = JuneauViews.bus.attachSource({
	 *   id: 'clock', downstream: ['app.clock'],
	 *   connect: function (ctx) {
	 *     var t = setInterval(function () { ctx.publish('app.clock', {now: Date.now()}, {retained: true}); }, 1000);
	 *     ctx.state('open');
	 *     return { close: function () { clearInterval(t); } };
	 *   }
	 * });
	 * JuneauViews.bus.subscribe('bridge:clock', function (s) { console.log(s.state, s.attempt); });
	 * // later: link.detach();
	 */
	function attachSource(source) {
		const problem = sourceProblem(source);
		if (problem) {
			const id = source && validBridgeId(source.id) ? source.id : null;
			const live = id && bridges[id] && bridges[id].state !== 'failed' && bridges[id].state !== 'closed';
			if (!id || live) {
				reportError(new NS.bus.BusError('E-JS-52', "bridge '" + (id || (source && source.id)) + "': " + problem,
					{ bridge: id, banner: true, from: null }));
				return { detach: function () {} };
			}
			const bad = newRecord({ id: id, downstream: [], transport: source.transport });
			fail(bad, 'E-JS-52', ': ' + problem);
			return { detach: function () { disposeRecord(bad); } };
		}
		const rec = newRecord(source);
		startUpstream(rec);
		connect(rec);
		return { detach: function () { disposeRecord(rec); } };
	}
	NS.bus.attachSource = attachSource;
	// ---- end bridge runtime ----

	// ---- frame codec (spec §11.4; pinned by bus-frames-corpus.json, shared with Java BusFrames) ---------------------

	const FRAME_VERSION = 1;
	/** Default `BusPolicy.maxFrameBytes`; frames are UTF-8 JSON text. */
	const MAX_FRAME_BYTES = 65536;
	/**
	 * Canonical field order after `v` and `type`, per frame type (BusFrames writes the same order). `required` fields
	 * must be present; the others are written only when present. A non-string `error.topic` is dropped, as in Java.
	 */
	const FRAME_FIELDS = Object.freeze({
		'pub': { order: ['topic', 'payload', 'retained', 'seq'], required: ['topic', 'payload'] },
		'clear': { order: ['topic', 'seq'], required: ['topic'] },
		'resync-begin': { order: ['seq'], required: [] },
		'resync-end': { order: ['seq'], required: [] },
		'ping': { order: [], required: [] },
		'error': { order: ['code', 'message', 'topic'], required: ['code', 'message'] }
	});
	const FIELD_CHECKS = Object.freeze({
		topic: function (v) { return typeof v === 'string' && v.length > 0; },
		payload: function () { return true; },
		retained: function (v) { return typeof v === 'boolean'; },
		seq: function (v) { return Number.isSafeInteger(v) && v >= 0; },
		code: function (v) { return typeof v === 'string'; },
		message: function (v) { return typeof v === 'string'; }
	});

	function badFrame(reason, detail) {
		return new NS.bus.BusError('E-JS-58', 'bad frame (' + reason + '): ' + detail, { reason: reason });
	}

	/** UTF-8 byte length of a JS string (what the server's maxFrameBytes counts). */
	function utf8Length(s) {
		let n = 0;
		for (let i = 0; i < s.length; i++) {
			const c = s.charCodeAt(i);
			if (c < 0x80) n += 1;
			else if (c < 0x800) n += 2;
			else if (c >= 0xD800 && c <= 0xDBFF && i + 1 < s.length
				&& s.charCodeAt(i + 1) >= 0xDC00 && s.charCodeAt(i + 1) <= 0xDFFF) { n += 4; i++; }
			else n += 3;
		}
		return n;
	}

	/** Validates `o` as a v1 frame and returns a new object holding only its type's fields, in canonical order. */
	function normalizeFrame(o) {
		if (!o || typeof o !== 'object' || Array.isArray(o)) throw badFrame('not-json', 'frame is not a JSON object');
		if (o.v !== FRAME_VERSION) throw badFrame('bad-version', 'v is ' + JSON.stringify(o.v) + ', expected 1');
		const spec = typeof o.type === 'string' && Object.hasOwn(FRAME_FIELDS, o.type) ? FRAME_FIELDS[o.type] : null;
		if (!spec) throw badFrame('unknown-type', "type '" + o.type + "' is not a v1 frame type");
		const out = { v: FRAME_VERSION, type: o.type };
		spec.order.forEach(function (f) {
			if (!Object.hasOwn(o, f) || o[f] === undefined) {
				if (spec.required.indexOf(f) >= 0) throw badFrame('bad-field', o.type + '.' + f + ' is missing');
				return;
			}
			if (!FIELD_CHECKS[f](o[f])) {
				if (o.type === 'error' && f === 'topic') return;
				throw badFrame('bad-field', o.type + '.' + f);
			}
			out[f] = o[f];
		});
		return out;
	}

	/** Writes a normalized frame: canonical field order, payload exactly as JSON.stringify writes it. */
	function writeFrame(f) {
		const parts = ['"v":1', '"type":' + JSON.stringify(f.type)];
		FRAME_FIELDS[f.type].order.forEach(function (k) {
			if (!Object.hasOwn(f, k)) return;
			const text = JSON.stringify(f[k]);
			if (text === undefined) throw badFrame('bad-field', f.type + '.' + k + ' is not JSON');
			parts.push(JSON.stringify(k) + ':' + text);
		});
		return '{' + parts.join(',') + '}';
	}

	/**
	 * Wire codec for bridge frames (spec §11.4). Field order is canonical (`v`, `type`, then the type's fields), so
	 * the browser and Java's `BusFrames` produce the same bytes; `bus-frames-corpus.json` pins both.
	 *
	 * @example
	 * JuneauViews.bus.frames.encode({type: 'pub', v: 1, payload: {}, topic: 'ops.cancel-all'});
	 * // -> '{"v":1,"type":"pub","topic":"ops.cancel-all","payload":{}}'
	 * JuneauViews.bus.frames.decode('{"type":"ping","v":1,"extra":true}');   // -> {v: 1, type: 'ping'}
	 * JuneauViews.bus.frames.decode('{"v":2,"type":"ping"}');               // throws BusError E-JS-58 (bad-version)
	 *
	 * @example
	 * // The SSE subset SseResponseSupport writes; chunks may split anywhere.
	 * var p = new JuneauViews.bus.frames.SseParser();
	 * p.push('event: bus\nid: 1\nda');                  // -> []
	 * p.push('ta: {"v":1,"type":"ping"}\n\n');          // -> [{event: 'bus', id: '1', data: '{"v":1,"type":"ping"}'}]
	 */
	const frames = {
		MAX_FRAME_BYTES: MAX_FRAME_BYTES,
		utf8Length: utf8Length,
		encode: function (frame) {
			const text = writeFrame(normalizeFrame(frame));
			if (utf8Length(text) > MAX_FRAME_BYTES)
				throw badFrame('too-large', utf8Length(text) + ' bytes exceeds ' + MAX_FRAME_BYTES);
			return text;
		},
		decode: function (text) {
			if (typeof text !== 'string') throw badFrame('not-json', 'frame is not text');
			if (utf8Length(text) > MAX_FRAME_BYTES)
				throw badFrame('too-large', utf8Length(text) + ' bytes exceeds ' + MAX_FRAME_BYTES);
			let o;
			try { o = JSON.parse(text); } catch (e) { throw badFrame('not-json', errText(e)); }
			return normalizeFrame(o);
		},
		SseParser: SseParser
	};

	/**
	 * Incremental parser for exactly the text/event-stream subset SseResponseSupport writes: `event:`, `id:`,
	 * `data:` (multi-line joined with \n), `:` comments, blank-line dispatch; LF, CRLF or CR line ends. `retry:` and
	 * unknown fields are ignored. `id` is per event (the bridge never reconnects by Last-Event-ID, OQ-B18).
	 */
	function SseParser() {
		this.buf = '';
		this.event = null;
		this.id = null;
		this.data = [];
		this.size = 0;
		this.dropping = false;
	}
	SseParser.prototype.push = function (chunk) {
		this.buf += chunk;
		const out = [];
		for (;;) {
			const m = /\r\n|\r|\n/.exec(this.buf);
			if (!m) {
				if (this.buf.length > MAX_FRAME_BYTES) { this.dropping = true; this.data = []; this.buf = ''; }   // one endless line
				break;
			}
			if (m[0] === '\r' && m.index === this.buf.length - 1) break;   // maybe the first half of CRLF
			const line = this.buf.slice(0, m.index);
			this.buf = this.buf.slice(m.index + m[0].length);
			this.line(line, out);
		}
		return out;
	};
	SseParser.prototype.line = function (l, out) {
		if (l === '') {
			if (this.dropping) out.push({ event: this.event || 'message', id: this.id, data: '', tooLarge: true });
			else if (this.data.length) out.push({ event: this.event || 'message', id: this.id, data: this.data.join('\n') });
			this.event = null;
			this.id = null;
			this.data = [];
			this.size = 0;
			this.dropping = false;
			return;
		}
		if (this.dropping) return;
		if (l.charAt(0) === ':') return;
		const i = l.indexOf(':');
		const f = i < 0 ? l : l.slice(0, i);
		let v = i < 0 ? '' : l.slice(i + 1);
		if (v.charAt(0) === ' ') v = v.slice(1);
		if (f === 'event') this.event = v;
		else if (f === 'data') {
			this.size += v.length + 1;
			if (this.size > MAX_FRAME_BYTES) { this.dropping = true; this.data = []; }   // about the byte limit (UTF-16 units)
			else this.data.push(v);
		}
		else if (f === 'id') this.id = v;
	};
	NS.bus.frames = frames;
	// ---- end frame codec ----

	// ---- stock sources: session handshake (spec §11.3) and the SSE source -----------------------------------------

	const SESSION_ID_RE = /^[0-9a-f]{64}$/;
	const ABSOLUTE_URL_RE = /^(https?:)\/\/([^/?#]+)/i;
	const DEFAULT_HEARTBEAT_MS = 15000;
	/** A stream with no frame (and no comment) for this many heartbeats is dead (§11.6). */
	const WATCHDOG_HEARTBEATS = 3;
	const DEFAULT_CSRF_HEADER = 'X-Csrf-Token';
	const UPSTREAM_REFUSAL_CODES = Object.freeze({ 'bus:upstream-denied': true, 'bus:upstream-failed': true, 'bus:rate-limited': true });

	/** Browser seams, each overridable through the source options (the Node harnesses inject fakes). */
	function sourceEnv(opts) {
		const w = typeof window !== 'undefined' ? window : {};
		return {
			fetch: opts.fetch || (typeof fetch === 'function' ? fetch : null),
			location: opts.location || w.location || null,
			document: opts.document || (typeof document !== 'undefined' ? document : null),
			AbortController: opts.AbortController || w.AbortController || (typeof AbortController === 'function' ? AbortController : null),
			TextDecoder: opts.TextDecoder || w.TextDecoder || (typeof TextDecoder === 'function' ? TextDecoder : null),
			WebSocket: opts.WebSocket || w.WebSocket || (typeof WebSocket === 'function' ? WebSocket : null)
		};
	}

	/**
	 * The session POST's CSRF headers: opts.csrf, else C6's JuneauViews.csrf.headers(body), else the C1-stamped
	 * `<body data-juneau-csrf data-juneau-csrf-header>`. No attribute = no header (the app's own guard applies);
	 * a stamped-but-blank token returns null, which refuses the session (the C6 fail-closed rule).
	 */
	function csrfHeaders(env, opts) {
		const h = {};
		if (opts.csrf) {
			if (!opts.csrf.token || !String(opts.csrf.token).trim()) return null;
			h[opts.csrf.header || DEFAULT_CSRF_HEADER] = opts.csrf.token;
			return h;
		}
		const body = env.document && env.document.body;
		if (NS.csrf && typeof NS.csrf.headers === 'function') return NS.csrf.headers(body) || null;
		const token = body && body.getAttribute('data-juneau-csrf');
		if (token == null) return h;
		if (!token.trim()) return null;
		const name = body.getAttribute('data-juneau-csrf-header');
		h[name && name.trim() ? name.trim() : DEFAULT_CSRF_HEADER] = token;
		return h;
	}

	function sameOrigin(url, loc) {
		if (typeof url !== 'string' || !url) return false;
		if (/[\u0000-\u001f\u007f\\]/.test(url)) return false;                   // browsers strip tabs/newlines: '/\t/host' is '//host'
		if (url.charAt(0) === '/' && url.charAt(1) !== '/' && url.charAt(1) !== '\\') return true;
		const m = url.match(ABSOLUTE_URL_RE);
		return !!(m && loc && m[1].toLowerCase() === loc.protocol && m[2].toLowerCase() === String(loc.host).toLowerCase());
	}

	function retryAfterMs(res) {
		const v = res && res.headers && typeof res.headers.get === 'function' ? res.headers.get('Retry-After') : null;
		return v && /^\d+$/.test(String(v).trim()) ? parseInt(v, 10) * 1000 : 0;
	}

	function readJson(res) {
		return Promise.resolve(res.text()).then(function (t) {
			try { return JSON.parse(t); } catch (e) { return null; }
		}, function () { return null; });
	}

	function describeRefusal(body) {
		if (!body || typeof body !== 'object') return 'no detail';
		const parts = [body.code || 'refused'];
		if (body.message) parts.push(body.message);
		if (Array.isArray(body.denied) && body.denied.length) parts.push('denied ' + body.denied.join(', '));
		return parts.join(': ');
	}

	function declaredRetain(topic) {
		const all = NS.bus.topics();
		for (let i = 0; i < all.length; i++)
			if (all[i].topic === topic) return all[i].retain;
		return undefined;
	}

	/** Checks a 200 session body against the request; returns {session} or a non-retryable refusal. */
	function acceptSession(body, opts) {
		const refuse = function (why, text) {
			return { retryable: false, error: { code: 'E-JS-53', message: 'session refused (' + why + '): ' + text } };
		};
		if (!body || body.v !== 1 || typeof body.sessionId !== 'string' || !SESSION_ID_RE.test(body.sessionId)
			|| !Array.isArray(body.downstream) || !body.paths || typeof body.paths !== 'object')
			return refuse('bad-response', 'malformed session response');
		const asked = opts.downstream.slice().sort().join(',');
		const got = body.downstream.map(function (d) { return d && d.topic; }).sort().join(',');
		if (asked !== got) return refuse('grant', 'server granted [' + got + '], page requested [' + asked + ']');
		const askedUp = (opts.upstream || []).slice().sort().join(',');
		const gotUp = (Array.isArray(body.upstream) ? body.upstream : []).slice().sort().join(',');
		if (askedUp !== gotUp) return refuse('grant', 'server granted upstream [' + gotUp + '], page requested [' + askedUp + ']');
		for (let i = 0; i < body.downstream.length; i++) {
			const d = body.downstream[i];
			const page = declaredRetain(d.topic);
			if (page !== undefined && page !== d.retain)
				return refuse('retain', "topic '" + d.topic + "' is retain=" + d.retain + ' on the server but retain=' + page + ' on the page');
		}
		return { session: {
			id: body.sessionId,
			heartbeatMs: Number.isInteger(body.heartbeatMs) && body.heartbeatMs > 0 ? body.heartbeatMs : DEFAULT_HEARTBEAT_MS,
			paths: body.paths
		} };
	}

	/**
	 * POST <session> (spec §11.3). Resolves {session} or a closed-info {retryable, retryAfterMs?, error} that the
	 * source hands straight to ctx.state('closed', info). Never rejects.
	 */
	function openSession(env, opts, transport) {
		const unavailable = function (why, retryable) {
			return { retryable: retryable, error: { code: 'E-JS-56', message: "transport '" + transport + "' unavailable: " + why } };
		};
		if (!env.fetch) return Promise.resolve(unavailable('no fetch in this browser', false));
		if (!sameOrigin(opts.session, env.location))
			return Promise.resolve({ retryable: false, error: { code: 'E-JS-52', message: "session URL '" + opts.session + "' is not same-origin" } });
		const csrf = csrfHeaders(env, opts);
		if (csrf === null)
			return Promise.resolve({ retryable: false, error: { code: 'E-JS-53', message: 'session refused (csrf): blank CSRF token on <body data-juneau-csrf>' } });
		const headers = Object.assign({ 'Content-Type': 'application/json', 'Accept': 'application/json' }, csrf);
		const body = JSON.stringify({ v: 1, bridge: opts.id, transport: transport, downstream: opts.downstream, upstream: opts.upstream || [] });
		return Promise.resolve()
			.then(function () { return env.fetch(opts.session, { method: 'POST', credentials: 'same-origin', headers: headers, body: body }); })
			.then(function (res) {
				const s = res.status;
				if (s === 200) return readJson(res).then(function (b) { return acceptSession(b, opts); });
				if (s === 429 || (s >= 500 && s !== 501))
					return { retryable: true, retryAfterMs: retryAfterMs(res), error: { code: 'E-JS-53', message: 'session refused (' + s + '): retrying' } };
				return readJson(res).then(function (b) {
					if (s === 501) return unavailable('server answered 501 (' + describeRefusal(b) + ')', false);
					return { retryable: false, error: { code: 'E-JS-53', message: 'session refused (' + s + '): ' + describeRefusal(b) } };
				});
			}, function (e) { return unavailable('session POST failed: ' + errText(e), true); });
	}

	/** Applies one decoded server frame to the source ctx; bad frames are E-JS-58 (dropped, connection stays up). */
	function applyFrameText(ctx, text) {
		let f;
		try { f = NS.bus.frames.decode(text); }
		catch (e) { ctx.error(e.code || 'E-JS-58', e.message); return; }
		switch (f.type) {
			case 'pub': ctx.publish(f.topic, f.payload, { retained: f.retained === true }); break;
			case 'clear': ctx.clear(f.topic); break;
			case 'resync-begin': ctx.resyncBegin(); break;
			case 'resync-end': ctx.resyncEnd(); break;
			case 'error':
				if (f.code === 'bus:slow-consumer') break;                  // the server closes next; the resync repairs
				if (UPSTREAM_REFUSAL_CODES[f.code])
					ctx.error('E-JS-55', "upstream '" + (f.topic || '?') + "' was refused by the server: " + f.code + ' ' + f.message);
				else
					ctx.error('E-JS-59', 'server error ' + f.code + ': ' + f.message);
				break;
			default: break;                                                 // ping: only feeds the watchdog
		}
	}

	/** Cancels a reader or body, ignoring both a throw and a rejected promise (the stream may already be finished). */
	function cancelQuietly(x) {
		try {
			const p = x.cancel();
			if (p && typeof p.catch === 'function') p.catch(function () { /* already finished */ });
		} catch (e) { /* already finished */ }
	}

	/** A watchdog that calls `onDead` when not fed for WATCHDOG_HEARTBEATS heartbeats. */
	function watchdog(onDead) {
		let id = null;
		return {
			feed: function (heartbeatMs) {
				const t = NS.bus.config.timers;
				if (id) t.clearTimeout(id);
				id = t.setTimeout(function () { id = null; onDead(WATCHDOG_HEARTBEATS * heartbeatMs); }, WATCHDOG_HEARTBEATS * heartbeatMs);
			},
			stop: function () { if (id) { NS.bus.config.timers.clearTimeout(id); id = null; } }
		};
	}

	/**
	 * Stock SSE source (spec §11.3 step 2a): session POST, then a `fetch` + ReadableStream reader on paths.sse
	 * (not EventSource, OQ-B23: the lifecycle needs the HTTP status, and its own retry would fight the backoff).
	 *
	 * @param {{id: string, session: string, downstream: string[], upstream?: string[], maxAttempts?: number,
	 *   fetch?: Function, location?: Location, document?: Document, TextDecoder?: Function,
	 *   csrf?: {header?: string, token: string}}} opts
	 * @returns {BusSource}
	 *
	 * @example
	 * var link = JuneauViews.bus.attachSource(JuneauViews.bus.sources.sse({
	 *   id: 'ops', session: '/rest/ops/jobs/juneau-bus/session', downstream: ['ops.jobs', 'cmd:jobs']
	 * }));
	 * JuneauViews.bus.subscribe('bridge:ops', function (s) { banner.hidden = s.state === 'open'; });
	 * // later: link.detach();
	 */
	function sseSource(opts) {
		const env = sourceEnv(opts);
		let session = null;
		return {
			id: opts.id, transport: 'sse', downstream: opts.downstream, upstream: opts.upstream || [],
			maxAttempts: opts.maxAttempts,
			connect: function (ctx) {
				let done = false;
				let reader = null;
				const abort = env.AbortController ? new env.AbortController() : null;
				const dog = watchdog(function (ms) {
					end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: no frame for " + ms + 'ms' } });
				});
				function stop() {
					done = true;
					dog.stop();
					if (reader) { cancelQuietly(reader); reader = null; }
					if (abort) { try { abort.abort(); } catch (e) { /* nothing in flight */ } }
				}
				function end(info) {
					if (done) return;
					stop();
					ctx.state('closed', info);
				}
				function onStream(res) {
					if (done) {                                                       // closed while the GET was pending
						if (res && res.body) cancelQuietly(res.body);
						return undefined;
					}
					const s = res.status;
					if (s === 404) {
						session = null;                                              // re-POST on the next connect
						return end({ retryable: true, capability: true, error: { code: 'E-JS-53', message: 'session refused (404): capability unknown or expired' } });
					}
					if (s === 429 || s >= 500)
						return end({ retryable: true, retryAfterMs: retryAfterMs(res), error: { code: 'E-JS-56', message: "transport 'sse' unavailable: stream answered " + s } });
					if (s !== 200)
						return end({ retryable: false, error: { code: 'E-JS-53', message: 'session refused (' + s + '): stream refused' } });
					if (!res.body || typeof res.body.getReader !== 'function' || !env.TextDecoder)
						return end({ retryable: false, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: no ReadableStream in this browser" } });
					reader = res.body.getReader();
					const decoder = new env.TextDecoder();
					const parser = new SseParser();
					dog.feed(session.heartbeatMs);
					const pump = function () {
						if (done || !reader) return;
						reader.read().then(function (step) {
							if (done) return;
							if (step.done) {
								end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: stream ended" } });
								return;
							}
							dog.feed(session.heartbeatMs);
							parser.push(decoder.decode(step.value, { stream: true })).forEach(function (ev) {
								if (done || ev.event !== 'bus') return;
								if (ev.tooLarge) ctx.error('E-JS-58', 'bad frame (too-large): event exceeds ' + MAX_FRAME_BYTES + ' bytes');
								else applyFrameText(ctx, ev.data);
							});
							pump();
						}).catch(function (e) {
							end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: " + errText(e) } });
						});
					};
					pump();
					return undefined;
				}
				(session ? Promise.resolve({ session: session }) : openSession(env, opts, 'sse'))
					.then(function (r) {
						if (done) return undefined;
						if (!r.session) return end(r);
						session = r.session;
						if (typeof session.paths.sse !== 'string' || !sameOrigin(session.paths.sse, env.location))
							return end({ retryable: false, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: server sent no same-origin sse path" } });
						return env.fetch(session.paths.sse, { method: 'GET', credentials: 'same-origin', cache: 'no-store',
							headers: { 'Accept': 'text/event-stream' }, signal: abort ? abort.signal : undefined }).then(onStream);
					})
					.catch(function (e) {
						end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'sse' unavailable: " + errText(e) } });
					});
				return { close: stop };
			}
		};
	}
	NS.bus.sources = NS.bus.sources || {};
	NS.bus.sources.sse = sseSource;
	// ---- end SSE source ----

	// ---- stock WebSocket source (spec §11.3 step 2b) and the shell hook ------------------------------------------

	/** Close codes the browser retries with backoff (§11.6); anything not listed below is also retried. */
	const WS_CAPABILITY_CLOSE = 4401;
	const WS_REPLACED_CLOSE = 4409;
	/** Policy closes: the bridge fails (E-JS-53 for 4403 bus:refused, E-JS-56 for 1008 / 1009). */
	const WS_POLICY_CLOSE = Object.freeze({ 1008: 'E-JS-56', 1009: 'E-JS-56', 4403: 'E-JS-53' });
	const WS_OPEN = 1;

	function wsCloseInfo(code, reason) {
		const why = code + (reason ? ' ' + reason : '');
		if (code === WS_CAPABILITY_CLOSE)
			return { retryable: true, capability: true, error: { code: 'E-JS-53', message: 'session refused (4401): ' + (reason || 'bus:unknown-session') } };
		if (code === WS_REPLACED_CLOSE)
			return { quiet: true, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: replaced by another connection (" + why + ')' } };
		if (Object.hasOwn(WS_POLICY_CLOSE, code)) {
			const c = WS_POLICY_CLOSE[code];
			return { retryable: false, error: { code: c, message: c === 'E-JS-53'
				? 'session refused (' + code + '): ' + (reason || 'bus:refused')
				: "transport 'websocket' unavailable: closed by policy (" + why + ')' } };
		}
		return { retryable: true, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: closed (" + why + ')' } };
	}

	/**
	 * Stock WebSocket source (spec §11.3 step 2b): session POST, then `new WebSocket(ws[s]://<location.host><path>)`
	 * (the URL is built from `location`, never from a server-sent host). Upstream topics are sent as text frames.
	 *
	 * @param {{id: string, session: string, downstream: string[], upstream?: string[], maxAttempts?: number,
	 *   fetch?: Function, location?: Location, document?: Document, WebSocket?: Function,
	 *   csrf?: {header?: string, token: string}}} opts
	 * @returns {BusSource}
	 *
	 * @example
	 * // A ribbon 'publish' item on ops.cancel-all now reaches the server's UpstreamHandler.
	 * JuneauViews.bus.attachSource(JuneauViews.bus.sources.websocket({
	 *   id: 'ops', session: '/rest/ops/jobs/juneau-bus/session',
	 *   downstream: ['ops.jobs'], upstream: ['ops.cancel-all']
	 * }));
	 */
	function wsSource(opts) {
		const env = sourceEnv(opts);
		let session = null;
		return {
			id: opts.id, transport: 'websocket', downstream: opts.downstream, upstream: opts.upstream || [],
			maxAttempts: opts.maxAttempts,
			connect: function (ctx) {
				let done = false;
				let ws = null;
				const dog = watchdog(function (ms) {
					end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: no frame for " + ms + 'ms' } });
				});
				function stop() {
					done = true;
					dog.stop();
					const s = ws;
					ws = null;
					if (s) {
						s.onmessage = s.onclose = s.onerror = null;
						try { s.close(1000, 'detach'); } catch (e) { /* already closing */ }
					}
				}
				function end(info) {
					if (done) return;
					stop();
					ctx.state('closed', info);
				}
				function open(r) {
					if (done) return;
					if (!r.session) return end(r);
					session = r.session;
					const path = session.paths.websocket;
					if (typeof path !== 'string' || path.charAt(0) !== '/' || path.charAt(1) === '/')
						return end({ retryable: false, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: server sent no websocket path" } });
					const loc = env.location;
					const s = new env.WebSocket((loc.protocol === 'https:' ? 'wss:' : 'ws:') + '//' + loc.host + path);
					ws = s;
					dog.feed(session.heartbeatMs);
					s.onmessage = function (ev) {
						if (done) return;
						dog.feed(session.heartbeatMs);
						if (typeof ev.data !== 'string') { ctx.error('E-JS-58', 'bad frame (not-json): binary message'); return; }
						applyFrameText(ctx, ev.data);
					};
					s.onerror = function () { /* onclose always follows */ };
					s.onclose = function (ev) {
						if (done) return;
						ws = null;
						if (ev.code === WS_CAPABILITY_CLOSE) session = null;       // re-POST on the next connect
						end(wsCloseInfo(ev.code, ev.reason));
					};
				}
				if (!env.WebSocket) {
					Promise.resolve().then(function () {
						end({ retryable: false, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: no WebSocket in this browser" } });
					});
				} else {
					(session ? Promise.resolve({ session: session }) : openSession(env, opts, 'websocket'))
						.then(open)
						.catch(function (e) {
							end({ retryable: true, error: { code: 'E-JS-56', message: "transport 'websocket' unavailable: " + errText(e) } });
						});
				}
				return {
					close: stop,
					send: function (frame) {
						if (done || !ws || ws.readyState !== WS_OPEN) throw new Error('bridge not open');
						let text;
						try { text = NS.bus.frames.encode(frame); }
						catch (e) { throw new Error(e.message); }
						ws.send(text);
					}
				};
			}
		};
	}
	NS.bus.sources.websocket = wsSource;

	/**
	 * Shell hook (spec §11.5): attaches one stock source per contract `bridges[]` entry and detaches them all on
	 * `pagehide`. An unknown transport is E-JS-52 (page banner). Returns the links, in contract order.
	 *
	 * @param {Object} contract the page contract (`bridges[]`: {id, transport, session, downstream, upstream?, maxAttempts?}).
	 * @param {{sourceOptions?: Object}} [opts] extra stock-source options (the harnesses inject fakes here).
	 * @returns {Array<{detach(): void}>}
	 *
	 * @example
	 * var links = JuneauViews.bus.wiring.attachBridges(contract);
	 * // teardown: links.forEach(function (l) { l.detach(); });
	 */
	function attachBridges(contract, opts) {
		const links = [];
		const extra = (opts && opts.sourceOptions) || {};
		((contract && contract.bridges) || []).forEach(function (b) {
			const make = b.transport === 'sse' ? NS.bus.sources.sse : b.transport === 'websocket' ? NS.bus.sources.websocket : null;
			if (!make) {
				reportError(new NS.bus.BusError('E-JS-52', "bridge '" + b.id + "': unknown transport '" + b.transport + "'",
					{ bridge: b.id, banner: true, from: null }));
				return;
			}
			links.push(NS.bus.attachSource(make(Object.assign({}, extra, {
				id: b.id, session: b.session, downstream: b.downstream, upstream: b.upstream || [], maxAttempts: b.maxAttempts
			}))));
		});
		const w = typeof window !== 'undefined' ? window : null;
		if (links.length && w && typeof w.addEventListener === 'function')
			// A page going into the back/forward cache (persisted) is restored with its bridges; only a real unload detaches.
			w.addEventListener('pagehide', function (ev) { if (ev && ev.persisted) return; links.forEach(function (l) { l.detach(); }); });
		return links;
	}
	NS.bus.wiring = NS.bus.wiring || {};
	NS.bus.wiring.attachBridges = attachBridges;
	// ---- end WebSocket source ----

	// ==== bridge runtime (Task 11) and frames/sources (Tasks 12-13) go here ====
})();
