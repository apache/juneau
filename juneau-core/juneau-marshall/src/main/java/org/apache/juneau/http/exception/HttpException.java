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
package org.apache.juneau.http.exception;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.entity.*;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.BasicHeader;
import org.apache.juneau.http.BasicStatusLine;
import org.apache.juneau.http.annotation.*;

/**
 * Exception thrown to trigger an error HTTP status.
 *
 * <p>
 * Note that while the {@link HttpResponse} interface allows you to modify the headers and status line on this
 * bean, it is more efficient to do so in the builder.
 */
@Response
@BeanIgnore
public class HttpException extends BasicRuntimeException implements HttpResponse {

	private static final long serialVersionUID = 1L;

	BasicHeaderGroup headers;
	BasicStatusLine statusLine;
	HttpEntity body;

	/**
	 * Creates a builder for this class.
	 *
	 * @param implClass The subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public static <T extends HttpException> HttpExceptionBuilder<T> create(Class<T> implClass) {
		return new HttpExceptionBuilder<>(implClass);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the arguments for this exception.
	 */
	public HttpException(HttpExceptionBuilder<?> builder) {
		super(builder);
		headers = builder.headers.build();
		statusLine = builder.statusLine.build();
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
	public HttpException(int statusCode, Throwable cause, String msg, Object...args) {
		this(create(null).statusCode(statusCode).causedBy(cause).message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 */
	public HttpException(int statusCode) {
		this(create(null).statusCode(statusCode));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param msg The message.  Can be <jk>null</jk>.
	 * @param args Optional {@link MessageFormat}-style arguments in the message.
	 */
	public HttpException(int statusCode, String msg, Object...args) {
		this(create(null).statusCode(statusCode).message(msg, args));
	}

	/**
	 * Constructor.
	 *
	 * @param statusCode The HTTP status code.
	 * @param causedBy The cause.  Can be <jk>null</jk>.
	 */
	public HttpException(int statusCode, Throwable causedBy) {
		this(create(null).statusCode(statusCode).causedBy(causedBy));
	}

	/**
	 * Creates a builder for this class initialized with the contents of this exception.
	 *
	 * @param implClass The subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public <T extends HttpException> HttpExceptionBuilder<T> builder(Class<T> implClass) {
		return create(implClass).copyFrom(this);
	}

	/**
	 * Returns the root cause of this exception.
	 *
	 * <p>
	 * The root cause is the first exception in the init-cause parent chain that's not one of the following:
	 * <ul>
	 * 	<li>{@link HttpException}
	 * 	<li>{@link InvocationTargetException}
	 * </ul>
	 *
	 * @return The root cause of this exception, or <jk>null</jk> if no root cause was found.
	 */
	public Throwable getRootCause() {
		Throwable t = this;
		while(t != null) {
			if (! (t instanceof HttpException || t instanceof InvocationTargetException))
				return t;
			t = t.getCause();
		}
		return null;
	}

	/**
	 * Returns the HTTP status code of this response.
	 *
	 * @return The HTTP status code of this response.
	 */
	@ResponseStatus
	public int getStatusCode() {
		return statusLine.getStatusCode();
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
			m = statusLine.getReasonPhrase();
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
		return statusLine.getProtocolVersion();
	}

	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return headers.containsHeader(name);
	}

	@Override /* HttpMessage */
	public Header[] getHeaders(String name) {
		return headers.getHeaders(name);
	}

	@Override /* HttpMessage */
	public Header getFirstHeader(String name) {
		return headers.getFirstHeader(name).orElse(null);
	}

	@Override /* HttpMessage */
	public Header getLastHeader(String name) {
		return headers.getLastHeader(name).orElse(null);
	}

	@Override /* HttpMessage */
	@ResponseHeader("*")
	public Header[] getAllHeaders() {
		return headers.getAllHeaders();
	}

	@Override /* HttpMessage */
	public void addHeader(Header value) {
		headers = headersBuilder().add(value).build();
	}

	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		headers = headersBuilder().add(new BasicHeader(name, value)).build();
	}

	@Override /* HttpMessage */
	public void setHeader(Header value) {
		headers = headersBuilder().update(value).build();
	}

	@Override /* HttpMessage */
	public void setHeader(String name, String value) {
		headers = headersBuilder().update(new BasicHeader(name, value)).build();
	}

	@Override /* HttpMessage */
	public void setHeaders(Header[] values) {
		headers = headersBuilder().set(values).build();
	}

	@Override /* HttpMessage */
	public void removeHeader(Header value) {
		headers = headersBuilder().remove(value).build();
	}

	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		headers = headersBuilder().remove(name).build();
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return headers.iterator();
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers.iterator(name);
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
		return statusLine;
	}

	@Override /* HttpMessage */
	public void setStatusLine(StatusLine value) {
		setStatusLine(value.getProtocolVersion(), value.getStatusCode(), value.getReasonPhrase());
	}

	@Override /* HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code) {
		statusLine = statusLineBuilder().protocolVersion(ver).statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		statusLine = statusLineBuilder().protocolVersion(ver).reasonPhrase(reason).statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		statusLine = statusLineBuilder().statusCode(code).build();
	}

	@Override /* HttpMessage */
	public void setReasonPhrase(String reason) throws IllegalStateException {
		statusLine = statusLineBuilder().reasonPhrase(reason).build();
	}

	@ResponseBody
	@Override /* HttpMessage */
	public HttpEntity getEntity() {
		// Constructing a StringEntity is somewhat expensive, so don't create it unless it's needed.
		if (body == null) {
			try {
				String msg = getMessage();
				if (msg != null)
					body = new StringEntity(msg);
			} catch (UnsupportedEncodingException e) {}
		}
		return body;
	}

	@Override /* HttpMessage */
	public void setEntity(HttpEntity entity) {
		assertModifiable();
		this.body = entity;
	}

	@Override /* HttpMessage */
	public Locale getLocale() {
		return statusLine.getLocale();
	}

	@Override /* HttpMessage */
	public void setLocale(Locale loc) {
		statusLine = statusLineBuilder().locale(loc).build();
	}

	private BasicStatusLineBuilder statusLineBuilder() {
		assertModifiable();
		return statusLine.builder();
	}

	private BasicHeaderGroupBuilder headersBuilder() {
		assertModifiable();
		return headers.builder();
	}
}
