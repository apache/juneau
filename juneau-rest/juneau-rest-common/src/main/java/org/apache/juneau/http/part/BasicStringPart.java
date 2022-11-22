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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;

/**
 * A {@link NameValuePair} that consists of a single string value.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public class BasicStringPart extends BasicPart {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return A new {@link BasicStringPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicStringPart of(String name, String value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringPart(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Part value is re-evaluated on each call to {@link NameValuePair#getValue()}.
	 *
	 * @param name The part name.
	 * @param value The part value supplier.
	 * @return A new {@link BasicStringPart} object, or <jk>null</jk> if the name or supplier is <jk>null</jk>.
	 */
	public static BasicStringPart of(String name, Supplier<String> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringPart(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String value;
	private final Supplier<String> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value.  Can be <jk>null</jk>.
	 */
	public BasicStringPart(String name, String value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value supplier.  Can be <jk>null</jk> or supply <jk>null</jk>.
	 */
	public BasicStringPart(String name, Supplier<String> value) {
		super(name, value);
		this.value = null;
		this.supplier = value;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this part.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentStringAssertion<BasicStringPart> assertString() {
		return new FluentStringAssertion<>(value(), this);
	}

	@Override /* Header */
	public String getValue() {
		return value();
	}

	/**
	 * Returns The part value as a {@link String} wrapped in an {@link Optional}.
	 *
	 * @return The part value as a {@link String} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> asString() {
		return optional(value());
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asString().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public String orElse(String other) {
		String x = value();
		return x != null ? x : other;
	}

	private String value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
