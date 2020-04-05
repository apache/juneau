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
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Ranges: bytes
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 *
 * @param <E> The enum type.
 */
public class BasicEnumHeader<E extends Enum<E>> extends BasicHeader {

	private static final long serialVersionUID = 1L;

	private final E enumValue;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name.
	 * @param value The raw header value.
	 * @param enumClass The enum class.
	 * @param def The default enum value if the value could not be parsed.
	 */
	protected BasicEnumHeader(String name, String value, Class<E> enumClass, E def) {
		super(name, value);
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
}
