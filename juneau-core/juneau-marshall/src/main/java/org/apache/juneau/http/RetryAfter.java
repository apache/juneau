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

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Retry-After</l> HTTP response header.
 *
 * <p>
 * If an entity is temporarily unavailable, this instructs the client to try again later.
 * Value could be a specified period of time (in seconds) or a HTTP-date.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Retry-After: 120
 * 	Retry-After: Fri, 07 Nov 2014 23:59:59 GMT
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Retry-After response-header field can be used with a 503 (Service Unavailable) response to indicate how long the
 * service is expected to be unavailable to the requesting client.
 * This field MAY also be used with any 3xx (Redirection) response to indicate the minimum time the user-agent is asked
 * wait before issuing the redirected request.
 * The value of this field can be either an HTTP-date or an integer number of seconds (in decimal) after the time of the
 * response.
 *
 * <p class='bcode w800'>
 * 	Retry-After  = "Retry-After" ":" ( HTTP-date | delta-seconds )
 * </p>
 *
 * <p>
 * Two examples of its use are
 * <p class='bcode w800'>
 * 	Retry-After: Fri, 31 Dec 1999 23:59:59 GMT
 * 	Retry-After: 120
 * </p>
 *
 * <p>
 * In the latter example, the delay is 2 minutes.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Retry-After")
public final class RetryAfter extends BasicStringHeader {

	/**
	 * Returns a parsed <c>Retry-After</c> header.
	 *
	 * @param value The <c>Retry-After</c> header string.
	 * @return The parsed <c>Retry-After</c> header, or <jk>null</jk> if the string was null.
	 */
	public static RetryAfter forString(String value) {
		if (value == null)
			return null;
		return new RetryAfter(value);
	}

	private RetryAfter(String value) {
		super("Retry-After", value);
	}

	/**
	 * Returns this header value as a {@link java.util.Date} object.
	 *
	 * @return This header value as a {@link java.util.Date} object, or <jk>null</jk> if the value is not a date.
	 */
	public java.util.Date asDate() {
		char c0 = charAt(asString(), 0);
		if (c0 >= '0' && c0 <= '9')
			return null;
		return DateUtils.parseDate(toString());
	}

	/**
	 * Returns this header value as an integer.
	 *
	 * @return This header value as a integer, or <c>-1</c> if the value is not an integer.
	 */
	public int asInt() {
		char c0 = charAt(asString(), 0);
		if (c0 >= '0' && c0 <= '9') {
			try {
				return Integer.parseInt(asString());
			} catch (NumberFormatException e) {
				return -1;
			}
		}
		return -1;
	}
}
