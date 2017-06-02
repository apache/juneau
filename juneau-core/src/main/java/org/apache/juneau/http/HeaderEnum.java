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

/**
 * Category of headers that consist of a single enum value.
 * <p>
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Accept-Ranges: bytes
 * </p>
 * @param <E> The enum type.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public class HeaderEnum<E extends Enum<E>> {

	private final String value;
	private final E enumValue;

	/**
	 * Constructor.
	 * @param value The raw header value.
	 * @param enumClass The enum class.
	 * @param def The default enum value if the value could not be parsed.
	 */
	protected HeaderEnum(String value, Class<E> enumClass, E def) {
		this.value = value;
		E _enumValue = def;
		try {
			_enumValue = Enum.valueOf(enumClass, value.toUpperCase());
		} catch (Exception e) {
			_enumValue = def;
		}
		this.enumValue = _enumValue;
	}

	/**
	 * Returns <jk>true</jk> if the specified value is the same using {@link String#equalsIgnoreCase(String)}.
	 *
	 * @return <jk>true</jk> if the specified value is the same.
	 */
	public E asEnum() {
		return enumValue;
	}

	/**
	 * Returns this header as a simple string value.
	 * <p>
	 * Functionally equivalent to calling {@link #toString()}.
	 *
	 * @return This header as a simple string.
	 */
	public String asString() {
		return value;
	}

	@Override /* Object */
	public String toString() {
		return value == null ? "" : value;
	}
}
