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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;

/**
 * Base class for HTTP error response exceptions (4xx and 5xx) in the {@code org.apache.juneau.http} stack.
 *
 * <p>
 * Extends {@link RuntimeException} while also implementing {@link HttpResponseMessage}, so that HTTP
 * error responses can be both thrown as exceptions and used as response objects. Provides classic-style
 * fluent setters that mutate state and return {@code this}.
 *
 * <p>
 * Immutability is expressed with the "funnel + nested {@code Unmodifiable} snapshot" paradigm: every mutator routes
 * through the single protected {@link #modify(Runnable)} choke-point, and each concrete leaf gets a nested
 * {@code X.Unmodifiable} whose only behavioral override is a throwing {@code modify(...)}.  A leaf's
 * {@link #unmodifiable()} factory returns a point-in-time snapshot; any attempt to set a property on it throws an
 * {@link UnsupportedOperationException}.
 *
 * <p>
 * This is a {@link Throwable}-rooted hierarchy, so (unlike {@link BasicHttpResponse}) it cannot be a self-typed (CRTP)
 * root — JLS &sect;8.1.2 forbids a generic {@code Throwable}.  The funnel therefore returns the base type and each leaf
 * keeps its covariant setter overrides.
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
@SuppressWarnings({
	"java:S3008", // Concrete subclasses use UPPER_CASE singleton instance constants (e.g., INSTANCE fields).
	"java:S1948", // HttpHeaderList is not Java-serializable; HTTP exceptions are not designed for Java serialization transport.
	"java:S1165"  // Mutable exception fields are intentional: HTTP exceptions support builder-style fluent setters.
})
public class BasicHttpException extends RuntimeException implements HttpResponseMessage {

	private static final long serialVersionUID = 1L;

	private HttpStatusLine statusLine;
	private HttpHeaderList headers;
	private HttpBody body;
	private Locale locale;

	/**
	 * Constructor with no message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase (e.g. {@code "Internal Server Error"}). May be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase) {
		this(statusCode, reasonPhrase, (Throwable)null, (String)null);
	}

	/**
	 * Constructor with a status code and a cause.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, Throwable cause) {
		this(statusCode, reasonPhrase, cause, cause != null ? cause.getMessage() : null);
	}

	/**
	 * Constructor with a {@link String#format(String, Object...) String.format}-style message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param msg The exception message and response body. May be <jk>null</jk>.
	 *    Treated as a {@link String#format(String, Object...) String.format}-style format string (<c>%s</c> placeholders) when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, String msg, Object...args) {
		this(statusCode, reasonPhrase, (Throwable)null, msg, args);
	}

	/**
	 * Constructor with a cause and a {@link String#format(String, Object...) String.format}-style message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param msg The exception message. May be <jk>null</jk>.
	 *    Treated as a {@link String#format(String, Object...) String.format}-style format string (<c>%s</c> placeholders) when {@code args} is non-empty.
	 * @param args Optional message arguments.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, Throwable cause, String msg, Object...args) {
		super(formatMessage(msg, args), cause);
		this.statusLine = HttpStatusLineBean.of(statusCode, reasonPhrase);
		this.headers = HttpHeaderList.create();
		var m = formatMessage(msg, args);
		this.body = m != null ? StringBody.of(m) : null;
	}

	private static String formatMessage(String msg, Object...args) {
		if (msg == null)
			return null;
		if (args == null || args.length == 0)
			return msg;
		return f(msg, args);
	}

	/**
	 * Copy constructor.
	 *
	 * <p>
	 * The mutable sub-beans are deep-copied by <b>direct field assignment</b>, never through a funneled setter.  This is
	 * the snapshot path used by {@link #unmodifiable()}: the constructor runs inside an {@code Unmodifiable} instance
	 * whose {@link #modify(Runnable)} already throws, so a setter-based copy would blow up during construction.
	 *
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpException(BasicHttpException copyFrom) {
		super(assertArgNotNull("copyFrom", copyFrom).getMessage(), copyFrom.getCause());
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

	@Override /* Throwable */
	public String getMessage() {
		var m = super.getMessage();
		if (m == null && getCause() != null)
			m = getCause().getMessage();
		if (m == null)
			m = statusLine.getReasonPhrase();
		return m;
	}

	/**
	 * Returns the HTTP status code.
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
	public BasicHttpException setStatusLine(HttpStatusLine value) {
		return modify(() -> statusLine = assertArgNotNull("value", value));
	}

	/**
	 * Sets the status code, preserving the existing protocol version and reason phrase.
	 *
	 * @param value New status code.
	 * @return This object.
	 */
	public BasicHttpException setStatusCode(int value) {
		return modify(() -> statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), value, statusLine.getReasonPhrase()));
	}

	/**
	 * Sets the reason phrase, preserving the existing protocol version and status code.
	 *
	 * @param value New reason phrase. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setReasonPhrase(String value) {
		return modify(() -> statusLine = HttpStatusLineBean.of(statusLine.getProtocolVersion(), statusLine.getStatusCode(), value));
	}

	/**
	 * Sets the protocol version, preserving the existing status code and reason phrase.
	 *
	 * @param value New protocol version. Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setProtocolVersion(HttpProtocolVersion value) {
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
	public BasicHttpException setHeaders(List<HttpHeader> values) {
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
	public BasicHttpException setHeaders(HttpHeader...values) {
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
	public BasicHttpException setHeader(HttpHeader value) {
		return modify(() -> headers.set(value));
	}

	/**
	 * Sets a single header, replacing any existing header with the same name.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setHeader(String name, String value) {
		return modify(() -> headers.set(name, value));
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param value The header to add. {@code null} is ignored.
	 * @return This object.
	 */
	public BasicHttpException addHeader(HttpHeader value) {
		return modify(() -> headers.append(value));
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param name Header name. Must not be <jk>null</jk>.
	 * @param value Header value. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException addHeader(String name, String value) {
		return modify(() -> headers.append(name, value));
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setBody(HttpBody value) {
		return modify(() -> body = value);
	}

	/**
	 * Sets the response body to a plain-text string body (UTF-8, {@code text/plain}).
	 *
	 * @param value Body text. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setContent(String value) {
		return setBody(value != null ? StringBody.of(value) : null);
	}

	/**
	 * Sets the response body.
	 *
	 * @param value New body. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setContent(HttpBody value) {
		return setBody(value);
	}

	/**
	 * Sets the locale associated with this response.
	 *
	 * @param value New locale. May be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setLocale(Locale value) {
		return modify(() -> locale = value);
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
	public BasicHttpException unmodifiable() {
		return this instanceof Unmodifiable ? this : new Unmodifiable(this);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Stack/cause helpers
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the root cause of this exception.
	 *
	 * <p>
	 * The root cause is the first exception in the init-cause parent chain that's not one of the following:
	 * <ul>
	 * 	<li>{@link BasicHttpException}
	 * 	<li>{@link InvocationTargetException}
	 * </ul>
	 *
	 * @return The root cause of this exception, or <jk>null</jk> if no root cause was found.
	 */
	public Throwable getRootCause() {
		Throwable t = this;
		while (nn(t)) {
			if (! (t instanceof BasicHttpException || t instanceof InvocationTargetException))
				return t;
			t = t.getCause();
		}
		return null;
	}

	/**
	 * Returns all error messages from all errors in this exception's cause chain joined into a single string.
	 *
	 * @param scrubForXssVulnerabilities
	 * 	If {@code true}, replaces {@code <}, {@code >}, and {@code &} characters with spaces.
	 * @return A multi-line string of messages. Never <jk>null</jk>.
	 */
	public String getFullStackMessage(boolean scrubForXssVulnerabilities) {
		var msg = getMessage();
		var sb = new StringBuilder();
		if (nn(msg)) {
			if (scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			sb.append(msg);
		}
		var e = getCause();
		while (nn(e)) {
			msg = e.getMessage();
			if (nn(msg) && scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			var cls = cns(e);
			if (msg == null)
				sb.append(f("\nCaused by (%s)", cls));
			else
				sb.append(f("\nCaused by (%s): %s", cls, msg));
			e = e.getCause();
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Immutability paradigm plumbing
	// ------------------------------------------------------------------------------------------------------------------

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every fluent setter routes through this method.  The nested {@code Unmodifiable} snapshot overrides only this
	 * method to throw, which freezes the entire mutation surface with a single override.  Because this is a
	 * {@link Throwable}-rooted hierarchy (no CRTP self-type), the funnel returns the base type.
	 *
	 * @param mutation The state change to apply.
	 * @return This object.
	 */
	protected BasicHttpException modify(Runnable mutation) {
		mutation.run();
		return this;
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
		// D3 content equality.  The 'body' HttpBody is deliberately excluded: it is a wrapper without value semantics
		// and is derived from the message (getMessage()), which is already compared here.
		return o instanceof BasicHttpException other && eq(this, other, (x, y) ->
			eq(x.statusLine, y.statusLine) && eq(x.headers, y.headers) && eq(x.getMessage(), y.getMessage()));
	}

	@Override /* Object */
	public int hashCode() {
		return h(statusLine, headers, getMessage());
	}

	@Override /* Object */
	public String toString() {
		return statusLine.toString();
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link BasicHttpException}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends BasicHttpException implements UnmodifiableBean {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param copyFrom The exception to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(BasicHttpException copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpException */
		protected BasicHttpException modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
