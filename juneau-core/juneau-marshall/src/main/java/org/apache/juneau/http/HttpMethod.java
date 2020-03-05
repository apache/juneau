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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public enum HttpMethod {

	/** {@doc RFC2616.section9#sec9.2 OPTIONS} */
	OPTIONS(false),

	/** {@doc RFC2616.section9#sec9.3 GET} */
	GET(false),

	/** {@doc RFC2616.section9#sec9.4 HEAD} */
	HEAD(false),

	/** {@doc RFC2616.section9#sec9.5 POST} */
	POST(true),

	/** {@doc RFC2616.section9#sec9.6 PUT} */
	PUT(true),

	/** PATH */
	PATCH(true),

	/** {@doc RFC2616.section9#sec9.7 DELETE} */
	DELETE(false),

	/** {@doc RFC2616.section9#sec9.8 TRACE} */
	TRACE(false),

	/** {@doc RFC2616.section9#sec9.9 CONNECT} */
	CONNECT(false),

	/** HTTP MOVE */
	MOVE(false),

	/** A non-standard value. */
	OTHER(true);

	private boolean hasContent;

	HttpMethod(boolean hasContent) {
		this.hasContent = hasContent;
	}

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
	 * Returns whether this HTTP method normally has content.
	 *
	 * @return <jk>true</jk> if this HTTP method normally has content.
	 */
	public boolean hasContent() {
		return hasContent;
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
