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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.net.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.Date;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.reflect.*;

/**
 * Standard predefined HTTP headers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public class HttpHeaders {

	@SuppressWarnings("javadoc")
	public static final Accept
		ACCEPT_APPLICATION_ATOM_XML = Accept.APPLICATION_ATOM_XML,
		ACCEPT_APPLICATION_FORM_URLENCODED = Accept.APPLICATION_FORM_URLENCODED,
		ACCEPT_APPLICATION_JSON = Accept.APPLICATION_JSON,
		ACCEPT_APPLICATION_OCTET_STREAM = Accept.APPLICATION_OCTET_STREAM,
		ACCEPT_APPLICATION_SOAP_XML = Accept.APPLICATION_SOAP_XML,
		ACCEPT_APPLICATION_SVG_XML = Accept.APPLICATION_SVG_XML,
		ACCEPT_APPLICATION_XHTML_XML = Accept.APPLICATION_XHTML_XML,
		ACCEPT_APPLICATION_XML = Accept.APPLICATION_XML,
		ACCEPT_IMAGE_BMP = Accept.IMAGE_BMP,
		ACCEPT_IMAGE_GIF = Accept.IMAGE_GIF,
		ACCEPT_IMAGE_JPEG = Accept.IMAGE_JPEG,
		ACCEPT_IMAGE_PNG = Accept.IMAGE_PNG,
		ACCEPT_IMAGE_SVG = Accept.IMAGE_SVG,
		ACCEPT_IMAGE_TIFF = Accept.IMAGE_TIFF,
		ACCEPT_IMAGE_WEBP = Accept.IMAGE_WEBP,
		ACCEPT_MULTIPART_FORM_DATA = Accept.MULTIPART_FORM_DATA,
		ACCEPT_TEXT_HTML = Accept.TEXT_HTML,
		ACCEPT_TEXT_PLAIN = Accept.TEXT_PLAIN,
		ACCEPT_TEXT_XML = Accept.TEXT_XML,
		ACCEPT_WILDCARD = Accept.WILDCARD;

	@SuppressWarnings("javadoc")
	public static final ContentType
		CONTENTTYPE_APPLICATION_ATOM_XML = ContentType.APPLICATION_ATOM_XML,
		CONTENTTYPE_APPLICATION_FORM_URLENCODED = ContentType.APPLICATION_FORM_URLENCODED,
		CONTENTTYPE_APPLICATION_JSON = ContentType.APPLICATION_JSON,
		CONTENTTYPE_APPLICATION_OCTET_STREAM = ContentType.APPLICATION_OCTET_STREAM,
		CONTENTTYPE_APPLICATION_SOAP_XML = ContentType.APPLICATION_SOAP_XML,
		CONTENTTYPE_APPLICATION_SVG_XML = ContentType.APPLICATION_SVG_XML,
		CONTENTTYPE_APPLICATION_XHTML_XML = ContentType.APPLICATION_XHTML_XML,
		CONTENTTYPE_APPLICATION_XML = ContentType.APPLICATION_XML,
		CONTENTTYPE_IMAGE_BMP = ContentType.IMAGE_BMP,
		CONTENTTYPE_IMAGE_GIF = ContentType.IMAGE_GIF,
		CONTENTTYPE_IMAGE_JPEG = ContentType.IMAGE_JPEG,
		CONTENTTYPE_IMAGE_PNG = ContentType.IMAGE_PNG,
		CONTENTTYPE_IMAGE_SVG = ContentType.IMAGE_SVG,
		CONTENTTYPE_IMAGE_TIFF = ContentType.IMAGE_TIFF,
		CONTENTTYPE_IMAGE_WEBP = ContentType.IMAGE_WEBP,
		CONTENTTYPE_MULTIPART_FORM_DATA = ContentType.MULTIPART_FORM_DATA,
		CONTENTTYPE_TEXT_HTML = ContentType.TEXT_HTML,
		CONTENTTYPE_TEXT_PLAIN = ContentType.TEXT_PLAIN,
		CONTENTTYPE_TEXT_XML = ContentType.TEXT_XML,
		CONTENTTYPE_WILDCARD = ContentType.WILDCARD;

	//-----------------------------------------------------------------------------------------------------------------
	// Standard HTTP headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new {@link Accept} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Accept accept(String value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link Accept} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Accept accept(MediaRanges value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link Accept} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Accept accept(MediaType value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link Accept} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Accept accept(Supplier<MediaRanges> value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptCharset acceptCharset(String value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptCharset acceptCharset(StringRanges value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptCharset acceptCharset(Supplier<StringRanges> value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptEncoding acceptEncoding(String value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptEncoding acceptEncoding(StringRanges value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptEncoding acceptEncoding(Supplier<StringRanges> value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptLanguage acceptLanguage(String value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptLanguage acceptLanguage(StringRanges value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptLanguage acceptLanguage(Supplier<StringRanges> value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptRanges} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptRanges acceptRanges(String value) {
		return AcceptRanges.of(value);
	}

	/**
	 * Creates a new {@link AcceptRanges} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final AcceptRanges acceptRanges(Supplier<String> value) {
		return AcceptRanges.of(value);
	}

	/**
	 * Creates a new {@link Age} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Integer#parseInt(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Age age(String value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Age} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Age age(Integer value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Age} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Age age(Supplier<Integer> value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Allow} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Allow allow(String value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Allow} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Allow allow(String...value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Allow} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Allow allow(Supplier<String[]> value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Authorization} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Authorization authorization(String value) {
		return Authorization.of(value);
	}

	/**
	 * Creates a new {@link Authorization} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Authorization authorization(Supplier<String> value) {
		return Authorization.of(value);
	}

	/**
	 * Creates a new {@link CacheControl} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final CacheControl cacheControl(String value) {
		return CacheControl.of(value);
	}

	/**
	 * Creates a new {@link CacheControl} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final CacheControl cacheControl(Supplier<String> value) {
		return CacheControl.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Version#of(String)}
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ClientVersion clientVersion(String value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ClientVersion clientVersion(Version value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ClientVersion clientVersion(Supplier<Version> value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link Connection} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Connection connection(String value) {
		return Connection.of(value);
	}

	/**
	 * Creates a new {@link Connection} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Connection connection(Supplier<String> value) {
		return Connection.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentDisposition contentDisposition(String value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentDisposition contentDisposition(StringRanges value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentDisposition contentDisposition(Supplier<StringRanges> value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentEncoding contentEncoding(String value) {
		return ContentEncoding.of(value);
	}

	/**
	 * Creates a new {@link ContentEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentEncoding contentEncoding(Supplier<String> value) {
		return ContentEncoding.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLanguage contentLanguage(String value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLanguage contentLanguage(String...value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLanguage contentLanguage(Supplier<String[]> value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Long#parseLong(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLength contentLength(String value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLength contentLength(Long value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLength contentLength(Supplier<Long> value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLocation contentLocation(String value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLocation contentLocation(URI value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentLocation contentLocation(Supplier<URI> value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentRange contentRange(String value) {
		return ContentRange.of(value);
	}

	/**
	 * Creates a new {@link ContentRange} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentRange contentRange(Supplier<String> value) {
		return ContentRange.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentType contentType(String value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentType contentType(MediaType value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ContentType contentType(Supplier<MediaType> value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link Date} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Date date(String value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link Date} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Date date(ZonedDateTime value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link Date} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Date date(Supplier<ZonedDateTime> value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link Debug} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final Debug debug(String value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link Debug} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final Debug debug(Boolean value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link Debug} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final Debug debug(Supplier<Boolean> value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link ETag} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ETag eTag(String value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link ETag} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ETag eTag(EntityTag value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link ETag} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ETag eTag(Supplier<EntityTag> value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link Expect} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Expect expect(String value) {
		return Expect.of(value);
	}

	/**
	 * Creates a new {@link Expect} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Expect expect(Supplier<String> value) {
		return Expect.of(value);
	}

	/**
	 * Creates a new {@link Expires} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Expires expires(String value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Expires} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Expires expires(ZonedDateTime value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Expires} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final Expires expires(Supplier<ZonedDateTime> value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Forwarded} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Forwarded forwarded(String value) {
		return Forwarded.of(value);
	}

	/**
	 * Creates a new {@link Forwarded} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Forwarded forwarded(Supplier<String> value) {
		return Forwarded.of(value);
	}

	/**
	 * Creates a new {@link From} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final From from(String value) {
		return From.of(value);
	}

	/**
	 * Creates a new {@link From} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final From from(Supplier<String> value) {
		return From.of(value);
	}

	/**
	 * Creates a new {@link Host} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Host host(String value) {
		return Host.of(value);
	}

	/**
	 * Creates a new {@link Host} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Host host(Supplier<String> value) {
		return Host.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfMatch ifMatch(String value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfMatch ifMatch(EntityTags value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfMatch ifMatch(Supplier<EntityTags> value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfModifiedSince ifModifiedSince(String value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfModifiedSince ifModifiedSince(ZonedDateTime value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfModifiedSince ifModifiedSince(Supplier<ZonedDateTime> value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfNoneMatch ifNoneMatch(String value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfNoneMatch ifNoneMatch(EntityTags value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final IfNoneMatch ifNoneMatch(Supplier<EntityTags> value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfRange ifRange(String value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfRange ifRange(ZonedDateTime value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfRange ifRange(EntityTag value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link EntityTag} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfRange ifRange(Supplier<?> value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfUnmodifiedSince ifUnmodifiedSince(String value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfUnmodifiedSince ifUnmodifiedSince(ZonedDateTime value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final IfUnmodifiedSince ifUnmodifiedSince(Supplier<ZonedDateTime> value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final LastModified lastModified(String value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final LastModified lastModified(ZonedDateTime value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final LastModified lastModified(Supplier<ZonedDateTime> value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link Location} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Location location(String value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link Location} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Location location(URI value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link Location} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Location location(Supplier<URI> value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Integer#parseInt(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final MaxForwards maxForwards(String value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final MaxForwards maxForwards(Integer value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final MaxForwards maxForwards(Supplier<Integer> value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final NoTrace noTrace(String value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final NoTrace noTrace(Boolean value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final NoTrace noTrace(Supplier<Boolean> value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link Origin} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Origin origin(String value) {
		return Origin.of(value);
	}

	/**
	 * Creates a new {@link Origin} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Origin origin(Supplier<String> value) {
		return Origin.of(value);
	}

	/**
	 * Creates a new {@link Pragma} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Pragma pragma(String value) {
		return Pragma.of(value);
	}

	/**
	 * Creates a new {@link Pragma} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Pragma pragma(Supplier<String> value) {
		return Pragma.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthenticate} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ProxyAuthenticate proxyAuthenticate(String value) {
		return ProxyAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthenticate} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ProxyAuthenticate proxyAuthenticate(Supplier<String> value) {
		return ProxyAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthorization} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ProxyAuthorization proxyAuthorization(String value) {
		return ProxyAuthorization.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthorization} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final ProxyAuthorization proxyAuthorization(Supplier<String> value) {
		return ProxyAuthorization.of(value);
	}

	/**
	 * Creates a new {@link Range} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Range range(String value) {
		return Range.of(value);
	}

	/**
	 * Creates a new {@link Range} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Range range(Supplier<String> value) {
		return Range.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Referer referer(String value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Referer referer(URI value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link Referer} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Referer referer(Supplier<URI> value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>) or an integer.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final RetryAfter retryAfter(String value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final RetryAfter retryAfter(ZonedDateTime value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final RetryAfter retryAfter(Integer value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Supplier must supply either {@link Integer} or {@link ZonedDateTime} objects.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final RetryAfter retryAfter(Supplier<?> value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link Server} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Server server(String value) {
		return Server.of(value);
	}

	/**
	 * Creates a new {@link Server} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Server server(Supplier<String> value) {
		return Server.of(value);
	}

	/**
	 * Creates a new {@link TE} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final TE te(String value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link TE} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final TE te(StringRanges value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link TE} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final TE te(Supplier<StringRanges> value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link Thrown} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Thrown thrown(String value) {
		return Thrown.of(value);
	}

	/**
	 * Creates a new {@link Thrown} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Thrown thrown(Throwable...value) {
		return Thrown.of(value);
	}

	/**
	 * Creates a new {@link Trailer} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Trailer trailer(String value) {
		return Trailer.of(value);
	}

	/**
	 * Creates a new {@link Trailer} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Trailer trailer(Supplier<String> value) {
		return Trailer.of(value);
	}

	/**
	 * Creates a new {@link TransferEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final TransferEncoding transferEncoding(String value) {
		return TransferEncoding.of(value);
	}

	/**
	 * Creates a new {@link TransferEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final TransferEncoding transferEncoding(Supplier<String> value) {
		return TransferEncoding.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Upgrade upgrade(String value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Upgrade upgrade(String...value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Upgrade upgrade(Supplier<String[]> value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link UserAgent} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final UserAgent userAgent(String value) {
		return UserAgent.of(value);
	}

	/**
	 * Creates a new {@link UserAgent} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final UserAgent userAgent(Supplier<String> value) {
		return UserAgent.of(value);
	}

	/**
	 * Creates a new {@link Vary} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Vary vary(String value) {
		return Vary.of(value);
	}

	/**
	 * Creates a new {@link Vary} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Vary vary(Supplier<String> value) {
		return Vary.of(value);
	}

	/**
	 * Creates a new {@link Via} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Via via(String value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Via} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Via via(String...value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Via} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Via via(Supplier<String[]> value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Warning} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Warning warning(String value) {
		return Warning.of(value);
	}

	/**
	 * Creates a new {@link Warning} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final Warning warning(Supplier<String> value) {
		return Warning.of(value);
	}

	/**
	 * Creates a new {@link WwwAuthenticate} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final WwwAuthenticate wwwAuthenticate(String value) {
		return WwwAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link WwwAuthenticate} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static final WwwAuthenticate wwwAuthenticate(Supplier<String> value) {
		return WwwAuthenticate.of(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Custom headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new {@link BasicBooleanHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Boolean#parseBoolean(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicBooleanHeader booleanHeader(String name, String value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicBooleanHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicBooleanHeader booleanHeader(String name, Boolean value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicBooleanHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicBooleanHeader booleanHeader(String name, Supplier<Boolean> value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicCsvHeader csvHeader(String name, String value) {
		return BasicCsvHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicCsvHeader csvHeader(String name, String...value) {
		return BasicCsvHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicCsvHeader csvHeader(String name, Supplier<String[]> value) {
		return BasicCsvHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicDateHeader dateHeader(String name, String value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicDateHeader dateHeader(String name, ZonedDateTime value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicDateHeader dateHeader(String name, Supplier<ZonedDateTime> value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagsHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagsHeader entityTagsHeader(String name, String value) {
		return BasicEntityTagsHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagsHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagsHeader entityTagsHeader(String name, EntityTags value) {
		return BasicEntityTagsHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagsHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagsHeader entityTagsHeader(String name, Supplier<EntityTags> value) {
		return BasicEntityTagsHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagHeader entityTagHeader(String name, String value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagHeader entityTagHeader(String name, EntityTag value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicEntityTagHeader entityTagHeader(String name, Supplier<EntityTag> value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable using {@link Integer#parseInt(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicIntegerHeader integerHeader(String name, String value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicIntegerHeader integerHeader(String name, Integer value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicIntegerHeader integerHeader(String name, Supplier<Integer> value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Long#parseLong(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicLongHeader longHeader(String name, String value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link Long#parseLong(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicLongHeader longHeader(String name, Long value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicLongHeader longHeader(String name, Supplier<Long> value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangesHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaRangesHeader mediaRangesHeader(String name, String value) {
		return BasicMediaRangesHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangesHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaRangesHeader mediaRangesHeader(String name, MediaRanges value) {
		return BasicMediaRangesHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangesHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaRangesHeader mediaRangesHeader(String name, Supplier<MediaRanges> value) {
		return value == null ? null : new BasicMediaRangesHeader(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaTypeHeader mediaTypeHeader(String name, String value) {
		return BasicMediaTypeHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaTypeHeader mediaTypeHeader(String name, MediaType value) {
		return BasicMediaTypeHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicMediaTypeHeader mediaTypeHeader(String name, Supplier<MediaType> value) {
		return value == null ? null : new BasicMediaTypeHeader(name, value);
	}

	/**
	 * Creates a {@link BasicHeader} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static final BasicStringHeader stringHeader(String pair) {
		return BasicStringHeader.ofPair(pair);
	}

	/**
	 * Creates a new {@link BasicHeader} header.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicHeader basicHeader(String name, Object value) {
		return BasicHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 * */
	public static final BasicHeader basicHeader(String name, Supplier<?> value) {
		return new BasicHeader(name, value);
	}

	/**
	 * Creates a new {@link SerializedHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The POJO to serialize as the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty.
	 */
	public static final SerializedHeader serializedHeader(String name, Object value) {
		return SerializedHeader.of(name, value);
	}

	/**
	 * Creates a new {@link SerializedHeader} header.
	 *
	 * @param name The HTTP header name name.
	 * @param value
	 * 	The POJO to serialize as the header value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader serializedHeader(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return SerializedHeader.of(name, value, serializer, schema, skipIfEmpty);
	}

	/**
	 * Creates a new {@link SerializedHeader} header.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The HTTP header name name.
	 * @param value
	 * 	The supplier of the POJO to serialize as the header value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader serializedHeader(String name, Supplier<?> value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return SerializedHeader.of(name, value, serializer, schema, skipIfEmpty);
	}

	/**
	 * Creates a new {@link SerializedHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the POJO to serialize as the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty.
	 */
	public static final SerializedHeader serializedHeader(String name, Supplier<?> value) {
		return SerializedHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicStringHeader stringHeader(String name, String value) {
		return BasicStringHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicStringHeader stringHeader(String name, Supplier<String> value) {
		return BasicStringHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangesHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicStringRangesHeader stringRangesHeader(String name, String value) {
		return BasicStringRangesHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangesHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicStringRangesHeader stringRangesHeader(String name, StringRanges value) {
		return BasicStringRangesHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangesHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicStringRangesHeader stringRangesHeader(String name, Supplier<StringRanges> value) {
		return BasicStringRangesHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicUriHeader uriHeader(String name, String value) {
		return BasicUriHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicUriHeader uriHeader(String name, URI value) {
		return BasicUriHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static final BasicUriHeader uriHeader(String name, Supplier<URI> value) {
		return BasicUriHeader.of(name, value);
	}

	/**
	 * Creates a new {@link Header} of the specified type.
	 *
	 * <p>
	 * The implementation class must have a public constructor taking in one of the following argument lists:
	 * <ul>
	 * 	<li><c><jk>public</jk> X(String <jv>headerValue</jv>)</c>
	 * 	<li><c><jk>public</jk> X(Object <jv>headerValue</jv>)</c>
	 * 	<li><c><jk>public</jk> X(String <jv>headerName</jv>, String <jv>headerValue</jv>)</c>
	 * 	<li><c><jk>public</jk> X(String <jv>headerName</jv>, Object <jv>headerValue</jv>)</c>
	 * </ul>
	 *
	 * @param <T> The header implementation class.
	 * @param type The header implementation class.
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static final <T extends Header> T header(Class<T> type, String name, Object value) {
		return HeaderBeanMeta.of(type).construct(name, value);
	}

	/**
	 * Creates a new {@link Header} of the specified type.
	 *
	 * <p>
	 * Same as {@link #header(Class, String, Object)} but the header name is pulled from the {@link org.apache.juneau.http.annotation.Header#name() @Header(name)} or
	 * 	{@link org.apache.juneau.http.annotation.Header#value() @Header(value)} annotations.
	 *
	 * @param <T> The header implementation class.
	 * @param type The header implementation class.
	 * @param value The header value.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static final <T extends Header> T header(Class<T> type, Object value) {
		return HeaderBeanMeta.of(type).construct(null, value);
	}

	/**
	 * Instantiates a new {@link org.apache.juneau.http.header.HeaderList}.
	 *
	 * @return A new empty builder.
	 */
	public static final HeaderList headerList() {
		return HeaderList.create();
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers The headers to add to the list.  Can be <jk>null</jk>.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static final HeaderList headerList(List<Header> headers) {
		return HeaderList.of(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers The headers to add to the list.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static final HeaderList headerList(Header...headers) {
		return HeaderList.of(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified name/value pairs.
	 *
	 * @param pairs
	 * 	Initial list of pairs.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static HeaderList headerList(String...pairs) {
		return HeaderList.ofPairs(pairs);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 */
	public static boolean canCast(Object o) {
		ClassInfo ci = ClassInfo.of(o);
		return ci != null && ci.isChildOfAny(Header.class, Headerable.class, NameValuePair.class, NameValuePairable.class, Map.Entry.class);
	}

	/**
	 * Utility method for converting an arbitrary object to a {@link Header}.
	 *
	 * @param o
	 * 	The object to cast or convert to a {@link Header}.
	 * @return Either the same object cast as a {@link Header} or converted to a {@link Header}.
	 */
	@SuppressWarnings("rawtypes")
	public static Header cast(Object o) {
		if (o instanceof Header)
			return (Header)o;
		if (o instanceof Headerable)
			return ((Headerable)o).asHeader();
		if (o instanceof NameValuePair)
			return BasicHeader.of((NameValuePair)o);
		if (o instanceof NameValuePairable)
			return BasicHeader.of(((NameValuePairable)o).asNameValuePair());
		if (o instanceof Map.Entry) {
			Map.Entry e = (Map.Entry)o;
			return BasicHeader.of(stringify(e.getKey()), stringify(e.getValue()));
		}
		throw new BasicRuntimeException("Object of type {0} could not be converted to a Header.", className(o));
	}
}
