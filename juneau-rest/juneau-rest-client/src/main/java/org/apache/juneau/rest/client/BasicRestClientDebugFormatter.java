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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.IoUtils.*;

import java.util.*;

import org.apache.juneau.http.*;

/**
 * Default {@link RestClientDebugFormatter} implementation &mdash; secure by default.
 *
 * <p>
 * Request/response bodies are <b>never dumped by default</b>; dumping is a deliberate operator opt-in behind the
 * {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment-variable master gate, mirroring
 * {@code org.apache.juneau.rest.server.logging.BasicRestDebugFormatter}'s server-side contract. See
 * {@link #bodyScrubber(RestDebugBodyScrubber)} and {@link RestClientDebugFormatter#isBodyRenderable(String)} for the
 * rest of the pipeline.
 *
 * @since 10.0.0
 */
public class BasicRestClientDebugFormatter implements RestClientDebugFormatter {

	/** Default body capture cap, in bytes (8 KB). */
	public static final int DEFAULT_BODY_CAP = 8 * 1024;

	/**
	 * The environment variable that gates body dumping. Keep byte-identical to
	 * {@code org.apache.juneau.rest.server.logging.BasicRestDebugFormatter}'s counterpart — the same operator-facing
	 * kill switch must protect both the server and client debug pipelines.
	 */
	static final String ENV_ALLOW_DUMP_BODIES = "JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES";

	/**
	 * The widened, formatter-local default set of header names whose values are masked. Starts from a copy of
	 * {@link RedactedHeaders#DEFAULT} and adds the credential-bearing headers the shared default omits.
	 *
	 * <p>
	 * Keep byte-identical to {@code org.apache.juneau.rest.server.logging.BasicRestDebugFormatter}'s counterpart.
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

	/** The set of header names whose values are masked. */
	protected Set<String> redactedHeaders = DEFAULT_REDACTED_HEADERS;

	/** Optional body scrubber applied to body text when the master gate is set. */
	protected RestDebugBodyScrubber bodyScrubber;

	/** The body capture cap in bytes. */
	protected int bodyCap = DEFAULT_BODY_CAP;

	/** Constructor. */
	public BasicRestClientDebugFormatter() {}

	/**
	 * Overrides the redacted-header set.
	 *
	 * @param value The new set of header names to mask.
	 * @return This object.
	 */
	public BasicRestClientDebugFormatter redactedHeaders(Collection<String> value) {
		redactedHeaders = new LinkedHashSet<>(value);
		return this;
	}

	/**
	 * Sets the body scrubber applied to body text when the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} gate is set.
	 *
	 * <p>
	 * The scrubber only ever runs once dumping is already permitted by the gate; it chooses scrubbed-vs-raw body
	 * text. It is fail-closed: a throw or a <jk>null</jk> return yields a placeholder, never the raw body.
	 *
	 * @param value The scrubber, or <jk>null</jk> for none (the default, which dumps raw when gated).
	 * @return This object.
	 */
	public BasicRestClientDebugFormatter bodyScrubber(RestDebugBodyScrubber value) {
		bodyScrubber = value;
		return this;
	}

	/**
	 * Overrides the body capture cap.
	 *
	 * @param value The new cap in bytes.
	 * @return This object.
	 */
	public BasicRestClientDebugFormatter bodyCap(int value) {
		bodyCap = value;
		return this;
	}

	@Override /* RestClientDebugFormatter */
	public int bodyCap() {
		return bodyCap;
	}

	@Override /* RestClientDebugFormatter */
	public String formatBasic(RestRequest req, RestResponse res) {
		var uri = req.getUri();
		var method = DebugTextSanitizer.sanitize(req.getMethod());
		var uriStr = DebugTextSanitizer.sanitize(uri != null ? uri.toString() : "");
		var sb = new StringBuilder();
		// Prepend the resolved correlation id (same effective-id order as the emit stamp): the confirmed echoed id
		// when present, else the sent id.  Both are already safe to interpolate (client-minted Uuid7 values are safe;
		// a server-echoed value was sanitized server-side), so no second sanitizer pass is needed for this token.
		var effectiveId = (res != null && res.getRequestId() != null) ? res.getRequestId() : (req != null ? req.getRequestId() : null);
		if (effectiveId != null)
			sb.append("[requestId=").append(effectiveId).append("] ");
		return sb
			.append('[').append(res.getStatusCode()).append("] ")
			.append("HTTP ").append(method).append(' ')
			.append(uriStr)
			.append(" (").append(req.getExecTime() != null ? req.getExecTime().toMillis() : 0).append("ms)")
			.toString();
	}

	@Override /* RestClientDebugFormatter */
	public String formatHeaders(RestRequest req, RestResponse res) {
		var sb = new StringBuilder();

		var reqLen = req.getCachedContentLength();
		if (reqLen >= 0)
			sb.append("\n\tRequest length: ").append(reqLen).append(" bytes");

		sb.append("\n\tResponse code: ").append(res.getStatusCode());

		var resLen = res.getCachedContentLength();
		if (resLen >= 0)
			sb.append("\n\tResponse length: ").append(resLen).append(" bytes");

		var normalizedRedacted = normalizedRedactedSet();

		if (!req.getResolvedHeaders().isEmpty()) {
			sb.append("\n---Request Headers---");
			for (var h : req.getResolvedHeaders())
				appendHeaderLine(sb, h.name(), h.value(), normalizedRedacted);
		}

		if (!res.getHeaders().isEmpty()) {
			sb.append("\n---Response Headers---");
			for (var h : res.getHeaders())
				appendHeaderLine(sb, h.name(), h.value(), normalizedRedacted);
		}
		return sb.toString();
	}

	private void appendHeaderLine(StringBuilder sb, String name, String value, Set<String> normalizedRedacted) {
		// Order: mask (name-based, on the raw value) → escape (name, then whatever survived masking) → cap (none here).
		var masked = isRedacted(name, normalizedRedacted) ? RedactedHeaders.REDACTED : value;
		sb.append("\n\t").append(DebugTextSanitizer.sanitize(name)).append(": ").append(DebugTextSanitizer.sanitize(masked));
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

	@Override /* RestClientDebugFormatter */
	public String formatBody(RestRequest req, RestResponse res) {
		var sb = new StringBuilder();
		// Content-Type/Content-Encoding are resolved independently per side, each from that side's own finalized,
		// wire-order header list — never reused across sides (Risk 5).
		var reqCt = header(req.getFirstHeader("Content-Type"));
		var reqCe = header(req.getFirstHeader("Content-Encoding"));
		var resCt = header(res.getFirstHeader("Content-Type"));
		var resCe = header(res.getFirstHeader("Content-Encoding"));
		appendBody(sb, "Request", req.getCachedContent(), req.getCachedContentLength(), reqCt, reqCe);
		appendBody(sb, "Response", res.getCachedContent(), res.getCachedContentLength(), resCt, resCe);
		return sb.toString();
	}

	private static String header(TransportHeader h) {
		return h == null ? null : h.value();
	}

	private void appendBody(StringBuilder sb, String label, byte[] content, long totalLength, String contentType, String contentEncoding) {
		if (content == null || content.length == 0)
			return;
		var byteCount = totalLength >= 0 ? totalLength : content.length;
		var ctToken = contentTypeToken(contentType);

		if (! isAllowDumpBodies()) {
			appendPlaceholder(sb, label, byteCount, ctToken, "; set " + ENV_ALLOW_DUMP_BODIES + " to enable", "body suppressed");
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
		return DebugTextSanitizer.sanitize(contentType);
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
	 * Worst-case escaping expands one byte to a 6-character {@code \\uXXXX} sequence, so the sanitized character cap
	 * is {@code bodyCap() * 6} (clamped to {@code Integer.MAX_VALUE}). Capture already bounds the raw bytes; this
	 * second layer bounds a scrubber that returns an oversized string.
	 */
	private int charCap() {
		var cap = Math.max(bodyCap, 0) * 6L;
		return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
	}

	/**
	 * Returns whether body dumping is enabled by the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment
	 * variable.
	 *
	 * <p>
	 * Resolved once from the environment and cached. Never reads a system property. Keep byte-identical to
	 * {@code org.apache.juneau.rest.server.logging.BasicRestDebugFormatter}'s counterpart.
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
	 * ambiguous values resolve to disabled. Keep byte-identical to
	 * {@code org.apache.juneau.rest.server.logging.BasicRestDebugFormatter}'s counterpart.
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
	 * Package-private and unreachable from application code, and never reads a system property. Production
	 * resolution stays env-only / read-once ({@link #isAllowDumpBodies()}).
	 *
	 * @param override {@code null} clears the cache so the next resolution re-reads the environment once; a
	 * 	non-<jk>null</jk> value is cached directly as the test gate state.
	 */
	static void resetAllowDumpBodiesForTest(Boolean override) {
		allowDumpBodiesCache = override;
	}
}
