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
package org.apache.juneau.ng.http.response;

import static org.apache.juneau.commons.utils.AssertionUtils.assertArgNotNull;
import static org.apache.juneau.commons.utils.Utils.eq;

import java.util.*;

import org.apache.juneau.ng.http.*;

/**
 * Default immutable implementation of {@link HttpStatusLine}.
 *
 * <p>
 * Create instances via the static factory methods:
 * <p class='bjava'>
 * 	<jc>// 200 OK  HTTP/1.1</jc>
 * 	HttpStatusLine <jv>sl</jv> = HttpStatusLineBean.<jsm>of</jsm>(200, <js>"OK"</js>);
 *
 * 	<jc>// Custom protocol version</jc>
 * 	HttpStatusLine <jv>sl2</jv> = HttpStatusLineBean.<jsm>of</jsm>(<js>"HTTP/2"</js>, 200, <js>"OK"</js>);
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
 * @since 9.2.1
 */
public final class HttpStatusLineBean implements HttpStatusLine {

	/** Default HTTP/1.1 protocol version string. */
	public static final String HTTP_1_1 = "HTTP/1.1";

	private final String protocolVersion;
	private final int statusCode;
	private final String reasonPhrase;

	private HttpStatusLineBean(String protocolVersion, int statusCode, String reasonPhrase) {
		this.protocolVersion = assertArgNotNull("protocolVersion", protocolVersion);
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	/**
	 * Creates an {@code HTTP/1.1} status line with the given status code and reason phrase.
	 *
	 * @param statusCode The HTTP status code (e.g. {@code 200}).
	 * @param reasonPhrase The reason phrase (e.g. {@code "OK"}). May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpStatusLineBean of(int statusCode, String reasonPhrase) {
		return new HttpStatusLineBean(HTTP_1_1, statusCode, reasonPhrase);
	}

	/**
	 * Creates a status line with a custom protocol version, status code, and reason phrase.
	 *
	 * @param protocolVersion The protocol version string (e.g. {@code "HTTP/2"}). Must not be <jk>null</jk>.
	 * @param statusCode The HTTP status code.
	 * @param reasonPhrase The reason phrase. May be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static HttpStatusLineBean of(String protocolVersion, int statusCode, String reasonPhrase) {
		return new HttpStatusLineBean(protocolVersion, statusCode, reasonPhrase);
	}

	@Override /* HttpStatusLine */
	public int getStatusCode() {
		return statusCode;
	}

	@Override /* HttpStatusLine */
	public String getReasonPhrase() {
		return reasonPhrase;
	}

	@Override /* HttpStatusLine */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	@Override /* Object */
	public String toString() {
		var sb = new StringBuilder(protocolVersion).append(' ').append(statusCode);
		if (reasonPhrase != null)
			sb.append(' ').append(reasonPhrase);
		return sb.toString();
	}

	@Override /* Object */
	public boolean equals(Object obj) {
		return (obj instanceof HttpStatusLineBean o2) && eq(this, o2, (x, y) ->
			x.statusCode == y.statusCode && eq(x.protocolVersion, y.protocolVersion) && eq(x.reasonPhrase, y.reasonPhrase));
	}

	@Override /* Object */
	public int hashCode() {
		return Objects.hash(protocolVersion, statusCode, reasonPhrase);
	}
}
