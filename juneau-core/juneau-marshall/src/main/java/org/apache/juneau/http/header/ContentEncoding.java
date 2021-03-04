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
 * Represents a parsed <l>Content-Encoding</l> HTTP response header.
 *
 * <p>
 * The type of encoding used on the data.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Content-Encoding: gzip
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Encoding entity-header field is used as a modifier to the media-type.
 * When present, its value indicates what additional content codings have been applied to the entity-body, and thus
 * what decoding mechanisms must be applied in order to obtain the media-type referenced by the Content-Type header
 * field.
 * Content-Encoding is primarily used to allow a document to be compressed without losing the identity of its
 * underlying media type.
 * <p class='bcode w800'>
 * 	Content-Encoding  = "Content-Encoding" ":" 1#content-coding
 * </p>
 *
 * <p>
 * Content codings are defined in section 3.5. An example of its use is...
 * <p class='bcode w800'>
 * 	Content-Encoding: gzip
 * </p>
 *
 * <p>
 * The content-coding is a characteristic of the entity identified by the Request-URI.
 * Typically, the entity-body is stored with this encoding and is only decoded before rendering or analogous usage.
 * However, a non-transparent proxy MAY modify the content-coding if the new coding is known to be acceptable to the
 * recipient, unless the "no-transform" cache-control directive is present in the message.
 *
 * <p>
 * If the content-coding of an entity is not "identity", then the response MUST include a Content-Encoding
 * entity-header (section 14.11) that lists the non-identity content-coding(s) used.
 *
 * <p>
 * If the content-coding of an entity in a request message is not acceptable to the origin server, the server SHOULD
 * respond with a status code of 415 (Unsupported Media Type).
 *
 * <p>
 * If multiple encodings have been applied to an entity, the content codings MUST be listed in the order in which they
 * were applied.
 * Additional information about the encoding parameters MAY be provided.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header(name="Content-Encoding",_enum={"gzip","compress","deflate","identity","other"})
public class ContentEncoding extends BasicStringHeader {

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
	 * @return A new {@link ContentEncoding} object.
	 */
	public static ContentEncoding of(Object value) {
		if (value == null)
			return null;
		return new ContentEncoding(value);
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
	 * @return A new {@link ContentEncoding} object.
	 */
	public static ContentEncoding of(Supplier<?> value) {
		if (value == null)
			return null;
		return new ContentEncoding(value);
	}

	/**
	 * Constructor.
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
	public ContentEncoding(Object value) {
		super("Content-Encoding", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public ContentEncoding(String value) {
		this((Object)value);
	}
}
