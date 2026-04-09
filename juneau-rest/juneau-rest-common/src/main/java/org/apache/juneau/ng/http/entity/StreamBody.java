/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.ng.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;

import org.apache.juneau.ng.http.*;

/**
 * A non-repeatable {@link HttpBody} that streams content from an {@link InputStream}.
 *
 * <p>
 * Because the stream is one-shot, {@link #isRepeatable()} returns {@code false} and {@link #getContentLength()}
 * returns {@code -1} (the transport must use chunked transfer encoding).
 *
 * <p>
 * The caller is responsible for closing the wrapped stream after the response has been consumed.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // stream is caller-owned; StreamBody does not close it (documented in class Javadoc)
})
public final class StreamBody implements HttpBody {

	private final InputStream stream;
	private final String contentType;

	private StreamBody(InputStream stream, String contentType) {
		this.stream = assertArgNotNull("stream", stream);
		this.contentType = contentType;
	}

	/**
	 * Creates a {@link StreamBody} with {@code application/octet-stream} content type.
	 *
	 * @param stream The input stream. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static StreamBody of(InputStream stream) {
		return new StreamBody(stream, "application/octet-stream");
	}

	/**
	 * Creates a {@link StreamBody} with the given content type.
	 *
	 * @param stream The input stream. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static StreamBody of(InputStream stream, String contentType) {
		return new StreamBody(stream, contentType);
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		stream.transferTo(out);
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return false;
	}
}
