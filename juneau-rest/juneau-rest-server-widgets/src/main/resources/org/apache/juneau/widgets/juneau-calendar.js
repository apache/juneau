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
 * server-stamped civil today, endpoint template, view, weekStart, maxPerDay, and lane budget, performs a FAIL-LOUD
 * contract-version handshake (a data-juneau-calendar-contract that !== the baked string "2" -> visible error, no
 * fetch/paint), then computes each month's grid client-side, hydrates events from a same-origin data-only per-month
 * GET (or the embedded seed sidecar for the initial month), and wires prev/next/today navigation plus the legend's
 * category toggle-filter.
 *
 * Everything in the "PURE LOGIC LAYER" is DOM-free (plain data in, plain data out) and unit-tested from Node; the
 * "DOM BINDING LAYER" is the thin shim that scans, clones <template>s, fills with textContent (never innerHTML), and
 * binds events.  Civil dates are bucketed field-wise (never Date.parse of a date-only string) so an all-day chip
 * cannot drift a day across timezones, and "today" is the server-stamped civil date, never the browser clock.
 *
 * LOAD-ORDER CONTRACT: juneau-views.js MUST be loaded BEFORE this file.  The "+N more" popover is a layer on the ONE
 * shared layer stack that juneau-views.js owns (window.JuneauViews.init.pushLayer), so Escape, light-dismiss,
 * focus-restore, and z-index ordering are shared with every other Juneau popup.  This file deliberately defines NO
 * pushLayer of its own: when the shared stack is absent the popover FAILS LOUD (console.error + a visible inline
 * error) rather than silently degrading to a second, competing stack.
 *
 * `end` is LAYOUT-SIGNIFICANT, with split inclusivity mirroring CalendarEvent: a date-only (all-day) `end` is
 * INCLUSIVE, a date-time (timed) `end` is EXCLUSIVE, and an omitted `end` means start-only.  An event covering more
 * than one day cell is drawn as a spanning BAR (one segment per week row, seated into per-week lanes); everything
 * else is a chip in its day cell.  The two caps are separate: lanes are budgeted per week row, maxPerDay caps chips.
 */
(function () {
	"use strict";

	// Contract-version handshake: MUST equal CalendarDef.CONTRACT_VERSION on the server.  The JSON STRING "2"
	// (a numeric 2 would fail this strict ===).  A live 200 body whose contractVersion differs is refused.
	// Bumped "1" -> "2" with the bean: `end` became layout-significant.  The envelope gained no new required field
	// (`end` was already carried), but this same string is the stamped data-juneau-calendar-contract attribute, so
	// the bump is what makes a v1 runtime paired with a v2 server fail loud instead of silently flattening a
	// multi-day event into a single chip.
	const JUNEAU_CALENDAR_CONTRACT_VERSION = "2";

	const MIN_POLL_INTERVAL_MS = 5000; // mirrors SafePathTemplate.MIN_POLL_INTERVAL_MS (not polled in v1; reserved).

	// DOM attribute names - MUST equal CalendarTable's constants of the same names on the server.
	const MARKER_ATTR = "data-juneau-calendar";
	const CONTRACT_ATTR = "data-juneau-calendar-contract";
	const TODAY_ATTR = "data-juneau-calendar-today";
	const ENDPOINT_ATTR = "data-juneau-calendar-endpoint";
	const VIEW_ATTR = "data-juneau-calendar-view";
	const WEEKSTART_ATTR = "data-juneau-calendar-weekstart";
	const MAXPERDAY_ATTR = "data-juneau-calendar-maxperday";
	const LANEBUDGET_ATTR = "data-juneau-calendar-lanebudget";
	const GRID_ATTR = "data-juneau-calendar-grid";
	const TITLE_ATTR = "data-juneau-calendar-title";
	const CAT_ATTR = "data-juneau-calendar-cat";
	const EVENT_ID_ATTR = "data-juneau-calendar-event-id";
	const LEGEND_TOGGLE_ATTR = "data-juneau-calendar-legend-toggle";
	const DAY_TEMPLATE_ATTR = "data-juneau-calendar-day";
	const EVENT_TEMPLATE_ATTR = "data-juneau-calendar-event";
	const BAR_TEMPLATE_ATTR = "data-juneau-calendar-bar";
	const SEED_ATTR = "data-juneau-calendar-seed";
	const INIT_PENDING_ATTR = "data-juneau-init-pending";

	const GRID_WEEKS = 6;
	const WEEK_DAYS = 7;
	const LANE_OVERFLOW = -1;   // mirrors CalendarLayout.LANE_OVERFLOW.
	const MAX_LANES_PER_WEEK = 8;   // mirrors CalendarDef.MAX_LANES_PER_WEEK.

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

	/** The epoch day of a "yyyy-MM-dd" civil key. */
	function keyToEpochDay(key) {
		return toEpochDay(parseInt(key.slice(0, 4), 10), parseInt(key.slice(5, 7), 10), parseInt(key.slice(8, 10), 10));
	}

	/** The civil date key of an epoch day. */
	function epochDayToKey(z) {
		const c = fromEpochDay(z);
		return dateKey(c.y, c.m, c.d);
	}

	/** Whether an ISO value is date-only ("yyyy-MM-dd") rather than a date-time. */
	function isDateOnly(v) {
		return typeof v === "string" && v.length <= 10;
	}

	/** Whether an ISO value carries a value at all (an omitted or blank end means start-only). */
	function hasEnd(e) {
		return typeof e.end === "string" && e.end.trim().length > 0;
	}

	/**
	 * Minutes-since-midnight of an ISO date-time, ignoring any trailing offset/"Z" (fail-soft: an offset is IGNORED,
	 * never a rejection - shipped seeds carry them).  Returns null for a date-only or unparseable value.
	 */
	function civilMinutes(s) {
		if (typeof s !== "string" || s.length <= 11 || s.charAt(10) !== "T")
			return null;
		const m = /^(\d{2}):(\d{2})/.exec(s.slice(11));
		if (!m)
			return null;
		const h = parseInt(m[1], 10);
		const min = parseInt(m[2], 10);
		return (h > 23 || min > 59) ? null : h * 60 + min;
	}

	/** The effective all-day flag - the declared allDay when boolean, else true when start carries no time. */
	function effectiveAllDay(e) {
		if (typeof e.allDay === "boolean")
			return e.allDay;
		return typeof e.start === "string" && e.start.indexOf("T") < 0;
	}

	/**
	 * The last day cell an event occupies, applying SPLIT INCLUSIVITY: a date-only (all-day) end is INCLUSIVE, a
	 * date-time (timed) end is EXCLUSIVE (so an end of exactly midnight lands on the previous day), and an omitted
	 * end means start-only.  Never before the start day.  Mirrors CalendarEvent.lastDay().
	 */
	function lastDayKey(e) {
		const startKey = civilKey(e.start);
		if (!startKey)
			return null;
		if (!hasEnd(e))
			return startKey;
		const endKey = civilKey(e.end);
		if (!endKey)
			return startKey;
		const startEpoch = keyToEpochDay(startKey);
		let endEpoch = keyToEpochDay(endKey);
		if (!effectiveAllDay(e) && !civilMinutes(e.end))
			endEpoch -= 1;   // exclusive bound at (or before) midnight: the last occupied instant is the day before.
		return epochDayToKey(Math.max(startEpoch, endEpoch));
	}

	/** Whether an event covers more than one day cell and therefore draws as a spanning bar rather than a chip. */
	function spanning(e) {
		const startKey = civilKey(e.start);
		const endKey = lastDayKey(e);
		return !!startKey && !!endKey && endKey > startKey;
	}

	/** The leading "HH:mm" label a timed chip renders, or null for an all-day event or an unparseable time. */
	function startTimeLabel(e) {
		if (effectiveAllDay(e))
			return null;
		const mins = civilMinutes(e.start);
		return mins === null ? null : pad2(Math.floor(mins / 60)) + ":" + pad2(mins % 60);
	}

	/**
	 * The reason an event is malformed, or null when it is well-formed.  This is the CLOSED set mirrored from
	 * CalendarEvent.malformedReason(): a missing id/title/start; an unparseable date; a declared allDay/end
	 * disagreement (decided by effectiveAllDay(), so a null allDay with matching shapes stays VALID); mixed
	 * start/end shapes with a null allDay; and an end that is not after (all-day: not on or after) start.  An
	 * OMITTED end is not malformed, and a trailing offset/"Z" is ignored, not rejected.
	 */
	function malformedReason(e) {
		if (!e || !e.id || !e.title || !e.start)
			return "missing id/title/start";
		const startKey = civilKey(e.start);
		if (!startKey)
			return "unparseable start";
		if (!hasEnd(e))
			return null;
		const endKey = civilKey(e.end);
		if (!endKey)
			return "unparseable end";
		if (effectiveAllDay(e) !== isDateOnly(e.end))
			return "end shape disagrees with the effective all-day flag";
		if (effectiveAllDay(e))
			return endKey < startKey ? "end must be >= start" : null;
		const startAt = keyToEpochDay(startKey) * 1440 + (civilMinutes(e.start) || 0);
		const endAt = keyToEpochDay(endKey) * 1440 + (civilMinutes(e.end) || 0);
		// Exclusive end: a timed [t, t) interval is empty, so equality is rejected as well as inversion.
		return endAt > startAt ? null : "timed end must be after start";
	}

	/** Strict contract handshake: the value must be exactly the string "2" (a numeric 2 fails). */
	function contractOk(v) {
		return v === JUNEAU_CALENDAR_CONTRACT_VERSION;
	}

	/** The envelope's year/month must echo the requested (numeric) window, else the body is dropped. */
	function echoOk(envelope, y, m) {
		return !!envelope && envelope.year === y && envelope.month === m;
	}

	/**
	 * Fail-soft wire-data cleanse: DROP the malformed events of a payload and keep painting the rest (console.warn
	 * each), keeping the first of any intra-payload duplicate id.  The drop predicate is malformedReason(), the same
	 * closed set CalendarDef.validate() drops on the seed path - GET is identical to POST, and neither one fails a
	 * whole month over one bad event.
	 */
	function sanitizeEvents(events) {
		const out = [];
		const seen = Object.create(null);
		if (!Array.isArray(events))
			return out;
		for (const e of events) {
			const bad = malformedReason(e);
			if (bad) {
				console.warn("juneau-calendar: dropping malformed event (" + bad + ")", e);
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

	/** Compares two strings, null-safely, for a total sort order. */
	function cmp(a, b) {
		const x = a == null ? "" : a;
		const y = b == null ? "" : b;
		return x < y ? -1 : (x > y ? 1 : 0);
	}

	/**
	 * Chip order inside a day cell: ALL-DAY chips first, then TIMED chips ASCENDING, with the event id as the final
	 * tie-break so the order is total and therefore identical across a re-render.  Mirrors CalendarLayout.CHIP_ORDER.
	 */
	function chipCompare(a, b) {
		const aa = effectiveAllDay(a) ? 0 : 1;
		const bb = effectiveAllDay(b) ? 0 : 1;
		if (aa !== bb)
			return aa - bb;
		return cmp(a.start, b.start) || cmp(a.id, b.id);
	}

	/** The NON-SPANNING events whose civil start is exactly the given in-month date key, in chip order. */
	function eventsForDay(events, key) {
		return events
			.filter(function (e) { return !spanning(e) && civilKey(e.start) === key; })
			.sort(chipCompare);
	}

	/** Splits a day's chips into the shown ones (<= maxPerDay) and the overflow count (design doc §5.11). */
	function applyCap(events, maxPerDay) {
		const cap = maxPerDay > 0 ? maxPerDay : events.length;
		return { shown: events.slice(0, cap), overflow: Math.max(0, events.length - cap) };
	}

	/** The derived per-week lane budget: maxPerDay clamped to the hard internal cap.  Never author-controllable. */
	function laneBudgetFor(maxPerDay) {
		return Math.min(maxPerDay > 0 ? maxPerDay : 1, MAX_LANES_PER_WEEK);
	}

	/**
	 * Cuts every spanning event into per-week-row pieces and seats them into lanes.  A piece is clipped to the
	 * month's visible IN-MONTH days (adjacent cells never carry events), so continuesLeft/continuesRight are raised
	 * both by a week-boundary cut and by a month clip - a span longer than the month therefore clips with a flag on
	 * both ends.  Every piece of one span carries the same event, so hover/focus/filter act on the whole event.
	 * Mirrors CalendarLayout.segments()/seatLanes().
	 */
	function buildSegments(events, y, m, weekStart, laneBudget) {
		const gridStart = toEpochDay(y, m, 1) - firstWeekdayOffset(y, m, weekStart);
		const monthStart = toEpochDay(y, m, 1);
		const monthEnd = monthStart + daysInMonth(y, m) - 1;
		const spans = events.filter(spanning);
		const out = [];
		for (let w = 0; w < GRID_WEEKS; w++) {
			const rowStart = gridStart + w * WEEK_DAYS;
			const rowEnd = rowStart + WEEK_DAYS - 1;
			const row = [];
			for (const e of spans) {
				const trueStart = keyToEpochDay(civilKey(e.start));
				const trueEnd = keyToEpochDay(lastDayKey(e));
				const from = Math.max(trueStart, monthStart, rowStart);
				const to = Math.min(trueEnd, monthEnd, rowEnd);
				if (from > to)
					continue;
				row.push({ event: e, id: e.id, week: w, startColumn: from - rowStart, endColumn: to - rowStart,
					continuesLeft: from > trueStart, continuesRight: to < trueEnd, lane: LANE_OVERFLOW });
			}
			seatLanes(row, laneBudget).forEach(function (s) { out.push(s); });
		}
		return out;
	}

	/**
	 * Greedy first-fit lane sweep over one week row: pieces are swept left to right (ties broken by start then id,
	 * so the sweep order is TOTAL and a re-render of the same month cannot reshuffle the lanes) and each is seated
	 * in the lowest lane whose previous occupant ends before it begins.  A piece that would need a lane beyond the
	 * budget is left at LANE_OVERFLOW and collapses into the crossed days' "+N more" instead of growing the row.
	 */
	function seatLanes(row, laneBudget) {
		row.sort(function (a, b) {
			return (a.startColumn - b.startColumn) || cmp(a.event.start, b.event.start) || cmp(a.id, b.id);
		});
		const laneEnds = [];
		for (const s of row) {
			let lane = LANE_OVERFLOW;
			for (let i = 0; i < laneEnds.length; i++) {
				if (laneEnds[i] < s.startColumn) { lane = i; break; }
			}
			if (lane === LANE_OVERFLOW && laneEnds.length < laneBudget) {
				lane = laneEnds.length;
				laneEnds.push(s.endColumn);
			} else if (lane !== LANE_OVERFLOW) {
				laneEnds[lane] = s.endColumn;
			}
			s.lane = lane;
		}
		row.sort(function (a, b) { return (a.lane - b.lane) || (a.startColumn - b.startColumn); });
		return row;
	}

	/** The seated (non-overflowed) pieces of one week row. */
	function segmentsForWeek(segments, week) {
		return segments.filter(function (s) { return s.week === week && s.lane !== LANE_OVERFLOW; });
	}

	/** The number of lanes a week row actually needs (its tallest seated lane plus one), or 0 when it draws none. */
	function laneCount(segments, week) {
		let n = 0;
		segmentsForWeek(segments, week).forEach(function (s) { n = Math.max(n, s.lane + 1); });
		return n;
	}

	/** The events of unseated bars crossing a given cell index - what that day's "+N more" additionally hides. */
	function overflowBarsAt(segments, week, column) {
		return segments
			.filter(function (s) {
				return s.week === week && s.lane === LANE_OVERFLOW && column >= s.startColumn && column <= s.endColumn;
			})
			.map(function (s) { return s.event; });
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

	/**
	 * Builds one event node (an <a> for a safe same-origin href, else a span), filled with textContent only.  A
	 * TIMED chip gets a leading "HH:mm" label in its own span ahead of the title; an all-day chip stays a single
	 * text node so a title of "<img onerror>" remains literal text either way.
	 */
	function fillEventNode(doc, event, categoryMap) {
		const linked = event.href && isSafeDocumentUrl(event.href);
		const node = doc.createElement(linked ? "a" : "span");
		const time = startTimeLabel(event);
		node.setAttribute("class", "jc-cal-event jc-cal-cat--" + colorToken(event.categoryId, categoryMap)
			+ (time ? " jc-cal-event--timed" : ""));
		if (linked)
			node.setAttribute("href", event.href);
		if (time) {
			node.appendChild(fillSpan(doc, "jc-cal-event-time", time));
			node.appendChild(fillSpan(doc, "jc-cal-event-title", event.title));
		} else {
			node.textContent = event.title; // textContent, NEVER innerHTML.
		}
		decorateEventNode(node, event);
		return node;
	}

	/** A span of one class filled with textContent. */
	function fillSpan(doc, cls, text) {
		const s = doc.createElement("span");
		s.setAttribute("class", cls);
		s.textContent = text;
		return s;
	}

	/** Stamps the event id, category hook, and tooltip shared by chips and spanning-bar pieces. */
	function decorateEventNode(node, event) {
		node.setAttribute(EVENT_ID_ATTR, event.id);
		if (event.categoryId)
			node.setAttribute(CAT_ATTR, event.categoryId);
		if (event.tooltip)
			node.setAttribute("title", event.tooltip);
	}

	/**
	 * Builds one piece of a spanning bar.  Every piece of a span carries the same event id, href, and tooltip, so
	 * hover, focus, activation, and the legend filter all work from ANY segment, not just the first.
	 */
	function fillBarNode(doc, segment, categoryMap) {
		const event = segment.event;
		const linked = event.href && isSafeDocumentUrl(event.href);
		const node = doc.createElement(linked ? "a" : "span");
		let cls = "jc-cal-bar jc-cal-cat--" + colorToken(event.categoryId, categoryMap);
		if (segment.continuesLeft)
			cls += " jc-cal-bar--continues-left";
		if (segment.continuesRight)
			cls += " jc-cal-bar--continues-right";
		node.setAttribute("class", cls);
		if (linked)
			node.setAttribute("href", event.href);
		node.textContent = event.title;
		if (segment.continuesLeft || segment.continuesRight)
			node.setAttribute("aria-label", event.title + " (continues)");
		if (node.style) {
			node.style.setProperty("--jc-cal-span", String(segment.endColumn - segment.startColumn + 1));
			node.style.setProperty("--jc-cal-lane", String(segment.lane));
		}
		decorateEventNode(node, event);
		return node;
	}

	/**
	 * Resolves the ONE shared layer stack (window.JuneauViews.init, owned by juneau-views.js) or null when views is
	 * not loaded.  This runtime NEVER defines pushLayer/popLayer of its own: the "+N more" popover is a client of the
	 * shared stack so it shares Escape, light-dismiss, focus-restore, and z-index depth with every other Juneau
	 * popup.  A null return is a LOAD-ORDER violation (juneau-views.js must be loaded first), reported loudly.
	 */
	function viewsLayerStack() {
		const views = window.JuneauViews && window.JuneauViews.init;
		return views && typeof views.pushLayer === "function" && typeof views.popLayer === "function" ? views : null;
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

	/** Caches an (already-sanitized) event list as the month's data, then paints through the category filter. */
	function paintMonth(state, events) {
		state.events = events;
		renderMonth(state);
	}

	/**
	 * Repaints the grid for state.year/state.month from the CACHED event list, minus the categories the legend has
	 * toggled off.  Filtering never refetches: a legend press is a pure repaint of data already in hand.
	 */
	function renderMonth(state) {
		const events = visibleEvents(state);
		const cells = buildMonthCells(state.year, state.month, state.weekStart);
		const segments = buildSegments(events, state.year, state.month, state.weekStart, state.laneBudget);
		const grid = state.grid;
		// Remove prior week rows (keep the weekday header row, the first [role=row]).
		const weeks = grid.querySelectorAll(".jc-cal-week");
		for (let i = 0; i < weeks.length; i++)
			weeks[i].parentNode.removeChild(weeks[i]);

		let row = null;
		for (let i = 0; i < cells.length; i++) {
			const week = Math.floor(i / WEEK_DAYS);
			if (i % WEEK_DAYS === 0) {
				row = document.createElement("div");
				row.setAttribute("role", "row");
				row.setAttribute("class", "jc-cal-week");
				grid.appendChild(row);
			}
			row.appendChild(buildCell(state, cells[i], events, segments, week, i % WEEK_DAYS));
		}
		const title = state.root.querySelector("[" + TITLE_ATTR + "]");
		if (title)
			title.textContent = MONTH_NAMES[state.month - 1] + " " + state.year;
	}

	/** The cached events minus every category the legend has toggled off (an uncategorized event is never hidden). */
	function visibleEvents(state) {
		return (state.events || []).filter(function (e) {
			return !(e.categoryId && state.hiddenCategories[e.categoryId]);
		});
	}

	/**
	 * Builds one day cell by cloning the day template, painting the day number, the spanning-bar pieces that START
	 * in this cell, and (in-month only) the capped chips plus "+N more".
	 */
	function buildCell(state, cell, events, segments, week, column) {
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

		// Every cell of a week row reserves the same lane band so bars line up across the row.
		const lanes = laneCount(segments, week);
		const laneBox = node.querySelector(".jc-cal-day-lanes");
		if (laneBox && lanes > 0) {
			if (node.style)
				node.style.setProperty("--jc-cal-lanes", String(lanes));
			segmentsForWeek(segments, week)
				.filter(function (s) { return s.startColumn === column; })
				.forEach(function (s) { laneBox.appendChild(fillBarNode(document, s, state.categoryMap)); });
		}

		const box = node.querySelector(".jc-cal-day-events");
		if (box && cell.inMonth) {
			const dayEvents = eventsForDay(events, cell.key);
			const capped = applyCap(dayEvents, state.maxPerDay);
			for (let i = 0; i < capped.shown.length; i++)
				box.appendChild(fillEventNode(document, capped.shown[i], state.categoryMap));
			// "+N more" counts ONLY what it actually hides: the chips over the cap plus any bar crossing this day
			// that the week's lane budget could not seat.  A seated bar costs the day cell nothing.
			const hidden = dayEvents.slice(capped.shown.length).concat(overflowBarsAt(segments, week, column));
			if (hidden.length > 0)
				box.appendChild(buildMoreButton(state, hidden));
		}
		return node;
	}

	/** The "+N more" button that opens the day popover listing exactly the events that cell hides. */
	function buildMoreButton(state, hidden) {
		const btn = document.createElement("button");
		btn.setAttribute("type", "button");
		btn.setAttribute("class", "jc-cal-more");
		btn.setAttribute("aria-haspopup", "dialog");
		btn.textContent = "+" + hidden.length + " more";
		btn.addEventListener("click", function () {
			openDayPopover(state, btn, hidden);
		});
		return btn;
	}

	/**
	 * Opens (or replaces) the day popover next to the trigger, listing exactly the events the cell hides, and
	 * registers it as a light-dismiss "popover" on the ONE shared layer stack so Escape pops it, focus returns to
	 * the trigger, and it stacks correctly against dialogs and menus.
	 *
	 * FAIL LOUD: when juneau-views.js was not loaded first there is no shared stack, and this refuses to open rather
	 * than standing up a second, competing stack of its own.
	 */
	function openDayPopover(state, trigger, hidden) {
		const stack = viewsLayerStack();
		if (!stack) {
			console.error("juneau-calendar: window.JuneauViews.init.pushLayer is missing - load juneau-views.js "
				+ "BEFORE juneau-calendar.js; refusing to open the '+N more' popover");
			showError(state, "Calendar popovers require juneau-views.js to be loaded first.");
			return;
		}
		closePopover(state);
		const pop = document.createElement("div");
		pop.setAttribute("class", "jc-cal-popover");
		pop.setAttribute("role", "dialog");
		for (let i = 0; i < hidden.length; i++)
			pop.appendChild(fillEventNode(document, hidden[i], state.categoryMap));
		state.root.appendChild(pop);
		trigger.setAttribute("aria-expanded", "true");
		state.popover = pop;
		state.popoverLayer = stack.pushLayer(pop, {
			kind: "popover",
			lightDismiss: true,
			returnFocusTo: trigger,
			onDismiss: function () {
				state.popover = null;
				state.popoverLayer = null;
				trigger.setAttribute("aria-expanded", "false");
			}
		});
	}

	/** Closes any open day popover by popping its shared-stack layer (which detaches it and restores focus). */
	function closePopover(state) {
		const stack = viewsLayerStack();
		if (state.popoverLayer && stack)
			stack.popLayer(state.popover);
		state.popover = null;
		state.popoverLayer = null;
	}

	/** Loads a month: uses the seed sidecar for the initial month, else fetches the same-origin per-month GET. */
	function loadMonth(state, useSeed) {
		clearError(state);
		const generation = ++state.generation;
		if (useSeed && state.seed && echoOk(state.seed, state.year, state.month)) {
			commitMonth(state, sanitizeEvents(state.seed.events));
			return;
		}
		if (!state.endpoint) {
			commitMonth(state, []); // seed-only calendar, no endpoint: nothing to fetch.
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
				state.pendingFilterReset = false;   // a refused body is a FAILED navigation: preserve the filter.
				showError(state, "Calendar data version mismatch.");
				return;
			}
			if (!echoOk(envelope, y, m))
				return; // echo-check: the body is for a different month - drop.
			commitMonth(state, sanitizeEvents(envelope.events));
		}).catch(function (e) {
			if (e && e.name === "AbortError")
				return;
			if (generation === state.generation) {
				state.pendingFilterReset = false;   // failed navigation: the category filter is PRESERVED.
				showError(state, "Unable to load calendar events.");
			}
		});
	}

	/**
	 * Commits a month's freshly-loaded events: a navigation that actually landed resets the category filter first
	 * (state is per-instance and per-month), then the grid paints.  A navigation that FAILED never reaches here, so
	 * its filter survives, and a seed-only calendar has no month nav at all so it never resets.
	 */
	function commitMonth(state, events) {
		if (state.pendingFilterReset) {
			state.pendingFilterReset = false;
			clearFilter(state);
		}
		paintMonth(state, events);
	}

	/** Clears every category toggle back to pressed/visible. */
	function clearFilter(state) {
		state.hiddenCategories = Object.create(null);
		const toggles = state.root.querySelectorAll("[" + LEGEND_TOGGLE_ATTR + "]");
		for (let i = 0; i < toggles.length; i++)
			toggles[i].setAttribute("aria-pressed", "true");
	}

	/** Navigates by a whole-month delta (or to today when delta is null), then reloads. */
	function navigate(state, delta) {
		closePopover(state);
		state.pendingFilterReset = true;
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
				barTemplate: root.querySelector("template[" + BAR_TEMPLATE_ATTR + "]"),
				categoryMap: readCategoryMap(root),
				generation: 0,
				abort: null,
				popover: null,
				popoverLayer: null,
				events: [],
				hiddenCategories: Object.create(null),
				pendingFilterReset: false,
				seed: readSeed(root)
			};
			// The lane budget is DERIVED (never author-set): the server stamps it, and an absent/garbled attribute
			// falls back to the same maxPerDay-clamped-to-8 formula rather than to an author value.
			const stamped = parseInt(root.getAttribute(LANEBUDGET_ATTR), 10);
			state.laneBudget = stamped > 0 ? Math.min(stamped, MAX_LANES_PER_WEEK) : laneBudgetFor(state.maxPerDay);
			const tk = civilKey(today);
			state.year = tk ? parseInt(tk.slice(0, 4), 10) : fromEpochDay(0).y;
			state.month = tk ? parseInt(tk.slice(5, 7), 10) : 1;
			// Prefer the server-painted seed month's year/month if a seed is present.
			if (state.seed && typeof state.seed.year === "number" && typeof state.seed.month === "number") {
				state.year = state.seed.year;
				state.month = state.seed.month;
			}
			wireNav(root, state);
			wireLegend(root, state);
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

	/**
	 * Wires the legend's category toggles.  Each is a real <button aria-pressed>, so it is keyboard-reachable and
	 * operable before this runs; pressing one hides that category's chips AND its spanning-bar segments by
	 * repainting the cached events - CLIENT-SIDE ONLY, with no refetch and no filtered endpoint.
	 */
	function wireLegend(root, state) {
		const toggles = root.querySelectorAll("[" + LEGEND_TOGGLE_ATTR + "]");
		for (let i = 0; i < toggles.length; i++) {
			const btn = toggles[i];
			const cat = btn.getAttribute(CAT_ATTR);
			if (!cat)
				continue;
			btn.addEventListener("click", function () {
				if (state.hiddenCategories[cat])
					delete state.hiddenCategories[cat];
				else
					state.hiddenCategories[cat] = true;
				btn.setAttribute("aria-pressed", state.hiddenCategories[cat] ? "false" : "true");
				closePopover(state);
				renderMonth(state);
			});
		}
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
		civilMinutes: civilMinutes,
		effectiveAllDay: effectiveAllDay,
		lastDayKey: lastDayKey,
		spanning: spanning,
		startTimeLabel: startTimeLabel,
		malformedReason: malformedReason,
		contractOk: contractOk,
		echoOk: echoOk,
		sanitizeEvents: sanitizeEvents,
		colorToken: colorToken,
		isSafeDocumentUrl: isSafeDocumentUrl,
		substituteEndpoint: substituteEndpoint,
		chipCompare: chipCompare,
		eventsForDay: eventsForDay,
		applyCap: applyCap,
		laneBudgetFor: laneBudgetFor,
		buildSegments: buildSegments,
		segmentsForWeek: segmentsForWeek,
		laneCount: laneCount,
		overflowBarsAt: overflowBarsAt,
		coalesceKey: coalesceKey
	};
	NS.fillEventNode = fillEventNode;
	NS.fillBarNode = fillBarNode;
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
