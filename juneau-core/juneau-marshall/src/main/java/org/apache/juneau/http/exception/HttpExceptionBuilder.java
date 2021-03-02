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

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.impl.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link HttpException} classes.
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="HttpExceptionBuilder<T>")
public class HttpExceptionBuilder<T extends HttpException> extends BasicRuntimeExceptionBuilder {

	BasicStatusLineBuilder statusLine = BasicStatusLine.create();
	BasicHeaderGroupBuilder headers = BasicHeaderGroup.create();
	HttpEntity body;
	private final Class<? extends HttpException> implClass;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link HttpException} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpExceptionBuilder} object.
	 */
	public HttpExceptionBuilder(Class<T> implClass) {
		this.implClass = implClass;
	}

	/**
	 * Instantiates the exception bean from the settings in this builder.
	 *
	 * @return A new {@link HttpException} bean.
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(HttpExceptionBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	public HttpExceptionBuilder<T> protocolVersion(ProtocolVersion value) {
		statusLine.protocolVersion(value);
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
	public HttpExceptionBuilder<T> statusCode(int value) {
		statusLine.statusCode(value);
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
	public HttpExceptionBuilder<T> reasonPhrase(String value) {
		statusLine.reasonPhrase(value);
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
	public HttpExceptionBuilder<T> reasonPhraseCatalog(ReasonPhraseCatalog value) {
		statusLine.reasonPhraseCatalog(value);
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
	public HttpExceptionBuilder<T> locale(Locale value) {
		statusLine.locale(value);
		return this;
	}

	/**
	 * Copies the values from the specified exception.
	 *
	 * @param value The exception to copy from.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> copyFrom(HttpException value) {
		super.copyFrom(value);
		statusLine = value.statusLine.builder();
		headers = value.headers.builder();
		body = value.body;
		return this;
	}

	/**
	 * Sets the status line builder for this builder.
	 *
	 * @param value The new status line builder.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpExceptionBuilder<T> statusLineBuilder(BasicStatusLineBuilder value) {
		statusLine = value;
		return this;
	}

	/**
	 * Sets the header group builder for this builder.
	 *
	 * @param value The new header group builder.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpExceptionBuilder<T> headerGroupBuilder(BasicHeaderGroupBuilder value) {
		headers = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderGroup setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> clearHeaders() {
		headers.clear();
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> header(Header value) {
		headers.add(value);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> header(String name, String value) {
		headers.add(name, value);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> headers(Header...values) {
		headers.add(values);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> headers(List<Header> values) {
		headers.add(values);
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> removeHeader(Header value) {
		headers.remove(value);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> removeHeaders(Header...values) {
		headers.remove(values);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpExceptionBuilder<T> removeHeaders(List<Header> values) {
		headers.remove(values);
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
	public HttpExceptionBuilder<T> updateHeader(Header value) {
		headers.update(value);
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
	public HttpExceptionBuilder<T> updateHeaders(Header...values) {
		headers.update(values);
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
	public HttpExceptionBuilder<T> updateHeaders(List<Header> values) {
		headers.update(values);
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
	public HttpExceptionBuilder<T> setHeaders(Header...values) {
		headers.set(values);
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
	public HttpExceptionBuilder<T> setHeaders(List<Header> values) {
		headers.set(values);
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
	public HttpExceptionBuilder<T> body(String value) {
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
	public HttpExceptionBuilder<T> body(HttpEntity value) {
		this.body = value;
		return this;
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicRuntimeExceptionBuilder */
	public HttpExceptionBuilder<T> causedBy(Throwable value) {
		super.causedBy(value);
		return this;
	}

	@Override /* GENERATED - BasicRuntimeExceptionBuilder */
	public HttpExceptionBuilder<T> copyFrom(BasicRuntimeException value) {
		super.copyFrom(value);
		return this;
	}

	@Override /* GENERATED - BasicRuntimeExceptionBuilder */
	public HttpExceptionBuilder<T> message(String msg, Object...args) {
		super.message(msg, args);
		return this;
	}

	@Override /* GENERATED - BasicRuntimeExceptionBuilder */
	public HttpExceptionBuilder<T> unmodifiable(boolean value) {
		super.unmodifiable(value);
		return this;
	}

	// </FluentSetters>

}
