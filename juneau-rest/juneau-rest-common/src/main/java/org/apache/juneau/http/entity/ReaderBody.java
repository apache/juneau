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
package org.apache.juneau.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.http.*;

/**
 * A non-repeatable {@link HttpBody} that streams character content from a {@link Reader}, encoded as UTF-8.
 *
 * <p>
 * The character analog of {@link StreamBody}.  Because the reader is one-shot, {@link #isRepeatable()} returns
 * {@code false} and {@link #getContentLength()} returns {@code -1} (the transport must use chunked transfer encoding).
 * Content is streamed directly through {@link #writeTo(OutputStream)} rather than being drained into a {@link String}
 * first.
 *
 * <p>
 * The caller is responsible for closing the wrapped reader after the response has been consumed.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // reader is caller-owned; the OutputStreamWriter wraps (but does not close) the caller-owned out
})
public final class ReaderBody implements HttpBody {

	private final Reader reader;
	private final String contentType;

	private ReaderBody(Reader reader, String contentType) {
		this.reader = assertArgNotNull("reader", reader);
		this.contentType = contentType;
	}

	/**
	 * Creates a {@link ReaderBody} with {@code text/plain} content type.
	 *
	 * @param reader The reader. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ReaderBody of(Reader reader) {
		return new ReaderBody(reader, "text/plain");
	}

	/**
	 * Creates a {@link ReaderBody} with the given content type.
	 *
	 * @param reader The reader. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ReaderBody of(Reader reader, String contentType) {
		return new ReaderBody(reader, contentType);
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		var w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		reader.transferTo(w);
		w.flush();
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return false;
	}
}
