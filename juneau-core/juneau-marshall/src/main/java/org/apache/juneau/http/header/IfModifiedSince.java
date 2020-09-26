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
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>If-Modified-Since</l> HTTP request header.
 *
 * <p>
 * Allows a 304 Not Modified to be returned if content is unchanged.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The If-Modified-Since request-header field is used with a method to make it conditional:
 * if the requested variant has not been modified since the time specified in this field, an entity will not be returned
 * from the server; instead, a 304 (not modified) response will be returned without any message-body.
 *
 * <p class='bcode w800'>
 * 	If-Modified-Since = "If-Modified-Since" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example of the field is:
 * <p class='bcode w800'>
 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <p>
 * A GET method with an If-Modified-Since header and no Range header requests that the identified entity be transferred
 * only if it has been modified since the date given by the If-Modified-Since header.
 * The algorithm for determining this includes the following cases:
 * <ol>
 * 	<li>If the request would normally result in anything other than a 200 (OK) status, or if the passed
 * 		If-Modified-Since date is invalid, the response is exactly the same as for a normal GET.
 * 		A date which is later than the server's current time is invalid.
 * 	<li>If the variant has been modified since the If-Modified-Since date, the response is exactly the same as for a
 * 		normal GET.
 * 	<li>If the variant has not been modified since a valid If-Modified-Since date, the server SHOULD return a 304
 * 		(Not Modified) response.
 * </ol>
 *
 * <p>
 * The purpose of this feature is to allow efficient updates of cached information with a minimum amount of transaction
 * overhead.
 *
 * <p>
 * Note: The Range request-header field modifies the meaning of If-Modified-Since; see section 14.35 for full details.
 *
 * <p>
 * Note: If-Modified-Since times are interpreted by the server, whose clock might not be synchronized with the client.
 *
 * <p>
 * Note: When handling an If-Modified-Since header field, some servers will use an exact date comparison function,
 * rather than a less-than function, for deciding whether to send a 304 (Not Modified) response.
 * To get best results when sending an If-Modified-Since header field for cache validation, clients are
 * advised to use the exact date string received in a previous Last-Modified header field whenever possible.
 *
 * <p>
 * Note: If a client uses an arbitrary date in the If-Modified-Since header instead of a date taken from the
 * Last-Modified header for the same request, the client should be aware of the fact that this date is interpreted in
 * the server's understanding of time.
 * The client should consider unsynchronized clocks and rounding problems due to the different encodings of time between
 * the client and server.
 * This includes the possibility of race conditions if the document has changed between the time it was first requested
 * and the If-Modified-Since date of a subsequent request, and the possibility of clock-skew-related problems if the
 * If-Modified-Since date is derived from the client's clock without correction to the server's clock.
 * Corrections for different time bases between client and server are at best approximate due to network latency.
 * The result of a request having both an If-Modified-Since header field and either an If-Match or an
 * If-Unmodified-Since header fields is undefined by this specification.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header("If-Modified-Since")
public class IfModifiedSince extends BasicDateHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link IfModifiedSince} object, or <jk>null</jk> if the value was null.
	 */
	public static IfModifiedSince of(Object value) {
		if (value == null)
			return null;
		return new IfModifiedSince(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link IfModifiedSince} object, or <jk>null</jk> if the value was null.
	 */
	public static IfModifiedSince of(Supplier<?> value) {
		if (value == null)
			return null;
		return new IfModifiedSince(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public IfModifiedSince(Object value) {
		super("If-Modified-Since", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public IfModifiedSince(String value) {
		this((Object)value);
	}
}
