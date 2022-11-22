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
 * Represents a parsed <l>Trailer</l> HTTP response header.
 *
 * <p>
 * The Trailer general field value indicates that the given set of header fields is present in the trailer of a message
 * encoded with chunked transfer coding.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Trailer: Max-Forwards
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Trailer general field value indicates that the given set of header fields is present in the trailer of a message
 * encoded with chunked transfer-coding.
 *
 * <p class='bcode'>
 * 	Trailer  = "Trailer" ":" 1#field-name
 * </p>
 *
 * <p>
 * An HTTP/1.1 message SHOULD include a Trailer header field in a message using chunked transfer-coding with a non-empty
 * trailer.
 * Doing so allows the recipient to know which header fields to expect in the trailer.
 *
 * <p>
 * If no Trailer header field is present, the trailer SHOULD NOT include any header fields.
 * See section 3.6.1 for restrictions on the use of trailer fields in a "chunked" transfer-coding.
 *
 * <p>
 * Message header fields listed in the Trailer header field MUST NOT include the following header fields:
 * <ul>
 * 	<li>Transfer-Encoding
 * 	<li>Content-Length
 * 	<li>Trailer
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Trailer")
public class Trailer extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Trailer";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Trailer of(String value) {
		return value == null ? null : new Trailer(value);
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
	public static Trailer of(Supplier<String> value) {
		return value == null ? null : new Trailer(value);
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
	public Trailer(String value) {
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
	public Trailer(Supplier<String> value) {
		super(NAME, value);
	}
}
