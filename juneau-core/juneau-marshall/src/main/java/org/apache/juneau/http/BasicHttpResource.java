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

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.ContentType;

/**
 * An abstract subclass of all {@link HttpResource} objects.
 */
public class BasicHttpResource extends AbstractHttpEntity implements HttpResource  {

	private final List<Header> headers = AList.of();
	private Object content;
	private boolean cache;

	/**
	 * Creator.
	 *
	 * @return A new empty {@link BasicHttpResource} object.
	 */
	public static BasicHttpResource create() {
		return new BasicHttpResource();
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
	 * 	</ul>
	 * </ul>
	 * @return A new empty {@link ReaderResource} object.
	 */
	public static BasicHttpResource of(Object content) {
		return new BasicHttpResource().content(content);
	}

	/**
	 * Constructor.
	 */
	public BasicHttpResource() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param contentType
	 * 	The content type of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 * @param contentEncoding
	 * 	The content encoding of the contents.
	 * 	<br>Can be <jk>null</jk>.
	 * @param content
	 * 	The content.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 	</ul>
	 * </ul>
	 */
	public BasicHttpResource(ContentType contentType, ContentEncoding contentEncoding, Object content) {
		setContentType(contentType);
		setContentEncoding(contentEncoding);
		this.content = content;
	}

	/**
	 * Returns the raw content of this resource.
	 *
	 * @return The raw content of this resource.
	 */
	protected Object getRawContent() {
		return content;
	}

	@Override
	@FluentSetter
	public BasicHttpResource chunked() {
		super.setChunked(true);
		return this;
	}

	@Override
	@FluentSetter
	public BasicHttpResource contentType(Header value) {
		super.setContentType(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #contentType(String)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource contentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override
	@FluentSetter
	public BasicHttpResource contentEncoding(Header value) {
		super.setContentEncoding(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentEncoding(String)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpResource contentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
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

	@Override
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

	@Override
	@FluentSetter
	public BasicHttpResource content(Object value) {
		this.content = value;
		return this;
	}

	@Override
	@FluentSetter
	public BasicHttpResource cache() {
		this.cache = true;
		return this;
	}

	@Override /* Resource */
	public List<Header> getHeaders() {
		return Collections.unmodifiableList(headers);
	}

	@Override
	public boolean isRepeatable() {
		return cache || content instanceof File || content instanceof CharSequence || content instanceof byte[];
	}

	@Override
	public long getContentLength() {
		try {
			tryCache();
		} catch (IOException e) {}
		if (content instanceof byte[])
			return ((byte[])content).length;
		if (content instanceof File)
			return ((File)content).length();
		if (content instanceof CharSequence)
			return ((CharSequence)content).length();
		return -1;
	}

	@Override
	public InputStream getContent() throws IOException, UnsupportedOperationException {
		tryCache();
		if (content == null)
			return null;
		if (content instanceof File)
			return new FileInputStream((File)content);
		if (content instanceof byte[])
			return new ByteArrayInputStream((byte[])content);
		if (content instanceof Reader)
			return new ReaderInputStream((Reader)content, IOUtils.UTF8);
		if (content instanceof InputStream)
			return (InputStream)content;
		return new ReaderInputStream(new StringReader(content.toString()),IOUtils.UTF8);
	}

	@Override
	public void writeTo(OutputStream os) throws IOException {
		if (content != null)
			IOUtils.pipe(content, os);
		os.flush();
	}

	@Override
	public boolean isStreaming() {
		return (content instanceof InputStream || content instanceof Reader);
	}

	private void tryCache() throws IOException {
		if (cache)
			if (content instanceof File || content instanceof InputStream || content instanceof Reader)
				content = IOUtils.readBytes(content);
	}

	// <FluentSetters>

	// </FluentSetters>
}
