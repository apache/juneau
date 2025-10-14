/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.httppart;

/**
 * Valid values for the <c>format</c> field.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
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
	 * Email address (RFC 5321).
	 *
	 * @since 9.2.0
	 */
	EMAIL,

	/**
	 * Internationalized email address (RFC 6531).
	 *
	 * @since 9.2.0
	 */
	IDN_EMAIL,

	/**
	 * Internet host name (RFC 1123).
	 *
	 * @since 9.2.0
	 */
	HOSTNAME,

	/**
	 * Internationalized host name (RFC 5890).
	 *
	 * @since 9.2.0
	 */
	IDN_HOSTNAME,

	/**
	 * IPv4 address (RFC 2673).
	 *
	 * @since 9.2.0
	 */
	IPV4,

	/**
	 * IPv6 address (RFC 4291).
	 *
	 * @since 9.2.0
	 */
	IPV6,

	/**
	 * Universal Resource Identifier (RFC 3986).
	 *
	 * @since 9.2.0
	 */
	URI,

	/**
	 * URI Reference (RFC 3986).
	 *
	 * @since 9.2.0
	 */
	URI_REFERENCE,

	/**
	 * Internationalized Resource Identifier (RFC 3987).
	 *
	 * @since 9.2.0
	 */
	IRI,

	/**
	 * IRI Reference (RFC 3987).
	 *
	 * @since 9.2.0
	 */
	IRI_REFERENCE,

	/**
	 * Universally Unique Identifier (RFC 4122).
	 *
	 * @since 9.2.0
	 */
	UUID,

	/**
	 * URI Template (RFC 6570).
	 *
	 * @since 9.2.0
	 */
	URI_TEMPLATE,

	/**
	 * JSON Pointer (RFC 6901).
	 *
	 * @since 9.2.0
	 */
	JSON_POINTER,

	/**
	 * Relative JSON Pointer.
	 *
	 * @since 9.2.0
	 */
	RELATIVE_JSON_POINTER,

	/**
	 * Regular expression (ECMA-262).
	 *
	 * @since 9.2.0
	 */
	REGEX,

	/**
	 * Duration (RFC 3339 Appendix A).
	 *
	 * @since 9.2.0
	 */
	DURATION,

	/**
	 * Time (RFC 3339).
	 *
	 * @since 9.2.0
	 */
	TIME,

	/**
	 * Date and time with time zone (RFC 3339).
	 *
	 * @since 9.2.0
	 */
	DATE_TIME_ZONE,

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