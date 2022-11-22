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
package org.apache.juneau.http.header;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Authorization</l> HTTP request header.
 *
 * <p>
 * Authentication credentials for HTTP authentication.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * A user agent that wishes to authenticate itself with a server--usually, but not necessarily, after receiving a 401
 * response--does so by including an Authorization request-header field with the request.
 *
 * <p>
 * The Authorization field value consists of credentials containing the authentication information of the user agent for
 * the realm of the resource being requested.
 *
 * <p class='bcode'>
 * 	Authorization  = "Authorization" ":" credentials
 * </p>
 *
 * <p>
 * HTTP access authentication is described in "HTTP Authentication: Basic and Digest Access Authentication".
 *
 * <p>
 * If a request is authenticated and a realm specified, the same credentials SHOULD be valid for all other requests
 * within this realm (assuming that the authentication scheme itself does not require otherwise, such as credentials
 * that vary according to a challenge value or using synchronized clocks).
 *
 * <p>
 * When a shared cache (see section 13.7) receives a request containing an Authorization field, it MUST NOT return the
 * corresponding response as a reply to any other request, unless one of the following specific exceptions holds:
 * <ol>
 * 	<li>If the response includes the "s-maxage" cache-control directive, the cache MAY use that response in replying
 * 		to a subsequent request.
 * 		But (if the specified maximum age has passed) a proxy cache MUST first revalidate it with the origin
 * 		server, using the request-headers from the new request to allow the origin server to authenticate the new
 * 		request.
 * 		(This is the defined behavior for s-maxage.)
 * 		If the response includes "s-maxage=0", the proxy MUST always revalidate it before re-using it.
 * 	<li>If the response includes the "must-revalidate" cache-control directive, the cache MAY use that response in
 * 		replying to a subsequent request.
 * 		But if the response is stale, all caches MUST first revalidate it with the origin server, using the
 * 		request-headers from the new request to allow the origin server to authenticate the new request.
 * 	<li>If the response includes the "public" cache-control directive, it MAY be returned in reply to any subsequent
 * 		request.
 * </ol>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Authorization")
public class Authorization extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Authorization";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Authorization of(String value) {
		return value == null ? null : new Authorization(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Authorization of(Supplier<String> value) {
		return value == null ? null : new Authorization(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Authorization(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Authorization(Supplier<String> value) {
		super(NAME, value);
	}
}
