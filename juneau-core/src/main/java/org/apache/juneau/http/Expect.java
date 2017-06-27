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
 * Represents a parsed <l>Expect</l> HTTP request header.
 * <p>
 * Indicates that particular server behaviors are required by the client.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Expect: 100-continue
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Expect request-header field is used to indicate that particular server behaviors are required by the client.
 * <p class='bcode'>
 * 	Expect       =  "Expect" ":" 1#expectation
 * 	expectation  =  "100-continue" | expectation-extension
 * 	expectation-extension =  token [ "=" ( token | quoted-string )
 * 	                         *expect-params ]
 * 	expect-params =  ";" token [ "=" ( token | quoted-string ) ]
 * </p>
 * <p>
 * A server that does not understand or is unable to comply with any of the expectation values in the Expect field of a
 * request MUST respond with appropriate error status.
 * The server MUST respond with a 417 (Expectation Failed) status if any of the expectations cannot be met or, if there
 * are other problems with the request, some other 4xx status.
 * <p>
 * This header field is defined with extensible syntax to allow for future extensions.
 * If a server receives a request containing an Expect field that includes an expectation-extension that it does not
 * support, it MUST respond with a 417 (Expectation Failed) status.
 * <p>
 * Comparison of expectation values is case-insensitive for unquoted tokens (including the 100-continue token), and is
 * case-sensitive for quoted-string expectation-extensions.
 * <p>
 * The Expect mechanism is hop-by-hop: that is, an HTTP/1.1 proxy MUST return a 417 (Expectation Failed) status if it
 * receives a request with an expectation that it cannot meet.
 * However, the Expect request-header itself is end-to-end; it MUST be forwarded if the request is forwarded.
 * <p>
 * Many older HTTP/1.0 and HTTP/1.1 applications do not understand the Expect header.
 * <p>
 * See section 8.2.3 for the use of the 100 (continue) status.
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
public final class Expect extends HeaderString {

	/**
	 * Returns a parsed <code>Expect</code> header.
	 *
	 * @param value The <code>Expect</code> header string.
	 * @return The parsed <code>Expect</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Expect forString(String value) {
		if (value == null)
			return null;
		return new Expect(value);
	}

	private Expect(String value) {
		super(value);
	}
}
