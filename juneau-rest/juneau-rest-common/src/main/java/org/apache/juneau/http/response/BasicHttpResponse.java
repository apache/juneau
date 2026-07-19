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
package org.apache.juneau.http.response;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;

/**
 * Base class for concrete HTTP response objects in the {@code org.apache.juneau.http} stack.
 *
 * <p>
 * Holds a status line, an optional body, and an ordered list of headers. Provides classic-style
 * fluent setters that mutate state and return {@code this}, plus an {@link #setUnmodifiable()} latch
 * that locks the instance against further changes.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public class BasicHttpResponse implements HttpResponseMessage {

	private HttpStatusLine statusLine;
	private final HttpHeaderList headers;
	private HttpBody body;
	private Locale locale;
	private boolean unmodifiable;

	/**
	 * Constructor with no body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine) {
		// Reason phrase is documented-nullable; StringBody.of asserts non-null, so guard against a valid status line
		// that carries a null reason phrase (would otherwise NPE on a legitimate input).
		this(statusLine, l(),
			statusLine != null && statusLine.getReasonPhrase() != null ? StringBody.of(statusLine.getReasonPhrase()) : null);
	}

	/**
	 * Constructor with a body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, HttpBody body) {
		this(statusLine, l(), body);
	}

	/**
	 * Constructor with a string body (UTF-8, {@code text/plain}).
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param body The response body as a plain-text string. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, String body) {
		this(statusLine, l(), body != null ? StringBody.of(body) : null);
	}

	/**
	 * Constructor with headers and an optional body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param headers The response headers. Can be <jk>null</jk> (treated as an empty list).
	 * @param body The response body. May be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpStatusLine statusLine, List<HttpHeader> headers, HttpBody body) {
		this.statusLine = assertArgNotNull("statusLine", statusLine);
		this.headers = HttpHeaderList.of(headers);
		this.body = body;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpResponse(BasicHttpResponse copyFrom) {
		assertArgNotNull("copyFrom", copyFrom);
		this.statusLine = copyFrom.statusLine;
		this.headers = copyFrom.headers.copy();
		this.body = copyFrom.body;
		this.locale = copyFrom.locale;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Read accessors
	// ------------------------------------------------------------------------------------------------------------------

	@Override /* HttpResponseMessage */
	public HttpStatusLine getStatusLine() {
		return statusLine;
	}

	@Override /* HttpResponseMessage */
	public List<HttpHeader> getHeaders() {
		return Collections.unmodifiableList(headers);
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
	 * Returns the locale associated with this response, if any.
	 *
	 * @return The locale, possibly <jk>null</jk>.
	 */
	public Locale getLocale() {
		return locale;
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Fluent setters (mutate this instance)
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the status line.
	 *
	 * @param value New status line. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setStatusLine(HttpStatusLine value) {
		assertModifiable();
		this.statusLine = assertArgNotNull("value", value);
		return this;
	}

	/**
	 * Sets the status code, preserving the existing protocol version and reason phrase.
	 *
	 * @param value New status code.
	 * @return This object.
	 */
	public BasicHttpResponse setStatusCode(int value) {
		assertModifiable();
		this.statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), value, statusLine.getReasonPhrase());
		return this;
	}

	/**
	 * Sets the reason phrase, preserving the existing protocol version and status code.
	 *
	 * @param value New reason phrase. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setReasonPhrase(String value) {
		assertModifiable();
		this.statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), statusLine.getStatusCode(), value);
		return this;
	}

	/**
	 * Sets the protocol version, preserving the existing status code and reason phrase.
	 *
	 * @param value New protocol version. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setProtocolVersion(HttpProtocolVersion value) {
		assertModifiable();
		assertArgNotNull("value", value);
		this.statusLine = HttpStatusLineBean.of(value, statusLine.getStatusCode(), statusLine.getReasonPhrase());
		return this;
	}

	/**
	 * Replaces all headers with the given list.
	 *
	 * @param values New headers. {@code null} is treated as an empty list.
	 * @return This object.
	 */
	public BasicHttpResponse setHeaders(List<HttpHeader> values) {
		assertModifiable();
		this.headers.clear();
		if (values != null)
			this.headers.append(values);
		return this;
	}

	/**
	 * Replaces all headers with the given array.
	 *
	 * @param values New headers. {@code null} is treated as an empty array.
	 * @return This object.
	 */
	public BasicHttpResponse setHeaders(HttpHeader...values) {
		assertModifiable();
		this.headers.clear();
		if (values != null)
			this.headers.append(values);
		return this;
	}

	/**
	 * Sets a single header, replacing any existing header with the same name.
	 *
	 * @param value The header to set. {@code null} is ignored.
	 * @return This object.
	 */
	public BasicHttpResponse setHeader(HttpHeader value) {
		assertModifiable();
		this.headers.set(value);
		return this;
	}

	/**
	 * Sets a single header, replacing any existing header with the same name.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setHeader(String name, String value) {
		assertModifiable();
		this.headers.set(name, value);
		return this;
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param value The header to add. {@code null} is ignored.
	 * @return This object.
	 */
	public BasicHttpResponse addHeader(HttpHeader value) {
		assertModifiable();
		this.headers.append(value);
		return this;
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse addHeader(String name, String value) {
		assertModifiable();
		this.headers.append(name, value);
		return this;
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setBody(HttpBody value) {
		assertModifiable();
		this.body = value;
		return this;
	}

	/**
	 * Sets the response body to a plain-text string body (UTF-8, {@code text/plain}).
	 *
	 * @param value Body text. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setContent(String value) {
		return setBody(value != null ? StringBody.of(value) : null);
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setContent(HttpBody value) {
		return setBody(value);
	}

	/**
	 * Sets the locale associated with this response.
	 *
	 * @param value New locale. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setLocale(Locale value) {
		assertModifiable();
		this.locale = value;
		return this;
	}

	/**
	 * Sets the value of the {@code Location} header.
	 *
	 * @param value The new location value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setLocation(String value) {
		return setHeader(Location.NAME, value);
	}

	/**
	 * Sets the value of the {@code Location} header.
	 *
	 * @param value The new location value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpResponse setLocation(URI value) {
		return setHeader(Location.NAME, value != null ? value.toString() : null);
	}

	/**
	 * Latches this instance so that subsequent calls to mutating setters throw {@link IllegalStateException}.
	 *
	 * @return This object.
	 */
	public BasicHttpResponse setUnmodifiable() {
		this.unmodifiable = true;
		return this;
	}

	/**
	 * Returns whether this instance is locked against modification.
	 *
	 * @return {@code true} if {@link #setUnmodifiable()} has been called.
	 */
	public boolean isUnmodifiable() {
		return unmodifiable;
	}

	/**
	 * Throws an {@link IllegalStateException} if this instance has been marked unmodifiable.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw new IllegalStateException("Instance is unmodifiable");
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Immutable "with" helpers (return new instances; preserved for callers that prefer immutability)
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns a new instance with the given body replacing the current body.
	 *
	 * @param value The new body. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public BasicHttpResponse withBody(HttpBody value) {
		var c = new BasicHttpResponse(this);
		c.body = value;
		return c;
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
		var c = new BasicHttpResponse(this);
		c.headers.append(header);
		return c;
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
