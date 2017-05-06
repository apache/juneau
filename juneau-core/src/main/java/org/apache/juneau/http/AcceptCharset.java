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

import static org.apache.juneau.http.Constants.*;

import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Accept-Charset</l> HTTP request header.
 * <p>
 * Character sets that are acceptable.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Accept-Charset: utf-8
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Accept-Charset request-header field can be used to indicate what character sets are acceptable for the response.
 * <p>
 * This field allows clients capable of understanding more comprehensive or special- purpose character sets to signal
 * that capability to a server which is capable of representing documents in those character sets.
 * <p class='bcode'>
 * 	Accept-Charset = "Accept-Charset" ":"
 * 	                 1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
 * </p>
 * <p>
 * Character set values are described in section 3.4. Each charset MAY be given an associated quality value which
 * represents the user's preference for that charset.
 * The default value is q=1.
 * An example is...
 * <p class='bcode'>
 * 	Accept-Charset: iso-8859-5, unicode-1-1;q=0.8
 * </p>
 * <p>
 * The special value "*", if present in the Accept-Charset field, matches every character set (including ISO-8859-1)
 * which is not mentioned elsewhere in the Accept-Charset field.
 * <p>
 * If no "*" is present in an Accept-Charset field, then all character sets not explicitly mentioned get a quality
 * value of 0, except for ISO-8859-1, which gets a quality value of 1 if not explicitly mentioned.
 * <p>
 * If no Accept-Charset header is present, the default is that any character set is acceptable.
 * <p>
 * If an Accept-Charset header is present, and if the server cannot send a response which is acceptable according to
 * the Accept-Charset header, then the server SHOULD send an error response with the 406 (not acceptable) status code,
 * though the sending of an unacceptable response is also allowed.
 */
public final class AcceptCharset extends HeaderRangeArray {

	private static final Cache<String,AcceptCharset> cache = new Cache<String,AcceptCharset>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed <code>Accept-Charset</code> header.
	 *
	 * @param value The <code>Accept-Charset</code> header string.
	 * @return The parsed <code>Accept-Charset</code> header, or <jk>null</jk> if the string was null.
	 */
	public static AcceptCharset forString(String value) {
		if (value == null)
			return null;
		AcceptCharset a = cache.get(value);
		if (a == null)
			a = cache.put(value, new AcceptCharset(value));
		return a;
	}

	private AcceptCharset(String value) {
		super(value);
	}
}
