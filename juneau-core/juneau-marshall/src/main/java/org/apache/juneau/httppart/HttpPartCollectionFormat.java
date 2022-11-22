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
 * Valid values for the <c>collectionFormat</c> field.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public enum HttpPartCollectionFormat {

	/**
	 * Comma-separated values (e.g. <js>"foo,bar"</js>).
	 */
	CSV,

	/**
	 * Space-separated values (e.g. <js>"foo bar"</js>).
	 */
	SSV,

	/**
	 * Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 */
	TSV,

	/**
	 * Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 */
	PIPES,

	/**
	 * Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 */
	MULTI,

	/**
	 * UON collection notation (e.g. <js>"@(foo,bar)"</js>).
	 */
	UONC,

	/**
	 * Not specified.
	 */
	NO_COLLECTION_FORMAT;

	/**
	 * Create from lowercase string.
	 *
	 * @param value The enum name.
	 * @return The enum.
	 */
	public static HttpPartCollectionFormat fromString(String value) {
		if (value == null)
			return null;
		if (value.equalsIgnoreCase("UON"))
			return UONC;
		return valueOf(value.toUpperCase());
	}

	@Override /* Object */
	public String toString() {
		return name().toLowerCase();
	}
}