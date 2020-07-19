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

import java.util.function.*;

import org.apache.juneau.http.*;

/**
 * Category of headers that consist of a single entity validator value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	ETag: "xyzzy"
 * 	ETag: W/"xyzzy"
 * 	ETag: ""
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
public class BasicEntityTagHeader extends BasicHeader {

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
	 * @return A new {@link BasicEntityTagHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicEntityTagHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicEntityTagHeader(name, value);
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
	 * 		<li><c>String</c> - A raw entity validator values (e.g. <js>"\"xyzzy\""</js>).
	 * 		<li>An {@link EntityTag} object.
	 * 	</ul>
	 * @return A new {@link BasicEntityTagHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicEntityTagHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicEntityTagHeader(name, value);
	}

	private EntityTag parsed;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A raw entity validator values (e.g. <js>"\"xyzzy\""</js>).
	 * 		<li>An {@link EntityTag} object.
	 * 	</ul>
	 */
	public BasicEntityTagHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = getParsedValue();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o instanceof String)
			return (String)o;
		return stringify(asEntityTag());
	}

	/**
	 * Returns this header as an {@link EntityTag}.
	 *
	 * @return This header as an {@link EntityTag}.
	 */
	public EntityTag asEntityTag() {
		return getParsedValue();
	}

	private EntityTag getParsedValue() {
		if (parsed != null)
			return parsed;
		return EntityTag.of(getRawValue());
	}
}
