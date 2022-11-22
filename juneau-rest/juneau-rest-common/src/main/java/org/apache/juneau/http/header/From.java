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
 * Represents a parsed <l>From</l> HTTP request header.
 *
 * <p>
 * The email address of the user making the request.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	From: user@example.com
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The From request-header field, if given, SHOULD contain an Internet e-mail address for the human user who controls
 * the requesting user agent.
 * The address SHOULD be machine-usable, as defined by "mailbox" in RFC 822 [9] as updated by RFC 1123 [8]:
 *
 * <p class='bcode'>
 * 	From   = "From" ":" mailbox
 * </p>
 *
 * <p>
 * An example is:
 * <p class='bcode'>
 * 	From: webmaster@w3.org
 * </p>
 *
 * <p>
 * This header field MAY be used for logging purposes and as a means for identifying the source of invalid or unwanted
 * requests.
 * It SHOULD NOT be used as an insecure form of access protection.
 * The interpretation of this field is that the request is being performed on behalf of the person given, who accepts
 * responsibility for the method performed.
 * In particular, robot agents SHOULD include this header so that the person responsible for running the robot can be
 * contacted if problems occur on the receiving end.
 *
 * <p>
 * The Internet e-mail address in this field MAY be separate from the Internet host which issued the request.
 * For example, when a request is passed through a proxy the original issuer's address SHOULD be used.
 *
 * <p>
 * The client SHOULD NOT send the From header field without the user's approval, as it might conflict with the user's
 * privacy interests or their site's security policy.
 * It is strongly recommended that the user be able to disable, enable, and modify the value of this field at any time
 * prior to a request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("From")
public class From extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "From";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static From of(String value) {
		return value == null ? null : new From(value);
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
	public static From of(Supplier<String> value) {
		return value == null ? null : new From(value);
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
	public From(String value) {
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
	public From(Supplier<String> value) {
		super(NAME, value);
	}
}
