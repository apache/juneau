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

import java.time.*;
import java.util.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Expires</l> HTTP response header.
 *
 * <p>
 * Gives the date/time after which the response is considered stale (in "HTTP-date" format as defined by RFC 7231).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Expires: Thu, 01 Dec 1994 16:00:00 GMT
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Expires entity-header field gives the date/time after which the response is considered stale.
 * A stale cache entry may not normally be returned by a cache (either a proxy cache or a user agent cache) unless it is
 * first validated with the origin server
 * (or with an intermediate cache that has a fresh copy of the entity).
 * See section 13.2 for further discussion of the expiration model.
 *
 * <p>
 * The presence of an Expires field does not imply that the original resource will change or cease to exist at, before,
 * or after that time.
 *
 * <p>
 * The format is an absolute date and time as defined by HTTP-date in section 3.3.1; it MUST be in RFC 1123 date format:
 *
 * <p class='bcode w800'>
 * 	Expires = "Expires" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example of its use is...
 * <p class='bcode w800'>
 * 	Expires: Thu, 01 Dec 1994 16:00:00 GMT
 * </p>
 *
 * <p>
 * Note: if a response includes a Cache-Control field with the max-age directive (see section 14.9.3), that directive
 * overrides the Expires field.
 *
 * <p>
 * HTTP/1.1 clients and caches MUST treat other invalid date formats, especially including the value "0", as in the past
 * (i.e., "already expired").
 *
 * <p>
 * To mark a response as "already expired," an origin server sends an Expires date that is equal to the Date header
 * value.
 * (See the rules for expiration calculations in section 13.2.4.)
 *
 * <p>
 * To mark a response as "never expires," an origin server sends an Expires date approximately one year from the time
 * the response is sent.
 * HTTP/1.1 servers SHOULD NOT send Expires dates more than one year in the future.
 *
 * <p>
 * The presence of an Expires header field with a date value of some time in the future on a response that otherwise
 * would by default be non-cacheable indicates that the response is cacheable, unless indicated otherwise by a
 * Cache-Control header field (section 14.9).
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Expires")
public final class Expires extends BasicDateHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Returns a parsed <c>Expires</c> header.
	 *
	 * @param value The <c>Expires</c> header string.
	 * @return The parsed <c>Expires</c> header, or <jk>null</jk> if the string was null.
	 */
	public static Expires of(String value) {
		if (value == null)
			return null;
		return new Expires(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value Header value in RFC1123 format.
	 */
	public Expires(String value) {
		super("Expires", value);
	}

	/**
	 * Constructor.
	 *
	 * @param value Header value.
	 */
	public Expires(Calendar value) {
		super("Expires", value);
	}

	/**
	 * Constructor.
	 *
	 * @param value Header value.
	 */
	public Expires(ZonedDateTime value) {
		super("Expires", value);
	}
}
