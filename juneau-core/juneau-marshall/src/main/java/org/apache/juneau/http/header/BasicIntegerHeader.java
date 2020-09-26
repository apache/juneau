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

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;

/**
 * Category of headers that consist of a single integer value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Age: 300
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header(type="integer",format="int32")
public class BasicIntegerHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicIntegerHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicIntegerHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicIntegerHeader(name, value);
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
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicIntegerHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicIntegerHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicIntegerHeader(name, value);
	}

	private Integer parsed;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicIntegerHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		return stringify(asInt());
	}

	/**
	 * Returns the header value as an integer.
	 *
	 * @return The header value as an integer.
	 */
	public Integer asInt() {
		return getParsedValue();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response content is older than 1.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.getHeader(<js>"Age"</js>).asIntegerHeader().assertInteger().isGreaterThan(1);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentIntegerAssertion<BasicIntegerHeader> assertInteger() {
		return new FluentIntegerAssertion<>(asInt(), this);
	}

	private Integer getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof Integer)
			return (Integer)o;
		if (o instanceof Number)
			return ((Number)o).intValue();
		String s = o.toString();
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			try {
				Long.parseLong(s);
				return Integer.MAX_VALUE;
			} catch (NumberFormatException e2) {
				throw new BasicIllegalArgumentException("Value could not be parsed as an int: {0}", o);
			}
		}
	}
}
