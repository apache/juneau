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

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;

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
 */
@BeanIgnore
public class BasicResource implements HttpResource {

	final BasicHttpEntity entity;
	final HeaderList headers;

	/**
	 * Creates a builder for this class.
	 *
	 * @param implClass The subclass that the builder is going to create.
	 * @param entityImplClass The entity subclass that the builder is going to create.
	 * @return A new builder bean.
	 */
	public static <T extends BasicResource> HttpResourceBuilder<T> create(Class<T> implClass, Class<? extends BasicHttpEntity> entityImplClass) {
		return new HttpResourceBuilder<>(implClass, entityImplClass);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the arguments for this bean.
	 */
	public BasicResource(HttpResourceBuilder<?> builder) {
		this.entity = builder.entity();
		this.headers = builder.headers();
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
		this(create(null, InputStreamEntity.class).copyFrom(response));
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	public HttpResourceBuilder<? extends BasicResource> copy() {
		return new HttpResourceBuilder<>(this);
	}

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
	// Header convenience methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Tests if headers with the given name are contained within this resource (excluding those set on the entity itself).
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if at least one header with the name is present.
	 */
	public boolean containsHeader(String name) {
		return headers.contains(name);
	}

	/**
	 * Gets all of the headers with the given name (excluding those set on the entity itself).
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 *
	 * @return An array containing all matching headers, or an empty array if none are found.
	 */
	public List<Header> getHeaders(String name) {
		return headers.get(name);
	}

	/**
	 * Gets the first header with the given name (excluding those set on the entity itself).
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The first matching header, or <jk>null</jk> if not found.
	 */
	public Header getFirstHeader(String name) {
		return headers.getFirst(name);
	}

	/**
	 * Gets the last header with the given name (excluding those set on the entity itself).
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The last matching header, or <jk>null</jk> if not found.
	 */
	public Header getLastHeader(String name) {
		return headers.getLast(name);
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
	public List<Header> getHeaders() {
		return headers.getAll();
	}
}
