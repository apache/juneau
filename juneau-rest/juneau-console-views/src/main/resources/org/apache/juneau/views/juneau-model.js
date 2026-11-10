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

/**
 * juneau-model.js - the client object model: a tree of handles (Page, Card, Table, Row, ...) with stable path ids,
 * a registry, a detach/destroy lifecycle, app and declarative triggers, and the named-handler registry.
 *
 * Loads after juneau-bus.js; the shell (juneau-console.js) hands it the built page.
 */
(function () {
	"use strict";

	const NS = (window.JuneauViews = window.JuneauViews || {});
	if (NS.model) return;                             // double-load guard

	const SUPPORTED_MODEL_VERSIONS = Object.freeze(["1"]);
	const registry = new Map();                       // path -> Handle (attached or re-openable only)
	const EMPTY_HELPERS = Object.freeze({});
	let currentPage = null;

	//-----------------------------------------------------------------------------------------------------------------
	// Path ids
	//-----------------------------------------------------------------------------------------------------------------

	// Anything outside [A-Za-z0-9._-] becomes %XX of the UTF-16 code unit, so a path splits on "/" and ":" safely.
	function encodeSeg(s) {
		return String(s).replace(/[^A-Za-z0-9._-]/g, c => "%" + c.charCodeAt(0).toString(16).toUpperCase().padStart(2, "0"));
	}
	// Kinds whose path segment name differs from the kind name.
	const SEGMENT_NAME = Object.freeze({ rowdetail: "detail", detailtab: "tab", ribbonitem: "item" });
	function segment(h) {
		const name = SEGMENT_NAME[h._kind] || h._kind;
		if (h._singleton) return name;
		return name + ":" + (h._positional ? "#" + h._id : encodeSeg(h._id));
	}

	function deepFreeze(v) {
		if (v !== null && typeof v === "object" && !Object.isFrozen(v)) {
			Object.freeze(v);
			for (const k of Object.keys(v)) deepFreeze(v[k]);
		}
		return v;
	}

	// A relative path ("row:651/detail") walks child segments; a single segment is a depth-first search.
	function findIn(root, selector) {
		const sel = String(selector);
		if (sel.includes("/")) {
			let cur = root;
			for (const seg of sel.split("/")) {
				cur = cur._children.find(c => segment(c) === seg);
				if (!cur) return null;
			}
			return cur;
		}
		const stack = root._children.slice().reverse();
		while (stack.length) {
			const h = stack.pop();
			if (segment(h) === sel) return h;
			for (let i = h._children.length - 1; i >= 0; i--) stack.push(h._children[i]);
		}
		return null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Handle
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// Events and handler plumbing
	//-----------------------------------------------------------------------------------------------------------------

	const EVENTS = deepFreeze({
		page: ["loaded"], card: ["loaded", "rendered"], table: ["loaded", "rendered"], row: ["rendered", "expanded", "collapsed"],
		rowdetail: ["loaded", "rendered"], detailtab: ["loaded", "rendered", "activated"], ribbonitem: ["activated"],
		pagetab: ["activated"], view: ["activated"], chart: ["loaded", "rendered"]
	});
	const bound = new Set();                          // handles that hold declared bindings
	let ledger = [];                                  // events fired before handlers were resolved: {h, b, ev}
	let resolved = false;
	const MSG = {
		"E-JS-116": "handler '%s' is not registered (declare the script that registers it in the page scripts)",
		"E-JS-118": "handler '%s' on '%s' threw: %s",
		"E-JS-119": "inline handler on '%s': %s"
	};
	const fmtMsg = (code, ...args) => { let i = 0; return MSG[code].replace(/%s/g, () => String(args[i++])); };

	// One frozen event for one bubbling level.
	function levelEvent(type, target, source, payload, shared, meta) {
		return Object.freeze({
			type: type, target: target, source: source, payload: payload, meta: Object.freeze(Object.assign({}, meta)),
			stopPropagation: () => { shared.stopped = true; }
		});
	}

	// Logs every handler failure and paints it inside the target's element only (a detached target logs only), reusing
	// one slot per code so a repeating failure updates the message in place rather than stacking banners.
	function handlerError(h, code, msg) {
		console.error("[juneau-model] " + code + ": " + msg);
		const host = h && h._el;
		if (!host || host.nodeType !== 1) return;
		let slot = null;
		for (const n of Array.from(host.querySelectorAll(".jc-card-bus-error")))
			if (n.getAttribute("data-juneau-error") === code && n.parentNode === host) { slot = n; break; }
		if (!slot) {
			slot = document.createElement("div");
			slot.className = "jc-card-bus-error";
			slot.setAttribute("data-juneau-error", code);
			host.appendChild(slot);
		}
		slot.textContent = msg;
	}

	// Runs fn, containing a throw or a rejection so the other handlers on the event still run.
	function runGuarded(h, label, fn) {
		const fail = e => handlerError(h, "E-JS-118", fmtMsg("E-JS-118", label, h.path || h._kind + ":" + h._id, e && e.message ? e.message : e));
		try {
			const r = fn();
			if (r && typeof r.then === "function") r.then(null, fail);
		} catch (e) {
			fail(e);
		}
	}

	// Runs a declared binding for an event; before the handler scripts are resolved a missing handler is queued.
	function runBinding(h, b, ev) {
		if (b.dead) return;
		const fn = b.fn || handlerMap.get(b.name);
		if (!fn) {
			if (!resolved) ledger.push({ h: h, b: b, ev: ev });
			return;
		}
		runGuarded(h, b.name || "inline", () => fn(h, ev));
	}

	let jsonMode = false;                              // true while a JSON block is applied: problems are findings, not throws
	const TEMPLATE_VARS = ["rowKey", "tabId", "cardId"];
	const TOPIC_RE = /^(card|selection|filter|redraw|detail|bulk|cmd|probe|job|badge|bridge|data|zoom|legend|[a-z][a-z0-9-]{0,31}\.[a-z][a-z0-9-]{0,31})(:[A-Za-z0-9_.-]{1,128})?$/;
	const ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;
	const ROLE_RE = /^[a-z][a-z0-9-]{0,31}$/;
	const underKind = (h, kinds) => { for (let x = h; x; x = x._parent) if (kinds.includes(x._kind)) return true; return false; };
	const TEMPLATE_SCOPE = { cardId: null, rowKey: ["row", "rowdetail", "detailtab"], tabId: ["detailtab"] };
	function validateTopicTemplate(h, t) {
		if (typeof t !== "string" || !t) throw model121(h, "listen needs a topic");
		for (const m of t.matchAll(/\{([^}]*)\}/g)) {
			if (!TEMPLATE_VARS.includes(m[1])) throw model121(h, "topic '" + t + "' uses unknown placeholder {" + m[1] + "} (use " + TEMPLATE_VARS.join(", ") + ")");
			const scope = TEMPLATE_SCOPE[m[1]];
			if (scope && !underKind(h, scope)) throw model121(h, "topic '" + t + "': {" + m[1] + "} is only available under " + scope.join(" or "));
		}
		if (!TOPIC_RE.test(t.replace(/\{[^}]*\}/g, "k"))) throw model121(h, "topic '" + t + "' is not a valid topic (family[:key])");
	}
	function expandTemplate(h, t) {
		let ok = true;
		const scope = { rowKey: () => h.closest("row"), tabId: () => h.closest("detailtab"), cardId: () => h.closest("card") };
		const out = t.replace(/\{(\w+)\}/g, (m, k) => {
			const x = scope[k]();
			if (!x) { ok = false; return m; }
			return encodeSeg(x._id);
		});
		return ok ? out : null;
	}

	// Validates a handler reference and returns the binding record.  Names are checked at once for shape; an
	// unregistered name is deferred until the page scripts are in (resolveHandlers), then it is an error.
	function validateHandlerRef(ref, h, where, mode) {
		if (jsonMode) mode = "json";
		if (typeof ref === "function") throw new TypeError(where + ": pass a registered handler name, not a function");
		if (ref && typeof ref === "object" && typeof ref.inline === "string") {
			const page = h.closest("page");
			const compile = NS.model && NS.model.inline && NS.model.inline.compile;
			if (!page || !page._spec.allowInlineHandlers) throw modelFatal("E-JS-119", where, "inline handlers are not enabled for this page");
			if (typeof compile !== "function") throw modelFatal("E-JS-119", where, "no inline handler file is loaded");
			let fn;
			try {
				fn = compile(ref.inline);
			} catch (e) {
				const csp = e && e.name === "EvalError";
				throw modelFatal("E-JS-119", where, csp
					? "the page's Content-Security-Policy blocks dynamic code; allow 'unsafe-eval' in script-src to use inline handlers"
					: String(e && e.message || e));
			}
			return { name: null, fn: fn, ref: ref };
		}
		if (typeof ref !== "string" || !NAME_RE.test(ref)) throw modelFatal("E-JS-114", ref);
		if (!handlerMap.has(ref)) {
			if (ref.startsWith("juneau.")) throw modelFatal("E-JS-123", ref);
			if (resolved && mode !== "json") throw modelFatal("E-JS-116", ref);
		}
		return { name: ref, fn: null, ref: ref };
	}

	// Paints E-JS-116 on the owner for each declared name that is still unregistered.
	function flagUnresolved(h) {
		const all = [].concat(h._declared.actions, h._declared.subscribes, ...Object.values(h._declared.on));
		for (const b of all) {
			if (b.name && !b.fn && !b.dead && !handlerMap.has(b.name)) {
				b.dead = true;
				handlerError(h, "E-JS-116", fmtMsg("E-JS-116", b.name));
			}
		}
	}

	// Called once the page scripts are loaded: flags every declared name that is still unregistered (painted inline on
	// its owner, the rest keep working), then replays the events fired before this point, once, in order.
	function resolveHandlers() {
		resolved = true;
		for (const h of bound) flagUnresolved(h);
		const q = ledger;
		ledger = [];
		for (const x of q) if (!x.h._destroyed) runBinding(x.h, x.b, x.ev);
	}

	class Handle {
		constructor(kind, spec, opts) { this._init(kind, spec, opts); }

		// The constructor body, separate so Card (which extends the shell base) can run it too.
		_init(kind, spec, opts) {
			this._kind = kind;
			this._spec = deepFreeze(spec || {});
			this._id = opts && opts.id != null ? String(opts.id) : kind;
			this._singleton = !!(opts && opts.singleton);
			this._positional = !!(opts && opts.positional);
			this._parent = null;
			this._children = [];
			this._el = null;
			this._data = undefined;
			this._attached = false;
			this._destroyed = false;
			this._warned = false;
			this._appSubs = [];                       // {topic, fn, off}: app bus listeners, suspended on detach
			this._listeners = new Map();              // name -> Set<fn>: app lifecycle listeners
			this._ownerObj = null;
			this._declOffs = [];
			this._declared = { actions: [], on: {}, subscribes: [] };
		}

		get id() { return this._id; }
		get kind() { return this._kind; }
		get spec() { return this._spec; }
		get el() { return this._el; }
		get data() { return this._data; }
		get helpers() { return EMPTY_HELPERS; }      // frozen {}; replaced by the whitelisted helpers later
		get attached() { return this._attached; }
		get destroyed() { return this._destroyed; }
		get parent() { return this._parent; }
		get children() { return this._children.slice(); }
		get path() {
			if (this._kind === "page") return "page/" + encodeSeg(this._id);
			if (!this._parent) return null;
			const pp = this._parent.path;
			return pp == null ? null : pp + "/" + segment(this);
		}

		closest(kind) {
			for (let h = this; h; h = h._parent) if (h._kind === kind) return h;
			return null;
		}
		find(selector) { return findIn(this, selector); }

		// Tree building (framework use): adds a child under this handle.
		_add(child) {
			if (child._parent && child._parent !== this) child._parent._children = child._parent._children.filter(c => c !== child);
			child._parent = this;
			if (!this._children.includes(child)) this._children.push(child);
			return child;
		}

		// Mutators funnel through _guard(): a detached or destroyed handle no-ops, warning once per detach.
		_guard(name) {
			if (this._attached && !this._destroyed) return true;
			if (!this._warned) {
				this._warned = true;
				console.warn("[juneau-model] E-JS-117: handle '" + (this.path || this._kind + ":" + this._id) + "' is detached; " + name + "() was ignored");
			}
			return false;
		}

		setLabel(text) { if (this._guard("setLabel")) this._applyLabel(String(text)); }
		setBadge(n, tone) { if (this._guard("setBadge")) this._applyBadge(n, tone); }
		show() { if (this._guard("show") && this._el) this._el.hidden = false; }
		hide() { if (this._guard("hide") && this._el) this._el.hidden = true; }
		activate() { if (this._guard("activate")) this._applyActivate(); }
		refresh() { if (this._guard("refresh")) this._applyRefresh(); }
		_applyLabel() { /* kind classes override */ }
		_applyBadge() { /* kind classes override */ }
		_applyActivate() { /* kind classes override */ }
		_applyRefresh() { /* kind classes override */ }

		// The draw cycle and the detail re-open call these.
		_attach(el, data) {
			this._el = el;
			if (data !== undefined) this._data = data;
			this._attached = true;
			this._warned = false;
			const p = this.path;
			if (p != null) registry.set(p, this);
			if (this._kind === "page") currentPage = this;
			this._resumeSubs();
			this._wireModel();
		}
		_detach() {
			for (const c of this._children) c._detach();
			if (!this._attached && !this._el) return;
			this._unwireModel();
			this._suspendSubs();
			this._attached = false;
			this._el = null;
		}
		_destroy() {
			if (this._destroyed) return;
			for (const c of this._children.slice()) c._destroy();
			this._detach();
			this._destroyed = true;
			const p = this.path;
			if (p != null && registry.get(p) === this) registry.delete(p);
			if (currentPage === this) currentPage = null;
			if (this._ownerObj) { this._ownerObj.dispose(); this._ownerObj = null; }
			this._listeners.clear();
			this._appSubs = [];
			bound.delete(this);
			if (this._parent) this._parent._children = this._parent._children.filter(c => c !== this);
		}

		// The bus owner for this handle, created on first use.
		_busOwner() {
			if (this._ownerFor) return this._ownerFor();      // shell cards share the shell's owner
			if (!this._ownerObj) this._ownerObj = NS.bus.owner(this._kind === "card" ? this._id : this.path);
			return this._ownerObj;
		}

		//-------------------------------------------------------------------------------------------------------------
		// Triggers
		//-------------------------------------------------------------------------------------------------------------

		publish(topic, payload, opts) {
			if (!this._guard("publish")) return undefined;
			return this._busOwner().publish(topic, payload, opts);
		}

		// An app lifecycle/custom-event listener: a function only.  Returns the remover.
		on(name, fn) {
			if (typeof fn !== "function") throw new TypeError("on('" + name + "', fn): fn must be a function (declare a handler by name with bind())");
			let set = this._listeners.get(name);
			if (!set) this._listeners.set(name, set = new Set());
			set.add(fn);
			return () => { set.delete(fn); };
		}

		// An app bus subscription: a function only.  Suspended while detached, re-made on attach.
		subscribe(topic, fn, opts) {
			if (typeof fn !== "function") throw new TypeError("subscribe('" + topic + "', fn): fn must be a function (declare a handler by name with listen())");
			const rec = { topic: topic, fn: fn, opts: opts, off: null };
			this._appSubs.push(rec);
			if (this._attached) rec.off = this._busOwner().subscribe(topic, fn, opts);
			return () => {
				const i = this._appSubs.indexOf(rec);
				if (i >= 0) this._appSubs.splice(i, 1);
				if (rec.off) { rec.off(); rec.off = null; }
			};
		}

		// Declares a handler (by name) for one of this kind's documented events.
		bind(event, ref) {
			const known = EVENTS[this._kind];
			if (!known || !known.includes(event)) throw model121(this, "event '" + event + "' is not documented for a " + this._kind + " (documented: " + (known ? known.join(", ") : "none") + ")");
			const b = validateHandlerRef(ref, this, "bind('" + event + "')", "imperative");
			(this._declared.on[event] = this._declared.on[event] || []).push(b);
			bound.add(this);
			return this;
		}

		// Declares a bus subscription (by handler name); {rowKey}, {tabId} and {cardId} expand from this handle's scope.
		listen(topicTemplate, ref) {
			validateTopicTemplate(this, topicTemplate);
			const b = validateHandlerRef(ref, this, "listen('" + topicTemplate + "')", "imperative");
			b.template = topicTemplate;
			b.off = null;
			this._declared.subscribes.push(b);
			bound.add(this);
			if (this._attached) this._subscribeDeclared(b);
			return this;
		}

		// Declares a button: action(id, icon, handlerRef, payload) or action({id, icon, label, handler, payload}).
		action(idOrDef, icon, ref, payload) {
			const def = idOrDef && typeof idOrDef === "object" ? idOrDef : { id: idOrDef, icon: icon, handler: ref, payload: payload };
			if (def.id == null || def.id === "") throw model121(this, "action needs an id");
			const id = String(def.id);
			if (!ID_RE.test(id)) throw model121(this, "action id '" + id + "' must match " + ID_RE.source);
			if (this._declared.actions.some(a => a.id === id)) throw model121(this, "action id '" + id + "' is already used");
			const b = validateHandlerRef(def.handler, this, "action('" + id + "')", "imperative");
			b.id = id; b.icon = def.icon; b.rawLabel = def.label; b.label = def.label != null ? String(def.label) : id; b.payload = def.payload;
			this._declared.actions.push(b);
			bound.add(this);
			if (this._attached) this._paintActions();
			return this;
		}

		emit(name, payload) { this._fire(name, payload); return this; }

		// This element's JSON block: its spec with the declared bindings written back under `model`.
		toJSON() { return jsonOf(this); }

		// Destroys this handle's subtree and removes it from its parent.
		remove() { this._destroy(); }

		// Adds a child under this handle with the imperative checks: no second parent, no sibling id collision.
		_addChecked(child) {
			if (child._parent) throw model121(this, "'" + child._kind + ":" + child._id + "' already has a parent (re-parenting is not supported)");
			const seg = segment(child);
			if (this._children.some(c => segment(c) === seg)) throw model121(this, "id '" + child._id + "' is already used by a sibling");
			this._add(child);
			try {
				child._applyAll();
			} catch (e) {
				this._children = this._children.filter(c => c !== child);
				child._parent = null;
				throw e;
			}
			return child;
		}

		// Declares the bindings carried by the spec's `model` block, throwing on the first bad one.
		_applyAll() {
			this._applySpecModel();
			for (const c of this._children) c._applyAll();
		}
		_applySpecModel() {
			if (this._modelApplied) return;
			this._modelApplied = true;
			const b = this._kind === "rowdetail" ? mergeActions(this._spec) : this._spec.model;
			if (!b || typeof b !== "object") return;
			for (const a of b.actions || []) this.action(a);
			for (const ev of Object.keys(b.on || {})) for (const r of [].concat(b.on[ev])) this.bind(ev, r);
			for (const sub of b.subscribes || []) this.listen(sub.topic, sub.handler);
		}

		// Bubbles an event from this handle to the page.  Each level gets its own frozen event (target = that
		// level's handle) sharing one stopped flag.  App listeners run before the declared handlers of a level.
		_fire(name, payload) {
			const shared = { stopped: false };
			for (let h = this; h && !shared.stopped; h = h._parent) {
				const ev = levelEvent(name, h, this, payload === undefined ? {} : payload, shared, null);
				const set = h._listeners.get(name);
				if (set) for (const fn of Array.from(set)) runGuarded(h, "on('" + name + "')", () => fn(ev));
				for (const b of (h._declared.on[name] || [])) runBinding(h, b, ev);
			}
		}

		_subscribeDeclared(b) {
			if (b.off) { b.off(); b.off = null; }
			const topic = expandTemplate(this, b.template);
			if (topic == null) return;
			b.off = this._busOwner().subscribe(topic, (payload, meta) => {
				const ev = levelEvent("topic", this, this, payload, { stopped: false }, { topic: topic, bus: meta });
				runBinding(this, b, ev);
			});
		}

		_paintActions() {
			const el = this._el;
			if (!el || !el.querySelector) return;
			const old = el.querySelector(":scope > .juneau-model-actions:not(.juneau-view-detail-actions)");
			if (old && old.parentNode) old.parentNode.removeChild(old);
			if (!this._declared.actions.length) return;
			const bar = document.createElement("div");
			bar.className = "juneau-model-actions";
			for (const a of this._declared.actions) {
				const onClick = () => {
					const ev = levelEvent("click", this, this, Object.assign({ actionId: a.id }, a.payload), { stopped: false }, null);
					runBinding(this, a, ev);
				};
				const R = NS.ribbon;
				let btn;
				if (R && typeof R.button === "function") btn = R.button(a.label, a.icon, onClick, "icon");
				else {
					btn = document.createElement("button");
					btn.setAttribute("aria-label", a.label);
					btn.addEventListener("click", onClick);
				}
				btn.setAttribute("type", "button");
				btn.setAttribute("data-action-id", a.id);
				bar.appendChild(btn);
			}
			el.appendChild(bar);
		}

		_wireModel() {
			this._unwireModel();
			for (const b of this._declared.subscribes) this._subscribeDeclared(b);
			this._paintActions();
		}
		_unwireModel() {
			for (const b of this._declared.subscribes) if (b.off) { b.off(); b.off = null; }
		}
		_suspendSubs() {
			for (const r of this._appSubs) if (r.off) { r.off(); r.off = null; }
		}
		_resumeSubs() {
			for (const r of this._appSubs) if (!r.off) r.off = this._busOwner().subscribe(r.topic, r.fn, r.opts);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Handler registry
	//-----------------------------------------------------------------------------------------------------------------

	const NAME_RE = /^[a-z][A-Za-z0-9]*(\.[a-z][A-Za-z0-9-]*)+$/;
	const handlerMap = new Map();
	let privateRegistration = true;                   // true until the built-ins are installed, then locked

	// A fatal model error: the shell's error type when the shell is loaded, else a plain Error carrying the code.
	function modelFatal(code, ...args) {
		const jc = window.JuneauConsole;
		if (jc && typeof jc.failCode === "function") return jc.failCode(code, ...args);
		const err = new Error(code + ": " + args.join(" / "));
		err.code = code;
		return err;
	}

	const handlers = {
		register(name, fn) {
			if (typeof name !== "string" || !NAME_RE.test(name)) throw modelFatal("E-JS-114", name);
			if (typeof fn !== "function") throw modelFatal("E-JS-122", name);
			if (!privateRegistration && name.startsWith("juneau.")) throw modelFatal("E-JS-123", name);
			if (handlerMap.has(name)) throw modelFatal("E-JS-115", name);
			handlerMap.set(name, fn);
		},
		get(name) { return handlerMap.get(name); },
		names() { return Array.from(handlerMap.keys()).sort(); }
	};
	Object.freeze(handlers);
	NS.handlers = handlers;

	// The label a server rendered for a handle: its spec's title, else its label.
	function serverLabel(target) { return (target.spec && (target.spec.title || target.spec.label)) || ""; }
	// The count a count handler shows: the event's count, else the target's table row count; null when unknown.
	function countOf(target, event) {
		if (event && event.payload && event.payload.count != null) return event.payload.count;
		const t = target.table;
		return t && typeof t.rowCount === "function" ? t.rowCount() : null;
	}

	handlers.register("juneau.refresh", target => { target.refresh(); });
	handlers.register("juneau.refreshClosest", (target, event) => {
		const c = target.closest((event && event.payload && event.payload.kind) || "card");
		if (c) c.refresh();
	});
	handlers.register("juneau.expand", target => { target.expand(); });
	handlers.register("juneau.collapse", target => { target.collapse(); });
	handlers.register("juneau.activate", target => { target.activate(); });
	handlers.register("juneau.publish", (target, event) => { target.publish(event.payload.topic, event.payload.payload); });
	handlers.register("juneau.countLabel", (target, event) => {
		const n = countOf(target, event);
		if (n != null) target.setLabel(serverLabel(target) + " (" + n + ")");
	});
	handlers.register("juneau.countBadge", (target, event) => {
		const n = countOf(target, event);
		if (n != null) target.setBadge(n, "info");
	});
	const BUILTIN_NAMES = Object.freeze(handlers.names());
	privateRegistration = false;

	//-----------------------------------------------------------------------------------------------------------------
	// Kind classes
	//-----------------------------------------------------------------------------------------------------------------

	const TONES = Object.freeze(["info", "success", "warning", "danger", "neutral"]);
	const CHIP_CLASS = "juneau-view-detail-tab-count";
	// Stock ribbon items' default ids come from the ribbon runtime (one copy), read when needed since load order varies.
	const ribbonDefaultId = type => {
		const m = NS.ribbon && NS.ribbon.stockDefaultIds;
		return m && Object.hasOwn(m, type) ? m[type] : null;
	};

	// A model error (a malformed use of the model): the shell's error when loaded, else a plain Error with the code.
	function model121(h, msg) {
		return modelFatal("E-JS-121", h && h.path ? h.path : (h ? h._kind + ":" + h._id : "model"), msg);
	}

	// Sets the first text node of an element (creating it first when there is none), leaving other children alone.
	function setFirstText(el, text) {
		if (!el) return;
		let n = Array.prototype.find.call(el.childNodes, c => c.nodeType === 3);
		if (!n) {
			n = (el.ownerDocument || document).createTextNode("");
			el.insertBefore(n, el.firstChild);
		}
		n.nodeValue = text;
	}

	// Writes, updates or (n == null) removes the count chip in `host` through textContent only.
	function paintChip(host, n, tone) {
		if (!host) return;
		if (n == null) {
			for (const c of host.querySelectorAll("." + CHIP_CLASS)) c.remove();
			return;
		}
		if (tone !== undefined && !TONES.includes(tone))
			throw new TypeError("setBadge tone must be one of " + TONES.join(", ") + ", not '" + tone + "'");
		let chip = host.querySelector("." + CHIP_CLASS);
		if (!chip) {
			chip = (host.ownerDocument || document).createElement("span");
			chip.className = CHIP_CLASS;
			host.appendChild(chip);
		}
		chip.textContent = String(n);
		if (tone === undefined) chip.removeAttribute("data-tone");
		else chip.setAttribute("data-tone", tone);
	}

	const specId = (spec, opts) => (opts && opts.id != null ? opts.id : spec && spec.id);
	const ofKind = (h, kind) => h._children.filter(c => c._kind === kind);
	const hook = (h, name, ...args) => { const f = h._hooks && h._hooks[name]; return typeof f === "function" ? f(...args) : undefined; };

	// The tree Table for a view table element inside a card of the current page; a standalone handle otherwise.
	function tableFor(el) {
		const ctx = el.__juneauCtx;
		if (currentPage && !(ctx && ctx.nested)) {
			for (const c of currentPage.cards) {
				if (!c._el || c._el === el || typeof c._el.contains !== "function" || !c._el.contains(el)) continue;
				let t = c._children.find(x => x._kind === "table");
				if (!t) t = c._add(new Table({}));
				if (!t._impl || t._impl.element === el) return t;
			}
		}
		return new Table({});
	}

	// Copies a template's declared bindings onto an instance (each with its own subscription state).
	function copyDeclared(from, to) {
		const d = from._declared;
		to._declared.actions = d.actions.map(b => Object.assign({}, b));
		to._declared.on = {};
		for (const k of Object.keys(d.on)) to._declared.on[k] = d.on[k].map(b => Object.assign({}, b));
		to._declared.subscribes = d.subscribes.map(b => Object.assign({}, b, { off: null }));
		to._modelApplied = true;
		if (to._declared.actions.length || to._declared.subscribes.length || Object.keys(to._declared.on).length) bound.add(to);
	}

	// Binds a detail's tab handles to the strip buttons in its element (by data-juneau-strip-tab).
	function bindDetailTabs(d, el) {
		const btns = el && el.querySelectorAll ? Array.from(el.querySelectorAll("[data-juneau-strip-tab]")) : [];
		for (const t of d.tabs) t._attach(btns.find(b => b.dataset.juneauStripTab === t._id) || null);
	}

	// The `model` block that reproduces a handle's declared bindings; undefined when it declares nothing.
	function modelBlock(h) {
		const d = h._declared, m = {};
		if (d.actions.length) {
			m.actions = d.actions.map(b => {
				const a = { id: b.id };
				if (b.icon !== undefined) a.icon = b.icon;
				if (b.rawLabel !== undefined) a.label = b.rawLabel;
				a.handler = b.ref;
				if (b.payload !== undefined) a.payload = clone(b.payload);
				return a;
			});
		}
		const evs = Object.keys(d.on).filter(k => d.on[k].length);
		if (evs.length) {
			m.on = {};
			for (const k of evs) { const refs = d.on[k].map(b => b.ref); m.on[k] = refs.length === 1 ? refs[0] : refs; }
		}
		if (d.subscribes.length) m.subscribes = d.subscribes.map(b => ({ topic: b.template, handler: b.ref }));
		return Object.keys(m).length ? m : undefined;
	}

	// A handle's spec as JSON with its declared bindings under `model`.
	function jsonOf(h, drop) {
		const o = clone(h._spec) || {};
		delete o.model;
		for (const k of drop || []) delete o[k];
		const m = modelBlock(h) || (!h._modelApplied && h._spec.model ? clone(h._spec.model) : undefined);
		if (m) o.model = m;
		return o;
	}

	const ribbonIdFor = a => (a && a.id != null ? String(a.id) : (a ? ribbonDefaultId(a.type) : null));

	const WRAP_TYPE = Object.freeze({ table: "datatables", chart: "chart", switcher: "switcher" });
	const DEFAULT_CONTRACT_VERSION = "1";
	class Page extends Handle {
		constructor(spec, opts) {
			super("page", spec, { id: specId(spec, opts), ...opts });
			const jc = window.JuneauConsole;
			const supported = (jc && jc.SUPPORTED_CONTRACT_VERSIONS) || [DEFAULT_CONTRACT_VERSION];
			if (spec && spec.contractVersion !== undefined && !supported.includes(spec.contractVersion)) throw modelFatal("E-JS-2", spec.contractVersion, supported.join(", "));
			if (spec && spec.modelVersion !== undefined && !SUPPORTED_MODEL_VERSIONS.includes(spec.modelVersion)) throw modelFatal("E-JS-120", String(spec.modelVersion), SUPPORTED_MODEL_VERSIONS.join(", "));
			if (opts && opts.lenient) return;                 // a JSON build reports problems as findings instead
			const s = spec || {};
			if (typeof s.title !== "string" || !s.title) throw model121(this, "a page needs a title");
			if (!Array.isArray(s.nav)) throw model121(this, "a page needs a nav array (it may be empty)");
			if (!Array.isArray(s.activeNav)) throw model121(this, "a page needs an activeNav array (it may be empty)");
			const tabs = (parent, nodes) => {
				for (const n of nodes) {
					if (!n || n.id == null || n.id === "") throw model121(parent, "a nav entry has no id");
					tabs(parent._addChecked(new PageTab(n)), n.children || []);
				}
			};
			tabs(this, s.nav);
			this._applySpecModel();
		}
		get cards() { return ofKind(this, "card"); }
		get tabs() { return ofKind(this, "pagetab"); }
		get findings() { return (this._findings || []).slice(); }
		// Destroys the whole tree (the page and everything under it).
		unmount() { this._destroy(); }
		card(id) { return this.cards.find(c => c._id === String(id)) || null; }

		// Adds a card; a bare Table, Chart or Switcher is wrapped in a card of its type with the child's id.
		add(child) {
			if (!(child instanceof Handle || child instanceof Card)) throw model121(this, "add() needs a handle");
			if (child._kind === "card") {
				if (child._spec.id == null && child._id === "card") throw model121(this, "a card needs an id (card ids are required)");
				return this._addChecked(child);
			}
			const type = WRAP_TYPE[child._kind];
			if (!type) throw model121(this, "a " + child._kind + " cannot be added to a page");
			if (child._parent) throw model121(this, "'" + child._kind + "' already has a parent (re-parenting is not supported)");
			const cid = child._spec.id;
			if (cid == null || cid === "") throw model121(this, "a " + child._kind + " added to a page needs an id (card ids are required)");
			const card = new Card({ id: cid, type: type });
			card._add(child);
			try {
				this._addChecked(card);
			} catch (e) {
				card._children = [];
				child._parent = null;
				throw e;
			}
			return child;
		}

		// Constructs the registered card class for `type` (E-JS-4 when unknown) and adds it.
		addCard(type, spec) {
			const full = Object.assign({}, spec, { type: type });
			if (full.id == null || full.id === "") throw model121(this, "a card needs an id (card ids are required)");
			const jc = window.JuneauConsole;
			const cls = jc && typeof jc.cardClass === "function" ? jc.cardClass(type) : undefined;
			let h;
			if (cls && cls.prototype instanceof Card) {
				h = new cls(full);
				h._spec = deepFreeze(full);
				h._id = String(full.id);
			} else if (jc && typeof jc.cardTypes === "function" && jc.cardTypes().includes(type)) {
				h = new Card(full);
			} else {
				throw modelFatal("E-JS-4", String(full.id), type, jc && typeof jc.cardTypes === "function" ? jc.cardTypes().join(", ") : "");
			}
			return this._addChecked(h);
		}
		addTab(spec) {
			if (!spec || spec.id == null || spec.id === "") throw model121(this, "a nav entry has no id");
			return this._addChecked(new PageTab(spec));
		}

		// Mounts this page through the shell: the page's own handles are the ones that get attached.
		mount(opts) {
			const jc = window.JuneauConsole;
			if (!jc || typeof jc.mount !== "function") throw modelFatal("E-JS-1", "the console shell (juneau-console.js) is not loaded");
			const instances = {};
			for (const c of this.cards) if (c.constructor !== Card) instances[c._id] = c;
			mountingPage = this;
			try {
				jc.mount(this.toJSON(), Object.assign({}, opts, { instances: instances }));
			} finally {
				mountingPage = null;
			}
			return this;
		}

		// The full page contract.
		toJSON() {
			const o = jsonOf(this, ["cards", "nav"]);
			if (o.contractVersion === undefined) o.contractVersion = DEFAULT_CONTRACT_VERSION;
			o.nav = this.tabs.map(t => t.toJSON());
			if (o.activeNav === undefined) o.activeNav = [];
			o.cards = this.cards.map(c => c.toJSON());
			if (o.modelVersion === undefined && hasModel(o)) o.modelVersion = SUPPORTED_MODEL_VERSIONS[0];
			return o;
		}
		_applyLabel(text) { const d = this._el && this._el.ownerDocument; if (d) d.title = text; }
		_applyRefresh() { hook(this, "refresh", this); }
	}

	class PageTab extends Handle {
		constructor(spec, opts) { super("pagetab", spec, { id: specId(spec, opts), ...opts }); }
		toJSON() {
			const o = jsonOf(this, ["children"]);
			const kids = ofKind(this, "pagetab").map(t => t.toJSON());
			if (kids.length) o.children = kids;
			return o;
		}
		_applyLabel(text) { setFirstText(this._el, text); }
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyActivate() { hook(this, "navigate", this); }
	}

	// The card handle.  It extends the shell's Card base, so the shell's own members (refresh, publish, subscribe)
	// win over the generic Handle ones of the same name; every other Handle member is copied onto it.
	const CardBase = (window.JuneauConsole && window.JuneauConsole.Card) || class {};
	class Card extends CardBase {
		constructor(spec, opts) {
			super();
			this._init("card", spec, { id: specId(spec, opts), ...opts });
		}
		get el() { return this._el; }
		set el(v) { this._el = v; }
		get spec() { return this._spec; }
		set spec(v) {
			this._spec = v;
			if (v && v.id != null) this._id = String(v.id);
		}
		toJSON() {
			const o = jsonOf(this);
			const t = ofKind(this, "table")[0];
			if (t) o.table = t.toJSON();
			for (const c of this._children) if (c._kind === "chart" || c._kind === "switcher") Object.assign(o, c.toJSON());
			return o;
		}
		_title() { return this._el && this._el.querySelector(".jc-card-title"); }
		_applyLabel(text) { setFirstText(this._title(), text); }
		_applyBadge(n, tone) { paintChip(this._title() || this._el, n, tone); }
	}
	{
		const own = new Set(Object.getOwnPropertyNames(Card.prototype));
		for (const k of Object.getOwnPropertyNames(CardBase.prototype)) own.add(k);
		for (const k of Object.getOwnPropertyNames(Handle.prototype)) {
			if (k === "constructor" || own.has(k)) continue;
			Object.defineProperty(Card.prototype, k, Object.getOwnPropertyDescriptor(Handle.prototype, k));
		}
	}

	class Table extends Handle {
		constructor(spec, opts) { super("table", spec, { id: "table", singleton: true, ...opts }); this._impl = null; }
		get element() { return this._impl ? this._impl.element : undefined; }
		get dataTable() { return this._impl ? this._impl.dataTable : undefined; }
		get viewDef() { return this._impl ? this._impl.viewDef : undefined; }
		contextOf(tr) { return this._impl ? this._impl.contextOf(tr) : undefined; }
		get rows() { return ofKind(this, "row"); }
		get detail() { return ofKind(this, "rowdetail").find(d => d._template) || null; }
		// The table's ribbon handle; it always exists (empty unless items were declared).
		get ribbon() {
			let r = ofKind(this, "ribbon")[0];
			if (!r) { r = new Ribbon({}); this._add(r); }
			return r;
		}

		// Defines the row-detail template (its tabs and bindings become the definition every expanded row copies).
		setDetail(spec) {
			const old = this.detail;
			if (old) old._destroy();
			const d = this._addChecked(new RowDetail(spec || {}, { template: true }));
			for (const tab of (spec && spec.tabs) || []) d.addTab(tab);
			return d;
		}
		toJSON() {
			const o = jsonOf(this, ["id"]);
			const r = ofKind(this, "ribbon")[0];
			if (r) {
				const items = r.toJSON();
				if (items.length) o.ribbon = items;
			}
			const d = this.detail;
			if (d) o.detail = d.toJSON();
			return o;
		}

		// The Table for a mounted view table element: the card's tree table when the element sits inside a card of
		// the current page, else a standalone handle.  `impl` supplies the live DataTables/viewDef/command members.
		static fromContext(el, impl) {
			let t = el.__juneauModelTable;
			// A handle made before the page tree existed is standalone (no path); once the tree is attached, adopt the tree's table.
			if (!t || t._destroyed || (t.path == null && currentPage && !(el.__juneauCtx && el.__juneauCtx.nested))) {
				const prior = t && !t._destroyed ? t : null;
				t = tableFor(el);
				if (prior && prior !== t) {
					// The ribbon was built against the standalone handle: the tree's items take over its elements.
					for (const pi of prior.ribbon.items) {
						const ti = pi._el ? t.ribbon.item(pi._id) : null;
						if (ti && !ti._attached) ti._attach(pi._el);
					}
					prior._destroy();
				}
				el.__juneauModelTable = t;
			}
			t._impl = impl;
			if (!t._attached) t._attach(el);
			return t;
		}

		_rowFor(tr) { return this.rows.find(r => r._el === tr) || null; }
		_newRow(key, positional) {
			const r = new Row({}, { key: String(key), positional: positional });
			r._hooks = {
				expand: row => { if (this._impl && this._impl.toggleRow) this._impl.toggleRow(row._el); },
				collapse: row => { if (this._impl && this._impl.toggleRow) this._impl.toggleRow(row._el); }
			};
			return this._add(r);
		}

		// Called before DataTables drops the current draw's rows (and child rows): positional rows go for good, keyed
		// rows are detached.  A keyed row's open detail is kept detached only while a re-open is pending.
		_beginDraw() {
			for (const r of this.rows) {
				if (r._positional) { r._destroy(); continue; }
				if (!this._reopenPending) for (const d of ofKind(r, "rowdetail")) d._destroy();
				r._detach();
			}
		}

		// Called after the draw with the rows now present ({key, el, data}; key null = positional): evicts keyed rows
		// that are gone, creates handles for new rows and re-attaches the rest.
		_endDraw(rowsNow) {
			const seen = new Set();
			(rowsNow || []).forEach((n, i) => {
				let r = n.key == null ? null : this.row(n.key);
				if (!r) r = this._newRow(n.key == null ? i : n.key, n.key == null);
				seen.add(r);
				r._attach(n.el, n.data);
			});
			for (const r of this.rows) if (!seen.has(r)) r._destroy();
		}

		// The re-open seam: re-attaches a kept (detached) detail of a keyed row to its new element.
		reattachDetail(rowKey, detailEl, data) {
			const r = this.row(rowKey), d = r && r.detail;
			if (!d) return null;
			d._attach(detailEl, data);
			bindDetailTabs(d, detailEl);
			return d;
		}

		// A row-detail panel was opened: create the Row's detail instance and tab handles from the template.
		_rowExpanded(tr, panel) {
			let r = this._rowFor(tr);
			if (!r) {
				const k = tr.getAttribute ? tr.getAttribute("data-juneau-row-id") : null;
				r = k != null && k !== "" ? (this.row(k) || this._newRow(k, false)) : this._newRow(this.rows.length, true);
				r._attach(tr);
			}
			for (const old of ofKind(r, "rowdetail")) old._destroy();
			const tpl = this.detail;
			const d = new RowDetail(tpl ? tpl.spec : {}, {});
			r._add(d);
			if (tpl) {
				copyDeclared(tpl, d);
				for (const tt of tpl.tabs) {
					const x = new DetailTab(tt.spec);
					copyDeclared(tt, x);
					d._add(x);
				}
			}
			d._attach(panel);
			bindDetailTabs(d, panel);
			r._fire("expanded", {});
			d._fire("rendered", {});
		}
		_rowCollapsed(tr) {
			const r = this._rowFor(tr);
			if (!r) return;
			for (const d of ofKind(r, "rowdetail")) d._destroy();
			r._fire("collapsed", {});
		}
		_rowDetailLoaded(tr) {
			const r = this._rowFor(tr), d = r && r.detail;
			if (d) d._fire("loaded", {});
		}

		// Ribbon building (called by the ribbon builder): the item handle for one declared item, or null when the
		// item has no usable id (the tree build already reported that).
		_ribbonItem(a) {
			const r = this.ribbon || this._add(new Ribbon({}));
			const id = ribbonIdFor(a);
			if (id == null) return null;
			let it = r.item(id);
			if (!it) {
				try { it = new RibbonItem(a); } catch (e) { return null; }
				r._add(it);
			}
			return it;
		}
		_ribbonEnd(bar) { const r = this.ribbon; if (r) r._attach(bar); }
		rowCount() { return this._impl && typeof this._impl.rowCount === "function" ? this._impl.rowCount() : this.rows.length; }
		row(key) { return this.rows.find(r => r._id === String(key)) || null; }
		selection() { return this._impl && this._impl.selection ? this._impl.selection() : []; }
		_call(name, ...args) { if (this._guard(name) && this._impl && this._impl[name]) return this._impl[name](...args); return undefined; }
		select(ids) { return this._call("select", ids); }
		clearSelection() { return this._call("clearSelection"); }
		setFilter(patch) { return this._call("setFilter", patch); }
		pausePolling() { return this._call("pausePolling"); }
		resumePolling() { return this._call("resumePolling"); }
		collapseAll() { return this._call("collapseAll"); }
		reload(opts) { return this._call("reload", opts || {}); }
		applyCmdPatch(cmd) { return this._call("applyCmdPatch", cmd); }
		_applyRefresh() { this.reload({}); }
	}

	class Row extends Handle {
		constructor(spec, opts) {
			const key = (opts && opts.key != null ? opts.key : (spec && (spec.key != null ? spec.key : spec.id)));
			super("row", spec, { id: key, positional: !!(opts && opts.positional), ...opts });
		}
		get key() { return this._id; }
		get positional() { return this._positional; }
		get detail() { return ofKind(this, "rowdetail")[0] || null; }
		get expanded() { return this.detail !== null; }
		expand() { if (this._guard("expand") && !this.expanded) hook(this, "expand", this); }
		collapse() { if (this._guard("collapse") && this.expanded) hook(this, "collapse", this); }
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyRefresh() { const t = this.closest("table"); if (t) t.refresh(); }
	}

	class RowDetail extends Handle {
		constructor(spec, opts) {
			super("rowdetail", spec, { id: "detail", singleton: true, ...opts });
			this._template = !!(opts && opts.template);
			this._activeId = null;
		}
		get template() { return this._template; }
		// Declared actions sit in the header line when the panel has one, else at the end of the panel.
		_paintActions() {
			super._paintActions();
			const el = this._el;
			if (!el || !el.querySelector) return;
			const bar = el.children && el.children.length !== undefined ? Array.prototype.find.call(el.children, c => c.classList && c.classList.contains("juneau-model-actions")) : null;
			const head = el.querySelector(".juneau-view-detail-header");
			if (bar && head && bar.parentNode !== head) head.appendChild(bar);
		}
		toJSON() {
			const o = jsonOf(this);
			const m = o.model, acts = m && m.actions;
			if (m) { delete m.actions; if (!Object.keys(m).length) delete o.model; }
			const keep = ((this._spec && this._spec.actions) || []).filter(a => !(a && a.type === "handler")).map(clone);
			const hs = (acts || []).map(a => Object.assign({ type: "handler" }, a));
			if (keep.length || hs.length) o.actions = keep.concat(hs); else delete o.actions;
			if (this._template) {
				const tabs = this.tabs.map(t => t.toJSON());
				if (tabs.length) o.tabs = tabs; else delete o.tabs;
			}
			return o;
		}
		get tabs() { return ofKind(this, "detailtab"); }
		tab(id) { return this.tabs.find(t => t._id === String(id)) || null; }
		get activeTab() { return (this._activeId != null && this.tab(this._activeId)) || this.tabs[0] || null; }
		addTab(spec) {
			if (!spec || spec.id == null || spec.id === "") throw model121(this, "a detail tab has no id (tab ids are required)");
			if (this.tab(spec.id)) throw model121(this, "detail tab id '" + spec.id + "' is already used");
			return this._addChecked(new DetailTab(spec));
		}
		removeTab(id) { const t = this.tab(id); if (t) t._destroy(); }
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyRefresh() { hook(this, "refresh", this); }
	}

	class DetailTab extends Handle {
		constructor(spec, opts) { super("detailtab", spec, { id: specId(spec, opts), ...opts }); }
		get table() { return this._children.find(c => c._kind === "table") || null; }
		get chart() { return this._children.find(c => c._kind === "chart") || null; }
		// The strip button's label is its own text next to an optional count chip: set the first text node only.
		_applyLabel(text) { setFirstText(this._el, text); }
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyActivate() {
			const w = this.closest("rowdetail");
			if (!w) return;
			w._activeId = this._id;
			hook(w, "activateTab", this._id);
		}
		_applyRefresh() { hook(this, "refresh", this); }
	}

	class Ribbon extends Handle {
		constructor(spec, opts) { super("ribbon", spec, { id: "ribbon", singleton: true, ...opts }); }
		get items() { return ofKind(this, "ribbonitem"); }
		item(id) { return this.items.find(i => i._id === String(id)) || null; }
		addItem(spec) {
			const it = new RibbonItem(spec);
			if (this.item(it._id)) throw model121(this, "ribbon item id '" + it._id + "' is already used: give it an explicit id");
			return this._addChecked(it);
		}
		// The ribbon's items as the JSON array, dividers and original order kept; items added later follow.
		toJSON() {
			const used = new Set(), out = [];
			for (const it of (this._spec && this._spec.items) || []) {
				if (!it || it.type === "divider") { out.push(clone(it)); continue; }
				const h = this.item(ribbonIdFor(it));
				if (h) { used.add(h); out.push(h.toJSON()); }
			}
			for (const h of this.items) if (!used.has(h)) out.push(h.toJSON());
			return out;
		}
		removeItem(id) { const i = this.item(id); if (i) i._destroy(); }
	}

	class RibbonItem extends Handle {
		constructor(spec, opts) {
			const type = spec && spec.type;
			if (type === "divider") throw new TypeError("a divider is not addressable and has no handle");
			let id = specId(spec, opts);
			if (id == null) id = ribbonDefaultId(type);
			if (id == null) throw model121(null, "ribbon item of type '" + type + "' needs an id");
			super("ribbonitem", spec, { id, ...opts });
		}
		_applyLabel(text) {
			if (!this._el) return;
			this._el.setAttribute("aria-label", text);
			this._el.dataset.jcTip = text;
		}
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyActivate() { hook(this, "activate", this); }
		_paintActions() { /* an item's own element is its button: declared actions never paint a bar inside it */ }
		// Runs the item's declared `click` action, if any; true when one ran (it replaces the stock behaviour).
		_click() {
			const a = this._declared.actions.find(x => x.id === "click");
			if (!a) return false;
			runBinding(this, a, levelEvent("click", this, this, Object.assign({ actionId: "click" }, a.payload), { stopped: false }, null));
			return true;
		}
		disable() { if (this._guard("disable") && this._el) this._el.disabled = true; }
		enable() { if (this._guard("enable") && this._el) this._el.disabled = false; }
	}

	// Switcher and its views: the handle surface only; the views runtime and the chart card fill in the behaviour.
	class Switcher extends Handle {
		constructor(spec, opts) { super("switcher", spec, { id: "switcher", singleton: true, ...opts }); this._activeId = null; }
		get views() { return ofKind(this, "view"); }
		view(id) { return this.views.find(v => v._id === String(id)) || null; }
		get active() { return (this._activeId != null && this.view(this._activeId)) || this.views[0] || null; }
		_applyRefresh() { const a = this.active; if (a) a.refresh(); }
		// Adds a view ({id, label, card | sections}); the switcher card supplies the behaviour.
		addView(def) { return this._guard("addView") ? hook(this, "addView", def) : undefined; }
	}

	class SwitcherView extends Handle {
		constructor(spec, opts) { super("view", spec, { id: specId(spec, opts), ...opts }); }
		get sections() { return ofKind(this, "detailtab"); }
		section(id) { return this.sections.find(s => s._id === String(id)) || null; }
		addSection(def) { return this._guard("addSection") ? hook(this, "addSection", def) : undefined; }
		// The nested card handles built so far in this view, in the order they were added.
		cards() { return ofKind(this, "card"); }
		_applyLabel(text) { setFirstText(this._el, text); }
		_applyBadge(n, tone) { paintChip(this._el, n, tone); }
		_applyActivate() { if (this._parent) this._parent._activeId = this._id; hook(this, "activate", this); }
		_applyRefresh() { for (const c of ofKind(this, "card")) c.refresh(); }
	}

	class Chart extends Handle {
		constructor(spec, opts) { super("chart", spec, { id: "chart", singleton: true, ...opts }); this._impl = null; }
		export(type, opts) { return this._impl && this._impl.export ? this._impl.export(type, opts) : undefined; }
		getOption() { return this._impl && this._impl.getOption ? this._impl.getOption() : undefined; }
		chart() { return this._impl && this._impl.chart ? this._impl.chart() : undefined; }
		_applyRefresh() { hook(this, "refresh", this); }
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Building the tree from JSON
	//-----------------------------------------------------------------------------------------------------------------

	const clone = v => (v === undefined ? undefined : JSON.parse(JSON.stringify(v)));
	// In a JSON build the grammar/namespace problems are all E-JS-121 findings; the other codes keep their own.
	const jsonCode = e => (e && e.code && e.code !== "E-JS-114" && e.code !== "E-JS-123" ? e.code : "E-JS-121");

	// Applies a `model` block (actions, on, subscribes) to a handle; each problem becomes a finding, never a throw.
	function applyModel(h, block, findings) {
		if (!block || typeof block !== "object") return;
		h._modelApplied = true;
		const run = fn => {
			jsonMode = true;
			try { fn(); } catch (e) { findings.push({ code: jsonCode(e), path: h.path, message: e && e.message, owner: h }); } finally { jsonMode = false; }
		};
		for (const a of block.actions || []) run(() => h.action(a));
		for (const ev of Object.keys(block.on || {})) for (const r of [].concat(block.on[ev])) run(() => h.bind(ev, r));
		for (const sub of block.subscribes || []) run(() => h.listen(sub.topic, sub.handler));
		if (resolved) flagUnresolved(h);
	}

	const alreadyDeclared = h => h._modelApplied || h._declared.actions.length || h._declared.subscribes.length || Object.keys(h._declared.on).length;

	// Builds the handle tree for a page contract.  `opts.instances` maps card ids to pre-built handles, which win.
	// Problems found while building are collected on page.findings (painted by the mount), never thrown.
	function fromJSON(json, opts) {
		const spec = clone(json) || {};
		const instances = (opts && opts.instances) || {};
		const page = new Page(spec, Object.assign({ lenient: true }, opts && opts.id != null ? { id: opts.id } : null));
		const findings = page._findings = [];
		const jobs = [[page, spec.model]];
		const add = (parent, h, block) => {
			const seg = segment(h);
			if (parent._children.some(c => c !== h && segment(c) === seg)) throw model121(parent, "id '" + h._id + "' is already used by a sibling");
			parent._add(h);
			jobs.push([h, block]);
			return h;
		};
		const bad = (parent, code, msg) => findings.push({ code: code, path: parent.path, message: msg, owner: parent });
		const guard = (parent, fn) => { try { return fn(); } catch (e) { bad(parent, jsonCode(e), e && e.message); return null; } };

		const navTabs = (parent, nodes) => {
			for (const n of nodes || []) {
				if (!n || n.id == null || n.id === "") { bad(parent, "E-JS-121", "a nav entry has no id"); continue; }
				const t = guard(parent, () => add(parent, new PageTab(n), n.model));
				if (t) navTabs(t, n.children);
			}
		};
		navTabs(page, spec.nav);

		for (const card of spec.cards || []) {
			if (!card || card.id == null || card.id === "") { bad(page, "E-JS-121", "a card has no id (card ids are required)"); continue; }
			const pre = Object.hasOwn(instances, card.id) ? instances[card.id] : null;
			const cls = !pre && window.JuneauConsole && typeof window.JuneauConsole.cardClass === "function" ? window.JuneauConsole.cardClass(card.type) : undefined;
			let h = pre;
			if (!h) h = cls && cls.prototype instanceof Card ? new cls() : new Card(card);
			h._id = String(card.id);
			if (h._spec !== card) h._spec = deepFreeze(card);
			if (h._kind == null) h._kind = "card";
			if (!guard(page, () => add(page, h, alreadyDeclared(h) ? undefined : card.model))) continue;
			cardRules(h, card, bad, cls, opts);
			if (card.table && typeof card.table === "object") buildTable(h, card.table, add, guard);
		}
		pageRules(page, spec, bad);
		for (const [h, block] of jobs) applyModel(h, block, findings);
		return page;
	}

	const USER_MENU_KEYS = Object.freeze(["label", "initials", "avatar", "items"]);
	const CARD_KEYS = Object.freeze(["id", "type", "src", "title", "template", "class", "visibleWhen", "publishes", "subscribes", "model", "table", "sanitize"]);
	const RESERVED_BODY_KEYS = Object.freeze(["publishes", "subscribes", "model"]);

	// The page-level rules the Java PageSpec enforces: the header's user menu takes only its documented keys.
	function pageRules(page, spec, bad) {
		const um = spec.header && spec.header.userMenu;
		if (um && typeof um === "object") for (const k of Object.keys(um)) if (!USER_MENU_KEYS.includes(k)) bad(page, "E-JS-121", "header userMenu key '" + k + "' is not allowed; allowed: " + USER_MENU_KEYS.join(", "));
	}

	// The card-level rules the Java builders enforce and the schema cannot express.
	function cardRules(h, card, bad, cls, opts) {
		for (const s of card.subscribes || []) {
			if (s && typeof s === "object" && s.as !== undefined && !(typeof s.as === "string" && ROLE_RE.test(s.as))) bad(h, "E-JS-121", "subscription to '" + s.topic + "': role '" + s.as + "' must match " + ROLE_RE.source);
		}
		for (const k of Object.keys(card)) {
			const v = card[k];
			if (CARD_KEYS.includes(k) || !v || typeof v !== "object" || Array.isArray(v)) continue;
			for (const r of RESERVED_BODY_KEYS) if (Object.hasOwn(v, r)) bad(h, "E-JS-121", "card body '" + k + "' may not set the reserved key '" + r + "'");
		}
		if (cls && typeof cls.requiredPacks === "function") {
			const have = opts && opts.packs;
			for (const pk of cls.requiredPacks(card) || []) if (have ? !have.includes(pk) : NS[pk] === undefined) bad(h, "E-JS-121", "card type '" + card.type + "' requires the '" + pk + "' pack, which is not loaded");
		}
	}

	function buildTable(card, tspec, add, guard) {
		const table = add(card, new Table(tspec), tspec.model);
		if (Array.isArray(tspec.ribbon)) {
			const ribbon = add(table, new Ribbon({ items: tspec.ribbon }), undefined);
			for (const it of tspec.ribbon) {
				if (!it || it.type === "divider") continue;
				guard(ribbon, () => {
					const item = new RibbonItem(it);
					if (ribbon.item(item._id)) throw model121(ribbon, "ribbon item id '" + item._id + "' is already used: give it an explicit id");
					add(ribbon, item, it.model);
				});
			}
		}
		if (tspec.detail && typeof tspec.detail === "object") {
			const d = tspec.detail;
			const detail = add(table, new RowDetail(d, { template: true }), mergeActions(d));
			for (const tab of d.tabs || []) {
				if (!tab || tab.id == null || tab.id === "") { guard(detail, () => { throw model121(detail, "a detail tab has no id (tab ids are required)"); }); continue; }
				guard(detail, () => {
					if (detail.tab(tab.id)) throw model121(detail, "detail tab id '" + tab.id + "' is already used");
					add(detail, new DetailTab(tab), tab.model);
				});
			}
		}
	}

	// A detail's header actions[] may carry handler actions (type "handler") next to its model block.
	function mergeActions(d) {
		const hs = (d.actions || []).filter(a => a && a.type === "handler");
		if (!hs.length) return d.model;
		const m = d.model || {};
		return Object.assign({}, m, { actions: hs.concat(m.actions || []) });
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Validation: schema port, model rules, bus wiring
	//-----------------------------------------------------------------------------------------------------------------

	// Exactly the keyword set of the Java SchemaSubsetValidator.KEYWORDS (a test compares the two).
	const SCHEMA_KEYWORDS = Object.freeze(["$schema", "$id", "$defs", "$ref", "title", "description", "default", "type", "const", "enum", "required",
		"properties", "additionalProperties", "propertyNames", "items", "pattern", "minLength", "maxLength", "minItems", "minimum", "allOf", "anyOf", "oneOf", "not"]);
	let modelSchema = null;
	let validateOnMount = false;
	const jsonEq = (a, b) => JSON.stringify(a) === JSON.stringify(b);
	const isObj = v => v !== null && typeof v === "object" && !Array.isArray(v);
	function typeOk(t, v) {
		switch (t) {
			case "object": return isObj(v);
			case "array": return Array.isArray(v);
			case "string": return typeof v === "string";
			case "boolean": return typeof v === "boolean";
			case "integer": return typeof v === "number" && Number.isInteger(v);
			case "number": return typeof v === "number";
			case "null": return v === null;
			default: throw new Error("Unsupported type: " + t);
		}
	}

	function schemaCheck(root, node, v, path, errors) {
		if (typeof node === "boolean") { if (!node) errors.push({ path: path, message: "not allowed" }); return; }
		const sub = (n, val, p) => { const o = []; schemaCheck(root, n, val, p, o); return o; };
		if (node.$ref !== undefined) {
			if (typeof node.$ref !== "string" || !node.$ref.startsWith("#/$defs/")) throw new Error("Unsupported $ref: " + node.$ref);
			const target = root.$defs && root.$defs[node.$ref.slice(8)];
			if (target === undefined) throw new Error("Dangling $ref: " + node.$ref);
			schemaCheck(root, target, v, path, errors);
		}
		if (node.type !== undefined) {
			const ts = Array.isArray(node.type) ? node.type : [node.type];
			if (!ts.some(t => typeOk(t, v))) { errors.push({ path: path, message: "expected " + JSON.stringify(node.type) }); return; }
		}
		if (node.const !== undefined && !jsonEq(node.const, v)) errors.push({ path: path, message: "must equal " + JSON.stringify(node.const) });
		if (node.enum !== undefined && !node.enum.some(x => jsonEq(x, v))) errors.push({ path: path, message: "must be one of " + JSON.stringify(node.enum) });
		if (typeof v === "string") {
			const len = Array.from(v).length;
			if (node.minLength !== undefined && len < node.minLength) errors.push({ path: path, message: "shorter than " + node.minLength });
			if (node.maxLength !== undefined && len > node.maxLength) errors.push({ path: path, message: "longer than " + node.maxLength });
			if (node.pattern !== undefined && !new RegExp(node.pattern).test(v)) errors.push({ path: path, message: "does not match " + node.pattern });
		}
		if (node.minimum !== undefined) {
			if (typeof node.minimum !== "number") throw new Error(path + ": schema 'minimum' must be a number");
			if (typeof v === "number" && v < node.minimum) errors.push({ path: path, message: "less than " + node.minimum });
		}
		if (Array.isArray(v)) {
			if (node.minItems !== undefined && v.length < node.minItems) errors.push({ path: path, message: "fewer than " + node.minItems + " items" });
			if (node.items !== undefined) v.forEach((x, i) => schemaCheck(root, node.items, x, path + "[" + i + "]", errors));
		}
		if (isObj(v)) {
			for (const r of node.required || []) if (!Object.hasOwn(v, r)) errors.push({ path: path, message: "missing required '" + r + "'" });
			const props = node.properties || {};
			for (const k of Object.keys(v)) {
				if (node.propertyNames !== undefined) schemaCheck(root, node.propertyNames, k, path + "{" + k + "}", errors);
				if (Object.hasOwn(props, k)) schemaCheck(root, props[k], v[k], path + "." + k, errors);
				else if (node.additionalProperties !== undefined) schemaCheck(root, node.additionalProperties, v[k], path + "." + k, errors);
			}
		}
		for (const n of node.allOf || []) schemaCheck(root, n, v, path, errors);
		if (node.anyOf !== undefined && !node.anyOf.some(n => sub(n, v, path).length === 0)) errors.push({ path: path, message: "matches none of anyOf" });
		if (node.oneOf !== undefined) {
			const n = node.oneOf.filter(x => sub(x, v, path).length === 0).length;
			if (n !== 1) errors.push({ path: path, message: "matches " + n + " of oneOf (expected exactly 1)" });
		}
		if (node.not !== undefined && sub(node.not, v, path).length === 0) errors.push({ path: path, message: "matches a forbidden shape (not)" });
	}

	function schemaFindings(schema, json) {
		const errors = [];
		try { schemaCheck(schema, schema, json, "$", errors); } catch (e) { errors.push({ path: "$", message: e.message }); }
		return errors.map(e => ({ code: "E-JS-121", path: e.path, message: "schema: " + e.path + ": " + e.message }));
	}

	function hasModel(v) {
		if (Array.isArray(v)) return v.some(hasModel);
		if (!isObj(v)) return false;
		if (isObj(v.model)) return true;
		if (Array.isArray(v.subscribes) && v.subscribes.some(s => isObj(s) && s.handler != null)) return true;
		return ["cards", "table", "ribbon", "detail", "tabs"].some(k => hasModel(v[k]));
	}

	// Validates a page contract without mounting it.  Never throws; returns [{code, path, message}].
	function validate(json, opts) {
		opts = opts || {};
		const out = [];
		const push = (code, path, message) => out.push({ code: code, path: path, message: message });
		if (!isObj(json)) { push("E-JS-1", "$", "the page contract is not an object"); return out; }
		const schema = opts.schema || modelSchema;
		if (schema) for (const f of schemaFindings(schema, json)) out.push(f);
		const jc = window.JuneauConsole;
		const supported = (jc && jc.SUPPORTED_CONTRACT_VERSIONS) || ["1"];
		if (!supported.includes(json.contractVersion)) push("E-JS-2", "$", "unsupported page contract version '" + json.contractVersion + "' (supported: " + supported.join(", ") + ")");
		if ((json.modelVersion !== undefined || hasModel(json)) && !SUPPORTED_MODEL_VERSIONS.includes(json.modelVersion))
			push("E-JS-120", "$", "modelVersion '" + (json.modelVersion === undefined ? "(missing)" : json.modelVersion) + "' is not supported (supported: " + SUPPORTED_MODEL_VERSIONS.join(", ") + ")");
		let page = null;
		try {
			page = fromJSON(json, { packs: opts.packs });
			for (const f of page._findings) push(f.code, f.path, f.message);
		} catch (e) {
			const code = (e && e.code) || "E-JS-121";
			if (!out.some(f => f.code === code)) push(code, "$", e && e.message ? e.message : String(e));
		} finally {
			if (page) page._destroy();
		}
		const wiring = NS.bus && NS.bus.wiring;
		if (wiring && typeof wiring.validate === "function") {
			try {
				for (const p of wiring.validate(json, opts.cardTypes || {})) push(p.code, p.card != null ? "card:" + p.card : "$", p.message);
			} catch (e) {
				push("E-JS-121", "$", "wiring: " + (e && e.message));
			}
		}
		return out;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Mount integration (called by the shell)
	//-----------------------------------------------------------------------------------------------------------------

	const settled = new Map();                        // card id -> true when the card mounted, false when it failed
	let mountingPage = null;                          // a pre-built page being mounted (imperative construction)

	// The shell is about to build a page: the previous page goes away and the per-mount state starts clean.
	function mountStarting() {
		if (currentPage) currentPage._destroy();
		settled.clear();
		ledger = [];
		resolved = false;
	}

	const replaceHandle = (old, neu) => {
		const parent = old._parent;
		const i = parent._children.indexOf(old);
		for (const c of old._children) { c._parent = neu; neu._children.push(c); }
		old._children = [];
		const el = old._el;
		old._destroy();
		neu._parent = parent;
		if (i >= 0) parent._children.splice(i, 0, neu);
		else parent._children.push(neu);
		if (parent._attached) neu._attach(el);
	};

	function paintFindings(page) {
		for (const f of page._findings || []) {
			if (f.painted) continue;
			f.painted = true;
			handlerError(f.owner, f.code, f.message);
		}
	}

	function checkPageLoaded(page) {
		if (page._loadedFired || page._destroyed || !page._expected) return;
		for (const id of page._expected) if (!settled.has(id)) return;
		page._loadedFired = true;
		page._fire("loaded", {});
	}

	function fireRendered(page, id) {
		const c = page.card(id);
		if (c && settled.get(id) === true) c._fire("rendered", {});
	}

	// Called at the end of a shell mount: builds (or adopts) the handle tree and binds it to the mounted elements.
	function attachContract(contract, cardState, root) {
		const instances = {};
		// An instance the shell made before this file loaded is not a model handle; the tree gets a placeholder bound to its element instead.
		for (const [id, st] of cardState) if (st.instance && st.instance instanceof Card) instances[id] = st.instance;
		const premade = mountingPage;
		const page = premade || fromJSON(contract, { instances: instances });
		mountingPage = null;
		if (!premade && modelSchema && (validateOnMount || /[?&]juneauValidate=1(&|$)/.test((window.location && window.location.search) || "")))
			for (const f of schemaFindings(modelSchema, contract)) page._findings.push({ code: f.code, path: page.path, message: f.message, owner: page });
		page._expected = Array.from(cardState.keys());
		page._attach(root);
		for (const t of page.tabs) bindTab(t, root);
		for (const c of page.cards) {
			const st = cardState.get(c._id);
			if (st) c._attach(st.el);
			for (const x of c._children) if (x._kind === "table") bindTemplates(x);
		}
		page._cardState = cardState;
		paintFindings(page);
		for (const id of settled.keys()) fireRendered(page, id);
		checkPageLoaded(page);
		return page;
	}

	function bindTab(tab, root) {
		const sel = '[data-juneau-nav-id="' + String(tab._id).replace(/"/g, "") + '"]';
		tab._attach(root.querySelector ? root.querySelector(sel) : null);
		for (const c of tab._children) if (c._kind === "pagetab") bindTab(c, root);
	}

	// The detail template and its tab templates are definitions, not elements: attached with no element so they resolve by path.
	function bindTemplates(table) {
		for (const d of table._children) {
			if (d._kind !== "rowdetail" || !d._template) continue;
			d._attach(null);
			for (const t of d._children) t._attach(null);
		}
	}

	// A card instance the shell created after the tree was built (its type registered late) replaces the placeholder.
	function adoptCard(id, instance) {
		if (!(instance instanceof Card)) return;   // a plain shell-card subclass has no model surface to adopt
		const page = currentPage;
		const old = page && page.card(id);
		if (!old || old === instance) return;
		instance._id = String(id);
		instance._spec = old._spec;
		if (!alreadyDeclared(instance)) applyModel(instance, old._spec.model, page._findings);
		replaceHandle(old, instance);
		paintFindings(page);
	}

	function cardSettled(id, ok) {
		settled.set(String(id), !!ok);
		const page = currentPage;
		if (!page || !page._cardState) return;
		fireRendered(page, String(id));
		checkPageLoaded(page);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Namespace
	//-----------------------------------------------------------------------------------------------------------------

	const model = {
		SUPPORTED_MODEL_VERSIONS: SUPPORTED_MODEL_VERSIONS,
		Handle: Handle,
		Page: Page, PageTab: PageTab, Card: Card, Table: Table, Row: Row, RowDetail: RowDetail, DetailTab: DetailTab,
		Ribbon: Ribbon, RibbonItem: RibbonItem, Switcher: Switcher, SwitcherView: SwitcherView, Chart: Chart,
		EVENTS: EVENTS,
		resolveHandlers: resolveHandlers,
		fromJSON: fromJSON,
		build: (json, opts) => fromJSON(json, opts).mount(opts),
		validate: validate,
		SCHEMA_KEYWORDS: SCHEMA_KEYWORDS,
		get schema() { return modelSchema; },
		set schema(v) { modelSchema = v; },
		get validateOnMount() { return validateOnMount; },
		set validateOnMount(v) { validateOnMount = v === true; },
		loadSchema: url => fetch(url).then(r => r.json()).then(sc => { modelSchema = sc; return sc; }),
		mountStarting: mountStarting, attachContract: attachContract, adoptCard: adoptCard, cardSettled: cardSettled,
		inline: Object.seal({ compile: null }),
		get: path => registry.get(path) || null,
		page: () => currentPage,
		isAbort: e => !!e && e.name === "AbortError"
	};
	NS.model = Object.freeze(model);
	if (window.JuneauConsole && typeof window.JuneauConsole.rebindBuiltins === "function") window.JuneauConsole.rebindBuiltins(Card);
})();
