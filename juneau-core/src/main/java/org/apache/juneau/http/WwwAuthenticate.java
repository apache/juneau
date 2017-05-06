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
 * Represents a parsed <l>WWW-Authenticate </l> HTTP response header.
 * <p>
 * Indicates the authentication scheme that should be used to access the requested entity.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	WWW-Authenticate: Basic
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The WWW-Authenticate response-header field MUST be included in 401 (Unauthorized) response messages.
 * The field value consists of at least one challenge that indicates the authentication scheme(s) and parameters
 * applicable to the Request-URI.
 * <p class='bcode'>
 * 	WWW-Authenticate  = "WWW-Authenticate" ":" 1#challenge
 * </p>
 * <p>
 * The HTTP access authentication process is described in "HTTP Authentication: Basic and Digest Access Authentication".
 * User agents are advised to take special care in parsing the WWW-Authenticate field value as it might contain more
 * than one challenge, or if more than one WWW-Authenticate header field is provided, the contents of a challenge
 * itself can contain a comma-separated list of authentication parameters.
 */
public final class WwwAuthenticate extends HeaderString {

	/**
	 * Returns a parsed <code>WWW-Authenticate</code> header.
	 *
	 * @param value The <code>WWW-Authenticate</code> header string.
	 * @return The parsed <code>WWW-Authenticate</code> header, or <jk>null</jk> if the string was null.
	 */
	public static WwwAuthenticate forString(String value) {
		if (value == null)
			return null;
		return new WwwAuthenticate(value);
	}

	private WwwAuthenticate(String value) {
		super(value);
	}
}
