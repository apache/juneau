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

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;

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
 * 	<li class='extlink'>{@doc RFC2616}
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
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicCsvArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayHeader(name, value);
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
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicCsvArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayHeader(name, value);
	}

	private List<String> parsed;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicCsvArrayHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o instanceof String)
			return (String)o;
		return joine(getParsedValue(), ',');
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean contains(String val) {
		List<String> vv = getParsedValue();
		if (val != null && vv != null)
			for (String v : vv)
				if (isEquals(v, val))
					return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean containsIc(String val) {
		List<String> vv = getParsedValue();
		if (val != null && vv != null)
			for (String v : vv)
				if (isEqualsIc(v, val))
					return true;
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
		return new FluentListAssertion<>(asList(), this);
	}

	/**
	 * Returns the contents of this header as a list of strings.
	 *
	 * @return The contents of this header as an unmodifiable list of strings, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public List<String> asList() {
		List<String> l = getParsedValue();
		return l == null ? null : Collections.unmodifiableList(l);
	}

	private List<String> getParsedValue() {
		if (parsed != null)
			return parsed;

		Object o = getRawValue();
		if (o == null)
			return null;

		AList<String> l = AList.of();
		if (o instanceof Collection) {
			for (Object o2 : (Collection<?>)o)
				l.add(stringify(o2));
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				l.add(stringify(Array.get(o, i)));
		} else {
			for (String s : split(o.toString()))
				l.add(s);
		}
		return l.unmodifiable();
	}
}
