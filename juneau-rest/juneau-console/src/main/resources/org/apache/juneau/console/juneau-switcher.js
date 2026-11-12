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
 * juneau-switcher.js - the switcher card (type "switcher"): a selector strip over a set of views, each holding a
 * nested card (or a sub-strip of sections, each holding one).  Views are built lazily on first selection and kept, so
 * switching back never rebuilds.  The selected view (and section) survives a reload through card preferences and
 * is shareable through the `sw` address-bar directive; the address wins over preferences, which win over the spec's
 * initialView / initialSection, which win over the first view.
 *
 * Spec:  { type: "switcher", id, title?, style? ("tabs" | "slider"), initialView?, views: [ { id, label, card | cards: [ ... ] | sections: [ { id, label, card } ],
 *          initialSection? } ] }
 * Nested cards are full cards (own bus topics, cardApi entry, lifecycle); a switcher cannot nest a switcher.
 */
(function () {
	"use strict";
	var JC = window.JuneauConsole;
	var NS = window.JuneauViews;
	if (!NS || !NS.model || !NS.helpers || !NS.urlState) return;
	if (JC && typeof JC.cardTypes === "function" && JC.cardTypes().indexOf("switcher") >= 0) return;   // loaded twice
	var model = NS.model;
	var helpers = NS.helpers;
	var U = NS.urlState;
	var live = {};       // card id -> { get(): "view[.section]" }
	var defaults = {};   // card id -> the spec's default "view[.section]"

	if (!U.hasDirective("sw")) {
		U.registerDirective("sw", {
			parse: function (text) {
				var out = {};
				String(text).split("~").forEach(function (entry) {
					var i = entry.indexOf(":");
					if (i > 0) out[entry.slice(0, i)] = entry.slice(i + 1);
				});
				return out;
			},
			serialize: function (value) {
				return Object.keys(value).sort().map(function (id) { return id + ":" + value[id]; }).join("~");
			},
			isEmpty: function (value) { return !value || Object.keys(value).length === 0; }
		});
		U.registerPageSource("sw", function () {
			var out = {};
			Object.keys(live).forEach(function (id) {
				var cur = live[id].get();
				if (cur !== defaults[id]) out[id] = cur;
			});
			return out;
		});
	}

	var leaving = false;   // set once the page is unloading, when rewriting the address would be wasted work
	if (typeof window.addEventListener === "function") window.addEventListener("pagehide", function () { leaving = true; });

	var ID_RE = /^[A-Za-z][A-Za-z0-9_-]{0,63}$/;

	function sectionsOf(view) {
		if (view.cards) return [{ id: "main", label: view.label, cards: view.cards }];
		return view.sections ? view.sections : [{ id: "main", label: view.label, card: view.card }];
	}

	function cardsOf(section) { return section.cards || [section.card]; }

	class SwitcherCard extends (model.Card || (JC && JC.Card) || class {}) {
		static lateBase = true;   // the shell puts the Card base under this class if it loaded first

		/** The nested card specs of a switcher spec, for the shell's bus wiring and topic declaration. */
		static nestedCards(spec) {
			var out = [];
			(spec && Array.isArray(spec.views) ? spec.views : []).forEach(function (v) {
				if (!v) return;
				if (v.card) out.push(v.card);
				if (Array.isArray(v.cards)) v.cards.forEach(function (c) { if (c) out.push(c); });
				if (Array.isArray(v.sections)) v.sections.forEach(function (s) { if (s && s.card) out.push(s.card); });
			});
			return out;
		}

		render() {
			var self = this;
			var spec = this.spec;
			this.views = (spec.views || []).slice();
			this.viewById = {};
			this.views.forEach(function (v) { self.viewById[v.id] = v; });
			this.built = {};          // viewId -> { host, handles, activeSection, sectionBuilt, subStrip }
			this.activeView = null;
			this._initialDone = false;
			this.prefsStore = this.prefs("switcher");
			if (!this.views.length) return;

			this.display = this.document.createElement("div");
			this.display.className = "juneau-switcher-display";
			this.display.id = this.el.id + "-display";
			this.display.setAttribute("role", "tabpanel");
			this.display.setAttribute("tabindex", "0");

			var start = this._resolveStart();
			this._start = start;
			var style = spec.style === "slider" ? "slider" : "tabs";
			this.styleName = style;
			this.selector = helpers.tabStrip(this.views.map(function (v) { return { id: v.id, label: v.label }; }), {
				mode: style === "slider" ? "ribbon" : "tab", panes: false, controls: this.display.id, active: start.view,
				ariaLabel: spec.title || "Views",
				onActivate: function (id, pane, reason) { self._show(id, reason); }
			});
			this.selector.className += " juneau-switcher-selector";
			this.selector.setAttribute("data-juneau-switcher-style", style);
			if (style === "tabs") {
				// Folder tabs: the display is the strip's own panel, so the selected tab joins it.
				this.selector.setAttribute("data-juneau-strip-panel", "");
				this.display.className += " juneau-view-helper-tabstrip-panes";
				this.selector.appendChild(this.display);
				this.el.appendChild(this.selector);
			} else {
				this.el.appendChild(this.selector);
				this.el.appendChild(this.display);
			}

			if (style === "slider") {
				var group = this.selector.querySelector(".juneau-view-ribbon-group");
				this.thumb = this.document.createElement("span");
				this.thumb.className = "juneau-switcher-thumb";
				this.thumb.setAttribute("aria-hidden", "true");
				group.insertBefore(this.thumb, group.firstChild);
				var reduce = typeof window.matchMedia === "function" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
				if (reduce) this.thumb.style.transition = "none";
				this._moveThumb = function () {
					var btn = self._tabButton(self.activeView || start.view);
					if (!btn || !self.thumb) return;
					self.thumb.style.width = btn.offsetWidth + "px";
					self.thumb.style.transform = "translateX(" + btn.offsetLeft + "px)";
				};
				this._onResize = function () { self._moveThumb(); };
				window.addEventListener("resize", this._onResize);
			}

			this._makeHandles();
			defaults[spec.id] = this._defaultKey();
			live[spec.id] = { get: function () { return self._currentKey(); } };

			this._show(start.view, "initial");
			this._initialDone = true;   // until here every activation is quiet: no prefs write, no address write, no event
		}

		destroy() {
			if (this._onResize) window.removeEventListener("resize", this._onResize);
			delete live[this.spec.id];
			delete defaults[this.spec.id];
			if (this.switcher) this.switcher._destroy();
			// The entry left with the card; rewrite the address so a stale sw(...) does not outlive it.
			if (!leaving && typeof U.syncPage === "function") U.syncPage();
		}

		/** Selects a view by id, or a section of a view as "view/section"; returns false for an unknown id. */
		select(target) {
			var slash = typeof target === "string" ? target.indexOf("/") : -1;
			var viewId = slash < 0 ? target : target.slice(0, slash);
			var view = this.viewById[viewId];
			if (!view) return false;
			var sectionId = slash < 0 ? null : target.slice(slash + 1);
			if (sectionId !== null && !this._pickSection(view, sectionId)) return false;
			this.selector.juneauTabs.activate(viewId);
			var b = this.built[viewId];
			if (sectionId !== null && b && b.subStrip) b.subStrip.juneauTabs.activate(sectionId);
			return true;
		}

		/** Refreshes the active view's nested cards only; never builds or remounts anything. */
		refresh() {
			var b = this.built[this.activeView];
			if (b) b.handles.forEach(function (h) { if (h && typeof h.refresh === "function") h.refresh(); });
			return undefined;
		}

		_defaultKey() {
			var v = this.viewById[this.spec.initialView] || this.views[0];
			var secs = sectionsOf(v);
			var sec = this._pickSection(v, v.initialSection) || secs[0].id;
			return secs.length > 1 ? v.id + "." + sec : v.id;
		}

		_pickSection(view, id) {
			var secs = sectionsOf(view);
			for (var i = 0; i < secs.length; i++) if (secs[i].id === id) return id;
			return null;
		}

		_currentKey() {
			if (!this.activeView) return this._defaultKey();
			var v = this.viewById[this.activeView];
			var b = this.built[this.activeView];
			var secs = sectionsOf(v);
			return secs.length > 1 && b && b.activeSection ? v.id + "." + b.activeSection : v.id;
		}

		_parseKey(key) {
			if (typeof key !== "string" || !key) return null;
			var dot = key.indexOf(".");
			var viewId = dot < 0 ? key : key.slice(0, dot);
			var view = this.viewById[viewId];
			if (!view) return null;
			return { view: viewId, section: dot < 0 ? null : this._pickSection(view, key.slice(dot + 1)) };
		}

		/** Address first, then preferences, then the spec's initial values, then the first view; unknown ids fall through. */
		_resolveStart() {
			var id = this.spec.id;
			var all = U.currentDirective("sw");
			var fromUrl = all && all[id] ? this._parseKey(all[id]) : null;
			if (fromUrl) return fromUrl;
			var pv = this.prefsStore.get("view");
			if (typeof pv === "string" && this.viewById[pv]) return { view: pv, section: null };
			var iv = this.viewById[this.spec.initialView] ? this.spec.initialView : this.views[0].id;
			return { view: iv, section: null };
		}

		/** The section a freshly built multi-section view opens on. */
		_firstSection(view) {
			var secs = sectionsOf(view);
			if (this._start && this._start.view === view.id && this._start.section) return this._start.section;
			var pref = this.prefsStore.get("section." + view.id);
			return this._pickSection(view, pref) || this._pickSection(view, view.initialSection) || secs[0].id;
		}

		/** Called by the selector strip on every activation (reason "initial" for the first). */
		_show(viewId, reason) {
			var self = this;
			var view = this.viewById[viewId];
			var button = this._tabButton(viewId);
			if (button && button.id) this.display.setAttribute("aria-labelledby", button.id);
			Object.keys(this.built).forEach(function (id) { self.built[id].host.hidden = true; });
			var b = this.built[viewId];
			if (!b) {
				b = { host: this.document.createElement("div"), handles: [], activeSection: null, sectionBuilt: {}, subStrip: null };
				b.host.className = "juneau-switcher-view";
				this.display.appendChild(b.host);
				this.built[viewId] = b;
				this._buildView(view, b);
			}
			b.host.hidden = false;
			this.activeView = viewId;
			if (this._moveThumb) this._moveThumb();
			this.switcher._activeId = viewId;
			if (reason === "initial" || !this._initialDone) return;
			this.prefsStore.set("view", viewId);
			U.syncPage();
			var vh = this.switcher.view(viewId);
			if (vh) vh._fire("activated", { id: viewId });
		}

		_buildView(view, b) {
			var self = this;
			var secs = sectionsOf(view);
			var vh = this.switcher.view(view.id);
			if (secs.length === 1) {
				b.activeSection = secs[0].id;
				b.sectionBuilt[secs[0].id] = true;
				cardsOf(secs[0]).forEach(function (c) { b.handles.push(self._nest(c, b.host, vh)); });
				return;
			}
			var first = this._firstSection(view);
			b.activeSection = first;
			b.subStrip = helpers.tabStrip(secs.map(function (s) {
				return { id: s.id, label: s.label, populate: function (pane) { self._buildSection(b, s, pane, vh); } };
			}), {
				mode: "tab", panes: true, active: first, ariaLabel: view.label,
				onActivate: function (id) { self._sectionActivated(view, b, id); }
			});
			b.host.appendChild(b.subStrip);
		}

		_buildSection(b, section, pane, vh) {
			if (b.sectionBuilt[section.id]) return;
			b.sectionBuilt[section.id] = true;
			b.handles.push(this._nest(section.card, pane, vh));
		}

		_nest(spec, host, viewHandle) {
			return this.renderNested(spec, host, { parent: viewHandle });
		}

		_sectionActivated(view, b, sectionId) {
			b.activeSection = sectionId;
			if (!this._initialDone) return;
			this.prefsStore.set("section." + view.id, sectionId);
			U.syncPage();
			var vh = this.switcher.view(view.id);
			var th = vh && vh.sections.find(function (s) { return s.id === sectionId; });
			if (th) th._fire("activated", { id: sectionId });
		}

		_tabButton(viewId) {
			return this.selector.querySelector('[data-tab-id="' + viewId + '"]');
		}

		/** Builds the Switcher, SwitcherView and DetailTab handles and wires their hook seams to this card. */
		_makeHandles() {
			var self = this;
			var sw = new model.Switcher({ initialView: this.spec.initialView, views: this.spec.views });
			sw._hooks = { addView: function (def) { return self._addView(def); } };
			this._addChecked(sw);
			sw._attach(this.el);
			this.switcher = sw;
			this.views.forEach(function (v) { self._makeViewHandle(sw, v); });
		}

		_makeViewHandle(sw, v) {
			var self = this;
			var vh = new model.SwitcherView({ id: v.id, label: v.label, model: v.model }, { id: v.id });
			vh._hooks = {
				activate: function () { self.select(v.id); },
				addSection: function () { throw new Error("addSection: sections cannot be added to a built switcher view"); }
			};
			sw._addChecked(vh);
			vh._attach(this._tabButton(v.id));
			vh._applyLabel = function (text) {
				var span = this._el && this._el.querySelector("span");
				if (span) span.textContent = text;
			};
			vh._applyActivate = function () { self.select(v.id); };
			(v.sections || []).forEach(function (s) {
				var th = new model.DetailTab({ id: s.id, label: s.label, model: s.model }, { id: s.id });
				vh._addChecked(th);
				th._attach(null);
				th._applyLabel = function (text) {
					var b = self.built[v.id];
					var btn = b && b.host.querySelector('[data-tab-id="' + s.id + '"]');
					var span = btn && btn.querySelector("span");
					if (span) span.textContent = text;
				};
				th._applyActivate = function () { self.select(v.id); var b = self.built[v.id]; if (b && b.subStrip) b.subStrip.juneauTabs.activate(s.id); };
			});
			return vh;
		}

		/** Adds a view at run time: { id, label, card | sections }. */
		_addView(def) {
			if (!def || typeof def.id !== "string" || !ID_RE.test(def.id) || typeof def.label !== "string")
				throw new Error("addView: a view needs a valid string id and a string label");
			if (this.viewById[def.id]) throw new Error("addView: view '" + def.id + "' already exists");
			if ((def.card ? 1 : 0) + (def.sections ? 1 : 0) + (def.cards ? 1 : 0) !== 1) throw new Error("addView: give exactly one of card, sections or cards");
			if ((def.card && def.card.type === "switcher") || (def.cards || []).some(function (c) { return c && c.type === "switcher"; })
				|| (def.sections || []).some(function (s) { return s.card && s.card.type === "switcher"; }))
				throw new Error("addView: a switcher cannot hold a switcher");
			this.views.push(def);
			this.viewById[def.id] = def;
			this.selector.juneauTabs.addTab({ id: def.id, label: def.label });
			return this._makeViewHandle(this.switcher, def);
		}
	}

	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["switcher", SwitcherCard]);
})();
