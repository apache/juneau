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
package org.apache.juneau.http.entity;

import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link HttpEntity} beans.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="HttpEntityBuilder<T>")
public class HttpEntityBuilder<T extends BasicHttpEntity> {

	boolean cached, chunked;
	Object content;
	Supplier<?> contentSupplier;
	ContentType contentType;
	ContentEncoding contentEncoding;
	Charset charset;
	long contentLength = -1;

	/** The HttpEntity implementation class. */
	protected final Class<? extends BasicHttpEntity> implClass;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link HttpResponse} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpEntityBuilder} object.
	 */
	public HttpEntityBuilder(Class<T> implClass) {
		this.implClass = implClass;
	}

	/**
	 * Copy constructor.
	 *
	 * @param impl
	 * 	The implementation object of {@link HttpEntity} to copy from.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpEntityBuilder} object.
	 */
	public HttpEntityBuilder(T impl) {
		implClass = impl.getClass();
		cached = impl.cached;
		content = impl.content;
		contentSupplier = impl.contentSupplier;
		contentType = impl.contentType;
		contentEncoding = impl.contentEncoding;
		charset = impl.charset;
		contentLength = impl.contentLength;
	}

	/**
	 * Instantiates the entity bean from the settings in this builder.
	 *
	 * @return A new {@link HttpEntity} bean.
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(HttpEntityBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> content(Object value) {
		this.content = value;
		return this;
	}

	/**
	 * Sets the content on this entity bean from a supplier.
	 *
	 * <p>
	 * Repeatable entities such as {@link StringEntity} use this to allow the entity content to be resolved at
	 * serialization time.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> content(Supplier<?> value) {
		this.contentSupplier = value == null ? ()->null : value;
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> contentType(String value) {
		return contentType(ContentType.of(value));
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> contentType(ContentType value) {
		contentType = value;
		return this;
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> contentLength(long value) {
		contentLength = value;
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> contentEncoding(String value) {
		return contentEncoding(ContentEncoding.of(value));
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> contentEncoding(ContentEncoding value) {
		contentEncoding = value;
		return this;
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> chunked() {
		return chunked(true);
	}

	/**
	 * Sets the 'chunked' flag value.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @param value The new value for this flag.
	 * @return This object.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> chunked(boolean value) {
		chunked = value;
		return this;
	}

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object.
	 * @throws IOException If entity could not be read into memory.
	 */
	@FluentSetter
	public HttpEntityBuilder<T> cached() throws IOException {
		cached = true;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
