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
 * juneau-joblog.js - the console job-log card (type "job-log"): a form that submits to an endpoint returning an
 * AsyncJobRef {jobId, streamUrl, cancelUrl}, then streams the job's progress lines from AsyncJobsMixin over
 * EventSource until its terminal ActionResult, with cancel and reload re-attach.
 *
 * Phases (data-phase on .jc-joblog): idle -> submitting -> running -> done; "New run" returns to idle.
 * The running job is stored under ctx.prefs() key "job" and, with urlParam, mirrored into the URL with
 * history.replaceState, so a reload re-attaches to the same stream.
 */
(function () {
	"use strict";

	const MIN_HEIGHT = 80, DEFAULT_HEIGHT = 240;
	const CLOSED = 2;   // EventSource.CLOSED
	const NOT_FOUND = "Job not found; it may have finished or the server restarted.";
	const ATTACH_FAILED = "Could not attach to the job's progress stream; the server may be busy. Reload the page to try again.";
	const REPLAY_QUIET_MS = 250;   // a reattach replay ends after this long with no new line
	const REPLAY_MAX_MS = 1000;   // ... or this long after the stream opened, whichever comes first
	const PROBE_TIMEOUT_MS = 5000;   // the attach probe gives up after this long and reads as a failed attach
	const INTERRUPTED = "The progress stream was interrupted; the job may still be running. Reload the page to reattach.";
	const NO_SSE = "Live progress is unavailable in this browser.";
	const OUTCOME_TEXT = {
		"success": "Done.",
		"failure": "The job failed.",
		"refusal": "The job was refused.",
		"unknown": "The job ended with an unknown outcome.",
		"cancelled": "Cancelled.",
		"cancelled-after-effect": "Cancelled after the job had already made changes."
	};
	const MSG = {
		"E-JS-77": "job-log card '%s' submit response is not a valid AsyncJobRef: '%s'",
		"E-JS-78": "job-log card '%s' stream event could not be parsed: '%s'",
		"E-JS-81": "job-log card '%s' submit.endpoint is not a same-origin path: '%s'"
	};

	const states = new WeakMap();

	function fmt(t) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return t.replace(/%s/g, () => String(args[i++]));
	}

	function jobError(code) {
		const e = new Error(code + ": " + fmt.apply(null, [MSG[code]].concat(Array.prototype.slice.call(arguments, 1))));
		e.code = code;
		return e;
	}

	function log(e) {
		console.error("[juneau-joblog] " + (e && e.message || e));
	}

	function has(o, k) {
		return Object.prototype.hasOwnProperty.call(o, k);
	}

	function preview(v) {
		let s;
		if (typeof v === "string") {
			s = v;
		} else {
			try { s = JSON.stringify(v); } catch (e) { s = undefined; }
			if (s === undefined) s = String(v);
		}
		return s.length > 200 ? s.substring(0, 200) + "\u2026" : s;
	}

	// The stream and cancel URLs come from a server response (or from storage), and the cancel POST carries the CSRF
	// token, so both must resolve to this page's origin.  Absolute same-origin URLs are allowed.
	function isSameOrigin(u) {
		const loc = window.location;
		try {
			return new URL(u, loc.href || (loc.origin + (loc.pathname || "/"))).origin === loc.origin;
		} catch (e) {
			return false;
		}
	}

	/**
	 * The one prefix rule every same-origin / allowlist URL guard applies to the ORIGINAL string, before anything else
	 * (never normalise and accept): true when the first or last character is a space or control character, or any
	 * character is a backslash, a C0 control character or DEL.  A browser strips leading/trailing C0 and space,
	 * deletes tab/CR/LF anywhere and reads a backslash as a slash, so each of those can turn a value that looks like a
	 * same-origin path into "//host".  The text of this function is identical in every file that carries it
	 * (checked by ViewsJs_SameOriginPath_Test) and mirrors SafePathTemplate.isSameOriginPath.  Callers handle the empty string.
	 */
	function hasUnsafeUrlChars(s) {
		return s.charCodeAt(0) <= 32 || s.charCodeAt(s.length - 1) <= 32 || /[\\\u0000-\u001f\u007f]/.test(s);
	}

	// The submit POST carries the CSRF token too.  Java already holds submit.endpoint to a same-origin path; here the
	// shared character rule (a backslash, or a leading or trailing control character or space) is applied before the
	// origin is compared, so absolute same-origin URLs are still allowed.
	function isSafeEndpoint(u) {
		return typeof u === "string" && u !== "" && !hasUnsafeUrlChars(u) && isSameOrigin(u);
	}

	function isRef(v) {
		return !!v && typeof v === "object"
			&& typeof v.jobId === "string" && v.jobId !== ""
			&& typeof v.streamUrl === "string" && v.streamUrl !== "" && isSameOrigin(v.streamUrl)
			&& (v.cancelUrl == null || (typeof v.cancelUrl === "string" && (v.cancelUrl === "" || isSameOrigin(v.cancelUrl))));
	}

	function toJob(v) {
		return { jobId: v.jobId, streamUrl: v.streamUrl, cancelUrl: v.cancelUrl || null };
	}

	// A fresh attach to the stream is live-only, so the card asks for the whole history with lastEventId=0.  After a
	// drop the browser's own reconnect adds Last-Event-ID, which the server prefers over the query.  isRef has already
	// held streamUrl to this origin; a relative URL stays relative and an absolute one stays absolute.
	function historyUrl(streamUrl) {
		const loc = window.location;
		const u = new URL(streamUrl, loc.href || (loc.origin + (loc.pathname || "/")));
		u.searchParams.set("lastEventId", "0");
		return /^[a-z][a-z0-9+.-]*:/i.test(streamUrl) ? u.href : u.pathname + u.search;
	}

	function mintKey() {
		const c = window.crypto;
		if (c && typeof c.randomUUID === "function") return c.randomUUID();
		return "jl-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2);
	}

	function setUrlParam(name, value) {
		const h = window.history;
		if (!name || !h || typeof h.replaceState !== "function") return;
		const loc = window.location;
		const p = new URLSearchParams(loc.search || "");
		if (value == null) {
			if (!p.has(name)) return;
			p.delete(name);
		} else {
			p.set(name, value);
		}
		const qs = p.toString();
		h.replaceState(h.state, "", (loc.pathname || "/") + (qs ? "?" + qs : "") + (loc.hash || ""));
	}

	function urlMatches(card, job) {
		if (!card.urlParam) return true;
		return new URLSearchParams(window.location.search || "").get(card.urlParam) === job.jobId;
	}

	function node(doc, tag, cls) {
		const n = doc.createElement(tag);
		if (cls) n.className = cls;
		return n;
	}

	function build(st) {
		const doc = st.ctx.document, card = st.card, submit = card.submit || {};
		const root = node(doc, "div", "jc-card-body jc-joblog");
		const form = node(doc, "form", "jc-joblog-form");
		st.inputs = [];
		for (const f of Array.isArray(submit.fields) ? submit.fields : []) {
			const label = node(doc, "label", "jc-joblog-field");
			const span = node(doc, "span", "jc-joblog-label");
			span.textContent = f.label || f.name;
			const input = node(doc, f.type === "textarea" ? "textarea" : "input", "jc-joblog-input");
			if (f.type !== "textarea") input.type = f.type === "number" ? "number" : "text";
			input.name = f.name;
			if (f.placeholder) input.setAttribute("placeholder", f.placeholder);
			if (f.required) {
				input.required = true;
				input.setAttribute("required", "");
			}
			if (f.default != null) input.value = String(f.default);
			label.appendChild(span);
			label.appendChild(input);
			form.appendChild(label);
			st.inputs.push({ field: f, input: input });
		}
		const actions = node(doc, "div", "jc-joblog-actions");
		const btn = node(doc, "button", "jc-joblog-submit");
		btn.type = "submit";
		const newRun = node(doc, "button", "jc-joblog-newrun");
		newRun.type = "button";
		newRun.textContent = "New run";
		newRun.hidden = true;
		actions.appendChild(btn);
		actions.appendChild(newRun);
		form.appendChild(actions);
		const banner = node(doc, "div", "jc-joblog-banner");
		banner.hidden = true;
		const output = node(doc, "pre", "jc-joblog-output");
		output.setAttribute("role", "log");
		output.setAttribute("aria-live", "polite");
		output.setAttribute("tabindex", "0");
		const h = Number.isInteger(card.height) ? Math.max(MIN_HEIGHT, card.height) : DEFAULT_HEIGHT;
		output.style.height = h + "px";
		root.appendChild(form);
		root.appendChild(banner);
		root.appendChild(output);

		form.addEventListener("submit", ev => {
			if (ev && ev.preventDefault) ev.preventDefault();
			if (st.phase === "idle") submitForm(st);
		});
		btn.addEventListener("click", ev => {
			if (st.phase !== "running") return;
			if (ev && ev.preventDefault) ev.preventDefault();
			cancel(st);
		});
		newRun.addEventListener("click", () => startNewRun(st));

		Object.assign(st, { root: root, form: form, btn: btn, newRun: newRun, banner: banner, output: output });
		st.el.appendChild(root);
	}

	// A focused button that is about to be disabled would drop focus to the page; park it on the log pane instead.
	function keepFocusOffDisabled(st) {
		if (st.btn.disabled && st.ctx.document.activeElement === st.btn && typeof st.output.focus === "function") st.output.focus();
	}

	function setPhase(st, phase) {
		// The Run/Cancel button and "New run" swap places at done/idle; keep keyboard focus on whichever is visible.
		const active = st.ctx.document.activeElement;
		const hadFocus = active === st.btn ? st.btn : active === st.newRun ? st.newRun : null;
		st.phase = phase;
		st.root.setAttribute("data-phase", phase);
		const running = phase === "running";
		for (const x of st.inputs) x.input.disabled = phase !== "idle";
		st.btn.hidden = phase === "done";
		st.btn.type = running ? "button" : "submit";
		st.btn.textContent = running ? "Cancel" : (st.card.submit && st.card.submit.submitLabel) || "Run";
		st.btn.disabled = phase === "submitting" || (running && !(st.job && st.job.cancelUrl));
		st.newRun.hidden = phase !== "done";
		if (hadFocus && hadFocus.hidden) {
			const next = hadFocus === st.btn ? st.newRun : st.btn;
			if (!next.hidden && typeof next.focus === "function") next.focus();
		}
		keepFocusOffDisabled(st);
	}

	function setBanner(st, tone, text) {
		const b = st.banner;
		if (tone == null) {
			b.hidden = true;
			b.className = "jc-joblog-banner";
			b.textContent = "";
			return;
		}
		// One channel per message: an error banner is itself role=alert; any other banner is
		// plain text and the shell's polite announce region reads it.  The role is in place before the text, and the
		// text is cleared first, so a repeat of the same error is still a change and is announced again.
		if (tone === "error") b.setAttribute("role", "alert");
		else b.removeAttribute("role");
		b.className = "jc-joblog-banner jc-joblog-" + tone;
		b.textContent = "";
		b.textContent = text;
		b.hidden = false;
		if (tone !== "error") st.ctx.announce(text);
	}

	function addLine(st, text, cls) {
		const n = node(st.ctx.document, "span", "jc-joblog-line" + (cls ? " " + cls : ""));
		n.textContent = text;
		st.output.appendChild(n);
		st.output.scrollTop = st.output.scrollHeight;
	}

	function clearOutput(st) {
		while (st.output.firstChild) st.output.removeChild(st.output.firstChild);
	}

	// A reattach replays the whole buffer at once (the card asks for the history from the start).  Announcing every line would
	// flood the polite live region, so the pane is silenced while the lines arrive in a burst and one summary is announced when
	// the burst goes quiet.
	function replayLine(st, counted) {
		if (!st.replaying) return;
		if (counted) st.replayCount++;
		if (st.replayTimer) clearTimeout(st.replayTimer);
		st.replayTimer = setTimeout(() => endReplay(st, true), REPLAY_QUIET_MS);
	}

	function endReplay(st, announce) {
		if (!st.replaying) return;
		st.replaying = false;
		if (st.replayTimer) clearTimeout(st.replayTimer);
		if (st.replayCap) clearTimeout(st.replayCap);
		st.replayTimer = null;
		st.replayCap = null;
		st.output.setAttribute("aria-live", "polite");
		if (announce && st.replayCount > 0)
			st.ctx.announce("Reattached to the running job; " + st.replayCount + " earlier " + (st.replayCount === 1 ? "line" : "lines") + " shown.");
	}

	function closeStream(st) {
		endReplay(st, false);
		abortProbe(st);
		if (!st.es) return;
		try { st.es.close(); } catch (e) { /* already closed */ }
		st.es = null;
	}

	function current(st, seq) {
		return states.get(st.el) === st && st.seq === seq;
	}

	function forget(st) {
		st.job = null;
		st.prefs.remove("job");
		setUrlParam(st.card.urlParam, null);
	}

	function submitForm(st) {
		const card = st.card, submit = card.submit || {};
		// A scripted submit (and the Node harness) skips the browser's constraint validation, so check required here.
		const missing = st.inputs.filter(x => x.field.required && String(x.input.value == null ? "" : x.input.value).trim() === "");
		for (const x of st.inputs) {
			if (missing.indexOf(x) >= 0) x.input.setAttribute("aria-invalid", "true");
			else x.input.removeAttribute("aria-invalid");
		}
		if (missing.length) {
			setBanner(st, "error", "Fill in the required fields: " + missing.map(x => "'" + (x.field.label || x.field.name) + "'").join(", ") + ".");
			if (typeof missing[0].input.focus === "function") missing[0].input.focus();
			return undefined;
		}
		if (!isSafeEndpoint(submit.endpoint)) {
			const e = jobError("E-JS-81", card.id, preview(submit.endpoint));
			log(e);
			setBanner(st, "error", e.message);
			return undefined;
		}
		const body = {};
		for (const x of st.inputs)
			body[x.field.name] = x.field.type === "number" ? (x.input.value === "" ? null : Number(x.input.value)) : x.input.value;
		body.idempotencyKey = mintKey();
		if (card.clearOnSubmit !== false) clearOutput(st);
		setBanner(st, null);
		setPhase(st, "submitting");
		const seq = ++st.seq;
		return st.ctx.fetchJson(submit.endpoint, {
			method: submit.method || "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(body)
		}).then(ref => {
			if (!current(st, seq)) return;
			if (!isRef(ref)) {
				const e = jobError("E-JS-77", card.id, preview(ref));
				log(e);
				setPhase(st, "idle");
				setBanner(st, "error", e.message);
				return;
			}
			const job = toJob(ref);
			st.prefs.set("job", job);
			setUrlParam(card.urlParam, job.jobId);
			open(st, job);
		}, e => {
			if (!current(st, seq) || st.ctx.isAbort(e)) return;
			setPhase(st, "idle");
			setBanner(st, "error", (e && e.message) || String(e));
		});
	}

	function open(st, job, reattach) {
		st.job = job;
		st.events = 0;
		st.replayCount = 0;
		st.cancelling = false;
		setPhase(st, "running");
		const ES = window.EventSource;
		if (typeof ES !== "function") {
			setPhase(st, "done");
			setBanner(st, "warning", NO_SSE);
			return;
		}
		const es = new ES(historyUrl(job.streamUrl));
		st.es = es;
		if (reattach) {
			st.replaying = true;
			st.output.setAttribute("aria-live", "off");
			st.replayCap = setTimeout(() => endReplay(st, true), REPLAY_MAX_MS);
			replayLine(st, false);
		}
		es.addEventListener("progress", ev => {
			if (st.es !== es) return;
			st.events++;
			// No client-side dedupe: the server resumes after Last-Event-ID, so a repeated line is a server bug, shown.
			addLine(st, ev && ev.data != null ? String(ev.data) : "");
			replayLine(st, true);
		});
		es.addEventListener("result", ev => {
			if (st.es !== es) return;
			st.events++;
			let r = null;
			try { r = JSON.parse(ev.data); } catch (x) { r = null; }
			if (!r || typeof r !== "object" || typeof r.outcome !== "string") {
				const e = jobError("E-JS-78", st.card.id, preview(ev && ev.data));
				log(e);
				finish(st, "error", e.message);
				return;
			}
			settle(st, r);
		});
		es.addEventListener("error", () => {
			// CONNECTING: the browser is retrying by itself and resumes after the last event id it saw.
			// CLOSED before any event: the server refused the stream.
			if (st.es !== es) return;
			if (es.readyState !== CLOSED) return;
			if (st.events === 0) {
				probeAttach(st);
			} else {
				closeStream(st);
				setPhase(st, "done");
				setBanner(st, "warning", INTERRUPTED);
			}
		});
	}

	function settle(st, r) {
		if (st.phase !== "running") return;
		const outcome = has(OUTCOME_TEXT, r.outcome) ? r.outcome : "unknown";
		finish(st, outcome === "success" ? "ok" : "error", OUTCOME_TEXT[outcome] + (r.message ? " " + r.message : ""));
	}

	function finish(st, tone, text) {
		closeStream(st);
		st.cancelling = false;
		setPhase(st, "done");
		setBanner(st, tone, text);
	}

	function abortProbe(st) {
		const pr = st.probe;
		if (!pr) return;
		st.probe = null;
		if (pr.timer) clearTimeout(pr.timer);
		try { pr.controller.abort(); } catch (x) { /* already settled */ }
	}

	// EventSource reports a refused stream with no status.  Ask once more (a plain GET, aborted as soon as the status is known) so a
	// 404 still reads "not found" while a 429, a 5xx, a network failure or a silent server reads as a failed attach and keeps the
	// stored job for a reload.  closeStream aborts a probe still in flight.
	function probeAttach(st) {
		const job = st.job;
		closeStream(st);
		const stillThisJob = () => states.get(st.el) === st && st.phase === "running" && st.job === job && !st.es;
		const attachFailed = () => {
			abortProbe(st);
			if (!stillThisJob()) return;
			setPhase(st, "done");
			setBanner(st, "warning", ATTACH_FAILED);
		};
		if (typeof fetch !== "function" || typeof AbortController !== "function") {
			attachFailed();
			return;
		}
		const controller = new AbortController();
		const pr = { controller: controller, timer: null };
		st.probe = pr;
		pr.timer = setTimeout(attachFailed, PROBE_TIMEOUT_MS);
		fetch(historyUrl(job.streamUrl), { method: "GET", credentials: "same-origin", headers: { Accept: "text/event-stream" }, signal: controller.signal })
			.then(r => {
				if (st.probe !== pr) return;
				abortProbe(st);
				if (!stillThisJob()) return;
				if (r && r.status === 404) notFound(st);
				else attachFailed();
			}, () => {
				if (st.probe === pr) attachFailed();
			});
	}

	function notFound(st) {
		closeStream(st);
		clearOutput(st);
		addLine(st, NOT_FOUND, "jc-joblog-notice");
		forget(st);
		setBanner(st, null);
		setPhase(st, "idle");
	}

	function cancel(st) {
		const job = st.job;
		if (st.phase !== "running" || st.cancelling || !job || !job.cancelUrl) return;
		st.cancelling = true;
		const hadFocus = st.ctx.document.activeElement === st.btn;
		st.btn.textContent = "Cancelling\u2026";
		st.btn.disabled = true;
		keepFocusOffDisabled(st);
		const stillThisJob = () => states.get(st.el) === st && st.phase === "running" && st.job === job;
		// A write boundary rejects a POST that is not JSON (415), so send an empty JSON object, as the views runtime does.
		st.ctx.fetchJson(job.cancelUrl, { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" }).then(r => {
			if (!stillThisJob()) return;
			settle(st, r && typeof r === "object" && typeof r.outcome === "string" ? r : { outcome: "cancelled" });
		}, e => {
			if (!stillThisJob() || st.ctx.isAbort(e)) return;
			st.cancelling = false;
			setPhase(st, "running");
			setBanner(st, "error", (e && e.message) || String(e));
			if (hadFocus && !st.btn.disabled && typeof st.btn.focus === "function") st.btn.focus();
		});
	}

	function startNewRun(st) {
		if (st.phase !== "done") return;
		closeStream(st);
		forget(st);
		setBanner(st, null);
		setPhase(st, "idle");
	}

	function teardown(el) {
		const st = states.get(el);
		if (!st) return;
		states.delete(el);
		st.seq++;
		closeStream(st);
	}

	function render(card, el, ctx) {
		teardown(el);
		for (const c of Array.from(el.childNodes))
			if (!(c.classList && c.classList.contains("jc-card-title"))) el.removeChild(c);
		const st = { card: card, el: el, ctx: ctx, prefs: ctx.prefs(), phase: "idle", seq: 0, job: null, es: null, events: 0, cancelling: false, replaying: false, replayTimer: null, replayCap: null, probe: null, replayCount: 0 };
		states.set(el, st);
		build(st);
		setPhase(st, "idle");
		ctx.setApi({
			phase: () => st.phase,
			job: () => st.job ? Object.assign({}, st.job) : null,
			cancel: () => cancel(st)
		});
		const stored = st.prefs.get("job", null);
		if (stored != null && !isRef(stored)) forget(st);
		else if (isRef(stored) && urlMatches(card, stored)) open(st, toJob(stored), true);
		return undefined;
	}

	// A running stream is not a poll: refresh must not remount (the shell's default refresh is render).
	function refresh() {
		return undefined;
	}

	function destroy(card, el) {
		teardown(el);
	}

	window.JuneauJobLog = Object.freeze({ version: "1" });

	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["job-log", { render: render, refresh: refresh, destroy: destroy }]);
})();
