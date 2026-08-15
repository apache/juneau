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

import org.apache.juneau.rest.server.*;

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
