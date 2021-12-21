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
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.nio.charset.Charset;

import org.apache.juneau.internal.*;

/**
 * A self contained, repeatable entity that obtains its content from a {@link String}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class StringEntity extends BasicHttpEntity {

	private static final String EMPTY = "";

	private final byte[] cache;
	private final Charset charset;

	/**
	 * Creates a new {@link StringEntity} builder.
	 *
	 * @return A new {@link StringEntity} builder.
	 */
	public static HttpEntityBuilder<StringEntity> create() {
		return new HttpEntityBuilder<>(StringEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 */
	public StringEntity(HttpEntityBuilder<?> builder) {
		super(builder);
		charset = firstNonNull(builder.charset, UTF8);
		cache = builder.cached ? string().getBytes(charset) : null;
	}

	/**
	 * Creates a new {@link StringEntity} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link StringEntity} builder initialized with the contents of this entity.
	 */
	@Override /* BasicHttpEntity */
	public HttpEntityBuilder<StringEntity> copy() {
		return new HttpEntityBuilder<>(this);
	}

	private String string() {
		return contentOrElse(EMPTY);
	}

	@Override
	public byte[] asBytes() throws IOException {
		return cache == null ? string().getBytes() : cache;
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
		if (l != -1 || isSupplied())
			return l;
		String s = string();
		if (charset == UTF8)
			for (int i = 0; i < s.length(); i++)
				if (s.charAt(i) > 127)
					return -1;
		return s.length();
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		if (cache != null)
			return new ByteArrayInputStream(cache);
		String s = string();
		if (s == null)
			return IOUtils.EMPTY_INPUT_STREAM;
		return new ReaderInputStream(new StringReader(s), charset);
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		if (cache != null) {
			out.write(cache);
		} else {
			OutputStreamWriter osw = new OutputStreamWriter(out, charset);
			osw.write(string());
			osw.flush();
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return false;
	}
}

