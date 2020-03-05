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
 * Represents a parsed <l>User-Agent</l> HTTP request header.
 *
 * <p>
 * The user agent string of the user agent.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The User-Agent request-header field contains information about the user agent originating the request.
 * This is for statistical purposes, the tracing of protocol violations, and automated recognition of user agents for
 * the sake of tailoring responses to avoid particular user agent limitations.
 * User agents SHOULD include this field with requests.
 * The field can contain multiple product tokens (section 3.8) and comments identifying the agent and any sub-products
 * which form a significant part of the user agent.
 * By convention, the product tokens are listed in order of their significance for identifying the application.
 *
 * <p class='bcode w800'>
 * 	User-Agent     = "User-Agent" ":" 1*( product | comment )
 * </p>
 *
 * <p>
 * Example:
 * <p class='bcode w800'>
 * 	User-Agent: CERN-LineMode/2.15 libwww/2.17b3
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("User-Agent")
public final class UserAgent extends BasicStringHeader {

	/**
	 * Returns a parsed <c>User-Agent</c> header.
	 *
	 * @param value The <c>User-Agent</c> header string.
	 * @return The parsed <c>User-Agent</c> header, or <jk>null</jk> if the string was null.
	 */
	public static UserAgent forString(String value) {
		if (value == null)
			return null;
		return new UserAgent(value);
	}

	private UserAgent(String value) {
		super("User-Agent", value);
	}
}
