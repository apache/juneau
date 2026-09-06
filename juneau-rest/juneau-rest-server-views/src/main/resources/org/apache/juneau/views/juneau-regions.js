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
 * juneau-regions.js - the region runtime: one `populate(ctx, container)` seam, a name-keyed populator registry, and
 * one page-scoped message bus.
 *
 * A REGION is an empty, framework-owned container whose body is drawn by a named JavaScript function instead of by
 * server-emitted markup.  The framework owns the container, its identity, its lifecycle and its cancellation; the
 * populator owns everything inside it.  Three region types exist and no more: "row-detail", "card-body", "tab-body".
 *
 *     JuneauViews.regions.register("my-populator", function populate(ctx, container) {
 *         // container is empty, in-document, and mine.
 *         // Return nothing, a cleanup function, a Promise, or a Promise of a cleanup function.
 *     });
 *
 * LOAD ORDER IS A CONTRACT, NOT A PREFERENCE.  This script MUST come after juneau-views.js: it publishes onto the
 * window.JuneauViews namespace that file creates, and it reuses that file's renderAsyncStatus status renderer and its
 * same-origin URL / CSRF / safe-method guards rather than defining second copies of them.  A page with no
 * [data-juneau-region] element never needs to load it, and a page that loads it but has no regions pays nothing: the
 * bus and the barrier are both created at first enrolment, never by a document scan.
 *
 * NO EMITTER IS WIRED HERE.  This file is the seam and the channel; the chrome call sites that enrol regions and the
 * chrome events that auto-emit onto the bus are a separate, later edit.  Nothing that ships today behaves
 * differently because this asset exists, which is the property that lets the riskiest construction in the design land
 * before anything depends on it.
 *
 * PLATFORM BASELINE.  AbortController/AbortSignal are a hard requirement, feature-detected exactly once at load and
 * never branched on again.  Every shipped Juneau asset already uses optional chaining, which is ES2020 syntax and
 * therefore a PARSE-time dependency - a browser that cannot parse juneau-views.js loads no region runtime at all, and
 * AbortController predates optional chaining across every engine.  So the baseline adds no requirement a consumer did
 * not already have; it only writes down one that was already true.  When the global is genuinely absent the runtime
 * REFUSES to populate and says so, rather than degrading to a signal-less ctx: ctx.signal is load-bearing in three
 * separate contracts (teardown abort, fork()'s supersede-the-previous-request mechanism, and ctx.write's prior-write
 * abort) and none of them has a non-signal fallback.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};

	// ==================================================================================================================
	// CONSTANTS
	// ==================================================================================================================

	/**
	 * The region contract version, echoed onto every ctx so a consumer that wants to assert it can.  Deliberately
	 * independent of the view, action-result, bulk, detail and nested envelope versions - a region-contract revision
	 * must never force one of those to bump, or vice-versa.  MUST equal RegionDef.CONTRACT_VERSION on the server.
	 */
	const REGION_CONTRACT_VERSION = "1";

	/** The region container's identity attributes - MUST equal the server's constants of the same names. */
	const REGION_ATTR = "data-juneau-region";
	const REGION_TYPE_ATTR = "data-juneau-region-type";
	const REGION_HOST_ATTR = "data-juneau-region-host";
	const REGION_POPULATE_ATTR = "data-juneau-region-populate";

	/**
	 * The enrolment mark, reflected as an attribute so the region's own loading/ok/error state is assertable from a
	 * test and visible in devtools.  Cleared by teardown and by nothing else, so a marked-but-disconnected node is a
	 * detectable bug rather than an invisible one.
	 */
	const REGION_STATE_ATTR = "data-juneau-region-state";

	/** The three region types.  Three, not four: a detail body is one region, not a strip of sections. */
	const REGION_TYPES = ["row-detail", "card-body", "tab-body"];

	/**
	 * The reserved populator name, and the whole of the reserved set.  `register("default", ...)` is refused so that
	 * the declarative default's non-privilege argument stays true: the name must resolve to Juneau's default and not
	 * to something a consumer swapped in.  Every OTHER Juneau-shipped populator is an ordinary, overridable registry
	 * entry - freezing the whole shipped set would make it a compatibility surface, so that adding a populator called
	 * "chart" in a later release would break a consumer that had already registered "chart".
	 */
	const DEFAULT_POPULATOR_NAME = "default";
	const RESERVED_POPULATOR_NAMES = [DEFAULT_POPULATOR_NAME];

	/** Framework-emitted `kind` values are namespaced.  A consumer `kind` must not start with this prefix. */
	const FRAMEWORK_KIND_PREFIX = "juneau:";

	/** `meta.from` for a framework-emitted message: the framework, not a region. */
	const FRAMEWORK_SENDER_KEY = "juneau:framework";

	/**
	 * The emit QUEUE DEPTH cap: how many emissions ONE DRAIN may queue from inside its own delivery before the drain
	 * is declared cyclic and terminated.  It is deliberately never consulted for a top-of-turn emit or for the
	 * initial emissions the barrier holds - a page with 65 control regions each emitting once as it paints contains no
	 * cycle, and reporting one would be a false diagnostic - and it is NOT a message budget over the life of a page or
	 * of a replay buffer.
	 *
	 * It counts emissions-per-drain rather than the queue's INSTANTANEOUS length, and the difference is the whole
	 * reason the cap terminates anything.  The obvious reading - cap `queue.length` - catches a cycle that nests,
	 * because nesting grows the queue faster than delivery drains it.  It does not catch the simplest cycle there is:
	 * two regions each re-emitting on the other's message hold the queue at exactly one pending entry forever, since
	 * every delivery removes one and adds one, so an instantaneous-length cap is never reached and the drain loops
	 * until the tab is killed.  Counting the emissions instead makes both shapes terminate at the same bound.
	 */
	const BUS_QUEUE_DEPTH_CAP = 64;

	/**
	 * The initial-drain replay buffer's LENGTH cap: appends over one buffer's lifetime.  Ordinary live traffic grows
	 * it monotonically and nothing removes an entry until the buffer is discarded whole, so reaching it means a
	 * long-lived released window rather than a defect in the traffic - which is why it is four times the queue-depth
	 * cap and why its overflow is a buffer abandonment and never a cycle report.  A queue that reaches depth 64 is
	 * pathological; a buffer that reaches length 64 is a moderately busy page.  The two caps are independent in both
	 * directions and must not be collapsed into one constant.
	 */
	const REPLAY_BUFFER_CAP = 256;

	/**
	 * The page-wide initial-broadcast barrier deadline, a CLAMPED constant and not a consumer-settable one: an
	 * author-settable barrier deadline is a way to configure a page-wide freeze, which is the same reasoning ViewDef
	 * gives for flooring a declarable poll interval.  Long enough for a normal first paint including one declared
	 * fetch, short enough that a user does not perceive a control as dead.  Because a released region also gets a
	 * bounded replay of the drain it missed, this number is a latency knob rather than a correctness one.
	 */
	const BARRIER_DEADLINE_MS = 2000;

	/** How many (from -> to) hops are retained for the cycle diagnostic.  Bounded; a ring, not a log. */
	const CYCLE_TRACE_CAP = 256;

	/** The abort reason a fork uses when it supersedes the previous fork or message signal from the same region. */
	const SUPERSEDED_REASON = "juneau:superseded";

	/** The abort reason a re-populate uses, distinct from teardown so a consumer can tell the two apart. */
	const REPOPULATE_REASON = "juneau:region-repopulated";

	/** The abort reason teardown uses. */
	const TEARDOWN_REASON = "juneau:region-torn-down";

	/**
	 * Builds the value an abort is reported WITH, and the shape is load-bearing rather than cosmetic.
	 *
	 * `controller.abort(someString)` makes `signal.reason` that string, and `fetch` rejects with `signal.reason`
	 * verbatim - so a populate running the documented swallow (`catch(e) { if (e.name !== "AbortError") throw e; }`)
	 * would read `e.name` as `undefined` on a string reason and RETHROW the cancellation it meant to ignore, turning
	 * every supersede into a surfaced region error.  Aborting with no argument avoids that but then teardown, a
	 * re-populate and a supersede are indistinguishable.  A DOMException named "AbortError" gets both: the platform
	 * contract every author and every third-party library already tests for, plus a message saying which of the three
	 * happened.
	 */
	function abortReason(reason) {
		if (typeof DOMException === "function") return new DOMException(reason, "AbortError");
		const e = new Error(reason);
		e.name = "AbortError";
		return e;
	}

	/**
	 * The ONE platform-baseline feature test in this runtime.  Evaluated at load and never re-evaluated: a second,
	 * per-call test is how a silent degrade creeps back in.
	 */
	const HAS_ABORT_CONTROLLER = typeof AbortController !== "undefined";

	// ==================================================================================================================
	// SHARED STATE  (all page-scoped, all created here, none exposed for enumeration)
	// ==================================================================================================================

	/** name -> populate fn.  Last-write-wins, exactly like the renderer registry. */
	const registry = {};

	/** Juneau-shipped populators, resolved ahead of the consumer registry.  The reserved default lands here. */
	const builtins = {};

	/** Every live region on the page, in enrolment order.  The bus resolves a `{to}` against this. */
	const liveRegions = [];

	/**
	 * The bus.  Deliberately NOT exposed as an object a region can enumerate: a region only ever sees its own ctx.emit
	 * and ctx.on, so the framework can sweep a region's subscriptions wholesale at teardown - which is exactly the
	 * capability a global `bus.on(...)` would not have.
	 */
	const bus = {
		subscribers: [],
		queue: [],
		fanOutDepth: 0,
		draining: false,
		queuedThisDrain: 0,
		hops: []
	};

	/** The one page-scoped barrier.  "unarmed" until the first enrolment; "lifted" exactly once thereafter. */
	const barrier = {
		state: "unarmed",
		outstanding: [],
		runtimes: [],
		ready: [],
		deadlineTimer: null,
		backstopArmed: false,
		warned: false
	};

	/** The initial-drain replay buffer.  One per page load; never reconstituted after an overflow. */
	const replay = {
		open: false,
		abandoned: false,
		buffer: [],
		windows: []
	};

	/** Set once when the AbortController baseline refusal has been reported, so a page gets one error and not N. */
	let baselineReported = false;

	// ==================================================================================================================
	// SMALL HELPERS
	// ==================================================================================================================

	function isElement(n) {
		return !!n && n.nodeType === 1;
	}

	function isConnected(el) {
		if (typeof el.isConnected === "boolean") return el.isConnected;
		return !!window.document?.contains?.(el);
	}

	function blank(s) {
		return s == null || String(s).trim() === "";
	}

	function keysOf(o) {
		return o ? Object.keys(o) : [];
	}

	/**
	 * Visibility, for the "is this region's first populate an initial one" question.  Mirrors the card runtime's
	 * predicate: the `hidden` attribute, an inline display:none, a non-active tab/subtab panel ancestor, and a
	 * computed display:none.
	 */
	function isRegionHidden(el) {
		let n = el;
		while (isElement(n)) {
			if (n.hidden) return true;
			if (n.style?.display === "none") return true;
			const cls = " " + (n.className || "") + " ";
			if ((cls.indexOf(" jc-panel ") >= 0 || cls.indexOf(" jc-subpanel ") >= 0) && cls.indexOf(" jc-active ") < 0)
				return true;
			const computed = window.getComputedStyle ? window.getComputedStyle(n) : null;
			if (computed?.display === "none") return true;
			n = n.parentNode;
		}
		return false;
	}

	/** Clears a container without touching the region mark: R4's clear-before-every-call must not re-mint a region. */
	function clearContainer(el) {
		if (typeof el.replaceChildren === "function") { el.replaceChildren(); return; }
		while (el.firstChild) el.firstChild.remove();
	}

	/**
	 * Reflects the region's loading/ok/error state.
	 *
	 * The state is carried by an ATTRIBUTE, and the view runtime's owned status paragraph is painted only for the
	 * error state - deliberately, and it is the one place this runtime declines to reuse renderAsyncStatus verbatim.
	 * That function appends a `<p role="status">` INTO the container it is given (juneau-views.js:1406-1410), and a
	 * region's container has to be EMPTY when the populator is handed it: painting a loading paragraph first would
	 * hand every populator a container with a framework child already in it, and painting one afterwards would append
	 * a framework node into DOM the author owns.  On the error path neither objection applies, because the container
	 * has just been cleared and its contents are the framework's own message rather than the author's content.
	 */
	function setRegionState(region, state, message) {
		region.state = state;
		region.el.setAttribute(REGION_STATE_ATTR, state);
		if (state !== "error") return;
		const render = NS.init?.renderAsyncStatus;
		if (typeof render === "function") render(region.el, "error", message);
	}

	// ==================================================================================================================
	// REGISTRY  (modeled on the renderer registry, with two deliberate differences)
	// ==================================================================================================================

	/**
	 * Registers a populator under `name`.  Last-write-wins, exactly like registerRenderer - a consumer may override a
	 * Juneau-shipped populator, with the ONE exception of the reserved default, which is refused with a console error
	 * and no mutation.
	 */
	function register(name, populateFn) {
		if (blank(name)) {
			window.console.error("JuneauViews.regions.register: a populator name must not be blank.");
			return null;
		}
		if (RESERVED_POPULATOR_NAMES.indexOf(name) >= 0) {
			window.console.error("JuneauViews.regions.register: '" + name
				+ "' is a reserved populator name and cannot be overridden.");
			return null;
		}
		if (typeof populateFn !== "function") {
			window.console.error("JuneauViews.regions.register: populator '" + name + "' must be a function.");
			return null;
		}
		registry[name] = populateFn;
		return populateFn;
	}

	/**
	 * Looks up a populator by name; returns null when unknown.  Unlike a renderer, an unknown name FAILS VISIBLY at
	 * the call site rather than falling back - a field has a raw value to fall back to and a region does not.
	 */
	function resolve(name) {
		if (typeof name !== "string") return null;
		if (Object.hasOwn(builtins, name)) return builtins[name];
		return Object.hasOwn(registry, name) ? registry[name] : null;
	}

	// ==================================================================================================================
	// CANCELLATION  (per-invocation signal, per-delivery signal, and fork())
	// ==================================================================================================================

	/**
	 * Attaches `fork` to a genuine platform AbortSignal as a non-enumerable, non-writable OWN property.
	 *
	 * NEVER on AbortSignal.prototype: a page hosting a region and an unrelated library on the same document would
	 * then see the framework's method on THEIR signals.  Non-enumerable so that a key-set walk of the ctx or of the
	 * signal does not see it.  The signal itself stays a real AbortSignal - `instanceof` holds, `aborted`/`reason`/
	 * `addEventListener`/`throwIfAborted` all behave normally, and it can be handed to fetch or to a third-party
	 * library with no adapter, which is the property that decided this shape over a wrapper object.
	 */
	function attachFork(region, signal) {
		Object.defineProperty(signal, "fork", {
			value: function () { return forkFrom(region); },
			enumerable: false,
			writable: false,
			configurable: false
		});
		return signal;
	}

	/**
	 * Returns a fresh child signal, aborting the previous fork from the same region and incrementing ctx.generation.
	 *
	 * A consumer may hold at most ONE live fork per region and should not try to hold more - forking again cancels
	 * the previous one, and that IS the mechanism.  A populate that genuinely needs two concurrent, independently
	 * cancellable requests allocates its own AbortController and returns a cleanup function; that is a legitimate
	 * case and this method does not cover it.  The child is a PLAIN AbortSignal with no `fork` of its own: forking is
	 * a capability of the invocation signal, not a chainable operation, so the tree stays exactly two levels deep and
	 * "which fork aborted which" stays answerable.
	 */
	function forkFrom(region) {
		if (region.lastFork && !region.lastFork.signal.aborted) region.lastFork.abort(abortReason(SUPERSEDED_REASON));
		if (region.lastForkUnlink) region.lastForkUnlink();
		const child = new AbortController();
		const link = linkToParent(region.signalCtl.signal, child);
		region.lastFork = child;
		region.lastForkUnlink = link.unlink;
		region.generation++;
		if (region.ctx) region.ctx.generation = region.generation;
		return link.signal;
	}

	/**
	 * Wires parent -> child abort with one listener, and returns the CHILD CONTROLLER'S OWN signal.
	 *
	 * Deliberately not AbortSignal.any, even where the platform has it: `any` returns a THIRD signal that is neither
	 * the parent's nor the child's, so the framework would hold a controller whose `signal.aborted` stays false after
	 * the parent aborts while the consumer holds a different object that reports true.  Two objects disagreeing about
	 * one cancellation is how "which fork aborted which" stops being answerable, and the supersede check reads the
	 * framework's side.  The listener is unlinked when the fork is superseded, so a region that forks on every message
	 * does not accumulate one listener per fork on its invocation signal.
	 */
	function linkToParent(parent, childCtl) {
		if (parent.aborted) {
			childCtl.abort(parent.reason);
			return { signal: childCtl.signal, unlink: function () { /* nothing was listening */ } };
		}
		const onAbort = function () { childCtl.abort(parent.reason); };
		parent.addEventListener("abort", onAbort, { once: true });
		return {
			signal: childCtl.signal,
			unlink: function () { parent.removeEventListener("abort", onAbort); }
		};
	}

	/**
	 * Aborts the region's live per-delivery signal, called immediately before the NEXT message reaches that region.
	 * A populate that re-populates on every message therefore gets ordering for free: its previous invocation's
	 * messageSignal is already aborted by the time its handler for the new message runs.
	 */
	function abortPriorMessageSignal(region) {
		if (region.messageCtl && !region.messageCtl.signal.aborted)
			region.messageCtl.abort(abortReason(SUPERSEDED_REASON));
		region.messageCtl = null;
	}

	// ==================================================================================================================
	// THE BUS
	// ==================================================================================================================

	function isDelivering() {
		return bus.fanOutDepth > 0;
	}

	/** Resolves an emit's `{to}` to a live region: a fully-qualified key first, then a short id within the host. */
	function resolveTarget(entry) {
		for (const r of liveRegions) if (r.key === entry.to) return r;
		const host = entry.fromRegion ? entry.fromRegion.host : null;
		if (blank(host)) return null;
		for (const r of liveRegions) if (r.host === host && r.id === entry.to) return r;
		return null;
	}

	function recordHop(entry) {
		bus.hops.push(entry.from + " -> " + (entry.to == null ? "*" : entry.to));
		if (bus.hops.length > CYCLE_TRACE_CAP) bus.hops.shift();
	}

	/**
	 * Reports an emit cycle by printing the SHORTEST REPEATING SUFFIX of the recent hop ring - the actual loop -
	 * rather than the pair of keys that happened to be on the stack when the cap was reached.  A cycle is
	 * A -> B -> C -> A and naming the last pair points at an arbitrary edge.  No property of any payload is read to
	 * produce this: printing a routing trace is not inspecting a message.
	 */
	function reportCycle() {
		window.console.error("JuneauViews.regions: emit cycle detected - one drain queued more than "
			+ BUS_QUEUE_DEPTH_CAP + " emissions from inside its own delivery, and was terminated.  Cycle: "
			+ shortestRepeatingSuffix(bus.hops));
	}

	function shortestRepeatingSuffix(hops) {
		for (let len = 1; len <= Math.floor(hops.length / 2); len++) {
			let repeats = true;
			for (let i = 0; i < len && repeats; i++)
				if (hops[hops.length - 1 - i] !== hops[hops.length - 1 - i - len]) repeats = false;
			if (repeats) return hops.slice(hops.length - len).join(" | ");
		}
		return hops.slice(Math.max(0, hops.length - 4)).join(" | ");
	}

	function enqueue(entry) {
		if (isDelivering() && ++bus.queuedThisDrain > BUS_QUEUE_DEPTH_CAP) {
			reportCycle();
			bus.queue.length = 0;
			return false;
		}
		bus.queue.push(entry);
		return true;
	}

	function emitEntry(entry) {
		if (!enqueue(entry)) return;
		if (barrier.state === "buffering") return;
		if (isDelivering()) return;
		drain();
	}

	function drain() {
		if (bus.draining) return;
		bus.draining = true;
		bus.queuedThisDrain = 0;
		try {
			while (bus.queue.length) deliver(bus.queue.shift());
		} finally {
			bus.draining = false;
			bus.queuedThisDrain = 0;
		}
	}

	/**
	 * One fan-out: one message to every matching subscriber, synchronously, in subscription order.
	 *
	 * The released-window MEMBERSHIP SNAPSHOT is taken once, here, before any handler runs.  Every message delivered
	 * in this fan-out is appended for exactly the windows that were open when it began; a window whose close
	 * condition becomes true partway through remains a member for this fan-out and is closed at its end, and a window
	 * cannot open mid-fan-out because opening one requires the deadline to fire and the deadline is not re-entrant
	 * into a drain.  Without the snapshot the buffer's contents would depend on when a promise happened to resolve
	 * inside the drain, and on subscription order, rather than on the message order.
	 */
	function deliver(entry) {
		const snapshot = replay.open ? replay.windows.slice() : [];
		appendForSnapshot(entry, snapshot);
		const target = entry.to == null ? null : resolveTarget(entry);
		if (entry.to != null && target == null) {
			window.console.warn("JuneauViews.regions: no region matches emit target '" + entry.to
				+ "'; the message was not delivered.");
			return;
		}
		if (target != null && target === entry.fromRegion) {
			window.console.warn("JuneauViews.regions: region '" + entry.from
				+ "' addressed itself; the message was not delivered.");
			return;
		}
		recordHop(entry);
		bus.fanOutDepth++;
		try {
			for (const sub of bus.subscribers.slice()) {
				if (!subscriberMatches(sub, entry, target)) continue;
				invokeSubscriber(sub, entry);
			}
		} finally {
			bus.fanOutDepth--;
			closeDeferredWindows(snapshot);
		}
	}

	/**
	 * YOUR OWN BROADCAST DOES NOT COME BACK TO YOU.  A self-reflecting control - a date-range card that also displays
	 * "showing last 7 days" - cannot learn its own state from the bus and must keep it in local state, updated in the
	 * same handler that emits.
	 */
	function subscriberMatches(sub, entry, target) {
		if (!sub.active) return false;
		if (target != null) return sub.region === target;
		return sub.region !== entry.fromRegion;
	}

	/**
	 * Per-subscriber error isolation: a throw is caught, reported once with both keys, and the loop continues.  One
	 * misbehaving consumer must not take the page down, and a repeatedly throwing subscriber is deliberately NOT
	 * auto-unsubscribed - silently dropping it turns a loud bug into a mysterious one.
	 */
	function invokeSubscriber(sub, entry) {
		abortPriorMessageSignal(sub.region);
		try {
			sub.handler(entry.msg, { from: entry.from, to: entry.to });
		} catch (e) {
			window.console.error("JuneauViews.regions: subscriber '" + sub.key + "' threw handling a message from '"
				+ entry.from + "'; the remaining subscribers were unaffected.", e);
		}
	}

	// --- the replay window ------------------------------------------------------------------------------------------

	function appendForSnapshot(entry, snapshot) {
		if (!replay.open || snapshot.length === 0) return;
		if (replay.buffer.length + 1 > REPLAY_BUFFER_CAP) {
			abandonReplayBuffer();
			return;
		}
		replay.buffer.push(entry);
	}

	/**
	 * The buffer's terminal condition.  It is ABANDONED, never truncated: a truncated replay violates same-order,
	 * exactly-once BY OMISSION - the region receives a contiguous run with a hole in the middle and no way to know -
	 * which is the one outcome rated worse than no replay at all.  The overflow discard SUPERSEDES the membership
	 * snapshot: the overflowing message is not appended for anyone, every window closes immediately rather than at
	 * the fan-out's end (overflow is the buffer's own terminal condition, identical for every member, so there is
	 * nothing for a boundary to make deterministic), and NO buffer is ever reconstituted for the rest of the page
	 * load.  Live delivery is untouched and the still-in-flight populates are not aborted.
	 *
	 * It is reported as a data-loss error and NEVER as a cycle: the traffic that reached this cap contains no
	 * repetition, and naming a defect that does not exist is worse than silence.
	 */
	function abandonReplayBuffer() {
		const affected = replay.windows.slice();
		replay.open = false;
		replay.abandoned = true;
		replay.buffer = [];
		replay.windows = [];
		for (const r of affected) { r.releasedWindowOpen = false; r.windowCloseDeferred = false; }
		window.console.error("JuneauViews.regions: the initial-drain replay buffer exceeded its cap of "
			+ REPLAY_BUFFER_CAP + " appends and was discarded whole rather than truncated.  Delivery exactly-once was"
			+ " NOT met for: " + affected.map(function (r) { return r.key; }).join(", ")
			+ ".  The traffic contained no repetition; this is a long-lived released window, not an emit cycle.");
	}

	function discardReplayBuffer() {
		replay.open = false;
		replay.buffer = [];
		replay.windows = [];
	}

	function closeReleasedWindow(region) {
		if (!region.releasedWindowOpen) return;
		if (isDelivering()) { region.windowCloseDeferred = true; return; }
		finishWindowClose(region);
	}

	function finishWindowClose(region) {
		region.releasedWindowOpen = false;
		region.windowCloseDeferred = false;
		const i = replay.windows.indexOf(region);
		if (i >= 0) replay.windows.splice(i, 1);
		if (replay.open && replay.windows.length === 0) discardReplayBuffer();
	}

	function closeDeferredWindows(snapshot) {
		for (const r of snapshot) if (r.windowCloseDeferred) finishWindowClose(r);
	}

	/**
	 * Replays the retained initial drain into a newly-subscribing released region.  The replay RUNS UNDER THE FAN-OUT
	 * FLAG, so it is governed by the ordinary ordering rules with no second rule to keep in sync: an emit from a
	 * replayed handler is queued rather than live, the queue-depth cap and the cycle diagnostic apply unchanged, and
	 * delivery stays synchronous within the ctx.on call.
	 */
	function replayInto(sub) {
		const buffered = replay.buffer.slice();
		bus.fanOutDepth++;
		try {
			for (const entry of buffered) {
				const target = entry.to == null ? null : resolveTarget(entry);
				if (!subscriberMatches(sub, entry, target)) continue;
				invokeSubscriber(sub, entry);
			}
		} finally {
			bus.fanOutDepth--;
		}
		drain();
	}

	// --- the ctx-scoped subscription --------------------------------------------------------------------------------

	function subscribe(region, handler) {
		if (typeof handler !== "function") {
			window.console.error("JuneauViews.regions: ctx.on(handler) requires a function.");
			return function () { /* nothing was subscribed, so nothing is unsubscribed */ };
		}
		const sub = { region: region, key: region.key, handler: handler, active: true };
		bus.subscribers.push(sub);
		region.subs.push(sub);
		if (replay.open && region.releasedWindowOpen && !region.replayed) {
			region.replayed = true;
			replayInto(sub);
		}
		return function () { unsubscribeOne(sub); };
	}

	function unsubscribeOne(sub) {
		sub.active = false;
		const i = bus.subscribers.indexOf(sub);
		if (i >= 0) bus.subscribers.splice(i, 1);
	}

	/**
	 * Emits from a region.  The payload is OPAQUE: the framework moves the object reference and never reads a
	 * property of it - no normalizing, no kind-based routing, no cloning, no serialization.  Two consequences the
	 * javadoc has to carry rather than defend against: the framework cannot warn about a message nobody handles,
	 * because it cannot tell a broadcast meant to be ignored from a misaddressed one; and payload identity is SHARED,
	 * so a subscriber that mutates the payload affects the others.  Copying would require inspecting the payload, and
	 * a structured clone would break a payload carrying a DOM node or a function - both of which a consumer may
	 * legitimately send.
	 *
	 * `{to}` re-couples two regions by id, which is the coupling the bus exists to remove.  Reach for broadcast; use
	 * `{to}` when you genuinely mean "drive that one".
	 */
	function emitFrom(region, msg, opts) {
		const to = opts?.to != null ? String(opts.to) : null;
		emitEntry({ msg: msg, from: region.key, to: to, fromRegion: region });
	}

	/**
	 * Emits a framework-authored message.  `meta.from` names the framework rather than a region, so a broadcast
	 * reaches every subscriber including the one whose chrome caused it.
	 *
	 * This is the CHROME half of the ownership invariant: Juneau-drawn chrome auto-emits, author-drawn content emits
	 * explicitly, and the framework never auto-emits for content it did not draw.  There is no framework listener
	 * anywhere inside a region container, and there must never be one - a "helpful" click delegate over author DOM
	 * would make the framework-emitted set open-ended and break the invariant at the click level.  The dividing line
	 * is drawn where knowledge actually is: the framework is the authoritative source for selection, expansion and
	 * redraw, and would have to guess at everything inside a region container.
	 */
	function emitFramework(msg, opts) {
		const to = opts?.to != null ? String(opts.to) : null;
		emitEntry({ msg: msg, from: FRAMEWORK_SENDER_KEY, to: to, fromRegion: null });
	}

	// --- the closed set of framework message schemas ------------------------------------------------------------------

	/**
	 * `viewId` is mandatory on all three schemas.  Broadcast plus two tables on one page otherwise leaves a
	 * subscriber unable to tell WHOSE selection arrived, and `meta.from` cannot disambiguate it either: for a
	 * framework-emitted message `meta.from` names the framework, so there is no host for a short target to resolve
	 * against.  The identity has to be in the payload, which is why the framework authoring these payloads is
	 * load-bearing rather than incidental.  `schemaVersion` is per-message and independent of the region contract
	 * version, for the same reason the envelopes are independent: adding a field here must not force a
	 * region-envelope bump.
	 */
	function selectionChangedMessage(o) {
		return {
			kind: FRAMEWORK_KIND_PREFIX + "selection-changed",
			schemaVersion: 1,
			viewId: o.viewId,
			ids: o.ids || [],
			rows: o.rows || [],
			added: o.added || [],
			removed: o.removed || []
		};
	}

	function detailToggledMessage(o) {
		return {
			kind: FRAMEWORK_KIND_PREFIX + "detail-toggled",
			schemaVersion: 1,
			viewId: o.viewId,
			rowId: o.rowId,
			expanded: !!o.expanded,
			generation: o.generation
		};
	}

	function tableRedrewMessage(o) {
		return {
			kind: FRAMEWORK_KIND_PREFIX + "table-redrew",
			schemaVersion: 1,
			viewId: o.viewId,
			nested: !!o.nested,
			rowCount: o.rowCount,
			page: o.page
		};
	}

	// ==================================================================================================================
	// THE BARRIER
	// ==================================================================================================================

	/**
	 * Declares a runtime present on the page.  Presence is established by the asset loading and registering itself at
	 * parse time rather than by a document scan, so there is no list to keep in sync.
	 */
	function registerRuntime(token) {
		if (blank(token)) return;
		if (barrier.runtimes.indexOf(token) < 0) barrier.runtimes.push(token);
	}

	/**
	 * Declares that a runtime has finished its enrolment walk.  Enrolment completion is DECLARED by the host runtime,
	 * not inferred from the clock: the enrolment walk is synchronous, so the set is complete for a host the moment
	 * that host's own init returns, which is what makes the declaration a statement a runtime can truthfully make.
	 */
	function ready(token) {
		if (blank(token)) return;
		if (barrier.ready.indexOf(token) < 0) barrier.ready.push(token);
		maybeLift();
	}

	/**
	 * Arms the barrier at the FIRST enrolment, never from a document scan - so a page with no regions never arms it,
	 * adds no listener and starts no timer.
	 */
	function armBarrier() {
		if (barrier.state !== "unarmed") return;
		barrier.state = "buffering";
		barrier.deadlineTimer = window.setTimeout(onDeadline, BARRIER_DEADLINE_MS);
		armBackstop();
	}

	/**
	 * The backstop under the declared-ready protocol, and what makes declaring safe to rely on.  A registered runtime
	 * that never reports has the barrier lift anyway at DOMContentLoaded plus one macrotask, with one error naming it
	 * - so a forgetful runtime degrades loudly rather than silently, which is the inverse of the failure mode the
	 * clock-only alternative was rejected for.
	 */
	function armBackstop() {
		if (barrier.backstopArmed) return;
		barrier.backstopArmed = true;
		const fire = function () { window.setTimeout(backstopCheck, 0); };
		if (window.document.readyState === "loading") window.document.addEventListener("DOMContentLoaded", fire);
		else fire();
	}

	function backstopCheck() {
		if (barrier.state !== "buffering") return;
		const missing = barrier.runtimes.filter(function (t) { return barrier.ready.indexOf(t) < 0; });
		if (missing.length === 0) return;
		window.console.error("JuneauViews.regions: lifting the initial-broadcast barrier without an enrolment report"
			+ " from: " + missing.join(", ") + ".  A runtime that enrols regions must call"
			+ " JuneauViews.regions.ready(...) when its walk completes.");
		for (const t of missing) barrier.ready.push(t);
		maybeLift();
	}

	/** Enters a region into the outstanding set.  Only a region whose FIRST populate is an initial one is a member. */
	function enterBarrier(region) {
		if (barrier.state !== "buffering") return;
		region.barrierMember = true;
		barrier.outstanding.push(region);
	}

	function leaveBarrier(region) {
		if (!region.barrierMember) return;
		region.barrierMember = false;
		const i = barrier.outstanding.indexOf(region);
		if (i >= 0) barrier.outstanding.splice(i, 1);
		maybeLift();
	}

	function maybeLift() {
		if (barrier.state !== "buffering") return;
		if (barrier.outstanding.length) return;
		if (barrier.runtimes.some(function (t) { return barrier.ready.indexOf(t) < 0; })) return;
		lift();
	}

	function lift() {
		barrier.state = "lifted";
		if (barrier.deadlineTimer !== null) {
			window.clearTimeout(barrier.deadlineTimer);
			barrier.deadlineTimer = null;
		}
		drain();
	}

	/**
	 * The deadline fires in two phases.  Phase one drains NOW, so the healthy regions are not made to wait on the
	 * slow one - that is the whole point of having a deadline.  Phase two gives each released region a bounded window
	 * in which its later ctx.on receives the drain it missed, once, whole and in the original order, so a released
	 * region ends in the state it would have reached had it settled on time.
	 *
	 * A released populate is LEFT RUNNING - not aborted, signal not fired, container untouched - and may still
	 * resolve and paint.  The barrier is an ordering device, not a lifecycle one; giving it the power to cancel a
	 * populate would turn a slow network into a cancelled paint, which is far worse than a late-delivered first
	 * message.  One warning for the page, not one per region.
	 */
	function onDeadline() {
		barrier.deadlineTimer = null;
		if (barrier.state !== "buffering") return;
		const released = barrier.outstanding.slice();
		barrier.outstanding.length = 0;
		for (const r of released) r.barrierMember = false;
		if (released.length && !replay.abandoned) {
			replay.open = true;
			replay.buffer = [];
			replay.windows = released.slice();
			for (const r of released) r.releasedWindowOpen = true;
		}
		if (released.length && !barrier.warned) {
			barrier.warned = true;
			window.console.warn("JuneauViews.regions: the initial-broadcast barrier reached its "
				+ BARRIER_DEADLINE_MS + "ms deadline and released: "
				+ released.map(function (r) { return r.key; }).join(", ")
				+ ".  Their populates are still running and may still paint.");
		}
		for (const t of barrier.runtimes) if (barrier.ready.indexOf(t) < 0) barrier.ready.push(t);
		lift();
	}

	// ==================================================================================================================
	// THE ctx
	// ==================================================================================================================

	/**
	 * Builds the exact ctx contract.  This literal IS the contract - a member introduced anywhere else has not been
	 * introduced - so it is written as one object literal rather than assembled across several assignments, and its
	 * member set is pinned by a golden test.
	 */
	function buildCtx(region, reason) {
		return {
			contractVersion: REGION_CONTRACT_VERSION,
			type: region.type,
			id: region.id,
			key: region.key,
			ids: region.ids,
			params: region.params,
			declared: region.declared,
			data: region.data,
			fetchDeclared: function () { return fetchDeclared(region); },
			defaultPopulate: defaultPopulate,
			write: function (url, body, opts) { return regionWrite(region, url, body, opts); },
			selection: region.selection,
			emit: function (msg, opts) { emitFrom(region, msg, opts); },
			on: function (handler) { return subscribe(region, handler); },
			helpers: NS.helpers,
			host: region.host,
			generation: region.generation,
			signal: region.signalCtl.signal,
			messageSignal: reason === "message" ? region.messageCtl.signal : null,
			reason: reason,
			refresh: function () { requestRefresh(region); }
		};
	}

	/**
	 * Performs the region's declared fetch, using the current invocation's signal.  A region that declared no dataUrl
	 * has nothing to fetch, so this resolves null - which is every region under the identity-only descriptor this
	 * runtime reads today.  It does not touch the region's loading/ok/error state: a fetch is not a paint.
	 */
	function fetchDeclared(region) {
		if (blank(region.declared.dataUrl)) return Promise.resolve(null);
		return Promise.reject(new Error("JuneauViews.regions: ctx.fetchDeclared() cannot run a declared fetch for '"
			+ region.key + "' because no declarative region descriptor is served to this runtime."));
	}

	/**
	 * Awaited delegation to the reserved default.  Exposed on ctx as well as on the namespace so a wrapper does not
	 * have to reach for a global.  An absent default fails VISIBLY, exactly as an unknown populator name does, rather
	 * than resolving quietly and leaving a blank container behind.
	 */
	function defaultPopulate(ctx, container) {
		const fn = resolve(DEFAULT_POPULATOR_NAME);
		if (typeof fn !== "function") {
			const message = "no populator is registered under the reserved name '" + DEFAULT_POPULATOR_NAME + "'";
			window.console.error("JuneauViews.regions: region '" + (ctx ? ctx.key : "?")
				+ "' delegated to the default and " + message + ".");
			return Promise.reject(new Error(message));
		}
		return Promise.resolve(fn(ctx, container));
	}

	/**
	 * The framework-owned, CSRF-stamped write.  It exists because a populate that paints a button has no other way to
	 * reach the token: the token is stamped on a host element and read back by the view runtime with a bare
	 * getAttribute, and a card grid carries no table.  Everything here mirrors the row-action write path rather than
	 * inventing a second one - the same safe-method refusal, the same non-normalizing blank-token test, the same
	 * Content-Type stamp that keeps the write inside the server's JSON boundary check, and the same
	 * refusal-marker-versus-response split.
	 *
	 * Two things it deliberately is NOT.  It is not a general HTTP client - a populate that wants an arbitrary
	 * cross-origin fetch writes one, gets no token, and that is the correct outcome.  And it does not replace the
	 * row-action and declarative-modal submit paths, which have their own typed result settling; this is the REGION
	 * write, for a button a populate painted itself.
	 *
	 * It does not touch the region's loading/ok/error state, in either direction and including on a refusal: a write
	 * is not a paint.
	 */
	function regionWrite(region, url, body, opts) {
		const o = opts || {};
		const init = NS.init;
		if (typeof init?.isSafeMethod !== "function" || typeof init.isSafeDetailUrl !== "function"
			|| typeof init.isBlankToken !== "function" || typeof init.resolveCsrfToken !== "function"
			|| typeof init.resolveCsrfHeaderName !== "function")
			return refuseWrite(region, "runtime-unavailable", "ctx.write cannot run because the view runtime's write"
				+ " guards are unavailable; juneau-regions.js must load after juneau-views.js.");
		const method = String(o.method || "POST").toUpperCase();
		if (init.isSafeMethod(method))
			return refuseWrite(region, "safe-method", "ctx.write refuses the safe method '" + method
				+ "'; a read is a fetch or ctx.fetchDeclared(), not a write.");
		if (!init.isSafeDetailUrl(url))
			return refuseWrite(region, "unsafe-url", "ctx.write refuses '" + url
				+ "': a write URL must be a same-origin path with no scheme and no '..' segment.");
		const host = region.el.closest("[data-juneau-csrf]");
		const token = host ? init.resolveCsrfToken(host) : null;
		if (init.isBlankToken(token))
			return refuseWrite(region, "missing-token", "no CSRF token available - the page did not supply one, so"
				+ " the request was not sent.");
		const headers = {};
		for (const k of keysOf(o.headers)) headers[k] = o.headers[k];
		headers["Content-Type"] = "application/json";
		headers[init.resolveCsrfHeaderName(host)] = token;
		const payload = {};
		for (const k of keysOf(body)) payload[k] = body[k];
		if (o.idempotencyKey != null) payload.idempotencyKey = o.idempotencyKey;
		return sendWrite(region, url, method, headers, payload, o.signal);
	}

	/**
	 * Issues the request under `opts.signal` when one was given and under a FORK otherwise - never under the bare
	 * ctx.signal, so two rapid writes from the same region cancel the first rather than racing.  Forking by default
	 * is what makes it impossible for an author to reproduce the stale-response overwrite here by forgetting.
	 */
	function sendWrite(region, url, method, headers, payload, signal) {
		return fetch(url, {
			method: method,
			headers: headers,
			body: JSON.stringify(payload),
			signal: signal || region.ctx.signal.fork()
		}).then(function (res) {
			return res.text().then(function (text) {
				let data = null;
				try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
				return { ok: res.ok, status: res.status, data: data };
			});
		});
	}

	/**
	 * A refusal is a DIFFERENT KIND OF EVENT from a 500, and collapsing them is how a caller ends up retrying
	 * something that was never sent - so a refusal rejects with a marker, renders a visible message, and issues zero
	 * network traffic.  The message is painted as its own framework-owned alert rather than through the region's
	 * status, because a write must not move the region's loading/ok/error state.
	 */
	function refuseWrite(region, reason, message) {
		window.console.error("JuneauViews.regions: region '" + region.key + "' - " + message);
		const p = window.document.createElement("p");
		p.setAttribute("data-juneau-region-write-error", "");
		p.setAttribute("role", "alert");
		p.textContent = message;
		region.el.appendChild(p);
		const e = new Error(message);
		e.reason = reason;
		return Promise.reject(e);
	}

	// ==================================================================================================================
	// THE PRIMITIVE
	// ==================================================================================================================

	/**
	 * Mints a region from its container.  `key` is the framework's own stable, opaque identity for the region -
	 * unique on the page and the value a fully-qualified emit target names - which is what makes a second expanded
	 * row's region a different subscriber from the first's.
	 */
	function mintRegion(el) {
		const id = el.getAttribute(REGION_ATTR) || "";
		const declaredType = el.getAttribute(REGION_TYPE_ATTR);
		const host = el.getAttribute(REGION_HOST_ATTR) || "";
		return {
			el: el,
			id: id,
			type: REGION_TYPES.indexOf(declaredType) >= 0 ? declaredType : "card-body",
			host: host,
			key: host ? host + "/" + id : id,
			ids: readIds(el),
			params: {},
			declared: { dataUrl: null, renderer: null, lazy: false, refreshMs: null, fields: null, titleFields: null },
			data: null,
			selection: null,
			populateName: el.getAttribute(REGION_POPULATE_ATTR),
			generation: 0,
			state: "idle",
			ctx: null,
			signalCtl: null,
			lastFork: null,
			lastForkUnlink: null,
			messageCtl: null,
			cleanup: null,
			pollTimer: null,
			inFlight: null,
			invoking: false,
			pendingReason: null,
			selfRefreshed: false,
			subs: [],
			deferred: false,
			barrierMember: false,
			releasedWindowOpen: false,
			windowCloseDeferred: false,
			replayed: false,
			firstPopulateDone: false,
			torn: false
		};
	}

	/**
	 * The enclosing identities, as a FLAT BAG of nullable fields rather than a union.  Deliberate: a consumer that
	 * only wants a row id writes ctx.ids.rowId regardless of region type, and the framework hands over what it
	 * already knows instead of making the consumer re-derive it by walking the DOM.  `sectionId` stays in the shape
	 * and is null on a row-detail region: a detail body is one region rather than a strip of sections, so there is no
	 * enclosing section to name, and an author reading it and getting the region's own id back would conclude
	 * otherwise.
	 */
	function readIds(el) {
		const read = function (attr) {
			const host = el.closest("[" + attr + "]");
			return host ? host.getAttribute(attr) : null;
		};
		return {
			viewId: read("data-juneau-view"),
			rowId: read("data-juneau-row-id"),
			pageId: read("data-juneau-page"),
			tabId: read("data-panel-tab"),
			subtabId: read("data-panel-subtab"),
			gridId: read("data-juneau-card-grid"),
			cardId: read("data-juneau-card"),
			sectionId: null
		};
	}

	/**
	 * Enrols one region container.
	 *
	 * IDEMPOTENT PER NODE, which is a copy of the table initializer's survives-the-settle discipline rather than a
	 * new one: the first call mints the region and marks the node, and a second call on a node that already carries a
	 * live region is a NO-OP returning the existing handle - no second populate, no second subscription, no second
	 * teardown entry, no reason transition.  So a second enrolment walk over the same panel enrols nothing new and a
	 * consumer's in-place state survives a tab round-trip.  The mark is cleared by teardown and by nothing else,
	 * deliberately NOT on settle: an implementer copying the in-flight-pending marker's lifecycle instead would clear
	 * it when the first populate returned, and the next walk would re-mint and re-populate every region on the page.
	 *
	 * ENROLMENT IS NOT BARRIER MEMBERSHIP.  A region visible at enrolment populates with reason "initial" and enters
	 * the barrier's outstanding set; a region enrolled HIDDEN is enrolled, owned and torn-down-able, defers its first
	 * populate, and takes reason "activate" when it becomes visible - so it never has an initial populate at all and
	 * was never a barrier member.  Otherwise a card in a tab the user never opens would hold the whole page's initial
	 * broadcast until the deadline, and every tabbed card page would wait out the full deadline before its VISIBLE
	 * card received anything.
	 *
	 * A marked node found DISCONNECTED is a loud named error rather than a silent no-op: a region whose node left the
	 * document without a teardown is a bug.  The detection is real but partial - a host detached and never
	 * reinserted and never re-enrolled is invisible here, because this function is never called again for it.
	 */
	function initRegion(el) {
		if (!isElement(el)) {
			window.console.error("JuneauViews.regions.initRegion: expected a region container element.");
			return null;
		}
		const existing = el._juneauRegion;
		if (existing) {
			if (!isConnected(el))
				window.console.error("JuneauViews.regions: region '" + existing.key
					+ "' is still marked but its container is no longer in the document; it was removed without a"
					+ " teardown, so its subscription and its signal are leaked.");
			return existing;
		}
		if (!HAS_ABORT_CONTROLLER) {
			refuseMissingBaseline(el);
			return null;
		}
		const region = mintRegion(el);
		el._juneauRegion = region;
		liveRegions.push(region);
		el.setAttribute(REGION_STATE_ATTR, "idle");
		armBarrier();
		if (isRegionHidden(el)) {
			region.deferred = true;
			return region;
		}
		enterBarrier(region);
		runPopulate(region, "initial");
		return region;
	}

	/**
	 * The baseline refusal.  A blank container with no error is the worst failure this runtime has, and a silently
	 * signal-less region is a subtler version of it - so every region gets the error state, and the PAGE gets exactly
	 * one console error naming the missing platform feature rather than one per region.
	 */
	function refuseMissingBaseline(el) {
		const message = "the region runtime requires AbortController, which this environment does not provide;"
			+ " no region on this page will populate.";
		if (!baselineReported) {
			baselineReported = true;
			window.console.error("JuneauViews.regions: " + message);
		}
		el.setAttribute(REGION_STATE_ATTR, "error");
		const render = NS.init?.renderAsyncStatus;
		if (typeof render === "function") render(el, "error", message);
	}

	/**
	 * Runs the deferred first populate for a region that has just become visible.  Its reason is "activate" and never
	 * "initial", and it is not a barrier member, so it receives nothing retroactively - not the initial broadcast,
	 * and not a replay even if a released region's window happens to be open at that moment.
	 */
	function activateRegion(region) {
		if (region.torn || region.firstPopulateDone || !region.deferred) return;
		if (!isConnected(region.el) || isRegionHidden(region.el)) return;
		region.deferred = false;
		runPopulate(region, "activate");
	}

	/**
	 * Walks `scopeEl` for region containers and enrols each one.  SYNCHRONOUS and complete in its caller's own turn:
	 * it enrols, it does not populate-and-wait, so a host runtime can truthfully declare its walk finished the moment
	 * its own init returns.  Returns the handles it minted, so a caller that keeps its own ownership record pushes
	 * them itself rather than having this walk reach into a private structure it does not own.
	 */
	function enrolIn(scopeEl) {
		const scope = scopeEl || window.document.body;
		if (typeof scope?.querySelectorAll !== "function") return [];
		const out = [];
		for (const el of scope.querySelectorAll("[" + REGION_ATTR + "]")) {
			const handle = initRegion(el);
			if (handle) out.push(handle);
		}
		return out;
	}

	/**
	 * Invokes the populator.
	 *
	 * The framework CLEARS THE CONTAINER BEFORE EVERY CALL, which makes "populate again" trivially correct and
	 * removes the class of bug where a re-populate appends a second copy.  A consumer that wants update-in-place
	 * instead does it from a bus handler and never re-populates.  A populate in flight is NOT re-entered: a second
	 * trigger aborts the current invocation's signal, waits for the settle, and then runs once.
	 *
	 * A throw is CONTAINED - the region goes to its error state and its siblings stay healthy - and a throw counts as
	 * a settle for the barrier in every one of its three forms: a synchronous throw, a rejected promise, and a
	 * promise that never settles at all (released at the deadline).
	 */
	function runPopulate(region, reason) {
		if (region.torn) return;
		if (region.inFlight || region.invoking) {
			region.pendingReason = reason;
			if (region.inFlight) region.signalCtl.abort(abortReason(REPOPULATE_REASON));
			return;
		}
		const fn = resolvePopulator(region);
		if (!fn) return;
		disposeInvocation(region, REPOPULATE_REASON);
		beginInvocation(region, reason);
		setRegionState(region, "loading");
		let returned;
		region.invoking = true;
		try {
			returned = fn(region.ctx, region.el);
		} catch (e) {
			region.invoking = false;
			failPopulate(region, e);
			return;
		}
		region.invoking = false;
		if (typeof returned?.then === "function") {
			region.inFlight = returned;
			returned.then(
				function (v) { settlePopulate(region, v); },
				function (e) { failPopulate(region, e); });
			return;
		}
		settlePopulate(region, returned);
	}

	/**
	 * An unknown populator name fails VISIBLY rather than silently - a region has no raw value to fall back to, and a
	 * blank container with no error reads as "no data" rather than "broken page".  The region leaves the barrier as
	 * it would on any other settle, so one bad name cannot hold the page's initial broadcast.
	 */
	function resolvePopulator(region) {
		const name = region.populateName == null ? DEFAULT_POPULATOR_NAME : region.populateName;
		const fn = resolve(name);
		if (typeof fn === "function") return fn;
		const message = "no populator is registered under the name '" + name + "'.";
		window.console.error("JuneauViews.regions: region '" + region.key + "' - " + message);
		clearContainer(region.el);
		setRegionState(region, "error", message);
		region.firstPopulateDone = true;
		leaveBarrier(region);
		closeReleasedWindow(region);
		return null;
	}

	function beginInvocation(region, reason) {
		region.signalCtl = new AbortController();
		region.lastFork = null;
		region.lastForkUnlink = null;
		attachFork(region, region.signalCtl.signal);
		if (reason === "message") {
			region.messageCtl = new AbortController();
			linkToParent(region.signalCtl.signal, region.messageCtl);
		}
		region.ctx = buildCtx(region, reason);
	}

	function settlePopulate(region, returned) {
		region.inFlight = null;
		if (typeof returned === "function") region.cleanup = returned;
		if (region.state !== "error") setRegionState(region, "ok");
		finishInvocation(region);
	}

	/**
	 * A rejected promise is a contained throw and registers NO cleanup - a populate that failed before it finished
	 * allocating cannot be trusted to know what to release.
	 */
	function failPopulate(region, e) {
		region.inFlight = null;
		region.cleanup = null;
		clearContainer(region.el);
		setRegionState(region, "error", e?.message ? String(e.message) : "the region's populate failed.");
		window.console.error("JuneauViews.regions: region '" + region.key + "' - its populate failed; the region is"
			+ " in its error state and its siblings are unaffected.", e);
		finishInvocation(region);
	}

	function finishInvocation(region) {
		region.firstPopulateDone = true;
		leaveBarrier(region);
		closeReleasedWindow(region);
		const pending = region.pendingReason;
		if (pending != null) {
			region.pendingReason = null;
			runPopulate(region, pending);
		}
	}

	/**
	 * Asks the framework to re-invoke this region's populate, so re-entrancy stays the framework's problem rather
	 * than a consumer's recursive call.  `reason` tracks the CAUSAL EVENT, not the API that was invoked: a refresh
	 * requested synchronously inside a bus fan-out arrives as "message", and the identical call from a timer or a
	 * click arrives as "refresh".  The field exists so an author can animate a data-driven update differently from a
	 * first paint, and "a message changed my inputs" is the distinction that matters - not which framework function
	 * was on the stack.
	 *
	 * A refresh requested from INSIDE a populate is legal and is coalesced: it runs once after the settle, and a
	 * second in-populate refresh from that re-run is dropped with a warning, so a populate that calls refresh()
	 * unconditionally spins exactly once rather than forever.  A later refresh from outside a populate re-arms the
	 * one permitted spin.
	 */
	function requestRefresh(region) {
		const reason = isDelivering() ? "message" : "refresh";
		if (region.invoking) {
			if (region.selfRefreshed) {
				window.console.warn("JuneauViews.regions: region '" + region.key + "' called ctx.refresh() from"
					+ " inside the populate its own previous refresh triggered; the second spin was coalesced away."
					+ "  A populate that refreshes itself unconditionally spins once, not forever.");
				return;
			}
			region.selfRefreshed = true;
			region.pendingReason = reason;
			return;
		}
		region.selfRefreshed = false;
		runPopulate(region, reason);
	}

	/**
	 * Teardown steps 0 through 5, in a FIXED order, on collapse, destroy, eviction or re-populate.
	 *
	 * Step 0 - the declared poll timer - precedes the abort deliberately: aborting while a timer is still armed
	 * leaves a tick that fires into a region already being torn down.  Steps 3 and 4 are the ones a consumer cannot
	 * get right on its own, which is why they are the framework's: a region that is going away should not keep
	 * receiving messages, and should not drive the page after it is gone.  The order is asserted by an
	 * order-recording test, because a plausible refactor reorders it silently.
	 */
	function disposeInvocation(region, reason) {
		// 0. Clear the region's declared poll timer.
		if (region.pollTimer !== null) {
			window.clearTimeout(region.pollTimer);
			region.pollTimer = null;
		}
		// 1. Abort ctx.signal, which transitively aborts every fork() child and any live messageSignal.
		if (region.signalCtl && !region.signalCtl.signal.aborted) region.signalCtl.abort(abortReason(reason));
		region.messageCtl = null;
		// 2. Invoke the cleanup function, if one was returned - including one resolved from a Promise.
		if (typeof region.cleanup === "function") {
			const cleanup = region.cleanup;
			region.cleanup = null;
			try { cleanup(); } catch (e) {
				window.console.error("JuneauViews.regions: region '" + region.key + "' - its cleanup function threw;"
					+ " the rest of the teardown ran anyway.", e);
			}
		}
		// 3. Unsubscribe every handler the region registered through ctx.on.
		for (const sub of region.subs.slice()) unsubscribeOne(sub);
		region.subs.length = 0;
		// 4. Discard any message this region queued that has not yet been delivered.
		bus.queue = bus.queue.filter(function (e) { return e.fromRegion !== region; });
		// 5. Clear the container.
		clearContainer(region.el);
	}

	/** Tears a region down for good: the invocation steps, then the marks and the page-scoped bookkeeping. */
	function teardownRegion(region) {
		if (!region || region.torn) return;
		region.torn = true;
		disposeInvocation(region, TEARDOWN_REASON);
		region.el.removeAttribute(REGION_STATE_ATTR);
		delete region.el._juneauRegion;
		const i = liveRegions.indexOf(region);
		if (i >= 0) liveRegions.splice(i, 1);
		leaveBarrier(region);
		closeReleasedWindow(region);
	}

	/**
	 * Sweeps every region under `root`, `root` itself included.  IDEMPOTENT over an already-swept root - a second
	 * sweep finds no mark and does nothing - because the sweep sites nest: a table teardown sweeps a subtree that a
	 * nested table's own teardown then sweeps again.
	 */
	function teardownRegionsIn(root) {
		if (typeof root?.querySelectorAll !== "function") return;
		if (root._juneauRegion) teardownRegion(root._juneauRegion);
		for (const el of root.querySelectorAll("[" + REGION_ATTR + "]"))
			if (el._juneauRegion) teardownRegion(el._juneauRegion);
	}

	// ==================================================================================================================
	// EXPORTS
	// ==================================================================================================================

	NS.regions = {
		CONTRACT_VERSION: REGION_CONTRACT_VERSION,
		REGION_ATTR: REGION_ATTR,
		REGION_TYPE_ATTR: REGION_TYPE_ATTR,
		REGION_HOST_ATTR: REGION_HOST_ATTR,
		REGION_POPULATE_ATTR: REGION_POPULATE_ATTR,
		REGION_STATE_ATTR: REGION_STATE_ATTR,
		REGION_TYPES: REGION_TYPES,
		RESERVED_POPULATOR_NAMES: RESERVED_POPULATOR_NAMES,
		FRAMEWORK_KIND_PREFIX: FRAMEWORK_KIND_PREFIX,
		FRAMEWORK_SENDER_KEY: FRAMEWORK_SENDER_KEY,
		BUS_QUEUE_DEPTH_CAP: BUS_QUEUE_DEPTH_CAP,
		REPLAY_BUFFER_CAP: REPLAY_BUFFER_CAP,
		BARRIER_DEADLINE_MS: BARRIER_DEADLINE_MS,
		register: register,
		resolve: resolve,
		initRegion: initRegion,
		activateRegion: activateRegion,
		enrolIn: enrolIn,
		teardownRegion: teardownRegion,
		teardownRegionsIn: teardownRegionsIn,
		registerRuntime: registerRuntime,
		ready: ready,
		defaultPopulate: defaultPopulate,
		builtins: builtins,
		emitFramework: emitFramework,
		selectionChangedMessage: selectionChangedMessage,
		detailToggledMessage: detailToggledMessage,
		tableRedrewMessage: tableRedrewMessage
	};
})();
