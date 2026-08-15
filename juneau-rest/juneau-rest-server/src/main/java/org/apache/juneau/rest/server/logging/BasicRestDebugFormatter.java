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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.RedactedHeaders;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * Default {@link RestDebugFormatter} implementation.
 *
 * <p>
 * Renders, cumulatively by tier:
 * <ul>
 * 	<li><b>Basic ({@code INFO})</b> &mdash; a single status line ({@code [status] HTTP method uri}).
 * 	<li><b>Headers ({@code FINE})</b> &mdash; request/response header blocks (credential-bearing values masked), plus
 * 		request/response lengths and the request execution time.
 * 	<li><b>Body ({@code FINEST})</b> &mdash; request/response bodies as UTF-8 + spaced-hex, reading the cached bytes,
 * 		with a {@code …[truncated N bytes]} marker when the body exceeded the capture cap.
 * </ul>
 *
 * <h5 class='section'>Secure-by-default</h5>
 * <p>
 * Header values for the well-known credential-bearing set ({@link RedactedHeaders#DEFAULT}) are masked with
 * {@link RedactedHeaders#REDACTED}. The set is overridable via {@link #redactedHeaders(Collection)}.
 *
 * <p>
 * Bodies are captured up to {@link #bodyCap()} bytes (default <b>8&nbsp;KB</b>). This default is deliberately smaller than
 * {@code EchoMixin}'s 1&nbsp;MB body cap &mdash; debug logging is intended for low-volume operator diagnostics where an
 * 8&nbsp;KB window keeps log lines readable and memory bounded, whereas {@code EchoMixin} is an explicit round-trip
 * introspection endpoint. The two caps are independent by design; do not "fix" them toward parity.
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

	/** The set of header names whose values are masked. Overridable via {@link #redactedHeaders(Collection)}. */
	protected Set<String> redactedHeaders = RedactedHeaders.DEFAULT;

	/** The body capture cap in bytes. Overridable via {@link #bodyCap(int)}. */
	protected int bodyCap = DEFAULT_BODY_CAP;

	/**
	 * Constructor.
	 */
	public BasicRestDebugFormatter() {}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Provided so subclasses can be instantiated through the {@link BeanInstantiator} chain (constructor injection).
	 *
	 * @param beanStore The bean store. Ignored by the default implementation.
	 */
	public BasicRestDebugFormatter(BeanStore beanStore) {}

	/**
	 * Overrides the redacted-header set.
	 *
	 * @param value The new set of header names to mask (case-insensitive). Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicRestDebugFormatter redactedHeaders(Collection<String> value) {
		redactedHeaders = new LinkedHashSet<>(value);
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

	@Override /* Overridden from RestDebugFormatter */
	public int bodyCap() {
		return bodyCap;
	}

	@Override /* Overridden from RestDebugFormatter */
	public String formatBasic(RestRequest req, RestResponse res) {
		return statusLine(req.getHttpServletRequest(), res.getHttpServletResponse());
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

		var reqHeaderNames = sreq.getHeaderNames();
		if (reqHeaderNames != null && reqHeaderNames.hasMoreElements()) {
			sb.append("\n---Request Headers---");
			while (reqHeaderNames.hasMoreElements()) {
				var h = reqHeaderNames.nextElement();
				sb.append("\n\t").append(h).append(": ").append(RedactedHeaders.redact(h, sreq.getHeader(h), redactedHeaders));
			}
		}

		var resHeaderNames = sres.getHeaderNames();
		if (resHeaderNames != null && ! resHeaderNames.isEmpty()) {
			sb.append("\n---Response Headers---");
			for (var h : resHeaderNames)
				sb.append("\n\t").append(h).append(": ").append(RedactedHeaders.redact(h, sres.getHeader(h), redactedHeaders));
		}

		return sb.toString();
	}

	@Override /* Overridden from RestDebugFormatter */
	public String formatBody(RestRequest req, RestResponse res) {
		var sb = new StringBuilder();
		appendBody(sb, "Request", req.getCachedContent(), req.getCachedContentLength());
		appendBody(sb, "Response", res.getCachedContent(), res.getCachedContentLength());
		return sb.toString();
	}

	private void appendBody(StringBuilder sb, String label, byte[] content, long totalLength) {
		if (content == null || content.length == 0)
			return;
		try {
			sb.append("\n---").append(label).append(" Content UTF-8---");
			sb.append("\n").append(new String(content, UTF8));
			sb.append("\n---").append(label).append(" Content Hex---");
			sb.append("\n").append(toSpacedHex(content));
			var omitted = totalLength - content.length;
			if (omitted > 0)
				sb.append("\n…[truncated ").append(omitted).append(" bytes]");
		} catch (Exception e) {
			sb.append("\n").append(e.getLocalizedMessage());
		}
	}

	/**
	 * Renders the basic status line ({@code [status] HTTP method uri}) directly from servlet objects.
	 *
	 * <p>
	 * Shared by {@link #formatBasic(RestRequest,RestResponse)} and by the pipeline's no-operation (404/405) path, which
	 * has no {@link RestRequest}/{@link RestResponse} to render through.
	 *
	 * @param req The servlet request. Never <jk>null</jk>.
	 * @param res The servlet response. Never <jk>null</jk>.
	 * @return The rendered status line.
	 */
	public static String statusLine(HttpServletRequest req, HttpServletResponse res) {
		return new StringBuilder()
			.append('[').append(res.getStatus()).append("] ")
			.append("HTTP ").append(req.getMethod()).append(' ')
			.append(req.getRequestURI())
			.toString();
	}
}
