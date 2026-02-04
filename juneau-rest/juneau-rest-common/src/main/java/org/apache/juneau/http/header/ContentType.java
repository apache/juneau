/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.http.annotation.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Type")
public class ContentType extends BasicMediaTypeHeader {
	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Type";

	private static Cache<String,ContentType> CACHE = Cache.of(String.class, ContentType.class).build();

	// Constants
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_ATOM_XML = of("application/atom+xml");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_FORM_URLENCODED = of("application/x-www-form-urlencoded");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_JSON = of("application/json");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_OCTET_STREAM = of("application/octet-stream");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_SOAP_XML = of("application/soap+xml");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_SVG_XML = of("application/svg+xml");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_XHTML_XML = of("application/xhtml+xml");
	@SuppressWarnings("javadoc")
	public static final ContentType APPLICATION_XML = of("application/xml");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_BMP = of("image/bmp");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_GIF = of("image/gif");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_JPEG = of("image/jpeg");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_PNG = of("image/png");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_SVG = of("image/svg+xml");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_TIFF = of("image/tiff");
	@SuppressWarnings("javadoc")
	public static final ContentType IMAGE_WEBP = of("image/webp");
	@SuppressWarnings("javadoc")
	public static final ContentType MULTIPART_FORM_DATA = of("multipart/form-data");
	@SuppressWarnings("javadoc")
	public static final ContentType TEXT_HTML = of("text/html");
	@SuppressWarnings("javadoc")
	public static final ContentType TEXT_OPENAPI = of("text/openapi");
	@SuppressWarnings("javadoc")
	public static final ContentType TEXT_PLAIN = of("text/plain");
	@SuppressWarnings("javadoc")
	public static final ContentType TEXT_XML = of("text/xml");
	@SuppressWarnings("javadoc")
	public static final ContentType WILDCARD = of("*/*");
	@SuppressWarnings("javadoc")
	public static final ContentType NULL = new ContentType((String)null);

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
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentType of(String value) {
		return value == null ? null : CACHE.get(value, () -> new ContentType(value));
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