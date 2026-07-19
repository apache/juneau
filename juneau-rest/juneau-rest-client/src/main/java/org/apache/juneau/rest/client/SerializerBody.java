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
package org.apache.juneau.rest.client;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * A repeatable {@link HttpBody} that serializes a POJO straight to the transport output stream.
 *
 * <p>
 * The next-generation analog of the classic {@code SerializedEntity}: rather than pre-serializing the POJO to an
 * in-memory {@link String}/{@code byte[]} and wrapping that, {@link #writeTo(OutputStream)} runs the supplied
 * {@link Serializer} directly against the wire output stream, so large payloads stream out without full in-memory
 * materialization.
 *
 * <p>
 * The body is {@linkplain #isRepeatable() repeatable}: each {@link #writeTo(OutputStream)} re-serializes the value, so
 * it can be safely resent (e.g. for a future auto-retry).  {@link #getContentLength()} returns {@code -1} (the
 * transport uses chunked transfer encoding).
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class SerializerBody implements HttpBody {

	private final Serializer serializer;
	private final Object value;
	private final String contentType;

	private SerializerBody(Serializer serializer, Object value, String contentType) {
		this.serializer = assertArgNotNull("serializer", serializer);
		this.value = value;
		this.contentType = contentType;
	}

	/**
	 * Creates a {@link SerializerBody} using the serializer's own {@linkplain Serializer#getResponseContentType() media type}.
	 *
	 * @param serializer The serializer that writes the POJO. Must not be <jk>null</jk>.
	 * @param value The POJO to serialize. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static SerializerBody of(Serializer serializer, Object value) {
		assertArgNotNull("serializer", serializer);
		var mt = serializer.getResponseContentType();
		return new SerializerBody(serializer, value, mt == null ? null : mt.toString());
	}

	/**
	 * Creates a {@link SerializerBody} with an explicit content type.
	 *
	 * @param serializer The serializer that writes the POJO. Must not be <jk>null</jk>.
	 * @param value The POJO to serialize. May be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static SerializerBody of(Serializer serializer, Object value, String contentType) {
		return new SerializerBody(serializer, value, contentType);
	}

	@Override /* HttpBody */
	public String getContentType() {
		return contentType;
	}

	@Override /* HttpBody */
	public long getContentLength() {
		return -1;
	}

	@Override /* HttpBody */
	public void writeTo(OutputStream out) throws IOException {
		try {
			serializer.write(value, out);
		} catch (SerializeException e) {
			throw new IOException(e);
		}
	}

	@Override /* HttpBody */
	public boolean isRepeatable() {
		return true;
	}
}
