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
 * Represents a parsed <l>Accept-Range</l> HTTP response header.
 * 
 * <p>
 * What partial content range types this server supports via byte serving.
 * 
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Accept-Ranges: bytes
 * </p>
 * 
 * <h5 class='topic'>RFC2616 Specification</h5>
 * 
 * The Accept-Ranges response-header field allows the server to indicate its acceptance of range requests for a
 * resource:
 * <p class='bcode'>
 * 	Accept-Ranges     = "Accept-Ranges" ":" acceptable-ranges
 * 	acceptable-ranges = 1#range-unit | "none"
 * </p>
 * 
 * <p>
 * Origin servers that accept byte-range requests MAY send...
 * <p class='bcode'>
 * 	Accept-Ranges: bytes
 * </p>
 * <p>
 * ...but are not required to do so.
 * 
 * <p>
 * Clients MAY generate byte-range requests without having received this header for the resource involved.
 * 
 * <p>
 * Range units are defined in section 3.12.
 * 
 * <p>
 * Servers that do not accept any kind of range request for a resource MAY send...
 * <p class='bcode'>
 * 	Accept-Ranges: none
 * </p>
 * <p>
 * ...to advise the client not to attempt a range request.
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
public final class AcceptRanges extends HeaderString {

	/**
	 * Returns a parsed <code>Accept-Ranges</code> header.
	 * 
	 * @param value The <code>Accept-Ranges</code> header string.
	 * @return The parsed <code>Accept-Ranges</code> header, or <jk>null</jk> if the string was null.
	 */
	public static AcceptRanges forString(String value) {
		if (value == null)
			return null;
		return new AcceptRanges(value);
	}

	private AcceptRanges(String value) {
		super(value);
	}
}
