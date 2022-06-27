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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A basic {@link org.apache.http.HttpEntity} implementation with additional features.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * 	<li>
 * 		Externally-supplied/dynamic content.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-common}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@BeanIgnore
public class BasicHttpEntity implements HttpEntity {

	/**
	 * An empty HttpEntity.
	 */
	public static final BasicHttpEntity EMPTY = create(BasicHttpEntity.class).build();

	final boolean cached, chunked;
	final Object content;
	final Supplier<?> contentSupplier;
	final ContentType contentType;
	final ContentEncoding contentEncoding;
	final Charset charset;
	final long contentLength;

	/**
	 * Creates a builder for this class.
	 *
	 * @param <T> The subclass that the builder is going to create.
	 * @param implClass The subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public static <T extends BasicHttpEntity> HttpEntityBuilder<T> create(Class<T> implClass) {
		return new HttpEntityBuilder<>(implClass);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the arguments for this bean.
	 */
	public BasicHttpEntity(HttpEntityBuilder<?> builder) {
		this.cached = builder.cached;
		this.chunked = builder.chunked;
		this.content = builder.content;
		this.contentSupplier = builder.contentSupplier;
		this.contentType = builder.contentType;
		this.contentEncoding = builder.contentEncoding;
		this.charset = builder.charset;
		this.contentLength = builder.contentLength;
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpEntityBuilder<? extends BasicHttpEntity> copy() {
		return new HttpEntityBuilder<>(this);
	}

	/**
	 * Converts the contents of this entity as a string.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a string.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public String asString() throws IOException {
		return read(getContent());
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public byte[] asBytes() throws IOException {
		return readBytes(getContent());
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentStringAssertion<BasicHttpEntity> assertString() throws IOException {
		return new FluentStringAssertion<>(asString(), this);
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<BasicHttpEntity> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	/**
	 * Returns the content of this entity.
	 *
	 * @param <T> The value type.
	 * @param def The default value if <jk>null</jk>.
	 * @return The content object.
	 */
	@SuppressWarnings("unchecked")
	protected <T> T contentOrElse(T def) {
		Object o = content;
		if (o == null && contentSupplier != null)
			o = contentSupplier.get();
		return (o == null ? def : (T)o);
	}

	/**
	 * Returns <jk>true</jk> if the contents of this entity are provided through an external supplier.
	 *
	 * <p>
	 * Externally supplied content generally means the content length cannot be reliably determined
	 * based on the content.
	 *
	 * @return <jk>true</jk> if the contents of this entity are provided through an external supplier.
	 */
	protected boolean isSupplied() {
		return contentSupplier != null;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return contentLength;
	}

	@Override
	public boolean isRepeatable() {
		return false;
	}

	@Override
	public boolean isChunked() {
		return chunked;
	}

	@Override
	public Header getContentType() {
		return contentType;
	}

	@Override
	public Header getContentEncoding() {
		return contentEncoding;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public void consumeContent() throws IOException {}

	@Override
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		return IOUtils.EMPTY_INPUT_STREAM;
	}

	@Override
	public void writeTo(OutputStream outStream) throws IOException {}

	// <FluentSetters>

	// </FluentSetters>
}
