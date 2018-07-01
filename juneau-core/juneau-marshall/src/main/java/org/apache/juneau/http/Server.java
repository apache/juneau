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
 * Represents a parsed <l>Server</l> HTTP response header.
 *
 * <p>
 * A name for the server.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Server: Apache/2.4.1 (Unix)
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Server response-header field contains information about the software used by the origin server to handle the
 * request.
 * The field can contain multiple product tokens (section 3.8) and comments identifying the server and any significant
 * sub-products.
 * The product tokens are listed in order of their significance for identifying the application.
 *
 * <p class='bcode'>
 * 	Server         = "Server" ":" 1*( product | comment )
 * </p>
 *
 * <p>
 * Example:
 * <p class='bcode'>
 * 	Server: CERN/3.0 libwww/2.17
 * </p>
 *
 * <p>
 * If the response is being forwarded through a proxy, the proxy application MUST NOT modify the Server response-header.
 * Instead, it SHOULD include a Via field (as described in section 14.45).
 *
 * <p>
 * Note: Revealing the specific software version of the server might allow the server machine to become more vulnerable
 * to attacks against software that is known to contain security holes.
 * Server implementors are encouraged to make this field a configurable option.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class Server extends HeaderString {

	/**
	 * Returns a parsed <code>Server</code> header.
	 *
	 * @param value The <code>Server</code> header string.
	 * @return The parsed <code>Server</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Server forString(String value) {
		if (value == null)
			return null;
		return new Server(value);
	}

	private Server(String value) {
		super(value);
	}
}
