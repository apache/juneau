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
 * A repeatable entity that obtains its content from a byte array.
 */
public class ByteArrayEntity extends BasicHttpEntity2 {

	private final byte[] content;

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * <p>
	 * Assumes no content type.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static HttpEntityBuilder<ByteArrayEntity> of(byte[] content) {
		return new HttpEntityBuilder<>(ByteArrayEntity.class).content(content);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null<jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static HttpEntityBuilder<ByteArrayEntity> of(byte[] content, ContentType contentType) {
		return new HttpEntityBuilder<>(ByteArrayEntity.class).content(content).contentType(contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 */
	public ByteArrayEntity(HttpEntityBuilder<?> builder) {
		super(builder);
		this.content = builder.content == null ? new byte[0] : (byte[])builder.content;
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(content, UTF8);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return content;
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return content.length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(content);
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		out.write(content);
	}
}
