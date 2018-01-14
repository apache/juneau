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
 * Represents a parsed <l>Transfer-Encoding</l> HTTP response header.
 * 
 * <p>
 * The form of encoding used to safely transfer the entity to the user.
 * Currently defined methods are: chunked, compress, deflate, gzip, identity.
 * 
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Transfer-Encoding: chunked
 * </p>
 * 
 * <h6 class='topic'>RFC2616 Specification</h6>
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
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class TransferEncoding extends HeaderString {

	/**
	 * Returns a parsed <code>Transfer-Encoding</code> header.
	 * 
	 * @param value The <code>Transfer-Encoding</code> header string.
	 * @return The parsed <code>Transfer-Encoding</code> header, or <jk>null</jk> if the string was null.
	 */
	public static TransferEncoding forString(String value) {
		if (value == null)
			return null;
		return new TransferEncoding(value);
	}

	private TransferEncoding(String value) {
		super(value);
	}
}
