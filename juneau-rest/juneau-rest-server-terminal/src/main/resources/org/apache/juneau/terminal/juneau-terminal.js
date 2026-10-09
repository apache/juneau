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
 * juneau-terminal.js - the terminal region: a command's raw output rendered by xterm.js.
 *
 * Publishes window.JuneauTerminal = { mount, createEngine, fitFont, findNext, parseHash, formatSize, linkHandlerFor }
 * and queues the "terminal" console card handler on window.JuneauConsoleCards.
 *
 * LOAD ORDER: after xterm.js (window.Terminal).  The console shell may load before or after this file: the card
 * queue covers both orders.
 *
 * LIMITS:
 *   - Search highlights by UTF-16 index, so a line with wide (double-cell) characters can highlight the wrong span.
 *   - Step and checkpoint lines are buffer line numbers; they go stale once the scrollback is full and old lines drop.
 *   - Once the bytes are done, the events are still polled (with the idle back-off) until one reports terminal.  After
 *     5 consecutive empty event polls the run is shown as finished, with no exit code, and polling stops.
 *
 * INVARIANTS:
 *   - Bytes reach xterm only through term.write(bytes, callback).  The next write and the next fetch wait for the
 *     callback.
 *   - term.resize is never called.  The terminal keeps the producer's cols x rows; only the font scales.
 *   - No input and no clipboard provider: disableStdin is set, and OSC 52 writes do nothing.
 *   - Links open only for http: and https:, on click, with noopener,noreferrer.
 *   - Server text reaches the DOM only through textContent.
 */
(function () {
	"use strict";

	const LOG_TAG = "[juneau-terminal]";
	const SCROLLBACK = 100000;
	const DEFAULT_REFRESH_MS = 1000;
	const IDLE_REFRESH_MS = 5000;
	const IDLE_AFTER_EMPTY = 3;
	const EVENTS_GIVE_UP = 5;
	const RETRY_MS = [1000, 2000, 5000, 10000];
	const DEFAULT_TAIL_BYTES = 2 * 1024 * 1024;
	const SLOW_LOAD_BYTES = 20 * 1024 * 1024;
	const BASE_FONT = 14;
	const MIN_FONT = 8;
	const MAX_FONT = 20;
	const MAX_EVENT_PAGES = 100;
	const NL = 0x0a;
	const OFFSET = /^\d{1,16}$/;
	const SIGNAL_NOTE = /killed by (SIG[A-Z0-9]+)/;
	const ENC = new TextEncoder();
	/** Written before the bytes after a ring-buffer gap: reset SGR, then leave the alternate screen. */
	const GAP_RESET = ENC.encode("\u001b[0m\u001b[?1049l");

	function isStr(v) { return typeof v === "string"; }

	/** The font size that makes a screen measured at {@code current} px fill the panel, clamped to 8-20. */
	function fitFont(current, panelWidth, screenWidth) {
		if (!(panelWidth > 0) || !(screenWidth > 0))
			return current;
		return Math.min(MAX_FONT, Math.max(MIN_FONT, Math.floor(current * panelWidth / screenWidth)));
	}

	/** "2 MiB", "1.5 MiB", "512 KiB" or "10 B". */
	function formatSize(n) {
		const unit = n >= 1024 * 1024 ? ["MiB", 1024 * 1024] : n >= 1024 ? ["KiB", 1024] : ["B", 1];
		const v = n / unit[1];
		return (Number.isInteger(v) ? String(v) : v.toFixed(1)) + " " + unit[0];
	}

	/** The byte offset in a "#<id>-O<offset>" hash, or null. */
	function parseHash(hash, id) {
		const prefix = "#" + id + "-O";
		if (!isStr(hash) || !isStr(id) || !id || !hash.startsWith(prefix))
			return null;
		const s = hash.slice(prefix.length);
		return OFFSET.test(s) && Number.isSafeInteger(Number(s)) ? Number(s) : null;
	}

	/**
	 * The next case-insensitive match of {@code query} in the buffer after {@code from} ({y, x}, or null to start at
	 * the top or, backward, the bottom), wrapping once.  Returns {y, x, length} or null.  Wrapped rows are searched
	 * one row at a time, so a match split across a wrap is not found.
	 */
	function findNext(buffer, query, from, backward) {
		const q = isStr(query) ? query.toLowerCase() : "";
		const n = buffer.length;
		if (!q || n === 0)
			return null;
		const startY = from ? from.y : backward ? n - 1 : 0;
		for (let k = 0; k <= n; k++) {
			const y = (((backward ? startY - k : startY + k) % n) + n) % n;
			const line = buffer.getLine(y);
			if (!line)
				continue;
			const text = line.translateToString(true).toLowerCase();
			let x;
			if (k === 0 && from)
				x = backward ? (from.x > 0 ? text.lastIndexOf(q, from.x - 1) : -1) : text.indexOf(q, from.x + 1);
			else
				x = backward ? text.lastIndexOf(q) : text.indexOf(q);
			if (x >= 0)
				return { y: y, x: x, length: query.length };
		}
		return null;
	}

	function withQuery(url, q) {
		return url + (url.includes("?") ? "&" : "?") + q;
	}

	function gapLine(n) {
		return ENC.encode("\u001b[2m[… " + n + " bytes dropped …]\u001b[0m\r\n");
	}

	function concat(a, b) {
		const out = new Uint8Array(a.length + b.length);
		out.set(a, 0);
		out.set(b, a.length);
		return out;
	}

	/** The Term-* headers, or null when one that must be there is missing or malformed. */
	function readHeaders(res) {
		const get = function (k) { return res.headers.get(k); };
		const num = function (k) { const v = get(k); return isStr(v) && OFFSET.test(v) ? Number(v) : NaN; };
		const h = {
			next: num("Term-Next"), end: num("Term-End"), cols: num("Term-Cols"), rows: num("Term-Rows"),
			done: get("Term-Done") === "true", truncated: get("Term-Truncated") === "true", gone: get("Term-Error") === "gone"
		};
		if (!Number.isSafeInteger(h.next) || !Number.isSafeInteger(h.end) || !(h.cols >= 1) || !(h.rows >= 1))
			return null;
		return h;
	}

	/**
	 * The poll loop, the write queue and the step-line map, with every side effect behind {@code env}:
	 *   fetch(url, init), setTimeout(fn, ms), clearTimeout(h), hidden(), onVisible(fn) -> off,
	 *   open(cols, rows) -> an xterm Terminal, onStatus(status).
	 * mount() supplies the browser's; the node harness supplies fakes.
	 */
	function createEngine(opts, env) {
		const bytesUrl = opts.bytesUrl;
		const eventsUrl = isStr(opts.eventsUrl) && opts.eventsUrl ? opts.eventsUrl : null;
		const refreshMs = opts.refreshMs > 0 ? opts.refreshMs : DEFAULT_REFRESH_MS;
		const tailBytes = opts.tailBytes > 0 ? opts.tailBytes : DEFAULT_TAIL_BYTES;
		const s = {
			term: null, gen: 0, probed: false, full: false, next: 0, end: 0, writtenTo: 0, bytesDone: false,
			eventsDone: eventsUrl === null, eventsNext: null, trimmedFrom: null, skipToNewline: false,
			emptyPolls: 0, eventsEmpty: 0, failures: 0, steps: [], lines: new Map(), checkpoints: [], pendingScroll: null,
			follow: true, timer: null, ctl: null, offVisible: null, stopped: false, destroyed: false, started: false,
			exit: null, signal: null, doneStatus: null, badge: "Connecting", kind: "running", reconnecting: false,
			banner: null
		};

		function status() {
			return {
				badge: s.badge, kind: s.kind, reconnecting: s.reconnecting, banner: s.banner, follow: s.follow,
				next: s.next, writtenTo: s.writtenTo, trimmedFrom: s.trimmedFrom, stopped: s.stopped
			};
		}

		function emit() {
			if (env.onStatus)
				env.onStatus(status());
		}

		function setBadge(text, kind) {
			s.badge = text;
			s.kind = kind;
			emit();
		}

		function curLine() {
			const b = s.term.buffer.normal;
			return b.baseY + b.cursorY;
		}

		function checkpoint(offset) {
			const line = curLine();
			const last = s.checkpoints[s.checkpoints.length - 1];
			if (!last || last[1] !== line)
				s.checkpoints.push([offset, line]);
		}

		function checkpointLine(o) {
			let line = null;
			for (const c of s.checkpoints) {
				if (c[0] > o)
					break;
				line = c[1];
			}
			return line;
		}

		/** The line of the nearest step at or below {@code o}, else of the nearest checkpoint, else null. */
		function lineFor(o) {
			let line = null;
			for (const st of s.steps) {
				if (st > o)
					break;
				if (s.lines.has(st))
					line = s.lines.get(st);
			}
			return line !== null ? line : checkpointLine(o);
		}

		/** Gives each step whose bytes are already written, and that has no line yet, its checkpoint's line. */
		function resolveLate() {
			for (const o of s.steps) {
				if (o > s.writtenTo)
					break;
				if (s.lines.has(o) || (s.trimmedFrom !== null && o < s.trimmedFrom))
					continue;
				const line = checkpointLine(o);
				if (line !== null)
					s.lines.set(o, line);
			}
		}

		function addStep(o) {
			let i = s.steps.length;
			while (i > 0 && s.steps[i - 1] > o)
				i--;
			if (s.steps[i - 1] !== o)
				s.steps.splice(i, 0, o);
		}

		function addEvents(events) {
			for (const e of events) {
				if (!e || typeof e !== "object")
					continue;
				if ((e.ev === "step" || e.ev === "suite" || e.ev === "test") && Number.isSafeInteger(e.rawOffset) && e.rawOffset >= 0)
					addStep(e.rawOffset);
				else if (e.ev === "end" && Number.isSafeInteger(e.exit))
					s.exit = e.exit;
				else if (e.ev === "note" && isStr(e.text) && SIGNAL_NOTE.test(e.text))
					s.signal = SIGNAL_NOTE.exec(e.text)[1];
				else if (e.ev === "done" && isStr(e.status))
					s.doneStatus = e.status;
			}
			resolveLate();
			applyPendingScroll();
		}

		function applyPendingScroll() {
			if (s.pendingScroll === null || !s.term || s.pendingScroll > s.writtenTo)
				return;
			if (s.trimmedFrom !== null && s.pendingScroll < s.trimmedFrom) {
				// The target was trimmed away (a deep link that arrived before the first poll): load everything, and
				// keep pendingScroll so it is applied once the replay has written that far.
				if (!s.full)
					replay(true);
				return;
			}
			const line = lineFor(s.pendingScroll);
			s.pendingScroll = null;
			if (line !== null)
				s.term.scrollToLine(line);
		}

		function request(url) {
			const ctl = new AbortController();
			s.ctl = ctl;
			return env.fetch(url, { credentials: "same-origin", cache: "no-store", signal: ctl.signal });
		}

		function write(bytes) {
			return new Promise(function (resolve) { s.term.write(bytes, resolve); });
		}

		/** Writes {@code bytes} (which start at offset {@code at}), split at every step offset inside them. */
		async function writeSplit(gen, bytes, at) {
			const end = at + bytes.length;
			let pos = at;
			for (const o of s.steps) {
				if (o < pos)
					continue;
				if (o >= end)
					break;
				if (o > pos) {
					await write(bytes.subarray(pos - at, o - at));
					if (gen !== s.gen)
						return false;
					pos = o;
				}
				if (!s.lines.has(o))
					s.lines.set(o, curLine());
			}
			if (pos < end) {
				await write(bytes.subarray(pos - at));
				if (gen !== s.gen)
					return false;
			}
			s.writtenTo = end;
			checkpoint(end);
			if (s.follow)
				s.term.scrollToBottom();
			return true;
		}

		async function probe(gen) {
			const res = await request(withQuery(bytesUrl, "from=0&max=0"));
			if (gen !== s.gen)
				return;
			checkStatus(res);
			const h = readHeaders(res);
			if (!h)
				throw { fatal: "Bad response from the terminal endpoint" };
			if (!s.term)
				s.term = env.open(h.cols, h.rows);
			s.end = h.end;
			const start = s.full || h.end <= tailBytes ? 0 : h.end - tailBytes;
			s.next = start;
			s.writtenTo = start;
			s.trimmedFrom = start > 0 ? start : null;
			s.skipToNewline = start > 0;
			s.banner = start > 0
				? { text: "Showing the last " + formatSize(tailBytes), slow: h.end > SLOW_LOAD_BYTES ? "The full log is " + formatSize(h.end) + "; loading it all may be slow." : null }
				: null;
			s.checkpoints = [[start, curLine()]];
			s.probed = true;
			emit();
		}

		function checkStatus(res) {
			if (res.status === 404)
				throw { fatal: "Terminal not found" };
			if (!res.ok)
				throw { status: res.status };
		}

		/** One bytes request.  Returns true when the source has more bytes than it sent (catch-up). */
		async function pollBytes(gen) {
			const from = s.next;
			const res = await request(withQuery(bytesUrl, "from=" + from));
			if (gen !== s.gen)
				return false;
			checkStatus(res);
			const h = readHeaders(res);
			if (!h)
				throw { fatal: "Bad response from the terminal endpoint" };
			// A gone source answers with no bytes and Term-Next equal to the requested offset.  Keep what is rendered and
			// keep our own s.next; never move it to the answer's Term-Next.
			if (h.gone) {
				s.bytesDone = true;
				throw { fatal: "Source unavailable" };
			}
			let body = new Uint8Array(await res.arrayBuffer());
			if (gen !== s.gen)
				return false;
			const received = body.length;
			let at = h.next - body.length;
			if (h.truncated && at > from) {
				await write(concat(GAP_RESET, gapLine(at - from)));
				if (gen !== s.gen)
					return false;
				checkpoint(at);
			}
			if (s.skipToNewline && body.length > 0) {
				const i = body.indexOf(NL);
				if (i < 0) {
					body = body.subarray(body.length);
					at = h.next;
				} else {
					body = body.subarray(i + 1);
					at += i + 1;
					s.skipToNewline = false;
				}
				s.trimmedFrom = at;
				s.writtenTo = at;
				s.checkpoints = [[at, curLine()]];
			}
			if (!(await writeSplit(gen, body, at)))
				return false;
			s.next = h.next;
			s.end = h.end;
			s.emptyPolls = body.length > 0 ? 0 : s.emptyPolls + 1;
			resolveLate();
			applyPendingScroll();
			if (gen !== s.gen)
				return false;
			if (h.done)
				s.bytesDone = true;
			return !h.done && h.next < h.end && !(received === 0 && h.next === from);
		}

		async function pollEvents(gen) {
			for (let i = 0; i < MAX_EVENT_PAGES; i++) {
				const url = s.eventsNext === null ? eventsUrl : withQuery(eventsUrl, "after=" + encodeURIComponent(s.eventsNext));
				const res = await request(url);
				if (gen !== s.gen)
					return;
				if (res.status === 410 && s.eventsNext !== null) {
					s.eventsNext = null;
					continue;
				}
				if (res.status >= 500 || res.status === 408 || res.status === 429)
					throw { status: res.status };
				if (!res.ok) {
					console.warn(LOG_TAG, "steps are off: the events endpoint answered HTTP " + res.status);
					s.eventsDone = true;
					return;
				}
				let page;
				try {
					page = await res.json();
				} catch (e) {
					page = null;
				}
				if (gen !== s.gen)
					return;
				if (!page || !Array.isArray(page.events) || !isStr(page.next)) {
					console.warn(LOG_TAG, "steps are off: the events endpoint sent something that is not a run-view page");
					s.eventsDone = true;
					return;
				}
				s.eventsNext = page.next;
				if (page.events.length > 0)
					s.eventsEmpty = 0;
				else
					s.eventsEmpty = s.bytesDone ? s.eventsEmpty + 1 : 0;
				if (page.terminal === true)
					s.eventsDone = true;
				addEvents(page.events);
				if (page.terminal === true || gen !== s.gen)
					return;
				if (page.more !== true || page.events.length === 0)
					return;
			}
		}

		function finalBadge() {
			if (s.exit !== null && s.exit !== 0)
				setBadge("exit " + s.exit + (s.signal ? " (" + s.signal + ")" : ""), "fail");
			else if (s.doneStatus === "fail")
				setBadge("Failed", "fail");
			else
				setBadge("Done", "ok");
		}

		function clearTimer() {
			if (s.timer !== null)
				env.clearTimeout(s.timer);
			s.timer = null;
			if (s.offVisible) {
				s.offVisible();
				s.offVisible = null;
			}
		}

		function schedule(ms) {
			clearTimer();
			s.timer = env.setTimeout(tick, ms);
		}

		/** Stops polling.  Load all can start it again unless the region was destroyed. */
		function halt() {
			s.stopped = true;
			s.gen++;
			clearTimer();
			if (s.ctl)
				s.ctl.abort();
			emit();
		}

		async function tick() {
			s.timer = null;
			if (s.stopped)
				return;
			if (env.hidden()) {
				s.offVisible = env.onVisible(function () {
					s.offVisible();
					s.offVisible = null;
					tick();
				});
				return;
			}
			const gen = s.gen;
			try {
				if (!s.eventsDone)
					await pollEvents(gen);
				if (gen !== s.gen)
					return;
				if (!s.probed)
					await probe(gen);
				let more = !s.bytesDone && gen === s.gen;
				while (more && gen === s.gen)
					more = await pollBytes(gen);
				if (gen !== s.gen)
					return;
				if (s.failures > 0 || s.reconnecting) {
					s.failures = 0;
					s.reconnecting = false;
				}
				if (s.bytesDone && (s.eventsDone || s.eventsEmpty >= EVENTS_GIVE_UP)) {
					finalBadge();
					halt();
					return;
				}
				setBadge("Running", "running");
				const idle = (s.bytesDone ? s.eventsEmpty : s.emptyPolls) >= IDLE_AFTER_EMPTY;
				schedule(idle ? IDLE_REFRESH_MS : refreshMs);
			} catch (e) {
				if (gen !== s.gen || s.stopped)
					return;
				onFailure(e);
			}
		}

		function onFailure(e) {
			if (e && isStr(e.fatal)) {
				setBadge(e.fatal, "error");
				halt();
				return;
			}
			const st = e && typeof e.status === "number" ? e.status : null;
			if (st === 416) {
				replay(false);
				return;
			}
			if (st !== null && st >= 400 && st < 500 && st !== 408 && st !== 429) {
				setBadge("HTTP " + st, "error");
				halt();
				return;
			}
			s.reconnecting = true;
			const delay = RETRY_MS[Math.min(s.failures, RETRY_MS.length - 1)];
			s.failures++;
			emit();
			schedule(delay);
		}

		/** Resets xterm and replays from the start: Load all ({@code full}), or after a 416 under the tail rules. */
		function replay(full) {
			if (s.destroyed)
				return;
			s.stopped = false;
			s.gen++;
			clearTimer();
			if (s.ctl)
				s.ctl.abort();
			if (s.term)
				s.term.reset();
			s.lines.clear();
			s.checkpoints = [];
			s.probed = false;
			s.full = full;
			s.bytesDone = false;
			s.emptyPolls = 0;
			s.eventsEmpty = 0;
			s.banner = null;
			emit();
			tick();
		}

		function scrollToOffset(o) {
			s.follow = false;
			s.pendingScroll = o;
			emit();
			if (s.trimmedFrom !== null && o < s.trimmedFrom)
				replay(true);
			else
				applyPendingScroll();
		}

		return {
			start: function () {
				if (!s.started) {
					s.started = true;
					tick();
				}
			},
			stop: function () {
				s.destroyed = true;
				halt();
			},
			loadAll: function () { replay(true); },
			scrollToOffset: scrollToOffset,
			setFollow: function (on) {
				s.follow = !!on;
				if (s.follow && s.term)
					s.term.scrollToBottom();
				emit();
			},
			lineFor: lineFor,
			status: status,
			terminal: function () { return s.term; }
		};
	}

	function el(doc, tag, cls, text) {
		const e = doc.createElement(tag);
		if (cls)
			e.className = cls;
		if (text !== undefined)
			e.textContent = text;
		return e;
	}

	/**
	 * The xterm OSC 8 link handler.  Two guards, each enough alone: allowNonHttpProtocols is left unset, so xterm drops
	 * every non-HTTP link before activate; and activate opens only http: and https:, in a new tab, with
	 * noopener,noreferrer.
	 */
	function linkHandlerFor(win) {
		return {
			activate: function (event, text) {
				let u;
				try {
					u = new URL(text);
				} catch (e) { // NOSONAR javascript:S2486 -- an unparseable link is ignored
					return;
				}
				if (u.protocol === "http:" || u.protocol === "https:")
					win.open(u.href, "_blank", "noopener,noreferrer");
			}
		};
	}

	/**
	 * Mounts a terminal region in {@code host}.  {@code opts} is the card's terminal object plus its {@code id}:
	 * {bytesUrl, eventsUrl?, refreshMs?, title?, tailBytes?, id?}.  Returns a cleanup that stops polling and removes
	 * the region.
	 */
	function mount(host, opts) {
		const doc = host.ownerDocument;
		const win = doc.defaultView;
		if (typeof win.Terminal !== "function")
			throw new Error(LOG_TAG + " xterm.js is not loaded: window.Terminal is missing");
		if (!opts || !isStr(opts.bytesUrl) || !opts.bytesUrl)
			throw new Error(LOG_TAG + " bytesUrl is required");

		const root = el(doc, "div", "juneau-term");
		root.setAttribute("role", "region");
		root.setAttribute("aria-label", isStr(opts.title) && opts.title ? opts.title : "Terminal");
		const bar = el(doc, "div", "juneau-term-bar");
		const search = el(doc, "input", "juneau-term-search");
		search.type = "search";
		search.placeholder = "Search";
		search.setAttribute("aria-label", "Search the terminal output");
		const copy = el(doc, "button", "juneau-term-copy", "Copy");
		copy.type = "button";
		const followLabel = el(doc, "label", "juneau-term-follow");
		const follow = el(doc, "input");
		follow.type = "checkbox";
		follow.checked = true;
		followLabel.appendChild(follow);
		followLabel.appendChild(doc.createTextNode(" Follow"));
		const reconnecting = el(doc, "span", "juneau-term-chip", "Reconnecting…");
		reconnecting.hidden = true;
		const badge = el(doc, "span", "juneau-term-badge");
		badge.setAttribute("role", "status");
		bar.append(search, copy, followLabel, reconnecting, badge);
		if (/\/bytes$/.test(opts.bytesUrl)) {
			const raw = el(doc, "a", "juneau-term-raw", "Raw");
			try {
				const u = new URL(opts.bytesUrl.replace(/\/bytes$/, "/raw"), win.location.href);
				if (u.protocol === "http:" || u.protocol === "https:") {
					raw.href = u.href;
					raw.setAttribute("download", "");
					bar.appendChild(raw);
				}
			} catch (e) { // NOSONAR javascript:S2486 -- an unparseable URL gets no Raw link
			}
		}
		const banner = el(doc, "div", "juneau-term-banner");
		banner.hidden = true;
		const bannerText = el(doc, "span", "juneau-term-banner-text");
		const loadAll = el(doc, "button", "juneau-term-load-all", "Load all");
		loadAll.type = "button";
		const bannerSlow = el(doc, "span", "juneau-term-banner-slow");
		banner.append(bannerText, doc.createTextNode(" · "), loadAll, bannerSlow);
		const screen = el(doc, "div", "juneau-term-screen");
		root.append(bar, banner, screen);
		host.appendChild(root);

		let term = null;
		let match = null;
		let ro = null;

		function fit() {
			const xs = screen.querySelector(".xterm-screen");
			if (!term || !xs)
				return;
			const size = fitFont(term.options.fontSize, screen.clientWidth, xs.getBoundingClientRect().width);
			if (size !== term.options.fontSize)
				term.options.fontSize = size;
		}

		const linkHandler = linkHandlerFor(win);

		function render(st) {
			badge.textContent = st.badge;
			badge.className = "juneau-term-badge juneau-term-" + st.kind;
			reconnecting.hidden = !st.reconnecting;
			follow.checked = st.follow;
			banner.hidden = !st.banner;
			bannerText.textContent = st.banner ? st.banner.text : "";
			bannerSlow.textContent = st.banner && st.banner.slow ? " " + st.banner.slow : "";
		}

		const engine = createEngine(opts, {
			fetch: win.fetch.bind(win),
			setTimeout: win.setTimeout.bind(win),
			clearTimeout: win.clearTimeout.bind(win),
			hidden: function () { return doc.hidden === true; },
			onVisible: function (fn) {
				const h = function () { if (!doc.hidden) fn(); };
				doc.addEventListener("visibilitychange", h);
				return function () { doc.removeEventListener("visibilitychange", h); };
			},
			open: function (cols, rows) {
				term = new win.Terminal({
					cols: cols, rows: rows, scrollback: SCROLLBACK, disableStdin: true, allowProposedApi: false,
					fontSize: BASE_FONT, cursorBlink: false, linkHandler: linkHandler
				});
				term.open(screen);
				fit();
				if (typeof win.ResizeObserver === "function") {
					ro = new win.ResizeObserver(fit);
					ro.observe(screen);
				}
				return term;
			},
			onStatus: render
		});

		search.addEventListener("keydown", function (e) {
			if (e.key !== "Enter" || !term)
				return;
			e.preventDefault();
			const m = findNext(term.buffer.active, search.value, match, e.shiftKey);
			search.setAttribute("aria-invalid", m || !search.value ? "false" : "true");
			if (!m)
				return;
			match = m;
			engine.setFollow(false);
			term.select(m.x, m.y, m.length);
			term.scrollToLine(Math.max(0, m.y - Math.floor(term.rows / 2)));
		});
		search.addEventListener("input", function () { match = null; });
		copy.addEventListener("click", function () {
			if (term && term.hasSelection() && win.navigator.clipboard)
				win.navigator.clipboard.writeText(term.getSelection()).catch(function () {});
		});
		follow.addEventListener("change", function () { engine.setFollow(follow.checked); });
		loadAll.addEventListener("click", function () { engine.loadAll(); });

		function onHash() {
			const o = parseHash(win.location.hash, opts.id);
			if (o !== null)
				engine.scrollToOffset(o);
		}
		win.addEventListener("hashchange", onHash);

		render(engine.status());
		engine.start();
		onHash();

		return function cleanup() {
			engine.stop();
			win.removeEventListener("hashchange", onHash);
			if (ro)
				ro.disconnect();
			if (term)
				term.dispose();
			if (root.parentNode)
				root.parentNode.removeChild(root);
		};
	}

	/** Host element -> cleanup of the region mounted in it. */
	const MOUNTED = new WeakMap();

	const cardHandler = {
		render: function (card, host) {
			const prev = MOUNTED.get(host);
			if (prev)
				prev();
			MOUNTED.set(host, mount(host, Object.assign({ id: card.id }, card.terminal)));
		},
		destroy: function (card, host) {
			const cleanup = MOUNTED.get(host);
			if (cleanup) {
				MOUNTED.delete(host);
				cleanup();
			}
		}
	};

	window.JuneauTerminal = Object.freeze({
		mount: mount, createEngine: createEngine, fitFont: fitFont, findNext: findNext, parseHash: parseHash,
		formatSize: formatSize, linkHandlerFor: linkHandlerFor
	});
	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["terminal", cardHandler]);
})();
