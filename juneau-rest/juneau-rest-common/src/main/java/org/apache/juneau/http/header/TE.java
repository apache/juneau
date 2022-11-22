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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>TE</l> HTTP request header.
 *
 * <p>
 * The transfer encodings the user agent is willing to accept: the same values as for the response header field
 * Transfer-Encoding can be used, plus the "trailers" value (related to the "chunked" transfer method) to notify the
 * server it expects to receive additional fields in the trailer after the last, zero-sized, chunk.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	TE: trailers, deflate
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The TE request-header field indicates what extension transfer-codings it is willing to accept in the response and
 * whether or not it is willing to accept trailer fields in a chunked transfer-coding.
 * Its value may consist of the keyword "trailers" and/or a comma-separated list of extension transfer-coding names
 * with optional accept parameters (as described in section 3.6).
 *
 * <p class='bcode'>
 * 	TE        = "TE" ":" #( t-codings )
 * 	t-codings = "trailers" | ( transfer-extension [ accept-params ] )
 * </p>
 *
 * <p>
 * The presence of the keyword "trailers" indicates that the client is willing to accept trailer fields in a chunked
 * transfer-coding, as defined in section 3.6.1.
 * This keyword is reserved for use with transfer-coding values even though it does not itself represent a
 * transfer-coding.
 *
 * <p>
 * Examples of its use are:
 * <p class='bcode'>
 * 	TE: deflate
 * 	TE:
 * 	TE: trailers, deflate;q=0.5
 * </p>
 *
 * <p>
 * The TE header field only applies to the immediate connection.
 * Therefore, the keyword MUST be supplied within a Connection header field (section 14.10) whenever TE is present in
 * an HTTP/1.1 message.
 *
 * <p>
 * A server tests whether a transfer-coding is acceptable, according to a TE field, using these rules:
 * <ol>
 * 	<li>The "chunked" transfer-coding is always acceptable.
 * 		If the keyword "trailers" is listed, the client indicates that it is willing to accept trailer fields in the
 * 		chunked response on behalf of itself and any downstream clients.
 * 		The implication is that, if given, the client is stating that either all downstream clients are willing to accept
 * 		trailer fields in the forwarded response, or that it will attempt to buffer the response on behalf of downstream
 * 		recipients.
 * 		Note: HTTP/1.1 does not define any means to limit the size of a chunked response such that a client can be assured
 * 		of buffering the entire response.
 * 	<li>If the transfer-coding being tested is one of the transfer-codings listed in the TE field, then it is acceptable
 * 		unless it is accompanied by a qvalue of 0. (As defined in section 3.9, a qvalue of 0 means "not acceptable.")
 * 	<li>If multiple transfer-codings are acceptable, then the acceptable transfer-coding with the highest non-zero
 * 		qvalue is preferred.
 * 		The "chunked" transfer-coding always has a qvalue of 1.
 * </ol>
 *
 * <p>
 * If the TE field-value is empty or if no TE field is present, the only transfer-coding is "chunked".
 * A message with no transfer-coding is always acceptable.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("TE")
public class TE extends BasicStringRangesHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "TE";

	private static final Cache<String,TE> CACHE = Cache.of(String.class, TE.class).build();

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static TE of(String value) {
		return value == null ? null : CACHE.get(value, ()->new TE(value));
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static TE of(StringRanges value) {
		return value == null ? null : new TE(value);
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
	public static TE of(Supplier<StringRanges> value) {
		return value == null ? null : new TE(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public TE(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public TE(StringRanges value) {
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
	public TE(Supplier<StringRanges> value) {
		super(NAME, value);
	}
}
