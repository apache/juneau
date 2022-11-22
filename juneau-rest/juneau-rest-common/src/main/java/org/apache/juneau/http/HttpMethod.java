// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;

/**
 * Represents valid HTTP 1.1 method name static strings per the RFC 2616 spec.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class HttpMethod {

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2">OPTIONS</a> */
	public static final String OPTIONS = "OPTIONS";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3">GET</a> */
	public static final String GET = "GET";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.4">HEAD</a> */
	public static final String HEAD = "HEAD";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5">POST</a> */
	public static final String POST = "POST";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6">PUT</a> */
	public static final String PUT = "PUT";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7">DELETE</a> */
	public static final String DELETE = "DELETE";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.8">TRACE</a> */
	public static final String TRACE = "TRACE";

	/** <a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.9">CONNECT</a> */
	public static final String CONNECT = "CONNECT";

	/** <a class="doclink" href="https://tools.ietf.org/html/rfc5789">PATCH</a> */
	public static final String PATCH = "PATCH";

	/** Special case for a REST method that implements a REST-RPC interface. */
	public static final String RRPC = "RRPC";

	/** A non-standard value. */
	public static final String OTHER = "OTHER";

	/** Represents any HTTP method. */
	public static final String ANY = "*";

	private static final Set<String> NO_BODY_METHODS = unmodifiable(set("GET","HEAD","DELETE","CONNECT","OPTIONS","TRACE"));

	/**
	 * Returns <jk>true</jk> if specified http method has content.
	 * <p>
	 * By default, anything not in this list can have content:  <c>GET, HEAD, DELETE, CONNECT, OPTIONS, TRACE</c>.
	 *
	 * @param name The HTTP method.
	 * @return <jk>true</jk> if specified http method has content.
	 */
	public static boolean hasContent(String name) {
		return ! NO_BODY_METHODS.contains(emptyIfNull(name).toUpperCase());
	}
}
