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

import static org.apache.juneau.internal.StringUtils.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;

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
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header("Retry-After")
public class RetryAfter extends BasicDateHeader {

	private static final long serialVersionUID = 1L;

	private final Object value;  // Only set if value is an integer.

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link RetryAfter} object.
	 */
	public static RetryAfter of(Object value) {
		if (value == null)
			return null;
		return new RetryAfter(value);
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
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link RetryAfter} object.
	 */
	public static RetryAfter of(Supplier<?> value) {
		if (value == null)
			return null;
		return new RetryAfter(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>{@link Number}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public RetryAfter(Object value) {
		super("Retry-After", dateValue(value));
		this.value = intValue(value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public RetryAfter(String value) {
		this((Object)value);
	}

	private static Object dateValue(Object o) {
		Object o2 = unwrap(o);
		if (o2 == null || isInt(o2))
			return null;
		return o;
	}

	private static Object intValue(Object o) {
		Object o2 = unwrap(o);
		if (o2 == null || isInt(o2))
			return o;
		return null;
	}

	private static boolean isInt(Object o) {
		if (o instanceof Number)
			return true;
		String s = o.toString();
		char c0 = charAt(s, 0);
		return Character.isDigit(c0);
	}

	@Override /* Header */
	public String getValue() {
		if (value == null)
			return super.getValue();
		Object o = unwrap(value);
		return (o == null ? null : o.toString());
	}

	/**
	 * Returns this header value as an integer.
	 *
	 * @return This header value as a integer, or <c>-1</c> if the value is not an integer.
	 */
	public int asInt() {
		if (value != null) {
			Object o = unwrap(value);
			return o == null ? -1 : Integer.parseInt(o.toString());
		}
		return -1;
	}
}
