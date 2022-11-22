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
 * Represents a parsed <l>Expect</l> HTTP request header.
 *
 * <p>
 * Indicates that particular server behaviors are required by the client.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Expect: 100-continue
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Expect request-header field is used to indicate that particular server behaviors are required by the client.
 * <p class='bcode'>
 * 	Expect       =  "Expect" ":" 1#expectation
 * 	expectation  =  "100-continue" | expectation-extension
 * 	expectation-extension =  token [ "=" ( token | quoted-string )
 * 	                         *expect-params ]
 * 	expect-params =  ";" token [ "=" ( token | quoted-string ) ]
 * </p>
 *
 * <p>
 * A server that does not understand or is unable to comply with any of the expectation values in the Expect field of a
 * request MUST respond with appropriate error status.
 * The server MUST respond with a 417 (Expectation Failed) status if any of the expectations cannot be met or, if there
 * are other problems with the request, some other 4xx status.
 *
 * <p>
 * This header field is defined with extensible syntax to allow for future extensions.
 * If a server receives a request containing an Expect field that includes an expectation-extension that it does not
 * support, it MUST respond with a 417 (Expectation Failed) status.
 *
 * <p>
 * Comparison of expectation values is case-insensitive for unquoted tokens (including the 100-continue token), and is
 * case-sensitive for quoted-string expectation-extensions.
 *
 * <p>
 * The Expect mechanism is hop-by-hop: that is, an HTTP/1.1 proxy MUST return a 417 (Expectation Failed) status if it
 * receives a request with an expectation that it cannot meet.
 * However, the Expect request-header itself is end-to-end; it MUST be forwarded if the request is forwarded.
 *
 * <p>
 * Many older HTTP/1.0 and HTTP/1.1 applications do not understand the Expect header.
 *
 * <p>
 * See section 8.2.3 for the use of the 100 (continue) status.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Expect")
public class Expect extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Expect";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Expect of(String value) {
		return value == null ? null : new Expect(value);
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
	public static Expect of(Supplier<String> value) {
		return value == null ? null : new Expect(value);
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
	public Expect(String value) {
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
	public Expect(Supplier<String> value) {
		super(NAME, value);
	}
}
