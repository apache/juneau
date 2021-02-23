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
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;

/**
 * Category of headers that consist of a single HTTP-date.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
public class BasicDateHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicDateHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicDateHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicDateHeader(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicDateHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicDateHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicDateHeader(name, value);
	}

	private ZonedDateTime parsed;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime} - Will be truncated to seconds.
	 * 		<li>{@link Calendar} - Will be truncated to seconds.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicDateHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		return RFC_1123_DATE_TIME.format(getParsedValue());
	}

	/**
	 * Returns this header value as a {@link java.util.Calendar}.
	 *
	 * @return This header value as a {@link java.util.Calendar}, or {@link Optional#empty()} if the header could not be parsed.
	 */
	public Optional<Calendar> asCalendar() {
		ZonedDateTime zdt = getParsedValue();
		return ofNullable(zdt == null ? null : GregorianCalendar.from(zdt));
	}

	/**
	 * Returns this header value as a {@link java.util.Date}.
	 *
	 * @return This header value as a {@link java.util.Date}, or {@link Optional#empty()} if the header could not be parsed.
	 */
	public Optional<java.util.Date> asDate() {
		Calendar c = asCalendar().orElse(null);
		return ofNullable(c == null ? null : c.getTime());
	}

	/**
	 * Returns this header value as a {@link ZonedDateTime}.
	 *
	 * @return This header value as a {@link ZonedDateTime}, or {@link Optional#empty()} if the header could not be parsed.
	 */
	public Optional<ZonedDateTime> asZonedDateTime() {
		return ofNullable(getParsedValue());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body content is not expired.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getDateHeader(<js>"Expires"</js>).assertThat().isLessThan(<jk>new</jk> Date());
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentZonedDateTimeAssertion<BasicDateHeader> assertZonedDateTime() {
		return new FluentZonedDateTimeAssertion<>(getParsedValue(), this);
	}

	private ZonedDateTime getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof ZonedDateTime)
			return ((ZonedDateTime)o).truncatedTo(SECONDS);
		if (o instanceof GregorianCalendar)
			return ((GregorianCalendar)o).toZonedDateTime().truncatedTo(SECONDS);
		return ZonedDateTime.from(RFC_1123_DATE_TIME.parse(o.toString())).truncatedTo(SECONDS);
	}
}
