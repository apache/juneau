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
 * Represents a parsed <l>Transfer-Encoding</l> HTTP response header.
 *
 * <p>
 * The form of encoding used to safely transfer the entity to the user.
 * Currently defined methods are: chunked, compress, deflate, gzip, identity.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Transfer-Encoding: chunked
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Transfer-Encoding general-header field indicates what (if any) type of transformation has been applied to the
 * message body in order to safely transfer it between the sender and the recipient.
 * This differs from the content-coding in that the transfer-coding is a property of the message, not of the entity.
 *
 * <p class='bcode'>
 * 	Transfer-Encoding       = "Transfer-Encoding" ":" 1#transfer-coding
 * </p>
 *
 * <p>
 * Transfer-codings are defined in section 3.6. An example is:
 *
 * <p class='bcode'>
 * 	Transfer-Encoding: chunked
 * </p>
 *
 * <p>
 * If multiple encodings have been applied to an entity, the transfer-codings MUST be listed in the order in which
 * they were applied.
 * Additional information about the encoding parameters MAY be provided by other entity-header fields not defined by
 * this specification.
 *
 * <p>
 * Many older HTTP/1.0 applications do not understand the Transfer-Encoding header.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Transfer-Encoding")
public class TransferEncoding extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Transfer-Encoding";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static TransferEncoding of(String value) {
		return value == null ? null : new TransferEncoding(value);
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
	public static TransferEncoding of(Supplier<String> value) {
		return value == null ? null : new TransferEncoding(value);
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
	public TransferEncoding(String value) {
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
	public TransferEncoding(Supplier<String> value) {
		super(NAME, value);
	}
}
