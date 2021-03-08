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

/**
 * A repeatable entity that obtains its content from a byte array.
 */
public class ByteArrayEntity extends BasicHttpEntity {

	private static final byte[] EMPTY = new byte[0];

	/**
	 * Creates a new {@link ByteArrayEntity} builder.
	 *
	 * @return A new {@link ByteArrayEntity} builder.
	 */
	public static HttpEntityBuilder<ByteArrayEntity> create() {
		return new HttpEntityBuilder<>(ByteArrayEntity.class);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The entity builder.
	 */
	public ByteArrayEntity(HttpEntityBuilder<?> builder) {
		super(builder);
	}

	/**
	 * Creates a new {@link ByteArrayEntity} builder initialized with the contents of this entity.
	 *
	 * @return A new {@link ByteArrayEntity} builder initialized with the contents of this entity.
	 */
	@Override /* BasicHttpEntity */
	public HttpEntityBuilder<ByteArrayEntity> copy() {
		return new HttpEntityBuilder<>(this);
	}

	private byte[] bytes() {
		return contentOrElse(EMPTY);
	}

	@Override /* AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(bytes(), UTF8);
	}

	@Override /* AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return bytes();
	}

	@Override /* HttpEntity */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* HttpEntity */
	public long getContentLength() {
		return isSupplied() ? super.getContentLength() : bytes().length;
	}

	@Override /* HttpEntity */
	public InputStream getContent() throws IOException {
		return new ByteArrayInputStream(bytes());
	}

	@Override /* HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull("out", out);
		out.write(bytes());
	}
}
