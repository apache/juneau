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
 * Represents a parsed <l>Allow</l> HTTP response header.
 * <p>
 * Valid methods for a specified resource. To be used for a 405 Method not allowed.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Allow: GET, HEAD
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Allow entity-header field lists the set of methods supported by the resource identified by the Request-URI.
 * The purpose of this field is strictly to inform the recipient of valid methods associated with the resource.
 * An Allow header field MUST be present in a 405 (Method Not Allowed) response.
 *
 * <p class='bcode'>
 * 	Allow   = "Allow" ":" #Method
 * </p>
 * <p>
 * Example of use:
 * <p class='bcode'>
 * 	Allow: GET, HEAD, PUT
 * </p>
 * <p>
 * This field cannot prevent a client from trying other methods.
 * However, the indications given by the Allow header field value SHOULD be followed.
 * <p>
 * The actual set of allowed methods is defined by the origin server at the time of each request.
 * <p>
 * The Allow header field MAY be provided with a PUT request to recommend the methods to be supported by the new or
 * modified resource.
 * <p>
 * The server is not required to support these methods and SHOULD include an Allow header in the response giving the
 * actual supported methods.
 * <p>
 * A proxy MUST NOT modify the Allow header field even if it does not understand all the methods specified, since the
 * user agent might
 * have other means of communicating with the origin server.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class Allow extends HeaderStringArray {

	/**
	 * Returns a parsed <code>Allow</code> header.
	 *
	 * @param value The <code>Allow</code> header string.
	 * @return The parsed <code>Allow</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Allow forString(String value) {
		if (value == null)
			return null;
		return new Allow(value);
	}

	private Allow(String value) {
		super(value);
	}
}
