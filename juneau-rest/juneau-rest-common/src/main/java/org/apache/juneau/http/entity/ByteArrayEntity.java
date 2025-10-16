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

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.header.*;

/**
 * A repeatable entity that obtains its content from a byte array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class ByteArrayEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final byte[] EMPTY = {};

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ByteArrayEntity() {
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.
	 * @param contents The entity contents.
	 */
	public ByteArrayEntity(ContentType contentType, byte[] contents) {
		super(contentType, contents);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.
	 */
	protected ByteArrayEntity(ByteArrayEntity copyFrom) {
		super(copyFrom);
	}

	@Override
	public ByteArrayEntity copy() {
		return new ByteArrayEntity(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private byte[] content() {
		return contentOrElse(EMPTY);
	}

	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(content(), getCharset());
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return content();
	}

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* Overridden from HttpEntity */
	public long getContentLength() {
		return isSupplied() ? super.getContentLength() : content().length;
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(content());
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		Utils.assertArgNotNull("out", out);
		out.write(content());
	}
	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}
}