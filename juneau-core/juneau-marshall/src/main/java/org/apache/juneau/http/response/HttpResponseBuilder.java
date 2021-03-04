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

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.impl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link HttpResponse} beans.
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="HttpResponseBuilder<T>")
public class HttpResponseBuilder<T extends BasicHttpResponse> {

	BasicStatusLine statusLine;
	HeaderList headerList = HeaderList.EMPTY;
	BasicStatusLineBuilder statusLineBuilder;
	HeaderListBuilder headerListBuilder;
	HttpEntity body;
	boolean unmodifiable;
	
	private final Class<? extends BasicHttpResponse> implClass;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link HttpResponse} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpResponseBuilder} object.
	 */
	public HttpResponseBuilder(Class<T> implClass) {
		this.implClass = implClass;
	}

	/**
	 * Instantiates the exception bean from the settings in this builder.
	 *
	 * @return A new {@link HttpResponse} bean.
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(HttpResponseBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Copies the contents of the specified HTTP response to this builder.
	 *
	 * @param response The response to copy from.  Must not be null.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<?> copyFrom(HttpResponse response) {
		headers(response.getAllHeaders());
		body(response.getEntity());
		return this;
	}

	/**
	 * Copies the values from the specified exception.
	 *
	 * @param value The exception to copy from.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> copyFrom(BasicHttpResponse value) {
		statusLine = value.statusLine;
		headerList = value.headerList;
		body = value.body;
		return this;
	}

	BasicStatusLine statusLine() {
		if (statusLineBuilder != null)
			return statusLineBuilder.build();
		return statusLine;
	}

	HeaderList headerList() {
		if (headerListBuilder != null)
			return headerListBuilder.build();
		if (headerList == null)
			return HeaderList.EMPTY;
		return headerList;
	}

	/**
	 * Specifies whether this exception should be unmodifiable after creation.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> unmodifiable() {
		unmodifiable = true;
		return this;
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> statusLine(BasicStatusLine value) {
		statusLine = value;
		statusLineBuilder = null;
		return this;
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> protocolVersion(ProtocolVersion value) {
		statusLineBuilder().protocolVersion(value);
		return this;
	}

	/**
	 * Sets the status code on the status line.
	 *
	 * <p>
	 * If not specified, <c>0</c> will be used.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> statusCode(int value) {
		statusLineBuilder().statusCode(value);
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> reasonPhrase(String value) {
		statusLineBuilder().reasonPhrase(value);
		return this;
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> reasonPhraseCatalog(ReasonPhraseCatalog value) {
		statusLineBuilder().reasonPhraseCatalog(value);
		return this;
	}

	/**
	 * Sets the locale used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link Locale#getDefault()}.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> locale(Locale value) {
		statusLineBuilder().locale(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderGroup setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> headerList(HeaderList value) {
		headerList = value;
		headerListBuilder = null;
		return this;
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> clearHeaders() {
		headerListBuilder().clear();
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> header(Header value) {
		headerListBuilder().add(value);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> header(String name, String value) {
		headerListBuilder().add(name, value);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> headers(Header...values) {
		headerListBuilder().add(values);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> headers(List<Header> values) {
		headerListBuilder().add(values);
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> removeHeader(Header value) {
		headerListBuilder().remove(value);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> removeHeaders(Header...values) {
		headerListBuilder().remove(values);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> removeHeaders(List<Header> values) {
		headerListBuilder().remove(values);
		return this;
	}

	/**
	 * Replaces the first occurrence of the header with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param value The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> updateHeader(Header value) {
		headerListBuilder().update(value);
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> updateHeaders(Header...values) {
		headerListBuilder().update(values);
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> updateHeaders(List<Header> values) {
		headerListBuilder().update(values);
		return this;
	}

	/**
	 * Sets all of the headers contained within this group overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the array.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> setHeaders(Header...values) {
		headerListBuilder().set(values);
		return this;
	}

	/**
	 * Sets all of the headers contained within this group overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the list.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> setHeaders(List<Header> values) {
		headerListBuilder().set(values);
		return this;
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> location(URI value) {
		updateHeader(Location.of(value));
		return this;
	}

	/**
	 * Specifies the value for the <c>Location</c> header.
	 *
	 * @param value The new header location.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResponseBuilder<T> location(String value) {
		updateHeader(Location.of(value));
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Body setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> body(String value) {
		try {
			body(new StringEntity(value));
		} catch (UnsupportedEncodingException e) { /* Not possible */ }
		return this;
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object (for method chaining).
	 */
	public HttpResponseBuilder<T> body(HttpEntity value) {
		this.body = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	private BasicStatusLineBuilder statusLineBuilder() {
		if (statusLineBuilder == null) {
			statusLineBuilder = statusLine == null ? BasicStatusLine.create() : statusLine.builder();
			statusLine = null;
		}
		return statusLineBuilder;
	}

	private HeaderListBuilder headerListBuilder() {
		if (headerListBuilder == null) {
			headerListBuilder = headerList == null ? HeaderList.create() : headerList.builder();
			headerList = null;
		}
		return headerListBuilder;
	}

	// <FluentSetters>

	// </FluentSetters>

}
