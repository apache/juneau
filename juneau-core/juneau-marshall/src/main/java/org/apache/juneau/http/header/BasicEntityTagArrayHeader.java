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
import static java.util.Optional.*;
import static java.util.Collections.*;

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
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
public class BasicEntityTagArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicEntityTagArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicEntityTagArrayHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicEntityTagArrayHeader(name, value);
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
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link BasicEntityTagArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicEntityTagArrayHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicEntityTagArrayHeader(name, value);
	}

	private List<EntityTag> parsed;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicEntityTagArrayHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o instanceof String)
			return (String)o;
		return StringUtils.join(asEntityTags().orElse(emptyList()), ',');
	}

	/**
	 * Returns this header value as an array of {@link EntityTag} objects.
	 *
	 * @return this header value as an array of {@link EntityTag} objects, or {@link Optional#empty()} if the value was <jk>null</jk>..
	 */
	public Optional<List<EntityTag>> asEntityTags() {
		return ofNullable(getParsedValue());
	}

	private List<EntityTag> getParsedValue() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof EntityTag[])
			return AList.of((EntityTag[])o).unmodifiable();

		AList<EntityTag> l = AList.create();
		if (o instanceof Collection) {
			for (Object o2 : (Collection<?>)o)
				l.add(EntityTag.of(o2));
		} else if (o.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(o); i++)
				l.add(EntityTag.of(Array.get(o, i)));
		} else {
			for (String s : split(o.toString()))
				l.add(EntityTag.of(s));
		}
		return l.unmodifiable();
	}
}
