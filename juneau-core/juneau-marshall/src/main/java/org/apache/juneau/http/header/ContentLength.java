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
 * <p class='bcode w800'>
 * 	Content-Length: 348
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Length entity-header field indicates the size of the entity-body, in decimal number of OCTETs, sent to
 * the recipient or, in the case of the HEAD method, the size of the entity-body that would have been sent had the
 * request been a GET.
 * <p class='bcode w800'>
 * 	Content-Length    = "Content-Length" ":" 1*DIGIT
 * </p>
 *
 * <p>
 * An example is...
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Content-Length")
public class ContentLength extends BasicLongHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link ContentLength} object.
	 */
	public static ContentLength of(Object value) {
		if (value == null)
			return null;
		return new ContentLength(value);
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
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new {@link ContentLength} object.
	 */
	public static ContentLength of(Supplier<?> value) {
		if (value == null)
			return null;
		return new ContentLength(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public ContentLength(Object value) {
		super("Content-Length", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public ContentLength(String value) {
		this((Object)value);
	}
}
