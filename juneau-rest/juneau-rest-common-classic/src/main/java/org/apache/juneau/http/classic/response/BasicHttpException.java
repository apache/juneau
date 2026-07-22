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
package org.apache.juneau.http.classic.response;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.classic.HttpEntities.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;
import static org.apache.juneau.test.assertions.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.params.*;
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.marshall.*;

/**
 * Basic implementation of the {@link HttpResponse} interface for error responses.
 *
 * <p>
 * Although this class implements the various setters defined on the {@link HttpResponse} interface, it's in general
 * going to be more efficient to set the status/headers/content of this bean through the builder.
 *
 * <p>
 * Immutability is expressed with the "funnel + nested {@code Unmodifiable} snapshot" paradigm: every mutator (fluent
 * setter and {@code void} interface mutator) routes through the single protected {@link #modify(Runnable)} choke-point,
 * and each concrete leaf gets a nested {@code X.Unmodifiable} whose only behavioral override is a throwing
 * {@code modify(...)}.  A leaf's {@link #unmodifiable()} factory returns a point-in-time snapshot of type
 * {@code X.Unmodifiable}; any attempt to set a property on it throws an {@link UnsupportedOperationException}.
 *
 * <p>
 * This is a {@link Throwable}-rooted hierarchy, so (unlike {@link BasicHttpResponse}) it cannot be a self-typed (CRTP)
 * root — JLS &sect;8.1.2 forbids a generic {@code Throwable}.  The funnel therefore returns the base type and each leaf
 * keeps its covariant setter overrides.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@Marshalled(as=MarshalledAs.STRING)
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., PROP_status)
	"java:S110" // Deep exception-hierarchy inheritance is structural and intentional; flattening would break the HTTP-response exception model.
})
public class BasicHttpException extends BasicRuntimeException implements HttpResponse {

	// Argument name constants for assertArgNotNull
	private static final String ARG_response = "response";

	private static final long serialVersionUID = 1L;

	@SuppressWarnings({
		"java:S1104", // Fields reassigned after construction, cannot be final
		"java:S1165"  // Cannot be final; setHeaders() replaces the list entirely via headers = value.copy()
	})
	HeaderList headers = HeaderList.create();
	@SuppressWarnings({
		"java:S1104", // Field reassigned after construction, cannot be final
		"java:S1165"  // Cannot be final; setStatusLine() replaces it entirely via statusLine = value.copy()
	})
	transient BasicStatusLine statusLine = new BasicStatusLine();
	@SuppressWarnings({
		"java:S1104", // Field reassigned after construction, cannot be final
		"java:S1165"  // Cannot be final; setContent() replaces it via content = value
	})
	transient HttpEntity content;

	/**
	 * Constructor.
	 */
	public BasicHttpException() {
		super((Throwable)null);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when parsing an HTTP response.
	 *
	 * @param response The HTTP response being parsed.  Must not be <jk>null</jk>.
	 */
	public BasicHttpException(HttpResponse response) {
		super((Throwable)null);
		assertArgNotNull(ARG_response, response);
		var h = response.getLastHeader("Thrown");
		if (nn(h)) {
			var partsOpt = thrown(h.getValue()).asParts();
			if (partsOpt.isPresent() && !partsOpt.get().isEmpty())
				setMessage(partsOpt.get().get(0).getMessage());
		}
		setHeaders(response.getAllHeaders());
		setContent(response.getEntity());
		setStatusCode(response.getStatusLine().getStatusCode());
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 */
	public BasicHttpException(int statusCode) {
		super((Throwable)null);
		setStatusCode(statusCode);
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param msg The message.  Can be <jk>null</jk>, in which case the reason phrase is used as the message.
	 * @param args Optional <c>printf</c>-style (<js>"%s"</js>) arguments in the message.
	 */
	public BasicHttpException(int statusCode, String msg, Object...args) {
		super(msg, args);
		setStatusCode(statusCode);
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param causedBy The cause.  Can be <jk>null</jk>, in which case no cause is chained and the reason phrase is used as the message.
	 */
	public BasicHttpException(int statusCode, Throwable causedBy) {
		super(causedBy);
		setStatusCode(statusCode);
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param cause The caused-by exception.  Can be <jk>null</jk> (no cause is chained).
	 * @param msg The message.  Can be <jk>null</jk>, in which case the cause's message is used, or the reason phrase if the cause is also <jk>null</jk> or has no message.
	 * @param args The message arguments.
	 */
	public BasicHttpException(int statusCode, Throwable cause, String msg, Object...args) {
		super(cause, msg, args);
		setStatusCode(statusCode);
		setContent(f(msg, args));
	}

	/**
	 * Copy constructor.
	 *
	 * <p>
	 * The mutable sub-beans are deep-copied by <b>direct field assignment</b>, never through a funneled setter.  This is
	 * the snapshot path used by {@link #unmodifiable()}: the constructor runs inside an {@code Unmodifiable} instance
	 * whose {@link #modify(Runnable)} already throws, so a setter-based copy would blow up during construction.
	 *
	 * @param copyFrom The bean to copy.  Must not be <jk>null</jk>.
	 */
	protected BasicHttpException(BasicHttpException copyFrom) {
		super(copyFrom.getCause(), "%s", copyFrom.getMessage());
		statusLine = copyFrom.statusLine.copy();
		headers = copyFrom.headers.copy();
		content = copyFrom.content;
	}

	@Override /* Overridden from HttpMessage */
	public void addHeader(Header value) {
		modify(() -> headers.append(value));
	}

	@Override /* Overridden from HttpMessage */
	public void addHeader(String name, String value) {
		modify(() -> headers.append(name, value));
	}

	@Override /* Overridden from HttpMessage */
	public boolean containsHeader(String name) {
		return headers.contains(name);
	}

	@Override /* Overridden from HttpMessage */
	public Header[] getAllHeaders() { return headers.getAll(); }

	@Override /* Overridden from HttpMessage */
	public HttpEntity getEntity() {
		// Constructing a StringEntity is somewhat expensive, so don't create it unless it's needed.
		if (content == null)
			content = stringEntity(getMessage());
		return content;
	}

	@Override /* Overridden from HttpMessage */
	public Header getFirstHeader(String name) {
		return headers.getFirst(name).orElse(null);
	}

	/**
	 * Returns all error messages from all errors in this stack.
	 *
	 * <p>
	 * Typically useful if you want to render all the error messages in the stack, but don't want to render all the
	 * stack traces too.
	 *
	 * @param scrubForXssVulnerabilities
	 * 	If <jk>true</jk>, replaces <js>'&lt;'</js>, <js>'&gt;'</js>, and <js>'&amp;'</js> characters with spaces.
	 * @return All error messages from all errors in this stack.
	 */
	public String getFullStackMessage(boolean scrubForXssVulnerabilities) {
		var msg = getMessage();
		StringBuilder sb = new StringBuilder();
		if (nn(msg)) {
			if (scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			sb.append(msg);
		}
		Throwable e = getCause();
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

	/**
	 * Returns access to the underlying builder for the headers.
	 *
	 * @return The underlying builder for the headers.
	 */
	public HeaderList getHeaders() {
		return headers;
	}

	@Override /* Overridden from HttpMessage */
	public Header[] getHeaders(String name) {
		return headers.getAll(name);
	}

	@Override /* Overridden from HttpMessage */
	public Header getLastHeader(String name) {
		return headers.getLast(name).orElse(null);
	}

	@Override /* Overridden from HttpMessage */
	public Locale getLocale() { return statusLine.getLocale(); }

	@Override /* Overridden from Throwable */
	public String getMessage() {
		String m = super.getMessage();
		if (m == null && nn(getCause()))
			m = getCause().getMessage();
		if (m == null)
			m = statusLine.getReasonPhrase();
		return m;
	}

	@SuppressWarnings({
		"deprecation" // Uses deprecated HttpMessage API
	})
	@Override /* Overridden from HttpMessage */
	public HttpParams getParams() { return null; }

	@Override /* Overridden from HttpMessage */
	public ProtocolVersion getProtocolVersion() { return statusLine.getProtocolVersion(); }

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

	@Override /* Overridden from HttpMessage */
	public StatusLine getStatusLine() { return statusLine; }

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		// D3 content equality.  The 'content' HttpEntity is deliberately excluded: it is a wrapper without value
		// semantics (two StringEntity instances of the same text are not equal), and it is derived from the message
		// (getEntity() lazily builds stringEntity(getMessage())), which is already compared here.
		return o instanceof BasicHttpException other && eq(this, other, (x, y) ->
			eq(x.statusLine, y.statusLine) && eq(x.headers, y.headers) && eq(x.getMessage(), y.getMessage()));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return h(statusLine, headers, getMessage());
	}

	@Override /* Overridden from HttpMessage */
	public HeaderIterator headerIterator() {
		return headers.headerIterator();
	}

	@Override /* Overridden from HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers.headerIterator(name);
	}

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is an {@link UnmodifiableBean} snapshot.
	 */
	public boolean isUnmodifiable() { return this instanceof UnmodifiableBean; }

	@Override /* Overridden from HttpMessage */
	public void removeHeader(Header value) {
		modify(() -> headers.remove(value));
	}

	@Override /* Overridden from HttpMessage */
	public void removeHeaders(String name) {
		modify(() -> headers.remove(name));
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public BasicHttpException setContent(HttpEntity value) {
		return modify(() -> content = value);
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.  Can be <jk>null</jk> (treated as an empty string).
	 * @return This object.
	 */
	public BasicHttpException setContent(String value) {
		return modify(() -> content = stringEntity(value));
	}

	@Override /* Overridden from HttpMessage */
	public void setEntity(HttpEntity entity) {
		modify(() -> content = entity);
	}

	@Override /* Overridden from HttpMessage */
	public void setHeader(Header value) {
		modify(() -> headers.set(value));
	}

	@Override /* Overridden from HttpMessage */
	public void setHeader(String name, String value) {
		modify(() -> headers.set(name, value));
	}

	/**
	 * Sets a header on this response.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public BasicHttpException setHeader2(String name, Object value) {
		return modify(() -> headers.set(name, value));
	}

	@Override /* Overridden from HttpMessage */
	public void setHeaders(Header[] values) {
		modify(() -> headers.removeAll().append(values));
	}

	/**
	 * Sets the specified headers on this response.
	 *
	 * @param value The new value.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setHeaders(HeaderList value) {
		return modify(() -> headers = value.copy());
	}

	/**
	 * Sets the specified headers on this response.
	 *
	 * @param values The headers to set.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public BasicHttpException setHeaders(List<Header> values) {
		return modify(() -> headers.set(values));
	}

	/**
	 * Sets multiple headers on this response.
	 *
	 * @param values The headers to add.
	 * @return This object.
	 */
	public BasicHttpException setHeaders2(Header...values) {
		return modify(() -> headers.set(values));
	}

	@Override /* Overridden from HttpMessage */
	public void setLocale(Locale loc) {
		modify(() -> statusLine.setLocale(loc));
	}

	/**
	 * Sets the locale used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link Locale#getDefault()}.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setLocale2(Locale value) {
		return modify(() -> statusLine.setLocale(value));
	}

	@Override /* Overridden from BasicRuntimeException */
	public BasicHttpException setMessage(String message, Object...args) {
		return modify(() -> super.setMessage(message, args));
	}

	@SuppressWarnings({
		"deprecation" // Uses deprecated HttpMessage API
	})
	@Override /* Overridden from HttpMessage */
	public void setParams(HttpParams params) {
		// Deprecated optional interface method; routed through the funnel so it is frozen on unmodifiable snapshots.
		modify(() -> { /* No-op */ });
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.  Can be <jk>null</jk>, in which case the default protocol version is cleared and {@link BasicStatusLine#getProtocolVersion()} returns <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setProtocolVersion(ProtocolVersion value) {
		return modify(() -> statusLine.setProtocolVersion(value));
	}

	@Override /* Overridden from HttpMessage */
	public void setReasonPhrase(String reason) throws IllegalStateException {
		modify(() -> statusLine.setReasonPhrase(reason));
	}

	/**
	 * Sets the reason phrase on the status line.
	 *
	 * <p>
	 * If not specified, the reason phrase will be retrieved from the reason phrase catalog
	 * using the locale on this builder.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to fall back to the reason phrase catalog.
	 * @return This object.
	 */
	public BasicHttpException setReasonPhrase2(String value) {
		return modify(() -> statusLine.setReasonPhrase(value));
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to fall back to {@link EnglishReasonPhraseCatalog#INSTANCE}.
	 * @return This object.
	 */
	public BasicHttpException setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		return modify(() -> statusLine.setReasonPhraseCatalog(value));
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		modify(() -> statusLine.setStatusCode(code));
	}

	/**
	 * Same as {@link #setStatusCode(int)} but returns this object.
	 *
	 * @param code The new status code.
	 * @return This object.
	 * @throws IllegalStateException If status code could not be set.
	 */
	public BasicHttpException setStatusCode2(int code) throws IllegalStateException {
		return modify(() -> statusLine.setStatusCode(code));
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpException setStatusLine(BasicStatusLine value) {
		return modify(() -> statusLine = value.copy());
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code) {
		modify(() -> statusLine.setProtocolVersion(ver).setStatusCode(code));
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		modify(() -> statusLine.setProtocolVersion(ver).setReasonPhrase(reason).setStatusCode(code));
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(StatusLine value) {
		modify(() -> statusLine.setProtocolVersion(value.getProtocolVersion()).setReasonPhrase(value.getReasonPhrase()).setStatusCode(value.getStatusCode()));
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
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return emptyIfNull(getLocalizedMessage());
	}

	/**
	 * Asserts that the specified HTTP response has the same status code as the one on the status line of this bean.
	 *
	 * @param response The HTTP response to check.  Must not be <jk>null</jk>.
	 * @throws AssertionError If status code is not what was expected.
	 */
	protected void assertStatusCode(HttpResponse response) throws AssertionError {
		assertArgNotNull(ARG_response, response);
		int expected = getStatusLine().getStatusCode();
		int actual = response.getStatusLine().getStatusCode();
		assertInteger(actual).setMsg("Unexpected status code.  Expected:[%s], Actual:[%s]", expected, actual).is(expected);
	}

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every mutator (fluent setter and {@code void} interface mutator) routes through this method.  The nested
	 * {@code Unmodifiable} snapshot overrides only this method to throw, which freezes the entire mutation surface with
	 * a single override.  Because this is a {@link Throwable}-rooted hierarchy (no CRTP self-type), the funnel returns
	 * the base type.
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
		statusLine = statusLine.unmodifiable();   // DIRECT field write — must NOT use setStatusLine(...)
		headers = headers.unmodifiable();         // DIRECT field write — snapshot the copied HeaderList.
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
