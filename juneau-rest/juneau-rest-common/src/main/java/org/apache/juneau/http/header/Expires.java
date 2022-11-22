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
 * Represents a parsed <l>Expires</l> HTTP response header.
 *
 * <p>
 * Gives the date/time after which the response is considered stale (in "HTTP-date" format as defined by RFC 7231).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
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
 * <p class='bcode'>
 * 	Expires = "Expires" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example of its use is...
 * <p class='bcode'>
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Expires")
public class Expires extends BasicDateHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Expires";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static Expires of(String value) {
		return value == null ? null : new Expires(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static Expires of(ZonedDateTime value) {
		return value == null ? null : new Expires(value);
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
	public static Expires of(Supplier<ZonedDateTime> value) {
		return value == null ? null : new Expires(value);
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
	public Expires(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Expires(ZonedDateTime value) {
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
	public Expires(Supplier<ZonedDateTime> value) {
		super(NAME, value);
	}
}
