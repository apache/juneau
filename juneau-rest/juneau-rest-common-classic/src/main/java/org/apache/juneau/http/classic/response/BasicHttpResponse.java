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
import static org.apache.juneau.http.classic.HttpEntities.*;
import static org.apache.juneau.test.assertions.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.impl.*;
import org.apache.http.params.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.marshall.*;

/**
 * Basic implementation of the {@link HttpResponse} interface.
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
 * This is a self-typed (CRTP) root: a leaf declares <c>class Ok <jk>extends</jk> BasicHttpResponse&lt;Ok&gt;</c> and
 * inherits leaf-typed fluent setters with no covariant overrides.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@Marshalled(as=MarshalledAs.STRING)
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., PROP_status)
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class BasicHttpResponse<SELF extends BasicHttpResponse<SELF>> implements HttpResponse {

	// Argument name constants for assertArgNotNull
	private static final String ARG_response = "response";

	BasicStatusLine statusLine = new BasicStatusLine();
	HeaderList headers = HeaderList.create();
	HttpEntity content;

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.  Must not be <jk>null</jk>.
	 */
	protected BasicHttpResponse(BasicHttpResponse<?> copyFrom) {
		statusLine = copyFrom.statusLine.copy();
		headers = copyFrom.headers.copy();
		content = copyFrom.content;
	}

	/**
	 * Constructor.
	 *
	 * @param statusLine The HTTP status line.  Must not be <jk>null</jk>.
	 */
	public BasicHttpResponse(BasicStatusLine statusLine) {
		setStatusLine(statusLine.copy());
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when parsing an HTTP response.
	 *
	 * @param response The HTTP response to copy from.  Must not be <jk>null</jk>.
	 */
	public BasicHttpResponse(HttpResponse response) {
		assertArgNotNull(ARG_response, response);
		setHeaders(response.getAllHeaders());
		setContent(response.getEntity());
		setStatusLine(response.getStatusLine());
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
			content = stringEntity(getStatusLine().getReasonPhrase());
		return content;
	}

	@Override /* Overridden from HttpMessage */
	public Header getFirstHeader(String name) {
		return headers.getFirst(name).orElse(null);
	}

	/**
	 * Returns access to the underlying builder for the headers.
	 *
	 * @return The underlying builder for the headers.
	 */
	public HeaderList getHeaders() { return headers; }

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

	@SuppressWarnings({
		"deprecation" // Uses deprecated HttpMessage API
	})
	@Override /* Overridden from HttpMessage */
	public HttpParams getParams() { return null; }

	@Override /* Overridden from HttpMessage */
	public ProtocolVersion getProtocolVersion() { return statusLine.getProtocolVersion(); }

	@Override /* Overridden from HttpMessage */
	public StatusLine getStatusLine() { return statusLine; }

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
	public SELF setContent(HttpEntity value) {
		return modify(() -> content = value);
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(String value) {
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
	 * Sets the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public SELF setHeader2(Header value) {
		return modify(() -> headers.set(value));
	}

	/**
	 * Sets the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public SELF setHeader2(String name, String value) {
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
	public SELF setHeaders(HeaderList value) {
		return modify(() -> headers = value.copy());
	}

	/**
	 * Sets the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public SELF setHeaders(List<Header> values) {
		return modify(() -> headers.set(values));
	}

	/**
	 * Sets the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public SELF setHeaders2(Header...values) {
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
	public SELF setLocale2(Locale value) {
		return modify(() -> statusLine.setLocale(value));
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setLocation(String value) {
		return modify(() -> headers.set(Location.of(value)));
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setLocation(URI value) {
		return modify(() -> headers.set(Location.of(value)));
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
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setProtocolVersion(ProtocolVersion value) {
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
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setReasonPhrase2(String value) {
		return modify(() -> statusLine.setReasonPhrase(value));
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		return modify(() -> statusLine.setReasonPhraseCatalog(value));
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		modify(() -> statusLine.setStatusCode(code));
	}

	/**
	 * Sets the status code on the status line.
	 *
	 * <p>
	 * If not specified, <c>0</c> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public SELF setStatusCode2(int value) {
		return modify(() -> statusLine.setStatusCode(value));
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
	public SELF setStatusLine(BasicStatusLine value) {
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
	public abstract SELF unmodifiable();

	@Override /* Overridden from Object */
	public String toString() {
		var sb = new StringBuilder().append(statusLine).append(' ').append(headers);
		if (nn(content))
			sb.append(' ').append(content);
		return sb.toString();
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof BasicHttpResponse<?> other && eq(this, other, (x, y) ->
			eq(x.statusLine, y.statusLine) && eq(x.headers, y.headers) && eq(x.content, y.content));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return h(statusLine, headers, content);
	}

	/**
	 * Returns this object cast to the self type.
	 *
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // CRTP self-type cast is safe: SELF is bound to the concrete leaf type.
	})
	protected final SELF self() { return (SELF) this; }

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every mutator (fluent setter and {@code void} interface mutator) routes through this method.  The nested
	 * {@code Unmodifiable} snapshot overrides only this method to throw, which freezes the entire mutation surface with
	 * a single override.
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
		statusLine = statusLine.unmodifiable();   // DIRECT field write — must NOT use setStatusLine(...)
		headers = headers.unmodifiable();         // DIRECT field write — snapshot the copied HeaderList.
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
}
