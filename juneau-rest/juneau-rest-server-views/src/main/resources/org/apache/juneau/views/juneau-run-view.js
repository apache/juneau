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
 * juneau-run-view.js - the run-view region: a live, failures-first view of a multi-step run (steps, test suites and
 * individual tests) built from a closed, versioned event stream.  Exposes window.JuneauViews.runView and registers the
 * "run-view" region populator.  Reuses the juneau-co-* classes; builds DOM with createElement/textContent only.
 */
(function () {
	"use strict";
	const NS = window.JuneauViews;
	const LOG_TAG = "[juneau-run-view]";
	if (!NS || !NS.init) { console.error(LOG_TAG, "needs juneau-views.js loaded first"); return; }

	const CONTRACT_VERSION = "1";
	const MAX_TITLE = 200, MAX_NAME = 512, MAX_MSG = 2000, MAX_TRACE = 8000, MAX_NOTE = 1000;
	const MAX_SAFE = 9007199254740991;
	const STEP_ID = /^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/, FW = /^[a-z0-9][a-z0-9._-]{0,31}$/;
	const STEP_STATES = ["running", "waiting"], END_STATUSES = ["ok", "fail", "skip"];
	const TEST_STATUSES = ["pass", "fail", "skip", "error"], LEVELS = ["info", "warn", "error"], DONE_STATUSES = ["ok", "fail", "cancelled"];
	const KINDS = ["step", "end", "suite", "test", "replace", "note", "done"];
	const FRAGMENT_HREF = /^#[A-Za-z0-9._:~-]{0,128}$/;

	// @section:checks

	function isStr(v) { return typeof v === "string"; }
	function isPlainObject(v) { return v !== null && typeof v === "object" && !Array.isArray(v); }
	function has(o, k) { return Object.prototype.hasOwnProperty.call(o, k); }
	function isInt(v, lo, hi) { return typeof v === "number" && Number.isInteger(v) && v >= lo && v <= hi; }
	function cap(s, max) { return s.length > max ? s.slice(0, max - 1) + "…" : s; }
	function stripTabCrLf(s) { return s.replace(/[\t\r\n]/g, ""); }

	/** A pure-string same-origin path check: non-blank, no "://", no "//" prefix, no scheme colon, no ".." segment. */
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

	function isSafeLineHref(s) {
		if (!isStr(s))
			return false;
		const t = stripTabCrLf(s);
		return t.startsWith("#") ? FRAGMENT_HREF.test(t) : isSafePath(t);
	}

	function isSafeNoteHref(s) {
		if (!isStr(s))
			return false;
		const t = stripTabCrLf(s);
		if (/^https?:\/\//.test(t)) {
			if (t.includes("\\") || /^https?:\/\/[\/?#]/.test(t))
				return false;
			try {
				const u = new URL(t);
				return (u.protocol === "http:" || u.protocol === "https:") && !!u.hostname;
			} catch (e) {
				return false;
			}
		}
		return isSafeLineHref(t);
	}

	function isSafeRawHref(s) {
		if (!isStr(s))
			return false;
		const i = s.indexOf("{line}");
		if (i < 0 || s.indexOf("{line}", i + 6) >= 0)
			return false;
		return isSafeLineHref(s.replace("{line}", "1"));
	}

	/** 850 ms / 1.2 s / 42 s / 4 m 05 s / 1 h 02 m; non-numbers and negatives give "". */
	function formatDuration(ms) {
		if (typeof ms !== "number" || !isFinite(ms) || ms < 0)
			return "";
		if (ms < 1000)
			return Math.floor(ms) + " ms";
		if (ms < 10000) {
			const tenths = Math.floor(ms / 100);
			return Math.floor(tenths / 10) + "." + (tenths % 10) + " s";
		}
		if (ms < 60000)
			return Math.floor(ms / 1000) + " s";
		const sec = Math.floor(ms / 1000);
		if (ms < 3600000)
			return Math.floor(sec / 60) + " m " + String(sec % 60).padStart(2, "0") + " s";
		return Math.floor(sec / 3600) + " h " + String(Math.floor((sec % 3600) / 60)).padStart(2, "0") + " m";
	}

	/**
	 * Validates one raw event.  Returns {event, problems, ignored}: event is a fresh plain object with only known
	 * members (or null when dropped/ignored); problems are {code:'E-RV-3', detail} for a dropped malformed event and
	 * {code:'E-RV-7', value} for an unsafe note href (the note is kept without it); ignored is true for an unknown ev.
	 */
	function validateEvent(raw) {
		const problems = [];
		function drop(detail) { return { event: null, problems: [{ code: "E-RV-3", detail: detail }], ignored: false }; }
		if (!isPlainObject(raw))
			return drop("event is not an object");
		if (!isStr(raw.ev))
			return drop("missing ev");
		if (KINDS.indexOf(raw.ev) < 0)
			return { event: null, problems: [], ignored: true };
		const ev = { ev: raw.ev };
		if (has(raw, "seq")) {
			if (!isInt(raw.seq, 1, MAX_SAFE))
				return drop(raw.ev + ": bad seq");
			ev.seq = raw.seq;
		}
		let bad = null;
		function fail(m) { if (!bad) bad = raw.ev + ": " + m; }
		function req(k, max, grammar) {
			const v = raw[k];
			if (!isStr(v) || v.length === 0) { fail(k + " must be a non-empty string"); return; }
			if (grammar) { if (!grammar.test(v)) fail(k + " is malformed"); else ev[k] = v; return; }
			ev[k] = cap(v, max);
		}
		function opt(k, max) {
			if (!has(raw, k) || raw[k] === undefined) return;
			if (!isStr(raw[k])) { fail(k + " must be a string"); return; }
			ev[k] = cap(raw[k], max);
		}
		function optInt(k, lo, hi) {
			if (!has(raw, k) || raw[k] === undefined) return;
			if (!isInt(raw[k], lo, hi)) { fail(k + " out of range"); return; }
			ev[k] = raw[k];
		}
		function oneOf(k, list, required) {
			if (!has(raw, k) || raw[k] === undefined) { if (required) fail(k + " is required"); return; }
			if (list.indexOf(raw[k]) < 0) { fail(k + " is not allowed"); return; }
			ev[k] = raw[k];
		}
		function result() {
			req("step", 0, STEP_ID); req("fw", 0, FW); req("suite", MAX_NAME);
		}
		switch (raw.ev) {
			case "step":
				req("id", 0, STEP_ID); req("title", MAX_TITLE); optInt("n", 1, 9999); oneOf("status", STEP_STATES, false); optInt("rawLine", 1, MAX_SAFE);
				break;
			case "end":
				req("id", 0, STEP_ID); oneOf("status", END_STATUSES, true); optInt("ms", 0, MAX_SAFE); optInt("exit", -MAX_SAFE, MAX_SAFE);
				break;
			case "suite": {
				result();
				const c = raw.counts;
				if (!isPlainObject(c)) { fail("counts is required"); break; }
				const counts = {};
				for (const k of ["pass", "fail", "skip"]) {
					if (!isInt(c[k], 0, 1000000000)) { fail("counts." + k + " out of range"); break; }
					counts[k] = c[k];
				}
				ev.counts = counts;
				optInt("rawLine", 1, MAX_SAFE);
				break;
			}
			case "test":
				result(); req("name", MAX_NAME); oneOf("status", TEST_STATUSES, true); optInt("ms", 0, MAX_SAFE);
				opt("msg", MAX_MSG); opt("trace", MAX_TRACE); optInt("rawLine", 1, MAX_SAFE);
				break;
			case "replace":
				req("step", 0, STEP_ID);
				break;
			case "note":
				oneOf("level", LEVELS, true); req("text", MAX_NOTE);
				if (has(raw, "step") && raw.step !== undefined) {
					if (isStr(raw.step) && STEP_ID.test(raw.step)) ev.step = raw.step; else fail("step is malformed");
				}
				if (has(raw, "href") && raw.href !== undefined) {
					if (isSafeNoteHref(raw.href)) ev.href = raw.href;
					else problems.push({ code: "E-RV-7", value: String(raw.href) });
				}
				break;
			case "done":
				oneOf("status", DONE_STATUSES, true);
				break;
		}
		if (bad)
			return drop(bad);
		return { event: ev, problems: problems, ignored: false };
	}

	// @section:reducer

	const MAX_STEPS = 1000, MAX_SUITES = 5000, MAX_OK_RECORDS = 20000, MAX_BAD_RECORDS = 5000, MAX_NOTES = 1000;
	const SEP = "\u0000";

	function newModel() {
		return { lastSeq: 0, steps: [], stepIndex: new Map(), notes: [], runNotes: [], done: null, terminal: false, fwOrder: [],
			retainedOk: 0, retainedBad: 0, runDirty: true,
			stats: { applied: 0, skipped: 0, dropped: 0, ignored: 0, droppedTests: 0 }, warned: new Set() };
	}

	function newStep(ev) {
		return { id: ev.id, title: ev.title, n: ev.n, status: ev.status || "running", ended: false, ms: undefined, exit: undefined,
			rawLine: ev.rawLine, suites: new Map(), suiteOrder: [], notes: [], dirty: true };
	}

	/**
	 * Applies one validated event.  Returns {applied, problems}; problems are E-RV-6 / E-RV-9 entries, each reported
	 * once per distinct case (de-duplicated through model.warned).
	 */
	function reduce(model, ev) {
		const out = { applied: false, problems: [] };
		let seq = ev.seq;
		if (seq === undefined)
			seq = model.lastSeq + 1;
		else if (seq <= model.lastSeq) {
			model.stats.skipped++;
			return out;
		}
		model.lastSeq = seq;

		function drop(code, key, detail) {
			model.stats.dropped++;
			if (!model.warned.has(key)) {
				model.warned.add(key);
				out.problems.push({ code: code, detail: detail });
			}
			return out;
		}
		function warnOnly(code, key, detail) {
			if (!model.warned.has(key)) {
				model.warned.add(key);
				out.problems.push({ code: code, detail: detail });
			}
		}
		function unknownStep(id) { return drop("E-RV-6", "E-RV-6:unknown step:" + id, ev.ev + " for unknown step " + id); }
		function done() { out.applied = true; model.stats.applied++; return out; }

		// The suite for an event's (fw, suite) key under a step; null when the suites-per-step cap drops it.
		function suiteFor(step) {
			const key = ev.fw + SEP + ev.suite;
			let s = step.suites.get(key);
			if (s)
				return s;
			if (step.suites.size >= MAX_SUITES) {
				drop("E-RV-9", "E-RV-9:suites:" + step.id, "step " + step.id + " holds more than " + MAX_SUITES + " suites; further suites are not shown");
				return null;
			}
			s = { fw: ev.fw, suite: ev.suite, pass: 0, fail: 0, skip: 0, detailed: false, tests: [], rawLine: undefined, dropped: 0, ver: 0 };
			step.suites.set(key, s);
			step.suiteOrder.push(key);
			if (model.fwOrder.indexOf(ev.fw) < 0)
				model.fwOrder.push(ev.fw);
			return s;
		}

		switch (ev.ev) {
			case "step": {
				let st = model.stepIndex.get(ev.id);
				if (!st) {
					if (model.steps.length >= MAX_STEPS)
						return drop("E-RV-9", "E-RV-9:steps", "more than " + MAX_STEPS + " steps; further steps are not shown");
					st = newStep(ev);
					model.steps.push(st);
					model.stepIndex.set(ev.id, st);
				} else if (st.ended) {
					return drop("E-RV-6", "E-RV-6:ended step:" + ev.id, "step event for ended step " + ev.id);
				} else {
					st.title = ev.title;
					if (ev.n !== undefined) st.n = ev.n;
					if (ev.status !== undefined) st.status = ev.status;
					if (ev.rawLine !== undefined) st.rawLine = ev.rawLine;
					st.dirty = true;
				}
				return done();
			}
			case "end": {
				const st = model.stepIndex.get(ev.id);
				if (!st)
					return unknownStep(ev.id);
				st.status = ev.status;
				st.ended = true;
				st.ms = ev.ms;
				st.exit = ev.exit;
				st.dirty = true;
				return done();
			}
			case "suite": {
				const st = model.stepIndex.get(ev.step);
				if (!st)
					return unknownStep(ev.step);
				const s = suiteFor(st);
				if (!s)
					return out;
				if (s.detailed) {
					model.stats.ignored++;
					return out;
				}
				s.pass = ev.counts.pass;
				s.fail = ev.counts.fail;
				s.skip = ev.counts.skip;
				s.rawLine = ev.rawLine;
				s.ver++;
				st.dirty = true;
				return done();
			}
			case "test": {
				const st = model.stepIndex.get(ev.step);
				if (!st)
					return unknownStep(ev.step);
				const s = suiteFor(st);
				if (!s)
					return out;
				if (!s.detailed) {
					s.detailed = true;
					s.pass = s.fail = s.skip = 0;
				}
				if (ev.status === "pass") s.pass++;
				else if (ev.status === "skip") s.skip++;
				else s.fail++;
				const bad = ev.status === "fail" || ev.status === "error";
				if (bad ? model.retainedBad < MAX_BAD_RECORDS : model.retainedOk < MAX_OK_RECORDS) {
					const rec = { name: ev.name, status: ev.status };
					for (const k of ["ms", "msg", "trace", "rawLine"])
						if (ev[k] !== undefined) rec[k] = ev[k];
					s.tests.push(rec);
					if (bad) model.retainedBad++; else model.retainedOk++;
				} else {
					s.dropped++;
					model.stats.droppedTests++;
				}
				s.ver++;
				st.dirty = true;
				return done();
			}
			case "replace": {
				const st = model.stepIndex.get(ev.step);
				if (!st)
					return unknownStep(ev.step);
				for (const s of st.suites.values()) {
					for (const r of s.tests) {
						if (r.status === "fail" || r.status === "error") model.retainedBad--; else model.retainedOk--;
					}
					model.stats.droppedTests -= s.dropped;
				}
				st.suites = new Map();
				st.suiteOrder = [];
				st.dirty = true;
				return done();
			}
			case "note": {
				if (model.notes.length >= MAX_NOTES)
					return drop("E-RV-9", "E-RV-9:notes", "more than " + MAX_NOTES + " notes; further notes are not shown");
				const rec = { level: ev.level, text: ev.text };
				if (ev.href !== undefined) rec.href = ev.href;
				const st = ev.step === undefined ? null : model.stepIndex.get(ev.step);
				if (st) {
					rec.step = ev.step;
					st.notes.push(rec);
					st.dirty = true;
				} else {
					if (ev.step !== undefined)
						warnOnly("E-RV-6", "E-RV-6:note for unknown step:" + ev.step, "note for unknown step " + ev.step + "; shown under the run");
					model.runNotes.push(rec);
				}
				model.notes.push(rec);
				model.runDirty = true;
				return done();
			}
			case "done":
				model.done = ev.status;
				model.runDirty = true;
				return done();
		}
		model.stats.ignored++;
		return out;
	}

	/**
	 * The derived view of a model: run status and headline, each step's shown state (open steps close to fail/skip once a
	 * done exists), exact per-framework counts and the failures list (step order, then suite order, then arrival order).
	 */
	function derive(model) {
		const shown = model.steps.map(function (st) {
			const open = st.status === "running" || st.status === "waiting";
			return { step: st, shown: open && model.done !== null ? (model.done === "fail" ? "fail" : "skip") : st.status };
		});
		let status, headline, glyph;
		if (model.done !== null) {
			status = model.done;
			if (status === "ok") { headline = "Passed"; glyph = "✓"; }
			else if (status === "cancelled") { headline = "Cancelled"; glyph = "○"; }
			else {
				const f = shown.find(function (x) { return x.shown === "fail"; });
				headline = f ? "Failed at " + f.step.title : "Failed";
				glyph = "✗";
			}
		} else if (model.terminal) {
			status = "stopped"; headline = "Stopped"; glyph = "⏸";
		} else if (model.stats.applied === 0) {
			status = "empty"; headline = "Waiting for events…"; glyph = "⋯";
		} else {
			const running = model.steps.some(function (st) { return st.status === "running"; });
			const waiting = model.steps.find(function (st) { return st.status === "waiting"; });
			if (waiting && !running) { status = "waiting"; headline = "Waiting at " + waiting.title; glyph = "⏸"; }
			else { status = "running"; headline = "Running"; glyph = "⋯"; }
		}
		const byFw = new Map();
		const failures = [];
		for (const st of model.steps) {
			for (const key of st.suiteOrder) {
				const s = st.suites.get(key);
				let c = byFw.get(s.fw);
				if (!c) { c = { fw: s.fw, pass: 0, fail: 0, skip: 0 }; byFw.set(s.fw, c); }
				c.pass += s.pass; c.fail += s.fail; c.skip += s.skip;
				if (s.detailed) {
					for (const r of s.tests)
						if (r.status === "fail" || r.status === "error") failures.push({ step: st, suite: s, test: r });
				} else if (s.fail > 0) {
					failures.push({ step: st, suite: s, test: null });
				}
			}
		}
		const counts = model.fwOrder.filter(function (fw) { return byFw.has(fw); }).map(function (fw) { return byFw.get(fw); });
		return { status: status, headline: headline, glyph: glyph, steps: shown, counts: counts, failures: failures };
	}

	// @section:render

	const MAX_FAILURES_FULL = 50, MAX_FAILURES_COMPACT = 3, MAX_FAILURE_MSG = 200;
	const MAX_BLOCKS = 300, MAX_SUITE_ROWS = 2000;
	const FW_LABELS = { "surefire": "Surefire", "junit-xml": "JUnit", "pytest": "pytest", "jest": "Jest", "playwright": "Playwright" };
	const GLYPH = { ok: "✓", fail: "✗", skip: "○", running: "⋯", waiting: "⏸" };
	const SR = { ok: "passed", fail: "failed", skip: "skipped", running: "running", waiting: "waiting" };
	const NOTE_GLYPH = { info: "ⓘ", warn: "⚠", error: "✗" };
	const NOTE_SR = { info: "info", warn: "warning", error: "error" };
	const ATTR_ALLOWED = /^(class|role|aria-[a-z]+|data-juneau-[a-z-]+|data-step|data-state|tabindex|href|rel|target|type|hidden|id)$/;
	const TIP_MARGIN = 4;

	/** Creates an element; the class and text are assigned as properties, never parsed as markup. */
	function mk(tag, cls, text) {
		const e = document.createElement(tag);
		if (cls)
			e.className = cls;
		if (text !== undefined)
			e.textContent = text;
		return e;
	}

	/** The one place an attribute is set: names outside the allowlist are refused. */
	function attr(e, name, value) {
		const n = String(name).toLowerCase();
		if (!ATTR_ALLOWED.test(n))
			return;
		e.setAttribute(n, String(value));
	}

	function fwLabel(fw) { return has(FW_LABELS, fw) ? FW_LABELS[fw] : fw; }

	function countsText(c, always) {
		let s = c.pass + " ✓";
		if (c.fail > 0 || always) s += " " + c.fail + " ✗";
		if (c.skip > 0 || always) s += " " + c.skip + " ○";
		return s;
	}

	function firstLine(s) {
		if (!isStr(s)) return "";
		const i = s.indexOf("\n");
		return (i < 0 ? s : s.slice(0, i)).trim();
	}

	function rawLink(inst, line) {
		return inst.rawHref && line !== undefined ? stripTabCrLf(inst.rawHref).replace("{line}", String(line)) : null;
	}

	function relFor(href) { return href.startsWith("#") ? null : "nofollow noreferrer"; }

	/** The text of a block's tooltip: name, "status . duration", first line of the message; empty lines omitted. */
	function blockTip(r) {
		const d = formatDuration(r.ms);
		const lines = [r.name, r.status + (d ? " · " + d : ""), firstLine(r.msg)];
		return lines.filter(function (l) { return l.length > 0; }).join("\n");
	}

	function blockLabel(r) {
		const d = formatDuration(r.ms);
		return r.name + ", " + r.status + (d ? ", " + d : "");
	}

	function buildBlock(inst, st, s, r) {
		const href = rawLink(inst, r.rawLine);
		const b = href ? mk("a", "juneau-co-block") : mk("span", "juneau-co-block");
		b.className = "juneau-co-block " + (r.status === "pass" ? "juneau-co-fill-success"
			: r.status === "skip" ? "juneau-co-block-empty" : "juneau-co-fill-error");
		attr(b, "data-juneau-co-tip", blockTip(r));
		attr(b, "aria-label", blockLabel(r));
		if (href) {
			attr(b, "href", href);
			const rel = relFor(href);
			if (rel) attr(b, "rel", rel);
		} else {
			attr(b, "tabindex", "0");
			attr(b, "role", "img");
		}
		b.__rv = { step: st, suite: s, test: r, link: !!href };
		return b;
	}

	/** Which retained tests get a block: all of them up to MAX_BLOCKS, else non-pass first, kept in arrival order. */
	function shownTests(s) {
		const t = s.tests;
		if (t.length <= MAX_BLOCKS)
			return t;
		const pick = new Set();
		for (let i = 0; i < t.length && pick.size < MAX_BLOCKS; i++)
			if (t[i].status !== "pass") pick.add(i);
		for (let i = 0; i < t.length && pick.size < MAX_BLOCKS; i++)
			if (!pick.has(i)) pick.add(i);
		return t.filter(function (r, i) { return pick.has(i); });
	}

	function ekeyOf(st, s) { return st.id + "/" + s.fw + "/" + s.suite; }

	function isExpanded(inst, st, s) {
		if (!s.detailed)
			return false;
		const v = inst.expanded.get(ekeyOf(st, s));
		return v === undefined ? s.fail > 0 : v;
	}

	function buildTrace(r) {
		const d = mk("details", "juneau-rv-trace");
		d.open = true;
		attr(d, "data-juneau-rv-trace", "1");
		const sum = mk("summary", null, r.name);
		d.appendChild(sum);
		if (r.msg) d.appendChild(mk("pre", "juneau-rv-trace-msg", r.msg));
		if (r.trace) d.appendChild(mk("pre", "juneau-rv-trace-text", r.trace));
		return d;
	}

	function buildSuiteRow(inst, st, s, open) {
		const ek = ekeyOf(st, s);
		const row = mk("div", "juneau-rv-suite");
		attr(row, "data-juneau-rv-key", ek);
		attr(row, "tabindex", "-1");
		const expanded = isExpanded(inst, st, s);
		if (s.detailed) {
			const btn = mk("button", "juneau-rv-suite-toggle", s.suite);
			attr(btn, "type", "button");
			attr(btn, "aria-expanded", expanded ? "true" : "false");
			attr(btn, "data-juneau-rv-act", "toggle-suite");
			attr(btn, "data-juneau-rv-key", ek);
			row.appendChild(btn);
		} else {
			row.appendChild(mk("span", "juneau-rv-suite-name", s.suite));
		}
		const sumText = s.detailed ? countsText(s, false) + (expanded ? " ▾" : " ▸") : countsText(s, true);
		row.appendChild(mk("span", "juneau-rv-suite-sum", sumText));
		if (!s.detailed && open)
			row.appendChild(mk("span", "juneau-rv-running", "running"));
		if (!expanded)
			return row;
		const strip = mk("div", "juneau-rv-strip");
		const shown = shownTests(s);
		for (const r of shown)
			strip.appendChild(buildBlock(inst, st, s, r));
		row.appendChild(strip);
		if (s.tests.length > shown.length)
			row.appendChild(mk("span", "juneau-rv-more", "+" + (s.tests.length - shown.length) + " more"));
		if (s.dropped > 0)
			row.appendChild(mk("div", "juneau-rv-more", "…and " + s.dropped + " more tests not shown"));
		const tr = inst.trace.get(ek);
		if (tr && s.tests.indexOf(tr) >= 0)
			row.appendChild(buildTrace(tr));
		return row;
	}

	function buildNote(n) {
		const li = mk("li", "juneau-rv-note juneau-rv-note-" + n.level);
		const g = mk("span", "juneau-rv-note-glyph", NOTE_GLYPH[n.level]);
		attr(g, "aria-hidden", "true");
		li.appendChild(g);
		li.appendChild(mk("span", "juneau-co-sr", NOTE_SR[n.level] + ": "));
		if (n.href !== undefined) {
			const h = stripTabCrLf(n.href);
			const a = mk("a", null, n.text);
			attr(a, "href", h);
			if (/^https?:\/\//.test(h)) {
				attr(a, "target", "_blank");
				attr(a, "rel", "noopener noreferrer");
			}
			li.appendChild(a);
		} else {
			li.appendChild(mk("span", null, n.text));
		}
		return li;
	}

	function renderNotes(ul, notes) {
		ul.replaceChildren();
		for (const n of notes)
			ul.appendChild(buildNote(n));
		ul.hidden = notes.length === 0;
	}

	function buildSummaryLine(inst, st, clean, expanded) {
		const row = mk("div", "juneau-rv-suite juneau-rv-clean-line");
		const pass = clean.reduce(function (a, s) { return a + s.pass; }, 0);
		const btn = mk("button", "juneau-rv-suite-toggle", clean.length + (clean.length === 1 ? " suite" : " suites"));
		attr(btn, "type", "button");
		attr(btn, "aria-expanded", expanded ? "true" : "false");
		attr(btn, "data-juneau-rv-act", "toggle-clean");
		attr(btn, "data-juneau-rv-key", st.id);
		row.appendChild(btn);
		row.appendChild(mk("span", "juneau-rv-suite-sum", " · " + pass + " ✓" + (expanded ? " ▾" : " ▸")));
		return row;
	}

	function suiteSig(inst, st, s, open) {
		return [s.ver, isExpanded(inst, st, s) ? 1 : 0, open ? 1 : 0, s.tests.indexOf(inst.trace.get(ekeyOf(st, s)))].join("|");
	}

	/** The suite row for (step, suite): reused when nothing about it changed, rebuilt otherwise. */
	function suiteRow(inst, rec, st, key, s, open) {
		const sig = suiteSig(inst, st, s, open);
		const old = rec.rows.get(key);
		if (old && old.sig === sig)
			return old.node;
		const node = buildSuiteRow(inst, st, s, open);
		rec.rows.set(key, { sig: sig, node: node });
		return node;
	}

	function renderSuites(inst, rec, st, open) {
		const nodes = [];
		const keys = st.suiteOrder.slice(0, MAX_SUITE_ROWS);
		const seen = new Set(keys);
		for (const k of Array.from(rec.rows.keys()))
			if (!seen.has(k)) rec.rows.delete(k);
		if (inst.compact) {
			const clean = [], cleanKeys = [];
			for (const k of keys) {
				const s = st.suites.get(k);
				if (s.fail > 0) nodes.push(suiteRow(inst, rec, st, k, s, open));
				else { clean.push(s); cleanKeys.push(k); }
			}
			if (clean.length > 0) {
				const exp = inst.expanded.get(st.id + "/*clean") === true;
				const line = buildSummaryLine(inst, st, clean, exp);
				nodes.unshift(line);
				if (exp) {
					const at = [];
					clean.forEach(function (s, i) { at.push(suiteRow(inst, rec, st, cleanKeys[i], s, open)); });
					nodes.splice.apply(nodes, [1, 0].concat(at));
				}
			}
		} else {
			for (const k of keys)
				nodes.push(suiteRow(inst, rec, st, k, st.suites.get(k), open));
		}
		if (st.suiteOrder.length > MAX_SUITE_ROWS)
			nodes.push(mk("div", "juneau-rv-more", "…" + (st.suiteOrder.length - MAX_SUITE_ROWS) + " more suites"));
		rec.suitesEl.replaceChildren();
		for (const n of nodes)
			rec.suitesEl.appendChild(n);
	}

	function renderHead(inst, rec, st, shown) {
		const head = rec.head;
		head.replaceChildren();
		const g = mk("span", "juneau-rv-glyph juneau-rv-state-" + shown, GLYPH[shown]);
		attr(g, "aria-hidden", "true");
		head.appendChild(g);
		head.appendChild(mk("span", "juneau-co-sr", SR[shown] + ": "));
		const label = (st.n !== undefined ? st.n + ". " : "") + st.title;
		const href = rawLink(inst, st.rawLine);
		if (href) {
			const a = mk("a", "juneau-rv-title", label);
			attr(a, "href", href);
			const rel = relFor(href);
			if (rel) attr(a, "rel", rel);
			head.appendChild(a);
		} else {
			head.appendChild(mk("span", "juneau-rv-title", label));
		}
		const d = formatDuration(st.ms);
		if (d) head.appendChild(mk("span", "juneau-rv-dur", d));
		if (st.exit !== undefined) {
			attr(head, "data-juneau-co-tip", "exit " + st.exit);
			attr(head, "tabindex", "0");
		} else {
			head.removeAttribute("data-juneau-co-tip");
			head.removeAttribute("tabindex");
		}
	}

	function renderStep(inst, st, shown) {
		let rec = inst.recs.get(st.id);
		if (!rec) {
			const li = mk("li", "juneau-rv-step");
			attr(li, "data-step", st.id);
			const head = mk("div", "juneau-rv-step-head");
			const suitesEl = mk("div", "juneau-rv-suites");
			const notesEl = mk("ul", "juneau-rv-notes");
			li.appendChild(head);
			li.appendChild(suitesEl);
			li.appendChild(notesEl);
			inst.stepsEl.appendChild(li);
			rec = { li: li, head: head, suitesEl: suitesEl, notesEl: notesEl, rows: new Map() };
			inst.recs.set(st.id, rec);
		}
		attr(rec.li, "data-state", shown);
		renderHead(inst, rec, st, shown);
		renderSuites(inst, rec, st, !st.ended && inst.model.done === null);
		renderNotes(rec.notesEl, st.notes);
		st.dirty = false;
	}

	function failureText(f) {
		if (f.test === null)
			return f.suite.suite + ": " + f.suite.fail + " failed";
		const m = firstLine(f.test.msg);
		return f.suite.suite + " › " + f.test.name + (m ? " – " + cap(m, MAX_FAILURE_MSG) : "");
	}

	function renderSummary(inst, d) {
		attr(inst.root, "data-juneau-rv-status", d.status);
		inst.headline.textContent = d.glyph + " " + d.headline;
		if (d.counts.length === 0) {
			inst.countsEl.textContent = "";
			inst.countsEl.hidden = true;
		} else {
			inst.countsEl.hidden = false;
			inst.countsEl.textContent = d.counts.map(function (c) { return fwLabel(c.fw) + " " + countsText(c, false); }).join(" · ");
		}
		const limit = inst.compact ? MAX_FAILURES_COMPACT : MAX_FAILURES_FULL;
		inst.failuresEl.replaceChildren();
		d.failures.slice(0, limit).forEach(function (f) {
			const li = mk("li", "juneau-rv-failure-item");
			const b = mk("button", "juneau-rv-failure", failureText(f));
			attr(b, "type", "button");
			attr(b, "data-juneau-rv-act", "failure");
			b.__rv = { step: f.step, suite: f.suite, test: f.test };
			li.appendChild(b);
			if (f.test && f.test.trace) {
				const tb = mk("button", "juneau-rv-failure-trace", "details");
				attr(tb, "type", "button");
				attr(tb, "aria-label", "Show details for " + f.test.name);
				attr(tb, "data-juneau-rv-act", "failure-trace");
				tb.__rv = b.__rv;
				li.appendChild(tb);
			}
			inst.failuresEl.appendChild(li);
		});
		if (d.failures.length > limit)
			inst.failuresEl.appendChild(mk("li", "juneau-rv-more", "…and " + (d.failures.length - limit) + " more"));
		inst.failuresEl.hidden = d.failures.length === 0;
	}

	function emitState(inst, d) {
		if (d.status === inst.lastStatus)
			return;
		inst.lastStatus = d.status;
		emitBus(inst, { kind: "run-view.state", id: inst.id, state: inst.errored ? "error" : d.status, terminal: inst.model.terminal });
	}

	function renderNow(inst) {
		if (inst.destroyed)
			return;
		if (inst.timer !== null) {
			clearTimeout(inst.timer);
			inst.timer = null;
		}
		inst.renders++;
		const m = inst.model;
		const d = derive(m);
		renderSummary(inst, d);
		const closing = inst.lastDone !== m.done;
		inst.lastDone = m.done;
		for (const x of d.steps)
			if (x.step.dirty || closing || !inst.recs.has(x.step.id))
				renderStep(inst, x.step, x.shown);
		if (m.runDirty) {
			renderNotes(inst.runNotesEl, m.runNotes);
			m.runDirty = false;
		}
		emitState(inst, d);
	}

	// Tooltip (modelled on the console-output module's, which does not export it)
	function tipBlock(inst, target) {
		const b = target && typeof target.closest === "function" ? target.closest("[data-juneau-co-tip]") : null;
		return b && inst.root.contains(b) ? b : null;
	}

	function viewport() {
		const de = document.documentElement || {};
		return { w: window.innerWidth || de.clientWidth || 0, h: window.innerHeight || de.clientHeight || 0 };
	}

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

	// @section:instance
	const MSG = {
		"E-RV-1": "run-view '%s': %s",
		"E-RV-2": "run-view '%s': response contractVersion '%s' is not '1'",
		"E-RV-3": "run-view '%s': %s",
		"E-RV-4": "run-view '%s': fetching events failed (%s)",
		"E-RV-5": "run-view '%s': invalid continuation token or non-increasing event numbers",
		"E-RV-8": "run-view '%s': the server restarted the event stream (410 Gone); reloading from the start",
		"E-RV-6": "run-view '%s': %s",
		"E-RV-7": "run-view '%s': unsafe note href removed (%s)",
		"E-RV-9": "run-view '%s': %s"
	};
	const REG_EL = new Map(), REG_ID = new Map();
	const INSTANCES = new WeakMap();

	function message(code, args) {
		let i = 0;
		return MSG[code].replace(/%[sd]/g, function () {
			const v = args[i++];
			return v === undefined ? "" : String(v);
		});
	}

	class JuneauRunViewError extends Error {
		constructor(code, msg) {
			super(msg);
			this.name = "JuneauRunViewError";
			this.code = code;
		}
	}

	function configError(id, detail) {
		const m = message("E-RV-1", [id, detail]);
		console.error(LOG_TAG, "E-RV-1", m);
		return new JuneauRunViewError("E-RV-1", m);
	}

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

	function checkOptions(el, options) {
		const o = isPlainObject(options) ? options : {};
		const id = o.id === undefined ? ((el && el.id) || "run") : o.id;
		if (!isStr(id) || id.length === 0)
			throw configError("run", "id must be a non-empty string");
		if (!el || typeof el.appendChild !== "function")
			throw configError(id, "create() needs a container element");
		if (o.compact !== undefined && typeof o.compact !== "boolean")
			throw configError(id, "compact must be a boolean");
		if (o.title !== undefined && (!isStr(o.title) || o.title.length > MAX_TITLE))
			throw configError(id, "title must be a string of at most " + MAX_TITLE + " characters");
		if (o.rawHref !== undefined && !isSafeRawHref(o.rawHref))
			throw configError(id, "rawHref must contain one {line} and be a safe same-origin link template");
		if (o.emit !== undefined && typeof o.emit !== "function")
			throw configError(id, "emit must be a function");
		return { id: id, compact: o.compact === true, title: o.title || "Run", rawHref: o.rawHref, emit: o.emit };
	}

	function listen(inst, target, type, fn) {
		target.addEventListener(type, fn);
		inst.listeners.push([target, type, fn]);
	}

	function scheduleRender(inst) {
		if (inst.destroyed || inst.timer !== null)
			return;
		inst.timer = setTimeout(function () {
			inst.timer = null;
			renderNow(inst);
		}, 0);
	}

	function focusFor(inst, info) {
		const rec = inst.recs.get(info.step.id);
		const row = rec && rec.rows.get(info.step.suiteOrder.find(function (k) { return info.step.suites.get(k) === info.suite; }));
		if (!row)
			return;
		let target = row.node;
		if (info.test) {
			const hit = row.node.querySelectorAll(".juneau-co-block").find(function (b) { return b.__rv && b.__rv.test === info.test; });
			if (hit) target = hit;
		}
		if (typeof target.focus === "function") target.focus();
		if (typeof target.scrollIntoView === "function") target.scrollIntoView({ block: "center" });
	}

	function revealFailure(inst, info, withTrace) {
		const ek = ekeyOf(info.step, info.suite);
		inst.expanded.set(ek, true);
		if (withTrace && info.test) inst.trace.set(ek, info.test);
		info.step.dirty = true;
		renderNow(inst);
		focusFor(inst, info);
	}

	function toggleTrace(inst, b) {
		const info = b.__rv;
		if (!info || !info.test.trace && !info.test.msg || (info.test.status !== "fail" && info.test.status !== "error"))
			return false;
		const ek = ekeyOf(info.step, info.suite);
		if (inst.trace.get(ek) === info.test) inst.trace.delete(ek); else inst.trace.set(ek, info.test);
		info.step.dirty = true;
		renderNow(inst);
		return true;
	}

	function onClick(inst, ev) {
		const t = ev.target;
		if (!t || typeof t.closest !== "function")
			return;
		const act = t.closest("[data-juneau-rv-act]");
		if (act) {
			const a = act.getAttribute("data-juneau-rv-act");
			if (a === "failure" || a === "failure-trace") {
				revealFailure(inst, act.__rv, a === "failure-trace");
				return;
			}
			const key = act.getAttribute("data-juneau-rv-key");
			const stepId = a === "toggle-clean" ? key : key.slice(0, key.indexOf("/"));
			const st = inst.model.stepIndex.get(stepId);
			const ek = a === "toggle-clean" ? key + "/*clean" : key;
			const cur = a === "toggle-clean" ? inst.expanded.get(ek) === true : act.getAttribute("aria-expanded") === "true";
			inst.expanded.set(ek, !cur);
			if (st) st.dirty = true;
			renderNow(inst);
			return;
		}
		const blk = t.closest(".juneau-co-block");
		if (blk && blk.__rv && !blk.__rv.link)
			toggleTrace(inst, blk);
	}

	function onKeydown(inst, ev) {
		if (ev.key === "Escape" && !inst.tipEl.hidden) {
			hideTip(inst);
			return;
		}
		if (ev.key !== "Enter" && ev.key !== " ")
			return;
		const t = ev.target;
		const blk = t && typeof t.closest === "function" ? t.closest(".juneau-co-block") : null;
		if (blk && blk.__rv && !blk.__rv.link && toggleTrace(inst, blk) && typeof ev.preventDefault === "function")
			ev.preventDefault();
	}

	function wire(inst) {
		const root = inst.root;
		listen(inst, root, "click", function (ev) { onClick(inst, ev); });
		listen(inst, root, "keydown", function (ev) { onKeydown(inst, ev); });
		const show = function (ev) {
			const b = tipBlock(inst, ev.target);
			if (b)
				showTip(inst, b);
		};
		listen(inst, root, "mouseover", show);
		listen(inst, root, "focusin", show);
		listen(inst, root, "mouseout", function (ev) {
			const b = tipBlock(inst, ev.target);
			if (b && b === inst.tipFor && !(ev.relatedTarget && b.contains(ev.relatedTarget)))
				hideTip(inst);
		});
		listen(inst, root, "focusout", function (ev) {
			if (tipBlock(inst, ev.target) === inst.tipFor)
				hideTip(inst);
		});
	}

	function snapshot(inst) {
		const d = derive(inst.model);
		return {
			status: d.status,
			steps: d.steps.map(function (x) {
				const st = x.step;
				return { id: st.id, title: st.title, n: st.n, status: x.shown, ms: st.ms, exit: st.exit, rawLine: st.rawLine,
					suites: st.suiteOrder.map(function (k) {
						const s = st.suites.get(k);
						return { fw: s.fw, suite: s.suite, pass: s.pass, fail: s.fail, skip: s.skip, detailed: s.detailed };
					}) };
			}),
			notes: inst.model.notes.map(function (n) { return Object.assign({}, n); }),
			counts: d.counts.map(function (c) { return Object.assign({}, c); })
		};
	}

	function append(inst, input) {
		const counts = { applied: 0, skipped: 0, dropped: 0 };
		if (inst.destroyed)
			return counts;
		const m = inst.model;
		const list = Array.isArray(input) ? input : [input];
		const details = [];
		for (const raw of list) {
			const v = validateEvent(raw);
			if (v.ignored) {
				m.stats.ignored++;
				continue;
			}
			for (const p of v.problems) {
				if (p.code === "E-RV-7") warn(inst, "E-RV-7", [inst.id, p.value], "E-RV-7:" + p.value);
				else details.push(p.detail);
			}
			if (!v.event) {
				counts.dropped++;
				m.stats.dropped++;
				continue;
			}
			const s0 = m.stats.skipped, d0 = m.stats.dropped;
			const r = reduce(m, v.event);
			for (const p of r.problems)
				warn(inst, p.code, [inst.id, p.detail], p.code + ":" + p.detail);
			if (r.applied) counts.applied++;
			else if (m.stats.skipped > s0) counts.skipped++;
			else if (m.stats.dropped > d0) counts.dropped++;
		}
		if (details.length > 0)
			console.warn(LOG_TAG, "E-RV-3", message("E-RV-3", [inst.id, "dropped " + details.length + " malformed event(s): " + details.slice(0, 3).join("; ")]));
		if (counts.applied > 0)
			scheduleRender(inst);
		return counts;
	}

	function resetInstance(inst) {
		if (inst.destroyed)
			return;
		inst.model = newModel();
		inst.recs = new Map();
		inst.lastDone = null;
		inst.trace = new Map();
		inst.stepsEl.replaceChildren();
		inst.runNotesEl.replaceChildren();
		hideTip(inst);
		inst.lastStatus = "empty";
		renderNow(inst);
	}

	function destroyInstance(inst) {
		if (inst.destroyed)
			return;
		inst.destroyed = true;
		if (inst.stopPoll)
			inst.stopPoll();
		for (const l of inst.listeners)
			l[0].removeEventListener(l[1], l[2]);
		inst.listeners = [];
		if (inst.timer !== null) {
			clearTimeout(inst.timer);
			inst.timer = null;
		}
		hideTip(inst);
		inst.tipEl.remove();
		inst.root.replaceChildren();
		inst.root.remove();
		if (REG_ID.get(inst.id) === inst.api) REG_ID.delete(inst.id);
		if (REG_EL.get(inst.host) === inst.api) REG_EL.delete(inst.host);
		inst.model = newModel();
		inst.recs = new Map();
	}

	function create(el, options) {
		const o = checkOptions(el, options);
		const inst = {
			id: o.id, host: el, compact: o.compact, rawHref: o.rawHref, emit: o.emit, model: newModel(), recs: new Map(),
			expanded: new Map(), trace: new Map(), warned: new Set(), listeners: [], timer: null, renders: 0, destroyed: false,
			lastDone: null, lastStatus: "empty", errored: false, tipFor: null, stopPoll: null, api: null, banner: null, connEl: null
		};
		const root = mk("section", "juneau-rv" + (o.compact ? " juneau-rv-compact" : ""));
		attr(root, "role", "region");
		attr(root, "aria-label", o.title);
		attr(root, "data-juneau-rv-status", "empty");
		const summary = mk("div", "juneau-rv-summary");
		inst.headline = mk("p", "juneau-rv-headline");
		attr(inst.headline, "aria-live", "polite");
		attr(inst.headline, "aria-atomic", "true");
		inst.summaryEl = summary;
		inst.countsEl = mk("p", "juneau-rv-counts");
		inst.failuresEl = mk("ul", "juneau-rv-failures");
		summary.appendChild(inst.headline);
		summary.appendChild(inst.countsEl);
		summary.appendChild(inst.failuresEl);
		inst.stepsEl = mk("ol", "juneau-rv-steps");
		inst.runNotesEl = mk("ul", "juneau-rv-run-notes");
		inst.tipEl = mk("div", "juneau-co-tooltip");
		attr(inst.tipEl, "role", "tooltip");
		attr(inst.tipEl, "id", o.id + "-tip");
		inst.tipEl.hidden = true;
		root.appendChild(summary);
		root.appendChild(inst.stepsEl);
		root.appendChild(inst.runNotesEl);
		root.appendChild(inst.tipEl);
		inst.root = root;
		el.appendChild(root);
		wire(inst);
		const api = Object.freeze({
			append: function (x) { return append(inst, x); },
			reset: function () { resetInstance(inst); },
			lastSeq: function () { return inst.model.lastSeq; },
			state: function () { return snapshot(inst); },
			stats: function () { return Object.assign({}, inst.model.stats); },
			destroy: function () { destroyInstance(inst); }
		});
		inst.api = api;
		INSTANCES.set(api, inst);
		REG_EL.set(el, api);
		REG_ID.set(o.id, api);
		renderNow(inst);
		return api;
	}

	function of(elOrId) {
		if (isStr(elOrId))
			return REG_ID.get(elOrId) || null;
		return (elOrId && REG_EL.get(elOrId)) || null;
	}

	// @section:poll
	const MIN_REFRESH = 1000;
	const DEF_REFRESH = 2000;
	const MAX_BACKOFF = 60000;
	const TOKEN = /^[A-Za-z0-9._~-]{1,128}$/;
	const NOT_A_PAGE = "(not a run-view page)";
	const GONE_TEXT = "the event stream was replaced (410 Gone) before a view was loaded";

	function isToken(s) { return isStr(s) && TOKEN.test(s); }

	function safeJson(v) {
		try {
			return JSON.stringify(v);
		} catch (e) { // NOSONAR javascript:S2486 -- an unserialisable value is reported by its String() form instead
			return String(v);
		}
	}

	function clip(s) {
		const v = isStr(s) ? s : String(s);
		return v.length > 64 ? v.slice(0, 64) + "…" : v;
	}

	function isAbort(err) { return !!err && err.name === "AbortError"; }

	function describeFailure(err) {
		if (err && Number.isInteger(err.status))
			return "HTTP " + err.status;
		return clip((err && err.message) || "network error");
	}

	function isResolvedUrl(u) { return isStr(u) && NS.init.isSafeDetailUrl(u) && !/[{}]/.test(u); }

	function withQuery(url, q) { return q ? url + (url.indexOf("?") >= 0 ? "&" : "?") + q : url; }

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
				} catch (e) { // NOSONAR javascript:S2486 -- an unparseable body is reported as E-RV-2 below
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

	/** The fatal [code, args] for a page that breaks the contract (spec 1.1, 5.3), or null. */
	function pageProblem(inst, page) {
		if (page.contractVersion !== CONTRACT_VERSION)
			return ["E-RV-2", [inst.id, String(page.contractVersion)]];
		if (!Array.isArray(page.events))
			return ["E-RV-2", [inst.id, NOT_A_PAGE]];
		if (!isToken(page.next))
			return ["E-RV-5", [inst.id]];
		let prev = -Infinity;
		for (const e of page.events) {
			if (isPlainObject(e) && Number.isSafeInteger(e.seq)) {
				if (e.seq <= prev)
					return ["E-RV-5", [inst.id]];
				prev = e.seq;
			}
		}
		return null;
	}

	/** Shows or clears the "Reconnecting..." line; created on the first outage so a healthy run has no such node. */
	function setReconnecting(inst, on) {
		if (!inst.connEl) {
			if (!on)
				return;
			inst.connEl = mk("p", "juneau-rv-reconnect", "Reconnecting…");
			attr(inst.connEl, "role", "status");
			inst.summaryEl.insertBefore(inst.connEl, inst.headline.nextSibling);
		}
		inst.connEl.hidden = !on;
	}

	/** A contract or request failure that ends polling: a banner, region state "error", a bus message; the content stays. */
	function fatal(inst, code, args) {
		if (inst.errored || inst.destroyed)
			return;
		inst.errored = true;
		const m = message(code, args);
		console.error(LOG_TAG, code, m);
		if (inst.stopPoll)
			inst.stopPoll();
		if (!inst.banner) {
			inst.banner = mk("div", "jc-console-error");
			attr(inst.banner, "role", "alert");
			inst.root.insertBefore(inst.banner, inst.root.firstChild);
		}
		const item = mk("div", "jc-console-error-item", m);
		attr(item, "data-juneau-error", code);
		inst.banner.appendChild(item);
		const host = (typeof inst.host.closest === "function" && inst.host.closest("[data-juneau-region]")) || inst.host;
		host.setAttribute("data-juneau-region-state", "error");
		emitBus(inst, { kind: "run-view.state", id: inst.id, state: "error", terminal: inst.model.terminal });
	}

	/**
	 * The polling loop (spec 5.3): a setTimeout chain with one request in flight.  Resolves at terminal, done,
	 * abort, destroy or a fatal code; never rejects.
	 */
	function poll(api, opts) {
		const inst = INSTANCES.get(api);
		const o = isPlainObject(opts) ? opts : {};
		if (!inst) {
			configError("run", "poll() needs an instance returned by create()");
			return Promise.resolve();
		}
		if (inst.destroyed || inst.errored || inst.stopPoll)
			return Promise.resolve();
		if (!isResolvedUrl(o.eventsUrl)) {
			configError(inst.id, "poll() needs a resolved same-origin eventsUrl: " + clip(safeJson(o.eventsUrl)));
			return Promise.resolve();
		}
		const eventsUrl = o.eventsUrl;
		const refreshMs = Number.isFinite(o.refreshMs) && o.refreshMs > 0 ? Math.max(MIN_REFRESH, o.refreshMs) : DEF_REFRESH;
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
			return next === null ? eventsUrl : withQuery(eventsUrl, "after=" + encodeURIComponent(next));
		}

		function run() {
			if (finished)
				return;
			let req;
			try {
				req = getPage(url(), ctl.signal);
			} catch (e) { // NOSONAR javascript:S2486 -- a fetch that throws synchronously is handled like a failed request
				onError(e);
				return;
			}
			req.then(onPage, onError);
		}

		/** Hidden tab: a due poll waits for visibilitychange, then runs at once. */
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
			if (finished || inst.destroyed || inst.errored) {
				finish();
				return;
			}
			const bad = pageProblem(inst, page);
			if (bad) {
				fatal(inst, bad[0], bad[1]);
				finish();
				return;
			}
			if (failures > 0) {
				failures = 0;
				inst.warned.delete("E-RV-4:outage");
				setReconnecting(inst, false);
			}
			next = page.next;
			let more = page.more === true && page.terminal !== true;
			if (more && page.events.length === 0) {
				warn(inst, "E-RV-3", [inst.id, "a page with more:true carried no events; treated as more:false"], "E-RV-3:empty-more");
				more = false;
			}
			append(inst, page.events);
			if (page.terminal === true && !more) {
				inst.model.terminal = true;
				scheduleRender(inst);
			}
			if (inst.destroyed)
				return;
			if (inst.model.terminal || (inst.model.done !== null && !more)) {
				finish();
				return;
			}
			schedule(more ? 0 : refreshMs);
		}

		function onError(err) {
			if (finished || inst.destroyed || inst.errored || isAbort(err)) {
				finish();
				return;
			}
			if (err && err.badBody) {
				fatal(inst, "E-RV-2", [inst.id, NOT_A_PAGE]);
				finish();
				return;
			}
			const status = err && Number.isInteger(err.status) ? err.status : null;
			if (status === 410) {
				if (next === null) {
					fatal(inst, "E-RV-4", [inst.id, "HTTP 410"]);
					finish();
					return;
				}
				console.warn(LOG_TAG, "E-RV-8", message("E-RV-8", [inst.id]));
				next = null;
				resetInstance(inst);
				schedule(0);
				return;
			}
			if (status !== null && status >= 400 && status < 500 && status !== 408 && status !== 429) {
				fatal(inst, "E-RV-4", [inst.id, describeFailure(err)]);
				finish();
				return;
			}
			failures++;
			warn(inst, "E-RV-4", [inst.id, describeFailure(err)], "E-RV-4:outage");
			setReconnecting(inst, true);
			let delay = Math.min(MAX_BACKOFF, refreshMs * Math.pow(2, failures));
			if (status === 429 && err.retryAfter !== null)
				delay = Math.max(refreshMs, err.retryAfter);
			schedule(delay);
		}

		inst.stopPoll = finish;
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

	/** {id} from rowId, then the same-origin check, then no leftover braces.  Throws E-RV-1. */
	function resolveUrl(id, name, url, rowId) {
		let u = url;
		if (u.indexOf("{id}") >= 0) {
			if (rowId === undefined || rowId === null || rowId === "")
				throw configError(id, name + " contains {id} but this region has no row id: " + clip(url));
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

	/**
	 * create() + poll() (spec 5.4).  Synchronous: returns the cleanup, never a promise.  Every configuration error
	 * throws E-RV-1 before any DOM exists.  ctx = {signal, rowId, id, emit}; all optional, and the card path passes {}.
	 */
	function mount(el, options, ctx) {
		const o = isPlainObject(options) ? options : {};
		const c = isPlainObject(ctx) ? ctx : {};
		const elId = el && typeof el.getAttribute === "function" ? (el.getAttribute("data-juneau-region") || el.id) : null;
		const id = String(o.id || c.id || elId || "run");
		if (o.poll !== undefined && typeof o.poll !== "boolean")
			throw configError(id, "poll must be a boolean: " + clip(safeJson(o.poll)));
		const polling = o.poll !== false;
		let eventsUrl;
		if (polling || o.eventsUrl !== undefined) {
			if (!isStr(o.eventsUrl) || o.eventsUrl.trim() === "")
				throw configError(id, "eventsUrl is required unless poll is false");
			eventsUrl = resolveUrl(id, "eventsUrl", o.eventsUrl, c.rowId);
		}
		if (o.refreshMs !== undefined && !(Number.isFinite(o.refreshMs) && o.refreshMs > 0))
			throw configError(id, "refreshMs must be a positive number: " + clip(safeJson(o.refreshMs)));
		const api = create(el, {
			id: id,
			compact: o.compact,
			title: o.title,
			rawHref: o.rawHref,
			emit: typeof c.emit === "function" ? c.emit : undefined
		});
		if (polling)
			poll(api, { eventsUrl: eventsUrl, refreshMs: o.refreshMs, signal: c.signal });
		return function () { api.destroy(); };
	}


	NS.runView = Object.freeze({ CONTRACT_VERSION, validateEvent, formatDuration, isSafeNoteHref, isSafeRawHref,
		create, poll, mount, of,
		__test: Object.freeze({ newModel, reduce, derive, renders: function (api) { const i = INSTANCES.get(api); return i ? i.renders : -1; } }) });

	// Registered at load time, so this file loads after juneau-regions.js (ViewsMixin#RUN_VIEW_JS_PATH).
	if (NS.regions && typeof NS.regions.register === "function") {
		NS.regions.register("run-view", function (ctx, el) {
			return mount(el, ctx.params || {}, {
				signal: ctx.signal,
				rowId: ctx.ids ? ctx.ids.rowId : undefined,
				id: ctx.id,
				emit: function (msg) { ctx.emit(msg); }
			});
		});
	} else {
		console.error(LOG_TAG, "juneau-run-view.js must load after juneau-regions.js; the run-view populator is not registered");
	}
})();
