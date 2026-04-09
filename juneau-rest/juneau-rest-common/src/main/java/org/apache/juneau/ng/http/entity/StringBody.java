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
import java.nio.charset.*;

import org.apache.juneau.ng.http.*;

/**
 * An {@link HttpBody} backed by a UTF-8 string.
 *
 * <p>
 * Repeatable: the same instance may be written multiple times.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * @since 9.2.1
 */
public final class StringBody implements HttpBody {

	private final String content;
	private final String contentType;
	private final byte[] bytes;

	private StringBody(String content, String contentType) {
		this.content = assertArgNotNull("content", content);
		this.contentType = contentType;
		this.bytes = content.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Creates a {@link StringBody} with the given content and {@code text/plain; charset=UTF-8} content type.
	 *
	 * @param content The string content. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static StringBody of(String content) {
		return new StringBody(content, "text/plain; charset=UTF-8");
	}

	/**
	 * Creates a {@link StringBody} with the given content and content type.
	 *
	 * @param content The string content. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type (e.g. {@code "application/json; charset=UTF-8"}). May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static StringBody of(String content, String contentType) {
		return new StringBody(content, contentType);
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return bytes.length;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		out.write(bytes);
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return true;
	}

	@Override /* Object */
	public String toString() {
		return content;
	}
}
