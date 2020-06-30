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

import java.util.function.*;

import org.apache.http.*;

/**
 * Category of headers that consist of multiple parameterized string values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept: application/json;q=0.9,text/xml;q=0.1
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
*/
public class BasicParameterizedArrayHeader extends BasicStringHeader {

	private static final long serialVersionUID = 1L;

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
	 * @return A new {@link BasicParameterizedArrayHeader} object.
	 */
	public static BasicParameterizedArrayHeader of(String name, Object value) {
		return new BasicParameterizedArrayHeader(name, value);
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
	 * @return A new {@link BasicParameterizedArrayHeader} object.
	 */
	public static BasicParameterizedArrayHeader of(String name, Supplier<?> value) {
		return new BasicParameterizedArrayHeader(name, value);
	}

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
	public BasicParameterizedArrayHeader(String name, Object value) {
		super(name, value);
	}

	/**
	 * Returns a parameterized value of the header.
	 *
	 * <p class='bcode w800'>
	 * 	ContentType ct = ContentType.<jsm>of</jsm>(<js>"application/json;charset=foo"</js>);
	 * 	assertEquals(<js>"foo"</js>, ct.getParameter(<js>"charset"</js>);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @return The parameter value, or <jk>null</jk> if the parameter is not present.
	 */
	public String getParameter(String name) {
		HeaderElement[] elements = getElements();
		if (elements.length == 0)
			return null;
		NameValuePair p = elements[0].getParameterByName(name);
		return p == null ? null : p.getValue();
	}
}
