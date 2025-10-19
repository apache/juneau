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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;

/**
 * Basic implementation of the {@link HttpResponse} interface for error responses.
 *
 * <p>
 * Although this class implements the various setters defined on the {@link HttpResponse} interface, it's in general
 * going to be more efficient to set the status/headers/content of this bean through the builder.
 *
 * <p>
 * If the <c>unmodifiable</c> flag is set on this bean, calls to the setters will throw {@link UnsupportedOperationException} exceptions.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 *
 * @serial exclude
 */
@BeanIgnore /* Use toString() to serialize */
public class BasicHttpException extends BasicRuntimeException implements HttpResponse {

	private static final long serialVersionUID = 1L;

	HeaderList headers = HeaderList.create();
	BasicStatusLine statusLine = new BasicStatusLine();
	HttpEntity content;

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
	 * @param response The HTTP response being parsed.
	 */
	public BasicHttpException(HttpResponse response) {
		super((Throwable)null);
		Header h = response.getLastHeader("Thrown");
		if (h != null)
			setMessage(thrown(h.getValue()).asParts().get().get(0).getMessage());
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
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public BasicHttpException(int statusCode, String msg, Object...args) {
		super(msg, args);
		setStatusCode(statusCode);
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param causedBy The cause.  Can be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, Throwable causedBy) {
		super(causedBy);
		setStatusCode(statusCode);
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param cause The caused-by exception.  Can be <jk>null</jk>.
	 * @param msg The message.  Can be <jk>null</jk>.
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
	 * @param copyFrom The bean to copy.
	 */
	protected BasicHttpException(BasicHttpException copyFrom) {
		this(0, copyFrom.getCause(), copyFrom.getMessage());
		setStatusLine(copyFrom.statusLine.copy());
	}

	@Override /* Overridden from HttpMessage */
	public void addHeader(Header value) {
		headers.append(value);
	}

	@Override /* Overridden from HttpMessage */
	public void addHeader(String name, String value) {
		headers.append(name, value);
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
		String msg = getMessage();
		StringBuilder sb = new StringBuilder();
		if (msg != null) {
			if (scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			sb.append(msg);
		}
		Throwable e = getCause();
		while (e != null) {
			msg = e.getMessage();
			if (msg != null && scrubForXssVulnerabilities)
				msg = msg.replace('<', ' ').replace('>', ' ').replace('&', ' ');
			String cls = e.getClass().getSimpleName();
			if (msg == null)
				sb.append(f("\nCaused by ({0})", cls));
			else
				sb.append(f("\nCaused by ({0}): {1}", cls, msg));
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
		assertModifiable();
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
		if (m == null && getCause() != null)
			m = getCause().getMessage();
		if (m == null)
			m = statusLine.getReasonPhrase();
		return m;
	}

	@SuppressWarnings("deprecation")
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
		while (t != null) {
			if (! (t instanceof BasicHttpException || t instanceof InvocationTargetException))
				return t;
			t = t.getCause();
		}
		return null;
	}

	@Override /* Overridden from HttpMessage */
	public StatusLine getStatusLine() { return statusLine; }

	@Override /* Overridden from Object */
	public int hashCode() {
		int i = 0;
		Throwable t = this;
		while (t != null) {
			for (StackTraceElement e : t.getStackTrace())
				i ^= e.hashCode();
			t = t.getCause();
		}
		return i;
	}

	@Override /* Overridden from HttpMessage */
	public HeaderIterator headerIterator() {
		return headers.headerIterator();
	}

	@Override /* Overridden from HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers.headerIterator(name);
	}

	@Override /* Overridden from HttpMessage */
	public void removeHeader(Header value) {
		headers.remove(value);
	}

	@Override /* Overridden from HttpMessage */
	public void removeHeaders(String name) {
		headers.remove(name);
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object.
	 */
	public BasicHttpException setContent(HttpEntity value) {
		assertModifiable();
		this.content = value;
		return this;
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object.
	 */
	public BasicHttpException setContent(String value) {
		setContent(stringEntity(value));
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setEntity(HttpEntity entity) {
		assertModifiable();
		this.content = entity;
	}

	@Override /* Overridden from HttpMessage */
	public void setHeader(Header value) {
		headers.set(value);
	}

	@Override /* Overridden from HttpMessage */
	public void setHeader(String name, String value) {
		headers.set(name, value);
	}

	/**
	 * Sets a header on this response.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public BasicHttpException setHeader2(String name, Object value) {
		headers.set(name, value);
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setHeaders(Header[] values) {
		headers.removeAll().append(values);
	}

	/**
	 * Sets the specified headers on this response.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setHeaders(HeaderList value) {
		assertModifiable();
		headers = value.copy();
		return this;
	}

	/**
	 * Sets the specified headers on this response.
	 *
	 * @param values The headers to set.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public BasicHttpException setHeaders(List<Header> values) {
		headers.set(values);
		return this;
	}

	/**
	 * Sets multiple headers on this response.
	 *
	 * @param values The headers to add.
	 * @return This object.
	 */
	public BasicHttpException setHeaders2(Header...values) {
		headers.set(values);
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setLocale(Locale loc) {
		statusLine.setLocale(loc);
	}

	/**
	 * Sets the locale used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link Locale#getDefault()}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setLocale2(Locale value) {
		statusLine.setLocale(value);
		return this;
	}

	@Override /* Overridden from BasicRuntimeException */
	public BasicHttpException setMessage(String message, Object...args) {
		super.setMessage(message, args);
		return this;
	}

	@SuppressWarnings("deprecation")
	@Override /* Overridden from HttpMessage */
	public void setParams(HttpParams params) {}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setProtocolVersion(ProtocolVersion value) {
		statusLine.setProtocolVersion(value);
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setReasonPhrase(String reason) throws IllegalStateException {
		statusLine.setReasonPhrase(reason);
	}

	/**
	 * Sets the reason phrase on the status line.
	 *
	 * <p>
	 * If not specified, the reason phrase will be retrieved from the reason phrase catalog
	 * using the locale on this builder.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setReasonPhrase2(String value) {
		statusLine.setReasonPhrase(value);
		return this;
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		statusLine.setReasonPhraseCatalog(value);
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		statusLine.setStatusCode(code);
	}

	/**
	 * Same as {@link #setStatusCode(int)} but returns this object.
	 *
	 * @param code The new status code.
	 * @return This object.
	 * @throws IllegalStateException If status code could not be set.
	 */
	public BasicHttpException setStatusCode2(int code) throws IllegalStateException {
		setStatusCode(code);
		return this;
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicHttpException setStatusLine(BasicStatusLine value) {
		assertModifiable();
		statusLine = value.copy();
		return this;
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code) {
		statusLine.setProtocolVersion(ver).setStatusCode(code);
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		statusLine.setProtocolVersion(ver).setReasonPhrase(reason).setStatusCode(code);
	}

	@Override /* Overridden from HttpMessage */
	public void setStatusLine(StatusLine value) {
		setStatusLine(value.getProtocolVersion(), value.getStatusCode(), value.getReasonPhrase());
	}

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	@Override
	public BasicHttpException setUnmodifiable() {
		super.setUnmodifiable();
		statusLine.setUnmodifiable();
		return this;
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
		Utils.assertArgNotNull("response", response);
		int expected = getStatusLine().getStatusCode();
		int actual = response.getStatusLine().getStatusCode();
		assertInteger(actual).setMsg("Unexpected status code.  Expected:[{0}], Actual:[{1}]", expected, actual).is(expected);
	}
}