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
package org.apache.juneau.http.pair;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.*;

/**
 * A {@link NameValuePair} that consists of a single string value.
*/
public class BasicNamedString extends BasicNameValuePair {

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link BasicNamedString} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicNamedString of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicNamedString(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link BasicNamedString} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicNamedString of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicNamedString(name, value);
	}

	private String parsed;

	/**
	 * Constructor
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicNamedString(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this parameter.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentStringAssertion<BasicNamedString> assertString() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	@Override /* Header */
	public String getValue() {
		return getParsedValue();
	}

	/**
	 * Returns the value of this parameter as a string.
	 *
	 * @return The value of this parameter as a string, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<String> asString() {
		return Optional.ofNullable(getParsedValue());
	}

	/**
	 * Return the value if present, otherwise return other.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, may be <jk>null</jk>.
	 * @return The value, if present, otherwise other.
	 */
	public String orElse(String other) {
		return asString().orElse(other);
	}

	private String getParsedValue() {
		if (parsed != null)
			return parsed;
		return stringify(getRawValue());
	}
}
