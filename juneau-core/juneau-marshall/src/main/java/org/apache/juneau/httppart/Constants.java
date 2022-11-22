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
 * HTTP-Part constants.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class Constants {

	//-----------------------------------------------------------------------------------------------------------------
	// CollectionFormat
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Comma-separated values (e.g. <js>"foo,bar"</js>).
	 */
	public static final String CF_CSV = "csv";

	/**
	 * Space-separated values (e.g. <js>"foo bar"</js>).
	 */
	public static final String CF_SSV = "ssv";

	/**
	 * Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 */
	public static final String CF_TSV = "tsv";

	/**
	 * Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 */
	public static final String CF_PIPES = "pipes";

	/**
	 * Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 */
	public static final String CF_MULTI = "multi";

	/**
	 * UON notation (e.g. <js>"@(foo,bar)"</js>).
	 */
	public static final String CF_UON = "uon";

	//-----------------------------------------------------------------------------------------------------------------
	// Type
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * String.
	 */
	public static final String TYPE_STRING = "string";

	/**
	 * Floating point number.
	 */
	public static final String TYPE_NUMBER = "number";

	/**
	 * Decimal number.
	 */
	public static final String TYPE_INTEGER = "integer";

	/**
	 * Boolean.
	 */
	public static final String TYPE_BOOLEAN = "boolean";

	/**
	 * Array or collection.
	 */
	public static final String TYPE_ARRAY = "array";

	/**
	 * Map or bean.
	 */
	public static final String TYPE_OBJECT = "object";

	/**
	 * File.
	 */
	public static final String TYPE_FILE = "file";

	//-----------------------------------------------------------------------------------------------------------------
	// Format
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Signed 32 bits.
	 */
	public static final String FORMAT_INT32 = "int32";

	/**
	 * Signed 64 bits.
	 */
	public static final String FORMAT_INT64 = "int64";

	/**
	 * 32-bit floating point number.
	 */
	public static final String FORMAT_FLOAT = "float";

	/**
	 * 64-bit floating point number.
	 */
	public static final String FORMAT_DOUBLE = "double";

	/**
	 * BASE-64 encoded characters.
	 */
	public static final String FORMAT_BYTE = "byte";

	/**
	 * Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 */
	public static final String FORMAT_BINARY = "binary";

	/**
	 * Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
	 */
	public static final String FORMAT_BINARY_SPACED = "binary-spaced";

	/**
	 * An RFC3339 full-date.
	 */
	public static final String FORMAT_DATE = "date";

	/**
	 *  An RFC3339 date-time.
	 */
	public static final String FORMAT_DATE_TIME = "date-time";

	/**
	 * Used to hint UIs the input needs to be obscured.
	 */
	public static final String FORMAT_PASSWORD = "password";

	/**
	 * UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>).
	 */
	public static final String FORMAT_UON = "uon";
}
