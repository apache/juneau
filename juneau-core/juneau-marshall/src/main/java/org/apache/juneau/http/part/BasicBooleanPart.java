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

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;

/**
 * A {@link NameValuePair} that consists of a single boolean value.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class BasicBooleanPart extends BasicPart {

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean} - As-is.
	 * 		<li>{@link String} - Parsed using {@link Boolean#parseBoolean(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicBooleanPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicBooleanPart of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicBooleanPart(name, value);
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
	 * 		<li>{@link Boolean} - As-is.
	 * 		<li>{@link String} - Parsed using {@link Boolean#parseBoolean(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicBooleanPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicBooleanPart of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicBooleanPart(name, value);
	}

	private Boolean parsed;

	/**
	 * Constructor.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean} - As-is.
	 * 		<li>{@link String} - Parsed using {@link Boolean#parseBoolean(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicBooleanPart(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* NameValuePair */
	public String getValue() {
		return stringify(getParsedValue());
	}

	/**
	 * Returns The part value as a boolean.
	 *
	 * @return The part value as a boolean.
	 */
	public Optional<Boolean> asBoolean() {
		return ofNullable(getParsedValue());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this part.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentBooleanAssertion<BasicBooleanPart> assertBoolean() {
		return new FluentBooleanAssertion<>(getParsedValue(), this);
	}

	private Boolean getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof Boolean)
			return (Boolean)o;
		String s = o.toString();
		if (isEmpty(s))
			return null;
		return Boolean.parseBoolean(s);
	}
}
