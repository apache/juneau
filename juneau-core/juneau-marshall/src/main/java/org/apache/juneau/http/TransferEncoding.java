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
 * Represents a parsed <l>Transfer-Encoding</l> HTTP response header.
 *
 * <p>
 * The form of encoding used to safely transfer the entity to the user.
 * Currently defined methods are: chunked, compress, deflate, gzip, identity.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Transfer-Encoding: chunked
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Transfer-Encoding general-header field indicates what (if any) type of transformation has been applied to the
 * message body in order to safely transfer it between the sender and the recipient.
 * This differs from the content-coding in that the transfer-coding is a property of the message, not of the entity.
 *
 * <p class='bcode w800'>
 * 	Transfer-Encoding       = "Transfer-Encoding" ":" 1#transfer-coding
 * </p>
 *
 * <p>
 * Transfer-codings are defined in section 3.6. An example is:
 *
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Transfer-Encoding")
public final class TransferEncoding extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns a parsed <c>Transfer-Encoding</c> header.
	 *
	 * @param value The <c>Transfer-Encoding</c> header string.
	 * @return The parsed <c>Transfer-Encoding</c> header, or <jk>null</jk> if the string was null.
	 */
	public static TransferEncoding of(String value) {
		if (value == null)
			return null;
		return new TransferEncoding(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The value for this header.
	 */
	public TransferEncoding(String value) {
		super("Transfer-Encoding", value);
	}
}
