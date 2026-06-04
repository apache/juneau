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
package org.apache.juneau.http.request;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.Utils.eq;

import java.util.*;

import org.apache.juneau.http.*;

/**
 * Default immutable implementation of {@link HttpRequestLine}.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// GET /users/123 HTTP/1.1</jc>
 * 	HttpRequestLine <jv>rl</jv> = HttpRequestLineBean.<jsm>of</jsm>(<js>"GET"</js>, <js>"/users/123"</js>);
 *
 * 	<jc>// Custom protocol version</jc>
 * 	HttpRequestLine <jv>rl2</jv> = HttpRequestLineBean.<jsm>of</jsm>(<js>"POST"</js>, <js>"/api/echo"</js>, HttpProtocolVersion.<jsf>HTTP_2_0</jsf>);
 * </p>
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
 * @since 10.0.0
 */
public final class HttpRequestLineBean implements HttpRequestLine {

	private final String method;
	private final String uri;
	private final HttpProtocolVersion protocolVersion;

	private HttpRequestLineBean(String method, String uri, HttpProtocolVersion protocolVersion) {
		this.method = assertArgNotNull("method", method);
		this.uri = assertArgNotNull("uri", uri);
		this.protocolVersion = assertArgNotNull("protocolVersion", protocolVersion);
	}

	/**
	 * Creates an {@code HTTP/1.1} request line with the given method and URI.
	 *
	 * @param method The request method (e.g. {@code "GET"}). Must not be <jk>null</jk>.
	 * @param uri The request URI. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpRequestLineBean of(String method, String uri) {
		return new HttpRequestLineBean(method, uri, HttpProtocolVersion.HTTP_1_1);
	}

	/**
	 * Creates a request line with the given method, URI, and typed protocol version.
	 *
	 * @param method The request method. Must not be <jk>null</jk>.
	 * @param uri The request URI. Must not be <jk>null</jk>.
	 * @param protocolVersion The protocol version. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpRequestLineBean of(String method, String uri, HttpProtocolVersion protocolVersion) {
		return new HttpRequestLineBean(method, uri, protocolVersion);
	}

	/**
	 * Creates a request line with the given method, URI, and parsed protocol version.
	 *
	 * @param method The request method. Must not be <jk>null</jk>.
	 * @param uri The request URI. Must not be <jk>null</jk>.
	 * @param protocolVersion The protocol version string (e.g. {@code "HTTP/2"}). Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpRequestLineBean of(String method, String uri, String protocolVersion) {
		return new HttpRequestLineBean(method, uri, HttpProtocolVersion.parse(protocolVersion));
	}

	@Override /* HttpRequestLine */
	public String getMethod() {
		return method;
	}

	@Override /* HttpRequestLine */
	public String getUri() {
		return uri;
	}

	@Override /* HttpRequestLine */
	public HttpProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	@Override /* Object */
	public String toString() {
		return method + ' ' + uri + ' ' + protocolVersion;
	}

	@Override /* Object */
	public boolean equals(Object obj) {
		return (obj instanceof HttpRequestLineBean o2) && eq(this, o2, (x, y) ->
			eq(x.method, y.method) && eq(x.uri, y.uri) && eq(x.protocolVersion, y.protocolVersion));
	}

	@Override /* Object */
	public int hashCode() {
		return Objects.hash(method, uri, protocolVersion);
	}
}
