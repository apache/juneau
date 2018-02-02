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
 * Represents a parsed <l>Referer</l> HTTP request header.
 * 
 * <p>
 * This is the address of the previous web page from which a link to the currently requested page was followed.
 * (The word “referrer” has been misspelled in the RFC as well as in most implementations to the point that it has
 * become standard usage and is considered correct terminology)
 * 
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
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
 * <p class='bcode'>
 * 	Referer        = "Referer" ":" ( absoluteURI | relativeURI )
 * </p>
 * 
 * <p>
 * Example:
 * <p class='bcode'>
 * 	Referer: http://www.w3.org/hypertext/DataSources/Overview.html
 * </p>
 * 
 * <p>
 * If the field value is a relative URI, it SHOULD be interpreted relative to the Request-URI.
 * The URI MUST NOT include a fragment. See section 15.1.3 for security considerations.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class Referer extends HeaderUri {

	/**
	 * Returns a parsed <code>Referer</code> header.
	 * 
	 * @param value The <code>Referer</code> header string.
	 * @return The parsed <code>Referer</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Referer forString(String value) {
		if (value == null)
			return null;
		return new Referer(value);
	}

	private Referer(String value) {
		super(value);
	}
}
