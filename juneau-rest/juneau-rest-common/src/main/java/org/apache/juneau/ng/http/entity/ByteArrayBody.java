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
package org.apache.juneau.ng.http.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;
import java.util.*;

import org.apache.juneau.ng.http.*;

/**
 * An {@link HttpBody} backed by a raw byte array.
 *
 * <p>
 * Repeatable: the same instance may be written multiple times.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * @since 9.2.1
 */
public final class ByteArrayBody implements HttpBody {

	private final byte[] content;
	private final String contentType;

	private ByteArrayBody(byte[] content, String contentType) {
		this.content = Arrays.copyOf(assertArgNotNull("content", content), content.length);
		this.contentType = contentType;
	}

	/**
	 * Creates a {@link ByteArrayBody} with {@code application/octet-stream} content type.
	 *
	 * @param content The byte array. Must not be <jk>null</jk>. The array is defensively copied.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ByteArrayBody of(byte[] content) {
		return new ByteArrayBody(content, "application/octet-stream");
	}

	/**
	 * Creates a {@link ByteArrayBody} with the given content type.
	 *
	 * @param content The byte array. Must not be <jk>null</jk>. The array is defensively copied.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static ByteArrayBody of(byte[] content, String contentType) {
		return new ByteArrayBody(content, contentType);
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return content.length;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		out.write(content);
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return true;
	}
}
