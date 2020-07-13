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
package org.apache.juneau.http;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.ContentType;

/**
 * An extension of an {@link HttpEntity} with support for arbitrary headers.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Default support for various streams and readers.
 * 	<li>
 * 		Content from {@link Supplier Suppliers}.
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * 	<li>
 * 		{@doc juneau-marshall.SimpleVariableLanguage.SvlVariables SVL variables}.
 * </ul>
 */
public class BasicHttpResource extends BasicHttpEntity implements HttpResource {

	private final List<Header> headers = AList.of();

	/**
	 * Creator.
	 *
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * </ul>
	 * @return A new empty {@link BasicHttpResource} object.
	 */
	public static BasicHttpResource of(Object content) {
		return new BasicHttpResource(content);
	}

	/**
	 * Creator.
	 *
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * </ul>
	 * @return A new empty {@link BasicHttpResource} object.
	 */
	public static BasicHttpResource of(Supplier<?> content) {
		return new BasicHttpResource(content);
	}

	/**
	 * Constructor.
	 *
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * </ul>
	 */
	public BasicHttpResource(Object content) {
		super(content);
	}

	/**
	 * Constructor.
	 *
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 * </ul>
	 * @param contentType
	 * 	The content type of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 * @param contentEncoding
	 * 	The content encoding of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public BasicHttpResource(Object content, ContentType contentType, ContentEncoding contentEncoding) {
		super(content, contentType, contentEncoding);
	}

	/**
	 * Adds an arbitrary header to this resource.
	 *
	 * @param name The header name.
	 * @param val The header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource header(String name, Object val) {
		if (name != null && val != null)
			headers.add(BasicHeader.of(name, val));
		return this;
	}

	/**
	 * Adds an arbitrary header to this resource.
	 *
	 * @param value The header.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource header(Header value) {
		if (value != null)
			headers.add(value);
		return this;
	}

	/**
	 * Adds an arbitrary collection of headers to this resource.
	 *
	 * @param headers The headers to add to this resource.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource headers(List<Header> headers) {
		this.headers.addAll(headers);
		return this;
	}

	/**
	 * Adds an arbitrary collection of headers to this resource.
	 *
	 * @param headers The headers to add to this resource.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource headers(Header...headers) {
		this.headers.addAll(Arrays.asList(headers));
		return this;
	}

	/**
	 * Returns the first header with the specified name as a string.
	 *
	 * @param name The header name.
	 * @return The header value or <jk>null</jk> if header was not found.
	 */
	public String getStringHeader(String name) {
		Header h = getLastHeader(name);
		return h == null ? null : h.getValue();
	}

	/**
	 * Returns the first header with the specified name as a string.
	 *
	 * @param name The header name.
	 * @return The header or <jk>null</jk> if header was not found.
	 */
	public Header getFirstHeader(String name) {
		for (Header h : headers)
			if (h.getName().equals(name))
				return h;
		return null;
	}

	/**
	 * Returns the last header with the specified name as a string.
	 *
	 * @param name The header name.
	 * @return The header or <jk>null</jk> if header was not found.
	 */
	public Header getLastHeader(String name) {
		for (ListIterator<Header> li = headers.listIterator(headers.size()); li.hasPrevious();) {
			Header h = li.previous();
			if (h.getName().equals(name))
				return h;
		}
		return null;
	}

	@Override /* Resource */
	public List<Header> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	// <FluentSetters>

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource cache() {
		super.cache();
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource cache(boolean value) {
		super.cache(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource chunked() {
		super.chunked();
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource chunked(boolean value) {
		super.chunked(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource contentEncoding(String value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource contentEncoding(Header value) {
		super.contentEncoding(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource contentLength(long value) {
		super.contentLength(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource contentType(String value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource contentType(Header value) {
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource resolving(VarResolver varResolver) {
		super.resolving(varResolver);
		return this;
	}

	@Override /* GENERATED - BasicHttpEntity */
	public BasicHttpResource resolving(VarResolverSession varSession) {
		super.resolving(varSession);
		return this;
	}

	// </FluentSetters>
}
