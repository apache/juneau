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

import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;

/**
 * Category of headers that consist of a comma-delimited list of entity validator values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	If-Match: "xyzzy"
 * 	If-Match: "xyzzy", "r2d2xxxx", "c3piozzzz"
 * 	If-Match: *
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class BasicEntityValidatorArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityValidator} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicEntityValidatorArrayHeader} object.
	 */
	public static BasicEntityValidatorArrayHeader of(String name, Object value) {
		return new BasicEntityValidatorArrayHeader(name, value);
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
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityValidator} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicEntityValidatorArrayHeader} object.
	 */
	public static BasicEntityValidatorArrayHeader of(String name, Supplier<?> value) {
		return new BasicEntityValidatorArrayHeader(name, value);
	}

	private List<EntityValidator> parsed;

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityValidator} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicEntityValidatorArrayHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		return StringUtils.join(getParsedValue(), ',');
	}

	/**
	 * Returns this header value as an array of {@link EntityValidator} objects.
	 *
	 * @return this header value as an array of {@link EntityValidator} objects.
	 */
	public List<EntityValidator> asValidators() {
		return getParsedValue();
	}

	private List<EntityValidator> getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof EntityValidator[])
			return AList.of((EntityValidator[])o).unmodifiable();

		AList<EntityValidator> l = AList.of();
		if (o instanceof Collection) {
			for (Object o2 : (Collection<?>)o)
				l.add(convert(o2));
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				l.add(convert(Array.get(o, i)));
		} else {
			for (String s : split(o.toString()))
				l.add(convert(s));
		}
		return l.unmodifiable();
	}

	private EntityValidator convert(Object o) {
		if (o == null)
			return null;
		if (o instanceof EntityValidator)
			return (EntityValidator)o;
		return new EntityValidator(o.toString());
	}
}
