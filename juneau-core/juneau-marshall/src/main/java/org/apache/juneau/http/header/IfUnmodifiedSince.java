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
 * Represents a parsed <l>If-Unmodified-Since</l> HTTP request header.
 *
 * <p>
 * Only send the response if the entity has not been modified since a specific time.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
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
 * <p class='bcode w800'>
 * 	If-Unmodified-Since = "If-Unmodified-Since" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example of the field is:
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("If-Unmodified-Since")
public class IfUnmodifiedSince extends BasicDateHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link IfUnmodifiedSince} object, or <jk>null</jk> if the value was null.
	 */
	public static IfUnmodifiedSince of(Object value) {
		if (value == null)
			return null;
		return new IfUnmodifiedSince(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link IfUnmodifiedSince} object, or <jk>null</jk> if the value was null.
	 */
	public static IfUnmodifiedSince of(Supplier<?> value) {
		if (value == null)
			return null;
		return new IfUnmodifiedSince(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public IfUnmodifiedSince(Object value) {
		super("If-Unmodified-Since", value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The parameter value.
	 */
	public IfUnmodifiedSince(String value) {
		super("If-Unmodified-Since", value);
	}
}
