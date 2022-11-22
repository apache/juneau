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

import java.time.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>If-Unmodified-Since</l> HTTP request header.
 *
 * <p>
 * Only send the response if the entity has not been modified since a specific time.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The If-Unmodified-Since request-header field is used with a method to make it conditional.
 * If the requested resource has not been modified since the time specified in this field, the server SHOULD perform the
 * requested operation as if the If-Unmodified-Since header were not present.
 *
 * <p>
 * If the requested variant has been modified since the specified time, the server MUST NOT perform the requested
 * operation, and MUST return a 412 (Precondition Failed).
 *
 * <p class='bcode'>
 * 	If-Unmodified-Since = "If-Unmodified-Since" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example of the field is:
 * <p class='bcode'>
 * 	If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <p>
 * If the request normally (i.e., without the If-Unmodified-Since header) would result in anything other than a 2xx or
 * 412 status, the If-Unmodified-Since header SHOULD be ignored.
 *
 * <p>
 * If the specified date is invalid, the header is ignored.
 *
 * <p>
 * The result of a request having both an If-Unmodified-Since header field and either an If-None-Match or an
 * If-Modified-Since header fields is undefined by this specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("If-Unmodified-Since")
public class IfUnmodifiedSince extends BasicDateHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "If-Unmodified-Since";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfUnmodifiedSince of(String value) {
		return value == null ? null : new IfUnmodifiedSince(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfUnmodifiedSince of(ZonedDateTime value) {
		return value == null ? null : new IfUnmodifiedSince(value);
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
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static IfUnmodifiedSince of(Supplier<ZonedDateTime> value) {
		return value == null ? null : new IfUnmodifiedSince(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfUnmodifiedSince(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfUnmodifiedSince(ZonedDateTime value) {
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
	public IfUnmodifiedSince(Supplier<ZonedDateTime> value) {
		super(NAME, value);
	}
}
