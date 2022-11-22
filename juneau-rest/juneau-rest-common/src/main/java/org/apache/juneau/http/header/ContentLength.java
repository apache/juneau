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
 * Represents a parsed <l>Content-Length</l> HTTP request/response header.
 *
 * <p>
 * The length of the response body in octets (8-bit bytes).
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Length: 348
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Length entity-header field indicates the size of the entity-body, in decimal number of OCTETs, sent to
 * the recipient or, in the case of the HEAD method, the size of the entity-body that would have been sent had the
 * request been a GET.
 * <p class='bcode'>
 * 	Content-Length    = "Content-Length" ":" 1*DIGIT
 * </p>
 *
 * <p>
 * An example is...
 * <p class='bcode'>
 * 	Content-Length: 3495
 * </p>
 *
 * <p>
 * Applications SHOULD use this field to indicate the transfer-length of the message-body, unless this is prohibited by
 * the rules in section 4.4.
 *
 * <p>
 * Any Content-Length greater than or equal to zero is a valid value.
 * Section 4.4 describes how to determine the length of a message-body if a Content-Length is not given.
 *
 * <p>
 * Note that the meaning of this field is significantly different from the corresponding definition in MIME, where it is
 * an optional field used within the "message/external-body" content-type.
 * In HTTP, it SHOULD be sent whenever the message's length can be determined prior to being transferred, unless this is
 * prohibited by the rules in section 4.4.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Length")
public class ContentLength extends BasicLongHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Length";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Long#parseLong(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLength of(String value) {
		return value == null ? null : new ContentLength(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLength of(Long value) {
		return value == null ? null : new ContentLength(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLength of(Supplier<Long> value) {
		return value == null ? null : new ContentLength(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Long#parseLong(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLength(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLength(Long value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLength(Supplier<Long> value) {
		super(NAME, value);
	}
}
