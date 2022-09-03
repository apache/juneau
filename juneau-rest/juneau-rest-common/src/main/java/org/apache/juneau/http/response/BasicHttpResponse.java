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
import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.internal.ArgUtils.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.params.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;

/**
 * Basic implementation of the {@link HttpResponse} interface.
 *
 * <p>
 * Although this class implements the various setters defined on the {@link HttpResponse} interface, it's in general
 * going to be more efficient to set the status/headers/content of this bean through the builder.
 *
 * <p>
 * If the <c>unmodifiable</c> flag is set on this bean, calls to the setters will throw {@link UnsupportedOperationException} exceptions.
 *
 * <ul class='notes'>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@BeanIgnore /* Use toString() to serialize */
public class BasicHttpResponse implements HttpResponse {

	HeaderList headers;
	BasicStatusLine statusLine = new BasicStatusLine();
	HttpEntity content;
	final boolean unmodifiable;

	/**
	 * Creates a builder for this class.
	 *
	 * @param <T> The implementation type.
	 * @param implClass The subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public static <T extends BasicHttpResponse> HttpResponseBuilder<T> create(Class<T> implClass) {
		return new HttpResponseBuilder<>(implClass);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the arguments for this bean.
	 */
	public BasicHttpResponse(HttpResponseBuilder<?> builder) {
		headers = builder.headers;
		statusLine = builder.statusLine.copy();
		content = builder.content;
		unmodifiable = builder.unmodifiable;
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
		this(create(null).copyFrom(response));
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpResponseBuilder<? extends BasicHttpResponse> copy() {
		return new HttpResponseBuilder<>(this);
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
		assertInteger(actual).setMsg("Unexpected status code.  Expected:[{0}], Actual:[{1}]", expected, actual).is(expected);
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder().append(statusLine).append(' ').append(headers);
		if (content != null)
			sb.append(' ').append(content);
		return sb.toString();
	}

	@Override /* HttpMessage */
	public ProtocolVersion getProtocolVersion() {
		return statusLine.getProtocolVersion();
	}

	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return headers.contains(name);
	}

	@Override /* HttpMessage */
	public Header[] getHeaders(String name) {
		return headers.getAll(name);
	}

	@Override /* HttpMessage */
	public Header getFirstHeader(String name) {
		return headers.getFirst(name).orElse(null);
	}

	@Override /* HttpMessage */
	public Header getLastHeader(String name) {
		return headers.getLast(name).orElse(null);
	}

	@Override /* HttpMessage */
	public Header[] getAllHeaders() {
		return headers.getAll();
	}

	@Override /* HttpMessage */
	public void addHeader(Header value) {
		headers.append(value);
	}

	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		headers.append(name, value);
	}

	@Override /* HttpMessage */
	public void setHeader(Header value) {
		headers.set(value);
	}

	@Override /* HttpMessage */
	public void setHeader(String name, String value) {
		headers.set(name, value);
	}

	@Override /* HttpMessage */
	public void setHeaders(Header[] values) {
		headers.removeAll().append(values);
	}

	@Override /* HttpMessage */
	public void removeHeader(Header value) {
		headers.remove(value);
	}

	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		headers.remove(name);
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return headers.headerIterator();
	}

	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers.headerIterator(name);
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
		statusLine.setProtocolVersion(ver).setStatusCode(code);
	}

	@Override /* HttpMessage */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		statusLine.setProtocolVersion(ver).setReasonPhrase(reason).setStatusCode(code);
	}

	@Override /* HttpMessage */
	public void setStatusCode(int code) throws IllegalStateException {
		statusLine.setStatusCode(code);
	}

	@Override /* HttpMessage */
	public void setReasonPhrase(String reason) throws IllegalStateException {
		statusLine.setReasonPhrase(reason);
	}

	@Override /* HttpMessage */
	public HttpEntity getEntity() {
		// Constructing a StringEntity is somewhat expensive, so don't create it unless it's needed.
		if (content == null)
			content = stringEntity(getStatusLine().getReasonPhrase()).build();
		return content;
	}

	@Override /* HttpMessage */
	public void setEntity(HttpEntity entity) {
		assertModifiable();
		this.content = entity;
	}

	@Override /* HttpMessage */
	public Locale getLocale() {
		return statusLine.getLocale();
	}

	@Override /* HttpMessage */
	public void setLocale(Locale loc) {
		statusLine.setLocale(loc);
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw new UnsupportedOperationException("Bean is read-only");
	}
}
