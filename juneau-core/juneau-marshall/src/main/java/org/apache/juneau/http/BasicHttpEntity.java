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
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.internal.*;

/**
 * An extension of {@link org.apache.http.entity.BasicHttpEntity} with fluent setters.
 * 
 * <p>
 * Includes automatic support for a variety of content types.
 */
public class BasicHttpEntity extends org.apache.http.entity.BasicHttpEntity {
	private Object content;
	private boolean cache;

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
	 * @return A new empty {@link ReaderResource} object.
	 */
	public static BasicHttpEntity of(Object content) {
		return new BasicHttpEntity().content(content);
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
	 * @return A new empty {@link ReaderResource} object.
	 */
	public static BasicHttpEntity of(Supplier<?> content) {
		return new BasicHttpEntity().content(content);
	}

	/**
	 * Creates a new basic entity.
	 * The content is initially missing, the content length
	 * is set to a negative number.
	 */
	public BasicHttpEntity() {
		super();
	}

	/**
	 * Sets the content on this entity.
	 *
	 * @param value
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity content(Object value) {
		this.content = value;
		return this;
	}

	/**
	 * Sets the content on this entity.
	 *
	 * @param value
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
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity content(Supplier<?> value) {
		this.content = value;
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentType(String)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity contentType(String value) {
		super.setContentType(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentType(Header)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity contentType(Header value) {
		super.setContentType(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentLength(long)}.
	 *
	 * @param value The new <c>Content-Length</c> header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity contentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentEncoding(String)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity contentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setContentEncoding(Header)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity contentEncoding(Header value) {
		super.setContentEncoding(value);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setChunked(boolean)} with <jk>true</jk>.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity chunked() {
		super.setChunked(true);
		return this;
	}

	/**
	 * Shortcut for calling {@link #setChunked(boolean)}.
	 *
	 * @param value The new value for this flag.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity chunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity cache() {
		return cache(true);
	}

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @param value The new value for this flag.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public BasicHttpEntity cache(boolean value) {
		this.cache = value;
		return this;
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
	public InputStream getContent() {
		try {
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeTo(OutputStream os) throws IOException {
		tryCache();
		if (content != null)
			IOUtils.pipe(content, os);
		os.flush();
	}

	@Override
	public boolean isStreaming() {
		return (content instanceof InputStream || content instanceof Reader);
	}

	/**
	 * Returns the raw content of this resource.
	 *
	 * @return The raw content of this resource.
	 */
	protected Object getRawContent() {
		return unwrap(content);
	}

	private void tryCache() throws IOException {
		if (cache && isCacheable(content))
			content = readBytes(content);
	}

	/**
	 * Returns <jk>true</jk> if the specified object is cachable as a byte array.
	 *
	 * <p>
	 * The default implementation returns <jk>true</jk> for the following types:
	 * <ul>
	 * 	<li>{@link File}
	 * 	<li>{@link InputStream}
	 * 	<li>{@link Reader}
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the specified object is cachable as a byte array.
	 */
	protected boolean isCacheable(Object o) {
		return (o instanceof File || o instanceof InputStream || o instanceof Reader);
	}

	/**
	 * Reads the contents of the specified object as a byte array.
	 *
	 * @param o The object to read.
	 * @return The byte array contents.
	 * @throws IOException If object could not be read.
	 */
	protected byte[] readBytes(Object o) throws IOException {
		return IOUtils.readBytes(o);
	}

	/**
	 * If the specified object is a {@link Supplier}, returns the supplied value, otherwise the same value.
	 *
	 * @param o The object to unwrap.
	 * @return The unwrapped object.
	 */
	protected Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	// <FluentSetters>

	// </FluentSetters>
}
