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
import static java.util.Collections.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;

/**
 * Category of headers that consist of a comma-delimited list of string values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Allow: GET, PUT
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
public class BasicCsvArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader of(String name, String value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayHeader(name, value);
	}

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader of(String name, List<String> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayHeader(name, value);
	}

	/**
	 * Convenience creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader of(String name, Supplier<List<String>> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayHeader(name, value);
	}

	private final List<String> value;
	private final Supplier<List<String>> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public BasicCsvArrayHeader(String name, String value) {
		super(name, value);
		this.value = parse(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public BasicCsvArrayHeader(String name, List<String> value) {
		super(name, serialize(value));
		this.value = value == null ? null : unmodifiableList(value);
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
	 */
	public BasicCsvArrayHeader(String name, Supplier<List<String>> value) {
		super(name, null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null)
			return serialize(supplier.get());
		return super.getValue();
	}

	/**
	 * Returns the contents of this header as a list of strings.
	 *
	 * @return The contents of this header as an unmodifiable list of strings, or {@link Optional#empty()} if the value was <jk>null</jk>.
	 */
	public Optional<List<String>> asList() {
		if (value != null)
			return ofNullable(value);
		if (supplier != null)
			return ofNullable(supplier.get());
		return empty();
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean contains(String val) {
		Optional<List<String>> o = asList();
		if (val != null && o.isPresent()) {
			List<String> l = o.get();
			for (int i = 0, j = l.size(); i < j; i++)
				if (eq(val, l.get(i)))
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
		Optional<List<String>> o = asList();
		if (val != null && o.isPresent()) {
			List<String> l = o.get();
			for (int i = 0, j = l.size(); i < j; i++)
				if (eqic(val, l.get(i)))
					return true;
		}
		return false;
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
	 * 		.(<js>"Expires"</js>).assertThat().isLessThan(<jk>new</jk> Date());
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentListAssertion<BasicCsvArrayHeader> assertList() {
		return new FluentListAssertion<>(asList().orElse(null), this);
	}

	private static String serialize(List<String> value) {
		return join(value, ", ");
	}

	private List<String> parse(String value) {
		return value == null ? null : unmodifiableList(Arrays.asList(split(value)));
	}
}
