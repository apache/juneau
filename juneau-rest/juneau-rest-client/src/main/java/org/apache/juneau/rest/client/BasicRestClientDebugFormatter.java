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
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.http.*;

/**
 * Default {@link RestClientDebugFormatter} implementation.
 *
 * @since 10.0.0
 */
public class BasicRestClientDebugFormatter implements RestClientDebugFormatter {

	/** Default body capture cap, in bytes (8 KB). */
	public static final int DEFAULT_BODY_CAP = 8 * 1024;

	/** The set of header names whose values are masked. */
	protected Set<String> redactedHeaders = RedactedHeaders.DEFAULT;

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
		return new StringBuilder()
			.append('[').append(res.getStatusCode()).append("] ")
			.append("HTTP ").append(req.getMethod()).append(' ')
			.append(uri != null ? uri : URI.create(""))
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

		if (!req.getResolvedHeaders().isEmpty()) {
			sb.append("\n---Request Headers---");
			for (var h : req.getResolvedHeaders())
				sb.append("\n\t").append(h.name()).append(": ").append(RedactedHeaders.redact(h.name(), h.value(), redactedHeaders));
		}

		if (!res.getHeaders().isEmpty()) {
			sb.append("\n---Response Headers---");
			for (var h : res.getHeaders())
				sb.append("\n\t").append(h.name()).append(": ").append(RedactedHeaders.redact(h.name(), h.value(), redactedHeaders));
		}
		return sb.toString();
	}

	@Override /* RestClientDebugFormatter */
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
}
