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
package org.apache.juneau.ng.http;

import java.io.*;

/**
 * An HTTP message body (entity), providing content and its associated metadata.
 *
 * <p>
 * Implementations <b>should</b> stream content directly through {@link #writeTo(OutputStream)} rather than
 * buffering the entire body in memory.  Implementations that can determine the content length without buffering
 * may return a non-negative value from {@link #getContentLength()}, which allows transports to set
 * {@code Content-Length} headers efficiently.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public interface HttpBody {

	/**
	 * Returns the MIME content type of this body, e.g. {@code "application/json; charset=UTF-8"}.
	 *
	 * @return The content type, or <jk>null</jk> if unknown.
	 */
	String getContentType();

	/**
	 * Returns the content length in bytes, or {@code -1} if unknown.
	 *
	 * <p>
	 * Implementations that buffer content may return the exact length; streaming implementations should
	 * return {@code -1} to cause the transport to use chunked transfer encoding.
	 *
	 * @return The content length, or {@code -1} if unknown.
	 */
	default long getContentLength() {
		return -1;
	}

	/**
	 * Writes the body content to the given output stream.
	 *
	 * <p>
	 * Implementations <b>must not</b> close {@code out}.
	 *
	 * @param out The stream to write to. Never <jk>null</jk>.
	 * @throws IOException If an I/O error occurs.
	 */
	void writeTo(OutputStream out) throws IOException;

	/**
	 * Returns {@code true} if this body can be written more than once.
	 *
	 * <p>
	 * Streaming bodies backed by a one-shot {@link InputStream} should return {@code false}.
	 * Buffered bodies (byte arrays, strings, files) should return {@code true}.
	 *
	 * @return {@code true} if repeatable.
	 */
	default boolean isRepeatable() {
		return false;
	}
}
