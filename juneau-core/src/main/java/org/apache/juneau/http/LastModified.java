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
 * Represents a parsed <l>Last-Modified</l> HTTP response header.
 * <p>
 * The last modified date for the requested object (in "HTTP-date" format as defined by RFC 7231).
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Last-Modified entity-header field indicates the date and time at which the origin server believes the variant was
 * last modified.
 * <p class='bcode'>
 * 	Last-Modified  = "Last-Modified" ":" HTTP-date
 * </p>
 * <p>
 * An example of its use is...
 * <p class='bcode'>
 * 	Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
 * </p>
 * <p>
 * The exact meaning of this header field depends on the implementation of the origin server and the nature of the
 * original resource.
 * For files, it may be just the file system last-modified time.
 * For entities with dynamically included parts, it may be the most recent of the set of last-modify times for its
 * component parts.
 * For database gateways, it may be the last-update time stamp of the record.
 * For virtual objects, it may be the last time the internal state changed.
 * <p>
 * An origin server MUST NOT send a Last-Modified date which is later than the server's time of message origination.
 * In such cases, where the resource's last modification would indicate some time in the future, the server MUST replace
 * that date with the message origination date.
 * <p>
 * An origin server SHOULD obtain the Last-Modified value of the entity as close as possible to the time that it
 * generates the Date value of its response.
 * This allows a recipient to make an accurate assessment of the entity's modification time, especially if the entity
 * changes near the time that the response is generated.
 * <p>
 * HTTP/1.1 servers SHOULD send Last-Modified whenever feasible.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class LastModified extends HeaderDate {

	/**
	 * Returns a parsed <code>Last-Modified</code> header.
	 *
	 * @param value The <code>Last-Modified</code> header string.
	 * @return The parsed <code>Last-Modified</code> header, or <jk>null</jk> if the string was null.
	 */
	public static LastModified forString(String value) {
		if (value == null)
			return null;
		return new LastModified(value);
	}

	private LastModified(String value) {
		super(value);
	}
}
