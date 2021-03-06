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
import java.nio.charset.Charset;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A self contained, repeatable entity that obtains its content from a {@link String}.
 */
public class StringEntity extends BasicHttpEntity2 {

	private final String content;
	private final byte[] cache;
	private final Charset charset;

	/**
	 * Creates a new {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @return A new {@link StringEntity} builder.
	 */
	public static HttpEntityBuilder<StringEntity> of(String content) {
		return new HttpEntityBuilder<>(StringEntity.class).content(content);
	}

	/**
	 * Creates a new {@link StringEntity} builder.
	 *
	 * @param content The entity content.  Can be <jk>null</jk>.
	 * @param contentType The entity content type, or <jk>null</jk> if not specified.
	 * @return A new {@link StringEntity} builder.
	 */
	public static HttpEntityBuilder<StringEntity> of(String content, ContentType contentType) {
		return new HttpEntityBuilder<>(StringEntity.class).content(content).contentType(contentType);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 */
	public StringEntity(HttpEntityBuilder<?> builder) {
		super(builder);
		content = builder.content == null ? "" : (String)builder.content;
		charset = builder.charset == null ? UTF8 : builder.charset;
		cache = builder.cached ? this.content.getBytes(charset) : null;
	}

	@Override
	public byte[] asBytes() throws IOException {
		return cache == null ? content.getBytes() : cache;
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		if (cache != null)
			return cache.length;
		long l = super.getContentLength();
		if (l != -1)
			return l;
		if (charset == UTF8)
			for (int i = 0; i < content.length(); i++)
				if (content.charAt(i) > 127)
					return -1;
		return content.length();
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return cache == null ? new ReaderInputStream(new StringReader(content), charset) : new ByteArrayInputStream(cache);
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		if (cache != null) {
			out.write(cache);
		} else {
			OutputStreamWriter osw = new OutputStreamWriter(out, charset);
			osw.write(content);
			osw.flush();
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return false;
	}
}

