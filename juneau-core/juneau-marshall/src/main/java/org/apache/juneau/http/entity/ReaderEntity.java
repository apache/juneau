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

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link Reader}.
 */
public class ReaderEntity extends BasicHttpEntity2 {

	private final Reader content;
	private final long length;
	private final Charset charset;
	private final byte[] cache;

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static HttpEntityBuilder<ReaderEntity> of(Reader content) {
		return new HttpEntityBuilder<>(ReaderEntity.class).content(content);
	}

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @param content The entity builder.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static HttpEntityBuilder<ReaderEntity> of(Reader content, long length, ContentType contentType) {
		return new HttpEntityBuilder<>(ReaderEntity.class).content(content).contentLength(length).contentType(contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 * @throws IOException If reader could not be read.
	 */
	public ReaderEntity(HttpEntityBuilder<?> builder) throws IOException {
		super(builder);
		this.content = builder.content == null ? EMPTY_READER : (Reader)builder.content;
		this.charset = builder.charset == null ? UTF8 : builder.charset;
		this.cache = builder.cached ? readBytes(this.content) : null;
		this.length = builder.contentLength == -1 && cache != null ? cache.length : builder.contentLength;
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return cache == null ? read(content) : new String(cache, UTF8);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return cache == null ? asString().getBytes(UTF8) : cache;
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return cache != null;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return cache == null ? new ReaderInputStream(content, charset) : new ByteArrayInputStream(cache);
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

		if (cache != null) {
			out.write(cache);
		} else {
			OutputStreamWriter osw = new OutputStreamWriter(out, charset);
			pipe(content, osw);
			osw.flush();
		}
		out.flush();
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cache == null;
	}
}
