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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.assertions.*;

/**
 * A {@link NameValuePair} that consists of a comma-delimited list of string values.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class BasicCsvArrayPart extends BasicPart {

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicCsvArrayPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicCsvArrayPart of(String name, Object value) {
		if (isEmpty(name) || value == null)
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
	 * @param value
	 * 	The part value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicCsvArrayPart} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicCsvArrayPart of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicCsvArrayPart(name, value);
	}

	private List<String> parsed;

	/**
	 * Constructor.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
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
	public BasicCsvArrayPart(String name, Object value) {
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
	 * Returns <jk>true</jk> if this part contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this part contains the specified value.
	 */
	public boolean contains(String val) {
		List<String> vv = getParsedValue();
		if (val != null && vv != null)
			for (String v : vv)
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
		List<String> vv = getParsedValue();
		if (val != null && vv != null)
			for (String v : vv)
				if (eqic(v, val))
					return true;
		return false;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this part.
	 *
	 * @return A new fluent assertion object.
	 * @throws AssertionError If assertion failed.
	 */
	public FluentListAssertion<String,BasicCsvArrayPart> assertList() {
		return new FluentListAssertion<>(getParsedValue(), this);
	}

	/**
	 * Returns the contents of this part as a list of strings.
	 *
	 * @return The contents of this part as an unmodifiable list of strings, or {@link Optional#empty()} if the value was <jk>null</jk>.
	 */
	public Optional<List<String>> asList() {
		List<String> l = getParsedValue();
		return optional(unmodifiable(l));
	}

	private List<String> getParsedValue() {
		if (parsed != null)
			return parsed;

		Object o = getRawValue();
		if (o == null)
			return null;

		List<String> l = list();
		if (o instanceof Collection) {
			for (Object o2 : (Collection<?>)o)
				l.add(stringify(o2));
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				l.add(stringify(Array.get(o, i)));
		} else {
			split(o.toString(), x -> l.add(x));
		}
		return unmodifiable(l);
	}
}
