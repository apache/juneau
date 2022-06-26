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

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link InputStream}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class InputStreamEntity extends BasicHttpEntity {

	private final InputStream content;
	private final long maxLength;
	private final byte[] cache;

	/**
	 * Creates a new {@link InputStreamEntity} builder.
	 *
	 * @return A new {@link InputStreamEntity} builder.
	 */
	public static HttpEntityBuilder<InputStreamEntity> create() {
		return new HttpEntityBuilder<>(InputStreamEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 * @throws IOException If stream could not be read.
	 */
	public InputStreamEntity(HttpEntityBuilder<?> builder) throws IOException {
		super(builder);
		content = contentOrElse(EMPTY_INPUT_STREAM);
		cache = builder.cached ? readBytes(content) : null;
		maxLength = builder.contentLength == -1 && cache != null ? cache.length : builder.contentLength;
	}

	/**
	 * Creates a new {@link InputStreamEntity} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link InputStreamEntity} builder initialized with the contents of this entity.
	 */
	@Override /* BasicHttpEntity */
	public HttpEntityBuilder<InputStreamEntity> copy() {
		return new HttpEntityBuilder<>(this);
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
		return maxLength;
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
			pipe(cache, out, (int)maxLength);
		} else {
			try (InputStream is = getContent()) {
				pipe(is, out, maxLength);
			}
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return cache == null;
	}
}
