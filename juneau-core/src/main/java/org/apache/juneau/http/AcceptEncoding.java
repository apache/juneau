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
 * Represents a parsed <l>Accept-Encoding</l> HTTP request header.
 * <p>
 * List of acceptable encodings.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Accept-Encoding: gzip, deflate
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Accept-Encoding request-header field is similar to Accept, but restricts the content-codings (section 3.5) that
 * are acceptable in the response.
 *
 * <p class='bcode'>
 * 	Accept-Encoding  = "Accept-Encoding" ":"
 * 	                   1#( codings [ ";" "q" "=" qvalue ] )
 * 	codings          = ( content-coding | "*" )
 * </p>
 * <p>
 * Examples of its use are:
 * <p class='bcode'>
 * 	Accept-Encoding: compress, gzip
 * 	Accept-Encoding:
 * 	Accept-Encoding: *
 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * 	Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
 * </p>
 * <p>
 * A server tests whether a content-coding is acceptable, according to an Accept-Encoding field, using these rules:
 * <ol>
 * 	<li>If the content-coding is one of the content-codings listed in the Accept-Encoding field, then it is
 * 		acceptable, unless it is accompanied by a qvalue of 0.
 * 		(As defined in section 3.9, a qvalue of 0 means "not acceptable.")
 * 	<li>The special "*" symbol in an Accept-Encoding field matches any available content-coding not explicitly listed
 * 		in the header field.
 * 	<li>If multiple content-codings are acceptable, then the acceptable content-coding with the highest non-zero
 * 		qvalue is preferred.
 * 	<li>The "identity" content-coding is always acceptable, unless specifically refused because the Accept-Encoding
 * 		field includes "identity;q=0", or because the field includes "*;q=0" and does not explicitly include the
 * 		"identity" content-coding.
 * 		If the Accept-Encoding field-value is empty, then only the "identity" encoding is acceptable.
 * </ol>
 * <p>
 * If an Accept-Encoding field is present in a request, and if the server cannot send a response which is acceptable
 * according to the Accept-Encoding header, then the server SHOULD send an error response with the 406 (Not Acceptable)
 * status code.
 * <p>
 * If no Accept-Encoding field is present in a request, the server MAY assume that the client will accept any content
 * coding.
 * In this case, if "identity" is one of the available content-codings, then the server SHOULD use the "identity"
 * content-coding, unless it has additional information that a different content-coding is meaningful to the client.
 * <p>
 * Note: If the request does not include an Accept-Encoding field, and if the "identity" content-coding is unavailable,
 * then content-codings commonly understood by HTTP/1.0 clients (i.e.,"gzip" and "compress") are preferred; some older
 * clients improperly display messages sent with other content-codings.
 * The server might also make this decision based on information about the particular user-agent or client.
 * <p>
 * Note: Most HTTP/1.0 applications do not recognize or obey qvalues associated with content-codings.
 * This means that qvalues will not work and are not permitted with x-gzip or x-compress.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class AcceptEncoding extends HeaderRangeArray {

	private static final Cache<String,AcceptEncoding> cache = new Cache<String,AcceptEncoding>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed <code>Accept-Encoding</code> header.
	 *
	 * @param value The <code>Accept-Encoding</code> header string.
	 * @return The parsed <code>Accept-Encoding</code> header, or <jk>null</jk> if the string was null.
	 */
	public static AcceptEncoding forString(String value) {
		if (value == null)
			return null;
		AcceptEncoding a = cache.get(value);
		if (a == null)
			a = cache.put(value, new AcceptEncoding(value));
		return a;
	}

	private AcceptEncoding(String value) {
		super(value);
	}
}
