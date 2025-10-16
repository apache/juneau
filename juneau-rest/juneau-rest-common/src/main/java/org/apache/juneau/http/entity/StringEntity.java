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

import static org.apache.juneau.common.utils.IOUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A self contained, repeatable entity that obtains its content from a {@link String}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
@SuppressWarnings("resource")
public class StringEntity extends BasicHttpEntity {
	private static final String EMPTY = "";
	private byte[] byteCache;

	/**
	 * Constructor.
	 */
	public StringEntity() {
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity contents.
	 */
	public StringEntity(ContentType contentType, String content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected StringEntity(StringEntity copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null)
			byteCache = content().getBytes(getCharset());
		if (byteCache != null)
			return byteCache;
		return content().getBytes(getCharset());
	}
	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		return content();
	}

	@Override
	public StringEntity copy() {
		return new StringEntity(this);
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException {
		if (isCached())
			return new ByteArrayInputStream(asBytes());
		return new ReaderInputStream(new StringReader(content()), getCharset());
	}

	@Override /* Overridden from HttpEntity */
	public long getContentLength() {
		if (isCached())
			return asSafeBytes().length;
		long l = super.getContentLength();
		if (l != -1 || isSupplied())
			return l;
		String s = content();
		if (getCharset() == UTF8)
			for (int i = 0; i < s.length(); i++)
				if (s.charAt(i) > 127)
					return -1;
		return s.length();
	}

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() {
		return false;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}
	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		Utils.assertArgNotNull("out", out);
		if (isCached()) {
			out.write(asBytes());
		} else {
			OutputStreamWriter osw = new OutputStreamWriter(out, getCharset());
			osw.write(content());
			osw.flush();
		}
	}

	private String content() {
		return contentOrElse(EMPTY);
	}
}