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

import static org.apache.juneau.http.Constants.*;

import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;


/**
 * Represents a parsed <l>Accept-Charset</l> HTTP request header.
 *
 * <p>
 * Character sets that are acceptable.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Charset: utf-8
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Accept-Charset request-header field can be used to indicate what character sets are acceptable for the response.
 *
 * <p>
 * This field allows clients capable of understanding more comprehensive or special- purpose character sets to signal
 * that capability to a server which is capable of representing documents in those character sets.
 * <p class='bcode w800'>
 * 	Accept-Charset = "Accept-Charset" ":"
 * 	                 1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
 * </p>
 *
 * <p>
 * Character set values are described in section 3.4. Each charset MAY be given an associated quality value which
 * represents the user's preference for that charset.
 * The default value is q=1.
 * An example is...
 * <p class='bcode w800'>
 * 	Accept-Charset: iso-8859-5, unicode-1-1;q=0.8
 * </p>
 *
 * <p>
 * The special value "*", if present in the Accept-Charset field, matches every character set (including ISO-8859-1)
 * which is not mentioned elsewhere in the Accept-Charset field.
 *
 * <p>
 * If no "*" is present in an Accept-Charset field, then all character sets not explicitly mentioned get a quality
 * value of 0, except for ISO-8859-1, which gets a quality value of 1 if not explicitly mentioned.
 *
 * <p>
 * If no Accept-Charset header is present, the default is that any character set is acceptable.
 *
 * <p>
 * If an Accept-Charset header is present, and if the server cannot send a response which is acceptable according to
 * the Accept-Charset header, then the server SHOULD send an error response with the 406 (not acceptable) status code,
 * though the sending of an unacceptable response is also allowed.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Accept-Charset")
public class AcceptCharset extends BasicRangeArrayHeader {

	private static final long serialVersionUID = 1L;

	private static final Cache<String,AcceptCharset> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed and cached header.
	 *
	 * @param value
	 * 	The parameter value.
	 * @return A cached {@link AcceptCharset} object.
	 */
	public static AcceptCharset of(String value) {
		if (value == null)
			return null;
		AcceptCharset x = CACHE.get(value);
		if (x == null)
			x = CACHE.put(value, new AcceptCharset(value));
		return x;
	}

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link AcceptCharset} object.
	 */
	public static AcceptCharset of(Object value) {
		return new AcceptCharset(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link AcceptCharset} object.
	 */
	public static AcceptCharset of(Supplier<?> value) {
		return new AcceptCharset(value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public AcceptCharset(Object value) {
		super("Accept-Charset", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The parameter value.
	 */
	public AcceptCharset(String value) {
		super("Accept-Charset", value);
	}
}
