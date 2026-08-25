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
package org.apache.juneau.rest.server.logging;

import static org.apache.juneau.commons.utils.IoUtils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * Default {@link RestDebugFormatter} implementation &mdash; secure by default.
 *
 * <p>
 * Renders, cumulatively by tier:
 * <ul>
 * 	<li><b>Basic ({@code INFO})</b> &mdash; a single status line ({@code [status] HTTP method uri}), with the method and
 * 		URI sanitized and the URI length-capped.
 * 	<li><b>Headers ({@code FINE})</b> &mdash; request/response header blocks (<b>every</b> value of every header,
 * 		credential-bearing values masked, all names/values sanitized and bounded), plus request/response lengths and the
 * 		request execution time.
 * 	<li><b>Body ({@code FINEST})</b> &mdash; request/response bodies, but <b>only if the operator has opted in</b> (see
 * 		below); otherwise a suppression placeholder + byte count.
 * </ul>
 *
 * <h5 class='section'>Secure-by-default body handling</h5>
 * <p>
 * Bodies are <b>never dumped by default</b>. Dumping is a deliberate operator opt-in behind a single environment-variable
 * master gate, {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES}:
 * <ul>
 * 	<li><b>Env-only.</b> It is an environment variable, never a system property, and has no system-property fallback
 * 		&mdash; application code cannot flip it at runtime.
 * 	<li><b>Truthy semantics.</b> The raw value is trimmed, then a non-empty value enables dumping except case-insensitive
 * 		{@code false}/{@code 0}; unset/empty/all-whitespace disables. It is resolved once and cached, making it an
 * 		emergency kill switch.
 * 	<li><b>Gate subordinate.</b> With the gate unset, {@link #formatBody(RestRequest,RestResponse) formatBody} emits only
 * 		the suppression placeholder + byte count &mdash; always, even if a {@link RestDebugBodyScrubber} is configured
 * 		(the scrubber is never invoked).
 * 	<li><b>Renderable content only.</b> Even when opted in, only renderable content (see
 * 		{@link #isBodyRenderable(String)}, plus an identity {@code Content-Encoding}) is dumped; binary/unknown/compressed
 * 		content yields a distinct non-renderable placeholder.
 * 	<li><b>Fail-closed scrubber.</b> When the gate is set and a {@link RestDebugBodyScrubber} is configured, its output is
 * 		dumped (then sanitized and capped); if it throws or returns <jk>null</jk>, the body fails closed to a placeholder
 * 		&mdash; never the raw body.
 * </ul>
 * No bytes appear in any placeholder in any representation.
 *
 * <h5 class='section'>Header redaction and sanitization</h5>
 * <p>
 * Values for a widened, formatter-local credential-bearing header set are masked with {@link RedactedHeaders#REDACTED}.
 * The set starts from {@link RedactedHeaders#DEFAULT} plus {@code X-Auth-Token}, {@code X-Authorization},
 * {@code WWW-Authenticate}, {@code Referer}, and {@code Location}; the shared {@link RedactedHeaders#DEFAULT} is left
 * unchanged. Matching is case-insensitive with separator folding ({@code -}/{@code _} stripped), so {@code X-Auth-Token},
 * {@code X_Auth_Token}, and {@code XAuthToken} all match. Replace the set with {@link #redactedHeaders(Collection)} or
 * extend it with {@link #addRedactedHeaders(Collection)}. Every emitted string (URI/method, header names/values, body
 * text, placeholders) is sanitized against CR/LF/control-char log forging via {@link DebugTextSanitizer}, in
 * <b>mask &rarr; escape &rarr; cap</b> order.
 *
 * <h5 class='section'>Bounds</h5>
 * <p>
 * Bodies are bounded by {@link #bodyCap()} bytes (default <b>8&nbsp;KB</b>) at capture time; headers by
 * {@link #maxHeaders(int)}/{@link #maxHeaderScan(int)}/{@link #maxFieldLength(int)}; the URI by {@link #maxUriLength(int)}.
 * The 8&nbsp;KB body cap is deliberately smaller than {@code EchoMixin}'s 1&nbsp;MB body cap &mdash; debug logging is
 * intended for low-volume operator diagnostics; the two caps are independent by design.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class BasicRestDebugFormatter implements RestDebugFormatter {

	/** Default body capture cap, in bytes (8&nbsp;KB). */
	public static final int DEFAULT_BODY_CAP = 8 * 1024;

	/** The environment variable that gates body dumping. */
	static final String ENV_ALLOW_DUMP_BODIES = "JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES";

	/**
	 * The widened, formatter-local default set of header names whose values are masked. Starts from a copy of
	 * {@link RedactedHeaders#DEFAULT} and adds the credential-bearing headers the shared default omits.
	 */
	static final Set<String> DEFAULT_REDACTED_HEADERS;
	static {
		var s = new LinkedHashSet<>(RedactedHeaders.DEFAULT);
		s.add("X-Auth-Token");
		s.add("X-Authorization");
		s.add("WWW-Authenticate");
		s.add("Referer");
		s.add("Location");
		DEFAULT_REDACTED_HEADERS = Collections.unmodifiableSet(s);
	}

	/**
	 * Cached resolution of the body-dump master gate. {@code null} means "not yet resolved"; resolution reads the
	 * environment exactly once. Not a system property, and there is no system-property fallback.
	 */
	private static volatile Boolean allowDumpBodiesCache;

	/** The set of header names whose values are masked. Overridable via {@link #redactedHeaders(Collection)}. */
	protected Set<String> redactedHeaders = DEFAULT_REDACTED_HEADERS;

	/** Optional body scrubber applied to body text when the master gate is set. */
	protected RestDebugBodyScrubber bodyScrubber;

	/** The body capture cap in bytes. Overridable via {@link #bodyCap(int)}. */
	protected int bodyCap = DEFAULT_BODY_CAP;

	/** Cap on emitted header values per block. Overridable via {@link #maxHeaders(int)}. */
	protected int maxHeaders = 100;

	/** Cap on values scanned while computing omitted counts. Overridable via {@link #maxHeaderScan(int)}. */
	protected int maxHeaderScan = 1000;

	/** Per header name/value length cap after masking/escaping. Overridable via {@link #maxFieldLength(int)}. */
	protected int maxFieldLength = 1024;

	/** Rendered URI length cap. Overridable via {@link #maxUriLength(int)}. */
	protected int maxUriLength = DEFAULT_MAX_URI_LENGTH;

	/**
	 * Constructor.
	 */
	public BasicRestDebugFormatter() {
		// No state to initialize; all fields have field-level defaults.
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Provided so subclasses can be instantiated through the {@link BeanInstantiator} chain (constructor injection).
	 *
	 * @param beanStore The bean store. Ignored by the default implementation.
	 */
	public BasicRestDebugFormatter(BeanStore beanStore) {
		// beanStore is intentionally unused; this overload only exists to satisfy constructor-injection.
	}

	/**
	 * Overrides the redacted-header set (replaces the built-in widened set).
	 *
	 * @param value The new set of header names to mask (case-insensitive, separator-folded). Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicRestDebugFormatter redactedHeaders(Collection<String> value) {
		redactedHeaders = new LinkedHashSet<>(value);
		return this;
	}

	/**
	 * Extends the redacted-header set additively (keeps the built-in widened protections).
	 *
	 * @param value The header names to add to the masked set (case-insensitive, separator-folded). Must not be
	 * 	<jk>null</jk>.
	 * @return This object.
	 */
	public BasicRestDebugFormatter addRedactedHeaders(Collection<String> value) {
		var s = new LinkedHashSet<>(redactedHeaders);
		s.addAll(value);
		redactedHeaders = s;
		return this;
	}

	/**
	 * Sets the body scrubber applied to body text when the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} gate is set.
	 *
	 * <p>
	 * The scrubber only ever runs when the gate is set; it chooses scrubbed-vs-raw once dumping is already permitted. It
	 * is fail-closed: a throw or a <jk>null</jk> return yields a placeholder, never the raw body.
	 *
	 * @param value The scrubber, or <jk>null</jk> for none (the default, which dumps raw when gated).
	 * @return This object.
	 */
	public BasicRestDebugFormatter bodyScrubber(RestDebugBodyScrubber value) {
		bodyScrubber = value;
		return this;
	}

	/**
	 * Overrides the body capture cap.
	 *
	 * @param value The new cap in bytes.
	 * @return This object.
	 */
	public BasicRestDebugFormatter bodyCap(int value) {
		bodyCap = value;
		return this;
	}

	/**
	 * Overrides the cap on emitted header values per block.
	 *
	 * @param value The new cap. Must be &ge; 0.
	 * @return This object.
	 */
	public BasicRestDebugFormatter maxHeaders(int value) {
		if (value < 0)
			throw new IllegalArgumentException("maxHeaders must be >= 0");
		maxHeaders = value;
		return this;
	}

	/**
	 * Overrides the cap on values scanned while computing omitted counts.
	 *
	 * @param value The new cap. Must be &ge; 0.
	 * @return This object.
	 */
	public BasicRestDebugFormatter maxHeaderScan(int value) {
		if (value < 0)
			throw new IllegalArgumentException("maxHeaderScan must be >= 0");
		maxHeaderScan = value;
		return this;
	}

	/**
	 * Overrides the per header name/value length cap.
	 *
	 * @param value The new cap. Must be &ge; 0.
	 * @return This object.
	 */
	public BasicRestDebugFormatter maxFieldLength(int value) {
		if (value < 0)
			throw new IllegalArgumentException("maxFieldLength must be >= 0");
		maxFieldLength = value;
		return this;
	}

	/**
	 * Overrides the rendered URI length cap.
	 *
	 * @param value The new cap. Must be &ge; 0.
	 * @return This object.
	 */
	public BasicRestDebugFormatter maxUriLength(int value) {
		if (value < 0)
			throw new IllegalArgumentException("maxUriLength must be >= 0");
		maxUriLength = value;
		return this;
	}

	@Override /* Overridden from RestDebugFormatter */
	public int bodyCap() {
		return bodyCap;
	}

	@Override /* Overridden from RestDebugFormatter */
	public String formatBasic(RestRequest req, RestResponse res) {
		return statusLine(req.getHttpServletRequest(), res.getHttpServletResponse());
	}

	@Override /* Overridden from RestDebugFormatter */
	public String statusLine(HttpServletRequest req, HttpServletResponse res) {
		var method = DebugTextSanitizer.sanitize(req.getMethod(), maxUriLength);
		var uri = DebugTextSanitizer.sanitize(req.getRequestURI(), maxUriLength);
		var sb = new StringBuilder();
		// Prepend the resolved correlation id (Should-fix 1): read the session-cached getRequestId() through the
		// session-handle seam — never a live public/custom-key attribute read.  Missing session/id → no prefix (today's
		// unprefixed line), never a throw.  Both formatBasic and the 404 render path route through here.
		var session = RestSession.fromRequest(req);
		if (session != null) {
			var id = session.getRequestId();
			if (id != null && ! id.isEmpty())
				sb.append("[requestId=").append(id).append("] ");
		}
		return sb.append('[').append(res.getStatus()).append("] HTTP ").append(method).append(' ').append(uri).toString();
	}

	@Override /* Overridden from RestDebugFormatter */
	public String formatHeaders(RestRequest req, RestResponse res) {
		var sreq = req.getHttpServletRequest();
		var sres = res.getHttpServletResponse();
		var sb = new StringBuilder();

		var reqLen = req.getCachedContentLength();
		if (reqLen >= 0)
			sb.append("\n\tRequest length: ").append(reqLen).append(" bytes");

		sb.append("\n\tResponse code: ").append(sres.getStatus());

		var resLen = res.getCachedContentLength();
		if (resLen >= 0)
			sb.append("\n\tResponse length: ").append(resLen).append(" bytes");

		var execTime = req.getExecTime();
		if (execTime != null)
			sb.append("\n\tExec time: ").append(execTime).append("ms");

		var normalizedRedacted = normalizedRedactedSet();
		appendRequestHeaders(sb, sreq, normalizedRedacted);
		appendResponseHeaders(sb, sres, normalizedRedacted);

		return sb.toString();
	}

	private void appendRequestHeaders(StringBuilder sb, HttpServletRequest sreq, Set<String> normalizedRedacted) {
		var names = sreq.getHeaderNames();
		if (names == null || ! names.hasMoreElements())
			return;
		sb.append("\n---Request Headers---");
		var state = new HeaderScanState();
		while (names.hasMoreElements() && ! state.scanCapped) {
			var name = names.nextElement();
			var redact = isRedacted(name, normalizedRedacted);
			var sName = DebugTextSanitizer.sanitize(name, maxFieldLength);
			var values = sreq.getHeaders(name);
			appendHeaderValues(sb, sName, redact, values == null ? Collections.emptyIterator() : values.asIterator(), state);
		}
		appendOmissionMarker(sb, state.scanCapped, state.extra);
	}

	private void appendResponseHeaders(StringBuilder sb, HttpServletResponse sres, Set<String> normalizedRedacted) {
		var names = sres.getHeaderNames();
		if (names == null || names.isEmpty())
			return;
		sb.append("\n---Response Headers---");
		var state = new HeaderScanState();
		for (var name : names) {
			if (state.scanCapped)
				break;
			var redact = isRedacted(name, normalizedRedacted);
			var sName = DebugTextSanitizer.sanitize(name, maxFieldLength);
			appendHeaderValues(sb, sName, redact, sres.getHeaders(name).iterator(), state);
		}
		appendOmissionMarker(sb, state.scanCapped, state.extra);
	}

	/**
	 * Emits (or counts as omitted) every value for one header name, shared by the request- and response-header render
	 * loops so neither has to nest a second scan/emit/omit loop inside its own.
	 */
	private void appendHeaderValues(StringBuilder sb, String sanitizedName, boolean redact, Iterator<String> values, HeaderScanState state) {
		while (values.hasNext()) {
			if (state.scanned >= maxHeaderScan) {
				state.scanCapped = true;
				return;
			}
			state.scanned++;
			var v = values.next();
			if (state.emitted < maxHeaders) {
				appendHeaderLine(sb, sanitizedName, redact ? RedactedHeaders.REDACTED : v);
				state.emitted++;
			} else {
				state.extra++;
			}
		}
	}

	/** Mutable per-header-block emission/omission counters, threaded through {@link #appendHeaderValues}. */
	private static final class HeaderScanState {
		int emitted;
		long extra;
		int scanned;
		boolean scanCapped;
	}

	private void appendHeaderLine(StringBuilder sb, String sanitizedName, String value) {
		// Order: mask (already applied by caller) → escape → cap.
		sb.append("\n\t").append(sanitizedName).append(": ").append(DebugTextSanitizer.sanitize(value, maxFieldLength));
	}

	private static void appendOmissionMarker(StringBuilder sb, boolean scanCapped, long extra) {
		if (scanCapped)
			sb.append("\n\t\u2026[more headers omitted]");
		else if (extra > 0)
			sb.append("\n\t\u2026[+").append(extra).append(" more headers omitted]");
	}

	@Override /* Overridden from RestDebugFormatter */
	public String formatBody(RestRequest req, RestResponse res) {
		var sreq = req.getHttpServletRequest();
		var sres = res.getHttpServletResponse();
		var sb = new StringBuilder();
		appendBody(sb, "Request", req.getCachedContent(), req.getCachedContentLength(), sreq.getContentType(),
			sreq.getHeader("Content-Encoding"));
		appendBody(sb, "Response", res.getCachedContent(), res.getCachedContentLength(), sres.getContentType(),
			sres.getHeader("Content-Encoding"));
		return sb.toString();
	}

	private void appendBody(StringBuilder sb, String label, byte[] content, long totalLength, String contentType, String contentEncoding) {
		if (content == null || content.length == 0)
			return;
		var byteCount = totalLength >= 0 ? totalLength : content.length;
		var ctToken = contentTypeToken(contentType);

		if (! isAllowDumpBodies()) {
			appendPlaceholder(sb, label, byteCount, ctToken,
				"; set " + ENV_ALLOW_DUMP_BODIES + " to enable", "body suppressed");
			return;
		}

		if (! isRenderable(contentType, contentEncoding)) {
			appendPlaceholder(sb, label, byteCount, ctToken, "; binary/non-renderable content", "body not rendered");
			return;
		}

		var raw = new String(content, UTF8);
		String bodyText;
		if (bodyScrubber != null) {
			String scrubbed;
			try {
				scrubbed = bodyScrubber.scrub(contentType, raw);
			} catch (Throwable t) {  // NOSONAR - fail closed on any scrubber failure; never re-leak via the exception.
				scrubbed = null;
			}
			if (scrubbed == null) {
				appendPlaceholder(sb, label, byteCount, ctToken, "; scrubber failed", "body suppressed");
				return;
			}
			bodyText = scrubbed;
		} else {
			bodyText = raw;
		}

		sb.append("\n---").append(label).append(" Content---");
		sb.append('\n').append(DebugTextSanitizer.sanitize(bodyText, charCap()));
		var omitted = byteCount - content.length;
		if (omitted > 0)
			sb.append("\n\u2026[truncated ").append(omitted).append(" bytes]");
	}

	private void appendPlaceholder(StringBuilder sb, String label, long byteCount, String ctToken, String reason, String verb) {
		sb.append("\n---").append(label).append(" Content---");
		sb.append("\n[").append(verb).append(": ").append(byteCount).append(" bytes, ").append(ctToken).append(reason).append(']');
	}

	private String contentTypeToken(String contentType) {
		if (contentType == null || contentType.isBlank())
			return "unknown";
		return DebugTextSanitizer.sanitize(contentType, maxFieldLength);
	}

	private boolean isRenderable(String contentType, String contentEncoding) {
		if (! isIdentityEncoding(contentEncoding))
			return false;
		return isBodyRenderable(contentType);
	}

	private static boolean isIdentityEncoding(String contentEncoding) {
		if (contentEncoding == null)
			return true;
		var e = contentEncoding.trim();
		return e.isEmpty() || e.equalsIgnoreCase("identity");
	}

	/**
	 * Returns the character-length cap for a dumped body, derived from {@link #bodyCap()}.
	 *
	 * <p>
	 * Worst-case escaping expands one byte to a 6-character {@code \\uXXXX} sequence, so the sanitized character cap is
	 * {@code bodyCap() * 6} (clamped to {@code Integer.MAX_VALUE}). Capture already bounds the raw bytes; this second
	 * layer bounds a scrubber that returns an oversized string.
	 */
	private int charCap() {
		var cap = Math.max(bodyCap, 0) * 6L;
		return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
	}

	private Set<String> normalizedRedactedSet() {
		var s = new HashSet<String>();
		for (var n : redactedHeaders)
			if (n != null)
				s.add(normalizeHeaderName(n));
		return s;
	}

	private static boolean isRedacted(String name, Set<String> normalizedRedacted) {
		return name != null && normalizedRedacted.contains(normalizeHeaderName(name));
	}

	/** Case-folds and strips {@code -}/{@code _} and whitespace so {@code X-Auth-Token}/{@code X_Auth_Token}/{@code XAuthToken} all match. */
	private static String normalizeHeaderName(String name) {
		var sb = new StringBuilder(name.length());
		for (var i = 0; i < name.length(); i++) {
			var c = name.charAt(i);
			if (c == '-' || c == '_' || Character.isWhitespace(c))
				continue;
			sb.append(Character.toLowerCase(c));
		}
		return sb.toString();
	}

	/**
	 * Returns whether body dumping is enabled by the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment variable.
	 *
	 * <p>
	 * Resolved once from the environment and cached. Never reads a system property.
	 *
	 * @return <jk>true</jk> if body dumping is enabled.
	 */
	static boolean isAllowDumpBodies() {
		var v = allowDumpBodiesCache;
		if (v == null) {
			v = parseAllowDumpBodies(System.getenv(ENV_ALLOW_DUMP_BODIES));
			allowDumpBodiesCache = v;
		}
		return v;
	}

	/**
	 * Parses a raw gate value with trim-then-parse truthy semantics.
	 *
	 * <p>
	 * The value is trimmed first; a non-empty trimmed value enables (returns <jk>true</jk>) except case-insensitive
	 * {@code false} or {@code 0}; {@code null}/empty/all-whitespace disables. This is a fail-safe kill switch, so
	 * ambiguous values resolve to disabled.
	 *
	 * @param raw The raw environment value. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the value enables body dumping.
	 */
	static boolean parseAllowDumpBodies(String raw) {
		if (raw == null)
			return false;
		var t = raw.trim();
		if (t.isEmpty())
			return false;
		return ! (t.equalsIgnoreCase("false") || t.equals("0"));
	}

	/**
	 * Test-only seam for exercising both gate states without mutating the process environment.
	 *
	 * <p>
	 * Package-private and unreachable from application code, and never reads a system property. Production resolution
	 * stays env-only / read-once ({@link #isAllowDumpBodies()}).
	 *
	 * @param override {@code null} clears the cache so the next resolution re-reads the environment once; a non-<jk>null</jk>
	 * 	value is cached directly as the test gate state.
	 */
	static void resetAllowDumpBodiesForTest(Boolean override) {
		allowDumpBodiesCache = override;
	}
}
