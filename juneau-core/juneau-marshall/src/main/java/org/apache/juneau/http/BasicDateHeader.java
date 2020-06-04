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

import static java.time.format.DateTimeFormatter.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;

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
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@BeanIgnore
public class BasicDateHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final ZonedDateTime zdt;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * @return A new {@link BasicDateHeader} object.
	 */
	public static BasicDateHeader of(String name, String value) {
		return new BasicDateHeader(name, value);
	}

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	public BasicDateHeader(String name, String value) {
		super(name, value);
		this.zdt = value == null ? null : ZonedDateTime.from(RFC_1123_DATE_TIME.parse(value));
	}

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The header value.
	 */
	public BasicDateHeader(String name, Calendar value) {
		this(name, asZdt(value));
	}

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The header value.
	 */
	public BasicDateHeader(String name, ZonedDateTime value) {
		super(name, value == null ? null : RFC_1123_DATE_TIME.format(value));
		this.zdt = value;
	}

	/**
	 * Returns this header value as a {@link java.util.Calendar}.
	 *
	 * @return This header value as a {@link java.util.Calendar}, or <jk>null</jk> if the header could not be parsed.
	 */
	public Calendar asCalendar() {
		return zdt == null ? null : GregorianCalendar.from(zdt);
	}

	/**
	 * Returns this header value as a {@link java.util.Date}.
	 *
	 * @return This header value as a {@link java.util.Date}, or <jk>null</jk> if the header could not be parsed.
	 */
	public java.util.Date asDate() {
		Calendar c = asCalendar();
		return c == null ? null : c.getTime();
	}

	/**
	 * Returns this header value as a {@link java.util.Calendar}.
	 *
	 * @return This header value as a {@link java.util.Calendar}, or <jk>null</jk> if the header could not be parsed.
	 */
	public ZonedDateTime asZonedDateTime() {
		return zdt;
	}

	private static ZonedDateTime asZdt(Calendar o) {
		if (o == null)
			return null;
		if (o instanceof GregorianCalendar)
			return ((GregorianCalendar)o).toZonedDateTime();
		return o.toInstant().atZone(ZoneId.systemDefault());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions.
	//------------------------------------------------------------------------------------------------------------------

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
	public FluentDateAssertion<BasicDateHeader> assertThat() {
		return new FluentDateAssertion<>(asDate(), this);
	}
}
