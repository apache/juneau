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

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.Utils.*;

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
 * fluent setters that mutate state and return {@code this}, plus an {@link #setUnmodifiable()} latch
 * that locks the instance against further changes.
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
@SuppressWarnings({"java:S3008", "java:S1948", "java:S1165"})
public class BasicHttpException extends RuntimeException implements HttpResponseMessage {

	private static final long serialVersionUID = 1L;

	private HttpStatusLine statusLine;
	private final HttpHeaderList headers;
	private HttpBody body;
	private Locale locale;
	private boolean unmodifiable;

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
	 * Constructor with a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param msg The exception message and response body. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty. Supports both {@link java.text.MessageFormat} ({@code {0}}) and {@link String#format(String, Object...) String.format} ({@code %s}) placeholders.
	 * @param args Optional message arguments.
	 */
	public BasicHttpException(int statusCode, String reasonPhrase, String msg, Object...args) {
		this(statusCode, reasonPhrase, (Throwable)null, msg, args);
	}

	/**
	 * Constructor with a cause and a {@link java.text.MessageFormat}- or {@link String#format(String, Object...) String.format}-style message body.
	 *
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @param cause The cause. May be <jk>null</jk>.
	 * @param msg The exception message. May be <jk>null</jk>.
	 *    Treated as a format pattern when {@code args} is non-empty. Supports both {@link java.text.MessageFormat} ({@code {0}}) and {@link String#format(String, Object...) String.format} ({@code %s}) placeholders.
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
	 * @param copyFrom The instance to copy. Must not be <jk>null</jk>.
	 */
	protected BasicHttpException(BasicHttpException copyFrom) {
		super(copyFrom.getMessage(), copyFrom.getCause());
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
	public BasicHttpException setStatusCode(int value) {
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
	public BasicHttpException setReasonPhrase(String value) {
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
	public BasicHttpException setProtocolVersion(HttpProtocolVersion value) {
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
	public BasicHttpException setHeaders(List<HttpHeader> values) {
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
	public BasicHttpException setHeaders(HttpHeader...values) {
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
	public BasicHttpException setHeader(HttpHeader value) {
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
	public BasicHttpException setHeader(String name, String value) {
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
	public BasicHttpException addHeader(HttpHeader value) {
		assertModifiable();
		this.headers.append(value);
		return this;
	}

	/**
	 * Appends a header to the end of the header list.
	 *
	 * @param name Header name.
	 * @param value Header value.
	 * @return This object.
	 */
	public BasicHttpException addHeader(String name, String value) {
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
	public BasicHttpException setBody(HttpBody value) {
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
		assertModifiable();
		this.locale = value;
		return this;
	}

	/**
	 * Latches this instance so that subsequent calls to mutating setters throw {@link IllegalStateException}.
	 *
	 * @return This object.
	 */
	public BasicHttpException setUnmodifiable() {
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
				sb.append(f("\nCaused by ({0})", cls));
			else
				sb.append(f("\nCaused by ({0}): {1}", cls, msg));
			e = e.getCause();
		}
		return sb.toString();
	}

	@Override /* Object */
	public String toString() {
		return statusLine.toString();
	}
}
