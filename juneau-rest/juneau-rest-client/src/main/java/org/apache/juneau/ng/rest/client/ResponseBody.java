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
package org.apache.juneau.ng.rest.client;

import java.io.*;
import java.nio.charset.*;

/**
 * A fluent accessor for an HTTP response body returned by {@link NgRestResponse}.
 *
 * <p>
 * Provides convenient methods for reading the body as a string, byte array, or stream.
 * The body is read lazily; each method reads and returns the body content once.
 *
 * <p>
 * Obtain instances via {@link NgRestResponse#body()}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class ResponseBody {

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed; caller closes it after reading body
	})
	private final NgRestResponse response;

	ResponseBody(NgRestResponse response) {
		this.response = response;
	}

	/**
	 * Returns the response body as a UTF-8 string.
	 *
	 * @return The body string, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	public String asString() throws IOException {
		return response.getBodyAsString();
	}

	/**
	 * Returns the response body decoded with the given charset.
	 *
	 * @param charset The character set to use for decoding. Must not be <jk>null</jk>.
	 * @return The body string, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	@SuppressWarnings({
		"resource" // Body stream owned by transport; release by closing NgRestResponse, not the stream
	})
	public String asString(Charset charset) throws IOException {
		var stream = response.getBodyStream();
		if (stream == null)
			return null;
		return new String(stream.readAllBytes(), charset);
	}

	/**
	 * Returns the response body as a byte array.
	 *
	 * @return The body bytes, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	@SuppressWarnings({
		"resource" // Body stream owned by transport; release by closing NgRestResponse, not the stream
	})
	public byte[] asBytes() throws IOException {
		var stream = response.getBodyStream();
		if (stream == null)
			return null;
		return stream.readAllBytes();
	}

	/**
	 * Returns the raw response body stream.
	 *
	 * <p>
	 * Callers should not close this stream directly; close the parent {@link NgRestResponse} instead.
	 *
	 * @return The body stream, or <jk>null</jk> if the response has no body.
	 */
	@SuppressWarnings({
		"resource" // Stream owned by transport; close parent NgRestResponse instead (see Javadoc)
	})
	public InputStream asStream() {
		return response.getBodyStream();
	}

	/**
	 * Reads the response body into a byte array and returns it, or returns {@code null} if there is no body.
	 *
	 * <p>
	 * Equivalent to {@link #asBytes()}.
	 *
	 * @return The body bytes, or <jk>null</jk> if the response has no body.
	 * @throws IOException If an I/O error occurs reading the body.
	 */
	public byte[] readAllBytes() throws IOException {
		return asBytes();
	}
}
