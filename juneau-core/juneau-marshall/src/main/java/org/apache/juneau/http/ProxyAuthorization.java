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

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Proxy-Authorization</l> HTTP request header.
 *
 * <p>
 * Authorization credentials for connecting to a proxy.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Proxy-Authorization request-header field allows the client to identify itself (or its user) to a proxy which
 * requires authentication.
 * The Proxy-Authorization field value consists of credentials containing the authentication information of the user
 * agent for the proxy and/or realm of the resource being requested.
 *
 * <p class='bcode w800'>
 * 	Proxy-Authorization     = "Proxy-Authorization" ":" credentials
 * </p>
 *
 * <p>
 * The HTTP access authentication process is described in "HTTP Authentication: Basic and Digest Access Authentication".
 * Unlike Authorization, the Proxy-Authorization header field applies only to the next outbound proxy that demanded
 * authentication using the Proxy-Authenticate field.
 * When multiple proxies are used in a chain, the Proxy-Authorization header field is consumed by the first outbound
 * proxy that was expecting to receive credentials.
 * A proxy MAY relay the credentials from the client request to the next proxy if that is the mechanism by which the
 * proxies cooperatively authenticate a given request.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Proxy-Authorization")
public final class ProxyAuthorization extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns a parsed <c>Proxy-Authorization</c> header.
	 *
	 * @param value The <c>Proxy-Authorization</c> header string.
	 * @return The parsed <c>Proxy-Authorization</c> header, or <jk>null</jk> if the string was null.
	 */
	public static ProxyAuthorization of(String value) {
		if (value == null)
			return null;
		return new ProxyAuthorization(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The value for this header.
	 */
	public ProxyAuthorization(String value) {
		super("Proxy-Authorization", value);
	}
}
