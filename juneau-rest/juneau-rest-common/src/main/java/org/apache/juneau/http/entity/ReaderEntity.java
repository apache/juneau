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
import java.util.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link Reader}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class ReaderEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private byte[] byteCache;
	private String stringCache;

	/**
	 * Constructor.
	 */
	public ReaderEntity() {
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity contents.
	 */
	public ReaderEntity(ContentType contentType, Reader content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected ReaderEntity(ReaderEntity copyFrom) {
		super(copyFrom);
	}

	@Override
	public ReaderEntity copy() {
		return new ReaderEntity(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("resource") // Caller closes
    private Reader content() {
		return Objects.requireNonNull(contentOrElse((Reader) null), "Reader is null.");
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		if (isCached() && stringCache == null)
			stringCache = read(content(), getMaxLength());
		if (stringCache != null)
			return stringCache;
		return read(content());
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null)
			byteCache = readBytes(content());
		if (byteCache != null)
			return byteCache;
		return readBytes(content());
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return isCached();
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		if (isCached())
			return asSafeBytes().length;
		return super.getContentLength();
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		if (isCached())
			return new ByteArrayInputStream(asBytes());
		return new ReaderInputStream(content(), getCharset());
	}

	/**
	 * Writes bytes from the {@code InputStream} this entity was constructed
	 * with to an {@code OutputStream}.  The content length
	 * determines how many bytes are written.  If the length is unknown ({@code -1}), the
	 * stream will be completely consumed (to the end of the stream).
	 */
	@Override
	public void writeTo(OutputStream out) throws IOException {
		Utils.assertArgNotNull("out", out);

		if (isCached()) {
			out.write(asBytes());
		} else {
			OutputStreamWriter osw = new OutputStreamWriter(out, getCharset());
			pipe(content(), osw);
			osw.flush();
		}
		out.flush();
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return ! isCached();
	}
	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ReaderEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}