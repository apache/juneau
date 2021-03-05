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
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A repeatable entity that obtains its content from a {@link File}.
 */
public class FileEntity extends AbstractHttpEntity {

	private final File content;
	private AtomicReference<byte[]> cache = new AtomicReference<>();

	/**
	 * Creates a new {@link FileEntity} object.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link FileEntity} object.
	 */
	public static FileEntity of(File content) {
		return new FileEntity(content, null);
	}

	/**
	 * Creates a new {@link FileEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link FileEntity} object.
	 */
	public static FileEntity of(File content, ContentType contentType) {
		return new FileEntity(content, contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 */
	public FileEntity(File content, ContentType contentType) {
		this.content = content;
		setContentType(contentType);
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return read(content);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		cache();
		return cache.get();
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return content == null ? 0 : content.length();
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		byte[] b = cache.get();
		return b == null ? new FileInputStream(content) : new ByteArrayInputStream(b);
	}

	@Override /* AbstractHttpEntity */
	public FileEntity cache() throws IOException {
		byte[] b = cache.get();
		if (b == null) {
			b = readBytes(content);
			cache.set(b);
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

		byte[] b = cache.get();
		if (b != null) {
			out.write(b);
		} else {
			try (InputStream is = getContent()) {
				IOUtils.pipe(is, out);
			}
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return false;
	}
}
