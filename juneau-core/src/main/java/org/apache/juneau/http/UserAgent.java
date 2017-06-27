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
 * Represents a parsed <l>User-Agent</l> HTTP request header.
 * <p>
 * The user agent string of the user agent.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The User-Agent request-header field contains information about the user agent originating the request.
 * This is for statistical purposes, the tracing of protocol violations, and automated recognition of user agents for
 * the sake of tailoring responses to avoid particular user agent limitations.
 * User agents SHOULD include this field with requests.
 * The field can contain multiple product tokens (section 3.8) and comments identifying the agent and any sub-products
 * which form a significant part of the user agent.
 * By convention, the product tokens are listed in order of their significance for identifying the application.
 * <p class='bcode'>
 * 	User-Agent     = "User-Agent" ":" 1*( product | comment )
 * </p>
 * <p>
 * Example:
 * <p class='bcode'>
 * 	User-Agent: CERN-LineMode/2.15 libwww/2.17b3
 * </p>
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
public final class UserAgent extends HeaderString {

	/**
	 * Returns a parsed <code>User-Agent</code> header.
	 *
	 * @param value The <code>User-Agent</code> header string.
	 * @return The parsed <code>User-Agent</code> header, or <jk>null</jk> if the string was null.
	 */
	public static UserAgent forString(String value) {
		if (value == null)
			return null;
		return new UserAgent(value);
	}

	private UserAgent(String value) {
		super(value);
	}
}
