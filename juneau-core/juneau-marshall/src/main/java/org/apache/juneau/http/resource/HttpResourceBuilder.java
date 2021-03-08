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
package org.apache.juneau.http.resource;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link HttpEntity} beans.
 *
 * @param <T> The bean type to create for this builder.
 */
@FluentSetters(returns="HttpResourceBuilder<T>")
public class HttpResourceBuilder<T extends BasicResource> {

	HeaderList headers = HeaderList.EMPTY;
	HeaderListBuilder headersBuilder;

	BasicHttpEntity entity;
	HttpEntityBuilder<?> entityBuilder;

	/** The HttpEntity implementation class. */
	protected final Class<? extends BasicResource> implClass;

	/** The HttpEntity implementation class. */
	protected final Class<? extends BasicHttpEntity> entityImplClass;

	/**
	 * Constructor.
	 *
	 * @param implClass
	 * 	The subclass of {@link HttpResponse} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpResourceBuilder} object.
	 * @param entityImplClass
	 * 	The subclass of {@link BasicHttpEntity} to create.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpEntityBuilder} object.
	 */
	public HttpResourceBuilder(Class<T> implClass, Class<? extends BasicHttpEntity> entityImplClass) {
		this.implClass = implClass;
		this.entityImplClass = entityImplClass;
	}

	/**
	 * Copy constructor.
	 *
	 * @param impl
	 * 	The implementation object of {@link HttpEntity} to copy from.
	 * 	<br>This must contain a public constructor that takes in an {@link HttpResourceBuilder} object.
	 */
	public HttpResourceBuilder(T impl) {
		implClass = impl.getClass();
		headers = impl.headers;
		entity = impl.entity;
		this.entityImplClass = entity.getClass();
	}

	/**
	 * Instantiates the entity bean from the settings in this builder.
	 *
	 * @return A new {@link HttpEntity} bean.
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		try {
			return (T) implClass.getConstructor(HttpResourceBuilder.class).newInstance(this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	HeaderList headers() {
		if (headersBuilder != null)
			return headersBuilder.build();
		if (headers == null)
			return HeaderList.EMPTY;
		return headers;
	}

	BasicHttpEntity entity() {
		if (entityBuilder != null)
			return entityBuilder.build();
		if (entity == null)
			return BasicHttpEntity.EMPTY;
		return entity;
	}

	/**
	 * Copies the contents of the specified HTTP response to this builder.
	 *
	 * @param response The response to copy from.  Must not be null.
	 * @return This object (for method chaining).
	 * @throws IOException If content could not be retrieved.
	 */
	public HttpResourceBuilder<?> copyFrom(HttpResponse response) throws IOException {
		headers(response.getAllHeaders());
		content(response.getEntity().getContent());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HttpEntityBuilder setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> content(Object value) {
		entityBuilder().content(value);
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentSupplier(Supplier<?> value) {
		entityBuilder().contentSupplier(value);
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentType(String value) {
		entityBuilder().contentType(value);
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentType(ContentType value) {
		entityBuilder().contentType(value);
		return this;
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentLength(long value) {
		entityBuilder().contentLength(value);
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentEncoding(String value) {
		entityBuilder().contentEncoding(value);
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> contentEncoding(ContentEncoding value) {
		entityBuilder().contentEncoding(value);
		return this;
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <ul class='notes'>
	 * 	<li>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> chunked() {
		entityBuilder().chunked();
		return this;
	}

	/**
	 * Sets the 'chunked' flag value.
	 *
	 * <ul class='notes'>
	 * 	<li>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @param value The new value for this flag.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResourceBuilder<T> chunked(boolean value) {
		entityBuilder().chunked(value);
		return this;
	}

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If entity could not be read into memory.
	 */
	@FluentSetter
	public HttpResourceBuilder<T> cached() throws IOException {
		entityBuilder().cached();
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
	public HttpResourceBuilder<T> headers(HeaderList value) {
		headers = value;
		headersBuilder = null;
		return this;
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> clearHeaders() {
		headersBuilder().clear();
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> header(Header value) {
		if (value != null)
			headersBuilder().add(value);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * <p>
	 * This is a no-op if either the name or value is <jk>null</jk>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> header(String name, String value) {
		if (name != null && value != null)
			headersBuilder().add(name, value);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> headers and headers with <jk>null</jk> names or values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> headers(Header...values) {
		for (Header h : values) {
			if (h != null) {
				String n = h.getName();
				String v = h.getValue();
				if (isNotEmpty(n)) {
					if (n.equalsIgnoreCase("content-type"))
						contentType(v);
					else if (n.equalsIgnoreCase("content-encoding"))
						contentEncoding(v);
					else if (n.equalsIgnoreCase("content-length"))
						contentLength(Long.parseLong(v));
					else
						headersBuilder().add(h);
				}
			}
		}
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> headers(List<Header> values) {
		headersBuilder().add(values);
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> removeHeader(Header value) {
		headersBuilder().remove(value);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> removeHeaders(Header...values) {
		headersBuilder().remove(values);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HttpResourceBuilder<T> removeHeaders(List<Header> values) {
		headersBuilder().remove(values);
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
	public HttpResourceBuilder<T> updateHeader(Header value) {
		headersBuilder().update(value);
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
	public HttpResourceBuilder<T> updateHeaders(Header...values) {
		headersBuilder().update(values);
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
	public HttpResourceBuilder<T> updateHeaders(List<Header> values) {
		headersBuilder().update(values);
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
	public HttpResourceBuilder<T> setHeaders(Header...values) {
		headersBuilder().set(values);
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
	public HttpResourceBuilder<T> setHeaders(List<Header> values) {
		headersBuilder().set(values);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	private HeaderListBuilder headersBuilder() {
		if (headersBuilder == null) {
			headersBuilder = headers == null ? HeaderList.create() : headers.copy();
			headers = null;
		}
		return headersBuilder;
	}

	private HttpEntityBuilder<?> entityBuilder() {
		if (entityBuilder == null) {
			entityBuilder = entity == null ? BasicHttpEntity.create(entityImplClass) : entity.copy();
			entity = null;
		}
		return entityBuilder;
	}

	// <FluentSetters>

	// </FluentSetters>
}
