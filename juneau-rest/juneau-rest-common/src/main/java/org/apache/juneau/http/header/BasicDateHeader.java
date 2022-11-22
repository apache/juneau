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
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;

/**
 * Category of headers that consist of a single HTTP-date.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicDateHeader extends BasicHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicDateHeader of(String name, String value) {
		return value == null ? null : new BasicDateHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicDateHeader of(String name, ZonedDateTime value) {
		return value == null ? null : new BasicDateHeader(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicDateHeader of(String name, Supplier<ZonedDateTime> value) {
		return value == null ? null : new BasicDateHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ZonedDateTime value;
	private final Supplier<ZonedDateTime> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicDateHeader(String name, String value) {
		super(name, value);
		this.value = isEmpty(value) ? null : ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value.toString())).truncatedTo(SECONDS);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicDateHeader(String name, ZonedDateTime value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicDateHeader(String name, Supplier<ZonedDateTime> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		ZonedDateTime x = value();
		return x == null ? null : RFC_1123_DATE_TIME.format(x);
	}

	/**
	 * Returns the header value as a {@link ZonedDateTime} wrapped in an {@link Optional}.
	 *
	 * @return The header value as a {@link ZonedDateTime} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
		return optional(value());
	}

	/**
	 * Returns the header value as a {@link ZonedDateTime}.
	 *
	 * @return The header value as a {@link ZonedDateTime}.  Can be <jk>null</jk>.
	 */
	public ZonedDateTime toZonedDateTime() {
		return value();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body content is not expired.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Expires"</js>).asDateHeader().assertZonedDateTime().isLessThan(<jk>new</jk> Date());
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentZonedDateTimeAssertion<BasicDateHeader> assertZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(value(), this);
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asZonedDateTime().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public ZonedDateTime orElse(ZonedDateTime other) {
		ZonedDateTime x = value();
		return x != null ? x : other;
	}

	private ZonedDateTime value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
