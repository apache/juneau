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

import org.apache.juneau.internal.*;

/**
 * A repeatable entity that obtains its content from a {@link File}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class FileEntity extends BasicHttpEntity {

	private final File content;
	private final byte[] cache;

	/**
	 * Creates a new {@link FileEntity} builder.
	 *
	 * @return A new {@link FileEntity} builder.
	 */
	public static HttpEntityBuilder<FileEntity> create() {
		return new HttpEntityBuilder<>(FileEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 * @throws IOException If file could not be read.
	 */
	public FileEntity(HttpEntityBuilder<?> builder) throws IOException {
		super(builder);
		content = contentOrElse(null);
		cache = builder.cached ? readBytes(content) : null;
	}

	/**
	 * Creates a new {@link FileEntity} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link FileEntity} builder initialized with the contents of this entity.
	 */
	@Override /* BasicHttpEntity */
	public HttpEntityBuilder<FileEntity> copy() {
		return new HttpEntityBuilder<>(this);
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return read(content);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return cache == null ? readBytes(this.content) : cache;
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
		return cache == null ? new FileInputStream(content) : new ByteArrayInputStream(cache);
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);

		if (cache != null) {
			out.write(cache);
		} else {
			try (InputStream is = getContent()) {
				IOUtils.pipe(is, out);
			}
		}
	}
}
