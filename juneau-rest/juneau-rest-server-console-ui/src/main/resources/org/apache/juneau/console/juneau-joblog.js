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
 * Phases (data-phase on .jc-joblog): idle -> submitting -> running -> done; "New run" returns to idle.  A region adds
 * "keying" (waiting for the row's server-minted key) before idle.
 *
 * One view layer, two transports:
 *   - page (the "job-log" console card): the shell's fetchJson, a client-minted key, the card's own EventSource, and
 *     reload re-attach through ctx.prefs() key "job" (mirrored into the URL with urlParam).  It never reads
 *     window.JuneauViews.
 *   - region (the "job-log" row-detail populator, and JuneauJobLog.mount): JuneauViews.jobs.run with paint "none",
 *     a key from the row's keyUrl, and a view-only re-attach through JuneauViews.jobs.active after a collapse.
 */
(function () {
	"use strict";

	const MIN_HEIGHT = 80, DEFAULT_HEIGHT = 240;
	const CLOSED = 2;   // EventSource.CLOSED
	const POPULATOR = "job-log";
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
	// A region's refusals and errors before any result (JuneauViews.jobs onEnd reasons).
	const END_TEXT = {
		"row-job-running": "A job is already running for this row.",
		"missing-token": "The page has no CSRF token, so the job was not sent.",
		"no-row-id": "This row has no id, so the job was not sent.",
		"request-failed": "The request could not be completed; try again.",
		"no-result": "The server's answer was neither a job nor a result.",
		"no-event-source": NO_SSE,
		"stream-interrupted": INTERRUPTED
	};
	const MSG = {
		"E-JS-77": "job-log card '%s' submit response is not a valid AsyncJobRef: '%s'",
		"E-JS-78": "job-log card '%s' stream event could not be parsed: '%s'",
		"E-JS-81": "job-log card '%s' submit.endpoint is not a same-origin path: '%s'",
		"E-JS-109": "job-log card '%s' could not get a submit key from '%s'",
		"E-JS-110": "job-log card '%s' field '%s' source '%s' is not a string, number, boolean or null, and has no default",
		// The same code for a hidden field with no source at all (a page card has no row to read): nothing to send.
		"E-JS-110 no-source": "job-log card '%s' hidden field '%s' has no default",
		"E-JS-111": "job-log region mode needs window.JuneauViews.%s; load juneau-views.js and juneau-regions.js",
		"E-JS-112": "job-log card '%s' output target %s %s"
	};
	// The one preset grammar (JobLogChecks.SOURCE): an own property of the parent row, never a path.
	const SOURCE_RE = /^row\.([A-Za-z_][A-Za-z0-9_]{0,63})$/;

	const states = new WeakMap();

	function fmt(t) {
		const args = Array.prototype.slice.call(arguments, 1);
		let i = 0;
		return t.replace(/%s/g, () => String(args[i++]));
	}

	// `key` is a code, or a code, a space and a variant naming another message under the same code.
	function jobError(key) {
		const code = key.split(" ")[0];
		const e = new Error(code + ": " + fmt.apply(null, [MSG[key]].concat(Array.prototype.slice.call(arguments, 1))));
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

	function isAbort(e) {
		return !!e && e.name === "AbortError";
	}

	// ==================================================================================================================
	// Field presets and output (shared by both transports)
	// ==================================================================================================================

	// A preset's value: an own property of the parent row (never another row, the DOM or the page), else the field's
	// default; undefined when neither gives a string, finite number, boolean or null.
	function sourceValue(st, f) {
		const m = typeof f.source === "string" ? SOURCE_RE.exec(f.source) : null;
		const row = m ? st.rowData() : null;
		const v = row && typeof row === "object" && has(row, m[1]) ? row[m[1]] : undefined;
		if (isScalar(v)) return v;
		return has(f, "default") && isScalar(f.default) ? f.default : undefined;
	}

	function isScalar(v) {
		return v === null || typeof v === "string" || typeof v === "boolean" || (typeof v === "number" && Number.isFinite(v));
	}

	// The submitted values, by field name: visible inputs as typed (number: empty is null), hidden fields bound now
	// (a source reads the row as of the click; the table may have polled since render).  Throws E-JS-110.
	function fieldValues(st) {
		const values = {};
		for (const x of st.inputs) {
			const f = x.field;
			if (f.type === "hidden") {
				const v = sourceValue(st, f);
				if (v === undefined)
					throw f.source == null ? jobError("E-JS-110 no-source", st.card.id, f.name) : jobError("E-JS-110", st.card.id, f.name, preview(f.source));
				values[f.name] = v;
			} else {
				values[f.name] = f.type === "number" ? (x.input.value === "" ? null : Number(x.input.value)) : x.input.value;
			}
		}
		return values;
	}

	function outputTarget(st, t) {
		const scope = st.el;
		let found = null;
		if (t && typeof t.field === "string") {
			found = Array.from(scope.querySelectorAll("[data-juneau-field]")).find(n => n.getAttribute("data-juneau-field") === t.field) || null;
			if (!found) log(jobError("E-JS-112", st.card.id, "field '" + t.field + "'", "was not found within the card"));
			return found;
		}
		const sel = t && typeof t.selector === "string" ? t.selector : null;
		try {
			found = sel == null ? null : scope.querySelector(sel);
		} catch (e) {
			log(jobError("E-JS-112", st.card.id, "selector '" + preview(sel) + "'", "is not a valid selector"));
			return null;
		}
		if (!found) log(jobError("E-JS-112", st.card.id, "selector '" + preview(sel) + "'", "was not found within the card"));
		return found;
	}

	// Writes a terminal result's value into output.target, within the card (or region) only, as text: an input or
	// textarea gets .value, anything else textContent, never markup.  An absent value writes nothing.
	function applyOutput(st, r) {
		const o = st.card.output;
		if (!o || typeof o !== "object" || !r || typeof r !== "object") return;
		if (o.when !== "any" && r.outcome !== "success") return;
		const from = o.from == null ? "message" : o.from;
		let v;
		if (from === "message") {
			v = r.message;
		} else {
			const m = typeof from === "string" ? SOURCE_RE.exec(from) : null;
			v = m && r.row && typeof r.row === "object" && has(r.row, m[1]) ? r.row[m[1]] : undefined;
		}
		if (v === null || !isScalar(v)) return;
		const target = outputTarget(st, o.target);
		if (!target) return;
		const s = String(v), append = o.mode === "append";
		if (target.tagName === "INPUT" || target.tagName === "TEXTAREA") {
			const cur = target.value || "";
			target.value = append && cur !== "" ? cur + "\n" + s : s;
		} else {
			const cur = target.textContent || "";
			target.textContent = append && cur !== "" ? cur + "\n" + s : s;
		}
	}

	// ==================================================================================================================
	// The view layer
	// ==================================================================================================================

	function build(st) {
		const doc = st.doc, card = st.card, submit = card.submit || {};
		const root = node(doc, "div", st.region ? "jc-joblog jc-joblog-region" : "jc-card-body jc-joblog");
		const form = node(doc, "form", "jc-joblog-form");
		st.inputs = [];
		for (const f of Array.isArray(submit.fields) ? submit.fields : []) {
			if (f.type === "hidden") {
				// Not shown, not focusable, not in the required check; the value is bound at submit.
				const input = node(doc, "input", "jc-joblog-input jc-joblog-hidden");
				input.type = "hidden";
				input.name = f.name;
				input.setAttribute("data-juneau-field", f.name);
				if (f.source == null && f.default != null) input.value = String(f.default);
				form.appendChild(input);
				st.inputs.push({ field: f, input: input });
				continue;
			}
			const label = node(doc, "label", "jc-joblog-field");
			const span = node(doc, "span", "jc-joblog-label");
			span.textContent = f.label || f.name;
			const input = node(doc, f.type === "textarea" ? "textarea" : "input", "jc-joblog-input");
			if (f.type !== "textarea") input.type = f.type === "number" ? "number" : "text";
			input.name = f.name;
			input.setAttribute("data-juneau-field", f.name);
			if (f.placeholder) input.setAttribute("placeholder", f.placeholder);
			if (f.required) {
				input.required = true;
				input.setAttribute("required", "");
			}
			// A visible preset is a prefill; the user's edit wins at submit.
			const v = f.source != null ? sourceValue(st, f) : f.default;
			if (v != null) input.value = String(v);
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
		if (st.region) {
			const retry = node(doc, "button", "jc-joblog-retry");
			retry.type = "button";
			retry.textContent = "Retry";
			retry.hidden = true;
			retry.addEventListener("click", () => {
				setBanner(st, null);
				awaitKey(st);
			});
			actions.appendChild(retry);
			st.retry = retry;
		}
		form.appendChild(actions);
		const banner = node(doc, "div", "jc-joblog-banner");
		banner.hidden = true;
		const output = node(doc, "pre", "jc-joblog-output");
		output.setAttribute("role", "log");
		output.setAttribute("aria-live", "polite");
		output.setAttribute("tabindex", "0");
		const h = Number.isInteger(card.height) ? Math.max(MIN_HEIGHT, card.height) : DEFAULT_HEIGHT;
		output.style.height = h + "px";
		if (st.region) {
			// A region has no shell announce region; this one reads its non-error banners and replay summaries.
			const sr = node(doc, "span", "jc-joblog-sr");
			sr.setAttribute("role", "status");
			sr.setAttribute("aria-live", "polite");
			sr.style.position = "absolute";
			sr.style.left = "-9999px";
			root.appendChild(sr);
			st.sr = sr;
			const detail = node(doc, "div", "jc-joblog-detail");
			root.appendChild(detail);
			st.detail = detail;
		}
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
		if (st.btn.disabled && st.doc.activeElement === st.btn && typeof st.output.focus === "function") st.output.focus();
	}

	// Region mode cancels through JuneauViews.jobs.cancel, which needs the row's registry entry to be running; an older
	// runtime without it keeps the originating view's handle.cancel() and leaves a re-attached view view-only.
	function canCancel(st) {
		const u = st.job && st.job.cancelUrl;
		if (typeof u !== "string" || u === "" || hasUnsafeUrlChars(u)) return false;
		if (!st.region) return true;
		if (typeof st.jobs.cancel === "function") return st.jobs.active(st.el, (st.card.submit || {}).action) != null;
		return !!st.handle;
	}

	function setPhase(st, phase) {
		// The Run/Cancel button and "New run" swap places at done/idle; keep keyboard focus on whichever is visible.
		const active = st.doc.activeElement;
		const hadFocus = active === st.btn ? st.btn : active === st.newRun ? st.newRun : null;
		st.phase = phase;
		st.root.setAttribute("data-phase", phase);
		const running = phase === "running";
		for (const x of st.inputs) x.input.disabled = phase !== "idle" && phase !== "keying";
		// A view-only region (attached to a job another view started) has no submit; while the job runs it shows Cancel
		// when the job can be cancelled from this page.
		st.btn.hidden = phase === "done" || (st.viewOnly && phase !== "idle" && phase !== "keying"
			&& !(running && canCancel(st)));
		st.btn.type = running ? "button" : "submit";
		st.btn.textContent = running ? "Cancel" : (st.card.submit && st.card.submit.submitLabel) || "Run";
		st.btn.disabled = phase === "submitting" || phase === "keying" || (running && !canCancel(st));
		st.newRun.hidden = phase !== "done";
		if (st.retry) st.retry.hidden = !(phase === "keying" && st.keyFailed);
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
		if (tone !== "error") st.announce(text);
	}

	function addLine(st, text, cls) {
		const n = node(st.doc, "span", "jc-joblog-line" + (cls ? " " + cls : ""));
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
			st.announce("Reattached to the running job; " + st.replayCount + " earlier " + (st.replayCount === 1 ? "line" : "lines") + " shown.");
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
		if (st.region) return;
		st.prefs.remove("job");
		setUrlParam(st.card.urlParam, null);
	}

	// Back to the form: a region waits for a key first.
	function toIdle(st) {
		st.viewOnly = false;
		if (st.region) awaitKey(st);
		else setPhase(st, "idle");
	}

	function submitForm(st) {
		const card = st.card, submit = card.submit || {};
		// A scripted submit (and the Node harness) skips the browser's constraint validation, so check required here.
		const missing = st.inputs.filter(x => x.field.required && x.field.type !== "hidden" && String(x.input.value == null ? "" : x.input.value).trim() === "");
		for (const x of st.inputs) {
			if (missing.indexOf(x) >= 0) x.input.setAttribute("aria-invalid", "true");
			else x.input.removeAttribute("aria-invalid");
		}
		if (missing.length) {
			setBanner(st, "error", "Fill in the required fields: " + missing.map(x => "'" + (x.field.label || x.field.name) + "'").join(", ") + ".");
			if (typeof missing[0].input.focus === "function") missing[0].input.focus();
			return undefined;
		}
		let values;
		try {
			values = fieldValues(st);
		} catch (e) {
			log(e);
			setBanner(st, "error", e.message);
			return undefined;
		}
		if (st.region) return regionSubmit(st, values);
		if (!isSafeEndpoint(submit.endpoint)) {
			const e = jobError("E-JS-81", card.id, preview(submit.endpoint));
			log(e);
			setBanner(st, "error", e.message);
			return undefined;
		}
		const key = mintKey();
		let body;
		if (submit.envelope === "action") {
			// Self-targeted: the key is its own target, as a self-targeted dialog sends it.
			body = { action: submit.action, targetId: key, idempotencyKey: key, fields: values };
		} else {
			body = values;
			body.idempotencyKey = key;
		}
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
			// A re-attached view's Cancel goes once the row's job can no longer be cancelled from this page.
			if (st.viewOnly && st.phase === "running" && !st.btn.hidden && !st.cancelling && !canCancel(st)) setPhase(st, "running");
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
		// A region's synchronous answer (a 2xx or 4xx ActionResult) settles before any stream: still "submitting".
		if (st.phase !== "running" && !(st.region && st.phase === "submitting")) return;
		const outcome = has(OUTCOME_TEXT, r.outcome) ? r.outcome : "unknown";
		finish(st, outcome === "success" ? "ok" : "error", OUTCOME_TEXT[outcome] + (r.message ? " " + r.message : ""));
		applyOutput(st, r);
		// A view-only region never held a key; get one for the next run now.
		if (st.region && st.viewOnly) prefetchKey(st);
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
		toIdle(st);
	}

	// The row's registry entry is gone (the original stream ended) while the job may still run: drop Cancel, keep streaming.
	function cannotCancel(st) {
		st.cancelling = false;
		setPhase(st, "running");
		setBanner(st, "info", "This job can no longer be cancelled from this page.");
	}

	function cancel(st) {
		const job = st.job;
		if (st.phase !== "running" || st.cancelling) return;
		if (!canCancel(st)) {
			if (st.region && !st.btn.hidden) cannotCancel(st);   // the button outlived the row's registry entry
			return;
		}
		st.cancelling = true;
		const hadFocus = st.doc.activeElement === st.btn;
		st.btn.textContent = "Cancelling\u2026";
		st.btn.disabled = true;
		keepFocusOffDisabled(st);
		const stillThisJob = () => states.get(st.el) === st && st.phase === "running" && st.job === job;
		const failed = text => {
			st.cancelling = false;
			setPhase(st, "running");
			setBanner(st, "error", text);
			if (hadFocus && !st.btn.disabled && typeof st.btn.focus === "function") st.btn.focus();
		};
		if (st.region) {
			// JuneauViews.jobs posts the cancel with the row's CSRF token; the outcome still arrives on the stream.
			const sent = typeof st.jobs.cancel === "function" ? st.jobs.cancel(st.el, (st.card.submit || {}).action) : st.handle.cancel();
			sent.then(ok => {
				if (ok || !stillThisJob()) return;
				if (canCancel(st)) {
					failed("The cancel request failed; the job is still running.");
				} else {
					// The row's entry was released (the original stream ended) while the job may still run.
					cannotCancel(st);
				}
			});
			return;
		}
		// A write boundary rejects a POST that is not JSON (415), so send an empty JSON object, as the views runtime does.
		st.ctx.fetchJson(job.cancelUrl, { method: "POST", headers: { "Content-Type": "application/json" }, body: "{}" }).then(r => {
			if (!stillThisJob()) return;
			settle(st, r && typeof r === "object" && typeof r.outcome === "string" ? r : { outcome: "cancelled" });
		}, e => {
			if (!stillThisJob() || st.ctx.isAbort(e)) return;
			failed((e && e.message) || String(e));
		});
	}

	function startNewRun(st) {
		if (st.phase !== "done") return;
		closeStream(st);
		forget(st);
		setBanner(st, null);
		toIdle(st);
	}

	function teardown(el) {
		const st = states.get(el);
		if (!st) return;
		states.delete(el);
		st.seq++;
		closeStream(st);
		if (st.ctl) {
			try { st.ctl.abort(); } catch (e) { /* already aborted */ }
		}
	}

	// ==================================================================================================================
	// The page transport: the "job-log" console card
	// ==================================================================================================================

	function render(card, el, ctx) {
		teardown(el);
		for (const c of Array.from(el.childNodes))
			if (!(c.classList && c.classList.contains("jc-card-title"))) el.removeChild(c);
		const st = { card: card, el: el, ctx: ctx, doc: ctx.document, announce: ctx.announce, rowData: () => null, region: false,
			prefs: ctx.prefs(), phase: "idle", seq: 0, job: null, es: null, events: 0, cancelling: false, replaying: false, replayTimer: null,
			replayCap: null, probe: null, replayCount: 0 };
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

	// ==================================================================================================================
	// The region transport: JuneauViews.jobs
	// ==================================================================================================================

	function viewsApi(name) {
		const V = window.JuneauViews;
		return V && V[name] ? V[name] : null;
	}

	// Fetches the row's key once at a time; st.key is set when it arrives.  A failure is left to the caller.
	function fetchKey(st) {
		if (!st.keyPromise) {
			const submit = st.card.submit || {};
			const p = st.jobs.fetchKey({ el: st.el, keyUrl: submit.keyUrl, action: submit.action, signal: st.ctl.signal });
			st.keyPromise = p;
			p.then(k => {
				if (st.keyPromise === p) { st.keyPromise = null; st.key = k; }
			}, () => {
				if (st.keyPromise === p) st.keyPromise = null;
			});
		}
		return st.keyPromise;
	}

	function prefetchKey(st) {
		if (!st.key && states.get(st.el) === st) fetchKey(st).catch(() => { /* reported when the form needs it */ });
	}

	// "keying" until the row's key arrives, then idle.  A refused or failed key leaves submit disabled behind an error
	// banner and a Retry button.
	function awaitKey(st) {
		if (st.key) {
			setPhase(st, "idle");
			return;
		}
		st.keyFailed = false;
		setPhase(st, "keying");
		fetchKey(st).then(() => {
			if (states.get(st.el) === st && st.phase === "keying") setPhase(st, "idle");
		}, e => {
			if (states.get(st.el) !== st || st.phase !== "keying" || isAbort(e)) return;
			// JuneauViews.jobs has already logged why the key was refused; the banner says which card is waiting.
			st.keyFailed = true;
			setBanner(st, "error", jobError("E-JS-109", st.card.id, preview((st.card.submit || {}).keyUrl)).message);
			setPhase(st, "keying");
		});
	}

	function regionSubmit(st, values) {
		const card = st.card, submit = card.submit || {};
		if (!st.key) {
			awaitKey(st);
			return undefined;
		}
		if (card.clearOnSubmit !== false) clearOutput(st);
		setBanner(st, null);
		setPhase(st, "submitting");
		const seq = ++st.seq;
		const live = () => current(st, seq);
		// A refusal ends the run before run() returns, so the handle is only kept for a run that is still going.
		let h = null, ended = false;
		// No signal: a collapse ends this view, never the job, which stays in the row's registry for a re-expand to
		// re-attach to.  The hooks are bound to this view's root, so they go quiet once it leaves the page.
		h = st.jobs.run({
			el: st.root, endpoint: submit.endpoint, method: submit.method || "POST", action: submit.action, envelope: "action",
			idempotencyKey: st.key, fields: values, paint: "none",
			hooks: {
				onStart: ref => {
					if (!live()) return;
					st.job = toJob(ref);
					st.events = 0;
					setPhase(st, "running");
				},
				onProgress: line => { if (live()) addLine(st, line != null ? String(line) : ""); },
				onResult: r => { if (live()) settle(st, r); },
				onEnd: x => {
					ended = true;
					if (live()) regionEnd(st, x, h);
				}
			}
		});
		if (!ended) st.handle = h;
		return undefined;
	}

	// The key lives until the server answers: a job pointer, a result or an HTTP error spends it; no answer keeps it,
	// so a retry dedups on the server.
	function regionEnd(st, x, h) {
		const reason = x && x.error ? x.error.reason : null;
		const answered = !!(h && h.job) || (x && x.result != null) || reason === "no-result" || /^http:/.test(String(reason));
		st.handle = null;
		st.cancelling = false;
		if (answered) {
			st.key = null;
			prefetchKey(st);
		}
		if (st.phase === "done") return;   // settle already showed the outcome
		closeStream(st);
		const text = has(END_TEXT, reason) ? END_TEXT[reason]
			: /^http:/.test(String(reason)) ? "The server refused the request (HTTP " + String(reason).substring(5) + ")."
				: x && x.state === "refused" ? "The job was not sent (" + reason + ")."
					: "The job's progress could not be shown (" + reason + ").";
		const tone = reason === "stream-interrupted" || reason === "no-event-source" ? "warning" : "error";
		if (answered) {
			setPhase(st, "done");
		} else {
			st.job = null;
			setPhase(st, "idle");
		}
		setBanner(st, tone, text);
	}

	// A job already running (or still being submitted) for this row and action, from a view that is gone: show it
	// view-only, from the start of its log.
	function attachActive(st) {
		const action = (st.card.submit || {}).action;
		const ref = st.jobs.active(st.el, action);
		if (isRef(ref)) {
			st.viewOnly = true;
			open(st, toJob(ref), true);
			return true;
		}
		const pending = typeof st.jobs.pending === "function" ? st.jobs.pending(st.el, action) : null;
		if (!pending) return false;
		st.viewOnly = true;
		setPhase(st, "submitting");
		pending.then(r => {
			if (states.get(st.el) !== st || st.phase !== "submitting") return;
			if (isRef(r)) open(st, toJob(r), true);
			else toIdle(st);
		});
		return true;
	}

	function paintDetail(st, opts) {
		const d = st.card.detail;
		if (!d || typeof d !== "object" || typeof opts.values !== "function") return Promise.resolve();
		const helpers = opts.helpers || viewsApi("helpers");
		return Promise.resolve(opts.values()).then(values => {
			if (states.get(st.el) !== st) return;
			const fn = helpers ? helpers[d.renderer] : null;
			if (typeof fn !== "function") throw new Error("job-log card '" + st.card.id + "' detail renderer '" + d.renderer + "' is not a JuneauViews helper.");
			const n = fn(d.fields || [], { values: values });
			if (n != null) st.detail.appendChild(n);
		});
	}

	function mountView(el, config, opts) {
		opts = opts || {};
		const jobs = viewsApi("jobs");
		if (!jobs || typeof jobs.run !== "function") {
			const e = jobError("E-JS-111", "jobs");
			log(e);
			throw e;
		}
		teardown(el);
		while (el.firstChild) el.removeChild(el.firstChild);
		const doc = el.ownerDocument || window.document;
		const card = Object.assign({}, config || {});
		if (card.id == null) card.id = opts.id != null ? opts.id : POPULATOR;
		const st = { card: card, el: el, ctx: null, doc: doc, region: true, jobs: jobs, ctl: new AbortController(),
			rowData: () => {
				const ra = viewsApi("rowActions");
				const c = ra && typeof ra.contextOf === "function" ? ra.contextOf(el) : null;
				return c ? c.row : null;
			},
			phase: "idle", seq: 0, job: null, es: null, events: 0, cancelling: false, replaying: false, replayTimer: null, replayCap: null,
			probe: null, replayCount: 0, key: null, keyPromise: null, keyFailed: false, handle: null, viewOnly: false };
		st.announce = text => {
			st.sr.textContent = "";
			st.sr.textContent = text;
		};
		states.set(el, st);
		build(st);
		setPhase(st, "idle");
		if (!attachActive(st)) awaitKey(st);
		const sig = opts.signal;
		if (sig && typeof sig.addEventListener === "function") {
			// The view goes, the job does not.
			if (sig.aborted) destroyView(st);
			else sig.addEventListener("abort", () => destroyView(st), { once: true });
		}
		st.ready = paintDetail(st, opts);
		return st;
	}

	function destroyView(st) {
		if (states.get(st.el) !== st) return;
		teardown(st.el);
		if (st.root && st.root.parentNode) st.root.parentNode.removeChild(st.root);
	}

	function viewApi(st) {
		return Object.freeze({
			destroy: () => destroyView(st),
			cancel: () => cancel(st),
			phase: () => st.phase
		});
	}

	/**
	 * Mounts a row-bound job-log into `el` (an element inside a view table row or its detail panel), from a job-log
	 * region config ({submit, detail, height, clearOnSubmit, output}).  `opts.signal` destroys the view when aborted
	 * (never the job).  Throws E-JS-111 without JuneauViews.jobs.
	 */
	function mount(el, config, opts) {
		return viewApi(mountView(el, config, opts));
	}

	// The populator: the region's own signal, its declared payload for the detail grid, and its helpers.
	function mountRegion(el, config, ctx) {
		const st = mountView(el, config, {
			signal: ctx.signal, id: ctx.id, helpers: ctx.helpers,
			values: () => ctx.data != null ? ctx.data : ctx.fetchDeclared()
		});
		return st.ready.then(() => () => destroyView(st));
	}

	function populate(ctx, el) {
		return mountRegion(el, ctx.declared && ctx.declared.config, ctx);
	}

	// Registers the populator once JuneauViews.regions exists, whatever the script order; an app's own "job-log"
	// populator, registered first, is kept (and one registered later wins, since register is last-write-wins).
	let registered = false;
	function tryRegister() {
		if (registered) return true;
		const R = viewsApi("regions");
		if (!R || typeof R.register !== "function") return false;
		registered = true;
		if (typeof R.resolve !== "function" || typeof R.resolve(POPULATOR) !== "function") R.register(POPULATOR, populate);
		return true;
	}

	function reportUnregistered() {
		const doc = window.document;
		if (tryRegister() || !doc || typeof doc.querySelector !== "function") return;
		if (doc.querySelector("[data-juneau-region-populate=\"" + POPULATOR + "\"]")) log(jobError("E-JS-111", "regions"));
	}

	window.JuneauJobLog = Object.freeze({ version: "1", mount: mount });

	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push(["job-log", { render: render, refresh: refresh, destroy: destroy }]);

	if (!tryRegister()) {
		const doc = window.document;
		if (doc && doc.readyState === "complete") {
			reportUnregistered();
		} else {
			if (doc && typeof doc.addEventListener === "function") doc.addEventListener("DOMContentLoaded", tryRegister);
			if (typeof window.addEventListener === "function") window.addEventListener("load", reportUnregistered);
		}
	}
})();
