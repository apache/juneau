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

import static org.apache.juneau.commons.utils.Utils.eqic;

import java.util.*;

import org.apache.juneau.ng.http.*;

/**
 * A complete HTTP response message: status line, headers, and optional body.
 *
 * <p>
 * Mirrors the semantics of {@code org.apache.http.HttpResponse} without the Apache HttpCore dependency.
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
public interface HttpResponseMessage {

	/**
	 * Returns the status line of this response.
	 *
	 * @return The status line. Never <jk>null</jk>.
	 */
	HttpStatusLine getStatusLine();

	/**
	 * Returns all response headers, in the order they were received.
	 *
	 * @return An unmodifiable list of response headers. Never <jk>null</jk>.
	 */
	List<HttpHeader> getHeaders();

	/**
	 * Returns the first response header with the given name (case-insensitive), or <jk>null</jk> if absent.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return The first matching header, or <jk>null</jk>.
	 */
	default HttpHeader getFirstHeader(String name) {
		return getHeaders().stream()
			.filter(h -> eqic(h.getName(), name))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns all response headers with the given name (case-insensitive).
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return A list of matching headers (possibly empty). Never <jk>null</jk>.
	 */
	default List<HttpHeader> getHeaders(String name) {
		return getHeaders().stream()
			.filter(h -> eqic(h.getName(), name))
			.toList();
	}

	/**
	 * Returns the response body, or <jk>null</jk> if the response has no body.
	 *
	 * @return The response body, possibly <jk>null</jk>.
	 */
	HttpBody getBody();
}
