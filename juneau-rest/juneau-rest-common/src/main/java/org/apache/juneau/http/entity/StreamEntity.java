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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link InputStream}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class StreamEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private byte[] byteCache;
	private String stringCache;

	/**
	 * Constructor.
	 */
	public StreamEntity() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity contents.
	 */
	public StreamEntity(ContentType contentType, InputStream content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected StreamEntity(StreamEntity copyFrom) {
		super(copyFrom);
	}

	@Override
	public StreamEntity copy() {
		return new StreamEntity(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private InputStream content() {
		InputStream is = contentOrElse((InputStream)null);
		if (is == null)
			throw new RuntimeException("Input stream is null.");
		return is;
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		if (isCached() && stringCache == null)
			stringCache = read(content(), getCharset());
		if (stringCache != null)
			return stringCache;
		return read(content());
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null)
			byteCache = readBytes(content(), getMaxLength());
		if (byteCache != null)
			return byteCache;
		return readBytes(content(), getMaxLength());
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
		return content();
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

		if (isCached()) {
			out.write(asBytes());
		} else {
			try (InputStream is = getContent()) {
				pipe(is, out, getMaxLength());
			}
		}
	}

	@Override /* HttpEntity */
	public boolean isStreaming() {
		return ! isCached();
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public StreamEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}
