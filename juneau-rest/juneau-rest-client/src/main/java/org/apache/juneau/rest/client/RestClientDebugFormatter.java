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

import java.util.*;

/**
 * Per-tier formatter for JUL-level-driven REST client debug logging.
 *
 * <p>
 * The client debug pipeline invokes these methods cumulatively based on the resolved logger level:
 * <ul>
 * 	<li>{@link #formatBasic(RestRequest, RestResponse)} at {@code INFO}.
 * 	<li>{@link #formatHeaders(RestRequest, RestResponse)} added at {@code FINE}.
 * 	<li>{@link #formatBody(RestRequest, RestResponse)} added at {@code FINEST}.
 * </ul>
 *
 * <p>
 * <b>Beta - API subject to change:</b> This type is part of the next-generation REST client and HTTP stack.
 *
 * @since 10.0.0
 */
public interface RestClientDebugFormatter {

	/**
	 * Renders the basic ({@code INFO}-tier) portion of a debug record.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return The formatted message chunk.
	 */
	String formatBasic(RestRequest req, RestResponse res);

	/**
	 * Renders the headers ({@code FINE}-tier) portion of a debug record.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return The formatted message chunk.
	 */
	default String formatHeaders(RestRequest req, RestResponse res) {
		return "";
	}

	/**
	 * Renders the body ({@code FINEST}-tier) portion of a debug record.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return The formatted message chunk.
	 */
	default String formatBody(RestRequest req, RestResponse res) {
		return "";
	}

	/**
	 * Maximum number of request/response body bytes to capture for {@code FINEST} rendering.
	 *
	 * @return The capture cap in bytes.
	 */
	default int bodyCap() {
		return 8 * 1024;
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
	 * <p>
	 * Keep byte-identical to {@code org.apache.juneau.rest.server.logging.RestDebugFormatter}'s counterpart.
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
}
