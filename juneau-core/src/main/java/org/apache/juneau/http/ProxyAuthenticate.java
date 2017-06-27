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

/**
 * Represents a parsed <l> Proxy-Authenticate</l> HTTP response header.
 * <p>
 * Request authentication to access the proxy.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Proxy-Authenticate: Basic
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Proxy-Authenticate response-header field MUST be included as part of a 407 (Proxy Authentication Required)
 * response.
 * The field value consists of a challenge that indicates the authentication scheme and parameters applicable to the
 * proxy for this Request-URI.
 * <p class='bcode'>
 * 	Proxy-Authenticate  = "Proxy-Authenticate" ":" 1#challenge
 * </p>
 * <p>
 * The HTTP access authentication process is described in "HTTP Authentication: Basic and Digest Access Authentication".
 * Unlike WWW-Authenticate, the Proxy-Authenticate header field applies only to the current connection and SHOULD NOT
 * be passed on to downstream clients.
 * However, an intermediate proxy might need to obtain its own credentials by requesting them from the downstream
 * client, which in some circumstances will appear as if the proxy is forwarding the Proxy-Authenticate header field.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class ProxyAuthenticate extends HeaderString {

	/**
	 * Returns a parsed <code>Proxy-Authenticate</code> header.
	 *
	 * @param value The <code>Proxy-Authenticate</code> header string.
	 * @return The parsed <code>Proxy-Authenticate</code> header, or <jk>null</jk> if the string was null.
	 */
	public static ProxyAuthenticate forString(String value) {
		if (value == null)
			return null;
		return new ProxyAuthenticate(value);
	}

	private ProxyAuthenticate(String value) {
		super(value);
	}
}
