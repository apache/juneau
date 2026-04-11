/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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

import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.part.*;

/**
 * Static factory methods for creating common HTTP body types.
 *
 * <p>
 * Import statically for clean DSL-style usage:
 * <p class='bjava'>
 * 	import static org.apache.juneau.ng.http.HttpBodies.*;
 *
 * 	NgRestRequest <jv>req</jv> = client.post(<js>"/api/upload"</js>)
 * 		.body(stringBody(<js>"hello world"</js>, <js>"text/plain"</js>));
 * </p>
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
public final class HttpBodies {

	private HttpBodies() {}

	/**
	 * Creates a UTF-8 {@code text/plain} body.
	 *
	 * @param content The string content. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static StringBody stringBody(String content) {
		return StringBody.of(content);
	}

	/**
	 * Creates a string body with a specific content type.
	 *
	 * @param content The string content. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static StringBody stringBody(String content, String contentType) {
		return StringBody.of(content, contentType);
	}

	/**
	 * Creates a body backed by a byte array.
	 *
	 * @param bytes The byte content. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static ByteArrayBody byteArrayBody(byte[] bytes) {
		return ByteArrayBody.of(bytes);
	}

	/**
	 * Creates a body backed by a byte array with a specific content type.
	 *
	 * @param bytes The byte content. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static ByteArrayBody byteArrayBody(byte[] bytes, String contentType) {
		return ByteArrayBody.of(bytes, contentType);
	}

	/**
	 * Creates a body backed by a file.
	 *
	 * @param file The file. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static FileBody fileBody(File file) {
		return FileBody.of(file);
	}

	/**
	 * Creates a body backed by a file with a specific content type.
	 *
	 * @param file The file. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static FileBody fileBody(File file, String contentType) {
		return FileBody.of(file, contentType);
	}

	/**
	 * Creates a streaming body backed by an {@link InputStream}.
	 *
	 * @param stream The input stream. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static StreamBody streamBody(InputStream stream) {
		return StreamBody.of(stream);
	}

	/**
	 * Creates a streaming body backed by an {@link InputStream} with a specific content type.
	 *
	 * @param stream The input stream. Must not be <jk>null</jk>.
	 * @param contentType The MIME content type. May be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static StreamBody streamBody(InputStream stream, String contentType) {
		return StreamBody.of(stream, contentType);
	}

	/**
	 * Creates a URL-encoded form body from the given parts.
	 *
	 * @param parts The form parts. Must not be <jk>null</jk>.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static PartList formBody(HttpPart... parts) {
		return PartList.of(parts);
	}

	/**
	 * Creates a URL-encoded form body from alternating name/value string pairs.
	 *
	 * @param pairs Alternating name/value strings. Length must be even.
	 * @return A new body. Never <jk>null</jk>.
	 */
	public static PartList formBodyOfPairs(String... pairs) {
		return PartList.ofPairs(pairs);
	}
}
