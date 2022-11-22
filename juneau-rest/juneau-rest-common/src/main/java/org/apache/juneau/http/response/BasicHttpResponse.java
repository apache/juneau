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
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.http.HttpEntities.*;

import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.params.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@BeanIgnore /* Use toString() to serialize */
@FluentSetters
public class BasicHttpResponse implements HttpResponse {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	BasicStatusLine statusLine = new BasicStatusLine();
	HeaderList headers = HeaderList.create();
	HttpEntity content;
	boolean unmodifiable;

	/**
	 * Constructor.
	 *
	 * @param statusLine The HTTP status line.
	 */
	public BasicHttpResponse(BasicStatusLine statusLine) {
		setStatusLine(statusLine.copy());
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public BasicHttpResponse(BasicHttpResponse copyFrom) {
		statusLine = copyFrom.statusLine.copy();
		headers = copyFrom.headers.copy();
		content = copyFrom.content;
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
		setHeaders(response.getAllHeaders());
		setContent(response.getEntity());
		setStatusLine(response.getStatusLine());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setUnmodifiable() {
		unmodifiable = true;
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is unmodifiable.
	 */
	public boolean isUnmodifiable() {
		return unmodifiable;
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw new UnsupportedOperationException("Bean is read-only");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicStatusLine setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setStatusLine(BasicStatusLine value) {
		assertModifiable();
		statusLine = value.copy();
		return this;
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
	@FluentSetter
	public BasicHttpResponse setStatusCode2(int value) {
		statusLine.setStatusCode(value);
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
	@FluentSetter
	public BasicHttpResponse setProtocolVersion(ProtocolVersion value) {
		statusLine.setProtocolVersion(value);
		return this;
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
	@FluentSetter
	public BasicHttpResponse setReasonPhrase2(String value) {
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
	@FluentSetter
	public BasicHttpResponse setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		statusLine.setReasonPhraseCatalog(value);
		return this;
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
	@FluentSetter
	public BasicHttpResponse setLocale2(Locale value) {
		statusLine.setLocale(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderGroup setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns access to the underlying builder for the headers.
	 *
	 * @return The underlying builder for the headers.
	 */
	public HeaderList getHeaders() {
		return headers;
	}

	/**
	 * Sets the specified headers on this response.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setHeaders(HeaderList value) {
		assertModifiable();
		headers = value.copy();
		return this;
	}

	/**
	 * Sets the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setHeader2(Header value) {
		headers.set(value);
		return this;
	}

	/**
	 * Sets the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setHeader2(String name, String value) {
		headers.set(name, value);
		return this;
	}

	/**
	 * Sets the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setHeaders2(Header...values) {
		headers.set(values);
		return this;
	}

	/**
	 * Sets the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setHeaders(List<Header> values) {
		headers.set(values);
		return this;
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setLocation(URI value) {
		headers.set(Location.of(value));
		return this;
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setLocation(String value) {
		headers.set(Location.of(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Body setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setContent(String value) {
		return setContent(stringEntity(value));
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object.
	 */
	@FluentSetter
	public BasicHttpResponse setContent(HttpEntity value) {
		assertModifiable();
		this.content = value;
		return this;
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
			content = stringEntity(getStatusLine().getReasonPhrase());
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

	// <FluentSetters>

	// </FluentSetters>
}
