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
 * juneau-data.js - the DataRegistry (JuneauViews.data): the columnar frames behind data:<source> bus topics.  The bus
 * carries small frozen handles; the arrays live here.  Loaded after juneau-bus.js.
 *
 *   JuneauViews.data.get(handle)            -> frame | null (null when the version is no longer held)
 *   JuneauViews.data.put(source, frame)     -> the new version (source publishers only)
 *   JuneauViews.data.release(source)        drops the frames
 *   JuneauViews.data.building(source, f)    sets (or clears) the partial frame get({...h, partial:true}) returns
 *   JuneauViews.data.isHandle(h)            true when h has a string source, an integer version >= 0, a known status
 *                                           and a dims array (anything else is malformed: E-JS-95 at the reader)
 *   JuneauViews.data.boot(contract)         one DataSource/Derived per contract.dataSources entry (the shell calls it)
 *   JuneauViews.data.builtins               [registry, name, impl] triples juneau-charts.js registers as built-ins
 *   JuneauViews.data.config                 {timers, platform, log}: the loader's clock, fetch/IndexedDB/WebCrypto, log
 *   JuneauViews.data.util                   the loader's pure helpers (frozen; for tests and app parsers)
 */
(function () {
	"use strict";

	const NS = window.JuneauViews = window.JuneauViews || {};
	if (NS.data) return;

	const STATUSES = ["loading", "ready", "error", "stale"];
	// source -> {version, current, previous, building}
	const held = new Map();

	function isHandle(h) {
		return !!h && typeof h === "object" && typeof h.source === "string" && h.source.length > 0
			&& Number.isInteger(h.version) && h.version >= 0 && STATUSES.indexOf(h.status) >= 0
			&& Array.isArray(h.dims);
	}

	function put(source, frame) {
		const e = held.get(source) || { version: 0, current: null, previous: null, building: null };
		const version = e.version + 1;
		const f = Object.assign({}, frame, { source: source, version: version });
		held.set(source, { version: version, current: f, previous: e.current, building: null });
		return version;
	}

	function get(handle) {
		if (!isHandle(handle)) return null;
		const e = held.get(handle.source);
		if (!e) return null;
		if (handle.partial === true) return e.building;
		if (e.current && e.current.version === handle.version) return e.current;
		if (e.previous && e.previous.version === handle.version) return e.previous;
		return null;
	}

	function building(source, frame) {
		const e = held.get(source) || { version: 0, current: null, previous: null, building: null };
		e.building = frame || null;
		held.set(source, e);
	}

	function release(source) {
		held.delete(source);
	}

	// ---- A. config and messages -------------------------------------------------------------------------------------

	// Reading indexedDB throws in some locked-down contexts; that is the same as having none.
	function openableIdb() {
		try {
			return typeof indexedDB !== "undefined" ? indexedDB : null;
		} catch (e) {
			return null;
		}
	}

	// Everything the loader uses outside the language.  The Node harness (data.cjs) swaps in a fake clock, fetch,
	// IndexedDB and WebCrypto; the page never needs to touch it.
	const config = {
		timers: {
			setTimeout: (fn, ms) => setTimeout(fn, ms),
			clearTimeout: id => clearTimeout(id),
			now: () => Date.now()
		},
		platform: {
			fetch: typeof fetch === "function" ? (url, init) => fetch(url, init) : null,
			TextDecoder: typeof TextDecoder === "function" ? TextDecoder : null,
			TextEncoder: typeof TextEncoder === "function" ? TextEncoder : null,
			AbortController: typeof AbortController === "function" ? AbortController : null,
			URLSearchParams: typeof URLSearchParams === "function" ? URLSearchParams : null,
			IDBKeyRange: typeof IDBKeyRange !== "undefined" ? IDBKeyRange : null,
			indexedDB: openableIdb(),
			subtle: typeof crypto !== "undefined" && crypto && crypto.subtle ? crypto.subtle : null
		},
		log: {
			error: msg => console.error("[juneau-data] " + msg),
			warn: msg => console.warn("[juneau-data] " + msg)
		}
	};

	// The data-source messages.  E-JS-62 names the source, not a card.
	const MSG = {
		"E-JS-62": "data source '%s' fetch failed: '%s'",
		"E-JS-92": "data source '%s' uses unknown parser '%s'; registered: '%s'",
		"E-JS-93": "data source '%s' uses unknown builder '%s'; registered: '%s'",
		"E-JS-94": "data source '%s' uses unknown transform '%s'; registered: '%s'",
		"E-JS-96": "data source '%s' aggregate response violates the contract: %s",
		"E-JS-97": "%s.register: name '%s' %s",
		"E-JS-98": "'%s' %s '%s' failed: '%s'",
		"E-JS-99": "data source '%s' chunk '%s' failed to parse at record %s: '%s'",
		"E-JS-100": "data source '%s' url is not a same-origin path: '%s'",
		"E-JS-101": "data source '%s' cache unavailable (%s); using memory",
		"E-JS-102": "data source '%s' truncated at %s rows",
		"E-JS-107": "data source '%s' window spans more than %s chunks"
	};

	function fmt(msg, ...args) {
		let i = 0;
		return msg.replace(/%s/g, () => String(args[i++]));
	}

	// The handle's error object: {code, message}, the message without the code.
	function problem(code, ...args) {
		return { code: code, message: fmt(MSG[code], ...args) };
	}

	function dataError(code, ...args) {
		const p = problem(code, ...args);
		const e = new Error(code + ": " + p.message);
		e.code = code;
		e.problem = p;
		return e;
	}

	function msgOf(e) {
		return e && e.message || String(e);
	}

	// Test-visible helpers, frozen at export (each section adds its own).
	const util = {};

	Object.assign(util, { fmt: fmt, problem: problem });

	// ---- B. extension lookup ----------------------------------------------------------------------------------------

	// Pinned to BusWiringValidator.EXTENSION_NAME by DataExtensionName_Test.
	const EXTENSION_NAME_RE = /^[a-z][a-zA-Z0-9]*(\.[a-z][a-zA-Z0-9-]*)*$/;
	const KINDS = ["parser", "builder", "transform"];
	// [registry, name, impl] triples juneau-charts.js drains as built-ins.
	const builtins = [];
	// The local registry, used when juneau-charts.js is not loaded.
	const local = { parser: new Map(), builder: new Map(), transform: new Map() };
	// Queue entries already read (or refused), so a second lookup neither re-registers nor re-logs them.
	const seen = new Set();

	function builtin(kind, name, impl) {
		builtins.push(Object.freeze([kind, name, impl]));
		local[kind].set(name, impl);
	}

	// Parsers and builders are objects with create(); transforms are functions.
	function implProblem(kind, impl) {
		if (kind === "transform") return typeof impl === "function" ? null : "is not a function";
		return impl && typeof impl.create === "function" ? null : "has no create function";
	}

	function localRegister(kind, name, impl) {
		let reason = null;
		if (typeof name !== "string" || !EXTENSION_NAME_RE.test(name)) reason = "is not a valid name";
		else if (name.indexOf(".") < 0) reason = "is bare; app names are dotted ('app." + name + "')";
		else if (local[kind].has(name)) reason = "is already registered";
		else reason = implProblem(kind, impl);
		if (reason) {
			config.log.error(dataError("E-JS-97", "JuneauCharts." + kind, name, reason).message);
			return;
		}
		local[kind].set(name, impl);
	}

	// Reads window.JuneauConsoleCharts without consuming it: juneau-charts.js, if it loads later, drains every entry.
	function readQueue() {
		const q = window.JuneauConsoleCharts;
		if (!Array.isArray(q)) return;
		for (const t of q) {
			if (seen.has(t)) continue;
			seen.add(t);
			if (Array.isArray(t) && t.length === 3 && KINDS.indexOf(t[0]) >= 0) localRegister(t[0], t[1], t[2]);
		}
	}

	function charts(kind) {
		const jc = window.JuneauCharts;
		const ok = jc && jc[kind] && typeof jc[kind].get === "function" && typeof jc[kind].names === "function";
		return ok ? jc[kind] : null;
	}

	// {impl, names}: impl is null for an unknown name; names is every registered name, for E-JS-92..94.
	function lookup(kind, name) {
		const jc = charts(kind);
		if (!jc) readQueue();
		const impl = jc && jc.get(name) || local[kind].get(name) || null;
		const names = new Set(local[kind].keys());
		if (jc) jc.names().forEach(n => names.add(n));
		return { impl: impl, names: Array.from(names).sort() };
	}

	Object.assign(util, { lookup: lookup });

	// ---- C. URLs and the fetch helper -------------------------------------------------------------------------------

	/**
	 * The one prefix rule every same-origin / allowlist URL guard applies to the ORIGINAL string, before anything else
	 * (never normalise and accept): true when the first or last character is a space or control character, or any
	 * character is a backslash, a C0 control character or DEL.  A browser strips leading/trailing C0 and space,
	 * deletes tab/CR/LF anywhere and reads a backslash as a slash, so each of those can turn a value that looks like a
	 * same-origin path into "//host".  The text of this function is identical in every file that carries it
	 * (checked by ViewsJs_SameOriginPath_Test) and mirrors SafePathTemplate.isSameOriginPath.  Callers handle the empty
	 * string.
	 */
	function hasUnsafeUrlChars(s) {
		return s.charCodeAt(0) <= 32 || s.charCodeAt(s.length - 1) <= 32 || /[\\\u0000-\u001f\u007f]/.test(s);
	}

	// Exact parity with SafePathTemplate.isSameOriginPath (juneau-commons): the original string is refused outright
	// when it has a leading/trailing space or control character, a backslash, or a control character anywhere.
	function isSafeDataPath(u) {
		const s = String(u);
		if (!s || hasUnsafeUrlChars(s) || s.includes("://") || s.startsWith("//")) return false;
		const colon = s.indexOf(":"), slash = s.indexOf("/");
		if (colon >= 0 && (slash < 0 || colon < slash)) return false;
		return s.split("/").indexOf("..") < 0;
	}

	const TOKEN_RE = /\{([A-Za-z0-9_.-]+)\}/g;

	function isScalar(v) {
		return typeof v === "string" || typeof v === "boolean" || (typeof v === "number" && Number.isFinite(v));
	}

	// {token}s take scalars, encodeURIComponent'd; every other non-null param joins the query in key order,
	// an array repeating its key.  {missing: true} while a token has no value (null, absent or the empty string: a
	// half-filled picker); E-JS-100 for an unusable value (naming the template) or a result that is not a same-origin
	// path (naming the URL).  extra (the chunk window) wins.
	// The template's own query stays verbatim and first; a key in both appears twice.  A fragment is E-JS-100.
	function resolveUrl(id, tpl, params, extra) {
		if (typeof tpl !== "string" || tpl.indexOf("#") >= 0) throw dataError("E-JS-100", id, String(tpl));
		const all = Object.assign({}, params || {}, extra || {});
		const used = new Set();
		let missing = false, bad = false;
		const path = tpl.replace(TOKEN_RE, (m, name) => {
			used.add(name);
			const v = Object.prototype.hasOwnProperty.call(all, name) ? all[name] : null;
			if (v == null || v === "") missing = true;
			else if (!isScalar(v)) bad = true;
			else return encodeURIComponent(String(v));
			return m;
		});
		if (bad) throw dataError("E-JS-100", id, tpl);
		if (missing) return { missing: true };
		const qs = new config.platform.URLSearchParams();
		for (const k of Object.keys(all).sort()) {
			if (used.has(k) || all[k] == null) continue;
			for (const x of Array.isArray(all[k]) ? all[k] : [all[k]]) {
				if (x == null) continue;
				if (!isScalar(x)) throw dataError("E-JS-100", id, tpl);
				qs.append(k, String(x));
			}
		}
		const q = qs.toString();
		// A trailing "?" is already a separator; a trailing "&" is one only when the path has a query.
		const hasQuery = path.indexOf("?") >= 0;
		const sep = /\?$/.test(path) || (hasQuery && /&$/.test(path)) ? "" : hasQuery ? "&" : "?";
		const url = q ? path + sep + q : path;
		if (!isSafeDataPath(url)) throw dataError("E-JS-100", id, url);
		return { url: url };
	}

	// ISO-8601 UTC, without a zero millisecond part: requests always send ISO-8601 UTC.
	function iso(t) {
		return new Date(t).toISOString().replace(".000Z", "Z");
	}

	const ACCEPT = Object.freeze({ csv: "text/csv", ndjson: "application/x-ndjson", json: "application/json" });

	// The source's own GET: checked same-origin again, same-origin credentials, an Accept for the format, the
	// generation's signal.  Only an AbortError raised by that signal passes through untouched, so the caller can drop
	// it silently; anything else is E-JS-62, including an AbortError the signal did not cause, a TimeoutError or any
	// other abort reason: those are real failures, not a superseded request.
	// A non-OK response has its body released.
	function fetchData(id, url, signal, accept) {
		if (typeof url !== "string" || !isSafeDataPath(url))
			return Promise.reject(dataError("E-JS-100", id, String(url)));
		const f = config.platform.fetch;
		if (!f) return Promise.reject(dataError("E-JS-62", id, "fetch is not available"));
		const init = { method: "GET", credentials: "same-origin", headers: { Accept: accept || "*/*" } };
		if (signal) init.signal = signal;
		return Promise.resolve().then(() => f(url, init)).then(res => {
			if (!res || !res.ok) {
				try {
					const c = res && res.body && typeof res.body.cancel === "function" ? res.body.cancel() : null;
					if (c && typeof c.catch === "function") c.catch(() => { /* the stream is already closed */ });
				} catch (x) { /* the stream is already closed */ }
				throw dataError("E-JS-62", id, "HTTP " + (res ? res.status : "no response"));
			}
			return res;
		}, e => {
			if (e && e.name === "AbortError" && signal && signal.aborted) throw e;
			throw dataError("E-JS-62", id, msgOf(e));
		});
	}

	// The body as text pieces, split-safe for UTF-8; response.text() when there is no reader or decoder.
	// A throw from onText (a parse error, a superseded generation) cancels the reader and propagates.
	async function readBody(res, onText) {
		const TD = config.platform.TextDecoder;
		if (!res.body || typeof res.body.getReader !== "function" || !TD) {
			const all = await res.text();
			if (all) onText(all);
			return;
		}
		const reader = res.body.getReader(), dec = new TD("utf-8");
		try {
			for (;;) {
				const r = await reader.read();
				if (r.done) break;
				const s = dec.decode(r.value, { stream: true });
				if (s) onText(s);
			}
			const tail = dec.decode();
			if (tail) onText(tail);
		} catch (e) {
			try {
				const c = reader.cancel();
				if (c && typeof c.catch === "function") c.catch(() => { /* the stream is already closed */ });
			} catch (x) { /* the stream is already closed */ }
			throw e;
		}
	}

	Object.assign(util, { isSafeDataPath: isSafeDataPath, resolveUrl: resolveUrl, iso: iso, ACCEPT: ACCEPT,
		fetchData: fetchData, readBody: readBody });

	// ---- D. parsers ------------------------------------------------------------------------------------------------

	const INFER_ROWS = 100;
	const NUMBER_RE = /^-?(\d+\.?\d*|\.\d+)([eE][-+]?\d+)?$/;
	const TYPES = ["number", "time", "string", "bool"];
	const OWN = Object.prototype.hasOwnProperty;

	// Parse errors carry a record number.  CSV counts data records from 1 (blank lines and the sentinel are not
	// records; the header is record 0).  NDJSON counts physical lines from 1, blank lines and the sentinel included.
	// JSON counts rows from 1 and reports 0 for the whole body.

	function parseError(record, reason) {
		const e = new Error(reason);
		e.record = record;
		return e;
	}

	function declared(opts) {
		const cols = opts && opts.columns;
		return Array.isArray(cols) && cols.length ? cols.map(c => ({ name: c.name, type: c.type })) : null;
	}

	// A function that drops one leading U+FEFF from the first non-empty piece it sees and passes the rest through.
	function bomStripper() {
		let first = true;
		return t => {
			if (!first || !t) return t;
			first = false;
			return t.charCodeAt(0) === 0xFEFF ? t.slice(1) : t;
		};
	}

	// A sentinel count: a non-negative integer.
	function isCount(n) {
		return Number.isInteger(n) && n >= 0;
	}

	// CSV inference: a column is a number when every non-null value of the held rows is numeric, else a string.
	function inferCsv(header, rows) {
		return header.map((name, k) => {
			let any = false, num = true;
			for (const r of rows) {
				if (r[k] == null) continue;
				any = true;
				if (!NUMBER_RE.test(r[k])) {
					num = false;
					break;
				}
			}
			return { name: name, type: any && num ? "number" : "string" };
		});
	}

	// Object records: keys in first-seen order; number, bool or string by the JSON type, a mix being a string.
	function inferObjects(rows) {
		const seen = new Map();
		for (const r of rows) {
			for (const k of Object.keys(r)) {
				const v = r[k];
				const t = typeof v === "number" ? "number" : typeof v === "boolean" ? "bool" : v == null ? null
					: "string";
				if (!seen.has(k)) seen.set(k, t);
				else if (t && seen.get(k) && seen.get(k) !== t) seen.set(k, "string");
				else if (t && !seen.get(k)) seen.set(k, t);
			}
		}
		return Array.from(seen, ([name, t]) => ({ name: name, type: t || "string" }));
	}

	// RFC 4180 over arbitrary piece boundaries.  Unquoted empty is null, quoted empty is "".  A trailing unquoted
	// "#truncated,<digits>" record is the sentinel, and a lone empty field is a blank line, skipped.
	function csvParser(opts) {
		const SPECIAL = /[,\r\n"]/g;
		const cols = declared(opts);
		const unbom = bomStripper();
		let field = "", quoted = false, inQ = false, pendingQuote = false, closed = false, atStart = true;
		let skipLF = false, rec = [], firstQuoted = false, header = null, dims = null, held = [], records = [];
		let sentinel = null, recNo = 0;

		// The number of the record being read: 0 until the header is complete.
		function at() {
			return header ? recNo + 1 : 0;
		}

		function endField() {
			if (rec.length === 0) firstQuoted = quoted;
			rec.push(quoted ? field : field === "" ? null : field);
			field = "";
			quoted = closed = false;
			atStart = true;
		}

		function emit(r) {
			if (dims) {
				records.push(r);
				return;
			}
			held.push(r);
			if (held.length >= INFER_ROWS) release();
		}

		function release() {
			dims = inferCsv(header || [], held);
			for (const r of held) records.push(r);
			held = [];
		}

		function endRecord() {
			const r = rec;
			rec = [];
			if (r.length === 1 && r[0] === null) return;
			if (!header) {
				header = r.map(x => x == null ? "" : x);
				const names = new Set();
				for (const name of header) {
					if (name === "") throw parseError(0, "empty column name");
					if (names.has(name)) throw parseError(0, "duplicate column '" + name + "'");
					names.add(name);
				}
				if (cols) dims = cols;
				return;
			}
			if (sentinel !== null) throw parseError(recNo + 1, "a record follows the #truncated sentinel");
			if (!firstQuoted && r.length === 2 && r[0] === "#truncated" && r[1] !== null && /^\d+$/.test(r[1])) {
				sentinel = Number(r[1]);
				return;
			}
			recNo++;
			if (r.length !== header.length)
				throw parseError(recNo, "the record has " + r.length + " fields; the header has " + header.length);
			emit(r);
		}

		function scan(s) {
			let i = 0;
			while (i < s.length) {
				if (skipLF) {
					skipLF = false;
					if (s[i] === "\n") {
						i++;
						continue;
					}
				}
				if (inQ) {
					if (pendingQuote) {
						pendingQuote = false;
						if (s[i] === '"') {
							field += '"';
							i++;
							continue;
						}
						inQ = false;
						closed = true;
					} else {
						const j = s.indexOf('"', i);
						if (j < 0) {
							field += s.slice(i);
							return;
						}
						field += s.slice(i, j);
						i = j + 1;
						pendingQuote = true;
						continue;
					}
				}
				const c = s[i];
				if (closed && c !== "," && c !== "\r" && c !== "\n")
					throw parseError(at(), "text follows a closing quote");
				if (c === '"') {
					if (!atStart) throw parseError(at(), "a quote inside an unquoted field");
					inQ = quoted = true;
					atStart = false;
					i++;
				} else if (c === ",") {
					endField();
					i++;
				} else if (c === "\r" || c === "\n") {
					endField();
					endRecord();
					skipLF = c === "\r";
					i++;
				} else {
					SPECIAL.lastIndex = i;
					const m = SPECIAL.exec(s);
					const j = m ? m.index : s.length;
					field += s.slice(i, j);
					atStart = false;
					i = j;
				}
			}
		}

		function take() {
			const out = records;
			records = [];
			return out;
		}

		return {
			push(text) {
				scan(unbom(text));
				return take();
			},
			end() {
				if (pendingQuote) {
					pendingQuote = inQ = false;
					closed = true;
				}
				if (inQ) throw parseError(at(), "an unterminated quoted field");
				if (field !== "" || quoted || rec.length) {
					endField();
					endRecord();
				}
				if (!dims) {
					if (cols) dims = cols;
					else release();
				}
				return take();
			},
			dims: () => dims,
			layout: () => header,
			truncated: () => sentinel
		};
	}

	// One JSON object per line; blank lines skipped; a final {"$truncated": n} line is the sentinel.
	function ndjsonParser(opts) {
		const cols = declared(opts);
		const unbom = bomStripper();
		// The unterminated tail of the text so far, as pieces, so a long line split over many pushes is joined once.
		let parts = [], line = 0, sentinel = null, dims = cols, held = [], records = [];

		function release() {
			dims = inferObjects(held);
			for (const r of held) records.push(r);
			held = [];
		}

		function onLine(raw) {
			line++;
			const s = raw.endsWith("\r") ? raw.slice(0, -1) : raw;
			if (!s.trim()) return;
			let v;
			try {
				v = JSON.parse(s);
			} catch (e) {
				throw parseError(line, "the line is not JSON");
			}
			if (!v || typeof v !== "object" || Array.isArray(v))
				throw parseError(line, "the line is not a JSON object");
			if (sentinel !== null) throw parseError(line, "a record follows the $truncated sentinel");
			const keys = Object.keys(v);
			if (keys.length === 1 && keys[0] === "$truncated" && isCount(v.$truncated)) {
				sentinel = v.$truncated;
				return;
			}
			if (dims) records.push(v);
			else {
				held.push(v);
				if (held.length >= INFER_ROWS) release();
			}
		}

		function take() {
			const out = records;
			records = [];
			return out;
		}

		return {
			push(text) {
				const t = unbom(text);
				let start = 0, nl;
				while ((nl = t.indexOf("\n", start)) >= 0) {
					const seg = t.slice(start, nl);
					start = nl + 1;
					if (parts.length) {
						parts.push(seg);
						const whole = parts.join("");
						parts = [];
						onLine(whole);
					} else onLine(seg);
				}
				if (start < t.length) parts.push(t.slice(start));
				return take();
			},
			end() {
				if (parts.length) {
					const whole = parts.join("");
					parts = [];
					onLine(whole);
				}
				if (!dims) release();
				return take();
			},
			dims: () => dims,
			layout: () => null,
			truncated: () => sentinel
		};
	}

	// An array of rows or {rows, truncated?}, parsed at end of body.  truncated: true is the row count.
	function jsonParser(opts) {
		const cols = declared(opts);
		const unbom = bomStripper();
		let text = "", dims = cols, sentinel = null;
		return {
			push(t) {
				text += unbom(t);
				return [];
			},
			end() {
				let v;
				try {
					v = JSON.parse(text);
				} catch (e) {
					throw parseError(0, "the body is not JSON");
				}
				const rows = Array.isArray(v) ? v : v && typeof v === "object" && Array.isArray(v.rows) ? v.rows : null;
				if (!rows) throw parseError(0, "the body is not an array or {rows}");
				rows.forEach((r, i) => {
					if (!r || typeof r !== "object" || Array.isArray(r))
						throw parseError(i + 1, "the row is not a JSON object");
				});
				if (!Array.isArray(v))
					sentinel = v.truncated === true ? rows.length : isCount(v.truncated) ? v.truncated : null;
				if (!dims) dims = inferObjects(rows.slice(0, INFER_ROWS));
				return rows;
			},
			dims: () => dims,
			layout: () => null,
			truncated: () => sentinel
		};
	}

	builtin("parser", "csv", Object.freeze({ create: csvParser }));
	builtin("parser", "ndjson", Object.freeze({ create: ndjsonParser }));
	builtin("parser", "json", Object.freeze({ create: jsonParser }));

	Object.assign(util, { csvParser: csvParser, ndjsonParser: ndjsonParser, jsonParser: jsonParser });

	// ---- E. the columnar builder and frames ------------------------------------------------------------------------

	const INITIAL = 1024;

	// A finite number, or NaN: strings must be numeric text (no blanks, hex, or Infinity), other types are NaN.
	function toNumber(v) {
		if (typeof v === "string") v = NUMBER_RE.test(v) ? Number(v) : NaN;
		return typeof v === "number" && Number.isFinite(v) ? v : NaN;
	}

	const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}/;

	// A date and time with no zone: Date.parse would read it as local time, but every time here is UTC.
	const NO_ZONE_RE = /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;

	// Epoch ms of numeric text, or of ISO-8601 text (it starts with YYYY-MM-DD; no zone means UTC); else NaN.
	function textTime(v) {
		if (NUMBER_RE.test(v)) return Number(v);
		if (!ISO_DATE_RE.test(v)) return NaN;
		return Date.parse(NO_ZONE_RE.test(v) ? v.replace(" ", "T") + "Z" : v);
	}

	// A time is epoch ms: a number, or numeric text read as the same number.  Other text must be ISO-8601 (see
	// textTime); anything else is NaN.
	function toTime(v) {
		if (typeof v === "string") v = textTime(v);
		return typeof v === "number" && Number.isFinite(v) ? v : NaN;
	}

	function toBool(v) {
		if (v === true || v === 1 || v === "true" || v === "1") return 1;
		if (v === false || v === 0 || v === "false" || v === "0") return 0;
		return -1;
	}

	// A string column's interning table: code(v) is the dict index of String(v), or -1 for null.
	function strings() {
		const dict = [], index = new Map();
		return {
			dict: dict,
			code(v) {
				if (v == null) return -1;
				const k = typeof v === "object" ? JSON.stringify(v) : String(v);
				let c = index.get(k);
				if (c === undefined) {
					c = dict.length;
					dict.push(k);
					index.set(k, c);
				}
				return c;
			}
		};
	}

	function grown(a, n) {
		if (n < a.length) return a;
		const b = new a.constructor(a.length * 2);
		b.set(a);
		return b;
	}

	function copyDims(dims) {
		return dims.map(d => ({ name: d.name, type: d.type }));
	}

	// The default builder.  Columns grow by doubling; finish() and snapshot() copy them out trimmed, so a frame never
	// shares a buffer with the builder or a cache.  layout names an array record's positions (CSV); object
	// records are read by own key, a missing key being null and extra keys ignored.
	function columnarBuilder(dims, opts) {
		const layout = opts && Array.isArray(opts.layout) ? opts.layout : null;
		const cols = dims.map(d => ({
			name: d.name, type: d.type, at: layout ? layout.indexOf(d.name) : -1,
			data: d.type === "string" ? new Int32Array(INITIAL)
				: d.type === "bool" ? new Int8Array(INITIAL) : new Float64Array(INITIAL),
			str: d.type === "string" ? strings() : null
		}));
		let n = 0;

		function onRecord(r) {
			const arr = Array.isArray(r);
			for (const c of cols) {
				const v = arr ? (c.at >= 0 ? r[c.at] : null) : OWN.call(r, c.name) ? r[c.name] : null;
				c.data = grown(c.data, n);
				c.data[n] = c.str ? c.str.code(v)
					: c.type === "bool" ? toBool(v) : c.type === "time" ? toTime(v) : toNumber(v);
			}
			n++;
		}

		function frame() {
			const columns = Object.create(null);
			for (const c of cols)
				columns[c.name] = c.str ? { codes: c.data.slice(0, n), dict: c.str.dict.slice() } : c.data.slice(0, n);
			return { dims: copyDims(dims), rowCount: n, truncated: false, columns: columns };
		}

		return { onRecord: onRecord, rowCount: () => n, finish: frame, snapshot: frame };
	}

	function sameType(f, d) {
		const fd = (f.dims || []).find(x => x.name === d.name);
		return !!fd && fd.type === d.type && OWN.call(f.columns, d.name) && f.columns[d.name] != null;
	}

	// The generation's frame: the chunk frames in time order, merged by dim name into fresh arrays.  A chunk without
	// a dim, or with another type for it, contributes nulls.  Strings are re-interned into one dict.  This works on
	// typed arrays from another realm (a structured clone), because it only calls subarray/set and reads by index.
	function concatFrames(dims, frames) {
		const total = frames.reduce((a, f) => a + f.rowCount, 0);
		const columns = Object.create(null);
		for (const d of dims) {
			let o = 0;
			if (d.type === "string") {
				const codes = new Int32Array(total), str = strings();
				for (const f of frames) {
					const src = sameType(f, d) ? f.columns[d.name] : null;
					const remap = src ? src.dict.map(k => str.code(k)) : null;
					for (let i = 0; i < f.rowCount; i++) {
						const c = src ? src.codes[i] : -1;
						codes[o++] = c < 0 ? -1 : remap[c];
					}
				}
				columns[d.name] = { codes: codes, dict: str.dict };
			} else {
				const out = d.type === "bool" ? new Int8Array(total) : new Float64Array(total);
				for (const f of frames) {
					if (sameType(f, d)) out.set(f.columns[d.name].subarray(0, f.rowCount), o);
					else out.fill(d.type === "bool" ? -1 : NaN, o, o + f.rowCount);
					o += f.rowCount;
				}
				columns[d.name] = out;
			}
		}
		return { dims: copyDims(dims), rowCount: total, truncated: frames.some(f => !!f.truncated), columns: columns };
	}

	const IS_TYPE = {
		number: v => v === null || typeof v === "number",
		time: v => v === null || typeof v === "number",
		string: v => v === null || typeof v === "string",
		bool: v => v === null || typeof v === "boolean"
	};

	// Null for a body that meets the aggregate contract, else the reason for E-JS-96.
	function aggregateProblem(b) {
		if (!b || typeof b !== "object" || Array.isArray(b)) return "the body is not an object";
		if (b.schemaVersion !== 1) return "schemaVersion is not 1";
		if (!Array.isArray(b.dims)) return "dims is not an array";
		if (!b.columns || typeof b.columns !== "object" || Array.isArray(b.columns)) return "columns is not an object";
		if (typeof b.truncated !== "boolean") return "truncated is not a boolean";
		let len = -1;
		const seenDims = new Set();
		for (const d of b.dims) {
			if (!d || typeof d.name !== "string" || TYPES.indexOf(d.type) < 0) return "a dim is not {name, type}";
			if (seenDims.has(d.name)) return "duplicate dim '" + d.name + "'";
			seenDims.add(d.name);
			const col = OWN.call(b.columns, d.name) ? b.columns[d.name] : null;
			if (!Array.isArray(col)) return "dim '" + d.name + "' has no column";
			if (len >= 0 && col.length !== len) return "the columns have unequal lengths";
			len = col.length;
			if (!col.every(IS_TYPE[d.type])) return "column '" + d.name + "' holds a value that is not a " + d.type;
		}
		if (!b.dims.some(d => d.name === "bucket" && d.type === "time")) return "there is no bucket time dim";
		return null;
	}

	// A checked aggregate body as a frame; it skips the builder.
	function frameFromColumns(b) {
		const n = b.dims.length ? b.columns[b.dims[0].name].length : 0;
		const columns = Object.create(null);
		for (const d of b.dims) {
			const src = b.columns[d.name];
			if (d.type === "string") {
				const str = strings();
				columns[d.name] = { codes: Int32Array.from(src, v => str.code(v)), dict: str.dict };
			} else if (d.type === "bool") columns[d.name] = Int8Array.from(src, v => v === null ? -1 : v ? 1 : 0);
			else columns[d.name] = Float64Array.from(src, v => v === null ? NaN : v);
		}
		return { dims: copyDims(b.dims), rowCount: n, truncated: b.truncated, columns: columns };
	}

	// One cell as a JS value: null for NaN, -1 codes and -1 bools.
	function readCell(frame, d, i) {
		const col = OWN.call(frame.columns, d.name) ? frame.columns[d.name] : null;
		if (col == null) return null;
		if (d.type === "string") return col.codes[i] < 0 ? null : col.dict[col.codes[i]];
		if (d.type === "bool") return col[i] < 0 ? null : col[i] === 1;
		return Number.isNaN(col[i]) ? null : col[i];
	}

	function rowsOf(frame) {
		const rows = [];
		for (let i = 0; i < frame.rowCount; i++) {
			const r = Object.create(null);
			for (const d of frame.dims) r[d.name] = readCell(frame, d, i);
			rows.push(r);
		}
		return rows;
	}

	builtin("builder", "columnar", Object.freeze({ create: columnarBuilder }));

	Object.assign(util, { columnarBuilder: columnarBuilder, concatFrames: concatFrames,
		aggregateProblem: aggregateProblem, frameFromColumns: frameFromColumns, readCell: readCell, rowsOf: rowsOf,
		strings: strings });

	// ---- F. time units and chunk lists ------------------------------------------------------------------------------

	const HOUR = 3600000, DAY = 24 * HOUR;
	const UNITS = ["hour", "day", "week", "month"];
	// The aggregate rule: mirrors SeriesAggregator.AUTO_BUCKETS.
	const AUTO_BUCKETS = 1000;

	// An ISO-8601 instant, epoch ms, or epoch ms as digits; null for anything else (the same rule as toTime).
	function parseTime(v) {
		const t = typeof v === "number" ? v : typeof v === "string" ? textTime(v) : NaN;
		return Number.isFinite(t) ? t : null;
	}

	// The start of t's UTC unit; any unit other than hour, day or week is month.  weekStart is a Date.getUTCDay index
	// (an integer 0-6), Monday (1) by default and for any other value.
	function floorUnit(t, unit, weekStart) {
		if (unit === "hour") return t - (((t % HOUR) + HOUR) % HOUR);
		const day = t - (((t % DAY) + DAY) % DAY);
		if (unit === "day") return day;
		if (unit === "week") {
			const ws = Number.isInteger(weekStart) && weekStart >= 0 && weekStart <= 6 ? weekStart : 1;
			return day - ((new Date(day).getUTCDay() - ws + 7) % 7) * DAY;
		}
		const d = new Date(day);
		return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), 1);
	}

	// The start of the unit after the one starting at t (a unit boundary); an unknown unit is month.
	function nextUnit(t, unit) {
		if (unit === "hour") return t + HOUR;
		if (unit === "day") return t + DAY;
		if (unit === "week") return t + 7 * DAY;
		const d = new Date(t);
		return Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 1);
	}

	// The smallest unit giving at most AUTO_BUCKETS buckets over [start, end); month when none does.  Counting stops at
	// AUTO_BUCKETS + 1, as on the server.
	function autoUnit(start, end, weekStart) {
		for (const u of UNITS) {
			let n = 0;
			for (let t = floorUnit(start, u, weekStart); t < end && n <= AUTO_BUCKETS; t = nextUnit(t, u))
				n++;
			if (n <= AUTO_BUCKETS) return u;
		}
		return "month";
	}

	// The UTC units covering [start, end), clipped to it.  Weeks are always Monday weeks.  A clipped chunk's key
	// carries its end, so it never satisfies a later full-unit read.  closed: the chunk ends at or before now.  An
	// empty or reversed window has no chunks.
	function chunkList(start, end, unit, now) {
		const out = [];
		if (!(end > start)) return out;
		for (let u = floorUnit(start, unit); u < end; u = nextUnit(u, unit)) {
			const next = nextUnit(u, unit);
			const s = Math.max(u, start), e = Math.min(next, end);
			const full = s === u && e === next;
			out.push({ key: full ? String(s) : s + "-" + e, start: s, end: e, full: full, closed: e <= now,
				label: iso(s) });
		}
		return out;
	}

	// The overall window is the params values under the chunk's two names; {missing: true} until both parse.
	function windowOf(params, chunk) {
		const s = parseTime(OWN.call(params, chunk.start) ? params[chunk.start] : null);
		const e = parseTime(OWN.call(params, chunk.end) ? params[chunk.end] : null);
		return s === null || e === null ? { missing: true } : { start: s, end: e };
	}

	Object.assign(util, { parseTime: parseTime, floorUnit: floorUnit, nextUnit: nextUnit, autoUnit: autoUnit,
		chunkList: chunkList, windowOf: windowOf });

	// ---- G. the chunk cache -----------------------------------------------------------------------------------------

	const DB_NAME = "juneau-data", STORE = "chunks", DB_VERSION = 1, SCHEMA_VERSION = 1;
	const MAX_AGE = 30 * DAY, OPEN_TIMEOUT = 5000;
	let dbOpen = null, warned101 = false;

	function txDone(tx) {
		return new Promise((resolve, reject) => {
			// A failed request ends in an abort, and tx.error is only set by then (not yet in onerror).
			tx.oncomplete = () => resolve();
			tx.onabort = () => reject(tx.error || new Error("the transaction was aborted"));
		});
	}

	// Deletes records older than 30 days by fetchedAt, walking the fetchedAt index by key so no chunk is read.  Silent
	// on failure: the cache stays usable, and nothing waits on this.
	function sweep(db) {
		try {
			const tx = db.transaction([STORE], "readwrite");
			const store = tx.objectStore(STORE);
			const cutoff = config.timers.now() - MAX_AGE;
			const cur = store.index("fetchedAt").openKeyCursor(config.platform.IDBKeyRange.upperBound(cutoff, true));
			cur.onsuccess = () => {
				const c = cur.result;
				if (!c) return;
				store.delete(c.primaryKey);
				c.continue();
			};
			txDone(tx).catch(() => {});
		} catch (e) {
			// An old or odd database: leave the records for the next open.
		}
	}

	// One open per page: {db}, or {db: null, reason}.  The sweep starts at open and is not waited for.
	function openDb() {
		if (dbOpen) return dbOpen;
		const idb = config.platform.indexedDB;
		if (!idb) return (dbOpen = Promise.resolve({ db: null, reason: "no indexedDB" }));
		dbOpen = new Promise(resolve => {
			let timer = null, late = false;
			const fail = e => {
				config.timers.clearTimeout(timer);
				resolve({ db: null, reason: "open failed: " + msgOf(e) });
			};
			let r;
			try {
				r = idb.open(DB_NAME, DB_VERSION);
			} catch (e) {
				fail(e);
				return;
			}
			// An open that never answers (some private modes) must not hold every read forever.
			timer = config.timers.setTimeout(() => {
				late = true;
				fail("timeout");
			}, OPEN_TIMEOUT);
			r.onupgradeneeded = () => {
				if (r.result.objectStoreNames.contains(STORE)) return;
				r.result.createObjectStore(STORE, { keyPath: "key" }).createIndex("fetchedAt", "fetchedAt");
			};
			r.onsuccess = () => {
				if (late) {
					r.result.close();
					return;
				}
				config.timers.clearTimeout(timer);
				r.result.onversionchange = () => r.result.close();
				sweep(r.result);
				resolve({ db: r.result });
			};
			r.onerror = () => fail(r.error);
		});
		return dbOpen;
	}

	function bytesOf(rec) {
		let n = 0;
		for (const k of Object.keys(rec.columns)) {
			const c = rec.columns[k];
			n += c.codes ? c.codes.byteLength : c.byteLength;
		}
		return n;
	}

	// Least recently used first out, past maxChunks entries or maxBytes of typed arrays.
	function memoryLru(maxChunks, maxBytes) {
		const m = new Map();
		let bytes = 0;
		function del(key) {
			const e = m.get(key);
			if (!e) return;
			m.delete(key);
			bytes -= e.bytes;
		}
		return {
			get(key) {
				const e = m.get(key);
				if (!e) return undefined;
				m.delete(key);
				m.set(key, e);
				return e.rec;
			},
			set(key, rec) {
				del(key);
				const e = { rec: rec, bytes: bytesOf(rec) };
				// A chunk bigger than the whole cap is not kept, and does not push the others out.
				if (e.bytes > maxBytes) return;
				m.set(key, e);
				bytes += e.bytes;
				for (const k of m.keys()) {
					if (m.size <= maxChunks && bytes <= maxBytes) break;
					del(k);
				}
			},
			drop(prefix) {
				for (const k of Array.from(m.keys())) if (k.startsWith(prefix)) del(k);
			},
			size: () => m.size,
			bytes: () => bytes
		};
	}

	// The page-wide chunk store behind cache: memory, and behind indexedDb after a fallback.
	const memory = memoryLru(64, 200 * 1024 * 1024);

	// Stable JSON text for JSON-like values only (strings, finite numbers, booleans, null, arrays, plain objects):
	// object keys sorted, undefined keys dropped.  A Date or other class instance would not be told apart.
	function canonicalJson(v) {
		if (Array.isArray(v)) return "[" + v.map(canonicalJson).join(",") + "]";
		if (v && typeof v === "object")
			return "{" + Object.keys(v).sort().filter(k => v[k] !== undefined)
				.map(k => JSON.stringify(k) + ":" + canonicalJson(v[k])).join(",") + "}";
		return JSON.stringify(v === undefined ? null : v);
	}

	// One stored chunk.  The columns are the chunk frame's own; concatFrames copies them, and IndexedDB clones them.
	function cacheRecord(ns, chunk, frame, epoch) {
		return { key: ns + "|" + chunk.key, ns: ns, chunkStart: chunk.start, epoch: epoch == null ? null : epoch,
			schemaVersion: SCHEMA_VERSION, dims: copyDims(frame.dims), columns: frame.columns, rowCount: frame.rowCount,
			fetchedAt: config.timers.now() };
	}

	// A stored record that can be read back as a chunk (its shape only); anything else is a miss.
	function usable(r) {
		return !!r && r.schemaVersion === SCHEMA_VERSION && Array.isArray(r.dims)
			&& !!r.columns && typeof r.columns === "object";
	}

	// A source's cache.  mode is the def's cache: indexedDb, memory or none.  kind() is the handle's cache value:
	// indexedDb, memory, memory-fallback, or null for none.
	function sourceCache(id, mode) {
		let kind = mode === "none" ? null : mode;
		let opened = null;

		function fallback(reason) {
			if (kind !== "indexedDb") return;
			kind = "memory-fallback";
			if (warned101) return;
			warned101 = true;
			config.log.warn(dataError("E-JS-101", id, reason).message);
		}

		// The database, or null once this source is not on IndexedDB.
		function open() {
			if (opened) return opened;
			if (kind !== "indexedDb") opened = Promise.resolve(null);
			else if (!config.platform.subtle || !config.platform.TextEncoder) {
				fallback("no crypto.subtle");
				opened = Promise.resolve(null);
			} else opened = openDb().then(r => {
				if (!r.db) fallback(r.reason);
				return r.db;
			});
			return opened;
		}

		// The window params are not part of the namespace, so every window shares one set of chunk records.
		async function namespace(def, params) {
			const win = def.chunk ? [def.chunk.start, def.chunk.end] : [];
			const p = Object.create(null);
			for (const k of Object.keys(params || {})) if (win.indexOf(k) < 0 && params[k] != null) p[k] = params[k];
			const text = canonicalJson({ url: def.url, params: p, columns: def.columns || null, format: def.parser,
				chunk: def.chunk ? def.chunk.unit : null, schemaVersion: SCHEMA_VERSION });
			const subtle = config.platform.subtle, TE = config.platform.TextEncoder;
			if (!subtle || !TE) return text;
			const buf = await subtle.digest("SHA-256", new TE().encode(text));
			return Array.from(new Uint8Array(buf), b => (b < 16 ? "0" : "") + b.toString(16)).join("").slice(0, 32);
		}

		function idbGet(db, keys) {
			const tx = db.transaction([STORE], "readonly"), store = tx.objectStore(STORE);
			const out = keys.map(() => undefined);
			keys.forEach((k, i) => {
				const r = store.get(k);
				r.onsuccess = () => { out[i] = r.result; };
			});
			return txDone(tx).then(() => out);
		}

		// A record read from IndexedDB is a structured clone, so its columns lose their null prototype.
		function detached(r) {
			if (usable(r)) r.columns = Object.assign(Object.create(null), r.columns);
			return r;
		}

		// Every listed chunk's record, in one transaction: a Map from chunk key to record.  With a known epoch, a
		// record from another epoch (or none) makes the read a miss and drops the namespace.
		async function read(ns, chunks, epoch) {
			const found = new Map();
			if (!kind || !chunks.length) return found;
			const db = await open();
			const keys = chunks.map(c => ns + "|" + c.key);
			let recs = null;
			if (db && kind === "indexedDb") {
				try {
					recs = (await idbGet(db, keys)).map(detached);
				} catch (e) {
					fallback("read failed: " + msgOf(e));
				}
			}
			if (!recs) recs = keys.map(k => memory.get(k));
			recs = recs.map(r => usable(r) ? r : null);
			if (epoch != null && recs.some(r => r && r.epoch !== epoch)) {
				await drop(ns);
				return found;
			}
			chunks.forEach((c, i) => { if (recs[i]) found.set(c.key, recs[i]); });
			return found;
		}

		// Stores finished chunk records.  A failed IndexedDB write (QuotaExceededError or any other) falls back for the
		// page and keeps the records in memory.
		async function write(records) {
			if (!kind || !records.length) return;
			const db = await open();
			if (db && kind === "indexedDb") {
				let tx = null;
				try {
					tx = db.transaction([STORE], "readwrite");
					const store = tx.objectStore(STORE);
					for (const r of records) store.put(r);
					await txDone(tx);
					return;
				} catch (e) {
					if (tx) try { tx.abort(); } catch (ignored) { /* already finished or aborted */ }
					fallback(e && e.name === "QuotaExceededError" ? "quota exceeded" : "write failed: " + msgOf(e));
				}
			}
			for (const r of records) memory.set(r.key, r);
		}

		// Every record of the namespace, from memory and IndexedDB (clearCache, an epoch change).
		async function drop(ns) {
			memory.drop(ns + "|");
			const db = await open();
			if (!db || kind !== "indexedDb") return;
			try {
				const tx = db.transaction([STORE], "readwrite");
				tx.objectStore(STORE).delete(config.platform.IDBKeyRange.bound(ns + "|", ns + "|\uffff"));
				await txDone(tx);
			} catch (e) {
				fallback("delete failed: " + msgOf(e));
			}
		}

		return { kind: () => kind, open: open, namespace: namespace, read: read, write: write, drop: drop };
	}

	Object.assign(util, { canonicalJson: canonicalJson, cacheRecord: cacheRecord, memoryLru: memoryLru,
		sourceCache: sourceCache });

	// ---- H. DataSource: generations, chunks, AUTO and aggregate -----------------------------------------------------

	const PROGRESS_MS = 250, MAX_CONCURRENCY = 6;
	// The most chunks one window may ask for.  A wider window is refused (or, in AUTO, aggregated) before any chunk
	// list is built.
	const MAX_CHUNKS = 10000;

	// The error that ends a generation which is no longer current.  It carries its own marker: an AbortError from
	// elsewhere is a failure of the load, not a supersede.
	function supersededError() {
		const e = new Error("superseded");
		e.name = "AbortError";
		e.superseded = true;
		return e;
	}

	// True when [start, end) spans more than limit chunks of the unit.  It stops counting at limit + 1, so a huge
	// window is never walked or listed in full.
	function overChunkLimit(start, end, unit, limit) {
		if (!(end > start)) return false;
		let n = 0;
		for (let u = floorUnit(start, unit); u < end; u = nextUnit(u, unit))
			if (++n > limit) return true;
		return false;
	}

	// Each map value is a resolvePath path into the payload; {empty: true} while any of them is missing.
	function mapParams(bus, sub, payload) {
		if (payload == null) return { empty: true };
		const value = Object.create(null);
		for (const k of Object.keys(sub.map)) {
			const v = bus.util.resolvePath(payload, sub.map[k]);
			if (v == null) return { empty: true };
			value[k] = v;
		}
		return { value: value };
	}

	function sameValue(a, b) {
		if (a === b) return true;
		return !!a && !!b && typeof a === "object" && typeof b === "object" && canonicalJson(a) === canonicalJson(b);
	}

	// A CSV record (an array in header order) as the object an app builder takes.
	function objectRecord(r, layout) {
		if (!Array.isArray(r)) return r;
		const o = Object.create(null);
		layout.forEach((name, i) => { o[name] = i < r.length ? r[i] : null; });
		return o;
	}

	// readBody, with a stream failure as E-JS-62.  An abort by the generation's own signal, a parse error (E-JS-99) or
	// a superseded generation passes through.
	function bodyOf(id, res, signal, onText) {
		return readBody(res, onText).catch(e => {
			const own = e && (e.superseded || e.problem || (e.name === "AbortError" && signal && signal.aborted));
			throw own ? e : dataError("E-JS-62", id, msgOf(e));
		});
	}

	// One DataSource.  def is a dataSources[] entry of kind source; partial: a partialFrames card reads it.  Every
	// trigger starts a new generation; every async step of a generation re-checks that it is still the current one
	// (live), so a superseded or disposed generation never publishes, writes the cache or logs.
	function createSource(entry, bus, opts) {
		// A field the entry leaves out takes the Java DataSourceDef default (concurrency 1 and mode raw are applied
		// where they are read).
		const def = Object.assign({}, entry, { parser: entry.parser || "csv", builder: entry.builder || "columnar",
			cache: entry.cache || "memory", debounceMs: entry.debounceMs == null ? 250 : entry.debounceMs });
		const id = def.id, topic = "data:" + id;
		const own = bus.owner("data-source:" + id);
		const partial = !!(opts && opts.partial);
		const cache = sourceCache(id, def.cache);
		const columnar = def.builder === "columnar";
		const params = Object.create(null);
		let gen = 0, run = null, timer = null, pendingClear = false, shown = null, disposed = false;

		// Publishes a handle for this source (the bus drops an identical repeat).  The frame fields describe the frame
		// at version; a loading handle's mode is its generation's.
		function publish(st, status, extra) {
			const h = { schemaVersion: 1, source: id, version: shown ? shown.version : 0, status: status,
				progress: null, rowCount: shown ? shown.rowCount : 0, truncated: shown ? shown.truncated : false,
				truncatedAt: shown ? shown.truncatedAt : null, dims: copyDims(shown ? shown.dims : def.columns || []),
				error: null };
			if (cache.kind()) h.cache = cache.kind();
			const mode = status === "loading" ? st.mode : shown && shown.mode;
			if (mode) h.mode = mode;
			if (shown) h.loadedAt = shown.loadedAt;
			own.publish(topic, Object.assign(h, extra));
		}

		function progressOf(st) {
			return st.total == null ? { loaded: st.loaded } : { loaded: st.loaded, total: st.total };
		}

		function live(st) {
			if (st.g !== gen || st.stopped || disposed) throw supersededError();
		}

		// A clearCache request that never reached the drop is asked for again by the next generation.
		function keepClear(st) {
			if (st.clearCache && !st.cleared) pendingClear = true;
		}

		function fail(st, p) {
			run = null;
			keepClear(st);
			building(id, null);
			config.log.error(p.code + ": " + p.message);
			publish(st, shown ? "stale" : "error", { error: p });
		}

		// Abort this generation for a switch to AGGREGATE; the caller throws the result, and start's catch (not the
		// generation) then loads the aggregate for the same window with no debounce.
		function switchTo(st) {
			st.switch = st.stopped = true;
			st.ctl.abort();
			return supersededError();
		}

		function schedule(clear) {
			if (disposed) return;
			gen++;
			if (run) {
				run.stopped = true;
				run.ctl.abort();
				keepClear(run);
				run = null;
				if (partial) building(id, null);
			}
			pendingClear = pendingClear || !!clear;
			if (timer !== null) config.timers.clearTimeout(timer);
			const g = gen;
			timer = config.timers.setTimeout(() => {
				timer = null;
				const clearCache = pendingClear;
				pendingClear = false;
				start(g, def.mode === "aggregate" ? "aggregate" : "raw", clearCache);
			}, def.debounceMs);
		}

		function start(g, mode, clearCache) {
			if (g !== gen || disposed) return;
			const AC = config.platform.AbortController;
			const st = { g: g, ctl: AC ? new AC() : { signal: null, abort() {} }, mode: mode,
				auto: def.mode === "auto" && mode === "raw", clearCache: clearCache, cleared: false, stopped: false,
				failure: null, parseFailure: null, loaded: 0, total: null, lastTick: 0,
				params: Object.assign(Object.create(null), params), done: new Map(), active: new Map(), writes: [],
				epoch: null, ns: null, cap: null };
			run = st;
			(mode === "aggregate" ? runAggregate(st) : runRaw(st)).catch(e => {
				if (st.g !== gen || disposed) return;
				try {
					if (st.switch) {
						// The raw attempt is over: its clear request (if the drop never ran) and its partial frame go.
						keepClear(st);
						if (partial) building(id, null);
						gen++;
						start(gen, "aggregate", false);
					} else
						fail(st, e && e.problem || problem("E-JS-98", id, "source", def.url || id, msgOf(e)));
				} catch (x) {
					config.log.error("E-JS-98: " + msgOf(x));
				}
			});
		}

		// No request while a token or the window is missing; the handle stays loading with nothing loaded.
		function awaiting(st) {
			run = null;
			keepClear(st);
			publish(st, "loading", { progress: { loaded: 0 } });
		}

		function windowParams(c) {
			const x = Object.create(null);
			if (def.chunk) {
				x[def.chunk.start] = iso(c.start);
				x[def.chunk.end] = iso(c.end);
			}
			return x;
		}

		// One GET per generation; the value at path, canonical JSON for an object, null when missing.
		async function probeEpoch(st) {
			const res = await fetchData(id, def.epoch.url, st.ctl.signal, ACCEPT.json);
			live(st);
			let text = "";
			await bodyOf(id, res, st.ctl.signal, t => { text += t; });
			live(st);
			let body;
			try {
				body = JSON.parse(text);
			} catch (e) {
				throw dataError("E-JS-62", id, "the epoch response is not JSON");
			}
			const v = bus.util.resolvePath(body, def.epoch.path);
			return v == null ? null : typeof v === "object" ? canonicalJson(v) : String(v);
		}

		// Throttled progress: at most every PROGRESS_MS, a partial snapshot first when a card reads one.
		function tick(st) {
			const now = config.timers.now();
			if (now - st.lastTick < PROGRESS_MS) return;
			st.lastTick = now;
			if (partial) building(id, partialFrame(st));
			publish(st, "loading", { progress: progressOf(st) });
		}

		// The declared columns, else the dims of the earliest chunk (in time order) that has rows.  With inferred dims
		// the first chunk with rows wins for the whole frame, so a schema change in later (or cached) chunks does not
		// change the dims until the cache namespace changes; chunks that differ are padded with nulls.
		function generationDims(parts) {
			if (def.columns) return copyDims(def.columns);
			const first = parts.find(p => p.rowCount > 0) || parts[0];
			return first ? first.dims : [];
		}

		// The building frame: finished chunks and running builders' snapshots, in time order.
		function partialFrame(st) {
			if (!columnar) return null;
			const parts = [];
			for (const c of st.chunks) {
				if (st.done.has(c.key)) parts.push(st.done.get(c.key));
				else if (st.active.has(c.key)) parts.push(st.active.get(c.key).snapshot());
			}
			return concatFrames(generationDims(parts), parts);
		}

		async function runRaw(st) {
			const parser = lookup("parser", def.parser);
			if (!parser.impl) throw dataError("E-JS-92", id, def.parser, parser.names.join(", "));
			st.parser = parser.impl;
			if (!columnar) {
				const b = lookup("builder", def.builder);
				if (!b.impl) throw dataError("E-JS-93", id, def.builder, b.names.join(", "));
				st.builder = b.impl;
			}
			let w = null;
			if (def.chunk) {
				w = windowOf(st.params, def.chunk);
				if (w.missing) return awaiting(st);
			}
			if (resolveUrl(id, def.url, st.params, windowParams({ start: 0, end: 0 })).missing) return awaiting(st);
			if (w && overChunkLimit(w.start, w.end, def.chunk.unit, MAX_CHUNKS)) {
				// Too many chunks for RAW: AUTO aggregates the window, every other mode refuses it.
				if (st.auto && def.aggregateUrl) throw switchTo(st);
				throw dataError("E-JS-107", id, MAX_CHUNKS);
			}
			st.chunks = w ? chunkList(w.start, w.end, def.chunk.unit, config.timers.now())
				: [{ key: "all", start: 0, end: 0, full: false, closed: false, label: "all" }];

			// The epoch, then one cache read for the full closed chunks, then loading with the cached rows.
			st.store = columnar && cache.kind() !== null;
			if (def.epoch && st.store) st.epoch = await probeEpoch(st);
			live(st);
			if (st.store) {
				st.ns = await cache.namespace(def, st.params);
				live(st);
				if (st.clearCache) {
					await cache.drop(st.ns);
					st.cleared = true;
					live(st);
				}
				const hits = await cache.read(st.ns, st.chunks.filter(c => c.full && c.closed), st.epoch);
				live(st);
				hits.forEach((rec, key) => {
					st.done.set(key, { dims: rec.dims, rowCount: rec.rowCount, truncated: false,
						columns: rec.columns });
					st.loaded += rec.rowCount;
				});
			}
			if (st.auto && def.aggregateUrl && def.maxRows != null && st.loaded > def.maxRows) throw switchTo(st);
			st.lastTick = config.timers.now();
			publish(st, "loading", { progress: progressOf(st) });

			// The missing chunks, newest-first; a parse failure fails its chunk only.
			await pool(st, st.chunks.filter(c => !st.done.has(c.key)).reverse());
			await Promise.all(st.writes);
			live(st);
			if (st.parseFailure) throw st.parseFailure;

			// The merge, in time order.
			const parts = st.chunks.map(c => st.done.get(c.key));
			if (columnar) return finish(st, concatFrames(generationDims(parts), parts));
			let f;
			try {
				const b = st.builder.create(generationDims(parts), {});
				for (const p of parts) for (const r of p.records) b.onRecord(r);
				f = b.finish();
			} catch (e) {
				throw dataError("E-JS-98", id, "builder", def.builder, msgOf(e));
			}
			finish(st, Object.assign({}, f, { truncated: !!f.truncated || parts.some(p => p.truncated) }));
		}

		// concurrency workers over items.  A fetch error (or a switch, or a newer generation) stops every worker; a
		// parse error is kept and the rest go on.  Every worker settles before this returns or throws.
		async function pool(st, items) {
			let next = 0;
			const worker = async () => {
				while (next < items.length && !st.stopped && st.g === gen) {
					try {
						await loadChunk(st, items[next++]);
					} catch (e) {
						if (e && e.parse) {
							if (!st.parseFailure) st.parseFailure = e;
							continue;
						}
						if (!st.failure) st.failure = e;
						st.stopped = true;
						st.ctl.abort();
					}
				}
			};
			const n = Math.min(Math.max(def.concurrency || 1, 1), MAX_CONCURRENCY, items.length);
			const workers = [];
			for (let i = 0; i < n; i++) workers.push(worker());
			await Promise.all(workers);
			if (st.failure) throw st.failure;
		}

		async function loadChunk(st, c) {
			const u = resolveUrl(id, def.url, st.params, windowParams(c));
			const res = await fetchData(id, u.url, st.ctl.signal, ACCEPT[def.parser] || "*/*");
			live(st);
			if (!def.chunk && res.headers) {
				const total = res.headers.get("X-Total-Rows");
				if (total != null && /^\d+$/.test(total)) st.total = Number(total);
			}
			const p = st.parser.create({ columns: def.columns || null });
			const records = [];
			let b = null;
			const parse = fn => {
				try {
					return fn();
				} catch (e) {
					const x = dataError("E-JS-99", id, c.label, e && e.record != null ? e.record : "?", msgOf(e));
					x.parse = true;
					throw x;
				}
			};
			const take = list => {
				live(st);
				if (!list.length) return;
				if (columnar && !b) {
					b = columnarBuilder(p.dims() || [], { layout: p.layout() });
					st.active.set(c.key, b);
				}
				for (const r of list) {
					if (b) b.onRecord(r);
					else records.push(objectRecord(r, p.layout()));
				}
				st.loaded += list.length;
				if (st.auto && def.aggregateUrl && def.maxRows != null && st.loaded > def.maxRows) throw switchTo(st);
				tick(st);
			};
			try {
				await bodyOf(id, res, st.ctl.signal, t => take(parse(() => p.push(t))));
				take(parse(() => p.end()));
			} finally {
				st.active.delete(c.key);
			}
			const cap = p.truncated();
			if (cap !== null) {
				if (st.auto && def.aggregateUrl) throw switchTo(st);
				// Chunks finish in any order; the largest cap is the one reported.
				st.cap = st.cap === null ? cap : Math.max(st.cap, cap);
			}
			let f;
			if (columnar)
				f = Object.assign((b || columnarBuilder(p.dims() || [], {})).finish(), { truncated: cap !== null });
			else f = { dims: p.dims() || [], rowCount: records.length, truncated: cap !== null, records: records };
			// Only a full, closed, untruncated chunk of the current generation is written.  A failed write is the
			// cache's own business (it falls back to memory) and never fails the load.
			if (st.store && c.full && c.closed && cap === null && st.g === gen && !st.stopped)
				st.writes.push(cache.write([cacheRecord(st.ns, c, f, st.epoch)]).catch(() => { /* kept in memory */ }));
			st.done.set(c.key, f);
		}

		// One request for the window, straight into a frame.
		async function runAggregate(st) {
			publish(st, "loading", { progress: { loaded: 0 } });
			const names = def.chunk ? [def.chunk.start, def.chunk.end] : ["start", "end"];
			const w = windowOf(st.params, { start: names[0], end: names[1] });
			if (w.missing) return awaiting(st);
			const rest = Object.create(null);
			for (const k of Object.keys(st.params)) if (names.indexOf(k) < 0) rest[k] = st.params[k];
			const a = def.aggregate || {};
			const u = resolveUrl(id, def.aggregateUrl, rest, { start: iso(w.start), end: iso(w.end),
				bucket: a.bucket || "auto", groupBy: a.groupBy || [], measure: a.measures || [], tz: "UTC" });
			if (u.missing) return awaiting(st);
			const res = await fetchData(id, u.url, st.ctl.signal, ACCEPT.json);
			live(st);
			let text = "";
			await bodyOf(id, res, st.ctl.signal, t => {
				live(st);
				text += t;
			});
			live(st);
			let body;
			try {
				body = JSON.parse(text);
			} catch (e) {
				throw dataError("E-JS-96", id, "the body is not JSON");
			}
			const why = aggregateProblem(body);
			if (why) throw dataError("E-JS-96", id, why);
			const f = frameFromColumns(body);
			f.bucketUnit = a.bucket && a.bucket !== "auto" ? a.bucket : autoUnit(w.start, w.end);
			st.loaded = f.rowCount;
			finish(st, f);
		}

		// Publishes the finished frame: the building frame and a last loading handle (the final progress) come first
		// so a partial reader sees the complete frame, then the frame is put and the ready handle published.
		// truncatedAt is the largest single-chunk cap (st.cap), or the row count for a truncated aggregate; it is not
		// a total.
		function finish(st, f) {
			if (partial) building(id, f);
			publish(st, "loading", { progress: progressOf(st) });
			if (f.truncated) config.log.warn(dataError("E-JS-102", id, st.cap !== null ? st.cap : f.rowCount).message);
			f.mode = st.mode;
			f.loadedAt = iso(config.timers.now());
			const version = put(id, f);
			shown = { version: version, rowCount: f.rowCount, truncated: !!f.truncated,
				truncatedAt: f.truncated ? (st.cap !== null ? st.cap : f.rowCount) : null, dims: f.dims, mode: f.mode,
				loadedAt: f.loadedAt };
			run = null;
			publish(st, "ready", {});
		}

		// A params subscription with a map copies each mapped value into params, and a missing value deletes the map's
		// keys unless whenEmpty is keep.  Without a map it only starts a generation.
		// With a map, a message that leaves the mapped values as they were (arrays and objects compared by value)
		// starts nothing.  So an identical republish no longer retries a source in the error state; a retry comes from
		// cmd refresh or reload.
		function onParams(sub, payload) {
			if (sub.map && typeof sub.map === "object") {
				const m = mapParams(bus, sub, payload);
				let changed = false;
				if (m.empty) {
					if (sub.whenEmpty === "keep") return;
					for (const k of Object.keys(sub.map)) {
						if (k in params) changed = true;
						delete params[k];
					}
				} else {
					for (const k of Object.keys(m.value)) {
						if (k in params && sameValue(params[k], m.value[k])) continue;
						params[k] = m.value[k];
						changed = true;
					}
				}
				if (!changed) return;
			}
			schedule(false);
		}

		function dispose() {
			if (disposed) return;
			disposed = true;
			gen++;
			if (run) {
				run.stopped = true;
				run.ctl.abort();
				run = null;
			}
			if (timer !== null) {
				config.timers.clearTimeout(timer);
				timer = null;
			}
			own.dispose();
			release(id);
		}

		// A throw while wiring (a bad subscribes topic) leaves nothing behind: no retained loading handle, no timer.
		try {
			publish({ mode: def.mode === "aggregate" ? "aggregate" : "raw" }, "loading", { progress: { loaded: 0 } });
			for (const sub of def.subscribes || []) {
				if (sub.as === "refresh") own.subscribe(sub.topic, () => schedule(false), { retained: false });
				else if (sub.as === "params") own.subscribe(sub.topic, payload => onParams(sub, payload));
			}
			own.subscribe("cmd:" + id, payload => {
				const op = payload && payload.op;
				if (op === "refresh" || op === "reload") schedule(!!(payload.options && payload.options.clearCache));
			}, { retained: false });
			schedule(false);
		} catch (e) {
			dispose();
			throw e;
		}
		return { id: id, dispose: dispose };
	}

	Object.assign(util, { createSource: createSource, memoryCache: memory, overChunkLimit: overChunkLimit,
		MAX_CHUNKS: MAX_CHUNKS });

	// ---- I. Derived sources and the built-in transforms -------------------------------------------------------------

	const MEASURE_RE = /^(sum|avg|min|max)\((.+)\)$/;
	// The coarser units each unit nests in exactly (weeks do not nest in months).
	const NESTS = { hour: ["day", "week", "month"], day: ["week", "month"], week: [], month: [] };
	const WEEKDAYS = ["sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"];
	const TOKEN_ARG_RE = /^\{([A-Za-z0-9_.-]+)\}$/;

	function dimOf(frame, name) {
		const d = frame.dims.find(x => x.name === name);
		if (!d) throw new Error("the frame has no field '" + name + "'");
		return d;
	}

	// The dim an argument names.
	function dimArg(frame, args, key) {
		if (typeof args[key] !== "string" || args[key] === "") throw new Error("'" + key + "' is required");
		return dimOf(frame, args[key]);
	}

	// An optional list argument.
	function listArg(args, key) {
		const v = args[key];
		if (v == null) return null;
		if (!Array.isArray(v)) throw new Error("'" + key + "' must be a list");
		return v;
	}

	function measureOf(text) {
		if (text === "count") return { op: "count", field: null, text: text };
		const m = typeof text === "string" ? MEASURE_RE.exec(text) : null;
		if (!m) throw new Error("'" + text + "' is not a measure");
		return { op: m[1], field: m[2], text: text };
	}

	// The measures a transform computes, count when none are given.  Like the server, a measure repeated is an error.
	function measuresOf(frame, args) {
		const list = listArg(args, "measures");
		const seenText = new Set();
		return (list && list.length ? list : ["count"]).map(t => {
			const m = measureOf(t);
			if (seenText.has(m.text)) throw new Error("duplicate measure '" + m.text + "'");
			seenText.add(m.text);
			if (m.field !== null) {
				m.dim = dimOf(frame, m.field);
				if (m.dim.type !== "number" && m.dim.type !== "time")
					throw new Error("'" + m.text + "' needs a number or time field");
			}
			return m;
		});
	}

	// The dims a transform groups by.  A name may appear once and may not be a measure; the bucket transform also
	// reserves its own bucket dim.
	function groupsOf(frame, args, key, measures, reserved) {
		const seenName = new Set();
		return (listArg(args, key) || []).map(n => {
			if (reserved && n === "bucket") throw new Error("groupBy field 'bucket' is reserved");
			if (seenName.has(n)) throw new Error("duplicate groupBy field '" + n + "'");
			seenName.add(n);
			if (measures.some(m => m.text === n)) throw new Error("groupBy field '" + n + "' collides with a measure");
			return dimOf(frame, n);
		});
	}

	// A transform's output keeps the input's truncated flag; bucketUnit only where the transform says so.
	function derivedFrame(frame, f, extra) {
		return Object.assign(f, { truncated: !!frame.truncated }, extra);
	}

	function unitOf(frame) {
		return frame.bucketUnit ? { bucketUnit: frame.bucketUnit } : {};
	}

	// The rows at idx, in that order, with the same dims (string dicts are kept whole).
	function selectRows(frame, idx) {
		const columns = Object.create(null);
		for (const d of frame.dims) {
			const col = frame.columns[d.name];
			if (d.type === "string") {
				const codes = new Int32Array(idx.length);
				idx.forEach((r, i) => { codes[i] = col.codes[r]; });
				columns[d.name] = { codes: codes, dict: col.dict.slice() };
			} else {
				const out = d.type === "bool" ? new Int8Array(idx.length) : new Float64Array(idx.length);
				idx.forEach((r, i) => { out[i] = col[r]; });
				columns[d.name] = out;
			}
		}
		return derivedFrame(frame, { dims: copyDims(frame.dims), rowCount: idx.length, columns: columns },
			unitOf(frame));
	}

	function framed(dims, records) {
		const b = columnarBuilder(dims, {});
		records.forEach(r => b.onRecord(r));
		return b.finish();
	}

	// A filter value as the field's own type; a value of another type is an error, not "no bound".
	function filterValue(d, v) {
		if (d.type === "time") {
			const t = parseTime(v);
			if (t === null) throw new Error("'" + v + "' is not a time");
			return t;
		}
		if (d.type === "bool") {
			if (toBool(v) < 0) throw new Error("'" + v + "' is not a boolean");
			return toBool(v) === 1;
		}
		const n = toNumber(v);
		if (Number.isNaN(n)) throw new Error("'" + v + "' is not a number");
		return n;
	}

	// A null (or undefined) entry in an in / notIn list is the null cell, for every dim type.
	function hasNull(list) {
		return !!list && list.some(v => v == null);
	}

	// String tests run once per dict entry and match strictly by value (no coercion).  A null cell is kept by in only
	// when in names null, dropped by notIn when notIn names null, and never passes contains, min or max.
	function filterTransform(frame, args) {
		const d = dimArg(frame, args, "field");
		const inList = listArg(args, "in"), notIn = listArg(args, "notIn");
		const nullOk0 = (!inList || hasNull(inList)) && !hasNull(notIn);
		let test;
		if (d.type === "string") {
			if (args.min != null || args.max != null) throw new Error("min and max need a number or time field");
			const col = frame.columns[d.name];
			const include = inList && new Set(inList.filter(v => typeof v === "string"));
			const exclude = notIn && new Set(notIn.filter(v => typeof v === "string"));
			const needle = args.contains == null ? null : String(args.contains).toLowerCase();
			const ok = col.dict.map(s => (!include || include.has(s)) && (!exclude || !exclude.has(s))
				&& (needle === null || s.toLowerCase().indexOf(needle) >= 0));
			const nullOk = nullOk0 && needle === null;
			test = i => col.codes[i] < 0 ? nullOk : ok[col.codes[i]];
		} else {
			if (args.contains != null) throw new Error("contains needs a string field");
			if (d.type === "bool" && (args.min != null || args.max != null))
				throw new Error("min and max need a number or time field");
			const conv = v => filterValue(d, v);
			const include = inList && inList.filter(v => v != null).map(conv);
			const exclude = notIn && notIn.filter(v => v != null).map(conv);
			const min = args.min == null ? null : conv(args.min), max = args.max == null ? null : conv(args.max);
			const nullOk = nullOk0 && min === null && max === null;
			test = i => {
				const v = readCell(frame, d, i);
				if (v === null) return nullOk;
				return (!include || include.indexOf(v) >= 0) && (!exclude || exclude.indexOf(v) < 0)
					&& (min === null || v >= min) && (max === null || v <= max);
			};
		}
		const idx = [];
		for (let i = 0; i < frame.rowCount; i++) if (test(i)) idx.push(i);
		return selectRows(frame, idx);
	}

	// The start of t's unit, or null when t is not a usable time: a time beyond the Date range (|t| > 8.64e15), or one
	// whose floor leaves it, is a row with no time.
	const MAX_TIME = 8.64e15;
	function bucketFloor(t, unit, weekStart) {
		if (!(Math.abs(t) <= MAX_TIME)) return null;
		const f = floorUnit(t, unit, weekStart);
		return Math.abs(f) <= MAX_TIME ? f : null;
	}

	// Null sorts first, then by value; keys are compared position by position.  A key is never NaN (bucketFloor and
	// readCell give null instead).
	function compareKeys(a, b) {
		for (let i = 0; i < a.length; i++) {
			if (a[i] === b[i]) continue;
			if (a[i] === null) return -1;
			if (b[i] === null) return 1;
			return a[i] < b[i] ? -1 : 1;
		}
		return 0;
	}

	// One pass, as SeriesAggregator.aggregate: cells keyed by keyOf(i) (null skips the row), sorted by key.  Only
	// finite numbers are measured; count counts the rows of the cell.
	function aggregateRows(frame, keyOf, measures) {
		const cols = measures.map(m => m.field !== null && OWN.call(frame.columns, m.dim.name)
			? frame.columns[m.dim.name] : null);
		const cells = new Map();
		for (let i = 0; i < frame.rowCount; i++) {
			const key = keyOf(i);
			if (key === null) continue;
			const k = JSON.stringify(key);
			let c = cells.get(k);
			if (!c) {
				c = { key: key, acc: measures.map(() => ({ n: 0, sum: 0, min: Infinity, max: -Infinity })) };
				cells.set(k, c);
			}
			measures.forEach((m, j) => {
				const a = c.acc[j];
				if (m.field === null) {
					a.n++;
					return;
				}
				const v = cols[j] === null ? NaN : cols[j][i];
				if (!Number.isFinite(v)) return;
				a.n++;
				a.sum += v;
				a.min = Math.min(a.min, v);
				a.max = Math.max(a.max, v);
			});
		}
		return Array.from(cells.values()).sort((a, b) => compareKeys(a.key, b.key));
	}

	// Sum of no values is 0; avg, min and max of none are null.  An overflow becomes null in the frame, as on the
	// server.
	function measureResult(m, a) {
		if (m.op === "count") return a.n;
		if (m.op === "sum") return a.sum;
		if (a.n === 0) return null;
		return m.op === "avg" ? a.sum / a.n : m.op === "min" ? a.min : a.max;
	}

	function cellsFrame(keyDims, cells, measures) {
		const dims = copyDims(keyDims).concat(measures.map(m => ({ name: m.text, type: "number" })));
		return framed(dims, cells.map(c => {
			const r = Object.create(null);
			c.key.forEach((v, i) => { r[keyDims[i].name] = v; });
			measures.forEach((m, i) => { r[m.text] = measureResult(m, c.acc[i]); });
			return r;
		}));
	}

	function groupByTransform(frame, args) {
		const measures = measuresOf(frame, args);
		const by = groupsOf(frame, args, "by", measures, false);
		const cells = aggregateRows(frame, i => by.map(d => readCell(frame, d, i)), measures);
		// With no groups the result is the one total cell, even over no rows.
		if (by.length === 0 && cells.length === 0)
			cells.push({ key: [], acc: measures.map(() => ({ n: 0, sum: 0, min: Infinity, max: -Infinity })) });
		return derivedFrame(frame, cellsFrame(by, cells, measures), {});
	}

	// The aggregate operation a bucketed frame's number dim holds, or null for a group dim.
	function measureOp(d) {
		if (d.type !== "number") return null;
		if (d.name === "count") return "count";
		const m = MEASURE_RE.exec(d.name);
		return m ? m[1] : null;
	}

	// A frame that already has buckets, to a coarser unit it nests in.  The groups and measures are the input's own,
	// and the frame does not say how many values an avg covered, so avg is weighted by the count column (exact only
	// where every row has a value).  A sum that is null (an overflow) stays null, and rows with no bucket are dropped.
	function rebucket(frame, unit, weekStart) {
		const from = frame.bucketUnit;
		if (UNITS.indexOf(from) < 0) throw new Error("'" + from + "' is not a bucket unit");
		if (unit === "auto" || unit === from) return frame;
		if (UNITS.indexOf(unit) < UNITS.indexOf(from))
			throw new Error("cannot re-bucket '" + from + "' buckets to the finer '" + unit + "'");
		if (NESTS[from].indexOf(unit) < 0)
			throw new Error("'" + from + "' buckets do not nest in '" + unit + "' buckets");
		const bucket = dimOf(frame, "bucket");
		const ops = frame.dims.map(d => d === bucket ? null : measureOp(d));
		const measures = frame.dims.filter((d, i) => ops[i] !== null && d !== bucket);
		const groups = frame.dims.filter((d, i) => ops[i] === null && d !== bucket);
		const count = measures.find(d => d.name === "count");
		if (!count && measures.some(d => measureOp(d) === "avg"))
			throw new Error("avg needs a count column to re-bucket");
		const col = frame.columns.bucket;
		const cells = new Map();
		for (let i = 0; i < frame.rowCount; i++) {
			const start = bucketFloor(col[i], unit, weekStart);
			if (start === null) continue;
			const key = [start].concat(groups.map(d => readCell(frame, d, i)));
			const k = JSON.stringify(key);
			let c = cells.get(k);
			if (!c) {
				c = { key: key, acc: measures.map(() =>
					({ n: 0, nulls: 0, sum: 0, w: 0, wsum: 0, min: Infinity, max: -Infinity })) };
				cells.set(k, c);
			}
			const rows = count ? readCell(frame, count, i) || 0 : 0;
			measures.forEach((d, j) => {
				const v = readCell(frame, d, i), a = c.acc[j];
				if (v === null) {
					a.nulls++;
					return;
				}
				a.n++;
				a.sum += v;
				a.w += rows;
				a.wsum += v * rows;
				a.min = Math.min(a.min, v);
				a.max = Math.max(a.max, v);
			});
		}
		const records = Array.from(cells.values()).sort((a, b) => compareKeys(a.key, b.key)).map(c => {
			const r = Object.create(null);
			r.bucket = c.key[0];
			groups.forEach((d, i) => { r[d.name] = c.key[i + 1]; });
			measures.forEach((d, j) => {
				const a = c.acc[j], op = measureOp(d);
				if (op === "count") r[d.name] = a.sum;
				else if (op === "sum") r[d.name] = a.nulls > 0 ? null : a.sum;
				else if (a.n === 0) r[d.name] = null;
				else if (op === "min") r[d.name] = a.min;
				else if (op === "max") r[d.name] = a.max;
				else r[d.name] = a.w > 0 ? a.wsum / a.w : null;
			});
			return r;
		});
		return derivedFrame(frame, framed(copyDims(frame.dims), records), { bucketUnit: unit });
	}

	// A weekday name or a Date.getUTCDay index (an integer 0-6); Monday when absent.
	function weekStartOf(v) {
		if (v == null) return 1;
		const i = typeof v === "number" && Number.isInteger(v) ? v
			: typeof v === "string" ? WEEKDAYS.indexOf(v.toLowerCase()) : -1;
		if (i < 0 || i > 6) throw new Error("'" + v + "' is not a weekday");
		return i;
	}

	function bucketTransform(frame, args) {
		const unit = args.unit == null ? "auto" : args.unit;
		if (unit !== "auto" && UNITS.indexOf(unit) < 0) throw new Error("'" + unit + "' is not a bucket unit");
		const weekStart = unit === "week" || unit === "auto" ? weekStartOf(args.weekStart) : 1;
		if (frame.bucketUnit) return rebucket(frame, unit, weekStart);
		const t = dimArg(frame, args, "field");
		if (t.type !== "time") throw new Error("field '" + t.name + "' is not a time field");
		const col = frame.columns[t.name];
		let u = unit;
		if (u === "auto") {
			let lo = Infinity, hi = -Infinity;
			for (let i = 0; i < frame.rowCount; i++) {
				if (!(Math.abs(col[i]) <= MAX_TIME)) continue;
				lo = Math.min(lo, col[i]);
				hi = Math.max(hi, col[i]);
			}
			u = lo === Infinity ? "hour" : autoUnit(lo, hi + 1, weekStart);
		}
		const measures = measuresOf(frame, args);
		const by = groupsOf(frame, args, "groupBy", measures, true);
		const cells = aggregateRows(frame, i => {
			const start = bucketFloor(col[i], u, weekStart);
			return start === null ? null : [start].concat(by.map(d => readCell(frame, d, i)));
		}, measures);
		const keyDims = [{ name: "bucket", type: "time" }].concat(by);
		return derivedFrame(frame, cellsFrame(keyDims, cells, measures), { bucketUnit: u });
	}

	// The n rows with the largest by, a null last and ties in input order.  With other, one more row stands for the
	// rest: the label in every string dim, the rest's sum in every additive number dim, null
	// elsewhere (avg, min and max columns do not add up).
	function topNTransform(frame, args) {
		const by = dimArg(frame, args, "by");
		if (by.type !== "number" && by.type !== "time") throw new Error("by needs a number or time field");
		const count = toNumber(args.n);
		if (Number.isNaN(count)) throw new Error("n is not a number");
		const n = Math.max(0, Math.floor(count));
		const vals = [];
		for (let i = 0; i < frame.rowCount; i++) {
			const v = readCell(frame, by, i);
			vals.push(v === null ? -Infinity : v);
		}
		const idx = vals.map((v, i) => i).sort((a, b) => (vals[b] - vals[a]) || (a - b));
		const top = selectRows(frame, idx.slice(0, n));
		if (args.other == null || idx.length <= n) return top;
		const rest = idx.slice(n), other = Object.create(null);
		for (const d of frame.dims) {
			const op = measureOp(d);
			if (op === "avg" || op === "min" || op === "max") other[d.name] = null;
			else if (d.type === "number") other[d.name] = rest.reduce((s, i) => s + (readCell(frame, d, i) || 0), 0);
			else other[d.name] = d.type === "string" ? String(args.other) : null;
		}
		return derivedFrame(frame, framed(copyDims(frame.dims), rowsOf(top).concat([other])), unitOf(frame));
	}

	builtin("transform", "filter", filterTransform);
	builtin("transform", "bucket", bucketTransform);
	builtin("transform", "groupBy", groupByTransform);
	builtin("transform", "topN", topNTransform);

	// A whole-string {token} is the param's value, and inside a list an array value is spread.  {missing: true} while
	// any token has no value.  Only the params' own keys are looked up.
	function fillArgs(v, params) {
		let missing = false;
		const fill = x => {
			if (typeof x === "string") {
				const m = TOKEN_ARG_RE.exec(x);
				if (!m) return x;
				if (!OWN.call(params, m[1]) || params[m[1]] == null) {
					missing = true;
					return null;
				}
				return params[m[1]];
			}
			if (Array.isArray(x)) {
				const out = [];
				for (const e of x) {
					const f = fill(e);
					const spread = typeof e === "string" && TOKEN_ARG_RE.test(e) && Array.isArray(f);
					if (spread) for (const v of f) out.push(v);
					else out.push(f);
				}
				return out;
			}
			if (x && typeof x === "object") {
				const o = Object.create(null);
				for (const k of Object.keys(x)) o[k] = fill(x[k]);
				return o;
			}
			return x;
		};
		const value = fill(v);
		return missing ? { missing: true } : { value: value };
	}

	function isFrame(f) {
		return !!f && typeof f === "object" && Array.isArray(f.dims) && Number.isInteger(f.rowCount) && f.rowCount >= 0
			&& !!f.columns && f.dims.every(d => !!d && typeof d.name === "string" && TYPES.indexOf(d.type) >= 0
			&& OWN.call(f.columns, d.name));
	}

	// One derived source over one input: it mirrors the input's handle, running the transforms on every new input
	// frame or changed argument.  A source it reads itself, or in a ring, never gets a frame to run on.
	function createDerived(def, bus) {
		const id = def.id, topic = "data:" + id;
		const own = bus.owner("data-source:" + id);
		const params = Object.create(null);
		let input = null, shown = null, lastKey = null, lastFrame = null, failed = null, noFrame = null;
		let disposed = false;

		function publish(status, extra) {
			const h = { schemaVersion: 1, source: id, version: shown ? shown.version : 0, status: status,
				progress: null, rowCount: shown ? shown.rowCount : 0, truncated: shown ? shown.truncated : false,
				truncatedAt: shown ? shown.truncatedAt : null, dims: copyDims(shown ? shown.dims : []), error: null };
			if (input && input.cache) h.cache = input.cache;
			if (input && input.mode) h.mode = input.mode;
			if (shown) h.loadedAt = shown.loadedAt;
			own.publish(topic, Object.assign(h, extra));
		}

		function fail(p) {
			config.log.error(p.code + ": " + p.message);
			publish(shown ? "stale" : "error", { error: p });
		}

		// The transforms in order; a failure is returned as the handle's error.
		function transformed(f, list) {
			for (const t of list) {
				const fn = lookup("transform", t.use);
				if (!fn.impl) return { problem: problem("E-JS-94", id, t.use, fn.names.join(", ")), unknown: t.use };
				try {
					f = fn.impl(f, t.args || {});
					if (!isFrame(f)) throw new Error("the result is not a frame");
				} catch (e) {
					return { problem: problem("E-JS-98", id, "transform", t.use, msgOf(e)) };
				}
			}
			return { frame: f };
		}

		function apply() {
			const h = input;
			if (!isHandle(h)) return;
			if (h.status === "loading") return publish("loading", { progress: h.progress || { loaded: 0 } });
			if (h.status === "error") {
				lastFrame = null;
				return publish(shown ? "stale" : "error", { error: h.error });
			}
			const filled = fillArgs(Array.isArray(def.transforms) ? def.transforms : [], params);
			if (filled.missing) return publish("loading", { progress: { loaded: 0 } });
			const key = h.version + "|" + canonicalJson(filled.value);
			// A failure is remembered for its input and args, except an unknown transform, which is looked up again in
			// case it has been registered since.
			if (failed !== null && failed.key === key && !(failed.unknown && lookup("transform", failed.unknown).impl))
				return publish(shown ? "stale" : "error", { error: failed.problem });
			const upstream = get(h);
			if (!upstream) {
				// Logged once per episode; repeats publish the same error quietly until a frame is held again.
				if (noFrame) return publish(shown ? "stale" : "error", { error: noFrame });
				noFrame = problem("E-JS-98", id, "transform input", def.from, "the input frame is not available");
				return fail(noFrame);
			}
			noFrame = null;
			// The held frame's identity counts too: a re-created input may reuse a version.
			if (key !== lastKey || upstream !== lastFrame) {
				const r = transformed(upstream, filled.value);
				if (r.problem) {
					failed = { key: key, problem: r.problem, unknown: r.unknown };
					return fail(r.problem);
				}
				failed = null;
				const f = Object.assign({}, r.frame, { loadedAt: h.loadedAt || iso(config.timers.now()) });
				const version = put(id, f);
				shown = { version: version, rowCount: f.rowCount, truncated: !!f.truncated,
					truncatedAt: f.truncated ? (typeof h.truncatedAt === "number" ? h.truncatedAt : f.rowCount) : null,
					dims: f.dims, loadedAt: f.loadedAt };
				lastKey = key;
				lastFrame = upstream;
			} else if (h.loadedAt) {
				// The handle and the held frame carry the input's latest loadedAt.
				shown.loadedAt = h.loadedAt;
				const e = held.get(id);
				if (e && e.current) e.current.loadedAt = h.loadedAt;
			}
			publish(h.status, { error: h.status === "stale" ? h.error : null });
		}

		function onParams(sub, payload) {
			if (sub.map && typeof sub.map === "object") {
				const m = mapParams(bus, sub, payload);
				if (m.empty) {
					if (sub.whenEmpty === "keep") return;
					for (const k of Object.keys(sub.map)) delete params[k];
				} else Object.assign(params, m.value);
			}
			apply();
		}

		function dispose() {
			if (disposed) return;
			disposed = true;
			lastFrame = null;
			own.dispose();
			release(id);
		}

		// A throw while wiring (a bad subscribes topic) leaves nothing behind: no retained loading handle.
		try {
			publish("loading", { progress: { loaded: 0 } });
			// The params subscriptions come first, so a retained params value is in place when the input's handle
			// arrives.
			for (const sub of def.subscribes || [])
				if (sub.as === "params") own.subscribe(sub.topic, payload => onParams(sub, payload));
			own.subscribe("data:" + def.from, h => {
				input = h;
				apply();
			});
			// A refresh aimed at the derived topic reloads its input.
			own.subscribe("cmd:" + id, payload => {
				const op = payload && payload.op;
				if (op === "refresh" || op === "reload") own.publish("cmd:" + def.from, payload);
			}, { retained: false });
		} catch (e) {
			dispose();
			throw e;
		}
		return { id: id, dispose: dispose };
	}

	Object.assign(util, { fillArgs: fillArgs, createDerived: createDerived });

	// ---- J. boot ----------------------------------------------------------------------------------------------------

	// One DataSource or Derived per contract.dataSources entry, in order.  The shell calls this once the bus is up and
	// before cards subscribe.  A source builds partial frames only when a partialFrames card reads it.  An entry that
	// throws does not stop the entries after it; the first error is rethrown once every entry has been tried.
	function boot(contract) {
		const bus = NS.bus;
		const cards = Array.isArray(contract && contract.cards) ? contract.cards : [];
		const defs = Array.isArray(contract && contract.dataSources) ? contract.dataSources : [];
		const made = [];
		let failed = null;
		for (const def of defs) {
			try {
				made.push(def.kind === "derived" ? createDerived(def, bus) : createSource(def, bus,
					{ partial: cards.some(c => c && c.partialFrames === true && c.source === "data:" + def.id) }));
			} catch (e) {
				if (!failed) failed = { error: e };
			}
		}
		if (failed) throw failed.error;
		return made;
	}

	NS.data = Object.freeze({ get: get, put: put, release: release, building: building, isHandle: isHandle, boot: boot,
		builtins: Object.freeze(builtins), config: config, util: Object.freeze(util) });
})();
