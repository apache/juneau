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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;

/**
 * Category of headers that consist of a comma-delimited list of string values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Allow: GET, PUT
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicCsvHeader extends BasicHeader {

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
	 * 	<br>Must be a comma-delimited list.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicCsvHeader of(String name, String value) {
		return value == null ? null : new BasicCsvHeader(name, value);
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
	public static BasicCsvHeader of(String name, String...value) {
		return value == null ? null : new BasicCsvHeader(name, value);
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
	public static BasicCsvHeader of(String name, Supplier<String[]> value) {
		return value == null ? null : new BasicCsvHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String[] value;
	private final Supplier<String[]> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicCsvHeader(String name, String value) {
		super(name, value);
		this.value = split(value);
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
	public BasicCsvHeader(String name, String...value) {
		super(name, join(value, ", "));
		this.value = copyOf(value);
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
	public BasicCsvHeader(String name, Supplier<String[]> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		return join(value(), ", ");
	}

	/**
	 * Returns the header value as an array wrapped in an {@link Optional}.
	 *
	 * <p>
	 * The array is a copy of the value of this header.
	 *
	 * @return The header value as an array wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String[]> asArray() {
		return optional(copyOf(value()));
	}

	/**
	 * Returns the header value as an array.
	 *
	 * <p>
	 * The array is a copy of the value of this header.
	 *
	 * @return The header value as an array.  Can be <jk>null</jk>.
	 */
	public String[] toArray() {
		return copyOf(value());
	}

	/**
	 * Returns the header value as a list wrapped in an {@link Optional}.
	 *
	 * <p>
	 * The list is unmodifiable.
	 *
	 * @return The header value as a list wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<List<String>> asList() {
		return optional(ulist(value()));
	}

	/**
	 * Returns the header value as a list.
	 *
	 * <p>
	 * The list is unmodifiable.
	 *
	 * @return The header value as a list.  Can be <jk>null</jk>.
	 */
	public List<String> toList() {
		return ulist(value());
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean contains(String val) {
		if (value != null)
			for (String v : value)
				if (eq(v, val))
					return true;
		if (supplier != null) {
			String[] value2 = supplier.get();
			if (value2 != null)
				for (String v : supplier.get())
					if (eq(v, val))
						return true;

		}
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean containsIgnoreCase(String val) {
		if (value != null)
			for (String v : value)
				if (eqic(v, val))
					return true;
		if (supplier != null) {
			String[] value2 = supplier.get();
			if (value2 != null)
				for (String v : supplier.get())
					if (eqic(v, val))
						return true;
		}
		return false;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this header.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentListAssertion<String,BasicCsvHeader> assertList() {
		return new FluentListAssertion<>(ulist(value()), this);
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
		return x != null ? x : other;
	}

	private String[] value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
