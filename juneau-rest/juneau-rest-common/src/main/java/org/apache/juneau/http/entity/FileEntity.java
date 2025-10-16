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

/**
 * A repeatable entity that obtains its content from a {@link File}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class FileEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private byte[] byteCache;
	private String stringCache;

	/**
	 * Constructor.
	 */
	public FileEntity() {
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param content The entity contents.
	 */
	public FileEntity(ContentType contentType, File content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected FileEntity(FileEntity copyFrom) {
		super(copyFrom);
	}

	@Override
	public FileEntity copy() {
		return new FileEntity(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private File content() {
		File f = contentOrElse((File)null);
		Objects.requireNonNull(f, "File");
		if (! f.exists())
			throw new IllegalStateException("File " + f.getAbsolutePath() + " does not exist.");
		if (! f.canRead())
			throw new IllegalStateException("File " + f.getAbsolutePath() + " is not readable.");
		return f;
	}

	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		if (isCached() && stringCache == null)
			stringCache = read(new InputStreamReader(new FileInputStream(content()), getCharset()), getMaxLength());
		if (stringCache != null)
			return stringCache;
		return read(new InputStreamReader(new FileInputStream(content()), getCharset()), getMaxLength());
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null)
			byteCache = readBytes(content(), getMaxLength());
		if (byteCache != null)
			return byteCache;
		return readBytes(content());
	}

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* Overridden from HttpEntity */
	public long getContentLength() {
		return content().length();
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException {
		if (isCached())
			return new ByteArrayInputStream(asBytes());
		return new FileInputStream(content());
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		Utils.assertArgNotNull("out", out);

		if (isCached()) {
			out.write(asBytes());
		} else {
			try (InputStream is = getContent()) {
				IOUtils.pipe(is, out, getMaxLength());
			}
		}
	}
	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public FileEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}