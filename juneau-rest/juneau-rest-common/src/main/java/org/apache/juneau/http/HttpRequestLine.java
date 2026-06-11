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
package org.apache.juneau.http;

/**
 * The first line of an HTTP request message, containing the method, request URI, and protocol version.
 *
 * <p>
 * Mirrors the semantics of {@code org.apache.http.RequestLine} without the Apache HttpCore dependency.
 * The default immutable implementation is {@link org.apache.juneau.http.request.HttpRequestLineBean}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 * For production use cases that require long-term binary stability, continue using the existing
 * {@code juneau-rest-client} and {@code juneau-rest-common} APIs until the {@code ng} stack is declared stable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface HttpRequestLine {

	/**
	 * Returns the request method (e.g. {@code "GET"}, {@code "POST"}).
	 *
	 * @return The method name. Never <jk>null</jk>.
	 */
	String getMethod();

	/**
	 * Returns the request URI (e.g. {@code "/users/123?verbose=true"}).
	 *
	 * @return The URI. Never <jk>null</jk>.
	 */
	String getUri();

	/**
	 * Returns the HTTP protocol version (e.g. {@link HttpProtocolVersion#HTTP_1_1}).
	 *
	 * @return The protocol version. Never <jk>null</jk>.
	 */
	HttpProtocolVersion getProtocolVersion();
}
