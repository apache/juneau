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
package org.apache.juneau.httppart;

/**
 * Valid values for the <c>format</c> field.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public enum HttpPartFormat {

	/**
	 * Signed 32 bits.
	 */
	INT32,

	/**
	 * Signed 64 bits.
	 */
	INT64,

	/**
	 * 32-bit floating point number.
	 */
	FLOAT,

	/**
	 * 64-bit floating point number.
	 */
	DOUBLE,

	/**
	 * BASE-64 encoded characters.
	 */
	BYTE,

	/**
	 * Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 */
	BINARY,

	/**
	 * Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 */
	BINARY_SPACED,

	/**
	 * An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 */
	DATE,

	/**
	 *  An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 */
	DATE_TIME,

	/**
	 * Used to hint UIs the input needs to be obscured.
	 */
	PASSWORD,

	/**
	 * UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>).
	 */
	UON,

	/**
	 * Not specified.
	 */
	NO_FORMAT;

	/**
	 * Create from lowercase dashed name.
	 *
	 * @param value The enum name.
	 * @return The enum.
	 */
	public static HttpPartFormat fromString(String value) {
		value = value.toUpperCase().replace('-','_');
		return valueOf(value);
	}

	/**
	 * Returns <jk>true</jk> if this format is in the provided list.
	 *
	 * @param list The list of formats to check against.
	 * @return <jk>true</jk> if this format is in the provided list.
	 */
	public boolean isOneOf(HttpPartFormat...list) {
		for (HttpPartFormat ff : list)
			if (this == ff)
				return true;
		return false;
	}

	@Override /* Object */
	public String toString() {
		String s = name().toLowerCase().replace('_','-');
		return s;
	}
}