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
 * Publishes window.JuneauTerminal = { mount, createEngine, createInput, linkFor, nudgeLinkFor, fitFont, findNext, parseHash, xtermOptions,
 * formatSize, linkHandlerFor, handle } and queues the "terminal" console card handler on window.JuneauConsoleCards.
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
 *   - Input is off unless the card sets input: true and the page wires a bus bridge for term.input; until the user arms
 *     it and the server answers live, disableStdin stays set.  No clipboard provider: OSC 52 writes do nothing.
 *   - Links open only for http: and https:, on click, with noopener,noreferrer.
 *   - Server text reaches the DOM only through textContent.
 */
(function () {
	"use strict";

	const LOG_TAG = "[juneau-terminal]";
	const SCROLLBACK = 100000;
	const MAX_SCROLLBACK = 1000000;
	const DEFAULT_REFRESH_MS = 1000;
	const IDLE_REFRESH_MS = 5000;
	/** Poll interval while bus nudges arrive: the poll is then only a safety net. */
	const NUDGE_REFRESH_MS = 5000;
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
	const INPUT_TOPIC = "term.input";
	const INPUT_STATUS_TOPIC = "term.input-status";
	const PROBE_TIMEOUT_MS = 2000;
	const FLUSH_MS = 100;
	const MAX_FRAME_BYTES = 4096;
	/** Cap on queued-but-unsent input (about 6.4 s of drain at one 4 KB frame per 100 ms); an over-cap chunk is refused whole. */
	const MAX_QUEUE_BYTES = 256 * 1024;
	const ESC_ESC_MS = 500;
	const DISCONNECTED = "Disconnected: input not sent";
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
			banner: null, nudged: false, linked: false, ticking: false, nudgePending: false, nudgeEnd: 0, nudgeDone: false
		};

		function status() {
			return {
				badge: s.badge, kind: s.kind, reconnecting: s.reconnecting, banner: s.banner, follow: s.follow,
				next: s.next, writtenTo: s.writtenTo, trimmedFrom: s.trimmedFrom, stopped: s.stopped, nudged: s.nudged
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
			s.ticking = false;
			s.nudgePending = false;
			s.nudgeEnd = 0;
			s.nudgeDone = false;
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
			s.ticking = true;
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
				schedule(s.nudged ? Math.max(refreshMs, NUDGE_REFRESH_MS) : (isIdle() ? IDLE_REFRESH_MS : refreshMs));
			} catch (e) {
				if (gen !== s.gen || s.stopped)
					return;
				onFailure(e);
			} finally {
				// A superseded tick (replay, halt) leaves the flags to the tick that replaced it.
				if (gen === s.gen) {
					s.ticking = false;
					// A failed tick has already scheduled its retry (failures > 0).
					const follow = s.nudgePending && !s.stopped && s.failures === 0
						&& (s.nudgeEnd > s.next || (s.nudgeDone && !s.bytesDone));
					s.nudgePending = false;
					s.nudgeEnd = 0;
					s.nudgeDone = false;
					if (follow) {
						clearTimer();
						tick();
					}
				}
			}
		}

		/** True once enough polls came back empty that the card has settled into the slow idle interval. */
		function isIdle() {
			return (s.bytesDone ? s.eventsEmpty : s.emptyPolls) >= IDLE_AFTER_EMPTY;
		}

		/** A bus nudge reported that the terminal's bytes now end at {@code p.end}: fetch now if that is ahead of what is read. */
		function nudge(p) {
			if (s.stopped || s.destroyed || !s.probed)
				return;
			if (!p || !Number.isSafeInteger(p.end) || p.end < 0)
				return;
			if (s.linked)
				s.nudged = true;
			// A retry is waiting out its back-off: a nudge must not skip it.
			if (s.failures > 0)
				return;
			if (!(p.end > s.next || (p.done === true && !s.bytesDone)))
				return;
			if (s.ticking) {
				s.nudgePending = true;
				s.nudgeEnd = Math.max(s.nudgeEnd, p.end);
				s.nudgeDone = s.nudgeDone || p.done === true;
				return;
			}
			// clearTimer first: it also drops a pending hidden-tab listener, so the re-entered tick registers exactly one.
			clearTimer();
			tick();
		}

		/** Reports whether the bus link that carries nudges is up; while it is down the poll runs at its normal pace. */
		function nudgeConnected(on) {
			s.linked = !!on;
			if (!on && s.nudged) {
				s.nudged = false;
				if (s.timer !== null && s.failures === 0)
					schedule(isIdle() ? IDLE_REFRESH_MS : refreshMs);
			}
			emit();
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
			s.ticking = false;
			s.nudgePending = false;
			s.nudgeEnd = 0;
			s.nudgeDone = false;
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
			nudge: nudge,
			nudgeConnected: nudgeConnected,
			terminal: function () { return s.term; }
		};
	}

	/** The UTF-8 length of one code point. */
	function utf8Len(cp) {
		return cp < 0x80 ? 1 : cp < 0x800 ? 2 : cp < 0x10000 ? 3 : 4;
	}

	/** The longest prefix of {@code s} that is whole code points and at most {@code max} UTF-8 bytes. */
	function headOf(s, max) {
		let bytes = 0, i = 0;
		for (const ch of s) {
			const n = utf8Len(ch.codePointAt(0));
			if (bytes + n > max)
				break;
			bytes += n;
			i += ch.length;
		}
		return s.slice(0, i);
	}

	/**
	 * The id of the bridge that carries terminal input: the first websocket bridge in {@code contract} whose upstream
	 * includes term.input and whose downstream includes term.input-status.  Null when there is none.
	 */
	function linkFor(contract) {
		const bridges = contract && Array.isArray(contract.bridges) ? contract.bridges : [];
		for (const b of bridges) {
			if (b && b.transport === "websocket" && isStr(b.id) && Array.isArray(b.upstream) && b.upstream.includes(INPUT_TOPIC)
					&& Array.isArray(b.downstream) && b.downstream.includes(INPUT_STATUS_TOPIC))
				return b.id;
		}
		return null;
	}

	/**
	 * The input state machine for one terminal card, with every side effect behind {@code env}:
	 *   publish(payload), setTimeout(fn, ms), clearTimeout(h), now(), onChange(view), armed(on).
	 * {@code opts.inputId} is the id sent in every payload and the only id whose answers are read.
	 *
	 * States: idle, probing, armed, denied and closed.  Pressing the toggle sends a probe ({id, data: ""}); only a live
	 * answer to a pending probe arms.  denied and closed apply whenever they come; busy marks the last frame dropped.
	 * While armed, keys are batched every 100 ms or at 4096 UTF-8 bytes, one frame per tick, split on code points.
	 * A chunk that would take the unsent queue past 256 KiB is refused whole (never truncated) and the chip says so until
	 * the next accepted chunk.  A busy report from the sink is shown the same way: the throttled chip stays through the drain
	 * until the next accepted chunk, since the sink sends no all-clear.
	 */
	function createInput(opts, env) {
		const id = opts.inputId;
		const s = { state: "idle", chip: null, chipState: null, queue: "", refused: false, throttled: false, timer: null, probeTimer: null, probeSeq: 0,
			pending: null, lastEsc: null, connected: true, disposed: false };

		function view() {
			const label = s.state === "denied" ? "Input not permitted" : s.state === "closed" ? "Job ended" : "Enable input";
			return {
				state: s.state, chip: s.chip, chipState: s.chipState,
				toggle: { label: label, pressed: s.state === "armed", disabled: s.state === "denied" || s.state === "closed" }
			};
		}

		function changed() {
			if (!s.disposed && env.onChange)
				env.onChange(view());
		}

		function setChip(text, tone) {
			s.chip = text;
			s.chipState = text === null ? null : tone;
		}

		function clearProbe() {
			if (s.probeTimer !== null)
				env.clearTimeout(s.probeTimer);
			s.probeTimer = null;
			s.pending = null;
		}

		function clearFlush() {
			if (s.timer !== null)
				env.clearTimeout(s.timer);
			s.timer = null;
		}

		/** Sends one frame of at most 4096 bytes from the queue, then waits a tick before the next. */
		function sendOne() {
			const head = headOf(s.queue, MAX_FRAME_BYTES);
			s.queue = s.queue.slice(head.length);
			env.publish({ id: id, data: head });
			if (s.state === "armed" && !s.refused && !s.throttled && s.chip !== "Input live")
				setChip("Input live", "live");
			s.timer = env.setTimeout(tick, FLUSH_MS);
		}

		function tick() {
			s.timer = null;
			if (s.state === "armed" && s.queue) {
				sendOne();
				changed();
			}
		}

		/** Leaves armed (or probing) for {@code next}; {@code flush} sends at most one frame of what is queued. */
		function disarm(next, flush) {
			const was = s.state === "armed";
			clearProbe();
			clearFlush();
			if (flush && was && s.queue)
				env.publish({ id: id, data: headOf(s.queue, MAX_FRAME_BYTES) });
			s.queue = "";    // no s.refused/s.throttled reset: the queue is empty, and an accepted chunk clears the flag before any frame
			s.lastEsc = null;
			s.state = next;
			if (was)
				env.armed(false);
		}

		function press() {
			if (s.disposed)
				return;
			if (s.state === "armed") {
				disarm("idle", true);
				setChip(null);
				changed();
				return;
			}
			if (s.state !== "idle")
				return;
			if (!s.connected) {
				setChip(DISCONNECTED, "warn");
				changed();
				return;
			}
			const seq = ++s.probeSeq;
			s.pending = seq;
			s.state = "probing";
			setChip(null);
			s.probeTimer = env.setTimeout(function () {
				if (s.pending !== seq)
					return;
				s.probeTimer = null;
				s.pending = null;
				s.state = "idle";
				setChip("Input unavailable", "warn");
				changed();
			}, PROBE_TIMEOUT_MS);
			env.publish({ id: id, data: "" });
			changed();
		}

		function onStatus(p) {
			if (s.disposed || !p || typeof p !== "object" || !isStr(p.id) || p.id !== id)
				return;
			if (p.state === "live") {
				if (s.pending !== s.probeSeq)    // no pending probe: an answer to an older or expired one
					return;
				clearProbe();
				s.state = "armed";
				s.throttled = false;
				setChip("Input live", "live");
				env.armed(true);
			} else if (p.state === "denied" || p.state === "closed") {
				if (s.state === "closed")
					return;
				disarm(p.state, false);
				setChip(p.state === "denied" ? "Input not permitted" : "Job ended", "warn");
			} else if (p.state === "busy") {
				if (s.state !== "armed")
					return;
				s.throttled = true;
				setChip("Input throttled: some keys not sent", "warn");
			} else {
				return;
			}
			changed();
		}

		function onData(d) {
			if (s.disposed || s.state !== "armed" || !isStr(d) || !d)
				return;
			// A UTF-16 unit is at most 3 UTF-8 bytes, so only a possibly over-cap total needs the exact count.
			if ((s.queue.length + d.length) * 3 > MAX_QUEUE_BYTES
					&& ENC.encode(s.queue).length + ENC.encode(d).length > MAX_QUEUE_BYTES) {
				s.refused = true;
				setChip("Paste too large: not sent", "warn");
				changed();
				return;
			}
			s.refused = false;
			s.throttled = false;
			s.queue += d;
			if (s.timer !== null)
				return;
			if (ENC.encode(s.queue).length >= MAX_FRAME_BYTES) {
				sendOne();
				changed();
			} else {
				s.timer = env.setTimeout(tick, FLUSH_MS);
			}
		}

		/** xterm's custom key event handler: Esc twice within 500 ms disarms, and the second Esc is swallowed. */
		function onKeyDown(ev) {
			if (s.state !== "armed" || !ev || ev.type !== "keydown")
				return true;
			if (ev.key !== "Escape") {
				s.lastEsc = null;
				return true;
			}
			const now = env.now();
			if (s.lastEsc !== null && now - s.lastEsc <= ESC_ESC_MS) {
				disarm("idle", true);
				setChip(null);
				changed();
				return false;
			}
			s.lastEsc = now;
			return true;
		}

		function connected(on) {
			s.connected = !!on;
			if (s.connected) {
				if (s.chip === DISCONNECTED) {
					setChip(null);
					changed();
				}
				return;
			}
			if (s.state !== "armed" && s.state !== "probing")
				return;
			disarm("idle", false);
			setChip(DISCONNECTED, "warn");
			changed();
		}

		function runEnded() {
			if (s.state === "closed")
				return;
			disarm("closed", false);
			setChip("Job ended", "warn");
			changed();
		}

		return {
			press: press, onStatus: onStatus, onData: onData, onKeyDown: onKeyDown, connected: connected,
			runEnded: runEnded, view: view,
			dispose: function () {
				disarm(s.state === "armed" || s.state === "probing" ? "idle" : s.state, false);
				s.disposed = true;
			}
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
	 * The xterm constructor options: {@code base} (Juneau-owned) plus the allowlisted keys of {@code extra} with the right
	 * type.  {@code convertEol} must be a boolean, {@code scrollback} an integer from 0 to 1,000,000, {@code fontFamily} a
	 * non-empty string.  Every other key, including the Juneau-owned cols/rows/disableStdin/linkHandler/allowProposedApi/
	 * fontSize/cursorBlink, is ignored ({@code scrollback} is in {@code base} but deliberately overridable).  Returns a
	 * new object.
	 */
	function xtermOptions(base, extra) {
		const o = Object.assign({}, base);
		if (!extra || typeof extra !== "object" || Array.isArray(extra))
			return o;
		const convertEol = extra.convertEol;
		const scrollback = extra.scrollback;
		const fontFamily = extra.fontFamily;
		if (typeof convertEol === "boolean")
			o.convertEol = convertEol;
		if (typeof scrollback === "number" && Number.isSafeInteger(scrollback) && scrollback >= 0 && scrollback <= MAX_SCROLLBACK)
			o.scrollback = scrollback;
		if (isStr(fontFamily) && fontFamily)
			o.fontFamily = fontFamily;
		return o;
	}

	/**
	 * The xterm colour theme for the page's current scheme, read from the {@code --jc-term-*} tokens on the root element.
	 * A missing token falls back to xterm's own default theme (white on black), so a page without the tokens looks unchanged.
	 */
	function termTheme(doc) {
		const cs = doc.defaultView.getComputedStyle(doc.documentElement);
		function token(name, fallback) {
			const v = cs.getPropertyValue(name);
			return isStr(v) && v.trim() ? v.trim() : fallback;
		}
		const background = token("--jc-term-bg", "#000000");
		return {
			background: background,
			foreground: token("--jc-term-fg", "#ffffff"),
			cursor: token("--jc-term-cursor", "#ffffff"),
			cursorAccent: background,
			selectionBackground: token("--jc-term-selection", "rgba(255,255,255,0.3)")
		};
	}

	/**
	 * Mounts a terminal region in {@code host}.  {@code opts} is the card's terminal object plus its {@code id}:
	 * {bytesUrl, eventsUrl?, refreshMs?, title?, tailBytes?, id?, input?, inputId?, terminal?}.  {@code terminal}, optional,
	 * is an object of xterm options: {convertEol? (boolean), scrollback? (integer 0 to 1,000,000), fontFamily? (non-empty string)}.  Other keys
	 * and wrong-typed values are ignored; the size, input, link and font-size options stay Juneau's.  {@code link}, optional, is the
	 * input path: {publish(payload), onConnected(fn) -> off?}.  The input toggle appears only with input, inputId and a
	 * link.  {@code nudgeLink}, optional, is {onConnected(fn) -> off?}: the state of the bus link that carries nudges, which
	 * the card routes to the engine through {@code roles.nudge}.  Returns a cleanup that stops polling and removes the region.
	 */
	function mount(host, opts, link, nudgeLink) {
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
		const hasInput = opts.input === true && isStr(opts.inputId) && !!opts.inputId && !!link && typeof link.publish === "function";
		let toggle = null, inputChip = null;
		if (hasInput) {
			toggle = el(doc, "button", "juneau-term-input");
			toggle.type = "button";
			inputChip = el(doc, "span", "juneau-term-chip juneau-term-chip--input");
			inputChip.setAttribute("role", "status");
			inputChip.hidden = true;
			bar.append(search, copy, followLabel, reconnecting, toggle, inputChip, badge);
		} else {
			bar.append(search, copy, followLabel, reconnecting, badge);
		}
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
		let armed = false;
		let input = null;
		let offConnected = null;

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
			if (input && (st.kind === "ok" || st.kind === "fail"))
				input.runEnded();
		}

		function renderInput(v) {
			toggle.textContent = v.toggle.label;
			toggle.setAttribute("aria-pressed", v.toggle.pressed ? "true" : "false");
			toggle.disabled = v.toggle.disabled;
			if (inputChip.textContent !== (v.chip || ""))
				inputChip.textContent = v.chip || "";
			inputChip.hidden = v.chip === null;
			if (v.chipState)
				inputChip.setAttribute("data-state", v.chipState);
			else
				inputChip.removeAttribute("data-state");
		}

		if (hasInput) {
			input = createInput({ inputId: opts.inputId }, {
				publish: function (p) { link.publish(p); },
				setTimeout: win.setTimeout.bind(win),
				clearTimeout: win.clearTimeout.bind(win),
				now: function () { return Date.now(); },
				onChange: renderInput,
				armed: function (on) {
					armed = on;
					if (!term)
						return;
					term.options.disableStdin = !on;
					if (on)
						term.focus();
				}
			});
			renderInput(input.view());
			toggle.addEventListener("click", function () { input.press(); });
			if (typeof link.onConnected === "function") {
				const off = link.onConnected(function (on) { input.connected(on); });
				offConnected = typeof off === "function" ? off : null;
			}
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
				term = new win.Terminal(xtermOptions({
					cols: cols, rows: rows, scrollback: SCROLLBACK, disableStdin: !armed, allowProposedApi: false,
					fontSize: BASE_FONT, cursorBlink: false, linkHandler: linkHandler, theme: termTheme(doc)
				}, opts.terminal));
				if (input) {
					term.onData(function (d) { input.onData(d); });
					term.attachCustomKeyEventHandler(function (e) { return input.onKeyDown(e); });
				}
				term.open(screen);
				// HTT: arming before the Terminal exists needs a live answer ahead of the first bytes fetch; no harness orders them that way.
				if (armed)
					term.focus();
				fit();
				if (typeof win.ResizeObserver === "function") {
					ro = new win.ResizeObserver(fit);
					ro.observe(screen);
				}
				return term;
			},
			onStatus: render
		});

		let offNudge = null;
		if (nudgeLink && isStr(opts.id) && opts.id) {
			NUDGES.set(opts.id, engine);
			if (typeof nudgeLink.onConnected === "function") {
				const off = nudgeLink.onConnected(function (on) { engine.nudgeConnected(on); });
				offNudge = typeof off === "function" ? off : null;
			}
		}

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

		function onTheme() {
			if (term)
				term.options.theme = termTheme(doc);
		}
		doc.addEventListener("juneau:theme-changed", onTheme);

		render(engine.status());
		engine.start();
		onHash();

		const handle = Object.freeze({ scrollToOffset: engine.scrollToOffset });
		if (isStr(opts.id) && opts.id) {
			HANDLES.set(opts.id, handle);
			if (input)
				INPUTS.set(opts.id, input);
		}

		return function cleanup() {
			if (HANDLES.get(opts.id) === handle)
				HANDLES.delete(opts.id);
			if (input) {
				if (INPUTS.get(opts.id) === input)
					INPUTS.delete(opts.id);
				input.dispose();
			}
			if (offConnected)
				offConnected();
			if (offNudge)
				offNudge();
			if (NUDGES.get(opts.id) === engine)
				NUDGES.delete(opts.id);
			engine.stop();
			win.removeEventListener("hashchange", onHash);
			doc.removeEventListener("juneau:theme-changed", onTheme);
			if (ro)
				ro.disconnect();
			if (term)
				term.dispose();
			if (root.parentNode)
				root.parentNode.removeChild(root);
		};
	}

	/** Card id -> the {scrollToOffset} handle of the terminal currently mounted for it. */
	const HANDLES = new Map();

	/** Card id -> the input state machine of the terminal currently mounted for it, when that card takes input. */
	const INPUTS = new Map();

	/** Card id -> the engine of the terminal currently mounted for it, when the card subscribes to nudges. */
	const NUDGES = new Map();

	/** The handle of the terminal mounted for the card, or null when the card is not mounted (yet). */
	function handle(id) {
		return HANDLES.get(id) || null;
	}

	/** Host element -> cleanup of the region mounted in it. */
	const MOUNTED = new WeakMap();

	/** The input link for a card: publishes on term.input and watches the input bridge's state.  Null without one. */
	function cardLink(ctx) {
		if (!ctx || typeof ctx.publish !== "function" || typeof ctx.subscribe !== "function")
			return null;
		const jc = window.JuneauConsole;
		const id = linkFor(jc && typeof jc.contract === "function" ? jc.contract() : null);
		if (id === null)
			return null;
		return {
			publish: function (p) { ctx.publish(INPUT_TOPIC, p); },
			onConnected: function (fn) { return ctx.subscribe("bridge:" + id, function (st) { fn(!!st && st.state === "open"); }); }
		};
	}

	/**
	 * The bridge that carries a nudge topic: the id of the first bridge in {@code contract} whose downstream lists
	 * {@code topic} exactly.  Null when there is none.
	 */
	function nudgeLinkFor(contract, topic) {
		const bridges = contract && Array.isArray(contract.bridges) ? contract.bridges : [];
		for (const b of bridges) {
			if (b && isStr(b.id) && Array.isArray(b.downstream) && b.downstream.includes(topic))
				return b.id;
		}
		return null;
	}

	/** The nudge link for a card that subscribes to a nudge topic and whose topic a bridge carries.  Null otherwise. */
	function cardNudgeLink(card, ctx) {
		const entry = (card.subscribes || []).find(function (e) { return e && e.as === "nudge" && isStr(e.topic) && !e.topic.endsWith(":*"); });
		if (!entry || !ctx || typeof ctx.subscribe !== "function")
			return null;
		const jc = window.JuneauConsole;
		const contract = jc && typeof jc.contract === "function" ? jc.contract() : null;
		const id = nudgeLinkFor(contract, entry.topic);
		if (id === null)
			return null;
		return {
			onConnected: function (fn) { return ctx.subscribe("bridge:" + id, function (st) { fn(!!st && st.state === "open"); }); }
		};
	}

	const cardHandler = {
		render: function (card, host, ctx) {
			const prev = MOUNTED.get(host);
			if (prev)
				prev();
			const terminal = card.terminal || {};
			MOUNTED.set(host, mount(host, Object.assign({ id: card.id }, terminal), terminal.input === true ? cardLink(ctx) : null, cardNudgeLink(card, ctx)));
		},
		destroy: function (card, host) {
			const cleanup = MOUNTED.get(host);
			if (cleanup) {
				MOUNTED.delete(host);
				cleanup();
			}
		},
		// This role set must match TerminalCardType.acceptedRoles: the console only routes the roles that a card type accepts.
		roles: {
			"input-status": function (payload, meta, card) {
				const i = card ? INPUTS.get(card.id) : null;
				if (i)
					i.onStatus(payload);
			},
			nudge: function (payload, meta, card) {
				const e = card ? NUDGES.get(card.id) : null;
				if (e)
					e.nudge(payload);
			}
		}
	};

	window.JuneauTerminal = Object.freeze({
		mount: mount, createEngine: createEngine, createInput: createInput, linkFor: linkFor, nudgeLinkFor: nudgeLinkFor, fitFont: fitFont,
		findNext: findNext, parseHash: parseHash, xtermOptions: xtermOptions,
		formatSize: formatSize, linkHandlerFor: linkHandlerFor, handle: handle
	});
	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["terminal", cardHandler]);
})();
