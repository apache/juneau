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

import java.io.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link InputStream}.
 */
public class InputStreamEntity extends AbstractHttpEntity {

	private final InputStream content;
	private final long length;
	private AtomicReference<byte[]> bytes = new AtomicReference<>();

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static InputStreamEntity of(InputStream content) {
		return new InputStreamEntity(content, -1, null);
	}

	/**
	 * Creates a new {@link InputStreamEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 * @return A new {@link InputStreamEntity} object.
	 */
	public static InputStreamEntity of(InputStream content, long length, ContentType contentType) {
		return new InputStreamEntity(content, length, contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @param length The content length, or <c>-1</c> if not known.
	 */
	public InputStreamEntity(InputStream content, long length, ContentType contentType) {
		this.content = content == null ? IOUtils.EMPTY_INPUT_STREAM : content;
		this.length = length;
		setContentType(contentType);
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return false;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		byte[] b = bytes.get();
		return b == null ? content : new ByteArrayInputStream(b);
	}

	@Override /* AbstractHttpEntity */
	public InputStreamEntity cache() throws IOException {
		byte[] b = bytes.get();
		if (b == null) {
			b = IOUtils.readBytes(content);
			bytes.set(b);
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
		InputStream is = getContent();
		try {
			byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
			int readLen;
			if (length < 0) {
				// consume until EOF
				while ((readLen = is.read(buffer)) != -1) {
					out.write(buffer, 0, readLen);
				}
			} else {
				// consume no more than length
				long remaining = length;
				while (remaining > 0) {
					readLen = is.read(buffer, 0, (int)Math.min(OUTPUT_BUFFER_SIZE, remaining));
					if (readLen == -1) {
						break;
					}
					out.write(buffer, 0, readLen);
					remaining -= readLen;
				}
			}
		} finally {
			is.close();
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return bytes.get() == null;
	}
}
