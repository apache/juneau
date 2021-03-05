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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link Reader}.
 */
public class ReaderEntity extends AbstractHttpEntity {

	private final Reader content;
	private final long length;
	private Charset charset = UTF8;
	private final AtomicReference<String> cache = new AtomicReference<>();

	/**
	 * Creates a new {@link ReaderEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link ReaderEntity} object.
	 */
	public static ReaderEntity of(Reader content) {
		return new ReaderEntity(content, -1, null);
	}

	/**
	 * Creates a new {@link ReaderEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link ReaderEntity} object.
	 */
	public static ReaderEntity of(Reader content, long length, ContentType contentType) {
		return new ReaderEntity(content, length, contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 */
	public ReaderEntity(Reader content, long length, ContentType contentType) {
		this.content = content == null ? EMPTY_READER : content;
		this.length = length;
		setContentType(contentType);
	}

	/**
	 * Specifies the charset to use for the output.
	 *
	 * <p>
	 * The default is <js>"UTF-8"</js>.
	 *
	 * @param value The new charset, or <js>"UTF-8"</js> if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public ReaderEntity charset(Charset value) {
		this.charset = value == null ? UTF8 : value;
		return this;
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		cache();
		return cache.get();
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return asString().getBytes(UTF8);
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return cache.get() != null;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		String s = cache.get();
		return s == null ? new ReaderInputStream(content, charset) : new ReaderInputStream(new StringReader(s), charset);
	}

	@Override /* AbstractHttpEntity */
	public ReaderEntity cache() throws IOException {
		String s = cache.get();
		if (s == null) {
			try (Reader r = content) {
				s = read(r);
			}
			cache.set(s);
		}
		return this;
	}

	/**
	 * Writes bytes from the {@code InputStream} this entity was constructed
	 * with to an {@code OutputStream}.  The content length
	 * determines how many bytes are written.  If the length is unknown ({@code -1}), the
	 * stream will be completely consumed (to the end of the stream).
	 */
	@Override
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);

		OutputStreamWriter osw = new OutputStreamWriter(out, charset);
		String s = cache.get();
		if (s != null) {
			osw.write(s);
		} else {
			pipe(content, osw);
		}
		osw.flush();
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cache.get() == null;
	}
}
