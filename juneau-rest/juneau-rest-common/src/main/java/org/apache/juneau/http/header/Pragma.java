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
 * Represents a parsed <l>Pragma</l> HTTP request/response header.
 *
 * <p>
 * Implementation-specific fields that may have various effects anywhere along the request-response chain.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Pragma: no-cache
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Pragma general-header field is used to include implementation- specific directives that might apply to any
 * recipient along the request/response chain.
 * All pragma directives specify optional behavior from the viewpoint of the protocol; however, some systems MAY
 * require that behavior be consistent with the directives.
 *
 * <p class='bcode'>
 * 	Pragma            = "Pragma" ":" 1#pragma-directive
 * 	pragma-directive  = "no-cache" | extension-pragma
 * 	extension-pragma  = token [ "=" ( token | quoted-string ) ]
 * </p>
 *
 * <p>
 * When the no-cache directive is present in a request message, an application SHOULD forward the request toward the
 * origin server even if it has a cached copy of what is being requested.
 * This pragma directive has the same semantics as the no-cache cache-directive (see section 14.9) and is defined here
 * for backward compatibility with HTTP/1.0.
 * Clients SHOULD include both header fields when a no-cache request is sent to a server not known to be HTTP/1.1
 * compliant.
 *
 * <p>
 * Pragma directives MUST be passed through by a proxy or gateway application, regardless of their significance to that
 * application, since the directives might be applicable to all recipients along the request/response chain.
 * It is not possible to specify a pragma for a specific recipient; however, any pragma directive not relevant to a
 * recipient SHOULD be ignored by that recipient.
 *
 * <p>
 * HTTP/1.1 caches SHOULD treat "Pragma: no-cache" as if the client had sent "Cache-Control: no-cache".
 * No new Pragma directives will be defined in HTTP.
 *
 * <p>
 * Note: because the meaning of "Pragma: no-cache as a response header field is not actually specified, it does not
 * provide a reliable replacement for "Cache-Control: no-cache" in a response.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Pragma")
public class Pragma extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Pragma";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Pragma of(String value) {
		return value == null ? null : new Pragma(value);
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
	public static Pragma of(Supplier<String> value) {
		return value == null ? null : new Pragma(value);
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
	public Pragma(String value) {
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
	public Pragma(Supplier<String> value) {
		super(NAME, value);
	}
}
