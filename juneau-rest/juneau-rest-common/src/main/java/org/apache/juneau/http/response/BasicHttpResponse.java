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
 * fluent setters that mutate state and return {@code this}.
 *
 * <p>
 * Immutability is expressed with the "funnel + nested {@code Unmodifiable} snapshot" paradigm: every mutator routes
 * through the single protected {@link #modify(Runnable)} choke-point, and each concrete leaf gets a nested
 * {@code X.Unmodifiable} whose only behavioral override is a throwing {@code modify(...)}.  A leaf's
 * {@link #unmodifiable()} factory returns a point-in-time snapshot of type {@code X.Unmodifiable}; any attempt to set a
 * property on it throws an {@link UnsupportedOperationException}.
 *
 * <p>
 * This is a self-typed (CRTP) root: a leaf declares <c>class Ok <jk>extends</jk> BasicHttpResponse&lt;Ok&gt;</c> and
 * inherits leaf-typed fluent setters with no covariant overrides.
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
 * @param <SELF> The self type for fluent setters.
 * @since 9.2.1
 */
@SuppressWarnings({
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class BasicHttpResponse<SELF extends BasicHttpResponse<SELF>> implements HttpResponseMessage {

	private HttpStatusLine statusLine;
	private HttpHeaderList headers;
	private HttpBody body;
	private Locale locale;

	/**
	 * Constructor with no body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 */
	protected BasicHttpResponse(HttpStatusLine statusLine) {
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
	protected BasicHttpResponse(HttpStatusLine statusLine, HttpBody body) {
		this(statusLine, l(), body);
	}

	/**
	 * Constructor with a string body (UTF-8, {@code text/plain}).
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param body The response body as a plain-text string. May be <jk>null</jk>.
	 */
	protected BasicHttpResponse(HttpStatusLine statusLine, String body) {
		this(statusLine, l(), body != null ? StringBody.of(body) : null);
	}

	/**
	 * Constructor with headers and an optional body.
	 *
	 * @param statusLine The status line. Must not be <jk>null</jk>.
	 * @param headers The response headers. Can be <jk>null</jk> (treated as an empty list).
	 * @param body The response body. May be <jk>null</jk>.
	 */
	protected BasicHttpResponse(HttpStatusLine statusLine, List<HttpHeader> headers, HttpBody body) {
		this.statusLine = assertArgNotNull("statusLine", statusLine);
		this.headers = HttpHeaderList.of(headers);
		this.body = body;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpResponse(BasicHttpResponse<?> copyFrom) {
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
	public SELF setStatusLine(HttpStatusLine value) {
		return modify(() -> statusLine = assertArgNotNull("value", value));
	}

	/**
	 * Sets the status code, preserving the existing protocol version and reason phrase.
	 *
	 * @param value New status code.
	 * @return This object.
	 */
	public SELF setStatusCode(int value) {
		return modify(() -> statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), value, statusLine.getReasonPhrase()));
	}

	/**
	 * Sets the reason phrase, preserving the existing protocol version and status code.
	 *
	 * @param value New reason phrase. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setReasonPhrase(String value) {
		return modify(() -> statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), statusLine.getStatusCode(), value));
	}

	/**
	 * Sets the protocol version, preserving the existing status code and reason phrase.
	 *
	 * @param value New protocol version. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setProtocolVersion(HttpProtocolVersion value) {
		return modify(() -> {
			assertArgNotNull("value", value);
			statusLine = HttpStatusLineBean.of(value, statusLine.getStatusCode(), statusLine.getReasonPhrase());
		});
	}

	/**
	 * Replaces all headers with the given list.
	 *
	 * @param values New headers. {@code null} is treated as an empty list.
	 * @return This object.
	 */
	public SELF setHeaders(List<HttpHeader> values) {
		return modify(() -> {
			headers.clear();
			if (values != null)
				headers.append(values);
		});
	}

	/**
	 * Replaces all headers with the given array.
	 *
	 * @param values New headers. {@code null} is treated as an empty array.
	 * @return This object.
	 */
	public SELF setHeaders(HttpHeader...values) {
		return modify(() -> {
			headers.clear();
			if (values != null)
				headers.append(values);
		});
	}

	/**
	 * Sets a single header, replacing any existing header with the same name.
	 *
	 * @param value The header to set. {@code null} is ignored.
	 * @return This object.
	 */
	public SELF setHeader(HttpHeader value) {
		return modify(() -> headers.set(value));
	}

	/**
	 * Sets a single header, replacing any existing header with the same name.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setHeader(String name, String value) {
		return modify(() -> headers.set(name, value));
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param value The header to add. {@code null} is ignored.
	 * @return This object.
	 */
	public SELF addHeader(HttpHeader value) {
		return modify(() -> headers.append(value));
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF addHeader(String name, String value) {
		return modify(() -> headers.append(name, value));
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setBody(HttpBody value) {
		return modify(() -> body = value);
	}

	/**
	 * Sets the response body to a plain-text string body (UTF-8, {@code text/plain}).
	 *
	 * @param value Body text. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(String value) {
		return setBody(value != null ? StringBody.of(value) : null);
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(HttpBody value) {
		return setBody(value);
	}

	/**
	 * Sets the locale associated with this response.
	 *
	 * @param value New locale. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setLocale(Locale value) {
		return modify(() -> locale = value);
	}

	/**
	 * Sets the value of the {@code Location} header.
	 *
	 * @param value The new location value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setLocation(String value) {
		return setHeader(Location.NAME, value);
	}

	/**
	 * Sets the value of the {@code Location} header.
	 *
	 * @param value The new location value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setLocation(URI value) {
		return setHeader(Location.NAME, value != null ? value.toString() : null);
	}

	/**
	 * Returns whether this instance is an unmodifiable snapshot.
	 *
	 * @return <jk>true</jk> if this bean is an {@link UnmodifiableBean} snapshot.
	 */
	public boolean isUnmodifiable() {
		return this instanceof UnmodifiableBean;
	}

	/**
	 * Returns an unmodifiable snapshot of this bean.
	 *
	 * <p>
	 * The returned instance is a point-in-time copy of the leaf's nested {@code Unmodifiable} type; any attempt to set a
	 * property on it throws an {@link UnsupportedOperationException}.  This method is idempotent: if this bean is already
	 * unmodifiable it returns itself rather than taking another snapshot.  The receiver is left unchanged (and still
	 * modifiable unless it was already unmodifiable).
	 *
	 * @return An unmodifiable snapshot of this bean, or this bean if it is already unmodifiable.
	 */
	public abstract SELF unmodifiable();

	// ------------------------------------------------------------------------------------------------------------------
	// Fluent "with" helpers (mutate this instance and return the leaf type)
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body on this response.
	 *
	 * @param value The new body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF withBody(HttpBody value) {
		return modify(() -> body = value);
	}

	/**
	 * Sets the body on this response as a plain-text string.
	 *
	 * @param value The new body as a plain-text string. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF withBody(String value) {
		return withBody(value != null ? StringBody.of(value) : null);
	}

	/**
	 * Appends a header to this response.
	 *
	 * @param header The header to add. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF withHeader(HttpHeader header) {
		return modify(() -> headers.append(assertArgNotNull("header", header)));
	}

	/**
	 * Appends a header to this response.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF withHeader(String name, String value) {
		return withHeader(HttpHeaderBean.of(name, value));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Immutability paradigm plumbing
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns this object cast to the self type.
	 *
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // CRTP self-type cast is safe: SELF is bound to the concrete leaf type.
	})
	protected final SELF self() {
		return (SELF) this;
	}

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every fluent setter routes through this method.  The nested {@code Unmodifiable} snapshot overrides only this
	 * method to throw, which freezes the entire mutation surface with a single override.
	 *
	 * @param mutation The state change to apply.
	 * @return This object.
	 */
	protected SELF modify(Runnable mutation) {
		mutation.run();
		return self();
	}

	/**
	 * Deep-freeze hook invoked from the nested {@code Unmodifiable} constructor after the snapshot copy completes.
	 *
	 * <p>
	 * The mutable sub-beans are frozen here by <b>direct field assignment</b> (never via a setter, which would route
	 * through the throwing {@link #modify(Runnable)} and blow up during construction).  A leaf that adds its own mutable
	 * sub-bean overrides this method (calling {@code super.freeze()} first).
	 */
	protected void freeze() {
		headers = headers.unmodifiable();   // DIRECT field write — snapshot the copied HttpHeaderList.
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return o instanceof BasicHttpResponse<?> other && eq(this, other, (x, y) ->
			eq(x.statusLine, y.statusLine) && eq(x.headers, y.headers) && eq(x.body, y.body));
	}

	@Override /* Object */
	public int hashCode() {
		return h(statusLine, headers, body);
	}

	@Override /* Object */
	public String toString() {
		return statusLine.toString();
	}
}
