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
package org.apache.juneau.http;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.annotation.Items;

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
@Header(type="array",collectionFormat="csv",items=@Items(type="string"))
public class BasicCsvArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final String[] value;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	protected BasicCsvArrayHeader(String name, String[] value) {
		super(name, StringUtils.joine(value, ','));
		this.value = value;
	}

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 */
	public BasicCsvArrayHeader(String name, String value) {
		super(name, value);
		this.value = split(value);
	}

	private static String[] split(String value) {
		return StringUtils.split(value);
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean contains(String val) {
		if (val != null)
			for (String v : value)
				if (val.equals(v))
					return true;
		return false;
	}

	/**
	 * Returns <jk>true</jk> if this header contains the specified value using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @param val The value to check for.
	 * @return <jk>true</jk> if this header contains the specified value.
	 */
	public boolean containsIC(String val) {
		if (val != null)
			for (String v : value)
				if (val.equalsIgnoreCase(v))
					return true;
		return false;
	}
}
