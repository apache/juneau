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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Content-Type</l> HTTP request/response header.
 *
 * <p>
 * The MIME type of this content.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Type: text/html; charset=utf-8
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Type entity-header field indicates the media type of the entity-body sent to the recipient or, in the
 * case of the HEAD method, the media type that would have been sent had the request been a GET.
 * <p class='bcode'>
 * 	Content-Type   = "Content-Type" ":" media-type
 * </p>
 *
 * <p>
 * Media types are defined in section 3.7.
 * An example of the field is...
 * <p class='bcode'>
 * 	Content-Type: text/html; charset=ISO-8859-4
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Type")
public class ContentType extends BasicMediaTypeHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Type";

	private static Cache<String,ContentType> CACHE = Cache.of(String.class, ContentType.class).build();

	// Constants
	@SuppressWarnings("javadoc")
	public static final ContentType
		APPLICATION_ATOM_XML = of("application/atom+xml"),
		APPLICATION_FORM_URLENCODED = of("application/x-www-form-urlencoded"),
		APPLICATION_JSON = of("application/json"),
		APPLICATION_OCTET_STREAM = of("application/octet-stream"),
		APPLICATION_SOAP_XML = of("application/soap+xml"),
		APPLICATION_SVG_XML = of("application/svg+xml"),
		APPLICATION_XHTML_XML = of("application/xhtml+xml"),
		APPLICATION_XML = of("application/xml"),
		IMAGE_BMP = of("image/bmp"),
		IMAGE_GIF = of("image/gif"),
		IMAGE_JPEG = of("image/jpeg"),
		IMAGE_PNG = of("image/png"),
		IMAGE_SVG = of("image/svg+xml"),
		IMAGE_TIFF = of("image/tiff"),
		IMAGE_WEBP = of("image/webp"),
		MULTIPART_FORM_DATA = of("multipart/form-data"),
		TEXT_HTML = of("text/html"),
		TEXT_OPENAPI = of("text/openapi"),
		TEXT_PLAIN = of("text/plain"),
		TEXT_XML = of("text/xml"),
		WILDCARD = of("*/*"),
		NULL = new ContentType((String)null);

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentType of(String value) {
		return value == null ? null : CACHE.get(value, ()->new ContentType(value));
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentType of(MediaType value) {
		return value == null ? null : new ContentType(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentType of(Supplier<MediaType> value) {
		return value == null ? null : new ContentType(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentType(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentType(MediaType value) {
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
	public ContentType(Supplier<MediaType> value) {
		super(NAME, value);
	}
}
