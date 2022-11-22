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

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.internal.*;

/**
 * A repeatable entity that obtains its content from a byte array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class ByteArrayEntity extends BasicHttpEntity {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final byte[] EMPTY = new byte[0];

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ByteArrayEntity() {
		super();
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

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(content(), getCharset());
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return content();
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return isSupplied() ? super.getContentLength() : content().length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(content());
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		out.write(content());
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setCached() throws IOException{
		super.setCached();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setCharset(Charset value) {
		super.setCharset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setChunked() {
		super.setChunked();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setChunked(boolean value) {
		super.setChunked(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContent(Object value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContent(Supplier<?> value) {
		super.setContent(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContentEncoding(String value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContentEncoding(ContentEncoding value) {
		super.setContentEncoding(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContentLength(long value) {
		super.setContentLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContentType(String value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setContentType(ContentType value) {
		super.setContentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setMaxLength(int value) {
		super.setMaxLength(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.http.entity.BasicHttpEntity */
	public ByteArrayEntity setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}
