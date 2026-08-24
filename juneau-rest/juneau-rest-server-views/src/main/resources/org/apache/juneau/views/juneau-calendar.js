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
 * juneau-calendar.js - client runtime for the Apache Juneau reusable calendar widget (parent concept #10).
 *
 * On DOMContentLoaded it owns init for every [data-juneau-calendar] element: it reads the instance id, contract,
 * server-stamped civil today, endpoint template, view, weekStart, and maxPerDay, performs a FAIL-LOUD
 * contract-version handshake (a data-juneau-calendar-contract that !== the baked string "1" -> visible error, no
 * fetch/paint), then computes each month's grid client-side, hydrates events from a same-origin data-only per-month
 * GET (or the embedded seed sidecar for the initial month), and wires prev/next/today navigation.
 *
 * Everything in the "PURE LOGIC LAYER" is DOM-free (plain data in, plain data out) and unit-tested from Node; the
 * "DOM BINDING LAYER" is the thin shim that scans, clones <template>s, fills with textContent (never innerHTML), and
 * binds events.  Civil dates are bucketed field-wise (never Date.parse of a date-only string) so an all-day chip
 * cannot drift a day across timezones, and "today" is the server-stamped civil date, never the browser clock.
 */
(function () {
	"use strict";

	// Contract-version handshake: MUST equal CalendarDef.CONTRACT_VERSION on the server.  The JSON STRING "1"
	// (a numeric 1 would fail this strict ===).  A live 200 body whose contractVersion differs is refused.
	const JUNEAU_CALENDAR_CONTRACT_VERSION = "1";

	const MIN_POLL_INTERVAL_MS = 5000; // mirrors SafePathTemplate.MIN_POLL_INTERVAL_MS (not polled in v1; reserved).

	// DOM attribute names - MUST equal CalendarTable's constants of the same names on the server.
	const MARKER_ATTR = "data-juneau-calendar";
	const CONTRACT_ATTR = "data-juneau-calendar-contract";
	const TODAY_ATTR = "data-juneau-calendar-today";
	const ENDPOINT_ATTR = "data-juneau-calendar-endpoint";
	const VIEW_ATTR = "data-juneau-calendar-view";
	const WEEKSTART_ATTR = "data-juneau-calendar-weekstart";
	const MAXPERDAY_ATTR = "data-juneau-calendar-maxperday";
	const GRID_ATTR = "data-juneau-calendar-grid";
	const TITLE_ATTR = "data-juneau-calendar-title";
	const CAT_ATTR = "data-juneau-calendar-cat";
	const DAY_TEMPLATE_ATTR = "data-juneau-calendar-day";
	const EVENT_TEMPLATE_ATTR = "data-juneau-calendar-event";
	const SEED_ATTR = "data-juneau-calendar-seed";
	const INIT_PENDING_ATTR = "data-juneau-init-pending";

	const NEUTRAL = "neutral";
	const MONTH_NAMES = ["January", "February", "March", "April", "May", "June", "July", "August", "September",
		"October", "November", "December"];

	const NS = window.JuneauCalendar = window.JuneauCalendar || {};
	NS.CONTRACT_VERSION = JUNEAU_CALENDAR_CONTRACT_VERSION;
	NS.MIN_POLL_INTERVAL_MS = MIN_POLL_INTERVAL_MS;

	// ==============================================================================================================
	// PURE LOGIC LAYER  (no DOM)
	// ==============================================================================================================

	/** Two-digit zero-padded string. */
	function pad2(n) {
		return (n < 10 ? "0" : "") + n;
	}

	/** Leap-aware day count for a 1-based month. */
	function daysInMonth(y, m) {
		if (m === 2)
			return ((y % 4 === 0 && y % 100 !== 0) || y % 400 === 0) ? 29 : 28;
		return (m === 4 || m === 6 || m === 9 || m === 11) ? 30 : 31;
	}

	/** Day-of-week for a civil date (Sakamoto's algorithm), 0=Sunday..6=Saturday.  No Date, no timezone. */
	function dayOfWeek(y, m, d) {
		const t = [0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4];
		let yy = y;
		if (m < 3)
			yy -= 1;
		return (((yy + Math.floor(yy / 4) - Math.floor(yy / 100) + Math.floor(yy / 400) + t[m - 1] + d) % 7) + 7) % 7;
	}

	/** Number of leading adjacent-month cells before the 1st, given weekStart "sunday" | "monday". */
	function firstWeekdayOffset(y, m, weekStart) {
		const dow = dayOfWeek(y, m, 1);
		return weekStart === "monday" ? (dow + 6) % 7 : dow;
	}

	/** Proleptic-Gregorian civil (y,m,d) -> days since 1970-01-01. */
	function toEpochDay(y, m, d) {
		let yy = m <= 2 ? y - 1 : y;
		const era = Math.floor((yy >= 0 ? yy : yy - 399) / 400);
		const yoe = yy - era * 400;
		const doy = Math.floor((153 * (m > 2 ? m - 3 : m + 9) + 2) / 5) + d - 1;
		const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
		return era * 146097 + doe - 719468;
	}

	/** Days since 1970-01-01 -> civil {y, m, d}. */
	function fromEpochDay(z) {
		z += 719468;
		const era = Math.floor((z >= 0 ? z : z - 146096) / 146097);
		const doe = z - era * 146097;
		const yoe = Math.floor((doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365);
		const y = yoe + era * 400;
		const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
		const mp = Math.floor((5 * doy + 2) / 153);
		const d = doy - Math.floor((153 * mp + 2) / 5) + 1;
		const m = mp < 10 ? mp + 3 : mp - 9;
		return { y: m <= 2 ? y + 1 : y, m: m, d: d };
	}

	/** The civil date key "yyyy-MM-dd" for a (y,m,d). */
	function dateKey(y, m, d) {
		return y + "-" + pad2(m) + "-" + pad2(d);
	}

	/**
	 * The always-6-week (42-cell) grid for a month.  Each cell is {key, y, m, d, inMonth}; adjacent-month cells are
	 * flagged inMonth=false and (per design doc §5.8) never carry events.
	 */
	function buildMonthCells(y, m, weekStart) {
		const start = toEpochDay(y, m, 1) - firstWeekdayOffset(y, m, weekStart);
		const cells = [];
		for (let i = 0; i < 42; i++) {
			const c = fromEpochDay(start + i);
			cells.push({ key: dateKey(c.y, c.m, c.d), y: c.y, m: c.m, d: c.d, inMonth: c.y === y && c.m === m });
		}
		return cells;
	}

	/**
	 * The civil date portion of an ISO date/date-time - the leading "yyyy-MM-dd", parsed field-wise.  Never
	 * Date.parse (which would timezone-shift a date-only string).  Returns null when not a valid civil date.
	 */
	function civilKey(s) {
		if (typeof s !== "string" || s.length < 10)
			return null;
		const head = s.slice(0, 10);
		if (!/^\d{4}-\d{2}-\d{2}$/.test(head))
			return null;
		if (s.length > 10 && s.charAt(10) !== "T")
			return null;
		return head;
	}

	/** Strict contract handshake: the value must be exactly the string "1" (a numeric 1 fails). */
	function contractOk(v) {
		return v === JUNEAU_CALENDAR_CONTRACT_VERSION;
	}

	/** The envelope's year/month must echo the requested (numeric) window, else the body is dropped. */
	function echoOk(envelope, y, m) {
		return !!envelope && envelope.year === y && envelope.month === m;
	}

	/**
	 * Fail-soft wire-data cleanse: drop events missing id/title/start (console.warn), keep first of any intra-payload
	 * duplicate id (console.warn later dupes).  The bean's OWN seed events fail loud in validate(); this guards
	 * fetched data only.
	 */
	function sanitizeEvents(events) {
		const out = [];
		const seen = Object.create(null);
		if (!Array.isArray(events))
			return out;
		for (const e of events) {
			if (!e || !e.id || !e.title || !e.start) {
				console.warn("juneau-calendar: dropping event missing id/title/start", e);
				continue;
			}
			if (seen[e.id]) {
				console.warn("juneau-calendar: dropping duplicate event id", e.id);
				continue;
			}
			seen[e.id] = true;
			out.push(e);
		}
		return out;
	}

	/** The color token for a category id against the declared map; unknown non-blank ids warn + fall back to neutral. */
	function colorToken(categoryId, categoryMap) {
		if (!categoryId)
			return NEUTRAL;
		if (categoryMap && Object.hasOwn(categoryMap, categoryId))
			return categoryMap[categoryId];
		console.warn("juneau-calendar: unknown categoryId", categoryId);
		return NEUTRAL;
	}

	/**
	 * Same-origin DOCUMENT-URL safety (isSafeDetailUrl semantics): no scheme, no protocol-relative "//", no scheme
	 * colon-before-slash, no ".." segments.  Query/hash are allowed.
	 */
	function isSafeDocumentUrl(url) {
		if (typeof url !== "string" || url.length === 0)
			return false;
		if (url.indexOf("://") >= 0)
			return false;
		if (url.slice(0, 2) === "//")
			return false;
		const colon = url.indexOf(":");
		const slash = url.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash))
			return false;
		const path = url.split(/[?#]/, 1)[0];
		for (const seg of path.split("/"))
			if (seg === "..")
				return false;
		return true;
	}

	/** Substitutes {year}/{month} (1-based, unpadded integers) into a same-origin path template. */
	function substituteEndpoint(template, y, m) {
		return template.replace(/\{year\}/g, String(y)).replace(/\{month\}/g, String(m));
	}

	/** Events whose civil start is exactly the given in-month date key, sorted by start ascending. */
	function eventsForDay(events, key) {
		return events
			.filter(function (e) { return civilKey(e.start) === key; })
			.sort(function (a, b) { return a.start < b.start ? -1 : (a.start > b.start ? 1 : 0); });
	}

	/** Splits a day's events into the shown chips (<= maxPerDay) and the overflow count (design doc §5.11). */
	function applyCap(events, maxPerDay) {
		const cap = maxPerDay > 0 ? maxPerDay : events.length;
		return { shown: events.slice(0, cap), overflow: Math.max(0, events.length - cap) };
	}

	/** The coalesce key that guards a stale month/generation fetch from painting. */
	function coalesceKey(id, y, m, generation) {
		return id + ":" + y + "-" + m + ":" + generation;
	}

	// ==============================================================================================================
	// DOM BINDING LAYER
	// ==============================================================================================================

	/** Reads the server-rendered legend into a {categoryId -> colorToken} map from each item's jc-cal-cat--* class. */
	function readCategoryMap(root) {
		const map = Object.create(null);
		const items = root.querySelectorAll("[" + CAT_ATTR + "]");
		for (let i = 0; i < items.length; i++) {
			const el = items[i];
			if (el.getAttribute("role") === "columnheader")
				continue;
			const id = el.getAttribute(CAT_ATTR);
			const token = tokenFromClass(el);
			if (id && token)
				map[id] = token;
		}
		return map;
	}

	/** Extracts the color token from an element's jc-cal-cat--{token} class, or null. */
	function tokenFromClass(el) {
		const cls = el.getAttribute("class") || "";
		const m = /\bjc-cal-cat--([a-z]+)\b/.exec(cls);
		return m ? m[1] : null;
	}

	/** Builds one event node (an <a> for a safe same-origin href, else a span), filled with textContent only. */
	function fillEventNode(doc, event, categoryMap) {
		const linked = event.href && isSafeDocumentUrl(event.href);
		const node = doc.createElement(linked ? "a" : "span");
		node.setAttribute("class", "jc-cal-event jc-cal-cat--" + colorToken(event.categoryId, categoryMap));
		if (linked)
			node.setAttribute("href", event.href);
		node.textContent = event.title; // textContent, NEVER innerHTML - a title of "<img onerror>" stays literal text.
		if (event.categoryId)
			node.setAttribute(CAT_ATTR, event.categoryId);
		if (event.tooltip)
			node.setAttribute("title", event.tooltip);
		return node;
	}

	/** ~20-line dismiss-on-Escape/outside-click helper, COPIED here (design doc §5.16 - no juneau-views.js import). */
	function bindDismiss(popover, onDismiss) {
		function onKey(e) {
			if (e.key === "Escape") {
				cleanup();
				onDismiss();
			}
		}
		function onClick(e) {
			if (!popover.contains(e.target)) {
				cleanup();
				onDismiss();
			}
		}
		function cleanup() {
			document.removeEventListener("keydown", onKey, true);
			document.removeEventListener("click", onClick, true);
		}
		document.addEventListener("keydown", onKey, true);
		document.addEventListener("click", onClick, true);
		return cleanup;
	}

	/** Renders a visible inline error and paints an empty month (single attempt - no retry storm; design doc §5.15). */
	function showError(state, message) {
		const grid = state.grid;
		let err = state.root.querySelector(".jc-cal-error");
		if (!err) {
			err = document.createElement("div");
			err.setAttribute("class", "jc-cal-error");
			err.setAttribute("role", "alert");
			state.root.insertBefore(err, grid);
		}
		err.textContent = message;
	}

	/** Clears a prior inline error, if any. */
	function clearError(state) {
		const err = state.root.querySelector(".jc-cal-error");
		if (err && err.parentNode)
			err.parentNode.removeChild(err);
	}

	/** Repaints the grid for state.year/state.month using the given (already-sanitized) event list. */
	function paintMonth(state, events) {
		const cells = buildMonthCells(state.year, state.month, state.weekStart);
		const grid = state.grid;
		// Remove prior week rows (keep the weekday header row, the first [role=row]).
		const weeks = grid.querySelectorAll(".jc-cal-week");
		for (let i = 0; i < weeks.length; i++)
			weeks[i].parentNode.removeChild(weeks[i]);

		let row = null;
		for (let i = 0; i < cells.length; i++) {
			if (i % 7 === 0) {
				row = document.createElement("div");
				row.setAttribute("role", "row");
				row.setAttribute("class", "jc-cal-week");
				grid.appendChild(row);
			}
			row.appendChild(buildCell(state, cells[i], events));
		}
		const title = state.root.querySelector("[" + TITLE_ATTR + "]");
		if (title)
			title.textContent = MONTH_NAMES[state.month - 1] + " " + state.year;
	}

	/** Builds one day cell by cloning the day template, painting the day number and (in-month only) event chips. */
	function buildCell(state, cell, events) {
		const node = state.dayTemplate.content.firstElementChild.cloneNode(true);
		let cls = "jc-cal-day";
		if (!cell.inMonth)
			cls += " jc-cal-day--adjacent";
		if (cell.key === state.today) {
			cls += " jc-cal-day--today";
			node.setAttribute("aria-current", "date");
		}
		node.setAttribute("class", cls);
		const num = node.querySelector(".jc-cal-day-num");
		if (num)
			num.textContent = String(cell.d);
		const box = node.querySelector(".jc-cal-day-events");
		if (box && cell.inMonth) {
			const dayEvents = eventsForDay(events, cell.key);
			const capped = applyCap(dayEvents, state.maxPerDay);
			for (let i = 0; i < capped.shown.length; i++)
				box.appendChild(fillEventNode(document, capped.shown[i], state.categoryMap));
			if (capped.overflow > 0)
				box.appendChild(buildMoreButton(state, cell.key, dayEvents, capped.overflow));
		}
		return node;
	}

	/** The "+N more" button that opens a dismiss-bound day popover listing all of that day's events. */
	function buildMoreButton(state, key, dayEvents, overflow) {
		const btn = document.createElement("button");
		btn.setAttribute("type", "button");
		btn.setAttribute("class", "jc-cal-more");
		btn.setAttribute("aria-haspopup", "dialog");
		btn.textContent = "+" + overflow + " more";
		btn.addEventListener("click", function () {
			openDayPopover(state, btn, key, dayEvents);
		});
		return btn;
	}

	/** Opens (or replaces) the day popover next to the trigger, listing every event for that day. */
	function openDayPopover(state, trigger, key, dayEvents) {
		closePopover(state);
		const pop = document.createElement("div");
		pop.setAttribute("class", "jc-cal-popover");
		pop.setAttribute("role", "dialog");
		for (let i = 0; i < dayEvents.length; i++)
			pop.appendChild(fillEventNode(document, dayEvents[i], state.categoryMap));
		state.root.appendChild(pop);
		trigger.setAttribute("aria-expanded", "true");
		state.popover = pop;
		state.popoverDismiss = bindDismiss(pop, function () { closePopover(state, trigger); });
	}

	/** Closes any open day popover. */
	function closePopover(state, trigger) {
		if (state.popoverDismiss) {
			state.popoverDismiss();
			state.popoverDismiss = null;
		}
		if (state.popover && state.popover.parentNode) {
			state.popover.parentNode.removeChild(state.popover);
			state.popover = null;
		}
		if (trigger)
			trigger.setAttribute("aria-expanded", "false");
	}

	/** Loads a month: uses the seed sidecar for the initial month, else fetches the same-origin per-month GET. */
	function loadMonth(state, useSeed) {
		clearError(state);
		const generation = ++state.generation;
		if (useSeed && state.seed && echoOk(state.seed, state.year, state.month)) {
			paintMonth(state, sanitizeEvents(state.seed.events));
			return;
		}
		if (!state.endpoint) {
			paintMonth(state, []); // seed-only calendar, no endpoint: nothing to fetch.
			return;
		}
		const y = state.year;
		const m = state.month;
		const url = substituteEndpoint(state.endpoint, y, m);
		if (state.abort)
			state.abort.abort();
		const controller = typeof AbortController !== "undefined" ? new AbortController() : null;
		state.abort = controller;
		fetch(url, {
			method: "GET",
			credentials: "same-origin",
			headers: { Accept: "application/json" },
			signal: controller ? controller.signal : undefined
		}).then(function (resp) {
			if (!resp.ok)
				throw new Error("HTTP " + resp.status);
			const ct = resp.headers && resp.headers.get ? resp.headers.get("Content-Type") : null;
			if (ct && ct.indexOf("application/json") < 0)
				throw new Error("non-JSON response");
			return resp.json();
		}).then(function (envelope) {
			if (generation !== state.generation)
				return; // a newer navigation superseded this fetch - drop.
			if (!contractOk(envelope && envelope.contractVersion)) {
				showError(state, "Calendar data version mismatch.");
				return;
			}
			if (!echoOk(envelope, y, m))
				return; // echo-check: the body is for a different month - drop.
			paintMonth(state, sanitizeEvents(envelope.events));
		}).catch(function (e) {
			if (e && e.name === "AbortError")
				return;
			if (generation === state.generation)
				showError(state, "Unable to load calendar events.");
		});
	}

	/** Navigates by a whole-month delta (or to today when delta is null), then reloads. */
	function navigate(state, delta) {
		closePopover(state);
		if (delta === null) {
			const t = civilKey(state.today);
			if (t) {
				state.year = parseInt(t.slice(0, 4), 10);
				state.month = parseInt(t.slice(5, 7), 10);
			}
		} else {
			let m = state.month + delta;
			let y = state.year;
			while (m < 1) { m += 12; y -= 1; }
			while (m > 12) { m -= 12; y += 1; }
			state.year = y;
			state.month = m;
		}
		loadMonth(state, false);
	}

	/** Initializes one [data-juneau-calendar] instance: contract handshake, first paint, nav wiring. */
	function initInstance(root) {
		if (root.getAttribute(INIT_PENDING_ATTR) === "1" || root.__juneauCalendarInit)
			return;
		root.setAttribute(INIT_PENDING_ATTR, "1");
		try {
			if (!contractOk(root.getAttribute(CONTRACT_ATTR))) {
				root.textContent = "";
				const err = document.createElement("div");
				err.setAttribute("class", "jc-cal-error");
				err.setAttribute("role", "alert");
				err.textContent = "Calendar runtime/contract version mismatch.";
				root.appendChild(err);
				console.error("juneau-calendar: contract mismatch; refusing to init", root.getAttribute(MARKER_ATTR));
				return;
			}
			const today = root.getAttribute(TODAY_ATTR);
			const state = {
				root: root,
				id: root.getAttribute(MARKER_ATTR),
				today: today,
				endpoint: root.getAttribute(ENDPOINT_ATTR),
				view: root.getAttribute(VIEW_ATTR) || "month",
				weekStart: root.getAttribute(WEEKSTART_ATTR) === "monday" ? "monday" : "sunday",
				maxPerDay: parseInt(root.getAttribute(MAXPERDAY_ATTR), 10) || 3,
				grid: root.querySelector("[" + GRID_ATTR + "]"),
				dayTemplate: root.querySelector("template[" + DAY_TEMPLATE_ATTR + "]"),
				eventTemplate: root.querySelector("template[" + EVENT_TEMPLATE_ATTR + "]"),
				categoryMap: readCategoryMap(root),
				generation: 0,
				abort: null,
				popover: null,
				popoverDismiss: null,
				seed: readSeed(root)
			};
			const tk = civilKey(today);
			state.year = tk ? parseInt(tk.slice(0, 4), 10) : fromEpochDay(0).y;
			state.month = tk ? parseInt(tk.slice(5, 7), 10) : 1;
			// Prefer the server-painted seed month's year/month if a seed is present.
			if (state.seed && typeof state.seed.year === "number" && typeof state.seed.month === "number") {
				state.year = state.seed.year;
				state.month = state.seed.month;
			}
			wireNav(root, state);
			loadMonth(state, true);
			root.__juneauCalendarInit = true;
		} finally {
			root.removeAttribute(INIT_PENDING_ATTR);
		}
	}

	/** Reads and JSON-parses the optional seed sidecar for the initial month, or null. */
	function readSeed(root) {
		const s = root.querySelector("script[" + SEED_ATTR + "]");
		if (!s)
			return null;
		try {
			return JSON.parse(s.textContent);
		} catch (e) {
			console.warn("juneau-calendar: unparseable seed sidecar");
			return null;
		}
	}

	/** Wires the prev/next/today nav buttons (disabled/absent nav is simply not bound). */
	function wireNav(root, state) {
		const prev = root.querySelector("[data-juneau-calendar-prev]");
		const next = root.querySelector("[data-juneau-calendar-next]");
		const todayBtn = root.querySelector("[data-juneau-calendar-today-btn]");
		if (prev)
			prev.addEventListener("click", function () { navigate(state, -1); });
		if (next)
			next.addEventListener("click", function () { navigate(state, 1); });
		if (todayBtn)
			todayBtn.addEventListener("click", function () { navigate(state, null); });
	}

	/** Scans and initializes every calendar instance on the page. */
	function initAll(scope) {
		const nodes = (scope || document).querySelectorAll("[" + MARKER_ATTR + "]");
		for (let i = 0; i < nodes.length; i++)
			initInstance(nodes[i]);
	}

	// Expose the pure core (for Node harness coverage) and the DOM entry points.
	NS.pure = {
		pad2: pad2,
		daysInMonth: daysInMonth,
		dayOfWeek: dayOfWeek,
		firstWeekdayOffset: firstWeekdayOffset,
		toEpochDay: toEpochDay,
		fromEpochDay: fromEpochDay,
		dateKey: dateKey,
		buildMonthCells: buildMonthCells,
		civilKey: civilKey,
		contractOk: contractOk,
		echoOk: echoOk,
		sanitizeEvents: sanitizeEvents,
		colorToken: colorToken,
		isSafeDocumentUrl: isSafeDocumentUrl,
		substituteEndpoint: substituteEndpoint,
		eventsForDay: eventsForDay,
		applyCap: applyCap,
		coalesceKey: coalesceKey
	};
	NS.fillEventNode = fillEventNode;
	NS.readCategoryMap = readCategoryMap;
	NS.paintMonth = paintMonth;
	NS.initInstance = initInstance;
	NS.initAll = initAll;

	if (typeof document !== "undefined" && document.addEventListener) {
		if (document.readyState === "loading")
			document.addEventListener("DOMContentLoaded", function () { initAll(); });
		else
			initAll();
	}
})();
