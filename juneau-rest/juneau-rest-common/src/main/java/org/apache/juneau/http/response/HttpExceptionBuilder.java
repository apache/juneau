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

import static org.apache.juneau.http.HttpEntities.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link BasicHttpException} beans.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="HttpExceptionBuilder<T>")
public class HttpExceptionBuilder<T extends BasicHttpException> extends BasicRuntimeException.Builder {

	BasicStatusLine statusLine;
	HeaderList headers = HeaderList.create();
	BasicStatusLine.Builder statusLineBuilder;
	private int TODO;
	HttpEntity content;

	private final Class<? extends BasicHttpException> implClass;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link BasicHttpException} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpExceptionBuilder} object.
	 */
	public HttpExceptionBuilder(Class<T> implClass) {
		this.implClass = implClass;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public HttpExceptionBuilder(T copyFrom) {
		super(copyFrom);
		implClass = copyFrom.getClass();
		statusLine = copyFrom.statusLine;
		headers = copyFrom.headers;
		content = copyFrom.content;
	}

	/**
	 * Instantiates the exception bean from the settings in this builder.
	 *
	 * @return A new {@link BasicHttpException} bean.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(HttpExceptionBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	/**
	 * Copies the contents of the specified HTTP response to this builder.
	 *
	 * @param response The response to copy from.  Must not be null.
	 * @return This object.
	 */
	public HttpExceptionBuilder<?> copyFrom(HttpResponse response) {
		Header h = response.getLastHeader("Thrown");
		if (h != null)
			message(thrown(h.getValue()).asParts().get().get(0).getMessage());
		headers(response.getAllHeaders());
		content(response.getEntity());
		return this;
	}

	BasicStatusLine buildStatusLine() {
		if (statusLineBuilder != null)
			return statusLineBuilder.build();
		return statusLine;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicStatusLine setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns access to the underlying builder for the status line.
	 *
	 * @return The underlying builder for the status line.
	 */
	public BasicStatusLine.Builder getStatusLine() {
		if (statusLineBuilder == null) {
			statusLineBuilder = statusLine == null ? BasicStatusLine.create() : statusLine.copy();
			statusLine = null;
		}
		return statusLineBuilder;
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
	public HttpExceptionBuilder<T> statusLine(BasicStatusLine value) {
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
	 * @return This object.
	 */
	@FluentSetter
	public HttpExceptionBuilder<T> protocolVersion(ProtocolVersion value) {
		getStatusLine().protocolVersion(value);
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
	public HttpExceptionBuilder<T> statusCode(int value) {
		getStatusLine().statusCode(value);
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
	public HttpExceptionBuilder<T> reasonPhrase(String value) {
		getStatusLine().reasonPhrase(value);
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
	public HttpExceptionBuilder<T> reasonPhraseCatalog(ReasonPhraseCatalog value) {
		getStatusLine().reasonPhraseCatalog(value);
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
	public HttpExceptionBuilder<T> locale(Locale value) {
		getStatusLine().locale(value);
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
	public HttpExceptionBuilder<T> headers(HeaderList value) {
		headers = value;
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public HttpExceptionBuilder<T> header(Header value) {
		headers.append(value);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HttpExceptionBuilder<T> header(String name, String value) {
		headers.append(name, value);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public HttpExceptionBuilder<T> headers(Header...values) {
		headers.append(values);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public HttpExceptionBuilder<T> headers(List<Header> values) {
		headers.append(values);
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
	public HttpExceptionBuilder<T> content(String value) {
		content(stringEntity(value).build());
		return this;
	}

	/**
	 * Sets the body on this response.
	 *
	 * @param value The body on this response.
	 * @return This object.
	 */
	public HttpExceptionBuilder<T> content(HttpEntity value) {
		this.content = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.BasicRuntimeException.Builder */
	public HttpExceptionBuilder<T> causedBy(Throwable value) {
		super.causedBy(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.BasicRuntimeException.Builder */
	public HttpExceptionBuilder<T> message(String msg, Object...args) {
		super.message(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.BasicRuntimeException.Builder */
	public HttpExceptionBuilder<T> unmodifiable() {
		super.unmodifiable();
		return this;
	}

	// </FluentSetters>

}
