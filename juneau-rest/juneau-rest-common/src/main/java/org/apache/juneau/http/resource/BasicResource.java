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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A basic {@link org.apache.juneau.http.resource.HttpResource} implementation with additional features.
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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@BeanIgnore  /* Use toString() to serialize */
@FluentSetters
public class BasicResource implements HttpResource {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	BasicHttpEntity entity;
	HeaderList headers = HeaderList.create();
	boolean unmodifiable;

	/**
	 * Constructor.
	 *
	 * @param entity The entity that makes up this resource content.
	 */
	public BasicResource(BasicHttpEntity entity) {
		this.entity = entity;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean bean copied.
	 */
	public BasicResource(BasicResource copyFrom) {
		this.entity = copyFrom.entity.copy();
		this.headers = copyFrom.headers.copy();
	}


	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when parsing an HTTP response.
	 *
	 * @param response The HTTP response to copy from.  Must not be <jk>null</jk>.
	 * @throws IOException Rethrown from {@link HttpEntity#getContent()}.
	 */
	public BasicResource(HttpResponse response) throws IOException {
		this(new StreamEntity());
		copyFrom(response);
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	public BasicResource copy() {
		return new BasicResource(this);
	}

	/**
	 * Copies the contents of the specified HTTP response to this builder.
	 *
	 * @param response The response to copy from.  Must not be null.
	 * @return This object.
	 * @throws IOException If content could not be retrieved.
	 */
	public BasicResource copyFrom(HttpResponse response) throws IOException {
		addHeaders(response.getAllHeaders());
		setContent(response.getEntity().getContent());
		return this;
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
	public BasicResource setUnmodifiable() {
		unmodifiable = true;
		entity.setUnmodifiable();
		headers.setUnmodifiable();
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

	/**
	 * Returns access to the underlying builder for the HTTP entity.
	 *
	 * @return The underlying builder for the HTTP entity.
	 */
	public HttpEntity getEntity() {
		return entity;
	}

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContent(Object value) {
		entity.setContent(value);
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
	public BasicResource setContent(Supplier<?> value) {
		entity.setContent(value);
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContentType(String value) {
		entity.setContentType(value);
		return this;
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContentType(ContentType value) {
		entity.setContentType(value);
		return this;
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContentLength(long value) {
		entity.setContentLength(value);
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContentEncoding(String value) {
		entity.setContentEncoding(value);
		return this;
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setContentEncoding(ContentEncoding value) {
		entity.setContentEncoding(value);
		return this;
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setChunked() {
		entity.setChunked();
		return this;
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
	@FluentSetter
	public BasicResource setChunked(boolean value) {
		entity.setChunked(value);
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
	public BasicResource setCached() throws IOException {
		entity.setCached();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BasicHeaderGroup setters.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the specified header in this builder.
	 *
	 * <p>
	 * This is a no-op if either the name or value is <jk>null</jk>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public BasicResource setHeader(String name, String value) {
		if (name != null && value != null)
			headers.set(name, value);
		return this;
	}

	/**
	 * Appends the specified header to the end of the headers in this builder.
	 *
	 * <p>
	 * This is a no-op if either the name or value is <jk>null</jk>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public BasicResource addHeader(String name, String value) {
		if (name != null && value != null)
			headers.append(name, value);
		return this;
	}

	/**
	 * Sets the specified headers in this builder.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	@FluentSetter
	public BasicResource setHeaders(HeaderList value) {
		headers = value.copy();
		return this;
	}

	/**
	 * Sets the specified headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public BasicResource setHeaders(Header...values) {
		for (Header h : values) {
			if (h != null) {
				String n = h.getName();
				String v = h.getValue();
				if (isNotEmpty(n)) {
					if (n.equalsIgnoreCase("content-type"))
						setContentType(v);
					else if (n.equalsIgnoreCase("content-encoding"))
						setContentEncoding(v);
					else if (n.equalsIgnoreCase("content-length"))
						setContentLength(Long.parseLong(v));
					else
						headers.set(h);
				}
			}
		}
		return this;
	}

	/**
	 * Appends the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to set.  <jk>null</jk> headers and headers with <jk>null</jk> names or values are ignored.
	 * @return This object.
	 */
	public BasicResource addHeaders(Header...values) {
		for (Header h : values) {
			if (h != null) {
				String n = h.getName();
				String v = h.getValue();
				if (isNotEmpty(n)) {
					if (n.equalsIgnoreCase("content-type"))
						setContentType(v);
					else if (n.equalsIgnoreCase("content-encoding"))
						setContentEncoding(v);
					else if (n.equalsIgnoreCase("content-length"))
						setContentLength(Long.parseLong(v));
					else
						headers.append(h);
				}
			}
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Converts the contents of the entity of this resource as a string.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a string.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public String asString() throws IOException {
		return entity.asString();
	}

	/**
	 * Converts the contents of the entity of this resource as a byte array.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public byte[] asBytes() throws IOException {
		return entity.asBytes();
	}

	/**
	 * Returns an assertion on the contents of the entity of this resource.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentStringAssertion<BasicResource> assertString() throws IOException {
		return new FluentStringAssertion<>(asString(), this);
	}

	/**
	 * Returns an assertion on the contents of the entity of this resource.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<BasicResource> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path-through methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* HttpEntity */
	public long getContentLength() {
		return entity.getContentLength();
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return entity.isRepeatable();
	}

	@Override /* HttpEntity */
	public boolean isChunked() {
		return entity.isChunked();
	}

	@Override /* HttpEntity */
	public Header getContentType() {
		return entity.getContentType();
	}

	@Override /* HttpEntity */
	public Header getContentEncoding() {
		return entity.getContentEncoding();
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return entity.isStreaming();
	}

	@Override
	public void consumeContent() throws IOException {}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		return entity.getContent();
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream outStream) throws IOException {
		entity.writeTo(outStream);
	}

	@Override /* HttpResource */
	public HeaderList getHeaders() {
		return headers;
	}

	// <FluentSetters>

	// </FluentSetters>
}
