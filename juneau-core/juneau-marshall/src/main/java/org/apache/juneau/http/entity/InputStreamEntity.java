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

import org.apache.juneau.http.header.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link InputStream}.
 */
public class InputStreamEntity extends BasicHttpEntity2 {

	private final InputStream content;
	private final long length;
	private final byte[] cache;

	/**
	 * Creates a new {@link InputStreamEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link InputStreamEntity} builder.
	 */
	public static HttpEntityBuilder<InputStreamEntity> of(InputStream content) {
		return new HttpEntityBuilder<>(InputStreamEntity.class).content(content);
	}

	/**
	 * Creates a new {@link InputStreamEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamEntity} builder.
	 */
	public static HttpEntityBuilder<InputStreamEntity> of(InputStream content, long length, ContentType contentType) {
		return new HttpEntityBuilder<>(InputStreamEntity.class).content(content).contentLength(length).contentType(contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 * @throws IOException If stream could not be read.
	 */
	public InputStreamEntity(HttpEntityBuilder<?> builder) throws IOException {
		super(builder);
		this.content = builder.content == null ? EMPTY_INPUT_STREAM : (InputStream)builder.content;
		this.cache = builder.cached ? readBytes(this.content) : null;
		this.length = builder.contentLength == -1 && cache != null ? cache.length : builder.contentLength;
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(asBytes(), UTF8);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return cache == null ? readBytes(content) : cache;
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
		return cache == null ? content : new ByteArrayInputStream(cache);
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
			pipe(cache, out, (int)length);
		} else {
			try (InputStream is = getContent()) {
				pipe(is, out, length);
			}
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cache == null;
	}
}
