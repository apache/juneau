/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  The ASF licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * juneau-console-output.js - the console-output region: a live-tailing, append-only log pane.
 *
 * Publishes JuneauViews.consoleOutput = { CONTRACT_VERSION, create, poll, mount, validateLine, isSafeColor,
 * isSafeLineHref, isSafeLineImageSrc } and registers the "console-output" region populator.
 *
 * LOAD ORDER IS A CONTRACT (see ViewsMixin#CONSOLE_OUTPUT_JS_PATH): after juneau-views.js (URL helpers) and
 * juneau-regions.js (the populator registry); before juneau-helpers.js.  JuneauViews.helpers.icon is resolved at
 * call time, so the module does not need helpers to be loaded first.
 *
 * INVARIANTS:
 *   - Line data reaches the DOM only through textContent, setAttribute on allowlisted attributes, or the CSSOM.
 *     Icons are nodes built by JuneauViews.helpers.icon from the app-controlled icon registry.
 *   - mount() is synchronous and returns a cleanup; poll() never rejects (spec section 4.1).
 *   - One request in flight per console; one earlier-lines request in flight per console.
 */
(function () {
	"use strict";

	const NS = window.JuneauViews;
	const LOG_TAG = "[juneau-console-output]";
	if (!NS || !NS.init) {
		console.error(LOG_TAG, "juneau-console-output.js must load after juneau-views.js");
		return;
	}

	const CONTRACT_VERSION = "1";

	/** Instance API object -> internal state.  poll() looks its instance up here. */
	const INTERNAL = new WeakMap();

	// @section:checks
	// Mirrors ConsoleOutputChecks.java; the shared console-output-vectors.json pins both.  Java's \s is ASCII-only,
	// so the RGB pattern spells out [ \t\n\x0B\f\r] instead of using JS's Unicode-aware \s.
	const WS = "[ \\t\\n\\x0B\\f\\r]*";
	const HEX_COLOR = /^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;
	const RGB_COLOR = new RegExp("^rgb\\(" + WS + "(\\d{1,3})" + WS + "," + WS + "(\\d{1,3})" + WS + "," + WS
		+ "(\\d{1,3})" + WS + "\\)$");
	const FRAGMENT_HREF = /^#[A-Za-z0-9._:~-]{0,128}$/;
	const ICON_NAME = /^[a-z][A-Za-z0-9.-]{0,63}$/;
	const TOKEN = /^[A-Za-z0-9._~-]{1,128}$/;
	const ANCHOR_PREFIX = /^[A-Za-z][A-Za-z0-9_-]{0,31}$/;
	const ISO_Z = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z$/;
	const LEVELS = ["SEVERE", "WARNING", "INFO", "FINE"];
	const STYLES = ["success", "warn", "error", "muted", "accent"];
	const LEVEL_STYLE = { SEVERE: "error", WARNING: "warn", INFO: null, FINE: "muted" };
	const MAX_FRAGS = 512;
	const MAX_TOOLTIP = 2048;
	const MAX_LABEL = 256;
	const MAX_WIDTH = 4096;

	function isStr(v) { return typeof v === "string"; }
	function isPlainObject(v) { return v !== null && typeof v === "object" && !Array.isArray(v); }
	function has(o, k) { return Object.prototype.hasOwnProperty.call(o, k); }
	function stripTabCrLf(s) { return s.replace(/[\t\r\n]/g, ""); }

	/**
	 * A pure-string port of RegionDef.isSafeDetailEndpoint, after folding "\\" to "/": non-blank, no "://", no "//"
	 * prefix, no scheme colon before the first slash, and no ".." segment.  Relative paths are allowed.  It never
	 * consults location, so the shared vectors run unchanged on both sides.
	 */
	function isSafePath(t) {
		const f = t.replace(/\\/g, "/");
		if (/^[ \t\n\x0B\f\r\x1C-\x1F]*$/.test(f) || f.includes("://") || f.startsWith("//"))
			return false;
		const colon = f.indexOf(":");
		const slash = f.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash))
			return false;
		return f.split("/").indexOf("..") < 0;
	}

	function isSafeColor(s) {
		if (!isStr(s))
			return false;
		if (HEX_COLOR.test(s))
			return true;
		const m = RGB_COLOR.exec(s);
		return !!m && Number(m[1]) <= 255 && Number(m[2]) <= 255 && Number(m[3]) <= 255;
	}

	function isSafeLineHref(s) {
		if (!isStr(s))
			return false;
		const t = stripTabCrLf(s);
		return t.startsWith("#") ? FRAGMENT_HREF.test(t) : isSafePath(t);
	}

	function isSafeLineImageSrc(s) {
		if (!isStr(s))
			return false;
		const t = stripTabCrLf(s);
		return !t.startsWith("#") && isSafePath(t);
	}

	/** The stored form of a value that passed isSafeLineHref / isSafeLineImageSrc. */
	function normalizeUrl(s) {
		const t = stripTabCrLf(s);
		return t.startsWith("#") ? t : t.replace(/\\/g, "/");
	}

	function isToken(s) { return isStr(s) && TOKEN.test(s); }
	function isAnchorPrefix(s) { return isStr(s) && ANCHOR_PREFIX.test(s); }

	/** Truncates a value for an error message: 64 characters plus an ellipsis (ConsoleOutputChecks.clip). */
	function clip(s) {
		const v = isStr(s) ? s : String(s);
		return v.length > 64 ? v.slice(0, 64) + "…" : v;
	}

	function cap(s, n) { return s.length > n ? s.slice(0, n) : s; }

	function copyColorOrStyle(src, dst, problems) {
		if (has(src, "color")) {
			if (isSafeColor(src.color))
				dst.color = src.color;
			else
				problems.push({ code: "E-CO-7", kind: "color", value: clip(src.color) });
		}
		if (STYLES.indexOf(src.style) >= 0)
			dst.style = src.style;
	}

	/** One fragment, or null when it is structurally invalid (which drops the whole line). */
	function validateFrag(raw, problems) {
		if (!isPlainObject(raw))
			return null;
		const f = {};
		if (raw.block === true) {
			if (has(raw, "text") || has(raw, "bold"))
				return null;
			f.block = true;
			copyColorOrStyle(raw, f, problems);
			if (isStr(raw.tooltip))
				f.tooltip = cap(raw.tooltip, MAX_TOOLTIP);
			if (has(raw, "href")) {
				if (isSafeLineHref(raw.href))
					f.href = normalizeUrl(raw.href);
				else
					problems.push({ code: "E-CO-7", kind: "href", value: clip(raw.href) });
			}
			if (isStr(raw.label))
				f.label = cap(raw.label, MAX_LABEL);
			return f;
		}
		if ((has(raw, "block") && raw.block !== false) || has(raw, "tooltip") || has(raw, "href") || has(raw, "label"))
			return null;
		if (!isStr(raw.text))
			return null;
		f.text = raw.text;
		copyColorOrStyle(raw, f, problems);
		if (raw.bold === true)
			f.bold = true;
		return f;
	}

	function validateUi(raw, problems) {
		const ui = {};
		copyColorOrStyle(raw, ui, problems);
		if (has(raw, "icon")) {
			if (isStr(raw.icon) && ICON_NAME.test(raw.icon))
				ui.icon = raw.icon;
			else
				problems.push({ code: "E-CO-6", value: clip(raw.icon) });
		}
		if (has(raw, "image")) {
			const im = raw.image;
			if (!isPlainObject(im) || !isSafeLineImageSrc(im.src))
				problems.push({ code: "E-CO-7", kind: "image src", value: clip(isPlainObject(im) ? im.src : im) });
			else if (!isStr(im.alt))
				problems.push({ code: "E-CO-7", kind: "image", value: "missing alt" });
			else {
				ui.image = { src: normalizeUrl(im.src), alt: im.alt };
				if (Number.isInteger(im.width) && im.width >= 1 && im.width <= MAX_WIDTH)
					ui.image.width = im.width;
			}
		}
		if (raw.marker === true)
			ui.marker = true;
		return Object.keys(ui).length ? ui : null;
	}

	/**
	 * Re-validates one line defensively (spec 2.7, 5.1).  Copies only allowlisted members into a fresh object, so
	 * unknown members are ignored silently.  Returns {line: null} for a structurally invalid line (E-CO-3), or the
	 * normalised line with removed members listed in problems (E-CO-6 / E-CO-7).
	 */
	function validateLine(input) {
		const problems = [];
		function drop(detail) {
			problems.push({ code: "E-CO-3", detail: detail });
			return { line: null, problems: problems };
		}
		if (!isPlainObject(input))
			return drop("not an object");
		const hasText = input.text !== undefined;
		const hasFrags = input.frags !== undefined;
		if (hasText === hasFrags)
			return drop(hasText ? "both text and frags" : "neither text nor frags");
		const line = {};
		if (input.n !== undefined) {
			if (!Number.isInteger(input.n) || input.n < 1)
				return drop("n must be an integer >= 1");
			line.n = input.n;
		}
		const level = input.level === undefined ? "INFO" : input.level;
		if (LEVELS.indexOf(level) < 0)
			return drop("unknown level");
		line.level = level;
		if (isStr(input.instant) && ISO_Z.test(input.instant) && !Number.isNaN(Date.parse(input.instant)))
			line.instant = input.instant;
		if (hasText) {
			if (!isStr(input.text))
				return drop("text must be a string");
			line.text = input.text;
		} else {
			if (!Array.isArray(input.frags) || input.frags.length < 1 || input.frags.length > MAX_FRAGS)
				return drop("frags must be an array of 1-" + MAX_FRAGS + " fragments");
			line.frags = [];
			for (const raw of input.frags) {
				const f = validateFrag(raw, problems);
				if (f === null)
					return drop("malformed fragment");
				line.frags.push(f);
			}
		}
		if (isPlainObject(input.ui)) {
			const ui = validateUi(input.ui, problems);
			if (ui)
				line.ui = ui;
		}
		if (input.open === true)
			line.open = true;
		return { line: line, problems: problems };
	}

	// @section:errors
	// Shapes mirror juneau-console.js's MSG/FATAL tables: console.error(LOG_TAG, code, message) plus, for fatal
	// codes after mount, an inline .jc-console-error[role=alert] banner inside the console root (spec section 9).
	const MSG = {
		"E-CO-1": "console-output region '%s': %s",
		"E-CO-2": "console-output region '%s': response contractVersion '%s' is not '1'",
		"E-CO-3": "console-output region '%s': dropped %d malformed line(s); first: %s",
		"E-CO-4": "console-output region '%s': fetching lines failed (%s)",
		"E-CO-5": "console-output region '%s': invalid continuation token or non-increasing line numbers",
		"E-CO-6": "console-output region '%s': unknown icon '%s' ignored",
		"E-CO-7": "console-output region '%s': unsafe %s '%s' ignored",
		"E-CO-8": "console-output region '%s': line %d not found",
		"E-CO-9": "console-output region '%s': anchor prefix '%s' already in use on this page; anchors disabled for this console"
	};
	const GONE_TEXT = "the log was replaced or truncated (410 Gone); reload to view it from the start";

	function message(code, args) {
		let i = 0;
		return MSG[code].replace(/%[sd]/g, function () {
			const v = args[i++];
			return v === undefined ? "" : String(v);
		});
	}

	class JuneauConsoleOutputError extends Error {
		constructor(code, msg) {
			super(msg);
			this.name = "JuneauConsoleOutputError";
			this.code = code;
		}
	}

	/** E-CO-1: logs and returns the error for the caller to throw (thrown from create/mount, before any DOM). */
	function configError(id, detail) {
		const m = message("E-CO-1", [id, detail]);
		console.error(LOG_TAG, "E-CO-1", m);
		return new JuneauConsoleOutputError("E-CO-1", m);
	}

	/** A non-fatal code, logged once per instance per key (key defaults to the code). */
	function warn(inst, code, args, key) {
		const k = key || code;
		if (inst.warned.has(k))
			return;
		inst.warned.add(k);
		console.warn(LOG_TAG, code, message(code, args));
	}

	function emitBus(inst, msg) {
		if (!inst.emit)
			return;
		try {
			inst.emit(msg);
		} catch (e) {
			console.warn(LOG_TAG, "bus emit failed", e);
		}
	}

	/**
	 * A fatal code after mount (E-CO-2, fatal E-CO-4, E-CO-5): never throws.  Stops polling and the ticker, keeps the
	 * rows, renders the banner as the root's first child, marks the nearest region (or the host) as errored without
	 * going through the runtime's setRegionState (which would repaint and wipe the rows), and publishes the state.
	 */
	function fatal(inst, code, args) {
		if (inst.dead || inst.destroyed)
			return;
		inst.dead = true;
		const m = message(code, args);
		console.error(LOG_TAG, code, m);
		if (inst.stopPoll)
			inst.stopPoll();
		if (inst.stopTicker)
			inst.stopTicker();
		if (!inst.banner) {
			inst.banner = mk("div", "jc-console-error");
			inst.banner.setAttribute("role", "alert");
			inst.root.insertBefore(inst.banner, inst.root.firstChild);
		}
		const item = mk("div", "jc-console-error-item");
		item.setAttribute("data-juneau-error", code);
		item.textContent = m;
		inst.banner.appendChild(item);
		const host = (typeof inst.el.closest === "function" && inst.el.closest("[data-juneau-region]")) || inst.el;
		host.setAttribute("data-juneau-region-state", "error");
		inst.state = "error";
		emitBus(inst, { kind: "console-output.state", id: inst.id, state: "error", terminal: inst.terminal });
	}

	function report(inst, problems) {
		for (const p of problems) {
			if (p.code === "E-CO-6")
				warn(inst, "E-CO-6", [inst.id, p.value]);
			else if (p.code === "E-CO-7")
				warn(inst, "E-CO-7", [inst.id, p.kind, p.value], "E-CO-7:" + p.kind);
		}
	}

	function safeJson(v) {
		try {
			return JSON.stringify(v);
		} catch (e) { // NOSONAR javascript:S2486 -- an unserialisable value is reported by its String() form instead
			return String(v);
		}
	}

	// @section:render
	// Line data reaches the DOM only via textContent, setAttribute on allowlisted attributes, and the CSSOM.

	function mk(tag, cls) {
		const e = document.createElement(tag);
		if (cls)
			e.className = cls;
		return e;
	}

	/** Resolved line colour (spec 2.5): ui.color, then ui.style, then the level default, else inherit. */
	function lineTone(line) {
		const ui = line.ui || {};
		if (ui.color)
			return { color: ui.color };
		if (ui.style)
			return { style: ui.style };
		const d = LEVEL_STYLE[line.level];
		return d ? { style: d } : {};
	}

	function applyTone(node, tone) {
		if (tone.color)
			node.style.color = tone.color;
		else if (tone.style)
			node.classList.add("juneau-co-s-" + tone.style);
	}

	function renderTextFrag(f) {
		const s = mk("span", f.bold ? "juneau-co-f juneau-co-b" : "juneau-co-f");
		applyTone(s, f);
		s.textContent = f.text;
		return s;
	}

	function renderBlock(f) {
		const b = mk(f.href ? "a" : "span", "juneau-co-block");
		if (f.color)
			b.style.backgroundColor = f.color;
		else if (f.style)
			b.classList.add("juneau-co-fill-" + f.style);
		else
			b.classList.add("juneau-co-block-empty");
		const name = f.label || f.tooltip;
		if (f.href) {
			b.setAttribute("href", f.href);
			if (!f.href.startsWith("#"))
				b.setAttribute("rel", "nofollow noreferrer");
			if (name)
				b.setAttribute("aria-label", name);
		} else if (f.tooltip) {
			b.setAttribute("tabindex", "0");
			b.setAttribute("role", "img");
			b.setAttribute("aria-label", name);
		} else if (f.label) {
			b.setAttribute("role", "img");
			b.setAttribute("aria-label", f.label);
		} else {
			b.setAttribute("aria-hidden", "true");
		}
		if (f.tooltip)
			b.setAttribute("data-juneau-co-tip", f.tooltip);
		return b;
	}

	function isMarkerLine(inst, line) {
		if (line.ui && line.ui.marker === true)
			return true;
		if (!inst.classify)
			return false;
		try {
			return inst.classify(line) === "marker";
		} catch (e) {
			console.warn(LOG_TAG, "classify() threw; line treated as a normal line", e);
			return false;
		}
	}

	function renderLine(inst, line) {
		const row = mk("div", "juneau-co-line");
		row.setAttribute("data-n", String(line.n));
		row.setAttribute("data-level", line.level);
		if (line.open) {
			row.classList.add("juneau-co-open");
			row.setAttribute("aria-live", "off");
		}
		if (isMarkerLine(inst, line)) {
			row.setAttribute("data-marker", "");
			row.classList.add("juneau-co-marker");
			markerSeen(inst);
		}
		if (inst.anchors)
			row.id = inst.prefix + line.n;
		const gutter = mk("a", "juneau-co-gutter");
		gutter.setAttribute("tabindex", "-1");
		if (inst.anchors)
			gutter.setAttribute("href", "#" + inst.prefix + line.n);
		if (line.instant)
			gutter.setAttribute("title", line.instant);
		gutter.textContent = String(line.n);
		row.appendChild(gutter);
		if (inst.showTime && line.instant) {
			const tm = mk("span", "juneau-co-time");
			tm.setAttribute("aria-hidden", "true");
			tm.textContent = new Date(Date.parse(line.instant)).toISOString().slice(11, 23);
			row.appendChild(tm);
		}
		const ui = line.ui || {};
		if (ui.icon) {
			const ic = NS.helpers && typeof NS.helpers.icon === "function" ? NS.helpers.icon(ui.icon) : null;
			if (!ic || ic.hidden) {
				warn(inst, "E-CO-6", [inst.id, ui.icon]);
			} else {
				ic.classList.add("juneau-co-icon");
				row.appendChild(ic);
			}
		}
		const body = mk("span", "juneau-co-text");
		applyTone(body, lineTone(line));
		if (line.level === "SEVERE" || line.level === "WARNING") {
			const sr = mk("span", "juneau-co-sr");
			sr.textContent = line.level === "SEVERE" ? "Error: " : "Warning: ";
			body.appendChild(sr);
		}
		if (line.frags) {
			for (const f of line.frags)
				body.appendChild(f.block ? renderBlock(f) : renderTextFrag(f));
		} else {
			body.appendChild(document.createTextNode(line.text));
		}
		row.appendChild(body);
		if (ui.image) {
			const im = mk("img", "juneau-co-image");
			im.setAttribute("src", ui.image.src);
			im.setAttribute("alt", ui.image.alt);
			im.setAttribute("loading", "lazy");
			im.setAttribute("decoding", "async");
			im.setAttribute("referrerpolicy", "same-origin");
			if (ui.image.width)
				im.setAttribute("width", String(ui.image.width));
			row.appendChild(im);
			// An image that loads after the row was pinned grows the row; re-pin while following the tail.
			im.addEventListener("load", function () {
				if (!inst.destroyed && inst.stuck && !inst.holdUnstuck)
					scrollToBottom(inst);
			}, { once: true });
		}
		return row;
	}

	// @section:status

	function pad2(n) { return n < 10 ? "0" + n : String(n); }

	/** HH:MM:SS, or Nd HH:MM:SS above 99 hours (spec 5.3). */
	function fmtDur(ms) {
		const total = Math.max(0, Math.floor(ms / 1000));
		const s = total % 60;
		const m = Math.floor(total / 60) % 60;
		const h = Math.floor(total / 3600);
		if (h > 99)
			return Math.floor(h / 24) + "d " + pad2(h % 24) + ":" + pad2(m) + ":" + pad2(s);
		return pad2(h) + ":" + pad2(m) + ":" + pad2(s);
	}

	function tick(inst) {
		inst.lastElapsed = Date.now() + inst.skew - inst.startedAt;
		inst.elapsedEl.textContent = "Elapsed " + fmtDur(inst.lastElapsed);
	}

	/** A 1 s setTimeout chain (never setInterval); local arithmetic, so it keeps running in a hidden tab. */
	function startTicker(inst) {
		tick(inst);
		if (inst.tickTimer !== null)
			return;
		const loop = function () {
			inst.tickTimer = setTimeout(function () {
				tick(inst);
				loop();
			}, 1000);
		};
		loop();
		inst.stopTicker = function () {
			clearTimeout(inst.tickTimer);
			inst.tickTimer = null;
		};
	}

	function stopTicker(inst) {
		if (inst.stopTicker)
			inst.stopTicker();
	}

	function setStatus(inst, s) {
		if (inst.destroyed || inst.dead || !isPlainObject(s))
			return;
		if (isStr(s.now)) {
			const p = Date.parse(s.now);
			if (!Number.isNaN(p))
				inst.skew = p - Date.now();
		}
		const state = isStr(s.state) ? s.state : inst.state;
		const terminal = s.terminal === true;
		inst.stateEl.textContent = state || "";
		for (const st of STYLES)
			inst.stateEl.classList.remove("juneau-co-s-" + st);
		if (STYLES.indexOf(s.stateStyle) >= 0)
			inst.stateEl.classList.add("juneau-co-s-" + s.stateStyle);
		const startedAt = isStr(s.startedAt) ? Date.parse(s.startedAt) : Number.NaN;
		if (terminal) {
			stopTicker(inst);
			let ms = null;
			if (Number.isFinite(s.durationMs) && s.durationMs >= 0)
				ms = s.durationMs;
			else if (Number.isFinite(inst.lastElapsed))
				ms = inst.lastElapsed;
			else if (Number.isFinite(startedAt))
				ms = Date.now() + inst.skew - startedAt;
			inst.elapsedEl.textContent = ms === null ? "" : "Duration " + fmtDur(ms);
		} else if (Number.isFinite(startedAt)) {
			inst.startedAt = startedAt;
			startTicker(inst);
		} else {
			stopTicker(inst);
			inst.elapsedEl.textContent = "Waiting to start…";
		}
		if (state && state !== inst.state) {
			inst.state = state;
			inst.liveEl.textContent = (inst.title ? inst.title + ": " : "") + state;
			emitBus(inst, { kind: "console-output.state", id: inst.id, state: state, terminal: terminal });
		}
		if (terminal && !inst.terminal) {
			inst.terminal = true;
			emitBus(inst, { kind: "console-output.terminal", id: inst.id, state: state });
			resolveQueuedTarget(inst);
		}
	}

	// @section:scroll
	const STUCK_PX = 4;

	function isPaneStuck(inst) {
		if (inst.holdUnstuck)
			return false;
		const p = inst.pane;
		return p.scrollHeight - p.scrollTop - p.clientHeight <= STUCK_PX;
	}

	function scrollToBottom(inst) {
		const p = inst.pane;
		p.scrollTop = p.scrollHeight;
		inst.guardTop = p.scrollTop;
	}

	function updateJump(inst) {
		inst.jumpBtn.hidden = inst.stuck;
		inst.jumpBtn.textContent = inst.newCount > 0 ? "Jump to latest (" + inst.newCount + " new)" : "Jump to latest";
	}

	/** role=log is polite already; turn it off while scrolled up or during a more:true backfill burst (spec 8). */
	function updateLive(inst) {
		inst.pane.setAttribute("aria-live", inst.stuck && !inst.backfilling ? "polite" : "off");
	}

	function afterAppend(inst, wasStuck, added) {
		if (wasStuck) {
			scrollToBottom(inst);
			inst.stuck = true;
			inst.newCount = 0;
		} else {
			inst.stuck = false;
			inst.newCount += added;
		}
		updateJump(inst);
		updateLive(inst);
	}

	function onScroll(inst) {
		const p = inst.pane;
		if (inst.guardTop !== null) {
			const g = inst.guardTop;
			inst.guardTop = null;
			if (Math.abs(p.scrollTop - g) <= 1)
				return;
		}
		if (inst.holdUnstuck)
			return;
		inst.stuck = isPaneStuck(inst);
		if (inst.stuck)
			inst.newCount = 0;
		updateJump(inst);
		updateLive(inst);
	}

	function clearTarget(inst) {
		if (inst.target) {
			inst.target.classList.remove("juneau-co-target");
			inst.target.classList.remove("juneau-co-reveal");
			inst.target = null;
		}
	}

	/** Unstick, highlight (rows only), centre and focus; a hidden marker row is shown while it is the target. */
	function reveal(inst, node) {
		inst.stuck = false;
		inst.holdUnstuck = true;
		clearTarget(inst);
		const isRow = node.classList.contains("juneau-co-line");
		if (isRow) {
			node.classList.add("juneau-co-target");
			if (node.hasAttribute("data-marker"))
				node.classList.add("juneau-co-reveal");
			inst.target = node;
		}
		node.scrollIntoView({ block: "center", behavior: "auto" });
		inst.guardTop = inst.pane.scrollTop;
		const focusEl = isRow ? node.querySelector(".juneau-co-gutter") : (node.querySelector("button, a") || null);
		if (focusEl)
			focusEl.focus({ preventScroll: true });
		updateJump(inst);
		updateLive(inst);
	}

	/** E-CO-8 (once per line) and the fallback: the last row, the top-of-pane control, or the nearest following row. */
	function notFound(inst, n) {
		warn(inst, "E-CO-8", [inst.id, n], "E-CO-8:" + n);
		let node = null;
		if (n > inst.last)
			node = inst.rowsByN.get(inst.last) || null;
		else if (n < inst.first && inst.earlierEl)
			node = inst.earlierEl;
		else {
			for (const r of inst.pane.children) {
				if (r.classList.contains("juneau-co-line") && Number(r.getAttribute("data-n")) > n) {
					node = r;
					break;
				}
			}
		}
		if (node)
			reveal(inst, node);
	}

	function scrollToLine(inst, n) {
		if (inst.destroyed || !Number.isSafeInteger(n) || n < 1)
			return false;
		const row = inst.rowsByN.get(n);
		if (row) {
			inst.queued = null;
			reveal(inst, row);
			return true;
		}
		if (n > inst.last && !inst.terminal && !inst.dead) {
			inst.queued = n;
			return false;
		}
		if (n < inst.first && inst.hasEarlier) {
			inst.queued = n;
			autoLoadEarlier(inst);
			return false;
		}
		inst.queued = null;
		notFound(inst, n);
		return false;
	}

	/** Called after every append, prepend and terminal status. */
	function resolveQueuedTarget(inst) {
		const n = inst.queued;
		if (n === null)
			return;
		const row = inst.rowsByN.get(n);
		if (row) {
			inst.queued = null;
			reveal(inst, row);
		} else if ((n > inst.last && inst.terminal) || (n < inst.first && !inst.hasEarlier && !inst.loadingEarlier)) {
			inst.queued = null;
			notFound(inst, n);
		}
	}

	/** The line number in location.hash for this console's prefix, or null. */
	function hashTarget(inst) {
		const h = window.location && window.location.hash;
		if (!isStr(h) || !h.startsWith("#" + inst.prefix))
			return null;
		const rest = h.slice(1 + inst.prefix.length);
		if (!/^\d{1,15}$/.test(rest))
			return null;
		return Number(rest);
	}

	function wire(inst) {
		const pane = inst.pane;
		listen(inst, pane, "scroll", function () { onScroll(inst); }, { passive: true });
		const release = function () { inst.holdUnstuck = false; };
		for (const type of ["wheel", "keydown", "pointerdown", "touchstart"])
			listen(inst, pane, type, release, { passive: true });
		listen(inst, inst.jumpBtn, "click", function () {
			inst.holdUnstuck = false;
			clearTarget(inst);
			scrollToBottom(inst);
			inst.stuck = true;
			inst.newCount = 0;
			updateJump(inst);
			updateLive(inst);
		});
		if (inst.anchors) {
			const initial = hashTarget(inst);
			if (initial !== null)
				inst.queued = initial;
			listen(inst, window, "hashchange", function () {
				const n = hashTarget(inst);
				if (n !== null)
					scrollToLine(inst, n);
			});
		}
		wireTooltip(inst);
		wireMarkers(inst);
	}

	// @section:earlier
	function isAbort(err) {
		return !!err && err.name === "AbortError";
	}

	/** E-CO-4's "%s": "HTTP <status>" for a response, the error message (or "network error") otherwise. */
	function describeFailure(err) {
		if (err && Number.isInteger(err.status))
			return "HTTP " + err.status;
		return clip((err && err.message) || "network error");
	}

	function ensureControl(inst) {
		if (!inst.earlierEl) {
			inst.earlierEl = mk("div", "juneau-co-control");
			inst.pane.insertBefore(inst.earlierEl, inst.pane.firstChild);
		}
		return inst.earlierEl;
	}

	function removeControl(inst) {
		if (inst.earlierEl) {
			inst.earlierEl.remove();
			inst.earlierEl = null;
			inst.earlierBtn = null;
		}
	}

	function showEarlierButton(inst) {
		const c = ensureControl(inst);
		if (inst.earlierBtn)
			return;
		c.textContent = "";
		const b = mk("button", "juneau-co-earlier");
		b.setAttribute("type", "button");
		b.textContent = "Load earlier lines";
		listen(inst, b, "click", function () { loadEarlier(inst); });
		c.appendChild(b);
		inst.earlierBtn = b;
	}

	/** The permanent "Earlier lines not shown" notice (spec 5.1, 5.9). */
	function showNotice(inst) {
		const c = ensureControl(inst);
		c.textContent = "";
		inst.earlierBtn = null;
		const s = mk("span", "juneau-co-earlier-notice");
		s.appendChild(document.createTextNode(inst.downloadUrl ? "Earlier lines not shown — " : "Earlier lines not shown"));
		if (inst.downloadUrl) {
			const a = mk("a", "juneau-co-earlier-download");
			a.setAttribute("href", inst.downloadUrl);
			a.setAttribute("download", "");
			a.textContent = "Download full log";
			s.appendChild(a);
		}
		c.appendChild(s);
	}

	function onTrimmed(inst) {
		showNotice(inst);
	}

	function announce(inst, text) {
		inst.announceEl.textContent = text;
	}

	/** Earlier history only (spec 5.9).  Also seeds hasEarlier/before when called with no lines.  Returns rows added. */
	function prepend(inst, lines, meta) {
		if (inst.destroyed || inst.dead)
			return 0;
		const list = Array.isArray(lines) ? lines : [lines];
		const m = isPlainObject(meta) ? meta : {};
		const valid = [];
		let dropped = 0;
		let firstBad;
		for (const raw of list) {
			const r = validateLine(raw);
			report(inst, r.problems);
			if (!r.line || r.line.n === undefined) {
				if (dropped++ === 0)
					firstBad = raw;
				continue;
			}
			valid.push(r.line);
		}
		if (dropped)
			warn(inst, "E-CO-3", [inst.id, dropped, clip(safeJson(firstBad))]);
		for (let i = 1; i < valid.length; i++) {
			if (valid[i].n <= valid[i - 1].n) {
				fatal(inst, "E-CO-5", [inst.id]);
				return 0;
			}
		}
		const hasEarlier = m.hasEarlier === true;
		if (hasEarlier && !isToken(m.before)) {
			fatal(inst, "E-CO-5", [inst.id]);
			return 0;
		}
		// After a trim the client holds no before token for the gap, so nothing more is inserted above it (spec 5.1).
		let keep = inst.trimmed ? [] : valid.filter(function (l) { return inst.first === 0 || l.n < inst.first; });
		const room = Math.max(0, inst.maxDomRows - inst.count);
		const capped = keep.length > room;
		if (capped)
			keep = keep.slice(keep.length - room);

		const p = inst.pane;
		const fromBottom = p.scrollHeight - p.scrollTop;
		if (keep.length) {
			const frag = document.createDocumentFragment();
			for (const line of keep) {
				const row = renderLine(inst, line);
				frag.appendChild(row);
				inst.rowsByN.set(line.n, row);
				inst.count++;
			}
			p.insertBefore(frag, inst.earlierEl ? inst.earlierEl.nextSibling : p.firstChild);
			inst.first = keep[0].n;
			if (inst.last === 0)
				inst.last = keep[keep.length - 1].n;
		}
		if (!inst.trimmed) {
			if (capped) {
				inst.trimmed = true;
				inst.hasEarlier = false;
				inst.before = null;
				showNotice(inst);
			} else {
				inst.hasEarlier = hasEarlier;
				inst.before = hasEarlier ? m.before : null;
				if (hasEarlier)
					showEarlierButton(inst);
				else
					removeControl(inst);
			}
		}
		p.scrollTop = p.scrollHeight - fromBottom;
		inst.guardTop = p.scrollTop;
		if (keep.length)
			announce(inst, "Loaded " + keep.length + " earlier line" + (keep.length === 1 ? "" : "s"));
		resolveQueuedTarget(inst);
		autoLoadEarlier(inst);
		return keep.length;
	}

	/** One ?before= request at a time (spec 5.9); resolves true when a page was applied. */
	function loadEarlier(inst) {
		if (inst.loadingEarlier || !inst.hasEarlier || !inst.loadEarlierFn || inst.destroyed || inst.dead)
			return Promise.resolve(false);
		const btn = inst.earlierBtn;
		inst.loadingEarlier = true;
		if (btn) {
			btn.textContent = "Loading…";
			btn.disabled = true;
		}
		let pending;
		try {
			pending = Promise.resolve(inst.loadEarlierFn(inst.before));
		} catch (e) {
			pending = Promise.reject(e);
		}
		return pending.then(function (page) {
			inst.loadingEarlier = false;
			if (inst.destroyed || inst.dead)
				return false;
			const hadFocus = !!btn && document.activeElement === btn;
			if (btn) {
				btn.textContent = "Load earlier lines";
				btn.disabled = false;
			}
			const pg = isPlainObject(page) ? page : {};
			const added = prepend(inst, Array.isArray(pg.lines) ? pg.lines : [], { hasEarlier: pg.hasEarlier, before: pg.before });
			if (hadFocus && inst.queued === null) {
				if (inst.earlierBtn === btn)
					btn.focus({ preventScroll: true });
				else if (added) {
					const g = inst.rowsByN.get(inst.first).querySelector(".juneau-co-gutter");
					if (g)
						g.focus({ preventScroll: true });
				}
			}
			return true;
		}, function (err) {
			inst.loadingEarlier = false;
			if (inst.destroyed || inst.dead || isAbort(err))
				return false;
			if (err && err.status === 410) {
				inst.trimmed = true;
				inst.hasEarlier = false;
				inst.before = null;
				showNotice(inst);
			} else {
				if (btn) {
					btn.textContent = "Load earlier lines (retry)";
					btn.disabled = false;
				}
				warn(inst, "E-CO-4", [inst.id, describeFailure(err)], "E-CO-4:before");
			}
			// A queued target below first cannot arrive now; fall back (spec 5.6).
			if (inst.queued !== null && inst.queued < inst.first) {
				const n = inst.queued;
				inst.queued = null;
				notFound(inst, n);
			}
			return false;
		});
	}

	/** Keeps loading earlier pages while a queued target sits below first (spec 5.6); bounded by the DOM cap. */
	function autoLoadEarlier(inst) {
		if (inst.queued !== null && inst.queued < inst.first && inst.hasEarlier && !inst.loadingEarlier)
			loadEarlier(inst);
	}

	// @section:tooltip
	const TIP_MARGIN = 4;

	function tipBlock(inst, target) {
		const b = target && typeof target.closest === "function" ? target.closest("[data-juneau-co-tip]") : null;
		return b && inst.pane.contains(b) ? b : null;
	}

	function viewport() {
		const de = document.documentElement || {};
		return { w: window.innerWidth || de.clientWidth || 0, h: window.innerHeight || de.clientHeight || 0 };
	}

	/** Below the block, or above it when that would overflow; always TIP_MARGIN px inside the viewport. */
	function positionTip(inst, block) {
		const r = block.getBoundingClientRect();
		const t = inst.tipEl.getBoundingClientRect();
		const vp = viewport();
		let left = r.left;
		let top = r.bottom + TIP_MARGIN;
		if (vp.h && top + t.height > vp.h - TIP_MARGIN)
			top = r.top - t.height - TIP_MARGIN;
		if (vp.w)
			left = Math.min(left, vp.w - t.width - TIP_MARGIN);
		left = Math.max(TIP_MARGIN, left);
		top = Math.max(TIP_MARGIN, top);
		inst.tipEl.style.left = Math.round(left) + "px";
		inst.tipEl.style.top = Math.round(top) + "px";
	}

	function showTip(inst, block) {
		if (inst.tipFor && inst.tipFor !== block)
			inst.tipFor.removeAttribute("aria-describedby");
		inst.tipEl.textContent = block.getAttribute("data-juneau-co-tip");
		inst.tipEl.hidden = false;
		block.setAttribute("aria-describedby", inst.tipEl.id);
		inst.tipFor = block;
		positionTip(inst, block);
	}

	function hideTip(inst) {
		inst.tipEl.hidden = true;
		if (inst.tipFor) {
			inst.tipFor.removeAttribute("aria-describedby");
			inst.tipFor = null;
		}
	}

	function wireTooltip(inst) {
		const pane = inst.pane;
		const show = function (ev) {
			const b = tipBlock(inst, ev.target);
			if (b)
				showTip(inst, b);
		};
		listen(inst, pane, "mouseover", show);
		listen(inst, pane, "focusin", show);
		listen(inst, pane, "mouseout", function (ev) {
			const b = tipBlock(inst, ev.target);
			if (b && b === inst.tipFor && !(ev.relatedTarget && b.contains(ev.relatedTarget)))
				hideTip(inst);
		});
		listen(inst, pane, "focusout", function (ev) {
			if (tipBlock(inst, ev.target) === inst.tipFor)
				hideTip(inst);
		});
		listen(inst, pane, "keydown", function (ev) {
			if (ev.key === "Escape" && !inst.tipEl.hidden)
				hideTip(inst);
		});
	}

	// Markers (spec 5.8)
	const MARKER_MODES = ["dim", "hide", "show"];

	function markerKey(inst) {
		return "juneau-co:markers:" + inst.id;
	}

	function readStoredMarkers(inst) {
		try {
			const v = window.sessionStorage && window.sessionStorage.getItem(markerKey(inst));
			return MARKER_MODES.indexOf(v) >= 0 ? v : null;
		} catch (e) {
			return null;
		}
	}

	function applyMarkers(inst) {
		for (const m of MARKER_MODES)
			inst.root.classList.remove("juneau-co-markers-" + m);
		inst.root.classList.add("juneau-co-markers-" + inst.markers);
		inst.markersBtn.setAttribute("aria-pressed", inst.markers === "hide" ? "true" : "false");
	}

	function setMarkers(inst, mode) {
		if (inst.destroyed || MARKER_MODES.indexOf(mode) < 0)
			return false;
		inst.markers = mode;
		applyMarkers(inst);
		try {
			if (window.sessionStorage)
				window.sessionStorage.setItem(markerKey(inst), mode);
		} catch (e) {
			// Storage unavailable (private mode, quota): the choice just doesn't persist.
		}
		return true;
	}

	function markerSeen(inst) {
		if (!inst.sawMarker) {
			inst.sawMarker = true;
			inst.markersBtn.hidden = false;
		}
	}

	function wireMarkers(inst) {
		inst.markerOff = inst.markers === "hide" ? "dim" : inst.markers;
		const stored = readStoredMarkers(inst);
		if (stored)
			inst.markers = stored;
		applyMarkers(inst);
		listen(inst, inst.markersBtn, "click", function () {
			setMarkers(inst, inst.markers === "hide" ? inst.markerOff : "hide");
		});
	}

	// @section:core

	/** Live anchor prefixes on this page (spec 5.6): create adds, destroy removes, a duplicate is E-CO-9. */
	const PREFIXES = new Set();
	let tipSeq = 0;

	function sanitizeId(s) {
		return String(s).replace(/[^A-Za-z0-9_-]/g, "-").slice(0, 26);
	}

	function defaultPrefix(id, rowId) {
		return "co-" + sanitizeId(rowId === undefined || rowId === null ? id : id + "-" + rowId) + "-L";
	}

	function listen(inst, target, type, fn, opts) {
		target.addEventListener(type, fn, opts);
		inst.listeners.push([target, type, fn, opts]);
	}

	function create(el, options) {
		const o = isPlainObject(options) ? options : {};
		if (!el || typeof el.appendChild !== "function")
			throw configError(String(o.id || "console"), "create() needs a container element");
		const id = String(o.id || el.getAttribute("data-juneau-region") || el.id || "console");
		if (o.anchorPrefix !== undefined && !isAnchorPrefix(o.anchorPrefix))
			throw configError(id, "anchorPrefix must match ^[A-Za-z][A-Za-z0-9_-]{0,31}$: " + clip(o.anchorPrefix));
		if (o.downloadUrl !== undefined && !(isStr(o.downloadUrl) && NS.init.isSafeDetailUrl(o.downloadUrl)))
			throw configError(id, "downloadUrl must be a same-origin path: " + clip(o.downloadUrl));
		const compact = o.compact === true;
		const inst = {
			el: el, id: id, compact: compact,
			rows: Number.isInteger(o.rows) ? o.rows : (compact ? 8 : 20),
			showTime: o.showTime === true,
			title: isStr(o.title) ? o.title : "",
			classify: typeof o.classify === "function" ? o.classify : null,
			downloadUrl: isStr(o.downloadUrl) ? o.downloadUrl : null,
			maxDomRows: Number.isInteger(o.maxDomRows) && o.maxDomRows > 0 ? o.maxDomRows : 50000,
			emit: typeof o.emit === "function" ? o.emit : null,
			loadEarlierFn: typeof o.loadEarlier === "function" ? o.loadEarlier : null,
			markers: ["dim", "hide", "show"].indexOf(o.markers) >= 0 ? o.markers : "dim",
			first: 0, last: 0, count: 0, rowsByN: new Map(),
			stuck: true, newCount: 0, hasEarlier: false, before: null, trimmed: false,
			warned: new Set(), listeners: [], stopPoll: null, stopTicker: null,
			state: null, terminal: false, queued: null, dead: false, destroyed: false, banner: null, sawMarker: false,
			skew: 0, startedAt: null, lastElapsed: null, tickTimer: null,
			guardTop: null, holdUnstuck: false, target: null, backfilling: false,
			earlierEl: null, earlierBtn: null, loadingEarlier: false,
			tipFor: null, markerOff: null
		};
		inst.prefix = o.anchorPrefix !== undefined ? o.anchorPrefix : defaultPrefix(id, o.rowId);
		inst.anchors = !PREFIXES.has(inst.prefix);
		if (inst.anchors)
			PREFIXES.add(inst.prefix);
		else
			warn(inst, "E-CO-9", [id, inst.prefix]);

		const root = mk("div", compact ? "juneau-co juneau-co-compact" : "juneau-co");
		const header = mk("div", "juneau-co-header");
		if (!compact && inst.title) {
			const t = mk("span", "juneau-co-title");
			t.textContent = inst.title;
			header.appendChild(t);
		}
		const stateLabel = mk("span", "juneau-co-state-label");
		stateLabel.textContent = "State: ";
		inst.stateEl = mk("span", "juneau-co-state");
		stateLabel.appendChild(inst.stateEl);
		header.appendChild(stateLabel);
		inst.elapsedEl = mk("span", "juneau-co-elapsed");
		inst.elapsedEl.setAttribute("aria-hidden", "true");
		header.appendChild(inst.elapsedEl);
		inst.markersBtn = mk("button", "juneau-co-markers");
		inst.markersBtn.setAttribute("type", "button");
		inst.markersBtn.setAttribute("aria-pressed", "false");
		inst.markersBtn.textContent = "Hide markers";
		inst.markersBtn.hidden = true;
		let download = null;
		if (inst.downloadUrl) {
			download = mk("a", "juneau-co-download");
			download.setAttribute("href", inst.downloadUrl);
			download.setAttribute("download", "");
			download.textContent = "Download";
		}
		if (compact) {
			const more = mk("button", "juneau-co-more");
			more.setAttribute("type", "button");
			more.setAttribute("aria-haspopup", "true");
			more.setAttribute("aria-expanded", "false");
			more.setAttribute("aria-label", "More console actions");
			more.textContent = "⋯";
			const menu = mk("div", "juneau-co-menu");
			menu.hidden = true;
			menu.appendChild(inst.markersBtn);
			if (download)
				menu.appendChild(download);
			listen(inst, more, "click", function () {
				menu.hidden = !menu.hidden;
				more.setAttribute("aria-expanded", menu.hidden ? "false" : "true");
			});
			header.appendChild(more);
			header.appendChild(menu);
		} else {
			header.appendChild(inst.markersBtn);
			if (download)
				header.appendChild(download);
		}
		root.appendChild(header);

		inst.liveEl = mk("div", "juneau-co-status-live juneau-co-sr");
		inst.liveEl.setAttribute("role", "status");
		root.appendChild(inst.liveEl);
		inst.announceEl = mk("div", "juneau-co-announce juneau-co-sr");
		inst.announceEl.setAttribute("aria-live", "polite");
		root.appendChild(inst.announceEl);

		const pane = mk("div", "juneau-co-pane");
		pane.setAttribute("role", "log");
		pane.setAttribute("tabindex", "0");
		pane.setAttribute("aria-label", inst.title || "Console output");
		pane.setAttribute("aria-live", "polite");
		pane.style.setProperty("--juneau-co-rows", String(inst.rows));
		root.appendChild(pane);
		inst.pane = pane;

		inst.jumpBtn = mk("button", "juneau-co-jump");
		inst.jumpBtn.setAttribute("type", "button");
		inst.jumpBtn.hidden = true;
		root.appendChild(inst.jumpBtn);

		inst.tipEl = mk("div", "juneau-co-tooltip");
		inst.tipEl.setAttribute("role", "tooltip");
		inst.tipEl.id = "juneau-co-tip-" + (++tipSeq);
		inst.tipEl.hidden = true;
		root.appendChild(inst.tipEl);

		inst.root = root;
		el.appendChild(root);

		const api = Object.freeze({
			append: function (lines) { return append(inst, lines); },
			prepend: function (lines, meta) { return prepend(inst, lines, meta); },
			setStatus: function (s) { return setStatus(inst, s); },
			scrollToLine: function (n) { return scrollToLine(inst, n); },
			setMarkers: function (mode) { return setMarkers(inst, mode); },
			destroy: function () { destroy(inst); }
		});
		INTERNAL.set(api, inst);
		inst.api = api;
		wire(inst);
		return api;
	}

	/** Removes the oldest rows past maxDomRows (spec 5.1); the control then becomes the "not shown" notice. */
	function trimToCap(inst) {
		let removed = 0;
		while (inst.count > inst.maxDomRows) {
			const row = inst.rowsByN.get(inst.first);
			let next = row.nextSibling;
			while (next && !(next.classList && next.classList.contains("juneau-co-line")))
				next = next.nextSibling;
			row.remove();
			inst.rowsByN.delete(inst.first);
			inst.count--;
			removed++;
			inst.first = next ? Number(next.getAttribute("data-n")) : 0;
		}
		if (removed) {
			inst.trimmed = true;
			inst.hasEarlier = false;
			inst.before = null;
			onTrimmed(inst);
		}
	}

	/** Re-renders an open row in place: the node, its id and its rowsByN entry stay, so nothing is added to the log. */
	function patchRow(inst, old, line) {
		const row = renderLine(inst, line);
		const target = old.classList.contains("juneau-co-target");
		const revealed = old.classList.contains("juneau-co-reveal");
		old.className = row.className;
		if (target)
			old.classList.add("juneau-co-target");
		if (revealed)
			old.classList.add("juneau-co-reveal");
		for (const a of ["data-level", "data-marker", "aria-live"]) {
			if (row.hasAttribute(a))
				old.setAttribute(a, row.getAttribute(a));
			else
				old.removeAttribute(a);
		}
		old.replaceChildren();
		while (row.firstChild)
			old.appendChild(row.firstChild);
	}

	/** THE ingestion API (spec 5.1).  Returns the number of rows added; an open row re-sent under its n is patched in place and not counted. */
	function append(inst, lines) {
		if (inst.destroyed || inst.dead)
			return 0;
		const list = Array.isArray(lines) ? lines : [lines];
		const wasStuck = isPaneStuck(inst);
		const frag = document.createDocumentFragment();
		let added = 0;
		let dropped = 0;
		let replaced = 0;
		let firstBad;
		for (const raw of list) {
			const r = validateLine(raw);
			report(inst, r.problems);
			if (!r.line) {
				if (dropped++ === 0)
					firstBad = raw;
				continue;
			}
			const line = r.line;
			if (line.n === undefined)
				line.n = inst.last + 1;
			if (line.n <= inst.last) {
				const old = line.n === inst.last ? inst.rowsByN.get(line.n) : null;
				if (old && old.classList.contains("juneau-co-open")) {
					patchRow(inst, old, line);
					replaced++;
				}
				continue;
			}
			const row = renderLine(inst, line);
			frag.appendChild(row);
			inst.rowsByN.set(line.n, row);
			if (inst.first === 0)
				inst.first = line.n;
			inst.last = line.n;
			inst.count++;
			added++;
		}
		if (dropped)
			warn(inst, "E-CO-3", [inst.id, dropped, clip(safeJson(firstBad))]);
		if (added) {
			inst.pane.appendChild(frag);
			trimToCap(inst);
			afterAppend(inst, wasStuck, added);
			resolveQueuedTarget(inst);
		}
		else if (replaced)
			afterAppend(inst, wasStuck, 0);
		return added;
	}

	function destroy(inst) {
		if (inst.destroyed)
			return;
		inst.destroyed = true;
		hideTip(inst);
		if (inst.stopPoll)
			inst.stopPoll();
		if (inst.stopTicker)
			inst.stopTicker();
		for (const [target, type, fn, opts] of inst.listeners)
			target.removeEventListener(type, fn, opts);
		inst.listeners.length = 0;
		if (inst.anchors)
			PREFIXES.delete(inst.prefix);
		inst.tipEl.hidden = true;
		inst.tipEl.remove();
	}

	// @section:poll
	const MIN_REFRESH = 1000;
	const DEF_REFRESH = 2000;
	const MAX_BACKOFF = 60000;
	const DEF_TAIL = 5000;
	const DEF_EARLIER = 2000;
	const MAX_LINES = 10000;
	const NOT_A_PAGE = "(not a console-output page)";

	function withQuery(url, q) {
		return q ? url + (url.indexOf("?") >= 0 ? "&" : "?") + q : url;
	}

	function clampInt(v, lo, hi, dflt) {
		return Number.isSafeInteger(v) ? Math.min(hi, Math.max(lo, v)) : dflt;
	}

	function isResolvedUrl(u) {
		return isStr(u) && isSafePath(u) && !/[{}]/.test(u);
	}

	/** Retry-After in ms (delta-seconds or an HTTP-date); null when absent or unparseable. */
	function retryAfterMs(res) {
		const h = res.headers && typeof res.headers.get === "function" ? res.headers.get("Retry-After") : null;
		if (!isStr(h) || h.trim() === "")
			return null;
		const s = h.trim();
		if (/^\d{1,10}$/.test(s))
			return Number(s) * 1000;
		const at = Date.parse(s);
		return Number.isNaN(at) ? null : Math.max(0, at - Date.now());
	}

	/**
	 * One GET.  Resolves to the parsed body (a plain object).  Rejects with an Error carrying `status` and
	 * `retryAfter` for an HTTP error, one carrying `badBody` for a body that is not a JSON object, or the fetch's own
	 * error (network failure, AbortError).
	 */
	function getPage(url, signal) {
		return fetch(url, { signal: signal, credentials: "same-origin", headers: { Accept: "application/json" } })
			.then(function (res) {
				if (!res.ok) {
					const err = new Error("HTTP " + res.status);
					err.status = res.status;
					err.retryAfter = retryAfterMs(res);
					throw err;
				}
				return res.text();
			})
			.then(function (text) {
				let body;
				try {
					body = JSON.parse(text);
				} catch (e) { // NOSONAR javascript:S2486 -- an unparseable body is reported as E-CO-2 below
					body = undefined;
				}
				if (!isPlainObject(body)) {
					const err = new Error("response is not a JSON object");
					err.badBody = true;
					throw err;
				}
				return body;
			});
	}

	/** The fatal [code, args] for a forward or tail page that breaks the contract (spec 3.2, 5.2), or null. */
	function forwardProblem(inst, page) {
		if (page.contractVersion !== CONTRACT_VERSION)
			return ["E-CO-2", [inst.id, String(page.contractVersion)]];
		if (!Array.isArray(page.lines))
			return ["E-CO-2", [inst.id, NOT_A_PAGE]];
		if (!isToken(page.next))
			return ["E-CO-5", [inst.id]];
		let prev = -Infinity;
		for (const l of page.lines) {
			if (isPlainObject(l) && Number.isSafeInteger(l.n)) {
				if (l.n <= prev)
					return ["E-CO-5", [inst.id]];
				prev = l.n;
			}
		}
		return null;
	}

	/** The ?before= loader poll() installs when create() got no loadEarlier (spec 5.9). */
	function fetchLoader(inst, linesUrl, earlierLimit, signal) {
		return function (before) {
			const url = withQuery(linesUrl, "before=" + encodeURIComponent(before) + "&limit=" + earlierLimit);
			return getPage(url, signal).then(function (page) {
				if (page.contractVersion !== CONTRACT_VERSION) {
					fatal(inst, "E-CO-2", [inst.id, String(page.contractVersion)]);
					return { lines: [] };
				}
				return page;
			}, function (err) {
				if (err && err.badBody) {
					fatal(inst, "E-CO-2", [inst.id, NOT_A_PAGE]);
					return { lines: [] };
				}
				throw err;
			});
		};
	}

	/**
	 * The polling loop (spec 5.2): a setTimeout chain with one request in flight.  Resolves at terminal, abort,
	 * destroy or a fatal code; never rejects.
	 */
	function poll(api, opts) {
		const inst = INTERNAL.get(api);
		const o = isPlainObject(opts) ? opts : {};
		if (!inst) {
			configError("console", "poll() needs an instance returned by create()");
			return Promise.resolve();
		}
		if (inst.destroyed || inst.dead || inst.stopPoll)
			return Promise.resolve();
		if (!isResolvedUrl(o.linesUrl)) {
			configError(inst.id, "poll() needs a resolved same-origin linesUrl: " + clip(safeJson(o.linesUrl)));
			return Promise.resolve();
		}
		const linesUrl = o.linesUrl;
		const refreshMs = Number.isFinite(o.refreshMs) && o.refreshMs > 0 ? Math.max(MIN_REFRESH, o.refreshMs) : DEF_REFRESH;
		const tail = o.tail === 0 ? 0 : clampInt(o.tail, 1, MAX_LINES, DEF_TAIL);
		const earlierLimit = clampInt(o.earlierLimit, 1, MAX_LINES, DEF_EARLIER);
		const ext = o.signal && typeof o.signal.addEventListener === "function" ? o.signal : null;
		const ctl = new AbortController();
		let timer = null;
		let onVisible = null;
		let onExtAbort = null;
		let finished = false;
		let next = null;
		let failures = 0;
		let resolveDone;
		const done = new Promise(function (r) { resolveDone = r; });

		function finish() {
			if (finished)
				return;
			finished = true;
			if (timer !== null) {
				clearTimeout(timer);
				timer = null;
			}
			if (onVisible) {
				document.removeEventListener("visibilitychange", onVisible);
				onVisible = null;
			}
			if (onExtAbort)
				ext.removeEventListener("abort", onExtAbort);
			if (inst.stopPoll === finish)
				inst.stopPoll = null;
			ctl.abort();
			resolveDone();
		}

		function url() {
			if (next !== null)
				return withQuery(linesUrl, "after=" + encodeURIComponent(next));
			return tail > 0 ? withQuery(linesUrl, "tail=" + tail) : linesUrl;
		}

		function run() {
			if (!finished)
				getPage(url(), ctl.signal).then(onPage, onError);
		}

		/** Hidden tab (spec 5.2): a due poll waits for visibilitychange, then runs at once. */
		function schedule(ms) {
			timer = setTimeout(function () {
				timer = null;
				if (finished)
					return;
				if (!document.hidden) {
					run();
					return;
				}
				onVisible = function () {
					if (document.hidden)
						return;
					document.removeEventListener("visibilitychange", onVisible);
					onVisible = null;
					run();
				};
				document.addEventListener("visibilitychange", onVisible);
			}, ms);
		}

		function onPage(page) {
			if (finished || inst.destroyed || inst.dead) {
				finish();
				return;
			}
			const bad = forwardProblem(inst, page);
			if (bad) {
				fatal(inst, bad[0], bad[1]);
				finish();
				return;
			}
			if (failures > 0) {
				failures = 0;
				inst.warned.delete("E-CO-4:outage");
			}
			const first = next === null;
			next = page.next;
			let more = page.more === true && page.terminal !== true;
			if (more && page.lines.length === 0) {
				warn(inst, "E-CO-5", [inst.id], "E-CO-5:empty-more");
				more = false;
			}
			inst.backfilling = more;
			if (first)
				prepend(inst, [], { hasEarlier: page.hasEarlier === true, before: page.before });
			if (!inst.dead)
				append(inst, page.lines);
			if (first && !inst.dead)
				autoLoadEarlier(inst);
			setStatus(inst, page);
			if (!inst.dead)
				updateLive(inst);
			if (inst.dead || inst.destroyed || inst.terminal) {
				finish();
				return;
			}
			schedule(more ? 0 : refreshMs);
		}

		function onError(err) {
			if (finished || inst.destroyed || inst.dead || isAbort(err)) {
				finish();
				return;
			}
			if (err && err.badBody) {
				fatal(inst, "E-CO-2", [inst.id, NOT_A_PAGE]);
				finish();
				return;
			}
			const status = err && Number.isInteger(err.status) ? err.status : null;
			if (status === 410) {
				fatal(inst, "E-CO-4", [inst.id, GONE_TEXT]);
				finish();
				return;
			}
			if (status !== null && status >= 400 && status < 500 && status !== 408 && status !== 429) {
				fatal(inst, "E-CO-4", [inst.id, describeFailure(err)]);
				finish();
				return;
			}
			failures++;
			warn(inst, "E-CO-4", [inst.id, describeFailure(err)], "E-CO-4:outage");
			inst.stateEl.textContent = "Reconnecting…";
			let delay = Math.min(MAX_BACKOFF, refreshMs * Math.pow(2, failures));
			if (status === 429 && err.retryAfter !== null)
				delay = Math.max(refreshMs, err.retryAfter);
			schedule(delay);
		}

		inst.stopPoll = finish;
		if (!inst.loadEarlierFn)
			inst.loadEarlierFn = fetchLoader(inst, linesUrl, earlierLimit, ext || undefined);
		if (ext) {
			if (ext.aborted) {
				finish();
				return done;
			}
			onExtAbort = function () { finish(); };
			ext.addEventListener("abort", onExtAbort, { once: true });
		}
		run();
		return done;
	}

	// @section:mount

	/** Spec 7.4: {id} from rowId, then the same-origin check, then no leftover braces.  Throws E-CO-1. */
	function resolveUrl(id, name, url, rowId) {
		let u = url;
		if (u.indexOf("{id}") >= 0) {
			if (rowId === undefined || rowId === null || rowId === "")
				throw configError(id, name + " contains {id} but this console has no row id: " + clip(url));
			u = NS.init.substituteDetailUrl(u, rowId);
			if (u === null)
				throw configError(id, name + " is not a same-origin path after {id} substitution: " + clip(url));
		}
		if (!NS.init.isSafeDetailUrl(u))
			throw configError(id, name + " must be a same-origin path: " + clip(url));
		if (/[{}]/.test(u))
			throw configError(id, name + " has an unsubstituted placeholder: " + clip(url));
		return u;
	}

	function checkInt(id, o, name, lo, hi) {
		const v = o[name];
		if (v !== undefined && !(Number.isSafeInteger(v) && v >= lo && v <= hi))
			throw configError(id, name + " must be an integer from " + lo + " to " + hi + ": " + clip(safeJson(v)));
	}

	/**
	 * create() + poll() (spec 4, 4.1).  Synchronous: returns the cleanup, never a promise.  Every configuration error
	 * throws E-CO-1 before any DOM exists.  ctx = {signal, rowId, id, emit}; id (the region id, used for bus messages) and emit (publishes them) are optional, and the card path passes {}.
	 */
	function mount(el, options, ctx) {
		const o = isPlainObject(options) ? options : {};
		const c = isPlainObject(ctx) ? ctx : {};
		const elId = el && typeof el.getAttribute === "function" ? (el.getAttribute("data-juneau-region") || el.id) : null;
		const id = String(o.id || c.id || elId || "console");
		if (!isStr(o.linesUrl) || o.linesUrl.trim() === "")
			throw configError(id, "linesUrl is required");
		const linesUrl = resolveUrl(id, "linesUrl", o.linesUrl, c.rowId);
		let downloadUrl;
		if (o.downloadUrl !== undefined) {
			if (!isStr(o.downloadUrl))
				throw configError(id, "downloadUrl must be a string: " + clip(safeJson(o.downloadUrl)));
			downloadUrl = resolveUrl(id, "downloadUrl", o.downloadUrl, c.rowId);
		}
		checkInt(id, o, "tail", 0, MAX_LINES);
		checkInt(id, o, "earlierLimit", 1, MAX_LINES);
		checkInt(id, o, "rows", 3, 200);
		if (o.refreshMs !== undefined && !(Number.isFinite(o.refreshMs) && o.refreshMs > 0))
			throw configError(id, "refreshMs must be a positive number: " + clip(safeJson(o.refreshMs)));
		const api = create(el, Object.assign({}, o, {
			id: id,
			rowId: c.rowId,
			downloadUrl: downloadUrl,
			emit: typeof c.emit === "function" ? c.emit : undefined
		}));
		if (o.poll !== false)
			poll(api, { linesUrl: linesUrl, refreshMs: o.refreshMs, tail: o.tail, earlierLimit: o.earlierLimit, signal: c.signal });
		return function () { api.destroy(); };
	}

	NS.consoleOutput = Object.freeze({
		CONTRACT_VERSION: CONTRACT_VERSION,
		create: create,
		poll: poll,
		mount: mount,
		validateLine: validateLine,
		isSafeColor: isSafeColor,
		isSafeLineHref: isSafeLineHref,
		isSafeLineImageSrc: isSafeLineImageSrc
	});

	// Registered at load time, so this file loads after juneau-regions.js (ViewsMixin#CONSOLE_OUTPUT_JS_PATH).
	if (NS.regions && typeof NS.regions.register === "function") {
		NS.regions.register("console-output", function (ctx, el) {
			return mount(el, ctx.params || {}, {
				signal: ctx.signal,
				rowId: ctx.ids ? ctx.ids.rowId : undefined,
				id: ctx.id,
				emit: function (msg) { ctx.emit(msg); }
			});
		});
	} else {
		console.error(LOG_TAG, "juneau-console-output.js must load after juneau-regions.js; the console-output populator is not registered");
	}
})();
