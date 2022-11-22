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
 * Represents a parsed <l>WWW-Authenticate </l> HTTP response header.
 *
 * <p>
 * Indicates the authentication scheme that should be used to access the requested entity.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	WWW-Authenticate: Basic
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The WWW-Authenticate response-header field MUST be included in 401 (Unauthorized) response messages.
 * The field value consists of at least one challenge that indicates the authentication scheme(s) and parameters
 * applicable to the Request-URI.
 *
 * <p class='bcode'>
 * 	WWW-Authenticate  = "WWW-Authenticate" ":" 1#challenge
 * </p>
 *
 * <p>
 * The HTTP access authentication process is described in "HTTP Authentication: Basic and Digest Access Authentication".
 * User agents are advised to take special care in parsing the WWW-Authenticate field value as it might contain more
 * than one challenge, or if more than one WWW-Authenticate header field is provided, the contents of a challenge
 * itself can contain a comma-separated list of authentication parameters.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("WWW-Authenticate")
public class WwwAuthenticate extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "WWW-Authenticate";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static WwwAuthenticate of(String value) {
		return value == null ? null : new WwwAuthenticate(value);
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
	public static WwwAuthenticate of(Supplier<String> value) {
		return value == null ? null : new WwwAuthenticate(value);
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
	public WwwAuthenticate(String value) {
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
	public WwwAuthenticate(Supplier<String> value) {
		super(NAME, value);
	}
}
