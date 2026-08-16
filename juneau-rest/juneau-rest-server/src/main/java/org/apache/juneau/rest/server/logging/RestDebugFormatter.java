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

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * Per-tier formatter for JUL-level-driven REST debug logging.
 *
 * <p>
 * This is the single public extension point for customizing how request/response debug records are rendered.
 * The internal two-phase pipeline invokes the tier methods <b>cumulatively</b> based on the resolved logger level:
 * <ul>
 * 	<li>{@link #formatBasic(RestRequest,RestResponse) formatBasic} &mdash; always (tier {@code INFO}).
 * 	<li>{@link #formatHeaders(RestRequest,RestResponse) formatHeaders} &mdash; added at tier {@code FINE}.
 * 	<li>{@link #formatBody(RestRequest,RestResponse) formatBody} &mdash; added at tier {@code FINEST}.
 * </ul>
 *
 * <p>
 * The two additive tiers default to the empty string so that a bare implementation supplying only
 * {@link #formatBasic(RestRequest,RestResponse) formatBasic} is additive-safe. Most implementations instead extend
 * {@link BasicRestDebugFormatter} and override only the tier(s) they wish to change.
 *
 * <h5 class='section'>Secure by default</h5>
 * <p>
 * {@link BasicRestDebugFormatter} is secure by default: request/response <b>bodies are never dumped</b> unless the
 * operator has deliberately set the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment variable (and even then only
 * at {@code FINEST} on renderable content). Credential-bearing header values are masked, every client-controlled string
 * is escaped against log forging, and all output is length-bounded. See
 * {@link BasicRestDebugFormatter} for the full contract.
 *
 * <p>
 * The cached request/response bytes, the thrown exception, and the request execution time are reachable through the
 * {@link RestRequest}/{@link RestResponse} accessors ({@link RestRequest#getCachedContent()},
 * {@link RestRequest#getCachedContentLength()}, {@link RestRequest#getException()}, {@link RestRequest#getExecTime()},
 * {@link RestResponse#getCachedContent()}, {@link RestResponse#getCachedContentLength()}).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface RestDebugFormatter {

	/** Default cap on the rendered URI length in the status line. */
	int DEFAULT_MAX_URI_LENGTH = 2048;

	/**
	 * Renders the basic ({@code INFO}-tier) portion of the debug record.
	 *
	 * <p>
	 * Always invoked. Typically a single status line such as {@code [200] HTTP GET /foo}.
	 *
	 * @param req The current REST request. Never <jk>null</jk>.
	 * @param res The current REST response. Never <jk>null</jk>.
	 * @return The rendered basic portion. Never <jk>null</jk>.
	 */
	String formatBasic(RestRequest req, RestResponse res);

	/**
	 * Renders the headers ({@code FINE}-tier) portion of the debug record.
	 *
	 * <p>
	 * Appended to {@link #formatBasic(RestRequest,RestResponse)} when the resolved logger is at {@code FINE}-or-finer.
	 * The default returns the empty string.
	 *
	 * @param req The current REST request. Never <jk>null</jk>.
	 * @param res The current REST response. Never <jk>null</jk>.
	 * @return The rendered headers portion. Never <jk>null</jk>.
	 */
	default String formatHeaders(RestRequest req, RestResponse res) {
		return "";
	}

	/**
	 * Renders the body ({@code FINEST}-tier) portion of the debug record.
	 *
	 * <p>
	 * Appended when the resolved logger is at {@code FINEST}. Reads the cached request/response bytes. The default
	 * returns the empty string.
	 *
	 * @param req The current REST request. Never <jk>null</jk>.
	 * @param res The current REST response. Never <jk>null</jk>.
	 * @return The rendered body portion. Never <jk>null</jk>.
	 */
	default String formatBody(RestRequest req, RestResponse res) {
		return "";
	}

	/**
	 * Renders the basic status line ({@code [status] HTTP method uri}) directly from servlet objects.
	 *
	 * <p>
	 * Used both by {@link #formatBasic(RestRequest,RestResponse)} and by the pipeline's no-operation (404/405) path,
	 * which has no {@link RestRequest}/{@link RestResponse} to render through. The method and URI are always
	 * {@link DebugTextSanitizer#sanitize(String,int) sanitized} (CR/LF and control characters escaped) so a
	 * client-controlled URI cannot forge a log line, and the rendered URI is length-capped. This default applies
	 * {@link #DEFAULT_MAX_URI_LENGTH}; {@link BasicRestDebugFormatter} overrides it to apply its configurable
	 * {@code maxUriLength}.
	 *
	 * <p>
	 * The query string is intentionally excluded (matching {@link HttpServletRequest#getRequestURI()}), so plaintext
	 * {@code ?token=…} disclosure is never re-introduced through this path.
	 *
	 * @param req The servlet request. Never <jk>null</jk>.
	 * @param res The servlet response. Never <jk>null</jk>.
	 * @return The rendered, sanitized status line.
	 */
	default String statusLine(HttpServletRequest req, HttpServletResponse res) {
		var method = DebugTextSanitizer.sanitize(req.getMethod(), DEFAULT_MAX_URI_LENGTH);
		var uri = DebugTextSanitizer.sanitize(req.getRequestURI(), DEFAULT_MAX_URI_LENGTH);
		return new StringBuilder().append('[').append(res.getStatus()).append("] HTTP ").append(method).append(' ').append(uri).toString();
	}

	/**
	 * Returns <jk>true</jk> if a body with the given content type is worth rendering as text (as opposed to being
	 * binary/unknown).
	 *
	 * <p>
	 * This is a text-vs-binary renderability predicate, <b>not</b> a redaction allowlist. It parses only the media type
	 * (type/subtype, ignoring parameters such as {@code ; charset=utf-8}), case-folds, and treats the following as
	 * renderable: {@code text/*}, {@code application/json}, {@code application/xml}, any {@code +json}/{@code +xml}
	 * suffix, and {@code application/x-www-form-urlencoded}. {@code multipart/form-data} is explicitly non-renderable.
	 * An absent or blank content type is conservatively treated as non-renderable.
	 *
	 * <p>
	 * {@code Content-Encoding} is not visible to this one-argument predicate; a caller that dumps bodies must separately
	 * treat any non-identity encoding (e.g. {@code gzip}) as non-renderable.
	 *
	 * @param contentType The body's content type. Can be <jk>null</jk> (returns <jk>false</jk>).
	 * @return <jk>true</jk> if the content type is renderable as text.
	 */
	default boolean isBodyRenderable(String contentType) {
		if (contentType == null)
			return false;
		var ct = contentType.trim();
		var semi = ct.indexOf(';');
		if (semi >= 0)
			ct = ct.substring(0, semi).trim();
		ct = ct.toLowerCase(Locale.ROOT);
		if (ct.isEmpty())
			return false;
		if (ct.startsWith("text/"))
			return true;
		if (ct.equals("multipart/form-data"))
			return false;
		if (ct.equals("application/json") || ct.equals("application/xml") || ct.equals("application/x-www-form-urlencoded"))
			return true;
		return ct.endsWith("+json") || ct.endsWith("+xml");
	}

	/**
	 * Returns the maximum number of request/response body bytes to capture for the {@code FINEST}-tier body rendering.
	 *
	 * <p>
	 * This cap is enforced at <i>capture</i> time by the two-phase pipeline (Phase A), so memory stays bounded even for
	 * large uploads/downloads. Defaults to 8&nbsp;KB.
	 *
	 * @return The body capture cap in bytes.
	 */
	default int bodyCap() {
		return 8 * 1024;
	}
}
