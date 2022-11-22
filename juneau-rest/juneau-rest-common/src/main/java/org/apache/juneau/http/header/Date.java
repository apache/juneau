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
 * Represents a parsed <l>Date</l> HTTP request/response header.
 *
 * <p>
 * The date and time that the message was sent (in "HTTP-date" format as defined by RFC 7231).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Date: Tue, 15 Nov 1994 08:12:31 GMT
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Date general-header field represents the date and time at which the message was originated, having the same
 * semantics as orig-date in RFC 822.
 * The field value is an HTTP-date, as described in section 3.3.1; it MUST be sent in RFC 1123 [8]-date format.
 * <p class='bcode'>
 * 	Date  = "Date" ":" HTTP-date
 * </p>
 *
 * <p>
 * An example is...
 * <p class='bcode'>
 * 	Date: Tue, 15 Nov 1994 08:12:31 GMT
 * </p>
 *
 * <p>
 * Origin servers MUST include a Date header field in all responses, except in these cases:
 * <ol>
 * 	<li>If the response status code is 100 (Continue) or 101 (Switching Protocols), the response MAY include a Date
 * 		header field, at the server's option.
 * 	<li>If the response status code conveys a server error, e.g. 500 (Internal Server Error) or 503 (Service
 * 		Unavailable), and it is inconvenient or impossible to generate a valid Date.
 * 	<li>If the server does not have a clock that can provide a reasonable approximation of the current time, its
 * 		responses MUST NOT include a Date header field.
 * 		In this case, the rules in section 14.18.1 MUST be followed.
 * </ol>
 *
 * <p>
 * A received message that does not have a Date header field MUST be assigned one by the recipient if the message will
 * be cached by that recipient or gatewayed via a protocol which requires a Date.
 * An HTTP implementation without a clock MUST NOT cache responses without revalidating them on every use.
 * An HTTP cache, especially a shared cache, SHOULD use a mechanism, such as NTP, to synchronize its clock with a
 * reliable external standard.
 *
 * <p>
 * Clients SHOULD only send a Date header field in messages that include an entity-body, as in the case of the PUT and
 * POST requests, and even then it is optional.
 * A client without a clock MUST NOT send a Date header field in a request.
 *
 * <p>
 * The HTTP-date sent in a Date header SHOULD NOT represent a date and time subsequent to the generation of the message.
 * It SHOULD represent the best available approximation of the date and time of message generation, unless the
 * implementation has no means of generating a reasonably accurate date and time.
 * In theory, the date ought to represent the moment just before the entity is generated.
 * In practice, the date can be generated at any time during the message origination without affecting its semantic
 * value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Date")
public class Date extends BasicDateHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Date";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static Date of(String value) {
		return value == null ? null : new Date(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static Date of(ZonedDateTime value) {
		return value == null ? null : new Date(value);
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
	public static Date of(Supplier<ZonedDateTime> value) {
		return value == null ? null : new Date(value);
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
	public Date(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Date(ZonedDateTime value) {
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
	public Date(Supplier<ZonedDateTime> value) {
		super(NAME, value);
	}
}
