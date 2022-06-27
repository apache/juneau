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

import static org.apache.juneau.internal.ArgUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link Reader}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ReaderEntity extends BasicHttpEntity {

	private final Reader content;
	private final long contentLength;
	private final Charset charset;
	private final byte[] cache;

	/**
	 * Creates a new {@link ReaderEntity} builder.
	 *
	 * @return A new {@link ReaderEntity} builder.
	 */
	public static HttpEntityBuilder<ReaderEntity> create() {
		return new HttpEntityBuilder<>(ReaderEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 * @throws IOException If reader could not be read.
	 */
	public ReaderEntity(HttpEntityBuilder<?> builder) throws IOException {
		super(builder);
		content = contentOrElse(EMPTY_READER);
		charset = firstNonNull(builder.charset, UTF8);
		cache = builder.cached ? readBytes(this.content) : null;
		contentLength = builder.contentLength == -1 && cache != null ? cache.length : builder.contentLength;
	}

	/**
	 * Creates a new {@link ReaderEntity} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link ReaderEntity} builder initialized with the contents of this entity.
	 */
	@Override /* BasicHttpEntity */
	public HttpEntityBuilder<ReaderEntity> copy() {
		return new HttpEntityBuilder<>(this);
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
		return contentLength;
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
