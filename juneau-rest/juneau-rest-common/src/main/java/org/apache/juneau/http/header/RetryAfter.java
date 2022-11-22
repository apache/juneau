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

import static java.time.format.DateTimeFormatter.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Retry-After</l> HTTP response header.
 *
 * <p>
 * If an entity is temporarily unavailable, this instructs the client to try again later.
 * Value could be a specified period of time (in seconds) or a HTTP-date.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
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
 * <p class='bcode'>
 * 	Retry-After  = "Retry-After" ":" ( HTTP-date | delta-seconds )
 * </p>
 *
 * <p>
 * Two examples of its use are
 * <p class='bcode'>
 * 	Retry-After: Fri, 31 Dec 1999 23:59:59 GMT
 * 	Retry-After: 120
 * </p>
 *
 * <p>
 * In the latter example, the delay is 2 minutes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Retry-After")
public class RetryAfter extends BasicDateHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Retry-After";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>) or an integer.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static RetryAfter of(String value) {
		return value == null ? null : new RetryAfter(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static RetryAfter of(ZonedDateTime value) {
		return value == null ? null : new RetryAfter(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static RetryAfter of(Integer value) {
		return value == null ? null : new RetryAfter(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link Integer} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static RetryAfter of(Supplier<?> value) {
		return value == null ? null : new RetryAfter(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Integer value;
	private final Supplier<?> supplier;

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>) or an integer.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public RetryAfter(String value) {
		super(NAME, isNumeric(value) ? null : value);
		this.value = isNumeric(value) ? Integer.parseInt(value) : null;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public RetryAfter(ZonedDateTime value) {
		super(NAME, value);
		this.value = null;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public RetryAfter(Integer value) {
		super(NAME, (String)null);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link Integer} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public RetryAfter(Supplier<?> value) {
		super(NAME, (String)null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null) {
			Object o = supplier.get();
			if (o == null)
				return null;
			if (o instanceof Integer) {
				return o.toString();
			} else if (o instanceof ZonedDateTime) {
				return RFC_1123_DATE_TIME.format((ZonedDateTime)o);
			}
			throw new BasicRuntimeException("Invalid object type returned by supplier: {0}", className(o));
		}
		if (value != null)
			return stringify(value);
		return super.getValue();
	}

	/**
	 * Returns this header value as an integer.
	 *
	 * @return This header value as a integer, or an empty optional if value was <jk>null</jk> or not an integer.
	 */
	public Optional<Integer> asInteger() {
		if (supplier != null) {
			Object o = supplier.get();
			return optional(o instanceof Integer ? (Integer)o : null);
		}
		return optional(value);
	}
}
