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
import static org.apache.juneau.internal.StringUtils.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A self contained, repeatable entity that obtains its content from a {@link String}.
 *
 * Similar to {@link org.apache.http.entity.StringEntity} but delays converting to an internal byte array until
 * actually used.
 */
public class StringEntity extends AbstractHttpEntity {

	private final String content;
	private final Charset charset;
	private AtomicReference<byte[]> bytes = new AtomicReference<>();

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * <p>
	 * Assumes {@link ContentType#TEXT_PLAIN TEXT/PLAIN} content type and <js>"UTF-8"</js> encoding.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} object.
	 */
	public static StringEntity of(String content) {
		return new StringEntity(content, ContentType.TEXT_PLAIN, IOUtils.UTF8);
	}

	/**
	 * Creates a new {@link StringEntity} object.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or {@link ContentType#TEXT_PLAIN} if not specified.
	 * @param charset The content character encoding, or <js>"UTF-8"</js> if not specified.
	 * @return A new {@link StringEntity} object.
	 */
	public static StringEntity of(String content, ContentType contentType, Charset charset) {
		return new StringEntity(content, contentType, null);
	}

	/**
	 * Constructor.
	 *
	 * @param content The entity content. Can be <jk>null</jk>.
	 * @param contentType The entity content type, or {@link ContentType#TEXT_PLAIN} if not specified.
	 * @param charset The content character encoding, or <js>"UTF-8"</js> if not specified.
	 */
	public StringEntity(String content, ContentType contentType, Charset charset) {
		this.content = emptyIfNull(content);
		setContentType(contentType);
		this.charset = charset;
	}

	@Override
	public byte[] asBytes() throws IOException {
		cache();
		return bytes.get();
	}

	@Override /* AbstractHttpEntity */
	public StringEntity cache() {
		getBytes();
		return this;
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		long len = super.getContentLength();
		if (len == -1) {
			len = getBytes().length;
			contentLength(len);
		}
		return len;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(getBytes());
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		out.write(getBytes());
		out.flush();
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return false;
	}

	private byte[] getBytes() {
		byte[] b = bytes.get();
		if (b == null) {
			 b = content.getBytes(charset == null ? IOUtils.UTF8 : charset);
			 bytes.set(b);
		}
		return b;
	}
}

