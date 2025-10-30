/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.part;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.common.utils.Utils.eqic;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.common.utils.*;

/**
 * A {@link NameValuePair} that consists of a comma-delimited list of string values.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
public class BasicCsvArrayPart extends BasicPart {
	private static final String[] EMPTY = {};

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return A new {@link BasicCsvArrayPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicCsvArrayPart of(String name, String...value) {
		if (StringUtils.isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayPart(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Part value is re-evaluated on each call to {@link NameValuePair#getValue()}.
	 *
	 * @param name The part name.
	 * @param value The part value supplier.
	 * @return A new {@link BasicCsvArrayPart} object, or <jk>null</jk> if the name or supplier is <jk>null</jk>.
	 */
	public static BasicCsvArrayPart of(String name, Supplier<String[]> value) {
		if (StringUtils.isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayPart(name, value);
	}

	private final String[] value;
	private final Supplier<String[]> supplier;
	private String stringValue;

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value.  Can be <jk>null</jk>.
	 */
	public BasicCsvArrayPart(String name, String...value) {
		super(name, value);
		this.value = value;
		this.supplier = null;
		this.stringValue = null;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * <jk>null</jk> values are treated as <jk>null</jk>.
	 * Otherwise parses as a comma-delimited list with whitespace trimmed.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value.  Can be <jk>null</jk>.
	 */
	public BasicCsvArrayPart(String name, String value) {
		super(name, value);
		this.value = splita(value);
		this.supplier = null;
		this.stringValue = value;
	}

	/**
	 * Constructor.
	 *
	 * @param name The part name.  Must not be <jk>null</jk>.
	 * @param value The part value supplier.  Can be <jk>null</jk> or supply <jk>null</jk>.
	 */
	public BasicCsvArrayPart(String name, Supplier<String[]> value) {
		super(name, value);
		this.value = null;
		this.supplier = value;
		this.stringValue = null;
	}

	/**
	 * Returns The part value as an array wrapped in an {@link Optional}.
	 *
	 * <p>
	 * Array is a copy of the value of this part.
	 *
	 * @return The part value as an array wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String[]> asArray() {
		return opt(copyOf(value()));
	}

	/**
	 * Returns The part value as a {@link List} wrapped in an {@link Optional}.
	 *
	 * <p>
	 * The list is unmodifiable.
	 *
	 * @return The part value as a {@link List} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<List<String>> asList() {
		return opt(toList());
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this part.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentListAssertion<String,BasicCsvArrayPart> assertList() {
		return new FluentListAssertion<>(u(l(value())), this);
	}

	/**
	 * Returns <jk>true</jk> if this part contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this part contains the specified value.
	 */
	public boolean contains(String val) {
		if (nn(val))
			for (String v : value())
				if (eq(v, val))
					return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this part contains the specified value using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this part contains the specified value.
	 */
	public boolean containsIgnoreCase(String val) {
		if (nn(val))
			for (String v : value())
				if (eqic(v, val))
					return true;
		return false;
	}

	@Override /* Overridden from Header */
	public String getValue() {
		if (nn(supplier))
			return join(supplier.get(), ',');
		if (nn(stringValue))
			stringValue = join(value, ',');
		return stringValue;
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asArray().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public String[] orElse(String[] other) {
		String[] x = value();
		return nn(x) ? x : other;
	}

	/**
	 * Returns The part value as an array.
	 *
	 * <p>
	 * The array is a copy of the value of this part.
	 *
	 * @return The part value as an array, or <jk>null</jk> if the value <jk>null</jk>.
	 */
	public String[] toArray() {
		return copyOf(value());
	}

	/**
	 * Returns The part value as a {@link List}.
	 *
	 * <p>
	 * The list is unmodifiable.
	 *
	 * @return The part value as a {@link List}, or <jk>null</jk> if the value <jk>null</jk>.
	 */
	public List<String> toList() {
		return u(l(value()));
	}

	private String[] value() {
		if (nn(supplier)) {
			String[] v = supplier.get();
			return nn(v) ? v : EMPTY;
		}
		return value;
	}
}