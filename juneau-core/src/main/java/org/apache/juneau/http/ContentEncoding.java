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
 * Represents a parsed <l>Content-Encoding</l> HTTP response header.
 * <p>
 * The type of encoding used on the data.
 *
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Content-Encoding: gzip
 * </p>
 *
 * <h6 class='topic'>RFC2616 Specification</h6>
 *
 * The Content-Encoding entity-header field is used as a modifier to the media-type.
 * When present, its value indicates what additional content codings have been applied to the entity-body, and thus
 * what decoding mechanisms must be applied in order to obtain the media-type referenced by the Content-Type header
 * field.
 * Content-Encoding is primarily used to allow a document to be compressed without losing the identity of its
 * underlying media type.
 * <p class='bcode'>
 * 	Content-Encoding  = "Content-Encoding" ":" 1#content-coding
 * </p>
 * <p>
 * Content codings are defined in section 3.5. An example of its use is...
 * <p class='bcode'>
 * 	Content-Encoding: gzip
 * </p>
 * <p>
 * The content-coding is a characteristic of the entity identified by the Request-URI.
 * Typically, the entity-body is stored with this encoding and is only decoded before rendering or analogous usage.
 * However, a non-transparent proxy MAY modify the content-coding if the new coding is known to be acceptable to the
 * recipient, unless the "no-transform" cache-control directive is present in the message.
 * <p>
 * If the content-coding of an entity is not "identity", then the response MUST include a Content-Encoding
 * entity-header (section 14.11) that lists the non-identity content-coding(s) used.
 * <p>
 * If the content-coding of an entity in a request message is not acceptable to the origin server, the server SHOULD
 * respond with a status code of 415 (Unsupported Media Type).
 * <p>
 * If multiple encodings have been applied to an entity, the content codings MUST be listed in the order in which they
 * were applied.
 * Additional information about the encoding parameters MAY be provided.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class ContentEncoding extends HeaderEnum<ContentEncodingEnum> {

	/**
	 * Returns a parsed <code>Content-Encoding</code> header.
	 *
	 * @param value The <code>Content-Encoding</code> header string.
	 * @return The parsed <code>Content-Encoding</code> header, or <jk>null</jk> if the string was null.
	 */
	public static ContentEncoding forString(String value) {
		if (value == null)
			return null;
		return new ContentEncoding(value);
	}

	private ContentEncoding(String value) {
		super(value, ContentEncodingEnum.class, ContentEncodingEnum.OTHER);
	}
}
