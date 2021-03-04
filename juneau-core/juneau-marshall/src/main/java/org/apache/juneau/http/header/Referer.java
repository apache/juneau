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

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Referer</l> HTTP request header.
 *
 * <p>
 * This is the address of the previous web page from which a link to the currently requested page was followed.
 * (The word “referrer” has been misspelled in the RFC as well as in most implementations to the point that it has
 * become standard usage and is considered correct terminology)
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Referer: http://en.wikipedia.org/wiki/Main_Page
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Referer[sic] request-header field allows the client to specify, for the server's benefit, the address (URI) of
 * the resource from which the Request-URI was obtained (the "referrer", although the header field is misspelled.)
 * The Referer request-header allows a server to generate lists of back-links to resources for interest, logging,
 * optimized caching, etc.
 * It also allows obsolete or mistyped links to be traced for maintenance.
 * The Referer field MUST NOT be sent if the Request-URI was obtained from a source that does not have its own URI,
 * such as input from the user keyboard.
 *
 * <p class='bcode w800'>
 * 	Referer        = "Referer" ":" ( absoluteURI | relativeURI )
 * </p>
 *
 * <p>
 * Example:
 * <p class='bcode w800'>
 * 	Referer: http://www.w3.org/hypertext/DataSources/Overview.html
 * </p>
 *
 * <p>
 * If the field value is a relative URI, it SHOULD be interpreted relative to the Request-URI.
 * The URI MUST NOT include a fragment. See section 15.1.3 for security considerations.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header("Referer")
public class Referer extends BasicUriHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link Referer} object.
	 */
	public static Referer of(Object value) {
		if (value == null)
			return null;
		return new Referer(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link Referer} object.
	 */
	public static Referer of(Supplier<?> value) {
		if (value == null)
			return null;
		return new Referer(value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public Referer(Object value) {
		super("Referer", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public Referer(String value) {
		this((Object)value);
	}
}
