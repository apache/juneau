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
 * Represents a parsed <l>ETag</l> HTTP response header.
 * <p>
 * An identifier for a specific version of a resource, often a message digest.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	ETag: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The ETag response-header field provides the current value of the entity tag for the requested variant.
 * The headers used with entity tags are described in sections 14.24, 14.26 and 14.44.
 * The entity tag MAY be used for comparison with other entities from the same resource (see section 13.3.3).
 * <p class='bcode'>
 * 	ETag = "ETag" ":" entity-tag
 * </p>
 * <p>
 * Examples:
 * <p class='bcode'>
 * 	ETag: "xyzzy"
 * 	ETag: W/"xyzzy"
 * 	ETag: ""
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class ETag extends HeaderString {

	/**
	 * Returns a parsed <code>ETag</code> header.
	 *
	 * @param value The <code>ETag</code> header string.
	 * @return The parsed <code>ETag</code> header, or <jk>null</jk> if the string was null.
	 */
	public static ETag forString(String value) {
		if (value == null)
			return null;
		return new ETag(value);
	}

	private ETag(String value) {
		super(value);
	}
}
