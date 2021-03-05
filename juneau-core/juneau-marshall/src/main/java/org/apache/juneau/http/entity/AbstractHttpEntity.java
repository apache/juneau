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

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.ContentType;
import org.apache.juneau.internal.*;

/**
 * An extension of {@link org.apache.http.entity.AbstractHttpEntity} with additional features.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * </ul>
 */
@FluentSetters
@BeanIgnore
public abstract class AbstractHttpEntity extends org.apache.http.entity.AbstractHttpEntity {

	private long length = -1;

	/**
	 * Shortcut for calling {@link #setContentType(String)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity contentType(String value) {
		return contentType(ContentType.of(value));
	}

	/**
	 * Shortcut for calling {@link #setContentType(Header)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity contentType(Header value) {
		super.setContentType(value);
		return this;
	}

	/**
	 * Sets the content length of this entity.
	 *
	 * @param value The new <c>Content-Length</c> header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity contentLength(long value) {
		length = value;
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentEncoding(String)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity contentEncoding(String value) {
		return contentEncoding(ContentEncoding.of(value));
	}

	/**
	 * Shortcut for calling {@link #setContentEncoding(Header)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity contentEncoding(Header value) {
		super.setContentEncoding(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setChunked(boolean)} with <jk>true</jk>.
	 *
	 * <ul class='notes'>
	 * 	<li>If the {@link #getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity chunked() {
		return chunked(true);
	}

	/**
	 * Shortcut for calling {@link #setChunked(boolean)}.
	 *
	 * <ul class='notes'>
	 * 	<li>If the {@link #getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @param value The new value for this flag.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public AbstractHttpEntity chunked(boolean value) {
		super.setChunked(value);
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
	public AbstractHttpEntity cache() throws IOException {
		return this;
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public String asString() throws IOException {
		return read(getContent());
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public byte[] asBytes() throws IOException {
		return readBytes(getContent());
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentStringAssertion<AbstractHttpEntity> assertString() throws IOException {
		return new FluentStringAssertion<>(asString(), this);
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<AbstractHttpEntity> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return length;
	}

	// <FluentSetters>

	// </FluentSetters>
}
