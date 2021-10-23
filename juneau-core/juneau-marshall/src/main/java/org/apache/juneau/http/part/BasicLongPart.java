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
package org.apache.juneau.http.part;

import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;

/**
 * A {@link NameValuePair} that consists of a single long value.
 */
public class BasicLongPart extends BasicPart {

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicLongPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicLongPart of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicLongPart(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Part value is re-evaluated on each call to {@link NameValuePair#getValue()}.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicLongPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicLongPart of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicLongPart(name, value);
	}

	private Long parsed;

	/**
	 * Constructor.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicLongPart(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		return stringify(getParsedValue());
	}

	/**
	 * Returns The part value as a long.
	 *
	 * @return The part value as a long, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<Long> asLong() {
		return ofNullable(getParsedValue());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this part.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentLongAssertion<BasicLongPart> assertLong() {
		return new FluentLongAssertion<>(getParsedValue(), this);
	}

	private Long getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof Number)
			return ((Number)o).longValue();
		String s = o.toString();
		if (isEmpty(s))
			return null;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw illegalArgumentException("Value could not be parsed as a long: {0}", o);
		}
	}
}
