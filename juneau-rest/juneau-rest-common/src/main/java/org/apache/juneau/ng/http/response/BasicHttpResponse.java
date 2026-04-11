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
package org.apache.juneau.ng.http.response;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.util.*;

import org.apache.juneau.ng.http.*;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.header.*;

/**
 * Base class for concrete HTTP response objects in the {@code org.apache.juneau.ng.http} stack.
 *
 * <p>
 * Holds an immutable status line and an optional body. Headers are currently not populated by default;
 * subclasses or callers should extend this class to add response-specific header handling.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class BasicHttpResponse implements HttpResponseMessage {

	private final HttpStatusLine statusLine;
	private final List<HttpHeader> headers;
	private final HttpBody body;

	/**
	 * Constructor with no body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine) {
		this(statusLine, List.of(), null);
	}

	/**
	 * Constructor with a body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, HttpBody body) {
		this(statusLine, List.of(), body);
	}

	/**
	 * Constructor with a string body (UTF-8, {@code text/plain}).
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param body The response body as a plain-text string. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, String body) {
		this(statusLine, List.of(), body != null ? StringBody.of(body) : null);
	}

	/**
	 * Constructor with headers and an optional body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param headers The response headers. Must not be <jk>null</jk>.
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, List<HttpHeader> headers, HttpBody body) {
		this.statusLine = assertArgNotNull("statusLine", statusLine);
		this.headers = List.copyOf(headers);
		this.body = body;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpResponse(BasicHttpResponse copyFrom) {
		this(copyFrom.statusLine, copyFrom.headers, copyFrom.body);
	}

	@Override /* HttpResponseMessage */
	public HttpStatusLine getStatusLine() {
		return statusLine;
	}

	@Override /* HttpResponseMessage */
	public List<HttpHeader> getHeaders() {
		return headers;
	}

	@Override /* HttpResponseMessage */
	public HttpBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP status code from the status line.
	 *
	 * @return The status code.
	 */
	public int getStatusCode() {
		return statusLine.getStatusCode();
	}

	/**
	 * Returns a new instance with the given body replacing the current body.
	 *
	 * @param value The new body. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public BasicHttpResponse withBody(HttpBody value) {
		return new BasicHttpResponse(statusLine, headers, value);
	}

	/**
	 * Returns a new instance with the given string body replacing the current body.
	 *
	 * @param value The new body as a plain-text string. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public BasicHttpResponse withBody(String value) {
		return withBody(value != null ? StringBody.of(value) : null);
	}

	/**
	 * Returns a new instance with the given header added.
	 *
	 * @param header The header to add. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public BasicHttpResponse withHeader(HttpHeader header) {
		var newHeaders = new ArrayList<>(headers);
		newHeaders.add(header);
		return new BasicHttpResponse(statusLine, newHeaders, body);
	}

	/**
	 * Returns a new instance with the given header name and value added.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public BasicHttpResponse withHeader(String name, String value) {
		return withHeader(HttpHeaderBean.of(name, value));
	}

	@Override /* Object */
	public String toString() {
		return statusLine.toString();
	}
}
