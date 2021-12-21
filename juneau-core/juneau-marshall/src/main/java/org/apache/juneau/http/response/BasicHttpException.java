// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http.response;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.http.HttpEntities.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
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
 * <ul class='spaced-list'>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @serial exclude
 */
@BeanIgnore /* Use toString() to serialize */
public class BasicHttpException extends BasicRuntimeException implements HttpResponse {

	private static final long serialVersionUID = 1L;

	HeaderList headers;
	BasicStatusLine statusLine;
	HeaderList.Builder headersBuilder;
	BasicStatusLine.Builder statusLineBuilder;
	HttpEntity body;

	/**
	 * Creates a builder for this class.
	 *
	 * @param implClass The subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public static <T extends BasicHttpException> HttpExceptionBuilder<T> create(Class<T> implClass) {
		return new HttpExceptionBuilder<>(implClass);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the arguments for this exception.
	 */
	public BasicHttpException(HttpExceptionBuilder<?> builder) {
		super(builder);
		headers = builder.headers();
		statusLine = builder.statusLine();
		body = builder.body;
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
		this(create(null).statusCode(statusCode).causedBy(cause).message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 */
	public BasicHttpException(int statusCode) {
		this(create(null).statusCode(statusCode));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public BasicHttpException(int statusCode, String msg, Object...args) {
		this(create(null).statusCode(statusCode).message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param causedBy The cause.  Can be <jk>null</jk>.
	 */
	public BasicHttpException(int statusCode, Throwable causedBy) {
		this(create(null).statusCode(statusCode).causedBy(causedBy));
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
		this(create(null).copyFrom(response));
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpExceptionBuilder<? extends BasicHttpException> copy() {
		return new HttpExceptionBuilder<>(this);
	}

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
		while(t != null) {
			if (! (t instanceof BasicHttpException || t instanceof InvocationTargetException))
				return t;
			t = t.getCause();
		}
		return null;
	}

	/**
	 * Asserts that the specified HTTP response has the same status code as the one on the status line of this bean.
	 *
	 * @param response The HTTP response to check.  Must not be <jk>null</jk>.
	 * @throws AssertionError If status code is not what was expected.
	 */
	protected void assertStatusCode(HttpResponse response) throws AssertionError {
		assertArgNotNull("response", response);
		int expected = getStatusLine().getStatusCode();
		int actual = response.getStatusLine().getStatusCode();
		assertInteger(actual).msg("Unexpected status code.  Expected:[{0}], Actual:[{1}]", expected, actual).is(expected);
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
				sb.append(format("\nCaused by ({0})", cls));
			else
				sb.append(format("\nCaused by ({0}): {1}", cls, msg));
			e = e.getCause();
		}
		return sb.toString();
	}

	@Override /* Throwable */
	public String getMessage() {
		String m = super.getMessage();
		if (m == null && getCause() != null)
			m = getCause().getMessage();
		if (m == null)
			m = statusLine().getReasonPhrase();
		return m;
	}

	@Override /* Object */
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

	@Override /* Object */
	public String toString() {
		return emptyIfNull(getLocalizedMessage());
	}

	@Override /* HttpMessage */
	public ProtocolVersion getProtocolVersion() {
		return statusLine().getProtocolVersion();
	}

	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return headers().contains(name);
	}

	@Override /* HttpMessage */
	public Header[] getHeaders(String name) {
		return headers().getAll(name);
	}

	@Override /* HttpMessage */
	public Header getFirstHeader(String name) {
		return headers().getFirst(name).orElse(null);
	}

	@Override /* HttpMessage */
	public Header getLastHeader(String name) {
		return headers().getLast(name).orElse(null);
	}

	@Override /* HttpMessage */
	public Header[] getAllHeaders() {
		return headers().getAll();
	}

	@Override /* HttpMessage */
	public void addHeader(Header value) {
		headersBuilder().append(value).build();
	}

	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		headersBuilder().append(name, value).build();
	}

	@Override /* HttpMessage */
	public void setHeader(Header value) {
		headersBuilder().set(value).build();
	}

	@Override /* HttpMessage */
	public void setHeader(String name, String value) {
		headersBuilder().set(name, value).build();
	}

	@Override /* HttpMessage */
	public void setHeaders(Header[] values) {
		headersBuilder().clear().append(values).build();
	}

	@Override /* HttpMessage */
	public void removeHeader(Header value) {
		headersBuilder().remove(value).build();
	}

	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		headersBuilder().remove(name).build();
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return headers().iterator();
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers().iterator(name);
	}

	@SuppressWarnings("deprecation")
	@Override /* HttpMessage */
	public HttpParams getParams() {
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override /* HttpMessage */
	public void setParams(HttpParams params) {
	}

	@Override /* HttpMessage */
	public StatusLine getStatusLine() {
		return statusLine();
	}

	@Override /* HttpMessage */
	public void setStatusLine(StatusLine value) {
		setStatusLine(value.getProtocolVersion(), value.getStatusCode(), value.getReasonPhrase());
	}

	@Override /* HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code) {
		statusLineBuilder().protocolVersion(ver).statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		statusLineBuilder().protocolVersion(ver).reasonPhrase(reason).statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		statusLineBuilder().statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setReasonPhrase(String reason) throws IllegalStateException {
		statusLineBuilder().reasonPhrase(reason).build();
	}

	@Override /* HttpMessage */
	public HttpEntity getEntity() {
		// Constructing a StringEntity is somewhat expensive, so don't create it unless it's needed.
		if (body == null)
			body = stringEntity(getMessage()).build();
		return body;
	}

	@Override /* HttpMessage */
	public void setEntity(HttpEntity entity) {
		assertModifiable();
		this.body = entity;
	}

	@Override /* HttpMessage */
	public Locale getLocale() {
		return statusLine().getLocale();
	}

	@Override /* HttpMessage */
	public void setLocale(Locale loc) {
		statusLineBuilder().locale(loc).build();
	}

	private BasicStatusLine statusLine() {
		if (statusLine == null) {
			statusLine = statusLineBuilder.build();
			statusLineBuilder = null;
		}
		return statusLine;
	}

	private HeaderList headers() {
		if (headers == null) {
			headers = headersBuilder.build();
			headersBuilder = null;
		}
		return headers;
	}

	private BasicStatusLine.Builder statusLineBuilder() {
		assertModifiable();
		if (statusLineBuilder == null) {
			statusLineBuilder = statusLine.copy();
			statusLine = null;
		}
		return statusLineBuilder;
	}

	private HeaderList.Builder headersBuilder() {
		assertModifiable();
		if (headersBuilder == null) {
			headersBuilder = headers.copy();
			headers = null;
		}
		return headersBuilder;
	}
}
