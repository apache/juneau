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
 * Juneau console shell. Renders a page contract (see juneau-page.schema.json) into header, nav,
 * main cards, and footer. Auto-mounts synchronously from <script id="juneau-page"> when loaded.
 *
 * Inline <script> elements inside <template data-card|data-slot> run once, in document order, when the
 * template is inserted. External <script src> elements inside templates are not order-guaranteed; load
 * those through <@page init=> or <@scripts> instead.
 *
 * @example
 * // Register a card type before the shell auto-mounts (load your script BEFORE juneau-console.js),
 * // or mount manually in a test harness:
 * JuneauConsole.registerCard('kpi', (card, el, ctx) => {
 *   el.textContent = card.title + ': ' + card.value;
 * });
 * JuneauConsole.mount(contract, { root: document.body, document });
 */
(function () {
	"use strict";

	const SUPPORTED_CONTRACT_VERSIONS = Object.freeze(["1"]);
	const TYPE_RE = /^[a-z][a-z0-9-]{0,31}$/;
	const FATAL = new Set(["E-JS-1", "E-JS-2", "E-JS-3", "E-JS-5", "E-JS-6", "E-JS-7", "E-JS-11", "E-JS-12"]);
	const MSG = {
		"E-JS-1": "missing or unparseable <script id=\"juneau-page\">: '%s'",
		"E-JS-2": "unsupported page contract version '%s'; this shell supports '%s'",
		"E-JS-2-renamed": "page contract key 'version' was renamed to 'contractVersion'; regenerate the page with a current Juneau",
		"E-JS-3": "%s references template '%s', but no <template data-%s=\"%s\"> exists",
		"E-JS-4": "card '%s' has unknown type '%s'; registered types: '%s'",
		"E-JS-5": "activeNav '%s' is not a path in the nav tree (failed at '%s')",
		"E-JS-6": "duplicate %s id '%s'",
		"E-JS-7": "duplicate <template data-%s=\"%s\">",
		"E-JS-8": "card '%s' (type '%s') handler threw: '%s'",
		"E-JS-9": "unsafe href '%s' on %s '%s'",
		"E-JS-10": "datatables card '%s' needs JuneauViews.regions; load the views toolkit",
		"E-JS-11": "JuneauConsole.mount called twice on the same root",
		"E-JS-12": "card type '%s' is already registered"
	};

	class JuneauConsoleError extends Error {
		constructor(code, msg) {
			super(msg);
			this.name = "JuneauConsoleError";
			this.code = code;
		}
	}

	const handlers = new Map();
	const mountedRoots = new WeakSet();
	let mounted = null;   // { contract, cards: Map<id, frozen card> }

	function fmt(msg, ...args) {
		let i = 0;
		return msg.replace(/%s/g, () => String(args[i++]));
	}

	// Every loud failure: one banner listing all failures, console.error, and a throw for fatal codes.
	function fail(doc, msgKey, ...args) {
		const msg = fmt(MSG[msgKey], ...args);
		const code = msgKey.replace(/-renamed$/, "");
		const body = doc.body;
		let banner = body && body.querySelector(".jc-console-error");
		if (body && !banner) {
			banner = doc.createElement("div");
			banner.className = "jc-console-error";
			banner.setAttribute("role", "alert");
			body.insertBefore(banner, body.firstChild);
		}
		if (banner) {
			const item = doc.createElement("div");
			item.className = "jc-console-error-item";
			item.setAttribute("data-juneau-error", code);
			item.textContent = msg;
			banner.appendChild(item);
		}
		console.error("[juneau-console] " + msg);
		if (FATAL.has(code))
			throw new JuneauConsoleError(code, msg);
	}

	/**
	 * Whether `t` (already known to start with "/" or "\") is a protocol-relative URL once the obfuscation a
	 * browser's WHATWG URL/HTML parser normalizes away is stripped: ASCII tab/CR/LF removed wherever they occur
	 * (URL spec "remove all ASCII tab or newline from input"), and backslash treated as forward slash (a
	 * special-scheme URL parser folds "\" into "/" before resolving). A resulting SECOND slash right after the
	 * first - a literal "//host", a triple-slash "///host", a two-backslash "\\host", or an interior-obfuscated
	 * "/\t/host" - resolves to a THIRD-PARTY origin, not the same-site relative/absolute path a bare single
	 * leading "/" is. Local duplicate of juneau-views.js's identically-named helper (WORK-J0516): console-ui
	 * must not depend on views (the module graph runs the other way), so the algorithm is copied verbatim here
	 * rather than imported.
	 */
	function isProtocolRelativeUrl(t) {
		return t.replace(/[\t\r\n]/g, "").replace(/\\/g, "/").charAt(1) === "/";
	}

	/**
	 * Whether `href` is safe to copy onto an href/src attribute: http(s), mailto, a fragment, or a path
	 * (relative or absolute, but NOT protocol-relative - see {@link isProtocolRelativeUrl}). javascript:/data:/
	 * vbscript: are rejected by the explicit prefix check below, but that check is NOT the load-bearing defense
	 * against an obfuscated spelling of one of those schemes (e.g. "java\tscript:", "da\nta:") - a browser's URL
	 * parser strips ASCII tab/newline/CR before resolving, so a literal prefix match on the un-stripped string
	 * can miss it. What actually catches that case is the final fallback below: `indexOf(":") < 0` on the
	 * embedded-control-stripped, lowercased string - an obfuscated scheme still contains a colon and matches
	 * none of the three allowed absolute prefixes, so it still fails closed. A leading "/" or "\" is accepted
	 * ONLY when {@link isProtocolRelativeUrl} says it is not protocol-relative - a bare "//evil.example/x"
	 * resolves to a THIRD-PARTY origin, not the "relative path" a naive check would accept it as (a
	 * phishing/open-redirect defense, not an XSS one - a protocol-relative URL cannot run JS). Mirrors
	 * juneau-views.js's isSafeMarkdownHref (WORK-J0516); duplicated rather than imported for the same
	 * module-graph reason as {@link isProtocolRelativeUrl} above.
	 */
	function isSafeHref(href) {
		if (typeof href !== "string") return false;
		const t = href.replace(/[\t\r\n]/g, "").trim();
		if (!t) return false;
		const lower = t.toLowerCase();
		if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:"))
			return false;
		if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("mailto:"))
			return true;
		if (t.charAt(0) === "#")
			return true;
		if (t.charAt(0) === "/" || t.charAt(0) === "\\")
			return !isProtocolRelativeUrl(t);
		return lower.indexOf(":") < 0;
	}

	function el(doc, tag, cls) {
		const e = doc.createElement(tag);
		if (cls) e.className = cls;
		return e;
	}

	function link(doc, l, cls, where) {
		const a = el(doc, "a", cls);
		if (!isSafeHref(l.href)) {
			fail(doc, "E-JS-9", l.href, where, l.label);
			return null;
		}
		a.setAttribute("href", l.href);
		if (l.title) a.setAttribute("title", l.title);
		if (l.target) a.setAttribute("target", l.target);
		if (l.target === "_blank") a.setAttribute("rel", "noopener");
		a.textContent = l.label;
		return a;
	}

	function indexTemplates(doc, root) {
		const byCard = new Map(), bySlot = new Map();
		for (const t of root.querySelectorAll("template")) {
			const card = t.getAttribute("data-card"), slot = t.getAttribute("data-slot");
			if (card != null) {
				if (byCard.has(card)) fail(doc, "E-JS-7", "card", card);
				byCard.set(card, t);
			}
			if (slot != null) {
				if (bySlot.has(slot)) fail(doc, "E-JS-7", "slot", slot);
				bySlot.set(slot, t);
			}
		}
		return { byCard, bySlot };
	}

	function clone(doc, tpl) {
		return doc.importNode(tpl.content, true);
	}

	function checkIds(doc, c) {
		(function walk(nodes) {
			const seen = new Set();
			for (const n of nodes || []) {
				if (seen.has(n.id)) fail(doc, "E-JS-6", "nav", n.id);
				seen.add(n.id);
				walk(n.children);
			}
		})(c.nav);
		const cards = new Set();
		for (const card of c.cards || []) {
			if (cards.has(card.id)) fail(doc, "E-JS-6", "card", card.id);
			cards.add(card.id);
		}
	}

	function walkPath(nav, ids) {
		let level = nav || [];
		for (const id of ids) {
			const n = level.find(x => x.id === id);
			if (!n) return id;
			level = n.children || [];
		}
		return null;
	}

	function checkRefs(doc, c, tpl) {
		for (const scope of ["header", "footer"]) {
			const slots = (c[scope] && c[scope].slots) || {};
			for (const name of Object.keys(slots))
				if (!tpl.bySlot.has(slots[name]))
					fail(doc, "E-JS-3", scope + ".slots." + name, slots[name], "slot", slots[name]);
		}
		for (const card of c.cards || [])
			if (card.template != null && !tpl.byCard.has(card.template))
				fail(doc, "E-JS-3", "card '" + card.id + "'", card.template, "card", card.template);
	}

	// §5.3: segment-wise longest prefix; every query param in the href must match; ties go deeper, then first.
	function prefixMatch(nav, loc) {
		const path = (loc && loc.pathname) || "/";
		const params = new URLSearchParams((loc && loc.search) || "");
		let best = null, bestLen = -1;
		(function walk(nodes, trail) {
			for (const n of nodes || []) {
				const t = trail.concat(n.id);
				if (n.href) {
					const len = matchLen(n.href, path, params);
					if (len >= 0 && (len > bestLen || (len === bestLen && t.length > best.length))) {
						best = t;
						bestLen = len;
					}
				}
				walk(n.children, t);
			}
		})(nav, []);
		return best;
	}

	function segs(p) {
		return p.split("/").filter(s => s.length > 0);
	}

	function matchLen(href, path, params) {
		const q = href.indexOf("?");
		const hp = q < 0 ? href : href.slice(0, q);
		const hq = q < 0 ? "" : href.slice(q + 1);
		if (!hp.startsWith("/") || hp.startsWith("//")) return -1;
		const hs = segs(hp), ps = segs(path);
		if (hs.length > ps.length) return -1;
		for (let i = 0; i < hs.length; i++)
			if (hs[i] !== ps[i]) return -1;
		for (const [k, v] of new URLSearchParams(hq))
			if (params.get(k) !== v) return -1;
		return hs.length;
	}

	function renderNav(doc, c, active) {
		if (!c.nav || c.nav.length === 0) return null;
		const nav = el(doc, "nav", "juneau-page-nav");
		if (c.navLayout === "vertical") nav.setAttribute("data-juneau-nav-layout", "vertical");
		const label = (c.header && c.header.title) || c.title;
		if (label) nav.setAttribute("aria-label", label);
		const row = (nodes, rowCls, linkCls, depth) => {
			const r = el(doc, "div", rowCls);
			let selected = null;
			for (const n of nodes) {
				const a = n.href ? link(doc, n, linkCls, "nav") : el(doc, "a", linkCls);
				if (!a) continue;
				if (!n.href) a.textContent = n.label;
				a.setAttribute("data-juneau-nav-id", n.id);
				if (active[depth] === n.id) {
					a.setAttribute("aria-current", "page");
					selected = n;
				}
				r.appendChild(a);
			}
			nav.appendChild(r);
			return selected;
		};
		let sel = row(c.nav, "juneau-page-nav-sections", "juneau-page-nav-section", 0);
		for (let depth = 1; sel && sel.children && sel.children.length; depth++)
			sel = row(sel.children, "juneau-page-nav-children", "juneau-page-nav-child", depth);
		return nav;
	}

	function hasHeader(h) {
		const s = h.slots || {};
		return !!(h.title || h.subtitle || h.logo || (h.links && h.links.length) || h.userMenu || s.brand || s.actions || s.replace);
	}

	function renderHeader(doc, c, tpl) {
		const h = c.header || {};
		if (!hasHeader(h)) return null;
		const s = h.slots || {};
		if (s.replace) return clone(doc, tpl.bySlot.get(s.replace));
		const header = el(doc, "header", "jc-header");
		if (s.brand) {
			const brand = el(doc, "div", "jc-brand");
			brand.appendChild(clone(doc, tpl.bySlot.get(s.brand)));
			header.appendChild(brand);
		} else if (h.title || h.subtitle || h.logo) {
			const logo = h.logo || {};
			let brand;
			if (logo.href && isSafeHref(logo.href)) {
				brand = el(doc, "a", "jc-brand");
				brand.setAttribute("href", logo.href);
			} else {
				if (logo.href) fail(doc, "E-JS-9", logo.href, "header", "logo");
				brand = el(doc, "div", "jc-brand");
			}
			if (logo.src) {
				if (isSafeHref(logo.src)) {
					const img = el(doc, "div", "jc-logo");
					img.setAttribute("role", "img");
					img.setAttribute("aria-label", logo.alt || h.title || "");
					img.setAttribute("style", "background-image:url('" + logo.src.replace(/['\\()\s]/g, encodeURIComponent) + "')");
					brand.appendChild(img);
				} else {
					fail(doc, "E-JS-9", logo.src, "header", "logo");
				}
			}
			if (h.title) {
				const t = el(doc, "span", "jc-brand-title");
				t.textContent = h.title;
				brand.appendChild(t);
			}
			if (h.subtitle) {
				const t = el(doc, "span", "jc-brand-subtitle");
				t.textContent = h.subtitle;
				brand.appendChild(t);
			}
			header.appendChild(brand);
		}
		if ((h.links && h.links.length) || s.actions || h.userMenu) {
			const actions = el(doc, "div", "jc-header-actions");
			for (const l of h.links || []) {
				const a = link(doc, l, "jc-header-link", "header link");
				if (a) actions.appendChild(a);
			}
			if (s.actions) actions.appendChild(clone(doc, tpl.bySlot.get(s.actions)));
			if (h.userMenu) actions.appendChild(renderUserMenu(doc, h.userMenu));
			header.appendChild(actions);
		}
		return header;
	}

	let menuSeq = 0;

	// Fallback (no-views-stack) user menus, tracked so a SINGLE delegated keydown listener per document (see
	// wireFallbackEscape) can close whichever one is open, instead of renderUserMenu adding one doc-level listener
	// per call - which would otherwise accumulate without bound across repeated mounts sharing one document.
	const fallbackMenus = new Set();
	const fallbackEscapeWired = new WeakSet();

	function wireFallbackEscape(doc) {
		if (fallbackEscapeWired.has(doc)) return;
		fallbackEscapeWired.add(doc);
		doc.addEventListener("keydown", function (ev) {
			if (ev.key !== "Escape" || hasViewsStack()) return;
			for (const entry of fallbackMenus)
				if (entry.doc === doc && entry.trigger.getAttribute("aria-expanded") === "true") {
					entry.trigger.setAttribute("aria-expanded", "false");
					entry.list.style.display = "none";
				}
		});
	}

	// The trigger/list pair the chrome section's wireMenus() already understands (Behavior.MENU shape), plus a
	// local open/close fallback used only when the JuneauViews layer stack is absent.
	function renderUserMenu(doc, um) {
		const wrap = el(doc, "div", "jc-user-menu");
		const id = "jc-user-menu-" + (++menuSeq);
		const trigger = el(doc, "button", "jc-avatar");
		trigger.setAttribute("type", "button");
		trigger.setAttribute("aria-haspopup", "menu");
		trigger.setAttribute("aria-expanded", "false");
		trigger.setAttribute("aria-controls", id);
		trigger.setAttribute("aria-label", um.label);
		trigger.setAttribute("data-juneau-behavior", "menu");
		if (um.avatar && isSafeHref(um.avatar)) {
			const img = el(doc, "img", "jc-avatar-img");
			img.setAttribute("src", um.avatar);
			img.setAttribute("alt", "");
			trigger.appendChild(img);
		}
		const initials = el(doc, "span", "jc-avatar-initials");
		initials.textContent = um.initials || um.label.slice(0, 1).toUpperCase();
		trigger.appendChild(initials);
		const list = el(doc, "div", "jc-menu");
		list.id = id;
		list.setAttribute("role", "menu");
		list.style.display = "none";
		for (const item of um.items || []) {
			const a = link(doc, item, "jc-menu-item", "userMenu item");
			if (!a) continue;
			a.setAttribute("role", "menuitem");
			list.appendChild(a);
		}
		wrap.appendChild(trigger);
		wrap.appendChild(list);
		trigger.addEventListener("click", function (ev) {
			if (hasViewsStack()) return;   // the chrome section's wireMenus() owns it
			ev.preventDefault();
			const open = trigger.getAttribute("aria-expanded") === "true";
			trigger.setAttribute("aria-expanded", open ? "false" : "true");
			list.style.display = open ? "none" : "";
		});
		fallbackMenus.add({ doc, trigger, list });
		wireFallbackEscape(doc);
		return wrap;
	}

	function hasViewsStack() {
		const v = window.JuneauViews && window.JuneauViews.init;
		return !!(v && typeof v.pushLayer === "function" && typeof v.popLayer === "function");
	}

	function renderFooter(doc, c, tpl) {
		const f = c.footer;
		if (!f) return null;
		const footer = el(doc, "footer", "jc-page-footer");
		if (f.text != null) {
			const ftpl = doc.createElement("template");
			ftpl.innerHTML = f.text;
			footer.appendChild(doc.importNode(ftpl.content, true));
		}
		else if (f.slots && f.slots.content) footer.appendChild(clone(doc, tpl.bySlot.get(f.slots.content)));
		for (const l of f.links || []) {
			const a = link(doc, l, "jc-footer-link", "footer link");
			if (a) footer.appendChild(a);
		}
		return footer;
	}

	function onReady(doc, fn) {
		if (doc.readyState === "loading") doc.addEventListener("DOMContentLoaded", fn);
		else fn();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Built-in card types
	//-----------------------------------------------------------------------------------------------------------------

	function htmlCard(card, el0, ctx) {
		if (card.template != null) {
			el0.appendChild(ctx.template(card.template));
			return;
		}
		if (!isSafeHref(card.src) || /^https?:/i.test(card.src) && !card.src.startsWith(window.location.origin + "/"))
			ctx.fail("src '" + card.src + "' is not same-origin");
		fetch(card.src, { credentials: "same-origin" })
			.then(r => r.ok ? r.text() : Promise.reject(new Error("HTTP " + r.status)))
			.then(text => {
				const tpl = ctx.document.createElement("template");
				tpl.innerHTML = text;
				el0.appendChild(ctx.document.importNode(tpl.content, true));
			})
			.catch(e => fail(ctx.document, "E-JS-8", card.id, card.type, e.message));
	}

	// C1 bridge (§4.6): one regions.mount(hookup) on DOMContentLoaded for all datatables cards.
	const pendingTables = {};
	let tablesScheduled = false;

	function datatablesCard(card, el0, ctx) {
		const body = ctx.document.createElement("div");
		// Derived id: the view's own table takes the card id (view.id), so the body must not repeat it (duplicate DOM ids).
		body.id = card.id + "-body";
		body.className = "jc-card-body";
		body.setAttribute("data-juneau-layout", "wide");
		el0.setAttribute("data-juneau-card", "datatables");
		el0.removeAttribute("id");
		el0.appendChild(body);
		pendingTables[body.id] = { table: card.table };
		if (tablesScheduled) return;
		tablesScheduled = true;
		ctx.onReady(function () {
			const regions = window.JuneauViews && window.JuneauViews.regions;
			if (!regions || typeof regions.mount !== "function") {
				for (const id of Object.keys(pendingTables)) fail(ctx.document, "E-JS-10", id.replace(/-body$/, ""));
				return;
			}
			regions.mount(Object.assign({}, pendingTables));
		});
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Mount
	//-----------------------------------------------------------------------------------------------------------------

	function mount(contract, opts) {
		const doc = (opts && opts.document) || window.document;
		const root = (opts && opts.root) || doc.body;
		if (mountedRoots.has(root)) fail(doc, "E-JS-11");
		mountedRoots.add(root);

		if (!contract || typeof contract !== "object") fail(doc, "E-JS-1", "not an object");
		// Diagnostic only: a leftover pre-rename 'version' key is named in the message but never read for behavior.
		if (contract.contractVersion === undefined && contract.version !== undefined)
			fail(doc, "E-JS-2-renamed");
		if (!SUPPORTED_CONTRACT_VERSIONS.includes(contract.contractVersion))
			fail(doc, "E-JS-2", contract.contractVersion, SUPPORTED_CONTRACT_VERSIONS.join(", "));

		const tpl = indexTemplates(doc, root);
		checkIds(doc, contract);
		let active = contract.activeNav || [];
		let source = active.length ? "contract" : "none";
		if (active.length) {
			const failedAt = walkPath(contract.nav, active);
			if (failedAt != null) fail(doc, "E-JS-5", active.join("/"), failedAt);
		}
		checkRefs(doc, contract, tpl);
		if (!active.length) {
			const m = prefixMatch(contract.nav, window.location);
			if (m) {
				active = m;
				source = "prefix";
			}
		}

		const anchor = root.querySelector("#juneau-page");
		const before = anchor && anchor.parentNode === root ? anchor : root.firstChild;
		const insert = node => { if (node) root.insertBefore(node, before); };

		const header = renderHeader(doc, contract, tpl);
		const nav = renderNav(doc, contract, active);
		// The banner slot sits between the header and the nav (inside .jc-chrome when chrome is on), the
		// position the pre-main region held before the shell drew the chrome.
		const hs = (contract.header && contract.header.slots) || {};
		const banner = hs.banner ? clone(doc, tpl.bySlot.get(hs.banner)) : null;
		if (contract.header && contract.header.chrome) {
			const chrome = el(doc, "div", "jc-chrome");
			if (header) chrome.appendChild(header);
			if (banner) chrome.appendChild(banner);
			if (nav) chrome.appendChild(nav);
			insert(chrome);
		} else {
			insert(header);
			insert(banner);
			insert(nav);
		}
		const main = el(doc, "main", "jc-main");
		insert(main);
		insert(renderFooter(doc, contract, tpl));

		const result = { activeNav: active.slice(), activeNavSource: source, cards: {} };
		const frozenCards = new Map();
		for (const card of contract.cards || []) {
			frozenCards.set(card.id, Object.freeze(Object.assign({}, card)));
			const handler = handlers.get(card.type);
			if (!handler) {
				fail(doc, "E-JS-4", card.id, card.type, Array.from(handlers.keys()).join(", "));
				continue;
			}
			const bare = card.type === "html" && card.bare === true;
			let host;
			if (bare) {
				host = doc.createDocumentFragment();
			} else {
				host = el(doc, "div", "jc-card");
				host.id = card.id;
				if (card.title) {
					const t = el(doc, "h2", "jc-card-title");
					t.textContent = card.title;
					host.appendChild(t);
				}
			}
			const ctx = {
				document: doc,
				template: id => {
					const t = tpl.byCard.get(id);
					if (!t) throw new Error("no <template data-card=\"" + id + "\">");
					return clone(doc, t);
				},
				fail: msg => { throw new Error(msg); },
				onReady: fn => onReady(doc, fn)
			};
			try {
				handler(card, host, ctx);
			} catch (e) {
				fail(doc, "E-JS-8", card.id, card.type, e && e.message);
				continue;
			}
			result.cards[card.id] = bare ? host.firstElementChild || null : host;
			main.appendChild(host);
		}

		for (const t of tpl.byCard.values()) t.parentNode && t.parentNode.removeChild(t);
		for (const t of tpl.bySlot.values()) t.parentNode && t.parentNode.removeChild(t);

		mounted = { contract: deepFreeze(contract), cards: frozenCards };
		const ev = new CustomEvent("juneau:console-mounted", { detail: result });
		doc.dispatchEvent(ev);
		onReady(doc, function () { chrome.initAll(); });
		return result;
	}

	function deepFreeze(o) {
		if (o && typeof o === "object" && !Object.isFrozen(o)) {
			Object.freeze(o);
			for (const k of Object.keys(o)) deepFreeze(o[k]);
		}
		return o;
	}

	function registerCard(type, handler) {
		const doc = window.document;
		if (typeof type !== "string" || !TYPE_RE.test(type) || typeof handler !== "function")
			throw new TypeError("registerCard(type, handler): type must match " + TYPE_RE + " and handler must be a function");
		if (handlers.has(type)) fail(doc, "E-JS-12", type);
		handlers.set(type, handler);
	}

	// ---- chrome section: absorbed from the retired standalone chrome script (C1 P12) ----

	// Contract-version handshakes: each MUST equal its server constant (AppHeaderDef.CONTRACT_VERSION /
	// BarSlot.CONTRACT_VERSION), surfaced as ViewsMixin.HEADER_CONTRACT_VERSION / ViewsMixin.BAR_CONTRACT_VERSION.
	// Two distinct constants: a bar-envelope revision must never force a header-sidecar bump, or vice-versa.
	const JUNEAU_HEADER_CONTRACT_VERSION = "1";
	const JUNEAU_BAR_CONTRACT_VERSION = "1";

	// DOM attribute names - MUST equal the attribute names used by juneau-views.js.
	const HEADER_MARKER = "data-juneau-app-header";
	const BAR_SLOT_MARKER = "data-juneau-bar-slot";
	const ACTION_MARKER = "data-juneau-header-action";
	const BEHAVIOR_ATTR = "data-juneau-behavior";
	const SAFE_ATTR = "data-juneau-safe";
	const ICON_ATTR = "data-juneau-icon";
	const BADGE_ATTR = "data-juneau-badge";
	const BADGE_MAX_ATTR = "data-juneau-badge-max";
	const AVATAR_MARKER = "data-juneau-avatar";
	const REFRESH_ATTR = "data-juneau-refresh";
	// A menu trigger's aria-controls points at its .jc-menu list; a marker records that a trigger is already wired.
	const MENU_CLASS = "jc-menu";
	const MENU_ITEM_CLASS = "jc-menu-item";
	const MENU_WIRED_ATTR = "data-juneau-menu-wired";
	const MENU_ITEMS_WIRED_ATTR = "data-juneau-menu-items-wired";
	// The ONE marker wireSafeActions and initAll share, so re-running initAll (the enhance-on-insert path a cloned
	// row-detail bar slot needs) can never bind a second click handler onto an already-wired SAFE action.  The avatar
	// image fallback carries the same guard for the same reason.
	const SAFE_WIRED_ATTR = "data-juneau-safe-wired";
	const AVATAR_WIRED_ATTR = "data-juneau-avatar-wired";
	const HEADER_SIDECAR_PREFIX = "juneau-header:";
	const BAR_SIDECAR_PREFIX = "juneau-bar:";

	// The CustomEvent a SAFE control dispatches for the host to act on (no built-in navigation, no new token type).
	const SAFE_EVENT = "juneau:chrome-safe";

	// SAFE host-dispatch token format - MUST equal the server SAFE_TOKEN pattern (HeaderAction / MenuItem):
	// a lowercase letter then up to 63 more lowercase-alnum-or-hyphen chars.  Defense-in-depth re-check.
	const SAFE_TOKEN_RE = /^[a-z][a-z0-9-]{0,63}$/;

	// ==================================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==================================================================================================================

	/**
	 * Client-side endpoint re-check before any demand-refresh fetch: same-origin AND non-templated - rejects an
	 * absolute URL ("scheme://"), a protocol-relative "//host", any "scheme:" prefix, a ".." path segment, and any
	 * "{...}" template placeholder.  Matches the Java same-origin rule the chrome beans validate() with; a chrome
	 * region is never row-scoped, so a "{id}" template is meaningless here and is rejected.
	 */
	function isSafeChromeEndpoint(path) {
		if (typeof path !== "string" || path.length === 0) return false;
		if (path.indexOf("{") >= 0) return false;                 // no template placeholder
		if (path.indexOf("://") >= 0) return false;               // absolute URL
		if (path.startsWith("//")) return false;                  // protocol-relative
		const colon = path.indexOf(":");
		const slash = path.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;   // "scheme:" before any slash (javascript:, servlet:)
		if (path === ".." || path.indexOf("../") >= 0 || path.indexOf("/..") >= 0) return false;
		return true;
	}

	/** True when a SAFE host-dispatch token is format-valid (mirrors the server SAFE_TOKEN pattern). */
	function isSafeToken(token) {
		return typeof token === "string" && SAFE_TOKEN_RE.test(token);
	}

	/** Fail-loud handshake predicate: a sidecar/refresh envelope's contractVersion must equal the baked-in expected value. */
	function envelopeContractOk(env, expected) {
		return typeof env === "object" && env?.contractVersion === expected;
	}

	/**
	 * Clamps a fresh count above `max` to "<max>+" (display text), as the views bar-slot painter does
	 * - so a demand-refresh re-paints the same clamped label.  Pure.
	 */
	function clampCount(count, max) {
		if (typeof count !== "number") return "";
		if (typeof max === "number" && count > max) return max + "+";
		return String(count);
	}

	/** Extracts the {namespaced-id -> count} map from a sidecar/refresh envelope; returns {} for a shapeless envelope. */
	function envelopeBadges(env) {
		return typeof env === "object" && env?.badges && typeof env.badges === "object" ? env.badges : {};
	}

	// ==================================================================================================================
	// DOM BINDING LAYER
	// ==================================================================================================================

	/** Resolves an icon glyph from the shared juneau-icons.js registry, or null when the registry/name is absent. */
	function resolveIconMarkup(name) {
		const icons = window.JuneauViews?.icons;
		return icons?.resolveIcon?.(name) ?? null;
	}

	/**
	 * Hydrates every [data-juneau-icon] action's empty .jc-icon span with its trusted first-party registry SVG.  This
	 * is an HTML-insertion sink whose trust basis is code provenance, not content provenance: the markup is
	 * registry-owned (juneau-icons.js), never request/response/app text - a different basis than renderFooter's and
	 * htmlCard's sinks, which trust server-template/config-authored content (also never request data, but
	 * author-supplied rather than code-shipped).
	 */
	function hydrateIcons(root) {
		if (!root || !root.querySelectorAll) return;
		const hosts = root.querySelectorAll("[" + ICON_ATTR + "]");
		for (const host of hosts) {
			const glyph = resolveIconMarkup(host.getAttribute(ICON_ATTR));
			if (!glyph) continue;
			const iconSpan = host.querySelector(".jc-icon");
			if (iconSpan) iconSpan.innerHTML = glyph;             // trusted registry markup only - the icon-registry sink
		}
	}

	/**
	 * Applies a fresh {namespaced-id -> count} map to a region's count badges via textContent only (never innerHTML):
	 * a badge whose id is absent from the map is left untouched, a non-numeric value is skipped, and a numeric value
	 * is re-clamped with the badge's own data-juneau-badge-max so it matches the server label.  Dot badges carry no
	 * count and are never in the map, so they are never rewritten.
	 */
	function applyCounts(root, counts) {
		if (!root || !root.querySelectorAll || !counts || typeof counts !== "object") return;
		const badges = root.querySelectorAll("[" + BADGE_ATTR + "]");
		for (const b of badges) {
			const id = b.getAttribute(BADGE_ATTR);
			if (!Object.hasOwn(counts, id)) continue;
			const raw = counts[id];
			if (typeof raw !== "number") continue;
			const maxAttr = b.getAttribute(BADGE_MAX_ATTR);
			const max = maxAttr != null ? Number.parseInt(maxAttr, 10) : null;
			b.textContent = clampCount(raw, max);                 // textContent only - a fresh count never reaches innerHTML
		}
	}

	/** Reads and JSON-parses a region's data-only sidecar (by "<prefix><id>" element id); returns null when absent/unparseable. */
	function readSidecar(prefix, id) {
		if (!window.document || !window.document.getElementById) return null;
		const el = window.document.getElementById(prefix + id);
		if (!el) return null;
		try {
			return JSON.parse(el.textContent);
		} catch (e) {
			return null;                                          // malformed/absent sidecar JSON - callers treat a null sidecar as "no sidecar"
		}
	}

	/**
	 * Resolves the ONE shared layer stack (window.JuneauViews.init, 445h/juneau-views.js) or null when views is not
	 * loaded.  This runtime NEVER defines pushLayer/popLayer of its own (Pass 5 M-P5-B1): it is strictly a client of
	 * the shared stack, so a chrome menu shares the same z-index / Escape / light-dismiss depth as views dialogs.
	 */
	function viewsLayerStack() {
		const views = window.JuneauViews?.init;
		return typeof views?.pushLayer === "function" && typeof views?.popLayer === "function" ? views : null;
	}

	/**
	 * chrome-local positioner: sets top/left (and shows) an ALREADY-portalled menu node under its trigger.  views'
	 * pushLayer already reparented it to body and set position:fixed; this only computes the cell-anchored offset (with
	 * a light viewport clamp), never re-parents.  positionCellPopover is not exported by views, so chrome carries this
	 * tiny helper rather than reaching into the views internals.
	 */
	function positionMenuUnderTrigger(menu, trigger) {
		if (!menu || !menu.style || !trigger || typeof trigger.getBoundingClientRect !== "function") return;
		const rect = trigger.getBoundingClientRect();
		const vw = (typeof window !== "undefined" && window.innerWidth) ? window.innerWidth : 1024;
		menu.style.display = "block";
		const w = menu.offsetWidth || 0;
		let left = rect.left;
		const top = rect.bottom + 4;
		if (left + w > vw - 4) left = Math.max(4, vw - w - 4);
		if (left < 4) left = 4;
		menu.style.left = left + "px";
		menu.style.top = top + "px";
	}

	/** Resolves a trigger's aria-controls'd .jc-menu list node (compared by id via getElementById, never interpolated). */
	function menuForTrigger(trigger) {
		const id = trigger?.getAttribute ? trigger.getAttribute("aria-controls") : null;
		if (!id || typeof document === "undefined" || typeof document.getElementById !== "function") return null;
		const el = document.getElementById(id);
		return el?.classList && Array.prototype.indexOf.call(el.classList, MENU_CLASS) >= 0 ? el : null;
	}

	// Single-open tracking: chrome menus are one-at-a-time (a new open closes any other), matching the row-action menus.
	let openMenuRec = null;

	/**
	 * Wires SAFE .jc-menu-item buttons in a list to dispatch the SAME host CustomEvent a top-level SAFE action uses
	 * (from the owning TRIGGER, which stays in the header, so the event bubbles through the header even though the list
	 * is portalled to body), then close the menu.  A format-invalid token is refused loud (hand-edited attribute only).
	 * LINK items are plain same-origin anchors (native navigation) - closing the menu on their click keeps no orphan
	 * open if navigation is intercepted.  Dividers are inert.  Idempotent per list.
	 */
	function wireMenuItems(menu, trigger, root) {
		if (!menu || !menu.querySelectorAll || menu.getAttribute(MENU_ITEMS_WIRED_ATTR) === "1") return;
		menu.setAttribute(MENU_ITEMS_WIRED_ATTR, "1");
		const safeItems = menu.querySelectorAll("." + MENU_ITEM_CLASS + "[" + SAFE_ATTR + "]");
		for (const item of safeItems) {
			const token = item.getAttribute(SAFE_ATTR);
			if (!isSafeToken(token)) {
				window.console?.error?.(
					"juneau-console.js: refusing to wire SAFE menu item with malformed token '" + token + "'.");
				continue;
			}
			if (!item.addEventListener) continue;
			item.addEventListener("click", function (ev) {
				ev?.preventDefault?.();
				dispatchSafe(trigger, token, root);
				closeMenu();
			});
		}
		const links = menu.querySelectorAll("a." + MENU_ITEM_CLASS);
		for (const link of links)
			if (link.addEventListener) link.addEventListener("click", function () { closeMenu(); });
	}

	/** Opens a trigger's menu on the shared views stack (no stack / no list -> inert, never a fake disclosure). */
	function openMenu(trigger, root) {
		const views = viewsLayerStack();
		const menu = menuForTrigger(trigger);
		if (!views || !menu) return;
		wireMenuItems(menu, trigger, root);
		views.pushLayer(menu, {
			kind: "menu", portal: true, lightDismiss: true, trapFocus: false, detachOnPop: false,
			returnFocusTo: trigger,
			onDismiss: function () {
				trigger.setAttribute?.("aria-expanded", "false");
				if (menu.style) menu.style.display = "none";
				if (openMenuRec?.menu === menu) openMenuRec = null;
			}
		});
		positionMenuUnderTrigger(menu, trigger);
		trigger.setAttribute?.("aria-expanded", "true");
		openMenuRec = { trigger: trigger, menu: menu };
	}

	/** Closes the open chrome menu (if any) by popping its layer off the shared views stack; onDismiss resets ARIA. */
	function closeMenu() {
		const views = viewsLayerStack();
		if (views && openMenuRec) views.popLayer(openMenuRec.menu);
	}

	/** Toggles a trigger's menu: a re-click on the open trigger closes it; opening one first closes any other. */
	function toggleMenu(trigger, root) {
		if (openMenuRec?.trigger === trigger) { closeMenu(); return; }
		if (openMenuRec) closeMenu();
		openMenu(trigger, root);
	}

	/**
	 * Wires each enabled Behavior.MENU trigger (header action or avatar chip) to toggle its .jc-menu list on the shared
	 * views layer stack.  Idempotent per trigger.  A trigger with no resolvable list / no views stack stays inert.
	 */
	function wireMenus(root) {
		if (!root || !root.querySelectorAll) return;
		const triggers = root.querySelectorAll("[" + BEHAVIOR_ATTR + "='menu']");
		for (const trigger of triggers) {
			if (trigger.getAttribute(MENU_WIRED_ATTR) === "1" || !trigger.addEventListener) continue;
			trigger.setAttribute(MENU_WIRED_ATTR, "1");
			(function (t) {
				t.addEventListener("click", function (ev) {
					ev?.preventDefault?.();
					toggleMenu(t, root);
				});
			})(trigger);
		}
	}

	/**
	 * Wires an avatar's image so a broken same-origin image falls back to the hidden initials chip (no dead avatar).
	 * Idempotent per image (AVATAR_WIRED_ATTR), so a re-scan cannot stack error handlers.
	 */
	function wireAvatarFallback(root) {
		if (!root || !root.querySelectorAll) return;
		const avatars = root.querySelectorAll("[" + AVATAR_MARKER + "]");
		for (const avatar of avatars) {
			const img = avatar.querySelector("img.jc-avatar-img");
			if (!img || !img.addEventListener) continue;
			if (img.getAttribute(AVATAR_WIRED_ATTR) === "1") continue;
			img.setAttribute(AVATAR_WIRED_ATTR, "1");
			img.addEventListener("error", function () {
				img.hidden = true;
				const initials = avatar.querySelector(".jc-avatar-initials");
				if (initials) initials.hidden = false;
			});
		}
	}

	/**
	 * Wires each fully-functional SAFE action to dispatch a bubbling CustomEvent the host listens for; the host does
	 * the work (there is no built-in navigation and no new token type).  A format-invalid token is refused loud - it
	 * can only be a hand-edited attribute, since the server already format-validates it.  This selects only top-level
	 * SAFE actions (data-juneau-header-action + behavior=safe); MENU triggers are wired separately by wireMenus, and
	 * SAFE items INSIDE a menu list by wireMenuItems.  LINK actions are plain anchors needing no wiring.
	 *
	 * Idempotent per action, guarded by SAFE_WIRED_ATTR - the marker this function SHARES with initAll.  initAll is
	 * now re-entrant on purpose (a row-detail bar slot cloned from a <template> is only enhanceable after insert), so
	 * without the shared marker a second scan would bind a second handler and one click would fire twice.
	 */
	function wireSafeActions(root) {
		if (!root || !root.querySelectorAll) return;
		const actions = root.querySelectorAll("[" + ACTION_MARKER + "][" + BEHAVIOR_ATTR + "='safe']");
		for (const el of actions) {
			const token = el.getAttribute(SAFE_ATTR);
			if (!isSafeToken(token)) {
				window.console?.error?.(
					"juneau-console.js: refusing to wire SAFE action with malformed token '" + token + "'.");
				continue;
			}
			if (!el.addEventListener) continue;
			if (el.getAttribute(SAFE_WIRED_ATTR) === "1") continue;
			el.setAttribute(SAFE_WIRED_ATTR, "1");
			el.addEventListener("click", function (ev) {
				ev?.preventDefault?.();
				dispatchSafe(el, token, root);
			});
		}
	}

	/** Dispatches the SAFE CustomEvent (bubbling) carrying the token, action id and its region root. */
	function dispatchSafe(el, token, root) {
		if (typeof window.CustomEvent !== "function" || !el.dispatchEvent) return;
		el.dispatchEvent(new window.CustomEvent(SAFE_EVENT, {
			bubbles: true,
			detail: { token: token, actionId: el.getAttribute(ACTION_MARKER), root: root }
		}));
	}

	/**
	 * Demand-refresh (no poller): fetches a region's data-juneau-refresh endpoint once, and on a contract-OK envelope
	 * re-applies its counts.  Returns a Promise<boolean> (false when there is no endpoint, it is unsafe, the fetch
	 * fails, or the handshake fails - a bad envelope never paints stale/foreign data).
	 */
	function refresh(root) {
		if (!root || !root.getAttribute) return Promise.resolve(false);
		const url = root.getAttribute(REFRESH_ATTR);
		if (!url || !isSafeChromeEndpoint(url)) return Promise.resolve(false);
		const expected = root.getAttribute(HEADER_MARKER) != null
			? JUNEAU_HEADER_CONTRACT_VERSION : JUNEAU_BAR_CONTRACT_VERSION;
		return window.fetch(url, { method: "GET", credentials: "same-origin", cache: "no-store" })
			.then(function (r) { return r.json(); })
			.then(function (env) {
				if (!envelopeContractOk(env, expected)) return false;
				applyCounts(root, envelopeBadges(env));
				return true;
			})
			.catch(function () { return false; });
	}

	/** Enhances one app-header: handshake, icon hydration, avatar fallback, SAFE wiring, initial-count apply. */
	function initHeader(header) {
		const id = header.getAttribute(HEADER_MARKER);
		const sidecar = readSidecar(HEADER_SIDECAR_PREFIX, id);
		if (sidecar && !envelopeContractOk(sidecar, JUNEAU_HEADER_CONTRACT_VERSION)) {
			window.console?.error?.(
				"juneau-console.js: app-header contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_HEADER_CONTRACT_VERSION + "'); leaving header un-enhanced.");
			return null;
		}
		hydrateIcons(header);
		wireAvatarFallback(header);
		wireSafeActions(header);
		wireMenus(header);
		if (sidecar) applyCounts(header, envelopeBadges(sidecar));
		return { root: header, refresh: function () { return refresh(header); } };
	}

	/** Enhances one bar slot: handshake + initial-count apply (bar widgets carry no icons or controls). */
	function initBarSlot(bar) {
		const id = bar.getAttribute(BAR_SLOT_MARKER);
		const sidecar = readSidecar(BAR_SIDECAR_PREFIX, id);
		if (sidecar && !envelopeContractOk(sidecar, JUNEAU_BAR_CONTRACT_VERSION)) {
			window.console?.error?.(
				"juneau-console.js: bar-slot contract mismatch (sidecar '" + sidecar.contractVersion
				+ "' != runtime '" + JUNEAU_BAR_CONTRACT_VERSION + "'); leaving bar slot un-enhanced.");
			return null;
		}
		if (sidecar) applyCounts(bar, envelopeBadges(sidecar));
		return { root: bar, refresh: function () { return refresh(bar); } };
	}

	/**
	 * DOMContentLoaded entry: enhance every app-header, then every bar slot (a bar id can repeat per sub-tab bar).
	 *
	 * Also the ENHANCE-ON-INSERT entry: the scan is document-wide, so a bar slot cloned from a row-detail
	 * {@code <template>} and inserted after load is picked up by simply calling this again (juneau-views.js does,
	 * once per expanded row).  Re-entrancy is safe because every binding step is marker-guarded - SAFE_WIRED_ATTR for
	 * SAFE actions (shared with wireSafeActions), AVATAR_WIRED_ATTR, MENU_WIRED_ATTR - and the remaining work
	 * (handshake, icon hydration, count apply) is idempotent by construction.  Still no poller: this only ever runs
	 * when something asks it to.
	 */
	function initAll() {
		const out = { headers: [], bars: [] };
		const headers = window.document.querySelectorAll("[" + HEADER_MARKER + "]");
		for (const header of headers) {
			const ctl = initHeader(header);
			if (ctl) out.headers.push(ctl);
		}
		const bars = window.document.querySelectorAll("[" + BAR_SLOT_MARKER + "]");
		for (const bar of bars) {
			const ctl = initBarSlot(bar);
			if (ctl) out.bars.push(ctl);
		}
		return out;
	}

	const chrome = Object.freeze({
		initAll: initAll,
		initBarSlot: initBarSlot,
		initHeader: initHeader,
		refresh: refresh,
		isSafeChromeEndpoint: isSafeChromeEndpoint,
		SAFE_EVENT: SAFE_EVENT,
		readSidecar: readSidecar,
		JUNEAU_BAR_CONTRACT_VERSION: JUNEAU_BAR_CONTRACT_VERSION
	});

	handlers.set("html", htmlCard);
	handlers.set("datatables", datatablesCard);

	window.JuneauConsole = {
		SUPPORTED_CONTRACT_VERSIONS: SUPPORTED_CONTRACT_VERSIONS,
		JuneauConsoleError: JuneauConsoleError,
		mount: mount,
		registerCard: registerCard,
		contract: () => mounted ? mounted.contract : null,
		card: id => mounted ? mounted.cards.get(id) || null : null,
		chrome: chrome
	};

	// Auto-mount, synchronously (§2 rule 4).
	const doc = window.document;
	const island = doc && doc.getElementById("juneau-page");
	if (island) {
		let contract;
		try {
			contract = JSON.parse(island.textContent);
		} catch (e) {
			fail(doc, "E-JS-1", e.message);
		}
		mount(contract, { root: island.parentNode, document: doc });
	} else if (doc && doc.querySelector("template[data-card], template[data-slot]")) {
		fail(doc, "E-JS-1", "no #juneau-page island");
	}
	if (!island && doc) onReady(doc, function () { chrome.initAll(); });
})();
