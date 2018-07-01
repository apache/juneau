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

import java.util.*;

/**
 * Represents valid HTTP 1.1 method names per the RFC 2616 spec.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public enum HttpMethod {

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.2" class="doclink">OPTIONS</a> */
	OPTIONS,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.3" class="doclink">GET</a> */
	GET,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.4" class="doclink">HEAD</a> */
	HEAD,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.5" class="doclink">POST</a> */
	POST,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.6" class="doclink">PUT</a> */
	PUT,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.7" class="doclink">DELETE</a> */
	DELETE,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.8" class="doclink">TRACE</a> */
	TRACE,

	/** <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9.9" class="doclink">CONNECT</a> */
	CONNECT,

	/** A non-standard value. */
	OTHER;

	private static final Map<String,HttpMethod> cache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	static {
		cache.put("OPTIONS", OPTIONS);
		cache.put("GET", GET);
		cache.put("HEAD", HEAD);
		cache.put("POST", POST);
		cache.put("PUT", PUT);
		cache.put("DELETE", DELETE);
		cache.put("TRACE", TRACE);
		cache.put("CONNECT", CONNECT);
	}

	/**
	 * Returns the enum for the specified key.
	 *
	 * <p>
	 * Case is ignored.
	 *
	 * @param key The HTTP method name.
	 * @return The HttpMethod enum, or {@link #OTHER} if it's not a standard method name.
	 */
	public static HttpMethod forString(String key) {
		HttpMethod m = cache.get(key);
		return m == null ? OTHER : m;
	}
}
