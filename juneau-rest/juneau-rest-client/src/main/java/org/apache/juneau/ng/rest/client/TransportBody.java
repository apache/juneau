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

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;

import java.io.*;

import org.apache.juneau.ng.http.*;

/**
 * The body of a transport-layer request — a thin wrapper around {@link HttpBody} that gives transports a
 * stable contract without needing to know about higher-level body types.
 *
 * <p>
 * Transport implementations call {@link HttpBody#writeTo(OutputStream)} to stream the body directly to the
 * underlying connection, avoiding full in-memory buffering.
 *
 * <p>
 * <b>Beta — API subject to change.</b>
 *
 * @since 9.2.1
 */
public final class TransportBody {

	private final HttpBody body;

	private TransportBody(HttpBody body) {
		this.body = body;
	}

	/**
	 * Wraps the given {@link HttpBody} as a {@link TransportBody}.
	 *
	 * @param body The HTTP body. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static TransportBody of(HttpBody body) {
		return new TransportBody(assertArgNotNull("body", body));
	}

	/**
	 * Returns the content type of this body (e.g. {@code "application/json; charset=UTF-8"}), or {@code null} if unknown.
	 *
	 * @return The content type, possibly <jk>null</jk>.
	 */
	public String getContentType() {
		return body.getContentType();
	}

	/**
	 * Returns the content length in bytes, or {@code -1} if unknown (signals chunked transfer).
	 *
	 * @return The content length, or {@code -1}.
	 */
	public long getContentLength() {
		return body.getContentLength();
	}

	/**
	 * Streams the body content to the given output stream.
	 *
	 * <p>
	 * The caller must not close {@code out}.
	 *
	 * @param out The output stream. Must not be <jk>null</jk>.
	 * @throws IOException If an I/O error occurs.
	 */
	public void writeTo(OutputStream out) throws IOException {
		body.writeTo(out);
	}

	/**
	 * Returns {@code true} if this body can be written more than once.
	 *
	 * @return {@code true} if repeatable.
	 */
	public boolean isRepeatable() {
		return body.isRepeatable();
	}
}
