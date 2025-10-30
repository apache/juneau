/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.entity;

import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;

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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
@BeanIgnore
@SuppressWarnings("resource")
public class BasicHttpEntity implements HttpEntity {
	/**
	 * An empty HttpEntity.
	 */
	public static final BasicHttpEntity EMPTY = new BasicHttpEntity().setUnmodifiable();
	private boolean cached, chunked, unmodifiable;
	private Object content;
	private Supplier<?> contentSupplier;
	private ContentType contentType;
	private ContentEncoding contentEncoding;
	private Charset charset;
	private long contentLength = -1;
	private int maxLength = -1;

	/**
	 * Constructor.
	 */
	public BasicHttpEntity() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	public BasicHttpEntity(BasicHttpEntity copyFrom) {
		this.cached = copyFrom.cached;
		this.chunked = copyFrom.chunked;
		this.content = copyFrom.content;
		this.contentSupplier = copyFrom.contentSupplier;
		this.contentType = copyFrom.contentType;
		this.contentEncoding = copyFrom.contentEncoding;
		this.contentLength = copyFrom.contentLength;
		this.charset = copyFrom.charset;
		this.maxLength = copyFrom.maxLength;
		this.unmodifiable = false;
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity content.
	 */
	public BasicHttpEntity(ContentType contentType, Object content) {
		this.contentType = contentType;
		this.content = content;
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
	public FluentByteArrayAssertion<BasicHttpEntity> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
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

	@Override /* Overridden from HttpEntity */
	public void consumeContent() throws IOException {}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	public BasicHttpEntity copy() {
		return new BasicHttpEntity(this);
	}

	/**
	 * Returns the charset to use when converting to and from stream-based resources.
	 *
	 * @return The charset to use when converting to and from stream-based resources.
	 */
	public Charset getCharset() { return charset == null ? UTF8 : charset; }

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException, UnsupportedOperationException { return EMPTY_INPUT_STREAM; }

	@Override /* Overridden from HttpEntity */
	public Header getContentEncoding() { return contentEncoding; }

	@Override /* Overridden from HttpEntity */
	public long getContentLength() { return contentLength; }

	@Override /* Overridden from HttpEntity */
	public Header getContentType() { return contentType; }

	/**
	 * Returns the maximum number of bytes to read or write to and from stream-based resources.
	 *
	 * @return The maximum number of bytes to read or write to and from stream-based resources.
	 */
	public int getMaxLength() { return maxLength; }

	/**
	 * Returns <jk>true</jk> if this entity is cached in-memory.
	 *
	 * @return <jk>true</jk> if this entity is cached in-memory.
	 */
	public boolean isCached() { return cached; }

	@Override /* Overridden from HttpEntity */
	public boolean isChunked() { return chunked; }

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return false; }

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() { return false; }

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is unmodifiable.
	 */
	public boolean isUnmodifiable() { return unmodifiable; }

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object.
	 * @throws IOException If entity could not be read into memory.
	 */
	public BasicHttpEntity setCached() throws IOException {
		assertModifiable();
		cached = true;
		return this;
	}

	/**
	 * Specifies the charset to use when converting to and from stream-based resources.
	 *
	 * @param value The new value.  If <jk>null</jk>, <c>UTF-8</c> is assumed.
	 * @return This object.
	 */
	public BasicHttpEntity setCharset(Charset value) {
		assertModifiable();
		this.charset = value;
		return this;
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object.
	 */
	public BasicHttpEntity setChunked() {
		return setChunked(true);
	}

	/**
	 * Sets the 'chunked' flag value.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @param value The new value for this flag.
	 * @return This object.
	 */
	public BasicHttpEntity setChunked(boolean value) {
		assertModifiable();
		chunked = value;
		return this;
	}

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	public BasicHttpEntity setContent(Object value) {
		assertModifiable();
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
	public BasicHttpEntity setContent(Supplier<?> value) {
		assertModifiable();
		this.contentSupplier = value == null ? () -> null : value;
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BasicHttpEntity setContentEncoding(ContentEncoding value) {
		assertModifiable();
		contentEncoding = value;
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BasicHttpEntity setContentEncoding(String value) {
		return setContentEncoding(ContentEncoding.of(value));
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object.
	 */
	public BasicHttpEntity setContentLength(long value) {
		assertModifiable();
		contentLength = value;
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BasicHttpEntity setContentType(ContentType value) {
		assertModifiable();
		contentType = value;
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public BasicHttpEntity setContentType(String value) {
		return setContentType(ContentType.of(value));
	}

	/**
	 * Specifies the maximum number of bytes to read or write to and from stream-based resources.
	 *
	 * <p>
	 * Implementation is not universal.
	 *
	 * @param value The new value.  The default is <c>-1</c> which means read everything.
	 * @return This object.
	 */
	public BasicHttpEntity setMaxLength(int value) {
		assertModifiable();
		this.maxLength = value;
		return this;
	}

	/**
	 * Specifies whether this bean should be unmodifiable.
	 * <p>
	 * When enabled, attempting to set any properties on this bean will cause an {@link UnsupportedOperationException}.
	 *
	 * @return This object.
	 */
	public BasicHttpEntity setUnmodifiable() {
		unmodifiable = true;
		return this;
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream outStream) throws IOException {}

	/**
	 * Same as {@link #asBytes()} but wraps {@link IOException IOExceptions} inside a {@link RuntimeException}.
	 *
	 * @return The contents of this entity as a byte array.
	 */
	protected byte[] asSafeBytes() {
		try {
			return asBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Throws an {@link UnsupportedOperationException} if the unmodifiable flag is set on this bean.
	 */
	protected final void assertModifiable() {
		if (unmodifiable)
			throw unsupportedOp("Bean is read-only");
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
		if (o == null && nn(contentSupplier))
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
	protected boolean isSupplied() { return nn(contentSupplier); }
}